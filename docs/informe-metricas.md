# Informe Técnico de Métricas del Sistema — SGL-DQR

- **Fecha de Generación**: 2026-10-25 17:59:09 -05:00
- **Hash de Commit**: `5820bad` (completo: `5820bade23edf874e364d5d00fd0041f3978d419`)
- **Rama Actual**: `fix/cierre-contratos-informe`
- **Generado por**: `scripts/informe-metricas.ps1` (extracción automática en vivo)

---

## 1. Resumen Ejecutivo de Métricas Clave

| Métrica | Valor Real | Observación |
|---|:---:|---|
| **Total de Pruebas Automatizadas** | **466** | Core (50), API (216), JavaFX (163), Web (37) |
| **Pruebas Fallidas / Errores** | **0 / 0** | **100% en verde** |
| **Operaciones REST (OpenAPI)** | **38** | 31 internas, 7 públicas |
| **Migraciones Flyway Oficiales** | **8** | V1 a V8 en `database/postgres/migrations` |
| **Tablas en Base de Datos** | **18** | Coincidencia 100% con las 18 tablas del Diccionario |
| **Total de Commits Git** | **145** | Primer commit: 2026-10-19 (0801caa) - chore: inicializar estructura base del repositorio SGL-DQR |
| **Líneas de Código Fuente (LOC)** | **40551** | 425 archivos (Java, TypeScript, SQL sin dependencias) |

---

## 2. Detalle de Pruebas Automatizadas por Módulo y Clase

### 2.1. marvisur-core (50 pruebas · 2.128 s)
- **Pruebas Unitarias de Dominio y Casos de Uso**: 45 pruebas
- **Pruebas de Reglas de Arquitectura Hexagonal (ArchUnit)**: 5 reglas verificadas

| Clase de Prueba | Pruebas | Fallos | Errores | Omitidas | Tiempo (s) | Tipo |
|---|:---:|:---:|:---:|:---:|:---:|---|
| CambiarEstadoServiceTest | 4 | 0 | 0 | 0 | 0.788 | Unitaria Dominio |
| RegistrarEncomiendaServiceTest | 3 | 0 | 0 | 0 | 0.076 | Unitaria Dominio |
| TarifarioPorPesoTest | 4 | 0 | 0 | 0 | 0.006 | Unitaria Dominio |
| ArquitecturaHexagonalTest | 5 | 0 | 0 | 0 | 0.955 | ArchUnit (Hexagonal) |
| AuditoriaYTrackingTest | 2 | 0 | 0 | 0 | 0.012 | Unitaria Dominio |
| EncomiendaTest | 7 | 0 | 0 | 0 | 0.013 | Unitaria Dominio |
| AlmacenYViajesModelosTest | 5 | 0 | 0 | 0 | 0.012 | Unitaria Dominio |
| MapeadorEventoTrackingTest | 6 | 0 | 0 | 0 | 0.075 | Unitaria Dominio |
| PreRegistroExpressTest | 3 | 0 | 0 | 0 | 0.005 | Unitaria Dominio |
| MotorQRTest | 8 | 0 | 0 | 0 | 0.03 | Unitaria Dominio |
| ZXingAdapterTest | 3 | 0 | 0 | 0 | 0.156 | Unitaria Dominio |

### 2.2. marvisur-api (216 pruebas · 108.989 s)
De las 27 clases de prueba del módulo, 10 clases ejecutan integración contra PostgreSQL 16 real con Testcontainers (@Testcontainers / @DynamicPropertySource), 8 clases contra base de datos en memoria H2 (modo compatibilidad PostgreSQL) y 9 clases de pruebas unitarias sin base de datos. Incluye pruebas de concurrencia de 10 hilos simultáneos, rollback transaccional atómico y control RBAC.

