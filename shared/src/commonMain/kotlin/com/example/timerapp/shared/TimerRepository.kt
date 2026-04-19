package com.example.timerapp.shared

class TimerRepository {
    private val engine = TimerEngine()

    fun start() = engine.start()

    fun pause() = engine.pause()

    fun reset() = engine.reset()

    fun addLap() = engine.addLap()

    fun tick(): TimerState = engine.tick()

    fun getState(): TimerState = engine.getCurrentState()
}