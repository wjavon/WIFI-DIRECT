package com.example.javonwalker.wifi_direct;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.annotation.SuppressLint;
import android.net.wifi.p2p.WifiP2pManager.Channel;

import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

/**
 * Created by utkudemir on 6/1/16.
 */
public class MessageClient {
    DatagramSocket ds = null;
    DatagramPacket dp;
    private WifiP2pManager manager;
    private Channel channel;
    public int Num=10;
    String dstAddress;
    int dstPort;
    int udpPort;
    String response = "";
    WeakReference<TextView> textResponse;
    private AsyncTask<Void, Void, Void> async_client;
    private DeviceDetailFragment myFragment;
    private Handler myhandler = new Handler(Looper.getMainLooper());
    private InetAddress UDPAddress=null;
    private static byte [] Message= new byte [1470];

    //MessageClient(String addr, int port, TextView textResponse) {
    MessageClient(String addr, int port, TextView textResponse, WifiP2pManager manager, Channel channel, DeviceDetailFragment myFragment) {
        dstAddress = addr;
        dstPort = port;
        udpPort = port+1010;
        this.textResponse = new WeakReference<TextView>(textResponse) ;
        this.manager = manager;
        this.channel = channel;
        this.myFragment = myFragment;}


    ////////////////////////////////////////////////
    ////////////////////////////////////////////////
    ////////////////// TCP Client //////////////////
    @SuppressLint("StaticFieldLeak")
    public void SendTCP(){
        async_client= new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... arg0) {
                Socket socket = null;
                try {
                    socket = new Socket(dstAddress, dstPort);
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    InputStream inputStream = socket.getInputStream();
                    /*
                     * notice: inputStream.read() will block if no data return
                     */
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                        response += byteArrayOutputStream.toString("UTF-8");
                    }

                } catch (UnknownHostException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    response = "UnknownHostException: " + e.toString();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    response = "I throw IOException: " + e.toString();
                } finally {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                return null;
            }

            @Override
            protected void onPreExecute() {
                TextView textView = textResponse.get();
                if (textView != null) {
                    textView.setText(textView.getText()+"---TCP Client started---\n");
                }
            }

            @Override
            protected void onPostExecute(Void result) {
                //textResponse.setText("I got the message. I'll create Group in "+response+" sec");
                //int groupTime = 1000*Integer.parseInt(response);
                TextView textView = textResponse.get();
                if (textView != null) {
                    textView.setText(textView.getText()+"Server said: I got " + response + "/"+String.valueOf(Num)+"\n");
                }

                Log.d("MessageClient", response);
                myFragment.handleResponse(response);
                super.onPostExecute(result);
            }
        };
        if (Build.VERSION.SDK_INT >= 11)
            async_client.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else async_client.execute();
    }



    ////////////////////////////////////////////////
    ////////////////////////////////////////////////
    ////////////////// UDP Client //////////////////
    @SuppressLint("StaticFieldLeak")
    public void Send() {
        async_client = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                TextView textView = textResponse.get();
                if (textView != null) {
                    textView.setText(textView.getText()+"---UDP Client Started---\n");
                }

                try {
                    UDPAddress = InetAddress.getByName(dstAddress);
                } catch (UnknownHostException e) {
                    Log.d("UDP Client", "Could not find IP"); }
            }

            @Override
            protected Void doInBackground(Void... params) {
                try{
                    ds = new DatagramSocket();
                    dp = new DatagramPacket(Message, Message.length, UDPAddress, udpPort);
                    for(int i=0;i<Num;i++) {
                        ds.send(dp);Thread.sleep(13);}
                } catch(SocketException e) {
                    e.printStackTrace();}
                catch (Exception e) {
                    e.printStackTrace(); }
                return null;
            }

            protected void onPostExecute(Void result) {
                if (ds != null) {
                    ds.close(); }
                TextView textView = textResponse.get();
                if (textView != null) {
                    textView.setText(textView.getText()+"---UDP Client Stopped---\n");
                }
                SendTCP();
                super.onPostExecute(result);
            }
        };

        if (Build.VERSION.SDK_INT >= 11)
            async_client.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else async_client.execute();
    }

}
