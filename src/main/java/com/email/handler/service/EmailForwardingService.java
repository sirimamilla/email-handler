package com.email.handler.service;

import com.email.handler.config.EmailHandlerProperties;
import com.email.handler.model.EmailAttachment;
import com.email.handler.model.EmailMessage;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;

@Service
public class EmailForwardingService {

    private static final Logger logger = LoggerFactory.getLogger(EmailForwardingService.class);

    @Autowired
    private EmailHandlerProperties properties;

    public void forwardEmail(EmailMessage originalEmail) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.host", properties.getSmtp().getHost());
        props.put("mail.smtp.port", properties.getSmtp().getPort());
        props.put("mail.smtp.auth", "true");
        
        if (properties.getSmtp().isStarttlsEnabled()) {
            props.put("mail.smtp.starttls.enable", "true");
        }

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                    properties.getSmtp().getUsername(),
                    properties.getSmtp().getPassword()
                );
            }
        });

        MimeMessage forwardedMessage = new MimeMessage(session);
        
        // Set basic properties
        forwardedMessage.setFrom(new InternetAddress(properties.getSmtp().getUsername()));
        forwardedMessage.setRecipients(Message.RecipientType.TO, 
            InternetAddress.parse(properties.getSmtp().getToAddress()));
        
        // Forward the subject with prefix
        String forwardedSubject = "Fwd: " + (originalEmail.getSubject() != null ? originalEmail.getSubject() : "");
        forwardedMessage.setSubject(forwardedSubject);

        // Preserve important headers
        preserveHeaders(originalEmail, forwardedMessage);

        // Create multipart message
        Multipart multipart = new MimeMultipart();
        
        // Add original content
        MimeBodyPart textPart = new MimeBodyPart();
        StringBuilder contentBuilder = new StringBuilder();
        
        // Add forwarding header
        contentBuilder.append("---------- Forwarded message ----------\n");
        contentBuilder.append("From: ").append(originalEmail.getFrom()).append("\n");
        contentBuilder.append("Subject: ").append(originalEmail.getSubject()).append("\n\n");
        
        // Add original content
        if (originalEmail.getContent() != null) {
            contentBuilder.append(originalEmail.getContent());
        }
        
        // Add transcripts if any
        addTranscriptsToContent(originalEmail, contentBuilder);
        
        textPart.setText(contentBuilder.toString());
        multipart.addBodyPart(textPart);

        // Add original attachments
        if (originalEmail.getAttachments() != null) {
            for (EmailAttachment attachment : originalEmail.getAttachments()) {
                addAttachmentToMessage(attachment, multipart);
            }
        }

        forwardedMessage.setContent(multipart);
        
        // Send the message
        Transport.send(forwardedMessage);
        
        logger.info("Successfully forwarded email with subject: {}", forwardedSubject);
    }

    private void preserveHeaders(EmailMessage originalEmail, MimeMessage forwardedMessage) throws MessagingException {
        if (originalEmail.getHeaders() != null) {
            for (Map.Entry<String, String> header : originalEmail.getHeaders().entrySet()) {
                String headerName = header.getKey();
                String headerValue = header.getValue();
                
                // Skip headers that shouldn't be copied
                if (!shouldSkipHeader(headerName)) {
                    try {
                        forwardedMessage.addHeader("X-Original-" + headerName, headerValue);
                    } catch (Exception e) {
                        logger.warn("Failed to add header {}: {}", headerName, e.getMessage());
                    }
                }
            }
        }
    }

    private boolean shouldSkipHeader(String headerName) {
        // Skip headers that would interfere with forwarding
        String lowerName = headerName.toLowerCase();
        return lowerName.equals("message-id") ||
               lowerName.equals("date") ||
               lowerName.equals("from") ||
               lowerName.equals("to") ||
               lowerName.equals("cc") ||
               lowerName.equals("bcc") ||
               lowerName.equals("reply-to") ||
               lowerName.equals("subject") ||
               lowerName.startsWith("content-");
    }

    private void addTranscriptsToContent(EmailMessage originalEmail, StringBuilder contentBuilder) {
        if (originalEmail.getAttachments() != null) {
            boolean hasTranscripts = false;
            StringBuilder transcriptSection = new StringBuilder();
            
            for (EmailAttachment attachment : originalEmail.getAttachments()) {
                if (attachment.isAudioVideo() && attachment.getTranscript() != null) {
                    if (!hasTranscripts) {
                        transcriptSection.append("\n\n---------- Audio/Video Transcripts ----------\n");
                        hasTranscripts = true;
                    }
                    transcriptSection.append("\nFile: ").append(attachment.getFilename()).append("\n");
                    transcriptSection.append("Transcript:\n").append(attachment.getTranscript()).append("\n");
                    transcriptSection.append("---\n");
                }
            }
            
            if (hasTranscripts) {
                contentBuilder.append(transcriptSection.toString());
            }
        }
    }

    private void addAttachmentToMessage(EmailAttachment attachment, Multipart multipart) throws MessagingException {
        MimeBodyPart attachmentPart = new MimeBodyPart();
        
        DataSource dataSource = new ByteArrayDataSource(
            attachment.getContent(), 
            attachment.getContentType(),
            attachment.getFilename()
        );
        
        attachmentPart.setDataHandler(new DataHandler(dataSource));
        attachmentPart.setFileName(attachment.getFilename());
        
        multipart.addBodyPart(attachmentPart);
    }

    // Custom DataSource implementation for byte array attachments
    private static class ByteArrayDataSource implements DataSource {
        private final byte[] data;
        private final String contentType;
        private final String name;

        public ByteArrayDataSource(byte[] data, String contentType, String name) {
            this.data = data;
            this.contentType = contentType;
            this.name = name;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(data);
        }

        @Override
        public OutputStream getOutputStream() {
            throw new UnsupportedOperationException("Read-only data source");
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}