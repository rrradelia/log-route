package game.industry;

import game.economy.Storage;
import game.goods.Good;
import game.goods.GoodType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base class for all industrial facilities on the map.
 */
public abstract class Industry {

    protected final String name;
    protected final int    tileX;
    protected final int    tileY;
    protected final int    widthTiles;
    protected final int    heightTiles;

    protected int  productionIntervalMs = 5_000;
    private   long accumulatedMs        = 0;

    protected final List<Storage> storages = new ArrayList<>();

    /**
     * Constructs an Industry at the given map position and tile size.
     * @param name the display name of this facility
     * @param tileX the x-coordinate of the top-left tile
     * @param tileY the y-coordinate of the top-left tile
     * @param widthTiles the width in tiles
     * @param heightTiles the height in tiles
     */
    protected Industry(String name, int tileX, int tileY, int widthTiles, int heightTiles) {
        this.name        = name;
        this.tileX       = tileX;
        this.tileY       = tileY;
        this.widthTiles  = widthTiles;
        this.heightTiles = heightTiles;
    }

    /**
     * Advances the industry by the given simulated time delta.
     * @param deltaMs simulated milliseconds elapsed since the last frame
     */
    public void tick(long deltaMs) {
        accumulatedMs += deltaMs;
        while (accumulatedMs >= productionIntervalMs) {
            accumulatedMs -= productionIntervalMs;
            consume();
            produce();
        }
    }

    /**
     * Produces output goods into the appropriate storage.
     */
    protected abstract void produce();

    /**
     * Consumes input goods from storage in order to enable production.
     */
    protected abstract void consume();

    /**
     * Returns the Storage slot for the given good type, or null if none exists.
     * @param type the good type to look up
     * @return the matching Storage, or null
     */
    public Storage getStorage(GoodType type) {
        return storages.stream()
                .filter(s -> s.getGoodType() == type)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns a list of goods currently available for pickup at this industry.
     * @return list of available Good instances
     */
    public List<Good> getAvailableGoods() {
        List<Good> available = new ArrayList<>();
        for (Storage s : storages) {
            if (!s.isEmpty()) available.add(new Good(s.getGoodType(), s.getCurrentAmount()));
        }
        return available;
    }

    /**
     * Returns whether the given good type is an output product of this industry.
     * Subclasses override to specify their output types.
     * @param type the good type to check
     * @return true if this industry produces this good type
     */
    public abstract boolean isOutputGood(GoodType type);

    /** @return name string */
    public String getName() { return name; }

    /** @return tileX */
    public int getTileX() { return tileX; }

    /** @return tileY */
    public int getTileY() { return tileY; }

    /** @return width in tiles */
    public int getWidthTiles() { return widthTiles; }

    /** @return height in tiles */
    public int getHeightTiles() { return heightTiles; }

    /** @return unmodifiable list of Storage instances */
    public List<Storage> getStorages() { return Collections.unmodifiableList(storages); }

    /**
     * Sets the production interval in simulated milliseconds.
     * @param ms interval duration
     */
    public void setProductionIntervalMs(int ms) { this.productionIntervalMs = ms; }
}
