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
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import kotlinx.coroutines.*
import java.awt.*
import java.io.File
import javax.swing.JFrame

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun videoWallpaper() {

    val isWallpaper = Global.isWallpaper.collectAsState()
    val isDeleteWallpaper = Global.isDeleteWallpaper.collectAsState()
    val isDownloadSuccessfully = remember { mutableStateOf(false) }
    var downloadVideo by remember { mutableStateOf(false) }

    LaunchedEffect(isWallpaper.value) {
        if (isWallpaper.value) {
            withContext(Dispatchers.IO) {
                while (!downloadVideo) {
                    downloadVideo = checkAndCopyModel(
                        "http://xyc.okc.today/Wallpaper.zip", File("D:/Xiaoye/"), File("D:/Xiaoye/Wallpaper/")
                    )
                    if (downloadVideo) {
                        isDownloadSuccessfully.value = true
                        break
                    }
                    delay(3000)
                }
            }
        }
    }

    LaunchedEffect(isDeleteWallpaper.value) {
        if (isDeleteWallpaper.value) {
            deleteFileOrDirectory("D:/Xiaoye/Wallpaper/")
        }
    }

    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }
    var fxPanel: JFXPanel? by remember { mutableStateOf(null) }
    var frame: JFrame? by remember { mutableStateOf(null) }
    var isRunWallpaper by remember { mutableStateOf(false) }

    JFXPanel()

    LaunchedEffect(isWallpaper.value, isDownloadSuccessfully.value) {
        if (isWallpaper.value) {

            if (isDownloadSuccessfully.value) {

                println("has been download")

                if (!isRunWallpaper) {

                    println("has been start")

                    isRunWallpaper = true

                    val user32 = User32.INSTANCE
                    val progman = user32.FindWindow("Progman", null)
                    if (progman != null) {
                        user32.SendMessage(progman, 0x052C, WPARAM(0), LPARAM(0))

                        val workerW = findWorkerW(user32)
                        if (workerW != null) {

                            user32.ShowWindow(workerW, WinUser.SW_HIDE)


                            val screenSize: Dimension = Toolkit.getDefaultToolkit().screenSize

                            frame = JFrame("Video Wallpaper").apply {
                                defaultCloseOperation = JFrame.EXIT_ON_CLOSE
                                isUndecorated = true
                                setSize(screenSize.width, screenSize.height)
                                background = Color(0, 0, 0, 0)
                                isVisible = true
                            }

                            val hwndFrame = getHwnd(frame!!)

                            if (hwndFrame != null) {
                                user32.SetParent(hwndFrame, progman)
                            }

                                fxPanel = JFXPanel()
                                fxPanel?.let { frame!!.contentPane.add(it, BorderLayout.CENTER) }

                                Platform.runLater {
                                    val media = Media(File("D:/Xiaoye/Wallpaper/wallpaper.mp4").toURI().toString())
                                    mediaPlayer = MediaPlayer(media).apply {
                                        isAutoPlay = true
                                        cycleCount = MediaPlayer.INDEFINITE
                                        volume = 0.0
                                        rate = 1.0
                                    }
                                    val mediaView = MediaView(mediaPlayer).apply {
                                        fitWidth = screenSize.width.toDouble()
                                        fitHeight = screenSize.height.toDouble()
                                        isPreserveRatio = false
                                    }
                                    fxPanel?.scene = Scene(StackPane(mediaView), screenSize.width.toDouble(), screenSize.height.toDouble())
                                }

                        }
                    }
                }
            }
        } else {
            Platform.runLater {
                fxPanel?.scene = null
                mediaPlayer?.dispose()
                mediaPlayer = null
                fxPanel = null
            }
            isRunWallpaper = false
        }
    }
}

fun findWorkerW(user32: User32): HWND? {
    user32.FindWindow("Progman", null)
    var workerW: HWND? = null
    user32.EnumWindows({ windowHwnd, _ ->
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
    frame.isVisible = true
    val pointer = Native.getComponentPointer(frame)
    return if (pointer != Pointer.NULL) HWND(pointer) else null
}
