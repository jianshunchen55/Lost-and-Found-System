CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) UNIQUE NOT NULL,
  email VARCHAR(128) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  enabled TINYINT DEFAULT 0,
  audited TINYINT DEFAULT 0,
  created_at TIMESTAMP NULL,
  nickname VARCHAR(64),
  department VARCHAR(64),
  phone VARCHAR(20),
  avatar VARCHAR(255),
  bio VARCHAR(255)
);
CREATE TABLE IF NOT EXISTS roles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(64) UNIQUE NOT NULL,
  name VARCHAR(64)
);
CREATE TABLE IF NOT EXISTS user_roles (
  user_id BIGINT,
  role_id BIGINT
);
CREATE TABLE IF NOT EXISTS lost_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128) NOT NULL,
  category VARCHAR(64),
  description TEXT,
  location VARCHAR(255),
  latitude DOUBLE,
  longitude DOUBLE,
  image_url TEXT,
  thumbnail_url VARCHAR(255),
  status VARCHAR(32),
  user_id BIGINT,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  lost_time DATETIME,
  point_id BIGINT,
  contact VARCHAR(128),
  images TEXT,
  audit_status TINYINT DEFAULT 0,
  claim_status TINYINT DEFAULT 0,
  claim_user_id BIGINT,
  claim_time DATETIME,
  claim_name VARCHAR(64),
  claim_phone VARCHAR(20),
  claim_audit_reply TEXT
);
CREATE TABLE IF NOT EXISTS found_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128) NOT NULL,
  category VARCHAR(64),
  description TEXT,
  location VARCHAR(255),
  latitude DOUBLE,
  longitude DOUBLE,
  image_url VARCHAR(255),
  thumbnail_url VARCHAR(255),
  status VARCHAR(32),
  user_id BIGINT,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  found_time DATETIME,
  point_id BIGINT,
  contact VARCHAR(128),
  images TEXT,
  audit_status TINYINT DEFAULT 0,
  claim_status TINYINT DEFAULT 0,
  claim_user_id BIGINT,
  claim_time DATETIME,
  claim_name VARCHAR(64),
  claim_phone VARCHAR(20),
  claim_audit_reply TEXT
);
CREATE TABLE IF NOT EXISTS message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  title VARCHAR(128),
  content TEXT,
  type VARCHAR(32),
  is_read TINYINT DEFAULT 0,
  related_id BIGINT,
  created_at TIMESTAMP NULL
);
CREATE TABLE IF NOT EXISTS map_point (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64),
  latitude DOUBLE,
  longitude DOUBLE,
  description VARCHAR(255)
);
CREATE TABLE IF NOT EXISTS notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255),
  content TEXT,
  updated_at DATETIME,
  image_url VARCHAR(255)
);
CREATE TABLE IF NOT EXISTS sys_config (
  cfg_key VARCHAR(64) PRIMARY KEY,
  cfg_value TEXT
);
CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sender_id BIGINT,
  receiver_id BIGINT,
  content TEXT,
  is_read TINYINT DEFAULT 0,
  created_at TIMESTAMP NULL
);
CREATE TABLE IF NOT EXISTS friendship (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  requester_id BIGINT,
  addressee_id BIGINT,
  status VARCHAR(32),
  requester_alias VARCHAR(64),
  addressee_alias VARCHAR(64),
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL
);
