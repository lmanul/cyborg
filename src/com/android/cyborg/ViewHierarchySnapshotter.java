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

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.Client;
import com.android.ddmlib.ClientData;
import com.android.ddmlib.HandleViewDebug;
import com.android.ddmlib.IDevice;

import com.google.common.collect.Lists;
import com.google.common.collect.SortedMultiset;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ViewHierarchySnapshotter {

  private static final String LOCATION_ON_SCREEN_X_KEY = "layout:getLocationOnScreen_x()";
  private static final String LOCATION_ON_SCREEN_Y_KEY = "layout:getLocationOnScreen_y()";
  private static final String MEASURED_WIDTH_KEY = "measurement:mMeasuredWidth";
  private static final String MEASURED_HEIGHT_KEY = "measurement:mMeasuredHeight";

  public static List<Rect> getRectsForElementsWithId(IDevice device, final String searchString) {
    // System.err.println("Searching view hierarchy for " + searchString + " on device " + device.getSerialNumber() + "...");
    Client[] allClients = device.getClients();
    ExecutorService executorService = Executors.newFixedThreadPool(10);
    List<Callable<List<Rect>>> callables = new ArrayList<>();

    for (int i = 0; i < allClients.length; i++) {
      final Client c = allClients[i];
      ClientData cd = c.getClientData();
      if (cd.hasFeature(ClientData.FEATURE_VIEW_HIERARCHY)) {
        try {
          List<String> windowTitles = new ListViewRootsHandler().getWindows(c, 5, TimeUnit.SECONDS);
          for (final String windowTitle : windowTitles) {
            callables.add(new HierarchyExplorerCallable(new Window(windowTitle, c), searchString));
          }
        } catch (IOException ignored) { }
      } else {
        // System.err.println("No view hierarchy feature");
      }
    }

    List<Rect> foundRects = new ArrayList<>();
    try {
      List<Future<List<Rect>>> tasks = executorService.invokeAll(callables);

      for (Future<List<Rect>> rectList : tasks) {
        foundRects.addAll(rectList.get());
      }
    } catch (InterruptedException e) {
      System.err.println("InterruptedException: " + e.getCause());
    } catch (ExecutionException e) {
      System.err.println("ExecutionException: " + e.getCause());
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      System.err.println(sw.toString());
    }

    // System.err.println("Finished searching, found " + foundRects.size() + " elements.");
    
    executorService.shutdown();

    return foundRects;
  }

  private static class HierarchyExplorerCallable implements Callable<List<Rect>> {
    private final String searchId;
    private final Client client;
    private final String windowTitle;
    private final List<Rect> foundEls = new ArrayList<>();

    public  HierarchyExplorerCallable(Window window, String searchId) {
      this.client = window.getClient();
      this.windowTitle = window.getTitle();
      this.searchId = searchId;
    }

    public List<Rect> call() {
      ViewNode root = loadWindowData(20, TimeUnit.SECONDS, new Window(windowTitle, client));
      recursivelySearchForId(root, searchId);
      return foundEls;
    }

    public void recursivelySearchForId(ViewNode root, String searchId) {
      if (root == null) {
        return;
      }
      if (root.id != null && root.id.equals("id/" + searchId)) {
        //System.err.println("\nFound " + root + " with parent " + root.parent + "\n");
        //System.err.println("" + root.left + ":" + root.top + ":" + root.width + ":" + root.height);
        ViewNode currentParent = root.parent;
        int globalX = root.left;
        int globalY = root.top;
        while (currentParent != null) {
          float translationX = Float.parseFloat(currentParent.namedProperties.get("drawing:translationX").value);
          float translationY = Float.parseFloat(currentParent.namedProperties.get("drawing:translationY").value);

          globalX += currentParent.left;
          globalY += currentParent.top;
          globalX += translationX;
          globalY += translationY;
          currentParent = currentParent.parent;
        }
        int x = 0, y = 0, width = 10, height = 10;
        //System.err.println("Coords: " + globalX + "," + globalY + " " + root.width + "x" + root.height + "\n");
        foundEls.add(new Rect(globalX, globalY, root.width, root.height));
      } else {
        for (int i = 0; i < root.children.size(); i++) {
          recursivelySearchForId(root.children.get(i), searchId);
        }
      }
    }
  }

  /**
   * Byte array representing the view hierachy dump of the window.
   */
  public static ViewNode loadWindowData(long timeout, TimeUnit unit, Window window) {
    Client client = window.getClient();
    if (client == null) {
      return null;
    }
    String title = window.getTitle();
    CaptureByteArrayHandler handler = new CaptureByteArrayHandler(HandleViewDebug.CHUNK_VURT);

    try {
      HandleViewDebug.dumpViewHierarchy(
        client, title,
        false /* skipChildren */,
        true  /* includeProperties */,
        handler);
    } catch (IOException e) {
      System.err.println("IOException while dumping view hierarchy: " + e.getCause());
      return null;
    }

    byte[] data = handler.getData(30, TimeUnit.SECONDS);

    ViewNode viewNode = parseViewHierarchy(data, window);

    ViewDumpParser parser = new ViewDumpParser();
    parser.parse(data);
    List<Map<Short, Object>> views = parser.getViews();
    return viewNode;
  }

  private static ViewNode parseViewHierarchy(byte[] data, Window window) {
    if (isEncoded(data)) {
      ViewDumpParser parser = new ViewDumpParser();
      parser.parse(data);
      if (parser.getViews().isEmpty()) {
        return null;
      }
      return parseViewHierarchy(window, null, parser.getViews().get(0), parser);
    } else {
      String viewHierarchy = new String(data, Charset.forName("UTF-8"));
      return parseViewHierarchy(
          new BufferedReader(new StringReader(viewHierarchy)), window);
    }
  }

  private static ViewNode parseViewHierarchy(Window window,
      ViewNode parent,
      Map<Short, Object> viewProps,
      ViewDumpParser parser) {
    ViewNode root = ViewNode.create(window, parent, viewProps, parser);
    Object v = viewProps.get(parser.getPropertyKey("meta:__childCount__"));
    int n = 0;
    if (v instanceof Integer) {
      n = (Integer) v;
    } else if (v instanceof Short) {
      n = (Short) v;
    }

    for (int i = 0; i < n; i++) {
      Map<Short, Object> childViewProps = (Map<Short, Object>) viewProps
          .get(parser.getPropertyKey("meta:__child__" + i));
      parseViewHierarchy(window, root, childViewProps, parser);
    }

    return root;
  }

  public static ViewNode parseViewHierarchy(BufferedReader in, Window window) {
    ViewNode currentNode = null;
    int currentDepth = -1;
    String line;
    try {
      while ((line = in.readLine()) != null) {
        if ("DONE.".equalsIgnoreCase(line)) {
          break;
        }
        int depth = 0;
        while (line.charAt(depth) == ' ') {
          depth++;
        }
        while (depth <= currentDepth) {
          if (currentNode != null) {
            currentNode = currentNode.parent;
          }
          currentDepth--;
        }
        currentNode = ViewNode.create(window, currentNode, line.substring(depth));
        currentDepth = depth;
      }
    } catch (IOException e) {
      System.err.println("Error reading view hierarchy stream: " + e.getMessage());
      return null;
    }
    if (currentNode == null) {
      return null;
    }
    while (currentNode.parent != null) {
      currentNode = currentNode.parent;
    }

    return currentNode;
  }

  private static boolean isEncoded(byte[] data) {
    return data[0] == 'M';
  }

  private void addAll(ViewNode node, SortedMultiset<String> set, Map<String, ViewNode> props) {
    set.add(node.name);
    props.put(node.name, node);

    for (ViewNode n : node.children) {
      addAll(n, set, props);
    }
  }

  private static class ListViewRootsHandler extends HandleViewDebug.ViewDumpHandler {

    private final List<String> myViewRoots = Lists.newCopyOnWriteArrayList();

    public ListViewRootsHandler() {
      super(HandleViewDebug.CHUNK_VULW);
    }

    @Override
    protected void handleViewDebugResult(ByteBuffer data) {
      int nWindows = data.getInt();

      for (int i = 0; i < nWindows; i++) {
        int len = data.getInt();
        myViewRoots.add(getString(data, len));
      }
    }

    public List<String> getWindows(Client c, long timeout, TimeUnit unit) throws IOException {
      List<String> windowTitles = new ArrayList<>();
      HandleViewDebug.listViewRoots(c, this);
      waitForResult(timeout, unit);
      for (String rootTitle : myViewRoots) {
        windowTitles.add(rootTitle);
      }
      return windowTitles;
    }
  }


  private static class CaptureByteArrayHandler extends HandleViewDebug.ViewDumpHandler {
    public CaptureByteArrayHandler(int type) {
      super(type);
    }

    private AtomicReference<byte[]> mData = new AtomicReference<byte[]>();

    @Override
    protected void handleViewDebugResult(ByteBuffer data) {
      byte[] b = new byte[data.remaining()];
      data.get(b);
      mData.set(b);

    }

    public byte[] getData(long timeout, TimeUnit unit) {
      waitForResult(timeout, unit);
      return mData.get();
    }
  }
}
