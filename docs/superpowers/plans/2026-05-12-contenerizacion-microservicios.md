# Contenerización de microservicios integrados — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Contenerizar API de Festivos (Node/Express/Mongo) y API de Calendario (Spring Boot/Postgres), hacerlas comunicar en una red Docker `redcalendario`, y producir evidencia de comandos como exige el Examen 4.

**Architecture:** Cuatro contenedores en una red bridge `redcalendario`: dos BDs (`dockerbdfestivos`, `dockerbdcalendario`) sin exposición al host y dos APIs (`dockerapifestivos:8080`, `dockerapicalendario:8082`) expuestas. Dockerfiles multi-stage en cada microservicio. Un `docker-compose.yml` en la raíz orquesta todo; un `EXAMEN4_EVIDENCIA.md` documenta los comandos `docker` puros equivalentes.

**Tech Stack:** Docker 24+, Docker Compose v2, Node.js 20-alpine, MongoDB 7, Eclipse Temurin 17 JRE, Maven 3.9, PostgreSQL 16, Spring Boot 3.3.4.

---

## File Structure

**Crear:**
- `validate_holydays_ms/Dockerfile` — multi-stage Node.js
- `validate_holydays_ms/.dockerignore`
- `consume_holidays_ms/Dockerfile` — multi-stage Maven + JRE
- `consume_holidays_ms/.dockerignore`
- `docker-compose.yml` (en raíz `microservicios/`)
- `EXAMEN4_EVIDENCIA.md` (en raíz `microservicios/`)

**Modificar:**
- `consume_holidays_ms/src/main/resources/application.properties` — parametrizar con env vars + defaults

**Eliminar:**
- `validate_holydays_ms/docker-compose.yml` (reemplazado por el de la raíz)

---

## Task 1: Dockerfile y .dockerignore para MS1 (validate_holydays_ms)

**Files:**
- Create: `validate_holydays_ms/Dockerfile`
- Create: `validate_holydays_ms/.dockerignore`

- [ ] **Step 1: Crear `.dockerignore` de MS1**

Archivo: `validate_holydays_ms/.dockerignore`

```
node_modules
npm-debug.log
.env
.env.example
.git
.gitignore
*.md
coverage
.DS_Store
Dockerfile
.dockerignore
```

- [ ] **Step 2: Crear `Dockerfile` multi-stage de MS1**

Archivo: `validate_holydays_ms/Dockerfile`

```dockerfile
# syntax=docker/dockerfile:1.6

FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --omit=dev

FROM node:20-alpine AS runtime
ENV NODE_ENV=production
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY src ./src
COPY package.json ./
USER node
EXPOSE 8080
CMD ["node", "src/app.js"]
```

- [ ] **Step 3: Construir la imagen para validar**

Ejecutar desde la raíz `microservicios/`:

```bash
docker build -t apifestivos:1.0 ./validate_holydays_ms
```

Esperado: salida termina con `Successfully tagged apifestivos:1.0` y `docker images apifestivos:1.0` la muestra.

- [ ] **Step 4: Smoke test de la imagen (Mongo no requerido aún)**

```bash
docker run --rm apifestivos:1.0 node -e "console.log(require('./package.json').name)"
```

Esperado: imprime `validate_holydays_ms` y sale con código 0.

- [ ] **Step 5: Commit**

```bash
cd validate_holydays_ms
git add Dockerfile .dockerignore
git commit -m "chore(docker): add multi-stage Dockerfile and .dockerignore for MS1"
cd ..
```

---

## Task 2: Parametrizar `application.properties` de MS2 con variables de entorno

**Files:**
- Modify: `consume_holidays_ms/src/main/resources/application.properties`

- [ ] **Step 1: Reemplazar valores fijos por placeholders con defaults**

Sustituir el contenido completo del archivo por:

```properties
server.port=8082

spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5433/calendario_db}
spring.datasource.username=${DB_USER:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.defer-datasource-initialization=true
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect

spring.sql.init.mode=always
spring.sql.init.data-locations=classpath:data.sql

ms1.festivos.base-url=${MS1_BASE_URL:http://localhost:8080}

springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.operationsSorter=alpha
```

- [ ] **Step 2: Verificar que el build sigue compilando**

```bash
cd consume_holidays_ms
mvn -B -DskipTests package
```

Esperado: `BUILD SUCCESS` y `target/calendario-1.0.0.jar` existe.

