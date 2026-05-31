jest.mock('../../services/festivoService');
const festivoService = require('../../services/festivoService');
const request = require('supertest');
const app = require('../../app');

describe('GET /api/festivos/verificar/:año/:mes/:dia', () => {
  test('retorna 200 con esFestivo true para una fecha festiva', async () => {
    festivoService.verificarSiFechaEsFestiva.mockResolvedValue({
      valida: true,
      esFestivo: true,
      nombreFestivo: 'San Pedro y San Pablo'
    });

    const respuesta = await request(app).get('/api/festivos/verificar/2023/6/12');

    expect(respuesta.status).toBe(200);
    expect(respuesta.body.esFestivo).toBe(true);
    expect(respuesta.body.mensaje).toBe('Es Festivo');
    expect(respuesta.body.nombreFestivo).toBe('San Pedro y San Pablo');
  });

  test('retorna 200 con esFestivo false para una fecha no festiva', async () => {
    festivoService.verificarSiFechaEsFestiva.mockResolvedValue({
      valida: true,
      esFestivo: false,
      nombreFestivo: null
    });

    const respuesta = await request(app).get('/api/festivos/verificar/2023/2/28');

    expect(respuesta.status).toBe(200);
    expect(respuesta.body.esFestivo).toBe(false);
    expect(respuesta.body.mensaje).toBe('No es festivo');
    expect(respuesta.body.nombreFestivo).toBeUndefined();
  });

  test('retorna 400 para una fecha inválida', async () => {
    festivoService.verificarSiFechaEsFestiva.mockResolvedValue({
      valida: false,
      esFestivo: null,
      nombreFestivo: null
    });

    const respuesta = await request(app).get('/api/festivos/verificar/2023/2/35');

    expect(respuesta.status).toBe(400);
    expect(respuesta.body.esFestivo).toBeNull();
    expect(respuesta.body.mensaje).toBe('Fecha No valida');
  });
});

describe('GET /api/festivos/obtener/:año', () => {
  test('retorna 200 con la lista de festivos del año', async () => {
    const festivosMock = [
      { festivo: 'Año nuevo', fecha: '2023-01-01' },
      { festivo: 'Santos Reyes', fecha: '2023-01-09' }
    ];
    festivoService.calcularFestivosDelAnio.mockResolvedValue(festivosMock);

    const respuesta = await request(app).get('/api/festivos/obtener/2023');

    expect(respuesta.status).toBe(200);
    expect(Array.isArray(respuesta.body)).toBe(true);
    expect(respuesta.body).toHaveLength(2);
    expect(respuesta.body[0].festivo).toBe('Año nuevo');
    expect(respuesta.body[0].fecha).toBe('2023-01-01');
  });

  test('retorna 200 con array vacío si no hay festivos', async () => {
    festivoService.calcularFestivosDelAnio.mockResolvedValue([]);

    const respuesta = await request(app).get('/api/festivos/obtener/2023');

    expect(respuesta.status).toBe(200);
    expect(respuesta.body).toEqual([]);
  });
});
