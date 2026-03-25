package game.vehicle;

import game.goods.GoodType;

/**
 * Slow, heavy load specialized truck.
 */
public class LargeTruck extends Truck {

    private static final double DEFAULT_SPEED = 1.0;
    private static final int DEFAULT_CAPACITY = 60;
    private static final double DEFAULT_MAINTENANCE = 150.0;
    public static final double COST = 12000;

    /**
     * Constructs a large truck for the given good type.
     * @param id The unique vehicle ID.
     * @param goodType The type of good to transport.
     */
    public LargeTruck(int id, GoodType goodType) {
        super(id, DEFAULT_SPEED, DEFAULT_CAPACITY, DEFAULT_MAINTENANCE, goodType);
        this.purchaseCost = COST;
    }
}
