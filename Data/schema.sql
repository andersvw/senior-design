-- MySQL Script generated by Anders Van Winkle
-- 10/20/14 11:31:47
-- Model: New Model    Version: 1.0
SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

-- -----------------------------------------------------
-- Schema kaching
-- -----------------------------------------------------
DROP SCHEMA IF EXISTS `kaching` ;
CREATE SCHEMA IF NOT EXISTS `kaching` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci ;
USE `kaching` ;

-- -----------------------------------------------------
-- Table `kaching`.`user`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `kaching`.`user` ;

CREATE TABLE IF NOT EXISTS `kaching`.`user` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(15) NOT NULL,
  `password` VARCHAR(44) NOT NULL,
  `salt` VARCHAR(16) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `id_UNIQUE` (`id` ASC))
ENGINE = InnoDB;

-- -----------------------------------------------------
-- Table `kaching`.`user_info`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `kaching`.`user_info` ;

CREATE TABLE IF NOT EXISTS `kaching`.`user_info` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `first_name` VARCHAR(50),
  `last_name` VARCHAR(50),
  `email` VARCHAR(50),
  `address` VARCHAR(150),
  `phone_number` VARCHAR(10),

  PRIMARY KEY (`id`),
  INDEX (`user_id`),

  FOREIGN KEY (`user_id`)
    REFERENCES user(`id`),

  UNIQUE INDEX `id_UNIQUE` (`id` ASC))
ENGINE = InnoDB;

-- -----------------------------------------------------
-- Table `kaching`.`account_type`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `kaching`.`account_type` ;

CREATE TABLE IF NOT EXISTS `kaching`.`account_type` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `type` VARCHAR(20),

  PRIMARY KEY (`id`),

  UNIQUE INDEX `id_UNIQUE` (`id` ASC))
ENGINE = InnoDB;

-- -----------------------------------------------------
-- Table `kaching`.`account`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `kaching`.`account` ;

CREATE TABLE IF NOT EXISTS `kaching`.`account` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `account_type_id` INT NOT NULL,
  `number` VARCHAR(10) NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `interest_rate` DECIMAL(10, 4) NOT NULL,
  `balance` DECIMAL(10, 2) NOT NULL,
  `overdraft` DECIMAL(10, 2),

  PRIMARY KEY (`id`),
  INDEX (`user_id`),

  FOREIGN KEY (`user_id`)
    REFERENCES user(`id`),

  FOREIGN KEY (`account_type_id`)
    REFERENCES account_type(`id`),

  UNIQUE INDEX `id_UNIQUE` (`id` ASC))
ENGINE = InnoDB;

-- -----------------------------------------------------
-- Table `kaching`.`credit_account`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `kaching`.`credit_account` ;

CREATE TABLE IF NOT EXISTS `kaching`.`credit_account` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `account_type_id` INT NOT NULL,
  `number` VARCHAR(16) NOT NULL,
  `cvv` VARCHAR(3) NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `expiry_date` DATE NOT NULL,
  `balance` DECIMAL(10, 2) NOT NULL,
  `limit` DECIMAL(10, 2) NOT NULL,

  PRIMARY KEY (`id`),
  INDEX (`user_id`),

  FOREIGN KEY (`user_id`)
    REFERENCES user(`id`),

  FOREIGN KEY (`account_type_id`)
    REFERENCES account_type(`id`),

  UNIQUE INDEX `id_UNIQUE` (`id` ASC))
ENGINE = InnoDB;

-- -----------------------------------------------------
-- Table `kaching`.`transaction_type`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `kaching`.`transaction_type` ;

CREATE TABLE IF NOT EXISTS `kaching`.`transaction_type` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `type` VARCHAR(20),

  PRIMARY KEY (`id`),

  UNIQUE INDEX `id_UNIQUE` (`id` ASC))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `kaching`.`history`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `kaching`.`history` ;

CREATE TABLE IF NOT EXISTS `kaching`.`history` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `transaction_type_id` INT NOT NULL,
  `account_number` VARCHAR(16) NOT NULL,
  `amount` DECIMAL(10,2),
  `datetime` DATETIME,

  PRIMARY KEY (`id`),

  UNIQUE INDEX `id_UNIQUE` (`id` ASC))
ENGINE = InnoDB;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
