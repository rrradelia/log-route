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
    private String pendingIndustryType = null;

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
        HBox hud = new HBox(20);
        hud.setPadding(new Insets(8, 15, 8, 15));
        hud.setAlignment(Pos.CENTER);
        hud.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #999; -fx-border-width: 0 0 2 0;");

        Label menuLabel = new Label("☰ Menu");
        menuLabel.setFont(Font.font(14));
        menuLabel.setStyle("-fx-cursor: hand; -fx-padding: 4 10; -fx-background-color: #e0e0e0; -fx-background-radius: 4;");
        menuLabel.setOnMouseClicked(e -> showMenuPopup());

        Label capitalLabel = new Label();
        capitalLabel.setFont(Font.font(16));
        capitalLabel.setStyle("-fx-font-weight: bold;");
        capitalLabel.textProperty().bind(
                gameController.getEconomy().capitalProperty().asString("$%,.0f"));

        Label dateLabel = new Label();
        dateLabel.setFont(Font.font(14));
        dateLabel.textProperty().bind(gameController.dayLabelProperty());

        HBox speedBox = new HBox(5);
        for (SimSpeed s : SimSpeed.values()) {
            Button btn = new Button(s.getLabel());
            btn.setStyle("-fx-font-size: 12;");
            btn.setOnAction(e -> gameController.setSimSpeed(s));
            speedBox.getChildren().add(btn);
        }

        Region spacer1 = new Region();
        Region spacer2 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        hud.getChildren().addAll(menuLabel, spacer1, capitalLabel, dateLabel, spacer2, speedBox);
        return hud;
    }

    /**
     * Creates the center map area with drag-scroll, minimap, and info overlays.
     * @return The map area layout.
     */
    private StackPane createMapArea() {
        Canvas canvas = renderer.getCanvas();
        Pane mapPane = new Pane(canvas);
        mapPane.setStyle("-fx-background-color: #333;");
        mapPane.widthProperty().addListener((o, ov, nv) -> {
            renderer.resizeCanvas(nv.doubleValue(), mapPane.getHeight());
            renderer.render(zoomLevel);
        });
        mapPane.heightProperty().addListener((o, ov, nv) -> {
            renderer.resizeCanvas(mapPane.getWidth(), nv.doubleValue());
            renderer.render(zoomLevel);
        });
        setupMapInteraction(mapPane);
        setupVehicleDrop(canvas);

        tileInfoLabel = new Label("");
        tileInfoLabel.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-text-fill: white; -fx-padding: 4 8;");
        tileInfoLabel.setFont(Font.font(11));
        tileInfoLabel.setVisible(false);

        costLabel = new Label("");
        costLabel.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: #0f0; -fx-padding: 4 8;");
        costLabel.setFont(Font.font(11));
        costLabel.setVisible(false);

        minimapCanvas = new Canvas(150, 150);
        StackPane minimapWrapper = new StackPane(minimapCanvas);
        minimapWrapper.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 2;");
        minimapWrapper.setMaxSize(154, 154);
        StackPane.setAlignment(minimapWrapper, Pos.BOTTOM_LEFT);
        StackPane.setMargin(minimapWrapper, new Insets(0, 0, 20, 20));
        setupMinimapClick(minimapWrapper, mapPane);

        StackPane stack = new StackPane(mapPane, minimapWrapper, tileInfoLabel, costLabel);
        StackPane.setAlignment(tileInfoLabel, Pos.TOP_LEFT);
        StackPane.setMargin(tileInfoLabel, new Insets(10, 0, 0, 10));
        StackPane.setAlignment(costLabel, Pos.TOP_LEFT);
        StackPane.setMargin(costLabel, new Insets(30, 0, 0, 10));
        return stack;
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
    private ScrollPane createSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.setPadding(new Insets(10));
        sidebar.setPrefWidth(245);
        sidebar.setStyle("-fx-background-color: #ddd;");

        TitledPane stopsPane = new TitledPane();
        stopsPane.setText("Stops");
        stopListBox = new VBox(3);
        stopListBox.getChildren().add(new Label("No stops placed yet"));
        stopsPane.setContent(stopListBox);

        TitledPane economyPane = new TitledPane();
        economyPane.setText("Economy");
        economyInfoBox = new VBox(3);
        economyInfoBox.getChildren().addAll(
                new Label("Cities: " + gameController.getCities().size()),
                new Label("Industries: " + gameController.getIndustries().size()),
                new Label("Vehicles: 0"),
                new Label("Routes: 0")
        );
        economyPane.setContent(economyInfoBox);

        Button buildRoadBtn = new Button("Build a road ($100)");
        buildRoadBtn.setMaxWidth(Double.MAX_VALUE);
        buildRoadBtn.setOnAction(e -> {
            renderer.setBuildMode("road".equals(renderer.getBuildMode()) ? null : "road");
            updateButtonStyles(buildRoadBtn, "road");
        });

        Button buildBridgeBtn = new Button("Build a bridge");
        buildBridgeBtn.setMaxWidth(Double.MAX_VALUE);
        buildBridgeBtn.setOnAction(e -> {
            renderer.setBuildMode("bridge".equals(renderer.getBuildMode()) ? null : "bridge");
            updateButtonStyles(buildBridgeBtn, "bridge");
        });

        Button setStopBtn = new Button("Set a stop");
        setStopBtn.setMaxWidth(Double.MAX_VALUE);
        setStopBtn.setOnAction(e -> {
            renderer.setBuildMode("stop".equals(renderer.getBuildMode()) ? null : "stop");
            updateButtonStyles(setStopBtn, "stop");
        });

        Button buildCityBtn = new Button("\ud83c\udfe0 Build City ($5,000)");
        buildCityBtn.setMaxWidth(Double.MAX_VALUE);
        buildCityBtn.setStyle("-fx-text-fill: #c77600;");
        buildCityBtn.setOnAction(e -> {
            renderer.setBuildMode("city".equals(renderer.getBuildMode()) ? null : "city");
            updateButtonStyles(buildCityBtn, "city");
        });

        Button buildIndustryBtn = new Button("\ud83c\udfed Build Industry ($3,000)");
        buildIndustryBtn.setMaxWidth(Double.MAX_VALUE);
        buildIndustryBtn.setStyle("-fx-text-fill: #8a2be2;");
        buildIndustryBtn.setOnAction(e -> {
            ChoiceDialog<String> typeDialog = new ChoiceDialog<>("Iron Mine", "Iron Mine", "Wood Farm", "Paper Mill");
            typeDialog.setTitle("Industry Type");
            typeDialog.setHeaderText("Select industry type to build");
            typeDialog.showAndWait().ifPresent(type -> {
                pendingIndustryType = type;
                renderer.setBuildMode("industry");
                updateButtonStyles(buildIndustryBtn, "industry");
                costLabel.setText("Click grass to place " + type);
                costLabel.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: #0f0; -fx-padding: 4 8;");
                costLabel.setVisible(true);
            });
        });

        VBox vehicleCards = createVehicleCards();

        Button createRouteBtn = new Button("Create route");
        createRouteBtn.setMaxWidth(Double.MAX_VALUE);
        createRouteBtn.setOnAction(e -> showCreateRouteDialog());

        Button assignRouteBtn = new Button("Assign vehicle to route");
        assignRouteBtn.setMaxWidth(Double.MAX_VALUE);
        assignRouteBtn.setOnAction(e -> showAssignRouteDialog());

        Button autoConnectBtn = new Button("⚡ Auto-Connect");
        autoConnectBtn.setMaxWidth(Double.MAX_VALUE);
        autoConnectBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        autoConnectBtn.setOnAction(e -> showAutoConnectDialog());

        Button toggleRouteBtn = new Button("Toggle route overlay");
        toggleRouteBtn.setMaxWidth(Double.MAX_VALUE);
        toggleRouteBtn.setOnAction(e -> { renderer.toggleRouteOverlay(); renderer.render(zoomLevel); });

        Button addTrafficLightBtn = new Button("Install traffic light");
        addTrafficLightBtn.setMaxWidth(Double.MAX_VALUE);
        addTrafficLightBtn.setOnAction(e -> {
            renderer.setBuildMode("trafficLight".equals(renderer.getBuildMode()) ? null : "trafficLight");
            updateButtonStyles(addTrafficLightBtn, "trafficLight");
        });

        HBox zoomBox = new HBox(5);
        zoomBox.setAlignment(Pos.CENTER);
        Button zoomInBtn = new Button("+");
        Button zoomOutBtn = new Button("-");
        Button zoomResetBtn = new Button("1:1");
        zoomInBtn.setOnAction(e -> { zoomLevel = Math.min(ZOOM_MAX, zoomLevel + ZOOM_STEP); clampCamera(renderer.getCamX(), renderer.getCamY()); renderer.render(zoomLevel); });
        zoomOutBtn.setOnAction(e -> { zoomLevel = Math.max(ZOOM_MIN, zoomLevel - ZOOM_STEP); clampCamera(renderer.getCamX(), renderer.getCamY()); renderer.render(zoomLevel); });
        zoomResetBtn.setOnAction(e -> { zoomLevel = 1.0; clampCamera(renderer.getCamX(), renderer.getCamY()); renderer.render(zoomLevel); });
        zoomBox.getChildren().addAll(new Label("Zoom:"), zoomOutBtn, zoomResetBtn, zoomInBtn);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setStyle("-fx-text-fill: red;");
        cancelBtn.setOnAction(e -> { cancelVehiclePurchase(); renderer.setBuildMode(null); renderer.render(zoomLevel); });

        sidebar.getChildren().addAll(
                stopsPane, economyPane,
                new Separator(),
                autoConnectBtn,
                new Separator(),
                buildRoadBtn, buildBridgeBtn, setStopBtn,
                buildCityBtn, buildIndustryBtn,
                new Separator(),
                vehicleCards, createRouteBtn, assignRouteBtn,
                new Separator(),
                toggleRouteBtn, addTrafficLightBtn,
                new Separator(),
                zoomBox,
                new Separator(),
                cancelBtn
        );
        ScrollPane sp = new ScrollPane(sidebar);
        sp.setFitToWidth(true);
        sp.setPrefWidth(260);
        sp.setStyle("-fx-border-color: #999; -fx-border-width: 0 0 0 2;");
        return sp;
    }

    /**
     * Shows the in-game menu popup with Continue, Restart, and Quit options.
     */
    private void showMenuPopup() {
        gameController.pause();
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(primaryStage);
        popup.initStyle(StageStyle.UNDECORATED);
        popup.setTitle("Menu");

        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30, 50, 30, 50));
        box.setStyle("-fx-background-color: #2c2c2c; -fx-background-radius: 10; -fx-border-color: #555; -fx-border-radius: 10; -fx-border-width: 2;");

        Label title = new Label("⏸ Game Paused");
        title.setFont(Font.font("Arial", 20));
        title.setTextFill(Color.WHITE);

        Button continueBtn = menuButton("▶ Continue", "#4CAF50");
        continueBtn.setOnAction(e -> { popup.close(); gameController.resume(); });

        Button rulesBtn = menuButton("📖 Game Rules", "#607D8B");
        rulesBtn.setOnAction(e -> showGameRulesDialog());

        Button restartBtn = menuButton("🔄 Restart", "#FF9800");
        restartBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Restart the game? All progress will be lost.", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(r -> {
                if (r == ButtonType.YES) { popup.close(); launchGame(); }
            });
        });

        Button mainMenuBtn = menuButton("🏠 Main Menu", "#2196F3");
        mainMenuBtn.setOnAction(e -> { popup.close(); returnToStartScreen(); });

        Button quitBtn = menuButton("✖ Quit", "#f44336");
        quitBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to quit?", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(r -> {
                if (r == ButtonType.YES) { popup.close(); Platform.exit(); }
            });
        });

        box.getChildren().addAll(title, continueBtn, rulesBtn, restartBtn, mainMenuBtn, quitBtn);
        Scene scene = new Scene(box);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        popup.showAndWait();
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
    private VBox createVehicleCards() {
        VBox cards = new VBox(6);
        cards.getChildren().addAll(
                vehicleCard("Small Truck", SmallTruck.COST, 0.10, 1500, 50, "#3B82F6"),
                vehicleCard("Large Truck", LargeTruck.COST, 0.05, 4000, 150, "#F59E0B"),
                vehicleCard("Small Bus", SmallBus.COST, 0.12, 1000, 60, "#EF4444"),
                vehicleCard("Big Bus", BigBus.COST, 0.08, 2000, 120, "#8B5CF6")
        );

        Button sellAllBtn = new Button("Sell All Vehicles (50%)");
        sellAllBtn.setMaxWidth(Double.MAX_VALUE);
        sellAllBtn.setStyle("-fx-text-fill: red; -fx-font-size: 11;");
        sellAllBtn.setOnAction(e -> {
            List<Vehicle> fleet = gameController.getVehicles();
            if (fleet.isEmpty()) { showAlert("No vehicles to sell."); return; }
            double refund = 0;
            for (Vehicle v : fleet) refund += v.getPurchaseCost() * 0.5;
            gameController.clearVehicles();
            gameController.getEconomy().refund(refund);
            pendingVehicle = null;
            activeVehicleCard = null;
            renderer.setBuildMode(null);
            renderer.setVehicles(gameController.getVehicles());
            renderer.render(zoomLevel);
            refreshEconomyInfo();
            costLabel.setText("Sold all vehicles! Refund: $" + String.format("%,.0f", refund));
            costLabel.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: #0f0; -fx-padding: 4 8;");
            costLabel.setVisible(true);
        });
        cards.getChildren().add(sellAllBtn);
        return cards;
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
     * Shows the auto-connect dialog listing all possible city↔city and industry↔city
     * connections with cost estimates. Player picks connection then vehicle type.
     */
    private void showAutoConnectDialog() {
        List<String> options = new ArrayList<>();
        List<int[]> fromRoads = new ArrayList<>();
        List<int[]> toRoads = new ArrayList<>();
        List<Industry> sourceIndustries = new ArrayList<>();
        List<int[]> roadCosts = new ArrayList<>();

        List<City> allCities = gameController.getCities();
        List<Industry> allIndustries = gameController.getIndustries();

        for (Industry ind : allIndustries) {
            int[] fromRoad = map.findStopRoad(ind.getTileX(), ind.getTileY(), ind.getWidthTiles(), ind.getHeightTiles());
            if (fromRoad == null) fromRoad = map.findEdgeRoad(ind.getTileX(), ind.getTileY(), ind.getWidthTiles(), ind.getHeightTiles());
            if (fromRoad == null) continue;
            for (City city : allCities) {
                int[] toRoad = map.findStopRoad(city.getTileX(), city.getTileY(), city.getWidthTiles(), city.getHeightTiles());
                if (toRoad == null) toRoad = map.findEdgeRoad(city.getTileX(), city.getTileY(), city.getWidthTiles(), city.getHeightTiles());
                if (toRoad == null) continue;
                int rc = map.calcRoadPathCost(fromRoad[0], fromRoad[1], toRoad[0], toRoad[1]);
                if (rc < 0) continue;
                options.add(ind.getName() + " → " + city.getName() + "  [Road: $" + rc + "]");
                fromRoads.add(fromRoad);
                toRoads.add(toRoad);
                sourceIndustries.add(ind);
                roadCosts.add(new int[]{rc});
            }
        }

        for (int i = 0; i < allCities.size(); i++) {
            City a = allCities.get(i);
            int[] fromRoad = map.findStopRoad(a.getTileX(), a.getTileY(), a.getWidthTiles(), a.getHeightTiles());
            if (fromRoad == null) fromRoad = map.findEdgeRoad(a.getTileX(), a.getTileY(), a.getWidthTiles(), a.getHeightTiles());
            if (fromRoad == null) continue;
            for (int j = i + 1; j < allCities.size(); j++) {
                City b = allCities.get(j);
                int[] toRoad = map.findStopRoad(b.getTileX(), b.getTileY(), b.getWidthTiles(), b.getHeightTiles());
                if (toRoad == null) toRoad = map.findEdgeRoad(b.getTileX(), b.getTileY(), b.getWidthTiles(), b.getHeightTiles());
                if (toRoad == null) continue;
                int rc = map.calcRoadPathCost(fromRoad[0], fromRoad[1], toRoad[0], toRoad[1]);
                if (rc < 0) continue;
                options.add(a.getName() + " ↔ " + b.getName() + "  [Road: $" + rc + "]");
                fromRoads.add(fromRoad);
                toRoads.add(toRoad);
                sourceIndustries.add(null);
                roadCosts.add(new int[]{rc});
            }
        }

        if (options.isEmpty()) {
            showAlert("No possible connections found! (no buildable paths between locations)");
            return;
        }

        ChoiceDialog<String> connDialog = new ChoiceDialog<>(options.get(0), options);
        connDialog.setTitle("Auto-Connect");
        connDialog.setHeaderText("Pick a connection to build automatically.\nRoad + stops + route + vehicle — all in one click!");
        connDialog.setContentText("Connection:");
        connDialog.getDialogPane().setMinWidth(600);
        connDialog.showAndWait().ifPresent(choice -> {
            int idx = options.indexOf(choice);
            if (idx < 0) return;
            Industry ind = sourceIndustries.get(idx);
            List<String> vehicleChoices = ind != null
                    ? List.of("Small Truck ($" + fmt(SmallTruck.COST) + ")", "Large Truck ($" + fmt(LargeTruck.COST) + ")")
                    : List.of("Small Bus ($" + fmt(SmallBus.COST) + ")", "Big Bus ($" + fmt(BigBus.COST) + ")");
            ChoiceDialog<String> vDialog = new ChoiceDialog<>(vehicleChoices.get(0), vehicleChoices);
            vDialog.setTitle("Choose Vehicle");
            vDialog.setHeaderText("Select vehicle type for this route");
            vDialog.showAndWait().ifPresent(vChoice -> {
                double vCost = vChoice.contains("Large Truck") ? LargeTruck.COST
                        : vChoice.contains("Big Bus") ? BigBus.COST
                        : vChoice.contains("Small Bus") ? SmallBus.COST
                        : SmallTruck.COST;
                int totalCost = roadCosts.get(idx)[0] + (int) vCost;
                if (gameController.getEconomy().getCapital() < totalCost) {
                    showAlert("Not enough money! Need $" + totalCost + ", have $"
                            + (int) gameController.getEconomy().getCapital());
                    return;
                }
                gameController.getEconomy().spend(vCost);
                gameController.getEconomy().recordVehiclePurchase(vCost);
                int vid = nextVehicleId++;
                Vehicle vehicle;
                if (ind != null) {
                    GoodType goodType = guessGoodType(ind);
                    vehicle = vChoice.startsWith("Large")
                            ? new LargeTruck(vid, goodType)
                            : new SmallTruck(vid, goodType);
                } else {
                    vehicle = vChoice.startsWith("Big")
                            ? new BigBus(vid)
                            : new SmallBus(vid);
                }
                String result = gameController.autoConnect(
                        fromRoads.get(idx)[0], fromRoads.get(idx)[1],
                        toRoads.get(idx)[0], toRoads.get(idx)[1], vehicle);
                refreshStopList();
                refreshEconomyInfo();
                renderer.setVehicles(gameController.getVehicles());
                renderer.setRoutes(gameController.getRoutes());
                renderer.render(zoomLevel);
                updateMinimap();
                costLabel.setText(result);
                costLabel.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-text-fill: #0f0; -fx-padding: 4 8;");
                costLabel.setVisible(true);
            });
        });
    }

    /**
     * Guesses the primary good type produced by an industry.
     * @param industry the industry
     * @return the good type to transport
     */
    private GoodType guessGoodType(Industry industry) {
        if (industry instanceof Mine) return GoodType.IRON;
        if (industry instanceof Farm) return GoodType.WOOD;
        if (industry instanceof Factory) return GoodType.PAPER;
        return GoodType.WOOD;
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
