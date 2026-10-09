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
| 1 | 3 | Terminar una ruta borra un ítem y después tira excepción: corta el recorrido y deja chofer y camión bloqueados |
| 2 | 2 | El estado de la ruta se persiste por efecto colateral de OSIV: con OSIV apagado, iniciar y terminar no guardan nada |
| 3 | 4 | La condición del camión está invertida: el caso normal responde "Camión no encontrado" |
| 6 | 26 | Replanificar crea rutas duplicadas para los mismos ítems, cada noche y en cada manual |
| 38 | 46 | Los lotes comparten la lista de camiones: un camión queda en dos rutas del mismo día y por encima de su capacidad |
| 39 | 47 | Dos callbacks concurrentes eligen el mismo chofer: una persona en dos camiones y un 500 |
| 8 | 27 | `findByChofer` devuelve una ruta histórica: iniciar/terminar opera sobre la ruta equivocada |
| 9 | 8 | Confirmar una entrega que no está en camino se ignora en silencio y responde 200 |
| 10 | 9 | Reportar una entrega fallida no valida el estado previo: una entrega_ok se puede revertir |
| 11 | 10 | El reingreso a depósito no valida nada y su comentario cita un método que no existe |
| 13 | 29 | Un PATCH/PUT sin el campo `disponible` aplica lo contrario: ocupa en silencio o revienta |
| 14 | 30 | `POST /entregas` responde 201 sin registrar nada cuando `bienes` o `idsDonaciones` vienen null |
| 15 | 32 | Toda violación de integridad se trata como carrera benigna: la donación se pierde sin DLQ |
| 40 | 48 | Un fallo del broker al publicar después del commit deja el 500, el cambio aplicado y la notificación perdida |
| 20 | 35 | Cada mensaje inserta país, provincia, ciudad y dirección nuevos aunque la entidad ya exista |
| 21 | 31 | El callback del simulador no tiene timeout y descarta la respuesta: un lote se pierde en silencio |
| 23 | 34 | `UnidadDeMedida` persiste constantes estáticas: cada reinicio duplica las filas |
| 24 | 17 | El planificador resetea la carga de los camiones y nunca la persiste |
| 25 | 7 | El getter de Parada ignora su propio campo: la columna persistida no se usa y depende de la lista |
| 26 | 18 | `return null` en el catch del planificador manual: el error se pierde |
| 27 | 36 | El callback devuelve 500 con internals para payloads que su contrato documenta como 400 |
| 28 | 37 | El CRUD de camiones/choferes devuelve 500 para errores de validación |
| 29 | 38 | Los repositorios de país/provincia/ciudad declaran ID `UUID` y la entidad tiene `Long` |
| 31 | 20 | Verificado: logística no invoca a `donaciones-service` ni incentivos ni habla con notificaciones |
| 32 | 44 | Los listados devuelven la tabla entera: sin paginar, una respuesta crece sin techo |
| 33 | 43 | El manejo de errores está repetido en cada controller: el mismo error da 404 en un método y 400 en otro |
| 34 | 40 | Los gestores están nombrados por entidad y mezclan reglas de negocio con persistencia |
| 35 | 41 | Los repositorios conviven en dos esquemas distintos, sin un criterio que los separe |
| 36 | 42 | Paquetes de primer nivel y ubicación de clientes, scheduler y eventos distintos al resto de los servicios |
| 37 | 45 | El callback del proveedor externo está fijado a `localhost:8086`: si el puerto cambia o el proveedor es real, el resultado no vuelve |
| 41 | 49 | Cada cambio de estado de una entrega recorre todas las rutas con sus paradas e ítems |
---

## 2. El estado de la ruta se persiste por efecto colateral de OSIV

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

Tiene tres defectos:

1. `this.findAll()` devuelve una `List` **nueva** en cada llamada: el `indexOf` mide contra una
   lista y el `set` escribe sobre otra. El `set` es tirar la lista a la basura.
2. `indexOf` usa `equals`, y `Ruta` no lo overridea, así que compara identidad.
3. No hay `save()` ni `@Transactional`.

**Verificado en vivo el 2026-10-09: el síntoma no se reproduce.** Con el servicio corriendo
contra una base real, `PATCH /rutas/chofer/{id}/iniciar` dejó la fila en `EN_CURSO` y con
`url_seguimiento` escrita, y `PATCH .../terminar` la dejó en `FINALIZADA`. Ninguna de las dos se
quedó en `PROGRAMADA`.

El motivo es `spring.jpa.open-in-view`, que está **encendido por defecto** y Spring avisa al
arrancar que conviene apagarlo. Con OSIV hay un `EntityManager` abierto durante todo el request,
así que los dos `findAll()` comparten el persistence context y devuelven **las mismas
instancias**: el `indexOf` encuentra la ruta y el `setEstado` corre sobre una entidad gestionada.
El `UPDATE` sale cuando otra llamada a repositorio hace flush —el `save` del chofer o del camión
que vienen después—, pero sale.

El punto queda vivo igual, por dos razones:

- **Depende de OSIV.** Apagarlo, que es lo que Spring recomienda, vuelve el síntoma real: las
  entidades quedan detached, `indexOf` devuelve `-1` y el estado no se guarda nunca.
- **Depende del orden.** El estado se escribe solo si después de la mutación hay otra llamada a
  repositorio que haga flush. Hoy la hay en los dos caminos, pero es una casualidad del orden
  del método, no una garantía.

`iniciarRuta` tiene hoy `@Transactional` (lo agregó el arreglo del punto 28), así que ese camino
ya no depende de OSIV. `terminarRuta` no lo tiene.

La misma dependencia aparece en `terminarRuta`: los ítems no entregados se mutan a `PENDIENTE`
con `publicarReingresoDeposito` y nadie los guarda. Lo único que los persiste es el flush que
dispara el `save` del evento de ese método, que va en su propia transacción. Con OSIV apagado
el evento de reingreso queda guardado pero el ítem sigue en `NO_RECIBIDA`: la bitácora y el
estado se contradicen.

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

## 7. El getter de Parada ignora su propio campo

**Estado:** abierto
**Severidad:** baja
**Archivos:** `models/entities/Parada/Parada.java:50-52`, `models/entities/Parada/Parada.java:83-84`, `models/entities/Ruta/Ruta.java:69`

### Qué pasa

```java
@ManyToOne
@JoinColumn(name = "id_entidad_beneficiaria", referencedColumnName = "id_entidad_beneficiaria", nullable = false)
private Entidad entidadDestino; //quedo raro porque hay un metodo que te da la entidad pero creo que es necesario pala la DB

public Entidad getEntidadDestino() {
    return items.isEmpty() ? null : items.getFirst().getEntidadDestino();
}
```

