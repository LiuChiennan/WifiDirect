package com.miko.zd.mywifiderect.Service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by 刘建南 on 2017/12/19.
 */

public class DataReceiveService extends Service {
    private Messenger mMessebger;

    @Override
    public IBinder onBind(Intent intent){
        return null;
    }

    @Override
    public void onCreate(){
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent,int flags, int startId){
        mMessebger = (Messenger)intent.getExtras().get("messenger");
        Log.i("xyz", "data doinback");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    ServerSocket serverSocket = new ServerSocket(8888);
                    System.out.println("start data receive service!");

                    while(true){
                        Log.i("xyz","串口创建完成");
                        Socket client = serverSocket.accept();
                        Log.i("xyz","阻塞已取消");
                        System.out.println("数据接收阻塞已取消");
                        new ReceiveData(client).start();
                    }
                }catch (IOException e){
                    System.out.println("接受进行错误");
                }
            }
        }).start();
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    class ReceiveData extends Thread{
        private Socket mSocket;
        public ReceiveData(Socket socket){
            this.mSocket = socket;
        }

        @Override
        public void run(){
            String str = "";
            try{
                Log.i("xyz","阻塞已取消");
                InputStream inputstream = mSocket.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int i;
                while ((i = inputstream.read()) != -1) {
                    baos.write(i);
                }

                str = baos.toString();
                mSocket.close();
            }catch (IOException e){
                System.out.println("data server socket IO exception!");
            }finally {
                try{
                    Message message = new Message();
                    message.what = 5;
                    message.obj = str;
                    mMessebger.send(message);
                }catch (Exception e){
                    System.out.println("send messenger error!");
                }
            }
        }
    }
}
