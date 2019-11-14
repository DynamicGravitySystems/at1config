AT1CONFIG Readme
================

Overview
--------

- AT1 Config (AT1C) is a graphical interface designed to allow users to diagnose and configure various aspects of their 
AT1A/AT1M Gravity Meter System
- AT1C allows the user to connect to the raw gravity or gps serial stream to view & validate the raw data sent by sensor
or GPS receiver, without resorting to unfriendly tools such as HyperTerm or Putty
- AT1C allows the user to send commands to the sensor e.g. Clamp/Unclamp, Synchronize Time etc.
- AT1C provides a graphical time synchronization tool which allows the user to start and view the progress of the sensor
as it synchronizes its internal Analog-Digital clock to the GPS aided 1PPS frequency signal.

Implementation Details
----------------------
- AT1C is programmed using Java 11, and the JavaFX 11 GUI framework
- The Apache Maven build tool is utilized to resolve dependencies, compile, and package the application
- Launch4j is used to generate an executable (.exe) wrapper for the application, allowing for ease of use on client's 
windows computers, requiring no installation, only the presence of the bundled Java Runtime Environment (JRE), which
can be distributed together with the executable in a zip file.

Building
--------

Building the project from source:

- Maven >= 3.5 is required
- Java >= 11 is required (should be compatible with any OpenJDK package e.g. AdoptOpenJDK)
- atgmlogger-common is required (must be installed in local maven repository)

The following will compile and automatically package the application as a JAR and .exe file, which will be located under
the ./target directory
```shell script
mvn clean package
```

The following command can be used to compile and execute the source from an IDE
```shell script
mvn clean javafx:run
```