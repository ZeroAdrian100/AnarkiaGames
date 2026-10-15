import { useQuery } from '@tanstack/react-query'
import { api } from '../api/cliente'

// Página provisional de la F0: comprueba que el frontend llega al backend. La F1 la reemplaza por el inicio del diseño.
export function Inicio() {
  const salud = useQuery({
    queryKey: ['salud'],
    queryFn: () => api<{ status: string }>('/actuator/health'),
  })

  return (
    <section className="mx-auto flex max-w-6xl flex-col items-center gap-6 px-4 py-24 text-center">
      <h1 className="font-display text-4xl italic tracking-wide sm:text-5xl">Anarkia Games</h1>
      <p className="text-lg text-white/80">Forma parte del evento gamer que estabas esperando.</p>
      <p className="rounded-full bg-superficie px-4 py-2 text-sm" role="status">
        {salud.isPending && 'Conectando con el backend…'}
        {salud.isSuccess && <span className="text-verde">Backend conectado ({salud.data.status})</span>}
        {salud.isError && <span className="text-red-400">Sin conexión con el backend: {salud.error.message}</span>}
      </p>
    </section>
  )
}
