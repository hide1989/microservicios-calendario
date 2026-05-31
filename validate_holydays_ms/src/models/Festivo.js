const mongoose = require('mongoose');

const festivoSchema = new mongoose.Schema({
  nombre: { type: String, required: true },
  idTipo: { type: Number, required: true },
  dia: { type: Number, default: null },
  mes: { type: Number, default: null },
  diasPascua: { type: Number, default: null }
});

module.exports = mongoose.model('Festivo', festivoSchema, 'festivo');
