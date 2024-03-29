/*
 * Copyright (C) 2008 The Android Open Source Project
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

import com.android.ddmlib.Client;
import com.android.ddmlib.IDevice;

/**
 * Used for storing a window from the window manager service on the device.
 * These are the windows that the device selector shows.
 */
public class Window {
    private final String mTitle;
    private final int mHashCode;
    private final Client mClient;

    public Window(String title, int hashCode) {
        mTitle = title;
        mHashCode = hashCode;
        mClient = null;
    }

    public Window(String title, Client c) {
        mTitle = title;
        mClient = c;
        mHashCode = c.hashCode();
    }

    public String getTitle() {
        return mTitle;
    }

    public int getHashCode() {
        return mHashCode;
    }

    public String encode() {
        return Integer.toHexString(mHashCode);
    }

    @Override
    public String toString() {
        return mTitle;
    }

    public Client getClient() {
        return mClient;
    }

    /*
     * After each refresh of the windows in the device selector, the windows are
     * different instances and automatically reselecting the same window doesn't
     * work in the device selector unless the equals method is defined here.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        Window other = (Window) obj;

        if (mHashCode != other.mHashCode)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result;
        result = prime * result + mHashCode;
        return result;
    }
}
