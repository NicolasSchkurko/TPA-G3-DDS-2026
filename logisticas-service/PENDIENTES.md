## Revisión del enunciado hasta Entrega 6

La logística implementa flota, choferes, entregas, rutas, callback de planificación, eventos y persistencia
MongoDB, consistente con E3-E5. Se mantienen abajo los riesgos técnicos vigentes; priorizar los de
serialización HTTP externa, tolerancia a fallas del proveedor/broker, concurrencia distribuida y trazado
real de rutas. E3 también solicita documentación de APIs (Postman y Swagger/OpenAPI); hay colección
Postman, pero no se encontró configuración OpenAPI versionada. E6 requiere que el front consuma estos
flujos con autenticación; el front ya tiene un cliente de logística para el seguimiento público de rutas.

---
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
| 1 | 74 | Baja | El planificador simulado podría resolver la parte geográfica con OSRM y la asignación con una heurística local |
| 2 | 20 | Informativa | Verificado: logística no invoca a `donaciones-service` ni incentivos ni habla con notificaciones |

---

## 74. El planificador simulado podría resolver la parte geográfica con OSRM y la asignación con una heurística local

**Estado:** abierto  
**Severidad:** baja  
**Archivos:** `models/entities/PlanificadorDeRutas/ProveedorRutasExterno/ProveedorRutasExternoSimulado.java`, `.../ProveedorRutasExternoHttp.java`, `config/LogisticaConfig.java`, `models/entities/PlanificadorDeRutas/PlanificadorDeRutas.java`, `services/PlanificadorRutasService.java`

### Qué pasa

Hoy `LogisticaConfig` registra `ProveedorRutasExternoSimulado`, que agrupa las entregas por ciudad y las asigna a camiones por capacidad, pero no usa la red vial. OSRM puede resolver la matriz de distancias/tiempos y el trazado por calles, aunque no puede hacer por sí solo la asignación de múltiples camiones con capacidad. No existe una implementación local que combine ambas partes.

### Propuesta

Crear `ProveedorRutasExternoOsrm implements ProveedorRutasExterno`:

1. Usar el lat/lon que ya viene en cada parada.
2. Pedir a OSRM `/table` la matriz de duraciones/distancias.
3. Resolver la asignación con una heurística Java simple: vecino más cercano + capacidad de peso/volumen, trabajando sobre copias de los `Camion` para no mutar los documentos de Mongo antes de persistir.
4. Pedir a OSRM `/route` por cada camión para obtener el `LineString` por calles.
5. Guardar el trazado por calles y reemplazar la línea entre paradas que hoy dibuja el front. Después, disparar el callback actual con `Map<patente, List<UUID>>`.

Elegir la implementación por property en `LogisticaConfig`, por ejemplo `planificador.proveedor=osrm|simulado`.

No hace falta sumar VROOM ni otro microservicio: OSRM queda como infraestructura y la clase local cubre lo que OSRM no hace. Si más adelante se necesita optimización real (ventanas de tiempo, prioridades, múltiples depósitos), se reemplaza solo esta clase por un adaptador a VROOM.

### Puntos finos

- OSRM necesita un origen: hoy `Camion` no tiene dirección base. Definir un depósito global por property o agregar lat/lon al camión.
- `/table` tiene límite de coordenadas (`--max-table-size`, 100 por defecto); configurarlo o partir el lote.
- Si OSRM no responde, mantener el simulador como fallback o dejar la planificación para la próxima corrida.
- Abstraer el cliente OSRM detrás de una interfaz para poder mockear la matriz en los tests.

### Por qué es baja

Es una mejora de calidad y realismo, no un bug: el simulador actual permite cumplir el flujo de planificación. Se implementa si sobra tiempo.

---

## 20. Verificado: logística no invoca a `donaciones-service` ni incentivos ni habla con notificaciones

**Estado:** verificado, sin cambios necesarios  
**Severidad:** informativa  
**Archivos:** todo `logisticas-service/src/main`

El servicio de logística mantiene el cumplimiento estricto del enunciado: no realiza llamadas síncronas HTTP a otros servicios de dominio y notifica exclusivamente publicando eventos asíncronos en RabbitMQ hacia el exchange de logística.

---

## Corregidos

- **71.** El callback resolvía un `findById` por cada ítem y recorría listas completas de camiones e ítems por asignación — ahora `PlanificadorRutasService` trae todos los ítems con un único `findAllById`, valida los ids faltantes y `PlanificadorDeRutas` arma `Map<patente, Camion>` y `Map<UUID, ItemEntrega>` una sola vez. Cobertura en `PlanificadorRutasServiceTest`.

- **73.** La ruta no tenía forma de dibujarse en el mapa — `Direccion`/`DireccionDTO` ya transportaban lat/lon y `RutaDTO` las exponía, así que se reemplazó el host ficticio de `urlSeguimiento` por una URL configurable al front (`LOGISTICA_SEGUIMIENTO_URL`), y el front incorporó `LogisticasClient` + `GET /seguimiento/{idRuta}`, que une las coordenadas de las paradas en un GeoJSON (`LineString` + puntos) y lo dibuja con `DDSIMap.addGeoJson`. Cobertura en `SeguimientoControllerTest`.

- **72.** El Dockerfile de logística no pasaba flags JVM y el compose no limitaba la memoria — el `ENTRYPOINT` ahora usa `-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError` y el servicio declara `mem_limit: 512m`, por lo que el heap se calcula contra el límite del contenedor.

