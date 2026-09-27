import 'package:shared_preferences/shared_preferences.dart';

class ApiConfig {
  static const String _keyServerUrl = 'server_base_url';
  
  // URL par défaut sur Render Cloud (24h/24 dans le monde entier)
  static const String defaultBaseUrl = 'https://koras.onrender.com/api/v1';

  /// Récupère l'URL de base actuelle
  static Future<String> getBaseUrl() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_keyServerUrl) ?? defaultBaseUrl;
  }

  /// Sauvegarde une nouvelle URL de base (nettoie les barres obliques et ajoute /api/v1 si nécessaire)
  static Future<String> setBaseUrl(String inputUrl) async {
    String cleanUrl = inputUrl.trim();
    if (cleanUrl.endsWith('/')) {
      cleanUrl = cleanUrl.substring(0, cleanUrl.length - 1);
    }
    
    // Si l'utilisateur a entré une URL sans /api/v1 (ex: https://xyz.trycloudflare.com), on l'ajoute
    if (!cleanUrl.endsWith('/api/v1')) {
      cleanUrl = '$cleanUrl/api/v1';
    }

    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keyServerUrl, cleanUrl);
    return cleanUrl;
  }
}
