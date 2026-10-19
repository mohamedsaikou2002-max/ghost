package com.casper.lab

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Purely synthetic waveform viewer. It never represents external RF measurements. */
class SpectrumDemoView(context: Context) : View(context) {
    var running = false
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val green = Color.rgb(75, 222, 159)
    private val muted = Color.rgb(45, 79, 67)
    private val white = Color.rgb(226, 241, 234)
    private var phase = 0.0

    init { minimumHeight = dp(260f) }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawColor(Color.rgb(6, 17, 14))
        p.strokeWidth = dp(1f); p.color = muted; p.alpha = 160
        for (i in 1..5) {
            val y = i * h / 6
            canvas.drawLine(dp(14f), y, w-dp(14f), y, p)
        }
        for (i in 1..7) {
            val x = i * w / 8
            canvas.drawLine(x, dp(14f), x, h-dp(18f), p)
        }
        p.alpha = 255
        val waveform = Path()
        val base = h * 0.68f
        val lineW = w - dp(28f)
        for (i in 0..280) {
            val u = i / 280.0
            val peaks = listOf(0.19 to 0.055, 0.43 to 0.033, 0.72 to 0.075, 0.87 to 0.024)
                .sumOf { (mu, sig) -> kotlin.math.exp(-0.5 * ((u-mu)/sig)*((u-mu)/sig)) }
            val noise = (sin(u*119 + phase)*0.11 + sin(u*217-phase*0.6)*0.055 + sin(u*481+phase)*0.02)
            val power = peaks*0.70 + noise*0.25
            val x = dp(14f) + (u*lineW).toFloat()
            val y = base - (power*h*0.57).toFloat()
            if (i == 0) waveform.moveTo(x,y) else waveform.lineTo(x,y)
        }
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(2f); p.color = green
        p.shader = LinearGradient(0f,0f,w,0f, white, green, Shader.TileMode.CLAMP)
        canvas.drawPath(waveform,p)
        p.shader = null; p.style = Paint.Style.FILL
        p.textSize = dp(10f); p.typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL)
        p.color = Color.rgb(126, 170, 149)
        canvas.drawText("SYNTHETIC FFT · ARBITRARY AXIS",dp(15f),dp(21f),p)
        canvas.drawText("TEST VECTOR — NOT SENSOR IQ",dp(15f),h-dp(11f),p)
        if (running) { phase += 0.08; postInvalidateDelayed(70) }
    }

    private fun dp(x: Float) = x * resources.displayMetrics.density
}

/** Rotatable synthetic 3D room/points; NOT a through-wall scan. */
class RoomSceneView(context: Context) : View(context) {
    var yaw = 0.45
    var zoom = 1.0
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lastX = 0f
    private val emerald = Color.rgb(81,219,166)
    private val white = Color.rgb(235,241,238)
    private val room = arrayOf(
        doubleArrayOf(-1.6,-0.9,-1.2), doubleArrayOf(1.6,-0.9,-1.2),
        doubleArrayOf(1.6,-0.9,1.2), doubleArrayOf(-1.6,-0.9,1.2),
        doubleArrayOf(-1.6,1.1,-1.2), doubleArrayOf(1.6,1.1,-1.2),
        doubleArrayOf(1.6,1.1,1.2), doubleArrayOf(-1.6,1.1,1.2)
    )
    init { minimumHeight = dp(300f) }

    private fun project(v: DoubleArray): Pair<Float,Float> {
        val x = v[0]*cos(yaw)-v[2]*sin(yaw)
        val z = v[0]*sin(yaw)+v[2]*cos(yaw)
        val y = v[1]*cos(0.27)-z*sin(0.27)
        val depth = 5.5 + v[1]*sin(0.27)+z*cos(0.27)
        val scale = minOf(width.toDouble()/3.8, height.toDouble()/3.3)*zoom*3.8/depth
        return Pair((width/2.0 + x*scale).toFloat(),(height/2.0 - y*scale).toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(5,16,13))
        p.textSize = dp(10f); p.typeface = android.graphics.Typeface.MONOSPACE; p.color = Color.rgb(126,170,149)
        canvas.drawText("SIMULATED ENVIRONMENT · ROTATE TO INSPECT",dp(12f),dp(18f),p)
        val pts = room.map { project(it) }
        p.style = Paint.Style.STROKE; p.strokeWidth = dp(1.4f)
        for ((a,b) in listOf(0 to 1,1 to 2,2 to 3,3 to 0,4 to 5,5 to 6,6 to 7,7 to 4,0 to 4,1 to 5,2 to 6,3 to 7)) {
            p.color = if (a < 4 && b < 4) emerald else Color.rgb(92,135,119)
            p.alpha = if (a < 4 && b < 4) 210 else 130
            canvas.drawLine(pts[a].first,pts[a].second,pts[b].first,pts[b].second,p)
        }
        p.style = Paint.Style.FILL; p.alpha=255
        // Deterministic mathematical test points, never human locations.
        for (i in 0 until 45) {
            val x = -1.2 + (i * 37 % 23)/23.0*2.4
            val z = -0.8 + (i * 17 % 19)/19.0*1.6
            val y = -0.85 + (i * 11 % 13)/13.0*1.4
            val center=project(doubleArrayOf(x,y,z))
            p.color = if (i % 5 == 0) white else emerald
            p.alpha = 70 + i%6*25
            canvas.drawCircle(center.first,center.second,dp(if (i%5==0) 2.7f else 1.5f),p)
        }
        p.alpha=255; p.color=Color.rgb(126,170,149)
        canvas.drawText("DRAG TO ROTATE • SYNTHETIC POINT CLOUD",dp(12f),height-dp(13f),p)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { lastX = event.x; return true }
            MotionEvent.ACTION_MOVE -> { yaw += (event.x-lastX)/260.0; lastX=event.x; invalidate(); return true }
        }
        return true
    }
    private fun dp(x: Float) = x * resources.displayMetrics.density
}
