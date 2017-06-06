package com.example.acitvity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;


public class BluetoothReceive extends ActionBarActivity {

    //请求连接设备的状态、安全、不安全、启用
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private int read_flag=0;
    private String finalResult="";
    private String readArray="";
    private Button getData;
    private Button returnData;

    // Layout Views
    private ListView mConversationView;

    //已经连接的设备
    private String mConnectedDeviceName = null;

    private ArrayAdapter<String> mConversationArrayAdapter;

    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothReceiveService mReceiveService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_receive);
        getData = (Button) findViewById(R.id.btnGetData);
        returnData=(Button)findViewById(R.id.btn_return_data);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mConversationView = (ListView) findViewById(R.id.in);
        if (mBluetoothAdapter == null) {
            Toast.makeText(BluetoothReceive.this, "蓝牙不存在", Toast.LENGTH_SHORT).show();
            BluetoothReceive.this.finish();
        }

        //获取蓝牙信息
        getData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serverIntent = new Intent(BluetoothReceive.this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
            }
        });

        //返回结果数据
        returnData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String uriString = finalResult;
                Uri data = Uri.parse(uriString);
                Intent result = new Intent(null, data);
                setResult(RESULT_OK, result);
                finish();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else if (mReceiveService == null) {
            setupReceive();
        }
    }

    /**
     *  开始接受
     */
    private void setupReceive()
    {
        mConversationArrayAdapter = new ArrayAdapter<String>(BluetoothReceive.this, R.layout.message);

        mConversationView.setAdapter(mConversationArrayAdapter);
        //初始化构造函数
        mReceiveService=new BluetoothReceiveService(BluetoothReceive.this,mHandler);
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    //处理消息
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if(read_flag==0) {
                    switch (msg.what) {
                        case 1:
                            switch (msg.arg1) {
                                case BluetoothReceiveService.STATE_CONNECTED:
                                    mConversationArrayAdapter.clear();
                                    break;
                                case BluetoothReceiveService.STATE_CONNECTING:
                                    break;
                                case BluetoothReceiveService.STATE_LISTEN:
                                case BluetoothReceiveService.STATE_NONE:
                                    break;
                            }
                            break;
                        case 3:
                            break;
                        case 2:
                            byte[] readBuf = (byte[]) msg.obj;
                            // construct a string from the valid bytes in the buffer
                            String readMessage = new String(readBuf, 0, msg.arg1);
                            readArray += readMessage;
                            if (readArray.charAt(0) == 'S' && readArray.charAt(readArray.length() - 1) == 'D') {
                                finalResult = getData_from_byte(readArray);
                            }
                            if (!finalResult.equals("")) {
                                read_flag = 1;
                                //设置按钮可见
                                returnData.setVisibility(View.VISIBLE);
                            }

                            //加在界面上而且要判断和是不是一致的
                            if(readMessage!=null&&(readArray.charAt(0) == 'S' && readArray.charAt(readArray.length() - 1) == 'D')){
//                                if(checkResult(finalResult)==true)
                                mConversationArrayAdapter.add(finalResult);
                            }

                            Toast.makeText(BluetoothReceive.this, "接收数据", Toast.LENGTH_LONG).show();
                            break;
                        case 4:
                            // save the connected device's name
                            mConnectedDeviceName = msg.getData().getString("device_name");
                            if (null != BluetoothReceive.this) {
                                Toast.makeText(BluetoothReceive.this, "Connected to "
                                        + mConnectedDeviceName, Toast.LENGTH_LONG).show();
                            }
                            break;
                        case 5:
                            if (null != BluetoothReceive.this) {
                                Toast.makeText(BluetoothReceive.this, msg.getData().getString("toast"),
                                        Toast.LENGTH_SHORT).show();
                            }
                            break;
                    }
            }
        }
    };


    //读取数据,解析传来的字符串
    private String getData_from_byte(String ss)
    {
        String tempS="";
        int flagStart=0,flagEnd=0;
        int indexStart=0,indexEnd=0;
        for(int i=0;i<ss.length();i++) {
            if (ss.charAt(i) == 'S') {
                indexStart = i;
                flagStart=1;
            }
            if (ss.charAt(i) == 'D') {
                indexEnd = i;
                flagEnd=1;
                break;
            }
        }
        if(flagEnd*flagStart!=0)
        {
            for(int i=indexStart;i<=indexEnd;i++)
                tempS+=ss.charAt(i);
        }
        return tempS;
    }

    //检查结果和对不对
    private boolean checkResult(String finalResult)
    {
        int flag=0;
        if(finalResult.equals(""))
            return false;
        else {

            String[] strings=finalResult.split(" ");
            int i=strings.length-1;
            if(strings[i-1].equals("1122")){
                return true;
            }else {
                return false;
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupReceive();
                } else {
                    // User did not enable Bluetooth or an error occurred

                    Toast.makeText(BluetoothReceive.this, "bluetooth is not able to stopֹͣ",
                            Toast.LENGTH_SHORT).show();
                    BluetoothReceive.this.finish();
                }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mReceiveService.connect(device, secure);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mReceiveService != null) {
            mReceiveService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mReceiveService != null) {
            if (mReceiveService.getState() == BluetoothReceiveService.STATE_NONE) {
                mReceiveService.start();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bluetooth_receive, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
