module com.dynamicgravitysystems.at1config {

    requires javafx.controls;
    requires javafx.fxml;
    requires com.fazecast.jSerialComm;
    requires io.reactivex.rxjava2;
    requires com.dynamicgravitysystems.common;
    requires org.apache.logging.log4j;

    opens com.dynamicgravitysystems.at1config to javafx.graphics;
    opens com.dynamicgravitysystems.at1config.controllers to javafx.fxml;
}