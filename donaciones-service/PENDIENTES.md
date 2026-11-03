# Pendientes técnicos de donaciones-service

Registro de problemas conocidos del servicio, con el motivo y la propuesta de arreglo para
que no se pierdan de vista al crecer el código.

**Están ordenados primero por prioridad (Severidad) y luego por nombre de paquete editado**, no por número de punto. El número es un
ID estable y no se renumera nunca, así que quedan huecos. Un punto corregido se borra de esta
lista y pasa a la sección [Corregidos](#corregidos) del final.

| #  | Punto | Severidad | Paquete | Por qué está acá |
|----|-------|-----------|---------|------------------|
| 1  | 42    | Media     | `models.entities` | Faltan "Lista para entregar" y "Entrega fallida" en el enum de estados |
| 2  | 62    | Media     | `models.entities` | `SegmentadorDonaciones` asigna `fechaEntrega` prematura en depósito falseando `SubAtendidos` y anulando la real |
| 3  | 63    | Media     | `models.entities` | Fallback de matchmaking sin intersección deja propuestas con posición duplicada y asigna propuesta equivocada |
| 4  | 64    | Media     | `models.gestores` | Donaciones que reingresan a depósito no pueden volver a pasar por matchmaking por colisión en `ResultadoMatchmaking` |
| 5  | 74    | Media     | `models.sheduler` | `AsignacionScheduler` transacciona todo el lote junto: un fallo en una donación tumba el batch por rollback-only |
| 6  | 51    | Media     | `services` | `EntidadBeneficiariaService` sin transacciones: altas y bajas de necesidades quedan a medias |
| 7  | 53    | Media     | `services` | La asignación notifica dos veces: mensaje directo y estrategia de notificación |
| 8  | 54    | Media     | `services` | `DELETE /donaciones/{id}` responde 409 si la donación tiene resultado de matchmaking |
| 9  | 65    | Media     | `services` | `AdminService.actualizarAdmin` inserta una nueva `Humana` en cada PUT dejando huérfana la persona anterior |
| 10 | 36    | Baja      | `arquitectura` | La nomenclatura de subcarpetas no coincide con la de incentivos |
| 11 | 39    | Baja      | `controllers` | `DonacionController` inyecta `RabbitTemplate` sin usarlo |
| 12 | 43    | Baja      | `controllers` | "Consultar rankings" no está expuesto en donaciones: confirmar alcance |
| 13 | 57    | Baja      | `dto` | Los POST de medios de contacto aceptan nulos/vacíos y responden 201 |
| 14 | 37    | Baja      | `models.repositories` | Los repositorios usan dos convenciones distintas entre servicios |
| 15 | 67    | Baja      | `models.sheduler` | `ActividadDonanteScheduler` evalúa formularios en orden arbitrario y dispara spam diario perpetuo a inactivos |
| 16 | 68    | Baja      | `services` | `GET /api/entidades/{id}/donaciones` responde 200 con lista vacía en lugar de 404 si la entidad no existe |


---


## 42. Faltan "Lista para entregar" y "Entrega fallida" en el enum de estados

**Estado:** abierto

**Severidad:** media

**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/Donaciones/Estado.java`, `src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/gestores/GestorEventosLogistica.java` (`manejarEntregaFallida`)

### Qué pasa

El enunciado pide trazar: "En depósito", "Asignación realizada", "Lista para entregar", "En traslado", "Entregada", "Entrega fallida" y "Vencida". El enum hoy es `EN_DEPOSITO`, `PENDIENTE_ASIGNACION`, `EN_TRASLADO`, `ASIGNADO`, `ENTREGADO`, `VENCIDO`, y cubre 5 de 7:

- **"Lista para entregar"** no tiene valor que lo represente: entre `ASIGNADO` y `EN_TRASLADO` el estado de la donación no cambia.

- **"Entrega fallida"** no es un estado: `manejarEntregaFallida` devuelve la donación a `EN_DEPOSITO` con una justificación (queda registrado en el historial).

La auditoría/trazabilidad persistente sí existe: `Donacion` guarda el historial de estados en la tabla `donacion_historial_estados` (`@ElementCollection`).

### Propuesta

Definir el mapeo con el equipo: agregar `LISTA_PARA_ENTREGAR` (al planificar la ruta) y/o `ENTREGA_FALLIDA` como valores del enum, o documentar que `ASIGNADO` cubre "lista para entregar" y que la fallida vuelve a depósito a propósito.


---


## 62. `SegmentadorDonaciones` asigna `fechaEntrega` prematura en depósito falseando `SubAtendidos` y anulando la real

**Estado:** abierto

**Severidad:** media

**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/SegmentadorDonaciones/SegmentadorDonaciones.java` (líneas 62-74), `.../models/entities/Donaciones/Donacion.java` (líneas 114-116), `.../models/entities/AsignadorDonaciones/AlgoritmosDeAsignacion/SubAtendidos.java` (líneas 64-70)

### Qué pasa

Al registrar un formulario de donación, `SegmentadorDonaciones.crearDonacion` asigna la fecha del formulario directamente al campo `fechaEntrega` de cada segmento creado:

```java
return new Donacion(

    donante,

    null,

    "Segmento de donación: " + sub.getNombre(),

    bienesDelGrupo,

    Estado.EN_DEPOSITO,

    sub,

    fechaEntrega // <-- Recibe formulario.getFechaRealizacion()

);
```

Esto genera dos inconsistencias graves:

1. `SubAtendidos.cantidadDonacionesUltimoTrimestre` cuenta donaciones recibidas filtrando por `d.getFechaEntrega() != null && d.getFechaEntrega().isAfter(haceUnTrimestre)`, **sin validar `d.getEstado() == Estado.ENTREGADO`**. Cualquier donación en depósito o en traslado ya suma como donación entregada para la entidad, penalizándola en el ranking injustamente.

2. En `Donacion.actualizarEstado`:

   `if (nuevoEstado == Estado.ENTREGADO && this.fechaEntrega == null) { this.fechaEntrega = LocalDate.now(); }`

   Como `this.fechaEntrega` ya fue poblada en la creación, esta condición nunca se cumple. La fecha de entrega real nunca se registra y queda congelada con la fecha de carga del formulario, distorsionando las ventanas de `NecesidadRecurrente.cantidadRecibidaEnPeriodo`.

### Cómo se dispara

Crear una donación vía `POST /api/donaciones/formulario` y consultar el ranking de matchmaking o el cálculo de `SubAtendidos` antes de que la donación haya sido entregada.

### Propuesta de arreglo

Instanciar la donación con `fechaEntrega = null` en `SegmentadorDonaciones.crearDonacion`, y verificar `d.getEstado() == Estado.ENTREGADO` en `SubAtendidos.cantidadDonacionesUltimoTrimestre`.


---


## 63. Fallback de matchmaking sin intersección deja propuestas con posición duplicada y asigna propuesta equivocada

**Estado:** abierto

**Severidad:** media

**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/AsignadorDonaciones/AsignadorDonaciones.java` (líneas 78-85), `.../models/gestores/GestorMatchmaking.java` (líneas 29-39)

### Qué pasa

En `AsignadorDonaciones.procesarMatchmaking`, si no hay intersección entre algoritmos (`huboCoincidenciaTotal == false`), se ejecuta:

```java
todasLasPropuestas.sort(Comparator.comparingInt(PropuestaAsignacion::getPosicion));

resultadoFinal = todasLasPropuestas;
```

A diferencia de la rama con intersección (donde se reasigna `setPosicion(i + 1)`), en esta rama cada algoritmo aportó propuestas numeradas `1..N`. La lista resultante contiene múltiples elementos con `posicion == 1`, múltiples con `posicion == 2`, etc.

`GestorMatchmaking.obtenerPropuestaSeleccionadaParaDonacion` asume que el parámetro `posicion` recibido en `POST /donaciones/asignar` corresponde al índice de la lista:

```java
PropuestaAsignacion propuesta = resultado.getPropuestasOrdenadas().get(posicion - 1);
```

Si el operador visualiza en la interfaz una propuesta con posición 2 (proveniente de `CompatibilidadSemantica`) y envía `posicionPropuesta = 2`, el código accede al índice 1 de la lista, que corresponde a la propuesta de `SubAtendidos` (cuya posición original era 1). El sistema aprueba y asigna una propuesta completamente distinta a la seleccionada.

### Cómo se dispara

1. Se corre matchmaking para una donación donde `CompatibilidadSemantica` y `SubAtendidos` recomiendan entidades distintas (`huboCoincidenciaTotal = false`).

2. `GET /donaciones/pendientes` devuelve la lista con posiciones repetidas (`1, 1, 2, 2...`).

3. Se invoca `POST /donaciones/asignar` con `posicionPropuesta = 2`.

4. Se asigna la entidad del índice 1 (que tiene posición 1 de otro algoritmo) en lugar de la elegida.

### Propuesta de arreglo

Renumerar secuencialmente las posiciones de `resultadoFinal` (`setPosicion(i + 1)`) en la rama `else` de `AsignadorDonaciones.procesarMatchmaking`, o permitir seleccionar la propuesta por un identificador unívoco (ID de necesidad/entidad).


---


## 64. Donaciones que reingresan a depósito no pueden volver a pasar por matchmaking por colisión en `ResultadoMatchmaking`

**Estado:** abierto

**Severidad:** media

**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/repositories/repos/RepositorioDeResultadosMatchmaking.java` (líneas 28-36), `.../models/entities/AsignadorDonaciones/AsignadorDonaciones.java` (líneas 178-192), `.../models/gestores/GestorEventosLogistica.java` (líneas 131 y 151)

### Qué pasa

Cuando una donación pasa por matchmaking, se persiste un `ResultadoMatchmaking` en la base de datos. Si la donación reingresa a depósito (por entrega fallida en `GestorEventosLogistica.manejarEntregaFallida` / `manejarReingresoDeposito`, o por reseteo manual de estado a `EN_DEPOSITO`), el `ResultadoMatchmaking` anterior no se elimina (únicamente se borra en `DonacionService.asignarPropuesta`).

Al volver a correr el matchmaking (`AsignacionScheduler` o `POST /matchmaking/ejecutar`), `buscarDonacionesSinAsignar()` incluye la donación en `EN_DEPOSITO`. Pero al persistir el nuevo resultado, `RepositorioDeResultadosMatchmaking.guardar` valida:

```java
if (findByDonacionId(resultado.getDonacion().getId()).isPresent()) {

    throw new IllegalArgumentException("Ya existe un resultado de matchmaking para la donación: " + resultado.getDonacion().getId());

}
```

La excepción es capturada en `AsignadorDonaciones.ejecutarMatchmakingBatch`, imprimiendo el error en consola y omitiendo la donación. La donación queda varada en `EN_DEPOSITO` permanentemente sin poder generar propuestas nuevas, y `GET /donaciones/pendientes` sigue sirviendo las propuestas obsoletas del ciclo anterior.

### Cómo se dispara

1. Una donación es asignada y enviada a entrega.

2. La entrega falla (`ENTREGA_FALLIDA`) o la donación reingresa a depósito (`REINGRESO_DEPOSITO`).

3. El scheduler de matchmaking intenta procesar la donación.

4. Falla con `IllegalArgumentException` por clave única duplicada y nunca más se vuelve a procesar.

### Propuesta de arreglo

En `AsignadorDonaciones.registrarDonacionPendienteDeAprobacion` (o en `RepositorioDeResultadosMatchmaking`), borrar o actualizar el resultado previo si ya existía para esa donación antes de insertar el nuevo.


---


## 74. `AsignacionScheduler` transacciona todo el lote junto: un fallo en una donación tumba el batch por rollback-only

**Estado:** abierto

**Severidad:** media

**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/sheduler/AsignacionScheduler.java` (línea 37), `.../models/entities/AsignadorDonaciones/AsignadorDonaciones.java` (líneas 51-57)

### Qué pasa

`AsignacionScheduler.ejecutarAsignacion()` está anotado con `@Transactional(rollbackFor = Exception.class)`. Dentro, `AsignadorDonaciones.ejecutarMatchmakingBatch` recorre las donaciones en un bucle `for` con un `try / catch`:

```java
for (Donacion donacion : todasLasDonaciones) {

    try {

        procesarMatchmaking(donacion, todasLasEntidades);

    } catch (Exception e) {

        System.err.println("Error al procesar matchmaking para la donación...: " + e.getMessage());

    }

}
```

El comentario asume que aislar con `try/catch` permite continuar con el resto del lote si una donación falla. Sin embargo, al estar bajo una transacción única de Spring/Hibernate, si una donación dispara una excepción a nivel base de datos (por ejemplo, violación de clave única en `ResultadoMatchmaking` o constraint de FK), Hibernate marca la sesión y la transacción física como **rollback-only**. Cuando el scheduler termina, Spring intenta hacer commit y arroja `UnexpectedRollbackException`, descartando todas las asignaciones exitosas del lote completo.

### Cómo se dispara

Cualquier corrida programada donde al menos una donación sufra un error de integridad o colisión en base de datos.

### Propuesta de arreglo

Remover `@Transactional` del método del scheduler y encapsular el procesamiento de cada donación individual en una transacción independiente mediante `TransactionTemplate` o un método de servicio con `@Transactional(propagation = Propagation.REQUIRES_NEW)`.


---


## 51. `EntidadBeneficiariaService` sin transacciones: altas y bajas de necesidades quedan a medias

**Estado:** abierto

**Severidad:** media

**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/EntidadBeneficiariaService.java` (líneas 82-88 y 105-109), `.../models/repositories/repos/RepositorioEntidadesBeneficiarias.java` (líneas 75-84 y 86-98), `.../models/repositories/repos/RepositorioNecesidades.java` (líneas 67-69), `.../models/entities/Donaciones/Donacion.java` (líneas 46-49)

### Qué pasa

El `@Transactional` del punto 8 se agregó solo a `DonacionService`; `EntidadBeneficiariaService` sigue sin ninguno. Dos síntomas del mismo patrón:

- **Alta:** `POST /entidades/{idInexistente}/necesidades` → `crearNecesidad` commitea la necesidad primero; después `agregarNecesidadAEntidad` no encuentra la entidad y tira `IllegalArgumentException` → 400. Queda una `Necesidad` huérfana (más la subcategoría creada) que ningún listado muestra.

- **Baja:** `DELETE /entidades/{id}/necesidades/{idNecesidad}` sobre una necesidad con donaciones asignadas → `eliminarNecesidadDeEntidad` **desvincula y commitea**; después `repositorioNecesidades.eliminarPorId` choca con la FK `necesidad_id` (`Donacion.necesidad` no tiene cascade) → 409. La necesidad queda viva pero sin entidad: desaparece de `GET /entidades/{id}/necesidades`, `verDonaciones()` deja de ver esas donaciones y el matchmaking no puede proponerla. No hay endpoint para re-vincularla.

### Por qué no se ve

Los dos endpoints responden códigos que suenan a validación (400/409) y nadie mira la base; la necesidad huérfana es invisible para las consultas normales.

### Propuesta

`@Transactional(rollbackFor = Exception.class)` en los métodos de escritura del servicio (agregar/eliminar necesidad, registrar/actualizar entidad); y en la baja, validar que la necesidad se puede borrar **antes** de desvincularla (o borrar en el orden inverso).


---


## 53. La asignación notifica dos veces: mensaje directo y estrategia de notificación

**Estado:** abierto

**Severidad:** media

**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/DonacionService.java` (líneas 170-181), `.../models/gestores/GestorAsignaciones.java` (líneas 38-42 y 122-137), `.../models/entities/ServicioMensaje/EstrategiasMensajes/NotificacionDonacionAsignada.java` (líneas 26-56), `.../clients/NotificacionesClient.java` (líneas 29-40)

### Qué pasa

Por cada `POST /donaciones/asignar` salen **dos** notificaciones por destinatario:

1. `gestorAsignaciones.asignarPropuesta` termina en `notificarAsignacion`, que publica un `NotificacionDTO` directo al medio predeterminado de la entidad y otro al del donante (líneas 130 y 134).

2. Inmediatamente después, `DonacionService.asignarPropuesta` llama `cambiarEstado(..., "ASIGNADO", ...)` → `procesarAccionesPostCambioEstado` → `fabricaEstrategiasNotificacion.ejecutar(DONACION_ASIGNADA, donacion)` → `NotificacionDonacionAsignada` vuelve a publicar el aviso a los mismos destinatarios (todos sus medios).

No hay deduplicación: son dos mensajes distintos con textos distintos al mismo exchange.

### Por qué no se ve

Las dos publicaciones "funcionan"; el usuario recibe el aviso repetido y se lee como ruido, no como bug.

### Propuesta

Dejar un solo camino de notificación: que `asignarPropuesta` no notifique y lo haga el cambio de estado, o que `procesarAccionesPostCambioEstado` no re-ejecute `DONACION_ASIGNADA` cuando el flujo ya notificó.


---


## 54. `DELETE /donaciones/{id}` responde 409 si la donación tiene resultado de matchmaking

**Estado:** abierto

**Severidad:** media

**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/DonacionService.java` (líneas 150-153), `.../models/repositories/repos/RepositorioDonaciones.java` (líneas 76-78), `.../models/entities/AsignadorDonaciones/ResultadoMatchmaking.java` (líneas 28-30), `.../exceptions/GlobalExceptionHandler.java` (líneas 52-57)

### Qué pasa

`eliminarDonacion` solo hace `repositorioDonaciones.eliminarPorId(id)`. Pero `ResultadoMatchmaking.donacion` es un `@ManyToOne` con FK `donacion_id` y **sin** cascade REMOVE, y toda donación que pasó por matchmaking tiene su fila en `resultado_matchmaking`. El delete viola la FK → `DataIntegrityViolationException` → **409**. El resultado solo se borra al aprobar la asignación (`DonacionService.asignarPropuesta`); no hay endpoint ni job que limpie resultados de donaciones borradas/varadas.

### Cómo se dispara

`POST /donaciones/matchmaking/ejecutar` (o el scheduler, cuando funcione) sobre una donación en depósito, y después `DELETE /donaciones/{id}`.

### Por qué no se ve

Hasta la primera corrida de matchmaking el DELETE funciona; después falla "sin motivo visible" y el front no tiene forma de distinguirlo.

### Propuesta

Borrar o desvincular el `ResultadoMatchmaking` dentro de `eliminarDonacion` (en una transacción), o darle un cascade explícito al vínculo.


---


## 65. `AdminService.actualizarAdmin` inserta una nueva `Humana` en cada PUT dejando huérfana la persona anterior

**Estado:** abierto

**Severidad:** media

**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/AdminService.java` (líneas 61-71), `.../models/entities/administrador/Administrador.java` (líneas 28-30)

### Qué pasa

En `AdminService.actualizarAdmin`:

```java
Administrador datosNuevos = dto.toDomain();

if (existente.getHumano() != null && datosNuevos.getHumano() != null) {

    repositorioPersonas.modificarPersona(existente.getHumano().getId(), datosNuevos.getHumano());

} else if (datosNuevos.getHumano() != null) {

    repositorioPersonas.registrarPersona(datosNuevos.getHumano());

    existente.setHumano(datosNuevos.getHumano());

}

existente.setHumano(datosNuevos.getHumano());
```

A pesar de que el primer `if` actualiza los datos de la `Humana` existente en `repositorioPersonas`, la línea 68 reasigna incondicionalmente `existente.setHumano(datosNuevos.getHumano())`. `datosNuevos.getHumano()` es una nueva instancia generada por `dto.toDomain()` con un UUID recién generado.

Al persistir `existente`, debido al `cascade = {CascadeType.PERSIST, CascadeType.MERGE}` de `Administrador.humano`, Hibernate inserta una fila nueva en las tablas `personas` y `humana`. La fila anterior actualizada queda huérfana en la base de datos. Cada llamada a `PUT /api/admins/{id}` acumula registros huérfanos.

### Cómo se dispara

Invocar `PUT /api/admins/{id}` sobre un administrador existente.

### Propuesta de arreglo

Eliminar la línea redundante `existente.setHumano(datosNuevos.getHumano())` para mantener la instancia de `Humana` vinculada al administrador cuando ya existía.


---


## 36. La nomenclatura de subcarpetas no coincide con la de incentivos

**Estado:** abierto

**Severidad:** baja

**Archivos:** todo el árbol de `src/main/java/ar/edu/utn/frba/ddsi/donaciones/` (paquetes `dto/`, `models/sheduler`, `messaging/` y `RabbitMQ/`, `exceptions/`)

### Qué pasa

La idea es que donaciones e incentivos tengan la misma estructura; hoy no coinciden en varios niveles:

- **DTOs.** Donaciones usa camelCase (`dto/admin`, `dto/donaciones`, `dto/personaDonante`, `dto/entidadBeneficiaria`, ...) y deja cuatro DTOs sueltos en la raíz de `dto/` (`AsignarPropuestaRequestDTO`, `DireccionDTO`, `PropuestaAsignacionDTO`, `ResultadoMatchmakingDTO`). Incentivos usa PascalCase (`dto/Admin`, `dto/Perfil`, `dto/Persona`, `dto/Notificaciones`) y tiene `controllers/request/` para los filtros; donaciones no tiene ese subpaquete.

- **Schedulers.** Donaciones los tiene en `models/sheduler` (con el typo en el nombre); incentivos en `models/ServiciosInternos/scheduler/`.

- **Mensajería.** Donaciones tiene dos paquetes al mismo nivel para la integración (`messaging/` y `RabbitMQ/`); incentivos no tiene ninguno: resuelve todo en `clients/`.

- **Excepciones.** `exceptions/CsvExceptions` rompe la convención en minúscula del resto.

Lo que **sí** coincide, y conviene preservar: `models/gestores` divididos por **comportamiento** y no por entidad. Donaciones: `GestorAsignaciones`, `GestorFormulario`, `GestorMatchmaking`, `GestorEventosLogistica`. Incentivos: `SecuenciaCategoria`, `SincronizacionPerfiles`, `ValidadorAdmin`. Las entidades también están agrupadas por entidad (`models/entities/<Entidad>/...`) en los dos servicios.

### Propuesta

Fijar una única convención de nombres de subcarpetas (minúscula, un solo paquete de mensajería, `scheduler` bien escrito, DTOs agrupados por dominio sin sueltos en la raíz) y aplicarla en donaciones para espejar incentivos.


---


## 39. `DonacionController` inyecta `RabbitTemplate` sin usarlo

**Estado:** abierto

**Severidad:** baja

**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/controllers/DonacionController.java`

### Qué pasa

El controller declara `private final RabbitTemplate rabbitTemplate` y lo recibe por constructor, pero no lo usa en ningún método: es una inyección muerta. Además deja la infraestructura de mensajería en la capa de controllers. En incentivos ningún controller conoce el broker: los controllers solo inyectan services, y las publicaciones pasan por services/clients.

### Propuesta

Quitar la dependencia del controller. Si en algún momento hace falta publicar desde un endpoint, que salga por el service correspondiente (o por `messaging/`), no por el controller.


---


## 43. "Consultar rankings" no está expuesto en donaciones: confirmar alcance

**Estado:** abierto

**Severidad:** baja

**Archivos:** controllers de donaciones (no hay endpoint)

### Qué pasa

El enunciado del servicio incluye, en la exposición REST, "permite consultar rankings". En donaciones no hay ningún endpoint de rankings: el ranking de donantes vive en `incentivos-service` (`GET /api/rankings/...`), y lo más cercano acá es `GET /api/donaciones/pendientes`, que devuelve las propuestas de matchmaking ordenadas (con posición y score) pero no un ranking.

### Propuesta

Definir si la consulta queda delegada en incentivos-service (dejarlo documentado) o si donaciones debería exponer un ranking propio (p. ej. el de sub-atendidos que ya calcula el matchmaking).


---


## 57. Los POST de medios de contacto aceptan nulos/vacíos y responden 201

**Estado:** abierto

**Severidad:** baja

**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/dto/notificaciones/MediosContactoDTO.java` (líneas 24-32), `.../services/DonanteService.java` (líneas 227-232), `.../controllers/DonanteController.java` (líneas 136-144), `.../models/entities/Mensaje/MedioDeContacto/MediosDeContacto.java` (líneas 42-44)

### Qué pasa

`MediosContactoDTO.toDomain()` devuelve `null` si `tipo` o `valor` vienen null, y no valida formato ni contenido: `agregarMedioContacto` pasa ese resultado a `agregarMedioDeContacto`, que agrega a la lista **sin guarda de null**, y el endpoint responde **201** aunque no se haya agregado nada. Con `{"tipo":"EMAIL","valor":""}` se persiste un `Mail` vacío: el fallo aparece recién en el próximo envío (`ServicioNotificaciones` valida "no vacío" al enviar, no al guardar).

### Cómo se dispara

- `POST /api/personas/{id}/medios-contacto` con `{}` → 201 sin agregar nada.

- `POST /api/personas/{id}/medios-contacto` con `{"tipo":"EMAIL","valor":""}` → 201 con un email vacío persistido.

### Por qué no se ve

La respuesta trae el donante serializado y los `null` desaparecen del JSON; el problema real aparece un salto después, al notificar.

### Propuesta

Validar en el DTO (`@NotBlank` en valor, tipo soportado) y rechazar en `agregarMedioDeContacto` el `null`; alinear la validación de contenido con la que ya hace `ServicioNotificaciones`.


---


## 37. Los repositorios usan dos convenciones distintas entre servicios

**Estado:** abierto

**Severidad:** baja

**Archivos:** `donaciones-service/src/main/java/.../models/repositories/` y `incentivos-service/src/main/java/.../models/repositories/`

### Qué pasa

Los dos servicios persisten con Spring Data, pero con convenciones distintas:

- **Incentivos:** las interfaces (`RepositorioX extends JpaRepository`) viven en `models/repositories/SpringRepositories/` y los services las usan directo, sin fachada. Queda mezclada ahí `RepositorioNotificacionesPendientes`: es una **clase** con un buffer en memoria (no es persistencia) en la raíz de `repositories/`.

- **Donaciones:** las interfaces (`XJpaRepository`) viven en `models/repositories/interfaces/` y sobre ellas hay **fachadas de clase** (`RepositorioX` con `@Repository`) en `models/repositories/repos/`, que son las que usan services y gestores. Son 14 fachadas para 14 entidades, casi todas delegando directo en la interfaz.

Ninguna de las dos es incorrecta: el problema es que no son la misma, y que la fachada obliga a mantener un archivo extra por entidad.

### Propuesta

**Decisión tomada (2026-10-09): sin fachadas.** Donaciones pasa a que services y gestores usen las interfaces `XJpaRepository` de `models/repositories/interfaces/` directo (como incentivos) y se eliminan las 14 fachadas de `models/repositories/repos/`; incentivos queda como está. Queda como trabajo pendiente la migración y el borrado de las fachadas. Aparte: mover el buffer de notificaciones pendientes de incentivos fuera de `repositories`, porque no es un repositorio de persistencia.


---


## 67. `ActividadDonanteScheduler` evalúa formularios en orden arbitrario y dispara spam diario perpetuo a inactivos

**Estado:** abierto

**Severidad:** baja

**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/DonanteService.java` (líneas 315-321), `.../models/entities/donador/Donante.java` (líneas 30-31), `.../models/sheduler/ActividadDonanteScheduler.java` (líneas 16-20)

### Qué pasa

`DonanteService.revisarActividades` evalúa la inactividad mediante:

```java
p.getFormularios().getLast().getFechaRealizacion().plusDays(20).isBefore(LocalDate.now())
```

1. `Donante.formularios` es un `@OneToMany` sin `@OrderBy`: el orden en que Hibernate llena la lista es arbitrario (depende del orden de lectura o UUID de la tabla `formulario`), por lo que `getLast()` no garantiza ser el formulario más reciente.

2. Cada vez que el cron se ejecuta (`0 0 0 * * ?` todas las noches), todo donante con más de 20 días de inactividad vuelve a cumplir la condición `isBefore(LocalDate.now())`. No existe marca de notificación enviada ni control de frecuencia, por lo que el donante recibe una notificación de inactividad todas las noches de forma indefinida.

### Cómo se dispara

Dejar transcurrir 21 días desde la última donación de un donante y esperar las corridas diarias del scheduler a medianoche.

### Propuesta de arreglo

Ordenar los formularios por `fechaRealizacion` de forma descendente (o via `@OrderBy`) y registrar una fecha o flag de `ultimaNotificacionInactividad` para evitar renotificar a diario.


---


## 68. `GET /api/entidades/{id}/donaciones` responde 200 con lista vacía en lugar de 404 si la entidad no existe

**Estado:** abierto

**Severidad:** baja

**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/EntidadBeneficiariaService.java` (líneas 111-113 y 124-132), `.../controllers/EntidadBeneficiariaController.java` (líneas 128-136)

### Qué pasa

En `EntidadBeneficiariaService.java`:

```java
private List<Donacion> obtenerDonacionesDeEntidad(UUID idEntidad) {

    EntidadBeneficiaria entidad = repositorioEntidadesBeneficiarias.buscarPorId(idEntidad).orElse(null);

    if (entidad != null) {

        return entidad.verDonaciones();

    } else {

        System.err.println("Entidad no encontrada.");

        return new ArrayList<>();

    }

}
```

A diferencia de `obtenerEntidadPorId` o `obtenerNecesidades`, si la entidad no existe, `obtenerDonacionesDeEntidad` atrapa la ausencia imprimiendo en `System.err` y retorna una lista vacía. El controller responde **200 OK** con `[]`. El cliente HTTP no puede distinguir si la entidad existe y no tiene donaciones, o si se consultó un UUID inexistente.

### Cómo se dispara

Enviar un `GET /api/entidades/{uuid-inexistente}/donaciones`.

### Propuesta de arreglo

Lanzar `IllegalArgumentException` o `EntityNotFoundException` cuando `buscarPorId(idEntidad)` esté vacío, de forma consistente con el resto de los métodos del servicio.


---


## Corregidos

- **75.** El cursor de deduplicación de eventos `ultimoIdProcesado` en `EventosListener` era en memoria: no sobrevivía reinicios ni clusters - reemplazado por la tabla de idempotencia `eventos_logistica_procesados` (clave primaria `id_evento`, vía `RepositorioEventosProcesados`). El reclamo se inserta al comienzo de la transacción de `GestorEventosLogistica.procesarEvento` (`persist` + `flush`), así que la verificación y la inserción son atómicas: si el evento falla, el rollback borra la fila y el retry lo reprocesa; si otra réplica ya lo reclamó, la PK duplicada se traduce a `EventoYaProcesadoException` y el listener confirma el mensaje sin reprocesar. La deduplicación es duradera y compartida entre todas las réplicas, sin alterar el contrato del Punto 45. Coberturas en `EventosListenerTest` y `GestorEventosLogisticaTest`.

- **55.** El catch-all del `GlobalExceptionHandler` convertía errores de cliente en 500 y filtraba internals - agregados handlers para `HttpMessageNotReadableException` y `MethodArgumentTypeMismatchException` (400), `HttpRequestMethodNotSupportedException` (405) y `NoResourceFoundException` (404), y el catch-all ahora responde un mensaje genérico y loguea el detalle.

- **38.** Los listados devuelven la colección entera: falta paginación - Se sumó paginación mediante `Pageable` (`@PageableDefault(page = 0, size = 10)`) y retorno de `Page<DTO>` en los 5 listados principales (`GET /api/donaciones`, `GET /api/donaciones/formularios`, `GET /api/donaciones/pendientes`, `GET /api/personas`, `GET /api/entidades`), implementando `findByActivoTrue` y `findByActivaTrue` en los repositorios Spring Data JPA para paginación a nivel SQL. Se adaptó el cliente consumidor en `front-service` (`DonacionesClient` mediante `PageResponse`) y los scripts de la colección de Postman para procesar tanto respuestas paginadas (`content`) como listas planas.
- **56.** Los PUT de persona, entidad y admin traducen validación a 404 mudo - Se removieron los bloques `catch (IllegalArgumentException e)` locales en `DonanteController`, `EntidadBeneficiariaController` y `AdminController`, permitiendo que el `GlobalExceptionHandler` mapee las excepciones de validación de negocio y DTOs a HTTP 400 Bad Request. En los servicios (`DonanteService`, `EntidadBeneficiariaService`, `AdminService`), la no existencia del recurso por ID lanza `EntityNotFoundException` para responder 404 Not Found estricto.
- **49.** El `RestTemplate` de integración no tenía timeouts: un servicio colgado bloqueaba hilos y listeners — configurados timeouts explícitos en `DonacionesServiceApplication.restTemplate` utilizando `HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(connectTimeout))` (default 5s) y `JdkClientHttpRequestFactory.setReadTimeout(Duration.ofSeconds(readTimeout))` (default 10s), parametrizables mediante propiedades `http.client.connect-timeout-seconds` y `http.client.read-timeout-seconds` en `application.properties`.
- **50.** La publicación a logística iba dentro de la transacción SQL y a un exchange que este servicio no declaraba — declarado el `CustomExchange` (`x-consistent-hash` con argumento `hash-header: x-id-donacion`) en `RabbitMQConfig.exchangeIntegracionHash` para que el servicio pueda publicar de forma autónoma aunque logística aún no haya levantado, y movida la llamada a `publicarEntregaALogistica` en `DonacionService.asignarPropuesta` para ejecutarse post-commit mediante `TransactionSynchronizationManager.registerSynchronization` (`afterCommit`), evitando enviar mensajes prematuros por estados revertidos ante fallos de base de datos o abortar asignaciones confirmadas por caídas del broker.
- **52.** El alta de donante creaba el perfil en incentivos y notificaba antes de persistir localmente — `crearPersona` anotado con `@Transactional(rollbackFor = Exception.class)`, primero se persisten `Persona` y `Donante` en la base de datos local, y recién tras el commit exitoso (`afterCommit` de `TransactionSynchronizationManager`) se crea el perfil en incentivos y se dispara la notificación con aislamiento en `try/catch`. En la importación CSV (`persistirConNotificacion`), la persistencia local también precede a la notificación.

- **72.** `DonanteService.buscarPorNombre` escaneaba toda la tabla de donantes en memoria — agregada consulta JPQL optimizada en `DonanteJpaRepository` (`buscarPorNombreActivo`) con `TYPE` y `TREAT` para `Humana` (nombre, apellido, combinación) y `Juridica` (razón social) filtrando donantes activos directo en el motor de base de datos, delegada desde `RepositorioDonantes.buscarPorNombre` sin traer toda la colección a la memoria heap de la JVM.

- **73.** Fuga de memoria progresiva en `DonanteService.reportesImportacion` — acotado el mapa en memoria de reportes a un máximo de 100 elementos con política de desalojo LRU (`LinkedHashMap` sincronizado con `removeEldestEntry`), eliminando la retención indefinida de cadenas de error y reportes completados.

- **41.** La modificación de donante (`PUT`) ignoraba campos de personas jurídicas — `RepositorioPersonas.modificarPersona` actualiza ahora exhaustivamente `rubro`, `tipoJuridico` y sincroniza la colección de `representantes` (reutilizando y limpiando la lista existente para no violar `orphanRemoval = true`), persistiendo los cambios al modificar entidades jurídicas.

- **71.** `executorImportacion` usa hilos daemon: un apagado o reinicio del contenedor abortaba la importación CSV a medias — migrado el pool a `ThreadPoolTaskExecutor` de Spring con hilos no-daemon (`setDaemon(false)`), espera de tareas activas al apagar (`setWaitForTasksToCompleteOnShutdown(true)`) y ventana de gracia de 30 segundos (`setAwaitTerminationSeconds(30)`), exponiendo `getThreadPoolExecutor()` como bean `ExecutorService` para preservar la inyección requerida por `DonanteService` sin que Spring destruya el `DataSource` ni los recursos de BD mientras hay importaciones en curso.

- **61.** Llamada síncrona a `IncentivosClient` en transacción de entrega revertía la persistencia y descartaba el evento de logística — aislada la llamada a `incentivosClient.notificarImpactoDonacion` para ejecutarse post-commit mediante `TransactionSynchronizationManager` con `afterCommit()` (o fallback directo aislado en `try/catch` con log de advertencia), evitando que caídas o demoras de incentivos reviertan la confirmación local de la donación a `ENTREGADO` y previniendo el descarte irreversible del evento en RabbitMQ.

- **70.** Soft Delete en `EntidadBeneficiaria` y cancelación lógica de necesidades, evitando 409 y preservando donaciones — implementada baja lógica (`activa = false`, `fechaBaja = now()`) en `EntidadBeneficiaria`, cancelación lógica de necesidades (`activa = false` en `Necesidad`), anonimización de persona jurídica y dirección, exclusión de entidades y necesidades inactivas en matchmaking (`CompatibilidadSemantica`, `SubAtendidos`, `AsignacionScheduler`, `DonacionService`) y listados, y eliminación del borrado físico en `EntidadBeneficiariaService.eliminarEntidad` y `eliminarNecesidad` para evitar el 409 por restricción de clave foránea (absorbió bug 66).

- **69.** Soft Delete y minimización de datos en `Donante` y `Persona`, eliminando desvinculación a `null` de donaciones — implementada baja lógica (`activo = false`, `fechaBaja = now()`) en `Donante`, anonimización en cascada (`Persona.anonimizar()`, `Humana`, `Juridica`, `Direccion`, `MediosDeContacto.limpiarMedios()`), filtrado de inactivos en `DonanteService.listarTodas()` y `buscarPorNombre()`, y eliminadas las desvinculaciones destructivas a `null` del punto 34 para preservar claves foráneas y prevenir `NullPointerException` en eventos de entrega logística (absorbió bug 60).

- **60.** NPE en eventos de logística al procesar donaciones de donantes desvinculados — absorbido por el **Punto 69**: con Soft Delete la donación nunca se desvincula del donante (se elimina el parche del punto 34 y la FK `donacion.donante_id` permanece intacta), atacando la causa raíz del error.

- **66.** `DELETE /api/entidades/{id}` respondía 409 por necesidades asociadas — absorbido por el **Punto 70**: con Soft Delete la entidad nunca se elimina físicamente (`activa = false`) y sus necesidades abiertas se cancelan lógicamente, protegiendo las FKs y el histórico de donaciones.

- **47.** `REINGRESO_DEPOSITO` dejaba la donación varada en `PENDIENTE_ASIGNACION` sin resultado de matchmaking - actualizado el estado a `EN_DEPOSITO` en `GestorEventosLogistica.manejarReingresoDeposito` para que el scheduler de matchmaking la recoja automáticamente.

- **48.** `DELETE /personas/{id}/medios-contacto` no borraba nada y respondía 302 - implementado equals/hashCode por tipo+valor en `MedioDeContacto`, `removeIf` en `MediosDeContacto`, retorno 200 OK en controller.

- **46.** `PUT /api/admins/{id}` insertaba un admin nuevo en cada modificación - `AdminService` reutiliza la entidad original y `RepositorioAdministradores.actualizar` fuerza `idOriginal` antes de `save()`.

- **45.** `EventosListener`: transacción y requeue en hot loop - aislada la transacción en `GestorEventosLogistica`, configurado retry/backoff en `application.properties` con `default-requeue-rejected=false`, y la deduplicación quedó persistida en la tabla `eventos_logistica_procesados` (ver 75), que reemplazó al cursor `ultimoIdProcesado`.

- **44.** Los schedulers corrían sin transacción (LazyInitializationException) - `@Transactional` agregado en `AsignacionScheduler.ejecutarAsignacion`, `ActividadDonanteScheduler.revisarActividadDonantes` y `DonanteService.revisarActividades`.

- **35.** Dar de baja un donante no borraba confiablemente su perfil en incentivos - local DB commit se asegura primero con `@Transactional` + `flush()`, y llamada a `incentivosClient.eliminarPerfil` se sincroniza `afterCommit` con `try/catch` para no abortar ante caída externa.

- **34.** Tras donar, el donante no se podía dar de baja por violación de FK - implementado `desvincularDonante` en `Donacion` y `Formulario` antes de eliminar al donante localmente.

Un bullet por arreglo; el número es el ID estable del punto original («sin ID» = arreglos de integración que no tenían punto propio). Los residuales que siguen abiertos quedan anotados en el bullet.

- **58.** Declaración duplicada de dependencia `spring-boot-starter-validation` en `pom.xml` — eliminada del `pom.xml` en commit `8e3fa0f`.

- **59.** Importación CSV con nombres malformados o vacíos — saneado en `Humana`, `Juridica` y `PersonaDonanteFilaConverter`, evitando strings vacíos o de solo espacios y asegurando compatibilidad con los DTOs de incentivos.

- **40.** Importación CSV no actualizaba donantes existentes — descartado / por diseño: se toman intencionalmente como donantes distintos al no poseer identificador unívoco.

- **33.** La importación CSV dejaba a los donantes sin medio predeterminado — el converter marca el primero si ninguno viene marcado.

- **32.** La importación CSV ignoraba el casing de encabezados/mapeos — claves canónicas `trim().toLowerCase()`.

- **31.** Logística registraba un solo bien por donación — el mensaje agrega cantidades por unidad con el id repetido.

- **30.** El payload a incentivos no cumplía el contrato — se agregó `idDonacion` y `fechaEntrega` como `LocalDateTime`.

- **29.** `POST /donaciones/formulario` devolvía 400 sin motivo — handler que loguea y devuelve el mensaje.

- **28.** `BienDTO` mezclaba el mensaje con el modelo de logística — queda con `cantidad`/`unidadDeMedida`.

- **27.** El contrato con logística dependía de DTOs a mano — `idEntidad` y tamaños alineados + test de contrato; los DTOs siguen duplicados a mano (documentado, no corregido).

- **26.** El PUT de necesidad ignoraba el id de entidad y casteaba a ciegas — valida pertenencia y rechaza el cambio de tipo.

- **25.** La importación CSV se tragaba los errores sin conteos — `ResultadoLectura` + `ReporteImportacionDTO` + `GET /personas/importar/{id}`.

- **24.** El PUT de donación dejaba bienes huérfanos — mitigado: solo se editan bienes en `EN_DEPOSITO` (las filas huérfanas siguen).

- **23.** La segmentación sumaba kilos con litros — la unidad de medida entró en la clave de segmentación.

- **22.** No había Bean Validation — starter + `@NotNull/@Positive/@NotBlank` y handler de validación de body; `Humana.edad` sigue sin validar.

- **21.** Los controllers devolvían 404 ante cualquier `RuntimeException` — handlers por tipo (400/404/409).

- **20.** Los eventos de logística no se aislaban dentro de una ruta — try/catch por item; los otros tres manejadores y la DLQ siguen pendientes (ver punto 45).

- **19.** Borrar el medio predeterminado lo dejaba colgando — se promueve otro antes de remover (por el camino de la API la comparación por identidad lo anula: ver punto 48).

- **18.** `cantidadObjetivo`/`plazoEnDias` sin validar cortaban el matchmaking — se validan en `NecesidadDTO.toDomain`.

- **17.** El ranking de sub-atendidos lo monopolizaba una entidad — desempate `<=` + orden determinista por id.

- **16.** La lista de formularios era `@Transient` — `@OneToMany(mappedBy)` + default de fecha; el scheduler sigue roto (ver punto 44).

- **15.** El score de compatibilidad medía contra el histórico — `cantidadFaltante()` con ventana del período + guarda de `NaN`.

- **14.** Las propuestas se numeraban 1-based y se leían 0-based — contrato 1-based de punta a punta.

- **13.** El PUT de donante cambiaba el `@Id` — se eliminó el `setId`.

- **12.** `CascadeType.ALL` en las necesidades borraba de más — bajado a `{PERSIST, MERGE}`.

- **11.** Guardar el estado antes de notificar dejaba el cambio persistido — acciones antes del `save` + guardas de nulidad.

- **10.** `fechaEntrega` nunca se persistía — se setea al pasar a `ENTREGADO` en `actualizarEstado`.

- **9.** Un fallo en una donación cortaba el lote de matchmaking — try/catch por donación y resultado guardado antes del cambio de estado.

- **8.** No había transacciones — `@Transactional(rollbackFor)` en `DonacionService`; los demás services siguen afuera (ver punto 51).

- **7.** Un bien sin `tipoBien` reventaba la segmentación — excepción con el índice del item.

- **6.** Una estrategia de notificación no era bean — `@Component` + chequeo de la fábrica al arrancar contra el enum.

- **5.** `PATCH /vencer` mandaba "VENCIDA" y el parser conoce "VENCIDO" — corregido el string (el parser sigue aceptando strings).

- **4.** Dependencia de notificaciones-service en Maven — eliminada de logisticas-service/pom.xml (no pertenecía al módulo de donaciones).

- **Clientes (sin ID).** Apuntaban a la raíz de los servicios — rutas reales (`POST /api/perfiles`, `PATCH /api/perfiles/donacion/{id}`) y notificaciones migradas a Rabbit.

- **Clientes (sin ID).** Se tragaban las excepciones de salida sin log — ahora loguean y relanzan.

- **Rabbit (sin ID).** Dos bindings ataban al exchange equivocado y usaban comodines `.#` — cada servicio declara lo suyo con la clave exacta.

- **Properties (sin ID).** URLs default sin context-path y faltaba el bloque `spring.rabbitmq.*` — parametrizados por entorno.

