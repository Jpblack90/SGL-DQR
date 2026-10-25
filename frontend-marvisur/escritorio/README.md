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

### Empaquetado Autocontenido y Distribución Portátil (RNF-04 / Tarea O4)
La aplicación puede empaquetarse como un paquete ejecutable autocontenido con su propio JRE mínimo embebido (Java 21 + JavaFX 21), permitiendo su uso en cualquier equipo Windows sin necesidad de tener Java ni Maven instalados.

#### 1. Cómo empaquetar la aplicación
Ejecute el script automatizado desde PowerShell:
```powershell
.\scripts\empaquetar-escritorio.ps1
```
El script realiza automáticamente:
1. Compilación del JAR principal (`mvn package -DskipTests`) y copia de dependencias de ejecución a `target/libs`.
2. Generación del JRE mínimo embebido con `jlink` conteniendo únicamente los módulos necesarios (`javafx.controls`, `javafx.fxml`, `javafx.graphics`, `javafx.base`, `java.desktop`, `java.net.http`, `java.sql`, `jdk.crypto.ec`, etc.).
3. Empaquetado de la aplicación con `jpackage --type app-image` hacia `frontend-marvisur/escritorio/target/dist/SGL-DQR/`.
4. Si **WiX Toolset** (3.11+) está instalado (`candle.exe` en `PATH`), genera adicionalmente el instalador `.msi` con acceso directo y entrada en el Menú Inicio.

#### 2. Cómo configurar la URL de la API Central
Al correr empaquetada, la aplicación resuelve la URL de la API en el siguiente orden de prioridad:
1. **Argumento CLI:** `--api.base-url=http://servidor:8080` al invocar `SGL-DQR.exe`.
2. **Propiedad de Sistema:** `-Dmarvisur.api.base-url=http://servidor:8080`.
3. **Archivo de configuración externo:** Archivo `escritorio.properties` ubicado en la misma carpeta que `SGL-DQR.exe`:
   ```properties
   api.base-url=http://servidor-central.marvisur.pe:8080
   http.timeout-segundos=2
   ```
4. **Valor por defecto embebido:** `http://localhost:8080`.

