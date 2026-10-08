# Pendientes técnicos de la raíz del repo

Registro de problemas que no pertenecen a un solo módulo: el build, los scripts de prueba y la
infraestructura que comparten los cuatro servicios. Los pendientes de cada servicio están en su
propio `PENDIENTES.md` (`incentivos-service/`, `donaciones-service/`,
`notificaciones-service/`, `logisticas-service/`).

**Están ordenados de más urgente a menos urgente**, no por número de punto. El número es un ID
estable y no se renumera nunca, así que quedan huecos. Un punto corregido se borra de esta
lista y pasa a la sección [Corregidos](#corregidos) del final.

| # | Punto | Por qué está acá |
|---|---|---|
| 1 | 1 | `docker compose up` no puede construir ningún servicio: el contexto es el módulo y el Dockerfile exige la raíz |
| 2 | 2 | Los scripts de prueba pegan a un endpoint público que no existe, y sus aserciones lo disimulan |
| 3 | 3 | Los scripts SQL apuntan al contenedor `tp-mysql` y el compose declara `tpa-mysql` |

Quedan tres abiertos.

---

## 1. `docker compose up` no puede construir los cuatro servicios: el contexto es el módulo y el Dockerfile exige la raíz

**Estado:** abierto
**Severidad:** alta
**Archivos:** `docker-compose.yml` (líneas 67, 90, 115, 160), `*/Dockerfile` (líneas 11 y 16),
`incentivos-service/compose.dev.yml` (líneas 24-25), `Readme.md` (líneas 96-104)

### Qué pasa

`docker-compose.yml` construye cada servicio con el módulo como contexto:

```yaml
incentivos-service:
  build: ./incentivos-service
```

Pero los cuatro Dockerfiles están escritos para el **repo completo**:

```dockerfile
COPY . .
RUN mvn clean package -pl ${SERVICE_NAME} -am -DskipTests
```

Con el contexto limitado al módulo, dentro del contenedor no existe el `pom.xml` de la raíz
(donde vive el parent `ddsi-tp-template` que heredan los cuatro poms), ni el resto del reactor
que `-pl incentivos-service -am` necesita. El build falla con *Non-resolvable parent POM* o con
*Could not find the selected project in the reactor*.

El propio archivo lo declara como si fuera la solución —líneas 3-6: *"los contenedores no se
podían construir desde el repo... ahora cada microservicio se construye desde su módulo con
`build:`"*—, pero el diagnóstico está al revés: el módulo solo no alcanza.

Contradice además el `Readme.md` (líneas 98-99: *"El contexto de construcción de Docker siempre
debe ser la **raíz** del proyecto, porque los microservicios dependen del `pom.xml` padre"*) y el
`compose.dev.yml` de cada servicio, que sí lo hace bien:

```yaml
build:
  context: ..
  dockerfile: incentivos-service/Dockerfile
```

### Propuesta

Para cada uno de los cuatro servicios del `docker-compose.yml`:

```yaml
build:
  context: .
  dockerfile: incentivos-service/Dockerfile
```

Mismo patrón que ya usa `compose.dev.yml`. Los Dockerfiles no hace falta tocarlos.

---

## 2. Los scripts de prueba pegan a un endpoint público que no existe, y sus aserciones lo disimulan

**Estado:** abierto
**Severidad:** media
**Archivos:** `test-conexiones.ps1` (líneas 177, 195, 249, 251),
`e2e-completo.ps1` (líneas 297-299), `incentivos-service/.../controllers/PerfilController.java`
(línea 146)

### Qué pasa

La ruta real del perfil público es

```
GET /api/perfiles/{idUsuario}/publico
```

(`PerfilController.java:146`), y así está registrada en el backlog de incentivos (punto 8). Los
scripts pegan al revés:

```
GET /api/perfiles/publico/{id}
```

(`test-conexiones.ps1:195` y `:249`, `e2e-completo.ps1:297`). Esa ruta no matchea ningún
mapping —el literal `publico` tendría que estar al final del patrón—, así que el 404 que
devuelve es de *no handler*, no de *perfil inexistente*.

El problema es que las aserciones no lo distinguen: aceptan cualquier 4xx
(`test-conexiones.ps1:177` y `:251`; `e2e-completo.ps1:299`) y marcan **[OK]** con mensajes
falsos — *"incentivos responde el endpoint publico"*, *"rechaza el id inexistente... que es lo
correcto"*, *"incentivos llego a donating"*— sin haber tocado jamás el endpoint real. El
chequeo de conectividad del perfil público no prueba nada.

### Propuesta

1. Apuntar a `http://localhost:8082/api/perfiles/{uuid}/publico`.
2. Para esperar 200, crear el perfil antes (el script ya lo hace en otras fases).
3. Para el caso "rechaza", distinguir el 404 de *no handler* del 404 de *perfil inexistente*:
   validar el cuerpo del error, o probar con un perfil creado previamente.

---

## 3. Los scripts SQL apuntan al contenedor `tp-mysql` y el compose declara `tpa-mysql`

**Estado:** abierto
**Severidad:** media
**Archivos:** `test-conexiones.ps1` (línea 60), `e2e-completo.ps1` (líneas 56, 85, 156, 159),
`docker-compose.yml` (línea 19)

### Qué pasa

Los scripts ejecutan SQL con:

```powershell
docker exec tp-mysql mysql ...
```

pero el compose del repo declara `container_name: tpa-mysql`. Con el compose del repo, **todas
las verificaciones por SQL fallan** (*No such container: tp-mysql*), incluida la que es
evidencia de la integración entre servicios:
`SELECT COUNT(*) FROM impacto_donacion` (`test-conexiones.ps1:237-239`), que es lo que prueba
que la donación llegó a `incentivos_db`.

O sea que las fases que dependen de SQL podrían estar fallando por el nombre del contenedor y no
por el código.

### Propuesta

Alinear un nombre con el otro. Lo más probable es que los scripts se escribieran contra otro
compose (o contra un `docker run` manual con ese nombre), así que conviene renombrar en los
scripts a `tpa-mysql`, que es el que está versionado.

---

## Nota: los scripts están sin trackear en git

`test-conexiones.ps1` y `e2e-completo.ps1` aparecen como `??` en `git status`: existen en el
árbol de trabajo pero no están en ningún commit. Cualquiera que clone el repo no los tiene, y
los puntos 2 y 3 de este archivo tampoco. Lo mismo pasa con la carpeta `BOOT-INF/` en la raíz,
que parece un artefacto de build (un jar descomprimido) y convendría borrar o ignorar.

---

## Corregidos

Todavía no hay ningún punto corregido en este archivo.
