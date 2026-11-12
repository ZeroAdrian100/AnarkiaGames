import { Link, useParams } from 'react-router'
import espectador from '../assets/modalidad/espectador.webp'
import jugador from '../assets/modalidad/jugador.webp'
import { useTorneo } from '../api/torneos'
import type { TipoParticipacion, TorneoDetalle } from '../api/tipos'
import { Cargando, ErrorCarga } from '../components/EstadoCarga'
import { botonDeshabilitado, botonVerde, contenedor } from '../components/estilos'
import { formatearSoles, ventaAbierta } from '../utils/formato'

const modalidades: { tipo: TipoParticipacion; titulo: string; imagen: string; alt: string }[] = [
  { tipo: 'ESPECTADOR', titulo: 'Espectador', imagen: espectador, alt: 'Público frente al escenario de un torneo' },
  { tipo: 'COMPETIDOR', titulo: 'Jugador', imagen: jugador, alt: 'Jugadores compitiendo en sus computadoras' },
]

export function Modalidad() {
  const { id } = useParams()
  const torneo = useTorneo(Number(id))

  if (torneo.isError) return <ErrorCarga error={torneo.error} />
  if (!torneo.isSuccess) return <Cargando texto="Cargando torneo…" />

  return (
    <section className={`${contenedor} py-12 lg:py-20`}>
      <h1 className="text-center font-display text-4xl italic tracking-wide lg:text-[64px]">Elige tu modalidad</h1>
      <p className="mt-3 text-center text-white/70">
        <Link to={`/torneos/${torneo.data.id}`} className="underline-offset-4 hover:underline">
          {torneo.data.nombre}
        </Link>
      </p>
      <ul className="mt-12 grid gap-14 md:grid-cols-2 lg:gap-x-24">
        {modalidades.map((modalidad) => (
          <li key={modalidad.tipo}>
            <TarjetaModalidad torneo={torneo.data} {...modalidad} />
          </li>
        ))}
      </ul>
    </section>
  )
}

interface PropsTarjeta {
  torneo: TorneoDetalle
  tipo: TipoParticipacion
  titulo: string
  imagen: string
  alt: string
}

function TarjetaModalidad({ torneo, tipo, titulo, imagen, alt }: PropsTarjeta) {
  const ticket = torneo.tiposTicket.find((t) => t.tipo === tipo)
  const disponible = ventaAbierta(torneo) && ticket !== undefined && ticket.disponibles > 0
  const detalle = !ticket ? 'No disponible en este torneo' : ticket.disponibles > 0 ? formatearSoles(ticket.precio) : 'Agotado'

  return (
    <article className="flex flex-col items-center gap-4 text-center">
      <img src={imagen} alt={alt} className="aspect-[592/333] w-full rounded-[30px] object-cover" />
      <h2 className="font-display text-4xl italic tracking-wide lg:text-[64px] lg:leading-tight">{titulo}</h2>
      <p className="text-lg text-white/80">{detalle}</p>
      {disponible ? (
        <Link to={`/torneos/${torneo.id}/comprar/${tipo}`} className={`${botonVerde} w-72 lg:text-[32px]`}>
          Elegir ticket
        </Link>
      ) : (
        <span className={`${botonDeshabilitado} w-72 lg:text-[32px]`}>Elegir ticket</span>
      )}
    </article>
  )
}
