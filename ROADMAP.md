# ROADMAP — SGL-DQR (Fase 3: Desarrollo e Implementación)

Leyenda: [x] hecho · [~] en curso · [ ] pendiente
Punto de reanudación: P11 (Entrega al equipo). Subpaso: Normalización de nombres de carpetas a minúsculas y refactor de auditoría autónoma. Archivo que sigue: backend-marvisur/marvisur-api/src/main/java/pe/marvisur/sgldqr/api/persistencia/postgres/AuditoriaEscrituraAutonoma.java.

## Estado general
- FASE A — Backend núcleo (trabajo individual): P1–P7 hechos; P8–P11 en curso.
- FASE B — Equipo en paralelo: empieza al publicarse v0.1.0 (cierre de P11).

## Decisiones vigentes (no reabrir sin avisar al equipo)
- Arquitectura hexagonal: marvisur-core es Java puro (sin Spring, JavaFX ni JPA);
  ArchUnit lo verifica en cada build.
- Todas las llaves son UUID (VARCHAR(36)), generados en el cliente (soporta offline).
- El QR2 se guarda solo como hash SHA-256 y se entrega en claro UNA vez, en la
  respuesta de registro y en el enlace /billetera#<token>. No se puede recuperar.
- «Bloqueada» no es un valor de estado_logistico: es un estado derivado
  (EN_ALMACEN_DESTINO + estado_pago = PENDIENTE). El core impide pasar a ENTREGADO.
- codigo_comprobante y la tarifa por peso son PROVISIONALES (hay TODO en el código);
  falta confirmar numeración y tarifario reales con Marvisur.
- Autenticación: JWT sin sesión; roles TN1 Recepcionista, TN2 Almacenero,
  TN3 Administrador, TN4 Chofer. Cliente final (An1) sin usuario ni contraseña.
- El Diccionario de Datos es la fuente de verdad del esquema (18 tablas, 6 módulos).
  No agregar ni quitar columnas sin acuerdo del equipo.
- Fuera de alcance: integración técnica con SUNAT (XML/CDR) y apps móviles nativas.

## FASE A — Backend núcleo
- [x] P1  Esqueleto del repositorio (Maven multimódulo, Spring Boot, JavaFX mínimo, CI)
- [x] P2  Migraciones Flyway V1–V7: 18 tablas del Diccionario + datos iniciales
- [x] P3  Core: dominio, puertos y servicios base
- [x] P4  Core: Motor Doble QR (ZXing, hashes, validación cruzada)
- [x] P5  API: persistencia JPA, adaptadores PostgreSQL, errores RFC 7807, Swagger
- [x] P6  API: autenticación JWT y RBAC (RF-01)
- [x] P7  API: registro de encomiendas con Doble QR (RF-04, RF-07)
- [x] P8  API: validación cruzada, cobro Contra Entrega y entrega (RF-04, RF-07)
- [x] P9  API: almacén — lotes, búsqueda DNI/RUC, incidencias (RF-06)
- [x] P10 API pública: tracking, pre-registro Código Express y Billetera (RF-08, 09, 10)
- [ ] P11 Entrega al equipo: datos de demo (perfil dev), CI con Testcontainers,
         normalizar nombre de carpetas, README, tag v0.1.0

## FASE B — Pistas paralelas (cada integrante edita SOLO su sección)
Dueños: I1 Backend · I2 Escritorio JavaFX · I3 Portal web y pruebas · I4 BD, hardware, offline e infraestructura

### I1 — Backend restante
Punto de reanudación: (vacío)
- [ ] B1 Dashboard (KPIs en tiempo real) y empleados con auditoría (RF-02, RF-03)
- [ ] B2 Flota, rutas y ciclo de viaje; TN4 actualiza estado; «En ruta» masivo (RF-05)
- [ ] B3 Comprobante PDF: comprobantes_pago y archivos_pdf_comprobante (RF-11)
- [ ] B4 Endpoints de sincronización idempotentes por UUID (apoya RNF-02)
- [ ] B5 Pruebas de rendimiento RNF-03 y Swagger desactivado en prod

### I2 — Escritorio JavaFX (consume la API)
Punto de reanudación: (vacío)
- [ ] D1 Shell, tema de marca, login y navegación por rol (RF-01)
- [ ] D2 Ventanilla: registro de encomienda y datos de impresión (RF-04)
- [ ] D3 Almacén: validación cruzada, Batch Scanning e incidencias (RF-06, RF-07)
- [ ] D4 Dashboard, empleados, choferes y rutas (RF-02, RF-03, RF-05)

### I3 — Portal web y pruebas (stack propuesto: React + Vite + TypeScript + Tailwind; confirmar antes de W1)
Punto de reanudación: (vacío)
- [ ] W1 Scaffold del portal con tema de marca
- [ ] W2 Rastreo de envío (RF-08)
- [ ] W3 Cotización y pre-registro Código Express (RF-10)
- [ ] W4 Billetera Digital: lee el token del fragmento #, nunca lo envía en la URL (RF-09)
- [ ] W5 Descarga de comprobante PDF (RF-11; requiere B3)
- [ ] Q1 Guion UAT y pruebas de integración end-to-end

### I4 — Hardware, offline e infraestructura
Punto de reanudación: (vacío)
- [ ] H1 Adaptador ESC/POS: imprimir QR1 en ticketera de 58 mm (ImpresionPort)
- [ ] H2 Adaptador de escáner óptico USB
- [ ] O1 Esquema SQLite desde el Diccionario, SQLiteAdapter y conmutación ≤ 2 s (RNF-02)
- [ ] O2 Sincronizador SQLite <-> PostgreSQL por UUID (usa B4)
- [ ] O3 HTTPS y VPS en producción (RNF-01)
- [ ] O4 Instalador .EXE autocontenido con jpackage (RNF-04)

## Cómo trabajar (desde v0.1.0)
1. git clone, luego copia .env.example a .env y ejecuta docker compose up -d.
2. API: http://localhost:8080/swagger-ui.html es la fuente de verdad de los endpoints.
3. Ramas: main (solo releases), develop (integración) y feature/<ID>-descripcion
   (por ejemplo feature/D1-login-escritorio). Nadie hace push directo a main ni a develop.
4. Todo entra por Pull Request: 1 aprobación y CI en verde.
5. Commits: Conventional Commits, por ejemplo feat(RF-06): lote de escaneo.
6. Al cerrar una tarea: márcala [x] en TU sección, añade una línea en CHANGELOG.md
   bajo [Unreleased] con el ID y actualiza tu «Punto de reanudación».
7. Con Antigravity: lee AGENTS.md, ROADMAP.md y CHANGELOG.md; trabaja solo en tu pista.