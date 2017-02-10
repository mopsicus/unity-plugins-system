using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using NiceJson;

public interface IPlugin {
	string Name {get;}
	void Init (JsonObject data);
	void OnData (JsonObject data);
	void OnError (JsonObject data);
}

public class PluginsController : MonoBehaviour {

	private string _dataReceiver = "OnDataReceive";
	private IPlugin[] _plugins;

	void Start () {
		JsonObject data = new JsonObject ();
		data["object"] = name;
		data["receiver"] = _dataReceiver;
		_plugins = GetComponentsInChildren <IPlugin> ();
		foreach (IPlugin plugin in _plugins)
			plugin.Init (data);
	}

	void OnDataReceive (string data) {
		JsonObject info = (JsonObject) JsonNode.ParseJsonString (data);
		foreach (IPlugin plugin in _plugins) {
			if (plugin.Name == info["name"]) {
				if (info.HasKey ("error"))
					plugin.OnError (info);
				else
					plugin.OnData (info);
				break;
			}
		}
	}
		


}
