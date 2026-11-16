package routenav;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Shortest path by travel time. Distance is carried along as a second total.
 */
public class Dijkstra {

    /**
     * One entry in the priority queue. Two entries with the same time are
     * ordered by location name, which keeps the result deterministic.
     */
    private static class Step implements Comparable<Step> {
        private final String name;
        private final double time;

        Step(String name, double time) {
            this.name = name;
            this.time = time;
        }

        public int compareTo(Step other) {
            if (this.time < other.time) {
                return -1;
            }
            if (this.time > other.time) {
                return 1;
            }
            return this.name.compareTo(other.name);
        }
    }

    /**
     * Returns the fastest path from start to end, or null if end cannot be reached.
     */
    public static PathResult shortestPath(CityMap map, String start, String end) {
        map.requireLocation(start);
        map.requireLocation(end);

        Map<String, Double> bestTime = new HashMap<>();
        Map<String, Double> bestDistance = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        for (String name : map.getLocationNames()) {
            bestTime.put(name, Double.POSITIVE_INFINITY);
            bestDistance.put(name, Double.POSITIVE_INFINITY);
        }
        bestTime.put(start, 0.0);
        bestDistance.put(start, 0.0);

        PriorityQueue<Step> queue = new PriorityQueue<>();
        queue.add(new Step(start, 0.0));

        while (!queue.isEmpty()) {
            Step current = queue.poll();

            // There is no decrease-key here. A location reached again with a
            // better time is just pushed again, so the old queue entry becomes
            // stale and is skipped by this check.
            if (current.time > bestTime.get(current.name)) {
                continue;
            }
            if (current.name.equals(end)) {
                break;
            }

            for (Road road : map.getRoads(current.name)) {
                double newTime = current.time + road.travelMinutes();
                if (newTime < bestTime.get(road.getTo())) {
                    bestTime.put(road.getTo(), newTime);
                    bestDistance.put(road.getTo(), bestDistance.get(current.name) + road.getDistanceKm());
                    previous.put(road.getTo(), current.name);
                    queue.add(new Step(road.getTo(), newTime));
                }
            }
        }

        if (bestTime.get(end) == Double.POSITIVE_INFINITY) {
            return null;
        }

        // Walk the "previous" links backwards, then flip the list.
        List<String> path = new ArrayList<>();
        String at = end;
        while (at != null) {
            path.add(at);
            at = previous.get(at);
        }
        Collections.reverse(path);

        return new PathResult(path, bestDistance.get(end), bestTime.get(end));
    }
}
