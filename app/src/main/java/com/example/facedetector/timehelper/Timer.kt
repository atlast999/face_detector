package com.example.facedetector.timehelper

import androidx.annotation.IntDef
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit

abstract class Timer(private val timeOut: Long, private val timeUnit: TimeUnit) {
    @IntDef(
        State.READY,
        State.RUNNING,
        State.INTERUPTED,
        State.CANCELED,
        State.COMPLETED
    )
    @Retention(AnnotationRetention.SOURCE)
    internal annotation class State {
        companion object {
            const val READY = 0
            const val RUNNING = 1
            const val INTERUPTED = 2
            const val CANCELED = 3
            const val COMPLETED = 4
        }
    }

    @IntDef(
        NextAction.PENDING,
        NextAction.REPEAT,
        NextAction.DISPOSE
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class NextAction {
        companion object {
            const val PENDING = 0
            const val REPEAT = 1
            const val DISPOSE = 2
        }
    }

    private var state: Int
    private var counterDisposable: Disposable? = null
    private var startedMilis: Long = 0
    private var interuptedMilis: Long = 0
    fun start(): Disposable? {
        if (state == State.READY || state == State.CANCELED) {
            counterDisposable?.let {
                if (!it.isDisposed) {
                    it.dispose()
                }
            }
            counterDisposable = onCreateTimeCounter(timeOut, timeUnit)
        }
        return counterDisposable
    }

    fun resume(): Disposable? {
        if (state != State.INTERUPTED) {
            return counterDisposable
        }
        val timeOutInMilis = timeUnit.toMillis(timeOut)
        val now = System.currentTimeMillis()
        val timeDiff = now - startedMilis
        if (timeDiff >= timeOutInMilis) {
            notifyTimeoutComplete()
        } else {
            val remainingTimeMilis = timeOutInMilis - (interuptedMilis - startedMilis)
            val remainTimeOut =
                timeUnit.convert(remainingTimeMilis, TimeUnit.MILLISECONDS)
            counterDisposable = onCreateTimeCounter(remainTimeOut, timeUnit)
        }
        return counterDisposable
    }

    fun interupt() {
        if (state == State.RUNNING) {
            state = State.INTERUPTED
            interuptedMilis = System.currentTimeMillis()
            terminate()
        }
    }

    fun cancel() {
//        if (state == State.COMPLETED) {
//            return
//        }
        terminate()
        state = State.CANCELED
    }

    private fun terminate() {
        counterDisposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
    }

    fun notifyTimeoutStarted() {
        startedMilis = System.currentTimeMillis()
        state = State.RUNNING
        onTimeoutStarted()
    }

    fun notifyTimeoutComplete() {
        val nextAction = onTimeoutCompleted()
        when (nextAction) {
            NextAction.PENDING -> {
                state = State.READY
                terminate()
            }
            NextAction.REPEAT -> {
                state = State.READY
                terminate()
                start()
            }
            NextAction.DISPOSE -> {
                state = State.COMPLETED
                terminate()
            }
            else -> {
                state = State.COMPLETED
                terminate()
            }
        }
    }

    protected abstract fun onCreateTimeCounter(
        timeOut: Long,
        timeUnit: TimeUnit?
    ): Disposable?

    protected fun onTimeoutStarted() {}

    @NextAction
    protected abstract fun onTimeoutCompleted(): Int

    init {
        state = State.READY
    }
}