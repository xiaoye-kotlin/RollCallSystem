package com.rollcall.app.ui.screen

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
import com.rollcall.app.network.NetworkHelper.checkAndCopyModel
import com.rollcall.app.network.NetworkHelper.getResourcePackageUrl
import com.rollcall.app.state.AppState
import com.rollcall.app.util.deleteFileOrDirectory
import javax.swing.JFrame
import javax.swing.WindowConstants

@Composable
fun videoWallpaper() {

    val isWallpaper = AppState.isWallpaper.collectAsState()
    val isDeleteWallpaper = AppState.isDeleteWallpaper.collectAsState()
    val isDownloadSuccessfully = remember { mutableStateOf(false) }
    var downloadVideo by remember { mutableStateOf(false) }

    LaunchedEffect(isWallpaper.value) {
        if (isWallpaper.value) {
            withContext(Dispatchers.IO) {
                while (!downloadVideo) {
                    downloadVideo = checkAndCopyModel(
                        getResourcePackageUrl("Wallpaper.zip"), File("D:/Xiaoye/"), File("D:/Xiaoye/Wallpaper/")
                    )
                    if (downloadVideo) {
                        isDownloadSuccessfully.value = true
                        break
                    }
                    delay(3000)
                }
            }
        } else {
            downloadVideo = false
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
    // Platform.runLater 之前必须先初始化 JavaFX toolkit，否则部分机器会直接抛出
    remember { JFXPanel() }

    DisposableEffect(Unit) {
        onDispose {
            Platform.runLater {
                mediaPlayer?.stop()
                mediaPlayer?.dispose()
                mediaPlayer = null
                fxPanel?.scene = null
                fxPanel = null
            }
            frame?.dispose()
            frame = null
            isRunWallpaper = false
        }
    }

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
                                defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
                                isUndecorated = true
                                setSize(screenSize.width, screenSize.height)
                                background = Color(0, 0, 0, 0)
                                isVisible = true
                            }

                            val createdFrame = frame ?: return@LaunchedEffect

                            val hwndFrame = getHwnd(createdFrame)

                            if (hwndFrame != null) {
                                user32.SetParent(hwndFrame, progman)
                            }

                            fxPanel = JFXPanel()
                            fxPanel?.let { createdFrame.contentPane.add(it, BorderLayout.CENTER) }

                            Platform.runLater {
                                mediaPlayer?.stop()
                                mediaPlayer?.dispose()

                                val media = Media(File("D:/Xiaoye/Wallpaper/wallpaper.mp4").toURI().toString())
                                mediaPlayer = MediaPlayer(media).apply {
                                    isAutoPlay = true
                                    cycleCount = MediaPlayer.INDEFINITE
                                    volume = 0.0
                                    rate = 1.0
                                }
                                val activePlayer = mediaPlayer ?: return@runLater
                                val mediaView = MediaView(activePlayer).apply {
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
                mediaPlayer?.stop()
                mediaPlayer?.dispose()
                mediaPlayer = null
                fxPanel = null
            }
            frame?.dispose()
            frame = null
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
