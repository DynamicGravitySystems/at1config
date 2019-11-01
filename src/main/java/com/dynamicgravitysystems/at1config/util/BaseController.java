package com.dynamicgravitysystems.at1config.util;

import javafx.fxml.Initializable;
import javafx.stage.Stage;

public abstract class BaseController implements Initializable {

    private Stage stage;

    public abstract void dispose();

    public final void setStage(Stage stage) {
        this.stage = stage;
    }

    protected Stage getStage() {
        return stage;
    }

}