El getter escrito a mano **pisa** el que genera Lombok con `@Getter` a nivel de clase. O sea:
la columna `id_entidad_beneficiaria` de `parada` se persiste y se lee, y después se tira a la
basura: el getter devuelve la entidad del **primer ítem**, no la de la parada, y `null` cuando
no hay ítems.

**Por qué bajó de severidad:** el `NoSuchElementException` del `getFirst()` sobre lista vacía
(que reventaba `GET /rutas` con 500) está cubierto en el árbol de trabajo, y bien: el guard
`items.isEmpty() ? null : ...` más el `convertirADireccionDTO` tolerante a `null` de
`RutaService:163-164` son correctos. **Pero están sin commitear** — en el último commit la
parada vacía sigue tirando 500, así que el punto cierra recién cuando eso se commitee.

Lo que queda abierto son dos cosas:

- **La columna sigue muerta.** El `@ManyToOne` de la línea 50-52 persiste un destino que
  nadie lee nunca: todo lo que importa sale de `items`. O se usa el campo, o se borra.
- **El `null` nuevo viaja hasta `Ruta.agregarEntrega:69`:**

  ```java
  .filter(p -> p.getEntidadDestino().equals(item.getEntidadDestino()))
  ```

  Si alguna parada de la ruta quedó sin ítems (el javadoc del propio getter admite que pasa:
  "la entrega se elimino, o la ruta se planifico y todavia no se le asigno nada"), ese
  `equals` sobre `null` es un `NullPointerException` en plena planificación.

### Cómo se dispara

1. Con el último commit (sin los guards): `GET /api/rutas` con una parada sin ítems → 500.
2. Con el árbol de trabajo: una ruta que ya tiene una parada sin ítems a la que se le
   planifica otra entrega → `NullPointerException` en `Ruta.java:69`.

### Propuesta

Commitear los guards que ya están escritos. Y atacar la raíz: borrar el getter manual y
dejar que Lombok genere el del campo (que es lo que el mapeo JPA persiste), o si el destino
se deriva del ítem, sacar el `@ManyToOne`. Si se mantiene el getter derivado, el filtro de
`agregarEntrega` tiene que tolerar `null` (`Objects::equals`).

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

## 20. Verificado: logística no invoca a `donaciones-service` ni a `incentivos`, y no habla con notificaciones

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

### Lo que queda: una dependencia sin uso — RESUELTO el 2026-10-07

`logisticas-service/pom.xml` declaraba `notificaciones-service` como dependencia `compile`, pero
**el módulo no importa ni una sola clase de ese artefacto**. Era una dependencia que no se usaba
y tenía un costo real: obligaba a compilar con `-am` o a tener el jar instalado en el `.m2`, y
rompía `mvn package -pl logisticas-service` con "Could not find artifact".

Es exactamente el punto 4 del backlog de `donaciones-service`, del lado de logística. Se
anotaba acá porque el enunciado pide que la separación sea real, y una dependencia de Maven entre
dos microservicios contradice esa separación aunque no se use.

**Se quitó** la declaración del `pom.xml` el 2026-10-07, después de confirmar que no hay ni un
import de notificaciones en el módulo (el único match era la palabra en un comentario). Se
verificó en vivo: `mvn -pl logisticas-service test` pasaba de `BUILD FAILURE` por no encontrar
el artefacto a `BUILD SUCCESS`, sin necesitar `-am`.

La verificación de que la separación es real ya está arriba, en la tabla: cero imports, cero
llamadas, cero dependencias.

---
## 26. Replanificar crea rutas duplicadas para los mismos ítems, cada noche y en cada manual

**Estado:** abierto
**Severidad:** alta
**Archivos:** `Scheduler/PlanificadorDeRutasScheduler.java:36,44`, `controllers/PlanificadorDeRutasController.java:88-91`, `models/entities/PlanificadorDeRutas/PlanificadorDeRutas.java:45-90`, `services/PlanificadorRutasService.java:80-97`

### Qué pasa

El cron de las 02:00 (y `POST /PlanificacionRutas/planificar-manual`, que llama al mismo
método) busca `repoItemEntrega.findByEstado(PENDIENTE)` y manda esos items al proveedor. El
callback construye `new Ruta(camion)` por cada asignación y la guarda **sin consultar si esos
items ya tienen una ruta** (`PlanificadorDeRutas:63`, `PlanificadorRutasService:83-97`).

El estado del ítem no cambia al crear la ruta: pasa a `EN_CAMINO` recién cuando el chofer la
inicia (`publicarInicioRuta`). Mientras la ruta siga `PROGRAMADA` y sin iniciar, sus items
siguen siendo `PENDIENTE`, y a la noche siguiente el cron los vuelve a planificar: otra
`Ruta` nueva con los mismos ítems y las mismas paradas. Cada noche suma una ruta más para las
mismas donaciones —el mismo ítem termina en dos camiones distintos—, y cada ejecución del
endpoint manual hace lo mismo. Las rutas anteriores quedan colgando en `PROGRAMADA` para
siempre, con chofer y camión tomados por `asignarChoferes`.

### Cómo se dispara

Crear una ruta por callback sin iniciarla y correr `POST /PlanificacionRutas/planificar-manual`
(o esperar al cron): aparece una segunda fila en `ruta` que contiene los mismos `idDonacion`
que la primera.

### Propuesta

Antes de mandar al proveedor, filtrar los items que ya pertenezcan a una ruta que no esté
`FINALIZADA`, o darles un estado `PLANIFICADO` al crear la ruta para que no vuelvan a entrar
en `findByEstado(PENDIENTE)`. La segunda opción además mata el síntoma de "el mismo ítem en
dos camiones".

---

## 27. `findByChofer` devuelve una ruta histórica: iniciar/terminar opera sobre la ruta equivocada

**Estado:** abierto
**Severidad:** alta
**Archivos:** `models/repositories/rutas/RepositorioRutas.java:21-27`, `services/RutaService.java:82-91,93-97`

### Qué pasa

```java
default Optional<Ruta> findByChofer(Chofer chofer){
    if (chofer == null) return Optional.empty();
    return this.findAll().stream()
            .filter(ruta -> ruta.getCamionAsignado() != null &&
                    chofer.equals(ruta.getCamionAsignado().getChofer()))
            .findFirst();
}
```

`findAll().stream()...findFirst()` devuelve la **primera** fila de la tabla (orden de
inserción) cuyo camión tenga ese chofer: no filtra por estado ni ordena, o sea que devuelve
la ruta más vieja.

Los dos únicos llamadores son `iniciarRuta` y `terminarRuta` (`RutaService:83,94`), que
interpretan el resultado como "la ruta del chofer". Con más de una ruta en la historia del
chofer —que es el caso normal—:

