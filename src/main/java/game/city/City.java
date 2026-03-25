package game.city;

import game.economy.Storage;
import game.goods.Good;
import game.goods.GoodType;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Represents a city on the game map that generates demand for goods and passengers.
 * Cities accept deliveries from vehicles and reward the player based on current demand.
 */
public class City {

    private final String name;
    private final int    tileX;
    private final int    tileY;
    private final int    widthTiles;
    private final int    heightTiles;

    private final IntegerProperty passengerDemand = new SimpleIntegerProperty(50);
    private final Storage ironStorage;
    private final Storage paperStorage;

    private int  demandChangeDelta           = 1;
    private long accumulatedMs               = 0;
    private static final int DEMAND_INTERVAL = 10_000;

    /**
     * Constructs a City at the given map position and tile size.
     * @param name the display name of this city
     * @param tileX the x-coordinate of the top-left tile
     * @param tileY the y-coordinate of the top-left tile
     * @param widthTiles the width in tiles
     * @param heightTiles the height in tiles
     */
    public City(String name, int tileX, int tileY, int widthTiles, int heightTiles) {
        this.name        = name;
        this.tileX       = tileX;
        this.tileY       = tileY;
        this.widthTiles  = widthTiles;
        this.heightTiles = heightTiles;
        ironStorage  = new Storage(GoodType.IRON,  200);
        paperStorage = new Storage(GoodType.PAPER, 200);
    }

    /**
     * Advances the city's demand simulation by the given time delta.
     * @param deltaMs simulated milliseconds elapsed since the last frame
     */
    public void tick(long deltaMs) {
        accumulatedMs += deltaMs;
        while (accumulatedMs >= DEMAND_INTERVAL) {
            accumulatedMs -= DEMAND_INTERVAL;
            updateDemand();
        }
    }

    /**
     * Shifts passenger demand by one unit, reversing at bounds.
     */
    private void updateDemand() {
        int newDemand = passengerDemand.get() + demandChangeDelta;
        if (newDemand >= 200) demandChangeDelta = -1;
        if (newDemand <= 10)  demandChangeDelta =  1;
        passengerDemand.set(Math.max(10, Math.min(200, newDemand)));
    }

    /**
     * Processes a delivery of goods or passengers to this city.
     * Income scales with the city's current passenger demand (as a proxy for overall demand).
     * Goods are stored in the city's warehouses.
     * @param good the type and quantity of the delivered cargo
     * @param amount the number of units delivered
     * @return the income earned from this delivery
     */
    public double acceptDelivery(Good good, int amount) {
        double basePrice = switch (good.getType()) {
            case IRON       -> 15.0;
            case PAPER      -> 20.0;
            case PASSENGERS -> 10.0;
            case WOOD       -> 8.0;
        };
        double demandMultiplier = passengerDemand.get() / 100.0;
        double income = amount * basePrice * demandMultiplier;
        if (good.getType() == GoodType.IRON)  ironStorage.add(amount);
        if (good.getType() == GoodType.PAPER) paperStorage.add(amount);
        return income;
    }

    /**
     * Reduces the passenger demand by the given amount (passengers picked up by a bus).
     * Demand will not drop below 10.
     * @param amount the number of passengers picked up
     */
    public void reducePassengerDemand(int amount) {
        passengerDemand.set(Math.max(10, passengerDemand.get() - amount));
    }

    /** @return name string */
    public String getName() { return name; }

    /** @return tileX */
    public int getTileX() { return tileX; }

    /** @return tileY */
    public int getTileY() { return tileY; }

    /** @return width in tiles */
    public int getWidthTiles() { return widthTiles; }

    /** @return height in tiles */
    public int getHeightTiles() { return heightTiles; }

    /** @return passenger demand value */
    public int getPassengerDemand() { return passengerDemand.get(); }

    /** @return iron Storage */
    public Storage getIronStorage() { return ironStorage; }

    /** @return paper Storage */
    public Storage getPaperStorage() { return paperStorage; }

    /** @return the IntegerProperty for UI binding */
    public IntegerProperty passengerDemandProperty() { return passengerDemand; }
}
