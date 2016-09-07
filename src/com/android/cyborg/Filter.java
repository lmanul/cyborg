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

public abstract class Filter {

  public abstract boolean apply(ViewNode node);

  public static Filter withId(String id) {
    return new Filter() {
      @Override
      public boolean apply(ViewNode node) {
        return (node.id != null) && (node.id.equals("id/" + id));
      }

      @Override
      public String toString() {
        return "<Filter for id='" + id + "'>";
      }
    };
  }

  public static Filter withContentDescriptionStart(String text) {
    return new Filter() {
      @Override
      public boolean apply(ViewNode node) {
        String description = node.namedProperties.get("accessibility:contentDescription").value;
        return description != null && description.startsWith(text);
      }

      @Override
      public String toString() {
        return "<Filter for contentDesc='" + text + "...'>";
      }
    };
  }

  public static Filter withContentDescriptionEnd(String text) {
    return new Filter() {
      @Override
      public boolean apply(ViewNode node) {
        String description = node.namedProperties.get("accessibility:contentDescription").value;
        return description != null && description.endsWith(text);
      }

      @Override
      public String toString() {
        return "<Filter for contentDesc='..." + text + "'>";
      }
    };
  }

  public static Filter withText(String searchText) {
    return new Filter() {
      @Override
      public boolean apply(ViewNode node) {
        String text = node.namedProperties.get("text:text").value;
        return text.equals(searchText);
      }
    };
  }

  public static Filter clickable() {
    return new Filter() {
      @Override
      public boolean apply(ViewNode node) {
        String clickable = node.namedProperties.get("misc:clickable").value;
        return clickable.equals("true");
      }
    };
  }

  public static Filter and(Filter... filters) {
    return new Filter() {
      @Override
      public boolean apply(ViewNode node) {
        for (int i = 0; i < filters.length; i++) {
          if (!filters[i].apply(node)) {
            return false;
          }
        }
        return true;
      }
    };
  }

  public static Filter or(Filter... filters) {
    return new Filter() {
      @Override
      public boolean apply(ViewNode node) {
        for (int i = 0; i < filters.length; i++) {
          if (filters[i].apply(node)) {
            return true;
          }
        }
        return false;
      }
    };
  }
}
