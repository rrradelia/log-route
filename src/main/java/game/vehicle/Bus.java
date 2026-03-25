package game.vehicle;

import game.city.City;
import game.goods.Good;
import game.goods.GoodType;
import game.transport.Stop;

/**
 * A vehicle specialized for transporting passengers between cities.
 * Loads passengers at city stops based on demand,
 * unloads at a different city stop and earns delivery income.
 */
public class Bus extends Vehicle {
    private int passengerCount = 0;
    private City originCity = null;

    /**
     * Constructs a bus with the given attributes.
     * @param id The unique vehicle ID.
     * @param speed The movement speed.
     * @param capacity The maximum passenger capacity.
     * @param maintenanceCost The periodic maintenance cost.
     */
    public Bus(int id, double speed, int capacity, double maintenanceCost) {
        super(id, speed, capacity, maintenanceCost);
    }

    /**
     * Loads passengers at a city stop based on the city's current demand.
     * Reduces the city's demand by the number of passengers picked up.
     * @param stop The stop to load passengers from.
     */
    @Override
    public void loadCargo(Stop stop) {
        City city = stop.getNearbyCity();
        if (city == null) return;
        if (passengerCount > 0) return;
        int available = Math.min(city.getPassengerDemand(), capacity);
        if (available <= 0) return;
        passengerCount = available;
        originCity = city;
        city.reducePassengerDemand(available);
        cargo = new Good(GoodType.PASSENGERS, passengerCount);
    }

    /**
     * Unloads passengers at a different city stop, earns demand-based income via city pricing.
     * @param stop The stop to unload passengers at.
     */
    @Override
    public void unloadCargo(Stop stop) {
        if (passengerCount <= 0) return;
        City city = stop.getNearbyCity();
        if (city == null || city == originCity) return;
        int delivered = passengerCount;
        passengerCount = 0;
        originCity = null;
        cargo = null;
        double income = city.acceptDelivery(new Good(GoodType.PASSENGERS, delivered), delivered);
        if (deliveryCallback != null) deliveryCallback.onDelivery(income);
    }

    /** @return The number of passengers currently on board. */
    public int getPassengerCount() { return passengerCount; }
}
