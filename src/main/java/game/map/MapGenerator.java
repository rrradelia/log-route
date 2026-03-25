package game.map;

import game.tile.*;
import game.tile.Tile.TileType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Procedurally generates the game map with rivers, lakes, cities,
 * industries, and forest clusters on a 64x64 grid.
 */
public class MapGenerator {

    private static final int CITY_COUNT = 4;
    private static final int INDUSTRY_COUNT = 6;
    private static final int LAKE_COUNT = 3;
    private static final int FOREST_CLUSTER_COUNT = 10;
    private static final int MIN_SPACING = 8;

    private final GameMap map;
    private final Random random;
    private final List<int[]> cityPositions = new ArrayList<>();
    private final List<String> cityNames = new ArrayList<>();
    private final List<int[]> industryPositions = new ArrayList<>();
    private final List<String> industryTypes = new ArrayList<>();

    /**
     * Constructs a map generator with a specific seed.
     * @param map The game map to populate.
     * @param seed The random seed for reproducible generation.
     */
    public MapGenerator(GameMap map, long seed) {
        this.map = map;
        this.random = new Random(seed);
    }

    /**
     * Constructs a map generator with a random seed.
     * @param map The game map to populate.
     */
    public MapGenerator(GameMap map) {
        this(map, System.currentTimeMillis());
    }

    /**
     * Generates terrain features on the map:
     * river, lakes, then forests. Cities and industries are player-placed.
     */
    public void generate() {
        generateRiver();
        generateLakes();
        generateForests();
    }

    /**
     * Generates a winding 2-wide river from top to bottom of the map.
     */
    private void generateRiver() {
        int x = 20 + random.nextInt(24);
        for (int y = 0; y < GameMap.HEIGHT; y++) {
            map.setTile(x, y, new WaterTile(x, y));
            if (x > 1 && x < GameMap.WIDTH - 2) {
                int adjacent = x + (random.nextBoolean() ? 1 : -1);
                map.setTile(adjacent, y, new WaterTile(adjacent, y));
            }
            int drift = random.nextInt(3) - 1;
            x = Math.max(2, Math.min(GameMap.WIDTH - 3, x + drift));
        }
    }