- **69.** Los eventos de trazabilidad crecían sin cota dentro de cada `ItemEntrega` y el polling cargaba toda la colección — el ítem ahora conserva solo el último evento, `GET /api/eventos` pagina con `desdeId` + `limite` (máximo 500) y `evento_logistica.fecha` tiene TTL de 30 días con `auto-index-creation` habilitado. Cobertura en `EventoLogisticaServiceTest`, `GestorPublicacionEventosTest` y `EventoLogisticaSecuenciaTest`.

- **70.** El driver de Mongo usaba `maxPoolSize=100` por instancia y no tenía timeouts — `MongoConfig` aplica pool 20/2 y timeouts de selección/conexión/lectura (5 s/5 s/10 s), configurables por `MONGODB_POOL_*`/`MONGODB_*_TIMEOUT_MS`. Cobertura en `MongoConfigTest`.

- **59** — `RutaService.terminarRuta` persistía instancias desactualizadas de `Camion` y `Chofer` embebidas en `Ruta`, disparando `OptimisticLockingFailureException` y pisando datos → Causa raíz: `terminarRuta` tomaba el camión y chofer directamente del snapshot embebido en el documento `Ruta` en lugar de sus repositorios correspondientes. Al persistir con `@Version`, si el camión o chofer fueron editados durante el día, fallaba con excepción de concurrencia optimista o sobreescribía los datos vigentes, además de provocar `NullPointerException` ante rutas sin camión. Solución: se recargan las entidades vigentes mediante `repoChoferes.findById(idChofer)` y `repoCamiones.findById(patente)` con guardas defensivas ante nulos antes de actualizar disponibilidad y persistir. Tests de regresión agregados en `RutaServiceTerminarRutaTest` (validando persistencia de entidades frescas con version superior y ruta sin camión). Archivos: `RutaService.java`, `RutaServiceTerminarRutaTest.java`.

- **58** — `Entidad` no implementaba `equals` ni `hashCode`: `Ruta.agregarEntrega` nunca agrupaba donaciones y generaba paradas duplicadas para el mismo destino → Causa raíz: al deserializar ítems desde MongoDB, cada `ItemEntrega` tiene su propia instancia de `Entidad` en memoria. Al evaluarse `Objects.equals(p.getEntidadDestino(), item.getEntidadDestino())`, Java recurría a la igualdad por identidad (`==`), fallando siempre la detección de paradas existentes para el mismo destino. Solución: se implementaron `equals` y `hashCode` en `Entidad` basados en `idEntidadBeneficiaria`. Test de regresión agregado en `RutaAgregarEntregaTest` verificando consolidación de múltiples ítems en una sola parada con instancias separadas. Archivos: `Entidad.java`, `RutaAgregarEntregaTest.java`.

- **57** — Si había menos choferes que rutas generadas, las rutas sobrantes quedaban guardadas en `PROGRAMADA` sin chofer y sus ítems bloqueados de por vida → Causa raíz: `PlanificadorRutasService.procesarCallbackRutas` guardaba incondicionalmente todas las rutas en `repoRutas` antes de asignar choferes. Como `PlanificadorDeRutasScheduler` excluye de nuevas planificaciones a los ítems en rutas con estado distinto a `FINALIZADA`, y las rutas huérfanas sin chofer no podían iniciarse ni finalizarse jamás, los ítems quedaban congelados indefinidamente en `PENDIENTE`. Solución: se eliminó la persistencia prematura en `procesarCallbackRutas`; `asignarChoferes` opera sobre una copia local de la lista de rutas generadas, asigna choferes hasta el límite de choferes disponibles y persiste (`persistirRutaAsignada`) exclusivamente aquellas rutas que consiguieron chofer asignado, dejando las no asignadas libres en memoria para que sus ítems continúen disponibles para futuras planificaciones; en `PlanificadorDeRutasController` se corrigió la comparación verificando si la cantidad de rutas asignadas cubre la totalidad de las generadas (`rutasGeneradas.size() == rutasAsignadas.size()`). Tests de regresión agregados en `PlanificadorRutasServiceAsignarChoferesTest`. Archivos: `PlanificadorRutasService.java`, `PlanificadorDeRutasController.java`, `PlanificadorRutasServiceAsignarChoferesTest.java`.

- **10** — El reingreso a depósito administrativo no validaba estado previo (`NO_RECIBIDA`) y citaba método inexistente → Causa raíz: en `EntregaService.actualizarEstado` el `case "PENDIENTE"` delegaba en `gestorPublicacionEventos.publicarReingresoDeposito(item)` asumiendo erróneamente en un comentario que un método inexistente `reingresarADeposito()` validaba el estado previo. Cualquier llamada a `PATCH /api/entregas/{id}/estado` con `PENDIENTE` sobre un ítem en `ENTREGADA` o `EN_CAMINO` lo devolvía a la cola de planificación. Además, `publicarReingresoDeposito` no tenía guarda defensiva. Solución: en `EntregaService.actualizarEstado` se valida que `item.getEstado() == EstadoEntrega.NO_RECIBIDA`, lanzando `IllegalStateException` ante otros estados. En `GestorPublicacionEventos.publicarReingresoDeposito` se agregó guarda defensiva que rechaza `ENTREGADA` y `PENDIENTE`, admitiendo `NO_RECIBIDA` y `EN_CAMINO` (para permitir el retorno al depósito al finalizar una jornada en `RutaService.terminarRuta`). Tests unitarios agregados en `EntregaServiceActualizarEstadoTest` y `GestorPublicacionEventosTest`. Archivos: `EntregaService.java`, `GestorPublicacionEventos.java`, `EntregaServiceActualizarEstadoTest.java`, `GestorPublicacionEventosTest.java`.

