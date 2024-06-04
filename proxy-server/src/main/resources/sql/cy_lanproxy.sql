/*
Navicat MySQL Data Transfer

Source Server         : 开发环境
Source Server Version : 50731
Source Host           : 172.16.2.253:3306
Source Database       : cy_lanproxy

Target Server Type    : MYSQL
Target Server Version : 50731
File Encoding         : 65001

Date: 2024-06-04 15:19:33
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for client
-- ----------------------------
DROP TABLE IF EXISTS `client`;
CREATE TABLE `client` (
  `id` varchar(32) NOT NULL,
  `name` varchar(50) NOT NULL COMMENT '客户端备注名称',
  `clientKey` varchar(32) NOT NULL COMMENT '代理客户端唯一标识key',
  `status` int(1) NOT NULL COMMENT '在线状态',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- ----------------------------
-- Table structure for client_proxy_mapping
-- ----------------------------
DROP TABLE IF EXISTS `client_proxy_mapping`;
CREATE TABLE `client_proxy_mapping` (
  `id` varchar(32) NOT NULL,
  `clientKey` varchar(32) NOT NULL COMMENT '客户端唯一标识',
  `inetPort` int(5) NOT NULL COMMENT '代理服务器端口',
  `lan` varchar(200) NOT NULL COMMENT '需要代理的网络信息（代理客户端能够访问），格式 192.168.1.99:80 (必须带端口)',
  `name` varchar(100) DEFAULT NULL COMMENT '备注名称',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) NOT NULL,
  `password` varchar(16) NOT NULL COMMENT '密码',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ix_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COMMENT='用户';

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES ('1', 'admin', 'admin');
