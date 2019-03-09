package industries.goodteam.gambit

import android.app.Activity
import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Bundle

class MainActivity : Activity() {

    private lateinit var gLView: GLSurfaceView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gLView = MyGLSurfaceView(this)
        setContentView(gLView)
    }

    class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {
        private val renderer: MyGLRenderer

        init {
            setEGLContextClientVersion(3)
            renderer = MyGLRenderer()
            setRenderer(renderer)
        }
    }
}
