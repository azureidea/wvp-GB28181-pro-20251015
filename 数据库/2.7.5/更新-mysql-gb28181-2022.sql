/*
* GB28181-2022 支持 - 设备表新增字段
* 添加协议版本和设备能力相关字段到 wvp_device 表
*/

-- MySQL 版本
DELIMITER //  -- 重定义分隔符避免分号冲突

CREATE PROCEDURE `wvp_gb28181_2022_device_update`()
BEGIN
    -- 添加协议版本字段
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'protocol_version')
    THEN
        ALTER TABLE wvp_device ADD protocol_version character varying(50) DEFAULT 'GB28181-2016';
    END IF;

    -- 添加精准云台支持标识
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'support_precision_ptz')
    THEN
        ALTER TABLE wvp_device ADD support_precision_ptz bool default false;
    END IF;

    -- 添加巡航轨迹支持标识
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'support_cruise_track')
    THEN
        ALTER TABLE wvp_device ADD support_cruise_track bool default false;
    END IF;

    -- 添加反向回放支持标识
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'support_reverse_playback')
    THEN
        ALTER TABLE wvp_device ADD support_reverse_playback bool default false;
    END IF;

    -- 添加拖拽回放支持标识
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'support_drag_playback')
    THEN
        ALTER TABLE wvp_device ADD support_drag_playback bool default false;
    END IF;

    -- 添加快速回放支持标识
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'support_fast_playback')
    THEN
        ALTER TABLE wvp_device ADD support_fast_playback bool default false;
    END IF;

    -- 添加慢速回放支持标识
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'support_slow_playback')
    THEN
        ALTER TABLE wvp_device ADD support_slow_playback bool default false;
    END IF;

    -- 添加逐帧回放支持标识
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'support_frame_by_frame_playback')
    THEN
        ALTER TABLE wvp_device ADD support_frame_by_frame_playback bool default false;
    END IF;

    -- 添加存储卡格式化支持标识
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'support_storage_card_format')
    THEN
        ALTER TABLE wvp_device ADD support_storage_card_format bool default false;
    END IF;

    -- 添加设备升级支持标识
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'support_device_upgrade')
    THEN
        ALTER TABLE wvp_device ADD support_device_upgrade bool default false;
    END IF;

    -- 添加 OSD 配置支持标识
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'support_osd_config')
    THEN
        ALTER TABLE wvp_device ADD support_osd_config bool default false;
    END IF;

    -- 添加移动设备位置订阅周期（GB28181-2022 新特性）
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'mobile_position_submission_interval_2022')
    THEN
        ALTER TABLE wvp_device ADD mobile_position_submission_interval_2022 integer DEFAULT 1;
    END IF;

    -- 添加设备类型（GB28181-2022 扩展）
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'device_type_2022')
    THEN
        ALTER TABLE wvp_device ADD device_type_2022 character varying(50);
    END IF;

    -- 添加设备能力集信息（JSON 格式存储完整能力集）
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns
                   WHERE TABLE_SCHEMA = (SELECT DATABASE()) 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'device_capabilities')
    THEN
        ALTER TABLE wvp_device ADD device_capabilities text;
    END IF;

END; //

call wvp_gb28181_2022_device_update();
DROP PROCEDURE wvp_gb28181_2022_device_update;

DELIMITER ;
