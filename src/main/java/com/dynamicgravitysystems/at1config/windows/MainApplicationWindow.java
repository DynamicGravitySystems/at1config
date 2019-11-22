package com.dynamicgravitysystems.at1config.windows;

import com.dynamicgravitysystems.at1config.util.ResourceManager;
import com.dynamicgravitysystems.at1config.util.WindowFXML;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplicationWindow extends BaseWindow {

    private static final String TITLE = "AT1 Configurator";

    public MainApplicationWindow(Stage mainStage) throws IOException {
        super(mainStage, WindowFXML.MAIN_WINDOW.getUrl(), 600, 800);

        setTitle(TITLE);
        setIcon(ResourceManager.LOGO_PNG.getResource());
    }

}
