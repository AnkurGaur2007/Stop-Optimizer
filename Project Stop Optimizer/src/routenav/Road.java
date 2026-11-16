package routenav;

/**
 * One road between two locations. CityMap stores a road for each direction.
 */
public class Road {
    private final String to;
    private final double distanceKm;
    private final Traffic traffic;

    public Road(String to, double distanceKm, Traffic traffic) {
        if (to == null || traffic == null) {
            throw new IllegalArgumentException("Road needs a destination and a traffic level");
        }
        if (distanceKm <= 0) {
            throw new IllegalArgumentException("Road distance must be positive: " + distanceKm);
        }
        this.to = to;
        this.distanceKm = distanceKm;
        this.traffic = traffic;
    }

    /**
     * Base speed is 30 km/h, so minutes = km / 30 * 60 * multiplier = km * 2 * multiplier.
     */
    public double travelMinutes() {
        return distanceKm * 2.0 * traffic.getMultiplier();
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

    @Override
    public String toString() {
        return to + " (" + distanceKm + " km, " + traffic + ")";
    }
}
