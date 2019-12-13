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

import com.robam.rper.R;
import com.robam.rper.display.items.base.DisplayItem;
import com.robam.rper.display.items.base.Displayable;
import com.robam.rper.display.items.base.RecordPattern;

import java.util.List;
import java.util.Map;

@DisplayItem(name = "网络", trigger = "清零", icon = R.drawable.net)
public class NetTools implements Displayable {


	@Override
	public void start() {

	}

	@Override
	public void stop() {

	}

	@Override
	public String getCurrentInfo() throws Exception {
		return null;
	}

	@Override
	public long getRefreshFrequency() {
		return 0;
	}

	@Override
	public void clear() {

	}

	@Override
	public void startRecord() {

	}

	@Override
	public void record() {

	}

	@Override
	public void trigger() {

	}

	@Override
	public Map<RecordPattern, List<RecordPattern.RecordItem>> stopRecord() {
		return null;
	}
}
