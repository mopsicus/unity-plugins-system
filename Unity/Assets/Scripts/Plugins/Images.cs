using System;
using NiceJson;
using UnityEngine;
#if UNITY_IOS
using System.Runtime.InteropServices;
#endif

/// <summary>
/// Mobile native image plugin
/// </summary>
public class Images : MonoBehaviour, IPlugin {

    /// <summary>
    /// State when user denied to show permission dialog
    /// </summary>
    const string NEVER_ASK_STATE = "NEVER_ASK_STATE";

    /// <summary>
    /// Wait state to show loader while image downloading
    /// </summary>
    const string WAIT_STATE = "WAIT_STATE";

    /// <summary>
    /// Cant save image to system gallery
    /// </summary>
    const string SAVE_ERROR = "SAVE_ERROR";

    /// <summary>
    /// Save image to system gallery success
    /// </summary>
    const string SAVE_SUCCESS = "SAVE_SUCCESS";

    /// <summary>
    /// Error occurs on capture photo or select in gallery
    /// </summary>
    const string IMAGE_ERROR = "IMAGE_ERROR";

#if UNITY_IOS
    /// <summary>
    /// User denied access
    /// </summary>
    const string NO_PERMISSION = "NO_PERMISSION";
#endif

    /// <summary>
    /// Current instance
    /// </summary>
    private static Images _instance = null;

    /// <summary>
    /// Cache data for hidden app state
    /// </summary>
    private JsonObject _data = null;

    /// <summary>
    /// Cache error for hidden app state
    /// </summary>
    private JsonObject _error = null;

    /// <summary>
    /// Callback cache
    /// </summary>
    private Action<string, int> _callback = null;

    /// <summary>
    /// Callback cache for images saving
    /// </summary>
    private Action<bool> _callbackSave = null;

#if UNITY_IOS
    /// <summary>
    /// Request image from gallery
    /// </summary>
    [DllImport("__Internal")]
    private static extern void imagesPick();

    /// <summary>
    /// Request image from camera
    /// </summary>
    [DllImport("__Internal")]
    private static extern void imagesCapture();

    /// <summary>
    /// Open app settings
    /// </summary>
    [DllImport("__Internal")]
    private static extern void imagesSettings();

    /// <summary>
    /// Save image to system gallery
    /// </summary>
    [DllImport("__Internal")]
    private static extern void imagesSave(string filePath);
#endif

    /// <summary>
    /// Constructor
    /// </summary>
    private void Awake() {
        if ((object)_instance == null) {
            _instance = GetComponent<Images>();
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
            string content = _data["data"];
            JsonObject response = (JsonObject)JsonNode.ParseJsonString(content);
            string path = response["path"];
            int degree = response["degree"];
            switch (path) {
                case WAIT_STATE:
                    Debug.Log("Show wait dialog...");
                    break;
                case SAVE_SUCCESS:
                    _callbackSave(true);
                    break;
                default:
                    Debug.Log("Hide wait dialog...");
                    _callback(path, degree);
                    break;
            }
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
                case NEVER_ASK_STATE:
                    AskOpenSettings();
                    break;
                case IMAGE_ERROR:
                    Debug.LogError("Show image load error");
                    break;
#elif UNITY_IOS
                case NO_PERMISSION:
                    AskOpenSettings();
                    break;
#endif
                case SAVE_ERROR:
                    _callbackSave(false);
                    break;
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
    /// Open system settigns if user denied to show
    /// </summary>
    private void AskOpenSettings() {
        // Make your own UI dialog and ask to open settings
        // In current example settings opens immediately
        // Service<Popup>.Get().Dialog(Localizer.Get(BaseLocalizations.Headers.PERMISSIONS), Localizer.Get(BaseLocalizations.Plugins.IMAGE_SETTINGS_MESSAGE), Localizer.Get(BaseLocalizations.Buttons.NO_YES), (button) => {
        //     if (button > 0) {
#if UNITY_EDITOR
#elif UNITY_ANDROID
                using (AndroidJavaClass plugin = new AndroidJavaClass(string.Format(Plugins.ANDROID_CLASS_MASK, _instance.Name))) {
                    plugin.CallStatic("settings");
                }
#elif UNITY_IOS
                imagesSettings();
#endif
            // }
        // });
    }

    /// <summary>
    /// Save image to system gallery
    /// </summary>
    /// <param name="filePath">Path to image to save</param>
    public static void Save(string filePath, Action<bool> callback) {
        _instance._callbackSave = callback;
#if UNITY_EDITOR
#elif UNITY_ANDROID
        using (AndroidJavaClass plugin = new AndroidJavaClass(string.Format(Plugins.ANDROID_CLASS_MASK, _instance.Name))) {
            plugin.CallStatic("save", filePath);
        }
#elif UNITY_IOS
        imagesSave(filePath);
#endif
    }

    /// <summary>
    /// Init app id for gallery
    /// </summary>
    /// <param name="app">App name to init provider</param>
    public static void Init(string app) {
#if UNITY_EDITOR
#elif UNITY_ANDROID
        using (AndroidJavaClass plugin = new AndroidJavaClass(string.Format(Plugins.ANDROID_CLASS_MASK, _instance.Name))) {
            plugin.CallStatic("init", app);
        }
#endif
    }

    /// <summary>
    /// Get image path from gallery
    /// </summary>
    /// <param name="callback">Path to image</param>
    public static void Pick(Action<string, int> callback) {
        _instance._callback = callback;

#if UNITY_EDITOR
#elif UNITY_ANDROID
        using (AndroidJavaClass plugin = new AndroidJavaClass(string.Format(Plugins.ANDROID_CLASS_MASK, _instance.Name))) {
            plugin.CallStatic("pick");
        }
#elif UNITY_IOS
        imagesPick();
#endif
    }

    /// <summary>
    /// Get image path from camera
    /// </summary>
    /// <param name="callback">Path to image</param>
    public static void Capture(Action<string, int> callback) {
        _instance._callback = callback;
#if UNITY_EDITOR
#elif UNITY_ANDROID
        using (AndroidJavaClass plugin = new AndroidJavaClass(string.Format(Plugins.ANDROID_CLASS_MASK, _instance.Name))) {
            plugin.CallStatic("capture");
        }
#elif UNITY_IOS
        imagesCapture();
#endif
    }

    /// <summary>
    /// Close service after use
    /// </summary>
    public static void Close() {
#if UNITY_EDITOR
#elif UNITY_ANDROID
        using (AndroidJavaClass plugin = new AndroidJavaClass(string.Format(Plugins.ANDROID_CLASS_MASK, _instance.Name))) {
            plugin.CallStatic("close");
        }
#endif        
    }

}