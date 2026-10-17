import { useContext } from 'react'
import { AuthContext } from './contexto'

export function useAuth() {
  const valor = useContext(AuthContext)
  if (!valor) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return valor
}
