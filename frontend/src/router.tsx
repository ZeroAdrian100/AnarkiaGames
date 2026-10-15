import { createBrowserRouter } from 'react-router'
import { Layout } from './components/Layout'
import { ErrorInesperado } from './pages/ErrorInesperado'
import { Inicio } from './pages/Inicio'
import { NoEncontrado } from './pages/NoEncontrado'

export const router = createBrowserRouter([
  {
    element: <Layout />,
    errorElement: <ErrorInesperado />,
    children: [
      { index: true, element: <Inicio /> },
      { path: '*', element: <NoEncontrado /> },
    ],
  },
])
