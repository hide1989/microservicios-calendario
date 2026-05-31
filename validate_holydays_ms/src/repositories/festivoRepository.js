const Festivo = require('../models/Festivo');

async function obtenerTodosLosFestivos() {
  return Festivo.find({});
}

module.exports = { obtenerTodosLosFestivos };