| Clase de Prueba | Pruebas | Fallos | Errores | Omitidas | Tiempo (s) |
|---|:---:|:---:|:---:|:---:|:---:|
| ArranqueContextoSpringTest | 1 | 0 | 0 | 0 | 16.618 |
| OpenApiExportTest | 2 | 0 | 0 | 0 | 18.493 |
| AlmacenControllerTest | 9 | 0 | 0 | 0 | 3.989 |
| AuditoriaControllerTest | 11 | 0 | 0 | 0 | 5.44 |
| ComprobantePdfIntegracionTest | 5 | 0 | 0 | 0 | 5.866 |
| DashboardControllerTest | 7 | 0 | 0 | 0 | 5.039 |
| EmpleadoControllerTest | 12 | 0 | 0 | 0 | 4.934 |
| EncomiendaControllerTest | 8 | 0 | 0 | 0 | 0.496 |
| EntregaControllerTest | 8 | 0 | 0 | 0 | 5.534 |
| PublicoControllerTest | 17 | 0 | 0 | 0 | 0.63 |
| SyncControllerTest | 9 | 0 | 0 | 0 | 4.786 |
| VehiculoControllerTest | 7 | 0 | 0 | 0 | 4.278 |
| ViajeControllerTest | 12 | 0 | 0 | 0 | 4.835 |
| DevSeedFlujoDemoTest | 9 | 0 | 0 | 0 | 4.913 |
| TrackingSerializacionTest | 3 | 0 | 0 | 0 | 0.01 |
| ManejadorGlobalExcepcionesTest | 6 | 0 | 0 | 0 | 1.133 |
| ComprobantePdfGeneradorTest | 3 | 0 | 0 | 0 | 0.914 |
| MigracionesFlywayTest | 8 | 0 | 0 | 0 | 9.143 |
| AuditoriaFiltroEspecificacionTest | 7 | 0 | 0 | 0 | 1.266 |
| EmpleadoFiltroEspecificacionTest | 7 | 0 | 0 | 0 | 0.085 |
| ViajeFiltroEspecificacionTest | 5 | 0 | 0 | 0 | 0.529 |
| PersistenciaPostgresTest | 12 | 0 | 0 | 0 | 1.043 |
| AutenticacionYRbacTest | 11 | 0 | 0 | 0 | 3.765 |
| CalculadorPeriodoDashboardTest | 5 | 0 | 0 | 0 | 0.183 |
| FlotaViajeServicioTest | 19 | 0 | 0 | 0 | 1.686 |
| RegistroEncomiendaAtomicidadTest | 2 | 0 | 0 | 0 | 3.319 |
| ValidadorEmpleadoTest | 0 | 0 | 0 | 0 | 0.033 |
| ValidadorEmpleadoTest$NormalizacionCorreoTests | 2 | 0 | 0 | 0 | 0.004 |
| ValidadorEmpleadoTest$PoliticaPasswordTests | 5 | 0 | 0 | 0 | 0.012 |
| ValidadorEmpleadoTest$ReglasSeguridadTests | 4 | 0 | 0 | 0 | 0.013 |

### 2.3. marvisur-escritorio (163 pruebas · 12.296 s)
Pruebas headless en JavaFX con mocks HTTP y base local SQLite. Valida conmutación offline en <= 2 s, backoff exponencial y cero persistencia de QR2 en disco.

