import { describe, expect, it } from 'vitest'
import { formatearFecha, formatearHora, formatearSoles, ventaAbierta } from './formato'

describe('formato', () => {
  it('convierte céntimos a soles', () => {
    expect(formatearSoles(2500).replace(/\s/g, ' ')).toBe('S/ 25.00')
    expect(formatearSoles(5).replace(/\s/g, ' ')).toBe('S/ 0.05')
  })

  it('muestra fecha y hora locales del torneo', () => {
    expect(formatearFecha('2026-11-20T18:05:00')).toBe('20/11/26')
    expect(formatearHora('2026-11-20T18:05:00')).toBe('18:05')
  })

  it('la venta está abierta solo en torneos activos que no empezaron', () => {
    const ahora = new Date('2026-11-01T12:00:00')
    expect(ventaAbierta({ estado: 'ACTIVO', fechaInicio: '2026-11-20T18:00:00' }, ahora)).toBe(true)
    expect(ventaAbierta({ estado: 'CANCELADO', fechaInicio: '2026-11-20T18:00:00' }, ahora)).toBe(false)
    expect(ventaAbierta({ estado: 'ACTIVO', fechaInicio: '2026-10-20T18:00:00' }, ahora)).toBe(false)
  })
})
