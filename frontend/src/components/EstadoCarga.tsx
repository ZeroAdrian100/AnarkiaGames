import { Link } from 'react-router'
import { ApiError } from '../api/cliente'
import { botonVerde } from './estilos'

export function Cargando({ texto = 'Cargando…' }: { texto?: string }) {
  return (
    <p role="status" className="py-24 text-center text-lg text-white/70">
      {texto}
    </p>
  )
}

export function ErrorCarga({ error }: { error: Error }) {
  const noExiste = error instanceof ApiError && error.status === 404
  return (
    <div role="alert" className="flex flex-col items-center gap-4 py-24 text-center">
      <p className="font-display text-3xl italic tracking-wide">{noExiste ? 'No encontramos este torneo' : 'No se pudo cargar'}</p>
      <p className="text-white/70">{noExiste ? 'Puede que el enlace esté mal o que el torneo ya no exista.' : error.message}</p>
      <Link to="/torneos" className={botonVerde}>
        Ver torneos
      </Link>
    </div>
  )
}
