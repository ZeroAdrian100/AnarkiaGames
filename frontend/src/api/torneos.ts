import { useQuery } from '@tanstack/react-query'
import { api } from './cliente'
import type { Torneo, TorneoDetalle } from './tipos'

export function useTorneos() {
  return useQuery({
    queryKey: ['torneos'],
    queryFn: () => api<Torneo[]>('/torneos'),
  })
}

export function useTorneo(id: number) {
  return useQuery({
    queryKey: ['torneos', id],
    queryFn: () => api<TorneoDetalle>(`/torneos/${id}`),
    enabled: Number.isInteger(id) && id > 0,
  })
}
