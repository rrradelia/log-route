module game {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.base;

    exports game;
    exports game.map;
    exports game.tile;
    exports game.transport;
    exports game.vehicle;
    exports game.util;
    exports game.goods;
    exports game.traffic;
    exports game.controller;
    exports game.economy;
    exports game.city;
    exports game.industry;
    exports game.ui;
}
