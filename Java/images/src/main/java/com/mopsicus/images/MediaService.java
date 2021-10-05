package com.mopsicus.images;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import com.unity3d.player.UnityPlayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Logger;

import static android.app.Activity.RESULT_OK;
import static android.app.Activity.RESULT_CANCELED;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

public class MediaService extends Service {

    /**
     * Downloader in background
     */
    private class Downloader extends AsyncTask<Uri, String, File> {

        @Override
        protected File doInBackground(Uri... uri) {
            File file = makeEmptyFileIntoExternalStorageWithTitle(googleFileName);
            try {
                InputStream inputStream = activity.getApplicationContext().getContentResolver().openInputStream(uri[0]);
                int originalSize = inputStream.available();
                BufferedInputStream streamInput = null;
                BufferedOutputStream streamOutput = null;
                streamInput = new BufferedInputStream(inputStream);
                streamOutput = new BufferedOutputStream(new FileOutputStream(file, false));
                byte[] buffer = new byte[originalSize];
                streamInput.read(buffer);
                do {
                    streamOutput.write(buffer);
                } while (streamInput.read(buffer) != -1);
                streamOutput.flush();
                streamOutput.close();
                streamInput.close();
            } catch (Exception e) {
                return null;
            }
            return file;
        }

        @Override
        protected void onPostExecute(File data) {
            if (data != null) {
                String path = data.getPath();
                int degree = getExifOrientation(path);
                sendData(path, degree);
            } else {
                Plugin.common.sendError(Plugin.name, IMAGE_ERROR, "data is null");
                return;
            }
        }

    }

    /**
     * Flag to wait before close fragment and show progress
     */
    private final String WAIT_STATE = "WAIT_STATE";

    /**
     * Flag to show that user denied to show permission request
     */
    private String NEVER_ASK_STATE = "NEVER_ASK_STATE";

    /**
     * Flag to show that can't load image from gallery or capture
     */
    private String IMAGE_ERROR = "IMAGE_ERROR";

    /**
     * Cahce file name for download
     */
    private String googleFileName;

    /**
     * Uri for image from camera
     */
    private Uri imageUri;

    /**
     * Donwloader instance
     */
    private Downloader downloader;

    /**
     * App name for file provider
     */
    private String appName;

    /**
     * Flag for permission request
     */
    private final int REQUEST_PERMISSION = 10000;

    /**
     * Cache for check permissions
     */
    private boolean isNeedPermissions;

    /**
     * Current activity
     */
    private Activity activity;

    /**
     * Cached intent
     */
    private Intent cachedIntent;

    /**
     * Cached action for permission first request
     */
    private Plugin.MediaAction cachedAction;

