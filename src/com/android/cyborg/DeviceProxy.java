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
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class DeviceProxy implements AndroidDebugBridge.IDeviceChangeListener,
    IShellOutputReceiver {

  static {
    AndroidDebugBridge.init(true);
  }

  private static DeviceProxy mInstance;

  AndroidDebugBridge mAdb = AndroidDebugBridge.createBridge();
  Set<IDevice> mConnectedDevices = new HashSet<>();

  public static DeviceProxy getInstance() {
    if (mInstance == null) {
      mInstance = new DeviceProxy();
    }
    return mInstance;
  }

  private DeviceProxy() {
    mAdb.addDeviceChangeListener(this);
  }

  public IDevice getConnectedDevice() {
    // Assume we want to get the first connected device we find.
    if (mConnectedDevices.size() < 1) {
      System.err.println("No device connected.");
      return null;
    }
    return (IDevice) mConnectedDevices.toArray()[0];
  }

  @Override
  public void deviceConnected(IDevice device) {
    mConnectedDevices.add(device);
    System.err.println("Connected " + device + ", now " + mConnectedDevices.size() + " connected devices.");
    System.err.println("Found one device to work with, stop listening.\n\n");
    mAdb.removeDeviceChangeListener(this);
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
    // Don't do anything.
  }

  public Client[] getAllClients() {
    return getFirstDevice().getClients();
  }

  public IDevice getFirstDevice() {
    return (IDevice) mConnectedDevices.toArray()[0];
  }

  public void runShellCommand(String command) {
    try {
      getFirstDevice().executeShellCommand(command, this);
    } catch (IOException e) {
      System.err.println(e.getCause());
    } catch (TimeoutException e) {
      System.err.println(e.getCause());
    } catch (AdbCommandRejectedException e) {
      System.err.println(e.getCause());
    } catch (ShellCommandUnresponsiveException e) {
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