- [ ] **Step 3: Verificar que los tests siguen pasando (no requieren BD)**

```bash
mvn test
```

Esperado: `BUILD SUCCESS`, todos los tests de `CalendarioServiceTest`, `CalendarioControllerTest`, `FestivoClientServiceTest` en verde.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/application.properties
git commit -m "chore(config): parametrize datasource and MS1 URL via env vars with local defaults"
cd ..
```

---

## Task 3: Dockerfile y .dockerignore para MS2 (consume_holidays_ms)

**Files:**
- Create: `consume_holidays_ms/Dockerfile`
- Create: `consume_holidays_ms/.dockerignore`

- [ ] **Step 1: Crear `.dockerignore` de MS2**

Archivo: `consume_holidays_ms/.dockerignore`

```
target
.git
.gitignore
.idea
*.iml
.vscode
*.md
.DS_Store
Dockerfile
.dockerignore
```

- [ ] **Step 2: Crear `Dockerfile` multi-stage de MS2**

Archivo: `consume_holidays_ms/Dockerfile`

```dockerfile
# syntax=docker/dockerfile:1.6

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package && \
    cp target/calendario-*.jar /build/calendario.jar

FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/calendario.jar /app/calendario.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/calendario.jar"]
```

- [ ] **Step 3: Construir la imagen**

```bash
docker build -t apicalendario:1.0 ./consume_holidays_ms
```

Esperado: termina con `Successfully tagged apicalendario:1.0`. La primera vez tomará varios minutos por descarga de dependencias Maven.

- [ ] **Step 4: Smoke test (verificar que el JAR arranca y falla limpiamente sin BD)**

```bash
docker run --rm --name probe-calendario apicalendario:1.0 --spring.main.banner-mode=off &
sleep 8
docker ps --filter name=probe-calendario --format '{{.Status}}' || true
docker stop probe-calendario 2>/dev/null || true
```

Esperado: el contenedor arranca y luego falla por no poder conectar a Postgres en `localhost:5433` (es lo esperado fuera de la red). Lo importante es que no falla por un `ClassNotFoundException` o un JAR corrupto.

- [ ] **Step 5: Commit**

```bash
cd consume_holidays_ms
git add Dockerfile .dockerignore
git commit -m "chore(docker): add multi-stage Dockerfile and .dockerignore for MS2"
cd ..
```

---

## Task 4: Eliminar el docker-compose viejo de MS1

**Files:**
- Delete: `validate_holydays_ms/docker-compose.yml`

- [ ] **Step 1: Eliminar el archivo**

```bash
rm "validate_holydays_ms/docker-compose.yml"
```

- [ ] **Step 2: Actualizar `validate_holydays_ms/CLAUDE.md`**

Eliminar la sección `## Docker Compose` (líneas 65-69) y la línea 9 (`docker-compose up -d mongodb`). Reemplazar con una nota apuntando al compose de la raíz:

Buscar:
```
# Levantar MongoDB (requerido antes de iniciar el servidor)
docker-compose up -d mongodb
```

Reemplazar con:
```
# Levantar MongoDB (requerido antes de iniciar el servidor) — desde la raíz del proyecto
docker compose up -d dockerbdfestivos
```

Buscar y eliminar completamente la sección `## Docker Compose` al final del archivo.

- [ ] **Step 3: Actualizar `consume_holidays_ms/CLAUDE.md`**

Buscar:
```
1. **PostgreSQL** en `localhost:5433` (base de datos `calendario_db`) — levantar desde el directorio hermano `validate_holydays_ms/`:
   ```bash
   docker-compose up -d postgres
   ```
```

Reemplazar con:
```
1. **PostgreSQL** en `localhost:5433` (base de datos `calendario_db`) — levantar desde la raíz del proyecto:
   ```bash
   docker compose up -d dockerbdcalendario
   ```
```

- [ ] **Step 4: Commit (en cada subrepo afectado)**

```bash
cd validate_holydays_ms
git add docker-compose.yml CLAUDE.md
git commit -m "chore(docker): remove obsolete compose, point to root-level compose"
cd ../consume_holidays_ms
git add CLAUDE.md
git commit -m "docs: point to root-level docker compose for postgres"
cd ..
```

---

## Task 5: `docker-compose.yml` en la raíz del proyecto

**Files:**
- Create: `docker-compose.yml` (en raíz `microservicios/`)

