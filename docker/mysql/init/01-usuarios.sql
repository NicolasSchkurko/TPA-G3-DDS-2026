-- Usuario dedicado para las aplicaciones. Solo tiene privilegios sobre las bases de la app.
-- NO usar root para las conexiones de los servicios.
-- En producción, además, se usarían migraciones (Flyway/Liquibase) en vez de ddl-auto=update.
CREATE DATABASE IF NOT EXISTS donaciones_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS incentivos_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS notificaciones CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'ddsi_app'@'%' IDENTIFIED BY 'ddsi-app-local-only';
-- Se incluyen privilegios de DDL porque los servicios usan ddl-auto=update en local.
GRANT ALL PRIVILEGES ON donaciones_db.* TO 'ddsi_app'@'%';
GRANT ALL PRIVILEGES ON incentivos_db.* TO 'ddsi_app'@'%';
GRANT ALL PRIVILEGES ON notificaciones.* TO 'ddsi_app'@'%';
FLUSH PRIVILEGES;