#### 3. Dónde se guardan los datos al correr empaquetada
Para garantizar que la aplicación funcione correctamente incluso si se instala en directorios de solo lectura (como `C:\Program Files\`), el almacenamiento local se redirige automáticamente al perfil del usuario:
- **Base de datos SQLite local (`marvisur-local.db`):**
  `%LOCALAPPDATA%\SGL-DQR\datos-locales\marvisur-local.db`
- **Archivos de salida (tickets térmicos PNG y reportes CSV de lotes):**
  `%LOCALAPPDATA%\SGL-DQR\salida\`

*(Nota: En modo desarrollo al correr con Maven, las rutas relativas `datos-locales/` y `salida/` se mantienen en la raíz del proyecto para facilitar depuración).*

#### 4. Cómo desinstalar o limpiar datos
- **Eliminar la aplicación portátil:** Simplemente borre la carpeta `SGL-DQR` de `target/dist/` o donde la haya descomprimido.
- **Limpiar datos locales, cola SQLite y tickets:** Elimine la carpeta de datos en `%LOCALAPPDATA%\SGL-DQR` ejecutando en PowerShell:
  ```powershell
  Remove-Item -Recurse -Force "$env:LOCALAPPDATA\SGL-DQR"
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
   MARVISUR:QR1:UUID-ENC-DEMO-001
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
   - QR1 Físico: `MARVISUR:QR1:UUID-ENC-DEMO-002` (o `UUID-ENC-DEMO-002`)
   - QR2 Digital: `DEMO-TOKEN-PENDIENTE-002`
2. Presione **Validar Entrega Física**.
3. **Resultado esperado (Bloqueo 409):**
   - Panel ámbar: *"Entrega Bloqueada por Pago Pendiente"*.
   - Monto a cobrar: `S/. 17.50`.
   - Botón habilitado: **Registrar cobro y autorizar**.
4. Haga clic en **Registrar cobro y autorizar**:
   - El sistema registra el cobro en la API (`POST /api/v1/encomiendas/{id}/cobro`).
   - El botón cambia a **Reintentar validación** (conservando los códigos QR en memoria sin requerir nuevo escaneo).
5. Haga clic en **Reintentar validación**:
   - La API ahora autoriza la entrega con éxito (200 OK).
   - Panel verde de confirmación con comprobante `MV-DEMO-002`.
   - Los secretos se limpian inmediatamente de la memoria RAM.

### C. Escenario 3: Token QR2 Inválido o Ya Utilizado
1. Intente validar nuevamente el QR1 `MARVISUR:QR1:UUID-ENC-DEMO-001` con el token `DEMO-TOKEN-PAGADA-001` (ya invalidado en el Escenario 1), o ingrese un token erróneo como `TOKEN-FALSO-999`.
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

---

## 5. Modo Sin Conexión (Offline-First) y Cola Local SQLite (O1, O2)

### A. Alcance Honesto del Modo Offline
- **Operaciones Soportadas Sin Conexión:**
  - **Registro de Encomiendas en Ventanilla:** Emisión completa local de la encomienda con generación offline de identificador único (idEncomienda UUID), código de comprobante oficial (MV-yyyyMMdd-XXXXXXXX), cálculo de tarifa con TarifarioPorPeso (tarifa base S/. 5.00 + S/. 2.50/kg) y generación criptográfica del par Doble QR con MotorQR de marvisur-core.
  - **Persistencia en Cola Local SQLite:** Almacenamiento seguro en datos-locales/marvisur-local.db (tabla cola_registro) con estado PENDIENTE.
  - **Caché Local de Sucursales:** El combo de sucursales destino se alimenta de cache_sucursales, sincronizada en cada arranque con conexión.
- **Operaciones NO Disponibles Sin Conexión (Muestran «Requiere conexión»):**
  - **Login nuevo:** El inicio de sesión inicial requiere conexión con el servidor central; la sesión previamente iniciada se mantiene en memoria sin conexión.
  - **Código Express:** La consulta y precarga de pre-registros web requiere conectividad central.
  - **Almacén (Entrega Física, Lotes Masivos y Contingencia):** Requieren validación contra la base de datos central para invalidar el token o registrar incidencias.
- **Esquema de Persistencia Local Mínimo:**
  - Base SQLite en datos-locales/marvisur-local.db con solo 2 tablas operativas: cola_registro (id, payload, estado, intentos, ultimo_error, creado_en, actualizado_en) y cache_sucursales (id, nombre_agencia, ciudad, direccion, actualizado_en).
- **Seguridad Cero Conocimiento (Zero Knowledge en Disco):**
  - El archivo binario .db almacena **únicamente hashes SHA-256** (uuidQr1, hashQr2).
  - El token qr2Token en texto claro jamás se persiste en SQLite, disco ni registros de logs. Reside exclusivamente en la memoria RAM del diálogo de confirmación mientras la ventana está abierta.

---

### B. Guía de Prueba Manual del Modo Offline y Sincronización Automática

1. **Paso 1 — Iniciar sesión con conexión:**
   - Inicie el backend central (marvisur-api) y la app de escritorio.
   - Inicie sesión como **TN1 Recepcionista** (ecepcionista.demo@marvisur.pe / Demo1234*).
   - Observe el indicador superior en verde: **«En línea»**.
2. **Paso 2 — Simular caída del servidor / desconexión:**
   - Detenga el backend central (Ctrl+C en la terminal del backend o apague el servicio).
   - Observe la cabecera: en **<= 2 segundos** (RNF-02) el indicador conmuta automáticamente a punto rojo: **«Sin conexión»**.
3. **Paso 3 — Verificar pestañas bloqueadas:**
   - Intente precargar un Código Express en Ventanilla o ingrese al módulo Almacén e intente validar una entrega: el sistema desplegará una alerta explicativa: *«Requiere conexión: esta operación requiere conexión activa con el servidor central»*.
4. **Paso 4 — Registrar encomienda sin conexión:**
   - En la pestaña **Ventanilla**, ingrese los datos del remitente, destinatario, sucursal destino (cargada desde la caché local SQLite) y peso del paquete.
   - Haga clic en **Registrar Encomienda**.
   - El sistema emitirá la encomienda localmente mostrando el modal con:
     - Banner amarillo: *«⚠️ REGISTRADO SIN CONEXIÓN: se sincronizará automáticamente; el enlace de la Billetera funcionará después de sincronizar»*.
     - Imagen del código **QR1 Físico** e impresión de ticket térmico en salida/.
     - Enlace temporal de la Billetera con el token QR2 en el fragmento #.
   - En la cabecera superior se actualizará el contador: **«Sin conexión · Pendientes: 1»**.
5. **Paso 5 — Inspeccionar la pantalla «Cola local»:**
   - En el menú lateral, haga clic en **📋 Cola Local** (módulo accesible para TN1 y TN3).
   - Verifique que la encomienda figura en la tabla con estado **PENDIENTE**, 0 intentos y fecha de creación.
6. **Paso 6 — Reconectar y verificar sincronización automática:**
   - Vuelva a encender el backend marvisur-api.
   - En **<= 2 segundos**, el indicador de la cabecera conmuta automáticamente a **«En línea»** (punto verde).
   - En un lapso de 10 segundos, el hilo de segundo plano (SincronizadorOffline) enviará automáticamente el lote a POST /api/v1/sync/encomiendas.
   - La cabecera superior notificará en verde: **«En línea · Sincronizado: 1 encomienda»**, y el contador de pendientes desaparecerá.
   - En la pantalla **Cola Local**, el estado del registro pasará automáticamente a **SINCRONIZADA**.
7. **Paso 7 — Verificación en base de datos central:**
   - Consulte la base de datos central PostgreSQL (marvisur_db) o consulte el tracking web del comprobante: la encomienda estará registrada con sus hashes de Doble QR intactos y trazabilidad activa.

---

## 6. Guía de Prueba Manual del Dashboard Administrativo (RF-02 - TN3)

### A. Escenario 1: Visualización y Filtros del Dashboard
1. **Inicio de Sesión como Administrador:**
   - Correo: `admin.demo@marvisur.pe` | Clave: `Demo1234*`.
   - En el menú lateral izquierdo, verifique que la opción **📊 Dashboard** se encuentra habilitada funcionalmente (únicamente para el rol `TN3_ADMINISTRADOR`).
2. **Carga Inicial de Indicadores:**
   - Haga clic en **📊 Dashboard**.
   - Verifique en la barra superior:
     - Etiqueta de última actualización: `Actualizado: <hora local>` (en formato `HH:mm:ss` del equipo).
     - Combo de período con valor predeterminado `Hoy`.
     - Combo de sucursal con valor predeterminado `Todas las agencias` y listado de sucursales activas cargado desde la API.
   - Verifique las 5 tarjetas KPI superiores:
     - **Registradas**: total de encomiendas admitidas en el período.
     - **Entregadas**: cantidad de entregas finalizadas con Doble QR.
     - **En ruta**: encomiendas en tránsito logístico hacia destino.
     - **Pendientes de pago**: conteo de paquetes por cobrar y monto acumulado en soles (`Monto: S/ XX.XX` con dos decimales y punto decimal).
     - **Recaudado**: total cobrado en soles (`S/ XXXX.XX`).
   - Verifique los paneles centrales e inferiores:
     - **Gráfico de barras**: distribución actual por estado con etiquetas legibles en español (`Registrado`, `Almacén origen`, `En ruta`, `Almacén destino`, `Entregado`).
     - **Tabla Recaudación por sucursal**: columnas `Agencia`, `Recaudado` y `Pendiente` en soles.
     - **Panel Validaciones Doble QR**: métricas de total, exitosas, rechazadas por pago pendiente, rechazadas por token inválido, latencia promedio y latencia máxima.
     - **Alerta de latencia (RNF-03)**: si la latencia máxima registrada supera los `1000 ms`, el valor se resalta automáticamente en color ámbar (`#F59E0B`).
     - **Tabla Envíos sin movimiento**: encomiendas no entregadas con más de N horas sin eventos (etiqueta `Umbral: 48 horas`), o placeholder informativo `Sin envíos estancados` si la lista está vacía.
     - **Nota al pie**: `Nota: Recaudado cuenta por fecha de registro del envío.`
     - **Privacidad estricta**: verifique que en ninguna sección se muestran nombres, documentos ni teléfonos.

### B. Escenario 2: Interacción con Filtros y Refresco Automático
1. **Filtro de Período y Sucursal:**
   - Cambie el período a **7 días** o **30 días**: verifique que el spinner se activa brevemente y las cifras se actualizan.
   - Seleccione una sucursal específica (ej. `Agencia Central Lima (SUC-LIM-01)`): verifique que los indicadores se acotan al origen filtrado.
   - Haga clic en **🔄 Actualizar**: dispara una consulta manual fuera del hilo de la interfaz.
2. **Refresco Periódico de 30 Segundos:**
   - Mantenga abierta la pestaña del Dashboard sin interactuar: cada 30 segundos se ejecuta automáticamente una consulta en segundo plano y se actualiza la hora local.
   - Si una consulta está en curso, la siguiente se omite de forma segura sin colisiones.
3. **Cancelación al Cambiar de Pestaña o Cerrar Sesión:**
   - Cambie al módulo **Ventanilla** o **Almacén**: el planificador de 30 segundos se detiene de inmediato sin dejar hilos ni temporizadores huérfanos en segundo plano.
   - Al volver a hacer clic en **📊 Dashboard**, el ciclo de refresco se reactiva limpiamente.

### C. Escenario 3: Comportamiento ante Caída de Red / Modo Sin Conexión
1. **Pérdida de Conectividad con Dashboard Abierto:**
   - Detenga el backend central `marvisur-api`.
   - Haga clic en **🔄 Actualizar** o espere el siguiente ciclo de 30 segundos.
   - **Resultado esperado:**
     - La cabecera superior conmuta a `Sin conexión` (punto rojo).
     - Aparece el banner de advertencia: `Requiere conexión`.
     - La etiqueta de hora cambia a: `Desactualizado (último: <hora>) · Requiere conexión`.
     - Los datos previamente cargados (tarjetas, gráfico, tablas) **se conservan visibles** sin desaparecer ni quedar en blanco.


---

## 7. Guía de Prueba Manual de Gestión de Empleados y Auditoría (RF-03 - TN3)

### A. Escenario 1: Pestaña «Personal» y Operaciones CRUD
1. **Inicio de Sesión y Acceso:**
   - Inicie sesión como Administrador (`admin.demo@marvisur.pe` / `Demo1234*`).
   - En el menú lateral, verifique que la opción **👥 Empleados** está disponible y habilitada exclusivamente para el rol `TN3_ADMINISTRADOR`.
   - Haga clic en **👥 Empleados**: se abrirá la vista con dos pestañas: **Personal** y **Auditoría**.
2. **Búsqueda y Filtros de Personal:**
   - Ingrese un término en el campo de búsqueda (DNI, nombres, apellidos o correo) y presione Enter o haga clic en **🔍 Buscar**.
   - Filtre por estado laboral (`Activos`, `Inactivos` o `Todos`), rol (`TN1 Recepcionista`, `TN2 Almacenero`, etc.) o agencia.
   - Verifique que la tabla se actualiza paginada a 20 registros por página, mostrando los botones de paginación (`Anterior`, `Siguiente`) y el total formateado (`Página 1 de N (Total: M)`).
3. **Registro de Nuevo Colaborador:**
   - Haga clic en **➕ Nuevo Empleado**: se despliega un diálogo modal.
   - Ingrese DNI (8 dígitos exactos), nombres, apellidos, correo corporativo, seleccione rol y agencia, e ingrese una contraseña (mínimo 8 caracteres con al menos una letra y un número) y su confirmación.
   - Verifique la validación previa: si faltan datos o la contraseña no coincide, se muestra un mensaje de alerta local sin realizar peticiones HTTP.
   - Complete datos válidos y confirme: el empleado se crea mediante `POST /api/v1/empleados`, las contraseñas se purgan inmediatamente de la memoria RAM (sin exponerse en logs ni `toString()`), el modal se cierra y la tabla se refresca automáticamente.
4. **Edición de Datos de Colaborador:**
   - Seleccione un empleado en la tabla y haga clic en **✏️ Editar**: se abrirá el diálogo con sus datos precargados.
   - El campo DNI se muestra deshabilitado (el DNI es inmutable).
   - Modifique nombres, apellidos, correo, rol o agencia y guarde los cambios (`PUT /api/v1/empleados/{id}`).
5. **Baja Lógica y Reactivación:**
   - Seleccione un colaborador activo y haga clic en **🚫 Dar de baja**: el sistema solicita confirmación modal antes de proceder (`PATCH /api/v1/empleados/{id}/estado`).
   - Si se intenta dar de baja al propio administrador en sesión o al único administrador activo del sistema, la API rechaza con código 409 y se despliega un mensaje explicativo claro: *«Debe quedar al menos un administrador activo»* o *«No puedes realizar esta acción sobre tu propia cuenta»* (sin contener la palabra `null`).
   - Para un colaborador inactivo, el botón cambia dinámicamente a **✅ Reactivar**.
6. **Restablecimiento Seguro de Contraseña:**
   - Seleccione un empleado activo y haga clic en **🔑 Restablecer Contraseña**.
   - Ingrese la nueva contraseña temporal y su confirmación.
   - Al confirmar (`POST /api/v1/empleados/{id}/password`), la clave se envía de manera segura, se purga de la memoria de inmediato y se notifica el éxito de la operación.

### B. Escenario 2: Pestaña «Auditoría» y Trazabilidad de Operaciones
1. **Consulta y Filtros de Auditoría:**
   - Haga clic en la pestaña **Auditoría**.
   - Seleccione un colaborador específico o mantenga `Todos los colaboradores`.
   - Seleccione una acción específica (ej. `LOGIN_EXITOSO`, `EMPLEADO_CREADO`, `REGISTRO_ENCOMIENDA`, `ENTREGA_VALIDADA`, etc.) o `Todas las acciones`.
   - Seleccione rango de fechas en los componentes `Fecha desde` y `Fecha hasta`: el cliente convierte las fechas automáticamente a inicio y fin del día en la zona horaria oficial `America/Lima` (-05:00) enviándolas en formato ISO 8601.
   - Haga clic en **🔍 Filtrar**: la tabla presenta los registros ordenados descendentemente a 50 ítems por página.
2. **Visualización y Zonas Horarias:**
   - Verifique que la columna `Fecha / Hora` muestra la marca temporal formateada en la zona horaria del equipo local del operador (`dd/MM/yyyy HH:mm:ss`).
   - Verifique las columnas `Colaborador`, `Acción`, `Tabla / Registro Afectado` e `IP Origen`.
3. **Manejo Sin Conexión y Expiración:**
   - Si se pierde la conectividad con el servidor central, se muestra el banner de advertencia `Requiere conexión` y se bloquean las consultas.
   - Si el token JWT de la sesión del administrador expira durante la operación, el cliente captura el error `HTTP 401`, notifica el cierre de sesión y redirige automáticamente a la pantalla de Login.

---

## 8. Guía de Prueba Manual de Rutas y Flota de Transporte (RF-05 - TN3 y TN4)

### A. Escenario 1: Flota y Programación de Viajes (Rol TN3 Administrador)
1. **Inicio de Sesión y Acceso al Módulo:**
   - Inicie sesión como Administrador (`admin.demo@marvisur.pe` / `Demo1234*`).
   - En el menú lateral izquierdo, verifique que la opción **🚚 Rutas / Flota** se encuentra disponible y habilitada.
   - Haga clic en **🚚 Rutas / Flota**: se abrirá la vista con dos pestañas: **Viajes Interprovinciales** y **Flota de Vehículos**.
2. **Gestión de la Flota de Vehículos:**
   - Haga clic en la pestaña **Flota de Vehículos**: se presentará la lista de unidades activas registradas con su placa, modelo/descripción y capacidad de carga útil en kilogramos.
   - Haga clic en **➕ Nuevo Vehículo**: se abre el diálogo modal de registro.
   - Ingrese una placa de rodaje válida (entre 5 y 10 caracteres alfanuméricos en mayúsculas, ej. `V1A-852` o `ABC-123`), modelo del camión (ej. `Volvo FH16`) y capacidad de carga (ej. `25000.00`).
   - Verifique la validación previa: si la placa tiene caracteres inválidos o la capacidad es menor o igual a cero, se notifica una advertencia local.
   - Confirme el registro: la petición se envía mediante `POST /api/v1/vehiculos`. En caso de placa duplicada, el sistema notifica amigablemente: *«Ya existe un vehículo con esa placa»*. Al completarse exitosamente, el modal se cierra y la tabla de vehículos se refresca.
3. **Programación de un Nuevo Viaje:**
   - Diríjase a la pestaña **Viajes Interprovinciales** y haga clic en **➕ Programar Viaje**.
   - Seleccione una ruta interprovincial habilitada (ej. `Lima Central ➔ Arequipa Terminal (1010.50 km)`).
   - Seleccione un vehículo disponible de la flota.
   - Seleccione un chofer calificado (el combo lista exclusivamente colaboradores activos con rol Chofer, formateados con nombres y DNI).
   - Establezca la fecha y hora de salida, así como la fecha y hora estimada de llegada.
   - Verifique la validación local: si la fecha de llegada no es posterior a la salida, se bloquea el envío notificando el error.
   - Confirme el formulario: se envía mediante `POST /api/v1/viajes`, registrándose con estado inicial `PROGRAMADO`. Si el chofer o el vehículo ya tienen un viaje en ruta, se notifica con claridad (*«El chofer ya tiene un viaje en ruta»* o *«El vehículo ya está en un viaje en ruta»*).
4. **Consulta Modal de Manifiesto de Carga (Cero Datos Personales):**
   - Seleccione cualquier viaje de la tabla y haga clic en **📋 Ver manifiesto**.
   - Se desplegará la ventana modal con el listado de encomiendas asignadas al viaje (`Comprobante`, `Estado Logístico`, `Agencia Destino`, `Peso`).
   - **Verificación de Privacidad:** Certifique que no se muestra ningún dato personal (sin DNI, nombres de remitente/destinatario ni teléfonos).
   - En el pie del diálogo, verifique el resumen de totales: número total de encomiendas y sumatoria exacta del peso en kilogramos.
5. **Control de Despacho y Transiciones de Estado:**
   - Con un viaje seleccionado en estado `PROGRAMADO`, verifique que el botón **▶ Iniciar viaje** está habilitado y el botón **🏁 Finalizar viaje** deshabilitado.
   - Haga clic en **▶ Iniciar viaje**: el sistema solicita confirmación modal antes de enviar `PATCH /api/v1/viajes/{id}/estado`. Al confirmar, el estado cambia a `EN_RUTA`.
   - Al volver a seleccionar el viaje, verifique que **▶ Iniciar viaje** queda deshabilitado y **🏁 Finalizar viaje** se habilita.
   - Haga clic en **🏁 Finalizar viaje**: confirme el diálogo modal y el estado se actualizará a `FINALIZADO`, liberando la unidad y al conductor.

### B. Escenario 2: Despacho y Operación de Viajes Asignados (Rol TN4 Chofer)
1. **Inicio de Sesión como Chofer:**
   - Inicie sesión con credenciales de chofer (`chofer.demo@marvisur.pe` / `Demo1234*`).
   - Verifique que la aplicación abre automáticamente el módulo de rutas al iniciar sesión.
2. **Adaptación de Interfaz para Chofer:**
   - El título de la vista se ajusta a: **«Mis Viajes Asignados»**.
   - La pestaña **Flota de Vehículos** se encuentra oculta/removida.
   - El botón **➕ Programar Viaje** se encuentra oculto.
   - La tabla muestra únicamente los viajes asignados al chofer en sesión.
3. **Operaciones Permitidas para Chofer:**
   - Puede consultar el manifiesto de carga de sus viajes (**📋 Ver manifiesto**).
   - Puede despachar e iniciar su viaje (**▶ Iniciar viaje**) y concluirlo (**🏁 Finalizar viaje**).

### C. Escenario 3: Comportamiento ante Caída de Red / Modo Sin Conexión
1. Si se interrumpe la red o se apaga la API central:
   - Al intentar filtrar o actualizar viajes o flota, se despliega el banner de advertencia: *«Requiere conexión: no se puede consultar viajes sin conexión central»*.
   - Ninguna pantalla muestra errores con el texto literal `null` ni excepciones sin procesar.
