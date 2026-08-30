package com.risealarm.engine.alarm

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt
import kotlin.random.Random

class StepFallbackTracker(
    context: Context,
    private val onProgress: (Int) -> Unit,
) : SensorEventListener, AutoCloseable {
    private val manager = context.getSystemService(SensorManager::class.java)
    private val detector = manager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val counter = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private var baseline: Float? = null
    private var detected = 0

    val available: Boolean get() = detector != null || counter != null

    fun start() {
        val sensor = detector ?: counter ?: return
        manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val value = if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            detected += 1
            detected
        } else {
            val current = event.values.firstOrNull() ?: return
            val base = baseline ?: current.also { baseline = it }
            (current - base).toInt().coerceAtLeast(0)
        }
        onProgress(value)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun close() { manager.unregisterListener(this) }
}

class ShakeFallbackTracker(
    context: Context,
    private val onProgress: (Int) -> Unit,
) : SensorEventListener, AutoCloseable {
    private val manager = context.getSystemService(SensorManager::class.java)
    private val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gravity = FloatArray(3)
    private var count = 0
    private var lastImpulseNanos = 0L
    val available: Boolean get() = accelerometer != null

    fun start() { accelerometer?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) } }

    override fun onSensorChanged(event: SensorEvent) {
        for (i in 0..2) gravity[i] = 0.8f * gravity[i] + 0.2f * event.values[i]
        val x = event.values[0] - gravity[0]
        val y = event.values[1] - gravity[1]
        val z = event.values[2] - gravity[2]
        val magnitude = sqrt(x * x + y * y + z * z)
        if (magnitude >= 12f && event.timestamp - lastImpulseNanos >= 350_000_000L) {
            lastImpulseNanos = event.timestamp
            onProgress(++count)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun close() { manager.unregisterListener(this) }
}

data class MathQuestion(val prompt: String, val answers: List<Int>, val correct: Int)

class MathChallengeGenerator(private val random: Random = Random.Default) {
    fun next(): MathQuestion {
        val subtract = random.nextBoolean()
        val a = random.nextInt(20, 100)
        val b = random.nextInt(10, if (subtract) a + 1 else 90)
        val correct = if (subtract) a - b else a + b
        val distractors = buildSet {
            while (size < 2) {
                val candidate = correct + random.nextInt(-12, 13)
                if (candidate >= 0 && candidate != correct) add(candidate)
            }
        }
        return MathQuestion("$a ${if (subtract) "−" else "+"} $b", (distractors + correct).shuffled(random), correct)
    }
}
