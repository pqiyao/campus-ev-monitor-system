package com.example.evmonitorplatform.vo.response;

import lombok.Data;

@Data
public class WeatherVO {
    private String condition;
    private Integer temperature;
    private Integer humidity;
    private Integer windSpeed;
    private String updateTime;
}