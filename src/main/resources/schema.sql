-- VaultPack Database Schema v3.0.0
-- Phase 9: MySQL Support with Component Architecture
-- Compatible with MySQL 8.0+

-- ============================================
-- Player Data Table (Component-Based Storage)
-- ============================================
-- Stores entire PlayerDataHolder as serialized YAML
-- This matches the file-based approach and supports all components
CREATE TABLE IF NOT EXISTS `vaultpack_player_data` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `uuid` VARCHAR(36) NOT NULL UNIQUE,
    `component_data` MEDIUMTEXT NOT NULL COMMENT 'Serialized YAML containing all component data',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_uuid` (`uuid`),
    INDEX `idx_updated` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Migration Tracking
-- ============================================
CREATE TABLE IF NOT EXISTS `vaultpack_migrations` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `version` VARCHAR(50) NOT NULL UNIQUE,
    `applied_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_version` (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Initial Migration Record
-- ============================================
INSERT IGNORE INTO `vaultpack_migrations` (`version`) VALUES ('v3.0.0_component_architecture');
