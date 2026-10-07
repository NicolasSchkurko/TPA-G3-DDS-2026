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
| 2 | 2 | Iniciar o terminar una ruta nunca persiste el estado: la operación responde OK y no pasa nada |
| 3 | 4 | La condición del camión está invertida: el caso normal responde "Camión no encontrado" |
| 6 | 26 | Replanificar crea rutas duplicadas para los mismos ítems, cada noche y en cada manual |
| 8 | 27 | `findByChofer` devuelve una ruta histórica: iniciar/terminar opera sobre la ruta equivocada |
| 9 | 8 | Confirmar una entrega que no está en camino se ignora en silencio y responde 200 |
| 10 | 9 | Reportar una entrega fallida no valida el estado previo: una entrega_ok se puede revertir |
| 11 | 10 | El reingreso a depósito no valida nada y su comentario cita un método que no existe |
| 13 | 29 | Un PATCH/PUT sin el campo `disponible` aplica lo contrario: ocupa en silencio o revienta |
| 14 | 30 | `POST /entregas` responde 201 sin registrar nada cuando `bienes` o `idsDonaciones` vienen null |
| 15 | 32 | Toda violación de integridad se trata como carrera benigna: la donación se pierde sin DLQ |
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

## Corregidos

### 1. El módulo no tiene un solo test

**Estado:** corregido el 2026-10-07 en `1e75220`
**Severidad:** alta
**Archivos:** `logisticas-service/src/test/`

`logisticas-service/src/test` no existía: el módulo era el único del proyecto sin cobertura
y justamente el que recibió un merge sin resolver. Con `1e75220` llegaron los primeros tests,
y no son decorativos: `EntregaServiceIdempotenciaTest` tiene 8 tests que miden la idempotencia
del registro de donaciones, y se verificó que tienen dientes (neutralizando la guarda a
propósito, 3 de 8 fallaron). También hay tests que levantan el contexto, que es la red que
falta contra un JPQL mal formado o una entidad fuera de la unidad de persistencia.

Lo que queda —cobertura de dominio, de la API y de los planificadores— es una mejora
continua, no el punto crítico que era: ya existe al menos una red que corre en cada build.

### 15. Registrar una donación duplica la dirección N veces

**Estado:** corregido el 2026-10-07 en `1e75220`
**Severidad:** media
**Archivos:** `.../services/EntregaService.java`

El `for` de bienes construía la dirección adentro: con una donación de 3 bienes quedaban 3
países, 3 provincias, 3 ciudades y 3 direcciones con el mismo contenido, y el `save` de
`Entidad` (clave natural) terminaba apuntando a la última dirección, dejando las otras dos
huérfanas.

**Qué se resolvió:** el catálogo salió del `for`: ahora `procesarPeticion` resuelve la
entidad una sola vez por mensaje, antes de registrar los items. Junto con el `@Version` de
esas entidades, dos instancias que lleguen a la vez reciben `OptimisticLockingFailureException`
en vez de pisarse. Hay test que lo mide.

**Residual:** la duplicación **por mensaje** sigue abierta y quedó registrada como el
punto 35: los `save` de país/provincia/ciudad corren antes de buscar si la entidad ya existe.

### 16. `procesarPeticion` no valida el payload

**Estado:** corregido el 2026-10-07 en `1e75220`
**Severidad:** media
**Archivos:** `.../services/EntregaService.java`, `.../RabbitMQ/DonacionListener.java`

El método validaba `request` y nada más: `donacionResumen` null, `entidadBeneficiaria` null,
listas de longitudes distintas o `null` revientaban con NPE/IndexOutOfBounds a mitad de la
escritura, y la base quedaba a medias.

**Qué se resolvió:** validaciones arriba, antes de tocar la base: resumen no nulo, entidad no
nula, `bienes`/`idsDonaciones` no nulos y de la misma longitud, unidad no nula y soportada, y
dirección no nula en `resolverEntidad`. Todas lanzan `IllegalArgumentException`, que el
controller traduce en 400 y el listener descarta con warning sin gastar reintentos.

### 19. La validación de la justificación de una entrega fallida está invertida

**Estado:** cerrado el 2026-10-07 como **falso positivo** (verificado contra el código actual)
**Severidad:** crítica (declarada)
**Archivo:** `.../services/EntregaService.java:334-336`

El punto sostenía que `comprobarExistencia` devuelve `true` cuando el texto **sí** está
escrito, con lo cual el `case "NO_RECIBIDA"` rechazaría las justificadas y aceptaría las que
faltan. Verificado contra el código, la premisa es falsa:

```java
private boolean comprobarExistencia(String elemento){
    return (elemento == null || elemento.trim().isEmpty());
}
```

