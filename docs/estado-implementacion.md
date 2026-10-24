# Matriz de Estado de Implementación — SGL-DQR (Expreso Marvisur)

Fecha de corte: **23 de octubre de 2026**  
Versión del sistema: **v0.2.1 (Fase B — Pre-registro y Cotización Operativos)**  
Metodología: **Auditoría técnica honesta basada en código fuente real, endpoints existentes, pantallas UI y pruebas automatizadas.**

---

## 1. Resumen de Pruebas Automatizadas

El sistema cuenta con un total de **263 pruebas automatizadas**, todas en estado **VERDE (0 fallos, 0 errores, 0 omitidas)**:

| Módulo / Proyecto | Tecnologías | Pruebas | Estado |
|---|---|:---:|:---:|
| `marvisur-core` | Java 21, JUnit 5, Mockito, ArchUnit | **50** | **Verde** |
| `marvisur-api` | Spring Boot 3.3, Testcontainers (PostgreSQL 16), Flyway | **104** | **Verde** |
| `marvisur-escritorio` | JavaFX 21, SQLite JDBC, HttpServer Mock | **76** | **Verde** |
| `portal-web` | React 18, Vite, TypeScript, Vitest, Testing Library | **33** | **Verde** |
| **TOTAL** | **Multiplataforma** | **263** | **100% VERDE** |

---

## 2. Requisitos Funcionales (RF)

