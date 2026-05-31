const { calcularDomingoPascua, calcularSiguienteLunes } = require('../pascuaService');

describe('calcularDomingoPascua', () => {
  test('calcula correctamente el Domingo de Pascua para 1999 (4 de abril)', () => {
    const resultado = calcularDomingoPascua(1999);
    expect(resultado.getFullYear()).toBe(1999);
    expect(resultado.getMonth()).toBe(3);
    expect(resultado.getDate()).toBe(4);
  });

  test('calcula correctamente el Domingo de Pascua para 2023 (9 de abril)', () => {
    const resultado = calcularDomingoPascua(2023);
    expect(resultado.getFullYear()).toBe(2023);
    expect(resultado.getMonth()).toBe(3);
    expect(resultado.getDate()).toBe(9);
  });

  test('calcula correctamente el Domingo de Pascua para 2024 (31 de marzo)', () => {
    const resultado = calcularDomingoPascua(2024);
    expect(resultado.getFullYear()).toBe(2024);
    expect(resultado.getMonth()).toBe(2);
    expect(resultado.getDate()).toBe(31);
  });

  test('retorna un objeto Date válido', () => {
    const resultado = calcularDomingoPascua(2025);
    expect(resultado).toBeInstanceOf(Date);
    expect(isNaN(resultado.getTime())).toBe(false);
  });
});

describe('calcularSiguienteLunes', () => {
  test('no traslada si la fecha ya es lunes', () => {
    const lunes = new Date(2023, 0, 9);
    const resultado = calcularSiguienteLunes(lunes);
    expect(resultado.getDate()).toBe(9);
    expect(resultado.getDay()).toBe(1);
  });

  test('traslada al siguiente lunes si la fecha es martes', () => {
    const martes = new Date(2023, 0, 10);
    const resultado = calcularSiguienteLunes(martes);
    expect(resultado.getDate()).toBe(16);
    expect(resultado.getDay()).toBe(1);
  });

  test('traslada al siguiente lunes si la fecha es jueves (29 junio 2023)', () => {
    const jueves = new Date(2023, 5, 29);
    const resultado = calcularSiguienteLunes(jueves);
    expect(resultado.getDate()).toBe(3);
    expect(resultado.getMonth()).toBe(6);
    expect(resultado.getDay()).toBe(1);
  });

  test('traslada al siguiente lunes si la fecha es domingo', () => {
    const domingo = new Date(2023, 0, 8);
    const resultado = calcularSiguienteLunes(domingo);
    expect(resultado.getDate()).toBe(9);
    expect(resultado.getDay()).toBe(1);
  });
});
