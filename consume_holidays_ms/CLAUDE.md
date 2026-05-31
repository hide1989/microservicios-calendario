# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Comandos principales

```bash
# Ejecutar la aplicación
mvn spring-boot:run

# Compilar el JAR
mvn package

# Ejecutar todos los tests (no requieren BD ni MS1 activo — usan mocks)
mvn test

# Ejecutar una clase de test específica
mvn test -Dtest=CalendarioServiceTest
mvn test -Dtest=CalendarioControllerTest
mvn test -Dtest=FestivoClientServiceTest
```

## Servicios externos requeridos

Este microservicio (MS2) depende de dos servicios externos que deben estar activos antes de arrancar:

1. **PostgreSQL** en `localhost:5433` (base de datos `calendario_db`) — levantar desde la raíz del proyecto:
   ```bash
   docker compose up -d dockerbdcalendario
   ```

2. **MS1 (Festivos API)** en `localhost:8080` — microservicio Node.js que expone `/api/festivos/obtener/{año}`. Este MS2 no puede generar calendarios sin él.

La aplicación corre en el puerto **8082**. Swagger UI disponible en `http://localhost:8082/swagger-ui.html`.

## Arquitectura

El flujo principal es: `FestivoController` / `CalendarioController` → servicios → repositorios JPA (PostgreSQL).

### Capas

- **`controller/`** — `CalendarioController` (`/api/calendario`) y `FestivoController` (`/api/festivos`). El primero expone `generar/{año}` y `listar/{año}`; el segundo actúa como proxy directo al MS1.
- **`service/`**
  - `FestivoClientService` — cliente reactivo (WebClient) que consume el MS1. Usa `.block()` para convertir el flujo reactivo a síncrono.
  - `CalendarioService` — lógica de clasificación de días: festivo tiene precedencia sobre fin de semana, que tiene precedencia sobre laboral. Detecta si el año ya existe en BD (`countByFechaBetween`) antes de insertar.
- **`model/`** — entidades JPA `Calendario` (id, fecha, tipo FK, descripcion) y `Tipo` (id, tipo).
- **`repository/`** — Spring Data JPA; `CalendarioRepository` incluye `findByFechaBetweenOrderByFechaAsc` y `countByFechaBetween`.
- **`dto/`** — `CalendarioDTO`, `FestivoDTO`, `TipoDTO` para serialización JSON.
- **`config/`** — `WebClientConfig` define el bean `festivosApiWebClient` con base URL desde `ms1.festivos.base-url`; `SwaggerConfig` configura OpenAPI.

### Base de datos

Hibernate gestiona el schema con `ddl-auto=update`. `data.sql` inserta los 3 tipos de día usando `ON CONFLICT DO NOTHING` al arrancar (garantizado por `defer-datasource-initialization=true`).

| id | tipo |
|----|------|
| 1  | Dia laboral |
| 2  | Fin de Semana |
| 3  | Dia festivo |
