import { Link } from 'react-router'
import { botonVerde } from './estilos'

export function Cargando({ texto = 'Cargando…' }: { texto?: string }) {
  return (
    <p role="status" className="py-24 text-center text-lg text-white/70">
      {texto}
    </p>
  )
}

export function ErrorCarga({ error }: { error: Error }) {
  return (
    <div role="alert" className="flex flex-col items-center gap-4 py-24 text-center">
      <p className="font-display text-3xl italic tracking-wide">No se pudo cargar</p>
      <p className="text-white/70">{error.message}</p>
      <Link to="/" className={botonVerde}>
        Volver al inicio
      </Link>
    </div>
  )
}
