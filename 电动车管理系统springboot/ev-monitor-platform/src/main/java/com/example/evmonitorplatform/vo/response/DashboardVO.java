package com.example.evmonitorplatform.vo.response;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DashboardVO {
    private Map<String, Object> statistics;
    private List<AlertVO> alerts;
    private List<CameraVO> cameras;
    private WeatherVO weather;
    private Map<String, Object> mapData;
    private String currentTime;
    private String currentDate;
}