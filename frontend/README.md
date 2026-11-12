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

## Probar con datos de ejemplo

Desde `anarkiagames`, levanta el backend de demostración (H2 en memoria, no toca SQL Server):

```bash
./mvnw spring-boot:test-run
```

Carga los 7 torneos del diseño y un admin `admin@demo.com` / `demo12345`. Las imágenes de los torneos están en `public/torneos/` y se referencian como `/torneos/<nombre>.webp` en el campo `imagenUrl`.

## Fuentes

El diseño usa **BLACK MONSTER** (títulos) y **Berlin Sans FB** (textos). Si están instaladas en el equipo, el navegador las usa; si no, usa Bangers y Nunito de Google Fonts. No se incluyen en el repo porque sus licencias no permiten redistribuirlas.

## Estructura

```
src/
  api/          cliente HTTP (JWT y refresh automático) y tipos de la API
  auth/         sesión (tokens en localStorage) y contexto de usuario
  assets/       imágenes y logos del diseño de Figma (WebP)
  components/   piezas compartidas: Layout, Header, Footer, ImagenTorneo, estilos de botones
  pages/        una página por ruta: Inicio, Torneos, TorneoDetalle, Modalidad, Merch
  utils/        formato de soles, fechas y horas
  router.tsx    rutas de la aplicación
  index.css     Tailwind y los colores y fuentes del diseño de Figma
```
