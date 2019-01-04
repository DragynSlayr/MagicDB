package github.dragynslayr.magicdb;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

class FileHandler {

    private static final String TAG = "MagicDB_File", USER_FILE = "user";

    static void saveUser(Context context, String user) {
        try {
            File file = new File(context.getApplicationContext().getFilesDir(), USER_FILE);
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(user);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            Log.e(TAG, "File write failed: " + e.toString());
        }
    }

    static String loadUser(Context context) {
        String s = "";
        try {
            File file = new File(context.getApplicationContext().getFilesDir(), USER_FILE);
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line = reader.readLine().trim();
                if (line.length() > 0) {
                    s = line;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "File read failed: " + e.toString());
        }
        return s;
    }
}
