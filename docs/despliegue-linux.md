# Despliegue de SGL-DQR en Linux (Ubuntu Server)

Guía para desplegar la API, la base de datos y el portal web de SGL-DQR en un servidor Ubuntu Server, en una máquina virtual de VirtualBox o de Hyper-V. La aplicación de escritorio JavaFX no se instala en el servidor: es un cliente que corre en Windows y se conecta a la API.

Se ha desplegado dos veces:

| Fecha | Virtualizador | Sistema | Cómo |
|---|---|---|---|
| 12/11/2026 | VirtualBox 7.2 | Ubuntu Server 26.04.1 LTS | Paso a paso, a mano |
| 17/11/2026 | Hyper-V (Windows 11 Pro) | Ubuntu Server 24.04.5 LTS | Con [`deploy/ubuntu/instalar.sh`](../deploy/ubuntu/instalar.sh): los 9 pasos terminaron sin errores en una máquina limpia |

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

### Opción Hyper-V (Windows 10/11 Pro)

Usar Hyper-V cuando Windows tiene activo su hipervisor (WSL2, Docker Desktop o la seguridad basada en virtualización): en ese caso VirtualBox no puede usar VT-x, funciona en un modo lento y la máquina se congela (ver sección 7).

Crear y arrancar la máquina desde **PowerShell como administrador** (ajustar la ruta del ISO):

```powershell
$vm  = "SGL-DQR-Ubuntu"
$iso = "C:\ruta\ubuntu-24.04.5-live-server-amd64.iso"
$dir = "$env:USERPROFILE\Hyper-V"
New-Item -ItemType Directory -Force $dir | Out-Null
New-VM -Name $vm -Generation 2 -MemoryStartupBytes 4GB -NewVHDPath "$dir\$vm.vhdx" -NewVHDSizeBytes 40GB -SwitchName "Default Switch" -Path $dir
Set-VM -Name $vm -ProcessorCount 4 -StaticMemory -CheckpointType Disabled
Set-VMFirmware -VMName $vm -SecureBootTemplate MicrosoftUEFICertificateAuthority
Add-VMDvdDrive -VMName $vm -Path $iso
Set-VMFirmware -VMName $vm -FirstBootDevice (Get-VMDvdDrive -VMName $vm)
Start-VM -Name $vm
vmconnect localhost $vm
```

| Diferencia con VirtualBox | En Hyper-V |
|---|---|
| Firmware | UEFI (generación 2) con la plantilla de arranque seguro «Microsoft UEFI Certificate Authority», que acepta Ubuntu |
| Particiones | Al marcar el disco con «Use As Boot Device», el instalador agrega `/boot/efi` (1 GB, FAT32); las otras cuatro se crean igual |
| Red | «Default Switch»: Windows llega directo a la IP de la máquina (por ejemplo `172.21.195.51`), sin reenvío de puertos |
| Acceso | `ssh sgl@<IP>` desde PowerShell. La IP se ve en el mensaje de bienvenida o con `hostname -I` |
| IP | Puede cambiar al reiniciar Windows; actualizarla con `bash deploy/ubuntu/actualizar-direccion.sh` (sección 6) |

Trabajar por SSH y no en la ventana de la máquina: «Portapapeles → Escribir texto del Portapapeles» de Hyper-V cambia letras y símbolos con el teclado latinoamericano.

---

## 3. Instalación automática

Desde PowerShell, entrar al servidor:

```powershell
ssh -p 2222 usuario@localhost   # VirtualBox (reenvío de puertos)
ssh usuario@<IP-de-la-máquina>  # Hyper-V
```

Dentro del servidor:

```bash
sudo apt-get update && sudo apt-get install -y git
sudo mkdir -p /opt/sgl-dqr && sudo chown $USER:$USER /opt/sgl-dqr
cd /opt/sgl-dqr
git clone https://github.com/Jpblack90/SGL-DQR.git
cd SGL-DQR
bash deploy/ubuntu/instalar.sh                                         # VirtualBox
PORTAL_URL=http://$(hostname -I | awk '{print $1}') bash deploy/ubuntu/instalar.sh   # Hyper-V
```

Mientras las mejoras de M1/M2 no estén fusionadas en `main`, clonar con `git clone -b feature/M1-qr-impreso ...`. En el despliegue del 17/11/2026 el script completo tardó unos 6 minutos.

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

`DB_CLAVE` solo se usa la primera vez. `PORTAL_URL`, si se indica de nuevo, actualiza la dirección guardada en `deploy/ubuntu/api.env`; para cambiarla sin recompilar está `deploy/ubuntu/actualizar-direccion.sh`.

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
| Portal desde Windows | `http://localhost:8088` (VirtualBox) o `http://<IP>` (Hyper-V) | Página del portal |
| Datos de demostración | `/rastreo` del portal, comprobante `MV-DEMO-002`, documento `45678904` | Línea de tiempo del envío |
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
| Cambió la IP de la máquina (Hyper-V) | `bash deploy/ubuntu/actualizar-direccion.sh` |
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
| VirtualBox muy lento; la consola deja de responder o se pierden teclas (`sudo` dice «incorrect password») | Windows tiene activo su hipervisor; `VBox.log` dice `VT-x is not available` y VirtualBox usa el modo NEM. `bcdedit /set hypervisorlaunchtype off` no bastó porque la seguridad basada en virtualización (VBS) seguía activa | Crear la máquina en Hyper-V (sección 2). Comprobar con `systeminfo` («Se detectó un hipervisor») y `Win32_DeviceGuard` (`VirtualizationBasedSecurityStatus` = 2) |
| En Hyper-V, el texto pegado con «Escribir texto del Portapapeles» sale con letras cambiadas | Esa función no respeta el teclado latinoamericano | Entrar por SSH desde PowerShell y pegar ahí |
| La Billetera o el portal usan una IP vieja | Hyper-V cambió la IP al reiniciar Windows | `bash deploy/ubuntu/actualizar-direccion.sh` |

---

## 8. Limitaciones

- El servidor opera sobre HTTP, sin certificado TLS propio; el despliegue de demostración en Render sí usa HTTPS, provisto por la plataforma (RNF-01, tarea O3).
- Usa el perfil `dev` y las credenciales de desarrollo del proyecto para cargar los datos de demostración. En un servidor real se cambian la contraseña de la base y `JWT_SECRET`, y se usa el perfil `prod`.
- Sin cortafuegos; las copias de la base quedan en el mismo servidor, así que conviene copiarlas también a otro equipo.
