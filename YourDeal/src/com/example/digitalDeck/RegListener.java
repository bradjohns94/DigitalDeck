package com.example.digitalDeck;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

/**
 * Created by Jason on 3/2/14.
 */
public class RegListener implements NsdManager.RegistrationListener {
    String mServiceName;
    @Override
    public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
        mServiceName = NsdServiceInfo.getServiceName();
        Log.i("stuff", mServiceName + " successfully registered.");
    }

    @Override
    public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
        mServiceName = serviceInfo.getServiceName();
        Log.i("stuff", mServiceName + " failed to register.");
    }

    @Override
    public void onServiceUnregistered(NsdServiceInfo arg0) {
        mServiceName = arg0.getServiceName();
        Log.i("stuff", mServiceName + " successfully unregistered.");
    }

    @Override
    public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
        mServiceName = serviceInfo.getServiceName();
        Log.i("stuff", mServiceName + " failed to unregister.");
    }
}
