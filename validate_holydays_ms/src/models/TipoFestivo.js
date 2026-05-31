const mongoose = require('mongoose');

const tipoFestivoSchema = new mongoose.Schema({
  codigo: { type: Number, required: true, unique: true },
  nombre: { type: String, required: true },
  descripcion: { type: String, required: true }
});

module.exports = mongoose.model('TipoFestivo', tipoFestivoSchema, 'tipo_festivo');
