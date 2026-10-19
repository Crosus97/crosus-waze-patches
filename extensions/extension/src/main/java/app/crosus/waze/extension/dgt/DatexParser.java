package app.crosus.waze.extension.dgt;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Parser de los XML DATEX II de la DGT, en streaming (SAX) para no cargar media España en memoria.
 * Java puro: funciona igual en Android y en el PC.
 *
 * <p>Tiempo real: verificado con el XML publicado por la DGT (perfil VmsPublication 3.7, 19/10/2026).
 * DATEX II v3, pero el perfil exacto de la DGT puede variar. Por eso el parser es tolerante:
 * ignora namespaces, compara nombres locales sin distinguir mayúsculas y no depende de la jerarquía exacta.
 *
 * <ul>
 *   <li><b>Localizaciones</b> (DevicePublication, se actualiza cada hora): todo elemento con atributo
 *   {@code id} que contenga {@code latitude}/{@code longitude} queda registrado con esas coordenadas.
 *   Si un panel está anidado dentro de una tabla que también tiene {@code id}, las dos entradas
 *   reciben coordenadas, pero solo la del panel coincidirá con las referencias del tiempo real.</li>
 *   <li><b>Tiempo real</b> (VmsPublication, cada minuto): cada registro contiene una referencia
 *   ({@code *Reference id="..."}) al panel y líneas de texto ({@code *textLine*}).</li>
 * </ul>
 */
public final class DatexParser {

    private DatexParser() {
    }

    /** Lee el XML de localizaciones y devuelve los paneles con posición, indexados por id. */
    public static Map<String, Panel> parseLocations(InputStream in) throws Exception {
        LocationsHandler handler = new LocationsHandler();
        newParser().parse(in, handler);
        Map<String, Panel> result = new HashMap<>();
        for (Panel p : handler.byId.values()) {
            if (p.hasLocation()) result.put(p.id, p);
        }
        return result;
    }

    /**
     * Lee el XML de tiempo real y actualiza el texto de los paneles conocidos.
     * Los paneles que no aparecen o vienen sin texto se quedan vacíos (panel apagado).
     *
     * @return número de paneles con texto tras la actualización.
     */
    public static int applyStatus(InputStream in, Map<String, Panel> panels) throws Exception {
        StatusHandler handler = new StatusHandler();
        newParser().parse(in, handler);
        int withMessage = 0;
        for (Panel p : panels.values()) {
            List<Panel.Page> pages = handler.pagesById.get(p.id);
            p.setPages(pages == null ? new ArrayList<Panel.Page>() : pages);
            if (p.hasText()) withMessage++;
        }
        return withMessage;
    }