    /**
     * Generates circular lakes at random positions on the map.
     */
    private void generateLakes() {
        for (int i = 0; i < LAKE_COUNT; i++) {
            int cx = 5 + random.nextInt(GameMap.WIDTH - 10);
            int cy = 5 + random.nextInt(GameMap.HEIGHT - 10);
            int radius = 2 + random.nextInt(2);

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (dx * dx + dy * dy <= radius * radius) {
                        int wx = cx + dx;
                        int wy = cy + dy;
                        if (map.inBounds(wx, wy) && map.getTile(wx, wy) instanceof GrassTile) {
                            map.setTile(wx, wy, new WaterTile(wx, wy));
                        }
                    }
                }
            }
        }
    }

    /**
     * Generates 4 cities (3x3) with internal cross-road networks and edge roads.
     */
    private void generateCities() {
        String[] cityNames = {"Northport", "Southville", "Eastburg", "Westfield"};
        int placed = 0;
        int attempts = 0;

        while (placed < CITY_COUNT && attempts < 500) {
            attempts++;
            int cx = 4 + random.nextInt(GameMap.WIDTH - 8);
            int cy = 4 + random.nextInt(GameMap.HEIGHT - 8);

            if (canPlaceArea(cx, cy, 3, 3) && isFarFromAll(cx, cy)) {
                placeCity(cx, cy, cityNames[placed]);
                cityPositions.add(new int[]{cx, cy});
                this.cityNames.add(cityNames[placed]);
                placed++;
            }
        }
    }

    /**
     * Places a 3x3 city with internal cross-road and connecting edge roads.
     * @param startX Top-left x-coordinate of the city.
     * @param startY Top-left y-coordinate of the city.
     * @param name The city name.
     */
    private void placeCity(int startX, int startY, String name) {
        for (int dx = 0; dx < 3; dx++) {
            for (int dy = 0; dy < 3; dy++) {
                int x = startX + dx;
                int y = startY + dy;
                map.setTile(x, y, new CityTile(x, y, name));
            }
        }
        int midX = startX + 1;
        int midY = startY + 1;
        placeCityRoad(midX, midY);
        placeCityRoad(startX, midY);
        placeCityRoad(startX + 2, midY);
        placeCityRoad(midX, startY);
        placeCityRoad(midX, startY + 2);
        placeEdgeRoad(startX - 1, midY);
        placeEdgeRoad(startX + 3, midY);
        placeEdgeRoad(midX, startY - 1);
        placeEdgeRoad(midX, startY + 3);
    }

    /**
     * Places a road tile inside a city and registers it as a city internal road.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     */
    private void placeCityRoad(int x, int y) {
        if (map.inBounds(x, y)) {
            RoadTile road = new RoadTile(x, y);
            map.setTile(x, y, road);
            map.addCityInternalRoad(road);
        }
    }

    /**
     * Places a road tile on the edge of a city/industry for external connectivity.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     */
    private void placeEdgeRoad(int x, int y) {
        if (map.inBounds(x, y) && map.getTile(x, y) instanceof GrassTile) {
            map.setTile(x, y, new RoadTile(x, y));
        }
    }

    /**
     * Generates 6 industries (2x2) with edge roads for connectivity.
     */
    private void generateIndustries() {
        String[] types = {"Iron Mine", "Iron Mine", "Wood Farm", "Wood Farm", "Paper Mill", "Paper Mill"};
        int placed = 0;
        int attempts = 0;

        while (placed < INDUSTRY_COUNT && attempts < 500) {
            attempts++;
            int ix = 3 + random.nextInt(GameMap.WIDTH - 6);
            int iy = 3 + random.nextInt(GameMap.HEIGHT - 6);

            if (canPlaceArea(ix, iy, 2, 2) && isFarFromAll(ix, iy)) {
                placeIndustry(ix, iy, types[placed]);
                industryPositions.add(new int[]{ix, iy});
                industryTypes.add(types[placed]);
                placed++;
            }
        }
    }

    /**
     * Places a 2x2 industry with connecting edge roads.
     * @param startX Top-left x-coordinate.
     * @param startY Top-left y-coordinate.
     * @param name The industry name.
     */
    private void placeIndustry(int startX, int startY, String name) {
        for (int dx = 0; dx < 2; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                int x = startX + dx;
                int y = startY + dy;
                map.setTile(x, y, new IndustryTile(x, y, name));
            }
        }
        placeEdgeRoad(startX - 1, startY);
        placeEdgeRoad(startX + 2, startY);
    }

    /**
     * Generates random forest clusters across the map on grass tiles.
     */
    private void generateForests() {
        for (int i = 0; i < FOREST_CLUSTER_COUNT; i++) {
            int cx = 2 + random.nextInt(GameMap.WIDTH - 4);
            int cy = 2 + random.nextInt(GameMap.HEIGHT - 4);
            int size = 3 + random.nextInt(4);

            for (int dx = -size; dx <= size; dx++) {
                for (int dy = -size; dy <= size; dy++) {
                    int fx = cx + dx;
                    int fy = cy + dy;
                    if (map.inBounds(fx, fy)
                            && map.getTile(fx, fy) instanceof GrassTile
                            && random.nextDouble() < 0.6) {
                        int trees = 1 + random.nextInt(ForestTile.MAX_TREES);
                        map.setTile(fx, fy, new ForestTile(fx, fy, trees));
                    }
                }
            }
        }
    }

    /**
     * Checks if a rectangular area can be placed without overlapping existing features.
     * @param startX Top-left x-coordinate.
     * @param startY Top-left y-coordinate.
     * @param w Width of the area.
     * @param h Height of the area.
     * @return True if the area is clear for placement.
     */
    public boolean canPlaceArea(int startX, int startY, int w, int h) {
        for (int dx = -1; dx <= w; dx++) {
            for (int dy = -1; dy <= h; dy++) {
                int x = startX + dx;
                int y = startY + dy;
                if (!map.inBounds(x, y)) return false;
                Tile t = map.getTile(x, y);
                if (dx >= 0 && dx < w && dy >= 0 && dy < h) {
                    if (!(t instanceof GrassTile)) return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks if a position is far enough from all existing cities and industries.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return True if minimum spacing is satisfied.
     */
    private boolean isFarFromAll(int x, int y) {
        for (int[] pos : cityPositions) {
            if (distance(x, y, pos[0], pos[1]) < MIN_SPACING) return false;
        }
        for (int[] pos : industryPositions) {
            if (distance(x, y, pos[0], pos[1]) < MIN_SPACING) return false;
        }
        return true;
    }

    /**
     * Calculates Euclidean distance between two points.
     * @param x1 First point x.
     * @param y1 First point y.
     * @param x2 Second point x.
     * @param y2 Second point y.
     * @return The distance.
     */
    private double distance(int x1, int y1, int x2, int y2) {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    /** @return The list of city top-left positions. */
    public List<int[]> getCityPositions() { return cityPositions; }

    /** @return The list of city names in placement order. */
    public List<String> getCityNames() { return cityNames; }

    /** @return The list of industry top-left positions. */
    public List<int[]> getIndustryPositions() { return industryPositions; }

    /** @return The list of industry type names in placement order. */
    public List<String> getIndustryTypes() { return industryTypes; }
}
