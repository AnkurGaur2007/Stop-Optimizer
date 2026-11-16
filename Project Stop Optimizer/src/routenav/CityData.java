package routenav;

/**
 * The invented city used by the demo and by the tests that check the planner.
 *
 * Traffic levels below are fixed values written into the data. They are not
 * live traffic data, the shortest route is just computed with these numbers.
 */
public class CityData {

    public static CityMap buildCity() {
        CityMap map = new CityMap();

        map.addLocation("College Gate");
        map.addLocation("Public Library");
        map.addLocation("Riverside Park");
        map.addLocation("Old Town Square");
        map.addLocation("Central Market");
        map.addLocation("City Hospital");
        map.addLocation("Bus Station");
        map.addLocation("Grand Mall");
        map.addLocation("City Stadium");
        map.addLocation("North Police Station");
        map.addLocation("Railway Station");
        map.addLocation("Tech Park");
        map.addLocation("City Airport");
        map.addLocation("Wholesale Market");
        map.addLocation("Fire Station");
        map.addLocation("Court House");
        map.addLocation("Art Museum");
        map.addLocation("Lakeview Zoo");

        map.addRoad("College Gate", "Public Library", 0.9, Traffic.LOW);
        map.addRoad("College Gate", "Riverside Park", 2.5, Traffic.MEDIUM);
        // Short but jammed: the route to the market goes around through the library.
        map.addRoad("College Gate", "Central Market", 1.5, Traffic.HIGH);

        map.addRoad("Public Library", "Old Town Square", 0.6, Traffic.LOW);
        map.addRoad("Public Library", "Central Market", 2.0, Traffic.LOW);
        map.addRoad("Public Library", "City Hospital", 3.0, Traffic.LOW);

        map.addRoad("Old Town Square", "Central Market", 1.0, Traffic.LOW);
        map.addRoad("Old Town Square", "City Hospital", 2.0, Traffic.MEDIUM);
        map.addRoad("Old Town Square", "Railway Station", 3.5, Traffic.LOW);
        map.addRoad("Old Town Square", "Art Museum", 2.8, Traffic.LOW);

        // Second short road that is faster to avoid: market -> mall -> bus station.
        map.addRoad("Central Market", "Bus Station", 1.2, Traffic.HIGH);
        map.addRoad("Central Market", "Grand Mall", 1.4, Traffic.LOW);
        map.addRoad("Central Market", "Wholesale Market", 3.6, Traffic.HIGH);

        map.addRoad("Bus Station", "Grand Mall", 0.8, Traffic.LOW);
        map.addRoad("Bus Station", "Railway Station", 2.8, Traffic.MEDIUM);

        map.addRoad("Grand Mall", "City Stadium", 3.0, Traffic.MEDIUM);
        map.addRoad("Grand Mall", "City Airport", 6.0, Traffic.MEDIUM);

        map.addRoad("City Stadium", "North Police Station", 1.5, Traffic.LOW);
        map.addRoad("City Stadium", "Riverside Park", 2.6, Traffic.LOW);

        map.addRoad("North Police Station", "City Hospital", 2.2, Traffic.MEDIUM);
        map.addRoad("North Police Station", "Court House", 2.5, Traffic.MEDIUM);

        map.addRoad("Railway Station", "Tech Park", 4.0, Traffic.LOW);
        map.addRoad("Railway Station", "Fire Station", 3.2, Traffic.HIGH);

        map.addRoad("Tech Park", "City Airport", 5.5, Traffic.MEDIUM);
        map.addRoad("Tech Park", "College Gate", 4.5, Traffic.LOW);

        map.addRoad("City Airport", "Wholesale Market", 4.2, Traffic.LOW);

        map.addRoad("Wholesale Market", "Fire Station", 2.4, Traffic.LOW);

        map.addRoad("Fire Station", "Court House", 1.8, Traffic.MEDIUM);

        map.addRoad("Court House", "Art Museum", 1.2, Traffic.LOW);

        map.addRoad("Art Museum", "Lakeview Zoo", 2.0, Traffic.LOW);

        map.addRoad("Lakeview Zoo", "Riverside Park", 3.4, Traffic.MEDIUM);

        map.validate();
        return map;
    }
}
