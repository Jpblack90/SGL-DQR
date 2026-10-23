# Informe Técnico de Métricas del Sistema — SGL-DQR

- **Fecha de Generación**: 2026-10-23 16:06:45 -05:00
- **Hash de Commit**: `b7938b3` (completo: `b7938b308dc213bb83becd08f779ebdb188e45a2`)
- **Rama Actual**: `feature/W3-preregistro`
- **Generado por**: `scripts/informe-metricas.ps1` (extracción automática en vivo)

---

## 1. Resumen Ejecutivo de Métricas Clave

| Métrica | Valor Real | Observación |
|---|:---:|---|
| **Total de Pruebas Automatizadas** | **246** | Core (50), API (100), JavaFX (64), Web (32) |
| **Pruebas Fallidas / Errores** | **0 / 0** | **100% en verde** |
| **Operaciones REST (OpenAPI)** | **23** | 17 internas, 6 públicas |
| **Migraciones Flyway Oficiales** | **8** | V1 a V8 en `database/postgres/migrations` |
| **Tablas en Base de Datos** | **18** | Coincidencia 100% con las 18 tablas del Diccionario |
| **Total de Commits Git** | **76** | Primer commit: 2026-10-19 (0801caa) - chore: inicializar estructura base del repositorio SGL-DQR |
| **Líneas de Código Fuente (LOC)** | **25604** | 309 archivos (Java, TypeScript, SQL sin dependencias) |

---

## 2. Detalle de Pruebas Automatizadas por Módulo y Clase

### 2.1. marvisur-core (50 pruebas · 2.44 s)
- **Pruebas Unitarias de Dominio y Casos de Uso**: 45 pruebas
- **Pruebas de Reglas de Arquitectura Hexagonal (ArchUnit)**: 5 reglas verificadas

| Clase de Prueba | Pruebas | Fallos | Errores | Omitidas | Tiempo (s) | Tipo |
|---|:---:|:---:|:---:|:---:|:---:|---|
| CambiarEstadoServiceTest | 4 | 0 | 0 | 0 | 0.844 | Unitaria Dominio |
| RegistrarEncomiendaServiceTest | 3 | 0 | 0 | 0 | 0.08 | Unitaria Dominio |
| TarifarioPorPesoTest | 4 | 0 | 0 | 0 | 0.005 | Unitaria Dominio |
| ArquitecturaHexagonalTest | 5 | 0 | 0 | 0 | 1.155 | ArchUnit (Hexagonal) |
| AuditoriaYTrackingTest | 2 | 0 | 0 | 0 | 0.023 | Unitaria Dominio |
| EncomiendaTest | 7 | 0 | 0 | 0 | 0.011 | Unitaria Dominio |
| AlmacenYViajesModelosTest | 5 | 0 | 0 | 0 | 0.051 | Unitaria Dominio |
| MapeadorEventoTrackingTest | 6 | 0 | 0 | 0 | 0.068 | Unitaria Dominio |
| PreRegistroExpressTest | 3 | 0 | 0 | 0 | 0.01 | Unitaria Dominio |
| MotorQRTest | 8 | 0 | 0 | 0 | 0.041 | Unitaria Dominio |
| ZXingAdapterTest | 3 | 0 | 0 | 0 | 0.152 | Unitaria Dominio |

### 2.2. marvisur-api (100 pruebas · 55.98 s)
Incluye integración con Testcontainers (PostgreSQL 16 real), pruebas de concurrencia de 10 hilos simultáneos, rollback transaccional atómico y control RBAC.

| Clase de Prueba | Pruebas | Fallos | Errores | Omitidas | Tiempo (s) |
|---|:---:|:---:|:---:|:---:|:---:|
| OpenApiExportTest | 2 | 0 | 0 | 0 | 16.973 |
| AlmacenControllerTest | 9 | 0 | 0 | 0 | 3.286 |
| EncomiendaControllerTest | 8 | 0 | 0 | 0 | 0.686 |
| EntregaControllerTest | 8 | 0 | 0 | 0 | 8.828 |
| PublicoControllerTest | 17 | 0 | 0 | 0 | 0.771 |
| SyncControllerTest | 9 | 0 | 0 | 0 | 5.321 |
| DevSeedFlujoDemoTest | 8 | 0 | 0 | 0 | 4.923 |
| ManejadorGlobalExcepcionesTest | 6 | 0 | 0 | 0 | 1.027 |
| MigracionesFlywayTest | 8 | 0 | 0 | 0 | 6.403 |
| PersistenciaPostgresTest | 12 | 0 | 0 | 0 | 0.666 |
| AutenticacionYRbacTest | 11 | 0 | 0 | 0 | 2.836 |
| RegistroEncomiendaAtomicidadTest | 2 | 0 | 0 | 0 | 4.26 |

