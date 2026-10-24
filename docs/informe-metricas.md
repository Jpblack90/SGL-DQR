# Informe Técnico de Métricas del Sistema — SGL-DQR

- **Fecha de Generación**: 2026-10-23 20:43:26 -05:00
- **Hash de Commit**: `f861a4c` (completo: `f861a4c98eb33c2dfd195fea556cc47f65577caa`)
- **Rama Actual**: `chore/cierre-v0.2.1`
- **Generado por**: `scripts/informe-metricas.ps1` (extracción automática en vivo)

---

## 1. Resumen Ejecutivo de Métricas Clave

| Métrica | Valor Real | Observación |
|---|:---:|---|
| **Total de Pruebas Automatizadas** | **263** | Core (50), API (104), JavaFX (76), Web (33) |
| **Pruebas Fallidas / Errores** | **0 / 0** | **100% en verde** |
| **Operaciones REST (OpenAPI)** | **21** | 15 internas, 6 públicas |
| **Migraciones Flyway Oficiales** | **8** | V1 a V8 en `database/postgres/migrations` |
| **Tablas en Base de Datos** | **18** | Coincidencia 100% con las 18 tablas del Diccionario |
| **Total de Commits Git** | **95** | Primer commit: 2026-10-19 (0801caa) - chore: inicializar estructura base del repositorio SGL-DQR |
| **Líneas de Código Fuente (LOC)** | **26475** | 314 archivos (Java, TypeScript, SQL sin dependencias) |

---

## 2. Detalle de Pruebas Automatizadas por Módulo y Clase

### 2.1. marvisur-core (50 pruebas · 2.045 s)
- **Pruebas Unitarias de Dominio y Casos de Uso**: 45 pruebas
- **Pruebas de Reglas de Arquitectura Hexagonal (ArchUnit)**: 5 reglas verificadas

| Clase de Prueba | Pruebas | Fallos | Errores | Omitidas | Tiempo (s) | Tipo |
|---|:---:|:---:|:---:|:---:|:---:|---|
| CambiarEstadoServiceTest | 4 | 0 | 0 | 0 | 0.769 | Unitaria Dominio |
| RegistrarEncomiendaServiceTest | 3 | 0 | 0 | 0 | 0.073 | Unitaria Dominio |
| TarifarioPorPesoTest | 4 | 0 | 0 | 0 | 0.005 | Unitaria Dominio |
| ArquitecturaHexagonalTest | 5 | 0 | 0 | 0 | 0.926 | ArchUnit (Hexagonal) |
| AuditoriaYTrackingTest | 2 | 0 | 0 | 0 | 0.013 | Unitaria Dominio |
| EncomiendaTest | 7 | 0 | 0 | 0 | 0.01 | Unitaria Dominio |
| AlmacenYViajesModelosTest | 5 | 0 | 0 | 0 | 0.011 | Unitaria Dominio |
| MapeadorEventoTrackingTest | 6 | 0 | 0 | 0 | 0.067 | Unitaria Dominio |
| PreRegistroExpressTest | 3 | 0 | 0 | 0 | 0.005 | Unitaria Dominio |
| MotorQRTest | 8 | 0 | 0 | 0 | 0.029 | Unitaria Dominio |
| ZXingAdapterTest | 3 | 0 | 0 | 0 | 0.137 | Unitaria Dominio |

### 2.2. marvisur-api (104 pruebas · 40.274 s)
Incluye integración con Testcontainers (PostgreSQL 16 real), pruebas de concurrencia de 10 hilos simultáneos, rollback transaccional atómico y control RBAC.

