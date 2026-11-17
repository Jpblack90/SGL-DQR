# Despliegue de SGL-DQR en Linux (Ubuntu Server)

Guía para desplegar la API, la base de datos y el portal web de SGL-DQR en un servidor Ubuntu Server, por ejemplo una máquina virtual de VirtualBox. La aplicación de escritorio JavaFX no se instala en el servidor: es un cliente que corre en Windows y se conecta a la API.

El procedimiento se ejecutó a mano en **Ubuntu Server 26.04.1 LTS** sobre VirtualBox. El script [`deploy/ubuntu/instalar.sh`](../deploy/ubuntu/instalar.sh) automatiza esos mismos pasos; su sintaxis está validada con `bash -n` y `shellcheck`, pero falta ejecutarlo de punta a punta en una máquina limpia.

---

## 1. Arquitectura

| Pieza | Tecnología | Ubicación en el servidor | Puerto |
|---|---|---|---|
| Base de datos | PostgreSQL | Servicio del sistema, base `marvisur_db` | 5432, solo local |
| API | Java 21 + Spring Boot, ejecutada con pm2 | Repositorio clonado + `deploy/ubuntu/iniciar-api.sh` | 8080 |
| Portal web | React compilado, servido por Nginx | `/var/www/sgl-dqr` | 80 |
| Acceso remoto | OpenSSH | Servicio del sistema | 22 |

Nginx entrega el portal y reenvía las rutas `/api/` a la API, que es la única que habla con PostgreSQL. La API crea las tablas al arrancar con las migraciones de Flyway (V1 a V8) y, con el perfil `dev`, carga los datos de demostración (V100).

---

## 2. Máquina virtual

| Recurso | Valor |
|---|---|
| Sistema | Ubuntu Server 24.04 LTS o superior (ISO `live-server`, sin escritorio) |
| Memoria | 4096 MB (2048 MB como mínimo) |
| Procesadores | 2 |
| Disco | 40 GB, de reserva dinámica |
| Red | NAT con reenvío de puertos |

**Reenvío de puertos (Configuración → Red → Adaptador 1 → Reenvío de puertos):**

| Nombre | Puerto anfitrión | Puerto invitado | Para qué |
|---|---|---|---|
| ssh | 2222 | 22 | `ssh -p 2222 usuario@localhost` desde PowerShell |
| portal | 8088 | 80 | Portal en `http://localhost:8088` |
| api | 8080 | 8080 | Escritorio JavaFX de Windows contra la API |

Desactivar la instalación desatendida de VirtualBox para poder definir las particiones a mano.

### Particiones (instalador → «Custom storage layout»)

| Partición | Tamaño | Formato | Punto de montaje | Función |
|---|---|---|---|---|
| Arranque | 1 GB | ext4 | `/boot` | Archivos de arranque |
| Intercambio | 8 GB | swap | — | Memoria de respaldo, 2 × la RAM |
| Raíz | 20 GB | ext4 | `/` | Sistema operativo y programas |
| Home | el resto | ext4 | `/home` | Archivos de los usuarios |

Marcar **«Install OpenSSH server»** durante la instalación. Comprobar después con `lsblk` y `free -h`.

---

## 3. Instalación automática

Desde PowerShell, entrar al servidor:

```powershell
ssh -p 2222 usuario@localhost
```

Dentro del servidor:

```bash
sudo apt-get update && sudo apt-get install -y git
sudo mkdir -p /opt/sgl-dqr && sudo chown $USER:$USER /opt/sgl-dqr
cd /opt/sgl-dqr
git clone https://github.com/Jpblack90/SGL-DQR.git
cd SGL-DQR
bash deploy/ubuntu/instalar.sh
```

El script hace, en orden:

1. Instala Git, OpenSSH, OpenJDK 21, Maven, Node.js, npm, Nginx, PostgreSQL y pm2.
2. Crea el usuario `marvisur` y la base `marvisur_db`.
3. Crea `deploy/ubuntu/api.env` con la configuración de la API (permiso 600, no se sube a git).
4. Compila la API con Maven (`marvisur-api-*-exec.jar`).
5. Compila el portal con `npm ci` y `npm run build`.
6. Copia el portal a `/var/www/sgl-dqr` y activa el sitio de Nginx ([`nginx-sgl-dqr.conf`](../deploy/ubuntu/nginx-sgl-dqr.conf)).
7. Asigna dueños y permisos (sección 4).
8. Inicia la API con pm2 y activa su arranque automático (`pm2 save` + `pm2 startup`).
9. Programa una copia diaria de la base a las 02:30 con [`respaldo-db.sh`](../deploy/ubuntu/respaldo-db.sh) y hace la primera.

Para otra contraseña de base o para entrar al portal por una IP real:

```bash
DB_CLAVE='otra-clave' PORTAL_URL='http://192.168.1.10' bash deploy/ubuntu/instalar.sh
```

Esas variables solo se usan la primera vez; después se editan en `deploy/ubuntu/api.env` y se reinicia con `pm2 restart sgl-dqr-api`.

**Actualizar a una versión nueva:** `git pull` y volver a ejecutar `bash deploy/ubuntu/instalar.sh`. El script es repetible: conserva `api.env` y la base de datos.

