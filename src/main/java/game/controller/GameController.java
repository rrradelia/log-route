package game.controller;

import game.city.City;
import game.economy.Economy;
import game.industry.Industry;
import game.map.GameMap;
import game.tile.RoadTile;
import game.tile.Tile;
import game.traffic.TrafficLight;
import game.transport.Route;
import game.transport.Stop;
import game.util.SimSpeed;
import game.vehicle.Vehicle;

import javafx.animation.AnimationTimer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central controller that drives the simulation loop and coordinates all game systems.
 * Manages the economy, industries, cities, vehicle fleet, traffic lights, forest growth,
 * day/night cycle, maintenance charges, and bankruptcy detection.
 */
public class GameController implements MaintenanceProvider {

    private final Economy economy;
    private GameMap map;

    private final ObjectProperty<SimSpeed> simSpeed =
            new SimpleObjectProperty<>(SimSpeed.NORMAL);
    private final StringProperty simSpeedLabel = new SimpleStringProperty("1x");
    private final StringProperty dayLabel      = new SimpleStringProperty("Day 1");

    private final List<Industry>     industries    = new ArrayList<>();
    private final List<City>         cities        = new ArrayList<>();
    private final List<Vehicle>      vehicles      = new ArrayList<>();
    private final List<Route>        routes        = new ArrayList<>();
    private final List<TrafficLight> trafficLights = new ArrayList<>();

    private long lastNanoTime     = -1;
    private long simulatedDayMs   = 0;
    private int  dayCount         = 1;
    private long maintenanceAccMs = 0;
    private long forestAccMs      = 0;
    private long vehicleMoveAccMs = 0;
    private boolean gameOver      = false;
    private boolean gameStarted   = false;

    private static final long MS_PER_DAY       = 60_000L;
    private static final long MAINT_INTERVAL   = 30_000L;
    private static final long FOREST_INTERVAL  = 120_000L;
    private static final long BASE_MOVE_INTERVAL = 300L;
    private static final long CONGEST_INTERVAL = 10_000L;
    private static final int  CONGESTION_THRESHOLD = 3;

    private Runnable onBankruptCallback;
    private Runnable onRestartCallback;
    private long congestionAccMs = 0;

    private final AnimationTimer gameLoop;

    /**
     * Constructs a new GameController and initialises the economy and game loop.
     */
    public GameController() {
        this.economy  = new Economy();
        this.gameLoop = buildGameLoop();
    }

    /**
     * Sets the game map reference so the controller can update forests.
     * @param map the game map
     */
    public void setMap(GameMap map) { this.map = map; }

    /**
     * Creates the JavaFX AnimationTimer that drives the simulation.
     * @return the configured AnimationTimer
     */
    private AnimationTimer buildGameLoop() {
        return new AnimationTimer() {
            @Override
            public void handle(long nowNano) {
                if (lastNanoTime < 0) { lastNanoTime = nowNano; return; }
                long realDeltaMs = (nowNano - lastNanoTime) / 1_000_000L;
                lastNanoTime = nowNano;
                long simDeltaMs = realDeltaMs * simSpeed.get().getMultiplier();
                if (simDeltaMs > 0 && !gameOver) update(simDeltaMs);
            }
        };
    }

