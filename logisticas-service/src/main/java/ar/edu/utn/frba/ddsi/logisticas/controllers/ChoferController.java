package ar.edu.utn.frba.ddsi.logisticas.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/choferes")
public class ChoferController {

    private final ChoferService choferService;

    public ChoferController(ChoferService choferService) {
        this.choferService = choferService;
    }

    //tiene que poder conseguir (GET) las rutas asignadas disponibles
    //tiene que poder brindarme los datos del camion q tenga a disposicion
    @PostMapping
    public ResponseEntity<?> registrarCamion(@RequestBody CamionDisponibleDTO dto) {
        try {
            CamionDisponibleDTO choferCreado = choferService.crearChofer(dto);
            return new ResponseEntity<>(choferCreado, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    //q pueda decirme cuando inicie la ruta
    @GetMapping("/{id}")
    public ResponseEntity<RutaDTO> iniciarRuta(@PathVariable UUID id) {
        Optional<RutaDTO> ruta = choferService.iniciarRuta();
        return ruta.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}