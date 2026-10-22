# SGL-DQR — Sistema de Gestión Logística con Doble QR

**Expreso Marvisur** · Fase 3: Desarrollo e Implementación · Versión 0.1.0  
Arquitectura Hexagonal · Java 21 · Spring Boot 3 · PostgreSQL 16 · Flyway · ZXing · JWT RBAC

---

## 1. Descripción del Sistema y Arquitectura

El **Sistema de Gestión Logística con Doble QR (SGL-DQR)** es una solución integral desarrollada para modernizar y asegurar las operaciones de transporte y despacho de encomiendas de **Expreso Marvisur**. Su núcleo criptográfico implementa una validación cruzada de dos códigos QR independientes: un código físico impreso en la etiqueta de la encomienda (QR1) y un código digital de un solo uso enviado a la Billetera Digital del cliente (QR2). La entrega física únicamente se autoriza tras verificar la correspondencia criptográfica de ambos códigos y confirmar el cobro total, invalidando irreversiblemente el QR2 para evitar fraudes y suplantaciones.

El proyecto está estructurado bajo una **Arquitectura Hexagonal (Puertos y Adaptadores)** estricta y desacoplada en módulos Maven:
- **`marvisur-core`**: Núcleo de dominio puro en Java 21 sin dependencias de frameworks externos (sin Spring, JPA ni JavaFX). Contiene las entidades (`Encomienda`, `ParDobleQR`, `Cliente`, `Empleado`), enums del negocio, excepciones de dominio, puertos de entrada/salida y el `MotorQR` criptográfico. Las reglas arquitecturales son verificadas en cada compilación mediante ArchUnit.
- **`marvisur-api`**: Capa de aplicación e infraestructura construida sobre Spring Boot 3.3. Expone servicios RESTful con autenticación sin estado (stateless) vía JWT, control de acceso basado en roles (RBAC para roles TN1 a TN4), adaptadores de persistencia Spring Data JPA hacia PostgreSQL 16, migraciones versionadas con Flyway y endpoints públicos de Fricción Cero con limitación de tasa (rate limiting).

---

## 2. Requisitos Previos

- **Java Development Kit (JDK)**: Versión 21 (LTS) de 64 bits (Microsoft Build of OpenJDK, Eclipse Temurin u Oracle JDK).
- **Apache Maven**: Versión 3.9 o superior.
- **Docker y Docker Compose**: Motor Docker v24+ y Docker Compose v2+ para base de datos local y ejecución de pruebas de integración con Testcontainers.
- **Git**: Versión 2.40 o superior.

---

## 3. Arranque Rápido en Desarrollo (Perfil `dev`)

### Paso 1: Clonar y configurar variables de entorno
```bash
git clone <URL_DEL_REPOSITORIO>
cd Sistema
cp .env.example .env
```

### Paso 2: Iniciar contenedor de base de datos PostgreSQL
```bash
docker compose up -d
```
Esto inicializa un contenedor PostgreSQL 16 expuesto en `localhost:5432` con la base de datos `marvisur_db`.

### Paso 3: Ejecutar la API con el perfil `dev`
```bash
cd backend-marvisur
mvn spring-boot:run -pl marvisur-api -Dspring-boot.run.profiles=dev
```

En este perfil:
1. Flyway ejecuta automáticamente las migraciones base `V1__...` a `V8__...` ubicadas en `database/postgres/migrations/`.
2. Flyway aplica adicionalmente la migración de desarrollo `V100__datos_demo.sql` ubicada en `database/postgres/dev-seed/`, precargando sucursales, vehículos, rutas, viajes, empleados de prueba con roles TN1..TN4, clientes y encomiendas con tokens QR2 precalculados.
3. Se publican los siguientes servicios:
   - **API REST**: `http://localhost:8080/api/v1`
   - **Swagger UI interactivo**: `http://localhost:8080/swagger-ui.html`
   - **Actuator Health Check**: `http://localhost:8080/actuator/health`

Para consultar las credenciales de los empleados y los tokens de prueba precargados, revisa la guía de datos demo en [docs/dev-demo.md](docs/dev-demo.md).

---

## 4. Ejecución de Pruebas

El proyecto cuenta con una suite integral de pruebas unitarias, de arquitectura y de integración:

### Pruebas Rápidas (Unitarias y H2 en memoria)
Ejecuta las pruebas de `marvisur-core` (dominio puro, `MotorQRTest`, `ArchUnitTest`) y pruebas de controladores de `marvisur-api` sobre base de datos H2:
```bash
cd backend-marvisur
mvn clean test
```

### Pruebas Completas de Integración (PostgreSQL real con Docker)
Ejecuta la verificación completa (`verify`), levantando contenedores reales de PostgreSQL 16 mediante Testcontainers (incluyendo pruebas de concurrencia y carrera de 10 hilos simultáneos sobre el mismo token QR2 y flujo demo `V100`):
```bash
cd backend-marvisur
mvn clean verify
```
> [!NOTE]
> Requiere Docker Desktop encendido. Si Docker no está en ejecución, las pruebas anotadas con `@Testcontainers(disabledWithoutDocker = true)` se omitirán automáticamente sin romper la compilación local.

---

## 5. Especificaciones OpenAPI (Swagger)

