# Matriz de Estado de Implementación — SGL-DQR (Expreso Marvisur)

Fecha de corte: **22 de octubre de 2026**  
Versión del sistema: **v0.2.0 (Fase B Congelada)**  
Metodología: **Auditoría honesta basada en código fuente real, pruebas automatizadas y artefactos desplegables.**

---

## 1. Resumen Ejecutivo del Conteo de Pruebas Automatizadas

El sistema cuenta con un total de **230 pruebas automatizadas**, todas en estado **VERDE (0 fallos, 0 errores, 0 omitidas)**, ejecutadas contra entornos reales (PostgreSQL 16 en contenedor Docker, base SQLite local y React Testing Library):

| Módulo / Proyecto | Tecnologías | Pruebas | Estado | Tiempo de Ejecución |
|---|---|:---:|:---:|:---:|
| `marvisur-core` | Java 21, JUnit 5, Mockito, ArchUnit | **50** | **100% Verde** | ~5.5 s |
| `marvisur-api` | Spring Boot 3.3, Testcontainers (PostgreSQL 16), Flyway | **100** | **100% Verde** | ~1 min 11 s |
| `marvisur-escritorio` | JavaFX 21, SQLite JDBC, HttpServer Mock | **64** | **100% Verde** | ~12.9 s |
| `portal-web` | React 18, Vite, TypeScript, Vitest, RTL | **16** | **100% Verde** | ~3.2 s |
| **TOTAL SISTEMA** | **Multiplataforma E2E** | **230** | **100% VERDE** | **~1 min 33 s** |

---

## 2. Requisitos Funcionales (RF)

