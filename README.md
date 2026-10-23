# SGL-DQR — Sistema de Gestión Logística con Doble QR

**Expreso Marvisur** · Fase 3: Desarrollo e Implementación · Versión 0.2.0 (Fase B Congelada)  
Arquitectura Hexagonal · Java 21 · Spring Boot 3 · JavaFX 21 (Offline-First) · React 18 · PostgreSQL 16 · SQLite · Flyway · ZXing · JWT RBAC

---

## 1. Descripción del Sistema y Arquitectura

El **Sistema de Gestión Logística con Doble QR (SGL-DQR)** es una solución integral desarrollada para modernizar, agilizar y asegurar las operaciones de transporte, despacho y entrega de encomiendas de **Expreso Marvisur**.

Su innovación central es el **Doble QR Criptográfico**:
- **QR1 Físico**: Impreso en la etiqueta térmica del paquete, visible para el personal de transporte y almacén.
- **QR2 Digital de Retiro**: Generado dinámicamente y expuesto exclusivamente en la Billetera Digital del cliente receptor mediante URL segura con token en el fragmento hash (`/billetera#<token>`).
- **Validación Cruzada**: La entrega en almacén se autoriza únicamente tras contrastar la correspondencia matemática de ambos hashes SHA-256 y confirmar el cobro total. Tras la entrega, el QR2 queda **irreversiblemente invalidado** en la base de datos (RNF-01).

### Componentes del Ecosistema:
1. **`marvisur-core`**: Núcleo de dominio puro en Java 21 sin dependencias de frameworks externos (sin Spring, JPA ni JavaFX). Contiene las entidades (`Encomienda`, `ParDobleQR`, `Cliente`, `Empleado`), enums de negocio, puertos y el `MotorQR` criptográfico. Verificado mediante ArchUnit.
2. **`marvisur-api`**: Backend RESTful Spring Boot 3.3 con PostgreSQL 16, migraciones Flyway V1–V8 + seed V100, seguridad JWT con RBAC (roles TN1 a TN4), rate limiting en endpoints públicos, transacciones pesimistas (`SELECT ... FOR UPDATE`) en entregas y sincronización idempotente por lotes (`POST /api/v1/sync/encomiendas`).
3. **`marvisur-escritorio`**: Cliente de ventanilla y almacén en JavaFX 21 con capacidades **offline-first** (RNF-02, RNF-05). Registra encomiendas localmente en SQLite (`datos-locales/marvisur-local.db`) ante caídas de red ($\le 2$ s) y sincroniza automáticamente en segundo plano con backoff exponencial. Incluye componente `CampoEscaneo` para lectores USB y emulación de impresión térmica de 58 mm.
4. **`portal-web`**: Portal web de autoservicio para clientes en React 18 + Vite + TypeScript + Tailwind CSS (mobile-first), con módulos de rastreo en vivo (`/rastreo`), cotización y Billetera Digital segura (`/billetera`).

---

## 2. Estado de Implementación y Kit de Demostración

- 📊 **[Matriz de Estado de Implementación y Trazabilidad (RF / RNF)](docs/estado-implementacion.md)**: Reporte detallado de los requisitos RF-01..RF-11 y RNF-01..RNF-05 con evidencias en el código, limitaciones y conteo exacto de pruebas.
- 🚀 **[Kit de Demostración y Scripts de Automatización](scripts/README.md)**: Guion paso a paso y scripts para Windows (`.ps1`) y Linux (`.sh`) para reset de base de datos, inicio de API, portal web, escritorio y simulación interactiva de cortes de red offline y reconexión.

---

## 3. Conteo de Pruebas Automatizadas (230 Pruebas en Verde)

El sistema cuenta con una cobertura integral de pruebas unitarias, de arquitectura, de integración con contenedores Docker reales y pruebas de interfaz:

| Módulo / Proyecto | Tecnologías | Pruebas | Resultado |
|---|---|:---:|:---:|
| `marvisur-core` | Java 21, JUnit 5, Mockito, ArchUnit | **50** | **100% Verde** |
| `marvisur-api` | Spring Boot 3.3, Testcontainers (PostgreSQL 16), Flyway | **100** | **100% Verde** |
| `marvisur-escritorio` | JavaFX 21, SQLite JDBC, HttpServer Mock | **64** | **100% Verde** |
| `portal-web` | React 18, Vite, TypeScript, Vitest, Testing Library | **16** | **100% Verde** |
| **TOTAL** | **Multiplataforma E2E** | **230** | **100% VERDE** |

---

## 4. Requisitos Previos

- **Java Development Kit (JDK)**: Versión 21 (LTS) de 64 bits (`JAVA_HOME` apuntando a JDK 21).
- **Apache Maven**: Versión 3.9 o superior.
- **Node.js**: Versión 20+ y `npm`.
- **Docker y Docker Compose**: Motor Docker v24+ y Docker Compose v2+ para PostgreSQL 16 y Testcontainers.
- **Git**: Versión 2.40 o superior.

---

## 5. Arranque Rápido con el Kit de Demo

