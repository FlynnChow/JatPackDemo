package com.example.jatpackdemo

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class LuminosityAnalyzer : ImageAnalysis.Analyzer {
    private var lastAnalyzedTime = 0L

    private fun ByteBuffer.toBytyeArray():ByteArray{
        rewind()//缓冲区跳转到0
        val data = ByteArray(remaining())
        get(data)//将缓冲区复制到data数组中
        return data
    }

    override fun analyze(image: ImageProxy?, rotationDegrees: Int) {
        val currtentTime = System.currentTimeMillis()
        if(currtentTime - lastAnalyzedTime >= TimeUnit.SECONDS.toMillis(1)){
            //imageAnalyzedsis 颜色格式是YUV
            val buffer = image?.planes?.get(0)?.buffer
            //从回调对象中提取图像数据
            val data = buffer?.toBytyeArray()
            //像素转化为像素值
            val pixels = data?.map { it.toInt() and 0xFF }
            val luma = pixels?.average()
            lastAnalyzedTime = currtentTime
            Log.d("相机分析人：亮度是->","$luma")
        }
    }
}