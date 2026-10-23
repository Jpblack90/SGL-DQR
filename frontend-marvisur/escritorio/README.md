# Cliente de Escritorio JavaFX — SGL-DQR (Expreso Marvisur)

Módulo de ventanilla, operaciones de almacén y contingencia para **Expreso Marvisur S.A.**, construido en Java 21 y JavaFX. Consume la API REST central (`marvisur-api`) siguiendo una arquitectura desacoplada basada en puertos, diseñada para incorporar sincronización offline con SQLite en fases posteriores.

---

## 1. Características Implementadas (Fases D1 + D2 + D3)

- **Seguridad y RBAC (D1):**
  - Autenticación contra `POST /api/v1/auth/login`.
  - El token JWT reside **exclusivamente en memoria RAM** (`SesionContexto`) y nunca se persiste en disco.
  - Menú lateral adaptativo por rol (TN1 Recepcionista, TN2 Almacenero, TN3 Administrador, TN4 Chofer).
  - Monitoreo continuo de salud del servidor central con polling en segundo plano cada 5 segundos a `/actuator/health` (indicador verde "En línea" / rojo "Sin conexión").
  - Cierre de sesión automático (auto-logout) ante respuestas `HTTP 401 Unauthorized` redirigiendo al login con mensaje explicativo.

- **Ventanilla de Admisión y Doble QR (D2):**
  - Formulario de registro de encomiendas con validación local (DNI de 8 dígitos, RUC de 11 dígitos, peso mayor a 0 kg).
  - Precarga automática de datos mediante **Código Express** (`GET /api/v1/preregistros/{codigoExpress}`) generado en el portal web público.
  - Selector de agencias activas cargado desde `GET /api/v1/publico/sucursales`.
  - Cotización estimada de tarifa en soles en tiempo real (`POST /api/v1/publico/cotizacion`).
  - Registro definitivo de encomienda con emisión de **Doble QR** (`POST /api/v1/encomiendas`):
    - **QR1 Físico (Operativo):** renderizado de la imagen Base64 devuelta por la API para adherir al bulto.
    - **QR2 Digital (Billetera / Llave de Retiro):** renderizado al vuelo del código QR del enlace web utilizando `ZXingAdapter` (`marvisur-core`), con botón de copiado de URL al portapapeles.
    - El token `qr2Token` vive únicamente en memoria mientras la ventana modal de confirmación está abierta y se limpia al cerrarla. En `toString()` está totalmente enmascarado (`***PROTECTED***`).
  - **Impresión Térmica Simulada:** implementación de `ImpresionPort` (`ImpresionSimuladaAdapter`) que diagramará y guardará la imagen PNG del ticket de 58 mm (384 px de ancho) en la carpeta local `salida/`.

- **Módulo de Almacén y Operaciones Logísticas (D3 - RF-06, RF-07):**
  - **Componente Reutilizable `CampoEscaneo`:** soporte para escáneres de código de barras USB (modo teclado con envío de Enter), manteniendo autoenfoque, limpieza automática tras escaneo y soporte de ingreso manual/pegar para demostraciones sin hardware.
  - **Pestaña 1 — Entrega Física y Validación Cruzada (RF-07):**
    - Escaneo pareado de QR1 (físico del paquete) y QR2 (digital del cliente).
    - Entrega directa (200 OK): mensaje verde con tiempo de latencia (< 2 s según RNF-02), datos del envío y purga inmediata de secretos en RAM.
    - Bloqueo Contra Entrega (409 `ENTREGA_BLOQUEADA_PAGO_PENDIENTE`): panel ámbar con monto adeudado y botón "Registrar cobro" que habilita el reintento inmediato sin volver a escanear (retención temporal en memoria).
    - Rechazos con feedback visual: Token Inválido/Expirado (rojo), Transición Inválida (rojo) y Encomienda No Encontrada (rojo).
  - **Pestaña 2 — Procesamiento Masivo por Lotes / Batch Scanning (RF-06):**
    - Acciones: `RECEPCIONAR_ORIGEN`, `DESPACHAR_A_VIAJE` (con selector dinámico de viajes programados) y `RECEPCIONAR_DESTINO`.
    - Detección y prevención de códigos duplicados al momento del escaneo.
    - Límite de lote seguro hasta 200 ítems.
    - Procesamiento asíncrono con barra de progreso y resumen estadístico (total, exitosos, ya registrados, errores).
    - Tabla detallada de resultados con opción de **Reintentar solo fallidos** y **Exportar reporte CSV** a `salida/`.
  - **Pestaña 3 — Modo Contingencia e Incidencias (RF-06):**
    - Búsqueda de encomiendas por DNI o RUC del remitente o destinatario (`GET /api/v1/almacen/encomiendas/buscar`).
    - Cobro directo de encomiendas Contra Entrega desde contingencia.
    - Reporte de incidencias físicas (`ETIQUETA_DANADA`, `PAQUETE_DANADO`, etc.) con regeneración en línea de imagen Base64 de QR1.
    - Impresión térmica directa de la etiqueta operativa regenerada hacia `salida/ticket_*.png`.
    - Historial de incidencias previas registradas para la encomienda.
  - **Seguridad Criptográfica:**
    - Ni QR1 ni QR2 aparecen en logs ni en representaciones `toString()`.
    - En caso de error o finalización, las variables se purgan inmediatamente de la memoria RAM.

