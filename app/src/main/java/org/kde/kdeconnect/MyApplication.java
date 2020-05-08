package org.kde.kdeconnect;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

public class MyApplication extends Application {
    private static class LifecycleObserver implements DefaultLifecycleObserver {
        private boolean inForeground = false;

        @Override
        public void onCreate(@NonNull LifecycleOwner owner) { }

        @Override
        public void onStart(@NonNull LifecycleOwner owner) {
            inForeground = true;
        }

        @Override
        public void onStop(@NonNull LifecycleOwner owner) {
            inForeground = false;
        }

        @Override
        public void onDestroy(@NonNull LifecycleOwner owner) { }

        @Override
        public void onPause(@NonNull LifecycleOwner owner) { }

        @Override
        public void onResume(@NonNull LifecycleOwner owner) { }

        boolean isInForeground() {
            return inForeground;
        }
    }

    private static final LifecycleObserver foregroundTracker = new LifecycleObserver();

    @Override
    public void onCreate() {
        super.onCreate();

        ProcessLifecycleOwner.get().getLifecycle().addObserver(foregroundTracker);
    }

    public static boolean isInForeground() {
        return foregroundTracker.isInForeground();
    }
}
