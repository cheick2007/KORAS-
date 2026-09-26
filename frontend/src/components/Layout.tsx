import { Outlet, NavLink } from 'react-router-dom'
import { Home, Mic, History, Settings, LogOut } from 'lucide-react'
import { useAuthStore } from '../store/authStore'

export default function Layout() {
  const { user, logout } = useAuthStore()
  
  return (
    <div className="flex h-screen bg-gray-900 text-white">
      {/* Sidebar */}
      <aside className="w-64 bg-gray-800 border-r border-gray-700 flex flex-col">
        <div className="p-6">
          <h1 className="text-2xl font-bold">🎙️ Koras</h1>
          <p className="text-sm text-gray-400 mt-1">{user?.email}</p>
        </div>
        
        <nav className="flex-1 px-4">
          <NavItem to="/" icon={Home} label="Tableau de bord" />
          <NavItem to="/assistant" icon={Mic} label="Assistant Vocal" />
          <NavItem to="/history" icon={History} label="Historique" />
          <NavItem to="/settings" icon={Settings} label="Paramètres" />
        </nav>
        
        <div className="p-4 border-t border-gray-700">
          <button
            onClick={logout}
            className="w-full flex items-center gap-3 px-4 py-2 text-red-400 hover:bg-gray-700 rounded-lg transition"
          >
            <LogOut className="w-5 h-5" />
            Déconnexion
          </button>
        </div>
      </aside>
      
      {/* Contenu principal */}
      <main className="flex-1 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  )
}

function NavItem({ to, icon: Icon, label }: any) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        `flex items-center gap-3 px-4 py-3 my-1 rounded-lg transition ${
          isActive
            ? 'bg-blue-600 text-white'
            : 'text-gray-300 hover:bg-gray-700'
        }`
      }
    >
      <Icon className="w-5 h-5" />
      {label}
    </NavLink>
  )
}
