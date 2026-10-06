package ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "notificaciones")
@NoArgsConstructor
public class Notificacion {

    @Id
    private UUID id = UUID.randomUUID();

    /**
     * El mensaje al que apunta la notificación.
     *
     * <p><b>El cascade es obligatorio y tiene que ser ALL, no PERSIST.</b>
     *
     * <p>Sin cascade, el mensaje queda como entidad transient en memoria y la FK
     * {@code id_mensaje} de MySQL no tiene a qué apuntar: el INSERT del mensaje no se ejecutaba
     * y la notificación moría con violación de clave foránea. Se comprobó contra la base: la
     * FK existe y las dos tablas estaban vacías.
     *
     * <p>Por qué ALL y no PERSIST: el cascade PERSIST solo actúa en {@code persist()}, pero el
     * repositorio es un {@code JpaRepository} y su {@code save()} decide entre
     * {@code persist()} y {@code merge()} según tenga o no id. Como el UUID se asigna en el
     * constructor, la notificación siempre va por {@code merge()}, y ALL sí cascada el merge
     * del mensaje. Con PERSIST fallaba con {@code EntityNotFoundException} al buscar la
     * notificación por su mensaje recién guardado.
     *
     * <p>ALL además incluye {@code REMOVE}, que es lo correcto: el mensaje no tiene sentido
     * sin su notificación.
     */
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "id_mensaje", referencedColumnName = "id_mensaje")
    private Mensaje mensaje;

    @Column(name = "direccion_contacto", nullable = false)
    private String direccionDeContacto;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Cuándo se envió de verdad.
     *
     * <p><b>Es nullable a propósito.</b> Estaba marcada {@code nullable = false} y nunca se
     * asignaba en ningún constructor: una notificación nace PENDIENTE y no tiene fecha de
     * envío hasta que se envía. Con la restricción, el primer INSERT fallaba en MySQL con
     * "Field 'fechaEnvio' doesn't have a default value", que es un error de base de datos
     * mucho más difícil de leer que un null.
     */
    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoNotificacion estado;

    @Column(name = "tipo_medio_contacto", nullable = false)
    private String tipoMedioDeContacto;

    public Notificacion(String direccionDeContacto, Mensaje mensaje) {
        this(direccionDeContacto, null, mensaje);
    }

    /**
     * El constructor completo.
     *
     * <p>El merge empezó a llamar a uno de tres parámetros desde
     * {@code GestorNotificaciones} pero este constructor no existía, así que el módulo no
     * compilaba. Y el de dos parámetros hacía {@code this.tipoMedioDeContacto =
     * tipoMedioDeContacto}, o sea que se asignaba el campo a sí mismo: compilaba, pero dejaba
     * el medio de contacto siempre en null y el INSERT fallaba por la restricción
     * {@code nullable = false}. Los dos fallos juntos eran invisibles al leer el archivo.
     */
    public Notificacion(String direccionDeContacto, String tipoMedioDeContacto, Mensaje mensaje) {
        this.fechaCreacion = LocalDateTime.now();
        this.estado = EstadoNotificacion.PENDIENTE;
        this.direccionDeContacto = direccionDeContacto;
        this.tipoMedioDeContacto = tipoMedioDeContacto;
        this.mensaje = mensaje;
    }

    public void marcarEnviada() {
        this.estado = EstadoNotificacion.ENVIADA;
        this.fechaEnvio = LocalDateTime.now();
    }

    public void marcarFallida() {
        this.estado = EstadoNotificacion.FALLIDA;
    }

    public void marcarPendiente() {
        this.estado = EstadoNotificacion.PENDIENTE;
    }
}