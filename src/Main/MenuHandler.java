package Main;

import Models.Mascota;
import Models.Microchip;
import Service.MascotaServiceImpl;
import java.util.List;
import java.util.Scanner;

/**
 * Controlador de las operaciones del menú (Menu Handler).
 * Gestiona toda la lógica de interacción con el usuario para operaciones CRUD.
 *
 * Responsabilidades:
 * - Capturar entrada del usuario desde consola (Scanner)
 * - Validar entrada básica (conversión de tipos, valores vacíos)
 * - Invocar servicios de negocio (MascotaService, MicrochipService)
 * - Mostrar resultados y mensajes de error al usuario
 * - Coordinar operaciones complejas (crear mascota con microchip, etc.)
 *
 * Patrón: Controller (MVC) - capa de presentación en arquitectura de 4 capas
 * Arquitectura: Main → Service → DAO → Models
 *
 * IMPORTANTE: Este handler NO contiene lógica de negocio.
 * Todas las validaciones de negocio están en la capa Service.
 */
public class MenuHandler {
    /**
     * Scanner compartido para leer entrada del usuario.
     * Inyectado desde AppMenu para evitar múltiples Scanners de System.in.
     */
    private final Scanner scanner;

    /**
     * Servicio de mascotas para operaciones CRUD.
     * También proporciona acceso a MicrochipService mediante getMicrochipService().
     */
    private final MascotaServiceImpl mascotaService;

    /**
     * Constructor con inyección de dependencias.
     * Valida que las dependencias no sean null (fail-fast).
     *
     * @param scanner Scanner compartido para entrada de usuario
     * @param mascotaService Servicio de mascotas
     * @throws IllegalArgumentException si alguna dependencia es null
     */
    public MenuHandler(Scanner scanner, MascotaServiceImpl mascotaService) {
        if (scanner == null) {
            throw new IllegalArgumentException("Scanner no puede ser null");
        }
        if (mascotaService == null) {
            throw new IllegalArgumentException("MascotaService no puede ser null");
        }
        this.scanner = scanner;
        this.mascotaService = mascotaService;
    }

    /**
     * Opción 1: Crear nueva mascota (con microchip opcional).
     *
     * Flujo:
     * 1. Solicita nombre, especie, raza, fecha_nacimiento, duenio
     * 2. Pregunta si desea agregar microchip
     * 3. Si sí, captura codigo, fecha_implantacion, veterinaria, observaciones
     * 4. Crea objeto Mascota y opcionalmente Microchip
     * 5. Invoca mascotaService.insertar() que:
     *    - Valida datos      *    
     *    - Si hay microchip, lo inserta primero (obtiene ID)
     *    - Inserta mascota con FK microchip_id correcta
     *
     * Input trimming: Aplica .trim() a todas las entradas (patrón consistente).
     *
     * Manejo de errores:
     * - IllegalArgumentException: Validaciones de negocio (muestra mensaje al usuario)
     * - SQLException: Errores de BD (muestra mensaje al usuario)
     * - Todos los errores se capturan y muestran, NO se propagan al menú principal
     */
    public void crearMascota() {
        try {
        String nombre  = pedirObligatorio("Nombre: ");
        String especie = pedirObligatorio("Especie: ");
        System.out.print("Raza: ");
        String raza = scanner.nextLine().trim();
        if (raza.isEmpty()) raza = null;
        System.out.print("Fecha de nacimiento (dd/MM/yyyy, vacío para omitir): ");
        String fn = scanner.nextLine().trim();
        java.time.LocalDate fechaNacimiento = null;
        if (!fn.isEmpty()) {
            try {
                fechaNacimiento = java.time.LocalDate.parse(
                    fn, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                );
            } catch (java.time.format.DateTimeParseException e) {
                System.out.println("Fecha inválida. Se guardará sin fecha de nacimiento.");
            }
        }
         String duenio  = pedirObligatorio("Duenio: ");

            Microchip microchip = null;
            System.out.print("¿Desea agregar un microchip? (s/n): ");
            if (scanner.nextLine().equalsIgnoreCase("s")) {
                microchip = crearMicrochip();
            }

            Mascota mascota = new Mascota(0, nombre, especie, raza, fechaNacimiento, duenio, microchip);
            mascota.setMicrochip(microchip);
            mascotaService.insertar(mascota);
            System.out.println("Mascota creada exitosamente con ID: " + mascota.getId());
        } catch (Exception e) {
            System.err.println("Error al crear mascota: " + e.getMessage());
        }
    }

