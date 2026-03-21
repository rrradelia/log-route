package game.tile;

/**
 * Represents an empty grass tile. Default terrain, buildable.
 */
public class GrassTile extends Tile {

    /**
     * Constructs a grass tile at the specified position.
     * @param x The x-coordinate on the map.
     * @param y The y-coordinate on the map.
     */
    public GrassTile(int x, int y) { super(x, y); }

    @Override public TileType getType() { return TileType.GRASS; }
    @Override public boolean isBuildable() { return true; }
}
