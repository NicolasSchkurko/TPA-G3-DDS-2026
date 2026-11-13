-- ============================================================================
-- Consistencia API vs base de datos (donaciones_db)
--
-- Companion de "DonaTrack - Consistencia API vs BD.postman_collection.json".
--
-- Como correrlo (desde la raiz del repo, con el stack levantado).
--
-- bash / cmd:
--
--   docker compose exec -T mysql mysql -uroot -proot --table < postman/consistencia-api-vs-bd.sql
--
-- PowerShell (NO soporta el redirect '<', tira "El operador '<' esta reservado
-- para uso futuro", hay que pipear):
--
--   Get-Content postman/consistencia-api-vs-bd.sql -Raw | docker compose exec -T mysql mysql -uroot -proot --table
--
-- El bloque 0 devuelve los tres numeros que hay que copiar a las variables de
-- coleccion `sqlDonantesActivos`, `sqlEntidadesActivas` y `sqlDonaciones`.
-- Los bloques siguientes son para entender cualquier diferencia a mano.
--
-- OJO con el soft delete: la API expone SOLO las filas activas.
--   * donante.activo = 1              -> aparece en GET /api/personas
--   * entidad_beneficiaria.activa = 1 -> aparece en GET /api/entidades
--   * donacion NO tiene soft delete   -> el total de la tabla es el total de la API
-- Por eso COUNT(*) crudo sobre donante/entidad_beneficiaria va a dar MAS que la
-- API y eso es correcto, no es una inconsistencia.
-- ============================================================================

USE donaciones_db;

-- ----------------------------------------------------------------------------
-- 0) Los tres numeros para la coleccion de Postman
-- ----------------------------------------------------------------------------
SELECT 'sqlDonantesActivos'   AS variable_de_postman,
       (SELECT COUNT(*) FROM donante WHERE activo = 1)              AS valor
UNION ALL
SELECT 'sqlEntidadesActivas',
       (SELECT COUNT(*) FROM entidad_beneficiaria WHERE activa = 1)
UNION ALL
SELECT 'sqlDonaciones',
       (SELECT COUNT(*) FROM donacion);

-- ----------------------------------------------------------------------------
-- 1) Donantes: activos vs dados de baja
--    Si la API trae menos que el total, la diferencia tiene que coincidir
--    exactamente con la fila "dados de baja (soft delete)".
-- ----------------------------------------------------------------------------
SELECT 'total en la tabla'            AS detalle, COUNT(*) AS cantidad FROM donante
UNION ALL
SELECT 'activos (los que ve la API)', COUNT(*) FROM donante WHERE activo = 1
UNION ALL
SELECT 'dados de baja (soft delete)', COUNT(*) FROM donante WHERE activo = 0
UNION ALL
SELECT 'de baja sin fecha_baja (!)',  COUNT(*) FROM donante
       WHERE activo = 0 AND fecha_baja IS NULL
UNION ALL
SELECT 'activos con fecha_baja (!)',  COUNT(*) FROM donante
       WHERE activo = 1 AND fecha_baja IS NOT NULL;

-- Desglose por tipo de persona. `tipoPersona` en el DTO se deriva de cual de las
-- dos tablas hijas (humana / juridica) tiene la fila, porque Persona usa herencia
-- JOINED.
SELECT CASE
         WHEN h.id IS NOT NULL THEN 'HUMANA'
         WHEN j.id IS NOT NULL THEN 'JURIDICA'
         ELSE 'SIN SUBTIPO (!)'
       END                                        AS tipo_persona,
       COUNT(*)                                   AS activos
FROM donante d
LEFT JOIN humana   h ON h.id = d.persona_id
LEFT JOIN juridica j ON j.id = d.persona_id
WHERE d.activo = 1
GROUP BY tipo_persona;

-- Nombres de los donantes activos, armados igual que `DonacionDTO.donanteName`
-- (razon social para juridicas, "nombre apellido" para humanas). Sirve para
-- cotejar contra la variable `nombresDonantes` que guarda la coleccion.
SELECT LOWER(HEX(d.id))                           AS donante_id,
       COALESCE(j.razon_social,
                TRIM(CONCAT(COALESCE(h.nombre, ''), ' ',
                            COALESCE(h.apellido, '')))) AS donante_name,
       CASE WHEN j.id IS NOT NULL THEN 'JURIDICA' ELSE 'HUMANA' END AS tipo_persona
FROM donante d
LEFT JOIN humana   h ON h.id = d.persona_id
LEFT JOIN juridica j ON j.id = d.persona_id
WHERE d.activo = 1
ORDER BY donante_name;

-- ----------------------------------------------------------------------------
-- 2) Entidades beneficiarias: activas vs dadas de baja
-- ----------------------------------------------------------------------------
SELECT 'total en la tabla'            AS detalle, COUNT(*) AS cantidad
       FROM entidad_beneficiaria
