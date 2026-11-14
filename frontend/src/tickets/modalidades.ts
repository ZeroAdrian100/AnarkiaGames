import espectador from '../assets/modalidad/espectador.webp'
import jugador from '../assets/modalidad/jugador.webp'
import type { TipoParticipacion } from '../api/tipos'

export interface Modalidad {
  /** Parte de la URL: /tickets/espectador o /tickets/jugador. */
  slug: string
  tipo: TipoParticipacion
  titulo: string
  /** Nombre de la entrada en la tarjeta "Únete al evento". */
  entrada: string
  imagen: string
  alt: string
}

export const MODALIDADES: Modalidad[] = [
  {
    slug: 'espectador',
    tipo: 'ESPECTADOR',
    titulo: 'Espectador',
    entrada: 'Público General',
    imagen: espectador,
    alt: 'Público frente al escenario de un torneo',
  },
  {
    slug: 'jugador',
    tipo: 'COMPETIDOR',
    titulo: 'Jugador',
    entrada: 'Competidor',
    imagen: jugador,
    alt: 'Jugadores compitiendo en sus computadoras',
  },
]

export function modalidadDe(slug: string | undefined) {
  return MODALIDADES.find((modalidad) => modalidad.slug === slug)
}
