package ar.edu.utn.frba.ddsi.donaciones.models.entities.logistica;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Registro de deduplicación de eventos de logística (Punto 75).
 *
 * <p>Cada fila representa un evento que ya fue procesado con éxito por este servicio.
 * La clave primaria es el id del evento ({@code id_evento}); al ser un {@code Long}
 * monotónico y compartido por todas las réplicas del clúster, la tabla sirve como
 * guardián de idempotencia duradero: sobrevive a los reinicios del servicio y no depende
 * del estado de memoria de cada contenedor.
 *
 * <p>La fila se inserta al comienzo de la transacción de procesamiento (ver {@code
 * RepositorioEventosProcesados.reclamar}). Si el evento falla, el rollback la borra y el
 * mecanismo de reintentos puede reprocesarlo; si la transacción commitea, la fila queda
 * como constancia de que el evento ya fue procesado con éxito.
 */
@Entity
@Table(name = "eventos_logistica_procesados")
@Getter
@Setter
public class EventoProcesado {

    /**
     * Clave primaria: el id del evento de logística.
     *
     * @see ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EventoLogisticaDTO
     */
    @Id
    @Column(name = "id_evento", nullable = false)
    private Long idEvento;

    /** Momento en que el evento quedó registrado como procesado. */
    @Column(name = "fecha_procesamiento")
    private LocalDateTime fechaProcesamiento;

    /** Constructor JPA (sin argumentos). */
    protected EventoProcesado() {
    }

    public EventoProcesado(Long idEvento, LocalDateTime fechaProcesamiento) {
        this.idEvento = idEvento;
        this.fechaProcesamiento = fechaProcesamiento;
    }
}
