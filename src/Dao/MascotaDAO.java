package Dao;

import Config.DatabaseConnection;
import Models.Mascota;
import Models.Microchip;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad Mascota.
 * Gestiona todas las operaciones de persistencia de mascotas en la base de datos.
 *
 * Características:
 * - Implementa GenericDAO<Mascota> para operaciones CRUD estándar
 * - Usa PreparedStatements en TODAS las consultas (protección contra SQL injection)
 * - Maneja LEFT JOIN con microchips para cargar la relación de forma eager
 * - Implementa soft delete (eliminado=TRUE, no DELETE físico)
 * - Proporciona búsquedas especializadas
 * - Soporta transacciones mediante insertTx() (recibe Connection externa)
 *
 * Patrón: DAO con try-with-resources para manejo automático de recursos JDBC
 */
public class MascotaDAO implements GenericDAO<Mascota> {
    /**
     * Query de inserción de mascota.
     * Inserta nombre, apellido, dni y FK domicilio_id.
     * El id es AUTO_INCREMENT y se obtiene con RETURN_GENERATED_KEYS.
     */
    private static final String INSERT_SQL = "INSERT INTO mascotas (nombre, especie, raza, fecha_nacimiento, duenio, microchip_id) VALUES (?, ?, ?, ?, ?, ?)";
 
    /**
     * Query de actualización de mascota.
     * Actualiza nombre, nombre, especie, raza, fecha de nacimiento, duenio y FK microchip_id por id.
     * NO actualiza el flag eliminado (solo se modifica en soft delete).
     */
    private static final String UPDATE_SQL = "UPDATE mascotas SET nombre = ?, especie = ?, raza = ?, fecha_nacimiento = ?, duenio = ?, microchip_id = ? WHERE id = ?";

     /**
     * Query de soft delete.
     * Marca eliminado=TRUE sin borrar físicamente la fila.
     * Preserva integridad referencial y datos históricos.
     */
    private static final String DELETE_SQL = "UPDATE mascotas SET eliminado = TRUE WHERE id = ?";   
    
     /**
     * Query para obtener mascota por ID.
     * LEFT JOIN con microchips para cargar la relación de forma eager.
     * Solo retorna mascotas activas (eliminado=FALSE).
     *
     * Campos del ResultSet:
     * - Mascota: id, nombre, especie, raza, fecha_nacimiento, duenio, microchip_id
     * - Microchip (puede ser NULL): codigo, fecha_implantacion, veterinaria
     */
    private static final String SELECT_BY_ID_SQL = "SELECT m.id, m.nombre, m.especie, m.raza, m.fecha_nacimiento, m.duenio, m.microchip_id, " +
            "c.id AS mc_id, c.codigo, c.fecha_implantacion, c.veterinaria" +
            "FROM mascotas m LEFT JOIN microchips c ON m.microchip_id = c.id " +
            "WHERE m.id = ? AND m.eliminado = FALSE";   
    
    /**
     * Query para obtener todas las mascotas activas.
     * LEFT JOIN con microchip para cargar relaciones.
     * Filtra por eliminado=FALSE (solo mascotas activas).
     */
    private static final String SELECT_ALL_SQL = "SELECT m.id, m.nombre, m.especie, m.raza, m.fecha_nacimiento, m.duenio, m.microchip_id, " +
            "c.id AS mc_id, c.codigo, c.fecha_implantacion, c.veterinaria" +
            "FROM mascotas m LEFT JOIN microchips c ON m.microchip_id = c.id " +
            "WHERE m.eliminado = FALSE";
 
    /**
     * Query de búsqueda por nombre o duenio con LIKE.
     * Permite búsqueda flexible: el usuario ingresa "juan" y encuentra "Juan", "Juana", etc.
     * Usa % antes y después del filtro: LIKE '%filtro%'
     * Solo personas activas (eliminado=FALSE).
     */
    private static final String SEARCH_BY_NAME_SQL = "SELECT m.id, m.nombre, m.especie, m.raza, m.fecha_nacimiento, m.duenio, m.microchip_id, " +
            "c.id AS mc_id, c.codigo, c.fecha_implantacion, c.veterinaria" +
            "FROM mascotas m LEFT JOIN microchips c ON m.microchip_id = c.id " +
            "WHERE m.eliminado = FALSE AND (m.nombre LIKE ? OR m.duenio LIKE ?)";
  
