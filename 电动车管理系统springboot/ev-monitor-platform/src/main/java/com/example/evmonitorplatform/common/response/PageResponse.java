package com.example.evmonitorplatform.common.response;

import lombok.Data;
import java.util.List;

@Data
public class PageResponse<T> {
    private int code;
    private String message;
    private T data;
    private int pageNum;
    private int pageSize;
    private long total;
    private int totalPages;
    private long timestamp;
    
    public PageResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public PageResponse(int code, String message, T data, int pageNum, int pageSize, long total) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.totalPages = (int) Math.ceil((double) total / pageSize);
        this.timestamp = System.currentTimeMillis();
    }
    
    public static <T> PageResponse<T> success(T data, int pageNum, int pageSize, long total) {
        return new PageResponse<>(200, "success", data, pageNum, pageSize, total);
    }
    
    public static <T> PageResponse<T> error(String message) {
        PageResponse<T> response = new PageResponse<>();
        response.setCode(500);
        response.setMessage(message);
        return response;
    }
}