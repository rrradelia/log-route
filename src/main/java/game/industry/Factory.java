package game.industry;

import game.economy.Storage;
import game.goods.GoodType;

/**
 * A processing facility that consumes iron and wood to produce paper.
 */
public class Factory extends Industry {

    private static final int INPUT_PER_TICK  = 5;
    private static final int OUTPUT_PER_TICK = 8;

    private final Storage ironInput;
    private final Storage woodInput;
    private final Storage paperOutput;
    private boolean consumedThisTick = false;

    /**
     * Constructs a Factory at the given map position.
     * @param name the display name of this factory
     * @param tileX the x-coordinate of the top-left tile
     * @param tileY the y-coordinate of the top-left tile
     */
    public Factory(String name, int tileX, int tileY) {
        super(name, tileX, tileY, 2, 2);
        ironInput   = new Storage(GoodType.IRON,  100);
        woodInput   = new Storage(GoodType.WOOD,  100);
        paperOutput = new Storage(GoodType.PAPER, 100);
        storages.add(ironInput);
        storages.add(woodInput);
        storages.add(paperOutput);
    }

    @Override
    public void tick(long deltaMs) {
        consumedThisTick = false;
        super.tick(deltaMs);
    }

    @Override
    protected void consume() {
        if (ironInput.getCurrentAmount() >= INPUT_PER_TICK
                && woodInput.getCurrentAmount() >= INPUT_PER_TICK) {
            ironInput.remove(INPUT_PER_TICK);
            woodInput.remove(INPUT_PER_TICK);
            consumedThisTick = true;
        }
    }

    @Override
    protected void produce() {
        if (consumedThisTick && !paperOutput.isFull()) paperOutput.add(OUTPUT_PER_TICK);
    }

    @Override
    public boolean isOutputGood(GoodType type) { return type == GoodType.PAPER; }

    /** @return iron Storage */
    public Storage getIronInput() { return ironInput; }

    /** @return wood Storage */
    public Storage getWoodInput() { return woodInput; }

    /** @return paper Storage */
    public Storage getPaperOutput() { return paperOutput; }
}
