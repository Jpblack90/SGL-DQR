# Cliente de Escritorio JavaFX — SGL-DQR (Expreso Marvisur)

Módulo de ventanilla y operaciones logísticas para **Expreso Marvisur S.A.**, construido en Java 21 y JavaFX. Consume la API REST central (`marvisur-api`) siguiendo una arquitectura desacoplada basada en puertos, diseñada para incorporar sincronización offline con SQLite en fases posteriores.

---

## 1. Características Implementadas (Fase D1 + D2)

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
app.nombre=Expreso Marvisur - Ventanilla SGL-DQR
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
| **TN1 Recepcionista** | `recepcionista.demo@marvisur.pe` | `Demo1234*` | Lima Central (`SUC-LIM-01`) | Ventanilla (activo) |
| **TN2 Almacenero** | `almacenero.demo@marvisur.pe` | `Demo1234*` | Arequipa (`SUC-AQP-01`) | Almacén (próximamente) |
| **TN3 Administrador** | `admin.demo@marvisur.pe` | `Demo1234*` | Lima Central (`SUC-LIM-01`) | Todos los módulos |
| **TN4 Chofer** | `chofer.demo@marvisur.pe` | `Demo1234*` | Lima Central (`SUC-LIM-01`) | Rutas (próximamente) |

---

## 4. Guía de Prueba Manual Paso a Paso

1. **Iniciar Backend API:**
   ```bash
   mvn spring-boot:run -pl backend-marvisur/marvisur-api
   ```
2. **Iniciar App de Escritorio:**
   ```bash
   mvn javafx:run -pl frontend-marvisur/escritorio
   ```
3. **Inicio de Sesión:**
   - Ingrese `recepcionista.demo@marvisur.pe` y `Demo1234*`.
   - Presione *Iniciar Sesión*. Verifique que ingrese a la pantalla principal y muestre la barra superior con el nombre del usuario, rol `TN1`, sucursal `SUC-LIM-01` y el punto verde *En línea*.
4. **Carga con Código Express (Opcional):**
   - Ingrese en el campo superior derecho un código de pre-registro (ej. generado previamente en el portal web).
   - Presione *Cargar*. Verifique que los datos de remitente, destinatario, agencia y peso se completen automáticamente.
5. **Registro Manual y Cotización:**
   - Ingrese DNI `45678901` y nombre del Remitente.
   - Ingrese DNI `45678902` y nombre del Destinatario.
   - Seleccione la *Agencia Destino* (ej. `Agencia Arequipa`).
   - Ingrese *Peso (Kg)*: `5.0`.
   - Presione *Cotizar Tarifa*. Verifique que la tarifa estimada se calcule (ej. S/. 17.50).
   - Seleccione la modalidad *Contado* o *Contra Entrega*.
6. **Emisión de Doble QR:**
   - Presione *Registrar Encomienda*.
   - Se abrirá la tarjeta modal de confirmación con:
     - Código de Comprobante generado (ej. `MV-20261022-XXXX`).
     - Imagen del **QR1 Físico** (para rotular el paquete).
     - Imagen del **QR2 Digital** con el enlace de retiro y botón *Copiar Enlace*.
     - Estado de Pago y Monto.
7. **Impresión Térmica:**
   - Presione *🖨️ Imprimir Ticket Térmico*.
   - Verifique que se cree el archivo PNG en la carpeta `salida/ticket_*.png` con el formato y ancho térmico estándar de 58 mm.
8. **Cierre de Diálogo:**
   - Presione *Cerrar y Nuevo Registro*. Verifique que el formulario se limpie y la variable en memoria de `qr2Token` quede purgada.