- **63** — Soft Delete en Chofer y desvinculación de camión asignado, preservando trazabilidad de rutas históricas → Causa raíz: `ChoferService.delete` realizaba un borrado físico directo (`repoChoferes.deleteById(id)`). Si el chofer estaba asignado a un camión en `repoCamiones`, el camión quedaba apuntando a un chofer inexistente. Si bien en MongoDB el documento `Ruta` embebe un snapshot del chofer y no se pierde la trazabilidad de rutas históricas, el chofer seguía siendo elegible si se recreaba con el mismo ID o se generaban inconsistencias de relaciones activas. Solución: se implementó baja lógica (Soft Delete) en `Chofer` (`activo = true`, `fechaBaja`, `darDeBaja()`, `isActivo()`), `Chofer.disponible()` valida que el chofer esté activo antes de cambiar el estado, `ChoferService.delete` marca baja lógica y desvincula automáticamente cualquier camión asignado (`repoCamiones.findByChofer_IdChofer(id)` -> `camion.eliminarChofer()`), `ChoferService.findAll` filtra solo choferes activos, `findById` y `cambiarDisponibilidad` rechazan choferes inactivos (404 / 400), `PlanificadorRutasService.asignarChoferes` filtra exclusivamente choferes activos, y `CamionService.update` valida que el chofer a asociar esté activo. Tests agregados en `DeleteDevuelve204Test` y `PlanificadorRutasServiceAsignarChoferesTest`. Archivos: `Chofer.java`, `ChoferDTO.java`, `ChoferService.java`, `CamionService.java`, `PlanificadorRutasService.java`, `DeleteDevuelve204Test.java`, `PlanificadorRutasServiceAsignarChoferesTest.java`.

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

### 60. `ProveedorRutasExternoHttp` no serializaba `LocalDateTime` por falta de `JavaTimeModule`

**Estado:** corregido
**Severidad:** alta
**Corregido:** 2026-11-08 · sin commit
**Archivos:** `logisticas-service/src/main/java/ar/edu/utn/frba/ddsi/logisticas/models/entities/PlanificadorDeRutas/ProveedorRutasExterno/ProveedorRutasExternoHttp.java`, `logisticas-service/src/test/java/ar/edu/utn/frba/ddsi/logisticas/models/entities/PlanificadorDeRutas/ProveedorRutasExterno/ProveedorRutasExternoHttpTest.java`

### Qué pasaba

El `ObjectMapper` se creaba con `new ObjectMapper()` sin `JavaTimeModule`. Al serializar el lote con `ItemEntrega`/`EventoLogistica` (que tienen `LocalDateTime`), Jackson lanzaba `InvalidDefinitionException`, el `catch (Exception e)` la tragaba y nunca se enviaba la petición HTTP al proveedor externo.

### Qué se cambió

Se registran los módulos con `findAndRegisterModules()` y se desactiva `WRITE_DATES_AS_TIMESTAMPS` para enviar las fechas en ISO-8601, igual que el `ObjectMapper` de Spring Boot.

### Cómo se verificó

Se agregó `ProveedorRutasExternoHttpTest`, que levanta un `HttpServer` local y comprueba que el lote llega con `"fechaCambioEstado":"2026-01-02T03:04:05"`. Falla sin el arreglo (timeout del latch, porque no se envía el request). Suite completa de `logisticas-service`: 82 tests, 0 fallos, `BUILD SUCCESS`.

### 64. `DonacionListener` no abortaba ante interrupción (el `Thread.sleep` acotado se mantiene)

**Estado:** corregido
**Severidad:** alta
**Corregido:** 2026-11-08 · sin commit
**Archivos:** `logisticas-service/src/main/java/ar/edu/utn/frba/ddsi/logisticas/messaging/DonacionListener.java`, `logisticas-service/src/test/java/ar/edu/utn/frba/ddsi/logisticas/messaging/DonacionListenerCarreraTest.java`

### Qué pasaba

Cuando el hilo se interrumpía (típico al apagarse el contexto), el `catch (InterruptedException)` restablecía el flag pero el bucle `for` no verificaba `isInterrupted()`: seguía reintentando contra un servicio que se estaba cerrando.

### Qué se cambió

`dormir()` ahora devuelve `false` si la espera fue interrumpida y el bucle corta ahí lanzando `AmqpRejectAndDontRequeueException`. El `Thread.sleep` acotado (2 s × 2) sigue siendo la estrategia documentada: no se migró al retry del container porque `RetryTemplate`/`FixedBackOffPolicy` también duerme el hilo consumidor; para no bloquear haría falta retry diferido con DLQ + TTL y republicación, que es un cambio aparte.

### Cómo se verificó

Test `DonacionListenerCarreraTest.interrupcionAbortaLosReintentos`: interrumpe el hilo durante la espera y verifica que `procesarPeticion` se invocó una sola vez (sin el arreglo se invocaba 3). Suite completa de `logisticas-service`: 83 tests, 0 fallos, `BUILD SUCCESS`.

### 61. `EntregaService.convertirADireccionDTO` y `convertirABienDTO` causan `NullPointerException` en `GET /api/entregas`

**Estado:** corregido
**Severidad:** alta
**Corregido:** 2026-11-08 · sin commit
**Archivos:** `logisticas-service/src/main/java/ar/edu/utn/frba/ddsi/logisticas/services/EntregaService.java`, `logisticas-service/src/test/java/ar/edu/utn/frba/ddsi/logisticas/services/EntregaServiceFindAllDatosParcialesTest.java`

