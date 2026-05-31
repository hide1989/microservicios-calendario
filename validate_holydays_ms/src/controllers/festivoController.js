const festivoService = require('../services/festivoService');

/**
 * @swagger
 * /api/festivos/verificar/{año}/{mes}/{dia}:
 *   get:
 *     summary: Verifica si una fecha es festiva en Colombia
 *     tags: [Festivos]
 *     parameters:
 *       - in: path
 *         name: año
 *         required: true
 *         schema:
 *           type: integer
 *           example: 2023
 *         description: Año de la fecha a verificar
 *       - in: path
 *         name: mes
 *         required: true
 *         schema:
 *           type: integer
 *           minimum: 1
 *           maximum: 12
 *           example: 6
 *         description: Mes de la fecha (1-12)
 *       - in: path
 *         name: dia
 *         required: true
 *         schema:
 *           type: integer
 *           minimum: 1
 *           maximum: 31
 *           example: 12
 *         description: Día del mes
 *     responses:
 *       200:
 *         description: Resultado de la verificación
 *         content:
 *           application/json:
 *             schema:
 *               oneOf:
 *                 - $ref: '#/components/schemas/FechaFestivaResponse'
 *                 - $ref: '#/components/schemas/FechaNoFestivaResponse'
 *             examples:
 *               esFestivo:
 *                 summary: La fecha es festiva
 *                 value:
 *                   esFestivo: true
 *                   fecha: "2023-06-12"
 *                   mensaje: "Es Festivo"
 *                   nombreFestivo: "San Pedro y San Pablo"
 *               noEsFestivo:
 *                 summary: La fecha no es festiva
 *                 value:
 *                   esFestivo: false
 *                   fecha: "2023-02-28"
 *                   mensaje: "No es festivo"
 *       400:
 *         description: Fecha inválida
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/FechaInvalidaResponse'
 *             example:
 *               esFestivo: null
 *               fecha: "2023-02-35"
 *               mensaje: "Fecha No valida"
 */
async function verificarFecha(req, res) {
  const anio = parseInt(req.params.año);
  const mes = parseInt(req.params.mes);
  const dia = parseInt(req.params.dia);

  const resultado = await festivoService.verificarSiFechaEsFestiva(anio, mes, dia);

  if (!resultado.valida) {
    return res.status(400).json({
      esFestivo: null,
      fecha: `${anio}-${String(mes).padStart(2, '0')}-${String(dia).padStart(2, '0')}`,
      mensaje: 'Fecha No valida'
    });
  }

  return res.status(200).json({
    esFestivo: resultado.esFestivo,
    fecha: `${anio}-${String(mes).padStart(2, '0')}-${String(dia).padStart(2, '0')}`,
    mensaje: resultado.esFestivo ? 'Es Festivo' : 'No es festivo',
    ...(resultado.esFestivo && { nombreFestivo: resultado.nombreFestivo })
  });
}

/**
 * @swagger
 * /api/festivos/obtener/{año}:
 *   get:
 *     summary: Obtiene la lista completa de festivos colombianos para un año dado
 *     tags: [Festivos]
 *     parameters:
 *       - in: path
 *         name: año
 *         required: true
 *         schema:
 *           type: integer
 *           example: 2023
 *         description: Año para el cual se desea obtener los festivos
 *     responses:
 *       200:
 *         description: Lista de festivos del año
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 $ref: '#/components/schemas/FestivoItem'
 *             example:
 *               - festivo: "Año nuevo"
 *                 fecha: "2023-01-01"
 *               - festivo: "Santos Reyes"
 *                 fecha: "2023-01-09"
 *               - festivo: "San José"
 *                 fecha: "2023-03-20"
 */
async function obtenerFestivosPorAnio(req, res) {
  const anio = parseInt(req.params.año);
  const festivos = await festivoService.calcularFestivosDelAnio(anio);
  return res.status(200).json(festivos);
}

module.exports = { verificarFecha, obtenerFestivosPorAnio };