| Requisito | Denominación | Estado | Evidencia Técnica en el Código | Limitaciones y Alcance Real |
|---|---|:---:|---|---|
| **RF-01** | **Módulo Login y Control de Accesos (RBAC)** | **Implementado** | • `POST /api/v1/auth/login` con JWT HMAC-SHA256.<br>• Roles `TN1_RECEPCIONISTA`, `TN2_ALMACENERO`, `TN3_ADMINISTRADOR`, `TN4_CHOFER`.<br>• `ControladorLogin.java`, `SesionContexto.java`, `ModeloMenuRol.java`.<br>• Pruebas: `AuthControllerTest` (10 tests), `ModeloMenuRolTest` (5 tests). | No incluye recuperación de contraseña por correo ni 2FA. El token JWT en escritorio vive exclusivamente en memoria RAM. |
| **RF-02** | **Panel Administrativo (Dashboard)** | **Parcial** | • `GET /actuator/health` con monitoreo en vivo.<br>• `GET /api/v1/viajes`, `GET /api/v1/publico/sucursales`.<br>• `GET /api/v1/almacen/encomiendas/buscar`, `GET /api/v1/almacen/incidencias`.<br>• Métricas en vivo en pantalla `ColaLocalView.fxml` de escritorio. | El cálculo masivo de KPIs agregados de facturación global (`B1`) y la pantalla dedicada `D4` en escritorio quedaron como trabajo futuro de la Fase B. |
| **RF-03** | **CRUD Empleado** | **Parcial** | • Tablas `empleados` y `usuarios` en migración `V1__esquema_inicial.sql`.<br>• Seed `V100__datos_demo.sql` con 4 empleados operativos.<br>• Trazabilidad de empleados en `auditoria_operaciones`. | La gestión y alta de nuevos empleados se realiza por base de datos o migraciones; no se implementó un formulario visual de creación en la UI. |
| **RF-04** | **CRUD Equipajes (Core Operativo de Admisión)** | **Implementado** | • `POST /api/v1/encomiendas` (transacción atómica con rollback completo verificado).<br>• Soporte de tarifas por peso (`TarifarioPorPeso.java`).<br>• Pantalla JavaFX `VentanillaView.fxml`, validación de DNI/RUC.<br>• Emisión de ticket térmico 58 mm (`ImpresionSimuladaAdapter`).<br>• Modo offline en ventanilla con SQLite (`VentanillaOfflineServicio`).<br>• Pruebas: `EncomiendaControllerTest`, `RegistroEncomiendaAtomicidadTest`. | La numeración de comprobantes (`MV-yyyyMMdd-XXXXXXXX`) y la fórmula tarifaria son provisionales conforme a la especificación de diseño. |
| **RF-05** | **RU Choferes y Flota** | **Parcial** | • Tablas y entidades JPA `vehiculos`, `rutas`, `programacion_viajes`.<br>• `GET /api/v1/viajes` (TN2, TN3, TN4) con filtros por estado y sucursal origen.<br>• Pruebas en `AlmacenControllerTest` y seed `V100`. | La asignación dinámica en caliente desde una interfaz visual y la actualización móvil por choferes (`B2`, `D4`) son extensiones para fases posteriores. |
| **RF-06** | **Módulo de Recepciones (Escaneo Batch / Almacén)** | **Implementado** | • `POST /api/v1/almacen/lotes/escaneo` (`RECEPCIONAR_ORIGEN`, `DESPACHAR_A_VIAJE`, `RECEPCIONAR_DESTINO`).<br>• Límite configurable de 200 ítems, transacciones atómicas por ítem.<br>• Pestaña Lotes en `AlmacenView.fxml`, componente `CampoEscaneo` para pistolas USB.<br>• Búsqueda de contingencia por DNI/RUC y registro de incidencias con regeneración de QR1.<br>• Pruebas: `AlmacenControllerTest` (9 tests), `LoteEscaneoTest`. | Máximo 200 ítems por solicitud para proteger la memoria del servidor. |
| **RF-07** | **Motor Central de Validación Criptográfica (Doble QR)** | **Implementado** | • `MotorQR.java` con generación de Doble QR, hashes SHA-256 e invalidación.<br>• `POST /api/v1/entregas/validar` con bloqueo pesimista `SELECT ... FOR UPDATE`.<br>• Bloqueo automático ante cobro Contra Entrega pendiente (`POST /api/v1/entregas/cobrar`).<br>• Invalidación irreversible de QR2 post-entrega.<br>• Pruebas: `EntregaControllerTest` (10 hilos concurrentes en PostgreSQL real), `MotorQRTest`. | El QR2 en claro se entrega una sola vez al registrar; no se puede regenerar una vez invalidado o entregado. |
| **RF-08** | **Portal Web de Seguimiento (Tracking)** | **Implementado** | • `GET /api/v1/publico/rastreo?codigo=...&documento=...` (Fricción Cero).<br>• Anonimización de nombres (`JUAN P***`) e historial cronológico.<br>• Página web `/rastreo` en React con línea de tiempo visual vertical y banners de advertencia de pago.<br>• Pruebas: `PublicoControllerTest`, `Rastreo.test.tsx` (4 tests). | Por privacidad, se exige ingresar el número de DNI/RUC del remitente o destinatario para acceder al historial detallado. |
| **RF-09** | **Billetera Digital de QR** | **Implementado** | • `POST /api/v1/publico/billetera` con búsqueda por índice de hash SHA-256.<br>• Renderizado al vuelo de imagen PNG Base64 de QR2.<br>• Página `/billetera` extrayendo el token estrictamente desde `location.hash` (`#<token>`).<br>• Garantía de cero almacenamiento en `localStorage`, cookies o logs.<br>• Pruebas: `PublicoControllerTest`, `Billetera.test.tsx` (6 tests). | Si el cliente pierde el enlace con el fragmento hash, el token no puede recuperarse del servidor (diseño Zero-Knowledge). |
| **RF-10** | **Pre-registro Web y Cotización** | **Implementado** | • `POST /api/v1/publico/cotizacion` con desglose tarifario oficial.<br>• `POST /api/v1/publico/pre-registros` generando código `PRE-XXXXXX-PE` (vigencia 48 h).<br>• Admisión y precarga inmediata en Ventanilla (`GET /api/v1/preregistros/{codigo}`).<br>• Pruebas en `PublicoControllerTest` (17 tests) y `ValidadorVentanillaTest`. | Los endpoints de backend y el consumo en la app de escritorio están al 100%; la pantalla React en portal web forma parte de la tarea `W3` congelada. |
| **RF-11** | **Comprobantes Web (PDF)** | **Parcial** | • Tablas `comprobantes_pago` y `archivos_pdf_comprobante` en base de datos.<br>• Generador de ticket térmico físico de 58 mm (`salida/ticket_*.png`) con desglose de importes y QR1.<br>• Comprobante digital visual en Billetera Digital. | La generación de archivos PDF binarios para descarga directa en navegador (`B3`, `W5`) quedó planificada para la siguiente fase. |

---

## 3. Requisitos No Funcionales (RNF)