Devuelve `true` cuando el texto **falta**, o sea que
`if (comprobarExistencia(request.getJustificacion())) throw "Se requiere justificar..."` lanza
exactamente cuando no hay justificación, que es lo que dice el mensaje. El caso `ENTREGADA`
(línea 298, foto) usa el mismo patrón con el mismo sentido y también es correcto.

El helper tiene un nombre engañoso —comprobarExistencia devuelve true cuando **no** existe—,
pero la lógica de los dos call-sites es la correcta y no se tocó ningún código. Comportamiento
ya correcto desde `cb8910a`.

---

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
`LogisticaPollingScheduler`. (Tanto ese binding como el `LogisticaPollingScheduler` se eliminaron
después, cuando se quitó el sondeo: ver la última entrada de `Corregidos`.)

### `SolicitudEventosListener` hacía request/response por cola

**Estado:** corregido
**Severidad:** media
**Archivo:** `.../RabbitMQ/SolicitudEventosListener.java`

Publicaba la respuesta en el exchange para que volviera a la cola del que preguntó. Eso convierte
el broker en un request/response: necesita dos colas y dos bindings por consumidor, y se rompe
entero si el que preguntó se cae antes de leer la respuesta.

**Qué se cambió:** el listener deja de publicar la respuesta. La trazabilidad queda disponible
por HTTP en `GET /api/eventos`, que es lo que pide el enunciado al describir el despliegue de
logística como accesible por sus URIs. El polling quedó como red de contención, pero se eliminó
después: sin respuesta no recuperaba nada (ver la última entrada de `Corregidos`).

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

# Corregidos

Estos cuatro son una sola cosa mirada desde cuatro ángulos: **qué hace falta para que N
instancias de logística puedan compartir la cola y la base**, que es lo que pide el enunciado
cuando dice *"más de 1 servicio de logística disponible"*. Se corrigieron juntos, y los tests
que los cubren son los primeros que tiene este módulo.

La regla de fondo, que no es obvia: **lo único que garantiza contra la duplicación es la
idempotencia; la base compartida evita que las instancias divergan; y el `@Version` convierte
un pisado silencioso en un error visible.** Con las tres, el sistema tolera N instancias sin
duplicar entregas ni perder estado.

---

### 21. Una redelivery reseteaba el ítem a `PENDIENTE`: no había idempotencia

**Estado:** corregido
**Severidad:** crítica
**Archivos:** `.../services/EntregaService.java`, `.../RabbitMQ/DonacionListener.java`

#### Qué pasaba

`procesarPeticion` no tenía ninguna guarda de idempotencia. Peor todavía: una redelivery **no
fallaba**.

El detalle está en la clave primaria. `ItemEntrega` declara:

```java
@Id
@Column(name = "id_donacion", nullable = false, updatable = false)
private UUID idDonacion;
```

`idDonacion` viene del mensaje y **no tiene `@GeneratedValue`**. Con id no nulo, el `isNew` de
Spring Data da `false` siempre y `save()` va **siempre por `merge()`**: hace `SELECT` y, si la
fila existe, hace `UPDATE`.

Ese `UPDATE` reescribía la fila con lo del constructor, que pone:

```java
this.estado = EstadoEntrega.PENDIENTE;
this.fechaCambioEstado = LocalDateTime.now();
```

O sea que si un ítem ya estaba entregado y el mensaje se reprocesaba, volvía a `PENDIENTE` y
perdía la foto del comprobante. Sin error, sin log, sin rastro. Es peor que una duplicación,
porque una duplicación se ve.

#### Qué se hizo

**1. Guarda de idempotencia explícita**, antes de construir nada:

```java
if (repoItemEntrega.existsById(idDonacion)) {
    repetidos++;
    log.info("La donación {} ya estaba registrada, el mensaje se repite y se omite", idDonacion);
    continue;
}
```

Es `continue` y no `return` a propósito: una donación puede traer bienes ya registrados **junto
con** bienes nuevos, y con un `return` se perderían los nuevos. Hay un test para eso.

**2. `DataIntegrityViolationException` como resultado benigno en el listener.** La guarda cubre
el caso secuencial, pero queda una ventana: dos instancias leen el mismo `idDonacion` antes de
que ninguna lo escriba, las dos pasan el `existsById` y las dos intentan insertar. Gana una; la
otra recibe la violación de clave primaria, **que es el estado final que se buscaba**.

Si se relanzara, el mensaje iría a la dead letter queue como si fuera un fallo y además frenaría
la cola compartida de la que salen las N instancias. Se registra y se sigue.

**3. El estado solo lo cambia el operador, nunca el registro.** Es la separación que vuelve
inofensiva una repetición por construcción, y no por accidente.

