package com.translation.speech.service.impl;

import cn.hutool.json.JSONUtil;
import com.translation.speech.service.SpeechRecognitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Whisper语音识别服务实现
 * 可以替换为其他ASR模型实现
 */
@Slf4j
@Service
public class WhisperSpeechRecognitionServiceImpl implements SpeechRecognitionService {
    
    @Value("${speech.whisper.script.path:./speech-service/scripts/whisper_processor.py}")
    private String whisperScriptPath;
    
    @Value("${speech.whisper.model:base}")
    private String whisperModel;
    
    private static final String[] SUPPORTED_LANGUAGES = {
        "zh", "en", "ja", "ko", "es", "fr", "de", "ru", "it", "pt"
    };
    
    @Override
    public SpeechRecognitionResult recognizeAudio(String audioFilePath, String language, Map<String, Object> options) {
        long startTime = System.currentTimeMillis();
        File audioFile = new File(audioFilePath);
        
        if (!audioFile.exists()) {
            return new SpeechRecognitionResult(false, null, 0.0, 
                audioFile.getName(), "音频文件不存在: " + audioFilePath, 0L);
        }
        
        try {
            log.info("开始识别音频文件: {}", audioFile.getName());
            
            // 构建Python命令
            List<String> command = new ArrayList<>();
            command.add("python3");
            command.add(whisperScriptPath);
            command.add(audioFilePath);
            command.add("--language");
            command.add(language);
            command.add("--model");
            command.add(whisperModel);
            
            // 添加任务ID如果提供
            if (options != null && options.containsKey("taskId")) {
                command.add("--task_id");
                command.add(options.get("taskId").toString());
            }
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // 读取输出
            Scanner scanner = new Scanner(process.getInputStream());
            StringBuilder output = new StringBuilder();
            while (scanner.hasNextLine()) {
                output.append(scanner.nextLine()).append("\n");
            }
            scanner.close();
            
            int exitCode = process.waitFor();
            long processingTime = System.currentTimeMillis() - startTime;
            
            if (exitCode != 0) {
                log.error("Whisper处理失败: exitCode={}, output={}", exitCode, output.toString());
                return new SpeechRecognitionResult(false, null, 0.0, 
                    audioFile.getName(), "Whisper处理失败: " + output.toString(), processingTime);
            }
            
            // 解析Whisper输出
            String outputStr = output.toString().trim();
            try {
                Map<String, Object> result = JSONUtil.toBean(outputStr, Map.class);
                
                if (result.containsKey("success") && Boolean.TRUE.equals(result.get("success"))) {
                    String recognizedText = (String) result.get("text");
                    Double confidence = result.containsKey("confidence") ? 
                        ((Number) result.get("confidence")).doubleValue() : 0.8;
                    
                    log.info("音频识别成功: {} -> {}", audioFile.getName(), 
                            recognizedText.length() > 50 ? recognizedText.substring(0, 50) + "..." : recognizedText);
                    
                    return new SpeechRecognitionResult(true, recognizedText, confidence, 
                        audioFile.getName(), null, processingTime);
                } else {
                    String errorMsg = (String) result.get("error");
                    return new SpeechRecognitionResult(false, null, 0.0, 
                        audioFile.getName(), errorMsg, processingTime);
                }
                
            } catch (Exception e) {
                log.warn("解析Whisper输出失败，使用原始输出: {}", e.getMessage());
                // 如果JSON解析失败，尝试直接使用输出作为文本
                if (outputStr.length() > 0) {
                    return new SpeechRecognitionResult(true, outputStr, 0.5, 
                        audioFile.getName(), null, processingTime);
                } else {
                    return new SpeechRecognitionResult(false, null, 0.0, 
                        audioFile.getName(), "无法解析识别结果", processingTime);
                }
            }
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("语音识别异常: " + audioFile.getName(), e);
            return new SpeechRecognitionResult(false, null, 0.0, 
                audioFile.getName(), "识别异常: " + e.getMessage(), processingTime);
        }
    }
    
    @Override
    public BatchRecognitionResult batchRecognizeAudio(String audioDirectoryPath, String language, Map<String, Object> options) {
        File audioDir = new File(audioDirectoryPath);
        
        if (!audioDir.exists() || !audioDir.isDirectory()) {
            return new BatchRecognitionResult(false, Collections.emptyList(), 0, 0, 0, 
                "音频目录不存在: " + audioDirectoryPath);
        }
        
        // 获取所有MP3文件
        File[] mp3Files = audioDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));
        if (mp3Files == null || mp3Files.length == 0) {
            return new BatchRecognitionResult(false, Collections.emptyList(), 0, 0, 0, 
                "目录中没有MP3文件");
        }
        
        log.info("开始批量识别音频文件: 目录={}, 文件数={}", audioDirectoryPath, mp3Files.length);
        
        List<SpeechRecognitionResult> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        
        for (File mp3File : mp3Files) {
            SpeechRecognitionResult result = recognizeAudio(mp3File.getAbsolutePath(), language, options);
            results.add(result);
            
            if (result.isSuccess()) {
                successCount++;
                // 保存识别结果到文件
                saveRecognitionResult(mp3File, result, audioDirectoryPath);
            } else {
                failureCount++;
            }
        }
        
        boolean overallSuccess = successCount > 0;
        log.info("批量语音识别完成: 总数={}, 成功={}, 失败={}", mp3Files.length, successCount, failureCount);
        
        return new BatchRecognitionResult(overallSuccess, results, mp3Files.length, 
            successCount, failureCount, null);
    }
    
    /**
     * 保存识别结果到文件
     */
    private void saveRecognitionResult(File audioFile, SpeechRecognitionResult result, String audioDirectoryPath) {
        try {
            String baseName = audioFile.getName().replaceFirst("[.][^.]+$", "");
            Path textFilePath = Paths.get(audioDirectoryPath, baseName + "_recognition.txt");
            
            StringBuilder content = new StringBuilder();
            content.append("音频文件: ").append(audioFile.getName()).append("\n");
            content.append("识别时间: ").append(new Date()).append("\n");
            content.append("置信度: ").append(result.getConfidence()).append("\n");
            content.append("处理时间: ").append(result.getProcessingTimeMs()).append("ms\n");
            content.append("识别文本:\n").append(result.getRecognizedText());
            
            Files.write(textFilePath, content.toString().getBytes("UTF-8"));
            log.debug("识别结果已保存: {}", textFilePath);
            
        } catch (Exception e) {
            log.warn("保存识别结果失败: " + audioFile.getName(), e);
        }
    }
    
    @Override
    public boolean isModelAvailable() {
        try {
            // 检查Whisper脚本是否存在
            File scriptFile = new File(whisperScriptPath);
            if (!scriptFile.exists()) {
                log.warn("Whisper脚本不存在: {}", whisperScriptPath);
                return false;
            }
            
            // 简单的Python环境检查
            ProcessBuilder pb = new ProcessBuilder("python3", "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            return exitCode == 0;
        } catch (Exception e) {
            log.warn("检查Whisper模型可用性失败", e);
            return false;
        }
    }
}