| Clase de Prueba | Pruebas | Fallos | Errores | Omitidas | Tiempo (s) |
|---|:---:|:---:|:---:|:---:|:---:|
| RutasFlotaViewModelTest | 13 | 0 | 0 | 0 | 0.109 |
| ConsistenciaVistasFxmlTest | 1 | 0 | 0 | 0 | 0.095 |
| DashboardViewModelTest | 11 | 0 | 0 | 0 | 0.018 |
| DeteccionCaidaServidorTest | 2 | 0 | 0 | 0 | 0.232 |
| EmpleadosViewModelTest | 18 | 0 | 0 | 0 | 0.021 |
| EncabezadoConexionViewModelTest | 6 | 0 | 0 | 0 | 0.005 |
| EntregaFlujoViewModelTest | 6 | 0 | 0 | 0 | 0.802 |
| ImpresionSimuladaAdapterTest | 1 | 0 | 0 | 0 | 0.165 |
| LoginDemoFlagTest | 4 | 0 | 0 | 0 | 0.013 |
| LoteEscaneoTest | 9 | 0 | 0 | 0 | 0.014 |
| MarvisurHttpClientAlmacenTest | 14 | 0 | 0 | 0 | 2.494 |
| MarvisurHttpClientDashboardTest | 4 | 0 | 0 | 0 | 2.131 |
| MarvisurHttpClientEmpleadosTest | 9 | 0 | 0 | 0 | 2.286 |
| MarvisurHttpClientFlotaTest | 13 | 0 | 0 | 0 | 2.208 |
| MarvisurHttpClientTest | 9 | 0 | 0 | 0 | 0.129 |
| ModeloMenuRolTest | 5 | 0 | 0 | 0 | 0.005 |
| PestanasBloqueadasOfflineViewModelTest | 2 | 0 | 0 | 0 | 0.004 |
| PoliticaActualizacionDashboardTest | 3 | 0 | 0 | 0 | 0.002 |
| RegistroOfflineCriptograficoTest | 1 | 0 | 0 | 0 | 0.333 |
| RutasEscritorioTest | 6 | 0 | 0 | 0 | 0.039 |
| SeguridadAlmacenQrTest | 2 | 0 | 0 | 0 | 0.009 |
| SeguridadCeroQr2EnSqliteTest | 1 | 0 | 0 | 0 | 0.152 |
| SeguridadQr2TokenTest | 2 | 0 | 0 | 0 | 0.039 |
| SincronizacionCableadoUITest | 5 | 0 | 0 | 0 | 0.372 |
| SincronizadorOfflineTest | 5 | 0 | 0 | 0 | 0.271 |
| SQLiteColaAdapterTest | 6 | 0 | 0 | 0 | 0.337 |
| ValidadorVentanillaTest | 5 | 0 | 0 | 0 | 0.011 |

### 2.4. portal-web (37 pruebas · 11.891 s)
Ejecutadas con Vitest y React Testing Library. Valida aislamiento de hash #, tracking público y manejo de errores RFC 7807.

| Archivo de Prueba | Pruebas | Fallos | Tiempo (s) |
|---|:---:|:---:|:---:|
| Billetera.test.tsx | 6 | 0 | 0.103 |
| cliente.test.ts | 8 | 0 | 0.006 |
| Cotiza.test.tsx | 4 | 0 | 0.896 |
| LineaTiempo.test.tsx | 5 | 0 | 0.043 |
| PreRegistro.test.tsx | 6 | 0 | 7.028 |
| Rastreo.test.tsx | 8 | 0 | 3.815 |

---

## 3. Catálogo de Endpoints REST (OpenAPI 3)

### 3.1. Operaciones por Grupo
| Grupo / Módulo | Operaciones | Especificación |
|---|:---:|---|
| Auditoría del Sistema | 1 | Interno |
| Autenticación y Sesión | 2 | Interno |
| Dashboard Administrativo | 1 | Interno |
| Encomiendas y Doble QR | 6 | Interno |
| Entregas y Validación Cruzada | 1 | Interno |
| Flota de Vehículos | 2 | Interno |
| Módulo de Almacén | 4 | Interno |
| Personal y Empleados | 6 | Interno |
| Portal Web Público | 7 | Público |
| Pre-Registros Ventanilla | 1 | Interno |
| Roles del Sistema | 1 | Interno |
| Rutas Interprovinciales | 1 | Interno |
| Sincronización Offline | 1 | Interno |
| Viajes y Flota | 4 | Interno |

