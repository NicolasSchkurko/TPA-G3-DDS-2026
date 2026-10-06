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
| 2 | 1 | Cualquiera que conozca un UUID de admin puede crear, editar y borrar misiones |
| 3 | 3 | Requisito explícito del enunciado sin cumplir (cola de mensajes) |
| 4 | 10 | El ranking no cuenta lo que el modelo dice que cuenta |
| 5 | 6 | Regla de prevención para no introducir `LazyInitializationException` |
| 6 | 23 | Higiene: código muerto, logs, encapsulación |
| 7 | 4 | `common-lib` es código muerto |
| 8 | 9 | No es un faltante: es una decisión de arquitectura |
| 9 | 37 | La config de Checkstyle vive solo en `.idea/` y no se comparte |
| 10 | 38 | Incentivos no publica por Rabbit y hoy no llega ninguna notificación a nadie |

Quedan nueve abiertos y uno a medias, que ya no son los mismos del principio. **Tres de los
que quedan son decisiones, no faltantes**: el 1 (autorización por header), el 9 (logística no
integrada) y el 10 (el ranking cuenta insignias). Los tres están anotados como aceptados a
propósito, y cerrarlos o no es una decisión del equipo, no una deuda técnica.

Los cerrados se agruparon en tandas porque se corrigieron juntos:

| Tanda | Puntos | Por qué juntos |
|---|---|---|
| 1ª | 21, 12, 8 | Autorización incompleta y llamadas HTTP dentro de transacciones |
| 2ª | 26, 27, 28 | La progresión del donante se medía mal y se duplicaba |
| 3ª | 13, 14 | Una sola cadena de fallo: el 500 provocaba el reintento, el reintento corrompía |
| 4ª | 25, 36, 17, 30 | Los cuatro eran fallos de progresión del donante y ninguno se manifestaba solo |
| 5ª | 22, 31, 32 | El 31 y el 32 hablan de lo mismo: un dato inválido que entra sin que nadie lo revise y rompe algo lejos de donde entró |
| 6ª | 33, 34, 24, 2 | Los cuatro son de coherencia entre lo que el código dice y lo que hace |

