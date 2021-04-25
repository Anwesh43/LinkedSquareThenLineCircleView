package com.example.squarethenlinecircleview

import android.view.View
import android.view.MotionEvent
import android.app.Activity
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.content.Context

val parts : Int = 4
val scGap : Float = 0.02f / parts
val strokeFactor : Float = 90f
val sizeFactor : Float = 2.9f
val delay : Long = 20
val colors : Array<Int> = arrayOf(
    "#f44336",
    "#9C27B0",
    "#6200EA",
    "#00C853",
    "#795548"
).map {
    Color.parseColor(it)
}.toTypedArray()
val backColor : Int = Color.parseColor("#BDBDBD")

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.sinify() : Float = Math.sin(this * Math.PI).toFloat()

fun Canvas.drawSquareThenLineCircle(scale : Float, w : Float, h : Float, paint : Paint) {
    val size : Float = Math.min(w, h) / sizeFactor
    val sf : Float = scale.sinify()
    val sf1 : Float = sf.divideScale(0, parts)
    val sf2 : Float = sf.divideScale(1, parts)
    val sf3 : Float = sf.divideScale(2, parts)
    val sf4 : Float = sf.divideScale(3, parts)
    save()
    translate(w / 2, h / 2)
    for (j in 0..1) {
        save()
        scale(1f - 2 * j, 1f)
        drawRect(RectF(-size / 2, -(size / 2) * sf1, size / 2, -(size / 2) * sf2), paint)
        drawLine(0f, (-size / 2) * sf2, 0f, (size / 2) * sf3, paint)
        restore()
    }
    drawArc(RectF(-size / 2, -size / 2, size / 2, size / 2), 0f, 360f * sf4, true, paint)
    restore()
}

fun Canvas.drawSTLCNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    paint.color = colors[i]
    paint.strokeCap = Paint.Cap.ROUND
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    drawSquareThenLineCircle(scale, w, h, paint)
}

class SquareThenLineCircleView(ctx : Context) : View(ctx) {

    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scGap * dir
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class STLCNode(var i : Int, val state : State = State()) {

        private var next : STLCNode? = null
        private var prev : STLCNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < colors.size - 1) {
                next = STLCNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawSTLCNode(i, state.scale, paint)
        }

        fun update(cb : (Float) -> Unit) {
            state.update(cb)
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : STLCNode {
            var curr : STLCNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class SquareThenLineCircle(var i : Int) {

        private var curr : STLCNode = STLCNode(0)
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            curr.draw(canvas, paint)
        }

        fun update(cb : (Float) -> Unit) {
            curr.update {
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : SquareThenLineCircleView) {

        private val animator : Animator = Animator(view)
        private val stlc : SquareThenLineCircle = SquareThenLineCircle(0)
        private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun render(canvas : Canvas) {
            canvas.drawColor(backColor)
            stlc.draw(canvas, paint)
            animator.animate {
                stlc.update {
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            stlc.startUpdating {
                animator.start()
            }
        }
    }
}