package Models;
import java.time.LocalDate;

/**
 * Entidad que representa una mascota en el sistema.
 * Hereda de Base para obtener id y eliminado.
 *
 * Relación con Microchip:
 * - Una Mascota puede tener 0 o 1 Microchip (relación opcional)
 * - Se relaciona mediante FK microchip_id en la tabla mascotas
 *
 * Tabla BD: mascotas
 * Campos:
 * - id: BIGINT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT,
 * - eliminado:BOOLEAN NOT NULL DEFAULT FALSE,
 * - nombre: VARCHAR(60)  NOT NULL,
 * - especie: VARCHAR(30)  NOT NULL,
 * - raza: VARCHAR(60)  NULL,
 * - fecha_nacimiento: DATE NULL,
 * - duenio: VARCHAR(120) NOT NULL,
 * - microchip_id: BIGINT UNSIGNED NULL UNIQUE,
 */
public class Mascota extends Base {
     // Atributos principales
    private String nombre;           // NOT NULL, máx. 60
    private String especie;          // NOT NULL, máx. 30
    private String raza;             // máx. 60
    private LocalDate fechaNacimiento; 
    private String duenio;           // NOT NULL, máx. 120
    // Relación 1→1 con Microchip
    private Microchip microchip;
  
    /** Constructor por defecto para crear una Mascota nueva sin ID. */
    public Mascota() {
        super();
        }
    
     /**
     * Constructor completo para reconstruir una Mascota  desde la BD.
     * Usado por MascotaDAO al mapear ResultSet.
     * El microchip se asigna posteriormente con setMicrochip().
     */
    public Mascota(int id, String nombre, String especie, 
                   String raza, LocalDate fechaNacimiento, String duenio, Microchip microchip) {
        super(id, false);
        this.nombre = nombre;
        this.especie = especie;
        this.raza = raza;
        this.fechaNacimiento = fechaNacimiento;
        this.duenio = duenio;
        this.microchip = microchip;
    }
    
    // -------------------------
    // Getters y Setters
    // -------------------------

    public String getNombre() {
        return nombre;
    }
    
    /**
     * Establece el nombre de la mascota.
     * Validación: PersonaServiceImpl verifica que no esté vacío.
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEspecie() {
        return especie;
    }
    /**
     * Establece la especie de la mascota.
     * Validación: PersonaServiceImpl verifica que no esté vacío.
     */
    public void setEspecie(String especie) {
        this.especie = especie;
    }

    public String getRaza() {
        return raza;
    }
    
    /**
     * Establece la raza de la mascota.
     * Validación: PersonaServiceImpl verifica que no este vacio.
     */
    public void setRaza(String raza) {
        this.raza = raza;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }
    
    /**
     * Establece la fecha de nacimiento de la mascota.
     * Validación: PersonaServiceImpl verifica que no esté vacío.
     */
    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getDuenio() {
        return duenio;
    }
    
    /**
     * Establece el duenio de la mascota.
     * Validación: PersonaServiceImpl verifica que no esté vacío.
     */
    public void setDuenio(String duenio) {
        this.duenio = duenio;
    }

    public Microchip getMicrochip() {
        return microchip;
    }
    
    /**
     * Asocia o desasocia un microchip  a la mascota.
     * Si domicilio es null, la FK microchip_id será NULL en la BD.
     */
    public void setMicrochip(Microchip microchip) {
        this.microchip = microchip;
    }
    
    @Override
    public String toString() {
        return "Mascota {" +
               "id=" + getId() +
               ", eliminado=" + isEliminado() +
               ", nombre='" + nombre + '\'' +
               ", especie='" + especie + '\'' +
               ", raza='" + raza + '\'' +
               ", fechaNacimiento=" + fechaNacimiento +
               ", duenio='" + duenio + '\'' +
               ", microchip=" + microchip +
               '}';
}
}

