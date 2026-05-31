# Festivos API — Colombia
API RESTful en Express JS + MongoDB que valida si una fecha es festiva en Colombia y lista los festivos de un año dado.

---

## Requisitos previos
- Node.js 20+
- Docker y Docker Compose (para la base de datos)

---

## Configuración

### 1. Levantar MongoDB con Docker
Desde la raíz del proyecto `validate_holydays_ms/`:
```bash
docker-compose up -d mongodb
```
Esto inicia MongoDB en `localhost:27017` con autenticación habilitada:
- Usuario: `admin`
- Contraseña: `password123`
- Base de datos: `festivos_db`

### 2. Variables de entorno
El archivo `.env` ya está configurado con los valores por defecto:
```
PORT=8080
MONGODB_URI=mongodb://admin:password123@localhost:27017/festivos_db?authSource=admin
```
Si necesitas cambiar algún valor, edita el archivo `.env`.

### 3. Instalar dependencias
```bash
npm install
```

### 4. Iniciar el servidor
```bash
npm start
```
Al arrancar, el servidor carga automáticamente los 19 festivos colombianos en MongoDB (solo la primera vez).

Salida esperada:
```
Conexión a MongoDB establecida correctamente
19 festivos colombianos insertados correctamente
Festivos API corriendo en http://localhost:8080
Swagger UI disponible en http://localhost:8080/api-docs
```

---

## Uso de la API

### Swagger UI
Accede a la documentación interactiva desde el navegador:
```
http://localhost:8080/api-docs
```
Desde ahí puedes probar todos los endpoints directamente.

---

### Endpoints

#### 1. Verificar si una fecha es festiva
```
GET /api/festivos/verificar/:año/:mes/:dia
```

**Ejemplos:**

Fecha festiva (San Pedro y San Pablo trasladado al lunes):
```bash
curl http://localhost:8080/api/festivos/verificar/2023/7/3
```
```json
{
  "esFestivo": true,
  "fecha": "2023-07-03",
  "mensaje": "Es Festivo",
  "nombreFestivo": "San Pedro y San Pablo"
}
```

Fecha no festiva:
```bash
curl http://localhost:8080/api/festivos/verificar/2023/2/28
```
```json
{
  "esFestivo": false,
  "fecha": "2023-02-28",
  "mensaje": "No es festivo"
}
```

Fecha inválida:
```bash
curl http://localhost:8080/api/festivos/verificar/2023/2/35
```
```json
{
  "esFestivo": null,
  "fecha": "2023-02-35",
  "mensaje": "Fecha No valida"
}
```

---

#### 2. Obtener lista de festivos del año
```
GET /api/festivos/obtener/:año
```

```bash
curl http://localhost:8080/api/festivos/obtener/2023
```
```json
[
  { "festivo": "Año nuevo",             "fecha": "2023-01-01" },
  { "festivo": "Santos Reyes",          "fecha": "2023-01-09" },
  { "festivo": "San José",              "fecha": "2023-03-20" },
  { "festivo": "Jueves Santo",          "fecha": "2023-04-06" },
  { "festivo": "Viernes Santo",         "fecha": "2023-04-07" },
  { "festivo": "Domingo de Pascua",     "fecha": "2023-04-09" },
  { "festivo": "Día del Trabajo",       "fecha": "2023-05-01" },
  { "festivo": "Ascensión del Señor",   "fecha": "2023-05-22" },
  { "festivo": "Corpus Christi",        "fecha": "2023-06-12" },
  { "festivo": "Sagrado Corazón",       "fecha": "2023-06-19" },
  { "festivo": "San Pedro y San Pablo", "fecha": "2023-07-03" },
  { "festivo": "Independencia Colombia","fecha": "2023-07-20" },
  { "festivo": "Batalla de Boyacá",     "fecha": "2023-08-07" },
  { "festivo": "Asunción de la Virgen", "fecha": "2023-08-21" },
  { "festivo": "Día de la Raza",        "fecha": "2023-10-16" },
  { "festivo": "Todos los santos",      "fecha": "2023-11-06" },
  { "festivo": "Independencia Cartagena","fecha": "2023-11-13" },
  { "festivo": "Inmaculada Concepción", "fecha": "2023-12-08" },
  { "festivo": "Navidad",               "fecha": "2023-12-25" }
]
```

---

## Ejecutar tests
```bash
npm test
```
```
Test Suites: 3 passed, 3 total
Tests:       24 passed, 24 total
```

Para ver cobertura:
```bash
npm run test:coverage
```

---

## Lógica de festivos colombianos

| Tipo | Descripción | Ejemplos |
|------|-------------|---------|
| 1 - Fijo | Fecha fija, no varía | Año nuevo (1 ene), Navidad (25 dic) |
| 2 - Puente | Se traslada al siguiente lunes si no cae en lunes | Santos Reyes, San José |
| 3 - Pascua | Se calcula desde el Domingo de Pascua | Jueves Santo (-3), Viernes Santo (-2) |
| 4 - Pascua + Puente | Calculado desde Pascua y trasladado al siguiente lunes | Ascensión (+40), Corpus Christi (+61) |

### Fórmula del Domingo de Pascua
```
a = año MOD 19
b = año MOD 4
c = año MOD 7
d = (19a + 24) MOD 30
dias = d + (2b + 4c + 6d + 5) MOD 7

Domingo de Ramos  = 15 de marzo + dias
Domingo de Pascua = Domingo de Ramos + 7 días
```

---

## Detener los servicios
```bash
docker-compose down
```
Para eliminar también los datos persistidos:
```bash
docker-compose down -v
```
