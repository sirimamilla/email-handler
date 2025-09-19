-- Create processed_emails table for tracking email processing status
CREATE TABLE processed_emails (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(255) UNIQUE NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    instance_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_processed_emails_message_id ON processed_emails(message_id);
CREATE INDEX idx_processed_emails_status ON processed_emails(status);
CREATE INDEX idx_processed_emails_processed_at ON processed_emails(processed_at);
CREATE INDEX idx_processed_emails_instance_id ON processed_emails(instance_id);

-- Create leader election table for horizontal scaling coordination
CREATE TABLE leader_election (
    lock_name VARCHAR(255) PRIMARY KEY,
    instance_id VARCHAR(255) NOT NULL,
    acquired_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    heartbeat_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create work queue table for distributed processing
CREATE TABLE email_work_queue (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(255) UNIQUE NOT NULL,
    email_data TEXT NOT NULL,
    priority INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PENDING',
    assigned_instance_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_at TIMESTAMP,
    completed_at TIMESTAMP,
    retry_count INTEGER DEFAULT 0,
    last_error TEXT
);

-- Create indexes for work queue
CREATE INDEX idx_work_queue_status ON email_work_queue(status);
CREATE INDEX idx_work_queue_assigned_instance ON email_work_queue(assigned_instance_id);
CREATE INDEX idx_work_queue_created_at ON email_work_queue(created_at);
CREATE INDEX idx_work_queue_priority ON email_work_queue(priority DESC);