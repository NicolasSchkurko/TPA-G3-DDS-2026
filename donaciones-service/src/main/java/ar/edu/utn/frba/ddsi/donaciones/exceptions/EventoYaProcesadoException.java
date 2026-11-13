package ar.edu.utn.frba.ddsi.donaciones.exceptions;

/**
 * Señala que un evento de logística ya fue reclamado por otra instancia (o por una entrega
 * anterior del mismo mensaje): la fila de deduplicación ya existía al intentar insertarla.
 *
 * <p>El listener la trata como un caso exitoso (confirma el mensaje sin reprocesar), a
 * diferencia de cualquier otra {@code DataIntegrityViolationException} del procesamiento,
 * que sí se propaga para que el broker reintente.
 */
public class EventoYaProcesadoException extends RuntimeException {

  private final long idEvento;

  public EventoYaProcesadoException(long idEvento, Throwable causa) {
    super("El evento " + idEvento + " ya fue procesado por otra instancia", causa);
    this.idEvento = idEvento;
  }

  public long getIdEvento() {
    return idEvento;
  }
}