Las definiciones OpenAPI 3 se encuentran exportadas y versionadas en el directorio `docs/`:
- [`docs/openapi-publico.json`](docs/openapi-publico.json): Contrato de los endpoints públicos de autoservicio (rastreo, cotización, pre-registro express, billetera digital).
- [`docs/openapi-interno.json`](docs/openapi-interno.json): Contrato de los endpoints operativos internos (autenticación JWT, encomiendas, entregas, almacén y viajes).

### Regeneración de especificaciones:
Las especificaciones se pueden regenerar de dos formas:
1. **Mediante prueba automatizada (recomendado, sin levantar el servidor manualmente):**
   ```bash
   cd backend-marvisur
   mvn test -Dtest=OpenApiExportTest -pl marvisur-api
   ```
2. **Mediante `curl` con la aplicación en ejecución:**
   ```bash
   curl -s http://localhost:8080/v3/api-docs/publico > docs/openapi-publico.json
   curl -s http://localhost:8080/v3/api-docs/interno > docs/openapi-interno.json
   ```

---

## 6. Estructura del Repositorio

```text
Sistema/
├── .github/
│   ├── workflows/ci.yml             # Pipeline de CI (Java 21, Maven cache, Docker verify)
│   ├── CODEOWNERS                   # Asignación de módulos a integrantes
│   └── pull_request_template.md     # Plantilla estándar para Pull Requests
├── backend-marvisur/                # Proyecto multimódulo Maven
│   ├── pom.xml                      # Reactor POM principal
│   ├── marvisur-core/               # Núcleo de dominio hexagonal (Java 21 puro)
│   └── marvisur-api/                # API REST Spring Boot 3 (PostgreSQL, JWT, Flyway)
├── frontend-marvisur/
│   ├── escritorio/                  # Cliente de escritorio JavaFX (offline-first)
│   └── portal-web/                  # Portal web de autoservicio y Billetera Digital (React)
├── database/
│   ├── postgres/
│   │   ├── migrations/              # Migraciones Flyway oficiales (V1__ a V8__)
│   │   └── dev-seed/                # Datos de prueba para desarrollo (V100__)
│   └── sqlite/                      # Base de datos local para clientes desconectados
├── docs/                            # Documentación técnica de arquitectura y contratos
│   ├── Diccionario_Datos_SGL_DQR.md # Fuente de verdad del esquema de base de datos
│   ├── requisitos.md                # Requisitos funcionales (RF) y no funcionales (RNF)
│   ├── dev-demo.md                  # Credenciales, tokens y catálogo de datos demo
│   ├── openapi-interno.json         # Especificación OpenAPI de endpoints internos
│   └── openapi-publico.json         # Especificación OpenAPI de endpoints públicos
├── docker-compose.yml               # Orquestación de contenedores (PostgreSQL 16)
├── ROADMAP.md                       # Estado de avance y asignación de pistas de trabajo
├── CHANGELOG.md                     # Registro de cambios por versión (Keep a Changelog)
├── AGENTS.md                        # Reglas de desarrollo en equipo y convenciones
├── .env.example                     # Plantilla de variables de entorno para producción/dev
└── README.md                        # Documentación general del repositorio
```

---

## 7. Reglas de Trabajo del Equipo (Fase B)

Para el desarrollo colaborativo en paralelo, cada integrante trabaja bajo las siguientes directrices (extraídas de [`AGENTS.md`](AGENTS.md)):

1. **Identificadores y Pistas de Trabajo**:
   - **I1 — Backend restante**: Dashboard KPIs, flota, viajes, comprobante PDF, sincronización.
   - **I2 — Escritorio JavaFX**: Ventanilla, validación física/escáner, impresión térmica ESC/POS, modo offline.
   - **I3 — Portal Web y Pruebas**: Pre-registro Código Express, Billetera Digital, tracking, pruebas E2E.
   - **I4 — BD, Hardware, Offline e Infraestructura**: Sincronización SQLite/PostgreSQL, tickets, Docker prod, CI/CD.
2. **Estrategia de Ramas**:
   - Toda rama parte siempre de `develop`.
   - Nomenclatura obligatoria: `feature/<ID>-<descripcion>` (ej. `feature/B1-dashboard-kpis`, `feature/W2-billetera-react`).
   - Flujo estricto: **1 prompt / tarea = 1 rama = 1 Pull Request hacia `develop`**.
3. **Commits Convencionales**:
   - Formato: `<tipo>(<ID>): <descripcion>` (ej. `feat(B1): endpoints de metricas para dashboard`).
4. **Independencia de Documentación**:
   - Cada integrante edita **únicamente su sección asignada** en `ROADMAP.md` y `CHANGELOG.md`.
   - Al completar una tarea se marca `[x]` en el ROADMAP y se añade la línea correspondiente en `CHANGELOG.md` bajo `[Unreleased]`.
5. **Código Compartido e Integridad**:
   - El módulo `marvisur-core` es compartido: cualquier modificación a entidades o puertos existentes debe ser consensuada previamente con el equipo.
   - Las reglas de arquitectura ArchUnit (`CoreHexagonalArchitectureTest`) deben mantenerse en verde en todo momento.
   - **Cero secretos en Git**: ninguna contraseña, llave privada ni JWT secret debe subirse al repositorio.
