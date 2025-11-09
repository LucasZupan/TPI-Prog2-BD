package Models;

public abstract class Base {
    private int id; //Identificador unico
    private Boolean eliminado; // Marca en la BD si el elemento esta eliminado
    
    public Base(int id, Boolean eliminado) {
    this.id = id;
    this.eliminado = eliminado;
    }
    
    public Base() {    
    this.eliminado = false;
    }
    public int getId() {
        return id;
    }
     
    public void setId(int id) {
        this.id = id;
    }
       
    public Boolean isEliminado() {
        return eliminado;
    }  
 
    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
    }
    
}
