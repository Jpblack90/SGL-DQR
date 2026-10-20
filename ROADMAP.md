# ROADMAP — SGL-DQR (construcción desde cero, modo individual)

**Punto de reanudación:** P5 — API: persistencia (adaptador PostgreSQL), errores y Swagger. Subpaso: Entidades JPA y mapeadores dominio-persistencia. Archivo que sigue: `Backend-marvisur/marvisur-api/src/main/java/pe/marvisur/api/persistencia/entidad/EncomiendaJpaEntity.java`.
Último commit: `feat(P2)` en rama `feature/API-01-flyway-postgresql`.

- [x] P1  Esqueleto del repo (Maven multimódulo, estructura de carpetas, .gitignore)
- [x] P2  BD PostgreSQL: migraciones Flyway desde el Diccionario de Datos (V1 a V7 según dependencias FK, 18 tablas exactas, BCrypt real verificado, README.md, docker-compose.yml y tests de integración con H2)
- [x] P3  Core: dominio, puertos y servicios base — entidades Encomienda, CodigoQR, Agencia, Ruta, Usuario, Vehiculo; puertos RepositorioEncomiendaPort, RepositorioCodigoQRPort, ImpresionPort; casos de uso Registrar/ValidarEntrega/Consultar (31 tests, BUILD SUCCESS)
- [x] P4  Core: Motor Doble QR con ZXing (SHA-256 de imagen PNG, validarCruzado, invalidarQR2)
- [ ] P5  API: persistencia (adaptador PostgreSQL), errores y Swagger
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