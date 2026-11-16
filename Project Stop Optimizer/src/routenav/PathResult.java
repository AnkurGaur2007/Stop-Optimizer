package routenav;

import java.util.ArrayList;
import java.util.List;

/**
 * One shortest path: the location names in order plus both totals.
 */
public class PathResult {
    private final List<String> path;
    private final double totalDistanceKm;
    private final double totalTimeMinutes;

    public PathResult(List<String> path, double totalDistanceKm, double totalTimeMinutes) {
        this.path = new ArrayList<>(path);
        this.totalDistanceKm = totalDistanceKm;
        this.totalTimeMinutes = totalTimeMinutes;
    }

    public List<String> getPath() {
        return new ArrayList<>(path);
    }

    public String getStart() {
        return path.get(0);
    }

    public String getEnd() {
        return path.get(path.size() - 1);
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public double getTotalTimeMinutes() {
        return totalTimeMinutes;
    }

    @Override
    public String toString() {
        return String.join(" -> ", path)
                + " (" + String.format("%.1f", totalDistanceKm) + " km, "
                + String.format("%.1f", totalTimeMinutes) + " min)";
    }
}
