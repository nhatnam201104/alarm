package com.risealarm.engine.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.util.concurrent.atomic.AtomicBoolean

class PoseLandmarkerRunner(
    context: Context,
    private val onObservation: (PoseObservation) -> Unit,
    private val onError: (Throwable) -> Unit,
) : AutoCloseable {
    private val busy = AtomicBoolean(false)
    private val landmarker: PoseLandmarker

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_ASSET)
            .build()
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumPoses(1)
            .setMinPoseDetectionConfidence(0.55f)
            .setMinPosePresenceConfidence(0.55f)
            .setMinTrackingConfidence(0.55f)
            .setOutputSegmentationMasks(false)
            .setResultListener(::handleResult)
            .setErrorListener { error -> busy.set(false); onError(error) }
            .build()
        landmarker = PoseLandmarker.createFromOptions(context.applicationContext, options)
    }

    fun analyze(imageProxy: ImageProxy, frontCamera: Boolean = true) {
        if (!busy.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }
        try {
            val bitmap = Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
            imageProxy.use { bitmap.copyPixelsFromBuffer(it.planes[0].buffer) }
            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                if (frontCamera) postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
            }
            val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            landmarker.detectAsync(BitmapImageBuilder(oriented).build(), SystemClock.uptimeMillis())
        } catch (error: Throwable) {
            busy.set(false)
            imageProxy.close()
            onError(error)
        }
    }

    private fun handleResult(result: PoseLandmarkerResult, @Suppress("UNUSED_PARAMETER") input: com.google.mediapipe.framework.image.MPImage) {
        busy.set(false)
        val pose = result.landmarks().firstOrNull() ?: run {
            onObservation(PoseObservation(result.timestampMs(), emptyMap()))
            return
        }
        val points = BodyLandmark.entries.mapNotNull { landmark ->
            pose.getOrNull(landmark.index)?.let { point ->
                landmark to PosePoint(
                    x = point.x(),
                    y = point.y(),
                    z = point.z(),
                    visibility = point.visibility().orElse(0f),
                )
            }
        }.toMap()
        onObservation(
            PoseObservation(
                timestampMillis = result.timestampMs(),
                landmarks = points,
                inferenceMillis = (SystemClock.uptimeMillis() - result.timestampMs()).coerceAtLeast(0),
            ),
        )
    }

    override fun close() { landmarker.close() }

    private companion object { const val MODEL_ASSET = "pose_landmarker_lite.task" }
}
