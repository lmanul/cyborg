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

import java.util.ArrayList;
import java.util.List;

public abstract class Filter {

  public abstract boolean apply(ViewNode node);
  abstract String getShortDesc();

  @Override
  public String toString() {
    return "<Filter for " + getShortDesc() + ">";
  }


  public static Filter withId(String id) {
    return new Filter() {
      @Override
      public boolean apply(ViewNode node) {
        return idEquals(node, id);
      }

      @Override
      String getShortDesc() {
        return "id='" + id + "'";
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
      String getShortDesc() {
        return "contentDesc='" + text + "...'";
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
      String getShortDesc() {
        return "contentDesc='..." + text + "'";
      }
    };
  }

  public static Filter withText(String searchText) {
    return new Filter() {
      @Override
      public boolean apply(ViewNode node) {
        if (!node.namedProperties.containsKey("text:text")) {
          return false;
        }
        String text = node.namedProperties.get("text:text").value;
        return text.trim().equals(searchText.trim());
      }

      @Override
      String getShortDesc() {
        return "text='" + searchText + "'";
      }
    };
  }

  public static Filter nthChildOfParentWithId(int n, String id) {
    return new Filter() {
      @Override
      public boolean apply(ViewNode node) {
        if (node.parent != null && idEquals(node.parent, id) &&
            node.parent.children.get(n) == node) {
          return true;
        }
        return false;
      }

      @Override
      String getShortDesc() {
        return "child #" + n + "of parent with id " + id;
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

      @Override
      String getShortDesc() {
        return "clickable";
      }
    };
  }

  public static Filter isFocused() {
    return new Filter() {
      @Override
      public boolean apply(ViewNode node) {
        String isFocused = node.namedProperties.get("focus:isFocused").value;
        return isFocused.equals("true");
      }

      @Override
      String getShortDesc() {
        return "is focused";
      }
    };
  }

  public static Filter withParentWithId(String id) {
    return new Filter() {
      @Override
      public boolean apply(ViewNode node) {
        return node.parent != null && idEquals(node.parent, id);
      }

      @Override
      String getShortDesc() {
        return "parentId='" + id + "'";
      }
    };
  }

  public static Filter and(Filter... filters) {
    return new Filter() {
      @Override
      public boolean apply(ViewNode node) {
        for (Filter filter : filters) {
          if (!filter.apply(node)) {
            return false;
          }
        }
        return true;
      }

      @Override
      String getShortDesc() {
        List<String> descriptions = new ArrayList<>();
        for (Filter filter : filters) {
          descriptions.add(filter.getShortDesc());
        }
        return String.join(" AND ", descriptions);
      }
    };
  }

  public static Filter or(Filter... filters) {
    return new Filter() {
      @Override
      public boolean apply(ViewNode node) {
        for (Filter filter : filters) {
          if (filter.apply(node)) {
            return true;
          }
        }
        return false;
      }

      @Override
      String getShortDesc() {
        List<String> descriptions = new ArrayList<>();
        for (Filter filter : filters) {
          descriptions.add(filter.getShortDesc());
        }
        return String.join(" OR ", descriptions);
      }
    };
  }

  private static boolean idEquals(ViewNode node, String id) {
    return node.id != null && node.id.equals("id/" + id);
  }
}
