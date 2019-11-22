package com.dynamicgravitysystems.at1config.windows;

import com.dynamicgravitysystems.at1config.util.DataSource;
import com.dynamicgravitysystems.at1config.util.LoggingServiceConfiguration;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;

public class LogConfigurationDialog extends Dialog<LoggingServiceConfiguration> {

    private final GridPane grid;
    private final TextField filePathField;
    private final Button btnBrowse;
    private final CheckBox checkEnabled;
    private final CheckBox checkAppend;

    private ObjectProperty<Path> selectedPath = new SimpleObjectProperty<>(null);

    public LogConfigurationDialog(DataSource source, LoggingServiceConfiguration currentConfiguration) {
        final DialogPane dialogPane = getDialogPane();

        setTitle("Logging Configuration");
        filePathField = new TextField();
        filePathField.setEditable(false);
        filePathField.textProperty().bind(Bindings.createStringBinding(() -> {
            Path path = selectedPath.getValue();
            if (path != null)
                return path.toAbsolutePath().toString();
            return "";
        }, selectedPath));

        if (currentConfiguration.getFilePath() != null) {
            selectedPath.set(currentConfiguration.getFilePath());
        }
        btnBrowse = new Button("Browse");
        btnBrowse.setOnAction(event -> chooseFile());

        checkEnabled = new CheckBox("Logging Enabled?");
        checkEnabled.setSelected(currentConfiguration.isEnabled());
        checkAppend = new CheckBox("Append?");
        checkAppend.setSelected(currentConfiguration.isAppend());

        grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(filePathField, 0, 0);
        grid.add(btnBrowse, 1, 0);
        grid.add(checkEnabled, 0, 1);
        grid.add(checkAppend, 0, 2);

        dialogPane.setContent(grid);
        dialogPane.getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);
        dialogPane.setHeaderText("Logging Configuration: " + source);

        setResultConverter((dialogButton) -> {
            ButtonBar.ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonBar.ButtonData.APPLY
                    ? new LoggingServiceConfiguration(selectedPath.getValue(), checkAppend.isSelected(), checkEnabled.isSelected())
                    : null;
        });

    }

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select log destination");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Log File", "*.log"),
                new FileChooser.ExtensionFilter("Data File", "*.dat")
        );

        File selected = chooser.showSaveDialog(getOwner());
        if (selected != null) {
            selectedPath.set(selected.toPath());
        }

    }
}