- `PATCH /rutas/chofer/{id}/iniciar` marca `EN_CURSO` una ruta **ya finalizada** y publica
  `INICIO_RUTA` con sus paradas viejas; la ruta recién planificada sigue `PROGRAMADA`.
- `PATCH /rutas/chofer/{id}/terminar` pone `FINALIZADA` sobre la ruta vieja, libera un chofer
  y un camión que quizá están en otra ruta, y hace el barrido de items sobre las paradas
  equivocadas.

### Cómo se dispara

Un chofer con una ruta `FINALIZADA` histórica y una `PROGRAMADA` recién creada →
`PATCH /rutas/chofer/{id}/iniciar` → la que pasa a `EN_CURSO` es la vieja (se ve con
`GET /rutas`).

### Propuesta

Filtrar por estado (`PROGRAMADA` para iniciar, `EN_CURSO` para terminar) o, mejor, recibir el
`idRuta` en el endpoint: el chofer no identifica una ruta.

---

## 29. Un PATCH/PUT sin el campo `disponible` aplica lo contrario: ocupa en silencio o revienta

**Estado:** abierto
**Severidad:** media
**Archivos:** `services/CamionService.java:75-90`, `services/ChoferService.java:48-52,66-72`, `dto/chofer/ChoferDTO.java:13`, `models/gestores/GestorCamiones.java:19-31`

### Qué pasa

Las tres vías que actualizan disponibilidad tratan "campo ausente" como "poner en ocupado":

- **`PATCH /camiones/{patente}/estado`**: `body.get("disponible")` devuelve `null` si el
  campo no viene → el `else` de la línea 83 ejecuta `camion.ocupado()` y responde
  **200 "Camión marcado como ocupado"**. La semántica de un PATCH es "no tocar lo que no se
  envía"; acá un body `{}` ocupa el camión.
- **`PATCH /choferes/{id}/estado`**: mismo patrón en `ChoferService:66-72`.
- **`PUT /choferes/{id}`**: `ChoferDTO.disponible` es primitiva `boolean` (línea 13), así
  que un JSON sin el campo se deserializa en `false` y la línea 52 hace
  `choferExistente.setDisponible(false)` → 200, chofer ocupado.
- **`PUT /camiones/{patente}`**: `CamionDTO.disponible` es `Boolean`, el `null` llega a
  `GestorCamiones.actualizarCamion:27` que hace `setDisponible(null)` sobre una columna
  `nullable = false` → `DataIntegrityViolationException`, que el controller no atrapa (solo
  catchea `IllegalArgumentException`) → **500**.

### Cómo se dispara

```bash
curl -X PATCH http://localhost:8086/api/camiones/ABC123/estado \
  -H "Content-Type: application/json" -d '{}'
# 200 "Camión marcado como ocupado" — sin haber pedido nada

curl -X PUT http://localhost:8086/api/choferes/<id> \
  -H "Content-Type: application/json" -d '{"nombre":"Juan"}'
# 200, y el chofer quedó disponible=false
```

### Propuesta

Distinguir "campo ausente" de "campo en false": en el PATCH, verificar
`body.containsKey("disponible")` y no tocar nada si no viene; en el PUT, usar `Boolean` en
`ChoferDTO` y no pisar el valor cuando es `null`.

---

## 30. `POST /entregas` responde 201 sin registrar nada cuando `bienes` o `idsDonaciones` vienen null

**Estado:** abierto
**Severidad:** media
**Archivos:** `services/EntregaService.java:119,131`, `controllers/EntregaController.java:62-72`

### Qué pasa

`procesarPeticion` tiene dos salidas silenciosas:

```java
if (request == null) return;                                  // línea 119
if (bienes == null || idsDonaciones == null) return;          // línea 131
```

Un `return` sin registrar nada y sin lanzar excepción hace que `EntregaController.crearItems`
responda **201 "Petición procesada exitosamente"**. La donación no se persiste, no se loguea,
no se descarta con warning: no pasó nada y el caller queda creyendo que sí.

Cuando el mismo endpoint lo invoca RabbitMQ (el flujo normal), el `return` además cuenta como
éxito: el mensaje se hace *ack* y se pierde para siempre — no hay reintentos ni DLQ porque
nadie falló.

### Cómo se dispara

```bash
curl -X POST http://localhost:8086/api/entregas -H "Content-Type: application/json" \
  -d '{"donacionResumen":{"idsDonaciones":null},"entidadBeneficiaria":{...}}'
# 201 "Petición procesada exitosamente mediante el proveedor: PROPIO"
# SELECT COUNT(*) FROM item_entrega -> sin cambios
```

### Propuesta

Reemplazar los `return` por `throw new IllegalArgumentException(...)`, igual que las demás
validaciones del método: el controller lo traduce en 400 y el listener lo descarta con
warning sin gastar reintentos, que es el comportamiento que ya existe para el resto de los
payloads inválidos.

---

## 31. El callback del simulador no tiene timeout y descarta la respuesta: un lote se pierde en silencio

**Estado:** abierto
**Severidad:** media
**Archivos:** `.../ProveedorRutasExternoSimulado.java:22,40-47`

### Qué pasa

Dos fallas en la misma llamada:

1. **Sin timeout.** `HttpClient.newHttpClient()` (línea 22) no define `connectTimeout` y el
   `HttpRequest` no define `.timeout()`. Si el callback no acepta la conexión o la respuesta
   nunca llega, `httpClient.send(...)` (línea 46) puede bloquear el hilo del
   `CompletableFuture.runAsync(...)` indefinidamente.
2. **El status se ignora.** `send(...)` devuelve la respuesta y el resultado se descarta. Si
   el callback responde 400 o 500 —payload que el propio controller rechaza—, el simulador
   no lo ve: no reintenta, no loguea, no avisa. El lote de rutas se planificó y nunca llegó,
   y los items quedan `PENDIENTE` sin que nadie se entere.

Todo el manejo de errores es un `System.err.println` dentro de una tarea asíncrona sin
supervisión: un fallo del lote no afecta al scheduler, que ya respondió 200.

### Cómo se dispara

Bajar el endpoint del callback (o poner cualquier cosa en el 8086 que responda 500) y
disparar `POST /PlanificacionRutas/planificar-manual`: el scheduler responde "Proceso de
planificación disparado", la simulación imprime su banner de error (o queda colgada si es
timeout) y ningún lote queda registrado.

### Propuesta

`HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))` y
`HttpRequest.newBuilder().timeout(Duration.ofSeconds(10))`; chequear
`respuesta.statusCode() != 200` y, ante cualquier fallo, reintentar o al menos dejar el lote
registrado en un log con nivel ERROR.

---

## 32. Toda violación de integridad se trata como carrera benigna: la donación se pierde sin DLQ

**Estado:** abierto
**Severidad:** media
**Archivos:** `RabbitMQ/DonacionListener.java:66-73`

