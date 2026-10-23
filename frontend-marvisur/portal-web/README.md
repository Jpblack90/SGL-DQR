# Portal Web del Cliente y Billetera Digital — Expreso Marvisur

Aplicación web Single Page Application (SPA) para clientes de **Expreso Marvisur**, diseñada bajo la filosofía de **Fricción Cero** (sin usuario ni contraseña). Permite el rastreo de encomiendas en tiempo real, consulta del catálogo de agencias y acceso a la **Billetera Digital** mediante el token de retiro en el fragmento (`#`) de la URL.

---

## 1. Stack Tecnológico

- **Framework**: [React 18](https://react.dev/) + [TypeScript](https://www.typescriptlang.org/)
- **Herramienta de compilación**: [Vite 5](https://vitejs.dev/)
- **Enrutamiento**: [React Router 6](https://reactrouter.com/)
- **Estilos e Identidad**: [Tailwind CSS 3](https://tailwindcss.com/) + [@fontsource/poppins](https://fontsource.org/fonts/poppins)
- **Pruebas unitarias y de componentes**: [Vitest](https://vitest.dev/) + [React Testing Library](https://testing-library.com/docs/react-testing-library/intro/) + [jsdom](https://github.com/jsdom/jsdom)
- **Linter**: [ESLint 9](https://eslint.org/) (Flat Config) con soporte TypeScript y React Hooks

---

## 2. Requisitos Previos

- **Node.js**: Versión 20 (LTS) o 22+.
- **npm**: Versión 10 o superior.
- **Backend API**: `marvisur-api` ejecutándose en `http://localhost:8080` (perfil `dev`).

---

## 3. Instalación y Configuración

```bash
# Desde la raíz del repositorio
cd frontend-marvisur/portal-web

# Instalar dependencias
npm install
```

### Variables de Entorno (Opcional)
Por defecto, Vite utiliza rutas relativas (`/api`) redirigidas automáticamente a `http://localhost:8080` mediante el proxy de desarrollo configurado en `vite.config.ts`. Si deseas apuntar a otra URL:

Crea un archivo `.env` en `frontend-marvisur/portal-web`:
```env
VITE_API_BASE_URL=http://localhost:8080
```

---

## 4. Ejecución en Desarrollo

1. **Asegúrate de que la API esté levantada en perfil `dev`:**
   ```bash
   cd backend-marvisur
   mvn spring-boot:run -pl marvisur-api "-Dspring-boot.run.profiles=dev"
   ```
2. **Inicia el servidor de desarrollo del portal:**
   ```bash
   cd frontend-marvisur/portal-web
   npm run dev
   ```
3. **Abre tu navegador en:**
   `http://localhost:5173`

---

## 5. Scripts Disponibles

| Comando | Descripción |
| :--- | :--- |
| `npm run dev` | Inicia el servidor de desarrollo local con Hot Module Replacement (HMR). |
| `npm run build` | Compila TypeScript (`tsc`) y empaqueta la aplicación de producción en `dist/`. |
| `npm run lint` | Ejecuta ESLint sobre todo el código fuente TypeScript/TSX. |
| `npm test` | Ejecuta las pruebas automatizadas con Vitest en modo ejecución única (`run`). |
| `npm run test:watch` | Ejecuta las pruebas interactivas en modo observador. |
| `npm run preview` | Previsualiza localmente el build de producción generado en `dist/`. |

---

## 6. Guion de Prueba Manual (con Datos Demo `docs/dev-demo.md`)

Con la API en ejecución bajo el perfil `dev` y los datos semilla `V100` aplicados:

### Caso 1: Rastreo de Encomienda Pagada (`MV-DEMO-001`)
1. Ingresa a `http://localhost:5173/rastreo`.
2. En **N° de Comprobante**, escribe: `MV-DEMO-001`.
3. En **DNI o RUC**, escribe: `45678902` (DNI del destinatario Luis Alberto Morales).
4. Clic en **Consultar Estado**.
5. **Resultado esperado**:
   - Comprobante: `MV-DEMO-001`.
   - Estado: `EN_ALMACEN_DESTINO` (badge "En almacén destino").
   - Pago: `PAGADO` (badge verde, S/. 16.25).
   - Origen: Lima Central (`A*** M*** F***`) ➔ Destino: Arequipa (`L*** A*** M***`).
   - Línea de tiempo con 3 hitos: Recibido en agencia ➔ En tránsito ➔ En almacén destino.

### Caso 2: Rastreo con Cobro Contra Entrega Pendiente (`MV-DEMO-002`)
1. Ingresa a `http://localhost:5173/rastreo`.
2. En **N° de Comprobante**, escribe: `MV-DEMO-002`.
3. En **DNI o RUC**, escribe: `45678904` (DNI de la destinataria Elena Ramos Castro).
4. Clic en **Consultar Estado**.
5. **Resultado esperado**:
   - Estado de pago: `PENDIENTE`.
   - Alerta visible: **Pago Contra Entrega Pendiente (S/. 17.50)** con indicación de abonar en ventanilla antes del retiro.

### Caso 3: Billetera Digital con QR2 Activo (`MV-DEMO-001`)
1. En tu navegador ingresa directamente a:
   `http://localhost:5173/billetera#DEMO-TOKEN-PAGADA-001`
2. **Resultado esperado**:
   - Se muestra el código QR2 grande en alta resolución generado al vuelo en Base64.
   - Badge: **CÓDIGO ACTIVO PARA RETIRO**.
   - Mensaje de seguridad: **«No compartas este enlace»**.
   - Instrucción para presentar el código en ventanilla de la Agencia Arequipa.

### Caso 4: Billetera Digital con Pago Contra Entrega (`MV-DEMO-002`)
1. Ingresa a:
   `http://localhost:5173/billetera#DEMO-TOKEN-PENDIENTE-002`
2. **Resultado esperado**:
   - Muestra el código QR2 activo y un banner destacado en color ámbar: **Monto a pagar en ventanilla antes de retirar: S/. 17.50**.

### Caso 5: Billetera Digital sin Token o Token Inexistente
1. Ingresa a `http://localhost:5173/billetera` (sin hash).
   - **Resultado**: Mensaje amigable indicando que debe usar el enlace oficial recibido al registrar su encomienda.
2. Ingresa a `http://localhost:5173/billetera#TOKEN-FALSO-123`.
   - **Resultado**: Mensaje de error 404 amigable: "El enlace de retiro o llave digital no existe o ya no se encuentra disponible."

### Caso 6: Red de Agencias
1. Ingresa a `http://localhost:5173/agencias`.
2. **Resultado esperado**:
   - Visualización de tarjetas para Lima Central, Arequipa y Huancayo.
   - Buscador funcional por ciudad ("Huancayo") o nombre de agencia.

---

## 7. Políticas de Seguridad Implementadas

1. **Fricción Cero con Protección Criptográfica**: El token de retiro viaja en el fragmento `#` de la URL, por lo que **nunca es enviado al servidor en cabeceras HTTP de petición de página**.
2. **Cero Persistencia de Llaves**: El token se envía únicamente en el cuerpo del `POST /api/v1/publico/billetera` y **jamás** se guarda en `localStorage`, `sessionStorage` ni `cookies`.
3. **Cero Fugas en Logs**: El token no se escribe en la consola del navegador ni en mensajes de error visibles.
4. **Protección contra Abuso**: Manejo de respuestas `HTTP 429 Too Many Requests` mostrando el tiempo de espera indicado en la cabecera `Retry-After`.
