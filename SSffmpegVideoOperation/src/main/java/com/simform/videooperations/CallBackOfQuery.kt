package com.simform.videooperations

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.util.concurrent.CyclicBarrier

/**
 * Created by Ashvin Vavaliya on 22,January,2021
 * Simform Solutions Pvt Ltd.
 */
class CallBackOfQuery {
    fun callQuery(query: Array<String>, fFmpegCallBack: FFmpegCallBack) {
        val gate = CyclicBarrier(2)
        object : Thread() {
            override fun run() {
                gate.await()
                process(query, fFmpegCallBack)
            }
        }.start()
        gate.await()
    }

    fun cancelProcess(executionId: Long) {
        if (!executionId.equals(0)) {
            FFmpegKit.cancel(executionId)
        } else {
            FFmpegKit.cancel()
        }
    }

    fun cancelProcess() {
        FFmpegKit.cancel()
    }

    private fun process(query: Array<String>, ffmpegCallBack: FFmpegCallBack) {
        val processHandler = Handler(Looper.getMainLooper())
        FFmpegKitConfig.enableLogCallback { logMessage ->
            val logs = LogMessage(logMessage.sessionId, logMessage.level, logMessage.message)
            processHandler.post {
                ffmpegCallBack.process(logs)
            }
        }
        FFmpegKitConfig.enableStatisticsCallback { statistics ->
            val statisticsLog =
                Statistics(
                    statistics.sessionId,
                    statistics.videoFrameNumber,
                    statistics.videoFps,
                    statistics.videoQuality,
                    statistics.size,
                    statistics.time.toInt(),
                    statistics.bitrate,
                    statistics.speed
                )
            processHandler.post {
                ffmpegCallBack.statisticsProcess(statisticsLog)
            }
        }

        val commandArray = arrayOf(
            "-i", "/storage/emulated/0/Download/VideoDownloader/tikto.mp4",
            "-i", "/storage/emulated/0/Download/tiktok_com_copy.mp4",
            "-f", "lavfi", "-t", "0.1", "-i", "anullsrc",
            "-filter_complex", "[0:v]scale=512x512,setdar=1:1[v0];[1:v]scale=512x512,setdar=1:1[v1];[v0][0:a][v1][1:a]concat=n=2:v=1:a=1[outv][outa]",
            "-map", "[outv]", "-map", "[outa]",
            "-preset", "ultrafast",
            "/storage/emulated/0/Android/data/com.simform.videoimageeditor/files/Output/Output_fixed.mp4"
        )


        val commandString = query.joinToString(" ")

        val session = FFmpegKit.execute(commandString)
        session.output
        when (session.returnCode.value) {
            ReturnCode.SUCCESS -> {
                processHandler.post {
                    ffmpegCallBack.success()
                }
            }
            ReturnCode.CANCEL -> {
                processHandler.post {
                    ffmpegCallBack.cancel()
                    FFmpegKit.cancel()
                }
            }
            else -> {
                processHandler.post {
                    ffmpegCallBack.failed()
                }
            }
        }
    }
}