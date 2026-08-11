package com.example.evmonitorplatform.common.utils;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class DateUtil {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    /**
     * 获取当前日期字符串 (yyyy-MM-dd)
     */
    public static String getCurrentDate() {
        return LocalDate.now().format(DATE_FORMATTER);
    }
    
    /**
     * 获取当前日期时间字符串 (yyyy-MM-dd HH:mm:ss)
     */
    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }
    
    /**
     * 获取当前时间字符串 (HH:mm:ss)
     */
    public static String getCurrentTime() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }
    
    /**
     * 格式化日期
     */
    public static String formatDate(LocalDate date) {
        if (date == null) return null;
        return date.format(DATE_FORMATTER);
    }
    
    /**
     * 格式化日期时间
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DATETIME_FORMATTER);
    }
    
    /**
     * 解析日期字符串
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }
    
    /**
     * 解析日期时间字符串
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) return null;
        return LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER);
    }
    
    /**
     * 计算两个日期之间的天数差
     */
    public static long daysBetween(LocalDate startDate, LocalDate endDate) {
        return ChronoUnit.DAYS.between(startDate, endDate);
    }
    
    /**
     * 计算两个日期时间之间的小时差
     */
    public static long hoursBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return ChronoUnit.HOURS.between(startTime, endTime);
    }
    
    /**
     * 添加天数
     */
    public static LocalDate addDays(LocalDate date, int days) {
        return date.plusDays(days);
    }
    
    /**
     * 添加小时
     */
    public static LocalDateTime addHours(LocalDateTime dateTime, int hours) {
        return dateTime.plusHours(hours);
    }
    
    /**
     * 获取日期是一周中的第几天 (1=星期一, 7=星期日)
     */
    public static int getDayOfWeek(LocalDate date) {
        int day = date.getDayOfWeek().getValue();
        return day == 7 ? 0 : day; // 调整为中国习惯 (0=星期日, 1=星期一)
    }
    
    /**
     * 获取月份的天数
     */
    public static int getDaysInMonth(int year, int month) {
        return LocalDate.of(year, month, 1).lengthOfMonth();
    }
    
    /**
     * Date 转 LocalDateTime
     */
    public static LocalDateTime dateToLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
    }
    
    /**
     * LocalDateTime 转 Date
     */
    public static Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
    }
    
    /**
     * 判断日期是否在范围内
     */
    public static boolean isDateInRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
    
    /**
     * 判断日期时间是否在范围内
     */
    public static boolean isDateTimeInRange(LocalDateTime dateTime, LocalDateTime startTime, LocalDateTime endTime) {
        return !dateTime.isBefore(startTime) && !dateTime.isAfter(endTime);
    }
    
    /**
     * 获取年龄
     */
    public static int getAge(LocalDate birthDate) {
        return (int) ChronoUnit.YEARS.between(birthDate, LocalDate.now());
    }
    
    /**
     * 获取季度
     */
    public static int getQuarter(LocalDate date) {
        int month = date.getMonthValue();
        return (month - 1) / 3 + 1;
    }
}