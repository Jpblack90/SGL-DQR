package pe.marvisur.sgldqr.escritorio.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.EncabezadoConexionViewModel;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ModeloMenuRol;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.UsuarioSesion;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.AutenticacionPort;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.EstadoConexionPort;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.ColaLocalPort;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.ServicioConexion;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.SincronizadorOffline;
import pe.marvisur.sgldqr.escritorio.infraestructura.sqlite.SQLiteColaAdapter;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.MarvisurHttpClient;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.SesionContexto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.EstadoConexionUI;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador de la ventana principal y orquestador del menú lateral según rol (RBAC).
 */
public class ControladorPrincipal {

    private static final Logger LOGGER = Logger.getLogger(ControladorPrincipal.class.getName());

    @FXML
    private Circle circuloConexion;

    @FXML
    private Label lblEstadoConexion;

    @FXML
    private Label lblPendientes;

    @FXML
    private Label lblNotificacionSync;

    @FXML
    private Label lblSucursal;

    @FXML
    private Label lblUsuario;

    @FXML
    private Label lblRol;

    @FXML
    private Button btnMenuVentanilla;

    @FXML
    private Button btnMenuAlmacen;

    @FXML
    private Button btnMenuContingencia;

    @FXML
    private Button btnMenuColaLocal;

    @FXML
    private Button btnMenuRutas;

    @FXML
    private Button btnMenuDashboard;

    @FXML
    private Button btnMenuEmpleados;

    @FXML
    private StackPane contenedorContenido;

    private final AutenticacionPort autenticacionPort;
    private final EstadoConexionPort estadoConexionPort;
    private final ServicioConexion servicioConexion;
    private final SincronizadorOffline sincronizadorOffline;
    private final ColaLocalPort colaLocalPort;
    private ScheduledExecutorService planificadorConectividad;
    private PauseTransition pausaNotificacionSync;
    private ControladorDashboard controladorDashboardActivo;

    public ControladorPrincipal() {
        this(new MarvisurHttpClient(), ServicioConexion.getInstancia(), SincronizadorOffline.getInstancia(), new SQLiteColaAdapter());
    }

    public ControladorPrincipal(AutenticacionPort autenticacionPort, EstadoConexionPort estadoConexionPort) {
        this(autenticacionPort, ServicioConexion.getInstancia(), SincronizadorOffline.getInstancia(), new SQLiteColaAdapter());
    }

    public ControladorPrincipal(AutenticacionPort autenticacionPort, ServicioConexion servicioConexion,
                                SincronizadorOffline sincronizadorOffline, ColaLocalPort colaLocalPort) {
        this.autenticacionPort = autenticacionPort;
        this.servicioConexion = servicioConexion;
        this.sincronizadorOffline = sincronizadorOffline;
        this.colaLocalPort = colaLocalPort;
        this.estadoConexionPort = (autenticacionPort instanceof EstadoConexionPort ecp) ? ecp : new MarvisurHttpClient();
    }

    @FXML
    public void initialize() {
        configurarDatosUsuario();
        configurarMenuSegunRol();
        iniciarMonitoreoConectividad();
        mostrarVistaInicialSegunRol();
    }

    private void configurarDatosUsuario() {
        UsuarioSesion sesion = SesionContexto.obtenerSesion();
        if (sesion != null) {
            lblUsuario.setText(sesion.getNombreCompleto());
            lblRol.setText(sesion.rol() != null ? sesion.rol().replace("ROLE_", "") : "COLABORADOR");
            lblSucursal.setText("Agencia: " + (sesion.idSucursal() != null ? sesion.idSucursal() : "S/A"));
        } else {
            lblUsuario.setText("Colaborador");
            lblRol.setText("INVITADO");
            lblSucursal.setText("Agencia: --");
        }
    }

    private void configurarMenuSegunRol() {
        UsuarioSesion sesion = SesionContexto.obtenerSesion();
        String rol = sesion != null ? sesion.rol() : "";

        configurarBotonMenu(btnMenuVentanilla, ModeloMenuRol.ModuloMenu.VENTANILLA, rol);
        configurarBotonMenu(btnMenuAlmacen, ModeloMenuRol.ModuloMenu.ALMACEN, rol);
        configurarBotonMenu(btnMenuContingencia, ModeloMenuRol.ModuloMenu.CONTINGENCIA, rol);
        configurarBotonMenu(btnMenuColaLocal, ModeloMenuRol.ModuloMenu.COLA_LOCAL, rol);
        configurarBotonMenu(btnMenuRutas, ModeloMenuRol.ModuloMenu.RUTAS, rol);
        configurarBotonMenu(btnMenuDashboard, ModeloMenuRol.ModuloMenu.DASHBOARD, rol);
        configurarBotonMenu(btnMenuEmpleados, ModeloMenuRol.ModuloMenu.EMPLEADOS, rol);
    }

