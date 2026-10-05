# Pendientes técnicos de `incentivos-service`

Registro de problemas conocidos del servicio, con el motivo y la propuesta de arreglo
para que no se pierdan de vista al crecer el código.

**Están ordenados de más urgente a menos urgente**, no por número de punto. El número es
un ID estable y no se renumera nunca, así que quedan huecos. Un punto corregido se borra
de esta lista y pasa a la sección [Corregidos](#corregidos) del final.

El orden no es el de la severidad declarada en cada punto sino el del daño real: cuánto se
rompe cuando pasa, y qué tan fácil es que pase.

| # | Punto | Por qué está acá |
|---|---|---|
| 1 | 5 | La integración está rota: el servicio no recibe las donaciones |
| 2 | 25 | `crearPerfil` sin transacción: los donantes nuevos no reciben misión y no progresan nunca |
| 3 | 1 | Cualquiera que conozca un UUID de admin puede crear, editar y borrar misiones |
| 4 | 36 | Sin `@Version`: dos donaciones simultáneas pierden progreso y pueden duplicar la insignia |
| 5 | 3 | Requisito explícito del enunciado sin cumplir (cola de mensajes) |
| 6 | 17 | Filas huérfanas que crecen para siempre |
| 7 | 22 | N+1 y tablas enteras en memoria |
| 8 | 30 | Quitar una misión de una categoría bloquea al donante para siempre |
| 9 | 10 | El ranking no cuenta lo que el modelo dice que cuenta |
| 10 | 31 | La secuencia de posiciones acepta valores fuera de rango en silencio |
| 11 | 32 | Se aceptan rankings futuros, y eso rompe el ranking "actual" |
| 12 | 24 | La insignia no tiene descripción propia: es texto derivado |
| 13 | 2 | El podio sale truncado sin avisar |
| 14 | 33 | `SUPERA_CANTIDAD` acepta el valor exacto donde el dominio pide "supera" |
| 15 | 6 | Regla de prevención para no introducir `LazyInitializationException` |
| 16 | 34 | Dos guardas que el código dice tener y no tiene |
| 17 | 23 | Higiene: código muerto, logs, encapsulación |
| 18 | 35 | Los "pendientes" en memoria dicen deduplicar y no deduplican |
| 19 | 4 | `common-lib` es código muerto |
| 20 | 9 | No es un faltante: es una decisión de arquitectura |

El punto 36 va después del punto 1 a propásito: el 1 es determinista (basta conocer un UUID
de admin para sufrir el daño), mientras que el 36 necesita que dos peticiones coincidan en
el tiempo. A igual impacto, el daño que se puede provocar sin condiciones va primero.
---

---

## 25. `crearPerfil` no abre transacción: los perfiles nuevos nunca reciben misión

**Estado:** abierto
**Severidad:** alta
**Salido de:** segunda revisión del servicio (2026-10-05)
**Archivos:** `services/PerfilService.java:66-87`

`crearPerfil` es el **único método de escritura de `PerfilService` sin `@Transactional`**:
los otros cinco (`evaluarConstanciaPerfiles`, `actualizarPerfilImpacto`,
`actualizarDatosPerfil`, `eliminarPerfil`) lo tienen.

```java
public PerfilDTO crearPerfil(PerfilDonanteDTO dto) {          // sin @Transactional
    Perfil nuevo = new Perfil(dto.getIdUsuario(), dto.getNombreUsuario());
    Categoria categoriaBase = repositorioCategorias.findAllByOrderByPosicionSecuenciaAsc()
                                                       .stream().findFirst()
                                                       .orElseThrow(...);
    nuevo.setCategoriaActual(categoriaBase);
    if (categoriaBase.primeraMision() != null) {             // lee una colección LAZY
        nuevo.setProgresoMisionActual(new ProgresoMision(categoriaBase.primeraMision()));
    }
```

**Por qué es grave.** `Categoria.categoriaMisiones` es `@OneToMany(mappedBy = "categoria")`
→ LAZY, y `open-in-view=false`. La consulta del repositorio corre en su propia
transacción read-only, así que al volver `categoriaBase` está **desligada**: no hay sesión
que reenganche la colección. `primeraMision()` hace
`if (this.categoriaMisiones.isEmpty()) return null;`, y sobre una colección desligada eso
falla de una de dos formas, y las dos son bugs:

- lanza `LazyInitializationException` → el alta del donante responde 500, o
- `PersistentBag.isEmpty()` devuelve el tamaño cacheado sin inicializar → devuelve `true`
  en silencio y `primeraMision()` devuelve `null`.

En el segundo caso, que es el más probable y el más silencioso: `POST /api/profiles`
responde 201 con `misionActual: null`, y el perfil queda con
`progresoMisionActual == null` **para siempre**. Como `progresarPerfil` corta en
`if (misionActual != null)`, **ninguna donación posterior de ese donante progresa jamás**:
no completa misiones, no recibe insignias y nunca aparece en el ranking. Es un fallo
funcional total del flujo principal, invisible en los tests porque son unitarios con mocks.

`convertirPerfilADTO(nuevo)` en la línea 86 tiene el mismo riesgo: `Perfil.insigniasObtenidas`
también es LAZY.

**Arreglo:** `@Transactional` en `crearPerfil`. Opcionalmente un
`@EntityGraph(attributePaths = "categoriaMisiones")` en la query, para no depender del
alcance de la transacción.
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
   `donaciones-service`), e incluir el rol como claim.
2. Reemplazar el header `Admin-Id` por el claim de rol, y autorizar con
   `@PreAuthorize("hasRole('ADMIN')")` sobre `SecurityConfig`.
3. Dejar `ValidadorAdmin` como **autorización de negocio** y no de identidad: que valide
   reglas propias (por ejemplo, que la categoría destino exista y que la secuencia sea
   consistente), no "si sos admin".
4. Eliminar el viaje de ida y vuelta a `donaciones-service` en cada operación de admin,
   que además suma latencia a cada escritura.

Mientras tanto, **no exponer el servicio fuera de la red interna** y tratar el header
`Admin-Id` como no confiable.
---

## 36. Sin `@Version`: dos donaciones simultáneas hacen perder progreso

**Estado:** abierto
**Severidad:** alta
**Salido de:** tercera revisión del servicio (2026-10-05), al revisar el punto 28
**Archivos:** `models/entities/Perfil/Perfil.java`,
`models/entities/Perfil/ProgresoMision.java`,
`services/PerfilService.java:158-166`

**Ninguna entidad del servicio tiene `@Version`.** `actualizarPerfilImpacto` es
`@Transactional` y hace leer-modificar-escribir del agregado `Perfil` sin ningún control de
concurrencia. Con dos donaciones del mismo donante entrando al mismo tiempo:

```
T1: lee progreso = 2          T2: lee progreso = 2
T1: progreso++ → 3            T2: progreso++ → 3
T1: save                       T2: save
    progreso final = 3     ← se perdió una donación entera
```

Es un *lost update* clásico: cada transacción parte de una lectura que la otra ya
invalidó, y la segunda escritura pisa a la primera.

### Qué se rompe

**1. Progreso perdido.** Es la falla más común y la más silenciosa: el contador queda
desfasado y el donante tiene que donar de más para completar la misión. En la dirección
"falla hacia el lado que no otorga insignias de más", por eso no es grave, pero es datos
corruptos.