Para iniciar rápidamente la demostración completa con datos semilla precargados:

### En Windows (PowerShell):
```powershell
# 1. Resetear base de datos y recrear datos semilla demo
.\scripts\demo-reset.ps1

# 2. Iniciar API backend (perfil dev con datos demo V100)
.\scripts\demo-api.ps1

# 3. En otra terminal, iniciar el portal web
.\scripts\demo-portal.ps1

# 4. En otra terminal, iniciar la aplicación de escritorio JavaFX
.\scripts\demo-escritorio.ps1
```

### En Linux / macOS (Bash):
```bash
# 1. Resetear base de datos y recrear datos semilla demo
./scripts/demo-reset.sh

# 2. Iniciar API backend
./scripts/demo-api.sh

# 3. En otra terminal, iniciar el portal web
./scripts/demo-portal.sh

# 4. En otra terminal, iniciar la aplicación de escritorio JavaFX
./scripts/demo-escritorio.sh
```

Para consultar las credenciales de los empleados demo y los tokens precargados, revisa la guía en [docs/dev-demo.md](docs/dev-demo.md).

---

## 6. Ejecución de Pruebas del Sistema

### Ejecución de pruebas Java completas (Core + API + Escritorio):
```bash
mvn clean verify
```
*(Requiere Docker encendido para los contenedores PostgreSQL de Testcontainers; si Docker no está activo, las pruebas de integración correspondientes se omiten de forma segura sin fallar el build).*

### Ejecución de pruebas del Portal Web:
```bash
cd frontend-marvisur/portal-web
npm ci
npm run lint
npm test
npm run build
```

---

## 7. Especificaciones OpenAPI (Swagger)

Las definiciones OpenAPI 3 se encuentran exportadas y versionadas en el directorio `docs/`:
- [`docs/openapi-publico.json`](docs/openapi-publico.json): Contrato de los endpoints públicos de autoservicio (rastreo, cotización, pre-registro express, billetera digital).
- [`docs/openapi-interno.json`](docs/openapi-interno.json): Contrato de los endpoints operativos internos (autenticación JWT, encomiendas, entregas, almacén y viajes).

---

## 8. Estructura del Repositorio

```text
Sistema/
├── .github/
│   ├── workflows/ci.yml             # Pipeline de CI (Java 21, Maven, Docker, Node 20, Vite)
│   ├── CODEOWNERS                   # Asignación de módulos y pistas
│   └── pull_request_template.md     # Plantilla estándar para Pull Requests
├── backend-marvisur/                # Módulos backend Maven
│   ├── pom.xml                      # POM padre backend
│   ├── marvisur-core/               # Núcleo de dominio hexagonal (Java 21 puro, ArchUnit)
│   └── marvisur-api/                # API REST Spring Boot 3 (PostgreSQL, JWT, Flyway)
├── frontend-marvisur/
│   ├── escritorio/                  # Cliente de escritorio JavaFX (offline-first con SQLite)
│   └── portal-web/                  # Portal web de clientes y Billetera Digital (React + Vite)
├── database/
│   ├── postgres/
│   │   ├── migrations/              # Migraciones Flyway oficiales (V1__ a V8__)
│   │   └── dev-seed/                # Datos semilla de prueba para desarrollo (V100__)
│   └── sqlite/                      # Base de datos local para clientes desconectados
├── scripts/                         # Kit de scripts para demostración (.ps1 y .sh)
│   ├── demo-reset                   # Reset de Docker PostgreSQL con espera pg_isready
│   ├── demo-api                     # Lanzador de la API con detección de IP LAN
│   ├── demo-portal                  # Lanzador del portal web en modo host
│   ├── demo-escritorio              # Lanzador de JavaFX con URL de API por parámetro
│   ├── demo-parar-api               # Corte de la API para simulación de caída de red (offline)
│   ├── demo-iniciar-api             # Restauración de la API para sincronización automática
│   └── README.md                    # Guía de uso y guion paso a paso de demostración
├── docs/                            # Documentación técnica y contratos
│   ├── estado-implementacion.md     # Matriz honesta de requerimientos RF-01..11 y RNF-01..05
│   ├── Diccionario_Datos_SGL_DQR.md # Fuente de verdad del esquema de base de datos
│   ├── requisitos.md                # Requisitos funcionales y no funcionales de diseño
│   ├── dev-demo.md                  # Credenciales, tokens y catálogo de datos demo
│   ├── openapi-interno.json         # Especificación OpenAPI de endpoints internos
│   └── openapi-publico.json         # Especificación OpenAPI de endpoints públicos
├── docker-compose.yml               # Orquestación de contenedores (PostgreSQL 16)
├── ROADMAP.md                       # Estado de avance y congelamiento de Fase B
├── CHANGELOG.md                     # Registro histórico de versiones (Keep a Changelog)
├── AGENTS.md                        # Reglas de desarrollo en equipo y convenciones
├── .env.example                     # Plantilla de variables de entorno para desarrollo y prod
├── pom.xml                          # Reactor raíz multimódulo Maven
└── README.md                        # Documentación general del repositorio
```
