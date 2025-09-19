package com.email.handler.service;

import com.email.handler.config.EmailHandlerProperties;
import com.email.handler.model.EmailAttachment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AudioVideoProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(AudioVideoProcessingService.class);

    @Autowired
    private EmailHandlerProperties properties;

    @Autowired
    private RestTemplate restTemplate;

    private Set<String> supportedAudioFormats;
    private Set<String> supportedVideoFormats;

    public AudioVideoProcessingService() {
        // Initialize after properties are injected
    }

    private void initializeSupportedFormats() {
        if (supportedAudioFormats == null) {
            supportedAudioFormats = Arrays.stream(
                properties.getProcessing().getSupportedAudioFormats().split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        }
        
        if (supportedVideoFormats == null) {
            supportedVideoFormats = Arrays.stream(
                properties.getProcessing().getSupportedVideoFormats().split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        }
    }

    public void processAttachments(List<EmailAttachment> attachments) {
        initializeSupportedFormats();
        
        for (EmailAttachment attachment : attachments) {
            if (isAudioVideoFile(attachment)) {
                attachment.setAudioVideo(true);
                try {
                    String transcript = convertToTranscript(attachment);
                    attachment.setTranscript(transcript);
                    logger.info("Successfully converted attachment {} to transcript", attachment.getFilename());
                } catch (Exception e) {
                    logger.error("Failed to convert attachment {} to transcript: {}", 
                        attachment.getFilename(), e.getMessage(), e);
                    attachment.setTranscript("Error: Unable to convert audio/video to text - " + e.getMessage());
                }
            }
        }
    }

    private boolean isAudioVideoFile(EmailAttachment attachment) {
        if (attachment.getFilename() == null) {
            return false;
        }
        
        String filename = attachment.getFilename().toLowerCase();
        String extension = getFileExtension(filename);
        
        return supportedAudioFormats.contains(extension) || supportedVideoFormats.contains(extension);
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }

    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
    )
    private String convertToTranscript(EmailAttachment attachment) throws Exception {
        String apiUrl = properties.getConversionApi().getBaseUrl() + 
                       properties.getConversionApi().getEndpoint();

        // Prepare multipart request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        
        // Create file resource from attachment
        ByteArrayResource fileResource = new ByteArrayResource(attachment.getContent()) {
            @Override
            public String getFilename() {
                return attachment.getFilename();
            }
        };
        
        body.add("file", fileResource);
        body.add("filename", attachment.getFilename());
        body.add("contentType", attachment.getContentType());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        logger.info("Sending conversion request for file: {}", attachment.getFilename());
        
        ResponseEntity<String> response = restTemplate.exchange(
            apiUrl,
            HttpMethod.POST,
            requestEntity,
            String.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            return parseTranscriptFromResponse(response.getBody());
        } else {
            throw new RuntimeException("API call failed with status: " + response.getStatusCode());
        }
    }

    private String parseTranscriptFromResponse(String responseBody) throws Exception {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            // Assume the API returns JSON with a "transcript" field
            if (jsonNode.has("transcript")) {
                return jsonNode.get("transcript").asText();
            } else if (jsonNode.has("text")) {
                return jsonNode.get("text").asText();
            } else if (jsonNode.has("content")) {
                return jsonNode.get("content").asText();
            } else {
                // If no expected field, return the whole response
                return responseBody;
            }
        } catch (Exception e) {
            logger.warn("Unable to parse JSON response, returning raw response: {}", e.getMessage());
            return responseBody;
        }
    }

    public boolean hasAudioVideoAttachments(List<EmailAttachment> attachments) {
        initializeSupportedFormats();
        
        return attachments.stream()
            .anyMatch(this::isAudioVideoFile);
    }
}