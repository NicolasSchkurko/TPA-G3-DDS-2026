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
| 5  | 5     | El endpoint de vencer una donación manda un estado que el parser no conoce                        |
| 7  | 7     | Un bien sin `tipoBien` se convierte en `null` y revienta la segmentación                          |
| 8  | 8     | No hay una sola transacción en el módulo: las escrituras quedan a medias                          |
| 9  | 9     | Un fallo en una donación corta el lote de matchmaking entero                                      |
| 11 | 11    | Guardar el estado antes de notificar deja el cambio persistido y responde 404                     |
| 12 | 12    | `CascadeType.ALL` en las necesidades borra de más al dar de baja una entidad                      |
| 13 | 13    | El PUT de donante cambia el `@Id` y duplica la Persona                                            |
| 14 | 14    | Las propuestas se numeran desde 1 pero se leen desde 0                                            |
| 15 | 15    | El score de compatibilidad mide contra el histórico, no contra el período                         |
| 16 | 16    | La lista de formularios del donante es `@Transient`: la inactividad nunca se avisa                |
| 17 | 17    | El ranking de sub-atendidos lo monopoliza una sola entidad                                        |
| 18 | 18    | `cantidadObjetivo` sin validar: NullPointerException que corta el matchmaking                     |
| 19 | 19    | Borrar el medio de contacto predeterminado lo deja colgando                                       |
| 20 | 20    | Los eventos de logística no se aíslan: un id inválido rebuclea el mensaje para siempre            |
| 21 | 21    | Los controllers responden 404 ante cualquier `RuntimeException`                                   |
| 24 | 24    | El PUT de donación deja los bienes anteriores huérfanos                                           |
| 25 | 25    | La importación CSV se traga los errores y no dice cuántos entraron                                |
| 26 | 26    | El PUT de necesidad ignora el id de entidad y castea a ciegas                                     |
| 28 | 28    | `BienDTO` mezcla el mensaje de integración con el modelo de logóstica                             |
| 29 | 29    | `POST /donaciones/formulario` devuelve 400 sin decir por qué                                    |
| 34 | 34    | Tras donar con un formulario, el donante no se puede dar de baja (FK sin cascade)                  |
| 35 | 35    | Dar de baja un donante no borra siempre su perfil en incentivos (cascada frágil)                    |
| 36 | 36    | La nomenclatura de subcarpetas no coincide con la de incentivos                                      |
| 37 | 37    | Los repositorios usan dos convenciones distintas entre servicios                                     |
| 38 | 38    | Los listados devuelven la colección entera: falta paginación                                         |
| 39 | 39    | `DonacionController` inyecta `RabbitTemplate` sin usarlo                                             |
| 40 | 40    | La importación CSV no actualiza donantes ya cargados: los duplica                                     |
| 41 | 41    | La modificación de donante ignora campos y la baja deja la Persona huérfana                          |
| 42 | 42    | Faltan "Lista para entregar" y "Entrega fallida" en el enum de estados                               |
| 43 | 43    | "Consultar rankings" no está expuesto en donaciones: confirmar alcance                               |
| 44 | 44    | Los schedulers corren sin transacción: los jobs mueren con LazyInitializationException en silencio    |
| 45 | 45    | `EventosListener`: el catch no evita el requeue de escrituras y el cursor puede perder el evento       |
| 46 | 46    | `PUT /api/admins/{id}` inserta un administrador nuevo en cada modificación                            |
| 47 | 47    | `REINGRESO_DEPOSITO` deja la donación varada en `PENDIENTE_ASIGNACION` sin resultado de matchmaking    |
| 48 | 48    | `DELETE /personas/{id}/medios-contacto` no borra nada y responde 302                                  |
| 49 | 49    | El `RestTemplate` de integración no tiene timeouts: un servicio colgado bloquea todo                  |
| 50 | 50    | La publicación a logística va dentro de la transacción y a un exchange que este servicio no declara   |
| 51 | 51    | `EntidadBeneficiariaService` sin transacciones: altas y bajas de necesidades quedan a medias           |
| 52 | 52    | El alta de donante crea el perfil en incentivos y notifica antes de persistir localmente               |
| 53 | 53    | La asignación notifica dos veces: mensaje directo y estrategia de notificación                         |
| 54 | 54    | `DELETE /donaciones/{id}` responde 409 si la donación tiene resultado de matchmaking                   |
| 55 | 55    | El catch-all del `GlobalExceptionHandler` convierte errores de cliente en 500 y filtra internals       |
| 56 | 56    | Los PUT de persona, entidad y admin traducen validación a 404 mudo                                    |
| 57 | 57    | Los POST de medios de contacto aceptan nulos/vacíos y responden 201                                   |

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
if(p.getFormularios() !=null&&!p.

getFormularios().

isEmpty() &&p.

getFormularios().

getLast()...)
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
if(datosNuevos instanceof NecesidadRecurrente){
        ((NecesidadRecurrente)existente).

setPlazoEnDias(...);
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

## 28. `BienDTO` mezcla el mensaje de integración con el modelo de logística

**Estado:** abierto
**Severidad:** baja
**Archivo:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/dto/logistica/entrega/BienDTO.java`

### Qué pasa

`BienDTO` está en el paquete `logistica.entrega` de `donaciones-service` pero describe datos de logística:
`estado`, `fechaCambioEstado`, `fotoComprobante`, `eventos`. Son campos que logóstica llena al
procesar, no que `donaciones-service` mande.

Para el mensaje de integración se agregó un constructor de dos parámetros (`cantidad`,
`unidadDeMedida`) que deja los otros cinco en `null`. Funciona, pero es el síntoma de que la
misma clase está sirviendo para dos cosas con formas distintas.

Además, el archivo declara un `import` de `EventoLogisticaDTO` y un `List<EventoLogisticaDTO>` que
solo existen para el constructor de siete parámetros.

### Propuesta

Separar el DTO de transporte del DTO de dominio: uno con lo que `donaciones-service` publica (cantidad y
unidad) y otro con lo que logística devuelve (estado, foto, eventos). Si los dos tienen que
mantener el mismo nombre de campo para que Jackson los empareje, conviene que eso sea a
propósito y no accidente de que la clase resultante tenga todos los campos.

---

## 29. `POST /donaciones/formulario` devuelve 400 sin decir por qué

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/controllers/DonacionController.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/exceptions/`

### Qué pasa

`POST /api/donaciones/formulario` devuelve `400` con el cuerpo vacío. El log del servidor no
registra nada: no hay ninguna línea de error ni warning en el momento de la llamada.

Se comprobóError porque no hay diagnosticabilidad, no por la conexión: el mismo endpoint
funciona con un payload bien armado y falla con uno incompleto, y en los dos casos la
respuesta es idéntica desde el punto de vista de quien la consume.

### Por qué no se investigó más

Queda fuera del alcance de la revisión de conexiones entre servicios, que es lo que motivó
esta tanda. Se verificó que no es un problema de integración: los cuatro servicios levantan, se
comunican, y `POST /personas` —que también dispara notificaciones y llamadas a otros
servicios— responde `201` con persistencia real.

### Propuesta

1. Registrar la excepción en el punto donde se convierte a `400`. Con `logger.error` y la
   excepción completa, el próximo `400` dice la causa.
2. Devolver el mensaje en el cuerpo de la respuesta, como ya hace el resto de los controllers
   del módulo con `e.getMessage()`.
3. Agregar validación explícita del `FormularioRequestDTO` con `@Valid` y un
   `@RestControllerAdvice`, que es lo que convierte los errores de mapeo en un `400` con
   detalle en vez de un `400` mudo.

**Nota:** el `GlobalExceptionHandler` de este servicio está comentado entero, igual que el de
`notificaciones-service`. Es la causa de que el `400` no diga nada.

---

## 34. Tras donar por el formulario, el donante no se puede dar de baja (FK sin cascade)

**Estado:** abierto
**Severidad:** alta
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/Donaciones/Formulario/Formulario.java` (líneas 27-29), `src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/Donaciones/Donacion.java` (líneas 37-39), `src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/DonanteService.java` (líneas 132-136), `src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/repositories/repos/RepositorioDonantes.java` (línea 50), `src/main/java/ar/edu/utn/frba/ddsi/donaciones/exceptions/GlobalExceptionHandler.java` (línea 52)

### Qué pasa

`POST /api/donaciones/formulario` crea un `Formulario` con FK `donante_id` (`@ManyToOne` **sin cascade REMOVE**) y, al segmentar, una o más `Donacion` que **también** apuntan al donante por `donante_id` (mismo criterio, sin cascade REMOVE). A partir de esa donación, `DELETE /api/personas/{id}` recorre `DonanteController.eliminarDonante` → `DonanteService.eliminarPersona` → `RepositorioDonantes.eliminarPorId` → `jpaRepository.deleteById(id)`, y el `delete` **choca contra la FK**: la base tira violación de integridad y `GlobalExceptionHandler.manejarDataIntegrityViolation` la mapea a **409 CONFLICT**. El donante (y su Persona) queda vivo.

En la práctica: **cualquier donante que haya donado alguna vez no se puede dar de baja**, que es el caso normal (un donante existe justamente para donar). Se reproduce a mano: `POST /api/donaciones/formulario` con el id de un donante y después `DELETE /api/personas/{id}` responde 409.

### Por qué no se ve / workaround

El propio `DonacionController.eliminarFormulario` documenta la causa ("Formulario.donante_id es FK no nula sin cascade REMOVE: hay que borrar los formularios de un Donante antes de poder borrar al Donante (si no, 409)"), y se agregó `GET /api/donaciones/formularios` + `DELETE /api/donaciones/formularios/{id}` para liberar al donante. Pero es un **workaround manual**: el front no lo hace, así que desde la UI "borrar el perfil" simplemente no borra — recibe un 409 con un cuerpo de error de integridad, igual que cualquier otro conflicto.

### Propuesta

1. Que la baja del donante resuelva en **una sola transacción** lo que la referencia: borrar (o anonimizar) los `Formulario` y las `Donacion` del donante antes del `delete`, o pasar las FK a `ON DELETE CASCADE`/`@OnDelete` según qué deba sobrevivir (las donaciones ya entregadas probablemente no).
2. Decidir la política: si un donante con historial no debería borrarse físicamente, hacer **baja lógica** (soft delete) en vez de un `delete` que siempre va a chocar contra su propio historial.
3. Mientras tanto, que `DELETE /api/personas/{id}` haga el trabajo del lado del servidor (borrar en orden, o un `?forzar=true`), para que el front no dependa de dos llamadas en el orden correcto.

---

## 35. Dar de baja un donante no borra siempre su perfil en incentivos

**Estado:** abierto
**Severidad:** alta
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/DonanteService.java` (líneas 126-136), `src/main/java/ar/edu/utn/frba/ddsi/donaciones/clients/IncentivosClient.java` (líneas 105-121). Del otro lado: `incentivos-service/.../controllers/PerfilController.java` (`DELETE /api/perfiles/interno/{idUsuario}`) e `incentivos-service/.../services/PerfilService.java` (`eliminarPerfilPorBajaDeDonante`). Ver también el punto 39 de `incentivos-service/PENDIENTES.md`.

### Qué pasa

Al dar de baja un donante en donaciones, el perfil que ese donante tiene en la base de incentivos **no siempre desaparece**: queda huérfano. La baja en cascada existe desde el commit `7e9da04` ("Arreglo de Eliminar perfil", 2026-10-08): `DonanteService.eliminarPersona` avisa con `DELETE /api/perfiles/interno/{idUsuario}` antes del `delete` local. El problema no es que falte el aviso, sino que la cascada es **mejor esfuerzo y no es atómica ni confiable**.

### Por qué pasa (causas concretas)

1. **Orden y atomicidad.** La llamada a incentivos va **antes** del `delete` local y no comparten transacción. Si el `delete` local falla (punto 34), el request termina en 409 pero el aviso a incentivos ya se hizo: los dos lados quedan desincronizados (perfil borrado, donante vivo) y nada lo detecta. En cualquier otro punto de fallo queda al revés (donante borrado, perfil vivo).
2. **Sin reintentos ni outbox.** Si incentivos está caído o la URL quedó desalineada, la excepción **aborta la baja entera** y el huérfano queda; nada lo reintenta después.
3. **Idempotente pero silencioso.** `PerfilService.eliminarPerfilPorBajaDeDonante` hace `if (!repositorioPerfiles.existsById(idUsuario)) return;`: responde 200 sin borrar nada cuando el id no coincide con ningún perfil (perfil creado con otro id, datos viejos o de un seed). El llamador no puede distinguir "se borró" de "no había nada".
4. **Versión.** El endpoint `/api/perfiles/interno/{idUsuario}` y la llamada son del commit `7e9da04`. Cualquier despliegue anterior de uno de los dos servicios deja el alta y la baja desalineadas (incentivos sin el endpoint → 404 → excepción → donante vivo).

### Propuesta

1. Mover la baja al **canal de eventos** que ya usan los dos servicios: donaciones publica `donante.baja` en el exchange (mismo patrón que las notificaciones asíncronas) e incentivos lo consume de forma idempotente, con DLQ y reintentos. Así la baja no depende de que incentivos esté arriba en el momento.
2. Si se mantiene HTTP: envolverlo con reintentos/outbox y **no abortar** la baja local si incentivos falla —dejar el huérfano marcado para reconciliar— en vez de bloquear el borrado del donante.
3. Agregar una **reconciliación** (job o endpoint de admin) que borre perfiles cuyo `idUsuario` ya no exista en donaciones.
4. Confirmar que el build que se prueba tiene `7e9da04` en **los dos** servicios.

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

## 39. `DonacionController` inyecta `RabbitTemplate` sin usarlo

**Estado:** abierto
**Severidad:** baja
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/controllers/DonacionController.java`

### Qué pasa

El controller declara `private final RabbitTemplate rabbitTemplate` y lo recibe por constructor, pero no lo usa en ningún método: es una inyección muerta. Además deja la infraestructura de mensajería en la capa de controllers. En incentivos ningún controller conoce el broker: los controllers solo inyectan services, y las publicaciones pasan por services/clients.

### Propuesta

Quitar la dependencia del controller. Si en algún momento hace falta publicar desde un endpoint, que salga por el service correspondiente (o por `messaging/`), no por el controller.

---

## 40. La importación CSV no actualiza donantes ya cargados: los duplica

**Estado:** abierto
**Severidad:** alta
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/DonanteService.java` (`importarDonantes`, `persistirConNotificacion`), `src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/lector/csv/filaconverter/PersonaDonanteFilaConverter.java`, `src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/repositories/interfaces/DonanteJpaRepository.java` y `PersonaJpaRepository.java`

### Qué pasa

El enunciado pide importar "creando nuevos usuarios **o actualizando** la información de los ya existentes". Hoy el import solo crea: `PersonaDonanteFilaConverter` arma siempre un `Donante` nuevo, el `id` de `Persona` es un `UUID.randomUUID()` generado en el constructor, y `RepositorioDonantes.guardar` solo controla `existsById` —con un UUID nuevo nunca da true—. Una fila de alguien ya cargado **crea una `Persona` + `Donante` duplicados** y el import la cuenta como `exitoso`. Además se propaga a incentivos: `persistirConNotificacion` agrega el id nuevo a `perfilesPendientes` y crea un perfil duplicado allá. No hay finders por documento/CUIT/mail/usuario para detectar al existente.

### Propuesta

En `importarDonantes` (o en el converter), buscar al donante por documento/CUIT/mail/usuario y, si existe, actualizar en vez de crear, contándolo aparte en el reporte (p. ej. `actualizados`). Requiere finders nuevos en `DonanteJpaRepository`/`PersonaJpaRepository`.

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

## 43. "Consultar rankings" no está expuesto en donaciones: confirmar alcance

**Estado:** abierto
**Severidad:** baja
**Archivos:** controllers de donaciones (no hay endpoint)

### Qué pasa

El enunciado del servicio incluye, en la exposición REST, "permite consultar rankings". En donaciones no hay ningún endpoint de rankings: el ranking de donantes vive en `incentivos-service` (`GET /api/rankings/...`), y lo más cercano acá es `GET /api/donaciones/pendientes`, que devuelve las propuestas de matchmaking ordenadas (con posición y score) pero no un ranking.

### Propuesta

Definir si la consulta queda delegada en incentivos-service (dejarlo documentado) o si donaciones debería exponer un ranking propio (p. ej. el de sub-atendidos que ya calcula el matchmaking).

---

## 44. Los schedulers corren sin transacción: los jobs mueren con `LazyInitializationException` en silencio

**Estado:** abierto
**Severidad:** alta
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/sheduler/AsignacionScheduler.java` (líneas 35-41), `.../models/sheduler/ActividadDonanteScheduler.java` (líneas 15-18), `.../services/DonanteService.java` (líneas 241-247), `.../models/entities/AsignadorDonaciones/AsignadorDonaciones.java` (líneas 45-58), `.../models/entities/AsignadorDonaciones/AlgoritmosDeAsignacion/CompatibilidadSemantica.java` (líneas 25-29)

### Qué pasa

Un `grep` de `@Transactional` en `src/main` devuelve solo dos archivos: `DonacionService` y `EventosListener`. Los schedulers no están cubiertos, y cada llamada a una fachada abre y cierra su propia transacción de Spring Data: los objetos vuelven **detached**. En el hilo del scheduler no hay OSIV (eso es solo para requests), así que la primera colección LAZY que se toca revienta con `LazyInitializationException`.

- **Asignación** (`cron 0 0 18,0,2,4,6,8 * * *`): `buscarDonacionesSinAsignar()` + `obtenerTodas()` → `AsignadorDonaciones.ejecutarMatchmakingBatch` → `CompatibilidadSemantica.rankear` línea 26 (`entidad.getNecesidades()`). El `catch (Exception)` por donación que se agregó al corregir el punto 9 se traga la LIE y la loguea con `System.err`: el batch termina "sin errores" y **ningún resultado de matchmaking se guarda**. El único camino que funciona es `POST /donaciones/matchmaking/ejecutar`, porque `DonacionService.ejecutarMatchmakingADemanda` sí es `@Transactional`.
- **Inactividad** (`cron 0 0 0 * * ?`): `revisarActividades` línea 243 evalúa `p.getFormularios()` sobre un `Donante` detached → LIE. No hay try/catch: muere en el primer donante y el aviso de inactividad no se manda nunca.

### Por qué no se ve

Los dos jobs no dejan rastro visible: el de asignación escribe una línea por donación en `System.err` (que parece "un dato que se salteó") y el de inactividad muere sin log. El punto 16 figura como corregido (se sacó el `@Transient`), pero **en el hilo del scheduler la colección sigue sin poder inicializarse**.

### Propuesta

`@Transactional` en los métodos de entrada de cada corrida (o un bean intermedio transaccional que envuelva el batch completo, como ya hace el camino a demanda). Con eso el `EntityManager` queda abierto durante todo el procesamiento y las colecciones lazy se inicializan.

---

## 45. `EventosListener`: el catch no evita el requeue de escrituras y el cursor puede perder el evento

**Estado:** abierto
**Severidad:** alta
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/RabbitMQ/EventosListener.java` (líneas 37-81), `.../models/gestores/GestorEventosLogistica.java` (los tres manejadores), `src/main/resources/application.properties` (sin configuración de listener)

### Qué pasa

`recibirEvento` es `@Transactional` (se agregó para que las colecciones lazy no revienten) y adentro tiene un `try/catch (RuntimeException)` que loguea y descarta. Con una excepción que no toca la base (p. ej. el `UUID.fromString` inválido del punto 20) el aislamiento funciona. Pero con una excepción de una **escritura** el mecanismo se da vuelta:

1. `EventosListener.java` línea 52: el listener abre la transacción.
2. Una escritura de `GestorEventosLogistica` (`repositorioDonaciones.guardar`, líneas 106/130/150) falla dentro de esa transacción: el interceptor de Spring Data participa de la transacción existente y la marca **rollback-only**.
3. El `catch` de la línea 75 traga la excepción y el método retorna normal, pero el commit de fin de método tira `UnexpectedRollbackException` **fuera del try**.
4. Spring AMQP lo ve como fallo del listener y reencola el mensaje de inmediato (`defaultRequeueRejected` queda en `true`, no hay configuración de retry en `application.properties`): el hot loop que el comentario dice evitar sigue pasando, ahora para fallas de base.

Y hay un segundo camino de pérdida: si `procesarEvento` terminó y `ultimoIdProcesado` quedó actualizado en la línea 73, pero el commit falla después (flush diferido, constraint, DB caída), el mensaje se reencola; en la reentrega la línea 61 (`evento.getId() <= ultimoIdProcesado`) lo descarta y se ackea: **los cambios se revirtieron y el evento quedó perdido**, sin retry. El cursor, además, es un campo en memoria: con dos instancias no se comparte y se resetea al reiniciar.

### Por qué no se ve

El punto 20 y su nota de corregido afirman que el try/catch del listener impide que la cola quede trabada, y eso es cierto solo para los errores que no pasan por la base. Para el resto, el log muestra el mismo evento reintentando y el síntoma se confunde con "el broker está raro".

### Propuesta

Definir el contrato de error: si el evento es recuperable, dejar que la excepción salga (con retry/backoff configurado y sin `try/catch` adentro de la transacción); si no lo es, mandarlo a una dead-letter queue. Y decidir el cursor: persistirlo, coordinarlo, o sacarlo y basar la idempotencia en el estado de la donación.

---

## 46. `PUT /api/admins/{id}` inserta un administrador nuevo en cada modificación

**Estado:** abierto
**Severidad:** alta
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/AdminService.java` (líneas 53-75, en particular 60 y 69), `.../models/repositories/repos/RepositorioAdministradores.java` (líneas 46-52), `.../models/entities/administrador/Administrador.java` (línea 22), `.../dto/admin/AdminDTO.java` (líneas 25-31), `.../controllers/AdminController.java` (líneas 50-58)

### Qué pasa

Es exactamente el patrón del punto 13, que se arregló solo del lado de la Persona:

- `actualizarAdmin` construye `datosNuevos = dto.toDomain()`, y `AdminDTO.toDomain` hace `new Administrador(...)`; el campo `@Id` de `Administrador` es `private UUID id = UUID.randomUUID()`, así que `datosNuevos` **siempre tiene un UUID nuevo** (el id del DTO no se lee).
- Se mutan los campos de `existente` (líneas 64-66) pero **nunca se guarda `existente`**: se llama `repositorioAdministradores.actualizar(id, datosNuevos)` (línea 69), que con `existsById(id)` verdadero hace `jpaRepository.save(datosNuevos)` → `merge()` sin fila con ese id → **INSERT** (más una `Humana` y un medio nuevos por cascade).
- La respuesta devuelve `AdminDTO.from(existente)`, o sea el id viejo: 200 mintiendo.

### Cómo se dispara

`PUT /api/admins/{idExistente}` con cualquier body válido, una o más veces: cada llamada agrega otra fila y el admin original queda con los datos viejos.

### Propuesta

Persistir `existente` (que es el que tiene el id del path) en lugar de `datosNuevos`, o setearle el id del path a `datosNuevos` antes de guardar. Sumar un test de "dos PUT seguidos no duplican".

---

## 47. `REINGRESO_DEPOSITO` deja la donación varada en `PENDIENTE_ASIGNACION` sin resultado de matchmaking

**Estado:** abierto
**Severidad:** alta
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/gestores/GestorEventosLogistica.java` (líneas 143-151, la sentencia en 149), `.../models/repositories/repos/RepositorioDonaciones.java` (líneas 34-40), `.../services/DonacionService.java` (líneas 262-268), `.../models/gestores/GestorMatchmaking.java` (líneas 22-27), `.../models/sheduler/AsignacionScheduler.java` (línea 38)

### Qué pasa

Cuando logística reporta `ENTREGA_FALLIDA`, la donación vuelve a `EN_DEPOSITO` y el scheduler la puede volver a matchear. Pero cuando el ítem reingresa físicamente al depósito, logística publica `REINGRESO_DEPOSITO` y `manejarReingresoDeposito` la deja en **`PENDIENTE_ASIGNACION`**, un estado que significa "hay un resultado de matchmaking para aprobar".

Y no hay ningún resultado: `DonacionService.asignarPropuesta` borró el `ResultadoMatchmaking` al asignar (línea 175). Entonces:

- `GET /donaciones/pendientes` no la lista (no hay resultado que mostrar).
- `POST /donaciones/asignar` responde `IllegalArgumentException("No hay resultado de matchmaking...")`.
- El scheduler solo recoge `EN_DEPOSITO` (`buscarDonacionesSinAsignar`); `buscarDonacionesPendientesDeAsignar()` existe pero **no la usa nadie**.

La donación queda varada indefinidamente salvo un `PATCH /donaciones/{id}/estado` manual a `EN_DEPOSITO`.

### Cómo se dispara

Flujo normal post-entrega fallida: `ENTREGA_FALLIDA` (vuelve a `EN_DEPOSITO`) y después `REINGRESO_DEPOSITO` desde logística.

### Propuesta

Mandar `REINGRESO_DEPOSITO` a `EN_DEPOSITO` (igual que `ENTREGA_FALLIDA`) o volver a generar el `ResultadoMatchmaking`. Si se decide usar `PENDIENTE_ASIGNACION`, que el scheduler también recoja donaciones en ese estado **sin** resultado y las re-matchet.

---

## 48. `DELETE /personas/{id}/medios-contacto` no borra nada y responde 302

**Estado:** abierto
**Severidad:** alta
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/controllers/DonanteController.java` (líneas 146-155, la respuesta en 151), `.../dto/notificaciones/MediosContactoDTO.java` (líneas 24-32), `.../services/DonanteService.java` (líneas 234-239), `.../models/repositories/repos/RepositorioPersonas.java` (líneas 113-126), `.../models/entities/Mensaje/MedioDeContacto/MediosDeContacto.java` (líneas 50-63)

### Qué pasa

Dos defectos en el mismo endpoint:

1. **No borra.** El body se convierte en una instancia **nueva** (`MediosContactoDTO.toDomain()` no lleva id) y `MediosDeContacto.eliminarMedioDeContacto` compara con `remove(...)`/`equals(...)`. `MedioDeContacto`, `Mail`, `Telefono` y `Whatsapp` **no sobrescriben `equals`/`hashCode`** (el único `equals` del módulo está en `PropuestaAsignacion`), así que la comparación es por identidad y siempre da falso: la lista queda intacta y `actualizar(...)` guarda sin cambios. La guarda de promoción del predeterminado que se agregó al corregir el punto 19 **tampoco aplica por este camino**, por la misma razón.
2. **Responde 302.** El controller devuelve `new ResponseEntity<>(actualizada, HttpStatus.FOUND)`: un `302` sin `Location` en un DELETE exitoso. Los clientes que solo aceptan 2xx (fetch, Angular) lo tratan como redirección/error.

### Por qué no se ve

La respuesta trae el donante serializado y parece "el donante actualizado"; el medio sigue en la lista y el próximo envío de notificación lo sigue usando.

### Propuesta

Comparar por contenido (implementar `equals` por tipo+valor) o buscar el medio por índice/id antes de remover, y devolver `200`/`204` en lugar de `302`.

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

## Corregidos

Un bullet por arreglo; el número es el ID estable del punto original («sin ID» = arreglos de integración que no tenían punto propio). Los residuales que siguen abiertos quedan anotados en el bullet.

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
- **Clientes (sin ID).** Apuntaban a la raíz de los servicios — rutas reales (`POST /api/perfiles`, `PATCH /api/perfiles/donacion/{id}`) y notificaciones migradas a Rabbit.
- **Clientes (sin ID).** Se tragaban las excepciones de salida sin log — ahora loguean y relanzan.
- **Rabbit (sin ID).** Dos bindings ataban al exchange equivocado y usaban comodines `.#` — cada servicio declara lo suyo con la clave exacta.
- **Properties (sin ID).** URLs default sin context-path y faltaba el bloque `spring.rabbitmq.*` — parametrizados por entorno.
