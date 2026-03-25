package game.ui;

import game.map.GameMap;
import game.tile.*;
import game.traffic.TrafficLight;
import game.transport.Route;
import game.transport.Stop;
import game.vehicle.Bus;
import game.vehicle.Truck;
import game.vehicle.Vehicle;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;

/**
 * Canvas-based renderer for the game map, minimap, stops, bridges,
 * vehicles, traffic lights, routes, and build mode highlighting.
 */
public class MapRenderer {

    public static final int TILE_SIZE = 16;

    private final Canvas canvas;
    private GameMap map;
    private double camX = 0, camY = 0;

    private int hoverX = -1, hoverY = -1;
    private String buildMode = null;
    private int bridgeStartX = -1, bridgeStartY = -1;
    private List<Vehicle> vehicles = new ArrayList<>();
    private List<TrafficLight> trafficLights = new ArrayList<>();
    private List<Route> routes = new ArrayList<>();
    private boolean showRouteOverlay = false;

    /**
     * Constructs a renderer for the given game map.
     * Canvas is sized to the viewport, not the full map.
     * @param map The game map to render.
     */
    public MapRenderer(GameMap map) {
        this.map = map;
        this.canvas = new Canvas(900, 700);
    }

    /** @return The canvas used for rendering. */
    public Canvas getCanvas() { return canvas; }

    /**
     * Sets the game map reference (used on restart).
     * @param map The new game map.
     */
    public void setMap(GameMap map) { this.map = map; }

    /** @return The game map being rendered. */
    public GameMap getMap() { return map; }

    /**
     * Sets the list of vehicles to render on the map.
     * @param vehicles The current vehicle fleet.
     */
    public void setVehicles(List<Vehicle> vehicles) { this.vehicles = vehicles; }

    /**
     * Sets the list of traffic lights to render.
     * @param lights The traffic lights.
     */
    public void setTrafficLights(List<TrafficLight> lights) { this.trafficLights = lights; }

    /**
     * Sets the list of routes for overlay rendering.
     * @param routes The routes.
     */
    public void setRoutes(List<Route> routes) { this.routes = routes; }

    /**
     * Toggles the route path overlay on/off.
     */
    public void toggleRouteOverlay() { this.showRouteOverlay = !showRouteOverlay; }

    /** @return True if route overlay is shown. */
    public boolean isRouteOverlayVisible() { return showRouteOverlay; }

    /**
     * Sets the hover position for build mode highlighting.
     * @param x The hovered tile x-coordinate.
     * @param y The hovered tile y-coordinate.
     */
    public void setHover(int x, int y) { this.hoverX = x; this.hoverY = y; }

    /** Clears the hover position. */
    public void clearHover() { this.hoverX = -1; this.hoverY = -1; }

    /**
     * Sets the current build mode and resets bridge start position.
     * @param mode The build mode or null to cancel.
     */
    public void setBuildMode(String mode) {
        this.buildMode = mode;
        this.bridgeStartX = -1;
        this.bridgeStartY = -1;
    }

    /** @return The current build mode, or null if not in build mode. */
    public String getBuildMode() { return buildMode; }

    /**
     * Sets the bridge start position for two-click bridge building.
     * @param x The start tile x-coordinate.
     * @param y The start tile y-coordinate.
     */
    public void setBridgeStart(int x, int y) { this.bridgeStartX = x; this.bridgeStartY = y; }

    /** @return The bridge start x-coordinate, or -1 if not set. */
    public int getBridgeStartX() { return bridgeStartX; }

    /** @return The bridge start y-coordinate, or -1 if not set. */
    public int getBridgeStartY() { return bridgeStartY; }

    /**
     * Renders the visible portion of the map based on camera offset and zoom.
     * @param zoom The current zoom level.
     */
    public void render(double zoom) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.save();
        gc.scale(zoom, zoom);
        gc.translate(-camX, -camY);

