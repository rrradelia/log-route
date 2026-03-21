package game.map;

import game.tile.Tile;
import game.util.BridgeType;
import java.util.Collections;
import java.util.List;

/**
 * Represents a bridge spanning water tiles between two land endpoints.
 * Bridges have a type that determines cost, max length, and speed limit.
 */
public class Bridge {
    private final BridgeType type;
    private final List<Tile> tiles;

    /**
     * Constructs a bridge with the given type and tile span.
     * @param type The bridge type (WOODEN, STONE, or STEEL).
     * @param tiles The list of tiles the bridge covers (including endpoints).
     * @throws IllegalArgumentException If the span is less than 2 tiles.
     */
    public Bridge(BridgeType type, List<Tile> tiles) {
        if (tiles.size() < 2) throw new IllegalArgumentException("Bridge must span at least 2 tiles");
        this.type = type;
        this.tiles = Collections.unmodifiableList(tiles);
    }

    /** @return The bridge type. */
    public BridgeType getType() { return type; }

    /** @return The construction cost based on bridge type. */
    public int getCost() { return type.getCost(); }

    /** @return The speed limit multiplier for vehicles crossing this bridge. */
    public double getSpeedLimit() { return type.getSpeedLimit(); }

    /** @return The total length of the bridge in tiles. */
    public int getLength() { return tiles.size(); }

    /** @return An unmodifiable list of tiles this bridge spans. */
    public List<Tile> getTiles() { return tiles; }
}
