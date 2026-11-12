import { createBrowserRouter } from 'react-router'
import { Layout } from './components/Layout'
import { ErrorInesperado } from './pages/ErrorInesperado'
import { Inicio } from './pages/Inicio'
import { Merch } from './pages/Merch'
import { Modalidad } from './pages/Modalidad'
import { NoEncontrado } from './pages/NoEncontrado'
import { TorneoDetalle } from './pages/TorneoDetalle'
import { Torneos } from './pages/Torneos'

export const router = createBrowserRouter([
  {
    element: <Layout />,
    errorElement: <ErrorInesperado />,
    children: [
      { index: true, element: <Inicio /> },
      { path: 'torneos', element: <Torneos /> },
      { path: 'torneos/:id', element: <TorneoDetalle /> },
      { path: 'torneos/:id/modalidad', element: <Modalidad /> },
      { path: 'merch', element: <Merch /> },
      { path: '*', element: <NoEncontrado /> },
    ],
  },
])
