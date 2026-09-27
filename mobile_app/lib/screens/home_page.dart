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
import 'dart:ui';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../main.dart';
import '../config/api_config.dart';

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

  List<Map<String, String>> _messages = [
    {"sender": "assistant", "text": "Bonjour ! Je suis Koras. Que puis-je faire pour vous aujourd'hui ?"}
  ];

  String _baseUrl = ApiConfig.defaultBaseUrl;
  static const platform = MethodChannel('com.koras.assistant/intent');
  
  // Par défaut, false. Si c'est lancé via le bouton home, ça passera à true
  bool _isAssistantMode = false; 

  @override
  void initState() {
    super.initState();
    _loadServerUrl();
    _checkLaunchMode();
    _loadMessages();

    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1000),
    )..repeat(reverse: true);
    
    _initSpeech();
    _initTts();
  }

  Future<void> _loadServerUrl() async {
    final url = await ApiConfig.getBaseUrl();
    if (mounted) {
      setState(() {
        _baseUrl = url;
      });
    }
  }

  void _showServerSettingsDialog() {
    final controller = TextEditingController(text: _baseUrl);
    showDialog(
      context: context,
      builder: (ctx) => BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 12, sigmaY: 12),
        child: AlertDialog(
          backgroundColor: const Color(0xFF131127).withValues(alpha: 0.88),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(24),
            side: BorderSide(color: const Color(0xFF8B5CF6).withValues(alpha: 0.4), width: 1.5),
          ),
          title: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: const Color(0xFF8B5CF6).withValues(alpha: 0.2),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Icon(Icons.dns_rounded, color: Color(0xFF10B981), size: 22),
              ),
              const SizedBox(width: 12),
              const Text('Serveur Backend', style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
            ],
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Adresse active du serveur Koras :',
                style: TextStyle(color: Colors.white70, fontSize: 13),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: controller,
                style: const TextStyle(color: Colors.white, fontSize: 14),
                decoration: InputDecoration(
                  hintText: 'https://xxx.trycloudflare.com',
                  hintStyle: const TextStyle(color: Colors.white30),
                  filled: true,
                  fillColor: Colors.black.withValues(alpha: 0.4),
                  contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(14),
                    borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.15)),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(14),
                    borderSide: const BorderSide(color: Color(0xFF10B981), width: 1.5),
                  ),
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Annuler', style: TextStyle(color: Colors.white60)),
            ),
            ElevatedButton(
              onPressed: () async {
                final newUrl = await ApiConfig.setBaseUrl(controller.text);
                if (mounted) {
                  setState(() {
                    _baseUrl = newUrl;
                  });
                }
                Navigator.pop(ctx);
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text('Serveur mis à jour : $newUrl'),
                    backgroundColor: const Color(0xFF10B981),
                    behavior: SnackBarBehavior.floating,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                );
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF10B981),
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
              child: const Text('Enregistrer', style: TextStyle(fontWeight: FontWeight.bold)),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _loadMessages() async {
    final prefs = await SharedPreferences.getInstance();
    final String? messagesJson = prefs.getString('saved_messages');
    if (messagesJson != null) {
      try {
        final List<dynamic> decoded = jsonDecode(messagesJson);
        setState(() {
          _messages = decoded.map((e) => Map<String, String>.from(e)).toList();
        });
      } catch (e) {
        // En cas d'erreur de décodage
      }
    } else {
      // Parle pour souhaiter la bienvenue uniquement la toute première fois
      await Future.delayed(const Duration(milliseconds: 500));
      await _flutterTts.speak("Bonjour ! Je suis Koras. Que puis-je faire pour vous aujourd'hui ?");
    }
  }

  Future<void> _saveMessages() async {
    final prefs = await SharedPreferences.getInstance();
    final String encoded = jsonEncode(_messages);
    await prefs.setString('saved_messages', encoded);
  }

  Future<void> _checkLaunchMode() async {
    try {
      final bool isAssistant = await platform.invokeMethod('isAssistantMode') ?? false;
      if (mounted) {
        setState(() {
          _isAssistantMode = isAssistant;
        });
        
        // Si c'est lancé via le bouton home, on écoute automatiquement après une micro pause
        if (_isAssistantMode) {
          Future.delayed(const Duration(milliseconds: 400), () {
            if (!_isListening) {
              _startListening();
            }
          });
        }
      }
    } on PlatformException {
      // Par défaut reste en mode normal
    }
  }

  Future<void> _initTts() async {
    await _flutterTts.setLanguage("fr-FR");
    await _flutterTts.setPitch(1.0);
    await _flutterTts.setSpeechRate(0.9);
  }

  Future<void> _initSpeech() async {
    bool available = await _speech.initialize(
      onError: (val) {
        if (mounted) {
          setState(() => _isListening = false);
        }
      },
      onStatus: (val) {
        if (val == 'done' || val == 'notListening') {
          if (mounted) {
            setState(() => _isListening = false);
          }
        }
      },
    );
    if (mounted) {
      setState(() => _speechAvailable = available);
    }
  }

  void _toggleListening() {
    if (_isListening) {
      _stopListening();
    } else {
      _startListening();
    }
  }

  void _startListening() async {
    var status = await Permission.microphone.status;
    if (!status.isGranted) {
      status = await Permission.microphone.request();
      if (!status.isGranted) return;
    }

    if (_speechAvailable) {
      setState(() {
        _isListening = true;
        _currentWords = "";
      });
      
      _speech.listen(
        onResult: (val) {
          setState(() {
            _currentWords = val.recognizedWords;
          });
          if (val.finalResult && _currentWords.isNotEmpty) {
            _handleUserSpeech(_currentWords);
          }
        },
        listenOptions: stt.SpeechListenOptions(
          partialResults: true,
          cancelOnError: false,
          pauseFor: const Duration(seconds: 3),
        ),
      );
    }
  }

  void _stopListening() async {
    await _speech.stop();
    setState(() => _isListening = false);
    if (_currentWords.isNotEmpty) {
      _handleUserSpeech(_currentWords);
    }
  }

  void _handleUserSpeech(String text) {
    setState(() {
      _messages.add({"sender": "user", "text": text});
      _currentWords = "";
      _isListening = false;
    });
    _saveMessages();
    _processWithBackend(text);
  }

  Future<String?> _findPhoneNumber(String contactName) async {
    try {
      final status = await fc.FlutterContacts.permissions.request(fc.PermissionType.read);
      if (status == fc.PermissionStatus.granted || status == fc.PermissionStatus.limited) {
        final contacts = await fc.FlutterContacts.getAll(properties: fc.ContactProperties.allProperties);
        final cleanSearchName = contactName.toLowerCase().trim();
        for (var contact in contacts) {
          final displayName = (contact.displayName ?? '').toLowerCase().trim();
          if (displayName.contains(cleanSearchName) || cleanSearchName.contains(displayName)) {
            if (contact.phones.isNotEmpty) {
              return contact.phones.first.number;
            }
          }
        }
      }
    } catch (_) {}
    return null;
  }

  Future<void> _launchIntentUrl(String url) async {
    final uri = Uri.parse(url);
    if (await canLaunchUrl(uri)) {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    }
  }

  Future<String> _openInstalledApp(String appName) async {
    try {
      final cleanName = appName.toLowerCase().trim();
      List<Application> apps = await DeviceApps.getInstalledApplications(
        includeSystemApps: true,
        onlyAppsWithLaunchIntent: true,
      );
          
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
      // Si en mode démo autonome direct, on exécute en local immédiatement
      if (widget.token == 'demo_autonomous_token') {
        await _processOfflineAutonomous(text);
        return;
      }

      final baseUrl = await ApiConfig.getBaseUrl();
      
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
      ).timeout(const Duration(seconds: 4));
      
      if (interpretRes.statusCode != 200) {
        if (interpretRes.statusCode == 500 || interpretRes.statusCode == 400) {
          if (mounted) {
            setState(() {
              _messages.add({"sender": "assistant", "text": "Désolé, je n'ai pas compris votre demande. Pourriez-vous répéter ?"});
            });
            await _flutterTts.speak("Désolé, je n'ai pas compris votre demande.");
          }
          return;
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
      ).timeout(const Duration(seconds: 4));
      
      if (executeRes.statusCode != 200) {
        throw Exception("Erreur Execution: Code ${executeRes.statusCode}");
      }
      
      // 3. Déterminer la réponse à vocaliser
      String responseText = "Action exécutée avec succès.";
      final typeIntention = intention['type'];
      
      // Exécution de l'action réelle sur le téléphone
      switch(typeIntention) {
        case 'METEO':
          responseText = "D'après mes informations, le temps est dégagé avec une température agréable.";
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
          final contactName = intention['entites']?['contact']?['nom'] ?? 'ce contact';
          final messageContent = intention['entites']?['message']?['contenu'] ?? '';
          String? numberToSend = await _findPhoneNumber(contactName);
          
          if (numberToSend != null) {
            responseText = "Préparation du SMS pour $contactName.";
            final intent = AndroidIntent(
              action: 'android.intent.action.SENDTO',
              data: 'smsto:$numberToSend',
              arguments: {
                'sms_body': messageContent,
              },
            );
            await intent.launch();
          } else {
            responseText = "Je n'ai pas trouvé le contact $contactName pour envoyer le SMS.";
          }
          break;
        case 'MESSAGE_WHATSAPP':
          final contactName = intention['entites']?['destinataire']?['valeur'] ?? '';
          final messageContent = intention['entites']?['message']?['contenu'] ?? '';
          
          if (contactName.isNotEmpty) {
            String? phoneNumber = await _findPhoneNumber(contactName);
            if (phoneNumber != null) {
              final cleanPhone = phoneNumber.replaceAll(RegExp(r'[^0-9+]'), '');
              final encodedMsg = Uri.encodeComponent(messageContent);
              responseText = "Ouverture de la discussion WhatsApp avec $contactName.";
              await _launchIntentUrl('https://wa.me/$cleanPhone?text=$encodedMsg');
            } else {
              final encodedMsg = Uri.encodeComponent(messageContent);
              responseText = "Je n'ai pas trouvé le numéro de $contactName. Ouverture de WhatsApp.";
              await _launchIntentUrl('https://wa.me/?text=$encodedMsg');
            }
          } else {
            responseText = "Ouverture de WhatsApp.";
            await _launchIntentUrl('whatsapp://');
          }
          break;
        case 'OUVERTURE_APP':
          final appName = intention['entites']?['application']?['nom'] ?? '';
          responseText = await _openInstalledApp(appName);
          break;
        case 'RECHERCHE_WEB':
          final query = intention['entites']?['requete']?['contenu'] ?? text;
          responseText = "Voici ce que j'ai trouvé sur le web concernant votre recherche.";
          _launchIntentUrl('https://www.google.com/search?q=${Uri.encodeComponent(query)}');
          break;
        case 'RECHERCHE_YOUTUBE':
          final query = intention['entites']?['requete']?['contenu'] ?? text;
          responseText = "Voici les résultats sur YouTube.";
          _launchIntentUrl('https://www.youtube.com/results?search_query=${Uri.encodeComponent(query)}');
          break;
        default:
          responseText = "J'ai bien compris votre demande concernant : $typeIntention.";
      }
      
      if (mounted) {
        setState(() {
          _messages.add({"sender": "assistant", "text": responseText});
        });
        _saveMessages();
      }
      
      await _flutterTts.speak(responseText);
      
    } catch (e) {
      // Fallback local automatique : l'application reste 100% fonctionnelle même sans serveur
      await _processOfflineAutonomous(text);
    }
  }

  // Moteur d'exécution autonome sur smartphone (Zéro dépendance serveur)
  Future<void> _processOfflineAutonomous(String text) async {
    final lower = text.toLowerCase().trim();
    String responseText = "J'ai bien compris votre demande.";

    // 1. YouTube (Ouvrir ou Rechercher)
    if (lower.contains('youtube')) {
      final reg1 = RegExp(r'(?:cherche|recherche|trouve|joue|mets|lance|regarde)\s+(.+?)(?:\s+sur\s+youtube|$)', caseSensitive: false);
      final reg2 = RegExp(r'youtube\s+(?:et\s+)?(?:cherche|recherche)\s+(.+)', caseSensitive: false);
      String query = '';
      if (reg1.hasMatch(lower)) {
        query = reg1.firstMatch(lower)!.group(1) ?? '';
      } else if (reg2.hasMatch(lower)) {
        query = reg2.firstMatch(lower)!.group(1) ?? '';
      }
      query = query.replaceAll(RegExp(r'\bsur\s+youtube\b', caseSensitive: false), '').trim();
      
      if (query.isNotEmpty) {
        responseText = "Recherche de $query sur YouTube.";
        await _launchIntentUrl('https://www.youtube.com/results?search_query=${Uri.encodeComponent(query)}');
      } else {
        responseText = "Ouverture de YouTube.";
        await _launchIntentUrl('https://www.youtube.com');
      }
    }
    // 2. WhatsApp (Envoi direct avec destinataire et message)
    else if (lower.contains('whatsapp')) {
      final reg = RegExp(r'(?:envoie|envoyer|ecris|écris|message|dis)\s+(?:un\s+message\s+)?(?:à|a)\s+(.+?)(?:\s+(?:que|pour\s+dire\s+que|sur\s+whatsapp\s+que|:)\s+(.+)|$)', caseSensitive: false);
      String contact = '';
      String msg = '';
      if (reg.hasMatch(lower)) {
        final match = reg.firstMatch(lower)!;
        contact = (match.group(1) ?? '').trim().replaceAll(RegExp(r'\bsur\s+whatsapp\b', caseSensitive: false), '').trim();
        msg = (match.group(2) ?? '').trim();
      }
      
      if (contact.isNotEmpty) {
        String? phone = await _findPhoneNumber(contact);
        if (phone != null) {
          final cleanPhone = phone.replaceAll(RegExp(r'[^0-9+]'), '');
          responseText = "Ouverture de WhatsApp pour envoyer votre message à $contact.";
          await _launchIntentUrl('https://wa.me/$cleanPhone?text=${Uri.encodeComponent(msg)}');
        } else {
          responseText = "Ouverture de WhatsApp avec votre message.";
          await _launchIntentUrl('https://wa.me/?text=${Uri.encodeComponent(msg)}');
        }
      } else {
        responseText = "Ouverture de WhatsApp.";
        await _launchIntentUrl('whatsapp://');
      }
    }
    // 3. Appels téléphoniques
    else if (lower.startsWith('appelle') || lower.startsWith('appeler') || lower.startsWith('téléphone')) {
      final reg = RegExp(r'(?:appelle|appeler|téléphone\s+à|telephone\s+a)\s+(.+)', caseSensitive: false);
      String contact = reg.firstMatch(lower)?.group(1)?.trim() ?? '';
      if (contact.isNotEmpty) {
        String? number = await _findPhoneNumber(contact);
        if (number != null) {
          var phoneStatus = await Permission.phone.status;
          if (!phoneStatus.isGranted) phoneStatus = await Permission.phone.request();
          if (phoneStatus.isGranted) {
            responseText = "Appel en cours vers $contact.";
            final intent = AndroidIntent(action: 'android.intent.action.CALL', data: 'tel:$number');
            await intent.launch();
          } else {
            responseText = "Autorisation d'appel manquante.";
          }
        } else {
          responseText = "Je n'ai pas trouvé le numéro de $contact dans vos contacts.";
        }
      }
    }
    // 4. SMS
    else if (lower.startsWith('sms') || lower.contains('par sms')) {
      final reg = RegExp(r'(?:envoie|envoyer)\s+(?:un\s+)?sms\s+(?:à|a)\s+(.+?)(?:\s+(?:que|:)\s+(.+)|$)', caseSensitive: false);
      if (reg.hasMatch(lower)) {
        final contact = reg.firstMatch(lower)!.group(1)?.trim() ?? '';
        final msg = reg.firstMatch(lower)!.group(2)?.trim() ?? '';
        String? phone = await _findPhoneNumber(contact);
        if (phone != null) {
          responseText = "Préparation du SMS pour $contact.";
          final intent = AndroidIntent(action: 'android.intent.action.SENDTO', data: 'smsto:$phone', arguments: {'sms_body': msg});
          await intent.launch();
        }
      }
    }
    // 5. Ouverture d'Application
    else if (lower.startsWith('ouvre') || lower.startsWith('ouvrir') || lower.startsWith('lance')) {
      final reg = RegExp(r'(?:ouvre|ouvrir|lance|lancer)\s+(?:l\s*application\s+|l\s*appli\s+)?(.+)', caseSensitive: false);
      final appName = reg.firstMatch(lower)?.group(1)?.trim() ?? '';
      if (appName.isNotEmpty) {
        final res = await _openInstalledApp(appName);
        if (res == "SUCCESS") {
          responseText = "Ouverture de $appName.";
        } else {
          responseText = "Je n'ai pas trouvé l'application $appName.";
        }
      }
    }
    // 6. Heure courante
    else if (lower.contains('heure')) {
      final now = DateTime.now();
      responseText = "Il est actuellement ${now.hour} heure${now.hour > 1 ? 's' : ''} et ${now.minute.toString().padLeft(2, '0')}.";
    }
    // 7. Identité / Présentation
    else if (lower.contains('qui es-tu') || lower.contains('ton nom') || lower.contains('qui t\'a créé') || lower.contains('bonjour')) {
      responseText = "Bonjour ! Je suis Koras, votre assistant vocal accessible et intelligent de nouvelle génération.";
    }
    // 8. Recherche Web par défaut
    else {
      responseText = "Recherche sur le web concernant : $text";
      await _launchIntentUrl('https://www.google.com/search?q=${Uri.encodeComponent(text)}');
    }

    if (mounted) {
      setState(() {
        _messages.add({"sender": "assistant", "text": responseText});
      });
      _saveMessages();
    }
    await _flutterTts.speak(responseText);
  }

  @override
  Widget build(BuildContext context) {
    if (_isAssistantMode) {
      return _buildAssistantBubbleView(context);
    } else {
      return _buildFullAppView(context);
    }
  }

  // VUE APPLICATION COMPLÈTE (Thème Sombre / Mauve / Vert Émeraude / Verre Dépoli)
  Widget _buildFullAppView(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF070B14),
      extendBodyBehindAppBar: true,
      appBar: PreferredSize(
        preferredSize: const Size.fromHeight(60),
        child: ClipRRect(
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 16, sigmaY: 16),
            child: AppBar(
              backgroundColor: const Color(0xFF0D1224).withValues(alpha: 0.65),
              elevation: 0,
              title: Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(2),
                    decoration: const BoxDecoration(
                      shape: BoxShape.circle,
                      gradient: LinearGradient(colors: [Color(0xFF8B5CF6), Color(0xFF10B981)]),
                    ),
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(20),
                      child: Image.asset('assets/icon.jpg', width: 28, height: 28, fit: BoxFit.cover),
                    ),
                  ),
                  const SizedBox(width: 10),
                  ShaderMask(
                    shaderCallback: (bounds) => const LinearGradient(
                      colors: [Color(0xFFC084FC), Color(0xFF34D399)],
                    ).createShader(bounds),
                    child: const Text(
                      'KORAS',
                      style: TextStyle(fontWeight: FontWeight.w900, color: Colors.white, fontSize: 20, letterSpacing: 1.5),
                    ),
                  ),
                ],
              ),
              actions: [
                IconButton(
                  icon: const Icon(Icons.tune_rounded, color: Color(0xFF34D399)),
                  tooltip: 'Paramètres Serveur',
                  onPressed: _showServerSettingsDialog,
                ),
                IconButton(
                  icon: const Icon(Icons.delete_sweep_rounded, color: Colors.white70),
                  tooltip: 'Effacer l\'historique',
                  onPressed: () {
                    setState(() {
                      _messages = [
                        {"sender": "assistant", "text": "Bonjour ! Je suis Koras. Que puis-je faire pour vous aujourd'hui ?"}
                      ];
                    });
                    _saveMessages();
                  },
                ),
                IconButton(
                  icon: const Icon(Icons.logout_rounded, color: Colors.white70),
                  tooltip: 'Déconnexion',
                  onPressed: () async {
                    final prefs = await SharedPreferences.getInstance();
                    await prefs.remove('auth_token');
                    if (mounted) {
                      Navigator.of(context).pushReplacement(
                        MaterialPageRoute(builder: (context) => const LoginPage()),
                      );
                    }
                  },
                )
              ],
            ),
          ),
        ),
      ),
      body: Stack(
        children: [
          // Halos lumineux en fond
          Positioned(
            top: 40,
            right: -60,
            child: Container(
              width: 260,
              height: 260,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [const Color(0xFF7C3AED).withValues(alpha: 0.2), Colors.transparent],
                ),
              ),
            ),
          ),
          Positioned(
            bottom: 120,
            left: -60,
            child: Container(
              width: 280,
              height: 280,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [const Color(0xFF059669).withValues(alpha: 0.2), Colors.transparent],
                ),
              ),
            ),
          ),

          SafeArea(
            child: Column(
              children: [
                // Bannière Verre Dépoli pour activer l'assistant par défaut
                Container(
                  margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.05),
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(color: const Color(0xFF8B5CF6).withValues(alpha: 0.3)),
                  ),
                  child: Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(8),
                        decoration: BoxDecoration(
                          color: const Color(0xFF8B5CF6).withValues(alpha: 0.2),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: const Icon(Icons.touch_app_rounded, color: Color(0xFFC084FC), size: 22),
                      ),
                      const SizedBox(width: 12),
                      const Expanded(
                        child: Text(
                          "Activez Koras via le bouton d'accueil pour la bulle flottante instantanée.",
                          style: TextStyle(color: Colors.white, fontSize: 12.5),
                        ),
                      ),
                      const SizedBox(width: 8),
                      ElevatedButton(
                        onPressed: () {
                          const intent = AndroidIntent(action: 'android.settings.VOICE_INPUT_SETTINGS');
                          intent.launch();
                        },
                        style: ElevatedButton.styleFrom(
                          backgroundColor: const Color(0xFF10B981),
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                        ),
                        child: const Text("Activer", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12)),
                      ),
                    ],
                  ),
                ),

                // Liste des messages de conversation
                Expanded(
                  child: ListView.builder(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                    itemCount: _messages.length,
                    itemBuilder: (context, index) {
                      final msg = _messages[index];
                      final isUser = msg["sender"] == "user";
                      return Align(
                        alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
                        child: Container(
                          margin: const EdgeInsets.only(bottom: 14),
                          constraints: BoxConstraints(
                            maxWidth: MediaQuery.of(context).size.width * 0.78,
                          ),
                          padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 13),
                          decoration: BoxDecoration(
                            gradient: isUser
                                ? const LinearGradient(
                                    colors: [Color(0xFF7C3AED), Color(0xFF6D28D9)],
                                    begin: Alignment.topLeft,
                                    end: Alignment.bottomRight,
                                  )
                                : null,
                            color: isUser ? null : Colors.white.withValues(alpha: 0.07),
                            borderRadius: BorderRadius.circular(22).copyWith(
                              bottomRight: isUser ? const Radius.circular(4) : const Radius.circular(22),
                              bottomLeft: !isUser ? const Radius.circular(4) : const Radius.circular(22),
                            ),
                            border: Border.all(
                              color: isUser
                                  ? const Color(0xFFA855F7).withValues(alpha: 0.5)
                                  : const Color(0xFF10B981).withValues(alpha: 0.35),
                              width: 1.2,
                            ),
                            boxShadow: [
                              BoxShadow(
                                color: isUser
                                    ? const Color(0xFF7C3AED).withValues(alpha: 0.25)
                                    : Colors.black.withValues(alpha: 0.2),
                                blurRadius: 12,
                                offset: const Offset(0, 4),
                              ),
                            ],
                          ),
                          child: Text(
                            msg["text"]!,
                            style: const TextStyle(color: Colors.white, fontSize: 15, height: 1.4),
                          ),
                        ),
                      );
                    },
                  ),
                ),
                
                // Retranscription en direct de ce qui est entendu
                if (_currentWords.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 6.0),
                    child: Text(
                      '"$_currentWords..."',
                      style: const TextStyle(color: Color(0xFF34D399), fontStyle: FontStyle.italic, fontSize: 15),
                      textAlign: TextAlign.center,
                    ),
                  ),
                
                // Zone du microphone animée (Halo Mauve & Vert Émeraude)
                Container(
                  padding: const EdgeInsets.only(top: 14, bottom: 28),
                  child: Center(
                    child: GestureDetector(
                      onTap: _toggleListening,
                      child: AnimatedBuilder(
                        animation: _pulseController,
                        builder: (context, child) {
                          return Container(
                            width: 90,
                            height: 90,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              gradient: const LinearGradient(
                                colors: [Color(0xFF7C3AED), Color(0xFF059669)],
                                begin: Alignment.topLeft,
                                end: Alignment.bottomRight,
                              ),
                              boxShadow: _isListening ? [
                                BoxShadow(
                                  color: const Color(0xFF8B5CF6).withValues(alpha: 0.6),
                                  blurRadius: 28 * _pulseController.value,
                                  spreadRadius: 12 * _pulseController.value,
                                ),
                                BoxShadow(
                                  color: const Color(0xFF10B981).withValues(alpha: 0.5),
                                  blurRadius: 20 * _pulseController.value,
                                  spreadRadius: 6 * _pulseController.value,
                                ),
                              ] : [
                                BoxShadow(
                                  color: const Color(0xFF7C3AED).withValues(alpha: 0.35),
                                  blurRadius: 16,
                                  offset: const Offset(0, 4),
                                )
                              ],
                              border: Border.all(
                                color: Colors.white.withValues(alpha: 0.3),
                                width: 2,
                              ),
                            ),
                            child: Icon(
                              _isListening ? Icons.square_rounded : Icons.mic_rounded,
                              size: _isListening ? 26 : 38,
                              color: Colors.white,
                            ),
                          );
                        },
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // VUE BULLE FLOTTANTE TRANSPARENTE TYPE GEMINI (Invoquée via le bouton Home)
  Widget _buildAssistantBubbleView(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.transparent, // Transparence totale pour laisser voir l'écran arrière
      body: Column(
        mainAxisAlignment: MainAxisAlignment.end,
        children: [
          // Espace tactile transparent supérieur pour fermer l'overlay
          Expanded(
            child: GestureDetector(
              onTap: () {
                SystemNavigator.pop();
              },
              behavior: HitTestBehavior.opaque,
            ),
          ),
          
          // Feuille Flottante en Verre Dépoli Translucide
          ClipRRect(
            borderRadius: const BorderRadius.only(
              topLeft: Radius.circular(36),
              topRight: Radius.circular(36),
            ),
            child: BackdropFilter(
              filter: ImageFilter.blur(sigmaX: 24, sigmaY: 24),
              child: Container(
                padding: const EdgeInsets.only(top: 16, left: 24, right: 24, bottom: 34),
                decoration: BoxDecoration(
                  color: const Color(0xFF090D18).withValues(alpha: 0.88),
                  borderRadius: const BorderRadius.only(
                    topLeft: Radius.circular(36),
                    topRight: Radius.circular(36),
                  ),
                  border: Border(
                    top: BorderSide(color: const Color(0xFF8B5CF6).withValues(alpha: 0.5), width: 1.5),
                    left: BorderSide(color: Colors.white.withValues(alpha: 0.1), width: 1),
                    right: BorderSide(color: Colors.white.withValues(alpha: 0.1), width: 1),
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: const Color(0xFF10B981).withValues(alpha: 0.18),
                      blurRadius: 40,
                      offset: const Offset(0, -10),
                    )
                  ],
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    // Barre de poignée (Handle)
                    Container(
                      width: 44,
                      height: 4,
                      margin: const EdgeInsets.only(bottom: 20),
                      decoration: BoxDecoration(
                        color: Colors.white.withValues(alpha: 0.3),
                        borderRadius: BorderRadius.circular(10),
                      ),
                    ),
                    
                    // Réponse ou message d'attente
                    Text(
                      _messages.last["text"] ?? "Je suis à votre écoute...",
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 17,
                        fontWeight: FontWeight.w600,
                        height: 1.4,
                      ),
                      textAlign: TextAlign.center,
                    ),
                    
                    const SizedBox(height: 20),
                    
                    // Parole détectée en temps réel
                    if (_currentWords.isNotEmpty)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 20.0),
                        child: Text(
                          '"$_currentWords..."',
                          style: const TextStyle(color: Color(0xFF34D399), fontStyle: FontStyle.italic, fontSize: 16),
                          textAlign: TextAlign.center,
                        ),
                      ),
                    
                    // Bouton Microphone Lumineux (Dégradé Mauve -> Vert)
                    GestureDetector(
                      onTap: _toggleListening,
                      child: AnimatedBuilder(
                        animation: _pulseController,
                        builder: (context, child) {
                          return Container(
                            width: 78,
                            height: 78,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              gradient: const LinearGradient(
                                colors: [Color(0xFF7C3AED), Color(0xFF059669)],
                                begin: Alignment.topLeft,
                                end: Alignment.bottomRight,
                              ),
                              boxShadow: _isListening ? [
                                BoxShadow(
                                  color: const Color(0xFF8B5CF6).withValues(alpha: 0.6),
                                  blurRadius: 26 * _pulseController.value,
                                  spreadRadius: 10 * _pulseController.value,
                                ),
                                BoxShadow(
                                  color: const Color(0xFF10B981).withValues(alpha: 0.5),
                                  blurRadius: 18 * _pulseController.value,
                                  spreadRadius: 5 * _pulseController.value,
                                ),
                              ] : [
                                BoxShadow(
                                  color: const Color(0xFF7C3AED).withValues(alpha: 0.3),
                                  blurRadius: 14,
                                  offset: const Offset(0, 4),
                                )
                              ],
                              border: Border.all(
                                color: Colors.white.withValues(alpha: 0.3),
                                width: 2,
                              ),
                            ),
                            child: Icon(
                              _isListening ? Icons.square_rounded : Icons.mic_rounded,
                              size: _isListening ? 24 : 36,
                              color: Colors.white,
                            ),
                          );
                        },
                      ),
                    ),
                    
                    const SizedBox(height: 14),
                    
                    // Statut d'écoute
                    Text(
                      _isListening ? "Écoute en cours (silence auto-stop)..." : "Appuyez pour parler",
                      style: TextStyle(
                        color: _isListening ? const Color(0xFF34D399) : Colors.white54,
                        fontSize: 13,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
