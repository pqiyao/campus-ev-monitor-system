package com.example.evmonitorplatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.evmonitorplatform.common.response.PageResponse;
import com.example.evmonitorplatform.entity.PassRecord;

import java.util.List;
import java.util.Map;

public interface PassRecordService extends IService<PassRecord> {

    void savePassRecord(PassRecord passRecord);

    PageResponse<List<PassRecord>> queryPassRecords(String keyword, String direction, String passResult,
                                       Integer vehicleId, Integer userId,
                                       Integer pageNum, Integer pageSize);

    List<PassRecord> getRecentRecords(Integer limit);

    Map<String, Object> getPassStatistics();
}
