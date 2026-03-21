package game.controller;

/**
 * Interface for providing the total vehicle fleet maintenance cost.
 */
public interface MaintenanceProvider {

    /**
     * Returns the total maintenance cost for the entire vehicle fleet.
     * @return total maintenance cost
     */
    double getTotalMaintenanceCost();
}
