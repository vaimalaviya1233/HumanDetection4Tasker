package online.avogadro.opencv4tasker.app;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.system.Os;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import online.avogadro.opencv4tasker.opencv.HumansDetector;

public class Util {

    private static final String TAG = "Util";

    public static String getMetadata(Context c, String key) {
        try {
            ApplicationInfo ai = c.getPackageManager().getApplicationInfo(c.getPackageName(),
                    PackageManager.GET_META_DATA);

            Bundle metaData = ai.metaData;

            return metaData.getString(key, "8");
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getMetadataInt(Context c, String key) {
        try {
            ApplicationInfo ai = c.getPackageManager().getApplicationInfo(c.getPackageName(),
                    PackageManager.GET_META_DATA);
            Bundle metaData = ai.metaData;

            return metaData.getInt(key,8);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getPathFromUri(Context context, Uri uri) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        InputStream inputStream = resolver.openInputStream(uri);

        // Create temporary file
        File tempFile = File.createTempFile("temp_image", ".jpg", context.getCacheDir());
        tempFile.deleteOnExit();

        // Copy input stream to temporary file
        FileOutputStream out = new FileOutputStream(tempFile);
        byte[] buffer = new byte[64*1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        out.flush();
        out.close();
        inputStream.close();

        return tempFile.getAbsolutePath();
    }

    /**
     * OpenCV and other libs are unable to handle content:// URIs
     * This method handls this for them by copying to a temporary file
     *
     * @param path
     * @return
     */
    /**
     * Tries to resolve the actual filesystem path from a content URI without copying the file.
     * Suitable for large files (e.g. ML models). Returns null if the path cannot be determined.
     *
     * Uses four strategies in order:
     * 1. MediaStore DATA column (direct query on URI)
     * 1b. DownloadsProvider msf: → MediaStore ID lookup
     * 2. /proc/self/fd/ symlink via ParcelFileDescriptor
     * 3. DISPLAY_NAME lookup in common Download directories
     */
    public static String getModelPathFromUri(Context context, Uri uri) {
        Log.d(TAG, "getModelPathFromUri: uri=" + uri + " authority=" + uri.getAuthority());

        if ("file".equals(uri.getScheme())) {
            return uri.getPath();
        }
        if (!"content".equals(uri.getScheme())) {
            return null;
        }

        // Track best candidate path found (even if canRead fails)
        String bestCandidate = null;

        // Strategy 1: MediaStore DATA column
        try {
            String[] projection = { MediaStore.MediaColumns.DATA };
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int col = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                        String path = cursor.getString(col);
                        Log.d(TAG, "Strategy 1: DATA=" + path + " canRead=" + (path != null && new File(path).canRead()) + " exists=" + (path != null && new File(path).exists()));
                        if (path != null && !path.isEmpty()) {
                            if (new File(path).canRead()) {
                                return path;
                            }
                            if (new File(path).exists()) {
                                bestCandidate = path;
                            }
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Strategy 1 (MediaStore DATA) failed: " + e.getMessage());
        }

        // Strategy 1b: DownloadsProvider with msf: document ID → MediaStore lookup
        try {
            if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                String docId = DocumentsContract.getDocumentId(uri);
                Log.d(TAG, "Strategy 1b: docId=" + docId);
                if (docId != null && docId.startsWith("msf:")) {
                    long mediaId = Long.parseLong(docId.substring(4));
                    Uri mediaUri = ContentUris.withAppendedId(
                            MediaStore.Files.getContentUri("external"), mediaId);
                    String[] proj = { MediaStore.MediaColumns.DATA };
                    Cursor cursor = context.getContentResolver().query(mediaUri, proj, null, null, null);
                    if (cursor != null) {
                        try {
                            if (cursor.moveToFirst()) {
                                String path = cursor.getString(0);
                                Log.d(TAG, "Strategy 1b: DATA=" + path + " canRead=" + (path != null && new File(path).canRead()) + " exists=" + (path != null && new File(path).exists()));
                                if (path != null && !path.isEmpty()) {
                                    if (new File(path).canRead()) {
                                        return path;
                                    }
                                    if (new File(path).exists()) {
                                        bestCandidate = path;
                                    }
                                }
                            } else {
                                Log.d(TAG, "Strategy 1b: cursor empty for mediaId=" + mediaId);
                            }
                        } finally {
                            cursor.close();
                        }
                    } else {
                        Log.d(TAG, "Strategy 1b: cursor is null");
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Strategy 1b (DownloadsProvider msf:) failed: " + e.getMessage());
        }

        // Strategy 2: /proc/self/fd/ symlink
        try {
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
            if (pfd != null) {
                try {
                    String fdPath = "/proc/self/fd/" + pfd.getFd();
                    String realPath = Os.readlink(fdPath);
                    Log.d(TAG, "Strategy 2: realPath=" + realPath + " canRead=" + (realPath != null && new File(realPath).canRead()) + " exists=" + (realPath != null && new File(realPath).exists()));
                    if (realPath != null && !realPath.isEmpty()
                            && !realPath.startsWith("/proc/")) {
                        if (new File(realPath).canRead()) {
                            return realPath;
                        }
                        if (new File(realPath).exists()) {
                            bestCandidate = realPath;
                        }
                    }
                } finally {
                    pfd.close();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Strategy 2 (/proc/self/fd) failed: " + e.getMessage());
        }

        // Strategy 3: DISPLAY_NAME + well-known Download directories
        try {
            String displayName = null;
            Cursor cursor = context.getContentResolver().query(uri,
                    new String[]{ OpenableColumns.DISPLAY_NAME }, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        displayName = cursor.getString(0);
                    }
                } finally {
                    cursor.close();
                }
            }
            Log.d(TAG, "Strategy 3: displayName=" + displayName);
            if (displayName != null && !displayName.isEmpty()) {
                String[] downloadDirs = {
                        "/storage/emulated/0/Download/",
                        "/storage/emulated/0/Downloads/"
                };
                for (String dir : downloadDirs) {
                    File candidate = new File(dir, displayName);
                    Log.d(TAG, "Strategy 3: trying " + candidate.getAbsolutePath() + " canRead=" + candidate.canRead() + " exists=" + candidate.exists());
                    if (candidate.canRead()) {
                        return candidate.getAbsolutePath();
                    }
                    if (candidate.exists() && bestCandidate == null) {
                        bestCandidate = candidate.getAbsolutePath();
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Strategy 3 (DISPLAY_NAME) failed: " + e.getMessage());
        }

        // If all strategies found a path but canRead() failed, return the best candidate anyway.
        // The file likely exists but File API lacks permission; LiteRT-LM may still open it.
        if (bestCandidate != null) {
            Log.w(TAG, "All strategies: canRead failed, returning bestCandidate=" + bestCandidate);
            return bestCandidate;
        }

        Log.w(TAG, "getModelPathFromUri: all strategies failed for " + uri);
        return null;
    }

    /**
     * Checks whether a model file path is valid and accessible on the filesystem.
     * Returns false for null, empty, content:// URIs (which need resolution first), or
     * files that cannot be read.
     */
    public static boolean isModelFileAccessible(String path) {
        if (path == null || path.isEmpty() || path.startsWith("content://")) {
            return false;
        }
        File f = new File(path);
        // canRead() may return false under scoped storage even for accessible files;
        // fall back to exists() which is more reliable for paths resolved from SAF URIs.
        return f.canRead() || f.exists();
    }

    public static String contentToFile(Context context, String path) throws IOException {
        if (path.startsWith("file:")) {
            return path;
        } else if (path.startsWith("content:")) {
            return getPathFromUri(context, Uri.parse(path));
        } else {
            Log.w(TAG,"formato path sconosciuto");
            return path;
        }

    }
}
