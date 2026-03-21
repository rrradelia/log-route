package game.map;

import game.tile.*;
import game.transport.Stop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents the 64x64 game map grid containing all tiles, bridges, and stops.
 * Handles map generation, road/bridge/stop construction, and forest updates.
 */
public class GameMap {
    public static final int WIDTH = 64;
    public static final int HEIGHT = 64;
    public static final int ROAD_COST = 100;

    private final Tile[][] grid = new Tile[WIDTH][HEIGHT];
    private final List<Bridge> bridges = new ArrayList<>();
    private final List<Stop> stops = new ArrayList<>();
    private final Set<Long> cityInternalRoads = new HashSet<>();
    private MapGenerator lastGenerator;

    /**
     * Generates the map with a random seed.
     * Initializes all tiles to grass, then places terrain features.
     */
    public void generate() {
        initGrid();
        lastGenerator = new MapGenerator(this);
        lastGenerator.generate();
    }

    /**
     * Generates the map with a specific seed for reproducible results.
     * @param seed The random seed to use.
     */
    public void generate(long seed) {
        initGrid();
        lastGenerator = new MapGenerator(this, seed);
        lastGenerator.generate();
    }

    /**
     * Returns the generator used for the last map generation.
     * Use this to retrieve city/industry positions and names.
     * @return The last MapGenerator, or null if generate() was not called.
     */
    public MapGenerator getLastGenerator() { return lastGenerator; }

    /**
     * Initializes the entire grid with grass tiles.
     */
    private void initGrid() {
        for (int x = 0; x < WIDTH; x++)
            for (int y = 0; y < HEIGHT; y++)
                grid[x][y] = new GrassTile(x, y);
    }

    /**
     * Returns the tile at the given coordinates.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return The tile, or null if out of bounds.
     */
    public Tile getTile(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return null;
        return grid[x][y];
    }

