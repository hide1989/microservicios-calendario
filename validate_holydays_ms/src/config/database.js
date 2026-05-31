const mongoose = require('mongoose');

async function conectarBaseDeDatos() {
  const uri = process.env.MONGODB_URI || 'mongodb://localhost:27017/festivos_db';
  await mongoose.connect(uri);
  console.log('Conexión a MongoDB establecida correctamente');
}

module.exports = { conectarBaseDeDatos };
