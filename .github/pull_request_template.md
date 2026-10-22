## Descripción del cambio
<!-- Resume el objetivo del Pull Request y los cambios técnicos introducidos -->

## Identificador de tarea
- **ID de tarea**: `<!-- Ej: B1, FX1, W1, O1 -->`
- **Pista**: `<!-- I1 Backend | I2 Escritorio | I3 Web | I4 Infra/BD -->`
- **Rama origen**: `feature/<ID>-<descripcion>`
- **Rama destino**: `develop`

---

## Lista de verificación (Checklist)

Marca con `[x]` los puntos verificados antes de solicitar revisión:

- [ ] **Rama base correcta**: El PR apunta exclusivamente a la rama `develop`.
- [ ] **Compilación y pruebas**: `mvn clean test` (y `mvn clean verify` si aplica) pasa exitosamente en local.
- [ ] **Arquitectura hexagonal y ArchUnit**: Las reglas de dependencias del core se mantienen en verde (`CoreHexagonalArchitectureTest`).
- [ ] **ROADMAP.md**: Se actualizó el estado de la tarea (`[x]`) y el punto de reanudación de mi pista.
- [ ] **CHANGELOG.md**: Se añadió la entrada de cambio bajo `[Unreleased]` con el formato `[Ix][ID] <descripción>`.
- [ ] **Cero secretos**: No se incluyen llaves privadas, JWT secrets, contraseñas ni archivos `.env`.
- [ ] **Convenciones de código**: Nombres en español, coherentes con `/docs` y el `Diccionario_Datos_SGL_DQR.md`.
- [ ] **Commits convencionales**: Todos los commits del branch siguen el formato `<tipo>(<ID>): <descripcion>`.
