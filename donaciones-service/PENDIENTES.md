# Pendientes técnicos de `donaciones-service`

## Revisión del enunciado hasta Entrega 6

El backlog técnico detallado debajo se mantiene como lista de trabajo del servicio. Contrastado con
E1-E5: existen dominio de donaciones, segmentación, necesidades, importación CSV, estados e historial,
APIs REST y persistencia relacional. Siguen abiertos los riesgos ya enumerados, especialmente estados
faltantes/trazabilidad, fecha real de entrega, consistencia del matchmaking, transacciones y notificación
asíncrona. Para cerrar los entregables del TP también falta versionar diagramas de clases/ERD y contratos
API actualizados (ver backlog raíz). E6 corresponde principalmente a `front-service`.

---

Registro de problemas conocidos del servicio, con el motivo y la propuesta de arreglo para
que no se pierdan de vista al crecer el código.

**Están ordenados de más urgente a menos urgente**, no por número de punto. El número es un
ID estable y no se renumera nunca, así que quedan huecos. Un punto corregido se borra de esta
lista y pasa a la sección [Corregidos](#corregidos) del final.

No hay pendientes abiertos.

---

## Corregidos

- **43.** "Consultar rankings" no está expuesto en donaciones — descartado por diseño: el ranking de donantes vive en `incentivos-service` (`GET /api/rankings/...`) y donaciones delega esa consulta.

- **37.** Se eliminaron las 15 fachadas de `models/repositories/repos/`; services y gestores inyectan las interfaces `RepositorioDe...` directo, con `default` methods para la lógica de cada entidad. La lógica cruzada se movió a `GestorCatalogo` (país/provincia/ciudad y categoría/subcategoría) y a `GestorAsignaciones.agregarDonacionANecesidad`; el claim de eventos quedó como `INSERT` nativo en `RepositorioDeEventosProcesados`. En incentivos, el buffer en memoria `RepositorioNotificacionesPendientes` se movió a `models/gestores/NotificacionesPendientes`. Tests: donaciones 197, incentivos 337.

- **36.** Se normalizaron las subcarpetas de donaciones a minúsculas: `models/sheduler`→`models/scheduler`, `exceptions/CsvExceptions`→`exceptions/csvexceptions`, `RabbitMQ`→`messaging`, `dto/entidadBeneficiaria`→`dto/entidadbeneficiaria`, `dto/personaDonante`→`dto/personadonante`, y los DTOs sueltos (`DireccionDTO`, `PropuestaAsignacionDTO`, `ResultadoMatchmakingDTO`, `AsignarPropuestaRequestDTO`) a `dto/direccion` y `dto/matchmaking`. `models/entities` se mantiene PascalCase como en incentivos. Sin cambios de comportamiento; suite completa en verde.

- **76.** `GeocodingScheduler` consultaba `DireccionJpaRepository` directo, salteando el gestor — la búsqueda de pendientes se movió a `GestorGeoref.buscarPendientes(...)`; el scheduler solo inyecta el gestor, `GeorefClient` sigue siendo el único HTTP y el repositorio la persistencia. Cobertura en `GeocodingSchedulerTest`.

- **82.** El matchmaking seguía navegando `necesidad.getDonaciones()`/`donacion.getBienes()` para compatibilidad y score — ahora `DonacionJpaRepository` agrega entregas por necesidad y fecha (`SUM(bien.peso)`), `DonacionService` precomputa la cantidad vigente por necesidad una vez por corrida (histórico para extraordinarias, ventana de `plazoEnDias` para recurrentes) y los algoritmos reciben ese mapa; `Necesidad` tiene overloads `esCompatibleCon(..., cantidad)`/`cantidadFaltante(cantidad)` y el camino viejo queda solo como fallback para llamadas directas/tests. Cobertura en `MatchmakingCantidadesVigentesTest`.

- **79.** `spring.jpa.show-sql`/`format_sql` quedaban prendidos, sin batch y con prefetch 250 de Rabbit; `EventosListener` cargaba administradores en cada evento — apagados por defecto (con `application-dev.properties` para dev), `hibernate.jdbc.batch_size=50` + `order_inserts`/`order_updates`, `prefetch=10` y `concurrency=1`/`max-concurrency=3`, y los admins se cargan solo para `ENTREGA_FALLIDA`. Cobertura en `EventosListenerTest`.

- **39.** `DonacionController` inyectaba `RabbitTemplate` sin usarlo — se eliminó la dependencia y se actualizaron los dos tests que lo construían; la publicación sigue en services/messaging.

- **81 (listados/OSIV).** Los listados expandían `entidad → necesidades → donaciones → bienes` y `open-in-view` mantenía la sesión abierta durante el mapeo/serialización — `EntidadBeneficiariaDTO` ya no expone `necesidades` y `NecesidadDTO` ya no expone `donaciones`; `PropuestaAsignacionDTO`/`ResultadoMatchmakingDTO` usan resúmenes (`EntidadBeneficiariaResumenDTO`, `NecesidadResumenDTO`, `DonacionResumenDTO`) y `GET /entidades/{id}/donaciones` devuelve `DonacionResumenDTO`. Se apagó `spring.jpa.open-in-view` y los read services abren `@Transactional(readOnly = true)` para mapear dentro de la sesión; `publicarEntregaALogistica` arma el `EntregaDTO` en una transacción nueva porque corre en `afterCommit`. Cobertura en `ListadosAplanadosTest`. El N+1 residual del matchmaking queda en el punto 82.

- **78.** La importación CSV mantenía en memoria el archivo completo (`getBytes()`) y todos los `Donante` convertidos — ahora copia el multipart a un archivo temporal, `LectorCSV` tiene un modo streaming (`importar(InputStream, Consumer<T>)`) que entrega fila por fila sin acumular, la persistencia ocurre en el callback y los perfiles a incentivos se envían por bloques de 500 (con flush final y de lo pendiente si la lectura falla). El temporal se borra en el worker y en el rechazo de la cola. Cobertura en `ImportacionSegundoPlanoTest` (no llama `getBytes`) e `ImportarCsvConMapeosTest` (streaming no retiene filas).

- **67.** `ActividadDonanteScheduler` evaluaba `p.getFormularios().getLast()` (orden arbitrario) y volvía a notificar todos los días a cualquier inactivo — agregado `ultimaNotificacionInactividad` a `Donante`; la query usa `MAX(fechaRealizacion)` y solo se avisa una vez por racha de inactividad, reavisando si el donante vuelve a donar. Cobertura en `DonanteServiceTest`.

- **80.** `revisarActividades` hacía `findAll()` de donantes y navegaba los formularios por donante (N+1) — ahora usa `buscarInactivosParaNotificar(limite, Pageable)` con `MAX(fechaRealizacion)`, procesa hasta 500 por corrida y no carga la colección de formularios. Cobertura en `DonanteServiceTest`.

- **53.** `POST /donaciones/asignar` notificaba dos veces por destinatario: `GestorAsignaciones.asignarPropuesta` publicaba directo a los medios predeterminados y el `cambiarEstado("ASIGNADO")` volvía a disparar la estrategia `DONACION_ASIGNADA` a todos los medios — se eliminó la notificación directa (y la dependencia muerta de `NotificacionesClient` en el gestor); el único camino es el cambio de estado, que notifica una vez a entidad y donante. Cobertura en `NotificacionAsignacionUnicaTest`, que verifica 2 avisos y fallaba con 4 contra el código anterior.

- **65.** `PUT /api/admins/{id}` insertaba una `Humana` nueva en cada actualización y dejaba huérfana la anterior — `AdminService.actualizarAdmin` ya no reasigna `existente.setHumano(datosNuevos.getHumano())`; cuando el admin ya tiene persona, `modificarPersona` actualiza la instancia gestionada y se conserva la identidad. La rama que registra una persona cuando el admin no tenía ninguna se mantiene. Cobertura en `AdminServiceTest`.

- **68.** `GET /api/entidades/{id}/donaciones` respondía 200 con `[]` cuando la entidad no existía — `obtenerDonacionesDeEntidad` ahora lanza `IllegalArgumentException` si `buscarPorId` está vacío, y el controller lo traduce a 404 como el resto de los métodos del servicio. Cobertura en `EntidadBeneficiariaServiceTest`.

- **57.** Los POST/DELETE de medios de contacto aceptaban nulos o vacíos y el POST respondía 201 — `@NotBlank` en `tipo`/`valor` y `@Valid` en el controller, `toDomain()` rechaza null/blank y tipos no soportados, y `MediosDeContacto.agregarMedioDeContacto` rechaza null. Cobertura en `MediosContactoDTOTest` y `MediosDeContactoTest`.

- **54.** `DELETE /donaciones/{id}` daba 409 si la donación tenía `ResultadoMatchmaking` — `eliminarDonacion` ahora borra primero el resultado (`findByDonacionId` + `eliminarResultado`) y después la donación, dentro de la misma transacción. Cobertura en `EliminarDonacionTest`.

- **76 (notificación).** La notificación de impacto a incentivos se perdía si incentivos estaba caído: el `afterCommit` solo logueaba el warning y el evento de Rabbit ya estaba ACKeado — agregada una outbox durable (`notificacion_incentivos_pendiente`) que guarda el impacto en la misma transacción de la entrega, y también la actualización de perfil de `actualizarPersona`. El `GestorIncentivos` hace un intento inmediato post-commit y un `@Scheduled` reintenta lo pendiente con lease de 60 s y backoff exponencial (30 s a 1 h); `IncentivosClient.actualizarPerfil` ahora relanza para que el reintento no se pierda. Cobertura en `GestorIncentivosTest`, `GestorIncentivosTransactionTest` y `ContratoIncentivosTest` (impacto encolado sin llamada directa).

- **77 (matchmaking).** El barrido de matchmaking cargaba todas las donaciones y entidades y disparaba N+1 al navegar necesidades/donaciones/bienes — ahora pide solo los ids de las donaciones `EN_DEPOSITO` (`findIdsByEstado`), carga las entidades activas con sus necesidades en una sola query (`@EntityGraph`) en vez de `findAll()`, usa `@BatchSize(50)` para las colecciones de donaciones y bienes, y calcula el conteo de entregadas del último trimestre con una única consulta agregada por corrida que `SubAtendidos` recibe como mapa (sin `verDonaciones()`). Los listados y `open-in-view` quedan en el punto 81. Cobertura en `MatchmakingSweepTest` y `SubAtendidosConteoTest`.

- **75 (geocoding).** `Direccion` no guardaba coordenadas y `getLatitud()`/`getLongitud()` devolvían un placeholder - agregados `latitud`/`longitud` persistentes y `GeorefClient`/`GestorGeoref` sobre la API pública Georef (POST `/direcciones` en lotes de hasta 1000, deduplicados, `max = 1` y reintento sin localidad). `GestorGeoref` coordina el alta y el PUT: geocodifica después del commit y en una transacción corta con lock pesimista (no se mantiene abierta la transacción durante el HTTP y no se pisan coordenadas entre instancias), conserva coordenadas y backoff si la dirección textual no cambió, y deja la operación de negocio en pie si Georef falla. `GeocodingScheduler` corre a las 00:30, limita la corrida con `geocoding.cron.max-por-corrida`, aplica backoff de 7 días y hasta 3 intentos, y distingue "sin resultado" de "API caída" (la caída no consume intento). `anonimizar()` borra las coordenadas y `DireccionDTO` las transporta a logística tanto en entregas como en paradas; los GET de donantes no publican las coordenadas (el PUT las conserva si la dirección textual no cambió). Cobertura en `GestorGeorefTest`, `GeocodingSchedulerTest`, `GeorefClientTest`, `DireccionTest` y `DireccionDTOTest`.

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
- **45.** `EventosListener`: transacción y requeue en hot loop - aislada la transacción en `GestorEventosLogistica`, configurado retry/backoff en `application.properties` con `default-requeue-rejected=false`, y cursor `ultimoIdProcesado` actualizado solo tras commit exitoso.
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
- **8.** No había transacciones — `@Transactional(rollbackFor)` en `DonacionService`; los demás services se cubrieron después con la misma anotación.
- **7.** Un bien sin `tipoBien` reventaba la segmentación — excepción con el índice del item.
- **6.** Una estrategia de notificación no era bean — `@Component` + chequeo de la fábrica al arrancar contra el enum.
- **5.** `PATCH /vencer` mandaba "VENCIDA" y el parser conoce "VENCIDO" — corregido el string (el parser sigue aceptando strings).
- **4.** Dependencia de notificaciones-service en Maven — eliminada de logisticas-service/pom.xml (no pertenecía al módulo de donaciones).
- **Clientes (sin ID).** Apuntaban a la raíz de los servicios — rutas reales (`POST /api/perfiles`, `PATCH /api/perfiles/donacion/{id}`) y notificaciones migradas a Rabbit.
- **Clientes (sin ID).** Se tragaban las excepciones de salida sin log — ahora loguean y relanzan.
- **Rabbit (sin ID).** Dos bindings ataban al exchange equivocado y usaban comodines `.#` — cada servicio declara lo suyo con la clave exacta.
- **Properties (sin ID).** URLs default sin context-path y faltaba el bloque `spring.rabbitmq.*` — parametrizados por entorno.
- **64.** `ResultadoMatchmaking` colisionaba al re-matchear: el reemplazo se hace al inicio del bucle de `ejecutarMatchmakingBatch` (borra el previo antes de procesar), `eliminarResultado` ahora hace `flush`, y `guardar` es un save puro. Cubierto por `RepositorioDeResultadosMatchmakingTest` y `AsignadorDonacionesReMatchmakingTest`.
- **74.** `AsignacionScheduler` transaccionaba todo el lote junto: un fallo en una donación tumbaba el batch por rollback-only — `DonacionService.ejecutarMatchmakingADemanda` ahora procesa cada donación dentro de un `TransactionTemplate` con `PROPAGATION_REQUIRES_NEW`, así un error de integridad solo revierte esa donación; el scheduler mantiene su `@Transactional` (punto 44) para la lectura inicial.
- **42.** Estados del enunciado: se agregaron `LISTA_PARA_ENTREGAR` y `ENTREGA_FALLIDA` al enum; `manejarEntregaFallida` registra `ENTREGA_FALLIDA`, `REINGRESO_DEPOSITO` vuelve a `EN_DEPOSITO`, y `asignarPropuesta` deja el estado final en `LISTA_PARA_ENTREGAR`.
- **62.** `fechaEntrega` prematura en depósito: `SegmentadorDonaciones` la deja en `null` (se setea al entregar) y `SubAtendidos` filtra por `Estado.ENTREGADO`.
- **63.** Fallback de matchmaking sin intersección: se renumeran las posiciones secuencialmente para evitar asignar la propuesta equivocada.
