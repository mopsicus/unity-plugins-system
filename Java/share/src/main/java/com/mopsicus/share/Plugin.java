package com.mopsicus.share;

import android.app.Activity;
import android.content.Intent;

import com.unity3d.player.UnityPlayer;

public class Plugin {

    /**
     * Send data to share dialog
     *
     * @param data Text to share
     */
    public static void share(String data) {
        Activity context = UnityPlayer.currentActivity;
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, data);
        intent.setType("text/plain");
        context.startActivity(Intent.createChooser(intent, null));
    }

}
