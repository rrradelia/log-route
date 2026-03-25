package game.economy;

import game.goods.GoodType;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * Manages the player's financial state throughout the simulation.
 * Handles all income and expenditure, calculates delivery rewards,
 * and determines whether the player has gone bankrupt.
 */
public class Economy {

    public static final double STARTING_CAPITAL = 50_000.0;

    private final DoubleProperty capital = new SimpleDoubleProperty(STARTING_CAPITAL);
    private double totalEarned       = 0.0;
    private double totalSpent        = 0.0;
    private double maintenanceSpent  = 0.0;
    private double constructionSpent = 0.0;
    private double vehicleSpent      = 0.0;
    private double deliveryEarned    = 0.0;
    private int    deliveryCount     = 0;

    /**
     * Resets all financial tracking to initial state.
     */
    public void reset() {
        capital.set(STARTING_CAPITAL);
        totalEarned = 0;
        totalSpent = 0;
        maintenanceSpent = 0;
        constructionSpent = 0;
        vehicleSpent = 0;
        deliveryEarned = 0;
        deliveryCount = 0;
    }

    /**
     * Refunds an amount to the player's capital without counting as income.
     * Used for cancelled purchases and sell-backs.
     * @param amount the refund amount, must be positive
     */
    public void refund(double amount) {
        if (amount < 0) throw new IllegalArgumentException("refund() amount must be positive");
        capital.set(capital.get() + amount);
    }

    /**
     * Adds the given amount to the player's capital.
     * @param amount the income to add, must be positive
     */
    public void earn(double amount) {
        if (amount < 0) throw new IllegalArgumentException("earn() amount must be positive");
        capital.set(capital.get() + amount);
        totalEarned += amount;
    }

    /**
     * Deducts the given amount from the player's capital.
     * @param amount the cost to deduct, must be positive
     */
    public void spend(double amount) {
        if (amount < 0) throw new IllegalArgumentException("spend() amount must be positive");
        capital.set(capital.get() - amount);
        totalSpent += amount;
    }

    /**
     * Returns true if the player's capital has fallen below zero.
     * @return true if capital is negative
     */
    public boolean isBankrupt() { return capital.get() < 0; }

    /**
     * Returns the base price per unit for a given good type.
     * Single source of truth for all delivery pricing.
     * @param goodType the type of good
     * @return the base price per unit
     */
    public static double getBasePrice(GoodType goodType) {
        return switch (goodType) {
            case WOOD       -> 8.0;
            case IRON       -> 15.0;
            case PAPER      -> 20.0;
            case PASSENGERS -> 10.0;
        };
    }

    /**
     * Calculates the income earned from a completed delivery.
     * @param goodType the type of good delivered
     * @param amount the number of units delivered
     * @param demandMultiplier demand-based multiplier from the receiving city
     * @return the calculated income
     */
    public static double calculateDeliveryIncome(GoodType goodType, int amount, double demandMultiplier) {
        return amount * getBasePrice(goodType) * demandMultiplier;
    }

    /**
     * Charges the player for constructing one road tile.
     * @param isForestTile true if the tile contains trees
     * @return true if the player had sufficient funds and was charged
     */
    public boolean chargeRoadConstruction(boolean isForestTile) {
        double cost = isForestTile ? 300.0 : 100.0;
        if (capital.get() >= cost) { spend(cost); return true; }
        return false;
    }

    /**
     * Charges the player for purchasing a vehicle.
     * @param vehicleCost the purchase price of the vehicle
     * @return true if the player had sufficient funds and was charged
     */
    public boolean chargeVehiclePurchase(double vehicleCost) {
        if (capital.get() >= vehicleCost) { spend(vehicleCost); vehicleSpent += vehicleCost; return true; }
        return false;
    }

    /**
     * Deducts recurring maintenance costs for the entire vehicle fleet.
     * @param totalFleetMaintenance the total maintenance cost across all vehicles
     */
    public void chargeMaintenanceCosts(double totalFleetMaintenance) {
        spend(totalFleetMaintenance);
        maintenanceSpent += totalFleetMaintenance;
    }

    /**
     * Records a delivery earning for tracking.
     * @param amount the delivery income
     */
    public void recordDelivery(double amount) {
        deliveryEarned += amount;
        deliveryCount++;
    }

    /** @return current capital value */
    public double getCapital() { return capital.get(); }

    /** @return total earned */
    public double getTotalEarned() { return totalEarned; }

    /** @return total spent */
    public double getTotalSpent() { return totalSpent; }

    /** @return the DoubleProperty representing current capital */
    public DoubleProperty capitalProperty() { return capital; }

    /** @return total maintenance spent */
    public double getMaintenanceSpent() { return maintenanceSpent; }

    /** @return total construction spent */
    public double getConstructionSpent() { return constructionSpent; }

    /** @return total vehicle purchase spent */
    public double getVehicleSpent() { return vehicleSpent; }

    /** @return total delivery income */
    public double getDeliveryEarned() { return deliveryEarned; }

    /** @return number of completed deliveries */
    public int getDeliveryCount() { return deliveryCount; }

    /**
     * Records a construction expense for tracking.
     * @param amount the construction cost
     */
    public void recordConstruction(double amount) { constructionSpent += amount; }

    /**
     * Records a vehicle purchase expense for tracking.
     * @param amount the vehicle purchase cost
     */
    public void recordVehiclePurchase(double amount) { vehicleSpent += amount; }
}
