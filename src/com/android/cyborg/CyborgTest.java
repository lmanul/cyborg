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

import java.io.StringWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

public class CyborgTest {

  private CyborgTestMethod currentTestMethod;
  protected Cyborg cyborg;

  public void setCyborg(Cyborg cyborg) {
    this.cyborg = cyborg;
  }

  public List<Rect> getRectsForObjectsWithFilter(Filter filter) {
    return cyborg.getRectsForObjectsWithFilter(filter);
  }

  private void tapOnRect(Rect rect) {
    cyborg.tapOnRect(rect);
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
    tapOnRect(rects.get(0));
  }

  public void dragAndDrop(Rect src, Rect dest, int time) {
    cyborg.dragAndDrop(src, dest, time);
  }

  public void waitUntilObjectIsVisible(Filter filter, int timeout) {
    waitUntilObjectIsVisible(filter, timeout, true);
  }

  public void waitUntilObjectIsHidden(Filter filter, int timeout) {
    waitUntilObjectIsVisible(filter, timeout, false);
  }

  private void waitUntilObjectIsVisible(Filter filter, int timeout, boolean visible) {
    if ((visible && hasVisibleObjectWithFilter(filter)) ||
        (!visible && !hasVisibleObjectWithFilter(filter))) {
      return;
    }
    int minStepTime = 100;
    int steps = 10;
    int stepTime = timeout / steps;
    if (stepTime < minStepTime) {
      steps = timeout / minStepTime;
      stepTime = minStepTime;
    }
    for (int i = steps; i > 0; i--) {
      cyborg.wait(stepTime);
      if ((visible && hasVisibleObjectWithFilter(filter)) ||
          (!visible && !hasVisibleObjectWithFilter(filter))) {
        break;
      }
    }
    if ((visible && !hasVisibleObjectWithFilter(filter)) ||
        (!visible && hasVisibleObjectWithFilter(filter))) {
      fail("Timed out waiting for object to " + (visible ? "" : "dis") + "appear: " + filter);
    }
  }

  private boolean hasVisibleObjectWithFilter(Filter filter) {
    return cyborg.isElementWithFilterVisible(filter);
  }

  public String getTextForObjectWithFilter(Filter filter) {
    List<ViewNode> nodes = cyborg.getNodesForObjectsWithFilter(filter);
    if (nodes.size() != 1) {
      fail("Was expecting exactly one object, but found " + nodes.size());
      return null;
    }
    ViewNode node = nodes.get(0);
    if (!node.namedProperties.containsKey("text:text")) {
      return null;
    }
    return node.namedProperties.get("text:text").value;
  }

  public String getContentDescriptionForObjectWithFilter(Filter filter) {
    List<ViewNode> nodes = cyborg.getNodesForObjectsWithFilter(filter);
    if (nodes.size() != 1) {
      fail("Was expecting exactly one object, but found " + nodes.size());
      return null;
    }
    ViewNode node = nodes.get(0);
    if (!node.namedProperties.containsKey("accessibility:contentDescription")) {
      return null;
    }
    return node.namedProperties.get("accessibility:contentDescription").value;
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

  public void pressKeyWithCode(int keyCode, int waitTime) {
    cyborg.pressKeyWithCode(keyCode, waitTime);
  }

  public void wait(int milliseconds) {
    cyborg.wait(milliseconds);
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
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    new RuntimeException("").printStackTrace(pw);
    System.err.println(sw.toString());
  }

  private void runTests(CyborgTest testObject) {
    Class clazz = testObject.getClass();
    Method[] m = clazz.getDeclaredMethods();
    List<CyborgTestMethod> testMethods = new ArrayList<>();
    CyborgTestMethod soloTestMethod = null;
    Method setUp = null, tearDown = null;
    int longestMethodNameLength = 0;
    for (int i = 0; i < m.length; i++) {
      String methodFullName = m[i].toString();
      String[] dotSeparated = methodFullName.split("[.]");
      String lastPart = dotSeparated[dotSeparated.length - 1];
      String methodName = lastPart.substring(0, lastPart.length() - 2);
      if (methodName.startsWith("test")) {
        longestMethodNameLength = java.lang.Math.max(longestMethodNameLength, methodName.length());
        testMethods.add(new CyborgTestMethod(m[i], methodName));
      }
      if (methodName.startsWith("solotest")) {
        soloTestMethod = new CyborgTestMethod(m[i], methodName.substring(4));
      }
      if (lastPart.equals("setUp()")) {
        setUp = m[i];
      }
      if (lastPart.equals("tearDown()")) {
        tearDown = m[i];
      }
    }
    if (testMethods.size() == 0 && soloTestMethod == null) {
      System.err.println("No test methods detected.");
    }

    if (soloTestMethod != null) {
      testMethods = new ArrayList<>();
      testMethods.add(soloTestMethod);
    }
    // Collections.sort(testMethods);
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
        System.err.println("Caught exception trying to run test");
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        System.err.println(sw.toString());
      }
    }
    int passed = 0;
    for (CyborgTestMethod method : testMethods) {
      passed += (method.status == CyborgTestMethod.Status.PASS) ? 1 : 0;
    }
    boolean plural = testMethods.size() > 1;
    System.err.println("\n" + passed + " of " + testMethods.size() +
        " test" + (plural ? "s" : "") + " passed.");
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

  private static class CyborgTestMethod implements Comparable<CyborgTestMethod> {

    private enum Status {
      PASS, FAIL;
    }

    final Method method;
    final String name;
    Status status;

    CyborgTestMethod(Method method, String name) {
      this.method = method;
      this.name = name;
      this.status = Status.PASS;
    }

    @Override
    public int compareTo(CyborgTestMethod other) {
      return this.name.compareTo(other.name);
    }
  }
}
