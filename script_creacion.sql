CREATE DATABASE IF NOT EXISTS mascotas_microchips
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE mascotas_microchips;

-- Tabla B: Microchip (PK propia) - crear primero
CREATE TABLE IF NOT EXISTS microchips (
  id                  BIGINT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT,
  eliminado           BOOLEAN NOT NULL DEFAULT FALSE,
  codigo              VARCHAR(25)  NOT NULL UNIQUE,
  fecha_implantacion  DATE         NULL,
  veterinaria         VARCHAR(120) NULL,
  observaciones       VARCHAR(255) NULL,
  -- CHECKS
  CONSTRAINT chk_micro_eliminado   CHECK (eliminado IN (0,1)),
  CONSTRAINT chk_codigo_no_vacio   CHECK (TRIM(codigo) <> '')
) ENGINE=InnoDB;

-- Tabla A: Mascota  (FK UNIQUE nullable → 1:1)
CREATE TABLE IF NOT EXISTS mascotas (
  id               BIGINT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT,
  eliminado        BOOLEAN NOT NULL DEFAULT FALSE,
  nombre           VARCHAR(60)  NOT NULL,
  especie          VARCHAR(30)  NOT NULL,
  raza             VARCHAR(60)  NULL,
  fecha_nacimiento DATE         NULL,
  duenio           VARCHAR(120) NOT NULL,
  microchip_id     BIGINT UNSIGNED NULL UNIQUE,

  CONSTRAINT fk_mascotas_microchip
    FOREIGN KEY (microchip_id) REFERENCES microchips (id)
    ON UPDATE RESTRICT ON DELETE RESTRICT,

  -- CHECKs
  CONSTRAINT chk_nombre_no_vacio   CHECK (TRIM(nombre)  <> ''),
  CONSTRAINT chk_especie_no_vacio  CHECK (TRIM(especie) <> ''),
  CONSTRAINT chk_duenio_no_vacio   CHECK (TRIM(duenio)  <> ''),
  CONSTRAINT chk_mascota_eliminado CHECK (eliminado IN (0,1))
) ENGINE=InnoDB;

--  Trigger anti-reasignacion (una vez asignado, no cambiar a otra mascota)
DELIMITER //

CREATE TRIGGER trg_mascotas_no_reassign_microchip
BEFORE UPDATE ON mascotas
FOR EACH ROW
BEGIN
  -- Si antes tenía microchip y ahora lo quiero cambiar o sacar:
  IF OLD.microchip_id IS NOT NULL 
     AND NEW.microchip_id <> OLD.microchip_id THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'No se permite cambiar o quitar un microchip ya asignado a una mascota';
  END IF;
END//

DELIMITER ;