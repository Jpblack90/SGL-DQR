ANÁLISIS DE REQUISITOS (FASE 1 - ACTUALIZADA Y CONSOLIDADA)
1. Levantamiento de Requisitos y Contexto Empresarial
1.1. Contexto Empresarial y Estratégico
Empresa / Proyecto: QuickScan Marvisur (Pequeña empresa simulada del sector logística y courier interprovincial).
Visión: Convertirse en el estándar tecnológico de gestión, validación y trazabilidad logística para agencias de encomiendas e-commerce en el Perú.
Misión: Eliminar las fricciones operativas en ventanilla y almacén mediante soluciones dinámicas de Doble QR, garantizando agilidad y trazabilidad sin exigir contraseñas al cliente final.
Entorno (AS-IS): Sector caracterizado por alta dependencia de guías físicas, procesos de verificación manual que generan cuellos de botella en almacén y excesivas colas de espera en ventanilla.
Estrategias (TO-BE): Despliegue de un motor de lectura masiva (pistoleo) para almacenes y un ecosistema de validación criptográfica mediante códigos QR dinámicos.
Planes: Ejecución del Producto Mínimo Viable (MVP) en 16 semanas, estructurando la arquitectura base para futuras integraciones gubernamentales (Guía de Remisión Electrónica SUNAT) y pasarelas de pago.
Modelo de Negocio: Soportado por el documento Business Model Canvas (anexo), cuyo flujo de ingresos proyectado se basa en una comisión transaccional referencial de S/ 2.00 a S/ 3.00 por envío procesado.
1.2. Matriz de Stakeholders
Sponsor / Docente del Curso: Anthony Vladimir Mora Bonilla.
Project Manager: Jean Pool Oroncuy Orcohuaranga.
Equipo Técnico (Soporte, Devs, QA): Jeyson Russel Vílchez Chambergo, Leonardo Sebastian Román Yeliz, Alexander Franklin León Almonacid.
1.3. Técnicas de Recolección de Datos
Reuniones de descubrimiento y entrevistas estructuradas con perfiles de operaciones, administración, choferes y clientes finales.
Benchmarking técnico de plataformas de última milla y courier en el mercado nacional.
2. Planteamiento y Evaluación de Alternativas de Solución
Las siguientes tres sub-soluciones tecnológicas se derivan del mismo problema central (fricciones en ventanilla y almacén) y cumplen con el requisito de ser desarrolladas en al menos un 50% bajo el lenguaje Java.
2.1. Definición de Sub-Soluciones
Sub-Solución 1: Sistema Híbrido (Seleccionada). Aplicación de Escritorio en JavaFX para la operación en sucursal (ventanilla y almacén) con conexión nativa USB a periféricos, combinada con un Portal Web en la Nube (Java Spring Boot) para el rastreo del cliente.
Pantallas (Mockups): 1) Login y Control de Turno, 2) Registro de Encomienda y Emisión QR, 3) Escaneo Masivo en Almacén, 4) Control de Rutas y Choferes, 5) Portal Web de Consulta de Estado.
Sub-Solución 2: Web Monolítica Cloud (Alternativa A). Centralización total en una plataforma web (Java Spring Boot API).
Pantallas (Mockups): 1) Login RBAC, 2) Dashboard Admin, 3) CRUD Encomiendas, 4) Módulo de Recepciones Web, 5) Panel Web Tracking.
Sub-Solución 3: Ecosistema Móvil Android + Web (Alternativa B). Operatividad de almacén basada en dispositivos móviles (App Java Android) y operaciones de ventanilla vía web.
Pantallas (Mockups): 1) Login Móvil, 2) Escáner por Cámara de Celular, 3) Inventario de Carga Móvil, 4) Registro Web, 5) Tracking Administrativo.
2.2. Cuadro Comparativo y Justificación
Justificación de Elección: Se selecciona la Sub-Solución 1 (Sistema Híbrido) porque el software de escritorio JavaFX asegura una velocidad de procesamiento ininterrumpida y conexión directa con impresoras térmicas y lectoras USB en las sucursales, mientras que el portal web brinda accesibilidad universal para que el cliente final rastree su envío sin fricciones.
3. Especificación de Requisitos Funcionales (RF)
RF-01 Autenticación interna por roles (RBAC); acceso del cliente final sin contraseña mediante tokens dinámicos.
RF-02 Dashboard administrativo con KPIs operativos en tiempo real.
RF-03 Gestión de personal operativo (CRUD) con registro de auditoría.
RF-04 Gestión del ciclo de vida y estado de pago de encomiendas, con bloqueo por pago "Pendiente".
RF-05 Control de flota y asignación de rutas interprovinciales.
RF-06 Módulo de almacén con procesamiento por lotes (Batch Scanning) y contingencia por DNI/RUC.
RF-07 Motor Criptográfico de Doble QR: generación y validación cruzada de QR1/QR2.
RF-08 Portal web público de tracking y trazabilidad histórica.
RF-09 Billetera Digital del código QR2 para el destinatario.
RF-10 Pre-registro web de encomiendas ("Código Express") con cotización dinámica.
RF-11 Descarga autónoma de comprobantes electrónicos en PDF.

