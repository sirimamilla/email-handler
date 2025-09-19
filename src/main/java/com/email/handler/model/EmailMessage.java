package com.email.handler.model;

import java.util.List;
import java.util.Map;

public class EmailMessage {
    
    private String messageId;
    private String subject;
    private String from;
    private String to;
    private String content;
    private String contentType;
    private Map<String, String> headers;
    private List<EmailAttachment> attachments;
    
    public EmailMessage() {}
    
    // Getters and setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    
    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    
    public List<EmailAttachment> getAttachments() { return attachments; }
    public void setAttachments(List<EmailAttachment> attachments) { this.attachments = attachments; }
}