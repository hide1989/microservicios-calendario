# PLAN DE DESARROLLO — MS2: Calendario API (Spring Boot + Postgres)

## Fases de desarrollo

---

### FASE 0 — Creación del proyecto Spring Boot
**Objetivo:** Generar el proyecto con las dependencias correctas.

- [ ] Generar proyecto con Spring Initializr:
  - Group: `com.pascualbravo`
  - Artifact: `calendario`
  - Java 21, Maven, Spring Boot 3.2
  - Dependencias: `Spring Web`, `Spring Data JPA`, `PostgreSQL Driver`, `Spring WebFlux` (WebClient), `Lombok`, `SpringDoc OpenAPI`
- [ ] Configurar `application.properties`:
  - Puerto `8081`
  - DataSource PostgreSQL
  - JPA `ddl-auto=update`
  - URL del MS1: `ms1.festivos.base-url=http://localhost:8080`
- [ ] Verificar compilación limpia del proyecto (`mvn clean compile`)

---

### FASE 1 — Modelo de datos y entidades JPA
**Objetivo:** Crear las entidades que mapean las tablas Postgres.

- [ ] Crear entidad `Tipo.java`
  - `@Entity @Table(name = "tipo")`
  - Campos: `id` (Integer, PK), `tipo` (String)
- [ ] Crear entidad `Calendario.java`
  - `@Entity @Table(name = "calendario")`
  - Campos: `id` (Long, PK, autoincrement), `fecha` (LocalDate), `tipo` (ManyToOne), `descripcion` (String)
- [ ] Crear `data.sql` con seed de los 3 tipos de día:
  ```sql
  INSERT INTO tipo (id, tipo) VALUES (1, 'Dia laboral') ON CONFLICT DO NOTHING;
  INSERT INTO tipo (id, tipo) VALUES (2, 'Fin de Semana') ON CONFLICT DO NOTHING;
  INSERT INTO tipo (id, tipo) VALUES (3, 'Dia festivo') ON CONFLICT DO NOTHING;
  ```
- [ ] Verificar que JPA crea las tablas al arrancar

---

### FASE 2 — Repositorios JPA
**Objetivo:** Acceso a datos con Spring Data.

- [ ] Crear `TipoRepository.java` — extiende `JpaRepository<Tipo, Integer>`
- [ ] Crear `CalendarioRepository.java` — extiende `JpaRepository<Calendario, Long>`
  - Método: `List<Calendario> findByFechaBetween(LocalDate inicio, LocalDate fin)`
  - Método: `long countByFechaBetween(LocalDate inicio, LocalDate fin)`

---

### FASE 3 — DTOs
**Objetivo:** Desacoplar la capa HTTP de las entidades JPA.

- [ ] Crear `FestivoDTO.java`
  - Campos: `String festivo`, `String fecha`
- [ ] Crear `CalendarioDTO.java`
  - Campos: `Long id`, `String fecha`, `TipoDTO tipo`, `String descripcion`
- [ ] Crear `TipoDTO.java`
  - Campos: `Integer id`, `String tipo`

---

### FASE 4 — Configuración WebClient
**Objetivo:** Bean para consumir el MS1.

- [ ] Crear `WebClientConfig.java`
  - Bean `WebClient` con `baseUrl` leído de properties (`ms1.festivos.base-url`)
- [ ] Crear `SwaggerConfig.java` (OpenAPI info: título, versión, descripción, servidor)

---

### FASE 5 — Servicio cliente del MS1
**Objetivo:** Consumir la API de festivos del MS1.

- [ ] Crear `FestivoClientService.java`
  - Inyectar `WebClient`
  - Método `obtenerFestivosDelAnio(int año)`:
    - Llamar `GET /api/festivos/obtener/{año}` del MS1
    - Retornar `List<FestivoDTO>`
    - Manejar errores de conexión con mensaje descriptivo

---

### FASE 6 — Servicio de lógica del calendario
**Objetivo:** Generar y clasificar los días del año.

