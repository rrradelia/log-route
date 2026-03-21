package game.tile;

/**
 * Represents a city tile. Cities are 3x3 areas that generate passengers.
 * Not buildable.
 */
public class CityTile extends Tile {
    private final String cityName;

    /**
     * Constructs a city tile at the specified position.
     * @param x The x-coordinate on the map.
     * @param y The y-coordinate on the map.
     * @param cityName The name of the city.
     */
    public CityTile(int x, int y, String cityName) {
        super(x, y);
        this.cityName = cityName;
    }

    @Override public TileType getType() { return TileType.CITY; }
    @Override public boolean isBuildable() { return false; }

    /** @return The name of the city this tile belongs to. */
    public String getCityName() { return cityName; }
}
