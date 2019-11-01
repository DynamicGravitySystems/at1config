package com.dynamicgravitysystems.at1config.util;

import javafx.scene.Parent;

public class ParentAndController {
    private final Parent parent;
    private final BaseController controller;

    public ParentAndController(Parent parent, BaseController controller) {
        this.parent = parent;
        this.controller = controller;
    }

    public Parent getParent() {
        return parent;
    }

    public BaseController getController() {
        return controller;
    }

}