**2. Insignias duplicadas.** Si las dos donaciones completan la misma misión (una misión con
`progresoObjetivo = 1` se completa con una sola donación, así que es el caso más probable),
cada transacción inserta su `InsigniaObtenida`. El `Set` de `Perfil.insigniasObtenidas`
**no las ve**: cada petición tiene su propio objeto `Perfil` en memoria con su propio `Set`,
así que los dos `Set.add` devuelven `true`. Es exactamente el bug del punto 28, pero por la
ventana de la concurrencia en lugar de por el reintento. Y como el ranking cuenta
`COUNT(insigniasObtenidas)`, el donante puntúa doble.

### Por qué el `@UniqueConstraint` no era la respuesta

Se llegó a poner un `@UniqueConstraint` sobre `(perfil_id, insignia_id)` y se quitó, por
tres razones:

1. **No arregla el problema real.** El *lost update* del contador de progreso sigue igual,
   porque el índice solo revisa filas de `insignia_obtenida`.
2. **Empeora el caso perdedor.** La segunda transacción falla por violación de unicidad y
   hace rollback, con lo que **se pierde la donación que sí era legítima**. Es peor que una
   fila duplicada que el `Set` ya evita en el camino normal.
3. **Da una falsa impresión.** Al leer `@UniqueConstraint` parece que la concurrencia está
   controlada, y no lo está.

### Qué hacer

**`@Version` en `Perfil`**, que es la raíz del agregado: es donde viven el progreso, la
categoría y las insignias. Con eso, la segunda transacción falla al hacer el `UPDATE` porque
la versión cambió, y hace rollback sin pisar nada.

Esto resuelve **las dos cosas a la vez** (el contador y la insignia duplicada), que es
justamente lo que el índice único no lograba.

**Lo que cambia en el comportamiento:** hoy la concurrencia se pierde en silencio;
con `@Version` la perdedora recibe un error de concurrencia. Eso hay que decidirlo
explícitamente y no dejarlo como un 500:

1. Mapear `ObjectOptimisticLockingFailureException` a **409** con un mensaje de "ya hubo otra
   donación al mismo tiempo, reintentá", que es un 409 reintentable por definición.
2. Opcionalmente reintentar unas pocas veces dentro del service, que para este caso es
   seguro: como el `idDonacion` es la primary key (punto 14), un reintento con la misma
   donación cae en el camino idempotente y no reprocesa nada.

### Nota

El mismo problema afecta a `CategoriaService.actualizarCategoria` y a
`SincronizacionPerfiles`, que también hacen leer-modificar-escribir sin control de
concurrencia. El punto se centra en `Perfil` porque es donde está el daño más visible,
pero la solución es la misma en los tres.
---

## 3. Las notificaciones van por HTTP síncrono y el requisito pide cola de mensajes

**Estado:** abierto (requisito explícito del enunciado desde la Entrega 4)
**Severidad:** alta
**Archivos:** `clients/NotificacionClient.java`,
`models/repositories/RepositorioNotificacionesPendientes.java`

El enunciado pide, para la Entrega 4, que *"la comunicación entre los servicios de dominio
(incluido Incentivos) y el Servicio de Notificaciones"* sea **asíncrona mediante una cola
de mensajes**, *"garantizando la disponibilidad ante picos de carga o fallas
transitorias"*.

Hoy `NotificacionClient` va con `RestTemplate` contra
`${servicio.notificaciones.url}` y los tres eventos (`MisionCompletada`,
`MisionCambiada`, `CategoriaNuevaPublicar`) se atienden en `@TransactionalEventListener`.
Eso es una llamada bloqueante: si notificaciones-service está caído o tarda, el flujo queda
esperando el timeout, y no hay reintento.

El ángulo positivo: **el equipo ya usa RabbitMQ para otra cosa**, así que la
infraestructura no hay que inventarla. El trabajo es cablear este flujo al broker.

Lo que falta en el repositorio, hoy:

- `spring-boot-starter-amqp` no está en el `pom.xml`.
- No hay `RabbitTemplate`, `@RabbitListener` ni cola declarada.
- El `docker-compose.yml` no levanta un broker.

**Propuesta**

1. Declarar `spring-boot-starter-amqp` y agregar el broker al `docker-compose.yml`
   (reusando la instancia que ya se usa para el otro flujo).
2. Reemplazar la llamada en `NotificacionClient` por publicación a una cola, conservando
   los tres eventos. El `@TransactionalEventListener` con fase `AFTER_COMMIT` ya es el
   momento correcto para publicar: no se manda nada si la transacción no commiteara.
3. `notificaciones-service` pasa a consumir de la cola en vez de exponer el endpoint HTTP.
4. Definir reintentos y cola de mensajes muertos en el broker, más una política de
   descarte (cantidad máxima de intentos o antigüedad).

### Punto anexo: las colecciones "pendientes" viven en memoria

`RepositorioNotificacionesPendientes` y `RepositorioPublicacionesPendientes` son
`ArrayList` dentro de beans `@Repository`: se pierden al reiniciar y no se comparten entre
réplicas. Para notificaciones dejan de tener sentido en cuanto el transporte sea la cola
(el broker ya es el buffer durable). Para las **publicaciones de n8n** el problema sigue
vigente, porque esa integración no está cubierta por el requisito de asincronía.

**Propuesta para n8n:** tabla de outbox transaccional + scheduler de reintento con backoff
exponencial.
---

## 17. Filas huérfanas por `@OneToMany`/`@OneToOne` sin `orphanRemoval`

**Estado:** abierto
**Severidad:** media
**Archivos:** `models/entities/Perfil/Perfil.java:43-44`,
`models/entities/Mision/Mision.java:24-30`,
`models/entities/Mision/Reglas/Regla.java:23-32`

Cinco relaciones son unidireccionales con `cascade = ALL` y **sin `orphanRemoval`**, y en
todas se reemplaza la referencia:

| Relación | Qué queda huérfano |
|---|---|
| `Perfil.progresoMisionActual` | un `ProgresoMision` por cada cambio de misión o de categoría |
| `Regla.constancia` | la `ReglaConstancia` anterior |
| `Regla.operacion` | la `Operacion` anterior, incluido su JSON de valores |
| `Mision.reglaDeProgreso` | la `Regla` anterior completa |
| `Mision.insigniaObjetivo` | la `Insignia` anterior, si se reemplaza |

Cuando Hibernate hace `this.progresoMisionActual = new ProgresoMision(mision)`, inserta
la fila nueva y actualiza la FK del perfil, pero **no borra la fila vieja**: queda en
`progreso_mision` sin que nadie la referencie. Como `cambiarMision` y `cambiarCategoria`
se ejecutan en cada misión completada, la tabla crece de forma indefinida.

`insigniasObtenidas` sí tiene `orphanRemoval = true` (línea 40) y por eso no sufre el
problema: es el ejemplo de cómo debería ser.

**Propuesta:** agregar `orphanRemoval = true` a las cinco relaciones, o borrar
explícitamente la entidad anterior antes de reemplazarla. Con `ddl-auto=update` en
desarrollo conviven las filas viejas con las nuevas, así que la limpieza es aparte.
---

## 22. Consultas N+1 y cargadas completas en memoria

**Estado:** abierto
**Severidad:** media
**Archivos:** `services/PerfilService.java:49-60`,
`services/MetricasService.java:30-60`,
`models/gestores/SincronizacionPerfiles.java:61`,
`models/repositories/SpringRepositories/RepositorioRankings.java`,
`models/entities/Perfil/InsigniaObtenida.java:27-29`