---

## 2. Requisitos y Ejecución

### Prerrequisitos
- JDK 21 instalado (`java -version`).
- Apache Maven 3.9+ (`mvn -version`).
- Backend `marvisur-api` ejecutándose en `http://localhost:8080` (opcional para pruebas unitarias).

### Configuración
El archivo `src/main/resources/escritorio.properties` define los parámetros del cliente:
```properties
api.base-url=http://localhost:8080
app.nombre=Expreso Marvisur - Ventanilla y Almacén SGL-DQR
app.version=1.0.0
```
Puede sobreescribirse mediante la propiedad del sistema:
```bash
mvn javafx:run -Dmarvisur.api.base-url=http://192.168.1.100:8080
```

### Ejecución de la Aplicación
Desde la raíz del repositorio:
```bash
mvn javafx:run -pl frontend-marvisur/escritorio
```
O desde la carpeta `frontend-marvisur/escritorio`:
```bash
mvn javafx:run
```

### Ejecución de Pruebas Unitarias (Headless sin TestFX)
Las pruebas unitarias del cliente de escritorio no requieren entorno gráfico interactivo ni display X11/Wayland:
```bash
mvn test -pl frontend-marvisur/escritorio
```

---

## 3. Cuentas de Acceso para Demostración

| Rol | Correo Demo | Contraseña | Sucursal | Acceso Menú |
| :--- | :--- | :--- | :--- | :--- |
| **TN1 Recepcionista** | `recepcionista.demo@marvisur.pe` | `Demo1234*` | Lima Central (`SUC-LIM-01`) | Ventanilla, Contingencia |
| **TN2 Almacenero** | `almacenero.demo@marvisur.pe` | `Demo1234*` | Arequipa (`SUC-AQP-01`) | Almacén (Entrega, Lotes, Contingencia) |
| **TN3 Administrador** | `admin.demo@marvisur.pe` | `Demo1234*` | Lima Central (`SUC-LIM-01`) | Todos los módulos |
| **TN4 Chofer** | `chofer.demo@marvisur.pe` | `Demo1234*` | Lima Central (`SUC-LIM-01`) | Rutas (próximamente) |

---

## 4. Guía de Prueba Manual de Almacén (Paso a Paso con Datos Demo)

> **Nota Importante:** La ejecución de pruebas de entrega física invalida irreversiblemente el token QR2 y cambia el estado de la encomienda a `ENTREGADA`. Para reiniciar los datos demo a su estado inicial, ejecute:
> ```bash
> docker compose down -v && docker compose up -d
> ```

### A. Escenario 1: Validación Directa Exitosa (Modalidad Contado)
1. Inicie sesión con la cuenta de Almacenero:
   - Correo: `almacenero.demo@marvisur.pe` | Clave: `Demo1234*`.
2. El sistema abrirá automáticamente el módulo **Almacén** en la pestaña **Entrega (Doble QR)**.
3. En el campo **QR1 Físico**, pegue o escanee:
   ```text
   UUID-ENC-DEMO-001
   ```
