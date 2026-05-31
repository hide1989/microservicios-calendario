function calcularDomingoPascua(anio) {
  const a = anio % 19;
  const b = anio % 4;
  const c = anio % 7;
  const d = (19 * a + 24) % 30;
  const dias = d + (2 * b + 4 * c + 6 * d + 5) % 7;

  const domingoDeramos = new Date(anio, 2, 15 + dias);
  const domingoPascua = new Date(domingoDeramos);
  domingoPascua.setDate(domingoDeramos.getDate() + 7);

  return domingoPascua;
}

function calcularSiguienteLunes(fecha) {
  const diaSemana = fecha.getDay();
  if (diaSemana === 1) return new Date(fecha);

  const diasHastaLunes = diaSemana === 0 ? 1 : 8 - diaSemana;
  const siguienteLunes = new Date(fecha);
  siguienteLunes.setDate(fecha.getDate() + diasHastaLunes);
  return siguienteLunes;
}

module.exports = { calcularDomingoPascua, calcularSiguienteLunes };
