package com.translation.translate.service.impl;

import cn.hutool.core.util.StrUtil;
import com.translation.common.enums.ErrorCode;
import com.translation.common.exception.BusinessException;
import com.translation.translate.dto.ValidationRequest;
import com.translation.translate.dto.ValidationResponse;
import com.translation.translate.service.TextValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 文本校验服务实现类
 * 实现STT识别结果与原始文本的准确性验证
 */
@Slf4j
@Service
public class TextValidationServiceImpl implements TextValidationService {

    // 常见的标点符号
    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("[\\p{Punct}\\s]+");
    
    // 中文字符匹配
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]+");
    
    // 英文字符匹配
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]+");
    
    // 日文字符匹配
    private static final Pattern JAPANESE_PATTERN = Pattern.compile("[\\u3040-\\u309f\\u30a0-\\u30ff\\u4e00-\\u9faf]+");

    @Override
    public ValidationResponse validateText(ValidationRequest request) {
        log.info("开始文本校验，任务ID: {}", request.getTaskId());

        try {
            // 验证请求参数
            validateRequest(request);

            // 计算相似度
            double similarityScore = calculateSimilarity(
                    request.getOriginalText(), 
                    request.getRecognizedText()
            );

            // 检测语言一致性
            String originalLanguage = detectLanguage(request.getOriginalText());
            String recognizedLanguage = detectLanguage(request.getRecognizedText());
            boolean languageConsistent = Objects.equals(originalLanguage, recognizedLanguage);

            // 生成纠错建议
            Object correctSuggestions = getCorrectSuggestions(
                    request.getOriginalText(), 
                    request.getRecognizedText()
            );

            // 计算准确性等级
            String accuracyLevel = calculateAccuracyLevel(similarityScore);

            // 构建响应
            ValidationResponse response = new ValidationResponse();
            response.setTaskId(request.getTaskId());
            response.setSimilarityScore(similarityScore);
            response.setAccuracyLevel(accuracyLevel);
            response.setLanguageConsistent(languageConsistent);
            response.setOriginalLanguage(originalLanguage);
            response.setRecognizedLanguage(recognizedLanguage);
            response.setValidationPassed(similarityScore >= request.getThreshold());
            response.setCorrectSuggestions(correctSuggestions);
            response.setTimestamp(LocalDateTime.now());

            // 详细分析
            Map<String, Object> detailedAnalysis = performDetailedAnalysis(
                    request.getOriginalText(), 
                    request.getRecognizedText()
            );
            response.setDetailedAnalysis(detailedAnalysis);

            log.info("文本校验完成，任务ID: {}, 相似度: {}, 验证通过: {}", 
                    request.getTaskId(), similarityScore, response.getValidationPassed());

            return response;

        } catch (Exception e) {
            log.error("文本校验失败，任务ID: {}", request.getTaskId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文本校验失败: " + e.getMessage());
        }
    }

    @Override
    public double calculateSimilarity(String text1, String text2) {
        if (StrUtil.isBlank(text1) || StrUtil.isBlank(text2)) {
            return 0.0;
        }

        // 标准化文本（去除标点符号，转小写）
        String normalizedText1 = normalizeText(text1);
        String normalizedText2 = normalizeText(text2);

        // 使用多种算法计算相似度
        double levenshteinSimilarity = calculateLevenshteinSimilarity(normalizedText1, normalizedText2);
        double jaccardSimilarity = calculateJaccardSimilarity(normalizedText1, normalizedText2);
        double cosineSimilarity = calculateCosineSimilarity(normalizedText1, normalizedText2);

        // 加权平均
        double weightedSimilarity = 
                levenshteinSimilarity * 0.4 + 
                jaccardSimilarity * 0.3 + 
                cosineSimilarity * 0.3;

        return Math.round(weightedSimilarity * 1000.0) / 1000.0;
    }

    @Override
    public String detectLanguage(String text) {
        if (StrUtil.isBlank(text)) {
            return "unknown";
        }

        // 统计各种字符的比例
        int totalChars = text.replaceAll("\\s+", "").length();
        if (totalChars == 0) {
            return "unknown";
        }

        int chineseCount = countMatches(text, CHINESE_PATTERN);
        int englishCount = countMatches(text, ENGLISH_PATTERN);
        int japaneseCount = countMatches(text, JAPANESE_PATTERN);

        double chineseRatio = (double) chineseCount / totalChars;
        double englishRatio = (double) englishCount / totalChars;
        double japaneseRatio = (double) japaneseCount / totalChars;

        // 根据字符比例判断语言
        if (chineseRatio > 0.5) {
            return "zh";
        } else if (englishRatio > 0.7) {
            return "en";
        } else if (japaneseRatio > 0.3) {
            return "ja";
        } else if (chineseRatio > 0.2) {
            return "zh";
        } else if (englishRatio > 0.3) {
            return "en";
        } else {
            return "unknown";
        }
    }

    @Override
    public Object getCorrectSuggestions(String originalText, String recognizedText) {
        Map<String, Object> suggestions = new HashMap<>();

        try {
            // 分析差异
            List<Map<String, Object>> differences = findDifferences(originalText, recognizedText);
            suggestions.put("differences", differences);

            // 常见错误模式
            List<String> commonErrors = identifyCommonErrors(originalText, recognizedText);
            suggestions.put("commonErrors", commonErrors);

            // 改进建议
            List<String> improvements = generateImprovements(originalText, recognizedText);
            suggestions.put("improvements", improvements);

            // 置信度评估
            double confidenceScore = assessConfidence(originalText, recognizedText);
            suggestions.put("confidenceScore", confidenceScore);

        } catch (Exception e) {
            log.error("生成纠错建议失败", e);
            suggestions.put("error", "无法生成纠错建议: " + e.getMessage());
        }

        return suggestions;
    }

    /**
     * 验证请求参数
     */
    private void validateRequest(ValidationRequest request) {
        if (StrUtil.isBlank(request.getOriginalText())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "原始文本不能为空");
        }
        
        if (StrUtil.isBlank(request.getRecognizedText())) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "识别文本不能为空");
        }
        
        if (request.getThreshold() < 0.0 || request.getThreshold() > 1.0) {
            throw new BusinessException(ErrorCode.INVALID_PARAM, "相似度阈值必须在0.0-1.0之间");
        }
    }

    /**
     * 标准化文本
     */
    private String normalizeText(String text) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        
        // 去除标点符号和多余空格
        String normalized = PUNCTUATION_PATTERN.matcher(text).replaceAll(" ");
        
        // 转换为小写并去除首尾空格
        return normalized.toLowerCase().trim();
    }

    /**
     * 计算编辑距离相似度
     */
    private double calculateLevenshteinSimilarity(String text1, String text2) {
        int maxLength = Math.max(text1.length(), text2.length());
        if (maxLength == 0) {
            return 1.0;
        }
        
        int distance = calculateLevenshteinDistance(text1, text2);
        return 1.0 - (double) distance / maxLength;
    }

    /**
     * 计算编辑距离
     */
    private int calculateLevenshteinDistance(String text1, String text2) {
        int[][] dp = new int[text1.length() + 1][text2.length() + 1];
        
        for (int i = 0; i <= text1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= text2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= text1.length(); i++) {
            for (int j = 1; j <= text2.length(); j++) {
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]) + 1;
                }
            }
        }
        
        return dp[text1.length()][text2.length()];
    }

    /**
     * 计算Jaccard相似度
     */
    private double calculateJaccardSimilarity(String text1, String text2) {
        Set<String> words1 = new HashSet<>(Arrays.asList(text1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.split("\\s+")));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * 计算余弦相似度
     */
    private double calculateCosineSimilarity(String text1, String text2) {
        Map<String, Integer> vector1 = getWordVector(text1);
        Map<String, Integer> vector2 = getWordVector(text2);
        
        Set<String> allWords = new HashSet<>();
        allWords.addAll(vector1.keySet());
        allWords.addAll(vector2.keySet());
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (String word : allWords) {
            int count1 = vector1.getOrDefault(word, 0);
            int count2 = vector2.getOrDefault(word, 0);
            
            dotProduct += count1 * count2;
            norm1 += count1 * count1;
            norm2 += count2 * count2;
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 获取词向量
     */
    private Map<String, Integer> getWordVector(String text) {
        Map<String, Integer> vector = new HashMap<>();
        String[] words = text.split("\\s+");
        
        for (String word : words) {
            if (!word.isEmpty()) {
                vector.put(word, vector.getOrDefault(word, 0) + 1);
            }
        }
        
        return vector;
    }

    /**
     * 统计匹配数量
     */
    private int countMatches(String text, Pattern pattern) {
        int count = 0;
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * 计算准确性等级
     */
    private String calculateAccuracyLevel(double similarity) {
        if (similarity >= 0.95) {
            return "EXCELLENT";
        } else if (similarity >= 0.85) {
            return "GOOD";
        } else if (similarity >= 0.7) {
            return "FAIR";
        } else if (similarity >= 0.5) {
            return "POOR";
        } else {
            return "VERY_POOR";
        }
    }

    /**
     * 执行详细分析
     */
    private Map<String, Object> performDetailedAnalysis(String originalText, String recognizedText) {
        Map<String, Object> analysis = new HashMap<>();
        
        try {
            // 长度比较
            analysis.put("originalLength", originalText.length());
            analysis.put("recognizedLength", recognizedText.length());
            analysis.put("lengthDifference", Math.abs(originalText.length() - recognizedText.length()));
            
            // 词数比较
            int originalWords = originalText.split("\\s+").length;
            int recognizedWords = recognizedText.split("\\s+").length;
            analysis.put("originalWords", originalWords);
            analysis.put("recognizedWords", recognizedWords);
            analysis.put("wordDifference", Math.abs(originalWords - recognizedWords));
            
            // 字符统计
            analysis.put("characterAnalysis", analyzeCharacters(originalText, recognizedText));
            
        } catch (Exception e) {
            log.error("详细分析失败", e);
            analysis.put("error", "分析失败: " + e.getMessage());
        }
        
        return analysis;
    }

    /**
     * 分析字符差异
     */
    private Map<String, Object> analyzeCharacters(String originalText, String recognizedText) {
        Map<String, Object> charAnalysis = new HashMap<>();
        
        // 统计字符类型
        charAnalysis.put("original", getCharacterStats(originalText));
        charAnalysis.put("recognized", getCharacterStats(recognizedText));
        
        return charAnalysis;
    }

    /**
     * 获取字符统计
     */
    private Map<String, Integer> getCharacterStats(String text) {
        Map<String, Integer> stats = new HashMap<>();
        
        stats.put("chinese", countMatches(text, CHINESE_PATTERN));
        stats.put("english", countMatches(text, ENGLISH_PATTERN));
        stats.put("japanese", countMatches(text, JAPANESE_PATTERN));
        stats.put("total", text.replaceAll("\\s+", "").length());
        
        return stats;
    }

    /**
     * 查找文本差异
     */
    private List<Map<String, Object>> findDifferences(String originalText, String recognizedText) {
        List<Map<String, Object>> differences = new ArrayList<>();
        
        String[] originalWords = originalText.split("\\s+");
        String[] recognizedWords = recognizedText.split("\\s+");
        
        int minLength = Math.min(originalWords.length, recognizedWords.length);
        
        for (int i = 0; i < minLength; i++) {
            if (!originalWords[i].equals(recognizedWords[i])) {
                Map<String, Object> diff = new HashMap<>();
                diff.put("position", i);
                diff.put("original", originalWords[i]);
                diff.put("recognized", recognizedWords[i]);
                diff.put("type", "SUBSTITUTION");
                differences.add(diff);
            }
        }
        
        // 处理长度差异
        if (originalWords.length != recognizedWords.length) {
            Map<String, Object> lengthDiff = new HashMap<>();
            lengthDiff.put("type", "LENGTH_DIFFERENCE");
            lengthDiff.put("originalLength", originalWords.length);
            lengthDiff.put("recognizedLength", recognizedWords.length);
            differences.add(lengthDiff);
        }
        
        return differences;
    }

    /**
     * 识别常见错误
     */
    private List<String> identifyCommonErrors(String originalText, String recognizedText) {
        List<String> errors = new ArrayList<>();
        
        // 检查常见的语音识别错误模式
        if (originalText.length() > recognizedText.length() * 1.2) {
            errors.add("识别结果过短，可能存在漏识别");
        }
        
        if (recognizedText.length() > originalText.length() * 1.2) {
            errors.add("识别结果过长，可能存在误识别");
        }
        
        // 检查数字识别
        if (originalText.matches(".*\\d+.*") && !recognizedText.matches(".*\\d+.*")) {
            errors.add("数字识别可能有误");
        }
        
        // 检查标点符号
        if (originalText.replaceAll("[^\\p{Punct}]", "").length() > 
            recognizedText.replaceAll("[^\\p{Punct}]", "").length()) {
            errors.add("标点符号识别不完整");
        }
        
        return errors;
    }

    /**
     * 生成改进建议
     */
    private List<String> generateImprovements(String originalText, String recognizedText) {
        List<String> improvements = new ArrayList<>();
        
        improvements.add("建议在安静环境中录音，减少背景噪音");
        improvements.add("说话时保持适当语速，避免过快或过慢");
        improvements.add("确保发音清晰，特别是易混淆的字词");
        improvements.add("录音设备距离嘴部20-30厘米为宜");
        
        // 根据识别结果质量给出具体建议
        double similarity = calculateSimilarity(originalText, recognizedText);
        if (similarity < 0.7) {
            improvements.add("识别准确率较低，建议检查音频质量");
            improvements.add("可尝试分段录音，提高识别精度");
        }
        
        return improvements;
    }

    /**
     * 评估置信度
     */
    private double assessConfidence(String originalText, String recognizedText) {
        double similarity = calculateSimilarity(originalText, recognizedText);
        
        // 基于相似度和其他因素计算置信度
        double confidence = similarity;
        
        // 长度差异影响置信度
        double lengthRatio = (double) Math.min(originalText.length(), recognizedText.length()) / 
                            Math.max(originalText.length(), recognizedText.length());
        confidence *= lengthRatio;
        
        return Math.round(confidence * 1000.0) / 1000.0;
    }
}
