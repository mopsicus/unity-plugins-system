package com.mopsicus.common;


import com.unity3d.player.UnityPlayer;

import org.json.JSONException;
import org.json.JSONObject;

public class Common {

    /**
     * GameObject on scene in Unity with handler
     */
    String object = "Plugins";

    /**
     * Function name which will be process messages from plugin
     */
    String receiver = "OnDataReceive";

    /**
     * Send data in JSON format to Unity
     *
     * @param plugin Plugin name
     * @param data   Data in to send in Unity
     */
    public void sendData(String plugin, String data) {
        JSONObject info = new JSONObject();
        try {
            info.put("name", plugin);
            info.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        UnityPlayer.UnitySendMessage(object, receiver, info.toString());
    }

    /**
     * Send error code without data
     *
     * @param plugin Plugin name
     * @param code   Error code
     */
    public void sendError(String plugin, String code) {
        sendError(plugin, code, "");
    }

    /**
     * Send error in JSON format to Unity
     *
     * @param plugin Plugin name
     * @param code   Error code
     * @param data   Error data
     */
    public void sendError(String plugin, String code, String data) {
        JSONObject error = new JSONObject();
        JSONObject info = new JSONObject();
        try {
            error.put("code", code);
            error.put("message", data);
            info.put("name", plugin);
            info.put("error", error);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        UnityPlayer.UnitySendMessage(object, receiver, info.toString());
    }

}
