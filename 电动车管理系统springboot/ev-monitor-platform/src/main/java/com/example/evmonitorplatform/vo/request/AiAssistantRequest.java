package com.example.evmonitorplatform.vo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiAssistantRequest {
    private String sessionId;
    @NotBlank(message = "问题不能为空")
    @Size(max = 1000, message = "问题不能超过1000字")
    private String question;
    private Long contextEventId;
    private Integer appealId;
}
