package com.rollcall.server

import io.ktor.http.ContentType

object BuiltInServerData {
    private const val AI_CONFIG = """
【AI_API_URL】https://n.39.la/v1/chat/completions【AI_API_URL】
【AI_API_KEY】sk-671f8134a47f4a131f5736bcafbf0ce09f21a3cef324c4a6f7f51af9df9ea298【AI_API_KEY】
【AI_MODEL】gpt-5.4【AI_MODEL】
【AI_MODEL_SUPPORTS_IMAGE】true【AI_MODEL_SUPPORTS_IMAGE】
【AI_TEMPERATURE】0.1【AI_TEMPERATURE】
【AI_PROMPT】你是一个专业的英语词汇分析助手。你接收到的内容可能是：1. 图片截图中的英文内容；2. OCR提取出的英文文本。任务要求：1. 只分析适合英语学习的通用英文正文内容。2. 如果内容里英文单词少于5个、主要是系统界面、按钮、网址、专业术语堆砌、杂乱排版，返回“未识别到有效文段”。3. 只提取单个英文单词，不分析短语，不分析中文，不分析专有名词和过于基础的词。4. 找出大一学生可能不认识的词，包含：完全生词 new_word；熟词生义 familiar_new_meaning。5. 输出必须为 JSON 数组，不要输出任何解释、标题、Markdown、代码块。6. 每个对象字段格式如下：{"word":"单词","type":"词性","meaning":"中文释义","category":"new_word 或 familiar_new_meaning","example":"一个很短的英文例句，没有就留空字符串","root":"词根词缀或构词提示，没有就留空字符串"}。7. 按原文顺序输出，去重。8. 如果没有合适结果，直接返回：未识别到有效文段。【AI_PROMPT】
【OCR_AUTO_INTERVAL_SECONDS】300【OCR_AUTO_INTERVAL_SECONDS】
"""

    private const val TIME_API = "https://api.uuni.cn//api/time"

    private const val NAME_LIST_1 = """
[{"name": "马伟杰", "probability": 100}, {"name": "林可伦", "probability": 100}, {"name": "刘思洋", "probability": 100}, {"name": "梅俊春", "probability": 100}, {"name": "张筱雅", "probability": 100}, {"name": "豆思枨", "probability": 100}, {"name": "张星", "probability": 100}, {"name": "杨锐", "probability": 100}, {"name": "彭一朝", "probability": 100}, {"name": "陈金泉", "probability": 100}, {"name": "莫子涵", "probability": 100}, {"name": "刘俣昊", "probability": 100}, {"name": "岳爽", "probability": 100}, {"name": "代承旭", "probability": 100}, {"name": "陈欣钰", "probability": 50}, {"name": "颜艾", "probability": 100}, {"name": "吴道源", "probability": 100}, {"name": "曾祥凤", "probability": 50}, {"name": "蔡鸿林", "probability": 0}, {"name": "吴青莲", "probability": 100}, {"name": "王芯颖", "probability": 50}, {"name": "王显福", "probability": 50}, {"name": "邓程", "probability": 100}, {"name": "周心怡", "probability": 100}, {"name": "王皓", "probability": 100}, {"name": "童锦睿", "probability": 100}, {"name": "刘炬彤", "probability": 100}, {"name": "王浩", "probability": 50}, {"name": "许佳禄", "probability": 50}, {"name": "胡祐铷", "probability": 100}, {"name": "张梦婕", "probability": 100}, {"name": "周微漪", "probability": 40}, {"name": "王宇欣", "probability": 40}, {"name": "方雯", "probability": 100}, {"name": "向阳", "probability": 100}, {"name": "胡宇熙", "probability": 1}, {"name": "段然", "probability": 0}, {"name": "陈彦霖", "probability": 0}, {"name": "张凌峰", "probability": 80}, {"name": "任艨镘", "probability": 0}, {"name": "文浥杉", "probability": 0}, {"name": "徐睿廷", "probability": 0}, {"name": "唐浩轩", "probability": 0}, {"name": "李希智", "probability": 0}]
"""