    /**
     * Advances all simulation systems by the given number of simulated milliseconds.
     * @param simDeltaMs the number of simulated milliseconds to advance
     */
    private void update(long simDeltaMs) {
        for (Industry ind : industries) ind.tick(simDeltaMs);
        for (City city : cities) city.tick(simDeltaMs);

        for (TrafficLight tl : trafficLights) tl.update((int) simDeltaMs);

        vehicleMoveAccMs += simDeltaMs;
        while (vehicleMoveAccMs >= BASE_MOVE_INTERVAL) {
            vehicleMoveAccMs -= BASE_MOVE_INTERVAL;
            for (Vehicle v : vehicles) {
                if (v.getAssignedRoute() == null || v.getCurrentTile() == null) continue;
                v.addMoveAccumulator(BASE_MOVE_INTERVAL);
                long interval = (long)(BASE_MOVE_INTERVAL / v.getSpeed());
                if (interval < 50) interval = 50;
                while (v.getMoveAccumulator() >= interval) {
                    v.drainMoveAccumulator(interval);
                    if (v.hasReachedTarget()) {
                        advanceVehicleRoute(v);
                    }
                    v.move();
                }
            }
        }

        if (map != null) {
            forestAccMs += simDeltaMs;
            if (forestAccMs >= FOREST_INTERVAL) {
                forestAccMs -= FOREST_INTERVAL;
                map.updateForests();
            }
        }

        maintenanceAccMs += simDeltaMs;
        if (maintenanceAccMs >= MAINT_INTERVAL) {
            maintenanceAccMs -= MAINT_INTERVAL;
            double cost = getTotalMaintenanceCost();
            if (cost > 0) economy.chargeMaintenanceCosts(cost);
        }

        congestionAccMs += simDeltaMs;
        if (congestionAccMs >= CONGEST_INTERVAL) {
            congestionAccMs -= CONGEST_INTERVAL;
            detectAndResolveCongestedRoutes();
        }

        simulatedDayMs += simDeltaMs;
        while (simulatedDayMs >= MS_PER_DAY) {
            simulatedDayMs -= MS_PER_DAY;
            dayCount++;
            dayLabel.set("Day " + dayCount);
        }

        if (economy.isBankrupt()) {
            gameOver = true;
            gameLoop.stop();
            if (onBankruptCallback != null) onBankruptCallback.run();
        }
    }

    /**
     * Advances a vehicle to the next stop on its route, computing a new path via BFS.
     * @param v the vehicle to advance
     */
    private void advanceVehicleRoute(Vehicle v) {
        if (map == null) return;
        Route route = v.getAssignedRoute();
        if (route.getStops().size() < 2) return;

        Stop current = v.getTargetStop();
        if (current == null) {
            current = route.getStops().get(0);
        }
        v.setCurrentStop(current);
        v.unloadCargo(current);
        v.loadCargo(current);

        Stop next = route.getNextStop(current);
        v.setTargetStop(next);

        List<RoadTile> path = map.findPath(
                v.getCurrentTile().getX(), v.getCurrentTile().getY(),
                next.getX(), next.getY());
        v.setPath(path);
    }

    /**
     * Calculates the total maintenance cost for the entire vehicle fleet.
     * @return total maintenance cost per interval
     */
    @Override
    public double getTotalMaintenanceCost() {
        double total = 0;
        for (Vehicle v : vehicles) total += v.getMaintenanceCost();
        return total;
    }

    /**
     * Adds a vehicle to the fleet, wires traffic light lookup and delivery callback.
     * @param vehicle the vehicle to add
     */
    public void addVehicle(Vehicle vehicle) {
        vehicle.setTrafficLightLookup(this::findTrafficLight);
        vehicle.setDeliveryCallback(income -> {
            economy.earn(income);
            economy.recordDelivery(income);
        });
        vehicles.add(vehicle);
    }

    /**
     * Finds the traffic light installed at the given road tile.
     * @param tile the road tile to check
     * @return the TrafficLight at that tile, or null if none
     */
    public TrafficLight findTrafficLight(RoadTile tile) {
        for (TrafficLight tl : trafficLights) {
            if (tl.getLocation() == tile) return tl;
        }
        return null;
    }

    /**
     * Removes a vehicle from the fleet.
     * @param vehicle the vehicle to remove
     */
    public void removeVehicle(Vehicle vehicle) { vehicles.remove(vehicle); }

