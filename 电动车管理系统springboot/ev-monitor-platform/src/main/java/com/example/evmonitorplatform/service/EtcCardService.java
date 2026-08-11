package com.example.evmonitorplatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.evmonitorplatform.common.response.PageResponse;
import com.example.evmonitorplatform.entity.ETCard;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface EtcCardService extends IService<ETCard> {

    void bindEtcToVehicle(String etcNumber, Integer vehicleId);

    void issueToVehicle(Integer vehicleId, String etcNumber, LocalDate expireDate);

    PageResponse<List<ETCard>> listCards(String keyword, String status, String userType, Integer pageNum, Integer pageSize);

    void updateCardStatus(Integer etcId, String status);

    Map<String, Object> cardStatusStats();
}
