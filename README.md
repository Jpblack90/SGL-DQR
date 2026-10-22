# SGL-DQR — Sistema de Gestión Logística con Doble QR

**Expreso Marvisur** · Arquitectura Hexagonal · Spring Boot 3 · Java 21 · PostgreSQL · ZXing

---

## API Pública (RF-08, RF-09, RF-10)

La API pública está diseñada bajo la premisa de **Fricción Cero**: permite a los clientes finales rastrear envíos, cotizar tarifas, pre-registrar paquetes y acceder a su Billetera Digital sin requerir usuario ni contraseña.

### Políticas de Seguridad y Privacidad
1. **Sin Autenticación**: Los endpoints bajo `/api/v1/publico/**` no exigen cabecera `Authorization`.
2. **Protección contra Abuso**: Filtro en memoria con limitación de tasa por ventana deslizante (`app.publico.limite-por-minuto=30` por defecto). Al superar la cuota, retorna `HTTP 429 Too Many Requests`, cabecera `Retry-After: <segundos>` y cuerpo RFC 7807 (`ProblemDetail`).
3. **Privacidad de Datos**:
   - Los nombres se anonimizan automáticamente (ej. `J*** P***`).
   - No se retornan documentos de identidad completos, números telefónicos ni correos electrónicos en endpoints públicos.
   - La consulta de tracking requiere comprobante y validación de DNI/RUC del remitente o destinatario.
4. **Billetera Digital Segura**: El token de retiro se envía en el cuerpo de una petición `POST` (nunca en query string ni en URLs). Se busca por su hash SHA-256 en `parejas_doble_qr` y la imagen QR2 se genera al vuelo en Base64. Cero tokens en logs o mensajes de error.
5. **Auditoría**: Las consultas públicas no se persisten en `auditoria_logs` (esta tabla requiere `id_empleado`); se registran en los logs de aplicación sin datos personales ni tokens.

---

### Catálogo de Endpoints Públicos

| Método | Ruta | Descripción | Auth |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/publico/sucursales` | Listar agencias activas para selectores web | Pública |
| `GET` | `/api/v1/publico/rastreo?comprobante=&documento=` | Línea de tiempo y estado con autorización por DNI/RUC | Pública |
| `POST` | `/api/v1/publico/cotizacion` | Cálculo estimado de tarifa según peso en kilogramos | Pública |
| `POST` | `/api/v1/publico/pre-registros` | Crear pre-registro web temporal (*Código Express*) | Pública |
| `POST` | `/api/v1/publico/billetera` | Consultar orden y generar llave digital QR2 al vuelo | Pública |
| `GET` | `/api/v1/preregistros/{codigoExpress}` | Precarga de pre-registro en ventanilla | `ROLE_RECEPCIONISTA`, `ROLE_ADMINISTRADOR` |

---

### Ejemplos de Petición y Respuesta

#### 1. Listar Sucursales
```bash
curl -X GET "http://localhost:8080/api/v1/publico/sucursales" \
     -H "Accept: application/json"
```

**Respuesta (200 OK):**
```json
[
  {
    "id": "suc-001",
    "nombreAgencia": "Agencia Central Lima",
    "ciudad": "Lima",
    "direccion": "Av. Materiales 2215, Cercado de Lima"
  },
  {
    "id": "suc-002",
    "nombreAgencia": "Agencia Arequipa",
    "ciudad": "Arequipa",
    "direccion": "Calle Jacinto Ibáñez 315, Parque Industrial"
  }
]
```

#### 2. Rastreo Público de Encomienda
Requiere número de comprobante y el DNI/RUC del remitente o destinatario.
```bash
curl -X GET "http://localhost:8080/api/v1/publico/rastreo?comprobante=MV-20261022-F47LRX2Q&documento=44556677" \
     -H "Accept: application/json"
