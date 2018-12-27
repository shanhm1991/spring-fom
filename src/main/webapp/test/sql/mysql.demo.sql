/*
Navicat MySQL Data Transfer

Source Server         : shanhm@192.168.141.13
Source Server Version : 80011
Source Host           : 192.168.141.13:3306
Source Database       : demo

Target Server Type    : MYSQL
Target Server Version : 80011
File Encoding         : 65001

Date: 2018-12-27 23:14:01
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for `demo`
-- ----------------------------
DROP TABLE IF EXISTS `demo`;
CREATE TABLE `demo` (
  `id` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `age` varchar(255) DEFAULT NULL,
  `tel` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of demo
-- ----------------------------
INSERT INTO `demo` VALUES ('1', 'name1', '18', '13618273945');
INSERT INTO `demo` VALUES ('2', 'name2', '36', '18617342763');
INSERT INTO `demo` VALUES ('3', 'name3', '26', '15217328432');
INSERT INTO `demo` VALUES ('4', 'name4', '52', '13829432745');
