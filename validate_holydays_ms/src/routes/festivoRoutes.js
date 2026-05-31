const express = require('express');
const router = express.Router();
const festivoController = require('../controllers/festivoController');

router.get('/verificar/:año/:mes/:dia', festivoController.verificarFecha);
router.get('/obtener/:año', festivoController.obtenerFestivosPorAnio);

module.exports = router;
