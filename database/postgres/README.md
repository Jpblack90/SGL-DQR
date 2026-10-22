# Base de Datos PostgreSQL — SGL-DQR (Expreso Marvisur)

Este módulo gestiona el esquema relacional y las migraciones versionadas con Flyway para PostgreSQL 16, implementadas en estricta conformidad con el documento [`docs/Diccionario_Datos_SGL_DQR.md`](../../docs/Diccionario_Datos_SGL_DQR.md).

---

## 1. Migraciones Flyway y Orden de Dependencias FK

Las migraciones se ordenan según sus dependencias de claves foráneas (FK), resolviendo dependencias circulares mediante `ALTER TABLE`:

| Archivo | Módulo Diccionario | Tablas creadas / Modificaciones | Dependencias FK resueltas |
|---|---|---|---|
| `V1__modulo2_sucursales_flotas_rutas.sql` | Módulo 2 | `sucursales`, `vehiculos`, `rutas`, `programacion_viajes` | Base independiente. `rutas` y `programacion_viajes` referencian `sucursales` y `vehiculos`. |
| `V2__modulo1_seguridad_personal_auditoria.sql` | Módulo 1 | `roles`, `empleados`, `auditoria_logs` + `ALTER TABLE programacion_viajes` | `empleados` referencia `roles` y `sucursales`. Resuelve circularidad añadiendo FK `programacion_viajes.id_chofer -> empleados.id_empleado`. |
| `V3__modulo3_clientes_preregistros.sql` | Módulo 3 | `clientes`, `pre_registros_express` | `pre_registros_express` referencia `clientes` (remitente/destinatario) y `sucursales`. |
| `V4__modulo4_encomiendas_pagos_almacen.sql` | Módulo 4 | `encomiendas`, `despachos_lote_detalles`, `incidencias_emergencia` | `encomiendas` referencia `clientes` y `sucursales`. `despachos_lote_detalles` referencia `programacion_viajes` y `encomiendas`. `incidencias_emergencia` referencia `encomiendas` y `empleados`. |
| `V5__modulo5_doble_qr_tokens.sql` | Módulo 5 | `parejas_doble_qr`, `tokens_qr2_estatus`, `validaciones_cruzadas_logs` | `parejas_doble_qr` referencia `encomiendas`. `tokens_qr2_estatus` referencia `parejas_doble_qr`. `validaciones_cruzadas_logs` referencia `parejas_doble_qr` y `empleados`. |
| `V6__modulo6_tracking_comprobantes.sql` | Módulo 6 | `historial_tracking`, `comprobantes_pago`, `archivos_pdf_comprobante` | `historial_tracking` referencia `encomiendas` y `sucursales`. `comprobantes_pago` referencia `encomiendas`. `archivos_pdf_comprobante` referencia `comprobantes_pago`. |
| `V7__datos_iniciales.sql` | Semilla Inicial | Datos maestros iniciales | Inserta los 4 roles del sistema, 1 sucursal de ejemplo y 1 empleado administrador inicial (TN3). |

---

## 2. Credenciales Iniciales del Administrador (TN3)

> **ADVERTENCIA DE SEGURIDAD**: Estas credenciales son de uso exclusivo para desarrollo y pruebas iniciales. Deben cambiarse obligatoriamente en el primer despliegue a producción.

- **Rol**: `ROL-TN3-ADMIN` (`Administrador`)
- **DNI**: `00000001`
- **Correo**: `admin@marvisur.pe`
- **Contraseña temporal en claro**: `AdminMarvisur2026!`
- **Hash BCrypt (rounds = 12)**:
  ```
  $2b$12$goz1tdfrnGCvrV/InZEN3OZS.xvzWnlozTQf2LUs8LoZx9uvYjzEW
  ```
- **Verificación**: Generado con librería estándar `bcrypt` (algoritmo Blowfish / 2b).

---

## 3. Comandos de Verificación con Docker Compose

### 3.1. Iniciar PostgreSQL
Desde la raíz del repositorio (`Sistema/`):
```bash
docker compose up -d postgres
```

### 3.2. Ejecutar la aplicación (aplica migraciones automáticamente vía Flyway)
```bash
cd backend-marvisur
mvn spring-boot:run -pl marvisur-api
```

### 3.3. Verificar tablas aplicadas en PostgreSQL mediante `psql`
```bash
docker exec -it marvisur_postgres psql -U marvisur -d marvisur_db -c "\dt"
```

### 3.4. Consultar el historial de versiones aplicado por Flyway
```bash
docker exec -it marvisur_postgres psql -U marvisur -d marvisur_db -c "SELECT version, description, type, script, success FROM flyway_schema_history ORDER BY installed_rank;"
```

---

## 4. Matriz de Coincidencia: Tablas del Diccionario vs. PostgreSQL

Total de tablas en Diccionario: **18**. Total creadas en BD: **18** (+ historial de Flyway).

