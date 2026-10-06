-- Flyway V1：初始化博物馆作品档案系统数据库
SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 完好程度
-- ------------------------------------------------------------
CREATE TABLE artwork_condition (
    id          INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL COMMENT '完好程度名称',
    sort_order  INT NOT NULL DEFAULT 1000 COMMENT '排序值',
    status      TINYINT NOT NULL DEFAULT 1 COMMENT '1启用，0停用',
    UNIQUE KEY uk_artwork_condition_name (name),
    KEY idx_artwork_condition_status_sort (status, sort_order, id),
    CONSTRAINT chk_artwork_condition_status CHECK (status IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作品完好程度字典';

INSERT INTO artwork_condition(id,name,sort_order,status) VALUES
(1,'真迹完好',1000,1),
(2,'真迹损坏',2000,1),
(3,'影印完好',3000,1),
(4,'影印损坏',4000,1);

-- ------------------------------------------------------------
-- 作品状态
-- ------------------------------------------------------------
CREATE TABLE artwork_status (
    id          INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL COMMENT '作品状态名称',
    sort_order  INT NOT NULL DEFAULT 1000 COMMENT '排序值',
    status      TINYINT NOT NULL DEFAULT 1 COMMENT '1启用，0停用',
    UNIQUE KEY uk_artwork_status_name (name),
    KEY idx_artwork_status_status_sort (status, sort_order, id),
    CONSTRAINT chk_artwork_status_status CHECK (status IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作品状态字典';

INSERT INTO artwork_status(id,name,sort_order,status) VALUES
(1,'库存',1000,1),
(2,'展出',2000,1),
(3,'丢失',3000,1),
(4,'馈赠',4000,1),
(5,'出版物',5000,1),
(6,'拍卖',6000,1);

-- ------------------------------------------------------------
-- 多媒体类型
-- ------------------------------------------------------------
CREATE TABLE media_type (
    id          INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL COMMENT '多媒体类型名称',
    sort_order  INT NOT NULL DEFAULT 1000 COMMENT '排序值',
    status      TINYINT NOT NULL DEFAULT 1 COMMENT '1启用，0停用',
    UNIQUE KEY uk_media_type_name (name),
    KEY idx_media_type_status_sort (status, sort_order, id),
    CONSTRAINT chk_media_type_status CHECK (status IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='多媒体类型字典';

INSERT INTO media_type(id,name,sort_order,status) VALUES
(1,'照片',1000,1),
(2,'视频',2000,1),
(3,'文字',3000,1);

-- ------------------------------------------------------------
-- 作品分类：仅 status，无 deleted
-- ------------------------------------------------------------
CREATE TABLE artwork_category (
    id          INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    parent_id   INT UNSIGNED DEFAULT NULL COMMENT '父分类ID，NULL表示根节点',
    name        VARCHAR(100) NOT NULL COMMENT '分类名称',
    level       TINYINT NOT NULL COMMENT '层级，1~5',
    sort_order  INT NOT NULL COMMENT '同级排序值',
    status      TINYINT NOT NULL DEFAULT 1 COMMENT '1启用，0停用/删除',
    KEY idx_artwork_category_parent_sort (parent_id, sort_order, id),
    KEY idx_artwork_category_status (status),
    CONSTRAINT fk_artwork_category_parent
        FOREIGN KEY (parent_id) REFERENCES artwork_category(id),
    CONSTRAINT chk_artwork_category_level CHECK (level BETWEEN 1 AND 5),
    CONSTRAINT chk_artwork_category_status CHECK (status IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作品分类树';

-- 169个分类节点，原顺序与ID保持不变。
-- 169 category nodes from the supplied workbook; order preserved.
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (1,NULL,'绘画',1,1000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (2,1,'水墨画',2,2000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (3,2,'人物',3,3000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (4,2,'动物',3,4000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (5,2,'其他',3,5000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (6,1,'油画',2,6000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (7,6,'人物',3,7000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (8,6,'动物',3,8000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (9,6,'其他',3,9000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (10,1,'铁笔画',2,10000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (11,10,'人物',3,11000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (12,10,'动物',3,12000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (13,10,'其他',3,13000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (14,1,'麦克笔画',2,14000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (15,14,'人物',3,15000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (16,14,'动物',3,16000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (17,14,'其他',3,17000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (18,NULL,'书法',1,18000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (19,18,'篆书',2,19000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (20,18,'行书',2,20000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (21,18,'楷书',2,21000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (22,18,'天书',2,22000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (23,18,'其他',2,23000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (24,NULL,'雕塑',1,24000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (25,24,'室内雕塑',2,25000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (26,25,'木雕',3,26000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (27,26,'人物系列',4,27000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (28,26,'动物系列',4,28000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (29,26,'家具系列',4,29000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (30,26,'其他',4,30000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (31,25,'铁艺',3,31000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (32,31,'人物系列',4,32000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (33,31,'动物系列',4,33000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (34,31,'其他',4,34000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (35,25,'石雕',3,35000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (36,35,'人物系列',4,36000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (37,35,'动物系列',4,37000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (38,35,'兵器系列',4,38000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (39,35,'其他',4,39000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (40,25,'铸造锻造',3,40000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (41,40,'人物系列',4,41000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (42,40,'动物系列',4,42000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (43,40,'兵器系列',4,43000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (44,40,'鼎爵系列',4,44000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (45,40,'其他',4,45000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (46,24,'城市雕塑',2,46000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (47,46,'青铜铸造',3,47000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (48,46,'紫铜锻造',3,48000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (49,46,'不锈钢锻造',3,49000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (50,46,'花岗岩石雕',3,50000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (51,46,'其他',3,51000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (52,NULL,'平面设计',1,52000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (53,52,'标志设计',2,53000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (54,52,'装帧设计',2,54000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (55,NULL,'陶瓷',1,55000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (56,55,'钧瓷',2,56000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (57,55,'挂盘',2,57000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (58,55,'青瓷',2,58000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (59,55,'花釉',2,59000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (60,55,'紫砂',2,60000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (61,55,'黑釉',2,61000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (62,55,'黑陶',2,62000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (63,55,'硫璃',2,63000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (64,55,'土陶',2,64000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (65,55,'其他',2,65000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (66,NULL,'剪纸',1,66000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (67,66,'人物',2,67000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (68,66,'动物',2,68000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (69,66,'其他',2,69000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (70,NULL,'民间艺术',1,70000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (71,70,'印染',2,71000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (72,70,'壁挂',2,72000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (73,70,'剪纸',2,73000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (74,70,'布老虎',2,74000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (75,70,'草编',2,75000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (76,70,'其他',2,76000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (77,NULL,'著作',1,77000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (78,77,'书籍',2,78000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (79,78,'散文集',3,79000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (80,78,'画册',3,80000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (81,78,'其他',3,81000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (82,77,'文章',2,82000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (83,82,'散文',3,83000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (84,82,'论文',3,84000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (85,82,'其他',3,85000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (86,NULL,'媒体',1,86000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (87,86,'影视',2,87000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (88,87,'电影',3,88000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (89,87,'电视',3,89000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (90,87,'其他',3,90000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (91,86,'报刊',2,91000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (92,91,'评论',3,92000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (93,91,'报道',3,93000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (94,91,'其他',3,94000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (95,NULL,'照片资料',1,95000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (96,95,'作品',2,96000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (97,95,'展览',2,97000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (98,95,'名人',2,98000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (99,95,'工作',2,99000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (100,95,'出访',2,100000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (101,95,'生活',2,101000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (102,95,'采风',2,102000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (103,95,'艺术馆',2,103000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (104,95,'其他',2,104000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (105,NULL,'展览',1,105000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (106,105,'国内展览',2,106000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (107,105,'国际展览',2,107000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (108,NULL,'周建萍',1,108000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (109,108,'小说',2,109000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (110,108,'电影文学剧本',2,110000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (111,108,'报告文学',2,111000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (112,108,'纪实文学',2,112000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (113,108,'散文',2,113000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (114,108,'报道',2,114000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (115,108,'评论',2,115000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (116,108,'写美林的文章',2,116000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (117,108,'书信',2,117000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (118,108,'其他',2,118000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (119,NULL,'赠品',1,119000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (120,119,'国内',2,120000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (121,119,'国外',2,121000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (122,NULL,'拍卖',1,122000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (123,122,'国内拍卖',2,123000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (124,122,'国际拍卖',2,124000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (125,NULL,'藏书',1,125000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (126,125,'综合',2,126000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (127,126,'文史资料',3,127000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (128,126,'医学',3,128000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (129,126,'经济学',3,129000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (130,126,'辞典',3,130000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (131,126,'民族学',3,131000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (132,126,'法律学',3,132000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (133,126,'世界史',3,133000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (134,126,'中国史',3,134000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (135,126,'经典理论',3,135000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (136,126,'建筑',3,136000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (137,126,'中国文学',3,137000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (138,126,'自然科学',3,138000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (139,126,'财政金融',3,139000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (140,126,'外国文学',3,140000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (141,126,'语言文学',3,141000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (142,126,'文物考古',3,142000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (143,126,'传记',3,143000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (144,126,'宗教',3,144000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (145,126,'教育',3,145000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (146,126,'体育',3,146000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (147,126,'哲学',3,147000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (148,126,'生活',3,148000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (149,126,'其他',3,149000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (150,125,'艺术',2,150000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (151,150,'音乐',3,151000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (152,150,'戏剧',3,152000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (153,150,'雕塑',3,153000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (154,150,'书法',3,154000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (155,150,'摄影',3,155000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (156,150,'工艺美术',3,156000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (157,150,'篆刻',3,157000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (158,150,'舞蹈',3,158000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (159,150,'影视',3,159000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (160,150,'陶瓷',3,160000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (161,150,'绘画',3,161000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (162,150,'其他',3,162000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (163,NULL,'艺术馆',1,163000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (164,163,'韩美林艺术馆(北京)',2,164000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (165,163,'韩美林艺术馆(杭州)',2,165000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (166,163,'韩美林艺术馆(香港)',2,166000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (167,163,'韩美林艺术馆(日本)',2,167000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (168,163,'韩美林艺术馆(韩国)',2,168000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);
INSERT INTO artwork_category(id,parent_id,name,level,sort_order,status) VALUES (169,163,'其他',2,169000,1) ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id),name=VALUES(name),level=VALUES(level),sort_order=VALUES(sort_order),status=VALUES(status);

-- ------------------------------------------------------------
-- 位置分类：仅 status，无 deleted，最多5级
-- ------------------------------------------------------------
CREATE TABLE location_category (
    id          INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    parent_id   INT UNSIGNED DEFAULT NULL COMMENT '父位置分类ID，NULL表示根节点',
    name        VARCHAR(100) NOT NULL COMMENT '位置分类名称',
    level       TINYINT NOT NULL COMMENT '层级，1~5',
    sort_order  INT NOT NULL COMMENT '同级排序值',
    status      TINYINT NOT NULL DEFAULT 1 COMMENT '1启用，0停用/删除',
    KEY idx_location_category_parent_sort (parent_id, sort_order, id),
    KEY idx_location_category_status (status),
    CONSTRAINT fk_location_category_parent
        FOREIGN KEY (parent_id) REFERENCES location_category(id),
    CONSTRAINT chk_location_category_level CHECK (level BETWEEN 1 AND 5),
    CONSTRAINT chk_location_category_status CHECK (status IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='位置分类树';

-- ------------------------------------------------------------
-- 作品主表
-- ------------------------------------------------------------
CREATE TABLE artwork (
    id                   BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    name                 VARCHAR(200) NOT NULL COMMENT '作品名称',
    primary_category_id  INT UNSIGNED DEFAULT NULL COMMENT '主作品分类',
    create_start_time    DATETIME(3) DEFAULT NULL COMMENT '创作开始时间',
    create_end_time      DATETIME(3) DEFAULT NULL COMMENT '创作结束时间',
    condition_id         INT UNSIGNED NOT NULL COMMENT '完好程度',
    dimensions           VARCHAR(500) DEFAULT NULL COMMENT '作品尺寸',
    price                VARCHAR(500) DEFAULT NULL COMMENT '作品价格',
    author               VARCHAR(200) DEFAULT NULL COMMENT '作者',
    registration_no      VARCHAR(100) DEFAULT NULL COMMENT '作品登记号',
    inscription          TEXT COMMENT '作品题跋',
    summary              TEXT COMMENT '作品简述',
    status_id            INT UNSIGNED NOT NULL COMMENT '作品状态',
    location_category_id INT UNSIGNED DEFAULT NULL COMMENT '位置分类',
    specific_location    VARCHAR(255) DEFAULT NULL COMMENT '具体位置',
    search_keywords      VARCHAR(500) DEFAULT NULL COMMENT '搜索关键字',
    version              INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    fulltext_content     TEXT COMMENT '由名称/作者/题跋/简述/搜索关键字组成的全文检索冗余字段',
    deleted              TINYINT NOT NULL DEFAULT 0 COMMENT '0正常，1逻辑删除',
    created_at           DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at           DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    KEY idx_artwork_category (primary_category_id),
    KEY idx_artwork_condition (condition_id),
    KEY idx_artwork_status (status_id),
    KEY idx_artwork_location (location_category_id),
    KEY idx_artwork_deleted_id (deleted, id),
    KEY idx_artwork_registration_no (registration_no),
    KEY idx_artwork_creation_start (create_start_time),
    KEY idx_artwork_creation_end (create_end_time),

    FULLTEXT KEY ft_artwork_fulltext_content (fulltext_content) WITH PARSER ngram,

    CONSTRAINT fk_artwork_primary_category
        FOREIGN KEY (primary_category_id) REFERENCES artwork_category(id),
    CONSTRAINT fk_artwork_condition
        FOREIGN KEY (condition_id) REFERENCES artwork_condition(id),
    CONSTRAINT fk_artwork_status
        FOREIGN KEY (status_id) REFERENCES artwork_status(id),
    CONSTRAINT fk_artwork_location_category
        FOREIGN KEY (location_category_id) REFERENCES location_category(id),
    CONSTRAINT chk_artwork_deleted CHECK (deleted IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作品主档案';

-- ------------------------------------------------------------
-- 作品-分类关系
-- ------------------------------------------------------------
CREATE TABLE artwork_category_relation (
    artwork_id  BIGINT UNSIGNED NOT NULL,
    category_id INT UNSIGNED NOT NULL,
    is_primary  TINYINT NOT NULL DEFAULT 0 COMMENT '1主分类，0其他分类',
    PRIMARY KEY (artwork_id, category_id),
    KEY idx_artwork_category_relation_category (category_id, artwork_id),
    CONSTRAINT fk_acr_artwork
        FOREIGN KEY (artwork_id) REFERENCES artwork(id),
    CONSTRAINT fk_acr_category
        FOREIGN KEY (category_id) REFERENCES artwork_category(id),
    CONSTRAINT chk_acr_is_primary CHECK (is_primary IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作品分类关系';

-- ------------------------------------------------------------
-- 多媒体
-- ------------------------------------------------------------
CREATE TABLE multimedia (
    id                 BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    artwork_id         BIGINT UNSIGNED NOT NULL COMMENT '作品ID',
    description        VARCHAR(200) DEFAULT NULL COMMENT '说明',
    address            VARCHAR(2000) DEFAULT NULL COMMENT '原始地址/来源地址',
    multimedia_type_id INT UNSIGNED NOT NULL COMMENT '多媒体类型',
    sort_order         INT NOT NULL DEFAULT 1000 COMMENT '排序值',
    is_primary         TINYINT NOT NULL DEFAULT 0 COMMENT '是否主要媒体',
    status             TINYINT NOT NULL DEFAULT 1 COMMENT '1启用，0停用',
    deleted            TINYINT NOT NULL DEFAULT 0 COMMENT '0正常，1逻辑删除',
    created_at         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_multimedia_artwork_sort (artwork_id, deleted, sort_order, id),
    KEY idx_multimedia_type (multimedia_type_id),
    CONSTRAINT fk_multimedia_artwork
        FOREIGN KEY (artwork_id) REFERENCES artwork(id),
    CONSTRAINT fk_multimedia_type
        FOREIGN KEY (multimedia_type_id) REFERENCES media_type(id),
    CONSTRAINT chk_multimedia_status CHECK (status IN (0,1)),
    CONSTRAINT chk_multimedia_deleted CHECK (deleted IN (0,1)),
    CONSTRAINT chk_multimedia_is_primary CHECK (is_primary IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='多媒体主表';

-- ------------------------------------------------------------
-- 多媒体文件版本
-- ------------------------------------------------------------
CREATE TABLE multimedia_variant (
    id               BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    multimedia_id    BIGINT UNSIGNED NOT NULL,
    variant_type     VARCHAR(40) NOT NULL COMMENT 'ORIGINAL/HIGH_RES/THUMB_1024/THUMB_256/THUMB_64',
    object_key       VARCHAR(1000) NOT NULL COMMENT 'RustFS/S3 Object Key',
    content_type     VARCHAR(100) NOT NULL,
    file_name        VARCHAR(500) DEFAULT NULL,
    file_size        BIGINT UNSIGNED DEFAULT NULL,
    width            INT DEFAULT NULL,
    height           INT DEFAULT NULL,
    checksum_sha256  CHAR(64) DEFAULT NULL,
    status           TINYINT NOT NULL DEFAULT 1,
    deleted          TINYINT NOT NULL DEFAULT 0,
    created_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_multimedia_variant (multimedia_id, variant_type),
    KEY idx_variant_multimedia_deleted (multimedia_id, deleted, variant_type),
    KEY idx_variant_checksum (checksum_sha256),
    CONSTRAINT fk_variant_multimedia
        FOREIGN KEY (multimedia_id) REFERENCES multimedia(id),
    CONSTRAINT chk_variant_status CHECK (status IN (0,1)),
    CONSTRAINT chk_variant_deleted CHECK (deleted IN (0,1)),
    CONSTRAINT chk_variant_dimensions CHECK ((width IS NULL AND height IS NULL) OR (width > 0 AND height > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='多媒体实际文件及图片变体';

-- ------------------------------------------------------------
-- 出库审批
-- ------------------------------------------------------------
CREATE TABLE outbound_approval (
    id                   BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    artwork_id           BIGINT UNSIGNED NOT NULL,
    target_status_id     INT UNSIGNED NOT NULL COMMENT '审批通过后目标作品状态',
    approval_status      TINYINT NOT NULL DEFAULT 0 COMMENT '0待审 1通过 2驳回 3已出库 4已归还',
    explanation         VARCHAR(4000) DEFAULT NULL COMMENT '说明',
    outbound_time       DATETIME(3) DEFAULT NULL COMMENT '实际/计划出库时间',
    expected_return_time DATETIME(3) DEFAULT NULL COMMENT '预计归还时间',
    actual_return_time  DATETIME(3) DEFAULT NULL COMMENT '实际归还时间',
    applicant_id        INT UNSIGNED NOT NULL COMMENT '申请人',
    approver_id         INT UNSIGNED DEFAULT NULL COMMENT '审批人',
    approval_time       DATETIME(3) DEFAULT NULL COMMENT '审批时间',
    remark              VARCHAR(4000) DEFAULT NULL COMMENT '备注',
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_outbound_artwork_status (artwork_id, approval_status, created_at),
    KEY idx_outbound_status_created (approval_status, created_at),
    CONSTRAINT fk_outbound_artwork
        FOREIGN KEY (artwork_id) REFERENCES artwork(id),
    CONSTRAINT fk_outbound_target_status
        FOREIGN KEY (target_status_id) REFERENCES artwork_status(id),
    CONSTRAINT chk_outbound_approval_status CHECK (approval_status BETWEEN 0 AND 4)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作品出库审批';

-- ------------------------------------------------------------
-- 作品历史
-- ------------------------------------------------------------
CREATE TABLE artwork_history (
    id                    BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    artwork_id            BIGINT UNSIGNED NOT NULL,
    operation_type        VARCHAR(60) NOT NULL,
    field_name            VARCHAR(100) DEFAULT NULL,
    old_value             JSON DEFAULT NULL,
    new_value             JSON DEFAULT NULL,
    detail                JSON DEFAULT NULL,
    related_outbound_id   BIGINT UNSIGNED DEFAULT NULL,
    related_multimedia_id BIGINT UNSIGNED DEFAULT NULL,
    operation_summary     VARCHAR(2000) DEFAULT NULL,
    ip_address            VARCHAR(64) DEFAULT NULL,
    operator_id           INT UNSIGNED DEFAULT NULL,
    operation_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    remark                VARCHAR(500) DEFAULT NULL,
    KEY idx_history_artwork_time (artwork_id, operation_at DESC, id DESC),
    KEY idx_history_operation_type (operation_type, operation_at DESC),
    CONSTRAINT fk_history_artwork
        FOREIGN KEY (artwork_id) REFERENCES artwork(id),
    CONSTRAINT fk_history_outbound
        FOREIGN KEY (related_outbound_id) REFERENCES outbound_approval(id),
    CONSTRAINT fk_history_multimedia
        FOREIGN KEY (related_multimedia_id) REFERENCES multimedia(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作品历史与审计事实';

-- ------------------------------------------------------------
-- 备份任务
-- ------------------------------------------------------------
CREATE TABLE backup_job (
    id                BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    backup_type       VARCHAR(30) NOT NULL,
    storage_location  VARCHAR(1000) DEFAULT NULL,
    file_name         VARCHAR(500) DEFAULT NULL,
    file_size         BIGINT UNSIGNED DEFAULT NULL,
    sha256            CHAR(64) DEFAULT NULL,
    status            VARCHAR(30) NOT NULL,
    started_at        DATETIME(3) NOT NULL,
    completed_at      DATETIME(3) DEFAULT NULL,
    error_message     VARCHAR(4000) DEFAULT NULL,
    KEY idx_backup_started (started_at DESC),
    KEY idx_backup_type_status (backup_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='备份任务记录';