### Qué pasaba

`convertirABienDTO` y `convertirADireccionDTO` desreferenciaban sin guardas `unidad`, `estado`, `entidadDestino`, `direccionDestino`, `ciudad`, `provincia` y `pais`. Un solo ítem con datos parciales en MongoDB hacía fallar con 500 `GET /api/entregas` y `GET /api/entregas/no-recibidas`.

### Qué se cambió

Se agregaron las guardas de nulidad que ya tenía `findById`, devolviendo `null` para los campos ausentes (incluida la dirección completa cuando falta entidad o dirección destino).

### Cómo se verificó

`EntregaServiceFindAllDatosParcialesTest` arma ítems con cada eslabón nulo (unidad/estado/entidad, dirección, ciudad, provincia, país) y verifica que los listados devuelven todos los DTOs. Sin el arreglo falla con NPE en `unidad.getNombre()`. Suite completa de `logisticas-service`: 85 tests, 0 fallos, `BUILD SUCCESS`.

### 68. `EntregaService.actualizarEstado` publica eventos al broker dentro de la transacción SQL (desactualizado: ya usa `afterCommit`)

**Estado:** corregido (ya estaba resuelto)
**Severidad:** media
**Corregido:** 2026-11-08 · sin commit
**Archivos:** `logisticas-service/src/main/java/ar/edu/utn/frba/ddsi/logisticas/models/gestores/GestorPublicacionEventos.java`, `logisticas-service/src/main/java/ar/edu/utn/frba/ddsi/logisticas/services/EntregaService.java`

### Qué pasaba

El pendiente describía una publicación al broker dentro de la transacción de base de datos (dual-write), con el evento pudiendo salir aunque el commit fallara.

### Qué se cambió

Nada: al revisar el código actual, `GestorPublicacionEventos.publicarAlCommit` ya difiere la publicación a `afterCommit` con `TransactionSynchronizationManager`, y solo publica en el momento cuando no hay transacción activa. Los tres caminos de `EntregaService.actualizarEstado` (ENTREGADA, NO_RECIBIDA, PENDIENTE) pasan por `guardarEventoDeItem` → `publicarAlCommit`.

### Cómo se verificó

Tests existentes en `GestorPublicacionEventosTest`: "Con transacción abierta el evento NO se publica hasta el commit" y "Un rollback deja el evento sin publicar". Suite completa de `logisticas-service`: `BUILD SUCCESS`.

### 65. `ProveedorRutasExternoSimulado` ejecuta I/O y sleep en `ForkJoinPool.commonPool()`

**Estado:** corregido
**Severidad:** media
**Corregido:** 2026-11-08 · sin commit
**Archivos:** `logisticas-service/src/main/java/ar/edu/utn/frba/ddsi/logisticas/models/entities/PlanificadorDeRutas/ProveedorRutasExterno/ProveedorRutasExternoSimulado.java`, `logisticas-service/src/main/java/ar/edu/utn/frba/ddsi/logisticas/config/LogisticaConfig.java`, `logisticas-service/src/test/java/ar/edu/utn/frba/ddsi/logisticas/models/entities/PlanificadorDeRutas/ProveedorRutasExterno/ProveedorRutasExternoSimuladoTest.java`

### Qué pasaba

`solicitarPlanificacion` llamaba `CompletableFuture.runAsync(task)` sin executor, así que el `Thread.sleep(2000)` y el POST corrían en el `ForkJoinPool.commonPool()` de la JVM, pensado para cómputo y no para esperas bloqueantes.

### Qué se cambió

Se inyecta un `ThreadPoolTaskExecutor` dedicado (`ejecutorRuteo`: un hilo, cola sin tope, daemon, prefijo `ruteo-`) y se usa `runAsync(task, executor)`. La cola sin tope mantiene la semántica del `commonPool` (encola, no rechaza) y serializa las planificaciones.

### Cómo se verificó

`ProveedorRutasExternoSimuladoTest` verifica que la tarea se entrega al executor provisto y que la cola no está acotada. Suite completa de `logisticas-service`: 87 tests, 0 fallos, `BUILD SUCCESS`. Los timeouts del `send` quedan para el punto 31.

### 31. Callback del simulador de rutas: URL fija, sin timeouts y status descartado

**Estado:** corregido
**Severidad:** media
**Corregido:** 2026-11-08 · sin commit
**Archivos:** `logisticas-service/src/main/java/ar/edu/utn/frba/ddsi/logisticas/models/entities/PlanificadorDeRutas/ProveedorRutasExterno/ProveedorRutasExternoSimulado.java`, `logisticas-service/src/main/java/ar/edu/utn/frba/ddsi/logisticas/config/LogisticaConfig.java`, `logisticas-service/src/main/resources/application.properties`, `logisticas-service/.env.example`, `logisticas-service/src/test/java/ar/edu/utn/frba/ddsi/logisticas/models/entities/PlanificadorDeRutas/ProveedorRutasExterno/ProveedorRutasExternoSimuladoTest.java`

### Qué pasaba

La URL del callback estaba cableada a `http://localhost:8086`, el `HttpClient`/`HttpRequest` no tenían timeouts y el `statusCode()` de la respuesta se descartaba: con otro puerto o ante un 4xx/5xx, el lote calculado se perdía en silencio.

### Qué se cambió

La URL sale de `logistica.callback-url` (`LOGISTICA_CALLBACK_URL`), con default que sigue a `server.port`. Se configuró `connectTimeout` de 5 s y `timeout` de request de 10 s. Se valida que el status sea 2xx y se loguea `ERROR` con URL, status y body cuando no lo es; el log de excepción también incluye la URL.

