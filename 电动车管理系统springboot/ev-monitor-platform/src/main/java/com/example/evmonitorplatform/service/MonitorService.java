package com.example.evmonitorplatform.service;

import com.example.evmonitorplatform.vo.response.AlertVO;
import com.example.evmonitorplatform.vo.response.CameraVO;
import com.example.evmonitorplatform.vo.response.DashboardVO;
import com.example.evmonitorplatform.vo.response.WeatherVO;
import java.util.List;
import java.util.Map;

public interface MonitorService {
    
    DashboardVO getDashboardData();
    
    List<AlertVO> getRecentAlerts(int limit);
    
    List<CameraVO> getAllCameras();
    
    WeatherVO getWeather();
    
    Map<String, Object> getCaseStatistics();
    
    Map<String, Object> getMapData();
    
    Map<String, Object> getRealtimeData();
}