package com.dynamicgravitysystems.at1config.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;

import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    MenuItem exitItem;

    @FXML
    TextArea textConsole;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        exitItem.setOnAction(actionEvent -> System.out.println("Exiting"));

    }
}
