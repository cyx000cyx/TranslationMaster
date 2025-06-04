package com.translation.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.translation.task.entity.TranslationTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 翻译任务Mapper接口
 */
@Mapper
public interface TranslationTaskMapper extends BaseMapper<TranslationTask> {
}