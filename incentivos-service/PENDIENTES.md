# Pendientes técnicos de `incentivos-service`

Registro de problemas conocidos del servicio, con el motivo y la propuesta de arreglo
para que no se pierdan de vista al crecer el código. Los puntos 1 a 9 son decisiones de
diseño o requisitos del enunciado que todavía no están implementados; los puntos 10 en
adelante son hallazgos de la auditoría del código.

La numeración no se renumera cuando un punto se corrige: los números son IDs estables y
quedan huecos. Un punto corregido se borra de acá, así que esta lista es solo lo que
sigue pendiente.

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

## 5. Desajuste de ruta en la integración desde `donaciones-service`

**Estado:** abierto (requiere tocar otro servicio)
**Archivo:** `donaciones-service/.../clients/IncentivosClient.java`

El cliente de donaciones compone las URLs así:

```java
restTemplate.postForEntity(incentivosUrl, dto, Void.class);            // crear perfil
restTemplate.postForEntity(incentivosUrl + "/" + idUsuario, dto, ...); // registrar impacto
```

Con `INCENTIVOS_URL=http://incentivos-service:8082/api/perfiles`, la primera queda
correcta (`POST /api/perfiles`) pero la segunda apunta a
`POST /api/perfiles/{id}`, mientras que el endpoint real de impacto es
**`PATCH /api/perfiles/donacion/{idUsuario}`**. Además el método HTTP no coincide.

Se corrigió el `INCENTIVOS_URL` del `docker-compose.yml` para que incluya el prefijo
`/api` (antes no lo tenía y por eso ya fallaba), pero el camino y el verbo del impacto
hay que corregirlos en `IncentivosClient`, que está fuera de este servicio.

**Además hay un desajuste de tipo en el mismo contrato.**
`donaciones-service/.../dto/incentivos/IncentivosDonacionDTO` declara
`private LocalDate fechaEntrega`, y `ImpactoDonacionDTO` de este servicio declara
`LocalDateTime`. Jackson no puede convertir `"2026-03-10"` en un `LocalDateTime`, así
que aunque se arreglaran el verbo y el camino, el pedido seguiría fallando. Con el
`HttpMessageNotReadableException` registrado en `GlobalExceptionHandler` ahora eso se ve
claro como un 400 en vez de un 500.

Hay que decidir de qué lado se alinea: cambiar `IncentivosDonacionDTO` a `LocalDateTime`
(pierde la hora, que acá no se usa para nada) o cambiar el DTO de acá a `LocalDate` y
ajustar `ImpactoDonacion` y `MetricasService`, que hoy hacen `YearMonth.from(...)`.
Lo natural es alinear el cliente, que es el que manda.

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

## 8. La categoría no está visible públicamente

**Estado:** abierto
**Severidad:** media
**Archivos:** `config/SecurityConfig.java`, `controllers/PerfilController.java`

El enunciado pide que *"la categoría actual debe ser visible públicamente junto al nombre
de usuario"*.

Hoy **no existe ningún endpoint público**: `SecurityConfig` deja
`anyRequest().authenticated()`, así que hasta el perfil exige credenciales.

El dato sí existe y se devuelve: `GET /api/perfiles/{idUsuario}` responde un `PerfilDTO`
con `nombreUsuario` y `categoriaActual`. Lo que falta es la decisión de a quién se lo
muestra.

Esto además choca con el punto 1: como no hay `UserDetailsService` ni usuarios en
memoria, la autenticación básica no tiene contra qué validar. O sea, hoy "público" en la
práctica depende de cómo se despliegue, y no de lo que dice el código.

**Propuesta**

1. Definir un endpoint de lectura pública y de propósito acotado, sin exponer el perfil
   completo. Por ejemplo `GET /api/publicos/perfiles/{idUsuario}` que devuelva solo
   `nombreUsuario` y `nombreCategoria`, con su `@Operation` de OpenAPI al pie y sin datos
   sensibles.
