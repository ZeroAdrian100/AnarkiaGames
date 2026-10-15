import type { Role, TokenResponse } from '../api/tipos'

/**
 * Guarda los tokens en localStorage para que la sesión sobreviva a recargar la página.
 * Los componentes se enteran de los cambios con suscribir(), incluso si cambian en otra pestaña.
 */

const CLAVE = 'anarkia.sesion'

export interface Sesion {
  accessToken: string
  refreshToken: string
}

export interface Usuario {
  email: string
  nombre: string
  role: Role
}

const oyentes = new Set<() => void>()
let cache: { raw: string | null; sesion: Sesion | null } = { raw: null, sesion: null }

function leerRaw(): string | null {
  try {
    return localStorage.getItem(CLAVE)
  } catch {
    return null
  }
}

function notificar() {
  oyentes.forEach((oyente) => oyente())
}

/** Devuelve siempre el mismo objeto mientras el valor guardado no cambie (lo exige useSyncExternalStore). */
export function obtenerSesion(): Sesion | null {
  const raw = leerRaw()
  if (raw !== cache.raw) {
    let sesion: Sesion | null = null
    try {
      sesion = raw ? (JSON.parse(raw) as Sesion) : null
    } catch {
      sesion = null
    }
    cache = { raw, sesion }
  }
  return cache.sesion
}

export function guardarSesion(tokens: TokenResponse) {
  const sesion: Sesion = { accessToken: tokens.access_token, refreshToken: tokens.refresh_token }
  try {
    localStorage.setItem(CLAVE, JSON.stringify(sesion))
  } catch {
    // Sin localStorage (modo privado estricto): la sesión dura hasta recargar.
    cache = { raw: JSON.stringify(sesion), sesion }
  }
  notificar()
}

export function borrarSesion() {
  try {
    localStorage.removeItem(CLAVE)
  } catch {
    cache = { raw: null, sesion: null }
  }
  notificar()
}

export function suscribir(oyente: () => void) {
  oyentes.add(oyente)
  const alCambiarOtraPestana = (e: StorageEvent) => {
    if (e.key === CLAVE) oyente()
  }
  window.addEventListener('storage', alCambiarOtraPestana)
  return () => {
    oyentes.delete(oyente)
    window.removeEventListener('storage', alCambiarOtraPestana)
  }
}

/** Lee nombre, email y rol del access token. No verifica la firma: eso lo hace el backend. */
export function usuarioDe(accessToken: string): Usuario | null {
  try {
    const payload = accessToken.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const json = new TextDecoder().decode(Uint8Array.from(atob(payload), (c) => c.charCodeAt(0)))
    const claims = JSON.parse(json) as { sub: string; name: string; role: Role }
    return { email: claims.sub, nombre: claims.name, role: claims.role }
  } catch {
    return null
  }
}