    private void configurarBotonMenu(Button boton, ModeloMenuRol.ModuloMenu modulo, String rol) {
        if (boton == null) return;

        boolean autorizado = ModeloMenuRol.estaAutorizado(rol, modulo);
        if (!autorizado) {
            boton.setVisible(false);
            boton.setManaged(false);
            return;
        }

        boolean habilitado = modulo.isImplementado();
        if (!habilitado) {
            boton.setDisable(false); // Se mantiene clickable para mostrar el diálogo explicativo
            boton.setStyle("-fx-opacity: 0.65;");
        }
    }

    private void mostrarVistaInicialSegunRol() {
        UsuarioSesion sesion = SesionContexto.obtenerSesion();
        String rol = sesion != null ? sesion.rol() : "";

        if (ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.VENTANILLA)) {
            mostrarVentanilla();
        } else if (ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.ALMACEN)) {
            mostrarAlmacen();
        } else if (ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.CONTINGENCIA)) {
            mostrarContingencia();
        } else {
            marcarBotonActivo(null);
            mostrarAvisoProximamente();
        }
    }

    private void marcarBotonActivo(Button botonActivo) {
        Button[] botones = {btnMenuVentanilla, btnMenuAlmacen, btnMenuContingencia, btnMenuColaLocal, btnMenuRutas, btnMenuDashboard, btnMenuEmpleados};
        for (Button btn : botones) {
            if (btn != null) {
                btn.getStyleClass().remove("boton-menu-activo");
            }
        }
        if (botonActivo != null && !botonActivo.getStyleClass().contains("boton-menu-activo")) {
            botonActivo.getStyleClass().add("boton-menu-activo");
        }
    }

    private void iniciarMonitoreoConectividad() {
        // 1. Suscripción a la fuente única de verdad (ServicioConexion)
        servicioConexion.agregarListenerUI((estado, pendientes, notificacion) -> {
            try {
                Platform.runLater(() -> actualizarIndicadorUI(estado, pendientes, notificacion));
            } catch (IllegalStateException e) {
                actualizarIndicadorUI(estado, pendientes, notificacion);
            }
        });

        // 2. Iniciar el sincronizador offline en segundo plano (daemon)
        sincronizadorOffline.iniciar();

        // 3. Si se inicia sesión con pendientes y la API está en línea, disparar sincronización inicial
        int pendientesIniciales = colaLocalPort.contarPendientes();
        servicioConexion.actualizarPendientes(pendientesIniciales);
        actualizarIndicadorUI(servicioConexion.getEstadoUI(), pendientesIniciales, "");

        if (pendientesIniciales > 0 && servicioConexion.estaEnLinea()) {
            LOGGER.info("Sesión iniciada con " + pendientesIniciales + " registros pendientes en cola local. Disparando sincronización...");
            sincronizadorOffline.sincronizarAhora();
        }

        // 4. Sondeo periódico de 5s conservado en ControladorPrincipal
        planificadorConectividad = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "hilo-monitoreo-conectividad");
            t.setDaemon(true);
            return t;
        });

        planificadorConectividad.scheduleAtFixedRate(() -> {
            boolean online = estadoConexionPort.verificarConectividad();
            if (online) {
                boolean eraOffline = !servicioConexion.estaEnLinea();
                servicioConexion.reportarExitoConexion();
                if (eraOffline) {
                    int p = colaLocalPort.contarPendientes();
                    servicioConexion.actualizarPendientes(p);
                    if (p > 0) {
                        LOGGER.info("Reconexión detectada con " + p + " pendientes. Disparando sincronización automática...");
                        sincronizadorOffline.sincronizarAhora();
                    }
                }
            } else {
                servicioConexion.reportarFalloConexion("Fallo en verificación periódica de salud");
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    private void actualizarIndicadorUI(EstadoConexionUI estado, int pendientes, String notificacion) {
        EncabezadoConexionViewModel vm = EncabezadoConexionViewModel.calcular(estado, pendientes, notificacion);

        if (circuloConexion != null) {
            circuloConexion.setFill(Color.web(vm.colorCirculoHex()));
        }

        if (lblEstadoConexion != null) {
            lblEstadoConexion.setText(vm.textoEstado());
            if (estado == EstadoConexionUI.EN_LINEA && notificacion != null && !notificacion.isBlank()) {
                if (pausaNotificacionSync != null) {
                    pausaNotificacionSync.stop();
                }
                try {
                    pausaNotificacionSync = new PauseTransition(Duration.seconds(5));
                    pausaNotificacionSync.setOnFinished(e -> {
                        if (servicioConexion.estaEnLinea()) {
                            lblEstadoConexion.setText("En línea");
                        }
                        servicioConexion.limpiarNotificacion();
                    });
                    pausaNotificacionSync.play();
                } catch (Exception ignored) {
                }
            }
        }

        if (lblPendientes != null) {
            lblPendientes.setVisible(vm.mostrarBadgePendientes());
            lblPendientes.setManaged(vm.mostrarBadgePendientes());
            if (vm.mostrarBadgePendientes()) {
                lblPendientes.setText(vm.textoBadgePendientes());
            }
        }
    }

    @FXML
    public void mostrarVentanilla() {
        desactivarDashboardSiActivo();
        marcarBotonActivo(btnMenuVentanilla);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pe/marvisur/sgldqr/escritorio/ui/VentanillaView.fxml"));
            Node vista = loader.load();
            contenedorContenido.getChildren().setAll(vista);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar VentanillaView.fxml: " + e.getMessage(), e);
        }
    }

    @FXML
    public void mostrarAlmacen() {
        desactivarDashboardSiActivo();
        marcarBotonActivo(btnMenuAlmacen);
        cargarVistaAlmacen("ENTREGA");
    }

    @FXML
    public void mostrarContingencia() {
        desactivarDashboardSiActivo();
        marcarBotonActivo(btnMenuContingencia);
        cargarVistaAlmacen("CONTINGENCIA");
    }

    @FXML
    public void mostrarColaLocal() {
        desactivarDashboardSiActivo();
        marcarBotonActivo(btnMenuColaLocal);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pe/marvisur/sgldqr/escritorio/ui/ColaLocalView.fxml"));
            Node vista = loader.load();
            contenedorContenido.getChildren().setAll(vista);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar ColaLocalView.fxml: " + e.getMessage(), e);
        }
    }

    private void cargarVistaAlmacen(String pestana) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pe/marvisur/sgldqr/escritorio/ui/AlmacenView.fxml"));
            Node vista = loader.load();
            ControladorAlmacen controlador = loader.getController();
            if (controlador != null && pestana != null) {
                controlador.seleccionarPestana(pestana);
            }
            contenedorContenido.getChildren().setAll(vista);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar AlmacenView.fxml: " + e.getMessage(), e);
        }
    }

    @FXML
    public void mostrarDashboard() {
        desactivarDashboardSiActivo();
        marcarBotonActivo(btnMenuDashboard);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pe/marvisur/sgldqr/escritorio/ui/DashboardView.fxml"));
            Node vista = loader.load();
            controladorDashboardActivo = loader.getController();
            if (controladorDashboardActivo != null) {
                controladorDashboardActivo.activar();
            }
            contenedorContenido.getChildren().setAll(vista);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar DashboardView.fxml: " + e.getMessage(), e);
        }
    }

    @FXML
    public void mostrarEmpleados() {
        desactivarDashboardSiActivo();
        marcarBotonActivo(btnMenuEmpleados);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pe/marvisur/sgldqr/escritorio/ui/EmpleadosView.fxml"));
            Node vista = loader.load();
            contenedorContenido.getChildren().setAll(vista);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar EmpleadosView.fxml: " + e.getMessage(), e);
        }
    }

    private void desactivarDashboardSiActivo() {
        if (controladorDashboardActivo != null) {
            controladorDashboardActivo.desactivar();
            controladorDashboardActivo = null;
        }
    }

    @FXML
    public void mostrarAvisoProximamente() {
        desactivarDashboardSiActivo();
        NavegadorUI.mostrarAlerta(
                Alert.AlertType.INFORMATION,
                "Módulo en Desarrollo",
                "Operación Próximamente Disponible",
                "Este módulo se habilitará en las siguientes entregas del plan de desarrollo (SGL-DQR)."
        );
    }

    @FXML
    public void manejarCerrarSesion() {
        detenerTareasFondo();
        autenticacionPort.cerrarSesion();
        NavegadorUI.mostrarLogin("Ha cerrado sesión correctamente.");
    }

    public void detenerTareasFondo() {
        desactivarDashboardSiActivo();
        if (planificadorConectividad != null && !planificadorConectividad.isShutdown()) {
            planificadorConectividad.shutdownNow();
        }
        if (sincronizadorOffline != null) {
            sincronizadorOffline.detener();
        }
        if (pausaNotificacionSync != null) {
            pausaNotificacionSync.stop();
        }
    }
}