2. Abrir únicamente esa ruta en `SecurityConfig` (`permitAll()`), dejando el resto en
   `authenticated()`.
3. Si el front ya consume el perfil autenticado, alcanza con relajarlo y filtrar qué
   campos viajan en el DTO.

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

# Auditoría de código (2026-10-04)

Revisión completa de los 95 archivos de `src/main/java` buscando bugs, huecos
funcionales y deuda de diseño. Los puntos siguientes son el resultado; el 1 al 9
son los que ya estaban antes.

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

## 12. `RestTemplate` sin timeouts y llamadas HTTP dentro de transacciones

**Estado:** abierto
**Severidad:** alta
**Archivos:** `IncentivosApplication.java:17-20`,
`services/PerfilService.java:176,185`,
`models/gestores/SincronizacionPerfiles.java:61`

El bean de `RestTemplate` es un `new RestTemplate()` pelado, que usa timeouts por
defecto **infinitos**. Si `donaciones-service`, `notificaciones-service` o `n8n` se
quedan colgados (no devuelven error, simplemente no responden), el hilo queda
bloqueado para siempre.

Peor: ese `RestTemplate` se usa **dentro de transacciones de base de datos**:

- `PerfilService.asignarSiguienteMision` llama a `donacionClient.obtenerContactoPersona`
  para poder construir el evento `MisionCambiada`, y lo hace cuando el donante
  completa una misión.
- `SincronizacionPerfiles.actualizarMisionesPorCambioDeCategoria` lo hace una vez por
  cada perfil afectado, dentro de un `@Transactional`.

Mientras la llamada está bloqueada, la transacción sigue abierta y **ocupa una
conexión del pool de Hikari**. Con 10 conexiones (default) y 10 requests colgados, el
servicio entero deja de responder consultas, aunque la base esté perfectamente sana.

A favor: `spring.threads.virtual.enabled=true` está activo, así que los hilos virtuales
no quedan clavados occupying un hilo de plataforma. Eso **no** salva la conexión de la
base de datos, que sigue retenida.

**Propuesta**

1. Configurar el `RestTemplate` con `connectTimeout` y `readTimeout` explícitos
   (3 a 5 segundos es razonable). A partir de ahí un downstream caído produce un
   error controlado en vez de un cuelgue.
2. Sacar la llamada HTTP de la frontera transaccional: obtener el contacto **antes**
   de abrir la transacción, o resolverlo por evento después del commit.
3. Agregar reintentos con backoff y, si la infraestructura lo permite, un circuit
   breaker para que un servicio caído no consuma el pool entero.

## 13. `N8nClient` propaga la excepción después del commit y el donante recibe 500

**Estado:** abierto
**Severidad:** alta
**Archivos:** `clients/N8nClient.java:46-51`,
`clients/NotificacionClient.java:76-93`

`publicarInsignia` está anotado `@TransactionalEventListener(AFTER_COMMIT)` y en el
`catch` vuelve a lanzar:

```java
} catch (Exception e) {
    repositorio.guardar(publicar);
    throw new EnvioPublicacionException(publicar);   // nadie lo captura
}
```

Un `@TransactionalEventListener` de fase `AFTER_COMMIT` se ejecuta **dentro** del
`afterCommit` de la transacción, que Spring invoca sin try/catch
(`TransactionSynchronizationUtils.invokeAfterCommit`). Si el listener lanza, la
excepción sube por `processCommit` y sale del `@Transactional` hasta el handler HTTP.

O sea: **la transacción ya se confirmó**, la donación quedó guardada y la insignia
otorgada, pero `donaciones-service` recibe un 500. Lo más probable es que lo reintente,
y ahí entra el punto 14.

`EnvioPublicacionException` además no está registrado en `GlobalExceptionHandler`.

`NotificacionClient` hace bien las cosas en este sentido: su helper privado `enviar()`
captura la excepción y solo loguea. `N8nClient` no. La asimetría es el bug.

