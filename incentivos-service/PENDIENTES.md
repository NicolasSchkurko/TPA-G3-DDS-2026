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
| 1 | 13 + 14 | Corrompe datos: progreso inflado e insignias otorgadas sin merecer. Basta un reintento HTTP, que es lo que hacen todos los clientes por defecto |
| 2 | 5 | La integración está rota: el servicio no recibe las donaciones |
| 3 | 25 | `crearPerfil` sin transacción: los donantes nuevos no reciben misión y no progresan nunca |
| 4 | 26 | 3 donaciones en 3 días completan la misión de "3 meses consecutivos": la mecánica de constancia está mal |
| 5 | 1 | Cualquiera que conozca un UUID de admin puede crear, editar y borrar misiones |
| 6 | 21 | Las rutas de ranking no validan nada: ni header ni permiso |
| 7 | 12 | Timeouts infinitos dentro de transacciones: un downstream colgado tumba el pool y con él el servicio entero |
| 8 | 3 | Requisito explícito del enunciado sin cumplir (cola de mensajes) |
| 9 | 28 | La misma misión en dos categorías: insignia duplicada y puntaje doble |
| 10 | 27 | El orden de las misiones dentro de una categoría es aleatorio |
| 11 | 17 | Filas huérfanas que crecen para siempre |
| 12 | 22 | N+1 y tablas enteras en memoria |
| 13 | 30 | Quitar una misión de una categoría bloquea al donante para siempre |
| 14 | 10 | El ranking no cuenta lo que el modelo dice que cuenta |
| 15 | 31 | La secuencia de posiciones acepta valores fuera de rango en silencio |
| 16 | 32 | Se aceptan rankings futuros, y eso rompe el ranking "actual" |
| 17 | 8 | Requisito del enunciado no implementado (categoría pública) |
| 18 | 24 | La insignia no tiene descripción propia: es texto derivado |
| 19 | 2 | El podio sale truncado sin avisar |
| 20 | 33 | `SUPERA_CANTIDAD` acepta el valor exacto donde el dominio pide "supera" |
| 21 | 6 | Regla de prevención para no introducir `LazyInitializationException` |
| 22 | 34 | Dos guardas que el código dice tener y no tiene |
| 23 | 23 | Higiene: código muerto, logs, encapsulación |
| 24 | 35 | Los "pendientes" en memoria dicen deduplicar y no deduplican |
| 25 | 4 | `common-lib` es código muerto |
| 26 | 9 | No es un faltante: es una decisión de arquitectura |

Los puntos 13 y 14 están juntos porque son una sola cadena de fallo: el primero provoca el
500, el cliente reintenta y el segundo convierte ese reintento en datos corruptos.
Arreglarlos de a uno no sirve.

---
Registro de problemas conocidos del servicio, con el motivo y la propuesta de arreglo
para que no se pierdan de vista al crecer el código.

