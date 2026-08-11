package com.example.evmonitorplatform.common.exception;

import com.example.evmonitorplatform.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import jakarta.validation.ConstraintViolationException;
import org.apache.ibatis.exceptions.PersistenceException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ApiResponse.error(400, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体无法解析: {}", e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage());
        return ApiResponse.error(400, "请求 JSON 格式或字段类型不正确（例如手机号请使用字符串 \"13800000000\"）");
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return ApiResponse.error(400, message);
    }
    
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", message);
        return ApiResponse.error(400, message);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return ApiResponse.error(400, message);
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("文件大小超出限制");
        return ApiResponse.error(400, "文件大小超出限制");
    }

    /**
     * 外键、唯一约束、非空等数据库错误，注册/绑定时常见；返回 400 便于前端展示，避免笼统 500。
     */
    /**
     * MyBatis 包装后的 SQL 异常（如表不存在），避免直接落到 500。
     */
    @ExceptionHandler(PersistenceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handlePersistenceException(PersistenceException e) {
        Throwable c = e.getCause();
        String msg = c != null ? c.getMessage() : e.getMessage();
        log.warn("MyBatis 持久化异常: {}", msg);
        if (msg != null && (msg.contains("doesn't exist") || msg.contains("Unknown table") || msg.contains("不存在"))) {
            return ApiResponse.error(400, "数据库表缺失或结构不匹配：请重启后端以自动创建 notification_user_inbox，或手动执行 resources/db/notification_user_inbox.sql");
        }
        return ApiResponse.error(400, "数据操作失败，请稍后重试或查看服务端日志");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String cause = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
        log.warn("数据约束异常: {}", cause);
        String hint = "数据无法保存（可能重复或字段超长/外键不存在）";
        if (cause != null) {
            if (cause.contains("Duplicate")) {
                hint = "记录已存在（如用户名、手机号、车牌或车架号重复）";
            } else if (cause.contains("foreign key") || cause.contains("FOREIGN KEY") || cause.contains("Cannot add or update")) {
                hint = "关联数据无效（如用户不存在或已被删除）";
            } else if (cause.contains("Data too long")) {
                hint = "某字段内容过长（如图片地址），请缩短或联系管理员调整库表字段长度";
            }
        }
        return ApiResponse.error(400, hint);
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        return ApiResponse.error(500, "系统内部错误");
    }
}