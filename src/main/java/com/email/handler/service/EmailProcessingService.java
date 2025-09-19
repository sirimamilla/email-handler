package com.email.handler.service;

import com.email.handler.config.EmailHandlerProperties;
import com.email.handler.model.EmailMessage;
import com.email.handler.model.ProcessedEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(EmailProcessingService.class);

    @Autowired
    private ImapEmailService imapEmailService;

    @Autowired
    private DuplicatePreventionService duplicatePreventionService;

    @Autowired
    private AudioVideoProcessingService audioVideoProcessingService;

    @Autowired
    private EmailForwardingService emailForwardingService;

    @Autowired
    private EmailHandlerProperties properties;

    @Scheduled(fixedDelayString = "#{emailHandlerProperties.imap.fetchInterval}")
    public void processEmails() {
        try {
            logger.debug("Starting email processing cycle");
            
            List<EmailMessage> emails = imapEmailService.fetchEmails();
            
            if (emails.isEmpty()) {
                logger.debug("No new emails found");
                return;
            }
            
            logger.info("Found {} new emails to process", emails.size());
            
            for (EmailMessage email : emails) {
                processEmailAsync(email);
            }
            
        } catch (Exception e) {
            logger.error("Error during email processing cycle: {}", e.getMessage(), e);
        }
    }

    @Async("emailProcessingExecutor")
    public CompletableFuture<Void> processEmailAsync(EmailMessage email) {
        return CompletableFuture.runAsync(() -> {
            try {
                processSingleEmail(email);
            } catch (Exception e) {
                logger.error("Error processing email {}: {}", email.getMessageId(), e.getMessage(), e);
                duplicatePreventionService.updateProcessingStatus(
                    email.getMessageId(),
                    ProcessedEmail.ProcessingStatus.FAILED,
                    e.getMessage()
                );
            }
        });
    }

    private void processSingleEmail(EmailMessage email) {
        String messageId = email.getMessageId();
        
        logger.info("Processing email: {}", messageId);
        
        // Check for duplicates
        if (duplicatePreventionService.isAlreadyProcessed(messageId)) {
            logger.info("Email {} already processed, skipping", messageId);
            return;
        }
        
        try {
            // Mark as received
            duplicatePreventionService.markAsProcessed(messageId, ProcessedEmail.ProcessingStatus.RECEIVED);
            
            // Update status to processing
            duplicatePreventionService.updateProcessingStatus(
                messageId,
                ProcessedEmail.ProcessingStatus.PROCESSING,
                null
            );
            
            // Check if email has audio/video attachments
            boolean hasAudioVideo = email.getAttachments() != null && 
                audioVideoProcessingService.hasAudioVideoAttachments(email.getAttachments());
            
            if (hasAudioVideo) {
                logger.info("Email {} contains audio/video attachments, processing conversions", messageId);
                
                // Process audio/video attachments
                audioVideoProcessingService.processAttachments(email.getAttachments());
                
                // Update status to converted
                duplicatePreventionService.updateProcessingStatus(
                    messageId,
                    ProcessedEmail.ProcessingStatus.CONVERTED,
                    null
                );
            } else {
                logger.info("Email {} contains no audio/video attachments, skipping conversion", messageId);
            }
            
            // Forward the email
            emailForwardingService.forwardEmail(email);
            
            // Mark as successfully forwarded
            duplicatePreventionService.updateProcessingStatus(
                messageId,
                ProcessedEmail.ProcessingStatus.FORWARDED,
                null
            );
            
            logger.info("Successfully processed and forwarded email: {}", messageId);
            
        } catch (Exception e) {
            logger.error("Failed to process email {}: {}", messageId, e.getMessage(), e);
            
            // Mark as failed
            duplicatePreventionService.updateProcessingStatus(
                messageId,
                ProcessedEmail.ProcessingStatus.FAILED,
                e.getMessage()
            );
            
            throw new RuntimeException("Email processing failed", e);
        }
    }

    // Retry failed emails periodically
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void retryFailedEmails() {
        logger.debug("Checking for failed emails to retry");
        
        // This would involve fetching failed emails from database and retrying
        // Implementation depends on retry strategy requirements
        // For now, just log that the check is happening
    }
}