    /**
     * Removes all vehicles from the fleet and clears them from their tiles.
     */
    public void clearVehicles() {
        for (Vehicle v : vehicles) {
            if (v.getCurrentTile() != null) v.getCurrentTile().getVehiclesOnTile().remove(v);
        }
        vehicles.clear();
    }

    /**
     * Creates a new route and registers it.
     * @return the created Route
     */
    public Route createRoute() {
        Route route = new Route();
        routes.add(route);
        return route;
    }

    /**
     * Adds a traffic light to the simulation.
     * @param tl the traffic light to add
     */
    public void addTrafficLight(TrafficLight tl) { trafficLights.add(tl); }

    /** @return unmodifiable list of traffic lights */
    public List<TrafficLight> getTrafficLights() { return Collections.unmodifiableList(trafficLights); }


    /**
     * Resolves which City and Industry are adjacent to a stop based on tile positions.
     * Also checks city internal roads. Called after a stop is placed to wire up cargo loading/unloading.
     * @param stop the stop to resolve neighbors for
     */
    public void resolveStopNeighbors(Stop stop) {
        int sx = stop.getX(), sy = stop.getY();
        for (City c : cities) {
            if (isAdjacentOrInsideArea(sx, sy, c.getTileX(), c.getTileY(), c.getWidthTiles(), c.getHeightTiles())) {
                stop.setNearbyCity(c);
                break;
            }
        }
        for (Industry ind : industries) {
            if (isAdjacentOrInsideArea(sx, sy, ind.getTileX(), ind.getTileY(), ind.getWidthTiles(), ind.getHeightTiles())) {
                stop.setNearbyIndustry(ind);
                break;
            }
        }
    }

    /**
     * Checks if a point is adjacent to (within 1 tile of) or inside a rectangular area.
     * @param px point x
     * @param py point y
     * @param ax area top-left x
     * @param ay area top-left y
     * @param aw area width
     * @param ah area height
     * @return true if adjacent or inside
     */
    private boolean isAdjacentOrInsideArea(int px, int py, int ax, int ay, int aw, int ah) {
        return px >= ax - 1 && px <= ax + aw && py >= ay - 1 && py <= ay + ah;
    }

    /**
     * Sets the simulation speed to the given value and updates the HUD label.
     * @param speed the desired SimSpeed
     */
    public void setSimSpeed(SimSpeed speed) {
        simSpeed.set(speed);
        simSpeedLabel.set(speed.getLabel());
    }

    /** Advances to the next speed in the cycle. */
    public void cycleSimSpeed() { setSimSpeed(simSpeed.get().next()); }

    /** Pauses the simulation. */
    public void pause() { setSimSpeed(SimSpeed.PAUSED); }

    /** Resumes the simulation at normal speed. */
    public void resume() { setSimSpeed(SimSpeed.NORMAL); }

    /**
     * Starts the game loop.
     */
    public void startGame() {
        if (!gameStarted) {
            gameStarted  = true;
            lastNanoTime = -1;
            gameLoop.start();
        }
    }

    /**
     * Resets the simulation to its initial state and restarts the game loop.
     * Invokes the restart callback so the UI can regenerate the map and domain objects.
     */
    public void restartGame() {
        gameLoop.stop();
        gameOver = false;
        gameStarted = false;
        dayCount = 1;
        simulatedDayMs = 0;
        maintenanceAccMs = 0;
        forestAccMs = 0;
        vehicleMoveAccMs = 0;
        congestionAccMs = 0;
        dayLabel.set("Day 1");
        economy.reset();
        industries.clear();
        cities.clear();
        vehicles.clear();
        routes.clear();
        trafficLights.clear();
        Route.resetIdCounter();
        Stop.resetIdCounter();
        if (onRestartCallback != null) onRestartCallback.run();
        startGame();
    }

    /** Stops the game loop immediately. */
    public void stopGame() { gameLoop.stop(); }

