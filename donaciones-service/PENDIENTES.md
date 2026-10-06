# Pendientes técnicos de `donaciones-service`

Registro de problemas conocidos del servicio, con el motivo y la propuesta de arreglo para
que no se pierdan de vista al crecer el código.

**Están ordenados de más urgente a menos urgente**, no por número de punto. El número es un
ID estable y no se renumera nunca, así que quedan huecos. Un punto corregido se borra de esta
lista y pasa a la sección [Corregidos](#corregidos) del final.

El orden no es el de la severidad declarada en cada punto sino el del daño real: cuánto se
rompe cuando pasa, y qué tan fácil es que pase.

| # | Punto | Por qué está acá |
|---|---|---|
| 1 | 1 | Las dos llamadas HTTP a otros servicios apuntan a rutas que no existen |
| 2 | 2 | Dos bindings de Rabbit atan al exchange equivocado |
| 3 | 3 | Se traga las excepciones de salida a propósito, sin log |
| 4 | 4 | Declara el bean de `notificaciones-service` como dependencia de Maven |
| 5 | 5 | El endpoint de vencer una donación manda un estado que el parser no conoce |
| 6 | 6 | Una estrategia de notificación no es bean: toda entrega fallida revienta |
| 7 | 7 | Un bien sin `tipoBien` se convierte en `null` y revienta la segmentación |
| 8 | 8 | No hay una sola transacción en el módulo: las escrituras quedan a medias |
| 9 | 9 | Un fallo en una donación corta el lote de matchmaking entero |
| 10 | 10 | `fechaEntrega` nunca se persiste, y dos funcionalidades dependen de ella |
| 11 | 11 | Guardar el estado antes de notificar deja el cambio persistido y responde 404 |
| 12 | 12 | `CascadeType.ALL` en las necesidades borra de más al dar de baja una entidad |
| 13 | 13 | El PUT de donante cambia el `@Id` y duplica la Persona |
| 14 | 14 | Las propuestas se numeran desde 1 pero se leen desde 0 |
| 15 | 15 | El score de compatibilidad mide contra el histórico, no contra el período |
| 16 | 16 | La lista de formularios del donante es `@Transient`: la inactividad nunca se avisa |
| 17 | 17 | El ranking de sub-atendidos lo monopoliza una sola entidad |
| 18 | 18 | `cantidadObjetivo` sin validar: NullPointerException que corta el matchmaking |
| 19 | 19 | Borrar el medio de contacto predeterminado lo deja colgando |
| 20 | 20 | Los eventos de logística no se aíslan: un id inválido rebuclea el mensaje para siempre |
| 21 | 21 | Los controllers responden 404 ante cualquier `RuntimeException` |
| 22 | 22 | No hay Bean Validation: entran cantidades negativas como `Bien.peso` |
| 23 | 23 | La segmentación no incluye la unidad de medida y suma kilos con litros |
| 24 | 24 | El PUT de donación deja los bienes anteriores huérfanos |
| 25 | 25 | La importación CSV se traga los errores y no dice cuántos entraron |
| 26 | 26 | El PUT de necesidad ignora el id de entidad y castea a ciegas |

---

## 1. Las dos llamadas a otros servicios apuntan a rutas que no existen

**Estado:** abierto
**Severidad:** crítica
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/clients/IncentivosClient.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/clients/NotificacionesClient.java`

### Qué pasa

Tres peticiones HTTP, las tres fallan. Se verificó levantando los cuatro servicios juntos y
mirando las rutas reales que declara cada controller.

**`IncentivosClient.peticionCrearPerfil`** hace `POST` a `servicio.incentivos.url`, que por
defecto es `http://localhost:8082/`. O sea, a la raíz. En `incentivos-service` el endpoint
real es `POST /api/perfiles`. Lo mismo con `notificarDonacionAsignada`, que hace `POST` a
`http://localhost:8082/{idUsuario}`: la ruta real es `PATCH /api/perfiles/donacion/{idUsuario}`.
Faltan el método HTTP, la ruta y el path variable.

**`NotificacionesClient.enviarNotificacion`** hace `POST` a `http://localhost:8083/`. El
controller de notificaciones está declarado como `@RequestMapping("/notificaciones")` y el
servicio tiene `server.servlet.context-path=/api`, así que la ruta real es
`POST /api/notificaciones`. Al cliente le falta el `/notificaciones`.

### Por qué no se ve

Los dos clientes envuelven la llamada en `try { ... } catch (Exception e)` y responden
`System.err.println`. Eso convierte un 404 en un mensaje en la consola y sigue. El flujo
principal cree que notificó y el registro nunca se crea.

Es el mismo punto 5 del backlog de `incentivos-service`, visto desde el otro lado: el
contrato roto no es de un servicio, es de los dos.

### Propuesta

Definir el contrato una vez y corregir los dos lados. Las rutas reales ya están declaradas en
los controllers, así que la corrección es completar los strings de los clientes.

---

## 2. Dos bindings de Rabbit atan al exchange equivocado

**Estado:** abierto
**Severidad:** media
**Archivo:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/config/RabbitMQConfig.java`

### Qué pasa

`RabbitMQConfig` está duplicado: hay una copia byte a byte en `donaciones-service` y otra en
`logisticas-service`. En la de `logisticas-service`, dos bindings usan `donacionesExchange()`
donde corresponde `logisticasExchange()`:

- `bindingSolicitudEventos` ata `solicitudEventosQueue` a `donaciones.exchange`.
- `bindingRespuestaEventos` ata `respuestaEventosQueue` a `logisticas.exchange` (este está
  bien, es el único de los dos que acierta).

El de `solicitudEventos` es el que importa: `logisticas.exchange` publica
`LogisticaPollingScheduler` y `SolicitudEventosListener` consumed
`logisticas.solicitud.eventos.queue`. Con el binding atado al exchange de donaciones, ese
tráfico no llega nunca.

### Por qué no se ve

El binding del mismo nombre declarado en `donaciones-service` **sí** ata
`solicitudEventosQueue` a `logisticas.exchange` con el routing key correcto. Como las dos
colas tienen el mismo nombre, la declaración de `donaciones-service` la deja atada igual y el
mensaje llega. El bug está enmascarado por el binding del otro servicio: funciona mientras
`donaciones-service` esté arriba, y se rompe si alguna vez se levanta logística sola.

Se verificó contra el broker: la cola `logisticas.solicitud.eventos.queue` aparece con dos
consumidores esperados y el binding de `donaciones.exchange` no aparece en la lista.

### Propuesta

Corregir el exchange del binding y eliminar la copia duplicada de `RabbitMQConfig`, dejando la
declaración de colas y exchanges en un solo módulo. Dos copias de la misma constante en
servicios distintos es exactamente lo que hace que un fix se aplique en un lado y no en el otro.

---

## 3. Se traga las excepciones de salida a propósito, sin log estructurado

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/clients/IncentivosClient.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/clients/NotificacionesClient.java`

### Qué pasa

Los dos clientes hacen `catch (Exception e) { System.err.println(...) }` y devuelven
`null`. Ninguno distingue un 404 de una base caída de un timeout.

### Por qué está anotado y no corregido

Es una decisión consciente del equipo: no cortar el flujo de donación porque falló una
notificación. El problema es la otra mitad: sin nivel ni logger, cuando algo falla no hay forma
de saber qué pasó, y con el punto 1 de arriba es imposible distinguir "no llegó la
notificación" de "se mandó y el endpoint no existe".

### Propuesta

Dejar la decisión (no propagar) y cambiar solo la observabilidad: logger con nivel `warn` en
lugar de `System.err`, y el código de respuesta HTTP en el mensaje. Es un cambio chico que
convierte un fallo invisible en uno diagnosticable.

---

## 4. Declara el bean de `notificaciones-service` como dependencia de Maven

**Estado:** abierto
**Severidad:** baja
**Archivo:** `logisticas-service/pom.xml`

### Qué pasa

`logisticas-service/pom.xml` depende de `ar.edu.utn.frba.ddsi:notificaciones-service`. Eso
obliga a construir o instalar el jar de otro microservicio para compilar logística, y hace
que dos servicios que se comunican por HTTP compartan artefacto de compilación.

Además explica una fricción del build: `mvn package -pl logisticas-service` falla con
"Could not find artifact notificaciones-service" si ese módulo no está instalado en el `.m2`.
Hay que usar `-am`.

### Propuesta

Sacar la dependencia y usar HTTP o el modelo de eventos, que es por donde ya se comunican.

---

## 5. El endpoint de vencer una donation manda un estado que el parser no conoce

**Estado:** abierto
**Severidad:** crítica
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/DonacionService.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/gestores/GestorAsignaciones.java`

### Qué pasa

`DonacionService.marcarComoVencida` (línea 120) llama
`gestorAsignaciones.cambiarEstado(id, "VENCIDA", ...)`. El `switch` de
`GestorAsignaciones.parseEstado` (líneas 72-86) sólo reconoce el case `"VENCIDO"`, que es el
nombre real del enum `Estado.VENCIDO`. `"VENCIDA"` cae en el `default` y tira
`IllegalArgumentException("Estado desconocido: VENCIDA")`.

`PATCH /donaciones/{id}/vencer` está roto el 100% de las veces, para toda donación.

### Por qué no se ve

`DonacionController.marcarComoVencida` (líneas 90-96) envuelve la llamada en
`try { ... } catch (RuntimeException e) { return ResponseEntity.notFound().build(); }`.
Como `IllegalArgumentException` es un `RuntimeException`, la falla se traduce a un 404 limpio:
la respuesta parece la de "no existe esa donación", que es un caso de error totalmente
distinto. El mismo `catch` se repite en `actualizarDonacion` y `cambiarEstado`.

### Propuesta

Que el servicio speak el mismo idioma que el enum (`"VENCIDO"`), y mejor: cambiar
`cambiarEstado` para que reciba `Estado` y no un `String`, así el compilador es el que impide
el desacople. De paso, sacar el `catch (RuntimeException) → 404` de los controllers (ver punto
21).

---

## 6. Una estrategia de notificación no es bean: toda entrega fallida revienta

**Estado:** abierto
**Severidad:** crítica
**Archivos:**
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/ServicioMensaje/EstrategiasMensajes/NotificacionEntregaFallida.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/ServicioMensaje/FabricaEstrategiasNotificacion.java`

### Qué pasa

`NotificacionEntregaFallida` es la única de las seis estrategias que **no** tiene `@Component`
(las otras cinco lo tienen: `NotificacionViaje:11`, `NotificacionRegistroPersona:11`,
`NotificacionInactividad:11`, `NotificacionEntregaCompletada:12`, `NotificacionDonacionAsignada:11`).

`FabricaEstrategiasNotificacion` arma su mapa inyectando `List<EstrategiaNotificacion>`
(líneas 21-29), o sea, sólo con los beans. Sin `@Component`, `NotificacionEntregaFallida`
nunca entra al mapa, así que la clave `ENTREGA_NO_RECIBIDA` no existe.

Cuando logística reporta `ENTREGA_FALLIDA`, `GestorEventosLogistica.manejarEntregaFallida`
(línea 113) llama `fabricaEstrategias.ejecutar(TipoEventoNotificacion.ENTREGA_NO_RECIBIDA, ...)`
y `FabricaEstrategiasNotificacion.ejecutar` (líneas 41-44) tira
`IllegalArgumentException("No existe una estrategia para ENTREGA_NO_RECIBIDA")`.

### Por qué no se ve

La notificación al donante y a la entidad happens en `NotificacionEntregaCompletada`, que sí
está registrada, y el estado de la donación se guarda justo antes (línea 110). O sea, el
mensaje de prueba aparece como si anduviera: lo único que falta es el aviso de entrega
fallida, y nobody ve el error porque `manejarEntregaFallida` no tiene try/catch y la excepción
se va por el listener de Rabbit.

### Propuesta

Ponerle `@Component`. Y como defensa: que la fábrica falle al arrancar si un
`TipoEventoNotificacion` del enum no tiene estrategia registrada, en lugar de fallar en
runtime la primera vez que se dispara el evento.

---

## 7. Un bien sin `tipoBien` se convierte en `null` y revienta la segmentación

**Estado:** abierto
**Severidad:** crítica
**Archivos:**
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/dto/donaciones/BienResumenDTO.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/DonacionService.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/SegmentadorDonaciones/SegmentadorDonaciones.java`

### Qué pasa

`BienResumenDTO.toDomain(SubcategoriaBien)` tiene, en la línea 48, un
`if (tipoBien == null) return null;`. O sea: un item del formulario sin `tipoBien` no es un
error, es un `null`.

`DonacionService.procesarFormulario` (línea 71) mete ese `null` en `bienesNormal` sin
filtrarlo, y en la línea 72 llama `crearBien(null)`, que `RepositorioBienes.guardar` ignora en
silencio (su guarda es `if (bien != null)`).

El `null` llega intacto a `SegmentadorDonaciones.segmentar`, que en
`generarClaveSegmentacion` (línea 44) hace `bien.getSubcategoria().getNombre()`:
**NullPointerException**. `POST /donaciones/formulario` responde 500.

Peor: si el CSV o el request trae *cualquier* otro error de validación, la línea 52 del mismo
DTO tira `IllegalArgumentException("Tipo de bien desconocido: ...")`, y `DonacionController`
no lo captura en ese endpoint, así que también es un 500 en vez de un 400.

### Por qué no se ve

El `System.err.println` de `crearBien` (línea 148) más el `catch (IllegalArgumentException)`
Dan la impresión de que "el bien se saltó". No se saltó: sigue en la lista y explota tres
líneas más abajo.

### Propuesta

Que `toDomain` no devuelva `null`: que tire `IllegalArgumentException` con el índice del item
y que `DonacionService` la rechace con un 400 descriptivo. Y agregar validación de Bean
Validation (ver punto 22) para que `tipoBien` sea obligatorio.

---

## 8. No hay una sola transacción en el módulo: las escrituras quedan a medias

**Estado:** abierto
**Severidad:** alta
**Archivo:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/DonacionService.java`

### Qué pasa

Un `grep` de `@Transactional` sobre todo `donaciones-service` no devuelve **ni una** ocurrencia.
Cada llamada a `jpaRepository.save()` abre su propia transacción y commitea sola.

`procesarFormulario` (líneas 67-82) hace cuatro escrituras encadenadas: `crearBien` por cada
biene, `repositorioFormularios.guardar`, `repositorioDonaciones.guardarDonaciones` y
`repositorioDonantes.agregarFormularioADonante`. Si la tercera falla, los bienes y el
formulario ya quedaron commiteados: quedan filas huérfanas en `bien` y en `formulario` para
una donación que el cliente nunca recibió.

`asignarPropuesta` (líneas 128-134) tiene el mismo problema en peor estado: asigna entidad,
agrega la donación a la necesidad, borra el resultado de matchmaking y recién ahí cambia el
estado a ASIGNADO. Si el `cambiarEstado` final falla, el resultado ya se borró y la donación
quedó asignada sin propuestas para aprobar, sin forma de volver atrás.

### Por qué no se ve

Cada repositorio funciona bien aislado, y los tests pasan. El problema sólo aparece cuando una
escritura intermedia falla, y en ese caso el síntoma es "apareció una donación sin formulario"
o "la asignación quedó a medias", que no señala al servicio.

### Propuesta

`@Transactional` en los métodos de servicio que hacen lectura-modificación-escritura:
`procesarFormulario`, `actualizarDonacion`, `asignarPropuesta`, `ejecutarMatchmakingADemanda`,
`cambiarEstado` y `eliminarDonacion`. Con `rollbackFor = Exception.class` por si aparece
alguna checked.

---

## 9. Un fallo en una donación corta el lote de matchmaking entero

**Estado:** abierto
**Severidad:** alta
**Archivos:**
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/AsignadorDonaciones/AsignadorDonaciones.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/repositories/repos/RepositorioDeResultadosMatchmaking.java`

### Qué pasa

`ejecutarMatchmakingBatch` (línea 49) recorre con
`todasLasDonaciones.forEach(donacion -> procesarMatchmaking(donacion, todasLasEntidades))`.
No hay try/catch por elemento: la primera excepción aborta el `forEach` y las donaciones
restantes no se procesan nunca. El `AsignacionScheduler` y `POST /donaciones/matchmaking/ejecutar`
mueren con ella.

El caso concreto que lo dispara es `registrarDonacionPendienteDeAprobacion` (líneas 164-179):
primero llama `gestorAsignaciones.cambiarEstado(donacion.getId(), "PENDIENTE_ASIGNACION", ...)`,
que **ya commitea** el cambio de estado (ver punto 8), y después
`repositorioDeResultadosMatchmaking.guardar(resultado)`, que en las líneas 32-34 tira
`IllegalArgumentException("Ya existe un resultado de matchmaking para la donación: ...")` si
ya había uno.

El resultado combinado es el peor de los dos mundos: la donación queda en
`PENDIENTE_ASIGNACION` sin `ResultadoMatchmaking`, y como
`RepositorioDonaciones.buscarDonacionesSinAsignar()` (línea 39) sólo trae `EN_DEPOSITO`, el
scheduler ya no la vuelve a pickear. Quedó huérfana para siempre: el front muestra
"PENDIENTE_ASIGNACION" sin propuestas y `POST /donaciones/asignar` responde "No hay resultado
de matchmaking".

### Por qué no se ve

Sólo se manifiesta la segunda vez que corre el matchmaking sobre la misma donación. Como el
primer corrida dejó el estado cambiado, la ventana es rara pero real (reintento manual del
endpoint, donante que vuelve a EN_DEPOSITO por `ENTREGA_FALLIDA`, rollback manual).

### Propuesta

Envolver `procesarMatchmaking` en un try/catch por donación, loguear y seguir. Y hacer la
escritura en el orden inverso: primero guardar el resultado, después cambiar el estado (o
juntarlo todo en una transacción, ver punto 8).

---

## 10. `fechaEntrega` nunca se persiste, y dos funcionalidades dependen de ella

**Estado:** abierto
**Severidad:** alta
**Archivos:**
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/SegmentadorDonaciones/SegmentadorDonaciones.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/AsignadorDonaciones/AlgoritmosDeAsignacion/SubAtendidos.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/Necesidades/NecesidadRecurrente.java`

### Qué pasa

Nadie escribe nunca `Donacion.fechaEntrega`. El único constructor que lo recibe es el de
`Donacion` (líneas 87-98) y el único que lo arma es
`SegmentadorDonaciones.crearDonacion`, que le pasa `null` explícitamente en la línea 66.
`DonacionDTO.toDomain()` tampoco lo setea. Un `grep` de `setFechaEntrega` en `src/main` sólo
devuelve las dos copias del valor hacia otros DTOs (`DonacionDTO:51` y
`GestorAsignaciones:94`), nunca la escritura en la entidad.

La columna queda siempre en `NULL`, y dos lógicas la usan como si fuera dato real:

- **`SubAtendidos.cantidadDonacionesUltimoTrimestre`** (líneas 57-63) filtra por
  `d.getFechaEntrega() != null`. Como siempre es `null`, el count es **siempre 0**: el
  algoritmo "priorizar a los sub-atendidos" no prioriza a nadie.
- **`NecesidadRecurrente.cantidadRecibidaEnPeriodo`** (líneas 31-40) filtra por
  `donacion.getFechaEntrega() != null && ...isAfter(fechaLimite)`. Siempre `null`, así que
  **siempre devuelve 0**: una necesidad recurrente jamás se da por satisfecha por más que se
  le entregue todo.

El resto del sistema tampoco avisa: `NotificacionDonacionAsignada` (líneas 40-42) imprime
literalmente "sin fecha definida" en el mensaje al donante.

### Por qué no se ve

El síntoma no es una excepción sino un algoritmo que devuelve siempre lo mismo. El
matchmaking "funciona", las propuestas salen, y nadie nota que el score de `SubAtendidos` es
constante ni que las necesidades recurrentes nunca se cierran.

### Propuesta

Definir quién setea `fechaEntrega` y cuándo (¿al confirmar la entrega, o al generar el
formulario?) y setearla en ese momento. Mientras tanto, `NecesidadRecurrente` y `SubAtendidos`
no deberían depender de un campo que nunca se llena.

---

## 11. Guardar el estado antes de notificar deja el cambio persistido y responde 404

**Estado:** abierto
**Severidad:** alta
**Archivos:**
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/gestores/GestorAsignaciones.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/controllers/DonacionController.java`

### Qué pasa

`GestorAsignaciones.cambiarEstado` (líneas 49-65) guarda la donación en la línea 60 y
*después* llama a `procesarAccionesPostCambioEstado`. Ese método (líneas 89-105) desreferencia
`donacion.getSubcategoria().getNombre()` (línea 96) y
`donacion.getEntidad().getPersonaJuridica().getRazonSocial()` (línea 98) sin ninguna guarda de
nulidad.

El caso fácil de llegar: `PATCH /donaciones/{id}/estado` con `{"nuevoEstado": "ASIGNADO"}` sobre
una donación que todavía no tiene entidad asignada. Y eso es exactamente el estado en que
quedan las donationsFresh segmentadas: `SegmentadorDonaciones.crearDonacion` pasa `null` como
entidad (línea 61). `getEntidad()` devuelve `null` y se revienta con NPE en la línea 98.

Como el `guardar` de la línea 60 ya commiteó (no hay transacción, ver punto 8), la donación
queda en ASIGNADO en la base. Y como `IllegalArgumentException`/NPE son `RuntimeException`, el
`catch (RuntimeException e) → notFound()` de `DonacionController.cambiarEstado` (líneas 83-85)
devuelve **404**: el cliente cree que la donación no existe y reintenta, cada vez dejando el
estado peor.

### Por qué no se ve

El 404 miente. Un 404 de este endpoint debería significar sólo "no hay donación con ese id", y
eso ya lo cubre `RuntimeException("Donación no encontrada con ID: " + id)` de la línea 53.

### Propuesta

Mover la validación y las notificaciones **antes** del `guardar`, y agregar guarda de nulidad
para `subcategoria`/`entidad`. Y que `cambiarEstado` valide que la transición sea legal desde el
estado actual (hoy se puede mandar cualquier estado desde cualquier estado).

---

## 12. `CascadeType.ALL` en las necesidades borra de más al dar de baja una entidad

**Estado:** abierto
**Severidad:** alta
**Archivo:**
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/EntidadBeneficiaria/EntidadBeneficiaria.java`

### Qué pasa

`EntidadBeneficiaria.necesidades` (líneas 37-39) está declarado como
`@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)`. `ALL` incluye `REMOVE`.

`RepositorioEntidadesBeneficiarias.eliminarPorId` (líneas 51-53) hace
`jpaRepository.deleteById(id)`. Con ese `ALL`, Hibernate borra en cascada **todas** las
necesidades de la entidad. Y esas necesidades están referenciadas por `Donacion.necesidad_id`
(`Donacion.java:51-53`), que es un `@ManyToOne` sin cascade: las donations quedan apuntando a
filas que ya no existen.

O lo que pasa es una violación de la FK y un 500, o (si la FK no llegara a crearse) donations
huérfanas que en `Necesidad.cantidadRecibida()` y en `EntidadBeneficiaria.verDonaciones()`
simplemente desaparecen del conteo. En los dos casos el borrado de una entidad destruye
histórico de donaciones de otras.

El mismo `CascadeType.ALL` con `orphanRemoval` aparece en
`EntidadBeneficiariaService.eliminarNecesidad` (líneas 99-103), que primero saca la necesidad
de la lista (la orphaned se borra sola) y después la vuelve a borrar con
`repositorioNecesidades.eliminarPorId`, con lo que hace un delete de una fila ya eliminada.

### Por qué no se ve

Los comentarios del código dicen explícitamente lo contrario: `Donacion.necesidad` (líneas
48-50) afirma "Sin cascade: la Necesidad vive en su propio repositorio". La cascada está del
lado del dueño de la relación, en `EntidadBeneficiaria`, y es `ALL`.

### Propuesta

Bajar `necesidades` a `cascade = {PERSIST, MERGE}` (o nada) y dejar el borrado de necesidades
explicito en `RepositorioNecesidades`, que es el que las tiene en su propio repositorio.

---

## 13. El PUT de donante cambia el `@Id` y duplica la Persona

**Estado:** abierto
**Severidad:** alta
**Archivo:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/repositories/repos/RepositorioPersonas.java`

### Qué pasa

`RepositorioPersonas.modificarPersona` (líneas 72-93) hace, en la línea 79:

```java
existente.setId(datosNuevos.getId()); // Se actualiza por si es necesario
```

`datosNuevos` viene de `DonanteService.actualizarPersona` (línea 88), que lo arma con
`dto.toDomain(ciudad)`. Ese camino termina en `PersonaDonanteDTO.construirDonante` (líneas
56-72), que hace `new Donante(dir, persona)` con una `Humana`/`Juridica` **nueva**, y el
constructor de `Persona` (líneas 31-34) genera `this.id = UUID.randomUUID()`.

O sea: `datosNuevos.getId()` es siempre un UUID recién generado, nunca `null` y nunca igual al
viejo. La línea 79 le cambia el identificador primario a una entidad que Hibernate ya tiene
cargada. En la línea 88, `actualizar(idOriginal, existente)` llama
`jpaRepository.save(existente)`: como el id ya no es `null`, Spring Data hace `merge()`, el
`merge` no encuentra fila con ese id, Hibernate la trata como transient y **inserta una
Persona nueva**. La Persona vieja queda huérfana, apuntada por el resto de las columnas, y el
`PUT /personas/{id}` devuelve 200 con un id que no es el del recurso que se pidió editar.

La línea 88 también pisa `existente.setId(...)` con el id viejo después... no: el orden es
setId (79) y después `actualizar(idOriginal, existente)` (88), que usa `existsById(idOriginal)`
para decidir, pero guarda la entidad con el id **nuevo**.

### Por qué no se ve

El comentario de la línea 79 dice "Se actualiza por si es necesario", como si fuera una
precaución inocua. Y el `System.out.println("Persona actualizada con éxito.")` de la línea 89
confirma el éxito de una operación que en realidad duplicó el registro.

### Propuesta

Sacar la línea 79. El id de una Persona no se modifica en un update; y si alguna vez hay que
cambiar la identidad del donante, es otra operación (con su endpoint y su validación).

---

## 14. Las propuestas se numeran desde 1 pero se leen desde 0

**Estado:** abierto
**Severidad:** media
**Archivos:**
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/AsignadorDonaciones/AlgoritmosDeAsignacion/AlgoritmoAsignacion.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/gestores/GestorMatchmaking.java`

### Qué pasa

El ranking que se expone al front es 1-based:

- `AlgoritmoAsignacion.extraerRanking` (línea 45) setea
  `propuesta.setPosicion(top10.size() + 1)` **después** del `poll()`, así que la primera
  (la peor) recibe `10` y la última (la mejor) recibe `1`.
- `AsignadorDonaciones.obtenerInterseccion` (líneas 132-134) setea
  `setPosicion(i + 1)`, o sea 1..N.
- `PropuestaAsignacionDTO.posicion` (línea 15) expone ese número tal cual.

`GestorMatchmaking.obtenerPropuestaSeleccionadaParaDonacion` (líneas 18-30), en cambio, valida
`posicion >= resultado.getPropuestasOrdenadas().size()` y después hace
`resultado.getPropuestasOrdenadas().get(posicion)`: indexa 0-based.

Consecuencia: si el admin aprueba "la propuesta número 1" (la mejor según la pantalla),
`POST /donaciones/asignar` con `{"posicionPropuesta": 1}` ejecuta `.get(1)` y asigna la
**segunda** propuesta. Y la última de la lista, que tiene `posicion == size`, es rechazada con
"Posición de propuesta inválida" aunque sea una propuesta perfectly válida de la pantalla.

### Por qué no se ve

El único chequeo de rango (`posicion >= size`) deja pasar el rango 0..size-1, que es
exactamente el rango 0-based, así que la validación "parece" correcta. El desfasaje de una
posición no se nota salvo que uno compare contra la lista de la UI.

### Propuesta

Definir el contrato: si `posicionPropuesta` es el número de la UI, usar `get(posicion - 1)` y
validar `posicion < 1 || posicion > size`. Si es un índice, entonces `extraerRanking` y
`obtenerInterseccion` tienen que empezar en 0 y el DTO mostrar el índice. Lo que no puede ser es
una cosa del lado del que arma y otra del que lee.

---

## 15. El score de compatibilidad mide contra el histórico, no contra el período

**Estado:** abierto
**Severidad:** media
**Archivo:**
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/AsignadorDonaciones/AlgoritmosDeAsignacion/CompatibilidadSemantica.java`

### Qué pasa

`calcularScore` (líneas 67-73) calcula
`cantidadFaltante = necesidad.getCantidadObjetivo() - necesidad.cantidadRecibida()`, y
`Necesidad.cantidadRecibida()` (líneas 56-61) suma **todas** las donations entregadas, sin
ventana temporal.

Pero la compatibilidad la decide `Necesidad.esCompatibleCon` (líneas 65-67), que llama
`estaSatisfecha()`, y para `NecesidadRecurrente` (líneas 26-29) eso es
`cantidadRecibidaEnPeriodo() >= cantidadObjetivo`: **con** ventana temporal.

Las dos definiciones de "cuánto falta" no son la misma. Una necesidad recurrente que ya recibió
todo su objetivo en el trimestre pasado y se venció tiene `cantidadRecibidaEnPeriodo() == 0`,
así que `esCompatibleCon` la da por compatible y entra al ranking. Pero
`calcularScore` la mide contra el histórico: `cantidadFaltante` da 0 o negativo, y el score
termina siendo `<= 0`, que es justo lo que el filtro de la línea 51 (`if (score <= 0) continue;`)
descarta.

Resultado: **una necesidad recurrente nunca vuelve a recibir una donación** una vez que se
llenó en algún momento del histórico. Para una necesidad recurrente, que es justamente lo que
debería volver a llenarse cada período, el algoritmo queda permanentemente inservible.

Y en el borde: si `cantidadFaltante == 0` y `donacion.sumaCantidadBienes() == 0` (una
donación con bienes de peso 0, que `BienResumenDTO.toDomain` genera con `cantidad != null ?
cantidad : 0` en la línea 50), la línea 71 calcula `0.0 / 0`, que en `double` es `NaN`. El
filtro `score <= 0` da `false` para `NaN`, así que el `NaN` entra al `PriorityQueue` (líneas
34-36) y rompe el orden del heap: `peek()` deja de ser el peor y el top-10 se congela.

### Por qué no se ve

El caso del `NaN` es raro. El de las recurrentes llenadas no: es el flujo normal, y se manifiesta
como "el matching propensity no propone necesidades recurrentes viejas", que se lee como
"el modelo no es bueno", no como "el score se calculó contra la columna equivocada".

### Propuesta

Unificar la noción de "faltante": que `calcularScore` use `cantidadRecibidaEnPeriodo()` para las
recurrentes (idealmente, que `Necesidad` exponga `cantidadFaltante()` y ambos caminos lo usen).
Y blindar la división: si el denominador es 0, devolver 0 y que el filtro lo descarte.

---

## 16. La lista de formularios del donante es `@Transient`: la inactividad nunca se avisa

**Estado:** abierto
**Severidad:** media
**Archivos:**
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/donador/Donante.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/repositories/repos/RepositorioDonantes.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/DonanteService.java`

### Qué pasa

`Donante.formularios` (líneas 29-30) está anotado `@Transient`. Es decir, **nunca** se carga
desde la base: `RepositorioDonantes.obtenerTodos()` devuelve donantes que siempre traen la
lista vacía.

`DonanteService.revisarActividades` (líneas 142-148), que corre todos los días desde
`ActividadDonanteScheduler`, empieza así:

```java
if (p.getFormularios() != null && !p.getFormularios().isEmpty() && p.getFormularios().getLast()...)
```

Como la lista siempre está vacía, la condición nunca pasa. **La notificación de inactividad
del donante no se manda nunca**, por más de 20 días que lleve sin donar.

El otro lado del mismo problema: `RepositorioDonantes.agregarFormularioADonante` (líneas
75-84) hace `donante.agregarFormulario(nuevoFormulario)` y `actualizar(idDonante, donante)`.
Como la lista es `@Transient`, ese `save()` no persiste nada: el formulario queda huérfano
desde el punto de vista del donante. El vínculo real existe sólo al revés, en
`formulario.donante_id`.

Y si el `@Transient` se sacara sin más, `revisarActividades` reventaría en
`getFechaRealizacion()` con NPE, porque `FormularioRequestDTO.fechaRealizacion` (línea 16) no
tiene validación y `Formulario` la acepta en `null` (línea 40).

### Por qué no se ve

El `System.out.println` de `agregarFormularioADonante` dice "Formulario agregado con éxito al
donante: <nombre>", y el flujo de alta parece completo. Lo que falta es el aviso 20 días
después, que es un síntoma diferido y sin relación causal visible con el alta.

### Propuesta

Mapear `Donante.formularios` como `@OneToMany(mappedBy = "donante")` (el dueño ya es
`Formulario.donante`, líneas 27-29 de `Formulario.java`) y sacar el `@Transient`. Agregar
`@NotNull` a `fechaRealizacion` o un default en `Formulario`, porque si no el scheduler
sigue exploendo.

---

## 17. El ranking de sub-atendidos lo monopoliza una sola entidad

**Estado:** abierto
**Severidad:** media
**Archivo:**
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/AsignadorDonaciones/AlgoritmosDeAsignacion/SubAtendidos.java`

### Qué pasa

El score del algoritmo es **por entidad**, no por necesidad: `cantidadDonaciones` se calcula
una vez por entidad (líneas 43-45, el "flag de evaluación perezosa") y se reutiliza para todas
las necesidades de esa entidad (líneas 47-51).

Con eso, si una entidad tiene 10 o más necesidades compatibles, el `top10` se llena entero con
las de esa entidad en el primer `for` interno, todas con score idéntico. Para las entidades
siguientes, la condición de reemplazo es `cantidadDonaciones < top10.peek().getScore()`
(línea 49), una comparación estricta: si la primera entidad tenía 0 donations y la segunda
también tiene 0, `0 < 0` es `false` y **ninguna** entra.

O sea: `SubAtendidos` devuelve siempre las necesidades de la primera entidad que toca,
aunque haya diez entidades con cero donations en el último trimestre. El objetivo declarado en
el comentario de las líneas 19-25 ("priorizamos a las necesidades con MENOS donaciones") no se
cumple: el desempate lo gana quien aparece primero en la lista.

A esto se suma el punto 10: como `Donacion.fechaEntrega` nunca se persiste, el count siempre
da 0, así que **todos los scores son 0** y el criterio de ordenamiento es completamente
inerte.

### Por qué no se ve

Los dos algoritmos devuelven propuestas y el cruce de `AsignadorDonaciones.obtenerInterseccion`
filtra por `apariciones.size() == totalAlgoritmos` (línea 126), así que las propuestas siempre
llegan por el otro lado. `SubAtendidos` nunca es el que decide, siempre acompaña.

### Propuesta

Un score por necesidad (o el criterio escrito como "entidad con menos donations primero" con
un desempate explícito por necesidad), y probablemente `thenComparing` por ID para que el
resultado sea determinista y no dependa del orden de `entidades`.

---

## 18. `cantidadObjetivo` sin validar: NullPointerException que corta el matchmaking

**Estado:** abierto
**Severidad:** media
**Archivos:**
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/dto/entidadBeneficiaria/NecesidadDTO.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/Necesidades/NecesidadExtraordinaria.java`

### Qué pasa

`NecesidadDTO.toDomain` (líneas 34-49) pasa `this.cantidadObjetivo` tal cual a los dos
constructores, sin chequear nada. `Necesidad.cantidadObjetivo` es un `Integer` (línea 38 de
`Necesidad.java`), o sea, admite `null`.

Después, `NecesidadExtraordinaria.estaSatisfecha` (línea 23) hace
`this.cantidadRecibida() >= cantidadObjetivo`: eso es un `int >= Integer`, así que Java
desempaqueta el `Integer` y **`NullPointerException`** si viene `null`. Lo mismo
`NecesidadRecurrente.estaSatisfecha` (línea 28).

La ruta para llegar es un `POST /entidades/{id}/necesidades` con el body
`{"tipoNecesidad":"EXTRAORDINARIA","descripcion":" frazadas"}` y sin `cantidadObjetivo`:
`EntidadBeneficiariaService.agregarNecesidad` lo persiste sin problema (líneas 82-88) y
responde 201. El NPE aparece recién en el próximo `esCompatibleCon`, que es invocado por
`CompatibilidadSemantica.rankear` línea 43 y `SubAtendidos.rankear` línea 38 -- o sea, en el
scheduler de asignación, y por el mecanismo del punto 9 corta el lote entero.

`plazoEnDias` tiene el mismo problema: `NecesidadDTO` línea 41 sólo lo protege de `null`
(default 30), pero un `0` o un negativo se acepta. Con `plazoEnDias = 0`,
`NecesidadRecurrente.cantidadRecibidaEnPeriodo` calcula `LocalDate.now().minusDays(0)` y
ninguna donation con fecha futura cuenta: la necesidad recurrente nunca se cierra. Con un
valor negativo, el límite queda en el futuro y el filtro `isAfter(fechaLimite)` es siempre
`true`: se cuenta todo el histórico y la necesidad se marca satisfecha al instante.

Igual para `BienResumenDTO.cantidad`: un `cantidad` negativo se persiste tal cual en
`Bien.peso` (ver punto 22).

### Por qué no se ve

El alta de la necesidad devuelve 201 e incluye el `id` de la necesidad recién creada, así que
desde el front todo salió bien. El NPE aparece en el scheduler de las 18:00, adentro de un
método cuyo nombre no dice nada de validación (`estaSatisfecha`), y como el scheduler no tiene
logger propio (ver punto 9) lo único que queda es un stack trace en la consola de un proceso
que corre desatendido.

### Propuesta

Bean Validation en el `@RequestBody` de los controllers: `@NotNull @Positive` en
`cantidadObjetivo`, `@Positive` en `plazoEnDias` y en `Bien.peso`. Y no dejar que una
necesidad inválida llegue a la base: el 400 tiene que salir en el alta, no en el scheduler.

---

## 19. Borrar el medio de contacto predeterminado lo deja colgando

**Estado:** abierto
**Severidad:** media
**Archivos:**
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/Mensaje/MedioDeContacto/MediosDeContacto.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/repositories/repos/RepositorioPersonas.java`

### Qué pasa

En `MediosDeContacto` hay dos referencias al mismo tipo de objeto, pero sólo una es dueña:

- `medioDeContactoPredeterminado` (líneas 29-31) es un `@ManyToOne` con cascade
  `PERSIST/MERGE` -- **no** es dueño de la fila.
- `listaMediosDeContacto` (líneas 35-37) es un `@OneToMany` con `cascade = ALL` y
  **`orphanRemoval = true`**: sí es dueño, y saca la fila de la base.

`MediosDeContacto.eliminarMedioDeContacto` (líneas 50-52) sólo hace
`listaMediosDeContacto.remove(medio)`. Con `orphanRemoval`, Hibernate borra la fila del medio
en el flush... y `medio_predeterminado_id` sigue apuntando a ese id.

`DELETE /personas/{id}/medios-contacto` (el flujo de `RepositorioPersonas.eliminarMedioDeContactoAPersona`,
líneas 110-123) no chequea si el medio que se borra es el predeterminado. Consecuencias:
violación de la FK `medio_predeterminado_id` (500) o, si la FK no llegara a crearse, un
`MediosDeContacto` cuyo `medioDeContactoPredeterminado` apunta a una fila inexistente, con lo
que `ServicioNotificaciones.enviarNotificacionAMedioPredeterminado` (líneas 39-45) sigue
"pasando" el chequeo de `!= null` y falla más adentro, al serializar.

Lo mismo del lado de la Persona: `Persona.mediosDeContacto` es
`@OneToOne(cascade = ALL, orphanRemoval = true)` (líneas 27-29), así que el `orphanRemoval`
está duplicado en los dos niveles.

### Por qué no se ve

Nadie borra el predeterminado a propósito: se borra "el medio que ya no sirve". El síntoma
aparece como un 500 en un `DELETE` que el usuario compartió sin querer, o --peor-- como una
notificación que sale con los datos de un medio que ya no existe. En los dos casos el culpable
parece ser el frontend.

### Propuesta

En `eliminarMedioDeContacto`, si el medio removido es el predeterminado, promover a
predeterminado el primero de la lista (o dejar `null`) **antes** de sacarlo de la lista. Con eso
basta: el `orphanRemoval` ya se encarga de la fila.

---

## 20. Los eventos de logística no se aíslan: un id inválido rebuclea el mensaje para siempre

**Estado:** abierto
**Severidad:** media
**Archivos:**
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/RabbitMQ/EventosListener.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/gestores/GestorEventosLogistica.java`

### Qué pasa

Tres de los cuatro manejadores hacen `UUID.fromString(evento.getReferenciaId())` sin ninguna
protección: `manejarEntregaConfirmada` línea 89, `manejarEntregaFallida` línea 106 y
`manejarReingresoDeposito` línea 124. Si `referenciaId` viene `null` o con un formato que no
sea UUID, `fromString` tira `IllegalArgumentException` y `procesarEvento` no la captura.

`EventosListener.recibirEventos` (líneas 34-58) tampoco tiene try/catch. La excepción sale del
método `@RabbitListener`, Spring Boot la re-encola, y el mismo mensaje vuelve a entrar. Como el
`setUltimoIdProcesado` de la línea 51 nunca se ejecuta, el cursor no avanza: **el mensaje
queda rebucleando para siempre** y bloquea la cola (los otros mensajes detrás no se consumen).

`manejarInicioRuta` (líneas 56-86) tiene el try/catch, pero abarca el `for` **entero**: el
`catch (Exception e)` de la línea 83 está afuera del loop de las líneas 71-82. Un solo
`payload.getItems()` con un id mal formado (línea 72) o una donación sin entidad
(`donacion.getEntidad().getPersonaJuridica()` en la línea 79, NPE clásico porque las donations
en depósito no tienen entidad) aborta el resto de la ruta: las donations siguientes quedan sin
`EN_TRASLADO` y sin notificación, y sólo se lee una línea en `System.err`.

### Por qué no se ve

El `System.err.println` de la línea 84 dice "Error parseando items de la ruta", que suena a
problema de parsing del payload cuando en realidad puede ser una NPE de una de las donations
del medio del loop. Y como el scheduler reintenta cada 2 minutos, la sensación es de "a veces
falla".

### Propuesta

Aislar cada evento: try/catch por evento en `procesarEvento`, log con nivel `warn` y seguir.
Para los que no se pueden procesar (referencia inválida), derivarlos a una dead-letter queue en
vez de re-encolar. Y validar `referenciaId` con un `try { UUID.fromString(...) } catch` que
descarte el evento explícitamente.

---

## 21. Los controllers responden 404 ante cualquier `RuntimeException`

**Estado:** abierto
**Severidad:** baja
**Archivo:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/controllers/DonacionController.java`

### Qué pasa

Tres endpoints hacen `catch (RuntimeException e) { return ResponseEntity.notFound().build(); }`:
`obtenerDonacion` (líneas 50-54), `actualizarDonacion` (60-64), `cambiarEstado` (77-85) y
`marcarComoVencida` (91-95).

`RuntimeException` es la base de casi todo lo que puede fallar en un servicio Spring: NPE,
`DataIntegrityViolationException`, `EntityNotFoundException`, `OptimisticLockException`, la
`IllegalArgumentException` de los puntos 5, 7 y 14. Todos terminan en un 404 sin cuerpo.

El caso de `DonacionService.procesarFormulario` línea 69 es el inverso y peor: para un donante
inexistente tira `new NullPointerException("No se encontró persona con ese ID")`. Un NPE es un
`RuntimeException`, así que no lo agarra ningún `@ExceptionHandler` específico y cae en el
catch-all de `GlobalExceptionHandler` (líneas 20-31), que devuelve **500** con
`printStackTrace()`. Un "no existe el donante" debería ser un 404 (o 400), no un 500.

### Por qué no se ve

Los controllers parecen estar manejando errores; lo que están haciendo es tapando el
diagnóstico. Justamente por esto los puntos 5, 11 y 14 se presentan como "404 limpio" en vez
de como el error que son.

### Propuesta

Que los controllers no cachen `RuntimeException`: dejar que `GlobalExceptionHandler` mapee por
tipo (`MethodArgumentNotValidException` → 400, `EntityNotFoundException` → 404,
`DataIntegrityViolationException` → 409, resto → 500). Y sacar el `NullPointerException` como
señal de negocio del punto 5/7: para eso están las `IllegalArgumentException`.

---

## 22. No hay Bean Validation: entran cantidades negativas como `Bien.peso`

**Estado:** abierto
**Severidad:** baja
**Archivos:**
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/dto/personaDonante/FormularioRequestDTO.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/dto/donaciones/BienResumenDTO.java`

### Qué pasa

Un `grep` de `jakarta.validation`, `@Valid`, `@NotNull`, `@NotBlank`, `@Positive` en
`src/main` devuelve exactamente **un** resultado: el `@NotNull` de
`GestorAsignaciones.java:143`, que además es el import equivocado (`org.jetbrains.annotations`,
no el de Bean Validation) y está sobre un método `private static`.

Los DTOs de entrada son cajas de Lombok sin una sola restricción. `FormularioRequestDTO`
(líneas 13-16) no exige `idDonante` ni `fechaRealizacion`; `BienResumenDTO` (líneas 11-20) no
exige `tipoBien` (ver punto 7) ni que `cantidad` sea positiva.

El caso concreto: `POST /donaciones/formulario` con
`{"bienes":[{"tipoBien":"CON_ESTADO","descripcion":"arroz","cantidad":-50,"usado":false}]}`.
`BienResumenDTO.toDomain` (línea 50) sólo protege el `null` (`cantidad != null ? cantidad : 0`),
así que guarda `peso = -50` en `Bien.peso` (línea 46 de `Bien.java`). Ese `-50` entra directo
en `Donacion.sumaCantidadBienes()` (líneas 100-102), que es el numerador de
`CompatibilidadSemantica.calcularScore` y el sumador de `Necesidad.cantidadRecibida()` y de
`NecesidadRecurrente.cantidadRecibidaEnPeriodo()`. El resultado es una necesidad que "recibió"
menos de lo que tenía y un score negativo que el filtro de la línea 51 descarta en silencio.

Lo mismo con `Humana.edad` (línea 19 de `Humana.java`): `int edad` acepta -3 y 0 sin que nadie
lo mire, y `PersonaDonanteFilaConverter` lo carga con un 0 hardcodeado (línea 82).

### Por qué no se ve

La API responde 201 y devuelve el `id` del bien recién creado, así que el cliente no tiene
por qué sospechar. El `-50` recién aparece semanas después, cuando una necesidad que debía
estar cubierta no se cierra y nadie sabe por qué: la única pista es un score negativo que
`CompatibilidadSemantica` filtra con un `continue` sin log (línea 51).

### Propuesta

Poner `@Valid` en los `@RequestBody` y anotaciones en los DTOs. Es el arreglo más chico de
toda la lista y el que más bugs futuros evita: hoy cada endpoint tiene que defenderse a mano y
ninguno lo hace.

---

## 23. La segmentación no incluye la unidad de medida y suma kilos con litros

**Estado:** abierto
**Severidad:** baja
**Archivo:**
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/SegmentadorDonaciones/SegmentadorDonaciones.java`

### Qué pasa

`generarClaveSegmentacion` (líneas 43-54) arma la clave del segmento con subcategoría, fecha de
vencimiento (si es perecedero) y usado/nuevo (si tiene estado). **La unidad de medida no
entra**: `Bien.unidadUtilizada` es un `UnidadDeMedida` (`KILOGRAMOS`, `LITROS`, `UNIDADES`) y
se ignora por completo.

Consecuencia: un formulario con "10 kilos de arroz" (KILOGRAMOS) y "5 litros de aceite"
(LITROS) de la misma subcategoría cae en el mismo grupo. `crearDonacion` (líneas 56-68) crea
**una sola** Donación con los dos bienes, y `Donacion.sumaCantidadBienes()` (líneas 100-102)
los suma: 15. Ese 15 se compara contra `cantidadObjetivo` de la necesidad, que está en una
sola unidad, en `CompatibilidadSemantica.calcularScore` y en `Necesidad.cantidadRecibida()`. Un
kilo vale un kilo y un litro no es medio kilo.

El mismo `--` aparece en el `toString` de `UnidadDeMedida`: el propio comentario de la línea 3
dice "q siempre lo pese, no unidades", o sea, que la decisión de reducir todo a un número está
pendiente de definirse.

### Por qué no se ve

La segmentación devuelve una donación por grupo y el grupo se ve bien: la descripción dice
"Segmento de donación: Almacenes" y los bienes están todos ahí. Nadie revisó que la lista
mezcle kilos con litros, porque el modelo trata `peso` como un número sin unidad y la UI nunca
muestra la unidad.

### Propuesta

O agregar `unidadUtilizada` a la clave de segmentación (y entonces cada donación es de una sola
unidad), o convertir todo a peso en el ingreso y sacar la `UnidadDeMedida` del modelo. Lo que no
puede es sumar una unidad con otra y usar el resultado para decidir asignaciones.

---

## 24. El PUT de donación deja los bienes anteriores huérfanos

**Estado:** abierto
**Severidad:** baja
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/DonacionService.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/Donaciones/Donacion.java`

### Qué pasa

`Donacion.bienes` (líneas 62-64 de `Donacion.java`) es un
`@OneToMany(cascade = {PERSIST, MERGE}) @JoinColumn(name = "donacion_id")`: unidireccional,
**sin** `orphanRemoval`, sin `CascadeType.REMOVE`.

`DonacionService.actualizarDonacion` (líneas 102-106) reemplaza la colección entera:

```java
existente.setBienes(bienesActualizados);
```

Como no hay `orphanRemoval`, Hibernate no borra los bienes que quedaron afuera de la lista:
los desasocia (les deja `donacion_id` en `NULL`) y quedan vivos en la tabla `bien` como filas
que ya no pertenece a ninguna donación. Peor: como los bienes se persisten antes, uno por uno,
con `crearBien` en la línea 104, cada PUT crea filas nuevas aunque la lista sea idéntica a la
que ya estaba.

El `Donacion.actualizarDonacion` tampoco valida el estado: se le puede mandar un PUT a una
donación `ENTREGADA` y le cambia el contenido del segmento, con lo que los conteos de
`Necesidad.cantidadRecibida()` quedan sin correspondencia con lo que realmente se entregó.

### Por qué no se ve

El PUT responde 200 con la donación actualizada y la lista de bienes que se mandó es la que
queda visible en la respuesta y en la base. Las filas viejas no aparecen en ningún lado: no
tiene Dueño, no se listan y no se cuentan. Sólo se descubren mirando el total de filas de la
tabla `bien` contra el total de bienes referenciados.

### Propuesta

Dos opciones consistentes: `orphanRemoval = true` en `Donacion.bienes` (que además obligaría a
que los bienes dejaran de ser propiedad compartida con `Formulario.donaciones`, que tiene su
propia `donacion_id`/`formulario_id` en la misma tabla `bien`), o un diff explícito de la lista
en el servicio. Y restringir el PUT a donaciones en `EN_DEPOSITO`.

---

## 25. La importación CSV se traga los errores y no dice cuántos entraron

**Estado:** abierto
**Severidad:** baja
**Archivo:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/DonanteService.java`

### Qué pasa

`importarDonantes` (líneas 102-117) corre `lector.importar(is)` dentro de un
`CompletableFuture.runAsync` y responde `"Importación en segundo plano iniciada."` de
inmediato. Por dentro:

- línea 111: `catch (Exception ignored) {}` por cada donante. Si el 90% del CSV está mal, no
  queda rastro.
- línea 113: `catch (IOException ignored) {}`. Si el archivo se corta a la mitad, se pierde
  todo y no se dice nada.
- No hay ningún contador ni acumulador: el 202 no dice cuántos entraron, cuántos fallaron ni
  por qué.

Además el trabajo corre sobre el `ForkJoinPool.commonPool()` sin ningún executor propio ni
límite de concurrencia, así que una importación grande compite por los mismos hilos que
atienden requests.

El resultado es un endpoint que devuelve 202 siempre, sin importar qué pase después. Es la
misma decisión del punto 3 (no cortar el flujo), pero acá no hay ni siquiera un `System.err`:
el fallo es completamente invisible.

### Por qué no se ve

El 202 es la respuesta correcta para un job en segundo plano, así que el front la muestra como
"importación iniciada" y sigue. La cantidad de donners importados se puede contar a mano con
`GET /personas` un rato después, y si no coincide con el CSV nadie tiene un número contra el cual
comparar.

### Propuesta

Devolver un reporte (conteos y los N primeros errores con número de línea, que
`LectorCSV.procesarYGuardarFila` ya calcula en la línea 61), o persistir el resultado de la
importación y que el front lo consulte. Y aunque siga siendo asíncrono, usar un executor
propio en vez del common pool.

---

## 26. El PUT de necesidad ignora el id de entidad y castea a ciegas

**Estado:** abierto
**Severidad:** baja
**Archivos:**
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/EntidadBeneficiariaService.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/repositories/repos/RepositorioNecesidades.java`

### Qué pasa

El endpoint es `PUT /entidades/{id}/necesidades/{idNecesidad}`
(`EntidadBeneficiariaController:105-113`). El `{id}` de entidad viaja pero
`EntidadBeneficiariaService.actualizarNecesidad(UUID id, NecesidadDTO dto)` (líneas 90-97) lo
recibe como parámetro `id` y **no lo usa nunca**: busca la necesidad por `idNecesidad` y la
modifica, sin verificar que pertenezca a la entidad del path.

Consecuencia: `PUT /entidades/{entidadA/necesidades/{necesidadDeEntidadB}` modifica la
necesidad de la entidad B y devuelve 200. Con eso, el endpoint de "actualizar necesidad de esta
entidad" sirve para editar cualquier necesidad del sistema.

El segundo problema está en `RepositorioNecesidades.modificarNecesidad` (líneas 73-93):

```java
if (datosNuevos instanceof NecesidadRecurrente) {
    ((NecesidadRecurrente) existente).setPlazoEnDias(...);
}
```

Se chequea el tipo del **nuevo** objeto pero se castea el **existente**. Si el PUT cambia el
tipo (de `EXTRAORDINARIA` a `RECURRENTE`, o al revés), el cast explícito de la línea 82 tira
`ClassCastException` y responde 500. `NecesidadDTO.toDomain` (líneas 34-42) permite cambiar el
tipo libremente, así que mandar `{"tipoNecesidad":"RECURRENTE","plazoEnDias":30}` contra una
necesidad extraordinaria ya guardada lo dispara.

### Por qué no se ve

El endpoint tiene dos path variables y devuelve la necesidad actualizada, así que a simple vista
parece que valida las dos. Lo que pasa es que el `{id}` de entidad es un argumento que el
servicio recibe y nunca lee: es código muerto que el compilador no señala porque es un
parámetro con nombre. El `catch (IllegalArgumentException) → 404` del controller (líneas
110-112) tampoco ayuda, porque el `ClassCastException` no es `IllegalArgumentException` y sale
por el catch-all como un 500 genérico.

### Propuesta

Usar el `{idEntidad}` del path para validar pertenencia (`entidad.buscarNecesidadPorId(idNecesidad)`)
y devolver 404 si no está. Y en `modificarNecesidad`, decidir qué pasa cuando cambia el tipo:
rechazar el cambio con un 400, o reemplazar la entidad en vez de castear.

---

# Corregidos
