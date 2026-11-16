package routenav;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the closest emergency place by travel time, not by distance.
 */
public class EmergencyFinder {
    private EmergencyFinder() {
    }

    /**
     * The places a type covers, or null when the type is not known. A list is
     * used so a type can have more than one place without changing the caller.
     */
    public static List<String> targets(String type) {
        if ("hospital".equals(type)) {
            return single("City Hospital");
        }
        if ("fire".equals(type)) {
            return single("Fire Station");
        }
        if ("police".equals(type)) {
            return single("North Police Station");
        }
        return null;
    }

    /**
     * The fastest path from the start to any of the targets, or null when none
     * of them can be reached.
     */
    public static PathResult nearest(CityMap map, String from, List<String> targets) {
        PathResult best = null;
        for (String target : targets) {
            PathResult result = Dijkstra.shortestPath(map, from, target);
            if (result == null) {
                continue;
            }
            if (best == null || result.getTotalTimeMinutes() < best.getTotalTimeMinutes()) {
                best = result;
            }
        }
        return best;
    }

    private static List<String> single(String name) {
        List<String> names = new ArrayList<>();
        names.add(name);
        return names;
    }
}
