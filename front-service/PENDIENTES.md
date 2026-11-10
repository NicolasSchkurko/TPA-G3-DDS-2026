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
- Cabecera unificada con tabs multi-rol (pestaña por cada `ROLE_*` que tenga la cuenta), badge de avisos y menú de usuario.
- Portal de entidad (`/entidad`): dashboard map-first con panel desplegable, KPIs, vista rápida de necesidades y entregas activas; vistas de necesidades, donaciones, detalle con confirmación de recepción con foto (base64), seguimiento en vivo y bandeja de notificaciones derivadas.
- Puente temporal de identidad (`username → perfilId`) en `front-service/data/identidad-usuarios.json`, poblado al registrar y usado como respaldo; la identidad principal se resuelve por el email de la cuenta (`GET /api/identidades/por-email`).
- Email requerido en el registro web; alta de la entidad con `Mail` en su persona jurídica; roles de donante/entidad/admin vinculados automáticamente al registrar y al iniciar sesión (unión de roles de dominio + perfiles de Keycloak con el mismo email).
- Panel del dashboard de entidad movido a la izquierda; al contraerlo queda un resumen (KPIs compactos) con botón "Ver detalle"; el estado se persiste en `localStorage`.
- Polling cada 12 s del seguimiento (pausado con la pestaña oculta).

## Portal de entidad — contratos de backend faltantes

Para que el portal funcione sin adaptadores temporales se necesita, en orden de prioridad:

| # | Necesidad | Contrato esperado (o cambio menor) | Estado actual | Workaround en el front |
|---|---|---|---|---|
| 1 | Resolver "mi entidad" desde la sesión | `GET /api/identidades/me` → `{ donanteId, entidadId, adminId, roles[] }`; o campo `username`/`keycloakId`/`email` en `EntidadBeneficiariaDTO` y `PersonaDonanteDTO`, más query por valor | No existe. `EntidadBeneficiaria` no guarda vínculo con Keycloak | Mapping local `username → perfilId` (`./data/identidad-usuarios.json`); si falta, selector manual (`/entidad/vincular`) con banner demo |
| 2 | Ubicación del camión | `latitud`, `longitud`, `ultimaActualizacion`, `eta` en `CamionDTO` o `RutaDTO`; idealmente `GET /api/camiones/{patente}/ubicacion` | No existe. Solo `ciudadDestinoActual` y `urlSeguimiento` | Posiciones simuladas de forma determinista entre el depósito y la zona de destino; badge "Ubicación simulada" |
| 3 | Coordenadas de direcciones | `latitud`/`longitud` en `DireccionDTO` (donaciones y logística) | No existe | Geocoding demo de barrios de CABA en `SeguimientoService` |
| 4 | Filtrar entregas/rutas por entidad | `GET /api/entregas?idEntidad={id}` y `GET /api/rutas?idEntidad={id}&estado=EN_CURSO` | No existe. Endpoints globales | Filtro en front por `bien.entidadDestino.idEntidad` y `parada.direccion.idEntidad` |
| 5 | Entregas de mi entidad | `GET /api/entidades/{id}/entregas` (o `BienDTO.idDonacion` explícito) | `GET /api/entregas` devuelve `{ idsDonaciones[], bienes[] }` alineados por índice; `BienDTO` no trae el id de donación | Emparejamiento por índice cliente; pendiente de confirmar con el servicio |
| 6 | Catálogo de necesidades | `GET /api/categorias`, `GET /api/categorias/{id}/subcategorias`, `POST /api/categorias`, `POST /api/subcategorias` | No existe. `CategoriaBienJpaRepository.findByNombre` y `SubcategoriaBienJpaRepository.findByNombreAndCategoriaNombre` existen pero sin endpoint | Catálogo curado en el front (datalist) + texto libre; `obtenerOCrearSubcategoria` crea en el alta |
| 7 | Notificaciones de la entidad | `GET /api/notificaciones?destinatario={id}` con `leida`, `tipo`, `referenciaId`; `PATCH /api/notificaciones/{id}/leida` | Solo `POST /api/notificaciones` y `GET /api/notificaciones/{id}`; sin listado ni marcado | Bandeja derivada de donaciones/entregas; leído/no leído en sesión HTTP (no persiste) |
| 8 | Fotos de recepción | Confirmar que `fotoUrl` acepta data URL base64 y su tamaño máximo en `PATCH /api/entregas/{idDonacion}/estado`; idealmente `fotos[]` y endpoint de subida | Un único `fotoComprobante` String; el front comprime a JPEG y manda data URL | Una foto por recepción; sin límite validado del lado del servidor (front limita a ~5 MB) |
| 9 | Email como llave de identidad (visión completa multi-rol) | `email` en `Keycloak` (verificado y único) + queries por mail en `DonanteJpaRepository` y `EntidadBeneficiariaJpaRepository` | Implementado parcialmente (registro + resolución por email). Falta exigir verificación de email antes de conceder roles y usar el `keycloakId` en el perfil de dominio | Email requerido en el registro web; `GET /api/identidades/por-email` en Donaciones; Keycloak sincronizado al registrar y authorities enriquecidas en el login; `IdentidadService` resuelve por email con fallback al mapping local |
| 10 | Progreso de necesidades | `NecesidadDTO` ya expone `donaciones[]`; falta el total acumulado o exposición consistente de `cantidadTotalBienes` | Parcial | Cálculo en el front con `DonacionDTO.cantidadTotalBienes` |

### Notas

- Conceder roles por coincidencia de email es un modelo de confianza para la demo: en producción hay que exigir verificación de email antes de habilitar DONANTE/ADMIN y no sincronizar ADMIN automáticamente.
- `PATCH /api/entregas/{idDonacion}/estado` acepta `{ estado, fotoUrl, justificacion }`; `estado` esperado: `ENTREGADA` (requiere foto) o `NO_RECIBIDA` (requiere justificación). El front envía `ENTREGADA` con `fotoUrl` base64.
- El item de entrega usa el **id de donación** como clave (`ItemEntrega.idDonacion`), por lo que `GET /api/entregas/{id}` y `PATCH .../estado` reciben el id de la donación.
- `GET /api/rutas` devuelve `RutasDTO { rutas[] }` con `RutaDTO { camionAsignado, estado, urlSeguimiento, paradas[] }`; el front filtra por `paradas[].direccion.idEntidad` y descarta `FINALIZADA`.
- Nada de esto requiere cambios de modelo fuera de lo indicado; el front queda listo para cambiar los adaptadores (`SeguimientoService`, `IdentidadService`, `NotificacionesDerivadasService`) sin tocar las vistas.
