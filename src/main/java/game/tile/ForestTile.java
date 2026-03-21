package game.tile;

/**
 * Represents a forest tile containing trees.
 * Buildable but requires clearing costs. Trees grow and spread over time.
 */
public class ForestTile extends Tile {
    public static final int MAX_TREES = 4;
    private int treeCount;

    /**
     * Constructs a forest tile with the specified number of trees.
     * @param x The x-coordinate on the map.
     * @param y The y-coordinate on the map.
     * @param treeCount Initial number of trees (clamped to 1-4).
     */
    public ForestTile(int x, int y, int treeCount) {
        super(x, y);
        this.treeCount = Math.max(1, Math.min(4, treeCount));
    }

    @Override public TileType getType() { return TileType.FOREST; }
    @Override public boolean isBuildable() { return true; }

    /** @return The current number of trees (1-4). */
    public int getTreeCount() { return treeCount; }

    /**
     * Grows one additional tree on this tile, up to the maximum of 4.
     */
    public void growTrees() {
        if (treeCount < 4) treeCount++;
    }

    /**
     * Checks whether this forest can spread to adjacent grass tiles.
     * @return True if tree count is 3 or more.
     */
    public boolean canSpread() { return treeCount >= 3; }

    /**
     * Calculates the cost to clear this forest for construction ($200 per tree).
     * @return The clearing cost in dollars.
     */
    public int getClearingCost() { return 200 * treeCount; }
}
