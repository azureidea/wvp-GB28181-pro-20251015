package com.genersoft.iot.vmp.dahua.audio;

import net.java.dev.jna.Library;
import net.java.dev.jna.Native;
import net.java.dev.jna.Pointer;

/**
 * FFmpeg音频转码接口（通过JNA调用）
 * 用于将大华设备的G.711/ADPCM音频转换为OPUS/AAC
 */
public interface FFmpegLib extends Library {
    
    FFmpegLib INSTANCE = Native.load("avcodec", FFmpegLib.class);
    
    /**
     * 查找解码器
     */
    Pointer avcodec_find_decoder(int codecId);
    
    /**
     * 查找编码器
     */
    Pointer avcodec_find_encoder(int codecId);
    
    /**
     * 分配编解码器上下文
     */
    Pointer avcodec_alloc_context3(Pointer codec);
    
    /**
     * 打开编解码器
     */
    int avcodec_open2(Pointer ctx, Pointer codec, Pointer options);
    
    /**
     * 分配音频帧
     */
    Pointer av_frame_alloc();
    
    /**
     * 发送数据包进行解码
     */
    int avcodec_send_packet(Pointer ctx, Pointer pkt);
    
    /**
     * 接收解码后的帧
     */
    int avcodec_receive_frame(Pointer ctx, Pointer frame);
    
    /**
     * 发送帧进行编码
     */
    int avcodec_send_frame(Pointer ctx, Pointer frame);
    
    /**
     * 接收编码后的数据包
     */
    int avcodec_receive_packet(Pointer ctx, Pointer pkt);
    
    /**
     * 释放编解码器上下文
     */
    void avcodec_free_context(Pointer[] ctx);
    
    /**
     * 释放帧
     */
    void av_frame_free(Pointer[] frame);
    
    /**
     * 初始化音频重采样上下文
     */
    Pointer swr_alloc_set_opts(Pointer ctx, 
                               long out_ch_layout, int out_sample_fmt, int out_sample_rate,
                               long in_ch_layout, int in_sample_fmt, int in_sample_rate,
                               int log_offset, Pointer log_ctx);
    
    /**
     * 初始化重采样上下文
     */
    int swr_init(Pointer ctx);
    
    /**
     * 重采样音频
     */
    int swr_convert(Pointer ctx, Pointer[] out, int out_count, Pointer[] in, int in_count);
    
    /**
     * 关闭重采样上下文
     */
    void swr_close(Pointer ctx);
    
    /**
     * 释放重采样上下文
     */
    void swr_free(Pointer[] ctx);
    
    // 编解码器ID常量
    int AV_CODEC_ID_PCM_ALAW = 65562;
    int AV_CODEC_ID_PCM_MULAW = 65563;
    int AV_CODEC_ID_ADPCM_G726 = 69640;
    int AV_CODEC_ID_OPUS = 86018;
    int AV_CODEC_ID_AAC = 86016;
    
    // 样本格式常量
    int AV_SAMPLE_FMT_S16 = 1;
    int AV_SAMPLE_FMT_FLT = 3;
}
