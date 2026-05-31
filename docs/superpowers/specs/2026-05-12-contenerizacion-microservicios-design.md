# Contenerización de microservicios integrados (Examen 4)

**Fecha:** 2026-05-12
**Autor:** David
**Estado:** Aprobado para implementación

## Contexto

El Examen 4 de Programación Avanzada exige contenerizar las dos APIs construidas en las evaluaciones previas y hacer que se comuniquen en una red Docker llamada `redcalendario`:

- **API de Festivos** (MS1) — Node.js + Express + Mongoose, valida fechas festivas colombianas. Carpeta `validate_holydays_ms/`. Puerto 8080. Usa MongoDB.
- **API de Calendario** (MS2) — Spring Boot 3.3.4 + Java 17 + JPA, genera el calendario anual consumiendo MS1. Carpeta `consume_holidays_ms/`. Puerto 8082. Usa PostgreSQL.

El diagrama del PDF exige 4 contenedores con nombres específicos en una red llamada `redcalendario` y la entrega de la **evidencia de los comandos** utilizados para construir las imágenes y crear los contenedores.

## Objetivos

1. Construir imágenes Docker reproducibles para ambos microservicios.
2. Levantar la pila completa (4 contenedores) en una red Docker `redcalendario` donde los servicios se descubran por nombre.
3. Producir un documento con la evidencia de comandos (`docker network`, `docker volume`, `docker build`, `docker run`) ejecutables en orden y verificables con `curl`.
4. No romper el flujo de desarrollo actual (`mvn spring-boot:run`, `npm run dev`) — el código debe seguir funcionando contra `localhost` sin Docker.

## No-objetivos

- Orquestación con Kubernetes, Swarm o ECS.
- CI/CD, publicación de imágenes a un registry remoto.
- HTTPS, autenticación entre microservicios, observabilidad (Prometheus, etc.).
- Refactorizar lógica de negocio de cualquiera de los dos servicios.

## Arquitectura

```
┌──────────────────── red docker bridge: redcalendario ─────────────────────┐
│                                                                            │
│   ┌─────────────────────┐         ┌──────────────────────────┐             │
│   │ dockerbdfestivos    │◄────────│ dockerapifestivos        │◄── :8080   │
│   │ image: mongo:7      │  27017  │ image: apifestivos:1.0   │  (host)    │
│   │ vol: mongodb_data   │         │ build: validate_holydays │             │
│   └─────────────────────┘         └──────────────────────────┘             │
│                                              ▲                             │
│                                              │ HTTP                        │
│                                              │ http://dockerapifestivos:8080│
│   ┌─────────────────────┐         ┌──────────────────────────┐             │
│   │ dockerbdcalendario  │◄────────│ dockerapicalendario      │◄── :8082   │
│   │ image: postgres:16  │  5432   │ image: apicalendario:1.0 │  (host)    │
│   │ vol: postgres_data  │         │ build: consume_holidays  │             │
│   └─────────────────────┘         └──────────────────────────┘             │
└────────────────────────────────────────────────────────────────────────────┘
```

**Reglas de exposición de puertos:**
- Las BDs **no** exponen puertos al host (solo accesibles dentro de `redcalendario`).
- Las APIs exponen sus puertos nativos: `8080` (festivos) y `8082` (calendario).

**Descubrimiento por DNS interno:**
- `dockerapifestivos` ↔ `dockerbdfestivos:27017`
- `dockerapicalendario` ↔ `dockerbdcalendario:5432`
- `dockerapicalendario` ↔ `dockerapifestivos:8080`

## Cambios en MS1 (validate_holydays_ms)

### Nuevos archivos

**`validate_holydays_ms/Dockerfile`** — multi-stage:
- Stage `deps`: `node:20-alpine`, `WORKDIR /app`, copia `package*.json`, `npm ci --omit=dev`.
- Stage `runtime`: `node:20-alpine`, copia `node_modules` desde `deps`, copia `src/`, `EXPOSE 8080`, `CMD ["node","src/app.js"]`.
- Usuario no-root (`USER node`).

**`validate_holydays_ms/.dockerignore`**:
```
node_modules
.env
.git
.gitignore
*.md
coverage
.DS_Store
```