    /**
     * Automatically connects two locations: builds road, places stops, creates route,
     * places a vehicle, and assigns it to the route.
     * @param fromX stop-road x near the origin
     * @param fromY stop-road y near the origin
     * @param toX stop-road x near the destination
     * @param toY stop-road y near the destination
     * @param vehicle the pre-created vehicle to use
     * @return a summary string describing what was done, or an error message
     */
    public String autoConnect(int fromX, int fromY, int toX, int toY, Vehicle vehicle) {
        if (map == null) return "No map loaded.";
        int roadCost = map.buildRoadPath(fromX, fromY, toX, toY);
        if (roadCost < 0) return "No path found between locations!";
        if (roadCost > 0) {
            economy.spend(roadCost);
            economy.recordConstruction(roadCost);
        }

        Stop stopA = map.placeStop(fromX, fromY);
        if (stopA == null) {
            for (Stop s : map.getStops()) {
                if (s.getX() == fromX && s.getY() == fromY) { stopA = s; break; }
            }
        }
        Stop stopB = map.placeStop(toX, toY);
        if (stopB == null) {
            for (Stop s : map.getStops()) {
                if (s.getX() == toX && s.getY() == toY) { stopB = s; break; }
            }
        }
        if (stopA == null || stopB == null) return "Could not place stops at endpoints!";
        resolveStopNeighbors(stopA);
        resolveStopNeighbors(stopB);

        Route route = createRoute();
        route.addStop(stopA);
        route.addStop(stopB);

        addVehicle(vehicle);
        RoadTile startTile = (RoadTile) map.getTile(fromX, fromY);
        vehicle.setCurrentTile(startTile);
        route.assignVehicle(vehicle);

        return "Road: $" + roadCost + " | Route #" + route.getId() + " | Vehicle #" + vehicle.getId() + " deployed";
    }

    /**
     * Detects congested road tiles (too many waiting vehicles) and reroutes
     * affected vehicles via an alternative path that avoids the congested tile.
     * If no alternative road path exists, builds a new bypass road.
     */
    private void detectAndResolveCongestedRoutes() {
        if (map == null) return;
        java.util.Set<Long> congestedKeys = new java.util.HashSet<>();
        for (int x = 0; x < GameMap.WIDTH; x++) {
            for (int y = 0; y < GameMap.HEIGHT; y++) {
                Tile tile = map.getTile(x, y);
                if (!(tile instanceof RoadTile road)) continue;
                if (road.getVehiclesOnTile().size() >= CONGESTION_THRESHOLD) {
                    congestedKeys.add(tileKey(x, y));
                }
            }
        }
        if (congestedKeys.isEmpty()) return;
        for (Vehicle v : vehicles) {
            if (v.getAssignedRoute() == null || v.getCurrentTile() == null) continue;
            if (!v.isWaiting()) continue;
            Stop target = v.getTargetStop();
            if (target == null) continue;
            int cx = v.getCurrentTile().getX(), cy = v.getCurrentTile().getY();
            int tx = target.getX(), ty = target.getY();
            List<RoadTile> altPath = map.findPathAvoiding(cx, cy, tx, ty, congestedKeys);
            if (!altPath.isEmpty()) {
                v.setPath(altPath);
            } else {
                List<RoadTile> bypassPath = buildBypassRoad(cx, cy, tx, ty, congestedKeys);
                if (!bypassPath.isEmpty()) v.setPath(bypassPath);
            }
        }
    }