    /**
     * Opción 2: Listar mascotas (todas o filtradas por nombre/duenio).
     *
     * Submenú:
     * 1. Listar todas las mascotas activas (getAll)
     * 2. Buscar por nombre o duenio con LIKE (buscarPorNombreDuenio)
     *
     * Muestra:
     * - ID, Nombre, Especie, Raza, fechaNacimiento, Duenio
     * - Microchip (si tiene): codigo, fechaImplantacion, veterinaria
     *
     * Manejo de casos especiales:
     * - Si no hay mascotas: Muestra "No se encontraron mascotas"
     * - Si la mascota no tiene microchip: Solo muestra datos de mascota
     *
     * Búsqueda por nombre/duenio:
     * - Usa MascotaDAO.buscarPorNombreDuenio() que hace LIKE '%filtro%'
     * - Insensible a mayúsculas en MySQL (depende de collation)
     * - Busca en nombre O duenio
     */
    public void listarMascotas() {
        try {
            System.out.print("¿Desea (1) listar todos o (2) buscar por nombre/duenio? Ingrese opcion: ");
            int subopcion = Integer.parseInt(scanner.nextLine());

            List<Mascota> mascotas;
            if (subopcion == 1) {
                mascotas = mascotaService.getAll();
            } else if (subopcion == 2) {
                System.out.print("Ingrese texto a buscar: ");
                String filtro = scanner.nextLine().trim();
                mascotas = mascotaService.buscarPorNombreDuenio(filtro);
            } else {
                System.out.println("Opcion invalida.");
                return;
            }

            if (mascotas.isEmpty()) {
                System.out.println("No se encontraron mascotas.");
                return;
            }

            for (Mascota m : mascotas) {
                System.out.println("ID: " + m.getId() + ", Nombre: " + m.getNombre() +
                        ", Especie: " + m.getEspecie() + ", Raza: " + m.getRaza() +
                        ", Fecha de Nacimiento: " + m.getFechaNacimiento() + ", Duenio: " + m.getDuenio());
                if (m.getMicrochip() != null) {
                    System.out.println("   Microchip: " + m.getMicrochip().getCodigo() +
                            ", Veterinaria: " + m.getMicrochip().getVeterinaria() +
                            ", Fecha de Implantacion: " + m.getMicrochip().getFechaImplantacion());
                }
            }
        } catch (Exception e) {
            System.err.println("Error al listar mascotas: " + e.getMessage());
        }
    }