#### Cómo se verificó

Tests nuevos en `EntregaServiceIdempotenciaTest`. Lo relevante es que **tienen dientes**: se
neutralizó la guarda a propósito (`if (false && existsById(...))`) y 3 de los 8 tests
fallaron, justamente los tres que miden la idempotencia:

```
EntregaServiceIdempotenciaTest.noVuelveAGuardarSiYaExiste        FAIL
EntregaServiceIdempotenciaTest.noTocaElEstadoDeUnItemYaEntregado  FAIL
EntregaServiceIdempotenciaTest.registraSoloLosBienesNuevos        FAIL
Tests run: 8, Failures: 3
```

Restaurada la guarda, los 8 vuelven a pasar.

---

### 22. Ninguna entidad tenía `@Version`: dos instancias escribiendo a la vez se pisaban

**Estado:** corregido
**Severidad:** alta
**Archivos:** las 12 entidades de `logisticas-service`

#### Qué pasaba

Búsqueda de `@Version` en el módulo: **cero resultados**. `procesarPeticion` era un patrón
leer-modificar-escribir sin protección, y además estaba **dentro del `for` de bienes**, con lo
que escribía `Pais → Provincia → Ciudad → Direccion → Entidad` una vez por bien.

Dos donaciones para la misma entidad, atendidas por dos instancias: las dos leen las mismas
filas, las dos escriben, y la segunda pisa a la primera con los valores que leyó **antes** del
UPDATE de la otra. Sin excepción, sin log.

#### Qué se hizo

**1. `@Version` en las 12 entidades**, no solo en `ItemEntrega`. En `ItemEntrega` es
especialmente importante porque su clave natural hacía que el `merge()` fuera el camino normal
y no la excepción.

**2. El catálogo se resuelve una vez por mensaje y no por bien.** Esto es el arreglo de la
carrera **y de paso el del punto 15**: una donación con tres bienes escribía la misma dirección
tres veces. Ahora se resuelve una vez, y el `@Version` hace que la segunda instancia que llegue
reciba `OptimisticLockingFailureException` en vez de sobrescribir. Hay un test que lo mide.

**3. `@Transactional` en `procesarPeticion` y en `actualizarEstado`.** Lo segundo es por una
razón distinta del primero: `actualizarEstado` publica el evento de trazabilidad y después
guarda el ítem. Sin transacción, si el `save` fallaba el evento ya había salido del servicio y
`donaciones-service` se enteraba de una entrega que en la base nunca ocurrió.

**4. Validaciones de payload**, para que un mensaje incompleto se rechace **antes** de escribir
en lugar de dejar la donación a medias. Es también el punto 16.

---

### 23. Con N consumidores no hay orden: se midió y el particionado quedó apagado

**Estado:** corregido. El reparto por hash está implementado pero **desactivado a propósito**
**Severidad:** media
**Archivo:** `.../config/RabbitMQConfig.java`, `.../RabbitMQ/DonacionListener.java`,
`application.properties`

#### Qué se investigó

Competing consumers significa que el broker reparte los mensajes de a uno entre las instancias,
**en cualquier orden y sin sincronización entre ellas**. Dos mensajes del mismo `idDonacion`
pueden ser atendidos por dos instancias al mismo tiempo, y el que se atiende segundo no tiene
por qué ser el que se mandó segundo. Con una máquina de estados
`PENDIENTE → EN_CAMINO → ENTREGADA` eso permite **retroceder el estado**.

Con un solo consumidor el orden se respeta, porque RabbitMQ le entrega los mensajes en el orden
en que los publica. **El orden no se pierde al usar el broker: se pierde al agregar el segundo
consumidor.** Por eso el problema nunca se habría detectado.

#### Se implementó el particionado, y después se midió

Primero se hizo completo: exchange `x-consistent-hash` sobre el encabezado `x-id-donacion`, N
colas de shard, y cada instancia atendiendo un subconjunto **disjunto** con
`LOGISTICA_SHARDS_ASIGNADAS`. Verificado: 5 mensajes de la misma donación caían 5/5 en la misma
cola, y dos instancias repartían 20 mensajes 7/13.

Después se probó **qué pasa si se cae una instancia**, y el resultado dio vuelta la decisión:

```
con la instancia B bajada (la de los shards 2 y 3):
  queue.0   msgs=0   cons=1
  queue.1   msgs=0   cons=1
  queue.2   msgs=4   cons=0    <- nadie la lee
  queue.3   msgs=11  cons=0    <- nadie la lee
```

De 20 mensajes, la instancia sana procesó **5** y los otros **15 quedaron parados**, aunque
estaba viva y con capacidad de sobra. El particionado **no se traba nunca**, pero tampoco
sobrevive: cada caída deja su mitad del trabajo detenida.

