-- admin y dos solicitantes (contraseña de todos: Password123*)
INSERT INTO usuarios (documento, nombre, correo, password_hash, sede, area, estado, rol) VALUES
('1000000001', 'Admin HelpFlow', 'admin@helpflow.com',
 '$2a$10$oEwKpS9RgSFaMma50wSQf.tnYQOCNQv91kqJXRlxUsbT1dxC00F7K', 'BARRANQUILLA', 'TI', 'ACTIVO', 'ROLE_ADMIN'),
('1000000002', 'Ana Usuario', 'ana@helpflow.com',
 '$2a$10$oEwKpS9RgSFaMma50wSQf.tnYQOCNQv91kqJXRlxUsbT1dxC00F7K', 'BARRANQUILLA', 'FINANZAS', 'ACTIVO', 'ROLE_USUARIO'),
('1000000003', 'Luis Usuario', 'luis@helpflow.com',
 '$2a$10$oEwKpS9RgSFaMma50wSQf.tnYQOCNQv91kqJXRlxUsbT1dxC00F7K', 'MEDELLIN', 'VENTAS', 'ACTIVO', 'ROLE_USUARIO');

-- 20 usuarios técnicos: tec001@helpflow.com ... tec020@helpflow.com
INSERT INTO usuarios (documento, nombre, correo, password_hash, sede, area, estado, rol)
SELECT 'T' || lpad(n::text, 4, '0'),
       'Técnico ' || lpad(n::text, 3, '0'),
       'tec' || lpad(n::text, 3, '0') || '@helpflow.com',
       '$2a$10$oEwKpS9RgSFaMma50wSQf.tnYQOCNQv91kqJXRlxUsbT1dxC00F7K',
       CASE WHEN ((n - 1) / 2) % 2 = 0 THEN 'BARRANQUILLA' ELSE 'MEDELLIN' END,
       'SOPORTE TI', 'ACTIVO', 'ROLE_TECNICO'
FROM generate_series(1, 20) AS n;

-- perfiles técnicos TEC-001 ... TEC-020, 4 por categoría (mismo orden que dispatch)
INSERT INTO tecnicos (codigo, usuario_id, nombre, especialidad, sede, carga_maxima, estado)
SELECT 'TEC-' || lpad(n::text, 3, '0'), u.id, u.nombre,
       CASE (n - 1) / 4 WHEN 0 THEN 'HARDWARE' WHEN 1 THEN 'SOFTWARE'
                        WHEN 2 THEN 'RED' WHEN 3 THEN 'ACCESOS' ELSE 'OTRO' END,
       u.sede, 5, 'ACTIVO'
FROM generate_series(1, 20) AS n
JOIN usuarios u ON u.correo = 'tec' || lpad(n::text, 3, '0') || '@helpflow.com';

-- un técnico inactivo para demostrar la regla de técnico habilitado
UPDATE tecnicos SET estado = 'INACTIVO' WHERE codigo = 'TEC-004';
