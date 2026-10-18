package app.crosus.waze.extension.geo;

/** Cálculos geográficos básicos. Java puro (sin Android), para poder probarlo en el PC. */
public final class Geo {
    private static final double EARTH_RADIUS_M = 6_371_000.0;

    private Geo() {
    }

    /** Distancia en metros entre dos puntos (fórmula del haversine). */
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    /** Rumbo inicial en grados [0, 360) desde el punto 1 hacia el punto 2 (0 = norte). */
    public static double bearingDegrees(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dLon = Math.toRadians(lon2 - lon1);
        double y = Math.sin(dLon) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(dLon);
        return normalize(Math.toDegrees(Math.atan2(y, x)));
    }

    /** Diferencia absoluta entre dos rumbos, en grados [0, 180]. */
    public static double angleDifference(double a, double b) {
        double d = Math.abs(normalize(a) - normalize(b));
        return d > 180 ? 360 - d : d;
    }

    /**
     * Punto al que se llega saliendo de (lat, lon) con un rumbo y una distancia dados.
     *
     * @return {latitud, longitud}
     */
    public static double[] destination(double lat, double lon, double bearingDeg, double distanceM) {
        double delta = distanceM / EARTH_RADIUS_M;
        double theta = Math.toRadians(bearingDeg);
        double phi1 = Math.toRadians(lat);
        double lambda1 = Math.toRadians(lon);
        double phi2 = Math.asin(Math.sin(phi1) * Math.cos(delta)
                + Math.cos(phi1) * Math.sin(delta) * Math.cos(theta));
        double lambda2 = lambda1 + Math.atan2(Math.sin(theta) * Math.sin(delta) * Math.cos(phi1),
                Math.cos(delta) - Math.sin(phi1) * Math.sin(phi2));
        return new double[]{Math.toDegrees(phi2), Math.toDegrees(lambda2)};
    }

    public static double normalize(double degrees) {
        double d = degrees % 360.0;
        return d < 0 ? d + 360.0 : d;
    }
}
