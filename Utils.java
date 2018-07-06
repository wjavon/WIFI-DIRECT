package com.example.javonwalker.wifi_direct;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Javon Walker on 7/19/2017.
 */

public class Utils {

    private static int AddBook = 0;
    private static String TAG ="Utils";
    public static String[] address = null;
    //public static Hashtable<String, String> Route = new Hashtable<String, String>();
    public static ConcurrentHashMap<String, String> Route = new ConcurrentHashMap<String, String>();
    public static ArrayList<String> YellowPages = new ArrayList<String>();

    private final static String p2pInt = "p2p-p2p0";

    private Utils(){

    }

    public static String[] getARP(){
        String[] arp = null;
        BufferedReader bf = null;
        int i = 0;
        try{
            bf = new BufferedReader(new FileReader("/proc/net/arp"));
            String line = null;
            while ((line = bf.readLine()) != null) {
                arp[i++] = line;
            }
        }catch(Exception e){
            return null;
        }finally {
            try{
                bf.close();
            }catch(Exception e){
                return null;
            }
        }
        return arp;
    }

////////////////////////////////////

    public static String getIPFromMac(String MAC) {
        /*
         * method modified from:
         *
         * http://www.flattermann.net/2011/02/android-howto-find-the-hardware-mac-address-of-a-remote-host/
         *
         * */
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {

                String[] splitted = line.split(" +");
                if (splitted != null && splitted.length >= 4) {
                    // Basic sanity check
                    String device = splitted[5];
                    if (device.matches(".*" +p2pInt+ ".*")){
                        String mac = splitted[3];
                        if (mac.matches(MAC)) {
                            return splitted[0];
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    public static String getLocalIPAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    String iface = intf.getName();
                    if(iface.matches(".*" +p2pInt+ ".*")){
                        if (inetAddress instanceof Inet4Address) { // fix for Galaxy Nexus. IPv4 is easy to use :-)
                            return getDottedDecimalIP(inetAddress.getAddress());
                        }
                        if (inetAddress instanceof Inet6Address) { // fix for Galaxy Nexus. IPv4 is easy to use :-)
                            return getDottedDecimalIP(inetAddress.getAddress());
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("AndroidNetAddFactory", "getLocalIPAddress()", ex);
        } catch (NullPointerException ex) {
            Log.e("AndroidNetAddFactory", "getLocalIPAddress()", ex);
        }
        return null;
    }

    private static String getDottedDecimalIP(byte[] ipAddr) {
        /*
         * ripped from:
         *
         * http://stackoverflow.com/questions/10053385/how-to-get-each-devices-ip-address-in-wifi-direct-scenario
         *
         * */
        String ipAddrStr = "";
        for (int i=0; i<ipAddr.length; i++) {
            if (i > 0) {
                ipAddrStr += ".";
            }
            ipAddrStr += ipAddr[i]&0xFF;
        }
        return ipAddrStr;
    }


    ///////////////////////////////
    public static String[] getAddress(){
        updateTable();
        YellowPages = new ArrayList<String>(Route.values());
        String[] neighbors = new String[YellowPages.size()];
        try{
            YellowPages.toArray(neighbors);
        }catch(ArrayStoreException e){
            e.printStackTrace();
        }catch(NullPointerException e){
            e.printStackTrace();
        }
        return neighbors;
    }

    public static void updateTable(){
        String result = null;
        BufferedReader br = null;
        Scanner sk = null;
        try {
            Route.clear();
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                // while((line = sk.nextLine()) != null){
                //line = sk.nextLine();
                //    Log.d(y.TAG, "ARP IS REPORTING \n" + line);
                String[] splitted = line.split(" +");
                //if(splitted[0] != "IP"){
                if (splitted != null && splitted.length >= 4) {
                    // Basic sanity check
                    for(int i = 0; i < splitted.length; i++){

                        String device = splitted[5];

                        if (device.matches(".*" +p2pInt+ ".*")){

                            String mac = converter(splitted[3]);
                            result = splitted[0];
                            if(!Route.contains(mac)){
                                //maybe add more checks?
                                Route.put(mac, result);
                                //                        Log.d("mike", "ARP HAS \n " + Route.toString() + "\n end \n");
                            }
                        }else if(device.matches(".*"+"wlan0"+".*")){
                            //Log.d(y.TAG, "COULDN'T SPLIT PROPERLY");
                            //if(!Route.contains(splitted[0])){//this statement only adds to the ip conflict issue
                            Route.put(splitted[3], splitted[0]);
                            //}//
                        }
                    }
                    //}
                }else{
                    Log.d(TAG, "SPLITTED IS NULL");
                }
            }
            YellowPages = new ArrayList<String>(Route.values());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static String LegConverter(String mac){
        //177 -> 49
        //140 -> 12
        //need to shift by 128
        String[] parts = mac.split(":");
        String [] error = (parts[0].split("(?<=\\G.{1})"));
        Character corrected = hexShift(14, error[1].charAt(0));

        String fixed = (new Character(error[0].charAt(0))).toString() + corrected.toString();
        return new String(fixed+":"+parts[1]+":"+parts[2]+":"+parts[3]+":"+parts[4]+":"+parts[5]);
    }

    public static String MyConverter(String mac){//not 100% sure what this was for....
        //177 -> 49
        //140 -> 12
        //need to shift by 128
        String[] parts = mac.split(":");
        String [] error = (parts[0].split("(?<=\\G.{1})"));
        Character corrected = hexShift(2, error[1].charAt(0));

        String fixed = (new Character(error[0].charAt(0))).toString() + corrected.toString();
        return new String(fixed+":"+parts[1]+":"+parts[2]+":"+parts[3]+":"+parts[4]+":"+parts[5]);
    }

    public static String BSSIDConverter(String mac){
        //177 -> 49
        //140 -> 12
        //need to shift by 128
        String[] parts = mac.split(":");
        String []error = (parts[5].split("(?<=\\G.{1})"));
        Character corrected = hexShift(11, error[0].charAt(0));

        String fixed = corrected.toString() + (new Character(error[1].charAt(0))).toString();
        return new String(parts[0]+":"+parts[1]+":"+parts[2]+":"+parts[3]+":"+parts[4]+":"+fixed );
    }

    public static String secondConverter(String mac){
        //177 -> 49
        //140 -> 12
        //need to shift by 128
        String[] parts = mac.split(":");
        String []error = (parts[4].split("(?<=\\G.{1})"));
        Character corrected = hexShift(1, error[1].charAt(0));

        String fixed = (new Character(error[0].charAt(0))).toString() + corrected.toString();
        return new String(parts[0]+":"+parts[1]+":"+parts[2]+":"+parts[3]+":"+fixed+":"+parts[5]);
    }

    public static String converter(String mac){
        //177 -> 49
        //140 -> 12
        //need to shift by 128
        String[] parts = mac.split(":");
        String []error = (parts[4].split("(?<=\\G.{1})"));
        Character corrected = hexShift(8, error[0].charAt(0));

        String fixed = corrected.toString() + (new Character(error[1].charAt(0))).toString();
        return new String(parts[0]+":"+parts[1]+":"+parts[2]+":"+parts[3]+":"+fixed+":"+parts[5]);
    }

    public static Character hexShift(int shift, Character shiftMe){

        Character [] hex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

        int i = 0;

        while(!shiftMe.equals(hex[i])){

            i++;
            if(i == 0) return null;
        }
        return hex[(i + shift) % 16];

    }



    public static String getWDAddress(){

        String address = null;

        try{
            Enumeration<NetworkInterface> list = NetworkInterface.getNetworkInterfaces();
            NetworkInterface p2p = null;

            while(list.hasMoreElements()){

                p2p = list.nextElement();

                if(p2p.getName().contains("p2p0")){

                    Enumeration<InetAddress> AddressList = p2p.getInetAddresses();

                    while(AddressList.hasMoreElements()){

                        InetAddress IAdd = AddressList.nextElement();
                        address = IAdd.getHostAddress();
                    }
                    break;
                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }
        return address;
    }




}
