# Microservicios Calendario — Pipeline Jenkins CI/CD

Proyecto de dos microservicios integrados con pipeline de despliegue continuo en Jenkins.

| Servicio | Tecnología | Puerto |
|---|---|---|
| `dockerapifestivos` | Node.js + Express + MongoDB | 8080 |
| `dockerapicalendario` | Spring Boot + PostgreSQL | 8082 |

---

## Requisitos previos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado y en ejecución
- Git

---

## 1. Levantar Jenkins

```bash
cd jenkins/
docker compose up -d --build
```

Jenkins queda disponible en `http://localhost:8090`.

### Obtener contraseña inicial

```bash
docker logs jenkins 2>&1 | grep -A 2 "Please use the following password"
# o directamente:
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

---

## 2. Configurar Jenkins (solo la primera vez)

1. Abre `http://localhost:8090` y pega la contraseña inicial
2. Selecciona **Install suggested plugins** y espera ~3 minutos
3. Crea un usuario administrador
4. Confirma la URL (`http://localhost:8090/`) → **Start using Jenkins**

### Crear el pipeline

1. **Nueva Tarea** → nombre: `Pipeline-Microservicios` → tipo **Pipeline** → **OK**
2. Sección **Pipeline**:
   - Definition: `Pipeline script from SCM`
   - SCM: `Git`
   - Repository URL: `https://github.com/hide1989/microservicios-calendario.git`
   - Branch Specifier: `*/main`
   - Script Path: `Jenkinsfile`
3. Sección **Build Triggers**: activar `Consultar repositorio (SCM)` → Schedule: `H/2 * * * *`
4. **Guardar** → **Build Now**

---

## 3. Verificar el despliegue

### Contenedores activos

```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

Resultado esperado (4 contenedores `Up`):

```
NAMES                STATUS    PORTS
dockerapicalendario  Up        0.0.0.0:8082->8082/tcp
dockerapifestivos    Up        0.0.0.0:8080->8080/tcp
dockerbdcalendario   Up        5432/tcp
dockerbdfestivos     Up        27017/tcp
```

### Probar las APIs

**macOS / Linux:**

```bash
# MS1 — Festivos de Colombia 2025
curl http://localhost:8080/api/festivos/obtener/2025

# MS2 — Calendario generado con festivos 2025
curl http://localhost:8082/api/calendario/generar/2025

# Verificar solo el código HTTP
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/festivos/obtener/2025
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8082/api/calendario/generar/2025
```

### Verificar la red Docker

```bash
docker network inspect redcalendario --format '{{range .Containers}}{{.Name}} {{end}}'
# Debe mostrar los 4 contenedores en la red redcalendario
```

---

## 4. Probar el trigger automático del pipeline

Cualquier push a la rama `main` dispara el pipeline automáticamente (polling cada 2 minutos).

```bash
# Desde la raíz del proyecto:
git commit --allow-empty -m "ci: test trigger automático"
git push origin main
```

Luego en `http://localhost:8090/job/Pipeline-Microservicios/` observa cómo Jenkins inicia un nuevo build dentro de los próximos 2 minutos.

---

## 5. Estructura del proyecto

```
microservicios/
├── Jenkinsfile                   # Pipeline Groovy (6 stages)
├── docker-compose.yml            # Orquestación local (sin Jenkins)
├── validate_holydays_ms/         # MS1: Node.js + Express + MongoDB
│   ├── Dockerfile
│   └── src/
└── consume_holidays_ms/          # MS2: Spring Boot + PostgreSQL
│   ├── Dockerfile
│   └── src/
└── jenkins/
    ├── Dockerfile                # Jenkins LTS + Docker CLI
    └── docker-compose.yml        # Servicio Jenkins con socket Docker
```

---

## 6. Etapas del pipeline

| Stage | Descripción |
|---|---|
| Clonar Repositorio | `git clone` desde GitHub |
| Construir Imágenes Docker | `docker build` para MS1 y MS2 |
| Detener Contenedores Anteriores | Limpia ejecuciones previas |
| Preparar Red y Volúmenes | Crea `redcalendario`, `mongodb_data`, `postgres_data` |
| Desplegar Bases de Datos | Levanta MongoDB y PostgreSQL |
| Desplegar APIs | Levanta MS1 (8080) y MS2 (8082) |

---

## 7. Detener todo

```bash
# Detener contenedores del pipeline
for c in dockerapicalendario dockerapifestivos dockerbdcalendario dockerbdfestivos; do
  docker stop $c && docker rm $c
done

# Detener Jenkins
cd jenkins/
docker compose down
```
