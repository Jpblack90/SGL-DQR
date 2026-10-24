# ROADMAP — SGL-DQR (Fase 3: Desarrollo e Implementación)

> Leyenda: [x] hecho · [~] en curso · [ ] pendiente  
> Estado: **Fase B CONGELADA para Entrega · Versión 0.2.1**

---

## Estado general

- **FASE A — Backend núcleo**: CERRADA (Release v0.1.0 publicado).
- **FASE B — Ecosistema Multiplataforma**: **CONGELADA PARA ENTREGA (Release v0.2.1)**.
  - El MVP operativo (Core, API REST, App de escritorio JavaFX con soporte offline de ventanilla y Portal Web de autoservicio con Billetera Digital y pre-registro Código Express) se encuentra respaldado por 263 pruebas automatizadas en verde.
  - Tareas no críticas o mejoras futuras quedan explícitamente postergadas como backlog pendiente: **B1, B2, B3, D4, H1, H2, O3, O4**.

---

## Decisiones vigentes (Congeladas para v0.2.1)

- **Arquitectura hexagonal**: `marvisur-core` es Java 21 puro (sin Spring, JavaFX ni JPA); ArchUnit lo verifica en cada compilación.
- **Identificadores**: Todas las llaves primarias son UUID (VARCHAR(36)), generadas en el cliente para soporte offline total.
- **Doble QR y Zero-Knowledge**: El QR2 se almacena únicamente como hash SHA-256 en PostgreSQL y SQLite local; se entrega en claro una sola vez (respuesta de admisión y fragmento `#` de la Billetera Digital). Es irreversiblemente invalidado tras la entrega física en almacén.
- **Estado de bloqueo**: «Bloqueada» es un estado derivado (`EN_ALMACEN_DESTINO` + `estado_pago = PENDIENTE`). El sistema exige cobro previo antes de validar la entrega.
- **Autenticación**: JWT stateless (256 bits) para roles internos `TN1_RECEPCIONISTA`, `TN2_ALMACENERO`, `TN3_ADMINISTRADOR`, `TN4_CHOFER`. Clientes finales (An1) operan con autoservicio sin contraseña (Fricción Cero).
- **Fuente de verdad**: `docs/Diccionario_Datos_SGL_DQR.md` (18 tablas, 6 módulos).

---

## FASE A — Backend núcleo (CERRADA - v0.1.0)

- [x] **P1** Esqueleto del repositorio (Maven multimódulo, Spring Boot, JavaFX mínimo, CI)
- [x] **P2** Migraciones Flyway V1–V7: 18 tablas del Diccionario + datos iniciales
- [x] **P3** Core: dominio, puertos y servicios base
- [x] **P4** Core: Motor Doble QR (ZXing, hashes, validación cruzada)
- [x] **P5** API: persistencia JPA, adaptadores PostgreSQL, errores RFC 7807, Swagger
- [x] **P6** API: autenticación JWT y RBAC (RF-01)
- [x] **P7** API: registro de encomiendas con Doble QR (RF-04, RF-07)
- [x] **P8** API: validación cruzada, cobro Contra Entrega y entrega (RF-04, RF-07)
- [x] **P9** API: almacén — lotes, búsqueda DNI/RUC, incidencias (RF-06)
- [x] **P10** API pública: tracking, pre-registro Código Express y Billetera (RF-08, 09, 10)
- [x] **P11** Entrega al equipo: datos de demo (perfil dev), CI con Testcontainers, normalizar nombres, README, tag v0.1.0

---

## FASE B — Ecosistema y Estabilización (CONGELADA PARA ENTREGA - v0.2.1)

- [x] **P12 Congelamiento de funciones y kit de demo**:
  - Scripts de demostración multiplataforma (`demo-reset`, `demo-api`, `demo-portal`, `demo-escritorio`, `demo-parar-api`, `demo-iniciar-api`).
  - Matriz de estado de requerimientos en `docs/estado-implementacion.md`.
  - Pipeline de CI en `.github/workflows/ci.yml` cubriendo Core, API, Escritorio y Portal Web.
  - Verificación estricta de cero secretos y preparación del Release v0.2.0.
- [x] **P12b Auditoría y corrección honesta de requerimientos y limitaciones**:
  - Corrección de definiciones oficiales de RNF-01 a RNF-05 y fuera de alcance en `docs/requisitos.md`.
  - Reauditoría honesta de RF y RNF contra código real en `docs/estado-implementacion.md` (RF-02 Pendiente, RF-03 Pendiente, RF-05 Parcial, RF-10 Parcial, RF-11 Pendiente; RNF-01..RNF-04 Parciales, RNF-05 Implementado).
  - Explicitación de limitaciones conocidas (hardware simulado, HTTPS ausente, sin instalador .EXE, tarifario provisional).
  - Ajuste de redacción en README raíz eliminando afirmaciones no verificables.
