# Calendario API — Colombia
API RESTful en Spring Boot + PostgreSQL que consume la API de Festivos (MS1), genera el calendario anual clasificando cada día como laboral, fin de semana o festivo, y lo persiste en base de datos.

---

## Requisitos previos
- Java 17+ (compatible con Java 23)
- Maven 3.8+
- Docker y Docker Compose
- **MS1 (Festivos API) corriendo en `localhost:8080`** — este servicio depende de él

---

## Configuración

### 1. Levantar PostgreSQL con Docker
Desde la carpeta `validate_holydays_ms/` (donde está el `docker-compose.yml`):
```bash
docker-compose up -d postgres
```
Esto inicia PostgreSQL en `localhost:5432` con la base de datos `calendario_db`.

### 2. Levantar también el MS1 (Festivos API)
Este microservicio necesita que el MS1 esté activo para consultar los festivos. Desde `validate_holydays_ms/`:
```bash
# En una terminal separada
npm start
```

### 3. Configuración de la aplicación
El archivo `src/main/resources/application.properties` ya tiene los valores por defecto:
```properties
server.port=8081
spring.datasource.url=jdbc:postgresql://localhost:5432/calendario_db
spring.datasource.username=postgres
spring.datasource.password=postgres
ms1.festivos.base-url=http://localhost:8080
```
Si cambiaste el puerto o credenciales del MS1/Postgres, actualiza este archivo.

### 4. Compilar y ejecutar
```bash
mvn spring-boot:run
```
Spring Boot creará automáticamente las tablas `tipo` y `calendario`, e insertará los 3 tipos de día (laboral, fin de semana, festivo).

Salida esperada:
```
Started CalendarioApplication in X.XXX seconds
Tomcat started on port 8082
```

---

## Uso de la API

### Swagger UI
Accede a la documentación interactiva desde el navegador:
```
http://localhost:8082/swagger-ui.html
```
Desde ahí puedes probar todos los endpoints directamente sin necesidad de curl o Postman.

---

### Flujo de uso recomendado

El uso típico sigue este orden:

**Paso 1:** Consultar festivos (requiere MS1 activo)
**Paso 2:** Generar el calendario del año (guarda en BD)
**Paso 3:** Listar el calendario generado (lectura de BD)

---

### Endpoints

#### 1. Obtener festivos del año (consume MS1)
```
GET /api/festivos/obtener/{año}
```
Consulta el MS1 y retorna los festivos colombianos del año.

```bash
curl http://localhost:8082/api/festivos/obtener/2023
```
```json
[
  { "festivo": "Año nuevo",             "fecha": "2023-01-01" },
  { "festivo": "Santos Reyes",          "fecha": "2023-01-09" },
  { "festivo": "San José",              "fecha": "2023-03-20" },
  ...
]
```

---

#### 2. Generar y persistir el calendario del año
```
GET /api/calendario/generar/{año}
```
Obtiene los festivos del MS1, clasifica cada día del año y los guarda en PostgreSQL.
Si el año ya fue generado, retorna `true` sin volver a insertar.

```bash
curl http://localhost:8082/api/calendario/generar/2023
```
```json
true
```

**Clasificación de días:**
| Tipo | Criterio |
|------|----------|
| Día laboral | Lunes a viernes que no sea festivo |
| Fin de Semana | Sábado o domingo que no sea festivo |
| Día festivo | Fecha presente en la lista de festivos del MS1 |

---

#### 3. Listar el calendario completo del año
```
GET /api/calendario/listar/{año}
```
Retorna todos los días del año con su tipo y descripción. Requiere haber generado el calendario primero.

```bash
curl http://localhost:8082/api/calendario/listar/2023
```
```json
[
  {
    "id": 1,
    "fecha": "2023-01-01",
    "tipo": { "id": 3, "tipo": "Dia festivo" },
    "descripcion": "Domingo - Año nuevo"
  },
  {
    "id": 2,
    "fecha": "2023-01-02",
    "tipo": { "id": 1, "tipo": "Dia laboral" },
    "descripcion": "Lunes"
  },
  {
    "id": 7,
    "fecha": "2023-01-07",
    "tipo": { "id": 2, "tipo": "Fin de Semana" },
    "descripcion": "Sábado"
  }
  ...
]
```

Si el año no ha sido generado previamente:
```bash
curl http://localhost:8082/api/calendario/listar/2099
```
```json
{
  "mensaje": "No existe calendario generado para el año 2099. Use /api/calendario/generar/2099"
}
```
HTTP Status: `404 Not Found`

---

## Ejecutar tests
```bash
mvn test
```
```
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Los tests son completamente unitarios con mocks — no requieren base de datos ni que el MS1 esté activo.

---

## Modelo de base de datos

```
┌─────────────┐        ┌──────────────────────────┐
│    tipo     │        │        calendario        │
├─────────────┤        ├──────────────────────────┤
│ id (PK)     │───────<│ id (PK)                  │
│ tipo        │        │ fecha                    │
└─────────────┘        │ id_tipo (FK → tipo.id)   │
                       │ descripcion              │
                       └──────────────────────────┘
```

**Valores de la tabla `tipo`:**
| id | tipo |
|----|------|
| 1  | Dia laboral |
| 2  | Fin de Semana |
| 3  | Dia festivo |

---

## Levantar todo con Docker Compose
Para iniciar ambas bases de datos de una vez (desde `validate_holydays_ms/`):
```bash
docker-compose up -d
```

Para detener:
```bash
docker-compose down
```

Para detener y eliminar los datos persistidos:
```bash
docker-compose down -v
```
