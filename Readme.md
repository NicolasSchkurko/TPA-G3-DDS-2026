# TPA DDSI 2026 — Grupo 3

Plataforma de donaciones de bienes con incentivos para donantes (gamificación), notificaciones y logística. Trabajo Práctico Anual de **Diseño de Sistemas de Información (DDSI)** — UTN FRBA.

[![CI](https://github.com/NicolasSchkurko/TPA-G3-DDS-2026/actions/workflows/ci.yml/badge.svg)](https://github.com/NicolasSchkurko/TPA-G3-DDS-2026/actions/workflows/ci.yml)

**Integrantes:**
- María Mercedes Otero
- Nicolas Pannunzio
- Mateo Julián Galanti
- Sofía Muñoz
- Oriana Bellorín
- Valentín Fondovila
- Nahuel Marek
- Nicolas Schkurko
- Marcelo He Zhen

## Arquitectura

Cuatro microservicios Spring Boot, cada uno con su propia base MySQL. La integración entre servicios es asíncrona por **RabbitMQ** (no por HTTP): las donaciones se publican para logística y los eventos de trazabilidad se consumen para notificar. Los flujos que requieren un workflow externo se resuelven con **n8n** por webhooks.

| Servicio | Responsabilidad | Puerto | Base de datos |
| --- | --- | --- | --- |
| `incentivos-service` | Perfiles de donante, misiones e insignias, rankings y métricas | 8082 | `incentivos_db` |
| `notificaciones-service` | Registro y envío de notificaciones | 8083 | `notificaciones_db` |
| `donaciones-service` | Donaciones (alta e importación CSV), personas y entidades beneficiarias | 8084 | `donaciones_db` |
| `logisticas-service` | Asignación y seguimiento logístico de las donaciones | 8086 | `logisticas_db` |

Cada servicio expone su API REST documentada con Swagger en `/api-docs`.

## Estructura del repositorio

```
pom.xml                     # POM padre: reactor de Maven, versiones y dependencyManagement
donaciones-service/         # Donaciones, personas y entidades beneficiarias — puerto 8084
incentivos-service/         # Gamificación: perfiles, misiones, insignias, rankings — puerto 8082
notificaciones-service/     # Notificaciones — puerto 8083
logisticas-service/         # Logística de las donaciones — puerto 8086
docker/                     # Imagen de RabbitMQ para Compose
postman/                    # Colecciones para probar los servicios (ciclo completo, smoke, webhooks n8n)
docker-compose.yml          # Servicios + MySQL + RabbitMQ + n8n
```

## Requisitos previos

- JDK 21
- Maven 3.9+
- Docker (opcional, solo para construir y ejecutar contenedores)

## Tecnologías

| Tecnología          | Versión       |
|---------------------|---------------|
| Java                | 21            |
| Spring Boot         | 3.2.5         |
| Spring Cloud BOM    | 2025.1.1      |
| Lombok              | 1.18.38       |
| Maven               | 3.9+          |

El BOM de Spring Cloud está declarado en el POM padre para que los módulos puedan incorporar dependencias de Spring Cloud sin especificar versión explícita.

## Desarrollo local (Maven)

Todos los comandos se ejecutan desde la **raíz del proyecto**.

```bash
# Compilar todos los módulos
mvn clean install

# Ejecutar un servicio
mvn spring-boot:run -pl incentivos-service
```

Maven resuelve las dependencias entre módulos directamente desde el reactor.

## Tests

```bash
# Suite completa
mvn test

# Un módulo
mvn test -pl incentivos-service
```

Los tests no necesitan infraestructura: los de persistencia de `incentivos-service` levantan H2 en memoria y el resto son unitarios con mocks. La suite corre en CI (GitHub Actions) por módulo para `donaciones`, `incentivos` y `notificaciones`. La de `logisticas-service` todavía no está estable y queda fuera del CI hasta arreglarla ([issue #2](https://github.com/NicolasSchkurko/TPA-G3-DDS-2026/issues/2)).

## Docker Compose

Desde la raíz del repositorio, Compose construye los cuatro servicios con el contexto del proyecto y levanta MySQL, RabbitMQ y n8n. n8n queda disponible en `http://localhost:5679` y conserva su configuración en un volumen:

```bash
docker compose up --build
```

Para ejecutar en segundo plano:

```bash
docker compose up --build -d
docker compose ps
```

Puertos disponibles desde la máquina local:

| Componente | Puerto |
|------------|--------|
| Incentivos | 8082 |
| Notificaciones | 8083 |
| Donaciones | 8084 |
| Logísticas | 8086 |
| RabbitMQ AMQP | 5672 |
| Panel de RabbitMQ | 15672 |
| n8n | 5679 |
| MySQL Incentivos | 3309 |
| MySQL Notificaciones | 3310 |
| MySQL Donaciones | 3311 |
| MySQL Logísticas | 3312 |

Los contenedores se comunican usando los nombres de servicio de Compose y el puerto interno del contenedor. Los volúmenes mantienen los datos al recrear contenedores; no usar `docker compose down -v` salvo que se quiera borrar también esos datos.

n8n arranca sin workflows. Para que se procesen publicaciones, hay que configurar y activar los webhooks `incentivos` y `notificaciones`; los servicios ya apuntan a `http://n8n:5678/webhook/<ruta>` dentro de Compose.

RabbitMQ es el canal de integración entre servicios: donaciones publica las donaciones asignadas para logística y consume los eventos de trazabilidad para notificar. Las colas y los listeners viven en el código de cada servicio; configurar el broker en Compose por sí solo no los reemplaza.

## Documentación por servicio

Cada microservicio tiene su Swagger en `/api-docs`, y la carpeta `postman/` trae colecciones para probar los flujos (entre ellas, un ciclo completo entre servicios).

Los cuatro mantienen un `PENDIENTES.md` propio con los problemas técnicos conocidos que quedaron abiertos. Los IDs nunca se renumeran, así que hay huecos: cada punto dice su severidad, los archivos afectados y una propuesta concreta. Lo que ya se corrigió queda en la sección `# Corregidos` de cada archivo, con el motivo por el que se cambió.

## Pendiente de enunciado: visibilidad configurable de insignias

> Estado: **no implementado** en `incentivos-service`.

El enunciado pide que las insignias obtenidas puedan visualizarse en el perfil de la persona donante *"siempre que la persona usuaria las configure como visibles"*. O sea, la visibilidad tiene que ser **una decisión de la persona donante**, no un dato que el servicio asuma.

Hoy no hay forma de configurarla:

- `InsigniaObtenida` (`models/entities/Perfil/InsigniaObtenida.java`) solo tiene `perfil`, `insignia` y `fechaObtencion`. **No existe el campo de visibilidad.**
- En consecuencia, `GET /api/perfiles/{idUsuario}/insignias` devuelve **todas** las insignias otorgadas, sin filtro.
- No hay endpoint para cambiar ese estado.

### Qué hay que hacer

1. Agregar `Boolean visible` a `InsigniaObtenida`, con `true` por defecto en el constructor para no cambiar el comportamiento de las insignias ya emitidas.
2. Exponer un endpoint de toggle, por ejemplo `PUT /api/perfiles/{idUsuario}/insignias/{idInsignia}/visibilidad`.
3. Filtrar por `visible` en el listado de insignias y en el DTO que se expone públicamente, dejando las insignias ocultas fuera de la respuesta pero **sin** borrar el registro (sigue contando para el ranking y para el historial).
4. Solo la persona dueña del perfil debería poder cambiar la visibilidad.
