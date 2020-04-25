/*
 * Copyright (C) 2015-present, Ant Financial Services Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.robam.rper.display.items;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;

import com.robam.rper.R;
import com.robam.rper.activity.MyApplication;
import com.robam.rper.annotation.Param;
import com.robam.rper.bean.ProcessInfo;
import com.robam.rper.display.items.base.DisplayItem;
import com.robam.rper.display.items.base.Displayable;
import com.robam.rper.display.items.base.FixedLengthCircularArray;
import com.robam.rper.display.items.base.RecordPattern;
import com.robam.rper.injector.InjectorService;
import com.robam.rper.injector.param.SubscribeParamEnum;
import com.robam.rper.injector.param.Subscriber;
import com.robam.rper.util.LogUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * dalvik：是指dalvik所使用的内存。
 * native：是被native堆使用的内存。应该指使用C\C++在堆上分配的内存。
 * other:是指除dalvik和native使用的内存。但是具体是指什么呢？至少包括在C\C++分配的非堆内存，比如分配在栈上的内存。puzlle!
 * private:是指私有的。非共享的。
 * share:是指共享的内存。
 * PSS：实际使用的物理内存（比例分配共享库占用的内存）
 * Pss：它是把共享内存根据一定比例分摊到共享它的各个进程来计算所得到进程使用内存。网上又说是比例分配共享库占用的内存，那么至于这里的共享是否只是库的共享，还是不清楚。
 * PrivateDirty：它是指非共享的，又不能换页出去（can not be paged to disk ）的内存的大小。比如Linux为了提高分配内存速度而缓冲的小对象，即使你的进程结束，该内存也不会释放掉，它只是又重新回到缓冲中而已。
 * SharedDirty:参照PrivateDirty我认为它应该是指共享的，又不能换页出去（can not be paged to disk ）的内存的大小。比如Linux为了提高分配内存速度而缓冲的小对象，即使所有共享它的进程结束，该内存也不会释放掉，它只是又重新回到缓冲中而已
 */
@DisplayItem(name = "内存", icon = R.drawable.mem)
public class MemTools implements Displayable {
	private static String TAG = "MemoryTools";

	private InjectorService injectorService;
	private Context context;
	private ActivityManager activityManager;
	private Long totalMeory = null;
	private ProcessInfo pid = null;
	private List<ProcessInfo> pids = null;

	private static Map<String, FixedLengthCircularArray<RecordPattern.RecordItem>> pssCachedData;
	private static Map<String, FixedLengthCircularArray<RecordPattern.RecordItem>> pDirtyCachedData;

	@Subscriber(@Param(SubscribeParamEnum.PID))
	public void setPid(ProcessInfo pid){
		if (pid != null && (this.pid == null || pid.getPid() != this.pid.getPid())){
			this.pid = pid;
		}
	}

	@Subscriber(@Param(SubscribeParamEnum.PID_CHILDREN))
	public void setPids(List<ProcessInfo> pids){
			this.pids = pids;
	}


	@Override
	public void start() {
		injectorService = MyApplication.getInstance().findServiceByName(InjectorService.class.getName());
		injectorService.register(this);
		this.context = MyApplication.getContext();
		activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
	}

	@Override
	public void stop() {
		injectorService.unregister(this);
		injectorService = null;
	}

	@Override
	public String getCurrentInfo() throws Exception {
		if (totalMeory == null){
			totalMeory = getTotalMemory();
		}
		long l = Runtime.getRuntime().maxMemory()/(1024*1024);
		if (pid != null && pid.getPid()>0){
			Debug.MemoryInfo[] memoryInfos = activityManager.getProcessMemoryInfo(new int[]{pid.getPid()});
			if (memoryInfos != null && memoryInfos.length>0){
				Debug.MemoryInfo info = memoryInfos[0];
				return String.format(Locale.CHINA,"pss:%.2fMB/privateDirty:%.2fMB", info.getTotalPss() / 1024f, info.getTotalPrivateDirty() / 1024f);
			}
		}
		return "可用内存:"+ getAvailMemory(context) + "MB/总内存:" + totalMeory + "MB";
	}

	//获取应用可用内存的大小
	public static Long getAvailMemory(Context context) {
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		if (context == null){
			return 0L;
		}
		ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
		activityManager.getMemoryInfo(info);
		//1M=1024*1024 BYTE
		return info.availMem/(1024*1024);

	}


	/**
	 * 获取设备总的内存数据
	 * @return
	 */
	private Long getTotalMemory() {
		if (activityManager == null){
			return 0L;
		}
		ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
		activityManager.getMemoryInfo(info);
		//1M=1024*1024 BYTE
		return info.totalMem/(1024*1024);
	}

