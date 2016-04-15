package com.example.tangoserialapplication;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import com.google.atap.tangoservice.Tango;

import java.io.IOException;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Created by brandonsladek on 4/12/16.
 */

public class NetworkControlActivity extends Activity {

    private ServerSocket serverSocket;
    private TextView mostRecentMessageTextView;
    private TextView connectionTextView;
    private TangoSerialConnection tangoSerialConnection;
    private NetworkControlActivity context = this;
    Handler updateConversationHandler;
    Thread serverThread = null;
    public static final int SERVERPORT = 5010;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_control);

        //tangoSerialConnection = (TangoSerialConnection) getIntent().getSerializableExtra("TangoSerialConnection");
        mostRecentMessageTextView = (TextView) findViewById(R.id.nc_mostRecentMessageTextView);
        connectionTextView = (TextView) findViewById(R.id.nc_connectionTextView);
        connectionTextView.setText("Not connected...");
        updateConversationHandler = new Handler();

        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();

    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ServerThread implements Runnable {

        public void run() {
            Socket socket = null;
            try {
                serverSocket = new ServerSocket(SERVERPORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {

                try {
                    // close this socket before closing serverSocket
                    socket = serverSocket.accept();

                    CommunicationThread commThread = new CommunicationThread(socket);
                    new Thread(commThread).start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class CommunicationThread implements Runnable {

        private Socket clientSocket;

        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {

            this.clientSocket = clientSocket;

            try {

                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {

            while (!Thread.currentThread().isInterrupted()) {

                try {
                    String read = input.readLine();

                    System.out.println("Read: " + read);

                    if (read.equals("c")) {
                        tangoSerialConnection = new TangoSerialConnection(context);
                        updateConversationHandler.post(new updateUIThread("Connected!"));
                    } else {
                        if (tangoSerialConnection != null) {
                            tangoSerialConnection.handleMessage(read.charAt(0));
                        }
                    }

                    updateConversationHandler.post(new updateUIThread(read));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    class updateUIThread implements Runnable {
        private String msg;

        public updateUIThread(String str) {
            this.msg = str;
        }

        @Override
        public void run() {
            mostRecentMessageTextView.setText(mostRecentMessageTextView.getText().toString()+"Client Says: "+ msg + "\n");
        }
    }
}
