package com.example.digitalDeck;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Jason on 3/2/14.
 */
public class GameListener implements NsdManager.DiscoveryListener {

    public ArrayList<Game> games;
    public ArrayList<NsdServiceInfo> services;

    public GameListener(ArrayList<Game> games) {
        this.games = games;
        services = new ArrayList<NsdServiceInfo>();
    }

    //  Called as soon as service discovery begins.
    @Override
    public void onDiscoveryStarted(String regType) {
        Log.i("stuff", "Discovery started with " + regType);
    }
    @Override
    public void onServiceFound(NsdServiceInfo service) {
        Log.i("stuff", "Service named " + service.getServiceName() + " found.");
        if (!services.contains(service)) {
            games.add(new Game(4, "Host", service.getServiceName()));
            services.add(service);
        }

    }
    @Override
    public void onServiceLost(NsdServiceInfo service) {
        Log.i("stuff", "Service named " + service.getServiceName() + " lost.");
    }
    @Override
    public void onDiscoveryStopped(String serviceType) {
        Log.i("stuff", "Stopped discovery for " + serviceType);
    }
    @Override
    public void onStartDiscoveryFailed(String serviceType, int errorCode) {
        Log.i("stuff", "Discovery of " + serviceType + " start fail, Error: " + errorCode);
    }
    @Override
    public void onStopDiscoveryFailed(String serviceType, int errorCode) {
        Log.i("stuff", "Discovery of " + serviceType + " stop fail, Error: " + errorCode);
    }
    public ArrayList<Game> getGames() {
        return games;
    }
    public ArrayList<NsdServiceInfo> getServices() {
        return services;
    }
}
