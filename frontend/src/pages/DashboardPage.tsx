import { useQuery } from '@tanstack/react-query'
import { Activity, Clock, TrendingUp, Users } from 'lucide-react'
import { apiService } from '../services/api'

export default function DashboardPage() {
  const { data: history } = useQuery({
    queryKey: ['history'],
    queryFn: () => apiService.getHistory({ limite: 10 })
  })
  
  return (
    <div className="p-8">
      <h1 className="text-3xl font-bold mb-8">Tableau de bord</h1>
      
      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <StatCard
          icon={Activity}
          label="Requêtes aujourd'hui"
          value="42"
          trend="+12%"
        />
        <StatCard
          icon={Clock}
          label="Temps moyen"
          value="320ms"
          trend="-5%"
        />
        <StatCard
          icon={TrendingUp}
          label="Taux de réussite"
          value="98.5%"
          trend="+2%"
        />
        <StatCard
          icon={Users}
          label="Utilisateurs actifs"
          value="156"
          trend="+8%"
        />
      </div>
      
      {/* Historique récent */}
      <div className="bg-gray-800 rounded-xl p-6">
        <h2 className="text-xl font-semibold mb-4">Activité récente</h2>
        
        <div className="space-y-4">
          {history?.data?.entrees?.map((entry: any) => (
            <div
              key={entry.id}
              className="flex items-center justify-between p-4 bg-gray-700 rounded-lg hover:bg-gray-600 transition"
            >
              <div>
                <p className="font-medium">{entry.action}</p>
                <p className="text-sm text-gray-400">
                  {new Date(entry.horodatage).toLocaleString()}
                </p>
              </div>
              <StatusBadge status={entry.resultat} />
            </div>
          )) || (
            <p className="text-gray-400 text-center py-8">
              Aucune activité récente
            </p>
          )}
        </div>
      </div>
    </div>
  )
}

function StatCard({ icon: Icon, label, value, trend }: any) {
  return (
    <div className="bg-gray-800 rounded-xl p-6 hover:bg-gray-700 transition">
      <div className="flex items-center justify-between mb-4">
        <div className="w-12 h-12 bg-blue-600 rounded-lg flex items-center justify-center">
          <Icon className="w-6 h-6" />
        </div>
        <span className="text-sm text-green-400">{trend}</span>
      </div>
      <p className="text-2xl font-bold mb-1">{value}</p>
      <p className="text-sm text-gray-400">{label}</p>
    </div>
  )
}

function StatusBadge({ status }: { status: string }) {
  const colors = {
    SUCCES: 'bg-green-600',
    ECHEC: 'bg-red-600',
    ANNULE: 'bg-yellow-600',
  }
  
  return (
    <span className={`px-3 py-1 ${colors[status] || 'bg-gray-600'} rounded-full text-sm`}>
      {status}
    </span>
  )
}
