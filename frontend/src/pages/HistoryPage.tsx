import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Filter, Download } from 'lucide-react'
import { apiService } from '../services/api'

export default function HistoryPage() {
  const [filters, setFilters] = useState({
    action: '',
    limite: 50
  })
  
  const { data, isLoading } = useQuery({
    queryKey: ['history', filters],
    queryFn: () => apiService.getHistory(filters)
  })
  
  const exportCSV = () => {
    if (!data?.data?.entrees) return
    
    const csv = [
      ['ID', 'Date', 'Action', 'Résultat', 'Durée (ms)'].join(','),
      ...data.data.entrees.map((entry: any) => [
        entry.id,
        entry.horodatage,
        entry.action,
        entry.resultat,
        entry.dureeMs
      ].join(','))
    ].join('\n')
    
    const blob = new Blob([csv], { type: 'text/csv' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `historique-${Date.now()}.csv`
    a.click()
  }
  
  return (
    <div className="p-8">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-3xl font-bold">Historique</h1>
        
        <div className="flex gap-4">
          <button className="flex items-center gap-2 px-4 py-2 bg-gray-800 hover:bg-gray-700 rounded-lg transition">
            <Filter className="w-4 h-4" />
            Filtrer
          </button>
          <button
            onClick={exportCSV}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded-lg transition"
          >
            <Download className="w-4 h-4" />
            Exporter CSV
          </button>
        </div>
      </div>
      
      <div className="bg-gray-800 rounded-xl overflow-hidden">
        <table className="w-full">
          <thead className="bg-gray-700">
            <tr>
              <th className="px-6 py-4 text-left text-sm font-semibold">Date</th>
              <th className="px-6 py-4 text-left text-sm font-semibold">Action</th>
              <th className="px-6 py-4 text-left text-sm font-semibold">Résultat</th>
              <th className="px-6 py-4 text-left text-sm font-semibold">Durée</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={4} className="px-6 py-8 text-center text-gray-400">
                  Chargement...
                </td>
              </tr>
            ) : data?.data?.entrees?.map((entry: any) => (
              <tr key={entry.id} className="border-t border-gray-700 hover:bg-gray-700/50 transition">
                <td className="px-6 py-4 text-sm">
                  {new Date(entry.horodatage).toLocaleString()}
                </td>
                <td className="px-6 py-4 font-medium">{entry.action}</td>
                <td className="px-6 py-4">
                  <span className={`px-2 py-1 rounded text-xs ${
                    entry.resultat === 'SUCCES' ? 'bg-green-600' :
                    entry.resultat === 'ECHEC' ? 'bg-red-600' : 'bg-yellow-600'
                  }`}>
                    {entry.resultat}
                  </span>
                </td>
                <td className="px-6 py-4 text-sm text-gray-400">
                  {entry.dureeMs}ms
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        
        {data?.data?.entrees?.length === 0 && (
          <div className="px-6 py-8 text-center text-gray-400">
            Aucune entrée dans l'historique
          </div>
        )}
      </div>
    </div>
  )
}
