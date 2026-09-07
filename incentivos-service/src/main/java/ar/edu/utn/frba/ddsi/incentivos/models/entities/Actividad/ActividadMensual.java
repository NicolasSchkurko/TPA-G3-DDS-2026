package ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.YearMonth;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "actividad_mensual",
        uniqueConstraints = @UniqueConstraint(columnNames = {"perfil_id", "periodo"})
)
public class ActividadMensual {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perfil_id", nullable = false)
    private Perfil perfil;

    @Column(nullable = false)
    @Convert(converter = YearMonthConverter.class) // traduce YearMonth a un formato que mysql entienda
    private YearMonth periodo;

    @Column(nullable = false)
    private Integer cantidadDonaciones = 0;

    @Column(nullable = false)
    private Integer cantidadBienes = 0;

    @ElementCollection
    @CollectionTable(
            name = "actividad_mensual_organizacion",
            joinColumns = @JoinColumn(name = "actividad_mensual_id")
    )
    @Column(name = "entidad_beneficiaria")
    private Set<String> entidadesBeneficiadas = new HashSet<>();

    public ActividadMensual(Perfil perfil, YearMonth periodo) {
        this.perfil = perfil;
        this.periodo = periodo;
    }

    public void registrarDonacion(Integer bienes, String entidadBeneficiaria) {
        cantidadDonaciones++;
        cantidadBienes += bienes == null ? 0 : bienes;

        if (entidadBeneficiaria != null && !entidadBeneficiaria.isBlank()) {
            entidadesBeneficiadas.add(entidadBeneficiaria);
        }
    }

    public Integer getCantidadOrganizaciones() {
        return entidadesBeneficiadas.size();
    }
}
