# Pendientes del proyecto

Revisión del enunciado `DDS-TP-Anual-2026.pdf` y del estado visible del repositorio. El detalle de
riesgos técnicos por módulo se conserva en los `PENDIENTES.md` de cada servicio.

El repo contiene cuatro servicios backend y `front-service` (MVC). Los siguientes pendientes son
transversales o corresponden a entregables del TP; no reemplazan la priorización técnica de cada
módulo.

| Prioridad | Pendiente | Evidencia / alcance |
|---|---|---|
| Alta | Completar y versionar la documentación de arquitectura y diseño | No se encontraron diagramas versionados de casos de uso, clases por servicio, componentes/despliegue ni modelos físicos/ERD, ni justificaciones de diseño consolidadas. El enunciado los solicita desde E1 y actualizaciones en las entregas siguientes. |
| Alta | Cerrar autenticación y autorización de extremo a extremo | El front usa Keycloak para login y roles; los backends no validan Keycloak y se protegen con ACL de red. Falta definir si se sumará autenticación servicio-a-servicio en despliegues fuera de la red interna. |
| Alta | Completar cobertura funcional de la interfaz MVC | El front ofrece landing, login, panel y vistas de donaciones/donantes, pero no se identifican vistas y flujos completos para las wireframes E1 ni para perfiles/misiones, entidades/necesidades, administración, logística y seguimiento. |
| Media | Ajustar la importación CSV al requisito de actualización por email | E1 pide actualizar donantes existentes por email y crear los nuevos. El backlog de Donaciones documenta que hoy se tratan como distintos; falta resolver la identificación única y el comportamiento de actualización. |
| Media | Publicar y mantener documentación de APIs | E3 solicita documentación de APIs con Postman y Swagger. Hay colecciones Postman para los cuatro servicios, pero no se encontró dependencia/configuración OpenAPI/Swagger versionada. Revisar que las colecciones cubran todos los endpoints y los flujos de creación, lectura, actualización y borrado con datos propios. |
| Media | Revisar integración resiliente entre servicios | Hay llamadas HTTP y mensajería RabbitMQ. Acordar contratos, reintentos/reconciliación y consistencia ante fallas entre donaciones, incentivos y notificaciones; los riesgos particulares están detallados en los pendientes de esos módulos. |
| Media | Cerrar pendientes de entrega y demostrar despliegue reproducible | Verificar desde un clon limpio el compose completo, configuración por entorno, persistencia de volúmenes y pasos de arranque documentados. No se registran aquí fallas antiguas de compose/scripts: el compose actual usa la raíz como contexto y los scripts mencionados en versiones anteriores no están en el árbol. |

## Cobertura por entrega

- **E1:** Donaciones, segmentación, necesidades, importación CSV y estructura modular están implementadas en parte; faltan los entregables formales de diseño y una UI que cubra las wireframes y perfiles de usuario.
- **E2:** Hay trazabilidad, asignación, incentivos, misiones/insignias, rankings y publicación a n8n. Ver los pendientes de `donaciones-service`, `incentivos-service` y `notificaciones-service` para brechas de comportamiento, robustez y canales. El canal SMS del enunciado se implementa como `telefono`; confirmar que ese alcance queda documentado.
- **E3:** Existe `logisticas-service`, planificación, seguimiento y colecciones Postman. Restan riesgos de producción indicados en su backlog; Swagger/OpenAPI no aparece configurado.
- **E4:** Se encuentra RabbitMQ en compose e integración asíncrona de avisos. Falta documentar el rol de intermediación entre Donaciones y proveedores/servicios de logística y verificar topología y fallas con la arquitectura final.
- **E5:** Los servicios persisten por separado; logística usa MongoDB y los demás servicios usan persistencia relacional. Falta entregar diagramas actualizados de entidades/modelos físicos y justificar decisiones en documentación versionada.
- **E6:** Existe un front MVC con Thymeleaf, Spring Security y login OIDC con Keycloak; el rol admin se valida ahí. Los backends quedan detrás de una ACL de red, no de JWT. Falta cobertura completa de pantallas/roles.

---

## Pendientes técnicos de la raíz del repo

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
| 2 | 4 | El despliegue con cada servicio en una instancia separada no está documentado ni tiene los recaudos que exige |
| 3 | 6 | Cuatro de los cinco servicios no tienen techo de memoria: el heap se dimensiona contra la RAM del host |

