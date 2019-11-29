package com.liang.animation

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetManager
import android.graphics.*
import android.text.TextUtils
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.annotation.ArrayRes
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/**
 * TODO: document your custom view class.
 */
class FrameAnimationView : SurfaceView, SurfaceHolder.Callback, Runnable {


    private val executors =
        Executors.newSingleThreadExecutor()

    private var assetsFolder = ""
    private val strings = ArrayList<String>()
    private val resIds = ArrayList<Int>()

    private var isStart = false
    private var isPause = false
    private var isAutoStart = false
    private var isRunning = false
    private var isInitialized = false
    private var duration: Long = 0
    private var isLoop = false
    private val srcRect = Rect()
    private var destRect: Rect? = null
    private val paint = Paint()
    private var index = 0

    private var isDestroyed = true

    private var assetsManager: AssetManager? = null

    private var action: ((AnimState, Float) -> Unit?)? = null


    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val typedArray = context.obtainStyledAttributes(
            attrs, R.styleable.FrameAnimationView, defStyle, 0
        )

        isLoop = typedArray.getBoolean(R.styleable.FrameAnimationView_loop, false)
        duration = typedArray.getInt(R.styleable.FrameAnimationView_duration, 100).toLong()
        isAutoStart = typedArray.getBoolean(R.styleable.FrameAnimationView_autoStart, false)
        typedArray.getString(R.styleable.FrameAnimationView_animAssets)?.let {
            setAnimAssets(it)
        }
        setAnimResource(typedArray.getResourceId(R.styleable.FrameAnimationView_animResource, 0))

        typedArray.recycle()

