import { describe, expect, it } from 'vitest'
import type { TipoTicket, TorneoDetalle } from '../api/tipos'
import { precioDesde, resumenEvento, sePuedeComprar } from './evento'

const ahora = new Date('2026-11-01T12:00:00')

function ticket(tipo: TipoTicket['tipo'], precio: number, disponibles = 10): TipoTicket {
  return { id: 1, torneoId: 1, tipo, stockMaximo: 10, disponibles, precio, descripcion: null, imagenUrl: null }
}

function torneo(cambios: Partial<TorneoDetalle>): TorneoDetalle {
  return {
    id: 1,
    nombre: 'Copa',
    juego: 'Tekken 8',
    fechaInicio: '2026-12-03T18:00:00',
    estado: 'ACTIVO',
    descripcion: null,
    imagenUrl: null,
    lugar: 'Jockey Plaza',
    duracionHoras: 8,
    tiposTicket: [ticket('ESPECTADOR', 2500), ticket('COMPETIDOR', 5000)],
    ...cambios,
  }
}

const sinEspacios = (texto: string | null) => texto?.replace(/\s/g, ' ')

describe('resumenEvento', () => {
  it('con una sola fecha muestra los datos comunes', () => {
    expect(resumenEvento([torneo({})], ahora)).toEqual({
      hayVenta: true,
      fecha: '03/12/26',
      horario: '18:00',
      lugar: 'Jockey Plaza',
      duracion: '8 hrs',
    })
  })

  it('con varias fechas muestra el rango y marca lo que varía', () => {
    const resumen = resumenEvento(
      [torneo({ fechaInicio: '2026-12-09T20:00:00', lugar: 'Arena Lima' }), torneo({})],
      ahora,
    )
    expect(resumen.fecha).toBe('03/12/26 al 09/12/26')
    expect(resumen.horario).toBe('Según el torneo')
    expect(resumen.lugar).toBe('Según el torneo')
    expect(resumen.duracion).toBe('8 hrs')
  })

  it('ignora torneos cancelados o que ya empezaron', () => {
    const resumen = resumenEvento(
      [torneo({ estado: 'CANCELADO', fechaInicio: '2026-12-20T10:00:00' }), torneo({ fechaInicio: '2026-10-01T10:00:00' })],
      ahora,
    )
    expect(resumen.hayVenta).toBe(false)
    expect(resumen.fecha).toBe('Por confirmar')
  })
})

describe('precioDesde', () => {
  it('muestra el precio único o el mínimo', () => {
    expect(sinEspacios(precioDesde([torneo({})], 'ESPECTADOR', ahora))).toBe('S/ 25.00')
    const varios = [torneo({}), torneo({ tiposTicket: [ticket('ESPECTADOR', 4000)] })]
    expect(sinEspacios(precioDesde(varios, 'ESPECTADOR', ahora))).toBe('Desde S/ 25.00')
  })

  it('no cuenta tickets agotados, de torneos cerrados o inexistentes', () => {
    const agotado = torneo({ tiposTicket: [ticket('ESPECTADOR', 1000, 0)] })
    expect(precioDesde([agotado], 'ESPECTADOR', ahora)).toBeNull()
    expect(precioDesde([agotado], 'COMPETIDOR', ahora)).toBeNull()
    expect(precioDesde([torneo({ estado: 'CANCELADO' })], 'ESPECTADOR', ahora)).toBeNull()
    expect(sePuedeComprar(torneo({}), 'COMPETIDOR', ahora)).toBe(true)
  })
})
