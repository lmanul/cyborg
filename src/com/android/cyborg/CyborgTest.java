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

  private Cyborg cyborg;
  private CyborgTestMethod currentTestMethod;

  public void setCyborg(Cyborg cyborg) {
    this.cyborg = cyborg;
  }

  public void tapOnObjectWithId(String id) {
    cyborg.tapOnObjectWithId(id);
  }

  public boolean hasVisibleObjectWithId(String id) {
    return cyborg.isElementWithIdVisible(id);
  }

  public void assertVisibleObjectWithId(String id) {
    if (!hasVisibleObjectWithId(id)) {
      fail();
    }
  }

  public void setUp() {
    // Subclasses will override.
  }

  public void tearDown() {
    // Subclasses will override.
  }

  public void fail() {
    currentTestMethod.status = CyborgTestMethod.Status.FAIL;
  }

  public void runTests(CyborgTest testObject) {
    Class clazz = testObject.getClass();
    Method[] m = clazz.getDeclaredMethods();
    List<CyborgTestMethod> testMethods = new ArrayList<>();
    Method setUp = null, tearDown = null;
    for (int i = 0; i < m.length; i++) {
      String methodFullName = m[i].toString();
      String[] dotSeparated = methodFullName.split("[.]");
      String lastPart = dotSeparated[dotSeparated.length - 1];
      if (lastPart.startsWith("test")) {
        testMethods.add(new CyborgTestMethod(m[i], lastPart.substring(0, lastPart.length() - 2)));
      }
      if (lastPart.equals("setUp()")) {
        setUp = m[i];
      }
      if (lastPart.equals("tearDown()")) {
        tearDown = m[i];
      }
    }
    if (testMethods.size() == 0) {
      System.err.println("No test methods detected.");
    }

    for (CyborgTestMethod testMethod : testMethods) {
      currentTestMethod = testMethod;
      try {
        System.err.print(testMethod.name + "... ");
        if (setUp != null) {
          setUp.invoke(testObject);
        }
        testMethod.method.invoke(testObject);
        if (tearDown != null) {
          tearDown.invoke(testObject);
        }
        if (currentTestMethod.status == CyborgTestMethod.Status.PASS) {
          printPass();
        } else {
          printFail();
        }
        System.err.println("");
      } catch (Exception e) {
        System.err.println("Caught exception trying to run test " + e.getCause());
      }
    }
    System.err.println("All done.");
    System.exit(0);
  }

  private static void printPass() {
    // Funny escape char business to get some color.
    System.err.print((char)27 + "[32mPASS" + (char)27 + "[0m");
  }

  private static void printFail() {
    // Funny escape char business to get some color.
    System.err.print((char)27 + "[31mFAIL" + (char)27 + "[0m");
  }

  public void init(final CyborgTest testObject) {
    DeviceProxy.getInstance().getFirstConnectedDevice(new DeviceReadyCallback() {
      @Override
      public void onDeviceReady(IDevice device) {
        testObject.setCyborg(new Cyborg(device));
        runTests(testObject);
      }
    });
  }

  private static class CyborgTestMethod {

    private enum Status {
      PASS, FAIL;
    }

    public final Method method;
    public final String name;
    public Status status;

    CyborgTestMethod(Method method, String name) {
      this.method = method;
      this.name = name;
      this.status = Status.PASS;
    }
  }
}
