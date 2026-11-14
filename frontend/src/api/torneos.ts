import { useQuery } from '@tanstack/react-query'
import { api } from './cliente'
import type { TorneoDetalle } from './tipos'

/** Todos los torneos, cada uno con sus tipos de ticket, precios y cupos. */
export function useTorneos() {
  return useQuery({
    queryKey: ['torneos'],
    queryFn: () => api<TorneoDetalle[]>('/torneos'),
  })
}
