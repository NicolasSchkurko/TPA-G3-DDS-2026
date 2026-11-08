# Pendientes del proyecto (revisión hasta Entrega 6)

Revisión del enunciado `DDS-TP-Anual-2026.pdf` y del estado visible del repositorio. El detalle de
riesgos técnicos por módulo se conserva en los `PENDIENTES.md` de cada servicio.

El repo contiene cuatro servicios backend y `front-service` (MVC). Los siguientes pendientes son
transversales o corresponden a entregables del TP; no reemplazan la priorización técnica de cada
módulo.

| Prioridad | Pendiente | Evidencia / alcance |
|---|---|---|
| Alta | Completar y versionar la documentación de arquitectura y diseño | No se encontraron diagramas versionados de casos de uso, clases por servicio, componentes/despliegue ni modelos físicos/ERD, ni justificaciones de diseño consolidadas. El enunciado los solicita desde E1 y actualizaciones en las entregas siguientes. |
| Alta | Cerrar autenticación y autorización de extremo a extremo | E6 pide servicio de autenticación y propagación de tokens desde el front a los servicios. `front-service` usa usuarios en memoria y sesión de Spring Security; los clientes REST no reenvían credenciales/tokens. Los servicios además tienen políticas diferentes. |
| Alta | Completar cobertura funcional de la interfaz MVC | El front ofrece landing, login, panel y vistas de donaciones/donantes, pero no se identifican vistas y flujos completos para las wireframes E1 ni para perfiles/misiones, entidades/necesidades, administración, logística y seguimiento. |
| Media | Ajustar la importación CSV al requisito de actualización por email | E1 pide actualizar donantes existentes por email y crear los nuevos. El backlog de Donaciones documenta que hoy se tratan como distintos; falta resolver la identificación única y el comportamiento de actualización. |
| Media | Publicar y mantener documentación de APIs | E3 solicita documentación de APIs con Postman y Swagger. Hay colecciones Postman para los cuatro servicios, pero no se encontró dependencia/configuración OpenAPI/Swagger versionada. Revisar que las colecciones cubran todos los endpoints y los flujos de creación, lectura, actualización y borrado con datos propios. |
| Media | Revisar integración resiliente entre servicios | Hay llamadas HTTP y mensajería RabbitMQ. Acordar contratos, reintentos/reconciliación y consistencia ante fallas entre donaciones, incentivos y notificaciones; los riesgos particulares están detallados en los pendientes de esos módulos. |
| Media | Cerrar pendientes de entrega y demostrar despliegue reproducible | Verificar desde un clon limpio el compose completo, configuración por entorno, persistencia de volúmenes y pasos de arranque documentados. No se registran aquí fallas antiguas de compose/scripts: el compose actual usa la raíz como contexto y los scripts mencionados en versiones anteriores no están en el árbol. |

## Cobertura por entrega

- **E1:** Donaciones, segmentación, necesidades, importación CSV y estructura modular están implementadas en parte; faltan los entregables formales de diseño y una UI que cubra las wireframes y perfiles de usuario.
- **E2:** Hay trazabilidad, asignación, incentivos, misiones/insignias, rankings y publicación a n8n. Ver los pendientes de `donaciones-service`, `incentivos-service` y `notificaciones-service` para brechas de comportamiento, robustez y canales. El backlog de Notificaciones registra que no está contemplado el canal SMS; cubrirlo o dejar documentada la decisión de alcance.
- **E3:** Existe `logisticas-service`, planificación, seguimiento y colecciones Postman. Restan riesgos de producción indicados en su backlog; Swagger/OpenAPI no aparece configurado.
- **E4:** Se encuentra RabbitMQ en compose e integración asíncrona de avisos. Falta documentar el rol de intermediación entre Donaciones y proveedores/servicios de logística y verificar topología y fallas con la arquitectura final.
- **E5:** Los servicios persisten por separado; logística usa MongoDB y los demás servicios usan persistencia relacional. Falta entregar diagramas actualizados de entidades/modelos físicos y justificar decisiones en documentación versionada.
- **E6:** Existe un front MVC con Thymeleaf y Spring Security, pero autenticación local en memoria, sin propagación de token a los backends y con cobertura parcial de pantallas/roles.

## Corregidos / descartados de la lista anterior

- El supuesto de que Docker Compose construía con contexto incorrecto no aplica al `docker-compose.yml` actual: sus builds usan `context: .` y Dockerfile por módulo.
- Los scripts `test-conexiones.ps1` y `e2e-completo.ps1` citados por el backlog anterior no están en el repositorio actual; tampoco están los scripts SQL mencionados. Se quitaron como pendientes activos hasta que vuelvan a incorporarse.