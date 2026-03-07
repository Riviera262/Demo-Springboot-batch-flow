-- ==========================================
-- 1. TABLE: SYS_USER
-- ==========================================
CREATE TABLE SYS_USER (
    username VARCHAR2(50) NOT NULL,
    password VARCHAR2(256) NOT NULL,
    full_name VARCHAR2(150),
    email VARCHAR2(150) UNIQUE,
    status VARCHAR2(20),
    role VARCHAR2(20),
    CONSTRAINT pk_sys_user PRIMARY KEY (username)
);

-- ==========================================
-- 2. TABLE: UPLOAD_BATCH
-- ==========================================
CREATE TABLE UPLOAD_BATCH (
    batch_id VARCHAR2(50) NOT NULL,
    uploaded_by VARCHAR2(50) NOT NULL,
    upload_time TIMESTAMP NOT NULL,
    file_count NUMBER NOT NULL,
    file_storage_path VARCHAR2(500),
    status VARCHAR2(20) DEFAULT 'PENDING' NOT NULL,
    total_rows NUMBER(10) DEFAULT 0,
    success_rows NUMBER(10) DEFAULT 0,
    failed_rows NUMBER(10) DEFAULT 0,
    start_process_time TIMESTAMP,
    end_process_time TIMESTAMP,
    result_message CLOB,
    approved_by VARCHAR2(50),
    approved_at TIMESTAMP,
    approved_rows NUMBER(10) DEFAULT 0,
    rejected_rows NUMBER(10) DEFAULT 0,
    approval_message CLOB,
    create_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT pk_upload_batch PRIMARY KEY (batch_id)
);

CREATE INDEX idx_upload_batch_status ON UPLOAD_BATCH(status);

-- ==========================================
-- 3. TABLE: MB_TRANSACTION_UPL (Bảng Tạm)
-- ==========================================
CREATE TABLE MB_TRANSACTION_UPL (
    batch_id VARCHAR2(40) NOT NULL,
    trace VARCHAR2(20) NOT NULL,
    inserted_at TIMESTAMP,
    uploaded_by VARCHAR2(50),
    from_acc VARCHAR2(20),
    tranx_time TIMESTAMP,
    amount NUMBER,
    to_acc VARCHAR2(20),
    remark VARCHAR2(200),
    tranx_type VARCHAR2(30),
    status VARCHAR2(10),
    createBy VARCHAR2(50),
    createTm TIMESTAMP,
    file_name VARCHAR2(200),
    CONSTRAINT pk_mb_transaction_upl PRIMARY KEY (batch_id, trace)
);

CREATE INDEX idx_mb_upl_batch_status ON MB_TRANSACTION_UPL(batch_id, status);

-- ==========================================
-- 4. TABLE: MB_TRANSACTION (Bảng Chính)
-- ==========================================
CREATE TABLE MB_TRANSACTION (
    trace VARCHAR2(20) NOT NULL,
    batch_id VARCHAR2(40) NOT NULL,
    inserted_at TIMESTAMP,
    uploaded_by VARCHAR2(50),
    from_acc VARCHAR2(20),
    tranx_time TIMESTAMP,
    amount NUMBER,
    to_acc VARCHAR2(20),
    remark VARCHAR2(200),
    tranx_type VARCHAR2(30),
    status VARCHAR2(10),
    createBy VARCHAR2(50),
    createTm TIMESTAMP,
    CONSTRAINT pk_mb_transaction PRIMARY KEY (trace)
);