Cuatro problemas de escalabilidad, todos con la misma raíz: se traen tablas enteras al
heap en lugar de resolver en SQL.

1. **`evaluarConstanciaPerfiles`** (el scheduler diario) carga **todos** los perfiles con
   misión de constancia en una lista, y por cada uno lanza una consulta de donaciones.
   Con 10.000 perfiles son 10.001 consultas y toda la colección en memoria. Debería ser
   un `UPDATE` en lote o paginado por bloques.
2. **`obtenerEvolucionHistorica`** lee todas las donating del usuario para agrupar por
   mes en Java. Debería ser un `SELECT year_month, COUNT(*), COUNT(DISTINCT entidad)`
   agrupado en la base.
3. **`SincronizacionPerfiles`** llama a `obtenerContactoPersona` **una vez por
   donante** afectado, en un bucle, dentro de la transacción. Reordenar las misiones de
   una categoría con 500 donantes son 500 llamadas HTTP secuenciales.
4. **`obtenerHistorialRankings`** devuelve `Page<RankingMesDTO>` donde cada elemento
   materializa `posiciones` → 1 consulta por ranking. Y `InsigniaObtenida.insignia` es
   `@ManyToOne(fetch = EAGER)`, así que la paginación de insignias hace 1 consulta por
   insignia de la página.

Aparte, `calcularRankingMensual` filtra con `MONTH(fechaObtencion) = :mes AND
YEAR(fechaObtencion) = :anio`. Las funciones sobre la columna impiden el uso de índices:
es un full scan de `insignias_obtenidas` cada mes. Debería ser un rango
`fechaObtencion >= :inicio AND fechaObtencion < :fin`, que sí es sargable.

**Propuesta:** un `@EntityGraph` para las relaciones que se usan en las lecturas,
`@Query` de agregación para las métricas, paginación por lotes para los schedulers, y
un `INSERT ... SELECT` para el contacto en lugar del bucle. Agregar índices explícitos
sobre `insignias_obtenidas(fechaObtencion)` y `progreso_mision`.
---

## 30. Quitar una misión de una categoría deja al donante bloqueado para siempre

**Estado:** abierto
**Severidad:** media
**Salido de:** segunda revisión del servicio (2026-10-05)
**Archivos:** `models/gestores/SincronizacionPerfiles.java:49-63`,
`models/entities/Perfil/Perfil.java:82-86`

```java
Mision nuevaMision = misionesPorPosicion.get(posicionAnterior);
...
perfil.cambiarMision(nuevaMision, misionActual, contacto);   // nuevaMision puede ser null
```

```java
public void cambiarMision(Mision misionNueva, Mision misionAnterior, MedioContacto contacto) {
    if (misionNueva == null) { this.progresoMisionActual = null; return; }   // ni evento ni reasignación
```

**Escenario de fallo:** la categoría tiene `[A(1), B(2), C(3)]` y hay 40 donantes
haciendo `C`. El admin hace `PUT /api/categorias/admin/{id}` con `"misiones": ["A","B"]`.
Para cada donante `posicionAnterior = 3`, `misionesPorPosicion.get(3) == null`, y entra
el `return` temprano. Consecuencias:

1. `GET /api/profiles/{id}/mision` empieza a devolver 404.
2. Toda donación posterior devuelve `false` en `progresarPerfil` porque
   `misionActual == null`: **el donante queda bloqueado para siempre** aunque la categoría
   todavía tenga misiones disponibles.
3. Como el `return` ocurre **antes** de `registerEvent`, no se emite `MisionCambiada`: el
   donante no se entera nunca de que perdió su misión.

Debería caer en `primeraMision()` de la categoría, que sí está disponible.

**Arreglo:** cuando la posición ya no existe, asignar la primera misión de la categoría y
emitir el evento. Y si la categoría se quedó sin misiones, avisar explícitamente en vez de
dejar el perfil en null en silencio.

---

## 31. `desplazarParaActualizar` descarta la posición pedida en silencio y el service la aplica igual

**Estado:** abierto
**Severidad:** media
**Salido de:** segunda revisión del servicio (2026-10-05)
**Archivos:** `models/gestores/SecuenciaCategoria.java:57-59`,
`services/CategoriaService.java:110-128`, `dto/Admin/CategoriaDTO.java`

El gestor sale sin hacer nada si la posición está fuera de rango:

```java
if (posicionMaxima == null || posicionNueva < 1 || posicionNueva > posicionMaxima) {
    return;                       // retorno silencioso
}
```

Pero el caller **igual escribe la posición pedida**, así que la secuencia queda con huecos
y el invariante "sin huecos" que el propio gestor declara queda roto:

- Secuencia `1..5`, `PUT` con `posicionSecuencia: 10` → `10 > 5` → sin desplazamiento →
  queda `1,2,3,4,5,10`. Un `desplazarHaciaArribaDesde(6)` posterior tampoco lo cierra.
- `posicionSecuencia: 0` → `0 < 1` → sin desplazamiento → categoría en posición 0. Peor:
  `crearPerfil` elige la categoría base con
  `findAllByOrderByPosicionSecuenciaAsc().findFirst()`, así que **todos los donantes
  nuevos pasan a arrancar en esa categoría** en lugar de en la base.

`CategoriaDTO.posicionSecuencia` no tiene `@Positive` ni `@Min(1)`, al contrario que
`ConstanciaDTO.cantidad` y `OperacionDTO.progresoObjetivo`, que sí lo tienen. Nada impide
pedir un valor fuera de rango.

**Arreglo:** `@Min(1)` en el DTO y que el service lance 400 cuando la posición está fuera
de rango, en vez de perder el pedido en silencio.

---

## 32. Se aceptan rankings de períodos futuros y eso rompe el ranking "actual"

**Estado:** abierto
**Severidad:** media
**Salido de:** segunda revisión del servicio (2026-10-05)
**Archivos:** `services/RankingService.java:87-102,113-119`,
`models/repositories/SpringRepositories/RepositorioRankings.java:20,25-31`

`crearRankingMensual` valida que el período no exista, pero **no valida que no sea
futuro**. Y "el ranking actual" se resuelve con `findFirstByOrderByPeriodoDesc()`, o sea el
período más alto existente.

**Escenario de fallo:** `POST /api/rankings {"periodo":"2030-01"}` → 200 con un ranking
vacío (nadie tiene insignias en 2030). A partir de ahí `GET /api/rankings/actual` devuelve
la lista vacía y `GET /api/rankings/{id}/puestoRanking` responde **404 para todos los
usuarios**, aunque el ranking real exista. Queda roto hasta que alguien borre el ranking
futuro. El mes en curso tiene el mismo problema: siempre sale vacío.

**Arreglo:** rechazar con 400 los períodos `>= YearMonth.now()`.

---

## 33. `SUPERA_CANTIDAD` usa `>=` donde el dominio pide "supera"

**Estado:** abierto
**Severidad:** baja
**Salido de:** segunda revisión del servicio (2026-10-05)
**Archivos:** `models/entities/Mision/Operacion/Operaciones/SuperaCantidad.java:35-37`,
`models/ServiciosInternos/InicializadorCategorias.java:65-73`

```java
return valorConvertido >= cantidadEsperada;
```

La misión semilla "Hábil Donador" se describe como *"Realiza 1 donación que **supera** 6
bienes"* con `cantidad = 6`. Una donación de exactamente 6 cuenta como cumplida, que es un
`>=` donde el dominio pide un `>` estricto: un donante con 6 obtiene la insignia que el
enunciado reserva para los de 7 o más.

