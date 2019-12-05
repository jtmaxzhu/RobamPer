package com.robam.rper.util;

import com.alibaba.fastjson.JSON;
import com.robam.rper.bean.DeviceInfo;
import com.robam.rper.display.items.base.RecordPattern;

import org.apache.commons.io.Charsets;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * author : liuxiaohu
 * date   : 2019/12/2 16:04
 * desc   :
 * version: 1.0
 */
public class RecordUtil {
    private static final String TAG = "RecordUtil";


    /**
     * 保存到文件夹
     * @param records
     * @return
     */
    public static File saveToFile(Map<RecordPattern, List<RecordPattern.RecordItem>> records){
        Date startTime = new Date(System.currentTimeMillis()*2);
        Date endTime = new Date(System.currentTimeMillis()/2);
        for (RecordPattern pattern:records.keySet()){
            Date tmpStart = new Date(pattern.getStartTime());
            Date tmpEnd = new Date(pattern.getEndTime());
            if (tmpStart.compareTo(startTime)<0){
                startTime = tmpStart;
            }
            if (tmpEnd.compareTo(endTime) > 0) {
                endTime = tmpEnd;
            }
        }
        File saveFolder = loadSaveDir(startTime, endTime);
        for (Map.Entry<RecordPattern, List<RecordPattern.RecordItem>> entry : records.entrySet()){
            RecordPattern pattern = entry.getKey();
            File saveFile = new File(saveFolder,
                    pattern.getName() + "_" + pattern.getSource() + "_" + pattern.getStartTime() + "_" + pattern.getEndTime() + ".csv");
            try {
                if (saveFile.createNewFile()){
                    BufferedWriter writer = new BufferedWriter(new FileWriter(saveFile));
                    //第一行写标题
                    writer.write("RecordTime," +pattern.getName()+ "(" +pattern.getUnit()+ "),extra\n");
                    writer.flush();
                    for (RecordPattern.RecordItem item : entry.getValue()){
                        writer.write(item.time + "," + item.value + "," + item.extra + "\n");
                        writer.flush();
                    }
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return saveFolder;
    }

    /**
     * 加载保存目录
     * @param startTime
     * @param endTime
     * @return
     */

    private static File loadSaveDir(Date startTime, Date endTime){
        File recordDir = FileUtils.getSubDir("records");
        DateFormat format = new SimpleDateFormat("MM月dd日HH:mm:ss", Locale.CHINA);
        File saveFolder = new File(recordDir, format.format(startTime) + "-" + format.format(endTime));
        saveFolder.mkdir();
        return saveFolder;
    }

    /**
     * 上传数据
     * @param path     地址
     * @param records  数据
     * @return
     */
    public static String uploadData(String path, Map<RecordPattern, List<RecordPattern.RecordItem>> records){
        Map<String, Map<String, List<RecordPattern.RecordItem>>> data = new HashMap<>();
        for (RecordPattern pattern : records.keySet()){
            Map<String, List<RecordPattern.RecordItem>> item;
            if (data.containsKey(pattern.getSource())){
                item = data.get(pattern.getSource());
            }else {
                item = new HashMap<>();
                data.put(pattern.getSource(), item);
            }
            item.put(pattern.getName(), records.get(pattern));
        }

        DeviceInfo deviceInfo = DeviceInfoUtil.generateDeviceInfo();

        UploadData uploadData = new UploadData(data, deviceInfo);

        final byte[] content = JSON.toJSONString(uploadData).getBytes(Charsets.UTF_8);

        try {
            RequestBody body = RequestBody.create(MediaType.get("application/json"), content);
            return HttpUtil.postSync(path, body);
        } catch (IOException e) {
            LogUtil.e(TAG, "抛出IO异常", e);
        }
        return null;
    }



    static class UploadData {
        Map<String, Map<String, List<RecordPattern.RecordItem>>> data;
        DeviceInfo model;

        public UploadData() {
        }

        public UploadData(Map<String, Map<String, List<RecordPattern.RecordItem>>> data, DeviceInfo model) {
            this.data = data;
            this.model = model;
        }

        public Map<String, Map<String, List<RecordPattern.RecordItem>>> getData() {
            return data;
        }

        public void setData(Map<String, Map<String, List<RecordPattern.RecordItem>>> data) {
            this.data = data;
        }

        public DeviceInfo getModel() {
            return model;
        }

        public void setModel(DeviceInfo model) {
            this.model = model;
        }
    }

    static class RecordUploadData {
        Map<String, Object> data;
        DeviceInfo model;

        public RecordUploadData() {
        }

        public RecordUploadData(long recordTime, String title, DeviceInfo model) {
            this.data = new HashMap<>(3);
            data.put("time", recordTime);
            data.put("title", title);

            this.model = model;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public void setData(Map<String, Object> data) {
            this.data = data;
        }

        public DeviceInfo getModel() {
            return model;
        }

        public void setModel(DeviceInfo model) {
            this.model = model;
        }
    }

}
