import { Link, useParams } from 'react-router'
import { useTorneos } from '../api/torneos'
import type { TorneoDetalle } from '../api/tipos'
import { Cargando, ErrorCarga } from '../components/EstadoCarga'
import { ImagenTorneo } from '../components/ImagenTorneo'
import { botonDeshabilitado, botonVerde, contenedor } from '../components/estilos'
import { sePuedeComprar, ticketDe } from '../tickets/evento'
import { modalidadDe, type Modalidad } from '../tickets/modalidades'
import { formatearFecha, formatearHora, formatearSoles, ventaAbierta } from '../utils/formato'
import { NoEncontrado } from './NoEncontrado'

/** Paso 3 de la compra: torneos con tickets de la modalidad elegida (frame "Tickets - III" del Figma). */
export function TicketsPorModalidad() {
  const { modalidad: slug } = useParams()
  const modalidad = modalidadDe(slug)
  const torneos = useTorneos()

  if (!modalidad) return <NoEncontrado />

  // Solo los torneos que ofrecen esa modalidad; primero los que se pueden comprar, por fecha.
  const conTicket = (torneos.data ?? [])
    .filter((torneo) => ticketDe(torneo, modalidad.tipo))
    .sort(
      (a, b) =>
        Number(sePuedeComprar(b, modalidad.tipo)) - Number(sePuedeComprar(a, modalidad.tipo)) ||
        a.fechaInicio.localeCompare(b.fechaInicio),
    )

  return (
    <section className={`${contenedor} py-12 lg:py-20`}>
      <div className="mb-12 flex flex-wrap items-baseline justify-between gap-4 lg:px-8">
        <h1 className="font-display text-3xl italic tracking-wide lg:text-5xl">Tickets de {modalidad.titulo.toLowerCase()}</h1>
        <Link to="/tickets/modalidad" className="text-menta underline-offset-4 hover:underline">
          Cambiar modalidad
        </Link>
      </div>

      {torneos.isPending && <Cargando texto="Cargando tickets…" />}
      {torneos.isError && <ErrorCarga error={torneos.error} />}
      {torneos.isSuccess && conTicket.length === 0 && (
        <p className="py-24 text-center text-lg text-white/70">
          Todavía no hay torneos con tickets de {modalidad.titulo.toLowerCase()}. ¡Vuelve pronto!
        </p>
      )}
      {torneos.isSuccess && (
        <ul className="flex flex-col gap-14 lg:gap-20">
          {conTicket.map((torneo) => (
            <li key={torneo.id}>
              <TarjetaTicket torneo={torneo} modalidad={modalidad} />
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

function TarjetaTicket({ torneo, modalidad }: { torneo: TorneoDetalle; modalidad: Modalidad }) {
  const ticket = ticketDe(torneo, modalidad.tipo)!
  const cerrado =
    torneo.estado === 'CANCELADO' ? 'Cancelado' : !ventaAbierta(torneo) ? 'Venta cerrada' : ticket.disponibles <= 0 ? 'Agotado' : null

  return (
    <article aria-labelledby={`torneo-${torneo.id}`}>
      <h2 id={`torneo-${torneo.id}`} className="mb-4 font-display text-xl italic tracking-wide lg:ml-8 lg:text-2xl">
        Evento: {torneo.nombre}
      </h2>
      <div className="flex flex-col gap-6 rounded-[20px] bg-superficie p-5 md:flex-row md:items-center lg:gap-16 lg:px-8 lg:py-5">
        <ImagenTorneo torneo={torneo} className="aspect-[384/216] w-full rounded-md md:w-80 lg:w-96" />
        <div className="flex flex-1 flex-col gap-2">
          <p className="text-lg leading-snug lg:max-w-md lg:text-2xl">
            {torneo.descripcion ?? `Participa en el torneo de ${torneo.juego}.`}
          </p>
          <p className="text-sm text-white/60">
            {formatearFecha(torneo.fechaInicio)} · {formatearHora(torneo.fechaInicio)}
            {torneo.lugar && ` · ${torneo.lugar}`}
          </p>
          <p className="font-display text-xl italic tracking-wide">
            {formatearSoles(ticket.precio)}
            {!cerrado && <span className="ml-3 font-body text-sm not-italic text-white/60">{ticket.disponibles} disponibles</span>}
          </p>
        </div>
        {cerrado ? (
          <span className={`${botonDeshabilitado} md:w-64 lg:w-72`}>{cerrado}</span>
        ) : (
          <Link to={`/tickets/${modalidad.slug}/${torneo.id}`} className={`${botonVerde} md:w-64 lg:w-72`}>
            Comprar ahora
          </Link>
        )}
      </div>
    </article>
  )
}
