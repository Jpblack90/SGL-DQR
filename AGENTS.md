# REGLAS DE TRABAJO (Modo Equipo — Fase B)

1. **Identificadores de pista y responsabilidades**:
   - **I1 — Backend API**: endpoints operativos, reportes, dashboard, flota y comprobante PDF.
   - **I2 — Escritorio JavaFX**: cliente ventanilla, validación física, escaneo e impresión térmica ESC/POS.
   - **I3 — Portal Web y Pruebas**: pre-registro Código Express, Billetera Digital, tracking y pruebas E2E.
   - **I4 — BD, Hardware, Offline e Infraestructura**: replicación y sincronización SQLite/PostgreSQL, Docker prod, CI/CD.

2. **Estrategia de ramas**:
   - La rama base de integración continua es `develop`.
   - Toda tarea se desarrolla en una rama propia nombrada: `feature/<ID>-<descripcion>` (por ejemplo: `feature/B1-dashboard-kpis`, `feature/FX1-pantalla-registro`, `feature/W1-scaffolding-web`).
   - Flujo de trabajo estricto: **1 prompt = 1 rama = 1 Pull Request hacia `develop`**.

3. **Commits y disciplina de trabajo**:
   - Haz commit tras cada subpaso funcional usando Conventional Commits con el ID de la tarea: `<tipo>(<ID>): <descripcion>` (ejemplo: `feat(B1): servicio de calculo de kpis de agencia`, `test(W2): pruebas unitarias de formulario de preregistro`).
   - Mi cupo de interacción o cuota puede agotarse en cualquier momento; nunca dejes trabajo funcional sin commit en tu rama.

4. **Actualización de documentación (ROADMAP y CHANGELOG)**:
   - En cada commit actualiza el "Punto de reanudación" de TU sección en `ROADMAP.md` (tarea, subpaso, archivo que sigue).
   - Cada integrante edita **exclusivamente su propia sección** del ROADMAP y CHANGELOG; no toques las secciones de otros integrantes.
   - Al cerrar la tarea márcala `[x]` en tu pista del ROADMAP y añade una línea en `CHANGELOG.md` bajo `[Unreleased]` con el ID de la tarea y tu identificador `[Ix][ID]`.

5. **Código compartido y arquitectura**:
   - Código, comentarios y nombres en español, coherentes con los diagramas UML y las especificaciones de `/docs`.
   - Pruebas unitarias en todo código de negocio. Las reglas de arquitectura hexagonal de `marvisur-core` (ArchUnit) deben mantenerse siempre en verde.
   - El módulo `marvisur-core` es compartido: si necesitas una nueva entidad, método o puerto, propón el cambio y consénsualo con el equipo antes de modificarlo. No toques el código de otra pista sin previo acuerdo.

6. **Seguridad y fuente de verdad**:
   - **Cero secretos en Git**: ninguna contraseña, credencial, archivo `.env` ni token privado debe versionarse.
   - Si algo no está en `/docs` o en el `Diccionario_Datos_SGL_DQR.md`, pregunta; no inventes tablas, columnas ni reglas de negocio.

7. **Consistencia de interfaz y pruebas en escritorio**:
   - **Nunca reescribir un FXML completo**: edita únicamente las líneas necesarias para evitar romper identificadores (`fx:id`) o anidamientos de la vista.
   - **Verificación de consistencia FXML-Controlador**: tras editar una vista FXML o un controlador JavaFX, ejecuta obligatoriamente `ConsistenciaVistasFxmlTest` para certificar la correspondencia exacta de campos `@FXML` y métodos manejadores.
   - **Reporte de verificación manual**: lo que las pruebas sin interfaz no pueden verificar (cómo se ve una pantalla y si abre) debe listarse explícitamente en el reporte como «pendiente de verificación manual del usuario».
