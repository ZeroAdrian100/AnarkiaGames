// Tipos de las respuestas del backend (ver docs/DOCUMENTACION.md, sección 6).

export type Role = 'ADMIN' | 'CLIENTE'

export type TipoParticipacion = 'COMPETIDOR' | 'ESPECTADOR'
export type EstadoTorneo = 'ACTIVO' | 'CANCELADO'

export interface Torneo {
  id: number
  nombre: string
  juego: string
  /** Fecha y hora local del torneo, sin zona horaria (por ejemplo 2026-11-20T18:00:00). */
  fechaInicio: string
  estado: EstadoTorneo
  descripcion: string | null
  imagenUrl: string | null
  lugar: string | null
  duracionHoras: number | null
}

export interface TipoTicket {
  id: number
  torneoId: number
  tipo: TipoParticipacion
  stockMaximo: number
  disponibles: number
  /** Precio en céntimos (S/ 25.00 = 2500). */
  precio: number
  descripcion: string | null
  imagenUrl: string | null
}

export interface TorneoDetalle extends Torneo {
  tiposTicket: TipoTicket[]
}

export interface TokenResponse {
  access_token: string
  refresh_token: string
}

export interface ErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
}
