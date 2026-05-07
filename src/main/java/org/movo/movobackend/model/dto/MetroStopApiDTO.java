package org.movo.movobackend.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class MetroStopApiDTO {
    private String id;
    private String name;
    private Double lat;
    private Double lon;
    private List<Integer> lines;
}