        double vw = canvas.getWidth() / zoom;
        double vh = canvas.getHeight() / zoom;
        int startX = Math.max(0, (int)(camX / TILE_SIZE) - 1);
        int startY = Math.max(0, (int)(camY / TILE_SIZE) - 1);
        int endX = Math.min(GameMap.WIDTH, (int)((camX + vw) / TILE_SIZE) + 2);
        int endY = Math.min(GameMap.HEIGHT, (int)((camY + vh) / TILE_SIZE) + 2);

        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                Tile tile = map.getTile(x, y);
                double px = x * TILE_SIZE;
                double py = y * TILE_SIZE;

                gc.setFill(tileColor(tile));
                gc.fillRect(px, py, TILE_SIZE, TILE_SIZE);

                gc.setStroke(Color.rgb(0, 0, 0, 0.06));
                gc.strokeRect(px, py, TILE_SIZE, TILE_SIZE);
            }
        }

        renderBridges(gc);
        renderStops(gc);
        renderTrafficLights(gc);
        if (showRouteOverlay) renderRouteOverlay(gc);
        renderVehicles(gc);

        if ("bridge".equals(buildMode) && bridgeStartX >= 0) {
            double sx = bridgeStartX * TILE_SIZE;
            double sy = bridgeStartY * TILE_SIZE;
            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(3);
            gc.strokeRect(sx, sy, TILE_SIZE, TILE_SIZE);
            gc.setLineWidth(1);
        }
        if (hoverX >= 0 && hoverY >= 0 && buildMode != null) {
            renderBuildHighlight(gc);
        }
        gc.restore();
    }

    /**
     * Renders with default zoom of 1.0.
     */
    public void render() { render(1.0); }

    /**
     * Renders all vehicles on the map with detailed truck/bus sprites.
     * Uses smooth interpolated positions, direction-based rotation, and overtaking offset.
     * @param gc The graphics context.
     */
    private void renderVehicles(GraphicsContext gc) {
        for (Vehicle v : vehicles) {
            if (v.getCurrentTile() == null) continue;
            double rx = v.getRenderX();
            double ry = v.getRenderY();
            double px = rx * TILE_SIZE;
            double py = ry * TILE_SIZE;

            if (v.isOvertaking()) {
                if (v.getDirection() == game.util.Direction.NORTH || v.getDirection() == game.util.Direction.SOUTH) {
                    px += TILE_SIZE * 0.3;
                } else {
                    py += TILE_SIZE * 0.3;
                }
            }

            double cx = px + TILE_SIZE / 2.0;
            double cy = py + TILE_SIZE / 2.0;

            Color bodyColor = vehicleBodyColor(v);
            double rotation = switch (v.getDirection()) {
                case EAST  -> 0;
                case SOUTH -> 90;
                case WEST  -> 180;
                case NORTH -> 270;
            };

            gc.save();
            gc.translate(cx, cy);
            gc.rotate(rotation);

            gc.setFill(Color.rgb(0, 0, 0, 0.3));
            gc.fillRoundRect(-10, -4, 22, 12, 4, 4);

            if (v instanceof Bus) {
                renderBusSprite(gc, bodyColor, v.getCargo() != null);
            } else {
                renderTruckSprite(gc, bodyColor, v.getCargo() != null);
            }
            gc.restore();

            String vLabel = v.getName();
            if (v.getAssignedRoute() != null) vLabel += " R" + v.getAssignedRoute().getId();
            gc.setFont(Font.font(8));
            gc.setFill(Color.rgb(0, 0, 0, 0.7));
            double vlw = vLabel.length() * 4.5 + 4;
            gc.fillRoundRect(px + TILE_SIZE + 2, py - 2, vlw, 11, 3, 3);
            gc.setFill(Color.WHITE);
            gc.fillText(vLabel, px + TILE_SIZE + 4, py + 7);
        }
    }

    /**
     * Determines the body color for a vehicle based on its route or type.
     * @param v The vehicle.
     * @return The body color.
     */
    private Color vehicleBodyColor(Vehicle v) {
        if (v.getAssignedRoute() != null) {
            for (int i = 0; i < routes.size(); i++) {
                if (routes.get(i) == v.getAssignedRoute()) return routeColor(i);
            }
        }
        return (v instanceof Truck) ? Color.rgb(0, 200, 255) : Color.rgb(255, 220, 0);
    }

    /**
     * Renders a detailed truck sprite at the origin (pre-translated and rotated).
     * @param gc The graphics context.
     * @param color The body color.
     * @param hasCargo True if carrying cargo.
     */
    private void renderTruckSprite(GraphicsContext gc, Color color, boolean hasCargo) {
        gc.setFill(color);
        gc.fillRect(-8, -5, 16, 10);
        if (hasCargo) {
            gc.setFill(Color.WHITE);
            gc.fillRect(-4, -3, 8, 6);
        }
    }

    /**
     * Renders a detailed bus sprite at the origin (pre-translated and rotated).
     * @param gc The graphics context.
     * @param color The body color.
     * @param hasPassengers True if carrying passengers.
     */
    private void renderBusSprite(GraphicsContext gc, Color color, boolean hasPassengers) {
        gc.setFill(color);
        gc.fillRect(-10, -6, 20, 12);
        if (hasPassengers) {
            gc.setFill(Color.YELLOW);
            gc.fillRect(-6, -3, 12, 6);
        }
    }

    /**
     * Returns a distinct color for a route index.
     * @param index The route index.
     * @return A color for that route.
     */
    private Color routeColor(int index) {
        Color[] palette = {
                Color.rgb(0, 200, 255), Color.rgb(255, 100, 50),
                Color.rgb(50, 255, 100), Color.rgb(255, 50, 200),
                Color.rgb(255, 255, 50), Color.rgb(150, 100, 255),
                Color.rgb(255, 150, 0),  Color.rgb(0, 255, 200)
        };
        return palette[index % palette.length];
    }

    /**
     * Renders stop markers with ID and nearby entity name label on the map.
     * @param gc The graphics context.
     */
    private void renderStops(GraphicsContext gc) {
        List<Stop> stops = map.getStops();
        for (Stop stop : stops) {
            double px = stop.getX() * TILE_SIZE;
            double py = stop.getY() * TILE_SIZE;
            gc.setFill(Color.RED);
            gc.fillOval(px + 2, py + 2, TILE_SIZE - 4, TILE_SIZE - 4);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font(8));
            gc.fillText(String.valueOf(stop.getId()), px + 3, py + TILE_SIZE - 4);

            String label = stopLabel(stop);
            if (label != null) {
                gc.setFont(Font.font(9));
                gc.setFill(Color.rgb(0, 0, 0, 0.7));
                double tw = label.length() * 5.0 + 4;
                gc.fillRoundRect(px - tw / 2 + TILE_SIZE / 2.0, py - 12, tw, 11, 3, 3);
                gc.setFill(Color.WHITE);
                gc.fillText(label, px - tw / 2 + TILE_SIZE / 2.0 + 2, py - 3);
            }
        }
    }

    /**
     * Returns a short label for a stop based on its nearby city or industry.
     * @param stop The stop.
     * @return The label string, or null if no nearby entity.
     */
    private String stopLabel(Stop stop) {
        if (stop.getNearbyCity() != null) return stop.getNearbyCity().getName();
        if (stop.getNearbyIndustry() != null) return stop.getNearbyIndustry().getName();
        return null;
    }

    /**
     * Renders large name labels on top of cities and industries so the player
     * can easily identify locations while scrolling the map.
     * Detects the top-left corner of each entity cluster to avoid duplicate labels.
     * @param gc The graphics context.
     */
    private void renderEntityLabels(GraphicsContext gc) {
        java.util.Set<String> rendered = new java.util.HashSet<>();
        for (int y = 0; y < GameMap.HEIGHT; y++) {
            for (int x = 0; x < GameMap.WIDTH; x++) {
                Tile tile = map.getTile(x, y);
                String name = null;
                int areaW = 0, areaH = 0;
                Color bgColor = null;
                Color borderColor = null;
                if (tile instanceof CityTile c) {
                    name = c.getCityName();
                    areaW = 3; areaH = 3;
                    bgColor = Color.rgb(180, 120, 20, 0.9);
                    borderColor = Color.rgb(255, 200, 50);
                } else if (tile instanceof IndustryTile ind) {
                    name = ind.getIndustryName();
                    areaW = 2; areaH = 2;
                    bgColor = Color.rgb(120, 40, 160, 0.9);
                    borderColor = Color.rgb(200, 100, 255);
                }
                if (name == null || rendered.contains(name)) continue;
                Tile left = x > 0 ? map.getTile(x - 1, y) : null;
                Tile above = y > 0 ? map.getTile(x, y - 1) : null;
                if (left != null && isSameEntity(left, name)) continue;
                if (above != null && isSameEntity(above, name)) continue;
                rendered.add(name);

                double centerX = (x + areaW / 2.0) * TILE_SIZE;
                double topY = y * TILE_SIZE;

                gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
                double textW = name.length() * 6.5 + 10;
                double textH = 15;
                double labelX = centerX - textW / 2;
                double labelY = topY - textH - 3;

                gc.setFill(bgColor);
                gc.fillRoundRect(labelX, labelY, textW, textH, 5, 5);
                gc.setStroke(borderColor);
                gc.setLineWidth(1.5);
                gc.strokeRoundRect(labelX, labelY, textW, textH, 5, 5);
                gc.setLineWidth(1);

                gc.setFill(Color.WHITE);
                gc.fillText(name, labelX + 5, labelY + 11);
            }
        }
    }

    /**
     * Checks if a tile belongs to the same named entity (city or industry).
     * @param tile The tile to check.
     * @param name The entity name to match.
     * @return True if the tile is part of the same entity.
     */
    private boolean isSameEntity(Tile tile, String name) {
        if (tile instanceof CityTile c) return name.equals(c.getCityName());
        if (tile instanceof IndustryTile i) return name.equals(i.getIndustryName());
        return false;
    }

    /**
     * Renders traffic light indicators on the map.
     * @param gc The graphics context.
     */
    private void renderTrafficLights(GraphicsContext gc) {
        for (TrafficLight tl : trafficLights) {
            RoadTile loc = tl.getLocation();
            double px = loc.getX() * TILE_SIZE;
            double py = loc.getY() * TILE_SIZE;
            boolean nsGreen = tl.getCurrentState() == game.traffic.TrafficState.GREEN;
            gc.setFill(nsGreen ? Color.rgb(0, 200, 0, 0.6) : Color.rgb(200, 0, 0, 0.6));
            gc.fillOval(px + 1, py + 1, 6, 6);
            gc.setFill(!nsGreen ? Color.rgb(0, 200, 0, 0.6) : Color.rgb(200, 0, 0, 0.6));
            gc.fillOval(px + TILE_SIZE - 7, py + TILE_SIZE - 7, 6, 6);
        }
    }

    /**
     * Renders route path overlay showing stop connections with route names.
     * @param gc The graphics context.
     */
    private void renderRouteOverlay(GraphicsContext gc) {
        for (int i = 0; i < routes.size(); i++) {
            Route route = routes.get(i);
            List<Stop> stops = route.getStops();
            if (stops.size() < 2) continue;
            Color color = routeColor(i);
            gc.setStroke(color);
            gc.setLineWidth(2);
            gc.setGlobalAlpha(0.5);
            for (int j = 0; j < stops.size(); j++) {
                Stop a = stops.get(j);
                Stop b = stops.get((j + 1) % stops.size());
                double ax = a.getX() * TILE_SIZE + TILE_SIZE / 2.0;
                double ay = a.getY() * TILE_SIZE + TILE_SIZE / 2.0;
                double bx = b.getX() * TILE_SIZE + TILE_SIZE / 2.0;
                double by = b.getY() * TILE_SIZE + TILE_SIZE / 2.0;
                gc.strokeLine(ax, ay, bx, by);
            }
            gc.setGlobalAlpha(1.0);
            gc.setLineWidth(1);

            Stop first = stops.get(0);
            String label = "R" + route.getId() + ": " + route.getName();
            double lx = first.getX() * TILE_SIZE;
            double ly = first.getY() * TILE_SIZE - 22;
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 9));
            double tw = label.length() * 5.0 + 6;
            gc.setFill(Color.rgb(0, 0, 0, 0.75));
            gc.fillRoundRect(lx, ly, tw, 12, 3, 3);
            gc.setStroke(color);
            gc.setLineWidth(1);
            gc.strokeRoundRect(lx, ly, tw, 12, 3, 3);
            gc.setFill(Color.WHITE);
            gc.fillText(label, lx + 3, ly + 9);
        }
    }

    /**
     * Renders the build mode hover highlight.
     * @param gc The graphics context.
     */
    private void renderBuildHighlight(GraphicsContext gc) {
        double px = hoverX * TILE_SIZE;
        double py = hoverY * TILE_SIZE;

        boolean valid = switch (buildMode) {
            case "road" -> map.isValidBuildSite(hoverX, hoverY);
            case "stop" -> map.isValidStopSite(hoverX, hoverY);
            case "bridge" -> map.isValidBridgeEndpoint(hoverX, hoverY);
            case "placeVehicle" -> isRoadNearEntity(hoverX, hoverY);
            case "trafficLight" -> isValidTrafficLightSite(hoverX, hoverY);
            case "city" -> map.canPlaceEntity(hoverX, hoverY, 3, 3);        // NEW
            case "industry" -> map.canPlaceEntity(hoverX, hoverY, 2, 2);   // NEW
            case "cutTree" -> map.getTile(hoverX, hoverY) instanceof ForestTile; // NEW
            default -> false;
        };

        gc.setFill(valid ? Color.rgb(0, 255, 0, 0.3) : Color.rgb(255, 0, 0, 0.3));
        gc.fillRect(px, py, TILE_SIZE, TILE_SIZE);
        gc.setStroke(valid ? Color.LIME : Color.RED);
        gc.setLineWidth(2);
        gc.strokeRect(px, py, TILE_SIZE, TILE_SIZE);
        gc.setLineWidth(1);
    }

    /**
     * Checks if there is a stop at the given tile coordinates.
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return True if a stop exists at this position.
     */
    private boolean isStopAt(int x, int y) {
        for (Stop s : map.getStops()) {
            if (s.getX() == x && s.getY() == y) return true;
        }
        return false;
    }

    /**
     * Checks if a tile is a road adjacent to a city or industry (for vehicle placement).
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return True if valid vehicle placement site.
     */
    private boolean isRoadNearEntity(int x, int y) {
        game.tile.Tile tile = map.getTile(x, y);
        if (!(tile instanceof game.tile.RoadTile)) return false;
        if (map.isCityInternalRoad(x, y)) return true;
        for (game.tile.Tile t : map.getNeighbors(x, y)) {
            if (t.getType() == game.tile.Tile.TileType.CITY || t.getType() == game.tile.Tile.TileType.INDUSTRY)
                return true;
        }
        return false;
    }

    /**
     * Checks if a tile is a valid traffic light installation site (3+ way road junction).
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return True if valid.
     */
    private boolean isValidTrafficLightSite(int x, int y) {
        game.tile.Tile tile = map.getTile(x, y);
        return tile instanceof game.tile.RoadTile && map.countRoadNeighbors(x, y) >= 3;
    }

    /**
     * Returns the color for a given tile type with distinct textures.
     * @param tile The tile to get the color for.
     * @return The fill color.
     */
    private Color tileColor(Tile tile) {
        return switch (tile.getType()) {
            case GRASS    -> Color.rgb(100, 180, 60);
            case ROAD     -> Color.rgb(90, 90, 90);
            case FOREST   -> forestColor((ForestTile) tile);
            case WATER    -> Color.rgb(50, 120, 220);
            case CITY     -> Color.rgb(240, 170, 50);
            case INDUSTRY -> Color.rgb(180, 80, 200);
        };
    }

    /**
     * Renders bridge overlays with color-coded type.
     * @param gc The graphics context.
     */
    private void renderBridges(GraphicsContext gc) {
        for (game.map.Bridge bridge : map.getBridges()) {
            Color bridgeColor = switch (bridge.getType()) {
                case WOODEN -> Color.rgb(139, 90, 43, 0.6);
                case STONE  -> Color.rgb(160, 160, 160, 0.6);
                case STEEL  -> Color.rgb(70, 130, 180, 0.6);
            };
            for (game.tile.Tile t : bridge.getTiles()) {
                double px = t.getX() * TILE_SIZE;
                double py = t.getY() * TILE_SIZE;
                gc.setFill(bridgeColor);
                gc.fillRect(px + 1, py + 1, TILE_SIZE - 2, TILE_SIZE - 2);
                gc.setStroke(bridgeColor.darker());
                gc.strokeRect(px + 1, py + 1, TILE_SIZE - 2, TILE_SIZE - 2);
            }
        }
    }

    /**
     * Returns a green shade based on tree count for forest tiles.
     * @param forest The forest tile.
     * @return The forest color.
     */
    private Color forestColor(ForestTile forest) {
        int shade = 40 + (forest.getTreeCount() * 18);
        return Color.rgb(20, Math.min(shade + 50, 130), 20);
    }

    /**
     * Sets the camera offset for viewport scrolling.
     * @param x Camera x offset in pixels.
     * @param y Camera y offset in pixels.
     */
    public void setCamera(double x, double y) { this.camX = x; this.camY = y; }

    /** @return Camera x offset. */
    public double getCamX() { return camX; }

    /** @return Camera y offset. */
    public double getCamY() { return camY; }

    /**
     * Resizes the canvas to match the given viewport dimensions.
     * @param w viewport width
     * @param h viewport height
     */
    public void resizeCanvas(double w, double h) {
        canvas.setWidth(w);
        canvas.setHeight(h);
    }

    /**
     * Converts canvas pixel coordinates to tile grid coordinates, accounting for camera offset and zoom.
     * @param canvasX The x pixel position on the canvas.
     * @param canvasY The y pixel position on the canvas.
     * @param zoom The current zoom level.
     * @return An int array {tileX, tileY}, or null if out of bounds.
     */
    public int[] tileAt(double canvasX, double canvasY, double zoom) {
        int tx = (int) ((canvasX / zoom + camX) / TILE_SIZE);
        int ty = (int) ((canvasY / zoom + camY) / TILE_SIZE);
        if (map.inBounds(tx, ty)) return new int[]{tx, ty};
        return null;
    }

    /**
     * Converts canvas pixel coordinates to tile grid coordinates (zoom=1.0).
     * @param canvasX The x pixel position on the canvas.
     * @param canvasY The y pixel position on the canvas.
     * @return An int array {tileX, tileY}, or null if out of bounds.
     */
    public int[] tileAt(double canvasX, double canvasY) {
        return tileAt(canvasX, canvasY, 1.0);
    }

    /**
     * Renders a scaled-down minimap on the given canvas.
     * @param miniCanvas The minimap canvas to render onto.
     * @param currentZoom
     */
    public void renderMinimap(Canvas miniCanvas, double currentZoom) {
        GraphicsContext gc = miniCanvas.getGraphicsContext2D();
        double scaleX = miniCanvas.getWidth() / GameMap.WIDTH;
        double scaleY = miniCanvas.getHeight() / GameMap.HEIGHT;

        for (int x = 0; x < GameMap.WIDTH; x++) {
            for (int y = 0; y < GameMap.HEIGHT; y++) {
                gc.setFill(tileColor(map.getTile(x, y)));
                gc.fillRect(x * scaleX, y * scaleY, Math.ceil(scaleX), Math.ceil(scaleY));
            }
        }

        gc.setFill(Color.RED);
        for (Stop stop : map.getStops()) {
            gc.fillRect(stop.getX() * scaleX, stop.getY() * scaleY,
                    Math.max(2, scaleX), Math.max(2, scaleY));
        }

        for (Vehicle v : vehicles) {
            if (v.getCurrentTile() != null) {
                gc.setFill(v instanceof Truck ? Color.CYAN : Color.YELLOW);
                gc.fillRect(v.getCurrentTile().getX() * scaleX, v.getCurrentTile().getY() * scaleY,
                        Math.max(2, scaleX), Math.max(2, scaleY));
            }
        }
        // --- NEW: Viewport Frame ---
        double viewW = (canvas.getWidth() / currentZoom) / TILE_SIZE * scaleX;
        double viewH = (canvas.getHeight() / currentZoom) / TILE_SIZE * scaleY;
        double viewX = (camX / TILE_SIZE) * scaleX;
        double viewY = (camY / TILE_SIZE) * scaleY;
        
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.5);
        gc.strokeRect(viewX, viewY, viewW, viewH);
    }
}