	@Override
	public long getRefreshFrequency() {
		return 10;
	}

	@Override
	public void clear() {

	}


	private static List<RecordPattern.RecordItem> usedMemory;
	private static Map<String, ArrayList<RecordPattern.RecordItem>[]> appMemory;
	private static Long startTime = 0L;
	@Override
	public void startRecord() {
		startTime = System.currentTimeMillis();
		usedMemory = new ArrayList<>();
		appMemory = new HashMap<>();
		pssCachedData = new HashMap<>();
		pDirtyCachedData = new HashMap<>();
	}


	private static int cacheLength = 10;
	@Override
	public void record() {
		if (pids != null && pids.size() > 0 && pid != null) {
			int[] realPids = new int[pids.size()];
			String[] processName = new String[pids.size()];
			for (int i = 0; i < pids.size(); i++) {
				realPids[i] = pids.get(i).getPid();
				processName[i] = pids.get(i).getProcessName() + "-" + realPids[i];
			}
			Debug.MemoryInfo[] memoryInfos = activityManager.getProcessMemoryInfo(realPids);
			if (memoryInfos != null && memoryInfos.length == pids.size()) {
				for (int i = 0; i < realPids.length; i++) {
					int pid = realPids[i];
					Debug.MemoryInfo pidMem = memoryInfos[i];


					FixedLengthCircularArray<RecordPattern.RecordItem> pssCache;
					FixedLengthCircularArray<RecordPattern.RecordItem> privcateDirtyCache;

					if ((pssCache = pssCachedData.get(processName[i])) == null) {
						pssCache = new FixedLengthCircularArray<>(cacheLength);
						pssCachedData.put(processName[i], pssCache);
					}

					if ((privcateDirtyCache = pDirtyCachedData.get(processName[i])) == null) {
						pssCache = new FixedLengthCircularArray<>(cacheLength);
						pDirtyCachedData.put(processName[i], privcateDirtyCache);
					}

					//实时数据
					RecordPattern.RecordItem[] record = new RecordPattern.RecordItem[]{
							new RecordPattern.RecordItem(System.currentTimeMillis(), pidMem.getTotalPss() / 1024f, ""),
							new RecordPattern.RecordItem(System.currentTimeMillis(), pidMem.getTotalPrivateDirty() / 1024f, "")};
					//如果是pid未发生变化
					if (pid == this.pid.getPid()) {
						//要保存的记录\
						ArrayList<RecordPattern.RecordItem>[] saveRecord;
						if ((saveRecord = appMemory.get(processName[i])) == null) {
							saveRecord = new ArrayList[]{new ArrayList<RecordPattern.RecordItem>(), new ArrayList<RecordPattern.RecordItem>()};
							appMemory.put(processName[i], saveRecord);
						}
						saveRecord[0].add(record[0]);
						saveRecord[1].add(record[1]);
					}
				}
			}
		}
		if (totalMeory == null){
			totalMeory = getTotalMemory();
		}
		usedMemory.add(new RecordPattern.RecordItem(System.currentTimeMillis(), (float)(totalMeory - getAvailMemory(context)), ""));
	}

	@Override
	public void trigger() {

	}

	@Override
	public Map<RecordPattern, List<RecordPattern.RecordItem>> stopRecord() {
		Long endTime = System.currentTimeMillis();
		Map<RecordPattern, List<RecordPattern.RecordItem>> result = new HashMap<>();
		RecordPattern pattern = new RecordPattern("系统占用", "MB", "Memory");
		pattern.setEndTime(endTime);
		pattern.setStartTime(startTime);
		result.put(pattern, usedMemory);

		for (Map.Entry<String, ArrayList<RecordPattern.RecordItem>[]> entry : appMemory.entrySet()) {
			//取出各个进程的数据记录
			ArrayList<RecordPattern.RecordItem>[] pidRecord = entry.getValue();
			pattern = new RecordPattern("PSS内存-" + entry.getKey(), "MB", "Memory");
			pattern.setStartTime(startTime);
			pattern.setEndTime(endTime);
			result.put(pattern, pidRecord[0]);
			pattern = new RecordPattern("PrivateDirty内存-" + entry.getKey(), "MB", "Memory");
			pattern.setStartTime(startTime);
			pattern.setEndTime(endTime);
			result.put(pattern, pidRecord[1]);
		}
		usedMemory = null;
		appMemory.clear();
		pssCachedData.clear();
		pDirtyCachedData.clear();
		return result;
	}

	private static native int fillMemory(int memory);

	private static native int releaseMemory();
}
