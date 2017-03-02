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
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.Client;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.RawImage;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class DeviceProxy implements AndroidDebugBridge.IDeviceChangeListener,
    IShellOutputReceiver {

  static {
    AndroidDebugBridge.init(true);
    AndroidDebugBridge.createBridge();
  }

  private static DeviceProxy mInstance;
  private DeviceReadyCallback callback;
  private Set<IDevice> mConnectedDevices = new HashSet<>();

  static DeviceProxy getInstance() {
    if (mInstance == null) {
      mInstance = new DeviceProxy();
    }
    return mInstance;
  }

  private DeviceProxy() {
    AndroidDebugBridge.addDeviceChangeListener(this);
  }

  void getFirstConnectedDevice(DeviceReadyCallback callback) {
    this.callback = callback;
    if (mConnectedDevices.size() > 0) {
      this.callback.onDeviceReady((IDevice) mConnectedDevices.toArray()[0]);
    } else {
      System.err.println("Waiting for a device to connect...");
    }
  }

  @Override
  public void deviceConnected(IDevice device) {
    mConnectedDevices.add(device);
    // System.err.println("Connected " + device + ", now " + mConnectedDevices.size() + " connected devices.");
  }

  @Override
  public void deviceDisconnected(IDevice device) {
    if (mConnectedDevices.contains(device)) {
      mConnectedDevices.remove(device);
    }
    System.err.println("Disconnected " + device + ", now " + mConnectedDevices.size() + " connected devices.");
  }

  @Override
  public void deviceChanged(IDevice device, int changeMask) {
    int nClients = getFirstDevice().getClients().length;
    if (callback != null && nClients > 0) {
      handleDeviceReady();
    }
  }

  private void handleDeviceReady() {
    AndroidDebugBridge.removeDeviceChangeListener(this);
    callback.onDeviceReady(getFirstDevice());
    callback = null;
  }

  private IDevice getFirstDevice() {
    return (IDevice) mConnectedDevices.toArray()[0];
  }

  void pressHome() {
    runShellCommand("input keyevent KEYCODE_HOME");
  }

  void getDisplaySize(IShellOutputReceiver receiver) {
    try {
      getFirstDevice().executeShellCommand("wm size", receiver);
    } catch (Exception e) {
      System.err.println(e.getCause());
    }
  }

  public RawImage getScreenshot() {
    try {
      return getFirstDevice().getScreenshot();
    } catch (TimeoutException e) {
      e.printStackTrace();
    } catch (AdbCommandRejectedException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public void runShellCommand(String command) {
    try {
      getFirstDevice().executeShellCommand(command, this);
    } catch (Exception e) {
      System.err.println(e.getCause());
    }
  }

  @Override
  public void addOutput(byte[] data, int offset, int length) {}

  @Override
  public void flush() {}

  @Override
  public boolean isCancelled() { return false; }
}
