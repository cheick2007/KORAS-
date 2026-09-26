import { useState, useRef } from 'react'
import { Mic, MicOff, Send, Volume2 } from 'lucide-react'
import { useMutation } from '@tanstack/react-query'
import { apiService } from '../services/api'

export default function VoiceAssistantPage() {
  const [isListening, setIsListening] = useState(false)
  const [text, setText] = useState('')
  const [messages, setMessages] = useState<any[]>([])
  const audioRef = useRef<MediaRecorder | null>(null)
  
  const interpretMutation = useMutation({
    mutationFn: (text: string) => apiService.interpret(text, 'FRANCAIS'),
    onSuccess: (response) => {
      const result = response.data
      setMessages(prev => [...prev, {
        type: 'response',
        intention: result.intention.type,
        confiance: result.confiance,
        texte: formatIntention(result.intention)
      }])
    }
  })
  
  const executeMutation = useMutation({
    mutationFn: (intention: any) => apiService.execute(intention),
    onSuccess: (response) => {
      setMessages(prev => [...prev, {
        type: 'execution',
        statut: response.data.statut,
        texte: 'Action exécutée avec succès'
      }])
    }
  })
  
  const handleVoiceInput = async () => {
    if (isListening) {
      audioRef.current?.stop()
      setIsListening(false)
    } else {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
        const recorder = new MediaRecorder(stream)
        audioRef.current = recorder
        
        const chunks: Blob[] = []
        recorder.ondataavailable = (e) => chunks.push(e.data)
        
        recorder.onstop = async () => {
          const audioBlob = new Blob(chunks, { type: 'audio/wav' })
          try {
            const response = await apiService.interpretAudio(audioBlob, 'FRANCAIS')
            setMessages(prev => [...prev, {
              type: 'voice',
              texte: response.data.intention.type
            }])
          } catch (error) {
            console.error('Erreur audio:', error)
          }
        }
        
        recorder.start()
        setIsListening(true)
      } catch (error) {
        console.error('Erreur micro:', error)
      }
    }
  }
  
  const handleTextSubmit = () => {
    if (!text.trim()) return
    
    setMessages(prev => [...prev, { type: 'user', texte: text }])
    interpretMutation.mutate(text)
    setText('')
  }
  
  const formatIntention = (intention: any) => {
    switch (intention.type) {
      case 'APPEL':
        return `Appeler ${intention.entites.contact?.nom}`
      case 'SMS':
        return `Envoyer SMS à ${intention.entites.contact?.nom}`
      case 'ALARME':
        return `Programmer une alarme`
      default:
        return intention.type
    }
  }
  
  return (
    <div className="flex flex-col h-full max-w-4xl mx-auto p-6">
      <div className="text-center mb-8">
        <h1 className="text-3xl font-bold mb-2">Assistant Vocal Koras</h1>
        <p className="text-gray-400">Parlez ou écrivez votre demande</p>
      </div>
      
      {/* Messages */}
      <div className="flex-1 overflow-y-auto mb-6 space-y-4">
        {messages.map((msg, i) => (
          <div
            key={i}
            className={`p-4 rounded-lg ${
              msg.type === 'user'
                ? 'bg-blue-600 ml-auto max-w-xs'
                : 'bg-gray-800 mr-auto max-w-md'
            }`}
          >
            <p>{msg.texte}</p>
            {msg.confiance && (
              <p className="text-sm text-gray-400 mt-1">
                Confiance: {msg.confiance}%
              </p>
            )}
          </div>
        ))}
      </div>
      
      {/* Contrôles vocaux */}
      <div className="flex items-center justify-center mb-6">
        <button
          onClick={handleVoiceInput}
          className={`w-20 h-20 rounded-full flex items-center justify-center transition ${
            isListening
              ? 'bg-red-600 hover:bg-red-700 animate-pulse'
              : 'bg-blue-600 hover:bg-blue-700'
          }`}
          aria-label={isListening ? 'Arrêter enregistrement' : 'Commencer enregistrement'}
        >
          {isListening ? (
            <MicOff className="w-8 h-8" />
          ) : (
            <Mic className="w-8 h-8" />
          )}
        </button>
      </div>
      
      {/* Input texte */}
      <div className="flex gap-2">
        <input
          type="text"
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyPress={(e) => e.key === 'Enter' && handleTextSubmit()}
          placeholder="Tapez votre commande..."
          className="flex-1 px-4 py-3 bg-gray-800 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          aria-label="Commande vocale"
        />
        <button
          onClick={handleTextSubmit}
          disabled={!text.trim() || interpretMutation.isPending}
          className="px-6 py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-700 disabled:cursor-not-allowed rounded-lg transition"
          aria-label="Envoyer"
        >
          <Send className="w-5 h-5" />
        </button>
      </div>
      
      {/* Statut */}
      {isListening && (
        <div className="mt-4 text-center text-sm text-gray-400 flex items-center justify-center gap-2">
          <Volume2 className="w-4 h-4 animate-pulse" />
          Écoute en cours...
        </div>
      )}
    </div>
  )
}
