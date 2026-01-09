-- VaultPack Database Schema
-- Phase 2: MySQL Support
-- Compatible with MySQL 8.0+

-- ============================================
-- Player Data Table
-- ============================================
CREATE TABLE IF NOT EXISTS `vaultpack_players` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `uuid` VARCHAR(36) NOT NULL UNIQUE,
    `unlocked_slots` INT NOT NULL DEFAULT 1,
    `unlocked_ender_pages` INT NOT NULL DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Backpacks Table
-- ============================================
CREATE TABLE IF NOT EXISTS `vaultpack_backpacks` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `player_uuid` VARCHAR(36) NOT NULL,
    `slot_number` INT NOT NULL,
    `tier` VARCHAR(20) NOT NULL,
    `type_id` VARCHAR(50) DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `unique_player_slot` (`player_uuid`, `slot_number`),
    INDEX `idx_player` (`player_uuid`),
    FOREIGN KEY (`player_uuid`) REFERENCES `vaultpack_players`(`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Backpack Contents Table
-- ============================================
CREATE TABLE IF NOT EXISTS `vaultpack_backpack_contents` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `backpack_id` INT NOT NULL,
    `slot_index` INT NOT NULL,
    `item_data` MEDIUMTEXT NOT NULL COMMENT 'Base64 encoded ItemStack',
    UNIQUE KEY `unique_backpack_slot` (`backpack_id`, `slot_index`),
    INDEX `idx_backpack` (`backpack_id`),
    FOREIGN KEY (`backpack_id`) REFERENCES `vaultpack_backpacks`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Ender Pages Table
-- ============================================
CREATE TABLE IF NOT EXISTS `vaultpack_ender_pages` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `player_uuid` VARCHAR(36) NOT NULL,
    `page_number` INT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `unique_player_page` (`player_uuid`, `page_number`),
    INDEX `idx_player` (`player_uuid`),
    FOREIGN KEY (`player_uuid`) REFERENCES `vaultpack_players`(`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Ender Page Contents Table
-- ============================================
CREATE TABLE IF NOT EXISTS `vaultpack_ender_contents` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `page_id` INT NOT NULL,
    `slot_index` INT NOT NULL,
    `item_data` MEDIUMTEXT NOT NULL COMMENT 'Base64 encoded ItemStack',
    UNIQUE KEY `unique_page_slot` (`page_id`, `slot_index`),
    INDEX `idx_page` (`page_id`),
    FOREIGN KEY (`page_id`) REFERENCES `vaultpack_ender_pages`(`id`) ON DELETE CASCADE
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
INSERT IGNORE INTO `vaultpack_migrations` (`version`) VALUES ('v1.0.0_initial_schema');
