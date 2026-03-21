package game.vehicle;

import game.tile.RoadTile;
import game.transport.Route;
import game.transport.Stop;
import game.goods.Good;
import game.util.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all vehicles (trucks and buses).
 * Vehicles have speed, capacity, maintenance cost, cargo, and route assignment.
 */
public abstract class Vehicle {
    protected int id;
    protected double speed;
    protected int capacity;
    protected double maintenanceCost;
    protected RoadTile currentTile;
    protected Route assignedRoute;
    protected Good cargo;
    protected Direction direction;
    protected List<RoadTile> currentPath = new ArrayList<>();
    protected int pathIndex = 0;
    protected Stop currentStop;
    protected Stop targetStop;
    protected boolean waiting = false;
    protected double renderX = -1;
    protected double renderY = -1;
    protected double interpolation = 1.0;
    protected boolean overtaking = false;
    private long moveAccMs = 0;

    protected double purchaseCost;

    /**
     * Constructs a vehicle with the given attributes.
     * @param id The unique vehicle ID.
     * @param speed The movement speed of the vehicle.
     * @param capacity The maximum cargo/passenger capacity.
     * @param maintenanceCost The periodic maintenance cost.
     */
    public Vehicle(int id, double speed, int capacity, double maintenanceCost) {
        this.id = id;
        this.speed = speed;
        this.capacity = capacity;
        this.maintenanceCost = maintenanceCost;
        this.direction = Direction.EAST;
    }

    /**
     * Moves the vehicle one step along its current path.
     * Respects tile occupancy limits, direction constraints, traffic lights,
     * and supports overtaking slower vehicles when the oncoming lane is clear.
     */
    public void move() {
        if (assignedRoute == null || currentTile == null) return;
        if (currentPath.isEmpty() || pathIndex >= currentPath.size()) {
            waiting = true;
            interpolation = 1.0;
            return;
        }
        waiting = false;
        overtaking = false;
        RoadTile nextTile = currentPath.get(pathIndex);
        if (nextTile != currentTile) {
            int dx = nextTile.getX() - currentTile.getX();
            int dy = nextTile.getY() - currentTile.getY();
            if (dx > 0) direction = Direction.EAST;
            else if (dx < 0) direction = Direction.WEST;
            else if (dy > 0) direction = Direction.SOUTH;
            else if (dy < 0) direction = Direction.NORTH;
            if (nextTile.hasTrafficLight() && trafficLightLookup != null) {
                game.traffic.TrafficLight tl = trafficLightLookup.apply(nextTile);
                if (tl != null && tl.getSignal(direction) == game.traffic.TrafficState.RED) {
                    waiting = true;
                    return;
                }
            }
            if (!nextTile.canAcceptVehicle(direction)) {
                if (canOvertake(nextTile)) {
                    overtaking = true;
                } else {
                    waiting = true;
                    return;
                }
            }
            renderX = currentTile.getX();
            renderY = currentTile.getY();
            interpolation = 0.0;
            currentTile.getVehiclesOnTile().remove(this);
            currentTile = nextTile;
            currentTile.getVehiclesOnTile().add(this);
        }
        pathIndex++;
    }

    /**
     * Advances the visual interpolation toward the current tile position.
     * Called by the render loop for smooth continuous movement.
     * @param fraction The interpolation step (0.0 to 1.0).
     */
    public void advanceInterpolation(double fraction) {
        interpolation = Math.min(1.0, interpolation + fraction);
    }

    /**
     * Checks if this vehicle can overtake a slower vehicle on the next tile
     * by temporarily using the oncoming lane.
     * @param blockedTile The tile that is currently occupied.
     * @return True if overtaking is possible.
     */
    private boolean canOvertake(RoadTile blockedTile) {
        Vehicle blocker = null;
        for (Vehicle v : blockedTile.getVehiclesOnTile()) {
            if (v.getDirection() == this.direction) { blocker = v; break; }
        }
        if (blocker == null || blocker.getSpeed() >= this.speed) return false;
        Direction opposite = direction.opposite();
        return blockedTile.canAcceptVehicle(opposite);
    }

    private java.util.function.Function<RoadTile, game.traffic.TrafficLight> trafficLightLookup;
    protected DeliveryCallback deliveryCallback;

    /**
     * Sets the function used to look up traffic lights at road tiles.
     * @param lookup function mapping a RoadTile to its TrafficLight, or null.
     */
    public void setTrafficLightLookup(java.util.function.Function<RoadTile, game.traffic.TrafficLight> lookup) {
        this.trafficLightLookup = lookup;
    }

    /**
     * Sets the callback invoked when a vehicle delivers cargo to a city.
     * @param callback the delivery callback
     */
    public void setDeliveryCallback(DeliveryCallback callback) {
        this.deliveryCallback = callback;
    }

