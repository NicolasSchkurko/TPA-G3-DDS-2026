package ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
public class Donacion {
    @Id
    private UUID id = UUID.randomUUID();

    // ManyToOne porque el Donante vive en su propio repositorio; sin cascade REMOVE:
    // eliminar la donación no debe borrar al donante.
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "donante_id")
    private Donante donante;

    // Al crearse la Donacion todavía no tiene entidad: se asigna después vía matchmaking.
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "entidad_id")
    private EntidadBeneficiaria entidad;

    // Lado dueño de la relación con Necesidad (inversa: Necesidad.donaciones).
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "necesidad_id")
    private Necesidad necesidad;

    private String descripcion;

    // Cada Bien pertenece a este segmento pero su ciclo de vida sigue en su propio
    // repositorio: cascade PERSIST/MERGE, sin orphanRemoval.
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "donacion_id")
    private List<Bien> bienes = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private Estado estado;

    // Catálogo compartido (buscar-o-crear vía RepositorioSubcategoriasDeBienes), sin cascade.
    @ManyToOne
    @JoinColumn(name = "subcategoria_id")
    private SubcategoriaBien subcategoria;

    private LocalDate fechaEntrega;

    /** Trazabilidad de los cambios de estado. */
    @ElementCollection
    @CollectionTable(name = "donacion_historial_estados", joinColumns = @JoinColumn(name = "donacion_id"))
    @Column(name = "registro", length = 1000)
    private List<String> historialEstados = new ArrayList<>();

    // Constructor vacío requerido por JPA y la deserialización de Spring.
    public Donacion() {
    }

    public Donacion(
        Donante donante, EntidadBeneficiaria entidad, String descripcion,
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
        return bienes.stream().mapToInt(Bien::getPeso).sum();
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
        // Única escritura de fechaEntrega en todo el módulo: de acá dependen
        // SubAtendidos.cantidadDonacionesUltimoTrimestre y
        // NecesidadRecurrente.cantidadRecibidaEnPeriodo, que hasta ahora siempre veían null.
        // Centralizado aquí (no en los gestores) porque hay dos caminos a ENTREGADO:
        // GestorAsignaciones.cambiarEstado (manual) y
        // GestorEventosLogistica.manejarEntregaConfirmada (evento de logística).
        if (nuevoEstado == Estado.ENTREGADO && this.fechaEntrega == null) {
            this.fechaEntrega = LocalDate.now();
        }
        registrarAuditoriaEstado(nuevoEstado, justificacion);
    }

    private void registrarAuditoriaEstado(Estado estado, String justificacion) {
        String registro = LocalDateTime.now() + " | Estado: " + estado + " | Justificación: " + (justificacion != null ? justificacion : "N/A");
        this.historialEstados.add(registro);
    }

    @Override
    public String toString() {
        return "Donacion{id=" + id + ", donante=" + donante + ", entidad=" + entidad + ", necesidad=" + (necesidad != null ? necesidad.getId() : null) + ", descripcion=" + descripcion + ", bienes=" + bienes + ", estado=" + estado + ", subcategoria=" + subcategoria + ", fechaEntrega=" + fechaEntrega + '}';
    }

}