### 2.3. marvisur-escritorio (64 pruebas · 5.476 s)
Pruebas headless en JavaFX con mocks HTTP y base local SQLite. Valida conmutación offline en <= 2 s, backoff exponencial y cero persistencia de QR2 en disco.

| Clase de Prueba | Pruebas | Fallos | Errores | Omitidas | Tiempo (s) |
|---|:---:|:---:|:---:|:---:|:---:|
| DeteccionCaidaServidorTest | 2 | 0 | 0 | 0 | 0.336 |
| EntregaFlujoViewModelTest | 6 | 0 | 0 | 0 | 0.755 |
| ImpresionSimuladaAdapterTest | 1 | 0 | 0 | 0 | 0.144 |
| LoteEscaneoTest | 4 | 0 | 0 | 0 | 0.021 |
| MarvisurHttpClientAlmacenTest | 13 | 0 | 0 | 0 | 2.642 |
| MarvisurHttpClientTest | 9 | 0 | 0 | 0 | 0.256 |
| ModeloMenuRolTest | 5 | 0 | 0 | 0 | 0.017 |
| PestanasBloqueadasOfflineViewModelTest | 2 | 0 | 0 | 0 | 0.008 |
| RegistroOfflineCriptograficoTest | 1 | 0 | 0 | 0 | 0.492 |
| SeguridadAlmacenQrTest | 2 | 0 | 0 | 0 | 0.018 |
| SeguridadCeroQr2EnSqliteTest | 1 | 0 | 0 | 0 | 0.084 |
| SeguridadQr2TokenTest | 2 | 0 | 0 | 0 | 0.03 |
| SincronizadorOfflineTest | 5 | 0 | 0 | 0 | 0.286 |
| SQLiteColaAdapterTest | 6 | 0 | 0 | 0 | 0.363 |
| ValidadorVentanillaTest | 5 | 0 | 0 | 0 | 0.024 |

### 2.4. portal-web (32 pruebas · 9.972 s)
Ejecutadas con Vitest y React Testing Library. Valida aislamiento de hash #, tracking público y manejo de errores RFC 7807.

| Archivo de Prueba | Pruebas | Fallos | Tiempo (s) |
|---|:---:|:---:|:---:|
| Billetera.test.tsx | 6 | 0 | 0.098 |
| cliente.test.ts | 8 | 0 | 0.006 |
| Cotiza.test.tsx | 4 | 0 | 0.923 |
| LineaTiempo.test.tsx | 4 | 0 | 0.037 |
| PreRegistro.test.tsx | 6 | 0 | 7.273 |
| Rastreo.test.tsx | 4 | 0 | 1.635 |

---

## 3. Catálogo de Endpoints REST (OpenAPI 3)

### 3.1. Operaciones por Grupo
| Grupo / Módulo | Operaciones | Especificación |
|---|:---:|---|
| Autenticación y Sesión | 2 | Interno |
| controlador-prueba-rbac | 2 | Interno |
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
| `GET` | `/api/v1/test-rbac/admin` | controlador-prueba-rbac | soloAdministrador |
| `GET` | `/api/v1/test-rbac/almacenero` | controlador-prueba-rbac | soloAlmacenero |
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

- **Total de Commits**: 76
- **Primer Commit**: 2026-10-19 (0801caa) - chore: inicializar estructura base del repositorio SGL-DQR
- **Último Commit**: 2026-10-23 (b7938b3) - feat(W3): modulo de cotizacion en linea y pre-registro codigo express con generador qr

### Distribución por Convención de Commits:
| Tipo de Commit | Cantidad | Descripción |
|---|:---:|---|
| `feat` | 44 | Nuevas funcionalidades operativas y de integración |
| `chore` | 11 | Mantenimiento, CI, dependencias y configuración |
| `docs` | 8 | Documentación técnica, matrices y manuales |
| `fix` | 4 | Corrección de defectos y deuda técnica |
| `merge` | 7 | Integraciones de ramas hacia develop y main |
| `test` | 0 | Pruebas automatizadas directas |
| Otros | 2 | Ajustes varios |

---

## 6. Líneas de Código Fuente (LOC)

*Nota: Se excluyen carpetas `target/`, `node_modules/`, `dist/`, `.git/` y respaldos `*.bak`.*

| Módulo / Carpeta | Lenguaje Principal | Archivos | Líneas de Código (LOC) |
|---|---|:---:|:---:|
| `backend-marvisur/marvisur-core` | Java 21 | 65 | 2916 |
| `backend-marvisur/marvisur-api` | Java 21 | 142 | 11764 |
| `frontend-marvisur/escritorio` | JavaFX 21 (Java) | 68 | 6636 |
| `frontend-marvisur/portal-web` | TypeScript / React | 25 | 3754 |
| `database` | PostgreSQL (SQL) | 9 | 534 |
| **TOTAL SISTEMA** | **Multi-lenguaje** | **309** | **25604** |

---

*Informe generado automáticamente por `scripts/informe-metricas.ps1` para auditoría técnica.*