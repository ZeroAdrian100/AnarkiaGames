import { Link } from 'react-router'
import { useTorneos } from '../api/torneos'
import type { Torneo } from '../api/tipos'
import { Cargando, ErrorCarga } from '../components/EstadoCarga'
import { ImagenTorneo } from '../components/ImagenTorneo'
import { botonDeshabilitado, botonVerde, contenedor } from '../components/estilos'
import { formatearFecha, ventaAbierta } from '../utils/formato'

/** Primero los torneos con venta abierta, por fecha; al final los que ya pasaron o se cancelaron. */
function ordenar(torneos: Torneo[]) {
  return [...torneos].sort(
    (a, b) => Number(ventaAbierta(b)) - Number(ventaAbierta(a)) || a.fechaInicio.localeCompare(b.fechaInicio),
  )
}

export function Torneos() {
  const torneos = useTorneos()

  return (
    <section className={`${contenedor} py-16 lg:py-24`}>
      <h1 className="sr-only">Torneos</h1>
      {torneos.isPending && <Cargando texto="Cargando torneos…" />}
      {torneos.isError && <ErrorCarga error={torneos.error} />}
      {torneos.isSuccess && torneos.data.length === 0 && (
        <p className="py-24 text-center text-lg text-white/70">Todavía no hay torneos publicados. ¡Vuelve pronto!</p>
      )}
      {torneos.isSuccess && (
        <ul className="flex flex-col gap-14 lg:gap-20">
          {ordenar(torneos.data).map((torneo) => (
            <li key={torneo.id}>
              <TarjetaTorneo torneo={torneo} />
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

function TarjetaTorneo({ torneo }: { torneo: Torneo }) {
  const abierta = ventaAbierta(torneo)

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
            {torneo.juego} · {formatearFecha(torneo.fechaInicio)}
            {torneo.lugar && ` · ${torneo.lugar}`}
          </p>
        </div>
        {abierta ? (
          <Link to={`/torneos/${torneo.id}`} className={`${botonVerde} md:w-64 lg:w-72`}>
            Comprar ahora
          </Link>
        ) : (
          <span className={`${botonDeshabilitado} md:w-64 lg:w-72`}>
            {torneo.estado === 'CANCELADO' ? 'Cancelado' : 'Venta cerrada'}
          </span>
        )}
      </div>
    </article>
  )
}
