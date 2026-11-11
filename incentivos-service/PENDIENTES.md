## Revisión del enunciado hasta Entrega 6

El servicio cubre perfiles, progreso de misiones, insignias, métricas/rankings, APIs REST y persistencia
relacional; también hay publicación de eventos a n8n con outbox. Los puntos técnicos abiertos que siguen
abajo afectan seguridad de administración, consistencia/concurrencia, operación de la outbox y reglas de
negocio. E2 requiere además confirmar el alcance funcional completo de analíticas y avisos. Faltan los
diagramas de clases/ERD y la justificación de diseño versionados (pendiente transversal). E6 es del
`front-service`; el backend ya no valida Keycloak, solo acepta tráfico de la red interna.

Nota de vigencia: la baja lógica/anonimización de perfil con preservación de métricas ya figura en
`Corregidos` (punto 54); no reabrirla como borrado físico.

---
# Pendientes técnicos de `incentivos-service`

Registro de problemas conocidos del servicio, con el motivo y la propuesta de arreglo
para que no se pierdan de vista al crecer el código.

**Están ordenados por severidad** (de mayor impacto a menor impacto). El número es
un ID estable y no se renumera nunca, así que quedan huecos. Un punto corregido se borra
de esta lista y pasa a la sección [Corregidos](#corregidos) del final.

| # | Punto | Severidad | Por qué está acá |
|---|---|---|---|
| 1 | 37 | Baja | La configuración Checkstyle no se ejecuta automáticamente en el build |
| 2 | 23 | Baja | Higiene del código: decisiones de identidad y cobertura JPA |
| 3 | 10 | Documentado | Decidir si el ranking cuenta insignias o misiones (decisión documentada, no relevante) |

El punto 10 queda documentado pero no es relevante: el equipo decidió deliberadamente contar insignias.
El punto 23 conserva anotaciones sobre decisiones de identidad y ampliación de cobertura JPA.

El detalle de cada fix está en [Corregidos](#corregidos), un ítem por corrección.

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
dónde había venido el problema. Los 4xx de integración se registran como `warn` sin stack
trace a propósito (son datos que no existen, no una caída) y el resto pasa a `error` con stack
trace.

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

1. **La cobertura JPA es inicial, no exhaustiva.** `PersistenciaJpaTest` crea el esquema con
   Hibernate/H2 y prueba persistencia de enums, borrado en cascada al reemplazar una regla y
   persistencia/reclamo de la outbox. Los nuevos mappings y consultas igual necesitan casos de
   integración específicos a medida que se agreguen.

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

## Corregidos

Un ítem por fix, en el orden en que se cerraron. Los números son los que tenía el punto cuando se
corrigió; los que no llevan número son fixes sin punto propio. Cuando hubo una decisión consciente
que conviene no deshacer sin leer el porqué, va en cursiva en la misma línea.

- **1.** Se eliminó `Admin-Id` y `ValidadorAdmin`: el rol de administrador se valida solo en el front después del login con Keycloak, y los backends se protegen con ACL de red (`seguridad.ips-internas` + `seguridad.servicios-internos`). Se eliminó el prefijo `/interno` y donaciones/logística/incentivos/notificaciones ya no dependen de Keycloak. *El alta de admins sigue usando el `sub` de Keycloak como `Administrador.id`.*
- **66.** Donaciones destacadas: query `buscarDestacadas` (entregadas, `completMision`, orden por fecha), `DestacadasService` con límite 1..100 y default `incentivos.destacadas.limite=30`, endpoint público `GET /api/donaciones/destacadas` con caché de 5 minutos, y landing server-side con Thymeleaf vía BFF. Cobertura en `DestacadasServiceTest`, `PersistenciaJpaTest` e `IncentivosClientTest`. *Sin coordenadas todavía: el mapa queda sin marcadores hasta que logística/donaciones las provean.*
- **46.** Un período de ranking ya existente responde 409 (`ConflictoException`); el scheduler devuelve el ranking existente sin volver a generarlo.
- **40.** El mensaje de la publicación ya no arranca con coma. El campo `redSocial` se eliminó del payload y de la outbox: n8n no lo usa y era una configuración muerta. *Migración: `ALTER TABLE publicacion_pendiente_n8n DROP COLUMN red_social;` en bases existentes.*
- **57.** `N8nClient` despacha cada envío a un pool acotado `n8nExecutor`; el ciclo del scheduler solo reclama y encola. El lease de la outbox sube a 300s para cubrir la cola.
- **45.** La outbox de n8n descarta la publicación tras 10 intentos fallidos, con log de error; la query de reclamo excluye las descartadas.
- **65.** Se eliminó `AdminService`: no estaba cableado a ningún controller y duplicaba operaciones masivas sin paginar de `CategoriaService`/`MisionService`.
- **53.** `actualizarDatosPerfil` ignora nombres compuestos solo por espacios y recorta los espacios de los válidos. Cobertura en `PerfilServiceTest`.
- **64.** Dockerfile con `-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError`, `mem_limit: 512m` en el compose y Hikari/batch configurados (pool 10, batch 50, batch fetch 50).
- **63.** El camino de donaciones usa `findByIdParaDonacion`, cuyo `EntityGraph` no trae `insigniasObtenidas`; `valoresObservados` se acota a `cantValoresDistintos`. Cobertura en `RendimientoConsultasTest`, `PersistenciaJpaTest` y `ProgresoMisionTest`.
- **62.** El alta en lote resuelve la categoría base una sola vez, busca los ids existentes en una query y persiste con `saveAll` en bloques de 100; si un bloque falla, reintenta ítem por ítem. Cobertura en `PerfilServiceTest` (sección `AltaEnLote`).
- **61.** La sincronización de perfiles pagina por categoría y por misión, y las notificaciones se publican desde un executor acotado de 4 hilos, fuera del hilo que commitea. *Residual: sigue habiendo un HTTP por evento; la resolución masiva de contactos queda pendiente.*
- **60.** La pasada de constancia trae las donaciones de cada bloque en una sola consulta por ids y misiones, filtra por la misión vigente y evalúa con `evaluarConstanciaOrdenada`; `reiniciarProgresoDeMision` también pagina. *Residual: la consulta no acota por fecha; trae el historial progresado de las misiones del bloque.*
- **56.** La consulta del scheduler de constancia pagina en SQL: se reemplazó `JOIN FETCH` + `Pageable` por `@EntityGraph` y `countQuery` explícito, evitando `HHH000104` y la paginación en memoria. Cobertura en `RendimientoConsultasTest` y `PersistenciaJpaTest`.
- **55.** El reintento por `OptimisticLockingFailureException` espera con backoff exponencial y jitter (20–100 ms) entre intentos, sin bucle cerrado. Cobertura en `PerfilServiceTest` (sección `Concurrencia`).
- **52.** La notificación que falla al publicarse ya no queda en un buffer en memoria: se persiste en la outbox `notificacion_pendiente` con lease y backoff, y un scheduler en `NotificacionClient` la republica y confirma o reprograma. Se conserva `idSolicitud` para la idempotencia del receptor. Cobertura en `NotificacionClientTest` y `PersistenciaJpaTest`.
- **51.** El reclamo de la outbox de n8n usa `PageRequest.of(0, 1)`, así el bloqueo pesimista afecta solo la fila que se va a enviar. Cobertura en `PublicacionesN8nServiceTest`.
- **50.** La deduplicación de insignias ahora compara `Insignia` por `idInsignia` (`equals`/`hashCode`), tolerando los proxies de Hibernate, y la tabla `insignia_obtenida` tiene un unique constraint `(perfil_id, insignia_id)`. Cobertura en `PerfilTest` (sección `InsigniasSinDuplicados`) y `PersistenciaJpaTest`.
- **59.** Lectura de rankings paginada: `paginaPosiciones` consulta `Ranking` por `rankingMensual.idRanking` y `puesto` con `LIMIT`/`OFFSET` en la base; `/top` limita en SQL, `GET /{id}` y `/actual` aceptan `Pageable`, el historial devuelve `RankingResumenDTO` sin posiciones y `RankingMesDTO.ranking` pasa a ser `Page<RankingDTO>`. Cobertura en `RankingServiceTest` (sección `Snapshot`) y `PersistenciaJpaTest`.
- **58.** Generación del ranking por lotes de 500: proyección `RankingFilaDTO` (id, nombre y total) sin materializar `Perfil`, estado de puestos y empates entre páginas, y `persist` + `flush` + `clear` por lote. Cobertura en `RankingServiceTest` (sección `Snapshot`) y `PersistenciaJpaTest`.
- **49.** La caducidad de la constancia mensual se evalúa por mes calendario, no por el instante exacto de la última donación: si la última fue el 1 de octubre, todo noviembre sigue dentro de la racha. Se respeta la cantidad configurada en `ReglaConstancia` (en meses se compara `YearMonth` con `plusMonths(cantidad)`; en las demás unidades se mantiene la suma de la cantidad). Cobertura en `ProgresoMisionTest` (sección `ConstanciaPorMeses`).
- **48.** `Categoria.siguienteMision` y `eliminarMision` comparan `Mision` por `idMision`, tolerando los proxies de Hibernate; si falta el id (misión nueva) comparan por referencia. Cobertura en `CategoriaTest`.
- **47.** `actualizarCategoria` ya permite mover una categoría a una posición ocupada: se eliminó la guarda `existsByPosicionSecuencia` que devolvía un 409 espurio y se dejó que `SecuenciaCategoria.desplazarParaActualizar` valide el rango y corra en bloque a las categorías del medio. Las categorías con posición actual `null` se incorporan como un alta. Cobertura en `CategoriaServiceTest` y `SecuenciaCategoriaTest`.
- **39.** La baja del donante viaja por la outbox durable de `donaciones-service` (`ELIMINAR_PERFIL`) con lease, backoff y reintentos, en la misma transacción del soft delete; se eliminó la reconciliación periódica por cron. Como hook puntual, el front expone `POST /api/reconciliacion/{idUsuario}`: si donaciones no encuentra al donante o lo devuelve inactivo, ejecuta la baja idempotente `DELETE /api/perfiles/{idUsuario}` en incentivos. *Residual: el hook necesita una pantalla o acción del front que lo dispare.*
- **54.** Soft Delete y minimización en `Perfil` preservando métricas de `ImpactoDonacion` y snapshots de `Ranking`: agregado estado `activo = true` en `Perfil`, método de dominio `darDeBaja()` (anonimiza `nombreUsuario = null`, desvincula categoría y anula misiones/insignias vía `orphanRemoval`), se eliminó definitivamente `deleteByIdUsuario` para salvaguardar la contabilidad histórica de donaciones, y las consultas/actualizaciones sobre perfiles inactivos responden 404 (`InexistenteException`).
- **44.** Alta de perfil acota el largo de `nombreUsuario` (`@Size(max = 500)` en `PerfilDonanteDTO` y `@Column(length = 500)` en `Perfil`; rechaza nombres largos con 400 Bean Validation en vez de un 409 por violación de integridad).
- **43.** El historial de rankings pagina en base de datos sin cargar todas las posiciones a memoria: se removió `@EntityGraph` en `RepositorioRankings` y se configuró `@BatchSize(size = 30)` en `RankingMensual.posiciones` para hidratar en sub-lotes.
- **42.** `crearPerfilesEnLote` maneja transacciones independientes por perfil (`TransactionTemplate`) evitando que un error de persistencia marque la sesión en rollback-only y tumbe el lote entero. Errores formateados sin exponer internals de BD.
- **21.** Las rutas de ranking quedan detrás de la ACL de red; el rol `ADMIN` se valida en el front. *El scheduler entra por un método privado sin permisos.*
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
