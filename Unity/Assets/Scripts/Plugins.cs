using System;
using System.Collections.Generic;
using NiceJson;
using UnityEngine;
#if UNITY_IOS
using System.Runtime.InteropServices;
#endif


/// <summary>
/// Mobile plugin interface
/// Each plugin must implement it
/// </summary>
public interface IPlugin {

    /// <summary>
    /// Plaugin name
    /// </summary>
    string Name { get; }

    /// <summary>
    /// Callback on get data
    /// </summary>
    void OnData(JsonObject data);

    /// <summary>
    /// Callback on get error
    /// </summary>
    void OnError(JsonObject data);
}

/// <summary>
/// Plugin service to manager all mobile plugins
/// </summary>
public class Plugins : MonoBehaviour {

#if UNITY_ANDROID
    /// <summary>
    /// Mask for Java classes
    /// </summary>
    public const string ANDROID_CLASS_MASK = "com.mopsicus.{0}.Plugin";
#endif

    /// <summary>
    /// Gameobject name on scene to receive data
    /// ACHTUNG! Do not change it
    /// </summary>
    const string _dataObject = "Plugins";

    /// <summary>
    /// Dictionary of plugins
    /// </summary>
    private Dictionary<string, IPlugin> _plugins = null;

    /// <summary>
    /// Constructor
    /// </summary>
    private void Awake() {
        name = _dataObject;
        DontDestroyOnLoad(gameObject);
        InitPlugins();
    }

    /// <summary>
    /// Destructor
    /// </summary>
    private void OnDestroy() {
        _plugins = null;
    }

    /// <summary>
    /// Init all plugins in app
    /// </summary>
    void InitPlugins() {
        gameObject.AddComponent<Images>();
        gameObject.AddComponent<Share>();
        gameObject.AddComponent<Review>();
        IPlugin[] plugins = GetComponents<IPlugin>();
        _plugins = new Dictionary<string, IPlugin>(plugins.Length);
        foreach (IPlugin item in plugins) {
            _plugins.Add(item.Name, item);
        }
#if DEBUG
        Debug.Log("Plugins inited");
#endif
    }

    /// <summary>
    /// Handler to process data to plugin
    /// Function name to receive data
    /// ACHTUNG! Do not change it
    /// </summary>
    /// <param name="data">data from plugin</param>
    void OnDataReceive(string data) {
#if DEBUG
        Debug.Log("Plugins receive data: " + data);
#endif
        try {
            JsonObject info = (JsonObject)JsonNode.ParseJsonString(data);
            if (_plugins.ContainsKey(info["name"])) {
                IPlugin plugin = _plugins[info["name"]];
                if (info.ContainsKey("error")) {
                    plugin.OnError(info);
                } else {
                    plugin.OnData(info);
                }
            } else {
#if DEBUG
                Debug.LogError(string.Format("{0} plugin does not exists", info["name"]));
#endif
            }
        } catch (Exception e) {
#if DEBUG
            Debug.LogError(string.Format("Plugins receive error: {0}, stack: {1}", e.Message, e.StackTrace));
#endif
        }

    }

}