/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.cyborg;

import com.google.common.collect.Lists;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ViewDumpParser {
    private Map<String, Short> mIds;
    private Map<Short, Object> mStringTable;
    private List<Map<Short,Object>> mViews;

    public void parse(byte[] data) {
        Decoder d = new Decoder(ByteBuffer.wrap(data));

        mViews = Lists.newArrayListWithExpectedSize(100);
        while (d.hasRemaining()) {
            Object o = d.readObject();
            if (o instanceof Map) {
                //noinspection unchecked
                mViews.add((Map<Short, Object>) o);
            }
        }

        if (mViews.isEmpty()) {
            return;
        }

        // the last one is the property map
        mStringTable = mViews.remove(mViews.size() - 1);
        mIds = reverse(mStringTable);
    }

    public String getFirstView() {
        if (mViews.isEmpty()) {
            return null;
        }

        Map<Short, Object> props = mViews.get(0);
        Object name = getProperty(props, "meta:__name__");
        Object hash = getProperty(props, "meta:__hash__");

        if (name instanceof String && hash instanceof Integer) {
            return String.format(Locale.US, "%s@%x", name, hash);
        } else {
            return null;
        }
    }

    private Object getProperty(Map<Short, Object> props, String key) {
        return props.get(mIds.get(key));
    }

    private static Map<String, Short> reverse(Map<Short, Object> m) {
        Map<String, Short> r = new HashMap<String, Short>(m.size());

        for (Map.Entry<Short, Object> e : m.entrySet()) {
            r.put((String)e.getValue(), e.getKey());
        }

        return r;
    }

    public List<Map<Short, Object>> getViews() {
        return mViews;
    }

    public Map<String, Short> getIds() {
        return mIds;
    }

    public Short getPropertyKey(String name) {
        return mIds.get(name);
    }

    public String getPropertyName(Short key) {
        Object v = mStringTable.get(key);
        return v instanceof String ? (String) v : null;
    }
}
