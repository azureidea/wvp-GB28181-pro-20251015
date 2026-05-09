package com.genersoft.iot.vmp.gb28181.protocol;

/**
 * GB28181协议版本枚举
 */
public enum GBProtocolVersion {
    /**
     * GB28181-2011
     */
    V2011("2011", "GB/T 28181-2011"),
    
    /**
     * GB28181-2016
     */
    V2016("2016", "GB/T 28181-2016"),
    
    /**
     * GB28181-2022
     */
    V2022("2022", "GB/T 28181-2022");
    
    private final String code;
    private final String description;
    
    GBProtocolVersion(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据版本号获取协议版本
     */
    public static GBProtocolVersion fromCode(String code) {
        for (GBProtocolVersion version : values()) {
            if (version.code.equals(code)) {
                return version;
            }
        }
        // 默认返回2016版本
        return V2016;
    }
    
    /**
     * 是否支持2022新特性
     */
    public boolean isSupport2022Features() {
        return this == V2022;
    }
    
    /**
     * 是否支持精准PTZ控制
     */
    public boolean isSupportPrecisionPTZ() {
        return this == V2022;
    }
    
    /**
     * 是否支持巡航轨迹
     */
    public boolean isSupportCruiseTrack() {
        return this == V2022;
    }
    
    /**
     * 是否支持录像倒放
     */
    public boolean isSupportReversePlayback() {
        return this == V2022;
    }
}
