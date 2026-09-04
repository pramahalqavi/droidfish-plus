/*
    DroidFish - An Android chess program.
    Copyright (C) 2016  Peter Österlund, peterosterlund2@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.petero.droidfish;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.OpenableColumns;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class FileUtil {
    /** Read a text file. Return string array with one string per line. */
    public static String[] readFile(String filename) throws IOException {
        ArrayList<String> ret = new ArrayList<>();
        try (InputStream inStream = new FileInputStream(filename);
             InputStreamReader inFile = new InputStreamReader(inStream, "UTF-8");
             BufferedReader inBuf = new BufferedReader(inFile)) {
            String line;
            while ((line = inBuf.readLine()) != null)
                ret.add(line);
        }
        return ret.toArray(new String[0]);
    }

    /** Write a text file. */
    public static void writeFile(String filename, String[] lines) throws IOException {
        try (OutputStream outStream = new FileOutputStream(filename)) {
            for (String line : lines) {
                byte[] bytes = (line + "\n").getBytes("UTF-8");
                outStream.write(bytes);
            }
        }
    }

    public static void writeFile(InputStream is, String filename) throws IOException {
        copyStreamToFile(is, new File(filename));
    }

    /** Copy an input stream to a file. */
    public static void copyStreamToFile(InputStream is, File f) throws IOException {
        try (OutputStream os = new FileOutputStream(f)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) > 0)
                os.write(buffer, 0, len);
        }
    }

    /** Copy a file to another file. */
    public static void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            copyStreamToFile(in, dst);
        }
    }

    /** Read an entire stream into a string. */
    public static String readFromStream(InputStream is) {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line).append('\n');
            }
            return total.toString();
        } catch (UnsupportedEncodingException e) {
            return "";
        } catch (IOException e) {
            return "";
        }
    }

    /** Return the length of a file, or -1 if length can not be determined. */
    public static long getFileLength(String filename) {
        try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
            return raf.length();
        } catch (IOException ex) {
            return -1;
        }
    }

    public interface FileNameFilter {
        boolean accept(String filename);
    }

    public static String[] findFilesInDirectory(String dirName, final FileNameFilter filter) {
        Set<String> resultSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        List<File> searchDirs = new ArrayList<>();
        String sep = File.separator;

        File extDir = Environment.getExternalStorageDirectory();
        if (extDir != null) {
            searchDirs.add(new File(extDir.getAbsolutePath() + sep + dirName));
        }
        Context ctx = DroidFishApp.getContext();
        if (ctx != null) {
            File appExt = ctx.getExternalFilesDir(null);
            if (appExt != null) {
                searchDirs.add(new File(appExt.getAbsolutePath() + sep + dirName));
            }
            searchDirs.add(new File(ctx.getFilesDir(), dirName));
        }

        for (File dir : searchDirs) {
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles(pathname -> {
                    if (!pathname.isFile())
                        return false;
                    return (filter == null) || filter.accept(pathname.getAbsolutePath());
                });
                if (files != null) {
                    for (File f : files) {
                        resultSet.add(f.getName());
                    }
                }
            }
        }
        return resultSet.toArray(new String[0]);
    }

    public static String getFullFilePath(String defaultDir, String fn) {
        String sep = File.separator;
        Context ctx = DroidFishApp.getContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            if (ctx != null) {
                File appExt = ctx.getExternalFilesDir(null);
                if (appExt != null) {
                    File appExtFile = new File(appExt.getAbsolutePath() + sep + defaultDir + sep + fn);
                    if (appExtFile.exists()) return appExtFile.getAbsolutePath();
                }
                File local = new File(ctx.getFilesDir(), defaultDir + sep + fn);
                if (local.exists()) return local.getAbsolutePath();

                if (appExt != null) {
                    File dir = new File(appExt, defaultDir);
                    dir.mkdirs();
                    return new File(dir, fn).getAbsolutePath();
                }
                File dir = new File(ctx.getFilesDir(), defaultDir);
                dir.mkdirs();
                return new File(dir, fn).getAbsolutePath();
            }
        }

        if (ctx != null) {
            File local = new File(ctx.getFilesDir(), defaultDir + sep + fn);
            if (local.exists()) return local.getAbsolutePath();
            File appExt = ctx.getExternalFilesDir(null);
            if (appExt != null) {
                File appExtFile = new File(appExt.getAbsolutePath() + sep + defaultDir + sep + fn);
                if (appExtFile.exists()) return appExtFile.getAbsolutePath();
            }
        }
        File extDir = Environment.getExternalStorageDirectory();
        if (extDir != null) {
            File extFile = new File(extDir.getAbsolutePath() + sep + defaultDir + sep + fn);
            if (extFile.exists()) return extFile.getAbsolutePath();
        }

        if (extDir != null) {
            return extDir.getAbsolutePath() + sep + defaultDir + sep + fn;
        }
        return fn;
    }

    public static String getFilePathFromUri(Context context, Uri uri) {
        if (uri == null)
            return null;
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        if (context == null)
            context = DroidFishApp.getContext();
        if (context != null && "content".equalsIgnoreCase(uri.getScheme())) {
            try {
                String displayName = "imported_" + System.currentTimeMillis() + ".pgn";
                try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (nameIdx >= 0) {
                            String name = cursor.getString(nameIdx);
                            if (name != null && name.length() > 0)
                                displayName = name;
                        }
                    }
                } catch (Exception ignore) {}

                File targetDir = new File(context.getFilesDir(), "pgn");
                targetDir.mkdirs();
                File dest = new File(targetDir, displayName);
                try (InputStream in = context.getContentResolver().openInputStream(uri);
                     OutputStream out = new FileOutputStream(dest)) {
                    if (in != null) {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                        return dest.getAbsolutePath();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return uri.getPath();
    }

    public static String getFilePathFromUri(Uri uri) {
        return getFilePathFromUri(DroidFishApp.getContext(), uri);
    }
}
