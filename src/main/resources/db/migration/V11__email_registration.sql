CREATE TABLE email_registrations (
 email VARCHAR(190) NOT NULL PRIMARY KEY,
 request_token VARCHAR(36) NOT NULL,
 student_name VARCHAR(80) NOT NULL,
 password_hash VARCHAR(255) NOT NULL,
 code_hash VARCHAR(255) NOT NULL,
 expires_at DATETIME(6) NOT NULL,
 sent_at DATETIME(6) NOT NULL,
 window_start DATETIME(6) NOT NULL,
 attempts INT NOT NULL,
 send_count INT NOT NULL
);
CREATE UNIQUE INDEX idx_email_registration_token ON email_registrations(request_token);
