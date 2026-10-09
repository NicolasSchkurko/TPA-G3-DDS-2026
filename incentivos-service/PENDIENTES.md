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
| 1 | 39 | Un perfil queda huérfano si la baja del donante en donaciones no llega |
| 2 | 1 | La identidad de admin declarada en un header no está vinculada a una identidad autenticada |
| 3 | 10 | Decidir si el ranking cuenta insignias o misiones, como pide el enunciado |
| 4 | 23 | Quedan decisiones de identidad y ampliar la cobertura de persistencia JPA |
| 5 | 37 | La configuración Checkstyle no se ejecuta automáticamente en el build |
| 6 | 6 | Vigilancia: mantener las lecturas de relaciones lazy dentro de transacciones |
| 7 | 41 | La difusión en redes no es verificable: la imagen la genera un workflow n8n fuera del repo |
| 8 | 40 | El mensaje publicado en redes arranca con una coma y la red está fija en `discord` |
| 9 | 42 | Un ítem que falla tumba el lote entero de alta de perfiles |
| 10 | 43 | El historial de rankings pagina en memoria: trae todos los rankings con todas sus posiciones |
| 11 | 44 | Un `nombreUsuario` largo revienta el alta con un 409 confuso |
| 12 | 45 | La outbox de n8n reintenta para siempre: no hay tope de intentos ni descarte |
| 13 | 46 | Un período de ranking ya existente responde 400 en vez de 409 |

Los puntos 1 y 10 requieren decisiones del equipo: el primero necesita acordar una identidad
compartida entre servicios; el segundo conserva, por ahora, la decisión documentada de contar
insignias. El punto 9 también es una decisión de arquitectura ya documentada, no un faltante.
El punto 41 es una verificación pendiente, no un bug: la generación de la imagen vive en un
workflow de n8n que no está en el repositorio.
La vigilancia del punto 6 sigue aplicando al agregar endpoints.

