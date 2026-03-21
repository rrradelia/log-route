package game.util;

/**
 * Represents the available simulation speed settings.
 * Each constant holds a display label and a time multiplier used
 * by the game loop to scale simulated time relative to real time.
 */
public enum SimSpeed {
    PAUSED       ("⏸",  0),
    NORMAL       ("1x", 1),
    FAST_2X      ("2x", 2),
    VERY_FAST_4X ("4x", 4);

    private final String label;
    private final int    multiplier;

    SimSpeed(String label, int multiplier) {
        this.label      = label;
        this.multiplier = multiplier;
    }

    /**
     * Returns the display label shown on the HUD speed button.
     * @return label string
     */
    public String getLabel() { return label; }

    /**
     * Returns the time multiplier for this speed setting.
     * @return integer multiplier
     */
    public int getMultiplier() { return multiplier; }

    /**
     * Returns the next speed in the cycle: PAUSED -> NORMAL -> 2x -> 4x -> PAUSED.
     * @return the next SimSpeed
     */
    public SimSpeed next() {
        SimSpeed[] values = SimSpeed.values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
