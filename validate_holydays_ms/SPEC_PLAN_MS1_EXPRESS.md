# PLAN DE DESARROLLO — MS1: Festivos API (Express JS + MongoDB)

## Fases de desarrollo

---

### FASE 0 — Estructura y configuración del proyecto
**Objetivo:** Dejar el proyecto listo para desarrollar.

- [ ] Inicializar proyecto Node.js (`npm init`)
- [ ] Instalar dependencias de producción:
  - `express`, `mongoose`, `dotenv`
  - `swagger-jsdoc`, `swagger-ui-express`
- [ ] Instalar dependencias de desarrollo:
  - `jest`, `supertest`, `@types/jest`
- [ ] Crear estructura de carpetas por capas:
  ```
  src/config, src/models, src/repositories,
  src/services, src/controllers, src/routes, src/seed
  ```
- [ ] Configurar `package.json` con scripts: `start`, `dev`, `test`
- [ ] Crear `.env` con `PORT=8080` y `MONGODB_URI`
- [ ] Crear `src/config/database.js` — conexión Mongoose
- [ ] Crear `src/app.js` — Express base con middleware JSON
- [ ] Verificar arranque del servidor sin errores

---

### FASE 1 — Modelos de datos MongoDB
**Objetivo:** Definir los schemas Mongoose.

- [ ] Crear `src/models/TipoFestivo.js`
  - Campos: `codigo` (Number, único), `nombre` (String), `descripcion` (String)
- [ ] Crear `src/models/Festivo.js`
  - Campos: `nombre` (String), `idTipo` (Number), `dia` (Number, nullable), `mes` (Number, nullable), `diasPascua` (Number, nullable)
- [ ] Verificar que los modelos se registran correctamente en Mongoose

---

### FASE 2 — Seed de datos
**Objetivo:** Pre-cargar los 18 festivos colombianos en MongoDB al arrancar.

- [ ] Crear `src/seed/seedFestivos.js`
  - Insertar 4 registros en `tipo_festivo` (Fijo, Puente, Pascua, Pascua+Puente)
  - Insertar 18 festivos con sus tipos, días, meses y diasPascua
  - Verificar con `countDocuments` si ya existe seed antes de insertar (idempotente)
- [ ] Llamar al seed desde `src/app.js` después de conectar a MongoDB

---

### FASE 3 — Servicio de cálculo de Pascua
**Objetivo:** Implementar la lógica matemática del Domingo de Pascua.

- [ ] Crear `src/services/pascuaService.js`
  - Función `calcularDomingoPascua(año)`:
    - Aplicar fórmula: `a, b, c, d, dias`
    - Retornar objeto `Date` del Domingo de Pascua
  - Función `calcularSiguienteLunes(fecha)`:
    - Si la fecha ya es lunes → retornar misma fecha
    - Si no → avanzar días hasta el siguiente lunes

---

### FASE 4 — Repositorio de festivos
**Objetivo:** Encapsular las consultas a MongoDB.

- [ ] Crear `src/repositories/festivoRepository.js`
  - Función `obtenerTodosLosFestivos()` → retorna todos los documentos de `Festivo`

---

### FASE 5 — Servicio de negocio de festivos
**Objetivo:** Calcular las fechas reales de festivos para un año dado.

- [ ] Crear `src/services/festivoService.js`
  - Función `calcularFestivosDelAnio(año)`:
    - Obtener todos los festivos del repositorio
    - Para tipo 1 (Fijo): crear `Date(año, mes-1, dia)`
    - Para tipo 2 (Puente): crear fecha y aplicar `calcularSiguienteLunes`
    - Para tipo 3 (Pascua): calcular Domingo de Pascua y sumar `diasPascua`
    - Para tipo 4 (Pascua+Puente): calcular fecha tipo 3 y aplicar `calcularSiguienteLunes`
    - Retornar array: `[{ festivo: "Nombre", fecha: "YYYY-MM-DD" }]`
  - Función `verificarSiFechaEsFestiva(año, mes, dia)`:
    - Validar que la fecha sea válida (construir Date y verificar coherencia)
    - Si inválida → retornar `{ valida: false }`
    - Si válida → obtener festivos del año y buscar coincidencia
    - Retornar `{ valida: true, esFestivo: boolean, nombreFestivo: string | null }`

---

### FASE 6 — Controller y rutas
**Objetivo:** Exponer los endpoints HTTP.

- [ ] Crear `src/controllers/festivoController.js`
  - `verificarFecha(req, res)`: maneja GET `/verificar/:año/:mes/:dia`
  - `obtenerFestivos(req, res)`: maneja GET `/obtener/:año`
- [ ] Crear `src/routes/festivoRoutes.js` — registrar rutas con el controller
- [ ] Registrar router en `app.js` bajo prefijo `/api/festivos`

---

### FASE 7 — Swagger
**Objetivo:** Documentar y poder probar la API desde el navegador.

- [ ] Crear `src/config/swagger.js`
  - Configurar `swaggerJsdoc` con info del servicio, servidor `localhost:8080`
  - Definir schemas: `FestivoResponse`, `VerificarFechaResponse`, `ErrorResponse`
- [ ] Agregar JSDoc `@swagger` en cada endpoint del controller
- [ ] Montar `swagger-ui-express` en ruta `/api-docs`
- [ ] Verificar que Swagger UI carga en `http://localhost:8080/api-docs`

---

### FASE 8 — Tests unitarios
**Objetivo:** Cubrir la lógica de negocio con Jest.

- [ ] Crear `src/services/__tests__/pascuaService.spec.js`
  - Test: Domingo de Pascua para años conocidos (1999, 2023, 2024)
  - Test: `calcularSiguienteLunes` con distintos días de semana
- [ ] Crear `src/services/__tests__/festivoService.spec.js`
  - Mock de `festivoRepository`
  - Test: lista correcta de festivos para 2023
  - Test: fecha festiva, no festiva e inválida
- [ ] Crear `src/controllers/__tests__/festivoController.spec.js`
  - Mock de `festivoService`
  - Test HTTP con Supertest para los 4 casos del spec

---

### FASE 9 — Docker
**Objetivo:** Dockerizar MongoDB.

- [ ] Crear `docker-compose.yml` en la raíz del workspace con servicio `mongodb`
  - Imagen: `mongo:7`
  - Puerto: `27017:27017`
  - Volumen persistente
  - Variable: `MONGO_INITDB_DATABASE=festivos_db`

---

## Dependencias entre fases
```
FASE 0 → FASE 1 → FASE 2 → FASE 4 → FASE 5 → FASE 6 → FASE 7
                  FASE 3 ─────────────↗
FASE 8 (después de FASE 5 y FASE 6)
FASE 9 (paralela, solo necesaria para correr en Docker)
```

---

## Criterios de aceptación
- [ ] `GET /api/festivos/verificar/2023/6/12` retorna `{ esFestivo: true, nombreFestivo: "San Pedro y San Pablo" }`
- [ ] `GET /api/festivos/verificar/2023/2/28` retorna `{ esFestivo: false }`
- [ ] `GET /api/festivos/verificar/2023/2/35` retorna `400` con mensaje "Fecha No valida"
- [ ] `GET /api/festivos/obtener/2023` retorna array de 18 festivos
- [ ] Swagger UI accesible y funcional en `/api-docs`
- [ ] Todos los tests de Jest pasan sin errores