El detalle de cada fix está en [Corregidos](#corregidos), un ítem por corrección.

El punto 23 aparece en la tabla porque quedan decisiones de identidad y ampliar la cobertura
de persistencia JPA. La llamada HTTP a n8n dentro de la transacción, el enum JDK persistido
como dominio y la falta total de pruebas JPA ya fueron atendidos; se detallan en su sección.

El punto 35 (los "pendientes" en memoria dicen deduplicar y no deduplican) **ya no es un punto
aparte**: la parte de notificaciones se resolvió con RabbitMQ y las publicaciones a n8n ahora
usan una outbox persistente con reclamos exclusivos y reintentos.

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

**Relación nueva que hay que tener en cuenta desde el punto 22:**
`InsigniaObtenida.insignia` pasó de `EAGER` a `LAZY`. No es una relación más que "está
perezosamente": `convertirPerfilADTO` lee `io.getInsignia().getNombre()`, así que cualquier
lectura del perfil tiene que traerla. Hoy lo cubre el `@EntityGraph` de `findByIdUsuario` y
el de `paginaInsigniasPorIdUsuario`, pero un endpoint nuevo que mapee un perfil por otro
camino va a necesitar el suyo.
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

1. Si el XML perde el `<?xml?>` o el `DOCTYPE`, Checkstyle **no parsea nada y sale con 0
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
sintonia con esto: el build tampoco falla por preferencias de formato.

### Lo que encontró el panel y sí era código muerto

Los warnings del panel no eran todos de estilo. Estos sí son defectos, y se corregieron:

- **`PerfilService` inyectaba `DonacionClient` y no lo usaba.** Era residuo del punto 12:
  cuando el contacto pasó a resolverse en el `AFTER_COMMIT` del listener, el servicio dejó de
  necesitarlo pero el campo, el import y el parámetro del constructor se quedaron. Con la
  inyección de por constructor, un parámetro que no se usa es ruido que además esconde que
  la dependencia ya no existe. Se sacaron el campo y el parámetro, y de los tres tests que
  lo pasaban.
- **`MedioContactoDTO` y `EnvioPublicacionException` no las referenciaba nadie.** Las dos
  se auto-referencian y nada más. `DonacionClient.obtenerContactoPersona` devuelve la entidad
  `MedioContacto`, no el DTO, así que `MedioContactoDTO` quedó huérfana cuando se dejó de
  deserializar a mano. `EnvioPublicacionException` nunca se lanzó: desde que el listener de
  n8n no relanza (punto 13), no hay quién la lance. Se borraron las dos.
- **Siete imports sin usar**, en `RepositorioMisiones`, `PerfilService` y cinco archivos de
  test.
- **La red de seguridad de encapsulación tenía cuatro entidades sin cubrir.** El
  `EntidadesSinSettersTest` tenía la lista de entidades duplicada: un `@ValueSource` con
  nueve clases que era el que corría, y un campo `ENTIDADES` con trece que no se usaba para
  nada. Las cuatro de la diferencia —`ImpactoDonacion`, `CategoriaMision`, `ReglaConstancia`
  y `Operacion`— no estabanjutadas por esa desincronización, no por una decisión. Ahora hay
  una sola lista, con `@MethodSource`, y los dos casos con motivo para quedar afuera
  (`Operacion` por abstracta, `ImpactoDonacion` por su setter de `idDonacion`) salen por un
  filtro explícito y cada uno tiene su propio test. Los tests subieron de 208 a 210.
- Campos y parámetros sin usar en tests: `OTRO`, `ENTIDADES`, `MISION_FACTORY`, `CONTACTO`.

---

## 23. Higiene del código: setters, logs, nombres y código muerto

**Estado:** abierto (queda la parte de fondo)
**Severidad:** muy baja
**Archivos:** varios

Es el punto que nunca se cierra del todo: siempre queda algo por limpiar. Se hizo la parte
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

1. Consumir un evento `donante.baja` publicado por donaciones (Rabbit) y borrar ahí el perfil y su historial de impactos de forma idempotente, con DLQ y reintentos. Deja de depender de una llamada HTTP en medio de la baja del otro servicio.
2. Exponer una **reconciliación** (endpoint de admin o job) que liste/borre perfiles sin donante, o un borrado de respaldo por `nombreUsuario`.
3. Verificar que el despliegue tenga el endpoint `/api/perfiles/interno/{idUsuario}` (commit `7e9da04`); sin él, la llamada de donaciones da 404 y la baja entera falla.

---

## 41. La generación de la imagen y la publicación viven en un workflow de n8n fuera del repositorio

**Estado:** abierto (verificación pendiente)
**Severidad:** media
**Archivos:** `.../clients/N8nClient.java`, `.../services/PublicacionesN8nService.java`,
`.../dto/n8n/PerfilPublicacionDTO.java`

### Qué pasa

El enunciado pide "un mecanismo automatizado que **genera una imagen y publica en redes
sociales**" cada vez que se desbloquea una insignia. Acá el servicio solo **encola un `prompt`**
(`"en el centro debe decir <insignia>"`), un `mensaje` y la red, y los POSTea al webhook de n8n:
la generación de la imagen y la publicación efectiva viven en un **workflow de n8n que no está
versionado en el repositorio**, así que no hay forma de verificarlo ni de reproducirlo con el
código. En la prueba real, el webhook de producción respondía
`404 "The requested webhook POST incentivos is not registered"` (el workflow no estaba activo).

### Propuesta

1. Versionar el workflow de n8n (export JSON) en el repo, o mover la generación de la imagen al
   servicio (plantilla + render) y dejar a n8n solo la publicación.
2. Documentar el contrato del webhook (payload esperado) y cómo activarlo, para que la difusión
   sea verificable de punta a punta.

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

## 42. `crearPerfilesEnLote` no puede ser parcial: un ítem que falla tumba el lote entero

**Estado:** abierto
**Severidad:** media
**Archivos:** `services/PerfilService.java:143-178`, `controllers/PerfilController.java:80-84`

### Qué pasa

El endpoint `POST /api/perfiles/lote` promete *"los que fallan vienen detallados en `errores`
sin tumbar el resto"*, pero `crearPerfilesEnLote` es **un solo `@Transactional`** que atrapa las
excepciones por ítem:

```java
@Transactional
public ResultadoLotePerfilesDTO crearPerfilesEnLote(List<PerfilDonanteDTO> perfiles) {
    for (PerfilDonanteDTO perfil : perfiles) {
        try {
            ...
            crearPerfilNuevo(perfil);   // adentro hace repositorioPerfiles.flush()
        } catch (Exception e) {
            errores.add(...);           // sigue con el próximo
        }
    }
```

Cuando un ítem provoca una excepción **de persistencia** en el `flush()`, el `catch` la traga
pero la sesión de Hibernate queda inservible (transacción rollback-only): el commit al final del
método lanza `UnexpectedRollbackException` y **se revierten los perfiles que sí se habían
creado**, con un 500. El `catch (Exception)` además traga errores de programación y devuelve
`e.getMessage()` crudo al cliente.

### Cómo se dispara

Cualquier ítem del lote que viole una constraint en el INSERT. El más fácil: `nombreUsuario` no
tiene límite de largo (`PerfilDonanteDTO` solo lo marca `@NotBlank`) y la columna es
`varchar(255)`; un nombre de 300 caracteres revienta el `flush` y con él todo el lote.

### Propuesta de arreglo

Una transacción por ítem (`TransactionTemplate` o un método `REQUIRES_NEW`) para que el fallo de
uno no marque rollback-only al resto, capturar solo las excepciones esperadas y no devolver
`getMessage()` crudo. Complementa al punto 44.

---

## 43. El historial de rankings pagina en memoria: trae todos los rankings con todas sus posiciones

**Estado:** abierto
**Severidad:** media
**Archivos:** `models/repositories/SpringRepositories/RepositorioRankings.java:30-31`,
`services/RankingService.java:168-172`

### Qué pasa

`GET /api/rankings` es paginado, pero la consulta que lo alimenta hace `fetch` de una colección
y a la vez recibe `Pageable`:

```java
@EntityGraph(attributePaths = "posiciones")
Page<RankingMensual> findAllByOrderByPeriodoDesc(Pageable pageable);
```

Hibernate no puede aplicar `LIMIT`/`OFFSET` sobre un `fetch join` a una colección: ignora el
límite y **trae todos los rankings con todas sus posiciones a memoria**, y recién ahí Spring
arma la página. La respuesta sale correcta, pero el costo crece con todo el historial.

### Cómo se dispara

Pedir una página del historial (`GET /api/rankings?size=10`) cuando hay muchos rankings
publicados —uno por mes— y cada uno con todos los donantes de ese mes.

### Propuesta de arreglo

Paginar sobre `RankingMensual` sin el `@EntityGraph` y resolver las posiciones por página, o una
`@Query` con `countQuery` aparte como ya se hizo en `paginaInsigniasPorIdUsuario`.

---

## 44. El alta de perfil no acota el largo de `nombreUsuario`: un nombre largo revienta con un 409 confuso

**Estado:** abierto
**Severidad:** media
**Archivos:** `dto/Persona/PerfilDonanteDTO.java:19-20`, `models/entities/Perfil/Perfil.java`

### Qué pasa

`PerfilDonanteDTO.nombreUsuario` solo tiene `@NotBlank`; no hay `@Size` ni `@Column(length = ...)`
en `Perfil`. La columna queda `varchar(255)`, así que un nombre de más de 255 caracteres pasa la
validación y falla en el INSERT con `DataIntegrityViolationException`, que el handler traduce a
**409 "hay datos relacionados que la impiden"** — un mensaje que no tiene nada que ver con la
causa. `role` está igual de suelto.

### Cómo se dispara

`POST /api/perfiles` (o `/lote`, ver punto 42) con `nombreUsuario` de 256+ caracteres.

### Propuesta de arreglo

`@Size(max = 255)` en el DTO y `@Column(length = ...)` en la entidad, para que sea un 400 con el
campo señalado.

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

## Corregidos

Un ítem por fix, en el orden en que se cerraron. Los números son los que tenía el punto cuando se
corrigió; los que no llevan número son fixes sin punto propio. Cuando hubo una decisión consciente
que conviene no deshacer sin leer el porqué, va en cursiva en la misma línea.

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
