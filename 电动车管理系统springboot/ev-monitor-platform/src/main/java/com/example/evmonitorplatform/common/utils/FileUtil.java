package com.example.evmonitorplatform.common.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.UUID;

public class FileUtil {
    
    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#,##0.##");
    
    /**
     * 格式化文件大小
     */
    public static String formatSize(long size) {
        if (size <= 0) return "0 B";
        
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        
        return SIZE_FORMAT.format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
    
    /**
     * 获取文件扩展名
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex + 1).toLowerCase();
    }
    
    /**
     * 获取文件名（不含扩展名）
     */
    public static String getFileNameWithoutExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? filename : filename.substring(0, dotIndex);
    }
    
    /**
     * 生成唯一的文件名
     */
    public static String generateUniqueFilename(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid + (extension.isEmpty() ? "" : "." + extension);
    }
    
    /**
     * 创建目录（如果不存在）
     */
    public static boolean createDirectoryIfNotExists(String directoryPath) {
        File dir = new File(directoryPath);
        if (!dir.exists()) {
            return dir.mkdirs();
        }
        return true;
    }
    
    /**
     * 删除文件
     */
    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            return file.delete();
        }
        return false;
    }
    
    /**
     * 删除目录及其内容
     */
    public static boolean deleteDirectory(String directoryPath) {
        File dir = new File(directoryPath);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file.getAbsolutePath());
                    } else {
                        file.delete();
                    }
                }
            }
            return dir.delete();
        }
        return false;
    }
    
    /**
     * 获取文件MIME类型
     */
    public static String getMimeType(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return Files.probeContentType(path);
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }
    
    /**
     * 检查文件是否为图片
     */
    public static boolean isImageFile(String filename) {
        String extension = getFileExtension(filename);
        String[] imageExtensions = {"jpg", "jpeg", "png", "gif", "bmp", "webp"};
        for (String ext : imageExtensions) {
            if (ext.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查文件是否为文档
     */
    public static boolean isDocumentFile(String filename) {
        String extension = getFileExtension(filename);
        String[] docExtensions = {"pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt"};
        for (String ext : docExtensions) {
            if (ext.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取目录大小
     */
    public static long getDirectorySize(String directoryPath) {
        long size = 0;
        File dir = new File(directoryPath);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else if (file.isDirectory()) {
                        size += getDirectorySize(file.getAbsolutePath());
                    }
                }
            }
        }
        return size;
    }
    
    /**
     * 获取目录中的文件数量
     */
    public static int getFileCount(String directoryPath) {
        int count = 0;
        File dir = new File(directoryPath);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        count++;
                    } else if (file.isDirectory()) {
                        count += getFileCount(file.getAbsolutePath());
                    }
                }
            }
        }
        return count;
    }
    
    /**
     * 复制文件
     */
    public static boolean copyFile(String sourcePath, String destPath) {
        try {
            Files.copy(Paths.get(sourcePath), Paths.get(destPath));
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 移动文件
     */
    public static boolean moveFile(String sourcePath, String destPath) {
        try {
            Files.move(Paths.get(sourcePath), Paths.get(destPath));
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 检查文件是否可读
     */
    public static boolean isReadable(String filePath) {
        File file = new File(filePath);
        return file.exists() && file.isFile() && file.canRead();
    }
    
    /**
     * 检查文件是否可写
     */
    public static boolean isWritable(String filePath) {
        File file = new File(filePath);
        return file.exists() && file.isFile() && file.canWrite();
    }
}