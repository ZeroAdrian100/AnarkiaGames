import type { Torneo } from '../api/tipos'

const soles = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })
const fecha = new Intl.DateTimeFormat('es-PE', { day: '2-digit', month: '2-digit', year: '2-digit' })
const hora = new Intl.DateTimeFormat('es-PE', { hour: '2-digit', minute: '2-digit', hour12: false })

/** 2500 céntimos → "S/ 25.00" */
export function formatearSoles(centimos: number) {
  return soles.format(centimos / 100)
}

/** "2026-11-20T18:00:00" → "20/11/26" */
export function formatearFecha(iso: string) {
  return fecha.format(new Date(iso))
}

/** "2026-11-20T18:00:00" → "18:00" */
export function formatearHora(iso: string) {
  return hora.format(new Date(iso))
}

/** El backend solo vende tickets de torneos activos que todavía no empezaron. */
export function ventaAbierta(torneo: Pick<Torneo, 'estado' | 'fechaInicio'>, ahora = new Date()) {
  return torneo.estado === 'ACTIVO' && new Date(torneo.fechaInicio) > ahora
}
