package com.example.evmonitorplatform.vo.response;

import lombok.Data;

@Data
public class AlertVO {
    private String location;
    private String time;
    private String description;
    private String level;
    private String status;
}