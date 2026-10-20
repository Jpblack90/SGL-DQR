# ROADMAP — SGL-DQR · Expreso Marvisur

> Leyenda: [ ] pendiente · [x] completado · [~] en progreso · [!] bloqueado

---

## I1 — Núcleo de Dominio (marvisur-core)

- [x] **CORE-01** Estructura Maven multimódulo + entidades de dominio (Encomienda, CodigoQR, Agencia, Ruta, Usuario, Vehiculo) + puertos (RepositorioEncomiendaPort, RepositorioCodigoQRPort, ImpresionPort)
- [ ] **CORE-02** MotorQR: generarDobleQR (ZXing + SHA-256), validarCruzado, invalidarQR2 + pruebas unitarias MotorQRTest
- [ ] **CORE-03** Casos de uso: RegistrarEncomiendaUseCase, ValidarEntregaUseCase, ConsultarEncomiendaUseCase + pruebas unitarias con Mockito
- [ ] **CORE-04** ArchUnit: reglas de arquitectura hexagonal (core sin dependencias de Spring/JPA/JavaFX)

---

## I2 — Backend API (marvisur-api · Spring Boot)

- [ ] **API-01** Configuración Spring Boot 3 + Flyway + PostgreSQL: DataSource, migraciones V1__esquema_inicial.sql, V2__datos_semilla.sql
- [ ] **API-02** Adaptador PostgreSQLAdapter (implementa RepositorioEncomiendaPort + RepositorioCodigoQRPort con JPA/Hibernate)
- [ ] **API-03** REST endpoints CRUD Encomienda: POST /api/encomiendas, GET /api/encomiendas/{id}, PUT /api/encomiendas/{id}/estado
- [ ] **API-04** REST endpoint validación entrega: POST /api/encomiendas/{id}/validar-entrega
- [ ] **API-05** REST endpoint token cliente (acceso sin usuario/contraseña): GET /api/encomiendas/token/{token}
- [ ] **API-06** Sincronización offline: POST /api/sync/encomiendas (recibe lote de UUIDs desde SQLite)
- [ ] **API-07** Pruebas de integración con H2 (@SpringBootTest, @Sql)

---

## I3 — Escritorio JavaFX (offline-first)

- [ ] **FX-01** Proyecto JavaFX 21 + Maven: módulo-info.java, App.java, estructura FXML
- [ ] **FX-02** SQLiteAdapter (implementa RepositorioEncomiendaPort + RepositorioCodigoQRPort con JDBC puro)
- [ ] **FX-03** Pantalla Registro de Encomienda (TN1): formulario, calcular tarifa, generar Doble QR
- [ ] **FX-04** Pantalla Validación Entrega (TN2): escanear QR1+QR2, validar cruce, bloquear/autorizar
- [ ] **FX-05** ESCPOSAdapter: imprimir ticket QR1 en ticketera térmica 58mm por USB/ESC·POS
- [ ] **FX-06** Sincronización con backend (conmutación <= 2 s · RNF-02)
- [ ] **FX-07** jpackage: empaquetar como .EXE autocontenido (RNF-04)

---

## I4 — Portal Web + Billetera Digital (React + Vite + TS + Tailwind)

- [ ] **WEB-01** Scaffolding: Vite + React 18 + TypeScript + Tailwind CSS + React Router
- [ ] **WEB-02** Página Pre-registro Web (An1): formulario cotización de tarifa (sin login)
- [ ] **WEB-03** Página Trazabilidad de Envío (An1): consulta por token temporal, timeline de estados
- [ ] **WEB-04** Billetera Digital (An1): visualizar QR2 digital, estado de pago, descargar comprobante
- [ ] **WEB-05** Integración con marvisur-api: cliente HTTP (axios/fetch), manejo offline/error
- [ ] **WEB-06** Pruebas con Vitest + React Testing Library

---

## Tareas Compartidas / DevOps

- [ ] **DEV-01** GitHub Actions CI: build + test en push a main y PRs
- [ ] **DEV-02** Docker Compose: postgres + marvisur-api para desarrollo local
- [ ] **DEV-03** Documentación API (OpenAPI 3 / Swagger UI)