### Cómo se verificó

`ProveedorRutasExternoSimuladoTest` usa `ApplicationContextRunner` para verificar property → `@Value` → POST real contra un `HttpServer` en puerto aleatorio, y otro test captura el `ERROR` de un callback 500. Con la URL fija el test de cableado falla. Suite completa de `logisticas-service`: 89 tests, 0 fallos, `BUILD SUCCESS`.

### 48. Un fallo del broker al publicar después del commit dejaba el 500 y la notificación perdida

**Estado:** corregido
**Severidad:** media
**Corregido:** 2026-11-08 · sin commit
**Archivos:** `logisticas-service/src/main/java/ar/edu/utn/frba/ddsi/logisticas/models/gestores/GestorPublicacionEventos.java`, `logisticas-service/src/test/java/ar/edu/utn/frba/ddsi/logisticas/models/gestores/GestorPublicacionEventosTest.java`

### Qué pasaba

`afterCommit` llamaba `productorEventos.publicar(evento)` sin proteger la excepción. Con RabbitMQ caído, la transacción ya había commiteado y el evento estaba persistido, pero la excepción salía hasta el controller: el cliente recibía 500 y al reintentar 400 porque el estado ya había avanzado.

### Qué se cambió

La publicación pasa por `publicarProtegido`, que atrapa `RuntimeException` y loguea `ERROR` con tipo e id del evento, sin propagar. Se usa tanto en `afterCommit` como en el camino sin transacción.

### Cómo se verificó

`GestorPublicacionEventosTest.unFalloDelBrokerTrasElCommitNoSePropaga` simula un fallo del productor y verifica que el commit no lanza, que se intentó publicar y que el evento quedó persistido; sin el fix falla con la `AmqpException` propagada. Suite completa de `logisticas-service`: 90 tests, 0 fallos, `BUILD SUCCESS`.

**Límite conocido:** el evento queda en `evento_logistica`, pero no hay republicación automática; recuperarlo requiere replay manual. El outbox con reintentos es un cambio mayor aparte.

### 44. Los listados devolvían la tabla entera

**Estado:** corregido
**Severidad:** media
**Corregido:** 2026-11-08 · sin commit
**Archivos:** `logisticas-service/src/main/java/ar/edu/utn/frba/ddsi/logisticas/controllers/{Camion,Chofer,Entrega,Ruta}Controller.java`, `.../services/{Camion,Chofer,Entrega,Ruta}Service.java`, `.../models/repositories/RepositorioChoferes.java`, `.../dto/entrega/BienDTO.java`, `logisticas-service/src/test/java/.../services/PaginacionListadosTest.java`, `.../controllers/CamionControllerPaginacionTest.java`, `postman/ciclo-completo.postman_collection.json`

### Qué pasaba

`GET /api/camiones`, `/choferes`, `/entregas` y `/rutas` hacían `findAll()` y devolvían todos los registros; la respuesta crecía sin límite.

### Qué se cambió

Los cuatro listados reciben `@ParameterObject @PageableDefault(size = 20) Pageable` y devuelven `Page<DTO>`; el `Pageable` llega al repositorio y se mapea con `Page.map`. Choferes pasó a `@Query("{ 'activo': { $ne: false } }")` para paginar con el filtro de activos en la base (equivalente a `isActivo()`, que incluye `null`/ausente). `BienDTO` ahora expone `idDonacion`, porque `/entregas` dejó de devolver el array `idsDonaciones`. Se actualizó el test de Postman de `/rutas` a `.content`.

### Cómo se verificó

`PaginacionListadosTest` verifica que el `Pageable` llegue al repositorio y que se devuelva la página; `CamionControllerPaginacionTest` prueba con `MockMvc` que sin parámetros el default es page 0 / size 20 (con fallback distinto para asegurar que viene de la anotación); `EntregaServiceFindAllDatosParcialesTest` fija `idDonacion` por ítem. Suite completa de `logisticas-service`: 94 tests, 0 fallos, `BUILD SUCCESS`.

**Límites conocidos:** `/entregas/no-recibidas` quedó sin paginar (no estaba entre los cuatro endpoints del punto) y no se fijó un `sort` default, así que el orden entre páginas no está garantizado. Cambio de contrato: `/entregas` ya no devuelve `idsDonaciones`; cada ítem trae `idDonacion`.

### 43. Manejo de errores repetido y códigos HTTP incorrectos en los controllers

**Estado:** corregido
**Severidad:** media
**Corregido:** 2026-11-08 · sin commit
**Archivos:** `logisticas-service/src/main/java/ar/edu/utn/frba/ddsi/logisticas/controllers/GlobalExceptionHandler.java` (nuevo), `.../exceptions/RecursoNoEncontradoException.java` (nuevo), `.../controllers/{Camion,Chofer,Entrega,Ruta}Controller.java`, `.../services/{Camion,Chofer,Entrega,Ruta}Service.java`, `.../models/gestores/GestorCamiones.java`, `logisticas-service/src/test/java/.../controllers/{GlobalExceptionHandlerTest,ErrorHandlingControllersTest}.java` (nuevos), tests existentes adaptados, `postman/ciclo-completo.postman_collection.json`

### Qué pasaba

Cada controller tenía su propio `try/catch` y no coincidían: una validación faltante devolvía 404 y un `IllegalStateException` de precondición devolvía 500. La raíz era que `IllegalArgumentException` se usaba tanto para "no encontrado" como para "pedido inválido".

### Qué se cambió

