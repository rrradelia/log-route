package game.tile;

/**
 * Abstract base class for all map tiles in the game.
 * Each tile occupies a single cell on the 64x64 game grid.
 */
public abstract class Tile {
    public enum TileType { GRASS, ROAD, FOREST, WATER, CITY, INDUSTRY }

    protected final int x, y;

    /**
     * Constructs a tile at the specified grid position.
     * @param x The x-coordinate on the map.
     * @param y The y-coordinate on the map.
     */
    protected Tile(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** @return The x-coordinate of this tile. */
    public int getX() { return x; }

    /** @return The y-coordinate of this tile. */
    public int getY() { return y; }

    /** @return The TileType enum value for this tile. */
    public abstract TileType getType();

    /** @return True if a road or structure can be built on this tile. */
    public abstract boolean isBuildable();
}
