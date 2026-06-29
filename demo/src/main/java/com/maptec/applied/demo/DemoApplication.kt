package com.maptec.applied.demo

import android.app.Application
import android.content.Context
import android.os.Debug
//import com.bytedance.raphael.*
import com.maptec.applied.javabase.log.LoggerFactory
import java.io.File
import com.maptec.applied.MapSDK
import com.maptec.applied.javabase.log.LogLevel


private val raphaelLog = LoggerFactory.getLogger(LOG_MODULE).withTag("Raphael")
private val memoryLog = LoggerFactory.getLogger(LOG_MODULE).withTag("Memory")

class DemoApplication: Application() {


    override fun onCreate() {
        super.onCreate()

        LoggerFactory.initialize(this, LogLevel.VERBOSE)

        MapSDK.getInstance(applicationContext,null).initialize(null)

     //   initRaphael(applicationContext)
    }
}

private fun initRaphael( context:Context) {
    // 创建Raphael存储目录
    val raphaelDir = File(context.getExternalFilesDir(null),"raphael")
    if(!raphaelDir.exists()){
        raphaelLog.d { "create raphael dir" }
        raphaelDir.mkdir()
    }else{
        raphaelLog.d { "raphael dir ${raphaelDir.absolutePath} exists" }
    }

//    Raphael.start(
//        Raphael.ALLOC_MODE or 0x0F0000 or 1024,
//        raphaelDir.absolutePath,  // need sdcard permission
//        null
//    )
//
//    Thread(Runnable {
//            while(true) {
//                val memoryInfo = Debug.MemoryInfo()
//                var maxLen = 0
//                Debug.getMemoryInfo(memoryInfo) // 无需传入PID
//
//                val pssKb: Int = memoryInfo.getTotalPss() // 单位：KB
//                val pssMb = (pssKb / 1024).toLong() // 转为 MB（整数除法）
//                if (pssMb >= 300 && pssMb > maxLen) {
//                    Raphael.print()
//                    maxLen = pssKb
//                    memoryLog.d { "PSS = $pssKb KB ($pssMb MB)" }
//                }
//
//                Thread.sleep(1000*10)
//        }
//    }).start()
}