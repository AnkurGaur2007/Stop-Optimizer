package routenav;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Where each location sits on the drawn map. The numbers are map units inside a
 * 640 x 730 viewBox, not metres: the map is a picture, not a scale drawing.
 *
 * This is the only place that holds coordinates, so the page never keeps its own
 * copy of them. The positions came from a throwaway force-directed layout
 * (Fruchterman-Reingold, edge targets from distanceKm) that was then nudged by
 * hand so the old town sits in the middle and the airport on the edge. The
 * README says how it was made.
 */
public final class CityLayout {

    public static final double WIDTH = 640;
    public static final double HEIGHT = 730;

    private static final Map<String, double[]> POINTS = new LinkedHashMap<>();

    static {
        put("Art Museum", 203.6, 26.0);
        put("Bus Station", 382.4, 503.5);
        put("Central Market", 333.2, 427.6);
        put("City Airport", 594.0, 606.2);
        put("City Hospital", 256.8, 171.0);
        put("City Stadium", 151.6, 430.0);
        put("College Gate", 188.7, 512.3);
        put("Court House", 295.1, 26.0);
        put("Fire Station", 504.6, 173.6);
        put("Grand Mall", 304.7, 547.1);
        put("Lakeview Zoo", 46.0, 117.5);
        put("North Police Station", 175.0, 215.2);
        put("Old Town Square", 328.2, 223.2);
        put("Public Library", 273.0, 357.7);
        put("Railway Station", 462.0, 431.2);
        put("Riverside Park", 46.0, 371.6);
        put("Tech Park", 407.3, 678.0);
        put("Wholesale Market", 548.0, 355.1);
    }

    private CityLayout() {
    }

    private static void put(String name, double x, double y) {
        POINTS.put(name, new double[]{x, y});
    }

    /**
     * Returns {x, y} for a location, or null when the name is not placed yet.
     */
    public static double[] get(String name) {
        double[] point = POINTS.get(name);
        if (point == null) {
            return null;
        }
        return new double[]{point[0], point[1]};
    }

    public static int getPointCount() {
        return POINTS.size();
    }
}
