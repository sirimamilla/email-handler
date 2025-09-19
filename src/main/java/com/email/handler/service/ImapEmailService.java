package com.email.handler.service;

import com.email.handler.config.EmailHandlerProperties;
import com.email.handler.model.EmailAttachment;
import com.email.handler.model.EmailMessage;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class ImapEmailService {

    private static final Logger logger = LoggerFactory.getLogger(ImapEmailService.class);

    @Autowired
    private EmailHandlerProperties properties;

    public Store connectToImapServer() throws MessagingException {
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imaps");
        props.setProperty("mail.imaps.host", properties.getImap().getHost());
        props.setProperty("mail.imaps.port", String.valueOf(properties.getImap().getPort()));
        props.setProperty("mail.imaps.ssl.enable", String.valueOf(properties.getImap().isSslEnabled()));
        
        Session session = Session.getInstance(props);
        Store store = session.getStore("imaps");
        store.connect(
            properties.getImap().getHost(),
            properties.getImap().getUsername(),
            properties.getImap().getPassword()
        );
        
        return store;
    }

    public List<EmailMessage> fetchEmails() throws MessagingException {
        List<EmailMessage> emails = new ArrayList<>();
        Store store = null;
        Folder folder = null;
        
        try {
            store = connectToImapServer();
            folder = store.getFolder(properties.getImap().getFolder());
            folder.open(Folder.READ_ONLY);
            
            int messageCount = folder.getMessageCount();
            if (messageCount == 0) {
                return emails;
            }
            
            // Fetch only the latest messages up to the configured limit
            int maxMessages = properties.getImap().getMaxMessagesPerFetch();
            int startIndex = Math.max(1, messageCount - maxMessages + 1);
            
            Message[] messages = folder.getMessages(startIndex, messageCount);
            
            for (Message message : messages) {
                try {
                    EmailMessage emailMessage = parseMessage((MimeMessage) message);
                    emails.add(emailMessage);
                } catch (Exception e) {
                    logger.error("Error parsing message: {}", e.getMessage(), e);
                }
            }
            
        } finally {
            if (folder != null && folder.isOpen()) {
                folder.close(false);
            }
            if (store != null && store.isConnected()) {
                store.close();
            }
        }
        
        return emails;
    }

    private EmailMessage parseMessage(MimeMessage message) throws MessagingException, IOException {
        EmailMessage emailMessage = new EmailMessage();
        
        // Basic message properties
        emailMessage.setMessageId(message.getMessageID());
        emailMessage.setSubject(message.getSubject());
        emailMessage.setFrom(message.getFrom()[0].toString());
        
        // Extract all headers
        Map<String, String> headers = new HashMap<>();
        Enumeration<Header> headerEnum = message.getAllHeaders();
        while (headerEnum.hasMoreElements()) {
            Header header = headerEnum.nextElement();
            headers.put(header.getName(), header.getValue());
        }
        emailMessage.setHeaders(headers);
        
        // Parse content and attachments
        List<EmailAttachment> attachments = new ArrayList<>();
        StringBuilder contentBuilder = new StringBuilder();
        
        parseMessageContent(message, contentBuilder, attachments);
        
        emailMessage.setContent(contentBuilder.toString());
        emailMessage.setAttachments(attachments);
        
        return emailMessage;
    }

    private void parseMessageContent(Part part, StringBuilder content, List<EmailAttachment> attachments) 
            throws MessagingException, IOException {
        
        if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
            content.append(part.getContent().toString());
        } else if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                parseMessageContent(multipart.getBodyPart(i), content, attachments);
            }
        } else if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || 
                   part.getFileName() != null) {
            // Handle attachment
            String filename = part.getFileName();
            String contentType = part.getContentType();
            
            InputStream inputStream = part.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            EmailAttachment attachment = new EmailAttachment(filename, contentType, outputStream.toByteArray());
            attachments.add(attachment);
            
            inputStream.close();
            outputStream.close();
        }
    }
}