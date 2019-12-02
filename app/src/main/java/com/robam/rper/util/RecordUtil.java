package com.robam.rper.util;

import com.robam.rper.display.items.base.RecordPattern;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * author : liuxiaohu
 * date   : 2019/12/2 16:04
 * desc   :
 * version: 1.0
 */
public class RecordUtil {
    private static final String TAG = "RecordUtil";

    public static File saveToFile(Map<RecordPattern, List<RecordPattern.RecordItem>> records){
        Date startTime = new Date(System.currentTimeMillis()*2);
        Date endTime = new Date(System.currentTimeMillis()/2);
        for (RecordPattern pattern:records.keySet()){
            
        }
    }

}
