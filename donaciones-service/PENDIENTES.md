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

## Corregidos

### 31. Logística registra un solo bien por donación: los bienes 2..N caen como "repetidos"

**Estado:** corregido
**Severidad:** media
**Corregido:** 2026-10-07 · sin commit
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/DonacionService.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/SegmentadorDonaciones/SegmentadorDonaciones.java` (junto con el punto 23)

### Qué pasaba

`ItemEntrega` (logística) tiene el `@Id` en `id_donacion`: un item por donación, con un solo
par `cantidad`+`unidad`, y `EntregaService.itemsEnUnaTransaccion` hace `existsById(idDonacion)`
antes de guardar cada par id-bien. Los bienes 2..N de la misma donación caían como "repetidos"
y se perdían. Enviar N ids tampoco sirve: el segundo item colisiona con el primero. La
decisión de granularidad que propone este punto: **el item es la cantidad agregada de la
donación en una unidad**, no un bien físico.

### Qué se cambió

1. `publicarEntregaALogistica` suma las cantidades **por unidad de medida**
   (`LinkedHashMap` + `merge` con `Integer::sum`, peso null → 0, igual que `BienResumenDTO`)
   y manda una entrada por unidad con el id de donación repetido — el receptor exige
   `bienes.size() == idsDonaciones.size()` (punto 27) y así los tamaños siempre alinean. Con
   un segmento homogéneo, logística registra **un** item con el total real: nada se pierde,
   y la idempotencia por re-delivery queda intacta.
2. **Punto 23 (opción A de su propia propuesta):** `generarClaveSegmentacion` incluye la
   `unidadUtilizada` (null-safe), así que los segmentos nuevos son de una sola unidad y la
   agregación siempre es sumable.
3. El modelo receptor no se tocó (hay trabajo en curso en esos archivos de logística).
4. Residual: los segmentos **legacy** con unidades mezcladas (creados antes de este arreglo)
   mandan dos entradas y logística registra solo la primera. Es data anterior al arreglo.

### Cómo se verificó

`ContratoLogisticaTest` ahora corre `asignarPropuesta` con 2 bienes (10 kg + 6 kg) y exige
ids `[idDonacion]` + un único bien con `cantidad: 16`, `KILOGRAMOS`. **RED verificado**
revirtiendo temporalmente el agregado: el mensaje salía `[id,id]` con 2 bienes — el
mecanismo exacto del descarte por dedupe. Suite: donaciones 33/33, BUILD SUCCESS.

---

### 23. La segmentación no incluye la unidad de medida y suma kilos con litros

**Estado:** corregido
**Severidad:** baja
**Corregido:** 2026-10-07 · sin commit (opción A de la propia propuesta, como parte del punto 31)
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/SegmentadorDonaciones/SegmentadorDonaciones.java`

### Qué pasaba

`generarClaveSegmentacion` armaba la clave del segmento con subcategoría, fecha de vencimiento
(si era perecedero) y usado/nuevo, pero **no entraba la unidad**: "10 kilos de arroz" y "5
litros de aceite" de la misma subcategoría caían en la misma donación y
`Donacion.sumaCantidadBienes()` los sumaba como 15, que es lo que consumen
`CompatibilidadSemantica.calcularScore` y `Necesidad.cantidadRecibida()`.

### Qué se cambió

`generarClaveSegmentacion` incluye `bien.getUnidadUtilizada()` en la clave (con guarda de
null, para los bienes que hoy llegan sin unidad): cada donación segmentada es de una sola
unidad. Es la opción A que el propio punto proponía, y es la que hace posible el agregado por
unidad del punto 31. No se migró a "convertir todo a peso" (la decisión del
`UnidadDeMedida.toString` de "que siempre lo pese" sigue en pie como decisión más grande).

### Cómo se verificó

Test nuevo `SegmentadorDonacionesTest.segmentar_BienesDeDistintaUnidad_NoSeSuman`
(kilo vs litro → 2 donaciones). **RED verificado** sin la unidad en la clave: devolvía 1
donación (`expected: <2> but was: <1>`). Suite: donaciones 33/33, BUILD SUCCESS.

---

### 27. La integración con logística ya va por broker, pero el contrato depende de DTOs duplicados a mano