    /**
     * Opción 3: Actualizar mascota existente.
     *
     * Flujo:
     * 1. Solicita ID de la mascota
     * 2. Obtiene mascota actual de la BD
     * 3. Muestra valores actuales y permite actualizar:
     *    - Nombre (Enter para mantener actual)
     *    - Especie (Enter para mantener actual)
     *    - Raza (Enter para mantener actual)
     *    - Fecha de Nacimiento (Enter para mantener actual)
     *    - Duenio (Enter para mantener actual) 
     * 4. Llama a actualizarMicrochipDeMascota() para manejar cambios en microchip
     * 5. Invoca mascotaService.actualizar() que valida:
     *    - Datos obligatorios (nombre, especie, duenio)
     *
     * Patrón "Enter para mantener":
     * - Lee input con scanner.nextLine().trim()
     * - Si isEmpty() → NO actualiza el campo (mantiene valor actual)
     * - Si tiene valor → Actualiza el campo
     *
     * IMPORTANTE: Esta operación NO actualiza el microchip directamente.
     * El microchip se maneja en actualizarMicrochipDeMascota() que puede:
     * - Actualizar microchip existente
     * - Agregar nuevo microchip si la mascota no tenía
     * - Dejar microchip sin cambios
     */
    public void actualizarMascota() {
        try {
            System.out.print("ID de la mascota a actualizar: ");
            int id = Integer.parseInt(scanner.nextLine());
            Mascota m = mascotaService.getById(id);

            if (m == null) {
                System.out.println("Mascota no encontrada.");
                return;
            }

            System.out.print("Nuevo nombre (actual: " + m.getNombre() + ", Enter para mantener): ");
            String nombre = scanner.nextLine().trim();
            if (!nombre.isEmpty()) {
                m.setNombre(nombre);
            }
            
            System.out.print("Nueva especie (actual: " + m.getEspecie()+ ", Enter para mantener): ");
            String especie = scanner.nextLine().trim();
            if (!especie.isEmpty()) {
                m.setEspecie(especie);
            }
            
            System.out.print("Nueva raza (actual: " + m.getRaza()+ ", Enter para mantener): ");
            String raza = scanner.nextLine().trim();
            if (!raza.isEmpty()) {
                m.setRaza(raza);
            }

            System.out.print("Nueva fecha de nacimiento (actual: " + m.getFechaNacimiento() + ", formato dd/MM/yyyy, Enter para mantener): ");
            String fechaStr = scanner.nextLine().trim();

            if (!fechaStr.isEmpty()) {
                try {
                    java.time.LocalDate nuevaFecha = java.time.LocalDate.parse(
                        fechaStr, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    );
                    m.setFechaNacimiento(nuevaFecha);
                } catch (java.time.format.DateTimeParseException e) {
                    System.out.println("Fecha inválida. Se mantiene la anterior.");
                }
            }
            
            System.out.print("Nuevo duenio (actual: " + m.getDuenio()+ ", Enter para mantener): ");
            String duenio = scanner.nextLine().trim();
            if (!duenio.isEmpty()) {
                m.setDuenio(duenio);
            }  

            actualizarMicrochipDeMascota(m);
            mascotaService.actualizar(m);
            
            System.out.println("Mascota actualizada exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al actualizar mascota: " + e.getMessage());
        }
    }

    /**
     * Opción 4: Eliminar mascota (soft delete).
     *
     * Flujo:
     * 1. Solicita ID de la mascota
     * 2. Invoca mascotaService.eliminar() que:
     *    - Marca mascota.eliminado = TRUE
     *    - NO elimina el microchip asociado
     *
     * Si se quiere eliminar también el microchip:
     * - Usar opción 10: "Eliminar microchip de una mascota" (eliminarMicrochipPorMascota)
     * - Esa opción primero desasocia el microchip, luego lo elimina (seguro)
     */
    public void eliminarMascota() {
        try {
            System.out.print("ID de la mascota a eliminar: ");
            int id = Integer.parseInt(scanner.nextLine());
            mascotaService.eliminar(id);
            System.out.println("Mascota eliminada exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al eliminar mascota: " + e.getMessage());
        }
    }

    /**
     * Opción 5: Crear microchip independiente (sin asociar a mascota).
     *
     * Flujo:
     * 1. Llama a crearMicrochip() para capturar codigo
     * 2. Invoca microchipervice.insertar() que:
     *    - Valida codigo obligatorio
     *    - Inserta en BD y asigna ID autogenerado
     * 3. Muestra ID generado
     *
     * Uso típico:
     * - Crear microchip que luego se asignará a una mascota (opción 9)
     * - Pre-cargar microchipz en la BD
     */
    public void crearMicrochipIndependiente() {
        try {
            Microchip microchip = crearMicrochip();
            mascotaService.getMicrochipService().insertar(microchip);
            System.out.println("Microchip creado exitosamente con ID: " + microchip.getId());
        } catch (Exception e) {
            System.err.println("Error al crear microchip: " + e.getMessage());
        }
    }

    /**
     * Opción 6: Listar todos los microchips activos.
     *
     * Muestra: codigo, fecha_implantacion, veterinaria
     *
     * Uso típico:
     * - Ver microchips disponibles antes de asignar a mascota (opción 7)
     * - Consultar ID de microchip para actualizar (opción 9) o eliminar (opción 8)
     *
     * Nota: Solo muestra microchips con eliminado=FALSE (soft delete).
     */
    public void listarMicrochips() {
        try {
            List<Microchip> microchips = mascotaService.getMicrochipService().getAll();
            if (microchips.isEmpty()) {
                System.out.println("No se encontraron microchips.");
                return;
            }
            for (Microchip m : microchips) {
                System.out.println("ID: " + m.getId() + ", codigo:" + m.getCodigo() +
                        ", fecha de implantacion: " + m.getFechaImplantacion() + 
                        ", veterinaria: " + m.getVeterinaria());
            }
        } catch (Exception e) {
            System.err.println("Error al listar microchips: " + e.getMessage());
        }
    }

