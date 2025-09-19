package com.email.handler.model;

public class EmailAttachment {
    
    private String filename;
    private String contentType;
    private byte[] content;
    private long size;
    private boolean isAudioVideo;
    private String transcript;
    
    public EmailAttachment() {}
    
    public EmailAttachment(String filename, String contentType, byte[] content) {
        this.filename = filename;
        this.contentType = contentType;
        this.content = content;
        this.size = content != null ? content.length : 0;
    }
    
    // Getters and setters
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    
    public byte[] getContent() { return content; }
    public void setContent(byte[] content) { 
        this.content = content;
        this.size = content != null ? content.length : 0;
    }
    
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    
    public boolean isAudioVideo() { return isAudioVideo; }
    public void setAudioVideo(boolean audioVideo) { this.isAudioVideo = audioVideo; }
    
    public String getTranscript() { return transcript; }
    public void setTranscript(String transcript) { this.transcript = transcript; }
}