#### La decisión: una sola cola, y por qué

**Se volvió a la cola compartida.** El motivo es que el análisis original acertaba: hoy ningún
mensaje que llegue a logística transporta una transición de estado.

| Routing key | Listener | Qué hace |
|---|---|---|
| `donaciones.creada` | `DonacionListener` | **Registra** ítems en `PENDIENTE` |

El registro quedó idempotente por construcción (punto 21). Las transiciones de estado entran por
otro camino: el `PATCH` contra `actualizarEstado`, que es la acción del operador sobre **una**
donación, y por lo tanto no compite con nadie.

O sea que el orden que el particionado compraba **no se necesita hoy**, y lo que cuesta es real:
disponibilidad. Se prefirió no pagar algo que no hace falta.

Medido con la cola compartida y dos instancias:

```
12 mensajes con las dos vivas  ->  A: 6, B: 6
se mata la instancia B
15 mensajes con una sola viva ->  A: 15
cola: 0 mensajes   DLQ: 0   27 de 27 en la base
```

Con una instancia caída, **la otra sigue consumiendo sin frenarse**. Eso es lo que se buscaba.

#### Qué quedó del particionado

El mecanismo sigue implementado y documentado en `application.properties`, **apagado por
defecto**. Se activa con `LOGISTICA_SHARDS_ASIGNADAS=0,1` repartido de forma disjunta.

El exchange sigue siendo `x-consistent-hash` en los dos modos: con un solo binding manda todo a
la cola única, y con cuatro los reparte. Así `donaciones-service` **no necesita saber en qué
modo está logística**: publica al mismo exchange con el mismo encabezado siempre.

#### Lo que resuelve el orden cuando aparezca un mensaje con estado

Una guarda que consulta la base **no sirve**, y conviene dejarse claro por qué. Si llega "la
donación está ENTREGADA" antes que "la donación pasó a EN_CAMINO", la guarda ve `PENDIENTE` y no
tiene nada que hacer: rechazarlo pierde el mensaje, aceptarlo deja el estado saltado. La guarda
dice **qué estado hay**, no arregla que el orden se haya roto.

Lo que sí cubre el caso real es **reintentar con espera**, y eso quedó en `DonacionListener`:

```
logistica.reintentos=3
logistica.espera-reintento-ms=2000
```

Un fallo transitorio (la base tardó, se cortó la conexión) se reintenta y al segundo intento
suele salir. Si tras los tres intentos sigue fallando, ahí sí va a la dead letter. Los errores de
negocio y la carrera de clave primaria **no** se reintentan: fallan igual todas las veces, y
reintentarlos solo frenaría la cola compartida.

Cuando aparezca un mensaje con transiciones de estado, la frontera es esta: o se prende
`LOGISTICA_SHARDS_ASIGNADAS` y se acepta el costo de disponibilidad, o el reintento con espera da
suficiente cobertura y la cola compartida sigue conviene.

---

### 24. El compose no se podía escalar

**Estado:** corregido
**Severidad:** media
**Archivo:** `docker-compose.yml`

#### Qué pasaba

El propio compose recomendaba en un comentario:

```
# servicio con `docker compose up --scale logisticas-service=2`.
```

**Y no funcionaba.** Dos cosas lo bloqueaban:

```yaml
container_name: logisticas-service   # no se pueden crear N contenedores con el mismo nombre
ports:
  - "8086:8086"                      # el puerto del host no se puede reservar dos veces
```

El `SERVER_PORT` tampoco llegaba a logística, porque su `application.properties` leía
`${PORT:8086}` en vez de `${SERVER_PORT:8086}`. No se notaba porque el default coincidía con lo
que pasa el compose, pero en cuanto hicieran falta dos instancias en el mismo host —que es
justo este punto— el puerto se ignoraba y las dos pelaban por el 8086.

#### Qué se hizo

1. **`container_name` fuera de los cuatro servicios de dominio.** Se deja solo en `mysql` y
   `rabbitmq`, que no se escalan y cuyo nombre estable evita depender de la red de Docker para
   llegar a ellos.
2. **El puerto de logística se publica efímero** (`- "8086"`, sin número de host), para que
   Docker asigne uno distinto por instancia. Para descubrir cuál le tocó:
   `docker compose port logisticas-service 8086`. El `SERVER_PORT` interno sigue siendo 8086
   para todas, porque dentro de la red de Docker cada contenedor tiene su propio espacio de
   puertos.
3. **`logisticas-service` lee `SERVER_PORT`**, igual que los otros tres.

`docker compose config` valida el archivo después del cambio.

#### El costo, dicho explícitamente

