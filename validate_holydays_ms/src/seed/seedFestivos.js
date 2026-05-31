const TipoFestivo = require('../models/TipoFestivo');
const Festivo = require('../models/Festivo');

const tiposFestivos = [
  { codigo: 1, nombre: 'Fijo', descripcion: 'Festivo en fecha fija, no se puede variar' },
  { codigo: 2, nombre: 'Puente festivo', descripcion: 'Se traslada al siguiente lunes si no cae en lunes' },
  { codigo: 3, nombre: 'Basado en Pascua', descripcion: 'Calculado a partir del Domingo de Pascua' },
  { codigo: 4, nombre: 'Pascua + Puente', descripcion: 'Calculado desde Pascua y trasladado al siguiente lunes' }
];

const festivosFijos = [
  { nombre: 'Año nuevo',             idTipo: 1, dia: 1,  mes: 1  },
  { nombre: 'Día del Trabajo',       idTipo: 1, dia: 1,  mes: 5  },
  { nombre: 'Independencia Colombia',idTipo: 1, dia: 20, mes: 7  },
  { nombre: 'Batalla de Boyacá',     idTipo: 1, dia: 7,  mes: 8  },
  { nombre: 'Inmaculada Concepción', idTipo: 1, dia: 8,  mes: 12 },
  { nombre: 'Navidad',               idTipo: 1, dia: 25, mes: 12 }
];

const festivosPuente = [
  { nombre: 'Santos Reyes',               idTipo: 2, dia: 6,  mes: 1  },
  { nombre: 'San José',                   idTipo: 2, dia: 19, mes: 3  },
  { nombre: 'San Pedro y San Pablo',      idTipo: 2, dia: 29, mes: 6  },
  { nombre: 'Asunción de la Virgen',      idTipo: 2, dia: 15, mes: 8  },
  { nombre: 'Día de la Raza',             idTipo: 2, dia: 12, mes: 10 },
  { nombre: 'Todos los santos',           idTipo: 2, dia: 1,  mes: 11 },
  { nombre: 'Independencia de Cartagena', idTipo: 2, dia: 11, mes: 11 }
];

const festivosPascua = [
  { nombre: 'Jueves Santo',    idTipo: 3, diasPascua: -3 },
  { nombre: 'Viernes Santo',   idTipo: 3, diasPascua: -2 },
  { nombre: 'Domingo de Pascua', idTipo: 3, diasPascua: 0 }
];

const festivosPascuaPuente = [
  { nombre: 'Ascensión del Señor',      idTipo: 4, diasPascua: 40 },
  { nombre: 'Corpus Christi',           idTipo: 4, diasPascua: 61 },
  { nombre: 'Sagrado Corazón de Jesús', idTipo: 4, diasPascua: 68 }
];

const todosFestivos = [
  ...festivosFijos,
  ...festivosPuente,
  ...festivosPascua,
  ...festivosPascuaPuente
];

async function ejecutarSeed() {
  const totalTipos = await TipoFestivo.countDocuments();
  if (totalTipos === 0) {
    await TipoFestivo.insertMany(tiposFestivos);
    console.log('Tipos de festivo insertados correctamente');
  }

  const totalFestivos = await Festivo.countDocuments();
  if (totalFestivos === 0) {
    await Festivo.insertMany(todosFestivos);
    console.log(`${todosFestivos.length} festivos colombianos insertados correctamente`);
  }
}

module.exports = { ejecutarSeed };