```

**Respuesta (200 OK):**
```json
{
  "codigoComprobante": "MV-20261022-F47LRX2Q",
  "estadoLogistico": "EN_RUTA",
  "estadoPago": "PAGADO",
  "montoTotal": 25.00,
  "sucursalOrigen": "Agencia Central Lima",
  "ciudadOrigen": "Lima",
  "sucursalDestino": "Agencia Arequipa",
  "ciudadDestino": "Arequipa",
  "nombreRemitenteEnmascarado": "J*** P***",
  "nombreDestinatarioEnmascarado": "M*** G***",
  "lineaDeTiempo": [
    {
      "estadoEvento": "REGISTRADO",
      "sucursal": "Agencia Central Lima",
      "fechaHora": "2026-10-22T08:30:00"
    },
    {
      "estadoEvento": "EN_RUTA",
      "sucursal": "Agencia Central Lima",
      "fechaHora": "2026-10-22T10:15:00"
    }
  ]
}
```

*Si el comprobante no existe o el documento no coincide:*
```json
{
  "type": "https://marvisur.pe/errores/solicitud",
  "title": "Error de Operación",
  "status": 404,
  "detail": "No se encontró la encomienda con los datos proporcionados"
}
```

#### 3. Cotización Estimada
```bash
curl -X POST "http://localhost:8080/api/v1/publico/cotizacion" \
     -H "Content-Type: application/json" \
     -d '{
       "idSucursalOrigen": "suc-001",
       "idSucursalDestino": "suc-002",
       "pesoKg": 4.5
     }'
```

**Respuesta (200 OK):**
```json
{
  "montoEstimado": 16.25,
  "moneda": "PEN"
}
```

#### 4. Pre-Registro Web (*Código Express*)
```bash
curl -X POST "http://localhost:8080/api/v1/publico/pre-registros" \
     -H "Content-Type: application/json" \
     -d '{
       "remitente": {
         "tipoDocumento": "DNI",
         "numeroDocumento": "44556677",
         "nombresRazonSocial": "Juan Perez",
         "telefono": "987654321",
         "correo": "juan.perez@example.com"
       },
       "destinatario": {
         "tipoDocumento": "DNI",
         "numeroDocumento": "88776655",
         "nombresRazonSocial": "Maria Gomez",
         "telefono": "912345678",
         "correo": "maria.gomez@example.com"
       },
       "idSucursalDestino": "suc-002",
       "pesoEstimado": 5.0
     }'
```

**Respuesta (201 Created):**
```json
{
  "codigoExpress": "PRE-724915-PE",
  "fechaExpiracion": "2026-10-24T08:30:00",
  "montoCotizado": 17.50,
  "pesoEstimado": 5.0,
  "sucursalDestino": "Agencia Arequipa",
  "remitenteEnmascarado": "J*** P***",
  "destinatarioEnmascarado": "M*** G***"
}
```

#### 5. Billetera Digital del Cliente (Retiro con Llave QR2)
El token viaja en el cuerpo mediante POST.
```bash
curl -X POST "http://localhost:8080/api/v1/publico/billetera" \
     -H "Content-Type: application/json" \
     -d '{
       "token": "4a8e9b1c7f3d2e5a6c8b9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a"
     }'
```

**Respuesta (200 OK):**
```json
{
  "codigoComprobante": "MV-20261022-F47LRX2Q",
  "estadoLogistico": "EN_ALMACEN_DESTINO",
  "estadoPago": "PAGADO",
  "montoTotal": 25.00,
  "agenciaDestino": "Agencia Arequipa",
  "ciudadDestino": "Arequipa",
  "estadoToken": "ACTIVO",
  "qr2Base64": "iVBORw0KGgoAAAANSUhEUgAA..."
}
```

#### 6. Precarga de Pre-Registro en Ventanilla (Interno)
```bash
curl -X GET "http://localhost:8080/api/v1/preregistros/PRE-724915-PE" \
     -H "Authorization: Bearer <TOKEN_JWT_TN1_O_TN3>"
```

**Respuesta (200 OK):**
```json
{
  "idPreRegistro": "8a3d4f12-...",
  "codigoExpress": "PRE-724915-PE",
  "remitente": {
    "tipoDocumento": "DNI",
    "numeroDocumento": "44556677",
    "nombresRazonSocial": "Juan Perez",
    "telefono": "987654321",
    "correo": "juan.perez@example.com"
  },
  "destinatario": {
    "tipoDocumento": "DNI",
    "numeroDocumento": "88776655",
    "nombresRazonSocial": "Maria Gomez",
    "telefono": "912345678",
    "correo": "maria.gomez@example.com"
  },
  "idSucursalDestino": "suc-002",
  "pesoEstimado": 5.0,
  "montoCotizado": 17.50,
  "fechaCreacion": "2026-10-22T08:30:00",
  "fechaExpiracion": "2026-10-24T08:30:00",
  "estado": "PENDIENTE_VENTANILLA"
}
```