    /**
     * Callback interface for delivery income processing.
     */
    @FunctionalInterface
    public interface DeliveryCallback {
        /**
         * Called when cargo or passengers are delivered for income.
         * @param income the pre-calculated income amount
         */
        void onDelivery(double income);
    }

    /**
     * Sets the path this vehicle should follow to reach the next stop.
     * Skips the first tile if it matches the current position to avoid wasting a tick.
     * @param path Ordered list of road tiles from current position to target.
     */
    public void setPath(List<RoadTile> path) {
        this.currentPath = path;
        this.pathIndex = 0;
        if (!path.isEmpty() && path.get(0) == currentTile) {
            this.pathIndex = 1;
        }
    }

    /**
     * Checks if the vehicle has reached the end of its current path.
     * @return True if path is complete.
     */
    public boolean hasReachedTarget() {
        return currentPath.isEmpty() || pathIndex >= currentPath.size();
    }

    /**
     * Loads cargo at the given stop based on capacity constraint.
     * @param stop The stop to load cargo from.
     */
    public void loadCargo(Stop stop) {}

    /**
     * Unloads cargo at the given stop.
     * @param stop The stop to unload cargo at.
     */
    public void unloadCargo(Stop stop) {}

    /**
     * Adds simulated time to this vehicle's movement accumulator.
     * @param ms milliseconds to add
     */
    public void addMoveAccumulator(long ms) { moveAccMs += ms; }

    /**
     * Drains the given amount from the movement accumulator.
     * @param ms milliseconds to drain
     */
    public void drainMoveAccumulator(long ms) { moveAccMs -= ms; }

    /**
     * Returns the current movement accumulator value.
     * @return accumulated milliseconds
     */
    public long getMoveAccumulator() { return moveAccMs; }

    /** @return The unique ID of this vehicle. */
    public int getId() { return id; }

    /**
     * Returns a descriptive display name for this vehicle.
     * @return Name like "SmallTruck #1 (Iron)" or "BigBus #3".
     */
    public String getName() {
        String type = getClass().getSimpleName();
        return type + " #" + id;
    }

    /** @return The purchase cost of this vehicle. */
    public double getPurchaseCost() { return purchaseCost; }

    /** @return The current speed of this vehicle. */
    public double getSpeed() { return speed; }

    /** @return The maximum capacity of this vehicle. */
    public int getCapacity() { return capacity; }

    /** @return The maintenance cost per cycle. */
    public double getMaintenanceCost() { return maintenanceCost; }

    /** @return The current tile this vehicle is on. */
    public RoadTile getCurrentTile() { return currentTile; }

    /**
     * Sets the current tile of this vehicle.
     * @param currentTile The road tile the vehicle is on.
     */
    public void setCurrentTile(RoadTile currentTile) { this.currentTile = currentTile; }

    /** @return The route this vehicle is assigned to. */
    public Route getAssignedRoute() { return assignedRoute; }

    /**
     * Sets the assigned route for this vehicle.
     * @param assignedRoute The route to assign.
     */
    public void setAssignedRoute(Route assignedRoute) { this.assignedRoute = assignedRoute; }

    /** @return The cargo this vehicle is carrying. */
    public Good getCargo() { return cargo; }

    /**
     * Sets the cargo for this vehicle.
     * @param cargo The cargo to carry.
     */
    public void setCargo(Good cargo) { this.cargo = cargo; }

    /** @return The current travel direction of this vehicle. */
    public Direction getDirection() { return direction; }

    /**
     * Sets the travel direction of this vehicle.
     * @param direction The new direction.
     */
    public void setDirection(Direction direction) { this.direction = direction; }

    /** @return The current target stop. */
    public Stop getTargetStop() { return targetStop; }

    /**
     * Sets the target stop for this vehicle.
     * @param stop The target stop.
     */
    public void setTargetStop(Stop stop) { this.targetStop = stop; }

    /** @return The current stop this vehicle is at. */
    public Stop getCurrentStop() { return currentStop; }

    /**
     * Sets the current stop.
     * @param stop The stop.
     */
    public void setCurrentStop(Stop stop) { this.currentStop = stop; }

    /** @return True if the vehicle is waiting at a stop. */
    public boolean isWaiting() { return waiting; }

    /** @return The visual x-coordinate for smooth rendering. */
    public double getRenderX() {
        if (currentTile == null) return -1;
        if (renderX < 0 || interpolation >= 1.0) return currentTile.getX();
        return renderX + (currentTile.getX() - renderX) * interpolation;
    }

    /** @return The visual y-coordinate for smooth rendering. */
    public double getRenderY() {
        if (currentTile == null) return -1;
        if (renderY < 0 || interpolation >= 1.0) return currentTile.getY();
        return renderY + (currentTile.getY() - renderY) * interpolation;
    }

    /** @return True if the vehicle is currently overtaking another vehicle. */
    public boolean isOvertaking() { return overtaking; }
}
