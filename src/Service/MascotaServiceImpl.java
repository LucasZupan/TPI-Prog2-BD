package Service;

import Models.Mascota;
import java.util.List;

/**
 * Implementación del servicio de negocio para la entidad Mascota.
 * Capa intermedia entre la UI y el DAO que aplica validaciones de negocio complejas.
 *
 * Responsabilidades:
 * - Validar datos de mascota ANTES de persistir 
 * - COORDINAR operaciones entre Mascota y Microchip (transaccionales)
 * - Proporcionar métodos de búsqueda especializados (nombre, duenio)
 * - Implementar eliminación SEGURA de microchips (evita FKs huérfanas)
 *
 * Patrón: Service Layer con inyección de dependencias y coordinación de servicios
 */
public class MascotaServiceImpl implements GenericService<Mascota>{
        /**
     * DAO para acceso a datos de mascotas.
     * Inyectado en el constructor (Dependency Injection).
     */
    private final MascotaDAO mascotaDAO;

    /**
     * Servicio de microchips para coordinar operaciones transaccionales.
     * IMPORTANTE: MascotaServiceImpl necesita MicrochipService porque:
     * - Una mascota puede crear/actualizar su microchip al insertarse/actualizarse
     * - El servicio coordina la secuencia: insertar microchip → insertar mascota
     * - Implementa eliminación segura: actualizar FK mascota → eliminar microchip
     */
    private final MicrochipServiceImpl microchipServiceImpl;

    /**
     * Constructor con inyección de dependencias.
     * Valida que ambas dependencias no sean null (fail-fast).
     *
     * @param mascotaDAO DAO de personas (normalmente MascotaDAO)
     * @param microchipServiceImpl Servicio de microchips para operaciones coordinadas
     * @throws IllegalArgumentException si alguna dependencia es null
     */
    public MascotaServiceImpl(MascotaDAO mascotaDAO, MicrochipServiceImpl microchipServiceImpl) {
        if (mascotaDAO == null) {
            throw new IllegalArgumentException("MascotaDAO no puede ser null");
        }
        if (microchipServiceImpl == null) {
            throw new IllegalArgumentException("MicrochipServiceImpl no puede ser null");
        }
        this.mascotaDAO = mascotaDAO;
        this.microchipServiceImpl = microchipServiceImpl;
    }
    
    /**
     * Inserta una nueva mascota en la base de datos.
     *
     * Flujo transaccional complejo:
     * 1. Valida que los datos de la mascota sean correctos (nombre, especie, raza, fecha_nacimiento, duenio)
     * 3. Si la mascota tiene microchip asociado:
     *    a. Si microchip.id == 0 → Es nuevo, lo inserta en la BD
     *    b. Si microchip.id > 0 → Ya existe, lo actualiza
     * 4. Inserta la mascota con la FK microchip_id correcta
     *
     * IMPORTANTE: La coordinación con MicrochipService permite que el microchip
     * obtenga su ID autogenerado ANTES de insertar la mascota (necesario para la FK).
     *
     * @param mascota Mascota a insertar (id será ignorado y regenerado)
     * @throws Exception Si la validación falla
     */
    @Override
    public void insertar(Mascota mascota) throws Exception {
        validateMascota(mascota);        

        // Coordinación con MicrochipService (transaccional)
        if (mascota.getMicrochip() != null) {
            if (mascota.getMicrochip().getId() == 0) {
                // Microchip nuevo: insertar primero para obtener ID autogenerado
                microchipServiceImpl.insertar(mascota.getMicrochip());
            } else {
                // Microchip existente: actualizar datos
                microchipServiceImpl.actualizar(mascota.getMicrochip());
            }
        }

        mascotaDAO.insertar(mascota);
    }
    /**
     * Actualiza una mascota existente en la base de datos.
     *
     * Validaciones:
     * - La mascota debe tener datos válidos (nombre, especie, raza, fecha_nacimiento, duenio)
     * - El ID debe ser > 0 (debe ser una mascota ya persistida)
     *
     * IMPORTANTE: Esta operación NO coordina con MicrochipService.
     * Para cambiar el microchip de una mascota, usar MenuHandler que:
     * - Asignar nuevo microchip: opción 6 (crea nuevo) o 7 (usa existente)
     * - Actualizar microchip: opción 9 (modifica microchip actual)
     *
     * @param persona Persona con los datos actualizados
     * @throws Exception Si la validación falla o la mascota no existe
     */
    @Override
    public void actualizar(Mascota mascota) throws Exception {
        validateMascota(mascota);
        if (mascota.getId() <= 0) {
            throw new IllegalArgumentException("El ID de la mascota debe ser mayor a 0 para actualizar");
        }      
        mascotaDAO.actualizar(mascota);
    }
    
    /**
     * Elimina lógicamente una mascota (soft delete).
     * Marca la mascota como eliminado=TRUE sin borrarla físicamente.
     *
     * ⚠️ IMPORTANTE: Este método NO elimina el microchip asociado.
     * Si la mascota tiene un microchip, este quedará activo en la BD.    
     *
     * @param id ID de la mascota a eliminar
     * @throws Exception Si id <= 0 o no existe la mascota
     */
    @Override
    public void eliminar(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        mascotaDAO.eliminar(id);
    }
    
