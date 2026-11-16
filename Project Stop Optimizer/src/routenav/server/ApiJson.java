package routenav.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import routenav.CityMapResponse;
import routenav.CityPoint;
import routenav.CityRoad;
import routenav.ErrorResponse;
import routenav.ItinerarySummary;
import routenav.LegSegment;
import routenav.LegSummary;
import routenav.NearestResponse;
import routenav.RouteResponse;
import routenav.json.Json;

/**
 * Builds the JSON values the API returns. The field names and their order are
 * the same as the original Spring Boot app produced, so the page keeps working
 * unchanged.
 */
final class ApiJson {

    private ApiJson() {
    }

    static Object locations(List<String> names) {
        return new ArrayList<>(names);
    }

    static Object error(String message) {
        ErrorResponse error = new ErrorResponse(message);
        Map<String, Object> body = Json.newObject();
        body.put("success", error.isSuccess());
        body.put("message", error.getMessage());
        return body;
    }

    static Object cityMap(CityMapResponse answer) {
        List<Object> locations = new ArrayList<>();
        for (CityPoint point : answer.getLocations()) {
            Map<String, Object> item = Json.newObject();
            item.put("name", point.getName());
            item.put("x", point.getX());
            item.put("y", point.getY());
            locations.add(item);
        }

        List<Object> roads = new ArrayList<>();
        for (CityRoad road : answer.getRoads()) {
            Map<String, Object> item = Json.newObject();
            item.put("from", road.getFrom());
            item.put("to", road.getTo());
            item.put("distanceKm", road.getDistanceKm());
            item.put("traffic", road.getTraffic());
            roads.add(item);
        }

        Map<String, Object> body = Json.newObject();
        body.put("locations", locations);
        body.put("roads", roads);
        return body;
    }

    static Object route(RouteResponse answer) {
        Map<String, Object> body = Json.newObject();
        body.put("success", answer.isSuccess());
        body.put("message", answer.getMessage());
        body.put("method", answer.getMethod());
        body.put("guaranteedBest", answer.isGuaranteedBest());
        body.put("yourOrder", itinerary(answer.getYourOrder()));
        body.put("optimized", itinerary(answer.getOptimized()));

        List<Object> legs = new ArrayList<>();
        for (LegSummary leg : answer.getLegs()) {
            legs.add(leg(leg));
        }
        body.put("legs", legs);

        List<Object> originalLegs = new ArrayList<>();
        for (LegSummary leg : answer.getOriginalLegs()) {
            originalLegs.add(leg(leg));
        }
        body.put("originalLegs", originalLegs);

        body.put("minutesSaved", answer.getMinutesSaved());
        return body;
    }

    static Object nearest(NearestResponse answer) {
        Map<String, Object> body = Json.newObject();
        body.put("success", true);
        body.put("path", answer.getPath());
        body.put("distanceKm", answer.getDistanceKm());
        body.put("timeMinutes", answer.getTimeMinutes());
        return body;
    }

    private static Object itinerary(ItinerarySummary summary) {
        Map<String, Object> body = Json.newObject();
        body.put("stops", summary.getStops());
        body.put("distanceKm", summary.getDistanceKm());
        body.put("timeMinutes", summary.getTimeMinutes());
        return body;
    }

    private static Object leg(LegSummary leg) {
        Map<String, Object> body = Json.newObject();
        body.put("from", leg.getFrom());
        body.put("to", leg.getTo());
        body.put("path", leg.getPath());
        body.put("distanceKm", leg.getDistanceKm());
        body.put("timeMinutes", leg.getTimeMinutes());
        body.put("passesThrough", leg.getPassesThrough());

        List<Object> segments = new ArrayList<>();
        for (LegSegment segment : leg.getSegments()) {
            segments.add(segment(segment));
        }
        body.put("segments", segments);
        return body;
    }

    private static Object segment(LegSegment segment) {
        Map<String, Object> body = Json.newObject();
        body.put("from", segment.getFrom());
        body.put("to", segment.getTo());
        body.put("distanceKm", segment.getDistanceKm());
        body.put("traffic", segment.getTraffic());
        return body;
    }
}
