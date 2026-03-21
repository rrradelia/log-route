package game;

import game.city.City;
import game.controller.GameController;
import game.economy.Economy;
import game.goods.GoodType;
import game.industry.*;
import game.map.Bridge;
import game.map.GameMap;
import game.map.MapGenerator;
import game.tile.*;
import game.traffic.TrafficLight;
import game.transport.Route;
import game.transport.Stop;
import game.ui.MapRenderer;
import game.util.BridgeType;
import game.util.SimSpeed;
import game.vehicle.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Main application class for Mini Transport Tycoon.
 * Initialises the game map, creates domain objects (cities, industries) from
 * procedural generation, wires them into the {@link GameController} simulation loop,
 * and builds the full JavaFX GUI with HUD, scrollable map, sidebar, and minimap.
 */
public class Main extends Application {

    private GameMap map;
    private MapRenderer renderer;
    private GameController gameController;

    private Label tileInfoLabel;
    private Label costLabel;
    private VBox stopListBox;
    private VBox economyInfoBox;
    private Canvas minimapCanvas;

    private double dragStartX, dragStartY;
    private double dragCamX, dragCamY;
    private boolean dragging = false;
    private double zoomLevel = 1.0;
    private static final double ZOOM_MIN = 0.5;
    private static final double ZOOM_MAX = 3.0;
    private static final double ZOOM_STEP = 0.15;

    private int nextVehicleId = 1;
    private Vehicle pendingVehicle = null;
    private String activeVehicleCard = null;
    private String pendingVehicleType = null;
    private double pendingVehicleCost = 0;
    private Stage primaryStage;
    private int nextCityId = 1;
    private String pendingCityName = null;
    private String pendingIndustryType = null;
    
    // Strict Monochrome & Gray Wireframe Styling Constants
    private final String UI_BG = "-fx-background-color: #e0e0e0;"; // Light gray for panels
    private final String UI_BORDER = "-fx-border-color: black; -fx-border-width: 1;";
    private final String BTN_NORMAL = "-fx-background-color: #d4d4d4; -fx-border-color: black; -fx-border-width: 1; -fx-text-fill: black; -fx-font-weight: bold;";
    private final String BTN_ACTIVE = "-fx-background-color: #555555; -fx-border-color: black; -fx-border-width: 1; -fx-text-fill: white; -fx-font-weight: bold;";
    private final String HEADER_STYLE = "-fx-border-color: black; -fx-border-width: 1; -fx-padding: 3; -fx-background-color: #cccccc;"; // Darker gray for headers
    // Dynamic UI Containers
    private VBox vehiclesBox = new VBox(2);
    private VBox routesBox = new VBox(2);
    private VBox industriesBox = new VBox(2);

    // Build Tool Tracking
    private java.util.List<Button> buildButtons = new java.util.ArrayList<>();
    private Button activeBuildButton = null;

