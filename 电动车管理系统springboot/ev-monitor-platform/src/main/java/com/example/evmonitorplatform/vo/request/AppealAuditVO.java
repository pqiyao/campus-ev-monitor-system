package com.example.evmonitorplatform.vo.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "申诉审核请求参数")
public class AppealAuditVO {

    @NotNull(message = "申诉ID不能为空")
    @Schema(description = "申诉记录ID", required = true, example = "1")
    private Integer appealId;

    @NotNull(message = "审核用户ID不能为空")
    @Schema(description = "审核用户ID", required = true, example = "1002")
    private Integer auditUserId;

    @NotNull(message = "审核结果不能为空")
    @Schema(description = "审核结果：1-通过, 2-拒绝, 3-部分通过, 4-转办", required = true, example = "1")
    private Integer appealResult;

    @NotBlank(message = "审核意见不能为空")
    @Schema(description = "审核意见", required = true, example = "经核实，情况属实，同意撤销违规记录")
    private String auditOpinion;

    @Schema(description = "拒绝原因（当结果为拒绝时填写）", example = "证据不足，无法证明当时情况")
    private String rejectReason;

    @Schema(description = "审核证据照片（多个用逗号分隔）", example = "http://example.com/audit1.jpg")
    private String evidencePhotos;

    @Schema(description = "后续处理建议", example = "建议加强该区域的管理")
    private String handlingSuggestion;

    @Schema(description = "是否通知用户", example = "true")
    private Boolean notifyUser = true;

    @Schema(description = "通知方式：sms-短信, app-APP推送, both-两者都", example = "app")
    private String notifyType = "app";
}