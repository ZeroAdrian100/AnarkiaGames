import { Link, isRouteErrorResponse, useRouteError } from 'react-router'

export function ErrorInesperado() {
  const error = useRouteError()
  const detalle = isRouteErrorResponse(error)
    ? `${error.status} ${error.statusText}`
    : error instanceof Error
      ? error.message
      : 'Error desconocido'

  return (
    <section className="mx-auto flex min-h-screen max-w-6xl flex-col items-center justify-center gap-4 px-4 text-center">
      <h1 className="font-display text-4xl italic tracking-wide">Algo salió mal</h1>
      <p className="text-white/80">{detalle}</p>
      <Link to="/" className="rounded-full bg-verde px-5 py-2 font-display italic tracking-wide text-tinta hover:bg-verde-oscuro">
        Volver al inicio
      </Link>
    </section>
  )
}
