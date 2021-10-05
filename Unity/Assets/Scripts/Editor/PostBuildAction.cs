using System;
using System.IO;
using System.Reflection;
using System.Text;
using UnityEditor;
using UnityEditor.Callbacks;
using UnityEngine;
#if UNITY_IOS
using UnityEditor.iOS.Xcode;
#endif

/// <summary>
/// Proccess data after build
/// Folder for libs in root, Frameworks
/// </summary>
public class PostBuildActions {

    /// <summary>
    /// Run after project build
    /// Save version.txt file for build shell script
    /// </summary>
    /// <param name="buildTarget">Platform</param>
    /// <param name="path">Path to folder</param>
    [PostProcessBuild]
    public static void PostProcess(BuildTarget buildTarget, string pathToBuiltProject) {
        if (buildTarget == BuildTarget.iOS) {
#if UNITY_IOS
            FixPlist(pathToBuiltProject);
            AddFrameworks(pathToBuiltProject);
#endif
            try {
                int build = int.Parse(PlayerSettings.iOS.buildNumber);
                build++;
                PlayerSettings.iOS.buildNumber = build.ToString();
                Debug.Log(string.Format("New build number: {0}", build));
            } catch (Exception e) {
                Debug.LogError(string.Format("Error on setup new build: {0}. Error: {1}", PlayerSettings.iOS.buildNumber, e.Message));
            }
        } else if (buildTarget == BuildTarget.Android) {
            PlayerSettings.Android.bundleVersionCode++;
            Debug.Log(string.Format("New bundle version code: {0}", PlayerSettings.Android.bundleVersionCode));
        }
    }

#if UNITY_IOS
    /// <summary>
    /// Add params to Info.plist
    /// </summary>
    /// <param name="path">Path to folder</param>
    private static void FixPlist(string path) {
        string plistPath = path + "/Info.plist";
        PlistDocument plist = new PlistDocument();
        plist.ReadFromString(File.ReadAllText(plistPath));
        PlistElementDict rootDict = plist.root;
        rootDict.SetString("NSPhotoLibraryUsageDescription", "$(PRODUCT_NAME) need to access the library in order to select photo");
        rootDict.SetString("NSCameraUsageDescription", "$(PRODUCT_NAME) need to access the camera in order to capture photo");
        File.WriteAllText(plistPath, plist.WriteToString());
        Debug.Log("Plist fixed");
    }

    /// <summary>
    /// Connect external libs
    /// </summary>
    /// <param name="path">Path to folder</param>
    private static void AddFrameworks(string path) {
     string projectPath = string.Format("{0}/Unity-iPhone.xcodeproj/project.pbxproj", path);
        PBXProject project = new PBXProject();
        string file = File.ReadAllText(projectPath);
        project.ReadFromString(file);
        string targetFramework = project.GetUnityFrameworkTargetGuid();
        project.AddFrameworkToProject(targetFramework, "StoreKit.framework", false);
        File.WriteAllText(projectPath, project.WriteToString());
        Debug.Log("Frameworks added");
    }
#endif

}