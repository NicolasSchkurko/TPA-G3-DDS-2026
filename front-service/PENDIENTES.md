# Pendientes técnicos de front-service

Revisión del alcance de Entrega 6 del enunciado. El módulo ya es una aplicación MVC con Thymeleaf,
Spring Security y clientes REST para Donaciones e Incentivos; tiene landing, login, panel y vistas de
consulta de donaciones/donantes. Los siguientes puntos describen brechas visibles en el código actual.

| Prioridad | Pendiente | Evidencia |
|---|---|---|
| Alta | Completar autorización de extremo a extremo | Keycloak autentica el front y protege sus rutas por rol; faltan validar JWT en las APIs y asociar cada cuenta con su actor persistido. |
| Alta | Propagar token/identidad a los servicios | `DonacionesClient` e `IncentivosClient` hacen REST sin reenviar token; no existe manejo de token en la sesión ni cliente para logística/notificaciones. E6 lo requiere. |
| Alta | Completar flujos y wireframes de E1 | Solo hay vistas de landing, panel, login y consultas de donaciones/donantes. Faltan los recorridos completos según perfil: donante, entidad beneficiaria y administración, incluyendo carga/gestión donde corresponda. |
| Media | Incorporar vistas de incentivos y logística | No hay controladores/templates para perfil, misiones, insignias, métricas, administración de entidades/necesidades, rutas y seguimiento. El `IncentivosClient` solo expone perfil público y no se usa actualmente desde un controlador. |
| Media | Manejar fallas de backend sin mostrar datos engañosos | `DonacionesClient` captura excepciones y devuelve lista vacía/null; el panel puede presentar un backend caído como cero registros. Mostrar estado/error y distinguirlo de una respuesta vacía. |
| Media | Completar pruebas MVC y de integración con APIs | El módulo solo contiene `FrontApplicationTest`; agregar cobertura de autorización por rol, renderizado y manejo de errores/contratos. |
| Baja | Preparar configuración segura por entorno | Las credenciales y URLs de desarrollo están documentadas; reemplazar secretos y URLs locales para cualquier entorno compartido. |

## Corregidos

- Login OIDC con Keycloak y roles `ADMIN`, `DONANTE`, `CHOFER` y `ENTIDAD`.
- Redirección a la pantalla inicial por rol y restricciones de rutas en Spring Security.
- Realm de desarrollo importable desde Docker Compose con usuarios locales de prueba.