Se creó un `@RestControllerAdvice` (`GlobalExceptionHandler`) que mapea `RecursoNoEncontradoException`→404, `IllegalArgumentException`→400, `IllegalStateException`→409 y `DuplicateKeyException`→409. Los servicios ahora lanzan `RecursoNoEncontradoException` para los recursos ausentes y dejaron `IllegalArgumentException` solo para validación. Se eliminaron los `try/catch` de los cuatro controllers y se actualizaron las anotaciones OpenAPI y la descripción de Postman.

### Cómo se verificó

`GlobalExceptionHandlerTest` cubre cada mapeo; `ErrorHandlingControllersTest` verifica por MockMvc que `GET` inexistente da 404, disponibilidad sin `disponible` da 400 y terminar sin ruta activa da 409. Se comprobó el rojo forzando los handlers a 500 (fallaban con `expected:<400/409> but was:<500>`). Suite completa de `logisticas-service`: 101 tests, 0 fallos, `BUILD SUCCESS`.

**Límite conocido:** al mapear `IllegalStateException`→409 también quedan como 409 dos ISE internas raras (fallo de serialización de payload y carrera al resolver una entidad); si se quiere distinguir, hace falta un tipo propio de conflicto de estado y dejar las internas en 500.

### 66. `ProveedorRutasExternoHttp` descartaba en silencio las fallas de red

**Estado:** corregido
**Severidad:** media
**Corregido:** 2026-11-08 · sin commit
**Archivos:** `logisticas-service/src/main/java/ar/edu/utn/frba/ddsi/logisticas/models/entities/PlanificadorDeRutas/ProveedorRutasExterno/ProveedorRutasExternoHttp.java`, `logisticas-service/src/test/java/ar/edu/utn/frba/ddsi/logisticas/models/entities/PlanificadorDeRutas/ProveedorRutasExterno/ProveedorRutasExternoHttpTest.java`

### Qué pasaba

`sendAsync(...).thenAccept(...)` no tenía `.exceptionally`: ante DNS, conexión rechazada o timeout, el `CompletableFuture` quedaba excepcional y la excepción se descartaba sin log. Tampoco había timeouts.

### Qué se cambió

Se agregó `connectTimeout` de 5 s, `timeout` de 10 s en el request, `orTimeout` de 10 s sobre el futuro (el timeout del request no cubre la lectura del body en Java 21/25) y un `.exceptionally` que loguea `ERROR` con la URL y el stack trace.

### Cómo se verificó

`ProveedorRutasExternoHttpTest.unaFallaDeRedSeLoguea` apunta a un puerto cerrado y, con un appender de Logback, verifica que se emite un `ERROR` con la URL y el throwable. Sin el `.exceptionally` el test falla por timeout del latch. Suite completa de `logisticas-service`: 102 tests, 0 fallos, `BUILD SUCCESS`.

### 49. RepositorioRutas escaneaba la colección entera de rutas en memoria

**Estado:** corregido
**Severidad:** media
**Corregido:** 2026-11-08 · sin commit
**Archivos:** `logisticas-service/src/main/java/ar/edu/utn/frba/ddsi/logisticas/models/repositories/RepositorioRutas.java`, `.../services/RutaService.java`, `.../services/EntregaService.java`, `.../test/.../models/repositories/RepositorioRutasQueriesTest.java` (nuevo), tests de rutas actualizados

### Qué pasaba

`RepositorioRutas` resolvía las búsquedas por donación y por chofer+estado con métodos `default` que hacían `findAll().stream()`. Cada cambio de estado de una entrega o cada inicio/fin de ruta bajaba toda la colección `ruta` a la JVM para filtrar en memoria.

### Qué se cambió

Se reemplazaron por derived queries `findFirstByParadasItemsIdDonacion` y `findFirstByCamionAsignadoChoferIdChoferAndEstado`. El `First` mantiene el resultado limitado a 1 (conserva la semántica del viejo `findFirst()` y evita el `IncorrectResultSizeDataAccessException` de un `@Query` con `Optional`). Se eliminó `findByChofer`, que no tenía usos, y se actualizaron los call sites y stubs. Además, `PlanificadorRutasService.asignarChoferes` y `PlanificadorDeRutasScheduler` dejaron de hacer `repoRutas.findAll()` y ahora usan `findByEstadoIn(PROGRAMADA, EN_CURSO)`.

### Cómo se verificó

`RepositorioRutasQueriesTest` verifica por reflexión que las búsquedas no son `default` ni `@Query` manual (son derived `findFirstBy`, con los nombres de propiedad Java que el mapping resuelve a `_id`). Al reintroducir un `@Query`/`findAll()` el test falla. Suite completa de `logisticas-service`: 103 tests, 0 fallos, `BUILD SUCCESS`.

**Límites conocidos:** no se crean índices (`spring.data.mongodb.auto-index-creation` apagado), así que Mongo sigue haciendo COLLSCAN aunque ya no transfiera el histórico a la JVM. `findFirstBy...` mantiene el comportamiento previo de devolver la primera ruta si la donación aparece en más de una.

### 62. `CamionService.update` desvinculaba el chofer si el DTO no enviaba `idChofer`

**Estado:** corregido
**Severidad:** media
**Corregido:** 2026-11-08 · sin commit
**Archivos:** `logisticas-service/src/main/java/ar/edu/utn/frba/ddsi/logisticas/models/gestores/GestorCamiones.java`, `logisticas-service/src/test/java/ar/edu/utn/frba/ddsi/logisticas/services/CamionServiceUpdateChoferTest.java` (nuevo)

### Qué pasaba