### Cambios de código

**Ninguno.** El servicio ya consume `MONGODB_URI` y `PORT` desde `process.env` (ver `src/config/database.js:4` y `src/app.js:32`).

### Variables de entorno en runtime Docker

| Variable | Valor en contenedor |
|---|---|
| `PORT` | `8080` |
| `MONGODB_URI` | `mongodb://admin:password123@dockerbdfestivos:27017/festivos_db?authSource=admin` |

## Cambios en MS2 (consume_holidays_ms)

### Nuevos archivos

**`consume_holidays_ms/Dockerfile`** — multi-stage:
- Stage `build`: `maven:3.9-eclipse-temurin-17`, `WORKDIR /build`, copia `pom.xml`, `mvn -B dependency:go-offline`, copia `src/`, `mvn -B -DskipTests package`.
- Stage `runtime`: `eclipse-temurin:17-jre-alpine`, copia el `*.jar` desde `build`, `EXPOSE 8082`, `ENTRYPOINT ["java","-jar","/app/calendario.jar"]`.
- Usuario no-root.

**`consume_holidays_ms/.dockerignore`**:
```
target
.git
.gitignore
.idea
*.iml
*.md
.DS_Store
```

### Cambios de código

**`consume_holidays_ms/src/main/resources/application.properties`** — parametrizar con env y defaults para conservar el flujo `mvn spring-boot:run` local:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5433/calendario_db}
spring.datasource.username=${DB_USER:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
ms1.festivos.base-url=${MS1_BASE_URL:http://localhost:8080}
```

(Las demás propiedades quedan igual.)

### Variables de entorno en runtime Docker

| Variable | Valor en contenedor |
|---|---|
| `DB_URL` | `jdbc:postgresql://dockerbdcalendario:5432/calendario_db` |
| `DB_USER` | `postgres` |
| `DB_PASSWORD` | `postgres` |
| `MS1_BASE_URL` | `http://dockerapifestivos:8080` |

## Orquestación

### Archivo `docker-compose.yml` en la raíz `microservicios/`

Reemplaza al `validate_holydays_ms/docker-compose.yml` actual (que se elimina). Define los 4 servicios con los nombres exigidos, healthchecks y `depends_on` con `condition: service_healthy` cuando aplica.

**Servicios:**
- `dockerbdfestivos` → `image: mongo:7`, healthcheck con `mongosh --eval "db.adminCommand('ping')"`.
- `dockerbdcalendario` → `image: postgres:16`, healthcheck con `pg_isready -U postgres`.
- `dockerapifestivos` → `build: ./validate_holydays_ms`, `depends_on: dockerbdfestivos (healthy)`, `ports: 8080:8080`.
- `dockerapicalendario` → `build: ./consume_holidays_ms`, `depends_on: dockerbdcalendario (healthy) + dockerapifestivos (started)`, `ports: 8082:8082`.

**Volúmenes nombrados:** `mongodb_data`, `postgres_data`.
**Red:** `redcalendario` (driver `bridge`).

### Orden de levantamiento (comandos docker puros)

1. `docker network create redcalendario`
2. `docker volume create mongodb_data` + `docker volume create postgres_data`
3. `docker run -d` para `dockerbdfestivos` y `dockerbdcalendario`
4. `docker build -t apifestivos:1.0 ./validate_holydays_ms`
5. `docker build -t apicalendario:1.0 ./consume_holidays_ms`
6. `docker run -d` para `dockerapifestivos` (con env apuntando a `dockerbdfestivos`)
7. `docker run -d` para `dockerapicalendario` (con env apuntando a `dockerbdcalendario` y `dockerapifestivos`)

## Entregable: EXAMEN4_EVIDENCIA.md (raíz del proyecto)

Documento estructurado en secciones:

1. **Prerrequisitos** — Docker Desktop ejecutándose, `cd` a la raíz del proyecto.
2. **Crear red** — `docker network create redcalendario` + `docker network ls`.
3. **Crear volúmenes** — `docker volume create mongodb_data && docker volume create postgres_data`.
4. **Levantar BDs** — comandos `docker run -d` completos para Mongo y Postgres, con `--network redcalendario`, env vars, volúmenes y healthcheck inicial via `docker exec`.
5. **Construir imágenes de APIs** — `docker build -t apifestivos:1.0 ./validate_holydays_ms` y `docker build -t apicalendario:1.0 ./consume_holidays_ms`. Mostrar `docker images`.
6. **Levantar APIs** — `docker run -d` completos para ambas, con `--network redcalendario`, `-p`, env vars apuntando a nombres internos.
7. **Verificación** —
   - `docker network inspect redcalendario` (mostrar los 4 contenedores conectados)
   - `docker ps` (estado healthy/running)
   - `curl http://localhost:8080/api/festivos/obtener/2025`
   - `curl http://localhost:8082/api/calendario/generar/2025`
   - `curl http://localhost:8082/api/festivos/obtener/2025` (proxy MS2→MS1, prueba comunicación entre APIs)
   - `docker logs dockerapicalendario` (verificar conexión a MS1)
8. **Limpieza** — `docker rm -f` de los 4 contenedores, `docker network rm redcalendario`, `docker volume rm mongodb_data postgres_data` (opcional).
9. **Anexo: alternativa con docker-compose** — `docker compose up -d --build`, `docker compose down -v`.

Cada comando va en un bloque ```bash y va seguido de la salida esperada o un placeholder `<<insertar captura>>` para capturas opcionales.

## Plan de pruebas

- **Smoke test del MS1 en contenedor:** `curl http://localhost:8080/api/festivos/obtener/2025` devuelve 18+ festivos.
- **Smoke test del MS2 en contenedor:** `curl http://localhost:8082/api/calendario/generar/2025` retorna 365 días.
- **Test de comunicación inter-contenedor:** `curl http://localhost:8082/api/festivos/obtener/2025` (proxy del MS2 al MS1) — comprueba que `dockerapicalendario` resuelve `http://dockerapifestivos:8080` por DNS interno.
- **Test de persistencia:** `docker restart dockerbdfestivos` no debe perder datos sembrados; `docker rm -f dockerbdfestivos && docker run ...` con el mismo volumen los recupera.
- **Test de aislamiento de red:** desde el host, `nc -z localhost 27017` y `nc -z localhost 5432` deben fallar (BDs no expuestas).
- **Test de flujo dev nativo:** `mvn spring-boot:run` y `npm run dev` siguen funcionando con sus defaults locales (gracias a los placeholders `${VAR:default}` y al `MONGODB_URI` por defecto).

## Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Spring Boot intenta conectar a Postgres antes de que esté listo | Healthcheck en `dockerbdcalendario` + `depends_on: condition: service_healthy`; `spring.datasource.hikari.initialization-fail-timeout=60000` si fuera necesario (no por ahora). |
| MS2 falla al consumir MS1 si MS1 aún no terminó el seed | El seed es idempotente y rápido (~ms); en la práctica MS2 reintenta peticiones al usar el endpoint. Si se observa flakiness, se añade retry en `FestivoClientService`. Fuera de scope inicial. |
| Tamaño de imagen Java grande | `eclipse-temurin:17-jre-alpine` como runtime + `.dockerignore` para excluir `target/`. |
| `mongosh` ausente en imagen oficial vieja de Mongo | Usar `mongo:7` (incluye `mongosh`). |

## Decisiones tomadas

- **Comandos docker puros como evidencia principal** + docker-compose como anexo opcional.
- **Renombrar** lo existente (`festivos_mongodb` → `dockerbdfestivos`, `calendario_postgres` → `dockerbdcalendario`, `microservicios_net` → `redcalendario`) en vez de duplicar.
- **Volúmenes nombrados** para Mongo y Postgres.
- **Multi-stage Dockerfiles** para ambas APIs.
- **Solo APIs exponen puertos** al host; BDs solo en red interna.
- **Dockerfiles** viven junto a cada microservicio; el `docker-compose.yml` y `EXAMEN4_EVIDENCIA.md` viven en la raíz `microservicios/`.
- **Versiones base:** `node:20-alpine`, `mongo:7`, `postgres:16`, `maven:3.9-eclipse-temurin-17`, `eclipse-temurin:17-jre-alpine`.
