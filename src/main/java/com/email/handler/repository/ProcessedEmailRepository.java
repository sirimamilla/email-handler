package com.email.handler.repository;

import com.email.handler.model.ProcessedEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessedEmailRepository extends JpaRepository<ProcessedEmail, Long> {
    
    Optional<ProcessedEmail> findByMessageId(String messageId);
    
    boolean existsByMessageId(String messageId);
    
    List<ProcessedEmail> findByStatusAndRetryCountLessThan(
        ProcessedEmail.ProcessingStatus status, 
        Integer maxRetryCount
    );
    
    List<ProcessedEmail> findByProcessedAtBefore(LocalDateTime dateTime);
}