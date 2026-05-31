jest.mock('../../repositories/festivoRepository');
const festivoRepository = require('../../repositories/festivoRepository');
const { calcularFestivosDelAnio, verificarSiFechaEsFestiva } = require('../festivoService');

const festivosMock = [
  { nombre: 'Año nuevo',              idTipo: 1, dia: 1,  mes: 1,  diasPascua: null },
  { nombre: 'Día del Trabajo',        idTipo: 1, dia: 1,  mes: 5,  diasPascua: null },
  { nombre: 'Independencia Colombia', idTipo: 1, dia: 20, mes: 7,  diasPascua: null },
  { nombre: 'Batalla de Boyacá',      idTipo: 1, dia: 7,  mes: 8,  diasPascua: null },
  { nombre: 'Inmaculada Concepción',  idTipo: 1, dia: 8,  mes: 12, diasPascua: null },
  { nombre: 'Navidad',                idTipo: 1, dia: 25, mes: 12, diasPascua: null },
  { nombre: 'Santos Reyes',               idTipo: 2, dia: 6,  mes: 1,  diasPascua: null },
  { nombre: 'San José',                   idTipo: 2, dia: 19, mes: 3,  diasPascua: null },
  { nombre: 'San Pedro y San Pablo',      idTipo: 2, dia: 29, mes: 6,  diasPascua: null },
  { nombre: 'Asunción de la Virgen',      idTipo: 2, dia: 15, mes: 8,  diasPascua: null },
  { nombre: 'Día de la Raza',             idTipo: 2, dia: 12, mes: 10, diasPascua: null },
  { nombre: 'Todos los santos',           idTipo: 2, dia: 1,  mes: 11, diasPascua: null },
  { nombre: 'Independencia de Cartagena', idTipo: 2, dia: 11, mes: 11, diasPascua: null },
  { nombre: 'Jueves Santo',     idTipo: 3, dia: null, mes: null, diasPascua: -3 },
  { nombre: 'Viernes Santo',    idTipo: 3, dia: null, mes: null, diasPascua: -2 },
  { nombre: 'Domingo de Pascua', idTipo: 3, dia: null, mes: null, diasPascua: 0 },
  { nombre: 'Ascensión del Señor',      idTipo: 4, dia: null, mes: null, diasPascua: 40 },
  { nombre: 'Corpus Christi',           idTipo: 4, dia: null, mes: null, diasPascua: 61 },
  { nombre: 'Sagrado Corazón de Jesús', idTipo: 4, dia: null, mes: null, diasPascua: 68 }
];

beforeEach(() => {
  festivoRepository.obtenerTodosLosFestivos.mockResolvedValue(festivosMock);
});

describe('calcularFestivosDelAnio', () => {
  test('retorna exactamente 19 festivos para el año 2023', async () => {
    const festivos = await calcularFestivosDelAnio(2023);
    expect(festivos).toHaveLength(19);
  });

  test('incluye Año nuevo el 1 de enero', async () => {
    const festivos = await calcularFestivosDelAnio(2023);
    const anioNuevo = festivos.find(f => f.festivo === 'Año nuevo');
    expect(anioNuevo).toBeDefined();
    expect(anioNuevo.fecha).toBe('2023-01-01');
  });

  test('San Pedro y San Pablo (29 jun, jueves) se traslada al 3 de julio (lunes)', async () => {
    const festivos = await calcularFestivosDelAnio(2023);
    const sanPedro = festivos.find(f => f.festivo === 'San Pedro y San Pablo');
    expect(sanPedro).toBeDefined();
    expect(sanPedro.fecha).toBe('2023-07-03');
  });

  test('Jueves Santo 2023 cae el 6 de abril', async () => {
    const festivos = await calcularFestivosDelAnio(2023);
    const juevesSanto = festivos.find(f => f.festivo === 'Jueves Santo');
    expect(juevesSanto).toBeDefined();
    expect(juevesSanto.fecha).toBe('2023-04-06');
  });

  test('Domingo de Pascua 2023 cae el 9 de abril', async () => {
    const festivos = await calcularFestivosDelAnio(2023);
    const pascua = festivos.find(f => f.festivo === 'Domingo de Pascua');
    expect(pascua).toBeDefined();
    expect(pascua.fecha).toBe('2023-04-09');
  });

  test('la lista está ordenada por fecha ascendente', async () => {
    const festivos = await calcularFestivosDelAnio(2023);
    for (let i = 1; i < festivos.length; i++) {
      expect(festivos[i].fecha >= festivos[i - 1].fecha).toBe(true);
    }
  });
});

describe('verificarSiFechaEsFestiva', () => {
  test('12 de junio de 2023 es festivo (San Pedro y San Pablo trasladado)', async () => {
    const resultado = await verificarSiFechaEsFestiva(2023, 7, 3);
    expect(resultado.valida).toBe(true);
    expect(resultado.esFestivo).toBe(true);
    expect(resultado.nombreFestivo).toBe('San Pedro y San Pablo');
  });

  test('28 de febrero de 2023 no es festivo', async () => {
    const resultado = await verificarSiFechaEsFestiva(2023, 2, 28);
    expect(resultado.valida).toBe(true);
    expect(resultado.esFestivo).toBe(false);
    expect(resultado.nombreFestivo).toBeNull();
  });

  test('1 de enero de 2023 es festivo (Año nuevo)', async () => {
    const resultado = await verificarSiFechaEsFestiva(2023, 1, 1);
    expect(resultado.valida).toBe(true);
    expect(resultado.esFestivo).toBe(true);
    expect(resultado.nombreFestivo).toBe('Año nuevo');
  });

  test('35 de febrero de 2023 es una fecha inválida', async () => {
    const resultado = await verificarSiFechaEsFestiva(2023, 2, 35);
    expect(resultado.valida).toBe(false);
    expect(resultado.esFestivo).toBeNull();
  });

  test('30 de febrero de 2023 es una fecha inválida', async () => {
    const resultado = await verificarSiFechaEsFestiva(2023, 2, 30);
    expect(resultado.valida).toBe(false);
  });
});