    private const val NAME_LIST_3 = """
[{"name":"曾鈴曦","probability":0},{"name":"陈智烔","probability":100},{"name":"耿艺嘉","probability":100},{"name":"何袁齐","probability":0},{"name":"何梓豪","probability":100},{"name":"黄宏奥","probability":100},{"name":"黄泽","probability":100},{"name":"胡峻熙","probability":100},{"name":"胡馨月","probability":100},{"name":"林宇鹏","probability":100},{"name":"刘发铭","probability":100},{"name":"刘佳朗","probability":0},{"name":"刘锶琪","probability":100},{"name":"刘宇豪","probability":0},{"name":"刘子豪","probability":100},{"name":"黎艾霏","probability":0},{"name":"李光凡","probability":100},{"name":"李俊轩","probability":0},{"name":"李宛颖","probability":0},{"name":"李宇浩","probability":100},{"name":"卢紫嫣","probability":0},{"name":"马文婷","probability":100},{"name":"彭政","probability":100},{"name":"尚倍宇","probability":100},{"name":"谭淇文","probability":0},{"name":"谭绍湫","probability":100},{"name":"谭昭靖","probability":100},{"name":"万恒瑞","probability":100},{"name":"肖斯译","probability":100},{"name":"谢金谷","probability":100},{"name":"徐朝君","probability":100},{"name":"徐倩梦","probability":100},{"name":"余美洁","probability":100},{"name":"余名坤","probability":100},{"name":"张佳麒","probability":100},{"name":"张筱涵","probability":100},{"name":"张译文","probability":100},{"name":"周嘉怡","probability":0},{"name":"杨力策","probability":100}]
"""

    private const val NAME_LIST_4 = """
[{"name":"程源","probability":100},{"name":"冯文静","probability":100},{"name":"符俊","probability":100},{"name":"高健轩","probability":100},{"name":"胡星","probability":100},{"name":"焦晨雨","probability":100},{"name":"刘湘杭","probability":100},{"name":"刘子宁","probability":100},{"name":"李佳丽","probability":100},{"name":"罗惠匀","probability":100},{"name":"马顺顺","probability":100},{"name":"欧思彤","probability":100},{"name":"彭怀河","probability":100},{"name":"彭康恩","probability":100},{"name":"任治昊","probability":100},{"name":"盛弘毅","probability":100},{"name":"宋想想","probability":100},{"name":"宋妍竺","probability":100},{"name":"唐金童鞋","probability":100},{"name":"唐雅珊","probability":100},{"name":"谭博文","probability":100},{"name":"谭皓尹","probability":100},{"name":"王荣宽","probability":100},{"name":"王志毅","probability":100},{"name":"徐锦程","probability":100},{"name":"杨皓然","probability":100},{"name":"游思凯","probability":100},{"name":"袁佳玺","probability":100},{"name":"余泉漫","probability":100},{"name":"张瑞涵","probability":100},{"name":"赵洪宇","probability":100},{"name":"周学鹏","probability":100},{"name":"吴昊","probability":0},{"name":"龙艺心","probability":0},{"name":"陆昭仪","probability":0},{"name":"胡珊珊","probability":0}]
"""