- [x] **P13 Coherencia documental y métricas reales**:
  - Normalización de títulos en `docs/estado-implementacion.md` para correspondencia exacta con `docs/requisitos.md` (RF-04 como CRUD Encomiendas, RF-07 y RF-11).
  - Reemplazo del término «equipaje» por «encomienda» en especificaciones técnicas y matrices.
  - Desglose explícito en RNF-05 de las 5 pruebas ArchUnit dentro del total de 50 pruebas en `marvisur-core`.
  - Precisión de RF-10 (backend y escritorio implementados; portal web W3 pendiente en backlog).
  - Creación de `scripts/informe-metricas.ps1` y `scripts/informe-metricas.sh` para extracción automatizada de métricas reales.
  - Generación del informe consolidado `docs/informe-metricas.md` y alineación con `README.md` y `docs/estado-implementacion.md`.

### Estado por Pista de Trabajo

#### I1 — Backend API
- [x] **B4** Endpoints de sincronización idempotentes por UUID (`POST /api/v1/sync/encomiendas`, RNF-02)
- [~] **B1** Dashboard (KPIs en tiempo real, API lista; empleados con auditoría pendiente) (RF-02, RF-03)
- [ ] **B2** *[Pendiente / Backlog]* Flota, rutas y ciclo de viaje; TN4 actualiza estado; «En ruta» masivo (RF-05)
- [ ] **B3** *[Pendiente / Backlog]* Comprobante PDF: comprobantes_pago y archivos_pdf_comprobante (RF-11)
- [x] **B5** Pruebas de rendimiento RNF-03 y perfil dev con Swagger / prod protegido

#### I2 — Escritorio JavaFX (offline-first)
- [x] **D1** Shell, tema de marca, login y navegación por rol (RF-01)
- [x] **D2** Ventanilla: registro de encomienda, Código Express y datos de impresión térmica 58 mm (RF-04)
- [x] **D3** Almacén: validación cruzada Doble QR, Batch Scanning, contingencia e incidencias (RF-06, RF-07)
- [ ] **D4** *[Pendiente / Backlog]* Dashboard, empleados, choferes y rutas (RF-02, RF-03, RF-05)

#### I3 — Portal Web y Pruebas (React + Vite + TypeScript + Tailwind)
- [x] **W1** Scaffold del portal con tema de marca y catálogo público de agencias
- [x] **W2** Rastreo de envío (RF-08) con línea de tiempo y anonimización
- [x] **W4** Billetera Digital con lectura segura del token en fragmento hash `#` (RF-09)
- [x] **W3** Cotización en línea y pre-registro web «Código Express» con generación QR en cliente (RF-10)
- [ ] **W5** *[Pendiente / Backlog]* Descarga de comprobante PDF en portal (RF-11; requiere B3)
- [x] **Q1** Suite de pruebas unitarias e integración en Vitest (33 tests) y manual UAT documentada

#### I4 — Hardware, Offline e Infraestructura
- [x] **O1** Esquema SQLite local, adaptador `SQLiteColaAdapter` y conmutación offline $\le 2$ s (RNF-02, RNF-05)
- [x] **O2** Sincronizador en segundo plano SQLite <-> PostgreSQL con reintentos exponenciales (usa B4)
- [ ] **H1** *[Pendiente / Backlog]* Adaptador ESC/POS para ticketera física de 58 mm por puerto USB/Serie (actualmente emulado en PNG)
- [ ] **H2** *[Pendiente / Backlog]* Adaptador de escáner óptico USB vía API nativa (actualmente soportado vía emulación de teclado con `CampoEscaneo`)
- [ ] **O3** *[Pendiente / Backlog]* Despliegue en VPS de producción con HTTPS (RNF-01)
- [ ] **O4** *[Pendiente / Backlog]* Instalador .EXE autocontenido con `jpackage` (RNF-04; ejecutable vía Maven y JAR)

---

## Resumen de Tareas Pendientes Post-v0.2.1

Para auditoría docente y técnica, se explicitan los 8 ítems que quedan fuera del congelamiento v0.2.0:
1. `B1` — Dashboard analítico de KPIs y módulo visual de empleados.
2. `B2` — Gestión dinámica de flota y actualización móvil por choferes.
3. `B3` — Generador binario de comprobantes en PDF.
4. `D4` — Pantallas administrativas de escritorio para TN3.
5. `H1` — Driver directo ESC/POS para hardware ticketera térmica física.
6. `H2` — Driver nativo USB para escáner óptico.
7. `O3` — Infraestructura VPS en la nube y certificados SSL/TLS.
8. `O4` — Empaquetado binario .EXE mediante `jpackage`.
