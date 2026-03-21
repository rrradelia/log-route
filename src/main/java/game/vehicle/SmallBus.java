package game.vehicle;

/**
 * Higher speed, lower capacity bus.
 */
public class SmallBus extends Bus {

    private static final double DEFAULT_SPEED = 2.0;
    private static final int DEFAULT_CAPACITY = 15;
    private static final double DEFAULT_MAINTENANCE = 50.0;
    public static final double COST = 400;

    /**
     * Constructs a small bus.
     * @param id The unique vehicle ID.
     */
    public SmallBus(int id) {
        super(id, DEFAULT_SPEED, DEFAULT_CAPACITY, DEFAULT_MAINTENANCE);
        this.purchaseCost = COST;
    }
}
