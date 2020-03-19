package org.scijava.maven.plugin.install;

/*   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class CompareDirectories {

  enum Status {
    ADDED, DELETED, MODIFIED, VERSION_CHANGED, MOVED;
  }

  static class FileChange {
    String oldPath = "", newPath = "";
    Status status;

    static String unversionedName(String name) {
      Matcher matcher = AbstractCopyJarsMojo.versionPattern.matcher(name);
      if(matcher.matches()) {
        return matcher.group(1);
      }else {
        return name;
      }
    }

    String unversionedOldPath() {
      return unversionedName(oldPath);
    }

    String unversionedNewPath() {
      return unversionedName(newPath);
    }

    String oldFileName() {
      return new File(oldPath).getName();
    }

    String newFileName() {
      return new File(newPath).getName();
    }
  }

  private static List<FileChange> getChanges(File commit1, File commit2) throws IOException {
    List<FileChange> changes = CompareDirectories.compare(commit1, commit2);
    detectVersionChanges(changes);
    detectMoves(changes);
    Collections.sort(changes, Comparator.comparing((FileChange c) -> c.status)
            .thenComparing(c -> c.oldPath)
            .thenComparing(c -> c.newPath));
    changes.forEach(change -> System.out.println(toString(change)));
    return changes;
  }

  private static String toString(FileChange change) {
    if(change.status.equals(Status.ADDED)) return change.status.name() + " " + change.newPath;
    if(change.status.equals(Status.DELETED)) return change.status.name() + " " + change.oldPath;
    if(change.status.equals(Status.VERSION_CHANGED)) {
      return change.status.name() + " " + change.unversionedOldPath() + " (" + version(change.oldPath) + " -> " + version(change.newPath) + ")";
    }
    return change.status.name() + " " + change.oldPath + " -> " + change.newPath;
  }

  private static String version(String name) {
    Matcher matcher = AbstractCopyJarsMojo.versionPattern.matcher(name);
    if(matcher.matches()) {
      return matcher.group(2).substring(1);
    }else {
      return "";
    }
  }

  private static void detectVersionChanges(List<FileChange> changes) {
    Map<String, FileChange> deleted = changes.stream()
            .filter(change -> change.status.equals(Status.DELETED))
            .collect(Collectors.toMap(FileChange::unversionedOldPath, Function.identity(),
                    (o1,o2) -> o1 , LinkedHashMap::new));
    Map<String, FileChange> added = changes.stream()
            .filter(change -> change.status.equals(Status.ADDED))
            .collect(Collectors.toMap(FileChange::unversionedNewPath, Function.identity(),
                    (o1,o2) -> o1 , LinkedHashMap::new));
    for (Map.Entry<String,FileChange> del : deleted.entrySet()) {
      FileChange add = added.get(del.getKey());
      if(add!=null) {
        changes.remove(del.getValue());
        changes.remove(add);
        FileChange c = new FileChange();
        c.oldPath = del.getValue().oldPath;
        c.newPath = add.newPath;
        c.status = Status.VERSION_CHANGED;
        changes.add(c);
      }
    }
  }

  private static void detectMoves(List<FileChange> changes) {
    Map<String, FileChange> deleted = changes.stream()
            .filter(change -> change.status.equals(Status.DELETED))
            .collect(Collectors.toMap(FileChange::oldFileName, Function.identity(),
                    (o1,o2) -> o1 , LinkedHashMap::new));
    Map<String, FileChange> added = changes.stream()
            .filter(change -> change.status.equals(Status.ADDED))
            .collect(Collectors.toMap(FileChange::newFileName, Function.identity(),
                    (o1,o2) -> o1 , LinkedHashMap::new));
    for (Map.Entry<String,FileChange> del : deleted.entrySet()) {
      FileChange add = added.get(del.getKey());
      if(add!=null) {
        changes.remove(del.getValue());
        changes.remove(add);
        FileChange c = new FileChange();
        c.oldPath = del.getValue().oldPath;
        c.newPath = add.newPath;
        c.status = Status.MOVED;
        changes.add(c);
      }
    }
  }

  private static List<FileChange> compare(File dir1, File dir2) throws IOException {
    List<FileChange> res = new ArrayList<>();

    System.out.println("Comparing " + dir1 + " with " + dir2 + ":");

    Set set1 = new LinkedHashSet();
    Set set2 = new LinkedHashSet();

    if(dir1 != null) {
      addFilesToSet(dir1, set1, dir1);
    }

    if(dir2 != null) {
      addFilesToSet(dir2, set2, dir2);
    }

    for (Iterator i = set1.iterator(); i.hasNext();) {
      String name = (String) i.next();
      if (!set2.contains(name)) {
        FileChange change = new FileChange();
        change.status = Status.DELETED;
        change.oldPath = name;
        res.add(change);
        continue;
      }
      set2.remove(name);
      if (!streamsEqual(new FileInputStream(new File(dir1.getAbsolutePath(), name)),
              new FileInputStream(new File(dir2.getAbsolutePath(), name)))) {
        FileChange change = new FileChange();
        change.status = Status.MODIFIED;
        change.oldPath = name;
        change.newPath = name;
        res.add(change);
      }
    }
    for (Object o : set2) {
      String name = (String) o;
      FileChange change = new FileChange();
      change.status = Status.ADDED;
      change.newPath = name;
      res.add(change);
    }

    return res;
  }

  private static void addFilesToSet(File baseDir, Set result, File file1) {
      if(file1.isDirectory()) for (File file : file1.listFiles()) {
        addFilesToSet(baseDir, result, file);
      } else {
        result.add(file1.getAbsolutePath().replace(baseDir.getAbsolutePath(), ""));
      }
  }

  private static boolean streamsEqual(InputStream stream1, InputStream stream2) throws IOException {
    byte[] buf1 = new byte[4096];
    byte[] buf2 = new byte[4096];
    boolean done1 = false;
    boolean done2 = false;

    try {
      while (!done1) {
        int off1 = 0;
        int off2 = 0;

        while (off1 < buf1.length) {
          int count = stream1.read(buf1, off1, buf1.length - off1);
          if (count < 0) {
            done1 = true;
            break;
          }
          off1 += count;
        }
        while (off2 < buf2.length) {
          int count = stream2.read(buf2, off2, buf2.length - off2);
          if (count < 0) {
            done2 = true;
            break;
          }
          off2 += count;
        }
        if (off1 != off2 || done1 != done2)
          return false;
        for (int i = 0; i < off1; i++) {
          if (buf1[i] != buf2[i])
            return false;
        }
      }
      return true;
    } finally {
      stream1.close();
      stream2.close();
    }
  }

  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.out.println("Add args [dir1] [dir2]");
      System.exit(1);
    }
    getChanges(new File(args[0]), new File(args[1]));
  }
}
