package com.example.threedjetpack

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Choreographer
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.example.threedjetpack.ui.theme.ThreeDJetpackTheme
import com.google.android.filament.View
import com.google.android.filament.android.UiHelper
import com.google.android.filament.utils.KTX1Loader
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import java.lang.Float.max
import java.nio.ByteBuffer


class MainActivity : ComponentActivity() {
    private var localModel = "Blaze.glb"
    companion object {
        init { Utils.init() }
    }

    private lateinit var modelViewer: ModelViewer

    private val choreographer = Choreographer.getInstance()
    private val frameScheduler = FrameCallback()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val brush = Brush.radialGradient(
                            0f to Color.hsl(192f, 0.04f, 0.24f),
                            1f to Color.hsl(192f, 0.04f, 0.14f),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = max(size.width, size.height) / 2f
                        )
                        onDrawWithContent {
                            drawRect(size = size, brush = brush)
                        }
                    }
            ) {
                Filament()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Composable
    fun Filament() {
        AndroidView(factory = { context ->
            SurfaceView(context)
        }) { surfaceView ->
            modelViewer = ModelViewer(
                surfaceView,
                uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
                    isOpaque = false
                }
            )

            surfaceView.setOnTouchListener { _, event ->
                modelViewer.onTouchEvent(event)
                true
            }

            val view = modelViewer.view
            view.blendMode = View.BlendMode.TRANSLUCENT
            view.antiAliasing = View.AntiAliasing.FXAA
            view.multiSampleAntiAliasingOptions = view.multiSampleAntiAliasingOptions.apply {
                enabled = true

            }
            view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply {
                enabled = true
            }

            modelViewer.renderer.clearOptions = modelViewer.renderer.clearOptions.apply {
                clear = true
            }

            loadIndirectLight()
            loadSceneFromGltf()
        }
    }

    private fun loadIndirectLight() {
        val engine = modelViewer.engine
        val scene = modelViewer.scene
        readCompressedAsset(localModel).let {
            scene.indirectLight = KTX1Loader.createIndirectLight(engine, it)
            scene.indirectLight!!.intensity = 30_000.0f
        }
    }

    private fun loadSceneFromGltf() {
        readCompressedAsset(localModel).let {
            modelViewer.loadModelGlb(it)
            modelViewer.transformToUnitCube()
        }
    }

    private fun readCompressedAsset(assetName: String): ByteBuffer {
        val input = assets.open(assetName)
        val bytes = ByteArray(input.available())
        input.read(bytes)
        return ByteBuffer.wrap(bytes)
    }

    override fun onResume() {
        super.onResume()
        choreographer.postFrameCallback(frameScheduler)
    }

    override fun onPause() {
        super.onPause()
        choreographer.removeFrameCallback(frameScheduler)
    }

    override fun onDestroy() {
        super.onDestroy()
        choreographer.removeFrameCallback(frameScheduler)
    }

    inner class FrameCallback : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            choreographer.postFrameCallback(this)
            if (this@MainActivity::modelViewer.isInitialized) {
                modelViewer.render(frameTimeNanos)
            }
        }
    }
}
