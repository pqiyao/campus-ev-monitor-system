package com.example.evmonitorplatform.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@TableName("etc_card")
public class ETCard {
    @TableId(type = IdType.AUTO)
    private Integer etcId;

    private Integer vehicleId;

    private String etcNumber;

    private BigDecimal balance;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate issueDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expireDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String licensePlate;

    @TableField(exist = false)
    private String ownerName;

    @TableField(exist = false)
    private String phone;

    @TableField(exist = false)
    private String userType;
}