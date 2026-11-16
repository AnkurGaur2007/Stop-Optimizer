package routenav;

import java.util.List;

/**
 * The answer of GET /api/map: every location with a position, and every road
 * once, so the page can draw the city without inventing anything.
 */
public class CityMapResponse {
    private final List<CityPoint> locations;
    private final List<CityRoad> roads;

    public CityMapResponse(List<CityPoint> locations, List<CityRoad> roads) {
        this.locations = locations;
        this.roads = roads;
    }

    public List<CityPoint> getLocations() {
        return locations;
    }

    public List<CityRoad> getRoads() {
        return roads;
    }
}
