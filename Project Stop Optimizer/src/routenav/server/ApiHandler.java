package routenav.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import routenav.RouteRequest;
import routenav.RouteService;
import routenav.json.Json;

/**
 * Serves the four API routes:
 * GET /api/locations, GET /api/map, POST /api/route and GET /api/nearest.
 *
 * RouteController used to do this job with Spring annotations; the behaviour is
 * the same, including the HTTP status codes and the error body shape.
 */
public final class ApiHandler implements HttpHandler {

    private static final String JSON_TYPE = "application/json; charset=utf-8";
    private static final String TEXT_TYPE = "text/plain; charset=utf-8";

    private final RouteService service = new RouteService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/api/locations")) {
                if (requireMethod(exchange, "GET")) {
                    sendJson(exchange, 200, ApiJson.locations(service.getLocationNames()));
                }
            } else if (path.equals("/api/map")) {
                if (requireMethod(exchange, "GET")) {
                    sendJson(exchange, 200, ApiJson.cityMap(service.getCityMap()));
                }
            } else if (path.equals("/api/route")) {
                if (requireMethod(exchange, "POST")) {
                    planRoute(exchange);
                }
            } else if (path.equals("/api/nearest")) {
                if (requireMethod(exchange, "GET")) {
                    nearest(exchange);
                }
            } else {
                sendText(exchange, 404, "Not found");
            }
        } catch (RuntimeException e) {
            sendJson(exchange, 500, ApiJson.error("Something went wrong on the server."));
        } finally {
            exchange.close();
        }
    }

    private void planRoute(HttpExchange exchange) throws IOException {
        RouteRequest request;
        try {
            byte[] body = exchange.getRequestBody().readAllBytes();
            request = readRequest(new String(body, StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            // The old message for a missing or broken body.
            sendJson(exchange, 400, ApiJson.error("Could not read the request."));
            return;
        }

        try {
            sendJson(exchange, 200, ApiJson.route(service.planRoute(request)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            // The service throws these for every validation mistake.
            sendJson(exchange, 400, ApiJson.error(e.getMessage()));
        }
    }

    /**
     * GET /api/nearest?from=<place>&type=hospital|fire|police
     */
    private void nearest(HttpExchange exchange) throws IOException {
        Map<String, String> query = readQuery(exchange.getRequestURI().getRawQuery());
        try {
            sendJson(exchange, 200, ApiJson.nearest(
                    service.nearestRoute(query.get("from"), query.get("type"))));
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendJson(exchange, 400, ApiJson.error(e.getMessage()));
        }
    }

    /**
     * Reads the query string into a small map. A broken % escape is skipped so
     * the service can still answer with its own message.
     */
    private Map<String, String> readQuery(String raw) {
        Map<String, String> query = new HashMap<>();
        if (raw == null || raw.isEmpty()) {
            return query;
        }
        String[] pairs = raw.split("&");
        for (String pair : pairs) {
            int at = pair.indexOf('=');
            String key = at < 0 ? pair : pair.substring(0, at);
            String value = at < 0 ? "" : pair.substring(at + 1);
            try {
                query.put(URLDecoder.decode(key, StandardCharsets.UTF_8),
                        URLDecoder.decode(value, StandardCharsets.UTF_8));
            } catch (IllegalArgumentException e) {
                // A broken escape is treated as a missing value.
            }
        }
        return query;
    }

    /**
     * Reads the request body. A wrong JSON type is treated as an unreadable
     * body, like the old JSON library did.
     */
    private RouteRequest readRequest(String body) {
        Object parsed = Json.parse(body);
        if (!(parsed instanceof Map)) {
            throw new IllegalArgumentException("The body must be a JSON object");
        }
        Map<?, ?> fields = (Map<?, ?>) parsed;

        RouteRequest request = new RouteRequest();
        request.setStart(textField(fields, "start"));
        request.setEnd(textField(fields, "end"));
        request.setReturnToStart(booleanField(fields, "returnToStart"));

        Object stops = fields.get("stops");
        if (stops != null) {
            if (!(stops instanceof List)) {
                throw new IllegalArgumentException("stops must be an array");
            }
            List<String> names = new ArrayList<>();
            for (Object stop : (List<?>) stops) {
                if (stop != null && !(stop instanceof String)) {
                    throw new IllegalArgumentException("every stop must be text");
                }
                names.add((String) stop);
            }
            request.setStops(names);
        }
        return request;
    }

    private String textField(Map<?, ?> fields, String key) {
        Object value = fields.get(key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String)) {
            throw new IllegalArgumentException(key + " must be text");
        }
        return (String) value;
    }

    private boolean booleanField(Map<?, ?> fields, String key) {
        Object value = fields.get(key);
        if (value == null) {
            return false;
        }
        if (!(value instanceof Boolean)) {
            throw new IllegalArgumentException(key + " must be true or false");
        }
        return (Boolean) value;
    }

    private boolean requireMethod(HttpExchange exchange, String allowed) throws IOException {
        if (allowed.equals(exchange.getRequestMethod())) {
            return true;
        }
        exchange.getResponseHeaders().set("Allow", allowed);
        sendText(exchange, 405, "Method not allowed");
        return false;
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        send(exchange, status, JSON_TYPE, Json.write(body).getBytes(StandardCharsets.UTF_8));
    }

    private void sendText(HttpExchange exchange, int status, String body) throws IOException {
        send(exchange, status, TEXT_TYPE, body.getBytes(StandardCharsets.UTF_8));
    }

    private void send(HttpExchange exchange, int status, String contentType, byte[] body)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
