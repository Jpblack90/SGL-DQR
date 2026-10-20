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
Pantallas (Mockups): 1) Login RBAC, 2) Dashboard Admin, 3) CRUD Equipajes, 4) Módulo de Recepciones Web, 5) Panel Web Tracking.
Sub-Solución 3: Ecosistema Móvil Android + Web (Alternativa B). Operatividad de almacén basada en dispositivos móviles (App Java Android) y operaciones de ventanilla vía web.
Pantallas (Mockups): 1) Login Móvil, 2) Escáner por Cámara de Celular, 3) Inventario de Carga Móvil, 4) Registro Web, 5) Tracking Administrativo.
2.2. Cuadro Comparativo y Justificación
Justificación de Elección: Se selecciona la Sub-Solución 1 (Sistema Híbrido) porque el software de escritorio JavaFX asegura una velocidad de procesamiento ininterrumpida y conexión directa con impresoras térmicas y lectoras USB en las sucursales, mientras que el portal web brinda accesibilidad universal para que el cliente final rastree su envío sin fricciones.
3. Especificación de Requisitos Funcionales (RF)
RF-01: Módulo Login y Control de Accesos (RBAC). Autenticación interna para el personal (Admin, Almacenero, Recepcionista, Chofer) y acceso público sin contraseña para clientes finales mediante token QR.
RF-02: Panel Administrativo (Dashboard). Visualización en tiempo real de métricas, KPIs operativos y estado general de la sucursal.
RF-03: CRUD Empleado. Gestión centralizada de perfiles operativos (altas, bajas y edición de almaceneros y recepcionistas).
RF-04: CRUD Equipajes (Core Operativo). Registro integral del paquete (peso, volumen, fechas, RUC/datos personales). Vincula automáticamente el envío al Sistema de Doble QR.
RF-05: RU Choferes y Flota. Control de unidades de transporte, asignación de rutas y actualización de horarios de salida/llegada.
RF-06: Módulo de Recepciones (Escaneo Batch). Interfaz de escritorio optimizada para la lectura masiva y rápida de encomiendas vía pistoleo en almacén.
RF-07: Motor Central de Validación Criptográfica (Sistema Doble QR). Lógica de generación simultánea de QR 1 (Operativo) y QR 2 (Retiro), vinculación criptográfica y expiración post-entrega. (Nota: Caso de Uso Representativo del sistema).
RF-08: Portal Web de Seguimiento (Tracking). Autoservicio web para la consulta de trazabilidad e historial de estados del paquete por parte del usuario final.
RF-09: Billetera Digital de QR. Interfaz web para que el cliente destinatario almacene y presente su código QR de retiro.
RF-10: Pre-registro Web y Cotización. Formulario para calcular tarifas e ingresar datos anticipadamente. (Diagrama documentado bajo estándar de Casos de Uso).
RF-11: Comprobantes Web. Generación y descarga automatizada de comprobantes de pago y entrega en formato PDF.
4. Especificación de Requisitos No Funcionales (RNF)
RNF-01 Seguridad (RBAC): Restricción estricta de módulos según rol interno. Invalidación automática del QR del cliente tras una entrega exitosa para evitar duplicidad.
RNF-02 Rendimiento: La latencia de validación cruzada criptográfica en almacén y actualización de base de datos no debe superar los 1000 milisegundos.
RNF-03 Usabilidad: La interfaz de recepción en escritorio debe minimizar los clics a cero durante el proceso de escaneo continuo.
RNF-04 Compatibilidad: El portal web debe ser funcional en navegadores modernos (Chrome, Edge, Safari) y completamente responsivo en móviles.
RNF-05 Disponibilidad: Uptime garantizado del 99.9% en el entorno cloud VPS durante horarios operativos.
RNF-06 Escalabilidad: La arquitectura debe soportar picos de peticiones concurrentes durante temporadas altas sin degradación del servicio.
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