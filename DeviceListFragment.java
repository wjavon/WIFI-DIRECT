package com.example.javonwalker.wifi_direct;

/**
 * Created by Javon Walker on 6/15/2017.
 */

import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.javonwalker.wifi_direct.helper.ConnectHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * A ListFragment that displays available peers on discovery and requests the
 * parent activity to handle user interaction events
 */
public class DeviceListFragment extends ListFragment implements PeerListListener, ConnectionInfoListener {

    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    ProgressDialog progressDialog = null;
    View mContentView = null;
    private WifiP2pDevice device;
    public WifiP2pInfo info;
    public int connInvAmount = 0;
    public boolean need_to_Check=true;
    public boolean canIStartSendingInv = false;
    public int connectionSendingPermissionIncrement = 0;




    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        this.info = info;

        myDiscoverPeersInList();

        //checkWhetherThereIsGO();

        //sendAutomaticConnInvForOnConnection();//entire process of moving onConnectionAvailable to Detail should be done now

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.setListAdapter(new WiFiPeerListAdapter(getActivity(), R.layout.row_devices, peers));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_list, null);
        return mContentView;
    }

    /**
     * @return this device
     */
    public WifiP2pDevice getDevice() {
        return device;
    }

    private static String getDeviceStatus(int deviceStatus) {
        Log.d(WiFiDirectActivity.TAG, "Peer status :" + deviceStatus);
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";
        }
    }
    public String getStatus(){
         return getDeviceStatus(device.status);
    }

    /**
     * Initiate a connection with the peer.
     */
    // I THINK HERE IT TAPS ON THE DEVICE SO THAT IT CAN CONNECT!!!
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        WifiP2pDevice device = (WifiP2pDevice) getListAdapter().getItem(position);// PICK THE DEVICE
        showDeviceDetails(device);
    }

    private void showDeviceDetails(WifiP2pDevice device) {
        ((DeviceActionListener) getActivity()).showDetails(device);
    }

    /**
     * Array adapter for ListFragment that maintains WifiP2pDevice list.
     */
    private class WiFiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {

        private List<WifiP2pDevice> items;

        /**
         * @param context
         * @param textViewResourceId
         * @param objects
         */
        public WiFiPeerListAdapter(Context context, int textViewResourceId,
                                   List<WifiP2pDevice> objects) {
            super(context, textViewResourceId, objects);
            items = objects;

        }

        // AVAILABLE DEVICE NAMES AFTER DISCOVERY INITIATED.
        // TOP: DEVICE NAME
        // BOTTOM: AVAILABILITY
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.row_devices, null);
            }
            WifiP2pDevice device = items.get(position);
            if (device != null) {
                TextView top = (TextView) v.findViewById(R.id.device_name);
                TextView bottom = (TextView) v.findViewById(R.id.device_details);
                if (top != null) {
                    top.setText(String.format("Hi %s", device.deviceName));
                    //time_spent.setText(String.format("Time spent: %.4f sec", timePassed));
                }
                if (bottom != null) {
                    bottom.setText(getDeviceStatus(device.status));
                }
            }

            return v;

        }
    }

    /**
     * Update UI for this device.
     *
     * @param device WifiP2pDevice object
     */
    public void updateThisDevice(WifiP2pDevice device) {
        this.device = device;
        TextView view = (TextView) mContentView.findViewById(R.id.my_name);
        view.setText(device.deviceName);
        view = (TextView) mContentView.findViewById(R.id.my_status);
        view.setText(getDeviceStatus(device.status));
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        peers.clear();
        peers.addAll(peerList.getDeviceList());
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
        if (peers.size() == 0) {
            Log.d(WiFiDirectActivity.TAG, "Ebem-No devices found");
            return;
        }

        //Falez_1: ae:22:0b:45:87:96
        //Falez_2: da:50:e6:74:db:75
        //Falez_3: da:50:e6:74:da:c5
        //Falez_4: da:50:e6:74:dc:b3
        //Falez_5: da:50:e6:74:da:d3
        //Falez_6:

       // if(need_to_Check) checkWhetherThereIsGO();

        //sendAutomaticConnInvForOnPeersAvail();

    }

    public void clearPeers() {
        peers.clear();
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
    }

