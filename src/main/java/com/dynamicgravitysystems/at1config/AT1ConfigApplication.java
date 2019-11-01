package com.dynamicgravitysystems.at1config;

import com.dynamicgravitysystems.at1config.services.SerialServiceManager;
import com.dynamicgravitysystems.at1config.windows.MainApplicationWindow;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;


public class AT1ConfigApplication extends Application {

    @Override
    public void init() throws Exception {
    }

    @Override
    public void start(Stage stage) throws Exception {
        MainApplicationWindow mainWindow = new MainApplicationWindow(stage);
        mainWindow.show();
    }

    @Override
    public void stop() throws Exception {
        SerialServiceManager.shutdown();
        Platform.exit();
    }
}