    /**
     * Opción 7: Actualizar microchip por ID.
     *
     * Flujo:
     * 1. Solicita ID del microchip
     * 2. Obtiene microchip actual de la BD
     * 3. Muestra valores actuales y permite actualizar:
     *    - Codigo (Enter para mantener actual)
     *    - Fecha Implantacion (Enter para mantener actual)
     *    - Veterinaria (Enter para mantener actual)
     *    - Observaciones (Enter para mantener actual)
     * 4. Invoca microchipService.actualizar()     
     */
    public void actualizarMicrochipPorId() {
        try {
            System.out.print("ID del microchip a actualizar: ");
            int id = Integer.parseInt(scanner.nextLine());
            Microchip m = mascotaService.getMicrochipService().getById(id);

            if (m == null) {
                System.out.println("Microchip no encontrado.");
                return;
            }
            System.out.print("Nuevo codigo (actual: " + m.getCodigo()+ ", Enter para mantener): ");
            String codigo = scanner.nextLine().trim();
            if (!codigo.isEmpty()) {
                m.setCodigo(codigo);
            }    
            System.out.print("Nueva fecha de implantacion (actual: " + m.getFechaImplantacion()+ ", formato dd/MM/yyyy, Enter para mantener): ");
            String fechaStr = scanner.nextLine().trim();
            if (!fechaStr.isEmpty()) {
                try {
                    java.time.LocalDate nuevaFecha = java.time.LocalDate.parse(
                        fechaStr, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    );
                    m.setFechaImplantacion(nuevaFecha);
                } catch (java.time.format.DateTimeParseException e) {
                    System.out.println("Fecha inválida. Se mantiene la anterior.");
                }
            }
            
            System.out.print("Nueva veterinaria (actual: " + m.getVeterinaria()+ ", Enter para mantener): ");
            String veterinaria = scanner.nextLine().trim();
            if (!veterinaria.isEmpty()) {
                m.setVeterinaria(veterinaria);
             }
            
            System.out.print("Nuevas observaciones (actuales: " + m.getObservaciones()+ ", Enter para mantener): ");
            String observaciones = scanner.nextLine().trim();
            if (!observaciones.isEmpty()) {
                m.setObservaciones(observaciones);    
             }
            
            mascotaService.getMicrochipService().actualizar(m);
            System.out.println("Microchip actualizado exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al actualizar microchip: " + e.getMessage());
        }
    }

    /**
     * Opción 8: Eliminar microchip por ID (PELIGROSO - soft delete directo).
     *
     * Este método NO verifica si hay mascota asociada.
     * Si hay mascota con FK a este microchip, quedará con referencia huérfana.
     *
     * Flujo:
     * 1. Solicita ID del microchip
     * 2. Invoca microchipService.eliminar() directamente
     * 3. Marca microchip.eliminado = TRUE
     *
     * Problemas potenciales:
     * - Mascota con microchip_id apuntando a microchip "eliminado"
     * - Datos inconsistentes en la BD
     *
     * ALTERNATIVA SEGURA: Opción 10 (eliminarMicrochipPorMascota)
     * - Primero desasocia microchip de la mascota (microchip_id = NULL)
     * - Luego elimina el microchip
     * - Garantiza consistencia
     *
     * Uso válido: 
     * - Limpiar microchips creados por error
     */
    public void eliminarMicrochipPorId() {
        try {
            System.out.print("ID del microchip a eliminar: ");
            int id = Integer.parseInt(scanner.nextLine());
            mascotaService.getMicrochipService().eliminar(id);
            System.out.println("Microchip eliminado exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al eliminar microchip: " + e.getMessage());
        }
    }