**Estado:** corregido (el residual que registró quedó resuelto junto con los puntos 31 y 23)
**Severidad:** media
**Corregido:** 2026-10-07 · sin commit
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/dto/DireccionDTO.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/services/DonacionService.java`

### Qué pasaba

El contrato de entrega son DTOs copiados a mano en cada módulo, y la afirmación de este punto
("los nombres de los campos coinciden hoy") quedó desactualizada. La revisión del receptor
(`EntregaService.procesarPeticion` y `resolverEntidad` de logisticas-service) mostró dos
desincronizaciones reales:

1. **Faltaba `idEntidad`.** El emisor armaba `DireccionDTO.from(entidad.getDireccion())` sin
   id, y el receptor lo exige: `resolverEntidad` hace `findById(dto.getIdEntidad())`
   (`EntregaService:153-155`); con null tira IAE y el `DonacionListener` descarta el mensaje.
   El 100% de las entregas se descartaba.
2. **Tamaños incompatibles.** El emisor mandaba `List.of(donacion.getId())` con N bienes y el
   receptor exige `bienes.size() == idsDonaciones.size()` (`EntregaService:119-122`): con
   N > 1 bienes el mensaje se descartaba entero. El diseño documentado en el propio
   `ProductorLogistica` ("el mensaje lleva un id de donación por bien") es el que usa el
   receptor; el que no lo cumplía era `publicarEntregaALogistica`.

### Qué se cambió

1. `DireccionDTO` ahora declara `idEntidad` (UUID), y `publicarEntregaALogistica` lo setea con
   `entidad.getId()`, que es la clave que logística guarda como `Entidad.idEntidadBeneficiaria`.
2. El mensaje lleva **un id de donación por bien** (los tamaños siempre coinciden; la clave de
   partición sigue siendo el menor de los ids, como documenta `ProductorLogistica`).
3. El lado receptor no se tocó: ya esperaba exactamente esta forma del mensaje.
4. El residual del modelo receptor (registra un solo bien por donación) que esta revisión
   descubrió quedó resuelto después por los puntos 31 y 23 (agregado por unidad).

### Cómo se verificó

Test de contrato nuevo `ContratoLogisticaTest`: corre `asignarPropuesta` completo con el
service real (deps mockeadas en los bordes), captura el `EntregaDTO` que entrega al
`ProductorLogistica`, lo serializa como lo hace el `Jackson2JsonMessageConverter`
(`ObjectMapper` default, estricto con campos desconocidos — si un lado renombra un campo, el
test lo ve) y lo deserializa y valida contra mirrors campo a campo de los cuatro DTOs
receptores, incluidos los campos que viajan en null. Fallaba antes del arreglo
(`idEntidad: must not be null` — el descarte garantizado) y pasa después, incluido el caso
multi-bien (2 bienes, ids alineados). Suite completa: donaciones 32/32, BUILD SUCCESS.

---

### 22. No hay Bean Validation: entran cantidades negativas como `Bien.peso`

**Estado:** corregido
**Severidad:** baja
**Corregido:** 2026-10-07 · sin commit
**Archivos:** `pom.xml`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/dto/personaDonante/FormularioRequestDTO.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/dto/donaciones/BienResumenDTO.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/dto/donaciones/DonacionDTO.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/controllers/DonacionController.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/exceptions/GlobalExceptionHandler.java`

### Qué pasaba

Los DTOs de entrada eran cajas de Lombok sin una sola restricción y el pom ni siquiera
declaraba `spring-boot-starter-validation` (desde Boot 2.3 no viene con starter-web), así que
cualquier anotación se habría ignorado. `POST /donaciones/formulario` con
`cantidad: -50` guardaba `Bien.peso = -50`, que arrastra `sumaCantidadBienes()`, los scores de
`CompatibilidadSemantica` y los conteos de las necesidades; un bien sin `tipoBien` producía un
`null` que llegaba a la segmentación.

### Qué se cambió

1. `pom.xml`: se agrega `spring-boot-starter-validation`.
2. `FormularioRequestDTO`: `@NotNull` en `idDonante` y `fechaRealizacion` (esta última además
   cierra el hueco que quedó del punto 30: la fecha que va a incentivos), y `@Valid` en
   `bienes` para que cada `BienResumenDTO` de la lista también se valide.
3. `BienResumenDTO`: `@NotNull @Positive` en `cantidad`, `@NotBlank` en `tipoBien`.
4. `DonacionDTO`: `@Valid` en `bienes` (cascada para el `PUT /donaciones/{id}`).
5. `DonacionController`: `@Valid` en los `@RequestBody` de `crearDonacion` y
   `actualizarDonacion`.
