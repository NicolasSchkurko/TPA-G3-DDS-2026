package ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "direccion")
@Getter
@Setter
@NoArgsConstructor
public class Direccion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_direccion", nullable = false, updatable = false)
    private UUID idDireccion;

    /**
     * Control de concurrencia optimista.
     *
     * <p>Sin esto, dos instancias de logistica que leen esta fila y escriben sobre ella lo
     * hacen en silencio: la segunda sobrescribe a la primera con los valores que leyo antes
     * del UPDATE de la otra. Con la version, el UPDATE lleva {@code WHERE version = ?} y si
     * otra instancia escribio en el medio Hibernate tira {@code OptimisticLockingFailureException}
     * en vez de pisar. Es lo que hace seguro el requisito de mas de un servicio de logistica.
     *
     * <p>La columna la crea sola {@code ddl-auto=update}.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "calle_1")
    private String calle1;

    @Column(name = "calle_2")
    private String calle2;

    @Column(name = "altura")
    private Integer altura;

    @Column(name = "sin_altura")
    private Boolean sinAltura;

    @Column(name = "piso")
    private Integer piso;

    @Column(name = "departamento")
    private String departamento;

    @ManyToOne
    @JoinColumn(name = "id_ciudad", referencedColumnName = "id_ciudad")
    private Ciudad ciudad;

    public Direccion(String calle1, String calle2, Integer altura, Integer piso, String departamento, String nombreCiudad, String nombreProvincia, String nombrePais){
        this.calle1 = calle1;
        this.calle2 = calle2;
        this.altura = altura;
        this.piso = piso;
        this.departamento = departamento;
        Pais pais = new Pais();
        pais.setNombre(nombrePais);
        Provincia provincia = new Provincia();
        provincia.setNombre(nombreProvincia);
        provincia.setPais(pais);
        this.ciudad = new Ciudad(nombreCiudad, provincia);
    }
}
