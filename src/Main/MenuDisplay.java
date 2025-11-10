package Main;

/**
 * Clase utilitaria para mostrar el menú de la aplicación.
 * Solo contiene métodos estáticos de visualización (no tiene estado).
 *
 * Responsabilidades:
 * - Mostrar el menú principal con todas las opciones disponibles
 * - Formatear la salida de forma consistente
 *
 * Patrón: Utility class (solo métodos estáticos, no instanciable)
 *
 * IMPORTANTE: Esta clase NO lee entrada del usuario.
 * Solo muestra el menú. AppMenu es responsable de leer la opción.
 */
public class MenuDisplay {
    /**
     * Muestra el menú principal con todas las opciones CRUD.
     *
     * Opciones de Mascotas (1-4):
     * 1. Crear mascotas: Permite crear mascota con microchip opcional
     * 2. Listar mascotas: Lista todas o busca por nombre/duenio
     * 3. Actualizar mascotas: Actualiza datos de mascota y opcionalmente su microchip
     * 4. Eliminar mascotas: Soft delete de mascota (NO elimina microchip asociado)
     *
     * Opciones de Microchips (5-10):
     * 5. Crear microchip: Crea microchip independiente (sin asociar a mascota)
     * 6. Listar microchips: Lista todos los microchips activos
     * 7. Actualizar microchip por ID: Actualiza microchip directamente
     * 8. Eliminar microchip por ID: PELIGROSO - puede dejar FKs huérfanas
     * 9. Actualizar microchip por ID de mascota: Busca mascota primero, luego actualiza su microchip
     * 10. Eliminar microchip por ID de mascota: SEGURO - actualiza FK primero, luego elimina
     *
     * Opción de salida:
     * 0. Salir: Termina la aplicación
     *
     * Formato:
     * - Separador visual "========= MENU ========="
     * - Lista numerada clara
     * - Prompt "Ingrese una opcion: " sin salto de línea (espera input)
     *
     * Nota: Los números de opción corresponden al switch en AppMenu.processOption().
     */
    public static void mostrarMenuPrincipal() {
        System.out.println("\n========= MENU =========");
        System.out.println("1. Crear mascota");
        System.out.println("2. Listar mascotas");
        System.out.println("3. Actualizar mascota");
        System.out.println("4. Eliminar mascota");
        System.out.println("5. Crear microchip");
        System.out.println("6. Listar microchips");
        System.out.println("7. Actualizar microchip por ID");
        System.out.println("8. Eliminar microchip por ID");
        System.out.println("9. Actualizar microchip por ID de mascota");
        System.out.println("10. Eliminar microchip por ID de mascota");
        System.out.println("0. Salir");
        System.out.print("Ingrese una opcion: ");
    }
}
