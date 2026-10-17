import { useState } from 'react'
import { Link, NavLink } from 'react-router'
import { useAuth } from '../auth/useAuth'

const enlacesMenu = [
  { to: '/', texto: 'Inicio' },
  { to: '/torneos', texto: 'Torneos' },
  { to: '/merch', texto: 'Merch' },
]

const pildora = 'rounded-full px-3 py-1 font-display text-sm italic tracking-wide'

export function Header() {
  const { usuario, cerrarSesion } = useAuth()
  const [menuAbierto, setMenuAbierto] = useState(false)

  return (
    <header className="sticky top-0 z-20 bg-negro">
      <div className="mx-auto flex h-14 max-w-8xl items-center gap-3 px-4">
        <button
          type="button"
          aria-label={menuAbierto ? 'Cerrar menú' : 'Abrir menú'}
          aria-expanded={menuAbierto}
          aria-controls="menu-principal"
          onClick={() => setMenuAbierto((abierto) => !abierto)}
          className="rounded p-1 hover:bg-superficie focus-visible:outline-2 focus-visible:outline-verde"
        >
          <svg viewBox="0 0 24 24" className="size-7" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
            <path d="M4 7h16M4 12h16M4 17h16" strokeLinecap="round" />
          </svg>
        </button>

        <Link to="/" className="mr-auto font-display text-xl italic tracking-wide sm:text-2xl">
          Anarkia Games
        </Link>

        <Link to="/torneos" className={`${pildora} hidden rounded-md bg-verde px-6 text-tinta hover:bg-verde-oscuro sm:block`}>
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
