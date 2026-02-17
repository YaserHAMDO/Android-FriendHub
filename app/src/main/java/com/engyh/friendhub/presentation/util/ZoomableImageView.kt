package com.engyh.friendhub.presentation.util

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.appcompat.widget.AppCompatImageView

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr),
    View.OnTouchListener,
    GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener {

    private val mScaleDetector: ScaleGestureDetector
    private val mGestureDetector: GestureDetector
    private val mMatrix: Matrix = Matrix()
    private val mMatrixValues: FloatArray = FloatArray(9)

    private var mode = NONE

    private var mSaveScale = 1f
    private var mMinScale = 1f
    private var mMaxScale = 4f

    private var origWidth = 0f
    private var origHeight = 0f
    private var viewWidth = 0
    private var viewHeight = 0

    private val mLast = PointF()
    private val mStart = PointF()

    init {
        super.setClickable(true)
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        setImageMatrix(mMatrix)
        scaleType = ScaleType.MATRIX
        mGestureDetector = GestureDetector(context, this)
        mGestureDetector.setOnDoubleTapListener(this)
        setOnTouchListener(this)
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mode = ZOOM
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var scaleFactor = detector.scaleFactor
            val prevScale = mSaveScale
            mSaveScale *= scaleFactor

            if (mSaveScale > mMaxScale) {
                mSaveScale = mMaxScale
                scaleFactor = mMaxScale / prevScale
            } else if (mSaveScale < mMinScale) {
                mSaveScale = mMinScale
                scaleFactor = mMinScale / prevScale
            }

            if (origWidth * mSaveScale <= viewWidth || origHeight * mSaveScale <= viewHeight) {
                mMatrix.postScale(
                    scaleFactor,
                    scaleFactor,
                    viewWidth / 2f,
                    viewHeight / 2f
                )
            } else {
                mMatrix.postScale(
                    scaleFactor,
                    scaleFactor,
                    detector.focusX,
                    detector.focusY
                )
            }

            fixTranslation()
            return true
        }
    }

    private fun fitToScreen() {
        mSaveScale = 1f
        val drawable: Drawable = drawable ?: return
        if (drawable.intrinsicWidth == 0 || drawable.intrinsicHeight == 0) return

        val imageWidth = drawable.intrinsicWidth
        val imageHeight = drawable.intrinsicHeight

        val scaleX = viewWidth.toFloat() / imageWidth.toFloat()
        val scaleY = viewHeight.toFloat() / imageHeight.toFloat()
        val scale = minOf(scaleX, scaleY)

        mMatrix.setScale(scale, scale)

        val redundantYSpace = (viewHeight - scale * imageHeight) / 2f
        val redundantXSpace = (viewWidth - scale * imageWidth) / 2f

        mMatrix.postTranslate(redundantXSpace, redundantYSpace)
        origWidth = viewWidth - 2 * redundantXSpace
        origHeight = viewHeight - 2 * redundantYSpace

        imageMatrix = mMatrix
    }

    private fun fixTranslation() {
        mMatrix.getValues(mMatrixValues)
        val transX = mMatrixValues[Matrix.MTRANS_X]
        val transY = mMatrixValues[Matrix.MTRANS_Y]

        val fixTransX = getFixTranslation(transX, viewWidth.toFloat(), origWidth * mSaveScale)
        val fixTransY = getFixTranslation(transY, viewHeight.toFloat(), origHeight * mSaveScale)

        if (fixTransX != 0f || fixTransY != 0f) {
            mMatrix.postTranslate(fixTransX, fixTransY)
        }
    }

    private fun getFixTranslation(trans: Float, viewSize: Float, contentSize: Float): Float {
        val (minTrans, maxTrans) = if (contentSize <= viewSize) {
            0f to (viewSize - contentSize)
        } else {
            (viewSize - contentSize) to 0f
        }

        return when {
            trans < minTrans -> -trans + minTrans
            trans > maxTrans -> -trans + maxTrans
            else -> 0f
        }
    }

    private fun getFixDragTrans(delta: Float, viewSize: Float, contentSize: Float): Float {
        return if (contentSize <= viewSize) 0f else delta
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        viewHeight = MeasureSpec.getSize(heightMeasureSpec)

        if (mSaveScale == 1f) {
            fitToScreen()
        }
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        mScaleDetector.onTouchEvent(event)
        mGestureDetector.onTouchEvent(event)

        val curr = PointF(event.x, event.y)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mLast.set(curr)
                mStart.set(mLast)
                mode = DRAG
            }

            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    val deltaX = curr.x - mLast.x
                    val deltaY = curr.y - mLast.y

                    val fixTransX = getFixDragTrans(deltaX, viewWidth.toFloat(), origWidth * mSaveScale)
                    val fixTransY = getFixDragTrans(deltaY, viewHeight.toFloat(), origHeight * mSaveScale)

                    mMatrix.postTranslate(fixTransX, fixTransY)
                    fixTranslation()
                    mLast.set(curr.x, curr.y)
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
            }
        }

        imageMatrix = mMatrix
        return false
    }

    override fun onDown(e: MotionEvent): Boolean = false
    override fun onShowPress(e: MotionEvent) = Unit
    override fun onSingleTapUp(e: MotionEvent): Boolean = false
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean = false
    override fun onLongPress(e: MotionEvent) = Unit
    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean = false

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean = false

    override fun onDoubleTap(e: MotionEvent): Boolean {
        fitToScreen()
        return false
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean = false

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }
}