Quitar el puerto fijo tiene un precio para desarrollo: `docker compose up` a secas deja de dar
`localhost:8086` fijo. Está anotado en el propio compose que se puede volver a `"8086:8086"`
cuando se levanta de a una sola instancia.

La alternativa era un puerto fijo **o** la opción de escalar, y no pueden convivir. Se eligió
escalar, porque es lo que pide el enunciado.

---
### 5. Los tres DELETE devuelven 404 despues de borrar bien

**Estado:** corregido el 2026-10-07, sin commit
**Severidad:** alta
**Archivos:** `services/EntregaService.java`, `services/CamionService.java`, `services/ChoferService.java`

#### Que pasaba

```java
Optional<X> x = repo.findById(id);
if(x.isPresent()){
  repo.deleteById(id);
  throw new IllegalArgumentException("... no encontrado");
}
```

La excepcion se lanzaba cuando el recurso SI existia: el DELETE borraba bien y el controller
devolvia 404. Y cuando no existia, no se entraba al if y devolvia 204 en silencio. Los dos
casos al reves.

#### Que se cambio

Se invirtio la condicion: el 404 queda para cuando el recurso no estaba, y el 204 para cuando se
borro. Se mantuvo el contrato que los tres controllers ya documentan en el Swagger, en vez de
dejar el 404 sin uso.

#### Como se verifico

`DeleteDevuelve204Test`, seis casos: los tres servicios por existentey por inexistente. Los seis
fallan contra el codigo viejo: los de "existe" por el `throw`, los de "no existe" porque el
viejo no tiraba nada. El test ademas verifica que `deleteById` NO se llame cuando el recurso no
esta.

### 6. La relacion item-evento apunta al id equivocado

**Estado:** corregido el 2026-10-07, sin commit
**Severidad:** alta
**Archivos:** `models/entities/ItemEntrega/ItemEntrega.java`, `models/entities/EventoLogistica/EventoLogistica.java`

#### Que pasaba

`@OneToMany(mappedBy = "id", ...)` apuntaba a la clave primaria del propio evento
(`id_evento`, `GenerationType.IDENTITY`), no a un atributo que referencie al item. Hibernate
armaba una relacion inventada, con la FK en `item_entrega.id_evento` siempre en NULL: la lista
salia siempre vacia y la trazabilidad del Swagger de `GET /entregas` no existia.

#### Que se cambio

Se agrego `@ManyToOne @JoinColumn(name="id_donacion") ItemEntrega item` en `EventoLogistica`
—el lado dueno— y se puso `mappedBy = "item"` en el lado muchos. El cascade y el `orphanRemoval`
**se conservaron**: sin ellos, la FK nueva hace imposible borrar un item con historial, porque
MySQL rechaza el DELETE de un padre con hijos. Borrar una entrega borra su historia, que es
coherente con que el item es el registro de esa entrega.

#### Como se verifico

`GestorPublicacionEventosTest`: se afirma por reflexion que `mappedBy` es `"item"` y que del
lado dueno hay un `@ManyToOne` con `@JoinColumn(name="id_donacion")`, y que el cascade sigue
(`contains(CascadeType.ALL)` y `orphanRemoval`). Contra el codigo viejo el primer test falla
con `but was: "id"`.

**Lo que no se pudo verificar sin una base real:** que el `cascade REMOVE` efectivamente borre
los hijos en `DELETE /entregas/{id}`. El modulo no tiene base embebida en el build offline. Si
al probar contra MySQL ese endpoint diera 500 por FK, el arreglo es borrar los eventos antes
que el item.

### 11. El mismo evento se mete en la lista de todos los items

**Estado:** corregido el 2026-10-07, sin commit
**Severidad:** media
**Archivos:** `models/gestores/GestorPublicacionEventos.java`

#### Que pasaba

Una sola instancia de `EventoLogistica` se agregaba a la lista `eventos` de todos los items de
la ruta. Con la relacion ya siendo `mappedBy="item"` eso no tiene a que FK asignarle una
instancia compartida, y con `orphanRemoval` desvincularlo de un item lo borraba aunque siguiera
en los demas.

#### Que se cambio

El `INICIO_RUTA` es un hecho de la ruta, no de cada item: su `referenciaId` ya lo dice, porque
es el `idRuta`, y los ids de las donaciones viajan en el payload. Se creo **un solo** evento,
con `item = null`, y las listas de los items dejaron de tocarse a mano. Para los eventos que si
son de una entrega, el lado dueno se setea y se guarda con su propio repositorio.

Se descarto la alternativa de un evento por item: multiplicaba por N las filas que devuelve el
polling, con el mismo payload y el mismo `referenciaId`, y un consumidor por HTTP habria
recibido N notificaciones del mismo inicio de ruta.