        val surfaceHolder = holder
        surfaceHolder.addCallback(this)
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_HARDWARE)
        setZOrderOnTop(true)
        surfaceHolder.setFormat(PixelFormat.TRANSLUCENT)
        paint.isAntiAlias = true
        assetsManager = context.applicationContext.assets

    }


    fun observe(action: ((AnimState, Float) -> Unit)) {
        this.action = action
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        action = null
        stop()
        isLoop = false
        executors.shutdown()
        strings.clear()
        resIds.clear()
    }

    fun isDestroyed(): Boolean {
        return isDestroyed
    }

    fun getDuration(): Long {
        return duration
    }

    fun setDuration(duration: Long) {
        this.duration = duration
    }

    fun isLoop(): Boolean {
        return isLoop
    }

    fun setLoop(loop: Boolean) {
        isLoop = loop
    }

    fun getAssetsFolder(): String? {
        return assetsFolder
    }

    fun isStart(): Boolean {
        return isStart
    }

    fun isPause(): Boolean {
        return isPause
    }

    fun isAutoStart(): Boolean {
        return isAutoStart
    }

    fun isRunning(): Boolean {
        return isRunning
    }

    fun isInitialized(): Boolean {
        return isInitialized
    }

    fun getStrings(): ArrayList<String>? {
        return strings
    }

    fun getResIds(): ArrayList<Int>? {
        return resIds
    }

    fun getIndex(): Int {
        return index
    }

    @SuppressLint("Recycle")
    fun setAnimResource(@ArrayRes arrayRes: Int) {
        if (arrayRes == 0) {
            return
        }
        strings.clear()
        resIds.clear()
        index = 0
        executors.execute {
            val typedArray = resources.obtainTypedArray(arrayRes)
            for (i in 0 until typedArray.length()) {
                resIds.add(typedArray.getResourceId(i, 0))
            }
            typedArray.recycle()
            checkStart()
        }
    }

    fun setAnimAssets(assetsFolder: String) {
        if (TextUtils.isEmpty(assetsFolder)) {
            return
        }
        resIds.clear()
        strings.clear()
        this.assetsFolder = assetsFolder
        index = 0
        executors.execute {
            try {
                assetsManager?.let {
                    it.list(assetsFolder)?.let { its ->
                        strings.addAll(its.toList())
                        checkStart()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun checkStart() {
        if (isAutoStart || isStart) {
            start()
        }
    }

    fun start() {
        if (isDestroyed || isRunning || !isInitialized) {
            return
        }
        isStart = true
        executors.execute(this)
    }

    fun stop() {
        isStart = false
        isAutoStart = false
        isPause = false
    }

    fun pause() {
        isPause = isRunning
        isStart = false
        isAutoStart = false
    }

    fun resume() {
        if (isAutoStart || isPause || isStart) {
            start()
        }
    }

    fun reStart() {
        index = 0
        start()
    }

    fun setProgress(progress: Float) {
        if (isRunning) {
            return
        }
        var offset = progress
        if (progress < 0) {
            offset = 0f
        }
        if (progress > 1) {
            offset = 1f
        }
        if (resIds.size > 0) {
            index = ((resIds.size - 1) * offset).roundToInt()
        } else if (strings.size > 0) {
            index = ((strings.size - 1) * offset).roundToInt()
        }
        drawFame(index)
    }

    fun getProgress(): Float {
        var progress = when {
            resIds.size > 0 -> {
                index * 1f / resIds.size
            }
            strings.size > 0 -> {
                index * 1f / strings.size
            }
            else -> {
                0f
            }
        }
        return when {
            progress > 1f -> 1f
            progress < 0f -> 0f
            else -> progress
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        isDestroyed = false
    }

    override fun surfaceChanged(
        holder: SurfaceHolder?,
        format: Int,
        width: Int,
        height: Int
    ) {
        destRect = Rect(0, 0, width, height)
        isInitialized = true
        drawFame(index)
        resume()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        isDestroyed = true
        pause()
    }

    override fun run() {
        if (resIds.isNotEmpty()) {
            runRes()
        }
        if (strings.isNotEmpty()) {
            runAssets()
        }
    }


    fun drawFame(index: Int) {
        if (isDestroyed) {
            return
        }
        executors.execute {
            if (resIds.isNotEmpty() && index < resIds.size) {
                drawRes(resIds[index])
            } else if (strings.isNotEmpty() && index < strings.size) {
                drawAssets(assetsFolder + "/" + strings[index])
            }
        }
    }


    private fun runRes() {
        if (resIds.isNotEmpty()) {
            isRunning = true
            postAnimationStart()
            do {
                drawRes(resIds[index])
                index++
                if (isLoop && index == resIds.size) {
                    index = 0
                    postAnimationRepeat()
                }

                postAnimationRunning()

                try {
                    Thread.sleep(duration)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            } while (!isDestroyed && isStart && index < resIds.size)
            refreshIndex(resIds.size)
            isRunning = false
            postAnimationEnd()
        }
    }

    private fun runAssets() {
        if (strings.isNotEmpty()) {
            isRunning = true
            postAnimationStart()
            do {
                drawAssets(assetsFolder + "/" + strings[index])
                index++
                if (isLoop && index == strings.size) {
                    index = 0
                    postAnimationRepeat()
                }

                postAnimationRunning()

                try {
                    Thread.sleep(duration)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            } while (!isDestroyed && isStart && index < strings.size)
            refreshIndex(strings.size)
            isRunning = false
            postAnimationEnd()
        }
    }


    private fun refreshIndex(count: Int) {
        if (index >= count) {
            index = 0
        }
        index = 0.coerceAtLeast(index - 1)
    }

    private fun drawRes(resId: Int) {
        drawBitmap(getBitmap(resId))
    }

    private fun drawAssets(name: String) {
        try {
            drawBitmap(BitmapFactory.decodeStream(assetsManager!!.open(name)))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun drawBitmap(bitmap: Bitmap?) {
        synchronized(this) {
            if (isDestroyed) {
                return
            }
            val surfaceHolder = holder
            if (surfaceHolder == null || bitmap == null) {
                return
            }
            val canvas = surfaceHolder.lockCanvas()
            if (canvas != null) {
                try {
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    if (!bitmap.isRecycled) {
                        srcRect[0, 0, bitmap.width] = bitmap.height
                        canvas.drawBitmap(bitmap, srcRect, destRect!!, paint)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                    bitmap.recycle()
                }
            }
        }
    }

    private fun postAnimationStart() {
        post {
            action?.let {
                it(AnimState.Start, getProgress())
            }
        }
    }

    private fun postAnimationEnd() {
        post {
            action?.let {
                it(AnimState.End, getProgress())
            }
        }
    }

    private fun postAnimationRepeat() {
        post {
            action?.let {
                it(AnimState.Repeat, getProgress())
            }
        }
    }

    private fun postAnimationRunning() {
        post {
            action?.let {
                it(AnimState.Running, getProgress())
            }
        }
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        if (visibility != View.VISIBLE) {
            stop()
        }
    }
}

enum class AnimState {
    Start,
    Running,
    Repeat,
    End
}