# Kit de Demostración — SGL-DQR (Expreso Marvisur)

Este directorio contiene los scripts de automatización para ejecutar, validar y demostrar el ecosistema completo **SGL-DQR** (Sistema de Gestión Logística con Doble QR) en entornos locales y de laboratorio.

Se incluyen versiones nativas para **Windows PowerShell (`.ps1`)** y para **Linux / macOS (`.sh`)**.

---

## Índice de Scripts

| Script | Función Principal | Parámetros / Opciones |
|---|---|---|
| `demo-reset` | Reinicia PostgreSQL y recrea volúmenes limpios (`docker compose down -v && up -d`), esperando a que la BD esté saludable en puerto 5433. | Ninguno |
| `demo-api` | Arranca `marvisur-api` en perfil `dev` (aplica migraciones V1–V8 + seed V100). Auto-detecta la IP LAN o usa `-PortalHost`. | `-PortalHost <IP>` (opcional)<br>`-Background` (PowerShell) / `BACKGROUND=true` (Bash) |
| `demo-portal` | Ejecuta el portal web React + Vite expuesto en red local (`npm run dev -- --host`). | Ninguno |
| `demo-escritorio` | Inicia la aplicación de escritorio JavaFX pasando la URL de la API. | `-ApiBaseUrl <URL>` (default: `http://localhost:8080`) |
| `demo-parar-api` | Detiene **exclusivamente** el proceso de la API (liberando puerto 8080) para simular la **caída de red (escena offline)**. | Ninguno |
| `demo-iniciar-api` | Inicia o recupera la API backend, esperando el estado UP en `/actuator/health` para la **escena de reconexión y sincronización**. | `-PortalHost <IP>` (opcional) |
| `informe-metricas` | Analiza y extrae automáticamente las métricas reales del proyecto (tests, OpenAPI, BD, LOC, git) generando `docs/informe-metricas.md`. | Ninguno |

---

## Requisitos Previos

1. **Docker Desktop** (o Docker Engine en Linux) en ejecución.
2. **Java 21** (`JAVA_HOME` apuntando a JDK 21).
3. **Apache Maven 3.9+** en el `PATH`.
4. **Node.js 20+** y `npm`.

---

## Guion de Demostración Paso a Paso (Flujo Completo E2E)

### Escena 1: Preparación y Reset del Entorno
Limpia la base de datos de desarrollo y levanta PostgreSQL fresco:
```powershell
# En Windows:
.\scripts\demo-reset.ps1

# En Linux / macOS:
./scripts/demo-reset.sh
```

### Escena 2: Inicio de Servicios Centrales (API y Portal Web)
1. En una primera terminal, inicia la API:
   ```powershell
   .\scripts\demo-api.ps1
   # O especificando IP LAN manual: .\scripts\demo-api.ps1 -PortalHost 192.168.1.100
   ```
   *La API cargará automáticamente los datos semilla de demo (`V100__datos_demo.sql`) con agencias, rutas, vehículos, usuarios TN1–TN4 y encomiendas precalculadas.*

2. En una segunda terminal, inicia el portal web:
   ```powershell
   .\scripts\demo-portal.ps1
   ```
   *Accede desde el navegador en `http://localhost:5173` o desde un teléfono móvil en la red local (`http://<IP-LAN>:5173`).*

### Escena 3: Operación en Ventanilla (Escritorio JavaFX)
En una tercera terminal, lanza la aplicación de escritorio:
```powershell
.\scripts\demo-escritorio.ps1
```
1. Inicia sesión con:
   - **Usuario**: `recepcionista` / **Contraseña**: `recep123` (Rol TN1).
2. Selecciona **Ventanilla**:
   - Registra una nueva encomienda.
   - Observa la emisión del ticket térmico en `frontend-marvisur/escritorio/salida/ticket_*.png`.
   - Abre el enlace de la Billetera Digital en el navegador para ver el QR2 generado.

### Escena 4: Simulación de Caída de Red y Modo Offline (RNF-02, O1)
1. En una cuarta terminal, ejecuta el script de corte:
   ```powershell
   .\scripts\demo-parar-api.ps1
   ```
2. Observa cómo la aplicación JavaFX en **$\le 2$ segundos**:
   - Cambia el indicador superior de `🟢 En línea` a `🔴 Sin conexión`.
   - Muestra aviso de «Modo Offline Activo».
   - Bloquea pestañas que requieren conexión remota (código express, almacén) con aviso informativo.
3. Registra una encomienda sin conexión:
   - Los datos se guardan en la cola local SQLite (`datos-locales/marvisur-local.db`).
   - El código QR físico y el enlace digital se calculan localmente mediante `MotorQR`.
   - El contador de pendientes en la cabecera se actualiza a `· Pendientes: 1`.

### Escena 5: Reconexión y Sincronización Automática (O2, B4)
1. Restablece la API ejecutando:
   ```powershell
   .\scripts\demo-iniciar-api.ps1
   ```
2. En la aplicación JavaFX:
   - En cuanto la API responde `UP`, el indicador vuelve a `🟢 En línea`.
   - El sincronizador en segundo plano detecta la red, envía la encomienda a `POST /api/v1/sync/encomiendas`.
   - El estado en la cola pasa a `SINCRONIZADA` y el contador de pendientes se limpia.
   - En el portal web, el código comprobante ya es rastreable en `http://localhost:5173/rastreo`.

### Escena 6: Entrega Física en Almacén (Doble QR y Cobro Contra Entrega)
1. Inicia sesión en el escritorio con `almacenero` / `almacen123` (Rol TN2).
2. Pestaña **Entrega Física**:
   - Escanea el QR1 del paquete físico.
   - Escanea el QR2 desde la Billetera Digital del cliente.
   - Si la encomienda es Contra Entrega, el sistema exige el cobro en ventanilla antes de autorizar.
   - Tras la entrega exitosa, el QR2 queda **irreversiblemente invalidado** (RNF-01).
