# ROADMAP — SGL-DQR (Fase 3: Desarrollo e Implementación)

> Leyenda: [x] hecho · [~] en curso · [ ] pendiente  
> Estado: **Versión 0.3.0 publicada · Despliegue en servidor Linux (Ubuntu) preparado para v0.3.1**

---

## Estado general

- **FASE A — Backend núcleo**: CERRADA (Release v0.1.0 publicado).
- **FASE B — Ecosistema Multiplataforma**: **CONGELADA PARA ENTREGA (Release v0.2.1)**.
  - El MVP operativo (Core, API REST, App de escritorio JavaFX con soporte offline de ventanilla y Portal Web de autoservicio con Billetera Digital y pre-registro Código Express) se encuentra respaldado por 263 pruebas automatizadas en verde.
  - En v0.3.0 se completaron las tareas que habían quedado en backlog: **B1, B2, B3, D4, D5, W5 y O4** (466 pruebas en verde).
- **Despliegue en servidor Linux (O5)**: script `deploy/ubuntu/instalar.sh` y guía `docs/despliegue-linux.md` (Ubuntu Server con PostgreSQL, Nginx y pm2), preparado para v0.3.1.
- **Pendiente real**: **H1, H2, O3** y el instalador MSI de O4 (ver el resumen al final).

---

## Decisiones vigentes (v0.3.0)

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
- [x] **B1** Dashboard y Personal (KPIs en tiempo real y gestión de empleados con auditoría completados en backend; resuelta integridad referencial en pruebas de auditoría) (RF-02, RF-03)
- [x] **B2** Flota, rutas y ciclo de viaje en backend API; endpoints vehiculos (TN3), rutas (TN2/TN3/TN4), viajes (TN2/TN3/TN4 con RBAC chofer), programación y avance de estado con auditoría y manifiesto sin datos personales (RF-05)
- [x] **B3** Comprobante PDF con Apache PDFBox: comprobantes_pago y archivos_pdf_comprobante, regenerado al cambiar el estado de pago (RF-11)
- [x] **B5** Pruebas de rendimiento RNF-03 y perfil dev con Swagger / prod protegido
- *Punto de reanudación I1:* Pista cerrada en v0.3.0 (B1 a B5 completas).

#### I2 — Escritorio JavaFX (offline-first)
- [x] **D1** Shell, tema de marca, login y navegación por rol (RF-01)
- [x] **D2** Ventanilla: registro de encomienda, Código Express y datos de impresión térmica 58 mm (RF-04)
- [x] **D3** Almacén: validación cruzada Doble QR, Batch Scanning, contingencia e incidencias (RF-06, RF-07)
- [x] **D4** Dashboard y Personal (RF-02 KPIs y RF-03 Gestión de personal y auditoría en escritorio para TN3 completados; consistencia FXML y formato amigable de combos verificado)
- [x] **D5** Rutas y Flota: gestión de unidades vehiculares (TN3), programación de viajes interprovinciales (TN3), despacho y avance de estado en ruta (TN3, TN4), consulta modal de manifiesto de carga sin datos personales y navegación RBAC (RF-05)
- *Punto de reanudación I2:* Pulido final de interfaz completado (traducción de errores de lote, vaciado de campo de escaneo, confirmación sin carga, columna de rutas, placeholders, menú lateral, título de ventana y login). Siguiente paso: verificación manual en entorno gráfico con usuario final.

#### I3 — Portal Web y Pruebas (React + Vite + TypeScript + Tailwind)
- [x] **W1** Scaffold del portal con tema de marca y catálogo público de agencias
- [x] **W2** Rastreo de envío (RF-08) con línea de tiempo y anonimización
- [x] **W4** Billetera Digital con lectura segura del token en fragmento hash `#` (RF-09)
- [x] **W3** Cotización en línea y pre-registro web «Código Express» con generación QR en cliente (RF-10)
- [x] **W5** Descarga del comprobante PDF desde el rastreo del portal (RF-11)
- [x] **Q1** Suite de pruebas unitarias e integración en Vitest (33 tests) y manual UAT documentada

