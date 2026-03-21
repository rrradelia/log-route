package game.industry;

import game.economy.Storage;
import game.goods.GoodType;

/**
 * A raw material facility that produces iron ore.
 */
public class Mine extends Industry {

    private static final int DEFAULT_CAPACITY    = 100;
    private static final int PRODUCTION_PER_TICK = 10;
    private final Storage ironStorage;

    /**
     * Constructs a Mine at the given map position.
     * @param name the display name of this mine
     * @param tileX the x-coordinate of the top-left tile
     * @param tileY the y-coordinate of the top-left tile
     */
    public Mine(String name, int tileX, int tileY) {
        super(name, tileX, tileY, 2, 2);
        ironStorage = new Storage(GoodType.IRON, DEFAULT_CAPACITY);
        storages.add(ironStorage);
    }

    @Override
    protected void produce() {
        if (!ironStorage.isFull()) ironStorage.add(PRODUCTION_PER_TICK);
    }

    @Override
    protected void consume() {}

    @Override
    public boolean isOutputGood(GoodType type) { return type == GoodType.IRON; }

    /** @return the iron Storage */
    public Storage getIronStorage() { return ironStorage; }
}
