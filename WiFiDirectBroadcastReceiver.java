package com.example.javonwalker.wifi_direct;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.Random;
/**
 * Created by Javon Walker on 6/15/2017.
 */

/**
 * A BroadcastReceiver that notifies of important wifi p2p events.
 */

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
    private WifiP2pManager manager;
    private Channel channel;
    private WiFiDirectActivity activity;
    private WifiP2pDevice currentDevice;
    private int nonGOcheck_inRcvr;
    private int numberOfFalez;
    public int pickedRnd;
    public String name="None";

    private int howManyTimesMyDiscovery = 0;
    /**
     * @param manager  WifiP2pManager system service
     * @param channel  Wifi p2p channel
     * @param activity activity associated with the receiver
     */
    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel, WiFiDirectActivity activity) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }

    /*
     * (non-Javadoc)
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        DeviceListFragment fragmentList = getDeviceListFragment();
        DeviceDetailFragment detailFragment = getDeviceDetailFragment();



        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // UI update to indicate wifi p2p status.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi Direct mode is enabled
                activity.setIsWifiP2pEnabled(true);
            } else {
                activity.setIsWifiP2pEnabled(false);
                activity.resetData();
            }
            Log.d(WiFiDirectActivity.TAG, "P2P state changed - " + state);
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            if (manager != null) {
                manager.requestPeers(channel, (PeerListListener) activity.getFragmentManager().findFragmentById(R.id.frag_list));
            }

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            if (manager == null) {
                return;
            }

            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                manager.requestConnectionInfo(channel, detailFragment);
                manager.requestConnectionInfo(channel, fragmentList);

            } else {
                // It's a disconnect
                activity.Cases_startTime =  System.nanoTime();
                activity.connectionCounter = 0;
                //activity.info = null;
                fragmentList.connInvAmount = 0;
                fragmentList.info = null;
                fragmentList.canIStartSendingInv = false;
                fragmentList.connectionSendingPermissionIncrement = 0;
                numberOfFalez = 0;
                myDiscoverPeers();
                howManyTimesMyDiscovery = 0;
                caseB_discoverPeers(detailFragment);

                activity.resetData();
            }
            //checkGroup();
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {

            currentDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            fragmentList.updateThisDevice(currentDevice);

        } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {

            Toast.makeText(activity, "Scan results here", Toast.LENGTH_SHORT).show();
        }
/*
        else if (WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION.equals(action)) {
            SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
            if (SupplicantState.isValidState(state) && state == SupplicantState.COMPLETED) {
                WifiInfo currentWifi = wifiManager.getConnectionInfo();
                if (currentWifi != null) {
                    // get current wifi network name
                    if (!(currentWifi.getSSID().toLowerCase().contains("DIRECT_".toLowerCase())))
                        if(possibleGO(context)) {
                        activity.tryConnectAsLC(context, getPossibleLC_GO(context));
                    }
                }
            }
        }
*/
    }


    public DeviceListFragment getDeviceListFragment() {
        return (DeviceListFragment) activity.getFragmentManager().findFragmentById(R.id.frag_list);
    }

    public DeviceDetailFragment getDeviceDetailFragment() {
        return (DeviceDetailFragment) activity.getFragmentManager().findFragmentById(R.id.frag_detail);
    }


    public void caseB_discoverPeers(DeviceDetailFragment detailFragmentInFcn) {
        if (!detailFragmentInFcn.isThisInitial()) {
            pickedRnd = pickMyRandom();
            Toast.makeText(activity, "my waitTime is " + String.valueOf(pickedRnd/1e3), Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {
                public void run() {
                    myDiscoverPeers();
                }
            }, pickedRnd-1000);

            new Handler().postDelayed(new Runnable() {
                public void run() {
                    DeviceListFragment frgList = getDeviceListFragment();
                    nonGOcheck_inRcvr = 0;
                    for (WifiP2pDevice d : frgList.getDevicesFromList()) {
                        if (d.deviceName.toLowerCase().contains("Falez_".toLowerCase())) {
                            if (!d.isGroupOwner()) {
                                nonGOcheck_inRcvr++;
                            }
                            numberOfFalez++;
                        }
                    }
                    if (nonGOcheck_inRcvr == numberOfFalez) {
                        //myCreateGroup();
                    } else {
                        Toast.makeText(activity, "I didn't create Group", Toast.LENGTH_SHORT).show();
                    }
                }
            }, pickedRnd);

        }
    }

    public void myDiscoverPeers() {

        howManyTimesMyDiscovery = howManyTimesMyDiscovery + 1;
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //Toast.makeText(activity, "Discovery() from BroadcastReceiver x" + String.valueOf(howManyTimesMyDiscovery), Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailure(int reasonCode) {
            }
        });
    }

    public int pickMyRandom() {
        Random rand = new Random();
        int waitTime_inFcn = rand.nextInt(9500);//in miliseconds
        return waitTime_inFcn;
    }

    public void myCreateGroup() {
        manager.createGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                activity.getGroupInfo();
                Toast.makeText(activity, "Savage! I created group", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reasonCode) {

            }
        });
    }

    public boolean possibleGO(Context context){
        boolean state=false;
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        List<ScanResult> wifiScan = wifiManager.getScanResults();
        for(ScanResult i : wifiScan){
            if(i!=null)
                if(i.SSID.contains("DIRECT_") && i.SSID.contains("Feliz_")){
                    state=true;
                    Toast.makeText(activity, "Found possible GO for LC connection.", Toast.LENGTH_SHORT).show();
                    break;
                }
        }
        if(!state)Toast.makeText(activity, "No possible GO Found.", Toast.LENGTH_SHORT).show();

        return state;
    }

}