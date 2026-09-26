import axios from 'axios'
import { useAuthStore } from '../store/authStore'

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Intercepteur requête : ajouter token
api.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().token
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Intercepteur réponse : gérer erreurs
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// Services API
export const apiService = {
  // Auth
  login: (email: string, password: string) =>
    api.post('/auth/login', { email, password }),
  
  refreshToken: (refreshToken: string) =>
    api.post('/auth/refresh', { refreshToken }),
  
  // Interprétation
  interpret: (text: string, language: string) =>
    api.post('/interprete', {
      type: 'TEXTE',
      contenu: text,
      langue: language
    }),
  
  interpretAudio: (audioData: Blob, language: string) => {
    const formData = new FormData()
    formData.append('audio', audioData)
    formData.append('langue', language)
    
    return api.post('/interprete', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  
  // Exécution
  execute: (intention: any, idempotenceToken?: string) =>
    api.post('/execute', {
      intention,
      tokenIdempotence: idempotenceToken
    }),
  
  // Historique
  getHistory: (params?: {
    action?: string
    dateDebut?: string
    dateFin?: string
    limite?: number
  }) => api.get('/historique', { params }),
  
  // Préférences
  getPreferences: () => api.get('/preferences'),
  
  updatePreferences: (prefs: any) => api.put('/preferences', prefs),
  
  // Health
  health: () => axios.get('/health')
}

export default api
