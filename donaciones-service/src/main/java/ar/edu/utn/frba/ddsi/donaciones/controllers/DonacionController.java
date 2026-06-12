package ar.edu.utn.frba.ddsi.donaciones.controllers;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/donaciones")
public class DonacionController {

     // Ruta para crear: POST /donacion
    @PostMapping
    public String crearDonacion() {
        // Lógica para crear una donación
        return "Donación creada con éxito";
    }

     // Ruta para buscar: GET /donacion/buscar?nombre=Donacion1
    @GetMapping("/buscar")
    public String buscarDonacion(@RequestParam String nombre) {
        // Lógica para buscar una donación por nombre
        return "Resultado de búsqueda para: " + nombre;
    }
}
