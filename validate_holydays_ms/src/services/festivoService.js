const { calcularDomingoPascua, calcularSiguienteLunes } = require('./pascuaService');
const festivoRepository = require('../repositories/festivoRepository');

function formatearFecha(fecha) {
  const anio = fecha.getFullYear();
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${anio}-${mes}-${dia}`;
}

function esFechaValida(anio, mes, dia) {
  const fecha = new Date(anio, mes - 1, dia);
  return (
    fecha.getFullYear() === anio &&
    fecha.getMonth() === mes - 1 &&
    fecha.getDate() === dia
  );
}

function calcularFechaFestivo(festivo, anio) {
  if (festivo.idTipo === 1) {
    return new Date(anio, festivo.mes - 1, festivo.dia);
  }

  if (festivo.idTipo === 2) {
    const fechaOriginal = new Date(anio, festivo.mes - 1, festivo.dia);
    return calcularSiguienteLunes(fechaOriginal);
  }

  const domingoPascua = calcularDomingoPascua(anio);

  if (festivo.idTipo === 3) {
    const fecha = new Date(domingoPascua);
    fecha.setDate(domingoPascua.getDate() + festivo.diasPascua);
    return fecha;
  }

  if (festivo.idTipo === 4) {
    const fechaBase = new Date(domingoPascua);
    fechaBase.setDate(domingoPascua.getDate() + festivo.diasPascua);
    return calcularSiguienteLunes(fechaBase);
  }

  return null;
}

async function calcularFestivosDelAnio(anio) {
  const festivosDefinicion = await festivoRepository.obtenerTodosLosFestivos();

  return festivosDefinicion
    .map(festivo => {
      const fecha = calcularFechaFestivo(festivo, anio);
      if (!fecha) return null;
      return { festivo: festivo.nombre, fecha: formatearFecha(fecha) };
    })
    .filter(Boolean)
    .sort((a, b) => a.fecha.localeCompare(b.fecha));
}

async function verificarSiFechaEsFestiva(anio, mes, dia) {
  if (!esFechaValida(anio, mes, dia)) {
    return { valida: false, esFestivo: null, nombreFestivo: null };
  }

  const fechaBuscada = formatearFecha(new Date(anio, mes - 1, dia));
  const festivosDelAnio = await calcularFestivosDelAnio(anio);
  const encontrado = festivosDelAnio.find(f => f.fecha === fechaBuscada);

  return {
    valida: true,
    esFestivo: Boolean(encontrado),
    nombreFestivo: encontrado ? encontrado.festivo : null
  };
}

module.exports = { calcularFestivosDelAnio, verificarSiFechaEsFestiva };
