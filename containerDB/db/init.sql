CREATE TABLE IF NOT EXISTS lineas (
                                      id VARCHAR(20) PRIMARY KEY,
    nombre TEXT NOT NULL,
    operador VARCHAR(20) NOT NULL
    );

CREATE TABLE IF NOT EXISTS paradas (
                                       linea VARCHAR(20) NOT NULL,
    sentido INTEGER NOT NULL,
    parada_id VARCHAR(20) NOT NULL,
    nombre TEXT NOT NULL,
    orden_parada INTEGER NOT NULL,
    lat DOUBLE PRECISION,
    lng DOUBLE PRECISION,
    operador VARCHAR(20) NOT NULL,
    PRIMARY KEY (linea, sentido, parada_id)
    );

CREATE INDEX IF NOT EXISTS idx_paradas_linea ON paradas(linea);
CREATE INDEX IF NOT EXISTS idx_paradas_operador ON paradas(operador);
CREATE INDEX IF NOT EXISTS idx_paradas_coords ON paradas(lat, lng);

-- ==========================================
-- INSERTS: LÍNEAS DE TITSA
-- ==========================================
INSERT INTO lineas (id, nombre, operador) VALUES
                                              ('014', 'SANTA CRUZ - LA LAGUNA (POR LA CUESTA)', 'TITSA'),
                                              ('015', 'SANTA CRUZ - LA LAGUNA (DIRECTA)', 'TITSA'),
                                              ('121', 'GÜIMAR - ARAFO - POLÍGONO DE GÜÍMAR - CANDELARIA', 'TITSA'),
                                              ('128', 'SANTA CRUZ - ARAFO - GÜÍMAR', 'TITSA')
    ON CONFLICT DO NOTHING;

-- ==========================================
-- INSERTS: PARADAS OFICIALES (Con IDs Reales)
-- ==========================================
INSERT INTO paradas (linea, sentido, parada_id, nombre, orden_parada, lat, lng, operador) VALUES
-- 🟢 LÍNEA 014 (Intercambiador SC -> Intercambiador LL)
('014', 1, '9181', 'INTERCAMBIADOR STA.CRUZ', 1, 28.4585, -16.2562, 'TITSA'),
('014', 1, '9419', 'MIRAFLORES', 2, 28.4633, -16.2530, 'TITSA'),
('014', 1, '9420', 'HOSPITALITO DR GUIGOU', 3, 28.4658, -16.2541, 'TITSA'),
('014', 1, '1385', 'LA HIGUERITA', 18, 28.4716, -16.3021, 'TITSA'),
('014', 1, '2625', 'INTERCAMBIADOR LAGUNA', 25, 28.4804, -16.3169, 'TITSA'),

-- 🟢 LÍNEA 015 (Directo)
('015', 1, '9181', 'INTERCAMBIADOR STA.CRUZ', 1, 28.4585, -16.2562, 'TITSA'),
('015', 1, '9385', 'EL CORTE INGLÉS', 2, 28.4565, -16.2580, 'TITSA'),
('015', 1, '9386', 'TRES DE MAYO', 3, 28.4550, -16.2620, 'TITSA'),
('015', 1, '1279', 'CHAMBERÍ', 6, 28.4510, -16.2800, 'TITSA'),
('015', 1, '1280', 'HOSPITAL LA CANDELARIA', 7, 28.4489, -16.2872, 'TITSA'),
('015', 1, '1282', 'HOSPITAL UNIVERSITARIO', 9, 28.4575, -16.2926, 'TITSA'),
('015', 1, '2533', 'CAMPUS DE GUAJARA', 10, 28.4687, -16.3021, 'TITSA'),
('015', 1, '2625', 'INTERCAMBIADOR LAGUNA', 11, 28.4804, -16.3169, 'TITSA'),

-- 🟢 LÍNEA 121 (Güímar -> Candelaria)
('121', 1, '2247', 'GÜÍMAR', 1, 28.3148, -16.4116, 'TITSA'),
('121', 1, '7064', 'ARAFO', 5, 28.3394, -16.4172, 'TITSA'),
('121', 1, '7119', 'CANDELARIA (T)', 15, 28.3533, -16.3712, 'TITSA'),

-- 🟢 LÍNEA 128 (Intercambiador SC -> Güímar)
('128', 1, '9181', 'INTERCAMBIADOR STA.CRUZ', 1, 28.4585, -16.2562, 'TITSA'),
('128', 1, '1151', 'SANTA MARÍA DEL MAR (T)', 4, 28.4230, -16.3100, 'TITSA'),
('128', 1, '7116', 'TABAIBA (T)', 7, 28.4042, -16.3245, 'TITSA'),
('128', 1, '7118', 'CALETILLAS (T)', 9, 28.3688, -16.3620, 'TITSA'),
('128', 1, '7119', 'CANDELARIA (T)', 10, 28.3533, -16.3712, 'TITSA'),
('128', 1, '2244', 'LA HIDALGA (T)', 13, 28.3420, -16.3900, 'TITSA'),
('128', 1, '2788', 'ARTESANÍA', 19, 28.3385, -16.3945, 'TITSA'),
('128', 1, '8052', 'ESTACION DE ARAFO (T)', 23, 28.3410, -16.4160, 'TITSA'),
('128', 1, '2247', 'GÜÍMAR', 35, 28.3148, -16.4116, 'TITSA')
    ON CONFLICT DO NOTHING;