- [ ] **Step 1: Crear el compose con los 4 servicios**

Archivo: `docker-compose.yml`

```yaml
services:
  dockerbdfestivos:
    image: mongo:7
    container_name: dockerbdfestivos
    environment:
      MONGO_INITDB_DATABASE: festivos_db
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: password123
    volumes:
      - mongodb_data:/data/db
    networks:
      - redcalendario
    healthcheck:
      test: ["CMD", "mongosh", "--quiet", "--eval", "db.adminCommand('ping').ok"]
      interval: 5s
      timeout: 5s
      retries: 10

  dockerbdcalendario:
    image: postgres:16
    container_name: dockerbdcalendario
    environment:
      POSTGRES_DB: calendario_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - redcalendario
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d calendario_db"]
      interval: 5s
      timeout: 5s
      retries: 10

  dockerapifestivos:
    build: ./validate_holydays_ms
    image: apifestivos:1.0
    container_name: dockerapifestivos
    environment:
      PORT: "8080"
      MONGODB_URI: "mongodb://admin:password123@dockerbdfestivos:27017/festivos_db?authSource=admin"
    ports:
      - "8080:8080"
    depends_on:
      dockerbdfestivos:
        condition: service_healthy
    networks:
      - redcalendario

  dockerapicalendario:
    build: ./consume_holidays_ms
    image: apicalendario:1.0
    container_name: dockerapicalendario
    environment:
      DB_URL: "jdbc:postgresql://dockerbdcalendario:5432/calendario_db"
      DB_USER: "postgres"
      DB_PASSWORD: "postgres"
      MS1_BASE_URL: "http://dockerapifestivos:8080"
    ports:
      - "8082:8082"
    depends_on:
      dockerbdcalendario:
        condition: service_healthy
      dockerapifestivos:
        condition: service_started
    networks:
      - redcalendario

volumes:
  mongodb_data:
  postgres_data:

networks:
  redcalendario:
    name: redcalendario
    driver: bridge
```

- [ ] **Step 2: Validar la sintaxis del compose**

```bash
docker compose config --quiet
```

Esperado: salida vacía y código 0. Cualquier error de YAML/sintaxis falla aquí.

- [ ] **Step 3: Levantar la pila completa**

```bash
docker compose up -d --build
```

Esperado: las 4 imágenes se construyen/descargan; al final `docker ps` muestra los 4 contenedores corriendo. Las BDs deben llegar a `(healthy)` antes de que las APIs inicien.

- [ ] **Step 4: Verificar healthchecks y logs**

```bash
docker compose ps
docker logs --tail 20 dockerapifestivos
docker logs --tail 20 dockerapicalendario
```

Esperado:
- `dockerbdfestivos` y `dockerbdcalendario` con estado `healthy`
- `dockerapifestivos`: log final `Festivos API corriendo en http://localhost:8080`
- `dockerapicalendario`: log final con `Started CalendarioApplication in X.Xs` o `Tomcat started on port 8082`

