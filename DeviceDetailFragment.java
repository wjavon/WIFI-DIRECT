package com.example.javonwalker.wifi_direct;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.javonwalker.wifi_direct.DeviceListFragment.DeviceActionListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Javon Walker on 6/15/2017.
 */

public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener{
    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    private WifiP2pGroup group;

    ProgressDialog progressDialog = null;
    Button btn_disconnect;
    Button btn_connect;
    TextView textView=null;
    static Handler myHandler;


    public long Cases_stopTime_GO;
    public long Cases_stopTime_GR;
    double Cases_timePassed_GO;
    double Cases_timePassed_GR;

    public boolean thisIsInitial = true;

    public int deviceAmount = 5;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_detail, null);
        final DeviceListFragment List = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
        btn_connect = (Button) mContentView.findViewById(R.id.btn_connect);
        btn_connect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                mySetup(config,List.getDevice());
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + device.deviceAddress, true, true
                );

                ((DeviceActionListener) getActivity()).connect(config);

            }
        });

        btn_disconnect = (Button) mContentView.findViewById(R.id.btn_disconnect);
        btn_disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((DeviceActionListener) getActivity()).disconnect();
            }
        });
        return mContentView;
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {

        this.info = info;
        thisIsInitial = false;
        WiFiDirectActivity myActivity3 = ((WiFiDirectActivity) getActivity());

        //set a handler for client/server to show messages
        myHandler = new Handler() {
            public void handleMessage(Message msg) {
                textView=mContentView.findViewById(R.id.sockets);
                textView.setText(textView.getText()+"\n"+(String)msg.obj);
            }
        };

        myActivity3.connectionCounter++;
        if(info.groupFormed){
            if (info.isGroupOwner == true) {
                myActivity3.getGroupInfo();
                TextView Network = (TextView) mContentView.findViewById(R.id.p2p_group_network_name);
                Network.setText(String.format("My P2P Group Name: "+myActivity3.getNetworkName()));
                TextView passphrase = (TextView) mContentView.findViewById(R.id.p2p_group_passphrase);
                passphrase.setText(String.format("My P2P Group Passphrase: "+ myActivity3.getNetworkPassword()));
                mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
            }
            else{
                mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
            }
            if (myActivity3.connectionCounter == 1) {
                Cases_stopTime_GO = System.nanoTime();
                Cases_timePassed_GO = (Cases_stopTime_GO - myActivity3.Cases_startTime) / 1e9;
                TextView time_spent_GO = (TextView) mContentView.findViewById(R.id.time_spent_GO);
                time_spent_GO.setText(String.format("GO-change time: %.4f sec", Cases_timePassed_GO));


            } else if (myActivity3.connectionCounter == deviceAmount) {
                Cases_stopTime_GR = System.nanoTime();
                Cases_timePassed_GR = (Cases_stopTime_GR - myActivity3.Cases_startTime) / 1e9;
                TextView time_spent_GR = (TextView) mContentView.findViewById(R.id.time_spent_GR);
                time_spent_GR.setText(String.format("Group reformation time: %.4f sec", Cases_timePassed_GR));
            }
        }


        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        this.getView().setVisibility(View.VISIBLE);

        // InetAddress from WifiP2pInfo struct.
        TextView view2 = (TextView) mContentView.findViewById(R.id.device_info);
        view2.setText("Group Owner EBEM3 IP - " + info.groupOwnerAddress.getHostAddress());

        if(info.isGroupOwner==false){
            view2 = (TextView) mContentView.findViewById(R.id.local_ip);
            view2.setText("Local IP Address: "+Utils.getLocalIPAddress());}

        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text) + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes) : getResources().getString(R.string.no)));

        TextView pickedTime = (TextView) mContentView.findViewById(R.id.pickedTime);
        pickedTime.setText(String.format("My random time is: %.4f", (myActivity3.receiver.pickedRnd/1e3)));
        //presents group passphrase of LC
        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.

    }

    public String getGOIP(){
        return info.groupOwnerAddress.getHostAddress();
    }


    public boolean isThisInitial(){
        return thisIsInitial;
    }

    /**
     * Updates the UI with device data
     *
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());

    }


    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.time_spent_GO);//
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.time_spent_GR);//
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.sockets);//
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.pickedTime);//
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.p2p_group_network_name);//
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.p2p_group_passphrase);//
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.local_ip);//
        view.setText(R.string.empty);
    }


    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }
    public void mySetup(WifiP2pConfig config,WifiP2pDevice device){
        String MAC = device.deviceAddress;
        List macList= new ArrayList<String>();
        macList.add("50:46:5d:c8:31:c9");macList.add(0,"ac:22:0b:45:87:96");macList.add("d8:50:e6:74:db:c5");macList.add(1,"d8:50:e6:74:da:c5");
        if((MAC ==macList.get(0))|(MAC==macList.get(1))) {
            config.groupOwnerIntent=15;
        }
        for(int i=2; i<macList.size(); i++){
            if(MAC==macList.get(i)){
                config.groupOwnerIntent=0;
            } }
    }


}