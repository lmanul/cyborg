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

import com.android.cyborg.DeviceProxy;
import com.android.cyborg.DeviceReadyCallback;
import com.android.ddmlib.IDevice;

// import java.lang.reflect.Class;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class CyborgTest {


  public static void runTests(CyborgTest testObject) {
    Class clazz = testObject.getClass();
    Method[] m = clazz.getDeclaredMethods();
    List<Method> testMethods = new ArrayList<>();
    for (int i = 0; i < m.length; i++) {
      String methodFullName = m[i].toString();
      String[] dotSeparated = methodFullName.split("[.]");
      if (dotSeparated[dotSeparated.length - 1].startsWith("test")) {
        testMethods.add(m[i]);
      }
    }
    if (testMethods.size() == 0) {
      System.err.println("No test methods detected.");
      System.exit(0);
    }

    for (Method testMethod : testMethods) {
      try {
        testMethod.invoke(testObject);
      } catch (Exception e) {
        System.err.println("Caught exception trying to run test " + e.getCause());
      }
    }
    System.err.println("All done.");
    System.exit(0);
  }

  public static void init(final CyborgTest testObject) {
    DeviceProxy.getInstance().getFirstConnectedDevice(new DeviceReadyCallback() {
      @Override
      public void onDeviceReady(IDevice device) {
        // Cyborg cyborg = new Cyborg(device);
        runTests(testObject);
      }
    });
  }
}
