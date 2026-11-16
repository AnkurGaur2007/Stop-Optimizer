package routenav;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plans a trip through several stops and also tries to find a better stop order.
 */
public class RoutePlanner {
    public static final int MAX_BRUTE_FORCE_STOPS = 7;

    // Two routes with the same real time can add up differently by about 1e-15
    // because doubles are not exact. A difference this small means a tie, and a
    // tie must keep the order the user typed instead of swapping stops for no gain.
    public static final double TIME_EPSILON = 1e-9;

    private final CityMap map;

    public RoutePlanner(CityMap map) {
        this.map = map;
    }

    /**
     * Builds two itineraries: the stops as the user entered them, and the best
     * order this planner can find. If returnToStart is true the trip ends back
     * at the start and the end parameter is ignored.
     */
    public TripPlan plan(String start, String end, List<String> stops, boolean returnToStart) {
        map.requireLocation(start);
        if (!returnToStart) {
            map.requireLocation(end);
        }
        for (String stop : stops) {
            map.requireLocation(stop);
        }

        List<String> stopList = new ArrayList<>(stops);
        String finish = returnToStart ? start : end;

        // Run Dijkstra once from every point a leg can leave, then reuse the
        // results for every order we try. The graph is small, so this is fine.
        Map<String, Map<String, PathResult>> cache = new HashMap<>();
        List<String> departures = new ArrayList<>();
        departures.add(start);
        departures.addAll(stopList);
        List<String> destinations = new ArrayList<>(stopList);
        destinations.add(finish);

        for (String from : departures) {
            if (cache.containsKey(from)) {
                continue;
            }
            Map<String, PathResult> fromHere = new HashMap<>();
            for (String to : destinations) {
                if (fromHere.containsKey(to)) {
                    continue;
                }
                PathResult result = Dijkstra.shortestPath(map, from, to);
                if (result == null) {
                    throw new IllegalStateException("No route from " + from + " to " + to);
                }
                fromHere.put(to, result);
            }
            cache.put(from, fromHere);
        }

        List<String> userOrder = new ArrayList<>();
        userOrder.add(start);
        userOrder.addAll(stopList);
        userOrder.add(finish);
        TripPlan.Itinerary yourOrder = buildItinerary(userOrder, cache);

        if (stopList.isEmpty()) {
            return new TripPlan(yourOrder, yourOrder, "direct route");
        }

        if (stopList.size() <= MAX_BRUTE_FORCE_STOPS) {
            // Few stops: walk every possible order with plain recursion.
            Best best = new Best();
            best.order = new ArrayList<>(userOrder);
            best.time = yourOrder.getTotalTimeMinutes();
            tryEveryOrder(stopList, 0, start, finish, cache, best);
            return new TripPlan(yourOrder, reorderByFirstVisit(best.order, cache),
                    "tried every order");
        }

        // Too many stops for brute force: always hop to the closest unvisited stop.
        List<String> nearestOrder = nearestNextOrder(start, finish, stopList, cache);
        TripPlan.Itinerary nearestItinerary = reorderByFirstVisit(nearestOrder, cache);
        // The greedy order can be slower than the order the user typed, so keep
        // whichever of the two is faster. A tie stays with the user's order, so
        // the greedy order has to win by more than the double-noise epsilon.
        if (nearestItinerary.getTotalTimeMinutes()
                < yourOrder.getTotalTimeMinutes() - TIME_EPSILON) {
            return new TripPlan(yourOrder, nearestItinerary,
                    "nearest next stop (not guaranteed best)");
        }
        return new TripPlan(yourOrder, yourOrder, "nearest next stop (not guaranteed best)");
    }

    /**
     * Best order found so far, used while trying permutations.
     */
    private static class Best {
        private List<String> order;
        private double time;
    }

    /**
     * Swaps the stops around index by index, which visits every permutation
     * without building separate lists. Only the time is compared here, the
     * itinerary is built once at the end for the winning order.
     */
    private void tryEveryOrder(List<String> stops, int index, String start, String finish,
                               Map<String, Map<String, PathResult>> cache, Best best) {
        if (index == stops.size()) {
            List<String> order = new ArrayList<>();
            order.add(start);
            order.addAll(stops);
            order.add(finish);
            double time = routeTime(order, cache);
            // Only replace the best order when it is faster by more than the
            // epsilon, otherwise float noise would reorder equal-time trips.
            if (time < best.time - TIME_EPSILON) {
                best.time = time;
                best.order = order;
            }
            return;
        }
        for (int i = index; i < stops.size(); i++) {
            swap(stops, index, i);
            tryEveryOrder(stops, index + 1, start, finish, cache, best);
            swap(stops, index, i);
        }
    }

