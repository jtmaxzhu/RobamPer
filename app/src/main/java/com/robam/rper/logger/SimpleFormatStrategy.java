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
package com.robam.rper.logger;

import android.util.Log;

import com.orhanobut.logger.FormatStrategy;

/**
 * author : liuxiaohu
 * date   : 2019/8/5 15:54
 * desc   :
 * version: 1.0
 */
public class SimpleFormatStrategy implements FormatStrategy {
    // 直接通过logcat打印
    public void log(int priority, String onceOnlyTag, String message) {
        Log.println(priority, onceOnlyTag, message);
    }
}
