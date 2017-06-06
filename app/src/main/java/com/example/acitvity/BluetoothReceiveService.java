package com.example.acitvity;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;



public class BluetoothReceiveService{


    private static final UUID MY_UUID =UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    //客户端
    //蓝牙的连接状态
    public static final int STATE_NONE = 0;
    //蓝牙收索
    public static final int STATE_LISTEN = 1;
    //正在连接
    public static final int STATE_CONNECTING = 2;
    //已经连接
    public static final int STATE_CONNECTED = 3;

    public BluetoothReceiveService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    private synchronized void setState(int state) {
        mState = state;
        //获取消息发送到目标,是不是连接上
        mHandler.obtainMessage(1, state, -1).sendToTarget();
    }

    public synchronized int getState() {
        return mState;
    }

    public synchronized void start()
    {
        if(mConnectedThread!=null)
        {
            mConnectedThread.cancel();
            mConnectedThread=null;
        }

        if(mConnectThread!=null)
        {
            mConnectThread.cancel();
            mConnectThread=null;
        }

        setState(STATE_LISTEN);

        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }

    }

    //连接
    public synchronized void connect(BluetoothDevice device, boolean secure) {

        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }


        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        //发送连接的名字给ui
        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(4);
        Bundle bundle = new Bundle();
        bundle.putString("Device Name:", device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    public synchronized void stop() {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    //连接失败
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(5);
        Bundle bundle = new Bundle();
        bundle.putString("TOAST", "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothReceiveService.this.start();
    }

    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(5);
        Bundle bundle = new Bundle();
        bundle.putString("Toast", "蓝牙丢失");
        msg.setData(bundle);
        //立刻发送消息
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothReceiveService.this.start();
    }

    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord("BluetoothReceiveService",
                            MY_UUID);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            "BluetoothReceiveService", MY_UUID);
                }
            } catch (IOException e) {

            }
            mmServerSocket = tmp;
        }

        public void run() {

            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {

                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothReceiveService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(),
                                        mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {

                                }
                                break;
                        }
                    }
                }
            }


        }

        public void cancel() {

            try {
                mmServerSocket.close();
            } catch (IOException e) {

            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     * 该线程在尝试与设备进行传出连接时运行。 它直奔 连接成功或失败。
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = "Secure";
            try {

                tmp = device.createRfcommSocketToServiceRecord(
                        MY_UUID);

            } catch (IOException e) {

            }
            mmSocket = tmp;
        }

        public void run() {

            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {

                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothReceiveService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {

            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     * 此线程在与远程设备的连接期间运行。它处理所有传入和传出传输。
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        public ConnectedThread(BluetoothSocket socket, String socketType) {

            mmSocket = socket;
            InputStream tmpIn = null;
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("Bluetooth","not get data");
            }
            mmInStream = tmpIn;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(2, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {

                    connectionLost();
                    // Start the service over to restart listening mode
                    BluetoothReceiveService.this.start();
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {

            }
        }
    }
}
