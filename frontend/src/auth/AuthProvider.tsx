import { useMemo, useSyncExternalStore, type ReactNode } from 'react'
import { AuthContext, type AuthValor } from './contexto'
import { borrarSesion, guardarSesion, obtenerSesion, suscribir, usuarioDe } from './sesion'

export function AuthProvider({ children }: { children: ReactNode }) {
  const sesion = useSyncExternalStore(suscribir, obtenerSesion)

  const valor = useMemo<AuthValor>(
    () => ({
      usuario: sesion ? usuarioDe(sesion.accessToken) : null,
      iniciarSesion: guardarSesion,
      cerrarSesion: borrarSesion,
    }),
    [sesion],
  )

  return <AuthContext value={valor}>{children}</AuthContext>
}
