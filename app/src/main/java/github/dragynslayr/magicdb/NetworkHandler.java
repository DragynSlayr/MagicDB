package github.dragynslayr.magicdb;

import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;

class NetworkHandler {

    private static final String IP = "70.72.212.179";
    private static final int PORT = 19615;
    private String arg;
    private Command cmd;

    NetworkHandler(Command cmd, String arg) {
        this.cmd = cmd;
        this.arg = arg;
    }

    String getString() {
        String found = "";
        try {
            InetAddress address = InetAddress.getByName(IP);
            Socket socket = new Socket(address, PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.print(cmd + ":" + arg);
            out.flush();

            String response = in.readLine();
            if (response != null) {
                found = response;
            }

            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return found;
    }

    String[] getStringArray() {
        ArrayList<String> found = new ArrayList<>();

        try {
            InetAddress address = InetAddress.getByName(IP);
            Socket socket = new Socket(address, PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.print(cmd + ":" + arg);
            out.flush();

            String response;
            while ((response = in.readLine()) != null) {
                Collections.addAll(found, response.split("\n"));
            }

            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return found.toArray(new String[0]);
    }

    public enum Command {
        Search("SRC"), Register("REG"), Login("LGN"), GetList("GET"), AddCard("PUT");

        private String cmd;

        Command(String t) {
            cmd = t;
        }

        @NonNull
        @Override
        public String toString() {
            return cmd;
        }
    }
}
