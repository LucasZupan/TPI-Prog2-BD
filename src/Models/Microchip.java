package Models;
import java.time.LocalDate;

public class Microchip extends Base {

    private String codigo;           // NOT NULL, UNIQUE, máx. 25
    private LocalDate fechaImplantacion;
    private String veterinaria;      // máx. 120
    private String observaciones;    // máx. 255

    // -------------------------
    // Constructores
    // -------------------------
    public Microchip() {
        super();
    }

    public Microchip(int id, String codigo, LocalDate fechaImplantacion,
                     String veterinaria, String observaciones) {
        super(id, false); // inicializa atributos heredados
        this.codigo = codigo;
        this.fechaImplantacion = fechaImplantacion;
        this.veterinaria = veterinaria;
        this.observaciones = observaciones;
    }

    // -------------------------
    // Getters y Setters
    // -------------------------
    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public LocalDate getFechaImplantacion() {
        return fechaImplantacion;
    }

    public void setFechaImplantacion(LocalDate fechaImplantacion) {
        this.fechaImplantacion = fechaImplantacion;
    }

    public String getVeterinaria() {
        return veterinaria;
    }

    public void setVeterinaria(String veterinaria) {
        this.veterinaria = veterinaria;
    }

    public String getObservaciones() {
        return observaciones;
    }

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
