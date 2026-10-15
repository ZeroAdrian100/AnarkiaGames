import { Link } from 'react-router'

export function NoEncontrado() {
  return (
    <section className="mx-auto flex max-w-6xl flex-col items-center gap-4 px-4 py-24 text-center">
      <h1 className="font-display text-4xl italic tracking-wide">Página no encontrada</h1>
      <p className="text-white/80">La página que buscas no existe o todavía no está disponible.</p>
      <Link to="/" className="rounded-full bg-verde px-5 py-2 font-display italic tracking-wide text-tinta hover:bg-verde-oscuro">
        Volver al inicio
      </Link>
    </section>
  )
}
