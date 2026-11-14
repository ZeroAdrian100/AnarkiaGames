import type { TipoParticipacion, TipoTicket, TorneoDetalle } from '../api/tipos'
import { formatearFecha, formatearHora, formatearSoles, ventaAbierta } from '../utils/formato'

const SEGUN_TORNEO = 'Según el torneo'

export function ticketDe(torneo: TorneoDetalle, tipo: TipoParticipacion): TipoTicket | undefined {
  return torneo.tiposTicket.find((ticket) => ticket.tipo === tipo)
}

/** Se puede comprar ese tipo de ticket: venta abierta y con cupos. */
export function sePuedeComprar(torneo: TorneoDetalle, tipo: TipoParticipacion, ahora = new Date()) {
  const ticket = ticketDe(torneo, tipo)
  return ventaAbierta(torneo, ahora) && ticket !== undefined && ticket.disponibles > 0
}

/** "S/ 25.00" si todos cuestan igual, "Desde S/ 25.00" si varía, o null si no hay tickets de ese tipo a la venta. */
export function precioDesde(torneos: TorneoDetalle[], tipo: TipoParticipacion, ahora = new Date()) {
  const precios = torneos.filter((torneo) => sePuedeComprar(torneo, tipo, ahora)).map((torneo) => ticketDe(torneo, tipo)!.precio)
  if (precios.length === 0) return null
  const minimo = Math.min(...precios)
  return minimo === Math.max(...precios) ? formatearSoles(minimo) : `Desde ${formatearSoles(minimo)}`
}

function comun(valores: (string | null)[]) {
  const distintos = new Set(valores)
  if (distintos.size !== 1) return distintos.size === 0 ? 'Por confirmar' : SEGUN_TORNEO
  return valores[0] ?? 'Por confirmar'
}

export interface ResumenEvento {
  hayVenta: boolean
  fecha: string
  horario: string
  lugar: string
  duracion: string
}

/** Datos generales del evento, calculados con los torneos que tienen la venta abierta. */
export function resumenEvento(torneos: TorneoDetalle[], ahora = new Date()): ResumenEvento {
  const abiertos = torneos
    .filter((torneo) => ventaAbierta(torneo, ahora))
    .sort((a, b) => a.fechaInicio.localeCompare(b.fechaInicio))
  if (abiertos.length === 0) {
    return { hayVenta: false, fecha: 'Por confirmar', horario: 'Por confirmar', lugar: 'Por confirmar', duracion: 'Por confirmar' }
  }

  const primera = formatearFecha(abiertos[0].fechaInicio)
  const ultima = formatearFecha(abiertos[abiertos.length - 1].fechaInicio)
  return {
    hayVenta: true,
    fecha: primera === ultima ? primera : `${primera} al ${ultima}`,
    horario: comun(abiertos.map((torneo) => formatearHora(torneo.fechaInicio))),
    lugar: comun(abiertos.map((torneo) => torneo.lugar)),
    duracion: comun(abiertos.map((torneo) => (torneo.duracionHoras ? `${torneo.duracionHoras} hrs` : null))),
  }
}
