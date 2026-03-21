package game.tile;

/**
 * Represents a water tile (river or lake).
 * Not buildable directly; players must build bridges to cross.
 */
public class WaterTile extends Tile {

    /**
     * Constructs a water tile at the specified position.
     * @param x The x-coordinate on the map.
     * @param y The y-coordinate on the map.
     */
    public WaterTile(int x, int y) { super(x, y); }

    @Override public TileType getType() { return TileType.WATER; }
    @Override public boolean isBuildable() { return false; }
}
