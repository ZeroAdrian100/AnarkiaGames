# Documentación técnica de AnarkiaGames

Backend REST para la venta de tickets de torneos de videojuegos. Un cliente compra tickets como **competidor** o como **espectador**; un administrador crea torneos y tipos de ticket.

Contenido:

1. [Arquitectura](#1-arquitectura)
2. [Estructura del código](#2-estructura-del-código)
3. [Modelo de datos](#3-modelo-de-datos)
4. [Reglas de negocio](#4-reglas-de-negocio)
5. [Seguridad y autenticación](#5-seguridad-y-autenticación)
6. [API REST](#6-api-rest)
7. [Manejo de errores](#7-manejo-de-errores)
8. [Pagos](#8-pagos)
9. [Configuración](#9-configuración)
10. [Ejecución y pruebas](#10-ejecución-y-pruebas)
11. [Base de datos y migraciones](#11-base-de-datos-y-migraciones)
12. [Pendientes](#12-pendientes)

---

## 1. Arquitectura

Aplicación Spring Boot en capas:

```
Cliente (frontend Vite en localhost:5173)
        │  HTTP + JSON, Authorization: Bearer <access_token>
        ▼
JwtAuthFilter ──► SecurityConfig (reglas por ruta y rol)
        │
        ▼
Controladores (controller)   ← DTOs de entrada/salida (records) + Bean Validation
        │
        ▼
Servicios (service)          ← reglas de negocio y transacciones
        │                └──► PaymentGateway (mock | culqi)
        ▼
Repositorios (repository)    ← Spring Data JPA
        │
        ▼
SQL Server
```

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 25 |
| Framework | Spring Boot 4.1 (Web, Data JPA, Security, Validation) |
| Base de datos | SQL Server (`mssql-jdbc`), migraciones con Flyway |
| Tests | JUnit 5, Mockito, MockMvc y H2 en memoria |
| Autenticación | JWT (`jjwt` 0.12) |
| Pagos | Culqi (`culqi-java`) o simulación |
| Variables de entorno | `spring-dotenv` (lee un archivo `.env`) |
| Documentación de la API | springdoc-openapi (Swagger UI) |
| Monitoreo | Spring Boot Actuator (`/actuator/health`) |
| Utilidades | Lombok |

---

## 2. Estructura del código

Paquete raíz: `com.utp.anarkiagames` (en `anarkiagames/src/main/java`).

### `config`

| Clase | Responsabilidad |
|---|---|
| `AsyncConfig` | Habilita `@Async`: los correos se envían en otro hilo. |
| `OpenApiConfig` | Título de la API y esquema de seguridad `bearerAuth` (JWT) para Swagger UI. |
| `AppConfig` | Beans de autenticación: `UserDetailsService` (busca usuarios por email), `AuthenticationProvider`, `AuthenticationManager` y `PasswordEncoder` (BCrypt). |
| `SecurityConfig` | Cadena de filtros: CORS, sesión stateless, reglas de acceso por ruta, respuesta 401 para peticiones sin autenticar y registro de `JwtAuthFilter`. |
| `AdminInicial` | Crea el primer ADMIN al arrancar a partir de `ADMIN_EMAIL` y `ADMIN_PASSWORD` (sección 9). |
| `JwtAuthFilter` | Lee `Authorization: Bearer <token>`. Si el access token es válido, autentica la petición con la entidad `User` como principal. Un token ausente, inválido o expirado deja la petición sin autenticar. |

### `controller`

Controladores REST y DTOs. Los DTOs son `record` de Java.

| Clase | Ruta base | Descripción |
|---|---|---|
| `AuthController` | `/auth` | Registro, login, renovación de token y recuperación de contraseña. |
| `TorneoController` | `/torneos` | Consulta pública de torneos. |
| `InscripcionController` | `/torneos` | Compra de tickets, listado de las inscripciones del usuario y sus códigos de ticket. |
| `TorneoAdminController` | `/admin/torneos` | Crear, editar y cancelar torneos; resumen de stock e ingresos; inscritos (solo ADMIN). |
| `TipoTicketController` | `/admin/torneos/{torneoId}/tipos-ticket` | Crear, editar y eliminar tipos de ticket (solo ADMIN). |
| `InscripcionAdminController` | `/admin/inscripciones` | Reembolsar una compra (solo ADMIN). |
| `UsuarioAdminController` | `/admin/usuarios` | Listar y buscar usuarios; cambiar su rol (solo ADMIN). |
| `CheckinAdminController` | `/admin/checkin` | Check-in de un ticket en la puerta del evento (solo ADMIN). |

DTOs de entrada (validados con `@Valid`): `RegisterRequest`, `LoginRequest`, `TorneoRequest`, `TipoTicketRequest`, `ActualizarTipoTicketRequest`, `InscripcionRequest`, `CambiarRolRequest`, `RecuperarPasswordRequest`, `RestablecerPasswordRequest`.

DTOs de salida: `TokenResponse`, `TorneoResponse`, `TorneoDetalleResponse`, `TipoTicketResponse`, `InscripcionResponse`, `MiInscripcionResponse`, `InscripcionAdminResponse`, `ResumenTorneoResponse`, `CancelacionResponse`, `UsuarioResponse`, `TicketResponse`, `CheckinResponse`, `MensajeResponse` y `PaginaResponse<T>` (listados paginados).

### `service`

| Clase | Responsabilidad |
|---|---|
| `CorreoService` | Envía los correos (confirmación de compra y recuperación de contraseña) en otro hilo. Sin `MAIL_USERNAME`, los escribe en el log. |
| `NotificacionesListener` | Escucha `CompraConfirmadaEvent` y pide el correo de confirmación **después del commit** de la compra. |
| `RecuperacionPasswordService` | Genera los enlaces de recuperación y cambia la contraseña con un token válido. |
| `AuthService` | Registra clientes (siempre con rol `CLIENTE`), autentica y renueva el access token a partir de un refresh token. |
| `JwtService` | Genera y valida JWT. Distingue access y refresh token con el claim `type`. |
| `TorneoService` | Crea, lista, detalla, edita y cancela torneos, y arma el resumen de stock e ingresos. |
| `TipoTicketService` | Crea, edita y elimina el tipo de ticket de un torneo; solo uno por tipo de participación. |
| `InscripcionService` | Compra de tickets con todas las reglas de negocio (sección 4), "mis inscripciones", inscritos de un torneo y reembolsos. |
| `UsuarioService` | Lista y busca usuarios; cambia su rol. |
| `TicketService` | Genera los tickets de cada compra, lista los del usuario y hace el check-in. |
| `IntentosLoginService` | Cuenta los logins fallidos por email e IP y bloquea tras 5 fallos en 15 minutos (sección 5). |

### `service.payment`

| Clase | Responsabilidad |
|---|---|
| `PaymentGateway` | Interfaz con `cobrar(...)` y `reembolsar(...)`. |
| `PaymentResult` | Resultado: `exitoso`, `referenciaId` (id del cargo o reembolso) y `mensajeError`. |
| `MockPaymentGateway` | Simulación. Se activa con `payment.gateway=mock` o si la propiedad falta. |
| `CulqiPaymentGateway` | Integración real con Culqi. Se activa con `payment.gateway=culqi`. |

### `repository`

| Interfaz | Consultas relevantes |
|---|---|
| `UserRepository` | `findByEmail`, `existsByEmail`, `existsByRole`, búsqueda paginada por nombre o email. |
| `TorneoRepository` | `findByIdForUpdate`: lee el torneo con bloqueo pesimista (`PESSIMISTIC_WRITE`). `marcarCancelado`. |
| `TipoTicketRepository` | `findByTorneo`, `findByTorneoAndTipo`. |
| `TokenRecuperacionRepository` | `findByTokenHash`, `eliminarPendientes` y `marcarUsado` (solo si el token no se usó antes). |
| `TicketRepository` | `findByInscripcionOrderByIdAsc`, `findByCodigo` y `marcarUsado` (solo si el ticket no se usó antes). |
| `InscripcionRepository` | `findByUsuario`, `findByTipoTicketTorneo` (paginado), `existsConOtroTipoEnTorneo`, `sumCantidadByTipoTicket` (vendidos) y `sumMontoByTipoTicket` (ingresos), solo de compras `PAGADA`. `cambiarEstado` actualiza el estado solo si no cambió antes. |

### `exception`

| Clase | Código HTTP |
|---|---|
| `SolicitudInvalidaException` | 400 |
| `RecursoNoEncontradoException` | 404 |
| `ConflictoException` | 409 |
| `PagoRechazadoException` | 402 |
| `ErrorPasarelaException` | 502 |
| `DemasiadosIntentosException` | 429 |
| `GlobalExceptionHandler` | Convierte toda excepción en un `ErrorResponse` (sección 7). |

### `model`

Entidades JPA y enums. Ver la sección 3.

---

## 3. Modelo de datos

```
users 1 ──── * inscripciones * ──── 1 tipo_tickets * ──── 1 torneos
```

### `User` → tabla `users`

| Campo | Tipo | Notas |
|---|---|---|
| `id` | `Long` | PK autoincremental. |
| `nombre` | `String` | Obligatorio. |
| `email` | `String` | Único. Es el "username" para Spring Security. |
| `passwordHash` | `String` | Hash BCrypt. |
| `role` | `Role` | `ADMIN` o `CLIENTE`. |
| `createdAt` | `LocalDateTime` | Lo llena Hibernate al insertar (`@CreationTimestamp`). |

`User` implementa `UserDetails`; su autoridad es `ROLE_<role>`.

### `Torneo` → tabla `torneos`

| Campo | Tipo | Notas |
|---|---|---|
| `id` | `Long` | PK. |
| `nombre` | `String` | Obligatorio. |
| `juego` | `String` | Obligatorio. |
| `fechaInicio` | `LocalDateTime` | Obligatorio. La venta cierra en esta fecha. |
| `estado` | `EstadoTorneo` | `ACTIVO` (por defecto) o `CANCELADO`. |
| `createdAt` | `LocalDateTime` | `@CreationTimestamp`. |

### `TipoTicket` → tabla `tipo_tickets`

Restricción única: `(torneo_id, tipo)`. Cada torneo tiene como máximo un tipo COMPETIDOR y un tipo ESPECTADOR.

| Campo | Tipo | Notas |
|---|---|---|
| `id` | `Long` | PK. |
| `torneo` | `Torneo` | FK `torneo_id`. |
| `tipo` | `TipoParticipacion` | `COMPETIDOR` o `ESPECTADOR`. |
| `stockMaximo` | `int` | Cupo total de tickets. |
| `precio` | `long` | Precio unitario **en céntimos** (S/ 25.00 = `2500`). |
| `descripcion` | `String` | Opcional. |
| `imagenUrl` | `String` | Opcional. |

### `Inscripcion` → tabla `inscripciones`

Una compra. Un espectador puede tener varias inscripciones en el mismo torneo.

| Campo | Tipo | Notas |
|---|---|---|
| `id` | `Long` | PK. |
| `usuario` | `User` | FK `usuario_id`. |
| `tipoTicket` | `TipoTicket` | FK `tipo_ticket_id`. |
| `cantidad` | `int` | Tickets de esta compra. Siempre 1 para COMPETIDOR. |
| `monto` | `long` | Total cobrado en céntimos (`precio × cantidad`). |
| `fechaCompra` | `LocalDateTime` | Momento de la compra. |
| `referenciaPago` | `String` | Id del cargo en la pasarela. Columna `culqi_charge_id`. |
| `estado` | `EstadoInscripcion` | `PAGADA` (por defecto) o `REEMBOLSADA`. Solo las `PAGADA` cuentan para el stock, los ingresos y las reglas de compra. |
| `referenciaReembolso` | `String` | Id del reembolso en la pasarela, si se reembolsó. |

### `TokenRecuperacion` → tabla `tokens_recuperacion`

| Campo | Tipo | Notas |
|---|---|---|
| `id` | `Long` | PK. |
| `usuario` | `User` | FK `usuario_id`. |
| `tokenHash` | `String` | SHA-256 del token enviado por correo. El token en claro nunca se guarda. |
| `expiraEn` | `LocalDateTime` | Momento en que vence (15 minutos, `jwt.reset.expiration`). |
| `usadoEn` | `LocalDateTime` | `null` hasta que se usa; cada token sirve una sola vez. |

### `Ticket` → tabla `tickets`

Una entrada individual. Cada compra genera tantos tickets como su `cantidad`: un espectador que compra 3 recibe 3 códigos.

| Campo | Tipo | Notas |
|---|---|---|
| `id` | `Long` | PK. |
| `inscripcion` | `Inscripcion` | FK `inscripcion_id`. |
| `codigo` | `String` | Único. 32 caracteres hexadecimales (128 bits de `SecureRandom`). Es el valor del QR. |
| `fechaUso` | `LocalDateTime` | Momento del check-in; `null` si no se usó. |

Un ticket es **válido** si no se usó, su compra sigue `PAGADA` y el torneo está `ACTIVO`. Un reembolso o una cancelación lo invalidan sin modificar la tabla.

---

## 4. Reglas de negocio

Todas se aplican en `InscripcionService.comprar`:

1. **Venta solo en torneos activos y antes del inicio.** Si el torneo está `CANCELADO` o `fechaInicio` ya pasó, la compra devuelve 409.
2. **Un tipo de participación por torneo.** Si el usuario ya tiene una inscripción del otro tipo en ese torneo, devuelve 409.
3. **Competidor:**
   - Solo puede pedir 1 ticket. Si pide más, devuelve 400.
   - Solo puede inscribirse una vez por torneo. Si repite, devuelve 409.
4. **Espectador:**
   - Puede pedir hasta `MAX_ESPECTADOR_POR_COMPRA` (5) tickets por compra. Si pide más, devuelve 400.
   - Puede comprar varias veces.
5. **Stock.** Los tickets vendidos son la suma de `cantidad` de las inscripciones `PAGADA` de ese tipo; un reembolso libera los cupos. Si la cantidad pedida supera los disponibles, devuelve 409.
6. **Concurrencia.** El método es `@Transactional` y bloquea la fila del torneo (`findByIdForUpdate`) hasta terminar. Dos compras del mismo torneo se ejecutan una después de la otra, así que el stock nunca se supera.
7. **Pago.** Se cobra `precio × cantidad`. Si el pago es rechazado, devuelve 402 y no se guarda nada.
8. **Reembolso automático.** Si el cobro pasa pero la inscripción no se puede guardar, se llama a `PaymentGateway.reembolsar`. Si el reembolso también falla, el error queda en el log con la referencia del cargo para reembolsarlo a mano.

Otras reglas:

- El registro público siempre crea usuarios `CLIENTE`. El primer `ADMIN` se crea con `AdminInicial` (sección 9).
- Un torneo admite un solo tipo de ticket por tipo de participación (`TipoTicketService`).
- Al crear o editar un torneo, `fechaInicio` debe estar en el futuro (`@Future`).
- Las compras reembolsadas no cuentan para las reglas 2 y 3: un competidor reembolsado puede volver a inscribirse, incluso como espectador.
- Cada compra genera sus tickets en la misma transacción. Si la generación falla, la compra se deshace y el cargo se reembolsa (regla 8).

### Tickets y check-in

- El código del ticket es lo que muestra el QR, y quien lo tiene puede entrar. Por eso es aleatorio (`SecureRandom`, 128 bits) y solo su dueño lo ve: si la compra es de otro usuario, la API responde 404 sin revelar que existe.
- El backend entrega el código y el frontend dibuja el QR, por ejemplo con la librería `qrcode` de npm.
- El check-in lo hace un ADMIN. Rechaza con 409 los tickets ya usados, reembolsados o de un torneo cancelado.
- El ticket se marca como usado con un `UPDATE` condicional (`fecha_uso IS NULL`). Si dos lectores escanean el mismo QR a la vez, solo uno entra.

### Administración

- **Editar un torneo:** solo si no está `CANCELADO`.
- **Cancelar un torneo:**
  1. El torneo pasa a `CANCELADO`. Si hay una compra en curso, la cancelación espera a que termine; las siguientes compras ya ven el torneo cancelado.
  2. Se reembolsa cada compra `PAGADA`. Cada reembolso se guarda por separado, así un fallo no deshace los reembolsos que la pasarela ya hizo.
  3. La respuesta indica cuántas compras se reembolsaron y cuántas quedaron pendientes. Las pendientes siguen `PAGADA`: se reintentan cancelando de nuevo, o reembolsándolas una por una.
- **Reembolsar una compra:** la compra se marca `REEMBOLSADA` *antes* de llamar a la pasarela. Si dos admins la reembolsan a la vez, solo uno gana el cambio de estado y la pasarela no reembolsa dos veces. Si la pasarela falla, la compra vuelve a `PAGADA` y la API responde 502.
- **Editar un tipo de ticket:** se puede cambiar stock, precio, descripción e imagen, pero no el tipo de participación. El stock no puede quedar por debajo de lo vendido. El nuevo precio solo afecta a las compras futuras. La edición bloquea el torneo igual que una compra.
- **Eliminar un tipo de ticket:** solo si nunca tuvo ventas, ni siquiera reembolsadas, para conservar el historial.
- **Cambiar el rol de un usuario:** un admin no puede cambiar su propio rol. Así siempre queda al menos un `ADMIN`.

---

## 5. Seguridad y autenticación

### Tokens

`POST /auth/login` y `POST /auth/register` devuelven dos JWT firmados con HMAC (`jwt.secret`):

| Token | Claim `type` | Duración (por defecto) | Uso |
|---|---|---|---|
| `access_token` | `access` | 24 h (`jwt.expiration`) | Header `Authorization: Bearer <token>` en cada petición. |
| `refresh_token` | `refresh` | 7 días (`jwt.refresh.expiration`) | Solo en `POST /auth/refresh` para obtener un nuevo access token. |

Claims de cada token: `sub` (email), `jti` (id del usuario), `name`, `role`, `type`, `iat` y `exp`.

Un refresh token no sirve como access token, ni un access token como refresh: `JwtService` verifica el claim `type`. Los tokens emitidos antes de este cambio no tienen `type` y ya no son válidos; el usuario debe volver a iniciar sesión.

### Reglas de acceso (`SecurityConfig`)

| Ruta | Acceso |
|---|---|
| `/`, `/auth/**` | Público |
| `/actuator/health` | Público |
| `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**` | Público (deshabilitado en el perfil `prod`) |
| `GET /torneos/mis-inscripciones/**` | Autenticado |
| `GET /torneos/**` | Público |
| `/admin/**` | Rol `ADMIN` |
| Cualquier otra | Autenticado |

- Petición sin token válido a una ruta protegida: **401**.
- Usuario autenticado sin el rol requerido: **403**.

### Recuperación de contraseña

1. `POST /auth/olvide-password` responde siempre lo mismo, exista o no el email, para no revelar qué emails tienen cuenta.
2. Si el email existe, se borran sus enlaces pendientes y se envía uno nuevo: `{FRONTEND_URL}/restablecer-password?token=...`. El token tiene 256 bits aleatorios y en la BD solo se guarda su hash SHA-256.
3. `POST /auth/restablecer-password` acepta el token si existe, no se usó y no venció (15 minutos). Lo marca como usado con un `UPDATE` condicional, así un enlace no sirve dos veces aunque llegue dos veces a la vez.
4. Los JWT emitidos antes del cambio siguen siendo válidos hasta que vencen, porque los tokens no se guardan en el servidor.

### Correos

- Se envían por SMTP (Gmail, puerto 587 con STARTTLS) usando `MAIL_USERNAME` y una contraseña de aplicación en `MAIL_PASSWORD`.
- Se envían en otro hilo (`@Async`). Un fallo de envío queda en el log y no afecta a la compra ni al pedido de recuperación.
- La confirmación de compra se envía solo después del commit (`@TransactionalEventListener(AFTER_COMMIT)`): si la compra se deshace, no sale ningún correo. Incluye torneo, fecha, tipo, cantidad, total y los códigos de los tickets.
- Sin `MAIL_USERNAME`, el contenido del correo se escribe en el log, incluido el enlace de recuperación. Sirve en desarrollo; en producción hay que configurar el SMTP.
- El health check de correo de Actuator está deshabilitado, para no abrir una conexión SMTP en cada consulta a `/actuator/health`.

### Límite de intentos de login

`IntentosLoginService` cuenta los logins fallidos por combinación de email (sin distinguir mayúsculas) e IP:

- Tras **5 fallos en 15 minutos**, el login responde **429** hasta que se cumplen los 15 minutos desde el primer fallo, aunque la contraseña sea correcta.
- Un login exitoso reinicia el contador.
- Se cuenta por email e IP, y no solo por email, para que un atacante no pueda bloquear la cuenta de otra persona desde su propia IP.
- El contador vive en memoria: se reinicia al reiniciar la app y no se comparte entre varias instancias.

### Documentación interactiva y health check

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`. Lista todos los endpoints y permite probarlos. Para los protegidos, pega el `access_token` en el botón **Authorize**. La especificación OpenAPI en JSON está en `/v3/api-docs`. En el perfil `prod` ambos están deshabilitados.
- **Health check:** `GET /actuator/health` responde `{"status":"UP"}` si la app y la BD funcionan. Es el único endpoint de Actuator expuesto, y no muestra detalles internos.

### CORS

Los orígenes permitidos salen de `app.cors.allowed-origins` (por defecto `http://localhost:5173`). Se permiten credenciales y los métodos GET, POST, PUT, DELETE, PATCH y OPTIONS.

---

## 6. API REST

Todos los cuerpos son JSON. Los montos van en céntimos. Las fechas usan ISO-8601 sin zona horaria (`2026-11-20T18:00:00`).

### Autenticación

#### `POST /auth/register` → 201

```json
{ "name": "Ana", "email": "ana@mail.com", "password": "secreta123" }
```

Validaciones:
- `name`: obligatorio, máximo 100 caracteres.
- `email`: formato válido.
- `password`: entre 8 y 72 caracteres.

Respuesta:

```json
{ "access_token": "eyJ...", "refresh_token": "eyJ..." }
```

Errores: 400 (validación), 409 (email ya registrado).

#### `POST /auth/login` → 200

```json
{ "email": "ana@mail.com", "password": "secreta123" }
```

Respuesta: igual que el registro. Errores: 400, 401 ("Email o contraseña incorrectos") y 429 (demasiados intentos fallidos; ver sección 5).

#### `POST /auth/refresh` → 200

Header: `Authorization: Bearer <refresh_token>`.

Respuesta: un `access_token` nuevo y el mismo `refresh_token`. Error: 401 si el token es inválido, expirado o no es de tipo refresh.

#### `POST /auth/olvide-password` → 200

```json
{ "email": "ana@mail.com" }
```

Respuesta (igual exista o no el email):

```json
{ "mensaje": "Si el email está registrado, te enviamos un enlace para restablecer la contraseña" }
```

Error: 400 si el email no tiene formato válido.

#### `POST /auth/restablecer-password` → 200

```json
{ "token": "<token del enlace>", "password": "nuevaClave99" }
```

Respuesta: `{ "mensaje": "Contraseña actualizada" }`. Error: 400 si el token es inválido, ya se usó o venció, o si la contraseña no tiene entre 8 y 72 caracteres.

### Torneos (público)

#### `GET /torneos` → 200

```json
[ { "id": 1, "nombre": "Copa Anarkia", "juego": "Tekken 8", "fechaInicio": "2026-11-20T18:00:00" } ]
```

#### `GET /torneos/{torneoId}` → 200

```json
{
  "id": 1,
  "nombre": "Copa Anarkia",
  "juego": "Tekken 8",
  "fechaInicio": "2026-11-20T18:00:00",
  "tiposTicket": [
    {
      "id": 3, "torneoId": 1, "tipo": "ESPECTADOR",
      "stockMaximo": 100, "disponibles": 87, "precio": 2500,
      "descripcion": "Entrada general", "imagenUrl": null
    }
  ]
}
```

Error: 404 si el torneo no existe.

### Inscripciones (autenticado)

#### `POST /torneos/{torneoId}/inscripciones` → 201

```json
{ "tipo": "ESPECTADOR", "cantidad": 3, "tokenId": "tkn_..." }
```

| Campo | Obligatorio | Descripción |
|---|---|---|
| `tipo` | Sí | `COMPETIDOR` o `ESPECTADOR`. |
| `cantidad` | No | Por defecto 1. Competidor: 1. Espectador: 1 a 5. |
| `tokenId` | Con Culqi | Token de tarjeta generado en el frontend. Con el mock, `tok_rechazado` simula un rechazo. |

Respuesta:

```json
{ "id": 15, "torneoId": 1, "tipo": "ESPECTADOR", "cantidad": 3, "monto": 7500, "fechaCompra": "2026-10-13T12:30:00" }
```

Errores: 400, 401, 402 (pago rechazado), 404 (torneo o tipo de ticket inexistente) y 409 (venta cerrada, otro tipo ya comprado, competidor repetido o sin cupos).

#### `GET /torneos/mis-inscripciones` → 200

```json
[
  {
    "inscripcionId": 15, "torneoId": 1, "torneoNombre": "Copa Anarkia",
    "juego": "Tekken 8", "fechaInicioTorneo": "2026-11-20T18:00:00",
    "tipo": "ESPECTADOR", "cantidad": 3, "estado": "PAGADA", "fechaCompra": "2026-10-13T12:30:00"
  }
]
```

`GET /torneos` y `GET /torneos/{torneoId}` también devuelven el campo `estado` del torneo (`ACTIVO` o `CANCELADO`).

#### `GET /torneos/mis-inscripciones/{inscripcionId}/tickets` → 200

Tickets de una compra del usuario, uno por unidad comprada.

```json
[
  { "codigo": "9f2c4e1a7b3d5f60c8e2a4b6d8f0e1c3", "valido": true, "fechaUso": null }
]
```

Errores: 401, y 404 si la compra no existe o es de otro usuario.

### Administración (rol ADMIN)

#### `POST /admin/checkin/{codigo}` → 200

Sin cuerpo. Marca el ticket como usado.

```json
{
  "codigo": "9f2c4e1a7b3d5f60c8e2a4b6d8f0e1c3", "torneoId": 1, "torneoNombre": "Copa Anarkia",
  "tipo": "ESPECTADOR", "usuarioNombre": "Ana", "fechaUso": "2026-11-20T17:45:12"
}
```

Errores: 404 (código inexistente) y 409 (ticket ya usado, compra reembolsada o torneo cancelado).

#### `POST /admin/torneos` → 201

```json
{ "nombre": "Copa Anarkia", "juego": "Tekken 8", "fechaInicio": "2026-11-20T18:00:00" }
```

Validaciones: `nombre` y `juego` son obligatorios; `fechaInicio` es obligatoria y debe ser futura. La respuesta es un `TorneoResponse`.

#### `POST /admin/torneos/{torneoId}/tipos-ticket` → 201

```json
{ "tipo": "COMPETIDOR", "stockMaximo": 32, "precio": 5000, "descripcion": "Cupo de jugador", "imagenUrl": null }
```

Validaciones: `tipo` es obligatorio; `stockMaximo` y `precio` deben ser mayores que 0. La respuesta es un `TipoTicketResponse` con `disponibles` igual a `stockMaximo`.

Errores: 404 (torneo inexistente) y 409 (ya existe ese tipo de ticket en el torneo).

#### `PUT /admin/torneos/{torneoId}` → 200

Mismo cuerpo y validaciones que la creación. Errores: 404, y 409 si el torneo está cancelado.

#### `POST /admin/torneos/{torneoId}/cancelar` → 200

Sin cuerpo. Cancela el torneo y reembolsa sus compras (sección 4). Se puede repetir para reintentar los reembolsos pendientes.

```json
{ "torneoId": 1, "reembolsadas": 12, "pendientes": 0 }
```

#### `GET /admin/torneos/{torneoId}/resumen` → 200

```json
{
  "torneoId": 1, "nombre": "Copa Anarkia", "estado": "ACTIVO",
  "tiposTicket": [
    { "tipoTicketId": 3, "tipo": "ESPECTADOR", "precio": 2500, "stockMaximo": 100,
      "vendidos": 13, "disponibles": 87, "ingresos": 32500 }
  ],
  "ingresosTotales": 32500
}
```

Solo cuenta las compras `PAGADA`. Los montos van en céntimos.

#### `GET /admin/torneos/{torneoId}/inscripciones` → 200

Compras del torneo, paginadas. Parámetros: `page` (desde 0), `size` (por defecto 20, máximo 100) y `sort` (por defecto `fechaCompra,desc`).

```json
{
  "contenido": [
    {
      "id": 15, "usuarioId": 4, "usuarioNombre": "Ana", "usuarioEmail": "ana@mail.com",
      "tipo": "ESPECTADOR", "cantidad": 3, "monto": 7500, "estado": "PAGADA",
      "fechaCompra": "2026-10-13T12:30:00", "referenciaPago": "chr_...", "referenciaReembolso": null
    }
  ],
  "pagina": 0, "tamano": 20, "totalElementos": 1, "totalPaginas": 1
}
```

#### `PUT /admin/torneos/{torneoId}/tipos-ticket/{tipoTicketId}` → 200

```json
{ "stockMaximo": 120, "precio": 3000, "descripcion": "Entrada general", "imagenUrl": null }
```

La respuesta es un `TipoTicketResponse`. Errores: 400, 404 (el tipo de ticket no existe o es de otro torneo) y 409 (el stock queda por debajo de lo vendido).

#### `DELETE /admin/torneos/{torneoId}/tipos-ticket/{tipoTicketId}` → 204

Errores: 404, y 409 si el tipo de ticket ya tiene ventas.

#### `POST /admin/inscripciones/{inscripcionId}/reembolsar` → 200

Sin cuerpo. La respuesta es la compra con `estado: "REEMBOLSADA"` y su `referenciaReembolso`. Errores: 404, 409 (la compra ya fue reembolsada) y 502 (la pasarela falló; la compra sigue `PAGADA`).

#### `GET /admin/usuarios` → 200

Parámetros: `buscar` (opcional; filtra por nombre o email sin distinguir mayúsculas), `page`, `size` (por defecto 20, máximo 100) y `sort` (por defecto `id`).

```json
{
  "contenido": [
    { "id": 4, "nombre": "Ana", "email": "ana@mail.com", "role": "CLIENTE", "createdAt": "2026-10-01T09:00:00" }
  ],
  "pagina": 0, "tamano": 20, "totalElementos": 1, "totalPaginas": 1
}
```

Error: 400 si `sort` usa una propiedad que no existe.

#### `PATCH /admin/usuarios/{usuarioId}/rol` → 200

```json
{ "role": "ADMIN" }
```

La respuesta es el `UsuarioResponse` actualizado. Errores: 400, 404, y 409 si el admin intenta cambiar su propio rol.

---

## 7. Manejo de errores

Todo error devuelve el mismo formato (`exception.ErrorResponse`):

```json
{
  "timestamp": "2026-10-13T12:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Solo quedan 2 cupos para ESPECTADOR"
}
```

| Origen | Status |
|---|---|
| Validación de DTO (`MethodArgumentNotValidException`) | 400. El mensaje lista `campo: error` separados por `;`. |
| JSON mal formado o enum desconocido | 400 |
| `sort` con una propiedad inexistente | 400 |
| `SolicitudInvalidaException` | 400 |
| Credenciales o refresh token inválidos | 401 |
| `PagoRechazadoException` | 402 |
| `ErrorPasarelaException` (la pasarela no pudo reembolsar) | 502 |
| `DemasiadosIntentosException` (login bloqueado) | 429 |
| `RecursoNoEncontradoException` | 404 |
| `ConflictoException`, violación de restricción en BD | 409 |
| Excepciones propias de Spring MVC (ruta inexistente, método no permitido, etc.) | El status que traen |
| Cualquier otra | 500 "Ocurrió un error inesperado". El detalle queda en el log. |

---

## 8. Pagos

La propiedad `payment.gateway` elige la implementación de `PaymentGateway`:

| Valor | Clase | Comportamiento |
|---|---|---|
| `mock` (por defecto) | `MockPaymentGateway` | Todo cobro es exitoso con referencia `chr_mock_<uuid>`, salvo el token `tok_rechazado`, que simula un rechazo. Los reembolsos siempre son exitosos. |
| `culqi` | `CulqiPaymentGateway` | Cobra en soles (PEN) con la API de Culqi usando `culqi.secret_key`. El reembolso usa el motivo `solicitud_comprador`. Los errores de red se devuelven como pago fallido, no como excepción. |

Para agregar otro proveedor (por ejemplo Mercado Pago), basta con crear una clase que implemente `PaymentGateway` con `@ConditionalOnProperty(name = "payment.gateway", havingValue = "<nombre>")`. El resto del código no cambia.

---

## 9. Configuración

Archivo base: `anarkiagames/src/main/resources/application.properties`. Los valores sensibles se leen de variables de entorno o del archivo `anarkiagames/.env`. Ese archivo está ignorado por git y nunca debe subirse.

### Perfiles

| Perfil | Cómo se activa | Diferencia |
|---|---|---|
| `dev` | Por defecto, si no se define `SPRING_PROFILES_ACTIVE` | Muestra el SQL de Hibernate (`application-dev.properties`). |
| `prod` | `SPRING_PROFILES_ACTIVE=prod` | No muestra SQL y deshabilita Swagger UI y `/v3/api-docs` (`application-prod.properties`). |
| `test` | Lo activan los tests con `@ActiveProfiles("test")` | H2 en memoria (`src/test/resources/application-test.properties`). |

### Variables de entorno

| Variable | Propiedad | Obligatoria | Descripción |
|---|---|---|---|
| `URL_DATABASE` | `spring.datasource.url` | Sí | Ejemplo: `jdbc:sqlserver://localhost:1433;databaseName=anarkiagames;encrypt=false` |
| `USER_DATABASE` | `spring.datasource.username` | Sí | Usuario de SQL Server. |
| `PASSWORD_DATABASE` | `spring.datasource.password` | Sí | Contraseña de SQL Server. |
| `JWT_SECRET` | `jwt.secret` | Sí | Clave en Base64 de al menos 256 bits. |
| `PAYMENT_GATEWAY` | `payment.gateway` | No | `mock` (por defecto) o `culqi`. |
| `CULQI_SECRET_KEY` | `culqi.secret_key` | Solo con Culqi | Llave secreta (`sk_test_...` o `sk_live_...`). |
| `MAIL_USERNAME` | `spring.mail.username` | No | Cuenta de Gmail que envía los correos. Sin valor, los correos se escriben en el log. |
| `MAIL_PASSWORD` | `spring.mail.password` | No | Contraseña de aplicación de Google (no la contraseña de la cuenta). |
| `FRONTEND_URL` | `app.frontend-url` | No | Base de los enlaces de los correos. Por defecto `http://localhost:5173`. |
| `CORS_ALLOWED_ORIGINS` | `app.cors.allowed-origins` | No | Orígenes separados por coma. Por defecto `http://localhost:5173`. |
| `ADMIN_EMAIL` | `app.admin.email` | No | Email del administrador inicial. |
| `ADMIN_PASSWORD` | `app.admin.password` | No | Contraseña del administrador inicial (mínimo 8 caracteres). |
| `ADMIN_NOMBRE` | `app.admin.nombre` | No | Nombre del administrador inicial. Por defecto `Administrador`. |
| `SPRING_PROFILES_ACTIVE` | — | No | `prod` en producción. |

### Otras propiedades

| Propiedad | Valor | Descripción |
|---|---|---|
| `jwt.expiration` | `86400000` | Duración del access token en ms (24 h). |
| `jwt.refresh.expiration` | `604800000` | Duración del refresh token en ms (7 días). |
| `jwt.reset.expiration` | `900000` | Reservada para la recuperación de contraseña (aún sin uso). |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Hibernate no modifica tablas; solo verifica que coincidan con las entidades. |
| `spring.flyway.baseline-on-migrate` | `true` | Ver la sección 11. |
| `server.port` | `8080` | Puerto HTTP. |

### Administrador inicial

El registro público solo crea clientes. Para tener el primer ADMIN, define `ADMIN_EMAIL` y `ADMIN_PASSWORD` antes de arrancar. Al iniciar, `AdminInicial`:

1. No hace nada si falta alguno de los dos valores o si ya existe algún ADMIN.
2. No hace nada, y avisa en el log, si la contraseña tiene menos de 8 caracteres.
3. Si el email ya está registrado como cliente, lo convierte en ADMIN y mantiene su contraseña actual.
4. Si no, crea el usuario ADMIN con esa contraseña.

Después de crear el administrador puedes quitar `ADMIN_PASSWORD` del `.env`.

### Archivo `.env`

El proyecto incluye la plantilla `anarkiagames/.env.example`. Cópiala como `anarkiagames/.env` y completa los valores:

```bash
cp anarkiagames/.env.example anarkiagames/.env
```

`spring-dotenv` lee el `.env` de la carpeta desde donde se ejecuta la app (`anarkiagames`). Como alternativa, puedes definir las mismas variables en la configuración de ejecución de tu IDE.

Una forma de generar `JWT_SECRET`:

```bash
openssl rand -base64 32
```

---

## 10. Ejecución y pruebas

Requisitos: JDK 25 y SQL Server con la base de datos creada (puede estar vacía).

Desde la carpeta `anarkiagames`, levantar la aplicación:

```bash
./mvnw spring-boot:run
```

Ejecutar todos los tests. No necesitan SQL Server, porque usan H2 en memoria:

```bash
./mvnw test
```

| Clase | Tipo | Qué cubre |
|---|---|---|
| `InscripcionServiceTest` | Unitario (Mockito) | Reglas de la sección 4: cantidades, límite por compra, tipo único por torneo, cierre de venta, torneo cancelado, stock, pago rechazado y reembolsos (fallo de la pasarela y doble reembolso). |
| `CompraIntegrationTest` | Integración (H2 + MockMvc) | Flujo HTTP completo: administrador inicial, creación de torneos y tickets, compras, cupos disponibles, 401/403, refresh token, validaciones y email duplicado. También valida las consultas JPQL. |
| `AdminIntegrationTest` | Integración (H2 + MockMvc) | Panel admin: resumen de stock e ingresos, cancelación con reembolsos, reembolso individual, edición y eliminación de tipos de ticket, edición de torneos, búsqueda de usuarios, cambio de rol y paginación. |
| `IntentosLoginServiceTest` | Unitario | Bloqueo tras 5 fallos, vencimiento de la ventana, clave por email e IP y reinicio tras un login exitoso. |
| `CorreoServiceTest` | Unitario | Sin remitente no envía; el correo de confirmación incluye total, fecha y códigos; un fallo SMTP no se propaga. |
| `CorreoIntegrationTest` | Integración (H2 + MockMvc) | La compra dispara el correo con los códigos; restablecer la contraseña con el enlace; un enlace no sirve dos veces; un pedido nuevo invalida el anterior; enlace vencido; email no registrado; token inventado. |
| `TicketIntegrationTest` | Integración (H2 + MockMvc) | Un código único por unidad, solo el dueño ve sus tickets, check-in una sola vez, solo ADMIN hace check-in, y reembolso y cancelación invalidan los tickets. |
| `InfraestructuraIntegrationTest` | Integración (H2 + MockMvc) | `/actuator/health` público, otros endpoints de Actuator ocultos, OpenAPI y Swagger UI públicos, y login bloqueado con 429. |
| `AnarkiagamesApplicationTests` | Integración | El contexto de Spring arranca. |

Las migraciones de Flyway usan T-SQL y no corren en H2. Por eso los tests crean el esquema desde las entidades, y las migraciones se verifican al arrancar la app contra SQL Server.

---

## 11. Base de datos y migraciones

El esquema lo maneja **Flyway**. Los scripts están en `anarkiagames/src/main/resources/db/migration` y se aplican solos al arrancar, en orden de versión. Flyway guarda en la tabla `flyway_schema_history` cuáles ya se ejecutaron.

| Versión | Script | Qué hace |
|---|---|---|
| 1 | `V1__esquema_inicial.sql` | Crea `users`, `torneos`, `tipo_tickets` e `inscripciones` como estaban antes de Flyway. |
| 2 | `V2__compras_multiples_de_espectador.sql` | Borra la restricción única `(usuario_id, tipo_ticket_id)` de `inscripciones`, agrega `cantidad` y `monto` si no existen y estima el `monto` de las compras anteriores con el precio actual del ticket. |
| 3 | `V3__estados_de_torneo_e_inscripcion.sql` | Agrega `torneos.estado` (`ACTIVO`) e `inscripciones.estado` (`PAGADA`) con sus restricciones `CHECK`, y la columna `inscripciones.referencia_reembolso`. |
| 4 | `V4__tickets.sql` | Crea `tickets` y genera un ticket por unidad para cada compra `PAGADA` anterior, con un código aleatorio de `NEWID()`. |
| 5 | `V5__tokens_recuperacion.sql` | Crea `tokens_recuperacion` para los enlaces de recuperación de contraseña. |

Comportamiento según la BD:

- **BD vacía:** Flyway ejecuta todas las migraciones en orden.
- **BD que ya existía (creada por Hibernate con `ddl-auto=update`):** en el primer arranque, Flyway la registra como versión 1 sin ejecutar V1 (`baseline-on-migrate`) y luego ejecuta V2. V2 funciona aunque Hibernate ya haya creado `cantidad` y `monto`, y aunque la restricción ya se haya borrado a mano.

Haz un respaldo de la BD antes del primer arranque con Flyway.

Para cambiar el esquema en el futuro:

1. Crea un script nuevo con la versión siguiente, por ejemplo `V4__descripcion.sql`.
2. Nunca edites un script que ya se ejecutó. Flyway guarda su checksum y falla al arrancar si cambia.
3. Actualiza la entidad JPA. Con `ddl-auto=validate`, la app no arranca si entidad y tabla no coinciden.

---

## 12. Pendientes

Mejoras identificadas que todavía no están implementadas:

- **Negocio:**
  - Datos del competidor (gamer tag, equipo).
- **Pagos:** obtener llaves de prueba de Culqi o agregar `MercadoPagoPaymentGateway`.
