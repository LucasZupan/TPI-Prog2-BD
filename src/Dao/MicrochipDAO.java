package Dao;

import Config.DatabaseConnection;
import Models.Microchip;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad Microchip.
 * Gestiona todas las operaciones de persistencia de microchips en la base de datos.
 *
 * Características:
 * - Implementa GenericDAO<Microchip> para operaciones CRUD estándar
 * - Usa PreparedStatements en TODAS las consultas (protección contra SQL injection)
 * - Implementa soft delete (eliminado=TRUE, no DELETE físico)
 * - NO maneja relaciones (Microchip es entidad independiente)
 * - Soporta transacciones mediante insertTx() (recibe Connection externa)
 *
 * Diferencias con MascotaDAO:
 * - Más simple: NO tiene LEFT JOINs (Microchip no tiene relaciones cargadas)
 * - NO tiene búsquedas especializadas (solo CRUD básico)
 * - Todas las queries filtran por eliminado=FALSE (soft delete)
 *
 * Patrón: DAO con try-with-resources para manejo automático de recursos JDBC
 */
public class MicrochipDAO implements IMicrochipDAO {
    
    /**
     * Query de inserción de Microchip.
     * Inserta codigo, fecha_implantacion, veterinaria, observaciones.
     * El id es AUTO_INCREMENT y se obtiene con RETURN_GENERATED_KEYS.
     * El campo eliminado tiene DEFAULT FALSE en la BD.
     */
    private static final String INSERT_SQL = "INSERT INTO microchips (codigo, fecha_implantacion, veterinaria, observaciones) VALUES (?, ?, ?, ?)";
    
    /**
     * Query de actualización de microchip.
     * Actualiza codigo, fecha_implantacion, veterinaria y observaciones por id.
     * NO actualiza el flag eliminado (solo se modifica en soft delete).
     */
    private static final String UPDATE_SQL = "UPDATE microchips SET codigo= ?, fecha_implantacion= ?, veterinaria= ?, observaciones= ? WHERE id = ?";
 
    /**
     * Query de soft delete.
     * Marca eliminado=TRUE sin borrar físicamente la fila.
     * Preserva integridad referencial y datos históricos.
     *
     * ⚠️ PELIGRO: Este método NO verifica si hay mascotas asociadas.
     * Puede dejar FKs huérfanas (mascotas.microchip_id apuntando a microchip eliminado).
     * ALTERNATIVA SEGURA: MascotaServiceImpl.eliminarMicrochipDeMascota()
     */
    private static final String DELETE_SQL = "UPDATE microchips SET eliminado = TRUE WHERE id = ?";    
    
    /**
     * Query para obtener microchip por ID.
     * Solo retorna microchips activos (eliminado=FALSE).
     * SELECT * es aceptable aquí porque Microchip tiene solo 6 columnas.
     */
    private static final String SELECT_BY_ID_SQL = "SELECT * FROM microchips WHERE id = ? AND eliminado = FALSE";

    /**
     * Query para obtener todos los microchips activos.
     * Filtra por eliminado=FALSE (solo microchips activos).
     * SELECT * es aceptable aquí porque Microchip tiene solo 6 columnas.
     */
    private static final String SELECT_ALL_SQL = "SELECT * FROM microchips WHERE eliminado = FALSE";
   
    /**
     * Query de búsqueda exacta por codigo.
     * Usa comparación exacta (=) porque el codigo es único.
     * Usado por MicrochiperviceImpl.validateCodigoUnique() para verificar unicidad.
     * Solo microchips activos (eliminado=FALSE).
     */
    private static final String SEARCH_BY_CODIGO_SQL = "SELECT c.id, c.codigo, c.fecha_implantacion, c.veterinaria, c.observaciones " +            
            "FROM microchips c " +
            "WHERE c.eliminado = FALSE AND c.codigo = ?";
    /**
     * Inserta un microchip en la base de datos (versión sin transacción).
     * Crea su propia conexión y la cierra automáticamente.
     *
     * Flujo:
     * 1. Abre conexión con DatabaseConnection.getConnection()
     * 2. Crea PreparedStatement con INSERT_SQL y RETURN_GENERATED_KEYS
     * 3. Setea parámetros (codigo, fecha_implantacion, veterinaria, observaciones)
     * 4. Ejecuta INSERT
     * 5. Obtiene el ID autogenerado y lo asigna a microchip.id
     * 6. Cierra recursos automáticamente (try-with-resources)
     *
     * IMPORTANTE: El ID generado se asigna al objeto microchip.
     * Esto permite que MascotaServiceImpl.insertar() use microchip.getId()
     * inmediatamente después de insertar.
     *
     * @param microchip Microchip a insertar (id será ignorado y regenerado)
     * @throws SQLException Si falla la inserción o no se obtiene ID generado
     */
    @Override
    public void insertar(Microchip microchip) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            setMicrochipParameters(stmt, microchip);
            stmt.executeUpdate();

