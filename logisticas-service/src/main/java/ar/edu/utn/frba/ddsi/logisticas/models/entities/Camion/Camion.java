package ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "camiones")
@Getter
@Setter
@NoArgsConstructor
public class Camion {
    private Chofer chofer;

    @Id
    @Field(name = "patente")
    private String patente;

    @Version
    @Field(name = "version")
    private Long version;

    @Field(name = "capacidad_volumen_m3")
    private Double capacidadVolumen;

    @Field(name = "altura_m")
    private Double altura;

    @Field(name = "capacidad_carga_kg")
    private Double capacidadCarga;

    @Field(name = "disponible")
    private Boolean disponible;

    @Field(name = "peso_ocupado_kg")
    private Double pesoOcupado;

    @Field(name = "volumen_ocupado_m3")
    private Double volumenOcupado;

    @Field(name = "ciudad_destino_actual")
    private String ciudadDestinoActual;

    public Camion(Chofer chofer, String patente, Double capacidadVolumen, Double altura, Double capacidadCarga, Boolean disponible){
        this.chofer = chofer;
        this.patente = patente;
        this.capacidadVolumen = capacidadVolumen;
        this.altura = altura;
        this.capacidadCarga = capacidadCarga;
        this.disponible = disponible;
        this.pesoOcupado = 0.0;
        this.volumenOcupado = 0.0;
        this.ciudadDestinoActual = null;
    }

    public Camion(String patente, Double capacidadVolumen, Double altura, Double capacidadCarga, Boolean disponible){
        this.chofer = null;
        this.patente = patente;
        this.capacidadVolumen = capacidadVolumen;
        this.altura = altura;
        this.capacidadCarga = capacidadCarga;
        this.disponible = disponible;
        this.pesoOcupado = 0.0;
        this.volumenOcupado = 0.0;
        this.ciudadDestinoActual = null;
    }

    public boolean puedeCargar(Double pesoKg, Double volumenM3) {
        return pesoKg <= capacidadCarga && volumenM3 <= capacidadVolumen;
    }

    public boolean puedeCargar(ItemEntrega item) {
        return puedeCargar(this.pesoOcupado + item.getPesoEstimadoKg(),
                           this.volumenOcupado + item.getVolumenEstimadoM3());
    }

    public void cargar(ItemEntrega item, String ciudadDestino) {
        this.pesoOcupado += item.getPesoEstimadoKg();
        this.volumenOcupado += item.getVolumenEstimadoM3();
        this.ciudadDestinoActual = ciudadDestino;
    }

    public void resetearCargaOcupada() {
        this.pesoOcupado = 0.0;
        this.volumenOcupado = 0.0;
        this.ciudadDestinoActual = null;
    }

    public boolean estaVacio() {
        return this.pesoOcupado == 0.0 && this.volumenOcupado == 0.0;
    }

    public void ocupado() {
        this.disponible = false;
    }

    public void disponible() {
        this.disponible = true;
    }

    public void eliminarChofer() {
        this.chofer = null;
    }
}