6. `GlobalExceptionHandler`: handler nuevo para `MethodArgumentNotValidException` → 400 con el
   detalle de campos (`ErrorResponseDTO`, mismo patrón que el resto) y `log.warn`. Sin él, el
   catch-all `Exception → 500` existente habría convertido la falla de validación en un 500.

**Queda pendiente:** la validación de `Humana.edad` (`PersonaDonanteDTO`), que este punto
mencionaba pero no tenía en su lista de archivos. Sigue en abierto.

### Cómo se verificó

Test nuevo `FormularioRequestValidacionTest` (MockMvc standalone contra el controller real,
con el advice `GlobalExceptionHandler` registrado y el service mockeado):

- Cantidad `-50` → 400 con `{"mensaje": "...cantidad...", "codigoEstado": 400}` y el service
  **nunca invocado**.
- Bien sin `tipoBien` → 400, service nunca invocado.
- Formulario sin `fechaRealizacion` → 400, service nunca invocado.
- Formulario válido → llega al service (protege contra sobre-bloqueo).

Los tres casos inválidos respondían 200 antes del arreglo. Suite completa: donaciones 31/31,
notificaciones 15/15, BUILD SUCCESS.

---

### 6. Una estrategia de notificación no es bean: toda entrega fallida revienta

**Estado:** corregido
**Severidad:** crítica
**Corregido:** 2026-10-07 · sin commit (el `@Component` ya estaba en el código; el pendiente estaba desactualizado)
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/ServicioMensaje/EstrategiasMensajes/NotificacionEntregaFallida.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/ServicioMensaje/FabricaEstrategiasNotificacion.java`

### Qué pasaba

`NotificacionEntregaFallida` era la única de las seis estrategias sin `@Component`, así que
nunca entraba al mapa de `FabricaEstrategiasNotificacion` (armado con
`List<EstrategiaNotificacion>`) y toda entrega fallida revientaba con
`IllegalArgumentException("No existe una estrategia para ENTREGA_NO_RECIBIDA")`.

### Qué se cambió

Nada en el código: al verificar el pendiente (Fase 1 del `bug-fixer`), la clase ya tiene
`@Component` (`NotificacionEntregaFallida.java:11`) y por lo tanto la fábrica la registra vía
la lista inyectada. El pendiente quedó desactualizado (probablemente corregido en una tanda
anterior sin actualizar este archivo). Lo que **no** está implementado de la propuesta
original es la defensa: que la fábrica falle al arrancar si algún `TipoEventoNotificacion` del
enum no tiene estrategia registrada; hoy sigue fallando en runtime si se agrega un valor al
enum sin su estrategia.

### Cómo se verificó

Lectura del código actual: `@Component` presente en la línea 11 y `FabricaEstrategiasNotificacion`
inyectando `List<EstrategiaNotificacion>` (líneas 21-28), con lo que la estrategia entra al
mapa y `ENTREGA_NO_RECIBIDA` existe.

---

### 10. `fechaEntrega` nunca se persiste, y dos funcionalidades dependen de ella

**Estado:** corregido
**Severidad:** alta
**Corregido:** 2026-10-07 · sin commit
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/SegmentadorDonaciones/SegmentadorDonaciones.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/Donaciones/Formulario/DonacionFacade.java`

### Qué pasaba

Nadie escribía nunca `Donacion.fechaEntrega`: `SegmentadorDonaciones.crearDonacion` le pasaba
`null` explícitamente (línea 66) y `DonacionDTO.toDomain()` tampoco lo seteaba. La columna
quedaba siempre en `NULL`, y dos lógicas la usaban como si fuera dato real:

- **`SubAtendidos.cantidadDonacionesUltimoTrimestre`** (líneas 57-63) filtra por
  `d.getFechaEntrega() != null`: el count era **siempre 0**.
- **`NecesidadRecurrente.cantidadRecibidaEnPeriodo`** (líneas 31-40) filtra por
  `getFechaEntrega() != null && ...isAfter(fechaLimite)`: **siempre devolvía 0**.

### Qué se cambió

