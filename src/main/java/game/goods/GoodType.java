package game.goods;

/**
 * Represents the types of cargo that can be transported in the game.
 */
public enum GoodType {
    WOOD, IRON, PAPER, PASSENGERS;

    /**
     * Returns the human-readable name of this good type.
     * @return display name string
     */
    public String displayName() {
        return switch (this) {
            case WOOD       -> "Wood";
            case IRON       -> "Iron";
            case PAPER      -> "Paper";
            case PASSENGERS -> "Passengers";
        };
    }
}
