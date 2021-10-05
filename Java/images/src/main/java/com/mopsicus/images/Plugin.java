package com.mopsicus.images;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import com.unity3d.player.UnityPlayer;
import com.mopsicus.common.Common;

public class Plugin {

    /**
     * Current plugin name
     */
    public static String name = "images";

    /**
     * Action for Media manager
     */
    public enum MediaAction {
        PICK_IMAGE,
        CAPTURE_PHOTO,
        SAVE_IMAGE,
        RESULT,
        PERMISSION
    }

    /**
     * Common for send data to Unity
     */
    public static Common common = new Common();

    /**
     * Param name
     */
    public static String ACTION = "ACTION";

    /**
     * Param data
     */
    public static String DATA = "DATA";

    /**
     * Param app name for file provider
     */
    public static String APP = "APP";

    /**
     * Cache for app id
     */
    private static String appId;

    /**
     * Open system settings
     */
    public static void settings() {
        Activity context = UnityPlayer.currentActivity;
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Save app id
     */
    public static void init(String app) {
        appId = app;
    }

    /**
     * Get image from gallery
     */
    public static void pick() {
        RunAction(MediaAction.PICK_IMAGE, null);
    }

    /**
     * Get image from camera
     */
    public static void capture() {
        RunAction(MediaAction.CAPTURE_PHOTO, null);
    }

    /**
     * Save image to system gallery
     *
     * @param path Path to file
     */
    public static void save(String path) {
        RunAction(MediaAction.SAVE_IMAGE, path);
    }

    /**
     * Stop service
     */
    public static void close() {
        Intent intent = new Intent(UnityPlayer.currentActivity, MediaService.class);
        UnityPlayer.currentActivity.stopService(intent);
    }

    /**
     * Run action with Media manager
     * @param action Action to use in service
     * @param data Optional data
     */
    public static void RunAction(MediaAction action, String data) {
        Intent intent = new Intent(UnityPlayer.currentActivity, MediaService.class);
        Bundle bundle = new Bundle();
        bundle.putString(APP, appId);
        bundle.putString(DATA, data);
        bundle.putInt(ACTION, action.ordinal());
        intent.putExtras(bundle);
        UnityPlayer.currentActivity.startService(intent);
    }

}
