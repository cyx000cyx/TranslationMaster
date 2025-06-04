#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Whisper音频处理脚本
用于调用本地Whisper模型进行语音识别
"""

import argparse
import json
import logging
import os
import sys
import time
import traceback
from pathlib import Path

try:
    import whisper
    import torch
    import numpy as np
except ImportError as e:
    print(f"导入模块失败: {e}")
    print("请安装所需依赖: pip install openai-whisper torch numpy")
    sys.exit(1)

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class WhisperProcessor:
    """Whisper语音识别处理器"""
    
    def __init__(self, model_name="base"):
        """
        初始化Whisper处理器
        
        Args:
            model_name: 模型名称 (tiny, base, small, medium, large)
        """
        self.model_name = model_name
        self.model = None
        self.supported_languages = {
            'zh': 'chinese',
            'en': 'english', 
            'ja': 'japanese'
        }
    
    def load_model(self):
        """加载Whisper模型"""
        try:
            logger.info(f"正在加载Whisper模型: {self.model_name}")
            start_time = time.time()
            
            # 检查CUDA可用性
            device = "cuda" if torch.cuda.is_available() else "cpu"
            logger.info(f"使用设备: {device}")
            
            # 加载模型
            self.model = whisper.load_model(self.model_name, device=device)
            
            load_time = time.time() - start_time
            logger.info(f"模型加载完成，耗时: {load_time:.2f}秒")
            
        except Exception as e:
            logger.error(f"加载模型失败: {e}")
            raise
    
    def process_audio(self, audio_file, language=None, task_id=None, **kwargs):
        """
        处理音频文件进行语音识别
        
        Args:
            audio_file: 音频文件路径
            language: 指定语言代码
            task_id: 任务ID
            **kwargs: 其他参数
            
        Returns:
            dict: 识别结果
        """
        try:
            logger.info(f"开始处理音频文件: {audio_file}")
            start_time = time.time()
            
            # 检查文件是否存在
            if not os.path.exists(audio_file):
                raise FileNotFoundError(f"音频文件不存在: {audio_file}")
            
            # 确保模型已加载
            if self.model is None:
                self.load_model()
            
            # 设置识别参数
            transcribe_options = {
                'task': 'transcribe',  # 默认转录任务
                'verbose': False
            }
            
            # 设置语言
            if language and language in self.supported_languages:
                transcribe_options['language'] = language
                logger.info(f"指定识别语言: {language}")
            else:
                logger.info("启用自动语言检测")
            
            # 执行语音识别
            logger.info("开始执行语音识别...")
            result = self.model.transcribe(audio_file, **transcribe_options)
            
            processing_time = int((time.time() - start_time) * 1000)
            
            # 构建响应结果
            response = {
                'success': True,
                'task_id': task_id,
                'text': result.get('text', '').strip(),
                'language': result.get('language', language),
                'duration': self._get_audio_duration(audio_file),
                'processing_time': processing_time,
                'confidence': self._calculate_avg_confidence(result),
                'segments': self._process_segments(result.get('segments', [])),
                'model_info': {
                    'model_name': self.model_name,
                    'device': str(self.model.device) if self.model else 'unknown'
                }
            }
            
            logger.info(f"语音识别完成，识别文本长度: {len(response['text'])}")
            return response
            
        except Exception as e:
            logger.error(f"处理音频失败: {e}")
            logger.error(traceback.format_exc())
            
            return {
                'success': False,
                'task_id': task_id,
                'error': str(e),
                'error_type': type(e).__name__,
                'processing_time': int((time.time() - start_time) * 1000) if 'start_time' in locals() else 0
            }
    
    def _get_audio_duration(self, audio_file):
        """获取音频时长"""
        try:
            import librosa
            duration = librosa.get_duration(filename=audio_file)
            return round(duration, 2)
        except ImportError:
            logger.warning("librosa未安装，无法获取音频时长")
            return 0.0
        except Exception as e:
            logger.warning(f"获取音频时长失败: {e}")
            return 0.0
    
    def _calculate_avg_confidence(self, result):
        """计算平均置信度"""
        try:
            segments = result.get('segments', [])
            if not segments:
                return 0.0
            
            # Whisper的segments中可能包含no_speech_prob等信息
            # 这里简单计算一个基于分段数量的置信度估计
            total_confidence = 0.0
            count = 0
            
            for segment in segments:
                # 使用(1 - no_speech_prob)作为置信度的一个估计
                no_speech_prob = segment.get('no_speech_prob', 0.5)
                confidence = 1.0 - no_speech_prob
                total_confidence += confidence
                count += 1
            
            return round(total_confidence / count if count > 0 else 0.0, 3)
            
        except Exception as e:
            logger.warning(f"计算置信度失败: {e}")
            return 0.0
    
    def _process_segments(self, segments):
        """处理分段信息"""
        processed_segments = []
        
        try:
            for segment in segments:
                processed_segment = {
                    'id': segment.get('id'),
                    'text': segment.get('text', '').strip(),
                    'start': round(segment.get('start', 0.0), 2),
                    'end': round(segment.get('end', 0.0), 2),
                    'confidence': round(1.0 - segment.get('no_speech_prob', 0.5), 3)
                }
                processed_segments.append(processed_segment)
                
        except Exception as e:
            logger.warning(f"处理分段信息失败: {e}")
        
        return processed_segments

def main():
    """主函数"""
    parser = argparse.ArgumentParser(description='Whisper语音识别处理器')
    parser.add_argument('--audio_file', required=True, help='音频文件路径')
    parser.add_argument('--model', default='base', help='Whisper模型名称')
    parser.add_argument('--language', help='指定语言代码')
    parser.add_argument('--task_id', help='任务ID')
    parser.add_argument('--output_format', default='json', help='输出格式')
    
    args = parser.parse_args()
    
    try:
        # 创建处理器
        processor = WhisperProcessor(model_name=args.model)
        
        # 处理音频
        result = processor.process_audio(
            audio_file=args.audio_file,
            language=args.language,
            task_id=args.task_id
        )
        
        # 输出结果
        if args.output_format == 'json':
            print(json.dumps(result, ensure_ascii=False, indent=2))
        else:
            if result.get('success'):
                print(result.get('text', ''))
            else:
                print(f"错误: {result.get('error', '未知错误')}")
                sys.exit(1)
        
    except KeyboardInterrupt:
        logger.info("用户中断处理")
        sys.exit(1)
    except Exception as e:
        logger.error(f"程序执行失败: {e}")
        logger.error(traceback.format_exc())
        
        error_result = {
            'success': False,
            'task_id': args.task_id,
            'error': str(e),
            'error_type': type(e).__name__
        }
        
        if args.output_format == 'json':
            print(json.dumps(error_result, ensure_ascii=False, indent=2))
        else:
            print(f"错误: {e}")
        
        sys.exit(1)

if __name__ == '__main__':
    main()
