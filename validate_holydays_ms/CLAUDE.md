# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Comandos esenciales

```bash
# Levantar MongoDB (requerido antes de iniciar el servidor) — desde la raíz del proyecto
docker compose up -d dockerbdfestivos

# Desarrollo con recarga automática
npm run dev

# Producción
npm start

# Tests
npm test

# Tests con cobertura
npm run test:coverage

# Ejecutar un solo archivo de tests
npx jest src/services/__tests__/festivoService.spec.js --forceExit
```

## Variables de entorno

Copiar `.env.example` a `.env` si es necesario. Valores por defecto:
```
PORT=8080
MONGODB_URI=mongodb://admin:password123@localhost:27017/festivos_db?authSource=admin
```

## Arquitectura

Microservicio REST para validar fechas festivas colombianas. Stack: Express 5 + Mongoose + MongoDB.

**Flujo de capas:**
```
Route → Controller → Service → Repository → Model (Mongoose)
```

- `src/routes/festivoRoutes.js` — define los dos endpoints (`/verificar/:año/:mes/:dia` y `/obtener/:año`)
- `src/controllers/festivoController.js` — parsea parámetros, formatea respuestas HTTP, contiene las anotaciones JSDoc para Swagger
- `src/services/festivoService.js` — lógica central: valida fechas, calcula la fecha real de cada festivo según su tipo, devuelve resultados
- `src/services/pascuaService.js` — funciones puras matemáticas: algoritmo de Gauss para Domingo de Pascua y cálculo del siguiente lunes
- `src/repositories/festivoRepository.js` — única función: `obtenerTodosLosFestivos()` desde MongoDB
- `src/models/Festivo.js` y `src/models/TipoFestivo.js` — esquemas Mongoose
- `src/seed/seedFestivos.js` — carga idempotente de los 19 festivos en MongoDB al iniciar el servidor

## Tipos de festivos (idTipo en la colección `festivo`)

| idTipo | Comportamiento | Campos usados |
|--------|---------------|---------------|
| 1 | Fecha fija | `dia`, `mes` |
| 2 | Puente: se traslada al siguiente lunes | `dia`, `mes` |
| 3 | Calculado desde Domingo de Pascua | `diasPascua` (offset en días) |
| 4 | Pascua + traslado al siguiente lunes | `diasPascua` |

## Tests

Los tests viven en `src/**/__tests__/*.spec.js`. Jest mockea el repositorio (`jest.mock`) para que los tests de servicio no requieran conexión a MongoDB. Los tests de controlador usan `supertest` contra la instancia de Express exportada por `src/app.js` (que no llama a `iniciarServidor()` cuando no es el módulo principal).
