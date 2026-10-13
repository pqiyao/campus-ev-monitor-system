-- MySQL dump 10.13  Distrib 8.0.43, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: ev_monitor
-- ------------------------------------------------------
-- Server version	8.0.43

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `agent_step`
--

DROP TABLE IF EXISTS `agent_step`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agent_step` (
  `step_id` bigint NOT NULL AUTO_INCREMENT,
  `mission_id` bigint NOT NULL,
  `phase` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL,
  `step_no` int NOT NULL,
  `title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `detail` text COLLATE utf8mb4_unicode_ci,
  `state` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL,
  `duration_ms` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`step_id`),
  KEY `idx_agent_step_mission` (`mission_id`,`step_no`)
) ENGINE=InnoDB AUTO_INCREMENT=150 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `agent_step`
--

LOCK TABLES `agent_step` WRITE;
/*!40000 ALTER TABLE `agent_step` DISABLE KEYS */;
INSERT INTO `agent_step` VALUES (1,1,'UNDERSTAND',1,'解析巡检目标','区域=东校区;时限=120秒','COMPLETED',0,'2026-08-22 10:57:29'),(2,1,'PLAN',2,'生成风险网格与初始航线','读取近期事件、车辆位置和围栏，生成2个航点','COMPLETED',0,'2026-08-22 10:57:29'),(3,1,'EXECUTE',3,'启动仿真无人机','开始沿规划航点执行巡检','RUNNING',0,'2026-08-22 10:57:29'),(4,1,'MONITOR',4,'暂停仿真任务','等待人工或智能体继续','PAUSED',0,'2026-08-22 10:57:32'),(5,1,'DELIVER',6,'生成巡检报告','输出航线、遥测、重规划次数和风险摘要','COMPLETED',0,'2026-08-22 10:57:32'),(6,2,'UNDERSTAND',1,'解析巡检目标','区域=东校区;时限=120秒','COMPLETED',0,'2026-08-22 10:59:16'),(7,2,'PLAN',2,'生成风险网格与初始航线','读取近期事件、车辆位置和围栏，生成2个航点','COMPLETED',0,'2026-08-22 10:59:16'),(8,2,'EXECUTE',3,'启动仿真无人机','开始沿规划航点执行巡检','RUNNING',0,'2026-08-22 10:59:16'),(9,2,'DELIVER',6,'仿真巡检完成','无人机已到达最后航点并完成遥测记录','COMPLETED',0,'2026-08-22 10:59:17'),(10,2,'MONITOR',4,'暂停仿真任务','等待人工或智能体继续','PAUSED',0,'2026-08-22 10:59:19'),(11,2,'DELIVER',6,'生成巡检报告','输出航线、遥测、重规划次数和风险摘要','COMPLETED',0,'2026-08-22 10:59:19'),(12,3,'UNDERSTAND',1,'解析巡检目标','区域=东校区;时限=120秒','COMPLETED',0,'2026-08-22 10:59:38'),(13,3,'PLAN',2,'生成风险网格与初始航线','读取近期事件、车辆位置和围栏，生成2个航点','COMPLETED',0,'2026-08-22 10:59:38'),(14,3,'OBSERVE',4,'接入巡检AI事件','事件 11 已关联到当前巡检任务','COMPLETED',0,'2026-08-22 10:59:38'),(15,3,'PLAN',2,'生成风险网格与初始航线','读取近期事件、车辆位置和围栏，生成2个航点','COMPLETED',0,'2026-08-22 10:59:38'),(16,3,'REPLAN',5,'动态重规划','发现巡检AI事件 11，自动插入重点复核航点','COMPLETED',0,'2026-08-22 10:59:38'),(17,4,'UNDERSTAND',1,'解析巡检目标','区域=东校区;时限=120秒','COMPLETED',0,'2026-08-22 11:00:34'),(18,3,'OBSERVE',4,'接入巡检AI事件','事件 17 已关联到当前巡检任务','COMPLETED',0,'2026-08-22 11:10:20'),(19,5,'UNDERSTAND',1,'解析巡检目标','区域=东校区;时限=120秒','COMPLETED',0,'2026-08-22 11:17:00'),(20,5,'PLAN',2,'生成风险网格与初始航线','读取近期事件、车辆位置和围栏，生成9个航点','COMPLETED',0,'2026-08-22 11:17:00'),(21,5,'EXECUTE',3,'启动仿真无人机','开始沿规划航点执行巡检','RUNNING',0,'2026-08-22 11:17:00'),(22,5,'MONITOR',4,'暂停仿真任务','等待人工或智能体继续','PAUSED',0,'2026-08-22 11:17:04'),(23,5,'OBSERVE',4,'接入巡检AI事件','事件 18 已关联到当前巡检任务','COMPLETED',0,'2026-08-22 11:17:05'),(24,5,'DELIVER',6,'生成巡检报告','输出航线、遥测、重规划次数和风险摘要','COMPLETED',0,'2026-08-22 11:17:05'),(25,5,'DELIVER',6,'生成巡检报告','输出航线、遥测、重规划次数和风险摘要','COMPLETED',0,'2026-08-22 11:53:00'),(26,6,'UNDERSTAND',1,'解析巡检目标','区域=东校区;时限=720秒','COMPLETED',0,'2026-08-22 11:53:06'),(27,3,'EXECUTE',3,'启动仿真无人机','开始沿规划航点执行巡检','RUNNING',0,'2026-08-22 13:40:51'),(28,3,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 13:40:51'),(29,3,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 13:40:52'),(30,3,'EXECUTE',3,'RETURNING','完成巡检航点，开始返航','RETURNING',0,'2026-08-22 13:40:52'),(31,3,'EXECUTE',3,'COMPLETED','无人机已返航，巡检任务完成','COMPLETED',0,'2026-08-22 13:40:52'),(32,3,'MONITOR',4,'暂停仿真任务','等待人工或智能体继续','PAUSED',0,'2026-08-22 13:40:54'),(33,7,'UNDERSTAND',1,'解析巡检目标','区域=桂林理工大学屏风校区;时限=720秒','COMPLETED',0,'2026-08-22 13:51:12'),(34,7,'PLAN',2,'生成风险网格与初始航线','读取近期事件、车辆位置和围栏，生成2个航点','COMPLETED',0,'2026-08-22 13:51:12'),(35,7,'EXECUTE',3,'启动仿真无人机','开始沿规划航点执行巡检','RUNNING',0,'2026-08-22 13:51:12'),(36,7,'MONITOR',4,'注入仿真故障','GPS_OFFSET {meters=12}','INJECTED',0,'2026-08-22 13:51:12'),(37,7,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 13:51:12'),(38,7,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 13:51:13'),(39,7,'EXECUTE',3,'RETURNING','完成巡检航点，开始返航','RETURNING',0,'2026-08-22 13:51:13'),(40,7,'EXECUTE',3,'COMPLETED','无人机已返航，巡检任务完成','COMPLETED',0,'2026-08-22 13:51:13'),(41,7,'DELIVER',6,'生成巡检报告','输出航线、遥测、重规划次数和风险摘要','COMPLETED',0,'2026-08-22 13:51:14'),(42,7,'PLAN',2,'生成风险网格与初始航线','读取近期事件、车辆位置和围栏，生成3个航点','COMPLETED',0,'2026-08-22 13:51:30'),(43,7,'REPLAN',5,'动态重规划','验证已执行航点保留和剩余航段调整','COMPLETED',0,'2026-08-22 13:51:30'),(44,8,'UNDERSTAND',1,'解析巡检目标','区域=桂林理工大学屏风校区;时限=600秒','COMPLETED',0,'2026-08-22 13:55:12'),(45,8,'PLAN',2,'生成风险网格与初始航线','读取近期事件、车辆位置和围栏，生成2个航点','COMPLETED',0,'2026-08-22 13:55:12'),(46,8,'EXECUTE',3,'启动仿真无人机','开始沿规划航点执行巡检','RUNNING',0,'2026-08-22 13:55:12'),(47,8,'MONITOR',4,'注入仿真故障','PACKET_LOSS {rate=0.5}','INJECTED',0,'2026-08-22 13:55:12'),(48,8,'MONITOR',4,'恢复仿真故障','已清除当前任务故障注入','RECOVERED',0,'2026-08-22 13:55:12'),(49,8,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 13:55:13'),(50,8,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 13:55:13'),(51,8,'EXECUTE',3,'RETURNING','完成巡检航点，开始返航','RETURNING',0,'2026-08-22 13:55:14'),(52,8,'EXECUTE',3,'COMPLETED','无人机已返航，巡检任务完成','COMPLETED',0,'2026-08-22 13:55:14'),(53,8,'DELIVER',6,'生成巡检报告','输出航线、遥测、重规划次数和风险摘要','COMPLETED',0,'2026-08-22 13:55:15'),(54,9,'UNDERSTAND',1,'解析巡检目标','区域=东校区;时限=720秒','COMPLETED',0,'2026-08-22 14:09:25'),(55,10,'UNDERSTAND',1,'解析巡检目标','区域=东校区;时限=720秒','COMPLETED',0,'2026-08-22 14:09:37'),(56,11,'UNDERSTAND',1,'解析巡检目标','区域=东校区;时限=720秒','COMPLETED',0,'2026-08-22 14:09:59'),(57,11,'PLAN',2,'生成风险网格与初始航线','读取近期事件、车辆位置和围栏，生成9个航点','COMPLETED',0,'2026-08-22 14:09:59'),(58,12,'UNDERSTAND',1,'解析巡检目标','区域=东校区;时限=720秒','COMPLETED',0,'2026-08-22 14:10:24'),(59,12,'PLAN',2,'生成风险网格与初始航线','读取近期事件、车辆位置和围栏，生成9个航点','COMPLETED',0,'2026-08-22 14:10:24'),(60,12,'EXECUTE',3,'启动仿真无人机','开始沿规划航点执行巡检','RUNNING',0,'2026-08-22 14:10:24'),(61,12,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 14:10:24'),(62,12,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 14:10:25'),(63,12,'MONITOR',4,'注入仿真故障','GPS_OFFSET {meters=12}','INJECTED',0,'2026-08-22 14:10:27'),(64,12,'MONITOR',4,'恢复仿真故障','已清除当前任务故障注入','RECOVERED',0,'2026-08-22 14:10:27'),(65,12,'MONITOR',4,'暂停仿真任务','等待人工或智能体继续','PAUSED',0,'2026-08-22 14:10:27'),(66,12,'DELIVER',6,'生成巡检报告','输出航线、遥测、重规划次数和风险摘要','COMPLETED',0,'2026-08-22 14:10:27'),(67,12,'DELIVER',6,'生成巡检报告','输出航线、遥测、重规划次数和风险摘要','COMPLETED',0,'2026-08-22 14:10:39'),(68,13,'UNDERSTAND',1,'解析巡检目标','区域=东校区;时限=720秒','COMPLETED',0,'2026-08-22 14:27:23'),(69,13,'PLAN',2,'生成风险网格与初始航线','读取近期事件、车辆位置和围栏，生成9个航点','COMPLETED',0,'2026-08-22 14:27:23'),(70,13,'DELIVER',6,'生成巡检报告','输出航线、遥测、重规划次数和风险摘要','COMPLETED',0,'2026-08-22 14:27:23'),(71,14,'UNDERSTAND',1,'解析巡检目标','区域=桂林理工大学屏风校区;时限=720秒','COMPLETED',0,'2026-08-22 14:44:46'),(72,14,'PLAN',2,'生成风险网格与初始航线','读取近期事件、车辆位置和围栏，生成2个航点','COMPLETED',0,'2026-08-22 14:44:46'),(73,14,'EXECUTE',3,'启动仿真无人机','开始沿规划航点执行巡检','RUNNING',0,'2026-08-22 14:44:47'),(74,14,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 14:44:47'),(75,14,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 14:44:48'),(76,14,'EXECUTE',3,'RETURNING','完成巡检航点，开始返航','RETURNING',0,'2026-08-22 14:44:48'),(77,14,'EXECUTE',3,'COMPLETED','无人机已返航，巡检任务完成','COMPLETED',0,'2026-08-22 14:44:48'),(78,14,'OBSERVE',4,'接入巡检AI事件','事件 22 已关联到当前巡检任务','COMPLETED',0,'2026-08-22 14:44:50'),(79,14,'DELIVER',6,'生成巡检报告','输出航线、遥测、重规划次数和风险摘要','COMPLETED',0,'2026-08-22 14:44:50'),(80,15,'UNDERSTAND',1,'解析巡检目标','区域=桂林理工大学屏风校区;时限=720秒','COMPLETED',0,'2026-08-22 14:45:55'),(81,15,'PLAN',2,'生成风险网格与初始航线','读取近期事件、车辆位置和围栏，生成2个航点','COMPLETED',0,'2026-08-22 14:45:55'),(82,15,'EXECUTE',3,'启动仿真无人机','开始沿规划航点执行巡检','RUNNING',0,'2026-08-22 14:45:55'),(83,15,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 14:45:55'),(84,15,'OBSERVE',4,'接入巡检AI事件','事件 23 已关联到当前巡检任务','COMPLETED',0,'2026-08-22 14:45:56'),(85,15,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 14:45:56'),(86,15,'DELIVER',6,'生成巡检报告','输出航线、遥测、重规划次数和风险摘要','COMPLETED',0,'2026-08-22 14:45:56'),(87,15,'EXECUTE',3,'RETURNING','完成巡检航点，开始返航','RETURNING',0,'2026-08-22 14:45:56'),(88,15,'EXECUTE',3,'COMPLETED','无人机已返航，巡检任务完成','COMPLETED',0,'2026-08-22 14:45:56'),(89,16,'UNDERSTAND',1,'解析巡检目标','区域=屏风校区;时限=720秒','COMPLETED',0,'2026-08-22 15:31:19'),(90,16,'PLAN',2,'生成风险网格与初始航线','读取近期事件、车辆位置和围栏，生成2个航点','COMPLETED',0,'2026-08-22 15:31:19'),(91,16,'EXECUTE',3,'启动仿真无人机','开始沿规划航点执行巡检','RUNNING',0,'2026-08-22 15:31:19'),(92,16,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 15:31:19'),(93,16,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 15:31:20'),(94,16,'EXECUTE',3,'RETURNING','完成巡检航点，开始返航','RETURNING',0,'2026-08-22 15:31:20'),(95,16,'EXECUTE',3,'COMPLETED','无人机已返航，巡检任务完成','COMPLETED',0,'2026-08-22 15:31:20'),(96,17,'UNDERSTAND',1,'解析巡检目标','区域=屏风校区;时限=720秒','COMPLETED',0,'2026-08-22 15:31:55'),(97,17,'PLAN',2,'生成风险网格与初始航线','读取近期事件、车辆位置和围栏，生成2个航点','COMPLETED',0,'2026-08-22 15:31:55'),(98,17,'EXECUTE',3,'启动仿真无人机','开始沿规划航点执行巡检','RUNNING',0,'2026-08-22 15:31:55'),(99,17,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 15:31:55'),(100,17,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 15:31:56'),(101,17,'EXECUTE',3,'RETURNING','完成巡检航点，开始返航','RETURNING',0,'2026-08-22 15:31:56'),(102,17,'EXECUTE',3,'COMPLETED','无人机已返航，巡检任务完成','COMPLETED',0,'2026-08-22 15:31:56'),(103,18,'UNDERSTAND',1,'解析巡检目标','区域=屏风校区;时限=720秒','COMPLETED',0,'2026-08-22 15:32:30'),(104,18,'PLAN',2,'生成风险网格与初始航线','读取近期事件、车辆位置和围栏，生成2个航点','COMPLETED',0,'2026-08-22 15:32:30'),(105,18,'EXECUTE',3,'启动仿真无人机','开始沿规划航点执行巡检','RUNNING',0,'2026-08-22 15:32:30'),(106,18,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 15:32:30'),(107,18,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 15:32:31'),(108,18,'EXECUTE',3,'RETURNING','完成巡检航点，开始返航','RETURNING',0,'2026-08-22 15:32:31'),(109,18,'EXECUTE',3,'COMPLETED','无人机已返航，巡检任务完成','COMPLETED',0,'2026-08-22 15:32:31'),(110,19,'UNDERSTAND',1,'解析巡检目标','区域=屏风校区;时限=720秒','COMPLETED',0,'2026-08-22 15:32:59'),(111,19,'PLAN',2,'生成风险网格与初始航线','读取近期事件、车辆位置和围栏，生成2个航点','COMPLETED',0,'2026-08-22 15:32:59'),(112,19,'EXECUTE',3,'启动仿真无人机','开始沿规划航点执行巡检','RUNNING',0,'2026-08-22 15:32:59'),(113,19,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 15:32:59'),(114,19,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 15:33:00'),(115,19,'EXECUTE',3,'RETURNING','完成巡检航点，开始返航','RETURNING',0,'2026-08-22 15:33:00'),(116,19,'EXECUTE',3,'COMPLETED','无人机已返航，巡检任务完成','COMPLETED',0,'2026-08-22 15:33:00'),(117,20,'UNDERSTAND',1,'解析巡检目标','区域=屏风校区;时限=720秒','COMPLETED',0,'2026-08-22 15:33:35'),(118,20,'PLAN',2,'生成风险网格与初始航线','读取近期事件、车辆位置和围栏，生成2个航点','COMPLETED',0,'2026-08-22 15:33:35'),(119,20,'EXECUTE',3,'启动仿真无人机','开始沿规划航点执行巡检','RUNNING',0,'2026-08-22 15:33:35'),(120,20,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 15:33:36'),(121,20,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 15:33:36'),(122,20,'EXECUTE',3,'RETURNING','完成巡检航点，开始返航','RETURNING',0,'2026-08-22 15:33:37'),(123,20,'EXECUTE',3,'COMPLETED','无人机已返航，巡检任务完成','COMPLETED',0,'2026-08-22 15:33:37'),(124,21,'UNDERSTAND',1,'解析巡检目标','区域=屏风校区;时限=720秒','COMPLETED',0,'2026-08-22 15:34:06'),(125,21,'PLAN',2,'生成风险网格与初始航线','读取近期事件、车辆位置和围栏，生成2个航点','COMPLETED',0,'2026-08-22 15:34:06'),(126,21,'EXECUTE',3,'启动仿真无人机','开始沿规划航点执行巡检','RUNNING',0,'2026-08-22 15:34:06'),(127,21,'OBSERVE',4,'接入巡检AI事件','事件 24 已关联到当前巡检任务','COMPLETED',0,'2026-08-22 15:34:06'),(128,21,'DELIVER',6,'生成巡检报告','输出航线、遥测、重规划次数和风险摘要','COMPLETED',0,'2026-08-22 15:34:06'),(129,21,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 15:34:06'),(130,21,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-08-22 15:34:07'),(131,21,'EXECUTE',3,'RETURNING','完成巡检航点，开始返航','RETURNING',0,'2026-08-22 15:34:08'),(132,21,'EXECUTE',3,'COMPLETED','无人机已返航，巡检任务完成','COMPLETED',0,'2026-08-22 15:34:08'),(133,22,'UNDERSTAND',1,'解析巡检目标','区域=屏风校区北门及宿舍区;时限=300秒','COMPLETED',0,'2026-08-24 20:41:56'),(134,23,'UNDERSTAND',1,'解析巡检目标','区域=屏风校区北门及宿舍区;时限=300秒','COMPLETED',0,'2026-08-24 20:46:23'),(135,23,'OBSERVE',4,'接入巡检AI事件','事件 25 已关联到当前巡检任务','COMPLETED',0,'2026-08-24 20:47:24'),(136,24,'UNDERSTAND',1,'解析巡检目标','区域=桂林理工大学屏风校区;时限=720秒','COMPLETED',0,'2026-09-10 18:59:41'),(137,24,'PLAN',2,'生成风险网格与初始航线','读取近期事件、车辆位置和围栏，生成9个航点','COMPLETED',0,'2026-09-10 18:59:41'),(138,24,'EXECUTE',3,'启动仿真无人机','开始沿规划航点执行巡检','RUNNING',0,'2026-09-10 18:59:41'),(139,24,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-09-10 18:59:42'),(140,24,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-09-10 18:59:42'),(141,24,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-09-10 18:59:45'),(142,24,'EXECUTE',3,'ARRIVED','到达规划航点','ARRIVED',0,'2026-09-10 18:59:49'),(143,24,'EXECUTE',4,'ARRIVED','到达规划航点','ARRIVED',0,'2026-09-10 18:59:52'),(144,24,'EXECUTE',5,'ARRIVED','到达规划航点','ARRIVED',0,'2026-09-10 18:59:55'),(145,24,'EXECUTE',6,'ARRIVED','到达规划航点','ARRIVED',0,'2026-09-10 18:59:58'),(146,24,'EXECUTE',7,'ARRIVED','到达规划航点','ARRIVED',0,'2026-09-10 19:00:03'),(147,24,'EXECUTE',8,'ARRIVED','到达规划航点','ARRIVED',0,'2026-09-10 19:00:06'),(148,24,'EXECUTE',3,'RETURNING','完成巡检航点，开始返航','RETURNING',0,'2026-09-10 19:00:06'),(149,24,'EXECUTE',3,'COMPLETED','无人机已返航，巡检任务完成','COMPLETED',0,'2026-09-10 19:00:10');
/*!40000 ALTER TABLE `agent_step` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_analysis`
--

DROP TABLE IF EXISTS `ai_analysis`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_analysis` (
  `analysis_id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL,
  `provider` varchar(32) NOT NULL,
  `model_name` varchar(64) NOT NULL,
  `prompt_version` varchar(32) NOT NULL,
  `valid_event` tinyint(1) NOT NULL DEFAULT '0',
  `confidence` decimal(5,4) NOT NULL DEFAULT '0.0000',
  `risk_level` varchar(16) DEFAULT NULL,
  `identity_reliable` tinyint(1) NOT NULL DEFAULT '0',
  `evidence_sufficient` tinyint(1) NOT NULL DEFAULT '0',
  `summary` varchar(1000) DEFAULT NULL,
  `recommended_action` varchar(64) DEFAULT NULL,
  `notification_text` varchar(1000) DEFAULT NULL,
  `observations_json` text,
  `raw_response` longtext,
  `latency_ms` int NOT NULL DEFAULT '0',
  `input_tokens` int DEFAULT NULL,
  `output_tokens` int DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`analysis_id`),
  KEY `idx_ai_analysis_event` (`event_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI研判结果';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_analysis`
--

LOCK TABLES `ai_analysis` WRITE;
/*!40000 ALTER TABLE `ai_analysis` DISABLE KEYS */;
INSERT INTO `ai_analysis` VALUES (1,1,'mock','mock-governance-v1','governance-v1',1,0.9500,'MEDIUM',0,1,'检测到驾驶人员疑似未佩戴安全头盔','CREATE_REVIEW_TASK','系统检测到您的车辆可能存在未佩戴头盔行为，请及时查看事件详情并按要求整改。','[\"连续关键帧均未发现有效头盔目标\",\"建议生成待确认治理事件\"]','mock:NO_HELMET',180,NULL,NULL,'2026-08-11 09:47:43'),(2,2,'mock','mock-governance-v1','governance-v1',1,0.8600,'MEDIUM',0,1,'车辆在非停车区域持续停留','CREATE_REVIEW_TASK','系统检测到您的车辆可能存在违规停车行为，请及时查看事件详情并按要求整改。','[\"位置与禁停区域重叠且停留时间超过阈值\",\"建议人工复核停车时长和围栏范围\"]','mock:ILLEGAL_PARKING',180,NULL,NULL,'2026-08-11 10:06:41'),(3,3,'openai-compatible','gpt-5.6-sol','governance-v1',1,0.9400,'MEDIUM',0,0,'东校区正门检测到疑似未佩戴头盔骑行行为，检测置信度较高，但数据来自模拟器，未识别车牌，身份置信度为0，无法可靠关联具体人员或车辆。','不执行处罚。建议人工复核原始影像；若确认存在未佩戴头盔行为，可在该区域加强安全提醒和现场劝导。不得基于当前信息追责个人。','安全提示：东校区正门发现疑似未佩戴头盔骑行行为。骑乘电动车请规范佩戴安全头盔，注意出行安全。','[\"事件来源为SIMULATOR，不能直接视为真实设备采集证据。\",\"未提供车牌信息。\",\"身份置信度为0.0，身份不可确认。\",\"行为检测置信度为0.94。\",\"发生时间为2026-08-11T15:49:38。\",\"现有证据仅适合安全提示和人工复核，不足以支持处罚或个人追责。\"]','{\"id\":\"resp_04b5a642634f94d4016a7ad4164dfc819bacf5a0a69277cc62\",\"object\":\"response\",\"created_at\":1786434582,\"status\":\"completed\",\"background\":false,\"completed_at\":1786434590,\"error\":null,\"frequency_penalty\":0.0,\"incomplete_details\":null,\"instructions\":\"你是校园电动车治理事件研判器。只返回JSON，不执行处罚。字段必须包含valid,eventType,confidence,riskLevel,identityReliable,evidenceSufficient,summary,recommendedAction,notificationText,observations。confidence范围0到1，riskLevel仅允许LOW、MEDIUM、HIGH、CRITICAL。\",\"max_output_tokens\":null,\"max_tool_calls\":null,\"model\":\"gpt-5.6-sol\",\"moderation\":null,\"output\":[{\"id\":\"rs_04b5a642634f94d4016a7ad418230c819b8388fbe8f6644970\",\"type\":\"reasoning\",\"content\":[],\"encrypted_content\":\"gAAAAABqetQZ3ZszB5Iuqz79h0Ed-dPATdJaT3N0Xo1YDA00D7CLHwL8QBpW5Y8qMTNqPji9t6tFQJDqXzugI3OI2DxDsGTwC7hxi1jPWZlnuC4xDUppyT9AnbCvB4bF0KQqvES7Uax85m6KoeICdN-ZQ1sWzSo1OTBu8-7aDxP_jrRn_ie6rc-BhZaY1cgvIlrlVsEEMyNGZqCOiz736bisxMfWJZS5YQw1IkkbMLTmuewW_EZ0jsuyzxQtJaVPV1F_zJaJ_SRIaJ51PZNPzjL-vUe1gf4uNb9REylJTkd_PvnpSyZcSoYTtlRjWqjrOwFJZchIjJG2BYQ_dHWAT8NRnEJ2ShUQXy4TGi9UJlK6w1AseLncsOUkFiz66uYvkeygADcAKS2JoxstYh-EMbwcGFIJBHknva31ig6O4EvJl6NaLCHZ-PLz-m6nNJHJdyqTVNG2A5IP8mPSfym4X1lNlbk1x0Jav6ebXt7Ogcf_TFa8U_g-BbshUn2qplE4R8ZMx_7oCaAEyiXqVsIuTdR6rRyEcwJjeZQdRgZMY4TXifnTnHFyiD37kw2Z227tTAZayzRlt367Rvi06Ti1H5kSbP91g2jwcrPr_C4cdOPgWuaByNyu9_ph2GA6KISAS8UxG3QLTYHexJqsAEBA2PpvUf2oepXiKwcf9Na9hcQsznOfSva3NESq33y116PkqgwNXwN-QDdlu3V1KRztzZTGR2wZB7jJ5-iubvRUvimrou4mDH-Gk4X_FkDxnQIq9x7ETBOpkjpgNLXpfGjX1gK-8v7KAl9WCGMODLN_NQUAZ8k58fsE0KjluD4i5zjqe3309RjmQVeZZEXdGyxwatMfGQIDjEb5jfM0C2-Ov6OQMbL74tmekTkb-MRc38RH0RMdVyGo55wmI4mO0XCH6ViqecqiRZjj3fVgJKANilI6X-LyKfxPNaqQ0SrtbbcBNEbIICgWfnUvw2oo2sao3RMXc6JCCMOEbn8oc48vUEPJ8EaK6B6gpuVllch55-xZI_62tQw1drlyPgDmrsuVrz_G3nctz6zShUWS4QVxFfdc8a4bjttyXkVDPAMRvFJHQNZ3qNTY74nVKXwHjIdpy-_o1aXY2wHyZaSyC3h3l4p4J1itgbxEL2_hzGRbf9NPx4MSIbW0KZaNdgzSUoCgI8F7YoFLSBTH2b4NpUaBCi-62o965bpk3Ab7YXxkjvfy8-7nBLkrDtXcr0MpchEV5vJ4QnEYeJvO2f_PVLX4yoo4_yeESIcIQ1KwbuqkVNrdZU4DAL43zvE9ngw8mNkPNS1Ppdj-e4sq-PEOYx4SLlqCH8xFSVer-mmCCFzPVmHQ4oY5Nu3zbGHKi10Yn0ctPSRQ488r3afM-w==\",\"summary\":[]},{\"id\":\"msg_04b5a642634f94d4016a7ad4197144819ba77ed9f51e5b1289\",\"type\":\"message\",\"status\":\"completed\",\"content\":[{\"type\":\"output_text\",\"annotations\":[],\"logprobs\":[],\"text\":\"{\\n  \\\"valid\\\": true,\\n  \\\"eventType\\\": \\\"NO_HELMET\\\",\\n  \\\"confidence\\\": 0.94,\\n  \\\"riskLevel\\\": \\\"MEDIUM\\\",\\n  \\\"identityReliable\\\": false,\\n  \\\"evidenceSufficient\\\": false,\\n  \\\"summary\\\": \\\"东校区正门检测到疑似未佩戴头盔骑行行为，检测置信度较高，但数据来自模拟器，未识别车牌，身份置信度为0，无法可靠关联具体人员或车辆。\\\",\\n  \\\"recommendedAction\\\": \\\"不执行处罚。建议人工复核原始影像；若确认存在未佩戴头盔行为，可在该区域加强安全提醒和现场劝导。不得基于当前信息追责个人。\\\",\\n  \\\"notificationText\\\": \\\"安全提示：东校区正门发现疑似未佩戴头盔骑行行为。骑乘电动车请规范佩戴安全头盔，注意出行安全。\\\",\\n  \\\"observations\\\": [\\n    \\\"事件来源为SIMULATOR，不能直接视为真实设备采集证据。\\\",\\n    \\\"未提供车牌信息。\\\",\\n    \\\"身份置信度为0.0，身份不可确认。\\\",\\n    \\\"行为检测置信度为0.94。\\\",\\n    \\\"发生时间为2026-08-11T15:49:38。\\\",\\n    \\\"现有证据仅适合安全提示和人工复核，不足以支持处罚或个人追责。\\\"\\n  ]\\n}\"}],\"phase\":\"final_answer\",\"role\":\"assistant\"}],\"parallel_tool_calls\":true,\"presence_penalty\":0.0,\"previous_response_id\":null,\"prompt_cache_key\":\"530d1365-84ec-5777-91a4-4cbc95c1251c\",\"prompt_cache_retention\":\"24h\",\"reasoning\":{\"context\":\"all_turns\",\"effort\":\"low\",\"mode\":\"standard\",\"summary\":null},\"safety_identifier\":\"user-AEw1UN0jCibsJn8ScMdkStS1\",\"service_tier\":\"default\",\"store\":false,\"temperature\":1.0,\"text\":{\"format\":{\"type\":\"text\"},\"verbosity\":\"medium\"},\"tool_choice\":\"auto\",\"tool_usage\":{\"image_gen\":{\"input_tokens\":0,\"input_tokens_details\":{\"image_tokens\":0,\"text_tokens\":0},\"output_tokens\":0,\"output_tokens_details\":{\"image_tokens\":0,\"text_tokens\":0},\"total_tokens\":0},\"web_search\":{\"num_requests\":0}},\"tools\":[{\"type\":\"image_generation\",\"background\":\"auto\",\"model\":\"gpt-image-2-codex\",\"moderation\":\"auto\",\"n\":1,\"output_compression\":100,\"output_format\":\"png\",\"quality\":\"auto\",\"size\":\"auto\"}],\"top_logprobs\":0,\"top_p\":0.98,\"truncation\":\"disabled\",\"usage\":{\"input_tokens\":441,\"input_tokens_details\":{\"cache_write_tokens\":0,\"cached_tokens\":0},\"output_tokens\":383,\"output_tokens_details\":{\"reasoning_tokens\":64},\"total_tokens\":824},\"user\":null,\"metadata\":{}}',13073,441,383,'2026-08-11 15:49:51'),(5,8,'openai-compatible','gpt-5.6-sol','governance-v1',1,0.9800,'HIGH',0,1,'图片中可见一名正在道路上骑行电动车的人员，头部清晰可见且未佩戴安全头盔，未佩戴头盔行为成立。该骑行目标车辆的校园电动车车牌未在图片中清晰显现，无法识别。','记录为未佩戴头盔事件并进行安全提醒；因关联车牌不可见、身份不可靠，不建议据此开展身份追溯或处罚，建议调取相邻时段或其他角度影像补','发现疑似电动车骑行人员未佩戴安全头盔。当前图片无法识别关联校园车牌，请注意骑行安全并补充核验影像。','[\"画面右下区域可见一名电动车骑行人员。\",\"骑行人员头部无遮挡且未见安全头盔。\",\"车辆正在道路区域行驶，属于实际骑行状态。\",\"电动车车身前部可见，但未见清晰可读的校园车牌。\",\"背景机动车号牌与目标电动车无关联，不作为目标身份依据。\"]','{\"id\":\"resp_0ec75005aa9af5b2016a7d097d48a08199be0cf246e31f96be\",\"object\":\"response\",\"created_at\":1786579325,\"status\":\"completed\",\"background\":false,\"completed_at\":1786579333,\"error\":null,\"frequency_penalty\":0.0,\"incomplete_details\":null,\"instructions\":\"你是校园电动车治理事件研判器。只返回JSON，不执行处罚。字段必须包含valid,eventType,confidence,riskLevel,identityReliable,evidenceSufficient,plateVisible,recognizedPlate,plateConfidence,summary,recommendedAction,notificationText,observations。请检查图片中是否确有未佩戴头盔行为，并读取与该骑行目标关联的校园电动车车牌。无法清晰读取或存在多个候选车牌时，recognizedPlate必须为null且plateConfidence不得高于0.5。车牌必须按图片原文返回，不得猜测；identityReliable仅表示车牌图像是否清晰可靠。confidence范围0到1，riskLevel仅允许LOW、MEDIUM、HIGH、CRITICAL。\",\"max_output_tokens\":null,\"max_tool_calls\":null,\"model\":\"gpt-5.6-sol\",\"moderation\":null,\"output\":[{\"id\":\"msg_0ec75005aa9af5b2016a7d097f7e9481999ecc770f0232219f\",\"type\":\"message\",\"status\":\"completed\",\"content\":[{\"type\":\"output_text\",\"annotations\":[],\"logprobs\":[],\"text\":\"{\\n  \\\"valid\\\": true,\\n  \\\"eventType\\\": \\\"NO_HELMET\\\",\\n  \\\"confidence\\\": 0.98,\\n  \\\"riskLevel\\\": \\\"HIGH\\\",\\n  \\\"identityReliable\\\": false,\\n  \\\"evidenceSufficient\\\": true,\\n  \\\"plateVisible\\\": false,\\n  \\\"recognizedPlate\\\": null,\\n  \\\"plateConfidence\\\": 0.0,\\n  \\\"summary\\\": \\\"图片中可见一名正在道路上骑行电动车的人员，头部清晰可见且未佩戴安全头盔，未佩戴头盔行为成立。该骑行目标车辆的校园电动车车牌未在图片中清晰显现，无法识别。\\\",\\n  \\\"recommendedAction\\\": \\\"记录为未佩戴头盔事件并进行安全提醒；因关联车牌不可见、身份不可靠，不建议据此开展身份追溯或处罚，建议调取相邻时段或其他角度影像补充车牌证据。\\\",\\n  \\\"notificationText\\\": \\\"发现疑似电动车骑行人员未佩戴安全头盔。当前图片无法识别关联校园车牌，请注意骑行安全并补充核验影像。\\\",\\n  \\\"observations\\\": [\\n    \\\"画面右下区域可见一名电动车骑行人员。\\\",\\n    \\\"骑行人员头部无遮挡且未见安全头盔。\\\",\\n    \\\"车辆正在道路区域行驶，属于实际骑行状态。\\\",\\n    \\\"电动车车身前部可见，但未见清晰可读的校园车牌。\\\",\\n    \\\"背景机动车号牌与目标电动车无关联，不作为目标身份依据。\\\"\\n  ]\\n}\"}],\"phase\":\"final_answer\",\"role\":\"assistant\"}],\"parallel_tool_calls\":true,\"presence_penalty\":0.0,\"previous_response_id\":null,\"prompt_cache_key\":\"b2320a8f-2793-594d-903a-5ec724a8cbac\",\"prompt_cache_retention\":\"24h\",\"reasoning\":{\"context\":\"all_turns\",\"effort\":\"low\",\"mode\":\"standard\",\"summary\":null},\"safety_identifier\":\"user-aAz48p4oEFAPZPNF8s3t2i0D\",\"service_tier\":\"default\",\"store\":false,\"temperature\":1.0,\"text\":{\"format\":{\"type\":\"text\"},\"verbosity\":\"medium\"},\"tool_choice\":\"auto\",\"tool_usage\":{\"image_gen\":{\"input_tokens\":0,\"input_tokens_details\":{\"image_tokens\":0,\"text_tokens\":0},\"output_tokens\":0,\"output_tokens_details\":{\"image_tokens\":0,\"text_tokens\":0},\"total_tokens\":0},\"web_search\":{\"num_requests\":0}},\"tools\":[{\"type\":\"image_generation\",\"background\":\"auto\",\"model\":\"gpt-image-2-codex\",\"moderation\":\"auto\",\"n\":1,\"output_compression\":100,\"output_format\":\"png\",\"quality\":\"auto\",\"size\":\"auto\"}],\"top_logprobs\":0,\"top_p\":0.98,\"truncation\":\"disabled\",\"usage\":{\"input_tokens\":14897,\"input_tokens_details\":{\"cache_write_tokens\":0,\"cached_tokens\":0},\"output_tokens\":352,\"output_tokens_details\":{\"reasoning_tokens\":0},\"total_tokens\":15249},\"user\":null,\"metadata\":{}}',29375,14897,352,'2026-08-13 08:02:15'),(6,9,'openai-compatible','gpt-5.6-sol','governance-v1',0,0.9400,'HIGH',0,0,'收到模拟器上报的车辆盗窃风险事件，但未提供图片，无法核实是否存在未佩戴头盔行为或读取关联校园电动车车牌。','仅进行现场复核和视频调阅，不执行处罚；核实车辆状态及车主身份后再行处置。','东校区正门于2026-08-13 21:35:16收到车辆盗窃风险模拟告警，当前缺少图像证据，无法确认违规行为和车牌，请安排现场或视频复核。','[\"事件来源为SIMULATOR，设备为demo-camera-east-gate。\",\"上报车牌为空，身份置信度为0.0。\",\"未提供事件图片，无法确认骑行人是否佩戴头盔。\"]','{\"id\":\"resp_089922df87ad209a016a7dc816f9bc81998e79d7e5a38fa09b\",\"object\":\"response\",\"created_at\":1786628119,\"status\":\"completed\",\"background\":false,\"completed_at\":1786628123,\"error\":null,\"frequency_penalty\":0.0,\"incomplete_details\":null,\"instructions\":null,\"max_output_tokens\":null,\"max_tool_calls\":null,\"model\":\"gpt-5.6-sol\",\"moderation\":null,\"output\":[{\"type\":\"message\",\"role\":\"assistant\",\"content\":[{\"type\":\"output_text\",\"text\":\"{\\\"valid\\\":false,\\\"eventType\\\":\\\"THEFT_RISK\\\",\\\"confidence\\\":0.94,\\\"riskLevel\\\":\\\"HIGH\\\",\\\"identityReliable\\\":false,\\\"evidenceSufficient\\\":false,\\\"plateVisible\\\":false,\\\"recognizedPlate\\\":null,\\\"plateConfidence\\\":0.0,\\\"summary\\\":\\\"收到模拟器上报的车辆盗窃风险事件，但未提供图片，无法核实是否存在未佩戴头盔行为或读取关联校园电动车车牌。\\\",\\\"recommendedAction\\\":\\\"仅进行现场复核和视频调阅，不执行处罚；核实车辆状态及车主身份后再行处置。\\\",\\\"notificationText\\\":\\\"东校区正门于2026-08-13 21:35:16收到车辆盗窃风险模拟告警，当前缺少图像证据，无法确认违规行为和车牌，请安排现场或视频复核。\\\",\\\"observations\\\":[\\\"事件来源为SIMULATOR，设备为demo-camera-east-gate。\\\",\\\"上报车牌为空，身份置信度为0.0。\\\",\\\"未提供事件图片，无法确认骑行人是否佩戴头盔。\\\"]}\"}]}],\"parallel_tool_calls\":false,\"presence_penalty\":0.0,\"previous_response_id\":null,\"prompt_cache_key\":\"019ffb55-955e-7ddf-8990-408362452e05\",\"prompt_cache_retention\":\"24h\",\"reasoning\":{\"context\":\"all_turns\",\"effort\":\"none\",\"mode\":\"standard\",\"summary\":null},\"safety_identifier\":\"user-VvZiBQIrOsiTeYsHGfHHBUxX\",\"service_tier\":\"default\",\"store\":false,\"temperature\":1.0,\"text\":{\"format\":{\"type\":\"text\"},\"verbosity\":\"low\"},\"tool_choice\":\"auto\",\"tool_usage\":{\"image_gen\":{\"input_tokens\":0,\"input_tokens_details\":{\"image_tokens\":0,\"text_tokens\":0},\"output_tokens\":0,\"output_tokens_details\":{\"image_tokens\":0,\"text_tokens\":0},\"total_tokens\":0},\"web_search\":{\"num_requests\":0}},\"tools\":[],\"top_logprobs\":0,\"top_p\":0.98,\"truncation\":\"disabled\",\"usage\":{\"input_tokens\":3797,\"input_tokens_details\":{\"cache_write_tokens\":0,\"cached_tokens\":0},\"output_tokens\":238,\"output_tokens_details\":{\"reasoning_tokens\":0},\"total_tokens\":4035},\"user\":null,\"metadata\":{}}',8755,3797,238,'2026-08-13 21:35:25'),(7,17,'mock-fallback','mock-governance-v1','governance-v1',1,0.9500,'MEDIUM',0,1,'检测到驾驶人员疑似未佩戴安全头盔','CREATE_REVIEW_TASK','系统检测到您的车辆可能存在未佩戴头盔行为，请及时查看事件详情并按要求整改。','[\"连续关键帧均未发现有效头盔目标\",\"建议生成待确认治理事件\",\"在线AI分析暂不可用，本次结果由演示分析生成，必须人工复核\"]','fallback-reason:多模态视觉分析服务调用失败，HTTP 401',180,NULL,NULL,'2026-08-22 11:10:20'),(8,18,'mock-fallback','mock-governance-v1','governance-v1',1,0.9500,'MEDIUM',0,1,'检测到驾驶人员疑似未佩戴安全头盔','CREATE_REVIEW_TASK','系统检测到您的车辆可能存在未佩戴头盔行为，请及时查看事件详情并按要求整改。','[\"连续关键帧均未发现有效头盔目标\",\"建议生成待确认治理事件\",\"在线AI分析暂不可用，本次结果由演示分析生成，必须人工复核\"]','fallback-reason:多模态视觉分析服务调用失败，HTTP 401',180,NULL,NULL,'2026-08-22 11:17:05'),(9,24,'mock-fallback','mock-governance-v1','governance-v1',1,0.9500,'MEDIUM',0,1,'检测到驾驶人员疑似未佩戴安全头盔','CREATE_REVIEW_TASK','系统检测到您的车辆可能存在未佩戴头盔行为，请及时查看事件详情并按要求整改。','[\"连续关键帧均未发现有效头盔目标\",\"建议生成待确认治理事件\",\"在线AI分析暂不可用，本次结果由本地规则兜底生成，必须人工复核\"]','fallback-reason:在线AI能力未配置有效凭证',180,NULL,NULL,'2026-08-22 15:34:06'),(10,25,'openai-compatible','deepseek-v4-flash','governance-v1',1,0.9400,'MEDIUM',0,1,'无人机在屏风校区北门及宿舍区巡逻时，捕捉到骑行者未佩戴头盔的行为。画面清晰，但车牌识别置信度较低（0.6），需要人工复核车牌。该行为违反校园交通安全规定，存在一定安全风险。','通知相关管理人员进行现场或视频复核，确认骑行人员身份后，按照校园规定进行教育和提醒，并加强该区域的头盔佩戴宣传和抽查。','尊敬的校园安全管理部门：无人机在2026-08-24T20:47:22于屏风校区北门及宿舍区发现一名骑行者未佩戴头盔，疑似违规。车牌号为桂C12345（识别置信度中等），请安排人员核实。','[\"图像中可见骑行者头部无头盔，面部特征清晰度一般。\",\"车牌区域存在一定模糊，数字和字母基本可读但置信度非满分。\",\"现场周边无其他干扰因素，但天色较暗，可能影响识别精度。\",\"无人机拍摄角度为正上方偏侧，能清晰看到骑行者的头部和车辆后方车牌。\"]','{\"id\":\"e72b64b9-6a28-4f65-b0bf-199d04d74df1\",\"object\":\"chat.completion\",\"created\":1787575842,\"model\":\"deepseek-v4-flash\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"{\\n  \\\"valid\\\": true,\\n  \\\"eventType\\\": \\\"NO_HELMET\\\",\\n  \\\"confidence\\\": 0.94,\\n  \\\"riskLevel\\\": \\\"MEDIUM\\\",\\n  \\\"identityReliable\\\": false,\\n  \\\"evidenceSufficient\\\": true,\\n  \\\"plateVisible\\\": true,\\n  \\\"recognizedPlate\\\": \\\"桂C12345\\\",\\n  \\\"plateConfidence\\\": 0.6,\\n  \\\"summary\\\": \\\"无人机在屏风校区北门及宿舍区巡逻时，捕捉到骑行者未佩戴头盔的行为。画面清晰，但车牌识别置信度较低（0.6），需要人工复核车牌。该行为违反校园交通安全规定，存在一定安全风险。\\\",\\n  \\\"recommendedAction\\\": \\\"通知相关管理人员进行现场或视频复核，确认骑行人员身份后，按照校园规定进行教育和提醒，并加强该区域的头盔佩戴宣传和抽查。\\\",\\n  \\\"notificationText\\\": \\\"尊敬的校园安全管理部门：无人机在2026-08-24T20:47:22于屏风校区北门及宿舍区发现一名骑行者未佩戴头盔，疑似违规。车牌号为桂C12345（识别置信度中等），请安排人员核实。\\\",\\n  \\\"observations\\\": [\\n    \\\"图像中可见骑行者头部无头盔，面部特征清晰度一般。\\\",\\n    \\\"车牌区域存在一定模糊，数字和字母基本可读但置信度非满分。\\\",\\n    \\\"现场周边无其他干扰因素，但天色较暗，可能影响识别精度。\\\",\\n    \\\"无人机拍摄角度为正上方偏侧，能清晰看到骑行者的头部和车辆后方车牌。\\\"\\n  ]\\n}\"},\"logprobs\":null,\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":326,\"completion_tokens\":335,\"total_tokens\":661,\"prompt_tokens_details\":{\"cached_tokens\":256},\"prompt_cache_hit_tokens\":256,\"prompt_cache_miss_tokens\":70},\"system_fingerprint\":\"a26a7955944dc5c60445bff77fac9c8e\"}',3717,326,335,'2026-08-24 20:50:46'),(11,26,'mock','mock-governance-v1','governance-v1',1,0.9500,'MEDIUM',0,1,'检测到驾驶人员疑似未佩戴安全头盔','CREATE_REVIEW_TASK','系统检测到您的车辆可能存在未佩戴头盔行为，请及时查看事件详情并按要求整改。','[\"连续关键帧均未发现有效头盔目标\",\"建议生成待确认治理事件\"]','mock:NO_HELMET',180,NULL,NULL,'2026-09-03 14:11:37'),(12,28,'mock','mock-governance-v1','governance-v1',1,0.9500,'MEDIUM',0,1,'检测到驾驶人员疑似未佩戴安全头盔','CREATE_REVIEW_TASK','系统检测到您的车辆可能存在未佩戴头盔行为，请及时查看事件详情并按要求整改。','[\"连续关键帧均未发现有效头盔目标\",\"建议生成待确认治理事件\"]','mock:NO_HELMET',180,NULL,NULL,'2026-09-09 20:09:58');
/*!40000 ALTER TABLE `ai_analysis` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_appeal_review`
--

DROP TABLE IF EXISTS `ai_appeal_review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_appeal_review` (
  `review_id` bigint NOT NULL AUTO_INCREMENT,
  `appeal_id` int NOT NULL,
  `conclusion` varchar(32) NOT NULL,
  `confidence` decimal(5,4) NOT NULL DEFAULT '0.0000',
  `evidence_analysis` text,
  `policy_refs_json` text,
  `recommendation` varchar(64) NOT NULL,
  `model_name` varchar(64) DEFAULT NULL,
  `raw_response` longtext,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`review_id`),
  KEY `idx_ai_appeal_review` (`appeal_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI申诉预审建议';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_appeal_review`
--

LOCK TABLES `ai_appeal_review` WRITE;
/*!40000 ALTER TABLE `ai_appeal_review` DISABLE KEYS */;
INSERT INTO `ai_appeal_review` VALUES (1,1,'APPEAL_EVIDENCE_MISSING',0.8800,'**结论：申诉 1 暂无法直接认定成立或驳回，建议转管理员人工复核。** 当前缺少原始违规证据及申诉证明材料，无法核验是否确在校方允许的充电桩充电，也无法排除识别错误。\n\n**关键数据**\n- 违规类型：违规充电\n- 时间：2024-01-13 18:45:00\n- 地点：经度 112.626789，纬度 26.902345\n- 申诉理由：在宿舍楼下允许的充电桩充电，可能存在无人机误识别\n- 原始证据：未提供\n- 申诉证据：未提供\n- 初步结果：申诉证据缺失\n\n**规则依据**\n- **RULE-04**：违规事件须有可核验的时间、地点、车辆身份及图像或轨迹证据；证据不足必须转人工复核。\n- **RULE-05**：申诉结论及处罚须由管理员确认，AI仅提供研判建议。\n- **RULE-06**：申诉人可补充充电现场图片、视频、车辆轨迹或其他证明材料。','[{\"document_id\":1,\"title\":\"校园电动车安全治理规则（平台演示版）\",\"version\":\"DEMO-1.0\",\"section_code\":\"RULE-04\",\"heading\":\"证据与人工复核\",\"content\":\"违规事件应具有可核验的时间、地点、车辆身份和图像或轨迹证据；证据不足时必须转人工复核。\"},{\"document_id\":1,\"title\":\"校园电动车安全治理规则（平台演示版）\",\"version\":\"DEMO-1.0\",\"section_code\":\"RULE-05\",\"heading\":\"处罚边界\",\"content\":\"正式扣分、处罚、车辆冻结和申诉结论必须由管理员确认，AI仅提供研判建议。\"},{\"document_id\":1,\"title\":\"校园电动车安全治理规则（平台演示版）\",\"version\":\"DEMO-1.0\",\"section_code\":\"RULE-06\",\"heading\":\"申诉权利\",\"content\":\"用户可在规定期限内提出申诉，并提交图片、视频、轨迹或其他证明材料。\"}]','REQUEST_EVIDENCE','gpt-5.6-sol','{\"id\":\"resp_06122d3c875a97b5016a7acf1849ec8199bd2117262ae4d734\",\"object\":\"response\",\"created_at\":1786433304,\"status\":\"completed\",\"background\":false,\"completed_at\":1786433310,\"error\":null,\"frequency_penalty\":0.0,\"incomplete_details\":null,\"instructions\":\"你是校园电动车治理分析助手。只能依据提供的只读查询结果和校规片段回答，不得编造数据，不得声称已执行处罚。输出简洁中文，先给结论，再列关键数据和规则依据。\",\"max_output_tokens\":null,\"max_tool_calls\":null,\"model\":\"gpt-5.6-sol\",\"moderation\":null,\"output\":[{\"id\":\"msg_06122d3c875a97b5016a7acf19ad888199bbb73840c8e1def6\",\"type\":\"message\",\"status\":\"completed\",\"content\":[{\"type\":\"output_text\",\"annotations\":[],\"logprobs\":[],\"text\":\"**结论：申诉 1 暂无法直接认定成立或驳回，建议转管理员人工复核。** 当前缺少原始违规证据及申诉证明材料，无法核验是否确在校方允许的充电桩充电，也无法排除识别错误。\\n\\n**关键数据**\\n- 违规类型：违规充电\\n- 时间：2024-01-13 18:45:00\\n- 地点：经度 112.626789，纬度 26.902345\\n- 申诉理由：在宿舍楼下允许的充电桩充电，可能存在无人机误识别\\n- 原始证据：未提供\\n- 申诉证据：未提供\\n- 初步结果：申诉证据缺失\\n\\n**规则依据**\\n- **RULE-04**：违规事件须有可核验的时间、地点、车辆身份及图像或轨迹证据；证据不足必须转人工复核。\\n- **RULE-05**：申诉结论及处罚须由管理员确认，AI仅提供研判建议。\\n- **RULE-06**：申诉人可补充充电现场图片、视频、车辆轨迹或其他证明材料。\"}],\"phase\":\"final_answer\",\"role\":\"assistant\"}],\"parallel_tool_calls\":true,\"presence_penalty\":0.0,\"previous_response_id\":null,\"prompt_cache_key\":\"7b3ad8d6-a005-5885-803f-7787240abf74\",\"prompt_cache_retention\":\"24h\",\"reasoning\":{\"context\":\"all_turns\",\"effort\":\"low\",\"mode\":\"standard\",\"summary\":null},\"safety_identifier\":\"user-582mmzFLWe9swkSdWqA6FS3V\",\"service_tier\":\"default\",\"store\":false,\"temperature\":1.0,\"text\":{\"format\":{\"type\":\"text\"},\"verbosity\":\"medium\"},\"tool_choice\":\"auto\",\"tool_usage\":{\"image_gen\":{\"input_tokens\":0,\"input_tokens_details\":{\"image_tokens\":0,\"text_tokens\":0},\"output_tokens\":0,\"output_tokens_details\":{\"image_tokens\":0,\"text_tokens\":0},\"total_tokens\":0},\"web_search\":{\"num_requests\":0}},\"tools\":[{\"type\":\"image_generation\",\"background\":\"auto\",\"model\":\"gpt-image-2-codex\",\"moderation\":\"auto\",\"n\":1,\"output_compression\":100,\"output_format\":\"png\",\"quality\":\"auto\",\"size\":\"auto\"}],\"top_logprobs\":0,\"top_p\":0.98,\"truncation\":\"disabled\",\"usage\":{\"input_tokens\":719,\"input_tokens_details\":{\"cache_write_tokens\":0,\"cached_tokens\":0},\"output_tokens\":274,\"output_tokens_details\":{\"reasoning_tokens\":0},\"total_tokens\":993},\"user\":null,\"metadata\":{}}','2026-08-11 15:28:35');
/*!40000 ALTER TABLE `ai_appeal_review` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_assistant_message`
--

DROP TABLE IF EXISTS `ai_assistant_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_assistant_message` (
  `message_id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(48) NOT NULL,
  `role` varchar(16) NOT NULL,
  `content` longtext NOT NULL,
  `intent` varchar(48) DEFAULT NULL,
  `tool_names` varchar(500) DEFAULT NULL,
  `citations_json` text,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`message_id`),
  KEY `idx_assistant_message_session` (`session_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI治理助手消息';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_assistant_message`
--

LOCK TABLES `ai_assistant_message` WRITE;
/*!40000 ALTER TABLE `ai_assistant_message` DISABLE KEYS */;
INSERT INTO `ai_assistant_message` VALUES (1,'AS-01E0594A417E47B193F0','user','分析最近7天校园电动车治理热点，并引用校规依据',NULL,NULL,NULL,'2026-08-11 15:09:16'),(2,'AS-01E0594A417E47B193F0','assistant','已完成近7天热点分析，共返回 2 个区域。参考 RULE-04《证据与人工复核》。','HOTSPOT_ANALYSIS','[\"QUERY_EVENT_HOTSPOTS\",\"POLICY_SEARCH\"]','[{\"document_id\":1,\"title\":\"校园电动车安全治理规则（平台演示版）\",\"version\":\"DEMO-1.0\",\"section_code\":\"RULE-04\",\"heading\":\"证据与人工复核\",\"content\":\"违规事件应具有可核验的时间、地点、车辆身份和图像或轨迹证据；证据不足时必须转人工复核。\"}]','2026-08-11 15:09:18'),(3,'AS-CB5340D493EA4AC0A6B9','user','分析最近7天校园电动车治理热点，并引用校规依据',NULL,NULL,NULL,'2026-08-11 15:24:12'),(4,'AS-CB5340D493EA4AC0A6B9','assistant','## 结论\n\n最近7天共记录 **2起**校园电动车相关事件，分别位于 **东校区正门**和**冒烟测试点**，各1起；最高风险均为**中风险（MEDIUM）**。事件数量较少且分散，暂未形成明显的高频聚集热点。\n\n## 关键数据\n\n统计时段：**2026-08-04 15:24 至 2026-08-11 15:24**\n\n- 东校区正门：1起，最高风险为中风险\n- 冒烟测试点：1起，最高风险为中风险\n- 合计：2起\n- 高风险事件：0起\n\n## 校规依据\n\n《校园电动车安全治理规则（平台演示版）》**RULE-04“证据与人工复核”**规定：\n\n> 违规事件应具有可核验的时间、地点、车辆身份和图像或轨迹证据；证据不足时必须转人工复核。\n\n因此，这2起事件是否构成违规，仍需核验车辆身份及图像或轨迹证据；证据不足的，应转人工复核，不能仅凭地点和风险等级直接认定或处罚。','HOTSPOT_ANALYSIS','[\"QUERY_EVENT_HOTSPOTS\",\"POLICY_SEARCH\"]','[{\"document_id\":1,\"title\":\"校园电动车安全治理规则（平台演示版）\",\"version\":\"DEMO-1.0\",\"section_code\":\"RULE-04\",\"heading\":\"证据与人工复核\",\"content\":\"违规事件应具有可核验的时间、地点、车辆身份和图像或轨迹证据；证据不足时必须转人工复核。\"}]','2026-08-11 15:24:28'),(5,'AS-D4B6E54134AE463C8911','user','你好',NULL,NULL,NULL,'2026-08-11 15:50:45'),(6,'AS-D4B6E54134AE463C8911','assistant','结论：你好！近7日校园电动车治理共记录3起事件，暂无确认违规，当前以人工复核和任务处置为主。\n\n**关键数据**\n- 待人工复核：2起\n- 已驳回：1起\n- 未结任务：2项\n- 待处理申诉：1项\n- 跨摄像头关联：0起\n- 热点：东校区正门2起；冒烟测试点1起，最高风险均为中等\n\n**规则依据**\n- RULE-04：违规事件须具备可核验的时间、地点、车辆身份及图像或轨迹证据；证据不足时必须转人工复核。','GOVERNANCE_OVERVIEW','[\"QUERY_GOVERNANCE_OVERVIEW\",\"POLICY_SEARCH\"]','[{\"document_id\":1,\"title\":\"校园电动车安全治理规则（平台演示版）\",\"version\":\"DEMO-1.0\",\"section_code\":\"RULE-04\",\"heading\":\"证据与人工复核\",\"content\":\"违规事件应具有可核验的时间、地点、车辆身份和图像或轨迹证据；证据不足时必须转人工复核。\"}]','2026-08-11 15:51:01'),(7,'AS-D4B6E54134AE463C8911','user','你是什么模型',NULL,NULL,NULL,'2026-08-11 15:51:28'),(8,'AS-D4B6E54134AE463C8911','assistant','**结论：**我是校园电动车治理分析助手，负责依据只读查询结果和校规片段进行分析；当前上下文未提供具体的底层模型名称。\n\n**限制说明：**\n- 不编造数据或治理事实。\n- 不声称已执行处罚。\n- 证据不足的违规事件须按 **RULE-04** 转人工复核。','GOVERNANCE_OVERVIEW','[\"QUERY_GOVERNANCE_OVERVIEW\",\"POLICY_SEARCH\"]','[{\"document_id\":1,\"title\":\"校园电动车安全治理规则（平台演示版）\",\"version\":\"DEMO-1.0\",\"section_code\":\"RULE-04\",\"heading\":\"证据与人工复核\",\"content\":\"违规事件应具有可核验的时间、地点、车辆身份和图像或轨迹证据；证据不足时必须转人工复核。\"}]','2026-08-11 15:51:36'),(9,'AS-1B2EE8D8D91C423988CD','user','近7天哪个区域事件最多？',NULL,NULL,NULL,'2026-09-09 20:10:07'),(10,'AS-1B2EE8D8D91C423988CD','assistant','已完成近7天热点分析，共返回 0 个区域。参考 RULE-04《证据与人工复核》。','HOTSPOT_ANALYSIS','[\"QUERY_EVENT_HOTSPOTS\",\"POLICY_SEARCH\"]','[{\"document_id\":1,\"title\":\"校园电动车安全治理规则（平台演示版）\",\"version\":\"DEMO-1.0\",\"section_code\":\"RULE-04\",\"heading\":\"证据与人工复核\",\"content\":\"违规事件应具有可核验的时间、地点、车辆身份和图像或轨迹证据；证据不足时必须转人工复核。\"}]','2026-09-09 20:10:07'),(11,'AS-1B2EE8D8D91C423988CD','user','你好',NULL,NULL,NULL,'2026-09-09 20:10:17'),(12,'AS-1B2EE8D8D91C423988CD','assistant','已完成近7天校园电动车治理概览。数据来自AI事件、治理任务、跨设备关联和申诉记录。参考 RULE-04《证据与人工复核》。','GOVERNANCE_OVERVIEW','[\"QUERY_GOVERNANCE_OVERVIEW\",\"POLICY_SEARCH\"]','[{\"document_id\":1,\"title\":\"校园电动车安全治理规则（平台演示版）\",\"version\":\"DEMO-1.0\",\"section_code\":\"RULE-04\",\"heading\":\"证据与人工复核\",\"content\":\"违规事件应具有可核验的时间、地点、车辆身份和图像或轨迹证据；证据不足时必须转人工复核。\"}]','2026-09-09 20:10:17'),(13,'AS-7346D9D9DBD24E259D10','user','你好',NULL,NULL,NULL,'2026-09-09 21:01:07'),(14,'AS-7346D9D9DBD24E259D10','assistant','已完成近7天校园电动车治理概览。数据来自AI事件、治理任务、跨设备关联和申诉记录。参考 RULE-04《证据与人工复核》。','GOVERNANCE_OVERVIEW','[\"QUERY_GOVERNANCE_OVERVIEW\",\"POLICY_SEARCH\"]','[{\"document_id\":1,\"title\":\"校园电动车安全治理规则（平台演示版）\",\"version\":\"DEMO-1.0\",\"section_code\":\"RULE-04\",\"heading\":\"证据与人工复核\",\"content\":\"违规事件应具有可核验的时间、地点、车辆身份和图像或轨迹证据；证据不足时必须转人工复核。\"}]','2026-09-09 21:01:07'),(15,'AS-F4D1099578BE47A0B64C','user','在吗',NULL,NULL,NULL,'2026-09-09 21:01:33'),(16,'AS-F4D1099578BE47A0B64C','assistant','已完成近7天校园电动车治理概览。数据来自AI事件、治理任务、跨设备关联和申诉记录。参考 RULE-04《证据与人工复核》。','GOVERNANCE_OVERVIEW','[\"QUERY_GOVERNANCE_OVERVIEW\",\"POLICY_SEARCH\"]','[{\"document_id\":1,\"title\":\"校园电动车安全治理规则（平台演示版）\",\"version\":\"DEMO-1.0\",\"section_code\":\"RULE-04\",\"heading\":\"证据与人工复核\",\"content\":\"违规事件应具有可核验的时间、地点、车辆身份和图像或轨迹证据；证据不足时必须转人工复核。\"}]','2026-09-09 21:01:33'),(17,'AS-1BE81081C12E49198A25','user','你好',NULL,NULL,NULL,'2026-09-09 21:05:02'),(18,'AS-1BE81081C12E49198A25','assistant','你好，我是校园电动车治理助手。你可以直接问我热点区域、事件趋势、重复车辆、待复核任务、申诉预审或治理报告。当前为本地模拟对话模式。','CONVERSATION','CONVERSATION_SIMULATOR',NULL,'2026-09-09 21:05:02'),(19,'AS-1BE81081C12E49198A25','user','在吗',NULL,NULL,NULL,'2026-09-09 21:05:09'),(20,'AS-1BE81081C12E49198A25','assistant','我已收到你的消息。当前是本地模拟智能体，会结合当前会话提供治理分析建议。你可以直接询问近7天热点、趋势、重复车辆、待复核任务或报告预览。','CONVERSATION','CONVERSATION_SIMULATOR',NULL,'2026-09-09 21:05:09'),(21,'AS-1BE81081C12E49198A25','user','？会说人话吗',NULL,NULL,NULL,'2026-09-09 21:05:24'),(22,'AS-1BE81081C12E49198A25','assistant','已完成近7天校园电动车治理概览。数据来自AI事件、治理任务、跨设备关联和申诉记录。参考 RULE-04《证据与人工复核》。','GOVERNANCE_OVERVIEW','[\"QUERY_GOVERNANCE_OVERVIEW\",\"POLICY_SEARCH\"]','[{\"document_id\":1,\"title\":\"校园电动车安全治理规则（平台演示版）\",\"version\":\"DEMO-1.0\",\"section_code\":\"RULE-04\",\"heading\":\"证据与人工复核\",\"content\":\"违规事件应具有可核验的时间、地点、车辆身份和图像或轨迹证据；证据不足时必须转人工复核。\"}]','2026-09-09 21:05:24'),(23,'AS-F12E09F506BB40D5A559','user','你好，请用一句话介绍你能做什么？',NULL,NULL,NULL,'2026-09-09 21:15:13'),(24,'AS-F12E09F506BB40D5A559','assistant','我能为您提供校园电动车日常监测管理，包括查看违规、处理预警和审核记录，但所有处罚通知与状态变更均需您确认后执行。','CONVERSATION','MODEL_CONVERSATION',NULL,'2026-09-09 21:15:14'),(25,'AS-6E35A6E29B16421796BA','user','在吗',NULL,NULL,NULL,'2026-09-09 21:15:57'),(26,'AS-6E35A6E29B16421796BA','assistant','在的，这里是校园电动车监测管理系统。有什么需要我帮忙的吗？您可以直接告诉我，比如查看校园电动车数据、处理违规通知，或者了解一下系统功能。','CONVERSATION','MODEL_CONVERSATION',NULL,'2026-09-09 21:15:58'),(27,'AS-6E35A6E29B16421796BA','user','你是',NULL,NULL,NULL,'2026-09-09 21:16:03'),(28,'AS-6E35A6E29B16421796BA','assistant','**结论：** 当前治理工作以“人工复核”和“待办任务”为主，暂无已确认的违规事件或热点聚集。\n\n**关键数据（2026-09-02 至 2026-09-09）：**\n- 总事件数：0\n- 已确认违规：0\n- **待人工复核：2**（按校规 RULE-04，证据不足时须转人工复核）\n- 被驳回：0\n- **待办任务：9**\n- **待处理申诉：1**\n- 跨摄像头关联：0\n- 热点区域：无\n\n**规则依据：**\n- 《校园电动车安全治理规则（平台演示版）》（DEMO-1.0）RULE-04：违规事件须有可核验的时间、地点、车辆身份和图像/轨迹证据；证据不足时必须转人工复核。当前 2 起待复核事件即对应此程序。','GOVERNANCE_OVERVIEW','[\"QUERY_GOVERNANCE_OVERVIEW\",\"POLICY_SEARCH\"]','[{\"document_id\":1,\"title\":\"校园电动车安全治理规则（平台演示版）\",\"version\":\"DEMO-1.0\",\"section_code\":\"RULE-04\",\"heading\":\"证据与人工复核\",\"content\":\"违规事件应具有可核验的时间、地点、车辆身份和图像或轨迹证据；证据不足时必须转人工复核。\"}]','2026-09-09 21:16:05'),(29,'AS-6E35A6E29B16421796BA','user','你是谁',NULL,NULL,NULL,'2026-09-09 21:16:17'),(30,'AS-6E35A6E29B16421796BA','assistant','您好，我是校园电动车监测管理系统的中文治理助手，可以协助您查看校园电动车运行情况、处理违规治理事宜。但请注意，涉及处罚、通知、冻结或申诉状态变更时，我只会给出建议，需要您（管理员）来最终确认。有什么可以帮您的？','CONVERSATION','MODEL_CONVERSATION',NULL,'2026-09-09 21:16:19'),(31,'AS-E2C36B65CFB64C94AD9A','user','你好，请简要说明你能帮管理员做什么？',NULL,NULL,NULL,'2026-09-10 08:59:52'),(32,'AS-E2C36B65CFB64C94AD9A','assistant','您好，我是校园电动车监测管理系统的中文治理助手。我可以帮您做这些事：\n\n- **查询与汇总**：帮您查询车辆、车主、登记状态、通行记录等（以系统实际数据为准）。\n- **违规处理辅助**：梳理违停、超速、逆行等违规记录，给出处理建议。\n- **通知与提醒起草**：帮您撰写通知、警告、整改提醒等文本。\n- **申诉与流程说明**：解释申诉流程、所需材料，整理待办事项。\n- **数据核对与统计**：按时间、区域、车辆类型等做统计汇总。\n\n需要说明的是：涉及**处罚、通知发送、账号冻结、申诉状态变更**等操作，我不能直接执行，必须由您确认后再处理；我也不会编造系统里没有的数据。\n\n您想先从哪方面开始？','CONVERSATION','MODEL_CONVERSATION',NULL,'2026-09-10 08:59:54');
/*!40000 ALTER TABLE `ai_assistant_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_assistant_session`
--

DROP TABLE IF EXISTS `ai_assistant_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_assistant_session` (
  `session_id` varchar(48) NOT NULL,
  `user_id` int NOT NULL,
  `title` varchar(200) NOT NULL,
  `context_event_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`session_id`),
  KEY `idx_assistant_user_time` (`user_id`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI治理助手会话';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_assistant_session`
--

LOCK TABLES `ai_assistant_session` WRITE;
/*!40000 ALTER TABLE `ai_assistant_session` DISABLE KEYS */;
INSERT INTO `ai_assistant_session` VALUES ('AS-01E0594A417E47B193F0',1,'分析最近7天校园电动车治理热点，并引用校规依据',NULL,'2026-08-11 15:09:16','2026-08-11 15:09:18'),('AS-1B2EE8D8D91C423988CD',1,'近7天哪个区域事件最多？',NULL,'2026-09-09 20:10:07','2026-09-09 20:10:17'),('AS-1BE81081C12E49198A25',1,'你好',NULL,'2026-09-09 21:05:02','2026-09-09 21:05:24'),('AS-6E35A6E29B16421796BA',1,'在吗',NULL,'2026-09-09 21:15:57','2026-09-09 21:16:19'),('AS-7346D9D9DBD24E259D10',1,'你好',NULL,'2026-09-09 21:01:07','2026-09-09 21:01:08'),('AS-CB5340D493EA4AC0A6B9',1,'分析最近7天校园电动车治理热点，并引用校规依据',NULL,'2026-08-11 15:24:12','2026-08-11 15:24:29'),('AS-D4B6E54134AE463C8911',1,'你好',NULL,'2026-08-11 15:50:45','2026-08-11 15:51:37'),('AS-E2C36B65CFB64C94AD9A',1,'你好，请简要说明你能帮管理员做什么？',NULL,'2026-09-10 08:59:52','2026-09-10 08:59:55'),('AS-F12E09F506BB40D5A559',1,'你好，请用一句话介绍你能做什么？',NULL,'2026-09-09 21:15:13','2026-09-09 21:15:15'),('AS-F4D1099578BE47A0B64C',1,'在吗',NULL,'2026-09-09 21:01:33','2026-09-09 21:01:34');
/*!40000 ALTER TABLE `ai_assistant_session` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_event`
--

DROP TABLE IF EXISTS `ai_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_event` (
  `event_id` bigint NOT NULL AUTO_INCREMENT,
  `event_code` varchar(48) NOT NULL,
  `event_type` varchar(32) NOT NULL,
  `source_type` varchar(32) NOT NULL,
  `device_id` varchar(64) DEFAULT NULL,
  `occurred_at` datetime NOT NULL,
  `location_name` varchar(128) DEFAULT NULL,
  `longitude` decimal(11,7) DEFAULT NULL,
  `latitude` decimal(10,7) DEFAULT NULL,
  `license_plate` varchar(32) DEFAULT NULL,
  `vehicle_id` int DEFAULT NULL,
  `user_id` int DEFAULT NULL,
  `identity_method` varchar(32) DEFAULT NULL,
  `identity_confidence` decimal(5,4) NOT NULL DEFAULT '0.0000',
  `detection_confidence` decimal(5,4) NOT NULL DEFAULT '0.0000',
  `evidence_urls` text,
  `raw_payload` longtext,
  `status` varchar(32) NOT NULL,
  `risk_level` varchar(16) DEFAULT NULL,
  `dedupe_key` varchar(96) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`event_id`),
  UNIQUE KEY `uk_ai_event_code` (`event_code`),
  UNIQUE KEY `uk_ai_event_dedupe` (`dedupe_key`),
  KEY `idx_ai_event_status_time` (`status`,`occurred_at`),
  KEY `idx_ai_event_vehicle` (`vehicle_id`,`occurred_at`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI原始事件';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_event`
--

LOCK TABLES `ai_event` WRITE;
/*!40000 ALTER TABLE `ai_event` DISABLE KEYS */;
INSERT INTO `ai_event` VALUES (1,'AI-C8A2ACDF438D49DF','NO_HELMET','SIMULATOR','demo-camera-east-gate','2026-08-11 09:47:43','东校区正门',NULL,NULL,NULL,NULL,NULL,'UNMATCHED',0.0000,0.9400,'[\"/demo/evidence/no_helmet.jpg\"]','{}','REVIEW_PENDING','MEDIUM','4f50302fa11dcd709910e1d97e7870fea74265667307229535499d826a88913f','2026-08-11 09:47:43','2026-08-11 09:47:43'),(2,'AI-DB159FC1F9B8495F','ILLEGAL_PARKING','SIMULATOR','smoke-camera','2026-08-11 10:06:41','冒烟测试点',NULL,NULL,'不存在测试车',NULL,NULL,'UNMATCHED',0.0000,0.9400,'[\"https://example.com/evidence.jpg\"]','{}','REJECTED','MEDIUM','eedaf5236eeb0722003c7de1bd920b031cc33b0392e47824adca4a9cd1825c8e','2026-08-11 10:06:41','2026-08-11 10:15:34'),(3,'AI-0DF704871E214A85','NO_HELMET','SIMULATOR','demo-camera-east-gate','2026-08-11 15:49:38','东校区正门',NULL,NULL,NULL,NULL,NULL,'UNMATCHED',0.0000,0.9400,'[\"/demo/evidence/no_helmet.jpg\"]','{}','REVIEW_PENDING','MEDIUM','e8a6b5d60bf469ab36c97678a8552459a0f67dd6e8caa9301df9f647d6b4afa6','2026-08-11 15:49:38','2026-08-11 15:49:51'),(8,'AI-DDA08C7C37A44445','NO_HELMET','CAMERA','codex-real-test-camera','2026-08-13 08:01:44','真实图片识别测试',NULL,NULL,NULL,NULL,NULL,'UNMATCHED',0.0000,0.9300,'[\"/uploads/ai-evidence/2026/08/13/6e25b0df828647f78fcd6054d2ab70e8.jpg\"]','{\"purpose\":\"gpt-multimodal-smoke-test\",\"groundTruth\":[\"two_wheeler\",\"without\"]}','REVIEW_PENDING','HIGH','af1d24ec131f117e3c71ad783ba282183c00409f36908380cb4de6236c26a636','2026-08-13 08:01:45','2026-08-13 08:02:15'),(9,'AI-3319F75E14B14B0A','THEFT_RISK','SIMULATOR','demo-camera-east-gate','2026-08-13 21:35:16','东校区正门',NULL,NULL,NULL,NULL,NULL,'UNMATCHED',0.0000,0.9400,'[\"/demo/evidence/theft_risk.jpg\"]','{}','REJECTED','HIGH','b36656d18e221c7f21b1fdd99350cf24d51209a332b46d88a4a1ef19b6359759','2026-08-13 21:35:16','2026-08-13 21:35:25'),(10,'EVT-CHAIN-FIRE-001','FIRE_DETECTED','CAMERA','camera-3','2026-08-14 04:52:43','三号停车棚',113.0062110,28.1423860,'湘A0599001',43,162,'PLATE',0.9600,0.9100,'[\"/demo/evidence/chain-fire.jpg\"]','{\"cameraId\":3,\"detector\":\"YOLOv8-fire-smoke\",\"demo\":true}','REVIEW_PENDING','HIGH','demo-chain-fire-001','2026-08-14 04:52:43','2026-08-14 08:52:43'),(11,'AI-F34825B6DEA6403D','NO_HELMET','DRONE','patrol-PM-D76E89BDE3F9','2026-08-22 10:59:38','东校区',112.6849000,26.8893000,NULL,NULL,NULL,'UNMATCHED',0.0000,0.9500,'[\"/demo/evidence/patrol.jpg\"]','{\"source\":\"patrol-demo\",\"patrolMissionId\":3}','INGESTED','UNKNOWN','58d4c3a513b01604a6bca5e2e00cd25977a77cf0e9114b6fdbdb4da60b9aaa53','2026-08-22 10:59:38','2026-08-22 10:59:38'),(17,'AI-950ABD7B555A4390','NO_HELMET','DRONE','patrol-PM-D76E89BDE3F9','2026-08-22 11:10:19','东校区',112.6849000,26.8893000,NULL,NULL,NULL,'UNMATCHED',0.0000,0.9500,'[\"/demo/evidence/patrol.jpg\"]','{\"source\":\"patrol-demo\",\"patrolMissionId\":3}','REVIEW_PENDING','MEDIUM','35e8b2d0e24cdb41d1e3869f6361f979d3c4cbf3bbf2b6385e66adee2cc09552','2026-08-22 11:10:19','2026-08-22 11:10:20'),(18,'AI-8EAB8C86F0664D32','NO_HELMET','DRONE','patrol-PM-055DDF7239A6','2026-08-22 11:17:04','东校区',112.6849000,26.8893000,NULL,NULL,NULL,'UNMATCHED',0.0000,0.9500,'[\"/demo/evidence/patrol.jpg\"]','{\"source\":\"patrol-demo\",\"patrolMissionId\":5}','REVIEW_PENDING','MEDIUM','76f758d2d63947895f2769ad1156a4640ca038b49f976cf066899042e4579654','2026-08-22 11:17:04','2026-08-22 11:17:05'),(19,'AI-FE8BD83288964127','NO_HELMET','GROUND_ROBOT','ground-robot-01','2026-08-22 14:09:59','东校区北门',112.6849000,26.8893000,NULL,NULL,NULL,'UNMATCHED',0.0000,0.9100,'[]','{\"test\":true}','INGESTED','UNKNOWN','db507ea7262de91758b3e809bbcbefc1a30eabf9f9c948b19cc75b8e421eeaad','2026-08-22 14:09:59','2026-08-22 14:09:59'),(20,'AI-0B52EBFA3C674964','ILLEGAL_PARKING','VEHICLE_TERMINAL','terminal-001','2026-08-22 14:12:56','东校区停车区',112.6849000,26.8893000,NULL,NULL,NULL,'UNMATCHED',0.0000,0.8800,'[]','{\"battery\":74,\"gpsQuality\":0.98}','INGESTED','UNKNOWN','0dd9d943a641248bc7463ac8459069d93bf400ed3dee0dd94be6b1ef2474e1f1','2026-08-22 14:12:56','2026-08-22 14:12:56'),(21,'AI-589A64158E0B4B24','RESTRICTED_AREA','VEHICLE_TERMINAL','terminal-final-01','2026-08-22 14:23:01','东校区北门',112.6849000,26.8893000,NULL,NULL,NULL,'UNMATCHED',0.0000,0.9300,'[]','{\"source\":\"final-verification\"}','INGESTED','UNKNOWN','17d4708b8fdd12f1901a50b573fc2356613808ba0bac8928f352ce5bdaecfc4a','2026-08-22 14:23:01','2026-08-22 14:23:01'),(22,'AI-56B164DD51A441E0','NO_HELMET','DRONE','patrol-14','2026-08-22 14:44:49','屏风校区南门',110.3239130,25.2875180,NULL,NULL,NULL,'UNMATCHED',0.0000,0.9600,'[]','{\"test\":true,\"missionId\":14}','INGESTED','UNKNOWN','6da1256d272252a603f382a7632515060f68e79aa7538d59c7cff4d7916ead99','2026-08-22 14:44:50','2026-08-22 14:44:50'),(23,'AI-429358607EAC491D','NO_HELMET','DRONE','patrol-15','2026-08-22 14:45:55','屏风校区巡检点',110.3239130,25.2875180,NULL,NULL,NULL,'UNMATCHED',0.0000,0.9600,'[]','{\"source\":\"e2e\",\"missionId\":15}','INGESTED','UNKNOWN','b144bb345ace41641152d8ced0143b9cbaf6302ca5687a93d24c993d3e303f6d','2026-08-22 14:45:56','2026-08-22 14:45:56'),(24,'AI-4463AAA0E8894740','NO_HELMET','DRONE','patrol-PM-D5779E3E69B7','2026-08-22 15:34:06','屏风校区',110.3239200,25.2875200,NULL,NULL,NULL,'UNMATCHED',0.0000,0.9500,'[]','{\"source\":\"patrol-adapter\",\"patrolMissionId\":21,\"evidenceMode\":\"telemetry-only\"}','REVIEW_PENDING','MEDIUM','5f943b560782a1a992805e477994d9ffc232e2869b7af554ca3028170f288bb0','2026-08-22 15:34:06','2026-08-22 15:34:06'),(25,'AI-A4F358C3590547CB','NO_HELMET','DRONE','patrol-PM-B28B1FE5A0BE','2026-08-24 20:47:22','屏风校区北门及宿舍区',110.3239500,25.2875500,'桂C12345',NULL,NULL,'UNMATCHED',0.0000,0.9400,'[]','{\"evidenceMode\":\"telemetry-only\",\"source\":\"patrol-adapter\",\"patrolMissionId\":23}','REVIEW_PENDING','MEDIUM','47aee01a7e06f87a4ae412132393e717d906cd48d8757eac88ce70bcd3216dce','2026-08-24 20:47:22','2026-08-24 20:50:46'),(26,'AI-450AD948EA6142BD','NO_HELMET','CAMERA','camera-east-gate','2026-08-25 11:44:08','东门非机动车道',110.3239100,25.2875180,'桂C77881',NULL,NULL,'UNMATCHED',0.0000,0.9100,'[]','{\"cameraFrame\":\"fusion-camera-1\"}','REVIEW_PENDING','MEDIUM','90ba50a05d8719a5e2bea8801a6b7c003d3b1adf7fb6748887ad6879b4d0042a','2026-08-25 11:44:09','2026-09-03 14:11:37'),(27,'AI-B94E7C86E9CA4552','RESTRICTED_AREA','VEHICLE_TERMINAL','terminal-fusion-01','2026-08-25 11:44:08','东门非机动车道',110.3239180,25.2875220,'桂C77881',NULL,NULL,'UNMATCHED',0.0000,0.8800,'[]','{\"gpsFix\":\"fusion-gps-1\"}','INGESTED','UNKNOWN','1d699574da6bd4e32c619240096d1108f9bb2887117d92a7af9a8a558dbd321e','2026-08-25 11:44:09','2026-08-25 11:44:09'),(28,'AI-3248EABBC01B4D3E','NO_HELMET','CAMERA','camera-east-gate','2026-08-25 11:44:59','东门非机动车道',110.3239100,25.2875180,'桂C77881',NULL,NULL,'UNMATCHED',0.0000,0.9100,'[]','{\"cameraFrame\":\"fusion-camera-2\"}','REVIEW_PENDING','MEDIUM','55791af6149512291c6c6854609bd674c3030ae055fd3c1aaede232835a6b357','2026-08-25 11:45:00','2026-09-09 20:09:58'),(29,'AI-2AD7C82B9CB94EB6','RESTRICTED_AREA','VEHICLE_TERMINAL','terminal-fusion-02','2026-08-25 11:44:59','东门非机动车道',110.3239180,25.2875220,'桂C77881',NULL,NULL,'UNMATCHED',0.0000,0.8800,'[]','{\"gpsFix\":\"fusion-gps-2\"}','INGESTED','UNKNOWN','9f688489ec8494bf5b2bb31e0f7c09d1cba4adce42ef78781f68ae475810b6d5','2026-08-25 11:45:00','2026-08-25 11:45:00');
/*!40000 ALTER TABLE `ai_event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_event_fusion`
--

DROP TABLE IF EXISTS `ai_event_fusion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_event_fusion` (
  `fusion_id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL,
  `source_count` int NOT NULL DEFAULT '1',
  `source_types` varchar(256) NOT NULL,
  `related_event_count` int NOT NULL DEFAULT '0',
  `spatial_score` decimal(5,4) NOT NULL DEFAULT '0.0000',
  `temporal_score` decimal(5,4) NOT NULL DEFAULT '0.0000',
  `identity_score` decimal(5,4) NOT NULL DEFAULT '0.0000',
  `evidence_score` decimal(5,4) NOT NULL DEFAULT '0.0000',
  `fused_risk_score` decimal(5,4) NOT NULL DEFAULT '0.0000',
  `fused_risk_level` varchar(16) NOT NULL DEFAULT 'LOW',
  `explanation` varchar(1200) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`fusion_id`),
  UNIQUE KEY `uk_ai_event_fusion_event` (`event_id`),
  KEY `idx_ai_event_fusion_risk` (`fused_risk_level`,`update_time`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='多源时空融合摘要';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_event_fusion`
--

LOCK TABLES `ai_event_fusion` WRITE;
/*!40000 ALTER TABLE `ai_event_fusion` DISABLE KEYS */;
INSERT INTO `ai_event_fusion` VALUES (1,26,2,'CAMERA,VEHICLE_TERMINAL',3,1.0000,1.0000,0.8200,0.9100,0.9415,'HIGH','融合2类来源、3条关联证据；空间1.00，时间1.00，身份0.82，检测0.91','2026-08-25 11:44:09','2026-09-03 14:11:37'),(2,27,2,'CAMERA,VEHICLE_TERMINAL',1,0.9923,1.0000,0.8200,0.9100,0.9396,'HIGH','融合2类来源、1条关联证据；空间0.99，时间1.00，身份0.82，检测0.91','2026-08-25 11:44:09','2026-08-25 11:44:09'),(3,28,2,'CAMERA,VEHICLE_TERMINAL',3,1.0000,1.0000,0.8200,0.9100,0.9415,'HIGH','融合2类来源、3条关联证据；空间1.00，时间1.00，身份0.82，检测0.91','2026-08-25 11:44:59','2026-09-09 20:09:57'),(4,29,2,'CAMERA,VEHICLE_TERMINAL',3,1.0000,1.0000,0.8200,0.9100,0.9415,'HIGH','融合2类来源、3条关联证据；空间1.00，时间1.00，身份0.82，检测0.91','2026-08-25 11:45:00','2026-08-25 11:45:00');
/*!40000 ALTER TABLE `ai_event_fusion` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_event_relation`
--

DROP TABLE IF EXISTS `ai_event_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_event_relation` (
  `relation_id` bigint NOT NULL AUTO_INCREMENT,
  `source_event_id` bigint NOT NULL,
  `target_event_id` bigint NOT NULL,
  `relation_type` varchar(32) NOT NULL,
  `confidence` decimal(5,4) NOT NULL DEFAULT '0.0000',
  `explanation` varchar(1000) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`relation_id`),
  UNIQUE KEY `uk_event_relation` (`source_event_id`,`target_event_id`,`relation_type`),
  KEY `idx_relation_source` (`source_event_id`),
  KEY `idx_relation_target` (`target_event_id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跨设备与重复事件关联';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_event_relation`
--

LOCK TABLES `ai_event_relation` WRITE;
/*!40000 ALTER TABLE `ai_event_relation` DISABLE KEYS */;
INSERT INTO `ai_event_relation` VALUES (1,26,27,'CROSS_SOURCE',0.9426,'同一车辆/车牌在时间窗口内被多源感知设备捕获','2026-08-25 11:44:09'),(2,26,28,'REPEAT_BEHAVIOR',0.9425,'同一车辆/车牌在时间窗口内被多源感知设备捕获','2026-08-25 11:44:59'),(3,27,28,'CROSS_SOURCE',0.9390,'同一车辆/车牌在时间窗口内被多源感知设备捕获','2026-08-25 11:44:59'),(4,28,29,'CROSS_SOURCE',0.9426,'同一车辆/车牌在时间窗口内被多源感知设备捕获','2026-08-25 11:45:00'),(5,26,29,'CROSS_SOURCE',0.9390,'同一车辆/车牌在时间窗口内被多源感知设备捕获','2026-08-25 11:45:00'),(6,27,29,'REPEAT_BEHAVIOR',0.9425,'同一车辆/车牌在时间窗口内被多源感知设备捕获','2026-08-25 11:45:00');
/*!40000 ALTER TABLE `ai_event_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_evidence`
--

DROP TABLE IF EXISTS `ai_evidence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_evidence` (
  `evidence_id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL,
  `evidence_type` varchar(24) NOT NULL,
  `evidence_url` varchar(1000) NOT NULL,
  `sha256` varchar(64) DEFAULT NULL,
  `metadata_json` text,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`evidence_id`),
  KEY `idx_ai_evidence_event` (`event_id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI事件证据';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_evidence`
--

LOCK TABLES `ai_evidence` WRITE;
/*!40000 ALTER TABLE `ai_evidence` DISABLE KEYS */;
INSERT INTO `ai_evidence` VALUES (1,1,'IMAGE','/demo/evidence/no_helmet.jpg',NULL,'{}','2026-08-11 09:47:42'),(2,2,'IMAGE','https://example.com/evidence.jpg',NULL,'{}','2026-08-11 10:06:41'),(3,3,'IMAGE','/demo/evidence/no_helmet.jpg',NULL,'{}','2026-08-11 15:49:38'),(7,8,'IMAGE','/uploads/ai-evidence/2026/08/13/6e25b0df828647f78fcd6054d2ab70e8.jpg',NULL,'{}','2026-08-13 08:01:45'),(8,9,'IMAGE','/demo/evidence/theft_risk.jpg',NULL,'{}','2026-08-13 21:35:15'),(9,11,'IMAGE','/demo/evidence/patrol.jpg',NULL,'{}','2026-08-22 10:59:37'),(15,17,'IMAGE','/demo/evidence/patrol.jpg',NULL,'{}','2026-08-22 11:10:18'),(16,18,'IMAGE','/demo/evidence/patrol.jpg',NULL,'{}','2026-08-22 11:17:03');
/*!40000 ALTER TABLE `ai_evidence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_generated_report`
--

DROP TABLE IF EXISTS `ai_generated_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_generated_report` (
  `report_id` bigint NOT NULL AUTO_INCREMENT,
  `report_code` varchar(48) NOT NULL,
  `report_type` varchar(32) NOT NULL,
  `title` varchar(200) NOT NULL,
  `period_start` datetime NOT NULL,
  `period_end` datetime NOT NULL,
  `content_json` longtext NOT NULL,
  `generated_by` int NOT NULL,
  `model_name` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`report_id`),
  UNIQUE KEY `uk_ai_report_code` (`report_code`),
  KEY `idx_ai_report_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI自动治理报告';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_generated_report`
--

LOCK TABLES `ai_generated_report` WRITE;
/*!40000 ALTER TABLE `ai_generated_report` DISABLE KEYS */;
INSERT INTO `ai_generated_report` VALUES (1,'RPT-6707E718155E4711','WEEKLY','校园电动车周期治理报告','2026-08-04 15:24:12','2026-08-11 15:24:12','{\"summary\":{\"events\":2,\"confirmed\":0,\"pendingReview\":1,\"rejected\":1,\"openTasks\":1,\"pendingAppeals\":1,\"crossCameraRelations\":0},\"trend\":[{\"date\":\"2026-08-11\",\"count\":2}],\"hotspots\":[{\"location\":\"东校区正门\",\"count\":1,\"max_risk\":\"MEDIUM\"},{\"location\":\"冒烟测试点\",\"count\":1,\"max_risk\":\"MEDIUM\"}],\"repeatVehicles\":[],\"correlations\":[],\"narrative\":\"# 校园电动车治理报告\\n\\n## 一、结论\\n\\n2026年8月11日共记录电动车相关事件2起，目前**无已确认违规事件**：1起待人工复核、1起已驳回。现有数据不足以形成重复违规车辆或跨摄像头关联判断。当前有1项未完成任务、1项待处理申诉，应优先完成人工核验和管理员审查，**不得直接据此扣分、处罚或冻结车辆**。\\n\\n## 二、关键数据\\n\\n- 事件总数：2起\\n- 已确认：0起\\n- 待人工复核：1起\\n- 已驳回：1起\\n- 未完成任务：1项\\n- 待处理申诉：1项\\n- 重复车辆：0辆\\n- 跨摄像头关联：0条\\n- 事件日期：2026年8月11日，共2起\\n\\n### 点位分布\\n\\n| 点位 | 事件数 | 最高风险 |\\n|---|---:|---|\\n| 东校区正门 | 1起 | 中风险 |\\n| 冒烟测试点 | 1起 | 中风险 |\\n\\n## 三、治理建议\\n\\n1. 对待复核事件核验时间、地点、车辆身份以及图像或轨迹证据。\\n2. 推进1项未完成任务，并由管理员处理1项待审申诉。\\n3. 对东校区正门和冒烟测试点保持关注，但因各仅有1起事件，不宜直接认定为持续性高发区域。\\n4. 当前无重复车辆和跨摄像头关联数据，不建议作重复违规或关联车辆认定。\\n\\n## 四、规则依据\\n\\n- **RULE-01**：驾乘人员应正确佩戴头盔，车牌须保持清晰，不得遮挡或污损。\\n- **RULE-02**：禁止违规载人、超速及违反禁行区域管理要求。\\n- **RULE-03**：车辆应在划定区域停放，不得占用消防通道、疏散通道和建筑出入口。\\n- **RULE-04**：违规事件须有可核验的时间、地点、车辆身份及图像或轨迹证据；证据不足必须转人工复核。\\n- **RULE-05**：正式扣分、处罚、车辆冻结及申诉结论均须由管理员确认，AI仅提供研判建议。\",\"policyReferences\":[{\"document_id\":1,\"title\":\"校园电动车安全治理规则（平台演示版）\",\"version\":\"DEMO-1.0\",\"section_code\":\"RULE-01\",\"heading\":\"头盔与车牌\",\"content\":\"驾乘校园电动车应正确佩戴安全头盔，保持车牌清晰，不得遮挡或污损。\"},{\"document_id\":1,\"title\":\"校园电动车安全治理规则（平台演示版）\",\"version\":\"DEMO-1.0\",\"section_code\":\"RULE-02\",\"heading\":\"道路通行\",\"content\":\"校园道路禁止违规载人、超速行驶；进入禁行区域须遵守电子围栏和现场标识。\"},{\"document_id\":1,\"title\":\"校园电动车安全治理规则（平台演示版）\",\"version\":\"DEMO-1.0\",\"section_code\":\"RULE-03\",\"heading\":\"车辆停放\",\"content\":\"车辆必须在划定区域有序停放，不得占用消防通道、疏散通道和建筑出入口。\"},{\"document_id\":1,\"title\":\"校园电动车安全治理规则（平台演示版）\",\"version\":\"DEMO-1.0\",\"section_code\":\"RULE-04\",\"heading\":\"证据与人工复核\",\"content\":\"违规事件应具有可核验的时间、地点、车辆身份和图像或轨迹证据；证据不足时必须转人工复核。\"},{\"document_id\":1,\"title\":\"校园电动车安全治理规则（平台演示版）\",\"version\":\"DEMO-1.0\",\"section_code\":\"RULE-05\",\"heading\":\"处罚边界\",\"content\":\"正式扣分、处罚、车辆冻结和申诉结论必须由管理员确认，AI仅提供研判建议。\"}]}',1,'gpt-5.6-sol','2026-08-11 15:24:36'),(2,'RPT-A981D6F9D5C44D39','WEEKLY','校园电动车周期治理报告','2026-08-04 15:51:44','2026-08-11 15:51:44','{\"summary\":{\"events\":3,\"confirmed\":0,\"pendingReview\":2,\"rejected\":1,\"openTasks\":2,\"pendingAppeals\":1,\"crossCameraRelations\":0},\"trend\":[{\"date\":\"2026-08-11\",\"count\":3}],\"hotspots\":[{\"location\":\"东校区正门\",\"count\":2,\"max_risk\":\"MEDIUM\"},{\"location\":\"冒烟测试点\",\"count\":1,\"max_risk\":\"MEDIUM\"}],\"repeatVehicles\":[],\"correlations\":[],\"narrative\":\"# 校园电动车治理报告\\n\\n## 一、结论\\n\\n2026年8月11日共记录电动车相关事件3起，风险等级最高为**中风险**。目前**无已确认事件**，其中2起待人工复核、1起已驳回，尚不足以认定存在正式违规或作出处罚。当前应优先完成待复核事件和待处理申诉，并加强东校区正门的巡查与安全提示。\\n\\n## 二、关键数据\\n\\n- 事件总数：3起\\n- 已确认：0起\\n- 待人工复核：2起\\n- 已驳回：1起\\n- 未完成任务：2项\\n- 待处理申诉：1项\\n- 重复车辆：0辆\\n- 跨摄像头关联：0条\\n\\n### 点位分布\\n\\n1. **东校区正门**：2起，最高风险为中风险  \\n2. **冒烟测试点**：1起，最高风险为中风险\\n\\n全部事件集中在2026年8月11日。现有数据未发现重复车辆或跨摄像头关联线索。\\n\\n## 三、治理建议\\n\\n1. 对2起待复核事件核验时间、地点、车辆身份及图像或轨迹证据。\\n2. 优先处理1项待申诉事项，申诉结论由管理员确认。\\n3. 针对东校区正门开展重点巡查，提示规范佩戴头盔、保持车牌清晰并遵守道路通行要求。\\n4. 核实“冒烟测试点”事件的现场性质及证据完整性，避免将测试或证据不足事件误判为违规。\\n5. 在管理员确认前，不进行正式扣分、处罚或车辆冻结。\\n\\n## 四、规则依据\\n\\n- **RULE-01**：驾乘人员应正确佩戴头盔，车牌不得遮挡或污损。\\n- **RULE-02**：禁止违规载人、超速行驶，并应遵守禁行区域标识和电子围栏。\\n- **RULE-03**：车辆须在划定区域停放，不得占用消防通道、疏散通道及建筑出入口。\\n- **RULE-04**：违规事件须具备可核验的时间、地点、车辆身份及图像或轨迹证据；证据不足必须转人工复核。\\n- **RULE-05**：正式扣分、处罚、车辆冻结及申诉结论须由管理员确认，AI仅提供研判建议。\",\"policyReferences\":[{\"document_id\":1,\"title\":\"校园电动车安全治理规则（平台演示版）\",\"version\":\"DEMO-1.0\",\"section_code\":\"RULE-01\",\"heading\":\"头盔与车牌\",\"content\":\"驾乘校园电动车应正确佩戴安全头盔，保持车牌清晰，不得遮挡或污损。\"},{\"document_id\":1,\"title\":\"校园电动车安全治理规则（平台演示版）\",\"version\":\"DEMO-1.0\",\"section_code\":\"RULE-02\",\"heading\":\"道路通行\",\"content\":\"校园道路禁止违规载人、超速行驶；进入禁行区域须遵守电子围栏和现场标识。\"},{\"document_id\":1,\"title\":\"校园电动车安全治理规则（平台演示版）\",\"version\":\"DEMO-1.0\",\"section_code\":\"RULE-03\",\"heading\":\"车辆停放\",\"content\":\"车辆必须在划定区域有序停放，不得占用消防通道、疏散通道和建筑出入口。\"},{\"document_id\":1,\"title\":\"校园电动车安全治理规则（平台演示版）\",\"version\":\"DEMO-1.0\",\"section_code\":\"RULE-04\",\"heading\":\"证据与人工复核\",\"content\":\"违规事件应具有可核验的时间、地点、车辆身份和图像或轨迹证据；证据不足时必须转人工复核。\"},{\"document_id\":1,\"title\":\"校园电动车安全治理规则（平台演示版）\",\"version\":\"DEMO-1.0\",\"section_code\":\"RULE-05\",\"heading\":\"处罚边界\",\"content\":\"正式扣分、处罚、车辆冻结和申诉结论必须由管理员确认，AI仅提供研判建议。\"}]}',1,'gpt-5.6-sol','2026-08-11 15:51:58');
/*!40000 ALTER TABLE `ai_generated_report` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ai_identity_match`
--

DROP TABLE IF EXISTS `ai_identity_match`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_identity_match` (
  `match_id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL,
  `vehicle_id` int DEFAULT NULL,
  `user_id` int DEFAULT NULL,
  `match_method` varchar(32) NOT NULL,
  `confidence` decimal(5,4) NOT NULL DEFAULT '0.0000',
  `match_status` varchar(24) NOT NULL,
  `explanation` varchar(500) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`match_id`),
  KEY `idx_ai_match_event` (`event_id`),
  KEY `idx_ai_match_vehicle` (`vehicle_id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI事件身份匹配';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_identity_match`
--

LOCK TABLES `ai_identity_match` WRITE;
/*!40000 ALTER TABLE `ai_identity_match` DISABLE KEYS */;
INSERT INTO `ai_identity_match` VALUES (1,1,NULL,NULL,'UNMATCHED',0.0000,'UNMATCHED','未找到可靠车辆身份','2026-08-11 09:47:42'),(2,2,NULL,NULL,'UNMATCHED',0.0000,'UNMATCHED','未找到可靠车辆身份','2026-08-11 10:06:41'),(3,3,NULL,NULL,'UNMATCHED',0.0000,'UNMATCHED','未找到可靠车辆身份','2026-08-11 15:49:38'),(8,8,NULL,NULL,'UNMATCHED',0.0000,'UNMATCHED','未找到可靠车辆身份','2026-08-13 08:01:45'),(9,9,NULL,NULL,'UNMATCHED',0.0000,'UNMATCHED','未找到可靠车辆身份','2026-08-13 21:35:15'),(10,11,NULL,NULL,'UNMATCHED',0.0000,'UNMATCHED','未找到可靠车辆身份','2026-08-22 10:59:37'),(16,17,NULL,NULL,'UNMATCHED',0.0000,'UNMATCHED','未找到可靠车辆身份','2026-08-22 11:10:18'),(17,18,NULL,NULL,'UNMATCHED',0.0000,'UNMATCHED','未找到可靠车辆身份','2026-08-22 11:17:03'),(18,19,NULL,NULL,'UNMATCHED',0.0000,'UNMATCHED','未找到可靠车辆身份','2026-08-22 14:09:58'),(19,20,NULL,NULL,'UNMATCHED',0.0000,'UNMATCHED','未找到可靠车辆身份','2026-08-22 14:12:56'),(20,21,NULL,NULL,'UNMATCHED',0.0000,'UNMATCHED','未找到可靠车辆身份','2026-08-22 14:23:00'),(21,22,NULL,NULL,'UNMATCHED',0.0000,'UNMATCHED','未找到可靠车辆身份','2026-08-22 14:44:49'),(22,23,NULL,NULL,'UNMATCHED',0.0000,'UNMATCHED','未找到可靠车辆身份','2026-08-22 14:45:55'),(23,24,NULL,NULL,'UNMATCHED',0.0000,'UNMATCHED','未找到可靠车辆身份','2026-08-22 15:34:06'),(24,25,NULL,NULL,'UNMATCHED',0.0000,'UNMATCHED','未找到可靠车辆身份','2026-08-24 20:47:21'),(25,26,NULL,NULL,'UNMATCHED',0.0000,'UNMATCHED','未找到可靠车辆身份','2026-08-25 11:44:09'),(26,27,NULL,NULL,'UNMATCHED',0.0000,'UNMATCHED','未找到可靠车辆身份','2026-08-25 11:44:09'),(27,28,NULL,NULL,'UNMATCHED',0.0000,'UNMATCHED','未找到可靠车辆身份','2026-08-25 11:44:59'),(28,29,NULL,NULL,'UNMATCHED',0.0000,'UNMATCHED','未找到可靠车辆身份','2026-08-25 11:45:00');
/*!40000 ALTER TABLE `ai_identity_match` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `appeal_operation_log`
--

DROP TABLE IF EXISTS `appeal_operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `appeal_operation_log` (
  `log_id` int NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `appeal_id` int NOT NULL COMMENT '申诉ID',
  `operation_type` varchar(50) NOT NULL COMMENT '操作类型：submit-提交, audit-审核, cancel-撤销, update-更新',
  `operator_id` int DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人姓名',
  `operation_detail` text COMMENT '操作详情',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_appeal_id` (`appeal_id`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `appeal_operation_log_ibfk_1` FOREIGN KEY (`appeal_id`) REFERENCES `illegal_appeal_record` (`appeal_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='申诉操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `appeal_operation_log`
--

LOCK TABLES `appeal_operation_log` WRITE;
/*!40000 ALTER TABLE `appeal_operation_log` DISABLE KEYS */;
INSERT INTO `appeal_operation_log` VALUES (1,2,'submit',30,NULL,'提交申诉',NULL,NULL,'2026-05-03 22:46:38'),(2,2,'audit',1,NULL,'审核申诉，结果：通过',NULL,NULL,'2026-05-03 22:48:01');
/*!40000 ALTER TABLE `appeal_operation_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `camera_monitor`
--

DROP TABLE IF EXISTS `camera_monitor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `camera_monitor` (
  `camera_id` int NOT NULL AUTO_INCREMENT COMMENT '摄像头ID',
  `camera_name` varchar(100) NOT NULL COMMENT '摄像头名称',
  `location` varchar(200) DEFAULT NULL COMMENT '位置',
  `location_lng` decimal(10,6) DEFAULT NULL COMMENT '经度',
  `location_lat` decimal(10,6) DEFAULT NULL COMMENT '纬度',
  `rtsp_url` varchar(255) DEFAULT NULL COMMENT 'RTSP视频流地址',
  `status` varchar(20) DEFAULT 'online' COMMENT '状态: online, offline, malfunction',
  `quality` varchar(20) DEFAULT 'good' COMMENT '画面质量: good, normal, poor',
  `last_heartbeat` datetime DEFAULT NULL COMMENT '最后心跳时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`camera_id`),
  KEY `idx_status` (`status`),
  KEY `idx_location` (`location`(50))
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='摄像头监控表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `camera_monitor`
--

LOCK TABLES `camera_monitor` WRITE;
/*!40000 ALTER TABLE `camera_monitor` DISABLE KEYS */;
INSERT INTO `camera_monitor` VALUES (1,'东校区正门监控','衡阳师范学院东校区正门',112.625000,26.901000,NULL,'online','good','2024-01-25 15:45:30','2026-02-07 21:18:29','2026-02-07 21:18:29'),(2,'教学楼A栋监控','教学楼A栋正门',112.627000,26.903000,NULL,'online','good','2024-01-25 15:44:20','2026-02-07 21:18:29','2026-02-07 21:18:29'),(3,'图书馆监控','图书馆南侧',112.623000,26.899000,NULL,'online','normal','2024-01-25 15:43:10','2026-02-07 21:18:29','2026-02-07 21:18:29'),(4,'学生公寓监控','学生公寓1号楼',112.626000,26.902000,NULL,'offline','poor','2024-01-25 14:30:00','2026-02-07 21:18:29','2026-02-07 21:18:29'),(5,'食堂监控','食堂一楼',112.624000,26.900000,NULL,'online','good','2024-01-25 15:42:00','2026-02-07 21:18:29','2026-02-07 21:18:29'),(6,'体育馆监控','体育馆入口',112.622000,26.898000,NULL,'online','good','2024-01-25 15:41:00','2026-02-07 21:18:29','2026-02-07 21:18:29'),(7,'行政楼监控','行政楼大厅',112.628000,26.904000,NULL,'online','good','2024-01-25 15:40:00','2026-02-07 21:18:29','2026-02-07 21:18:29'),(8,'实验楼监控','实验楼东侧',112.621000,26.897000,NULL,'malfunction','poor','2024-01-25 13:20:00','2026-02-07 21:18:29','2026-02-07 21:18:29');
/*!40000 ALTER TABLE `camera_monitor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `counselor_class`
--

DROP TABLE IF EXISTS `counselor_class`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `counselor_class` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `counselor_user_id` int NOT NULL COMMENT '导员 user.user_id',
  `college` varchar(64) DEFAULT NULL COMMENT '学院（展示分组）',
  `major` varchar(64) DEFAULT NULL COMMENT '专业（展示分组）',
  `class_name` varchar(128) NOT NULL COMMENT '班级名称，与学生 user.class_name 一致',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_counselor_class` (`counselor_user_id`,`class_name`),
  KEY `idx_counselor` (`counselor_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='导员管辖班级（简化版）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `counselor_class`
--

LOCK TABLES `counselor_class` WRITE;
/*!40000 ALTER TABLE `counselor_class` DISABLE KEYS */;
INSERT INTO `counselor_class` VALUES (7,3,'2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术2班','2026-03-23 00:10:25'),(8,3,'2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术3班','2026-03-23 14:37:57'),(9,3,'2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术4班','2026-03-23 14:38:02');
/*!40000 ALTER TABLE `counselor_class` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `decision_log`
--

DROP TABLE IF EXISTS `decision_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `decision_log` (
  `decision_id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL,
  `incident_id` bigint DEFAULT NULL,
  `decision_source` varchar(24) NOT NULL,
  `decision_code` varchar(32) NOT NULL,
  `rule_hits_json` text,
  `explanation` varchar(1000) DEFAULT NULL,
  `operator_user_id` int DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`decision_id`),
  KEY `idx_decision_event` (`event_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI治理决策审计';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `decision_log`
--

LOCK TABLES `decision_log` WRITE;
/*!40000 ALTER TABLE `decision_log` DISABLE KEYS */;
INSERT INTO `decision_log` VALUES (1,1,1,'RULE_ENGINE','PENDING_REVIEW','[\"身份匹配需要人工确认\",\"AI置信度达到自动受理阈值\",\"事件证据完整\"]','事件进入人工复核队列',NULL,'2026-08-11 09:47:42'),(2,2,2,'RULE_ENGINE','PENDING_REVIEW','[\"身份匹配需要人工确认\",\"AI置信度达到复核阈值\",\"事件证据完整\"]','事件进入人工复核队列',NULL,'2026-08-11 10:06:41'),(3,2,2,'ADMIN','REJECTED','[\"管理员驳回\"]','管理员驳回事件',1,'2026-08-11 10:15:33'),(4,3,3,'RULE_ENGINE','PENDING_REVIEW','[\"身份匹配需要人工确认\",\"AI置信度达到自动受理阈值\",\"事件证据需要补充\"]','事件进入人工复核队列',NULL,'2026-08-11 15:49:51'),(6,8,5,'RULE_ENGINE','PENDING_REVIEW','[\"身份匹配需要人工确认\",\"AI置信度达到自动受理阈值\",\"事件证据完整\"]','事件进入人工复核队列',NULL,'2026-08-13 08:02:14'),(7,9,6,'RULE_ENGINE','REJECTED','[\"AI判定事件无效\"]','事件未通过AI有效性校验',NULL,'2026-08-13 21:35:24'),(8,17,8,'RULE_ENGINE','PENDING_REVIEW','[\"身份匹配需要人工确认\",\"AI置信度达到自动受理阈值\",\"事件证据完整\"]','事件进入人工复核队列',NULL,'2026-08-22 11:10:20'),(9,18,9,'RULE_ENGINE','PENDING_REVIEW','[\"身份匹配需要人工确认\",\"AI置信度达到自动受理阈值\",\"事件证据完整\"]','事件进入人工复核队列',NULL,'2026-08-22 11:17:04'),(10,24,10,'RULE_ENGINE','PENDING_REVIEW','[\"身份匹配需要人工确认\",\"AI置信度达到自动受理阈值\",\"事件证据完整\"]','事件进入人工复核队列',NULL,'2026-08-22 15:34:06'),(11,25,11,'RULE_ENGINE','PENDING_REVIEW','[\"身份匹配需要人工确认\",\"AI置信度达到自动受理阈值\",\"事件证据完整\"]','事件进入人工复核队列',NULL,'2026-08-24 20:50:45'),(12,26,12,'RULE_ENGINE','PENDING_REVIEW','[\"身份匹配需要人工确认\",\"AI置信度达到自动受理阈值\",\"事件证据完整\"]','事件进入人工复核队列',NULL,'2026-09-03 14:11:37'),(13,28,13,'RULE_ENGINE','PENDING_REVIEW','[\"身份匹配需要人工确认\",\"AI置信度达到自动受理阈值\",\"事件证据完整\"]','事件进入人工复核队列',NULL,'2026-09-09 20:09:57');
/*!40000 ALTER TABLE `decision_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `drone_evidence`
--

DROP TABLE IF EXISTS `drone_evidence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `drone_evidence` (
  `evidence_id` int NOT NULL AUTO_INCREMENT COMMENT '证据ID',
  `drone_id` varchar(50) NOT NULL COMMENT '无人机ID',
  `evidence_type` varchar(20) NOT NULL COMMENT '证据类型: photo, video',
  `evidence_url` varchar(255) NOT NULL COMMENT '证据URL',
  `location` varchar(100) NOT NULL COMMENT '位置（经纬度）',
  `collect_time` datetime NOT NULL COMMENT '采集时间',
  `description` text COMMENT '描述',
  `status` varchar(20) DEFAULT 'active' COMMENT '状态',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`evidence_id`),
  KEY `idx_drone_id` (`drone_id`),
  KEY `idx_collect_time` (`collect_time`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='无人机取证表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `drone_evidence`
--

LOCK TABLES `drone_evidence` WRITE;
/*!40000 ALTER TABLE `drone_evidence` DISABLE KEYS */;
INSERT INTO `drone_evidence` VALUES (1,'DRONE001','photo','/uploads/evidence/drone_001.jpg','112.625456,26.901234','2024-01-15 14:25:00','东校区正门违规停放','active','2026-02-07 21:18:28'),(2,'DRONE001','video','/uploads/evidence/drone_002.mp4','112.627891,26.903456','2024-01-15 10:15:00','教学楼A栋超速行驶','active','2026-02-07 21:18:28'),(3,'DRONE002','photo','/uploads/evidence/drone_003.jpg','112.626789,26.902345','2024-01-13 18:45:00','学生公寓违规充电','active','2026-02-07 21:18:28');
/*!40000 ALTER TABLE `drone_evidence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `drone_telemetry`
--

DROP TABLE IF EXISTS `drone_telemetry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `drone_telemetry` (
  `telemetry_id` bigint NOT NULL AUTO_INCREMENT,
  `mission_id` bigint NOT NULL,
  `recorded_at` datetime NOT NULL,
  `lng` decimal(11,7) NOT NULL,
  `lat` decimal(10,7) NOT NULL,
  `altitude_m` decimal(7,2) DEFAULT NULL,
  `speed_mps` decimal(7,2) DEFAULT NULL,
  `battery` decimal(5,2) DEFAULT NULL,
  `gps_quality` decimal(5,2) DEFAULT NULL,
  `payload_state` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `raw_payload` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`telemetry_id`),
  KEY `idx_drone_telemetry_mission` (`mission_id`,`recorded_at`)
) ENGINE=InnoDB AUTO_INCREMENT=109 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `drone_telemetry`
--

LOCK TABLES `drone_telemetry` WRITE;
/*!40000 ALTER TABLE `drone_telemetry` DISABLE KEYS */;
INSERT INTO `drone_telemetry` VALUES (1,2,'2026-08-22 10:59:16',112.6849000,26.8893000,30.00,3.00,99.88,1.00,'READY','simulated'),(2,2,'2026-08-22 10:59:17',112.6849000,26.8893000,30.00,3.00,99.76,1.00,'READY','simulated'),(3,5,'2026-08-22 11:17:01',112.6849000,26.8893000,30.00,3.00,99.88,1.00,'READY','simulated'),(4,5,'2026-08-22 11:17:02',112.6849000,26.8893000,30.00,3.00,99.76,1.00,'READY','simulated'),(5,5,'2026-08-22 11:17:02',112.6849000,26.8892650,30.00,3.00,99.64,1.00,'READY','simulated'),(6,5,'2026-08-22 11:17:03',112.6849000,26.8892300,30.00,3.00,99.52,1.00,'READY','simulated'),(7,5,'2026-08-22 11:17:03',112.6849000,26.8891950,30.00,3.00,99.40,1.00,'READY','simulated'),(8,5,'2026-08-22 11:17:04',112.6849000,26.8891600,30.00,3.00,99.28,1.00,'READY','simulated'),(9,3,'2026-08-22 13:40:51',112.6849000,26.8893000,30.00,3.00,99.88,1.00,'READY','simulated'),(10,3,'2026-08-22 13:40:52',112.6849000,26.8893000,30.00,3.00,99.76,1.00,'READY','simulated'),(11,3,'2026-08-22 13:40:52',112.6849000,26.8893000,30.00,3.00,99.76,1.00,'READY','simulated'),(12,7,'2026-08-22 13:51:12',110.3240211,25.2876261,30.00,3.00,99.88,1.00,'READY','simulated'),(13,7,'2026-08-22 13:51:13',110.3240211,25.2876261,30.00,3.00,99.76,1.00,'READY','simulated'),(14,7,'2026-08-22 13:51:13',110.3240211,25.2876261,30.00,3.00,99.76,1.00,'READY','simulated'),(15,8,'2026-08-22 13:55:13',110.3239130,25.2875180,30.00,3.00,99.88,1.00,'READY','simulated'),(16,8,'2026-08-22 13:55:13',110.3239130,25.2875180,30.00,3.00,99.76,1.00,'READY','simulated'),(17,8,'2026-08-22 13:55:14',110.3239130,25.2875180,30.00,3.00,99.76,1.00,'READY','simulated'),(18,12,'2026-08-22 14:10:24',112.6849000,26.8893000,30.00,3.00,99.88,1.00,'READY','simulated'),(19,12,'2026-08-22 14:10:25',112.6849000,26.8893000,30.00,3.00,99.76,1.00,'READY','simulated'),(20,12,'2026-08-22 14:10:25',112.6849000,26.8892650,30.00,3.00,99.64,1.00,'READY','simulated'),(21,12,'2026-08-22 14:10:26',112.6849000,26.8892300,30.00,3.00,99.52,1.00,'READY','simulated'),(22,12,'2026-08-22 14:10:26',112.6849000,26.8891950,30.00,3.00,99.40,1.00,'READY','simulated'),(23,12,'2026-08-22 14:10:27',112.6849000,26.8891600,30.00,3.00,99.28,1.00,'READY','simulated'),(24,14,'2026-08-22 14:44:47',110.3239130,25.2875179,30.00,3.00,99.88,1.00,'READY','simulated'),(25,14,'2026-08-22 14:44:48',110.3239130,25.2875179,30.00,3.00,99.76,1.00,'READY','simulated'),(26,14,'2026-08-22 14:44:48',110.3239130,25.2875179,30.00,3.00,99.76,1.00,'READY','simulated'),(27,14,'2026-08-22 14:44:50',110.3239130,25.2875180,30.00,3.00,88.00,1.00,'READY','simulated'),(28,15,'2026-08-22 14:45:55',110.3239130,25.2875180,30.00,3.00,99.88,1.00,'READY','simulated'),(29,15,'2026-08-22 14:45:56',110.3239130,25.2875180,30.00,3.00,91.00,1.00,'READY','simulated'),(30,15,'2026-08-22 14:45:56',110.3239130,25.2875180,30.00,3.00,99.76,1.00,'READY','simulated'),(31,15,'2026-08-22 14:45:56',110.3239130,25.2875180,30.00,3.00,99.76,1.00,'READY','simulated'),(32,16,'2026-08-22 15:31:19',110.3239130,25.2875179,30.00,3.00,99.88,1.00,'READY','simulated'),(33,16,'2026-08-22 15:31:20',110.3239130,25.2875179,30.00,3.00,99.76,1.00,'READY','simulated'),(34,16,'2026-08-22 15:31:20',110.3239130,25.2875179,30.00,3.00,99.76,1.00,'READY','simulated'),(35,17,'2026-08-22 15:31:55',110.3239130,25.2875179,30.00,3.00,99.88,1.00,'READY','simulated'),(36,17,'2026-08-22 15:31:56',110.3239130,25.2875179,30.00,3.00,99.76,1.00,'READY','simulated'),(37,17,'2026-08-22 15:31:56',110.3239130,25.2875179,30.00,3.00,99.76,1.00,'READY','simulated'),(38,18,'2026-08-22 15:32:30',110.3239130,25.2875179,30.00,3.00,99.88,1.00,'READY','simulated'),(39,18,'2026-08-22 15:32:31',110.3239130,25.2875179,30.00,3.00,99.76,1.00,'READY','simulated'),(40,18,'2026-08-22 15:32:31',110.3239130,25.2875179,30.00,3.00,99.76,1.00,'READY','simulated'),(41,19,'2026-08-22 15:32:59',110.3239130,25.2875179,30.00,3.00,99.88,1.00,'READY','simulated'),(42,19,'2026-08-22 15:33:00',110.3239130,25.2875179,30.00,3.00,99.76,1.00,'READY','simulated'),(43,19,'2026-08-22 15:33:00',110.3239130,25.2875179,30.00,3.00,99.76,1.00,'READY','simulated'),(44,20,'2026-08-22 15:33:36',110.3239130,25.2875179,30.00,3.00,99.88,1.00,'READY','simulated'),(45,20,'2026-08-22 15:33:36',110.3239130,25.2875179,30.00,3.00,99.76,1.00,'READY','simulated'),(46,20,'2026-08-22 15:33:37',110.3239130,25.2875179,30.00,3.00,99.76,1.00,'READY','simulated'),(47,21,'2026-08-22 15:34:06',110.3239130,25.2875179,30.00,3.00,88.00,0.95,'READY','simulated'),(48,21,'2026-08-22 15:34:06',110.3239130,25.2875179,30.00,3.00,99.88,1.00,'READY','simulated'),(49,21,'2026-08-22 15:34:07',110.3239130,25.2875179,30.00,3.00,99.76,1.00,'READY','simulated'),(50,21,'2026-08-22 15:34:08',110.3239130,25.2875179,30.00,3.00,99.76,1.00,'READY','simulated'),(51,24,'2026-09-10 18:59:42',110.3239130,25.2875179,30.00,3.00,99.88,1.00,'READY','simulated'),(52,24,'2026-09-10 18:59:42',110.3239130,25.2875179,30.00,3.00,99.76,1.00,'READY','simulated'),(53,24,'2026-09-10 18:59:43',110.3239130,25.2874829,30.00,3.00,99.64,1.00,'READY','simulated'),(54,24,'2026-09-10 18:59:43',110.3239130,25.2874479,30.00,3.00,99.52,1.00,'READY','simulated'),(55,24,'2026-09-10 18:59:44',110.3239130,25.2874129,30.00,3.00,99.40,1.00,'READY','simulated'),(56,24,'2026-09-10 18:59:44',110.3239130,25.2873779,30.00,3.00,99.28,1.00,'READY','simulated'),(57,24,'2026-09-10 18:59:45',110.3239130,25.2873429,30.00,3.00,99.16,1.00,'READY','simulated'),(58,24,'2026-09-10 18:59:45',110.3239130,25.2873379,30.00,3.00,99.04,1.00,'READY','simulated'),(59,24,'2026-09-10 18:59:46',110.3238780,25.2873379,30.00,3.00,98.92,1.00,'READY','simulated'),(60,24,'2026-09-10 18:59:46',110.3238430,25.2873379,30.00,3.00,98.80,1.00,'READY','simulated'),(61,24,'2026-09-10 18:59:47',110.3238080,25.2873379,30.00,3.00,98.68,1.00,'READY','simulated'),(62,24,'2026-09-10 18:59:47',110.3237730,25.2873379,30.00,3.00,98.56,1.00,'READY','simulated'),(63,24,'2026-09-10 18:59:48',110.3237380,25.2873379,30.00,3.00,98.44,1.00,'READY','simulated'),(64,24,'2026-09-10 18:59:48',110.3237030,25.2873379,30.00,3.00,98.32,1.00,'READY','simulated'),(65,24,'2026-09-10 18:59:49',110.3236930,25.2873379,30.00,3.00,98.20,1.00,'READY','simulated'),(66,24,'2026-09-10 18:59:49',110.3236930,25.2873729,30.00,3.00,98.08,1.00,'READY','simulated'),(67,24,'2026-09-10 18:59:50',110.3236930,25.2874079,30.00,3.00,97.96,1.00,'READY','simulated'),(68,24,'2026-09-10 18:59:50',110.3236930,25.2874429,30.00,3.00,97.84,1.00,'READY','simulated'),(69,24,'2026-09-10 18:59:51',110.3236930,25.2874779,30.00,3.00,97.72,1.00,'READY','simulated'),(70,24,'2026-09-10 18:59:51',110.3236930,25.2875129,30.00,3.00,97.60,1.00,'READY','simulated'),(71,24,'2026-09-10 18:59:52',110.3236930,25.2875179,30.00,3.00,97.48,1.00,'READY','simulated'),(72,24,'2026-09-10 18:59:52',110.3236930,25.2875529,30.00,3.00,97.36,1.00,'READY','simulated'),(73,24,'2026-09-10 18:59:53',110.3236930,25.2875879,30.00,3.00,97.24,1.00,'READY','simulated'),(74,24,'2026-09-10 18:59:53',110.3236930,25.2876229,30.00,3.00,97.12,1.00,'READY','simulated'),(75,24,'2026-09-10 18:59:54',110.3236930,25.2876579,30.00,3.00,97.00,1.00,'READY','simulated'),(76,24,'2026-09-10 18:59:54',110.3236930,25.2876929,30.00,3.00,96.88,1.00,'READY','simulated'),(77,24,'2026-09-10 18:59:55',110.3236930,25.2876979,30.00,3.00,96.76,1.00,'READY','simulated'),(78,24,'2026-09-10 18:59:55',110.3237280,25.2876979,30.00,3.00,96.64,1.00,'READY','simulated'),(79,24,'2026-09-10 18:59:56',110.3237630,25.2876979,30.00,3.00,96.52,1.00,'READY','simulated'),(80,24,'2026-09-10 18:59:56',110.3237980,25.2876979,30.00,3.00,96.40,1.00,'READY','simulated'),(81,24,'2026-09-10 18:59:57',110.3238330,25.2876979,30.00,3.00,96.28,1.00,'READY','simulated'),(82,24,'2026-09-10 18:59:57',110.3238680,25.2876979,30.00,3.00,96.16,1.00,'READY','simulated'),(83,24,'2026-09-10 18:59:58',110.3239030,25.2876979,30.00,3.00,96.04,1.00,'READY','simulated'),(84,24,'2026-09-10 18:59:58',110.3239130,25.2876979,30.00,3.00,95.92,1.00,'READY','simulated'),(85,24,'2026-09-10 18:59:59',110.3239401,25.2876757,30.00,3.00,95.80,1.00,'READY','simulated'),(86,24,'2026-09-10 18:59:59',110.3239672,25.2876536,30.00,3.00,95.68,1.00,'READY','simulated'),(87,24,'2026-09-10 19:00:00',110.3239943,25.2876314,30.00,3.00,95.56,1.00,'READY','simulated'),(88,24,'2026-09-10 19:00:00',110.3240214,25.2876092,30.00,3.00,95.44,1.00,'READY','simulated'),(89,24,'2026-09-10 19:00:01',110.3240484,25.2875871,30.00,3.00,95.32,1.00,'READY','simulated'),(90,24,'2026-09-10 19:00:01',110.3240755,25.2875649,30.00,3.00,95.20,1.00,'READY','simulated'),(91,24,'2026-09-10 19:00:02',110.3241026,25.2875428,30.00,3.00,95.08,1.00,'READY','simulated'),(92,24,'2026-09-10 19:00:02',110.3241297,25.2875206,30.00,3.00,94.96,1.00,'READY','simulated'),(93,24,'2026-09-10 19:00:03',110.3241330,25.2875179,30.00,3.00,94.84,1.00,'READY','simulated'),(94,24,'2026-09-10 19:00:03',110.3241330,25.2874829,30.00,3.00,94.72,1.00,'READY','simulated'),(95,24,'2026-09-10 19:00:04',110.3241330,25.2874479,30.00,3.00,94.60,1.00,'READY','simulated'),(96,24,'2026-09-10 19:00:04',110.3241330,25.2874129,30.00,3.00,94.48,1.00,'READY','simulated'),(97,24,'2026-09-10 19:00:05',110.3241330,25.2873779,30.00,3.00,94.36,1.00,'READY','simulated'),(98,24,'2026-09-10 19:00:05',110.3241330,25.2873429,30.00,3.00,94.24,1.00,'READY','simulated'),(99,24,'2026-09-10 19:00:06',110.3241330,25.2873379,30.00,3.00,94.12,1.00,'READY','simulated'),(100,24,'2026-09-10 19:00:06',110.3241059,25.2873601,30.00,3.00,94.12,1.00,'READY','simulated'),(101,24,'2026-09-10 19:00:07',110.3240788,25.2873822,30.00,3.00,94.12,1.00,'READY','simulated'),(102,24,'2026-09-10 19:00:07',110.3240517,25.2874044,30.00,3.00,94.12,1.00,'READY','simulated'),(103,24,'2026-09-10 19:00:08',110.3240246,25.2874266,30.00,3.00,94.12,1.00,'READY','simulated'),(104,24,'2026-09-10 19:00:08',110.3239976,25.2874487,30.00,3.00,94.12,1.00,'READY','simulated'),(105,24,'2026-09-10 19:00:09',110.3239705,25.2874709,30.00,3.00,94.12,1.00,'READY','simulated'),(106,24,'2026-09-10 19:00:09',110.3239434,25.2874930,30.00,3.00,94.12,1.00,'READY','simulated'),(107,24,'2026-09-10 19:00:10',110.3239163,25.2875152,30.00,3.00,94.12,1.00,'READY','simulated'),(108,24,'2026-09-10 19:00:10',110.3239130,25.2875179,30.00,3.00,94.12,1.00,'READY','simulated');
/*!40000 ALTER TABLE `drone_telemetry` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `eb_rider`
--

DROP TABLE IF EXISTS `eb_rider`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eb_rider` (
  `id` int NOT NULL AUTO_INCREMENT,
  `device` varchar(10) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `identitie` int NOT NULL,
  `lane` tinyint NOT NULL,
  `direction` tinyint NOT NULL,
  `helmet` tinyint NOT NULL,
  `path` varchar(50) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `time` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `eb_rider`
--

LOCK TABLES `eb_rider` WRITE;
/*!40000 ALTER TABLE `eb_rider` DISABLE KEYS */;
/*!40000 ALTER TABLE `eb_rider` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `etc_card`
--

DROP TABLE IF EXISTS `etc_card`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `etc_card` (
  `etc_id` int NOT NULL AUTO_INCREMENT COMMENT 'ETC卡ID',
  `vehicle_id` int NOT NULL COMMENT '车辆ID',
  `etc_number` varchar(50) NOT NULL COMMENT 'ETC卡号',
  `balance` decimal(10,2) DEFAULT '0.00' COMMENT '余额',
  `status` varchar(20) DEFAULT 'normal' COMMENT '状态: normal, lost, expired, disabled',
  `issue_date` date DEFAULT NULL COMMENT '发卡日期',
  `expire_date` date DEFAULT NULL COMMENT '过期日期',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`etc_id`),
  UNIQUE KEY `vehicle_id` (`vehicle_id`),
  UNIQUE KEY `etc_number` (`etc_number`),
  KEY `idx_etc_number` (`etc_number`),
  KEY `idx_status` (`status`),
  CONSTRAINT `etc_card_ibfk_1` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle_info` (`vehicle_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ETC卡表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `etc_card`
--

LOCK TABLES `etc_card` WRITE;
/*!40000 ALTER TABLE `etc_card` DISABLE KEYS */;
INSERT INTO `etc_card` VALUES (1,1,'BLE001',100.00,'normal','2024-01-15','2025-01-15','2026-02-07 21:18:28','2026-05-02 12:02:56'),(2,2,'BLE002',50.00,'normal','2024-01-16','2025-01-16','2026-02-07 21:18:28','2026-05-02 12:02:56'),(3,4,'BLE004',200.00,'lost','2024-01-10','2025-01-10','2026-02-07 21:18:28','2026-05-02 12:02:56'),(4,5,'BLE005',80.00,'normal','2024-01-12','2025-01-12','2026-02-07 21:18:28','2026-05-02 12:02:56'),(5,6,'BLE006',150.00,'normal','2024-01-14','2025-01-14','2026-02-07 21:18:28','2026-05-02 12:02:56'),(6,8,'123456',0.00,'normal','2026-03-23',NULL,'2026-03-23 14:41:55','2026-03-23 14:41:55'),(7,12,'123457',0.00,'normal','2026-03-23',NULL,'2026-03-23 14:42:27','2026-03-23 14:42:27'),(8,11,'123458',0.00,'normal','2026-03-23',NULL,'2026-03-23 14:42:44','2026-03-23 14:42:44'),(19,23,'BLE-HY-0001',58.50,'normal','2026-05-01','2028-05-01','2026-05-01 17:19:38','2026-05-03 15:31:48'),(20,24,'BLE-HY-0002',42.00,'normal','2026-05-01','2028-05-01','2026-05-01 17:19:38','2026-05-03 15:31:48'),(21,25,'BLE-HY-0003',76.20,'normal','2026-05-01','2028-05-01','2026-05-01 17:19:38','2026-05-03 15:31:48'),(22,26,'BLE-HY-0004',31.80,'normal','2026-05-01','2028-05-01','2026-05-01 17:19:38','2026-05-03 15:31:48'),(23,27,'BLE-HY-0005',64.00,'normal','2026-05-01','2028-05-01','2026-05-01 17:19:38','2026-05-03 15:31:48'),(24,28,'BLE-HY-0006',25.50,'normal','2026-05-01','2028-05-01','2026-05-01 17:19:38','2026-05-03 15:31:48'),(25,29,'BLE-HY-0007',88.00,'normal','2026-05-01','2028-05-01','2026-05-01 17:19:38','2026-05-03 15:31:48'),(26,30,'BLE-HY-0008',19.90,'normal','2026-05-01','2028-05-01','2026-05-01 17:19:38','2026-05-03 15:31:48'),(27,31,'BLE-HY-0009',51.60,'normal','2026-05-01','2028-05-01','2026-05-01 17:19:38','2026-05-03 15:31:48'),(28,32,'BLE-HY-0010',37.30,'normal','2026-05-01','2028-05-01','2026-05-01 17:19:38','2026-05-03 15:31:48'),(29,33,'RFID-GUIDE-0001',58.50,'normal','2026-05-03','2028-05-03','2026-05-03 22:38:33','2026-05-03 22:38:33'),(30,34,'RFID-GUIDE-0002',42.00,'normal','2026-05-03','2028-05-03','2026-05-03 22:38:33','2026-05-03 22:38:33'),(31,35,'RFID-GUIDE-0003',76.20,'normal','2026-05-03','2028-05-03','2026-05-03 22:38:33','2026-05-03 22:38:33'),(32,36,'RFID-GUIDE-0004',31.80,'normal','2026-05-03','2028-05-03','2026-05-03 22:38:33','2026-05-03 22:38:33'),(33,37,'RFID-GUIDE-0005',64.00,'normal','2026-05-03','2028-05-03','2026-05-03 22:38:33','2026-05-03 22:38:33'),(34,38,'RFID-GUIDE-0006',25.50,'normal','2026-05-03','2028-05-03','2026-05-03 22:38:33','2026-05-03 22:38:33'),(35,39,'RFID-GUIDE-0007',88.00,'normal','2026-05-03','2028-05-03','2026-05-03 22:38:33','2026-05-03 22:38:33'),(36,40,'RFID-GUIDE-0008',19.90,'normal','2026-05-03','2028-05-03','2026-05-03 22:38:33','2026-05-03 22:38:33'),(37,41,'RFID-GUIDE-0009',51.60,'normal','2026-05-03','2028-05-03','2026-05-03 22:38:33','2026-05-03 22:38:33'),(38,42,'RFID-GUIDE-0010',37.30,'normal','2026-05-03','2028-05-03','2026-05-03 22:38:33','2026-05-03 22:38:33'),(39,43,'BLE-CHAIN-0001',36.50,'normal','2026-07-25','2028-08-14','2026-08-14 08:52:43','2026-08-14 08:52:43');
/*!40000 ALTER TABLE `etc_card` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `event_timeline`
--

DROP TABLE IF EXISTS `event_timeline`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `event_timeline` (
  `timeline_id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL,
  `incident_id` bigint DEFAULT NULL,
  `stage` varchar(32) NOT NULL,
  `title` varchar(128) NOT NULL,
  `detail` varchar(1000) DEFAULT NULL,
  `actor_type` varchar(24) NOT NULL,
  `actor_id` int DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`timeline_id`),
  KEY `idx_timeline_event` (`event_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=72 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI事件处理时间线';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `event_timeline`
--

LOCK TABLES `event_timeline` WRITE;
/*!40000 ALTER TABLE `event_timeline` DISABLE KEYS */;
INSERT INTO `event_timeline` VALUES (1,1,NULL,'INGESTED','事件接入','来源：SIMULATOR','SYSTEM',NULL,'2026-08-11 09:47:42'),(2,1,NULL,'ANALYZING','开始AI研判','模型模式：mock','SYSTEM',NULL,'2026-08-11 09:47:42'),(3,1,1,'DECIDED','规则引擎完成决策','事件进入人工复核队列','SYSTEM',NULL,'2026-08-11 09:47:42'),(4,2,NULL,'INGESTED','事件接入','来源：SIMULATOR','SYSTEM',NULL,'2026-08-11 10:06:41'),(5,2,NULL,'ANALYZING','开始AI研判','模型模式：mock','SYSTEM',NULL,'2026-08-11 10:06:41'),(6,2,2,'DECIDED','规则引擎完成决策','事件进入人工复核队列','SYSTEM',NULL,'2026-08-11 10:06:41'),(7,2,2,'REJECTED','管理员驳回','API权限与审核人落库验证','ADMIN',1,'2026-08-11 10:15:33'),(8,3,NULL,'INGESTED','事件接入','来源：SIMULATOR','SYSTEM',NULL,'2026-08-11 15:49:38'),(9,3,NULL,'ANALYZING','开始AI研判','模型模式：gpt','SYSTEM',NULL,'2026-08-11 15:49:38'),(10,3,3,'DECIDED','规则引擎完成决策','事件进入人工复核队列','SYSTEM',NULL,'2026-08-11 15:49:51'),(20,8,NULL,'INGESTED','事件接入','来源：CAMERA','SYSTEM',NULL,'2026-08-13 08:01:45'),(21,8,NULL,'ANALYZING','开始AI研判','模型模式：gpt','SYSTEM',NULL,'2026-08-13 08:01:45'),(22,8,5,'DECIDED','规则引擎完成决策','事件进入人工复核队列','SYSTEM',NULL,'2026-08-13 08:02:14'),(23,9,NULL,'INGESTED','事件接入','来源：SIMULATOR','SYSTEM',NULL,'2026-08-13 21:35:15'),(24,9,NULL,'ANALYZING','启动AI智能研判','多模态视觉分析引擎已接入','SYSTEM',NULL,'2026-08-13 21:35:15'),(25,9,6,'DECIDED','规则引擎完成决策','事件未通过AI有效性校验','SYSTEM',NULL,'2026-08-13 21:35:24'),(26,11,NULL,'INGESTED','事件接入','来源：DRONE','SYSTEM',NULL,'2026-08-22 10:59:37'),(42,17,NULL,'INGESTED','事件接入','来源：DRONE','SYSTEM',NULL,'2026-08-22 11:10:18'),(43,17,NULL,'ANALYZING','启动AI智能研判','多模态视觉分析引擎已接入','SYSTEM',NULL,'2026-08-22 11:10:18'),(44,17,NULL,'MODEL_FALLBACK','AI智能分析降级','多模态视觉分析服务调用失败，HTTP 401','SYSTEM',NULL,'2026-08-22 11:10:20'),(45,17,8,'DECIDED','规则引擎完成决策','事件进入人工复核队列','SYSTEM',NULL,'2026-08-22 11:10:20'),(46,18,NULL,'INGESTED','事件接入','来源：DRONE','SYSTEM',NULL,'2026-08-22 11:17:03'),(47,18,NULL,'ANALYZING','启动AI智能研判','多模态视觉分析引擎已接入','SYSTEM',NULL,'2026-08-22 11:17:03'),(48,18,NULL,'MODEL_FALLBACK','AI智能分析降级','多模态视觉分析服务调用失败，HTTP 401','SYSTEM',NULL,'2026-08-22 11:17:04'),(49,18,9,'DECIDED','规则引擎完成决策','事件进入人工复核队列','SYSTEM',NULL,'2026-08-22 11:17:04'),(50,19,NULL,'INGESTED','事件接入','来源：GROUND_ROBOT','SYSTEM',NULL,'2026-08-22 14:09:58'),(51,20,NULL,'INGESTED','事件接入','来源：VEHICLE_TERMINAL','SYSTEM',NULL,'2026-08-22 14:12:56'),(52,21,NULL,'INGESTED','事件接入','来源：VEHICLE_TERMINAL','SYSTEM',NULL,'2026-08-22 14:23:00'),(53,22,NULL,'INGESTED','事件接入','来源：DRONE','SYSTEM',NULL,'2026-08-22 14:44:49'),(54,23,NULL,'INGESTED','事件接入','来源：DRONE','SYSTEM',NULL,'2026-08-22 14:45:55'),(55,24,NULL,'INGESTED','事件接入','来源：DRONE','SYSTEM',NULL,'2026-08-22 15:34:06'),(56,24,NULL,'ANALYZING','启动AI智能研判','多模态视觉分析引擎已接入','SYSTEM',NULL,'2026-08-22 15:34:06'),(57,24,NULL,'MODEL_FALLBACK','AI智能分析降级','在线AI能力未配置有效凭证','SYSTEM',NULL,'2026-08-22 15:34:06'),(58,24,10,'DECIDED','规则引擎完成决策','事件进入人工复核队列','SYSTEM',NULL,'2026-08-22 15:34:06'),(59,25,NULL,'INGESTED','事件接入','来源：DRONE','SYSTEM',NULL,'2026-08-24 20:47:21'),(60,25,NULL,'ANALYZING','启动AI智能研判','多模态视觉分析引擎已接入','SYSTEM',NULL,'2026-08-24 20:47:21'),(61,25,NULL,'ANALYSIS_FAILED','AI智能研判失败','AI视觉研判失败: Cannot construct instance of `java.util.ArrayList` (although at least one Creator exists): no String-argument constructor/factory method to deserialize from String value (\'检测置信度0.94，但车牌识别置信度为0.0，车牌信息可能来自其他来源，需人工核实。\')\n at [Source: (String)\"{\n  \"valid\": true,\n  \"eventType\": \"NO_HELMET\",\n  \"confidence\": 0.94,\n  \"riskLevel\": \"MEDIUM\",\n  \"identityReliable\": false,\n  \"evidenceSufficient\": true','SYSTEM',NULL,'2026-08-24 20:47:23'),(62,25,NULL,'ANALYZING','启动AI智能研判','多模态视觉分析引擎已接入','SYSTEM',NULL,'2026-08-24 20:50:41'),(63,25,11,'DECIDED','规则引擎完成决策','事件进入人工复核队列','SYSTEM',NULL,'2026-08-24 20:50:45'),(64,26,NULL,'INGESTED','事件接入','来源：CAMERA','SYSTEM',NULL,'2026-08-25 11:44:09'),(65,27,NULL,'INGESTED','事件接入','来源：VEHICLE_TERMINAL','SYSTEM',NULL,'2026-08-25 11:44:09'),(66,28,NULL,'INGESTED','事件接入','来源：CAMERA','SYSTEM',NULL,'2026-08-25 11:44:59'),(67,29,NULL,'INGESTED','事件接入','来源：VEHICLE_TERMINAL','SYSTEM',NULL,'2026-08-25 11:45:00'),(68,26,NULL,'ANALYZING','启动AI智能研判','多模态视觉分析引擎已接入','SYSTEM',NULL,'2026-09-03 14:11:37'),(69,26,12,'DECIDED','规则引擎完成决策','事件进入人工复核队列','SYSTEM',NULL,'2026-09-03 14:11:37'),(70,28,NULL,'ANALYZING','启动AI智能研判','多模态视觉分析引擎已接入','SYSTEM',NULL,'2026-09-09 20:09:57'),(71,28,13,'DECIDED','规则引擎完成决策','事件进入人工复核队列','SYSTEM',NULL,'2026-09-09 20:09:57');
/*!40000 ALTER TABLE `event_timeline` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `file_info`
--

DROP TABLE IF EXISTS `file_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `file_info` (
  `file_id` int NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `file_name` varchar(255) NOT NULL COMMENT '文件名',
  `original_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_url` varchar(500) NOT NULL COMMENT '文件URL',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `file_type` varchar(50) DEFAULT NULL COMMENT '文件类型',
  `content_type` varchar(100) DEFAULT NULL COMMENT '内容类型',
  `upload_user_id` int DEFAULT NULL COMMENT '上传用户ID',
  `file_category` varchar(20) DEFAULT 'general' COMMENT '文件分类: general, image, document, evidence',
  `related_id` int DEFAULT NULL COMMENT '关联ID（如车辆ID、违规ID等）',
  `related_type` varchar(50) DEFAULT NULL COMMENT '关联类型',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`file_id`),
  KEY `upload_user_id` (`upload_user_id`),
  KEY `idx_file_category` (`file_category`),
  KEY `idx_related` (`related_type`,`related_id`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `file_info_ibfk_1` FOREIGN KEY (`upload_user_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `file_info`
--

LOCK TABLES `file_info` WRITE;
/*!40000 ALTER TABLE `file_info` DISABLE KEYS */;
INSERT INTO `file_info` VALUES (1,'5f8d4e8c8b4e4.jpg','vehicle_photo_1.jpg','/uploads/vehicle/2024/01/15/5f8d4e8c8b4e4.jpg',2457600,NULL,'image/jpeg',2,'image',1,'vehicle','2026-02-07 21:18:29','2026-02-07 21:18:29',0),(2,'7a9b3c2d1e5f6.jpg','violation_evidence_1.jpg','/uploads/evidence/2024/01/15/7a9b3c2d1e5f6.jpg',3189760,NULL,'image/jpeg',1,'evidence',1,'violation','2026-02-07 21:18:29','2026-02-07 21:18:29',0),(3,'3c4d5e6f7a8b9.pdf','regulation_2024.pdf','/uploads/document/2024/01/15/3c4d5e6f7a8b9.pdf',1258291,NULL,'application/pdf',1,'document',NULL,NULL,'2026-02-07 21:18:29','2026-02-07 21:18:29',0),(4,'8b9c0d1e2f3a4.jpg','rectify_photo_1.jpg','/uploads/rectify/2024/01/15/8b9c0d1e2f3a4.jpg',2097152,NULL,'image/jpeg',3,'evidence',1,'rectify','2026-02-07 21:18:29','2026-02-07 21:18:29',0);
/*!40000 ALTER TABLE `file_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `geofence`
--

DROP TABLE IF EXISTS `geofence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `geofence` (
  `fence_id` int NOT NULL AUTO_INCREMENT COMMENT '围栏ID',
  `fence_name` varchar(100) NOT NULL COMMENT '围栏名称',
  `fence_type` tinyint NOT NULL DEFAULT '1' COMMENT '1=禁止进入 2=禁止驶出 3=限速区域 4=监控区域 5=允许停车区',
  `fence_color` varchar(20) DEFAULT '#FF4444' COMMENT '地图显示颜色',
  `coordinates` json NOT NULL COMMENT '边界坐标数组 [{lng,lat},...]',
  `center_lng` double DEFAULT NULL COMMENT '中心点经度（用于标注）',
  `center_lat` double DEFAULT NULL COMMENT '中心点纬度',
  `alert_level` tinyint DEFAULT '2' COMMENT '预警级别：1=提示 2=警告 3=严重',
  `alert_mode` varchar(50) DEFAULT 'notify' COMMENT '预警方式：notify/mqtt/both',
  `description` varchar(500) DEFAULT NULL COMMENT '围栏描述',
  `status` tinyint DEFAULT '1' COMMENT '状态：0=禁用 1=启用',
  `create_user_id` int DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`fence_id`),
  KEY `idx_status` (`status`),
  KEY `idx_fence_type` (`fence_type`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='电子围栏表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `geofence`
--

LOCK TABLES `geofence` WRITE;
/*!40000 ALTER TABLE `geofence` DISABLE KEYS */;
INSERT INTO `geofence` VALUES (1,'主图书馆禁停区',1,'#FF4444','[{\"lat\": 31.2304, \"lng\": 121.4737}, {\"lat\": 31.2304, \"lng\": 121.4752}, {\"lat\": 31.229, \"lng\": 121.4752}, {\"lat\": 31.229, \"lng\": 121.4737}]',121.47444999999999,31.229699999999998,2,'notify','图书馆周边禁止电动车停放',1,NULL,'2026-03-20 11:30:56','2026-03-24 13:27:52',1),(2,'教学楼禁行区',1,'#FF8800','[{\"lat\": 31.2315, \"lng\": 121.472}, {\"lat\": 31.2315, \"lng\": 121.474}, {\"lat\": 31.23, \"lng\": 121.474}, {\"lat\": 31.23, \"lng\": 121.472}]',121.473,31.2307,3,'notify','教学楼正门禁止电动车通行',1,NULL,'2026-03-20 11:30:56','2026-05-01 10:55:18',1),(3,'充电桩监控区',4,'#00BB44','[{\"lat\": 31.232, \"lng\": 121.476}, {\"lat\": 31.232, \"lng\": 121.4775}, {\"lat\": 31.231, \"lng\": 121.4775}, {\"lat\": 31.231, \"lng\": 121.476}]',121.47675000000001,31.231499999999997,1,'notify','充电桩区域实时监控',1,NULL,'2026-03-20 11:30:56','2026-03-24 13:27:55',1),(4,'sky',1,'#00d4ff','[{\"lat\": 31.232, \"lng\": 121.476}, {\"lat\": 31.232, \"lng\": 121.4775}, {\"lat\": 31.231, \"lng\": 121.4775}, {\"lat\": 31.231, \"lng\": 121.476}]',121.47675000000001,31.231499999999997,2,'notify','',1,NULL,'2026-03-24 00:19:53','2026-03-24 13:27:48',1),(6,'彭',1,'#00d4ff','[{\"lat\": 26.887226626967585, \"lng\": 112.68194468932052}, {\"lat\": 26.888037057316588, \"lng\": 112.68188013491356}, {\"lat\": 26.88779103443883, \"lng\": 112.68384904432648}, {\"lat\": 26.88730381106347, \"lng\": 112.68358544716465}, {\"lat\": 26.887226626967585, \"lng\": 112.68194468932052}]',112.68264080100914,26.887517031350814,2,'notify','',1,NULL,'2026-03-24 13:18:45','2026-05-01 10:42:48',1),(7,'理科2楼禁停区',1,'#FFEE00','[{\"lat\": 26.88727619873498, \"lng\": 112.6819405741528}, {\"lat\": 26.88730583061822, \"lng\": 112.68332843350242}, {\"lat\": 26.886404171226022, \"lng\": 112.68334731594256}, {\"lat\": 26.88638723858572, \"lng\": 112.68192641232272}, {\"lat\": 26.886408404385698, \"lng\": 112.68193113293276}, {\"lat\": 26.88727619873498, \"lng\": 112.6819405741528}]',112.68240240716767,26.886843007047602,2,'notify','',1,NULL,'2026-05-01 10:43:57','2026-08-13 17:55:06',1),(8,'电动车充电区',5,'#00FF15','[{\"lat\": 26.889974698628656, \"lng\": 112.6869128111194}, {\"lat\": 26.89000401613035, \"lng\": 112.68760756869028}, {\"lat\": 26.889659534999662, \"lng\": 112.68759939507184}, {\"lat\": 26.889622888008383, \"lng\": 112.68680655407915}, {\"lat\": 26.889974698628656, \"lng\": 112.6869128111194}]',112.68716782801603,26.889847167279147,1,'notify','电动车充电区',1,NULL,'2026-05-01 10:47:05','2026-08-13 17:55:08',1),(9,'停车区',5,'#2FFF00','[{\"lat\": 26.8885071398939, \"lng\": 112.68533320775757}, {\"lat\": 26.88763723148684, \"lng\": 112.68616949502557}, {\"lat\": 26.887522243087425, \"lng\": 112.68596878608123}, {\"lat\": 26.888437147513866, \"lng\": 112.68513249881326}, {\"lat\": 26.888437147513866, \"lng\": 112.68513249881326}, {\"lat\": 26.8885071398939, \"lng\": 112.68533320775757}]',112.68551161570808,26.8881746748983,1,'notify','',1,NULL,'2026-05-01 10:55:08','2026-08-13 17:55:11',1),(10,'禁止进入',1,'#FF0D00','[{\"lat\": 26.88668247025956, \"lng\": 112.68684310465235}, {\"lat\": 26.887180068597512, \"lng\": 112.68614763044984}, {\"lat\": 26.88721987636888, \"lng\": 112.68457911416334}, {\"lat\": 26.884950810778836, \"lng\": 112.6845495195164}, {\"lat\": 26.884950810778836, \"lng\": 112.68658415149184}, {\"lat\": 26.88668247025956, \"lng\": 112.68684310465235}]',112.68592443748769,26.886277751173864,2,'notify','',1,NULL,'2026-05-02 16:21:27','2026-05-03 16:31:37',0),(11,'南门停车场',5,'#6BFF84','[{\"lat\": 26.88495, \"lng\": 112.6815}, {\"lat\": 26.885, \"lng\": 112.68335}, {\"lat\": 26.88588, \"lng\": 112.68325}, {\"lat\": 26.88582, \"lng\": 112.68156}]',112.68241499999999,26.8854125,3,'both','车位数8，当前占用接近满载，用于触发停车引导。',1,NULL,'2026-05-03 22:38:33','2026-05-10 21:10:40',1),(12,'东侧停车场',5,'#00d4ff','[{\"lat\": 26.88618, \"lng\": 112.68315}, {\"lat\": 26.88624, \"lng\": 112.6847}, {\"lat\": 26.88722, \"lng\": 112.68462}, {\"lat\": 26.88712, \"lng\": 112.68322}]',112.683925,26.8867,2,'both','推荐分流区域，适合学生端接收引导信息。',1,NULL,'2026-05-03 22:38:33','2026-05-04 17:46:59',1),(13,'北区停车场',5,'#00ff7f','[{\"lat\": 26.88702, \"lng\": 112.6812}, {\"lat\": 26.88708, \"lng\": 112.68295}, {\"lat\": 26.88786, \"lng\": 112.68285}, {\"lat\": 26.88778, \"lng\": 112.68126}]',112.682075,26.88744,2,'both','备用停车区域，保留少量空位。',1,NULL,'2026-05-03 22:38:33','2026-05-10 21:10:38',1),(14,'快速围栏-6504',1,'#00d4ff','[{\"lat\": 26.884724810782615, \"lng\": 112.68373107361876}, {\"lat\": 26.884724810782615, \"lng\": 112.68533107361876}, {\"lat\": 26.88632481078262, \"lng\": 112.68533107361876}, {\"lat\": 26.88632481078262, \"lng\": 112.68373107361876}]',112.68453107361876,26.88552481078262,2,'both','大屏快速创建',1,1,'2026-05-03 23:41:37','2026-05-04 17:46:26',1),(15,'消防区',1,'#00d4ff','[{\"lat\": 26.889163515131028, \"lng\": 112.68172302542064}, {\"lat\": 26.88920530815747, \"lng\": 112.68360282784067}, {\"lat\": 26.888062959811716, \"lng\": 112.68364943451223}, {\"lat\": 26.88841123798337, \"lng\": 112.68154436651297}, {\"lat\": 26.888383375769564, \"lng\": 112.68152106317716}, {\"lat\": 26.889163515131028, \"lng\": 112.68172302542064}]',112.68229395714737,26.888731651997364,2,'notify','',1,NULL,'2026-06-04 13:10:32','2026-06-04 13:10:32',0);
/*!40000 ALTER TABLE `geofence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `geofence_alert`
--

DROP TABLE IF EXISTS `geofence_alert`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `geofence_alert` (
  `alert_id` int NOT NULL AUTO_INCREMENT COMMENT '预警ID',
  `fence_id` int NOT NULL COMMENT '触发围栏ID',
  `fence_name` varchar(100) DEFAULT NULL COMMENT '围栏名称（冗余）',
  `vehicle_id` int DEFAULT NULL COMMENT '车辆ID',
  `license_plate` varchar(20) DEFAULT NULL COMMENT '车牌号（冗余）',
  `user_id` int DEFAULT NULL COMMENT '车主ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT '车主姓名（冗余）',
  `alert_type` varchar(20) DEFAULT 'enter' COMMENT '类型：enter=入围 exit=出围',
  `trigger_lng` double DEFAULT NULL COMMENT '触发时经度',
  `trigger_lat` double DEFAULT NULL COMMENT '触发时纬度',
  `alert_level` tinyint DEFAULT '2' COMMENT '预警级别',
  `is_processed` tinyint DEFAULT '0' COMMENT '是否已处理：0=未处理 1=已处理',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '触发时间',
  PRIMARY KEY (`alert_id`),
  KEY `idx_fence_id` (`fence_id`),
  KEY `idx_vehicle_id` (`vehicle_id`),
  KEY `idx_create_time` (`create_time` DESC),
  KEY `idx_is_processed` (`is_processed`),
  CONSTRAINT `geofence_alert_ibfk_1` FOREIGN KEY (`fence_id`) REFERENCES `geofence` (`fence_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='围栏预警记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `geofence_alert`
--

LOCK TABLES `geofence_alert` WRITE;
/*!40000 ALTER TABLE `geofence_alert` DISABLE KEYS */;
INSERT INTO `geofence_alert` VALUES (1,9,'停车区',4,'衡阳0533322',4,'王五','enter',112.6852998,26.8882853,1,1,'2026-05-01 14:56:14'),(2,9,'停车区',4,'衡阳0533322',4,'王五','enter',112.6851683,26.8884382,1,1,'2026-05-01 14:58:48');
/*!40000 ALTER TABLE `geofence_alert` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `governance_incident`
--

DROP TABLE IF EXISTS `governance_incident`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `governance_incident` (
  `incident_id` bigint NOT NULL AUTO_INCREMENT,
  `incident_code` varchar(48) NOT NULL,
  `event_id` bigint NOT NULL,
  `vehicle_id` int DEFAULT NULL,
  `user_id` int DEFAULT NULL,
  `event_type` varchar(32) NOT NULL,
  `title` varchar(200) NOT NULL,
  `summary` varchar(1000) DEFAULT NULL,
  `risk_level` varchar(16) NOT NULL,
  `decision_type` varchar(32) NOT NULL,
  `state` varchar(32) NOT NULL,
  `deadline` datetime DEFAULT NULL,
  `violation_id` int DEFAULT NULL,
  `reviewer_id` int DEFAULT NULL,
  `review_remark` varchar(500) DEFAULT NULL,
  `reviewed_at` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`incident_id`),
  UNIQUE KEY `uk_incident_code` (`incident_code`),
  UNIQUE KEY `uk_incident_event` (`event_id`),
  KEY `idx_incident_state_time` (`state`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI治理事件';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `governance_incident`
--

LOCK TABLES `governance_incident` WRITE;
/*!40000 ALTER TABLE `governance_incident` DISABLE KEYS */;
INSERT INTO `governance_incident` VALUES (1,'GI-B07C3D5928C74060',1,NULL,NULL,'NO_HELMET','AI治理：未佩戴头盔','检测到驾驶人员疑似未佩戴安全头盔','MEDIUM','PENDING_REVIEW','PENDING_REVIEW','2026-08-12 09:47:43',NULL,NULL,NULL,NULL,'2026-08-11 09:47:43','2026-08-11 09:47:43'),(2,'GI-75C43CBBE5024EE0',2,NULL,NULL,'ILLEGAL_PARKING','AI治理：违规停车','车辆在非停车区域持续停留','MEDIUM','PENDING_REVIEW','REJECTED','2026-08-12 10:06:41',NULL,1,'API权限与审核人落库验证','2026-08-11 10:15:34','2026-08-11 10:06:41','2026-08-11 10:15:34'),(3,'GI-99CCA52E60F84A77',3,NULL,NULL,'NO_HELMET','AI治理：未佩戴头盔','东校区正门检测到疑似未佩戴头盔骑行行为，检测置信度较高，但数据来自模拟器，未识别车牌，身份置信度为0，无法可靠关联具体人员或车辆。','MEDIUM','PENDING_REVIEW','PENDING_REVIEW','2026-08-12 15:49:51',NULL,NULL,NULL,NULL,'2026-08-11 15:49:51','2026-08-11 15:49:51'),(5,'GI-323DDD2DA6F14C0A',8,NULL,NULL,'NO_HELMET','AI治理：未佩戴头盔','图片中可见一名正在道路上骑行电动车的人员，头部清晰可见且未佩戴安全头盔，未佩戴头盔行为成立。该骑行目标车辆的校园电动车车牌未在图片中清晰显现，无法识别。','HIGH','PENDING_REVIEW','PENDING_REVIEW','2026-08-14 08:02:15',NULL,NULL,NULL,NULL,'2026-08-13 08:02:15','2026-08-13 08:02:15'),(6,'GI-DBB1B07455FB4891',9,NULL,NULL,'THEFT_RISK','AI治理：异常移动','收到模拟器上报的车辆盗窃风险事件，但未提供图片，无法核实是否存在未佩戴头盔行为或读取关联校园电动车车牌。','HIGH','REJECTED','REJECTED','2026-08-14 21:35:25',NULL,NULL,NULL,NULL,'2026-08-13 21:35:25','2026-08-13 21:35:25'),(7,'INC-CHAIN-FIRE-001',10,43,162,'FIRE_DETECTED','三号停车棚明火预警','摄像头3检测到明火，请值班人员立即复核处置。','HIGH','MANUAL_REVIEW','PENDING','2026-08-14 09:22:43',NULL,NULL,NULL,NULL,'2026-08-14 04:52:43','2026-08-14 08:52:43'),(8,'GI-AFB4380542A94727',17,NULL,NULL,'NO_HELMET','AI治理：未佩戴头盔','检测到驾驶人员疑似未佩戴安全头盔','MEDIUM','PENDING_REVIEW','PENDING_REVIEW','2026-08-23 11:10:20',NULL,NULL,NULL,NULL,'2026-08-22 11:10:20','2026-08-22 11:10:20'),(9,'GI-81C038F9F5704FCC',18,NULL,NULL,'NO_HELMET','AI治理：未佩戴头盔','检测到驾驶人员疑似未佩戴安全头盔','MEDIUM','PENDING_REVIEW','PENDING_REVIEW','2026-08-23 11:17:05',NULL,NULL,NULL,NULL,'2026-08-22 11:17:05','2026-08-22 11:17:05'),(10,'GI-9983826AC6C44145',24,NULL,NULL,'NO_HELMET','AI治理：未佩戴头盔','检测到驾驶人员疑似未佩戴安全头盔','MEDIUM','PENDING_REVIEW','PENDING_REVIEW','2026-08-23 15:34:06',NULL,NULL,NULL,NULL,'2026-08-22 15:34:06','2026-08-22 15:34:06'),(11,'GI-8D851E94E7AD48B9',25,NULL,NULL,'NO_HELMET','AI治理：未佩戴头盔','无人机在屏风校区北门及宿舍区巡逻时，捕捉到骑行者未佩戴头盔的行为。画面清晰，但车牌识别置信度较低（0.6），需要人工复核车牌。该行为违反校园交通安全规定，存在一定安全风险。','MEDIUM','PENDING_REVIEW','PENDING_REVIEW','2026-08-25 20:50:46',NULL,NULL,NULL,NULL,'2026-08-24 20:50:46','2026-08-24 20:50:46'),(12,'GI-33578168D0054110',26,NULL,NULL,'NO_HELMET','AI治理：未佩戴头盔','检测到驾驶人员疑似未佩戴安全头盔','MEDIUM','PENDING_REVIEW','PENDING_REVIEW','2026-09-04 14:11:37',NULL,NULL,NULL,NULL,'2026-09-03 14:11:37','2026-09-03 14:11:37'),(13,'GI-5B1EC04F82934450',28,NULL,NULL,'NO_HELMET','AI治理：未佩戴头盔','检测到驾驶人员疑似未佩戴安全头盔','MEDIUM','PENDING_REVIEW','PENDING_REVIEW','2026-09-10 20:09:58',NULL,NULL,NULL,NULL,'2026-09-09 20:09:58','2026-09-09 20:09:58');
/*!40000 ALTER TABLE `governance_incident` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `illegal_appeal_record`
--

DROP TABLE IF EXISTS `illegal_appeal_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `illegal_appeal_record` (
  `appeal_id` int NOT NULL AUTO_INCREMENT COMMENT '申诉ID',
  `illegal_id` int NOT NULL COMMENT '违规ID',
  `user_id` int NOT NULL COMMENT '申诉人ID',
  `appeal_reason` text NOT NULL COMMENT '申诉理由',
  `appeal_evidence` text COMMENT '申诉证据（多张用逗号分隔）',
  `appeal_time` datetime NOT NULL COMMENT '申诉时间',
  `appeal_state` int DEFAULT '0' COMMENT '申诉状态: 0待审核,1审核中,2申诉成功,3申诉失败',
  `audit_user_id` int DEFAULT NULL COMMENT '审核人ID',
  `audit_remark` text COMMENT '审核备注',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `appeal_source` varchar(20) DEFAULT 'user' COMMENT '申诉来源',
  `urgency_level` int DEFAULT '1' COMMENT '紧急程度1-3',
  `appeal_result` int DEFAULT NULL COMMENT '申诉结果1-4',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '拒绝原因',
  `evidence_photos` varchar(500) DEFAULT NULL COMMENT '审核证据照片',
  `audit_opinion` text COMMENT '审核意见',
  PRIMARY KEY (`appeal_id`),
  UNIQUE KEY `illegal_id` (`illegal_id`),
  KEY `user_id` (`user_id`),
  KEY `idx_illegal_id` (`illegal_id`),
  KEY `idx_appeal_state` (`appeal_state`),
  KEY `idx_appeal_state_time` (`appeal_state`,`appeal_time` DESC),
  CONSTRAINT `illegal_appeal_record_ibfk_1` FOREIGN KEY (`illegal_id`) REFERENCES `vehicle_illegal_record` (`illegal_id`) ON DELETE CASCADE,
  CONSTRAINT `illegal_appeal_record_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='违规申诉记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `illegal_appeal_record`
--

LOCK TABLES `illegal_appeal_record` WRITE;
/*!40000 ALTER TABLE `illegal_appeal_record` DISABLE KEYS */;
INSERT INTO `illegal_appeal_record` VALUES (1,4,4,'我在宿舍楼下充电桩充电，这是学校允许的充电位置，并未违规。可能是无人机识别错误。',NULL,'2024-01-13 19:30:00',1,1,'正在审核中',NULL,'2026-02-07 21:18:28','2026-02-07 21:18:28','user',1,NULL,NULL,NULL,NULL),(2,36,30,'我没有违停，我今天下午都没有经过那里',NULL,'2026-05-03 22:46:39',2,1,NULL,'2026-05-03 22:48:02','2026-05-03 22:46:39','2026-05-03 22:46:39','user',1,1,NULL,NULL,'经审核，情况属实，予以通过'),(3,41,162,'对识别结果有疑问，申请查看原始证据。','/demo/appeal/chain-explanation.jpg','2026-08-12 20:52:43',3,NULL,'原始证据能够确认未佩戴头盔，维持原处理。','2026-08-13 14:52:43','2026-08-12 20:52:43','2026-08-14 08:52:43','user',1,2,NULL,NULL,'证据完整，申诉不成立。');
/*!40000 ALTER TABLE `illegal_appeal_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `illegal_rectify_record`
--

DROP TABLE IF EXISTS `illegal_rectify_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `illegal_rectify_record` (
  `rectify_id` int NOT NULL AUTO_INCREMENT COMMENT '整改ID',
  `illegal_id` int NOT NULL COMMENT '违规ID',
  `user_id` int NOT NULL COMMENT '整改人ID',
  `rectify_photo` text COMMENT '整改照片（多张用逗号分隔）',
  `rectify_desc` text COMMENT '整改说明',
  `rectify_time` datetime NOT NULL COMMENT '整改时间',
  `verify_state` int DEFAULT '0' COMMENT '核实状态: 0待核实,1核实中,2核实通过,3核实不通过',
  `verify_user_id` int DEFAULT NULL COMMENT '核实人ID',
  `verify_evidence` varchar(255) DEFAULT NULL COMMENT '核实证据',
  `verify_remark` text COMMENT '核实备注',
  `verify_time` datetime DEFAULT NULL COMMENT '核实时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`rectify_id`),
  UNIQUE KEY `illegal_id` (`illegal_id`),
  KEY `user_id` (`user_id`),
  KEY `idx_illegal_id` (`illegal_id`),
  KEY `idx_verify_state` (`verify_state`),
  CONSTRAINT `illegal_rectify_record_ibfk_1` FOREIGN KEY (`illegal_id`) REFERENCES `vehicle_illegal_record` (`illegal_id`) ON DELETE CASCADE,
  CONSTRAINT `illegal_rectify_record_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='违规整改记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `illegal_rectify_record`
--

LOCK TABLES `illegal_rectify_record` WRITE;
/*!40000 ALTER TABLE `illegal_rectify_record` DISABLE KEYS */;
INSERT INTO `illegal_rectify_record` VALUES (1,2,3,NULL,'已将车辆移至指定停车区域，并拍照确认','2024-01-15 11:30:00',2,1,NULL,'无人机巡检确认已整改','2024-01-15 14:00:00','2026-02-07 21:18:28','2026-02-07 21:18:28'),(2,3,2,NULL,'该车辆为外来车辆，已联系车主驶离校园','2024-01-14 09:15:00',2,1,NULL,'保安确认车辆已驶离','2024-01-14 10:30:00','2026-02-07 21:18:28','2026-02-07 21:18:28'),(3,8,2,NULL,NULL,'2026-02-08 22:55:10',0,NULL,NULL,NULL,NULL,'2026-02-08 22:55:10','2026-02-08 22:55:10'),(4,6,2,NULL,NULL,'2026-02-09 14:56:29',0,NULL,NULL,NULL,NULL,'2026-02-09 14:56:29','2026-02-09 14:56:29'),(5,7,2,NULL,NULL,'2026-02-09 15:46:34',0,NULL,NULL,NULL,NULL,'2026-02-09 15:46:34','2026-02-09 15:46:34'),(6,5,2,NULL,NULL,'2026-02-09 15:46:37',0,NULL,NULL,NULL,NULL,'2026-02-09 15:46:37','2026-02-09 15:46:37'),(7,1,2,NULL,NULL,'2026-02-11 18:47:39',3,1,NULL,'垃圾','2026-03-27 17:34:21','2026-02-11 18:47:39','2026-02-11 18:47:39'),(8,10,8,'/uploads/image/2026/03/22/c2a7d04354844485b5bdc644274c21b3.jpg','学生上传整改佐证','2026-03-22 17:48:35',0,NULL,NULL,NULL,NULL,'2026-03-22 17:48:29','2026-03-22 17:48:29'),(9,41,162,'/demo/rectify/chain-wearing-helmet.jpg','已按要求佩戴安全头盔，并提交整改照片。','2026-08-13 08:52:43',2,NULL,NULL,'整改材料清晰，核实通过。','2026-08-13 20:52:43','2026-08-13 08:52:43','2026-08-14 08:52:43');
/*!40000 ALTER TABLE `illegal_rectify_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `license_point_log`
--

DROP TABLE IF EXISTS `license_point_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `license_point_log` (
  `log_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `delta` int NOT NULL COMMENT '正为加分负为扣分',
  `balance_after` int NOT NULL COMMENT '变更后剩余记分',
  `biz_type` varchar(32) NOT NULL COMMENT 'VIOLATION_DEDUCT/DELETION_RESTORE/ADMIN_BONUS/WELFARE_BONUS',
  `ref_illegal_id` int DEFAULT NULL,
  `ref_welfare_id` int DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `evidence_urls` varchar(2000) DEFAULT NULL,
  `operator_user_id` int DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_user_time` (`user_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='记分流水';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `license_point_log`
--

LOCK TABLES `license_point_log` WRITE;
/*!40000 ALTER TABLE `license_point_log` DISABLE KEYS */;
INSERT INTO `license_point_log` VALUES (1,8,-2,10,'VIOLATION_DEDUCT',11,NULL,'违规记分扣除',NULL,NULL,'2026-03-27 16:57:08'),(2,8,1,11,'ADMIN_BONUS',NULL,NULL,'管理员加分',NULL,NULL,'2026-03-27 16:57:54'),(14,4,-3,9,'VIOLATION_DEDUCT',23,NULL,'违规记分扣除',NULL,NULL,'2026-05-01 14:54:12'),(15,5,-3,9,'VIOLATION_DEDUCT',24,NULL,'违规记分扣除',NULL,NULL,'2026-05-01 14:54:12'),(16,6,-3,9,'VIOLATION_DEDUCT',25,NULL,'违规记分扣除',NULL,NULL,'2026-05-01 14:54:12'),(17,92,-2,10,'VIOLATION_DEDUCT',26,NULL,'demo full linked data: violation -2 points','/demo/evidence/violation-001.jpg',NULL,'2026-04-21 17:19:39'),(18,93,-2,10,'VIOLATION_DEDUCT',27,NULL,'demo full linked data: violation -2 points','/demo/evidence/violation-002.jpg',NULL,'2026-04-22 17:19:39'),(19,94,-2,10,'VIOLATION_DEDUCT',28,NULL,'demo full linked data: violation -2 points','/demo/evidence/violation-003.jpg',NULL,'2026-04-23 17:19:39'),(20,95,-2,10,'VIOLATION_DEDUCT',29,NULL,'demo full linked data: violation -2 points','/demo/evidence/violation-004.jpg',NULL,'2026-04-24 17:19:39'),(21,96,-2,10,'VIOLATION_DEDUCT',30,NULL,'demo full linked data: violation -2 points','/demo/evidence/violation-005.jpg',NULL,'2026-04-25 17:19:39'),(22,97,-2,10,'VIOLATION_DEDUCT',31,NULL,'demo full linked data: violation -2 points','/demo/evidence/violation-006.jpg',NULL,'2026-04-26 17:19:39'),(23,98,-2,10,'VIOLATION_DEDUCT',32,NULL,'demo full linked data: violation -2 points','/demo/evidence/violation-007.jpg',NULL,'2026-04-27 17:19:39'),(24,99,-2,10,'VIOLATION_DEDUCT',33,NULL,'demo full linked data: violation -2 points','/demo/evidence/violation-008.jpg',NULL,'2026-04-28 17:19:39'),(25,100,-2,10,'VIOLATION_DEDUCT',34,NULL,'demo full linked data: violation -2 points','/demo/evidence/violation-009.jpg',NULL,'2026-04-29 17:19:39'),(26,101,-2,10,'VIOLATION_DEDUCT',35,NULL,'demo full linked data: violation -2 points','/demo/evidence/violation-010.jpg',NULL,'2026-04-30 17:19:39'),(27,30,-3,9,'VIOLATION_DEDUCT',36,NULL,'违规记分扣除',NULL,NULL,'2026-05-02 12:13:17'),(28,8,-3,8,'VIOLATION_DEDUCT',37,NULL,'违规记分扣除',NULL,NULL,'2026-05-02 12:13:17'),(29,2,-3,9,'VIOLATION_DEDUCT',38,NULL,'违规记分扣除',NULL,NULL,'2026-05-02 12:13:17'),(30,5,-3,6,'VIOLATION_DEDUCT',39,NULL,'违规记分扣除',NULL,NULL,'2026-05-02 12:13:17'),(31,6,-3,6,'VIOLATION_DEDUCT',40,NULL,'违规记分扣除',NULL,NULL,'2026-05-02 12:13:18'),(32,30,3,12,'APPEAL_PASS_RESTORE',36,NULL,'申诉通过，恢复记分',NULL,1,'2026-05-03 22:48:02'),(33,162,-2,10,'VIOLATION_DEDUCT',41,NULL,'完整链路演示: 未佩戴头盔扣2分','/demo/evidence/chain-no-helmet.jpg',NULL,'2026-08-12 08:52:43');
/*!40000 ALTER TABLE `license_point_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `location_device`
--

DROP TABLE IF EXISTS `location_device`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `location_device` (
  `device_id` int NOT NULL AUTO_INCREMENT COMMENT '设备ID',
  `vehicle_id` int NOT NULL COMMENT '车辆ID',
  `device_number` varchar(50) NOT NULL COMMENT '设备编号',
  `device_type` varchar(20) DEFAULT 'gps' COMMENT '设备类型: gps, bluetooth, rfid',
  `current_lng` decimal(10,6) DEFAULT NULL COMMENT '当前经度',
  `current_lat` decimal(10,6) DEFAULT NULL COMMENT '当前纬度',
  `status` varchar(20) DEFAULT 'online' COMMENT '状态: online, offline, low_battery',
  `battery_level` int DEFAULT '100' COMMENT '电池电量(%)',
  `last_report_time` datetime DEFAULT NULL COMMENT '最后上报时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`device_id`),
  UNIQUE KEY `vehicle_id` (`vehicle_id`),
  UNIQUE KEY `device_number` (`device_number`),
  KEY `idx_device_number` (`device_number`),
  KEY `idx_status` (`status`),
  CONSTRAINT `location_device_ibfk_1` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle_info` (`vehicle_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='位置设备表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `location_device`
--

LOCK TABLES `location_device` WRITE;
/*!40000 ALTER TABLE `location_device` DISABLE KEYS */;
INSERT INTO `location_device` VALUES (1,1,'GPS001','gps',112.625123,26.901234,'online',85,'2024-01-25 15:30:00','2026-02-07 21:18:29','2026-02-07 21:18:29'),(2,2,'GPS002','gps',112.681533,26.881480,'online',92,'2026-03-21 15:25:40','2026-02-07 21:18:29','2026-02-07 21:18:29'),(3,4,'GPS004','gps',112.684763,26.888371,'offline',45,'2026-05-02 12:13:17','2026-02-07 21:18:29','2026-02-07 21:18:29'),(4,5,'GPS005','gps',112.684913,26.889371,'online',78,'2026-05-02 12:13:17','2026-02-07 21:18:29','2026-02-07 21:18:29'),(5,6,'GPS006','gps',112.694863,26.882371,'online',65,'2026-05-02 12:13:18','2026-02-07 21:18:29','2026-02-07 21:18:29'),(6,11,'AABBCCDDEEFK','GPS',112.686313,26.895821,'active',100,'2026-05-02 12:13:17','2026-03-21 15:46:09','2026-03-21 15:46:09'),(7,12,'AABBCCDDEEFL','GPS',112.686763,26.889821,'active',100,'2026-05-02 12:13:17','2026-03-21 17:41:21','2026-03-21 17:41:21'),(18,23,'GPS-HY-0001','GPS',112.682213,26.885821,'active',96,'2026-05-02 12:13:17','2026-05-01 17:19:38','2026-05-03 15:31:48'),(19,24,'GPS-HY-0002','GPS',112.682223,26.885736,'active',91,'2026-05-01 17:20:19','2026-05-01 17:19:38','2026-05-03 15:31:48'),(20,25,'GPS-HY-0003','GPS',112.682403,26.885856,'active',87,'2026-05-01 17:20:19','2026-05-01 17:19:38','2026-05-03 15:31:48'),(21,26,'GPS-HY-0004','GPS',112.682583,26.885976,'active',94,'2026-05-01 17:20:19','2026-05-01 17:19:38','2026-05-03 15:31:48'),(22,27,'GPS-HY-0005','GPS',112.682763,26.886096,'active',83,'2026-05-01 17:20:19','2026-05-01 17:19:38','2026-05-03 15:31:48'),(23,28,'GPS-HY-0006','GPS',112.682943,26.886216,'active',89,'2026-05-01 17:20:19','2026-05-01 17:19:38','2026-05-03 15:31:48'),(24,29,'GPS-HY-0007','GPS',112.683123,26.886336,'active',97,'2026-05-01 17:20:19','2026-05-01 17:19:38','2026-05-03 15:31:48'),(25,30,'GPS-HY-0008','GPS',112.683303,26.886456,'active',78,'2026-05-01 17:20:19','2026-05-01 17:19:38','2026-05-03 15:31:48'),(26,31,'GPS-HY-0009','GPS',112.681663,26.885821,'active',85,'2026-05-02 12:13:17','2026-05-01 17:19:38','2026-05-03 15:31:48'),(27,32,'GPS-HY-0010','GPS',112.687663,26.883371,'active',92,'2026-05-02 12:13:17','2026-05-01 17:19:38','2026-05-03 15:31:48'),(28,33,'GPS-GUIDE-0001','GPS',112.681820,26.885200,'active',96,'2026-05-03 22:37:33','2026-05-03 22:38:33','2026-05-03 22:38:33'),(29,34,'GPS-GUIDE-0002','GPS',112.682100,26.885360,'active',91,'2026-05-03 22:36:33','2026-05-03 22:38:33','2026-05-03 22:38:33'),(30,35,'GPS-GUIDE-0003','GPS',112.682380,26.885520,'active',87,'2026-05-03 22:35:33','2026-05-03 22:38:33','2026-05-03 22:38:33'),(31,36,'GPS-GUIDE-0004','GPS',112.682660,26.885700,'active',94,'2026-05-03 22:37:33','2026-05-03 22:38:33','2026-05-03 22:38:33'),(32,37,'GPS-GUIDE-0005','GPS',112.683020,26.885300,'active',83,'2026-05-03 22:34:33','2026-05-03 22:38:33','2026-05-03 22:38:33'),(33,38,'GPS-GUIDE-0006','GPS',112.683280,26.885620,'active',89,'2026-05-03 22:36:33','2026-05-03 22:38:33','2026-05-03 22:38:33'),(34,39,'GPS-GUIDE-0007','GPS',112.681580,26.885760,'active',97,'2026-05-03 22:37:33','2026-05-03 22:38:33','2026-05-03 22:38:33'),(35,40,'GPS-GUIDE-0008','GPS',112.683620,26.886440,'active',78,'2026-05-03 22:33:33','2026-05-03 22:38:33','2026-05-03 22:38:33'),(36,41,'GPS-GUIDE-0009','GPS',112.684180,26.886900,'active',85,'2026-05-03 22:35:33','2026-05-03 22:38:33','2026-05-03 22:38:33'),(37,42,'GPS-GUIDE-0010','GPS',112.682180,26.887420,'active',92,'2026-05-03 22:36:33','2026-05-03 22:38:33','2026-05-03 22:38:33'),(38,43,'GPS-CHAIN-0001','GPS',113.006211,28.142386,'online',92,'2026-08-14 08:51:43','2026-08-14 08:52:43','2026-08-14 08:52:43');
/*!40000 ALTER TABLE `location_device` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mission_report`
--

DROP TABLE IF EXISTS `mission_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mission_report` (
  `report_id` bigint NOT NULL AUTO_INCREMENT,
  `mission_id` bigint NOT NULL,
  `report_code` varchar(48) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_json` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`report_id`),
  UNIQUE KEY `mission_id` (`mission_id`),
  UNIQUE KEY `report_code` (`report_code`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mission_report`
--

LOCK TABLES `mission_report` WRITE;
/*!40000 ALTER TABLE `mission_report` DISABLE KEYS */;
INSERT INTO `mission_report` VALUES (1,1,'RPT-6A3F30FB8316','{\"missionCode\":\"PM-79BC5BEAF265\",\"goal\":\"验收东校区低空巡检链路\",\"state\":\"PAUSED\",\"waypointCount\":2,\"telemetryCount\":0,\"eventCount\":0,\"batteryStart\":100.00,\"batteryCurrent\":100.00,\"replans\":0,\"generatedAt\":\"2026-08-22T10:57:31.678978\"}','2026-08-22 10:57:32'),(2,2,'RPT-52C17D5DF608','{\"missionCode\":\"PM-B44B382783E9\",\"goal\":\"仿真遥测修复验收\",\"state\":\"PAUSED\",\"waypointCount\":2,\"telemetryCount\":2,\"eventCount\":0,\"batteryStart\":100.00,\"batteryCurrent\":99.76,\"replans\":0,\"generatedAt\":\"2026-08-22T10:59:19.0372988\"}','2026-08-22 10:59:19'),(3,5,'RPT-8CDFF880058A','{\"missionCode\":\"PM-055DDF7239A6\",\"goal\":\"比赛演示：低空巡检东校区头盔与禁行风险\",\"state\":\"PAUSED\",\"waypointCount\":9,\"telemetryCount\":6,\"eventCount\":3,\"batteryStart\":100.00,\"batteryCurrent\":99.28,\"replans\":0,\"generatedAt\":\"2026-08-22T11:53:00.3051867\"}','2026-08-22 11:53:00'),(5,7,'RPT-420A72B37D05','{\"missionCode\":\"PM-C9B5793738A3\",\"goal\":\"应急巡查充电区域烟雾和禁停区，发现风险后复核\",\"state\":\"COMPLETED\",\"waypointCount\":2,\"telemetryCount\":3,\"eventCount\":0,\"batteryStart\":100.00,\"batteryCurrent\":99.76,\"replans\":0,\"planChanges\":[],\"faults\":[{\"fault_type\":\"GPS_OFFSET\",\"parameters_json\":\"{\\\"meters\\\":12}\",\"injected_at\":\"2026-08-22T13:51:12\",\"recovered_at\":null,\"state\":\"INJECTED\"}],\"metrics\":{\"waypointArrivalRate\":1.0,\"telemetryRecords\":3,\"patrolEvents\":0,\"replanCount\":0,\"packetLossFaults\":0,\"completionState\":\"COMPLETED\"},\"generatedAt\":\"2026-08-22T13:51:13.7591913\"}','2026-08-22 13:51:14'),(6,8,'RPT-BC080959B49D','{\"missionCode\":\"PM-12BFEBD608CA\",\"goal\":\"验证结构化巡检计划和故障恢复\",\"state\":\"COMPLETED\",\"waypointCount\":2,\"telemetryCount\":3,\"eventCount\":0,\"batteryStart\":100.00,\"batteryCurrent\":99.76,\"replans\":0,\"planChanges\":[],\"faults\":[{\"fault_type\":\"PACKET_LOSS\",\"parameters_json\":\"{\\\"rate\\\":0.5}\",\"injected_at\":\"2026-08-22T13:55:12\",\"recovered_at\":\"2026-08-22T13:55:12\",\"state\":\"RECOVERED\"}],\"metrics\":{\"waypointArrivalRate\":1.0,\"telemetryRecords\":3,\"patrolEvents\":0,\"replanCount\":0,\"packetLossFaults\":1,\"completionState\":\"COMPLETED\"},\"generatedAt\":\"2026-08-22T13:55:14.5171946\"}','2026-08-22 13:55:15'),(7,12,'RPT-C4D99F99871C','{\"missionCode\":\"PM-9F86B212807C\",\"goal\":\"验证无人机运行与故障恢复\",\"state\":\"PAUSED\",\"waypointCount\":9,\"telemetryCount\":6,\"eventCount\":4,\"batteryStart\":100.00,\"batteryCurrent\":99.28,\"replans\":0,\"planChanges\":[],\"faults\":[{\"fault_type\":\"GPS_OFFSET\",\"parameters_json\":\"{\\\"meters\\\":12}\",\"injected_at\":\"2026-08-22T14:10:27\",\"recovered_at\":\"2026-08-22T14:10:27\",\"state\":\"RECOVERED\"}],\"metrics\":{\"waypointArrivalRate\":0.2222222222222222,\"telemetryRecords\":6,\"patrolEvents\":4,\"replanCount\":0,\"packetLossFaults\":0,\"completionState\":\"PAUSED\"},\"generatedAt\":\"2026-08-22T14:10:38.6900376\"}','2026-08-22 14:10:39'),(9,13,'RPT-64C1EAF05431','{\"missionCode\":\"PM-714152F365FC\",\"goal\":\"审计与报告接口验证\",\"state\":\"PLANNED\",\"waypointCount\":9,\"telemetryCount\":0,\"eventCount\":6,\"batteryStart\":100.00,\"batteryCurrent\":100.00,\"replans\":0,\"planChanges\":[],\"faults\":[],\"metrics\":{\"waypointArrivalRate\":0.0,\"telemetryRecords\":0,\"patrolEvents\":6,\"replanCount\":0,\"packetLossFaults\":0,\"completionState\":\"PLANNED\"},\"generatedAt\":\"2026-08-22T14:27:23.4270705\",\"reportCode\":\"RPT-64C1EAF05431\"}','2026-08-22 14:27:23'),(10,14,'RPT-CADB028963DC','{\"missionCode\":\"PM-6DBDEB4C6594\",\"goal\":\"核查校园电动车高风险区域并回传统一AI事件\",\"state\":\"COMPLETED\",\"waypointCount\":2,\"telemetryCount\":4,\"eventCount\":1,\"batteryStart\":100.00,\"batteryCurrent\":99.76,\"replans\":0,\"planChanges\":[],\"faults\":[],\"steps\":[{\"phase\":\"UNDERSTAND\",\"step_no\":1,\"title\":\"解析巡检目标\",\"detail\":\"区域=桂林理工大学屏风校区;时限=720秒\",\"state\":\"COMPLETED\",\"duration_ms\":0,\"create_time\":\"2026-08-22T14:44:46\"},{\"phase\":\"PLAN\",\"step_no\":2,\"title\":\"生成风险网格与初始航线\",\"detail\":\"读取近期事件、车辆位置和围栏，生成2个航点\",\"state\":\"COMPLETED\",\"duration_ms\":0,\"create_time\":\"2026-08-22T14:44:46\"},{\"phase\":\"EXECUTE\",\"step_no\":3,\"title\":\"启动仿真无人机\",\"detail\":\"开始沿规划航点执行巡检\",\"state\":\"RUNNING\",\"duration_ms\":0,\"create_time\":\"2026-08-22T14:44:47\"},{\"phase\":\"EXECUTE\",\"step_no\":3,\"title\":\"ARRIVED\",\"detail\":\"到达规划航点\",\"state\":\"ARRIVED\",\"duration_ms\":0,\"create_time\":\"2026-08-22T14:44:47\"},{\"phase\":\"EXECUTE\",\"step_no\":3,\"title\":\"ARRIVED\",\"detail\":\"到达规划航点\",\"state\":\"ARRIVED\",\"duration_ms\":0,\"create_time\":\"2026-08-22T14:44:48\"},{\"phase\":\"EXECUTE\",\"step_no\":3,\"title\":\"RETURNING\",\"detail\":\"完成巡检航点，开始返航\",\"state\":\"RETURNING\",\"duration_ms\":0,\"create_time\":\"2026-08-22T14:44:48\"},{\"phase\":\"EXECUTE\",\"step_no\":3,\"title\":\"COMPLETED\",\"detail\":\"无人机已返航，巡检任务完成\",\"state\":\"COMPLETED\",\"duration_ms\":0,\"create_time\":\"2026-08-22T14:44:48\"},{\"phase\":\"OBSERVE\",\"step_no\":4,\"title\":\"接入巡检AI事件\",\"detail\":\"事件 22 已关联到当前巡检任务\",\"state\":\"COMPLETED\",\"duration_ms\":0,\"create_time\":\"2026-08-22T14:44:50\"}],\"audit\":[{\"action\":\"CREATE_MISSION\",\"state\":\"SUCCEEDED\",\"create_time\":\"2026-08-22T14:44:46\"},{\"action\":\"PLAN_MISSION\",\"state\":\"STARTED\",\"create_time\":\"2026-08-22T14:44:46\"},{\"action\":\"START_MISSION\",\"state\":\"SUCCEEDED\",\"create_time\":\"2026-08-22T14:44:47\"},{\"action\":\"INGEST_TELEMETRY\",\"state\":\"SUCCEEDED\",\"create_time\":\"2026-08-22T14:44:50\"},{\"action\":\"ATTACH_EVENT\",\"state\":\"SUCCEEDED\",\"create_time\":\"2026-08-22T14:44:50\"}],\"metrics\":{\"waypointArrivalRate\":1.0,\"telemetryRecords\":4,\"patrolEvents\":1,\"replanCount\":0,\"packetLossFaults\":0,\"completionState\":\"COMPLETED\"},\"generatedAt\":\"2026-08-22T14:44:49.6966589\",\"reportCode\":\"RPT-CADB028963DC\"}','2026-08-22 14:44:50'),(11,15,'RPT-E2124D96BCFB','{\"missionCode\":\"PM-997D69CEEAA6\",\"goal\":\"E2E验收：原大屏巡检统一事件闭环\",\"state\":\"RUNNING\",\"waypointCount\":2,\"telemetryCount\":2,\"eventCount\":2,\"batteryStart\":100.00,\"batteryCurrent\":99.88,\"replans\":0,\"planChanges\":[],\"faults\":[],\"steps\":[{\"phase\":\"UNDERSTAND\",\"step_no\":1,\"title\":\"解析巡检目标\",\"detail\":\"区域=桂林理工大学屏风校区;时限=720秒\",\"state\":\"COMPLETED\",\"duration_ms\":0,\"create_time\":\"2026-08-22T14:45:55\"},{\"phase\":\"PLAN\",\"step_no\":2,\"title\":\"生成风险网格与初始航线\",\"detail\":\"读取近期事件、车辆位置和围栏，生成2个航点\",\"state\":\"COMPLETED\",\"duration_ms\":0,\"create_time\":\"2026-08-22T14:45:55\"},{\"phase\":\"EXECUTE\",\"step_no\":3,\"title\":\"启动仿真无人机\",\"detail\":\"开始沿规划航点执行巡检\",\"state\":\"RUNNING\",\"duration_ms\":0,\"create_time\":\"2026-08-22T14:45:55\"},{\"phase\":\"EXECUTE\",\"step_no\":3,\"title\":\"ARRIVED\",\"detail\":\"到达规划航点\",\"state\":\"ARRIVED\",\"duration_ms\":0,\"create_time\":\"2026-08-22T14:45:55\"},{\"phase\":\"OBSERVE\",\"step_no\":4,\"title\":\"接入巡检AI事件\",\"detail\":\"事件 23 已关联到当前巡检任务\",\"state\":\"COMPLETED\",\"duration_ms\":0,\"create_time\":\"2026-08-22T14:45:56\"}],\"audit\":[{\"action\":\"CREATE_MISSION\",\"state\":\"SUCCEEDED\",\"create_time\":\"2026-08-22T14:45:55\"},{\"action\":\"PLAN_MISSION\",\"state\":\"STARTED\",\"create_time\":\"2026-08-22T14:45:55\"},{\"action\":\"START_MISSION\",\"state\":\"SUCCEEDED\",\"create_time\":\"2026-08-22T14:45:55\"},{\"action\":\"INGEST_TELEMETRY\",\"state\":\"SUCCEEDED\",\"create_time\":\"2026-08-22T14:45:56\"},{\"action\":\"ATTACH_EVENT\",\"state\":\"SUCCEEDED\",\"create_time\":\"2026-08-22T14:45:56\"}],\"metrics\":{\"waypointArrivalRate\":1.0,\"telemetryRecords\":2,\"patrolEvents\":2,\"replanCount\":0,\"packetLossFaults\":0,\"completionState\":\"RUNNING\"},\"generatedAt\":\"2026-08-22T14:45:55.6900241\",\"reportCode\":\"RPT-E2124D96BCFB\"}','2026-08-22 14:45:56'),(12,21,'RPT-3026ACAFCD6B','{\"missionCode\":\"PM-D5779E3E69B7\",\"goal\":\"比赛回归：巡查校园禁停区和高风险车辆\",\"state\":\"RUNNING\",\"waypointCount\":2,\"telemetryCount\":1,\"eventCount\":1,\"batteryStart\":100.00,\"batteryCurrent\":100.00,\"replans\":0,\"planChanges\":[],\"faults\":[],\"steps\":[{\"phase\":\"UNDERSTAND\",\"step_no\":1,\"title\":\"解析巡检目标\",\"detail\":\"区域=屏风校区;时限=720秒\",\"state\":\"COMPLETED\",\"duration_ms\":0,\"create_time\":\"2026-08-22T15:34:06\"},{\"phase\":\"PLAN\",\"step_no\":2,\"title\":\"生成风险网格与初始航线\",\"detail\":\"读取近期事件、车辆位置和围栏，生成2个航点\",\"state\":\"COMPLETED\",\"duration_ms\":0,\"create_time\":\"2026-08-22T15:34:06\"},{\"phase\":\"EXECUTE\",\"step_no\":3,\"title\":\"启动仿真无人机\",\"detail\":\"开始沿规划航点执行巡检\",\"state\":\"RUNNING\",\"duration_ms\":0,\"create_time\":\"2026-08-22T15:34:06\"},{\"phase\":\"OBSERVE\",\"step_no\":4,\"title\":\"接入巡检AI事件\",\"detail\":\"事件 24 已关联到当前巡检任务\",\"state\":\"COMPLETED\",\"duration_ms\":0,\"create_time\":\"2026-08-22T15:34:06\"}],\"audit\":[{\"action\":\"CREATE_MISSION\",\"state\":\"SUCCEEDED\",\"create_time\":\"2026-08-22T15:34:06\"},{\"action\":\"PLAN_MISSION\",\"state\":\"STARTED\",\"create_time\":\"2026-08-22T15:34:06\"},{\"action\":\"START_MISSION\",\"state\":\"SUCCEEDED\",\"create_time\":\"2026-08-22T15:34:06\"},{\"action\":\"INGEST_TELEMETRY\",\"state\":\"SUCCEEDED\",\"create_time\":\"2026-08-22T15:34:06\"},{\"action\":\"ATTACH_EVENT\",\"state\":\"SUCCEEDED\",\"create_time\":\"2026-08-22T15:34:06\"}],\"metrics\":{\"waypointArrivalRate\":0.0,\"telemetryRecords\":1,\"patrolEvents\":1,\"replanCount\":0,\"packetLossFaults\":0,\"completionState\":\"RUNNING\"},\"generatedAt\":\"2026-08-22T15:34:06.0714325\",\"reportCode\":\"RPT-3026ACAFCD6B\"}','2026-08-22 15:34:06');
/*!40000 ALTER TABLE `mission_report` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification`
--

DROP TABLE IF EXISTS `notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification` (
  `notification_id` int NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `title` varchar(100) NOT NULL COMMENT '通知标题',
  `content` text NOT NULL COMMENT '通知内容',
  `type` varchar(20) DEFAULT 'system' COMMENT '通知类型: system, violation, security, activity, personal',
  `is_urgent` tinyint(1) DEFAULT '0' COMMENT '是否紧急',
  `receiver_type` varchar(20) DEFAULT 'all' COMMENT '接收范围: all, student, teacher, service',
  `receiver_ids` text COMMENT '指定接收人ID（逗号分隔）',
  `send_time` datetime DEFAULT NULL COMMENT '发送时间',
  `read_count` int DEFAULT '0' COMMENT '已读人数',
  `total_count` int DEFAULT '0' COMMENT '总接收人数',
  `status` varchar(20) DEFAULT 'draft' COMMENT '状态: draft, sent, failed',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`notification_id`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`),
  KEY `idx_send_time` (`send_time`)
) ENGINE=InnoDB AUTO_INCREMENT=92 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification`
--

LOCK TABLES `notification` WRITE;
/*!40000 ALTER TABLE `notification` DISABLE KEYS */;
INSERT INTO `notification` VALUES (1,'校园电动车管理规定更新通知','为进一步规范校园电动车管理，保障校园交通安全，现对《校园电动车管理规定》进行修订。修订内容包括：1. 调整停车区域划分；2. 明确充电安全规范；3. 新增违规处罚条款。请全体师生认真阅读并遵守相关规定。','system',0,'all',NULL,'2024-01-15 10:30:00',850,1000,'sent','2026-02-07 21:18:28','2026-03-21 09:41:27',1),(2,'违规停放车辆处理通知','您的车辆于2024年1月15日在图书馆前违规停放，请及时处理并前往指定停车区域。如24小时内未整改，将扣除信用分2分。','violation',1,'student',NULL,'2024-01-15 14:20:00',1,1,'sent','2026-02-07 21:18:28','2026-02-08 23:00:58',1),(3,'冬季电动车充电安全提醒','冬季气温较低，电动车充电时请注意安全：1. 避免在室内充电；2. 使用原装充电器；3. 充电时间不宜过长；4. 发现异常立即断电。祝大家安全过冬！','security',0,'all',NULL,'2024-01-14 09:00:00',750,1000,'sent','2026-02-07 21:18:28','2026-03-21 09:41:27',1),(4,'校园电动车年检通知','请所有已注册电动车的师生于2024年1月20日前完成车辆年检。年检内容包括：车辆安全状况、电池性能、车牌清晰度等。逾期未检车辆将被限制进出校园。','system',1,'all',NULL,'2024-01-13 15:30:00',920,1000,'sent','2026-02-07 21:18:28','2026-02-07 21:18:28',0),(5,'校园安全月活动通知','为进一步增强师生安全意识，学校将于2024年1月20日至2月20日开展校园安全月活动。活动期间将组织安全知识讲座、应急演练等，欢迎广大师生积极参与。','activity',0,'all',NULL,'2024-01-12 11:00:00',680,1000,'sent','2026-02-07 21:18:28','2026-02-09 13:44:52',1),(6,'你好','你违规尼玛呢','violation',0,'student',NULL,'2026-02-09 13:45:20',0,0,'sent','2026-02-09 13:45:20','2026-03-21 09:41:27',1),(7,'cd','cdc','system',1,'student',NULL,'2026-02-09 13:45:48',0,0,'sent','2026-02-09 13:45:48','2026-03-21 09:41:27',1),(8,'违规行为提醒','您的车辆湘D·A12345存在违规行为（违规载人），请及时处理。','personal',0,'all','2','2026-02-09 14:11:11',0,1,'sent','2026-02-09 14:11:11','2026-03-21 09:41:27',1),(9,'违规行为提醒','您的车辆湘D·B67890存在违规行为（遮挡车牌），请及时处理。','personal',0,'all','3','2026-02-09 14:16:30',0,1,'sent','2026-02-09 14:16:30','2026-02-09 14:36:40',1),(10,'违规行为提醒','您的车辆湘D·E77889存在违规行为（违规停放），请及时处理。','personal',0,'all','5','2026-02-09 15:46:17',0,1,'sent','2026-02-09 15:46:17','2026-03-21 09:41:27',1),(11,'违规行为提醒','您的车辆湘D·A12345存在违规行为（违规停放），请及时处理。','personal',0,'all','2','2026-02-09 15:46:39',0,1,'sent','2026-02-09 15:46:39','2026-03-21 09:41:27',1),(12,'违规行为提醒','您的车辆湘D·B67890存在违规行为（违规停放），请及时处理。','personal',0,'all','3','2026-02-14 14:09:58',0,1,'sent','2026-02-14 14:09:58','2026-03-21 09:41:27',1),(13,'违规行为提醒','您的车辆湘D·B67890存在违规行为（遮挡车牌），请及时处理。','personal',0,'all','3','2026-02-21 15:15:48',0,1,'sent','2026-02-21 15:15:48','2026-02-21 15:18:21',1),(14,'车辆整改提醒','您的车辆湘A·12345有违规记录待处理，请及时整改。','personal',0,'all','8','2026-03-18 13:16:26',0,1,'sent','2026-03-18 13:16:26','2026-03-21 09:41:27',1),(15,'整改催办提醒','您的车辆湘A·12345违规整改请尽快处理！','personal',0,'all','8','2026-03-18 23:37:52',0,1,'sent','2026-03-18 23:37:52','2026-03-21 09:41:27',1),(16,'违规行为提醒','您的车辆湘A·12345存在违规行为（违规停放），请及时处理。','personal',0,'all','8','2026-03-22 19:32:43',0,1,'sent','2026-03-22 19:32:43','2026-03-22 19:32:43',0),(17,'违规行为提醒','您的车辆湘A·12345存在违规行为（违规停放），请及时处理。','personal',0,'all','8','2026-03-22 19:34:06',0,1,'sent','2026-03-22 19:34:06','2026-03-22 19:34:06',0),(18,'违规行为提醒','您的车辆湘A·12345存在违规行为（违规停放），请及时处理。','personal',0,'all','8','2026-03-22 19:34:08',0,1,'sent','2026-03-22 19:34:08','2026-03-22 19:34:08',0),(19,'违规行为提醒','您的车辆湘A·12345存在违规行为（违规停放），请及时处理。','personal',0,'all','8','2026-03-22 19:34:15',0,1,'sent','2026-03-22 19:34:15','2026-03-22 19:34:15',0),(20,'违规行为提醒','您的车辆湘A·12345存在违规行为（违规停放），请及时处理。','personal',0,'all','8','2026-03-22 19:34:17',0,1,'sent','2026-03-22 19:34:17','2026-03-22 19:34:17',0),(21,'违规行为提醒','您的车辆湘A·12345存在违规行为（违规停放），请及时处理。','personal',0,'specified','8','2026-03-22 23:04:22',0,1,'sent','2026-03-22 23:04:22','2026-03-22 23:04:22',0),(22,'违规行为提醒','您的车辆湘A·12345存在违规行为（违规停放），请及时处理。','personal',0,'specified','8','2026-03-22 23:05:01',0,1,'sent','2026-03-22 23:05:01','2026-03-22 23:05:01',0),(23,'违规行为提醒','您的车辆湘A·12345存在违规行为（违规停放），请及时处理。','personal',0,'specified','8','2026-03-23 00:09:05',0,1,'sent','2026-03-23 00:09:05','2026-03-23 00:09:05',0),(24,'违规行为提醒','您的车辆湘A·12345存在违规行为（违规停放），请及时处理。','personal',0,'specified','8','2026-03-23 00:09:33',0,1,'sent','2026-03-23 00:09:33','2026-03-23 00:09:33',0),(25,'整改催办提醒','您的车辆湘A·12345违规整改请尽快处理！','personal',0,'specified','8','2026-03-24 21:14:19',0,1,'sent','2026-03-24 21:14:19','2026-03-24 21:14:19',0),(26,'车辆整改提醒','您的车辆湘K·12345有违规记录待处理，请及时整改。','personal',0,'specified','30','2026-03-27 17:32:35',0,1,'sent','2026-03-27 17:32:35','2026-03-27 17:32:35',0),(27,'车辆整改提醒','您的车辆湘A·12345有违规记录待处理，请及时整改。','personal',0,'specified','8','2026-04-28 21:43:50',0,1,'sent','2026-04-28 21:43:50','2026-04-28 21:43:50',0),(28,'车辆整改提醒','您的车辆湘A·E0001有违规记录待处理，请及时整改。','personal',0,'specified','82','2026-05-01 13:28:07',0,1,'sent','2026-05-01 13:28:07','2026-05-01 13:28:07',0),(29,'车辆整改提醒','您的车辆湘A·E0001有违规记录待处理，请及时整改。','personal',0,'specified','82','2026-05-01 15:53:21',0,1,'sent','2026-05-01 15:53:21','2026-05-01 15:53:21',0),(30,'车辆整改提醒','您的车辆湘A·E0003有违规记录待处理，请及时整改。','personal',0,'specified','84','2026-05-01 15:53:23',0,1,'sent','2026-05-01 15:53:23','2026-05-01 15:53:23',0),(31,'车辆整改提醒','您的车辆湘A·E0005有违规记录待处理，请及时整改。','personal',0,'specified','86','2026-05-01 15:53:25',0,1,'sent','2026-05-01 15:53:25','2026-05-01 15:53:25',0),(32,'车辆整改提醒','您的车辆湘A·E0008有违规记录待处理，请及时整改。','personal',0,'specified','89','2026-05-01 15:53:27',0,1,'sent','2026-05-01 15:53:27','2026-05-01 15:53:27',0),(33,'违规行为提醒','您的车辆湘A0533315存在违规行为（违规停放），请及时处理。','personal',0,'specified','8','2026-05-03 16:20:47',0,1,'sent','2026-05-03 16:20:47','2026-05-03 16:20:47',0),(34,'违规行为提醒','您的车辆湘A0533326存在违规行为（违规停放），请及时处理。','personal',0,'specified','30','2026-05-03 16:34:05',0,1,'sent','2026-05-03 16:34:05','2026-05-03 16:34:05',0),(35,'停车引导','附近区域车位已满，推荐前往 东侧停车场。当前车辆：湘A90001、湘A90002、湘A90003、湘A90004、湘A90005、湘A90006、湘A90007。','guide',1,'specified','152,153,154,155,156,157,158,159,160,161','2026-05-03 22:35:33',0,10,'sent','2026-05-03 22:38:33','2026-05-03 22:38:33',0),(36,'车辆历史轨迹回放','车辆 湘A90008 已进入东侧停车场，可在数据大屏查看历史轨迹回放。','location',0,'specified','159,160,161','2026-05-03 22:37:33',0,3,'sent','2026-05-03 22:38:33','2026-05-03 22:38:33',0),(37,'新的违规申诉待处理','用户 董吉林 对违规记录（车牌：未知车牌，时间：2026-05-02 12:13:17）提交了申诉，请及时处理。','batch',0,'specified','1','2026-05-03 22:46:39',0,1,'sent','2026-05-03 22:46:39','2026-05-03 22:46:39',0),(38,'申诉处理结果：通过','您的申诉（ID：2）已处理完成。\n处理结果：通过\n处理意见：经审核，情况属实，予以通过\n处理时间：2026-05-03 22:48:01\n','batch',0,'specified','30','2026-05-03 22:48:02',0,1,'sent','2026-05-03 22:48:02','2026-05-03 22:48:02',0),(39,'停车引导','附近区域车位已满，推荐前往东侧停车场。车牌湘A0533305，请按现场指引规范停放。','guide',1,'specified','92','2026-05-04 23:32:23',0,1,'sent','2026-05-04 23:32:23','2026-05-04 23:32:23',0),(40,'停车引导','附近区域车位已满，推荐前往 北区停车场。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 11:47:53',0,0,'sent','2026-05-05 11:47:53','2026-05-05 11:47:53',0),(41,'停车引导','附近区域车位已满，推荐前往 北区停车场。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 11:57:56',0,0,'sent','2026-05-05 11:57:56','2026-05-05 11:57:56',0),(42,'停车引导','附近区域车位已满，推荐前往 北区停车场。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 12:32:56',0,0,'sent','2026-05-05 12:32:56','2026-05-05 12:32:56',0),(43,'停车引导','附近区域车位已满，推荐前往 北区停车场。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 19:27:19',0,0,'sent','2026-05-05 19:27:19','2026-05-05 19:27:19',0),(44,'停车引导','附近区域车位已满，推荐前往 北区停车场。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 19:37:24',0,0,'sent','2026-05-05 19:37:24','2026-05-05 19:37:24',0),(45,'停车引导','附近区域车位已满，推荐前往 北区停车场。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 19:47:26',0,0,'sent','2026-05-05 19:47:26','2026-05-05 19:47:26',0),(46,'停车引导','附近区域车位已满，推荐前往 北区停车场。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 19:57:27',0,0,'sent','2026-05-05 19:57:27','2026-05-05 19:57:27',0),(47,'停车引导','附近区域车位已满，推荐前往 北区停车场。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 20:07:29',0,0,'sent','2026-05-05 20:07:29','2026-05-05 20:07:29',0),(48,'停车引导','附近区域车位已满，推荐前往 北区停车场。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 20:17:34',0,0,'sent','2026-05-05 20:17:34','2026-05-05 20:17:34',0),(49,'停车引导','附近区域车位已满，推荐前往 北区停车场。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 20:27:36',0,0,'sent','2026-05-05 20:27:36','2026-05-05 20:27:36',0),(50,'停车引导','附近区域车位已满，推荐前往 北区停车场。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 20:37:37',0,0,'sent','2026-05-05 20:37:37','2026-08-13 14:54:38',1),(51,'停车引导','附近区域车位已满，推荐前往 北区停车场。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 20:47:38',0,0,'sent','2026-05-05 20:47:38','2026-08-13 14:54:38',1),(52,'停车引导','附近区域车位已满，推荐前往 北区停车场。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 20:57:40',0,0,'sent','2026-05-05 20:57:40','2026-08-13 14:54:38',1),(53,'停车引导','附近区域车位已满，推荐前往 北区停车场。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 21:07:44',0,0,'sent','2026-05-05 21:07:44','2026-08-13 14:54:38',1),(54,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 21:15:36',0,0,'sent','2026-05-05 21:15:36','2026-08-13 14:54:38',1),(55,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 21:25:38',0,0,'sent','2026-05-05 21:25:38','2026-08-13 14:54:38',1),(56,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 22:04:32',0,0,'sent','2026-05-05 22:04:32','2026-08-13 14:54:38',1),(57,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 22:14:34',0,0,'sent','2026-05-05 22:14:34','2026-08-13 14:54:38',1),(58,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 22:24:35',0,0,'sent','2026-05-05 22:24:35','2026-08-13 14:54:38',1),(59,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 22:34:37',0,0,'sent','2026-05-05 22:34:37','2026-08-13 14:54:32',1),(60,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 22:44:40',0,0,'sent','2026-05-05 22:44:40','2026-05-05 22:44:40',0),(61,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 22:54:42',0,0,'sent','2026-05-05 22:54:42','2026-05-05 22:54:42',0),(62,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 23:04:46',0,0,'sent','2026-05-05 23:04:46','2026-05-05 23:04:46',0),(63,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 8/8，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 23:09:28',0,0,'sent','2026-05-05 23:09:28','2026-05-05 23:09:28',0),(64,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 23:14:51',0,0,'sent','2026-05-05 23:14:51','2026-05-05 23:14:51',0),(65,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 23:24:55',0,0,'sent','2026-05-05 23:24:55','2026-05-05 23:24:55',0),(66,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 23:34:57',0,0,'sent','2026-05-05 23:34:57','2026-05-05 23:34:57',0),(67,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 23:44:58',0,0,'sent','2026-05-05 23:44:58','2026-05-05 23:44:58',0),(68,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-05 23:54:59',0,0,'sent','2026-05-05 23:54:59','2026-05-05 23:54:59',0),(69,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-06 00:05:01',0,0,'sent','2026-05-06 00:05:01','2026-05-06 00:05:01',0),(70,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-06 00:15:03',0,0,'sent','2026-05-06 00:15:03','2026-05-06 00:15:03',0),(71,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-06 00:25:05',0,0,'sent','2026-05-06 00:25:05','2026-05-06 00:25:05',0),(72,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-06 00:35:07',0,0,'sent','2026-05-06 00:35:07','2026-05-06 00:35:07',0),(73,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-06 00:45:08',0,0,'sent','2026-05-06 00:45:08','2026-05-06 00:45:08',0),(74,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-06 00:55:11',0,0,'sent','2026-05-06 00:55:11','2026-05-06 00:55:11',0),(75,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-06 01:29:35',0,0,'sent','2026-05-06 01:29:35','2026-05-06 01:29:35',0),(76,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-06 14:31:19',0,0,'sent','2026-05-06 14:31:19','2026-05-06 14:31:19',0),(77,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-06 14:41:20',0,0,'sent','2026-05-06 14:41:20','2026-05-06 14:41:20',0),(78,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-06 14:51:23',0,0,'sent','2026-05-06 14:51:23','2026-05-06 14:51:23',0),(79,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-06 15:01:24',0,0,'sent','2026-05-06 15:01:24','2026-05-06 15:01:24',0),(80,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-06 15:11:26',0,0,'sent','2026-05-06 15:11:26','2026-05-06 15:11:26',0),(81,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-06 15:21:28',0,0,'sent','2026-05-06 15:21:28','2026-08-13 14:53:58',1),(82,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-06 15:31:30',0,0,'sent','2026-05-06 15:31:30','2026-05-06 15:31:30',0),(83,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-06 15:41:32',0,0,'sent','2026-05-06 15:41:32','2026-05-06 15:41:32',0),(84,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-06 15:51:33',0,0,'sent','2026-05-06 15:51:33','2026-05-06 15:51:33',0),(85,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-06 16:01:37',0,0,'sent','2026-05-06 16:01:37','2026-05-06 16:01:37',0),(86,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-06 16:11:37',0,0,'sent','2026-05-06 16:11:37','2026-05-06 16:11:37',0),(87,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-06 16:21:40',0,0,'sent','2026-05-06 16:21:40','2026-05-06 16:21:40',0),(88,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-06 16:31:41',0,0,'sent','2026-05-06 16:31:41','2026-05-06 16:31:41',0),(89,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-06 16:41:43',0,0,'sent','2026-05-06 16:41:43','2026-05-06 16:41:43',0),(90,'停车引导','附近区域车位已满，推荐前往 停车区。南门停车场 当前占用 2/2，请按现场指引规范停放。','guide',1,'all',NULL,'2026-05-10 21:10:06',0,0,'sent','2026-05-10 21:10:06','2026-05-10 21:10:06',0),(91,'模拟用户违规闭环通知','车辆湘A0599001存在未佩戴头盔记录，整改已核实通过，申诉审核已完成。','violation',1,'student','162','2026-08-12 08:52:43',0,1,'sent','2026-08-12 08:52:43','2026-08-14 08:52:43',0);
/*!40000 ALTER TABLE `notification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification_user_inbox`
--

DROP TABLE IF EXISTS `notification_user_inbox`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_user_inbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `notification_id` int NOT NULL,
  `hidden` tinyint(1) NOT NULL DEFAULT '0' COMMENT '1=用户已从列表移除',
  `read_at` datetime DEFAULT NULL COMMENT '用户已读时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_notification` (`user_id`,`notification_id`),
  KEY `idx_user_hidden` (`user_id`,`hidden`)
) ENGINE=InnoDB AUTO_INCREMENT=47 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知用户维度的已读/隐藏';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification_user_inbox`
--

LOCK TABLES `notification_user_inbox` WRITE;
/*!40000 ALTER TABLE `notification_user_inbox` DISABLE KEYS */;
INSERT INTO `notification_user_inbox` VALUES (1,8,22,1,NULL,'2026-03-22 23:25:43','2026-03-22 23:25:43'),(2,8,20,1,NULL,'2026-03-22 23:25:46','2026-03-22 23:25:46'),(3,8,19,1,NULL,'2026-03-22 23:25:47','2026-03-22 23:25:47'),(4,8,18,1,NULL,'2026-03-22 23:25:54','2026-03-22 23:25:54'),(5,8,21,1,NULL,'2026-03-22 23:35:30','2026-03-22 23:35:30'),(6,81,20,1,NULL,'2026-04-28 21:43:15','2026-04-28 21:43:15'),(7,81,19,1,NULL,'2026-04-28 21:43:16','2026-04-28 21:43:16'),(8,81,18,1,NULL,'2026-04-28 21:43:17','2026-04-28 21:43:17'),(9,81,17,1,NULL,'2026-04-28 21:43:18','2026-04-28 21:43:18'),(10,81,16,1,NULL,'2026-04-28 21:43:20','2026-04-28 21:43:20'),(11,81,4,1,NULL,'2026-04-28 21:43:27','2026-04-28 21:43:27'),(12,92,18,1,'2026-05-01 17:54:14','2026-05-01 17:54:14','2026-05-01 17:54:14'),(13,92,20,1,'2026-05-02 12:23:31','2026-05-02 12:23:31','2026-05-02 12:23:31'),(14,92,19,1,'2026-05-02 12:23:36','2026-05-02 12:23:36','2026-05-02 12:23:36'),(15,92,17,1,NULL,'2026-05-03 16:20:11','2026-05-03 16:20:11'),(16,92,4,1,NULL,'2026-05-03 16:20:13','2026-05-03 16:20:13'),(17,92,16,1,NULL,'2026-05-03 16:20:14','2026-05-03 16:20:14'),(18,30,26,1,NULL,'2026-05-03 16:33:19','2026-05-03 16:33:19'),(19,30,20,1,NULL,'2026-05-03 16:33:20','2026-05-03 16:33:20'),(20,30,19,1,NULL,'2026-05-03 16:33:21','2026-05-03 16:33:21'),(21,30,18,1,NULL,'2026-05-03 16:33:22','2026-05-03 16:33:22'),(22,30,17,1,NULL,'2026-05-03 16:33:23','2026-05-03 16:33:23'),(23,30,16,1,NULL,'2026-05-03 16:33:25','2026-05-03 16:33:25'),(24,30,4,1,NULL,'2026-05-03 16:33:26','2026-05-03 16:33:26'),(25,30,34,0,'2026-05-03 22:45:02','2026-05-03 16:34:13','2026-05-03 16:34:13'),(26,152,35,0,NULL,'2026-05-03 22:38:33','2026-05-03 22:38:33'),(27,153,35,0,NULL,'2026-05-03 22:38:33','2026-05-03 22:38:33'),(28,154,35,0,NULL,'2026-05-03 22:38:33','2026-05-03 22:38:33'),(29,155,35,0,NULL,'2026-05-03 22:38:33','2026-05-03 22:38:33'),(30,156,35,0,NULL,'2026-05-03 22:38:33','2026-05-03 22:38:33'),(31,157,35,0,NULL,'2026-05-03 22:38:33','2026-05-03 22:38:33'),(32,158,35,0,NULL,'2026-05-03 22:38:33','2026-05-03 22:38:33'),(33,159,35,0,NULL,'2026-05-03 22:38:33','2026-05-03 22:38:33'),(34,160,35,0,NULL,'2026-05-03 22:38:33','2026-05-03 22:38:33'),(35,161,35,0,NULL,'2026-05-03 22:38:33','2026-05-03 22:38:33'),(36,159,36,0,NULL,'2026-05-03 22:38:33','2026-05-03 22:38:33'),(37,160,36,0,NULL,'2026-05-03 22:38:33','2026-05-03 22:38:33'),(38,161,36,0,NULL,'2026-05-03 22:38:33','2026-05-03 22:38:33'),(39,92,39,1,'2026-05-05 10:33:02','2026-05-05 10:33:02','2026-05-05 10:33:02'),(40,92,40,1,NULL,'2026-05-05 19:27:49','2026-05-05 19:27:49'),(41,92,41,1,NULL,'2026-05-05 19:27:51','2026-05-05 19:27:51'),(42,92,46,1,NULL,'2026-05-05 20:03:05','2026-05-05 20:03:05'),(43,92,45,1,NULL,'2026-05-05 20:03:07','2026-05-05 20:03:07'),(44,92,44,1,NULL,'2026-05-05 20:03:08','2026-05-05 20:03:08'),(45,92,43,1,'2026-05-05 20:03:08','2026-05-05 20:03:08','2026-05-05 20:03:08'),(46,162,91,0,NULL,'2026-08-12 08:52:43','2026-08-14 08:52:43');
/*!40000 ALTER TABLE `notification_user_inbox` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `org_class`
--

DROP TABLE IF EXISTS `org_class`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `org_class` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `college` varchar(64) NOT NULL COMMENT '学院（含年级）',
  `major` varchar(64) NOT NULL COMMENT '专业',
  `class_name` varchar(128) NOT NULL COMMENT '班级名称',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序，小在前',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_org_class` (`college`,`major`,`class_name`),
  KEY `idx_sort` (`sort_order`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标准班级字典';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `org_class`
--

LOCK TABLES `org_class` WRITE;
/*!40000 ALTER TABLE `org_class` DISABLE KEYS */;
INSERT INTO `org_class` VALUES (1,'2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术1班',1,'2026-03-23 00:07:40'),(2,'2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术2班',2,'2026-03-23 00:07:40'),(3,'2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术3班',3,'2026-03-23 00:07:40'),(4,'2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术4班',4,'2026-03-23 00:07:40'),(5,'计算机科学与技术学院','计算机科学与技术','计算机科学与技术1班',11,'2026-04-28 20:40:05'),(6,'计算机科学与技术学院','计算机科学与技术','计算机科学与技术2班',12,'2026-04-28 20:40:05'),(7,'计算机科学与技术学院','计算机科学与技术','计算机科学与技术3班',13,'2026-04-28 20:40:05'),(8,'计算机科学与技术学院','计算机科学与技术','计算机科学与技术4班',14,'2026-04-28 20:40:05'),(9,'计算机科学与技术学院','计算机科学与技术','计算机科学与技术5班',15,'2026-04-28 20:40:05'),(10,'计算机科学与技术学院','计算机科学与技术','计算机科学与技术6班',16,'2026-04-28 20:40:05'),(11,'软件学院','软件工程','软件工程1班',21,'2026-04-28 20:40:05'),(12,'软件学院','软件工程','软件工程2班',22,'2026-04-28 20:40:05'),(13,'软件学院','软件工程','软件工程3班',23,'2026-04-28 20:40:05'),(14,'人工智能学院','人工智能','人工智能1班',31,'2026-04-28 20:40:05'),(15,'人工智能学院','人工智能','人工智能2班',32,'2026-04-28 20:40:05'),(20,'信息工程学院','计算机科学与技术','计科2401班',101,'2026-04-30 14:42:42'),(21,'信息工程学院','计算机科学与技术','计科2402班',102,'2026-04-30 14:42:42'),(22,'软件学院','软件工程','软件2401班',201,'2026-04-30 14:42:42'),(23,'人工智能学院','人工智能','智能2401班',301,'2026-04-30 14:42:42'),(24,'Info Engineering College','Computer Science','CS2401',101,'2026-05-01 17:19:38'),(25,'Info Engineering College','Computer Science','CS2402',102,'2026-05-01 17:19:38'),(26,'Software College','Software Engineering','SE2401',201,'2026-05-01 17:19:38'),(27,'AI College','Artificial Intelligence','AI2401',301,'2026-05-01 17:19:38');
/*!40000 ALTER TABLE `org_class` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pass_record`
--

DROP TABLE IF EXISTS `pass_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pass_record` (
  `pass_id` int NOT NULL AUTO_INCREMENT COMMENT '通行记录ID',
  `etc_number` varchar(64) DEFAULT NULL COMMENT 'BLE蓝牙标签编号',
  `vehicle_id` int DEFAULT NULL COMMENT '车辆ID',
  `license_plate` varchar(20) DEFAULT NULL COMMENT '车牌号',
  `user_id` int DEFAULT NULL COMMENT '用户ID',
  `reader_id` varchar(32) DEFAULT NULL COMMENT '蓝牙网关ID',
  `reader_name` varchar(64) DEFAULT NULL COMMENT '蓝牙网关名称',
  `direction` varchar(10) DEFAULT 'in' COMMENT '通行方向：in=入校, out=出校',
  `pass_result` varchar(10) DEFAULT 'pass' COMMENT '通行结果：pass=通过, deny=拒绝',
  `pass_time` datetime DEFAULT NULL COMMENT '通行时间',
  `image_url` varchar(255) DEFAULT NULL COMMENT '抓拍图片URL',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`pass_id`),
  KEY `idx_etc_number` (`etc_number`),
  KEY `idx_vehicle_id` (`vehicle_id`),
  KEY `idx_pass_time` (`pass_time`),
  KEY `idx_direction` (`direction`)
) ENGINE=InnoDB AUTO_INCREMENT=47 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='BLE蓝牙识别通行记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pass_record`
--

LOCK TABLES `pass_record` WRITE;
/*!40000 ALTER TABLE `pass_record` DISABLE KEYS */;
INSERT INTO `pass_record` VALUES (1,'BLE001',1,'湘A0533319',2,'BLE-GW-001','东校区正门蓝牙网关','in','pass','2024-03-24 07:35:22','/uploads/pass/20240324/1.jpg','2026-03-24 20:31:11',0),(2,'BLE002',2,'湘A0533320',3,'BLE-GW-001','东校区正门蓝牙网关','in','pass','2024-03-24 07:42:15','/uploads/pass/20240324/2.jpg','2026-03-24 20:31:11',0),(3,'BLE004',4,'湘A0533322',4,'BLE-GW-002','西校区侧门蓝牙网关','in','pass','2024-03-24 08:05:33','/uploads/pass/20240324/3.jpg','2026-03-24 20:31:11',0),(4,'BLE005',5,'湘A0533323',5,'BLE-GW-001','东校区正门蓝牙网关','out','pass','2024-03-24 12:10:45','/uploads/pass/20240324/4.jpg','2026-03-24 20:31:11',0),(5,'BLE006',6,'湘A0533324',6,'BLE-GW-002','西校区侧门蓝牙网关','in','pass','2024-03-24 13:20:11','/uploads/pass/20240324/5.jpg','2026-03-24 20:31:11',0),(6,'BLE001',1,'湘A0533319',2,'BLE-GW-001','东校区正门蓝牙网关','out','pass','2024-03-24 18:25:30','/uploads/pass/20240324/6.jpg','2026-03-24 20:31:11',0),(7,'BLE002',2,'湘A0533320',3,'BLE-GW-002','西校区侧门蓝牙网关','out','pass','2024-03-24 18:30:55','/uploads/pass/20240324/7.jpg','2026-03-24 20:31:11',0),(8,'BLE005',5,'湘A0533323',5,'BLE-GW-001','东校区正门蓝牙网关','in','deny','2024-03-24 19:15:20','/uploads/pass/20240324/8.jpg','2026-03-24 20:31:11',0),(9,'2a:1f:7f:0b:ab:ed',NULL,NULL,NULL,NULL,NULL,'in','pass','2026-03-25 20:57:20',NULL,'2026-03-25 20:57:20',0),(10,'7b:2d:b2:8e:d0:7d',NULL,NULL,NULL,NULL,NULL,'in','pass','2026-03-25 21:04:28',NULL,'2026-03-25 21:04:28',0),(11,'24:1a:1c:62:d9:5f',NULL,NULL,NULL,NULL,NULL,'in','pass','2026-03-25 21:10:04',NULL,'2026-03-25 21:10:04',0),(12,'39:40:8a:ab:17:db',NULL,NULL,NULL,NULL,NULL,'in','pass','2026-03-25 21:10:05',NULL,'2026-03-25 21:10:05',0),(13,'e1:75:72:1e:75:5a',NULL,NULL,NULL,NULL,NULL,'in','pass','2026-03-31 20:28:49',NULL,'2026-03-31 20:28:49',0),(14,'e1:75:72:1e:75:5a',NULL,NULL,NULL,NULL,NULL,'in','pass','2026-03-31 20:29:02',NULL,'2026-03-31 20:29:02',0),(15,'e1:75:72:1e:75:5a',NULL,NULL,NULL,NULL,NULL,'in','pass','2026-03-31 20:29:07',NULL,'2026-03-31 20:29:07',0),(16,'e1:75:72:1e:75:5a',NULL,NULL,NULL,NULL,NULL,'in','pass','2026-03-31 20:29:18',NULL,'2026-03-31 20:29:18',0),(17,'e1:75:72:1e:75:5a',NULL,NULL,NULL,NULL,NULL,'in','pass','2026-03-31 20:29:22',NULL,'2026-03-31 20:29:22',0),(18,'e1:75:72:1e:75:5a',NULL,NULL,NULL,NULL,NULL,'in','pass','2026-03-31 20:29:36',NULL,'2026-03-31 20:29:36',0),(19,'e1:75:72:1e:75:5a',NULL,NULL,NULL,NULL,NULL,'in','pass','2026-03-31 20:29:41',NULL,'2026-03-31 20:29:41',0),(20,'e1:75:72:1e:75:5a',NULL,NULL,NULL,NULL,NULL,'in','pass','2026-03-31 20:29:50',NULL,'2026-03-31 20:29:50',0),(21,'e1:75:72:1e:75:5a',NULL,NULL,NULL,NULL,NULL,'in','pass','2026-03-31 20:29:54',NULL,'2026-03-31 20:29:54',0),(34,'BLE-HY-0001',23,'湘A0533305',92,'GATE-EAST-01','East Gate BLE Gateway','in','pass','2026-05-01 08:19:39',NULL,'2026-05-01 17:19:39',0),(35,'BLE-HY-0001',23,'湘A0533305',92,'GATE-EAST-01','East Gate BLE Gateway','out','pass','2026-05-01 15:19:39',NULL,'2026-05-01 17:19:39',0),(36,'BLE-HY-0002',24,'湘A0533306',93,'GATE-SOUTH-01','South Gate BLE Gateway','in','pass','2026-05-01 09:19:39',NULL,'2026-05-01 17:19:39',0),(37,'BLE-HY-0002',24,'湘A0533306',93,'GATE-SOUTH-01','South Gate BLE Gateway','out','pass','2026-05-01 16:19:39',NULL,'2026-05-01 17:19:39',0),(38,'BLE-HY-0003',25,'湘A0533307',94,'GATE-WEST-01','West Gate BLE Gateway','in','pass','2026-05-01 10:19:39',NULL,'2026-05-01 17:19:39',0),(39,'BLE-HY-0004',26,'湘A0533308',95,'GATE-NORTH-01','North Gate BLE Gateway','in','pass','2026-05-01 11:19:39',NULL,'2026-05-01 17:19:39',0),(40,'BLE-HY-0005',27,'湘A0533309',96,'GATE-EAST-01','East Gate BLE Gateway','in','pass','2026-05-01 12:19:39',NULL,'2026-05-01 17:19:39',0),(41,'BLE-HY-0006',28,'湘A0533310',97,'GATE-SOUTH-01','South Gate BLE Gateway','out','pass','2026-05-01 13:19:39',NULL,'2026-05-01 17:19:39',0),(42,'BLE-HY-0007',29,'湘A0533311',98,'GATE-WEST-01','West Gate BLE Gateway','in','pass','2026-05-01 14:19:39',NULL,'2026-05-01 17:19:39',0),(43,'BLE-HY-0008',30,'湘A0533312',99,'GATE-NORTH-01','North Gate BLE Gateway','out','pass','2026-05-01 15:19:39',NULL,'2026-05-01 17:19:39',0),(44,'BLE-HY-0009',31,'湘A0533313',100,'GATE-EAST-01','East Gate BLE Gateway','in','pass','2026-05-01 16:19:39',NULL,'2026-05-01 17:19:39',0),(45,'BLE-HY-0010',32,'湘A0533314',101,'GATE-SOUTH-01','South Gate BLE Gateway','in','pass','2026-05-01 16:49:39',NULL,'2026-05-01 17:19:39',0),(46,'BLE-CHAIN-0001',43,'湘A0599001',162,'GATE-EAST-01','东门蓝牙网关','in','pass','2026-08-11 08:52:43','/demo/pass/chain-entry.jpg','2026-08-11 08:52:43',0);
/*!40000 ALTER TABLE `pass_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `patrol_audit_log`
--

DROP TABLE IF EXISTS `patrol_audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `patrol_audit_log` (
  `audit_id` bigint NOT NULL AUTO_INCREMENT,
  `mission_id` bigint DEFAULT NULL,
  `operator_user_id` int DEFAULT NULL,
  `action` varchar(48) COLLATE utf8mb4_unicode_ci NOT NULL,
  `input_summary` text COLLATE utf8mb4_unicode_ci,
  `result_summary` text COLLATE utf8mb4_unicode_ci,
  `state` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`audit_id`),
  KEY `idx_patrol_audit_mission` (`mission_id`,`create_time`),
  KEY `idx_patrol_audit_operator` (`operator_user_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patrol_audit_log`
--

LOCK TABLES `patrol_audit_log` WRITE;
/*!40000 ALTER TABLE `patrol_audit_log` DISABLE KEYS */;
INSERT INTO `patrol_audit_log` VALUES (1,13,1,'CREATE_MISSION','{\"area\":\"东校区\",\"durationLimitSec\":720}','{\"missionCode\":\"PM-714152F365FC\"}','SUCCEEDED','2026-08-22 14:27:23'),(2,13,1,'PLAN_MISSION','{\"preserveCompleted\":false}','{\"oldWaypointCount\":0}','STARTED','2026-08-22 14:27:23'),(3,13,1,'GENERATE_REPORT','{}','{\"reportCode\":\"RPT-64C1EAF05431\"}','SUCCEEDED','2026-08-22 14:27:23'),(4,14,1,'CREATE_MISSION','{\"durationLimitSec\":720,\"area\":\"桂林理工大学屏风校区\"}','{\"missionCode\":\"PM-6DBDEB4C6594\"}','SUCCEEDED','2026-08-22 14:44:46'),(5,14,1,'PLAN_MISSION','{\"preserveCompleted\":false}','{\"oldWaypointCount\":0}','STARTED','2026-08-22 14:44:46'),(6,14,1,'START_MISSION','{}','{\"state\":\"RUNNING\"}','SUCCEEDED','2026-08-22 14:44:47'),(7,14,NULL,'INGEST_TELEMETRY','{\"lat\":25.287518,\"lng\":110.323913}','{\"battery\":88.0}','SUCCEEDED','2026-08-22 14:44:50'),(8,14,1,'ATTACH_EVENT','{\"eventId\":22}','{\"triggerReplan\":false}','SUCCEEDED','2026-08-22 14:44:50'),(9,14,1,'GENERATE_REPORT','{}','{\"reportCode\":\"RPT-CADB028963DC\"}','SUCCEEDED','2026-08-22 14:44:50'),(10,15,1,'CREATE_MISSION','{\"durationLimitSec\":720,\"area\":\"桂林理工大学屏风校区\"}','{\"missionCode\":\"PM-997D69CEEAA6\"}','SUCCEEDED','2026-08-22 14:45:55'),(11,15,1,'PLAN_MISSION','{\"preserveCompleted\":false}','{\"oldWaypointCount\":0}','STARTED','2026-08-22 14:45:55'),(12,15,1,'START_MISSION','{}','{\"state\":\"RUNNING\"}','SUCCEEDED','2026-08-22 14:45:55'),(13,15,NULL,'INGEST_TELEMETRY','{\"lat\":25.287518,\"lng\":110.323913}','{\"battery\":91.0}','SUCCEEDED','2026-08-22 14:45:56'),(14,15,1,'ATTACH_EVENT','{\"eventId\":23}','{\"triggerReplan\":false}','SUCCEEDED','2026-08-22 14:45:56'),(15,15,1,'GENERATE_REPORT','{}','{\"reportCode\":\"RPT-E2124D96BCFB\"}','SUCCEEDED','2026-08-22 14:45:56'),(16,16,1,'CREATE_MISSION','{\"durationLimitSec\":720,\"area\":\"屏风校区\"}','{\"missionCode\":\"PM-6C747523EA22\"}','SUCCEEDED','2026-08-22 15:31:19'),(17,16,1,'PLAN_MISSION','{\"preserveCompleted\":false}','{\"oldWaypointCount\":0}','STARTED','2026-08-22 15:31:19'),(18,16,1,'START_MISSION','{}','{\"state\":\"RUNNING\"}','SUCCEEDED','2026-08-22 15:31:19'),(19,17,1,'CREATE_MISSION','{\"durationLimitSec\":720,\"area\":\"屏风校区\"}','{\"missionCode\":\"PM-DB4F0DC8023B\"}','SUCCEEDED','2026-08-22 15:31:55'),(20,17,1,'PLAN_MISSION','{\"preserveCompleted\":false}','{\"oldWaypointCount\":0}','STARTED','2026-08-22 15:31:55'),(21,17,1,'START_MISSION','{}','{\"state\":\"RUNNING\"}','SUCCEEDED','2026-08-22 15:31:55'),(22,18,1,'CREATE_MISSION','{\"durationLimitSec\":720,\"area\":\"屏风校区\"}','{\"missionCode\":\"PM-666F296862ED\"}','SUCCEEDED','2026-08-22 15:32:30'),(23,18,1,'PLAN_MISSION','{\"preserveCompleted\":false}','{\"oldWaypointCount\":0}','STARTED','2026-08-22 15:32:30'),(24,18,1,'START_MISSION','{}','{\"state\":\"RUNNING\"}','SUCCEEDED','2026-08-22 15:32:30'),(25,19,1,'CREATE_MISSION','{\"durationLimitSec\":720,\"area\":\"屏风校区\"}','{\"missionCode\":\"PM-4CD267B8FD43\"}','SUCCEEDED','2026-08-22 15:32:59'),(26,19,1,'PLAN_MISSION','{\"preserveCompleted\":false}','{\"oldWaypointCount\":0}','STARTED','2026-08-22 15:32:59'),(27,19,1,'START_MISSION','{}','{\"state\":\"RUNNING\"}','SUCCEEDED','2026-08-22 15:32:59'),(28,20,1,'CREATE_MISSION','{\"durationLimitSec\":720,\"area\":\"屏风校区\"}','{\"missionCode\":\"PM-F94F8747403B\"}','SUCCEEDED','2026-08-22 15:33:35'),(29,20,1,'PLAN_MISSION','{\"preserveCompleted\":false}','{\"oldWaypointCount\":0}','STARTED','2026-08-22 15:33:35'),(30,20,1,'START_MISSION','{}','{\"state\":\"RUNNING\"}','SUCCEEDED','2026-08-22 15:33:35'),(31,21,1,'CREATE_MISSION','{\"durationLimitSec\":720,\"area\":\"屏风校区\"}','{\"missionCode\":\"PM-D5779E3E69B7\"}','SUCCEEDED','2026-08-22 15:34:06'),(32,21,1,'PLAN_MISSION','{\"preserveCompleted\":false}','{\"oldWaypointCount\":0}','STARTED','2026-08-22 15:34:06'),(33,21,1,'START_MISSION','{}','{\"state\":\"RUNNING\"}','SUCCEEDED','2026-08-22 15:34:06'),(34,21,NULL,'INGEST_TELEMETRY','{\"lat\":25.2875179,\"lng\":110.323913}','{\"battery\":88.0}','SUCCEEDED','2026-08-22 15:34:06'),(35,21,1,'ATTACH_EVENT','{\"eventId\":24}','{\"triggerReplan\":false}','SUCCEEDED','2026-08-22 15:34:06'),(36,21,1,'GENERATE_REPORT','{}','{\"reportCode\":\"RPT-3026ACAFCD6B\"}','SUCCEEDED','2026-08-22 15:34:06'),(37,23,1,'CREATE_MISSION','{\"durationLimitSec\":300,\"area\":\"屏风校区北门及宿舍区\"}','{\"missionCode\":\"PM-B28B1FE5A0BE\"}','SUCCEEDED','2026-08-24 20:46:20'),(38,23,1,'ATTACH_EVENT','{\"eventId\":25}','{\"triggerReplan\":false}','SUCCEEDED','2026-08-24 20:47:24'),(39,24,1,'CREATE_MISSION','{\"durationLimitSec\":720,\"area\":\"桂林理工大学屏风校区\"}','{\"missionCode\":\"PM-E56A5B4324B7\"}','SUCCEEDED','2026-09-10 18:59:39'),(40,24,1,'PLAN_MISSION','{\"preserveCompleted\":false}','{\"oldWaypointCount\":0}','STARTED','2026-09-10 18:59:41'),(41,24,1,'START_MISSION','{}','{\"state\":\"RUNNING\"}','SUCCEEDED','2026-09-10 18:59:41');
/*!40000 ALTER TABLE `patrol_audit_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `patrol_event_link`
--

DROP TABLE IF EXISTS `patrol_event_link`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `patrol_event_link` (
  `link_id` bigint NOT NULL AUTO_INCREMENT,
  `mission_id` bigint NOT NULL,
  `event_id` bigint NOT NULL,
  `waypoint_id` bigint DEFAULT NULL,
  `relation_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DISCOVERED',
  `trigger_replan` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`link_id`),
  UNIQUE KEY `uk_patrol_event_link` (`mission_id`,`event_id`),
  KEY `idx_patrol_event_mission` (`mission_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=160 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patrol_event_link`
--

LOCK TABLES `patrol_event_link` WRITE;
/*!40000 ALTER TABLE `patrol_event_link` DISABLE KEYS */;
INSERT INTO `patrol_event_link` VALUES (1,3,11,NULL,'DISCOVERED',1,'2026-08-22 10:59:38'),(5,4,11,NULL,'NEARBY',0,'2026-08-22 11:00:34'),(6,3,17,NULL,'DISCOVERED',0,'2026-08-22 11:10:20'),(9,5,11,NULL,'NEARBY',0,'2026-08-22 11:17:00'),(10,5,17,NULL,'NEARBY',0,'2026-08-22 11:17:00'),(19,5,18,NULL,'DISCOVERED',0,'2026-08-22 11:17:05'),(53,6,11,NULL,'NEARBY',0,'2026-08-22 11:53:06'),(54,6,17,NULL,'NEARBY',0,'2026-08-22 11:53:06'),(55,6,18,NULL,'NEARBY',0,'2026-08-22 11:53:06'),(58,3,18,NULL,'NEARBY',0,'2026-08-22 13:40:51'),(68,9,11,NULL,'NEARBY',0,'2026-08-22 14:09:25'),(69,9,17,NULL,'NEARBY',0,'2026-08-22 14:09:25'),(70,9,18,NULL,'NEARBY',0,'2026-08-22 14:09:25'),(71,10,11,NULL,'NEARBY',0,'2026-08-22 14:09:37'),(72,10,17,NULL,'NEARBY',0,'2026-08-22 14:09:37'),(73,10,18,NULL,'NEARBY',0,'2026-08-22 14:09:37'),(74,11,11,NULL,'NEARBY',0,'2026-08-22 14:09:59'),(75,11,17,NULL,'NEARBY',0,'2026-08-22 14:09:59'),(76,11,18,NULL,'NEARBY',0,'2026-08-22 14:09:59'),(80,12,11,NULL,'NEARBY',0,'2026-08-22 14:10:24'),(81,12,17,NULL,'NEARBY',0,'2026-08-22 14:10:24'),(82,12,18,NULL,'NEARBY',0,'2026-08-22 14:10:24'),(83,12,19,NULL,'NEARBY',0,'2026-08-22 14:10:24'),(108,12,20,NULL,'NEARBY',0,'2026-08-22 14:24:09'),(109,12,21,NULL,'NEARBY',0,'2026-08-22 14:24:09'),(116,13,11,NULL,'NEARBY',0,'2026-08-22 14:27:23'),(117,13,17,NULL,'NEARBY',0,'2026-08-22 14:27:23'),(118,13,18,NULL,'NEARBY',0,'2026-08-22 14:27:23'),(119,13,19,NULL,'NEARBY',0,'2026-08-22 14:27:23'),(120,13,20,NULL,'NEARBY',0,'2026-08-22 14:27:23'),(121,13,21,NULL,'NEARBY',0,'2026-08-22 14:27:23'),(134,14,22,NULL,'DISCOVERED',0,'2026-08-22 14:44:50'),(138,15,22,NULL,'NEARBY',0,'2026-08-22 14:45:55'),(141,15,23,NULL,'DISCOVERED',0,'2026-08-22 14:45:56'),(158,21,24,NULL,'DISCOVERED',0,'2026-08-22 15:34:06'),(159,23,25,NULL,'DISCOVERED',0,'2026-08-24 20:47:24');
/*!40000 ALTER TABLE `patrol_event_link` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `patrol_fault_injection`
--

DROP TABLE IF EXISTS `patrol_fault_injection`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `patrol_fault_injection` (
  `fault_id` bigint NOT NULL AUTO_INCREMENT,
  `mission_id` bigint NOT NULL,
  `fault_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `parameters_json` text COLLATE utf8mb4_unicode_ci,
  `injected_at` datetime NOT NULL,
  `recovered_at` datetime DEFAULT NULL,
  `state` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'INJECTED',
  PRIMARY KEY (`fault_id`),
  KEY `idx_patrol_fault_mission` (`mission_id`,`injected_at`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patrol_fault_injection`
--

LOCK TABLES `patrol_fault_injection` WRITE;
/*!40000 ALTER TABLE `patrol_fault_injection` DISABLE KEYS */;
INSERT INTO `patrol_fault_injection` VALUES (1,7,'GPS_OFFSET','{\"meters\":12}','2026-08-22 13:51:12',NULL,'INJECTED'),(2,8,'PACKET_LOSS','{\"rate\":0.5}','2026-08-22 13:55:12','2026-08-22 13:55:12','RECOVERED'),(3,12,'GPS_OFFSET','{\"meters\":12}','2026-08-22 14:10:27','2026-08-22 14:10:27','RECOVERED');
/*!40000 ALTER TABLE `patrol_fault_injection` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `patrol_goal_plan`
--

DROP TABLE IF EXISTS `patrol_goal_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `patrol_goal_plan` (
  `plan_id` bigint NOT NULL AUTO_INCREMENT,
  `mission_id` bigint NOT NULL,
  `provider` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `model_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `plan_json` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `raw_response` longtext COLLATE utf8mb4_unicode_ci,
  `latency_ms` bigint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`plan_id`),
  UNIQUE KEY `mission_id` (`mission_id`),
  KEY `idx_patrol_goal_plan_mission` (`mission_id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patrol_goal_plan`
--

LOCK TABLES `patrol_goal_plan` WRITE;
/*!40000 ALTER TABLE `patrol_goal_plan` DISABLE KEYS */;
INSERT INTO `patrol_goal_plan` VALUES (1,7,'rule-fallback','patrol-rule-v1','{\"taskType\":\"CAMPUS_EBIKE_PATROL\",\"targetTypes\":[\"ILLEGAL_PARKING\",\"RESTRICTED_AREA\",\"NO_HELMET\",\"SMOKE_DETECTED\"],\"priority\":\"CRITICAL\",\"area\":\"桂林理工大学屏风校区\",\"durationLimitSec\":720,\"sensorRequirements\":[\"RGB_CAMERA\",\"GPS\",\"TELEMETRY\"],\"constraints\":[\"ACTIVE_GEOFENCE\",\"NO_FLY_ZONE\",\"RETURN_BATTERY_25\",\"HOME_RETURN\"],\"replanTriggers\":[\"NEW_AI_EVENT\",\"LOW_BATTERY\",\"TEMPORARY_NO_FLY_ZONE\",\"WAYPOINT_UNREACHABLE\"],\"executionSteps\":[\"UNDERSTAND\",\"OBSERVE\",\"PLAN\",\"EXECUTE\",\"MONITOR\",\"REPLAN\",\"DELIVER\"],\"fallbackReason\":\"HTTP 401\"}','fallback:HTTP 401',1203,'2026-08-22 13:51:12'),(2,8,'rule-fallback','patrol-rule-v1','{\"taskType\":\"CAMPUS_EBIKE_PATROL\",\"targetTypes\":[\"ILLEGAL_PARKING\",\"RESTRICTED_AREA\",\"NO_HELMET\"],\"priority\":\"HIGH\",\"area\":\"桂林理工大学屏风校区\",\"durationLimitSec\":600,\"sensorRequirements\":[\"RGB_CAMERA\",\"GPS\",\"TELEMETRY\"],\"constraints\":[\"ACTIVE_GEOFENCE\",\"NO_FLY_ZONE\",\"RETURN_BATTERY_25\",\"HOME_RETURN\"],\"replanTriggers\":[\"NEW_AI_EVENT\",\"LOW_BATTERY\",\"TEMPORARY_NO_FLY_ZONE\",\"WAYPOINT_UNREACHABLE\"],\"executionSteps\":[\"UNDERSTAND\",\"OBSERVE\",\"PLAN\",\"EXECUTE\",\"MONITOR\",\"REPLAN\",\"DELIVER\"],\"fallbackReason\":\"HTTP 401\"}','fallback:HTTP 401',1186,'2026-08-22 13:55:12'),(3,9,'rule-fallback','patrol-rule-v1','{\"taskType\":\"CAMPUS_EBIKE_PATROL\",\"targetTypes\":[\"ILLEGAL_PARKING\",\"RESTRICTED_AREA\",\"NO_HELMET\"],\"priority\":\"HIGH\",\"area\":\"东校区\",\"durationLimitSec\":720,\"sensorRequirements\":[\"RGB_CAMERA\",\"GPS\",\"TELEMETRY\"],\"constraints\":[\"ACTIVE_GEOFENCE\",\"NO_FLY_ZONE\",\"RETURN_BATTERY_25\",\"HOME_RETURN\"],\"replanTriggers\":[\"NEW_AI_EVENT\",\"LOW_BATTERY\",\"TEMPORARY_NO_FLY_ZONE\",\"WAYPOINT_UNREACHABLE\"],\"executionSteps\":[\"UNDERSTAND\",\"OBSERVE\",\"PLAN\",\"EXECUTE\",\"MONITOR\",\"REPLAN\",\"DELIVER\"],\"fallbackReason\":\"HTTP 401\"}','fallback:HTTP 401',1199,'2026-08-22 14:09:25'),(4,10,'rule-fallback','patrol-rule-v1','{\"taskType\":\"CAMPUS_EBIKE_PATROL\",\"targetTypes\":[\"ILLEGAL_PARKING\",\"RESTRICTED_AREA\",\"NO_HELMET\"],\"priority\":\"HIGH\",\"area\":\"东校区\",\"durationLimitSec\":720,\"sensorRequirements\":[\"RGB_CAMERA\",\"GPS\",\"TELEMETRY\"],\"constraints\":[\"ACTIVE_GEOFENCE\",\"NO_FLY_ZONE\",\"RETURN_BATTERY_25\",\"HOME_RETURN\"],\"replanTriggers\":[\"NEW_AI_EVENT\",\"LOW_BATTERY\",\"TEMPORARY_NO_FLY_ZONE\",\"WAYPOINT_UNREACHABLE\"],\"executionSteps\":[\"UNDERSTAND\",\"OBSERVE\",\"PLAN\",\"EXECUTE\",\"MONITOR\",\"REPLAN\",\"DELIVER\"],\"fallbackReason\":\"HTTP 401\"}','fallback:HTTP 401',979,'2026-08-22 14:09:37'),(5,11,'rule-fallback','patrol-rule-v1','{\"taskType\":\"CAMPUS_EBIKE_PATROL\",\"targetTypes\":[\"ILLEGAL_PARKING\",\"RESTRICTED_AREA\",\"NO_HELMET\"],\"priority\":\"HIGH\",\"area\":\"东校区\",\"durationLimitSec\":720,\"sensorRequirements\":[\"RGB_CAMERA\",\"GPS\",\"TELEMETRY\"],\"constraints\":[\"ACTIVE_GEOFENCE\",\"NO_FLY_ZONE\",\"RETURN_BATTERY_25\",\"HOME_RETURN\"],\"replanTriggers\":[\"NEW_AI_EVENT\",\"LOW_BATTERY\",\"TEMPORARY_NO_FLY_ZONE\",\"WAYPOINT_UNREACHABLE\"],\"executionSteps\":[\"UNDERSTAND\",\"OBSERVE\",\"PLAN\",\"EXECUTE\",\"MONITOR\",\"REPLAN\",\"DELIVER\"],\"fallbackReason\":\"HTTP 401\"}','fallback:HTTP 401',1040,'2026-08-22 14:09:59'),(6,12,'rule-fallback','patrol-rule-v1','{\"taskType\":\"CAMPUS_EBIKE_PATROL\",\"targetTypes\":[\"ILLEGAL_PARKING\",\"RESTRICTED_AREA\",\"NO_HELMET\"],\"priority\":\"HIGH\",\"area\":\"东校区\",\"durationLimitSec\":720,\"sensorRequirements\":[\"RGB_CAMERA\",\"GPS\",\"TELEMETRY\"],\"constraints\":[\"ACTIVE_GEOFENCE\",\"NO_FLY_ZONE\",\"RETURN_BATTERY_25\",\"HOME_RETURN\"],\"replanTriggers\":[\"NEW_AI_EVENT\",\"LOW_BATTERY\",\"TEMPORARY_NO_FLY_ZONE\",\"WAYPOINT_UNREACHABLE\"],\"executionSteps\":[\"UNDERSTAND\",\"OBSERVE\",\"PLAN\",\"EXECUTE\",\"MONITOR\",\"REPLAN\",\"DELIVER\"],\"fallbackReason\":\"HTTP 401\"}','fallback:HTTP 401',1028,'2026-08-22 14:10:24'),(7,13,'rule-fallback','patrol-rule-v1','{\"taskType\":\"CAMPUS_EBIKE_PATROL\",\"targetTypes\":[\"ILLEGAL_PARKING\",\"RESTRICTED_AREA\",\"NO_HELMET\"],\"priority\":\"HIGH\",\"area\":\"东校区\",\"durationLimitSec\":720,\"sensorRequirements\":[\"RGB_CAMERA\",\"GPS\",\"TELEMETRY\"],\"constraints\":[\"ACTIVE_GEOFENCE\",\"NO_FLY_ZONE\",\"RETURN_BATTERY_25\",\"HOME_RETURN\"],\"replanTriggers\":[\"NEW_AI_EVENT\",\"LOW_BATTERY\",\"TEMPORARY_NO_FLY_ZONE\",\"WAYPOINT_UNREACHABLE\"],\"executionSteps\":[\"UNDERSTAND\",\"OBSERVE\",\"PLAN\",\"EXECUTE\",\"MONITOR\",\"REPLAN\",\"DELIVER\"]}','fallback:ai-not-configured',0,'2026-08-22 14:27:23'),(8,14,'rule-fallback','patrol-rule-v1','{\"taskType\":\"CAMPUS_EBIKE_PATROL\",\"targetTypes\":[\"ILLEGAL_PARKING\",\"RESTRICTED_AREA\",\"NO_HELMET\"],\"priority\":\"HIGH\",\"area\":\"桂林理工大学屏风校区\",\"durationLimitSec\":720,\"sensorRequirements\":[\"RGB_CAMERA\",\"GPS\",\"TELEMETRY\"],\"constraints\":[\"ACTIVE_GEOFENCE\",\"NO_FLY_ZONE\",\"RETURN_BATTERY_25\",\"HOME_RETURN\"],\"replanTriggers\":[\"NEW_AI_EVENT\",\"LOW_BATTERY\",\"TEMPORARY_NO_FLY_ZONE\",\"WAYPOINT_UNREACHABLE\"],\"executionSteps\":[\"UNDERSTAND\",\"OBSERVE\",\"PLAN\",\"EXECUTE\",\"MONITOR\",\"REPLAN\",\"DELIVER\"]}','fallback:ai-not-configured',0,'2026-08-22 14:44:46'),(9,15,'rule-fallback','patrol-rule-v1','{\"taskType\":\"CAMPUS_EBIKE_PATROL\",\"targetTypes\":[\"ILLEGAL_PARKING\",\"RESTRICTED_AREA\",\"NO_HELMET\"],\"priority\":\"HIGH\",\"area\":\"桂林理工大学屏风校区\",\"durationLimitSec\":720,\"sensorRequirements\":[\"RGB_CAMERA\",\"GPS\",\"TELEMETRY\"],\"constraints\":[\"ACTIVE_GEOFENCE\",\"NO_FLY_ZONE\",\"RETURN_BATTERY_25\",\"HOME_RETURN\"],\"replanTriggers\":[\"NEW_AI_EVENT\",\"LOW_BATTERY\",\"TEMPORARY_NO_FLY_ZONE\",\"WAYPOINT_UNREACHABLE\"],\"executionSteps\":[\"UNDERSTAND\",\"OBSERVE\",\"PLAN\",\"EXECUTE\",\"MONITOR\",\"REPLAN\",\"DELIVER\"]}','fallback:ai-not-configured',0,'2026-08-22 14:45:55'),(10,16,'rule-fallback','patrol-rule-v1','{\"taskType\":\"CAMPUS_EBIKE_PATROL\",\"targetTypes\":[\"ILLEGAL_PARKING\",\"RESTRICTED_AREA\",\"NO_HELMET\"],\"priority\":\"HIGH\",\"area\":\"屏风校区\",\"durationLimitSec\":720,\"sensorRequirements\":[\"RGB_CAMERA\",\"GPS\",\"TELEMETRY\"],\"constraints\":[\"ACTIVE_GEOFENCE\",\"NO_FLY_ZONE\",\"RETURN_BATTERY_25\",\"HOME_RETURN\"],\"replanTriggers\":[\"NEW_AI_EVENT\",\"LOW_BATTERY\",\"TEMPORARY_NO_FLY_ZONE\",\"WAYPOINT_UNREACHABLE\"],\"executionSteps\":[\"UNDERSTAND\",\"OBSERVE\",\"PLAN\",\"EXECUTE\",\"MONITOR\",\"REPLAN\",\"DELIVER\"]}','fallback:ai-not-configured',0,'2026-08-22 15:31:19'),(11,17,'rule-fallback','patrol-rule-v1','{\"taskType\":\"CAMPUS_EBIKE_PATROL\",\"targetTypes\":[\"ILLEGAL_PARKING\",\"RESTRICTED_AREA\",\"NO_HELMET\"],\"priority\":\"HIGH\",\"area\":\"屏风校区\",\"durationLimitSec\":720,\"sensorRequirements\":[\"RGB_CAMERA\",\"GPS\",\"TELEMETRY\"],\"constraints\":[\"ACTIVE_GEOFENCE\",\"NO_FLY_ZONE\",\"RETURN_BATTERY_25\",\"HOME_RETURN\"],\"replanTriggers\":[\"NEW_AI_EVENT\",\"LOW_BATTERY\",\"TEMPORARY_NO_FLY_ZONE\",\"WAYPOINT_UNREACHABLE\"],\"executionSteps\":[\"UNDERSTAND\",\"OBSERVE\",\"PLAN\",\"EXECUTE\",\"MONITOR\",\"REPLAN\",\"DELIVER\"]}','fallback:ai-not-configured',0,'2026-08-22 15:31:55'),(12,18,'rule-fallback','patrol-rule-v1','{\"taskType\":\"CAMPUS_EBIKE_PATROL\",\"targetTypes\":[\"ILLEGAL_PARKING\",\"RESTRICTED_AREA\",\"NO_HELMET\"],\"priority\":\"HIGH\",\"area\":\"屏风校区\",\"durationLimitSec\":720,\"sensorRequirements\":[\"RGB_CAMERA\",\"GPS\",\"TELEMETRY\"],\"constraints\":[\"ACTIVE_GEOFENCE\",\"NO_FLY_ZONE\",\"RETURN_BATTERY_25\",\"HOME_RETURN\"],\"replanTriggers\":[\"NEW_AI_EVENT\",\"LOW_BATTERY\",\"TEMPORARY_NO_FLY_ZONE\",\"WAYPOINT_UNREACHABLE\"],\"executionSteps\":[\"UNDERSTAND\",\"OBSERVE\",\"PLAN\",\"EXECUTE\",\"MONITOR\",\"REPLAN\",\"DELIVER\"]}','fallback:ai-not-configured',0,'2026-08-22 15:32:30'),(13,19,'rule-fallback','patrol-rule-v1','{\"taskType\":\"CAMPUS_EBIKE_PATROL\",\"targetTypes\":[\"ILLEGAL_PARKING\",\"RESTRICTED_AREA\",\"NO_HELMET\"],\"priority\":\"HIGH\",\"area\":\"屏风校区\",\"durationLimitSec\":720,\"sensorRequirements\":[\"RGB_CAMERA\",\"GPS\",\"TELEMETRY\"],\"constraints\":[\"ACTIVE_GEOFENCE\",\"NO_FLY_ZONE\",\"RETURN_BATTERY_25\",\"HOME_RETURN\"],\"replanTriggers\":[\"NEW_AI_EVENT\",\"LOW_BATTERY\",\"TEMPORARY_NO_FLY_ZONE\",\"WAYPOINT_UNREACHABLE\"],\"executionSteps\":[\"UNDERSTAND\",\"OBSERVE\",\"PLAN\",\"EXECUTE\",\"MONITOR\",\"REPLAN\",\"DELIVER\"]}','fallback:ai-not-configured',0,'2026-08-22 15:32:59'),(14,20,'rule-fallback','patrol-rule-v1','{\"taskType\":\"CAMPUS_EBIKE_PATROL\",\"targetTypes\":[\"ILLEGAL_PARKING\",\"RESTRICTED_AREA\",\"NO_HELMET\"],\"priority\":\"HIGH\",\"area\":\"屏风校区\",\"durationLimitSec\":720,\"sensorRequirements\":[\"RGB_CAMERA\",\"GPS\",\"TELEMETRY\"],\"constraints\":[\"ACTIVE_GEOFENCE\",\"NO_FLY_ZONE\",\"RETURN_BATTERY_25\",\"HOME_RETURN\"],\"replanTriggers\":[\"NEW_AI_EVENT\",\"LOW_BATTERY\",\"TEMPORARY_NO_FLY_ZONE\",\"WAYPOINT_UNREACHABLE\"],\"executionSteps\":[\"UNDERSTAND\",\"OBSERVE\",\"PLAN\",\"EXECUTE\",\"MONITOR\",\"REPLAN\",\"DELIVER\"]}','fallback:ai-not-configured',0,'2026-08-22 15:33:35'),(15,21,'rule-fallback','patrol-rule-v1','{\"taskType\":\"CAMPUS_EBIKE_PATROL\",\"targetTypes\":[\"ILLEGAL_PARKING\",\"RESTRICTED_AREA\",\"NO_HELMET\"],\"priority\":\"HIGH\",\"area\":\"屏风校区\",\"durationLimitSec\":720,\"sensorRequirements\":[\"RGB_CAMERA\",\"GPS\",\"TELEMETRY\"],\"constraints\":[\"ACTIVE_GEOFENCE\",\"NO_FLY_ZONE\",\"RETURN_BATTERY_25\",\"HOME_RETURN\"],\"replanTriggers\":[\"NEW_AI_EVENT\",\"LOW_BATTERY\",\"TEMPORARY_NO_FLY_ZONE\",\"WAYPOINT_UNREACHABLE\"],\"executionSteps\":[\"UNDERSTAND\",\"OBSERVE\",\"PLAN\",\"EXECUTE\",\"MONITOR\",\"REPLAN\",\"DELIVER\"]}','fallback:ai-not-configured',0,'2026-08-22 15:34:06'),(16,23,'openai-compatible','deepseek-chat','{\"taskType\":\"LOW_ALTITUDE_PATROL\",\"targetTypes\":[\"ILLEGAL_PARKING\",\"NO_HELMET\",\"THEFT_RISK\"],\"priority\":\"HIGH\",\"area\":\"屏风校区北门及宿舍区\",\"durationLimitSec\":300,\"sensorRequirements\":[\"RGB_CAMERA\",\"THERMAL_CAMERA\"],\"constraints\":[\"ACTIVE_GEOFENCE\",\"RETURN_BATTERY_25\",\"NO_FLY_ZONE_AVOIDANCE\"],\"replanTriggers\":[\"BATTERY_LOW\",\"GEOFENCE_VIOLATION\",\"OBSTACLE_DETECTED\"],\"executionSteps\":[\"TAKEOFF_FROM_NORTH_GATE\",\"PATROL_NORTH_GATE_FOR_ILLEGAL_PARKING_AND_NO_HELMET\",\"PROCEED_TO_DORMITORY_AREA\",\"SCAN_FOR_THEFT_RISK_AND_ILLEGAL_PARKING\",\"FOCUSED_INSPECTION_ON_SUSPECTED_CHARGING_VIOLATIONS_WITH_THERMAL\",\"RETURN_TO_BASE_WITH_BATTERY_RESERVE_25_PERCENT\"]}','{\"taskType\":\"LOW_ALTITUDE_PATROL\",\"targetTypes\":[\"ILLEGAL_PARKING\",\"NO_HELMET\",\"THEFT_RISK\"],\"priority\":\"HIGH\",\"area\":\"屏风校区北门及宿舍区\",\"durationLimitSec\":300,\"sensorRequirements\":[\"RGB_CAMERA\",\"THERMAL_CAMERA\"],\"constraints\":[\"ACTIVE_GEOFENCE\",\"RETURN_BATTERY_25\",\"NO_FLY_ZONE_AVOIDANCE\"],\"replanTriggers\":[\"BATTERY_LOW\",\"GEOFENCE_VIOLATION\",\"OBSTACLE_DETECTED\"],\"executionSteps\":[\"TAKEOFF_FROM_NORTH_GATE\",\"PATROL_NORTH_GATE_FOR_ILLEGAL_PARKING_AND_NO_HELMET\",\"PROCEED_TO_DORMITORY_AREA\",\"SCAN_FOR_THEFT_RISK_AND_ILLEGAL_PARKING\",\"FOCUSED_INSPECTION_ON_SUSPECTED_CHARGING_VIOLATIONS_WITH_THERMAL\",\"RETURN_TO_BASE_WITH_BATTERY_RESERVE_25_PERCENT\"]}',3081,'2026-08-24 20:46:23'),(17,24,'openai-compatible','deepseek-chat','{\"taskType\":\"校园低空巡检\",\"targetTypes\":[\"NO_HELMET\",\"OVERLOAD\",\"ILLEGAL_PARKING\",\"RESTRICTED_AREA\",\"SMOKE_DETECTED\",\"FIRE_DETECTED\",\"THEFT_RISK\"],\"priority\":\"HIGH\",\"area\":\"桂林理工大学屏风校区\",\"durationLimitSec\":720,\"sensorRequirements\":[\"可见光相机\",\"红外热成像\",\"实时图传\"],\"constraints\":{\"ACTIVE_GEOFENCE\":true,\"RETURN_BATTERY_25\":true,\"noFlyZones\":[\"教学楼顶\",\"图书馆上方\",\"人员密集区\"]},\"replanTriggers\":[\"发现火情或烟雾\",\"电池电量低于30%\",\"强风或降雨\",\"GPS信号丢失\",\"禁飞区冲突\"],\"executionSteps\":[\"解析任务，加载校园地图与禁飞区数据\",\"设定巡检航线，覆盖电动车停放区、充电棚、主要道路\",\"起飞前检查电池与传感器状态，确保电量>30%\",\"按航线自动巡检，实时AI识别目标事件\",\"发现异常事件，记录位置并回传至原AI事件中心\",\"持续监控电池，若低于25%自动返航\",\"完成巡检或触发返航后，安全降落并上传数据\"]}','{\"taskType\":\"校园低空巡检\",\"targetTypes\":[\"NO_HELMET\",\"OVERLOAD\",\"ILLEGAL_PARKING\",\"RESTRICTED_AREA\",\"SMOKE_DETECTED\",\"FIRE_DETECTED\",\"THEFT_RISK\"],\"priority\":\"HIGH\",\"area\":{\"name\":\"桂林理工大学屏风校区\",\"description\":\"校园电动车高风险区域\"},\"durationLimitSec\":720,\"sensorRequirements\":[\"可见光相机\",\"红外热成像\",\"实时图传\"],\"constraints\":{\"ACTIVE_GEOFENCE\":true,\"RETURN_BATTERY_25\":true,\"noFlyZones\":[\"教学楼顶\",\"图书馆上方\",\"人员密集区\"]},\"replanTriggers\":[\"发现火情或烟雾\",\"电池电量低于30%\",\"强风或降雨\",\"GPS信号丢失\",\"禁飞区冲突\"],\"executionSteps\":[\"解析任务，加载校园地图与禁飞区数据\",\"设定巡检航线，覆盖电动车停放区、充电棚、主要道路\",\"起飞前检查电池与传感器状态，确保电量>30%\",\"按航线自动巡检，实时AI识别目标事件\",\"发现异常事件，记录位置并回传至原AI事件中心\",\"持续监控电池，若低于25%自动返航\",\"完成巡检或触发返航后，安全降落并上传数据\"]}',2390,'2026-09-10 18:59:41');
/*!40000 ALTER TABLE `patrol_goal_plan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `patrol_mission`
--

DROP TABLE IF EXISTS `patrol_mission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `patrol_mission` (
  `mission_id` bigint NOT NULL AUTO_INCREMENT,
  `mission_code` varchar(48) COLLATE utf8mb4_unicode_ci NOT NULL,
  `goal` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `area_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `center_lng` decimal(11,7) NOT NULL,
  `center_lat` decimal(10,7) NOT NULL,
  `duration_limit_sec` int NOT NULL DEFAULT '720',
  `state` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL,
  `battery_start` decimal(5,2) NOT NULL DEFAULT '100.00',
  `battery_current` decimal(5,2) NOT NULL DEFAULT '100.00',
  `plan_version` int NOT NULL DEFAULT '0',
  `replans` int NOT NULL DEFAULT '0',
  `created_by` int DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`mission_id`),
  UNIQUE KEY `mission_code` (`mission_code`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patrol_mission`
--

LOCK TABLES `patrol_mission` WRITE;
/*!40000 ALTER TABLE `patrol_mission` DISABLE KEYS */;
INSERT INTO `patrol_mission` VALUES (1,'PM-79BC5BEAF265','验收东校区低空巡检链路','东校区',112.6849000,26.8893000,120,'PAUSED',100.00,100.00,1,0,NULL,'2026-08-22 10:57:29','2026-08-22 10:57:32'),(2,'PM-B44B382783E9','仿真遥测修复验收','东校区',112.6849000,26.8893000,120,'PAUSED',100.00,99.76,1,0,NULL,'2026-08-22 10:59:16','2026-08-22 10:59:19'),(3,'PM-D76E89BDE3F9','事件关联重规划验收','东校区',112.6849000,26.8893000,120,'PAUSED',100.00,99.76,2,1,NULL,'2026-08-22 10:59:38','2026-08-22 13:40:54'),(4,'PM-5E06EBAAFE46','Mock AI研判验收','东校区',112.6849000,26.8893000,120,'DRAFT',100.00,100.00,0,0,NULL,'2026-08-22 11:00:34','2026-08-22 11:00:34'),(5,'PM-055DDF7239A6','比赛演示：低空巡检东校区头盔与禁行风险','东校区',112.6849000,26.8893000,120,'PAUSED',100.00,99.28,1,0,NULL,'2026-08-22 11:17:00','2026-08-22 11:17:04'),(6,'PM-D3CF9A27FECA','巡查东校区晚高峰电动车安全情况，重点关注未戴头盔、禁行区和异常聚集。','东校区',112.6849000,26.8893000,720,'DRAFT',100.00,100.00,0,0,NULL,'2026-08-22 11:53:06','2026-08-22 11:53:06'),(7,'PM-C9B5793738A3','应急巡查充电区域烟雾和禁停区，发现风险后复核','桂林理工大学屏风校区',110.3239130,25.2875180,720,'PLANNED',100.00,99.76,2,1,NULL,'2026-08-22 13:51:10','2026-08-22 13:51:30'),(8,'PM-12BFEBD608CA','验证结构化巡检计划和故障恢复','桂林理工大学屏风校区',110.3239130,25.2875180,600,'COMPLETED',100.00,99.76,1,0,NULL,'2026-08-22 13:55:11','2026-08-22 13:55:14'),(9,'PM-281ADD15565E','巡查校园电动车高风险区域','东校区',112.6849000,26.8893000,720,'DRAFT',100.00,100.00,0,0,1,'2026-08-22 14:09:23','2026-08-22 14:09:23'),(10,'PM-57AF25979902','巡查校园电动车高风险区域','东校区',112.6849000,26.8893000,720,'DRAFT',100.00,100.00,0,0,1,'2026-08-22 14:09:36','2026-08-22 14:09:36'),(11,'PM-7506F033CEDD','巡查校园电动车高风险区域','东校区',112.6849000,26.8893000,720,'PLANNED',100.00,100.00,1,0,1,'2026-08-22 14:09:58','2026-08-22 14:09:59'),(12,'PM-9F86B212807C','验证无人机运行与故障恢复','东校区',112.6849000,26.8893000,720,'PAUSED',100.00,99.28,1,0,1,'2026-08-22 14:10:23','2026-08-22 14:10:27'),(13,'PM-714152F365FC','审计与报告接口验证','东校区',112.6849000,26.8893000,720,'PLANNED',100.00,100.00,1,0,1,'2026-08-22 14:27:23','2026-08-22 14:27:23'),(14,'PM-6DBDEB4C6594','核查校园电动车高风险区域并回传统一AI事件','桂林理工大学屏风校区',110.3239130,25.2875179,720,'COMPLETED',100.00,99.76,1,0,1,'2026-08-22 14:44:46','2026-08-22 14:44:48'),(15,'PM-997D69CEEAA6','E2E验收：原大屏巡检统一事件闭环','桂林理工大学屏风校区',110.3239130,25.2875180,720,'COMPLETED',100.00,99.76,1,0,1,'2026-08-22 14:45:55','2026-08-22 14:45:56'),(16,'PM-6C747523EA22','比赛回归：巡查校园禁停区和高风险车辆','屏风校区',110.3239130,25.2875179,720,'COMPLETED',100.00,99.76,1,0,1,'2026-08-22 15:31:19','2026-08-22 15:31:20'),(17,'PM-DB4F0DC8023B','比赛回归：巡查校园禁停区和高风险车辆','屏风校区',110.3239130,25.2875179,720,'COMPLETED',100.00,99.76,1,0,1,'2026-08-22 15:31:55','2026-08-22 15:31:56'),(18,'PM-666F296862ED','比赛回归：巡查校园禁停区和高风险车辆','屏风校区',110.3239130,25.2875179,720,'COMPLETED',100.00,99.76,1,0,1,'2026-08-22 15:32:30','2026-08-22 15:32:31'),(19,'PM-4CD267B8FD43','比赛回归：巡查校园禁停区和高风险车辆','屏风校区',110.3239130,25.2875179,720,'COMPLETED',100.00,99.76,1,0,1,'2026-08-22 15:32:59','2026-08-22 15:33:00'),(20,'PM-F94F8747403B','比赛回归：巡查校园禁停区和高风险车辆','屏风校区',110.3239130,25.2875179,720,'COMPLETED',100.00,99.76,1,0,1,'2026-08-22 15:33:35','2026-08-22 15:33:37'),(21,'PM-D5779E3E69B7','比赛回归：巡查校园禁停区和高风险车辆','屏风校区',110.3239130,25.2875179,720,'COMPLETED',100.00,99.76,1,0,1,'2026-08-22 15:34:06','2026-08-22 15:34:08'),(22,'PM-AC9B5BDCD951','请巡查屏风校区北门和宿舍区，重点识别禁停、未戴头盔以及疑似违规充电风险','屏风校区北门及宿舍区',112.6849000,26.8893000,300,'DRAFT',100.00,100.00,0,0,NULL,'2026-08-24 20:41:56','2026-08-24 20:41:56'),(23,'PM-B28B1FE5A0BE','请巡查屏风校区北门和宿舍区，重点识别禁停、未戴头盔以及疑似违规充电风险','屏风校区北门及宿舍区',110.3239130,25.2875180,300,'DRAFT',100.00,100.00,0,0,1,'2026-08-24 20:46:20','2026-08-24 20:46:20'),(24,'PM-E56A5B4324B7','巡查校园电动车高风险区域，发现异常后回传原AI事件中心','桂林理工大学屏风校区',110.3239130,25.2875179,720,'COMPLETED',100.00,94.12,1,0,1,'2026-09-10 18:59:39','2026-09-10 19:00:10');
/*!40000 ALTER TABLE `patrol_mission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `patrol_plan_change`
--

DROP TABLE IF EXISTS `patrol_plan_change`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `patrol_plan_change` (
  `change_id` bigint NOT NULL AUTO_INCREMENT,
  `mission_id` bigint NOT NULL,
  `from_version` int NOT NULL,
  `to_version` int NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `preserved_count` int NOT NULL DEFAULT '0',
  `removed_count` int NOT NULL DEFAULT '0',
  `added_count` int NOT NULL DEFAULT '0',
  `diff_json` longtext COLLATE utf8mb4_unicode_ci,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`change_id`),
  KEY `idx_patrol_plan_change_mission` (`mission_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patrol_plan_change`
--

LOCK TABLES `patrol_plan_change` WRITE;
/*!40000 ALTER TABLE `patrol_plan_change` DISABLE KEYS */;
INSERT INTO `patrol_plan_change` VALUES (1,7,1,2,'局部重规划',2,0,1,'{\"preservedWaypointIds\":[18,19],\"preservedSequenceEnd\":1,\"routeStart\":{\"lat\":25.287518,\"lng\":110.323913}}','2026-08-22 13:51:30');
/*!40000 ALTER TABLE `patrol_plan_change` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `patrol_risk_grid`
--

DROP TABLE IF EXISTS `patrol_risk_grid`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `patrol_risk_grid` (
  `grid_id` bigint NOT NULL AUTO_INCREMENT,
  `mission_id` bigint NOT NULL,
  `row_no` int NOT NULL,
  `col_no` int NOT NULL,
  `center_lng` decimal(11,7) NOT NULL,
  `center_lat` decimal(10,7) NOT NULL,
  `recent_score` decimal(8,4) NOT NULL DEFAULT '0.0000',
  `density_score` decimal(8,4) NOT NULL DEFAULT '0.0000',
  `fence_score` decimal(8,4) NOT NULL DEFAULT '0.0000',
  `blind_score` decimal(8,4) NOT NULL DEFAULT '0.0000',
  `risk_score` decimal(8,4) NOT NULL DEFAULT '0.0000',
  `selected` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`grid_id`),
  UNIQUE KEY `uk_patrol_grid` (`mission_id`,`row_no`,`col_no`)
) ENGINE=InnoDB AUTO_INCREMENT=501 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patrol_risk_grid`
--

LOCK TABLES `patrol_risk_grid` WRITE;
/*!40000 ALTER TABLE `patrol_risk_grid` DISABLE KEYS */;
INSERT INTO `patrol_risk_grid` VALUES (1,1,-2,-2,112.6844600,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(2,1,-2,-1,112.6846800,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(3,1,-2,0,112.6849000,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(4,1,-2,1,112.6851200,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(5,1,-2,2,112.6853400,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(6,1,-1,-2,112.6844600,26.8891200,0.0000,0.0000,0.0000,0.6500,0.1475,0),(7,1,-1,-1,112.6846800,26.8891200,0.0000,0.2000,0.0000,0.1500,0.1125,0),(8,1,-1,0,112.6849000,26.8891200,0.0000,0.2000,0.0000,0.1500,0.1125,0),(9,1,-1,1,112.6851200,26.8891200,0.0000,0.2000,0.0000,0.1500,0.1125,0),(10,1,-1,2,112.6853400,26.8891200,0.0000,0.0000,0.0000,0.6500,0.1475,0),(11,1,0,-2,112.6844600,26.8893000,0.0000,0.0000,0.0000,0.6500,0.1475,0),(12,1,0,-1,112.6846800,26.8893000,0.0000,0.2000,0.0000,0.1500,0.1125,0),(13,1,0,0,112.6849000,26.8893000,0.0000,0.2000,0.0000,0.1500,0.4125,1),(14,1,0,1,112.6851200,26.8893000,0.0000,0.2000,0.0000,0.1500,0.1125,0),(15,1,0,2,112.6853400,26.8893000,0.0000,0.0000,0.0000,0.6500,0.1475,0),(16,1,1,-2,112.6844600,26.8894800,0.0000,0.0000,0.0000,0.6500,0.1475,0),(17,1,1,-1,112.6846800,26.8894800,0.0000,0.2000,0.0000,0.1500,0.1125,0),(18,1,1,0,112.6849000,26.8894800,0.0000,0.2000,0.0000,0.1500,0.1125,0),(19,1,1,1,112.6851200,26.8894800,0.0000,0.2000,0.0000,0.1500,0.1125,0),(20,1,1,2,112.6853400,26.8894800,0.0000,0.0000,0.0000,0.6500,0.1475,0),(21,1,2,-2,112.6844600,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(22,1,2,-1,112.6846800,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(23,1,2,0,112.6849000,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(24,1,2,1,112.6851200,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(25,1,2,2,112.6853400,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(26,2,-2,-2,112.6844600,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(27,2,-2,-1,112.6846800,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(28,2,-2,0,112.6849000,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(29,2,-2,1,112.6851200,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(30,2,-2,2,112.6853400,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(31,2,-1,-2,112.6844600,26.8891200,0.0000,0.0000,0.0000,0.6500,0.1475,0),(32,2,-1,-1,112.6846800,26.8891200,0.0000,0.2000,0.0000,0.1500,0.1125,0),(33,2,-1,0,112.6849000,26.8891200,0.0000,0.2000,0.0000,0.1500,0.1125,0),(34,2,-1,1,112.6851200,26.8891200,0.0000,0.2000,0.0000,0.1500,0.1125,0),(35,2,-1,2,112.6853400,26.8891200,0.0000,0.0000,0.0000,0.6500,0.1475,0),(36,2,0,-2,112.6844600,26.8893000,0.0000,0.0000,0.0000,0.6500,0.1475,0),(37,2,0,-1,112.6846800,26.8893000,0.0000,0.2000,0.0000,0.1500,0.1125,0),(38,2,0,0,112.6849000,26.8893000,0.0000,0.2000,0.0000,0.1500,0.4125,1),(39,2,0,1,112.6851200,26.8893000,0.0000,0.2000,0.0000,0.1500,0.1125,0),(40,2,0,2,112.6853400,26.8893000,0.0000,0.0000,0.0000,0.6500,0.1475,0),(41,2,1,-2,112.6844600,26.8894800,0.0000,0.0000,0.0000,0.6500,0.1475,0),(42,2,1,-1,112.6846800,26.8894800,0.0000,0.2000,0.0000,0.1500,0.1125,0),(43,2,1,0,112.6849000,26.8894800,0.0000,0.2000,0.0000,0.1500,0.1125,0),(44,2,1,1,112.6851200,26.8894800,0.0000,0.2000,0.0000,0.1500,0.1125,0),(45,2,1,2,112.6853400,26.8894800,0.0000,0.0000,0.0000,0.6500,0.1475,0),(46,2,2,-2,112.6844600,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(47,2,2,-1,112.6846800,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(48,2,2,0,112.6849000,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(49,2,2,1,112.6851200,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(50,2,2,2,112.6853400,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(76,3,-2,-2,112.6844600,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(77,3,-2,-1,112.6846800,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(78,3,-2,0,112.6849000,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(79,3,-2,1,112.6851200,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(80,3,-2,2,112.6853400,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(81,3,-1,-2,112.6844600,26.8891200,0.0000,0.0000,0.0000,0.6500,0.1475,0),(82,3,-1,-1,112.6846800,26.8891200,0.3333,0.2000,0.0000,0.1500,0.2292,0),(83,3,-1,0,112.6849000,26.8891200,0.3333,0.2000,0.0000,0.1500,0.2292,0),(84,3,-1,1,112.6851200,26.8891200,0.3333,0.2000,0.0000,0.1500,0.2292,0),(85,3,-1,2,112.6853400,26.8891200,0.0000,0.0000,0.0000,0.6500,0.1475,0),(86,3,0,-2,112.6844600,26.8893000,0.0000,0.0000,0.0000,0.6500,0.1475,0),(87,3,0,-1,112.6846800,26.8893000,0.3333,0.2000,0.0000,0.1500,0.2292,0),(88,3,0,0,112.6849000,26.8893000,0.3333,0.2000,0.0000,0.1500,0.5292,1),(89,3,0,1,112.6851200,26.8893000,0.3333,0.2000,0.0000,0.1500,0.2292,0),(90,3,0,2,112.6853400,26.8893000,0.0000,0.0000,0.0000,0.6500,0.1475,0),(91,3,1,-2,112.6844600,26.8894800,0.0000,0.0000,0.0000,0.6500,0.1475,0),(92,3,1,-1,112.6846800,26.8894800,0.3333,0.2000,0.0000,0.1500,0.2292,0),(93,3,1,0,112.6849000,26.8894800,0.3333,0.2000,0.0000,0.1500,0.2292,0),(94,3,1,1,112.6851200,26.8894800,0.3333,0.2000,0.0000,0.1500,0.2292,0),(95,3,1,2,112.6853400,26.8894800,0.0000,0.0000,0.0000,0.6500,0.1475,0),(96,3,2,-2,112.6844600,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(97,3,2,-1,112.6846800,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(98,3,2,0,112.6849000,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(99,3,2,1,112.6851200,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(100,3,2,2,112.6853400,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(101,5,-2,-2,112.6844600,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(102,5,-2,-1,112.6846800,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(103,5,-2,0,112.6849000,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(104,5,-2,1,112.6851200,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(105,5,-2,2,112.6853400,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(106,5,-1,-2,112.6844600,26.8891200,0.0000,0.0000,0.0000,0.6500,0.1475,0),(107,5,-1,-1,112.6846800,26.8891200,0.6667,0.2000,0.0000,0.1500,0.3458,1),(108,5,-1,0,112.6849000,26.8891200,0.6667,0.2000,0.0000,0.1500,0.3458,1),(109,5,-1,1,112.6851200,26.8891200,0.6667,0.2000,0.0000,0.1500,0.3458,1),(110,5,-1,2,112.6853400,26.8891200,0.0000,0.0000,0.0000,0.6500,0.1475,0),(111,5,0,-2,112.6844600,26.8893000,0.0000,0.0000,0.0000,0.6500,0.1475,0),(112,5,0,-1,112.6846800,26.8893000,0.6667,0.2000,0.0000,0.1500,0.3458,1),(113,5,0,0,112.6849000,26.8893000,0.6667,0.2000,0.0000,0.1500,0.6458,1),(114,5,0,1,112.6851200,26.8893000,0.6667,0.2000,0.0000,0.1500,0.3458,1),(115,5,0,2,112.6853400,26.8893000,0.0000,0.0000,0.0000,0.6500,0.1475,0),(116,5,1,-2,112.6844600,26.8894800,0.0000,0.0000,0.0000,0.6500,0.1475,0),(117,5,1,-1,112.6846800,26.8894800,0.6667,0.2000,0.0000,0.1500,0.3458,1),(118,5,1,0,112.6849000,26.8894800,0.6667,0.2000,0.0000,0.1500,0.3458,1),(119,5,1,1,112.6851200,26.8894800,0.6667,0.2000,0.0000,0.1500,0.3458,1),(120,5,1,2,112.6853400,26.8894800,0.0000,0.0000,0.0000,0.6500,0.1475,0),(121,5,2,-2,112.6844600,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(122,5,2,-1,112.6846800,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(123,5,2,0,112.6849000,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(124,5,2,1,112.6851200,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(125,5,2,2,112.6853400,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(151,7,-2,-2,110.3234730,25.2871580,0.0000,0.0000,0.0000,0.6500,0.1475,0),(152,7,-2,-1,110.3236930,25.2871580,0.0000,0.0000,0.0000,0.6500,0.1475,0),(153,7,-2,0,110.3239130,25.2871580,0.0000,0.0000,0.0000,0.6500,0.1475,0),(154,7,-2,1,110.3241330,25.2871580,0.0000,0.0000,0.0000,0.6500,0.1475,0),(155,7,-2,2,110.3243530,25.2871580,0.0000,0.0000,0.0000,0.6500,0.1475,0),(156,7,-1,-2,110.3234730,25.2873380,0.0000,0.0000,0.0000,0.6500,0.1475,0),(157,7,-1,-1,110.3236930,25.2873380,0.0000,0.0000,0.0000,0.1500,0.0725,0),(158,7,-1,0,110.3239130,25.2873380,0.0000,0.0000,0.0000,0.1500,0.0725,0),(159,7,-1,1,110.3241330,25.2873380,0.0000,0.0000,0.0000,0.1500,0.0725,0),(160,7,-1,2,110.3243530,25.2873380,0.0000,0.0000,0.0000,0.6500,0.1475,0),(161,7,0,-2,110.3234730,25.2875180,0.0000,0.0000,0.0000,0.6500,0.1475,0),(162,7,0,-1,110.3236930,25.2875180,0.0000,0.0000,0.0000,0.1500,0.0725,0),(163,7,0,0,110.3239130,25.2875180,0.0000,0.0000,0.0000,0.1500,0.3725,1),(164,7,0,1,110.3241330,25.2875180,0.0000,0.0000,0.0000,0.1500,0.0725,0),(165,7,0,2,110.3243530,25.2875180,0.0000,0.0000,0.0000,0.6500,0.1475,0),(166,7,1,-2,110.3234730,25.2876980,0.0000,0.0000,0.0000,0.6500,0.1475,0),(167,7,1,-1,110.3236930,25.2876980,0.0000,0.0000,0.0000,0.1500,0.0725,0),(168,7,1,0,110.3239130,25.2876980,0.0000,0.0000,0.0000,0.1500,0.0725,0),(169,7,1,1,110.3241330,25.2876980,0.0000,0.0000,0.0000,0.1500,0.0725,0),(170,7,1,2,110.3243530,25.2876980,0.0000,0.0000,0.0000,0.6500,0.1475,0),(171,7,2,-2,110.3234730,25.2878780,0.0000,0.0000,0.0000,0.6500,0.1475,0),(172,7,2,-1,110.3236930,25.2878780,0.0000,0.0000,0.0000,0.6500,0.1475,0),(173,7,2,0,110.3239130,25.2878780,0.0000,0.0000,0.0000,0.6500,0.1475,0),(174,7,2,1,110.3241330,25.2878780,0.0000,0.0000,0.0000,0.6500,0.1475,0),(175,7,2,2,110.3243530,25.2878780,0.0000,0.0000,0.0000,0.6500,0.1475,0),(176,8,-2,-2,110.3234730,25.2871580,0.0000,0.0000,0.0000,0.6500,0.1475,0),(177,8,-2,-1,110.3236930,25.2871580,0.0000,0.0000,0.0000,0.6500,0.1475,0),(178,8,-2,0,110.3239130,25.2871580,0.0000,0.0000,0.0000,0.6500,0.1475,0),(179,8,-2,1,110.3241330,25.2871580,0.0000,0.0000,0.0000,0.6500,0.1475,0),(180,8,-2,2,110.3243530,25.2871580,0.0000,0.0000,0.0000,0.6500,0.1475,0),(181,8,-1,-2,110.3234730,25.2873380,0.0000,0.0000,0.0000,0.6500,0.1475,0),(182,8,-1,-1,110.3236930,25.2873380,0.0000,0.0000,0.0000,0.1500,0.0725,0),(183,8,-1,0,110.3239130,25.2873380,0.0000,0.0000,0.0000,0.1500,0.0725,0),(184,8,-1,1,110.3241330,25.2873380,0.0000,0.0000,0.0000,0.1500,0.0725,0),(185,8,-1,2,110.3243530,25.2873380,0.0000,0.0000,0.0000,0.6500,0.1475,0),(186,8,0,-2,110.3234730,25.2875180,0.0000,0.0000,0.0000,0.6500,0.1475,0),(187,8,0,-1,110.3236930,25.2875180,0.0000,0.0000,0.0000,0.1500,0.0725,0),(188,8,0,0,110.3239130,25.2875180,0.0000,0.0000,0.0000,0.1500,0.3725,1),(189,8,0,1,110.3241330,25.2875180,0.0000,0.0000,0.0000,0.1500,0.0725,0),(190,8,0,2,110.3243530,25.2875180,0.0000,0.0000,0.0000,0.6500,0.1475,0),(191,8,1,-2,110.3234730,25.2876980,0.0000,0.0000,0.0000,0.6500,0.1475,0),(192,8,1,-1,110.3236930,25.2876980,0.0000,0.0000,0.0000,0.1500,0.0725,0),(193,8,1,0,110.3239130,25.2876980,0.0000,0.0000,0.0000,0.1500,0.0725,0),(194,8,1,1,110.3241330,25.2876980,0.0000,0.0000,0.0000,0.1500,0.0725,0),(195,8,1,2,110.3243530,25.2876980,0.0000,0.0000,0.0000,0.6500,0.1475,0),(196,8,2,-2,110.3234730,25.2878780,0.0000,0.0000,0.0000,0.6500,0.1475,0),(197,8,2,-1,110.3236930,25.2878780,0.0000,0.0000,0.0000,0.6500,0.1475,0),(198,8,2,0,110.3239130,25.2878780,0.0000,0.0000,0.0000,0.6500,0.1475,0),(199,8,2,1,110.3241330,25.2878780,0.0000,0.0000,0.0000,0.6500,0.1475,0),(200,8,2,2,110.3243530,25.2878780,0.0000,0.0000,0.0000,0.6500,0.1475,0),(201,11,-2,-2,112.6844600,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(202,11,-2,-1,112.6846800,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(203,11,-2,0,112.6849000,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(204,11,-2,1,112.6851200,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(205,11,-2,2,112.6853400,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(206,11,-1,-2,112.6844600,26.8891200,0.0000,0.0000,0.0000,0.6500,0.1475,0),(207,11,-1,-1,112.6846800,26.8891200,1.0000,0.2000,0.0000,0.1500,0.4625,1),(208,11,-1,0,112.6849000,26.8891200,1.0000,0.2000,0.0000,0.1500,0.4625,1),(209,11,-1,1,112.6851200,26.8891200,1.0000,0.2000,0.0000,0.1500,0.4625,1),(210,11,-1,2,112.6853400,26.8891200,0.0000,0.0000,0.0000,0.6500,0.1475,0),(211,11,0,-2,112.6844600,26.8893000,0.0000,0.0000,0.0000,0.6500,0.1475,0),(212,11,0,-1,112.6846800,26.8893000,1.0000,0.2000,0.0000,0.1500,0.4625,1),(213,11,0,0,112.6849000,26.8893000,1.0000,0.2000,0.0000,0.1500,0.7625,1),(214,11,0,1,112.6851200,26.8893000,1.0000,0.2000,0.0000,0.1500,0.4625,1),(215,11,0,2,112.6853400,26.8893000,0.0000,0.0000,0.0000,0.6500,0.1475,0),(216,11,1,-2,112.6844600,26.8894800,0.0000,0.0000,0.0000,0.6500,0.1475,0),(217,11,1,-1,112.6846800,26.8894800,1.0000,0.2000,0.0000,0.1500,0.4625,1),(218,11,1,0,112.6849000,26.8894800,1.0000,0.2000,0.0000,0.1500,0.4625,1),(219,11,1,1,112.6851200,26.8894800,1.0000,0.2000,0.0000,0.1500,0.4625,1),(220,11,1,2,112.6853400,26.8894800,0.0000,0.0000,0.0000,0.6500,0.1475,0),(221,11,2,-2,112.6844600,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(222,11,2,-1,112.6846800,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(223,11,2,0,112.6849000,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(224,11,2,1,112.6851200,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(225,11,2,2,112.6853400,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(226,12,-2,-2,112.6844600,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(227,12,-2,-1,112.6846800,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(228,12,-2,0,112.6849000,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(229,12,-2,1,112.6851200,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(230,12,-2,2,112.6853400,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(231,12,-1,-2,112.6844600,26.8891200,0.0000,0.0000,0.0000,0.6500,0.1475,0),(232,12,-1,-1,112.6846800,26.8891200,1.0000,0.2000,0.0000,0.1500,0.4625,1),(233,12,-1,0,112.6849000,26.8891200,1.0000,0.2000,0.0000,0.1500,0.4625,1),(234,12,-1,1,112.6851200,26.8891200,1.0000,0.2000,0.0000,0.1500,0.4625,1),(235,12,-1,2,112.6853400,26.8891200,0.0000,0.0000,0.0000,0.6500,0.1475,0),(236,12,0,-2,112.6844600,26.8893000,0.0000,0.0000,0.0000,0.6500,0.1475,0),(237,12,0,-1,112.6846800,26.8893000,1.0000,0.2000,0.0000,0.1500,0.4625,1),(238,12,0,0,112.6849000,26.8893000,1.0000,0.2000,0.0000,0.1500,0.7625,1),(239,12,0,1,112.6851200,26.8893000,1.0000,0.2000,0.0000,0.1500,0.4625,1),(240,12,0,2,112.6853400,26.8893000,0.0000,0.0000,0.0000,0.6500,0.1475,0),(241,12,1,-2,112.6844600,26.8894800,0.0000,0.0000,0.0000,0.6500,0.1475,0),(242,12,1,-1,112.6846800,26.8894800,1.0000,0.2000,0.0000,0.1500,0.4625,1),(243,12,1,0,112.6849000,26.8894800,1.0000,0.2000,0.0000,0.1500,0.4625,1),(244,12,1,1,112.6851200,26.8894800,1.0000,0.2000,0.0000,0.1500,0.4625,1),(245,12,1,2,112.6853400,26.8894800,0.0000,0.0000,0.0000,0.6500,0.1475,0),(246,12,2,-2,112.6844600,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(247,12,2,-1,112.6846800,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(248,12,2,0,112.6849000,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(249,12,2,1,112.6851200,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(250,12,2,2,112.6853400,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(251,13,-2,-2,112.6844600,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(252,13,-2,-1,112.6846800,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(253,13,-2,0,112.6849000,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(254,13,-2,1,112.6851200,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(255,13,-2,2,112.6853400,26.8889400,0.0000,0.0000,0.0000,0.6500,0.1475,0),(256,13,-1,-2,112.6844600,26.8891200,0.0000,0.0000,0.0000,0.6500,0.1475,0),(257,13,-1,-1,112.6846800,26.8891200,1.0000,0.2000,0.0000,0.1500,0.4625,1),(258,13,-1,0,112.6849000,26.8891200,1.0000,0.2000,0.0000,0.1500,0.4625,1),(259,13,-1,1,112.6851200,26.8891200,1.0000,0.2000,0.0000,0.1500,0.4625,1),(260,13,-1,2,112.6853400,26.8891200,0.0000,0.0000,0.0000,0.6500,0.1475,0),(261,13,0,-2,112.6844600,26.8893000,0.0000,0.0000,0.0000,0.6500,0.1475,0),(262,13,0,-1,112.6846800,26.8893000,1.0000,0.2000,0.0000,0.1500,0.4625,1),(263,13,0,0,112.6849000,26.8893000,1.0000,0.2000,0.0000,0.1500,0.7625,1),(264,13,0,1,112.6851200,26.8893000,1.0000,0.2000,0.0000,0.1500,0.4625,1),(265,13,0,2,112.6853400,26.8893000,0.0000,0.0000,0.0000,0.6500,0.1475,0),(266,13,1,-2,112.6844600,26.8894800,0.0000,0.0000,0.0000,0.6500,0.1475,0),(267,13,1,-1,112.6846800,26.8894800,1.0000,0.2000,0.0000,0.1500,0.4625,1),(268,13,1,0,112.6849000,26.8894800,1.0000,0.2000,0.0000,0.1500,0.4625,1),(269,13,1,1,112.6851200,26.8894800,1.0000,0.2000,0.0000,0.1500,0.4625,1),(270,13,1,2,112.6853400,26.8894800,0.0000,0.0000,0.0000,0.6500,0.1475,0),(271,13,2,-2,112.6844600,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(272,13,2,-1,112.6846800,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(273,13,2,0,112.6849000,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(274,13,2,1,112.6851200,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(275,13,2,2,112.6853400,26.8896600,0.0000,0.0000,0.0000,0.6500,0.1475,0),(276,14,-2,-2,110.3234730,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(277,14,-2,-1,110.3236930,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(278,14,-2,0,110.3239130,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(279,14,-2,1,110.3241330,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(280,14,-2,2,110.3243530,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(281,14,-1,-2,110.3234730,25.2873379,0.0000,0.0000,0.0000,0.6500,0.1475,0),(282,14,-1,-1,110.3236930,25.2873379,0.0000,0.0000,0.0000,0.1500,0.0725,0),(283,14,-1,0,110.3239130,25.2873379,0.0000,0.0000,0.0000,0.1500,0.0725,0),(284,14,-1,1,110.3241330,25.2873379,0.0000,0.0000,0.0000,0.1500,0.0725,0),(285,14,-1,2,110.3243530,25.2873379,0.0000,0.0000,0.0000,0.6500,0.1475,0),(286,14,0,-2,110.3234730,25.2875179,0.0000,0.0000,0.0000,0.6500,0.1475,0),(287,14,0,-1,110.3236930,25.2875179,0.0000,0.0000,0.0000,0.1500,0.0725,0),(288,14,0,0,110.3239130,25.2875179,0.0000,0.0000,0.0000,0.1500,0.3725,1),(289,14,0,1,110.3241330,25.2875179,0.0000,0.0000,0.0000,0.1500,0.0725,0),(290,14,0,2,110.3243530,25.2875179,0.0000,0.0000,0.0000,0.6500,0.1475,0),(291,14,1,-2,110.3234730,25.2876979,0.0000,0.0000,0.0000,0.6500,0.1475,0),(292,14,1,-1,110.3236930,25.2876979,0.0000,0.0000,0.0000,0.1500,0.0725,0),(293,14,1,0,110.3239130,25.2876979,0.0000,0.0000,0.0000,0.1500,0.0725,0),(294,14,1,1,110.3241330,25.2876979,0.0000,0.0000,0.0000,0.1500,0.0725,0),(295,14,1,2,110.3243530,25.2876979,0.0000,0.0000,0.0000,0.6500,0.1475,0),(296,14,2,-2,110.3234730,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(297,14,2,-1,110.3236930,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(298,14,2,0,110.3239130,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(299,14,2,1,110.3241330,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(300,14,2,2,110.3243530,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(301,15,-2,-2,110.3234730,25.2871580,0.0000,0.0000,0.0000,0.6500,0.1475,0),(302,15,-2,-1,110.3236930,25.2871580,0.0000,0.0000,0.0000,0.6500,0.1475,0),(303,15,-2,0,110.3239130,25.2871580,0.0000,0.0000,0.0000,0.6500,0.1475,0),(304,15,-2,1,110.3241330,25.2871580,0.0000,0.0000,0.0000,0.6500,0.1475,0),(305,15,-2,2,110.3243530,25.2871580,0.0000,0.0000,0.0000,0.6500,0.1475,0),(306,15,-1,-2,110.3234730,25.2873380,0.0000,0.0000,0.0000,0.6500,0.1475,0),(307,15,-1,-1,110.3236930,25.2873380,0.3333,0.0000,0.0000,0.1500,0.1892,0),(308,15,-1,0,110.3239130,25.2873380,0.3333,0.0000,0.0000,0.1500,0.1892,0),(309,15,-1,1,110.3241330,25.2873380,0.3333,0.0000,0.0000,0.1500,0.1892,0),(310,15,-1,2,110.3243530,25.2873380,0.0000,0.0000,0.0000,0.6500,0.1475,0),(311,15,0,-2,110.3234730,25.2875180,0.0000,0.0000,0.0000,0.6500,0.1475,0),(312,15,0,-1,110.3236930,25.2875180,0.3333,0.0000,0.0000,0.1500,0.1892,0),(313,15,0,0,110.3239130,25.2875180,0.3333,0.0000,0.0000,0.1500,0.4892,1),(314,15,0,1,110.3241330,25.2875180,0.3333,0.0000,0.0000,0.1500,0.1892,0),(315,15,0,2,110.3243530,25.2875180,0.0000,0.0000,0.0000,0.6500,0.1475,0),(316,15,1,-2,110.3234730,25.2876980,0.0000,0.0000,0.0000,0.6500,0.1475,0),(317,15,1,-1,110.3236930,25.2876980,0.3333,0.0000,0.0000,0.1500,0.1892,0),(318,15,1,0,110.3239130,25.2876980,0.3333,0.0000,0.0000,0.1500,0.1892,0),(319,15,1,1,110.3241330,25.2876980,0.3333,0.0000,0.0000,0.1500,0.1892,0),(320,15,1,2,110.3243530,25.2876980,0.0000,0.0000,0.0000,0.6500,0.1475,0),(321,15,2,-2,110.3234730,25.2878780,0.0000,0.0000,0.0000,0.6500,0.1475,0),(322,15,2,-1,110.3236930,25.2878780,0.0000,0.0000,0.0000,0.6500,0.1475,0),(323,15,2,0,110.3239130,25.2878780,0.0000,0.0000,0.0000,0.6500,0.1475,0),(324,15,2,1,110.3241330,25.2878780,0.0000,0.0000,0.0000,0.6500,0.1475,0),(325,15,2,2,110.3243530,25.2878780,0.0000,0.0000,0.0000,0.6500,0.1475,0),(326,16,-2,-2,110.3234730,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(327,16,-2,-1,110.3236930,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(328,16,-2,0,110.3239130,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(329,16,-2,1,110.3241330,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(330,16,-2,2,110.3243530,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(331,16,-1,-2,110.3234730,25.2873379,0.0000,0.0000,0.0000,0.6500,0.1475,0),(332,16,-1,-1,110.3236930,25.2873379,0.6667,0.0000,0.0000,0.1500,0.3058,0),(333,16,-1,0,110.3239130,25.2873379,0.6667,0.0000,0.0000,0.1500,0.3058,0),(334,16,-1,1,110.3241330,25.2873379,0.6667,0.0000,0.0000,0.1500,0.3058,0),(335,16,-1,2,110.3243530,25.2873379,0.0000,0.0000,0.0000,0.6500,0.1475,0),(336,16,0,-2,110.3234730,25.2875179,0.0000,0.0000,0.0000,0.6500,0.1475,0),(337,16,0,-1,110.3236930,25.2875179,0.6667,0.0000,0.0000,0.1500,0.3058,0),(338,16,0,0,110.3239130,25.2875179,0.6667,0.0000,0.0000,0.1500,0.6058,1),(339,16,0,1,110.3241330,25.2875179,0.6667,0.0000,0.0000,0.1500,0.3058,0),(340,16,0,2,110.3243530,25.2875179,0.0000,0.0000,0.0000,0.6500,0.1475,0),(341,16,1,-2,110.3234730,25.2876979,0.0000,0.0000,0.0000,0.6500,0.1475,0),(342,16,1,-1,110.3236930,25.2876979,0.6667,0.0000,0.0000,0.1500,0.3058,0),(343,16,1,0,110.3239130,25.2876979,0.6667,0.0000,0.0000,0.1500,0.3058,0),(344,16,1,1,110.3241330,25.2876979,0.6667,0.0000,0.0000,0.1500,0.3058,0),(345,16,1,2,110.3243530,25.2876979,0.0000,0.0000,0.0000,0.6500,0.1475,0),(346,16,2,-2,110.3234730,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(347,16,2,-1,110.3236930,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(348,16,2,0,110.3239130,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(349,16,2,1,110.3241330,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(350,16,2,2,110.3243530,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(351,17,-2,-2,110.3234730,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(352,17,-2,-1,110.3236930,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(353,17,-2,0,110.3239130,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(354,17,-2,1,110.3241330,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(355,17,-2,2,110.3243530,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(356,17,-1,-2,110.3234730,25.2873379,0.0000,0.0000,0.0000,0.6500,0.1475,0),(357,17,-1,-1,110.3236930,25.2873379,0.6667,0.0000,0.0000,0.1500,0.3058,0),(358,17,-1,0,110.3239130,25.2873379,0.6667,0.0000,0.0000,0.1500,0.3058,0),(359,17,-1,1,110.3241330,25.2873379,0.6667,0.0000,0.0000,0.1500,0.3058,0),(360,17,-1,2,110.3243530,25.2873379,0.0000,0.0000,0.0000,0.6500,0.1475,0),(361,17,0,-2,110.3234730,25.2875179,0.0000,0.0000,0.0000,0.6500,0.1475,0),(362,17,0,-1,110.3236930,25.2875179,0.6667,0.0000,0.0000,0.1500,0.3058,0),(363,17,0,0,110.3239130,25.2875179,0.6667,0.0000,0.0000,0.1500,0.6058,1),(364,17,0,1,110.3241330,25.2875179,0.6667,0.0000,0.0000,0.1500,0.3058,0),(365,17,0,2,110.3243530,25.2875179,0.0000,0.0000,0.0000,0.6500,0.1475,0),(366,17,1,-2,110.3234730,25.2876979,0.0000,0.0000,0.0000,0.6500,0.1475,0),(367,17,1,-1,110.3236930,25.2876979,0.6667,0.0000,0.0000,0.1500,0.3058,0),(368,17,1,0,110.3239130,25.2876979,0.6667,0.0000,0.0000,0.1500,0.3058,0),(369,17,1,1,110.3241330,25.2876979,0.6667,0.0000,0.0000,0.1500,0.3058,0),(370,17,1,2,110.3243530,25.2876979,0.0000,0.0000,0.0000,0.6500,0.1475,0),(371,17,2,-2,110.3234730,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(372,17,2,-1,110.3236930,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(373,17,2,0,110.3239130,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(374,17,2,1,110.3241330,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(375,17,2,2,110.3243530,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(376,18,-2,-2,110.3234730,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(377,18,-2,-1,110.3236930,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(378,18,-2,0,110.3239130,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(379,18,-2,1,110.3241330,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(380,18,-2,2,110.3243530,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(381,18,-1,-2,110.3234730,25.2873379,0.0000,0.0000,0.0000,0.6500,0.1475,0),(382,18,-1,-1,110.3236930,25.2873379,0.6667,0.0000,0.0000,0.1500,0.3058,0),(383,18,-1,0,110.3239130,25.2873379,0.6667,0.0000,0.0000,0.1500,0.3058,0),(384,18,-1,1,110.3241330,25.2873379,0.6667,0.0000,0.0000,0.1500,0.3058,0),(385,18,-1,2,110.3243530,25.2873379,0.0000,0.0000,0.0000,0.6500,0.1475,0),(386,18,0,-2,110.3234730,25.2875179,0.0000,0.0000,0.0000,0.6500,0.1475,0),(387,18,0,-1,110.3236930,25.2875179,0.6667,0.0000,0.0000,0.1500,0.3058,0),(388,18,0,0,110.3239130,25.2875179,0.6667,0.0000,0.0000,0.1500,0.6058,1),(389,18,0,1,110.3241330,25.2875179,0.6667,0.0000,0.0000,0.1500,0.3058,0),(390,18,0,2,110.3243530,25.2875179,0.0000,0.0000,0.0000,0.6500,0.1475,0),(391,18,1,-2,110.3234730,25.2876979,0.0000,0.0000,0.0000,0.6500,0.1475,0),(392,18,1,-1,110.3236930,25.2876979,0.6667,0.0000,0.0000,0.1500,0.3058,0),(393,18,1,0,110.3239130,25.2876979,0.6667,0.0000,0.0000,0.1500,0.3058,0),(394,18,1,1,110.3241330,25.2876979,0.6667,0.0000,0.0000,0.1500,0.3058,0),(395,18,1,2,110.3243530,25.2876979,0.0000,0.0000,0.0000,0.6500,0.1475,0),(396,18,2,-2,110.3234730,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(397,18,2,-1,110.3236930,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(398,18,2,0,110.3239130,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(399,18,2,1,110.3241330,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(400,18,2,2,110.3243530,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(401,19,-2,-2,110.3234730,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(402,19,-2,-1,110.3236930,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(403,19,-2,0,110.3239130,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(404,19,-2,1,110.3241330,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(405,19,-2,2,110.3243530,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(406,19,-1,-2,110.3234730,25.2873379,0.0000,0.0000,0.0000,0.6500,0.1475,0),(407,19,-1,-1,110.3236930,25.2873379,0.6667,0.0000,0.0000,0.1500,0.3058,0),(408,19,-1,0,110.3239130,25.2873379,0.6667,0.0000,0.0000,0.1500,0.3058,0),(409,19,-1,1,110.3241330,25.2873379,0.6667,0.0000,0.0000,0.1500,0.3058,0),(410,19,-1,2,110.3243530,25.2873379,0.0000,0.0000,0.0000,0.6500,0.1475,0),(411,19,0,-2,110.3234730,25.2875179,0.0000,0.0000,0.0000,0.6500,0.1475,0),(412,19,0,-1,110.3236930,25.2875179,0.6667,0.0000,0.0000,0.1500,0.3058,0),(413,19,0,0,110.3239130,25.2875179,0.6667,0.0000,0.0000,0.1500,0.6058,1),(414,19,0,1,110.3241330,25.2875179,0.6667,0.0000,0.0000,0.1500,0.3058,0),(415,19,0,2,110.3243530,25.2875179,0.0000,0.0000,0.0000,0.6500,0.1475,0),(416,19,1,-2,110.3234730,25.2876979,0.0000,0.0000,0.0000,0.6500,0.1475,0),(417,19,1,-1,110.3236930,25.2876979,0.6667,0.0000,0.0000,0.1500,0.3058,0),(418,19,1,0,110.3239130,25.2876979,0.6667,0.0000,0.0000,0.1500,0.3058,0),(419,19,1,1,110.3241330,25.2876979,0.6667,0.0000,0.0000,0.1500,0.3058,0),(420,19,1,2,110.3243530,25.2876979,0.0000,0.0000,0.0000,0.6500,0.1475,0),(421,19,2,-2,110.3234730,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(422,19,2,-1,110.3236930,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(423,19,2,0,110.3239130,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(424,19,2,1,110.3241330,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(425,19,2,2,110.3243530,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(426,20,-2,-2,110.3234730,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(427,20,-2,-1,110.3236930,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(428,20,-2,0,110.3239130,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(429,20,-2,1,110.3241330,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(430,20,-2,2,110.3243530,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(431,20,-1,-2,110.3234730,25.2873379,0.0000,0.0000,0.0000,0.6500,0.1475,0),(432,20,-1,-1,110.3236930,25.2873379,0.6667,0.0000,0.0000,0.1500,0.3058,0),(433,20,-1,0,110.3239130,25.2873379,0.6667,0.0000,0.0000,0.1500,0.3058,0),(434,20,-1,1,110.3241330,25.2873379,0.6667,0.0000,0.0000,0.1500,0.3058,0),(435,20,-1,2,110.3243530,25.2873379,0.0000,0.0000,0.0000,0.6500,0.1475,0),(436,20,0,-2,110.3234730,25.2875179,0.0000,0.0000,0.0000,0.6500,0.1475,0),(437,20,0,-1,110.3236930,25.2875179,0.6667,0.0000,0.0000,0.1500,0.3058,0),(438,20,0,0,110.3239130,25.2875179,0.6667,0.0000,0.0000,0.1500,0.6058,1),(439,20,0,1,110.3241330,25.2875179,0.6667,0.0000,0.0000,0.1500,0.3058,0),(440,20,0,2,110.3243530,25.2875179,0.0000,0.0000,0.0000,0.6500,0.1475,0),(441,20,1,-2,110.3234730,25.2876979,0.0000,0.0000,0.0000,0.6500,0.1475,0),(442,20,1,-1,110.3236930,25.2876979,0.6667,0.0000,0.0000,0.1500,0.3058,0),(443,20,1,0,110.3239130,25.2876979,0.6667,0.0000,0.0000,0.1500,0.3058,0),(444,20,1,1,110.3241330,25.2876979,0.6667,0.0000,0.0000,0.1500,0.3058,0),(445,20,1,2,110.3243530,25.2876979,0.0000,0.0000,0.0000,0.6500,0.1475,0),(446,20,2,-2,110.3234730,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(447,20,2,-1,110.3236930,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(448,20,2,0,110.3239130,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(449,20,2,1,110.3241330,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(450,20,2,2,110.3243530,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(451,21,-2,-2,110.3234730,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(452,21,-2,-1,110.3236930,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(453,21,-2,0,110.3239130,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(454,21,-2,1,110.3241330,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(455,21,-2,2,110.3243530,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(456,21,-1,-2,110.3234730,25.2873379,0.0000,0.0000,0.0000,0.6500,0.1475,0),(457,21,-1,-1,110.3236930,25.2873379,0.6667,0.0000,0.0000,0.1500,0.3058,0),(458,21,-1,0,110.3239130,25.2873379,0.6667,0.0000,0.0000,0.1500,0.3058,0),(459,21,-1,1,110.3241330,25.2873379,0.6667,0.0000,0.0000,0.1500,0.3058,0),(460,21,-1,2,110.3243530,25.2873379,0.0000,0.0000,0.0000,0.6500,0.1475,0),(461,21,0,-2,110.3234730,25.2875179,0.0000,0.0000,0.0000,0.6500,0.1475,0),(462,21,0,-1,110.3236930,25.2875179,0.6667,0.0000,0.0000,0.1500,0.3058,0),(463,21,0,0,110.3239130,25.2875179,0.6667,0.0000,0.0000,0.1500,0.6058,1),(464,21,0,1,110.3241330,25.2875179,0.6667,0.0000,0.0000,0.1500,0.3058,0),(465,21,0,2,110.3243530,25.2875179,0.0000,0.0000,0.0000,0.6500,0.1475,0),(466,21,1,-2,110.3234730,25.2876979,0.0000,0.0000,0.0000,0.6500,0.1475,0),(467,21,1,-1,110.3236930,25.2876979,0.6667,0.0000,0.0000,0.1500,0.3058,0),(468,21,1,0,110.3239130,25.2876979,0.6667,0.0000,0.0000,0.1500,0.3058,0),(469,21,1,1,110.3241330,25.2876979,0.6667,0.0000,0.0000,0.1500,0.3058,0),(470,21,1,2,110.3243530,25.2876979,0.0000,0.0000,0.0000,0.6500,0.1475,0),(471,21,2,-2,110.3234730,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(472,21,2,-1,110.3236930,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(473,21,2,0,110.3239130,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(474,21,2,1,110.3241330,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(475,21,2,2,110.3243530,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(476,24,-2,-2,110.3234730,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(477,24,-2,-1,110.3236930,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(478,24,-2,0,110.3239130,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(479,24,-2,1,110.3241330,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(480,24,-2,2,110.3243530,25.2871579,0.0000,0.0000,0.0000,0.6500,0.1475,0),(481,24,-1,-2,110.3234730,25.2873379,0.0000,0.0000,0.0000,0.6500,0.1475,0),(482,24,-1,-1,110.3236930,25.2873379,1.0000,0.0000,0.0000,0.1500,0.4225,1),(483,24,-1,0,110.3239130,25.2873379,1.0000,0.0000,0.0000,0.1500,0.4225,1),(484,24,-1,1,110.3241330,25.2873379,1.0000,0.0000,0.0000,0.1500,0.4225,1),(485,24,-1,2,110.3243530,25.2873379,0.0000,0.0000,0.0000,0.6500,0.1475,0),(486,24,0,-2,110.3234730,25.2875179,0.0000,0.0000,0.0000,0.6500,0.1475,0),(487,24,0,-1,110.3236930,25.2875179,1.0000,0.0000,0.0000,0.1500,0.4225,1),(488,24,0,0,110.3239130,25.2875179,1.0000,0.0000,0.0000,0.1500,0.7225,1),(489,24,0,1,110.3241330,25.2875179,1.0000,0.0000,0.0000,0.1500,0.4225,1),(490,24,0,2,110.3243530,25.2875179,0.0000,0.0000,0.0000,0.6500,0.1475,0),(491,24,1,-2,110.3234730,25.2876979,0.0000,0.0000,0.0000,0.6500,0.1475,0),(492,24,1,-1,110.3236930,25.2876979,1.0000,0.0000,0.0000,0.1500,0.4225,1),(493,24,1,0,110.3239130,25.2876979,1.0000,0.0000,0.0000,0.1500,0.4225,1),(494,24,1,1,110.3241330,25.2876979,1.0000,0.0000,0.0000,0.1500,0.4225,1),(495,24,1,2,110.3243530,25.2876979,0.0000,0.0000,0.0000,0.6500,0.1475,0),(496,24,2,-2,110.3234730,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(497,24,2,-1,110.3236930,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(498,24,2,0,110.3239130,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(499,24,2,1,110.3241330,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0),(500,24,2,2,110.3243530,25.2878779,0.0000,0.0000,0.0000,0.6500,0.1475,0);
/*!40000 ALTER TABLE `patrol_risk_grid` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `patrol_waypoint`
--

DROP TABLE IF EXISTS `patrol_waypoint`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `patrol_waypoint` (
  `waypoint_id` bigint NOT NULL AUTO_INCREMENT,
  `mission_id` bigint NOT NULL,
  `sequence_no` int NOT NULL,
  `lng` decimal(11,7) NOT NULL,
  `lat` decimal(10,7) NOT NULL,
  `altitude_m` decimal(7,2) NOT NULL DEFAULT '30.00',
  `dwell_sec` int NOT NULL DEFAULT '3',
  `risk_score` decimal(8,4) NOT NULL DEFAULT '0.0000',
  `state` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PLANNED',
  PRIMARY KEY (`waypoint_id`),
  UNIQUE KEY `uk_patrol_waypoint` (`mission_id`,`sequence_no`),
  KEY `idx_patrol_waypoint_mission` (`mission_id`)
) ENGINE=InnoDB AUTO_INCREMENT=75 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `patrol_waypoint`
--

LOCK TABLES `patrol_waypoint` WRITE;
/*!40000 ALTER TABLE `patrol_waypoint` DISABLE KEYS */;
INSERT INTO `patrol_waypoint` VALUES (1,1,0,112.6849000,26.8893000,30.00,3,0.0000,'PLANNED'),(2,1,1,112.6849000,26.8893000,30.00,3,0.4125,'PLANNED'),(3,2,0,112.6849000,26.8893000,30.00,3,0.0000,'PLANNED'),(4,2,1,112.6849000,26.8893000,30.00,3,0.4125,'PLANNED'),(7,3,0,112.6849000,26.8893000,30.00,3,0.0000,'ARRIVED'),(8,3,1,112.6849000,26.8893000,30.00,3,0.5292,'ARRIVED'),(9,5,0,112.6849000,26.8893000,30.00,3,0.0000,'PLANNED'),(10,5,1,112.6849000,26.8893000,30.00,3,0.6458,'PLANNED'),(11,5,2,112.6849000,26.8891200,30.00,3,0.3458,'PLANNED'),(12,5,3,112.6846800,26.8891200,30.00,3,0.3458,'PLANNED'),(13,5,4,112.6846800,26.8893000,30.00,3,0.3458,'PLANNED'),(14,5,5,112.6846800,26.8894800,30.00,3,0.3458,'PLANNED'),(15,5,6,112.6849000,26.8894800,30.00,3,0.3458,'PLANNED'),(16,5,7,112.6851200,26.8893000,30.00,3,0.3458,'PLANNED'),(17,5,8,112.6851200,26.8891200,30.00,3,0.3458,'PLANNED'),(18,7,0,110.3239130,25.2875180,30.00,3,0.0000,'ARRIVED'),(19,7,1,110.3239130,25.2875180,30.00,3,0.3725,'ARRIVED'),(20,7,2,110.3239130,25.2875180,30.00,3,0.3725,'PLANNED'),(21,8,0,110.3239130,25.2875180,30.00,3,0.0000,'ARRIVED'),(22,8,1,110.3239130,25.2875180,30.00,3,0.3725,'ARRIVED'),(23,11,0,112.6849000,26.8893000,30.00,3,0.0000,'PLANNED'),(24,11,1,112.6849000,26.8893000,30.00,3,0.7625,'PLANNED'),(25,11,2,112.6849000,26.8891200,30.00,3,0.4625,'PLANNED'),(26,11,3,112.6846800,26.8891200,30.00,3,0.4625,'PLANNED'),(27,11,4,112.6846800,26.8893000,30.00,3,0.4625,'PLANNED'),(28,11,5,112.6846800,26.8894800,30.00,3,0.4625,'PLANNED'),(29,11,6,112.6849000,26.8894800,30.00,3,0.4625,'PLANNED'),(30,11,7,112.6851200,26.8893000,30.00,3,0.4625,'PLANNED'),(31,11,8,112.6851200,26.8891200,30.00,3,0.4625,'PLANNED'),(32,12,0,112.6849000,26.8893000,30.00,3,0.0000,'ARRIVED'),(33,12,1,112.6849000,26.8893000,30.00,3,0.7625,'ARRIVED'),(34,12,2,112.6849000,26.8891200,30.00,3,0.4625,'PLANNED'),(35,12,3,112.6846800,26.8891200,30.00,3,0.4625,'PLANNED'),(36,12,4,112.6846800,26.8893000,30.00,3,0.4625,'PLANNED'),(37,12,5,112.6846800,26.8894800,30.00,3,0.4625,'PLANNED'),(38,12,6,112.6849000,26.8894800,30.00,3,0.4625,'PLANNED'),(39,12,7,112.6851200,26.8893000,30.00,3,0.4625,'PLANNED'),(40,12,8,112.6851200,26.8891200,30.00,3,0.4625,'PLANNED'),(41,13,0,112.6849000,26.8893000,30.00,3,0.0000,'PLANNED'),(42,13,1,112.6849000,26.8893000,30.00,3,0.7625,'PLANNED'),(43,13,2,112.6849000,26.8891200,30.00,3,0.4625,'PLANNED'),(44,13,3,112.6846800,26.8891200,30.00,3,0.4625,'PLANNED'),(45,13,4,112.6846800,26.8893000,30.00,3,0.4625,'PLANNED'),(46,13,5,112.6846800,26.8894800,30.00,3,0.4625,'PLANNED'),(47,13,6,112.6849000,26.8894800,30.00,3,0.4625,'PLANNED'),(48,13,7,112.6851200,26.8893000,30.00,3,0.4625,'PLANNED'),(49,13,8,112.6851200,26.8891200,30.00,3,0.4625,'PLANNED'),(50,14,0,110.3239130,25.2875179,30.00,3,0.0000,'ARRIVED'),(51,14,1,110.3239130,25.2875179,30.00,3,0.3725,'ARRIVED'),(52,15,0,110.3239130,25.2875180,30.00,3,0.0000,'ARRIVED'),(53,15,1,110.3239130,25.2875180,30.00,3,0.4892,'ARRIVED'),(54,16,0,110.3239130,25.2875179,30.00,3,0.0000,'ARRIVED'),(55,16,1,110.3239130,25.2875179,30.00,3,0.6058,'ARRIVED'),(56,17,0,110.3239130,25.2875179,30.00,3,0.0000,'ARRIVED'),(57,17,1,110.3239130,25.2875179,30.00,3,0.6058,'ARRIVED'),(58,18,0,110.3239130,25.2875179,30.00,3,0.0000,'ARRIVED'),(59,18,1,110.3239130,25.2875179,30.00,3,0.6058,'ARRIVED'),(60,19,0,110.3239130,25.2875179,30.00,3,0.0000,'ARRIVED'),(61,19,1,110.3239130,25.2875179,30.00,3,0.6058,'ARRIVED'),(62,20,0,110.3239130,25.2875179,30.00,3,0.0000,'ARRIVED'),(63,20,1,110.3239130,25.2875179,30.00,3,0.6058,'ARRIVED'),(64,21,0,110.3239130,25.2875179,30.00,3,0.0000,'ARRIVED'),(65,21,1,110.3239130,25.2875179,30.00,3,0.6058,'ARRIVED'),(66,24,0,110.3239130,25.2875179,30.00,3,0.0000,'ARRIVED'),(67,24,1,110.3239130,25.2875179,30.00,3,0.7225,'ARRIVED'),(68,24,2,110.3239130,25.2873379,30.00,3,0.4225,'ARRIVED'),(69,24,3,110.3236930,25.2873379,30.00,3,0.4225,'ARRIVED'),(70,24,4,110.3236930,25.2875179,30.00,3,0.4225,'ARRIVED'),(71,24,5,110.3236930,25.2876979,30.00,3,0.4225,'ARRIVED'),(72,24,6,110.3239130,25.2876979,30.00,3,0.4225,'ARRIVED'),(73,24,7,110.3241330,25.2875179,30.00,3,0.4225,'ARRIVED'),(74,24,8,110.3241330,25.2873379,30.00,3,0.4225,'ARRIVED');
/*!40000 ALTER TABLE `patrol_waypoint` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `policy_chunk`
--

DROP TABLE IF EXISTS `policy_chunk`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `policy_chunk` (
  `chunk_id` bigint NOT NULL AUTO_INCREMENT,
  `document_id` bigint NOT NULL,
  `section_code` varchar(64) DEFAULT NULL,
  `heading` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `keywords` varchar(500) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`chunk_id`),
  KEY `idx_policy_chunk_document` (`document_id`),
  KEY `idx_policy_chunk_section` (`section_code`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='规则知识分段';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `policy_chunk`
--

LOCK TABLES `policy_chunk` WRITE;
/*!40000 ALTER TABLE `policy_chunk` DISABLE KEYS */;
INSERT INTO `policy_chunk` VALUES (1,1,'RULE-01','头盔与车牌','驾乘校园电动车应正确佩戴安全头盔，保持车牌清晰，不得遮挡或污损。','头盔,车牌,遮挡','2026-08-11 14:20:03'),(2,1,'RULE-02','道路通行','校园道路禁止违规载人、超速行驶；进入禁行区域须遵守电子围栏和现场标识。','载人,超速,禁行,围栏','2026-08-11 14:20:03'),(3,1,'RULE-03','车辆停放','车辆必须在划定区域有序停放，不得占用消防通道、疏散通道和建筑出入口。','停车,违停,消防通道','2026-08-11 14:20:03'),(4,1,'RULE-04','证据与人工复核','违规事件应具有可核验的时间、地点、车辆身份和图像或轨迹证据；证据不足时必须转人工复核。','证据,复核,身份,轨迹','2026-08-11 14:20:03'),(5,1,'RULE-05','处罚边界','正式扣分、处罚、车辆冻结和申诉结论必须由管理员确认，AI仅提供研判建议。','扣分,处罚,冻结,管理员','2026-08-11 14:20:03'),(6,1,'RULE-06','申诉权利','用户可在规定期限内提出申诉，并提交图片、视频、轨迹或其他证明材料。','申诉,材料,图片,视频','2026-08-11 14:20:03'),(7,1,'RULE-07','盗抢风险处置','疑似盗抢、异常移动事件应优先通知车主和安保人员，不直接作为交通违规扣分依据。','盗抢,异常移动,安保,通知','2026-08-11 14:20:03');
/*!40000 ALTER TABLE `policy_chunk` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `policy_document`
--

DROP TABLE IF EXISTS `policy_document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `policy_document` (
  `document_id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `category` varchar(64) NOT NULL,
  `version` varchar(32) DEFAULT NULL,
  `effective_date` date DEFAULT NULL,
  `content` longtext NOT NULL,
  `source` varchar(500) DEFAULT NULL,
  `status` varchar(24) NOT NULL DEFAULT 'ACTIVE',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`document_id`),
  KEY `idx_policy_status_category` (`status`,`category`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='校园治理规则知识库';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `policy_document`
--

LOCK TABLES `policy_document` WRITE;
/*!40000 ALTER TABLE `policy_document` DISABLE KEYS */;
INSERT INTO `policy_document` VALUES (1,'校园电动车安全治理规则（平台演示版）','CAMPUS_RULE','DEMO-1.0','2026-08-11','第一条 驾乘校园电动车应正确佩戴安全头盔，保持车牌清晰，不得遮挡或污损。\n第二条 校园道路禁止违规载人、超速行驶；进入教学核心区等禁行区域须遵守围栏和现场标识。\n第三条 电动车必须在划定区域有序停放，不得占用消防通道、疏散通道和建筑出入口。\n第四条 违规事件应具有可核验的时间、地点、车辆身份和图像或轨迹证据；证据不足时必须转人工复核。\n第五条 正式扣分、处罚、车辆冻结和申诉结论必须由管理员确认，AI仅提供研判建议。\n第六条 用户可在规定期限内对违规记录提出申诉，并提交图片、视频、轨迹或其他证明材料。\n第七条 对疑似盗抢、异常移动事件，应优先通知车主和安保人员，不直接作为交通违规扣分依据。\n','平台内置演示规则','ACTIVE','2026-08-11 14:20:03','2026-08-11 14:20:03');
/*!40000 ALTER TABLE `policy_document` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tool_execution`
--

DROP TABLE IF EXISTS `tool_execution`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tool_execution` (
  `execution_id` bigint NOT NULL AUTO_INCREMENT,
  `mission_id` bigint NOT NULL,
  `step_id` bigint DEFAULT NULL,
  `tool_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `input_summary` text COLLATE utf8mb4_unicode_ci,
  `output_summary` text COLLATE utf8mb4_unicode_ci,
  `state` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL,
  `duration_ms` bigint DEFAULT NULL,
  `retry_count` int NOT NULL DEFAULT '0',
  `error_message` text COLLATE utf8mb4_unicode_ci,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`execution_id`),
  KEY `idx_tool_execution_mission` (`mission_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=75 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tool_execution`
--

LOCK TABLES `tool_execution` WRITE;
/*!40000 ALTER TABLE `tool_execution` DISABLE KEYS */;
INSERT INTO `tool_execution` VALUES (1,1,NULL,'PARSE_PATROL_GOAL','{goal=验收东校区低空巡检链路, area=东校区}','{durationLimitSec=120}','SUCCEEDED',0,0,NULL,'2026-08-22 10:57:29'),(2,1,NULL,'PLAN_PATROL_ROUTE','{durationLimitSec=120, gridSizeMeters=20}','{waypointCount=2, hardConstraints=[禁飞区, 返航电量]}','SUCCEEDED',0,0,NULL,'2026-08-22 10:57:29'),(3,1,NULL,'START_SIMULATION','{missionId=1}','{telemetryIntervalMs=500, state=RUNNING}','SUCCEEDED',0,0,NULL,'2026-08-22 10:57:29'),(4,2,NULL,'PARSE_PATROL_GOAL','{area=东校区, goal=仿真遥测修复验收}','{durationLimitSec=120}','SUCCEEDED',0,0,NULL,'2026-08-22 10:59:16'),(5,2,NULL,'PLAN_PATROL_ROUTE','{gridSizeMeters=20, durationLimitSec=120}','{hardConstraints=[禁飞区, 返航电量], waypointCount=2}','SUCCEEDED',0,0,NULL,'2026-08-22 10:59:16'),(6,2,NULL,'START_SIMULATION','{missionId=2}','{state=RUNNING, telemetryIntervalMs=500}','SUCCEEDED',0,0,NULL,'2026-08-22 10:59:16'),(7,3,NULL,'PARSE_PATROL_GOAL','{area=东校区, goal=事件关联重规划验收}','{durationLimitSec=120}','SUCCEEDED',0,0,NULL,'2026-08-22 10:59:38'),(8,3,NULL,'PLAN_PATROL_ROUTE','{gridSizeMeters=20, durationLimitSec=120}','{hardConstraints=[禁飞区, 返航电量], waypointCount=2}','SUCCEEDED',0,0,NULL,'2026-08-22 10:59:38'),(9,3,NULL,'INGEST_PATROL_EVENT','{eventId=11, missionId=3}','{triggerReplan=true}','SUCCEEDED',0,0,NULL,'2026-08-22 10:59:38'),(10,3,NULL,'PLAN_PATROL_ROUTE','{gridSizeMeters=20, durationLimitSec=120}','{hardConstraints=[禁飞区, 返航电量], waypointCount=2}','SUCCEEDED',0,0,NULL,'2026-08-22 10:59:38'),(11,3,NULL,'REPLAN_ROUTE','{reason=发现巡检AI事件 11，自动插入重点复核航点}','{planVersion=2}','SUCCEEDED',0,0,NULL,'2026-08-22 10:59:38'),(12,4,NULL,'PARSE_PATROL_GOAL','{area=东校区, goal=Mock AI研判验收}','{durationLimitSec=120}','SUCCEEDED',0,0,NULL,'2026-08-22 11:00:34'),(13,3,NULL,'INGEST_PATROL_EVENT','{eventId=17, missionId=3}','{triggerReplan=false}','SUCCEEDED',0,0,NULL,'2026-08-22 11:10:20'),(14,5,NULL,'PARSE_PATROL_GOAL','{area=东校区, goal=比赛演示：低空巡检东校区头盔与禁行风险}','{durationLimitSec=120}','SUCCEEDED',0,0,NULL,'2026-08-22 11:17:00'),(15,5,NULL,'PLAN_PATROL_ROUTE','{durationLimitSec=120, gridSizeMeters=20}','{waypointCount=9, hardConstraints=[禁飞区, 返航电量]}','SUCCEEDED',0,0,NULL,'2026-08-22 11:17:00'),(16,5,NULL,'START_SIMULATION','{missionId=5}','{state=RUNNING, telemetryIntervalMs=500}','SUCCEEDED',0,0,NULL,'2026-08-22 11:17:00'),(17,5,NULL,'INGEST_PATROL_EVENT','{eventId=18, missionId=5}','{triggerReplan=false}','SUCCEEDED',0,0,NULL,'2026-08-22 11:17:05'),(18,6,NULL,'PARSE_PATROL_GOAL','{area=东校区, goal=巡查东校区晚高峰电动车安全情况，重点关注未戴头盔、禁行区和异常聚集。}','{durationLimitSec=720}','SUCCEEDED',0,0,NULL,'2026-08-22 11:53:06'),(19,3,NULL,'START_SIMULATION','{missionId=3}','{state=RUNNING, telemetryIntervalMs=500}','SUCCEEDED',0,0,NULL,'2026-08-22 13:40:51'),(20,7,NULL,'PARSE_PATROL_GOAL','{goal=应急巡查充电区域烟雾和禁停区，发现风险后复核, area=桂林理工大学屏风校区}','{plan={taskType=CAMPUS_EBIKE_PATROL, targetTypes=[ILLEGAL_PARKING, RESTRICTED_AREA, NO_HELMET, SMOKE_DETECTED], priority=CRITICAL, area=桂林理工大学屏风校区, durationLimitSec=720, sensorRequirements=[RGB_CAMERA, GPS, TELEMETRY], constraints=[ACTIVE_GEOFENCE, NO_FLY_ZONE, RETURN_BATTERY_25, HOME_RETURN], replanTriggers=[NEW_AI_EVENT, LOW_BATTERY, TEMPORARY_NO_FLY_ZONE, WAYPOINT_UNREACHABLE], executionSteps=[UNDERSTAND, OBSERVE, PLAN, EXECUTE, MONITOR, REPLAN, DELIVER], fallbackReason=HTTP 401}, provider=rule-fallback}','SUCCEEDED',1203,0,NULL,'2026-08-22 13:51:12'),(21,7,NULL,'PLAN_PATROL_ROUTE','{durationLimitSec=720, gridSizeMeters=20}','{waypointCount=2, hardConstraints=[禁飞区, 返航电量]}','SUCCEEDED',0,0,NULL,'2026-08-22 13:51:12'),(22,7,NULL,'START_SIMULATION','{missionId=7}','{telemetryIntervalMs=500, state=RUNNING}','SUCCEEDED',0,0,NULL,'2026-08-22 13:51:12'),(23,7,NULL,'INJECT_DRONE_FAULT','{parameters={meters=12}, faultType=GPS_OFFSET}','{accepted=true}','SUCCEEDED',0,0,NULL,'2026-08-22 13:51:12'),(24,7,NULL,'PLAN_PATROL_ROUTE','{durationLimitSec=720, gridSizeMeters=20}','{waypointCount=3, hardConstraints=[禁飞区, 返航电量]}','SUCCEEDED',0,0,NULL,'2026-08-22 13:51:30'),(25,7,NULL,'REPLAN_ROUTE','{reason=验证已执行航点保留和剩余航段调整}','{planVersion=2}','SUCCEEDED',0,0,NULL,'2026-08-22 13:51:30'),(26,8,NULL,'PARSE_PATROL_GOAL','{area=桂林理工大学屏风校区, goal=验证结构化巡检计划和故障恢复}','{provider=rule-fallback, plan={taskType=CAMPUS_EBIKE_PATROL, targetTypes=[ILLEGAL_PARKING, RESTRICTED_AREA, NO_HELMET], priority=HIGH, area=桂林理工大学屏风校区, durationLimitSec=600, sensorRequirements=[RGB_CAMERA, GPS, TELEMETRY], constraints=[ACTIVE_GEOFENCE, NO_FLY_ZONE, RETURN_BATTERY_25, HOME_RETURN], replanTriggers=[NEW_AI_EVENT, LOW_BATTERY, TEMPORARY_NO_FLY_ZONE, WAYPOINT_UNREACHABLE], executionSteps=[UNDERSTAND, OBSERVE, PLAN, EXECUTE, MONITOR, REPLAN, DELIVER], fallbackReason=HTTP 401}}','SUCCEEDED',1186,0,NULL,'2026-08-22 13:55:12'),(27,8,NULL,'PLAN_PATROL_ROUTE','{gridSizeMeters=20, durationLimitSec=600}','{hardConstraints=[禁飞区, 返航电量], waypointCount=2}','SUCCEEDED',0,0,NULL,'2026-08-22 13:55:12'),(28,8,NULL,'START_SIMULATION','{missionId=8}','{state=RUNNING, telemetryIntervalMs=500}','SUCCEEDED',0,0,NULL,'2026-08-22 13:55:12'),(29,8,NULL,'INJECT_DRONE_FAULT','{faultType=PACKET_LOSS, parameters={rate=0.5}}','{accepted=true}','SUCCEEDED',0,0,NULL,'2026-08-22 13:55:12'),(30,8,NULL,'CLEAR_DRONE_FAULT','{missionId=8}','{recovered=true}','SUCCEEDED',0,0,NULL,'2026-08-22 13:55:12'),(31,9,NULL,'PARSE_PATROL_GOAL','{goal=巡查校园电动车高风险区域, area=东校区}','{provider=rule-fallback, plan={taskType=CAMPUS_EBIKE_PATROL, targetTypes=[ILLEGAL_PARKING, RESTRICTED_AREA, NO_HELMET], priority=HIGH, area=东校区, durationLimitSec=720, sensorRequirements=[RGB_CAMERA, GPS, TELEMETRY], constraints=[ACTIVE_GEOFENCE, NO_FLY_ZONE, RETURN_BATTERY_25, HOME_RETURN], replanTriggers=[NEW_AI_EVENT, LOW_BATTERY, TEMPORARY_NO_FLY_ZONE, WAYPOINT_UNREACHABLE], executionSteps=[UNDERSTAND, OBSERVE, PLAN, EXECUTE, MONITOR, REPLAN, DELIVER], fallbackReason=HTTP 401}}','SUCCEEDED',1199,0,NULL,'2026-08-22 14:09:25'),(32,10,NULL,'PARSE_PATROL_GOAL','{goal=巡查校园电动车高风险区域, area=东校区}','{provider=rule-fallback, plan={taskType=CAMPUS_EBIKE_PATROL, targetTypes=[ILLEGAL_PARKING, RESTRICTED_AREA, NO_HELMET], priority=HIGH, area=东校区, durationLimitSec=720, sensorRequirements=[RGB_CAMERA, GPS, TELEMETRY], constraints=[ACTIVE_GEOFENCE, NO_FLY_ZONE, RETURN_BATTERY_25, HOME_RETURN], replanTriggers=[NEW_AI_EVENT, LOW_BATTERY, TEMPORARY_NO_FLY_ZONE, WAYPOINT_UNREACHABLE], executionSteps=[UNDERSTAND, OBSERVE, PLAN, EXECUTE, MONITOR, REPLAN, DELIVER], fallbackReason=HTTP 401}}','SUCCEEDED',979,0,NULL,'2026-08-22 14:09:37'),(33,11,NULL,'PARSE_PATROL_GOAL','{goal=巡查校园电动车高风险区域, area=东校区}','{provider=rule-fallback, plan={taskType=CAMPUS_EBIKE_PATROL, targetTypes=[ILLEGAL_PARKING, RESTRICTED_AREA, NO_HELMET], priority=HIGH, area=东校区, durationLimitSec=720, sensorRequirements=[RGB_CAMERA, GPS, TELEMETRY], constraints=[ACTIVE_GEOFENCE, NO_FLY_ZONE, RETURN_BATTERY_25, HOME_RETURN], replanTriggers=[NEW_AI_EVENT, LOW_BATTERY, TEMPORARY_NO_FLY_ZONE, WAYPOINT_UNREACHABLE], executionSteps=[UNDERSTAND, OBSERVE, PLAN, EXECUTE, MONITOR, REPLAN, DELIVER], fallbackReason=HTTP 401}}','SUCCEEDED',1040,0,NULL,'2026-08-22 14:09:59'),(34,11,NULL,'PLAN_PATROL_ROUTE','{gridSizeMeters=20, durationLimitSec=720}','{hardConstraints=[禁飞区, 返航电量], waypointCount=9}','SUCCEEDED',0,0,NULL,'2026-08-22 14:09:59'),(35,12,NULL,'PARSE_PATROL_GOAL','{goal=验证无人机运行与故障恢复, area=东校区}','{provider=rule-fallback, plan={taskType=CAMPUS_EBIKE_PATROL, targetTypes=[ILLEGAL_PARKING, RESTRICTED_AREA, NO_HELMET], priority=HIGH, area=东校区, durationLimitSec=720, sensorRequirements=[RGB_CAMERA, GPS, TELEMETRY], constraints=[ACTIVE_GEOFENCE, NO_FLY_ZONE, RETURN_BATTERY_25, HOME_RETURN], replanTriggers=[NEW_AI_EVENT, LOW_BATTERY, TEMPORARY_NO_FLY_ZONE, WAYPOINT_UNREACHABLE], executionSteps=[UNDERSTAND, OBSERVE, PLAN, EXECUTE, MONITOR, REPLAN, DELIVER], fallbackReason=HTTP 401}}','SUCCEEDED',1028,0,NULL,'2026-08-22 14:10:24'),(36,12,NULL,'PLAN_PATROL_ROUTE','{gridSizeMeters=20, durationLimitSec=720}','{hardConstraints=[禁飞区, 返航电量], waypointCount=9}','SUCCEEDED',0,0,NULL,'2026-08-22 14:10:24'),(37,12,NULL,'START_SIMULATION','{missionId=12}','{telemetryIntervalMs=500, state=RUNNING}','SUCCEEDED',0,0,NULL,'2026-08-22 14:10:24'),(38,12,NULL,'INJECT_DRONE_FAULT','{faultType=GPS_OFFSET, parameters={meters=12}}','{accepted=true}','SUCCEEDED',0,0,NULL,'2026-08-22 14:10:27'),(39,12,NULL,'CLEAR_DRONE_FAULT','{missionId=12}','{recovered=true}','SUCCEEDED',0,0,NULL,'2026-08-22 14:10:27'),(40,13,NULL,'PARSE_PATROL_GOAL','{area=东校区, goal=审计与报告接口验证}','{provider=rule-fallback, plan={taskType=CAMPUS_EBIKE_PATROL, targetTypes=[ILLEGAL_PARKING, RESTRICTED_AREA, NO_HELMET], priority=HIGH, area=东校区, durationLimitSec=720, sensorRequirements=[RGB_CAMERA, GPS, TELEMETRY], constraints=[ACTIVE_GEOFENCE, NO_FLY_ZONE, RETURN_BATTERY_25, HOME_RETURN], replanTriggers=[NEW_AI_EVENT, LOW_BATTERY, TEMPORARY_NO_FLY_ZONE, WAYPOINT_UNREACHABLE], executionSteps=[UNDERSTAND, OBSERVE, PLAN, EXECUTE, MONITOR, REPLAN, DELIVER]}}','SUCCEEDED',0,0,NULL,'2026-08-22 14:27:23'),(41,13,NULL,'PLAN_PATROL_ROUTE','{gridSizeMeters=20, durationLimitSec=720}','{waypointCount=9, hardConstraints=[禁飞区, 返航电量]}','SUCCEEDED',0,0,NULL,'2026-08-22 14:27:23'),(42,14,NULL,'PARSE_PATROL_GOAL','{goal=核查校园电动车高风险区域并回传统一AI事件, area=桂林理工大学屏风校区}','{plan={taskType=CAMPUS_EBIKE_PATROL, targetTypes=[ILLEGAL_PARKING, RESTRICTED_AREA, NO_HELMET], priority=HIGH, area=桂林理工大学屏风校区, durationLimitSec=720, sensorRequirements=[RGB_CAMERA, GPS, TELEMETRY], constraints=[ACTIVE_GEOFENCE, NO_FLY_ZONE, RETURN_BATTERY_25, HOME_RETURN], replanTriggers=[NEW_AI_EVENT, LOW_BATTERY, TEMPORARY_NO_FLY_ZONE, WAYPOINT_UNREACHABLE], executionSteps=[UNDERSTAND, OBSERVE, PLAN, EXECUTE, MONITOR, REPLAN, DELIVER]}, provider=rule-fallback}','SUCCEEDED',0,0,NULL,'2026-08-22 14:44:46'),(43,14,NULL,'PLAN_PATROL_ROUTE','{durationLimitSec=720, gridSizeMeters=20}','{hardConstraints=[禁飞区, 返航电量], waypointCount=2}','SUCCEEDED',0,0,NULL,'2026-08-22 14:44:47'),(44,14,NULL,'START_SIMULATION','{missionId=14}','{telemetryIntervalMs=500, state=RUNNING}','SUCCEEDED',0,0,NULL,'2026-08-22 14:44:47'),(45,14,NULL,'INGEST_PATROL_EVENT','{missionId=14, eventId=22}','{triggerReplan=false}','SUCCEEDED',0,0,NULL,'2026-08-22 14:44:50'),(46,15,NULL,'PARSE_PATROL_GOAL','{goal=E2E验收：原大屏巡检统一事件闭环, area=桂林理工大学屏风校区}','{plan={taskType=CAMPUS_EBIKE_PATROL, targetTypes=[ILLEGAL_PARKING, RESTRICTED_AREA, NO_HELMET], priority=HIGH, area=桂林理工大学屏风校区, durationLimitSec=720, sensorRequirements=[RGB_CAMERA, GPS, TELEMETRY], constraints=[ACTIVE_GEOFENCE, NO_FLY_ZONE, RETURN_BATTERY_25, HOME_RETURN], replanTriggers=[NEW_AI_EVENT, LOW_BATTERY, TEMPORARY_NO_FLY_ZONE, WAYPOINT_UNREACHABLE], executionSteps=[UNDERSTAND, OBSERVE, PLAN, EXECUTE, MONITOR, REPLAN, DELIVER]}, provider=rule-fallback}','SUCCEEDED',0,0,NULL,'2026-08-22 14:45:55'),(47,15,NULL,'PLAN_PATROL_ROUTE','{durationLimitSec=720, gridSizeMeters=20}','{hardConstraints=[禁飞区, 返航电量], waypointCount=2}','SUCCEEDED',0,0,NULL,'2026-08-22 14:45:55'),(48,15,NULL,'START_SIMULATION','{missionId=15}','{telemetryIntervalMs=500, state=RUNNING}','SUCCEEDED',0,0,NULL,'2026-08-22 14:45:55'),(49,15,NULL,'INGEST_PATROL_EVENT','{missionId=15, eventId=23}','{triggerReplan=false}','SUCCEEDED',0,0,NULL,'2026-08-22 14:45:56'),(50,16,NULL,'PARSE_PATROL_GOAL','{goal=比赛回归：巡查校园禁停区和高风险车辆, area=屏风校区}','{plan={taskType=CAMPUS_EBIKE_PATROL, targetTypes=[ILLEGAL_PARKING, RESTRICTED_AREA, NO_HELMET], priority=HIGH, area=屏风校区, durationLimitSec=720, sensorRequirements=[RGB_CAMERA, GPS, TELEMETRY], constraints=[ACTIVE_GEOFENCE, NO_FLY_ZONE, RETURN_BATTERY_25, HOME_RETURN], replanTriggers=[NEW_AI_EVENT, LOW_BATTERY, TEMPORARY_NO_FLY_ZONE, WAYPOINT_UNREACHABLE], executionSteps=[UNDERSTAND, OBSERVE, PLAN, EXECUTE, MONITOR, REPLAN, DELIVER]}, provider=rule-fallback}','SUCCEEDED',0,0,NULL,'2026-08-22 15:31:19'),(51,16,NULL,'PLAN_PATROL_ROUTE','{durationLimitSec=720, gridSizeMeters=20}','{hardConstraints=[禁飞区, 返航电量], waypointCount=2}','SUCCEEDED',0,0,NULL,'2026-08-22 15:31:19'),(52,16,NULL,'START_SIMULATION','{missionId=16}','{state=RUNNING, telemetryIntervalMs=500}','SUCCEEDED',0,0,NULL,'2026-08-22 15:31:19'),(53,17,NULL,'PARSE_PATROL_GOAL','{goal=比赛回归：巡查校园禁停区和高风险车辆, area=屏风校区}','{plan={taskType=CAMPUS_EBIKE_PATROL, targetTypes=[ILLEGAL_PARKING, RESTRICTED_AREA, NO_HELMET], priority=HIGH, area=屏风校区, durationLimitSec=720, sensorRequirements=[RGB_CAMERA, GPS, TELEMETRY], constraints=[ACTIVE_GEOFENCE, NO_FLY_ZONE, RETURN_BATTERY_25, HOME_RETURN], replanTriggers=[NEW_AI_EVENT, LOW_BATTERY, TEMPORARY_NO_FLY_ZONE, WAYPOINT_UNREACHABLE], executionSteps=[UNDERSTAND, OBSERVE, PLAN, EXECUTE, MONITOR, REPLAN, DELIVER]}, provider=rule-fallback}','SUCCEEDED',0,0,NULL,'2026-08-22 15:31:55'),(54,17,NULL,'PLAN_PATROL_ROUTE','{durationLimitSec=720, gridSizeMeters=20}','{hardConstraints=[禁飞区, 返航电量], waypointCount=2}','SUCCEEDED',0,0,NULL,'2026-08-22 15:31:55'),(55,17,NULL,'START_SIMULATION','{missionId=17}','{state=RUNNING, telemetryIntervalMs=500}','SUCCEEDED',0,0,NULL,'2026-08-22 15:31:55'),(56,18,NULL,'PARSE_PATROL_GOAL','{goal=比赛回归：巡查校园禁停区和高风险车辆, area=屏风校区}','{plan={taskType=CAMPUS_EBIKE_PATROL, targetTypes=[ILLEGAL_PARKING, RESTRICTED_AREA, NO_HELMET], priority=HIGH, area=屏风校区, durationLimitSec=720, sensorRequirements=[RGB_CAMERA, GPS, TELEMETRY], constraints=[ACTIVE_GEOFENCE, NO_FLY_ZONE, RETURN_BATTERY_25, HOME_RETURN], replanTriggers=[NEW_AI_EVENT, LOW_BATTERY, TEMPORARY_NO_FLY_ZONE, WAYPOINT_UNREACHABLE], executionSteps=[UNDERSTAND, OBSERVE, PLAN, EXECUTE, MONITOR, REPLAN, DELIVER]}, provider=rule-fallback}','SUCCEEDED',0,0,NULL,'2026-08-22 15:32:30'),(57,18,NULL,'PLAN_PATROL_ROUTE','{durationLimitSec=720, gridSizeMeters=20}','{hardConstraints=[禁飞区, 返航电量], waypointCount=2}','SUCCEEDED',0,0,NULL,'2026-08-22 15:32:30'),(58,18,NULL,'START_SIMULATION','{missionId=18}','{state=RUNNING, telemetryIntervalMs=500}','SUCCEEDED',0,0,NULL,'2026-08-22 15:32:30'),(59,19,NULL,'PARSE_PATROL_GOAL','{goal=比赛回归：巡查校园禁停区和高风险车辆, area=屏风校区}','{plan={taskType=CAMPUS_EBIKE_PATROL, targetTypes=[ILLEGAL_PARKING, RESTRICTED_AREA, NO_HELMET], priority=HIGH, area=屏风校区, durationLimitSec=720, sensorRequirements=[RGB_CAMERA, GPS, TELEMETRY], constraints=[ACTIVE_GEOFENCE, NO_FLY_ZONE, RETURN_BATTERY_25, HOME_RETURN], replanTriggers=[NEW_AI_EVENT, LOW_BATTERY, TEMPORARY_NO_FLY_ZONE, WAYPOINT_UNREACHABLE], executionSteps=[UNDERSTAND, OBSERVE, PLAN, EXECUTE, MONITOR, REPLAN, DELIVER]}, provider=rule-fallback}','SUCCEEDED',0,0,NULL,'2026-08-22 15:32:59'),(60,19,NULL,'PLAN_PATROL_ROUTE','{durationLimitSec=720, gridSizeMeters=20}','{hardConstraints=[禁飞区, 返航电量], waypointCount=2}','SUCCEEDED',0,0,NULL,'2026-08-22 15:32:59'),(61,19,NULL,'START_SIMULATION','{missionId=19}','{state=RUNNING, telemetryIntervalMs=500}','SUCCEEDED',0,0,NULL,'2026-08-22 15:32:59'),(62,20,NULL,'PARSE_PATROL_GOAL','{goal=比赛回归：巡查校园禁停区和高风险车辆, area=屏风校区}','{plan={taskType=CAMPUS_EBIKE_PATROL, targetTypes=[ILLEGAL_PARKING, RESTRICTED_AREA, NO_HELMET], priority=HIGH, area=屏风校区, durationLimitSec=720, sensorRequirements=[RGB_CAMERA, GPS, TELEMETRY], constraints=[ACTIVE_GEOFENCE, NO_FLY_ZONE, RETURN_BATTERY_25, HOME_RETURN], replanTriggers=[NEW_AI_EVENT, LOW_BATTERY, TEMPORARY_NO_FLY_ZONE, WAYPOINT_UNREACHABLE], executionSteps=[UNDERSTAND, OBSERVE, PLAN, EXECUTE, MONITOR, REPLAN, DELIVER]}, provider=rule-fallback}','SUCCEEDED',0,0,NULL,'2026-08-22 15:33:35'),(63,20,NULL,'PLAN_PATROL_ROUTE','{durationLimitSec=720, gridSizeMeters=20}','{hardConstraints=[禁飞区, 返航电量], waypointCount=2}','SUCCEEDED',0,0,NULL,'2026-08-22 15:33:35'),(64,20,NULL,'START_SIMULATION','{missionId=20}','{state=RUNNING, telemetryIntervalMs=500}','SUCCEEDED',0,0,NULL,'2026-08-22 15:33:35'),(65,21,NULL,'PARSE_PATROL_GOAL','{goal=比赛回归：巡查校园禁停区和高风险车辆, area=屏风校区}','{plan={taskType=CAMPUS_EBIKE_PATROL, targetTypes=[ILLEGAL_PARKING, RESTRICTED_AREA, NO_HELMET], priority=HIGH, area=屏风校区, durationLimitSec=720, sensorRequirements=[RGB_CAMERA, GPS, TELEMETRY], constraints=[ACTIVE_GEOFENCE, NO_FLY_ZONE, RETURN_BATTERY_25, HOME_RETURN], replanTriggers=[NEW_AI_EVENT, LOW_BATTERY, TEMPORARY_NO_FLY_ZONE, WAYPOINT_UNREACHABLE], executionSteps=[UNDERSTAND, OBSERVE, PLAN, EXECUTE, MONITOR, REPLAN, DELIVER]}, provider=rule-fallback}','SUCCEEDED',0,0,NULL,'2026-08-22 15:34:06'),(66,21,NULL,'PLAN_PATROL_ROUTE','{durationLimitSec=720, gridSizeMeters=20}','{hardConstraints=[禁飞区, 返航电量], waypointCount=2}','SUCCEEDED',0,0,NULL,'2026-08-22 15:34:06'),(67,21,NULL,'START_SIMULATION','{missionId=21}','{state=RUNNING, telemetryIntervalMs=500}','SUCCEEDED',0,0,NULL,'2026-08-22 15:34:06'),(68,21,NULL,'INGEST_PATROL_EVENT','{missionId=21, eventId=24}','{triggerReplan=false}','SUCCEEDED',0,0,NULL,'2026-08-22 15:34:06'),(69,22,NULL,'PARSE_PATROL_GOAL','{area=屏风校区北门及宿舍区, goal=请巡查屏风校区北门和宿舍区，重点识别禁停、未戴头盔以及疑似违规充电风险}','{durationLimitSec=300}','SUCCEEDED',0,0,NULL,'2026-08-24 20:41:56'),(70,23,NULL,'PARSE_PATROL_GOAL','{goal=请巡查屏风校区北门和宿舍区，重点识别禁停、未戴头盔以及疑似违规充电风险, area=屏风校区北门及宿舍区}','{plan={taskType=LOW_ALTITUDE_PATROL, targetTypes=[ILLEGAL_PARKING, NO_HELMET, THEFT_RISK], priority=HIGH, area=屏风校区北门及宿舍区, durationLimitSec=300, sensorRequirements=[RGB_CAMERA, THERMAL_CAMERA], constraints=[ACTIVE_GEOFENCE, RETURN_BATTERY_25, NO_FLY_ZONE_AVOIDANCE], replanTriggers=[BATTERY_LOW, GEOFENCE_VIOLATION, OBSTACLE_DETECTED], executionSteps=[TAKEOFF_FROM_NORTH_GATE, PATROL_NORTH_GATE_FOR_ILLEGAL_PARKING_AND_NO_HELMET, PROCEED_TO_DORMITORY_AREA, SCAN_FOR_THEFT_RISK_AND_ILLEGAL_PARKING, FOCUSED_INSPECTION_ON_SUSPECTED_CHARGING_VIOLATIONS_WITH_THERMAL, RETURN_TO_BASE_WITH_BATTERY_RESERVE_25_PERCENT]}, provider=openai-compatible}','SUCCEEDED',3081,0,NULL,'2026-08-24 20:46:23'),(71,23,NULL,'INGEST_PATROL_EVENT','{missionId=23, eventId=25}','{triggerReplan=false}','SUCCEEDED',0,0,NULL,'2026-08-24 20:47:24'),(72,24,NULL,'PARSE_PATROL_GOAL','{goal=巡查校园电动车高风险区域，发现异常后回传原AI事件中心, area=桂林理工大学屏风校区}','{plan={taskType=校园低空巡检, targetTypes=[NO_HELMET, OVERLOAD, ILLEGAL_PARKING, RESTRICTED_AREA, SMOKE_DETECTED, FIRE_DETECTED, THEFT_RISK], priority=HIGH, area=桂林理工大学屏风校区, durationLimitSec=720, sensorRequirements=[可见光相机, 红外热成像, 实时图传], constraints={ACTIVE_GEOFENCE=true, RETURN_BATTERY_25=true, noFlyZones=[教学楼顶, 图书馆上方, 人员密集区]}, replanTriggers=[发现火情或烟雾, 电池电量低于30%, 强风或降雨, GPS信号丢失, 禁飞区冲突], executionSteps=[解析任务，加载校园地图与禁飞区数据, 设定巡检航线，覆盖电动车停放区、充电棚、主要道路, 起飞前检查电池与传感器状态，确保电量>30%, 按航线自动巡检，实时AI识别目标事件, 发现异常事件，记录位置并回传至原AI事件中心, 持续监控电池，若低于25%自动返航, 完成巡检或触发返航后，安全降落并上传数据]}, provider=openai-compatible}','SUCCEEDED',2390,0,NULL,'2026-09-10 18:59:41'),(73,24,NULL,'PLAN_PATROL_ROUTE','{durationLimitSec=720, gridSizeMeters=20}','{hardConstraints=[禁飞区, 返航电量], waypointCount=9}','SUCCEEDED',0,0,NULL,'2026-09-10 18:59:41'),(74,24,NULL,'START_SIMULATION','{missionId=24}','{state=RUNNING, telemetryIntervalMs=500}','SUCCEEDED',0,0,NULL,'2026-09-10 18:59:41');
/*!40000 ALTER TABLE `tool_execution` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `user_id` int NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '密码',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `user_type` varchar(20) NOT NULL COMMENT '用户类型: student, teacher, service, admin',
  `student_id` varchar(20) DEFAULT NULL COMMENT '学号',
  `job_id` varchar(20) DEFAULT NULL COMMENT '工号',
  `phone` varchar(20) NOT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `department` varchar(100) DEFAULT NULL COMMENT '院系/部门',
  `college` varchar(64) DEFAULT NULL COMMENT '学院',
  `major` varchar(64) DEFAULT NULL COMMENT '专业',
  `class_name` varchar(128) DEFAULT NULL COMMENT '班级名称',
  `status` varchar(20) DEFAULT 'active' COMMENT '状态: active, frozen, deleted',
  `license_points` int NOT NULL DEFAULT '12' COMMENT '剩余记分(满分12)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_username` (`username`),
  KEY `idx_phone` (`phone`),
  KEY `idx_user_type` (`user_type`)
) ENGINE=InnoDB AUTO_INCREMENT=163 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'admin','$2a$10$HlOL7hV7mOtwymt1B5renuKAY9GzOXerDowau6xhkFkkJutJ5aaRq','管理员','admin',NULL,NULL,'13800138000','admin@ev.com',NULL,'计算机学院',NULL,NULL,NULL,'active',12,'2026-02-07 21:18:28','2026-03-27 12:37:56',0),(2,'linhao','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','林浩','student','20231001',NULL,'13800138001','linhao@campus.edu.cn',NULL,'计算机学院',NULL,NULL,NULL,'active',9,'2026-02-07 21:18:28','2026-05-03 16:30:29',0),(3,'xucheng','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTV0UiC','许诚','teacher',NULL,NULL,'13800138002','xucheng@campus.edu.cn',NULL,'计算机学院',NULL,NULL,NULL,'active',12,'2026-02-07 21:18:28','2026-05-03 15:36:47',0),(4,'guojiaqi','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTV0UiC','郭佳琪','service',NULL,NULL,'13800138003','guojiaqi@campus.edu.cn',NULL,'保卫处',NULL,NULL,NULL,'active',9,'2026-02-07 21:18:28','2026-05-03 15:36:47',0),(5,'songyue','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','宋悦','student','20231002',NULL,'13800138004','songyue@campus.edu.cn',NULL,'电子工程学院',NULL,NULL,NULL,'active',6,'2026-02-07 21:18:28','2026-05-03 16:30:29',0),(6,'sunqi','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTV0UiC','孙七','teacher',NULL,NULL,'13800138005','sunqi@ev.com',NULL,'机械工程学院',NULL,NULL,NULL,'active',6,'2026-02-07 21:18:28','2026-02-07 21:18:28',0),(7,'zhouba','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTV0UiC','周八','service',NULL,NULL,'13800138006','zhouba@ev.com',NULL,'后勤保障部',NULL,NULL,NULL,'active',12,'2026-02-07 21:18:28','2026-02-07 21:18:28',0),(8,'yyy','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','彭淇垚','student','23123453',NULL,'17570950339','3341742487@qq.com',NULL,'计算机学院',NULL,NULL,NULL,'active',8,'2026-02-10 14:06:50','2026-05-03 16:30:29',0),(9,'骑马耶夫','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','李五','student','13423454',NULL,'13770949009','',NULL,'',NULL,NULL,NULL,'active',12,'2026-02-21 15:48:51','2026-05-03 16:30:29',0),(10,'sdc ','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','零全新','student','2134546',NULL,'17870987990','',NULL,'',NULL,NULL,NULL,'active',12,'2026-02-21 16:34:25','2026-05-03 16:30:29',0),(11,'skylqx','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','零全新','student','2134546',NULL,'17870987996','',NULL,'',NULL,NULL,NULL,'active',12,'2026-02-21 16:35:10','2026-05-03 16:30:29',0),(12,'asdfd ','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','洛碛西','student','324567',NULL,'18737680093','',NULL,'',NULL,NULL,NULL,'active',12,'2026-02-21 16:39:26','2026-05-03 16:30:29',0),(13,'lssd','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','两千三','student','4356',NULL,'17838904330','',NULL,'',NULL,NULL,NULL,'active',12,'2026-02-21 16:49:31','2026-05-03 16:30:29',0),(16,'skykk','$2a$10$930lhQj84sbfmG7rrbMYC.WhFcA2WXQrPqhMwi462E9z4.8dS5sN6','彭淇亚','student','2323342',NULL,'17570950999','',NULL,'计算机学院',NULL,NULL,NULL,'active',12,'2026-02-24 16:20:24','2026-03-21 11:27:48',1),(17,'李广文','$2a$10$KXYkALRDHITbDUjuJbTGPeAMBLWkLNY0IcBARnGVXZT8k15GpN/S.','李广文','student','27890934',NULL,'17678900009','',NULL,'',NULL,NULL,NULL,'active',12,'2026-03-20 23:38:32','2026-03-21 15:39:02',1),(18,'李广文2','$2a$10$g5KDWaNIKmu2JMn4A9PYjObazr2UL.pPhJQKR5RmERGisSxQA0EpK','李广文2','student','27890934',NULL,'17670930229','',NULL,'',NULL,NULL,NULL,'active',12,'2026-03-20 23:39:35','2026-03-21 11:08:54',1),(19,'李广文3','$2a$10$9ngNK297Kc31bTE.ZVVdBu7abK9xYZ2mFywrYXcy0O0yi7m/KjdbS','李广文2','student','24320987',NULL,'17876560998','',NULL,'',NULL,NULL,NULL,'active',12,'2026-03-20 23:58:23','2026-03-21 11:08:52',1),(20,'skypqy','$2a$10$Y0hoSomSTp9v2OemFKMnLOxga974v.sgb3kMZRW2EyY3pPSvsr246','李广文','student','24980431',NULL,'17870970779','',NULL,'',NULL,NULL,NULL,'active',12,'2026-03-21 00:00:20','2026-03-21 11:27:54',1),(25,'李广文5','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','李广文5','student','3454345',NULL,'19789097889','',NULL,'',NULL,NULL,NULL,'active',12,'2026-03-21 15:46:09','2026-05-03 16:30:29',0),(30,'董吉林','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','董吉林','student','24890834',NULL,'17890930228','',NULL,'','计算机科学与技术学院','计算机科学与技术','4班','active',12,'2026-03-21 17:41:21','2026-05-03 16:30:29',0),(31,'sim2024_001','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','陈思源','student','202424001',NULL,'13900000001','sim2024_001@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术1班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(32,'sim2024_002','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','李雨桐','student','202424002',NULL,'13900000002','sim2024_002@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术1班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(33,'sim2024_003','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','王子轩','student','202424003',NULL,'13900000003','sim2024_003@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术1班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(34,'sim2024_004','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','赵梓涵','student','202424004',NULL,'13900000004','sim2024_004@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术1班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(35,'sim2024_005','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','刘一诺','student','202424005',NULL,'13900000005','sim2024_005@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术1班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(36,'sim2024_006','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','张梓萱','student','202424006',NULL,'13900000006','sim2024_006@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术1班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(37,'sim2024_007','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','黄思睿','student','202424007',NULL,'13900000007','sim2024_007@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术1班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(38,'sim2024_008','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','周子豪','student','202424008',NULL,'13900000008','sim2024_008@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术1班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(39,'sim2024_009','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','吴若曦','student','202424009',NULL,'13900000009','sim2024_009@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术1班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(40,'sim2024_010','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','郑浩然','student','202424010',NULL,'13900000010','sim2024_010@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术1班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(41,'sim2024_011','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','孙语汐','student','202424011',NULL,'13900000011','sim2024_011@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术1班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(42,'sim2024_012','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','胡景行','student','202424012',NULL,'13900000012','sim2024_012@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术1班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(43,'sim2024_013','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','朱思颖','student','202424013',NULL,'13900000013','sim2024_013@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术2班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(44,'sim2024_014','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','高子墨','student','202424014',NULL,'13900000014','sim2024_014@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术2班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(45,'sim2024_015','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','林妙可','student','202424015',NULL,'13900000015','sim2024_015@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术2班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(46,'sim2024_016','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','何子涵','student','202424016',NULL,'13900000016','sim2024_016@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术2班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(47,'sim2024_017','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','郭诗雨','student','202424017',NULL,'13900000017','sim2024_017@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术2班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(48,'sim2024_018','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','罗宇轩','student','202424018',NULL,'13900000018','sim2024_018@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术2班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(49,'sim2024_019','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','梁心怡','student','202424019',NULL,'13900000019','sim2024_019@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术2班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(50,'sim2024_020','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','谢卓然','student','202424020',NULL,'13900000020','sim2024_020@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术2班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(51,'sim2024_021','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','唐婉清','student','202424021',NULL,'13900000021','sim2024_021@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术2班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(52,'sim2024_022','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','韩云飞','student','202424022',NULL,'13900000022','sim2024_022@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术2班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(53,'sim2024_023','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','冯子瑜','student','202424023',NULL,'13900000023','sim2024_023@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术2班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(54,'sim2024_024','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','于思源','student','202424024',NULL,'13900000024','sim2024_024@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术2班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(55,'sim2024_025','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','董佳宁','student','202424025',NULL,'13900000025','sim2024_025@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术3班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(56,'sim2024_026','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','薛文博','student','202424026',NULL,'13900000026','sim2024_026@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术3班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(57,'sim2024_027','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','程若彤','student','202424027',NULL,'13900000027','sim2024_027@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术3班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(58,'sim2024_028','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','邓景铄','student','202424028',NULL,'13900000028','sim2024_028@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术3班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(59,'sim2024_029','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','曾子衿','student','202424029',NULL,'13900000029','sim2024_029@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术3班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(60,'sim2024_030','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','彭嘉怡','student','202424030',NULL,'13900000030','sim2024_030@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术3班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(61,'sim2024_031','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','苏文昊','student','202424031',NULL,'13900000031','sim2024_031@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术3班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(62,'sim2024_032','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','叶思齐','student','202424032',NULL,'13900000032','sim2024_032@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术3班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(63,'sim2024_033','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','阎子睿','student','202424033',NULL,'13900000033','sim2024_033@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术3班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(64,'sim2024_034','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','余瑾萱','student','202424034',NULL,'13900000034','sim2024_034@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术3班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(65,'sim2024_035','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','潘俊逸','student','202424035',NULL,'13900000035','sim2024_035@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术3班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(66,'sim2024_036','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','杜雨泽','student','202424036',NULL,'13900000036','sim2024_036@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术3班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(67,'sim2024_037','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','戴思琪','student','202424037',NULL,'13900000037','sim2024_037@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术3班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(68,'sim2024_038','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','夏子安','student','202424038',NULL,'13900000038','sim2024_038@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术4班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(69,'sim2024_039','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','钟灵秀','student','202424039',NULL,'13900000039','sim2024_039@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术4班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(70,'sim2024_040','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','汪文博','student','202424040',NULL,'13900000040','sim2024_040@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术4班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(71,'sim2024_041','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','田思远','student','202424041',NULL,'13900000041','sim2024_041@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术4班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(72,'sim2024_042','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','任子墨','student','202424042',NULL,'13900000042','sim2024_042@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术4班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(73,'sim2024_043','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','姜雨萌','student','202424043',NULL,'13900000043','sim2024_043@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术4班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(74,'sim2024_044','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','方景曜','student','202424044',NULL,'13900000044','sim2024_044@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术4班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(75,'sim2024_045','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','白若溪','student','202424045',NULL,'13900000045','sim2024_045@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术4班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(76,'sim2024_046','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','崔子轩','student','202424046',NULL,'13900000046','sim2024_046@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术4班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(77,'sim2024_047','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','龚雨霏','student','202424047',NULL,'13900000047','sim2024_047@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术4班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(78,'sim2024_048','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','邵文博','student','202424048',NULL,'13900000048','sim2024_048@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术4班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(79,'sim2024_049','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','孟思涵','student','202424049',NULL,'13900000049','sim2024_049@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术4班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(80,'sim2024_050','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','彭淇垚','student','202424050',NULL,'13900000050','sim2024_050@campus.edu.cn',NULL,'2024级计算机科学与技术学院','2024级计算机科学与技术学院','计算机科学与技术','计算机科学与技术4班','active',12,'2026-03-22 23:52:28','2026-05-03 16:30:29',0),(81,'彭淇垚_旧_81','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','彭淇垚','student','24890832',NULL,'17809290339','3341742487@qq.com',NULL,NULL,'软件学院','软件工程','软件工程1班','active',12,'2026-04-28 20:59:53','2026-05-03 16:30:29',0),(92,'彭淇垚','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','彭淇垚','student','202425001',NULL,'13866010001','demo_stu_001@campus.edu.cn',NULL,'Info Engineering College','Info Engineering College','Computer Science','CS2401','active',10,'2026-05-01 17:19:38','2026-05-03 16:30:29',0),(93,'demo_stu_002','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','陈思源','student','202425002',NULL,'13866010002','demo_stu_002@campus.edu.cn',NULL,'Info Engineering College','Info Engineering College','Computer Science','CS2401','active',10,'2026-05-01 17:19:38','2026-05-03 16:30:29',0),(94,'demo_stu_003','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','李雨桐','student','202425003',NULL,'13866010003','demo_stu_003@campus.edu.cn',NULL,'Info Engineering College','Info Engineering College','Computer Science','CS2401','active',10,'2026-05-01 17:19:38','2026-05-03 16:30:29',0),(95,'demo_stu_004','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','王子涵','student','202425004',NULL,'13866010004','demo_stu_004@campus.edu.cn',NULL,'Info Engineering College','Info Engineering College','Computer Science','CS2402','active',10,'2026-05-01 17:19:38','2026-05-03 16:30:29',0),(96,'demo_stu_005','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','刘昊然','student','202425005',NULL,'13866010005','demo_stu_005@campus.edu.cn',NULL,'Info Engineering College','Info Engineering College','Computer Science','CS2402','active',10,'2026-05-01 17:19:38','2026-05-03 16:30:29',0),(97,'demo_stu_006','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','张佳怡','student','202425006',NULL,'13866010006','demo_stu_006@campus.edu.cn',NULL,'Info Engineering College','Info Engineering College','Computer Science','CS2402','active',10,'2026-05-01 17:19:38','2026-05-03 16:30:29',0),(98,'demo_stu_007','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','周俊杰','student','202425007',NULL,'13866010007','demo_stu_007@campus.edu.cn',NULL,'Software College','Software College','Software Engineering','SE2401','active',10,'2026-05-01 17:19:38','2026-05-03 16:30:29',0),(99,'demo_stu_008','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','黄梓涵','student','202425008',NULL,'13866010008','demo_stu_008@campus.edu.cn',NULL,'Software College','Software College','Software Engineering','SE2401','active',10,'2026-05-01 17:19:38','2026-05-03 16:30:29',0),(100,'demo_stu_009','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','赵一鸣','student','202425009',NULL,'13866010009','demo_stu_009@campus.edu.cn',NULL,'AI College','AI College','Artificial Intelligence','AI2401','active',10,'2026-05-01 17:19:38','2026-05-03 16:30:29',0),(101,'demo_stu_010','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','吴雨薇','student','202425010',NULL,'13866010010','demo_stu_010@campus.edu.cn',NULL,'AI College','AI College','Artificial Intelligence','AI2401','active',10,'2026-05-01 17:19:38','2026-05-03 16:30:29',0),(152,'guide_stu_001','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','陈思源','student','20260503001',NULL,'13870010001','guide_stu_001@campus.edu.cn',NULL,'信息工程学院','信息工程学院','计算机科学与技术','计科2401班','active',10,'2026-05-03 22:38:33','2026-05-03 22:38:33',0),(153,'guide_stu_002','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','李雨桐','student','20260503002',NULL,'13870010002','guide_stu_002@campus.edu.cn',NULL,'信息工程学院','信息工程学院','计算机科学与技术','计科2401班','active',10,'2026-05-03 22:38:33','2026-05-03 22:38:33',0),(154,'guide_stu_003','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','王子涵','student','20260503003',NULL,'13870010003','guide_stu_003@campus.edu.cn',NULL,'信息工程学院','信息工程学院','计算机科学与技术','计科2401班','active',10,'2026-05-03 22:38:33','2026-05-03 22:38:33',0),(155,'guide_stu_004','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','赵一鸣','student','20260503004',NULL,'13870010004','guide_stu_004@campus.edu.cn',NULL,'信息工程学院','信息工程学院','计算机科学与技术','计科2402班','active',10,'2026-05-03 22:38:33','2026-05-03 22:38:33',0),(156,'guide_stu_005','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','刘浩然','student','20260503005',NULL,'13870010005','guide_stu_005@campus.edu.cn',NULL,'信息工程学院','信息工程学院','计算机科学与技术','计科2402班','active',10,'2026-05-03 22:38:33','2026-05-03 22:38:33',0),(157,'guide_stu_006','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','张佳怡','student','20260503006',NULL,'13870010006','guide_stu_006@campus.edu.cn',NULL,'软件学院','软件学院','软件工程','软工2401班','active',10,'2026-05-03 22:38:33','2026-05-03 22:38:33',0),(158,'guide_stu_007','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','周俊杰','student','20260503007',NULL,'13870010007','guide_stu_007@campus.edu.cn',NULL,'软件学院','软件学院','软件工程','软工2401班','active',10,'2026-05-03 22:38:33','2026-05-03 22:38:33',0),(159,'guide_stu_008','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','黄梓涵','student','20260503008',NULL,'13870010008','guide_stu_008@campus.edu.cn',NULL,'人工智能学院','人工智能学院','人工智能','智科2401班','active',10,'2026-05-03 22:38:33','2026-05-03 22:38:33',0),(160,'guide_stu_009','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','吴雨薇','student','20260503009',NULL,'13870010009','guide_stu_009@campus.edu.cn',NULL,'人工智能学院','人工智能学院','人工智能','智科2401班','active',10,'2026-05-03 22:38:33','2026-05-03 22:38:33',0),(161,'guide_stu_010','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','彭启耀','student','20260503010',NULL,'13870010010','guide_stu_010@campus.edu.cn',NULL,'人工智能学院','人工智能学院','人工智能','智科2401班','active',10,'2026-05-03 22:38:33','2026-05-03 22:38:33',0),(162,'link_demo_001','$2a$10$G9oVjrneNefi.w1GPU/VgO4x0hj5fYok2fn8UcxU6mD6bkKHDl5mK','林小满','student','2026081401',NULL,'13866018801','linxiaoman@campus.edu.cn',NULL,'信息工程学院','信息工程学院','计算机科学与技术','计科2401班','active',10,'2026-07-15 08:52:43','2026-08-14 08:52:43',0);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_notification`
--

DROP TABLE IF EXISTS `user_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_notification` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `notification_id` int NOT NULL COMMENT '通知ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `is_read` tinyint(1) DEFAULT '0' COMMENT '是否已读',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_user` (`notification_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `user_notification_ibfk_1` FOREIGN KEY (`notification_id`) REFERENCES `notification` (`notification_id`) ON DELETE CASCADE,
  CONSTRAINT `user_notification_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户通知阅读记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_notification`
--

LOCK TABLES `user_notification` WRITE;
/*!40000 ALTER TABLE `user_notification` DISABLE KEYS */;
INSERT INTO `user_notification` VALUES (1,1,2,1,'2024-01-15 10:35:00','2026-02-07 21:18:29'),(2,2,2,0,NULL,'2026-02-07 21:18:29'),(3,3,2,1,'2024-01-14 09:15:00','2026-02-07 21:18:29'),(4,4,2,0,NULL,'2026-02-07 21:18:29'),(5,5,2,1,'2024-01-12 11:30:00','2026-02-07 21:18:29'),(6,1,3,1,'2024-01-15 10:40:00','2026-02-07 21:18:29'),(7,3,3,0,NULL,'2026-02-07 21:18:29'),(8,4,3,1,'2024-01-13 16:00:00','2026-02-07 21:18:29'),(9,5,3,1,'2024-01-12 12:00:00','2026-02-07 21:18:29'),(10,91,162,0,NULL,'2026-08-12 08:52:43');
/*!40000 ALTER TABLE `user_notification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vehicle_illegal_record`
--

DROP TABLE IF EXISTS `vehicle_illegal_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vehicle_illegal_record` (
  `illegal_id` int NOT NULL AUTO_INCREMENT COMMENT '违规ID',
  `vehicle_id` int DEFAULT NULL COMMENT '车辆ID',
  `user_id` int DEFAULT NULL COMMENT '用户ID',
  `illegal_type` int NOT NULL COMMENT '违规类型: 1违规停放,2超速行驶,3违规充电,4未登记车辆,5进入禁行区,6违规载人,7遮挡车牌,8其他违规',
  `illegal_lng` decimal(10,6) DEFAULT NULL COMMENT '违规经度',
  `illegal_lat` decimal(10,6) DEFAULT NULL COMMENT '违规纬度',
  `illegal_time` datetime NOT NULL COMMENT '违规时间',
  `deduction_points` int DEFAULT '0' COMMENT '扣分数',
  `points_deducted` int NOT NULL DEFAULT '0' COMMENT '本条实际扣除记分',
  `evidence_id` int DEFAULT NULL COMMENT '证据ID',
  `illegal_source` int DEFAULT '1' COMMENT '违规来源: 1无人机巡检,2定位模块,3人工举报',
  `illegal_state` int DEFAULT '0' COMMENT '违规状态: 0待整改,1整改中,2已整改,3逾期未整改,4申诉中',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  `evidence_url` varchar(500) DEFAULT NULL COMMENT '违规证据图片URL',
  PRIMARY KEY (`illegal_id`),
  KEY `user_id` (`user_id`),
  KEY `evidence_id` (`evidence_id`),
  KEY `idx_illegal_time` (`illegal_time`),
  KEY `idx_illegal_type` (`illegal_type`),
  KEY `idx_illegal_state` (`illegal_state`),
  KEY `idx_vehicle_id` (`vehicle_id`),
  KEY `idx_vir_illegal_time` (`illegal_time` DESC),
  CONSTRAINT `vehicle_illegal_record_ibfk_1` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle_info` (`vehicle_id`) ON DELETE SET NULL,
  CONSTRAINT `vehicle_illegal_record_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL,
  CONSTRAINT `vehicle_illegal_record_ibfk_3` FOREIGN KEY (`evidence_id`) REFERENCES `drone_evidence` (`evidence_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆违规记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vehicle_illegal_record`
--

LOCK TABLES `vehicle_illegal_record` WRITE;
/*!40000 ALTER TABLE `vehicle_illegal_record` DISABLE KEYS */;
INSERT INTO `vehicle_illegal_record` VALUES (1,1,2,1,112.625456,26.901234,'2024-01-15 14:25:00',2,0,1,1,1,'2026-02-07 21:18:28','2026-02-07 21:18:28',0,NULL),(2,2,3,2,112.627891,26.903456,'2024-01-15 10:15:00',3,0,2,1,1,'2026-02-07 21:18:28','2026-02-07 21:18:28',0,NULL),(3,NULL,NULL,4,112.623456,26.899012,'2024-01-14 08:30:00',5,0,NULL,3,2,'2026-02-07 21:18:28','2026-02-07 21:18:28',0,NULL),(4,4,4,3,112.626789,26.902345,'2024-01-13 18:45:00',4,0,3,1,4,'2026-02-07 21:18:28','2026-02-07 21:18:28',0,NULL),(5,5,5,1,112.624123,26.900456,'2024-01-16 09:30:00',2,0,NULL,2,1,'2026-02-07 21:18:28','2026-02-07 21:18:28',0,NULL),(6,6,6,5,112.625789,26.901789,'2024-01-17 14:45:00',3,0,NULL,1,1,'2026-02-07 21:18:28','2026-02-07 21:18:28',0,NULL),(7,1,2,6,112.626456,26.902123,'2024-01-18 11:20:00',2,0,NULL,3,1,'2026-02-07 21:18:28','2026-02-07 21:18:28',0,NULL),(8,2,3,7,112.624789,26.900789,'2024-01-19 16:10:00',4,0,NULL,1,1,'2026-02-07 21:18:28','2026-02-07 21:18:28',0,NULL),(9,2,3,1,112.682763,26.886096,'2026-02-12 07:41:00',2,0,NULL,3,0,'2026-02-12 15:41:53','2026-02-12 15:41:53',0,NULL),(10,8,8,1,112.682763,26.886096,'2026-02-21 08:53:00',2,0,NULL,3,1,'2026-02-21 16:54:00','2026-02-21 16:54:00',0,'/uploads/image/2026/02/21/21320963ec20487883920f29cf3e2635.jpg'),(11,8,8,1,114.300000,30.500000,'2026-03-27 16:57:08',2,2,NULL,2,0,'2026-03-27 16:57:08','2026-03-27 16:57:08',0,NULL),(23,4,4,1,112.684763,26.888371,'2026-05-01 14:54:12',3,3,NULL,2,0,'2026-05-01 14:54:12','2026-05-01 14:54:12',0,NULL),(24,5,5,1,112.684913,26.889371,'2026-05-01 14:54:12',3,3,NULL,2,0,'2026-05-01 14:54:12','2026-05-01 14:54:12',0,NULL),(25,6,6,1,112.694863,26.882371,'2026-05-01 14:54:12',3,3,NULL,2,0,'2026-05-01 14:54:12','2026-05-01 14:54:12',0,NULL),(26,23,92,1,112.682763,26.886096,'2026-04-21 17:19:38',2,2,NULL,2,0,'2026-05-01 17:19:38','2026-05-01 17:20:19',0,'/demo/evidence/violation-001.jpg'),(27,24,93,2,112.682763,26.886096,'2026-04-22 17:19:38',2,2,NULL,2,0,'2026-05-01 17:19:38','2026-05-01 17:20:19',0,'/demo/evidence/violation-002.jpg'),(28,25,94,3,112.682763,26.886096,'2026-04-23 17:19:39',2,2,NULL,2,0,'2026-05-01 17:19:39','2026-05-01 17:20:19',0,'/demo/evidence/violation-003.jpg'),(29,26,95,5,112.682763,26.886096,'2026-04-24 17:19:39',2,2,NULL,2,0,'2026-05-01 17:19:39','2026-05-01 17:20:19',0,'/demo/evidence/violation-004.jpg'),(30,27,96,6,112.682763,26.886096,'2026-04-25 17:19:39',2,2,NULL,2,0,'2026-05-01 17:19:39','2026-05-01 17:20:19',0,'/demo/evidence/violation-005.jpg'),(31,28,97,7,112.682763,26.886096,'2026-04-26 17:19:39',2,2,NULL,2,0,'2026-05-01 17:19:39','2026-05-01 17:20:19',0,'/demo/evidence/violation-006.jpg'),(32,29,98,1,112.682763,26.886096,'2026-04-27 17:19:39',2,2,NULL,2,0,'2026-05-01 17:19:39','2026-05-01 17:20:19',0,'/demo/evidence/violation-007.jpg'),(33,30,99,3,112.682763,26.886096,'2026-04-28 17:19:39',2,2,NULL,2,0,'2026-05-01 17:19:39','2026-05-01 17:20:19',0,'/demo/evidence/violation-008.jpg'),(34,31,100,5,112.682763,26.886096,'2026-04-29 17:19:39',2,2,NULL,2,0,'2026-05-01 17:19:39','2026-05-01 17:20:19',0,'/demo/evidence/violation-009.jpg'),(35,32,101,8,112.682763,26.886096,'2026-04-30 17:19:39',2,2,NULL,2,0,'2026-05-01 17:19:39','2026-05-01 17:20:19',0,'/demo/evidence/violation-010.jpg'),(36,12,30,1,112.686763,26.889821,'2026-05-02 12:13:17',3,0,NULL,2,5,'2026-05-02 12:13:17','2026-05-02 12:13:17',0,NULL),(37,8,8,1,112.685000,26.885761,'2026-05-02 12:13:17',3,3,NULL,2,0,'2026-05-02 12:13:17','2026-05-02 12:13:17',0,NULL),(38,3,2,1,112.683213,26.889371,'2026-05-02 12:13:17',3,3,NULL,2,0,'2026-05-02 12:13:17','2026-05-02 12:13:17',0,NULL),(39,5,5,1,112.684913,26.889371,'2026-05-02 12:13:17',3,3,NULL,2,0,'2026-05-02 12:13:17','2026-05-02 12:13:17',0,NULL),(40,6,6,1,112.694863,26.882371,'2026-05-02 12:13:17',3,3,NULL,2,0,'2026-05-02 12:13:17','2026-05-02 12:13:17',0,NULL),(41,43,162,9,113.006211,28.142386,'2026-08-12 08:52:43',2,2,NULL,1,2,'2026-08-12 08:52:43','2026-08-14 08:52:43',0,'/demo/evidence/chain-no-helmet.jpg');
/*!40000 ALTER TABLE `vehicle_illegal_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vehicle_info`
--

DROP TABLE IF EXISTS `vehicle_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vehicle_info` (
  `vehicle_id` int NOT NULL AUTO_INCREMENT COMMENT '车辆ID',
  `user_id` int NOT NULL COMMENT '车主ID',
  `license_plate` varchar(20) NOT NULL COMMENT '车牌号',
  `brand` varchar(50) NOT NULL COMMENT '品牌',
  `color` varchar(20) NOT NULL COMMENT '颜色',
  `frame_num` varchar(50) NOT NULL COMMENT '车架号',
  `battery_type` varchar(20) DEFAULT NULL COMMENT '电池类型',
  `vehicle_type` varchar(20) DEFAULT 'electric' COMMENT '车辆类型: electric, car, motorcycle',
  `vehicle_photo` varchar(255) DEFAULT NULL COMMENT '车辆照片',
  `status` varchar(20) DEFAULT 'pending' COMMENT '状态: pending, approved, rejected, expired, frozen',
  `is_frozen` tinyint(1) DEFAULT '0' COMMENT '是否冻结',
  `is_blacklist` tinyint(1) DEFAULT '0' COMMENT '是否黑名单',
  `register_time` datetime DEFAULT NULL COMMENT '注册时间',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_user_id` int DEFAULT NULL COMMENT '审核人ID',
  `valid_date` date DEFAULT NULL COMMENT '有效期至',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`vehicle_id`),
  UNIQUE KEY `license_plate` (`license_plate`),
  UNIQUE KEY `frame_num` (`frame_num`),
  KEY `idx_license_plate` (`license_plate`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `vehicle_info_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vehicle_info`
--

LOCK TABLES `vehicle_info` WRITE;
/*!40000 ALTER TABLE `vehicle_info` DISABLE KEYS */;
INSERT INTO `vehicle_info` VALUES (1,2,'湘A0533319','雅迪','白色','YD20230123456','锂电池','electric',NULL,'approved',1,0,'2024-01-15 10:30:00','2024-01-15 14:20:00',1,'2025-01-15','2026-02-07 21:18:28','2026-05-03 15:46:42',1),(2,3,'湘A0533320','小牛','黑色','XN20230123457','铅酸电池','electric','/uploads/vehicle/2026/02/12/05bc01737dc84e4c80083c3573d9e60d.png','approved',1,1,'2024-01-16 09:15:00','2024-01-16 11:30:00',1,'2025-01-16','2026-02-07 21:18:28','2026-05-03 15:46:42',0),(3,2,'湘A0533321','爱玛','红色','AM20230123458','锂电池','electric','/uploads/vehicle/2026/02/12/ce7e11aabad8477b8757012eac1600a2.png','approved',1,0,'2024-01-18 14:20:00','2026-02-09 14:32:30',1,'2027-02-09','2026-02-07 21:18:28','2026-05-03 15:46:42',0),(4,4,'湘A0533322','新日','蓝色','XR20230123459','铅酸电池','electric','/uploads/vehicle/2026/02/21/8c4501c6a83a453985ce6b9ccf5172ee.png','approved',1,0,'2024-01-10 08:45:00','2024-01-10 10:15:00',1,'2025-01-10','2026-02-07 21:18:28','2026-05-03 15:46:42',0),(5,5,'湘A0533323','台铃','灰色','TL20230123460','锂电池','electric','/uploads/vehicle/2026/03/21/fc4e6998e3bd413fb01dd91550ffe675.jpg','approved',0,0,'2024-01-12 13:20:00','2024-01-12 15:40:00',1,'2025-01-12','2026-02-07 21:18:28','2026-05-03 15:46:42',0),(6,6,'湘A0533324','绿源','绿色','LY20230123461','铅酸电池','electric',NULL,'approved',1,0,'2024-01-14 11:10:00','2024-01-14 14:30:00',1,'2025-01-14','2026-02-07 21:18:28','2026-05-03 15:46:42',0),(7,7,'湘A0533325','九号','黄色','JH20230123462','锂电池','electric',NULL,'rejected',0,0,'2024-01-17 16:45:00','2024-01-17 17:30:00',1,NULL,'2026-02-07 21:18:28','2026-05-03 15:46:42',1),(8,8,'湘A0533315','其他','白色','2432222','锂电池','电动自行车','/uploads/vehicle/2026/03/01/2f1154ae0a42459497f450b54857bbc5.webp','approved',1,0,'2026-02-10 14:06:50','2026-02-10 14:09:56',1,'2027-02-10','2026-02-10 14:06:50','2026-05-03 15:46:42',0),(9,9,'湘A0533316','雅迪','白色','kky889','锂电池','电动自行车','','pending',0,0,'2026-02-21 15:48:52',NULL,NULL,NULL,'2026-02-21 15:48:52','2026-05-03 15:46:42',1),(10,13,'湘A0533317','雅迪','白色','sdfg35y','锂电池','电动自行车','/uploads/image/2026/02/21/c828ef640f084f9eb92ec0f453ae8769.jpg','approved',0,0,'2026-02-21 16:49:32','2026-02-21 16:50:01',1,'2027-02-21','2026-02-21 16:49:32','2026-05-03 15:46:42',1),(11,25,'湘A0533318','雅迪','白色','AABBCCDDEEFk','锂电池','电动自行车','/uploads/vehicle/2026/03/27/2c9f09b7522c46d9b3517b25af957c58.jpg','approved',0,0,'2026-03-21 15:46:09','2026-03-21 15:46:24',1,'2027-03-21','2026-03-21 15:46:09','2026-05-03 15:46:42',0),(12,30,'湘A0533326','hei','白','AABBCCDDEEFL','核电','电动车','/uploads/image/2026/03/21/a3aba4e3ad974efda807b6ba3f613745.jpg','pending',0,0,'2026-03-21 17:41:21',NULL,NULL,NULL,'2026-03-21 17:41:21','2026-05-03 15:46:42',0),(23,92,'湘A0533305','雅迪','白色','HYDD202605010001','锂电池','电动自行车','','approved',0,0,'2026-04-03 17:19:38','2026-04-04 17:19:38',NULL,'2027-05-01','2026-05-01 17:19:38','2026-05-03 15:46:42',0),(24,93,'湘A0533306','爱玛','黑色','HYDD202605010002','锂电池','电动自行车','','approved',0,0,'2026-04-04 17:19:38','2026-04-05 17:19:38',NULL,'2027-05-01','2026-05-01 17:19:38','2026-05-03 15:46:42',0),(25,94,'湘A0533307','台铃','蓝色','HYDD202605010003','锂电池','电动自行车','','approved',0,0,'2026-04-05 17:19:38','2026-04-06 17:19:38',NULL,'2027-05-01','2026-05-01 17:19:38','2026-05-03 15:46:42',0),(26,95,'湘A0533308','小刀','银色','HYDD202605010004','锂电池','电动自行车','','approved',0,0,'2026-04-06 17:19:38','2026-04-07 17:19:38',NULL,'2027-05-01','2026-05-01 17:19:38','2026-05-03 15:46:42',0),(27,96,'湘A0533309','绿源','红色','HYDD202605010005','锂电池','电动自行车','','approved',0,0,'2026-04-07 17:19:38','2026-04-08 17:19:38',NULL,'2027-05-01','2026-05-01 17:19:38','2026-05-03 15:46:42',0),(28,97,'湘A0533310','新日','灰色','HYDD202605010006','锂电池','电动自行车','','approved',0,0,'2026-04-08 17:19:38','2026-04-09 17:19:38',NULL,'2027-05-01','2026-05-01 17:19:38','2026-05-03 15:46:42',0),(29,98,'湘A0533311','雅迪','绿色','HYDD202605010007','锂电池','电动自行车','','approved',0,0,'2026-04-09 17:19:38','2026-04-10 17:19:38',NULL,'2027-05-01','2026-05-01 17:19:38','2026-05-03 15:46:42',0),(30,99,'湘A0533312','爱玛','黄色','HYDD202605010008','锂电池','电动自行车','','approved',0,0,'2026-04-10 17:19:38','2026-04-11 17:19:38',NULL,'2027-05-01','2026-05-01 17:19:38','2026-05-03 15:46:42',0),(31,100,'湘A0533313','台铃','白色','HYDD202605010009','锂电池','电动自行车','','approved',0,0,'2026-04-11 17:19:38','2026-04-12 17:19:38',NULL,'2027-05-01','2026-05-01 17:19:38','2026-05-03 15:46:42',0),(32,101,'湘A0533314','九号','黑色','HYDD202605010010','锂电池','电动自行车','','approved',0,0,'2026-04-12 17:19:38','2026-04-13 17:19:38',NULL,'2027-05-01','2026-05-01 17:19:38','2026-05-03 15:46:42',0),(33,152,'湘A90001','雅迪','白色','TRACKGUIDE202605030001','锂电池','电动自行车','','approved',0,0,'2026-03-09 22:38:33','2026-03-10 22:38:33',NULL,'2027-05-03','2026-05-03 22:38:33','2026-05-03 22:38:33',0),(34,153,'湘A90002','爱玛','黑色','TRACKGUIDE202605030002','锂电池','电动自行车','','approved',0,0,'2026-03-14 22:38:33','2026-03-15 22:38:33',NULL,'2027-05-03','2026-05-03 22:38:33','2026-05-03 22:38:33',0),(35,154,'湘A90003','台铃','蓝色','TRACKGUIDE202605030003','锂电池','电动自行车','','approved',0,0,'2026-03-19 22:38:33','2026-03-20 22:38:33',NULL,'2027-05-03','2026-05-03 22:38:33','2026-05-03 22:38:33',0),(36,155,'湘A90004','小刀','银色','TRACKGUIDE202605030004','锂电池','电动自行车','','approved',0,0,'2026-03-22 22:38:33','2026-03-23 22:38:33',NULL,'2027-05-03','2026-05-03 22:38:33','2026-05-03 22:38:33',0),(37,156,'湘A90005','绿源','红色','TRACKGUIDE202605030005','锂电池','电动自行车','','approved',0,0,'2026-03-26 22:38:33','2026-03-27 22:38:33',NULL,'2027-05-03','2026-05-03 22:38:33','2026-05-03 22:38:33',0),(38,157,'湘A90006','新日','灰色','TRACKGUIDE202605030006','锂电池','电动自行车','','approved',0,0,'2026-03-30 22:38:33','2026-03-31 22:38:33',NULL,'2027-05-03','2026-05-03 22:38:33','2026-05-03 22:38:33',0),(39,158,'湘A90007','雅迪','绿色','TRACKGUIDE202605030007','锂电池','电动自行车','','approved',0,0,'2026-04-03 22:38:33','2026-04-04 22:38:33',NULL,'2027-05-03','2026-05-03 22:38:33','2026-05-03 22:38:33',0),(40,159,'湘A90008','爱玛','黄色','TRACKGUIDE202605030008','锂电池','电动自行车','','approved',0,0,'2026-04-07 22:38:33','2026-04-08 22:38:33',NULL,'2027-05-03','2026-05-03 22:38:33','2026-05-03 22:38:33',0),(41,160,'湘A90009','台铃','白色','TRACKGUIDE202605030009','锂电池','电动自行车','','approved',0,0,'2026-04-11 22:38:33','2026-04-12 22:38:33',NULL,'2027-05-03','2026-05-03 22:38:33','2026-05-03 22:38:33',0),(42,161,'湘A90010','九号','黑色','TRACKGUIDE202605030010','锂电池','电动自行车','','approved',0,0,'2026-04-15 22:38:33','2026-04-16 22:38:33',NULL,'2027-05-03','2026-05-03 22:38:33','2026-05-03 22:38:33',0),(43,162,'湘A0599001','雅迪','白色','DEMOCHAIN20260814001','锂电池','电动自行车','','approved',0,0,'2026-07-20 08:52:43','2026-07-21 08:52:43',NULL,'2027-08-14','2026-07-20 08:52:43','2026-08-14 08:52:43',0);
/*!40000 ALTER TABLE `vehicle_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `view_vehicle_detail`
--

DROP TABLE IF EXISTS `view_vehicle_detail`;
/*!50001 DROP VIEW IF EXISTS `view_vehicle_detail`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `view_vehicle_detail` AS SELECT 
 1 AS `vehicle_id`,
 1 AS `user_id`,
 1 AS `username`,
 1 AS `real_name`,
 1 AS `user_type`,
 1 AS `phone`,
 1 AS `license_plate`,
 1 AS `brand`,
 1 AS `color`,
 1 AS `frame_num`,
 1 AS `battery_type`,
 1 AS `vehicle_type`,
 1 AS `vehicle_photo`,
 1 AS `status`,
 1 AS `is_frozen`,
 1 AS `is_blacklist`,
 1 AS `register_time`,
 1 AS `audit_time`,
 1 AS `audit_user_id`,
 1 AS `valid_date`,
 1 AS `create_time`,
 1 AS `update_time`,
 1 AS `etc_number`,
 1 AS `balance`,
 1 AS `device_number`,
 1 AS `device_type`,
 1 AS `device_status`,
 1 AS `battery_level`,
 1 AS `last_report_time`,
 1 AS `violation_count`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `view_violation_detail`
--

DROP TABLE IF EXISTS `view_violation_detail`;
/*!50001 DROP VIEW IF EXISTS `view_violation_detail`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `view_violation_detail` AS SELECT 
 1 AS `illegal_id`,
 1 AS `vehicle_id`,
 1 AS `user_id`,
 1 AS `owner_name`,
 1 AS `phone`,
 1 AS `license_plate`,
 1 AS `illegal_type`,
 1 AS `type_name`,
 1 AS `illegal_lng`,
 1 AS `illegal_lat`,
 1 AS `illegal_time`,
 1 AS `deduction_points`,
 1 AS `evidence_id`,
 1 AS `evidence_url`,
 1 AS `illegal_source`,
 1 AS `source_name`,
 1 AS `illegal_state`,
 1 AS `state_name`,
 1 AS `create_time`,
 1 AS `update_time`,
 1 AS `rectify_id`,
 1 AS `rectify_desc`,
 1 AS `rectify_time`,
 1 AS `verify_state`,
 1 AS `verify_state_name`,
 1 AS `appeal_id`,
 1 AS `appeal_state`,
 1 AS `appeal_state_name`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `violation_type`
--

DROP TABLE IF EXISTS `violation_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `violation_type` (
  `type_id` int NOT NULL AUTO_INCREMENT COMMENT '类型ID',
  `type_name` varchar(50) NOT NULL COMMENT '类型名称',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `deduction_points` int DEFAULT '1' COMMENT '扣除积分',
  `penalty_amount` decimal(10,2) DEFAULT '0.00' COMMENT '罚款金额',
  `appeal_deadline` int DEFAULT '72' COMMENT '申诉截止时间（小时）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`type_id`),
  UNIQUE KEY `uk_type_name` (`type_name`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='违规类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `violation_type`
--

LOCK TABLES `violation_type` WRITE;
/*!40000 ALTER TABLE `violation_type` DISABLE KEYS */;
INSERT INTO `violation_type` VALUES (1,'违规停放','在禁止停放区域停放电动车',2,0.00,72,'2026-02-10 15:12:36'),(2,'超速行驶','在校园内超速行驶',3,0.00,72,'2026-02-10 15:12:36'),(3,'违规充电','在禁止充电区域或使用违规方式充电',2,0.00,72,'2026-02-10 15:12:36'),(4,'未登记车辆','未登记车辆进入校园',1,0.00,72,'2026-02-10 15:12:36'),(5,'进入禁行区','车辆进入禁止通行区域',2,0.00,72,'2026-02-10 15:12:36'),(6,'违规载人','电动车违规载人',1,0.00,72,'2026-02-10 15:12:36'),(7,'遮挡车牌','故意遮挡或污损车牌',3,0.00,72,'2026-02-10 15:12:36'),(8,'其他违规','其他违规行为',1,0.00,72,'2026-02-10 15:12:36'),(9,'未佩戴头盔','骑乘校园电动车时未按规定佩戴安全头盔',2,0.00,72,'2026-08-13 01:58:01');
/*!40000 ALTER TABLE `violation_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `webcam`
--

DROP TABLE IF EXISTS `webcam`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `webcam` (
  `device` varchar(10) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `longitude` float(9,6) NOT NULL,
  `latitude` float(9,6) NOT NULL,
  `source` varchar(100) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  PRIMARY KEY (`device`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `webcam`
--

LOCK TABLES `webcam` WRITE;
/*!40000 ALTER TABLE `webcam` DISABLE KEYS */;
/*!40000 ALTER TABLE `webcam` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `welfare_point_request`
--

DROP TABLE IF EXISTS `welfare_point_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `welfare_point_request` (
  `request_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `content` varchar(800) NOT NULL COMMENT '公益说明',
  `image_urls` varchar(2000) DEFAULT NULL COMMENT '凭证图URL，逗号分隔',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0待审1通过2拒绝',
  `points_awarded` int NOT NULL DEFAULT '0',
  `admin_remark` varchar(500) DEFAULT NULL,
  `reviewer_id` int DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `audit_time` datetime DEFAULT NULL,
  PRIMARY KEY (`request_id`),
  KEY `idx_user_status` (`user_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公益加分申请';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `welfare_point_request`
--

LOCK TABLES `welfare_point_request` WRITE;
/*!40000 ALTER TABLE `welfare_point_request` DISABLE KEYS */;
/*!40000 ALTER TABLE `welfare_point_request` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `workflow_task`
--

DROP TABLE IF EXISTS `workflow_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `workflow_task` (
  `task_id` bigint NOT NULL AUTO_INCREMENT,
  `incident_id` bigint NOT NULL,
  `task_type` varchar(32) NOT NULL,
  `task_title` varchar(200) NOT NULL,
  `assignee_id` int DEFAULT NULL,
  `state` varchar(24) NOT NULL,
  `priority` varchar(16) NOT NULL,
  `due_at` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `result_remark` varchar(500) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`task_id`),
  KEY `idx_workflow_incident` (`incident_id`),
  KEY `idx_workflow_state_due` (`state`,`due_at`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI治理任务';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `workflow_task`
--

LOCK TABLES `workflow_task` WRITE;
/*!40000 ALTER TABLE `workflow_task` DISABLE KEYS */;
INSERT INTO `workflow_task` VALUES (1,1,'INCIDENT_REVIEW','AI治理：未佩戴头盔',NULL,'OPEN','MEDIUM','2026-08-12 09:47:43',NULL,NULL,'2026-08-11 09:47:43','2026-08-11 09:47:43'),(2,2,'INCIDENT_REVIEW','AI治理：违规停车',NULL,'CANCELLED','MEDIUM','2026-08-12 10:06:41','2026-08-11 10:15:34','API权限与审核人落库验证','2026-08-11 10:06:41','2026-08-11 10:15:34'),(3,3,'INCIDENT_REVIEW','AI治理：未佩戴头盔',NULL,'OPEN','MEDIUM','2026-08-12 15:49:51',NULL,NULL,'2026-08-11 15:49:51','2026-08-11 15:49:51'),(4,5,'INCIDENT_REVIEW','AI治理：未佩戴头盔',NULL,'OPEN','HIGH','2026-08-14 08:02:15',NULL,NULL,'2026-08-13 08:02:15','2026-08-13 08:02:15'),(5,8,'INCIDENT_REVIEW','AI治理：未佩戴头盔',NULL,'OPEN','MEDIUM','2026-08-23 11:10:20',NULL,NULL,'2026-08-22 11:10:20','2026-08-22 11:10:20'),(6,9,'INCIDENT_REVIEW','AI治理：未佩戴头盔',NULL,'OPEN','MEDIUM','2026-08-23 11:17:05',NULL,NULL,'2026-08-22 11:17:05','2026-08-22 11:17:05'),(7,10,'INCIDENT_REVIEW','AI治理：未佩戴头盔',NULL,'OPEN','MEDIUM','2026-08-23 15:34:06',NULL,NULL,'2026-08-22 15:34:06','2026-08-22 15:34:06'),(8,11,'INCIDENT_REVIEW','AI治理：未佩戴头盔',NULL,'OPEN','MEDIUM','2026-08-25 20:50:46',NULL,NULL,'2026-08-24 20:50:46','2026-08-24 20:50:46'),(9,12,'INCIDENT_REVIEW','AI治理：未佩戴头盔',NULL,'OPEN','MEDIUM','2026-09-04 14:11:37',NULL,NULL,'2026-09-03 14:11:37','2026-09-03 14:11:37'),(10,13,'INCIDENT_REVIEW','AI治理：未佩戴头盔',NULL,'OPEN','MEDIUM','2026-09-10 20:09:58',NULL,NULL,'2026-09-09 20:09:58','2026-09-09 20:09:58');
/*!40000 ALTER TABLE `workflow_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'ev_monitor'
--

--
-- Dumping routines for database 'ev_monitor'
--

--
-- Final view structure for view `view_vehicle_detail`
--

/*!50001 DROP VIEW IF EXISTS `view_vehicle_detail`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `view_vehicle_detail` AS select `v`.`vehicle_id` AS `vehicle_id`,`v`.`user_id` AS `user_id`,`u`.`username` AS `username`,`u`.`real_name` AS `real_name`,`u`.`user_type` AS `user_type`,`u`.`phone` AS `phone`,`v`.`license_plate` AS `license_plate`,`v`.`brand` AS `brand`,`v`.`color` AS `color`,`v`.`frame_num` AS `frame_num`,`v`.`battery_type` AS `battery_type`,`v`.`vehicle_type` AS `vehicle_type`,`v`.`vehicle_photo` AS `vehicle_photo`,`v`.`status` AS `status`,`v`.`is_frozen` AS `is_frozen`,`v`.`is_blacklist` AS `is_blacklist`,`v`.`register_time` AS `register_time`,`v`.`audit_time` AS `audit_time`,`v`.`audit_user_id` AS `audit_user_id`,`v`.`valid_date` AS `valid_date`,`v`.`create_time` AS `create_time`,`v`.`update_time` AS `update_time`,`e`.`etc_number` AS `etc_number`,`e`.`balance` AS `balance`,`d`.`device_number` AS `device_number`,`d`.`device_type` AS `device_type`,`d`.`status` AS `device_status`,`d`.`battery_level` AS `battery_level`,`d`.`last_report_time` AS `last_report_time`,(select count(0) from `vehicle_illegal_record` where ((`vehicle_illegal_record`.`vehicle_id` = `v`.`vehicle_id`) and (`vehicle_illegal_record`.`deleted` = false))) AS `violation_count` from (((`vehicle_info` `v` left join `user` `u` on((`v`.`user_id` = `u`.`user_id`))) left join `etc_card` `e` on((`v`.`vehicle_id` = `e`.`vehicle_id`))) left join `location_device` `d` on((`v`.`vehicle_id` = `d`.`vehicle_id`))) where (`v`.`deleted` = false) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `view_violation_detail`
--

/*!50001 DROP VIEW IF EXISTS `view_violation_detail`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `view_violation_detail` AS select `vir`.`illegal_id` AS `illegal_id`,`vir`.`vehicle_id` AS `vehicle_id`,`vir`.`user_id` AS `user_id`,`u`.`real_name` AS `owner_name`,`u`.`phone` AS `phone`,`v`.`license_plate` AS `license_plate`,`vir`.`illegal_type` AS `illegal_type`,(case `vir`.`illegal_type` when 1 then '违规停放' when 2 then '超速行驶' when 3 then '违规充电' when 4 then '未登记车辆' when 5 then '进入禁行区' when 6 then '违规载人' when 7 then '遮挡车牌' when 8 then '其他违规' end) AS `type_name`,`vir`.`illegal_lng` AS `illegal_lng`,`vir`.`illegal_lat` AS `illegal_lat`,`vir`.`illegal_time` AS `illegal_time`,`vir`.`deduction_points` AS `deduction_points`,`vir`.`evidence_id` AS `evidence_id`,`de`.`evidence_url` AS `evidence_url`,`vir`.`illegal_source` AS `illegal_source`,(case `vir`.`illegal_source` when 1 then '无人机巡检' when 2 then '定位模块' when 3 then '人工举报' end) AS `source_name`,`vir`.`illegal_state` AS `illegal_state`,(case `vir`.`illegal_state` when 0 then '待整改' when 1 then '整改中' when 2 then '已整改' when 3 then '逾期未整改' when 4 then '申诉中' end) AS `state_name`,`vir`.`create_time` AS `create_time`,`vir`.`update_time` AS `update_time`,`irr`.`rectify_id` AS `rectify_id`,`irr`.`rectify_desc` AS `rectify_desc`,`irr`.`rectify_time` AS `rectify_time`,`irr`.`verify_state` AS `verify_state`,(case `irr`.`verify_state` when 0 then '待核实' when 1 then '核实中' when 2 then '核实通过' when 3 then '核实不通过' end) AS `verify_state_name`,`iar`.`appeal_id` AS `appeal_id`,`iar`.`appeal_state` AS `appeal_state`,(case `iar`.`appeal_state` when 0 then '待审核' when 1 then '审核中' when 2 then '申诉成功' when 3 then '申诉失败' end) AS `appeal_state_name` from (((((`vehicle_illegal_record` `vir` left join `user` `u` on((`vir`.`user_id` = `u`.`user_id`))) left join `vehicle_info` `v` on((`vir`.`vehicle_id` = `v`.`vehicle_id`))) left join `drone_evidence` `de` on((`vir`.`evidence_id` = `de`.`evidence_id`))) left join `illegal_rectify_record` `irr` on((`vir`.`illegal_id` = `irr`.`illegal_id`))) left join `illegal_appeal_record` `iar` on((`vir`.`illegal_id` = `iar`.`illegal_id`))) where (`vir`.`deleted` = false) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-11 11:53:51
