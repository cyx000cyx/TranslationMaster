package com.translation.encoding.controller;

import com.translation.common.dto.ApiResponse;
import com.translation.encoding.dto.EncodingRequest;
import com.translation.encoding.dto.EncodingResponse;
import com.translation.encoding.dto.QueryRequest;
import com.translation.encoding.service.TextEncodingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

/**
 * 编码控制器
 * 提供文本编码和查询的REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/encoding")
@RequiredArgsConstructor
@Validated
public class EncodingController {

    @Resource
    private TextEncodingService textEncodingService;

    /**
     * 编码多语言文本
     * 将多种语言的文本打包成紧凑格式
     */
    @PostMapping("/encode")
    public ApiResponse<EncodingResponse> encodeTexts(@Valid @RequestBody EncodingRequest request) {
        log.info("收到文本编码请求，任务ID: {}, 语言数: {}", 
                request.getTaskId(), request.getTexts().size());
        
        try {
            EncodingResponse response = textEncodingService.encodeTexts(request);
            log.info("文本编码完成，任务ID: {}, 编码ID: {}", 
                    request.getTaskId(), response.getEncodingId());
            
            return ApiResponse.success(response, "编码成功");
        } catch (Exception e) {
            log.error("文本编码失败", e);
            return ApiResponse.error("编码失败: " + e.getMessage());
        }
    }

    /**
     * 查询编码文本
     * 通过语言->文本编号->文本来源快速查询
     */
    @PostMapping("/query")
    public ApiResponse<Object> queryText(@Valid @RequestBody QueryRequest request) {
        log.info("收到文本查询请求: {}", request);
        
        try {
            Object result = textEncodingService.queryText(request);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("文本查询失败", e);
            return ApiResponse.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取编码信息
     */
    @GetMapping("/{encodingId}")
    public ApiResponse<Object> getEncodingInfo(@PathVariable String encodingId) {
        log.info("查询编码信息，编码ID: {}", encodingId);
        
        try {
            Object info = textEncodingService.getEncodingInfo(encodingId);
            return ApiResponse.success(info);
        } catch (Exception e) {
            log.error("查询编码信息失败，编码ID: {}", encodingId, e);
            return ApiResponse.error("查询编码信息失败: " + e.getMessage());
        }
    }

    /**
     * 解码文本包
     * 将编码后的文本包解码为原始文本
     */
    @PostMapping("/{encodingId}/decode")
    public ApiResponse<Object> decodeTexts(@PathVariable String encodingId) {
        log.info("解码文本包，编码ID: {}", encodingId);
        
        try {
            Object result = textEncodingService.decodeTexts(encodingId);
            return ApiResponse.success(result, "解码成功");
        } catch (Exception e) {
            log.error("解码文本包失败，编码ID: {}", encodingId, e);
            return ApiResponse.error("解码失败: " + e.getMessage());
        }
    }

    /**
     * 删除编码数据
     */
    @DeleteMapping("/{encodingId}")
    public ApiResponse<Void> deleteEncoding(@PathVariable String encodingId) {
        log.info("删除编码数据，编码ID: {}", encodingId);
        
        try {
            textEncodingService.deleteEncoding(encodingId);
            return ApiResponse.success(null, "删除成功");
        } catch (Exception e) {
            log.error("删除编码数据失败，编码ID: {}", encodingId, e);
            return ApiResponse.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取编码统计信息
     */
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getEncodingStatistics() {
        try {
            Map<String, Object> statistics = textEncodingService.getEncodingStatistics();
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            log.error("获取编码统计信息失败", e);
            return ApiResponse.error("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 批量查询文本
     */
    @PostMapping("/batch-query")
    public ApiResponse<Object> batchQueryTexts(@Valid @RequestBody java.util.List<QueryRequest> requests) {
        log.info("收到批量文本查询请求，数量: {}", requests.size());
        
        try {
            Object results = textEncodingService.batchQueryTexts(requests);
            return ApiResponse.success(results);
        } catch (Exception e) {
            log.error("批量文本查询失败", e);
            return ApiResponse.error("批量查询失败: " + e.getMessage());
        }
    }

    /**
     * 压缩率分析
     */
    @GetMapping("/{encodingId}/compression-analysis")
    public ApiResponse<Object> analyzeCompression(@PathVariable String encodingId) {
        log.info("分析压缩率，编码ID: {}", encodingId);
        
        try {
            Object analysis = textEncodingService.analyzeCompression(encodingId);
            return ApiResponse.success(analysis);
        } catch (Exception e) {
            log.error("压缩率分析失败，编码ID: {}", encodingId, e);
            return ApiResponse.error("压缩率分析失败: " + e.getMessage());
        }
    }

    /**
     * 优化编码
     * 对现有编码进行优化，提高压缩率
     */
    @PostMapping("/{encodingId}/optimize")
    public ApiResponse<Object> optimizeEncoding(@PathVariable String encodingId) {
        log.info("优化编码，编码ID: {}", encodingId);
        
        try {
            Object result = textEncodingService.optimizeEncoding(encodingId);
            return ApiResponse.success(result, "编码优化完成");
        } catch (Exception e) {
            log.error("编码优化失败，编码ID: {}", encodingId, e);
            return ApiResponse.error("编码优化失败: " + e.getMessage());
        }
    }
}
