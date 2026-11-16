package routenav;

/**
 * One road of a leg, with its traffic level. The page uses these to draw the
 * little low/medium/high bar beside each leg.
 */
public class LegSegment {
    private final String from;
    private final String to;
    private final double distanceKm;
    private final Traffic traffic;

    public LegSegment(String from, String to, double distanceKm, Traffic traffic) {
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