---

## 4. Permisos

| Ruta | Dueño | Permiso | Motivo |
|---|---|---|---|
| `/opt/sgl-dqr` (repositorio) | usuario que despliega | carpetas 755 | Solo el dueño modifica el código |
| `deploy/ubuntu/*.sh` | usuario que despliega | 755 | Scripts ejecutables |
| `deploy/ubuntu/api.env` | usuario que despliega | 600 | Contiene la contraseña de la base: solo el dueño la lee |
| `/var/www/sgl-dqr` | `www-data` (usuario de Nginx) | carpetas 755, archivos 644 | Nginx lee el portal; nadie más lo modifica |
| `/var/backups/sgl-dqr` | `postgres` | carpeta 700, copias 600 | Las copias tienen datos personales de clientes |

r = 4, w = 2, x = 1; los tres dígitos son dueño, grupo y otros. No se usa 777, porque cualquier usuario del servidor podría modificar o borrar el sitio.

**Comprobación del efecto de los permisos:**

```bash
sudo chmod 000 /var/www/sgl-dqr/index.html
curl -I http://localhost          # HTTP/1.1 403 Forbidden
sudo chmod 644 /var/www/sgl-dqr/index.html
curl -I http://localhost          # HTTP/1.1 200 OK
```

---

## 5. Verificación

| Prueba | Comando o dirección | Resultado esperado |
|---|---|---|
| Particiones y swap | `lsblk` y `free -h` | `/boot`, `[SWAP]`, `/` y `/home` montados |
| API en ejecución | `pm2 status` | `sgl-dqr-api` en `online` |
| Salud de la API | `curl http://localhost:8080/actuator/health` | `"status":"UP"` |
| Nginx | `systemctl status nginx --no-pager` | `active (running)` |
| Portal desde Windows | `http://localhost:8088` | Página del portal |
| Datos de demostración | `http://localhost:8088/rastreo`, comprobante `MV-DEMO-002`, documento `45678904` | Línea de tiempo del envío |
| Arranque automático | `sudo reboot`, esperar 2 minutos y repetir las pruebas | Todo responde sin intervención |

Escritorio JavaFX desde Windows: con la API local de Windows detenida (`.\scripts\demo-parar-api.ps1`), ejecutar `.\scripts\demo-escritorio.ps1`; apunta a `http://localhost:8080`, que VirtualBox reenvía al servidor.

---

## 6. Operación

| Tarea | Comando |
|---|---|
| Ver el estado de la API | `pm2 status` |
| Ver los registros de la API | `pm2 logs sgl-dqr-api --lines 80` |
| Reiniciar la API | `pm2 restart sgl-dqr-api` |
| Recargar Nginx tras un cambio | `sudo nginx -t && sudo systemctl reload nginx` |
| Hacer una copia de la base ahora | `sudo bash deploy/ubuntu/respaldo-db.sh` |
| Ver las copias | `sudo ls -l /var/backups/sgl-dqr` |
| Restaurar una copia (reemplaza los datos actuales) | `gunzip -c /var/backups/sgl-dqr/<archivo>.sql.gz \| sudo -u postgres psql -d marvisur_db` |
| Apagar el servidor | `sudo shutdown now` |

---

## 7. Problemas encontrados y solución

| Problema | Causa | Solución |
|---|---|---|
| Pantalla negra tras «Try or Install Ubuntu» | Se usó la ISO de Ubuntu Desktop | Usar la ISO de Ubuntu Server (instalador en modo texto) |
| No hay espacio para la máquina virtual | Disco C: lleno | Crear la máquina y guardar la ISO en otro disco |
| SSH pide una clave que no corresponde | El usuario del comando no es el creado en la instalación | `ssh -p 2222 <usuario-creado>@localhost` |
| `pm2 status` sale vacío tras reiniciar | Faltaba `pm2 save` y `pm2 startup` | `instalar.sh` los ejecuta; a mano: `pm2 start deploy/ubuntu/iniciar-api.sh --name sgl-dqr-api --interpreter bash`, `pm2 save` y la línea que imprime `pm2 startup` |
| `/var/www/sgl-dqr` con dueño `root` | Se copió con `sudo` y faltó el `chown` | `sudo chown -R www-data:www-data /var/www/sgl-dqr` |
| `ls -l /var/www/sgl-dqr` muestra `total 0` | El portal compilado no se copió | Verificar `dist/` y repetir la copia |
| El portal abre pero cotizar da error o `502 Bad Gateway` | La API está apagada o arrancando | `pm2 restart sgl-dqr-api` y esperar un minuto |

---

## 8. Limitaciones

- El servidor opera sobre HTTP, sin certificado TLS propio; el despliegue de demostración en Render sí usa HTTPS, provisto por la plataforma (RNF-01, tarea O3).
- Usa el perfil `dev` y las credenciales de desarrollo del proyecto para cargar los datos de demostración. En un servidor real se cambian la contraseña de la base y `JWT_SECRET`, y se usa el perfil `prod`.
- Sin cortafuegos; las copias de la base quedan en el mismo servidor, así que conviene copiarlas también a otro equipo.
