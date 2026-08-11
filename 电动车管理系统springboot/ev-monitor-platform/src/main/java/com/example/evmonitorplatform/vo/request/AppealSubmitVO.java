package com.example.evmonitorplatform.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "申诉提交请求参数")
public class AppealSubmitVO {

    @NotNull(message = "违规ID不能为空")
    @Schema(description = "违规记录ID", required = true, example = "1")
    private Integer illegalId;

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID", required = true, example = "1001")
    private Integer userId;

    @NotBlank(message = "申诉理由不能为空")
    @Schema(description = "申诉理由", required = true, example = "当时有紧急情况需要临时停车")
    private String appealReason;

    @Schema(description = "申诉证据（图片URL，多个用逗号分隔）", example = "http://example.com/photo1.jpg,http://example.com/photo2.jpg")
    private String appealEvidence;

    @Schema(description = "联系人姓名", example = "张三")
    private String contactName;

    @Schema(description = "联系电话", example = "13800138000")
    private String contactPhone;

    @Schema(description = "紧急程度：1-一般, 2-紧急, 3-特急", example = "1")
    private Integer urgencyLevel;

    @Schema(description = "期望处理方式", example = "取消违规记录")
    private String expectedHandling;
}