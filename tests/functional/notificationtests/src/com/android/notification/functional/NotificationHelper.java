/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.notification.functional;

import android.app.Instrumentation;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.RemoteException;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.Direction;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.Until;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.android.notification.functional.R;

import java.lang.InterruptedException;
import java.util.List;
import java.util.Map;

public class NotificationHelper {

    private static final String LOG_TAG = NotificationHelper.class.getSimpleName();
    private static final int LONG_TIMEOUT = 2000;
    private static final int SHORT_TIMEOUT = 200;
    private static final UiSelector LIST_VIEW = new UiSelector().className(ListView.class);
    private static final UiSelector LIST_ITEM_VALUE = new UiSelector().className(TextView.class);

    private UiDevice mDevice;
    private Instrumentation mInst;
    private NotificationManager mNotificationManager = null;
    private Context mContext = null;

    public NotificationHelper(UiDevice device, Instrumentation inst, NotificationManager nm) {
        this.mDevice = device;
        mInst = inst;
        mNotificationManager = nm;
        mContext = inst.getContext();
    }

    public void sleepAndWakeUpDevice() throws RemoteException, InterruptedException {
        mDevice.sleep();
        Thread.sleep(LONG_TIMEOUT);
        mDevice.wakeUp();
    }

    public static void launchSettingsPage(Context ctx, String pageName) throws Exception {
        Intent intent = new Intent(pageName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
        Thread.sleep(LONG_TIMEOUT * 2);
    }

    /**
     * Sets the screen lock pin
     * @param pin 4 digits
     * @return false if a pin is already set or pin value is not 4 digits
     * @throws UiObjectNotFoundException
     */
    public boolean setScreenLockPin(int pin) throws Exception {
        if (pin >= 0 && pin <= 9999) {
            navigateToScreenLock();
            if (new UiObject(new UiSelector().text("Confirm your PIN")).exists()) {
                UiObject pinField = new UiObject(
                        new UiSelector().className(EditText.class.getName()));
                pinField.setText(String.format("%04d", pin));
                mDevice.pressEnter();
            }
            new UiObject(new UiSelector().text("PIN")).click();
            clickText("No thanks");
            UiObject pinField = new UiObject(new UiSelector().className(EditText.class.getName()));
            pinField.setText(String.format("%04d", pin));
            mDevice.pressEnter();
            pinField.setText(String.format("%04d", pin));
            mDevice.pressEnter();
            clickText("Hide sensitive notification content");
            clickText("DONE");
            return true;
        }
        return false;
    }

    public boolean removeScreenLock(int pin, String mode) throws Exception {
        navigateToScreenLock();
        if (new UiObject(new UiSelector().text("Confirm your PIN")).exists()) {
            UiObject pinField = new UiObject(new UiSelector().className(EditText.class.getName()));
            pinField.setText(String.format("%04d", pin));
            mDevice.pressEnter();
            clickText(mode);
            clickText("YES, REMOVE");
        } else {
            clickText(mode);
        }
        return true;
    }

    public void unlockScreenByPin(int pin) throws Exception {
        String command = String.format(" %s %s %s", "input", "text", Integer.toString(pin));
        executeAdbCommand(command);
        Thread.sleep(SHORT_TIMEOUT);
        mDevice.pressEnter();
    }

    public void enableNotificationViaAdb(boolean isShow) {
        String command = String.format(" %s %s %s %s %s", "settings", "put", "secure",
                "lock_screen_show_notifications",
                isShow ? "1" : "0");
        executeAdbCommand(command);
    }

    private void executeAdbCommand(String command) {
        Log.i(LOG_TAG, String.format("executing - %s", command));
        mInst.getUiAutomation().executeShellCommand(command);
        mDevice.waitForIdle();
    }

    private void navigateToScreenLock() throws Exception {
        launchSettingsPage(mInst.getContext(), Settings.ACTION_SECURITY_SETTINGS);
        new UiObject(new UiSelector().text("Screen lock")).click();
    }

    private void clickText(String text) throws UiObjectNotFoundException {
        mDevice.wait(Until.findObject(By.text(text)), LONG_TIMEOUT).click();
    }

    public void sendNotification(int id, int visibility, String title) throws Exception {
        Log.v(LOG_TAG, "Sending out notification...");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
        CharSequence subtitle = String.valueOf(System.currentTimeMillis());
        Notification notification = new Notification.Builder(mContext)
                .setSmallIcon(R.drawable.stat_notify_email)
                .setWhen(System.currentTimeMillis()).setContentTitle(title).setContentText(subtitle)
                .setContentIntent(pendingIntent).setVisibility(visibility)
                .setPriority(Notification.PRIORITY_HIGH)
                .build();
        mNotificationManager.notify(id, notification);
        Thread.sleep(LONG_TIMEOUT);
    }

    public void sendNotifications(Map<Integer, String> lists) throws Exception {
        Log.v(LOG_TAG, "Sending out notification...");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
        CharSequence subtitle = String.valueOf(System.currentTimeMillis());
        for (Map.Entry<Integer, String> l : lists.entrySet()) {
            Notification notification = new Notification.Builder(mContext)
                    .setSmallIcon(R.drawable.stat_notify_email)
                    .setWhen(System.currentTimeMillis()).setContentTitle(l.getValue())
                    .setContentText(subtitle)
                    .build();
            mNotificationManager.notify(l.getKey(), notification);
        }
        Thread.sleep(LONG_TIMEOUT);
    }

    public void sendBundlingNotifications(List<Integer> lists, String groupKey) throws Exception {
        Notification childNotification = new Notification.Builder(mContext)
                .setContentTitle(lists.get(1).toString())
                .setSmallIcon(R.drawable.stat_notify_email)
                .setGroup(groupKey)
                .build();
        mNotificationManager.notify(lists.get(1),
                childNotification);
        childNotification = new Notification.Builder(mContext)
                .setContentText(lists.get(2).toString())
                .setSmallIcon(R.drawable.stat_notify_email)
                .setGroup(groupKey)
                .build();
        mNotificationManager.notify(lists.get(2),
                childNotification);
        Notification notification = new Notification.Builder(mContext)
                .setContentTitle(lists.get(0).toString())
                .setSubText(groupKey)
                .setSmallIcon(R.drawable.stat_notify_email)
                .setGroup(groupKey)
                .setGroupSummary(true)
                .build();
        mNotificationManager.notify(lists.get(0),
                notification);
    }

    static SpannableStringBuilder BOLD(CharSequence str) {
        final SpannableStringBuilder ssb = new SpannableStringBuilder(str);
        ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, ssb.length(), 0);
        return ssb;
    }