#### Como se verifico

`GestorPublicacionEventosTest` afirma que `repoEventos.save` se llama **una** vez, que las listas
de los items quedan vacias y que el evento guardado tiene `getItem() == null` y la referencia de
la ruta. Contra el codigo viejo fallaba.

### 12. El polling de eventos reenvia el ultimo evento y explota con `desdeId` null

**Estado:** corregido el 2026-10-07, sin commit
**Severidad:** media
**Archivos:** `services/EventoLogisticaService.java`, `RabbitMQ/SolicitudEventosListener.java`, `controllers/EventoLogisticaController.java`

#### Que pasaba

`obtenerEventosNuevos` pasaba `desdeId - 1`, o sea `id >= desdeId`: el evento con
`id == desdeId` es el ultimo que el cliente ya proceso y se lo volvia a mandar. Ademas
`desdeId - 1` sobre un `Long` null desempaquetaba y lanzaba NullPointerException.

#### Que se cambio

Se pasa `desdeId` tal cual, y un null se trata como 0 —que es "mandame todo", la lectura util de
un poll recien arrancado—, sin desempaquetar. La proteccion quedo en el service, que es donde
esta el unboxing, asi que cualquier llamador queda cubierto y no depende de que cada listener se
acuerde de filtrar.

En el listener de sondeo se mantuvo la salida temprana con `desdeId` ausente: sin cursor no hay
desde donde consultar, y atenderlo con `id > 0` traia la tabla entera para tirar el resultado a
la basura, porque el metodo es `void`.

#### Como se verifico

`EventoLogisticaServiceTest` afirma que se consulta con el id **exacto** (y que nunca se
pide `desdeId - 1`), que un null no revienta y se traduce a 0, y que el listener no consulta
nada sin cursor. Los siete fallan contra el codigo viejo, uno de ellos con el NPE exacto:
`Cannot invoke "java.lang.Long.longValue()" because "desdeId" is null`.

### 13. `findByIdGreaterThanOrderByIdAsc` no ordena

**Estado:** corregido el 2026-10-07, sin commit
**Severidad:** media
**Archivos:** `models/repositories/eventos/RepositorioEventoLogistica.java`

#### Que pasaba

Era un `default` method con `findAll()` y un `stream().filter(...)`: traia la tabla entera a
memoria para descartar casi todo, y el orden dependia de lo que MySQL tuviera ganas de
devolver. El comentario que lo acompanaba no garantizaba nada.

#### Que se cambio

Ahora es una derived query de verdad, que Spring Data traduce a `WHERE id > ?1 ORDER BY id
ASC`. El filtro y el orden los resuelve la base, que es lo unico que puede garantizarlo.

#### Como se verifico

`EventoLogisticaServiceTest` afirma por reflexion que el metodo **no** es `default`. Sin
base embebida no se puede comprobar el SQL generado, asi que se verifica la declaracion y el
criterio (el id exacto que se le pasa), no la consulta.

### 14. `GET /entregas/{id}` devuelve la entidad cruda

**Estado:** corregido el 2026-10-07, sin commit
**Severidad:** media
**Archivos:** `services/EntregaService.java`, `models/entities/ItemEntrega/ItemEntrega.java`

#### Que pasaba

`findById` devolvia el `ItemEntrega` de JPA y el controller lo serializaba tal cual. Jackson
sigue los getters y entra en ciclo —`item.parada` → `parada.ruta` → `ruta.paradas` →
`parada.items` → `item.parada`— hasta reventar al construir el JSON. Y ademas exponia
`id_parada` e `id_unidad_medida` en un endpoint que el Swagger declara como `BienDTO`.

#### Que se cambio

`findById` devuelve `BienDTO` usando el mismo `convertirABienDTO` que ya usan `findAll` y
`obtenerEntregasNoRecibidas`: la inconsistencia entre los tres era el indicio de que faltaba
esa linea. Y se puso `@JsonIgnore` en `ItemEntrega.parada` como red, para que un mapping
equivocado a futuro no sea una denegacion de servicio.

#### Como se verifico

`EntregaServiceFindByIdDevuelveDtoTest` arma el ciclo completo (parada → ruta → sus paradas de
vuelta al item) y afirma que el JSON no trae `parada` ni `id_parada`, y que el controller
responde 200 con un `BienDTO`. Contra el codigo viejo, cinco de los seis fallan y el volcado
muestra el ciclo real: `Parada["ruta"]->Ruta["paradas"]->ArrayList[0]->Parada["ruta"]->...`

### 25. Un mensaje malformado se reencola para siempre

**Estado:** corregido el 2026-10-07, sin commit
**Severidad:** alta
**Archivos:** `RabbitMQ/DonacionListener.java`, `src/main/resources/application.properties`

