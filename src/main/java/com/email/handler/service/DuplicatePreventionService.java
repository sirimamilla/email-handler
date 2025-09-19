package com.email.handler.service;

import com.email.handler.config.EmailHandlerProperties;
import com.email.handler.model.ProcessedEmail;
import com.email.handler.repository.ProcessedEmailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class DuplicatePreventionService {

    private static final Logger logger = LoggerFactory.getLogger(DuplicatePreventionService.class);
    private static final String CACHE_KEY_PREFIX = "email:processed:";

    @Autowired
    private EmailHandlerProperties properties;

    @Autowired
    private ProcessedEmailRepository processedEmailRepository;

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    public boolean isAlreadyProcessed(String messageId) {
        if (!properties.getDuplicatePrevention().isEnabled()) {
            return false;
        }

        // Check Redis cache first (faster)
        String cacheKey = CACHE_KEY_PREFIX + messageId;
        if (redisTemplate != null) {
            Boolean cached = redisTemplate.hasKey(cacheKey);
            if (Boolean.TRUE.equals(cached)) {
                logger.debug("Message {} found in cache, already processed", messageId);
                return true;
            }
        }

        // Check database as fallback
        boolean existsInDb = processedEmailRepository.existsByMessageId(messageId);
        if (existsInDb) {
            logger.debug("Message {} found in database, already processed", messageId);
            // Update cache for future lookups
            if (redisTemplate != null) {
                Duration cacheDuration = parseDuration(properties.getDuplicatePrevention().getCacheDuration());
                redisTemplate.opsForValue().set(cacheKey, "processed", cacheDuration.toSeconds(), TimeUnit.SECONDS);
            }
            return true;
        }

        return false;
    }

    public void markAsProcessed(String messageId, ProcessedEmail.ProcessingStatus status) {
        if (!properties.getDuplicatePrevention().isEnabled()) {
            return;
        }

        try {
            // Save to database
            ProcessedEmail processedEmail = new ProcessedEmail(messageId, status);
            processedEmailRepository.save(processedEmail);

            // Cache for quick lookup
            if (redisTemplate != null) {
                String cacheKey = CACHE_KEY_PREFIX + messageId;
                Duration cacheDuration = parseDuration(properties.getDuplicatePrevention().getCacheDuration());
                redisTemplate.opsForValue().set(cacheKey, status.name(), cacheDuration.toSeconds(), TimeUnit.SECONDS);
            }

            logger.debug("Marked message {} as processed with status {}", messageId, status);
        } catch (Exception e) {
            logger.error("Error marking message {} as processed: {}", messageId, e.getMessage(), e);
        }
    }

    public void updateProcessingStatus(String messageId, ProcessedEmail.ProcessingStatus status, String errorMessage) {
        try {
            ProcessedEmail processedEmail = processedEmailRepository.findByMessageId(messageId)
                .orElse(new ProcessedEmail(messageId, status));
            
            processedEmail.setStatus(status);
            if (errorMessage != null) {
                processedEmail.setErrorMessage(errorMessage);
                processedEmail.setRetryCount(processedEmail.getRetryCount() + 1);
            }
            
            processedEmailRepository.save(processedEmail);

            // Update cache
            if (redisTemplate != null) {
                String cacheKey = CACHE_KEY_PREFIX + messageId;
                Duration cacheDuration = parseDuration(properties.getDuplicatePrevention().getCacheDuration());
                redisTemplate.opsForValue().set(cacheKey, status.name(), cacheDuration.toSeconds(), TimeUnit.SECONDS);
            }

        } catch (Exception e) {
            logger.error("Error updating processing status for message {}: {}", messageId, e.getMessage(), e);
        }
    }

    private Duration parseDuration(String duration) {
        try {
            if (duration.endsWith("h")) {
                return Duration.ofHours(Long.parseLong(duration.substring(0, duration.length() - 1)));
            } else if (duration.endsWith("m")) {
                return Duration.ofMinutes(Long.parseLong(duration.substring(0, duration.length() - 1)));
            } else if (duration.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(duration.substring(0, duration.length() - 1)));
            } else {
                // Default to hours if no unit specified
                return Duration.ofHours(Long.parseLong(duration));
            }
        } catch (Exception e) {
            logger.warn("Unable to parse duration '{}', using default 24h", duration);
            return Duration.ofHours(24);
        }
    }
}