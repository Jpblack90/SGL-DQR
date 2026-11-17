# Despliegue de SGL-DQR en Microsoft Azure

SGL-DQR corre en Azure desde el 17/11/2026, en la suscripción **Azure for Students** de la UTP, con HTTPS en la API y en el portal. El despliegue completo es reproducible con [`deploy/azure/desplegar.sh`](../deploy/azure/desplegar.sh), ejecutado en Azure Cloud Shell.

| Pieza | Servicio de Azure | Dirección | Región |
|---|---|---|---|
| Portal web (React) | Static Web Apps, plan Free | <https://gray-sky-0b9d5fb1e.5.azurestaticapps.net> | Distribución global |
| API (Spring Boot, Java 21) | App Service Linux, plan B1 | <https://sgl-dqr-api-318404.azurewebsites.net> | Mexico Central |
| Base de datos | PostgreSQL – Servidor flexible 16, Burstable B1ms, 32 GB | `sgl-dqr-pg-318404.postgres.database.azure.com` | Mexico Central |
| Escritorio JavaFX | Se instala en cada PC de ventanilla y almacén | Se conecta a la API por HTTPS | — |

Todos los recursos están en el grupo `sgl-dqr-rg`, con la etiqueta `proyecto=SGL-DQR`.

---

## 1. Arquitectura

```
 Navegador del cliente ──HTTPS──► Static Web Apps (portal React)
          │                               
          └──────────HTTPS (CORS)──────────► App Service (API Spring Boot) ──TLS──► PostgreSQL flexible
 Escritorio JavaFX (ventanilla/almacén) ──HTTPS──┘
```

- El portal es estático: el navegador llama a la API directamente; la API solo acepta llamadas del dominio del portal (CORS).
- La base tiene dirección pública, pero su cortafuegos solo admite conexiones que vienen de servicios de Azure (regla `azure-access`, 0.0.0.0) y exige TLS.
- La API y la base están en Mexico Central, la región permitida más cercana a Perú.

## 2. Configuración de seguridad

| Medida | Cómo está configurada |
|---|---|
| HTTPS obligatorio | `httpsOnly=true` en la API (HTTP responde 301 a HTTPS); TLS 1.2 como mínimo; Static Web Apps solo sirve HTTPS |
| Secretos fuera del código | Contraseña de la base escrita por el responsable en Cloud Shell (`read -s`, nunca se muestra) y `JWT_SECRET` aleatorio de 384 bits generado con `openssl`; ambos solo en la configuración de la API |
| Conexión cifrada a la base | `sslmode=require` en la URL JDBC |
| Superficie mínima | Swagger y la documentación OpenAPI desactivados; `/actuator/health` responde solo `UP`/`DOWN`, sin detalles internos; FTP desactivado |
| Abuso de endpoints públicos | Límite de 30 solicitudes por minuto por IP, leyendo la IP real del cliente detrás del proxy de Azure |
| Cabeceras del portal | `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy` y `Permissions-Policy` ([`staticwebapp.config.json`](../deploy/azure/staticwebapp.config.json)) |
| Copias de la base | Copias automáticas de Azure con restauración a cualquier punto de los últimos 7 días |
| Disponibilidad | «Always On» activo en la API y comprobación de salud en `/actuator/health` |

La API usa el perfil `dev` para cargar el catálogo y los usuarios de demostración (migración V100): 4 agencias, rutas, flota, un usuario por rol y encomiendas de ejemplo. Las contraseñas de esos usuarios están en el repositorio, así que **deben cambiarse antes de entregar el sistema a Marvisur** (o desplegar con el perfil `prod`, que solo crea el administrador inicial).

## 3. Cómo se despliega

En [Azure Cloud Shell](https://shell.azure.com) (Bash), con la cuenta que tiene la suscripción:

```bash
git clone -b feature/M1-qr-impreso https://github.com/Jpblack90/SGL-DQR.git
cd SGL-DQR
bash deploy/azure/desplegar.sh
```

El script descarga Java 21 si hace falta, compila la API, crea los recursos que falten, publica el portal y la API, y espera a que la API responda. Volver a ejecutarlo publica una versión nueva sin borrar datos.

## 4. Escritorio JavaFX contra Azure

Desde la carpeta del proyecto en Windows:

```powershell
.\scripts\demo-escritorio.ps1 -ApiBaseUrl https://sgl-dqr-api-318404.azurewebsites.net
```

Para el paquete portátil (`scripts/empaquetar-escritorio.ps1`), copiar [`deploy/azure/escritorio.properties`](../deploy/azure/escritorio.properties) junto al ejecutable `SGL-DQR.exe`.

## 5. Verificación (17/11/2026)

| Prueba | Resultado |
|---|---|
| `GET /actuator/health` | `UP`, con la base PostgreSQL conectada |
| `GET /api/v1/publico/sucursales` | 200 |
| Rastreo público `MV-DEMO-002` / `45678904` | Línea de tiempo con nombres enmascarados (`C*** A*** S***`) |
| CORS desde el portal | `Access-Control-Allow-Origin` = dominio del portal |
| Rutas del portal `/`, `/rastreo`, `/billetera`, `/cotiza` | 200 |
| `http://` de la API | 301 a `https://` |

## 6. Costos y apagado

Costo aproximado de lista: App Service B1 unos 13 USD al mes y PostgreSQL B1ms con 32 GB unos 16 USD al mes; Static Web Apps Free no cuesta. Se descuenta del crédito de Azure for Students, compartido con otros proyectos de la misma cuenta.

| Tarea | Comando en Cloud Shell |
|---|---|
| Pausar la base cuando no se usa | `az postgres flexible-server stop -g sgl-dqr-rg -n sgl-dqr-pg-318404` |
| Volver a encenderla | `az postgres flexible-server start -g sgl-dqr-rg -n sgl-dqr-pg-318404` |
| Ver los registros de la API | `az webapp log tail -g sgl-dqr-rg -n sgl-dqr-api-318404` |
| Borrar todo al terminar el curso | `az group delete -n sgl-dqr-rg` |

## 7. Problemas encontrados

| Problema | Causa | Solución |
|---|---|---|
| «Operation cannot be completed without additional quota» al crear el plan B1 en West US 2 | Azure for Students no tiene cuota de B1 en todas las regiones permitidas | Se creó en Mexico Central, que sí tenía cuota |
| `--database-name` rechazado al crear PostgreSQL | En la versión actual de la CLI ese parámetro es solo para clústeres elásticos | Crear el servidor y luego `az postgres flexible-server db create -s <servidor> -n marvisur_db` |
| Cloud Shell solo trae Java 17 | La API necesita Java 21 | El script descarga Temurin 21 en la sesión |
