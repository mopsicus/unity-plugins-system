using System.IO;
using UnityEngine;
using UnityEngine.UI;

public class DemoController : MonoBehaviour {

    [SerializeField]
    private Image PreviewImage = null;

    /// <summary>
    /// Name of app should equls with name in manifest
    /// </summary>
    void Start() {
        Images.Init("pluginssystem");
    }

    /// <summary>
    /// Url to open for review in external browser
    /// </summary>
    public static string AppStoreUrl {
        get {
#if UNITY_EDITOR
            return "https://mopsicus.ru";
#elif UNITY_ANDROID
            return "https://play.google.com/store/apps/details?id=com.linarapps.kitchenassistant";
#elif UNITY_IOS
            return "https://apps.apple.com/ru/app/id1572795201";
#endif
        }
    }

    /// <summary>
    /// Pick image from gallery
    /// </summary>
    public void Pick() {
        Images.Pick((path, degree) => {
#if DEBUG
            Debug.LogFormat("Pick image result: path = {0}, degree = {1}", path, degree);
#endif
            byte[] data = LoadFile(path);
            PreviewImage.sprite = CreateSpriteFromData(data);
        });
    }

    /// <summary>
    /// Capture image from camera
    /// </summary>
    public void Capture() {
        Images.Capture((path, degree) => {
#if DEBUG
            Debug.LogFormat("Capture image result: path = {0}, degree = {1}", path, degree);
#endif
            byte[] data = LoadFile(path);
            PreviewImage.sprite = CreateSpriteFromData(data);
        });
    }

    /// <summary>
    /// Open native share dialog
    /// </summary>
    public void ShareText() {
        Share.Text("This is demo from Mopsicus's article about plugins: https://habr.com/ru/post/581160/");
    }

    /// <summary>
    /// Open in-app review popup
    /// Works only in internal testing or production
    /// </summary>
    public void ReviewApp() {
        Review.Open();
    }
    /// <summary>
    /// Load file from disk
    /// If path is null, then filename should be with full path
    /// </summary>
    /// <param name="fileName">File name</param>
    /// <param name="filePath">File path</param>
    public static byte[] LoadFile(string fileName, string filePath = null) {
        string path = (!string.IsNullOrEmpty(filePath)) ? Path.Combine(filePath, fileName) : fileName;
        if (!File.Exists(path)) {
            return null;
        } else {
            return File.ReadAllBytes(path);
        }
    }

    /// <summary>
    /// Make sprite from bytes array
    /// </summary>
    Sprite CreateSpriteFromData(byte[] data) {
        Texture2D image = null;
#if UNITY_EDITOR
        image = new Texture2D(1, 1, TextureFormat.RGBA32, false);
#elif UNITY_ANDROID
        image = new Texture2D(1, 1, TextureFormat.ETC2_RGBA8, false);
#elif UNITY_IOS
        image = new Texture2D(1, 1, TextureFormat.ASTC_4x4, false);
#endif
        image.LoadImage(data);
        return Sprite.Create(image, new Rect(0f, 0f, image.width, image.height), new Vector2(0.5f, 0.5f));
    }

}
