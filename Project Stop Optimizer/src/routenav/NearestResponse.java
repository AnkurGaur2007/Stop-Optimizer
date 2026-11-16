package routenav;

import java.util.ArrayList;
import java.util.List;

/**
 * The answer of GET /api/nearest: one path to the closest emergency place.
 */
public class NearestResponse {
    private final List<String> path;
    private final double distanceKm;
    private final double timeMinutes;

    public NearestResponse(List<String> path, double distanceKm, double timeMinutes) {
        this.path = new ArrayList<>(path);
        this.distanceKm = distanceKm;
        this.timeMinutes = timeMinutes;
    }

    public List<String> getPath() {
        return new ArrayList<>(path);
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public double getTimeMinutes() {
        return timeMinutes;
    }
}