**Arreglo:** decidir cuál es la semántica correcta y hacerla explícita en el nombre de la
operación, o ajustar el dato semilla para que no haya ambigüedad.

---

## 34. `obtenerTodas` parsea el enum sin normalizar y `crearConstancia` contradice su javadoc

**Estado:** abierto
**Severidad:** baja
**Salido de:** segunda revisión del servicio (2026-10-05)
**Archivos:** `models/repositories/SpringRepositories/RepositorioMisiones.java:46-48`,
`models/entities/Mision/Factory/MisionFactory.java:103-106`

Dos cosas del mismo estilo, "parece validado pero no lo está":

**a) El enum se parsea sin la normalización que sí usa el POST.** `obtenerTodas` hace
`AtributoImpacto.valueOf(str.trim().toUpperCase())`, mientras que
`MisionFactory.crearAtributoImpacto` usa un `normalizar()` que quita acentos. Entonces
`GET /api/misiones?atributo=CATEGORÍA` (con acento, como lo escribe una persona) devuelve
400 con el mensaje crudo de Java, mientras que el `POST` acepta esa misma cadena. Además
el `valueOf` solo sobrevive porque `GlobalExceptionHandler` mapea
`IllegalArgumentException` a 400; si ese handler cambia, pasa a ser 500.

**b) `crearConstancia` dice rechazar y no rechaza.** Su javadoc afirma que si viene solo una
de las dos partes *"es un error y no una ausencia de constancia, así que se rechaza en vez
de ignorarse"*, y el código hace
`if (cantidadTiempo == null || unidadTiempo == null || ...) return null;`. Por HTTP está
cubierto porque `ConstanciaDTO` tiene `@NotNull` + `@NotBlank` en ambos campos, pero
cualquier llamada interna (un scheduler, un test) crea una misión **sin exigencia de
racha** creyendo que sí la tiene, y no se detecta en ningún lado.

**Arreglo:** reusar el normalizador en el repositorio y envolver en
`DatosInvalidosException`; y hacer que `crearConstancia` cumpla lo que dice su javadoc.

---

## 35. Los "pendientes" en memoria dicen deduplicar y no deduplican

**Estado:** abierto
**Severidad:** baja
**Salido de:** segunda revisión del servicio (2026-10-05)
**Archivos:** `models/repositories/RepositorioNotificacionesPendientes.java`,
`models/repositories/RepositorioPublicacionesPendientes.java`,
`dto/*/PerfilNotificacionDTO.java`, `dto/*/PerfilPublicacionDTO.java`

`guardar(...)` deduplica con `pendientes.contains(dto)`, pero `PerfilNotificacionDTO` y
`PerfilPublicacionDTO` solo tienen `@Getter/@Setter`, sin `@EqualsAndHashCode`. El
`contains` compara por **identidad**, así que nunca deduplica: dos objetos con los mismos
datos se guardan los dos.

El impacto es bajo porque el buffer es transitorio, pero el código dice que deduplica y no
lo hace. Cuando el buffer de notificaciones migre a la cola (punto 3) deja de importar,
pero el de publicaciones n8n sigue en pie.

**Arreglo:** `@EqualsAndHashCode`, o guardar por clave en un `Map` en vez de en una `List`.
---

## 10. El ranking cuenta insignias, pero el modelo y el enunciado dicen misiones

**Estado:** abierto (divergencia conocida y aceptada a propósito)
**Severidad:** media
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

## 24. La insignia no tiene descripción propia: es texto derivado del nombre de la misión

**Estado:** abierto
**Severidad:** baja (es un problema de modelo y de texto, no de lógica)
**Archivos:** `models/entities/Mision/Mision.java:40`, `models/entities/Insignia/Insignia.java:25`
**Salió de:** al corregir el punto 15

El constructor de `Mision` deriva la descripción de la insignia del **nombre de la
misión**, no de la descripción de la insignia, y el parámetro `descripcion` de la
insignia directamente no existe:

```java
public Mision(String nombre, UUID idAdmin, String descripcion, String nombreInsignia, Regla regla) {
    ...
    this.descripcion = descripcion;
    this.insigniaObjetivo = new Insignia(nombreInsignia, nombre);  // <-- "nombre", no "descripcion"
}
```

O sea que la insignia **nunca tiene un texto propio**: hereda el de la misión. Eso es lo
que causaba el bug del punto 15, donde `actualizar` le pasaba `this.descripcion` a la
insignia y por eso cada edición de una misión dejaba el texto de la insignia pegado al
texto de la misión. Eso ya está corregido, pero la causa de fondo sigue: **el texto de
la insignia se deriva de dos fuentes distintas según si creás o editás** (el nombre al
crear, el de la insignia entrante al editar), y ninguna de las dos es el texto de la
insignia.

El enunciado pide que las insignias especifican nombre, descripción e imagen, pero
`MisionDTO` solo tiene `insigniaObjetivo` (un `String`, que es el nombre) y
`Insignia.urlImagen` nunca se setea: queda siempre en `null`. De los tres campos del
enunciado, solo el nombre se puede cargar.

**Propuesta:** agregar `insigniaDescripcion` al DTO de creación y edición, y que `Mision`
deje de inventar el texto. Si no se quiere cambiar la API, al menos que el constructor
sea coherente consigo mismo y use `descripcion` en vez de `nombre`.
---

## 2. El snapshot mensual de ranking guarda solo 10 posiciones

**Estado:** abierto (decisión consciente)
**Archivo:** `services/RankingService.java`

`crearRankingMensual` persiste el top 10 (`RANKING_PREDETERMINADO`). Si después se pide
el podio con un `limite` mayor que 10, el ranking se completa recién hasta 10 y el
resultado se ve truncado sin avisar.

Se sacó el `FETCH FIRST 10 ROWS ONLY` de la query para que el corte pase a ser un
parámetro, pero el **snapshot** sigue siendo finito.

**Propuesta:** persistir el ranking completo (o un tope alto y configurable) y aplicar el
`limite` solo al responder, que es lo que hace el endpoint `GET /api/rankings/{id}/top`.
---

## 6. `open-in-view` desactivado: revisar cargas perezosas al agregar endpoints

**Estado:** vigilancia
**Archivo:** `src/main/resources/application.properties`

Se fijó `spring.jpa.open-in-view=false` y se marcaron con
`@Transactional(readOnly = true)` los métodos de lectura que tocan colecciones perezosas
(`Categoria.categoriaMisiones`, `RankingMensual.posiciones`, `Perfil.insigniasObtenidas`).

El riesgo es el habitual: un endpoint nuevo que mapee una entidad a DTO **fuera** de una
transacción va a fallar con `LazyInitializationException` en runtime, no al compilar.

**Regla:** todo método de lectura que llame a un `...DTO.desdeEntidad(...)` sobre una
colección necesita `@Transactional(readOnly = true)`.
---

## 23. Higiene: código muerto, logs a `System.err` y setters públicos

**Estado:** abierto
**Severidad:** baja
**Archivos:** varios

Nada de esto rompe nada hoy, pero son cosas que hacen más difícil el trabajo del que
sigue.

**Código muerto, verificado por grep:**

- Eventos que nadie publica ni escucha: `CategoriaCambiada`, `UltimaMisionCategoria`,
  `ResultadosRanking`, `GenerarRanking`. Cuatro records completos que no hacen nada.
