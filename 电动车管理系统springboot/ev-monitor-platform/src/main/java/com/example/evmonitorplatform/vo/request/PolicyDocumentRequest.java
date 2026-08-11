package com.example.evmonitorplatform.vo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PolicyDocumentRequest {
    @NotBlank(message = "制度标题不能为空")
    private String title;
    private String category = "CAMPUS_RULE";
    private String version;
    private String source;
    @NotBlank(message = "制度正文不能为空")
    @Size(max = 30000, message = "制度正文不能超过30000字")
    private String content;
}
