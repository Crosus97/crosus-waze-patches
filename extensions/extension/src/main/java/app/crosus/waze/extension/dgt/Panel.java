package app.crosus.waze.extension.dgt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Un panel de mensaje variable: posición (del XML de localizaciones) y mensaje actual (del XML de tiempo real). */
public final class Panel {
    public final String id;
    public double latitude = Double.NaN;
    public double longitude = Double.NaN;
    /** Hacia dónde mira el panel (rumbo del tráfico que lo lee), en grados. NaN si la DGT no lo da. */
    public double bearing = Double.NaN;
    /** Carretera, si viene en los datos (p. ej. "MA-20"). Puede ser null. */
    public String road;

    /**
     * Una página del panel. Muchos paneles alternan varias (p. ej. "OBRAS A 2 KM" y luego "CARRIL IZQDO CORTADO").
     * La DGT pone pictogramas a izquierda y derecha del texto.
     */
    public static final class Page {
        public final List<String> lines;
        /** Códigos DGT de pictograma (p. ej. "R301100I" = límite 100). Sin huecos vacíos ("NEGRO"). */
        public final List<String> pictogramCodes;
        /** URL de la imagen oficial de cada pictograma, en el mismo orden que los códigos. */
        public final List<String> pictogramUrls;

        public Page(List<String> lines, List<String> pictogramCodes, List<String> pictogramUrls) {
            this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
            this.pictogramCodes = Collections.unmodifiableList(new ArrayList<>(pictogramCodes));
            this.pictogramUrls = Collections.unmodifiableList(new ArrayList<>(pictogramUrls));
        }

        public boolean isEmpty() {
            return lines.isEmpty() && pictogramCodes.isEmpty();
        }
    }

    private volatile List<Page> pages = Collections.emptyList();

    public Panel(String id) {
        this.id = id;
    }

    public boolean hasLocation() {
        return !Double.isNaN(latitude) && !Double.isNaN(longitude);
    }

    public List<Page> getPages() {
        return pages;
    }

    public void setPages(List<Page> newPages) {
        List<Page> clean = new ArrayList<>();
        for (Page p : newPages) if (!p.isEmpty()) clean.add(p);
        pages = Collections.unmodifiableList(clean);
    }

    /** Atajo: una sola página de texto sin pictogramas (tests y modo prueba). */
    public void setTextLines(List<String> lines) {
        List<Page> list = new ArrayList<>();
        list.add(new Page(lines, Collections.<String>emptyList(), Collections.<String>emptyList()));
        setPages(list);
    }

    /** Todas las líneas de texto de todas las páginas, en orden. */
    public List<String> getTextLines() {
        List<String> all = new ArrayList<>();
        for (Page p : pages) all.addAll(p.lines);
        return all;
    }

    /** Un panel "tiene mensaje" si muestra texto o algún pictograma. */
    public boolean hasText() {
        return !pages.isEmpty();
    }

    /** Primer pictograma con imagen del panel (el que se usa como icono del aviso), o null. */
    public String firstPictogramUrl() {
        for (Page p : pages) if (!p.pictogramUrls.isEmpty()) return p.pictogramUrls.get(0);
        return null;
    }

    public String firstPictogramCode() {
        for (Page p : pages) if (!p.pictogramCodes.isEmpty()) return p.pictogramCodes.get(0);
        return null;
    }

    /** Texto del panel: líneas separadas por salto de línea, páginas separadas por " · ". */
    public String text() {
        StringBuilder sb = new StringBuilder();
        for (Page p : pages) {
            if (p.lines.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" · ");
            sb.append(String.join("\n", p.lines));
        }
        return sb.toString();
    }

    /** Identifica el mensaje actual (texto y pictogramas), para no repetir el mismo aviso. */
    public String signature() {
        StringBuilder sb = new StringBuilder(text());
        for (Page p : pages) sb.append('|').append(p.pictogramCodes);
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder pics = new StringBuilder();
        for (Page p : pages) pics.append(p.pictogramCodes);
        return "Panel{" + id + " " + latitude + "," + longitude + " bearing=" + bearing
                + " road=" + road + " text=" + getTextLines() + " pictos=" + pics + "}";
    }
}
