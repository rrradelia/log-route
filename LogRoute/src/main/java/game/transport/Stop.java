package game.transport;

import game.city.City;
import game.industry.Industry;
import game.tile.RoadTile;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a transport stop placed on a road tile adjacent to a city or industry.
 * Vehicles pick up and drop off passengers/goods at stops.
 * Tracks which city and/or industry this stop serves for cargo operations.
 */
public class Stop {
    private static int nextId = 1;
    private final int id;
    private RoadTile tile;
    private final List<Route> routes = new ArrayList<>();
    private City nearbyCity;
    private Industry nearbyIndustry;

    /**
     * Constructs a stop on the given road tile with an auto-incremented ID.
     * @param tile The road tile this stop is placed on.
     */
    public Stop(RoadTile tile) {
        this.id = nextId++;
        this.tile = tile;
    }

    /**
     * Resets the static ID counter (used on game restart).
     */
    public static void resetIdCounter() { nextId = 1; }

    /**
     * Assigns a route to this stop so it can serve multiple routes.
     * @param route The route to add.
     */
    public void addRoute(Route route) {
        if (!routes.contains(route)) routes.add(route);
    }

    /** @return The unique ID of this stop. */
    public int getId() { return id; }

    /** @return The road tile this stop is placed on. */
    public RoadTile getTile() { return tile; }

    /**
     * Sets the road tile for this stop.
     * @param tile The new road tile.
     */
    public void setTile(RoadTile tile) { this.tile = tile; }

    /** @return The x-coordinate of this stop on the map. */
    public int getX() { return tile.getX(); }

    /** @return The y-coordinate of this stop on the map. */
    public int getY() { return tile.getY(); }

    /** @return The list of routes that include this stop. */
    public List<Route> getRoutes() { return routes; }

    /** @return The city this stop serves, or null. */
    public City getNearbyCity() { return nearbyCity; }

    /**
     * Sets the city this stop serves.
     * @param city the adjacent city
     */
    public void setNearbyCity(City city) { this.nearbyCity = city; }

    /** @return The industry this stop serves, or null. */
    public Industry getNearbyIndustry() { return nearbyIndustry; }

    /**
     * Sets the industry this stop serves.
     * @param industry the adjacent industry
     */
    public void setNearbyIndustry(Industry industry) { this.nearbyIndustry = industry; }

    @Override
    public String toString() {
        String label = "Stop #" + id + " (" + tile.getX() + "," + tile.getY() + ")";
        if (nearbyCity != null) label += " [" + nearbyCity.getName() + "]";
        if (nearbyIndustry != null) label += " [" + nearbyIndustry.getName() + "]";
        return label;
    }
}
