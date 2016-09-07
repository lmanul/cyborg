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

  public List<Rect> getRectsForObjectsWithFilter(Filter filter) {
    return cyborg.getRectsForObjectsWithFilter(filter);
  }

  public void tapOnObjectWithFilter(Filter filter) {
    List<Rect> rects = cyborg.getRectsForObjectsWithFilter(filter);
    if (rects.size() == 0) {
      fail("Can't find object to tap on for " + filter);
      return;
    } else if (rects.size() > 1) {
      fail("Found " + rects.size() + " objects to tap on for " + filter);
      int i = 1;
      for (Rect rect : rects) {
        System.err.println("Object " + (i++) + " located at " + rect);
      }
      return;
    }
    cyborg.tapOnRect(rects.get(0));
  }

  public boolean hasVisibleObjectWithFilter(Filter filter) {
    return cyborg.isElementWithFilterVisible(filter);
  }

  public void assertTrue(boolean condition) {
    assertTrue(null, condition);
  }

  public void assertTrue(String message, boolean condition) {
    if (message == null || message.isEmpty()) {
      message = "Called assertTrue with false argument.";
    }
    if (!condition) {
      fail(message);
    }
  }

  public void assertFalse(boolean condition) {
    assertFalse(null, condition);
  }

  public void assertFalse(String message, boolean condition) {
    if (message == null || message.isEmpty()) {
      message = "Called assertFalse with true argument.";
    }
    if (condition) {
      fail(message);
    }
  }

  public void pressHome() {
    cyborg.pressHome();
  }

  public void pressKeyWithCode(int keyCode) {
    cyborg.pressKeyWithCode(keyCode);
  }

  public void setUp() {
    // Subclasses will override.
  }

  public void tearDown() {
    // Subclasses will override.
  }

  public void fail(String message) {
    if (message != null) {
      System.err.println(message);
    }
    currentTestMethod.status = CyborgTestMethod.Status.FAIL;
  }

  public void runTests(CyborgTest testObject) {
    Class clazz = testObject.getClass();
    Method[] m = clazz.getDeclaredMethods();
    List<CyborgTestMethod> testMethods = new ArrayList<>();
    Method setUp = null, tearDown = null;
    int longestMethodNameLength = 0;
    for (int i = 0; i < m.length; i++) {
      String methodFullName = m[i].toString();
      String[] dotSeparated = methodFullName.split("[.]");
      String lastPart = dotSeparated[dotSeparated.length - 1];
      if (lastPart.startsWith("test")) {
        String methodName = lastPart.substring(0, lastPart.length() - 2);
        longestMethodNameLength = java.lang.Math.max(longestMethodNameLength, methodName.length());
        testMethods.add(new CyborgTestMethod(m[i], methodName));
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

    System.err.println("\n");
    for (CyborgTestMethod testMethod : testMethods) {
      currentTestMethod = testMethod;
      try {
        StringBuilder sb = new StringBuilder(testMethod.name + "...");
        for (int i = testMethod.name.length(); i <= longestMethodNameLength; i++) {
          sb.append(" ");
        }
        System.err.print(sb.toString());
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
    System.err.println("\nAll done.");
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

  public void init() {
    DeviceProxy.getInstance().getFirstConnectedDevice(new DeviceReadyCallback() {
      @Override
      public void onDeviceReady(IDevice device) {
        CyborgTest.this.setCyborg(new Cyborg(device));
        runTests(CyborgTest.this);
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
