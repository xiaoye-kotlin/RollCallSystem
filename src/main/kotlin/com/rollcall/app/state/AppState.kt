package com.rollcall.app.state

import com.rollcall.app.data.model.DailySchedule
import com.rollcall.app.data.model.Student
import com.rollcall.app.data.model.parseStudentJson
import com.rollcall.app.data.model.parseSubjectJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/**
 * 应用全局状态管理
 * 使用StateFlow管理所有可观察的状态，实现响应式UI更新
 * 采用单例模式，确保全局状态唯一
 */
object AppState {
    enum class LearningTriggerMode {
        MANUAL,
        AUTO
    }

    // ==================== 应用常量 ====================
    /** 应用版本号 */
    const val VERSION = 17
    /** 班级编号 */
    const val CLASS = 3

    // ==================== 颜色配置 ====================
    /** 主界面强调色 */
    var accentColorMain = 0xFFFF5733.toInt()
    /** 悬浮窗强调色 */
    var accentColorFloating = 0xFFFF5733.toInt()
    /** 点名界面强调色 */
    var accentColorNamed = 0xFFFF5733.toInt()
    /** 是否已随机选择颜色 */
    var isRandomColor = false

    // ==================== 网络配置 ====================
    /** 全局API域名 */
    var url = ""
    /** 文件下载域名 */
    var downloadUrl = ""
    /** 时间API地址 */
    var timeApi = ""
    /** 倒数日名称 */
    var countdownName = ""
    /** 倒数日时间 */
    var countdownTime = ""

    // ==================== 学生数据 ====================
    /** 学生列表 */
    private var studentList: List<Student> = emptyList()

    /** 从JSON更新学生列表 */
    fun updateStudentListFromJson(json: String) {
        studentList = parseStudentJson(json)
    }

    /** 课程表数据 */
    var subjectList: Map<String, DailySchedule> = emptyMap()

    /** 从JSON更新课程表 */
    fun updateSubjectListFromJson(json: String) {
        println("正在解析课程表数据")
        subjectList = parseSubjectJson(json)
        println("课程表: $subjectList")
    }

    // ==================== 可观察状态 ====================

    /** 网络是否可用 */
    private val _isInternetAvailable = MutableStateFlow(true)
    val isInternetAvailable: StateFlow<Boolean> get() = _isInternetAvailable
    fun setIsInternetAvailable(value: Boolean) { _isInternetAvailable.value = value }

    /** 是否正在加载 */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> get() = _isLoading
    fun setIsLoading(value: Boolean) { _isLoading.value = value }

    /** 程序远程开关 */
    private val _isOpen = MutableStateFlow(true)
    val isOpen: StateFlow<Boolean> get() = _isOpen
    fun setIsOpen(value: Boolean) { _isOpen.value = value }

    /** 悬浮窗按钮状态文字 */
    private val _buttonState = MutableStateFlow("点名")
    val buttonState: StateFlow<String> get() = _buttonState
    fun setButtonState(value: String) { _buttonState.value = value }

    /** 是否显示下节课提醒 */
    private val _isChangeFace = MutableStateFlow(false)
    val isChangeFace: StateFlow<Boolean> get() = _isChangeFace
    fun setIsChangeFace(value: Boolean) { _isChangeFace.value = value }

    /** 语音识别开关 */
    private val _isVoiceIdentify = MutableStateFlow(false)
    val isVoiceIdentify: StateFlow<Boolean> get() = _isVoiceIdentify
    fun setIsVoiceIdentify(value: Boolean) { _isVoiceIdentify.value = value }

    /** 倒数日开关 */
    private val _isCountDownDayOpen = MutableStateFlow(false)
    val isCountDownDayOpen: StateFlow<Boolean> get() = _isCountDownDayOpen
    fun setIsCountDownDayOpen(value: Boolean) { _isCountDownDayOpen.value = value }

    /** 时间提醒开关 */
    private val _isTime = MutableStateFlow(false)
    val isTime: StateFlow<Boolean> get() = _isTime
    fun setIsTime(value: Boolean) { _isTime.value = value }

