# Datos de Demostración (Seed DEV) — SGL-DQR

> [!CAUTION]
> **SOLO DESARROLLO; NUNCA EN PRODUCCIÓN.**
> Las credenciales, tokens en claro y llaves de prueba documentados en este archivo son exclusivamente para desarrollo local, pruebas manuales y demostraciones académicas. Están prohibidos en entornos de producción.

---

## 1. Cuentas de Acceso (Usuarios Demo)

Todos los usuarios de demostración tienen la contraseña común:
**`Demo1234*`** (hasheada en BD con BCrypt, factor de coste 10).

| Rol | Identificador | Correo | Sucursal Asignada | Propósito Demo |
| :--- | :--- | :--- | :--- | :--- |
| **TN1 Recepcionista** | `EMP-DEMO-TN1` | `recepcionista.demo@marvisur.pe` | Lima Central (`SUC-LIM-01`) | Registro en ventanilla, cobro y atención con Código Express |
| **TN2 Almacenero** | `EMP-DEMO-TN2` | `almacenero.demo@marvisur.pe` | Arequipa (`SUC-AQP-01`) | Recepción, validación cruzada Doble QR y entrega |
| **TN3 Administrador** | `EMP-DEMO-TN3` | `admin.demo@marvisur.pe` | Lima Central (`SUC-LIM-01`) | Gestión global, auditoría y control de incidencias |
| **TN4 Chofer** | `EMP-DEMO-TN4` | `chofer.demo@marvisur.pe` | Lima Central (`SUC-LIM-01`) | Visualización de viajes y asignación de flotas |

---

## 2. Sucursales de Demostración

| ID Sucursal | Nombre Agencia | Ciudad | Dirección |
| :--- | :--- | :--- | :--- |
| `SUC-LIM-01` | Agencia Lima Central | Lima | Av. Grau 450, Cercado de Lima |
| `SUC-AQP-01` | Agencia Arequipa | Arequipa | Calle Jacinto Ibañez 315, Parque Industrial |
| `SUC-HYO-01` | Agencia Huancayo | Huancayo | Av. Ferrocarril 1020 |

---

## 3. Encomiendas Demo y Llaves Doble QR

### Encomienda 1: PAGADA (Lista para entrega física)
- **Código Comprobante:** `MV-DEMO-001`
- **ID Encomienda:** `UUID-ENC-DEMO-001`
- **Origen:** Lima Central (`SUC-LIM-01`) | **Destino:** Arequipa (`SUC-AQP-01`)
- **Estado Logístico:** `EN_ALMACEN_DESTINO`
- **Estado de Pago:** `PAGADO` (S/. 16.25)
- **Remitente:** Ana María Flores (DNI `45678901`) | **Destinatario:** Luis Alberto Morales (DNI `45678902`)
- **QR1 Físico (Operativo):**
  - **Texto exacto para el campo QR1:** `MARVISUR:QR1:UUID-ENC-DEMO-001` *(también admite `UUID-ENC-DEMO-001`)*
  - Columna en BD (`parejas_doble_qr.uuid_qr1_operativo`): `MARVISUR:QR1:UUID-ENC-DEMO-001`
- **QR2 Digital (Billetera / Llave de Retiro):**
  - **Texto exacto para el campo QR2 (token en claro):** `DEMO-TOKEN-PAGADA-001`
  - Hash SHA-256 en BD (`parejas_doble_qr.hash_qr2_llave_digital`): `7e62e05647a89c6d03218739d3ae6d620eab27978620e12882a43fe68d3ecab2`
  - Estado del Token: `ACTIVO`
  - URL Billetera demo: `http://localhost:5173/billetera#DEMO-TOKEN-PAGADA-001`
- **Resultado esperado en validación cruzada:**
  - `200 OK` (`EXITOSO`): Entrega física autorizada, token QR2 pasa a `INVALIDADO` y encomienda a `ENTREGADO`.
  - Reintento con el mismo QR2: `409 Conflict` (`TOKEN_QR2_INVALIDO`).

---

### Encomienda 2: CONTRA ENTREGA PENDIENTE (Bloqueo de entrega)
- **Código Comprobante:** `MV-DEMO-002`
- **ID Encomienda:** `UUID-ENC-DEMO-002`
- **Origen:** Lima Central (`SUC-LIM-01`) | **Destino:** Arequipa (`SUC-AQP-01`)
- **Estado Logístico:** `EN_ALMACEN_DESTINO`
- **Estado de Pago:** `PENDIENTE` (S/. 17.50)
- **Remitente:** Comercializadora Andina S.A.C. (RUC `20601234567`) | **Destinatario:** Elena Ramos Castro (DNI `45678904`)
- **QR1 Físico (Operativo):**
  - **Texto exacto para el campo QR1:** `MARVISUR:QR1:UUID-ENC-DEMO-002` *(también admite `UUID-ENC-DEMO-002`)*
  - Columna en BD (`parejas_doble_qr.uuid_qr1_operativo`): `MARVISUR:QR1:UUID-ENC-DEMO-002`
