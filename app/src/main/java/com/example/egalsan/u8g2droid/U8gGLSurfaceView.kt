
package com.example.egalsan.u8g2droid

import android.content.Context
import android.opengl.GLSurfaceView


class U8gGLSurfaceView(context: Context, dataFeeder: U8gDataFeeder) : GLSurfaceView(context) {
    init {
        // Create an OpenGL ES 1.0 context
        setEGLContextClientVersion(1)

        val renderer = U8gGLRenderer(context, dataFeeder)

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(renderer)

        // Render the view only when there is a change in the drawing data
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}