- `RepositorioPerfiles.findByNombreUsuario` no se usa en ningún lado.
- `Categoria.esUltimaMision` solo aparece en los tests, nunca en producción. O sea que
  el test le está dando cobertura a algo que el servicio nunca llama.
- `RepositorioPerfiles.reiniciarProgresoDeMision` es un `default` que carga todos los
  perfiles y hace `saveAll`: es lógica de negocio escrita dentro de una interfaz de
  repositorio, y `SincronizacionPerfiles.reiniciarProgresoDeMision` solo lo reenvía.
- `SincronizacionPerfiles` tiene un comentario que dice "aca quiza si haria una
  interface para repo" (`MetricasService.java:38`) y `ProgresoMision.java:94-95` tiene un
  comentario sobre `PosicionRanking` que ya no aplica.

**Logging inconsistente:** `DonacionClient` usa `System.err.println` en tres lugares
(líneas 47, 65, 69) mientras el resto del proyecto usa `@Slf4j`. Peor: imprime solo
`e.getMessage()`, **sin stack trace**, así que cuando una integración falla no queda
rastro de dónde vino el problema.

**Falta el `@Tag` en `RankingController`:** es el único controller sin anotación de tag,
así que sus endpoints no aparecen agrupados en el Swagger.

**Setters públicos en el agregado:** `Perfil`, `ProgresoMision`, `Mision`, `Categoria`,
`Operacion` y las subclases de `Operacion` tienen `@Setter`. Eso permite que desde
cualquier lado se llame a `perfil.setCategoriaActual(...)` o
`progresoMisionActual.setProgreso(0)` y se esquiven los eventos de dominio. En un
agregado DDD, las transiciones tienen que pasar por los métodos de negocio: `cambiarCategoria`,
`cambiarMision`, `progresarMision`. `PerfilService` ya usa setters en dos lugares
(`setProgresoMisionActual(null)`), lo que confirma que la encapsulación no está
realmente vigente.

**Booleanos envueltos:** `PerfilService.progresarPerfil`, `actualizarPerfilImpacto` y
`eliminarPerfil` devuelven `Boolean` en vez de `void` o `boolean`. El controller
`progresarPerfil` compara contra `null` para decidir el 404, lo que sugiere que en
algún momento se consideró devolver null. `void` con excepciones expresses sería más
claro.

**Propuesta:** borrar el código muerto (o, si `GenerarRanking` y `ResultadosRanking`
alcance a usarse, terminar de conectarlos), pasar `DonacionClient` a `@Slf4j` con
stack trace, agregar el `@Tag` faltante, sacar `@Setter` de las entidades y dejar
setters solo donde hacen falta para JPA, y cambiar los `Boolean` de retorno por `void`.
---

## 4. `common-lib` está en el repositorio pero no en el build

**Estado:** abierto
**Archivos:** `common-lib/`, `pom.xml`

La carpeta `common-lib/` existe (con un `target/` old y clases de `Persona` y
`Saludador`) pero **no está declarada en `<modules>`** del POM padre ni la referencia
ningún servicio, así que no se compila ni se distribuye. Es código muerto.

**Propuesta:** o se declara el módulo y se adopta realmente como librería compartida de
los contratos entre servicios, o se borra. Decidirlo antes de seguir acumulando clases
sueltas ahí.
---

## 9. Logística no se integra de forma directa: es decisión de arquitectura

**Estado:** documentado, no es un faltante
**Archivos:** `clients/` (no hay cliente de logística, a propósito)

A primera vista parece un requisito incumplido, porque el enunciado pide integrarse con
el Servicio de Logística *"para evaluar misiones que dependan de la entrega efectiva de
los bienes"*. Pero **no hay conexión directa a propósito**: la cadena de llamadas ya
resuelve eso.

```
logisticas-service  ->  donaciones-service  ->  incentivos-service
```

Logística le informa a Donaciones el estado de la entrega, y Donaciones le traduce a
Incentivos el impacto de la donación. Así que para el requisito de "Donaciones Exitosas"
no hace falta un cliente propio: la información llega, sólo que por un salto.

**Lo que hay que tener en cuenta**

- Ese salto es responsabilidad de `donaciones-service`: si Logística no le avisa,
  Incentivos nunca se entera. Acá no hay nada que hacer.
- La misión "Donaciones Exitosas" evalúa `ESTADO == "RECIBIDA"`, o sea confía en un
  string que escribe otro servicio. Sigue siendo razonable si se acepta ese contrato,
  pero es un acoplamiento fuerte.
- Si alguna vez hace falta distinguir "la entidad recibió" de "logística entregó", esa
  distinción no puede viajar por el atajo de Donaciones y sí exigiría un contrato propio.
  Queda anotado por si el alcance del TP cambia.
---

# Corregidos

Lo que ya está arreglado, para no volver a tocarlo. Los números son los que tenía
cada punto cuando se corrigió, así que no aparecen en la lista de arriba.


---

## 21 + 12 + 8 - corregidos

### 21. Las rutas de ranking exigen administrador

`POST /api/rankings` y `DELETE /api/rankings/{idRanking}` no validaban nada: ni pedían el
header `Admin-Id` ni llamaban a `ValidadorAdmin`, que sí se usaba en categorías y misiones.
Con lo que hay hoy (`anyRequest().authenticated()` y sin `UserDetailsService`), cualquiera
que llegara al servicio podía **crear rankings para meses históricos arbitrarios** y
**borrar rankings ya publicados**.

Ahora ambas rutas piden `@RequestHeader("Admin-Id")` y el service llama a
`verificarPermisos`, igual que el resto de la superficie de administración. La seguridad
sigue siendo la del punto 1 (un header que elige el cliente), pero al menos se exige que
exista un admin, que antes no se exigía nada.

**El scheduler no se salta el control por espalda.** `crearRankingMensualActual()` lo llama
`RankingScheduler`, que corre dentro del proceso y no tiene request ni header. Se lo dejó
delegando en un método privado `generarYGuardar` que es el que no valida permisos, en vez de
pasar por `crearRankingMensual` (que sí lo valida) y tener que inventar un id de admin. La
distinción está documentada en el javadoc de los dos métodos para que no parezca un
agujero: es una entrada interna, no una ruta HTTP.

De paso se agregó el `@Tag` que le faltaba a `RankingController`, que era el único controller
sin anotación (aparecía sin agrupar en el Swagger).

### 12. Las llamadas HTTP salieron de las transacciones

El `RestTemplate` era un `new RestTemplate()` pelado, que usa timeouts **infinitos**. Si un
downstream caído no devuelve error sino que simplemente no responde, el hilo queda
bloqueado para siempre; y como varias de estas llamadas se hacían **dentro de
transacciones**, la conexión del pool de Hikari quedaba retenida mientras tanto. Con 10
conexiones (el default) y 10 requests colgados el servicio entero dejaba de responder,
aunque la base estuviera sana.

**1. Timeouts explícitos.** El bean se movió a `HttpClientConfig` con `RestTemplateBuilder`
y valores configurables: `clientes.http.connect-timeout-ms` (3000 por defecto) y
`clientes.http.read-timeout-ms` (5000 por defecto). El peor caso pasa de "infinito" a
"conexión retenida 8 segundos", que es acotado y el pool se recupera solo.

**2. Ninguna llamada HTTP dentro de una transacción.** Esta era la parte que más dolía, y
no era un rediseño. El problema era que el `MedioContacto` se pedía **dentro** de la
transacción, se guardaba en el evento, y solo se usaba en el listener de `AFTER_COMMIT`: se
retenía una conexión del pool durante una llamada cuyo resultado no se usaba hasta mucho
después.

