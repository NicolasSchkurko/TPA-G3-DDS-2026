package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/donaciones")
public class DonacionesViewController {

    private final DonacionesClient donacionesClient;

    public DonacionesViewController(DonacionesClient donacionesClient) {
        this.donacionesClient = donacionesClient;
    }

    @GetMapping
    public String listarDonaciones(@RequestParam(value = "q", required = false) String q,
                                  @RequestParam(value = "estado", required = false) String estado,
                                  @RequestParam(value = "categoria", required = false) String categoria,
                                  @RequestParam(value = "page", defaultValue = "0") int page,
                                  Model model) {
        List<DonacionDTO> todas = donacionesClient.obtenerTodasLasDonaciones();
        if (todas == null) {
            todas = List.of();
        }

        String qNorm = (q == null || q.isBlank()) ? null : q.trim().toLowerCase();
        String estadoNorm = (estado == null || estado.isBlank()) ? null : estado.trim();
        String categoriaNorm = (categoria == null || categoria.isBlank()) ? null : categoria.trim();

        List<DonacionDTO> filtradas = todas.stream()
                .filter(d -> estadoNorm == null || estadoNorm.equalsIgnoreCase(d.getEstado()))
                .filter(d -> categoriaNorm == null || categoriaNorm.equalsIgnoreCase(d.getCategoriaBienName()))
                .filter(d -> qNorm == null || contieneTexto(d, qNorm))
                .collect(Collectors.toList());

        List<String> estadosDisponibles = todas.stream()
                .map(DonacionDTO::getEstado)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.toList());

        List<String> categoriasDisponibles = todas.stream()
                .map(DonacionDTO::getCategoriaBienName)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.toList());

        Map<String, String> filtros = new LinkedHashMap<>();
        filtros.put("q", q == null ? "" : q);
        filtros.put("estado", estado == null ? "" : estado);
        filtros.put("categoria", categoria == null ? "" : categoria);

        int pageSize = 6;
        int totalResultados = filtradas.size();
        int totalPaginas = Math.max(1, (int) Math.ceil((double) totalResultados / pageSize));
        int paginaActual = Math.min(Math.max(page, 0), totalPaginas - 1);
        int desde = paginaActual * pageSize;
        int hasta = Math.min(desde + pageSize, totalResultados);
        List<DonacionDTO> pagina = desde >= totalResultados ? List.of() : filtradas.subList(desde, hasta);

        model.addAttribute("donaciones", pagina);
        model.addAttribute("donacionesFiltradas", pagina);
        model.addAttribute("estadosDisponibles", estadosDisponibles);
        model.addAttribute("categoriasDisponibles", categoriasDisponibles);
        model.addAttribute("filtros", filtros);
        model.addAttribute("totalResultados", totalResultados);
        model.addAttribute("paginaActual", paginaActual);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("tienePaginaAnterior", paginaActual > 0);
        model.addAttribute("tienePaginaSiguiente", paginaActual + 1 < totalPaginas);
        return "donaciones/listar";
    }

    private static boolean contieneTexto(DonacionDTO d, String q) {
        return contiene(d.getDescripcion(), q)
                || contiene(d.getDonanteName(), q)
                || contiene(d.getEntidadBeneficiaria(), q)
                || contiene(d.getSubcategoriaName(), q)
                || contiene(d.getCategoriaBienName(), q);
    }

    private static boolean contiene(String valor, String q) {
        return valor != null && valor.toLowerCase().contains(q);
    }

    @GetMapping("/{id}")
    public String detalleDonacion(@PathVariable UUID id, Model model) {
        DonacionDTO donacion = donacionesClient.obtenerDonacionPorId(id);
        if (donacion == null) {
            model.addAttribute("error", "No se encontró la donación solicitada.");
            return "redirect:/donaciones";
        }
        model.addAttribute("donacion", donacion);
        return "donacion/detalle-donacion";
    }

    @GetMapping("/donantes")
    public String listarDonantes(Model model) {
        var donantes = donacionesClient.obtenerDonantes();
        model.addAttribute("donantes", donantes);
        return "donaciones/donantes";
    }
}
