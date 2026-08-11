package com.example.evmonitorplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.evmonitorplatform.entity.PassRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PassRecordMapper extends BaseMapper<PassRecord> {

    @Select("SELECT * FROM pass_record WHERE deleted = 0 ORDER BY pass_time DESC LIMIT #{limit}")
    List<PassRecord> selectRecentRecords(@Param("limit") Integer limit);

    @Select("SELECT COUNT(*) FROM pass_record WHERE deleted = 0 AND DATE(pass_time) = CURDATE()")
    Integer countToday();

    @Select("SELECT COUNT(*) FROM pass_record WHERE deleted = 0 AND DATE(pass_time) = CURDATE() AND direction = 'in'")
    Integer countTodayIn();

    @Select("SELECT COUNT(*) FROM pass_record WHERE deleted = 0 AND DATE(pass_time) = CURDATE() AND direction = 'out'")
    Integer countTodayOut();
}