    private static SAXParser newParser() throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        // Seguridad: sin DTD ni entidades externas (evita XXE aunque la fuente sea la DGT).
        trySetFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        trySetFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
        trySetFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        return factory.newSAXParser();
    }

    private static void trySetFeature(SAXParserFactory factory, String name, boolean value) {
        try {
            factory.setFeature(name, value);
        } catch (Exception ignored) {
            // Algunas implementaciones (Android) no soportan todas las features.
        }
    }

    private static String localName(String localName, String qName) {
        String name = (localName == null || localName.isEmpty()) ? qName : localName;
        int colon = name.indexOf(':');
        return (colon >= 0 ? name.substring(colon + 1) : name).toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------ localizaciones

    private static final class LocationsHandler extends DefaultHandler {
        final Map<String, Panel> byId = new HashMap<>();
        /** Elementos abiertos que tienen id (de fuera hacia dentro). */
        private final Deque<Panel> open = new ArrayDeque<>();
        private final Deque<Boolean> openHasId = new ArrayDeque<>();
        private final StringBuilder text = new StringBuilder();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) {
            text.setLength(0);
            String id = atts.getValue("id");
            boolean hasId = id != null && !id.isEmpty();
            if (hasId) {
                Panel p = byId.get(id);
                if (p == null) {
                    p = new Panel(id);
                    byId.put(id, p);
                }
                open.push(p);
            }
            openHasId.push(hasId);
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            text.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String name = localName(localName, qName);
            String value = text.toString().trim();
            text.setLength(0);
            if (!value.isEmpty()) {
                switch (name) {
                    case "latitude":
                        setOnOpen(value, Field.LAT);
                        break;
                    case "longitude":
                        setOnOpen(value, Field.LON);
                        break;
                    case "bearing":
                        setOnOpen(value, Field.BEARING);
                        break;
                    case "roadname":
                    case "roadnumber":
                        setOnOpen(value, Field.ROAD);
                        break;
                    default:
                        break;
                }
            }
            if (openHasId.pop()) open.pop();
        }

        private enum Field {LAT, LON, BEARING, ROAD}

        /** Solo rellena el primer valor de cada campo, para que una tabla no machaque al panel. */
        private void setOnOpen(String value, Field field) {
            for (Panel p : open) {
                try {
                    switch (field) {
                        case LAT:
                            if (Double.isNaN(p.latitude)) p.latitude = Double.parseDouble(value);
                            break;
                        case LON:
                            if (Double.isNaN(p.longitude)) p.longitude = Double.parseDouble(value);
                            break;
                        case BEARING:
                            if (Double.isNaN(p.bearing)) p.bearing = Double.parseDouble(value);
                            break;
                        case ROAD:
                            if (p.road == null) p.road = value;
                            break;
                    }
                } catch (NumberFormatException ignored) {
                    // Valor no numérico: se ignora.
                }
            }
        }
    }

    // ------------------------------------------------------------------ tiempo real

    /**
     * Estructura real del perfil DGT (VmsPublication 3.7), verificada con el XML publicado:
     * <pre>
     * vmsControllerStatus
     *   vmsControllerTableReference id=""          (vacío: se ignora)
     *   vmsControllerReference id="61441"          (id del panel)
     *   vmsStatus/vmsStatus
     *     vmsMessage messageIndex="1"/vmsMessage   (una página; puede haber varias)
     *       displayAreaSettings displayAreaIndex="1"  PictogramDisplay: pictogramDisplayUrl + customPictogramCode
     *       displayAreaSettings displayAreaIndex="2"  TextDisplay: textLine con saltos de línea
     *       displayAreaSettings displayAreaIndex="3"  PictogramDisplay (derecha)
     * </pre>
     */
    private static final class StatusHandler extends DefaultHandler {
        /** Huecos de pictograma sin contenido. */
        private static final java.util.Set<String> EMPTY_PICTOGRAMS =
                new java.util.HashSet<>(java.util.Arrays.asList("NEGRO", "0", ""));

        final Map<String, List<Panel.Page>> pagesById = new HashMap<>();
        private final StringBuilder text = new StringBuilder();
        private int depth;
        private int recordDepth = -1;
        private String currentRef;
        private List<Panel.Page> currentPages;

        // Página en curso.
        private boolean inPage;
        private List<String> lines;
        private List<String> codes;
        private List<String> urls;
        private String pendingUrl;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) {
            depth++;
            text.setLength(0);
            String name = localName(localName, qName);
            String id = atts.getValue("id");
            if (currentRef == null && name.endsWith("reference") && id != null && !id.isEmpty()) {
                currentRef = id;
                currentPages = new ArrayList<>();
                recordDepth = depth - 1;
            } else if (currentRef != null && name.equals("vmsmessage") && atts.getValue("messageIndex") != null) {
                flushPage();
                inPage = true;
                lines = new ArrayList<>();
                codes = new ArrayList<>();
                urls = new ArrayList<>();
                pendingUrl = null;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            text.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String name = localName(localName, qName);
            String value = text.toString().trim();
            text.setLength(0);
            if (currentRef != null) {
                if (!inPage) {
                    // Perfil desconocido sin vmsMessage: se crea una página implícita.
                    if (!value.isEmpty() && (name.contains("textline") || name.equals("text"))) {
                        inPage = true;
                        lines = new ArrayList<>();
                        codes = new ArrayList<>();
                        urls = new ArrayList<>();
                    }
                }
                if (inPage && !value.isEmpty()) {
                    if (name.contains("textline") || name.equals("text") || name.equals("vmstext")) {
                        for (String line : value.split("\\r?\\n")) {
                            String l = line.trim();
                            if (!l.isEmpty()) lines.add(l);
                        }
                    } else if (name.equals("pictogramdisplayurl")) {
                        pendingUrl = value;
                    } else if (name.equals("custompictogramcode")) {
                        if (!EMPTY_PICTOGRAMS.contains(value)) {
                            codes.add(value);
                            urls.add(pendingUrl != null ? pendingUrl : pictogramUrl(value));
                        }
                        pendingUrl = null;
                    }
                }
                if (depth == recordDepth) {
                    flushPage();
                    if (!currentPages.isEmpty()) {
                        List<Panel.Page> existing = pagesById.get(currentRef);
                        if (existing == null) pagesById.put(currentRef, currentPages);
                        else existing.addAll(currentPages);
                    }
                    currentRef = null;
                    currentPages = null;
                    recordDepth = -1;
                }
            }
            depth--;
        }

        private void flushPage() {
            if (inPage) {
                Panel.Page page = new Panel.Page(lines, codes, urls);
                if (!page.isEmpty()) currentPages.add(page);
            }
            inPage = false;
        }
    }

    /** URL oficial de la imagen de un pictograma DGT, por si el XML no la trae. */
    public static String pictogramUrl(String code) {
        return "https://etraffic.dgt.es/estaticosEtraffic/Iconografia/pictogramas/" + code + ".png";
    }
}
