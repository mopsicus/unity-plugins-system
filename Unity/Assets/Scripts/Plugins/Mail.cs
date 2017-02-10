using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using System.Runtime.InteropServices;
using NiceJson;

public class Mail : MonoBehaviour, IPlugin {

	private static Mail _instance;

	#if UNITY_ANDROID
		const string ANDROID_CLASS_MASK = "com.mopsicus.{0}.Plugin";
	#elif UNITY_IOS
		[DllImport ("__Internal")]
		private static extern void initMail (string data);

		[DllImport ("__Internal")]
		private static extern void testMail ();
	#endif

	void Awake () {
		_instance = GetComponent<Mail> ();
	}

	public string Name {
		get {
			return GetType ().Name.ToLower ();
		}
	}

	public void Init (JsonObject data) {
		#if UNITY_ANDROID
			using (AndroidJavaClass plugin = new AndroidJavaClass (string.Format (ANDROID_CLASS_MASK, Name))) {
				plugin.CallStatic ("init", data.ToJsonString ());
			}
		#elif UNITY_IOS
			initMail (data.ToJsonString ());
		#endif
	}

	public void OnData (JsonObject data) {
		Debug.Log ("OnData "+data.ToJsonPrettyPrintString ());
	}

	public void OnError (JsonObject data) {
		Debug.Log ("OnError "+data.ToJsonPrettyPrintString ());
	}

	public static void Test () {
		#if UNITY_ANDROID
			using (AndroidJavaClass plugin = new AndroidJavaClass (string.Format (ANDROID_CLASS_MASK, _instance.Name))) {
				plugin.CallStatic ("test");
			}
		#elif UNITY_IOS
			testMail ();
		#endif
	}
}
