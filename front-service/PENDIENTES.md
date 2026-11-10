# Pendientes técnicos de `front-service`

Registro de problemas conocidos del servicio, con el motivo y la propuesta de arreglo para
que no se pierdan de vista al crecer el código.

**Están ordenados de más urgente a menos urgente**, no por número de punto. El número es un
ID estable y no se renumera nunca, así que quedan huecos. Un punto corregido se borra de esta
lista y pasa a la sección [Corregidos](#corregidos) del final.

El orden no es el de la severidad declarada en cada punto sino el del daño real: cuánto se
rompe cuando pasa, y qué tan fácil es que pase.

| # | Punto | Severidad | Por qué está acá |
|---|---|---|---|
| 1 | 1 | Alta | Los listados descargan listas completas y las renderizan enteras, sin paginación ni límite |
| 2 | 2 | Alta | `HomeController` descarga todas las donaciones y donantes solo para usar `.size()` |
| 3 | 3 | Media | Virtual threads habilitados sin techo de concurrencia sobre llamadas bloqueantes que bufferizan todo |
| 4 | 4 | Media | `spring.thymeleaf.cache=false` en la configuración que se empaqueta en la imagen |
| 5 | 5 | Media | Las sesiones HTTP no tienen timeout ni límite explícito |
| 6 | 6 | Baja | Dependencias y beans sin uso (`mysql-connector-j`, validation, `devtools`, `RestTemplate`) |
| 7 | 7 | Baja | `MaxRAMPercentage=75` sin `mem_limit` en el compose: el heap se calcula contra la RAM del host |

---

## 1. Los listados descargan listas completas y las renderizan enteras, sin paginación ni límite

**Estado:** abierto
**Severidad:** alta
**Archivos:** `src/main/java/.../clients/DonacionesClient.java` (líneas 30-40, 54-64), `src/main/java/.../controllers/DonacionesViewController.java` (líneas 25, 43), `templates/donaciones/listar.html` (línea 48), `templates/donaciones/donantes.html` (línea 45)

### Qué pasa

`RestClient ... .body(new ParameterizedTypeReference<List<...>>(){})` descarga la respuesta HTTP completa en memoria (bytes crudos + deserialización a objetos) y devuelve un `List` sin tope. No hay paginación ni `maxInMemorySize` (ese límite existe en `WebClient`, no en `RestClient`). El controller mete la lista entera en el `Model` y Thymeleaf itera y arma el HTML completo también en memoria.

Con datasets grandes, cada request produce un pico de heap proporcional al tamaño total de donaciones/donantes (y ~2× durante la deserialización), y el template repite el costo al renderizar todas las filas.

### Cómo se dispara

Abrir un listado con muchas donaciones o donantes.

### Propuesta de arreglo

Paginar (`?page=&size=`) o exponer endpoints de conteo/resumen en `donaciones-service`; si se mantiene `RestClient`, acotar el tamaño de respuesta en el backend y renderizar solo la página actual.

---

## 2. `HomeController` descarga todas las donaciones y donantes solo para usar `.size()`

**Estado:** abierto
**Severidad:** alta
**Archivos:** `src/main/java/.../controllers/HomeController.java` (líneas 44-50)

### Qué pasa

El índice (`/`) hace:

```java
var donaciones = donacionesClient.obtenerTodasLasDonaciones();
var donantes  = donacionesClient.obtenerDonantes();
model.addAttribute("totalDonaciones", donaciones.size());
model.addAttribute("totalDonantes", donantes.size());
```

Descarga **todas** las donaciones y **todos** los donantes para usar únicamente `.size()`, descartando los objetos. Es el doble de tráfico y el doble de pico de memoria en la página más visitada, y se repite en cada visita.

### Cómo se dispara

Cada carga de la página de inicio.

### Propuesta de arreglo

Consumir un endpoint de conteo (`/api/donaciones/count`, `/api/personas/count`) o un DTO de resumen; no traer las listas completas.

---

## 3. Virtual threads habilitados sin techo de concurrencia sobre llamadas bloqueantes que bufferizan todo

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/main/resources/application.properties` (línea 3), `src/main/java/.../config/WebClientConfig.java` (líneas 15-21)

### Qué pasa

`spring.threads.virtual.enabled=true` habilita concurrencia prácticamente ilimitada de requests en Tomcat, y cada request hace llamadas bloqueantes (`SimpleClientHttpRequestFactory` → `HttpURLConnection`) que retienen el cuerpo completo en heap. Sin bulkhead ni límite de concurrencia, N requests concurrentes × dataset completo = pico de heap no acotado. No es un leak (GC recupera), pero es la causa típica de OOM bajo carga en este diseño.

### Cómo se dispara

Varias requests concurrentes a listados con datasets grandes.

### Propuesta de arreglo

Acotar la concurrencia de salida (`Semaphore`/bulkhead o pool de conexiones con límite) y paginar las respuestas; alternativamente usar `WebClient` con `maxInMemorySize` y `timeout`.

---

## 4. `spring.thymeleaf.cache=false` en la configuración que se empaqueta en la imagen

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/main/resources/application.properties` (línea 6)

### Qué pasa

Con la caché apagada, Thymeleaf relee y reparsea cada template en cada request, generando objetos y garbage constantes y quemando CPU. Está pensado para desarrollo, pero el archivo es el que se empaqueta en la imagen.

### Cómo se dispara

Uso normal en producción o cualquier entorno que no sea de desarrollo.

### Propuesta de arreglo

`spring.thymeleaf.cache=true` por defecto y dejar `false` solo en un `application-dev.properties`/perfil de desarrollo.

---

## 5. Las sesiones HTTP no tienen timeout ni límite explícito

**Estado:** abierto
**Severidad:** media
**Archivos:** `src/main/java/.../config/SecurityConfig.java` (líneas 20-42)

### Qué pasa

El login por formulario crea una `HttpSession` por usuario con el `SecurityContext`. No hay `server.servlet.session.timeout` ni control de sesiones concurrentes, así que se acumulan durante el timeout por defecto (30 min). Con muchos usuarios, la memoria de sesiones crece y solo se libera por expiración.

### Cómo se dispara

Muchos usuarios con sesión abierta o sesiones abandonadas.

### Propuesta de arreglo

Definir `server.servlet.session.timeout` (por ejemplo `15m`) y, si aplica, `sessionManagement().maximumSessions(...)`.

---

## 6. Dependencias y beans sin uso (`mysql-connector-j`, validation, `devtools`, `RestTemplate`)

**Estado:** abierto
**Severidad:** baja
**Archivos:** `pom.xml` (líneas 40-44, 46-50, 60-65), `src/main/java/.../config/WebClientConfig.java` (líneas 24-30)

### Qué pasa

- El front no tiene JPA/DataSource, así que `mysql-connector-j` no se usa.
- `spring-boot-starter-validation` no se usa en los controllers/DTOs revisados.
- `spring-boot-devtools` no va en el JAR repackageado, pero en ejecución local activa el restart classloader y retiene dos classloaders, elevando el uso de memoria en desarrollo.
- Se instancia un `RestTemplate` que ningún componente inyecta.

Es overhead de arranque/heap pequeño, no una fuga.

### Cómo se dispara

Desarrollo local o un arranque normal del servicio.

### Propuesta de arreglo

Quitar `mysql-connector-j` y validation si no se usan, mantener `devtools` con scope/opcional solo para desarrollo, y eliminar el bean `restTemplate()`.

---

## 7. `MaxRAMPercentage=75` sin `mem_limit` en el compose: el heap se calcula contra la RAM del host

**Estado:** abierto
**Severidad:** baja
**Archivos:** `Dockerfile` (línea 30), `docker-compose.yml` (sección `front-service`), `PENDIENTES.md` raíz (punto 6)

### Qué pasa

El `ENTRYPOINT` usa `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`. Si el contenedor no tiene límite de memoria (como en el compose actual), la JVM usa la RAM del host como base y puede dimensionar un heap de hasta el 75 % de la máquina, aunque el front sea liviano.

### Cómo se dispara

Levantar el servicio con `docker compose up` en un host con RAM abundante.

### Propuesta de arreglo

Fijar `mem_limit`/`deploy.resources.limits.memory` para `front-service` (por ejemplo 384 MB); `MaxRAMPercentage` solo tiene sentido junto a un límite de cgroup (ver punto 6 raíz).

---

## Corregidos

Todavía no hay ningún punto corregido en este archivo.
