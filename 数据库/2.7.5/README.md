# GB28181-2022 数据库迁移说明

## 概述
本目录包含支持 GB28181-2022 标准的数据库迁移脚本，用于在现有系统中添加协议版本和设备能力相关字段。

## 文件说明

### MySQL 版本
- **更新-mysql-gb28181-2022.sql**: 适用于 MySQL 数据库的迁移脚本

### PostgreSQL/Kingbase 版本
- **更新-postgresql-gb28181-2022.sql**: 适用于 PostgreSQL 或 Kingbase 数据库的迁移脚本

## 新增字段说明

| 字段名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| protocol_version | character varying(50) | 'GB28181-2016' | 设备支持的协议版本（GB28181-2011/2016/2022） |
| support_precision_ptz | bool | false | 是否支持精准云台控制（GB28181-2022 新特性） |
| support_cruise_track | bool | false | 是否支持巡航轨迹查询（GB28181-2022 新特性） |
| support_reverse_playback | bool | false | 是否支持反向回放（GB28181-2022 新特性） |
| support_drag_playback | bool | false | 是否支持拖拽回放（GB28181-2022 新特性） |
| support_fast_playback | bool | false | 是否支持快速回放（GB28181-2022 新特性） |
| support_slow_playback | bool | false | 是否支持慢速回放（GB28181-2022 新特性） |
| support_frame_by_frame_playback | bool | false | 是否支持逐帧回放（GB28181-2022 新特性） |
| support_storage_card_format | bool | false | 是否支持存储卡格式化（GB28181-2022 新特性） |
| support_device_upgrade | bool | false | 是否支持设备升级（GB28181-2022 新特性） |
| support_osd_config | bool | false | 是否支持 OSD 配置（GB28181-2022 新特性） |
| mobile_position_submission_interval_2022 | integer | 1 | 移动设备位置订阅周期（GB28181-2022 增强） |
| device_type_2022 | character varying(50) | null | 设备类型（GB28181-2022 扩展） |
| device_capabilities | text | null | 设备能力集信息（JSON 格式存储完整能力集） |

## 执行方法

### MySQL
```bash
mysql -u username -p database_name < 更新-mysql-gb28181-2022.sql
```

或在 MySQL 客户端中：
```sql
source /path/to/更新-mysql-gb28181-2022.sql;
```

### PostgreSQL/Kingbase
```bash
psql -U username -d database_name -f 更新-postgresql-gb28181-2022.sql
```

或在 psql 客户端中：
```sql
\i /path/to/更新-postgresql-gb28181-2022.sql
```

## 兼容性说明

1. **向后兼容**: 所有新增字段都有默认值，不会影响现有设备的正常使用
2. **渐进式升级**: 现有 GB28181-2016 设备可继续正常工作，新功能仅在设备支持时启用
3. **安全执行**: 脚本使用条件判断，重复执行不会报错

## 验证方法

执行迁移后，可通过以下 SQL 验证字段是否添加成功：

### MySQL
```sql
DESCRIBE wvp_device;
-- 或
SHOW COLUMNS FROM wvp_device LIKE '%protocol%';
```

### PostgreSQL/Kingbase
```sql
\d wvp_device
-- 或
SELECT column_name, data_type, column_default 
FROM information_schema.columns 
WHERE table_name = 'wvp_device' 
AND column_name LIKE '%protocol%';
```

## 注意事项

1. 执行迁移前请务必备份数据库
2. 建议在测试环境先验证迁移脚本
3. 生产环境执行前请在维护窗口进行
4. Kingbase 数据库请使用 PostgreSQL 版本脚本

## 后续工作

数据库迁移完成后，需要：
1. 更新 Java 实体类（已完成）
2. 更新 SIP 信令处理逻辑
3. 更新设备注册流程
4. 更新设备能力集获取和解析逻辑
5. 前端界面适配新字段显示
