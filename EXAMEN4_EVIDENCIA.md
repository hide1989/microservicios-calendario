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

Esperar ~20s (Spring Boot tarda en arrancar):
```bash
sleep 20
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

> **Nota (macOS / Docker Desktop):** Las BDs no tienen `ports:` en el compose (aislamiento correcto), pero `nc -z localhost 27017` puede reportar conexión exitosa en macOS por un artefacto del hipervisor de Docker Desktop. Para confirmar que las BDs no están expuestas, usa `docker inspect dockerbdfestivos | grep -A5 Ports` — debe mostrar `'27017/tcp': null`.

## 10. Limpieza (opcional)

```bash
docker rm -f dockerapicalendario dockerapifestivos dockerbdcalendario dockerbdfestivos
docker network rm redcalendario
docker volume rm mongodb_data postgres_data
```

---

## Anexo A: levantar todo con docker-compose

Como alternativa al flujo anterior, desde la raíz del proyecto:

```bash
docker compose up -d --build
docker compose ps
docker compose down       # detener
docker compose down -v    # detener y borrar volúmenes
```

---

## Anexo B: adaptaciones para Windows

Los comandos de Docker son idénticos en Windows. Las diferencias están en los comandos de shell que los rodean.

### Prerequisito

Usar **PowerShell 5+** (viene con Windows 10/11) o **Windows Terminal**. Git Bash también funciona con la sintaxis de Linux del documento principal.

### Diferencias por sección

#### Sección 1 — Limpieza previa

PowerShell no tiene `2>/dev/null || true`. Usar:

```powershell
docker rm -f dockerapicalendario dockerapifestivos dockerbdcalendario dockerbdfestivos
docker network rm redcalendario
```

(Ignorar los mensajes de error si los contenedores/red no existen.)

#### Sección 2 — Verificar la red

Reemplazar `grep` por `findstr`:

```powershell
docker network ls | findstr redcalendario
```

#### Sección 3 — Verificar volúmenes

```powershell
docker volume ls | findstr "mongodb_data postgres_data"
```

#### Secciones 4 y 5 — `docker run` multilínea

PowerShell usa `` ` `` (backtick) como carácter de continuación de línea en lugar de `\`:

```powershell
docker run -d `
  --name dockerbdfestivos `
  --network redcalendario `
  -e MONGO_INITDB_DATABASE=festivos_db `
  -e MONGO_INITDB_ROOT_USERNAME=admin `
  -e MONGO_INITDB_ROOT_PASSWORD=password123 `
  -v mongodb_data:/data/db `
  mongo:7
```

```powershell
docker run -d `
  --name dockerbdcalendario `
  --network redcalendario `
  -e POSTGRES_DB=calendario_db `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=postgres `
  -v postgres_data:/var/lib/postgresql/data `
  postgres:16
```

#### Sección 6 — Verificar imágenes

```powershell
docker images | findstr "apifestivos apicalendario"
```

#### Sección 7 — API de Festivos (comillas en `-e`)

En PowerShell las comillas simples `'...'` no siempre funcionan para valores con `@` o `?`. Usar comillas dobles escapando el `$`:

```powershell
docker run -d `
  --name dockerapifestivos `
  --network redcalendario `
  -p 8080:8080 `
  -e PORT=8080 `
  -e "MONGODB_URI=mongodb://admin:password123@dockerbdfestivos:27017/festivos_db?authSource=admin" `
  apifestivos:1.0
```

Esperar y verificar:

```powershell
Start-Sleep -Seconds 5
docker logs --tail 5 dockerapifestivos
curl -s http://localhost:8080/api/festivos/obtener/2025
```

#### Sección 8 — API de Calendario

```powershell
docker run -d `
  --name dockerapicalendario `
  --network redcalendario `
  -p 8082:8082 `
  -e "DB_URL=jdbc:postgresql://dockerbdcalendario:5432/calendario_db" `
  -e DB_USER=postgres `
  -e DB_PASSWORD=postgres `
  -e "MS1_BASE_URL=http://dockerapifestivos:8080" `
  apicalendario:1.0
```

Esperar y verificar:

```powershell
Start-Sleep -Seconds 20
docker logs --tail 10 dockerapicalendario
curl -s http://localhost:8082/api/calendario/generar/2025
```

#### Sección 9 — Verificación completa

El formato de tabla de `docker ps` y `docker network inspect` funciona igual. Los `curl` también:

```powershell
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
docker network inspect redcalendario --format '{{range .Containers}}{{.Name}} {{end}}'

curl -s -o NUL -w "MS1 directo            -> %{http_code}`n" http://localhost:8080/api/festivos/obtener/2025
curl -s -o NUL -w "MS2 generar calendario -> %{http_code}`n" http://localhost:8082/api/calendario/generar/2025
curl -s -o NUL -w "MS2 proxy a MS1        -> %{http_code}`n" http://localhost:8082/api/festivos/obtener/2025
```

> **Nota:** En PowerShell, `curl` es un alias de `Invoke-WebRequest`. Si no funciona `-o NUL -w`, usa `curl.exe` explícitamente (apunta al curl de Windows, no al alias):
> ```powershell
> curl.exe -s -o NUL -w "%{http_code}" http://localhost:8080/api/festivos/obtener/2025
> ```

#### Sección 9 — Aislamiento de BDs (alternativa a `nc`)

Windows no tiene `nc`. Usar `Test-NetConnection` de PowerShell:

```powershell
Test-NetConnection -ComputerName localhost -Port 27017 -InformationLevel Quiet
Test-NetConnection -ComputerName localhost -Port 5432 -InformationLevel Quiet
```

Salida esperada: `False` en ambos (puertos no expuestos al host).

> **Nota (Docker Desktop para Windows):** Al igual que en macOS, Docker Desktop puede reportar `True` aunque los puertos no estén mapeados, por el hipervisor interno. Verificar con `docker inspect`:
> ```powershell
> docker inspect dockerbdfestivos --format "{{json .NetworkSettings.Ports}}"
> ```
> Resultado correcto: `{"27017/tcp":null}`

#### Sección 10 — Limpieza

```powershell
docker rm -f dockerapicalendario dockerapifestivos dockerbdcalendario dockerbdfestivos
docker network rm redcalendario
docker volume rm mongodb_data postgres_data
```

### Resumen de equivalencias

| Linux/macOS | PowerShell Windows |
|---|---|
| `\` (continuación de línea) | `` ` `` (backtick) |
| `sleep N` | `Start-Sleep -Seconds N` |
| `grep texto` | `findstr texto` |
| `2>/dev/null \|\| true` | omitir (ignorar errores) |
| `-e 'VALOR=...'` con `@` o `?` | `-e "VALOR=..."` |
| `nc -z localhost PORT` | `Test-NetConnection -Port PORT -InformationLevel Quiet` |
| `-o /dev/null` | `-o NUL` |
| `curl` | `curl.exe` (evitar alias de PowerShell) |

### Alternativa simplificada: Git Bash en Windows

Si tienes **Git for Windows** instalado, el **Git Bash** incluido entiende la sintaxis de Linux directamente. Puedes ejecutar todos los comandos del documento principal sin adaptaciones, excepto la verificación con `nc` (no disponible en Git Bash por defecto — usar `Test-NetConnection` en PowerShell para esa verificación).
