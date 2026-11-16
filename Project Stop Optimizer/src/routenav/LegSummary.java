package routenav;

import java.util.List;

/**
 * One leg of an itinerary. The path may pass through other locations,
 * including other stops, on the way from the departure to the next stop.
 * The segments are the roads that make up that path.
 */
public class LegSummary {
    private final String from;
    private final String to;
    private final List<String> path;
    private final double distanceKm;
    private final double timeMinutes;
    private final List<LegSegment> segments;
    private final List<String> passesThrough;

    public LegSummary(String from, String to, List<String> path,
                      double distanceKm, double timeMinutes, List<LegSegment> segments,
                      List<String> passesThrough) {
        this.from = from;
        this.to = to;
        this.path = path;
        this.distanceKm = distanceKm;
        this.timeMinutes = timeMinutes;
        this.segments = segments;
        this.passesThrough = passesThrough;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public List<String> getPath() {
        return path;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public double getTimeMinutes() {
        return timeMinutes;
    }

    public List<LegSegment> getSegments() {
        return segments;
    }

    public List<String> getPassesThrough() {
        return passesThrough;
    }
}
