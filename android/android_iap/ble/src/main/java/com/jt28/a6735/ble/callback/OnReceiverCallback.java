package com.jt28.a6735.ble.callback;

/**
 * 描述:接收设备向手机发送的广播数据
 */
public interface OnReceiverCallback {

    void onRecive(byte[] value);
}