`GestorCamiones.actualizarCamion` hacía `camionExistente.setChofer(nuevoChofer)` incondicionalmente. Un `PUT /api/camiones/{patente}` que mandaba solo `disponible` o la capacidad resolvía `nuevoChofer = null` y desvinculaba silenciosamente al chofer.

### Qué se cambió

La asignación del chofer pasó a ser condicional (`if (nuevoChofer != null)`), igual que el resto de los campos del DTO. Desvincular queda como operación explícita (`ChoferService.delete`, `RutaService.terminarRuta`), no como efecto de un PUT parcial.

### Cómo se verificó

`CamionServiceUpdateChoferTest` cubre: sin `idChofer` conserva el chofer y actualiza los demás campos; con `idChofer` válido lo actualiza; con `idChofer` inexistente o inactivo devuelve `RecursoNoEncontrado` sin tocar el camión. Sin el `if` el primer caso falla. Suite completa de `logisticas-service`: 107 tests, 0 fallos, `BUILD SUCCESS`.

**Nota:** un `"idChofer": null` explícito tampoco desvincula (no se distingue de ausente); ningún caller dependía de eso. `POST /camiones` sigue ignorando `idChofer` (preexistente, fuera de este punto).

### 67. `PlanificadorRutasService.asignarChoferes` usaba `synchronized` local

**Estado:** corregido
**Severidad:** media
**Corregido:** 2026-11-08 · sin commit
**Archivos:** `logisticas-service/src/main/java/ar/edu/utn/frba/ddsi/logisticas/services/PlanificadorRutasService.java`, `logisticas-service/src/test/java/ar/edu/utn/frba/ddsi/logisticas/services/PlanificadorRutasServiceAsignarChoferesTest.java`

### Qué pasaba

`asignarChoferes` era `synchronized`, pero el monitor es por JVM. Con dos o más réplicas, dos callbacks concurrentes leían los mismos choferes disponibles y podían asignar el mismo chofer a dos rutas.

### Qué se cambió

Se reemplazó el lock local por un reclamo atómico en Mongo (`buscarChofer`): `findAndModify` con filtro `_id` + `disponible=true` + `activo≠false`, update `$set disponible=false` y `$inc version=1`, `returnNew(true)`. Si otra réplica ganó el claim, la ruta se reintenta con el próximo candidato. Si falla el guardado posterior, `liberarChofer` libera con CAS por `version` y se re-propaga la excepción original (con `addSuppressed` si la liberación también falla). La ruta se persiste al final, y una ruta sin camión ya no se persiste.

### Cómo se verificó

`PlanificadorRutasServiceAsignarChoferesTest` cubre el filtro/update del claim, el `returnNew`, el caso de otra réplica (claim `null`), el reintento con otro chofer, la ruta sin camión, la falla de parada (ruta no persistida + liberación), la liberación con CAS y la preservación de la excepción original. Sin el claim atómico el test de otra réplica falla. Suite completa de `logisticas-service`: 114 tests, 0 fallos, `BUILD SUCCESS`.

**Límites conocidos:** no hay transacción multi-documento; un crash entre el claim y el save, o un timeout con escritura ya aplicada, puede dejar un chofer ocupado o liberar de más. El camión no se compensa en fallos parciales. El filtro `_id` con UUID no tiene test contra Mongo real.

### 17. El planificador reseteaba la carga de los camiones y nunca la persistía

**Estado:** corregido
**Severidad:** media
**Corregido:** 2026-11-08 · sin commit
**Archivos:** `logisticas-service/src/main/java/ar/edu/utn/frba/ddsi/logisticas/models/entities/PlanificadorDeRutas/PlanificadorDeRutas.java`, `logisticas-service/src/test/java/ar/edu/utn/frba/ddsi/logisticas/models/entities/PlanificadorDeRutas/PlanificadorDeRutasTest.java`, `logisticas-service/src/test/java/ar/edu/utn/frba/ddsi/logisticas/services/PlanificadorRutasServicePersistenciaCargaTest.java` (nuevo)

### Qué pasaba

El simulador creaba copias locales de `Camion` y reseteaba su carga para asignar los ítems en memoria. La ocupación (`pesoOcupado`, `volumenOcupado`) se calculaba sólo sobre esas copias y nunca se escribía en `repoCamiones`: la base quedaba siempre en 0.

### Qué se cambió

Al reconstruir cada ruta en `PlanificadorDeRutas.procesarCallbackRutas` se setea la ocupación del camión desde `pesoTotalCargadoKg()` y `volumenTotalCargadoM3()` de la ruta. El `Camion` es el mismo objeto que luego `PlanificadorRutasService.asignarChoferes` guarda para las rutas que consiguen chofer; las rutas sin chofer no se persisten (punto 57).

### Cómo se verificó

`PlanificadorDeRutasTest` verifica que el camión queda con la carga de la ruta (10 kg / 0.01 m³) y `PlanificadorRutasServicePersistenciaCargaTest` cubre el camino completo callback → `asignarChoferes`, capturando el `Camion` que recibe `repoCamiones.save`. Sin los setters el test unitario falla con `expected: <10.0> but was: <0.0>`. Suite completa de `logisticas-service`: 116 tests, 0 fallos, `BUILD SUCCESS`.

**Límites conocidos:** en un fallo parcial el camión puede quedar guardado con la ocupación de una ruta que no se persistió. El invariante “disponible ⇒ vacío” no lo fuerza `CamionService.cambiarDisponibilidad`. `ciudadDestinoActual` no se persiste (hoy nadie lee el valor de la base).

### 37. El CRUD de camiones/choferes devolvía 500 para errores de validación

