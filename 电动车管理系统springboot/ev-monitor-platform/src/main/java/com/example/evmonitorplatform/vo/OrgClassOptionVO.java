package com.example.evmonitorplatform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "班级下拉项（标准库或已有学生班级）")
public class OrgClassOptionVO {

    @Schema(description = "org_class 主键，来自学生去重时为 null")
    private Integer id;

    private String college;

    private String major;

    private String className;
}
