package com.mopsicus.base;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.mopsicus.common.Common;
import com.mopsicus.images.Plugin.MediaAction;
import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.mopsicus.images.Plugin.RunAction;


public class Plugin extends UnityPlayerActivity {

    /**
     * Common for send data to Unity
     */
    public static Common common = new Common();

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * Callback on activity result
     *
     * @param requestCode Code for process
     * @param resultCode  Result of request
     * @param result      Data to process
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        super.onActivityResult(requestCode, resultCode, result);
        JSONObject data = new JSONObject();
        try {
            data.put("requestCode", requestCode);
            data.put("resultCode", resultCode);
            String uri = "";
            if (result != null) {
                Uri raw = result.getData();
                uri = (raw != null) ? raw.toString() : "";
            }
            data.put("result", uri);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RunAction(MediaAction.RESULT, data.toString());
    }

    /**
     * Callback on permission request
     *
     * @param requestCode  Request code
     * @param permissions  Permissions list
     * @param grantResults Granted results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        JSONObject data = new JSONObject();
        JSONArray permsList = new JSONArray();
        JSONArray grantsList = new JSONArray();
        try {
            for (String item : permissions) {
                permsList.put(item);
            }
            for (int item : grantResults) {
                grantsList.put(item);
            }
            data.put("requestCode", requestCode);
            data.put("permissions", permsList);
            data.put("grantResults", grantsList);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RunAction(MediaAction.PERMISSION, data.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}
