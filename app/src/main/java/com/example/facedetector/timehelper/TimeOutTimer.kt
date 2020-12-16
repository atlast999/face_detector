package com.example.facedetector.timehelper

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit

abstract class TimeOutTimer(
    timeOut: Long,
    timeUnit: TimeUnit
) : Timer(timeOut, timeUnit) {
    override fun onCreateTimeCounter(
        timeOut: Long,
        timeUnit: TimeUnit?
    ): Disposable? {
        val completable =
            Completable.timer(timeOut, timeUnit, AndroidSchedulers.mainThread())
        return completable.doOnSubscribe { notifyTimeoutStarted() }
            .subscribe { notifyTimeoutComplete() }
    }
}