import facebook from '../assets/redes/facebook.svg'
import instagram from '../assets/redes/instagram.svg'
import linkedin from '../assets/redes/linkedin.svg'
import youtube from '../assets/redes/youtube.svg'

const redes = [
  { nombre: 'Instagram', icono: instagram },
  { nombre: 'YouTube', icono: youtube },
  { nombre: 'LinkedIn', icono: linkedin },
  { nombre: 'Facebook', icono: facebook },
]

const ANIO = new Date().getFullYear()

export function Footer() {
  return (
    <footer className="bg-negro">
      <div className="mx-auto max-w-8xl px-4 pb-8 lg:px-20">
        <div className="border-t border-borde" />
        <div className="flex flex-col gap-6 pt-6 sm:flex-row sm:justify-between">
          <p className="font-display text-2xl italic tracking-wide">Anarkia Games</p>
          <ul className="flex flex-col gap-2" aria-label="Redes sociales">
            {redes.map((red) => (
              <li key={red.nombre} className="flex items-center gap-2">
                <img src={red.icono} alt={red.nombre} width={24} height={24} className="size-6" />
                <span className="font-display italic tracking-wide">Anarkia Games</span>
              </li>
            ))}
          </ul>
        </div>
        <p className="mt-6 text-xs text-white/50">© {ANIO} Anarkia Games</p>
      </div>
    </footer>
  )
}
