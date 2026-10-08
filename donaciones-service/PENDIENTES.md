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

### 33. La importación CSV dejaba a los donantes sin medio de contacto predeterminado

**Estado:** corregido
**Severidad:** alta
**Corregido:** 2026-10-08 · sin commit
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/lector/csv/filaconverter/PersonaDonanteFilaConverter.java`

### Qué pasaba

El converter agregaba los medios de contacto que trae el CSV (mail, teléfono, whatsapp) pero
**nunca marcaba el predeterminado**. `ServicioNotificaciones` (línea 41) exige uno para
enviar, así que la notificación de registro (`REGISTRO_PERSONA`) tiraba
`IllegalArgumentException("No hay un medio de contacto predeterminado para enviar la
notificacion")` para **todas** las filas y el alta entera no se persistía. Se descubrió en
vivo: una carga real de 499 filas terminó 0 exitosos / 499 fallidos con ese mensaje (ya con
el punto 32 corregido, que era el que rompía antes).

El alta por HTTP no tenía el problema porque `PersonaDonanteDTO.resolverMedioPredeterminado`
(líneas 150-165) aplica el criterio: si hay medios y ninguno especificado, el primero es el
predeterminado.

### Qué se cambió

`PersonaDonanteFilaConverter.vincularMediosDeContacto` aplica el mismo criterio que el alta
HTTP: si quedaron medios y no hay predeterminado marcado, se setea el primero.

### Cómo se verificó

Test nuevo `CargaRealCsvTest.todosQuedanConMedioPredeterminado`: con la carga real de 499
filas, ningún donante con medios queda sin predeterminado (el RED fue la corrida real contra
docker: 499/499 fallidos con el mensaje exacto). Suite: donaciones 50/50, BUILD SUCCESS.

---

### 32. La importación CSV ignoraba las columnas si el mapeo no coincidía mayúscula por mayúscula

**Estado:** corregido
**Severidad:** media
**Corregido:** 2026-10-08 · sin commit
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/lector/csv/LectorCSV.java`,
`src/main/java/ar/edu/utn/frba/ddsi/donaciones/models/entities/lector/csv/filaconverter/PersonaDonanteFilaConverter.java`

### Qué pasaba

`LectorCSV` vinculaba los encabezados con `encabezado.trim()` y el
`PersonaDonanteFilaConverter` buscaba las columnas del mapeo **tal cual** llegaban del
request: sin normalizar mayúsculas de ninguno de los dos lados. Con un mapeo de
`"nombre completo"` contra un encabezado `"Nombre Completo"`, el `get()` fallaba, la fila
llegaba al converter con nombre y apellido `""`, y la Humana devolvía
`getNombreDeUsuario()` = `" "` → incentivos la rechazaba con
`400 "El donante requiere un nombre de usuario"`, una por fila: exactamente el error que
lluvioso del perfil durante una carga CSV real. Si además el `TipoPersona` no coincidía, la
fila entera se descartaba con un warning y el donante ni existía.

### Qué se cambió

Ambos lados usan ahora una clave canónica (`trim().toLowerCase()`): `LectorCSV` al armar el
mapa `encabezado → valor`, y el converter al buscar cada nombre de columna del mapeo. El
resto del comportamiento queda igual (el `EncabezadoCsvDuplicadoException` ahora también es
insensible al casing, que es el criterio nuevo).

**La causa real de la carga que disparó este punto** no era del código sino del
`mapeos` del request: la collection de Postman (`ciclo-completo`) mapeaba
`NOMBRE_RAZON_SOCIAL → ["Nombre","Apellido"]` y `TELEFONO → ["Telefono"]`, columnas que no
existen en el CSV real (`"Nombre/Razón Social"`, `"Teléfono"`, ambas con tilde o barra).
Con mapeos que nombran columnas inexistentes, las humanas nacen con nombre en blanco
(`nombreUsuario = " "`) y las jurídicas con `""` — que es exactamente lo que mostró el log
corregido de `IncentivosClient` (`(nombreUsuario=' ', role='DONANTE')`). La collection quedó
corregida a los encabezados reales del CSV; si el front comparte ese mapeo, hay que
corregirlo del mismo modo. La normalización del casing de este punto no puede compensar
columnas que no existen.

### Cómo se verificó

Test nuevo `ImportarCsvConMapeosTest` (3 tests): importa un CSV con encabezados
`"TipoPersona,Nombre Completo,Dni"` y mapeo `"tipopersona" / "nombre completo" / "dni"` (y
otra corrida con encabezados ya canónicos), y exige 1 donante con `nombreUsuario` completo.
**RED verificado** antes del arreglo: `expected: <1> but was: <0>` — la fila se descartaba
entera. Suite: donaciones 37/37, BUILD SUCCESS.

---

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
