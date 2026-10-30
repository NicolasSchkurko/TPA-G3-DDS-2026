# Pendientes técnicos de `logisticas-service`

Registro de problemas conocidos del servicio, con el motivo y la propuesta de arreglo para
que no se pierdan de vista al crecer el código.

**Están ordenados de más urgente a menos urgente**, no por número de punto. El número es un
ID estable y no se renumera nunca, así que quedan huecos. Un punto corregido se borra de esta
lista y pasa a la sección [Corregidos](#corregidos) del final.

El orden no es el de la severidad declarada en cada punto sino el del daño real: cuánto se
rompe cuando pasa, y qué tan fácil es que pase.

| # | Punto | Severidad | Por qué está acá |
|---|---|---|---|
| 1 | 10 | Media | El reingreso a depósito no valida nada y su comentario cita un método que no existe |
| 2 | 31 | Media | El callback del simulador no tiene timeout y descarta la respuesta: un lote se pierde en silencio |
| 3 | 48 | Media | Un fallo del broker al publicar después del commit deja el 500, el cambio aplicado y la notificación perdida |
| 4 | 54 | Media | `RutaController.terminarRuta` no atrapa `IllegalStateException` y responde 500 ante un chofer sin ruta en curso |
| 5 | 43 | Media | El manejo de errores está repetido en cada controller: 404 ante campos obligatorios ausentes y 500 ante errores de validación |
| 6 | 44 | Media | Los listados devuelven la tabla entera: sin paginar, una respuesta crece sin techo |
| 7 | 17 | Media | El planificador resetea la carga de los camiones y nunca la persiste |
| 8 | 37 | Baja | El CRUD de camiones/choferes devuelve 500 para errores de validación |
| 9 | 40 | Baja | Los gestores están nombrados por entidad y mezclan reglas de negocio con persistencia |
| 10 | 42 | Baja | Paquetes de primer nivel y ubicación de clientes, scheduler y eventos distintos al resto de los servicios |
| 11 | 45 | Baja | El callback del proveedor externo está fijado a `localhost:8086`: si el puerto cambia o el proveedor es real, el resultado no vuelve |
| 12 | 49 | Baja | Cada cambio de estado de una entrega recorre todas las rutas con sus paradas e ítems en memoria |
| 13 | 20 | Informativa | Verificado: logística no invoca a `donaciones-service` ni incentivos ni habla con notificaciones |

---

## 10. El reingreso a depósito no valida nada y su comentario cita un método que no existe

**Estado:** abierto  
**Severidad:** media  
**Archivos:** `services/EntregaService.java:348-353`, `models/gestores/GestorPublicacionEventos.java:122-132`

### Qué pasa

En `EntregaService`:

```java
case "PENDIENTE":
  // Reingreso a depósito tras revisión de una entrega NO_RECIBIDA.
  // reingresarADeposito() ya valida que solo se pueda hacer desde NO_RECIBIDA.
  repoItemEntrega.save(gestorPublicacionEventos.publicarReingresoDeposito(item));
  break;
```

1. `reingresarADeposito()` **no existe** en ningún lugar de la base de código.
2. En `GestorPublicacionEventos.publicarReingresoDeposito(item)` se eliminó la guarda de estado en el commit `31396d0`. El método cambia el estado incondicionalmente a `PENDIENTE` sin validar si el estado actual es `NO_RECIBIDA`.

Un `PATCH /api/entregas/{id}/estado` con body `{"estado": "PENDIENTE"}` sobre un ítem en `ENTREGADA` o `EN_CAMINO` lo transiciona a `PENDIENTE` sin validar nada, reincorporándolo a la cola de pendientes a planificar.

### Propuesta de arreglo

Validar que `item.getEstado() == EstadoEntrega.NO_RECIBIDA` antes de procesar el reingreso en `publicarReingresoDeposito` o en `EntregaService`.

---

## 31. El callback del simulador no tiene timeout y descarta la respuesta: un lote se pierde en silencio

**Estado:** abierto  
**Severidad:** media  
**Archivos:** `models/entities/PlanificadorDeRutas/ProveedorRutasExterno/ProveedorRutasExternoSimulado.java:24-53`

### Qué pasa

1. **Sin timeout:** `HttpClient.newHttpClient()` no define `connectTimeout` y `HttpRequest` no tiene `.timeout()`. Si el callback no acepta la conexión, `httpClient.send(...)` puede bloquear el hilo del `CompletableFuture.runAsync` indefinidamente.
2. **Status descartado:** `send(...)` ignora el `statusCode()` de la respuesta HTTP. Si el callback responde 400 o 500, el simulador no lo detecta ni reintenta. El lote se pierde silenciosamente y los ítems quedan en `PENDIENTE` sin aviso.

### Cómo se dispara

Simular caída o error 500 en el callback durante la ejecución del scheduler: la falla no se registra ni se reintenta.

### Propuesta de arreglo

Configurar timeouts en `HttpClient` y `HttpRequest`, verificar `statusCode() == 200` y loguear con ERROR ante cualquier fallo.

---

## 48. Un fallo del broker al publicar después del commit deja el 500, el cambio aplicado y la notificación perdida

**Estado:** abierto  
**Severidad:** media  
**Archivos:** `models/gestores/GestorPublicacionEventos.java:140-159`, `messaging/ProductorEventosLogistica.java:30-40`, `controllers/EntregaController.java:80-92`

### Qué pasa

`publicarAlCommit` registra una sincronización `afterCommit` que invoca `productorEventos.publicar(evento)`. Si RabbitMQ no está disponible en ese instante, la excepción se propaga fuera del interceptor de transacciones cuando la transacción de base de datos **ya fue confirmada**:

- El ítem queda actualizado en la base de datos (por ejemplo a `ENTREGADA`).
- El evento queda persistido en `evento_logistica`.
- El cliente HTTP recibe un **500 Internal Server Error** con el stack trace o mensaje interno.
- El evento nunca llegó al broker: `donaciones-service` no notifica.
- Al reintentar la llamada HTTP por el 500, el endpoint rechaza con 400 ("no está en camino") porque el estado ya cambió.

### Propuesta de arreglo

Atrapar la excepción de RabbitMQ dentro de `afterCommit` y loguear como advertencia, o implementar un patrón Transactional Outbox con reintentos para publicación asíncrona.

---

## 54. `RutaController.terminarRuta` no atrapa `IllegalStateException` y responde 500 ante un chofer sin ruta en curso

**Estado:** abierto  
**Severidad:** media  
**Archivos:** `controllers/RutaController.java:90-99`, `services/RutaService.java:156-162`

### Qué pasa

Cuando un chofer intenta finalizar una ruta que no existe o no está en estado `EN_CURSO`:
`RutaService.rutaDelChoferPorEstado` lanza:

```java
return repoRutas.findByChoferYEstado(chofer, estado)
        .orElseThrow(() -> new IllegalStateException(
                "No se encontró una ruta en estado " + estado + " para el chofer " + idChofer));
```

En `RutaController.terminarRuta`:

```java
@PatchMapping("/chofer/{idChofer}/terminar")
public ResponseEntity<String> terminarRuta(@PathVariable UUID idChofer) {
  try {
    rutaService.terminarRuta(idChofer);
    return ResponseEntity.ok("Ruta finalizada correctamente...");
  } catch (IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(e.getMessage());
  } catch (Exception e) {
    return ResponseEntity.internalServerError().body("Error interno al finalizar la ruta: " + e.getMessage());
  }
}
```

A diferencia de `iniciarRuta` (línea 81), que atrapa `IllegalArgumentException | IllegalStateException` y responde 400 Bad Request, `terminarRuta` solo atrapa `IllegalArgumentException`. La `IllegalStateException` cae en `catch (Exception e)` y responde **500 Internal Server Error** filtrando el mensaje interno.

### Cómo se dispara

Llamar a `PATCH /api/rutas/chofer/{idChofer}/terminar` para un chofer que no tiene una ruta en `EN_CURSO` → 500 Internal Server Error en vez de 400/404.

### Propuesta de arreglo

Agregar `IllegalStateException` al bloque `catch` de `RutaController.terminarRuta`.

---

## 43. El manejo de errores está repetido en cada controller

**Estado:** abierto  
**Severidad:** media  
**Archivos:** `controllers/CamionController.java:101-106`, `controllers/ChoferController.java:104-109`, `controllers/RutaController.java:90-99`, `controllers/EntregaController.java:44-52`

### Qué pasa

No hay `@ControllerAdvice`: cada controlador maneja las excepciones de forma aislada e inconsistente:

- En `ChoferController.cambiarEstadoDisponibilidad` y `CamionController.cambiarEstadoDisponibilidad`: omitir el campo obligatorio `disponible` en el body dispara `IllegalArgumentException("El campo 'disponible' es obligatorio.")` en el service, y el controller la atrapa y responde **404 Not Found** en lugar de 400 Bad Request.
- `RutaController.terminarRuta` responde 500 ante `IllegalStateException` (punto 54).
- `EntregaController.obtenerPorId` traduce `IllegalArgumentException` en 404, mientras que `actualizarEstadoEntrega` la traduce en 400.

### Propuesta de arreglo

Crear un `@ControllerAdvice` global que mapee excepciones por tipo y significado (400 para argumentos inválidos, 404 para entidades no encontradas, 422 para reglas de negocio) y limpiar los `try/catch` dispersos en los controladores.

---

## 44. Los listados devuelven la tabla entera: falta paginación

**Estado:** abierto  
**Severidad:** media  
**Archivos:** `controllers/CamionController.java:31-34`, `controllers/ChoferController.java:31-34`, `controllers/EntregaController.java:28-31`, `controllers/RutaController.java:26-29`

### Qué pasa

Los endpoints de listado (`GET /api/camiones`, `GET /api/choferes`, `GET /api/entregas`, `GET /api/rutas`) devuelven todos los registros llamando a `findAll()` sin soporte para `Pageable`. En producción el tamaño de la respuesta crece sin límite.

### Propuesta de arreglo

Implementar paginación con `Pageable` y `@PageableDefault` devolviendo `Page<DTO>` o listas paginadas.

---

## 17. El planificador resetea la carga de los camiones y nunca la persiste

**Estado:** abierto  
**Severidad:** media  
**Archivos:** `models/entities/PlanificadorDeRutas/ProveedorRutasExterno/ProveedorRutasExternoSimulado.java:59-73`, `Scheduler/PlanificadorDeRutasScheduler.java:64-70`

### Qué pasa

El simulador crea copias locales de `Camion` y resetea su carga ocupada (`copia.resetearCargaOcupada()`) para simular la asignación en memoria. Sin embargo, una vez asignados los ítems y generada la ruta, el estado de ocupación de los camiones (`pesoOcupado`, `volumenOcupado`) nunca se persiste en `repoCamiones`. Las columnas en la base de datos quedan siempre en 0.

### Propuesta de arreglo

Persistir el estado de carga acumulado de los camiones tras la asignación, o derivar la ocupación directamente de las rutas activas persistidas.

---

## 37. El CRUD de camiones/choferes devuelve 500 para errores de validación

**Estado:** abierto  
**Severidad:** baja  
**Archivos:** `controllers/CamionController.java:51-63`, `controllers/ChoferController.java:51-63`

### Qué pasa

`POST /api/camiones` y `POST /api/choferes` no usan anotaciones de validación (`@Valid`) ni manejan errores de campos requeridos faltantes. Si se envía un payload sin campos requeridos, el guardado falla con excepciones de integridad o nulos que se traducen en HTTP 500 en lugar de 400 Bad Request.

### Propuesta de arreglo

Agregar anotaciones de validación (`@NotNull`, `@NotBlank`) en `CamionDTO` y `ChoferDTO`, marcar los métodos de controller con `@Valid` y capturar `MethodArgumentNotValidException`.

---

## 40. Los gestores están nombrados por entidad y mezclan reglas con persistencia

**Estado:** abierto  
**Severidad:** baja  
**Archivos:** `models/gestores/GestorCamiones.java`, `models/gestores/GestorPublicacionEventos.java`

### Qué pasa

`GestorCamiones` mezcla actualización de entidad con persistencia directa en base de datos. La capa de gestores no tiene una responsabilidad homogénea ni desacoplada de la persistencia frente a los services.

### Propuesta de arreglo

Refactorizar gestores para que contengan exclusivamente lógica de dominio y políticas de negocio, delegando la persistencia a la capa de servicios o repositorios.

---

## 42. Paquetes de primer nivel y ubicación de clientes, scheduler y eventos

**Estado:** abierto  
**Severidad:** baja  
**Archivos:** `RabbitMQ/`, `Scheduler/`, `messaging/`

### Qué pasa

`RabbitMQ/` y `Scheduler/` son paquetes en PascalCase ubicados en el nivel raíz del código fuente, en discrepancia con las convenciones estándar de nombres de paquetes en Java y con la estructura modular del resto de los microservicios del proyecto.

### Propuesta de arreglo

Renombrar los paquetes a minúsculas (`rabbitmq`, `scheduler`) y ubicarlos bajo convenciones estándar (`config`, `scheduler`, `messaging`).

---

## 45. El callback del proveedor externo está fijado a `localhost:8086`

**Estado:** abierto  
**Severidad:** baja  
**Archivos:** `models/entities/PlanificadorDeRutas/ProveedorRutasExterno/ProveedorRutasExternoSimulado.java:23`

### Qué pasa

La URL de callback está cableada como constante estática:

```java
private final String URL_CALLBACK_LOCAL = "http://localhost:8086/api/PlanificacionRutas/callback";
```

Si el servicio corre en otro puerto (`SERVER_PORT != 8086`) o en un entorno contenerizado en red bridge, el callback pega contra un puerto incorrecto y el lote se pierde silenciosamente.

### Propuesta de arreglo

Extraer la URL a una propiedad configurable (`logistica.callback-url`) en `application.properties`.

---

## 49. Cada cambio de estado de una entrega recorre todas las rutas con sus paradas e ítems en memoria

**Estado:** abierto  
**Severidad:** baja  
**Archivos:** `models/repositories/RepositorioRutas.java:15-20`, `services/EntregaService.java:334,343`

### Qué pasa

```java
default Optional<Ruta> findByIdDonacion(UUID idDonacion){
    return this.findAll().stream()
            .filter(ruta -> ruta.obtenerTodosLosItems().stream()
                    .anyMatch(item -> item.getIdDonacion().equals(idDonacion)))
            .findFirst();
}
```

Cada actualización de estado de entrega invoca `repoRutas.findByIdDonacion`, el cual ejecuta `this.findAll()` trayendo **todas** las rutas de la historia a memoria para filtrarlas por stream.

### Propuesta de arreglo

Reemplazar por una query nativa de Spring Data MongoDB sobre la colección `ruta` filtrando por el campo embebido `paradas.items.id_donacion`.

---

## 20. Verificado: logística no invoca a `donaciones-service` ni incentivos ni habla con notificaciones

**Estado:** verificado, sin cambios necesarios  
**Severidad:** informativa  
**Archivos:** todo `logisticas-service/src/main`

El servicio de logística mantiene el cumplimiento estricto del enunciado: no realiza llamadas síncronas HTTP a otros servicios de dominio y notifica exclusivamente publicando eventos asíncronos en RabbitMQ hacia el exchange de logística.

---

## Corregidos

- **34** — `UnidadDeMedida` persiste constantes estáticas con `@Version`: carreras concurrentes tiran `OptimisticLockingFailureException` y reinicios duplican catálogo → Causa raíz: `UNIDADES`, `KILOGRAMOS` y `LITROS` son singletons estáticos compartidos en memoria declarados con `@Version`, y `EntregaService` ejecutaba `repoUnidades.save` por cada donación, colisionando versiones entre hilos concurrentes. Además, se instanciaban con `UUID.randomUUID()` duplicando documentos al reiniciar. Solución: se removió `@Version` de `UnidadDeMedida`, se asignaron UUIDs deterministas basados en el nombre (`UUID.nameUUIDFromBytes`), se removió `repoUnidades.save` del flujo por donación y se pre-carga el catálogo en `@PostConstruct` de `EntregaService`. Archivos: `UnidadDeMedida.java`, `EntregaService.java`, `UnidadDeMedidaTest.java`. Referencia: Pendiente de commit (cambios en staged).

- **55** — `DonacionListener` no captura `DataIntegrityViolationException`: carreras concurrentes reintentan 3 veces innecesariamente y van a la DLQ → Causa raíz: al procesar mensajes duplicados o concurrentes que violan integridad (ej. claves duplicadas), caía en el `catch (Exception e)` genérico, agotaba los 3 reintentos de RabbitMQ y enviaba el mensaje a la DLQ. Solución: se captura `DataIntegrityViolationException` explícitamente y se retorna sin relanzar ni enviar a DLQ. Archivos: `DonacionListener.java`, `DonacionListenerCarreraTest.java`. Referencia: Pendiente de commit (cambios en staged).
- **56** — Tests unitarios desactualizados rompen el build tras migración a MongoDB → Causa raíz: `ConfiguracionArranqueTest` verificaba claves JPA eliminadas; `EventoLogisticaServiceTest` usaba reflexión buscando `(String, String)` en lugar de `(Long, String)`; `EntregaServiceIdempotenciaTest` no stubbeaba el guardado del catálogo de direcciones (`repoPaises`, `repoProvincias`, `repoCiudades`, etc.). Solución: se actualizaron las aserciones a `spring.data.mongodb.uri`, se corrigió la firma del método en la reflexión a `Long.class` y se agregaron mocks/stubbings en `@BeforeEach`. Archivos: `ConfiguracionArranqueTest.java`, `EventoLogisticaServiceTest.java`, `EntregaServiceIdempotenciaTest.java`. Referencia: Pendiente de commit (cambios en staged).

- **51** — Regresión del punto 12: `obtenerEventosNuevos` resta 1 a `desdeId`, tira `NullPointerException` con null y duplica eventos → Causa raíz: al consultar eventos se calculaba `desde = (desdeId == null ? 0L : desdeId)` pero se pasaba `(desdeId - 1)` al repositorio, provocando NPE con null y reenviando el último evento ya procesado. Solución: se pasa `desde` directamente a `repoEventos.findByIdGreaterThanOrderByIdAsc(desde)`. Archivos: `EventoLogisticaService.java`, `EventoLogisticaServiceObtenerEventosNuevosTest.java`. Referencia: Pendiente de commit (cambios en staged).
- **52** — `iniciarRuta` pierde `urlSeguimiento` porque `obtenerRutaActualizada` recarga de la BD antes de guardar → Causa raíz: `iniciarRuta` asignaba la URL de seguimiento en memoria pero luego recargaba de MongoDB (donde aún era null) antes de persistir, sobreescribiendo el campo con null. Solución: se persiste la ruta (`repoRutas.save(rutaConEvento)`) antes de recargar. Archivos: `RutaService.java`, `RutaServiceIniciarRutaTest.java`. Referencia: Pendiente de commit (cambios en staged).
- **53** — `Secuencia.siguiente()` invoca un `MongoTemplate` estático no inicializado dentro del constructor de `EventoLogistica` → Causa raíz: `EventoLogistica` invocaba `Secuencia.siguiente()` en sus constructores, el cual requería que Spring hubiese inicializado `mongoTemplate`, estallando con NPE en tests unitarios. Solución: se agregó fallback atómico en memoria `AtomicLong` cuando `mongoTemplate == null` y método `setMongoTemplate`. Archivos: `Secuencia.java`, `EventoLogisticaSecuenciaTest.java`. Referencia: Pendiente de commit (cambios en staged).
- **46** — Los lotes comparten la lista de camiones: un camión queda en dos rutas del mismo día y por encima de su capacidad → Causa raíz: al particionar en lotes de 100 ítems, se enviaba la lista completa de camiones a cada lote y el simulador reseteaba la carga ocupada, permitiendo que un camión fuera asignado a múltiples rutas el mismo día. Solución: `PlanificadorDeRutas.iniciarPlanificacion` ahora particiona la lista de camiones disponibles en subconjuntos disjuntos por cada lote de ítems, garantizando que ningún camión viaje en más de un lote. Archivos: `PlanificadorDeRutas.java`, `PlanificadorDeRutasScheduler.java`, `PlanificadorDeRutasTest.java`. Referencia: Pendiente de commit (cambios en staged).
- **47** — Dos callbacks concurrentes eligen el mismo chofer → Causa raíz: `PlanificadorRutasService.asignarChoferes` leía choferes disponibles y asignaba al azar sin exclusión mutua, permitiendo que callbacks simultáneos eligieran al mismo chofer y colisionaran en base de datos. Solución: se sincronizó `asignarChoferes` (`synchronized`) y se excluyen los choferes que ya se encuentren asignados a camiones en rutas activas (`PROGRAMADA` / `EN_CURSO`). Archivos: `PlanificadorRutasService.java`, `PlanificadorRutasServiceAsignarChoferesTest.java`. Referencia: Pendiente de commit (cambios en staged).
- **26** — Replanificar crea rutas duplicadas para los mismos ítems, cada noche y en cada manual → Causa raíz: `PlanificadorDeRutasScheduler.iniciarPlanificacionAutomatica` obtenía todos los ítems `PENDIENTE` sin excluir los que ya habían sido asignados a rutas `PROGRAMADA` o `EN_CURSO`. Solución: se inyectó `RepositorioRutas` y se filtran los ítems activos antes de enviar los lotes al proveedor externo. Archivos: `PlanificadorDeRutasScheduler.java`, `PlanificadorDeRutasSchedulerTest.java`. Referencia: Pendiente de commit (cambios en staged).
- **50** — Terminar una ruta revierte todas las entregas confirmadas a `PENDIENTE` y publica falso `REINGRESO_DEPOSITO` → Causa raíz: `terminarRuta` iteraba sobre los ítems embebidos en el documento `Ruta` que retenían el estado `EN_CAMINO` sin sincronizar con `repoItemEntrega`, y `EntregaService.actualizarEstado` no sincronizaba el documento de la ruta al registrar entregas. Solución: se agregó sincronización de paradas en el documento de ruta en `EntregaService.actualizarEstado`, se adelantó `obtenerRutaActualizada` al inicio del bucle en `RutaService.terminarRuta` y se eliminó la recarga redundante tras los deletes. Archivos: `EntregaService.java`, `RutaService.java`, `RutaServiceTerminarRutaTest.java`. Referencia: Pendiente de commit (cambios en staged).
- **2** — El estado de la ruta se persistía por efecto colateral de OSIV → Corregido en la migración a MongoDB: se eliminó el método `default` incorrecto en `RepositorioRutas` y `RutaService` persiste explícitamente mediante `repoRutas.save(rutaActual)`.
- **3** — Terminar una ruta borraba un ítem y después tiraba excepción dentro de `if(isPresent())` → Corregido en `610ccb2` / `a6f8296`: se eliminó el `throw` anómalo y la eliminación de entregas terminadas se valida con `existsById`.
- **4** — La condición del camión en `terminarRuta` estaba invertida (`camion.isPresent() throw "Camión no encontrado"`) → Corregido en `610ccb2`: se desvincula y resetea el camión directamente sobre `rutaActual.getCamionAsignado()`.
- **7** — El getter manual de `Parada.getEntidadDestino()` pisaba a Lombok e ignoraba su propio campo → Corregido en `7e1fed5`: se eliminó el getter manual y Lombok provee el getter del campo `entidadDestino`.
- **8** — Confirmar una entrega que no estaba en camino se ignoraba en silencio respondiendo 200 → Corregido: `GestorPublicacionEventos.publicarEntregaConfirmada` valida `item.getEstado() == EstadoEntrega.EN_CAMINO` y lanza `IllegalStateException`.
- **9** — Reportar una entrega fallida no validaba el estado previo → Corregido: `GestorPublicacionEventos.publicarEntregaFallida` valida `item.getEstado() == EstadoEntrega.EN_CAMINO` y lanza `IllegalStateException`.
- **18** — `return null` en el catch del planificador manual hacía fallar a Spring con 500 genérico → Corregido en `PlanificadorDeRutasController`: devuelve `ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)`.
- **27** — `findByChofer` devolvía la primera ruta histórica sin filtrar estado → Corregido: `RepositorioRutas.findByChoferYEstado` filtra por chofer y estado (`PROGRAMADA` / `EN_CURSO`).
- **29** — Un PATCH/PUT sin el campo `disponible` marcaba en ocupado o lanzaba error → Corregido: `CamionService` y `ChoferService` verifican `body.containsKey("disponible")` y los DTOs no pisan valores `null`.
- **30** — `POST /entregas` respondía 201 sin registrar nada cuando `bienes` o `idsDonaciones` venían null → Corregido: `EntregaService.procesarPeticion` lanza `IllegalArgumentException`.
- **32** — `DonacionListener` atrapaba toda `DataIntegrityViolationException` descartando mensajes como duplicados → Corregido: se removió el catch genérico en `DonacionListener` y los fallos van a la DLQ.
- **35** — Cada mensaje insertaba país, provincia y ciudad nuevos aunque la entidad ya existiera → Corregido: se consulta `repoEntidades.findById` primero y se busca por `findByNombreIgnoreCase`.
- **36** — El callback devolvía 500 para IDs inexistentes por envolver en `RuntimeException` de base de datos → Corregido: `PlanificadorRutasService` relanza `IllegalArgumentException` y `IllegalStateException` hacia el controller (códigos 400 y 422).
- **38** — Los repositorios de país, provincia y ciudad declaraban ID `UUID` y la entidad `Long` → Corregido en la migración a MongoDB: las entidades y repositorios unificaron el tipo de clave a `String`.
- **41** — Los repositorios convivían repartidos en dos esquemas de paquetes distintos → Corregido: se consolidaron todas las interfaces de repositorio en `models/repositories/`.
- **1** — El módulo no tenía un solo test → llegaron los primeros, con los casos de N instancias.
- **5** — Los tres `DELETE` borraban y después tiraban "no encontrado", devolviendo 404 tras borrado exitoso → se invirtió el chequeo.
- **6** — `ItemEntrega.eventos` usaba `mappedBy="id"` apuntando a la PK del evento → relación corregida.
- **11** — Una misma instancia de evento se metía en la lista de todos los ítems de la ruta → un solo `INICIO_RUTA` por ruta.
- **12** — El polling pedía `id > desdeId - 1` y reventaba con null → corregido inicialmente, aunque reapareció como regresión (ver punto 51).
- **13** — `findByIdGreaterThanOrderByIdAsc` era un `default` con `findAll()` → derived query real.
- **14** — `GET /entregas/{id}` devolvía la entidad JPA → devuelve `BienDTO`.
- **15** — Una donación con N bienes creaba N direcciones huérfanas → catálogo resuelto por mensaje.
- **16** — `procesarPeticion` validaba solo `request` y el resto reventaba a mitad de escritura → validaciones previas al guardado.
- **19** — Validación de justificación de entrega fallida → verificado sin inversión.
- **21** — Una redelivery reseteaba el ítem a `PENDIENTE` → guarda `existsById`.
- **22** — Concurrencia sin `@Version` → se incorporó `@Version`.
- **23** — Orden con N consumidores → reparto por hash implementado.
- **24** — Compose no escalable por `container_name` fijo → puerto efímero y `SERVER_PORT`.
- **25** — Mensajes agotados se reencolaban indefinidamente → rechazo sin requeue hacia DLQ.
- **28** — Publicación de evento dentro de transacción → movida a `afterCommit`.
- **33** — Credenciales de BD hardcodeadas → `.env` desacoplado.
- **39** — Cron corría a las 02:00 UTC en lugar de ART → `zone` explícita en `@Scheduled`.