El arreglo es que los eventos lleven el `idUsuario` en vez del contacto resuelto, y que el
listener lo consulte. `MisionCompletada` ya lo hacía así; ahora `MisionCambiada` y
`CategoriaNuevaPublicar` hacen lo mismo, y `Perfil.cambiarMision` /
`Perfil.cambiarCategoria` dejaron de recibir el contacto.

Con eso desaparecieron **las dos** llamadas dentro de transacciones, incluida la peor, que
era `SincronizacionPerfiles.actualizarMisionesPorCambioDeCategoria`: pedía el contacto
**una vez por donante, dentro de un bucle**, así que reordenar las misiones de una categoría
con 500 donantes eran 500 llamadas HTTP secuenciales con 500 conexiones retenidas. Por eso
`SincronizacionPerfiles` ya no depende de `DonacionClient`.

**Lo que queda, y es secundario:** `ValidadorAdmin.verificarPermisos` sí llama a
`donaciones-service` dentro de la transacción de los servicios de administración. Se dejó
así porque son operaciones de baja frecuencia y quedan acotadas por los timeouts; sacarla
exigiría partir la validación en otra clase, porque llamar a un método `@Transactional`
desde la misma clase no pasa por el proxy.

**Lo que no se hizo, y acá la razón sí es técnica:** reintentos automáticos. La llamada a
n8n es un `POST` que publica en redes sociales: si el webhook procesa la publicación pero la
respuesta se pierde (un timeout de red, un proxy), reintentarlo a ciegas publica dos veces, y
n8n no devuelve un idempotency key. El `GET` de contactos sí sería reintentable, pero quedó
para más adelante junto con backoff y circuit breaker.

`NotificacionClientResuelveContactoTest` fija el comportamiento nuevo: que el listener
resuelve el contacto y que cambiar de misión dentro del agregado no toca la red.

### 8. La categoría del donante es visible públicamente

El enunciado pide que *"la categoría actual debe ser visible públicamente junto al nombre de
usuario"*, y no existía ningún endpoint público: `SecurityConfig` tenía
`anyRequest().authenticated()`, así que hasta el perfil exigía credenciales.

Ahora hay `GET /api/perfiles/{idUsuario}/publico` con `permitAll()`, que devuelve solo el
`nombreUsuario` y el `nombreCategoria`.

Dos decisiones que importan:

1. **Vive en `PerfilController`, pero con DTO aparte** (`PerfilPublicoDTO`, con solo esos
   dos campos). Lo del DTO es lo importante: como la ruta es `permitAll()`, lo que sale de
   ahí queda expuesto, así que no puede devolver el `PerfilDTO` completo (que incluye
   misión vigente, insignias e ids internos) sino algo acotado.
   Lo del controller es secundario: estuvo en uno aparte y terminó sobrando. La regla de
   seguridad se escribe por método y ruta (`GET /api/perfiles/*/publico`), así que se sigue
   viendo igual de bien cuál es el único endpoint abierto, y además el prefijo propio
   obligaba a mantener un `/api/publicos/**` en `SecurityConfig` al lado de un controller
   de un solo método.

2. **Un perfil sin categoría devuelve `nombreCategoria = null`, no un 404.** El donante
   existe y su nombre tiene que poder verse igual; solo falla si el donante no existe.

Esto **no** resuelve el punto 1: la autenticación por header sigue siendo débil, y el
servicio tiene dos mecanismos de seguridad que no se hablan (el `UserDetailsService` falta).
Lo que se resolvió es el requisito puntual del enunciado sobre la categoría pública.

---

## 26 + 27 + 28 - corregidos

### 26. La constancia ahora cuenta meses calendario, no donaciones

La racha se calculaba contando donaciones cuya antigüedad respecto de la anterior no
superaba `constancia.cantidad` en `constancia.unidadTiempo`. O sea que `cantidad` se
usaba como margen en días, y con la misión *"Realiza 1 donación durante 3 meses
consecutivos"* (`constancia = (1, MONTHS)`, `COINCIDENCIAS(3, "ENTREGADA")`) **tres PATCH
en tres días consecutivos completaban la misión de tres meses**.

`evaluarConstancia` ahora cuenta los meses calendario consecutivos hacia atrás desde el
mes de la última donación:

- dos o más donaciones en el mismo mes cuentan **una sola vez**;
- un mes sin donación **corta la racha** en el hueco;
- la racha **caduca** si pasó `cantidad`/`unidadTiempo` desde la última donación.

O sea que `cantidad` y `unidadTiempo` pasaron a tener el papel que corresponde: definir cada
cuánto se puede dejar de donar antes de perder la racha. Con `(1, MONTHS)` el donante tiene
que donar al menos una vez por mes, que es lo que dice el enunciado.

**Ojo con el cambio de significado:** para las misiones con constancia, `progreso` pasó a
contar meses y no donaciones. Como `progresoObjetivo` de la misión semilla "Racha" es 3 y
significa "3 meses", queda coherente. Si alguna otra misión con constancia usara
`progresoObjetivo` pensando en FCA de donaciones, hay que revisarla.

`ConstanciaPorMesesTest` cubre el caso del bug (tres donaciones en tres días) y los límites:
dos en el mismo mes, hueco de un mes, caducidad y donaciones que no progresaron.

### 27. `conseguirMisiones` respeta el orden del admin

`findAllById` genera un `SELECT ... WHERE id IN (...)` sin `ORDER BY`, así que el orden con
que volvía no estaba garantizado. Y ese orden importa más de lo que parece:
`Categoria.agregarMision` asigna `posicion = size + 1`, o sea que **el orden de la lista es
la secuencia de progresión del donante**. Con el orden barajado, el donante arrancaba en
una misión distinta a la que el admin puso primera.

Ahora se resuelve en un `Map` por id y se reordena según los ids pedidos. La deduplicación
del punto 19 se mantiene.

`RepositorioMisionesOrdenTest` simula que la base responde en el orden contrario y verifica
que igual sale en el pedido. El mock del repositorio necesita `Answers.CALLS_REAL_METHODS`
porque `conseguirMisiones` es un método `default` de la interfaz y un mock normal no lo
ejecuta.

### 28. La insignia ya no se otorga dos veces

El seed pone **la misma instancia** de `misionRacha` en dos categorías (`sostenedor` y
`transformador`), y `misionHabilDonador` también. Al cambiar de categoría se crea un
`ProgresoMision` nuevo para la misma `idMision`, y como el historial se busca por
`idMision` sin ventana temporal, en la donación siguiente la racha volvía a estar completa y
se otorgaba la misma insignia otra vez: perfil con la insignia duplicada, segunda
notificación, segunda publicación en n8n y puntaje doble en el ranking.

La corrección es la que planteaste:

1. **`Perfil.insigniasObtenidas` pasa de `List` a `Set`.** `LinkedHashSet` para no perder el
   orden de obtención, que se expone en el perfil y en el historial.
2. **`InsigniaObtenida` necesita `equals`/`hashCode`** por `(perfil, insignia)`. Sin esto un
   `Set` de entidades compara por identidad y no reconoce nada, así que el paso a `Set` no
   habría deduplicado nada. La clave es (perfil, insignia) y no el id, justamente para que
   dos objetos distintos que representan lo mismo se reconozcan.
