package routenav;

/**
 * One road for the drawn map. Every road is listed once here, not once per
 * direction, because the map draws a single line for it.
 */
public class CityRoad {
    private final String from;
    private final String to;
    private final double distanceKm;
    private final Traffic traffic;

    public CityRoad(String from, String to, double distanceKm, Traffic traffic) {
        this.from = from;
        this.to = to;
        this.distanceKm = distanceKm;
        this.traffic = traffic;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public Traffic getTraffic() {
        return traffic;
    }
}
