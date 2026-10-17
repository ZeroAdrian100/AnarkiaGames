import { API_URL } from '../config'
import { borrarSesion, guardarSesion, obtenerSesion } from '../auth/sesion'
import type { ErrorResponse, TokenResponse } from './tipos'

/** Error de la API con el status HTTP y el mensaje que devuelve el backend. */
export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

interface Opciones {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
}

// Si varias peticiones reciben 401 a la vez, todas esperan el mismo refresh.
let refrescando: Promise<boolean> | null = null

async function refrescar(): Promise<boolean> {
  const sesion = obtenerSesion()
  if (!sesion) return false
  try {
    const respuesta = await fetch(`${API_URL}/auth/refresh`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${sesion.refreshToken}` },
    })
    if (!respuesta.ok) return false
    guardarSesion((await respuesta.json()) as TokenResponse)
    return true
  } catch {
    return false
  }
}

async function enviar(ruta: string, opciones: Opciones): Promise<Response> {
  const headers: Record<string, string> = {}
  if (opciones.body !== undefined) headers['Content-Type'] = 'application/json'
  const sesion = obtenerSesion()
  if (sesion) headers.Authorization = `Bearer ${sesion.accessToken}`

  try {
    return await fetch(`${API_URL}${ruta}`, {
      method: opciones.method ?? 'GET',
      headers,
      body: opciones.body === undefined ? undefined : JSON.stringify(opciones.body),
    })
  } catch {
    throw new ApiError(0, 'No se pudo conectar con el servidor')
  }
}

/**
 * Llama a la API del backend. Envía el access token si hay sesión y, si vence (401),
 * lo renueva con el refresh token y repite la petición una vez. Si no se puede renovar, cierra la sesión.
 */
export async function api<T>(ruta: string, opciones: Opciones = {}): Promise<T> {
  let respuesta = await enviar(ruta, opciones)

  if (respuesta.status === 401 && obtenerSesion() && !ruta.startsWith('/auth/')) {
    refrescando ??= refrescar().finally(() => {
      refrescando = null
    })
    if (await refrescando) {
      respuesta = await enviar(ruta, opciones)
    } else {
      borrarSesion()
    }
  }

  if (!respuesta.ok) {
    let mensaje = 'Ocurrió un error inesperado'
    try {
      mensaje = ((await respuesta.json()) as ErrorResponse).message || mensaje
    } catch {
      // Respuesta sin cuerpo JSON (por ejemplo, un 401 de Spring Security).
    }
    throw new ApiError(respuesta.status, mensaje)
  }

  if (respuesta.status === 204) return undefined as T
  return (await respuesta.json()) as T
}
