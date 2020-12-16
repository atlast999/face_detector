package com.example.facedetector.timehelper

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

abstract class ItervalTimeOutTimer(
    count: Long,
    private val interval: Long,
    timeUnit: TimeUnit
) : Timer(count, timeUnit) {
    private val counterTick: AtomicLong = AtomicLong(count)

    override fun onCreateTimeCounter(
        timeOut: Long,
        timeUnit: TimeUnit?
    ): Disposable? {
        counterTick.set(timeOut)
        return Observable.interval(
            interval,
            timeUnit,
            Schedulers.computation()
        )
            .map { counterTick.getAndDecrement() }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { notifyTimeoutStarted() }
            .subscribe { notifyTimeTick() }
    }

    protected fun notifyTimeTick() {
        counterTick.get().let {
            if (it <= 0) {
                notifyTimeoutComplete()
            } else {
                onTimeTick(it)
            }
        }
    }

    protected abstract fun onTimeTick(remain: Long)

}