**Estado:** corregido
**Severidad:** baja
**Corregido:** 2026-11-08 · sin commit
**Archivos:** `logisticas-service/pom.xml`, `dto/camion/CamionDTO.java`, `dto/camion/CamionesDTO.java`, `dto/chofer/ChoferDTO.java`, `dto/chofer/ChoferesDTO.java`, `controllers/CamionController.java`, `controllers/ChoferController.java`, `controllers/GlobalExceptionHandler.java`, `services/CamionService.java`, `test/.../controllers/ValidacionControllersTest.java` (nuevo), `test/.../services/CamionServiceConvertirDTOTest.java` (nuevo)

### Qué pasaba

`POST /api/camiones` y `POST /api/choferes` (y los endpoints de registro masivo) no validaban campos requeridos. Un payload incompleto llegaba al servicio y terminaba en excepción de integridad/NPE, traducida a 500.

### Qué se cambió

Se agregó `spring-boot-starter-validation` y constraints en los DTO (`patente`, capacidades y `nombre` obligatorios; listas masivas `@NotNull` + `@Valid` en cascada, incluidos elementos `null`). Los POST usan `@Valid`; los PUT quedaron sin validar para preservar la actualización parcial. `GlobalExceptionHandler` traduce `MethodArgumentNotValidException` a 400 con `campo: mensaje`. Además, si un camión nuevo no trae `disponible`, queda en `true` (igual que choferes y evita el NPE del scheduler).

### Cómo se verificó

`ValidacionControllersTest` cubre POST incompletos (400, sin tocar el servicio), cascada en los masivos, elemento `null`, payloads válidos (201) y el cuerpo `campo: mensaje`. `CamionServiceConvertirDTOTest` cubre el default de `disponible`. Sin `@Valid` los POST incompletos devuelven 201 y los tests fallan. Suite completa de `logisticas-service`: 128 tests, 0 fallos, `BUILD SUCCESS`.

**Límites conocidos:** no se validan rangos (`@Positive`); documentos viejos con `disponible: null` en Mongo podrían romper al scheduler (preexistente).

### 40. Los gestores mezclaban reglas de negocio con persistencia

**Estado:** corregido (alcance: `GestorCamiones`)
**Severidad:** baja
**Corregido:** 2026-11-08 · sin commit
**Archivos:** `logisticas-service/src/main/java/ar/edu/utn/frba/ddsi/logisticas/models/gestores/GestorCamiones.java`, `.../services/CamionService.java`, `.../test/.../services/CamionServiceUpdateChoferTest.java`

### Qué pasaba

`GestorCamiones.actualizarCamion` hacía `findById` y `save` además de aplicar los cambios del request. La persistencia quedaba decidida por el gestor y no por el service.

### Qué se cambió

El gestor quedó puro: `actualizarCamion` recibe el `Camion` ya cargado, aplica los cambios y lo devuelve; `resetearCamion` ya era puro. `CamionService.update` resuelve el chofer, carga el camión (404 si no existe), llama al gestor y persiste. Se conservó la precedencia original de los errores (chofer antes que camión). Se agregaron tests para patente inexistente y se verifica que los caminos de error no guardan.

### Cómo se verificó

Comportamiento sin cambios; lo cubren `CamionServiceUpdateChoferTest` (5 casos) y la suite completa de `logisticas-service`: 129 tests, 0 fallos, `BUILD SUCCESS`.

**Excepción documentada:** `GestorPublicacionEventos` conserva sus repositorios porque implementa el patrón outbox (persiste el evento antes de publicar en `afterCommit`) y en `publicarReingresoDeposito` guarda el ítem que `RutaService.terminarRuta` no persiste. Su responsabilidad es homogénea (publicación de eventos); separar la persistencia obligaría a mover la semántica transaccional a los services sin beneficio funcional.

### 42. Paquetes de primer nivel en PascalCase (`RabbitMQ/`, `Scheduler/`)

**Estado:** corregido
**Severidad:** baja
**Corregido:** 2026-11-08 · sin commit
**Archivos:** `logisticas-service/src/main/java/ar/edu/utn/frba/ddsi/logisticas/messaging/DonacionListener.java`, `.../scheduler/PlanificadorDeRutasScheduler.java`, tests homónimos, `controllers/PlanificadorDeRutasController.java`, `ConfiguracionArranqueTest.java`, `controllers/PlanificadorDeRutasControllerTest.java`

### Qué pasaba

`RabbitMQ/` y `Scheduler/` eran paquetes en PascalCase en la raíz del código, contra la convención Java y el resto de los microservicios.

### Qué se cambió

`DonacionListener` pasó al paquete existente `messaging` (junto a `ProductorEventosLogistica`) y `PlanificadorDeRutasScheduler` a `scheduler`. Se actualizaron las declaraciones `package` y los imports de main y tests. No se tocó lógica ni configuración; los beans siguen bajo el paquete base escaneado por `@SpringBootApplication`.

### Cómo se verificó

`git mv` preservó los cuatro archivos como renames; no quedan referencias a `logisticas.RabbitMQ` ni `logisticas.Scheduler` (grep) ni directorios viejos. Los tests se descubren en los nuevos paquetes. Suite completa de `logisticas-service`: 129 tests, 0 fallos, `BUILD SUCCESS`.

**Límites conocidos:** el rename `Scheduler/` → `scheduler/` difiere sólo en mayúsculas; conviene verificar un checkout en filesystem case-insensitive (Windows/macOS). No hay test de contexto que detecte un component scan roto; la verificación de referencias fue por grep.
