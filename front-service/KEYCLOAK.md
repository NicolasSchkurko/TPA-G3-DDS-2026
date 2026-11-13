# Keycloak en desarrollo

El Compose raíz levanta Keycloak en `http://localhost:8180` y crea el realm `ddsi` a partir de `docker/keycloak/realm-ddsi.json`. El realm tiene roles `ADMIN`, `DONANTE`, `CHOFER` y `ENTIDAD`; esos roles se incluyen en el ID token para que Spring Security los convierta en autoridades `ROLE_*`.

## Inicio

Desde la raíz del repositorio:

```powershell
docker compose up -d --build
```

Abrir `http://localhost:8080` y seleccionar **Ingresar**. La autenticación continúa en Keycloak. La consola de administración está en `http://localhost:8180/admin`.

## Cuentas locales de demostración

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin123` | `ADMIN` |
| `donante` | `donante123` | `DONANTE` |
| `chofer` | `chofer123` | `CHOFER` |
| `entidad` | `entidad123` | `ENTIDAD` |

La cuenta de consola inicial es `admin` / `admin-local-only`. Son credenciales locales de demostración; cambiarlas antes de cualquier entorno compartido. El realm y sus cuentas persisten en el volumen `keycloak-data`; el archivo de importación se aplica cuando ese realm todavía no existe.

## Alcance actual

- El front inicia y cierra sesión con OpenID Connect usando Keycloak.
- Spring Security convierte roles del realm en `ROLE_ADMIN`, `ROLE_DONANTE`, `ROLE_CHOFER` y `ROLE_ENTIDAD`, redirige al inicio del perfil y protege las rutas del front.
- Las pantallas de donante, chofer y entidad son el punto de entrada visual. Las cuentas de demostración todavía no están asociadas a registros concretos de las bases de Donaciones o Logística.
- El front es el único que valida roles con Keycloak. Incentivos, Donaciones, Logística y Notificaciones no dependen de Keycloak: reciben llamadas del front o de otros servicios y las limitan con ACL de red (`IPS_INTERNAS` + `SERVICIOS_INTERNOS`).

## Registro web

El formulario crea la cuenta y asigna el rol en Keycloak, y luego crea el perfil en Donaciones. El cliente confidencial `front-registration` se usa solo desde `front-service`; su cuenta de servicio necesita los roles `manage-users` y `view-realm` del cliente `realm-management`.

Keycloak 26.8 requiere además incluir la audiencia del Admin API en el token. Por eso `front-registration` tiene el client scope `kc-admin-api` como scope predeterminado. La configuración inicial está en `docker/keycloak/realm-ddsi.json`.

> Keycloak importa el archivo solo cuando todavía no existe el realm `ddsi`. En un volumen existente, los cambios del JSON no se aplican al reiniciar. En ese caso, configurar `front-registration` en la consola del realm: habilitar autenticación de cliente y Service Accounts, asignar `manage-users` y `view-realm` desde Service account roles → `realm-management`, y agregar `kc-admin-api` como client scope predeterminado. `front-service` necesita `roles` entre sus client scopes predeterminados para que el ID token traiga los roles que usa el propio front.

El formulario público permite registrarse como donante o entidad beneficiaria. La opción de administrador solo aparece para una sesión con `ROLE_ADMIN`; el front es quien exige ese rol y usa el `sub` de Keycloak como `id` del `Administrador`. El primer administrador es la cuenta `admin` del realm; a partir de ahí, un administrador autenticado puede crear otros desde el formulario o desde `POST /api/admins` enviando el `sub` de Keycloak en `id`. Las cuentas de chofer deben ser creadas por una persona administradora.

## Theme DonaTrack

El theme propio está en `docker/keycloak/themes/donatrack/` y se monta read-only en el contenedor:

- `login/`: login, registro, recuperación, OTP y errores.
- `account/`: account console.
- `admin/`: branding del admin console.
- `email/`: wrapper HTML de emails transaccionales.

Keycloak 26 lo carga por los defaults `KC_SPI_THEME_DEFAULT_LOGIN`, `KC_SPI_THEME_DEFAULT_ACCOUNT`, `KC_SPI_THEME_DEFAULT_EMAIL` y `KC_SPI_THEME_DEFAULT_ADMIN` definidos en `docker-compose.yml`.

En un volumen `keycloak-data` ya existente, el `loginTheme`/`displayName` del JSON no se reimporta. El tema por defecto se aplica al reiniciar Keycloak; si el realm tiene un theme explícito, hay que cambiarlo desde la consola de administración. No hace falta borrar el volumen.
