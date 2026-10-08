# Pendientes técnicos de `donaciones-service`

Registro de problemas conocidos del servicio, con el motivo y la propuesta de arreglo para
que no se pierdan de vista al crecer el código.

**Están ordenados de más urgente a menos urgente**, no por número de punto. El número es un
ID estable y no se renumera nunca, así que quedan huecos. Un punto corregido se borra de esta
lista y pasa a la sección [Corregidos](#corregidos) del final.

El orden no es el de la severidad declarada en cada punto sino el del daño real: cuánto se
rompe cuando pasa, y qué tan fácil es que pase.

| #  | Punto | Por qué está acá                                                                                  |
|----|-------|---------------------------------------------------------------------------------------------------|
| 4  | 4     | Declara el bean de `notificaciones-service` como dependencia de Maven                             |

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

## Corregidos

### `IncentivosClient` publicaba contra la raíz del servicio: 404 y 405 garantizados

**Estado:** corregido
**Severidad:** crítica
**Archivos:** `.../clients/IncentivosClient.java`

Las dos llamadas entre `donaciones-service` e incentivos apuntaban a rutas que no existen:

| Método                      | Antes                             | Ahora                               | Ruta real                                  |
|-----------------------------|-----------------------------------|-------------------------------------|--------------------------------------------|
| `peticionCrearPerfil`       | `POST http://localhost:8082/`     | `POST /api/perfiles`                | `POST /api/perfiles`                       |
| `notificarDonacionAsignada` | `POST http://localhost:8082/{id}` | `PATCH /api/perfiles/donacion/{id}` | `PATCH /api/perfiles/donacion/{idUsuario}` |

El método HTTP del avance es **`PATCH`, no `POST`**: `POST` contra un endpoint que solo declara
`PATCH` devuelve 405, y contra una ruta inexistente devuelve 404.

**Lo que lo escondía:** el `catch (Exception)` con `System.err.println` se tragaba el error y
el servicio seguía como si la notificación hubiera salido. Ahora loguea con nivel, incluye la
URL exacta y **relanza**, para que el fallo de una integración no se confunda con un fallo de
dominio.

**Cómo se verificó:** `POST /api/personas` responde `201` y el perfil queda creado en la base de
incentivos. Antes devolvía `500` por el `401` que le llegaba de vuelta.

### `NotificacionesClient` apuntaba a la raíz: 404 en cada notificación

**Estado:** corregido
**Severidad:** crítica
**Archivos:** `.../clients/NotificacionesClient.java`,
`src/main/resources/application.properties`

El cliente publicaba contra `http://localhost:8083/` (la raíz). La ruta real es
`POST /api/notificaciones`, porque el controller cuelga de `@RequestMapping("/notificaciones")`
y el servicio tiene `context-path=/api`.

El error se veía en el log: `No se pudo enviar la notificación a
http://localhost:8083/notificaciones: 404`. La ruta estaba mal en dos lugares —el default de la
propiedad y el sufijo que compone el cliente— y los dos hacía falta.

**Qué se cambió:** la propiedad apunta a `http://localhost:8083/api` y el cliente compone
`/notificaciones`, normalizando la barra final para que no produzca `/api//notificaciones`. Si
alguien configura la propiedad con el sufijo completo, el cliente lo detecta y no lo duplica.

**Cómo se verificó:** al crear un donante, la notificación "Nuevo Registro en DonaTrack" llega
y queda persistida en la base de notificaciones.

### Las URLs por defecto no incluían el context-path

**Estado:** corregido
**Severidad:** alta
**Archivo:** `src/main/resources/application.properties`

Los defaults apuntaban a la raíz de los servicios que tienen `context-path=/api`, así que
cualquier llamada sin variable de entorno daba 404 aunque la ruta estuviera bien escrita:

- `servicio.notificaciones.url` era `http://localhost:8083/` → ahora `http://localhost:8083/api`.
- `servicio.incentivos.url` era `http://localhost:8082/` → ahora `http://localhost:8082`
  (incentivos **no** tiene context-path, sus rutas ya empiezan con `/api`; el doble `/api`
  habría sido el error en sentido contrario).

**Cómo se verificó:** levantando los cuatro servicios y recorriendo todas las llamadas
HTTP entre ellos.

### Faltaba el bloque `spring.rabbitmq.*`

**Estado:** corregido
**Severidad:** media
**Archivo:** `src/main/resources/application.properties`

Sin el bloque, el `CachingConnectionFactory` se crea con los defaults de Spring Boot
(`localhost:5672`, `guest`/`guest`). En la máquina de desarrollo funciona; dentro de un
contenedor busca `localhost`, que es el propio contenedor, y no encuentra al broker.

**Qué se cambió:** `spring.rabbitmq.host`, `port`, `username` y `password` parametrizados por
variables de entorno, igual que el resto de la configuración.

### Los DTO de integración están duplicados a mano entre los dos servicios

**Estado:** documentado, no corregido
**Severidad:** media
**Archivos:** `.../dto/logistica/entrega/EntregaDTO.java`, `.../dto/logistica/entrega/BienDTO.java`

Ver punto 27. El contrato son DTO copiados en cada módulo sin nada que los mantenga
sincronizados. Los nombres de campo coinciden hoy; si uno de los dos lados renombra, el mensaje
llega al listener y falla con `MessageConversionException`, que no dice cuál de los dos se
desalineó. Ese error se sufrió durante esta tanda y costó tiempo de diagnóstico.

---

# Corregidos

### 1. Las dos llamadas a otros servicios apuntaban a rutas que no existían

**Estado:** corregido
**Severidad:** crítica
**Archivo:** `.../clients/IncentivosClient.java`, `.../clients/NotificacionesClient.java`

### Qué pasaba

Los dos clientes de salida apuntaban mal, por motivos distintos:

**`IncentivosClient`.** La propiedad por defecto era `http://localhost:8082` sin el context-path,
y las rutas que concatenaba no existían:

| Llamada           | Antes                          | Ahora                               |
|-------------------|--------------------------------|-------------------------------------|
| Crear perfil      | `POST /`                       | `POST /api/perfiles`                |
| Reportar donación | `POST /perfiles/donacion/{id}` | `PATCH /api/perfiles/donacion/{id}` |

Lo de `POST` contra un endpoint que solo declara `PATCH` es lo que más fácil de pasar por alto:
aunque la ruta hubiera existido, el métodoverbs mismatch da **405**, no 404.

**`NotificacionesClient`.** Iba por HTTP a la raíz del otro servicio, así que cada
notificación daba 404. Y según el enunciado no tenía que ir por HTTP en absoluto: la integración
de los servicios de dominio con notificaciones es asíncrona por cola de mensajes.

### Qué se hizo

- `IncentivosClient` usa las rutas reales, con `exchange(..., HttpMethod.PATCH, ...)` para el
  informe de donación, y loguea la URL que intenta antes de llamar.
- `NotificacionesClient` **migró a Rabbit**: publica en `notificaciones.exchange` con la routing key
  `notificaciones.donacion`. Es la razón por la que se agregó el exchange a este `RabbitMQConfig`.

### Cómo se verificó

Los cuatro servicios levantados contra MySQL y RabbitMQ reales:

- `POST /api/personas` en donating → **201**, y el perfil aparece creado en la base de
  incentivos.
- `PATCH /api/perfiles/donacion/{id}` en incentivos → **200 `true`**, que es la respuesta de que
  la misión se completó.
- El alta de donante publica por Rabbit y la notificación queda `ENVIADA` con `fecha_envio`.

---

### 2. Dos bindings de Rabbit ataban al exchange equivocado

**Estado:** corregido
**Severidad:** alta
**Archivo:** `.../config/RabbitMQConfig.java`

### Qué pasaba

Este módulo declaraba **colas y bindings que son de logística**, y encima los ataba al exchange
equivocado. Logística tenía su propia copia de esos beans, idéntica salvo por esos dos bindings
mal atados.

Con las dos declaraciones, el broker aceptaba ambas. El binding bueno de este módulo tapaba el
malo del otro, así que en el arranque normal de los cuatro juntos todo parecía andar. Pero
levantando logística sola, las colas que este módulo declara no existen y la cadena se corta:
**el bug estaba oculto por el orden de arranque**.

### Qué se hizo

La frontera quedó así: **cada servicio declara lo suyo y nada más.**

- Este módulo declara `logistica.exchange`, `logistica.eventos.exchange` y
  `notificaciones.exchange` (los tres de los que publica), más **su** cola de eventos y **su**
  binding.
- Logística declara las colas que consume.

Un exchange es un punto de encuentro: lo declara quien publica y lo usan todos los que lo
consumen, así que el nombre no vive en los dos lados.

De paso se sacaron los dos bindings con `#.` del routing key. En RabbitMQ el comodín `#` **exige
al menos un nivel más**: `notificaciones.incentivo.#` no matchea su propia clave
`notificaciones.incentivo`. El broker aceptaba el mensaje y lo descartaba — la peor combinación
para diagnosticar, porque `routed=true` y no hay mensaje.

### Cómo se verificó

Los bindings declarados en el broker, con las tres routing keys de notificaciones unidas a la
cola:

```
--[notificaciones.donacion]--> notificaciones
--[notificaciones.evento.logistica]--> notificaciones
--[notificaciones.incentivo]--> notificaciones
```

Y los mensajes de los dos servicios de dominio salen efectivamente por esa cola.

---

### 3. Se tragaba las excepciones de salida a propósito, sin log estructurado

**Estado:** corregido
**Severidad:** media
**Archivo:** `.../clients/IncentivosClient.java`

### Qué pasaba

Los clientes de salida envolvían la llamada en un `try/catch` que **se tragaba la excepción sin
loguear nada**. El síntoma era el peor posible: la operación de dominio se completaba y devolvía
`201` o `200`, el servicio parecía sano, y el mensaje nunca había salido. Nadie se enteraba hasta
que un donante se quejaba de que no le llegó la notificación.

Es el mismo patrón que el `__TypeId__` del otro lado: **el servicio responde bien y el problema
aparece un salto después.**

### Qué se hizo

Los clientes ahora loguean la URL que van a llamar y **relanzan**. Que re-lancen es lo correcto:
el fallo de integración no es un fallo de la operación de dominio, y ocultarlo hacía que un
problema de conectividad fuera indistinguible de un noop.

Lo que se dejó como estaba: el `catch` sigue existiendo donde corresponde —publicar en Rabbit es
un efecto secundario de una donación que ya está guardada, y si el broker está caído la donación
ya está persistida. Ahí sí corresponde tragarse la excepción, pero **logueándola**.

### Cómo se verificó

El mensaje del `catch` incluye la URL y la routing key, que es lo que hace falta para diagnosticar
sin tener que reproducir:

```
No se pudo publicar la notificación con routing key notificaciones.donacion: <motivo>
```

---

### 4. Punto 5 — El endpoint de vencer una donación manda un estado que el parser no conoce

**Estado:** corregido
**Severidad:** crítica
**Archivo:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/DonacionService.java`

### Qué pasaba

`DonacionService.marcarComoVencida` llamaba `gestorAsignaciones.cambiarEstado(id, "VENCIDA", ...)`, pero el `switch` de `GestorAsignaciones.parseEstado` solo reconoce el case `"VENCIDO"` (el nombre real del enum `Estado.VENCIDO`). `"VENCIDA"` caía siempre en el `default` y tiraba `IllegalArgumentException`, que el controller traducía a un 404 limpio: `PATCH /donaciones/{id}/vencer` estaba roto el 100% de las veces.

### Qué se hizo

`marcarComoVencida` ahora llama a `cambiarEstado(id, "VENCIDO", ...)`, igualando el string al nombre real del enum. El desacople de fondo que proponía el punto —que `cambiarEstado` reciba un `Estado` y no un `String`— no se hizo: el `switch` de `parseEstado` sigue aceptando un string suelto, así que un futuro renombre del enum puede volver a romper esto en silencio.

### Cómo se verificó

Nota para toda esta tanda (puntos 5 a 29 del backlog original, entradas 4 a 28 de esta sección): no hay Maven instalado en este entorno, así que ninguno de estos arreglos se verificó corriendo `mvn compile`/`mvn test` ni levantando los servicios. La verificación fue por lectura manual del diff contra la descripción de cada bug; de acá en más se indica solo lo puntual revisado en cada entrada. Acá: se confirmó que el único string que `DonacionService` le pasa a `cambiarEstado` para vencer una donación ("VENCIDO") coincide exactamente con el único case que reconoce `GestorAsignaciones.parseEstado`.

---

### 5. Punto 6 — Una estrategia de notificación no era bean: toda entrega fallida reventaba

**Estado:** corregido
**Severidad:** crítica
**Archivo:** `models/entities/ServicioMensaje/EstrategiasMensajes/NotificacionEntregaFallida.java`, `models/entities/ServicioMensaje/FabricaEstrategiasNotificacion.java`

### Qué pasaba

`NotificacionEntregaFallida` era la única de las seis estrategias sin `@Component`. `FabricaEstrategiasNotificacion` arma su mapa inyectando `List<EstrategiaNotificacion>` (solo los beans), así que la clave `ENTREGA_NO_RECIBIDA` nunca entraba al mapa y `GestorEventosLogistica.manejarEntregaFallida` tiraba `IllegalArgumentException` sin que nada lo mostrara.

### Qué se hizo

El `@Component` que falta en `NotificacionEntregaFallida` ya se había agregado en un commit anterior de esta misma rama (`a789c93`, "Arregle errores en notificaciones"), antes de esta tanda de cambios sin commitear. Lo que se agregó en este diff es la defensa estructural que proponía el punto: `FabricaEstrategiasNotificacion` ahora recorre, al construirse, todos los valores de `TipoEventoNotificacion` y tira `IllegalStateException` si a alguno le falta la estrategia registrada como bean, en vez de esperar al primer evento en producción para fallar.

### Cómo se verificó

Revisado manualmente: `NotificacionEntregaFallida` tiene `@Component` en el archivo actual, y el nuevo chequeo de `FabricaEstrategiasNotificacion` recorre los seis valores de `TipoEventoNotificacion` contra el mapa de estrategias inyectadas.

---

### 6. Punto 7 — Un bien sin `tipoBien` se convertía en `null` y reventaba la segmentación

**Estado:** corregido
**Severidad:** crítica
**Archivo:** `dto/donaciones/BienResumenDTO.java`, `services/DonacionService.java`

### Qué pasaba

`BienResumenDTO.toDomain` devolvía `null` cuando `tipoBien` venía vacío, en vez de rechazar el item. `DonacionService.procesarFormulario` metía ese `null` en la lista de bienes sin filtrarlo, y llegaba intacto a `SegmentadorDonaciones.segmentar`, que explotaba con NPE (`POST /donaciones/formulario` → 500 en vez de 400).

### Qué se hizo

`BienResumenDTO.toDomain` ahora tira `IllegalArgumentException("El tipo de bien es obligatorio")` en vez de devolver `null`. `DonacionService` reemplazó el `.map(this::resolverBien)` suelto por un nuevo método privado `resolverBienes`, que itera con índice y re-lanza la excepción prefijada con `"Bien en la posición N: ..."`, para que el 400 resultante diga cuál ítem de la lista está mal. De paso se agregaron `@Positive` en `cantidad` y `@NotBlank` en `tipoBien` sobre el propio DTO (Bean Validation, comparte diff con el punto 22).

### Cómo se verificó

Revisado manualmente: el único `return null` de `toDomain` fue reemplazado por el `throw`, y `resolverBienes` quedó como el único punto de entrada para mapear `List<BienResumenDTO>` a `List<Bien>` tanto en `procesarFormulario` como en `actualizarDonacion`.

---

### 7. Punto 8 — No había una sola transacción en el módulo

**Estado:** corregido
**Severidad:** alta
**Archivo:** `services/DonacionService.java`

### Qué pasaba

Ningún método de servicio tenía `@Transactional`: cada `jpaRepository.save()` commiteaba solo. `procesarFormulario` y `asignarPropuesta` encadenaban varias escrituras, y si una fallaba a mitad de camino, las anteriores ya habían quedado persistidas (bienes/formularios huérfanos, o una donación asignada sin propuesta para aprobar).

### Qué se hizo

Se agregó `@Transactional(rollbackFor = Exception.class)` a los seis métodos que proponía el punto: `procesarFormulario`, `actualizarDonacion`, `asignarPropuesta`, `ejecutarMatchmakingADemanda`, `cambiarEstado` y `eliminarDonacion`; y de paso a `marcarComoVencida`, que hace el mismo tipo de escritura aunque no estaba en la lista original.

### Cómo se verificó

Revisado manualmente que la anotación está en los seis métodos mencionados en la propuesta original más `marcarComoVencida`. No se pudo verificar en runtime que el rollback efectivamente revierte una escritura parcial.

---

### 8. Punto 9 — Un fallo en una donación cortaba el lote de matchmaking entero

**Estado:** corregido
**Severidad:** alta
**Archivo:** `models/entities/AsignadorDonaciones/AsignadorDonaciones.java`

### Qué pasaba

`ejecutarMatchmakingBatch` recorría las donaciones con un `forEach` sin try/catch: la primera excepción abortaba el resto del lote. Y `registrarDonacionPendienteDeAprobacion` cambiaba el estado a `PENDIENTE_ASIGNACION` *antes* de guardar el `ResultadoMatchmaking`; si el guardado fallaba (resultado duplicado), la donación quedaba en ese estado sin resultado y el scheduler, que solo recoge `EN_DEPOSITO`, no la volvía a ver nunca.

### Qué se hizo

El `forEach` se cambió por un `for` con try/catch por donación (loguea con `System.err` y sigue con la siguiente). Y en `registrarDonacionPendienteDeAprobacion` se invirtió el orden: ahora primero se guarda el `ResultadoMatchmaking` y recién después se cambia el estado a `PENDIENTE_ASIGNACION`, así que si el guardado falla el estado no llega a cambiar.

### Cómo se verificó

Revisado manualmente el orden de las dos líneas (guardar resultado, después cambiar estado) y que el nuevo try/catch envuelve únicamente la llamada a `procesarMatchmaking` por elemento del lote.

---

### 9. Punto 10 — `fechaEntrega` nunca se persistía

**Estado:** corregido
**Severidad:** alta
**Archivo:** `models/entities/Donaciones/Donacion.java`

### Qué pasaba

Ningún camino del código escribía `Donacion.fechaEntrega`: quedaba siempre `null`, y de ahí dependían `SubAtendidos.cantidadDonacionesUltimoTrimestre` y `NecesidadRecurrente.cantidadRecibidaEnPeriodo`, que filtran por `fechaEntrega != null`. El resultado era que esos dos algoritmos devolvían siempre 0, sin ninguna excepción que lo delatara.

### Qué se hizo

`Donacion.actualizarEstado` ahora setea `this.fechaEntrega = LocalDate.now()` la primera vez que el nuevo estado es `ENTREGADO` (si todavía no tenía fecha). Se centralizó ahí, no en los gestores, porque hay dos caminos a `ENTREGADO`: `GestorAsignaciones.cambiarEstado` (manual) y `GestorEventosLogistica.manejarEntregaConfirmada` (evento de logística), y los dos pasan por `actualizarEstado`.

### Cómo se verificó

Revisado manualmente que `actualizarEstado` es el único setter de `fechaEntrega` en la entidad y que ambos gestores llaman a `actualizarEstado(Estado.ENTREGADO, ...)` sin pasar por otro camino que lo esquive.

---

### 10. Punto 11 — Guardar el estado antes de notificar dejaba el cambio persistido y respondía 404

**Estado:** corregido
**Severidad:** alta
**Archivo:** `models/gestores/GestorAsignaciones.java`

### Qué pasaba

`cambiarEstado` guardaba la donación y *después* llamaba a `procesarAccionesPostCambioEstado`, que desreferenciaba `donacion.getEntidad()` y `donacion.getSubcategoria()` sin guarda de nulidad. Si la donación todavía no tenía entidad asignada (el caso normal de una donación recién segmentada), explotaba con NPE, pero el cambio de estado ya había commiteado, y el controller devolvía 404.

### Qué se hizo

Se invirtió el orden: `procesarAccionesPostCambioEstado` se llama *antes* de `repositorioDonaciones.guardar(donacion)`. Y se agregaron guardas explícitas al principio de ese método: si la transición es a `ASIGNADO` y la donación no tiene `entidad` o no tiene `subcategoria`, tira `IllegalArgumentException` con un mensaje descriptivo en vez de dejar que la NPE salga más adentro.

### Cómo se verificó

Revisado manualmente el orden de las dos llamadas en `cambiarEstado` y que las dos guardas de nulidad están antes de cualquier acceso a `getPersonaJuridica()`/`getRazonSocial()`. La validación de transición de estado "legal" que también proponía el punto no se implementó; sigue pendiente.

---

### 11. Punto 12 — `CascadeType.ALL` en las necesidades borraba de más al dar de baja una entidad

**Estado:** corregido
**Severidad:** alta
**Archivo:** `models/entities/EntidadBeneficiaria/EntidadBeneficiaria.java`

### Qué pasaba

`EntidadBeneficiaria.necesidades` tenía `cascade = CascadeType.ALL, orphanRemoval = true`. Borrar una entidad borraba en cascada todas sus necesidades, y esas necesidades estaban referenciadas por `Donacion.necesidad` (un `@ManyToOne` sin cascade): las donaciones quedaban apuntando a filas inexistentes.

### Qué se hizo

El cascade de `necesidades` se bajó a `{CascadeType.PERSIST, CascadeType.MERGE}` (sin `REMOVE` ni `orphanRemoval`). El borrado de necesidades queda a cargo explícitamente de `RepositorioNecesidades`, que es quien las tiene en su propio repositorio.

### Cómo se verificó

Revisado manualmente que la anotación ya no incluye `CascadeType.ALL` ni `orphanRemoval`. No se verificó en runtime (requeriría dar de baja una entidad con necesidades y donaciones asignadas contra una base real).

---

### 12. Punto 13 — El PUT de donante cambiaba el `@Id` y duplicaba la Persona

**Estado:** corregido
**Severidad:** alta
**Archivo:** `models/repositories/repos/RepositorioPersonas.java`

### Qué pasaba

`modificarPersona` hacía `existente.setId(datosNuevos.getId())` antes de guardar. `datosNuevos` siempre viene de un objeto transiente recién construido (con un UUID nuevo generado en su propio constructor), así que esa línea le cambiaba el id a una entidad ya persistida. El `save()` posterior hacía un `merge()` que, al no encontrar fila con el id nuevo, insertaba una Persona duplicada en vez de actualizar la original.

### Qué se hizo

Se borró la línea `existente.setId(datosNuevos.getId())`. El id de una Persona ya no se reasigna en un update.

### Cómo se verificó

Revisado manualmente que la línea fue eliminada y que no queda ningún otro punto de `modificarPersona` que toque `existente.setId(...)`.

---

### 13. Punto 14 — Las propuestas se numeraban desde 1 pero se leían desde 0

**Estado:** corregido
**Severidad:** media
**Archivo:** `models/gestores/GestorMatchmaking.java`

### Qué pasaba

El ranking expuesto al front es 1-based (`extraerRanking`/`obtenerInterseccion` setean `posicion` entre 1 y N), pero `GestorMatchmaking.obtenerPropuestaSeleccionadaParaDonacion` indexaba la lista con `.get(posicion)` (0-based). Aprobar "la propuesta 1" de la pantalla terminaba asignando la segunda, y la última propuesta de la lista se rechazaba como "inválida" aunque fuera válida.

### Qué se hizo

La validación de rango se cambió a `posicion < 1 || posicion > size` (antes `posicion < 0 || posicion >= size`), y el acceso a la lista pasó de `.get(posicion)` a `.get(posicion - 1)`. El contrato quedó fijado como 1-based de punta a punta: así lo setean los algoritmos y así lo lee este método.

### Cómo se verificó

Revisado manualmente que el rango de validación y el índice usado en `.get()` son consistentes entre sí (1-based) y con `PropuestaAsignacionDTO.posicion`, que es lo que ve el front.

---

### 14. Punto 15 — El score de compatibilidad medía contra el histórico, no contra el período

**Estado:** corregido
**Severidad:** media
**Archivo:** `models/entities/AsignadorDonaciones/AlgoritmosDeAsignacion/CompatibilidadSemantica.java`, `models/entities/Necesidades/Necesidad.java`, `models/entities/Necesidades/NecesidadRecurrente.java`

### Qué pasaba

`CompatibilidadSemantica.calcularScore` calculaba `cantidadFaltante` contra `necesidad.cantidadRecibida()` (histórico completo), pero `esCompatibleCon`/`estaSatisfecha()` decide la compatibilidad de una `NecesidadRecurrente` contra `cantidadRecibidaEnPeriodo()` (con ventana temporal). Una necesidad recurrente ya llenada en el pasado y vencida quedaba "compatible" (período en 0) pero con score `<= 0` (histórico ya cubierto), así que el filtro la descartaba para siempre. Además, `cantidadDonada == 0` producía una división `0.0/0` (`NaN`) que se colaba por el filtro `score <= 0` y rompía el orden del heap.

### Qué se hizo

Se agregó `Necesidad.cantidadFaltante()` (contra el histórico, comportamiento por defecto) y `NecesidadRecurrente` la sobrescribe contra `cantidadRecibidaEnPeriodo()`. `calcularScore` ahora llama a `necesidad.cantidadFaltante()` en vez de calcular la resta a mano, así que usa la misma ventana que decide la compatibilidad. Se agregó también una guarda explícita: si `cantidadFaltante <= 0` o `cantidadDonada <= 0`, el método devuelve `0` antes de llegar a la división, blindando el caso `NaN`.

### Cómo se verificó

Revisado manualmente que `calcularScore` ya no calcula la resta inline sino que delega en `cantidadFaltante()`, y que la guarda cubre los dos denominadores que podían dar 0/0. No se corrieron los algoritmos de matchmaking contra datos reales.

---

### 15. Punto 16 — La lista de formularios del donante era `@Transient`: la inactividad nunca se avisaba

**Estado:** corregido
**Severidad:** media
**Archivo:** `models/entities/donador/Donante.java`, `models/entities/Donaciones/Formulario/Formulario.java`

### Qué pasaba

`Donante.formularios` estaba anotado `@Transient`: nunca se cargaba desde la base, así que `DonanteService.revisarActividades` (el scheduler de inactividad) siempre veía la lista vacía y la notificación de "20 días sin donar" no se mandaba nunca. El dueño real de la relación (`Formulario.donante`) ya existía, pero el lado inverso no estaba mapeado.

### Qué se hizo

`Donante.formularios` pasó de `@Transient` a `@OneToMany(mappedBy = "donante")`, usando a `Formulario.donante` como dueño (ya tenía `@JoinColumn("donante_id")`), sin agregarle cascade propio. Y para evitar el NPE que el propio punto anticipaba (`revisarActividades` llamando `getFechaRealizacion()` sobre un valor nulo), el constructor de `Formulario` ahora defaultea `fechaRealizacion` a `LocalDate.now()` si viene `null`, en vez de dejarlo pasar.

### Cómo se verificó

Revisado manualmente que `Formulario.donante` sigue siendo el lado dueño (`@JoinColumn`) y que `Donante.formularios` solo agrega `mappedBy`, sin cascade propio. No se pudo levantar el contexto de Hibernate para confirmar el mapeo bidireccional contra el esquema real.

---

### 16. Punto 17 — El ranking de sub-atendidos lo monopolizaba una sola entidad

**Estado:** corregido
**Severidad:** media
**Archivo:** `models/entities/AsignadorDonaciones/AlgoritmosDeAsignacion/SubAtendidos.java`

### Qué pasaba

El desempate del `PriorityQueue` usaba una comparación estricta (`cantidadDonaciones < top10.peek().getScore()`): si la primera entidad evaluada tenía 0 donaciones, llenaba el top10 entero, y ninguna otra entidad con el mismo score (también 0) podía desplazarla, porque `0 < 0` es `false`. El resultado siempre eran las necesidades de la primera entidad que tocaba la iteración.

### Qué se hizo

La comparación pasó de `<` a `<=`, así que una entidad con el mismo score que el peor del top10 ahora sí puede entrar a competir por el lugar. Y se agregó un `thenComparing(p -> p.getNecesidad().getId())` al comparador del heap, para que el desempate sea determinista (por id de necesidad) y no dependa del orden en que llegan las entidades.

### Cómo se verificó

Revisado manualmente el comparador (`Comparator.comparingDouble(...).reversed().thenComparing(...)`) y el cambio de `<` a `<=` en la condición de reemplazo. No se ejecutó el algoritmo contra un set de entidades con scores empatados.

---

### 17. Punto 18 — `cantidadObjetivo` sin validar cortaba el matchmaking con NullPointerException

**Estado:** corregido
**Severidad:** media
**Archivo:** `dto/entidadBeneficiaria/NecesidadDTO.java`

### Qué pasaba

`NecesidadDTO.toDomain` pasaba `cantidadObjetivo` (un `Integer`, admite `null`) sin chequear nada a los constructores de `Necesidad`. El alta de una necesidad sin `cantidadObjetivo` devolvía 201 igual, y el NPE aparecía recién en el scheduler de asignación (`estaSatisfecha()` hace `int >= Integer`), cortando el lote entero (ver punto 9 de esta misma sección). `plazoEnDias` tenía el mismo problema con valores `0` o negativos.

### Qué se hizo

`toDomain` ahora valida al principio: tira `IllegalArgumentException` si `cantidadObjetivo` es `null` o `<= 0`, y si `plazoEnDias` viene no nulo pero `<= 0`. El 400 sale en el alta (`POST /entidades/{id}/necesidades`), antes de que la necesidad inválida llegue a la base.

### Cómo se verificó

Revisado manualmente que las dos validaciones están al principio del método, antes de construir cualquier `Necesidad`, y que lanzan `IllegalArgumentException` (mapeada a 400 por `GlobalExceptionHandler`, ver punto 20 de esta sección).

---

### 18. Punto 19 — Borrar el medio de contacto predeterminado lo dejaba colgando

**Estado:** corregido
**Severidad:** media
**Archivo:** `models/entities/Mensaje/MedioDeContacto/MediosDeContacto.java`

### Qué pasaba

`eliminarMedioDeContacto` solo hacía `listaMediosDeContacto.remove(medio)`. Con `orphanRemoval = true` en esa lista, Hibernate borraba la fila en el flush, pero `medioDeContactoPredeterminado` (que no es dueño de la relación) seguía apuntando a ese id: violación de FK o, en el mejor caso, un predeterminado que apunta a una fila inexistente.

### Qué se hizo

Antes de sacar el medio de la lista, si coincide con `medioDeContactoPredeterminado`, se promueve a predeterminado el primero de los que quedan (o se deja `null` si no queda ninguno). Se aplicó tanto en `eliminarMedioDeContacto` como en `eliminarMediosDeContacto` (la versión que borra varios a la vez).

### Cómo se verificó

Revisado manualmente que la promoción ocurre antes del `.remove()`/`.removeAll()` en los dos métodos, y que cubre el caso de lista vacía (`ifPresentOrElse` con el `else` seteando `null`).

---

### 19. Punto 20 — Los eventos de logística no se aislaban dentro de una ruta

**Estado:** mitigado (parcial)
**Severidad:** media
**Archivo:** `models/gestores/GestorEventosLogistica.java`

### Qué pasaba

`manejarInicioRuta` envolvía el `for` de items **entero** en un único try/catch: un solo item con un UUID malformado, o una donación sin entidad asignada (NPE), abortaba el procesamiento del resto de las donaciones de esa ruta.

### Qué se hizo

El try/catch se movió adentro del loop: ahora cada item de `payload.getItems()` se procesa en su propio try/catch, así que un item roto solo descarta ese item (con un log) y el resto de la ruta sigue. El parseo del payload (`objectMapper.readValue`) también se separó a su propio try/catch, antes de entrar al loop.

**Lo que sigue sin resolver:** los otros tres manejadores que señalaba el punto (`manejarEntregaConfirmada`, `manejarEntregaFallida`, `manejarReingresoDeposito`) siguen llamando `UUID.fromString(evento.getReferenciaId())` sin ninguna protección propia, y la dead-letter queue que proponía la sección "Propuesta" no se implementó. Lo que evita que esto bloquee la cola para siempre es un mecanismo preexistente y no tocado en este diff: `EventosListener.recibirEvento` ya envuelve toda la llamada a `procesarEvento` en un try/catch de `RuntimeException` que loguea y descarta el evento en vez de dejar que la excepción se propague y el broker reintente indefinidamente. Ese comportamiento no formaba parte del planteo original del punto 20 ni de este batch de cambios.

### Cómo se verificó

Revisado manualmente que el try/catch de `manejarInicioRuta` quedó dentro del `for`, con su propio log por item, y que `EventosListener.recibirEvento` efectivamente atrapa `RuntimeException` alrededor de `procesarEvento`. No se verificó contra un broker real con un item de UUID inválido en el medio de una lista.

---

### 20. Punto 21 — Los controllers respondían 404 ante cualquier `RuntimeException`

**Estado:** corregido
**Severidad:** baja
**Archivo:** `controllers/DonacionController.java`, `exceptions/GlobalExceptionHandler.java`, `services/DonacionService.java`, `models/gestores/GestorAsignaciones.java`

### Qué pasaba

`obtenerDonacion`, `actualizarDonacion`, `cambiarEstado` y `marcarComoVencida` atajaban cualquier `RuntimeException` (NPE incluido) y devolvían 404, escondiendo el diagnóstico real de los puntos 5, 7, 11 y 14 de este backlog.

### Qué se hizo

Se sacaron los cuatro bloques `try { ... } catch (RuntimeException e) { return ResponseEntity.notFound().build(); }` de `DonacionController`. A cambio, `GlobalExceptionHandler` ahora mapea por tipo: se agregaron `@ExceptionHandler` para `EntityNotFoundException` (404), `DataIntegrityViolationException` (409) y `MethodArgumentNotValidException` (400, con el detalle de cada campo que falló). Para que el 404 real siga funcionando, `DonacionService.obtenerPorId`/`actualizarDonacion` y `GestorAsignaciones.cambiarEstado` pasaron de tirar `RuntimeException`/`IllegalArgumentException` genérica a tirar `jakarta.persistence.EntityNotFoundException` cuando el recurso no existe.

### Cómo se verificó

Revisado manualmente que ninguno de los cuatro endpoints de `DonacionController` sigue envolviendo la llamada al service en un catch de `RuntimeException`, y que los tres `@ExceptionHandler` nuevos están registrados en `GlobalExceptionHandler` junto con el catch-all de `Exception` (que ahora loguea con `log.error` en vez de `printStackTrace()`).

---

### 21. Punto 22 — No había Bean Validation: entraban cantidades negativas como `Bien.peso`

**Estado:** corregido
**Severidad:** baja
**Archivo:** `pom.xml`, `dto/personaDonante/FormularioRequestDTO.java`, `dto/donaciones/BienResumenDTO.java`, `controllers/DonacionController.java`

### Qué pasaba

Un `grep` de anotaciones de Bean Validation en `src/main` no devolvía nada útil: los DTOs de entrada no exigían `idDonante`, `fechaRealizacion`, `tipoBien`, ni que `cantidad` fuera positiva. Un `cantidad: -50` se guardaba tal cual en `Bien.peso` y envenenaba en silencio los cálculos de `CompatibilidadSemantica`/`Necesidad.cantidadRecibida()`.

### Qué se hizo

Se agregó la dependencia `spring-boot-starter-validation` al `pom.xml` (no estaba declarada, pese a que ya se usaba `jakarta.validation` suelto en otro punto). `FormularioRequestDTO` ganó `@NotNull` en `idDonante` y `@Valid` en la lista de `bienes` (para que las anotaciones de `BienResumenDTO` se evalúen en cascada). `BienResumenDTO` ganó `@Positive` en `cantidad` y `@NotBlank` en `tipoBien`. Y `DonacionController.crearDonacion` agregó `@Valid` al `@RequestBody`, que es lo que dispara la validación y, si falla, cae en el nuevo handler de `MethodArgumentNotValidException` (ver punto 20 de esta sección).

### Cómo se verificó

Revisado manualmente que la cadena `@Valid` está completa: controller → `FormularioRequestDTO` → `@Valid List<BienResumenDTO>` → anotaciones del DTO interno. Las validaciones de `Humana.edad` que también mencionaba este punto no se tocaron (siguen sin Bean Validation); las de `plazoEnDias`/`cantidadObjetivo` se resolvieron por otro lado (ver punto 17 de esta sección).

---

### 22. Punto 23 — La segmentación no incluía la unidad de medida y sumaba kilos con litros

**Estado:** corregido
**Severidad:** baja
**Archivo:** `models/entities/SegmentadorDonaciones/SegmentadorDonaciones.java`

### Qué pasaba

`generarClaveSegmentacion` armaba la clave con subcategoría, vencimiento y usado/nuevo, pero no con `unidadUtilizada`. Un formulario con "10 kilos de arroz" y "5 litros de aceite" de la misma subcategoría caía en el mismo grupo, y `Donacion.sumaCantidadBienes()` los sumaba como si fueran la misma unidad.

### Qué se hizo

La clave ahora incluye la unidad: `bien.getSubcategoria().getNombre() + "-" + (unidadUtilizada != null ? unidadUtilizada.name() : "SIN_UNIDAD")`. Con eso, dos bienes de la misma subcategoría pero distinta unidad caen en segmentos (y por lo tanto Donaciones) distintos.

### Cómo se verificó

Revisado manualmente que la clave de segmentación concatena la unidad antes de los demás componentes (vencimiento/usado), y que el caso `unidadUtilizada == null` no rompe la concatenación (cae en el literal `"SIN_UNIDAD"`).

---

### 23. Punto 24 — El PUT de donación dejaba los bienes anteriores huérfanos

**Estado:** mitigado (la causa de fondo sigue sin resolverse)
**Severidad:** baja
**Archivo:** `services/DonacionService.java`

### Qué pasaba

`Donacion.bienes` es un `@OneToMany` unidireccional sin `orphanRemoval`. `actualizarDonacion` reemplazaba la colección entera con `existente.setBienes(...)`: los bienes que quedaban afuera de la lista nueva no se borraban, solo se desasociaban (`donacion_id = NULL`) y quedaban vivos como filas huérfanas en la tabla `bien`. Encima, el PUT no validaba el estado de la donación: se podía reescribir el contenido de una donación ya `ENTREGADA`.

### Qué se hizo

De las dos opciones que proponía el punto (`orphanRemoval = true` en `Donacion.bienes`, o un diff explícito de la lista en el servicio), **ninguna de las dos se implementó**; el propio comentario agregado en el diff lo deja explícito ("los bienes que la lista deja afuera no se borran"). Lo que sí se hizo fue la otra mitad de la propuesta: `actualizarDonacion` ahora rechaza con `IllegalArgumentException` cualquier intento de modificar los bienes de una donación que no esté en estado `EN_DEPOSITO`. Esto no borra las filas huérfanas que ya puedan existir ni evita que se sigan generando al editar una donación que todavía está en depósito, pero acota el daño: ya no se puede desincronizar el conteo de una donación `ASIGNADA`/`ENTREGADA` editándole los bienes por este endpoint.

### Cómo se verificó

Revisado manualmente que la guarda de estado está antes de `resolverBienes`/`setBienes`, y que `Donacion.bienes` sigue sin `orphanRemoval` (no se tocó la entidad): el problema de filas huérfanas al editar una donación en depósito queda abierto.

---

### 24. Punto 25 — La importación CSV se tragaba los errores y no decía cuántos entraron

**Estado:** corregido
**Severidad:** baja
**Archivo:** `controllers/DonanteController.java`, `services/DonanteService.java`, `models/entities/lector/Lector.java`, `models/entities/lector/csv/LectorCSV.java`, `dto/personaDonante/ReporteImportacionDTO.java` (nuevo), `models/entities/lector/ResultadoLectura.java` (nuevo)

### Qué pasaba

`importarDonantes` corría dentro de un `CompletableFuture.runAsync` sobre el `ForkJoinPool.commonPool()`, con `catch (Exception ignored) {}` por cada donante y `catch (IOException ignored) {}` para el archivo completo. El endpoint devolvía siempre `202 "Importación en segundo plano iniciada."`, sin contador ni forma de saber después cuántos entraron o por qué fallaron los demás.

### Qué se hizo

`Lector<T>.importar` cambió su firma de `List<T>` a la nueva clase `ResultadoLectura<T>` (elementos, errores con número de línea, y total de filas), y `LectorCSV` acumula esos errores en vez de solo logearlos y perderlos. `DonanteService.importarDonantes` ahora genera un `UUID` de importación, lo guarda `EN_PROGRESO` en un `Map<UUID, ReporteImportacionDTO>` en memoria, y despacha el trabajo a un `ExecutorService` propio (`Executors.newFixedThreadPool(2)`) en vez del common pool. `DonanteController` expone `GET /personas/importar/{importId}` para consultar el reporte (`ReporteImportacionDTO`, nuevo), con el conteo de filas totales, exitosos, fallidos y hasta 50 mensajes de error.

### Cómo se verificó

Revisado manualmente la cadena completa: `LectorCSV.procesarYGuardarFila` agrega a `errores` en vez de solo logear; `ResultadoLectura` expone `getTotalFilas()/getErrores()/getElementos()`; `DonanteService.procesarImportacion` acumula también los fallos de `crearPersona` (no solo los de parseo CSV) en el mismo reporte. No se probó subiendo un CSV real contra el servicio levantado.

---

### 25. Punto 26 — El PUT de necesidad ignoraba el id de entidad y casteaba a ciegas

**Estado:** corregido
**Severidad:** baja
**Archivo:** `controllers/EntidadBeneficiariaController.java`, `services/EntidadBeneficiariaService.java`, `models/repositories/repos/RepositorioNecesidades.java`

### Qué pasaba

`EntidadBeneficiariaService.actualizarNecesidad` recibía el `id` de la entidad del path pero nunca lo usaba: `PUT /entidades/{A}/necesidades/{necesidadDeB}` modificaba la necesidad de B igual, con 200. Y `RepositorioNecesidades.modificarNecesidad` chequeaba el tipo del objeto **nuevo** (`datosNuevos instanceof NecesidadRecurrente`) pero casteaba el **existente**: si el PUT cambiaba el tipo de la necesidad, el cast explícito tiraba `ClassCastException` (500).

### Qué se hizo

`actualizarNecesidad` ahora recibe `(UUID idEntidad, UUID idNecesidad, NecesidadDTO dto)`. Busca la entidad por `idEntidad`, y si la necesidad no está en `entidad.buscarNecesidadPorId(idNecesidad)`, tira `IllegalArgumentException` (la necesidad no pertenece a esa entidad). El controller (`EntidadBeneficiariaController.actualizarNecesidad`) se actualizó para pasar los dos ids. En `RepositorioNecesidades.modificarNecesidad` se agregó una validación explícita: si `existente.getClass()` no coincide con `datosNuevos.getClass()`, se rechaza el cambio de tipo con `IllegalArgumentException` (400) antes de llegar al cast; y el cast que queda ahora chequea el tipo de `existente` (con pattern-matching `instanceof NecesidadRecurrente existenteRecurrente`), no el de `datosNuevos`.

### Cómo se verificó

Revisado manualmente que `actualizarNecesidad` valida pertenencia antes de llamar a `modificarNecesidad`, y que la nueva guarda de tipo en `RepositorioNecesidades` está antes del cast que antes podía tirar `ClassCastException`.

---

### 26. Punto 27 — La integración con logística va por broker, pero el contrato seguía dependiendo de DTOs duplicados a mano

**Estado:** mitigado (la propuesta principal, un `common-lib` compartido, quedó fuera de alcance)
**Severidad:** media
**Archivo (nuevo):** `src/test/java/ar/edu/utn/frba/ddsi/donaciones/dto/logistica/entrega/EntregaDTOContractTest.java`

### Qué pasaba

El contrato de integración con logística son DTOs copiados a mano en los dos módulos (`EntregaDTO`, `BienDTO`, `DireccionDTO`), sin nada que avise si uno de los dos lados renombra o agrega un campo: el mensaje se publica, llega al broker, y el consumidor falla con `Failed to convert message`/`MessageConversionException`, sin decir cuál de los dos lados se desalineó. La propuesta completa (un `common-lib` compartido) requiere reconectar al build un módulo que hoy está desconectado (ver punto 4 de este backlog), y quedó fuera de alcance de esta tanda.

### Qué se hizo

Se implementó la **alternativa barata** que la propia propuesta dejaba planteada: un test de contrato (`EntregaDTOContractTest`) que serializa un `EntregaDTO` armado con los DTOs reales de `donaciones-service` (`BienDTO`, `DireccionDTO`) y verifica, sobre el JSON resultante, que los campos que logística espera (`donacionResumen.idsDonaciones`, `donacionResumen.bienes[].cantidad`/`unidadDeMedida`, `entidadBeneficiaria.calleUno`/`ciudad`/`provincia`/`pais`) existen y no son `null`. No lee el módulo de logística (no hay forma, sin el `common-lib`): fija la forma que **este lado** publica, para que un rename o un campo borrado de `donaciones-service` falle en este test en vez de en el primer incidente de integración.

**Descubierto pero NO corregido (fuera de alcance):** al escribir este test se encontró que el `DireccionDTO` de `logisticas-service` (`logisticas-service/src/main/java/.../dto/entrega/DireccionDTO.java`) tiene un campo `idEntidad` (`UUID`, con constructor de 9 parámetros) sin equivalente en el `DireccionDTO` compartido de `donaciones-service` (`dto/DireccionDTO.java`, 8 campos, sin `idEntidad`). Hoy no rompe nada porque Jackson simplemente deja ese campo en `null` del lado de logística y nada lo exige todavía, pero es exactamente el tipo de desalineación silenciosa que describe este punto. No se modificó ningún archivo por esto: queda señalado para quien retome el punto 27 completo.

### Cómo se verificó

Revisado manualmente el JSON que produce `ObjectMapper.writeValueAsString(entrega)` contra los nombres de campo del lado de logística (inspeccionando los archivos de `logisticas-service` directamente, no ejecutando el otro servicio). El test en sí no se pudo correr (`mvn test`) en este entorno por no tener Maven instalado, así que sus aserciones no se ejecutaron, solo se revisaron por lectura.

---

### 27. Punto 28 — `BienDTO` mezclaba el mensaje de integración con el modelo de logística

**Estado:** corregido
**Severidad:** baja
**Archivo:** `dto/logistica/entrega/BienDTO.java`

### Qué pasaba

`BienDTO`, en el paquete `logistica.entrega` de `donaciones-service`, tenía siete campos: dos que `donaciones-service` efectivamente publica (`cantidad`, `unidadDeMedida`) y cinco del dominio de logística (`estado`, `fechaCambioEstado`, `fotoComprobante`, `entidadDestino`, `eventos`), completados solo por logística al procesar. Existía un constructor de 7 parámetros que `donaciones-service` nunca invocaba (siempre mandaba esos cinco en `null`), señal de que la clase servía para dos formas distintas.

### Qué se hizo

Se borraron los cinco campos de logística y el constructor de 7 parámetros (junto con los imports de `DireccionDTO`, `EventoLogisticaDTO` y `List` que solo existían para ese constructor). `BienDTO` quedó con los dos campos que `donaciones-service` realmente publica y un único constructor de 2 parámetros. El javadoc de la clase se reescribió para aclarar que logística mantiene su propio `BienDTO` con los campos adicionales, y que esta clase modela solo la mitad del mensaje que le corresponde a este servicio.

### Cómo se verificó

Revisado manualmente que no queda ningún caller en `donaciones-service` que invoque el constructor de 7 parámetros que se borró (el único uso de `BienDTO` en el flujo de integración construye el mensaje con cantidad/unidad). No se corrió el flujo de punta a punta contra logística.

---

### 28. Punto 29 — `POST /donaciones/formulario` devolvía 400 sin decir por qué

**Estado:** corregido
**Severidad:** media
**Archivo:** `exceptions/GlobalExceptionHandler.java`, `dto/personaDonante/FormularioRequestDTO.java`, `dto/donaciones/BienResumenDTO.java`, `controllers/DonacionController.java`

### Qué pasaba

`POST /donaciones/formulario` devolvía `400` con el cuerpo vacío y sin ninguna línea en el log del servidor. El propio punto señalaba como causa que `GlobalExceptionHandler` no logueaba nada en el handler de `IllegalArgumentException`, y que no había validación explícita del body con `@Valid`.

### Qué se hizo

No hubo un cambio dedicado a este endpoint por separado: quedó resuelto como consecuencia de los puntos 20 y 21 de esta sección, que tocan los mismos archivos. `GlobalExceptionHandler.manejarIllegalArgumentException` ahora hace `log.warn(..., ex)` antes de devolver el 400 (deja rastro en el log del servidor). El nuevo `@ExceptionHandler(MethodArgumentNotValidException.class)` devuelve en el cuerpo un mensaje por cada campo que falló la validación (`campo: mensaje`), en vez de un 400 mudo. Y `DonacionController.crearDonacion` junto con las anotaciones de `FormularioRequestDTO`/`BienResumenDTO` (punto 21 de esta sección) hacen que un body incompleto dispare esa validación en vez de llegar a una excepción más adentro sin diagnóstico.

### Cómo se verificó

Revisado manualmente que `manejarIllegalArgumentException` y el nuevo handler de `MethodArgumentNotValidException` logean antes de construir la respuesta, y que los dos devuelven el mensaje en el cuerpo (`ErrorResponseDTO`). La nota del planteo original de que "el `GlobalExceptionHandler` de este servicio está comentado entero" no corresponde al estado actual del archivo (está activo, con cinco `@ExceptionHandler`); probablemente describía un estado anterior del código. No se reprodujo el `POST` con un body incompleto contra un servicio levantado.
