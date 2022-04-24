/*
 * Copyright 2016 Carmen Alvarez
 *
 * This file is part of Scrum Chatter.
 *
 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Scrum Chatter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Scrum Chatter. If not, see <http://www.gnu.org/licenses/>.
 */
package ca.rmen.android.scrumchatter.settings;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import ca.rmen.android.scrumchatter.Constants;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class Theme {
    private static final String TAG = Constants.TAG + Theme.class.getSimpleName();

    /**
     * If the app isn't using the theme in the shared preferences, this
     * will restart the activity and set the global flag to use the right theme.
     * Logically, this might make more sense in an application class.
     */
    public static void checkTheme(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return;
        }

        Single.fromCallable(() -> Prefs.getInstance(activity).getTheme())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(theme -> {
                    if (theme == Prefs.Theme.Dark) {
                        if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_YES) {
                            Log.v(TAG, "Restarting in dark mode");
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                            activity.recreate();
                        }
                    } else if (theme == Prefs.Theme.Light){
                        if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_NO) {
                            Log.v(TAG, "Restarting in light mode");
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                            activity.recreate();
                        }
                    } else if (theme == Prefs.Theme.Auto) {
                        if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY) {
                            Log.v(TAG, "Restarting in auto mode");
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                            activity.recreate();
                        }
                    }
                });
    }
}