| # | Tabla en Diccionario | Módulo | PK | Claves Foráneas (FK) | Restricciones / Índices Clave | Estado |
|---|---|---|---|---|---|---|
| 1 | `roles` | Módulo 1 | `id_rol` | - | - | Coincide 100% |
| 2 | `empleados` | Módulo 1 | `id_empleado` | `id_rol`, `id_sucursal` | UNIQUE(`dni`), UNIQUE(`correo`), `estado` BOOLEAN | Coincide 100% |
| 3 | `auditoria_logs` | Módulo 1 | `id_log` | `id_empleado` | `fecha_hora` TIMESTAMPTZ | Coincide 100% |
| 4 | `sucursales` | Módulo 2 | `id_sucursal` | - | `nombre_agencia`, `ciudad` | Coincide 100% |
| 5 | `vehiculos` | Módulo 2 | `id_vehiculo` | - | UNIQUE(`placa`), `capacidad_carga_kg` NUMERIC | Coincide 100% |
| 6 | `rutas` | Módulo 2 | `id_ruta` | `id_sucursal_origen`, `id_sucursal_destino` | `distancia_km` NUMERIC | Coincide 100% |
| 7 | `programacion_viajes` | Módulo 2 | `id_viaje` | `id_ruta`, `id_vehiculo`, `id_chofer` | CHECK(`estado_viaje` IN ('PROGRAMADO','EN_RUTA','FINALIZADO')) | Coincide 100% |
| 8 | `clientes` | Módulo 3 | `id_cliente` | - | UNIQUE(`numero_documento`), CHECK(`tipo_documento` IN ('DNI','RUC')) | Coincide 100% |
| 9 | `pre_registros_express` | Módulo 3 | `id_preregistro` | `id_cliente_remitente`, `id_cliente_destinatario`, `id_sucursal_destino` | UNIQUE(`codigo_express`), CHECK(`estado` IN ('PENDIENTE_VENTANILLA','PROCESADO','EXPIRADO')) | Coincide 100% |
| 10 | `encomiendas` | Módulo 4 | `id_encomienda` | `id_remitente`, `id_destinatario`, `id_sucursal_origen`, `id_sucursal_destino` | UNIQUE(`codigo_comprobante`), CHECK(`estado_pago` IN ('PAGADO','PENDIENTE')), CHECK(`estado_logistico` IN ('REGISTRADO','EN_ALMACEN_ORIGEN','EN_RUTA','EN_ALMACEN_DESTINO','ENTREGADO')) | Coincide 100% |
| 11 | `despachos_lote_detalles` | Módulo 4 | `id_despacho_detalle` | `id_viaje`, `id_encomienda` | `fecha_asociacion` TIMESTAMPTZ | Coincide 100% |
| 12 | `incidencias_emergencia` | Módulo 4 | `id_incidencia` | `id_encomienda`, `id_empleado_reporta` | CHECK(`tipo_incidencia` IN ('ETIQUETA_DANADA','ETIQUETA_PERDIDA')) | Coincide 100% |
| 13 | `parejas_doble_qr` | Módulo 5 | `id_pareja_qr` | `id_encomienda` (UNIQUE) | `uuid_qr1_operativo`, `hash_qr2_llave_digital` | Coincide 100% |
| 14 | `tokens_qr2_estatus` | Módulo 5 | `id_token_status` | `id_pareja_qr` | CHECK(`estado_token` IN ('ACTIVO','INVALIDADO','EXPIRADO')), CHECK(`motivo_inactivacion` IN ('ENTREGA_EXITOSA','INTENTO_FRAUDE')) | Coincide 100% |
| 15 | `validaciones_cruzadas_logs` | Módulo 5 | `id_validacion` | `id_pareja_qr`, `id_empleado_valida` | CHECK(`resultado` IN ('EXITOSO','FALLIDO_PAGO_PENDIENTE','FALLIDO_TOKEN_INVALIDO')), `latencia_procesamiento_ms` INT | Coincide 100% |
| 16 | `historial_tracking` | Módulo 6 | `id_tracking` | `id_encomienda`, `id_sucursal_actual` | `estado_evento`, `fecha_hora_evento` TIMESTAMPTZ | Coincide 100% |
| 17 | `comprobantes_pago` | Módulo 6 | `id_comprobante` | `id_encomienda` (UNIQUE) | CHECK(`tipo_comprobante` IN ('BOLETA','FACTURA')), `serie_correlativo` | Coincide 100% |
| 18 | `archivos_pdf_comprobante` | Módulo 6 | `id_pdf` | `id_comprobante` (UNIQUE) | `hash_sha256_archivo`, `ruta_almacenamiento_vps` | Coincide 100% |

---

## 5. Seguridad y Mejoras Futuras

### 5.1. Cambio Obligatorio de Credenciales Temporales
> **IMPORTANTE**: La contraseña temporal del usuario administrador inicial (`EMP-ADMIN-01` / `admin@marvisur.pe`) es de uso exclusivo para aprovisionamiento inicial y pruebas. **Debe cambiarse de forma obligatoria antes de cualquier despliegue** en entornos de preproducción o producción.

### 5.2. Mejoras Futuras de Seguridad (Fuera del Alcance del Diccionario Actual)
En estricto apego a las reglas de trabajo del proyecto SGL-DQR (cero invención de tablas y campos no contemplados en `docs/Diccionario_Datos_SGL_DQR.md`), las siguientes capacidades se documentan como mejoras futuras recomendadas:
1. **Bloqueo de cuentas por intentos fallidos**: Requeriría incorporar a la tabla `empleados` las columnas `intentos_fallidos INT` y `fecha_bloqueo TIMESTAMP`.
2. **Recuperación y restablecimiento de contraseña autogestionado**: Requeriría incorporar a la tabla `empleados` las columnas `token_recuperacion VARCHAR(64)` y `expiracion_token TIMESTAMP`.