| Clase de Prueba | Pruebas | Fallos | Errores | Omitidas | Tiempo (s) |
|---|:---:|:---:|:---:|:---:|:---:|
| OpenApiExportTest | 2 | 0 | 0 | 0 | 16.755 |
| AlmacenControllerTest | 9 | 0 | 0 | 0 | 2.879 |
| EncomiendaControllerTest | 8 | 0 | 0 | 0 | 0.559 |
| EntregaControllerTest | 8 | 0 | 0 | 0 | 7.418 |
| PublicoControllerTest | 17 | 0 | 0 | 0 | 0.27 |
| SyncControllerTest | 9 | 0 | 0 | 0 | 3.46 |
| DevSeedFlujoDemoTest | 9 | 0 | 0 | 0 | 3.579 |
| TrackingSerializacionTest | 3 | 0 | 0 | 0 | 0.006 |
| ManejadorGlobalExcepcionesTest | 6 | 0 | 0 | 0 | 0.365 |
| MigracionesFlywayTest | 8 | 0 | 0 | 0 | 2.193 |
| PersistenciaPostgresTest | 12 | 0 | 0 | 0 | 0.191 |
| AutenticacionYRbacTest | 11 | 0 | 0 | 0 | 1.147 |
| RegistroEncomiendaAtomicidadTest | 2 | 0 | 0 | 0 | 1.452 |

### 2.3. marvisur-escritorio (76 pruebas · 5.594 s)
Pruebas headless en JavaFX con mocks HTTP y base local SQLite. Valida conmutación offline en <= 2 s, backoff exponencial y cero persistencia de QR2 en disco.

| Clase de Prueba | Pruebas | Fallos | Errores | Omitidas | Tiempo (s) |
|---|:---:|:---:|:---:|:---:|:---:|
| DeteccionCaidaServidorTest | 2 | 0 | 0 | 0 | 0.279 |
| EncabezadoConexionViewModelTest | 6 | 0 | 0 | 0 | 0.012 |
| EntregaFlujoViewModelTest | 6 | 0 | 0 | 0 | 0.765 |
| ImpresionSimuladaAdapterTest | 1 | 0 | 0 | 0 | 0.14 |
| LeerDbRealTest | 1 | 0 | 0 | 0 | 0.156 |
| LoteEscaneoTest | 4 | 0 | 0 | 0 | 0.025 |
| MarvisurHttpClientAlmacenTest | 13 | 0 | 0 | 0 | 2.479 |
| MarvisurHttpClientTest | 9 | 0 | 0 | 0 | 0.166 |
| ModeloMenuRolTest | 5 | 0 | 0 | 0 | 0.01 |
| PestanasBloqueadasOfflineViewModelTest | 2 | 0 | 0 | 0 | 0.008 |
| RegistroOfflineCriptograficoTest | 1 | 0 | 0 | 0 | 0.376 |
| SeguridadAlmacenQrTest | 2 | 0 | 0 | 0 | 0.018 |
| SeguridadCeroQr2EnSqliteTest | 1 | 0 | 0 | 0 | 0.104 |
| SeguridadQr2TokenTest | 2 | 0 | 0 | 0 | 0.022 |
| SincronizacionCableadoUITest | 5 | 0 | 0 | 0 | 0.479 |
| SincronizadorOfflineTest | 5 | 0 | 0 | 0 | 0.228 |
| SQLiteColaAdapterTest | 6 | 0 | 0 | 0 | 0.314 |
| ValidadorVentanillaTest | 5 | 0 | 0 | 0 | 0.013 |

### 2.4. portal-web (33 pruebas · 9.606 s)
Ejecutadas con Vitest y React Testing Library. Valida aislamiento de hash #, tracking público y manejo de errores RFC 7807.

| Archivo de Prueba | Pruebas | Fallos | Tiempo (s) |
|---|:---:|:---:|:---:|
| Billetera.test.tsx | 6 | 0 | 0.097 |
| cliente.test.ts | 8 | 0 | 0.005 |
| Cotiza.test.tsx | 4 | 0 | 0.824 |
| LineaTiempo.test.tsx | 5 | 0 | 0.041 |
| PreRegistro.test.tsx | 6 | 0 | 6.996 |
| Rastreo.test.tsx | 4 | 0 | 1.643 |

---

## 3. Catálogo de Endpoints REST (OpenAPI 3)

### 3.1. Operaciones por Grupo
| Grupo / Módulo | Operaciones | Especificación |
|---|:---:|---|
| Autenticación y Sesión | 2 | Interno |
| Encomiendas y Doble QR | 5 | Interno |
| Entregas y Validación Cruzada | 1 | Interno |
| Módulo de Almacén | 4 | Interno |
| Portal Web Público | 6 | Público |
| Pre-Registros Ventanilla | 1 | Interno |
| Sincronización Offline | 1 | Interno |
| Viajes y Flota | 1 | Interno |

