package com.genersoft.iot.vmp.dahua.audio;

import com.genersoft.iot.vmp.dahua.bean.IntercomSession;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 音频转码器
 * 使用FFmpeg将大华设备的G.711/ADPCM音频转换为OPUS/AAC
 */
@Slf4j
public class FfmpegAudioTranscoder {
    
    private final IntercomSession session;
    private final ExecutorService transcoderExecutor;
    private final BlockingQueue<byte[]> inputQueue;
    private final AudioOutputListener outputListener;
    private final AtomicBoolean running;
    
    // 音频参数
    private final int inputCodec;       // 输入编码 (G.711A/G.711U/ADPCM)
    private final int outputCodec;      // 输出编码 (OPUS/AAC)
    private final int sampleRate;       // 采样率
    private final int channels;         // 声道数
    
    /**
     * 音频输出监听器接口
     */
    public interface AudioOutputListener {
        void onAudioData(byte[] data, long timestamp);
    }
    
    public FfmpegAudioTranscoder(IntercomSession session, 
                                  int inputCodec, 
                                  int outputCodec,
                                  AudioOutputListener outputListener) {
        this.session = session;
        this.inputCodec = inputCodec;
        this.outputCodec = outputCodec;
        this.sampleRate = session.getSampleRate();
        this.channels = session.getChannels();
        this.outputListener = outputListener;
        this.running = new AtomicBoolean(false);
        this.inputQueue = new ArrayBlockingQueue<>(100);
        this.transcoderExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "AudioTranscoder-" + session.getSessionId());
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * 启动转码器
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            session.setTranscoding(new java.util.concurrent.atomic.AtomicBoolean(true));
            transcoderExecutor.submit(this::transcodeLoop);
            log.info("Audio transcoder started for session: {}", session.getSessionId());
        }
    }
    
    /**
     * 停止转码器
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            session.setTranscoding(new java.util.concurrent.atomic.AtomicBoolean(false));
            inputQueue.clear();
            transcoderExecutor.shutdownNow();
            log.info("Audio transcoder stopped for session: {}", session.getSessionId());
        }
    }
    
    /**
     * 提交音频数据进行转码
     * @param audioData 原始音频数据（G.711/ADPCM）
     */
    public void submitAudioData(byte[] audioData) {
        if (!running.get()) {
            return;
        }
        
        try {
            if (!inputQueue.offer(audioData, 1000, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                log.warn("Audio queue full, dropping frame for session: {}", session.getSessionId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 转码主循环
     */
    private void transcodeLoop() {
        log.info("Transcoder loop started, inputCodec={}, outputCodec={}", inputCodec, outputCodec);
        
        // TODO: 初始化FFmpeg编解码器
        // 实际实现需要调用JNA接口初始化FFmpeg
        
        long frameCount = 0;
        long startTime = System.currentTimeMillis();
        
        while (running.get()) {
            try {
                byte[] inputData = inputQueue.take();
                
                // TODO: 使用FFmpeg进行转码
                // 1. 解码G.711/ADPCM到PCM
                // 2. 重采样（如果需要）
                // 3. 编码为OPUS/AAC
                
                // 模拟转码延迟（实际应由FFmpeg处理）
                byte[] outputData = transcodeFrame(inputData);
                
                if (outputData != null && outputListener != null) {
                    long timestamp = System.currentTimeMillis() - startTime;
                    outputListener.onAudioData(outputData, timestamp);
                }
                
                frameCount++;
                if (frameCount % 100 == 0) {
                    log.debug("Transcoded {} frames for session: {}", frameCount, session.getSessionId());
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error during audio transcoding", e);
            }
        }
        
        // TODO: 释放FFmpeg资源
        log.info("Transcoder loop finished, total frames: {}", frameCount);
    }
    
    /**
     * 转码单帧音频
     * 这是一个简化实现，实际需要调用FFmpeg
     */
    private byte[] transcodeFrame(byte[] inputData) {
        // 对于G.711，可以直接封装为RTP或转换为OPUS
        // 这里返回原始数据作为占位实现
        // 实际项目需要集成FFmpeg或使用Java音频库
        
        if (inputCodec == FFmpegLib.AV_CODEC_ID_PCM_ALAW || 
            inputCodec == FFmpegLib.AV_CODEC_ID_PCM_MULAW) {
            // G.711转PCM（简单展开）
            return g711ToPcm(inputData, inputCodec == FFmpegLib.AV_CODEC_ID_PCM_ALAW);
        }
        
        // 其他编码格式需要FFmpeg处理
        return inputData;
    }
    
    /**
     * G.711 A-law/U-law转PCM
     */
    private byte[] g711ToPcm(byte[] g711Data, boolean isALaw) {
        byte[] pcmData = new byte[g711Data.length * 2];
        
        for (int i = 0; i < g711Data.length; i++) {
            short pcmValue = isALaw ? alaw2linear(g711Data[i]) : ulaw2linear(g711Data[i]);
            pcmData[i * 2] = (byte) (pcmValue & 0xFF);
            pcmData[i * 2 + 1] = (byte) ((pcmValue >> 8) & 0xFF);
        }
        
        return pcmData;
    }
    
    /**
     * A-law转线性PCM
     */
    private static short alaw2linear(byte aVal) {
        int t, seg;
        
        aVal ^= 0x55;
        t = (aVal & 0x7f) << 4;
        seg = (aVal & 0x70) >> 4;
        
        switch (seg) {
            case 0: t += 8; break;
            case 1: t += 0x108; break;
            default: t += 0x108; t <<= seg - 1;
        }
        
        return (short) ((aVal & 0x80) != 0 ? t : -t);
    }
    
    /**
     * U-law转线性PCM
     */
    private static short ulaw2linear(byte uVal) {
        int t;
        
        uVal = (byte) ~uVal;
        t = ((uVal & 0x7f) << 4) + 8;
        t = ((t << ((uVal & 0x70) >> 4)) - 8);
        
        return (short) ((uVal & 0x80) != 0 ? -t : t);
    }
}