    /**
     * Constructor
     */
    public void onCreate() {
        activity = UnityPlayer.currentActivity;
        super.onCreate();
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Clear all
     */
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Action when start service
     */
    public int onStartCommand(Intent intent, int flags, int startId) {
        cachedIntent = intent;
        Bundle bundle = cachedIntent.getExtras();
        Plugin.MediaAction action = Plugin.MediaAction.values()[bundle.getInt(Plugin.ACTION, -1)];
        final String data = bundle.getString(Plugin.DATA);
        switch (action) {
            case RESULT:
                handleActivityResult(data);
                break;
            case PERMISSION:
                handlePermissionResult(data);
                break;
            default:
                cachedAction = action;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (isPermissions()) {
                        execute();
                    } else {
                        isNeedPermissions = isNeedPermissionsDialog();
                        activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
                    }
                } else {
                    execute();
                }
                break;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Execute passed action
     */
    private void execute() {
        Bundle bundle = cachedIntent.getExtras();
        appName = bundle.getString(Plugin.APP);
        final String data = bundle.getString(Plugin.DATA);
        switch (cachedAction) {
            case CAPTURE_PHOTO:
                imageUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
                Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                captureIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                activity.startActivityForResult(captureIntent, Plugin.MediaAction.CAPTURE_PHOTO.ordinal());
                break;
            case PICK_IMAGE:
                Intent pickIntent = new Intent(Intent.ACTION_GET_CONTENT);
                pickIntent.setType("image/*");
                pickIntent.addCategory(Intent.CATEGORY_OPENABLE);
                pickIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, false);
                Intent picker = Intent.createChooser(pickIntent, null);
                activity.startActivityForResult(picker, Plugin.MediaAction.PICK_IMAGE.ordinal());
                break;
            case SAVE_IMAGE:
                saveToLibrary(data);
                break;
            default:
                break;
        }
    }

    /**
     * Send image path and degree to Unity
     *
     * @param path   Path to file
     * @param degree Degree to rotate
     */
    private void sendData(String path, int degree) {
        JSONObject data = new JSONObject();
        try {
            data.put("path", path);
            data.put("degree", degree);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Plugin.common.sendData(Plugin.name, data.toString());
    }

    /**
     * Process data from activity result and give answer
     *
     * @param jsonString JSON for parse
     */
    private void handleActivityResult(String jsonString) {
        JSONObject json = null;
        int resultCode = RESULT_CANCELED;
        int requestCode = -1;
        String uri = null;
        try {
            json = new JSONObject(jsonString);
            resultCode = json.getInt("resultCode");
            requestCode = json.getInt("requestCode");
            uri = json.getString("result");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (resultCode == RESULT_CANCELED) {
            return;
        }
        if (resultCode != RESULT_OK) {
            Plugin.common.sendError(Plugin.name, IMAGE_ERROR, String.valueOf(resultCode));
            return;
        }
        if (uri != null && uri.length() > 0) {
            imageUri = Uri.parse(uri);
        }
        if (requestCode == Plugin.MediaAction.PICK_IMAGE.ordinal()) {
            String path = getPath(activity, imageUri);
            if (path.equals(WAIT_STATE)) {
                sendData(WAIT_STATE, -1);
                return;
            }
            int degree = getExifOrientation(path);
            sendData(path, degree);
        } else if (requestCode == Plugin.MediaAction.CAPTURE_PHOTO.ordinal()) {
            String path = imageUri.getPath();
            path = path.replace("/external_files", Environment.getExternalStorageDirectory().toString());
            int degree = getExifOrientation(path);
            sendData(path, degree);
        }
    }

    /**
     * Process data from permission result
     *
     * @param jsonString JSON for parse
     */
    private void handlePermissionResult(String jsonString) {
        JSONObject json = null;
        JSONArray perms = null;
        JSONArray grants = null;
        int requestCode = -1;
        String uri = null;
        try {
            json = new JSONObject(jsonString);
            requestCode = json.getInt("requestCode");
            perms = json.getJSONArray("permissions");
            grants = json.getJSONArray("grantResults");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        int[] grantResults = new int[grants.length()];
        for (int i = 0; i < grants.length(); ++i) {
            grantResults[i] = grants.optInt(i);
        }
        switch (requestCode) {
            case REQUEST_PERMISSION: {
                if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    execute();
                } else {
                    if (!isNeedPermissions && !isNeedPermissionsDialog()) {
                        Plugin.common.sendError(Plugin.name, NEVER_ASK_STATE);
                    }
                }
            }
        }
    }

    /**
     * Get orientation from Exif info
     *
     * @param filepath Path to file
     * @return Degree
     */
    public int getExifOrientation(String filepath) {
        int degree = 0;
        ExifInterface exif = null;
        if (filepath == null || filepath.equals(""))
            return degree;
        try {
            exif = new ExifInterface(filepath);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (exif != null) {
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            if (orientation != -1) {
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        degree = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        degree = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        degree = 270;
                        break;
                }

            }
        }
        return degree;
    }

    /**
     * Save image to system gallery
     *
     * @param path Path to file for save in gallery
     */
    private void saveToLibrary(String path) {
        File file = new File(path);
        String extension = getExtension(path);
        if (file.exists()) {
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/" + extension);
                values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
                activity.getApplicationContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                sendData("SAVE_SUCCESS", -1);
            } catch (Exception e) {
                Plugin.common.sendError(Plugin.name, "SAVE_ERROR");
            }
        } else {
            Plugin.common.sendError(Plugin.name, "SAVE_ERROR");
        }
    }

    /**
     * Check are there permissions to read and write to external storage
     *
     * @return Has permissions or not
     */
    private boolean isPermissions() {
        int writePermission = ContextCompat.checkSelfPermission(activity.getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermission = ContextCompat.checkSelfPermission(activity.getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
        return (readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED);
    }

    /**
     * Check need or not show permission request dialog
     *
     * @return Need show or not
     */
    private boolean isNeedPermissionsDialog() {
        boolean isWriteRequest = ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        boolean isReadRequest = ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        return (isWriteRequest || isReadRequest);
    }

    /**
     * Create a file Uri for saving an image or video
     *
     * @param type File type
     * @return Uri
     */
    private Uri getOutputMediaFileUri(int type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String authority = appName.concat(".fileprovider");
            return FileProvider.getUriForFile(activity.getApplicationContext(), authority, getOutputMediaFile(type));
        } else {
            return Uri.fromFile(getOutputMediaFile(type));
        }
    }

    /**
     * Create a File for saving an image or video
     *
     * @param type File type
     * @return File
     */
    private File getOutputMediaFile(int type) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Camera");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }
        return mediaFile;
    }

    /**
     * Get cache directory
     *
     * @param context Current context
     * @return File object
     */
    public File getDocumentCacheDir(@NonNull Context context) {
        File dir = new File(context.getCacheDir(), "documents");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Generate filename in folder, add postfix if exists
     *
     * @param name      Name of file
     * @param directory Directory to generate
     * @return File object
     */
    public File generateFileName(@Nullable String name, File directory) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        File file = new File(directory, name);
        if (file.exists()) {
            String fileName = name;
            String extension = "";
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0) {
                fileName = name.substring(0, dotIndex);
                extension = name.substring(dotIndex);
            }
            int index = 0;
            while (file.exists()) {
                index++;
                name = fileName + '(' + index + ')' + extension;
                file = new File(directory, name);
            }
        }
        try {
            if (!file.createNewFile()) {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
        return file;
    }

    /**
     * Get name of file
     *
     * @param filename Full name of file
     * @return Name
     */
    public static String getName(String filename) {
        if (filename == null) {
            return null;
        }
        int index = filename.lastIndexOf('/');
        return filename.substring(index + 1);
    }

    /**
     * Get file name
     *
     * @param context Current context
     * @param uri     Uri to query
     * @return File name
     */
    public String getFileName(@NonNull Context context, Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        String filename = null;
        if (mimeType == null && context != null) {
            String path = getPath(context, uri);
            if (path == null || path.isEmpty()) {
                uri.toString();
            }
            if (path == null) {
                filename = getName(uri.toString());
            } else {
                File file = new File(path);
                filename = file.getName();
            }
        } else {
            Cursor returnCursor = context.getContentResolver().query(uri, null, null, null, null);
            if (returnCursor != null) {
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();
                filename = returnCursor.getString(nameIndex);
                returnCursor.close();
            }
        }
        return filename;
    }

    /**
     * Save file to device
     *
     * @param context         Current context
     * @param uri             Uri of file
     * @param destinationPath Path to save
     */
    private void saveFileFromUri(Context context, Uri uri, String destinationPath) {
        InputStream inputStream = null;
        BufferedOutputStream outputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);
            outputStream = new BufferedOutputStream(new FileOutputStream(destinationPath, false));
            byte[] buffer = new byte[1024];
            inputStream.read(buffer);
            do {
                outputStream.write(buffer);
            } while (inputStream.read(buffer) != -1);
        } catch (IOException e) {
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
            }
        }
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     * Callers should check whether the path is local before assuming it
     * represents a local file.
     *
     * @param context The context
     * @param uri     The Uri to query
     */
    @SuppressLint("NewApi")
    public String getPath(final Context context, final Uri uri) {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    String path = Environment.getExternalStorageDirectory().getPath();
                    if (split.length > 1) {
                        path += "/" + split[1];
                    }
                    return path;
                } else if ("raw".equalsIgnoreCase(type)) {
                    return split[1];
                }
            } else if (isDownloadsDocument(uri)) {
                String id = DocumentsContract.getDocumentId(uri);
                if (id.startsWith("raw:")) {
                    return id.substring(4);
                }
                if (id.startsWith("msf:")) {
                    id = id.substring(4);
                }
                String[] contentUriPrefixesToTry = new String[]{"content://downloads/public_downloads", "content://downloads/my_downloads", "content://downloads/all_downloads"};
                for (String contentUriPrefix : contentUriPrefixesToTry) {
                    Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.valueOf(id));
                    try {
                        String path = getDataColumn(context, contentUri, null, null);
                        if (path != null) {
                            return path;
                        }
                    } catch (Exception e) {
                    }
                }
                String fileName = getFileName(context, uri);
                File cacheDir = getDocumentCacheDir(context);
                File file = generateFileName(fileName, cacheDir);
                String destinationPath = null;
                if (file != null) {
                    destinationPath = file.getAbsolutePath();
                    saveFileFromUri(context, uri, destinationPath);
                }
                return destinationPath;
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                } else if ("raw".equals(type)) {
                    return split[1];
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
            if (isGoogleDriveUri(uri)) {
                DocumentFile documentFile = DocumentFile.fromSingleUri(activity.getApplicationContext(), uri);
                googleFileName = documentFile.getName();
                downloader = new Downloader();
                downloader.execute(uri);
                return WAIT_STATE;
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            if (isGooglePhotosUri(uri)) {
                String path = getDataColumn(context, uri, null, null);
                if (path == null) {
                    DocumentFile documentFile = DocumentFile.fromSingleUri(activity.getApplicationContext(), uri);
                    googleFileName = documentFile.getName();
                    downloader = new Downloader();
                    downloader.execute(uri);
                    return WAIT_STATE;
                }
            }
            if (isGoogleDriveUri(uri)) {
                DocumentFile documentFile = DocumentFile.fromSingleUri(activity.getApplicationContext(), uri);
                googleFileName = documentFile.getName();
                downloader = new Downloader();
                downloader.execute(uri);
                return WAIT_STATE;
            }
            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * Save empty file to disc
     *
     * @param title File name
     * @return Empty file
     */
    public static File makeEmptyFileIntoExternalStorageWithTitle(String title) {
        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
        return new File(root, title);
    }

    /**
     * Gets the extension of a file name, like ".png" or ".jpg".
     *
     * @param path Path to file
     * @return Extension including the dot("."); "" if there is no extension;
     * null if uri was null.
     */
    public static String getExtension(String path) {
        if (path == null) {
            return null;
        }
        int dot = path.lastIndexOf(".");
        return (dot >= 0) ? path.substring(dot) : "";
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.contentprovider".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Drive.
     */
    public static boolean isGoogleDriveUri(Uri uri) {
        return "com.google.android.apps.docs.storage.legacy".equals(uri.getAuthority());
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

}
