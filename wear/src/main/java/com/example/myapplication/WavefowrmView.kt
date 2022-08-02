package com.example.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class WavefowrmView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var paint = Paint()
    private var amplitudes = ArrayList<Float>()
    private var spikes = ArrayList<RectF>()

    private var radius = 2f
    private var w=4f
    private var d=2f

    private var sw = 0f
    private var sh = 80f

    private var maxSpikes = 0

    init {
        paint.color = Color.rgb(244,81,30)

        sw = resources.displayMetrics.widthPixels.toFloat()

        maxSpikes = (sw/(w+d)).toInt()
    }

    fun addAmplitude(amp: Float){
        var norm = Math.min(amp.toInt()/160,80).toFloat()
        amplitudes.add(norm)

        spikes.clear()
        var amps = amplitudes.takeLast(maxSpikes)
        for(i in amps.indices ){
            var left = sw - i*(w+d)
            var top = sh/2 - amps[i]/2
            var right = left + w
            var bottom = top+ amps[i]
            spikes.add(RectF(left, top, right, bottom))
        }


        invalidate()
    }

    fun clear(): ArrayList<Float>{
        var amps = amplitudes.clone() as ArrayList<Float>
        amplitudes.clear()
        spikes.clear()
        invalidate()
        return amps
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        spikes.forEach(){
            canvas?.drawRoundRect(it, radius, radius, paint)
        }
        //canvas?.drawRoundRect(RectF(4f,6f,4+6f,6f+12f),1f,1f, paint)
        //canvas?.drawRoundRect(RectF(12f,18f,12+18f,18f+32f),3f,3f, paint)
    }
}