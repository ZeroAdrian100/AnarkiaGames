import type { ReactNode } from 'react'
import { Link, useParams } from 'react-router'
import { useTorneo } from '../api/torneos'
import type { TipoTicket, TorneoDetalle as Detalle } from '../api/tipos'
import { Cargando, ErrorCarga } from '../components/EstadoCarga'
import { ImagenTorneo } from '../components/ImagenTorneo'
import { botonDeshabilitado, botonVerde, contenedor } from '../components/estilos'
import { formatearFecha, formatearHora, formatearSoles, ventaAbierta } from '../utils/formato'

const DESCRIPCION_GENERAL =
  'Únete al evento gamer más esperado: participa en las competiciones para demostrar tus habilidades y ganar increíbles premios, o adquiere tu entrada para vivir la emoción como espectador y disfrutar del mejor show de videojuegos en vivo.'

const NOMBRE_TIPO: Record<TipoTicket['tipo'], string> = {
  COMPETIDOR: 'Competidor',
  ESPECTADOR: 'Público General',
}

export function TorneoDetalle() {
  const { id } = useParams()
  const torneo = useTorneo(Number(id))

  if (torneo.isError) return <ErrorCarga error={torneo.error} />
  if (!torneo.isSuccess) return <Cargando texto="Cargando torneo…" />

  const t = torneo.data
  return (
    // En móvil las entradas van justo debajo de la imagen; en escritorio, en una columna a la derecha.
    <div className={`${contenedor} grid gap-10 py-12 lg:grid-cols-[minmax(0,1fr)_minmax(0,522px)] lg:gap-x-16 lg:py-20`}>
      <div className="flex flex-col gap-10 lg:col-start-1">
        <h1 className="font-display text-3xl italic tracking-wide lg:text-5xl">{t.nombre}</h1>
        <ImagenTorneo torneo={t} className="aspect-[730/386] w-full max-w-[730px]" />
      </div>

      <aside className="flex flex-col items-center gap-8 lg:sticky lg:top-[135px] lg:col-start-2 lg:row-span-2 lg:row-start-1 lg:self-start">
        <Entradas torneo={t} />
        <EleccionTicket torneo={t} />
      </aside>

      <div className="flex flex-col gap-10 lg:col-start-1">
        <section>
          <h2 className="text-2xl font-bold lg:text-3xl">Descripción</h2>
          <p className="mt-4 text-lg leading-relaxed lg:pl-7 lg:text-2xl">{t.descripcion ?? DESCRIPCION_GENERAL}</p>
        </section>
        <InformacionAdicional torneo={t} />
      </div>
    </div>
  )
}

function InformacionAdicional({ torneo }: { torneo: Detalle }) {
  return (
    <section className="text-lg leading-relaxed lg:text-2xl">
      <h2 className="text-2xl font-bold lg:text-3xl">Información Adicional</h2>
      <dl className="mt-6 space-y-1">
        <Dato titulo="Evento">
          {torneo.nombre} – {torneo.juego}
        </Dato>
        <Dato titulo="Lugar">{torneo.lugar ?? 'Por confirmar'}</Dato>
        <Dato titulo="Duración" icono="📅">
          {torneo.duracionHoras ? `${torneo.duracionHoras} hrs` : 'Por confirmar'}
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

function Entradas({ torneo }: { torneo: Detalle }) {
  const tipos = [...torneo.tiposTicket].sort((a, b) => a.tipo.localeCompare(b.tipo))

  return (
    <section aria-labelledby="unete" className="w-full rounded-[20px] bg-tarjeta p-6 font-display italic tracking-wide lg:px-11 lg:py-6 lg:text-2xl">
      <h2 id="unete">Únete al evento</h2>
      <dl className="mt-8 space-y-4">
        <div>
          <dt className="inline">Fecha:</dt> <dd className="inline">{formatearFecha(torneo.fechaInicio)}</dd>
        </div>
        <div>
          <dt className="inline">Horario:</dt> <dd className="inline">{formatearHora(torneo.fechaInicio)}</dd>
        </div>
      </dl>
      <hr className="my-6 border-borde" />
      <h3>Entradas</h3>
      {tipos.length === 0 ? (
        <p className="mt-3 font-body not-italic text-white/70">Las entradas aún no están a la venta.</p>
      ) : (
        <dl className="mt-3 space-y-3">
          {tipos.map((tipo) => (
            <div key={tipo.id}>
              <dt>{NOMBRE_TIPO[tipo.tipo]}</dt>
              <dd className="pl-2">
                {formatearSoles(tipo.precio)}
                <span className="ml-3 font-body text-sm not-italic text-white/60">
                  {tipo.disponibles > 0 ? `${tipo.disponibles} disponibles` : 'Agotado'}
                </span>
              </dd>
            </div>
          ))}
        </dl>
      )}
    </section>
  )
}

function EleccionTicket({ torneo }: { torneo: Detalle }) {
  const hayCupos = torneo.tiposTicket.some((tipo) => tipo.disponibles > 0)
  const motivo =
    torneo.estado === 'CANCELADO'
      ? 'Este torneo fue cancelado.'
      : !ventaAbierta(torneo)
        ? 'La venta de tickets ya cerró.'
        : !hayCupos
          ? 'No quedan tickets disponibles.'
          : null

  return (
    <div className="flex flex-col items-center gap-4 text-center">
      <p className="text-xl lg:text-[27px]">{motivo ?? 'Elige tu ticket aquí'}</p>
      {motivo ? (
        <span className={`${botonDeshabilitado} w-72 lg:text-[32px]`}>Elegir ticket</span>
      ) : (
        <Link to={`/torneos/${torneo.id}/modalidad`} className={`${botonVerde} w-72 lg:text-[32px]`}>
          Elegir ticket
        </Link>
      )}
    </div>
  )
}