### Qué pasa

```java
} catch (DataIntegrityViolationException yaRegistrada) {
    log.info("La donación ya fue registrada por otra instancia, mensaje descartado ...");
    return;
}
```

El `catch` asume que **toda** `DataIntegrityViolationException` es la carrera de clave
primaria entre dos instancias —y en ese caso descartar es correcto—, pero la misma excepción
la levantan el resto de las violaciones: dato fuera de rango, columna excedida, `NOT NULL`
violado, clave foránea rota. Esos casos **no** significan "ya registrada": significan "este
mensaje tiene datos inválidos", y acá se tragan igual. `return` (ack), un log a nivel `info`
con un mensaje que dice lo contrario de lo que pasó, y la donación se pierde sin DLQ ni registro
útil.

Es el espejo del punto 25: ahí un fallo que **debería** ir a la DLQ se reencola para siempre;
acá uno que **debería** ir a la DLQ se descarta como éxito.

### Cómo se dispara

Mandar un mensaje cuyo dato reviente una restricción de la base dentro de
`itemsEnUnaTransaccion` (por ejemplo una `cantidad` que la columna DECIMAL no admite): el
log muestra "La donación ya fue registrada por otra instancia" y el mensaje se acepta.

### Propuesta

Acotar el `catch` al caso que se quiere (clave primaria duplicada, que ya está cubierta por
la guarda de idempotencia `existsById`), y dejar que cualquier otra violación siga el camino
de reintento y termine en la DLQ con el error original visible.

---

## 34. `UnidadDeMedida` persiste constantes estáticas: cada reinicio duplica las filas

**Estado:** abierto
**Severidad:** media
**Archivos:** `models/entities/ItemEntrega/UnidadDeMedida.java:16-23`, `services/EntregaService.java:233-234`

### Qué pasa

`UNIDADES`, `KILOGRAMOS` y `LITROS` son objetos Java `static final` (líneas 16-18) con
`@GeneratedValue(strategy = GenerationType.UUID)`. En `itemsEnUnaTransaccion` se hace
`repoUnidades.save(unidadDominio)` (línea 234) con esa constante: la primera vez su
`idUnidad` es `null`, Hibernate inserta **una fila nueva** y le asigna un UUID.

Dentro de una misma JVM la constante sobrevive, así que los mensajes siguientes reutilizan la
fila. Pero:

- **Cada reinicio o redeploy** genera constantes nuevas con id `null` → otras 3 filas
  "Unidades", "Kilogramos", "Litros" en `unidad_medida`.
- **Cada una de las N instancias de logística** (el requisito del enunciado) tiene sus
  propias constantes → cada instancia inserta las suyas.

La tabla se llena de filas repetidas en proporción a los arranques, y dos instancias pueden
estar apuntando a filas distintas para "Kilogramos": el catálogo deja de ser único, que es
justamente lo que un catálogo tiene que ser.

### Cómo se dispara

Reiniciar el servicio después de haber procesado algunos mensajes y correr
`SELECT nombre, COUNT(*) FROM unidad_medida GROUP BY nombre` → `Kilogramos: 2, 3, ...`.

### Propuesta

Resolver la unidad por nombre (`findByNombre`) antes de insertar, o fijar el id de las tres
filas catálogo en el `schema.sql` de modo que el `save` siempre haga merge de la misma fila.
Mejor todavía: cargar el catálogo una sola vez al inicializar y que el servicio solo lea.

---

## 35. Cada mensaje inserta país, provincia, ciudad y dirección nuevos aunque la entidad ya exista

**Estado:** abierto
**Severidad:** media
**Archivos:** `services/EntregaService.java:173-204`

### Qué pasa

Es el residual del punto 15: la duplicación **por bien** se corrigió (`1e75220` movió el
catálogo fuera del `for`), pero dentro de `resolverEntidad` los `save` del catálogo siguen
corriendo **antes** de mirar si la entidad ya existe:

```java
repoPaises.save(direccion.getCiudad().getProvincia().getPais());   // 180
repoProvincias.save(direccion.getCiudad().getProvincia());         // 181
repoCiudades.save(direccion.getCiudad());                          // 182
repoDirecciones.save(direccion);                                   // 183
...
Optional<Entidad> yaExistente = repoEntidades.findById(idEntidad); // 187
if (yaExistente.isPresent()) return yaExistente.get();
```

`convertirDireccionDTO` construye objetos nuevos en cada mensaje (no hay ninguna búsqueda por
nombre), así que los cuatro `save` insertan filas nuevas **cada vez**, incluso cuando a la
línea 187 se devuelve la entidad existente: la dirección recién insertada queda huérfana.
`Pais`, `Provincia` y `Ciudad` usan `GenerationType.IDENTITY` y no tienen constraint único,
así que nadie protesta y la tabla `ciudad` crece lineal con la cantidad de mensajes.

### Cómo se dispara

Mandar dos mensajes para la misma entidad beneficiaria → `SELECT COUNT(*) FROM ciudad` sube
en 2, y la `Entidad` sigue apuntando a su primera dirección (la segunda queda sin usar).

### Propuesta

Buscar primero la entidad (línea 187) y solo construir el catálogo si no existe; o resolver
país/provincia/ciudad por `findByNombre` antes de cada `save`, que es lo que decía la
propuesta original del punto 15.

---

## 36. El callback devuelve 500 con internals para payloads que su contrato documenta como 400

**Estado:** abierto
**Severidad:** baja
**Archivos:** `controllers/PlanificadorDeRutasController.java:64-75`, `services/PlanificadorRutasService.java:69-77`

### Qué pasa

El `@ApiResponses` del endpoint documenta **400** para "JSON malformado o IDs inexistentes".
El JSON malformado efectivamente devuelve 400 (lo envuelve en `IllegalArgumentException`,
líneas 54-58), pero los IDs inexistentes no: `procesarCallbackRutas` busca los items dentro
de un `try` cuyo `catch (Exception e)` (línea 75) envuelve **todo** —incluida la propia
`IllegalArgumentException("Entrega no encontrada")` de la línea 73— en un
`RuntimeException("Falla en la base de datos al recuperar información para el ruteo")`. Ese
`RuntimeException` no es `IllegalArgumentException`, así que en el controller cae en el
`catch (Exception)` → **500**, y el body filtra lo interno:
`"Error interno del servidor: Faila en la base de datos al recuperar información para el
ruteo"`.

Además, el 500 de la última línea del controller concatena `e.getMessage()` en la respuesta:
información interna del servicio hacia el cliente.

### Cómo se dispara

Callback con un `idDonacion` que no existe en la base → 500 en vez del 400 documentado.

### Propuesta

