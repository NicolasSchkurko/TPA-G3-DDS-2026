# Pendientes técnicos de `logisticas-service`

Registro de problemas conocidos del servicio, con el motivo y la propuesta de arreglo para
que no se pierdan de vista al crecer el código.

**Están ordenados de más urgente a menos urgente**, no por número de punto. El número es un
ID estable y no se renumera nunca, así que quedan huecos. Un punto corregido se borra de esta
lista y pasa a la sección [Corregidos](#corregidos) del final.

El orden no es el de la severidad declarada en cada punto sino el del daño real: cuánto se
rompe cuando pasa, y qué tan fácil es que pase.

| # | Punto | Por qué está acá |
|---|---|---|
| 1 | 1 | No tiene ni un test: nada de lo que hay adentro está verificado |
| 2 | 2 | Iniciar o terminar una ruta nunca persiste el estado: la operación responde OK y no pasa nada |
| 3 | 3 | Terminar una ruta borra un ítem y después tira excepción: corta el recorrido a mitad de camino |
| 4 | 4 | La condición del camión está invertida: el caso normal responde "Camión no encontrado" |
| 5 | 5 | Los tres DELETE devuelven 404 después de borrar bien, y con razón equivocada |
| 6 | 6 | La relación ítem-evento apunta al id equivocado: la trazabilidad no se persiste |
| 7 | 7 | El getter de Parada ignora su propio campo y lee el primer ítem: revienta con la lista vacía |
| 8 | 8 | Confirmar una entrega que no está en camino se ignora en silencio y responde 200 |
| 9 | 9 | Reportar una entrega fallida no valida el estado previo: una entrega_ok se puede revertir |
| 10 | 10 | El reingreso a depósito no valida nada y su comentario cita un método que no existe |
| 11 | 11 | El mismo evento se mete en la lista de todos los ítems con `orphanRemoval` |
| 12 | 12 | El polling de eventos reenvía el último evento y explota si `desdeId` viene null |
| 13 | 13 | `findByIdGreaterThanOrderByIdAsc` no ordena: el nombre promete algo que el código no hace |
| 14 | 14 | `GET /entregas` y `GET /entregas/{id}` devuelven la entidad cruda: recursión infinita de Jackson |
| 15 | 15 | Registrar una donación con N bienes duplica país, provincia, ciudad y dirección N veces |
| 16 | 16 | `procesarPeticion` no valida el payload: NPE e IndexOutOfBounds con requests incompletos |
| 17 | 17 | El planificador resetea la carga de los camiones y nunca la persiste |
| 18 | 18 | `return null` en el catch del planificador manual: el error se pierde |
| 19 | 19 | La validación de la justificación de una entrega fallida está invertida |
| 20 | 20 | Verificado: logóstica no invoca a MetaDonacion ni incentivos ni habla con notificaciones |
---

## 1. El módulo no tiene un solo test

**Estado:** abierto
**Severidad:** alta
**Archivos:** todo `logisticas-service/src/main`

### Qué pasa

`logisticas-service/src/test` no existe. Es el único módulo del proyecto sin cobertura, y es
justamente el que recibió un merge sin resolver: el compilador era la única red de seguridad y
esa red no avisó.

No es una observación estética. Los bugs que se listan abajo son de la clase que un test
atrapa con dos líneas y que se descubren tarde: un JPQL mal formado, un método que se borra y
deja la llamada colgando, imports que apuntan al paquete viejo.

### Qué había que haber atrapado

1. `GestorPublicacionEventos.java` se commiteó con marcadores de conflicto de merge
   (`<<<<<<< HEAD`, `=======`, `>>>>>>>`). El módulo entero no compilaba.
2. `RepositorioBienes.buscarPorId` se borró dejando la llamada colgando (esto fue en
   `donaciones-service`, mismo patrón).
3. Un `@Query` sin `FROM` hacía que el bean del repositorio no se pudiera crear y el servicio
   no arrancara, con los tests en verde.

Los tres son fallos que un `mvn compile` o un test de arranque los muestran en segundos.

### Propuesta

Un `@SpringBootTest` que levante el contexto ya cubre el punto 1 y el 3: el contexto no
arranca si un JPQL está mal o si una entidad referencia a otra que no está en la unidad de
persistencia. Cuesta un test y es la red que falta.

Después, tests sobre `GestorPublicacionEventos` y sobre los `default` de los repositorios,
que son la lógica que más se toca.

---

## 2. Iniciar o terminar una ruta nunca persiste el estado

**Estado:** abierto
**Severidad:** alta
**Archivos:** `models/repositories/rutas/RepositorioRutas.java:29`, `services/RutaService.java:86`, `services/RutaService.java:97`

### Qué pasa

`RepositorioRutas.actualizarEstado` no actualiza nada:

```java
default void actualizarEstado(Ruta ruta, EstadoRuta nuevoEstado){
    int posicion = this.findAll().indexOf(ruta);
    if (posicion != -1) {
        ruta.setEstado(nuevoEstado);
        this.findAll().set(posicion, ruta);
    }
}
```

Son tres fallas encimadas en tres líneas:

1. `this.findAll()` devuelve una `List` **nueva** en cada llamada. El `indexOf` mide contra una
   lista y el `set` escribe sobre otra distinta. El `set` no toca la lista que se indexó, así
   que aunque el `if` entre, el efecto es tirar la lista a la basura.
2. `indexOf` usa `equals`, y `Ruta` no overridea `equals`, así que compara identidad. La
   `ruta` que le pasa `RutaService` viene de `findByChofer`, que a su vez la saca de **otro**
   `findAll()`. Son instancias distintas, `indexOf` devuelve `-1` y el `if` nunca entra.
3. No hay `save()`. La lista devuelta por `findAll()` de Spring Data es de entidades
   **detached**: la transacción read-only que abrió el repositorio ya se cerró cuando el
   método devolvió. Fuera de sesión, `ruta.setEstado(nuevoEstado)` muta un objeto Java que
   JDBC nunca ve.

El resultado en `RutaService.iniciarRuta:86` y `terminarRuta:97`: el endpoint devuelve 200 con
"Ruta iniciada correctamente", se publica el evento `INICIO_RUTA`, pero la fila de `ruta`
sigue con `estado = 'PROGRAMADA'` para siempre. Tampoco hay ningún `@Transactional` en ningún
servicio del módulo, así que la lectura-modificación-escritura ocurre en transacciones
separadas por cada llamada a repositorio.

Lo mismo invalida `urlSeguimiento`: `GestorPublicacionEventos.publicarInicioRuta:44` lo setea
sobre la misma entidad detached y nunca se guarda.

### Propuesta

Borrar el `default` y dejar que el servicio haga `repoRutas.saveAndFlush(ruta)` sobre la
entidad que ya tiene en mano, o `save(ruta.getIdRuta(), ruta)`. Si se quiere conservar el
encapsulamiento, que el `default` use `findById` en vez de `findAll`, yJpa se encarga del
merge. En cualquiera de los dos casos hay que agregar `@Transactional` a los métodos de
`RutaService` que tocan más de un repositorio.

---

## 3. Terminar una ruta borra un ítem y después tira excepción

**Estado:** abierto
**Severidad:** alta
**Archivos:** `services/RutaService.java:103-107`

### Qué pasa

```java
} else {
  Optional<ItemEntrega> itemEncontrado = repoItemEntrega.findById(item.getIdDonacion());
  if(itemEncontrado.isPresent()){
    repoItemEntrega.deleteById(item.getIdDonacion());
    throw new IllegalArgumentException("Entrega no encontrada");
  }
}
```

El `throw` está adentro del `if (isPresent())`: es decir, se lanza **cuando el ítem
existía**, que es el caso para el que se escribió el `else`. La intención era claramente
`if (itemEncontrado.isEmpty()) throw`.

Consecuencias, todas en el mismo request:

- El ítem se borra igual, y después se tira la excepción: se pierde el resultado del borrado.
- La excepción corta el `for` de las paradas. Si la ruta tenía cinco ítems entregados, se borra
  el primero y los otros cuatro nunca se procesan.
- Nunca se llega a `chofer.disponible()`, `camionDeRuta.disponible()` ni al reseteo de carga
  (líneas 111-123). El chofer y el camión quedan bloqueados para siempre.
- `RutaController.terminarRuta:99` la traduce a un 400 con el mensaje "Entrega no encontrada",
  que además es falso: la entrega se encontró, se eliminó, y lo que faltó fue terminar el
  recorrido.

### Propuesta

Sacar el `throw` de adentro del `if` y dejar el borrado como está. Si de verdad se quiere
avisar cuando algo no está, tiene que ser con `isEmpty()`, y una excepción de negocio no
debería usarse para el flujo normal. El borrado de ítems entregados, además, merece una
decisión explícita: hoy destruye la fila que `ItemEntrega.eventos` usa como bitácora.

---

## 4. La condición del camión está invertida

**Estado:** abierto
**Severidad:** alta
**Archivos:** `services/RutaService.java:118-123`

### Qué pasa

```java
Optional<Camion> camion = repoCamiones.findByChofer_IdChofer(idChofer);
if (camion.isPresent()) {
    camion.get().eliminarChofer();
    gestorCamiones.resetearCamion(camion.get());
    throw new IllegalArgumentException("Camión no encontrado");
}
```

Mismo patrón que el punto 3, y el mismo error de fondo: `isPresent()` donde debía ser
`isEmpty()`. El flujo normal —el chofer tiene camión, que es exactamente lo que se pidió— cae
dentro del `if`, ejecuta el reseteo correcto y después tira "Camión no encontrado".

Peor todavía: el `else` implícito no hace nada. Si de verdad no hay camión, `terminarRuta`
responde 200 diciendo "Ruta finalizada correctamente" sin haber avisado nada.

Lo que salva parcialmente a este punto es que la excepción del punto 3 cortaba el método antes
de llegar acá. Arreglando el 3 sin tocar el 4, la excepción pasa a ser la que aparece siempre.

### Propuesta

Invertir a `if (camion.isEmpty())` y sacar el `throw` del camino feliz. Si la ausencia de
camión es un error de negocio, ahí va el `throw`, con un mensaje que describa lo que realmente
faltó.

---

## 5. Los tres DELETE devuelven 404 después de borrar bien

**Estado:** abierto
**Severidad:** alta
**Archivos:** `services/EntregaService.java:69-75`, `services/CamionService.java:67-73`, `services/ChoferService.java:57-63`

### Qué pasa

Los tres métodos tienen la misma forma:

```java
Optional<X> x = repo.findById(id);
if(x.isPresent()){
  repo.deleteById(id);
  throw new IllegalArgumentException("... no encontrado");
}
```

`DELETE /entregas/{id}`, `DELETE /camiones/{patente}` y `DELETE /choferes/{id}` borran el
registro y después lanzan excepción. Los tres controllers (líneas 105, 85 y 88
respectivamente) la cazan y devuelven **404 con el mensaje "no encontrado"**, cuando en
realidad el borrado funcionó y lo que no se encontró fue... nada.

Es el mismo bug repetido tres veces, lo cual sugiere que viene de un mismo patrón copiado. Y
es peor que una respuesta incorrecta: el cliente que reintenta el DELETE recibe 404 y da por
hecho que el recurso ya no está, cuando en realidad lo acaba de eliminar. Un `DELETE` que
siempre devuelve 404 tampoco sirve como idempotente.

El `if (isPresent())` sobre un `findById` seguido de un `deleteById` tampoco tiene sentido:
`deleteById` ya es no-op si no existe. El `isPresent` solo sirve para decidir cuándo tirar la
excepción, y la decisión está al revés.

### Propuesta

`repo.deleteById(id)` y nada más. Si el recurso no existe, 204 también es una respuesta
correcta para un DELETE y hace que el endpoint sea idempotente. Si hay que distinguir el
404, el chequeo va al revés: `if (x.isEmpty()) throw ...`.

---

## 6. La relación ítem-evento apunta al id equivocado

**Estado:** abierto
**Severidad:** alta
**Archivos:** `models/entities/ItemEntrega/ItemEntrega.java:51`

### Qué pasa

```java
@OneToMany(mappedBy = "id", cascade = CascadeType.ALL, orphanRemoval = true)
private List<EventoLogistica> eventos;
```

`mappedBy` debe apuntar al atributo del lado muchos que contiene la FK hacia acá. Del lado
`EventoLogistica` (`models/entities/EventoLogistica/EventoLogistica.java:19-47`) **no existe
ningún atributo** que referencie a `ItemEntrega`. Lo único que se llama `id` es la clave
primaria del propio evento (`id_evento`, `GenerationType.IDENTITY`).

Lo que Hibernate entiende entonces es que la FK vive en `item_entrega.id_evento` apuntando a
`evento_logistica.id_evento`: una relación inventada que no está en el modelo. Como el schema
se genera con `ddl-auto=update`, la columna `id_evento` se agrega a `item_entrega` y queda
siempre en NULL.

Los síntomas: `item.getEventos()` devuelve siempre lista vacía, y el `cascade = ALL` no
persiste los eventos que se agregan a la lista. La trazabilidad que el módulo promete en el
Swagger de `GET /entregas` no existe.

Peor: como `cascade = ALL` incluye `orphanRemoval`, ver el punto 11.

### Propuesta

El modelo necesita una FK real. Agregar en `EventoLogistica` un
`@ManyToOne @JoinColumn(name = "id_donacion", referencedColumnName = "id_donacion") private
ItemEntrega item;` y setear `mappedBy = "item"`. Si no se quiere esa FK, sacar la relación y
dejar `EventosLogistica` como bitácora suelta, consultada por `referenciaId`, que es como ya
funciona el polling del punto 13.

---

## 7. El getter de Parada ignora su propio campo

**Estado:** abierto
**Severidad:** alta
**Archivos:** `models/entities/Parada/Parada.java:37`, `models/entities/Parada/Parada.java:54-56`

### Qué pasa

```java
@ManyToOne
@JoinColumn(name = "id_entidad_beneficiaria", referencedColumnName = "id_entidad_beneficiaria", nullable = false)
private Entidad entidadDestino; //quedo raro porque hay un metodo que te da la entidad pero creo que es necesario pala la DB

public Entidad getEntidadDestino() {
    return items.getFirst().getEntidadDestino();
}
```

El getter escrito a mano **pisa** el que genera Lombok con `@Getter` a nivel de clase. O sea:
la columna `id_entidad_beneficiaria` de `parada` se persiste y se lee, y después se tira a la
basura, porque el getter devuelve otra cosa.

Y lo que devuelve no es seguro:

- `List.getFirst()` lanza `NoSuchElementException` sobre una lista vacía. Una `Parada` recién
  leída de la base puede no tener ítems cargados todavía, y `items` es LAZY.
- Devuelve el `entidadDestino` del **primer ítem**, no el de la parada. Si algún día una parada
  agrupara ítems de entidades distintas, el getter miente.
- El propio comentario de la línea 37 admite que la relación quedó rara. La solución fue
  agregar un `@JoinColumn` nullable y después un getter que lo ignora: el `@ManyToOne` sobra.

Los dos llamadores del getter son `RutaService.convertirAParadaDTO:153` y
`Ruta.agregarEntrega:54`, o sea que un `GET /rutas` puede reventar con 500.

### Propuesta

Borrar el getter manual de las líneas 54-56 y dejar que Lombok genere el del campo, que es
lo que el mapeo JPA ya persiste. Si de verdad el destino se deriva del ítem, entonces el
`@ManyToOne` de la línea 35-37 no debería estar.

---

## 8. Confirmar una entrega que no está en camino se ignora en silencio

**Estado:** abierto
**Severidad:** media
**Archivos:** `models/gestores/GestorPublicacionEventos.java:72`, `services/EntregaService.java:137-139`

### Qué pasa

`publicarEntregaConfirmada` envuelve todo su cuerpo en una guarda:

```java
if (item.getEstado() == EstadoEntrega.EN_CAMINO) {
    item.setFotoComprobante(foto);
    ...
}
return item;
```

Si el ítem no está `EN_CAMINO` —porque la ruta nunca se inició, o porque el ítem volvió a
`PENDIENTE` por el punto 10— el método devuelve el ítem sin tocarlo. No lanza, no avisa.

`EntregaService.actualizarEstado:137` no se entera: guarda lo que le devuelven y sigue. El
controller responde **200 con "Estado de la entrega actualizado correctamente a: ENTREGADA"**.
La foto no se guarda, el estado no cambia, no se emite evento, y el receptor de la entidad
cree que confirmó la entrega.

Es peor que un error visible: es un falso éxito.

### Propuesta

Que la guarda no se lleve el `return`. Si el estado no es `EN_CAMINO`, tirar una excepción de
negocio que diga que la entrega no está en camino. `publicarEntregaFallida` (punto 9) tiene el
problema inverso y se arregla en la misma línea de estilo.

---

## 9. Reportar una entrega fallida no valida el estado previo

**Estado:** abierto
**Severidad:** media
**Archivos:** `models/gestores/GestorPublicacionEventos.java:87-88`

### Qué pasa

```java
public ItemEntrega publicarEntregaFallida(ItemEntrega item, Ruta ruta, String justificacion) {
    item.getEstado().cambiarEstado(item, EstadoEntrega.NO_RECIBIDA);
```

No hay guarda de ningún tipo. El método transiciona desde el estado que sea.

Como `EntregaService.actualizarEstado:142` expone `case "NO_RECIBIDA":` sin chequear el estado
actual, un `PATCH /entregas/{id}/estado` con `"NO_RECIBIDA"` sobre un ítem que ya estaba
`ENTREGADA` lo revierte: queda `NO_RECIBIDA`, con su foto de comprobante cargada y con los
eventos `ENTREGA_CONFIRMADA` y `ENTREGA_FALLIDA` en la bitácora del mismo ítem.

La asimetría con `publicarEntregaConfirmada` es la señal: uno valida el estado de partida y el
otro no. En un dominio donde las transiciones importan, esa asimetría casi siempre es un
olvido y no una decisión.

### Propuesta

Agregar la guarda que falta. Lo razonable es que la transición válida sea desde `EN_CAMINO`, y
que cualquier otro estado de partida tire excepción de negocio en vez de mutar en silencio.

---

## 10. El reingreso a depósito no valida nada

**Estado:** abierto
**Severidad:** media
**Archivos:** `services/EntregaService.java:151-155`, `models/gestores/GestorPublicacionEventos.java:101-102`

### Qué pasa

```java
case "PENDIENTE":
    // Reingreso a depósito tras revisión de una entrega NO_RECIBIDA.
    // reingresarADeposito() ya valida que solo se pueda hacer desde NO_RECIBIDA.
    repoItemEntrega.saveAndFlush(gestorPublicacionEventos.publicarReingresoDeposito(item));
    break;
```

El comentario promete una validación en dos lugares donde no hay nada:

1. `reingresarADeposito()` **no existe**. No está en `GestorPublicacionEventos`, no está en
   ningún service del módulo. El comentario quedó del merge.
2. `publicarReingresoDeposito` (líneas 101-102) es exactamente igual de permisivo que el
   punto 9: `cambiarEstado(item, PENDIENTE)` y nada más.

O sea que un `PATCH /entregas/{id}/estado` con `"PENDIENTE"` sobre **cualquier** ítem lo manda
a `PENDIENTE`. También sirve para deshacer una `ENTREGA_CONFIRMADA`, incluso con foto.

Y el efecto no es inocuo: al volver a `PENDIENTE` el ítem vuelve a entrar en
`findByEstado(EstadoEntrega.PENDIENTE)`, que es la query que alimenta el planificador nightly
(`PlanificadorDeRutasScheduler:44`). Un endpoint sin validar puede generar rutas duplicadas a
las 2 AM.

### Propuesta

Implementar la validación que el comentario describe: solo desde `NO_RECIBIDA`, y el `case`
debería pedir el estado explícitamente. Si el método que el comentario menciona iba a existir,
crearlo; si no, borrar la referencia para que el próximo que lea no busque algo que no está.

---

## 11. El mismo evento se mete en la lista de todos los ítems

**Estado:** abierto
**Severidad:** media
**Archivos:** `models/gestores/GestorPublicacionEventos.java:60-66`

### Qué pasa

```java
EventoLogistica evento = new EventoLogistica("INICIO_RUTA", ruta.getIdRuta().toString(), LocalDateTime.now(), null);
evento.setPayloadJson(serializar(payload));

ruta.getParadas().forEach(parada -> parada.getItems().forEach(item -> item.getEventos().add(evento)));
repoEventos.save(evento);
```

Una sola instancia de `EventoLogistica` se agrega a la lista `eventos` de **todos** los ítems de
la ruta. Con el mapeo del punto 6 eso ya no persiste nada, así que hoy el daño está tapado. Pero
en cuanto se arregle el `mappedBy`, aparecen dos problemas:

- `cascade = CascadeType.ALL` sobre la lista hace que `save` de cualquier ítem intente
  persistir el mismo evento una vez por ítem, y la segunda vez Hibernate lo trata como entidad
  detached o como unflushed.
- `orphanRemoval = true` significa que el evento se considera hijo de cada ítem. Desvincularlo
  de uno de ellos lo borra, aunque siga en los otros. Con diez ítems en la ruta, un solo
  `remove` borra el evento de los diez.

Además el `add` explícito no hace falta: `cascade = ALL` ya persiste los hijos agregados.

### Propuesta

Dejar que el cascade se encargue: guardar el evento una vez con `repoEventos.save(evento)`,
setear la referencia inversa en el ítem, y no tocar la lista a mano. Si el evento es realmente
de la ruta y no del ítem, el modelo está mal desde el punto 6.

---

## 12. El polling de eventos reenvía el último evento y explota con `desdeId` null

**Estado:** abierto
**Severidad:** media
**Archivos:** `services/EventoLogisticaService.java:20`, `RabbitMQ/SolicitudEventosListener.java:29`, `dto/evento/SolicitudEventosDTO.java:9`

### Qué pasa

```java
public EventoLogisticaResponseDTO obtenerEventosNuevos(Long desdeId) {
    return new EventoLogisticaResponseDTO(convertirEventosADTO(repoEventos.findByIdGreaterThanOrderByIdAsc((desdeId - 1))));
}
```

Tres problemas en esa línea:

**Off-by-one.** Se pide `id > desdeId - 1`, o sea `id >= desdeId`. El evento con `id ==
desdeId` es exactamente el último que el cliente ya procesó, y se lo vuelve a mandar. El
propio contrato lo dice al revés: `EventoLogisticaController:38` documenta "Se devolverán los
eventos con ID estrictamente mayor a este valor". Con un poll por segundo, el consumidor
notifica el mismo evento indefinidamente.

**NPE por unboxing.** `desdeId - 1` desempaqueta el `Long`. `SolicitudEventosDTO:9` declara
`private Long desdeId;` sin valor por defecto, y `SolicitudEventosListener:29` lo pasa directo
desde el mensaje de RabbitMQ. Un mensaje sin `desdeId` —o con `desdeId: null`, que es lo que
manda un productor que no lo setea— revienta con `NullPointerException` en el listener.

**Carga entera.** Ver punto 13.

### Propuesta

Pasar `desdeId` sin restar y dejar que `findByIdGreaterThanOrderByIdAsc` sea un derived query
real (`WHERE e.id > :id ORDER BY e.id ASC`), que resuelve el off-by-one y el orden de una. Y
proteger el null: `@RequestParam(required = false)` con default `0L` en el controller, y en el
listener `solicitud.getDesdeId() != null ? ... : 0L`.

---

## 13. `findByIdGreaterThanOrderByIdAsc` no ordena

**Estado:** abierto
**Severidad:** media
**Archivos:** `models/repositories/eventos/RepositorioEventoLogistica.java:12-16`

### Qué pasa

```java
default List<EventoLogistica> findByIdGreaterThanOrderByIdAsc(Long id) {
    return this.findAll().stream()
                  .filter(e -> e.getId() > id)
                  // Al guardarse secuencialmente en la lista, el orden de fecha y de ID coinciden
                  .collect(Collectors.toList());
}
```

El nombre dice `OrderByIdAsc` y no hay ningún orden. `findAll()` de Spring Data no lleva
`ORDER BY` implícito: el orden que devuelva es el que JPA le dé a la base, que no está
garantizado y cambia según el plan de ejecución. En cuanto haya dos eventos con el mismo
timestamp o un índice distinto, el consumidor de polling (punto 12) recibe la bitácora
desordenada.

El comentario de la línea 15 justifica el shortcutsolo para el caso trivial de una base
vacía. No aplica cuando la tabla tiene las miles de filas que produce el resto del módulo.

Además trae toda la tabla a memoria para filtrar en el cliente, y `e.getId() > id` con `getId`
en `Long` vuelve a desempaquetar, así que un evento con `id` null —recién insertado dentro de
la misma sesión, antes del flush— revienta.

### Propuesta

Dejar de escribirlo a mano y declararlo como derived query:

```java
List<EventoLogistica> findByIdGreaterThanOrderByIdAsc(Long id);
```

Spring Data genera `where id_evento > ?1 order by id_evento asc` y filtra en SQL, que es lo que
el nombre ya promete.

---

## 14. `GET /entregas` y `GET /entregas/{id}` devuelven la entidad cruda

**Estado:** abierto
**Severidad:** media
**Archivos:** `controllers/EntregaController.java:47-53`, `services/EntregaService.java:64-67`

### Qué pasa

`EntregaController.obtenerPorId` hace:

```java
return ResponseEntity.ok(entregaService.findById(id));
```

y `EntregaService.findById:65` devuelve un `ItemEntrega`, la entidad JPA, no un DTO. Falta el
`convertirABienDTO` que el mismo service usa en `findAll` (línea 61).

Serializar la entidad cruda rompe de dos maneras:

- **Ciclo.** Jackson sigue las getters: `ItemEntrega.getParada()` → `Parada.getRuta()` →
  `Ruta.getParadas()` → `Parada.getItems()` → `ItemEntrega.getParada()`... hasta
  `StackOverflowError` o un `OutOfMemoryError` al construir el JSON.
- **Fuga de esquema.** Aunque no hubiera ciclo, la respuesta expondría `id_unidad_medida`,
  `id_parada`, `id_donacion` y toda la estructura interna, en un endpoint que el Swagger
  declara como `BienDTO`.

Lo mismo pasa con `GET /entregas/no-recibidas`, que sí usa el mapper, así que la inconsistencia
dentro del mismo controller es lo que hace evidente que es un olvido.

### Propuesta

`findById` que devuelva `BienDTO` con `convertirABienDTO`, y anotarlo en el controller con
`@ApiResponse` de 200. Y poner `@JsonIgnore` en las relaciones inversas (`ItemEntrega.parada`)
como red, para que un mapping equivocado no se convierta en un DoS.

---

## 15. Registrar una donación duplica la dirección N veces

**Estado:** abierto
**Severidad:** media
**Archivos:** `services/EntregaService.java:84-106`

### Qué pasa

El `for` sobre los bienes tiene adentro la construcción de la dirección:

```java
for (int j = 0; j < bienes.size(); j++) {
    BienDTO bien = bienes.get(j);
    Direccion direccionEntidad = this.convertirDireccionDTO(request.getEntidadBeneficiaria());
    repoPaises.save(direccionEntidad.getCiudad().getProvincia().getPais());
    repoProvincias.save(direccionEntidad.getCiudad().getProvincia());
    repoCiudades.save(direccionEntidad.getCiudad());
    repoDirecciones.save(direccionEntidad);
    Entidad nuevaEntidad = new Entidad(request.getEntidadBeneficiaria().getIdEntidad(), direccionEntidad);
    repoEntidades.save(nuevaEntidad);
    ...
}
```

`request.getEntidadBeneficiaria()` es **el mismo objeto para todos los bienes**, pero
`convertirDireccionDTO` (líneas 187-194) construye un `Direccion` nuevo, y el constructor de
`Direccion` (línea 49) construye un `Pais` nuevo, que a su vez trae un `Provincia` nueva, que
trae una `Ciudad` nueva.

Con una donación de 3 bienes, quedan en la base 3 países, 3 provincias, 3 ciudades y 3
direcciones con el mismo contenido. `Pais`, `Provincia` y `Ciudad` usan
`GenerationType.IDENTITY`, así que cada iteración inserta una fila nueva sin deduplicar. Con
las rutas de una campaña de donaciones, la tabla `ciudad` se infla de forma lineal con la
cantidad de bienes.

El `repoEntidades.save` en la línea 93 es peor: `Entidad` tiene id manual
(`id_entidad_beneficiaria`), así que en la segunda iteración no inserta: hace merge de la misma
fila y le **cambia la `id_direccion_destino` a la dirección de la iteración 2**. Al terminar,
la entidad apunta a la última de las tres direcciones duplicadas y las otras dos quedan
huérfanas.

### Propuesta

Sacar la construcción de la dirección y de la entidad fuera del `for`: se calculan una vez y se
reusan para todos los bienes. Y para el lado del `save`, resolver país/provincia/ciudad por
nombre (`findByNombre`) antes de insertar, en vez de confiar en que se dupliquen.

---

## 16. `procesarPeticion` no valida el payload

**Estado:** abierto
**Severidad:** media
**Archivos:** `services/EntregaService.java:78-106`

### Qué pasa

El método valida `request` y `bienes`, y nada más. Tres caminos a excepción:

**`request.getEntidadBeneficiaria()` en null.** `convertirDireccionDTO:188` devuelve `null`
para un dto `null`, y la línea 87 hace `direccionEntidad.getCiudad()` sobre ese `null`:
`NullPointerException` en la primera iteración. La línea 92 desreferencia
`request.getEntidadBeneficiaria()` otra vez, así que aunque se evitara el 87, el 92 revienta
igualmente.

**`getIdsDonaciones()` en null o corto.** La línea 100 indexa
`request.getDonacionResumen().getIdsDonaciones().get(j)` con el mismo `j` que recorre
`bienes`, sin verificar que ambas listas tengan la misma longitud. Si el producer manda tres
bienes y dos ids, `IndexOutOfBoundsException` después de haber insertado país, provincia,
ciudad, dirección y entidad: la base queda a medias.

**`getDonacionResumen()` en null.** La línea 81 desreferencia sin chequear, y solo se valida
`getBienes()`, que es un nivel más adentro.

`EntregaController:69` lo transforma todo en un 400 con el mensaje del `NullPointerException`,
que es `null`. Y `DonacionListener:21` lo traga con un `System.err.println`, así que el mensaje
de RabbitMQ se acepta igual y el evento se pierde en silencio.

### Propuesta

Validar la estructura del payload una vez, arriba, antes de tocar la base: `entidadBeneficiaria`,
`donacionResumen`, `idsDonaciones` no nulos y `idsDonaciones.size() == bienes.size()`. Con un
`@Valid` y anotaciones en el DTO sale bastante más limpio que la validación a mano.

---

## 17. El planificador resetea la carga de los camiones y nunca la persiste

**Estado:** abierto
**Severidad:** media
**Archivos:** `models/entities/PlanificadorDeRutas/ProveedorRutasExterno/ProveedorRutasExternoSimulado.java:63`, `ProveedorRutasExternoSimulado.java:77`, `PlanificadorDeRutasScheduler.java:62`

### Qué pasa

`procesarAgrupacion` muta los `Camion` que recibió:

```java
for (Camion c : camionesDisponibles) {
    asignacion.put(c.getPatente(), new ArrayList<>());
    c.resetearCargaOcupada();
}
...
c.cargar(item, ciudad);
```

`resetearCargaOcupada` pone `pesoOcupado`, `volumenOcupado` y `ciudadDestinoActual` en cero;
`cargar` los vuelve a acumular. Pero:

1. **Nunca se guarda.** No hay `repoCamiones.save()` en ninguna parte de la clase: no tiene
   repositorio inyectado. Las columnas `peso_ocupado_kg` y `volumen_ocupado_m3` quedan siempre
   en 0.
2. **Las entidades están detached.** `PlanificadorDeRutasScheduler:45` las saca de
   `repoCamiones.findAll()`, cuya transacción ya se cerró, y después las muta desde un
   `CompletableFuture.runAsync` (línea 28), o sea en otro hilo. Mutar entidades JPA fuera de
   sesión es una escritura que no existe.

El efecto es que `Camion.puedeCargar(ItemEntrega)` (líneas 76-79) siempre calcula contra 0, y
`Camion.estaVacio()` siempre da `true`. Por eso la rama 2 de la línea 88 (`c.estaVacio()`)
gana siempre y la rama 1 de la línea 77 es **código muerto**: `ciudadDestinoActual` se acaba
de resetear a `null` en la línea 63, así que `ciudad.equals(c.getCiudadDestinoActual())`
nunca puede dar true. La agrupación por ciudad, que es el objetivo del algoritmo, no está
implementada.

Y `PlanificadorDeRutasScheduler:62` pasa la **misma** lista `camionesDisponibles` a cada uno
de los lotes del `for` (línea 60). Cada lote dispara su propio `runAsync` asíncrono, así que N
hilos resetean y cargan los mismos objetos `Camion` a la vez, sin sincronización. Con dos
lotes de 100, la carga acumulada es una intercalación no determinista de los dos.

### Propuesta

Decidir qué representa el estado de carga y persistirlo: si es parte del modelo, guardar los
camiones al final de la asignación, dentro de una transacción, en el hilo que la hace. Si
`ciudadDestinoActual` es estado en memoria de una sola pasada, que el planificador deje de
compartir las instancias entre lotes y use `subList` de una copia por hilo. Y sacar la rama 1
muerta o arreglar el reseteo para que no la mate.

---

## 18. `return null` en el catch del planificador manual

**Estado:** abierto
**Severidad:** baja
**Archivos:** `controllers/PlanificadorDeRutasController.java:88-101`

### Qué pasa

```java
@PostMapping("/planificar-manual")
public ResponseEntity<String> forzarPlanificacionManual() {
    try {
        planificadorScheduler.iniciarPlanificacionAutomatica();
        return ResponseEntity.ok("Proceso de planificación disparado. Aguardando respuesta del proveedor externo...");
    } catch (Exception e) {
        System.err.println("=================================");
        ...
        return null;
    }
}
```

El único `catch` del método devuelve `null` en vez de una `ResponseEntity`. Spring recibe un
`null` de un handler de request, tira `IllegalStateException` al intentar escribir la
respuesta, y el cliente recibe un 500 genérico sin cuerpo. Los siete `println` que lo anteceden
explican el error real en el log del servidor, que es justo donde el que depura no está
mirando.

`iniciarPlanificacionAutomatica` ya traga sus excepciones de base de datos (líneas 49-52), así
que este camino solo se toma por fallos del proveedor externo o por estado corrupto del plan.

### Propuesta

Devolver `ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " +
e.getMessage())`, como ya hace `PlanificadorDeRutasController:74` en el otro endpoint del mismo
controller. Y bajar los `println` a `log.error` con la excepción completa.

---

## 19. La validación de la justificación de una entrega fallida está invertida

**Estado:** abierto
**Severidad:** crítica
**Archivo:** `src/main/java/ar/edu/utn/frba/ddsi/logisticas/services/EntregaService.java:143-145`

### Qué pasa

```java
case "NO_RECIBIDA":
  if(comprobarExistencia(request.getJustificacion())) {
    throw new IllegalArgumentException("Se requiere justificar el motivo por el cual falló la entrega.");
  }
```

La condición está al revés. `comprobarExistencia(...)` devuelve verdadero cuando el texto
**sí** está escrito, así que el endpoint:

- Rechaza la entrega fallida cuando el donante escribió la justificación.
- Acepta la entrega fallida sin justificación, que es justo lo que el mensaje dice impedir.

### Por qué es el más grave del módulo

Es el único de los dieciocho donde una validación hace lo contrario de lo que dice. Los
demás fallan de forma ruidosa: un 404, un 500, un estado que no se persiste. Este acepta
datos inválidos en silencio y rechaza los válidos, así que en producción el síntoma va a ser
"los donantes no pueden reportar una entrega fallida" más entregas fallidas sin motivo.

Los dos `if` de alrededor repiten el patrón, lo cual sugiere que se copiaron sin revisar:

- `EntregaService.java:134` — `if(comprobarExistencia(request.getFotoUrl()))` exige la foto.
- `EntregaService.java:143` — `if(comprobarExistencia(request.getJustificacion()))` exige el motivo.

El primero está en el `case "ENTREGADA"` y hace lo que dice. El segundo está en
`case "NO_RECIBIDA"` y hace lo contrario. La misma función, el mismo helper, sentido opuesto.

### Propuesta

Invertir a `if(!comprobarExistencia(...))`. De paso, el `break` que sigue al `throw` es
código muerto: cuando la condición se cumple el método ya salió por la excepción, así que
nunca se llega.

---
## 20. Verificado: logística no invoca a MetaDonacion ni a incentivos, y no habla con notificaciones

**Estado:** verificado, sin cambios necesarios
**Severidad:** informativa
**Archivos:** todo `logisticas-service/src/main`

### Qué exigía el enunciado

> *3. El servicio de logística no debe invocar los servicios de donaciones ni incentivos sino
> dejar disponible la información.*
>
> *4. El servicio de logística no debe comunicarse con el servicio de notificaciones.*

### Cómo se verificó

Se revisaron las cuatro vías por las que un servicio podría invocar a otro: imports de código,
dependencias de Maven, URLs configuradas y llamadas HTTP o por broker.

| Vía | Resultado |
|---|---|
| Imports de `...donaciones.` o `...incentivos.` en logística | **ninguno** |
| Imports de `...notificaciones.` en logística | **ninguno** |
| URLs de esos servicios en `application.properties` | **ninguna** (solo `spring.datasource.url`) |
| `RestTemplate`, `WebClient`, `FeignClient`, `HttpClient` | solo el `HttpClient` del proveedor externo de ruteo |
| Exchanges ajenos declarados en logística | **ninguno** |

Las únicas dos URLs literales del módulo son:

- `ProveedorRutasExternoSimulado` → `http://localhost:8086/api/PlanificacionRutas/callback`,
  que es **a sí mismo**, el callback que el enunciado exige en el punto 1 de implementación.
- `GestorPublicacionEventos` → `https://donaciones-app.example.com/seguimiento/`, una plantilla
  de texto para armar el enlace de seguimiento que pide el caso de "inicio de ruta". No es una
  llamada.

### Cómo se cumple en la práctica

Logística **deja disponible la información** publicando eventos en
`logistica.eventos.exchange` (routing key `logistica.evento`). No llama a nadie para que notifique:
`donaciones-service` está suscrito a esa cola y es quien dispara las notificaciones, porque es
el que conoce a los donantes, las entidades y los administradores.

Eso es justamente lo que hace posible cumplir los tres casos de notificación exigidos
(inicio de ruta, entrega realizada, entrega no satisfactoria) sin violar la prohibición: la
dirección del flujo es invertida respecto de lo que se suele hacer.

Verificado en vivo: se publicó un evento a mano por la management API de Rabbit y el listener
de `donaciones-service` lo procesó (`Evento 9999 procesado`, hilo `rabbit-simple-0`).

### Lo que queda: una dependencia sin uso

`logisticas-service/pom.xml` declara `notificaciones-service` como dependencia `compile`, pero
**el módulo no importa ni una sola clase de ese artefacto**. Es una dependencia que no se usa
y tiene un costo real: obliga a compilar con `-am` o a tener el jar instalado en el `.m2`, y
rompe `mvn package -pl logisticas-service` con "Could not find artifact".

Es exactamente el punto 4 del backlog de `donaciones-service`, del lado de logística. Se
anota acá porque el enunciado pide que la separación sea real, y una dependencia de Maven entre
dos microservicios contradice esa separación aunque no se use. **No se tocó**: quitar una
dependencia del pom amerita confirmar con el equipo que no hay planes de reusar código de
notificaciones desde logística, que es lo que induce a esa dependencia.

---
## Corregidos

### El módulo se commiteó con marcadores de conflicto de merge sin resolver

**Estado:** corregido
**Severidad:** crítica
**Archivo:** `.../gestores/GestorPublicacionEventos.java`

Traía los marcadores `<<<<<<< HEAD`, `=======` y `>>>>>>>` de dos ramas distintas en el mismo
archivo, y así quedó commiteado: el módulo entero no compilaba.

**Qué se resolvió:** se conservó la versión de `GestorPublicacionEventos` y se descartó la de
`GestorEventos`, porque `EntregaService` y `RutaService` consumen la primera. Los dos métodos de
la otra (`buscarEventos` y `guardarEvento`) no los usa nadie en el módulo.

### `RepositorioCamiones` y `RepositorioChoferes` duplicados en dos paquetes

**Estado:** corregido
**Severidad:** alta
**Archivos:** `.../repositories/RepositorioCamiones.java`, `.../repositories/RepositorioChoferes.java`

El merge dejó los mismos repositorios en el paquete plano y en subpaquete. Los consumidores
importaban el del paquete plano, pero usaban métodos que **solo existen en la versión de
subpaquete** (`findByChofer`, `actualizarEstado`, `findByEstado`, `findByIdDonacion`): el módulo
no compilaba.

**Qué se resolvió:** se consolidó hacia los subpaquetes, que son superconjunto, y se borraron los
duplicados del paquete plano. Además, borrar los duplicados evita que Spring Data registre dos
beans con el mismo nombre.

### Cinco `DataSourceConfig` apuntaban a cinco bases distintas

**Estado:** corregido
**Severidad:** crítica
**Archivos:** `.../config/CamionesDataSourceConfig.java`, `ChoferesDataSourceConfig.java`,
`EventosDataSourceConfig.java`, `ItemsDataSourceConfig.java`, `RutasDataSourceConfig.java`

Cada uno creaba su propio `DataSource`, `EntityManagerFactory` y `TransactionManager`, apuntando
a una base distinta (`camiones`, `choferes`, `eventos`, `items`, `rutas`) con **credenciales
hardcodeadas** que ignoraban `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`.

**Por qué no podía funcionar:** el modelo es una base por agregado, pero los agregados se
referencian entre sí. `Ruta` referencia `Camion` y `Parada`; `ItemEntrega` referencia `Parada`,
`UnidadDeMedida`, `Entidad` y `EventoLogistica`; `Camion` referencia `Chofer`. Al arrancar se
moría con `AnnotationException: Association 'Camion.chofer' targets an unknown entity`, porque
`Chofer` no estaba en la unidad de persistencia de `Camion`.

**Qué se resolvió:** se borraron los cinco. Un solo `DataSource` sobre la base `logisticas`, con
las entidades y los repositorios donde ya estaban. Las 12 tablas se crean ahí.

**Efecto secundario:** las credenciales dejaron de estar hardcodeadas, así que el servicio
ahora arranca dentro de Docker con las variables de entorno.

### El binding de `solicitudEventosQueue` usaba el exchange equivocado

**Estado:** corregido
**Severidad:** media
**Archivo:** `.../config/RabbitMQConfig.java`

Ataba la cola al `donaciones.exchange` en vez de al `logisticas.exchange`. Funcionaba solo
porque `donaciones-service` declara el mismo binding contra el exchange correcto y el broker
acumula las dos declaraciones: levantando logística sola, las solicitudes de eventos quedaban
sin ruta.

**Qué se resolvió:** el binding usa el exchange de integración, que es al que publica
`LogisticaPollingScheduler`.

### `SolicitudEventosListener` hacía request/response por cola

**Estado:** corregido
**Severidad:** media
**Archivo:** `.../RabbitMQ/SolicitudEventosListener.java`

Publicaba la respuesta en el exchange para que volviera a la cola del que preguntó. Eso convierte
el broker en un request/response: necesita dos colas y dos bindings por consumidor, y se rompe
entero si el que preguntó se cae antes de leer la respuesta.

**Qué se cambió:** el listener deja de publicar la respuesta. La trazabilidad queda disponible
por HTTP en `GET /api/eventos`, que es lo que pide el enunciado al describir el despliegue de
logística como accesible por sus URIs. El polling queda como red de contención.

### `DonacionListener` se tragaba todos los errores

**Estado:** corregido
**Severidad:** media
**Archivo:** `.../RabbitMQ/DonacionListener.java`

El `catch (Exception)` con `System.err.println` descartaba el mensaje sin dejar rastro: una donación
se perdía sin registrar por qué.

**Qué se cambió:** los errores de negocio (`IllegalArgumentException`) no se relanzan, porque van
a fallar igual en cada reintento y bloquearían la cola compartida de la que salen las N
instancias de logística. Los demás se relanzan a propósito, para que la dead letter queue los
reciba.

---
# Corregidos
