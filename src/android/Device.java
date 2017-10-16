
package org.apache.cordova.device;


import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import java.util.HashMap;

public class Device extends CordovaPlugin {
    private final byte START_SYSEX = (byte) 0xF0;
    private final byte END_SYSEX = (byte) 0xF7;
    private String monitorString = "";
    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private UsbInterface usbIf;
    private UsbEndpoint usbEpOut;
    private UsbDeviceConnection connection;
    private boolean isDeviceReady;

    public Device() {
    }

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action          The action to execute.
     * @param args            JSONArry of arguments for the plugin.
     * @param callbackContext The callback id used when calling back into JavaScript.
     * @return True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if ("テストアクション".equals(action)) {
            JSONObject r = new JSONObject();
            //keyにスペースや日本語を使ってはいけない(jsで呼び出すとき厄介なので)
            r.put("debug", "レスポンステスト");
            r.put("key3", args);
            callbackContext.success(r);
        } else if ("connect".equals(action)) {
            JSONObject r = new JSONObject();
            r.put("debug", "接続確認@connect");
            if (connect()) r.put("usb", usbDevice.toString());
            callbackContext.success(r);
        } else if ("miniSend".equals(action)) {
            JSONObject r = new JSONObject();
//            r.put("debug", "接続確認@miniSend");
            String str = (String)args.get(0);
            r.put("debug",str);
            byte data[]= hexStringToByteArray(str);
            r.put("success", "success@miniSend");
            miniSend(data);
            callbackContext.success(r);
        } else {
            return false;
        }
        return true;
    }


    /////local methods/////
    private boolean connect() {
        usbManager = (UsbManager) cordova.getActivity().getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            return false;
        }
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        for (String name : deviceList.keySet()) {
            UsbDevice usbDevice = deviceList.get(name);
            if (usbDevice.getInterfaceCount() >= 2) {
                this.usbDevice = usbDevice;
                UsbInterface usbIf = usbDevice.getInterface(1);
                for (int i = 0; i < usbIf.getEndpointCount(); i++) {
                    UsbEndpoint usbEp = usbIf.getEndpoint(i);
                    if (usbEp.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (usbEp.getDirection() == UsbConstants.USB_DIR_OUT) {
                            usbEpOut = usbEp;
                            this.usbIf = usbIf;
                            if (!usbManager.hasPermission(usbDevice)) {
                                usbManager.requestPermission(usbDevice,
                                        PendingIntent.getBroadcast(cordova.getActivity().getApplicationContext(), 0, new Intent("usbPermissionDialog"), 0));
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    /**miniSend(byte)
     * 受け取ったdataを7bitデータとして扱い、前後に区切りbyteを付けてシリアル送信。
     * つまり、[0xF0, B0???????を受け取った数だけ, 0xF7] を送信*/
    private boolean miniSend(byte[] data) {
        if (usbDevice == null) return false;
        if (!usbManager.hasPermission(usbDevice)) return false;
        if (!isDeviceReady) sendControlSignal();

        if (data.length > 32) return false;
        byte[] writeData = new byte[data.length + 2];
        writeData[0] = START_SYSEX;

        for (int i = 0; i < data.length; i++) {
            writeData[i + 1] = (byte) (data[i] & 0x7f); //先頭ビット0にする
        }

        writeData[writeData.length - 1] = END_SYSEX;
        int result = connection.bulkTransfer(usbEpOut,writeData, writeData.length, 0);
        return true;
    }

    private void sendControlSignal() {
        connection = usbManager.openDevice(usbDevice);
        boolean result = connection.claimInterface(usbIf, true);
        //下の二つを送ると送受信用のエンドポイントが通るようになるらしい
        connection.controlTransfer(0x21, 34, 0, 0, null, 0, 0);
        connection.controlTransfer(0x21, 32, 0, 0, new byte[]{
                (byte) 0x80, 0x25, 0x00, 0x00, 0x00, 0x00, 0x08
        }, 7, 0);
        isDeviceReady = true;
    }


    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    /////DEBUG/////
    private String monitor(String str) {
        monitorString = str + "___" + monitorString;
        return monitorString;
    }

}