No envolver las `IllegalArgumentException` de negocio en el `catch` de BD (o relanzarlas tal
cuál), y dejar de concatenar `e.getMessage()` en la respuesta del 500.

---

## 37. El CRUD de camiones/choferes devuelve 500 para errores de validación

**Estado:** abierto
**Severidad:** baja
**Archivos:** `controllers/CamionController.java:47-59`, `controllers/ChoferController.java:51-58`, `services/CamionService.java:43-52`, `services/ChoferService.java`

### Qué pasa

`POST /camiones` y `POST /choferes` no tienen `try/catch` ni `@Valid`, y los PUT/PATCH solo
atrapan `IllegalArgumentException`. Cualquier otra excepción de validación o de la base se
escapa y responde **500**:

- `POST /camiones` sin los campos obligatorios (`capacidad_volumen_m3`, `altura_m`,
  `capacidad_carga_kg` son `nullable = false`) → `DataIntegrityViolationException` → 500.
- `POST /choferes` sin `nombre` (`nullable = false`) → 500.
- `PUT` con campos null sobre columnas `NOT NULL` → 500 (mismo camino que el punto 29).

No hay forma de distinguir "dato inválido" (400) de "conflicto" (409) de "no existe" (404):
lo que no entra en el `catch` de `IllegalArgumentException` es 500, y el cliente se queda sin
saber qué corregir.

### Cómo se dispara

```bash
curl -X POST http://localhost:8086/api/camiones -H "Content-Type: application/json" -d '{}'
# 500
```

### Propuesta

`@Valid` con anotaciones en los DTO, y un `@ControllerAdvice` que traduzca
`DataIntegrityViolationException` en 409/400 con un mensaje entendible, en lugar de que cada
controlador decida con su propio `try`.

---

## 38. Los repositorios de país/provincia/ciudad declaran ID `UUID` y la entidad tiene `Long`

**Estado:** abierto
**Severidad:** baja
**Archivos:** `models/repositories/RepositorioPaises.java:10`, `RepositorioProvincias.java:10`, `RepositorioCiudades.java:10` vs. `models/entities/Direccion/Pais.java:18`, `Provincia.java:17`, `Ciudad.java:17`

### Qué pasa

```java
public interface RepositorioPaises extends JpaRepository<Pais, UUID> { }
// pero en la entidad:
private Long idPais;   // @GeneratedValue(strategy = IDENTITY)
```

El tipo de id del repositorio (`UUID`) no coincide con el tipo real de la clave (`Long`) en
los tres. Hoy no se nota porque a esos repos **solo se les llama `save`**
(`EntregaService:180-182`) y `save` no depende del tipo de id. Pero cualquier `findById(...)`,
`existsById(...)` o `deleteById(...)` con el tipo declarado revientaría al bindear un `UUID`
contra una columna `BIGINT`. Es deuda latente: compila, arranca, y falla recién en el primer
uso.

### Cómo se dispara

Agregar en cualquier punto `repoPaises.findById(algunUUID)` y ejecutarlo: excepción de
bindeo de Hibernate en lugar de un `Optional` vacío.

### Propuesta

Cambiar los tres a `JpaRepository<Pais, Long>` (y `Provincia`, `Ciudad`), que es lo que la
entidad declara. Es una línea por archivo.

---

## 40. Los gestores están por entidad y mezclan reglas con persistencia

**Estado:** abierto
**Severidad:** baja
**Archivos:** `models/gestores/GestorCamiones.java`, `models/gestores/GestorPublicacionEventos.java`, `services/CamionService.java`, `services/ChoferService.java`

### Qué pasa

`incentivos-service` nombra sus gestores por la regla de negocio que encapsulan y sin prefijo
—`SecuenciaCategoria`, `SincronizacionPerfiles`, `ValidadorAdmin`—, y en `SecuenciaCategoria` el
repositorio **entra por parámetro** en vez de inyectarse: es una política sin estado, no un
servicio con dependencias.

Logística hace lo contrario:

- `GestorCamiones` está nombrado por la entidad, y adentro mezcla dos comportamientos: el
  `actualizarCamion` que persiste, y un `resetearCamion` que está bajo un `// --- MAPPERS ---`
  aunque también escribe en la base.
- `GestorPublicacionEventos` sí está nombrado por comportamiento, pero conserva el prefijo.
- `ChoferService` no tiene gestor: hace la regla y la persistencia él mismo.
- `CamionService.cambiarDisponibilidad` persiste en el service, mientras que `update` delega en
  el gestor. La misma capa hace las dos cosas según el método.

No hay una capa donde buscar una regla de negocio: está repartida entre services y gestores sin
un criterio que diga cuál va dónde.

### Propuesta

Nombrar los gestores por comportamiento y sin prefijo, y que cada uno encapsule **una** regla.
`GestorCamiones` se parte según sus dos comportamientos (actualizar el camión y resetear su
carga), con el estilo de `SecuenciaCategoria`: repositorio por parámetro cuando la regla no
necesita estado, y javadoc que explique la regla y sus excepciones. La capa queda
controller → service (orquesta y mapea a DTO) → gestor (regla) → repositorio.

---

## 41. Los repositorios conviven en dos esquemas distintos

**Estado:** abierto
**Severidad:** baja
**Archivos:** `models/repositories/` (12 archivos)

### Qué pasa

Los doce repositorios están repartidos con dos criterios a la vez:

- Cinco en subpaquetes por entidad: `camiones/RepositorioCamiones`,
  `choferes/RepositorioChoferes`, `items/RepositorioItemEntrega`, `rutas/RepositorioRutas`,
  `eventos/RepositorioEventoLogistica`.
- Siete sueltos en la raíz de `repositories/`: `RepositorioCiudades`, `RepositorioDirecciones`,
  `RepositorioEntidades`, `RepositorioPaises`, `RepositorioParadas`, `RepositorioProvincias`,
  `RepositorioUnidadesDeMedida`.

Nada distingue a los primeros de los segundos: los doce son interfaces de Spring Data.
`incentivos-service` usa un criterio explícito: los Spring Data en
`models/repositories/SpringRepositories/`, y en la raíz de `repositories/` solo lo que no es
Spring Data (su cola en memoria `RepositorioNotificacionesPendientes`).

### Propuesta

Mover los doce a `models/repositories/SpringRepositories/`. Si se prefieren los subpaquetes por
entidad, el criterio tiene que aplicarse a los doce y no a cinco.

---

## 42. Paquetes de primer nivel y ubicación de clientes, scheduler y eventos

**Estado:** abierto
**Severidad:** baja
**Archivos:** `RabbitMQ/`, `Scheduler/`, `messaging/`, `models/entities/PlanificadorDeRutas/ProveedorRutasExterno/`, `dto/`

### Qué pasa

Comparado con `incentivos-service`:

