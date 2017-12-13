package com.example.egalsan.u8g2droid

import android.content.Context
import javax.microedition.khronos.opengles.GL10
import android.opengl.GLSurfaceView
import com.android.texample.GLText


class U8gGLRenderer(context: Context, dataFeeder: U8gDataFeeder) : GLSurfaceView.Renderer {
    private var mGlText: GLText? = null
    private val mContext: Context = context
    private val mDataFeeder: U8gDataFeeder = dataFeeder

    private var mSurfaceWidth: Int = -1
    private var mSurfaceHeight: Int = -1


    override fun onSurfaceCreated(gl: GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
        // Set the background frame color
        gl!!.glClearColor(0.5f, 0.5f, 0.5f, 1.0f)

        // Create the GLText
        mGlText = GLText(gl, mContext.assets)

        // Load the font from file (set size + padding), creates the texture
        // NOTE: after a successful call to this the font is ready for rendering!
        mGlText!!.load("Roboto-Regular.ttf", 36, 2, 2)

    }

    override fun onDrawFrame(gl: GL10) {
        // Redraw background color
        gl.glClear( GL10.GL_COLOR_BUFFER_BIT )

        // Set to ModelView mode
        gl.glMatrixMode( GL10.GL_MODELVIEW )
        gl.glLoadIdentity()

        // Enable texture + alpha blending
        // NOTE: this is required for text rendering! we could incorporate it into
        // the GLText class, but then it would be called multiple times (which impacts performance).
        gl.glEnable( GL10.GL_TEXTURE_2D )
        gl.glEnable( GL10.GL_BLEND )
        gl.glBlendFunc( GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA )

        mGlText!!.begin( 0.0f, 0.0f, 1.0f, 1.0f )
        mGlText!!.drawC("Hola Juanro :)", mSurfaceWidth / 2.0f, mSurfaceHeight / 2.0f)
        mGlText!!.end()

        mGlText!!.begin( 1.0f, 1.0f, 1.0f, 1.0f )
        mGlText!!.draw("Read data from BT: ${mDataFeeder.feedData()}", 0.0f, 0.0f)
        mGlText!!.end()

        // Disable texture + alpha
        gl.glDisable( GL10.GL_BLEND )
        gl.glDisable( GL10.GL_TEXTURE_2D )
    }


    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        mSurfaceWidth = width
        mSurfaceHeight = height

        gl.glViewport(0, 0, width, height)

        // Setup orthographic projection
        gl.glMatrixMode(GL10.GL_PROJECTION)     // Activate Projection Matrix
        gl.glLoadIdentity()                     // Load Identity Matrix
        gl.glOrthof(                            // Set Ortho Projection (Left,Right,Bottom,Top,Front,Back)
                0.0f, width.toFloat(),
                0.0f, height.toFloat(),
                1.0f, -1.0f
        )
    }
}