UNION ALL
SELECT 'activas (las que ve la API)', COUNT(*) FROM entidad_beneficiaria
       WHERE activa = 1
UNION ALL
SELECT 'dadas de baja (soft delete)', COUNT(*) FROM entidad_beneficiaria
       WHERE activa = 0
UNION ALL
SELECT 'de baja sin fecha_baja (!)',  COUNT(*) FROM entidad_beneficiaria
       WHERE activa = 0 AND fecha_baja IS NULL;

-- Razones sociales y CUIT de las entidades activas. Cotejar contra
-- `razonesSocialesEntidades` y contra el test de unicidad de CUIT.
SELECT LOWER(HEX(e.id))  AS entidad_id,
       j.razon_social,
       j.cuit,
       j.tipo_juridico
FROM entidad_beneficiaria e
LEFT JOIN juridica j ON j.id = e.juridica_id
WHERE e.activa = 1
ORDER BY j.razon_social;

-- CUIT repetidos entre entidades activas (la API testea unicidad).
SELECT j.cuit, COUNT(*) AS veces
FROM entidad_beneficiaria e
JOIN juridica j ON j.id = e.juridica_id
WHERE e.activa = 1
GROUP BY j.cuit
HAVING COUNT(*) > 1;

-- Necesidades por entidad activa. La API expone pedido/recibido por necesidad;
-- aca se ve cuantas necesidades activas tiene cada una.
SELECT j.razon_social,
       COUNT(n.id)                                   AS necesidades,
       SUM(CASE WHEN n.activa = 1 THEN 1 ELSE 0 END) AS activas,
       SUM(n.cantidad_objetivo)                      AS objetivo_total
FROM entidad_beneficiaria e
JOIN juridica j  ON j.id = e.juridica_id
LEFT JOIN necesidad n ON n.entidad_beneficiaria_id = e.id
WHERE e.activa = 1
GROUP BY e.id, j.razon_social
ORDER BY j.razon_social;

-- ----------------------------------------------------------------------------
-- 3) Donaciones
--    No hay soft delete: COUNT(*) tiene que dar igual que totalElements de
--    GET /api/donaciones.
-- ----------------------------------------------------------------------------
SELECT 'total en la tabla' AS detalle, COUNT(*) AS cantidad FROM donacion;

-- Desglose por estado. La coleccion loguea el mismo desglose desde la API:
-- tienen que coincidir estado por estado, no solo en el total.
SELECT COALESCE(estado, 'NULL (!)') AS estado, COUNT(*) AS cantidad
FROM donacion
GROUP BY estado
ORDER BY cantidad DESC, estado;

-- Estados que no pertenecen al enum Java `Estado`. Deberia volver vacio.
-- `donacion.estado` es varchar(32), asi que la base no valida nada por si sola.
SELECT COALESCE(estado, 'NULL') AS estado_invalido, COUNT(*) AS cantidad
FROM donacion
WHERE estado IS NULL
   OR estado NOT IN ('EN_DEPOSITO', 'PENDIENTE_ASIGNACION', 'ASIGNADO',
                     'LISTA_PARA_ENTREGAR', 'EN_TRASLADO', 'ENTREGADO',
                     'ENTREGA_FALLIDA', 'VENCIDO')
GROUP BY estado;

-- Coherencia estado <-> entidad asignada. Las dos filas tienen que dar 0.
SELECT 'asignada pero sin entidad_id (!)' AS detalle, COUNT(*) AS cantidad
FROM donacion
WHERE estado IN ('ASIGNADO', 'LISTA_PARA_ENTREGAR', 'EN_TRASLADO', 'ENTREGADO')
  AND entidad_id IS NULL
UNION ALL
SELECT 'EN_DEPOSITO con entidad_id (!)', COUNT(*)
FROM donacion
WHERE estado = 'EN_DEPOSITO'
  AND entidad_id IS NOT NULL;

-- Bienes por donacion. `cantidadTotalBienes` del DTO se compara contra la suma
-- de `peso` (la columna que guarda la cantidad del bien).
SELECT LOWER(HEX(dn.id))          AS donacion_id,
       dn.estado,
       dn.descripcion,
       COUNT(b.id)                AS bienes,
       COALESCE(SUM(b.peso), 0)   AS suma_cantidades
FROM donacion dn
LEFT JOIN bien b ON b.donacion_id = dn.id
GROUP BY dn.id, dn.estado, dn.descripcion
ORDER BY dn.estado, dn.descripcion;

-- Donaciones sin ningun bien. La coleccion exige >= 1 bien por donacion.
SELECT LOWER(HEX(dn.id)) AS donacion_sin_bienes, dn.estado, dn.descripcion
FROM donacion dn
LEFT JOIN bien b ON b.donacion_id = dn.id
WHERE b.id IS NULL;