    /**
     * Opción 9: Actualizar microchip de una mascota específica.
     *
     * Flujo:
     * 1. Solicita ID de la mascota
     * 2. Verifica que la mascota exista y tenga microchip
     * 3. Muestra valores actuales del microchip
     * 4. Permite actualizar codigo, fecha de implantacion, veterinaria, observaciones
     * 5. Invoca microchipService.actualizar()
     * 
     * Diferencia con opción 8 (actualizarMicrochipPorId):
     * - Esta opción: Busca mascota primero, luego actualiza su microchip
     * - Opción 8: Actualiza microchip directamente por ID
     */
    public void actualizarMicrochipPorMascota() {
        try {
            System.out.print("ID de la mascota cuyo microchip desea actualizar: ");
            int mascotaId = Integer.parseInt(scanner.nextLine());
            Mascota m = mascotaService.getById(mascotaId);

            if (m == null) {
                System.out.println("Mascota no encontrada.");
                return;
            }

            if (m.getMicrochip()== null) {
                System.out.println("La mascota no tiene microchip asociado.");
                return;
            }

            Microchip c = m.getMicrochip();
            
            System.out.print("Nuevo codigo (actual: " + c.getCodigo()+ ", Enter para mantener): ");
            String codigo = scanner.nextLine().trim();
            if (!codigo.isEmpty()) {
                c.setCodigo(codigo);
            }    
            System.out.print("Nueva fecha de implantacion (actual: " + (c.getFechaImplantacion() != null ? c.getFechaImplantacion():"Sin fecha")
                    + ", formato dd/MM/yyyy, Enter para mantener): ");
            String fechaStr = scanner.nextLine().trim();
            if (!fechaStr.isEmpty()) {
                try {
                    java.time.LocalDate nuevaFecha = java.time.LocalDate.parse(
                        fechaStr, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    );
                    c.setFechaImplantacion(nuevaFecha);
                } catch (java.time.format.DateTimeParseException e) {
                    System.out.println("Fecha inválida. Se mantiene la anterior.");
                }
            }
            
            System.out.print("Nueva veterinaria (actual: " + (c.getVeterinaria() != null ? c.getVeterinaria() : "No asignada")
                    + ", Enter para mantener): ");
            String veterinaria = scanner.nextLine().trim();
            if (!veterinaria.isEmpty()) {
                c.setVeterinaria(veterinaria);
             }
            
            System.out.print("Nuevas observaciones (actuales: " + (c.getObservaciones() != null ? c.getObservaciones() : "Sin observaciones") 
                    + ", Enter para mantener): ");
            String observaciones = scanner.nextLine().trim();
            if (!observaciones.isEmpty()) {
                c.setObservaciones(observaciones);    
             }
            
            mascotaService.getMicrochipService().actualizar(c);
            System.out.println("Microchip actualizado exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al actualizar microchip: " + e.getMessage());
        }  
    }

    /**
     * Opción 10: Eliminar microchip de una mascota (MÉTODO SEGURO).
     *
     * Flujo transaccional SEGURO:
     * 1. Solicita ID de la mascota
     * 2. Verifica que la mascota exista y tenga microchip
     * 3. Invoca mascotaService.eliminarMicrochipDeMascota() que:
     *    a. Desasocia microchip de mascota (mascota.microchip = null)
     *    b. Actualiza mascota en BD (microchip_id = NULL)
     *    c. Elimina el microchip (ahora no hay FKs apuntando a él)
     *
     * Ventaja sobre opción 8 (eliminarMicrochipPorId):
     * - Garantiza consistencia: Primero actualiza FK, luego elimina
     * - NO deja referencias huérfanas
     * - Implementa eliminación segura.
     *
     * Este es el método RECOMENDADO para eliminar microchips en producción.
     */
    public void eliminarMicrochipPorMascota() {
        try {
            System.out.print("ID de la mascota cuyo microchip desea eliminar: ");
            int mascotaId = Integer.parseInt(scanner.nextLine());
            Mascota m = mascotaService.getById(mascotaId);

            if (m == null) {
                System.out.println("Mascota no encontrada.");
                return;
            }

            if (m.getMicrochip()== null) {
                System.out.println("La mascota no tiene microchip asociado.");
                return;
            }

            int microchipId = m.getMicrochip().getId();
            mascotaService.eliminarMicrochipDeMascota(mascotaId, microchipId);
            System.out.println("Microchip eliminado exitosamente y referencia actualizada.");
        } catch (Exception e) {
            System.err.println("Error al eliminar microchip: " + e.getMessage());
        }
    }

