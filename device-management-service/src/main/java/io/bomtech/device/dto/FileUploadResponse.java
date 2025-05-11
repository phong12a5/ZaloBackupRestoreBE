package io.bomtech.device.dto;

public class FileUploadResponse {
    private String message;
    private String filePath;

    public FileUploadResponse(String message, String filePath) {
        this.message = message;
        this.filePath = filePath;
    }

    public String getMessage() {
        return message;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
