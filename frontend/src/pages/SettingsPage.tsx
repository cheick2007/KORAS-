import { useState, useEffect } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Save, Check } from 'lucide-react'
import { apiService } from '../services/api'

export default function SettingsPage() {
  const [prefs, setPrefs] = useState({
    vitesseParole: 1.0,
    volumeParole: 80,
    vibrationActivee: true,
    modeVerbeux: false,
    periodeRetention: 'TRENTE_JOURS'
  })
  
  const [saved, setSaved] = useState(false)
  
  const { data } = useQuery({
    queryKey: ['preferences'],
    queryFn: () => apiService.getPreferences()
  })
  
  useEffect(() => {
    if (data?.data) {
      setPrefs(data.data)
    }
  }, [data])
  
  const saveMutation = useMutation({
    mutationFn: (prefs: any) => apiService.updatePreferences(prefs),
    onSuccess: () => {
      setSaved(true)
      setTimeout(() => setSaved(false), 3000)
    }
  })
  
  const handleSave = () => {
    saveMutation.mutate(prefs)
  }
  
  return (
    <div className="p-8 max-w-3xl mx-auto">
      <h1 className="text-3xl font-bold mb-8">Paramètres</h1>
      
      <div className="space-y-6">
        {/* Parole */}
        <Section title="Assistant Vocal">
          <Setting label="Vitesse de parole">
            <input
              type="range"
              min="0.5"
              max="2"
              step="0.1"
              value={prefs.vitesseParole}
              onChange={(e) => setPrefs({ ...prefs, vitesseParole: parseFloat(e.target.value) })}
              className="w-full"
            />
            <span className="text-sm text-gray-400">{prefs.vitesseParole}x</span>
          </Setting>
          
          <Setting label="Volume">
            <input
              type="range"
              min="0"
              max="100"
              value={prefs.volumeParole}
              onChange={(e) => setPrefs({ ...prefs, volumeParole: parseInt(e.target.value) })}
              className="w-full"
            />
            <span className="text-sm text-gray-400">{prefs.volumeParole}%</span>
          </Setting>
          
          <Setting label="Vibration">
            <Toggle
              checked={prefs.vibrationActivee}
              onChange={(checked) => setPrefs({ ...prefs, vibrationActivee: checked })}
            />
          </Setting>
          
          <Setting label="Mode verbeux">
            <Toggle
              checked={prefs.modeVerbeux}
              onChange={(checked) => setPrefs({ ...prefs, modeVerbeux: checked })}
            />
          </Setting>
        </Section>
        
        {/* Confidentialité */}
        <Section title="Confidentialité">
          <Setting label="Période de rétention">
            <select
              value={prefs.periodeRetention}
              onChange={(e) => setPrefs({ ...prefs, periodeRetention: e.target.value })}
              className="px-4 py-2 bg-gray-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="SEPT_JOURS">7 jours</option>
              <option value="TRENTE_JOURS">30 jours</option>
              <option value="QUATRE_VINGT_DIX_JOURS">90 jours</option>
              <option value="JAMAIS">Jamais</option>
            </select>
          </Setting>
        </Section>
        
        {/* Bouton sauvegarder */}
        <button
          onClick={handleSave}
          disabled={saveMutation.isPending || saved}
          className="w-full py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-700 rounded-lg font-semibold transition flex items-center justify-center gap-2"
        >
          {saved ? (
            <>
              <Check className="w-5 h-5" />
              Sauvegardé
            </>
          ) : (
            <>
              <Save className="w-5 h-5" />
              Sauvegarder
            </>
          )}
        </button>
      </div>
    </div>
  )
}

function Section({ title, children }: any) {
  return (
    <div className="bg-gray-800 rounded-xl p-6">
      <h2 className="text-xl font-semibold mb-4">{title}</h2>
      <div className="space-y-4">{children}</div>
    </div>
  )
}

function Setting({ label, children }: any) {
  return (
    <div className="flex items-center justify-between">
      <label className="text-sm font-medium">{label}</label>
      <div className="flex items-center gap-4">{children}</div>
    </div>
  )
}

function Toggle({ checked, onChange }: any) {
  return (
    <button
      onClick={() => onChange(!checked)}
      className={`relative w-12 h-6 rounded-full transition ${
        checked ? 'bg-blue-600' : 'bg-gray-600'
      }`}
    >
      <span
        className={`absolute top-1 w-4 h-4 bg-white rounded-full transition-transform ${
          checked ? 'translate-x-7' : 'translate-x-1'
        }`}
      />
    </button>
  )
}
