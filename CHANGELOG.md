# CHANGELOG — SGL-DQR · Expreso Marvisur

Formato basado en [Keep a Changelog](https://keepachangelog.com/es/1.1.0/).
Versiones siguiendo [Semantic Versioning](https://semver.org/lang/es/).

---

## [Unreleased]

### Added
- [I1][P1] Estructura Maven multimódulo raíz (`marvisur-parent`), módulos `marvisur-core` y `marvisur-api` (esqueleto), carpetas `frontend-marvisur/escritorio`, `frontend-marvisur/portal-web`, `database/migrations`, `database/sqlite`. Rama: `feature/CORE-01-dominio-hexagonal`.
- [I1][P3] Dominio hexagonal puro (`marvisur-core`, sin Spring/JPA/JavaFX): entidades `Encomienda` (ciclo de vida + calcularTarifa), `CodigoQR` (invalidarQR2 irreversible), `Agencia`, `Ruta`, `Usuario`, `Vehiculo`; enums `EstadoEnvio`, `EstadoPago`, `EstadoQR2`, `Rol`; excepciones de dominio; puertos `RepositorioEncomiendaPort`, `RepositorioCodigoQRPort`, `ImpresionPort`; casos de uso `RegistrarEncomiendaUseCase`, `ValidarEntregaUseCase`, `ConsultarEncomiendaUseCase`. 31 tests unitarios — BUILD SUCCESS.
- [I1][P4] `MotorQR`: generación de par QR con ZXing (PNG 300×300), hash SHA-256 de bytes de imagen (nunca del texto en claro), `validarCruzado`, `invalidarQR2`. `ParDobleQR` record, `ResultadoValidacion` enum. Pendiente: ArchUnit (CORE-04).

---

## [0.1.0] — TBD

### Added
- Estructura inicial del repositorio SGL-DQR

---

*Los integrantes añaden entradas bajo [Unreleased] con formato:*
*`[Ix][ID-TAREA] Descripción breve — pendiente: X`*
