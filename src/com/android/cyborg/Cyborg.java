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

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;

import com.android.ddmlib.RawImage;
import com.android.ddmlib.TimeoutException;
import java.awt.Point;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.lang.InterruptedException;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

public class Cyborg {

  /** Device proxy to communicate with devices. */
  private static final DeviceProxy deviceProxy;

  /** The device paired with this cyborg instance. */
  private CyborgDevice device;

  static {
    deviceProxy = DeviceProxy.getInstance();
  }

  public Cyborg(IDevice device) {
    this.device = new CyborgDevice(device);
    getDeviceDisplaySize();
  }

  private void getDeviceDisplaySize() {
    deviceProxy.getDisplaySize(new IShellOutputReceiver() {
      @Override
      public void addOutput(byte[] data, int offset, int length) {
        String[] spacedPieces = new String(data).trim().split(" ");
        Cyborg.this.device.displayWidth =
            Integer.parseInt(spacedPieces[spacedPieces.length - 1].split("x")[0]);
        Cyborg.this.device.displayHeight =
            Integer.parseInt(spacedPieces[spacedPieces.length - 1].split("x")[1]);
        System.err.println("Found device " + device.getSerialNumber());
      }
      @Override
      public void flush() {}
      @Override
        public boolean isCancelled() { return false; }
      });
  }

  public void pressHome() {
    deviceProxy.pressHome();
    onAfterUserInteraction();
  }

  public void pressKeyWithCode(int keyCode) {
    DeviceProxy.getInstance().runShellCommand("input keyevent " + keyCode);
    onAfterUserInteraction();
  }

  public void pressKeyWithCode(int keyCode, int waitTime) {
    DeviceProxy.getInstance().runShellCommand("input keyevent " + keyCode);
    onAfterUserInteraction(waitTime);
  }

  public RawImage getScreenshot() {
    return DeviceProxy.getInstance().getScreenshot();
  }


  public void wait(int milliseconds) {
    try {
      TimeUnit.MILLISECONDS.sleep(milliseconds);
    } catch (InterruptedException e) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      new RuntimeException("").printStackTrace(pw);
      System.err.println(sw.toString());
    }
  }

  public void onAfterUserInteraction() {
    onAfterUserInteraction(300);
  }

  public void onAfterUserInteraction(int waitTime) {
    this.wait(waitTime);
  }

  public boolean isElementWithFilterVisible(Filter filter) {
    List<ViewNode> nodes = ViewHierarchySnapshotter.getNodesForFilter(device, filter);
    return nodes.size() > 0;
  }

  public List<ViewNode> getNodesForObjectsWithFilter(Filter filter) {
    return ViewHierarchySnapshotter.getNodesForFilter(device, filter);
  }

  public List<Rect> getRectsForObjectsWithFilter(Filter filter) {
    List<Rect> rects = new ArrayList<>();
    List<ViewNode> nodes = ViewHierarchySnapshotter.getNodesForFilter(device, filter);
    for (ViewNode node : nodes) {
      rects.add(ViewHierarchySnapshotter.findVisibleRect(node));
    }
    return rects;
  }

  public static Rect getRectForNode(ViewNode node) {
    return ViewHierarchySnapshotter.findVisibleRect(node);
  }

  public void tapOnRect(Rect rect) {
    Point toClick = rect.getCenter();
    // System.err.println("Tap on (" + toClick.x + ", " + toClick.y + ")");
    DeviceProxy.getInstance().runShellCommand("input tap " + toClick.x + " " + toClick.y);
    // Built-in half-second wait after tapping.
    onAfterUserInteraction();
  }

  public void dragAndDrop(Rect src, Rect dest, int time) {
    DeviceProxy.getInstance().runShellCommand("input draganddrop " + src.getCenter().x + " " +
        src.getCenter().y + " " + dest.getCenter().x + " " + dest.getCenter().y + " " + time);
  }

  public void runShellCommand(String command) {
    deviceProxy.runShellCommand(command);
  }
}