    /**
     * Builds a bypass road around congested tiles and returns the new path.
     * @param sx start x
     * @param sy start y
     * @param ex end x
     * @param ey end y
     * @param avoid set of tile keys to avoid
     * @return the new path, or empty if impossible
     */
    private List<RoadTile> buildBypassRoad(int sx, int sy, int ex, int ey, java.util.Set<Long> avoid) {
        java.util.Map<Long, Long> cameFrom = new java.util.HashMap<>();
        java.util.Queue<long[]> queue = new java.util.LinkedList<>();
        long startKey = tileKey(sx, sy);
        long endKey = tileKey(ex, ey);
        cameFrom.put(startKey, -1L);
        queue.add(new long[]{sx, sy});

        while (!queue.isEmpty()) {
            long[] cur = queue.poll();
            int cx = (int) cur[0], cy = (int) cur[1];
            if (tileKey(cx, cy) == endKey) {
                List<int[]> coords = new java.util.ArrayList<>();
                long k = endKey;
                while (k != -1L) {
                    coords.add(0, new int[]{(int)(k >> 32), (int)(k & 0xFFFFFFFFL)});
                    k = cameFrom.get(k);
                }
                int totalCost = 0;
                for (int[] p : coords) {
                    Tile t = map.getTile(p[0], p[1]);
                    if (t instanceof RoadTile) continue;
                    if (t != null && t.isBuildable()) {
                        int c = map.buildRoad(p[0], p[1]);
                        totalCost += c;
                    }
                }
                if (totalCost > 0) {
                    economy.spend(totalCost);
                    economy.recordConstruction(totalCost);
                }
                return map.findPathAvoiding(sx, sy, ex, ey, avoid);
            }
            int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
            for (int[] d : dirs) {
                int nx = cx + d[0], ny = cy + d[1];
                if (!map.inBounds(nx, ny)) continue;
                long nk = tileKey(nx, ny);
                if (cameFrom.containsKey(nk)) continue;
                if (avoid.contains(nk) && nk != endKey) continue;
                Tile nt = map.getTile(nx, ny);
                if (nt instanceof RoadTile || (nt != null && nt.isBuildable())) {
                    cameFrom.put(nk, tileKey(cx, cy));
                    queue.add(new long[]{nx, ny});
                }
            }
        }
        return List.of();
    }

    private long tileKey(int x, int y) { return (long) x << 32 | (y & 0xFFFFFFFFL); }

    /**
     * Registers an industry with the game loop.
     * @param industry the industry to register
     */
    public void addIndustry(Industry industry) { industries.add(industry); }

    /**
     * Registers a city with the game loop.
     * @param city the city to register
     */
    public void addCity(City city) { cities.add(city); }

    /**
     * Sets a callback to run when the player goes bankrupt.
     * @param callback the bankruptcy handler
     */
    public void setOnBankruptCallback(Runnable callback) { this.onBankruptCallback = callback; }

    /**
     * Sets a callback to run when the game is restarted (to regenerate map/domain objects).
     * @param callback the restart handler
     */
    public void setOnRestartCallback(Runnable callback) { this.onRestartCallback = callback; }

    /** @return the Economy */
    public Economy getEconomy() { return economy; }

    /** @return the GameMap */
    public GameMap getMap() { return map; }

    /** @return current SimSpeed */
    public SimSpeed getSimSpeed() { return simSpeed.get(); }

    /** @return day count */
    public int getDayCount() { return dayCount; }

    /** @return true if game over */
    public boolean isGameOver() { return gameOver; }

    /** @return unmodifiable list of Industry instances */
    public List<Industry> getIndustries() { return Collections.unmodifiableList(industries); }

    /** @return unmodifiable list of City instances */
    public List<City> getCities() { return Collections.unmodifiableList(cities); }

    /** @return unmodifiable list of all vehicles in the fleet */
    public List<Vehicle> getVehicles() { return Collections.unmodifiableList(vehicles); }

    /** @return unmodifiable list of all routes */
    public List<Route> getRoutes() { return Collections.unmodifiableList(routes); }

    /** @return ObjectProperty of SimSpeed */
    public ObjectProperty<SimSpeed> simSpeedProperty() { return simSpeed; }

    /** @return StringProperty for the speed label */
    public StringProperty simSpeedLabelProperty() { return simSpeedLabel; }

    /** @return StringProperty for the day label */
    public StringProperty dayLabelProperty() { return dayLabel; }
}