- **QR2 Digital (Billetera / Llave de Retiro):**
  - **Texto exacto para el campo QR2 (token en claro):** `DEMO-TOKEN-PENDIENTE-002`
  - Hash SHA-256 en BD (`parejas_doble_qr.hash_qr2_llave_digital`): `5871ddaceca42227833c9d428f56f2225d6d074203f6c33f854f93a6f9b9caf4`
  - Estado del Token: `ACTIVO`
  - URL Billetera demo: `http://localhost:5173/billetera#DEMO-TOKEN-PENDIENTE-002`
- **Resultado esperado en validación cruzada:**
  - **1º Intento:** `409 Conflict` (`ENTREGA_BLOQUEADA_PAGO_PENDIENTE`), indicando monto pendiente S/. 17.50.
  - **Cobro previo:** Registrar cobro en ventanilla/almacén mediante botón «Registrar cobro» o `POST /api/v1/encomiendas/UUID-ENC-DEMO-002/cobro` (pasa `estado_pago` a `PAGADO`).
  - **2º Intento (Reintento tras cobro):** `200 OK` (`EXITOSO`), entrega física autorizada y token QR2 pasa a `INVALIDADO`.
  - **Reintento posterior:** `409 Conflict` (`TOKEN_QR2_INVALIDO`).
  - **Rastreo público:** `GET /api/v1/publico/rastreo?comprobante=MV-DEMO-002&documento=45678904` responde trazabilidad completa y estado `ENTREGADO`.

---

### Encomienda 3: REGISTRADA (En origen)
- **Código Comprobante:** `MV-DEMO-003`
- **ID Encomienda:** `UUID-ENC-DEMO-003`
- **Origen:** Lima Central (`SUC-LIM-01`) | **Destino:** Huancayo (`SUC-HYO-01`)
- **Estado Logístico:** `REGISTRADO`
- **Estado de Pago:** `PAGADO` (S/. 10.00)
- **Remitente:** Ana María Flores (DNI `45678901`) | **Destinatario:** Elena Ramos Castro (DNI `45678904`)
- **QR1 Físico (Operativo):**
  - **Texto exacto para el campo QR1:** `MARVISUR:QR1:UUID-ENC-DEMO-003` *(también admite `UUID-ENC-DEMO-003`)*
  - Columna en BD (`parejas_doble_qr.uuid_qr1_operativo`): `MARVISUR:QR1:UUID-ENC-DEMO-003`
- **QR2 Digital (Billetera / Llave de Retiro):**
  - **Texto exacto para el campo QR2 (token en claro):** `DEMO-TOKEN-REGISTRADA-003`
  - Hash SHA-256 en BD (`parejas_doble_qr.hash_qr2_llave_digital`): `4a0ac62303a57ec9a47bcd87c159b4a16c283812070a4bedfb9456fffe8b0b76`
  - Estado del Token: `ACTIVO`
  - URL Billetera demo: `http://localhost:5173/billetera#DEMO-TOKEN-REGISTRADA-003`
- **Resultado esperado en validación cruzada:**
  - `409 Conflict` (`TRANSICION_INVALIDA`): La encomienda no se encuentra en `EN_ALMACEN_DESTINO` (estado actual: `REGISTRADO`).

---

## 4. Pre-Registro Web ('Código Express') Vigente
- **Código Express:** `PRE-DEMO01-PE`
- **ID Pre-Registro:** `PRE-DEMO-001`
- **Destino:** Arequipa (`SUC-AQP-01`)
- **Remitente:** Ana María Flores (DNI `45678901`) | **Destinatario:** Luis Alberto Morales (DNI `45678902`)
- **Peso Estimado:** 5.00 kg | **Monto Cotizado:** S/. 17.50
- **Estado:** `PENDIENTE_VENTANILLA`
- **Uso en Ventanilla:** Se consulta en `GET /api/v1/preregistros/PRE-DEMO01-PE` y se asocia en `POST /api/v1/encomiendas` mediante el campo `codigoExpress`.

---

## 5. Rutas, Flota y Viajes Programados

- **Vehículos:**
  - `VEH-DEMO-01` (Placa `V1A-100`, Volvo FH 460, 15 Toneladas)
  - `VEH-DEMO-02` (Placa `V2B-200`, Scania R500, 18 Toneladas)
- **Rutas:**
  - `RUT-LIM-AQP` (Lima -> Arequipa, 1010.5 km)
  - `RUT-LIM-HYO` (Lima -> Huancayo, 305.0 km)
  - `RUT-AQP-LIM` (Arequipa -> Lima, 1010.5 km)
  - `RUT-HYO-LIM` (Huancayo -> Lima, 305.0 km)
- **Viajes Programados:**
  - `VIAJE-DEMO-01` (Ruta Lima-Arequipa, Chofer Jorge Huamán)
  - `VIAJE-DEMO-02` (Ruta Lima-Huancayo, Chofer Jorge Huamán)
  - `VIAJE-DEMO-03` (Ruta Arequipa-Lima, Chofer Jorge Huamán)
  - `VIAJE-DEMO-04` (Ruta Huancayo-Lima, Chofer Jorge Huamán)