-- Vista plana donacion -> donante -> entidad, con los nombres tal como los
-- devuelve la API. Es la comparacion mas directa contra el listado de
-- GET /api/donaciones (`donanteName` y `entidadBeneficiaria`).
SELECT LOWER(HEX(dn.id))                          AS donacion_id,
       dn.estado,
       COALESCE(jd.razon_social,
                TRIM(CONCAT(COALESCE(hd.nombre, ''), ' ',
                            COALESCE(hd.apellido, '')))) AS donante_name,
       COALESCE(je.razon_social, 'No asignada')   AS entidad_beneficiaria,
       sc.nombre                                  AS subcategoria_name
FROM donacion dn
LEFT JOIN donante d  ON d.id = dn.donante_id
LEFT JOIN humana   hd ON hd.id = d.persona_id
LEFT JOIN juridica jd ON jd.id = d.persona_id
LEFT JOIN entidad_beneficiaria e ON e.id = dn.entidad_id
LEFT JOIN juridica je ON je.id = e.juridica_id
LEFT JOIN subcategoria_bien sc ON sc.id = dn.subcategoria_id
ORDER BY dn.estado, donante_name;

-- ----------------------------------------------------------------------------
-- 4) Integridad referencial: FKs que apuntan a filas que no existen
--    o a filas dadas de baja. Todo esto deberia volver vacio.
-- ----------------------------------------------------------------------------
SELECT 'donacion -> donante inexistente' AS problema, COUNT(*) AS cantidad
FROM donacion dn
LEFT JOIN donante d ON d.id = dn.donante_id
WHERE dn.donante_id IS NOT NULL AND d.id IS NULL
UNION ALL
SELECT 'donacion -> donante dado de baja', COUNT(*)
FROM donacion dn
JOIN donante d ON d.id = dn.donante_id
WHERE d.activo = 0
UNION ALL
SELECT 'donacion -> entidad inexistente', COUNT(*)
FROM donacion dn
LEFT JOIN entidad_beneficiaria e ON e.id = dn.entidad_id
WHERE dn.entidad_id IS NOT NULL AND e.id IS NULL
UNION ALL
SELECT 'donacion -> entidad dada de baja', COUNT(*)
FROM donacion dn
JOIN entidad_beneficiaria e ON e.id = dn.entidad_id
WHERE e.activa = 0
UNION ALL
SELECT 'donacion -> necesidad inexistente', COUNT(*)
FROM donacion dn
LEFT JOIN necesidad n ON n.id = dn.necesidad_id
WHERE dn.necesidad_id IS NOT NULL AND n.id IS NULL
UNION ALL
SELECT 'bien -> donacion inexistente', COUNT(*)
FROM bien b
LEFT JOIN donacion dn ON dn.id = b.donacion_id
WHERE b.donacion_id IS NOT NULL AND dn.id IS NULL
UNION ALL
SELECT 'bien sin subcategoria', COUNT(*)
FROM bien b
WHERE b.subcategoria_id IS NULL
UNION ALL
SELECT 'donante -> persona inexistente', COUNT(*)
FROM donante d
LEFT JOIN persona p ON p.id = d.persona_id
WHERE d.persona_id IS NOT NULL AND p.id IS NULL
UNION ALL
SELECT 'entidad -> juridica inexistente', COUNT(*)
FROM entidad_beneficiaria e
LEFT JOIN juridica j ON j.id = e.juridica_id
WHERE e.juridica_id IS NOT NULL AND j.id IS NULL;

-- ----------------------------------------------------------------------------
-- 5) Coherencia del subtipo de bien
--    Bien usa herencia JOINED: cada fila de `bien` tiene que tener exactamente
--    una fila hija, en `bien_con_estado` O en `bien_perecedero`, nunca en las
--    dos ni en ninguna.
-- ----------------------------------------------------------------------------
SELECT CASE
         WHEN bce.id IS NOT NULL AND bp.id IS NOT NULL THEN 'EN LAS DOS TABLAS (!)'
         WHEN bce.id IS NOT NULL                       THEN 'CON_ESTADO'
         WHEN bp.id  IS NOT NULL                       THEN 'PERECEDERO'
         ELSE 'SIN SUBTIPO (!)'
       END          AS subtipo,
       COUNT(*)     AS cantidad
FROM bien b
LEFT JOIN bien_con_estado bce ON bce.id = b.id
LEFT JOIN bien_perecedero bp  ON bp.id = b.id
GROUP BY subtipo;

-- ----------------------------------------------------------------------------
-- 6) Historial de estados
--    Cada donacion que ya salio de EN_DEPOSITO deberia tener al menos una
--    entrada de historial que lo justifique.
-- ----------------------------------------------------------------------------
SELECT LOWER(HEX(dn.id))      AS donacion_id,
       dn.estado              AS estado_actual,
       COUNT(h.donacion_id)   AS entradas_de_historial
FROM donacion dn
LEFT JOIN donacion_historial_estados h ON h.donacion_id = dn.id
GROUP BY dn.id, dn.estado
ORDER BY entradas_de_historial, dn.estado;
