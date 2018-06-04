package com.example.javonwalker.wifi_direct;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.util.ArrayList;

import com.example.javonwalker.wifi_direct.DeviceListFragment.DeviceActionListener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static android.os.Environment.*;

public class WiFiDirectActivity extends Activity implements ChannelListener, DeviceActionListener {

    public static final String TAG = "wifidirectdemo";
    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;
    private WifiP2pInfo info;
    private String name="None";
    private String passphrase="None";
/*    private List<String> pass_Feliz_1 = Arrays.asList("L0gMJUXg", "KkfGGUSG","AbWHO7oC");
    private List<String> pass_Feliz_2 = Arrays.asList("Jby4lnjL", "YCKO5hBu");
    private List<String> pass_Feliz_3 = Arrays.asList("N1tLFJof", "R596Js6e", "AlgqHd8D","MLEJP8n0");
    private List<String> pass_Feliz_6 = Arrays.asList("R9S76o0J", "r3UjKW7V", "gl93lcaw", "SYSJJPhv");*/
    private final IntentFilter intentFilter = new IntentFilter();
    private Channel channel;
    public WiFiDirectBroadcastReceiver receiver = null;


    public int connectionCounter = 0;
    public long Cases_startTime;

    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // add necessary intent values to be matched.

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

    }

    public WifiP2pManager getMyManager(){
        return manager;
    }

    public Channel getMyChannel(){
        return channel;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * register the BroadcastReceiver with the intent values to be matched
     */
    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    /**
     * Remove all peers and clear all fields. This is called on
     * BroadcastReceiver receiving a state change event.
     */
    public void resetData() {
        DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
        DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail);

        if (fragmentList != null) {
            fragmentList.clearPeers();
        }
        if (fragmentDetails != null) {
            fragmentDetails.resetViews();
        }
    }

    public String getNetworkName(){
        return  name;
       }

    public String getNetworkPassword(){
        return passphrase;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        return true;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.atn_direct_enable:
                if (manager != null && channel != null) {
                    // Since this is the system wireless settings activity, it's
                    // not going to send us a result. We will be notified by
                    // WiFiDeviceBroadcastReceiver instead.
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                } else {
                    Log.e(TAG, "channel or manager is null");
                }
                return true;

            case R.id.atn_direct_discover:
                if (!isWifiP2pEnabled) {
                    Toast.makeText(WiFiDirectActivity.this, R.string.p2p_off_warning, Toast.LENGTH_SHORT).show();
                    return true;
                }
                final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
                //fragment.onInitiateDiscovery(); //JUST TO SHOW THE MESSAGE
                manager.discoverPeers(channel, new ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WiFiDirectActivity.this, "Discovery Failed : " + reasonCode, Toast.LENGTH_SHORT).show();
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void showDetails(WifiP2pDevice device) {
        DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail);
        fragment.showDetails(device);

    }

    @Override
    public void connect(WifiP2pConfig config) {
        manager.connect(channel, config, new ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(WiFiDirectActivity.this, "Connection -> Framework success", Toast.LENGTH_SHORT).show();
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(WiFiDirectActivity.this, "Connect failed. Retry.", Toast.LENGTH_SHORT).show();
            }
        });

    }


    @Override
    public void disconnect() {
        final DeviceDetailFragment DetailFragment = (DeviceDetailFragment) getFragmentManager().findFragmentById(R.id.frag_detail);
        final DeviceListFragment ListFragment = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
        DetailFragment.resetViews();
        ListFragment.need_to_Check=true;
        manager.removeGroup(channel, new ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                //Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
                Toast.makeText(WiFiDirectActivity.this, "GO could not disconnect", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSuccess() {
                DetailFragment.getView().setVisibility(View.GONE);
                Toast.makeText(WiFiDirectActivity.this, "GO DID disconnect", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onChannelDisconnected() {
        // we will try once more
        if (manager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(this,
                    "Savage! Channel is probably lost permanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void cancelDisconnect() {
        /*
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        if (manager != null) {
            final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            if (fragment.getDevice() == null
                    || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE
                    || fragment.getDevice().status == WifiP2pDevice.INVITED) {

                manager.cancelConnect(channel, new ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(WiFiDirectActivity.this, "Aborting connection",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WiFiDirectActivity.this,
                                "Connect abort request failed. Reason Code: " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

        }
    }
    public void getGroupInfo() {//retreive group name and password from group owner
        manager.requestGroupInfo(channel, new GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
                Toast.makeText(WiFiDirectActivity.this,
                        "Showing Group Info.",
                        Toast.LENGTH_SHORT).show();
                if (group != null) {
                    name = group.getNetworkName();
                    passphrase = group.getPassphrase();
                } else {
                    name = "No Group Formed";
                    passphrase = "None";
                 }
            }
        });

    }

    //to use a toast to show message on screen
    public void showMessage(String str){
        Toast.makeText(WiFiDirectActivity.this, str,Toast.LENGTH_SHORT).show();
    }



    ///For Async Servers
/*    public static void updateRecord(double packet, String s){
        File gpxfile;
        try {
            File root = new File(Environment.getDownloadCacheDirectory().getAbsoluteFile(), "Motive_Data");
            if (!root.exists()) {
                root.mkdirs();
            }
            if(s=="sent") gpxfile = new File(root, "Packets_Sent.txt");
            else gpxfile = new File(root, "Packets_Receive.txt");
            FileWriter writer = new FileWriter(gpxfile, true);
            writer.append(String.valueOf(packet) + "\n");
            writer.flush();
            writer.close();
        }catch(IOException e) {
            Log.d("Writing to Data.txt","IO Error.");
        }
    }


*/





    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /*
    //Code for automatically creating a LC-GO connection
    public void tryConnectAsLC(Context context, String Nname){
        List<String> Pass;
        if (Nname.toLowerCase().contains("Feliz_1".toLowerCase()))
            Pass=pass_Feliz_1;
        else if (Nname.toLowerCase().contains("Feliz_2".toLowerCase()))
            Pass=pass_Feliz_2;
        else if (Nname.toLowerCase().contains("Feliz_3".toLowerCase()))
            Pass=pass_Feliz_3;
        else //if (Nname.toLowerCase().contains("Feliz_6".toLowerCase()))
            Pass=pass_Feliz_6;
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = Nname;   // Please note the quotes. String should contain ssid in quotes
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals(Nname)) {
                for(int l=0; l<Pass.size(); l++){
                    conf.preSharedKey = "\""+ Pass.get(l)+"\"";
                    wifiManager.addNetwork(conf);
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(i.networkId, true);
                    if(wifiManager.reconnect()){
                        Toast.makeText(WiFiDirectActivity.this,
                                "LC connnection establish: "+Nname,
                                Toast.LENGTH_SHORT).show();
                        break;}
                    else
                        continue;
                }


                break;
            }

        }
    }*/
}