//////////////////   //////////////////   //////////////////   //////////////////   //////////////////


    /**
     * An interface-callback for the activity to listen to fragment interaction
     * events.
     */
    public List<WifiP2pDevice> getDevicesFromList() {
        return peers;
    }

    private void myDiscoverPeersInList() {
        WiFiDirectActivity myActivityInList = ((WiFiDirectActivity) getActivity());
        myActivityInList.getMyManager().discoverPeers(myActivityInList.getMyChannel(), new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reasonCode) {
            }
        });
    }

    private void checkWhetherThereIsGO() {
        connectionSendingPermissionIncrement = 0;
        if (info != null) {
            if (info.isGroupOwner) {
                for (WifiP2pDevice d : peers) {
                    if (d.deviceName.toLowerCase().contains("Falez_".toLowerCase())) {
                        if (d.isGroupOwner()) {
                          /*  ((DeviceActionListener) getActivity()).disconnect(); //disconnect
                            // User chose to disconnect as a group owner
                            Toast.makeText(getActivity(), "Found another GO, I am going", Toast.LENGTH_SHORT).show();
                            break;*/
                            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                            alert.setTitle("Group Owner Found").
                                    setMessage("Do you want me to disconnect as a Group Owner?");
                            alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    ((DeviceActionListener) getActivity()).disconnect(); //disconnect
                                    // User chose to disconnect as a group owner
                                    Toast.makeText(getActivity(), "I am going", Toast.LENGTH_SHORT).show();
                                    need_to_Check=true;
                                }
                            });
                            alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    Toast.makeText(getActivity(), "Ok, I'll stay.", Toast.LENGTH_SHORT).show();
                                    need_to_Check=false;
                                    // User cancelled the dialog
                                    }
                            });
                            alert.create().show();
                            break;
                        }
                    }
                }
            }
        }
    }

    private void sendAutomaticConnInvForOnConnection() {
        WifiP2pDevice deviceToBeConnected;
        myDiscoverPeersInList();
        if (info != null) {
            if (info.isGroupOwner) {
                for (WifiP2pDevice d : peers) { //could implement a list and a for loop here
                    if ("ae:22:0b:45:87:96".equals(d.deviceAddress) || "da:50:e6:74:db:75".equals(d.deviceAddress) || "da:50:e6:74:da:c5".equals(d.deviceAddress) || "da:50:e6:74:dc:b3".equals(d.deviceAddress) || "da:50:e6:74:da:d3".equals(d.deviceAddress) || "da:50:e6:74:db:32".equals(d.deviceAddress)) {
                        deviceToBeConnected = d;
                        if (deviceToBeConnected != null) {
                            if (!"Connected".equals(getDeviceStatus(deviceToBeConnected.status))) {
                                ConnectHelper.onConnectBegin((DeviceActionListener) getActivity(), deviceToBeConnected);
                                connInvAmount = connInvAmount + 1;
                                Toast.makeText(getActivity(), "onConnection inv." + String.valueOf(connInvAmount) + "times", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }
        }
    }

    private void sendAutomaticConnInvForOnPeersAvail() {
        WifiP2pDevice deviceToBeConnected;
        //myDiscoverPeersInList();
        if (info != null) {
            if (info.isGroupOwner) {
                for (WifiP2pDevice d : peers) {
                    if ("ae:22:0b:45:87:96".equals(d.deviceAddress) || "da:50:e6:74:db:75".equals(d.deviceAddress) || "da:50:e6:74:da:c5".equals(d.deviceAddress) || "da:50:e6:74:dc:b3".equals(d.deviceAddress) || "da:50:e6:74:da:d3".equals(d.deviceAddress) || "da:50:e6:74:db:32".equals(d.deviceAddress)) {
                        deviceToBeConnected = d;
                        if (deviceToBeConnected != null) {
                            if ("Available".equals(getDeviceStatus(deviceToBeConnected.status))) {
                                ConnectHelper.onConnectBegin((DeviceActionListener) getActivity(), deviceToBeConnected);
                                connInvAmount = connInvAmount + 1;
                                Toast.makeText(getActivity(), "onConnection inv." + String.valueOf(connInvAmount) + "times", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }
        }
    }


    public String getGOMac(){
        String MAC="";
        for(WifiP2pDevice d : peers)
            if(d.isGroupOwner())
                MAC=d.deviceAddress;
        return MAC;
    }

    public interface DeviceActionListener {
        void showDetails(WifiP2pDevice device);

        void cancelDisconnect();

        void connect(WifiP2pConfig config);

        void disconnect();
    }
}
