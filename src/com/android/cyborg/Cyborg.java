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

import com.android.ddmlib.IDevice;

import java.awt.Point;
import java.lang.InterruptedException;
import java.util.concurrent.TimeUnit;
import java.util.List;

public class Cyborg {

  /** Device proxy to communicate with devices. */
  private static final DeviceProxy deviceProxy;

  /** The device paired with this cyborg instance. */
  private IDevice device;

  static {
    deviceProxy = DeviceProxy.getInstance();
  }

  public Cyborg(IDevice device) {
    this.device = device;
    System.err.println("Cyborg initialized with device " + device.getSerialNumber());
  }

  public void pressHome() {
    deviceProxy.pressHome();
    onAfterUserInteraction();
  }

  public void onAfterUserInteraction() {
    try {
      TimeUnit.MILLISECONDS.sleep(500);
    } catch (InterruptedException e) {
    }
  }

  public boolean isElementWithFilterVisible(Filter filter) {
    List<Rect> rects = ViewHierarchySnapshotter.getRectsForElementsWithFilter(device, filter);
    return rects.size() > 0;
  }

  public List<Rect> getRectsForObjectsWithFilter(Filter filter) {
    return ViewHierarchySnapshotter.getRectsForElementsWithFilter(device, filter);
  }

  public void tapOnObjectWithFilter(Filter filter) {
    List<Rect> rects = ViewHierarchySnapshotter.getRectsForElementsWithFilter(device, filter);
    if (rects.size() == 0) {
      System.err.println("Not found");
    } else {
      Point toClick = rects.get(0).getCenter();
      // System.err.println("Tap on (" + toClick.x + ", " + toClick.y + ")");
      DeviceProxy.getInstance().runShellCommand("input tap " + toClick.x + " " + toClick.y);
      // Built-in half-second wait after tapping.
      onAfterUserInteraction();
    }
  }
}
