# AnarkiaGames

Backend de una plataforma de torneos de videojuegos, desarrollado con Spring Boot.
 
Este proyecto viene del curso "Curso Integrador I" (2025). El objetivo de esta nueva versión es mejorar el proyecto de ese entonces aplicando buenas prácticas: separación clara de responsabilidades, autenticación con JWT, roles de usuario bien definidos y una arquitectura de pagos desacoplada del proveedor.

## Stack

-**Java 25**
- **Spring Boot 4.1.0**
- **Spring Security + JWT** (autenticación y autorización basada en roles)
- **Spring Data JPA** (SQL Server)
- **Lombok**
- **Culqi** (pasarela de pagos) — con un modo *mock* para desarrollo sin credenciales reales

## Funcionalidades
 
- **Roles de usuario:** `ADMIN` y `CLIENTE`. El registro público siempre crea un `CLIENTE`.
- **Torneos:** creación por parte de un administrador, consulta pública (sin necesidad de login).
- **Tipos de ticket por torneo:** cada torneo define precio y cupo máximo para dos tipos de participación, `COMPETIDOR` y `ESPECTADOR`.
- **Inscripciones:** un cliente compra un ticket eligiendo el tipo de participación para un torneo específico. La compra pasa por una pasarela de pago antes de confirmarse.
- **Pagos desacoplados:** la lógica de cobro vive detrás de una interfaz (`PaymentGateway`), con dos implementaciones intercambiables por configuración:
  - `culqi` — cobro real a través de la API de Culqi.
  - `mock` — simula un pago exitoso, útil mientras no se cuenta con credenciales de comercio verificado.