package industries.goodteam.gambit

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

const val COORDS_PER_VERTEX = 3
val squareCoords = floatArrayOf(
    -0.5f, -0.5f, 0.0f,
    0.5f, -0.5f, 0.0f,
    -0.5f, 0.5f, 0.0f,
    0.5f, 0.5f, 0.0f
)

class Card {

    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)

    private val vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(squareCoords.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(squareCoords)
                position(0)
            }
        }

    private val drawListBuffer: ShortBuffer =
        ByteBuffer.allocateDirect(drawOrder.size * 2).run {
            order(ByteOrder.nativeOrder())
            asShortBuffer().apply {
                put(drawOrder)
                position(0)
            }
        }

    private val vertexShaderCode =
        """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            void main() {
                gl_Position = uMVPMatrix * vPosition;
            }
        """.trimIndent()

    private var vPMatrixHandle: Int = 0

    private val fragmentShaderCode =
        """
            precision mediump float;
            uniform vec4 vColor;
            void main() {
                gl_FragColor = vColor;
            }
        """.trimIndent()

    fun loadShader(type: Int, shaderCode: String): Int {
        return GLES30.glCreateShader(type).also { shader ->
            GLES30.glShaderSource(shader, shaderCode)
            GLES30.glCompileShader(shader)
        }
    }

    private var program: Int

    init {
        val vertexShader: Int = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES30.glCreateProgram().also {
            GLES30.glAttachShader(it, vertexShader)
            GLES30.glAttachShader(it, fragmentShader)
            GLES30.glLinkProgram(it)
        }
    }

    val color = floatArrayOf(0.5f, 0.75f, 0.25f, 1.0f)

    private var positionHandle: Int = 0
    private var colorHandle: Int = 0

    private val vertexCount: Int = squareCoords.size / COORDS_PER_VERTEX
    private val vertexStride: Int = COORDS_PER_VERTEX * 4

    fun draw(mvpMatrix: FloatArray) {

        GLES30.glUseProgram(program)
        positionHandle = GLES30.glGetAttribLocation(program, "vPosition").also {
            GLES30.glEnableVertexAttribArray(it)
            GLES30.glVertexAttribPointer(
                it,
                COORDS_PER_VERTEX,
                GLES30.GL_FLOAT,
                false,
                vertexStride,
                vertexBuffer
            )
            colorHandle = GLES30.glGetUniformLocation(program, "vColor").also { colorHandle ->
                GLES30.glUniform4fv(colorHandle, 1, color, 0)
            }

            vPMatrixHandle = GLES30.glGetUniformLocation(program, "uMVPMatrix")
            GLES30.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, vertexCount)
            GLES30.glDisableVertexAttribArray(it)
        }
    }
}