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
| 1 | 5 | Credenciales reales en los `.env.example` versionados |
| 2 | 1 | `docker compose up` no puede construir ningún servicio: el contexto es el módulo y el Dockerfile exige la raíz |
| 3 | 2 | Los scripts de prueba pegan a un endpoint público que no existe, y sus aserciones lo disimulan |
| 4 | 3 | Los scripts SQL apuntan al contenedor `tp-mysql` y el compose declara `tpa-mysql` |
| 5 | 4 | El despliegue con cada servicio en una instancia separada no está documentado ni tiene los recaudos que exige |
| 6 | 6 | Ningún servicio tiene techo de memoria: el compose no declara `mem_limit` y el heap se dimensiona contra la RAM del host |

Quedan seis abiertos.

---

## 5. Credenciales reales en los `.env.example` versionados

**Estado:** abierto
**Severidad:** alta
**Archivos:** `logisticas-service/.env.example` (líneas 10 y 17), `donaciones-service/.env.example`
(líneas 10-13), `incentivos-service/.env.example` (línea 8), `notificaciones-service/.env.example`
(línea 7)

### Qué pasa

Los cinco `.env.example` están trackeados en git y solo `.env` está en `.gitignore`, pero varios
ejemplos no son plantillas: traen credenciales reales.

- `logisticas-service/.env.example:10` tiene `DB_PASSWORD=10032001` y `:17` una `MONGODB_URI` de
  Atlas con usuario y contraseña (`vfondovila_db_user`).
- `donaciones-service/.env.example:10-13` apunta a un broker de Railway
  (`shuttle.proxy.rlwy.net`) con usuario y contraseña.
- `incentivos-service/.env.example:8` y `notificaciones-service/.env.example:7` traen usuarios
  reales (`incentivos_user`, `marcelo`) con contraseña vacía.

El encabezado de cada archivo dice "completá la contraseña", pero en logística ya está escrita:
quien clone el repo obtiene las credenciales de esa base y del clúster de Mongo.

### Propuesta

1. Rotar la contraseña de MySQL que aparece en el archivo y la del usuario `vfondovila_db_user` de
   Atlas.
2. Reemplazar los valores por placeholders: `DB_PASSWORD=` y
   `MONGODB_URI=mongodb+srv://<usuario>:<password>@<cluster>/...`.
3. Revisar si las credenciales están en commits anteriores (`git log -p -- '**/.env.example'`); si
   aparecen, la rotación es obligatoria y conviene limpiar el historial con `git filter-repo` o BFG.
4. Dejar una sola convención: los `.env.example` solo llevan valores vacíos o de ejemplo, nunca
   hosts, usuarios ni contraseñas de infraestructura real.

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

## 4. El despliegue con cada servicio en una instancia separada no está documentado ni tiene los recaudos que exige

**Estado:** abierto
**Severidad:** media
**Archivos:** `Readme.md` (secciones "Docker Compose" y "Desarrollo local"), `docker-compose.yml`,
`*/pom.xml` (ningún servicio declara `spring-boot-starter-actuator`),
`*/src/main/resources/application.properties`

### Qué pasa

A nivel configuración el sistema ya puede correr en hosts separados: las URLs entre servicios
(`servicio.*.url`), el puerto (`SERVER_PORT`), las bases (`DB_URL`), Mongo (`MONGODB_URI`) y el
broker (`RABBITMQ_*`) salen de variables de entorno, y ningún servicio llama a otro al arrancar
(no hay `@PostConstruct` ni `CommandLineRunner` cruzado): ningún servicio se cae porque otro no
esté levantado. Pero el único despliegue documentado y probado es un solo host con
`docker-compose.yml`, y el escenario multi-host tiene requisitos que hoy no están cubiertos:

1. **Broker único compartido.** RabbitMQ no se replica por servicio: los cuatro usan el mismo
   broker y el mismo exchange consistent-hash. Además, `logistica.integracion.hash` lo declara
   solo logística, así que en un broker nuevo donaciones no puede asignar hasta que logística
   arranque al menos una vez (punto 50 de `donaciones-service/PENDIENTES.md`).
2. **Sin health checks.** Ningún servicio declara actuator ni expone `/health`; un balanceador no
   tiene cómo distinguir una instancia viva de una colgada.
3. **Endpoints internos sin autenticación.** `GET /api/personas/**`, `/api/admins/**`,
   `/api/perfiles/interno/**` y compañía no validan identidad. En un solo host quedan detrás de
   localhost; en hosts separados, si se publican, quedan abiertos a cualquiera.
4. **Sin service discovery.** Las URLs son direcciones fijas: mover o reemplazar una instancia
   obliga a reconfigurar y reiniciar a todos sus consumidores.
