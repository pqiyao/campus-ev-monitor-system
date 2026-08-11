package com.example.evmonitorplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.evmonitorplatform.entity.RectifyRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface RectifyRecordMapper extends BaseMapper<RectifyRecord> {
    
    IPage<RectifyRecord> selectRectifyPage(Page<RectifyRecord> page, 
                                          @Param("illegalId") Integer illegalId,
                                          @Param("verifyState") Integer verifyState);

    IPage<RectifyRecord> selectRectifyListPage(Page<RectifyRecord> page,
                                               @Param("verifyState") Integer verifyState,
                                               @Param("licensePlate") String licensePlate);
    
    @Select("SELECT r.*, u.real_name as user_name, u.user_type as user_role " +
            "FROM illegal_rectify_record r " +
            "LEFT JOIN `user` u ON r.user_id = u.user_id " +
            "WHERE r.illegal_id = #{illegalId}")
    RectifyRecord selectWithUserInfo(@Param("illegalId") Integer illegalId);
    
    @Select("SELECT r.*, u.real_name as user_name, u.user_type as user_role, " +
            "vu.real_name as verify_user_name " +
            "FROM illegal_rectify_record r " +
            "LEFT JOIN `user` u ON r.user_id = u.user_id " +
            "LEFT JOIN `user` vu ON r.verify_user_id = vu.user_id " +
            "WHERE r.rectify_id = #{rectifyId}")
    RectifyRecord selectWithDetail(@Param("rectifyId") Integer rectifyId);
}