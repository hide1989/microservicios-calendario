require('dotenv').config();
const express = require('express');
const swaggerUi = require('swagger-ui-express');
const swaggerSpec = require('./config/swagger');
const { conectarBaseDeDatos } = require('./config/database');
const { ejecutarSeed } = require('./seed/seedFestivos');
const festivoRoutes = require('./routes/festivoRoutes');

const app = express();

app.use(express.json());

app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(swaggerSpec, {
  customSiteTitle: 'Festivos API — Colombia',
  swaggerOptions: { tryItOutEnabled: true }
}));

app.use('/api/festivos', festivoRoutes);

app.get('/', (req, res) => {
  res.json({
    servicio: 'Festivos API Colombia',
    version: '1.0.0',
    documentacion: '/api-docs'
  });
});

async function iniciarServidor() {
  await conectarBaseDeDatos();
  await ejecutarSeed();

  const puerto = process.env.PORT || 8080;
  app.listen(puerto, () => {
    console.log(`Festivos API corriendo en http://localhost:${puerto}`);
    console.log(`Swagger UI disponible en http://localhost:${puerto}/api-docs`);
  });
}

if (require.main === module) {
  iniciarServidor();
}

module.exports = app;
