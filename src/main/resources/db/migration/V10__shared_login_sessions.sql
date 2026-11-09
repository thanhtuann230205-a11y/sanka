-- Shared Spring Session JDBC storage for all application instances.
CREATE TABLE SANKA_SESSION (
 primary_id CHAR(36) NOT NULL,
 session_id CHAR(36) NOT NULL,
 creation_time BIGINT NOT NULL,
 last_access_time BIGINT NOT NULL,
 max_inactive_interval INT NOT NULL,
 expiry_time BIGINT NOT NULL,
 principal_name VARCHAR(190),
 CONSTRAINT pk_sanka_sessions PRIMARY KEY (primary_id)
);
CREATE UNIQUE INDEX idx_sanka_session_id ON SANKA_SESSION(session_id);
CREATE INDEX idx_sanka_session_expiry ON SANKA_SESSION(expiry_time);
CREATE INDEX idx_sanka_session_principal ON SANKA_SESSION(principal_name);

CREATE TABLE SANKA_SESSION_ATTRIBUTES (
 session_primary_id CHAR(36) NOT NULL,
 attribute_name VARCHAR(200) NOT NULL,
 attribute_bytes BLOB NOT NULL,
 CONSTRAINT pk_sanka_session_attributes PRIMARY KEY (session_primary_id, attribute_name),
 CONSTRAINT fk_sanka_session_attributes FOREIGN KEY (session_primary_id)
  REFERENCES SANKA_SESSION(primary_id) ON DELETE CASCADE
);
