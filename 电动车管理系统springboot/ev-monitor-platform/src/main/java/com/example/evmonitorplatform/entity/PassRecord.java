package com.example.evmonitorplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("pass_record")
public class PassRecord {
    @TableId(type = IdType.AUTO)
    private Integer passId;

    private String etcNumber;

    private Integer vehicleId;

    private String licensePlate;

    private Integer userId;

    private String readerId;

    private String readerName;

    private String direction;

    private String passResult;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime passTime;

    private String imageUrl;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableLogic
    private Boolean deleted;

    @TableField(exist = false)
    private String ownerName;

    @TableField(exist = false)
    private String phone;
}
