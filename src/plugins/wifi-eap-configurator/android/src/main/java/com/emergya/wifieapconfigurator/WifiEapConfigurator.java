package com.emergya.wifieapconfigurator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WifiNetworkSuggestion;
import android.net.wifi.hotspot2.ConfigParser;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.net.wifi.hotspot2.pps.HomeSp;
import android.net.wifi.hotspot2.pps.Credential;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
//import android.net.wifi.WifiNetworkSuggestion;
//import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.security.KeyChain;
import android.util.Base64;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import static androidx.core.content.PermissionChecker.checkSelfPermission;

@NativePlugin(
        permissions = {
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION
        })
public class WifiEapConfigurator extends Plugin {

    List<ScanResult> results = null;

    private WifiManager wifiManager;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @PluginMethod()
    public void configureAP(PluginCall call) throws JSONException {
        String[] ssids = new String[call.getArray("ssid").length()];
        boolean res = true;

        String[] oids = new String[call.getArray("oid").length()];
        if (call.getArray("oid").length() != 0 && call.getArray("oid").get(0) != "") {
            List aux = call.getArray("oid").toList();
            for (int i = 0 ; i < aux.size() ; i++) {
                oids[i] = aux.get(i).toString();
            }
        }

        if((call.getArray("ssid").length() == 0 || call.getArray("ssid").get(0) == "") && oids.length == 0) {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.ssid.missing");
            call.success(object);
            res = false;
        }

        if (call.getArray("ssid").length() != 0 && call.getArray("ssid").get(0) != "") {
            List aux = call.getArray("ssid").toList();
            for (int i = 0 ; i < aux.size() ; i++) {
                ssids[i] = aux.get(i).toString();
            }
        }

        String clientCertificate = null;
        if (call.getString("clientCertificate") != null && !call.getString("clientCertificate").equals("")) {
            clientCertificate = call.getString("clientCertificate");
        }

        String passPhrase = null;
        if (call.getString("passPhrase") != null && !call.getString("passPhrase").equals("")) {
            passPhrase = call.getString("passPhrase");
        }

        String anonymousIdentity = null;
        if (call.getString("anonymous") != null && !call.getString("anonymous").equals("")) {
            anonymousIdentity = call.getString("anonymous");
        }

        String[] caCertificate = new String[call.getArray("caCertificate").length()];
        if (call.getArray("caCertificate").length() != 0 && call.getArray("caCertificate").get(0) != "") {
            List aux = call.getArray("caCertificate").toList();
            for (int i = 0 ; i < aux.size() ; i++) {
                caCertificate[i] = aux.get(i).toString();
            }
        }

        Integer eap = getEapMethod(call.getInt("eap"));
        if (eap == null) {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.eap.invalid");
            call.success(object);
            res = false;
        }

        String[] servername = new String[call.getArray("servername").length()];
        if (call.getArray("servername").length() != 0 && call.getArray("servername").get(0) != "") {
            List aux = call.getArray("servername").toList();
            for (int i = 0 ; i < aux.size() ; i++) {
                servername[i] = aux.get(i).toString();
            }
        }

        String username = null;
        String password = null;
        Integer auth = null;

        String id = null;
        if (call.getString("id") != null && !call.getString("id").equals("")) {
            id = call.getString("id");
        }
        String displayName = null;
        if (call.getString("displayName") != null && !call.getString("displayName").equals("")) {
            displayName = call.getString("displayName");
        }

        if (clientCertificate == null && passPhrase == null) {
            if (call.getString("username") != null && !call.getString("username").equals("")) {
                username = call.getString("username");
            } else {
                JSObject object = new JSObject();
                object.put("success", false);
                object.put("message", "plugin.wifieapconfigurator.error.username.missing");
                call.success(object);
                res = false;
            }

            if (call.getString("password") != null && !call.getString("password").equals("")) {
                password = call.getString("password");
            } else {
                JSObject object = new JSObject();
                object.put("success", false);
                object.put("message", "plugin.wifieapconfigurator.error.password.missing");
                call.success(object);
                res = false;
            }


            auth = getAuthMethod(call.getInt("auth"));
            if (auth == null) {
                JSObject object = new JSObject();
                object.put("success", false);
                object.put("message", "plugin.wifieapconfigurator.error.auth.invalid");
                call.success(object);
                res = false;
            }
        }

        if (res) {
            for (int i = 0 ; i < ssids.length ; i++) {
                res = getNetworkAssociated(call, ssids[i]);
            }
        }

        if (res) {
            connectAP(ssids, username, password, servername, caCertificate, clientCertificate, passPhrase, eap, auth, anonymousIdentity, displayName, id, oids, call);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    void connectAP(String[] ssids, String username, String password, String[] servernames, String[] caCertificates, String clientCertificate, String passPhrase,
                   Integer eap, Integer auth, String anonymousIdentity, String displayName, String id, String[] oids, PluginCall call) {

        WifiEnterpriseConfig enterpriseConfig = new WifiEnterpriseConfig();

        enterpriseConfig.setAnonymousIdentity(anonymousIdentity);

        if (servernames.length != 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                enterpriseConfig.setDomainSuffixMatch(getLongestSuffix(servernames));
                enterpriseConfig.setAltSubjectMatch("DNS:" + String.join(";DNS:", servernames));
            }
        } else {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.ca.missing");
            call.success(object);
            return;
        }

        enterpriseConfig.setEapMethod(eap);

        CertificateFactory certFactory = null;
        X509Certificate[] caCerts = null;
        List<X509Certificate> certificates = new ArrayList<X509Certificate>();
        // building the certificates
        for (String certString : caCertificates) {
            byte[] bytes = Base64.decode(certString, Base64.NO_WRAP);
            ByteArrayInputStream b = new ByteArrayInputStream(bytes);

            try {
                certFactory = CertificateFactory.getInstance("X.509");
                certificates.add((X509Certificate) certFactory.generateCertificate(b));
            } catch (CertificateException e) {
                JSObject object = new JSObject();
                object.put("success", false);
                object.put("message", "plugin.wifieapconfigurator.error.ca.invalid");
                call.success(object);
                return;
            } catch (IllegalArgumentException e) {
                JSObject object = new JSObject();
                object.put("success", false);
                object.put("message", "plugin.wifieapconfigurator.error.ca.invalid");
                call.success(object);
                return;
            }
        }
        try {
            enterpriseConfig.setCaCertificates(certificates.toArray(new X509Certificate[certificates.size()]));
        } catch (IllegalArgumentException e) {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.ca.invalid");
            call.success(object);
            return;
        }

        X509Certificate cert = null;
        PrivateKey key = null;

        // Explicitly reset client certificate, will set later if needed
        enterpriseConfig.setClientKeyEntry(null, null);

        if (eap != WifiEnterpriseConfig.Eap.TLS) {
            enterpriseConfig.setIdentity(username);
            enterpriseConfig.setPassword(password);

            enterpriseConfig.setPhase2Method(auth);

        } else {
            // Explicitly unset unused fields
            enterpriseConfig.setPassword("");
            enterpriseConfig.setPhase2Method(WifiEnterpriseConfig.Phase2.NONE);

            // For TLS, "identity" is used for outer identity,
            // while for PEAP/TTLS, "identity" is the inner identity,
            // and anonymousIdentity is the outer identity
            // - so we have to do some weird shuffling here.
            enterpriseConfig.setIdentity(anonymousIdentity);

            KeyStore pkcs12ks = null;
            try {
                pkcs12ks = KeyStore.getInstance("pkcs12");

                byte[] bytes = Base64.decode(clientCertificate, Base64.NO_WRAP);
                ByteArrayInputStream b = new ByteArrayInputStream(bytes);
                InputStream in = new BufferedInputStream(b);
                try {
                    pkcs12ks.load(in, passPhrase.toCharArray());
                } catch(Exception e) {
                    JSObject object = new JSObject();
                    object.put("success", false);
                    object.put("message", "plugin.wifieapconfigurator.error.passphrase.null");
                    call.success(object);
                    return;
                }

                Enumeration<String> aliases = pkcs12ks.aliases();

                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    cert = (X509Certificate) pkcs12ks.getCertificate(alias);
                    key = (PrivateKey) pkcs12ks.getKey(alias, passPhrase.toCharArray());
                    enterpriseConfig.setClientKeyEntry(key, cert);
                }

            } catch (KeyStoreException e) {
                sendClientCertificateError(e, call);
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                sendClientCertificateError(e, call);
                e.printStackTrace();
            } catch (UnrecoverableKeyException e) {
                sendClientCertificateError(e, call);
                e.printStackTrace();
            }
        }

        if (ssids.length > 0) {
            for (String ssid : ssids) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    connectWifiAndroidQ(ssid, enterpriseConfig, call);
                } else {
                    connectWifiBySsid(ssid, enterpriseConfig, call, displayName);
                }
            }
        }

        if (oids.length > 0) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                removePasspoint(id, call);
            }
            connectPasspoint(id, displayName, oids, enterpriseConfig, call, key);
        }
    }

    private void removePasspoint(String id, PluginCall call) {
        List passpointsConfigurated = new ArrayList();
        WifiManager wifiManager = getWifiManager();

        try {
            passpointsConfigurated = wifiManager.getPasspointConfigurations();
            int pos = 0;
            boolean enc = false;
            while (passpointsConfigurated.size() > pos && !enc) {
                if ((passpointsConfigurated.get(pos)).equals(id)) {
                    enc = true;
                } else {
                    pos++;
                }
            }
            if (enc) {
                wifiManager.removePasspointConfiguration(id);
            }
        } catch (IllegalArgumentException e) {
            JSObject object = new JSObject();
            object.put("success", true);
            object.put("message", "plugin.wifieapconfigurator.error.passpoint.remove");
            call.success(object);
        } catch (Exception e) {
            JSObject object = new JSObject();
            object.put("success", true);
            object.put("message", "plugin.wifieapconfigurator.error.passpoint.not.enabled");
            call.success(object);
        }
    }

    private void connectPasspoint(String id, String displayName, String[] oid, WifiEnterpriseConfig enterpriseConfig, PluginCall call, PrivateKey key) {

        PasspointConfiguration config = new PasspointConfiguration();

        HomeSp homeSp = new HomeSp();
        homeSp.setFqdn(enterpriseConfig.getDomainSuffixMatch());

        if (displayName != null) {
            homeSp.setFriendlyName(displayName);
        } else {
            homeSp.setFriendlyName(id + " via Passpoint");
        }

        long[] roamingConsortiumOIDs = new long[oid.length];
        int index = 0;
        for (String roamingConsortiumOIDString : oid) {
            if (!roamingConsortiumOIDString.startsWith("0x")) {
                roamingConsortiumOIDString = "0x" + roamingConsortiumOIDString;
            }
            roamingConsortiumOIDs[index] = Long.decode(roamingConsortiumOIDString);
            index++;
        }
        homeSp.setRoamingConsortiumOis(roamingConsortiumOIDs);

        config.setHomeSp(homeSp);
        Credential cred = new Credential();
        cred.setRealm(id);
        cred.setCaCertificate(enterpriseConfig.getCaCertificate());

        switch(enterpriseConfig.getEapMethod()) {
            case WifiEnterpriseConfig.Eap.TLS:
                Credential.CertificateCredential certCred = new Credential.CertificateCredential();
                certCred.setCertType("x509v3");
                cred.setClientPrivateKey(key);
                cred.setClientCertificateChain(enterpriseConfig.getClientCertificateChain());
                certCred.setCertSha256Fingerprint(getFingerprint(enterpriseConfig.getClientCertificateChain()[0]));
                cred.setCertCredential(certCred);
                break;
            case WifiEnterpriseConfig.Eap.PEAP:
            case WifiEnterpriseConfig.Eap.TTLS:
            case WifiEnterpriseConfig.Eap.PWD:
                byte[] data = new byte[0];
                try {
                    data = enterpriseConfig.getPassword().getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                String base64 = Base64.encodeToString(data, Base64.DEFAULT);

                Credential.UserCredential us = new Credential.UserCredential();
                us.setUsername(enterpriseConfig.getIdentity());
                us.setPassword(base64);
                us.setEapType(21);
                switch(enterpriseConfig.getPhase2Method()) {
                    // Strings from android.net.wifi.hotspot2.pps.Credential.UserCredential.AUTH_METHOD_*
                    case WifiEnterpriseConfig.Phase2.MSCHAPV2: us.setNonEapInnerMethod("MS-CHAP-V2"); break;
                    case WifiEnterpriseConfig.Phase2.PAP: us.setNonEapInnerMethod("PAP"); break;
                    case WifiEnterpriseConfig.Phase2.MSCHAP: us.setNonEapInnerMethod("MS-CHAP"); break;
                    // Do we need a default case here?
                }
                cred.setUserCredential(us);
                break;
            default:
                return; // this is probably not what we should do..
        }

        config.setCredential(cred);

        try {
            wifiManager.addOrUpdatePasspointConfiguration(config);
            JSObject object = new JSObject();
            object.put("success", true);
            object.put("message", "plugin.wifieapconfigurator.success.passpoint.linked");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.passpoint.linked");
            call.success(object);
        } catch (Exception e) {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.passpoint.not.enabled");
            call.success(object);
        }
    }

    private void connectWifiBySsid(String ssid, WifiEnterpriseConfig enterpriseConfig, PluginCall call, String displayName) {
        WifiManager myWifiManager = getWifiManager();
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + ssid + "\"";
        config.priority = 1;
        config.status = WifiConfiguration.Status.ENABLED;
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
        config.enterpriseConfig = enterpriseConfig;

        try {
            int wifiIndex = myWifiManager.addNetwork(config);
            myWifiManager.disconnect();
            myWifiManager.enableNetwork(wifiIndex, true);
            myWifiManager.reconnect();

            myWifiManager.setWifiEnabled(true);

            JSObject object = new JSObject();
            object.put("success", true);
            object.put("message", "plugin.wifieapconfigurator.success.network.linked");
            call.success(object);
        } catch (java.lang.SecurityException e) {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.network.linked");
            call.success(object);
            e.printStackTrace();
            Log.e("error", e.getMessage());
        }
    }

    /*
    private boolean createSuggestion(WifiManager wifiManager, String ssid, WifiEnterpriseConfig enterpriseConfig, PasspointConfiguration passpointConfig){
            boolean configured = false;
            if (getPermission(Manifest.permission.CHANGE_NETWORK_STATE)) {

                ArrayList<WifiNetworkSuggestion> suggestions = new ArrayList<>();
                WifiNetworkSuggestion.Builder suggestionBuilder =  new WifiNetworkSuggestion.Builder();

                if (ssid != null) {
                    suggestionBuilder.setSsid(ssid);
                }
                suggestionBuilder.setWpa2EnterpriseConfig(enterpriseConfig);

                if (passpointConfig != null && Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    suggestionBuilder.setPasspointConfig(passpointConfig);
                }
                final WifiNetworkSuggestion suggestion = suggestionBuilder.build();

                // WifiNetworkSuggestion approach
                suggestions.add(suggestion);

                wifiManager.removeNetworkSuggestions(new ArrayList<WifiNetworkSuggestion>());
                int status = wifiManager.addNetworkSuggestions(suggestions);

                if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {

                    final IntentFilter intentFilter =
                            new IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION);

                    final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if (!intent.getAction().equals(
                                    WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
                                return;
                            }
                        }
                    };
                    getContext().registerReceiver(broadcastReceiver, intentFilter);

                    configured = true;
                } else {
                    Log.d("STATUS ERROR", "" + status);
                    configured = false;
                }
            }
            return configured;

    }*/


    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void connectWifiAndroidQ(String ssid, WifiEnterpriseConfig enterpriseConfig, PluginCall call) {
        if (getPermission(Manifest.permission.CHANGE_NETWORK_STATE)) {

            ArrayList<WifiNetworkSuggestion> suggestions = new ArrayList<>();
            WifiNetworkSpecifier suggestion =  new WifiNetworkSpecifier.Builder()
                    .setSsid(ssid)
                    .setWpa2EnterpriseConfig(enterpriseConfig)
                    .build();

            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(suggestion)
                    .build();

            ConnectivityManager connectivityManager = (ConnectivityManager)getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback());

            /*if (passpointConfig != null) {
                suggestionBuilder.setPasspointConfig(passpointConfig);
            }*/

            // WifiNetworkSuggestion approach
            /*suggestions.add(suggestion);
            WifiManager wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int status = wifiManager.addNetworkSuggestions(suggestions);

            if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {

                final IntentFilter intentFilter =
                        new IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION);

                final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (!intent.getAction().equals(
                                WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
                            return;
                        }
                    }
                };
                getContext().registerReceiver(broadcastReceiver, intentFilter);

                JSObject object = new JSObject();
                object.put("success", true);
                object.put("message", "plugin.wifieapconfigurator.success.network.linked");
                call.success(object);
            } else {
                JSObject object = new JSObject();
                object.put("success", false);
                object.put("message", "plugin.wifieapconfigurator.success.network.reachable");
                call.success(object);
            }*/

            /*
            WifiNetworkSpecifier wifiNetworkSpecifier = suggestionBuilder.build();
            NetworkRequest.Builder networkRequestBuilder = new NetworkRequest.Builder();
            networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
            networkRequestBuilder.setNetworkSpecifier(wifiNetworkSpecifier);
            NetworkRequest networkRequest = networkRequestBuilder.build();
            final ConnectivityManager cm = (ConnectivityManager)
                    getContext().getApplicationContext()
                            .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                cm.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        super.onAvailable(network);
                        cm.bindProcessToNetwork(network);
                    }});
            }*/
        }

    }

    private void sendClientCertificateError(Exception e, PluginCall call) {
        JSObject object = new JSObject();
        object.put("success", false);
        object.put("message", "plugin.wifieapconfigurator.error.clientCertificate.invalid - " + e.getMessage());
        call.success(object);
        Log.e("error", e.getMessage());
    }

    @PluginMethod
    public boolean removeNetwork(PluginCall call) {
        String ssid = null;
        boolean res = false;

        if (call.getString("ssid") != null && !call.getString("ssid").equals("")) {
            ssid = call.getString("ssid");
        } else {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.ssid.missing");
            call.success(object);
            return res;
        }

        WifiManager wifi = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        /*if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { */
        List<WifiConfiguration> configuredNetworks = wifi.getConfiguredNetworks();
        for (WifiConfiguration conf : configuredNetworks) {
            if (conf.SSID.equals(ssid) || conf.SSID.equals("\"" + ssid + "\"")) { // TODO document why ssid can be surrounded by quotes
                wifi.removeNetwork(conf.networkId);
                wifi.saveConfiguration();
                JSObject object = new JSObject();
                object.put("success", true);
                object.put("message", "plugin.wifieapconfigurator.success.network.removed");
                call.success(object);
                res = true;
            }
        }
        /*} else {
            wifi.removeNetworkSuggestions(new ArrayList<WifiNetworkSuggestion>());
            JSObject object = new JSObject();
            object.put("success", true);
            object.put("message", "plugin.wifieapconfigurator.success.network.removed");
            call.success(object);
            res = true;
        }*/

        if (!res) {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.success.network.missing");
            call.success(object);
        }

        return res;
    }

    private WifiManager getWifiManager() {
        if (wifiManager == null) {
            wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        }
        return wifiManager;
    }

    @PluginMethod
    public void enableWifi(PluginCall call) {
        //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        WifiManager wifiManager = getWifiManager();
        if (wifiManager.setWifiEnabled(true)) {
            JSObject object = new JSObject();
            object.put("success", true);
            object.put("message", "plugin.wifieapconfigurator.success.wifi.enabled");
            call.success(object);
        } else {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.wifi.disabled");
            call.success(object);
        }
        /*} else{
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.wifi.disabled");
            call.success(object);
        }*/
    }

    @PluginMethod
    public boolean isNetworkAssociated(PluginCall call) {
        String ssid = null;
        boolean res = false, isOverridable = false;

        //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        if (call.getString("ssid") != null && !call.getString("ssid").equals("")) {
            ssid = call.getString("ssid");
        } else {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.ssid.missing");
            call.success(object);
            return res;
        }

        WifiManager wifi = getWifiManager();

        List<WifiConfiguration> configuredNetworks = wifi.getConfiguredNetworks();
        for (WifiConfiguration conf : configuredNetworks) {
            if (conf.SSID.equals(ssid) || conf.SSID.equals("\"" + ssid + "\"")) { // TODO document why ssid can be surrounded by quotes

                String packageName = getContext().getPackageName();
                if (conf.toString().toLowerCase().contains(packageName.toLowerCase())) { // TODO document why case insensitive
                    isOverridable = true;
                }

                JSObject object = new JSObject();
                object.put("success", false);
                object.put("message", "plugin.wifieapconfigurator.error.network.alreadyAssociated");
                object.put("overridable", isOverridable);
                call.success(object);
                res = true;
                break;
            }
        }

        if (!res) {
            JSObject object = new JSObject();
            object.put("success", true);
            object.put("message", "plugin.wifieapconfigurator.success.network.missing");
            call.success(object);
        }
        /*} else{
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.ssid.missing");
            call.success(object);
        }*/

        return res;
    }

    @PluginMethod
    public void reachableSSID(PluginCall call) {
        String ssid = null;
        boolean isReachable = false;
        if (call.getString("ssid") != null && !call.getString("ssid").equals("")) {
            ssid = call.getString("ssid");
        } else {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.ssid.missing");
            call.success(object);
        }

        boolean granted = getPermission(Manifest.permission.ACCESS_FINE_LOCATION);

        LocationManager lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        boolean location = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!location) {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.location.disabled");
            call.success(object);
        } else if (granted) {

            WifiManager wifiManager = getWifiManager();
            Iterator<ScanResult> results = wifiManager.getScanResults().iterator();

            while (isReachable == false && results.hasNext()) {
                ScanResult s = results.next();
                if (s.SSID.equals(ssid) || s.SSID.equals("\"" + ssid + "\"")) { // TODO document why ssid can be surrounded by quotes
                    isReachable = true;
                }
            }

            String message = isReachable ? "plugin.wifieapconfigurator.success.network.reachable" : "plugin.wifieapconfigurator.error.network.notReachable";

            JSObject object = new JSObject();
            object.put("success", true);
            object.put("message", message);
            object.put("isReachable", isReachable);
            call.success(object);
        } else {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.permission.notGranted");
            call.success(object);
        }
    }

    @PluginMethod
    public void isConnectedSSID(PluginCall call) {
        String ssid = null;
        boolean isConnected = false;
        if (call.getString("ssid") != null && !call.getString("ssid").equals("")) {
            ssid = call.getString("ssid");
        } else {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.ssid.missing");
            call.success(object);
        }

        boolean granted = getPermission(Manifest.permission.ACCESS_FINE_LOCATION);

        LocationManager lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        boolean location = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!location) {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.location.disabled");
            call.success(object);
        } else if (granted) {
            WifiManager wifiManager = getWifiManager();
            WifiInfo info = wifiManager.getConnectionInfo();
            String currentlySsid = info.getSSID();
            if (currentlySsid != null && (currentlySsid.equals("\"" + ssid + "\"") || currentlySsid.equals(ssid))) { // TODO document why ssid can be surrounded by quotes
                isConnected = true;
            }

            String message = isConnected ? "plugin.wifieapconfigurator.success.network.connected" : "plugin.wifieapconfigurator.error.network.notConnected";

            JSObject object = new JSObject();
            object.put("success", true);
            object.put("message", message);
            object.put("isConnected", isConnected);
            call.success(object);
        } else {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.permission.notGranted");
            call.success(object);
        }

    }

    private boolean getNetworkAssociated(PluginCall call, String ssid) {
        boolean res = true, isOverridable = false;

        //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        WifiManager wifi = getWifiManager();
        List<WifiConfiguration> configuredNetworks = wifi.getConfiguredNetworks();

        for (WifiConfiguration conf : configuredNetworks) {
            if (conf.SSID.equals(ssid) || conf.SSID.equals("\"" + ssid + "\"")) { // TODO document why ssid can be surrounded by quotes
                String packageName = getContext().getPackageName();
                if (conf.toString().toLowerCase().contains(packageName.toLowerCase())) { // TODO document why case insensitive
                    isOverridable = true;
                }

                JSObject object = new JSObject();
                object.put("success", false);
                object.put("message", "plugin.wifieapconfigurator.error.network.alreadyAssociated");
                object.put("overridable", isOverridable);
                call.success(object);
                res = false;
                break;
            }
        }
        //}
        return res;
    }

    @PluginMethod
    public boolean checkEnabledWifi(PluginCall call) {
        boolean res = true;
        WifiManager wifi = getWifiManager();

        if (!wifi.isWifiEnabled()) {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.wifi.disabled");
            call.success(object);
            res = false;
        }
        return res;
    }

    private Integer getEapMethod(Integer eap) {
        switch (eap) {
            case 13: return WifiEnterpriseConfig.Eap.TLS;
            case 21: return WifiEnterpriseConfig.Eap.TTLS;
            case 25: return WifiEnterpriseConfig.Eap.PEAP;
            default: return null;
        }
    }

    private Integer getAuthMethod(Integer auth) {
        switch (auth) {
            case -1: return WifiEnterpriseConfig.Phase2.PAP;
            case -2: return WifiEnterpriseConfig.Phase2.MSCHAP;
            case -3:
            case 26: /* Android cannot do PEAP-EAP-MSCHAPv2, we expect the ionic code to not let it happen, but if it does, try PEAP-MSCHAPv2 instead */
                return WifiEnterpriseConfig.Phase2.MSCHAPV2;
            /*
            case _:
                return WifiEnterpriseConfig.Phase2.GTC;
            */
            default: return null;
        }
    }

    boolean getPermission(String permission) {
        boolean res = true;
        if (!(checkSelfPermission(getContext(), permission) == PackageManager.PERMISSION_GRANTED)) {
            res = false;
            ActivityCompat.requestPermissions(getActivity(), new String[]{permission}, 123);
        }

        return res;
    }

    private void verifyCaCert(X509Certificate caCert)
            throws GeneralSecurityException, IOException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        CertPathValidator validator =
                CertPathValidator.getInstance(CertPathValidator.getDefaultType());
        CertPath path = factory.generateCertPath(Arrays.asList(caCert));
        KeyStore ks = KeyStore.getInstance("AndroidCAStore");
        ks.load(null, null);
        PKIXParameters params = new PKIXParameters(ks);
        params.setRevocationEnabled(false);
        validator.validate(path, params);
    }

    private byte[] getFingerprint(X509Certificate certChain) {

        MessageDigest digester = null;
        byte[] fingerprint = null;
        try {
            digester = MessageDigest.getInstance("SHA-256");
            digester.reset();
            fingerprint = digester.digest(certChain.getEncoded());
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            e.printStackTrace();
        }
        return fingerprint;
    }

    private static String getLongestSuffix(String[] strings) {
        if (strings.length == 0) return "";
        if (strings.length == 1) return strings[0];
        String longest = strings[0];
        for(String candidate : strings) {
            int pos = candidate.length();
            do {
                pos = candidate.lastIndexOf('.', pos - 2) + 1;
            } while (pos > 0 && longest.endsWith(candidate.substring(pos)));
            if (!longest.endsWith(candidate.substring(pos))) {
                pos = candidate.indexOf('.', pos);
            }
            if (pos == -1) {
                longest = "";
            } else if (longest.endsWith(candidate.substring(pos))) {
                longest = candidate.substring(pos == 0 ? 0 : pos + 1);
            }
        }
        return longest;
    }

}
