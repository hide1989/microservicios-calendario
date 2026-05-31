INSERT INTO tipo (id, tipo) VALUES (1, 'Dia laboral') ON CONFLICT (id) DO NOTHING;
INSERT INTO tipo (id, tipo) VALUES (2, 'Fin de Semana') ON CONFLICT (id) DO NOTHING;
INSERT INTO tipo (id, tipo) VALUES (3, 'Dia festivo') ON CONFLICT (id) DO NOTHING;
