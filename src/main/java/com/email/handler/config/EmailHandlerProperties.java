package com.email.handler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "email.handler")
public class EmailHandlerProperties {
    
    private Imap imap = new Imap();
    private Smtp smtp = new Smtp();
    private ConversionApi conversionApi = new ConversionApi();
    private Processing processing = new Processing();
    private DuplicatePrevention duplicatePrevention = new DuplicatePrevention();
    
    // Getters and setters
    public Imap getImap() { return imap; }
    public void setImap(Imap imap) { this.imap = imap; }
    
    public Smtp getSmtp() { return smtp; }
    public void setSmtp(Smtp smtp) { this.smtp = smtp; }
    
    public ConversionApi getConversionApi() { return conversionApi; }
    public void setConversionApi(ConversionApi conversionApi) { this.conversionApi = conversionApi; }
    
    public Processing getProcessing() { return processing; }
    public void setProcessing(Processing processing) { this.processing = processing; }
    
    public DuplicatePrevention getDuplicatePrevention() { return duplicatePrevention; }
    public void setDuplicatePrevention(DuplicatePrevention duplicatePrevention) { this.duplicatePrevention = duplicatePrevention; }
    
    public static class Imap {
        private String host;
        private int port = 993;
        private String username;
        private String password;
        private boolean sslEnabled = true;
        private String folder = "INBOX";
        private long fetchInterval = 5000;
        private int maxMessagesPerFetch = 1;
        
        // Getters and setters
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public boolean isSslEnabled() { return sslEnabled; }
        public void setSslEnabled(boolean sslEnabled) { this.sslEnabled = sslEnabled; }
        
        public String getFolder() { return folder; }
        public void setFolder(String folder) { this.folder = folder; }
        
        public long getFetchInterval() { return fetchInterval; }
        public void setFetchInterval(long fetchInterval) { this.fetchInterval = fetchInterval; }
        
        public int getMaxMessagesPerFetch() { return maxMessagesPerFetch; }
        public void setMaxMessagesPerFetch(int maxMessagesPerFetch) { this.maxMessagesPerFetch = maxMessagesPerFetch; }
    }
    
    public static class Smtp {
        private String host;
        private int port = 587;
        private String username;
        private String password;
        private boolean starttlsEnabled = true;
        private String toAddress;
        
        // Getters and setters
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public boolean isStarttlsEnabled() { return starttlsEnabled; }
        public void setStarttlsEnabled(boolean starttlsEnabled) { this.starttlsEnabled = starttlsEnabled; }
        
        public String getToAddress() { return toAddress; }
        public void setToAddress(String toAddress) { this.toAddress = toAddress; }
    }
    
    public static class ConversionApi {
        private String baseUrl;
        private String endpoint = "/api/audio-video/convert";
        private int timeout = 300000;
        private int retryAttempts = 3;
        private long retryDelay = 5000;
        
        // Getters and setters
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        
        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
        
        public int getRetryAttempts() { return retryAttempts; }
        public void setRetryAttempts(int retryAttempts) { this.retryAttempts = retryAttempts; }
        
        public long getRetryDelay() { return retryDelay; }
        public void setRetryDelay(long retryDelay) { this.retryDelay = retryDelay; }
    }
    
    public static class Processing {
        private int threadPoolSize = 10;
        private int queueCapacity = 1000;
        private String supportedAudioFormats = "mp3,wav,m4a,aac,flac";
        private String supportedVideoFormats = "mp4,avi,mov,mkv,wmv";
        private String maxFileSize = "100MB";
        
        // Getters and setters
        public int getThreadPoolSize() { return threadPoolSize; }
        public void setThreadPoolSize(int threadPoolSize) { this.threadPoolSize = threadPoolSize; }
        
        public int getQueueCapacity() { return queueCapacity; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
        
        public String getSupportedAudioFormats() { return supportedAudioFormats; }
        public void setSupportedAudioFormats(String supportedAudioFormats) { this.supportedAudioFormats = supportedAudioFormats; }
        
        public String getSupportedVideoFormats() { return supportedVideoFormats; }
        public void setSupportedVideoFormats(String supportedVideoFormats) { this.supportedVideoFormats = supportedVideoFormats; }
        
        public String getMaxFileSize() { return maxFileSize; }
        public void setMaxFileSize(String maxFileSize) { this.maxFileSize = maxFileSize; }
    }
    
    public static class DuplicatePrevention {
        private boolean enabled = true;
        private String cacheDuration = "24h";
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getCacheDuration() { return cacheDuration; }
        public void setCacheDuration(String cacheDuration) { this.cacheDuration = cacheDuration; }
    }
}