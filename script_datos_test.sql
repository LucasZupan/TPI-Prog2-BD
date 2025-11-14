USE mascotas_microchips;

-- Limpieza previa (por si ya existen registros)
DELETE FROM mascotas;
DELETE FROM microchips;

-- Inserción de microchips (15 en total)
INSERT INTO microchips (codigo, fecha_implantacion, veterinaria, observaciones, eliminado)
VALUES
  -- Implantados (asignados, activos)
  ('MC-1001', '2024-01-12', 'Vet Los Pinos', 'Implantado sin complicaciones', 0),
  ('MC-1002', '2024-02-05', 'Vet Central', 'Paciente tranquilo', 0),
  ('MC-1003', '2024-02-20', 'Vet Norte', 'Chequeo al día', 0),
  ('MC-1004', '2024-03-10', 'Vet San Roque', 'Implantación exitosa', 0),
  ('MC-1005', '2024-03-25', 'Vet del Parque', 'Ninguna observación', 0),

  -- Libres (no implantados → veterinaria y observaciones NULL)
  ('MC-2001', NULL, NULL, NULL, 0),
  ('MC-2002', NULL, NULL, NULL, 0),
  ('MC-2003', NULL, NULL, NULL, 0),
  ('MC-2004', NULL, NULL, NULL, 0),
  ('MC-2005', NULL, NULL, NULL, 0),

  -- Implantados pero inactivos (mascotas eliminadas)
  ('MC-3001', '2023-09-10', 'Vet San Martín', 'Implantación antigua', 1),
  ('MC-3002', '2023-08-05', 'Vet Central', 'Microchip desasignado', 1),
  ('MC-3003', '2022-12-18', 'Vet Los Pinos', 'Mascota fallecida', 1),
  ('MC-3004', '2023-02-20', 'Vet Norte', 'Inactivo por duplicado', 1),
  ('MC-3005', '2023-07-15', 'Vet del Parque', 'Desactivado', 1);

-- Inserción de mascotas (15 en total)
INSERT INTO mascotas (nombre, especie, raza, fecha_nacimiento, duenio, microchip_id, eliminado)
VALUES
  -- Activas con microchip
  ('Luna',  'Perro', 'Labrador', '2020-04-15', 'Carlos Gómez', 1, 0),
  ('Simba', 'Gato',  'Siames',   '2021-06-20', 'Ana Ruiz', 2, 0),
  ('Rocky', 'Perro', 'Boxer',    '2019-09-01', 'Lucía Torres', 3, 0),
  ('Milo',  'Gato',  'Persa',    '2022-03-12', 'Federico Díaz', 4, 0),
  ('Nala',  'Perro', 'Golden Retriever', '2020-11-05', 'Sofía Fernández', 5, 0),

  -- Activas sin microchip
  ('Toby',  'Perro', 'Beagle',   '2018-07-18', 'Laura Pérez', NULL, 0),
  ('Kira',  'Gato',  'Común Europeo', '2021-01-22', 'Mariano López', NULL, 0),
  ('Max',   'Perro', 'Caniche',  '2022-09-14', 'Carolina Herrera', NULL, 0),
  ('Coco',  'Gato',  'Bengalí',  '2023-02-08', 'Javier Romero', NULL, 0),
  ('Bobby', 'Perro', 'Bulldog',  '2019-10-03', 'Natalia Suárez', NULL, 0),

  -- Inactivas (eliminadas) con microchip inactivo
  ('Lola',  'Perro', 'Dálmata',   '2017-06-11', 'Diego Castro', 11, 1),
  ('Felix', 'Gato',  'Persa',     '2016-09-23', 'Patricia Gómez', 12, 1),
  ('Thor',  'Perro', 'Pastor Alemán', '2018-01-30', 'Gabriel Rivas', 13, 1),
  ('Mora',  'Gato',  'Siberiano', '2019-03-19', 'Rocío López', 14, 1),
  ('Bruno', 'Perro', 'Doberman',  '2020-07-04', 'Alejandro Díaz', 15, 1);

-- Verificación rápida
SELECT COUNT(*) AS total_microchips FROM microchips;
SELECT COUNT(*) AS total_mascotas FROM mascotas;

-- Listado de todas las mascotas con su microchip (si tienen)
SELECT m.id, m.nombre, m.especie, m.eliminado AS mascota_eliminada,
       mc.codigo AS microchip, mc.eliminado AS microchip_eliminado
FROM mascotas m
LEFT JOIN microchips mc ON m.microchip_id = mc.id
ORDER BY m.id;