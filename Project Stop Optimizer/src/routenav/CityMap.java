package routenav;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The city graph: location name -> roads leaving that location.
 */
public class CityMap {
    private final Map<String, List<Road>> roads = new HashMap<>();
    private final List<String> names = new ArrayList<>();

    public void addLocation(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Location name must not be empty");
        }
        if (roads.containsKey(name)) {
            throw new IllegalArgumentException("Location already added: " + name);
        }
        roads.put(name, new ArrayList<Road>());
        names.add(name);
    }

    /**
     * Adds a two-way road, so both directions end up in the adjacency list.
     */
    public void addRoad(String from, String to, double distanceKm, Traffic traffic) {
        requireLocation(from);
        requireLocation(to);
        roads.get(from).add(new Road(to, distanceKm, traffic));
        roads.get(to).add(new Road(from, distanceKm, traffic));
    }

    public boolean hasLocation(String name) {
        return roads.containsKey(name);
    }

    public void requireLocation(String name) {
        if (!roads.containsKey(name)) {
            throw new IllegalArgumentException("Unknown location: " + name);
        }
    }

    public List<Road> getRoads(String name) {
        requireLocation(name);
        return roads.get(name);
    }

    public List<String> getLocationNames() {
        return new ArrayList<>(names);
    }

    /**
     * Counts the roads once, even though every road is stored twice.
     */
    public int getRoadCount() {
        int ends = 0;
        for (String name : names) {
            ends = ends + roads.get(name).size();
        }
        return ends / 2;
    }

    /**
     * Breadth-first search from the first location. If some locations are not
     * reached, the graph is disconnected and no route planner can work with it.
     */
    public void validate() {
        if (names.isEmpty()) {
            throw new IllegalStateException("City map is empty");
        }
        Set<String> visited = new HashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        String first = names.get(0);
        visited.add(first);
        queue.add(first);
        while (!queue.isEmpty()) {
            String current = queue.remove();
            for (Road road : roads.get(current)) {
                if (!visited.contains(road.getTo())) {
                    visited.add(road.getTo());
                    queue.add(road.getTo());
                }
            }
        }
        if (visited.size() != names.size()) {
            List<String> missing = new ArrayList<>();
            for (String name : names) {
                if (!visited.contains(name)) {
                    missing.add(name);
                }
            }
            throw new IllegalStateException(
                    "City map is disconnected, these locations cannot be reached from "
                            + first + ": " + missing);
        }
    }
}
