# SPEC — MS2: Calendario API (Spring Boot + Postgres)

## Descripción general
Microservicio REST en Java con Spring Boot que actúa como cliente del MS1 (Festivos API),
genera un calendario anual clasificando cada día del año, y lo persiste en PostgreSQL.

---

## Stack técnico
| Componente     | Tecnología                        |
|----------------|-----------------------------------|
| Lenguaje       | Java 21                           |
| Framework      | Spring Boot 3.2                   |
| Base de datos  | PostgreSQL 16 (Docker)            |
| ORM            | Spring Data JPA / Hibernate       |
| Cliente HTTP   | WebClient (Spring WebFlux)        |
| Documentación  | SpringDoc OpenAPI 3 (Swagger UI)  |
| Tests          | JUnit 5 + Mockito                 |
| Build          | Maven                             |
| Variables env  | application.properties / env vars |

---

## Arquitectura por capas

```
src/main/java/com/pascualbravo/calendario/
├── config/
│   ├── WebClientConfig.java       # Bean WebClient para llamar MS1
│   └── SwaggerConfig.java         # Configuración OpenAPI
├── model/
│   ├── Tipo.java                  # Entidad JPA tabla "tipo"
│   └── Calendario.java            # Entidad JPA tabla "calendario"
├── repository/
│   ├── TipoRepository.java        # Spring Data JPA
│   └── CalendarioRepository.java  # Spring Data JPA
├── dto/
│   ├── FestivoDTO.java            # DTO respuesta MS1
│   └── CalendarioDTO.java         # DTO respuesta listar calendario
├── service/
│   ├── FestivoClientService.java  # Consume API MS1
│   └── CalendarioService.java     # Lógica de negocio del calendario
└── controller/
    ├── FestivoController.java     # Endpoints /api/festivos
    └── CalendarioController.java  # Endpoints /api/calendario

src/main/resources/
├── application.properties
└── data.sql                       # Seed tipos de día
```

---

## Modelo de datos PostgreSQL

### Tabla: `tipo`
```sql
CREATE TABLE tipo (
    id   SERIAL PRIMARY KEY,
    tipo VARCHAR(50) NOT NULL
);

-- Seed:
INSERT INTO tipo (id, tipo) VALUES (1, 'Dia laboral');
INSERT INTO tipo (id, tipo) VALUES (2, 'Fin de Semana');
INSERT INTO tipo (id, tipo) VALUES (3, 'Dia festivo');
```

### Tabla: `calendario`
```sql
CREATE TABLE calendario (
    id          SERIAL PRIMARY KEY,
    fecha       DATE        NOT NULL,
    id_tipo     INT         NOT NULL REFERENCES tipo(id),
    descripcion VARCHAR(100) NOT NULL
);
```

---

## Entidades JPA

### Tipo
```java
@Entity
@Table(name = "tipo")
public class Tipo {
    @Id
    private Integer id;
    private String tipo;
}
```

### Calendario
```java
@Entity
@Table(name = "calendario")
public class Calendario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDate fecha;

    @ManyToOne
    @JoinColumn(name = "id_tipo")
    private Tipo tipo;

    private String descripcion;
}
```

---

## Integración con MS1

El servicio llama a:
```
GET http://localhost:8080/api/festivos/obtener/{año}
```

Respuesta esperada del MS1:
```json
[
  { "festivo": "Año nuevo",    "fecha": "2023-01-01" },
  { "festivo": "Santos Reyes", "fecha": "2023-01-09" },
  ...
]
```

URL del MS1 configurable vía `application.properties`:
```properties
ms1.festivos.base-url=http://localhost:8080
```

---

## Lógica de generación de calendario

Para cada día del año dado:
1. Llamar MS1 y obtener lista de fechas festivas.
2. Construir un `Set<LocalDate>` con las fechas festivas.
3. Por cada día del año:
   - Si `fecha` está en el set de festivos → `idTipo=3` (Día festivo), `descripcion = "{NombreDia} - {NombreFestivo}"`
   - Si `fecha` es sábado o domingo → `idTipo=2` (Fin de Semana), `descripcion = "{NombreDia}"`
   - En otro caso → `idTipo=1` (Día laboral), `descripcion = "{NombreDia}"`
