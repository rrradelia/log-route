package game.transport;

import game.vehicle.Vehicle;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a circular transport route connecting multiple stops.
 * Vehicles are assigned to routes and travel between stops in order.
 */
public class Route {
    private static int nextId = 1;
    private final int id;
    private String name;
    private final List<Stop> stops = new ArrayList<>();
    private final List<Vehicle> assignedVehicles = new ArrayList<>();

    /**
     * Constructs a new route with an auto-incremented ID.
     */
    public Route() { this.id = nextId++; }

    /**
     * Resets the static ID counter (used on game restart).
     */
    public static void resetIdCounter() { nextId = 1; }

    /**
     * Adds a stop to this route and registers this route with the stop.
     * @param stop The stop to add.
     */
    public void addStop(Stop stop) {
        if (!stops.contains(stop)) {
            stops.add(stop);
            stop.addRoute(this);
        }
    }

    /**
     * Returns the next stop after the given stop in circular order.
     * @param current The current stop.
     * @return The next stop, or null if the route has no stops.
     */
    public Stop getNextStop(Stop current) {
        if (stops.isEmpty()) return null;
        int idx = stops.indexOf(current);
        if (idx == -1 || idx == stops.size() - 1) return stops.get(0);
        return stops.get(idx + 1);
    }

    /**
     * Assigns a vehicle to this route and sets the vehicle's assigned route.
     * @param vehicle The vehicle to assign.
     */
    public void assignVehicle(Vehicle vehicle) {
        if (!assignedVehicles.contains(vehicle)) {
            assignedVehicles.add(vehicle);
            vehicle.setAssignedRoute(this);
        }
    }

    /** @return The unique ID of this route. */
    public int getId() { return id; }

    /** @return The display name of this route. */
    public String getName() {
        if (name != null) return name;
        return buildAutoName();
    }

    /**
     * Sets a custom name for this route.
     * @param name The route name.
     */
    public void setName(String name) { this.name = name; }

    /**
     * Builds an auto-generated name from stop locations.
     * @return A descriptive name like "Millville → Iron Mine".
     */
    private String buildAutoName() {
        if (stops.isEmpty()) return "Route #" + id;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stops.size(); i++) {
            if (i > 0) sb.append(" → ");
            sb.append(stopShortName(stops.get(i)));
        }
        return sb.toString();
    }

    /**
     * Returns a short name for a stop based on its nearby entity.
     * @param stop The stop.
     * @return Short name string.
     */
    private String stopShortName(Stop stop) {
        if (stop.getNearbyCity() != null) return stop.getNearbyCity().getName();
        if (stop.getNearbyIndustry() != null) return stop.getNearbyIndustry().getName();
        return "Stop #" + stop.getId();
    }

    /** @return The ordered list of stops on this route. */
    public List<Stop> getStops() { return stops; }

    /** @return The list of vehicles assigned to this route. */
    public List<Vehicle> getAssignedVehicles() { return assignedVehicles; }

    @Override
    public String toString() {
        return "Route #" + id + ": " + getName()
                + " (" + stops.size() + " stops, " + assignedVehicles.size() + " vehicles)";
    }
}
