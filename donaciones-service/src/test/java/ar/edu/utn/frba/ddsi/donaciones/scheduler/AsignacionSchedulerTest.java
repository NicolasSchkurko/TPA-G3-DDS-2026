package ar.edu.utn.frba.ddsi.donaciones.scheduler;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.frba.ddsi.donaciones.models.scheduler.AsignacionScheduler;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

@DisplayName("AsignacionScheduler")
class AsignacionSchedulerTest {

  @Test
  @DisplayName("ejecutarAsignacion debe ser @Transactional")
  void ejecutarAsignacionEsTransactional() throws NoSuchMethodException {
    Method method = AsignacionScheduler.class.getMethod("ejecutarAsignacion");
    assertTrue(method.isAnnotationPresent(Transactional.class),
        "ejecutarAsignacion debe mantener el EntityManager abierto en el hilo del scheduler");
  }
}