    /**
     * Initializes the primary stage and shows the start screen.
     * @param stage The primary stage.
     */
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Log Route");
        showStartScreen();
        stage.show();
    }

    /**
     * Displays a minimal start screen with game title, Start Game, and Quit buttons.
     */
    private void showStartScreen() {
        VBox root = new VBox(18);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20, 40, 20, 40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e, #0f3460);");

        Label title = new Label("\ud83d\ude9b Mini Transport Tycoon");
        title.setFont(Font.font("Arial", 38));
        title.setTextFill(Color.WHITE);

        Label subtitle = new Label("Build roads \u00b7 Transport goods \u00b7 Grow your empire");
        subtitle.setFont(Font.font("Arial", 14));
        subtitle.setTextFill(Color.rgb(180, 200, 220));

        Button startBtn = new Button("\u25b6  Start Game");
        startBtn.setPrefWidth(240);
        startBtn.setPrefHeight(50);
        startBtn.setFont(Font.font("Arial", 20));
        startBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        startBtn.setOnMouseEntered(e -> startBtn.setStyle("-fx-background-color: #66BB6A; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;"));
        startBtn.setOnMouseExited(e -> startBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;"));
        startBtn.setOnAction(e -> launchGame());

        Button quitBtn = new Button("\u2716  Quit");
        quitBtn.setPrefWidth(240);
        quitBtn.setPrefHeight(38);
        quitBtn.setFont(Font.font("Arial", 14));
        quitBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #999; -fx-border-color: #555; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;");
        quitBtn.setOnAction(e -> Platform.exit());

        Label credits = new Label("Group 4, Team 1 \u2014 Adelia \u00b7 Kutmanbek \u00b7 Zuhriddin");
        credits.setFont(Font.font(11));
        credits.setTextFill(Color.rgb(120, 140, 160));

        Region spacerTop = new Region();
        Region spacerBottom = new Region();
        VBox.setVgrow(spacerTop, Priority.ALWAYS);
        VBox.setVgrow(spacerBottom, Priority.ALWAYS);

        root.getChildren().addAll(spacerTop, title, subtitle, startBtn, quitBtn, spacerBottom, credits);
        primaryStage.setScene(new Scene(root, 1100, 750));
    }

    /**
     * Creates a legend row with a colored shape symbol and description.
     * @param color The tile/entity color matching MapRenderer.
     * @param symbol The shape character.
     * @param text The description.
     * @return The legend row HBox.
     */
    private HBox legendRow(Color color, String symbol, String text) {
        Label sym = new Label(symbol);
        sym.setFont(Font.font("Arial", 14));
        sym.setTextFill(color);
        sym.setMinWidth(18);
        Label desc = new Label(text);
        desc.setFont(Font.font(11));
        desc.setTextFill(Color.rgb(210, 220, 230));
        HBox row = new HBox(6, sym, desc);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /**
     * Creates a styled rule label for the start screen.
     * @param text The rule text.
     * @return The styled Label.
     */
    private Label ruleLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font(11));
        l.setTextFill(Color.rgb(200, 210, 225));
        return l;
    }

    /**
     * Tears down any running game (render loop, controller) before starting fresh.
     */
    private void tearDownGame() {
        if (renderLoop != null) { renderLoop.stop(); renderLoop = null; }
        if (gameController != null) { gameController.stopGame(); gameController = null; }
        pendingVehicle = null;
        activeVehicleCard = null;
        nextVehicleId = 1;
        zoomLevel = 1.0;
    }

    /**
     * Initializes the game world, builds the game UI, and starts the simulation.
     */
    private void launchGame() {
        tearDownGame();

        gameController = new GameController();

        map = new GameMap();
        map.generate();
        gameController.setMap(map);

        gameController.setOnBankruptCallback(() -> Platform.runLater(this::showGameOverDialog));
        gameController.setOnRestartCallback(() -> {
            map = new GameMap();
            map.generate();
            gameController.setMap(map);
            nextVehicleId = 1;
            nextCityId = 1;
            pendingVehicle = null;
            activeVehicleCard = null;
            pendingVehicleType = null;
            pendingIndustryType = null;
            renderer.setMap(map);
            renderer.setVehicles(gameController.getVehicles());
            renderer.setTrafficLights(gameController.getTrafficLights());
            renderer.setRoutes(gameController.getRoutes());
            renderer.render(zoomLevel);
            refreshStopList();
            refreshEconomyInfo();
            updateMinimap();
        });

        renderer = new MapRenderer(map);
        renderer.setVehicles(gameController.getVehicles());
        renderer.setTrafficLights(gameController.getTrafficLights());
        renderer.setRoutes(gameController.getRoutes());
        renderer.render(zoomLevel);

        startRenderLoop();

        BorderPane root = new BorderPane();
        root.setTop(createHUD());
        root.setCenter(createMapArea());
        root.setRight(createSidebar());

        primaryStage.setScene(new Scene(root, 1100, 750));

        updateMinimap();
        gameController.startGame();
    }

    /**
     * Returns to the start screen, tearing down the current game.
     */
    private void returnToStartScreen() {
        tearDownGame();
        showStartScreen();
    }

    @Override
    public void stop() {
        if (renderLoop != null) renderLoop.stop();
        if (gameController != null) gameController.stopGame();
    }

    private Timeline renderLoop;

    /**
     * Starts a periodic render/UI refresh loop (4 FPS) to keep vehicles, traffic lights,
     * economy info, and minimap in sync with the simulation.
     * Also advances vehicle interpolation for smooth continuous movement.
     */
    private void startRenderLoop() {
        renderLoop = new Timeline(new KeyFrame(Duration.millis(250), e -> {
            if (gameController.isGameOver()) return;
            for (Vehicle v : gameController.getVehicles()) v.advanceInterpolation(0.5);
            renderer.setVehicles(gameController.getVehicles());
            renderer.setTrafficLights(gameController.getTrafficLights());
            renderer.setRoutes(gameController.getRoutes());
            renderer.render(zoomLevel);
            refreshEconomyInfo();
            updateMinimap();
        }));
        renderLoop.setCycleCount(Timeline.INDEFINITE);
        renderLoop.play();
    }

    /**
     * Creates City and Industry domain objects from the map generator's placement data
     * and registers them with the GameController for tick updates.
     * Currently a no-op since the map starts empty — cities/industries are player-placed.
     */
    private void createDomainObjects() {}

    /**
     * Creates the top HUD bar with capital (bound to Economy), day label, and speed controls.
     * @return The HUD layout.
     */
    private HBox createHUD() {
        HBox hud = new HBox();
        hud.setAlignment(Pos.CENTER);
        hud.setStyle(UI_BG + "-fx-border-color: black; -fx-border-width: 0 0 1 0;");

        Button menuBtn = new Button("Menu");
        menuBtn.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 0 1 0 0; -fx-padding: 5 15;");
        menuBtn.setOnAction(e -> showMenuPopup());

        Label capitalLabel = new Label();
        capitalLabel.textProperty().bind(gameController.getEconomy().capitalProperty().asString("$%,.0f"));
        capitalLabel.setAlignment(Pos.CENTER);
        capitalLabel.setPrefWidth(120);
        capitalLabel.setStyle("-fx-border-color: black; -fx-border-width: 0 1 0 0; -fx-padding: 5;");

        Label dateLabel = new Label();
        dateLabel.textProperty().bind(gameController.dayLabelProperty());
        dateLabel.setAlignment(Pos.CENTER);
        dateLabel.setPrefWidth(150);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox speedBox = new HBox();
        speedBox.setStyle("-fx-border-color: black; -fx-border-width: 0 0 0 1;");

        Button pauseBtn = new Button("II");
        pauseBtn.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 0 1 0 0; -fx-font-weight: bold;");
        pauseBtn.setOnAction(e -> gameController.setSimSpeed(game.util.SimSpeed.PAUSED));

        Button speed1xBtn = new Button("1x");
        speed1xBtn.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 0 1 0 0;");
        speed1xBtn.setOnAction(e -> gameController.setSimSpeed(game.util.SimSpeed.NORMAL));

        Button speed2xBtn = new Button("2x");
        speed2xBtn.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 0 1 0 0;");
        speed2xBtn.setOnAction(e -> gameController.setSimSpeed(game.util.SimSpeed.FAST_2X));

        Button speed4xBtn = new Button("4x");
        speed4xBtn.setStyle("-fx-background-color: white;");
        speed4xBtn.setOnAction(e -> gameController.setSimSpeed(game.util.SimSpeed.VERY_FAST_4X));

        speedBox.getChildren().addAll(pauseBtn, speed1xBtn, speed2xBtn, speed4xBtn);

        hud.getChildren().addAll(menuBtn, capitalLabel, dateLabel, spacer, speedBox);
        return hud;
    }

    /**
     * Creates the center map area with drag-scroll, minimap, and info overlays.
     * @return The map area layout.
     */
    private StackPane createMapArea() {
        Canvas canvas = renderer.getCanvas();
        Pane mapPane = new Pane(canvas);
        mapPane.setStyle("-fx-background-color: white;"); // Match wireframe background

        // Wire up resizing/rendering hooks as you had them...
        mapPane.widthProperty().addListener((o, ov, nv) -> { renderer.resizeCanvas(nv.doubleValue(), mapPane.getHeight()); renderer.render(zoomLevel); });
        mapPane.heightProperty().addListener((o, ov, nv) -> { renderer.resizeCanvas(mapPane.getWidth(), nv.doubleValue()); renderer.render(zoomLevel); });
        setupMapInteraction(mapPane);

        // --- Minimap (Bottom Left) ---
        minimapCanvas = new Canvas(120, 120);
        VBox minimapWrapper = new VBox(new Label("Minimap"), minimapCanvas);
        minimapWrapper.setAlignment(Pos.TOP_CENTER);
        minimapWrapper.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 1; -fx-padding: 2;");
        minimapWrapper.setMaxSize(126, 140);

        // --- Traffic Light Control (Bottom Right) ---
        VBox trafficControl = new VBox(5);
        trafficControl.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 1; -fx-padding: 5;");
        trafficControl.setMaxSize(180, 100);

        Label tcTitle = new Label("Traffic Light Control");
        tcTitle.setStyle("-fx-border-color: black; -fx-border-width: 0 0 1 0;");
        tcTitle.setMaxWidth(Double.MAX_VALUE);

        HBox nsBox = new HBox(10, new Label("N-S Greed Time"), createSpacer(), new Label("30s"));
        HBox ewBox = new HBox(10, new Label("E-W Greed Time"), createSpacer(), new Label("15s"));

        Button applyBtn = new Button("Apply");
        applyBtn.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: black; -fx-border-width: 1;");
        applyBtn.setMaxWidth(Double.MAX_VALUE);

        trafficControl.getChildren().addAll(tcTitle, nsBox, ewBox, applyBtn);

        // Stack them over the map
        StackPane stack = new StackPane(mapPane, minimapWrapper, trafficControl);

        StackPane.setAlignment(minimapWrapper, Pos.BOTTOM_LEFT);
        StackPane.setMargin(minimapWrapper, new Insets(0, 0, 20, 20));

        StackPane.setAlignment(trafficControl, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(trafficControl, new Insets(0, 20, 20, 0));

        return stack;
    }
    
    private Region createSpacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }
    
    /**
     * Sets up drag-and-drop vehicle placement on the map canvas.
     * @param canvas The map canvas.
     */
    private void setupVehicleDrop(Canvas canvas) {
        canvas.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) e.acceptTransferModes(javafx.scene.input.TransferMode.MOVE);
            e.consume();
        });
        canvas.setOnDragDropped(e -> {
            String data = e.getDragboard().getString();
            if (data == null || !data.contains("|")) return;
            String[] parts = data.split("\\|");
            String vName = parts[0];
            double vCost = Double.parseDouble(parts[1]);
            int[] pos = renderer.tileAt(e.getX(), e.getY(), zoomLevel);
            if (pos == null) return;
            Tile tile = map.getTile(pos[0], pos[1]);
            if (!(tile instanceof RoadTile road) || !isNearCityOrIndustry(pos[0], pos[1])) {
                showCostError("Must drop on a road near a city or industry!");
                e.setDropCompleted(false);
                e.consume();
                return;
            }
            if (gameController.getEconomy().getCapital() < vCost) {
                showCostError("Not enough money! Need $" + String.format("%,.0f", vCost));
                e.setDropCompleted(false);
                e.consume();
                return;
            }
            int vid = nextVehicleId++;
            Vehicle vehicle;
            if (vName.contains("Truck")) {
                GoodType goodType = pickGoodType();
                if (goodType == null) { nextVehicleId--; e.setDropCompleted(false); e.consume(); return; }
                vehicle = vName.equals("Small Truck") ? new SmallTruck(vid, goodType) : new LargeTruck(vid, goodType);
            } else {
                vehicle = vName.equals("Small Bus") ? new SmallBus(vid) : new BigBus(vid);
            }
            gameController.getEconomy().spend(vCost);
            gameController.getEconomy().recordVehiclePurchase(vCost);
            gameController.addVehicle(vehicle);
            vehicle.setCurrentTile(road);
            renderer.setVehicles(gameController.getVehicles());
            renderer.render(zoomLevel);
            refreshEconomyInfo();
            costLabel.setText(vName + " placed!");
            costLabel.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: #0f0; -fx-padding: 4 8;");
            costLabel.setVisible(true);
            e.setDropCompleted(true);
            e.consume();
        });
    }

    /**
     * Sets up mouse drag-scroll, click-to-build, scroll-to-zoom, and hover events on the map.
     * @param mapPane The pane containing the map canvas.
     */
    private void setupMapInteraction(Pane mapPane) {
        Canvas canvas = renderer.getCanvas();
        canvas.setOnMousePressed(e -> {
            dragStartX = e.getScreenX();
            dragStartY = e.getScreenY();
            dragCamX = renderer.getCamX();
            dragCamY = renderer.getCamY();
            dragging = false;
        });
        canvas.setOnMouseDragged(e -> {
            double dx = e.getScreenX() - dragStartX;
            double dy = e.getScreenY() - dragStartY;
            if (Math.abs(dx) > 3 || Math.abs(dy) > 3) dragging = true;
            double newCamX = dragCamX - dx / zoomLevel;
            double newCamY = dragCamY - dy / zoomLevel;
            clampCamera(newCamX, newCamY);
            renderer.render(zoomLevel);
        });
        canvas.setOnMouseReleased(e -> {
            if (!dragging && renderer.getBuildMode() != null) {
                int[] pos = renderer.tileAt(e.getX(), e.getY(), zoomLevel);
                if (pos != null) handleBuildClick(pos[0], pos[1]);
            }
        });
        canvas.setOnMouseMoved(e -> {
            int[] pos = renderer.tileAt(e.getX(), e.getY(), zoomLevel);
            if (pos != null) {
                renderer.setHover(pos[0], pos[1]);
                updateTileInfo(pos[0], pos[1]);
            } else {
                renderer.clearHover();
                tileInfoLabel.setVisible(false);
                costLabel.setVisible(false);
            }
            renderer.setVehicles(gameController.getVehicles());
            renderer.render(zoomLevel);
        });
        canvas.setOnScroll(e -> {
            double oldZoom = zoomLevel;
            if (e.getDeltaY() > 0) zoomLevel = Math.min(ZOOM_MAX, zoomLevel + ZOOM_STEP);
            else zoomLevel = Math.max(ZOOM_MIN, zoomLevel - ZOOM_STEP);
            double mouseWorldX = e.getX() / oldZoom + renderer.getCamX();
            double mouseWorldY = e.getY() / oldZoom + renderer.getCamY();
            double newCamX = mouseWorldX - e.getX() / zoomLevel;
            double newCamY = mouseWorldY - e.getY() / zoomLevel;
            clampCamera(newCamX, newCamY);
            renderer.render(zoomLevel);
        });
    }

    /**
     * Clamps the camera so the viewport stays within the map bounds.
     * @param cx Desired camera x.
     * @param cy Desired camera y.
     */
    private void clampCamera(double cx, double cy) {
        double mapW = GameMap.WIDTH * MapRenderer.TILE_SIZE;
        double mapH = GameMap.HEIGHT * MapRenderer.TILE_SIZE;
        double vw = renderer.getCanvas().getWidth() / zoomLevel;
        double vh = renderer.getCanvas().getHeight() / zoomLevel;
        cx = Math.max(0, Math.min(cx, mapW - vw));
        cy = Math.max(0, Math.min(cy, mapH - vh));
        renderer.setCamera(cx, cy);
    }

    /**
     * Sets up click-to-navigate on the minimap, accounting for zoom level.
     * @param minimapWrapper The minimap wrapper pane.
     * @param mapPane The main map pane.
     */
    private void setupMinimapClick(StackPane minimapWrapper, Pane mapPane) {
        minimapCanvas.setOnMouseClicked(e -> {
            double ratioX = e.getX() / minimapCanvas.getWidth();
            double ratioY = e.getY() / minimapCanvas.getHeight();
            double vw = renderer.getCanvas().getWidth() / zoomLevel;
            double vh = renderer.getCanvas().getHeight() / zoomLevel;
            double cx = ratioX * GameMap.WIDTH * MapRenderer.TILE_SIZE - vw / 2;
            double cy = ratioY * GameMap.HEIGHT * MapRenderer.TILE_SIZE - vh / 2;
            clampCamera(cx, cy);
            renderer.render(zoomLevel);
        });
    }

    /**
     * Creates the right sidebar with stops, economy info, build buttons, and vehicle purchase.
     * @return The sidebar layout.
     */
    private VBox createSidebar() {
        VBox sidebar = new VBox(5);
        sidebar.setPadding(new Insets(10));
        sidebar.setPrefWidth(250);
        sidebar.setStyle(UI_BG + "-fx-border-color: black; -fx-border-width: 0 0 0 1;");

        // --- Dynamic Lists Sections ---
        HBox vehiclesHeader = createSectionHeader("Vehicles", this::showAddVehicleDialog);
        ScrollPane vScroll = new ScrollPane(vehiclesBox);
        vScroll.setPrefHeight(100); vScroll.setStyle("-fx-background-color: white; -fx-background: white;");

        HBox routesHeader = createSectionHeader("Routes", () -> showCreateRouteDialog());
        ScrollPane rScroll = new ScrollPane(routesBox);
        rScroll.setPrefHeight(100); rScroll.setStyle("-fx-background-color: white; -fx-background: white;");

        HBox industriesHeader = createSectionHeader("Industries", () -> {});
        ScrollPane iScroll = new ScrollPane(industriesBox);
        iScroll.setPrefHeight(100); iScroll.setStyle("-fx-background-color: white; -fx-background: white;");

        // --- Build Action Buttons ---
        Label buildLabel = new Label("Build Tools");
        buildLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5 0 0 0;");

        GridPane buildGrid = new GridPane();
        buildGrid.setHgap(5);
        buildGrid.setVgap(5);

        buildGrid.add(createBuildButton("Road", "road"), 0, 0);
        buildGrid.add(createBuildButton("Stop", "stop"), 1, 0);
        buildGrid.add(createBuildButton("Light", "trafficLight"), 0, 1);
        buildGrid.add(createBuildButton("Bridge", "bridge"), 1, 1);
        buildGrid.add(createBuildButton("City", "city"), 0, 2);
        buildGrid.add(createBuildButton("Industry", "industry"), 1, 2);

        sidebar.getChildren().addAll(
            vehiclesHeader, vScroll,
            routesHeader, rScroll,
            industriesHeader, iScroll,
            new Region(), // spacer
            buildLabel, buildGrid
        );

        return sidebar;
    }   
    
    
    // Helper for build buttons
    private Button createBuildButton(String text, String mode) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(btn, Priority.ALWAYS);
        btn.setStyle(BTN_NORMAL);
        buildButtons.add(btn); // Track it

        btn.setOnAction(e -> {
            // If clicking the already active button, deselect it
            if (activeBuildButton == btn) {
                btn.setStyle(BTN_NORMAL);
                activeBuildButton = null;
                renderer.setBuildMode(null);
                return;
            }

            // Deselect previous button, select new one
            if (activeBuildButton != null) {
                activeBuildButton.setStyle(BTN_NORMAL);
            }
            activeBuildButton = btn;
            btn.setStyle(BTN_ACTIVE);

            // Handle specific popups for Industry/City
            if (mode.equals("industry")) {
                ChoiceDialog<String> typeDialog = new ChoiceDialog<>("Iron Mine", "Iron Mine", "Wood Farm", "Paper Mill");
                typeDialog.setTitle("Industry");
                typeDialog.setHeaderText(null);
                typeDialog.showAndWait().ifPresentOrElse(type -> {
                    pendingIndustryType = type;
                    renderer.setBuildMode("industry");
                }, () -> resetBuildButtons()); // Deselect if canceled

            } else if (mode.equals("city")) {
                TextInputDialog cityDialog = new TextInputDialog("New City");
                cityDialog.setTitle("City");
                cityDialog.setHeaderText(null);
                cityDialog.showAndWait().ifPresentOrElse(name -> {
                    pendingCityName = name;
                    renderer.setBuildMode("city");
                }, () -> resetBuildButtons()); // Deselect if canceled

            } else {
                renderer.setBuildMode(mode);
            }
        });
        return btn;
    }
    
    // Helper to reset all buttons (call this when player presses ESC to cancel building)
    private void resetBuildButtons() {
        for (Button b : buildButtons) b.setStyle(BTN_NORMAL);
        activeBuildButton = null;
        renderer.setBuildMode(null);
    }

    private void showAddVehicleDialog() {
        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.UNDECORATED); 

        VBox root = new VBox(20);
        root.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 1; -fx-padding: 25;");

        HBox content = new HBox(40);
        content.setAlignment(Pos.CENTER);

        VBox left = new VBox(15);
        CheckBox smallTruckCb = new CheckBox("Small Truck");
        CheckBox largeTruckCb = new CheckBox("Large Truck");
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().add("Category");
        categoryBox.getSelectionModel().selectFirst();
        categoryBox.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-radius: 0;");
        left.getChildren().addAll(smallTruckCb, largeTruckCb, categoryBox);

        Separator sep = new Separator(javafx.geometry.Orientation.VERTICAL);

        VBox right = new VBox(15);
        CheckBox smallBusCb = new CheckBox("Small Bus");
        smallBusCb.setSelected(true); // Checked in the mockup
        CheckBox bigBusCb = new CheckBox("Big Bus");
        right.getChildren().addAll(smallBusCb, bigBusCb);

        content.getChildren().addAll(left, sep, right);

        VBox bottom = new VBox(10);
        bottom.setAlignment(Pos.CENTER);
        Label totalLabel = new Label("Total: $5000");
        Button buyBtn = new Button("Buy");
        buyBtn.setPrefWidth(120);
        buyBtn.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: black; -fx-border-width: 1; -fx-font-size: 14px;");
        buyBtn.setOnAction(e -> dialog.close());

        bottom.getChildren().addAll(totalLabel, buyBtn);
        root.getChildren().addAll(content, bottom);

        dialog.setScene(new Scene(root));
        dialog.show();
    }
    
    // Helpers to match the wireframe style perfectly
    private HBox createSectionHeader(String title, Runnable onAdd) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button plusBtn = new Button("+");
        Button minusBtn = new Button("-");
        plusBtn.setStyle("-fx-padding: 0 5; -fx-background-color: white; -fx-border-color: black; -fx-border-radius: 0;");
        minusBtn.setStyle("-fx-padding: 0 6; -fx-background-color: white; -fx-border-color: black; -fx-border-radius: 0;");
        plusBtn.setOnAction(e -> onAdd.run());

        HBox header = new HBox(5, titleLabel, spacer, plusBtn, minusBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(HEADER_STYLE);
        return header;
    }

    private Label createListItem(String text) {
        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-padding: 5;");
        return label;
    }

    private VBox createIndustryItem(String name, String capacity, String status) {
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 0 0 1 0;");
        nameLabel.setMaxWidth(Double.MAX_VALUE);

        Label capLabel = new Label(capacity);
        Label statusLabel = new Label(status);
        statusLabel.setStyle("-fx-border-color: black; -fx-border-width: 0 0 0 1; -fx-padding: 0 0 0 5;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottomRow = new HBox(capLabel, spacer, statusLabel);

        VBox item = new VBox(2, nameLabel, bottomRow);
        item.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-padding: 3;");
        return item;
    }

    private Button createActionButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-background-color: #f0f0f0; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 6;");
        return btn;
    }

    /**
     * Shows the in-game menu popup with Continue, Restart, and Quit options.
     */
    private void showMenuPopup() {
        Stage menuStage = new Stage();
        menuStage.initStyle(StageStyle.UNDECORATED);

        VBox root = new VBox(10);
        // Applying the exact same gray background and black border
        root.setStyle(UI_BG + "-fx-border-color: black; -fx-border-width: 2; -fx-padding: 20;");
        root.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Menu");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 0 0 10 0;");

        Button continueBtn = new Button("Continue");
        continueBtn.setMaxWidth(Double.MAX_VALUE);
        continueBtn.setStyle(BTN_NORMAL);
        continueBtn.setOnAction(e -> menuStage.close());

        Button restartBtn = new Button("Restart");
        restartBtn.setMaxWidth(Double.MAX_VALUE);
        restartBtn.setStyle(BTN_NORMAL);
        restartBtn.setOnAction(e -> {
            gameController.restartGame();
            menuStage.close();
        });

        // --- NEW: Main Menu Button ---
        Button mainMenuBtn = new Button("Main Menu");
        mainMenuBtn.setMaxWidth(Double.MAX_VALUE);
        mainMenuBtn.setStyle(BTN_NORMAL);
        mainMenuBtn.setOnAction(e -> {
            gameController.stopGame(); 
            menuStage.close();
            showMainMenu(); // Or whatever your method is called to show the start screen
        });

        Button quitBtn = new Button("Quit");
        quitBtn.setMaxWidth(Double.MAX_VALUE);
        quitBtn.setStyle(BTN_NORMAL);
        quitBtn.setOnAction(e -> javafx.application.Platform.exit());

        root.getChildren().addAll(titleLabel, continueBtn, restartBtn, mainMenuBtn, quitBtn);

        // Made the height slightly taller to fit the new button
        menuStage.setScene(new Scene(root, 200, 240)); 
        menuStage.show();
    }
    
    // Assuming this is the method that launches your game's first screen
    private void showMainMenu() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);

        // Apply the exact same light gray background as the rest of the game
        root.setStyle(UI_BG); 

        // Plain text, no emojis
        Label titleLabel = new Label("Logistics Route Simulator"); 
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: black;");

        // Start Button using our gray wireframe constants
        Button startBtn = new Button("Start Game");
        startBtn.setPrefWidth(200);
        startBtn.setStyle(BTN_NORMAL);

        // Add hover effects manually if you want the wireframe active state
        startBtn.setOnMouseEntered(e -> startBtn.setStyle(BTN_ACTIVE));
        startBtn.setOnMouseExited(e -> startBtn.setStyle(BTN_NORMAL));
        startBtn.setOnAction(e -> {
            // Your logic to switch scenes to the main game map goes here
            gameController.startGame(); 
        });

        // Exit Button using our gray wireframe constants
        Button exitBtn = new Button("Exit");
        exitBtn.setPrefWidth(200);
        exitBtn.setStyle(BTN_NORMAL);

        exitBtn.setOnMouseEntered(e -> exitBtn.setStyle(BTN_ACTIVE));
        exitBtn.setOnMouseExited(e -> exitBtn.setStyle(BTN_NORMAL));
        exitBtn.setOnAction(e -> javafx.application.Platform.exit());

        root.getChildren().addAll(titleLabel, startBtn, exitBtn);

        // Set the scene on your primary stage
        primaryStage.setScene(new Scene(root, 900, 700));
        primaryStage.show();
    }
    /**
     * Shows a game rules and guide dialog with map legend, costs, and gameplay instructions.
     */
    private void showGameRulesDialog() {
        Stage rulesStage = new Stage();
        rulesStage.initModality(Modality.APPLICATION_MODAL);
        rulesStage.initOwner(primaryStage);
        rulesStage.setTitle("Game Rules & Guide");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #1e1e2e;");

        Label heading = new Label("📖 Game Rules & Guide");
        heading.setFont(Font.font("Arial", 20));
        heading.setTextFill(Color.WHITE);

        HBox columns = new HBox(20);
        columns.setAlignment(Pos.TOP_CENTER);

        VBox legendBox = new VBox(5);
        legendBox.setPadding(new Insets(10));
        legendBox.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-background-radius: 8;");
        Label legendTitle = new Label("🗺 Map Legend");
        legendTitle.setFont(Font.font("Arial", 14));
        legendTitle.setTextFill(Color.rgb(200, 220, 255));
        legendBox.getChildren().addAll(legendTitle,
                legendRow(Color.rgb(100, 180, 60),  "■", "Grass — buildable terrain"),
                legendRow(Color.rgb(20, 90, 20),    "▲", "Forest — clearing costs extra"),
                legendRow(Color.rgb(50, 120, 220),  "●", "Water — needs bridge to cross"),
                legendRow(Color.rgb(90, 90, 90),    "━", "Road — $100 per tile"),
                legendRow(Color.rgb(240, 170, 50),  "■", "City (3×3) — accepts deliveries"),
                legendRow(Color.rgb(180, 80, 200),  "■", "Industry (2×2) — produces goods"),
                legendRow(Color.RED,                "●", "Stop — load/unload point"),
                legendRow(Color.rgb(0, 200, 255),   "◆", "Truck — transports cargo"),
                legendRow(Color.rgb(255, 220, 0),   "◆", "Bus — transports passengers")
        );

        VBox guideBox = new VBox(5);
        guideBox.setPadding(new Insets(10));
        guideBox.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-background-radius: 8;");
        Label guideTitle = new Label("🎮 How to Play");
        guideTitle.setFont(Font.font("Arial", 14));
        guideTitle.setTextFill(Color.rgb(200, 220, 255));
        guideBox.getChildren().addAll(guideTitle,
                ruleLabel("1. Build a City ($5,000) and an Industry ($3,000)"),
                ruleLabel("2. Build roads ($100/tile) connecting them"),
                ruleLabel("3. Place stops near cities and industries"),
                ruleLabel("4. Buy a vehicle (truck for cargo, bus for passengers)"),
                ruleLabel("5. Create a route with 2+ stops"),
                ruleLabel("6. Assign the vehicle to the route"),
                ruleLabel("7. Watch it deliver goods and earn money!"),
                ruleLabel("   ─── or use ⚡ Auto-Connect for steps 2-6 ───")
        );

        VBox costsBox = new VBox(5);
        costsBox.setPadding(new Insets(10));
        costsBox.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-background-radius: 8;");
        Label costsTitle = new Label("💰 Costs & Economy");
        costsTitle.setFont(Font.font("Arial", 14));
        costsTitle.setTextFill(Color.rgb(200, 220, 255));
        costsBox.getChildren().addAll(costsTitle,
                ruleLabel("💵 Starting capital: $50,000"),
                ruleLabel("🚨 Bankruptcy: capital < $0 = game over"),
                ruleLabel("🛣 Roads: $100/tile, forests cost extra"),
                ruleLabel("🌉 Bridges: Wooden $500, Stone $1,500, Steel $3,000"),
                ruleLabel("🏠 City: $5,000 (3×3 area)"),
                ruleLabel("🏭 Industry: $3,000 (2×2 area)"),
                ruleLabel("🚛 Small Truck: $5,000 | Large Truck: $12,000"),
                ruleLabel("🚌 Small Bus: $400 | Big Bus: $1,000"),
                ruleLabel("🔄 Routes are circular: A → B → C → A"),
                ruleLabel("🚦 Traffic lights at 3+ way junctions"),
                ruleLabel("⏱ Speed: Pause, 1x, 2x, 4x")
        );

        columns.getChildren().addAll(legendBox, guideBox, costsBox);

        Button closeBtn = new Button("Close");
        closeBtn.setPrefWidth(120);
        closeBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> rulesStage.close());

        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(heading, columns, closeBtn);

        rulesStage.setScene(new Scene(content, 750, 480));
        rulesStage.show();
    }

    /**
     * Creates a styled menu button.
     * @param text The button label.
     * @param color The background color.
     * @return The styled button.
     */
    private Button menuButton(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefWidth(200);
        btn.setPrefHeight(40);
        btn.setFont(Font.font(14));
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
        return btn;
    }

    /**
     * Shows the game over dialog after bankruptcy with Play Again and Quit options.
     */
    private void showGameOverDialog() {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(primaryStage);
        popup.initStyle(StageStyle.UNDECORATED);

        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30, 50, 30, 50));
        box.setStyle("-fx-background-color: #2c2c2c; -fx-background-radius: 10; -fx-border-color: #c62828; -fx-border-radius: 10; -fx-border-width: 2;");

        Label title = new Label("💀 Bankruptcy!");
        title.setFont(Font.font("Arial", 22));
        title.setTextFill(Color.rgb(220, 50, 50));

        Label info = new Label("You went bankrupt on Day " + gameController.getDayCount() + ".");
        info.setTextFill(Color.LIGHTGRAY);
        info.setFont(Font.font(14));

        Button playAgainBtn = menuButton("🔄 Play Again", "#4CAF50");
        playAgainBtn.setOnAction(e -> { popup.close(); launchGame(); });

        Button mainMenuBtn = menuButton("🏠 Main Menu", "#FF9800");
        mainMenuBtn.setOnAction(e -> { popup.close(); returnToStartScreen(); });

        Button quitBtn = menuButton("✖ Quit", "#f44336");
        quitBtn.setOnAction(e -> { popup.close(); Platform.exit(); });

        box.getChildren().addAll(title, info, playAgainBtn, mainMenuBtn, quitBtn);
        Scene scene = new Scene(box);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        popup.showAndWait();
    }

    /**
     * Creates inline vehicle purchase cards for the sidebar.
     * @return VBox containing the 4 vehicle cards and a Sell All button.
     */
    private HBox createVehicleCards() {
        ComboBox<String> vehicleBox = new ComboBox<>();
        vehicleBox.getItems().addAll(
            "Small Truck ($5,000)", "Large Truck ($12,000)", 
            "Small Bus ($400)", "Big Bus ($1,000)"
        );
        vehicleBox.getSelectionModel().selectFirst();
        vehicleBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(vehicleBox, Priority.ALWAYS);

        Button buyBtn = new Button("Buy");
        buyBtn.setOnAction(e -> {
            String selection = vehicleBox.getValue();
            String name = selection.substring(0, selection.indexOf(" ("));
            double cost = selection.contains("Large Truck") ? LargeTruck.COST :
                          selection.contains("Small Truck") ? SmallTruck.COST :
                          selection.contains("Big Bus") ? BigBus.COST : SmallBus.COST;

            // Simulate the old card click logic
            handleVehicleCardClick(name, cost, "#3B82F6", buyBtn); 
        });

        return new HBox(5, vehicleBox, buyBtn);
    }

    /**
     * Creates a single vehicle purchase card button.
     * @param name Vehicle display name.
     * @param cost Purchase cost.
     * @param speed Speed value.
     * @param revenue Revenue per delivery.
     * @param maint Maintenance per day.
     * @param color Hex color for the swatch.
     * @return The styled card button.
     */
    private Button vehicleCard(String name, double cost, double speed, double revenue, double maint, String color) {
        javafx.scene.shape.Circle swatch = new javafx.scene.shape.Circle(5, Color.web(color));
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label costLbl = new Label("$" + String.format("%,.0f", cost));
        costLbl.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold; -fx-font-size: 11;");
        HBox top = new HBox(5, swatch, nameLabel, spacer, costLbl);
        top.setAlignment(Pos.CENTER_LEFT);

        Label stats = new Label("Spd: " + speed + " | Rev: $" + String.format("%,.0f", revenue) + " | Maint: $" + String.format("%,.0f", maint) + "/day");
        stats.setStyle("-fx-text-fill: #777; -fx-font-size: 9;");

        VBox content = new VBox(2, top, stats);
        content.setPadding(new Insets(4, 6, 4, 6));

        Button card = new Button();
        card.setGraphic(content);
        card.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(cardStyle(false, false));
        card.setOnMouseEntered(e -> { if (!name.equals(activeVehicleCard)) card.setStyle(cardStyle(true, false)); });
        card.setOnMouseExited(e -> { if (!name.equals(activeVehicleCard)) card.setStyle(cardStyle(false, false)); });
        card.setOnAction(e -> handleVehicleCardClick(name, cost, color, card));
        card.setOnDragDetected(e -> {
            javafx.scene.input.Dragboard db = card.startDragAndDrop(javafx.scene.input.TransferMode.MOVE);
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(name + "|" + cost);
            db.setContent(cc);
            e.consume();
        });
        return card;
    }

    /**
     * Returns CSS style for a vehicle card based on hover/active state.
     * @param hover True if hovered.
     * @param active True if selected/active.
     * @return CSS style string.
     */
    private String cardStyle(boolean hover, boolean active) {
        if (active) return "-fx-background-color: #dbeafe; -fx-border-color: #3B82F6; -fx-border-width: 2; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 0;";
        if (hover) return "-fx-background-color: #e8f0fe; -fx-border-color: #90b0e0; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 0;";
        return "-fx-background-color: #f8f8f8; -fx-border-color: #ccc; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 0;";
    }

    /**
     * Handles a vehicle card click: toggles selection, charges cost, creates vehicle, enters placement mode.
     * @param name The vehicle type name.
     * @param cost The purchase cost.
     * @param color The vehicle color hex.
     * @param card The card button for styling.
     */
    private void handleVehicleCardClick(String name, double cost, String color, Button card) {
        if (name.equals(activeVehicleCard)) {
            cancelVehiclePurchase();
            card.setStyle(cardStyle(false, false));
            return;
        }
        if (gameController.getEconomy().getCapital() < cost) {
            showCostError("Not enough money! Need $" + String.format("%,.0f", cost));
            return;
        }
        gameController.getEconomy().spend(cost);
        gameController.getEconomy().recordVehiclePurchase(cost);
        int vid = nextVehicleId++;
        Vehicle vehicle;
        if (name.contains("Truck")) {
            GoodType goodType = pickGoodType();
            if (goodType == null) {
                gameController.getEconomy().refund(cost);
                nextVehicleId--;
                return;
            }
            vehicle = name.equals("Small Truck") ? new SmallTruck(vid, goodType) : new LargeTruck(vid, goodType);
        } else {
            vehicle = name.equals("Small Bus") ? new SmallBus(vid) : new BigBus(vid);
        }
        gameController.addVehicle(vehicle);
        pendingVehicle = vehicle;
        activeVehicleCard = name;
        card.setStyle(cardStyle(false, true));
        renderer.setBuildMode("placeVehicle");
        costLabel.setText("Click a road near city/industry to place " + name);
        costLabel.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: #0f0; -fx-padding: 4 8;");
        costLabel.setVisible(true);
        refreshEconomyInfo();
    }

    /**
     * Cancels a pending vehicle purchase and resets state.
     */
    private void cancelVehiclePurchase() {
        if (pendingVehicle != null) {
            gameController.removeVehicle(pendingVehicle);
            gameController.getEconomy().refund(pendingVehicle.getPurchaseCost());
        }
        pendingVehicle = null;
        activeVehicleCard = null;
        renderer.setBuildMode(null);
        costLabel.setVisible(false);
        refreshEconomyInfo();
    }



    /**
     * Shows a dialog to pick the cargo type for a truck.
     * @return The selected GoodType, or null if cancelled.
     */
    private GoodType pickGoodType() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Wood", "Wood", "Iron", "Paper");
        dialog.setTitle("Cargo Type");
        dialog.setHeaderText("What should this truck transport?");
        var result = dialog.showAndWait();
        if (result.isEmpty()) return null;
        return switch (result.get()) {
            case "Iron"  -> GoodType.IRON;
            case "Paper" -> GoodType.PAPER;
            default      -> GoodType.WOOD;
        };
    }

    /**
     * Updates the style of a build button based on active build mode.
     * @param active The button to style.
     * @param mode The build mode this button represents.
     */
    private void updateButtonStyles(Button active, String mode) {
        if (renderer.getBuildMode() != null && renderer.getBuildMode().equals(mode)) {
            active.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        } else {
            active.setStyle("");
        }
    }

    /**
     * Handles a build click based on the current build mode.
     * Deducts costs from the Economy and updates the map/renderer.
     * @param x The clicked tile x-coordinate.
     * @param y The clicked tile y-coordinate.
     */
    private void handleBuildClick(int x, int y) {
        String mode = renderer.getBuildMode();
        if (mode == null) return;
        switch (mode) {
            case "road" -> {
                Tile tile = map.getTile(x, y);
                if (tile == null || !tile.isBuildable()) return;
                boolean isForest = tile instanceof ForestTile;
                int cost = GameMap.ROAD_COST;
                if (isForest) cost += ((ForestTile) tile).getClearingCost();
                if (gameController.getEconomy().getCapital() < cost) {
                    showAlert("Not enough money! Need $" + cost);
                    return;
                }
                int actual = map.buildRoad(x, y);
                if (actual > 0) {
                    gameController.getEconomy().spend(actual);
                    gameController.getEconomy().recordConstruction(actual);
                    renderer.render(zoomLevel);
                    updateMinimap();
                }
            }
            case "stop" -> {
                Stop stop = map.placeStop(x, y);
                if (stop != null) {
                    gameController.resolveStopNeighbors(stop);
                    refreshStopList();
                    renderer.render(zoomLevel);
                    updateMinimap();
                }
            }
            case "bridge" -> handleBridgeClick(x, y);
            case "placeVehicle" -> {
                if (pendingVehicle != null) {
                    Tile tile = map.getTile(x, y);
                    if (tile instanceof RoadTile road && isNearCityOrIndustry(x, y)) {
                        pendingVehicle.setCurrentTile(road);
                        pendingVehicle = null;
                        activeVehicleCard = null;
                        renderer.setBuildMode(null);
                        costLabel.setText("Vehicle placed!");
                        costLabel.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: #0f0; -fx-padding: 4 8;");
                        costLabel.setVisible(true);
                        renderer.setVehicles(gameController.getVehicles());
                        renderer.render(zoomLevel);
                        updateMinimap();
                    } else {
                        showCostError("Must place on a road near a city or industry!");
                    }
                }
            }
            case "trafficLight" -> handleTrafficLightClick(x, y);
            case "city" -> handleBuildCity(x, y);
            case "industry" -> handleBuildIndustry(x, y);
        }
    }

    private static final int CITY_COST = 5000;
    private static final int INDUSTRY_COST = 3000;

    /**
     * Handles placing a 3x3 city on the map.
     * @param x The clicked tile x-coordinate (top-left of city).
     * @param y The clicked tile y-coordinate (top-left of city).
     */
    private void handleBuildCity(int x, int y) {
        if (gameController.getEconomy().getCapital() < CITY_COST) {
            showCostError("Not enough money! Need $" + CITY_COST);
            return;
        }
        String name = "City " + nextCityId;
        if (!map.placeCity(x, y, name)) {
            showCostError("Cannot place city here! Need 3x3 clear area.");
            return;
        }
        gameController.getEconomy().spend(CITY_COST);
        gameController.getEconomy().recordConstruction(CITY_COST);
        City city = new City(name, x, y, 3, 3);
        gameController.addCity(city);
        nextCityId++;
        renderer.render(zoomLevel);
        updateMinimap();
        refreshEconomyInfo();
        costLabel.setText(name + " built!");
        costLabel.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: #0f0; -fx-padding: 4 8;");
        costLabel.setVisible(true);
    }

    /**
     * Handles placing a 2x2 industry on the map.
     * @param x The clicked tile x-coordinate (top-left of industry).
     * @param y The clicked tile y-coordinate (top-left of industry).
     */
    private void handleBuildIndustry(int x, int y) {
        if (pendingIndustryType == null) return;
        if (gameController.getEconomy().getCapital() < INDUSTRY_COST) {
            showCostError("Not enough money! Need $" + INDUSTRY_COST);
            return;
        }
        if (!map.placeIndustry(x, y, pendingIndustryType)) {
            showCostError("Cannot place industry here! Need 2x2 clear area.");
            return;
        }
        gameController.getEconomy().spend(INDUSTRY_COST);
        gameController.getEconomy().recordConstruction(INDUSTRY_COST);
        Industry industry = switch (pendingIndustryType) {
            case "Iron Mine"  -> new Mine(pendingIndustryType, x, y);
            case "Wood Farm"  -> new Farm(pendingIndustryType, x, y);
            case "Paper Mill" -> new Factory(pendingIndustryType, x, y);
            default           -> new Mine(pendingIndustryType, x, y);
        };
        gameController.addIndustry(industry);
        renderer.render(zoomLevel);
        updateMinimap();
        refreshEconomyInfo();
        costLabel.setText(pendingIndustryType + " built!");
        costLabel.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: #0f0; -fx-padding: 4 8;");
        costLabel.setVisible(true);
    }

    /**
     * Handles traffic light installation/reconfiguration at a junction.
     * @param x The clicked tile x-coordinate.
     * @param y The clicked tile y-coordinate.
     */
    private void handleTrafficLightClick(int x, int y) {
        Tile tile = map.getTile(x, y);
        if (!(tile instanceof RoadTile road)) {
            showCostError("Must be a road tile!");
            return;
        }
        if (map.countRoadNeighbors(x, y) < 3) {
            showCostError("Need a 3-way or 4-way junction!");
            return;
        }
        TrafficLight existing = gameController.findTrafficLight(road);
        int defaultNs = existing != null ? existing.getNsGreenTime() / 1000 : 5;
        int defaultEw = existing != null ? existing.getEwGreenTime() / 1000 : 5;

        Dialog<int[]> dialog = new Dialog<>();
        dialog.setTitle("Traffic Light");
        dialog.setHeaderText(existing != null ? "Reconfigure traffic light" : "Install traffic light");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Spinner<Integer> nsSpinner = new Spinner<>(1, 30, defaultNs);
        Spinner<Integer> ewSpinner = new Spinner<>(1, 30, defaultEw);
        nsSpinner.setEditable(true);
        ewSpinner.setEditable(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("N-S green (sec):"), 0, 0);
        grid.add(nsSpinner, 1, 0);
        grid.add(new Label("E-W green (sec):"), 0, 1);
        grid.add(ewSpinner, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) return new int[]{nsSpinner.getValue(), ewSpinner.getValue()};
            return null;
        });

        dialog.showAndWait().ifPresent(times -> {
            if (existing != null) {
                existing.setNsGreenTime(times[0] * 1000);
                existing.setEwGreenTime(times[1] * 1000);
                costLabel.setText("Traffic light reconfigured!");
            } else {
                road.setTrafficLight(true);
                TrafficLight tl = new TrafficLight(road, times[0] * 1000, times[1] * 1000);
                gameController.addTrafficLight(tl);
                costLabel.setText("Traffic light installed!");
            }
            costLabel.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: #0f0; -fx-padding: 4 8;");
            costLabel.setVisible(true);
            renderer.setTrafficLights(gameController.getTrafficLights());
            renderer.render(zoomLevel);
        });
    }

    /**
     * Checks if a tile is adjacent to any city or industry.
     * @param x the tile x
     * @param y the tile y
     * @return true if near a city or industry
     */
    private boolean isNearCityOrIndustry(int x, int y) {
        if (map.isCityInternalRoad(x, y)) return true;
        for (Tile t : map.getNeighbors(x, y)) {
            if (t instanceof CityTile || t instanceof IndustryTile) return true;
        }
        return false;
    }


    /**
     * Handles bridge building with two-click start/end selection.
     * @param x The clicked tile x-coordinate.
     * @param y The clicked tile y-coordinate.
     */
    private void handleBridgeClick(int x, int y) {
        if (!map.isValidBridgeEndpoint(x, y)) return;
        if (renderer.getBridgeStartX() < 0) {
            renderer.setBridgeStart(x, y);
            costLabel.setText("Bridge start set. Click other side.");
            costLabel.setVisible(true);
            renderer.render(zoomLevel);
        } else {
            int sx = renderer.getBridgeStartX(), sy = renderer.getBridgeStartY();
            if (sx != x && sy != y) {
                showCostError("Bridge must be a straight line!");
                return;
            }
            showBridgeTypeDialog(sx, sy, x, y);
        }
    }

    /**
     * Shows a dialog for selecting bridge type and attempts to build the bridge.
     * @param x1 Start x-coordinate.
     * @param y1 Start y-coordinate.
     * @param x2 End x-coordinate.
     * @param y2 End y-coordinate.
     */
    private void showBridgeTypeDialog(int x1, int y1, int x2, int y2) {
        ChoiceDialog<BridgeType> dialog = new ChoiceDialog<>(BridgeType.WOODEN, BridgeType.values());
        dialog.setTitle("Bridge Type");
        dialog.setHeaderText("Select bridge type");
        dialog.setContentText("Wooden=$500/max3, Stone=$1500/max5, Steel=$3000/max10");
        dialog.showAndWait().ifPresent(type -> {
            if (gameController.getEconomy().getCapital() < type.getCost()) {
                showCostError("Not enough money! Need $" + type.getCost());
                renderer.setBridgeStart(-1, -1);
                renderer.render(zoomLevel);
                return;
            }
            Bridge bridge = map.buildBridge(x1, y1, x2, y2, type);
            if (bridge != null) {
                gameController.getEconomy().spend(bridge.getCost());
                gameController.getEconomy().recordConstruction(bridge.getCost());
                renderer.setBridgeStart(-1, -1);
                renderer.render(zoomLevel);
                updateMinimap();
                costLabel.setText("Bridge built! Cost: $" + bridge.getCost());
                costLabel.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: #0f0; -fx-padding: 4 8;");
                costLabel.setVisible(true);
            } else {
                showCostError("Cannot build bridge here! (too long or no water)");
                renderer.setBridgeStart(-1, -1);
                renderer.render(zoomLevel);
            }
        });
    }

    /**
     * Updates the tile info and cost preview labels based on hover position.
     * @param x The hovered tile x-coordinate.
     * @param y The hovered tile y-coordinate.
     */
    private void updateTileInfo(int x, int y) {
        Tile tile = map.getTile(x, y);
        if (tile == null) return;

        String info = "(" + x + ", " + y + ") " + tile.getType();
        if (tile instanceof ForestTile f) info += " [" + f.getTreeCount() + " trees]";
        if (tile instanceof CityTile c) info += " - " + c.getCityName();
        if (tile instanceof IndustryTile i) info += " - " + i.getIndustryName();
        if (map.isCityInternalRoad(x, y)) info += " (city road)";
        tileInfoLabel.setText(info);
        tileInfoLabel.setVisible(true);

        if ("road".equals(renderer.getBuildMode()) && map.isValidBuildSite(x, y)) {
            int cost = GameMap.ROAD_COST;
            if (tile instanceof ForestTile f) cost += f.getClearingCost();
            costLabel.setText("Cost: $" + cost);
            costLabel.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: #0f0; -fx-padding: 4 8;");
            costLabel.setVisible(true);
        } else if ("stop".equals(renderer.getBuildMode()) && map.isValidStopSite(x, y)) {
            costLabel.setText("Place stop here");
            costLabel.setVisible(true);
        } else if ("bridge".equals(renderer.getBuildMode()) && map.isValidBridgeEndpoint(x, y)) {
            costLabel.setText(renderer.getBridgeStartX() < 0 ? "Click to set bridge start" : "Click to set bridge end");
            costLabel.setVisible(true);
        } else if ("placeVehicle".equals(renderer.getBuildMode())) {
            Tile t = map.getTile(x, y);
            boolean valid = t instanceof RoadTile && isNearCityOrIndustry(x, y);
            costLabel.setText(valid ? "Click to place vehicle here" : "Must be a road near city/industry!");
            costLabel.setStyle(valid
                    ? "-fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: #0f0; -fx-padding: 4 8;"
                    : "-fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: red; -fx-padding: 4 8;");
            costLabel.setVisible(true);
        } else if ("trafficLight".equals(renderer.getBuildMode())) {
            boolean valid = tile instanceof RoadTile && map.countRoadNeighbors(x, y) >= 3;
            costLabel.setText(valid ? "Click to install/configure traffic light" : "Need a 3+ way junction!");
            costLabel.setStyle(valid
                    ? "-fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: #0f0; -fx-padding: 4 8;"
                    : "-fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: red; -fx-padding: 4 8;");
            costLabel.setVisible(true);
        } else {
            costLabel.setVisible(false);
        }
    }

    /**
     * Refreshes the stop list in the sidebar with named labels and route/vehicle info.
     */
    private void refreshStopList() {
        stopListBox.getChildren().clear();
        if (map.getStops().isEmpty()) {
            stopListBox.getChildren().add(new Label("No stops placed yet"));
            return;
        }
        for (Stop stop : map.getStops()) {
            String label = "#" + stop.getId() + " " + stopDisplayName(stop);
            if (!stop.getRoutes().isEmpty()) {
                List<String> rNames = new ArrayList<>();
                for (Route r : stop.getRoutes()) rNames.add("R" + r.getId());
                label += " [" + String.join(",", rNames) + "]";
            }
            Label lbl = new Label(label);
            lbl.setStyle("-fx-font-size: 11;");
            stopListBox.getChildren().add(lbl);
        }
    }

    /**
     * Returns a short display name for a stop based on its nearby entity.
     * @param stop The stop.
     * @return Display name like "Millville" or "Iron Mine" or "(12,34)".
     */
    private String stopDisplayName(Stop stop) {
        if (stop.getNearbyCity() != null) return stop.getNearbyCity().getName();
        if (stop.getNearbyIndustry() != null) return stop.getNearbyIndustry().getName();
        return "(" + stop.getX() + "," + stop.getY() + ")";
    }

    /**
     * Refreshes the economy info panel in the sidebar with earnings, expenses breakdown,
     * supply/demand stats, and fleet details.
     */
    private void refreshEconomyInfo() {
        economyInfoBox.getChildren().clear();
        Economy eco = gameController.getEconomy();
        economyInfoBox.getChildren().addAll(
                new Label("Cities: " + gameController.getCities().size()),
                new Label("Industries: " + gameController.getIndustries().size()),
                new Label("Vehicles: " + gameController.getVehicles().size()),
                new Label("Routes: " + gameController.getRoutes().size()),
                new Separator(),
                styledLabel("Deliveries: " + eco.getDeliveryCount(), "#2e7d32"),
                styledLabel("Income: $" + fmt(eco.getDeliveryEarned()), "#2e7d32"),
                styledLabel("Road costs: $" + fmt(eco.getConstructionSpent()), "#c62828"),
                styledLabel("Vehicles: $" + fmt(eco.getVehicleSpent()), "#c62828"),
                styledLabel("Maintenance: $" + fmt(eco.getMaintenanceSpent()), "#c62828"),
                new Separator(),
                styledLabel("Profit: $" + fmt(eco.getTotalEarned() - eco.getTotalSpent()),
                        eco.getTotalEarned() >= eco.getTotalSpent() ? "#2e7d32" : "#c62828")
        );
        for (Route r : gameController.getRoutes()) {
            Label rl = new Label("📍 R" + r.getId() + ": " + r.getName());
            rl.setStyle("-fx-font-size: 10; -fx-text-fill: #1565c0;");
            economyInfoBox.getChildren().add(rl);
        }
        if (!gameController.getVehicles().isEmpty()) {
            economyInfoBox.getChildren().add(new Separator());
        }
        for (Vehicle v : gameController.getVehicles()) {
            String status = v.isWaiting() ? "⏳" : "🚛";
            String cargo = v.getCargo() != null ? v.getCargo().toString() : "empty";
            String routeInfo = v.getAssignedRoute() != null
                    ? " → R" + v.getAssignedRoute().getId()
                    : " (no route)";
            Label vl = new Label(status + " " + v.getName() + routeInfo + " [" + cargo + "]");
            vl.setStyle("-fx-font-size: 10;");
            economyInfoBox.getChildren().add(vl);
        }
    }

    /**
     * Creates a styled label with the given color.
     * @param text The label text.
     * @param color The CSS color string.
     * @return The styled Label.
     */
    private Label styledLabel(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 11; -fx-text-fill: " + color + ";");
        return l;
    }

    /**
     * Formats a double as a comma-separated integer string.
     * @param val The value to format.
     * @return The formatted string.
     */
    private String fmt(double val) { return String.format("%,.0f", val); }

    /**
     * Shows a red error message on the cost label overlay.
     * @param msg The error message.
     */
    private void showCostError(String msg) {
        costLabel.setText(msg);
        costLabel.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: red; -fx-padding: 4 8;");
        costLabel.setVisible(true);
    }

    /**
     * Shows a warning alert dialog.
     * @param msg The message to display.
     */
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    /**
     * Shows a dialog to create a route by selecting 2+ stops from the placed stops list.
     * Optionally lets the player name the route.
     */
    private void showCreateRouteDialog() {
        List<Stop> allStops = map.getStops();
        if (allStops.size() < 2) {
            showAlert("Need at least 2 stops to create a route.");
            return;
        }
        List<Stop> selected = new ArrayList<>();
        while (true) {
            List<Stop> available = new ArrayList<>(allStops);
            available.removeAll(selected);
            if (available.isEmpty()) break;
            List<String> choices = new ArrayList<>();
            for (Stop s : available) choices.add(s.toString());
            if (selected.size() >= 2) choices.add(0, "-- Done --");
            String header = "Selected: " + selected.size() + " stops";
            if (!selected.isEmpty()) {
                header += "\n";
                for (int i = 0; i < selected.size(); i++) {
                    if (i > 0) header += " → ";
                    header += stopDisplayName(selected.get(i));
                }
            }
            ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
            dialog.setTitle("Create Route");
            dialog.setHeaderText(header + "\nPick next stop:");
            dialog.getDialogPane().setMinWidth(500);
            var result = dialog.showAndWait();
            if (result.isEmpty() || "-- Done --".equals(result.get())) break;
            String pick = result.get();
            for (Stop s : available) {
                if (s.toString().equals(pick)) { selected.add(s); break; }
            }
        }
        if (selected.size() >= 2) {
            Route route = gameController.createRoute();
            for (Stop s : selected) route.addStop(s);
            TextInputDialog nameDialog = new TextInputDialog(route.getName());
            nameDialog.setTitle("Name Route");
            nameDialog.setHeaderText("Route #" + route.getId() + ": " + route.getName());
            nameDialog.setContentText("Custom name (or leave as-is):");
            nameDialog.showAndWait().ifPresent(n -> {
                if (!n.isBlank() && !n.equals(route.getName())) route.setName(n);
            });
            refreshStopList();
            refreshEconomyInfo();
        }
    }

    /**
     * Shows a dialog to assign an unassigned vehicle to an existing route.
     * Uses descriptive vehicle and route names for better UX.
     */
    private void showAssignRouteDialog() {
        List<Vehicle> unassigned = new ArrayList<>();
        for (Vehicle v : gameController.getVehicles()) {
            if (v.getAssignedRoute() == null && v.getCurrentTile() != null) unassigned.add(v);
        }
        if (unassigned.isEmpty()) {
            showAlert("No unassigned placed vehicles available.");
            return;
        }
        List<Route> allRoutes = gameController.getRoutes();
        if (allRoutes.isEmpty()) {
            showAlert("No routes created yet.");
            return;
        }
        List<String> vChoices = new ArrayList<>();
        for (Vehicle v : unassigned) vChoices.add(v.getName());
        ChoiceDialog<String> vDialog = new ChoiceDialog<>(vChoices.get(0), vChoices);
        vDialog.setTitle("Assign Vehicle");
        vDialog.setHeaderText("Select vehicle to assign");
        vDialog.getDialogPane().setMinWidth(450);
        vDialog.showAndWait().ifPresent(vChoice -> {
            Vehicle picked = null;
            for (Vehicle v : unassigned) {
                if (v.getName().equals(vChoice)) { picked = v; break; }
            }
            if (picked == null) return;
            List<String> rChoices = new ArrayList<>();
            for (Route r : allRoutes) rChoices.add(r.toString());
            ChoiceDialog<String> rDialog = new ChoiceDialog<>(rChoices.get(0), rChoices);
            rDialog.setTitle("Assign Route");
            rDialog.setHeaderText("Select route for " + picked.getName());
            rDialog.getDialogPane().setMinWidth(500);
            Vehicle finalPicked = picked;
            rDialog.showAndWait().ifPresent(rChoice -> {
                for (Route r : allRoutes) {
                    if (r.toString().equals(rChoice)) {
                        r.assignVehicle(finalPicked);
                        refreshEconomyInfo();
                        break;
                    }
                }
            });
        });
    }

    /**
     * Updates the minimap rendering.
     */
    private void updateMinimap() { renderer.renderMinimap(minimapCanvas); }

    /**
     * Application entry point.
     * @param args Command line arguments.
     */
    public static void main(String[] args) { launch(args); }
}