    /** 当前日期 */
    private val _date = MutableStateFlow("无")
    val date: StateFlow<String> get() = _date
    fun setDate(value: String) { _date.value = value }

    /** 当前星期 */
    private val _week = MutableStateFlow("无")
    val week: StateFlow<String> get() = _week
    fun setWeek(value: String) { _week.value = value }

    /** 当前时间 */
    private val _time = MutableStateFlow("无")
    val time: StateFlow<String> get() = _time
    fun setTime(value: String) { _time.value = value }

    /** 幸运学生列表（JSON格式） */
    private val _luckyGuy = MutableStateFlow("无")
    val luckyGuy: StateFlow<String> get() = _luckyGuy
    fun setLuckyGuy(value: String) { _luckyGuy.value = value }

    /** 倒霉学生列表（JSON格式） */
    private val _poolGuy = MutableStateFlow("无")
    val poolGuy: StateFlow<String> get() = _poolGuy
    fun setPoolGuy(value: String) { _poolGuy.value = value }

    /** 是否长按悬浮窗 */
    private val _isLongPressed = MutableStateFlow(false)
    val isLongPressed: StateFlow<Boolean> get() = _isLongPressed
    fun setIsLongPressed(value: Boolean) { _isLongPressed.value = value }

    /** 彩蛋开关 */
    private val _isEasterEgg = MutableStateFlow(false)
    val isEasterEgg: StateFlow<Boolean> get() = _isEasterEgg
    fun setIsEasterEgg(value: Boolean) { _isEasterEgg.value = value }

    /** 是否正在拖动悬浮窗 */
    private val _isDragging = MutableStateFlow(false)
    val isDragging: StateFlow<Boolean> get() = _isDragging
    fun setIsDragging(value: Boolean) { _isDragging.value = value }

    /** 倒计时类型（0=未开始, 1=1分钟, 2=3分钟, 3=5分钟, 4=10分钟） */
    private val _countDownType = MutableStateFlow(0)
    val countDownType: StateFlow<Int> get() = _countDownType
    fun setCountDownType(value: Int) { _countDownType.value = value }

    /** 倒计时功能开关 */
    private val _isCountDownOpen = MutableStateFlow(false)
    val isCountDownOpen: StateFlow<Boolean> get() = _isCountDownOpen
    fun setIsCountDownOpen(value: Boolean) { _isCountDownOpen.value = value }

    /** 动态壁纸开关 */
    private val _isWallpaper = MutableStateFlow(false)
    val isWallpaper: StateFlow<Boolean> get() = _isWallpaper
    fun setIsWallpaper(value: Boolean) { _isWallpaper.value = value }

    /** 删除壁纸开关 */
    private val _isDeleteWallpaper = MutableStateFlow(false)
    val isDeleteWallpaper: StateFlow<Boolean> get() = _isDeleteWallpaper
    fun setIsDeleteWallpaper(value: Boolean) { _isDeleteWallpaper.value = value }

    /** 最小化开关 */
    private val _isMinimize = MutableStateFlow(false)
    val isMinimize: StateFlow<Boolean> get() = _isMinimize
    fun setIsMinimize(value: Boolean) { _isMinimize.value = value }

    /** 学习模式开关（英语单词识别） */
    private val _isLearning = MutableStateFlow(false)
    val isLearning: StateFlow<Boolean> get() = _isLearning
    fun setIsLearning(value: Boolean) { _isLearning.value = value }

    /** OCR远程自动识别开关 */
    private val _isLearningRemoteEnabled = MutableStateFlow(false)
    val isLearningRemoteEnabled: StateFlow<Boolean> get() = _isLearningRemoteEnabled
    fun setIsLearningRemoteEnabled(value: Boolean) { _isLearningRemoteEnabled.value = value }

