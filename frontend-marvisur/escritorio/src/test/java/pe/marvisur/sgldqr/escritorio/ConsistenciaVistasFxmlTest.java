package pe.marvisur.sgldqr.escritorio;

import javafx.event.Event;
import javafx.fxml.FXML;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Prueba de consistencia estricta entre las vistas FXML y sus controladores asociados.
 * Se ejecuta de forma 100% headless sin inicializar el entorno gráfico de JavaFX.
 */
class ConsistenciaVistasFxmlTest {

    private static final String PAQUETE_CONTROLADORES_DEFAULT = "pe.marvisur.sgldqr.escritorio.ui";

    @Test
    @DisplayName("Todas las vistas *View.fxml son consistentes con sus controladores JavaFX")
    void testConsistenciaVistasFxml() throws Exception {
        Path rutaRecursos = resolverRutaRecursos();
        assertThat(Files.exists(rutaRecursos))
                .withFailMessage("No se encontró el directorio de recursos en: " + rutaRecursos.toAbsolutePath())
                .isTrue();

        List<Path> archivosFxml;
        try (Stream<Path> stream = Files.walk(rutaRecursos)) {
            archivosFxml = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith("View.fxml"))
                    .sorted()
                    .toList();
        }

        assertThat(archivosFxml).isNotEmpty();

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        List<String> erroresGlobales = new ArrayList<>();

        for (Path fxmlPath : archivosFxml) {
            String nombreArchivo = fxmlPath.getFileName().toString();
            try (InputStream is = Files.newInputStream(fxmlPath)) {
                Document doc = dBuilder.parse(is);
                doc.getDocumentElement().normalize();

                // 1. Obtener la clase del controlador
                String nombreClaseControlador = obtenerClaseControlador(doc, nombreArchivo);
                Class<?> claseControlador;
                try {
                    claseControlador = Class.forName(nombreClaseControlador, false, getClass().getClassLoader());
                } catch (ClassNotFoundException e) {
                    erroresGlobales.add(String.format("[%s] (d) El controlador '%s' no existe en el classpath",
                            nombreArchivo, nombreClaseControlador));
                    continue;
                }

                // 2. Analizar el árbol DOM para extraer fx:ids y manejadores de eventos
                Set<String> fxIdsEnFxml = new HashSet<>();
                Set<String> fxIdsDuplicados = new HashSet<>();
                List<ManejadorEventoXml> manejadoresEnFxml = new ArrayList<>();

                recorrerArbolDom(doc.getDocumentElement(), fxIdsEnFxml, fxIdsDuplicados, manejadoresEnFxml);

                // (c) Verificar que no hay fx:id duplicados
                if (!fxIdsDuplicados.isEmpty()) {
                    for (String dup : fxIdsDuplicados) {
                        erroresGlobales.add(String.format("[%s] (c) fx:id duplicado en la vista: '%s'",
                                nombreArchivo, dup));
                    }
                }

                // (a) Verificar que cada campo @FXML del controlador existe en la vista
                List<Field> camposFxml = obtenerCamposFxml(claseControlador);
                for (Field campo : camposFxml) {
                    String nombreCampo = campo.getName();
                    if (!fxIdsEnFxml.contains(nombreCampo)) {
                        erroresGlobales.add(String.format("[%s] (a) El campo @FXML '%s' del controlador '%s' no tiene un fx:id correspondiente en la vista",
                                nombreArchivo, nombreCampo, claseControlador.getSimpleName()));
                    }
                }

                // (b) Verificar que cada manejador '#metodo' existe en el controlador con firma válida
                for (ManejadorEventoXml handler : manejadoresEnFxml) {
                    String nombreMetodo = handler.metodo();
                    Optional<Method> metodoOpt = buscarMetodoManejador(claseControlador, nombreMetodo);
                    if (metodoOpt.isEmpty()) {
                        erroresGlobales.add(String.format("[%s] (b) El manejador '%s' referenciado en el elemento <%s> no existe en el controlador '%s' con firma válida (sin parámetros o con un parámetro Event)",
                                nombreArchivo, "#" + nombreMetodo, handler.nombreElemento(), claseControlador.getSimpleName()));
                    }
                }
            }
        }

