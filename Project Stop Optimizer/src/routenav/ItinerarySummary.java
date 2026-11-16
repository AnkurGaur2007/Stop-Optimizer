package routenav;

import java.util.List;

/**
 * One itinerary in the response: the stop order plus the rounded totals.
 */
public class ItinerarySummary {
    private final List<String> stops;
    private final double distanceKm;
    private final double timeMinutes;

    public ItinerarySummary(List<String> stops, double distanceKm, double timeMinutes) {
        this.stops = stops;
        this.distanceKm = distanceKm;
        this.timeMinutes = timeMinutes;
    }

    public List<String> getStops() {
        return stops;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public double getTimeMinutes() {
        return timeMinutes;
    }
}
