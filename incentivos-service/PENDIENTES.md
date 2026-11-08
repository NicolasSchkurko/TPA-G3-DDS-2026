## Revisión del enunciado hasta Entrega 6

El servicio cubre perfiles, progreso de misiones, insignias, métricas/rankings, APIs REST y persistencia
relacional; también hay publicación de eventos a n8n con outbox. Los puntos técnicos abiertos que siguen
abajo afectan seguridad de administración, consistencia/concurrencia, operación de la outbox y reglas de
negocio. E2 requiere además confirmar el alcance funcional completo de analíticas y avisos. Faltan los
diagramas de clases/ERD y la justificación de diseño versionados (pendiente transversal). E6 es del
`front-service`; hoy no propaga identidad/token a este backend.

Nota de vigencia: la baja lógica/anonimización de perfil con preservación de métricas ya figura en
`Corregidos` (punto 54); no reabrirla como borrado físico. Verificar que referencias antiguas del punto
39 se interpreten a la luz de esa corrección.

---
# Pendientes técnicos de `incentivos-service`

Registro de problemas conocidos del servicio, con el motivo y la propuesta de arreglo
para que no se pierdan de vista al crecer el código.

**Están ordenados por severidad** (de mayor impacto a menor impacto). El número es
un ID estable y no se renumera nunca, así que quedan huecos. Un punto corregido se borra
de esta lista y pasa a la sección [Corregidos](#corregidos) del final.

| # | Punto | Severidad | Por qué está acá |
|---|---|---|---|
| 1 | 39 | Alta | Un perfil queda huérfano si la baja del donante en donaciones no llega |
| 2 | 47 | Alta | `actualizarCategoria` rechaza cualquier cambio de posición en la secuencia de categorías con un 409 espurio |
| 3 | 48 | Alta | `Categoria.siguienteMision` y `eliminarMision` comparan `Mision` por igualdad referencial en vez de ID o `equals`, saltándose misiones del donante |
| 4 | 49 | Alta | La racha de constancia mensual expira al inicio del mes siguiente si la última donación fue a principios de mes |
| 5 | 1 | Alta | La identidad de admin declarada en un header no está vinculada a una identidad autenticada |
| 6 | 50 | Media | `InsigniaObtenida.equals` compara instancias de `Insignia` sin `equals`, permitiendo duplicar medallas en JPA |
| 7 | 51 | Media | `buscarSiguienteParaEnviar` en la outbox de n8n bloquea pesimistamente toda la tabla de pendientes sin paginación (`LIMIT 1`) |
| 8 | 52 | Media | `RepositorioNotificacionesPendientes` retiene notificaciones fallidas en memoria sin reintentos ni persistencia |
| 9 | 55 | Media | `PerfilService.actualizarPerfilImpacto` reintenta en bucle cerrado (tight spin loop) sin backoff ni jitter ante colisión optimista |
| 10 | 56 | Media | `buscarPerfilesConMisionQueRequiereConstancia` combina `JOIN FETCH` con `Pageable`, arriesgando paginación en memoria |
| 11 | 58 | Media | No existe el algoritmo de donaciones destacadas: últimas N entregadas que completaron una misión |
| 12 | 53 | Baja | `actualizarDatosPerfil` acepta nombres de usuario compuestos únicamente por espacios en blanco |
| 13 | 40 | Baja | El mensaje publicado en redes arranca con una coma y la red está fija en `discord` |
| 14 | 45 | Baja | La outbox de n8n reintenta para siempre: no hay tope de intentos ni descarte |
| 15 | 46 | Baja | Un período de ranking ya existente responde 400 en vez de 409 |
| 16 | 57 | Baja | `N8nClient.procesarPendientes` procesa la outbox de n8n secuencialmente bloqueando el hilo del scheduler |
| 17 | 37 | Baja | La configuración Checkstyle no se ejecuta automáticamente en el build |
| 18 | 23 | Baja | Higiene del código: decisiones de identidad y cobertura JPA |
| 19 | 10 | Documentado | Decidir si el ranking cuenta insignias o misiones (decisión documentada, no relevante) |

El punto 1 requiere una decisión del equipo para acordar una identidad compartida entre servicios.
El punto 10 queda documentado pero no es relevante: el equipo decidió deliberadamente contar insignias.
El punto 23 conserva anotaciones sobre decisiones de identidad y ampliación de cobertura JPA.

El detalle de cada fix está en [Corregidos](#corregidos), un ítem por corrección.

---

## 39. Un perfil queda huérfano si la baja del donante en donaciones no llega

**Estado:** abierto
**Severidad:** alta
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/incentivos/controllers/PerfilController.java` (`DELETE /api/perfiles/interno/{idUsuario}`), `src/main/java/ar/edu/utn/frba/ddsi/incentivos/services/PerfilService.java` (`eliminarPerfilPorBajaDeDonante`, `borrarPerfilYHistorial`). Contraparte: punto 35 de `donaciones-service/PENDIENTES.md`.

### Qué pasa

El perfil gamificado de un donante dado de baja en `donaciones-service` **sobrevive en la base de incentivos**. La baja la dispara y la ejecuta donaciones por HTTP síncrono (`DELETE /api/perfiles/interno/{idUsuario}`); incentivos no tiene forma de enterarse por sí solo de que el donante dejó de existir. Cuando esa llamada no llega —o llega con un id que no matchea— el perfil queda huérfano y nada lo limpia después.

### Por qué no se ve

1. `PerfilService.eliminarPerfilPorBajaDeDonante` es idempotente y **devuelve 200 aunque no borre nada**: hace `if (!repositorioPerfiles.existsById(idUsuario)) return;`. Un id que no coincide con ningún perfil (perfil creado con otro id, datos viejos o de un seed) es indistinguible de un borrado exitoso. El llamador no puede saber si quedó algo.
2. La baja depende de que donaciones ejecute la llamada, y **en el momento correcto**: si la baja local aborta antes (por la FK del punto 34 de donaciones) o falla la red, la cascada no corre y no hay reintento.
3. No hay ninguna consulta ni job que detecte perfiles cuyo `idUsuario` ya no exista en donaciones.

### Propuesta

1. Consumir un evento `donante.baja` publicado por donaciones (Rabbit) para procesar la baja de forma idempotente con DLQ y reintentos, desacoplándola de la llamada HTTP en medio de la transacción del otro servicio.
2. **Alineación con punto 54:** El procesamiento de la baja (sea por evento o por el endpoint interno `DELETE /api/perfiles/interno/{idUsuario}`) debe aplicar **soft delete y minimización** en `Perfil` (`activo = false`, anonimizar `nombreUsuario`, limpiar insignias y progreso) y **preservar intacto el historial en `impacto_donacion`** (eliminar la llamada a `deleteByIdUsuario` para no destruir las métricas históricas de la plataforma).
3. Exponer una **reconciliación** (job periódico o endpoint de admin) que detecte perfiles cuyo donante ya no exista en `donaciones-service` y aplique sobre ellos la baja lógica y minimización del punto 54.
4. Mantener el endpoint `/api/perfiles/interno/{idUsuario}` operativo y transaccional como canal síncrono de respaldo (commit `7e9da04`).

---

## 47. `actualizarCategoria` rechaza cualquier cambio de posición en la secuencia de categorías con un 409 espurio

**Estado:** abierto
**Severidad:** alta
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/incentivos/services/CategoriaService.java:140-146`, `src/main/java/ar/edu/utn/frba/ddsi/incentivos/models/gestores/SecuenciaCategoria.java:21-38`

### Qué pasa

En una secuencia válida de categorías, las posiciones ocupadas son consecutivas y contiguas (`1..N`, sin huecos). Cuando un administrador intenta modificar la posición de una categoría existente mediante `PUT /api/categorias/admin/{id}` (por ejemplo, mover la categoría de posición 1 a 2):

```java
Integer destino = categoriaModificada.getPosicionSecuencia();
if (destino != null) {
    // La posición tiene que estar libre, salvo que sea la misma que ya tenía esta
    // categoría (el caso normal de editar el nombre).
    if (!destino.equals(categoriaActual.getPosicionSecuencia())
            && repoCategorias.existsByPosicionSecuencia(destino)) {
        throw new ConflictoException(
                "Ya hay una categoría en la posición " + destino + ".");
    }

    gestorSecuencia.desplazarParaActualizar(
            repoCategorias,
            categoriaActual.getPosicionSecuencia(),
            destino,
            gestorSecuencia.posicionMaxima(repoCategorias)
    );
    categoriaActual.moverAPosicion(destino);
}
```

Como la posición de destino ya está ocupada por otra categoría dentro de `1..N`, `repoCategorias.existsByPosicionSecuencia(destino)` es siempre `true`. La guarda aborta la operación con `ConflictoException` (409 Conflict).
Si se intenta pasar una posición libre más allá de `N` (ej. `destino = max + 1`), `SecuenciaCategoria.verificarPosicionEnRango` rechaza con `IllegalArgumentException` (400) porque en actualización el tope es `max`.
En consecuencia, es imposible mover cualquier categoría a una posición distinta; el bloque `gestorSecuencia.desplazarParaActualizar` es código muerto inalcanzable.

### Cómo se dispara

Llamar a `PUT /api/categorias/admin/{id}` con un `posicionSecuencia` distinto al actual (cualquier valor de 1 a N). Responde 409 Conflict en lugar de reordenar las categorías.

### Propuesta de arreglo

Eliminar la guarda `repoCategorias.existsByPosicionSecuencia(destino)` en `actualizarCategoria`: `SecuenciaCategoria.desplazarParaActualizar` ya se encarga de validar el rango válido `[1, max]` y desplazar en bloque las demás categorías hacia arriba o abajo para hacer lugar al destino.

---

## 48. `Categoria.siguienteMision` y `eliminarMision` comparan `Mision` por igualdad referencial en vez de ID o `equals`, saltándose misiones del donante

**Estado:** abierto
**Severidad:** alta
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/incentivos/models/entities/CategoriaPerfil/Categoria.java:79, 115`, `src/main/java/ar/edu/utn/frba/ddsi/incentivos/models/entities/Mision/Mision.java`, `src/main/java/ar/edu/utn/frba/ddsi/incentivos/models/entities/CategoriaPerfil/CategoriaMision.java:34`, `src/main/java/ar/edu/utn/frba/ddsi/incentivos/services/PerfilService.java:357-372`

### Qué pasa

`Mision` no implementa `equals` ni `hashCode`, por lo que hereda `Object.equals` (comparación por identidad `this == other`).
En `CategoriaMision`, la relación con `mision` es perezosa (`fetch = FetchType.LAZY`).
Cuando un donante completa una misión, `PerfilService.asignarSiguienteMision` llama a `categoriaActual.siguienteMision(misionCompletada)`:

```java
Integer posicionActual = this.categoriaMisiones.stream()
        .filter(cm -> cm.getMision().equals(misionActual))
        .map(CategoriaMision::getPosicion)
        .findFirst()
        .orElse(null);
```

`cm.getMision()` es un `HibernateProxy` creado al hidratar la colección `categoriaMisiones`. Al evaluar `cm.getMision().equals(misionActual)`, Java evalúa `proxy == misionActual`, lo cual siempre da `false` porque el proxy y la entidad cargada previamente en el contexto de persistencia son objetos distintos en memoria.
Al dar `false`, `posicionActual` queda en `null`, y `siguienteMision` retorna `null`.
`PerfilService.asignarSiguienteMision` interpreta erróneamente que la categoría no tiene más misiones y promueve al donante a la siguiente categoría (`cambiarCategoria`) o da por terminada la secuencia (`finalizarSecuencia`), salteándose todas las misiones intermedias de la categoría actual tras completar la primera.
De igual forma, en `Categoria.eliminarMision(Mision mision)`:

```java
boolean removida = this.categoriaMisiones.removeIf(cm -> cm.getMision().equals(mision));
```

El `removeIf` no elimina la relación si `cm.getMision()` es un proxy, dejando la fila en `categoria_mision` y rompiendo el posterior borrado de la misión en `MisionService.eliminarMision` por violación de clave foránea.

### Cómo se dispara

Un donante completa la primera misión de una categoría que posee 2 o más misiones (flujo normal de donación en `PATCH /api/perfiles/donacion/{idUsuario}`). El servicio promueve al donante directamente a la siguiente categoría en lugar de asignarle la segunda misión.

### Propuesta de arreglo

Implementar `equals` y `hashCode` en `Mision` basados en `idMision` compatibles con proxies Hibernate (`idMision != null && idMision.equals(...)`), o comparar explícitamente los identificadores en `Categoria`: `Objects.equals(cm.getMision().getIdMision(), misionActual.getIdMision())`, tal como ya se hace en `SincronizacionPerfiles`.

---

## 49. La racha de constancia mensual expira al inicio del mes siguiente si la última donación fue a principios de mes

**Estado:** abierto
**Severidad:** alta
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/incentivos/models/entities/Perfil/ProgresoMision.java:109-117`, `src/main/java/ar/edu/utn/frba/ddsi/incentivos/models/ServiciosInternos/scheduler/MisionesScheduler.java:23-26`

### Qué pasa

La constancia en misiones como "Racha" se define por meses calendario consecutivos (punto 26 de Corregidos). Sin embargo, `ProgresoMision.evaluarConstancia` calcula el límite de caducidad sumando la duración directamente a la marca temporal de la última donación:

```java
ImpactoDonacion ultima = donacionesQueProgresaron.get(donacionesQueProgresaron.size() - 1);
LocalDateTime limite = ultima.getFechaEntrega()
        .plus(constancia.getCantidad(), constancia.getUnidadTiempo().comoChronoUnit());

if (fechaEvaluacion.isAfter(limite)) {
    // La racha caduco: el donante arranca de cero.
    reiniciarProgreso();
    return;
}
```

Si un donante realiza su aporte el 1 de octubre a las 10:00, `limite` queda fijado en el 1 de noviembre a las 10:00.
Cuando `MisionesScheduler` corre a la medianoche del 2 de noviembre (00:00):
`fechaEvaluacion.isAfter(limite)` es `true` (2 de noviembre > 1 de noviembre).
El scheduler resetea a cero el progreso del donante (`reiniciarProgreso()`), a pesar de que el mes de noviembre acaba de comenzar y el donante aún dispone de prácticamente todo el mes para cumplir su donación mensual consecutiva.

### Cómo se dispara

Un donante en una misión de constancia mensual dona los primeros días del mes M, y no dona el primer día del mes M+1. El scheduler nocturno borra su progreso el día 2 de M+1.

### Propuesta de arreglo

Para constancias basadas en meses, evaluar la caducidad comparando `YearMonth` (la racha solo vence si `YearMonth.from(fechaEvaluacion)` es posterior al mes siguiente a la última donación), o extender `limite` hasta el fin del mes calendario correspondiente.

---

## 1. La autorización de admin se apoya en un header controlado por el cliente

**Estado:** abierto (decisión explícita del equipo, no corregido a propósito)
**Severidad:** alta
**Archivos:** `controllers/CategoriaController.java`, `controllers/MisionController.java`,
`models/gestores/ValidadorAdmin.java`, `config/SecurityConfig.java`

### Qué pasa

Los endpoints de administración reciben el UUID del administrador en un **header
enviado por el cliente**:

```java
@PostMapping("/admin")
public ResponseEntity<CategoriaDTO> crearCategoria(
    @RequestHeader("Admin-Id") UUID idAdmin,
    @RequestBody CategoriaDTO request) { ... }
```

`ValidadorAdmin` se limita a preguntar a `donaciones-service` si ese UUID existe:

```java
public void verificarPermisos(UUID idAdmin) {
  if (!donacionClient.verificarAdmin(idAdmin)) {
    throw new SecurityException("El usuario no tiene permisos de administrador o no existe.");
  }
}
```

Eso valida **identidad declarada, no autoridad**: cualquiera que conozca el UUID de un
admin puede mandar `Admin-Id: <uuid-del-admin>` y crear, modificar o borrar categorías y
misiones. No hace falta ser ese admin.

Se suma una incoherencia de fondo: conviven dos mecanismos de seguridad que no se hablan.

| Capa | Qué exige | Qué protege |
|---|---|---|
| `SecurityConfig` | HTTP Basic sobre `anyRequest().authenticated()` | que la petición tenga usuario |
| `ValidadorAdmin` + header `Admin-Id` | que el UUID exista en donaciones-service | que el usuario sea admin |

Además, `SecurityConfig` no tiene un `UserDetailsService`: sin usuarios en memoria ni
proveedor externo, `httpBasic` no tiene contra qué validar credenciales, así que en la
práctica levanta un `AuthenticationProvider` vacío. Conviven dos controles y ninguno
está bien anclado a una identidad verificable.

### Por qué no se corrigió

El arreglo correcto requiere decidir el modelo de identidad entre los cuatro
microservicios (¿JWT compartido? ¿gateway con SSO? ¿propagar el `Principal` por
`RestTemplate`?), lo cual excede el alcance de este servicio y afecta a los demás
módulos y al cliente de front.

### Propuesta

1. Emitir un **JWT firmado por un emisor común** en la puerta de entrada (gateway o
   auth-service).
2. Hacer que `SecurityConfig` valide la firma del token y extraiga los roles:
   `hasRole("ADMIN")` sobre las rutas `/api/categorias/admin/**` y
   `/api/misiones/admin/**`.
3. Eliminar el header `Admin-Id` y `ValidadorAdmin`: la identidad viene en el token y la
   auditoría lee el subject autenticado.
4. Mientras tanto, **no exponer el servicio fuera de la red interna** y tratar el header
   `Admin-Id` como no confiable.

---

## 50. `InsigniaObtenida.equals` compara instancias de `Insignia` sin `equals`, permitiendo duplicar medallas en JPA

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/incentivos/models/entities/Perfil/InsigniaObtenida.java:64-78`, `src/main/java/ar/edu/utn/frba/ddsi/incentivos/models/entities/Insignia/Insignia.java`, `src/main/java/ar/edu/utn/frba/ddsi/incentivos/models/entities/Perfil/Perfil.java:125-138`

### Qué pasa

La deduplicación de insignias para evitar que un colaborador reciba la misma medalla dos veces (punto 28 de Corregidos) se apoya en el `Set<InsigniaObtenida>` de `Perfil`:

```java
boolean esNueva = insigniasObtenidas.add(new InsigniaObtenida(this, insignia));
```

`InsigniaObtenida` define `equals` y `hashCode` delegando en sus atributos `perfil` e `insignia`:

```java
return Objects.equals(perfil, otra.perfil)
        && Objects.equals(insignia, otra.insignia);
```

Sin embargo, ni `Insignia` ni `Perfil` implementan `equals` ni `hashCode`.
Además, en `InsigniaObtenida`, `insignia` está mapeada con `FetchType.LAZY`. Cuando el perfil se recupera de la base de datos, las insignias obtenidas previamente contienen proxies de Hibernate (`Insignia$HibernateProxy`). Al ganar una misión (o recompletar una misión compartida entre categorías, como ocurre en el seed entre "Sostenedor" y "Transformador"), `insignia` es la entidad instanciada desde la misión activa.
Dado que `Insignia` no sobreescribe `equals`, la comparación `proxy.equals(entidad)` usa identidad de memoria (`==`) y da `false`, con hash codes divergentes.
El `Set.add` añade la insignia nuevamente, se inserta una fila duplicada en `insignia_obtenida` (que carece de unique constraint en base de datos), y se reemite el evento `MisionCompletada`, disparando notificaciones y publicaciones a n8n repetidas.

### Cómo se dispara

Un donante que ya tiene una insignia vuelve a completar la misma misión (por ejemplo, en otra categoría de la secuencia donde se reutiliza la misión, o ante un recálculo). Al estar el perfil hidratado desde la base de datos, el `Set` no detecta el duplicado.

### Propuesta de arreglo

Implementar `equals` y `hashCode` en `Insignia` basados en `idInsignia` (o comparar `getIdInsignia()` en `InsigniaObtenida.equals`), y agregar un `@UniqueConstraint(name = "uk_insignia_obtenida_perfil_insignia", columnNames = {"perfil_id", "insignia_id"})` en la tabla `insignia_obtenida`.

---

## 51. `buscarSiguienteParaEnviar` en la outbox de n8n bloquea pesimistamente toda la tabla de pendientes sin paginación (`LIMIT 1`)

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/incentivos/models/repositories/SpringRepositories/RepositorioPublicacionesPendientes.java:20-27`, `src/main/java/ar/edu/utn/frba/ddsi/incentivos/services/PublicacionesN8nService.java:44-50`, `src/main/java/ar/edu/utn/frba/ddsi/incentivos/clients/N8nClient.java:58-84`

### Qué pasa

En `RepositorioPublicacionesPendientes`:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM PublicacionPendienteN8n p "
        + "WHERE p.proximoIntento <= :ahora "
        + "AND (p.leaseHasta IS NULL OR p.leaseHasta <= :ahora) "
        + "ORDER BY p.fechaCreacion, p.id")
List<PublicacionPendienteN8n> buscarSiguienteParaEnviar(
        @Param("ahora") LocalDateTime ahora
);
```

La consulta aplica bloqueo pesimista exclusivo (`SELECT ... FOR UPDATE`) sobre todas las filas coincidentes sin límite SQL (`LIMIT 1`).
`PublicacionesN8nService.reclamarSiguiente` toma sólo la primera fila con `stream().findFirst()`, y `N8nClient.procesarPendientes` ejecuta esto en un bucle de hasta 50 iteraciones seguidas cada 30 segundos.
En cada iteración se bloquea la totalidad de publicaciones pendientes en la tabla. Si en ese intervalo llegan donaciones que completan misiones, el listener de persistencia `N8nClient.encolarInsignia` (que se ejecuta dentro de la transacción de la donación vía `@EventListener`) intenta insertar una fila en `publicacion_pendiente_n8n` y queda en espera pasiva por los cerrojos de tabla/índice. Esto genera cuellos de botella en la ingesta de donaciones, degradación de latencia o excepciones por lock wait timeout.

### Cómo se dispara

Procesamiento del scheduler de n8n con publicaciones pendientes acumuladas concurrente con el registro de donaciones que completan misiones.

### Propuesta de arreglo

Limitar la consulta a un único registro mediante `Pageable` (`PageRequest.of(0, 1)`) o método derivado con `LIMIT 1` (`findFirstBy...`), de modo que el bloqueo `FOR UPDATE` afecte únicamente a la fila que efectivamente se va a reclamar.

---

## 52. `RepositorioNotificacionesPendientes` retiene notificaciones fallidas en memoria sin reintentos ni persistencia

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/incentivos/models/repositories/RepositorioNotificacionesPendientes.java:13-33`, `src/main/java/ar/edu/utn/frba/ddsi/incentivos/clients/NotificacionClient.java:51-56`

### Qué pasa

Cuando falla la publicación de una notificación en RabbitMQ, `NotificacionClient.enviarNotificacion` atrapa el error y lo guarda en `RepositorioNotificacionesPendientes`:

```java
} catch (Exception e) {
    log.error("Error al publicar la notificación para {}, guardando en pendientes",
            dto.getDireccionDeContacto(), e);

    repositorioPendientes.guardar(dto);
    throw new EnvioNotificacionException(dto);
}
```

`RepositorioNotificacionesPendientes` acumula los DTOs en un `List<PerfilNotificacionDTO>` en memoria RAM.
Sin embargo:
1. No existe en todo el proyecto ningún worker, scheduler ni consumidor que llame a `listarTodas()` ni reintente enviar los mensajes pendientes.
2. Si la aplicación se reinicia o el pod se recrea, todas las notificaciones pendientes se pierden sin posibilidad de recuperación.
3. La lista interna es un `ArrayList` estándar no sincronizado, por lo que llamadas concurrentes desde distintos hilos pueden provocar `ConcurrentModificationException`.

### Cómo se dispara

Caída momentánea del broker RabbitMQ al momento de confirmarse una donación que completa una misión o cambia de categoría.

### Propuesta de arreglo

Implementar una outbox persistente en base de datos para notificaciones con un scheduler de reintento periódico (idéntico al esquema de `publicacion_pendiente_n8n`), o apoyarse en el mecanismo de reintentos nativo de Spring AMQP con DLQ.

---

## 53. `actualizarDatosPerfil` acepta nombres de usuario compuestos únicamente por espacios en blanco

**Estado:** abierto
**Severidad:** baja
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/incentivos/services/PerfilService.java:390-392`, `src/main/java/ar/edu/utn/frba/ddsi/incentivos/controllers/PerfilController.java:184-192`

### Qué pasa

`PUT /api/perfiles/{idUsuario}` recibe `@RequestBody PerfilDTO perfil` sin la anotación `@Valid`.
En `PerfilService.actualizarDatosPerfil`:

```java
if (dto.getNombreUsuario() != null && !dto.getNombreUsuario().isEmpty()) {
    p.cambiarNombre(dto.getNombreUsuario());
}
```

La condición comprueba `!isEmpty()` en lugar de `!isBlank()`. Si se envía una cadena compuesta únicamente por espacios en blanco (ej. `"   "`), `isEmpty()` devuelve `false`, la condición se cumple y se asigna a la entidad un nombre de usuario en blanco sin sanitizar ni aplicar `trim()`. Esto deja al donante con un nombre invisible en la base de datos, en las consultas públicas y en los rankings desnormalizados.

### Cómo se dispara

Llamar a `PUT /api/perfiles/{idUsuario}` con header `Admin-Id` válido y cuerpo `{ "nombreUsuario": "   " }`. Responde 200 OK y persiste el nombre de usuario compuesto únicamente por espacios en blanco.

### Propuesta de arreglo

Sanitizar con `!dto.getNombreUsuario().isBlank()` y aplicar `.trim()`, o anotar el DTO con `@NotBlank` y activar `@Valid` en el controlador.

---

## 40. La publicación en redes arranca con una coma y la red está fija en `discord`

**Estado:** abierto
**Severidad:** baja
**Archivos:** `.../clients/N8nClient.java`, `.../N8nClientTest.java`

### Qué pasa

`encolarInsignia` arma el `mensaje` de la publicación como
`", por ganar la insignia " + event.insigniaObtenida() + ...`: **arranca con `", "`**, resto de
una concatenación a la que le falta el prefijo, así que el texto que sale a la red empieza con
una coma. Además la red está **hardcodeada en `"discord"`**. El test solo verifica
`containsString`, así que pasa igual con la coma.

### Propuesta

Armar el mensaje completo (por ejemplo `event.nombreUsuario() + " ganó la insignia ..."`), hacer
configurable la red social, y cambiar el test a una igualdad exacta sobre `mensaje`.

---

## 45. La outbox de n8n reintenta para siempre: no hay tope de intentos ni descarte

**Estado:** abierto
**Severidad:** baja
**Archivos:** `services/PublicacionesN8nService.java:70-79`,
`models/entities/PublicacionPendienteN8n.java:74-83`

### Qué pasa

`reintentar` agenda el próximo intento con backoff exponencial acotado a una hora
(`RETRASO_MAXIMO_SEGUNDOS = 3600`) y no hay máximo de intentos ni cola de descarte: una
publicación que nunca va a poder enviarse (payload inválido, red inexistente) se reintenta cada
hora **indefinidamente**. `intentos` y `ultimoError` se guardan, así que el dato está, pero nada
corta el ciclo.

### Cómo se dispara

Una fila en `publicacion_pendiente_n8n` cuyo `POST` a n8n siempre falla (por ejemplo el webhook
responde 4xx por el payload): el scheduler `procesarPendientes` la reintenta en cada ciclo.

### Propuesta de arreglo

Un tope de intentos (`intentos >= N` → estado `DESCARTADA`) o una DLQ, y un log/alerta cuando se
descarta.

---

## 46. Un período de ranking ya existente responde 400 en vez de 409

**Estado:** abierto
**Severidad:** baja
**Archivos:** `services/RankingService.java:124-135`

### Qué pasa

`generarYGuardar` lanza `IllegalArgumentException("Ya existe un ranking para el período: ...")`
cuando `findByPeriodo(...)` encuentra algo. `GlobalExceptionHandler` mapea
`IllegalArgumentException` a **400**, aunque el conflicto con un recurso existente es un **409**
(y `ConflictoException` ya existe para eso). Además, el camino del scheduler
(`crearRankingMensualActual`) usa el mismo método, así que si un admin generó el mes anterior a
mano, el scheduler loguea un **ERROR** por un caso esperado.

### Cómo se dispara

`POST /api/rankings` con un período ya publicado (devuelve 400), o el `RankingScheduler` cuando
el ranking del mes anterior ya existe.

### Propuesta de arreglo

Lanzar `ConflictoException` (409) en el camino HTTP y tratar "ya existe" como no-op idempotente
en el camino del scheduler.

---

## 37. La config de Checkstyle está en el repo pero `mvn` no la aplica

**Estado:** abierto
**Severidad:** baja
**Archivos:** `incentivos-service/config/checkstyle/checkstyle.xml`, `pom.xml`

La configuración que se armó para el punto 23 está versionada. El `pom.xml` tiene un perfil
optativo `checkstyle`, que aplica esa configuración en la fase `verify`; el build habitual no lo
activa y no necesita descargar Checkstyle.

Eso tiene dos consecuencias:

1. Quien clone el repo y ejecute `mvn test` no ejecuta las reglas de estilo; para aplicarlas
   debe correr `mvn -Pcheckstyle -pl incentivos-service -am verify` con acceso a Maven Central.
2. Todo lo que la inspección de IntelliJ marca como `Warning` aparece en el panel de Problems
   junto a las inspecciones propias del IDE, y el panel no distingue de dónde salió cada
   cosa. Por eso el número que se ve no es comparable con el que da la config por separado.

El perfil es optativo para que una compilación offline normal no falle al intentar descargar el
plugin y sus dependencias. Para ejecutarlo, la máquina necesita acceso a Maven Central:

```bash
mvn -Pcheckstyle -pl incentivos-service -am verify
```

`violationSeverity=warning` porque la config deja casi todo en `info` a propósito (ver abajo):
solo las 16 reglas que detectan defectos reales quedan en `warning`, y solo esas hacen
fallar el build. `includeTestSourceDirectory=false` porque la config silencia los
`MissingJavadoc*` en tests de todas formas. Y fase `verify` y no `test` para no frenar el
ciclo rápido.

### El detalle de por qué el panel del IDE marcaba "447 errores"

Con la config de Google por defecto, el panel marcaba 447 errores y 2.087 warnings. Casi
ninguno era un problema del código. De dónde venían:

| Origen | Cuántos | Qué eran |
|---|---|---|
| `config/checkstyle/checkstyle.xml` | ~400 | El IDE valida el XML contra el DTD de Checkstyle, que no tiene descargado, así que marca "Element type must be declared" en cada etiqueta |
| `PENDIENTES.md` | ~20 | Los bloques de código Java del Markdown, parseados como Java |
| Archivos `.java` | 3 | Reales, y de los tres dos estaban en el javadoc nuevo (ver abajo) |

Los dos primeros se arreglan en el IDE, no en el código: en el error de `checkstyle.xml`,
"Accept" el DTD en el aviso, o en *Settings > Languages & Frameworks > Schemas and DTDs*
agregar `https://checkstyle.org/dtds/configuration_1_3.dtd`. Para el `.md`, el problema es
que el inspector de Java está activo sobre todo el árbol y no solo sobre `src/`.

Los tres de Java sí eran reales, y eran del javadoc que se escribió en este punto:
`{@link Operacion}` y `{@link AtributoImpacto}` apuntan a clases de otro paquete que no
están importadas, así que el IDE no las resolvía. Se cambiaron a `{@code ...}`.

**Nota sobre `gen-checkstyle.ps1`:** existe porque `checkstyle.xml` es el Google Checks de
upstream con parches encima, y a mano esos parches se pierden en el primer merge. El script
aplica los mismos parches sobre `google_checks.xml` del jar de Checkstyle y regenera el
archivo. Si algún día se actualiza Checkstyle, hay que volver a correrlo y revisar el diff.

Dos trampas que ya costaron tiempo y quedaron comentadas en el script:

1. Si el XML pierde el `<?xml?>` o el `DOCTYPE`, Checkstyle **no parsea nada y sale con 0
   findings sin avisar**. Un "todo limpio" así no vale nada, por eso `run-checkstyle.ps1`
   chequea el texto crudo en busca de `CheckstyleException` antes de contar.
2. Al parchar con regex, `(?m)^\s*` captura también los saltos de línea anteriores (porque
   `\s` matchea `\n`), y al reinsertar el texto se duplican líneas hasta romper el XML. Hay
   que usar `[ \t]*`.

### Cómo se hizo la config "menos rompebolas"

Google pone **todas** sus reglas en `warning`. Con 3.550 findings, el panel del IDE los
mezcla con los de cualquier otra inspección y el problema real se pierde de vista entre las
preferencias de formato.

La config del proyecto invierte eso: **todo en `info` por defecto, y solo 16 reglas en
`warning`**, las que detectan defectos y no cuestiones de gusto:

`AvoidStarImport`, `EqualsHashCode`, `FallThrough`, `FileTabCharacter`, `IllegalCatch`,
`IllegalImport`, `MissingOverride`, `MissingSwitchDefault`, `MultipleVariableDeclarations`,
`NeedBraces`, `OneStatementPerLine`, `RedundantImport`, `SimplifyBooleanExpression`,
`SimplifyBooleanReturn`, `StringLiteralEquality`, `UnusedImports`, `VisibilityModifier`,
`VariableDeclarationUsageDistance`, `WhitespaceAfter`, `WhitespaceAround`.

Lo que sigue escribiendo sobre diseño —`Indentation`, `LineLength`, `PackageName`,
`AbbreviationAsWordInName`, `MissingJavadoc*`, `CustomImportOrder`, `TextBlock...`— queda en
`info`: documentado y validado, pero no interrumpe. Así el panel muestra solo lo que hay que
arreglar.

El `violationSeverity=warning` del plugin de Maven (arriba, cuando se pueda agregar) está en
sintonía con esto: el build tampoco falla por preferencias de formato.

### Lo que encontró el panel y sí era código muerto

Los warnings del panel no eran todos de estilo. Estos sí son defectos, y se corrigieron:

- **`PerfilService` inyectaba `DonacionClient` y no lo usaba.** Era residuo del punto 12:
  cuando el contacto pasó a resolverse en el `AFTER_COMMIT` del listener, el servicio dejó de
  necesitarlo pero el campo, el import y el parámetro del constructor se quedaron. Con la
  config por defecto era un warning perdido entre 2000; con la config limpia saltó al toque y
  se eliminó.
- **Imports muertos:** 11 archivos tenían imports sin usar que quedaron de refactorings
  anteriores.
- **Visibilidad innecesaria:** métodos de test con modificador `public` cuando JUnit 5 no lo
  necesita.

---

## 23. Higiene del código: setters, logs, nombres y código muerto

**Estado:** abierto (decisiones de identidad y cobertura JPA pendientes)
**Severidad:** baja
**Archivos:** capa de dominio (`models/entities/**`), servicios, controladores y repositorios.

Parte del trabajo grande de este punto ya está hecho; se documenta acá porque fue la pasada
que estaba más clara y se bajó al fondo de la lista a propósito, pero sigue abierto porque
el resto es mantenimiento de cada tanda.

### Lo que se corrigió

**Convenciones de estilo, que las tenía pendientes desde antes de este punto.** El proyecto
nunca venía complying con Google Java Style, que es lo que el plugin de Checkstyle de
IntelliJ trae por defecto. El código usaba indentación de 4 espacios donde Google pide 2,
los paquetes se llamaban `dto.Admin` y `dto.Persona`, y `CategoriaDTO` violaba la regla de
abreviaturas. Con la config por defecto eso son casi 4000 warnings que nadie iba a leer.

Los defectos **que sí son defectos** se corrigieron: tabs, llaves faltantes, imports con
comodín, `//` pegado al texto, operadores al final de línea, variables declaradas lejos de
su uso, statements duplicados, y dos archivos cuya indentación había quedado ilegible
(`MisionService.actualizarMision` y `SincronizacionPerfiles` tenían el cuerpo del lambda
sangrado a 27 y 169 columnas).

Para el resto se agregó `incentivos-service/config/checkstyle/checkstyle.xml`: el Google
Checks de Checkstyle 14.1.0 con las convenciones del proyecto declaradas explícitamente
(indentación de 4, abreviaturas de hasta 3 letras, `dto.Admin` permitido, camelCase en
castellano permitido, javadoc solo en métodos públicos de 3 líneas o más). Es una copia
del upstream y no un archivo propio a propósito: se regenera con `gen-checkstyle.ps1` de
la raíz del repo y se puede comparar contra el original con `diff` para ver exactamente qué
se tocó.

Dos reglas se silencian por ubicación y no globalmente, y las dos tienen el motivo anotado
en el archivo:

- `TextBlockGoogleStyleFormatting` en `**/repositories/**`: el módulo no es configurable en
  14.1.0 (no tiene la propiedad `openingQuotesOnNewLine`), y cumplirlo obliga a tres
  líneas de ceremonia por cada JPQL.
- Los `MissingJavadoc*` en DTO, repositorios y tests, donde el nombre ya describe de qué se
  trata.

Lo que **no** se tocó son las reglas que detectan defectos reales: `NeedBraces`,
`AvoidStarImport`, `FileTabCharacter`, `OperatorWrapNL`,
`VariableDeclarationUsageDistance`, `EqualsHashCode`, `MissingSwitchDefault`, `FallThrough`
e `IllegalCatch` siguen igual que en Google.

Al final se escribió el javadoc que faltaba en las 39 clases y 43 métodos de la capa de
dominio (servicios, controllers, entidades, factories, handlers y schedulers), con lo que
el módulo queda en **0 errores y 0 warnings**.

**Código muerto borrado:**

- Cuatro eventos que nadie publica ni escucha: `CategoriaCambiada`,
  `UltimaMisionCategoria`, `ResultadosRanking` y `GenerarRanking`.
- `RepositorioPerfiles.findByNombreUsuario`, sin un solo uso.
- `Categoria.esUltimaMision`: solo lo llamaban los tests, o sea que el test le daba
  cobertura a algo que el servicio nunca ejecuta. Se sustituyó por la pregunta que sí
  importa de verdad, que es si `siguienteMision` devuelve `null`.
- `RepositorioPerfiles.reiniciarProgresoDeMision`, que era un `default` con lógica de
  negocio dentro de una interfaz de repositorio. Ahora la lógica está en
  `SincronizacionPerfiles`, que es donde estaba el reenvío que no hacía nada.

**Setters fuera de las entidades.** Este era el que de verdad importaba. `Perfil`,
`ProgresoMision`, `Mision`, `Categoria`, `CategoriaMision`, `Insignia`, `Regla`,
`ReglaConstancia`, las tres `Operacion`, `Ranking`, `RankingMensual`, `InsigniaObtenida`,
`ImpactoDonacion` y `MedioContacto` ya no tienen `@Setter`. Cada estado que se escribe
desde afuera pasa ahora por un método que dice qué está pasando:

- `Perfil`: `iniciarEn`, `finalizarSecuencia`, `cambiarNombre`, y las que ya existían
  (`cambiarMision`, `cambiarCategoria`, `progresarMision`).
- `ProgresoMision`: `reiniciarProgreso`, `registrarValorObservado` y
  `limpiarValoresObservados`.
- `ImpactoDonacion`: `registrarProgresoEn(idMision, hizoProgresar)` y
  `registrarSiCompletoMision(completo)`. El `idDonacion` pasó al constructor, que es
  donde debería estar: es la primary key del punto 14 y no quiero que quede como algo que
  se pueda olvidar.
- `Categoria`: `agregarMision`, `eliminarMision`, `moverAPosicion`, y `copiar` que pasó a
  llamarse `actualizarCon` (decir "copiar" de otra categoría no es una copia:
  reconstruye la secuencia y recalcula las posiciones).
- `Insignia`: `actualizar(nombre, descripcion)`, que es lo que necesita la insignia
  objetivo cuando el admin edita la misión.
- `Ranking` y `RankingMensual` quedaron inmutables: el período de un ranking publicado no
  se edita.

El motivo no es estético: con setters públicos `PerfilService` podía llamar
`setProgresoMisionActual(...)` y saltearse los eventos de dominio. El donante avanzaba de
misión y no se publicaba `MisionCambiada`, así que no le llegaban ni la notificación ni la
publicación. `EntidadesSinSettersTest` deja esto fijo: si alguien vuelve a poner un setter,
falla el test y no tres meses después cuando alguien lo use sin querer.

**Logging.** `DonacionClient` pasó a `@Slf4j`. Además de dejar de imprimir a `System.err`
mientras el resto del proyecto usa SLF4J, ahora manda el stack trace completo: antes se
imprimía solo `e.getMessage()`, así que cuando una integración fallaba no quedaba rastro de
dónde había venido el problema. El 4xx de `verificarAdmin` sigue siendo un `warn` sin stack
trace a propósito (es "este id no es de un admin", no un problema de infraestructura) y el
resto pasa a `error` con stack trace.

**Nombres.**

- `copiar` pasó a `actualizarCon`, y `convertirDTO` a `convertirImpactoDonacion`: sonaba a
  "convertir a DTO" pero hacía lo contrario, de DTO a entidad.
- `manageTipoInvalido` pasó a `manejarTipoInvalido`: estaba en inglés entre handlers que
  todos se llaman "manejar...".
- `SecuenciaCategoria` tenía un constructor vacío, y `Perfil.verificarProgresoMision` un
  `if` sin llaves que además ocultaba que el cuerpo entero está en una sola línea.
- Comentarios que ya no aplican: el "aca quiza si haria una interface para repo" de
  `MetricasService`, la nota sobre `PosicionRanking` en `ProgresoMision` y el
  `"la donacion se"` a medio escribir.

**Booleanos envueltos.** `Operacion`, `Regla` y las subclases devuelven `boolean` en vez de
`Boolean`: un `Boolean` de retorno invita a que alguien compare contra `null` sin querer. Lo
mismo con `Perfil.progresarMision` y `PerfilService.actualizarPerfilImpacto`.

`eliminarPerfil` y `eliminarRanking` pasaron a `void`: devolvían `true` o lanzaban, así que
el valor de retorno no le decía nada a nadie. El 404 va por `InexistenteException`, que es lo
que el handler traduce.

Se conservan `Boolean` en dos lugares a propósito: las columnas `hizoProgresarMision` y
`completMision`, que son nullable en la base y pueden venir en `null` de filas viejas, y el
`ResponseEntity<Boolean>` del controller, que es el contrato con `donaciones-service`.

**Warnings del compilador.** Se activó `-Xlint:all` en el `pom.xml` de `incentivos-service` y
se dejaron en cero: `serialVersionUID` en las siete excepciones, `transient` en los dos
campos de `EnvioNotificacionException` y `EnvioPublicacionException` que guardan DTOs no
serializables, y el `this-escape` del constructor de `Categoria`.

El del `this-escape` está suprimido con `@SuppressWarnings` y no "arreglado", y vale la pena
decir por qué: para armar `CategoriaMision` hay que pasarle la categoría, así que
inevitablemente se entrega `this` antes de terminar de construir. Es seguro porque
`CategoriaMision` solo guarda la referencia, y no hay forma de hacerlo al revés sin un
setter, que es justo lo que se sacó.

### Lo que queda abierto

1. **La identidad de admin sigue dependiendo del header `Admin-Id`.** Se mantiene abierto
   hasta acordar autenticación e identidad compartidas con los otros servicios; suspender la
   transacción durante la llamada remota no convierte ese header en una identidad confiable.
2. **La cobertura JPA es inicial, no exhaustiva.** `PersistenciaJpaTest` crea el esquema con
   Hibernate/H2 y prueba persistencia de enums, borrado en cascada al reemplazar una regla,
   suspensión de la transacción ante la llamada remota y persistencia/reclamo de la outbox.
   Los nuevos mappings y consultas igual necesitan casos de integración específicos a medida
   que se agreguen.

---

## 10. El ranking cuenta insignias, pero el modelo y el enunciado dicen misiones

**Estado:** documentado (no relevante: decisión deliberada del equipo de contar insignias)
**Severidad:** documentado
**Archivos:** `models/repositories/SpringRepositories/RepositorioPerfiles.java:60-74`,
`models/entities/Ranking/Ranking.java:28`, `models/entities/Ranking/RankingMensual.java:48`

El enunciado pide que el ranking se determine *"en función de la cantidad de
**misiones cumplidas** durante ese período mensual"*. La decisión del equipo fue
contar **insignias**, así que la consulta cuenta `InsigniaObtenida`:

```java
@Query("SELECT p, COUNT(io) as total " +
    "FROM Perfil p JOIN p.insigniasObtenidas io " +
    "WHERE MONTH(io.fechaObtencion) = :mes AND YEAR(io.fechaObtencion) = :anio " +
    "GROUP BY p ORDER BY total DESC, p.nombreUsuario ASC")
```

El problema no es la consulta, es que **todo el modelo lo nombra de otra cosa**:

| Lugar | Dice | Guarda |
|---|---|---|
| `Ranking.misionesCumplidas` | misiones | `COUNT(insignias)` |
| `RankingMensual.calcularYAgregarPosiciones` | `totalMisiones` | insignias |

Hoy los números coinciden porque cada misión completada otorga exactamente una
insignia. Dejan de coincidir en cuanto una misión se edite y se reutilice una
insignia (ver punto 15), o si algún día se otorga una insignia sin completar
misión.

**Propuesta**

1. Decidir cuál de las dos cosas es la oficial y renombrar el otro extremo para que
   el código no mienta. Si el ranking es "por insignias", el campo debería llamarse
   `insigniasObtenidas` y el enunciado de la entrega debería ajustarse o justificarse.
2. Si se quiere cumplir el enunciado al pie de la letra, el dato ya existe:
   `ImpactoDonacion` guarda `idMision` y `hizoProgresarMision`, así que basta contar
   donaciones con `hizoProgresarMision = true` cuya misión estaba dentro del período.
   Como el progreso solo avanza una misión por vez, "misiones cumplidas en el mes" se
   puede reconstruir, aunque hay que definir bien el período.

---

## 55. `PerfilService.actualizarPerfilImpacto` reintenta en bucle cerrado (tight spin loop) sin backoff ni jitter ante `OptimisticLockingFailureException`

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/incentivos/services/PerfilService.java` (líneas 268-282)

### Qué pasa

En `PerfilService.actualizarPerfilImpacto`:

```java
for (int intento = 1; intento <= MAX_INTENTOS_CONCURRENCIA; intento++) {
    try {
        Boolean resultado = transactionTemplate.execute(estado ->
                aplicarImpacto(idUsuario, dto));
        return Boolean.TRUE.equals(resultado);
    } catch (OptimisticLockingFailureException excepcion) {
        ultimaFalla = excepcion;
        log.warn("Carrera de concurrencia al aplicar la donacion...; intento {} de {}", ...);
    }
}
throw ultimaFalla;
```

Cuando ocurre una colisión concurrente (dos donaciones que impactan sobre el mismo donante al mismo tiempo), la versión de la entidad `@Version` en `Perfil` falla. El bloque `catch` atrapa la excepción y reintenta de inmediato en la siguiente iteración del `for`, sin ninguna pausa.
Al no existir ningún retardo ni jitter aleatorio, dos hilos que compiten vuelven a chocar casi con certeza en el segundo y tercer intento consecutivo, agotando rápidamente `MAX_INTENTOS_CONCURRENCIA` (3 intentos) y lanzando un `OptimisticLockingFailureException` que el `GlobalExceptionHandler` traduce a un HTTP 409 espurio.

### Cómo se dispara

Múltiples eventos de entrega o donaciones concurrentes procesadas en paralelo para un mismo donante.

### Propuesta de arreglo

Introducir una espera breve con backoff exponencial y jitter aleatorio (por ejemplo, entre 20ms y 100ms) antes de cada reintento, o delegar en la anotación `@Retryable(retryFor = OptimisticLockingFailureException.class, backoff = @Backoff(delay = 50, multiplier = 2, random = true))`.

---

## 56. `buscarPerfilesConMisionQueRequiereConstancia` combina `JOIN FETCH` con `Pageable`

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/incentivos/models/repositories/SpringRepositories/RepositorioPerfiles.java` (líneas 69-75), `.../services/PerfilService.java` (líneas 82-112)

### Qué pasa

`RepositorioPerfiles.buscarPerfilesConMisionQueRequiereConstancia` declara:

```java
@Query("SELECT p "
        + "FROM Perfil p "
        + "JOIN FETCH p.progresoMisionActual pm "
        + "JOIN FETCH pm.mision m "
        + "JOIN FETCH m.reglaDeProgreso "
        + "WHERE p.activo = true AND m.reglaDeProgreso.constancia IS NOT NULL")
Page<Perfil> buscarPerfilesConMisionQueRequiereConstancia(Pageable pageable);
```

En JPA/Hibernate y Spring Data, combinar `JOIN FETCH` con un parámetro `Pageable` genera dos problemas críticos:
1. Hibernate no puede aplicar un `LIMIT` / `OFFSET` confiable a nivel de base de datos cuando hay joins fetch sobre asociaciones, produciendo la advertencia: `HHH000104: firstResult/maxResults specified with collection fetch; applying in memory!`. En ese escenario, Hibernate ejecuta la consulta trayendo **todas las filas coincidentes** a memoria y pagina en el heap de Java.
2. La consulta de conteo (`countQuery`) automática generada por Spring Data falla o resulta inválida con JPQL porque la cláusula `FETCH` no está permitida en un `COUNT(...)`.

### Cómo se dispara

Cada ejecución diaria del scheduler de misiones (`MisionesScheduler.evaluarProgresosConstantes()`).

### Propuesta de arreglo

Configurar la consulta base sin `JOIN FETCH` y con `countQuery` explícito, utilizando `@EntityGraph(attributePaths = {"progresoMisionActual", "progresoMisionActual.mision", "progresoMisionActual.mision.reglaDeProgreso"})` para hidratar las relaciones sin corromper el cálculo de paginación de SQL.

---

## 58. No existe el algoritmo de donaciones destacadas: últimas N entregadas que completaron una misión

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/incentivos/models/repositories/SpringRepositories/RepositorioDonaciones.java`, `.../services/MetricasService.java` o un `DestacadasService` nuevo, `.../controllers/` (endpoint nuevo), `../front-service/src/main/resources/templates/landing.html` (sidebar `Donaciones destacadas`), `../front-service/src/main/resources/static/js/landing.js` (array `DONATIONS` de ejemplo)

### Qué pasa

El landing muestra una lista fija de cinco donaciones de ejemplo en `landing.js` (`DONATIONS`). No hay ningún algoritmo que consulte las donaciones reales. El dato necesario ya existe en `incentivos-service`: `ImpactoDonacion` guarda `fechaEntrega`, `estado` y `completMision` (se pone en `true` cuando esa donación completó la misión vigente del donante). Pero no hay query, servicio ni endpoint que devuelva “las últimas N donaciones entregadas que completaron una misión”.

La definición funcional es:

- solo donaciones **entregadas** (`estado = "ENTREGADA"`, que es el valor que manda `IncentivosDonacionDTO`; **no** `ENTREGADO`, que es el nombre del enum local de donaciones);
- solo donaciones que **completaron una misión** (`completMision = true`);
- ordenadas por `fechaEntrega` descendente;
- limitadas a N, con **N configurable** (por defecto 30; el número puede variar).

### Cómo se dispara

Abrir el landing y ver el sidebar “Donaciones destacadas”. Hoy muestra datos de ejemplo; cuando se conecte a datos reales no hay endpoint al que llamar.

### Propuesta de arreglo

1. Agregar una query en `RepositorioDonaciones`:
   ```java
   @Query("""
       SELECT d FROM ImpactoDonacion d
       WHERE d.completMision = true
         AND d.estado = 'ENTREGADA'
         AND d.fechaEntrega IS NOT NULL
       ORDER BY d.fechaEntrega DESC, d.idDonacion DESC
       """)
   List<ImpactoDonacion> buscarDestacadas(Pageable pageable);
   ```
   El desempate por `idDonacion` mantiene el orden estable cuando dos donaciones comparten `fechaEntrega`. Ojo: el filtro correcto es `ENTREGADA`, no `ENTREGADO`; ese último es el nombre del enum local de `donaciones-service` y no viaja así al impacto de incentivos.
2. Crear un `DestacadasService` (o un método en `MetricasService`) que reciba el límite, valide `1 <= limite <= 100`, use `PageRequest.of(0, limite)` y mapee a un DTO público sin datos personales: `idDonacion`, `fechaEntrega`, `cantidadBienes`, `categoria`, `subCategoria`, `entidadBeneficiaria`. No exponer `idUsuario` ni el nombre del donante.
3. Exponer `GET /api/donaciones/destacadas?limite=30` en `incentivos-service`, público y de solo lectura. Si no hay resultados, devolver `200` con lista vacía. El límite por defecto sale de una property, por ejemplo `incentivos.destacadas.limite=30`.
4. Cachear la respuesta unos minutos (`@Cacheable` o `Cache-Control`) porque el landing la pide seguido y solo cambia cuando se entrega una donación que completa misión.
5. En `front-service`, hacer que el landing consuma el endpoint por el BFF (no desde el navegador si se quiere evitar CORS/acoplamiento) y reemplace el array `DONATIONS` de ejemplo. Renderizar la lista server-side con Thymeleaf es lo más liviano; el mapa puede seguir usando los marcadores cuando existan coordenadas (pendientes 75 de donaciones y 69 de logística).
6. Tests: solo entregadas, solo con `completMision = true`, orden descendente, límite configurado, menos resultados que el límite y lista vacía.

### Ojo con los datos viejos

`completMision` tiene default `false`. Las filas de `impacto_donacion` anteriores a ese campo no van a aparecer como destacadas aunque hayan completado una misión. Si se quieren incluir, hay que hacer un backfill/recalculo único; si no, el algoritmo solo empieza a mostrar donaciones nuevas.

---

## 57. `N8nClient.procesarPendientes` procesa la outbox de n8n secuencialmente bloqueando el hilo del scheduler

**Estado:** abierto
**Severidad:** baja
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/incentivos/clients/N8nClient.java` (líneas 62-84)

### Qué pasa

`N8nClient.procesarPendientes()` corre cada 30 segundos con `@Scheduled` y procesa hasta `MAX_PUBLICACIONES_POR_CICLO = 50` publicaciones pendientes:

```java
for (int procesadas = 0; procesadas < MAX_PUBLICACIONES_POR_CICLO; procesadas++) {
    Optional<PublicacionReclamada> siguiente = publicaciones.reclamarSiguiente(LocalDateTime.now());
    if (siguiente.isEmpty()) return;
    PublicacionReclamada reclamada = siguiente.orElseThrow();
    try {
        restTemplate.postForEntity(n8nUrl, reclamada.contenido(), void.class);
        publicaciones.confirmar(reclamada.id());
    } catch (RestClientException exception) {
        ...
    }
}
```

Cada llamada a `restTemplate.postForEntity` es una petición HTTP síncrona y bloqueante contra una URL externa de n8n. Con una latencia habitual de 500ms a 2 segundos por llamada, procesar un lote de 30 o 50 publicaciones retiene el hilo de `@Scheduled` bloqueado durante 30 a 100 segundos. Si el `TaskScheduler` de Spring no tiene un pool dimensionado, retrasa otras tareas programadas del sistema (`MisionesScheduler`, `RankingScheduler`).

### Cómo se dispara

Acumulación de varias insignias en la outbox tras un evento masivo o período de alto tráfico de donaciones.

### Propuesta de arreglo

Despachar las publicaciones reclamadas de forma asíncrona mediante un pool de hilos o `CompletableFuture`, o procesar en sub-lotes más pequeños con timeout estricto.

---

## Corregidos

Un ítem por fix, en el orden en que se cerraron. Los números son los que tenía el punto cuando se
corrigió; los que no llevan número son fixes sin punto propio. Cuando hubo una decisión consciente
que conviene no deshacer sin leer el porqué, va en cursiva en la misma línea.

- **54.** Soft Delete y minimización en `Perfil` preservando métricas de `ImpactoDonacion` y snapshots de `Ranking`: agregado estado `activo = true` en `Perfil`, método de dominio `darDeBaja()` (anonimiza `nombreUsuario = null`, desvincula categoría y anula misiones/insignias vía `orphanRemoval`), se eliminó definitivamente `deleteByIdUsuario` para salvaguardar la contabilidad histórica de donaciones, y las consultas/actualizaciones sobre perfiles inactivos responden 404 (`InexistenteException`).
- **44.** Alta de perfil acota el largo de `nombreUsuario` (`@Size(max = 500)` en `PerfilDonanteDTO` y `@Column(length = 500)` en `Perfil`; rechaza nombres largos con 400 Bean Validation en vez de un 409 por violación de integridad).
- **43.** El historial de rankings pagina en base de datos sin cargar todas las posiciones a memoria: se removió `@EntityGraph` en `RepositorioRankings` y se configuró `@BatchSize(size = 30)` en `RankingMensual.posiciones` para hidratar en sub-lotes.
- **42.** `crearPerfilesEnLote` maneja transacciones independientes por perfil (`TransactionTemplate`) evitando que un error de persistencia marque la sesión en rollback-only y tumbe el lote entero. Errores formateados sin exponer internals de BD.
- **21.** Las rutas de ranking exigen administrador (`Admin-Id` + `ValidadorAdmin`). *El scheduler entra por un método privado sin permisos.*
- **12.** Las llamadas HTTP salieron de las transacciones (timeouts; el contacto se resuelve en `AFTER_COMMIT`). *Sin reintentos automáticos: el `POST` a n8n no tiene idempotency key.*
- **8.** La categoría del donante es visible en `GET /api/perfiles/{idUsuario}/publico` (`permitAll`). *Con DTO aparte; sin categoría devuelve `null`, no 404.*
- **26.** La constancia cuenta meses calendario, no donaciones. *En misiones con constancia, `progreso` cuenta meses.*
- **27.** `conseguirMisiones` respeta el orden del admin (`findAllById` no ordena y la lista **es** la secuencia).
- **28.** La insignia no se otorga dos veces (`LinkedHashSet` con `equals` por `(perfil, insignia)`). *Sin `@UniqueConstraint`; igual devuelve `true` para que el donante avance.*
- **13.** `N8nClient` ya no relanza después del commit.
- **14.** La ingesta de donaciones es idempotente por `ImpactoDonacion.idDonacion` (PK de origen) y `completMision` repite la respuesta. *Se descartó comparar el payload como clave.*
- **7.** Validación de entrada en los DTO (`spring-boot-starter-validation` + `@Valid`; las factories validan la `Regla`). `ChronoUnit.toString()` pasó a `name()`.
- **11.** `ValoresDistintos` guarda el estado en el donante (`ValorObservado`), no en la misión compartida. *Migración en prod: crear la tabla `valor_observado`.*
- **15.** Editar una misión ya no borra el progreso de todos (`Mision.actualizar` decide). *`SuperaCantidad` no compara el umbral.*
- **16.** El progreso de la misión se expone (`progresoFaltante` y desglose) y el DTO no invierte actual/objetivo.
- **18.** Los borrados contestan 409 con la cantidad de referencias, y la guarda va antes de tocar la secuencia.
- **19.** El tope de la secuencia de categorías sale de `listarPosiciones()`, no de `count()`. *Sin `@Column(unique = true)`: rompe los `UPDATE` en bloque.*
- **20.** Códigos de estado consistentes y `desdeEntidad` null-safe; `convertirPerfilADTO` unificado. *Deuda: `obtenerPorId` devuelve `null` y "no existe" lanza dos excepciones distintas.*
- **25.** `crearPerfil` abre transacción (la categoría volvía desligada y el perfil quedaba sin misión en silencio).
- **36.** `@Version` en `Perfil` y `Categoria`, con reintento y 409. *En la raíz del agregado; el reintento usa `TransactionTemplate`.*
- **17.** `orphanRemoval` donde la referencia se reemplaza (`Perfil.progresoMisionActual`, `Mision.reglaDeProgreso`). *No en `insigniaObjetivo` ni en `Regla.constancia`/`operacion`.*
- **30.** Quitar una misión ya no bloquea al donante: retrocede a la más cercana. *Pierde el avance de la misión sacada.*
- **31.** Una posición fuera de rango es 400 (antes `posicionSecuencia: 0` secuestraba la categoría base). *Rango `[1, max]` en edición y `[1, max+1]` en alta.*
- **32.** No se publica el ranking de un período sin cerrar; el "actual" es el último mes cerrado.
- **22.** Consultas de tablas grandes: rango semiabierto en el ranking, agrupado en SQL, N+1 con `@EntityGraph` (`InsigniaObtenida.insignia` a `LAZY`), constancia por bloques de 500 con orden explícito. *La consulta de donaciones por perfil sigue siendo una por perfil; los índices son declaraciones.*
- **33.** "Supera" es estricto (`>`). *El umbral no se compara en `esEquivalenteA`.*
- **34.** Dos guardas: el filtro de misiones normaliza el enum (acentos) y `crearConstancia` rechaza la constancia a medias.
- **24.** La insignia tiene sus tres datos (nombre, descripción e imagen como URL). *URL y no bytes: un `byte[]` rompería la deduplicación del 28.*
- **2.** El ranking se persiste completo; el recorte va al responder. *La tabla de posiciones crece con todos los donantes del mes.*
- **23.** Higiene de código (código muerto, setters, logs, nombres): la parte grande se hizo; quedan tres cosas anotadas en su punto.
- **35.** Los buffers "pendientes" en memoria: notificaciones a RabbitMQ y n8n a una outbox persistente.
- **4.** `common-lib` se eliminó del repositorio: no era un módulo del build ni lo referenciaba nadie.
- **3 / 38.** Las notificaciones van por Rabbit (`notificaciones.exchange`) en vez de HTTP síncrono; se eliminó el 401 del receptor y la ruta muerta.
- **—** AMQP: `spring-boot-starter-amqp` + `RabbitMQConfig` con `Jackson2JsonMessageConverter` (sin él no serializaba el DTO).
- **—** El DTO de notificación pasó a `direccionDeContacto` (el receptor leía ese nombre; el desalineado moría en el INSERT del otro lado). *Sin alias `@JsonProperty`.*
- **—** `SecurityConfig` exigía HTTP Basic con contraseña autogenerada y los otros servicios no podían llamar. *Se dejó el servicio abierto a propósito; se conserva la clase como lugar para reintroducir política.*
- **—** El `@Query` de `findAllByMisionActual` no tenía `FROM`: el servicio no arrancaba (los tests mockeaban el repo).
- **—** Faltaba el bloque `spring.rabbitmq.*`: dentro del contenedor apuntaba a `localhost`.
- **—** Publicaciones a n8n con outbox durable (`publicacion_pendiente_n8n`), lease y reintento con backoff. *Entrega "al menos una vez".*
- **—** `GET /api/metricas/{id}/periodo` daba 500: `Optional<Object[]>` traía el array de filas → proyección tipada `ResumenMetricaDTO`. *Decisión: borde superior inclusivo (`<= :hasta`).*
- **—** `GET /api/metricas/{id}/actividad` daba 500: `obtenerTotalesDonaciones` era `Object[]` → `List<Object[]>` + `get(0)`.
- **—** Regresión de `/periodo`: se restauró el `GROUP BY` (sin donaciones volvía 200 con nulos en vez de 404) y se actualizaron dos asserts de `RendimientoConsultasTest`.
- **—** El id del perfil es ahora el `idUsuario` del donante (se eliminó `idPerfil`); consultas a `findById`/`existsById`/`deleteById`. *Migración: recrear el schema, cambia la PK.*
- **—** La pasada de constancia corre la consulta y el recálculo en la misma transacción (evita `LazyInitializationException` sobre `valoresObservados`).
- **—** Del otro lado: el `__TypeId__` del converter en notificaciones y el default de la URL de n8n.