Bug adjunto: `notificarMisionCompletada` resuelve el contacto con
`donacionClient.obtenerContactoPersona(...)` **antes** de llamar a `enviar()`. Si esa
llamada falla (donaciones-service caído), la excepción sale del listener con la misma
consecuencia del párrafo anterior, y además **la notificación ni siquiera llega a la
lista de pendientes**, porque el fallo ocurre antes de `enviarNotificacion`.

**Propuesta:** que `N8nClient` capture la excepción después de guardar en pendientes, en
lugar de relanzarla, y resolver el contacto de forma tolerante a fallos (si no hay
contacto, se registra y se sigue). Vale la pena cubrir esto con un test que verifique
que el `PATCH /api/perfiles/donacion/{idUsuario}` devuelve 200 aunque n8n esté caído.

## 14. La ingesta de donaciones no es idempotente

**Estado:** abierto
**Severidad:** alta
**Archivos:** `services/PerfilService.java:127-143`,
`models/entities/Actividad/ImpactoDonacion.java:20-48`,
`controllers/PerfilController.java:110-120`

`PATCH /api/perfiles/donacion/{idUsuario}` no tiene ninguna protección contra
repetidos. `ImpactoDonacion` genera su `idDonacion` internamente y **no guarda ningún
identificador de la donación en el servicio de origen**, así que no hay clave natural
para deduplicar.

Escenario: `donaciones-service` manda la donación, n8n falla, el cliente ve el 500 del
punto 13 y reintenta. La segunda llamada:

- inserta un segundo `ImpactoDonacion` con los mismos datos,
- vuelve a aplicar la regla y suma otra vez al `progreso`,
- para una misión de `ValoresDistintos`, agrega el mismo valor (que ya estaba, así que
  no cambia, pero el `progreso++` sí).

El resultado es progreso inflado y, en el peor caso, una insignia otorgada antes de
tiempo. Es el bug más fácil de explotar de todos los listados, porque sólo hace falta
que un cliente HTTP reintente, que es el comportamiento por defecto de cualquier
cliente o proxy.

**Propuesta**

1. Propagar un identificador estable desde `donaciones-service` (el id de la donación
   allá) y agregarlo a `ImpactoDonacion` con un `@Column(unique = true)`.
2. Antes de procesar, intentar insertar; si viola la restricción, devolver la
   respuesta guardada sin reprocesar. Con eso el reintento se vuelve seguro.
3. Mientras tanto, como mitigación barata: registrar las donaciones por
   `(idUsuario, entidadBeneficiaria, fechaEntrega, cantidadBienes)` y descartar
   coincidencias exactas.

## 15. Editar una misión borra el progreso de todos los que están en ella

**Estado:** abierto
**Severidad:** alta
**Archivos:** `services/MisionService.java:62-77`,
`models/entities/Mision/Mision.java:44-71`,
`models/repositories/SpringRepositories/RepositorioPerfiles.java:76-80`

`actualizarMision` hace tres cosas destructivas, y las tres se ejecutan siempre, haya
o no un cambio real:

```java
Mision misionModificada = construirMision(idAdmin, dto);   // 1
misionActual.actualizar(misionModificada);                 // 2
Mision actualizada = repoMisiones.save(misionActual);
gestorSincronizacion.reiniciarProgresoDeMision(actualizada.getIdMision());  // 3
```

1. `construirMision` arma una `Regla`, una `Operacion` y una `Insignia` **nuevas** cada
   vez.
2. `Mision.actualizar` reemplaza la referencia: `this.reglaDeProgreso =
   misionModificada.getReglaDeProgreso()`. La regla vieja queda huérfana (ver punto 18),
   y para `ValoresDistintos` la lista de valores arranca vacía.
3. `reiniciarProgresoDeMision` pone el `progreso` en 0 para **todos** los perfiles que
   están en esa misión.

El punto 3 es el que duele: **cambiar la descripción de una misión borra el avance de
todos los donantes que estaban por completarla**, sin aviso y sin notificación. Y como
el paso 1 reconstruye todo, también se pierde el histórico de valores distintos.

