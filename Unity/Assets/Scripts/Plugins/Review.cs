using System;
using NiceJson;
using UnityEngine;
#if UNITY_IOS
using System.Runtime.InteropServices;
#endif

/// <summary>
/// Review options
/// </summary>
public class Review : MonoBehaviour, IPlugin {

    /// <summary>
    /// Can't open review popup
    /// </summary>
    const string REVIEW_FAIL = "REVIEW_FAIL";

    /// <summary>
    /// Current instance
    /// </summary>
    private static Review _instance = null;

    /// <summary>
    /// Cache data for hidden app state
    /// </summary>
    private JsonObject _data = null;

    /// <summary>
    /// Cache error for hidden app state
    /// </summary>
    private JsonObject _error = null;

#if UNITY_IOS

    /// <summary>
    /// Open review dialog
    /// </summary>
    [DllImport("__Internal")]
    private static extern void reviewOpen();

#endif

    /// <summary>
    /// Constructor
    /// </summary>
    private void Awake() {
        if ((object)_instance == null) {
            _instance = GetComponent<Review>();
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
#if UNITY_ANDROID
                case REVIEW_FAIL:
                    OpenMarket();
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
    /// Open market page
    /// </summary>
    public static void OpenMarket() {
        Application.OpenURL(DemoController.AppStoreUrl);
    }

    /// <summary>
    /// Open review dialog
    /// </summary>
    public static void Open() {
#if UNITY_EDITOR
#elif UNITY_ANDROID
        using (AndroidJavaClass plugin = new AndroidJavaClass(string.Format(Plugins.ANDROID_CLASS_MASK, _instance.Name))) {
            plugin.CallStatic("openReview");
        }
#elif UNITY_IOS
        reviewOpen();
#endif
    }

}