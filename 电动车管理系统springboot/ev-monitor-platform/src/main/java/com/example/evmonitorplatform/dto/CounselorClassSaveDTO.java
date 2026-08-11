package com.example.evmonitorplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CounselorClassSaveDTO {
    @NotNull
    private Integer counselorUserId;
    private String college;
    private String major;
    @NotBlank
    private String className;
}
