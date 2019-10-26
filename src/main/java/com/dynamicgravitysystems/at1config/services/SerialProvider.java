package com.dynamicgravitysystems.at1config.services;

import com.fazecast.jSerialComm.SerialPort;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class SerialProvider {
    private static final int DATA_BITS = 8;
    private final int baudRate;

    private final PublishSubject<String> dataPublisher;
    private final SerialPort serialPort;

    public SerialProvider(final String port, final int baudRate) {
        this.baudRate = baudRate;

        dataPublisher = PublishSubject.create();

        Observable<String> serialObserver = Observable.using(this::createResource, this::createObservable, this::disposer);
        serialObserver.subscribe(dataPublisher);

        serialPort = SerialPort.getCommPort(port);
    }


    public Observable<String> getDataObservable() {
        return dataPublisher;
    }

    private BufferedReader createResource() {
        serialPort.setComPortParameters(baudRate, DATA_BITS, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);
        serialPort.openPort();


        return new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
    }

    private Observable<String> createObservable(final BufferedReader reader) {

        return Observable.interval(0, 1000, TimeUnit.MILLISECONDS).flatMap(val -> {
            return Observable.just(reader.readLine())
                    .onErrorResumeNext(Observable.empty());
        });

    }

    private void disposer(BufferedReader reader) throws IOException {
        System.out.println("Closing serial port");
        reader.close();
        serialPort.closePort();
    }


}
