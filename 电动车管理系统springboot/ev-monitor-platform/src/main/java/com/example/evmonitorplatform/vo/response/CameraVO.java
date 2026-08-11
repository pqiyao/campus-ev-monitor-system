package com.example.evmonitorplatform.vo.response;

import lombok.Data;

@Data
public class CameraVO {
    private Integer id;
    private String name;
    private String location;
    private String status;
    private Double lng;
    private Double lat;
    private String quality;
    private String rtspUrl;
}