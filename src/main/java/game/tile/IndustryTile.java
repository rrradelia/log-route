package game.tile;

/**
 * Represents an industry tile. Industries are 2x2 areas that produce/consume goods.
 * Not buildable.
 */
public class IndustryTile extends Tile {
    private final String industryName;

    /**
     * Constructs an industry tile at the specified position.
     * @param x The x-coordinate on the map.
     * @param y The y-coordinate on the map.
     * @param industryName The industry name (e.g., "Iron Mine", "Paper Mill").
     */
    public IndustryTile(int x, int y, String industryName) {
        super(x, y);
        this.industryName = industryName;
    }

    @Override public TileType getType() { return TileType.INDUSTRY; }
    @Override public boolean isBuildable() { return false; }

    /** @return The name of the industry this tile belongs to. */
    public String getIndustryName() { return industryName; }
}
