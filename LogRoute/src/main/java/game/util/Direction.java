package game.util;

/**
 * Cardinal directions for vehicle movement on the map grid.
 */
public enum Direction {
    NORTH(0, -1), SOUTH(0, 1), EAST(1, 0), WEST(-1, 0);

    private final int dx, dy;

    Direction(int dx, int dy) { this.dx = dx; this.dy = dy; }

    /** @return The x-offset for this direction (-1, 0, or 1). */
    public int getDx() { return dx; }

    /** @return The y-offset for this direction (-1, 0, or 1). */
    public int getDy() { return dy; }

    /**
     * Returns the opposite direction.
     * @return The opposite Direction.
     */
    public Direction opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
        };
    }
}