#### I4 — Hardware, Offline e Infraestructura
- [x] **O1** Esquema SQLite local, adaptador `SQLiteColaAdapter` y conmutación offline $\le 2$ s (RNF-02, RNF-05)
- [x] **O2** Sincronizador en segundo plano SQLite <-> PostgreSQL con reintentos exponenciales (usa B4)
- [ ] **H1** *[Pendiente / Backlog]* Adaptador ESC/POS para ticketera física de 58 mm por puerto USB/Serie (actualmente emulado en PNG)
- [ ] **H2** *[Pendiente / Backlog]* Adaptador de escáner óptico USB vía API nativa (actualmente soportado vía emulación de teclado con `CampoEscaneo`)
- [~] **O3** Despliegue con HTTPS (RNF-01): la demostración en Render (API en Docker y portal estático) usa HTTPS de la plataforma; falta un servidor propio con certificado TLS (Nginx + Let's Encrypt)
- [x] **O4** Paquete portátil autocontenido .EXE con JRE embebido mediante jlink y jpackage (RNF-04)
- [x] **O5** Despliegue en servidor Linux propio (Ubuntu Server con PostgreSQL, Nginx y pm2): script `deploy/ubuntu/instalar.sh`, permisos por carpeta y guía `docs/despliegue-linux.md`
- [x] **CIERRE** Pipeline automatizado de cierre de versión v0.3.0 (`scripts/cierre-version.ps1`), sincronización OpenAPI y actualización de matrices
- *Punto de reanudación I4:* Cierre de versión v0.3.0 finalizado con contratos OpenAPI normalizados (38 operaciones sin /test-rbac), scripts con BOM UTF-8 y métricas sincronizadas (466 pruebas en verde, 40,551 LOC, 18 tablas). Merge a `main` y etiqueta `v0.3.0` realizados. Siguiente paso: probar `deploy/ubuntu/instalar.sh` en una máquina limpia y cerrar v0.3.1.

#### Mejoras para la versión comercial
- [x] **M1** QR impreso y envío al cliente: etiqueta del bulto con QR1 (al registrar y en la reimpresión de Contingencia), ticket del cliente con QR de rastreo y nombres de agencias, texto del QR1 con botón «Copiar QR1», envío de la Billetera por WhatsApp o correo, y rastreo del portal con el comprobante en la dirección (`/rastreo?codigo=`). El QR2 sigue siendo solo digital.
- [x] **O6** Copia de seguridad diaria de PostgreSQL en el servidor Linux (`deploy/ubuntu/respaldo-db.sh`, retención de 14 días, carpeta 700).
- [x] **M2** Operación multiagencia: el Administrador (TN3) recepciona y despacha en cualquier agencia, el historial registra la agencia física del bulto y el cierre de viaje avisa cuántos bultos faltan recepcionar en destino.
- *Punto de reanudación M:* M1, M2 y O6 terminados con pruebas (núcleo 50, escritorio 174, portal 40, API con la nueva prueba de Almacén). Siguiente paso: validar en la máquina virtual y decidir con Marvisur las tareas de la Ola 3.

---

## Resumen de tareas pendientes (después de v0.3.0)

Las tareas B1, B2, B3, D4 y O4 que figuraban como backlog en v0.2.1 se completaron en v0.3.0. Queda pendiente:

1. `H1` — Driver ESC/POS para la ticketera térmica física (hoy el ticket se genera como imagen PNG).
2. `H2` — Driver nativo USB para el escáner óptico (hoy funciona por emulación de teclado).
3. `O3` — Servidor propio con HTTPS (certificado TLS en Nginx); la demostración en Render ya usa HTTPS.
4. `O4` (parcial) — Instalador MSI de la app de escritorio, que requiere WiX Toolset; el paquete portátil .EXE ya existe.
