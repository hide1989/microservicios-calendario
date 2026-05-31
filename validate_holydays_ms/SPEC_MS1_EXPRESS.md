# SPEC — MS1: Festivos API (Express JS + MongoDB)

## Descripción general
Microservicio REST en Node.js con Express que valida si una fecha es festiva o no,
y lista todos los festivos de un año dado, usando MongoDB como base de datos.

---

## Stack técnico
| Componente     | Tecnología              |
|----------------|-------------------------|
| Runtime        | Node.js 20+             |
| Framework      | Express 4               |
| Base de datos  | MongoDB 7 (Docker)      |
| ODM            | Mongoose                |
| Documentación  | Swagger UI (swagger-jsdoc + swagger-ui-express) |
| Tests          | Jest + Supertest        |
| Variables env  | dotenv                  |

---

## Arquitectura por capas

```
src/
├── config/
│   ├── database.js          # Conexión Mongoose
│   └── swagger.js           # Configuración Swagger
├── models/
│   ├── TipoFestivo.js       # Schema Mongoose: tipos de cálculo
│   └── Festivo.js           # Schema Mongoose: definición de festivos
├── repositories/
│   └── festivoRepository.js # Consultas a MongoDB
├── services/
│   ├── pascuaService.js     # Lógica cálculo domingo de Pascua
│   └── festivoService.js    # Lógica de negocio festivos
├── controllers/
│   └── festivoController.js # Manejo de request/response HTTP
├── routes/
│   └── festivoRoutes.js     # Definición de rutas
├── seed/
│   └── seedFestivos.js      # Carga inicial de datos en MongoDB
└── app.js                   # Configuración Express + arranque
```

---

## Modelo de datos MongoDB

### Colección: `tipo_festivo`
```json
{
  "_id": ObjectId,
  "codigo": Number,       // 1, 2, 3, 4
  "nombre": String,       // "Fijo", "Puente festivo", "Pascua", "Pascua + Puente"
  "descripcion": String
}
```

### Colección: `festivo`
```json
{
  "_id": ObjectId,
  "nombre": String,
  "idTipo": Number,
  "dia": Number,          // null para tipos 3 y 4
  "mes": Number,          // null para tipos 3 y 4
  "diasPascua": Number    // null para tipos 1 y 2
}
```

### Seed data

**Tipo 1 - Fijo:**
| Día | Mes | Nombre                  |
|-----|-----|-------------------------|
| 1   | 1   | Año nuevo               |
| 1   | 5   | Día del Trabajo         |
| 20  | 7   | Independencia Colombia  |
| 7   | 8   | Batalla de Boyacá       |
| 8   | 12  | Inmaculada Concepción   |
| 25  | 12  | Navidad                 |

**Tipo 2 - Puente festivo (traslada al siguiente lunes):**
| Día | Mes | Nombre                       |
|-----|-----|------------------------------|
| 6   | 1   | Santos Reyes                 |
| 19  | 3   | San José                     |
| 29  | 6   | San Pedro y San Pablo        |
| 15  | 8   | Asunción de la Virgen        |
| 12  | 10  | Día de la Raza               |
| 1   | 11  | Todos los santos             |
| 11  | 11  | Independencia de Cartagena   |

**Tipo 3 - Basado en Domingo de Pascua:**
| Días desde Pascua | Nombre              |
|-------------------|---------------------|
| -3                | Jueves Santo        |
| -2                | Viernes Santo       |
| 0                 | Domingo de Pascua   |

**Tipo 4 - Basado en Pascua + Puente festivo:**
| Días desde Pascua | Nombre                    |
|-------------------|---------------------------|
| +40               | Ascensión del Señor       |
| +61               | Corpus Christi            |
| +68               | Sagrado Corazón de Jesús  |

---

## Lógica de negocio

### Cálculo del Domingo de Pascua
```
a = año MOD 19
b = año MOD 4
c = año MOD 7
d = (19a + 24) MOD 30
dias = d + (2b + 4c + 6d + 5) MOD 7

Domingo de Ramos = 15 de marzo + dias días
Domingo de Pascua = Domingo de Ramos + 7 días
```

