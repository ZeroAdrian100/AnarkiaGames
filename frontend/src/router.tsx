import { createBrowserRouter } from 'react-router'
import { Layout } from './components/Layout'
import { ErrorInesperado } from './pages/ErrorInesperado'
import { Evento } from './pages/Evento'
import { Inicio } from './pages/Inicio'
import { Merch } from './pages/Merch'
import { Modalidad } from './pages/Modalidad'
import { NoEncontrado } from './pages/NoEncontrado'
import { TicketsPorModalidad } from './pages/TicketsPorModalidad'

export const router = createBrowserRouter([
  {
    element: <Layout />,
    errorElement: <ErrorInesperado />,
    children: [
      { index: true, element: <Inicio /> },
      // Compra en tres pasos: evento, modalidad y tickets de esa modalidad.
      { path: 'tickets', element: <Evento /> },
      { path: 'tickets/modalidad', element: <Modalidad /> },
      { path: 'tickets/:modalidad', element: <TicketsPorModalidad /> },
      { path: 'merch', element: <Merch /> },
      { path: '*', element: <NoEncontrado /> },
    ],
  },
])