Bug adicional en el mismo `actualizar`: **la insignia se corrompe**. Las dos ramas del
`if` interno terminan con la misma línea:

```java
if (nombreNuevo.equals(this.insigniaObjetivo.getNombre())) {
    this.insigniaObjetivo.setDescripcion(this.descripcion);   // <-- descripción de la MISIÓN
} else {
    this.insigniaObjetivo.setNombre(nombreNuevo);
    this.insigniaObjetivo.setDescripcion(this.descripcion);   // <-- descripción de la MISIÓN
}
```

`this.descripcion` es la descripción de la **misión**, no de la insignia. Cada vez que
se edita una misión, la insignia objetivo queda con la descripción del texto de la
misión. Lo que debería recibir es
`misionModificada.getInsigniaObjetivo().getDescripcion()`.

**Propuesta**

1. Comparar la regla nueva con la existente y **solo** reiniciar el progreso si el
   objetivo, el atributo o la operación cambiaron de verdad.
2. Corregir `setDescripcion`: la insignia debe recibir
   `misionModificada.getInsigniaObjetivo().getDescripcion()`, no `this.descripcion`.
3. Si el objetivo de una misión cambia, decidir explícitamente qué pasa con el progreso
   ya acumulado (reiniciar es válido, pero debería ser una decisión consciente y
   notificada, no un efecto colateral).

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

## 18. Integridad referencial al borrar categorías y misiones

**Estado:** abierto
**Severidad:** media
**Archivos:** `services/CategoriaService.java:117-130`,
`services/MisionService.java:79-83`,
`models/entities/Perfil/InsigniaObtenida.java:23-29`

No hay ninguna guarda para impedir borrar algo que todavía está en uso:

- **`eliminarCategoria`**: `Perfil.categoriaActual` es un `@ManyToOne` sin
  `optional = false`. Si hay donantes en esa categoría, el borrado falla por violación de
  FK y sale un `DataIntegrityViolationException` sin handler, o sea un **500**. Si la
  base está en MySQL con `ddl-auto=update`, la FK existe y el error aparece.
- **`eliminarMision`**: `Mision.insigniaObjetivo` tiene `cascade = ALL`, así que borrar
  la misión borra también la insignia. Pero `InsigniaObtenida.insignia` es un
  `@ManyToOne` sin cascada: si alguien ya obtuvo esa insignia, el borrado revienta por
  FK. O sea, **no se puede borrar una misión que alguien ya completó**, y el error es un
  500 opaco. Además `eliminarMision` no devuelve 404 si la misión no existe: `repoMisiones
  .eliminarMision` devuelve `null` en silencio y el controller responde 204 igual.
- Las filas `CategoriaMision` se borran por cascada, pero las `Mision` que quedaban sin
  categoría **no se borran**: quedan misiones sin categoría que no aparecen en ningún
  lado.

**Propuesta:** antes de borrar, contar referencias y responder 409 Conflict con un
mensaje claro ("la categoría tiene 12 donantes asignados"), o bloquear el borrado y
ofrecer una baja lógica. Y registrar `DataIntegrityViolationException` en
`GlobalExceptionHandler` para que una violación de FK devuelva un mensaje entendible en
lugar de un 500.

## 19. Secuencia de categorías sin garantía de unicidad

**Estado:** abierto
**Severidad:** media
**Archivos:** `models/entities/CategoriaPerfil/Categoria.java:28-30`,
`models/repositories/SpringRepositories/RepositorioCategorias.java:19-52`,
`services/CategoriaService.java:96-104`

`Categoria.posicionSecuencia` no tiene `@Column(unique = true)`, así que dos categorías
pueden compartir posición. Todo el mecanismo de secuencia asume lo contrario:

- `obtenerCategoriaSiguiente` usa
  `findFirstByPosicionSecuenciaGreaterThanOrderByPosicionSecuenciaAsc`. Con dos
  categorías en la misma posición, "la siguiente" es ambigua y el ascenso de categoría
  puede saltear una.
