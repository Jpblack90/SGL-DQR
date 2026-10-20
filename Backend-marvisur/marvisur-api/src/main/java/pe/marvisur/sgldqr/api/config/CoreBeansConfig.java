package pe.marvisur.sgldqr.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.marvisur.sgldqr.aplicacion.CambiarEstadoService;
import pe.marvisur.sgldqr.aplicacion.RegistrarEncomiendaService;
import pe.marvisur.sgldqr.aplicacion.TarifarioPorPeso;
import pe.marvisur.sgldqr.dominio.servicio.MotorQR;
import pe.marvisur.sgldqr.infraestructura.qr.ZXingAdapter;
import pe.marvisur.sgldqr.puertos.ImagenQRPort;
import pe.marvisur.sgldqr.puertos.ImpresionPort;
import pe.marvisur.sgldqr.puertos.RelojPort;
import pe.marvisur.sgldqr.puertos.RepositorioEncomiendaPort;
import pe.marvisur.sgldqr.puertos.TarifarioPort;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Configuración de Spring para instanciar los componentes del núcleo de dominio
 * respetando la arquitectura hexagonal (el core se mantiene puro sin anotaciones de Spring).
 */
@Configuration
public class CoreBeansConfig {

    private static final Logger log = LoggerFactory.getLogger(CoreBeansConfig.class);

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public RelojPort relojPort(Clock clock) {
        return () -> LocalDateTime.now(clock);
    }

    @Bean
    public MotorQR motorQR(Clock clock) {
        return new MotorQR(clock);
    }

    @Bean
    public ImagenQRPort imagenQRPort() {
        return new ZXingAdapter();
    }

    @Bean
    public TarifarioPort tarifarioPort() {
        return TarifarioPorPeso.conValoresPorDefecto();
    }

    @Bean
    public ImpresionPort impresionPort() {
        // En la API REST, la impresión física se delega a la aplicación de escritorio (ESC/POS 58mm)
        return datosTicket -> log.debug("Impresión delegada al cliente de escritorio: {}", datosTicket);
    }

    @Bean
    public RegistrarEncomiendaService registrarEncomiendaService(RepositorioEncomiendaPort repositorioEncomiendaPort,
                                                                 TarifarioPort tarifarioPort,
                                                                 RelojPort relojPort,
                                                                 ImpresionPort impresionPort) {
        return new RegistrarEncomiendaService(repositorioEncomiendaPort, tarifarioPort, relojPort, impresionPort);
    }

    @Bean
    public CambiarEstadoService cambiarEstadoService(RepositorioEncomiendaPort repositorioEncomiendaPort) {
        return new CambiarEstadoService(repositorioEncomiendaPort);
    }
}
