# CHANGELOG — SGL-DQR · Expreso Marvisur

Formato basado en [Keep a Changelog](https://keepachangelog.com/es/1.1.0/).
Versiones siguiendo [Semantic Versioning](https://semver.org/lang/es/).

---

## [Unreleased]

### Added
- [P3] Core de dominio en `Backend-marvisur/marvisur-core` (Java 21 puro, paquete `pe.marvisur.sgldqr`): entidades `Encomienda`, `ParDobleQR`, `Cliente`, `Empleado`, `Sucursal` y enums oficiales del Diccionario (`EstadoPago`, `EstadoLogistico`, `Rol`, `TipoDocumento`, `EstadoTokenQR2`); puertos `RepositorioEncomiendaPort`, `RepositorioClientePort`, `RepositorioEmpleadoPort`, `RepositorioSucursalPort`, `RepositorioParDobleQRPort`, `ImpresionPort`, `RelojPort`, `TarifarioPort`; servicios de aplicación `RegistrarEncomiendaService`, `CambiarEstadoService` y tarifario provisional `TarifarioPorPeso` (con TODO explicativo); regla de negocio que bloquea la transición a `ENTREGADO` si el pago está `PENDIENTE` (Contra Entrega); 23 pruebas automatizadas (JUnit 5 + Mockito + ArchUnit) en verde garantizando aislamiento frente a Spring, JPA y JavaFX. Pendiente: P4 (Core: Motor Doble QR con ZXing).
- [P2] BD PostgreSQL: migraciones Flyway (V1 a V7) agrupadas por módulo del Diccionario de Datos y ordenadas estrictamente por dependencias FK (18 tablas exactas: `sucursales`, `vehiculos`, `rutas`, `programacion_viajes`, `roles`, `empleados`, `auditoria_logs`, `clientes`, `pre_registros_express`, `encomiendas`, `despachos_lote_detalles`, `incidencias_emergencia`, `parejas_doble_qr`, `tokens_qr2_estatus`, `validaciones_cruzadas_logs`, `historial_tracking`, `comprobantes_pago`, `archivos_pdf_comprobante`), llaves VARCHAR(36), resolución de FK circular mediante ALTER TABLE, datos iniciales con 4 roles y administrador TN3 (hash BCrypt verificado con cost factor 12), `database/postgres/README.md`, `docker-compose.yml`, y 7 pruebas de integración automatizadas. Pendiente: P5 (API: persistencia JPA, adaptador PostgreSQL, errores y Swagger).
- [I1][P1] Estructura Maven multimódulo raíz (`marvisur-parent`), módulos `marvisur-core` y `marvisur-api` (esqueleto), carpetas `frontend-marvisur/escritorio`, `frontend-marvisur/portal-web`, `database/migrations`, `database/sqlite`. Rama: `feature/CORE-01-dominio-hexagonal`.
- [I1][P3] Dominio hexagonal puro (`marvisur-core`, sin Spring/JPA/JavaFX): entidades `Encomienda` (ciclo de vida + calcularTarifa), `CodigoQR` (invalidarQR2 irreversible), `Agencia`, `Ruta`, `Usuario`, `Vehiculo`; enums `EstadoEnvio`, `EstadoPago`, `EstadoQR2`, `Rol`; excepciones de dominio; puertos `RepositorioEncomiendaPort`, `RepositorioCodigoQRPort`, `ImpresionPort`; casos de uso `RegistrarEncomiendaUseCase`, `ValidarEntregaUseCase`, `ConsultarEncomiendaUseCase`. 31 tests unitarios — BUILD SUCCESS.
- [I1][P4] `MotorQR`: generación de par QR con ZXing (PNG 300×300), hash SHA-256 de bytes de imagen (nunca del texto en claro), `validarCruzado`, `invalidarQR2`. `ParDobleQR` record, `ResultadoValidacion` enum. Pendiente: ArchUnit (CORE-04).

---

## [0.1.0] — TBD

### Added
- Estructura inicial del repositorio SGL-DQR

---

*Los integrantes añaden entradas bajo [Unreleased] con formato:*
*`[Ix][ID-TAREA] Descripción breve — pendiente: X`*