| Requisito | Denominación | Estado | Evidencia Técnica en el Código | Limitaciones y Criterios |
|---|---|:---:|---|---|
| **RNF-01** | **Seguridad: RBAC y Cero Conocimiento de QR2** | **Implementado** | • Firma JWT segura de 256 bits (`app.jwt.secreto`).<br>• En BD y SQLite local solo se persisten hashes SHA-256 de QR2.<br>• Prueba `SeguridadCeroQr2EnSqliteTest` escanea bytes brutos del archivo `.db` verificando cero apariciones del token.<br>• Filtro `FiltroRateLimitingPublico` con límite de 60 req/min y respuesta HTTP 429. | Requiere configurar `JWT_SECRET` seguro en entornos de producción. |
| **RNF-02** | **Rendimiento y Conmutación Offline ($\le 1000$ ms y $\le 2$ s)** | **Implementado** | • Latencia de validación cruzada criptográfica en backend: $< 50$ ms (exigencia: $< 1000$ ms).<br>• Conmutación a modo sin conexión en escritorio en $\le 2$ segundos con timeouts acotados (`ServicioConexion.java`).<br>• Procesamiento masivo de 50 ítems en almacén en ~330 ms.<br>• Pruebas: `DeteccionCaidaServidorTest`, `AlmacenControllerTest`. | Verificado en red local y entornos de laboratorio. |
| **RNF-03** | **Usabilidad: Cero Clics en Escaneo Continuo** | **Implementado** | • Componente personalizado `CampoEscaneo.java` en JavaFX.<br>• Auto-enfoque persistente, detección automática de tecla `Enter` enviada por pistolas USB.<br>• Limpieza inmediata tras cada escaneo sin requerir uso del mouse.<br>• Pruebas: `LoteEscaneoTest`. | El escáner de código de barras USB debe estar configurado con sufijo Enter (`CR`). |
| **RNF-04** | **Compatibilidad y Portabilidad** | **Implementado** | • Portal Web responsivo (React 18 + Tailwind CSS), verificado en navegadores modernos y móviles.<br>• Aplicación de escritorio JavaFX compatible con Windows, Linux y macOS bajo JDK 21.<br>• Scripts de despliegue y demo en `.ps1` (PowerShell) y `.sh` (Bash). | El empaquetador `.EXE` con `jpackage` (`O4`) se mantiene como opción de distribución futura; se ejecuta mediante Maven o JAR. |
| **RNF-05** | **Disponibilidad y Resiliencia (Modo Offline-First)** | **Implementado** | • Ventanilla 100% operativa sin conexión a internet ni servidor central (`SQLiteColaAdapter`).<br>• Sincronización automática periódica (cada 10 s) y reactiva a reconexión (`SincronizadorOffline.java`).<br>• Backoff exponencial ($2^{\min(n,5)}$ s) y tolerancia a fallos transitorios.<br>• Pruebas: `SincronizadorOfflineTest`, `SQLiteColaAdapterTest`. | Las operaciones de almacén y entrega física requieren conexión central para garantizar la unicidad del inventario global. |

---

## 4. Tareas Congeladas en Fase B (Trabajo Futuro)

Para mantener la integridad, estabilidad y el alcance acordado en la entrega de la **versión v0.2.0**, se declaran formalmente congeladas las siguientes tareas secundarias:

1. **`B1` — Dashboard Backend y Empleados con Auditoría**: Endpoints para agregación masiva de estadísticas y reportes gerenciales.
2. **`B2` — Flota y Rutas**: Asignación dinámica de unidades y ciclo de viaje con actualización masiva por choferes.
3. **`B3` — Microservicio de Comprobante PDF**: Generación binaria de PDFs con OpenPDF para `comprobantes_pago`.
4. **`D4` — Pantallas de Administración en Escritorio**: Vistas gráficas en JavaFX para el rol TN3 Administrador.
5. **`H1` — Adaptador ESC/POS para Ticketera USB de 58 mm**: Conexión por puerto serie/USB directa al hardware físico (actualmente soportado vía emulación gráfica en PNG).
6. **`H2` — Integración directa con escáner óptico USB vía API nativa** (actualmente soportado vía emulación de teclado HID con `CampoEscaneo`).
7. **`O3` — Despliegue en VPS de Producción con HTTPS**.
8. **`O4` — Instalador .EXE autocontenido con `jpackage`**.