### 3.2. Listado Completo de Operaciones
| Método | Ruta | Grupo | Operación (ID) |
|---|---|---|---|
| `GET` | `/api/v1/almacen/encomiendas/buscar` | Módulo de Almacén | buscarEncomiendas |
| `POST` | `/api/v1/almacen/incidencias` | Módulo de Almacén | registrarIncidencia |
| `GET` | `/api/v1/almacen/incidencias` | Módulo de Almacén | listarIncidencias |
| `POST` | `/api/v1/almacen/lotes/escaneo` | Módulo de Almacén | procesarLoteEscaneo |
| `GET` | `/api/v1/auditoria` | Auditoría del Sistema | listarAuditoria |
| `POST` | `/api/v1/auth/login` | Autenticación y Sesión | login |
| `GET` | `/api/v1/auth/me` | Autenticación y Sesión | me |
| `GET` | `/api/v1/dashboard/resumen` | Dashboard Administrativo | obtenerResumen |
| `POST` | `/api/v1/empleados` | Personal y Empleados | crearEmpleado |
| `GET` | `/api/v1/empleados` | Personal y Empleados | listarEmpleados |
| `PUT` | `/api/v1/empleados/{id}` | Personal y Empleados | actualizarEmpleado |
| `GET` | `/api/v1/empleados/{id}` | Personal y Empleados | obtenerPorId |
| `PATCH` | `/api/v1/empleados/{id}/estado` | Personal y Empleados | cambiarEstado |
| `POST` | `/api/v1/empleados/{id}/password` | Personal y Empleados | restablecerPassword |
| `POST` | `/api/v1/encomiendas` | Encomiendas y Doble QR | registrarEncomienda |
| `GET` | `/api/v1/encomiendas` | Encomiendas y Doble QR | listarEncomiendas |
| `GET` | `/api/v1/encomiendas/{id}` | Encomiendas y Doble QR | consultarPorId |
| `POST` | `/api/v1/encomiendas/{id}/cobro` | Encomiendas y Doble QR | registrarCobro |
| `GET` | `/api/v1/encomiendas/{id}/comprobante.pdf` | Encomiendas y Doble QR | descargarComprobantePdf |
| `GET` | `/api/v1/encomiendas/{id}/qr1.png` | Encomiendas y Doble QR | obtenerImagenQr1 |
| `POST` | `/api/v1/entregas/validar` | Entregas y Validación Cruzada | validarEntrega |
| `GET` | `/api/v1/preregistros/{codigoExpress}` | Pre-Registros Ventanilla | consultarPorCodigo |
| `POST` | `/api/v1/publico/billetera` | Portal Web Público | consultarBilletera |
| `GET` | `/api/v1/publico/comprobante.pdf` | Portal Web Público | descargarComprobantePdf |
| `POST` | `/api/v1/publico/cotizacion` | Portal Web Público | cotizar |
| `GET` | `/api/v1/publico/ping` | Portal Web Público | ping |
| `POST` | `/api/v1/publico/pre-registros` | Portal Web Público | crearPreRegistro |
| `GET` | `/api/v1/publico/rastreo` | Portal Web Público | consultarRastreo |
| `GET` | `/api/v1/publico/sucursales` | Portal Web Público | listarSucursales |
| `GET` | `/api/v1/roles` | Roles del Sistema | listarRoles |
| `GET` | `/api/v1/rutas` | Rutas Interprovinciales | listarRutas |
| `POST` | `/api/v1/sync/encomiendas` | Sincronización Offline | sincronizarEncomiendas |
| `GET` | `/api/v1/vehiculos` | Flota de Vehículos | listarVehiculos |
| `POST` | `/api/v1/vehiculos` | Flota de Vehículos | crearVehiculo |
| `GET` | `/api/v1/viajes` | Viajes y Flota | listarViajes |
| `POST` | `/api/v1/viajes` | Viajes y Flota | crearViaje |
| `GET` | `/api/v1/viajes/{id}/encomiendas` | Viajes y Flota | obtenerManifiesto |
| `PATCH` | `/api/v1/viajes/{id}/estado` | Viajes y Flota | cambiarEstadoViaje |

---

## 4. Estructura de Base de Datos y Persistencia

