package com.dynamicgravitysystems.at1config.windows;

import com.dynamicgravitysystems.at1config.util.ResourceManager;
import com.dynamicgravitysystems.at1config.util.WindowFXML;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class TimeSynchronizerWindow extends BaseWindow {

    public TimeSynchronizerWindow(Stage owner) throws IOException {
        super(new Stage(), WindowFXML.TIME_SYNC.getUrl(), 700, 800);

        getStage().initOwner(owner);
        getStage().initModality(Modality.APPLICATION_MODAL);

        setTitle("AT1 Time Synchronization");
        setIcon(ResourceManager.LOGO_PNG.getResource());
    }
}
