import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { borrarSesion, guardarSesion, obtenerSesion, usuarioDe } from '../auth/sesion'
import { api, ApiError } from './cliente'

function jwt(claims: Record<string, unknown>) {
  const parte = (o: unknown) =>
    btoa(String.fromCharCode(...new TextEncoder().encode(JSON.stringify(o))))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '')
  return `${parte({ alg: 'HS256' })}.${parte(claims)}.firma`
}

function authDe(init?: RequestInit) {
  return new Headers(init?.headers).get('Authorization')
}

function json(status: number, cuerpo?: unknown) {
  return new Response(cuerpo === undefined ? null : JSON.stringify(cuerpo), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

const fetchMock = vi.fn<typeof fetch>()

beforeEach(() => {
  borrarSesion()
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('usuarioDe', () => {
  it('lee email, nombre con tildes y rol del access token', () => {
    expect(usuarioDe(jwt({ sub: 'ana@mail.com', name: 'Adrián', role: 'ADMIN' }))).toEqual({
      email: 'ana@mail.com',
      nombre: 'Adrián',
      role: 'ADMIN',
    })
  })

  it('devuelve null con un token mal formado', () => {
    expect(usuarioDe('no-es-un-jwt')).toBeNull()
  })
})

describe('api', () => {
  it('envía el access token y devuelve el JSON', async () => {
    guardarSesion({ access_token: 'acceso-1', refresh_token: 'refresh-1' })
    fetchMock.mockResolvedValueOnce(json(200, [{ id: 1 }]))

    await expect(api('/torneos/mis-inscripciones')).resolves.toEqual([{ id: 1 }])
    expect(authDe(fetchMock.mock.calls[0][1])).toBe('Bearer acceso-1')
  })

  it('con 401 renueva el token y repite la petición', async () => {
    guardarSesion({ access_token: 'vencido', refresh_token: 'refresh-1' })
    fetchMock
      .mockResolvedValueOnce(json(401))
      .mockResolvedValueOnce(json(200, { access_token: 'acceso-2', refresh_token: 'refresh-1' }))
      .mockResolvedValueOnce(json(200, { ok: true }))

    await expect(api('/torneos/mis-inscripciones')).resolves.toEqual({ ok: true })
    expect(obtenerSesion()?.accessToken).toBe('acceso-2')
    expect(authDe(fetchMock.mock.calls[2][1])).toBe('Bearer acceso-2')
  })

  it('varias peticiones con 401 comparten un solo refresh', async () => {
    guardarSesion({ access_token: 'vencido', refresh_token: 'refresh-1' })
    fetchMock.mockImplementation(async (url, init) => {
      if (String(url).endsWith('/auth/refresh')) {
        return json(200, { access_token: 'acceso-2', refresh_token: 'refresh-1' })
      }
      return authDe(init) === 'Bearer acceso-2' ? json(200, { ok: true }) : json(401)
    })

    await Promise.all([api('/a'), api('/b'), api('/c')])
    const refrescos = fetchMock.mock.calls.filter(([url]) => String(url).endsWith('/auth/refresh'))
    expect(refrescos).toHaveLength(1)
  })

  it('si el refresh falla, cierra la sesión', async () => {
    guardarSesion({ access_token: 'vencido', refresh_token: 'vencido-tambien' })
    fetchMock.mockResolvedValueOnce(json(401)).mockResolvedValueOnce(json(401))

    await expect(api('/torneos/mis-inscripciones')).rejects.toMatchObject({ status: 401 })
    expect(obtenerSesion()).toBeNull()
  })

  it('usa el mensaje de error del backend', async () => {
    fetchMock.mockResolvedValueOnce(json(409, { status: 409, error: 'Conflict', message: 'No hay más cupos', timestamp: '' }))

    const error = await api('/torneos/1/inscripciones', { method: 'POST', body: {} }).catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 409, message: 'No hay más cupos' })
  })

  it('un fallo de red se informa con status 0', async () => {
    fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'))
    await expect(api('/torneos')).rejects.toMatchObject({ status: 0, message: 'No se pudo conectar con el servidor' })
  })

  it('una respuesta 204 devuelve undefined', async () => {
    guardarSesion({ access_token: 'acceso-1', refresh_token: 'refresh-1' })
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }))
    await expect(api('/admin/torneos/1/tipos-ticket/2', { method: 'DELETE' })).resolves.toBeUndefined()
  })
})
