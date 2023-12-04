package com.example.labappmobili;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

public class ActivityMonitor extends Application {

    private static boolean isAnyActivityRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                isAnyActivityRunning = true;
            }

            @Override
            public void onActivityStarted(Activity activity) {
                // Not needed for this example
            }

            @Override
            public void onActivityResumed(Activity activity) {
                isAnyActivityRunning = true;
            }

            @Override
            public void onActivityPaused(Activity activity) {
                // Not needed for this example
            }

            @Override
            public void onActivityStopped(Activity activity) {
                // Not needed for this example
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                // Not needed for this example
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                isAnyActivityRunning = false;
            }
        });
    }

    public static boolean isAnyActivityRunning() {
        return isAnyActivityRunning;
    }
}
