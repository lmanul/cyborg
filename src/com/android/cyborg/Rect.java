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

import java.awt.Point;

public class Rect {
  public int x;
  public int y;
  public int w;
  public int h;

  public Rect(int x, int y, int w, int h) {
    this.x = x; this.y = y; this.w = w; this.h = h;
  }

  public Point getCenter() {
    return new Point(x + (w / 2), y + (h / 2));
  }


  public void grow(int units) {
    x -= units;
    y -= units;
    w += 2 * units;
    h += 2 * units;
  }

  public void shrink(int units) {
    x += units;
    y += units;
    w -= 2 * units;
    h -= 2 * units;
  }

  @Override
  public String toString() {
    return "<Rect (" + x + ", " + y + ") w=" + w + " h=" + h + ">";
  }
}
