package com.dynamicgravitysystems.at1config.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.EnumMap;

public class DataWriter {
    private final EnumMap<DataSource, File> dataFileMap = new EnumMap<>(DataSource.class);

    public DataWriter() {

        File exFile = new File("./test.dat");


        try {
            BufferedWriter writer = Files.newBufferedWriter(exFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }

    }


    public void writeLine(DataSource source, String line) {

    }

}
