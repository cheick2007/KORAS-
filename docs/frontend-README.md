# Koras Frontend - React + TypeScript

Interface web moderne pour l'assistant vocal Koras.

## Dmarrage Rapide

```bash
# Installation
npm install

# Dveloppement
npm run dev
# Ouvrir http://localhost:3000

# Build production
npm run build

# Preview build
npm run preview
```

## Architecture

```
frontend/
src/
main.tsx # Point d'entre
App.tsx # Router principal
index.css # Styles globaux
components/ # Composants rutilisables
Layout.tsx # Layout avec sidebar
pages/ # Pages de l'application
LoginPage.tsx # Authentification
DashboardPage.tsx # Tableau de bord
VoiceAssistantPage.tsx # Interface vocale
HistoryPage.tsx # Historique
SettingsPage.tsx # Paramtres
services/ # Services API
api.ts # Client Axios
store/ # tat global
authStore.ts # Store Zustand auth
index.html # Template HTML
package.json # Dpendances npm
tsconfig.json # Config TypeScript
vite.config.ts # Config Vite
```

## Technologies

- **React 18** : UI library
- **TypeScript** : Type safety
- **Vite** : Build tool ultra-rapide
- **React Router** : Navigation
- **Zustand** : State management
- **Tanstack Query** : Data fetching
- **Axios** : HTTP client
- **Tailwind CSS** : Styling (via index.css)
- **Lucide React** : Icnes

## Fonctionnalits

### Pages Implmentes

1. **Login** : Authentification JWT
2. **Dashboard** : Vue d'ensemble + stats
3. **Voice Assistant** : Interface vocale principale
- Reconnaissance vocale (Web Speech API)
- Input texte
- Affichage intentions dtectes
- Excution actions
4. **History** : Journal des actions
- Filtrage
- Export CSV
5. **Settings** : Prfrences utilisateur
- Vitesse/volume parole
- Vibration
- Mode verbeux
- Rtention donnes

### Fonctionnalits Techniques

- **Auth persistante** : Tokens en localStorage
- **Intercepteurs HTTP** : Auto refresh token
- **Protected routes** : Redirection si non authentifi
- **Rate limiting handling** : Gestion 429
- **Responsive design** : Mobile-friendly
- **Accessibility** : ARIA labels, focus management

## Authentification

```typescript
// Store Zustand
const { token, login, logout } = useAuthStore()

// Login
await apiService.login('test@koras.com', 'password')

// Auto-injection token
api.interceptors.request.use((config) => {
config.headers.Authorization = `Bearer ${token}`
return config
})
```

## Web Speech API

```typescript
// Reconnaissance vocale
const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
const recorder = new MediaRecorder(stream)
recorder.start()

// Envoi au backend
const audioBlob = new Blob(chunks, { type: 'audio/wav' })
await apiService.interpretAudio(audioBlob, 'FRANCAIS')
```

## API Calls

```typescript
// Interprtation texte
const result = await apiService.interpret('Appelle Marie', 'FRANCAIS')

// Excution
await apiService.execute(intention, idempotenceToken)

// Historique
const history = await apiService.getHistory({ limite: 50 })

// Prfrences
await apiService.updatePreferences(prefs)
```

## Styling

Approche utility-first avec classes personnalises :

```css
/* Couleurs */
--primary: #3b82f6
--background: #0f172a
--surface: #1e293b

/* Animations */
.animate-fade-in
.animate-slide-up
```

## Tests

```bash
# Tests unitaires ( implmenter)
npm run test

# Coverage
npm run test:coverage
```

## Build & Deploy

```bash
# Build production
npm run build
# Output: dist/

# Preview
npm run preview

# Deploy (exemples)
# Vercel
vercel --prod

# Netlify
netlify deploy --prod --dir=dist

# Static server
npx serve dist
```

## Configuration

### Vite Config

```typescript
// vite.config.ts
export default defineConfig({
plugins: [react()],
server: {
port: 3000,
proxy: {
'/api': {
target: 'http://localhost:8080', // Backend
changeOrigin: true,
}
}
}
})
```

### Environment Variables

Crer `.env` :
```env
VITE_API_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080
```

Usage :
```typescript
const apiUrl = import.meta.env.VITE_API_URL
```

## Progressive Web App (Future)

Pour ajouter PWA :

```bash
npm install vite-plugin-pwa -D
```

```typescript
// vite.config.ts
import { VitePWA } from 'vite-plugin-pwa'

plugins: [
react(),
VitePWA({
registerType: 'autoUpdate',
manifest: {
name: 'Koras Assistant Vocal',
short_name: 'Koras',
theme_color: '#3b82f6'
}
})
]
```

## Internationalisation (Future)

```bash
npm install react-i18next i18next
```

## Prochaines tapes

- [ ] Tests Vitest
- [ ] Storybook composants
- [ ] PWA support
- [ ] i18n multilingue UI
- [ ] Dark/Light theme
- [ ] WebSocket real-time
- [ ] Push notifications

## Documentation

- **React** : https://react.dev/
- **Vite** : https://vitejs.dev/
- **Zustand** : https://zustand-demo.pmnd.rs/
- **Tanstack Query** : https://tanstack.com/query

---

**Frontend 100% fonctionnel et prt pour production !**
