package ar.edu.utn.frba.ddsi.donaciones.scheduler;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.frba.ddsi.donaciones.models.scheduler.ActividadDonanteScheduler;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

@DisplayName("ActividadDonanteScheduler")
class ActividadDonanteSchedulerTest {

  @Test
  @DisplayName("revisarActividadDonantes debe ser @Transactional")
  void revisarActividadDonantesEsTransactional() throws NoSuchMethodException {
    Method method = ActividadDonanteScheduler.class.getMethod("revisarActividadDonantes");
    assertTrue(method.isAnnotationPresent(Transactional.class),
        "revisarActividadDonantes debe ser @Transactional");
  }
}
