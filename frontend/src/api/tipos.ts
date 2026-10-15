// Tipos de las respuestas del backend (ver docs/DOCUMENTACION.md, sección 6).

export type Role = 'ADMIN' | 'CLIENTE'

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
