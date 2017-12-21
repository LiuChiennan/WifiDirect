package com.miko.zd.mywifiderect.Activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.miko.zd.mywifiderect.Adapter.MyAdapter;
import com.miko.zd.mywifiderect.BroadcastReceiver.WifiDirectBroadcastReceiver;
import com.miko.zd.mywifiderect.NetworkFlowUnity.NetworkSpeedUnity;
import com.miko.zd.mywifiderect.R;
import com.miko.zd.mywifiderect.Service.DataReceiveService;
import com.miko.zd.mywifiderect.Service.DataTransferService;
import com.miko.zd.mywifiderect.Service.FileReceiveService;
import com.miko.zd.mywifiderect.Service.FileTransferService;
import com.miko.zd.mywifiderect.Task.DataServerAsyncTask;
import com.miko.zd.mywifiderect.Task.FileServerAsyncTask;
import com.miko.zd.mywifiderect.Utils.Utils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final String TAG="wifi-direct";
    private String connectedDevice = "5a:7f:66:d5:06:2e";

    private Button conState;
    private Button iden;
    private Button discover;
    private Button stopdiscover;
    private Button stopconnect;
    private Button sendvideo;
    private Button senddata;
    private Button begrouppwener;

    private TextView speed;
    private TextView infoText;

    private AlertDialog.Builder builder;
    private EditText editText;

    private RecyclerView mRecyclerView;
    private MyAdapter mAdapter;
    private List peers = new ArrayList();
    private List<HashMap<String, String>> peersshow = new ArrayList();

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private BroadcastReceiver mReceiver;
    private IntentFilter mFilter;
    private WifiP2pInfo info;

    private FileServerAsyncTask mServerTask;
    private DataServerAsyncTask mDataTask;

    private NetworkSpeedUnity networkSpeedUnity;

    Handler mHandler;
    Messenger mMessenger;

    private Utils utils;

    private boolean isConnect;

    public static final String REMOVE_GROUP = "REMOVE_GROUP";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initIntentFilter();
        initReceiver();
        initEvents();

    }
    @Override
    public void onStart(){
        super.onStart();
        SetButtonGone();
        Intent dataRecService = new Intent(this, DataReceiveService.class);
        dataRecService.putExtra("messenger",mMessenger);
        startService(dataRecService);

        Intent fileRecService = new Intent(this, FileReceiveService.class);
        fileRecService.putExtra("messenger",mMessenger);
        startService(fileRecService);
    }

    private void initView() {

        speed = (TextView)findViewById(R.id.speed);
        infoText = (TextView)findViewById(R.id.tv_main);
        conState = (Button)findViewById(R.id.conState);
        iden = (Button)findViewById(R.id.iden);
        begrouppwener= (Button) findViewById(R.id.bt_bgowner);
        stopdiscover = (Button) findViewById(R.id.bt_stopdiscover);
        discover = (Button) findViewById(R.id.bt_discover);
        stopconnect = (Button) findViewById(R.id.bt_stopconnect);
        sendvideo = (Button) findViewById(R.id.bt_sendpicture);
        senddata = (Button) findViewById(R.id.bt_senddata);
        //SetButtonGone();

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview);

        mAdapter = new MyAdapter(peersshow);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager
                (this.getApplicationContext()));




    }

    private void initIntentFilter() {
        mFilter = new IntentFilter();
        mFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    //初始化wifip2pmanager，设置两个监听器
    private void initReceiver() {
        mHandler = new Handler(){
            public void handleMessage(Message mess){
                int i = mess.what;
                //广播监听器
                if(i == 1){
                    isConnect = true;
                    conState.setText("已连接");
                    SetButtonVisible();
                }
                //广播监听器
                else if(i == 0){
                    isConnect = false;
                    SetButtonGone();
                    conState.setText("未连接");
                }
                //广播监听器
                else if(i == 2){
                    iden.setText("GO");
                }
                //广播监听器
                else if(i == 3){
                    iden.setText("GC");
                }
                else if(i == 4){
                    DiscoverPeers();
                }
                //数据接受的service的消息
                else if(i == 5){
                    String infomation = String.valueOf(mess.obj);
                    if(infomation.equals(REMOVE_GROUP)){
                        RemoveGroups();
                    }else{
                        infoText.setText(infomation);
                    }
                }
                //文件接受service消息处理
                else if(i == 6){
                    String filePath = String.valueOf(mess.obj);
                    infoText.setText(filePath);
                }
                //网络流量消息
                else if(i == 20){
                    String rate = String .valueOf(mess.obj);
                    speed.setText(rate);
                }
            }
        };

        mMessenger = new Messenger(mHandler);

        networkSpeedUnity = new NetworkSpeedUnity(this,mHandler);
        networkSpeedUnity.startShowNetwork();

        mManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, Looper.myLooper(), null);

        WifiP2pManager.PeerListListener mPeerListListerner = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peersList) {
                peers.clear();
                peersshow.clear();
                Collection<WifiP2pDevice> aList = peersList.getDeviceList();
                peers.addAll(aList);

                for (int i = 0; i < aList.size(); i++) {
                    WifiP2pDevice a = (WifiP2pDevice) peers.get(i);
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("name", a.deviceName);
                    map.put("address", a.deviceAddress);
                    peersshow.add(map);
                }
                mAdapter = new MyAdapter(peersshow);
                mRecyclerView.setAdapter(mAdapter);
                mRecyclerView.setLayoutManager(new LinearLayoutManager
                        (MainActivity.this));
                mAdapter.SetOnItemClickListener(new MyAdapter.OnItemClickListener() {
                    @Override
                    public void OnItemClick(View view, int position) {
                        CreateConnect(peersshow.get(position).get("address"),
                                peersshow.get(position).get("name"));

                    }

                    @Override
                    public void OnItemLongClick(View view, int position) {

                    }
                });
            }
        };

        WifiP2pManager.ConnectionInfoListener mInfoListener = new WifiP2pManager.ConnectionInfoListener() {

            @Override
            public void onConnectionInfoAvailable(final WifiP2pInfo minfo) {

                Log.i("xyz", "InfoAvailable is on");
                info = minfo;
                //TextView view = (TextView) findViewById(R.id.tv_main);
                if (info.groupFormed && info.isGroupOwner) {
                    SetButtonGone();
                    Log.i("xyz", "owmer start");
                    iden.setText("GO");
                    /*mServerTask = new FileServerAsyncTask(MainActivity.this, infoText);
                    mServerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);*/

                    /*mDataTask = new DataServerAsyncTask(MainActivity.this, infoText);
                    mDataTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
*/                }else if(!info.isGroupOwner){
                    iden.setText("GC");
                }else{
                    iden.setText("");
                }
            }
        };
        mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, this, mPeerListListerner, mInfoListener,mHandler);
    }

    private void initEvents() {

        discover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DiscoverPeers();
            }
        });
        begrouppwener.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BeGroupOwener();
            }
        });

        stopdiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StopDiscoverPeers();
            }
        });
        stopconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StopConnect();
            }
        });
        sendvideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/mp4");
                startActivityForResult(intent, 20);

            }
        });

        senddata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try{
                    editText = new EditText(MainActivity.this);
                    editText.setHint("请输入消息");
                    editText.setTextColor(getResources().getColor(R.color.orchid));
                    //builder = new AlertDialog.Builder(MainActivity.this);
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage("请输入需要发送的信息")
                            .setView(editText)
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    sendInfo(editText.getText().toString());
                                    dialogInterface.dismiss();
                                }
                            })
                            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    editText = null;
                                    dialogInterface.dismiss();
                                }
                            })
                            .show();

                }catch (Exception e){
                    System.out.println("view error!");
                }
            }
        });


        mAdapter.SetOnItemClickListener(new MyAdapter.OnItemClickListener() {
            @Override
            public void OnItemClick(View view, int position) {
                CreateConnect(peersshow.get(position).get("address"),
                        peersshow.get(position).get("name"));
            }

            @Override
            public void OnItemLongClick(View view, int position) {
            }
        });
    }

    private void sendInfo(String res){
        if(res != null){
            if(isConnect){
                System.out.println("begin send\t" + res);
                Intent serviceIntent = new Intent(MainActivity.this,
                        DataTransferService.class);

                serviceIntent.setAction(DataTransferService.ACTION_SEND_FILE);

                InetAddress GOAddressInet = info.groupOwnerAddress;
                String GOAddress = GOAddressInet.getHostAddress();

                serviceIntent.putExtra("info",res);

                serviceIntent.putExtra(DataTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                        GOAddress);
                Log.d(TAG, "owenerip is " + info.groupOwnerAddress.getHostAddress());
                serviceIntent.putExtra(DataTransferService.EXTRAS_GROUP_OWNER_PORT,
                        8888);
                MainActivity.this.startService(serviceIntent);
                System.out.println("start service!");
            }else{
                Toast.makeText(this,"Disconnected! Please connect first",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void SetButtonGone() {
        sendvideo.setVisibility(View.GONE);
        senddata.setVisibility(View.GONE);
    }

    private void BeGroupOwener() {
        //让GO端移除掉group
        sendInfo(REMOVE_GROUP);
        RemoveGroups();
        mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {

            }
        });
    }

    private void RemoveGroups(){
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode == 20) {
            super.onActivityResult(requestCode, resultCode, data);
            Uri uri = data.getData();
            Intent serviceIntent = new Intent(MainActivity.this,
                    FileTransferService.class);

            serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
            serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH,
                    uri.toString());

            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                    info.groupOwnerAddress.getHostAddress());
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT,
                    8988);
            MainActivity.this.startService(serviceIntent);
        } else{
            Toast.makeText(this,"未选取文件",Toast.LENGTH_SHORT).show();
        }
    }

    private void StopConnect() {
        SetButtonGone();
        RemoveGroups();
        DiscoverPeers();
    }

    /*A demo base on API which you can connect android device by wifidirect,
    and you can send file or data by socket,what is the most important is that you can set
    which device is the client or service.*/

    private void CreateConnect(String address, final String name) {
        WifiP2pDevice device;
        WifiP2pConfig config = new WifiP2pConfig();
        Log.d(TAG, address);

        config.deviceAddress = address;
        /*mac地址*/

        config.wps.setup = WpsInfo.PBC;
        Log.d(TAG, "MAC IS " + address);
        if (address.equals("9a:ff:d0:23:85:97")) {
            config.groupOwnerIntent = 0;
            Log.d("address", "lingyige shisun");
        }
        if (address.equals("36:80:b3:e8:69:a6")) {
            config.groupOwnerIntent = 15;
            Log.i("address", "lingyigeshiwo");

        }

        Log.d("address", "lingyige youxianji" + String.valueOf(config.groupOwnerIntent));

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {


            }
        });
        //Toast.makeText(this,"已连接",Toast.LENGTH_SHORT).show();
    }

    private void StopDiscoverPeers() {
        mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {


            }
        });
    }

    private void SetButtonVisible() {
        sendvideo.setVisibility(View.VISIBLE);
        senddata.setVisibility(View.VISIBLE);
    }

    private void DiscoverPeers() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(int reason) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("xyz", "hehehehehe");
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        StopConnect();
    }

    public void ResetReceiver() {

        unregisterReceiver(mReceiver);
        registerReceiver(mReceiver, mFilter);

    }
}
