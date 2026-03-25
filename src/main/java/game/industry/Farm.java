package game.industry;

import game.economy.Storage;
import game.goods.GoodType;

/**
 * A raw material facility that produces wood.
 */
public class Farm extends Industry {

    private static final int DEFAULT_CAPACITY    = 100;
    private static final int PRODUCTION_PER_TICK = 8;
    private final Storage woodStorage;

    /**
     * Constructs a Farm at the given map position.
     * @param name the display name of this farm
     * @param tileX the x-coordinate of the top-left tile
     * @param tileY the y-coordinate of the top-left tile
     */
    public Farm(String name, int tileX, int tileY) {
        super(name, tileX, tileY, 2, 2);
        woodStorage = new Storage(GoodType.WOOD, DEFAULT_CAPACITY);
        storages.add(woodStorage);
    }

    @Override
    protected void produce() {
        if (!woodStorage.isFull()) woodStorage.add(PRODUCTION_PER_TICK);
    }

    @Override
    protected void consume() {}

    @Override
    public boolean isOutputGood(GoodType type) { return type == GoodType.WOOD; }

    /** @return the wood Storage */
    public Storage getWoodStorage() { return woodStorage; }
}
