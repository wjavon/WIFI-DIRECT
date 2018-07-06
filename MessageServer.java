package com.example.javonwalker.wifi_direct;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.Random;
import java.util.concurrent.TimeoutException;

/**
 * Created by utkudemir on 6/1/16.
 */
public class MessageServer extends Thread {
    //WiFiDirectActivity activity;
    ServerSocket serverSocket;
    String message = "";
    public int clientOrder = 0;
    static final int socketServerPORT = 8080;
    public int received;
    private AsyncTask<Void, Void, Void> async;
    private boolean Server_activ = true;
    WeakReference<TextView> textResponse;

    Random rand = new Random();
    int sendingMsgTime = rand.nextInt(11);//in miliseconds

    public MessageServer(TextView text) {
        //this.activity = activity;
        this.textResponse=new WeakReference<TextView>(text) ;
        received=0;
        Thread udpThread=new Thread(new UDPserver());
        udpThread.start();
        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();
        TextView textView = textResponse.get();
        if (textView != null) {
            textView.setText(textView.getText()+"---Server started---\n");
        }
    }

    public void onDestroy() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    ////////////    ////////////    ////////////    ////////////    ////////////
    private class SocketServerThread extends Thread {

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(socketServerPORT);
                while (true) {
                    final Socket socket = serverSocket.accept();


                    message = "Connection request from "
                            + socket.getInetAddress() + ":"
                            + socket.getPort() + "\n";
                    Log.i("MessageServer", message);
                    Log.i("MessageServer", socket.getInetAddress().toString());

                    if(socket != null){
                        Log.d("MessageServer", "good job");
                        clientOrder = clientOrder + 1;
                    }

////                    activity.runOnUiThread(new Runnable() {
////
////                        @Override
////                        public void run() {
////                            //activity.socket_message2.setText(message);
////                            //Toast.makeText(getActivity(), "Some String", Toast.LENGTH_SHORT).show();
////                            Log.i("MessageServer", "_ac2" + message);
////                        }
////                    });
//                    new Handler().postDelayed(new Runnable() {
//                        public void run() {
                    SocketServerReplyThread socketServerReplyThread = new SocketServerReplyThread(socket, clientOrder);
                    clientOrder=0;
                    //SocketServerReplyThread socketServerReplyThread = new SocketServerReplyThread(socket);
                    socketServerReplyThread.run();
//                        }
//                    }, 8000);

                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    ////////////    ////////////    ////////////    ////////////    ////////////
    private class SocketServerReplyThread extends Thread {

        private Socket hostThreadSocket; // "socket" --> for Client, "ServerSocket" --> for Server
        private int clientOrder2;

        SocketServerReplyThread(Socket socket, int order) {
            //SocketServerReplyThread(Socket socket) {
            hostThreadSocket = socket;
            clientOrder2 = order;
        }

        @Override
        public void run() {
            OutputStream outputStream;
            String msgReply;
            if(clientOrder2==1){
                msgReply = String.valueOf(received);
                received=0;
            } else {
                msgReply = "DIDNTSENT";
            }

            if (msgReply!=null) {
                try {

                    outputStream = hostThreadSocket.getOutputStream();
                    PrintStream printStream = new PrintStream(outputStream);
                    printStream.print(msgReply);
                    printStream.close();

                    message = "Server (Me) sent: " + msgReply + "\n";

////               activity.runOnUiThread(new Runnable() {
////
////                    @Override
////                    public void run() {
////                        //activity.msg.setText(message);
////                        Log.i("MessageServer", "_l2" + message);
////                    }
////                });

                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    message = "Something wrong! " + e.toString() + "\n";
                }
            }
////            activity.runOnUiThread(new Runnable() {
////
////                @Override
////                public void run() {
////                    //activity.msg.setText(message);
////                    Log.i("MessageServer", "_l3" + message);
////                }
////            });
        }
    }
    ////////////////////////////////////////////////
    ////////////////// UDP SERVER //////////////////
    private class UDPserver extends Thread {
        @Override
        public void run() {
            byte[] lMsg = new byte[4096];
            DatagramPacket dp = new DatagramPacket(lMsg, lMsg.length);
            DatagramSocket ds = null;

            try {
                ds = new DatagramSocket(socketServerPORT+1010);
                while(Server_activ) {
                    ds.receive(dp);
                    if(ds!=null){
                        received++;
                    }
                }
            } catch (SocketTimeoutException e){
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (ds != null) {
                    ds.close();
                }
            }
        }
    }

    public void stop_UDP_Server()
    {
        Server_activ = false;
    }
}













//////////// GETTING THE IP ADDRESS ////////////
//    public String getIpAddress() {
//        String ip = "";
//        try {
//            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
//                    .getNetworkInterfaces();
//            while (enumNetworkInterfaces.hasMoreElements()) {
//                NetworkInterface networkInterface = enumNetworkInterfaces
//                        .nextElement();
//                Enumeration<InetAddress> enumInetAddress = networkInterface
//                        .getInetAddresses();
//                while (enumInetAddress.hasMoreElements()) {
//                    InetAddress inetAddress = enumInetAddress
//                            .nextElement();
//
//                    if (inetAddress.isSiteLocalAddress()) {
//                        ip += "Server running at : "
//                                + inetAddress.getHostAddress();
//
//                        Log.i("Server stuff_p: ", inetAddress.getHostAddress());
//                    }
//                }
//            }
//
//        } catch (SocketException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//            ip += "Something Wrong! " + e.toString() + "\n";
//        }
//        return ip;
//    }
