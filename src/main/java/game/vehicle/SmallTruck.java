package game.vehicle;

import game.goods.GoodType;

/**
 * Fast, light load specialized truck.
 */
public class SmallTruck extends Truck {

    private static final double DEFAULT_SPEED = 1.8;
    private static final int DEFAULT_CAPACITY = 20;
    private static final double DEFAULT_MAINTENANCE = 60.0;
    public static final double COST = 5000;

    /**
     * Constructs a small truck for the given good type.
     * @param id The unique vehicle ID.
     * @param goodType The type of good to transport.
     */
    public SmallTruck(int id, GoodType goodType) {
        super(id, DEFAULT_SPEED, DEFAULT_CAPACITY, DEFAULT_MAINTENANCE, goodType);
        this.purchaseCost = COST;
    }
}
