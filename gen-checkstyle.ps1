param(
  [string]$Base = "$env:TEMP\google_checks.xml",
  [string]$Destino = "incentivos-service\config\checkstyle\checkstyle.xml"
)

$utf8 = [System.Text.UTF8Encoding]::new($false)
$x = [System.IO.File]::ReadAllText($Base, $utf8)

# Ojo con el orden: la declaracion XML tiene que ser lo primero del archivo y el DOCTYPE
# va despues. Si se rompe cualquiera de los dos, Checkstyle no parsea la config y sale con
# 0 findings sin avisar: un falso "todo limpio" que no sirve para nada.
$decl = $x.Substring(0, $x.IndexOf('?>') + 2)
$resto = $x.Substring($x.IndexOf('?>') + 2)

$cabecera = @'
<!--
  Checkstyle del proyecto.

  Es el "Google Checks" de Checkstyle 14.1.0 (el mismo que trae el plugin de IntelliJ)
  con las convenciones que este proyecto realmente usa. Que sea una copia con ajustes y no
  un archivo propio es a proposito: se puede comparar contra el upstream con `diff` y se ve
  exactamente que se toco y por que. Se regenera con `gen-checkstyle.ps1` de la raiz.

  Lo que se cambia respecto de Google:

  1. Indentacion de 4 espacios, no de 2. El proyecto usa 4; obligar a 2 seria reformatear
     mas de 100 archivos sin cambiar una sola linea de logica.

  2. `PackageName` admite un segmento con mayuscula inicial. Los paquetes son `dto.Admin`,
     `dto.Persona`, `entidades.Insignia`: son contenedores de nombres, y pasarlos a
     minuscula tocaria cada import del proyecto.

  3. `AbbreviationAsWordInName` admite abreviaturas de hasta 3 letras. `CategoriaDTO` y
     `ImpactoDonacionDTO` son los nombres correctos en Java; `CategoriaDto` seria peor.

  4. `GoogleMethodName` se reemplaza por `MethodName`. `GoogleMethodName` rechaza una
     minuscula seguida de mayuscula, que en camelCase castellano es lo normal:
     `moverAPosicion`, `convertirPerfilADTO`, `obtenerCategoriaSiguiente`.

  5. Lineas de hasta 120, y se perdona una linea que sea solo un literal de texto (las
     descripciones de OpenAPI son largas por naturaleza y partirlas en concatenaciones las
     hace ilegibles). Lo que sigue prohibiendo es codigo largo.

  6. `MissingJavadocMethod` pide javadoc solo en metodos publicos de 3 lineas o mas. Un
     getter de una linea no necesita un comentario que repita el nombre; una transicion de
     dominio si. Y se silencia en DTO, repositorios y tests, donde el nombre ya describe.

  Lo que NO se toca: todo lo que detecta defectos de verdad. NeedBraces, AvoidStarImport,
  FileTabCharacter, OperatorWrapNL, VariableDeclarationUsageDistance, EqualsHashCode,
  MissingSwitchDefault, FallThrough, IllegalCatch, y el resto siguen igual que en Google.
-->
'@

$out = $decl + "`r`n" + $cabecera.TrimEnd() + "`r`n" + $resto

# --- 1. Indentacion de 4 ---
$out = $out.Replace('<property name="basicOffset" value="2"/>', '<property name="basicOffset" value="4"/>')
$out = $out.Replace('<property name="braceAdjustment" value="2"/>', '<property name="braceAdjustment" value="0"/>')
$out = $out.Replace('<property name="caseIndent" value="2"/>', '<property name="caseIndent" value="4"/>')
$out = $out.Replace('<property name="throwsIndent" value="4"/>', '<property name="throwsIndent" value="8"/>')
# arrayInitIndent 8: los @ApiResponses(value = { ... }) sangran con 8.
$out = $out.Replace('<property name="arrayInitIndent" value="2"/>', '<property name="arrayInitIndent" value="8"/>')

# --- 2. PackageName con segmento en mayuscula ---
$out = $out.Replace('value="^[a-z]+(\.[a-z][a-z0-9]*)*$"/>', 'value="^[a-z]+(\.[A-Za-z][a-zA-Z0-9]*)*$"/>')

# --- 3. Abreviaturas de hasta 3 letras ---
$out = $out.Replace('<property name="allowedAbbreviationLength" value="0"/>', '<property name="allowedAbbreviationLength" value="3"/>')
$out = $out.Replace('<property name="ignoreFinal" value="false"/>', '<property name="ignoreFinal" value="true"/>')