- `desplazarParaActualizar` recibe `repoCategorias.count()` como límite superior,
  asumiendo que las posiciones son `1..count`. Si hay huecos o duplicados, el
  desplazamiento numera mal.
- `desplazarHaciaAbajo` / `desplazarHaciaArriba` son `@Modifying` sin
  `clearAutomatically` ni `flushAutomatically`. Tras un UPDATE masivo, el contexto de
  persistencia puede seguir teniendo entidades `Categoria` con la posición anterior, y
  en `actualizarCategoria` eso juega en contra del `setPosicionSecuencia` que viene
  después.

**Propuesta:** `@Column(nullable = false, unique = true)` en `posicionSecuencia`, agregar
`clearAutomatically = true, flushAutomatically = true` a los `@Modifying`, y probar de
usar `listarPosiciones` en vez de `count()` para calcular el rango.

**Aparte, misiones repetidas en el DTO rompen la actualización de una categoría.**
`CategoriaService.actualizarCategoria:90-94` arma un mapa de posiciones con
`Collectors.toMap`, que **lanza `IllegalStateException: Duplicate key`** si una misma
misión aparece dos veces:

```java
Map<UUID, Integer> posicionesAnteriores = categoriaActual.getCategoriaMisiones().stream()
        .collect(Collectors.toMap(
            cm -> cm.getMision().getIdMision(),
            cm -> cm.getPosicion()
        ));
```

El `PUT /api/categorias/admin/{id}` con `{"misiones": ["m1", "m1"]}` produce un 500 sin
mensaje útil. Y antes de llegar ahí, `conseguirMisiones` ya falla con un error
engañoso: compara `misiones.size() != idMisiones.size()`, así que con una lista con
repetidos de ids **que sí existen** igual responde "Una o más misiones solicitadas no
existen".

Lo que debería hacer: deduplicar los ids antes de resolver, y comparar conjuntos
(`new HashSet<>(idMisiones).size()`) en lugar de tamaños de listas.

## 21. Rutas de administración de ranking sin control de administrador

**Estado:** abierto
**Severidad:** media
**Archivos:** `controllers/RankingController.java:45-49,145-151`,
`models/gestores/ValidadorAdmin.java`

`POST /api/rankings` y `DELETE /api/rankings/{idRanking}` **no verifican al
administrador**: no piden el header `Admin-Id` ni llaman a `ValidadorAdmin`, que sí se
usa en
`POST/PUT/DELETE /api/categorias/admin` y en las rutas de misiones.

Con lo que hay hoy (`anyRequest().authenticated()` y sin `UserDetailsService`), eso
significa que cualquiera que llegue al servicio puede **crear rankings para meses
históricos arbitrarios** y **borrar rankings ya publicados**. Es un problema distinto
del punto 1: ahí el admin se valida con un header que el cliente elige (aclaración de
permisos); acá no hay validación de ningún tipo.

**Propuesta:** aplicar `ValidadorAdmin` a las dos rutas, como ya se hace en el resto de
la superficie de administración. Y de paso, activar el `@Tag` que falta en
`RankingController` (ver punto 23).

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

# Corregidos

Lo que ya está arreglado, para no volver a tocarlo. Los números son los que tenía cada
punto cuando se corrigió, así que no aparecen en la lista de arriba.

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

## 16. El progreso de la misión no se expone, y el DTO invierte dos campos — corregido

`MisionPerfilDTO` tenía los parámetros del constructor en el orden equivocado, así que
`progresoActual` y `progresoObjetivo` salían intercambiados. Se reescribió y se le
agregaron `progresoFaltante` y el desglose de `progresoActual`/`progresoObjetivo`. Del
lado del repositorio, `obtenerProgresoMisionPorIdUsuario` pasó a devolver
`Optional<ProgresoMision>` en vez de `null`, y `PerfilService` suma
`convertirProgresoMisionADTO`.

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