    public boolean checkNotificationExistence(int id, boolean exists) throws Exception {
        boolean isFound = false;
        for (int tries = 3; tries-- > 0;) {
            isFound = false;
            StatusBarNotification[] sbns = mNotificationManager.getActiveNotifications();
            for (StatusBarNotification sbn : sbns) {
                if (sbn.getId() == id) {
                    isFound = true;
                    break;
                }
            }
            if (isFound == exists) {
                break;
            }
            Thread.sleep(SHORT_TIMEOUT);
        }
        Log.i(LOG_TAG, "checkNotificationExistence..." + isFound);
        return isFound == exists;
    }

    public void swipeUp() throws Exception {
        mDevice.swipe(mDevice.getDisplayWidth() / 2, mDevice.getDisplayHeight(),
                mDevice.getDisplayWidth() / 2, 0, 30);
        Thread.sleep(SHORT_TIMEOUT);
    }

    public void swipeDown() throws Exception {
        mDevice.swipe(mDevice.getDisplayWidth() / 2, 0, mDevice.getDisplayWidth() / 2,
                mDevice.getDisplayHeight() / 2 + 50, 20);
        Thread.sleep(SHORT_TIMEOUT);
    }

    /**
     * This is the main list view containing the items that settings are possible for
     */
    public static class SettingsListView {
        public static boolean selectSettingsFor(String name) throws UiObjectNotFoundException {
            UiScrollable settingsList = new UiScrollable(
                    new UiSelector().resourceId("android:id/content"));
            UiObject appSettings = settingsList.getChildByText(LIST_ITEM_VALUE, name);
            if (appSettings != null) {
                return appSettings.click();
            }
            return false;
        }

        public boolean checkSettingsExists(String name) {
            try {
                UiScrollable settingsList = new UiScrollable(LIST_VIEW);
                UiObject appSettings = settingsList.getChildByText(LIST_ITEM_VALUE, name);
                return appSettings.exists();
            } catch (UiObjectNotFoundException e) {
                return false;
            }
        }
    }
}