    /**
     * Obtiene una mascota por su ID.
     * Incluye el microchip asociado mediante LEFT JOIN (MascotaDAO).
     *
     * @param id ID de la mascota a buscar
     * @return Mascota encontrada (con su microchip si tiene), o null si no existe o está eliminada
     * @throws Exception Si id <= 0 o hay error de BD
     */
    @Override
    public Mascota getById(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        return mascotaDAO.getById(id);
    }
    
    /**
     * Obtiene todas las mascotas activas (eliminado=FALSE).
     * Incluye sus microchips mediante LEFT JOIN (MascotaDAO).
     *
     * @return Lista de mascotas activas con sus microchips (puede estar vacía)
     * @throws Exception Si hay error de BD
     */
    @Override
    public List<Mascota> getAll() throws Exception {
        return mascotaDAO.getAll();
    }
    
    /**
     * Expone el servicio de microchips para que MenuHandler pueda usarlo.
     * Necesario para operaciones de menú que trabajan directamente con microchips.
     *
     * @return Instancia de MicrochipServiceImpl inyectada en este servicio
     */
    public MicrochipServiceImpl getMicrochipService() {
        return this.microchipServiceImpl;
    }
    
    /**
     * Busca mascotas por nombre o duenio (búsqueda flexible con LIKE).
     * Usa MascotaDAO.buscarPorNombreDuenio() que realiza:
     * - LIKE %filtro% en nombre O duenio
     * - Insensible a mayúsculas/minúsculas (LOWER())
     * - Solo mascotas activas (eliminado=FALSE)
     *
     * Uso típico: El usuario ingresa "juan" y encuentra "Juan Pérez", "María Juana", etc.
     *
     * @param filtro Texto a buscar (no puede estar vacío)
     * @return Lista de mascotas que coinciden con el filtro (puede estar vacía)
     * @throws IllegalArgumentException Si el filtro está vacío
     * @throws Exception Si hay error de BD
     */
    public List<Mascota> buscarPorNombreDuenio(String filtro) throws Exception {
        if (filtro == null || filtro.trim().isEmpty()) {
            throw new IllegalArgumentException("El filtro de búsqueda no puede estar vacío");
        }
        return mascotaDAO.buscarPorNombreDuenio(filtro);
    }

    /**
     * Elimina un microchip de forma SEGURA actualizando primero la FK de la mascota.
     * Este es el método RECOMENDADO para eliminar microchips.
     *
     * Flujo transaccional SEGURO:
     * 1. Obtiene la mascota por ID y valida que exista
     * 2. Verifica que el microchip pertenezca a esa mascota (evita eliminar microchip ajeno)
     * 3. Desasocia el microchip de la mascota (mascota.microchip = null)
     * 4. Actualiza la mascota en BD (microchip_id = NULL)
     * 5. Elimina el microchip (ahora no hay FKs apuntando a él)
     *
     * DIFERENCIA con MicrochipService.eliminar():
     * - MicrochipService.eliminar(): Elimina directamente (PELIGROSO, puede dejar FKs huérfanas)
     * - Este método: Primero actualiza FK, luego elimina (SEGURO)
     *
     * Usado en MenuHandler opción 10: "Eliminar microchip de una mascota"
     *
     * @param mascotaId ID de la mascota dueña del microchip
     * @param microchipId ID del microchip a eliminar
     * @throws IllegalArgumentException Si los IDs son <= 0, la mascota no existe, o el microchip no pertenece a la persona
     * @throws Exception Si hay error de BD
     */
    public void eliminarMicrochipDeMascota(int mascotaId, int microchipId) throws Exception {
        if (mascotaId <= 0 || microchipId <= 0) {
            throw new IllegalArgumentException("Los IDs deben ser mayores a 0");
        }

        Mascota mascota = mascotaDAO.getById(mascotaId);
        if (mascota == null) {
            throw new IllegalArgumentException("Mascota no encontrada con ID: " + mascotaId);
        }

        if (mascota.getMicrochip()== null || mascota.getMicrochip().getId() != microchipId) {
            throw new IllegalArgumentException("El microchip no pertenece a esta mascota");
        }

        // Secuencia transaccional: actualizar FK → eliminar domicilio
        mascota.setMicrochip(null);
        mascotaDAO.actualizar(mascota);
        microchipServiceImpl.eliminar(microchipId);
    }
    
    /**
     * Valida que una mascota tenga datos correctos.
     *
     * Reglas de negocio aplicadas:
     *
     * @param mascota Mascota a validar
     * @throws IllegalArgumentException Si alguna validación falla
     */
    private void validateMascota(Mascota mascota) {
        if (mascota == null) {
            throw new IllegalArgumentException("La mascota no puede ser null");
        }
        if (mascota.getNombre() == null || mascota.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (mascota.getEspecie()== null || mascota.getEspecie().trim().isEmpty()) {
            throw new IllegalArgumentException("La especie no puede estar vacía");
        }
        if (mascota.getDuenio()== null || mascota.getDuenio().trim().isEmpty()) {
            throw new IllegalArgumentException("El duenio no puede estar vacío");
        }        
    }    
}