#### Que pasaba

Agotados los 3 intentos, el listener hacia `throw ultimoFallo` y el log decia "va a la cola de
mensajes muertos". No era asi: sin `default-requeue-rejected=false`, rige el default de Spring
AMQP, que es reencolar. El mensaje volvia a su posicion original en la cola compartida y
volvia a entrar, tres intentos mas, para siempre. Un solo payload malformado frenaba a todas
las instancias y la DLQ recibia cero mensajes, porque el dead letter solo se activa con un
rechazo **sin** requeue.

#### Que se cambio

Dos capas. La explicita: el listener tira `AmqpRejectAndDontRequeueException` con el fallo
original como causa, que es la senal que Spring AMQP respeta siempre. Y la de config:
`spring.rabbitmq.listener.simple.default-requeue-rejected=false`, que cubre tambien los fallos
que nunca llegan al listener (un mensaje que no se puede convertir a `EntregaDTO`).

Se eligio la exception en vez de solo la property para no cambiarle el comportamiento al otro
listener del servicio a ciegas.

#### Como se verifico

`DonacionListenerCarreraTest`: se afirma el tipo de la excepcion y que la causa se conserva.
Dos tests existentes afirmaban `IllegalStateException` —el contrato viejo, que era el bug— y se
actualizaron. `ConfiguracionArranqueTest` afirma la property.

**Efecto colateral:** `default-requeue-rejected` es global al servicio, asi que un fallo
transitorio de la base en cualquier listener va a la DLQ de integracion en vez de reencolar. Se
eligio eso: reencolar para siempre un mensaje que falla igual trava la cola de la misma manera.
(El sondeo que se mencionaba aca se elimino despues; ver la ultima entrada de `Corregidos`.)

### 28. Publica el evento antes del commit

**Estado:** corregido el 2026-10-07, sin commit
**Severidad:** media
**Archivos:** `models/gestores/GestorPublicacionEventos.java`, `services/RutaService.java`

#### Que pasaba

`productorEventos.publicar(evento)` se llamaba **dentro** de la transaccion. Si un `save`
posterior fallaba —por ejemplo un `OptimisticLockingFailureException` cuando dos operadores
confirman la misma entrega casi al mismo tiempo— la base hacia rollback pero el mensaje ya
habia salido: `donaciones-service` notificaba una entrega que en logistica nunca ocurrio. En
`iniciarRuta` pasaba lo mismo: se publicaba y recien despues se persistian los items con el
estado `EN_CAMINO` que se les habia puesto en memoria.

#### Que se cambio

La publication se registra con `TransactionSynchronizationManager` y se manda en `afterCommit`,
que es exactamente la garantia que faltaba: un rollback no despacha `afterCommit`. Sin
transaccion activa se publica en el momento, que es el unico jeito de no perder el evento cuando
no hay commit al que esperar.

Y `RutaService.iniciarRuta` ahora es `@Transactional`, que es lo que hace que esa garantia
aplique tambien ahi: antes no habia commit al que agendarse.

#### Como se verifico

`GestorPublicacionEventosTest`: con transaccion abierta el evento **no** se publica hasta el
commit, un rollback no publica nada, y sin transaccion se publica igual. Se despachan a mano las
sincronizaciones que despacha Spring, sin levantar un contexto.

**Lo que quedo fuera:** `terminarRuta` sigue sin ser transaccional, y a proposito: tiene `throw`
deliberados de bugs que todavia no se arreglaron, y volverla transaccional haria rollback de
cosas no relacionadas. Sus eventos de reingreso se publican en el momento, como antes.

**Efecto colateral no pedido:** con `@Transactional`, `findByChofer` y `actualizarEstado` ven la
misma instancia gestionada de la ruta, asi que `indexOf` ahora la encuentra y `iniciarRuta` si
persiste el estado `EN_CURSO`, que antes no se guardaba. Es una mejora, pero cambia el
comportamiento del punto 2.

### 33. Credenciales de la base hardcodeadas

**Estado:** corregido el 2026-10-07, sin commit
**Severidad:** media
**Archivos:** `src/main/resources/application.properties`, `docker-compose.yml` (raiz del repo), `.env.example`

#### Que pasaba

`spring.datasource.password=${DB_PASSWORD:10032001}`: la credencial real estaba como **default**.
Cualquier arranque sin las variables —un `java -jar` a secas, un deploy que se olvido de
definirlas— conectaba en claro y sin avisar. Y el `docker-compose.yml` la repetia en el archivo,
quedando commiteada en el historial de git.

#### Que se cambio