    private const val SUBJECT_LIST_1 = """
{
  "星期一": {
    "schedule": [
      { "period": 0, "subject": "自习", "start_time": "7:25", "end_time": "7:55", "dismissal_time": "8:00" },
      { "period": 1, "subject": "数学", "start_time": "8:00", "end_time": "8:40", "dismissal_time": "8:50" },
      { "period": 2, "subject": "数学", "start_time": "8:50", "end_time": "9:30", "dismissal_time": "9:55" },
      { "period": 3, "subject": "物理", "start_time": "9:55", "end_time": "10:35", "dismissal_time": "10:40" },
      { "period": 4, "subject": "英语", "start_time": "10:40", "end_time": "11:20", "dismissal_time": "11:30" },
      { "period": 5, "subject": "化/政", "start_time": "11:30", "end_time": "12:10", "dismissal_time": "12:50" },
      { "period": 6, "subject": "地理", "start_time": "14:30", "end_time": "15:10", "dismissal_time": "15:20" },
      { "period": 7, "subject": "语文", "start_time": "15:20", "end_time": "16:00", "dismissal_time": "16:10" },
      { "period": 8, "subject": "班会", "start_time": "16:10", "end_time": "16:50", "dismissal_time": "17:00" },
      { "period": 9, "subject": "自习", "start_time": "17:00", "end_time": "17:40", "dismissal_time": "18:10" },
      { "period": 10, "subject": "语文", "start_time": "18:30", "end_time": "19:30", "dismissal_time": "19:40" },
      { "period": 11, "subject": "自习", "start_time": "19:50", "end_time": "20:30", "dismissal_time": "20:40" }
    ]
  },
  "星期二": {
    "schedule": [
      { "period": 0, "subject": "自习", "start_time": "7:25", "end_time": "7:55", "dismissal_time": "8:00" },
      { "period": 1, "subject": "化/政", "start_time": "8:00", "end_time": "8:40", "dismissal_time": "8:50" },
      { "period": 2, "subject": "语文", "start_time": "8:50", "end_time": "9:30", "dismissal_time": "9:55" },
      { "period": 3, "subject": "地理", "start_time": "9:55", "end_time": "10:35", "dismissal_time": "10:40" },
      { "period": 4, "subject": "物理", "start_time": "10:40", "end_time": "11:20", "dismissal_time": "11:30" },
      { "period": 5, "subject": "体育", "start_time": "11:30", "end_time": "12:10", "dismissal_time": "12:50" },
      { "period": 6, "subject": "英语", "start_time": "14:30", "end_time": "15:10", "dismissal_time": "15:20" },
      { "period": 7, "subject": "英语", "start_time": "15:20", "end_time": "16:00", "dismissal_time": "16:10" },
      { "period": 8, "subject": "数学", "start_time": "16:10", "end_time": "16:50", "dismissal_time": "17:00" },
      { "period": 9, "subject": "自习", "start_time": "17:00", "end_time": "17:40", "dismissal_time": "18:10" },
      { "period": 10, "subject": "英语", "start_time": "18:30", "end_time": "19:30", "dismissal_time": "19:40" },
      { "period": 11, "subject": "自习", "start_time": "19:50", "end_time": "20:30", "dismissal_time": "20:40" }
    ]
  },
  "星期三": {
    "schedule": [
      { "period": 0, "subject": "自习", "start_time": "7:25", "end_time": "7:55", "dismissal_time": "8:00" },
      { "period": 1, "subject": "语文", "start_time": "8:00", "end_time": "8:40", "dismissal_time": "8:50" },
      { "period": 2, "subject": "语文", "start_time": "8:50", "end_time": "9:30", "dismissal_time": "9:55" },
      { "period": 3, "subject": "数学", "start_time": "9:55", "end_time": "10:35", "dismissal_time": "10:40" },
      { "period": 4, "subject": "英语", "start_time": "10:40", "end_time": "11:20", "dismissal_time": "11:30" },
      { "period": 5, "subject": "英语", "start_time": "11:30", "end_time": "12:10", "dismissal_time": "12:50" },
      { "period": 6, "subject": "物理", "start_time": "14:30", "end_time": "15:10", "dismissal_time": "15:20" },
      { "period": 7, "subject": "化/政", "start_time": "15:20", "end_time": "16:00", "dismissal_time": "16:10" },
      { "period": 8, "subject": "信息", "start_time": "16:10", "end_time": "16:50", "dismissal_time": "17:00" },
      { "period": 9, "subject": "自习", "start_time": "17:00", "end_time": "17:40", "dismissal_time": "18:10" },
      { "period": 10, "subject": "物理", "start_time": "18:30", "end_time": "19:30", "dismissal_time": "19:40" },
      { "period": 11, "subject": "自习", "start_time": "19:50", "end_time": "20:30", "dismissal_time": "20:40" }
    ]
  },
  "星期四": {
    "schedule": [
      { "period": 0, "subject": "自习", "start_time": "7:25", "end_time": "7:55", "dismissal_time": "8:00" },
      { "period": 1, "subject": "数学", "start_time": "8:00", "end_time": "8:40", "dismissal_time": "8:50" },
      { "period": 2, "subject": "数学", "start_time": "8:50", "end_time": "9:30", "dismissal_time": "9:55" },
      { "period": 3, "subject": "英语", "start_time": "9:55", "end_time": "10:35", "dismissal_time": "10:40" },
      { "period": 4, "subject": "物理", "start_time": "10:40", "end_time": "11:20", "dismissal_time": "11:30" },
      { "period": 5, "subject": "语文", "start_time": "11:30", "end_time": "12:10", "dismissal_time": "12:50" },
      { "period": 6, "subject": "政治", "start_time": "14:30", "end_time": "15:10", "dismissal_time": "15:20" },
      { "period": 7, "subject": "地理", "start_time": "15:20", "end_time": "16:00", "dismissal_time": "16:10" },
      { "period": 8, "subject": "化/政", "start_time": "16:10", "end_time": "16:50", "dismissal_time": "17:00" },
      { "period": 9, "subject": "自习", "start_time": "17:00", "end_time": "17:40", "dismissal_time": "18:10" },
      { "period": 10, "subject": "化/地", "start_time": "18:30", "end_time": "19:30", "dismissal_time": "19:40" },
      { "period": 11, "subject": "自习", "start_time": "19:50", "end_time": "20:30", "dismissal_time": "20:40" }
    ]
  },
  "星期五": {
    "schedule": [
      { "period": 0, "subject": "自习", "start_time": "7:25", "end_time": "7:55", "dismissal_time": "8:00" },
      { "period": 1, "subject": "语文", "start_time": "8:00", "end_time": "8:40", "dismissal_time": "8:50" },
      { "period": 2, "subject": "语文", "start_time": "8:50", "end_time": "9:30", "dismissal_time": "9:55" },
      { "period": 3, "subject": "物理", "start_time": "9:55", "end_time": "10:35", "dismissal_time": "10:40" },
      { "period": 4, "subject": "物理", "start_time": "10:40", "end_time": "11:20", "dismissal_time": "11:30" },
      { "period": 5, "subject": "地理", "start_time": "11:30", "end_time": "12:10", "dismissal_time": "12:50" },
      { "period": 6, "subject": "体育", "start_time": "14:30", "end_time": "15:10", "dismissal_time": "15:20" },
      { "period": 7, "subject": "数学", "start_time": "15:20", "end_time": "16:00", "dismissal_time": "16:10" },
      { "period": 8, "subject": "英语", "start_time": "16:10", "end_time": "16:50", "dismissal_time": "17:00" },
      { "period": 9, "subject": "自习", "start_time": "17:00", "end_time": "17:40", "dismissal_time": "18:10" },
      { "period": 10, "subject": "数学", "start_time": "18:30", "end_time": "19:30", "dismissal_time": "19:40" },
      { "period": 11, "subject": "自习", "start_time": "19:50", "end_time": "20:30", "dismissal_time": "20:40" }
    ]
  }
}
"""

