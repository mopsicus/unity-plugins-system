package com.mopsicus.cloud;

import com.unity3d.player.UnityPlayer;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mopsicus.ru
 */

public class Plugin {

    //region plugin functions

    static String object;
    static String receiver;
    static String name = getName ();

    static String getName () {
        String[] parts = new Plugin().getClass().getPackage().getName().split("\\.");
        return parts[2];
    }

    // Send data in JSON format to Unity
    public static void sendData(String data) {
        JSONObject info = new JSONObject();
        try {
            info.put("name", name);
            info.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        UnityPlayer.UnitySendMessage(object, receiver, info.toString());
    }

    // Send error in JSON format to Unity
    public static void sendError(int code, String data) {
        JSONObject error = new JSONObject();
        JSONObject info = new JSONObject();
        try {
            error.put("code", code);
            error.put("message", data);
            info.put("name", name);
            info.put("error", error);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        UnityPlayer.UnitySendMessage(object, receiver, info.toString());
    }

    // Plugin initialize
    public static void init(String data) {
        try {
            JSONObject info = new JSONObject(data);
            object = info.getString("object");
            receiver = info.getString("receiver");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //endregion

    //region user functions

    public static void test() {
        sendData("cloud operation success");
        sendError(1, "cloud operation error");
    }

//    public static void another() {
//
//    }

    //endregion

}
