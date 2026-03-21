package game.vehicle;

import game.city.City;
import game.economy.Storage;
import game.goods.Good;
import game.goods.GoodType;
import game.industry.Industry;
import game.transport.Stop;

/**
 * A vehicle specialized for transporting goods of a specific type.
 * Loads from industry storage at stops near industries,
 * unloads to cities at stops near cities and earns delivery income.
 */
public class Truck extends Vehicle {
    protected GoodType goodType;
    private int loadedAmount = 0;

    /**
     * Constructs a truck with the given attributes.
     * @param id The unique vehicle ID.
     * @param speed The movement speed.
     * @param capacity The maximum cargo capacity.
     * @param maintenanceCost The periodic maintenance cost.
     * @param goodType The type of good this truck transports.
     */
    public Truck(int id, double speed, int capacity, double maintenanceCost, GoodType goodType) {
        super(id, speed, capacity, maintenanceCost);
        this.goodType = goodType;
    }

    /**
     * Loads cargo from the industry storage at this stop.
     * Only loads if the stop is near an industry that has the matching good type.
     * @param stop The stop to load cargo from.
     */
    @Override
    public void loadCargo(Stop stop) {
        Industry ind = stop.getNearbyIndustry();
        if (ind == null) return;
        Storage storage = ind.getStorage(goodType);
        if (storage == null || storage.isEmpty()) return;
        int space = capacity - loadedAmount;
        if (space <= 0) return;
        int taken = storage.remove(Math.min(space, storage.getCurrentAmount()));
        loadedAmount += taken;
        cargo = new Good(goodType, loadedAmount);
    }

    /**
     * Unloads cargo at a stop. If the stop is near a city, delivers for income via city pricing.
     * If the stop is near an industry, pushes goods into its INPUT storage only
     * (not the same type as the industry's output, to avoid self-delivery loops).
     * @param stop The stop to unload cargo at.
     */
    @Override
    public void unloadCargo(Stop stop) {
        if (loadedAmount <= 0) return;
        City city = stop.getNearbyCity();
        if (city != null) {
            int delivered = loadedAmount;
            loadedAmount = 0;
            cargo = null;
            double income = city.acceptDelivery(new Good(goodType, delivered), delivered);
            if (deliveryCallback != null) deliveryCallback.onDelivery(income);
            return;
        }
        Industry ind = stop.getNearbyIndustry();
        if (ind != null) {
            Storage input = ind.getStorage(goodType);
            if (input == null || input.isFull()) return;
            if (ind.isOutputGood(goodType)) return;
            int accepted = input.add(loadedAmount);
            loadedAmount -= accepted;
            cargo = loadedAmount > 0 ? new Good(goodType, loadedAmount) : null;
        }
    }

    /** @return The type of good this truck transports. */
    public GoodType getGoodType() { return goodType; }

    @Override
    public String getName() {
        return getClass().getSimpleName() + " #" + id + " (" + goodType.name().toLowerCase() + ")";
    }

    /**
     * Sets the type of good this truck transports.
     * @param goodType The good type.
     */
    public void setGoodType(GoodType goodType) { this.goodType = goodType; }

    /** @return The amount of cargo currently loaded. */
    public int getLoadedAmount() { return loadedAmount; }
}
