package org.movo.movobackend.model;

import lombok.Data;

@Data
public class MetroInfoPanel {
    private String service;
    private String stop;
    private int stopSAE;
    private String destinationStop;
    private String stopDescription;
    private String destinationStopDescription;
    private int route;
    private int direction;
    private long lastUpdate;
    private String lastUpdateFormatted;
    private int remainingMinutes;
    private int orderStop;
}