# --- 4. GoogleMethodName -> MethodName ---
$out = $out.Replace('<module name="GoogleMethodName"/>', '<module name="MethodName"/>')

# NOTA: `TextBlockGoogleStyleFormatting` no se toca. Exige que el `"""` de apertura este
# en su propia linea, sin nada antes, o sea:
#
#     @Query(
#             """
#             SELECT ...
#             """)
#
# Son tres lineas de ceremonyia por cada consulta JPQL. La forma de una sola linea
# (`@Query("""` con el contenido y el cierre sangrados) se lee igual de bien. El modulo no
# es configurable (no tiene la propiedad `openingQuotesOnNewLine` en 14.1.0), asi que en
# vez de relajar la regla para todo el proyecto se silencia SOLO en los repositorios,
# que son los unicos que tienen text blocks.

# --- 5. Lineas largas ---
$out = $out.Replace('<property name="max" value="100"/>', '<property name="max" value="120"/>')
$out = $out.Replace(
  'value="^package.*|^import.*|href\s*=\s*&quot;[^&quot;]*&quot;|http://|https://|ftp://"/>',
  'value="^package.*|^import.*|href\s*=\s*&quot;[^&quot;]*&quot;|http://|https://|ftp://|^[^&quot;&apos;]*&quot;[^&quot;]*&quot;[,;)]?$"/>')

# --- 6. Javadoc solo donde aporta ---
$out = $out.Replace('        <module name="MissingJavadocMethod">' + "`r`n" + '          <property name="scope" value="protected"/>',
                    '        <module name="MissingJavadocMethod">' + "`r`n" + '          <property name="scope" value="public"/>' + "`r`n" + '          <property name="minLineCount" value="3"/>')

# 6b. Tres ajustes mas, todos con el mismo motivo: la documentacion ya existe, en otro
# lugar, y un javadoc al lado seria duplicarla.
#
#   a) Las anotaciones de Spring web y de OpenAPI cuentan como documentacion. Cada
#      endpoint del proyecto tiene `@Operation(summary = ..., description = ...)`, que es
#      lo que sale en el Swagger. Un javadoc con el mismo texto seria copiar y pegar.
#
#   b) Los constructors no se documentan. Los que hay son casi todos de inyeccion de
#      dependencias, donde el nombre del parametro ya dice que se inyecta
#      (`public CategoriaController(CategoriaService service)`). Lo que no aparece en el
#      nombre, como el criterio de desempate del constructor de `Mision`, va en un
#      comentario de linea al lado, que es donde se lee.
$out = $out.Replace(
  '<property name="allowedAnnotations" value="Override, Test"/>',
  '<property name="allowedAnnotations" value="Override, Test, GetMapping, PostMapping, PutMapping, PatchMapping, DeleteMapping, RequestMapping, Operation"/>')

# Los CTOR_DEF salen de los tokens: los constructores no llevan javadoc, por lo explicado
# arriba. El valor original de `tokens` esta partido en dos lineas en el XML de Google, asi
# que se cambian las dos partes por separado en vez de Replace con el bloque entero.
$out = $out.Replace('<property name="tokens" value="METHOD_DEF, CTOR_DEF, ANNOTATION_FIELD_DEF,', '<property name="tokens" value="METHOD_DEF, ANNOTATION_FIELD_DEF,')
$out = $out.Replace('                 COMPACT_CTOR_DEF"/>', '                 COMPACT_CTOR_DEF"/>')

# --- 7. Severidad: esto es lo que hace que la config no sea rompebolas -------------
#
# Google pone TODO en `warning`, y el panel de Problems del IDE muestra los 3.550 avisos
# juntos, mezclados con los de cualquier otra inspeccion. Cuando se mistura un problema real
# ("falta una llave") con una preferencia de formato ("la linea tiene 101 columnas"), el
# ruido tapa el problema.
#
# Asi que se invierte la logica: TODO en `info` por defecto, y solo se sube a `warning` las
# reglas que detectan defectos de verdad. El panel muestra solo esas, y el resto queda
# documentado pero silencioso.
$defectos = @(
  'ArrayTrailingComma', 'AvoidEscapedUnicodeCharacters', 'AvoidInlineConditionals',
  'AvoidNestedBlocks', 'AvoidStarImport', 'EmptyStatement', 'EqualsAvoidNull',
  'EqualsHashCode', 'FallThrough', 'FileTabCharacter', 'IllegalCatch', 'IllegalImport',
  'IllegalTokenText', 'MissingBraces', 'MissingOverride', 'MissingSwitchDefault',
  'ModifiedControlVariable', 'MultipleVariableDeclarations', 'NestedForDepth',
  'NestedIfDepth', 'NestedTryDepth', 'NeedBraces', 'NoFinalizer', 'NoLineWrap',
  'OneStatementPerLine', 'OneTopLevelClass', 'OuterTypeFilename', 'RedundantImport',
  'SimplifyBooleanExpression', 'SimplifyBooleanReturn', 'StringLiteralEquality',
  'StringSplitter', 'UnnecessaryParentheses', 'UnusedImports', 'VariableDeclarationUsageDistance',
  'VisibilityModifier', 'WhitespaceAfter', 'WhitespaceAround'
)

