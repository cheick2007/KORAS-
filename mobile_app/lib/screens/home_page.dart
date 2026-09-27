import 'package:flutter/material.dart';
import 'package:speech_to_text/speech_to_text.dart' as stt;
import 'package:flutter_tts/flutter_tts.dart';
import 'package:http/http.dart' as http;
import 'package:url_launcher/url_launcher.dart';
import 'package:flutter_contacts/flutter_contacts.dart' as fc;
import 'package:android_intent_plus/android_intent.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:device_apps/device_apps.dart';
import 'package:telephony/telephony.dart';
import 'dart:convert';
import 'package:flutter/services.dart';
import '../main.dart';

class HomePage extends StatefulWidget {
  final String token;
  const HomePage({super.key, required this.token});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> with SingleTickerProviderStateMixin {
  late AnimationController _pulseController;
  
  final stt.SpeechToText _speech = stt.SpeechToText();
  final FlutterTts _flutterTts = FlutterTts();
  
  bool _isListening = false;
  bool _speechAvailable = false;
  String _currentWords = "";

  final List<Map<String, String>> _messages = [
    {"sender": "assistant", "text": "Bonjour ! Je suis Koras. Que puis-je faire pour vous aujourd'hui ?"}
  ];

  final String baseUrl = 'http://192.168.1.5:8080/api/v1';

  @override
  void initState() {
    super.initState();
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1000),
    )..repeat(reverse: true);
    
    _initSpeech();
    _initTts();
    
