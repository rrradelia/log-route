package game.vehicle;

/**
 * Lower speed, higher capacity bus.
 */
public class BigBus extends Bus {

    private static final double DEFAULT_SPEED = 1.2;
    private static final int DEFAULT_CAPACITY = 40;
    private static final double DEFAULT_MAINTENANCE = 120.0;
    public static final double COST = 1000;

    /**
     * Constructs a big bus.
     * @param id The unique vehicle ID.
     */
    public BigBus(int id) {
        super(id, DEFAULT_SPEED, DEFAULT_CAPACITY, DEFAULT_MAINTENANCE);
        this.purchaseCost = COST;
    }
}