`application.properties` quedo sin default: si falta la variable, el servicio no levanta, que es
lo correcto. El compose lee del entorno con `${DB_USERNAME:?...}`, y se agrego `.env.example`
para documentar como se define. No hizo falta tocar `.gitignore`: el del repo ya cubria `.env`.

#### Como se verifico

`ConfiguracionArranqueTest` afirma que las dos properties no tienen default y que la clave no
aparece en `application.properties`. Contra el codigo viejo falla con
`but was: "${DB_USERNAME:valentin}"`.

**Lo que quedo a medias:** los bloques de `donaciones-service` y `notificaciones-service` en el
compose siguen con usuario y clave en claro. `donaciones-service` usa la MISMA clave que se acaba
de sacar. El punto 33 solo nombraba el bloque de logisticas, asi que no se toco, pero el
`grep` de la credencial sigue dando y queda anotado en el propio compose.

**Y lo que no se puede arreglar desde el codigo:** la clave ya esta en el historial de git.
Sacar la linea no la borra de ahi. Hay que rotarla en MySQL.

### 39. El cron de planificacion corre a las 02:00 UTC

**Estado:** corregido el 2026-10-07, sin commit
**Severidad:** baja
**Archivos:** `Scheduler/PlanificadorDeRutasScheduler.java`

#### Que pasaba

`@Scheduled(cron = "0 0 2 * * ?")` no declara `zone`, asi que Spring usa la zona de la JVM. La
imagen final (`eclipse-temurin:21-jre`) no define `TZ`, el compose tampoco le pasa ninguna, y el
`DB_URL` hasta fuerza `serverTimezone=UTC`: en Docker la planificacion ocurre a las 02:00 UTC,
que son las 23:00 de Argentina. Fuera de Docker depende del host.

#### Que se cambio

`zone = "America/Argentina/Buenos_Aires"` explicito: deja de depender de la imagen y del host.

#### Como se verifico

`ConfiguracionArranqueTest` lee la anotacion por reflexion y afirma que declara la zona y que el
cron sigue siendo `"0 0 2 * * ?"`. Contra el codigo viejo falla con `zone() == ""`.

**Sin verificar:** la observacion con la hora del contenedor real. El pendiente estaba marcado
como sospechado por eso, y corregir la zona lo hace deterministico igual.

### 40. El sondeo de trazabilidad por cola era un no-op

**Estado:** corregido el 2026-10-07, sin commit
**Severidad:** media
**Archivos:** `RabbitMQ/SolicitudEventosListener.java` (eliminado),
`dto/evento/SolicitudEventosDTO.java` (eliminado), `config/RabbitMQConfig.java`,
`donaciones-service/.../models/sheduler/LogisticaPollingScheduler.java` (eliminado en donaciones),
`donaciones-service/.../config/RabbitMQConfig.java`

#### Que pasaba

El camino real de eventos es el push: `GestorPublicacionEventos` persiste el `EventoLogistica` y lo
publica despues del commit a `logistica.eventos.exchange`, y donaciones lo consume en
`EventosListener`. El polling era una "red de contencion" que no contenia nada:

- `SolicitudEventosListener` leia los eventos y **solo logueaba**; nunca publicaba la respuesta, y
  `LogisticaPollingScheduler` publicaba con `convertAndSend` (sin esperar respuesta). No habia
  camino de vuelta a donaciones.
- El cursor `ultimoIdProcesado` del scheduler nunca avanzaba: cada 5 minutos preguntaba desde 0.
- `GET /api/eventos` estaba disponible pero donaciones no lo llamaba.

O sea: no era un fallback imperfecto, era un no-op. Lo unico observable era un log cada 5 minutos.

#### Que se cambio

Se elimino la cadena completa: `LogisticaPollingScheduler`, `SolicitudEventosListener`, la cola
`logistica.sondeo.queue`, el exchange `logistica.exchange`, la routing key
`logistica.solicitud.eventos` y los DTO `SolicitudEventosDTO` / `EventoLogisticaResponseDTO` del
lado de donaciones. La consulta de trazabilidad queda por HTTP en `GET /api/eventos`
(`EventoLogisticaController` + `EventoLogisticaService`), que es lo que el enunciado pide al
describir logistica accesible por sus URIs.

El push ya es at-least-once (cola durable): el polling no agregaba cobertura, porque el unico caso
que podria cubrir —un evento publicado a un exchange sin ruta— requiere una respuesta real y
deduplicacion persistente, justo el request/response que se habia quitado a proposito.

#### Como se verifico

`EventoLogisticaServiceTest` (antes `EventoLogisticaServicePollingTest`) cubre la lectura por id:
off-by-one, null, orden y la derived query. Se quitaron los dos tests del listener, que ya no
existe. Los dos modulos compilan (`mvn test-compile`) y el test corre 6/6 en verde.
