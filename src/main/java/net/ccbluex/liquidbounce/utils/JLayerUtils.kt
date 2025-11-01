package net.ccbluex.liquidbounce.utils

import javazoom.jl.decoder.JavaLayerException
import javazoom.jl.player.Player
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread

/**
 * JLayer工具类，用于播放MP3文件
 */
object JLayerUtils {
    // 使用线程池来管理音频播放线程
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor {
        Thread(it, "JLayer-Player").apply { isDaemon = true }
    }

    // 当前正在播放的Player实例
    private var currentPlayer: Player? = null
    private var isPlaying: Boolean = false
    private var currentPlaybackThread: Thread? = null

    /**
     * 播放MP3文件
     * @param filePath MP3文件路径
     * @return 是否成功开始播放
     */
    fun playMP3(resourcePath: String) {
        thread(start = true) { // 在新线程中播放，避免阻塞主线程
            try {
                // 使用当前类的ClassLoader加载资源
                val inputStream: InputStream = JLayerUtils::class.java.getResourceAsStream(resourcePath)
                    ?: throw IllegalArgumentException("音频资源未找到: $resourcePath")

                // 使用BufferedInputStream提高性能
                val bufferedStream = BufferedInputStream(inputStream)
                currentPlayer = Player(bufferedStream)
                currentPlayer?.play()

                // 播放完成后关闭流
                bufferedStream.close()
                inputStream.close()
                currentPlayer?.close()

            } catch (e: Exception) {
                println("JLayerUtils错误: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * 从InputStream播放MP3
     * @param inputStream MP3文件的输入流
     * @return 是否成功开始播放
     */
    fun playStream(inputStream: InputStream): Boolean {
        try {
            // 先停止当前播放的声音
            stop()

            // 创建Player实例
            currentPlayer = Player(inputStream)
            isPlaying = true

            // 在单独的线程中播放音乐
            currentPlaybackThread = Thread {
                try {
                    currentPlayer?.play()
                } catch (e: JavaLayerException) {
                    println("JLayerUtils: 播放过程出错: ${e.message}")
                    e.printStackTrace()
                } finally {
                    synchronized(this) {
                        isPlaying = false
                        currentPlayer = null
                        currentPlaybackThread = null
                        try {
                            inputStream.close()
                        } catch (ignored: Exception) {}
                    }
                }
            }

            // 提交到线程池执行
            executorService.submit(currentPlaybackThread)
            return true
        } catch (e: JavaLayerException) {
            println("JLayerUtils: 创建播放器失败: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * 停止播放
     */
    fun stop() {
        synchronized(this) {
            if (isPlaying && currentPlayer != null) {
                try {
                    currentPlayer?.close()
                } catch (ignored: Exception) {}
                isPlaying = false
                currentPlayer = null
                currentPlaybackThread = null
            }
        }
    }

    /**
     * 检查是否正在播放
     */
    fun isPlaying(): Boolean {
        return isPlaying
    }

    /**
     * 关闭播放器并释放资源
     */
    fun shutdown() {
        stop()
        executorService.shutdown()
    }
}