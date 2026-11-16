package routenav;

import java.util.ArrayList;
import java.util.List;

/**
 * The result of planning a trip: the stops in the user's order and in the
 * optimized order, plus a short note about how the optimized order was found.
 */
public class TripPlan {

    /**
     * One itinerary: the ordered stops, one shortest path per leg, and the totals.
     */
    public static class Itinerary {
        private final List<String> stops;
        private final List<PathResult> legs;
        private final double totalDistanceKm;
        private final double totalTimeMinutes;

        public Itinerary(List<String> stops, List<PathResult> legs) {
            this.stops = new ArrayList<>(stops);
            this.legs = new ArrayList<>(legs);
            double distance = 0;
            double time = 0;
            for (PathResult leg : legs) {
                distance = distance + leg.getTotalDistanceKm();
                time = time + leg.getTotalTimeMinutes();
            }
            this.totalDistanceKm = distance;
            this.totalTimeMinutes = time;
        }

        public List<String> getStops() {
            return new ArrayList<>(stops);
        }

        public List<PathResult> getLegs() {
            return new ArrayList<>(legs);
        }

        public double getTotalDistanceKm() {
            return totalDistanceKm;
        }

        public double getTotalTimeMinutes() {
            return totalTimeMinutes;
        }
    }

    private final Itinerary yourOrder;
    private final Itinerary optimized;
    private final String method;

    public TripPlan(Itinerary yourOrder, Itinerary optimized, String method) {
        this.yourOrder = yourOrder;
        this.optimized = optimized;
        this.method = method;
    }

    public Itinerary getYourOrder() {
        return yourOrder;
    }

    public Itinerary getOptimized() {
        return optimized;
    }

    public String getMethod() {
        return method;
    }
}
