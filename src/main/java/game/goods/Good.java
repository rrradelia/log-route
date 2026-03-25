package game.goods;

/**
 * Represents a parcel of cargo consisting of a type and a quantity.
 */
public class Good {

    private final GoodType type;
    private int amount;

    /**
     * Constructs a Good with the specified type and amount.
     * @param type the category of this cargo
     * @param amount the quantity of units, must be non-negative
     */
    public Good(GoodType type, int amount) {
        if (amount < 0) throw new IllegalArgumentException("Amount cannot be negative");
        this.type   = type;
        this.amount = amount;
    }

    /** @return the GoodType */
    public GoodType getType() { return type; }

    /** @return the amount */
    public int getAmount() { return amount; }

    /**
     * Sets the quantity of this good.
     * @param amount the new amount, must be non-negative
     */
    public void setAmount(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Amount cannot be negative");
        this.amount = amount;
    }

    /**
     * Increases or decreases the amount by the given delta.
     * @param delta the value to add
     */
    public void add(int delta) { setAmount(this.amount + delta); }

    @Override
    public String toString() { return amount + "x " + type.displayName(); }
}
