package com.translation.encoding.service;

import com.translation.encoding.dto.EncodingRequest;
import com.translation.encoding.dto.EncodingResponse;
import com.translation.encoding.dto.QueryRequest;

import java.util.List;
import java.util.Map;

/**
 * 文本编码服务接口
 * 定义多语言文本编码和查询的核心功能
 */
public interface TextEncodingService {

    /**
     * 编码多语言文本
     * 将多种语言的文本打包成高效、紧凑的格式
     * 
     * @param request 编码请求
     * @return 编码响应
     */
    EncodingResponse encodeTexts(EncodingRequest request);

    /**
     * 查询文本
     * 通过语言->文本编号->文本来源快速查询文本内容
     * 
     * @param request 查询请求
     * @return 查询结果
     */
    Object queryText(QueryRequest request);

    /**
     * 批量查询文本
     * 
     * @param requests 批量查询请求
     * @return 批量查询结果
     */
    Object batchQueryTexts(List<QueryRequest> requests);

    /**
     * 获取编码信息
     * 
     * @param encodingId 编码ID
     * @return 编码信息
     */
    Object getEncodingInfo(String encodingId);

    /**
     * 解码文本包
     * 将编码后的文本包解码为原始格式
     * 
     * @param encodingId 编码ID
     * @return 解码结果
     */
    Object decodeTexts(String encodingId);

    /**
     * 删除编码数据
     * 
     * @param encodingId 编码ID
     */
    void deleteEncoding(String encodingId);

    /**
     * 获取编码统计信息
     * 
     * @return 统计信息
     */
    Map<String, Object> getEncodingStatistics();

    /**
     * 分析压缩率
     * 
     * @param encodingId 编码ID
     * @return 压缩分析结果
     */
    Object analyzeCompression(String encodingId);

    /**
     * 优化编码
     * 对现有编码进行优化，提高压缩率和查询效率
     * 
     * @param encodingId 编码ID
     * @return 优化结果
     */
    Object optimizeEncoding(String encodingId);
}
