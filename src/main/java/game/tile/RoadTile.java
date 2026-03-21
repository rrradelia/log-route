package game.tile;

import game.util.Direction;
import game.vehicle.Vehicle;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a road tile that vehicles can travel on.
 * Can optionally have traffic lights and tracks vehicles currently on it.
 */
public class RoadTile extends Tile {
    private boolean hasTrafficLight;
    private final List<Vehicle> vehiclesOnTile = new ArrayList<>();

    /**
     * Constructs a road tile at the specified position.
     * @param x The x-coordinate on the map.
     * @param y The y-coordinate on the map.
     */
    public RoadTile(int x, int y) { super(x, y); }

    @Override public TileType getType() { return TileType.ROAD; }
    @Override public boolean isBuildable() { return false; }

    /** @return True if a traffic light is installed on this tile. */
    public boolean hasTrafficLight() { return hasTrafficLight; }

    /**
     * Sets or removes the traffic light on this road tile.
     * @param v True to install, false to remove.
     */
    public void setTrafficLight(boolean v) { hasTrafficLight = v; }

    /** @return The list of vehicles currently on this tile. */
    public List<Vehicle> getVehiclesOnTile() { return vehiclesOnTile; }

    /**
     * Checks whether a vehicle traveling in the given direction can enter this tile.
     * Only one vehicle per direction is allowed at a time.
     * @param dir The direction the vehicle is traveling.
     * @return True if no other vehicle with the same direction is on this tile.
     */
    public boolean canAcceptVehicle(Direction dir) {
        return vehiclesOnTile.stream().noneMatch(v -> v.getDirection() == dir);
    }
}