- [ ] Crear `CalendarioService.java`
  - Inyectar `FestivoClientService`, `CalendarioRepository`, `TipoRepository`
  - Método `generarCalendarioDelAnio(int año)`:
    1. Verificar con `countByFechaBetween` si ya existe → retornar `true` si existe
    2. Llamar `FestivoClientService.obtenerFestivosDelAnio(año)`
    3. Construir `Map<LocalDate, String>` de festivos (fecha → nombre)
    4. Iterar cada día del año con `LocalDate`:
       - Si fecha en map festivos → `Tipo(3)`, descripcion = `"{DíaSemana} - {NombreFestivo}"`
       - Si sábado o domingo → `Tipo(2)`, descripcion = `"{DíaSemana}"`
       - Else → `Tipo(1)`, descripcion = `"{DíaSemana}"`
    5. Guardar con `saveAll()`, retornar `true`
  - Método `listarCalendarioDelAnio(int año)`:
    - Consultar todos los registros del año
    - Mapear a `List<CalendarioDTO>`
    - Retornar lista (vacía si no existe)

---

### FASE 7 — Controllers
**Objetivo:** Exponer los endpoints HTTP REST.

- [ ] Crear `FestivoController.java`
  - `GET /api/festivos/obtener/{año}` → llama `FestivoClientService`
- [ ] Crear `CalendarioController.java`
  - `GET /api/calendario/generar/{año}` → llama `CalendarioService.generarCalendarioDelAnio`
  - `GET /api/calendario/listar/{año}` → llama `CalendarioService.listarCalendarioDelAnio`, retorna 404 si vacío
- [ ] Agregar anotaciones `@Operation`, `@ApiResponse`, `@Tag` de SpringDoc en cada endpoint

---

### FASE 8 — Swagger / OpenAPI
**Objetivo:** Documentar completamente la API para pruebas desde el navegador.

- [ ] Verificar que `SpringDoc` genera el spec en `/v3/api-docs`
- [ ] Configurar `springdoc.swagger-ui.path=/swagger-ui.html`
- [ ] Documentar schemas `FestivoDTO`, `CalendarioDTO`, `TipoDTO` con `@Schema`
- [ ] Verificar que Swagger UI carga en `http://localhost:8081/swagger-ui.html`

---

### FASE 9 — Tests unitarios
**Objetivo:** Cubrir la lógica de negocio con JUnit 5 + Mockito.

- [ ] Crear `FestivoClientServiceTest.java`
  - Mock `WebClient`
  - Test: retorna lista de festivos del MS1
  - Test: lanza excepción descriptiva si MS1 no responde
- [ ] Crear `CalendarioServiceTest.java`
  - Mock `FestivoClientService`, `CalendarioRepository`, `TipoRepository`
  - Test: genera 365 días para 2023
  - Test: 1 enero 2023 clasificado como festivo
  - Test: 2 enero 2023 clasificado como laboral
  - Test: 7 enero 2023 clasificado como fin de semana
  - Test: si año ya existe, retorna true sin llamar saveAll
- [ ] Crear `CalendarioControllerTest.java`
  - `@WebMvcTest` con mock de services
  - Test HTTP para los 4 endpoints

---

### FASE 10 — Docker Compose (complementar MS1)
**Objetivo:** Añadir Postgres al docker-compose.yml existente.

- [ ] Agregar servicio `postgres` al `docker-compose.yml`:
  - Imagen: `postgres:16`
  - Puerto: `5432:5432`
  - Variables: `POSTGRES_DB=calendario_db`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
  - Volumen persistente

---

## Dependencias entre fases
```
FASE 0 → FASE 1 → FASE 2 → FASE 5 → FASE 6 → FASE 7 → FASE 8
               ↘ FASE 3 ──────────↗
               ↘ FASE 4 ──────────↗
FASE 9 (después de FASE 6 y FASE 7)
FASE 10 (paralela)
```

---

## Criterios de aceptación
- [ ] `GET /api/festivos/obtener/2023` retorna los 18 festivos (consumiendo MS1)
- [ ] `GET /api/calendario/generar/2023` retorna `true` y persiste 365 días
- [ ] Segunda llamada a `generar/2023` retorna `true` sin reinsertar datos
- [ ] `GET /api/calendario/listar/2023` retorna 365 días con tipo y descripcion correctos
  - 1 enero: `tipo.id=3`, `descripcion="Domingo - Año nuevo"`
  - 2 enero: `tipo.id=1`, `descripcion="Lunes"`
  - 7 enero: `tipo.id=2`, `descripcion="Sábado"`
- [ ] `GET /api/calendario/listar/2099` (sin generar) retorna 404
- [ ] Swagger UI accesible y funcional en `/swagger-ui.html`
- [ ] Todos los tests de JUnit pasan sin errores