4. Si ya existen registros para ese año → se omite la inserción y retorna `true`.
5. Guardar todos los registros en la tabla `calendario`.

**Nombres de día:** Lunes, Martes, Miércoles, Jueves, Viernes, Sábado, Domingo.

---

## API Endpoints

### 1. Listar festivos del año (consume MS1)

```
GET /api/festivos/obtener/{año}
```

**Parámetro:**
| Param | Tipo | Ejemplo | Descripción     |
|-------|------|---------|-----------------|
| año   | int  | 2023    | Año a consultar |

**Respuesta `200 OK`:**
```json
[
  { "festivo": "Año nuevo",    "fecha": "2023-01-01" },
  { "festivo": "Santos Reyes", "fecha": "2023-01-09" },
  ...
]
```

---

### 2. Generar y persistir calendario del año

```
GET /api/calendario/generar/{año}
```

**Parámetro:**
| Param | Tipo | Ejemplo | Descripción     |
|-------|------|---------|-----------------|
| año   | int  | 2023    | Año a generar   |

**Respuesta `200 OK`:**
```json
true
```

**Comportamiento:**
- Si el año ya existe en BD → retorna `true` sin reinsertar.
- Si el año no existe → genera los ~365/366 días, persiste y retorna `true`.

---

### 3. Listar calendario completo del año

```
GET /api/calendario/listar/{año}
```

**Parámetro:**
| Param | Tipo | Ejemplo | Descripción     |
|-------|------|---------|-----------------|
| año   | int  | 2023    | Año a listar    |

**Respuesta `200 OK`:**
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
  ...
]
```

**Respuesta `404 Not Found`** si no se ha generado el calendario para ese año:
```json
{ "mensaje": "No existe calendario generado para el año 2023. Use /api/calendario/generar/2023" }
```

---

## Variables de entorno / application.properties
```properties
server.port=8081
spring.datasource.url=jdbc:postgresql://localhost:5432/calendario_db
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
ms1.festivos.base-url=http://localhost:8080
springdoc.swagger-ui.path=/swagger-ui.html
```

---

## Especificación de tests (JUnit 5 + Mockito)

### FestivoClientServiceTest
| Test | Mock | Resultado esperado |
|------|------|--------------------|
| Retorna lista de festivos del MS1 | WebClient mockeado | Lista con 18 festivos para 2023 |
| Maneja error de conexión al MS1 | WebClient lanza excepción | Lanza RuntimeException descriptiva |

### CalendarioServiceTest
| Test | Mock | Resultado esperado |
|------|------|--------------------|
| Genera calendario 2023 con 365 días | FestivoClientService mock + Repo mock | saveAll llamado con 365 registros |
| 1 enero 2023 clasificado como festivo | Festivos mock incluyen 2023-01-01 | idTipo=3, descripcion contiene "Año nuevo" |
| 2 enero 2023 clasificado como laboral | Festivos mock sin 2023-01-02, lunes | idTipo=1 |
| 7 enero 2023 clasificado como fin de semana | Festivos mock, sábado | idTipo=2 |
| Si año ya existe, retorna true sin insertar | Repo mock devuelve count > 0 | true, saveAll no llamado |

### CalendarioControllerTest
| Test | Mock | Status esperado |
|------|------|----------------|
| GET /api/calendario/generar/2023 | Service mock retorna true | 200, body: true |
| GET /api/calendario/listar/2023 | Service mock retorna lista | 200, array no vacío |
| GET /api/calendario/listar/2099 | Service mock retorna vacío | 404 |
| GET /api/festivos/obtener/2023 | FestivoClientService mock | 200, array con festivos |

---

## Docker
PostgreSQL corre en contenedor Docker expuesto en puerto `5432`.
Definido en `docker-compose.yml` en la raíz del proyecto.