- [ ] **Step 5: Smoke test funcional**

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/festivos/obtener/2025
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8082/api/calendario/generar/2025
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8082/api/festivos/obtener/2025
```

Esperado: tres `200`. El tercer curl prueba la comunicación inter-contenedor (MS2 proxy a MS1 vía DNS interno).

- [ ] **Step 6: Verificar la red**

```bash
docker network inspect redcalendario --format '{{range .Containers}}{{.Name}} {{end}}'
```

Esperado: `dockerbdcalendario dockerbdfestivos dockerapicalendario dockerapifestivos` (orden puede variar) — los 4 conectados.

- [ ] **Step 7: Bajar la pila para liberar recursos antes de la siguiente tarea**

```bash
docker compose down
```

(No usar `-v`; queremos conservar los volúmenes para la tarea siguiente).

- [ ] **Step 8: Commit**

```bash
git add docker-compose.yml
git commit -m "feat(docker): add root-level compose orchestrating festivos and calendario stack"
```

---

## Task 6: `EXAMEN4_EVIDENCIA.md` con comandos docker puros

**Files:**
- Create: `EXAMEN4_EVIDENCIA.md` (en raíz)

- [ ] **Step 1: Crear el documento de evidencia**

Archivo: `EXAMEN4_EVIDENCIA.md`

````markdown
# Examen 4 — Evidencia de contenerización

Comandos para construir las imágenes y crear los 4 contenedores de la red `redcalendario` exigida por el examen.

**Estructura:**

| Contenedor | Imagen | Rol | Puerto host |
|---|---|---|---|
| `dockerbdfestivos` | `mongo:7` | BD de festivos | — (solo red interna) |
| `dockerapifestivos` | `apifestivos:1.0` | API Node/Express | `8080` |
| `dockerbdcalendario` | `postgres:16` | BD de calendario | — (solo red interna) |
| `dockerapicalendario` | `apicalendario:1.0` | API Spring Boot | `8082` |

Ejecutar desde la raíz del proyecto.

---

## 1. Limpieza previa (idempotente)

```bash
docker rm -f dockerapicalendario dockerapifestivos dockerbdcalendario dockerbdfestivos 2>/dev/null || true
docker network rm redcalendario 2>/dev/null || true
```

## 2. Crear la red

```bash
docker network create redcalendario
docker network ls | grep redcalendario
```

Salida esperada:
```
<network-id>   redcalendario   bridge   local
```

## 3. Crear los volúmenes nombrados

```bash
docker volume create mongodb_data
docker volume create postgres_data
docker volume ls | grep -E "mongodb_data|postgres_data"
```

## 4. Levantar el contenedor de MongoDB (BD de festivos)

```bash
docker run -d \
  --name dockerbdfestivos \
  --network redcalendario \
  -e MONGO_INITDB_DATABASE=festivos_db \
  -e MONGO_INITDB_ROOT_USERNAME=admin \
  -e MONGO_INITDB_ROOT_PASSWORD=password123 \
  -v mongodb_data:/data/db \
  mongo:7
```

Verificación:
```bash
docker exec dockerbdfestivos mongosh --quiet --eval "db.adminCommand('ping').ok"
```
Salida esperada: `1`

## 5. Levantar el contenedor de PostgreSQL (BD de calendario)

```bash
docker run -d \
  --name dockerbdcalendario \
  --network redcalendario \
  -e POSTGRES_DB=calendario_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -v postgres_data:/var/lib/postgresql/data \
  postgres:16
```

Verificación:
```bash
docker exec dockerbdcalendario pg_isready -U postgres -d calendario_db
```
Salida esperada: `/var/run/postgresql:5432 - accepting connections`

## 6. Construir las imágenes de las APIs

```bash
docker build -t apifestivos:1.0 ./validate_holydays_ms
docker build -t apicalendario:1.0 ./consume_holidays_ms
docker images | grep -E "apifestivos|apicalendario"
```

## 7. Levantar el contenedor de la API de Festivos

```bash
docker run -d \
  --name dockerapifestivos \
  --network redcalendario \
  -p 8080:8080 \
  -e PORT=8080 \
  -e MONGODB_URI='mongodb://admin:password123@dockerbdfestivos:27017/festivos_db?authSource=admin' \
  apifestivos:1.0
```

Esperar ~5s y verificar:
```bash
sleep 5
docker logs --tail 5 dockerapifestivos
curl -s http://localhost:8080/api/festivos/obtener/2025 | head -c 200
```

## 8. Levantar el contenedor de la API de Calendario

```bash
docker run -d \
  --name dockerapicalendario \
  --network redcalendario \
  -p 8082:8082 \
  -e DB_URL='jdbc:postgresql://dockerbdcalendario:5432/calendario_db' \
  -e DB_USER=postgres \
  -e DB_PASSWORD=postgres \
  -e MS1_BASE_URL='http://dockerapifestivos:8080' \
  apicalendario:1.0
```

Esperar ~15s (Spring Boot tarda en arrancar):
```bash
sleep 15
docker logs --tail 10 dockerapicalendario
curl -s http://localhost:8082/api/calendario/generar/2025 | head -c 200
```

## 9. Verificación completa

```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
docker network inspect redcalendario --format '{{range .Containers}}{{.Name}} {{end}}'

echo "--- MS1 directo ---"
curl -s -o /dev/null -w "GET /api/festivos/obtener/2025  -> %{http_code}\n" http://localhost:8080/api/festivos/obtener/2025

echo "--- MS2 calendario ---"
curl -s -o /dev/null -w "GET /api/calendario/generar/2025 -> %{http_code}\n" http://localhost:8082/api/calendario/generar/2025

