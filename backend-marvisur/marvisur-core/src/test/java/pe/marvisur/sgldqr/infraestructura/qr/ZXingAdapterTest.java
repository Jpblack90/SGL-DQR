package pe.marvisur.sgldqr.infraestructura.qr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.dominio.excepcion.ExcepcionDominio;
import pe.marvisur.sgldqr.puertos.ImagenQRPort;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas unitarias de ZXingAdapter (ImagenQRPort)")
class ZXingAdapterTest {

    private ImagenQRPort zxingAdapter;

    @BeforeEach
    void setUp() {
        zxingAdapter = new ZXingAdapter();
    }

    @Test
    @DisplayName("Ciclo completo de generación PNG y posterior decodificación óptica con ZXing")
    void testCicloPNG_GeneracionYDecodificacionConZXing() {
        String contenidoOriginal = "MARVISUR:SGL-DQR:ENVIO:9a12c48b-7e61-4cfa-8941-bf17b6dc1234";
        int tamano = 300;

        // 1. Generar PNG
        byte[] bytesPNG = zxingAdapter.generarPNG(contenidoOriginal, tamano);
        assertNotNull(bytesPNG);
        assertTrue(bytesPNG.length > 0, "Los bytes generados no deben estar vacíos");

        // Cabecera mágica de archivo PNG: 0x89 0x50 0x4E 0x47 (89 'P' 'N' 'G')
        assertEquals((byte) 0x89, bytesPNG[0]);
        assertEquals((byte) 0x50, bytesPNG[1]);
        assertEquals((byte) 0x4E, bytesPNG[2]);
        assertEquals((byte) 0x47, bytesPNG[3]);

        // 2. Decodificar PNG
        String contenidoDecodificado = zxingAdapter.decodificar(bytesPNG);
        assertNotNull(contenidoDecodificado);
        assertEquals(contenidoOriginal, contenidoDecodificado, "El contenido decodificado debe coincidir exactamente con el original");
    }

    @Test
    @DisplayName("Generación de QR con contenido nulo, vacío o tamaño inválido lanza excepciones")
    void testGenerarPNG_Validaciones() {
        assertThrows(NullPointerException.class, () -> zxingAdapter.generarPNG(null, 300));
        assertThrows(IllegalArgumentException.class, () -> zxingAdapter.generarPNG("   ", 300));
        assertThrows(IllegalArgumentException.class, () -> zxingAdapter.generarPNG("contenido", 0));
        assertThrows(IllegalArgumentException.class, () -> zxingAdapter.generarPNG("contenido", -100));
    }

    @Test
    @DisplayName("Decodificación con arreglo vacío o bytes corruptos lanza excepciones controladas")
    void testDecodificar_BytesInvalidos() {
        assertThrows(NullPointerException.class, () -> zxingAdapter.decodificar(null));
        assertThrows(IllegalArgumentException.class, () -> zxingAdapter.decodificar(new byte[0]));

        byte[] bytesCorruptos = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        assertThrows(ExcepcionDominio.class, () -> zxingAdapter.decodificar(bytesCorruptos));
    }
}
