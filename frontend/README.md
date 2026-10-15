# Frontend de AnarkiaGames

React 19 + TypeScript + Vite + Tailwind CSS. Consume la API del backend (`../anarkiagames`).

## Requisitos

- Node.js 24 o superior.
- El backend corriendo en `http://localhost:8080`. Si corre en otra URL, copia `.env.example` como `.env` y cambia `VITE_API_URL`.

## Comandos

```bash
npm install
```

```bash
npm run dev
```

Abre `http://localhost:5173`. Este puerto es el que el backend permite por CORS.

| Comando | Qué hace |
|---|---|
| `npm run dev` | Servidor de desarrollo con recarga automática. |
| `npm run build` | Revisa los tipos con TypeScript y genera la versión de producción en `dist/`. |
| `npm run lint` | Revisa el código con oxlint. |
| `npm run preview` | Sirve la versión de `dist/` para probarla. |

## Estructura

```
src/
  api/          cliente HTTP (JWT y refresh automático) y tipos de la API
  auth/         sesión (tokens en localStorage) y contexto de usuario
  components/   piezas compartidas: Layout, Header, Footer
  pages/        una página por ruta
  router.tsx    rutas de la aplicación
  index.css     Tailwind y los colores y fuentes del diseño de Figma
```
