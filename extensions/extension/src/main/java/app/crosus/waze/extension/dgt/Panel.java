package app.crosus.waze.extension.dgt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Un panel de mensaje variable: posición (del XML de localizaciones) y texto actual (del XML de tiempo real). */
public final class Panel {
    public final String id;
    public double latitude = Double.NaN;
    public double longitude = Double.NaN;
    /** Hacia dónde mira el panel (rumbo del tráfico que lo lee), en grados. NaN si la DGT no lo da. */
    public double bearing = Double.NaN;
    /** Carretera, si viene en los datos (p. ej. "MA-20"). Puede ser null. */
    public String road;

    private volatile List<String> textLines = Collections.emptyList();

    public Panel(String id) {
        this.id = id;
    }

    public boolean hasLocation() {
        return !Double.isNaN(latitude) && !Double.isNaN(longitude);
    }

    public List<String> getTextLines() {
        return textLines;
    }

    public void setTextLines(List<String> lines) {
        textLines = Collections.unmodifiableList(new ArrayList<>(lines));
    }

    public boolean hasText() {
        return !textLines.isEmpty();
    }

    /** Texto del panel en una sola cadena, línea a línea. */
    public String text() {
        return String.join("\n", textLines);
    }

    @Override
    public String toString() {
        return "Panel{" + id + " " + latitude + "," + longitude + " bearing=" + bearing
                + " road=" + road + " text=" + textLines + "}";
    }
}