| Requisito | Denominación Oficial | Estado | Evidencia Real en Código | Limitaciones y Alcance Real |
|---|---|:---:|---|---|
| **RF-01** | **Autenticación interna por roles (RBAC); acceso del cliente final sin contraseña mediante tokens dinámicos.** | **Implementado** | • `POST /api/v1/auth/login` con JWT (HMAC-SHA256).<br>• Roles `TN1_RECEPCIONISTA`, `TN2_ALMACENERO`, `TN3_ADMINISTRADOR`, `TN4_CHOFER`.<br>• Pantalla JavaFX `ControladorLogin.java`, `SesionContexto.java`, `ModeloMenuRol.java`.<br>• Pruebas: `AuthControllerTest` (10 tests), `ModeloMenuRolTest` (5 tests). | Sin recuperación de contraseña por email ni 2FA. El token JWT en escritorio vive exclusivamente en memoria RAM. |
| **RF-02** | **Dashboard administrativo con KPIs operativos en tiempo real.** | **Pendiente** | • Ninguna. No existen endpoints de cálculo de KPIs agregados de agencia ni pantalla de dashboard administrativo en la app de escritorio o web. | Tarea `B1` (backend) y `D4` (escritorio) no fueron desarrolladas; quedan en el backlog. |
| **RF-03** | **Gestión de personal operativo (CRUD) con registro de auditoría.** | **Pendiente** | • Existen la tabla `empleados`, la entidad JPA `EmpleadoJpaEntity` y verificación de credenciales en login, pero **NO** existen endpoints REST para altas, bajas o modificaciones de empleados, ni interfaz gráfica para su gestión. | La gestión de empleados solo es posible mediante consultas SQL directas o datos semilla de migraciones. |
| **RF-04** | **Gestión del ciclo de vida y estado de pago de encomiendas, con bloqueo por pago "Pendiente".** | **Implementado** | • `POST /api/v1/encomiendas` (transacción atómica con rollback completo verificado ante fallos).<br>• Formulario JavaFX `VentanillaView.fxml` con validación DNI/RUC y cálculo de tarifa.<br>• Modo offline en ventanilla con SQLite (`VentanillaOfflineServicio`, `SQLiteColaAdapter`).<br>• Pruebas: `EncomiendaControllerTest`, `RegistroEncomiendaAtomicidadTest`. | La numeración de comprobantes (`MV-yyyyMMdd-XXXXXXXX`) y la fórmula tarifaria son provisionales (marcadas con `TODO`). No incluye anulación de envíos despachados. |
| **RF-05** | **Control de flota y asignación de rutas interprovinciales.** | **Parcial** | • Entidades JPA `Vehiculo`, `Ruta`, `ProgramacionViaje`.<br>• Endpoint `GET /api/v1/viajes` (solo lectura con filtros para elegir viaje en despacho).<br>• Despacho de encomiendas a viaje (`POST /api/v1/almacen/lotes/escaneo`, acción `DESPACHAR_A_VIAJE`) transiciona a `EN_RUTA`. | **Sin gestión de flota ni choferes**: no existen endpoints ni pantallas para crear/editar vehículos o asignar choferes dinámicamente (`B2` pendiente). |
| **RF-06** | **Módulo de almacén con procesamiento por lotes (Batch Scanning) y contingencia por DNI/RUC.** | **Implementado** | • `POST /api/v1/almacen/lotes/escaneo` (`RECEPCIONAR_ORIGEN`, `DESPACHAR_A_VIAJE`, `RECEPCIONAR_DESTINO`) con límite de 200 ítems y reporte individual.<br>• Pantalla Batch Scanning en `AlmacenView.fxml` con componente `CampoEscaneo`.<br>• Búsqueda por DNI/RUC (`GET /api/v1/almacen/encomiendas/buscar`) e incidencias (`POST /api/v1/almacen/incidencias`).<br>• Pruebas: `AlmacenControllerTest`, `LoteEscaneoTest`. | Máximo 200 ítems por lote para control de carga en servidor. |
| **RF-07** | **Motor Criptográfico de Doble QR: generación y validación cruzada de QR1/QR2.** | **Implementado** | • `MotorQR.java` con generación de Doble QR, hashes SHA-256 e invalidación post-entrega.<br>• `POST /api/v1/entregas/validar` con bloqueo pesimista `SELECT ... FOR UPDATE` sobre `tokens_qr2_estatus`.<br>• Bloqueo transaccional si Contra Entrega está pendiente (`POST /api/v1/encomiendas/{id}/cobro`).<br>• Pantalla de entrega física en `AlmacenView.fxml`.<br>• Pruebas: `EntregaControllerTest` (concurrencia de 10 hilos en PostgreSQL real), `MotorQRTest`. | El QR2 en claro se expone solo una vez al registrar; tras la entrega queda invalidado irreversiblemente. |
| **RF-08** | **Portal web público de tracking y trazabilidad histórica.** | **Implementado** | • `GET /api/v1/publico/rastreo?codigo=...&documento=...` (Fricción Cero).<br>• Portal web `/rastreo` en React con línea de tiempo vertical, nombres anonimizados (`JUAN P***`) y sin persistencia en `localStorage`.<br>• Pruebas: `PublicoControllerTest`, `Rastreo.test.tsx`, `LineaTiempo.test.tsx`. | Requiere ingresar documento de identidad por motivos de privacidad para ver el detalle del historial. |
| **RF-09** | **Billetera Digital del código QR2 para el destinatario.** | **Implementado** | • `POST /api/v1/publico/billetera` con búsqueda por hash SHA-256.<br>• Portal web `/billetera` extrayendo el token digital **exclusivamente desde `location.hash` (`#<token>`)**.<br>• Renderizado PNG Base64 de QR2 de alta resolución y aviso de confidencialidad.<br>• Pruebas: `PublicoControllerTest`, `Billetera.test.tsx`. | Si el cliente extravía el enlace con el fragmento hash, el token no puede recuperarse del servidor (diseño Zero-Knowledge). |
| **RF-10** | **Pre-registro web de encomiendas ("Código Express") con cotización dinámica.** | **Implementado** | • Rutas del portal web `/cotiza` y `/pre-registro` (asistente de 3 pasos, renderizado de código QR con `qrcode`, botones de impresión limpia y compartición en WhatsApp).<br>• Backend API operativo: `POST /api/v1/publico/cotizacion` y `POST /api/v1/publico/pre-registros` (código `PRE-XXXXXX-PE`, vigencia 48 h).<br>• Escritorio operativo: admisión y precarga con Código Express en Ventanilla (`GET /api/v1/preregistros/{codigo}`).<br>• Pruebas: `Cotiza.test.tsx` (4 tests), `PreRegistro.test.tsx` (6 tests), `cliente.test.ts` (8 tests), `PublicoControllerTest` y `ValidadorVentanillaTest`. | El código express expira a las 48 horas si no se deposita en ventanilla. La tarifa web es referencial y se confirma con pesaje físico oficial. |
| **RF-11** | **Descarga autónoma de comprobantes electrónicos en PDF.** | **Pendiente** | • Ninguna. Tablas `comprobantes_pago` y `archivos_pdf_comprobante` existen en el esquema Flyway, pero **NO** existe generación ni descarga de PDFs (no hay microservicio de PDF ni endpoint ni pantalla web). | La generación de ticket térmico PNG simulado en ventanilla es una emulación de impresión local, **NO** satisface el RF-11 de comprobantes en PDF. Tarea `B3`/`W5` en backlog. |

---

## 3. Requisitos No Funcionales (RNF) — Definiciones Oficiales

