import { Outlet, ScrollRestoration } from 'react-router'
import { Footer } from './Footer'
import { Header } from './Header'

export function Layout() {
  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <main className="flex-1">
        <Outlet />
      </main>
      <Footer />
      {/* Al cambiar de página vuelve arriba; al ir atrás, recupera la posición anterior. */}
      <ScrollRestoration />
    </div>
  )
}