Quedan tres abiertos.

---

## 5. Credenciales reales en los `.env.example` versionados

**Estado:** abierto
**Severidad:** alta
**Archivos:** `donaciones-service/.env.example` (líneas 10-13),
`notificaciones-service/.env.example` (línea 7). Los de `logisticas-service`, `incentivos-service` y
`front-service` ya no están versionados (se eliminaron en `develop-front`), pero sus credenciales
siguen en el historial de git.

### Qué pasa

Los `.env.example` que quedan están trackeados y solo `.env` está en `.gitignore`, pero no son
plantillas: traen credenciales reales.

- `donaciones-service/.env.example:10-13` apunta a un broker de Railway
  (`shuttle.proxy.rlwy.net`) con usuario y contraseña.
- `notificaciones-service/.env.example:7` trae el usuario real `marcelo` con contraseña vacía.

El encabezado de cada archivo dice "completá la contraseña", pero los hosts y usuarios reales ya
están escritos: quien clone el repo obtiene esos datos de infraestructura.

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
3. **Endpoints internos sin autenticación.** `GET /api/personas/**` y las lecturas de
   perfiles siguen abiertas. Las escrituras de perfiles aceptan un admin autenticado, una IP de la allowlist
   o el hostname de un servicio interno; en hosts separados hay que configurar esa lista. Las rutas de administración de incentivos, donaciones y logística exigen token.
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
| `donaciones-service` | `incentivos` sigue vivo y sus rutas de administración validan el token por su cuenta. El contacto del donante queda en `null` y las actualizaciones/bajas desde donaciones degradan con warning. No hay caída. |
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

## 6. Dos de los cinco servicios no tienen techo de memoria: el heap se dimensiona contra la RAM del host

**Estado:** abierto
**Severidad:** alta
**Archivos:** `docker-compose.yml` (secciones de donaciones y front), `*/Dockerfile` (línea 30 de donaciones y front)

### Qué pasa

Solo `notificaciones-service`, `logisticas-service` e `incentivos-service` declaran `mem_limit`
y flags JVM; los otros dos no declaran `mem_limit` ni `deploy.resources.limits.memory`. Sin
límite de cgroup, la JVM de esos dos toma como referencia la memoria del **host**, no la que el
servicio necesita:

- `donaciones-service` y `front-service` usan `-XX:MaxRAMPercentage=75.0`. En un host de 15 GB eso
  permite un heap de hasta ~11 GB por servicio.

Los servicios son chicos y no necesitan ese techo. El riesgo aparece con los picos ya registrados
en cada backlog (rankings, matchmaking, importación CSV, eventos de logística): la JVM crece porque
puede, no porque haga falta, y el OOM killer del host termina matando procesos sin que el servicio
haya estado realmente saturado.

### Cómo se dispara

Levantar los cinco servicios con `docker compose up` en una máquina con RAM abundante y ejecutar
cualquiera de los barridos masivos documentados en los pendientes de cada módulo.

### Propuesta de arreglo

1. Definir `mem_limit` para los dos servicios restantes en `docker-compose.yml` (por ejemplo
   384-512 MB para `donaciones-service` y 256-384 MB para el front).
2. Alinear cada Dockerfile con `-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError` para que el
   heap se calcule contra el límite del contenedor y un OOM termine con un exit code visible en
   lugar de quedar colgado.
3. Revisar los puntos de Dockerfile de `donaciones-service` y `front-service`.
   `incentivos-service` (64) y `logisticas-service` ya quedaron cubiertos.

---

## Corregidos

- **1. `docker compose up` no podía construir los servicios** — el `docker-compose.yml` actual usa
  `context: .` y `dockerfile: <servicio>/Dockerfile`; el build desde el módulo ya no aplica.
  Corregido con el merge de `develop-front` (2026-11-10).
- **2. Los scripts de prueba** — `test-conexiones.ps1` y `e2e-completo.ps1` ya no están en el árbol;
  no hay endpoint público roto que probar. Se retiran como pendientes activos.
- **3. Los scripts SQL** — no están versionados en el repo; `tp-mysql` solo aparecía en este archivo.