| Aspecto | incentivos | logística |
|---|---|---|
| Comunicación saliente | `clients/` (`DonacionClient`, `N8nClient`, `NotificacionClient`) | `messaging/ProductorEventosLogistica` y el proveedor HTTP anidado en `models/entities/PlanificadorDeRutas/ProveedorRutasExterno/` |
| Comunicación entrante | no tiene listeners propios | `RabbitMQ/` como paquete de primer nivel y capitalizado |
| Procesos internos | `models/ServiciosInternos/` y `.../scheduler/` | `Scheduler/` de primer nivel y capitalizado |
| Eventos | `models/events/` con records | `models/entities/EventoLogistica` (entidad) |
| DTOs | subpaquetes en PascalCase, filtros en `controllers/request/` | subpaquetes en minúscula (`camion`, `chofer`, `entrega`, `evento`, `rutas`) |

El caso más visible es el proveedor externo: un cliente HTTP —`ProveedorRutasExterno`,
`ProveedorRutasExternoHttp`, `ProveedorRutasExternoSimulado`— vive dentro de las entidades del
dominio, entre `PlanificadorDeRutas` y `Reglas`.

### Propuesta

Unificar con el criterio de incentivos: `clients/` para lo que sale del servicio (el proveedor
externo y el publicador de eventos), `config/` para el `RabbitMQConfig`, y los procesos internos
bajo `models/ServiciosInternos/` con su `scheduler/`. Los DTO en PascalCase y los filtros de
listado en `controllers/request/`.

---

## 43. El manejo de errores está repetido en cada controller

**Estado:** abierto
**Severidad:** media
**Archivos:** los 6 controllers de `controllers/`

### Qué pasa

No hay `exceptions/` ni `@ExceptionHandler`: cada controller atrapa lo que le interesa con
`try/catch` y decide el código. Como la decisión está repetida, el **mismo error sale con
códigos distintos según dónde se atrape**:

- `IllegalArgumentException` es **404** en `CamionController`, en `ChoferController` y en los
  DELETE de `EntregaController`; **400** en `EntregaController.actualizarEstadoEntrega` y en
  `PlanificadorDeRutasController`; y dentro del propio `RutaController` es 404 en
  `obtenerPorId`, `update` y `eliminarRuta`, pero 400 en `iniciarRuta` y `terminarRuta`.
- `IllegalStateException` es **400** en `EntregaController` y **422** en
  `PlanificadorDeRutasController`.

Y hay un caso más, del otro lado: `EntregaController.crearItems` atrapa `Exception` y responde
**400** con `e.getMessage()`, así que una caída de la base se le informa al cliente como un
error de la petición, con el texto interno adentro.

`incentivos-service` tiene `exceptions/` con excepciones tipadas (`InexistenteException`,
`ConflictoException`, `DatosInvalidosException`, `PerfilExistenteException`,
`CategoriaBaseInexistenteException`, `EnvioNotificacionException`) y un `GlobalExceptionHandler`
que las mapea a códigos en un solo lugar. Los puntos 36 y 37 de este backlog son dos síntomas
concretos de esta misma causa.

### Propuesta

Crear `exceptions/` con excepciones tipadas por significado (no encontrado, datos inválidos,
conflicto) y un `GlobalExceptionHandler`, y sacar los `try/catch` de los controllers.

---

## 44. Los listados devuelven la tabla entera: falta paginación

**Estado:** abierto
**Severidad:** media
**Archivos:** `controllers/CamionController.java`, `controllers/ChoferController.java`, `controllers/EntregaController.java`, `controllers/RutaController.java`

### Qué pasa

Los listados no paginan: devuelven todas las filas en una sola respuesta.

| Endpoint | Devuelve | Repositorio |
|---|---|---|
| `GET /api/camiones` | `CamionesDTO` con todos | `findAll()` |
| `GET /api/choferes` | `ChoferesDTO` con todos | `findAll()` |
| `GET /api/entregas` | `BienesDTO` con todos | `findAll()` |
| `GET /api/entregas/no-recibidas` | `BienesDTO` con todos | `findByEstado(...)` |
| `GET /api/rutas` | `RutasDTO` con todas | `findAll()` |

`incentivos-service` usa la convención de Spring Data en sus listados:

```java
@GetMapping
public ResponseEntity<Page<CategoriaDTO>> obtenerCategorias(
        @ParameterObject @ModelAttribute CategoriaFiltroRequest filtros,
        @ParameterObject
        @PageableDefault(page = 0, size = 10, sort = "posicionSecuencia", direction = Sort.Direction.ASC)
        Pageable pageable) {
    return ResponseEntity.ok(service.obtenerCategorias(filtros, pageable));
}
```

con los filtros como records en `controllers/request/`.

**`GET /api/eventos` no entra en esto**: es polling por cursor (`desdeId`), el consumidor
avanza con el id que ya procesó y ya lo tiene acotado. Paginarlo rompería ese contrato.

### Propuesta

Pasar los cinco listados a `Page<T>` con `Pageable` y `@PageableDefault`, siguiendo la
convención de incentivos, y agregar filtros en `controllers/request/` donde tenga sentido
(por ejemplo `disponible` en camiones y `estado` en entregas).

Ojo con el contrato: hoy devuelven envoltorios propios (`CamionesDTO`, `ChoferesDTO`,
`RutasDTO`, y `BienesDTO` con dos listas paralelas, ids y bienes). Paginar cambia la forma de
la respuesta, así que es un cambio de contrato para el front.

---
## 45. El callback del proveedor externo está fijado a `localhost:8086`

**Estado:** abierto
**Severidad:** baja
**Archivos:** `models/entities/PlanificadorDeRutas/ProveedorRutasExterno/ProveedorRutasExternoSimulado.java:23`, `.../ProveedorRutasExternoHttp.java`, `config/LogisticaConfig.java`

### Qué pasa

El enunciado pide la planificación "utilizando una URL de callback para procesar los resultados".
El callback existe (`POST /api/PlanificacionRutas/callback`) y el simulador le pega, pero la URL
está escrita como constante:

```java
private final String URL_CALLBACK_LOCAL = "http://localhost:8086/api/PlanificacionRutas/callback";
```

Dos consecuencias:

- **Ignora el puerto configurable.** El servicio escucha en `${SERVER_PORT:8086}`; la URL no. Si
  corre en otro puerto, el callback pega en el vacío y el lote se pierde en silencio (el `send`
  no tiene timeout ni revisa la respuesta: punto 31).
- **No sirve para un proveedor externo real.** El simulador corre en el mismo proceso, así que
  `localhost` le funciona. Un componente en otra máquina necesita la URL accesible del servicio,
  y hoy nadie se la comunica: `ProveedorRutasExternoHttp` manda `donaciones` y `camiones` y
  ninguna URL de callback. Además nadie lo instancia: `LogisticaConfig` cablea siempre el
  simulador, así que tampoco hay forma de configurar el proveedor.