4. Especificación de Requisitos No Funcionales (RNF)
RNF-01 Seguridad: comunicaciones cifradas HTTPS/SSL; el hash del QR2 se invalida en <= 500 ms tras la validación cruzada.
RNF-02 Fiabilidad y contingencia offline: conmutación automática a base local (SQLite/H2) en <= 2 s ante caída de red; uptime operativo 100% en ventanilla.
RNF-03 Rendimiento: latencia máxima 1000 ms en la validación cruzada y en actualizaciones por lote, con hasta 50 lecturas ópticas por minuto.
RNF-04 Portabilidad e instalación: cliente web responsivo sin descargas; App de escritorio como instalador autocontenido (.EXE/.MSI) con JRE embebido (jpackage).
RNF-05 Mantenibilidad: Arquitectura Hexagonal; lógica de negocio desacoplada de los frameworks.

Fuera de alcance: integración técnica con SUNAT (XML/CDR) y apps móviles nativas.

5. Requisitos de Integraciones Externas
SUNAT: Diseño de arquitectura para el envío asíncrono de XML hacia la API de SUNAT (emisión de Guías de Remisión Electrónica).
Pasarelas de Pago: Capas de abstracción para conexiones vía webhooks con Yape, Plin y procesadores de tarjetas (Niubiz/Izipay).
Notificaciones: Consumo de APIs REST (Twilio / WhatsApp Business API) para el envío automatizado del QR al cliente.
6. Consolidación y Cierre de Requisitos
Metodología Oficial: Todos los entregables y el desarrollo técnico se rigen estrictamente bajo el modelo en Cascada (Waterfall), con una duración de 16 semanas (10 de agosto al 27 de noviembre de 2026).
Especificación de Requisitos de Software (SRS): Elaboración y cierre del documento técnico formal según estándar IEEE.
Validación y Acta de Conformidad: Se da por aprobada la matriz de validación (Tabla E.1), confirmando las entrevistas clave, la identificación del problema y la aprobación de RF y RNF.
Línea Base (Baseline): Aprobada y congelada para el inicio de la Fase 2 (Diseño Arquitectónico y Base de Datos).

| Criterio Técnico | Sub-Solución 1 (Híbrida) | Sub-Solución 2 (Web Monolítica) | Sub-Solución 3 (Móvil + Web) |
| --- | --- | --- | --- |
| Cumplimiento Java | 100% (JavaFX + Spring Boot) | ~70% (Spring Boot + Frontend Web) | ~85% (Android Nativo + Spring Boot) |
| Velocidad en Almacén | Alta (Soporta pistolas USB físicas) | Media (Depende de red) | Media-Baja (Cámara de celular) |
| Complejidad Despliegue | Media (Instalador local en agencia) | Baja (Navegador web) | Alta (APK y gestión de dispositivos) |
| Adopción del Cliente | Alta (Portal web sin instalación) | Alta (Portal web) | Alta (Portal web) |