    /**
     * Sets a tile at the given coordinates.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @param tile The tile to place.
     */
    public void setTile(int x, int y, Tile tile) {
        if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT) grid[x][y] = tile;
    }

    /**
     * Builds a road at the given position, clearing forest if needed.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return The total cost (base + clearing), or 0 if not buildable.
     */
    public int buildRoad(int x, int y) {
        Tile tile = getTile(x, y);
        if (tile == null || !tile.isBuildable()) return 0;
        int cost = ROAD_COST;
        if (tile instanceof ForestTile f) cost += f.getClearingCost();
        setTile(x, y, new RoadTile(x, y));
        return cost;
    }

    /**
     * Checks if a road can be built at the given position.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return True if the tile exists and is buildable.
     */
    public boolean isValidBuildSite(int x, int y) {
        Tile tile = getTile(x, y);
        return tile != null && tile.isBuildable();
    }

    /**
     * Builds a bridge between two endpoints across water tiles.
     * @param x1 Start x-coordinate.
     * @param y1 Start y-coordinate.
     * @param x2 End x-coordinate.
     * @param y2 End y-coordinate.
     * @param type The bridge type (WOODEN, STONE, or STEEL).
     * @return The built Bridge, or null if invalid placement.
     */
    public Bridge buildBridge(int x1, int y1, int x2, int y2, game.util.BridgeType type) {
        if (x1 != x2 && y1 != y2) return null;
        Tile start = getTile(x1, y1);
        Tile end = getTile(x2, y2);
        if (start == null || end == null) return null;
        if (start instanceof WaterTile || end instanceof WaterTile) return null;

        List<Tile> span = new ArrayList<>();
        if (y1 == y2) {
            int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
            for (int x = minX; x <= maxX; x++) span.add(getTile(x, y1));
        } else {
            int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
            for (int y = minY; y <= maxY; y++) span.add(getTile(x1, y));
        }
        if (span.size() < 3) return null;
        if (span.size() - 2 > type.getMaxLength()) return null;
        for (int i = 1; i < span.size() - 1; i++) {
            if (!(span.get(i) instanceof WaterTile)) return null;
        }
        for (int i = 1; i < span.size() - 1; i++) {
            Tile w = span.get(i);
            setTile(w.getX(), w.getY(), new RoadTile(w.getX(), w.getY()));
        }
        if (!(start instanceof RoadTile)) setTile(x1, y1, new RoadTile(x1, y1));
        if (!(end instanceof RoadTile)) setTile(x2, y2, new RoadTile(x2, y2));
        List<Tile> bridgeTiles = new ArrayList<>();
        if (y1 == y2) {
            int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
            for (int x = minX; x <= maxX; x++) bridgeTiles.add(getTile(x, y1));
        } else {
            int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
            for (int y = minY; y <= maxY; y++) bridgeTiles.add(getTile(x1, y));
        }
        Bridge bridge = new Bridge(type, bridgeTiles);
        bridges.add(bridge);
        return bridge;
    }

    /**
     * Checks if a tile is a valid bridge endpoint (not water, not out of bounds).
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return True if valid.
     */
    public boolean isValidBridgeEndpoint(int x, int y) {
        Tile t = getTile(x, y);
        return t != null && !(t instanceof WaterTile);
    }

    /**
     * Checks if a tile is part of any built bridge.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return True if the tile belongs to a bridge.
     */
    public boolean isBridgeTile(int x, int y) {
        for (Bridge b : bridges) {
            for (Tile t : b.getTiles()) {
                if (t.getX() == x && t.getY() == y) return true;
            }
        }
        return false;
    }

    /**
     * Updates all forest tiles: grows trees and spreads to adjacent grass tiles.
     */
    public void updateForests() {
        List<int[]> spreadTargets = new ArrayList<>();
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                Tile t = grid[x][y];
                if (t instanceof ForestTile f) {
                    f.growTrees();
                    if (f.canSpread()) {
                        int[][] neighbors = {{x-1,y},{x+1,y},{x,y-1},{x,y+1}};
                        for (int[] n : neighbors) {
                            Tile nt = getTile(n[0], n[1]);
                            if (nt instanceof GrassTile) spreadTargets.add(n);
                        }
                    }
                }
            }
        }
        for (int[] pos : spreadTargets) {
            setTile(pos[0], pos[1], new ForestTile(pos[0], pos[1], 1));
        }
    }

    /**
     * Places a stop on a road tile adjacent to a city or industry.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return The created Stop, or null if invalid placement.
     */
    public Stop placeStop(int x, int y) {
        if (!isValidStopSite(x, y)) return null;
        Tile tile = getTile(x, y);
        if (!(tile instanceof RoadTile road)) return null;
        for (Stop s : stops) {
            if (s.getX() == x && s.getY() == y) return null;
        }
        Stop stop = new Stop(road);
        stops.add(stop);
        return stop;
    }

    /**
     * Checks if a stop can be placed at the given position.
     * Valid if it is a road tile adjacent to a city, industry, or is a city internal road.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return True if valid stop placement.
     */
    public boolean isValidStopSite(int x, int y) {
        Tile tile = getTile(x, y);
        if (!(tile instanceof RoadTile)) return false;
        if (isCityInternalRoad(x, y)) return true;
        int[][] neighbors = {{x-1,y},{x+1,y},{x,y-1},{x,y+1}};
        for (int[] n : neighbors) {
            Tile nt = getTile(n[0], n[1]);
            if (nt instanceof CityTile || nt instanceof IndustryTile) return true;
        }
        return false;
    }

    /**
     * Builds a road path between two points using BFS on buildable tiles.
     * Clears forests along the way. Does not charge economy — returns total cost.
     * @param sx Start x.
     * @param sy Start y.
     * @param ex End x.
     * @param ey End y.
     * @return Total construction cost, or -1 if no path found.
     */
    public int buildRoadPath(int sx, int sy, int ex, int ey) {
        if (!inBounds(sx, sy) || !inBounds(ex, ey)) return -1;
        java.util.Map<Long, Long> cameFrom = new java.util.HashMap<>();
        java.util.Queue<long[]> queue = new java.util.LinkedList<>();
        long startKey = key(sx, sy);
        long endKey = key(ex, ey);
        cameFrom.put(startKey, -1L);
        queue.add(new long[]{sx, sy});

        while (!queue.isEmpty()) {
            long[] cur = queue.poll();
            int cx = (int) cur[0], cy = (int) cur[1];
            if (key(cx, cy) == endKey) {
                List<int[]> path = new java.util.ArrayList<>();
                long k = endKey;
                while (k != -1L) {
                    int px = (int)(k >> 32);
                    int py = (int)(k & 0xFFFFFFFFL);
                    path.add(0, new int[]{px, py});
                    k = cameFrom.get(k);
                }
                int totalCost = 0;
                for (int[] p : path) {
                    Tile t = getTile(p[0], p[1]);
                    if (t instanceof RoadTile) continue;
                    if (t != null && t.isBuildable()) {
                        int c = buildRoad(p[0], p[1]);
                        totalCost += c;
                    }
                }
                return totalCost;
            }
            int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
            for (int[] d : dirs) {
                int nx = cx + d[0], ny = cy + d[1];
                if (!inBounds(nx, ny)) continue;
                long nk = key(nx, ny);
                if (cameFrom.containsKey(nk)) continue;
                Tile nt = getTile(nx, ny);
                if (nt instanceof RoadTile || (nt != null && nt.isBuildable())) {
                    cameFrom.put(nk, key(cx, cy));
                    queue.add(new long[]{nx, ny});
                }
            }
        }
        return -1;
    }

    /**
     * Finds the nearest road tile adjacent to a rectangular area (city/industry).
     * @param tileX Top-left x of the area.
     * @param tileY Top-left y of the area.
     * @param w Width in tiles.
     * @param h Height in tiles.
     * @return Coordinates {x, y} of the nearest edge road, or null.
     */
    public int[] findEdgeRoad(int tileX, int tileY, int w, int h) {
        for (int dx = -1; dx <= w; dx++) {
            for (int dy = -1; dy <= h; dy++) {
                if (dx >= 0 && dx < w && dy >= 0 && dy < h) continue;
                int x = tileX + dx, y = tileY + dy;
                if (inBounds(x, y) && getTile(x, y) instanceof RoadTile) return new int[]{x, y};
            }
        }
        return null;
    }

    /**
     * Finds a road tile adjacent to a rectangular area that is also a valid stop site.
     * Prefers tiles directly touching the entity's CityTile/IndustryTile.
     * @param tileX Top-left x of the area.
     * @param tileY Top-left y of the area.
     * @param w Width in tiles.
     * @param h Height in tiles.
     * @return Coordinates {x, y} of a valid stop road, or null.
     */
    public int[] findStopRoad(int tileX, int tileY, int w, int h) {
        for (int dx = -1; dx <= w; dx++) {
            for (int dy = -1; dy <= h; dy++) {
                if (dx >= 0 && dx < w && dy >= 0 && dy < h) continue;
                int x = tileX + dx, y = tileY + dy;
                if (inBounds(x, y) && isValidStopSite(x, y)) return new int[]{x, y};
            }
        }
        return null;
    }

    /** @return The list of all placed stops. */
    public List<Stop> getStops() { return stops; }

    /** @return The list of all built bridges. */
    public List<Bridge> getBridges() { return bridges; }

    /**
     * Checks if the given coordinates are within the map bounds.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return True if within bounds.
     */
    public boolean inBounds(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }

    /**
     * Registers a road tile as a city internal road.
     * @param road The road tile to register.
     */
    public void addCityInternalRoad(RoadTile road) {
        cityInternalRoads.add(key(road.getX(), road.getY()));
    }

    /**
     * Checks if a tile is a city internal road.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return True if this is a predefined city road.
     */
    public boolean isCityInternalRoad(int x, int y) {
        return cityInternalRoads.contains(key(x, y));
    }

    /**
     * Returns the four cardinal neighbors of the given tile.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return List of neighboring tiles (only in-bounds, non-null).
     */
    public List<Tile> getNeighbors(int x, int y) {
        List<Tile> result = new ArrayList<>();
        int[][] offsets = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] o : offsets) {
            Tile t = getTile(x + o[0], y + o[1]);
            if (t != null) result.add(t);
        }
        return result;
    }

    /**
     * Checks if any neighbor of (x,y) is of the given tile type.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @param type The tile type to check for.
     * @return True if at least one adjacent tile matches.
     */
    public boolean isAdjacentToType(int x, int y, Tile.TileType type) {
        for (Tile t : getNeighbors(x, y)) {
            if (t.getType() == type) return true;
        }
        return false;
    }

    /**
     * Finds the shortest path of RoadTiles between two positions using BFS.
     * @param sx Start x.
     * @param sy Start y.
     * @param ex End x.
     * @param ey End y.
     * @return Ordered list of RoadTiles from start to end, or empty if no path.
     */
    public List<RoadTile> findPath(int sx, int sy, int ex, int ey) {
        if (!inBounds(sx, sy) || !inBounds(ex, ey)) return List.of();
        if (!(getTile(sx, sy) instanceof RoadTile) || !(getTile(ex, ey) instanceof RoadTile)) return List.of();

        java.util.Map<Long, Long> cameFrom = new java.util.HashMap<>();
        java.util.Queue<long[]> queue = new java.util.LinkedList<>();
        long startKey = key(sx, sy);
        long endKey = key(ex, ey);
        cameFrom.put(startKey, -1L);
        queue.add(new long[]{sx, sy});

        while (!queue.isEmpty()) {
            long[] cur = queue.poll();
            int cx = (int) cur[0], cy = (int) cur[1];
            if (key(cx, cy) == endKey) {
                List<RoadTile> path = new ArrayList<>();
                long k = endKey;
                while (k != -1L) {
                    int px = (int)(k >> 32);
                    int py = (int)(k & 0xFFFFFFFFL);
                    path.add(0, (RoadTile) getTile(px, py));
                    k = cameFrom.get(k);
                }
                return path;
            }
            int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
            for (int[] d : dirs) {
                int nx = cx + d[0], ny = cy + d[1];
                if (!inBounds(nx, ny)) continue;
                if (!(getTile(nx, ny) instanceof RoadTile)) continue;
                long nk = key(nx, ny);
                if (cameFrom.containsKey(nk)) continue;
                cameFrom.put(nk, key(cx, cy));
                queue.add(new long[]{nx, ny});
            }
        }
        return List.of();
    }

    /**
     * Finds the shortest path of RoadTiles between two positions using BFS,
     * avoiding a set of congested tile keys.
     * @param sx Start x.
     * @param sy Start y.
     * @param ex End x.
     * @param ey End y.
     * @param avoid Set of tile keys to avoid (except start and end).
     * @return Ordered list of RoadTiles from start to end, or empty if no path.
     */
    public List<RoadTile> findPathAvoiding(int sx, int sy, int ex, int ey, Set<Long> avoid) {
        if (!inBounds(sx, sy) || !inBounds(ex, ey)) return List.of();
        if (!(getTile(sx, sy) instanceof RoadTile) || !(getTile(ex, ey) instanceof RoadTile)) return List.of();

        java.util.Map<Long, Long> cameFrom = new java.util.HashMap<>();
        java.util.Queue<long[]> queue = new java.util.LinkedList<>();
        long startKey = key(sx, sy);
        long endKey = key(ex, ey);
        cameFrom.put(startKey, -1L);
        queue.add(new long[]{sx, sy});

        while (!queue.isEmpty()) {
            long[] cur = queue.poll();
            int cx = (int) cur[0], cy = (int) cur[1];
            if (key(cx, cy) == endKey) {
                List<RoadTile> path = new ArrayList<>();
                long k = endKey;
                while (k != -1L) {
                    int px = (int)(k >> 32);
                    int py = (int)(k & 0xFFFFFFFFL);
                    path.add(0, (RoadTile) getTile(px, py));
                    k = cameFrom.get(k);
                }
                return path;
            }
            int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
            for (int[] d : dirs) {
                int nx = cx + d[0], ny = cy + d[1];
                if (!inBounds(nx, ny)) continue;
                if (!(getTile(nx, ny) instanceof RoadTile)) continue;
                long nk = key(nx, ny);
                if (cameFrom.containsKey(nk)) continue;
                if (avoid.contains(nk) && nk != endKey && nk != startKey) continue;
                cameFrom.put(nk, key(cx, cy));
                queue.add(new long[]{nx, ny});
            }
        }
        return List.of();
    }

    /**
     * Counts how many road-tile neighbors a road tile has (for junction detection).
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return Number of adjacent road tiles (0-4).
     */
    public int countRoadNeighbors(int x, int y) {
        int count = 0;
        for (Tile t : getNeighbors(x, y)) {
            if (t instanceof RoadTile) count++;
        }
        return count;
    }

    /**
     * Estimates the cost of building a road path between two points without actually building.
     * Uses BFS on buildable/road tiles. Returns the cost of only the tiles that need construction.
     * @param sx Start x.
     * @param sy Start y.
     * @param ex End x.
     * @param ey End y.
     * @return Estimated construction cost, or -1 if no path found.
     */
    public int calcRoadPathCost(int sx, int sy, int ex, int ey) {
        if (!inBounds(sx, sy) || !inBounds(ex, ey)) return -1;
        java.util.Map<Long, Long> cameFrom = new java.util.HashMap<>();
        java.util.Queue<long[]> queue = new java.util.LinkedList<>();
        long startKey = key(sx, sy);
        long endKey = key(ex, ey);
        cameFrom.put(startKey, -1L);
        queue.add(new long[]{sx, sy});

        while (!queue.isEmpty()) {
            long[] cur = queue.poll();
            int cx = (int) cur[0], cy = (int) cur[1];
            if (key(cx, cy) == endKey) {
                int totalCost = 0;
                long k = endKey;
                while (k != -1L) {
                    int px = (int)(k >> 32);
                    int py = (int)(k & 0xFFFFFFFFL);
                    Tile t = getTile(px, py);
                    if (!(t instanceof RoadTile) && t != null && t.isBuildable()) {
                        totalCost += ROAD_COST;
                        if (t instanceof ForestTile f) totalCost += f.getClearingCost();
                    }
                    k = cameFrom.get(k);
                }
                return totalCost;
            }
            int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
            for (int[] d : dirs) {
                int nx = cx + d[0], ny = cy + d[1];
                if (!inBounds(nx, ny)) continue;
                long nk = key(nx, ny);
                if (cameFrom.containsKey(nk)) continue;
                Tile nt = getTile(nx, ny);
                if (nt instanceof RoadTile || (nt != null && nt.isBuildable())) {
                    cameFrom.put(nk, key(cx, cy));
                    queue.add(new long[]{nx, ny});
                }
            }
        }
        return -1;
    }

    /**
     * Places a 3x3 city at the given position with internal cross-roads and edge roads.
     * @param startX Top-left x-coordinate.
     * @param startY Top-left y-coordinate.
     * @param name The city name.
     * @return True if placed successfully.
     */
    public boolean placeCity(int startX, int startY, String name) {
        if (!canPlaceEntity(startX, startY, 3, 3)) return false;
        for (int dx = 0; dx < 3; dx++)
            for (int dy = 0; dy < 3; dy++)
                setTile(startX + dx, startY + dy, new CityTile(startX + dx, startY + dy, name));
        int midX = startX + 1, midY = startY + 1;
        placeCityRoad(midX, midY);
        placeCityRoad(startX, midY);
        placeCityRoad(startX + 2, midY);
        placeCityRoad(midX, startY);
        placeCityRoad(midX, startY + 2);
        placeEdgeRoad(startX - 1, midY);
        placeEdgeRoad(startX + 3, midY);
        placeEdgeRoad(midX, startY - 1);
        placeEdgeRoad(midX, startY + 3);
        return true;
    }

    /**
     * Places a 2x2 industry at the given position with edge roads.
     * @param startX Top-left x-coordinate.
     * @param startY Top-left y-coordinate.
     * @param name The industry name.
     * @return True if placed successfully.
     */
    public boolean placeIndustry(int startX, int startY, String name) {
        if (!canPlaceEntity(startX, startY, 2, 2)) return false;
        for (int dx = 0; dx < 2; dx++)
            for (int dy = 0; dy < 2; dy++)
                setTile(startX + dx, startY + dy, new IndustryTile(startX + dx, startY + dy, name));
        placeEdgeRoad(startX - 1, startY);
        placeEdgeRoad(startX + 2, startY);
        return true;
    }

    /**
     * Checks if an entity can be placed at the given area (all tiles must be grass or forest).
     * @param startX Top-left x.
     * @param startY Top-left y.
     * @param w Width.
     * @param h Height.
     * @return True if placeable.
     */
    private boolean canPlaceEntity(int startX, int startY, int w, int h) {
        for (int dx = -1; dx <= w; dx++) {
            for (int dy = -1; dy <= h; dy++) {
                int x = startX + dx, y = startY + dy;
                if (!inBounds(x, y)) return false;
                if (dx >= 0 && dx < w && dy >= 0 && dy < h) {
                    Tile t = getTile(x, y);
                    if (!(t instanceof GrassTile) && !(t instanceof ForestTile)) return false;
                }
            }
        }
        return true;
    }

    private void placeCityRoad(int x, int y) {
        if (inBounds(x, y)) {
            RoadTile road = new RoadTile(x, y);
            setTile(x, y, road);
            addCityInternalRoad(road);
        }
    }

    private void placeEdgeRoad(int x, int y) {
        if (inBounds(x, y) && (getTile(x, y) instanceof GrassTile || getTile(x, y) instanceof ForestTile)) {
            setTile(x, y, new RoadTile(x, y));
        }
    }

    private long key(int x, int y) { return (long) x << 32 | (y & 0xFFFFFFFFL); }
}