### Propuesta

Sacar la URL a una property (por ejemplo `logistica.callback-url`, con default
`http://localhost:${server.port}` para el simulador) y, si se va a soportar un proveedor externo
real, incluirla en el pedido o dejarla configurada del lado del proveedor. El proveedor debería
elegirse por configuración en `LogisticaConfig`, no por una constante.

---
## 46. Los lotes comparten la lista de camiones: un camión queda en dos rutas del mismo día

**Estado:** abierto
**Severidad:** alta
**Archivos:** `Scheduler/PlanificadorDeRutasScheduler.java:59-63`, `models/entities/PlanificadorDeRutas/PlanificadorDeRutas.java:28-36`, `models/entities/PlanificadorDeRutas/ProveedorRutasExterno/ProveedorRutasExternoSimulado.java:55-72`, `services/PlanificadorRutasService.java:77-93`

### Qué pasa

El scheduler parte los pendientes en lotes de 100 por el límite del proveedor, pero **pasa la
misma lista de camiones a todos los lotes**:

```java
for (int i = 0; i < itemsPendientes.size(); i += 100) {
  List<ItemEntrega> lote = itemsPendientes.subList(i, Math.min(i + 100, itemsPendientes.size()));
  planificadorDominio.iniciarPlanificacion(lote, camionesDisponibles);   // la misma lista
}
```

Cada lote viaja al proveedor por separado, y el simulador arranca con
`camion.resetearCargaOcupada()` sobre esos mismos `Camion`: la carga del lote anterior se
descarta y cada lote valida capacidad **solo contra sus propios ítems**. El callback, después,
crea una `Ruta` por cada patente que venga en la asignación sin mirar si ese camión ya tiene una
ruta para la misma fecha.

Con más de 100 pendientes —el caso para el que existe el loteo— un mismo camión puede aparecer
en dos asignaciones y termina con **dos rutas para el mismo día**, cada una validada por
separado. La suma no la valida nadie: la ruta planificada puede superar la capacidad del camión.

No hace falta que los lotes se solapen en el tiempo: la ocupación es estado en memoria y no se
persiste (punto 17), así que ni el segundo lote ve lo que cargó el primero.

### Cómo se dispara

Cargar 101 pendientes o más y correr `POST /PlanificacionRutas/planificar-manual`: salen dos
filas en `ruta` con la misma `patente_camion` y la misma `fecha_programada`, y la carga sumada
de las dos puede superar `capacidad_carga_kg`.

### Propuesta

Lotear también los camiones: que cada camión viaje en un solo lote, o que el callback vuelva a
consultar los disponibles descontando los que ya quedaron en rutas del día. Lo segundo cubre
además el caso de dos instancias.

---

## 47. Dos callbacks concurrentes eligen el mismo chofer

**Estado:** abierto
**Severidad:** alta
**Archivos:** `services/PlanificadorRutasService.java:104-152`, `controllers/PlanificadorDeRutasController.java:56-62`

### Qué pasa

`asignarChoferes` lee los disponibles, elige uno al azar y guarda en pasos separados:

```java
List<Chofer> choferesDisponibles = new ArrayList<>(repoChoferes.findAll().stream()
        .filter(Chofer::isDisponible).toList());          // 1. lee
...
Chofer choferElegido = choferesDisponibles.get(random.nextInt(choferesDisponibles.size()));
camion.setChofer(choferElegido);
camion.ocupado(); choferElegido.ocupado();
repoCamiones.save(camion);          // 2. escribe el camion, commit propio
repoChoferes.save(choferElegido);   // 3. escribe el chofer, commit propio
```

No hay transacción que envuelva el método ni lock sobre los choferes, y el callback es un
endpoint HTTP: el proveedor puede tener dos pedidos en vuelo. Eso pasa de forma normal, porque
**el scheduler manda un lote por cada 100 pendientes y las respuestas vuelven casi juntas** (el
simulador duerme 2 s y postea), sin contar el requisito de "más de una instancia".

Con dos callbacks concurrentes:

1. Los dos leen la misma lista de disponibles.
2. Los dos eligen el mismo chofer X.
3. Los dos guardan su camión con `id_chofer = X`: esos `save` van en transacciones separadas y
   commitean.
4. El primer `save` del chofer sube la `version`; el segundo choca con el `@Version`, tira
   `OptimisticLockingFailureException` y corta el método por la mitad.

Queda un chofer asignado a **dos camiones** —o sea, dos rutas del mismo día con el mismo
chofer— más un 500 en el callback, que descarta el resto de la asignación de ese lote.

### Cómo se dispara

Dos callbacks que lleguen juntos (más de 100 pendientes, o dos instancias de logística): el
chofer que eligen ambos queda en dos rutas. Con una sola instancia y menos de 100 pendientes no
se ve.

### Propuesta

Elegir el chofer y el camión dentro de una misma transacción, con el chofer tomado por lock (o
un `UPDATE ... WHERE disponible = true` y quedarse con el que ganó), y que la asignación del
lote sea atómica. La alternativa es no asignar el chofer en el callback sino al iniciar la
ruta, que es donde importa.

---

## 48. Un fallo del broker al publicar después del commit deja el 500 y pierde la notificación

**Estado:** abierto
**Severidad:** media
**Archivos:** `models/gestores/GestorPublicacionEventos.java:176-195`, `messaging/ProductorEventosLogistica.java:43-52`

### Qué pasa

El arreglo del punto 28 movió la publicación a `afterCommit` para que un rollback no deje el
evento afuera. El borde que quedó es el otro: `productorEventos.publicar(evento)` corre en el
`afterCommit` de la sincronización, y si el broker no responde —caído, reiniciando, un corte de
red— la excepción sale por el interceptor de la transacción, que **ya commiteó**.

En un `PATCH /entregas/{id}/estado` con el broker fallando en ese instante:

- La entrega queda `ENTREGADA` en la base: el commit pasó.
- El controller cae en su `catch (Exception)` y devuelve **500 con el mensaje interno**.
- El evento quedó persistido (`repoEventos.save` corre antes), pero **no salió al broker**:
  `donaciones-service` no se entera y no hay notificación.

El cliente ve un 500 y el sistema quedó con la operación aplicada. Un reintento —el reflejo
normal ante un 500— no republica el evento: encuentra el ítem ya en `ENTREGADA` y
`publicarEntregaConfirmada` no hace nada (punto 8). La notificación se recupera recién por el
camino de polling (`GET /api/eventos`), si alguien lo consulta.

### Cómo se dispara

Bajar el broker, o cortarle la red al servicio, justo antes de un
`PATCH /entregas/{id}/estado`: la respuesta es 500 y en el broker no hay mensaje.

