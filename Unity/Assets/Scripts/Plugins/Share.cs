using System;
using NiceJson;
using UnityEngine;
#if UNITY_IOS
using System.Runtime.InteropServices;
#endif

/// <summary>
/// Share app to socials, etc
/// </summary>
public class Share : MonoBehaviour, IPlugin {

    /// <summary>
    /// Flag for error code for no mail account on device
    /// </summary>
    const string NO_ACCOUNT = "NO_ACCOUNT";

    /// <summary>
    /// Current instance
    /// </summary>
    private static Share _instance;

    /// <summary>
    /// Cache data for hidden app state
    /// </summary>
    private JsonObject _data;

    /// <summary>
    /// Cache error for hidden app state
    /// </summary>
    private JsonObject _error;

#if UNITY_IOS
    /// <summary>
    /// Open standart share dialog
    /// </summary>
    /// <param name="data">Text to share</param>
    [DllImport("__Internal")]
    private static extern void shareText(string data);
#endif

    /// <summary>
    /// Constructor
    /// </summary>
    private void Awake() {
        if ((object)_instance == null) {
            _instance = GetComponent<Share>();
        }
    }

    /// <summary>
    /// Plugin name
    /// </summary>
    public string Name {
        get {
            return GetType().Name.ToLower();
        }
    }

    /// <summary>
    /// Callback on data
    /// </summary>
    public void OnData(JsonObject data) {
#if DEBUG
        Debug.Log(string.Format("{0} plugin OnData: {1}", GetType().Name, data.ToJsonPrettyPrintString()));
#endif
        _data = data;
        try {
            _data = null;
        } catch (Exception e) {
#if DEBUG
            Debug.LogError(string.Format("{0} plugin OnData error: {1}", GetType().Name, e.Message));
#endif
        }
    }

    /// <summary>
    /// Callback on error
    /// </summary>
    public void OnError(JsonObject data) {
#if DEBUG
        Debug.LogError(string.Format("{0} plugin OnError: {1}", GetType().Name, data.ToJsonPrettyPrintString()));
#endif
        _error = (JsonObject)data["error"];
        try {
            string code = _error["code"];
            switch (code) {
#if UNITY_IOS
                case NO_ACCOUNT:
                    Debug.LogError("Show no mail account error");
                    break;
#endif
                default:
                    break;
            }
            _error = null;
        } catch (Exception e) {
#if DEBUG
            Debug.LogError(string.Format("{0} plugin OnError error: {1}", GetType().Name, e.Message));
#endif
        }
    }

    /// <summary>
    /// Send text to share dialog
    /// </summary>
    /// <param name="data">Text to share</param>
    public static void Text(string data) {
#if UNITY_EDITOR
#elif UNITY_ANDROID
        using (AndroidJavaClass plugin = new AndroidJavaClass(string.Format(Plugins.ANDROID_CLASS_MASK, _instance.Name))) {
            plugin.CallStatic("share", data);
        }
#elif UNITY_IOS
        shareText(data);
#endif
    }

}