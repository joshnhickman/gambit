package industries.goodteam.gambit.opengl

import android.content.Context
import android.content.pm.ActivityInfo
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent

private const val TOUCH_SCALE_FACTOR = 180.0f / 320f

class MainActivity : AppCompatActivity() {

    private lateinit var gLView: GLSurfaceView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

        gLView = MyGLSurfaceView(this)
        setContentView(gLView)
    }

    class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {

        private val renderer: MyGLRenderer

        private var previousX: Float = 0f
        private var previousY: Float = 0f

        init {
            setEGLContextClientVersion(3)
            renderer = MyGLRenderer()
            setRenderer(renderer)

            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            val x: Float = e.x
            val y: Float = e.y

            when (e.action) {
                MotionEvent.ACTION_MOVE -> {
                    var dx: Float = x - previousX
                    var dy: Float = y - previousY

                    if (y > height / 2) {
                        dx *= -1
                    }

                    if (x < width / 2) {
                        dy *= -1
                    }

                    renderer.angle += (dx + dy) * TOUCH_SCALE_FACTOR
                    requestRender()
                }
            }

            previousX = x
            previousY = y
            return true
        }
    }
}
