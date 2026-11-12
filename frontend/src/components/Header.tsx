import { useState } from 'react'
import { Link, NavLink } from 'react-router'
import menu from '../assets/redes/menu.svg'
import { useAuth } from '../auth/useAuth'

const enlacesMenu = [
  { to: '/', texto: 'Inicio' },
  { to: '/torneos', texto: 'Torneos' },
  { to: '/merch', texto: 'Merch' },
]

const pildora = 'rounded-full px-3 py-1 font-display text-sm italic tracking-wide lg:px-6 lg:py-1.5 lg:text-xl'

export function Header() {
  const { usuario, cerrarSesion } = useAuth()
  const [menuAbierto, setMenuAbierto] = useState(false)

  return (
    <header className="sticky top-0 z-20 bg-negro">
      <div className="mx-auto flex h-16 max-w-8xl items-center gap-3 px-4 lg:h-[115px] lg:gap-6 lg:px-10">
        <button
          type="button"
          aria-label={menuAbierto ? 'Cerrar menú' : 'Abrir menú'}
          aria-expanded={menuAbierto}
          aria-controls="menu-principal"
          onClick={() => setMenuAbierto((abierto) => !abierto)}
          className="rounded p-1 hover:bg-superficie focus-visible:outline-2 focus-visible:outline-verde"
        >
          <img src={menu} alt="" width={40} height={34} className="h-6 w-auto lg:h-7" />
        </button>

        <Link to="/" className="mr-auto font-display text-2xl italic tracking-wide sm:text-3xl lg:ml-16 lg:text-5xl">
          Anarkia Games
        </Link>

        <Link to="/torneos" className={`${pildora} hidden rounded-[10px] bg-verde px-6 text-tinta hover:bg-verde-oscuro sm:block lg:px-12 lg:text-3xl`}>
          Tickets
        </Link>

        {usuario ? (
          <>
            <span className="hidden max-w-40 truncate text-sm text-white/80 md:block" title={usuario.email}>
              {usuario.nombre}
            </span>
            <button type="button" onClick={cerrarSesion} className={`${pildora} border border-verde hover:bg-superficie`}>
              Cerrar sesión
            </button>
          </>
        ) : (
          <>
            <Link to="/login" className={`${pildora} bg-verde text-tinta hover:bg-verde-oscuro`}>
              Iniciar sesión
            </Link>
            <Link to="/registro" className={`${pildora} hidden border border-verde hover:bg-superficie sm:block`}>
              Registrarse
            </Link>
          </>
        )}
      </div>

      {menuAbierto && (
        <nav id="menu-principal" aria-label="Principal" className="border-t border-superficie bg-negro">
          <ul className="mx-auto flex max-w-6xl flex-col px-4 py-2">
            {enlacesMenu.map((enlace) => (
              <li key={enlace.to}>
                <NavLink
                  to={enlace.to}
                  end
                  onClick={() => setMenuAbierto(false)}
                  className={({ isActive }) =>
                    `block rounded px-2 py-2 font-display italic tracking-wide hover:bg-superficie ${isActive ? 'text-verde' : ''}`
                  }
                >
                  {enlace.texto}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
      )}
    </header>
  )
}