    private const val SUBJECT_LIST_3 = """
{
  "星期一": {
    "schedule": [
      { "period": 0, "subject": "自习", "start_time": "7:25", "end_time": "7:55", "dismissal_time": "8:00" },
      { "period": 1, "subject": "语文", "start_time": "8:00", "end_time": "8:40", "dismissal_time": "8:50" },
      { "period": 2, "subject": "英语", "start_time": "8:50", "end_time": "9:30", "dismissal_time": "9:55" },
      { "period": 3, "subject": "英语", "start_time": "9:55", "end_time": "10:35", "dismissal_time": "10:40" },
      { "period": 4, "subject": "数学", "start_time": "10:40", "end_time": "11:20", "dismissal_time": "11:30" },
      { "period": 5, "subject": "数学", "start_time": "11:30", "end_time": "12:10", "dismissal_time": "12:50" },
      { "period": 6, "subject": "物理", "start_time": "14:30", "end_time": "15:10", "dismissal_time": "15:20" },
      { "period": 7, "subject": "生物", "start_time": "15:20", "end_time": "16:00", "dismissal_time": "16:10" },
      { "period": 8, "subject": "班会", "start_time": "16:10", "end_time": "16:50", "dismissal_time": "17:00" },
      { "period": 9, "subject": "自习", "start_time": "17:00", "end_time": "17:40", "dismissal_time": "18:10" },
      { "period": 10, "subject": "英语", "start_time": "18:30", "end_time": "19:30", "dismissal_time": "19:40" },
      { "period": 11, "subject": "自习", "start_time": "19:50", "end_time": "20:30", "dismissal_time": "20:40" }
    ]
  },
  "星期二": {
    "schedule": [
      { "period": 0, "subject": "自习", "start_time": "7:25", "end_time": "7:55", "dismissal_time": "8:00" },
      { "period": 1, "subject": "数学", "start_time": "8:00", "end_time": "8:40", "dismissal_time": "8:50" },
      { "period": 2, "subject": "数学", "start_time": "8:50", "end_time": "9:30", "dismissal_time": "9:55" },
      { "period": 3, "subject": "物理", "start_time": "9:55", "end_time": "10:35", "dismissal_time": "10:40" },
      { "period": 4, "subject": "语文", "start_time": "10:40", "end_time": "11:20", "dismissal_time": "11:30" },
      { "period": 5, "subject": "生物", "start_time": "11:30", "end_time": "12:10", "dismissal_time": "12:50" },
      { "period": 6, "subject": "体育", "start_time": "14:30", "end_time": "15:10", "dismissal_time": "15:20" },
      { "period": 7, "subject": "化学", "start_time": "15:20", "end_time": "16:00", "dismissal_time": "16:10" },
      { "period": 8, "subject": "英语", "start_time": "16:10", "end_time": "16:50", "dismissal_time": "17:00" },
      { "period": 9, "subject": "自习", "start_time": "17:00", "end_time": "17:40", "dismissal_time": "18:10" },
      { "period": 10, "subject": "语文", "start_time": "18:30", "end_time": "19:30", "dismissal_time": "19:40" },
      { "period": 11, "subject": "自习", "start_time": "19:50", "end_time": "20:30", "dismissal_time": "20:40" }
    ]
  },
  "星期三": {
    "schedule": [
      { "period": 0, "subject": "自习", "start_time": "7:25", "end_time": "7:55", "dismissal_time": "8:00" },
      { "period": 1, "subject": "英语", "start_time": "8:00", "end_time": "8:40", "dismissal_time": "8:50" },
      { "period": 2, "subject": "语文", "start_time": "8:50", "end_time": "9:30", "dismissal_time": "9:55" },
      { "period": 3, "subject": "数学", "start_time": "9:55", "end_time": "10:35", "dismissal_time": "10:40" },
      { "period": 4, "subject": "生物", "start_time": "10:40", "end_time": "11:20", "dismissal_time": "11:30" },
      { "period": 5, "subject": "体育", "start_time": "11:30", "end_time": "12:10", "dismissal_time": "12:50" },
      { "period": 6, "subject": "政治", "start_time": "14:30", "end_time": "15:10", "dismissal_time": "15:20" },
      { "period": 7, "subject": "物理", "start_time": "15:20", "end_time": "16:00", "dismissal_time": "16:10" },
      { "period": 8, "subject": "化学", "start_time": "16:10", "end_time": "16:50", "dismissal_time": "17:00" },
      { "period": 9, "subject": "自习", "start_time": "17:00", "end_time": "17:40", "dismissal_time": "18:10" },
      { "period": 10, "subject": "数学", "start_time": "18:30", "end_time": "19:30", "dismissal_time": "19:40" },
      { "period": 11, "subject": "自习", "start_time": "19:50", "end_time": "20:30", "dismissal_time": "20:40" }
    ]
  },
  "星期四": {
    "schedule": [
      { "period": 0, "subject": "自习", "start_time": "7:25", "end_time": "7:55", "dismissal_time": "8:00" },
      { "period": 1, "subject": "语文", "start_time": "8:00", "end_time": "8:40", "dismissal_time": "8:50" },
      { "period": 2, "subject": "语文", "start_time": "8:50", "end_time": "9:30", "dismissal_time": "9:55" },
      { "period": 3, "subject": "数学", "start_time": "9:55", "end_time": "10:35", "dismissal_time": "10:40" },
      { "period": 4, "subject": "英语", "start_time": "10:40", "end_time": "11:20", "dismissal_time": "11:30" },
      { "period": 5, "subject": "英语", "start_time": "11:30", "end_time": "12:10", "dismissal_time": "12:50" },
      { "period": 6, "subject": "化学", "start_time": "14:30", "end_time": "15:10", "dismissal_time": "15:20" },
      { "period": 7, "subject": "物理", "start_time": "15:20", "end_time": "16:00", "dismissal_time": "16:10" },
      { "period": 8, "subject": "物理", "start_time": "16:10", "end_time": "16:50", "dismissal_time": "17:00" },
      { "period": 9, "subject": "自习", "start_time": "17:00", "end_time": "17:40", "dismissal_time": "18:10" },
      { "period": 10, "subject": "生/化", "start_time": "18:30", "end_time": "19:30", "dismissal_time": "19:40" },
      { "period": 11, "subject": "自习", "start_time": "19:50", "end_time": "20:30", "dismissal_time": "20:40" }
    ]
  },
  "星期五": {
    "schedule": [
      { "period": 0, "subject": "自习", "start_time": "7:25", "end_time": "7:55", "dismissal_time": "8:00" },
      { "period": 1, "subject": "语文", "start_time": "8:00", "end_time": "8:40", "dismissal_time": "8:50" },
      { "period": 2, "subject": "语文", "start_time": "8:50", "end_time": "9:30", "dismissal_time": "9:55" },
      { "period": 3, "subject": "信息", "start_time": "9:55", "end_time": "10:35", "dismissal_time": "10:40" },
      { "period": 4, "subject": "化学", "start_time": "10:40", "end_time": "11:20", "dismissal_time": "11:30" },
      { "period": 5, "subject": "物理", "start_time": "11:30", "end_time": "12:10", "dismissal_time": "12:50" },
      { "period": 6, "subject": "英语", "start_time": "14:00", "end_time": "14:40", "dismissal_time": "14:50" },
      { "period": 7, "subject": "数学", "start_time": "14:50", "end_time": "15:30", "dismissal_time": "15:40" },
      { "period": 8, "subject": "生物", "start_time": "15:40", "end_time": "16:20", "dismissal_time": "16:30" },
      { "period": 9, "subject": "选修", "start_time": "16:30", "end_time": "17:40", "dismissal_time": "18:10" },
      { "period": 10, "subject": "物理", "start_time": "18:30", "end_time": "19:30", "dismissal_time": "19:40" },
      { "period": 11, "subject": "自习", "start_time": "19:40", "end_time": "20:30", "dismissal_time": "20:40" }
    ]
  }
}
"""