### Propuesta

Atrapar la excepción dentro del `afterCommit` y loguearla, en vez de dejarla propagar: el evento
ya está en la base y el polling lo cubre, así que el 500 solo agrega confusión. Con garantía de
entrega, un outbox con reintentos.

---

## 49. Cada cambio de estado de una entrega recorre todas las rutas con sus paradas e ítems

**Estado:** abierto
**Severidad:** baja
**Archivos:** `models/repositories/rutas/RepositorioRutas.java:14-19`, `services/EntregaService.java:262,271`

### Qué pasa

```java
default Optional<Ruta> findByIdDonacion(UUID idDonacion){
    return this.findAll().stream()
            .filter(ruta -> ruta.obtenerTodosLosItems().stream()
                    .anyMatch(item -> item.getIdDonacion().equals(idDonacion)))
            .findFirst();
}
```

`findAll()` trae **todas** las rutas de la historia y `obtenerTodosLosItems()` recorre las
paradas y los ítems de cada una, que siendo colecciones lazy son consultas adicionales (N+1).
El método se llama en los dos caminos del endpoint con el que las entidades confirman o
rechazan una entrega (`EntregaService:262` y `:271`), así que el costo crece con el historial
completo de ruteo y lo paga la operación más frecuente del servicio.

Hoy, con pocos datos, no se nota: es deuda de escalabilidad, no un fallo funcional.

### Propuesta

Invertir la consulta: `ItemEntrega` ya tiene la FK a su parada (`item.getParada().getRuta()`), y
`actualizarEstado` ya cargó el ítem, así que no hace falta buscar la ruta desde las rutas. La
alternativa es una derived query que navegue `Ruta` → `paradas` → `items` por `idDonacion`.

---

## Corregidos

Un bullet por fix. El número es el ID del punto que estaba abierto; los bullets sin número
nunca fueron un punto abierto. Fechas: 1, 15 y 16 salieron en `1e75220` (2026-10-07); 5, 6,
11–14, 25, 28, 33, 39 y el sondeo se corrigieron el 2026-10-07 sin commit; los sin número no
tienen fecha registrada. Los 21 a 24 salieron juntos: son lo que hace falta para que N instancias
compartan la cola y la base.

- **1** — El módulo no tenía un solo test → llegaron los primeros, con los casos de N instancias.
- **5** — Los tres `DELETE` borraban y después tiraban "no encontrado", así que devolvían 404
  tras un borrado exitoso → se invirtió el chequeo: 404 solo cuando el recurso no estaba.
- **6** — `ItemEntrega.eventos` usaba `mappedBy="id"`, que apunta a la PK del evento → FK real
  (`@ManyToOne item`, `mappedBy="item"`); el cascade se conservó para poder borrar un ítem con
  historial.
- **11** — Una misma instancia de evento se metía en la lista de todos los ítems de la ruta →
  un solo `INICIO_RUTA` por ruta, sin ítem, y sin tocar las listas a mano.
- **12** — El polling pedía `id > desdeId - 1` (reenviaba el último evento ya procesado) y
  reventaba con `desdeId` null → se pasa el id tal cual y null vale 0.
- **13** — `findByIdGreaterThanOrderByIdAsc` era un `default` con `findAll()` y sin `ORDER BY` →
  derived query real.
- **14** — `GET /entregas/{id}` devolvía la entidad JPA (ciclo de Jackson y esquema interno) →
  devuelve `BienDTO`, y `ItemEntrega.parada` quedó con `@JsonIgnore` como red.
- **15** — Una donación con N bienes creaba N direcciones y las huérfanas quedaban colgando →
  el catálogo se resuelve una vez por mensaje.
- **16** — `procesarPeticion` validaba solo `request` y el resto reventaba a mitad de la
  escritura → validaciones antes de tocar la base.
- **19** — La validación de la justificación de una entrega fallida → **corregido**: el punto
  describía una inversión que no existía. Verificado contra el código, `comprobarExistencia`
  devuelve `true` cuando el texto falta, así que los dos call-sites ya hacían lo correcto y no
  hubo cambios (estaba bien desde `cb8910a`).
- **21** — Una redelivery reseteaba el ítem a `PENDIENTE`: no había idempotencia → guarda
  `existsById` antes de registrar, `DataIntegrityViolationException` como carrera benigna, y el
  estado solo lo cambia el operador.
- **22** — Ninguna entidad tenía `@Version` y dos instancias se pisaban en silencio → `@Version`
  en las 12 entidades.
- **23** — Con N consumidores no hay orden entre mensajes → el reparto por hash quedó
  implementado y **apagado a propósito**: la cola compartida da disponibilidad y hoy ningún
  mensaje lleva transición de estado.
- **24** — El compose no se podía escalar (`container_name` fijo y `8086:8086`) → se quitó el
  nombre fijo, el puerto se publica efímero y logística lee `SERVER_PORT`.
- **25** — Un mensaje que agotaba los reintentos se reencolaba para siempre y trababa la cola →
  `AmqpRejectAndDontRequeueException` más `default-requeue-rejected=false`, que es lo que activa
  la DLQ.
- **28** — El evento se publicaba dentro de la transacción y un rollback lo dejaba afuera → se
  publica en `afterCommit`, y `iniciarRuta` pasó a ser `@Transactional`.
- **33** — La credencial de la base estaba como default en el código y en claro en el compose →
  un `.env` por módulo, sin defaults en `application.properties`.
- **39** — El cron corría a las 02:00 UTC (23:00 de Argentina) → `zone` explícita en
  `@Scheduled`.
- Marcadores de merge sin resolver en `GestorPublicacionEventos`: el módulo no compilaba → quedó
  la versión que consumen los services.
- `RepositorioCamiones` y `RepositorioChoferes` duplicados en dos paquetes → se consolidaron en
  los subpaquetes y se borraron los del paquete plano.
- Cinco `DataSourceConfig` apuntaban a cinco bases distintas con credenciales hardcodeadas → se
  borraron: un solo `DataSource` sobre `logisticas`.
- El binding de la cola de sondeo apuntaba al exchange equivocado → corregido y después
  eliminado junto con el sondeo.
- `SolicitudEventosListener` hacía request/response por cola → dejó de responder por el broker, y
  después se eliminó.
- `DonacionListener` se tragaba todos los errores → los de negocio se descartan con warning y el
  resto se relanza para que vaya a la DLQ.
- El sondeo de trazabilidad por cola era un no-op: nadie publicaba la respuesta y el cursor nunca
  avanzaba → se eliminó la cadena completa y la trazabilidad queda por `GET /api/eventos`.
  (Venía numerado 40, que ya es el punto abierto de gestores: quedó sin número, como los otros
  fixes que nunca fueron un punto abierto.)
