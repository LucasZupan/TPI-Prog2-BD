package Service;

import Dao.GenericDAO;
import Dao.IMicrochipDAO;
import Models.Microchip;
import java.util.List;

/**
 * Implementación del servicio de negocio para la entidad Microchip.
 * Capa intermedia entre la UI y el DAO que aplica validaciones de negocio.
 *
 * Responsabilidades:
 * - Validar que los datos del microchip sean correctos ANTES de persistir
 * - Aplicar reglas de negocio
 * - Delegar operaciones de BD al DAO
 * - Transformar excepciones técnicas en errores de negocio comprensibles
 *
 * Patrón: Service Layer con inyección de dependencias
 */
public class MicrochipServiceImpl implements GenericService<Microchip>{
    
    /**
     * DAO para acceso a datos de microchip.
     * Inyectado en el constructor (Dependency Injection).   
     */
    private final IMicrochipDAO  microchipDAO;   
    

    /**
     * Constructor con inyección de dependencias.
     * Valida que el DAO no sea null (fail-fast).
     *
     * @param microchipDAO DAO de microchips (normalmente MicrochipDAO)
     * @throws IllegalArgumentException si microchipDAO es null
     */
    public MicrochipServiceImpl(IMicrochipDAO microchipDAO) {
        if (microchipDAO == null) throw new IllegalArgumentException("MicrochipDAO no puede ser null");
        this.microchipDAO = microchipDAO;
    }
    
    /**
     * Inserta un nuevo microchip en la base de datos.
     *
     * Flujo:
     * 1. Valida que codigo no sea null y sea Unique
     * 2. Delega al DAO para insertar
     * 3. El DAO asigna el ID autogenerado al objeto microchip
     *
     * @param microchip Microchip a insertar (id será ignorado y regenerado)
     * @throws Exception Si la validación falla o hay error de BD
     */
    @Override
    public void insertar(Microchip microchip) throws Exception {
        validateMicrochip(microchip);
        validateCodigoUnique(microchip.getCodigo(), microchip.getId());
        microchipDAO.insertar(microchip);
    }
    
    /**
     * Actualiza un microchip existente en la base de datos.
     *
     * Validaciones:
     * - El microchip debe tener datos válidos
     * - El ID debe ser > 0 (debe ser un microchip ya persistido)
     *
     * @param microchip Microchip con los datos actualizados
     * @throws Exception Si la validación falla o el microchip no existe
     */
    @Override
    public void actualizar(Microchip microchip) throws Exception {
        validateMicrochip(microchip);
        if (microchip.getId() <= 0) {
            throw new IllegalArgumentException("El ID del microchip debe ser mayor a 0 para actualizar");
        }
        validateCodigoUnique(microchip.getCodigo(), microchip.getId());
        microchipDAO.actualizar(microchip);
    }
 
    /**
     * Elimina lógicamente un microchip (soft delete).
     * Marca el microchip como eliminado=TRUE sin borrarlo físicamente.
     *
     * ⚠️ ADVERTENCIA: Este método NO verifica si hay mascotas asociadas.
     * Puede dejar referencias huérfanas en mascotas.microchip_id.
     *
     * ALTERNATIVA SEGURA: Usar MascotaServiceImpl.eliminarMicrochipDeMascota()
     * que actualiza la FK antes de eliminar (opción 10 del menú).
     *
     * @param id ID del microchip a eliminar
     * @throws Exception Si id <= 0 o no existe el microchip
     */
    @Override
    public void eliminar(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        microchipDAO.eliminar(id);
    }

    /**
     * Obtiene un microchip por su ID.
     *
     * @param id ID del microchip a buscar
     * @return Microchip encontrado, o null si no existe o está eliminado
     * @throws Exception Si id <= 0 o hay error de BD
     */
    @Override
    public Microchip getById(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        return microchipDAO.getById(id);
    }

    /**
     * Obtiene todos los microchips activos (eliminado=FALSE).
     *
     * @return Lista de microchips activos (puede estar vacía)
     * @throws Exception Si hay error de BD
     */
    @Override
    public List<Microchip> getAll() throws Exception {
        return microchipDAO.getAll();
    }    
    
    /**
     * Valida que un microchip tenga datos correctos.
     *
     * Reglas de negocio aplicadas:
     *
     * @param microchip Microchip a validar
     * @throws IllegalArgumentException Si alguna validación falla
     */
    private void validateMicrochip(Microchip microchip) {
        if (microchip == null) {
            throw new IllegalArgumentException("El microchip no puede ser null");
        }
        if (microchip.getCodigo()== null || microchip.getCodigo().trim().isEmpty()) {
            throw new IllegalArgumentException("El codigo no puede estar vacío");
        }        
    }
    
        /**
     * Valida que un Codigo sea único en el sistema.
     
     * Lógica:
     * 1. Busca si existe un microchip con ese codigo en la BD
     * 2. Si NO existe → OK, el codigo es único
     * 3. Si existe → Verifica si es el mismo microchip que estamos actualizando:
     *    a. Si microchipId == null (INSERT) → Error, codigo duplicado
     *    b. Si microchipId != null (UPDATE) y existente.id == microchipId → OK, es el mismo microchip
     *    c. Si microchipId != null (UPDATE) y existente.id != microchipId → Error, microchip duplicado 
     *
     * @param codigo Codigo a validar
     * @param microchipId del microchip (null para INSERT, != null para UPDATE)
     * @throws IllegalArgumentException Si el microchip ya existe y pertenece a otra mascota
     * @throws Exception Si hay error de BD al buscar
     */
    private void validateCodigoUnique(String codigo, Integer microchipId) throws Exception {
        Microchip existente = microchipDAO.buscarPorCodigo(codigo);
        if (existente != null) {
            // Existe un microchip con ese Codigo
            if (microchipId == null || existente.getId() != microchipId) {
                // Es INSERT (microchipId == null) o es UPDATE pero el microchip pertenece a otra mascota
                throw new IllegalArgumentException("Ya existe un Microchip con el codigo: " + codigo);
            }
            // Si llegamos aquí: es UPDATE y el codigo pertenece al microchip → OK
        }
    }
}    

