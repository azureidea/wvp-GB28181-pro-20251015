package com.genersoft.iot.vmp.dahua.config;

import com.genersoft.iot.vmp.dahua.handler.DahuaServerHandler;
import com.genersoft.iot.vmp.dahua.service.DahuaService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

/**
 * 大华设备接入服务器启动器
 */
@Slf4j
@Component
public class DahuaServerStarter {
    
    @Value("${dahua.enabled:true}")
    private boolean enabled;
    
    @Value("${dahua.port:9080}")
    private int port;
    
    @Value("${dahua.boss-threads:1}")
    private int bossThreads;
    
    @Value("${dahua.worker-threads:0}")
    private int workerThreads;
    
    @Autowired
    private DahuaService dahuaService;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture serverChannel;
    
    @PostConstruct
    public void start() throws Exception {
        if (!enabled) {
            log.info("Dahua server is disabled, skipping startup");
            return;
        }
        
        log.info("Starting Dahua server on port {}", port);
        
        // 初始化线程组
        bossGroup = new NioEventLoopGroup(bossThreads > 0 ? bossThreads : 1);
        workerGroup = new NioEventLoopGroup(workerThreads > 0 ? workerThreads : Runtime.getRuntime().availableProcessors() * 2);
        
        // 创建并配置ServerBootstrap
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new DahuaChannelInitializer(dahuaService))
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);
        
        // 绑定端口并启动
        serverChannel = bootstrap.bind(new InetSocketAddress(port)).sync();
        
        log.info("Dahua server started successfully on port {}", port);
    }
    
    @PreDestroy
    public void stop() {
        if (!enabled) {
            return;
        }
        
        log.info("Shutting down Dahua server...");
        
        try {
            if (serverChannel != null) {
                serverChannel.channel().close().sync();
            }
            
            if (bossGroup != null) {
                bossGroup.shutdownGracefully().sync();
            }
            
            if (workerGroup != null) {
                workerGroup.shutdownGracefully().sync();
            }
            
            log.info("Dahua server stopped successfully");
        } catch (Exception e) {
            log.error("Error stopping Dahua server", e);
        }
    }
}