**Están ordenados de más urgente a menos urgente**, no por número de punto. El número es
un ID estable y no se renumera nunca, así que quedan huecos. Un punto corregido se borra
de esta lista y pasa a la sección [Corregidos](#corregidos) del final.

El orden no es el de la severidad declarada en cada punto sino el del daño real: cuánto se
rompe cuando pasa, y qué tan fácil es que pase.

| # | Punto | Por qué está acá |
|---|---|---|
| 1 | 13 + 14 | Corrompe datos: progreso inflado e insignias otorgadas sin merecer. Basta un reintento HTTP, que es lo que hacen todos los clientes por defecto |
| 2 | 5 | La integración está rota: el servicio no recibe las donaciones |
| 3 | 25 | `crearPerfil` sin transacción: los donantes nuevos no reciben mión y no progresan nunca |
| 4 | 26 | 3 donaciones en 3 días completan la misión de "3 meses consecutivos": la mecánica de constancia está mal |
| 5 | 1 | Cualquiera que conozca un UUID de admin puede crear, editar y borrar misiones |
| 6 | 21 | Las rutas de ranking no validan nada: ni header ni permiso |
| 7 | 12 | Timeouts infinitos dentro de transacciones: un downstream colgado tumba el pool y con él el servicio entero |
| 8 | 3 | Requisito explícito del enunciado sin cumplir (cola de mensajes) |
| 9 | 28 | La misma misión en dos categorías: insignia duplicada y puntaje doble |
| 10 | 27 | El orden de las misiones dentro de una categoría es aleatorio |
| 11 | 17 | Filas huérfanas que crecen para siempre |
| 12 | 22 | N+1 y tablas enteras en memoria |
| 13 | 30 | Quitar una misión de una categoría bloquea al donante para siempre |
| 14 | 10 | El ranking no cuenta lo que el modelo dice que cuenta |
| 15 | 31 | La secuencia de posiciones acepta valores fuera de rango en silencio |
| 16 | 32 | Se aceptan rankings futuros, y eso rompe el ranking "actual" |
| 17 | 8 | Requisito del enunciado no implementado (categoría pública) |
| 18 | 24 | La insignia no tiene descripción propia: es texto derivado |
| 19 | 2 | El podio sale truncado sin avisar |
| 20 | 33 | `SUPERA_CANTIDAD` acepta el valor exacto donde el dominio pide "supera" |
| 21 | 6 | Regla de prevención para no introducir `LazyInitializationException` |
| 22 | 34 | Dos guardas que el código dice tener y no tiene |
| 23 | 23 | Higiene: código muerto, logs, encapsulación |
| 24 | 35 | Los "pendientes" en memoria dicen deduplicar y no deduplican |
| 25 | 4 | `common-lib` es código muerto |
| 26 | 9 | No es un faltante: es una decisión de arquitectura |

Los puntos 13 y 14 están juntos porque son una sola cadena de fallo: el primero provoca el
500, el cliente reintenta y el segundo convierte ese reintento en datos corruptos.
Arreglarlos de a uno no sirve.

---

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

---

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

## 26. La constancia no exige meses distintos: 3 donaciones en 3 días completan "3 meses consecutivos"

**Estado:** abierto
**Severidad:** alta
**Salido de:** segunda revisión del servicio (2026-10-05)
**Archivos:** `models/entities/Perfil/ProgresoMision.java:119-137`,
`models/ServiciosInternos/InicializadorCategorias.java:47-55`

```java
for (ImpactoDonacion donacion : donacionesEvaluar) {
    LocalDateTime limite = anterior == null ? null
            : anterior.plus(constancia.getCantidad(), constancia.getUnidadTiempo());
    if (limite != null && donacion.getFechaEntrega().isAfter(limite)) {
        progresoActual = 0;
    }
    progresoActual++;
    anterior = donacion.getFechaEntrega();
}
```

La única condición es "esta donación no tiene más de `cantidad` unidades de antigüedad que
la anterior". Con la misión semilla **"Racha"** (descripción: *"Realiza 1 donación durante
3 meses consecutivos"*, `constancia = (1, MONTHS)`, `COINCIDENCIAS(3, "ENTREGADA")`):

| donación | `anterior` | `limite` | ¿excede? | progreso |
|---|---|---|---|---|
| 2026-03-10 | — | — | — | 1 |
| 2026-03-11 | 03-10 | 04-10 | no | 2 |
| 2026-03-12 | 03-11 | 04-11 | no | 3 |

`progreso = 3 >= 3` → `estaCompleta()` → se otorga la insignia "Constancia solidaria".
**Tres PATCH en tres días consecutivos completan una misión de racha de 3 meses.**

La `cantidad` de la constancia se está interpretando como "días de margen entre
donaciones", no como "una donación por mes", y nunca se exige que las donaciones caigan
en meses distintos. No es un detalle del seed: es la mecánica de constancia entera mal
implementada.

**Arreglo:** exigir que cada donación de la racha caiga en un `YearMonth` distinto, o
calcular la racha sobre la cantidad de meses calendario transcurridos y no sobre la
cantidad de donaciones. La segunda es más simple; hay que decidir cuál refleja el
enunciado.

## 27. `findAllById` no preserva el orden: la secuencia de misiones dentro de una categoría es aleatoria

**Estado:** abierto
**Severidad:** media-alta
**Salido de:** segunda revisión del servicio (2026-10-05)
**Archivos:** `models/repositories/SpringRepositories/RepositorioMisiones.java:67-73`,
`models/entities/CategoriaPerfil/Categoria.java:52-56`

`conseguirMisiones` deduplica los ids (arreglo del punto 19) pero los resuelve con
`findAllById`, que genera `SELECT ... WHERE id IN (...)` **sin `ORDER BY`**: el orden con
que vuelve es el que devuelva la base, sin garantía. Y ese orden importa, porque

```java
public void agregarMision(Mision mision) {
    int nuevaPosicion = this.categoriaMisiones.size() + 1;
    this.categoriaMisiones.add(new CategoriaMision(this, mision, nuevaPosicion));
}
```

la posición **es** el orden de la lista, y `Categoria.siguienteMision` avanza con
`posicion + 1`. O sea que el orden de la lista es literalmente la secuencia de progresión
del donante.

**Escenario de fallo:** `POST /api/categorias/admin` con `"misiones": ["uuidHabil",
"uuidPrimera"]` → la base devuelve `[uuidPrimera, uuidHabil]` → el donante arranca en la
misión equivocada. Y es silencioso: el DTO de respuesta sale con el orden ya barajado, así
que el admin no ve el cambio. Afecta también a `actualizarCategoria`, que pasa por
`copiar()`.

**Arreglo:** resolver en un mapa por id y reordenar según `idsUnicos` antes de pasarlos a
`Categoria`.

## 28. La misma misión en dos categorías hace que el donante la re-complete y reciba la insignia duplicada

**Estado:** abierto
**Severidad:** media-alta
**Salido de:** segunda revisión del servicio (2026-10-05)
**Archivos:** `models/ServiciosInternos/InicializadorCategorias.java:95,97`,
`services/PerfilService.java:142-147`,
`models/entities/Perfil/InsigniaObtenida.java:19-31`

El seed pone **la misma instancia** de `misionRacha` en dos categorías (`sostenedor`
línea 95 y `transformador` línea 97), y `misionHabilDonador` también (líneas 96 y 99). Al
cambiar de categoría se crea un `ProgresoMision` nuevo con `progreso = 0` para la **misma**
`idMision`, y el historial se busca por `idMision`:

```java
donaciones = repositorioDonaciones.findByIdUsuarioAndIdMisionOrderByFechaEntregaAsc(
        perfil.getIdUsuario(), misionActual.getIdMision());   // historial completo, sin ventana
```

**Escenario de fallo:** el donante completa "Racha" en Sostenedor (3 donaciones, insignia
"Constancia solidaria"). Al pasar a Transformador arranca de cero para la misma misión. En
la **primera** donación nueva, `progresarPerfil` lee las 3 viejas más la nueva →
`evaluarConstancia` recalcula `progreso = 4 >= 3` → `Perfil.progresarMision` inserta
**otro** `InsigniaObtenida` de la misma insignia y vuelve a disparar `MisionCompletada`.

Consecuencias: el perfil muestra la insignia dos veces, el donante recibe una segunda
notificación y una segunda publicación en n8n, y el ranking puntúa doble (agrava el punto
10). `InsigniaObtenida` no tiene ninguna restricción única sobre `(perfil_id, insignia_id)`
que lo impida.

**Arreglo:** restricción única en `(perfil_id, insignia_id)`, y acotar el historial al
intento en curso (por ejemplo, desde la fecha en que se asignó la misión) en vez de a toda
la misión.

---


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

---

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


---


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


---


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

## 16. El progreso de la misión no se expone, y el DTO invierte dos campos — corregido

`MisionPerfilDTO` tenía los parámetros del constructor en el orden equivocado, así que
`progresoActual` y `progresoObjetivo` salían intercambiados. Se reescribió y se le
agregaron `progresoFaltante` y el desglose de `progresoActual`/`progresoObjetivo`. Del
lado del repositorio, `obtenerProgresoMisionPorIdUsuario` pasó a devolver
`Optional<ProgresoMision>` en vez de `null`, y `PerfilService` suma
`convertirProgresoMisionADTO`.

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