    /** OCR触发来源 */
    private val _learningTriggerMode = MutableStateFlow(LearningTriggerMode.MANUAL)
    val learningTriggerMode: StateFlow<LearningTriggerMode> get() = _learningTriggerMode
    fun startLearning(triggerMode: LearningTriggerMode = LearningTriggerMode.MANUAL) {
        _learningTriggerMode.value = triggerMode
        _isLearning.value = true
    }
    fun finishLearning() {
        _isLearning.value = false
        _learningTriggerMode.value = LearningTriggerMode.MANUAL
    }

    /** 闹钟开关 */
    private val _isAlarmClock = MutableStateFlow(false)
    val isAlarmClock: StateFlow<Boolean> get() = _isAlarmClock
    fun setIsAlarmClock(value: Boolean) { _isAlarmClock.value = value }

    /** 闹钟是否已响过 */
    private val _isAlarmHasBeenHeard = MutableStateFlow(false)
    val isAlarmHasBeenHeard: StateFlow<Boolean> get() = _isAlarmHasBeenHeard
    fun setIsAlarmHasBeenHeard(value: Boolean) { _isAlarmHasBeenHeard.value = value }

    /** 课程表窗口开关 */
    private val _isScheduleOpen = MutableStateFlow(false)
    val isScheduleOpen: StateFlow<Boolean> get() = _isScheduleOpen
    fun setIsScheduleOpen(value: Boolean) { _isScheduleOpen.value = value }

    /** 快捷工具面板开关 */
    private val _isQuickToolsOpen = MutableStateFlow(false)
    val isQuickToolsOpen: StateFlow<Boolean> get() = _isQuickToolsOpen
    fun setIsQuickToolsOpen(value: Boolean) { _isQuickToolsOpen.value = value }

    // ==================== 随机点名逻辑 ====================

    /** 最近被点到的学生记录（防止连续重复） */
    private val recentStudents = ArrayDeque<Student>()
    /** 最近记录的最大长度 */
    private const val MAX_RECENT_STUDENTS = 30

    /**
     * 随机选择一个学生
     * 使用权重随机算法，避免连续点到同一个学生
     * @return 姓名的Pair（姓, 名），如果学生列表为空或没有有效学生则返回null
     */
    fun getRandomStudent(): Pair<String, String>? {
        println("正在执行随机点名！")

        if (studentList.isEmpty()) return null

        // 过滤出姓名不为空的有效学生
        val validStudents = studentList.filter { it.name.isNotEmpty() }
        if (validStudents.isEmpty()) return null

        // 过滤掉最近被点到的学生
        val availableStudents = validStudents.filter { !recentStudents.contains(it) }

        // 如果所有学生都被点过，清空记录重新开始
        val candidates = if (availableStudents.isEmpty()) {
            recentStudents.clear()
            validStudents
        } else {
            availableStudents
        }

        // 打乱顺序增加随机性
        val shuffledStudents = candidates.shuffled()

        // 计算可用学生的总权重
        val totalWeight = shuffledStudents.sumOf { it.probability }

        // 如果总权重为0（所有学生权重都为0），使用均匀随机选择
        val selectedStudent: Student = if (totalWeight <= 0) {
            shuffledStudents.random()
        } else {
            // 使用权重进行随机选择
            val randomWeight = Random.nextInt(0, totalWeight)
            var cumulativeWeight = 0
            var picked: Student? = null

            for (student in shuffledStudents) {
                cumulativeWeight += student.probability
                if (randomWeight < cumulativeWeight) {
                    picked = student
                    break
                }
            }
            picked ?: shuffledStudents.random()
        }

        // 加入最近记录
        recentStudents.add(selectedStudent)
        if (recentStudents.size > MAX_RECENT_STUDENTS) {
            recentStudents.removeFirst()
        }

        // 分离姓和名
        val name = selectedStudent.name
        val firstName = name.firstOrNull()?.toString() ?: ""
        val lastName = name.drop(1)

        // 记录点名次数
        com.rollcall.app.util.FileHelper.recordAttendance(name)

        return Pair(firstName, lastName)
    }
}
