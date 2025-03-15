import androidx.compose.runtime.*
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.*
import com.sun.jna.platform.win32.WinUser
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Frame
import java.io.File
import javax.swing.JFrame
import javax.swing.SwingUtilities

@Composable
fun videoWallpaper(videoPath: String) {
    // 收集全局状态 isWallpaper
    val isWallpaper = Global.isWallpaper.collectAsState()

    // 使用 remember 保存 MediaPlayer、JFXPanel 和 JFrame 的引用
    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }
    var fxPanel: JFXPanel? by remember { mutableStateOf(null) }
    var frame: JFrame? by remember { mutableStateOf(null) }
    var isRunWallpaper by remember { mutableStateOf(false) }

    JFXPanel()

    // 根据 isWallpaper 状态管理视频壁纸的生命周期
    LaunchedEffect(isWallpaper.value) {
        if (isWallpaper.value) {
            if (!isRunWallpaper) {
                isRunWallpaper = true

                // 初始化视频播放器和窗口
                val user32 = User32.INSTANCE
                val progman = user32.FindWindow("Progman", null)
                if (progman != null) {
                    // 发送消息让 Windows Explorer 创建 "WorkerW" 窗口
                    user32.SendMessage(progman, 0x052C, WPARAM(0), LPARAM(0))
                    // 查找 WorkerW 窗口
                    val workerW = findWorkerW(user32)
                    if (workerW != null) {
                        // 隐藏 WorkerW，避免遮挡桌面图标
                        user32.ShowWindow(workerW, WinUser.SW_HIDE)

                        // 创建新的 JFrame 并嵌入桌面
                        frame = JFrame("Video Wallpaper").apply {
                            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
                            isUndecorated = true
                            setSize(1920, 1080)
                            background = Color(0, 0, 0, 0) // 透明背景
                            isVisible = true
                        }
                        val hwndFrame = getHwnd(frame!!)
                        if (hwndFrame != null) {
                            user32.SetParent(hwndFrame, progman)
                        }

                        // 创建新的 JFXPanel，并加入到 JFrame 中
                        fxPanel = JFXPanel()
                        fxPanel?.let { frame!!.contentPane.add(it, BorderLayout.CENTER) }

                        // 在 JavaFX 线程中初始化媒体播放器及其 Scene
                        Platform.runLater {
                            val media = javafx.scene.media.Media(File(videoPath).toURI().toString())
                            mediaPlayer = MediaPlayer(media).apply {
                                isAutoPlay = true
                                cycleCount = MediaPlayer.INDEFINITE
                                volume = 0.0
                                rate = 1.0
                            }
                            val mediaView = MediaView(mediaPlayer).apply {
                                fitWidth = 1920.0
                                fitHeight = 1080.0
                                isPreserveRatio = false
                            }
                            val scene = Scene(StackPane(mediaView), 1920.0, 1080.0)
                            fxPanel?.scene = scene
                        }
                    }
                }
            }
        } else {
            // 当 isWallpaper 为 false 时，清理所有资源
            SwingUtilities.invokeLater {
                frame?.dispose()   // 关闭窗口
                frame = null       // 置空引用，确保下次创建全新实例
            }
            Platform.runLater {
                fxPanel?.scene = null
                mediaPlayer?.dispose()  // 正确释放 MediaPlayer
                mediaPlayer = null      // 置空引用
                fxPanel = null          // 置空引用
            }
            isRunWallpaper = false  // 重置运行标志，允许下次重新初始化
        }
    }
}

fun findWorkerW(user32: User32): HWND? {
    user32.FindWindow("Progman", null)
    var workerW: HWND? = null
    user32.EnumWindows({ windowHwnd, _ ->  // 修改这里，避免与外部变量冲突
        val shellDllDefView = user32.FindWindowEx(windowHwnd, null, "SHELLDLL_DefView", null)
        if (shellDllDefView != null) {
            workerW = user32.FindWindowEx(null, windowHwnd, "WorkerW", null)
            return@EnumWindows false
        }
        true
    }, null)
    return workerW
}


fun getHwnd(frame: Frame): HWND? {
    frame.isVisible = true // 确保窗口已创建
    val pointer = Native.getComponentPointer(frame)
    return if (pointer != Pointer.NULL) HWND(pointer) else null
}
