package routenav;

import java.util.List;

/**
 * The successful answer of POST /api/route.
 */
public class RouteResponse {
    private final boolean success;
    private final String message;
    private final String method;
    private final boolean guaranteedBest;
    private final ItinerarySummary yourOrder;
    private final ItinerarySummary optimized;
    private final List<LegSummary> legs;
    private final List<LegSummary> originalLegs;
    private final double minutesSaved;

    public RouteResponse(boolean success, String message, String method, boolean guaranteedBest,
                         ItinerarySummary yourOrder, ItinerarySummary optimized,
                         List<LegSummary> legs, List<LegSummary> originalLegs,
                         double minutesSaved) {
        this.success = success;
        this.message = message;
        this.method = method;
        this.guaranteedBest = guaranteedBest;
        this.yourOrder = yourOrder;
        this.optimized = optimized;
        this.legs = legs;
        this.originalLegs = originalLegs;
        this.minutesSaved = minutesSaved;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getMethod() {
        return method;
    }

    public boolean isGuaranteedBest() {
        return guaranteedBest;
    }

    public ItinerarySummary getYourOrder() {
        return yourOrder;
    }

    public ItinerarySummary getOptimized() {
        return optimized;
    }

    public List<LegSummary> getLegs() {
        return legs;
    }

    public List<LegSummary> getOriginalLegs() {
        return originalLegs;
    }

    public double getMinutesSaved() {
        return minutesSaved;
    }
}
