package ar.edu.utn.frba.ddsi.front.controllers;


import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.LogisticasClient;
import ar.edu.utn.frba.ddsi.front.dto.BienDonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.EdicionDonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.front.dto.FormularioDonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.FormularioDonanteDTO;
import ar.edu.utn.frba.ddsi.front.dto.FormularioEntidadDTO;
import ar.edu.utn.frba.ddsi.front.dto.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.front.dto.PersonaDonanteDTO;
import ar.edu.utn.frba.ddsi.front.dto.RepresentanteDTO;
import ar.edu.utn.frba.ddsi.front.dto.ResultadoMatchmakingDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final LogisticasClient logisticasClient;
    private final DonacionesClient donacionesClient;

    public AdminController(LogisticasClient logisticasClient, DonacionesClient donacionesClient) {
        this.logisticasClient = logisticasClient;
        this.donacionesClient = donacionesClient;
    }

    @GetMapping("/admin")
    public String mostrarAdmin(
            @AuthenticationPrincipal OidcUser usuario,
            Model model) {

        agregarDatosDelAdmin(usuario, model);
        return "admin/admin-landing";
    }

    @GetMapping("/landing")
    public String mostrarlanding() {
        return "/landing";
    }

    @GetMapping("/camiones")
    public String listarcamiones(Model model) {
        var camiones = logisticasClient.obtenerCamiones();
        model.addAttribute("camiones", camiones);
        return "admin/camiones";
    }

    // ---------------------------------------------------------------------
    // Administración de donantes
    // ---------------------------------------------------------------------

    /**
     * Listado completo de donantes. El filtro por nombre se resuelve en el navegador sobre esta
     * misma tabla (ver donantes.js): el endpoint /api/personas/buscar del backend devuelve un único
     * donante y no una lista, así que no sirve para un buscador incremental.
     */
    @GetMapping("/admin/donantes")
    public String mostrarDonantes(@AuthenticationPrincipal OidcUser usuario, Model model) {
        agregarDatosDelAdmin(usuario, model);
        List<PersonaDonanteDTO> donantes = donacionesClient.obtenerDonantes();
        model.addAttribute("donantes", donantes);
        return "admin/donantes";
    }

    @PostMapping("/admin/donantes/importar")
    public String importarDonantes(@RequestParam("archivo") MultipartFile archivo,
                                   RedirectAttributes redirect) {
        if (archivo == null || archivo.isEmpty()) {
            redirect.addFlashAttribute("error", "Elegí un archivo CSV antes de importar.");
            return "redirect:/admin/donantes";
        }

        String nombreArchivo = archivo.getOriginalFilename();
        if (nombreArchivo != null && !nombreArchivo.toLowerCase().endsWith(".csv")) {
            redirect.addFlashAttribute("error",
                    "El archivo tiene que ser un .csv (recibimos '" + nombreArchivo + "').");
            return "redirect:/admin/donantes";
        }

        try {
            UUID importId = donacionesClient.importarDonantesCSV(archivo.getBytes(), nombreArchivo);
            // La importación es asincrónica: el backend responde 202 y procesa en segundo plano,
            // así que el listado recién recargado puede todavía no incluir las filas nuevas.
            redirect.addFlashAttribute("exito",
                    "Importación encolada. Los donantes van a aparecer en el listado en unos segundos; "
                            + "actualizá la página para verlos.");
            if (importId != null) {
                redirect.addFlashAttribute("importId", importId.toString());
            }
        } catch (Exception e) {
            log.error("Falló la importación de donantes por CSV: {}", e.getMessage());
            redirect.addFlashAttribute("error", "No se pudo importar el archivo: " + e.getMessage());
        }
        return "redirect:/admin/donantes";
    }

    /**
     * Formulario de alta. Es una pantalla propia y no un modal porque son más de veinte campos más
     * las secciones repetibles de medios de contacto y representantes: no entran en un diálogo.
     */
    @GetMapping("/admin/donantes/nuevo")
    public String nuevoDonante(@RequestParam(name = "volverA", required = false) String volverA,
                               @AuthenticationPrincipal OidcUser usuario,
                               Model model) {
        agregarDatosDelAdmin(usuario, model);
        FormularioDonanteDTO formulario = new FormularioDonanteDTO();
        formulario.setTipoPersona("HUMANA");
        prepararFormulario(model, formulario, List.of(), List.of());
        model.addAttribute("edicion", false);
        model.addAttribute("accion", "/admin/donantes/nuevo");
        model.addAttribute("volverA", rutaInternaONull(volverA));
        return "admin/donante-formulario";
    }

    /**
     * @param volverA pantalla a la que volver con el donante nuevo ya elegido. Lo manda el alta de
     *                donación cuando el admin no reconoce al donador y lo registra desde ahí.
     */
    @PostMapping("/admin/donantes/nuevo")
    public String crearDonante(@RequestParam(name = "volverA", required = false) String volverA,
                               @AuthenticationPrincipal OidcUser usuario,
                               @ModelAttribute FormularioDonanteDTO formulario,
                               Model model,
                               RedirectAttributes redirect) {
        String destino = rutaInternaONull(volverA);
        try {
            UUID id = donacionesClient.crearDonante(formulario.aPayload());
            redirect.addFlashAttribute("exito", "Donante registrado correctamente.");
            if (destino != null) {
                // Vuelve al formulario que lo mandó acá con el donante nuevo preseleccionado.
                return "redirect:" + destino + (id != null ? "?donante=" + id : "");
            }
            return "redirect:/admin/donantes";
        } catch (Exception e) {
            log.error("Falló el registro de un donante: {}", e.getMessage());
            // Se repinta el formulario con lo que el admin había cargado en vez de redirigir,
            // para que no tenga que tipear todo de nuevo.
            agregarDatosDelAdmin(usuario, model);
            prepararFormulario(model, formulario,
                    formulario.mediosPrecargados(), formulario.representantesPrecargados());
            model.addAttribute("edicion", false);
            model.addAttribute("accion", "/admin/donantes/nuevo");
            model.addAttribute("volverA", destino);
            model.addAttribute("error", mensajeDeError(e, "No se pudo registrar el donante"));
            return "admin/donante-formulario";
        }
    }

    /**
     * Acepta un destino de vuelta sólo si es una ruta del panel de administración. El valor llega
     * por query string, así que sin este filtro alcanzaría con cambiar la URL a mano para que el
     * formulario redirija a un sitio externo después de guardar.
     */
    private static String rutaInternaONull(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return null;
        }
        String limpia = ruta.trim();
        // "//host" es una URL protocol-relative y ":" abre la puerta a otros esquemas. Se rechazan
        // también "?" y "#" porque al destino se le agrega después ?donante=<id>.
        if (!limpia.startsWith("/admin/") || limpia.startsWith("//")
                || limpia.contains(":") || limpia.contains("\\")
                || limpia.contains("?") || limpia.contains("#")) {
            return null;
        }
        return limpia;
    }

    /** Formulario de edición con todos los datos modificables del donante ya cargados. */
    @GetMapping("/admin/donantes/{id}/editar")
    public String editarDonante(@PathVariable UUID id,
                                @AuthenticationPrincipal OidcUser usuario,
                                Model model,
                                RedirectAttributes redirect) {
        PersonaDonanteDTO donante = donacionesClient.obtenerDonantePorId(id);
        if (donante == null) {
            redirect.addFlashAttribute("error", "No encontramos al donante que querés editar.");
            return "redirect:/admin/donantes";
        }

        agregarDatosDelAdmin(usuario, model);
        prepararFormulario(model, FormularioDonanteDTO.desde(donante),
                donante.getMediosDeContacto(), donante.getRepresentantes());
        model.addAttribute("edicion", true);
        model.addAttribute("idDonante", id);
        model.addAttribute("accion", "/admin/donantes/" + id + "/actualizar");
        model.addAttribute("donante", donante);
        return "admin/donante-formulario";
    }

    @PostMapping("/admin/donantes/{id}/actualizar")
    public String actualizarDonante(@PathVariable UUID id,
                                    @AuthenticationPrincipal OidcUser usuario,
                                    @ModelAttribute FormularioDonanteDTO formulario,
                                    Model model,
                                    RedirectAttributes redirect) {
        try {
            donacionesClient.actualizarDonanteCampos(id, formulario.aPayload());
            redirect.addFlashAttribute("exito", "Donante actualizado correctamente.");
            return "redirect:/admin/donantes";
        } catch (Exception e) {
            log.error("Falló la actualización del donante {}: {}", id, e.getMessage());
            agregarDatosDelAdmin(usuario, model);
            prepararFormulario(model, formulario,
                    formulario.mediosPrecargados(), formulario.representantesPrecargados());
            model.addAttribute("edicion", true);
            model.addAttribute("idDonante", id);
            model.addAttribute("accion", "/admin/donantes/" + id + "/actualizar");
            model.addAttribute("error", mensajeDeError(e, "No se pudo actualizar el donante"));
            return "admin/donante-formulario";
        }
    }

    @PostMapping("/admin/donantes/{id}/eliminar")
    public String eliminarDonante(@PathVariable UUID id, RedirectAttributes redirect) {
        try {
            donacionesClient.eliminarDonante(id);
            redirect.addFlashAttribute("exito", "Donante eliminado correctamente.");
        } catch (Exception e) {
            log.error("Falló la baja del donante {}: {}", id, e.getMessage());
            redirect.addFlashAttribute("error", "No se pudo eliminar el donante: " + e.getMessage());
        }
        return "redirect:/admin/donantes";
    }

    // ---------------------------------------------------------------------
    // Administración de entidades beneficiarias
    // ---------------------------------------------------------------------

    /**
     * Listado completo de entidades. El backend ya filtra las dadas de baja y devuelve las
     * necesidades embebidas en cada entidad, así que el panel desplegable de necesidades se
     * renderiza del lado del servidor sin un segundo request por fila.
     */
    @GetMapping("/admin/entidades")
    public String mostrarEntidades(@AuthenticationPrincipal OidcUser usuario, Model model) {
        agregarDatosDelAdmin(usuario, model);
        List<EntidadBeneficiariaDTO> entidades = donacionesClient.obtenerEntidades();
        model.addAttribute("entidades", entidades);
        return "admin/entidades";
    }

    @GetMapping("/admin/entidades/nuevo")
    public String nuevaEntidad(@AuthenticationPrincipal OidcUser usuario, Model model) {
        agregarDatosDelAdmin(usuario, model);
        FormularioEntidadDTO formulario = new FormularioEntidadDTO();
        formulario.setTipoJuridico("ONG");
        model.addAttribute("formulario", formulario);
        model.addAttribute("edicion", false);
        model.addAttribute("accion", "/admin/entidades/nuevo");
        return "admin/entidad-formulario";
    }

    @PostMapping("/admin/entidades/nuevo")
    public String crearEntidad(@AuthenticationPrincipal OidcUser usuario,
                               @ModelAttribute FormularioEntidadDTO formulario,
                               Model model,
                               RedirectAttributes redirect) {
        try {
            donacionesClient.crearEntidad(formulario.aPayload());
            redirect.addFlashAttribute("exito", "Entidad registrada correctamente.");
            return "redirect:/admin/entidades";
        } catch (Exception e) {
            log.error("Falló el registro de una entidad: {}", e.getMessage());
            // Se repinta el formulario con lo cargado en vez de redirigir, para no perder el tipeo.
            agregarDatosDelAdmin(usuario, model);
            model.addAttribute("formulario", formulario);
            model.addAttribute("edicion", false);
            model.addAttribute("accion", "/admin/entidades/nuevo");
            model.addAttribute("error", mensajeDeError(e, "No se pudo registrar la entidad"));
            return "admin/entidad-formulario";
        }
    }

    @GetMapping("/admin/entidades/{id}/editar")
    public String editarEntidad(@PathVariable UUID id,
                                @AuthenticationPrincipal OidcUser usuario,
                                Model model,
                                RedirectAttributes redirect) {
        EntidadBeneficiariaDTO entidad = donacionesClient.obtenerEntidadPorId(id);
        if (entidad == null) {
            redirect.addFlashAttribute("error", "No encontramos la entidad que querés editar.");
            return "redirect:/admin/entidades";
        }

        agregarDatosDelAdmin(usuario, model);
        model.addAttribute("formulario", FormularioEntidadDTO.desde(entidad));
        model.addAttribute("edicion", true);
        model.addAttribute("idEntidad", id);
        model.addAttribute("accion", "/admin/entidades/" + id + "/actualizar");
        model.addAttribute("entidad", entidad);
        return "admin/entidad-formulario";
    }

    @PostMapping("/admin/entidades/{id}/actualizar")
    public String actualizarEntidad(@PathVariable UUID id,
                                    @AuthenticationPrincipal OidcUser usuario,
                                    @ModelAttribute FormularioEntidadDTO formulario,
                                    Model model,
                                    RedirectAttributes redirect) {
        try {
            donacionesClient.actualizarEntidad(id, formulario.aPayload());
            redirect.addFlashAttribute("exito", "Entidad actualizada correctamente.");
            return "redirect:/admin/entidades";
        } catch (Exception e) {
            log.error("Falló la actualización de la entidad {}: {}", id, e.getMessage());
            agregarDatosDelAdmin(usuario, model);
            model.addAttribute("formulario", formulario);
            model.addAttribute("edicion", true);
            model.addAttribute("idEntidad", id);
            model.addAttribute("accion", "/admin/entidades/" + id + "/actualizar");
            model.addAttribute("error", mensajeDeError(e, "No se pudo actualizar la entidad"));
            return "admin/entidad-formulario";
        }
    }

    @PostMapping("/admin/entidades/{id}/eliminar")
    public String eliminarEntidad(@PathVariable UUID id, RedirectAttributes redirect) {
        try {
            donacionesClient.eliminarEntidad(id);
            redirect.addFlashAttribute("exito", "Entidad eliminada correctamente.");
        } catch (Exception e) {
            log.error("Falló la baja de la entidad {}: {}", id, e.getMessage());
            redirect.addFlashAttribute("error", "No se pudo eliminar la entidad: " + e.getMessage());
        }
        return "redirect:/admin/entidades";
    }

    // ---------------------------------------------------------------------
    // Administración de donaciones
    // ---------------------------------------------------------------------

    /**
     * Dos vistas sobre la misma pantalla.
     *
     * <ul>
     *   <li>Por defecto, todas las donaciones del sistema para actualizarlas, cambiarles el estado
     *       o eliminarlas.</li>
     *   <li>Con {@code ?vista=matchmaking}, sólo las que esperan asignación, cada una con el
     *       ranking de entidades que propuso el asignador. Esa lista sale de
     *       GET /api/donaciones/pendientes, que devuelve un resultado por donación en
     *       PENDIENTE_ASIGNACION: el backend guarda el resultado y después cambia el estado, así
     *       que las dos cosas van siempre juntas.</li>
     * </ul>
     */
    @GetMapping("/admin/donaciones")
    public String mostrarDonaciones(@RequestParam(name = "vista", required = false) String vista,
                                    @AuthenticationPrincipal OidcUser usuario,
                                    Model model) {
        agregarDatosDelAdmin(usuario, model);

        boolean matchmaking = "matchmaking".equalsIgnoreCase(vista);
        model.addAttribute("vistaMatchmaking", matchmaking);

        if (matchmaking) {
            List<ResultadoMatchmakingDTO> resultados = donacionesClient.obtenerResultadosMatchmaking();
            model.addAttribute("resultados", resultados);
            model.addAttribute("pendientes", resultados.size());
        } else {
            List<DonacionDTO> donaciones = donacionesClient.obtenerTodasLasDonaciones();
            model.addAttribute("donaciones", donaciones);
            // Para el contador del botón de matchmaking: cuántas están esperando que alguien
            // elija la entidad.
            model.addAttribute("pendientes", donaciones.stream()
                    .filter(d -> "PENDIENTE_ASIGNACION".equalsIgnoreCase(d.getEstado()))
                    .count());
        }
        return "admin/donaciones";
    }

    /**
     * Alta de donación.
     *
     * @param donante id a preseleccionar. Lo usa la vuelta desde el alta de donante: si el admin
     *                no reconocía al donador, lo registra y vuelve acá con el nuevo ya elegido.
     */
    @GetMapping("/admin/donaciones/nuevo")
    public String nuevaDonacion(@RequestParam(name = "donante", required = false) UUID donante,
                                @AuthenticationPrincipal OidcUser usuario,
                                Model model) {
        agregarDatosDelAdmin(usuario, model);
        FormularioDonacionDTO formulario = new FormularioDonacionDTO();
        formulario.setIdDonante(donante);
        prepararFormularioDonacion(model, formulario);
        return "admin/donacion-nueva";
    }

    @PostMapping("/admin/donaciones/nuevo")
    public String registrarDonacion(@AuthenticationPrincipal OidcUser usuario,
                                    @ModelAttribute FormularioDonacionDTO formulario,
                                    Model model,
                                    RedirectAttributes redirect) {
        try {
            formulario.validarYNormalizar();
            List<DonacionDTO> creadas = donacionesClient.registrarDonacion(formulario);
            redirect.addFlashAttribute("exito", mensajeDeAlta(creadas));
            return "redirect:/admin/donaciones";
        } catch (Exception e) {
            log.error("Falló el registro de una donación: {}", e.getMessage());
            agregarDatosDelAdmin(usuario, model);
            prepararFormularioDonacion(model, formulario);
            model.addAttribute("error", mensajeDeError(e, "No se pudo registrar la donación"));
            return "admin/donacion-nueva";
        }
    }

    /**
     * Pantalla de actualización. Incluye el cambio de estado con su motivo, que viaja por otro
     * endpoint (PATCH) pero conceptualmente es parte de "actualizar la donación".
     */
    @GetMapping("/admin/donaciones/{id}/editar")
    public String editarDonacion(@PathVariable UUID id,
                                 @AuthenticationPrincipal OidcUser usuario,
                                 Model model,
                                 RedirectAttributes redirect) {
        DonacionDTO donacion = donacionesClient.obtenerDonacionPorId(id);
        if (donacion == null) {
            redirect.addFlashAttribute("error", "No encontramos la donación que querés editar.");
            return "redirect:/admin/donaciones";
        }

        agregarDatosDelAdmin(usuario, model);
        model.addAttribute("formulario", EdicionDonacionDTO.desde(donacion));
        model.addAttribute("donacion", donacion);
        return "admin/donacion-editar";
    }

    @PostMapping("/admin/donaciones/{id}/actualizar")
    public String actualizarDonacion(@PathVariable UUID id,
                                     @AuthenticationPrincipal OidcUser usuario,
                                     @ModelAttribute EdicionDonacionDTO formulario,
                                     Model model,
                                     RedirectAttributes redirect) {
        DonacionDTO donacion = donacionesClient.obtenerDonacionPorId(id);
        if (donacion == null) {
            redirect.addFlashAttribute("error", "No encontramos la donación que querés actualizar.");
            return "redirect:/admin/donaciones";
        }

        try {
            donacionesClient.actualizarDonacion(id, formulario.aPayload(donacion.puedeEditarBienes()));
            redirect.addFlashAttribute("exito", "Donación actualizada correctamente.");
            return "redirect:/admin/donaciones";
        } catch (Exception e) {
            log.error("Falló la actualización de la donación {}: {}", id, e.getMessage());
            agregarDatosDelAdmin(usuario, model);
            model.addAttribute("formulario", formulario);
            model.addAttribute("donacion", donacion);
            model.addAttribute("error", mensajeDeError(e, "No se pudo actualizar la donación"));
            return "admin/donacion-editar";
        }
    }

    /**
     * Cambio de estado con justificación. El motivo es obligatorio acá aunque el backend lo acepte
     * en null: queda asentado en el historial de la donación y es la única traza de por qué alguien
     * la movió a mano.
     */
    @PostMapping("/admin/donaciones/{id}/estado")
    public String cambiarEstadoDonacion(@PathVariable UUID id,
                                        @RequestParam(name = "nuevoEstado", required = false) String nuevoEstado,
                                        @RequestParam(name = "justificacion", required = false) String justificacion,
                                        RedirectAttributes redirect) {
        String motivo = justificacion == null ? "" : justificacion.trim();
        try {
            if (nuevoEstado == null || nuevoEstado.isBlank()) {
                throw new IllegalArgumentException("Elegí a qué estado querés pasar la donación.");
            }
            if (motivo.isEmpty()) {
                throw new IllegalArgumentException("Escribí el motivo del cambio de estado.");
            }
            donacionesClient.cambiarEstadoDonacion(id, nuevoEstado.trim().toUpperCase(), motivo);
            redirect.addFlashAttribute("exito",
                    "Estado actualizado a " + DonacionDTO.etiquetaDeEstado(nuevoEstado) + ".");
            return "redirect:/admin/donaciones";
        } catch (Exception e) {
            log.error("Falló el cambio de estado de la donación {} a {}: {}", id, nuevoEstado, e.getMessage());
            redirect.addFlashAttribute("error", mensajeDeError(e, "No se pudo cambiar el estado"));
            return "redirect:/admin/donaciones/" + id + "/editar";
        }
    }

    @PostMapping("/admin/donaciones/{id}/eliminar")
    public String eliminarDonacion(@PathVariable UUID id, RedirectAttributes redirect) {
        try {
            donacionesClient.eliminarDonacion(id);
            redirect.addFlashAttribute("exito", "Donación eliminada correctamente.");
        } catch (Exception e) {
            log.error("Falló la baja de la donación {}: {}", id, e.getMessage());
            redirect.addFlashAttribute("error", "No se pudo eliminar la donación: " + e.getMessage());
        }
        return "redirect:/admin/donaciones";
    }

    /**
     * Dispara el asignador sobre todas las donaciones en depósito. Las que consigan al menos una
     * necesidad compatible pasan a PENDIENTE_ASIGNACION y aparecen en la vista de matchmaking; las
     * que no, se quedan en depósito y no generan ninguna propuesta.
     */
    @PostMapping("/admin/donaciones/matchmaking/ejecutar")
    public String ejecutarMatchmaking(RedirectAttributes redirect) {
        try {
            donacionesClient.ejecutarMatchmaking();
            redirect.addFlashAttribute("exito",
                    "Asignador ejecutado. Abajo quedan las donaciones que consiguieron propuestas; "
                            + "las que no aparecen siguen en depósito porque ninguna entidad tiene "
                            + "una necesidad compatible.");
        } catch (Exception e) {
            log.error("Falló la ejecución del asignador: {}", e.getMessage());
            redirect.addFlashAttribute("error", "No se pudo ejecutar el asignador: " + e.getMessage());
        }
        return "redirect:/admin/donaciones?vista=matchmaking";
    }

    /**
     * Aprueba una propuesta del ranking: la donación queda asignada a esa entidad, se suma a su
     * necesidad y sale publicada a logística.
     *
     * @param posicion posición 1-based de la propuesta elegida.
     * @param entidad  razón social, sólo para el mensaje de confirmación.
     */
    @PostMapping("/admin/donaciones/{id}/asignar")
    public String asignarDonacion(@PathVariable UUID id,
                                  @RequestParam(name = "posicion", required = false) Integer posicion,
                                  @RequestParam(name = "entidad", required = false) String entidad,
                                  RedirectAttributes redirect) {
        try {
            if (posicion == null || posicion < 1) {
                throw new IllegalArgumentException("Elegí una de las entidades propuestas.");
            }
            donacionesClient.asignarPropuesta(id, posicion);
            redirect.addFlashAttribute("exito", entidad == null || entidad.isBlank()
                    ? "Donación asignada correctamente."
                    : "Donación asignada a " + entidad + ". Ya quedó lista para entregar.");
        } catch (Exception e) {
            log.error("Falló la asignación de la donación {} (propuesta {}): {}", id, posicion, e.getMessage());
            // El backend sí manda cuerpo cuando algo sale mal, así que se muestra. Se agrega la
            // causa habitual porque el mensaje suele ser genérico ("Error interno del servidor").
            redirect.addFlashAttribute("error", mensajeDeError(e,
                    "No se pudo asignar la donación. Puede que la propuesta haya quedado vieja "
                            + "(volvé a ejecutar el asignador) o que la necesidad ya esté satisfecha"));
        }
        return "redirect:/admin/donaciones?vista=matchmaking";
    }

    /**
     * Atributos del formulario de alta de donación: el comando y la lista de donantes entre los
     * que elegir. Se mandan todos y el filtrado es en el navegador, igual que en /admin/donantes.
     */
    private void prepararFormularioDonacion(Model model, FormularioDonacionDTO formulario) {
        if (formulario.getBienes() == null || formulario.getBienes().isEmpty()) {
            formulario.setBienes(new ArrayList<>(List.of(new BienDonacionDTO())));
        }
        model.addAttribute("formulario", formulario);
        model.addAttribute("donantes", donacionesClient.obtenerDonantes());
    }

    /** Avisa cuándo el formulario se partió en varias donaciones, para que no sorprenda. */
    private static String mensajeDeAlta(List<DonacionDTO> creadas) {
        String base = creadas.size() > 1
                ? "Donación registrada: quedó segmentada en " + creadas.size()
                        + " donaciones, una por subcategoría y unidad de medida."
                : "Donación registrada correctamente.";
        return base + " Queda en depósito: ejecutá el asignador desde la vista de matchmaking "
                + "para proponerle entidades.";
    }

    /**
     * Atributos comunes del formulario de donante: el comando y las filas repetibles ya cargadas.
     * Las opciones de los combos (género, tipo jurídico, tipo de contacto) van escritas en la
     * plantilla porque el value tiene que coincidir con el enum del backend pero la etiqueta que ve
     * el admin no: ahí se muestra "Hombre", no "HOMBRE".
     */
    private void prepararFormulario(Model model,
                                    FormularioDonanteDTO formulario,
                                    List<MediosContactoDTO> medios,
                                    List<RepresentanteDTO> representantes) {
        model.addAttribute("formulario", formulario);
        model.addAttribute("mediosPrecargados", medios != null ? medios : List.of());
        model.addAttribute("representantesPrecargados", representantes != null ? representantes : List.of());
    }

    /** Los errores de validación propios se muestran tal cual; los del backend, resumidos. */
    private static String mensajeDeError(Exception e, String prefijo) {
        if (e instanceof IllegalArgumentException) {
            return e.getMessage();
        }
        return prefijo + ": " + e.getMessage();
    }

    /** Datos que consume la cabecera de admin (nombre, apellido y rol). */
    private void agregarDatosDelAdmin(OidcUser usuario, Model model) {
        if (usuario == null) {
            model.addAttribute("nombre", "");
            model.addAttribute("apellido", "");
            model.addAttribute("rol", "Usuario");
            return;
        }

        model.addAttribute("nombre", usuario.getGivenName());
        model.addAttribute("apellido", usuario.getFamilyName());
        model.addAttribute("username", usuario.getPreferredUsername());

        boolean esAdministrador = usuario.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        model.addAttribute("rol", esAdministrador ? "Administrador" : "Usuario");
    }
}
