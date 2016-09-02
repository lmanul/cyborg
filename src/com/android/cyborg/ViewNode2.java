/*
 * Copyright (C) 2016 The Android Open Source Project
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

import java.util.Map;

public class ViewNode2 extends ViewNode {
    ViewNode2(Window window, ViewNode parent) {
        super(window, parent);
    }

    public void initialize(Map<Short, Object> viewProperties,
            ViewDumpParser parser) {
        Object v = viewProperties.get(parser.getPropertyKey("meta:__name__"));
        if (v instanceof String) {
            name = (String)v;
        }
        v = viewProperties.get(parser.getPropertyKey("meta:__hash__"));
        if (v instanceof Integer) {
            hashCode = Integer.toHexString((Integer) v);
        }

        id = "unknown";
        width = height = 10;
        measureTime = layoutTime = drawTime = -1;

        loadProperties(viewProperties, parser);
    }

    private void loadProperties(Map<Short, Object> viewProperties, ViewDumpParser parser) {
        for (Map.Entry<Short, Object> p : viewProperties.entrySet()) {
            ViewNode.Property property = new ViewNode.Property();
            property.name = parser.getPropertyName(p.getKey());
            Object v = p.getValue();
            property.value = v != null ? v.toString() : "";

            properties.add(property);
            namedProperties.put(property.name, property);
        }

        id = namedProperties.containsKey("id") ? namedProperties.get("id").value : "unknown";
        left = getInt("layout:left", 0);
        top = getInt("layout:top", 0);
        width = getInt("layout:width", 0);
        height = getInt("layout:height", 0);
        scrollX = getInt("layout:scrollX", 0);
        scrollY = getInt("layout:scrollY", 0);

        paddingLeft = getInt("padding:paddingLeft", 0);
        paddingRight = getInt("padding:paddingRight", 0);
        paddingTop = getInt("padding:paddingTop", 0);
        paddingBottom = getInt("padding:paddingBottom", 0);

        marginLeft = getInt("layout_leftMargin", Integer.MIN_VALUE);
        marginRight = getInt("layout_rightMargin", Integer.MIN_VALUE);
        marginTop = getInt("layout_topMargin", Integer.MIN_VALUE);
        marginBottom = getInt("layout_bottomMargin", Integer.MIN_VALUE);

        baseline = getInt("layout:baseline", 0);
        willNotDraw = getBoolean("drawing:willNotDraw", false);
        hasFocus = getBoolean("focus:hasFocus", false);

        hasMargins =
                marginLeft != Integer.MIN_VALUE && marginRight != Integer.MIN_VALUE
                        && marginTop != Integer.MIN_VALUE && marginBottom != Integer.MIN_VALUE;

        for (String name : namedProperties.keySet()) {
            int index = name.indexOf(':');
            if (index != -1) {
                categories.add(name.substring(0, index));
            }
        }
        if (categories.size() != 0) {
            categories.add(MISCELLANIOUS);
        }
    }
}

