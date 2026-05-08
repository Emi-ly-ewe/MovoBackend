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
('014', 1, '9181', 'INTERCAMBIADOR STA.CRUZ', 1, 28.459, -16.2527, 'TITSA'), -- modificado
('014', 1, '9419', 'MIRAFLORES', 2, 28.4655, -16.2516, 'TITSA'), -- modificado
('014', 1, '9420', 'HOSPITALITO DR GUIGOU', 3, 28.4662, -16.2544, 'TITSA'),-- modificado
('014', 1, '1385', 'LA HIGUERITA', 18, 28.4696, -16.2937, 'TITSA'), -- modificado
('014', 1, '2625', 'INTERCAMBIADOR LAGUNA', 25, 28.4804, -16.3169, 'TITSA'), -- está bien de base

-- 🟢 LÍNEA 015 (Directo)
('015', 1, '9181', 'INTERCAMBIADOR STA.CRUZ', 1, 28.459, -16.2527, 'TITSA'), -- modificado
('015', 1, '9385', 'EL CORTE INGLÉS', 2, 28.4597, -16.2543, 'TITSA'), -- modificado
('015', 1, '9386', 'TRES DE MAYO', 3, 28.4589, -16.2577, 'TITSA'), -- modificado
('015', 1, '1279', 'CHAMBERÍ', 6, 28.4524, -16.2748, 'TITSA'), -- modificado
('015', 1, '1280', 'HOSPITAL LA CANDELARIA', 7, 28.4479, -16.2839, 'TITSA'), -- modificado
('015', 1, '1282', 'HOSPITAL UNIVERSITARIO', 9, 28.4569, -16.2938, 'TITSA'), -- modificado
('015', 1, '2533', 'CAMPUS DE GUAJARA', 10, 28.4672, -16.3042, 'TITSA'), -- modificado
('015', 1, '2625', 'INTERCAMBIADOR LAGUNA', 11, 28.4804, -16.3169, 'TITSA'), -- está bien de base

-- 🟢 LÍNEA 121 (Güímar -> Candelaria)
('121', 1, '2247', 'GÜÍMAR', 1, 28.3189,    -16.4086, 'TITSA'), -- modificado
('121', 1, '7064', 'ARAFO', 5, 28.34, -16.4191, 'TITSA'), -- modificado
('121', 1, '7119', 'CANDELARIA (T)', 15, 28.3553, -16.3712, 'TITSA'), -- modificado

-- 🟢 LÍNEA 128 (Intercambiador SC -> Güímar)
('128', 1, '9181', 'INTERCAMBIADOR STA.CRUZ', 1, 28.459, -16.2527, 'TITSA'), -- modificado
('128', 1, '1151', 'SANTA MARÍA DEL MAR (T)', 4, 28.4253, -16.3044, 'TITSA'), -- modificado
('128', 1, '7116', 'TABAIBA (T)', 7, 28.4057, -16.3313, 'TITSA'), -- modificado
('128', 1, '7118', 'CALETILLAS (T)', 9, 28.382, -16.3634, 'TITSA'), -- modificado
('128', 1, '7119', 'CANDELARIA (T)', 10, 28.3596, -16.3723, 'TITSA'), -- modificado
('128', 1, '2244', 'LA HIDALGA (T)', 13, 28.3341, -16.395, 'TITSA'), -- modificado
('128', 1, '2788', 'ARTESANÍA', 19, 28.3403, -16.4152, 'TITSA'), -- modificado
('128', 1, '8052', 'ESTACION DE ARAFO (T)', 23, 28.3379, -16.4195, 'TITSA'), -- modificado
('128', 1, '2247', 'GÜÍMAR', 35, 28.3189,    -16.4086, 'TITSA') -- modificado
    ON CONFLICT DO NOTHING;