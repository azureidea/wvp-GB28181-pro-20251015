package com.genersoft.iot.vmp.dahua.config;

import com.genersoft.iot.vmp.dahua.handler.DahuaServerHandler;
import com.genersoft.iot.vmp.dahua.service.DahuaService;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 大华设备Netty通道初始化器
 */
@Slf4j
public class DahuaChannelInitializer extends ChannelInitializer<SocketChannel> {
    
    private final DahuaService dahuaService;
    private final int heartbeatTimeout;
    
    public DahuaChannelInitializer(DahuaService dahuaService) {
        this(dahuaService, 60);
    }
    
    public DahuaChannelInitializer(DahuaService dahuaService, int heartbeatTimeout) {
        this.dahuaService = dahuaService;
        this.heartbeatTimeout = heartbeatTimeout;
    }
    
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        // 添加空闲检测（心跳保活）
        ch.pipeline().addLast(new IdleStateHandler(
            heartbeatTimeout,  // 读空闲超时（秒）
            0,                 // 写空闲超时
            0,                 // 总空闲超时
            TimeUnit.SECONDS
        ));
        
        // 添加长度字段帧解码器
        // 大华协议头格式：[Magic:2][Version:2][CmdType:4][Seq:4][Length:4][Payload...]
        // length字段偏移量 = 2+2+4+4 = 12，长度字段本身占4字节
        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(
            1048576,  // 最大帧长度 (1MB)
            12,       // 长度字段偏移量
            4,        // 长度字段长度（字节）
            4,        // 长度调整值（不包括长度字段本身）
            0         // 初始跳过字节数
        ));
        
        // 添加业务处理器
        ch.pipeline().addLast(new DahuaServerHandler(dahuaService));
        
        log.debug("Dahua channel initialized: {}", ch.remoteAddress());
    }
}
