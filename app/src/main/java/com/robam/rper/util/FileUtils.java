package com.robam.rper.util;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import com.robam.rper.activity.MyApplication;
import com.robam.rper.service.SPService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * author : liuxiaohu
 * date   : 2019/8/12 10:02
 * desc   :
 * version: 1.0
 */
public class FileUtils extends org.apache.commons.io.FileUtils {
    private static final String TAG = "FileUtils";
    private static final String SHELL_SDCARD = "/sdcard";
    private static String sdcardPath = null;

    private static String RPER_FOLDER_NAME = null;

    private static File rperBaseDir = null;

    public static void installApk(Context context, Uri apkPath) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        //此处因为上下文是Context，所以要加此Flag，不然会报错
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(apkPath, "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    public static String readFile(File file) {
        String content = null;
        BufferedReader br = null;

        // 避免文件不存在抛异常
        if (!file.exists()) {
            return null;
        }

        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            content = sb.toString();
        } catch (IOException e) {
            LogUtil.e(TAG, "Catch IOException: " + e.getMessage(), e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    LogUtil.e(TAG, "Catch IOException: " + e.getMessage(), e);
                }
            }
        }
        return content;
    }

    /**
     * 获取RobamPer文件夹目录名称
     * @return
     */
    public static String getRperFolderName() {
        if (RPER_FOLDER_NAME == null) {
            RPER_FOLDER_NAME = SPService.getString(SPService.KEY_RPER_PATH_NAME, "RobamPer");
        }

        return RPER_FOLDER_NAME;
    }

    /**
     * 获取RobamPer输出目录
     * @return
     */
    public static File getRperDir() {
        if (rperBaseDir == null) {
            if (RPER_FOLDER_NAME == null) {
                RPER_FOLDER_NAME = SPService.getString(SPService.KEY_RPER_PATH_NAME, "RobamPer");
            }

            rperBaseDir = new File(Environment.getExternalStorageDirectory(), RPER_FOLDER_NAME);

        }
        if (!rperBaseDir.exists()) {
            rperBaseDir.mkdirs();
        }

        return rperBaseDir;
    }

    /**
     * 只有在开了IO权限也写不了的情况下才会fallback
     * @param context
     * @return
     */
    public static File fallBackToExternalDir(Context context) {
        File extrenalDir = context.getExternalFilesDir("RobamPer");
        if (extrenalDir == null) {
            return null;
        }

        File base = Environment.getExternalStorageDirectory();
        String extrenalPath = extrenalDir.getAbsolutePath();
        String sdcardPath = base.getAbsolutePath();
        int idx = extrenalPath.indexOf(sdcardPath);
        if (idx > -1) {
            RPER_FOLDER_NAME = extrenalPath.substring(idx + sdcardPath.length() + 1);
        } else {
            // 强行回到根目录
            RPER_FOLDER_NAME = extrenalPath;
        }
        // 保存下新目录
        SPService.putString(SPService.KEY_RPER_PATH_NAME, RPER_FOLDER_NAME);

        rperBaseDir = extrenalDir;
        if (!extrenalDir.exists()) {
            extrenalDir.mkdirs();
        }

        return extrenalDir;
    }

    /**
     * 获取需要自动清理文件夹列表
     * @return
     */
    public static File[] getAutoClearDirs() {
        return new File[] {getSubDir("download"), getSubDir("screenshot"),
                getSubDir("tmp"), getSubDir("logcat"), getSubDir("ScreenCaptures"),
                new File(MyApplication.getInstance().getExternalCacheDir(), "logs")};
    }

    /**
     * 获取Soloπ子目录
     * @return
     */
    public static File getSubDir(String name) {
        File subDir = new File(getRperDir(), name);
        if (!subDir.exists()) {
            subDir.mkdirs();
        }

        return subDir;
    }

    /**
     * 获取SoloπCache目录
     * @param name
     * @return
     */
    public static File getSubCacheDir(String name) {
        File subDir = new File(MyApplication.getInstance().getExternalCacheDir(), name);
        if (!subDir.exists()) {
            subDir.mkdirs();
        }

        return subDir;
    }

    /**
     * 获取shell下的路径
     * @param file
     * @return
     */
    public static String getPathInShell(File file) {
        if (file == null) {
            return null;
        }

        String path = file.getAbsolutePath();
        if (sdcardPath == null) {
            sdcardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        }

        // 替换为/sdcard模式
        if (path.startsWith(sdcardPath)) {
            path = SHELL_SDCARD + path.substring(sdcardPath.length());
        }

        return path;
    }

    /**
     * 获取Soloπ子目录
     * @return
     */
    public static File getInnerSubDir(String name) {
        File subDir = new File(MyApplication.getContext().getFilesDir(), name);
        if (!subDir.exists()) {
            subDir.mkdirs();
        }

        return subDir;
    }

    public static byte[] getFileBytes(File file){
        byte[] buf = null;
        try {
            InputStream in = new FileInputStream(file);
            buf = new byte[in.available()];
            while (in.read(buf) != -1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return buf;
    }

    public static long getFileSize(File file) {
        if (file.isFile()) {
            return file.length();
        }

        long length = 0;
        for (File curFile : file.listFiles()) {
            if (curFile.isFile())
                length += curFile.length();
            else
                length += getFileSize(curFile);
        }
        return length;
    }


    public static void writeToFile(String content, File file) {
        OutputStreamWriter writer = null;
        BufferedWriter bw = null;
        try {
            OutputStream os = new FileOutputStream(file);
            writer = new OutputStreamWriter(os);
            bw = new BufferedWriter(writer);
            bw.write(content);
            bw.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void deleteFile(File file) {
        if (file == null) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            for (File curFile : children) {
                deleteFile(curFile);
            }
        }
        file.delete();
    }

    private static String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = MyApplication.getContext().getContentResolver()
                .query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    public static String getImageAbsolutePath(Uri uri) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            String imagePath = null;
            if (DocumentsContract.isDocumentUri(MyApplication.getContext(), uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                    String id = docId.split(":")[1];
                    String selection = MediaStore.Images.Media._ID + "=" + id;
                    imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
                } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                    Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"),
                            Long.valueOf(docId));
                    imagePath = getImagePath(contentUri, null);
                }
            } else if ("content".equalsIgnoreCase(uri.getScheme())) {
                imagePath = getImagePath(uri, null);
            }
            return imagePath;
        } else {
            return getImagePath(uri, null);
        }
    }

    /**
     * 检查文件MD5值
     * @param file
     * @param md5
     * @return
     */
    public static boolean checkFileMd5(File file, String md5) {
        if (!file.isFile() || StringUtil.isEmpty(md5)) {
            return false;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);

            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        BigInteger bigInt = new BigInteger(1, digest.digest());
        return new BigInteger(md5, 16).equals(bigInt);
    }


    private static String SDPATH = "";

    /**
     * 获取到sd卡的根目录，并以String形式返回
     *
     * @return
     */
    public static String getSDCardPath() {
        SDPATH = Environment.getExternalStorageDirectory() + "/";
        return SDPATH;
    }

    /**
     * 创建文件或文件夹
     *
     * @param fileName
     *            文件名或问文件夹名
     */
    public static void createFile(String fileName) {
        File file = new File(getSDCardPath() + fileName);
        if (!file.exists()) {
            if (fileName.contains(".")) {
                // 说明包含，即使创建文件, 返回值为-1就说明不包含.,即使文件
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                System.out.println("创建了文件");
            } else {
                // 创建文件夹
                file.mkdir();
                System.out.println("创建了文件夹");
            }
        }
    }

    public static boolean isExists(String fileName) {
        File file = new File(getSDCardPath() + fileName);
        return file.exists();
    }
}