| Requisito | Definición Oficial | Estado | Evidencia Real en Código | Limitaciones y Declaración Honesta |
|---|---|:---:|---|---|
| **RNF-01** | **Seguridad**:<br>*Comunicaciones cifradas HTTPS/SSL; el hash del QR2 se invalida en <= 500 ms tras la validación cruzada.* | **Parcial** | • El hash del QR2 y su invalidación atómica están probados en BD (`tokens_qr2_estatus.estado = 'INVALIDADO'`) mediante `SELECT ... FOR UPDATE` en `EntregaApiServicio.java`.<br>• Verificado en `EntregaControllerTest` (concurrencia de 10 hilos). | **HTTPS/SSL NO está configurado**: el sistema opera sobre HTTP plano sin certificados TLS/SSL (tarea `O3` pendiente). |
| **RNF-02** | **Fiabilidad y contingencia offline**:<br>*Conmutación automática a base local (SQLite/H2) en <= 2 s ante caída de red; uptime operativo 100% en ventanilla.* | **Parcial** | • Conmutación a modo sin conexión en $\le 2$ s con timeouts acotados (`ServicioConexion.java`), probada con servidor simulado en `DeteccionCaidaServidorTest`.<br>• Emisión offline en ventanilla guardando en SQLite (`SQLiteColaAdapter`) y sincronización automática cableada en UI (`SincronizadorOffline`, `ControladorPrincipal`).<br>• Indicador reactivo en cabecera UI («Sin conexión», «Pendientes: N», «Sincronizando…», «Sincronizado»), vista de «Cola Local» y verificación en `SincronizacionCableadoUITest`. | **Solo el REGISTRO en ventanilla funciona sin conexión**. Entrega física, escaneo de lotes en almacén y contingencia muestran «Requiere conexión» y bloquean operaciones sin conexión activa con el servidor central. |
| **RNF-03** | **Rendimiento**:<br>*Latencia máxima 1000 ms en la validación cruzada y en actualizaciones por lote, con hasta 50 lecturas ópticas por minuto.* | **Parcial** | • Prueba automatizada de 50 validaciones cruzadas secuenciales con latencia individual $< 1000$ ms en `EntregaControllerTest`. | **No hay prueba de carga sostenida de 50 lecturas ópticas por minuto**. Se comprobó la ejecución secuencial de 50 peticiones unitarias, pero no un benchmark de estrés continuo con hardware lector a ese ritmo. |
| **RNF-04** | **Portabilidad e instalación**:<br>*Cliente web responsivo sin descargas; App de escritorio como instalador autocontenido (.EXE/.MSI) con JRE embebido (jpackage).* | **Parcial** | • Portal web responsivo en React 18 + Tailwind CSS (mobile-first), verificado en navegadores modernos sin instalación ni descargas. | **La app de escritorio NO cuenta con instalador autocontenido (.EXE/.MSI) ni JRE embebido mediante `jpackage`** (tarea `O4` pendiente); se ejecuta mediante `mvn javafx:run` o JAR con dependencia de JDK 21 preinstalado. |
| **RNF-05** | **Mantenibilidad**:<br>*Arquitectura Hexagonal; lógica de negocio desacoplada de los frameworks.* | **Implementado** | • Módulo `marvisur-core` desarrollado en Java 21 puro sin dependencias de frameworks externos (sin Spring, JPA ni JavaFX).<br>• Total de 50 pruebas en verde en `marvisur-core`: 45 pruebas unitarias de dominio y aplicación, y 5 pruebas de reglas arquitecturales verificadas por ArchUnit (`ArquitecturaHexagonalTest`). | Cumplimiento estricto verificado en compilación continua. |

---

## 4. Limitaciones Conocidas

1. **Hardware simulado (sin dispositivos físicos)**:
   - La ticketera térmica de 58 mm está emulada en software (`ImpresionSimuladaAdapter`) generando imágenes PNG en `salida/ticket_*.png`. No se conecta a impresoras físicas por puerto serie o USB/ESC-POS (tarea `H1` pendiente).
   - El escáner óptico USB opera mediante emulación de teclado con el componente `CampoEscaneo`; no hay integración directa con APIs nativas de escáner USB (tarea `H2` pendiente).
2. **Ausencia de HTTPS/SSL**:
   - Todo el tráfico cliente-servidor (escritorio y portal web hacia Spring Boot) utiliza HTTP en texto plano. No se han aprovisionado certificados TLS ni proxy inverso (tarea `O3` pendiente).
3. **Sin instalador autocontenido**:
   - La aplicación de escritorio JavaFX requiere un entorno con Maven y JDK 21 configurado en la máquina host. No se generaron instaladores `.EXE` ni `.MSI` mediante `jpackage` (tarea `O4` pendiente).
4. **Tarifa y numeración de comprobantes provisionales**:
   - La lógica tarifaria por peso (`TarifarioPorPeso.java`) y el formato de comprobante (`MV-yyyyMMdd-XXXXXXXX`) son referenciales y contienen marcas `TODO`, pendientes de confirmación contra las tablas comerciales y numeración fiscal de Expreso Marvisur.
5. **Fuera de alcance del proyecto**:
   - Integración técnica con la API de SUNAT (emisión electrónica de Guías de Remisión en formato XML con constancia CDR).
   - Aplicaciones móviles nativas para choferes o clientes (Android / iOS).
