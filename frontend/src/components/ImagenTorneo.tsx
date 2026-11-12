import { useState } from 'react'
import estadio from '../assets/inicio/estadio.webp'
import type { Torneo } from '../api/tipos'

interface Props {
  torneo: Pick<Torneo, 'nombre' | 'imagenUrl'>
  className?: string
}

/** Imagen del torneo; si no tiene o no carga, usa la foto del estadio. */
export function ImagenTorneo({ torneo, className = '' }: Props) {
  const [fallo, setFallo] = useState(false)
  const src = torneo.imagenUrl && !fallo ? torneo.imagenUrl : estadio

  return (
    <img
      src={src}
      alt={`Torneo ${torneo.nombre}`}
      loading="lazy"
      onError={() => setFallo(true)}
      className={`object-cover ${className}`}
    />
  )
}
