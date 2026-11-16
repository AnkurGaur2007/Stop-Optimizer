package routenav;

import java.util.List;

/**
 * What the page sends to POST /api/route.
 */
public class RouteRequest {
    private String start;
    private String end;
    private List<String> stops;
    private boolean returnToStart;

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public List<String> getStops() {
        return stops;
    }

    public void setStops(List<String> stops) {
        this.stops = stops;
    }

    public boolean isReturnToStart() {
        return returnToStart;
    }

    public void setReturnToStart(boolean returnToStart) {
        this.returnToStart = returnToStart;
    }
}
