package Models;
import java.time.LocalDate;

public class Mascota extends Base {
     // Atributos principales
    private String nombre;           // NOT NULL, máx. 60
    private String especie;          // NOT NULL, máx. 30
    private String raza;             // máx. 60
    private LocalDate fechaNacimiento; 
    private String duenio;           // NOT NULL, máx. 120
    // Relación 1→1 con Microchip
    private Microchip microchip;
    
    public Mascota() {
        super();
        }

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

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEspecie() {
        return especie;
    }

    public void setEspecie(String especie) {
        this.especie = especie;
    }

    public String getRaza() {
        return raza;
    }

    public void setRaza(String raza) {
        this.raza = raza;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getDuenio() {
        return duenio;
    }

    public void setDuenio(String duenio) {
        this.duenio = duenio;
    }

    public Microchip getMicrochip() {
        return microchip;
    }

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

