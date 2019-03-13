package industries.goodteam.gambit.opengl

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyGLRenderer : GLSurfaceView.Renderer {

    // projection
    private val vPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    // rotate
    private val rotationMatrix = FloatArray(16)

    @Volatile
    var angle: Float = 0f

    private lateinit var card: Card

    override fun onDrawFrame(unused: GL10?) {
        val scratch = FloatArray(16)

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        // projection
        Matrix.setLookAtM(viewMatrix,
            0, 0f, 0f, -3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // rotate
//        val time = SystemClock.uptimeMillis() % 4000L
//        val angle = 0090f * time.toInt()
        Matrix.setRotateM(rotationMatrix, 0, angle, 0f, 0f, -1.0f)

        // combine projection and rotation
        Matrix.multiplyMM(scratch, 0, vPMatrix, 0, rotationMatrix, 0)

        card.draw(scratch)
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)

        val ratio: Float = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
    }

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        card = Card()
    }
}