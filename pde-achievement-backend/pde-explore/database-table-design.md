---
ruleType: Model Request
description: MySQL数据库建表规范，包含表必须使用InnoDB引擎、表必备字段要求、表名字段名命名规范、禁用MySQL保留字、表名使用单数形式、枚举字段使用字符串、布尔字段命名规范、小数类型使用DECIMAL、字符串类型选择、字段注释要求、分库分表建议、逻辑删除建议等建表规范
category: SQL规范
priority: medium
globs: *.java
---

# MySQL数据库建表规范

## 一、说明

本规范定义了MySQL数据库建表的标准，包含表引擎选择、必备字段、命名规范、字段类型选择、注释要求等方面的规范。遵循本规范能够确保数据库设计的规范性和一致性。

## 二、规则内容

### N001 表必须使用InnoDB引擎

表一律使用InnoDB引擎，确保事务支持和性能优化。

### N002 表必备字段要求

表必备字段：主键。建议字段：创建时间、更新时间。

**说明**：
- 更新时间建议设置默认值DEFAULT NOW() ON UPDATE CURRENT_TIMESTAMP，并增加索引
- 主键类型为BIGINT、单表时自增、步长为1
- 时间字段类型为DATETIME，不建议使用TIMESTAMP

### N003 表名字段名命名规范

表名、字段名只允许由字母、数字或下划线构成，禁止使用符合特定规则的表名（如临时表、影子表、归档表格式）。

### N004 禁用MySQL保留字

表名或字段名禁用保留字，如DESC、RANGE、MATCH、DELAYED等，包括MySQL 8.0保留字。

### N005 表名使用单数形式

表名不使用复数名词，表名应该仅仅表示表里面的实体内容，不应该表示实体数量。

### N006 枚举字段使用字符串

不允许使用数字表示枚举类型字段，枚举使用表意的字符串。

### N007 布尔字段命名规范

表达是与否概念的字段，建议使用xxx_flag/xxx_status，数据类型使用TINYINT UNSIGNED（1表示是，0表示否）。

### N008 小数类型使用DECIMAL

小数类型为DECIMAL，禁止使用FLOAT和DOUBLE，避免精度损失问题。

### N009 字符串类型选择

如果存储固定长度的字符串，使用CHAR定长字符串类型。VARCHAR长度不要超过5000，如果存储长度大于此值，定义字段类型为TEXT，独立出来一张表。

### N010 字段注释要求

如果修改字段含义或对字段表示的状态追加，必须及时更新字段注释。建议对表和字段都增加必要的注释。

### N011 分库分表建议

单表行数超过1000万行或者单表容量超过80GB，才推荐进行分库分表。如果预计三年后的数据量根本达不到这个级别，请不要在创建表时就分库分表。

### N012 逻辑删除建议

数据库中不使用物理删除操作，建议使用逻辑删除。

## 三、执行要求

1. 所有建表操作必须遵循本规范要求
2. 建表前必须进行表设计评审
3. 对于不符合规范的表结构必须及时整改
4. 新表必须严格按照本规范设计

## 四、示例

### N001 表必须使用InnoDB引擎示例

#### good case:

```sql
CREATE TABLE user_info (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### bad case:

```sql
CREATE TABLE user_info (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=MyISAM;
```

### N002 表必备字段要求示例

#### good case:

```sql
CREATE TABLE user_info (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### bad case:

```sql
CREATE TABLE user_info (
    name VARCHAR(50) NOT NULL,
    create_time TIMESTAMP
) ENGINE=InnoDB;
```

### N003 表名字段名命名规范示例

#### good case:

```sql
CREATE TABLE meituan_admin (
    id BIGINT NOT NULL AUTO_INCREMENT,
    level3_name VARCHAR(50),
    PRIMARY KEY (id)
) ENGINE=InnoDB;
```

#### bad case:

```sql
CREATE TABLE abc_tmp (
    id BIGINT NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE test_archive20241201 (
    id BIGINT NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (id)
) ENGINE=InnoDB;
```

### N004 禁用MySQL保留字示例

#### good case:

```sql
CREATE TABLE user_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_desc VARCHAR(200),
    PRIMARY KEY (id)
) ENGINE=InnoDB;
```

#### bad case:

```sql
CREATE TABLE `order` (
    id BIGINT NOT NULL AUTO_INCREMENT,
    `desc` VARCHAR(200),
    `match` VARCHAR(100),
    PRIMARY KEY (id)
) ENGINE=InnoDB;
```

### N005 表名使用单数形式示例

#### good case:

```sql
CREATE TABLE book (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(100),
    PRIMARY KEY (id)
) ENGINE=InnoDB;
```

#### bad case:

```sql
CREATE TABLE books (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(100),
    PRIMARY KEY (id)
) ENGINE=InnoDB;
```

### N006 枚举字段使用字符串示例

#### good case:

```sql
CREATE TABLE user_info (
    id BIGINT NOT NULL AUTO_INCREMENT,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    PRIMARY KEY (id)
) ENGINE=InnoDB;
```

#### bad case:

```sql
CREATE TABLE user_info (
    id BIGINT NOT NULL AUTO_INCREMENT,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1-激活 2-禁用',
    PRIMARY KEY (id)
) ENGINE=InnoDB;
```

### N007 布尔字段命名规范示例

#### good case:

```sql
CREATE TABLE user_info (
    id BIGINT NOT NULL AUTO_INCREMENT,
    deleted_flag TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '1-已删除 0-未删除',
    active_status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1-激活 0-未激活',
    PRIMARY KEY (id)
) ENGINE=InnoDB;
```

#### bad case:

```sql
CREATE TABLE user_info (
    id BIGINT NOT NULL AUTO_INCREMENT,
    is_deleted BOOLEAN DEFAULT FALSE,
    active INT DEFAULT 1,
    PRIMARY KEY (id)
) ENGINE=InnoDB;
```

### N008 小数类型使用DECIMAL示例

#### good case:

```sql
CREATE TABLE product (
    id BIGINT NOT NULL AUTO_INCREMENT,
    price DECIMAL(10,2) NOT NULL,
    weight DECIMAL(8,3),
    PRIMARY KEY (id)
) ENGINE=InnoDB;
```

#### bad case:

```sql
CREATE TABLE product (
    id BIGINT NOT NULL AUTO_INCREMENT,
    price FLOAT NOT NULL,
    weight DOUBLE,
    PRIMARY KEY (id)
) ENGINE=InnoDB;
```

### N009 字符串类型选择示例

#### good case:

```sql
CREATE TABLE user_info (
    id BIGINT NOT NULL AUTO_INCREMENT,
    mobile CHAR(11) NOT NULL,
    name VARCHAR(50),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE user_content (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    content TEXT,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB;
```

#### bad case:

```sql
CREATE TABLE user_info (
    id BIGINT NOT NULL AUTO_INCREMENT,
    mobile VARCHAR(11) NOT NULL,
    description VARCHAR(8000),
    PRIMARY KEY (id)
) ENGINE=InnoDB;
```

### N010 字段注释要求示例

#### good case:

```sql
CREATE TABLE user_info (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    name VARCHAR(50) NOT NULL COMMENT '用户姓名',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '用户状态：1-正常 2-禁用',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='用户信息表';
```

#### bad case:

```sql
CREATE TABLE user_info (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    status TINYINT UNSIGNED NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
) ENGINE=InnoDB;
```

### N011 分库分表建议示例

#### good case:

```sql
-- 普通业务表，无需分库分表
CREATE TABLE user_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    order_amount DECIMAL(10,2),
    PRIMARY KEY (id)
) ENGINE=InnoDB;
```

#### bad case:

```sql
-- 过早进行分表设计
CREATE TABLE user_order_0 (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    order_amount DECIMAL(10,2),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE user_order_1 (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    order_amount DECIMAL(10,2),
    PRIMARY KEY (id)
) ENGINE=InnoDB;
```

### N012 逻辑删除建议示例

#### good case:

```sql
CREATE TABLE user_info (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    deleted_flag TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除标识：0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_deleted_flag (deleted_flag)
) ENGINE=InnoDB;

-- 逻辑删除操作
UPDATE user_info SET deleted_flag = 1 WHERE id = 123;
```

#### bad case:

```sql
-- 物理删除操作
DELETE FROM user_info WHERE id = 123;
```

## 五、规范执行要点

### 核心要求
1. **存储引擎**：表必须使用InnoDB引擎
2. **必备字段**：主键(BIGINT自增),建议包含创建时间、更新时间
3. **主键规范**：类型为BIGINT,单表时自增,步长为1
4. **时间字段**：类型为DATETIME,不建议TIMESTAMP
5. **更新时间**：建议设置DEFAULT NOW() ON UPDATE CURRENT_TIMESTAMP并增加索引
6. **命名规范**：表名和字段名只允许字母、数字或下划线,禁用保留字
7. **表名形式**：使用单数形式,不使用复数名词
8. **枚举字段**：必须使用表意的字符串(如'active'),禁用数字
9. **布尔字段**：使用xxx_flag/xxx_status命名,类型为TINYINT UNSIGNED(1=是,0=否)
10. **小数类型**：必须使用DECIMAL,禁用FLOAT和DOUBLE
11. **字符串类型**：固定长度用CHAR,VARCHAR不超过5000,超过用TEXT并独立表
12. **字段注释**：修改字段含义必须及时更新注释,建议所有表和字段都有注释
13. **分库分表**：仅当单表超过1000万行或80GB时才考虑,避免过早优化
14. **删除方式**：必须使用逻辑删除,禁用物理删除

### 强制禁止
- ✗ 禁止使用MyISAM等非InnoDB引擎
- ✗ 禁止表没有主键
- ✗ 禁止使用TIMESTAMP类型(时区问题)
- ✗ 禁止表名或字段名使用MySQL保留字
- ✗ 禁止表名使用复数形式(如users)
- ✗ 禁止枚举字段使用数字(如1,2,3)
- ✗ 禁止小数使用FLOAT或DOUBLE(精度损失)
- ✗ 禁止VARCHAR超过5000(应拆分为TEXT独立表)
- ✗ 禁止过早分库分表(数据量未达1000万行/80GB)
- ✗ 禁止物理删除数据(DELETE操作)

### 代码审查检查点
- [ ] 表是否使用了InnoDB引擎
- [ ] 表是否有主键,类型是否为BIGINT自增
- [ ] 是否包含创建时间和更新时间字段
- [ ] 时间字段是否使用DATETIME而非TIMESTAMP
- [ ] 表名和字段名是否符合命名规范
- [ ] 是否使用了MySQL保留字
- [ ] 表名是否使用了复数形式
- [ ] 枚举字段是否使用了字符串而非数字
- [ ] 布尔字段命名和类型是否规范
- [ ] 小数类型是否使用了DECIMAL
- [ ] VARCHAR长度是否超过5000
- [ ] 表和字段是否有必要的注释
- [ ] 是否过早进行了分库分表设计
- [ ] 是否存在物理删除操作