### 4.1. Migraciones Flyway (8 archivos)
| Versión | Archivo de Migración |
|---|---|
| V1 | `V1__modulo2_sucursales_flotas_rutas.sql` |
| V2 | `V2__modulo1_seguridad_personal_auditoria.sql` |
| V3 | `V3__modulo3_clientes_preregistros.sql` |
| V4 | `V4__modulo4_encomiendas_pagos_almacen.sql` |
| V5 | `V5__modulo5_doble_qr_tokens.sql` |
| V6 | `V6__modulo6_tracking_comprobantes.sql` |
| V7 | `V7__datos_iniciales.sql` |
| V8 | `V8__indice_hash_qr2.sql` |

### 4.2. Tablas Implementadas (18 de las 18 del Diccionario)
| N° | Nombre de Tabla | Módulo Diccionario |
|:---:|---|---|
| 1 | `archivos_pdf_comprobante` | 18 tablas oficiales |
| 2 | `auditoria_logs` | 18 tablas oficiales |
| 3 | `clientes` | 18 tablas oficiales |
| 4 | `comprobantes_pago` | 18 tablas oficiales |
| 5 | `despachos_lote_detalles` | 18 tablas oficiales |
| 6 | `empleados` | 18 tablas oficiales |
| 7 | `encomiendas` | 18 tablas oficiales |
| 8 | `historial_tracking` | 18 tablas oficiales |
| 9 | `incidencias_emergencia` | 18 tablas oficiales |
| 10 | `parejas_doble_qr` | 18 tablas oficiales |
| 11 | `pre_registros_express` | 18 tablas oficiales |
| 12 | `programacion_viajes` | 18 tablas oficiales |
| 13 | `roles` | 18 tablas oficiales |
| 14 | `rutas` | 18 tablas oficiales |
| 15 | `sucursales` | 18 tablas oficiales |
| 16 | `tokens_qr2_estatus` | 18 tablas oficiales |
| 17 | `validaciones_cruzadas_logs` | 18 tablas oficiales |
| 18 | `vehiculos` | 18 tablas oficiales |

---

## 5. Métricas de Git y Control de Versiones

- **Total de Commits**: 145
- **Primer Commit**: 2026-10-19 (0801caa) - chore: inicializar estructura base del repositorio SGL-DQR
- **Último Commit**: 2026-10-25 (5820bad) - chore(cierre): preparacion de cierre de version v0.3.0

### Distribución por Convención de Commits:
| Tipo de Commit | Cantidad | Descripción |
|---|:---:|---|
| `feat` | 56 | Nuevas funcionalidades operativas y de integración |
| `chore` | 13 | Mantenimiento, CI, dependencias y configuración |
| `docs` | 17 | Documentación técnica, matrices y manuales |
| `fix` | 29 | Corrección de defectos y deuda técnica |
| `merge` | 26 | Integraciones de ramas hacia develop y main |
| `test` | 2 | Pruebas automatizadas directas |
| Otros | 2 | Ajustes varios |

---

## 6. Líneas de Código Fuente (LOC)

*Nota: Se excluyen carpetas `target/`, `node_modules/`, `dist/`, `.git/` y respaldos `*.bak`.*

| Módulo / Carpeta | Lenguaje Principal | Archivos | Líneas de Código (LOC) |
|---|---|:---:|:---:|
| `backend-marvisur/marvisur-core` | Java 21 | 67 | 2983 |
| `backend-marvisur/marvisur-api` | Java 21 | 207 | 19331 |
| `frontend-marvisur/escritorio` | JavaFX 21 (Java) | 117 | 13685 |
| `frontend-marvisur/portal-web` | TypeScript / React | 25 | 4018 |
| `database` | PostgreSQL (SQL) | 9 | 534 |
| **TOTAL SISTEMA** | **Multi-lenguaje** | **425** | **40551** |

---

*Informe generado automáticamente por `scripts/informe-metricas.ps1` para auditoría técnica.*