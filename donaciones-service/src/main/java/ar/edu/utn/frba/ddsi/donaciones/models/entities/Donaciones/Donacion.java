package ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaDonante;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class Donacion {
    private UUID id = UUID.randomUUID(); // Identificador único como UUID autogenerado
    private PersonaDonante donante;
    private EntidadBeneficiaria entidad;
    private String descripcion;
    private List<Bien> bienes = new ArrayList<>();
    private Estado estado; // Cambiado a minúscula por convención
    private SubcategoriaBien subcategoria;
    private LocalDate fechaEntrega;

    // Lista para garantizar trazabilidad y auditoría de los estados
    private List<String> historialEstados = new ArrayList<>();

    // Constructor vacío necesario para la deserialización (JSON a Objeto) de Spring
    public Donacion() {
    }

    public Donacion(PersonaDonante donante, EntidadBeneficiaria entidad, String descripcion,
                    List<Bien> bienes, Estado estado, SubcategoriaBien subcategoria, LocalDate fechaEntrega){
        this.donante = donante;
        this.entidad = entidad;
        this.descripcion = descripcion;
        this.bienes = bienes;
        this.estado = estado;
        this.subcategoria = subcategoria;
        this.fechaEntrega = fechaEntrega;
        registrarAuditoriaEstado(estado, "Creación inicial");
    }

    public Integer sumaCantidadBienes(){
        return bienes.stream().mapToInt(Bien::getCantidad).sum();
    }

    public void agregarBien(Bien nuevoBien) {
        if (nuevoBien != null) {
            this.bienes.add(nuevoBien);
        }
    }

    public void agregarBienes(List<Bien> nuevosBienes) {
        this.bienes.addAll(nuevosBienes);
    }

    public void actualizarEstado(Estado nuevoEstado, String justificacion) {
        this.estado = nuevoEstado;
        registrarAuditoriaEstado(nuevoEstado, justificacion);
    }

    private void registrarAuditoriaEstado(Estado estado, String justificacion) {
        String registro = LocalDateTime.now() + " | Estado: " + estado + " | Justificación: " + (justificacion != null ? justificacion : "N/A");
        this.historialEstados.add(registro);
    }

    @Override
    public String toString() {
        return "Donacion{id=" + id + ", donante=" + donante + ", entidad=" + entidad + ", descripcion=" + descripcion + ", bienes=" + bienes + ", estado=" + estado + ", subcategoria=" + subcategoria + ", fechaEntrega=" + fechaEntrega + '}';
    }

}