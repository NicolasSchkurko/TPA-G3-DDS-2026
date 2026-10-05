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

## Documentación por servicio

Cada microservicio tiene su Swagger en `/api-docs`. Además, `incentivos-service` y el
resto de módulos mantienen un `PENDIENTES.md` con los problemas técnicos conocidos que
quedaron abiertos.