            setGeneratedId(stmt, microchip);
        }
    }

    /**
     * Inserta un microchip dentro de una transacción existente.
     * NO crea nueva conexión, recibe una Connection externa.
     * NO cierra la conexión (responsabilidad del caller con TransactionManager).
     *
     * Usado por: Prueba para chequear rollback
     * - Operaciones que requieren múltiples inserts coordinados
     * - Rollback automático si alguna operación falla
     *
     * @param microchip Microchip a insertar
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @throws Exception Si falla la inserción
     */
    @Override
    public void insertTx(Microchip microchip, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            setMicrochipParameters(stmt, microchip);
            stmt.executeUpdate();
            setGeneratedId(stmt, microchip); 
        }
    }
    
    /**
     * Actualiza un microchip existente en la base de datos.
     * Actualiza codigo, fecha_implantacion, veterinaria, observaciones
     *
     * Validaciones:
     * - Si rowsAffected == 0 → El microchip no existe o ya está eliminado
     *
     * @param microchip Microchip con los datos actualizados (id debe ser > 0)
     * @throws SQLException Si el microchip no existe o hay error de BD
     */
    @Override
    public void actualizar(Microchip microchip) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setString(1, microchip.getCodigo());
            stmt.setObject(2, microchip.getFechaImplantacion() != null ? 
                    java.sql.Date.valueOf(microchip.getFechaImplantacion()) : null, java.sql.Types.DATE);
            stmt.setString(3, microchip.getVeterinaria());
            stmt.setString(4, microchip.getObservaciones());
            stmt.setInt(5, microchip.getId());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar el microchip con ID: " + microchip.getId());
            }
        }
    }

    /**
     * Elimina lógicamente un microchip (soft delete).
     * Marca eliminado=TRUE sin borrar físicamente la fila.
     *
     * Validaciones:
     * - Si rowsAffected == 0 → El microchip no existe o ya está eliminado
     *
     * ⚠️ PELIGRO: Este método NO verifica si hay mascotas asociadas.
     * Si hay mascotas con mascota.microchip_id apuntando a este microchip,
     * quedarán con FK huérfana (apuntando a un microchip eliminado).
     *
     * Esto puede causar:
     * - Datos inconsistentes (mascota asociada a microchip "eliminado")
     * - Errores en LEFT JOINs que esperan microchips activos
     *
     * ALTERNATIVA SEGURA: MascotaServiceImpl.eliminarMicrochipDeMascota()
     * - Primero actualiza mascota.microchip_id = NULL
     * - Luego elimina el microchip
     * - Garantiza que no queden FKs huérfanas
     *
     * Este método se mantiene para casos donde:
     * - Se está seguro de que el microchip NO tiene mascotas asociadas
     * - Se quiere eliminar microchips en lote (administración)
     *
     * @param id ID del microchip a eliminar
     * @throws SQLException Si el microchip no existe o hay error de BD
     */
    @Override
    public void eliminar(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("No se encontró microchip con ID: " + id);
            }
        }
    }

    /**
     * Obtiene un microchip por su ID.
     * Solo retorna microchips activos (eliminado=FALSE).
     *
     * @param id ID del Microchip a buscar
     * @return Microchip encontrado, o null si no existe o está eliminado
     * @throws SQLException Si hay error de BD
     */
    @Override
    public Microchip getById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMicrochip(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Obtiene todos los microchips activos (eliminado=FALSE).
     *
     * Nota: Usa Statement (no PreparedStatement) porque no hay parámetros.
     *
     * Uso típico:
     * - MenuHandler opción 7: Listar microchips existentes para asignar a mascota
     *
     * @return Lista de microchips activos (puede estar vacía)
     * @throws SQLException Si hay error de BD
     */
    @Override
    public List<Microchip> getAll() throws SQLException {
        List<Microchip> microchips = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_SQL)) {

            while (rs.next()) {
                microchips.add(mapResultSetToMicrochip(rs));
            }
        }

        return microchips;
    }

    /**
     * Setea los parámetros de microchip en un PreparedStatement.
     * Método auxiliar usado por insertar() e insertTx().
     *
     * Parámetros seteados:
     * 1. codigo (String)
     * 2. fecha_implementacion (Date)
     * 3. veterinaria (String)
     * 4. observaciones (String
     *
     * @param stmt PreparedStatement con INSERT_SQL
     * @param microchip Microchip con los datos a insertar
     * @throws SQLException Si hay error al setear parámetros
     */
    private void setMicrochipParameters(PreparedStatement stmt, Microchip microchip) throws SQLException {
            stmt.setString(1, microchip.getCodigo());
            stmt.setObject(2,microchip.getFechaImplantacion() != null ? 
                    java.sql.Date.valueOf(microchip.getFechaImplantacion()) : null, java.sql.Types.DATE);
            stmt.setString(3, microchip.getVeterinaria());
            stmt.setString(4, microchip.getObservaciones());
    }

    /**
     * Obtiene el ID autogenerado por la BD después de un INSERT.
     * Asigna el ID generado al objeto microchip.
     *
     * IMPORTANTE: Este método es crítico para mantener la consistencia:
     * - Después de insertar, el objeto microchip debe tener su ID real de la BD
     * - MascotaServiceImpl.insertar() depende de esto para setear la FK:
     *   1. microchipService.insertar(microchip) → microchip.id se setea aquí
     *   2. mascotaDAO.insertar(mascota) → usa mascota.getMicrochip().getId() para la FK
     * - Necesario para operaciones transaccionales que requieren el ID generado
     *
     * @param stmt PreparedStatement que ejecutó el INSERT con RETURN_GENERATED_KEYS
     * @param microchip Objeto microchip a actualizar con el ID generado
     * @throws SQLException Si no se pudo obtener el ID generado (indica problema grave)
     */
    private void setGeneratedId(PreparedStatement stmt, Microchip microchip) throws SQLException {
        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                microchip.setId(generatedKeys.getInt(1));
            } else {
                throw new SQLException("La inserción del microchip falló, no se obtuvo ID generado");
            }
        }
    }
    /**
     * Mapea un ResultSet a un objeto Microchip.
     * Reconstruye el objeto usando el constructor completo.
     *
     * Mapeo de columnas:
     * - id → id
     * - codigo → codigo
     * - fecha_implementacion → fecha_implementacion
     * - veterinaria - veterinaria
     * - observaciones - observaciones
     *
     * Nota: El campo eliminado NO se mapea porque las queries filtran por eliminado=FALSE,
     * garantizando que solo se retornan microchips activos.
     *
     * @param rs ResultSet posicionado en una fila con datos de microchip
     * @return Microchip reconstruido
     * @throws SQLException Si hay error al leer columnas del ResultSet
     */
    private Microchip mapResultSetToMicrochip(ResultSet rs) throws SQLException {
        java.sql.Date fi = rs.getDate("fecha_implantacion");
        return new Microchip(
            rs.getInt("id"),
            rs.getString("codigo"),
            fi != null ? fi.toLocalDate() : null,
            rs.getString("veterinaria"),
            rs.getString("observaciones")
        );
    } 
    
    
    public Microchip buscarPorCodigo(String codigo) throws SQLException {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new IllegalArgumentException("El codigo no puede estar vacío");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SEARCH_BY_CODIGO_SQL)) {

            stmt.setString(1, codigo.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMicrochip(rs);
                }
            }
        }
        return null;
    }
}
