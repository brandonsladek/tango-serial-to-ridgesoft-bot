package com.example.tangoserialapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by brandonsladek on 4/13/16.
 */

public class NetworkConnectionThread implements Runnable {
    @Override
    public void run() {

    }

//    private ServerSocket serverSocket;
//    private static final int SERVER_PORT = 5010;
//    private BufferedReader incomingData;
//    private Handler handler;
//    private String NETWORK_CONNECTION_THREAD_TAG = "NETWORK_CONNECTION_THREAD_TAG";
//
//    public NetworkConnectionThread(Handler handler, ServerSocket serverSocket) {
//        this.handler = handler;
//        this.serverSocket = serverSocket;
//    }
//
//    @Override
//    public void run() {
//        Socket socket = null;
//
//        try {
//            serverSocket = new ServerSocket(SERVER_PORT);
//        } catch(IOException ioe) {
//            Log.w(NETWORK_CONNECTION_THREAD_TAG, "Exception instantiating new server socket!");
//        }
//
//        while(!Thread.currentThread().isInterrupted()) {
//            try {
//                // close socket before closing serverSocket
//                socket = serverSocket.accept();
//            } catch(IOException ioe) {
//                Log.w(NETWORK_CONNECTION_THREAD_TAG, "Exception accepting server socket!");
//            }
//
//            try {
//                if (socket != null) {
//                    incomingData = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//                }
//            } catch(IOException ioe) {
//                Log.w(NETWORK_CONNECTION_THREAD_TAG, "Exception getting socket input stream!");
//            }
//
//            try {
//                if (incomingData != null) {
//                    String receivedData = incomingData.readLine();
//                    Bundle bundle = new Bundle();
//                    bundle.putString("NETWORK_DATA", receivedData);
//                    Message msg = new Message();
//                    msg.setData(bundle);
//                    handler.sendMessage(msg);
//                }
//                // update UI thread with received data
//            } catch(IOException ioe) {
//                Log.w(NETWORK_CONNECTION_THREAD_TAG, "Exception reading line from incoming data!");
//            }
//        }
//        // everything should be in here...
//
//        // while(true) {
//        // listen for commands...
//        // ask for handler from parent that created thread
//        // parent.handler.sendMessage
//    }

}