echo "--- MS2 -> MS1 (prueba comunicación inter-contenedor) ---"
curl -s -o /dev/null -w "GET /api/festivos/obtener/2025  -> %{http_code}\n" http://localhost:8082/api/festivos/obtener/2025
```

Salida esperada:
- `docker ps` muestra los 4 contenedores con `Up`
- `docker network inspect` lista los 4 nombres
- Los 3 curl devuelven `200`

## 10. Limpieza (opcional)

```bash
docker rm -f dockerapicalendario dockerapifestivos dockerbdcalendario dockerbdfestivos
docker network rm redcalendario
docker volume rm mongodb_data postgres_data
```

---

## Anexo: levantar todo con docker-compose

Como alternativa al flujo anterior, desde la raíz del proyecto:

```bash
docker compose up -d --build
docker compose ps
docker compose down       # detener
docker compose down -v    # detener y borrar volúmenes
```
````

- [ ] **Step 2: Ejecutar el documento de evidencia de principio a fin para validar**

Seguir literalmente las secciones 1–9 del documento recién creado. En la sección 9, los 3 curl deben devolver `200`.

- [ ] **Step 3: Limpieza tras validación**

Ejecutar la sección 10 del documento.

- [ ] **Step 4: Commit**

```bash
git add EXAMEN4_EVIDENCIA.md
git commit -m "docs: add Examen 4 evidence with raw docker commands"
```

---

## Task 7: Validación final end-to-end con compose

**Files:** ninguno (validación)

- [ ] **Step 1: Levantar la pila con compose desde estado limpio**

```bash
docker compose down -v 2>/dev/null || true
docker compose up -d --build
```

- [ ] **Step 2: Esperar a que las APIs estén listas**

```bash
for i in 1 2 3 4 5 6 7 8 9 10; do
  curl -sf http://localhost:8082/api/calendario/generar/2025 > /dev/null && break
  echo "Esperando MS2... ($i)"
  sleep 3
done
```

Esperado: sale del bucle antes del intento 10.

- [ ] **Step 3: Validar los 3 endpoints clave**

```bash
echo "== MS1 directo =="
curl -s http://localhost:8080/api/festivos/obtener/2025 | python3 -m json.tool | head -20

echo "== MS2 generar calendario =="
curl -s http://localhost:8082/api/calendario/generar/2025 | python3 -m json.tool | head -10

echo "== MS2 proxy a MS1 (inter-contenedor) =="
curl -s http://localhost:8082/api/festivos/obtener/2025 | python3 -m json.tool | head -20
```

Esperado: las tres respuestas devuelven JSON válido. El primero y el tercero deben listar al menos 18 festivos de 2025. El segundo lista 365 días.

- [ ] **Step 4: Validar aislamiento de las BDs**

```bash
nc -z -G 2 localhost 27017 && echo "MAL: Mongo expuesto" || echo "OK: Mongo no expuesto"
nc -z -G 2 localhost 5432 && echo "MAL: Postgres expuesto" || echo "OK: Postgres no expuesto"
```

Esperado: dos `OK:`.

- [ ] **Step 5: Validar persistencia tras reinicio**

```bash
docker restart dockerbdfestivos dockerbdcalendario
sleep 10
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8082/api/calendario/generar/2025
```

Esperado: `200`. La data sobrevive al restart por los volúmenes nombrados.

- [ ] **Step 6: Bajar la pila**

```bash
docker compose down
```

(Sin `-v` — los volúmenes se conservan para uso futuro).

- [ ] **Step 7: Commit final si quedan cambios pendientes**

```bash
git status
```

Si no hay cambios, no se requiere commit. Si los hubiera (p.ej. una corrección menor durante la validación), commitearlos con mensaje descriptivo.

---

## Notas

- **Compatibilidad con flujo dev local:** gracias a los placeholders `${VAR:default}` en `application.properties` y al uso de `process.env.MONGODB_URI` (que ya tenía MS1), `mvn spring-boot:run` y `npm run dev` siguen funcionando si se levantan solamente `dockerbdfestivos` y `dockerbdcalendario` con `docker compose up -d dockerbdfestivos dockerbdcalendario`.
- **Naming:** los nombres `dockerbdfestivos`, `dockerapifestivos`, `dockerbdcalendario`, `dockerapicalendario` y la red `redcalendario` son **literales del PDF del examen** — no renombrar.
- **Tags de imagen:** `apifestivos:1.0` y `apicalendario:1.0`. Si se reconstruye con cambios incompatibles, subir a `1.1`.
