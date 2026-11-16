package routenav;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Holds the city and the planner, checks the request and turns the engine
 * result into the JSON structure the page expects.
 */
public class RouteService {
    private final CityMap map = CityData.buildCity();
    private final RoutePlanner planner = new RoutePlanner(map);

    public List<String> getLocationNames() {
        List<String> names = map.getLocationNames();
        Collections.sort(names);
        return names;
    }

    /**
     * The drawn map needs a position for every place and one road per pair.
     * CityMap stores each road twice, once per direction, so a road is added
     * to the answer the first time it is seen and skipped the second time.
     */
    public CityMapResponse getCityMap() {
        List<CityPoint> points = new ArrayList<>();
        List<String> names = map.getLocationNames();
        Collections.sort(names);
        for (String name : names) {
            double[] point = CityLayout.get(name);
            if (point == null) {
                throw new IllegalStateException("No map position for " + name);
            }
            points.add(new CityPoint(name, point[0], point[1]));
        }

        List<CityRoad> roads = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String from : names) {
            for (Road road : map.getRoads(from)) {
                String key = from.compareTo(road.getTo()) < 0
                        ? from + "|" + road.getTo()
                        : road.getTo() + "|" + from;
                if (seen.add(key)) {
                    roads.add(new CityRoad(from, road.getTo(),
                            round(road.getDistanceKm()), road.getTraffic()));
                }
            }
        }
        return new CityMapResponse(points, roads);
    }

    public RouteResponse planRoute(RouteRequest request) {
        String start = trim(request.getStart());
        String end = trim(request.getEnd());
        boolean returnToStart = request.isReturnToStart();

        List<String> stops = new ArrayList<>();
        if (request.getStops() != null) {
            for (String stop : request.getStops()) {
                String cleaned = trim(stop);
                if (!cleaned.isEmpty()) {
                    stops.add(cleaned);
                }
            }
        }

        // The page is never trusted, so every check happens here before the
        // planner runs and the engine's own exception text never reaches the page.
        if (start.isEmpty()) {
            throw new IllegalArgumentException("Pick a start location.");
        }
        if (!returnToStart && end.isEmpty()) {
            throw new IllegalArgumentException("Pick a destination location.");
        }
        // A round trip has no destination, so the sent end is ignored completely.
        if (returnToStart) {
            end = "";
        }
        if (!map.hasLocation(start)) {
            throw new IllegalArgumentException("Unknown location: " + start + ".");
        }
        if (!returnToStart && !map.hasLocation(end)) {
            throw new IllegalArgumentException("Unknown location: " + end + ".");
        }
        for (String stop : stops) {
            if (!map.hasLocation(stop)) {
                throw new IllegalArgumentException("Unknown location: " + stop + ".");
            }
        }
        for (int i = 0; i < stops.size(); i++) {
            for (int j = i + 1; j < stops.size(); j++) {
                if (stops.get(i).equals(stops.get(j))) {
                    throw new IllegalArgumentException(
                            "Stop '" + stops.get(i) + "' appears more than once.");
                }
            }
        }
        for (String stop : stops) {
            if (stop.equals(start) || (!returnToStart && stop.equals(end))) {
                throw new IllegalArgumentException(
                        "Stop '" + stop + "' is already your start or destination.");
            }
        }
        if (!returnToStart && start.equals(end)) {
            throw new IllegalArgumentException(
                    "Start and destination are the same place. Tick 'return to start' for a round trip.");
        }

        TripPlan plan;
        try {
            plan = planner.plan(start, end, stops, returnToStart);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Safety net, the checks above should catch normal mistakes first.
            throw new IllegalStateException("Could not plan this route.");
        }

        return buildResponse(plan);
    }

    /**
     * The fastest way from one place to the closest emergency place of a type.
     */
    public NearestResponse nearestRoute(String from, String type) {
        String start = trim(from);
        String kind = trim(type);

        if (start.isEmpty()) {
            throw new IllegalArgumentException("Pick a start location.");
        }
        if (!map.hasLocation(start)) {
            throw new IllegalArgumentException("Unknown location: " + start + ".");
        }
        if (kind.isEmpty()) {
            throw new IllegalArgumentException("Pick an emergency type: hospital, fire or police.");
        }
        List<String> targets = EmergencyFinder.targets(kind);
        if (targets == null) {
            throw new IllegalArgumentException(
                    "Unknown emergency type: " + kind + ". Use hospital, fire or police.");
        }

        PathResult result = EmergencyFinder.nearest(map, start, targets);
        if (result == null) {
            throw new IllegalStateException("No emergency route from " + start + ".");
        }
        return new NearestResponse(result.getPath(),
                round(result.getTotalDistanceKm()), round(result.getTotalTimeMinutes()));
    }

    private RouteResponse buildResponse(TripPlan plan) {
        String method = plan.getMethod();
        boolean guaranteedBest = method.equals("tried every order")
                || method.equals("direct route");

        ItinerarySummary yourOrder = summarize(plan.getYourOrder());
        ItinerarySummary optimized = summarize(plan.getOptimized());

        // The page shows the optimized route leg by leg, so these legs come from
        // the optimized itinerary even when that is the user's own order. The
        // user's own legs are sent too, so the map can draw the dashed line.
        List<LegSummary> legs = buildLegs(plan.getOptimized());
        List<LegSummary> originalLegs = buildLegs(plan.getYourOrder());

        double saved = plan.getYourOrder().getTotalTimeMinutes()
                - plan.getOptimized().getTotalTimeMinutes();
        if (saved < 0) {
            saved = 0;
        }

        return new RouteResponse(true, "Route planned.", method, guaranteedBest,
                yourOrder, optimized, legs, originalLegs, round(saved));
    }

    private List<LegSummary> buildLegs(TripPlan.Itinerary itinerary) {
        List<String> chosenStops = itinerary.getStops();
        List<LegSummary> legs = new ArrayList<>();
        for (PathResult leg : itinerary.getLegs()) {
            legs.add(new LegSummary(leg.getStart(), leg.getEnd(), leg.getPath(),
                    round(leg.getTotalDistanceKm()), round(leg.getTotalTimeMinutes()),
                    buildSegments(leg.getPath()), passesThrough(leg, chosenStops)));
        }
        return legs;
    }

    /**
     * The other chosen stops a leg's path crosses. After the planner's
     * first-visit reordering this is usually empty, but a later leg can still
     * pass a stop that was already visited, and the page shows that.
     */
    private List<String> passesThrough(PathResult leg, List<String> chosenStops) {
        List<String> found = new ArrayList<>();
        for (String place : leg.getPath()) {
            if (place.equals(leg.getStart()) || place.equals(leg.getEnd())) {
                continue;
            }
            if (chosenStops.contains(place) && !found.contains(place)) {
                found.add(place);
            }
        }
        return found;
    }

    /**
     * Walks a leg path and reads each road from the city, so the page can show
     * how much of the leg ran through each traffic level.
     */
    private List<LegSegment> buildSegments(List<String> path) {
        List<LegSegment> segments = new ArrayList<>();
        for (int i = 0; i < path.size() - 1; i++) {
            String from = path.get(i);
            String to = path.get(i + 1);
            Road road = findRoad(from, to);
            if (road == null) {
                throw new IllegalStateException("No road from " + from + " to " + to);
            }
            segments.add(new LegSegment(from, to, round(road.getDistanceKm()), road.getTraffic()));
        }
        return segments;
    }

    private Road findRoad(String from, String to) {
        for (Road road : map.getRoads(from)) {
            if (road.getTo().equals(to)) {
                return road;
            }
        }
        return null;
    }

    private ItinerarySummary summarize(TripPlan.Itinerary itinerary) {
        return new ItinerarySummary(itinerary.getStops(),
                round(itinerary.getTotalDistanceKm()),
                round(itinerary.getTotalTimeMinutes()));
    }

    /**
     * The engine adds doubles, so totals can carry noise like 103.39999999999999.
     * Rounding happens once, here at the edge.
     */
    private double round(double value) {
        double rounded = Math.round(value * 10) / 10.0;
        if (rounded == 0.0) {
            return 0.0;
        }
        return rounded;
    }

    private String trim(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