La fecha de realización del formulario —que `DonacionService.procesarFormulario` ya recibía
del request y guardaba en `Formulario.fechaRealizacion`— es ahora la `fechaEntrega` de cada
donación segmentada: `DonacionFacade.crearDonaciones` la pasa a
`SegmentadorDonaciones.segmentar(donante, bienes, fecha)` y `crearDonacion` ya no manda
`null`. Se corrigió junto con el punto 30, que necesitaba el valor para cumplir el `@NotNull`
de incentivos. Si el request no trae `fechaRealizacion`, el valor sigue siendo `null`: cerrar
esa entrada es territorio del punto 22 (Bean Validation).

### Cómo se verificó

`ContratoIncentivosTest.fechaRealizacionDelFormulario_quedaEnCadaDonacionSegmentada` fallaba
antes del arreglo (`expected: <2026-10-01> but was: <null>`) y pasa después. Los tres
consumidores del campo (`SubAtendidos`, `NecesidadRecurrente` y el reporte a incentivos) ahora
reciben un valor real. Suite completa: donaciones 27/27, BUILD SUCCESS.

---

### 30. El payload de la donación no cumple el contrato de incentivos: toda asignación responde 400

**Estado:** corregido
**Severidad:** crítica
**Corregido:** 2026-10-07 · sin commit
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/dto/incentivos/IncentivosDonacionDTO.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/gestores/GestorAsignaciones.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/SegmentadorDonaciones/SegmentadorDonaciones.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/Donaciones/Formulario/DonacionFacade.java`

### Qué pasaba

El payload del reporte de asignación no cumplía el contrato que incentivos exige
(`ImpactoDonacionDTO`, recibido con `@Valid @RequestBody` en `PerfilController:163-167`):

| | Este servicio manda | Incentivos exige |
|---|---|---|
| `idDonacion` | **no existía el campo** en `IncentivosDonacionDTO` | `@NotNull UUID` — es la clave de idempotencia del otro lado |
| `fechaEntrega` | `LocalDate` (`"2026-10-07"`) | `@NotNull LocalDateTime` |

Y `fechaEntrega` además siempre salía `null`, porque nadie la persistía (ver punto 10 de esta
lista). El `IncentivosClient` relanza la excepción y `procesarAccionesPostCambioEstado` no la
captura, así que la asignación entera respondía 500 y `publicarEntregaALogistica` nunca se
ejecutaba: logística no se enteraba de la entrega por un problema con incentivos.

### Qué se cambió

1. `IncentivosDonacionDTO` ahora declara `idDonacion` (UUID) y `fechaEntrega` como
   `LocalDateTime`.
2. `GestorAsignaciones.procesarAccionesPostCambioEstado` setea
   `dto.setIdDonacion(donacion.getId())` y convierte la fecha con
   `LocalDate.atStartOfDay()`, con guarda de `null` (si no hay fecha, incentivos responde 400
   explícito, que es lo que el receptor pide).
3. La causa raíz de la fecha nula (punto 10, corregido en la misma tanda):
   `DonacionFacade.crearDonaciones` pasa `formulario.getFechaRealizacion()` a
   `SegmentadorDonaciones.segmentar(donante, bienes, fecha)`, y `crearDonacion` ya no manda
   `null`. `Donacion.fechaEntrega` sigue siendo `LocalDate`; la conversión a `LocalDateTime`
   ocurre sólo en la frontera del DTO.
4. El lado de incentivos no se tocó: el payload ahora cumple exactamente su contrato.

### Cómo se verificó

Test de contrato nuevo `ContratoIncentivosTest` (2 tests), en `src/test/.../incentivos/`:

- `payloadDeAsignacion_cumpleContratoDeIncentivos`: captura el DTO que
  `cambiarEstado(..., "ASIGNADO", ...)` le pasa al `IncentivosClient`, lo serializa con
  Jackson (misma config default de Spring Boot: `JavaTimeModule`, fechas ISO) y lo
  deserializa y valida contra un mirror del `ImpactoDonacionDTO` receptor con las mismas
  anotaciones (`@NotNull`/`@NotBlank`/`@PositiveOrZero`).
- `fechaRealizacionDelFormulario_quedaEnCadaDonacionSegmentada`: la fecha del formulario
  queda como `fechaEntrega` de cada donación segmentada.

Los dos fallaban antes del arreglo —uno con `expected: <2026-10-01> but was: <null>` y el
otro con `Cannot deserialize value of type java.time.LocalDateTime from String "2026-10-07"`—
y pasan después. Suite completa: donaciones 27/27, notificaciones (upstream) 13/13,
BUILD SUCCESS.

---

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

- Este módulo declara `logistica.integracion.hash`, `logistica.eventos.exchange` y
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