    private const val SUBJECT_LIST_4 = """
{
  "星期一": {
    "schedule": [
      { "period": 0, "subject": "自习", "start_time": "7:25", "end_time": "7:55", "dismissal_time": "8:00" },
      { "period": 1, "subject": "语文", "start_time": "8:00", "end_time": "8:40", "dismissal_time": "8:50" },
      { "period": 2, "subject": "语文", "start_time": "8:50", "end_time": "9:30", "dismissal_time": "9:55" },
      { "period": 3, "subject": "数学", "start_time": "9:55", "end_time": "10:35", "dismissal_time": "10:40" },
      { "period": 4, "subject": "数学", "start_time": "10:40", "end_time": "11:20", "dismissal_time": "11:30" },
      { "period": 5, "subject": "生/政", "start_time": "11:30", "end_time": "12:10", "dismissal_time": "12:50" },
      { "period": 6, "subject": "英语", "start_time": "14:30", "end_time": "15:10", "dismissal_time": "15:20" },
      { "period": 7, "subject": "物理", "start_time": "15:20", "end_time": "16:00", "dismissal_time": "16:10" },
      { "period": 8, "subject": "班会", "start_time": "16:10", "end_time": "16:50", "dismissal_time": "17:00" },
      { "period": 9, "subject": "自习", "start_time": "17:00", "end_time": "17:40", "dismissal_time": "18:10" },
      { "period": 10, "subject": "自习", "start_time": "18:30", "end_time": "19:30", "dismissal_time": "19:40" },
      { "period": 11, "subject": "自习", "start_time": "19:50", "end_time": "20:30", "dismissal_time": "20:40" }
    ]
  },
  "星期二": {
    "schedule": [
      { "period": 0, "subject": "自习", "start_time": "7:25", "end_time": "7:55", "dismissal_time": "8:00" },
      { "period": 1, "subject": "生/政", "start_time": "8:00", "end_time": "8:40", "dismissal_time": "8:50" },
      { "period": 2, "subject": "物理", "start_time": "8:50", "end_time": "9:30", "dismissal_time": "9:55" },
      { "period": 3, "subject": "化学", "start_time": "9:55", "end_time": "10:35", "dismissal_time": "10:40" },
      { "period": 4, "subject": "英语", "start_time": "10:40", "end_time": "11:20", "dismissal_time": "11:30" },
      { "period": 5, "subject": "英语", "start_time": "11:30", "end_time": "12:10", "dismissal_time": "12:50" },
      { "period": 6, "subject": "体育", "start_time": "14:30", "end_time": "15:10", "dismissal_time": "15:20" },
      { "period": 7, "subject": "数学", "start_time": "15:20", "end_time": "16:00", "dismissal_time": "16:10" },
      { "period": 8, "subject": "语文", "start_time": "16:10", "end_time": "16:50", "dismissal_time": "17:00" },
      { "period": 9, "subject": "自习", "start_time": "17:00", "end_time": "17:40", "dismissal_time": "18:10" },
      { "period": 10, "subject": "自习", "start_time": "18:30", "end_time": "19:30", "dismissal_time": "19:40" },
      { "period": 11, "subject": "自习", "start_time": "19:50", "end_time": "20:30", "dismissal_time": "20:40" }
    ]
  },
  "星期三": {
    "schedule": [
      { "period": 0, "subject": "自习", "start_time": "7:25", "end_time": "7:55", "dismissal_time": "8:00" },
      { "period": 1, "subject": "英语", "start_time": "8:00", "end_time": "8:40", "dismissal_time": "8:50" },
      { "period": 2, "subject": "语文", "start_time": "8:50", "end_time": "9:30", "dismissal_time": "9:55" },
      { "period": 3, "subject": "语文", "start_time": "9:55", "end_time": "10:35", "dismissal_time": "10:40" },
      { "period": 4, "subject": "数学", "start_time": "10:40", "end_time": "11:20", "dismissal_time": "11:30" },
      { "period": 5, "subject": "体育", "start_time": "11:30", "end_time": "12:10", "dismissal_time": "12:50" },
      { "period": 6, "subject": "物理", "start_time": "14:30", "end_time": "15:10", "dismissal_time": "15:20" },
      { "period": 7, "subject": "生/政", "start_time": "15:20", "end_time": "16:00", "dismissal_time": "16:10" },
      { "period": 8, "subject": "化学", "start_time": "16:10", "end_time": "16:50", "dismissal_time": "17:00" },
      { "period": 9, "subject": "自习", "start_time": "17:00", "end_time": "17:40", "dismissal_time": "18:10" },
      { "period": 10, "subject": "自习", "start_time": "18:30", "end_time": "19:30", "dismissal_time": "19:40" },
      { "period": 11, "subject": "自习", "start_time": "19:50", "end_time": "20:30", "dismissal_time": "20:40" }
    ]
  },
  "星期四": {
    "schedule": [
      { "period": 0, "subject": "自习", "start_time": "7:25", "end_time": "7:55", "dismissal_time": "8:00" },
      { "period": 1, "subject": "英语", "start_time": "8:00", "end_time": "8:40", "dismissal_time": "8:50" },
      { "period": 2, "subject": "英语", "start_time": "8:50", "end_time": "9:30", "dismissal_time": "9:55" },
      { "period": 3, "subject": "数学", "start_time": "9:55", "end_time": "10:35", "dismissal_time": "10:40" },
      { "period": 4, "subject": "数学", "start_time": "10:40", "end_time": "11:20", "dismissal_time": "11:30" },
      { "period": 5, "subject": "语文", "start_time": "11:30", "end_time": "12:10", "dismissal_time": "12:50" },
      { "period": 6, "subject": "物理", "start_time": "14:30", "end_time": "15:10", "dismissal_time": "15:20" },
      { "period": 7, "subject": "化学", "start_time": "15:20", "end_time": "16:00", "dismissal_time": "16:10" },
      { "period": 8, "subject": "生/政", "start_time": "16:10", "end_time": "16:50", "dismissal_time": "17:00" },
      { "period": 9, "subject": "自习", "start_time": "17:00", "end_time": "17:40", "dismissal_time": "18:10" },
      { "period": 10, "subject": "自习", "start_time": "18:30", "end_time": "19:30", "dismissal_time": "19:40" },
      { "period": 11, "subject": "自习", "start_time": "19:50", "end_time": "20:30", "dismissal_time": "20:40" }
    ]
  },
  "星期五": {
    "schedule": [
      { "period": 0, "subject": "自习", "start_time": "7:25", "end_time": "7:55", "dismissal_time": "8:00" },
      { "period": 1, "subject": "物理", "start_time": "8:00", "end_time": "8:40", "dismissal_time": "8:50" },
      { "period": 2, "subject": "物理", "start_time": "8:50", "end_time": "9:30", "dismissal_time": "9:55" },
      { "period": 3, "subject": "英语", "start_time": "9:55", "end_time": "10:35", "dismissal_time": "10:40" },
      { "period": 4, "subject": "信息", "start_time": "10:40", "end_time": "11:20", "dismissal_time": "11:30" },
      { "period": 5, "subject": "化学", "start_time": "11:30", "end_time": "12:10", "dismissal_time": "12:50" },
      { "period": 6, "subject": "政治", "start_time": "14:30", "end_time": "15:10", "dismissal_time": "15:20" },
      { "period": 7, "subject": "语文", "start_time": "15:20", "end_time": "16:00", "dismissal_time": "16:10" },
      { "period": 8, "subject": "数学", "start_time": "16:10", "end_time": "16:50", "dismissal_time": "17:00" },
      { "period": 9, "subject": "自习", "start_time": "17:00", "end_time": "17:40", "dismissal_time": "18:10" },
      { "period": 10, "subject": "自习", "start_time": "18:30", "end_time": "19:30", "dismissal_time": "19:40" },
      { "period": 11, "subject": "自习", "start_time": "19:50", "end_time": "20:30", "dismissal_time": "20:40" }
    ]
  }
}
"""

    val fileMap: Map<String, String> = linkedMapOf(
        "AiConfig.txt" to AI_CONFIG,
        "NameList1.txt" to NAME_LIST_1,
        "NameList3.txt" to NAME_LIST_3,
        "NameList4.txt" to NAME_LIST_4,
        "SubjectList1.txt" to SUBJECT_LIST_1,
        "SubjectList3.txt" to SUBJECT_LIST_3,
        "SubjectList4.txt" to SUBJECT_LIST_4,
        "TimeApi.txt" to TIME_API
    )

    val fileNamesJson: String = fileMap.keys.joinToString(
        prefix = "[",
        postfix = "]",
        separator = ","
    ) { "\"$it\"" }

    fun contentTypeFor(path: String): ContentType = when {
        path.endsWith(".txt", ignoreCase = true) -> ContentType.parse("text/plain; charset=utf-8")
        else -> ContentType.Application.OctetStream
    }
}