    /**
     * Método auxiliar privado: Crea un objeto Microchip capturando codigo, fecha de implantacion, veterinaria, observaciones.
     *
     * Flujo:
     * 1. Solicita codigo (con trim)
     * 2. Solicita fecha de implantacion
     * 1. Solicita veterinaria (con trim)
     * 1. Solicita observaciones (con trim)
     * 3. Crea objeto Microchip con ID=0 (será asignado por BD al insertar)
     *
     * Usado por:
     * - crearMascota(): Para agregar microchip al crear mascota
     * - crearMicrochipIndependiente(): Para crear microchip sin asociar
     * - actualizarMicrochipDeMascota(): Para agregar microchip a mascota sin microchip
     *
     * Nota: NO persiste en BD, solo crea el objeto en memoria.
     * El caller es responsable de insertar el microchip.
     *
     * @return Microchip nuevo (no persistido, ID=0)
     */
    private Microchip crearMicrochip() {
        String codigo = pedirObligatorio("Codigo: ");
        System.out.print("Fecha de implantacion: (formato dd/MM/yyyy) ");        
        String fechaStr = scanner.nextLine().trim();
        java.time.LocalDate nuevaFecha = null;
            if (!fechaStr.isEmpty()) {
                try {
                    nuevaFecha = java.time.LocalDate.parse(
                        fechaStr, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));             
                } catch (java.time.format.DateTimeParseException e) {
                    System.out.println("Fecha inválida. No se inicializa");
                }
            }
        System.out.print("Veterinaria: ");
        String veterinaria = scanner.nextLine().trim();
        System.out.print("Observaciones: ");
        String observaciones = scanner.nextLine().trim();            
            