5. **Casos ya registrados que pegan directo en este escenario:** callback a `localhost:8086` en
   `ProveedorRutasExternoSimulado` (punto 31 de logística), `RestTemplate` sin timeouts (punto 49
   de donaciones), cursor de eventos en memoria (punto 75 de donaciones), `synchronized` local
   entre réplicas (punto 67 de logística) e idempotencia check-then-act de notificaciones
   (punto 21).

### Matriz de degradación en runtime

Ninguna caída impide arrancar a los demás; lo que cambia es qué deja de funcionar:

| Si falta | Efecto |
|---|---|
| `incentivos-service` | `POST /api/personas` responde 500 y no persiste el donante (`DonanteService.java:102` llama a incentivos antes de guardar y relanza). La importación CSV sigue, persiste localmente y acumula los errores en el reporte. Actualización y baja degradan con un warning. La notificación de impacto de una entrega se pierde sin reintento (punto 76 de donaciones). |
| `donaciones-service` | `incentivos` sigue vivo, pero `verificarAdmin` devuelve `false` (403 en endpoints de administración) y el contacto del donante queda en `null`. No hay caída. |
| `logisticas-service` | `donaciones` sigue vivo. `asignarPropuesta` publica al exchange hash y relanza: si el exchange no existe (logística nunca arrancó) hay rollback y 500; si ya existe, la cola durable acumula y procesa al volver. |
| `notificaciones-service` | Nada se cae: la cola durable retiene los avisos. Los publicados antes de que la cola exista se pierden, porque no hay outbox y el productor de donaciones solo loguea el error (`NotificacionesClient.java:37-40`). |
| `n8n` | Nada se cae: incentivos reintenta desde su outbox persistente; notificaciones reintenta, manda a DLQ y permite reintento manual. |
| RabbitMQ | Los cuatro servicios arrancan y reconectan. Fallan las publicaciones síncronas: la asignación a logística da 500 y rollback; las notificaciones se pierden con un log. |
| MySQL o Mongo | Se cae solo el servicio dueño de esa base. Si los cuatro comparten una instancia de MySQL, se caen los cuatro. |

### Propuesta

1. Documentar en el `Readme.md` la topología multi-host: matriz de variables por servicio, broker
   y bases compartidos, y qué infraestructura es externa (Mongo Atlas, n8n).
2. Agregar `spring-boot-starter-actuator` y exponer solo `/actuator/health` en los cinco servicios.
3. Publicar únicamente el front; los puertos de dominio y del broker van en red privada (VPC o
   security groups) con firewall entre servicios.
4. Resolver primero el punto 50 de donaciones y el 31 de logística: sin eso, separar hosts deja la
   asignación rota en un broker limpio y el callback de rutas apuntando a la instancia equivocada.
5. Usar un broker y una instancia de MySQL compartidos (una base por servicio); no una copia de la
   infraestructura por instancia.

---

## 6. Ningún servicio tiene techo de memoria: el compose no declara `mem_limit` y el heap se dimensiona contra la RAM del host

**Estado:** abierto
**Severidad:** alta
**Archivos:** `docker-compose.yml` (secciones de los cinco servicios), `*/Dockerfile` (líneas 25 de incentivos, logísticas y notificaciones; línea 30 de donaciones y front)

### Qué pasa

Ninguna definición de servicio en `docker-compose.yml` declara `mem_limit` ni
`deploy.resources.limits.memory`. Sin límite de cgroup, la JVM toma como referencia la memoria del
**host**, no la que el servicio necesita:

- `donaciones-service` y `front-service` usan `-XX:MaxRAMPercentage=75.0`. En un host de 15 GB eso
  permite un heap de hasta ~11 GB por servicio.
- `incentivos-service`, `logisticas-service` y `notificaciones-service` no pasan ningún flag JVM,
  así que usan el default `MaxRAMPercentage=25`: hasta ~3.75 GB de heap cada uno en el mismo host.

Los servicios son chicos y no necesitan ese techo. El riesgo aparece con los picos ya registrados
en cada backlog (rankings, matchmaking, importación CSV, eventos de logística): la JVM crece porque
puede, no porque haga falta, y el OOM killer del host termina matando procesos sin que el servicio
haya estado realmente saturado.

### Cómo se dispara

Levantar los cinco servicios con `docker compose up` en una máquina con RAM abundante y ejecutar
cualquiera de los barridos masivos documentados en los pendientes de cada módulo.

### Propuesta de arreglo

1. Definir `mem_limit` por servicio en `docker-compose.yml` (por ejemplo 384-512 MB para los
   servicios de dominio y 256-384 MB para el front).
2. Alinear cada Dockerfile con `-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError` para que el
   heap se calcule contra el límite del contenedor y un OOM termine con un exit code visible en
   lugar de quedar colgado.
3. Revisar los puntos de Dockerfile de `incentivos-service` (64), `logisticas-service` (72) y
   `notificaciones-service` (45), que son los tres módulos sin flags JVM.

---

## Corregidos

Todavía no hay ningún punto corregido en este archivo.
