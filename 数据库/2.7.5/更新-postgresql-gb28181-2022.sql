/*
* GB28181-2022 支持 - 设备表新增字段
* 添加协议版本和设备能力相关字段到 wvp_device 表
* PostgreSQL/Kingbase 版本
*/

DO $$
BEGIN
    -- 添加协议版本字段
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'public' 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'protocol_version') THEN
        ALTER TABLE wvp_device ADD COLUMN protocol_version character varying(50) DEFAULT 'GB28181-2016';
    END IF;

    -- 添加精准云台支持标识
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'public' 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'support_precision_ptz') THEN
        ALTER TABLE wvp_device ADD COLUMN support_precision_ptz boolean default false;
    END IF;

    -- 添加巡航轨迹支持标识
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'public' 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'support_cruise_track') THEN
        ALTER TABLE wvp_device ADD COLUMN support_cruise_track boolean default false;
    END IF;

    -- 添加反向回放支持标识
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'public' 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'support_reverse_playback') THEN
        ALTER TABLE wvp_device ADD COLUMN support_reverse_playback boolean default false;
    END IF;

    -- 添加拖拽回放支持标识
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'public' 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'support_drag_playback') THEN
        ALTER TABLE wvp_device ADD COLUMN support_drag_playback boolean default false;
    END IF;

    -- 添加快速回放支持标识
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'public' 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'support_fast_playback') THEN
        ALTER TABLE wvp_device ADD COLUMN support_fast_playback boolean default false;
    END IF;

    -- 添加慢速回放支持标识
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'public' 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'support_slow_playback') THEN
        ALTER TABLE wvp_device ADD COLUMN support_slow_playback boolean default false;
    END IF;

    -- 添加逐帧回放支持标识
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'public' 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'support_frame_by_frame_playback') THEN
        ALTER TABLE wvp_device ADD COLUMN support_frame_by_frame_playback boolean default false;
    END IF;

    -- 添加存储卡格式化支持标识
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'public' 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'support_storage_card_format') THEN
        ALTER TABLE wvp_device ADD COLUMN support_storage_card_format boolean default false;
    END IF;

    -- 添加设备升级支持标识
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'public' 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'support_device_upgrade') THEN
        ALTER TABLE wvp_device ADD COLUMN support_device_upgrade boolean default false;
    END IF;

    -- 添加 OSD 配置支持标识
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'public' 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'support_osd_config') THEN
        ALTER TABLE wvp_device ADD COLUMN support_osd_config boolean default false;
    END IF;

    -- 添加移动设备位置订阅周期（GB28181-2022 新特性）
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'public' 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'mobile_position_submission_interval_2022') THEN
        ALTER TABLE wvp_device ADD COLUMN mobile_position_submission_interval_2022 integer DEFAULT 1;
    END IF;

    -- 添加设备类型（GB28181-2022 扩展）
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'public' 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'device_type_2022') THEN
        ALTER TABLE wvp_device ADD COLUMN device_type_2022 character varying(50);
    END IF;

    -- 添加设备能力集信息（JSON 格式存储完整能力集）
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'public' 
                   AND table_name = 'wvp_device' 
                   AND column_name = 'device_capabilities') THEN
        ALTER TABLE wvp_device ADD COLUMN device_capabilities text;
    END IF;
END $$;
