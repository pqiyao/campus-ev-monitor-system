package com.example.evmonitorplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.evmonitorplatform.entity.GeoFence;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GeoFenceMapper extends BaseMapper<GeoFence> {

    @Select("SELECT * FROM geofence WHERE status = 1 AND deleted = 0")
    List<GeoFence> selectAllEnabled();
}