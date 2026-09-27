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
    // Toujours commencer par le moteur local ultra-rapide pour les commandes device
    _smartProcess(text);
  }

  // Routeur intelligent : local pour commandes simples, serveur pour requêtes complexes
  Future<void> _smartProcess(String text) async {
    final lower = text.toLowerCase().trim();

    // Commandes purement locales (Zéro latence serveur)
    final bool isLocalCommand =
        lower.contains('whatsapp') ||
        lower.contains('youtube') ||
        RegExp(r'\b(appelle|appeler|appel|téléphone|telephone|contacte)\b').hasMatch(lower) ||
        RegExp(r'\b(sms|texto|message)\b').hasMatch(lower) ||
        RegExp(r'\b(ouvre|ouvrir|ouvres|ouvrez|lance|lancer|démarre|demarrer|affiche|montre|démarre|start|ouvre-moi|mets|mettre)\b').hasMatch(lower) ||
        RegExp(r'\b(heure|quelle heure|il est quelle)\b').hasMatch(lower) ||
        RegExp(r'\b(qui es-tu|tu es qui|ton nom|qui t.a créé|bonjour|salut)\b').hasMatch(lower);

    if (isLocalCommand || widget.token == 'demo_autonomous_token') {
      // Exécution instantanée sans passer par le réseau
      await _processOfflineAutonomous(text);
      return;
    }

    // Pour les requêtes complexes (météo, calculs, etc.) → serveur
    await _processWithBackend(text);
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
      final baseUrl = await ApiConfig.getBaseUrl();
      
      // Appel NLU avec timeout généreux pour requêtes complexes
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
      ).timeout(const Duration(seconds: 8));
      
      if (interpretRes.statusCode != 200) {
        throw Exception("Erreur NLU: Code ${interpretRes.statusCode}");
      }
      
      final interpretData = jsonDecode(interpretRes.body);
      final intention = interpretData['intention'];
      final typeIntention = intention['type'];
      String responseText = "Action exécutée avec succès.";

      switch(typeIntention) {
        case 'METEO':
          responseText = "D'après mes informations, le temps est dégagé avec une température agréable.";
          _launchIntentUrl('https://weather.com/fr-FR/temps/aujour/l/FRXX0076');
          break;
        default:
          responseText = "J'ai bien compris votre demande. Traitement en cours.";
      }
      
      if (mounted) {
        setState(() {
          _messages.add({"sender": "assistant", "text": responseText});
        });
        _saveMessages();
      }
      await _flutterTts.speak(responseText);
      
    } catch (e) {
      // Serveur inaccessible → moteur local
      await _processOfflineAutonomous(text);
    }
  }

  // Moteur NLU local ultra-rapide (0ms latence, 100% hors-ligne)
  Future<void> _processOfflineAutonomous(String text) async {
    final lower = text.toLowerCase().trim();
    String responseText = "J'ai bien compris votre demande.";

    // Noms d'applications courants → package Android exact
    final Map<String, String> appPackages = {
      'whatsapp': 'com.whatsapp',
      'instagram': 'com.instagram.android',
      'facebook': 'com.facebook.katana',
      'twitter': 'com.twitter.android',
      'x': 'com.twitter.android',
      'tiktok': 'com.zhiliaoapp.musically',
      'snapchat': 'com.snapchat.android',
      'telegram': 'org.telegram.messenger',
      'youtube': 'com.google.android.youtube',
      'gmail': 'com.google.android.gm',
      'maps': 'com.google.android.apps.maps',
      'google maps': 'com.google.android.apps.maps',
      'spotify': 'com.spotify.music',
      'netflix': 'com.netflix.mediaclient',
      'chrome': 'com.android.chrome',
      'appareil photo': 'com.android.camera2',
      'camera': 'com.android.camera2',
      'galerie': 'com.google.android.apps.photos',
      'photos': 'com.google.android.apps.photos',
      'paramètres': 'com.android.settings',
      'settings': 'com.android.settings',
      'calculatrice': 'com.google.android.calculator',
      'calendrier': 'com.google.android.calendar',
    };

    // ── 1. YOUTUBE ─────────────────────────────────────────────────────────────
    if (lower.contains('youtube')) {
      final queryReg = RegExp(
        r'(?:cherche|recherche|trouve|joue|mets|lance|regarde|montre)\s+(.+?)(?:\s+sur\s+youtube|$)',
        caseSensitive: false,
      );
      final queryReg2 = RegExp(
        r'youtube\s+(?:et\s+)?(?:cherche|recherche|pour)\s+(.+)',
        caseSensitive: false,
      );
      String query = '';
      if (queryReg.hasMatch(lower)) {
        query = (queryReg.firstMatch(lower)!.group(1) ?? '').replaceAll(RegExp(r'\bsur\s+youtube\b', caseSensitive: false), '').trim();
      } else if (queryReg2.hasMatch(lower)) {
        query = (queryReg2.firstMatch(lower)!.group(1) ?? '').trim();
      }

      if (query.isNotEmpty) {
        responseText = "Recherche de $query sur YouTube.";
        await _launchIntentUrl('https://www.youtube.com/results?search_query=${Uri.encodeComponent(query)}');
      } else {
        responseText = "Ouverture de YouTube.";
        final launched = await DeviceApps.openApp('com.google.android.youtube');
        if (!launched) await _launchIntentUrl('https://www.youtube.com');
      }
    }

    // ── 2. WHATSAPP ────────────────────────────────────────────────────────────
    else if (lower.contains('whatsapp')) {
      final msgReg = RegExp(
        r'(?:envoie|envoyer|écris|ecris|dis|envoie-lui|envoie lui)\s+(?:un\s+)?(?:message\s+)?(?:à|a|pour)\s+(.+?)(?:\s+(?:que|pour\s+dire|:\s*|sur\s+whatsapp\s*:?\s*)\s*(.+)|$)',
        caseSensitive: false,
      );
      final contactReg = RegExp(
        r'(?:ouvre|ouvrir|lance|appelle|contacte|message)\s+(?:\w+\s+)?(?:à|a)?\s*(.+?)\s+(?:sur|via)\s+whatsapp',
        caseSensitive: false,
      );
      String contact = '';
      String msg = '';

      if (msgReg.hasMatch(lower)) {
        final m = msgReg.firstMatch(lower)!;
        contact = (m.group(1) ?? '').replaceAll(RegExp(r'\bsur\s+whatsapp\b', caseSensitive: false), '').trim();
        msg = (m.group(2) ?? '').trim();
      } else if (contactReg.hasMatch(lower)) {
        contact = (contactReg.firstMatch(lower)!.group(1) ?? '').trim();
      }

      if (contact.isNotEmpty) {
        String? phone = await _findPhoneNumber(contact);
        if (phone != null) {
          final cleanPhone = phone.replaceAll(RegExp(r'[^0-9+]'), '');
          responseText = msg.isNotEmpty
            ? "Envoi du message WhatsApp à $contact."
            : "Ouverture de WhatsApp avec $contact.";
          await _launchIntentUrl('https://wa.me/$cleanPhone${msg.isNotEmpty ? '?text=${Uri.encodeComponent(msg)}' : ''}');
        } else {
          responseText = "Contact introuvable. Ouverture de WhatsApp.";
          await DeviceApps.openApp('com.whatsapp');
        }
      } else {
        responseText = "Ouverture de WhatsApp.";
        final launched = await DeviceApps.openApp('com.whatsapp');
        if (!launched) await _launchIntentUrl('whatsapp://');
      }
    }

    // ── 3. APPELS TÉLÉPHONIQUES ────────────────────────────────────────────────
    else if (RegExp(r'\b(appelle|appeler|appels|téléphone|telephone|contacte|appel)\b').hasMatch(lower)) {
      final reg = RegExp(
        r'(?:appelle|appeler|téléphone\s+à|telephone\s+a|contacte|appel\s+de)\s+(.+)',
        caseSensitive: false,
      );
      final contact = reg.firstMatch(lower)?.group(1)?.trim() ?? '';
      if (contact.isNotEmpty) {
        String? number = await _findPhoneNumber(contact);
        if (number != null) {
          var phoneStatus = await Permission.phone.status;
          if (!phoneStatus.isGranted) phoneStatus = await Permission.phone.request();
          if (phoneStatus.isGranted) {
            responseText = "Appel en cours vers $contact.";
            await AndroidIntent(action: 'android.intent.action.CALL', data: 'tel:$number').launch();
          } else {
            responseText = "Autorisation d'appel refusée. Ouverture du composeur.";
            await _launchIntentUrl('tel:$number');
          }
        } else {
          responseText = "Je n'ai pas trouvé le numéro de $contact dans vos contacts.";
        }
      } else {
        responseText = "Dites-moi qui vous souhaitez appeler.";
      }
    }

    // ── 4. SMS ─────────────────────────────────────────────────────────────────
    else if (RegExp(r'\b(sms|texto|envoie un message|par sms)\b').hasMatch(lower)) {
      final reg = RegExp(
        r'(?:envoie|envoyer|envoie un message)\s+(?:un\s+)?(?:sms|texto|message)\s+(?:à|a|pour)\s+(.+?)(?:\s+(?:que|:)\s+(.+)|$)',
        caseSensitive: false,
      );
      if (reg.hasMatch(lower)) {
        final contact = reg.firstMatch(lower)!.group(1)?.trim() ?? '';
        final msg = reg.firstMatch(lower)!.group(2)?.trim() ?? '';
        String? phone = await _findPhoneNumber(contact);
        if (phone != null) {
          responseText = "Préparation du SMS pour $contact.";
          await AndroidIntent(
            action: 'android.intent.action.SENDTO',
            data: 'smsto:$phone',
            arguments: {'sms_body': msg},
          ).launch();
        } else {
          responseText = "Je n'ai pas trouvé le contact $contact.";
        }
      } else {
        responseText = "Ouverture de l'application SMS.";
        await _launchIntentUrl('sms:');
      }
    }

    // ── 5. OUVERTURE D'APPLICATION ────────────────────────────────────────────
    else if (RegExp(r'\b(ouvre|ouvrir|ouvres|ouvrez|lance|lancer|démarre|demarrer|affiche|montre|démarre|start|mets|mettre|ouvre-moi)\b').hasMatch(lower)) {
      final reg = RegExp(
        r'(?:ouvre|ouvrir|ouvres|ouvrez|lance|lancer|démarre|demarrer|affiche|montre|start|mets|mettre|ouvre-moi)\s+(?:l[ae]?\s+)?(?:application\s+|appli(?:cation)?\s+|app\s+)?(.+)',
        caseSensitive: false,
      );
      String appName = reg.firstMatch(lower)?.group(1)?.trim() ?? '';
      // Nettoyer les mots parasites
      appName = appName
          .replaceAll(RegExp(r'\bsur\s+mon\s+téléphone\b', caseSensitive: false), '')
          .replaceAll(RegExp(r'\bpour\s+moi\b', caseSensitive: false), '')
          .replaceAll(RegExp(r'\bmaintenant\b', caseSensitive: false), '')
          .trim();

      if (appName.isNotEmpty) {
        // Essai 1 : package connu directement
        bool launched = false;
        for (final entry in appPackages.entries) {
          if (appName.contains(entry.key) || entry.key.contains(appName)) {
            launched = await DeviceApps.openApp(entry.value);
            if (launched) {
              responseText = "Ouverture de ${appName[0].toUpperCase()}${appName.substring(1)}.";
              break;
            }
          }
        }
        // Essai 2 : scan des apps installées
        if (!launched) {
          final res = await _openInstalledApp(appName);
          if (res == "SUCCESS") {
            responseText = "Ouverture de ${appName[0].toUpperCase()}${appName.substring(1)}.";
          } else {
            responseText = "Je n'ai pas trouvé l'application \"$appName\" sur votre téléphone.";
          }
        }
      } else {
        responseText = "Dites-moi quelle application vous souhaitez ouvrir.";
      }
    }

    // ── 6. HEURE ──────────────────────────────────────────────────────────────
    else if (RegExp(r'\b(heure|quelle heure|il est quelle|combien d.heure)\b').hasMatch(lower)) {
      final now = DateTime.now();
      responseText = "Il est ${now.hour}h${now.minute.toString().padLeft(2, '0')}.";
    }

    // ── 7. DATE ───────────────────────────────────────────────────────────────
    else if (RegExp(r'\b(date|quel jour|on est quel|aujourd.hui)\b').hasMatch(lower)) {
      final now = DateTime.now();
      final jours = ['lundi','mardi','mercredi','jeudi','vendredi','samedi','dimanche'];
      final mois = ['janvier','février','mars','avril','mai','juin','juillet','août','septembre','octobre','novembre','décembre'];
      responseText = "Nous sommes ${jours[now.weekday - 1]} ${now.day} ${mois[now.month - 1]} ${now.year}.";
    }

    // ── 8. MÉTÉO (locale simple) ───────────────────────────────────────────────
    else if (RegExp(r'\b(météo|meteo|temps qu.il fait|quel temps|température)\b').hasMatch(lower)) {
      responseText = "Ouverture de la météo pour votre localisation.";
      await _launchIntentUrl('https://weather.com/fr-FR/temps/aujour/');
    }

    // ── 9. IDENTITÉ ───────────────────────────────────────────────────────────
    else if (RegExp(r'\b(qui es.tu|tu es qui|ton nom|qui t.a créé|bonjour|salut|comment tu t.appelles)\b').hasMatch(lower)) {
      responseText = "Bonjour ! Je suis Koras, votre assistant vocal accessible et intelligent, conçu pour simplifier votre quotidien numérique.";
    }

    // ── 10. RECHERCHE WEB (par défaut — jamais Google si commande reconnue) ────
    else {
      final searchQuery = lower
          .replaceAll(RegExp(r'^(?:cherche|recherche|trouve|google)\s+', caseSensitive: false), '')
          .replaceAll(RegExp(r'\bsur\s+(?:google|le\s+web|internet)\b', caseSensitive: false), '')
          .trim();
      responseText = "Recherche en cours pour : $searchQuery.";
      await _launchIntentUrl('https://www.google.com/search?q=${Uri.encodeComponent(searchQuery)}');
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