### 3.2. Listado Completo de Operaciones
| Método | Ruta | Grupo | Operación (ID) |
|---|---|---|---|
| `GET` | `/api/v1/almacen/encomiendas/buscar` | Módulo de Almacén | buscarEncomiendas |
| `POST` | `/api/v1/almacen/incidencias` | Módulo de Almacén | registrarIncidencia |
| `GET` | `/api/v1/almacen/incidencias` | Módulo de Almacén | listarIncidencias |
| `POST` | `/api/v1/almacen/lotes/escaneo` | Módulo de Almacén | procesarLoteEscaneo |
| `POST` | `/api/v1/auth/login` | Autenticación y Sesión | login |
| `GET` | `/api/v1/auth/me` | Autenticación y Sesión | me |
| `POST` | `/api/v1/encomiendas` | Encomiendas y Doble QR | registrarEncomienda |
| `GET` | `/api/v1/encomiendas` | Encomiendas y Doble QR | listarEncomiendas |
| `GET` | `/api/v1/encomiendas/{id}` | Encomiendas y Doble QR | consultarPorId |
| `POST` | `/api/v1/encomiendas/{id}/cobro` | Encomiendas y Doble QR | registrarCobro |
| `GET` | `/api/v1/encomiendas/{id}/qr1.png` | Encomiendas y Doble QR | obtenerImagenQr1 |
| `POST` | `/api/v1/entregas/validar` | Entregas y Validación Cruzada | validarEntrega |
| `GET` | `/api/v1/preregistros/{codigoExpress}` | Pre-Registros Ventanilla | consultarPorCodigo |
| `POST` | `/api/v1/publico/billetera` | Portal Web Público | consultarBilletera |
| `POST` | `/api/v1/publico/cotizacion` | Portal Web Público | cotizar |
| `GET` | `/api/v1/publico/ping` | Portal Web Público | ping |
| `POST` | `/api/v1/publico/pre-registros` | Portal Web Público | crearPreRegistro |
| `GET` | `/api/v1/publico/rastreo` | Portal Web Público | consultarRastreo |
| `GET` | `/api/v1/publico/sucursales` | Portal Web Público | listarSucursales |
| `POST` | `/api/v1/sync/encomiendas` | Sincronización Offline | sincronizarEncomiendas |
| `GET` | `/api/v1/viajes` | Viajes y Flota | listarViajes |

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

- **Total de Commits**: 95
- **Primer Commit**: 2026-10-19 (0801caa) - chore: inicializar estructura base del repositorio SGL-DQR
- **Último Commit**: 2026-10-23 (f861a4c) - chore(api): regenerar contratos openapi en utf-8 limpio sin mojibake

### Distribución por Convención de Commits:
| Tipo de Commit | Cantidad | Descripción |
|---|:---:|---|
| `feat` | 44 | Nuevas funcionalidades operativas y de integración |
| `chore` | 12 | Mantenimiento, CI, dependencias y configuración |
| `docs` | 12 | Documentación técnica, matrices y manuales |
| `fix` | 13 | Corrección de defectos y deuda técnica |
| `merge` | 11 | Integraciones de ramas hacia develop y main |
| `test` | 1 | Pruebas automatizadas directas |
| Otros | 2 | Ajustes varios |

---

## 6. Líneas de Código Fuente (LOC)

*Nota: Se excluyen carpetas `target/`, `node_modules/`, `dist/`, `.git/` y respaldos `*.bak`.*

| Módulo / Carpeta | Lenguaje Principal | Archivos | Líneas de Código (LOC) |
|---|---|:---:|:---:|
| `backend-marvisur/marvisur-core` | Java 21 | 65 | 2916 |
| `backend-marvisur/marvisur-api` | Java 21 | 143 | 11919 |
| `frontend-marvisur/escritorio` | JavaFX 21 (Java) | 72 | 7333 |
| `frontend-marvisur/portal-web` | TypeScript / React | 25 | 3773 |
| `database` | PostgreSQL (SQL) | 9 | 534 |
| **TOTAL SISTEMA** | **Multi-lenguaje** | **314** | **26475** |

---

*Informe generado automáticamente por `scripts/informe-metricas.ps1` para auditoría técnica.*