package game.traffic;

import game.tile.RoadTile;
import game.util.Direction;

/**
 * Manages traffic signal timing at a road intersection.
 * Alternates between north-south and east-west green phases.
 */
public class TrafficLight {
    private RoadTile location;
    private int nsGreenTime;
    private int ewGreenTime;
    private TrafficState currentState;
    private int timeInCurrentState;
    private boolean isNsAxisGreen;

    /**
     * Constructs a traffic light at the given road tile.
     * @param location The road tile where this light is placed.
     * @param nsGreenTime Duration of the north-south green phase.
     * @param ewGreenTime Duration of the east-west green phase.
     */
    public TrafficLight(RoadTile location, int nsGreenTime, int ewGreenTime) {
        this.location = location;
        this.nsGreenTime = nsGreenTime;
        this.ewGreenTime = ewGreenTime;
        this.currentState = TrafficState.GREEN;
        this.isNsAxisGreen = true;
        this.timeInCurrentState = 0;
    }

    /**
     * Updates the traffic light timer and switches states if the interval has passed.
     * @param deltaTime The time elapsed since the last update.
     */
    public void update(int deltaTime) {
        timeInCurrentState += deltaTime;
        int currentPhaseDuration = isNsAxisGreen ? nsGreenTime : ewGreenTime;
        if (timeInCurrentState >= currentPhaseDuration) {
            isNsAxisGreen = !isNsAxisGreen;
            timeInCurrentState = 0;
            currentState = isNsAxisGreen ? TrafficState.GREEN : TrafficState.RED;
        }
    }

    /**
     * Returns the signal state for a vehicle approaching from the given direction.
     * @param direction The direction the vehicle is traveling.
     * @return RED or GREEN based on the current active phase.
     */
    public TrafficState getSignal(Direction direction) {
        if (direction == Direction.NORTH || direction == Direction.SOUTH) {
            return isNsAxisGreen ? TrafficState.GREEN : TrafficState.RED;
        } else if (direction == Direction.EAST || direction == Direction.WEST) {
            return isNsAxisGreen ? TrafficState.RED : TrafficState.GREEN;
        }
        return TrafficState.RED;
    }

    /** @return The road tile where this light is placed. */
    public RoadTile getLocation() { return location; }

    /** @return The north-south green phase duration. */
    public int getNsGreenTime() { return nsGreenTime; }

    /**
     * Sets the north-south green phase duration.
     * @param nsGreenTime The new duration.
     */
    public void setNsGreenTime(int nsGreenTime) { this.nsGreenTime = nsGreenTime; }

    /** @return The east-west green phase duration. */
    public int getEwGreenTime() { return ewGreenTime; }

    /**
     * Sets the east-west green phase duration.
     * @param ewGreenTime The new duration.
     */
    public void setEwGreenTime(int ewGreenTime) { this.ewGreenTime = ewGreenTime; }

    /** @return The current baseline traffic state. */
    public TrafficState getCurrentState() { return currentState; }
}
