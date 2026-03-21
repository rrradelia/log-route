package game.util;

/**
 * Enumeration of bridge types with varying cost, max length, and speed limits.
 */
public enum BridgeType {
    WOODEN(500, 3, 0.5),
    STONE(1500, 5, 0.75),
    STEEL(3000, 10, 1.0);

    private final int cost;
    private final int maxLength;
    private final double speedLimit;

    BridgeType(int cost, int maxLength, double speedLimit) {
        this.cost = cost;
        this.maxLength = maxLength;
        this.speedLimit = speedLimit;
    }

    /** @return The construction cost in dollars. */
    public int getCost() { return cost; }

    /** @return The maximum span length in water tiles. */
    public int getMaxLength() { return maxLength; }

    /** @return The speed limit multiplier for vehicles on this bridge. */
    public double getSpeedLimit() { return speedLimit; }
}