3. **`Perfil.progresarMision`** usa lo que devuelve `Set.add`: si la insignia ya estaba,
   no se guarda otra vez y **no se dispara `MisionCompletada`**, así que no hay segunda
   notificación ni segunda publicación. Pero devuelve `true` igual, porque el donante
   **sí tiene que avanzar de misión**: si devolviera `false` quedaría trabado en una misión
   que ya no puede volver a completar, ya que la insignia no se le va a volver a otorgar.

`InsigniaSinDuplicadosTest` cubre la identidad, el rechazo del duplicado y el escenario
completo de completar, cambiar de categoría y volver a completar.

**Lo que NO se puso, y por qué.** Se llegó a agregar un
`@UniqueConstraint` sobre `(perfil_id, insignia_id)` para cubrir el caso de dos peticiones
simultáneas con la misma insignia, que un `Set` en memoria no puede ver. Se quitó al
revisarlo: sin `@Version` en ninguna entidad (ver punto 36) el problema real es otro y más
grave, el índice no lo cubría, y además hacía que la transacción perdedora hiciera rollback
y perdiera una donación legítima. Poner el bloqueo optimista en el agregado es lo que
resuelve las dos cosas a la vez.

**Pendiente que queda:** el historial de las misiones con constancia sigue siendo el de
toda la misión, no solo el del intento en curso. Eso hace que el `progreso` se recalcule con
donaciones de intentos anteriores. Ya no produce daño visible (el `Set` evita el
duplicado), pero el número es inflado. Acotarlo requiere que `ProgresoMision` guarde desde
cuándo empieza el intento y que la consulta del historial filtre por esa fecha.

## 13 + 14. El 500 después del commit y la ingesta no idempotente - corregidos

Se corrigieron juntos porque son **una sola cadena de fallo**: el 13 provocaba el 500, el
cliente reintenta y el 14 convertía ese reintento en datos corruptos. Arreglando solo uno,
el otro seguía produciendo el daño.

### 13. `N8nClient` relanzaba la excepción después del commit

`publicarInsignia` es `@TransactionalEventListener(AFTER_COMMIT)`, y en el `catch`
guardaba en pendientes y lanzaba `EnvioPublicacionException`. Ese listener corre dentro
del `afterCommit`, que Spring invoca sin try/catch
(`TransactionSynchronizationUtils.invokeAfterCommit`), así que la excepción subía por el
`processCommit`, salía del `@Transactional` y llegaba al handler HTTP.

O sea: **la transacción ya se había confirmado**, la donación estaba guardada y la insignia
otorgada, pero el donante recibía un 500. Y `EnvioPublicacionException` no estaba registrado
en `GlobalExceptionHandler`.

Ahora el `catch` registra y deja la publicación en pendientes para reintentar, sin relanzar.
Queda la misma asimetría que ya estaba resuelta en `NotificacionClient`, cuyo helper privado
captura la excepción y solo loguea: el bug era justamente la falta de ese helper en
`N8nClient`.

El test `N8nClientTest.guardaLaPublicacionPendienteSiFalla` **codificaba el bug**
(`assertThatThrownBy(...).isInstanceOf(EnvioPublicacionException.class)`), así que se
invirtió a `assertThatNoException()`. Si alguien vuelve a relanzar, el test lo corta.

### 14. La ingesta de donaciones ahora es idempotente

`ImpactoDonacion` generaba su id internamente (`@GeneratedValue`) y no guardaba ningún
identificador del servicio de origen, así que no había forma de reconocer un reintento.
Cada reintento insertaba una fila nueva y volvía a aplicar la regla: progreso inflado y,
en el peor caso, una insignia otorgada antes de tiempo.

La corrección es que **`ImpactoDonacion.idDonacion` es el id de la donación en
`donaciones-service`, guardado tal cual, y además es la primary key local**. Se le saca el
`@GeneratedValue`.

Eso simplifica todo lo que venía después:

1. **La deduplicación es un `findById`.** No hace falta una consulta por columna ni
   comparar el contenido: si la fila existe, la petición es un reintento. La primary key
   ya garantiza la unicidad, así que no hay que agregar un índice único aparte.
2. **`idDonacion` es obligatorio en el DTO** (`@NotNull`). Sin id no hay clave con la que
   deduplicar, y un 400 explícito es preferible a guardar una fila imposible de
   deduplicar. Antes se había puesto un campo `idDonacionOrigen` nullable con un fallback
   que comparaba `(idUsuario, entidadBeneficiaria, fechaEntrega, cantidadBienes)`; ese
   camino se descartó porque el identificador del origen es justamente lo que hace falta, y
   comparar el payload tenía dos problemas: era menos discriminante (dos dones realmente
   distintas con los mismos cuatro campos se tomarían por un reintento) y no estaba
   protegido contra dos peticiones simultáneas, porque no había restricción que lo cubriera.
3. **`ImpactoDonacion.completMision`**: guarda si esa donación completó la misión, para
   poder **repetir la misma respuesta** ante un reintento. Un endpoint idempotente no puede
   devolver algo distinto la segunda vez: el cliente ya recibió `true`, y si recibiera
   `false` lo tomaría por un fallo.
4. **`actualizarPerfilImpacto`** busca por `findById` antes de procesar. Si encuentra la
   fila, devuelve el resultado guardado y no toca ni el perfil ni el histórico.

`PerfilServiceIdempotenciaTest` cubre el reintento con y sin misión completada, la donación
nueva, que el id guardado sea el de origen, y que dos donaciones distintas del mismo
donante se procesen por separado.

**Requisito para el otro servicio:** `donaciones-service` tiene que mandar el id de la
donación. Es parte del contrato ahora, no una mejora opcional: mientras no lo mande, toda
donación entra con un 400.

**Migración en prod:** la columna `id_donacion` deja de autogenerarse y pasa a ser
obligatoria (hoy admite NULL en tablas ya creadas), y se agrega `complet_mision`. Con
`ddl-auto=validate` hay que hacerlo a mano.

---

## 7. Sin validación de entrada en los DTO — corregido

Se agregó `spring-boot-starter-validation` y `@Valid` en todos los `@RequestBody`, con
constraints en los DTO de entrada. Además `MisionFactory` y `OperacionFactory` validan la
integridad de la `Regla` en código, que Bean Validation no puede expresar: `COINCIDENCIAS`
exige `valorEsperado` y `VALORES_DISTINTOS`/`SUPERA_CANTIDAD` exigen `cantidad`.
`GlobalExceptionHandler` mapea `MethodArgumentNotValidException`,
`HttpMessageNotReadableException` y `MethodArgumentTypeMismatchException` a 400.

De paso: `MisionDTO.desdeEntidad` usaba `getUnidadTiempo().toString()`, y
`ChronoUnit.toString()` devuelve `"Months"` en camelCase; ahora usa `name()` y devuelve
`"MONTHS"`, como los demás enums.

---

## 11. `ValoresDistintos` guardaba el estado de la misión, no del donante — corregido

`ValoresDistintos` mutaba una lista de valores que vive en la entidad de la misión, o
sea compartida por todos los que la hacen: al tercer donante la misión figuraba completa
para los tres. Peor: `MAPPER.valueToTree(null)` devolvía `null` y ese `null` se agregaba
a la lista, así que una donación sin `categoria` inflaba el conteo de valores distintos.

