import type { ReactNode } from 'react'
import { Link } from 'react-router'
import estadio from '../assets/inicio/estadio.webp'
import { useTorneos } from '../api/torneos'
import type { TorneoDetalle } from '../api/tipos'
import { Cargando, ErrorCarga } from '../components/EstadoCarga'
import { botonDeshabilitado, botonVerde, contenedor } from '../components/estilos'
import { precioDesde, resumenEvento, type ResumenEvento } from '../tickets/evento'
import { MODALIDADES } from '../tickets/modalidades'

/** Paso 1 de la compra: descripción general del evento (frame "Tickets" del Figma). */
export function Evento() {
  const torneos = useTorneos()

  if (torneos.isError) return <ErrorCarga error={torneos.error} />
  if (!torneos.isSuccess) return <Cargando texto="Cargando evento…" />

  const resumen = resumenEvento(torneos.data)
  return (
    // En móvil las entradas van justo debajo de la imagen; en escritorio, en una columna a la derecha.
    <div className={`${contenedor} grid gap-10 py-12 lg:grid-cols-[minmax(0,1fr)_minmax(0,522px)] lg:gap-x-16 lg:py-20`}>
      <div className="lg:col-start-1">
        <h1 className="sr-only">Tickets para Anarkia Games</h1>
        <img src={estadio} alt="Estadio lleno durante la final de un torneo" className="aspect-[730/386] w-full max-w-[730px] object-cover" />
      </div>

      <aside className="flex flex-col items-center gap-8 lg:sticky lg:top-[135px] lg:col-start-2 lg:row-span-2 lg:row-start-1 lg:self-start">
        <Entradas torneos={torneos.data} resumen={resumen} />
        <div className="flex flex-col items-center gap-4 text-center">
          <p className="text-xl lg:text-[27px]">
            {resumen.hayVenta ? 'Elige tu ticket aquí' : 'Por ahora no hay torneos con venta abierta.'}
          </p>
          {resumen.hayVenta ? (
            <Link to="/tickets/modalidad" className={`${botonVerde} w-72 lg:text-[32px]`}>
              Elegir ticket
            </Link>
          ) : (
            <span className={`${botonDeshabilitado} w-72 lg:text-[32px]`}>Elegir ticket</span>
          )}
        </div>
      </aside>

      <div className="flex flex-col gap-10 lg:col-start-1">
        <section>
          <h2 className="text-2xl font-bold lg:text-3xl">Descripción</h2>
          <p className="mt-4 text-lg leading-relaxed lg:pl-7 lg:text-2xl">
            Únete al evento gamer más esperado: participa en las competiciones para demostrar tus habilidades y ganar
            increíbles premios, o adquiere tu entrada para vivir la emoción como espectador y disfrutar del mejor show de
            videojuegos en vivo.
          </p>
        </section>
        <InformacionAdicional resumen={resumen} />
      </div>
    </div>
  )
}

function Entradas({ torneos, resumen }: { torneos: TorneoDetalle[]; resumen: ResumenEvento }) {
  return (
    <section aria-labelledby="unete" className="w-full rounded-[20px] bg-tarjeta p-6 font-display italic tracking-wide lg:px-11 lg:py-6 lg:text-2xl">
      <h2 id="unete">Únete al evento</h2>
      <dl className="mt-8 space-y-4">
        <div>
          <dt className="inline">Fecha:</dt> <dd className="inline">{resumen.fecha}</dd>
        </div>
        <div>
          <dt className="inline">Horario:</dt> <dd className="inline">{resumen.horario}</dd>
        </div>
      </dl>
      <hr className="my-6 border-borde" />
      <h3>Entradas</h3>
      <dl className="mt-3 space-y-3">
        {[...MODALIDADES].reverse().map((modalidad) => (
          <div key={modalidad.tipo}>
            <dt>{modalidad.entrada}</dt>
            <dd className="pl-2">{precioDesde(torneos, modalidad.tipo) ?? 'No disponible'}</dd>
          </div>
        ))}
      </dl>
    </section>
  )
}

function InformacionAdicional({ resumen }: { resumen: ResumenEvento }) {
  return (
    <section className="text-lg leading-relaxed lg:text-2xl">
      <h2 className="text-2xl font-bold lg:text-3xl">Información Adicional</h2>
      <dl className="mt-6 space-y-1">
        <Dato titulo="Evento">Anarkia Games – Competencias de videojuegos</Dato>
        <Dato titulo="Lugar">{resumen.lugar}</Dato>
        <Dato titulo="Duración" icono="📅">
          {resumen.duracion}
        </Dato>
        <Dato titulo="Público recomendado" icono="👥">
          Apto para todas las edades (menores acompañados de un adulto)
        </Dato>
        <Dato titulo="Modalidades" icono="🏆">
          <ul className="mt-1 list-disc pl-10">
            <li>
              <strong>Jugador:</strong> Participa en las competiciones y gana increíbles premios.
            </li>
            <li>
              <strong>Espectador:</strong> Compra tu entrada y vive la emoción de los esports en vivo.
            </li>
          </ul>
        </Dato>
      </dl>
      <p className="mt-6">
        <strong>Organización y producción:</strong> Anarkia Games
      </p>
    </section>
  )
}

function Dato({ titulo, icono, children }: { titulo: string; icono?: string; children: ReactNode }) {
  return (
    <div>
      <dt className="inline font-bold">
        {icono && <span aria-hidden="true">{icono} </span>}
        {titulo}:
      </dt>{' '}
      <dd className="inline">{children}</dd>
    </div>
  )
}
