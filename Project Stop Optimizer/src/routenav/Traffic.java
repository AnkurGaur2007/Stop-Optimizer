package routenav;

/**
 * How busy a road is. The numbers are fixed in CityData, not live traffic data.
 */
public enum Traffic {
    LOW(1.0),
    MEDIUM(1.5),
    HIGH(2.0);

    private final double multiplier;

    Traffic(double multiplier) {
        this.multiplier = multiplier;
    }

    public double getMultiplier() {
        return multiplier;
    }
}
