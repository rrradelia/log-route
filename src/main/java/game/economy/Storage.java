package game.economy;

import game.goods.GoodType;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Represents a warehouse slot attached to an industry or city.
 * Tracks how many units of a specific good are currently stored.
 */
public class Storage {

    private final GoodType goodType;
    private final int capacity;
    private final IntegerProperty currentAmount = new SimpleIntegerProperty(0);

    /**
     * Constructs a Storage for the given good type with the specified capacity.
     * @param goodType the type of good this storage holds
     * @param capacity the maximum number of units that can be stored
     */
    public Storage(GoodType goodType, int capacity) {
        this.goodType = goodType;
        this.capacity = capacity;
    }

    /** @return true if current amount equals capacity */
    public boolean isFull() { return currentAmount.get() >= capacity; }

    /** @return true if current amount is zero */
    public boolean isEmpty() { return currentAmount.get() == 0; }

    /**
     * Adds the given amount of goods to storage, capped at capacity.
     * @param amount the number of units to add
     * @return the number of units actually stored
     */
    public int add(int amount) {
        int space = capacity - currentAmount.get();
        int toAdd = Math.min(amount, space);
        currentAmount.set(currentAmount.get() + toAdd);
        return toAdd;
    }

    /**
     * Removes the given amount of goods from storage.
     * @param amount the number of units to remove
     * @return the number of units actually removed
     */
    public int remove(int amount) {
        int toRemove = Math.min(amount, currentAmount.get());
        currentAmount.set(currentAmount.get() - toRemove);
        return toRemove;
    }

    /** @return the GoodType */
    public GoodType getGoodType() { return goodType; }

    /** @return capacity in units */
    public int getCapacity() { return capacity; }

    /** @return current amount */
    public int getCurrentAmount() { return currentAmount.get(); }

    /** @return the IntegerProperty for UI binding */
    public IntegerProperty currentAmountProperty() { return currentAmount; }

    @Override
    public String toString() {
        return goodType.displayName() + " " + currentAmount.get() + "/" + capacity;
    }
}