        return new Microchip(0, codigo, nuevaFecha, veterinaria, observaciones);  
    }    

    /**
     * Método auxiliar privado: Maneja actualización de microchip dentro de actualizar mascota.
     *
     * Casos:
     * 1. Mascota TIENE microchip:
     *    - Pregunta si desea actualizar
     *    - Si sí, permite cambiar codigo, fecha de implantacion, veterinaria, observaciones (Enter para mantener)
     *    - Actualiza microchip en BD
     *
     * 2. Mascota NO TIENE microchip:
     *    - Pregunta si desea agregar uno
     *    - Si sí, captura codigo, fecha de implantacion, veterinaria, observaciones con crearMicrochip()
     *    - Inserta microchip en BD (obtiene ID)
     *    - Asocia microchip a la mascota
     *
     * Usado exclusivamente por actualizarMascota() (opción 3).
     *
     * IMPORTANTE: El parámetro Mascota se modifica in-place (setMicrochip).
     * El caller debe invocar mascotaService.actualizar() después para persistir.
     *
     * @param m Mascota la que se le actualizará/agregará microchip
     * @throws Exception Si hay error al insertar/actualizar microchip
     */
    private void actualizarMicrochipDeMascota(Mascota m) throws Exception {
        if (m.getMicrochip()!= null) {
            System.out.print("¿Desea actualizar el microchip? (s/n): ");
            if (scanner.nextLine().equalsIgnoreCase("s")) {
                System.out.print("Nuevo codigo (" + m.getMicrochip().getCodigo() + "): ");
                String codigo = scanner.nextLine().trim();
                if (!codigo.isEmpty()) {
                    m.getMicrochip().setCodigo(codigo);
                }
                
            System.out.print("Nueva fecha de implantación (" + m.getMicrochip().getFechaImplantacion() + ", formato dd/MM/yyyy): ");
            String fechaStr = scanner.nextLine().trim();
            if (!fechaStr.isEmpty()) {
                try {
                    java.time.LocalDate nuevaFecha = java.time.LocalDate.parse(
                        fechaStr, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    );
                    m.getMicrochip().setFechaImplantacion(nuevaFecha);
                } catch (java.time.format.DateTimeParseException e) {
                    System.out.println("Fecha inválida. Se mantiene la anterior.");
                }
            }
            
                System.out.print("Nueva veterinaria (" + m.getMicrochip().getVeterinaria()+ "): ");
                String veterinaria = scanner.nextLine().trim();
                if (!veterinaria.isEmpty()) {
                   m.getMicrochip().setVeterinaria(veterinaria);
                }
                
                System.out.print("Nuevas observaciones (" + m.getMicrochip().getObservaciones()+ "): ");
                String observaciones = scanner.nextLine().trim();
                if (!observaciones.isEmpty()) {
                   m.getMicrochip().setObservaciones(observaciones);
                }          

                mascotaService.getMicrochipService().actualizar(m.getMicrochip());
            }
        } else {
            System.out.print("La mascota no tiene microchip. ¿Desea agregar uno? (s/n): ");
            if (scanner.nextLine().equalsIgnoreCase("s")) {
                Microchip nuevoMic = crearMicrochip();
                mascotaService.getMicrochipService().insertar(nuevoMic);
                m.setMicrochip(nuevoMic);
            }
        }
    }

        private String pedirObligatorio(String prompt) {
        String s;
        do {
            System.out.print(prompt);
            s = scanner.nextLine().trim();
            if (s.isEmpty()) {
                System.out.println("Este campo es obligatorio. Intente nuevamente.");
            }
        } while (s.isEmpty());
        return s;    
    }
        
        
    /**
     * Opción 11: Crear mascota con microchip en una Tx
     * 
     * Crea una nueva mascota con microchip, sin validar el campo de dueño.
     *
     * Esta función se utiliza para la demostración de rollback transaccional.
     * Permite ingresar un dueño vacío, lo que provoca un error SQL debido a la
     * restricción CHECK (chk_duenio_no_vacio) definida en la base de datos.
     *
     * Flujo:
     * 1. Solicita al usuario los datos de la mascota.
     * 2. Permite cargar un microchip asociado.
     * 3. Llama al servicio MascotaServiceImpl.insertarConTransaccionDemoSinValidar().
     * 4. Si la base de datos lanza una excepción (por ejemplo, por dueño vacío),
     *    la transacción se revierte y no se guarda ni el microchip ni la mascota.
     *
     * Objetivo: demostrar que el TransactionManager realiza rollback automático
     * ante un fallo real de integridad en la base de datos.
     *
     * Manejo de errores:
     * - Cualquier excepción durante el proceso se muestra en consola con prefijo "Error al crear mascota".
 */
    public void crearMascotaSinValidar() {
        try {
            
        
        System.out.println("PRUEBA DE TRANSACCIÓN (ROLLBACK DEMO)");
        System.out.println("Primero se ingresarán los datos del Microchip.");
        System.out.println("Luego se pedirá la información de la Mascota.");
        System.out.println("Si ocurre un error en la mascota, se revertirá todo (rollback).");
              
        Microchip microchip = null;    
        microchip = crearMicrochip();  
        
        System.out.print("Nombre: ");
        String nombre = scanner.nextLine().trim();
        System.out.print("Especie: ");
        String especie = scanner.nextLine().trim();
        System.out.print("Raza: ");
        String raza = scanner.nextLine().trim();
        if (raza.isEmpty()) raza = null;
        System.out.print("Fecha de nacimiento (dd/MM/yyyy, vacío para omitir): ");
        String fn = scanner.nextLine().trim();
        java.time.LocalDate fechaNacimiento = null;
        if (!fn.isEmpty()) {
            try {
                fechaNacimiento = java.time.LocalDate.parse(
                    fn, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                );
            } catch (java.time.format.DateTimeParseException e) {
                System.out.println("Fecha inválida. Se guardará sin fecha de nacimiento.");
            }
        }
         System.out.print("Duenio: ");
         String duenio = scanner.nextLine().trim();

            Mascota mascota = new Mascota(0, nombre, especie, raza, fechaNacimiento, duenio, microchip);
            mascota.setMicrochip(microchip);
            mascotaService.insertarConTransaccionDemoSinValidar(mascota);
            System.out.println("Mascota creada exitosamente con ID: " + mascota.getId());
        } catch (Exception e) {
            System.out.println("Error al crear mascota: " + e.getMessage());
        }
    }
}
