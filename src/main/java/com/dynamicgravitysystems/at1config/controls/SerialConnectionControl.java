package com.dynamicgravitysystems.at1config.controls;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * The SerialConnectionControl provides a UI element to control the connection to a serial port, allowing user specified
 * port, baud rate, etc. And managing the connection state.
 */
public class SerialConnectionControl extends VBox {

    SerialConnectionControl() {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/SerialConnectionControl.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }

}