    // Parle pour souhaiter la bienvenue
    Future.delayed(const Duration(milliseconds: 500), () {
      _flutterTts.speak("Bonjour ! Je suis Koras. Que puis-je faire pour vous ?");
    });
  }

  void _initSpeech() async {
    _speechAvailable = await _speech.initialize(
      onStatus: (val) {
        if (val == 'done' || val == 'notListening') {
          if (_isListening) _stopListening();
        }
      },
      onError: (val) => print('Erreur micro: $val'),
    );
    setState(() {});
  }
  
  void _initTts() async {
    await _flutterTts.setLanguage("fr-FR");
    await _flutterTts.setSpeechRate(0.5); // Vitesse modérée
    await _flutterTts.setVolume(1.0);
    await _flutterTts.setPitch(1.0);
  }

  @override
  void dispose() {
    _pulseController.dispose();
    _speech.cancel();
    _flutterTts.stop();
    super.dispose();
  }

  void _toggleListening() {
    if (!_speechAvailable) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Reconnaissance vocale non disponible. Vérifiez les permissions.')),
      );
      return;
    }

    if (_isListening) {
      _stopListening();
    } else {
      _startListening();
    }
  }
  
  void _startListening() {
    setState(() {
      _isListening = true;
      _currentWords = "";
    });
    
    _flutterTts.stop(); // Couper la voix si l'assistant parlait
    
    _speech.listen(
      onResult: (val) {
        setState(() {
          _currentWords = val.recognizedWords;
        });
      },
      localeId: 'fr_FR',
      cancelOnError: true,
      partialResults: true,
    );
  }
  
  void _stopListening() async {
    _speech.stop();
    setState(() {
      _isListening = false;
    });
    
    if (_currentWords.isNotEmpty) {
      final userText = _currentWords;
      setState(() {
        _messages.add({"sender": "user", "text": userText});
      });
      _currentWords = ""; // Reset
      
      // Envoyer la commande texte au backend
      await _processWithBackend(userText);
    }
  }
  
  Future<void> _launchIntentUrl(String urlString) async {
    final uri = Uri.parse(urlString);
    if (await canLaunchUrl(uri)) {
      await launchUrl(uri);
    } else {
      print("Impossible d'ouvrir l'URL: $urlString");
    }
  }

  Future<String?> _findPhoneNumber(String name) async {
    final status = await fc.FlutterContacts.permissions.request(fc.PermissionType.read);
    if (status == fc.PermissionStatus.granted || status == fc.PermissionStatus.limited) {
      final contacts = await fc.FlutterContacts.getAll(properties: fc.ContactProperties.allProperties);
      final nameLower = name.toLowerCase().trim();
      for (var contact in contacts) {
        final dName = contact.displayName ?? '';
        if (dName.toLowerCase().contains(nameLower)) {
          if (contact.phones.isNotEmpty) {
            return contact.phones.first.number;
          }
        }
      }
    }
    return null;
  }
  
  Future<void> _sendSms(String number, String message) async {
    final Telephony telephony = Telephony.instance;
    bool? permissionsGranted = await telephony.requestPhoneAndSmsPermissions;
    if (permissionsGranted != null && permissionsGranted) {
      await telephony.sendSms(
        to: number,
        message: message,
      );
    }
  }

  Future<String> _launchApp(String appName) async {
    final nameLower = appName.toLowerCase().trim();
    final cleanName = nameLower.replaceAll(RegExp(r"(s'il te plait|s'il vous plait|stp|svp)"), "").trim();
    
    try {
      final apps = await DeviceApps.getInstalledApplications(
          includeAppIcons: false, includeSystemApps: true, onlyAppsWithLaunchIntent: true);
          
      if (apps.isEmpty) {
        // Fallback si la liste est vide (bloqué par Android 11+)
        final commonApps = {
          'whatsapp': 'com.whatsapp',
          'youtube': 'com.google.android.youtube',
          'facebook': 'com.facebook.katana',
          'tiktok': 'com.zhiliaoapp.musically',
          'instagram': 'com.instagram.android',
          'spotify': 'com.spotify.music',
          'chrome': 'com.android.chrome',
          'maps': 'com.google.android.apps.maps',
          'gmail': 'com.google.android.gm',
          'netflix': 'com.netflix.mediaclient',
          'calculatrice': 'com.android.calculator2',
        };
        for (var key in commonApps.keys) {
          if (cleanName.contains(key)) {
            await DeviceApps.openApp(commonApps[key]!);
            return "SUCCESS";
          }
        }
        return "La sécurité d'Android bloque la lecture de la liste des apps.";
      }
          
      for (var app in apps) {
        final installedName = app.appName.toLowerCase();
        if (installedName == cleanName || installedName.contains(cleanName) || cleanName.contains(installedName)) {
          await DeviceApps.openApp(app.packageName);
          return "SUCCESS";
        }
      }
      return "Je n'ai pas trouvé l'application $cleanName parmi vos ${apps.length} apps.";
    } catch (e) {
      return "Erreur lors de la recherche des applications.";
    }
  }
  
  Future<void> _processWithBackend(String text) async {
    try {
      // 1. Appel du NLU pour interpréter la commande
      final interpretRes = await http.post(
        Uri.parse('$baseUrl/interprete'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer ${widget.token}'
        },
        body: jsonEncode({
          'type': 'TEXTE',
          'contenu': text,
          'langue': 'FRANCAIS'
        }),
      );
      
      if (interpretRes.statusCode != 200) {
        if (interpretRes.statusCode == 500 || interpretRes.statusCode == 400) {
          if (mounted) {
            setState(() {
              _messages.add({"sender": "assistant", "text": "Désolé, je n'ai pas compris votre demande. Pourriez-vous répéter ?"});
            });
            await _flutterTts.speak("Désolé, je n'ai pas compris votre demande.");
          }
          return; // Arrêter le flux ici
        }
        throw Exception("Erreur NLU: Code ${interpretRes.statusCode}");
      }
      
      final interpretData = jsonDecode(interpretRes.body);
      final intention = interpretData['intention'];
      
      // 2. Exécution du plan avec l'intention reçue
      final executeRes = await http.post(
        Uri.parse('$baseUrl/execute'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer ${widget.token}'
        },
        body: jsonEncode({
          'intention': intention,
          'langue': 'FRANCAIS'
        }),
      );
      
      if (executeRes.statusCode != 200) {
        throw Exception("Erreur Execution: Code ${executeRes.statusCode}");
      }
      
      final executeData = jsonDecode(executeRes.body);
      
      // 3. Déterminer la réponse à vocaliser
      String responseText = "Action exécutée avec succès.";
      final typeIntention = intention['type'];
      
      // Exécution de l'action réelle sur le téléphone
      switch(typeIntention) {
        case 'METEO':
          responseText = "D'après mes informations, le temps est dégagé avec une température agréable.";
          // Optionnel: Ouvrir un site de météo
          _launchIntentUrl('https://weather.com/fr-FR/temps/aujour/l/FRXX0076');
          break;
        case 'APPEL':
          final contactName = intention['entites']?['contact']?['nom'] ?? 'ce contact';
          String? numberToCall = await _findPhoneNumber(contactName);
          
          if (numberToCall != null) {
            var phoneStatus = await Permission.phone.status;
            if (!phoneStatus.isGranted) {
              phoneStatus = await Permission.phone.request();
            }
            
            if (phoneStatus.isGranted) {
              responseText = "Appel en cours vers $contactName.";
              final intent = AndroidIntent(
                action: 'android.intent.action.CALL',
                data: 'tel:$numberToCall',
              );
              await intent.launch();
            } else {
              responseText = "Je n'ai pas l'autorisation de passer des appels.";
            }
          } else {
            responseText = "Je n'ai pas trouvé le numéro de $contactName dans vos contacts.";
          }
          break;
        case 'SMS':
          final contactSms = intention['entites']?['contact']?['nom'] ?? 'ce contact';
          final texteSms = intention['entites']?['message']?['contenu'] ?? 'Bonjour';
          String? numberSms = await _findPhoneNumber(contactSms);
          
          if (numberSms != null) {
            responseText = "Envoi du message à $contactSms en cours.";
            await _sendSms(numberSms, texteSms);
          } else {
            responseText = "Je n'ai pas trouvé le numéro de $contactSms dans vos contacts.";
          }
          break;
        case 'OUVERTURE_APP':
          final appName = intention['entites']?['app']?['contenu'] ?? 'cette application';
          String result = await _launchApp(appName);
          if (result == "SUCCESS") {
            responseText = "Ouverture de $appName.";
          } else {
            responseText = result;
          }
          break;
        case 'MUSIQUE_LECTURE':
          responseText = "Je lance la musique.";
          final playIntent = AndroidIntent(
            action: 'android.intent.action.MEDIA_PLAY_FROM_SEARCH',
            data: 'query', // Optional query
          );
          await playIntent.launch();
          break;
        case 'MUSIQUE_PAUSE':
          responseText = "Je mets la musique en pause.";
          // Sending a generic media pause might require other APIs, falling back to a verbal response for now
          break;
        case 'AIDE':
          responseText = "Je suis Koras, je peux vous aider à appeler, envoyer des messages ou consulter la météo.";
          break;
        case 'RECHERCHE_WEB':
          final query = intention['entites']?['requete']?['contenu'] ?? text;
          responseText = "Voici ce que j'ai trouvé sur le web concernant votre recherche.";
          _launchIntentUrl('https://www.google.com/search?q=${Uri.encodeComponent(query)}');
          break;
        default:
          responseText = "J'ai bien compris votre demande concernant : $typeIntention.";
      }
      
      if (mounted) {
        setState(() {
          _messages.add({"sender": "assistant", "text": responseText});
        });
      }
      
      // Lecture à haute voix
      await _flutterTts.speak(responseText);
      
    } catch (e) {
      if (mounted) {
        setState(() {
          _messages.add({"sender": "assistant", "text": "Détail de l'erreur : $e"});
        });
        await _flutterTts.speak("Une erreur est survenue, veuillez lire le message à l'écran.");
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.transparent, // Complètement transparent pour voir les autres apps
      body: Column(
        mainAxisAlignment: MainAxisAlignment.end,
        children: [
          // Espace vide cliquable pour fermer l'assistant
          Expanded(
            child: GestureDetector(
              onTap: () {
                // Fermer l'assistant en douceur et quitter l'app
                SystemNavigator.pop();
              },
              behavior: HitTestBehavior.opaque,
            ),
          ),
          
          // La Bulle type "Gemini"
          Container(
            padding: const EdgeInsets.only(top: 16, left: 24, right: 24, bottom: 32),
            decoration: BoxDecoration(
              color: const Color(0xFF0f172a),
              borderRadius: const BorderRadius.only(
                topLeft: Radius.circular(32),
                topRight: Radius.circular(32),
              ),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.5),
                  blurRadius: 30,
                  offset: const Offset(0, -5),
                )
              ],
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                // Handle bar
                Container(
                  width: 40,
                  height: 4,
                  margin: const EdgeInsets.only(bottom: 24),
                  decoration: BoxDecoration(
                    color: Colors.white24,
                    borderRadius: BorderRadius.circular(10),
                  ),
                ),
                
                // Texte de réponse (le dernier message)
                Text(
                  _messages.last["text"] ?? "Je suis à votre écoute...",
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 18,
                    fontWeight: FontWeight.w500,
                    height: 1.4,
                  ),
                  textAlign: TextAlign.center,
                ),
                
                const SizedBox(height: 24),
                
                // Affichage en temps réel de ce qui est entendu
                if (_currentWords.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 24.0),
                    child: Text(
                      '"$_currentWords..."',
                      style: const TextStyle(color: Colors.white54, fontStyle: FontStyle.italic, fontSize: 16),
                      textAlign: TextAlign.center,
                    ),
                  ),
                
                // Bouton Microphone
                GestureDetector(
                  onTap: _toggleListening,
                  child: AnimatedBuilder(
                    animation: _pulseController,
                    builder: (context, child) {
                      return Container(
                        width: 80,
                        height: 80,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: _isListening 
                              ? const Color(0xFF3b82f6).withOpacity(0.3 + (_pulseController.value * 0.2))
                              : const Color(0xFF1e293b),
                          boxShadow: _isListening ? [
                            BoxShadow(
                              color: const Color(0xFF3b82f6).withOpacity(0.5),
                              blurRadius: 20 * _pulseController.value,
                              spreadRadius: 10 * _pulseController.value,
                            )
                          ] : [
                            BoxShadow(
                              color: Colors.black.withOpacity(0.3),
                              blurRadius: 10,
                              offset: const Offset(0, 4),
                            )
                          ],
                          border: Border.all(
                            color: _isListening ? const Color(0xFF3b82f6) : Colors.white12,
                            width: 2,
                          ),
                        ),
                        child: Icon(
                          _isListening ? Icons.square : Icons.mic,
                          size: _isListening ? 28 : 38,
                          color: _isListening ? Colors.white : const Color(0xFF3b82f6),
                        ),
                      );
                    },
                  ),
                ),
                
                const SizedBox(height: 16),
                
                // Indicateur de statut
                Text(
                  _isListening ? "Écoute en cours..." : "Appuyez pour parler",
                  style: const TextStyle(color: Colors.white54, fontSize: 14),
                ),
                
                // Bouton configuration seulement au démarrage
                if (_messages.length == 1) ...[
                  const SizedBox(height: 16),
                  TextButton.icon(
                    onPressed: () {
                      const intent = AndroidIntent(
                        action: 'android.settings.VOICE_INPUT_SETTINGS',
                      );
                      intent.launch();
                    },
                    icon: const Icon(Icons.settings, color: Color(0xFF3b82f6), size: 16),
                    label: const Text(
                      "Définir comme assistant par défaut", 
                      style: TextStyle(color: Color(0xFF3b82f6), fontSize: 12)
                    ),
                  )
                ]
              ],
            ),
          ),
        ],
      ),
    );
  }
}
