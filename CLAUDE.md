# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**SGL-DQR** — Sistema de Gestión Logística con Doble QR para Expreso Marvisur. El mecanismo central es el **Doble QR Criptográfico**: un QR1 físico en la etiqueta del paquete y un QR2 digital de retiro en la billetera del cliente, cuya validación cruzada (SHA-256) es obligatoria antes de cada entrega. El QR2 se invalida irreversiblemente en la BD tras la entrega.

Código, comentarios y nombres van **en español**, coherentes con los diagramas UML y `/docs`.

## Módulos y Estructura

Reactor Maven raíz (`pom.xml`) con tres módulos Java más un proyecto React independiente:

| Módulo | Ruta | Tecnología |
|---|---|---|
| `marvisur-core` | `Backend-marvisur/marvisur-core/` | Java 21 puro, ArchUnit |
| `marvisur-api` | `Backend-marvisur/marvisur-api/` | Spring Boot 3.3, PostgreSQL 16, Flyway, JWT |
| `escritorio` | `frontend-marvisur/escritorio/` | JavaFX 21, SQLite (offline), HttpClient |
| `portal-web` | `frontend-marvisur/portal-web/` | React 18 + Vite + TypeScript + Tailwind |

## Comandos

### Java (Core + API + Escritorio)
```bash
# Compilar y verificar todos los módulos (requiere Docker para Testcontainers)
mvn clean verify

# Solo un módulo específico
mvn clean verify -pl Backend-marvisur/marvisur-core
mvn clean verify -pl Backend-marvisur/marvisur-api
mvn clean verify -pl frontend-marvisur/escritorio

# Prueba individual (API como ejemplo)
mvn test -pl Backend-marvisur/marvisur-api -Dtest=EncomiendaControllerTest

# Forzar UTC e inglés para independencia de entorno (obligatorio antes de marcar tarea como terminada)
mvn clean verify -Duser.timezone=UTC -Duser.language=en -Duser.country=US
```

### Portal Web
```bash
cd frontend-marvisur/portal-web
npm ci
npm run lint
npm test          # Vitest
npm run build
```

### Demo y arranque local
```powershell
# Windows — ejecutar en orden, cada uno en su terminal
.\scripts\demo-reset.ps1      # Resetear Docker PostgreSQL con seed V100
.\scripts\demo-api.ps1        # API Spring Boot en :8080
.\scripts\demo-portal.ps1     # Portal web en :5173
.\scripts\demo-escritorio.ps1 # App JavaFX

# Simular caída de red (modo offline escritorio)
.\scripts\demo-parar-api.ps1
.\scripts\demo-iniciar-api.ps1  # Restaura y dispara sincronización automática
```

El perfil `dev` activa la capa de seed `classpath:db/dev-seed` (migraciones V100+) además de las migraciones base V1–V8.

## Arquitectura Hexagonal (marvisur-core)

`marvisur-core` es el núcleo de dominio sin dependencias de frameworks. Las reglas de aislamiento se verifican automáticamente en cada compilación mediante **ArchUnit** (`ArquitecturaHexagonalTest`):

- `dominio/` — entidades puras (`Encomienda`, `ParDobleQR`, `Cliente`, `Empleado`, etc.) y enums de negocio. No puede depender de `puertos/`, `aplicacion/` ni `infraestructura/`.
- `puertos/` — interfaces de repositorios y servicios externos (`RepositorioEncomiendaPort`, `ImagenQRPort`, `RelojPort`, etc.). Solo referencia `dominio/` y tipos de Java.
- `aplicacion/` — casos de uso (`RegistrarEncomiendaService`, `CambiarEstadoService`) que implementan lógica orquestando puertos.
- `infraestructura/` — adaptadores (solo en `marvisur-api` y `escritorio`; no existe en `core`).

Nunca añadir dependencias de Spring, JPA ni JavaFX a `marvisur-core`. Si se necesita un nuevo puerto o entidad compartida, consensuarlo con el equipo antes de modificar.

## Seguridad y RBAC

La API usa JWT con roles **TN1–TN4** gestionados en `SeguridadConfig`. Los endpoints públicos (`/api/v1/publico/**`, `/api/v1/rastreo/**`, `/api/v1/cotiza/**`) tienen rate limiting por IP (`FiltroRateLimitingPublico`). Las entregas usan transacciones pesimistas (`SELECT ... FOR UPDATE`) para evitar doble entrega.

Variables de entorno requeridas: ver `.env.example`. En perfil `prod` la app no inicia sin `JWT_SECRET` configurado.

## Escritorio JavaFX — Reglas Críticas

- **Nunca reescribir un FXML completo**: editar solo las líneas necesarias para no romper `fx:id` ni anidamientos.
- **Tras editar cualquier vista FXML o controlador**, ejecutar obligatoriamente:
  ```bash
  mvn test -pl frontend-marvisur/escritorio -Dtest=ConsistenciaVistasFxmlTest
  ```
- El escritorio opera **offline-first**: encomiendas se guardan en SQLite (`datos-locales/marvisur-local.db`) y se sincronizan automáticamente en segundo plano al recuperar conexión (`SincronizadorOfflineTest`).
- Los tickets térmicos 58 mm se emulan generando PNG en `salida/ticket_*.png`.

## Convenciones de Rama y Commits

- Rama base de integración: `develop`.
- Nombre de rama: `feature/<ID>-<descripcion>` (ej. `feature/B1-dashboard-kpis`).
- Commits con Conventional Commits + ID de pista: `feat(B1): servicio de cálculo de KPIs`.
- En cada commit actualizar el "Punto de reanudación" en la **propia sección** del ROADMAP; no tocar secciones de otros integrantes.
- Al cerrar tarea: marcar `[x]` en ROADMAP y añadir línea en `CHANGELOG.md` bajo `[Unreleased]`.

## Fuente de Verdad del Esquema

`docs/Diccionario_Datos_SGL_DQR.md` es la referencia canónica de tablas y columnas. No inventar tablas ni reglas de negocio que no estén documentadas ahí o en `/docs`. Las migraciones Flyway en `database/postgres/migrations/` (V1–V8) son la fuente definitiva del esquema PostgreSQL.

## CI

GitHub Actions (`.github/workflows/ci.yml`) corre en push/PR a `develop` y `main`:
- **Java**: `mvn -B clean verify` (requiere Docker para Testcontainers de la API).
- **Portal**: `npm ci && npm run lint && npm test && npm run build`.