        if (!erroresGlobales.isEmpty()) {
            StringBuilder sb = new StringBuilder("Se encontraron inconsistencias entre las vistas FXML y sus controladores:\n");
            for (String err : erroresGlobales) {
                sb.append("  - ").append(err).append("\n");
            }
            fail(sb.toString());
        }
    }

    private Path resolverRutaRecursos() {
        Path p1 = Path.of("src/main/resources");
        if (Files.exists(p1)) return p1;
        Path p2 = Path.of("frontend-marvisur/escritorio/src/main/resources");
        if (Files.exists(p2)) return p2;
        return p1;
    }

    private String obtenerClaseControlador(Document doc, String nombreArchivoFxml) {
        // Buscar atributo fx:controller en el elemento raíz
        String fxController = doc.getDocumentElement().getAttribute("fx:controller");
        if (fxController != null && !fxController.isBlank()) {
            return fxController.trim();
        }
        // Convención: NombreView.fxml -> ControladorNombre
        String base = nombreArchivoFxml.replace("View.fxml", "");
        return PAQUETE_CONTROLADORES_DEFAULT + ".Controlador" + base;
    }

    private void recorrerArbolDom(Node nodo, Set<String> idsUnicos, Set<String> idsDuplicados, List<ManejadorEventoXml> manejadores) {
        if (nodo.getNodeType() == Node.ELEMENT_NODE) {
            NamedNodeMap attrs = nodo.getAttributes();
            if (attrs != null) {
                for (int i = 0; i < attrs.getLength(); i++) {
                    Node attr = attrs.item(i);
                    String attrName = attr.getNodeName();
                    String attrValue = attr.getNodeValue();

                    // Detectar fx:id
                    if ("fx:id".equals(attrName) || attrName.endsWith(":id") || "id".equals(attr.getLocalName())) {
                        if (attrValue != null && !attrValue.isBlank()) {
                            String id = attrValue.trim();
                            if (!idsUnicos.add(id)) {
                                idsDuplicados.add(id);
                            }
                        }
                    }

                    // Detectar manejadores de eventos (onAction="#...", onMouseClicked="#...", etc.)
                    if (attrName.startsWith("on") && attrName.length() > 2 && Character.isUpperCase(attrName.charAt(2))) {
                        if (attrValue != null && attrValue.startsWith("#")) {
                            String metodo = attrValue.substring(1).trim();
                            if (!metodo.isEmpty()) {
                                manejadores.add(new ManejadorEventoXml(nodo.getNodeName(), attrName, metodo));
                            }
                        }
                    }
                }
            }
        }

        NodeList hijos = nodo.getChildNodes();
        for (int i = 0; i < hijos.getLength(); i++) {
            recorrerArbolDom(hijos.item(i), idsUnicos, idsDuplicados, manejadores);
        }
    }

    private List<Field> obtenerCamposFxml(Class<?> clazz) {
        List<Field> campos = new ArrayList<>();
        Class<?> actual = clazz;
        while (actual != null && actual != Object.class) {
            for (Field f : actual.getDeclaredFields()) {
                if (f.isAnnotationPresent(FXML.class)) {
                    campos.add(f);
                }
            }
            actual = actual.getSuperclass();
        }
        return campos;
    }

    private Optional<Method> buscarMetodoManejador(Class<?> clazz, String nombreMetodo) {
        Class<?> actual = clazz;
        while (actual != null && actual != Object.class) {
            for (Method m : actual.getDeclaredMethods()) {
                if (m.getName().equals(nombreMetodo)) {
                    // Firma válida para JavaFX: sin parámetros o con 1 parámetro de tipo Event (o subtipo)
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == 0) {
                        return Optional.of(m);
                    }
                    if (params.length == 1 && Event.class.isAssignableFrom(params[0])) {
                        return Optional.of(m);
                    }
                }
            }
            actual = actual.getSuperclass();
        }
        return Optional.empty();
    }

    private record ManejadorEventoXml(String nombreElemento, String atributoEvento, String metodo) {}
}
