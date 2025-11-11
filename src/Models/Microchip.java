package Models;

import java.time.LocalDate;


/**
 * Entidad que representa un microchip en el sistema.
 * Hereda de Base para obtener id y eliminado.
 *
 * Relación con Mascota:
 * - Una Mascota puede tener 0 o 1 Microchip
 * - Un Microchip solo puede estar asociado a una Mascota(relación 1:1 desde Mascota)
 *
 * Tabla BD: microchips
 * Campos:
 * - id: BIGINT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT, (heredado de Base)
 * - eliminado: BOOLEAN NOT NULL DEFAULT FALSE, (heredado de Base)
 * - codigo: VARCHAR(25)  NOT NULL UNIQUE,
 * - fecha_implantacion: DATE NULL,
 * - veterinaria: VARCHAR(120) NULL,
 * - observaciones: VARCHAR(255) NULL,
 */

public class Microchip extends Base {

    private String codigo;           // NOT NULL, UNIQUE, máx. 25
    private LocalDate fechaImplantacion;
    private String veterinaria;      // máx. 120
    private String observaciones;    // máx. 255

    /**
     * Constructor por defecto para crear un microchip nuevo.
     * El ID será asignado por la BD al insertar.
     * El flag eliminado se inicializa en false por Base.
     */
    public Microchip() {
        super();
    }
    /**
     * Constructor completo para reconstruir un Microchip desde la base de datos.
     * Usado por MascotaDAO y MicrochipDAO al mapear ResultSet.
     */
    public Microchip(int id, String codigo, LocalDate fechaImplantacion,
                     String veterinaria, String observaciones) {
        super(id, false); // Llama al constructor de Base con eliminado=false
        this.codigo = codigo;
        this.fechaImplantacion = fechaImplantacion;
        this.veterinaria = veterinaria;
        this.observaciones = observaciones;
    }


    public String getCodigo() {
        return codigo;
    }
    /**
     * Establece el codigo.
     * Validación: MicrochipServiceImpl verifica que no esté vacío.
     */
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public LocalDate getFechaImplantacion() {
        return fechaImplantacion;
    }
    /**
     * Establece la fecha de implantacion.
    */
    public void setFechaImplantacion(LocalDate fechaImplantacion) {
        this.fechaImplantacion = fechaImplantacion;
    }

    public String getVeterinaria() {
        return veterinaria;
    }
    /**
     * Establece la veterinaria.
    */
    public void setVeterinaria(String veterinaria) {
        this.veterinaria = veterinaria;
    }

    public String getObservaciones() {
        return observaciones;
    }
    /**
     * Establece las observaciones.
    */
    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    @Override
    public String toString() {
        return "Microchip{" +
                "id=" + getId() +
                ", eliminado=" + isEliminado() +
                ", codigo='" + codigo + '\'' +
                ", fechaImplantacion=" + fechaImplantacion +
                ", veterinaria='" + veterinaria + '\'' +
                ", observaciones='" + observaciones + '\'' +
                '}';
    }
}
