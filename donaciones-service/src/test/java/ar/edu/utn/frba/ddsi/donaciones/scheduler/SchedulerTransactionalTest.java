package ar.edu.utn.frba.ddsi.donaciones.scheduler;

import ar.edu.utn.frba.ddsi.donaciones.models.sheduler.ActividadDonanteScheduler;
import ar.edu.utn.frba.ddsi.donaciones.models.sheduler.AsignacionScheduler;
import ar.edu.utn.frba.ddsi.donaciones.services.DonanteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchedulerTransactionalTest {

  @Test
  @DisplayName("Punto 44: AsignacionScheduler.ejecutarAsignacion debe ser @Transactional para evitar LazyInitializationException")
  void asignacionScheduler_ejecutarAsignacion_tieneTransactional() throws NoSuchMethodException {
    Method method = AsignacionScheduler.class.getMethod("ejecutarAsignacion");
    assertTrue(method.isAnnotationPresent(Transactional.class),
        "ejecutarAsignacion debe estar anotado con @Transactional para mantener el EntityManager abierto en el hilo del scheduler");
  }

  @Test
  @DisplayName("Punto 44: ActividadDonanteScheduler.revisarActividadDonantes debe ser @Transactional")
  void actividadDonanteScheduler_revisarActividadDonantes_tieneTransactional() throws NoSuchMethodException {
    Method method = ActividadDonanteScheduler.class.getMethod("revisarActividadDonantes");
    assertTrue(method.isAnnotationPresent(Transactional.class),
        "revisarActividadDonantes debe estar anotado con @Transactional");
  }

  @Test
  @DisplayName("Punto 44: DonanteService.revisarActividades debe ser @Transactional para cargar formularios lazy")
  void donanteService_revisarActividades_tieneTransactional() throws NoSuchMethodException {
    Method method = DonanteService.class.getMethod("revisarActividades");
    assertTrue(method.isAnnotationPresent(Transactional.class),
        "revisarActividades debe estar anotado con @Transactional para acceder a la colección de formularios");
  }
}
