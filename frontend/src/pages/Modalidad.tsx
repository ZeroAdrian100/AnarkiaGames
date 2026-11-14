import { Link } from 'react-router'
import { useTorneos } from '../api/torneos'
import { Cargando, ErrorCarga } from '../components/EstadoCarga'
import { botonDeshabilitado, botonVerde, contenedor } from '../components/estilos'
import { precioDesde } from '../tickets/evento'
import { MODALIDADES } from '../tickets/modalidades'

/** Paso 2 de la compra: espectador o jugador (frame "Tickets - II" del Figma). */
export function Modalidad() {
  const torneos = useTorneos()

  if (torneos.isError) return <ErrorCarga error={torneos.error} />
  if (!torneos.isSuccess) return <Cargando texto="Cargando modalidades…" />

  return (
    <section className={`${contenedor} py-12 lg:py-20`}>
      <h1 className="text-center font-display text-4xl italic tracking-wide lg:text-[64px]">Elige tu modalidad</h1>
      <ul className="mt-12 grid gap-14 md:grid-cols-2 lg:gap-x-24">
        {MODALIDADES.map((modalidad) => {
          const precio = precioDesde(torneos.data, modalidad.tipo)
          return (
            <li key={modalidad.slug}>
              <article className="flex flex-col items-center gap-4 text-center">
                <img src={modalidad.imagen} alt={modalidad.alt} className="aspect-[592/333] w-full rounded-[30px] object-cover" />
                <h2 className="font-display text-4xl italic tracking-wide lg:text-[64px] lg:leading-tight">{modalidad.titulo}</h2>
                <p className="text-lg text-white/80">{precio ?? 'Sin tickets disponibles por ahora'}</p>
                {precio ? (
                  <Link to={`/tickets/${modalidad.slug}`} className={`${botonVerde} w-72 lg:text-[32px]`}>
                    Elegir ticket
                  </Link>
                ) : (
                  <span className={`${botonDeshabilitado} w-72 lg:text-[32px]`}>Elegir ticket</span>
                )}
              </article>
            </li>
          )
        })}
      </ul>
    </section>
  )
}
