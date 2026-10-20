# ROADMAP — SGL-DQR (construcción desde cero, modo individual)

**Punto de reanudación:** P6 — API: autenticación y RBAC por rol (RF-01). Subpaso: Servicio de autenticación JWT y filtros de seguridad Spring Security. Archivo que sigue: `Backend-marvisur/marvisur-api/src/main/java/pe/marvisur/sgldqr/api/seguridad/ServicioAutenticacion.java`.
Último commit: `feat(P5): capa de persistencia jpa, adaptadores postgresql, beans y manejo de errores rfc 7807` en rama `feature/P4-motor-doble-qr`.

- [x] P1  Esqueleto del repo (Maven multimódulo, estructura de carpetas, .gitignore)
- [x] P2  BD PostgreSQL: migraciones Flyway desde el Diccionario de Datos (V1 a V7 según dependencias FK, 18 tablas exactas, BCrypt real verificado, README.md, docker-compose.yml y tests de integración con H2)
- [x] P3  Core: dominio, puertos y servicios base — entidades Encomienda, ParDobleQR, Cliente, Empleado, Sucursal y enums del Diccionario; puertos RepositorioPort por agregado, ImpresionPort, RelojPort, TarifarioPort; servicios RegistrarEncomiendaService, CambiarEstadoService, TarifarioPorPeso; regla de bloqueo de entrega por pago pendiente; 23 pruebas unitarias y de arquitectura con ArchUnit en verde
- [x] P4  Core: Motor Criptográfico Doble QR con ZXing (RF-07, RNF-01, RNF-03) — MotorQR (generarDobleQR con SecureRandom y SHA-256, validarCruzado en tiempo constante con MessageDigest.isEqual, bloqueo FALLIDO_PAGO_PENDIENTE sin invalidar, invalidación irreversible con fecha en éxito, latencia ms), puerto ImagenQRPort y adaptador ZXingAdapter en infraestructura.qr; 34 pruebas en verde con ArchUnit
- [x] P5  API: persistencia (adaptador PostgreSQL), errores y Swagger
- [ ] P6  API: autenticación y RBAC por rol (RF-01)
- [ ] P7  API: registrar encomienda + Doble QR + auditoría (RF-04, RF-07)
- [ ] P8  API: validación cruzada, bloqueo Contra Entrega y entrega (RF-04, RF-07)
- [ ] P9  API: dashboard y empleados (RF-02, RF-03)
- [ ] P10 API: flota, rutas, despacho por lote y búsqueda DNI/RUC (RF-05, RF-06)
- [ ] P11 API pública: tracking, pre-registro Código Express y Billetera (RF-08, RF-09, RF-10)
- [ ] P12 API: comprobante PDF (RF-11)
- [ ] P13 Escritorio: shell, tema de marca, login y navegación por rol (RF-01)
- [ ] P14 Escritorio: ventanilla + impresión ESC/POS del QR1 (RF-04, RF-07)
- [ ] P15 Escritorio: almacén, escáner, Batch Scanning y validación cruzada (RF-06, RF-07)
- [ ] P16 Escritorio: dashboard, empleados, choferes y rutas (RF-02, RF-03, RF-05)
- [ ] P17 Offline: esquema SQLite, SQLiteAdapter y conmutación <= 2 s (RNF-02)
- [ ] P18 Offline: sincronización SQLite <-> PostgreSQL por UUID
- [ ] P19 Portal web: scaffold, tema, rastreo, pre-registro y Billetera (RF-08 a RF-11)
- [ ] P20 Cierre: HTTPS, instalador .EXE (jpackage) y pruebas de rendimiento (RNF-01, 03, 04)