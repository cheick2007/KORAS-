import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface AuthState {
  token: string | null
  refreshToken: string | null
  isAuthenticated: boolean
  user: {
    id: string
    email: string
    roles: string[]
  } | null
  
  login: (token: string, refreshToken: string, user: any) => void
  logout: () => void
  updateToken: (token: string) => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      refreshToken: null,
      isAuthenticated: false,
      user: null,
      
      login: (token, refreshToken, user) => set({
        token,
        refreshToken,
        user,
        isAuthenticated: true
      }),
      
      logout: () => set({
        token: null,
        refreshToken: null,
        user: null,
        isAuthenticated: false
      }),
      
      updateToken: (token) => set({ token })
    }),
    {
      name: 'koras-auth'
    }
  )
)
