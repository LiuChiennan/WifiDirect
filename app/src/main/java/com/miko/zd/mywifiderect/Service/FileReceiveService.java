package com.miko.zd.mywifiderect.Service;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by 刘建南 on 2017/12/19.
 */

public class FileReceiveService extends Service {
    private Messenger mMessenger;

    @Override
    public IBinder onBind(Intent intent){
        return null;
    }

    @Override
    public void onCreate(){
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        mMessenger = (Messenger)intent.getExtras().get("messenger");
        Log.i("xyz", "file doinback");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    ServerSocket serverSocket = new ServerSocket(8988);
                    System.out.println("start file receive service!");
                    while(true){
                        Socket client = serverSocket.accept();
                        System.out.println("文件接受阻塞取消");
                        new ReceFileThread(client).start();
                    }
                }catch (IOException e){
                    System.out.println("create file receiver socket error!");
                }

            }
        }).start();

        return super.onStartCommand(intent,flags,startId);
    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    class ReceFileThread extends Thread{
        private Socket mSocket;
        public ReceFileThread(Socket socket){this.mSocket = socket;}

        @Override
        public void run(){
            String result = "";
            try{
                final File f = new File(
                        Environment.getExternalStorageDirectory()
                                + "/wifip2pshared-"
                                + System.currentTimeMillis() + ".mp4");

                File dirs = new File(f.getParent());

                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();


                /*Returns an input stream to read data from this socket*/
                InputStream inputstream = mSocket.getInputStream();
                System.out.println("begin receive file!");
                copyFile(inputstream, new FileOutputStream(f));
                result = f.getAbsolutePath();
            }catch (IOException e){
                System.out.println("file receive error!");
            }finally {
                try{
                    Message message = new Message();
                    message.what = 6;
                    message.obj = result;
                    mMessenger.send(message);
                    mSocket.close();
                }catch (Exception e){
                    System.out.println("file receiver send message error!");
                }
            }
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }
}