Ahora la operación es sólo configuración (`cantValoresDistintos`) y el avance vive del
lado del donante: nueva entidad `ValorObservado` (`progreso_mision_id` + `valor`, con
restricción única) manejada por `ProgresoMision`. Para que la operación pueda leer y
escribir ese estado se agregó la interfaz `ProgresoDelDonante` y el `ProgresoMision` se
la pasa a sí mismo en `calcularProgreso` y `estaCompleta`; el contexto va en la firma y
no en un campo de la operación justamente para que el estado compartido no pueda volver.

Una donación sin el atributo ya no aporta (devuelve `false` y no registra nada). Y cuando
la constancia rompe la racha, `evaluarConstancia` borra los valores observados además de
poner `progreso` en 0; si no, el donante conservaría crédito por categorías de una racha
que ya no existe.

**Migración pendiente en prod:** `ddl-auto=validate` y sin Flyway ni Liquibase, así que
la tabla `valor_observado` hay que crearla a mano donde la base ya existe (en dev se crea
sola con `update`). La columna JSON `valores_distintos` de `operacion` queda sin mapear y
conviene eliminarla.

---

## 15. Editar una mision borraba el progreso de todos los que estaban en ella - corregido

`actualizarMision` reiniciaba el progreso de todos los donantes de la mision SIEMPRE,
haya o no un cambio real. O sea que corregir una errata en la descripcion le costaba el
avance a todos los que estaban por completarla, sin aviso.

Ahora `Mision.actualizar` devuelve si cambio lo que el donante tiene que cumplir, y el
service solo reinicia en ese caso. Ese "que hay que cumplir" es la `Regla`, y se
compara con `Regla.esEquivalenteA` (atributo + constancia + operacion):

- **NO** reinicia: cambios de nombre, descripcion o insignia de la mision.
- **SI** reinicia: cambiar el objetivo, el atributo, el tipo de operacion, o los
  parametros que definen que cuenta.

La comparacion vive en `Operacion.esEquivalenteA`, que cada subclase extiende segun sus
campos, asi que agregar una operacion nueva no compila hasta que defina que es "cambio de
verdad" para ella. Un detalle que va en contra de lo obvio: en `SuperaCantidad` el
`cantidadEsperada` NO se compara, porque subir el minimo exigido no invalida lo que el
donante ya acredito (una donacion de 5 bienes contaba antes y cuenta ahora). En
`ValoresDistintos` y `CantidadCoincidencias` los parametros si se comparan.

De paso se corrigio que `actualizar` le pasaba a la insignia `this.descripcion`, que es la
de la MISION. Ahora recibe la descripcion de la insignia entrante.

**Pendiente que queda:** el constructor de `Mision` deriva la descripcion de la insignia
del NOMBRE de la mision (`new Insignia(nombreInsignia, nombre)`), y ni el DTO ni el
constructor reciben un texto propio para la insignia. O sea que la insignia no tiene
descripcion independiente: sale del nombre de la mision. Arreglarlo en serio requiere
agregar el campo al DTO, asi que no se toco.

---

## 16. El progreso de la misión no se expone, y el DTO invierte dos campos — corregido

`MisionPerfilDTO` tenía los parámetros del constructor en el orden equivocado, así que
`progresoActual` y `progresoObjetivo` salían intercambiados. Se reescribió y se le
agregaron `progresoFaltante` y el desglose de `progresoActual`/`progresoObjetivo`. Del
lado del repositorio, `obtenerProgresoMisionPorIdUsuario` pasó a devolver
`Optional<ProgresoMision>` en vez de `null`, y `PerfilService` suma
`convertirProgresoMisionADTO`.

---

## 18. Integridad referencial al borrar categorias y misiones - corregido

Borrar una categoria con donantes o una mision ya completada reventaba por violacion de
FK: un 500 sin explicacion. Y `eliminarMision` no daba 404 si la mision no existia
(`eliminarMision` del repositorio devolvia `null` en silencio y el controller respondia
204 igual).

Ahora ambos borran con guarda previa y contestan **409** con la cantidad exacta de
referencias que lo bloquean, mediante la nueva `ConflictoException`:

- `eliminarCategoria` cuenta los donantes en `Perfil.categoriaActual`.
- `eliminarMision` cuenta los que estan haciendo la mision y los que ya obtuvieron su
  insignia, y avisa por separado en cada caso.
- Si no existe, responde 404 con `InexistenteException` (antes `eliminarCategoria` usaba
  `EntityNotFoundException` y `eliminarMision` no dava nada: el mismo caso con dos
  comportamientos).

La guarda va ANTES de tocar la secuencia de posiciones: antes el borrado fallaba por FK
despues de haber desplazado las posiciones, dejando la secuencia movida sin haber borrado
nada.

---

## 19. Secuencia de categorias sin garantia de unicidad - corregido

`posicionSecuencia` no tenia garantia de unicidad, y todo el mecanismo de secuencia
asumia que las posiciones iban de 1 a N sin huecos ni repeticiones. Ademas
`desplazarParaActualizar` recibia `count()` como limite superior, lo que da por hecho
justo lo que no estaba garantizado.

Corregido:

- El limite superior ahora sale de `listarPosiciones()` (la posicion realmente ocupada mas
  alta), no de `count()`.
- `agregarCategoria` y `actualizarCategoria` rechazan con 409 una posicion que ya esta
  ocupada, en vez de dejarla pasar en silencio.
- Los `@Modifying` de `RepositorioCategorias` llevan `flushAutomatically = true`.
- Los dos `Collectors.toMap` (en `CategoriaService` y `SincronizacionPerfiles`) llevan
  funcion de merge, para que datos repetidos no revienten con `IllegalStateException`.
- `conseguirMisiones` deduplica los ids y compara conjuntos, no tamanos de lista: antes un
  `{"misiones": ["m1","m1"]}` con m1 existente daba el error falso "una o mas misiones
  solicitadas no existen".
- Se elimino `RepositorioMisiones.eliminarMision`, que devolvia `null` en silencio.

**Decision consciente:** NO se agrego `@Column(unique = true)` a `posicionSecuencia`,
aunque era lo que la propuesta del punto pedia. Es incompatible con los `UPDATE` en bloque de
este gestor: al mover la fila de la posicion 3 a la 4, pisaria a la que todavia sigue en
la 4, y la base rechazaria el UPDATE dejando la secuencia a medias. Garantizarlo con
un trigger, o con UPDATE fila por fila en el orden correcto, exigiria reescribir el
gestor; el chequeo en la capa de aplicacion con un 409 explicito cumple para el tamano de
este servicio.

---

## 20. Códigos de estado inconsistentes y NPE en los `desdeEntidad` — corregido

`MisionDTO.desdeEntidad` armaba `"null"` a mano cuando faltaba un valor, y
`OperacionDTO` hacía lo mismo. Ahora son null-safe y usan `name()` en vez de `toString()`
para los enums. Los services lanzan `InexistenteException` (404) en vez de devolver `null`,
y los controllers sacaron los chequeos de `null` que sobraban.
`PerfilService.convertirPerfilADTO`, que estaba duplicado cuatro veces, se extrajo a un
solo método.

`RankingService.obtenerPuestoRankingActual` ahora tira 404, y `crearRankingMensal` tiene
guard de `periodo` nulo.

**Deuda que quedó:** `RepositorioCategorias` y `RepositorioMisiones` siguen con métodos
`obtenerPorId` que devuelven `null` en vez de `Optional`, y la excepción para "no existe"
no es uniforme: `CategoriaService.eliminarCategoria` lanza `EntityNotFoundException`
mientras `RankingService.eliminarRanking` lanza `InexistenteException` para el mismo caso.
