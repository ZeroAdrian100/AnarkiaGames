import { createContext } from 'react'
import type { TokenResponse } from '../api/tipos'
import type { Usuario } from './sesion'

export interface AuthValor {
  usuario: Usuario | null
  iniciarSesion: (tokens: TokenResponse) => void
  cerrarSesion: () => void
}

export const AuthContext = createContext<AuthValor | null>(null)