# Subir a warning solo esos, y bajar el resto a info.
#
# OJO con el patron: `[ \t]*` y no `\s*`. Con `(?m)^\s*` el grupo captura tambien los
# saltos de linea anteriores (porque \s matchea \n), y al reinsertar el texto duplica
# lineas en blanco hasta romper el XML. Ya paso: el archivo quedo malformado y
# Checkstyle salio con 0 findings en vez de avisar.
foreach ($regla in $defectos) {
  # Solo los que existen en el archivo de Google, para no fallar si el upstream cambia.
  $patron = '(?m)^([ \t]*)<module name="' + [regex]::Escape($regla) + '"(/?)>'
  if ($out -match $patron) {
    if ($matches[2] -eq '/') {
      # self-closing: hay que abrirlo, meter la propiedad y volverlo a cerrar
      $out = [regex]::Replace(
        $out,
        '(?m)^([ \t]*)<module name="' + [regex]::Escape($regla) + '"/>',
        '$1<module name="' + $regla + '">$1  <property name="severity" value="warning"/>$1</module>',
        1)
    } else {
      $out = [regex]::Replace(
        $out,
        $patron,
        '$1<module name="' + $regla + '">$1  <property name="severity" value="warning"/>',
        1)
    }
  }
}
$out = $out.Replace(
  '<property name="severity" value="${org.checkstyle.google.severity}" default="warning"/>',
  '<property name="severity" value="info"/>')

$supresiones = @'

  <!--
    Los DTO no llevan javadoc por tipo: el nombre de la clase ya lo dice, y un comentario
    que repita el nombre no aporta nada.
  -->
  <module name="SuppressionSingleFilter">
    <property name="checks" value="MissingJavadocType|MissingJavadocMethod|JavadocParagraph"/>
    <property name="files" value="[\\/]dto[\\/]"/>
  </module>

  <!--
    Lo mismo en los repositorios: los nombres de las consultas derivadas de Spring Data ya
    dicen que consultan (`findAllByCategoriaActual`, `existsByIdUsuario`). Lo que si lleva
    comentario son las de `@Query`, porque ahi lo que explica la consulta es el JPQL.
  -->
  <module name="SuppressionSingleFilter">
    <property name="checks" value="MissingJavadocMethod|JavadocVariable"/>
    <property name="files" value="[\\/]repositories[\\/]"/>
  </module>

  <!-- Lo mismo en los tests: un @DisplayName ya describe el caso. -->
  <module name="SuppressionSingleFilter">
    <property name="checks" value="MissingJavadocType|MissingJavadocMethod|JavadocParagraph|JavadocVariable"/>
    <property name="files" value="[\\/]src[\\/]test[\\/]"/>
  </module>

  <!--
    Los repositorios son los unicos con text blocks (los JPQL de los @Query). Google exige
    que el `"""` de apertura este en su propia linea, sin nada antes:

        @Query(
                """
                SELECT ...
                """)

    Son tres lineas de ceremonyia por consulta, y el modulo no tiene la propiedad
    `openingQuotesOnNewLine` en Checkstyle 14.1.0, asi que no se puede relajar solo esa
    parte. La forma de una sola linea se lee igual de bien:

        @Query("""
                SELECT ...
                """)

    Se silencia aqui y no en todo el proyecto para que la regla siga funcionando en
    cualquier text block que se escriba fuera de un repositorio.
  -->
  <module name="SuppressionSingleFilter">
    <property name="checks" value="TextBlockGoogleStyleFormatting"/>
    <property name="files" value="[\\/]repositories[\\/]"/>
  </module>
'@

$idx = $out.LastIndexOf("</module>")
$out = $out.Substring(0, $idx) + $supresiones.TrimStart() + "`r`n" + $out.Substring($idx)

New-Item -ItemType Directory -Force -Path (Split-Path $Destino -Parent) | Out-Null
[System.IO.File]::WriteAllText($Destino, $out, $utf8)
Write-Output ("escrito: " + $Destino + "  (" + ([System.IO.File]::ReadAllLines($Destino, $utf8).Count) + " lineas)")
