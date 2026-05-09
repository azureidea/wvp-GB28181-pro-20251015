package com.genersoft.iot.vmp.isup.config;

import com.genersoft.iot.vmp.isup.handler.IsupServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * 海康ISUP协议服务器配置与启动类
 * 
 * 配置项：
 * - isup.server.port: ISUP服务器监听端口（默认：7660）
 * - isup.server.backlog: TCP连接队列长度（默认：1024）
 * - isup.server.so-keepalive: TCP保活（默认：true）
 * - isup.server.idle-timeout: 空闲超时时间（分钟，默认：10）
 */
@Slf4j
@Component
public class IsupServerConfig implements CommandLineRunner {
    
    /**
     * ISUP服务器监听端口
     */
    @Value("${isup.server.port:7660}")
    private Integer port;
    
    /**
     * TCP连接队列长度
     */
    @Value("${isup.server.backlog:1024}")
    private Integer backlog;
    
    /**
     * TCP保活
     */
    @Value("${isup.server.so-keepalive:true}")
    private Boolean soKeepalive;
    
    /**
     * 空闲超时时间（分钟）
     */
    @Value("${isup.server.idle-timeout:10}")
    private Integer idleTimeout;
    
    /**
     * 事件循环组
     */
    private EventLoopGroup bossGroup;
    
    /**
     * 工作线程组
     */
    private EventLoopGroup workerGroup;
    
    /**
     * 服务通道
     */
    private Channel serverChannel;
    
    /**
     * 是否正在运行
     */
    private volatile boolean isRunning = false;
    
    /**
     * Spring事件发布器
     */
    private final ApplicationEventPublisher applicationEventPublisher;
    
    public IsupServerConfig(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
    
    @Override
    public void run(String... args) throws Exception {
        start();
    }
    
    /**
     * 启动ISUP服务器
     */
    public synchronized void start() {
        if (isRunning) {
            log.warn("ISUP服务器已经启动，端口：{}", port);
            return;
        }
        
        try {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(NioChannelOption.SO_BACKLOG, backlog)
                    .option(NioChannelOption.SO_REUSEADDR, true)
                    .childOption(NioChannelOption.TCP_NODELAY, true)
                    .childOption(NioChannelOption.SO_KEEPALIVE, soKeepalive)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            // 空闲检测
                            pipeline.addLast(new IdleStateHandler(idleTimeout, 0, 0, TimeUnit.MINUTES));
                            
                            // ISUP协议处理器
                            pipeline.addLast(new IsupServerHandler());
                        }
                    });
            
            ChannelFuture future = bootstrap.bind(port).sync();
            future.addListener((ChannelFutureListener) f -> {
                if (f.isSuccess()) {
                    log.info("=============================================");
                    log.info("海康ISUP服务器启动成功");
                    log.info("监听端口：{}", port);
                    log.info("最大连接数：{}", backlog);
                    log.info("空闲超时：{} 分钟", idleTimeout);
                    log.info("=============================================");
                } else {
                    log.error("ISUP服务器绑定端口失败：{}, 原因：{}", port, f.cause().getMessage());
                }
            });
            
            serverChannel = future.channel();
            isRunning = true;
            
            // 等待服务关闭
            serverChannel.closeFuture().sync();
            
        } catch (Exception e) {
            log.error("ISUP服务器启动异常", e);
        } finally {
            stop();
        }
    }
    
    /**
     * 停止ISUP服务器
     */
    public synchronized void stop() {
        if (!isRunning) {
            return;
        }
        
        log.info("正在停止ISUP服务器...");
        
        isRunning = false;
        
        // 关闭服务通道
        if (serverChannel != null) {
            serverChannel.close();
        }
        
        // 优雅关闭线程组
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        
        log.info("ISUP服务器已停止");
    }
    
    /**
     * Spring容器销毁时回调
     */
    @PreDestroy
    public void destroy() {
        stop();
    }
    
    /**
     * 检查服务器是否运行中
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * 获取监听端口
     */
    public Integer getPort() {
        return port;
    }
}
