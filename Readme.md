ddsi - Grupo 3

Integrantes: 
- María Mercedes Otero 
- Nicolas Pannunzio 
- Mateo Julián Galanti 
- Sofía Muñoz 
- Oriana Bellorín
- Valentín Fondovila
- Nahuel Marek
- Nicolas Schkurko
- Marcelo He Zhen

# ddsi-tp-template

Plantilla base para el trabajo práctico de DDSI (UTN FRBA). Implementa una arquitectura de servicios con Spring Boot, usando un reactor de Maven multi-módulo.

---

## Requisitos previos

- JDK 21
- Maven 3.9+
- Docker (opcional, solo para construir y ejecutar contenedores)

---

## Estructura del repositorio

```
ddsi-tp-template/
├── pom.xml                    # POM padre: versiones y dependencyManagement
├── common-lib/                # (obsoleto, ver PENDIENTES.md de cada servicio)
├── donaciones-service/        # Servicio de donaciones — puerto 8084
├── incentivos-service/         # Servicio de gamificación — puerto 8082
├── notificaciones-service/    # Servicio de notificaciones — puerto 8083
└── logisticas-service/        # Servicio de logística — puerto 8086
```

> `common-lib/` ya no participa del build (no está en `<modules>` del POM padre ni lo
> referencia ningún servicio). Ver la sección de pendientes de cada módulo.

---

## Tecnologías

| Tecnología          | Versión       |
|---------------------|---------------|
| Java                | 21            |
| Spring Boot         | 3.2.5         |
| Spring Cloud BOM    | 2025.1.1      |
| Lombok              | 1.18.38       |
| Maven               | 3.9+          |

El BOM de Spring Cloud está declarado en el POM padre para que los módulos puedan incorporar dependencias de Spring Cloud sin especificar versión explícita.

---

## Puertos

| Servicio              | Puerto |
|-----------------------|--------|
| `incentivos-service`  | 8082   |
| `notificaciones-service` | 8083 |
| `donaciones-service`  | 8084   |
| `logisticas-service`  | 8086   |

---

## Desarrollo local (Maven)

Todos los comandos se ejecutan desde la **raíz del proyecto**.

### Compilar todos los módulos

```bash
mvn clean install
```

### Ejecutar un servicio

```bash
mvn spring-boot:run -pl incentivos-service
```

Maven resuelve las dependencias entre módulos directamente desde el reactor.

### Tests

```bash
mvn test -pl incentivos-service
```

---

## Construcción de imágenes Docker

El contexto de construcción de Docker siempre debe ser la **raíz** del proyecto, porque
los microservicios dependen del `pom.xml` padre.

```bash
docker build -t incentivos-img -f incentivos-service/Dockerfile .
docker build -t donaciones-img -f donaciones-service/Dockerfile .
```

### Levantar todo junto

```bash
docker compose up
```

Para levantar un servicio con su base de datos aislada (MySQL en el puerto 3307):

```bash
cd incentivos-service
docker compose -f compose.dev.yml up --build
```

---

## Requisito pendiente: visibilidad configurable de insignias

> Estado: **no implementado** en `incentivos-service`.

El enunciado pide que las insignias obtenidas puedan visualizarse en el perfil de la
persona donante *"siempre que la persona usuaria las configure como visibles"*. O sea,
la visibilidad tiene que ser **una decisión de la persona donante**, no un dato que el
servicio asuma.

Hoy no hay forma de configurarla:

- `InsigniaObtenida` (`models/entities/Perfil/InsigniaObtenida.java`) solo tiene
  `perfil`, `insignia` y `fechaObtencion`. **No existe el campo de visibilidad.**
- En consecuencia, `GET /api/perfiles/{idUsuario}/insignias` devuelve **todas** las
  insignias otorgadas, sin filtro.
- No hay endpoint para cambiar ese estado.

### Qué hay que hacer

1. Agregar `Boolean visible` a `InsigniaObtenida`, con `true` por defecto en el
   constructor para no cambiar el comportamiento de las insignias ya emitidas.
2. Exponer un endpoint de toggle, por ejemplo
   `PUT /api/perfiles/{idUsuario}/insignias/{idInsignia}/visibilidad`.
3. Filtrar por `visible` en el listado de insignias y en el DTO que se expone
   públicamente, dejando las insignias ocultas fuera de la respuesta pero **sin** borrar
   el registro (sigue contando para el ranking y para el historial).
4. Solo la persona dueña del perfil debería poder cambiar la visibilidad.

---

## Documentación por servicio

Cada microservicio tiene su Swagger en `/api-docs`.

Los cuatro mantienen un `PENDIENTES.md` propio con los problemas técnicos conocidos que
quedaron abiertos. Los IDs nunca se renumeran, así que hay huecos: cada punto dice su
severidad, los archivos afectados y una propuesta concreta. Lo que ya se corrigió queda en la
sección `# Corregidos` de cada archivo, con el motivo por el que se cambió.

Los tres comandos de arranque y el compose están en [docker-compose.yml](docker-compose.yml),
que incluye MySQL y RabbitMQ: la integración entre servicios va por el broker, no por HTTP.