El detalle de cada fix está en [Corregidos](#corregidos), un ítem por corrección.

El punto 23 no aparece en la tabla porque está en la sección de su propio detalle más abajo, y
tampoco cuenta como "abierto a medias": su parte grande se hizo y lo que queda son tres cosas
anotadas.

El punto 35 (los "pendientes" en memoria dicen deduplicar y no deduplican) **ya no es un punto
aparte**: quedó absorbido por el
[anexo del punto 3](#punto-anexo-los-buffers-pendientes-en-memoria-absorbe-el-ex-punto-35), que
es el código que hay que tocar para arreglarlo.

**Por qué se absorbió y no se cerró.** No es que el 35 quede resuelto por el 3: el buffer de
**notificaciones** sí desaparece con la cola, pero el de **publicaciones de n8n** sigue igual,
porque el requisito de asincronía del enunciado no cubre esa integración. Dejarlo como punto
propio daba la falsa impresión de que cerrando el 3 se cerraba también el 35, y el bug quedaba
sin dueño. El anexo deja escrito cuál de las dos mitades se va con la cola y cuál no, y que la
de n8n necesita la tabla de outbox.

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

## 3. Las notificaciones van por HTTP síncrono y el requisito pide cola de mensajes

**Estado:** abierto
**Severidad:** crítica
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/incentivos/clients/NotificacionClient.java`,
`src/main/resources/application.properties`,
`pom.xml`

### Qué pasa

El enunciado dice, textualmente: *"La integración entre los servicios de dominio y el
Servicio de Notificaciones deberá realizarse de forma asíncrona, a través de una cola de
mensajes, a fin de no afectar la disponibilidad del sistema ante picos de carga o fallas
transitorias."*

`incentivos-service` **no usa RabbitMQ en absoluto**. No tiene `spring-boot-starter-amqp` en el
pom, ninguna clase toca `RabbitTemplate`, y no hay exchange ni cola declarados. La
notificación sale por `RestTemplate.postForEntity` dentro de un `@TransactionalEventListener`,
o sea **sincrónica y bloqueante**: si el servicio de notificaciones no responde, la
notificación falla y el evento ya commiteado tira la excepción hacia atrás.

Es exactamente el punto 3 del backlog, confirmado contra el código y contra el enunciado.

### Por qué hoy no funciona, y no solo por el HTTP

Se verificó levantando `incentivos-service` y `notificaciones-service` juntos y llamando por
HTTP. Hay **tres fallos encadenados**, y cualquiera de los tres basta para que ninguna
notificación llegue a ningún lado:

**1. El servicio de notificaciones exige autenticación.**
`spring-boot-starter-security` es dependencia directa de su pom. Spring Boot genera una
password aleatoria al arrancar y *toda* petición sin credenciales recibe `401`, incluido
`POST /api/notificaciones`. No hay `SecurityFilterChain` propio ni `permitAll`. Es el motivo
de que hoy no funcione ninguna notificación por HTTP, con independencia de lo demás.

**2. La ruta está mal.** La propiedad apunta a `${NOTIFICACIONES_URL:http://localhost:8083/}`,
es decir la raíz con barra final. El controller está declarado como
`@RequestMapping("/notificaciones")` y el servicio tiene
`server.servlet.context-path=/api`, así que la ruta real es `POST /api/notificaciones`.
Comprobado: `POST http://localhost:8083/` devuelve **404**.

**3. Los DTO no coinciden en un nombre de campo.** `PerfilNotificacionDTO` declara
`direccionContacto`; `SolicitudNotificacionDTO` declara `direccionDeContacto`. Con Jackson el
campo queda en `null` sin error, y como `direccionDeContacto` está en `nullable = false`, el
INSERT muere con violación de restricción en el servidor de notificaciones. Un `renamed`
implícito que no da ningún aviso.

**4. El medio de contacto tampoco habríaITDA.** `MedioDeEnvioFactory` indexa por nombre de
bean en minúsculas (`email`, `telefono`, `whatsapp`) y buscaba con `get()` case-sensitive,
mientras los servicios mandan `"EMAIL"` o `"WHATSAPP"`. Eso ya está corregido en
`notificaciones-service`, pero conviene saber que era la cuarta barrera.

### Qué se ya resolvió en el otro extremo

`notificaciones-service` ya tiene su topología de broker declarada y funcionando
(`notificaciones.exchange`, cola `notificaciones`, routing keys
`notificaciones.incentivo.#` y `notificaciones.evento.logistica.#`, con
`Jackson2JsonMessageConverter`). Lo que falta es el lado de acá: publicar en vez de llamar
por HTTP.

### Propuesta

1. Agregar `spring-boot-starter-amqp` al pom de este módulo.
2. Declarar el exchange y el converter, o mejor: definir el exchange en el servicio que
   publica y que `notificaciones-service` solo declare su cola, que es la frontera que se
   eligió para el broker de logística.
3. Reemplazar el `postForEntity` por `convertAndSend(RabbitMQ, RK_INCENTIVO, dto)`.
4. Alinear el nombre del campo con el del receptor, o definir un DTO de transporte con los
   nombres que el broker espera. Lo segundo es más frágil; lo primero, un cambio de una
   línea.
5. Decidir qué hacer con la autorización del endpoint HTTP de notificaciones: o `permitAll`
   solo para `POST /notificaciones`, o que la cadena sea únicamente por broker y el endpoint
   quede para uso interno.

El paso 5 es decisión del equipo y no se puede dar por hecho: **no tocar la autorización sin
que lo defina el equipo**, porque el punto 1 de este mismo backlog ya discute ese tema.

### Verificación que hay que dejar

Un test que publicaría por el broker y comprueba que una notificación queda persistida. Hoy
ningún test cubre este camino, que es la razón por la que el bug llevaba tiempo sin
detectarse.

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

## 37. La config de Checkstyle no se comparte: vive solo en `.idea/`

**Estado:** abierto
**Severidad:** baja
**Archivos:** `incentivos-service/config/checkstyle/checkstyle.xml`, `pom.xml`,
`gen-checkstyle.ps1`, `run-checkstyle.ps1`

La config que se armó para el punto 23 está en el repo, pero **nada la ejecuta**. El build no
tiene el plugin de Checkstyle de Maven, así que `mvn test` no corre ninguna de las reglas y
el único que las aplica es el plugin de IntelliJ, que la lee desde `.idea/checkstyle-idea.xml`.
Y como `.idea/` está en `.gitignore`, ni siquiera el archivo que le dice al IDE dónde está la
config se comparte: eso queda solo en la máquina de quien la configuró.

Eso tiene dos consecuencias:

1. Quien clone el repo y ejecute `mvn test` no recibe ninguna señal de estilo. Solo lo ve
   quien abre el proyecto en IntelliJ con el plugin instalado.
2. Todo lo que el plugin de IntelliJ marca como `Warning` aparece en el panel de Problems
   junto a las inspecciones propias del IDE, y el panel no distingue de dónde salió cada
   cosa. Por eso el número que se ve no es comparable con el que da la config por separado.

**Se probó agregar el plugin de Maven al `pom.xml` y no va.** Rompe `mvn verify` en una
máquina sin internet: además de `maven-reporting`, `doxia` y `plexus`, el plugin necesita
`com.puppycrawl:checkstyle`, y ninguno de esos artifacts está en el `.m2` local. Un build
que falla por una dependencia que no se puede bajar es peor que un build que no valida
estilo, así que el `pom.xml` quedó sin el plugin y con un comentario que explica por qué.

Lo que sí quedó es `run-checkstyle.ps1` en la raíz del repo, que corre **la misma
configuración** con el jar de Checkstyle que ya trae el plugin de IntelliJ. No necesita Maven
ni internet:

```powershell
.\run-checkstyle.ps1
```

Por dentro el script arma el classpath con los jars de
`%APPDATA%\JetBrains\<versión>\plugins\checkstyle-idea\checkstyle\lib`. O sea que depende
de que el plugin de IntelliJ esté instalado; para correrlo en un CI hay que bajar Checkstyle
por otra vía.

**Lo que falta para que la validación llegue al build**, en una máquina con acceso a Maven
Central:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.5.0</version>
    <configuration>
        <configLocation>config/checkstyle/checkstyle.xml</configLocation>
        <includeTestSourceDirectory>false</includeTestSourceDirectory>
        <violationSeverity>warning</violationSeverity>
        <consoleOutput>true</consoleOutput>
        <failOnViolation>true</failOnViolation>
    </configuration>
    <executions>
        <execution>
            <id>validar-estilo</id>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
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

1. **`ValidadorAdmin.verificarPermisos` sigue llamando a `donaciones-service` dentro de la
   transacción.** Es la última llamada HTTP que quedó dentro de una transacción (el resto se
   sacaron en el punto 12). Se dejó así porque son operaciones de administración de baja
   frecuencia y acotadas por los timeouts, pero sacarla exige partir la validación en otra
   clase: llamar a un método `@Transactional` desde la misma clase no pasa por el proxy.
2. **`ReglaConstancia.unidadTiempo` es un `java.time.temporal.ChronoUnit` persistido como
   string.** Es un enum de la JDK y no del dominio, y no hay garantía de que sus constantes
   se mantengan estables entre versiones de Java. Un `UnidadTiempo` propio con `MESES` y
   `DIAS` sería más seguro.
3. **No hay ni un test de persistencia.** No hay H2 ni `@DataJpaTest`, así que todos los
   tests son unitarios con Mockito y ninguno valida un mapping JPA: una `@Column` mal escrita
   o un `orphanRemoval` que falta no se detectan hasta que la aplicación arranca contra
   MySQL. Es el hueco más grande que queda de la suite. La tanda de los puntos 25, 36, 17 y
   30 lo tapó a medias con tests de contrato por reflexión (que el `@Transactional`, el
   `@Version` y los `orphanRemoval` estén donde deben), pero eso **no** prueba que
   Hibernate los ejecute: solo que las anotaciones estén puestas. Cerrar esto de verdad
   necesita H2, y es lo primero que agregaría.
---

## 38. Incentivos publica por HTTP y no llega a nadie: la URL está mal y el receptor exige autenticación

**Estado:** abierto
**Severidad:** crítica
**Archivos:** `src/main/java/ar/edu/utn/frba/ddsi/incentivos/clients/NotificacionClient.java`,
`src/main/resources/application.properties`,
`notificaciones-service/pom.xml`

### Qué pasa

Es la confirmación ejecutable del [punto 3](#3-las-notificaciones-van-por-http-sincrono-y-el-requisito-pide-cola-de-mensajes),
que documenta el requisito; acá está la evidencia medida. Al levantar `incentivos-service` y
`notificaciones-service` juntos y llamar por HTTP, el resultado es:

| Prueba | Resultado |
|---|---|
| `POST http://localhost:8083/` (la URL que usa incentivos) | **404** |
| `POST http://localhost:8083/api/notificaciones` (la real) | **401** |
| Notificaciones persistidas al final | **0** |

O sea que hoy **ninguna notificación de incentive llega a ningún lado**, y por dos motivos
independientes: la ruta que usa el cliente no existe, y la ruta que sí existe pide
credenciales.

### El 401 no es un detalle de configuración

`spring-boot-starter-security` es dependencia **directa** del pom de
`notificaciones-service`. No hay `SecurityFilterChain` propio, ni `permitAll`, ni clase de
seguridad en el código: solo dos `@Configuration` (`RestTemplateConfig` y `RabbitConfig`).
Spring Boot entonces aplica su configuración por defecto, genera una password aleatoria al
arrancar y bloquea todo.

Es el único módulo de los cuatro con seguridad activa, así que es también el único que puede
explicar un 401. Y explica por qué el bug fue invisible: nadie lo detectó porque el síntoma
es "no llegan notificaciones", que selee como "el n8n no está" o "falta configurar algo", y
no como "el receptor pide autenticación".

### Los DTO tampoco coinciden

`incentivos-service` manda `PerfilNotificacionDTO`, que declara `direccionContacto`.
`notificaciones-service` recibe `SolicitudNotificacionDTO`, que declara `direccionDeContacto`.
Con Jackson el campo desalineado queda en `null` sin error ni aviso, y como
`direccionDeContacto` está en `nullable = false`, el INSERT muere en el servidor con violación
de restricción.

Este detalle importa por separado: **es un fallo que se activaría solo después de arreglar los
otros dos**. Si se arregla la URL y la seguridad por partes, el siguiente síntoma va a ser un
500 en notificaciones con un `null` en el log, y va a parecer un problema nuevo.

### Propuesta

Orden de arreglo, del más bloqueante al menos:

1. Definir la autorización de `POST /notificaciones`: es decisión del equipo y la propone el
   [punto 1](#1-la-autorizaci-n-de-admin-se-apoya-en-un-header-controlado-por-el-cliente), no
se puede aplicar de forma automatica.
2. Corregir la URL a `http://localhost:8083/api/notificaciones`, o mejor: eliminar la llamada
   por HTTP y publicar por broker, que es lo que pide el enunciado (punto 3).
3. Alinear el nombre del campo entre los dos DTO.

El punto 1 va primero a propósito: es una decisión de diseño y las otras dos dependen de
cómo se resuelva.

---
## Corregidos

### `incentivos-service` no tenía AMQP: las notificaciones iban por HTTP síncrono

**Estado:** corregido
**Severidad:** crítica
**Archivos:** `pom.xml`, `.../config/RabbitMQConfig.java`, `.../clients/NotificacionClient.java`,
`src/main/resources/application.properties`

El enunciado pide textualmente que la integración con el servicio de notificaciones sea
"asíncrona, a través de una cola de mensajes". `incentivos-service` no tenía
`spring-boot-starter-amqp`, ninguna clase tocaba `RabbitTemplate` y no declaraba ni exchange ni
cola. Publicaba con `restTemplate.postForEntity` dentro de un
`@TransactionalEventListener`, o sea **sincrónico y bloqueante**: si notificaciones tardaba o
estaba caído, el hilo quedaba esperando.

**Qué se cambió:**
1. `spring-boot-starter-amqp` en el pom.
2. `RabbitMQConfig` declarando `notificaciones.exchange` y el `Jackson2JsonMessageConverter`,
   que sin él el `RabbitTemplate` queda con `SimpleMessageConverter` y no puede serializar el
   DTO.
3. `NotificacionClient` publica con `convertAndSend` en vez de llamar por HTTP. Si el broker
   falla, guarda en pendientes y propaga, que es el mismo contrato que tenía.

**El exchange lo declara el que publica** y notificaciones ata su cola: es la frontera elegida
para que el nombre de las colas no viva en los dos lados.

**Cómo se verificó:** un mensaje con `routing_key=notificaciones.incentivo` llega al consumidor
y termina persistido en la base de notificaciones.

### El DTO mandaba `direccionContacto` y el receptor lee `direccionDeContacto`

**Estado:** corregido
**Severidad:** crítica
**Archivo:** `.../dto/Notificaciones/PerfilNotificacionDTO.java`

El DTO de incentives declaraba `direccionContacto`; el de notificaciones declara
`direccionDeContacto`. Con Jackson el campo desalineado **no da error**: queda en `null` en
silencio, y como `direccionDeContacto` está en `nullable = false`, el INSERT del otro lado muere
con violación de restricción.

Es el peor tipo de bug de contrato: no falla al publicar, falla en el servidor del otro servicio y
sin rastro de la causa.

**Qué se cambió:** el campo se renombró a `direccionDeContacto`, que es lo que lee el receptor. No
se agregó un `@JsonProperty` como alias porque un alias invisible es exactamente lo que reproduce
el bug la próxima vez. El DTO ahora implementa `Serializable` y tiene constructor sin
argumentos, que es lo que el converter necesita para deserializar.

### `SecurityConfig` exigía HTTP Basic con contraseña autogenerada: los demás servicios no podían llamar

**Estado:** corregido
**Severidad:** crítica
**Archivos:** `.../config/SecurityConfig.java`, `pom.xml`

`incentivos-service` exigía HTTP Basic en todo salvo el perfil público, y **no hay
`UserDetailsService`**: usaba la contraseña que Spring Boot genera al arrancar, que cambia en cada
arranque. No existía credencial fija que un llamador pudiera conocer, así que la integración no
tenía cómo resolverse: `POST /api/perfiles` y `PATCH /api/perfiles/donacion/{id}` devolvían
`401` desde `donaciones-service`.

Se comprobó levantando MetaDonacion e incentivos juntos: `POST /api/personas` devolvía `500`, y el log
de la causa era `No se pudo crear el perfil ... : 401`.

**Qué se decidió:** dejar el servicio abierto, igual que los otros tres módulos. Lo que se
pierde es la superficie pública del perfil del punto 8, que queda abierta igual. Lo que queda es
la autorización de las operaciones de admin, que se validan por header `Admin-Id` —que es un
header que controla el cliente, así que nunca fue seguridad— y está anotada como punto 1.

**Lo que no se hizo, a propósito:** quitar `spring-boot-starter-security` del pom. Se conserva la
dependencia y la clase, con la política escrita y explícita, para que quede a la vista dónde
reintroducir una política real si algún día se define. Borrar la dependencia sería menos
explícito, no más seguro.

**Cómo se verificó:** `POST /api/personas` responde `201` y el perfil queda creado en la base de
incentivos.

### El `@Query` de `findAllByMisionActual` no tenía `FROM`

**Estado:** corregido
**Severidad:** crítica
**Archivo:** `.../repositories/SpringRepositories/RepositorioPerfiles.java`

`@Query("SELECT p JOIN p.progresoMisionActual pm WHERE ...")` no es JPQL válido: le falta
`FROM Perfil p`. Todos los demás `@Query` del repositorio lo tienen. Spring Data lo rechaza al
construir el repositorio con un `Validation failed for query` que **no dice que falta el FROM**,
y como el bean del repositorio no se podía crear, **el servicio entero no arrancaba**.

Los 302 tests pasaban en verde porque mockean el repositorio: la query nunca se validaba. Solo se
detectó levantando el servicio.

**Cómo se verificó:** el servicio levanta y el contexto de Spring arma completo.

### Faltaba el bloque `spring.rabbitmq.*`

**Estado:** corregido
**Severidad:** media
**Archivo:** `src/main/resources/application.properties`

Sin el bloque, el `CachingConnectionFactory` usa `localhost:5672` y `guest`/`guest`. En la
máquina de desarrollo funciona; dentro de un contenedor busca `localhost`, que es el propio
contenedor. Agregado con variables de entorno, como el resto de la configuración.

---
# Corregidos

Lo que ya está arreglado, para no volver a tocarlo. Los números son los que tenía cada punto
cuando se corrigió, así que no aparecen en la lista de arriba.

**Un ítem por fix, en el orden en que se fueron cerrando.** Cuando hubo una decisión
consciente —algo que el código deja de hacer a propósito, y que conviene no rehacer sin
volver a leer el porqué— va en la misma línea en cursiva. Las decisiones que están en su
propio punto del backlog, con el detalle largo, se dejan acá en una línea y no se repiten.

---

## Tanda 1 — 21 + 12 + 8

- **21. Las rutas de ranking exigen administrador.** `POST /api/rankings` y
  `DELETE .../{idRanking}` validan `Admin-Id` y llaman a `ValidadorAdmin`; antes cualquiera
  creaba rankings de meses arbitrarios o borraba los publicados. *El scheduler entra por un
  método privado que no valida permisos*: es una entrada interna del proceso, no una ruta
  HTTP, y dejarlo pasar por el método público obligaría a inventar un id de admin.
- **12. Las llamadas HTTP salieron de las transacciones.** Timeouts configurables (3 s de
  conexión, 5 s de lectura) en vez del infinito del `RestTemplate` pelado, y los eventos
  llevan `idUsuario` en vez del contacto, que ahora se resuelve en `AFTER_COMMIT`. Desapareció
  la peor: `SincronizacionPerfiles` pedía el contacto una vez por donante dentro de un bucle
  transaccional. *No se pusieron reintentos automáticos*: publicar en n8n es un `POST` sin
  idempotency key, así que un reintento a ciegas publica dos veces.
- **8. La categoría del donante es visible públicamente.**
  `GET /api/perfiles/{idUsuario}/publico` con `permitAll()`. *Con un DTO aparte, porque la
  ruta está abierta*: lo que sale de ahí queda expuesto y no puede ser el `PerfilDTO`
  completo. Un perfil sin categoría devuelve `nombreCategoria = null`, no 404: el donante
  existe y su nombre tiene que poder verse igual.

## Tanda 2 — 26 + 27 + 28

- **26. La constancia cuenta meses calendario.** `cantidad` se usaba como margen en días, así
  que tres `PATCH` en tres días consecutivos completaban la misión de tres meses. Ahora cuenta
  hacia atrás: dos donaciones del mismo mes cuentan una sola vez y un mes vacío corta la
  racha. *Para las misiones con constancia, `progreso` pasó a contar meses y no donaciones.*
- **27. `conseguirMisiones` respeta el orden del admin.** `findAllById` genera un
  `WHERE id IN (...)` sin `ORDER BY`, y el orden de la lista **es** la secuencia de progresión
  del donante, porque `agregarMision` asigna `posicion = size + 1`.
- **28. La insignia ya no se otorga dos veces.** El seed ponía la misma misión en dos
  categorías, así que al cambiar de categoría la racha volvía a estar completa y se otorgaba
  la insignia otra vez. `insigniasObtenidas` pasó de `List` a `LinkedHashSet` con `equals`
  por `(perfil, insignia)`, y `progresarMision` usa lo que devuelve `Set.add`: no guarda ni
  dispara `MisionCompletada` dos veces, pero devuelve `true` igual porque **el donante sí tiene
  que avanzar** de misión. *No se puso `@UniqueConstraint`*: sin `@Version` el problema real
  era otro y más grave, y hacía que la transacción perdedora perdiera una donación legítima.

## Tanda 3 — 13 + 14

Una sola cadena de fallo: el 13 provocaba el 500, el cliente reintentaba y el 14 convertía
ese reintento en datos corruptos. Arreglando uno solo el otro seguía haciendo daño.

- **13. `N8nClient` ya no relanza después del commit.** El `catch` del listener
  `AFTER_COMMIT` logueaba y lanzaba; la excepción subía por el `processCommit` y el donante
  veía un 500 **con la transacción ya confirmada**.
- **14. La ingesta de|es idempotente.** `ImpactoDonacion.idDonacion` es el id de origen y
  además la primary key local, así que un reintento se reconoce con un `findById`;
  `completMision` guarda la respuesta para poder repetirla. *Se descartó comparar el payload*
  como clave: era menos discriminante (dos donaciones distintas con los mismos cuatro campos
  se tomarían por un reintento) y no cubría dos peticiones simultáneas. **Requisito para el
  otro servicio:** `donaciones-service` tiene que mandar el id de la donación o toda donación
  entra con 400.

## Sueltos — 7, 11, 15, 16, 18, 19, 20

- **7. Validación de entrada en los DTO.** `spring-boot-starter-validation` más `@Valid` en
  todos los `@RequestBody`, y las factories validan en código la integridad de la `Regla` que
  Bean Validation no puede expresar. De paso, `ChronoUnit.toString()` ("Months") pasó a
  `name()` ("MONTHS").
- **11. `ValoresDistintos` guardaba el estado en la misión, no en el donante.** Mutaba una
  lista que vive en la entidad de la misión, o sea compartida por todos los que la hacen: al
  tercero figuraba completa para los tres. El avance ahora vive en `ValorObservado`, y el
  contexto va en la firma (`ProgresoDelDonante`) y no en un campo de la operación, justamente
  para que el estado compartido no pueda volver. *Migración pendiente en prod*: la tabla
  `valor_observado` hay que crearla a mano donde la base ya existe.
- **15. Editar una misión ya no borra el progreso de todos.** `Mision.actualizar` devuelve si
  cambió lo que el donante tiene que cumplir, y solo ahí se reinicia. *Contra lo obvio:
  `SuperaCantidad` NO compara el umbral*, porque subir el mínimo exigido no invalida lo que
  el donante ya acreditó.
- **16. El progreso de la misión se expone**, con `progresoFaltante` y el desglose, y el DTO
  ya no invierte `progresoActual` con `progresoObjetivo`.
- **18. Los borrados contestan 409 con la cantidad de referencias.** Antes: 500 opaco por
  violación de FK, y `eliminarMision` devolvía 204 sin avisar cuando la misión no existía. La
  guarda va **antes** de tocar la secuencia de posiciones, que antes quedaba movida sin
  haber borrado nada.
- **19. La secuencia de categorías.** El tope sale de `listarPosiciones()` y no de `count()`,
  que daba por hecho justo lo que no estaba garantizado. *No se puso
  `@Column(unique = true)`*: es incompatible con los `UPDATE` en bloque del gestor, que al
  mover la fila de la 3 a la 4 pisaría a la que todavía sigue en la 4.
- **20. Códigos de estado consistentes y `desdeEntidad` null-safe.** `convertirPerfilADTO`
  estaba duplicado cuatro veces. *Deuda que quedó*: `obtenerPorId` sigue devolviendo `null` en
  vez de `Optional`, y "no existe" lanza dos excepciones distintas según el servicio.

## Tanda 4 — 25 + 36 + 17 + 30

Los cuatro eran fallos de progresión del donante, y cada uno rompía el mismo camino por un
lado distinto.

- **25. `crearPerfil` abre transacción.** Sin ella la categoría volvía desligada de la
  sesión, y `PersistentBag.isEmpty()` devuelve el tamaño cacheado **sin inicializar**: el
  perfil quedaba sin misión para siempre, en silencio y sin ningún error. Además la consulta
  de la categoría base trae la secuencia con `fetch`, para no depender del alcance de la
  transacción.
- **36. `@Version` en `Perfil` y `Categoria`, con reintento y 409.** Sin eso dos donaciones
  simultáneas perdían una, y si las dos completaban la misión cada una insertaba su
  `InsigniaObtenida` porque el `Set` en memoria de cada petición es distinto. *Va en la raíz
  del agregado*, no en `ProgresoMision`, para cubrir también el avance de categoría y el set
  de insignias. *El reintento usa `TransactionTemplate`* y no `@Transactional(REQUIRES_NEW)`
  en un método privado, porque las llamadas internas no pasan por el proxy.
- **17. `orphanRemoval` donde la referencia se reemplaza.** En `Perfil.progresoMisionActual` y
  en `Mision.reglaDeProgreso` (que cubre de una vez la `Regla` vieja y, por su
  `cascade = ALL`, su constancia y su operación). *Explícitamente NO* en
  `Mision.insigniaObjetivo`, porque la referencian todos los que ya la obtuvieron, ni en
  `Regla.constancia`/`operacion`, que se van con la regla vieja.
- **30. Quitar una misión ya no bloquea al donante.** Buscaba la posición pedida y, si ya no
  existía, lo dejaba sin misión para siempre —sin progreso, sin insignia, sin ranking— y sin
  evento que lo explicara. Ahora retrocede a la misión más cercana por debajo. *El donante
  pierde el avance de la misión que se le sacó*: preferible a dejarlo congelado.

## Tanda 5 — 22 + 31 + 32

- **31. Una posición fuera de rango es un 400.** El gestor se salía en silencio y el caller
  escribía la posición pedida igual, dejando la secuencia con huecos; con
  `posicionSecuencia: 0` la categoría **secuestraba la categoría base** y todos los donantes
  nuevos arrancaban en ella. *El rango es `[1, max]` en la edición y `[1, max+1]` en el
  alta*: en la edición el número de categorías no cambia, y admitir la `max+1` dejaba un hueco.
- **32. No se publica el ranking de un período sin cerrar.** Un solo ranking futuro
  rompía el "actual" entero: `findFirstByOrderByPeriodoDesc()` lo tomaba y `puestoRanking`
  daba 404 para todos. *El "actual" ahora se resuelve con el último mes cerrado* — el filtro
  va en la consulta, no solo en la validación del alta, así que tampoco lo rompen los datos
  que ya estaban en una base de desarrollo.
- **22. Las consultas de las tablas grandes.** El filtro del ranking pasó de `MONTH()/YEAR()`
  a un rango semiabierto; la evolución mensual se agrupa en SQL en vez de traer toda la tabla
  para agrupar en Java; tres N+1 eliminados con `@EntityGraph`, incluido
  `InsigniaObtenida.insignida` que pasó de `EAGER` a `LAZY`; y `evaluarConstanciaPerfiles` va
  por bloques de 500 en transacciones separadas, con orden explícito porque paginar por offset
  sin `ORDER BY` no es estable. *La consulta de las donaciones de cada perfil sigue siendo una
  por perfil*: es un compromiso, no un descuido. *Los índices son declaraciones*: sin una base
  de prueba no hay `EXPLAIN` que confirme que la base los usa.

## Tanda 6 — 33 + 34 + 24 + 2

Los cuatro eran el mismo error en cuatro lugares: **el código afirmaba una propiedad que no
tenía**.

- **33. "Supera" es estricto.** `>=` pasó a `>`. El enunciado dice "supera 6 bienes" y en
  español "supera" es "excede": un donante con 6 se llevaba la insignia de los de 7. Con `>=`
  el nombre de la clase también mentía. *El umbral sigue sin compararse en
  `esEquivalenteA`.*
- **34. Dos guardas que faltaban.** El filtro del listado de misiones parseaba el enum sin
  normalizar, así que `?atributo=CATEGORÍA` con acento daba 400 mientras el `POST` de la misma
  misión aceptaba esa cadena; ahora reusa el normalizador de `MisionFactory` y el error dice
  qué valores valen. Y `crearConstancia` decía rechazar la constancia a medio y devolvía
  `null`: por HTTP lo cubría el bean validation, pero una llamada interna creaba una misión
  **sin exigencia de racha** sin que nada lo indicara.
- **24. La insignia tiene sus tres datos.** El enunciado pide nombre, descripción e imagen y
  solo se podía cargar el nombre; el texto de la insignia se derivaba del nombre de la misión,
  y al editar venía de otra fuente. *La imagen es una URL y no los bytes*: no hay dónde
  guardarlos, base64 haría que editar el nombre reenvíe el archivo entero, y un `byte[]` en
  `Insignia` rompería la deduplicación del punto 28 en silencio.
- **2. El ranking se persiste completo.** Pedir el podio con `limite=50` devolvía 10 con un
  200 y sin avisar; ahora el snapshot es completo y el recorte es al responder. *La tabla de
  posiciones crece con todos los donantes del mes, no con diez*: un ranking publicado es un
  hecho del período.

## Absorbidos o a medias

- **23. Higiene de código** (código muerto, setters, logs, nombres). La parte grande se hizo;
  quedan tres cosas anotadas en su propio punto.
- **35. Los buffers "pendientes" en memoria.** No se corrige: quedó absorbido por el anexo
  del punto 3, porque es el mismo código. La mitad de notificaciones se va con la cola; la
  de publicaciones de n8n no, y necesita la tabla de outbox.
