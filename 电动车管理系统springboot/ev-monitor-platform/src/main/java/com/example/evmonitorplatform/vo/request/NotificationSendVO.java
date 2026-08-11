package com.example.evmonitorplatform.vo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class NotificationSendVO {
    @NotBlank(message = "标题不能为空")
    private String title;
    
    @NotBlank(message = "内容不能为空")
    private String content;
    
    @NotBlank(message = "类型不能为空")
    private String type;
    
    private Boolean isUrgent;
    
    @NotNull(message = "接收类型不能为空")
    private String receiverType;
    
    private List<String> receiverIds;
}