4. Presione `Enter`. El cursor saltará automáticamente al campo **QR2 Digital**.
5. Ingrese el token de la encomienda pagada:
   ```text
   DEMO-TOKEN-PAGADA-001
   ```
6. Presione `Enter` o haga clic en **Validar Entrega Física**.
7. **Resultado esperado:**
   - Panel verde de confirmación: *"Entrega validada y completada con éxito"*.
   - Comprobante `MV-DEMO-001`, estado `ENTREGADA`.
   - Medición de latencia visible (< 200 ms).
   - Los campos de texto se limpian y los secretos se eliminan de la memoria.

### B. Escenario 2: Contra Entrega (Bloqueo 409 -> Cobro -> Reintento Exitoso)
1. En la pestaña **Entrega**, ingrese:
   - QR1 Físico: `UUID-ENC-DEMO-002`
   - QR2 Digital: `DEMO-TOKEN-PENDIENTE-002`
2. Presione **Validar Entrega Física**.
3. **Resultado esperado (Bloqueo 409):**
   - Panel ámbar: *"Entrega Bloqueada por Pago Pendiente"*.
   - Monto a cobrar: `S/. 45.50`.
   - Botón habilitado: **Registrar cobro y autorizar**.
4. Haga clic en **Registrar cobro y autorizar**:
   - El sistema registra el cobro en la API (`POST /api/v1/encomiendas/{id}/cobro`).
   - El botón cambia a **Reintentar validación** (conservando los códigos QR en memoria sin requerir nuevo escaneo).
5. Haga clic en **Reintentar validación**:
   - La API ahora autoriza la entrega con éxito (200 OK).
   - Panel verde de confirmación con comprobante `MV-DEMO-002`.
   - Los secretos se limpian inmediatamente de la memoria RAM.

### C. Escenario 3: Token QR2 Inválido o Ya Utilizado
1. Intente validar nuevamente el QR1 `UUID-ENC-DEMO-001` con el token `DEMO-TOKEN-PAGADA-001` (ya invalidado en el Escenario 1), o ingrese un token erróneo como `TOKEN-FALSO-999`.
2. Presione **Validar Entrega Física**.
3. **Resultado esperado:**
   - Panel rojo de alerta: *"Token QR2 Inválido o Expirado"*.
   - Detalle: *"El código QR2 digital presentado es inválido, ya fue utilizado o no corresponde a esta encomienda"*.

### D. Escenario 4: Procesamiento Masivo por Lotes (Batch Scanning)
1. Diríjase a la pestaña **Lotes (Batch Scanning)**.
2. Seleccione la acción: **Despachar a viaje**.
3. Se desplegará el combo de viajes programados desde la sucursal de origen. Seleccione el viaje disponible.
4. En el campo de escaneo rápido, ingrese varios códigos QR1 presionando `Enter` tras cada uno.
   - Verifique que códigos duplicados se rechacen automáticamente y no se añadan a la lista.
   - El contador en vivo indicará la cantidad de paquetes listos para despacho.
5. Haga clic en **Procesar Lote Masivo**.
6. **Resultado esperado:**
   - Visualización de la barra de progreso.
   - Resumen estadístico: Total procesados, Exitosos, Ya registrados y Errores.
   - Tabla detallada con el estado de cada ítem.
   - Botones disponibles: **Reintentar solo fallidos** y **Exportar reporte CSV** (guardado en `salida/lote_escaneo_*.csv`).

### E. Escenario 5: Contingencia, Búsqueda por DNI y Reimpresión de Etiqueta QR1
1. Diríjase a la pestaña **Contingencia (Búsqueda Doc)**.
2. Ingrese el DNI del cliente demo en el buscador: `45678901` y presione **Buscar**.
3. Se listarán las encomiendas vinculadas al cliente con sus estados logísticos y de pago.
4. Si la etiqueta física está rota o ilegible:
   - Seleccione la encomienda y presione **Reportar Incidencia / Reimprimir**.
   - Seleccione el tipo de incidencia (ej. `ETIQUETA_DANADA`) e ingrese el motivo.
   - Presione **Confirmar Incidencia y Regenerar QR1**.
   - Se mostrará la imagen regenerada del código **QR1 Operativo**.
   - Presione **Imprimir Etiqueta Térmica** para generar el archivo físico simulado en `salida/ticket_*.png`.