### Ley de Puente Festivo (estándar colombiano)
Si el festivo NO cae en lunes → se traslada al siguiente lunes.
Si ya cae en lunes → no se traslada.

### Validación de fecha
Una fecha es inválida si no puede construirse como objeto Date real
(ej: día 35, mes 13, feb 30, etc.).

---

## API Endpoints

### 1. Verificar si una fecha es festiva

```
GET /api/festivos/verificar/:año/:mes/:dia
```

**Parámetros de ruta:**
| Param | Tipo   | Ejemplo | Descripción      |
|-------|--------|---------|------------------|
| año   | Number | 2023    | Año de la fecha  |
| mes   | Number | 6       | Mes (1-12)       |
| dia   | Number | 12      | Día del mes      |

**Respuestas:**

`200 OK` — Fecha festiva:
```json
{
  "esFestivo": true,
  "fecha": "2023-06-12",
  "mensaje": "Es Festivo",
  "nombreFestivo": "San Pedro y San Pablo"
}
```

`200 OK` — Fecha no festiva:
```json
{
  "esFestivo": false,
  "fecha": "2023-02-28",
  "mensaje": "No es festivo"
}
```

`400 Bad Request` — Fecha inválida:
```json
{
  "esFestivo": null,
  "fecha": "2023-02-35",
  "mensaje": "Fecha No valida"
}
```

---

### 2. Obtener lista de festivos del año

```
GET /api/festivos/obtener/:año
```

**Parámetros de ruta:**
| Param | Tipo   | Ejemplo | Descripción    |
|-------|--------|---------|----------------|
| año   | Number | 2023    | Año a consultar |

**Respuesta `200 OK`:**
```json
[
  { "festivo": "Año nuevo",      "fecha": "2023-01-01" },
  { "festivo": "Santos Reyes",   "fecha": "2023-01-09" },
  { "festivo": "San José",       "fecha": "2023-03-20" },
  { "festivo": "Jueves Santo",   "fecha": "2023-04-06" },
  ...
]
```

---

## Variables de entorno (.env)
```
PORT=8080
MONGODB_URI=mongodb://localhost:27017/festivos_db
```

---

## Especificación de tests (Jest)

### pascuaService.spec.js
| Test | Entrada | Resultado esperado |
|------|---------|-------------------|
| Calcula domingo de Pascua 1999 | año=1999 | 1999-04-04 |
| Calcula domingo de Pascua 2023 | año=2023 | 2023-04-09 |
| Calcula domingo de Pascua 2024 | año=2024 | 2024-03-31 |
| Traslada puente al siguiente lunes (martes) | 2023-06-29 (jueves) | 2023-07-03 |
| No traslada si ya es lunes | 2023-01-09 (lunes) | 2023-01-09 |

### festivoService.spec.js
| Test | Entrada | Resultado esperado |
|------|---------|-------------------|
| Retorna lista de 18 festivos para 2023 | año=2023 | array.length === 18 |
| 12 junio 2023 es festivo | año=2023, mes=6, dia=12 | esFestivo: true |
| 28 febrero 2023 NO es festivo | año=2023, mes=2, dia=28 | esFestivo: false |
| 35 febrero 2023 es fecha inválida | año=2023, mes=2, dia=35 | fecha inválida |

### festivoController.spec.js (Supertest)
| Test | Request | Status esperado |
|------|---------|----------------|
| GET /api/festivos/verificar/2023/6/12 | - | 200, esFestivo: true |
| GET /api/festivos/verificar/2023/2/28 | - | 200, esFestivo: false |
| GET /api/festivos/verificar/2023/2/35 | - | 400 |
| GET /api/festivos/obtener/2023 | - | 200, array con 18 items |

---

## Docker
MongoDB corre en contenedor Docker expuesto en puerto `27017`.
Definido en `docker-compose.yml` en la raíz del proyecto.