    private void swap(List<String> list, int a, int b) {
        String temp = list.get(a);
        list.set(a, list.get(b));
        list.set(b, temp);
    }

    /**
     * From the current location, pick the unvisited stop with the smallest
     * travel time. Equal times go to the name that comes first alphabetically.
     */
    private List<String> nearestNextOrder(String start, String finish, List<String> stops,
                                          Map<String, Map<String, PathResult>> cache) {
        List<String> remaining = new ArrayList<>(stops);
        List<String> order = new ArrayList<>();
        order.add(start);
        String current = start;

        while (!remaining.isEmpty()) {
            String bestStop = null;
            double bestTime = Double.POSITIVE_INFINITY;
            for (String stop : remaining) {
                double time = cache.get(current).get(stop).getTotalTimeMinutes();
                if (bestStop == null || time < bestTime
                        || (time == bestTime && stop.compareTo(bestStop) < 0)) {
                    bestStop = stop;
                    bestTime = time;
                }
            }
            order.add(bestStop);
            remaining.remove(bestStop);
            current = bestStop;
        }

        order.add(finish);
        return order;
    }

    /**
     * The best stop order can still send a leg straight through a stop that is
     * numbered later, so the numbers on the map jump backwards. Walk the whole
     * path once and put the middle stops in the order they are really reached
     * first. The start stays first and the finish stays last, so a round trip
     * still begins and ends at the same place.
     */
    private TripPlan.Itinerary reorderByFirstVisit(List<String> order,
                                                   Map<String, Map<String, PathResult>> cache) {
        TripPlan.Itinerary before = buildItinerary(order, cache);
        List<String> middle = order.subList(1, order.size() - 1);

        // The service rejects a repeated stop before planning, but this class can
        // be called directly. Leave that case exactly as it was rather than
        // dropping one of the two copies.
        if (hasDuplicate(middle)) {
            return before;
        }

        List<String> fullPath = joinLegPaths(before.getLegs());
        List<String> newOrder = new ArrayList<>();
        newOrder.add(order.get(0));

        List<String> placed = new ArrayList<>();
        for (String place : fullPath) {
            if (middle.contains(place) && !placed.contains(place)) {
                placed.add(place);
                newOrder.add(place);
            }
        }
        // A connected city always shows every stop on the path, but if one is
        // missing it is kept at the end instead of being lost.
        for (String stop : middle) {
            if (!placed.contains(stop)) {
                newOrder.add(stop);
            }
        }
        newOrder.add(order.get(order.size() - 1));

        // Reordering must never make the trip slower. If it somehow does, keep
        // the order the search found.
        TripPlan.Itinerary after = buildItinerary(newOrder, cache);
        if (after.getTotalTimeMinutes() > before.getTotalTimeMinutes() + TIME_EPSILON) {
            return before;
        }
        return after;
    }

    /**
     * Joins the legs into one path. The last place of a leg is the first place
     * of the next one, so that repeated joint is only added once.
     */
    private List<String> joinLegPaths(List<PathResult> legs) {
        List<String> path = new ArrayList<>();
        for (PathResult leg : legs) {
            List<String> legPath = leg.getPath();
            for (int i = 0; i < legPath.size(); i++) {
                if (i == 0 && !path.isEmpty()) {
                    continue;
                }
                path.add(legPath.get(i));
            }
        }
        return path;
    }

    private boolean hasDuplicate(List<String> list) {
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                if (list.get(i).equals(list.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }

    private double routeTime(List<String> order, Map<String, Map<String, PathResult>> cache) {
        double time = 0;
        for (int i = 0; i + 1 < order.size(); i++) {
            time = time + cache.get(order.get(i)).get(order.get(i + 1)).getTotalTimeMinutes();
        }
        return time;
    }

    private TripPlan.Itinerary buildItinerary(List<String> order,
                                              Map<String, Map<String, PathResult>> cache) {
        List<PathResult> legs = new ArrayList<>();
        for (int i = 0; i + 1 < order.size(); i++) {
            legs.add(cache.get(order.get(i)).get(order.get(i + 1)));
        }
        return new TripPlan.Itinerary(order, legs);
    }
}
