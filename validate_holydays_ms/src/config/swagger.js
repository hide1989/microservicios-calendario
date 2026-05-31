const swaggerJsdoc = require('swagger-jsdoc');

const swaggerOptions = {
  definition: {
    openapi: '3.0.0',
    info: {
      title: 'Festivos API — Colombia',
      version: '1.0.0',
      description:
        'API RESTful para verificar si una fecha es festiva en Colombia y listar los festivos de un año. ' +
        'Los festivos incluyen fechas fijas, festivos de puente (Ley 51 de 1983), ' +
        'festivos basados en el Domingo de Pascua y combinación de ambos.',
      contact: {
        name: 'Pascual Bravo — Programación Avanzada'
      }
    },
    servers: [
      {
        url: 'http://localhost:8080',
        description: 'Servidor de desarrollo'
      }
    ],
    components: {
      schemas: {
        FechaFestivaResponse: {
          type: 'object',
          properties: {
            esFestivo: { type: 'boolean', example: true },
            fecha: { type: 'string', format: 'date', example: '2023-06-12' },
            mensaje: { type: 'string', example: 'Es Festivo' },
            nombreFestivo: { type: 'string', example: 'San Pedro y San Pablo' }
          }
        },
        FechaNoFestivaResponse: {
          type: 'object',
          properties: {
            esFestivo: { type: 'boolean', example: false },
            fecha: { type: 'string', format: 'date', example: '2023-02-28' },
            mensaje: { type: 'string', example: 'No es festivo' }
          }
        },
        FechaInvalidaResponse: {
          type: 'object',
          properties: {
            esFestivo: { type: 'string', nullable: true, example: null },
            fecha: { type: 'string', example: '2023-02-35' },
            mensaje: { type: 'string', example: 'Fecha No valida' }
          }
        },
        FestivoItem: {
          type: 'object',
          properties: {
            festivo: { type: 'string', example: 'Año nuevo' },
            fecha: { type: 'string', format: 'date', example: '2023-01-01' }
          }
        }
      }
    },
    tags: [
      {
        name: 'Festivos',
        description: 'Operaciones relacionadas con festivos colombianos'
      }
    ]
  },
  apis: ['./src/controllers/*.js']
};

const swaggerSpec = swaggerJsdoc(swaggerOptions);

module.exports = swaggerSpec;
