package org.github.sawervatoryy.mirageClans.model;

import java.util.concurrent.atomic.AtomicLong;

/** Прогресс одного квеста клана в текущем периоде. */
public final class QuestProgress {

    private final AtomicLong value = new AtomicLong();
    private volatile boolean completed;
    private volatile String periodKey;

    public QuestProgress(String periodKey) {
        this.periodKey = periodKey;
    }

    public long value() {
        return value.get();
    }

    public long add(long delta) {
        return value.addAndGet(delta);
    }

    public void set(long v) {
        value.set(v);
    }

    public boolean completed() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String periodKey() {
        return periodKey;
    }

    public void setPeriodKey(String periodKey) {
        this.periodKey = periodKey;
    }
}