    /**
     * DAO de microchips (actualmente no usado, pero disponible para operaciones futuras).
     * Inyectado en el constructor por si se necesita coordinar operaciones.
     */
    private final MicrochipDAO microchipDAO;

    /**
     * Constructor con inyección de MicrochipDAO.
     * Valida que la dependencia no sea null (fail-fast).
     *
     * @param microchipDAO DAO de microchips
     * @throws IllegalArgumentException si domicilioDAO es null
     */
    public MascotaDAO(MicrochipDAO microchipDAO) {
        if (microchipDAO == null) {
            throw new IllegalArgumentException("MicrochipDAO no puede ser null");
        }
        this.microchipDAO = microchipDAO;
    }
    
    /**
     * Inserta una mascota en la base de datos (versión sin transacción).
     * Crea su propia conexión y la cierra automáticamente.
     *
     * Flujo:
     * 1. Abre conexión con DatabaseConnection.getConnection()
     * 2. Crea PreparedStatement con INSERT_SQL y RETURN_GENERATED_KEYS
     * 3. Setea parámetros (nombre, especie, raza, fecha_nacimiento, duenio, microchip_id)
     * 4. Ejecuta INSERT
     * 5. Obtiene el ID autogenerado y lo asigna a mascota.id
     * 6. Cierra recursos automáticamente (try-with-resources)
     *
     * @param mascota Mascota a insertar (id será ignorado y regenerado)
     * @throws Exception Si falla la inserción o no se obtiene ID generado
     */
    @Override
    public void insertar(Mascota mascota) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            setMascotaParameters(stmt, mascota);
            stmt.executeUpdate();
            setGeneratedId(stmt, mascota);
        }
    }
    
    /**
     * Inserta una mascota dentro de una transacción existente.
     * NO crea nueva conexión, recibe una Connection externa.
     * NO cierra la conexión (responsabilidad del caller con TransactionManager).
     *
     * Usado por: (Actualmente no usado, pero disponible para transacciones futuras)
     * - Operaciones que requieren múltiples inserts coordinados
     * - Rollback automático si alguna operación falla
     *
     * @param mascota Mascota a insertar
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @throws Exception Si falla la inserción
     */
    @Override
    public void insertTx(Mascota mascota, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            setMascotaParameters(stmt, mascota);
            stmt.executeUpdate();
            setGeneratedId(stmt, mascota);
        }
    }
    
    /**
     * Actualiza una mascota existente en la base de datos.
     * Actualiza nombre, especie, raza, fecha_nacimiento, duenio y FK microchip_id.
     *
     * Validaciones:
     * - Si rowsAffected == 0 → La mascota no existe o ya está eliminada
     * @param mascota mascota con los datos actualizados (id debe ser > 0)
     * @throws SQLException Si la mascota no existe o hay error de BD
     */
    @Override
    public void actualizar(Mascota mascota) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setString(1, mascota.getNombre());
            stmt.setString(2, mascota.getEspecie());
            stmt.setString(3, mascota.getRaza());
            stmt.setDate(4, java.sql.Date.valueOf(mascota.getFechaNacimiento()));
            stmt.setString(5, mascota.getDuenio());
            setMicrochipId(stmt, 6, mascota.getMicrochip());
            stmt.setInt(7, mascota.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar la mascota con ID: " + mascota.getId());
            }
        }
    }
    
    /**
     * Elimina lógicamente una mascota (soft delete).
     * Marca eliminado=TRUE sin borrar físicamente la fila.
     *
     * Validaciones:
     * - Si rowsAffected == 0 → La mascota no existe o ya está eliminada
     *
     * IMPORTANTE: NO elimina el microchip asociado.
     * Múltiples personas pueden compartir un domicilio.
     *
     * @param id ID de la mascota a eliminar
     * @throws SQLException Si la persona no existe o hay error de BD
     */
    @Override
    public void eliminar(int id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("No se encontró mascota con ID: " + id);
            }
        }
    }
    
    /**
     * Obtiene una mascota por su ID.
     * Incluye su microchip asociado mediante LEFT JOIN.
     *
     * @param id ID de la mascota a buscar
     * @return Mascota encontrada con su microchip, o null si no existe o está eliminada
     * @throws Exception Si hay error de BD (captura SQLException y re-lanza con mensaje descriptivo)
     */
    @Override
    public Mascota getById(int id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMascota(rs);
                }
            }
        } catch (SQLException e) {
            throw new Exception("Error al obtener mascota por ID: " + e.getMessage(), e);
        }
        return null;
    }    
    
    /**
     * Obtiene todas las mascotas activas (eliminado=FALSE).
     * Incluye sus microchip mediante LEFT JOIN.
     *
     * Nota: Usa Statement (no PreparedStatement) porque no hay parámetros.
     *
     * @return Lista de mascotas activas con sus microchips (puede estar vacía)
     * @throws Exception Si hay error de BD
     */
    @Override
    public List<Mascota> getAll() throws Exception {
        List<Mascota> mascotas = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_SQL)) {

            while (rs.next()) {
                mascotas.add(mapResultSetToMascota(rs));
            }
        } catch (SQLException e) {
            throw new Exception("Error al obtener todas las personas: " + e.getMessage(), e);
        }
        return mascotas;
    }    
    
    /**
     * Busca mascotas por nombre o duenio con búsqueda flexible (LIKE).
     * Permite búsqueda parcial: "juan" encuentra "Juan", "María Juana", etc.
     *
     * Patrón de búsqueda: LIKE '%filtro%' en nombre O apellido
     * Búsqueda case-sensitive en MySQL (depende de la collation de la BD).
     *
     * Ejemplo:
     * - filtro = "garcia" → Encuentra mascotas con nombre o duenio que contengan "garcia"
     *
     * @param filtro Texto a buscar (no puede estar vacío)
     * @return Lista de mascotas que coinciden con el filtro (puede estar vacía)
     * @throws IllegalArgumentException Si el filtro está vacío
     * @throws SQLException Si hay error de BD
     */
    public List<Mascota> buscarPorNombreDuenio(String filtro) throws SQLException {
        if (filtro == null || filtro.trim().isEmpty()) {
            throw new IllegalArgumentException("El filtro de búsqueda no puede estar vacío");
        }

        List<Mascota> mascotas = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SEARCH_BY_NAME_SQL)) {

            // Construye el patrón LIKE: %filtro%
            String searchPattern = "%" + filtro + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    mascotas.add(mapResultSetToMascota(rs));
                }
            }
        }
        return mascotas;
    }
    
    /**
     * Setea los parámetros de persona en un PreparedStatement.
     * Método auxiliar usado por insertar() e insertTx().
     *
     * Parámetros seteados:
     * 1. nombre (String)
     * 2. especie (String)
     * 3. raza (String)
     * 4. fecha_nacimiento (Date)
     * 5. duenio (String)
     * 6. microchip_id (Integer o NULL)
     *
     * @param stmt PreparedStatement con INSERT_SQL
     * @param mascota Mascota con los datos a insertar
     * @throws SQLException Si hay error al setear parámetros
     */
    private void setMascotaParameters(PreparedStatement stmt, Mascota mascota) throws SQLException {
        stmt.setString(1, mascota.getNombre());
        stmt.setString(2, mascota.getEspecie());
        stmt.setString(3, mascota.getRaza());
        stmt.setDate(4, java.sql.Date.valueOf(mascota.getFechaNacimiento()));
        stmt.setString(5, mascota.getDuenio());
        setMicrochipId(stmt, 6, mascota.getMicrochip());       
    }    
    
     /**
     * Setea la FK microchip_id en un PreparedStatement.
     * Maneja correctamente el caso NULL (mascota sin microchip).
     *
     * Lógica:
     * - Si microchip != null Y microchip.id > 0 → Setea el ID
     * - Si microchip == null O microchip.id <= 0 → Setea NULL
     *
     * Importante: El tipo Types.INTEGER es necesario para setNull() en JDBC.
     *
     * @param stmt PreparedStatement
     * @param parameterIndex Índice del parámetro (1-based)
     * @param microchip Microchip asociado (puede ser null)
     * @throws SQLException Si hay error al setear el parámetro
     */
    private void setMicrochipId(PreparedStatement stmt, int parameterIndex, Microchip microchip) throws SQLException {
        if (microchip != null && microchip.getId() > 0) {
            stmt.setInt(parameterIndex, microchip.getId());
        } else {
            stmt.setNull(parameterIndex, Types.INTEGER);
        }
    }
   
    /**
     * Obtiene el ID autogenerado por la BD después de un INSERT.
     * Asigna el ID generado al objeto persona.
     *
     * IMPORTANTE: Este método es crítico para mantener la consistencia:
     * - Después de insertar, el objeto mascota debe tener su ID real de la BD
     * - Permite usar mascota.getId() inmediatamente después de insertar
     * - Necesario para operaciones transaccionales que requieren el ID generado
     *
     * @param stmt PreparedStatement que ejecutó el INSERT con RETURN_GENERATED_KEYS
     * @param mascota Objeto mascota a actualizar con el ID generado
     * @throws SQLException Si no se pudo obtener el ID generado (indica problema grave)
     */
    private void setGeneratedId(PreparedStatement stmt, Mascota mascota) throws SQLException {
        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                mascota.setId(generatedKeys.getInt(1));
            } else {
                throw new SQLException("La inserción de la mascota falló, no se obtuvo ID generado");
            }
        }
    }   
    
        /**
     * Mapea un ResultSet a un objeto Mascota.
     * Reconstruye la relación con Microchip usando LEFT JOIN.
     *
     * Mapeo de columnas:
     * Mascota:
     * - id → m.id
     * - nombre → m.nombre
     * - especie → m.especie
     * - raza → m.raza
     * - fecha_nacimiento → m.fecha_nacimiento
     * - duenio → m.duenio
     * 
     * Microchip (puede ser NULL si la mascota no tiene microchip):
     * - id → c.id AS mc_id
     * - codigo → c.codigo
     * - fecha_implantacion → c.fecha_implantacion
     * - veterinaria → c.veterinaria
     *
     * Lógica de NULL en LEFT JOIN:
     * - Si microchip_id es NULL → persona.microchip = null (correcto)
     * - Si microchip_id > 0 → Se crea objeto Microchip y se asigna a persona
     *
     * @param rs ResultSet posicionado en una fila con datos de mascota y microchip
     * @return Mascota reconstruida con su microchip (si tiene)
     * @throws SQLException Si hay error al leer columnas del ResultSet
     */
    private Mascota mapResultSetToMascota(ResultSet rs) throws SQLException {
        Mascota mascota = new Mascota();
        mascota.setId(rs.getInt("id"));
        mascota.setNombre(rs.getString("nombre"));
        mascota.setEspecie(rs.getString("especie"));
        mascota.setRaza(rs.getString("raza"));
        mascota.setFechaNacimiento(rs.getDate("fecha_nacimiento").toLocalDate());

        // Manejo correcto de LEFT JOIN: verificar si domicilio_id es NULL
        int microchipId = rs.getInt("microchip_id");
        if (microchipId > 0 && !rs.wasNull()) {
            Microchip microchip = new Microchip();
            microchip.setId(rs.getInt("mc_id"));
            microchip.setCodigo(rs.getString("codigo"));
            microchip.setFechaImplantacion(rs.getDate("fecha_implantacion").toLocalDate());
            microchip.setVeterinaria("veterinaria");
            mascota.setMicrochip(microchip);
        }

        return mascota;
    }
}
