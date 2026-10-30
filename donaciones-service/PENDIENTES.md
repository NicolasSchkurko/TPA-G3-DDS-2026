# Pendientes técnicos de `donaciones-service`

Registro de problemas conocidos del servicio, con el motivo y la propuesta de arreglo para
que no se pierdan de vista al crecer el código.

**Están ordenados de más urgente a menos urgente**, no por número de punto. El número es un
ID estable y no se renumera nunca, así que quedan huecos. Un punto corregido se borra de esta
lista y pasa a la sección [Corregidos](#corregidos) del final.

El orden no es el de la severidad declarada en cada punto sino el del daño real: cuánto se
rompe cuando pasa, y qué tan fácil es que pase.

| #  | Punto | Severidad | Por qué está acá                                                                                  |
|----|-------|-----------|---------------------------------------------------------------------------------------------------|
| 1  | 49    | Media     | El `RestTemplate` de integración no tiene timeouts: un servicio colgado bloquea todo              |
| 2  | 50    | Media     | La publicación a logística va dentro de la transacción y a un exchange que este servicio no declara |
| 3  | 51    | Media     | `EntidadBeneficiariaService` sin transacciones: altas y bajas de necesidades quedan a medias      |
| 4  | 52    | Media     | El alta de donante crea el perfil en incentivos y notifica antes de persistir localmente          |
| 5  | 54    | Media     | `DELETE /donaciones/{id}` responde 409 si la donación tiene resultado de matchmaking              |
| 6  | 55    | Media     | El catch-all del `GlobalExceptionHandler` convierte errores de cliente en 500 y filtra internals  |
| 7  | 56    | Media     | Los PUT de persona, entidad y admin traducen validación a 404 mudo                                |
| 8  | 41    | Media     | La modificación de donante ignora campos y la baja deja la Persona huérfana                       |
| 9  | 42    | Media     | Faltan "Lista para entregar" y "Entrega fallida" en el enum de estados                            |
| 10 | 53    | Media     | La asignación notifica dos veces: mensaje directo y estrategia de notificación                    |
| 11 | 38    | Media     | Los listados devuelven la colección entera: falta paginación                                      |
| 12 | 58    | Baja      | Declaración duplicada de dependencia `spring-boot-starter-validation` en `pom.xml`                |
| 13 | 57    | Baja      | Los POST de medios de contacto aceptan nulos/vacíos y responden 201                               |
| 14 | 39    | Baja      | `DonacionController` inyecta `RabbitTemplate` sin usarlo                                          |
| 15 | 36    | Baja      | La nomenclatura de subcarpetas no coincide con la de incentivos                                   |
| 16 | 37    | Baja      | Los repositorios usan dos convenciones distintas entre servicios                                  |
| 17 | 43    | Baja      | "Consultar rankings" no está expuesto en donaciones: confirmar alcance                            |

---

## 49. El `RestTemplate` de integración no tiene timeouts: un servicio colgado bloquea todo

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/DonacionesServiceApplication.java` (líneas 36-42), `.../clients/IncentivosClient.java` (líneas 53-115)

### Qué pasa

El único bean de transporte se construye así:

```java
return new RestTemplate(new JdkClientHttpRequestFactory(HttpClient.newHttpClient()));
```

`HttpClient.newHttpClient()` no setea `connectTimeout` (el JDK no tiene default) y a la `JdkClientHttpRequestFactory` nadie le llama `setReadTimeout`. No hay `RestTemplateBuilder` ni propiedades que lo compensen.

### Cómo se dispara

Incentivos (u otro host) acepta el TCP y nunca responde, o la ruta cae en un blackhole: el request queda colgado **sin cota**. Afecta a `POST /api/personas`, `DELETE /api/personas/{id}`, al `@RabbitListener` de eventos (que llama a incentivos y tiene concurrency 1: la cola de eventos deja de consumirse) y a los 2 hilos del pool de importación CSV.

### Por qué no se ve

Con el otro servicio **caído** el connect falla rápido y el error aparece; el cuelgue (overload, red que descarta paquetes) es el caso que no aparece en una demo.

### Propuesta

Configurar `HttpClient.connectTimeout` (2-5s) y `JdkClientHttpRequestFactory.setReadTimeout` (5-10s), parametrizables por properties, y un backoff/aislamiento para el listener.

---

## 50. La publicación a logística va dentro de la transacción y a un exchange que este servicio no declara

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/DonacionService.java` (líneas 170-181 y 186-229, publicación en 228), `.../messaging/ProductorLogistica.java` (líneas 47-70), `.../config/RabbitMQConfig.java` (líneas 23-28)

### Qué pasa

`asignarPropuesta` es `@Transactional(rollbackFor = Exception.class)` y publica al final del método: `ProductorLogistica.publicarDonacionAsignada` hace `convertAndSend` al exchange `logistica.integracion.hash` **sin try/catch** (relanza a propósito: si no se publica, nadie entrega la donación). Pero ese exchange lo declara logística (`CustomExchange` en su `RabbitMQConfig`): donaciones solo tiene la constante y el comentario —la nota del Corregido #2 dice que este módulo lo declara, pero el bean no existe—.

Consecuencias:

- **Logística nunca arrancó** (broker sin el exchange): `convertAndSend` tira `AmqpException` → 500 y **rollback** de la asignación… pero las notificaciones al donante y a la entidad, que salen antes en el mismo flujo y tragan sus errores, **ya se enviaron**: se avisó una asignación que quedó sin efecto.
- **El commit falla después de publicar** (constraint diferido, caída de DB): el mensaje a logística ya salió y describe un estado que se revirtió. El javadoc del productor ("si no se publica, la donación queda asignada") describe lo contrario del rollback real.

### Por qué no se ve

En la demo los cuatro servicios suelen estar arriba y logística declaró el exchange al arrancar. El problema aparece cuando donaciones corre solo (o logística todavía no subió).

### Propuesta

Declarar el exchange en `RabbitMQConfig` (es de los que este servicio publica: la misma regla que dejó el Corregido #2) y sacar la publicación del cuerpo transaccional: publicar en `afterCommit` o con un outbox, para que el mensaje salga solo si la asignación quedó persistida.

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

## 52. El alta de donante crea el perfil en incentivos y notifica antes de persistir localmente

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/DonanteService.java` (líneas 70-79, 87-94 y 257-263), `.../clients/IncentivosClient.java` (líneas 58-69), `.../models/entities/ServicioMensaje/EstrategiasMensajes/NotificacionRegistroPersona.java` (líneas 35-38), `.../models/entities/ServicioNotificaciones/ServicioNotificaciones.java` (líneas 39-45)

### Qué pasa

`crearPersona` ejecuta primero `incentivosClient.peticionCrearPerfil(perfilDe(nuevoDonante))` y **después** `persistirConNotificacion`, que a su vez ejecuta la notificación `REGISTRO_PERSONA` **antes** de `registrarPersona`/`registrarDonante`. Con un donante humano sin medios válidos (p. ej. `POST /api/personas` sin `mediosDeContacto`, o con `{"tipo":"EMAIL"}` sin valor, que el DTO descarta en silencio), la notificación tira `IllegalArgumentException("No hay un medio de contacto predeterminado...")` → **400**, no se persiste nada local… y el perfil en incentivos **ya quedó creado**. Reintentar con otro body genera otro UUID → otro perfil; el huérfano no se limpia (mismo daño de fondo que el punto 35, pero en el alta).

A esto se suma que `DonanteService` no tiene `@Transactional`: cualquier fallo entre los dos `save` deja `Persona` sin `Donante` (o al revés) e incentivos desincronizado.

### Por qué no se ve

El 400 se lee como "body inválido" y nadie mira el otro servicio; el perfil fantasma puede aparecer en rankings y recibir notificaciones.

### Propuesta

Validar y persistir primero, en una transacción; recién después crear el perfil externo y notificar (idealmente por evento/outbox, como discute el punto 35).

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

## 55. El catch-all del `GlobalExceptionHandler` convierte errores de cliente en 500 y filtra internals

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/exceptions/GlobalExceptionHandler.java` (líneas 59-69)

### Qué pasa

El advice no extiende `ResponseEntityExceptionHandler` y declara `@ExceptionHandler(Exception.class)`, así que además del catch-all de negocio captura las excepciones estándar de Spring MVC que no están mapeadas: `HttpMessageNotReadableException` (JSON mal formado o fecha con formato inválido), `MethodArgumentTypeMismatchException` (UUID de path inválido), `HttpRequestMethodNotSupportedException`, `NoResourceFoundException`. Todas responden **500** con `ex.getMessage()` de Jackson/Spring, que incluye tipos Java y el detalle del parseo.

### Cómo se dispara

- `POST /api/donaciones/formulario` con `"fechaRealizacion":"ayer"` → 500 (debería 400).
- `GET /api/personas/abc` → 500 (debería 400).
- `DELETE /api/donaciones` → 500 (debería 405).
- `GET /api/ruta-inexistente` → 500 (debería 404).

### Por qué no se ve

Un 500 genérico en el cliente se lee como "el servicio se rompió" y se reintenta o se escala; la causa real es un request mal formado.

### Propuesta

Extender `ResponseEntityExceptionHandler` (o agregar handlers para esas excepciones) para devolver 400/404/405, y en el catch-all devolver un mensaje genérico en vez de `getMessage()` crudo.

---

## 56. Los PUT de persona, entidad y admin traducen validación a 404 mudo

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/controllers/DonanteController.java` (líneas 74-82), `.../controllers/EntidadBeneficiariaController.java` (líneas 64-72), `.../controllers/AdminController.java` (líneas 50-58); DTOs: `.../dto/personaDonante/PersonaDonanteDTO.java` (líneas 118-135), `.../dto/entidadBeneficiaria/EntidadBeneficiariaDTO.java` (líneas 29-34), `.../dto/admin/AdminDTO.java` (líneas 25-31)

### Qué pasa

A diferencia de `DonacionController` (arreglado al corregir el punto 21), estos tres controllers siguen con `catch (IllegalArgumentException e) { return ResponseEntity.notFound().build(); }`. Pero la `IllegalArgumentException` no es solo "no existe": los DTO también la usan para validar el body:

- `PersonaDonanteDTO`: `"Tipo de persona inválido"` y `Genero.valueOf(...)` con un género fuera del enum.
- `EntidadBeneficiariaDTO`: `"La razón social es obligatoria..."`.
- `AdminDTO`: `"El administrador debe tener un medio de contacto asignado."`.

Ese catch local convierte un body inválido en un **404 sin cuerpo**, idéntico a "el recurso no existe", y saltea el handler global que mapea IAE → 400.

### Cómo se dispara

- `PUT /api/personas/{idExistente}` con `{"tipoPersona":"MASCOTA"}` → 404 (debería 400).
- `PUT /api/entidades/{idExistente}` sin razón social → 404.
- `PUT /api/admins/{idExistente}` con `{}` → 404.

### Propuesta

Quitar los catches de esos tres controllers y dejar que el `GlobalExceptionHandler` mapee por tipo (IAE → 400, `EntityNotFoundException` → 404), como ya se hizo en `DonacionController`.

---

## 41. La modificación de donante ignora campos y la baja deja la Persona huérfana

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/repositories/repos/RepositorioDonantes.java` (`modificarDonante`), `.../models/repositories/repos/RepositorioPersonas.java` (`modificarPersona`), `.../services/DonanteService.java` (`actualizarPersona`, `eliminarPersona`)

### Qué pasa

El enunciado pide alta, baja y modificación de donantes humanas y jurídicas. La modificación guarda poco y responde 200 (el cliente cree que guardó):

- `RepositorioDonantes.modificarDonante` solo pisa `direccion`.
- `RepositorioPersonas.modificarPersona` solo pisa `mediosDeContacto` y, si es jurídica, `razonSocial`/`cuit`. Para humanas no se actualizan `nombre`, `apellido`, `documento`, `edad` ni `genero`; para jurídicas se ignoran `rubro`, `tipoJuridico` y `representantes`.

Y la baja (`eliminarPersona`) borra el `Donante` pero **no la `Persona`**, que queda huérfana en la base. (El 409 por formularios asociados está en el punto 34; el id del PUT, en el punto 13.)

### Propuesta

Completar el update campo por campo según el tipo de persona, o devolver 400 por lo que hoy se ignora en silencio. En la baja, borrar también la `Persona` en la misma transacción; la política de bajas con historial se discute en los puntos 34 y 35.

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

## 38. Los listados devuelven la colección entera: falta paginación

**Estado:** abierto
**Severidad:** media
**Archivos:** `controllers/DonacionController.java`, `controllers/DonanteController.java`, `controllers/EntidadBeneficiariaController.java` y los services/fachadas de cada listado

### Qué pasa

Los endpoints que devuelven colecciones lo hacen con `List<...>` completo, sin `Pageable` ni paginación. El volumen no es teórico: la importación CSV cargó 499 donantes y `GET /api/personas` los devuelve todos en un solo payload.

Endpoints que correspondería paginar (listados que crecen sin techo):

- `GET /api/donaciones` → `DonacionService.obtenerTodas()`
- `GET /api/donaciones/formularios` → `DonacionService.obtenerFormularios()`
- `GET /api/donaciones/pendientes` → `DonacionService.obtenerTodosLosResultadosMatchmaking()`
- `GET /api/personas` → `DonanteService.listarTodas()`
- `GET /api/entidades` → `EntidadBeneficiariaService.obtenerTodas()`

En incentivos ya se pagina con `Page`/`Pageable` en misiones, categorías, insignias e historial de rankings; la excepción es el detalle del ranking (`RankingMesDTO.ranking` sigue siendo una lista completa). Los sublistados acotados por recurso (`/{id}/medios-contacto`, `/{id}/necesidades`, `/{id}/donaciones`) pueden quedar como están: crecen con el recurso, no con el sistema.

### Por qué no se ve

Con pocos datos de demo no molesta; el problema aparece con los datos reales. Y como el contrato actual es un array plano, cambiar a `Page` cambia la forma de la respuesta: hay que actualizar en el mismo cambio a los consumidores —la collection `servicio donaciones` (cinco requests iteran las listas para limpiar) y el front—.

### Propuesta

Sumar paginación en los cinco listados: `Pageable` en el controller (default chico, 10-20 por página, como incentivos), `Page<DTO>` en service y fachada (`findAll(pageable)`), y actualizar los consumidores. Los tests actuales no tocan estos métodos, así que el cambio no rompe la suite; el riesgo es todo de contrato.

---

## 58. Declaración duplicada de dependencia `spring-boot-starter-validation` en `pom.xml`

**Estado:** abierto
**Severidad:** baja
**Archivos:** `pom.xml:43-46`

### Qué pasa

En `donaciones-service/pom.xml`, el starter `org.springframework.boot:spring-boot-starter-validation` está declarado dos veces (en las líneas 34-36 y en las líneas 43-46).

Maven emite una advertencia formal durante la fase de análisis del modelo efectivo:

```text
[WARNING] 'dependencies.dependency.(groupId:artifactId:type:classifier)' must be unique: org.springframework.boot:spring-boot-starter-validation:jar -> duplicate declaration of version (?) @ line 43, column 21
[WARNING] It is highly recommended to fix these problems because they threaten the stability of your build.
[WARNING] For this reason, future Maven versions might no longer support building such malformed projects.
```

### Cómo se dispara

Cualquier comando de compilación o empaquetado Maven (`mvn clean compile`, `mvn verify`).

### Propuesta

Remover el bloque `<dependency>` duplicado de `spring-boot-starter-validation` en las líneas 43-46.

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

## 39. `DonacionController` inyecta `RabbitTemplate` sin usarlo

**Estado:** abierto
**Severidad:** baja
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/controllers/DonacionController.java`

### Qué pasa

El controller declara `private final RabbitTemplate rabbitTemplate` y lo recibe por constructor, pero no lo usa en ningún método: es una inyección muerta. Además deja la infraestructura de mensajería en la capa de controllers. En incentivos ningún controller conoce el broker: los controllers solo inyectan services, y las publicaciones pasan por services/clients.

### Propuesta

Quitar la dependencia del controller. Si en algún momento hace falta publicar desde un endpoint, que salga por el service correspondiente (o por `messaging/`), no por el controller.

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

## 43. "Consultar rankings" no está expuesto en donaciones: confirmar alcance

**Estado:** abierto
**Severidad:** baja
**Archivos:** controllers de donaciones (no hay endpoint)

### Qué pasa

El enunciado del servicio incluye, en la exposición REST, "permite consultar rankings". En donaciones no hay ningún endpoint de rankings: el ranking de donantes vive en `incentivos-service` (`GET /api/rankings/...`), y lo más cercano acá es `GET /api/donaciones/pendientes`, que devuelve las propuestas de matchmaking ordenadas (con posición y score) pero no un ranking.

### Propuesta

Definir si la consulta queda delegada en incentivos-service (dejarlo documentado) o si donaciones debería exponer un ranking propio (p. ej. el de sub-atendidos que ya calcula el matchmaking).

---

## Corregidos


- **47.** `REINGRESO_DEPOSITO` dejaba la donación varada en `PENDIENTE_ASIGNACION` sin resultado de matchmaking - actualizado el estado a `EN_DEPOSITO` en `GestorEventosLogistica.manejarReingresoDeposito` para que el scheduler de matchmaking la recoja automáticamente.
- **48.** `DELETE /personas/{id}/medios-contacto` no borraba nada y respondía 302 - implementado equals/hashCode por tipo+valor en `MedioDeContacto`, `removeIf` en `MediosDeContacto`, retorno 200 OK en controller.
- **46.** `PUT /api/admins/{id}` insertaba un admin nuevo en cada modificación - `AdminService` reutiliza la entidad original y `RepositorioAdministradores.actualizar` fuerza `idOriginal` antes de `save()`.
- **45.** `EventosListener`: transacción y requeue en hot loop - aislada la transacción en `GestorEventosLogistica`, configurado retry/backoff en `application.properties` con `default-requeue-rejected=false`, y cursor `ultimoIdProcesado` actualizado solo tras commit exitoso.
- **44.** Los schedulers corrían sin transacción (LazyInitializationException) - `@Transactional` agregado en `AsignacionScheduler.ejecutarAsignacion`, `ActividadDonanteScheduler.revisarActividadDonantes` y `DonanteService.revisarActividades`.
- **35.** Dar de baja un donante no borraba confiablemente su perfil en incentivos - local DB commit se asegura primero con `@Transactional` + `flush()`, y llamada a `incentivosClient.eliminarPerfil` se sincroniza `afterCommit` con `try/catch` para no abortar ante caída externa.
- **34.** Tras donar, el donante no se podía dar de baja por violación de FK - implementado `desvincularDonante` en `Donacion` y `Formulario` antes de eliminar al donante localmente.

Un bullet por arreglo; el número es el ID estable del punto original («sin ID» = arreglos de integración que no tenían punto propio). Los residuales que siguen abiertos quedan anotados en el bullet.

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
