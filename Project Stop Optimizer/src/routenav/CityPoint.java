package routenav;

/**
 * One location on the drawn map: its name and its position in map units.
 */
public class CityPoint {
    private final String name;
    private final double x;
    private final double y;

    public CityPoint(String name, double x, double y) {
        this.name = name;
        this.x = x;
        this.y = y;
    }

    public String getName() {
        return name;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
