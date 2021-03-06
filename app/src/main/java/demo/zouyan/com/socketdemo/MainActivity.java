package demo.zouyan.com.socketdemo;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // Socket变量
    private Socket socket;

    //第三方弹幕协议服务器地址
    private static final String hostname = "openbarrage.douyutv.com";

    //第三方弹幕协议服务器端口
    private static final int port = 8601;

    //设置字节获取buffer的最大值
    private static final int MAX_BUFFER_LENGTH = 4096;

    private BufferedInputStream in;
    private BufferedOutputStream out;
    //获取弹幕线程及心跳线程运行和停止标记
    private boolean readyFlag = false;
    private List<Map<String, Object>> list;
    private LinearLayout ll_head;
    private TextView tv_txt;
    private TextView tv_nn;
    boolean isShowing;//是否正在显示

    private DanmuAdapter adapter;
    private ListView lv_danmu;
    private List<Map<String, Object>> saveList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window window = getWindow();
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            View statusBar = (View) findViewById(R.id.SysStatusBar);
            ViewGroup.LayoutParams layoutParams = statusBar.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = getStatusHeight(this);
            statusBar.setLayoutParams(layoutParams);
        }

        lv_danmu = (ListView) findViewById(R.id.lv_danmu);
        ll_head = (LinearLayout) findViewById(R.id.ll_head);
        tv_nn = (TextView) findViewById(R.id.tv_nn);
        tv_txt = (TextView)findViewById(R.id.tv_txt);
        list = new ArrayList<>();
        saveList = new ArrayList<>();
        adapter = new DanmuAdapter(list, this);
        lv_danmu.setAdapter(adapter);

        lv_danmu.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if(list.size()==0){
                    return;
                }
                Map<String,Object> map = list.get(firstVisibleItem);
                String nl = (String) map.get("nl");
                if(nl==null){
                    return;
                }
                if(Integer.parseInt(nl)>0&&map.get("isshow")==null){
                    if (!isShowing) {
                        show(firstVisibleItem);
                    }
                }
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                //连接弹幕服务器
                connectServer();
                //登陆指定房间
                EnterRoom(196);
                //加入指定的弹幕池
                joinGroup(196, -9999);
                readyFlag = true;
                KeepAlive keepAlive = new KeepAlive();
                keepAlive.start();
                KeepGetMsg keepGetMsg = new KeepGetMsg();
                keepGetMsg.start();
            }
        }).start();
    }

    //显示悬停布局
    public void show(int position) {
        isShowing = true;
        //设置悬停布局，为了看起来是悬停效果，布局的内容要设置成与ItemView一致
        Map<String, Object> map = list.get(position);
        list.get(position).put("isshow",true);
        tv_txt.setText((String) map.get("txt"));
        tv_nn.setText(map.get("nn") + " : ");
        //添加悬停布局
        ll_head.setVisibility(View.VISIBLE);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                hide();
            }
        },10*1000);
    }

    //隐藏悬停布局
    public void hide() {
        if (ll_head != null) {
            isShowing = false;
            ll_head.setVisibility(View.GONE);
        }
    }


    class KeepAlive extends Thread {
        @Override
        public void run() {
            //获取弹幕客户端

            //判断客户端就绪状态
            while (readyFlag) {
                //发送心跳保持协议给服务器端
                keepAlive();
                try {
                    //设置间隔45秒再发送心跳协议
                    Thread.sleep(45000);        //keep live at least once per minute
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class KeepGetMsg extends Thread {
        @Override
        public void run() {
            //判断客户端就绪状态
            while (readyFlag) {
                //获取服务器发送的弹幕信息
                getServerMsg();
            }
        }
    }

    private void connectServer() {
        try {
            //获取弹幕服务器访问host
            String host = InetAddress.getByName(hostname).getHostAddress();

            //建立socke连接
            socket = new Socket(host, port);

            //设置socket输入及输出
            in = new BufferedInputStream(socket.getInputStream());
            out = new BufferedOutputStream(socket.getOutputStream());
            if (socket.isConnected()) {
                Log.i("TAG", "socket连接成功");
            } else {
                Log.i("TAG", "socket连接成功");

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void EnterRoom(int roomId) {
        try {
            //获取弹幕服务器登陆请求数据包
            byte[] loginRequestData = DyMessage.getLoginRequestData(roomId);

            //发送登陆请求数据包给弹幕服务器
            out.write(loginRequestData, 0, loginRequestData.length);
            out.flush();

            //初始化弹幕服务器返回值读取包大小
            byte[] recvByte = new byte[MAX_BUFFER_LENGTH];
            //获取弹幕服务器返回值
            in.read(recvByte, 0, recvByte.length);

            //解析服务器返回的登录信息
            if (DyMessage.parseLoginRespond(recvByte)) {
                Log.i("TAG", "登录成功");
            } else {
                Log.i("TAG", "登录失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void joinGroup(int roomId, int groupId) {
        //获取弹幕服务器加弹幕池请求数据包
        byte[] joinGroupRequest = DyMessage.getJoinGroupRequest(roomId, groupId);

        try {
            //向弹幕服务器发送加入弹幕池请求数据
            out.write(joinGroupRequest, 0, joinGroupRequest.length);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 服务器心跳连接
     */
    public void keepAlive() {
        //获取与弹幕服务器保持心跳的请求数据包
        byte[] keepAliveRequest = DyMessage.getKeepAliveData((int) (System.currentTimeMillis() / 1000));

        try {
            //向弹幕服务器发送心跳请求数据包
            out.write(keepAliveRequest, 0, keepAliveRequest.length);
            out.flush();
            Log.d("TAG", "keepAlive: Send keep alive request successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("TAG", "keepAlive: Send keep alive request failed!");
        }
    }


    /**
     * 获取服务器返回信息
     */
    public void getServerMsg() {
        //初始化获取弹幕服务器返回信息包大小
        byte[] recvByte = new byte[MAX_BUFFER_LENGTH];
        //定义服务器返回信息的字符串
        String dataStr;
        try {
            //读取服务器返回信息，并获取返回信息的整体字节长度
            int recvLen = in.read(recvByte, 0, recvByte.length);

            //根据实际获取的字节数初始化返回信息内容长度
            byte[] realBuf = new byte[recvLen];
            //按照实际获取的字节长度读取返回信息
            System.arraycopy(recvByte, 0, realBuf, 0, recvLen);
            //根据TCP协议获取返回信息中的字符串信息
            dataStr = new String(realBuf, 12, realBuf.length - 12);

            //对单一数据包进行解析
            MsgView msgView = new MsgView(dataStr);
            //分析该包的数据类型，以及根据需要进行业务操作
            parseServerMsg(msgView.getMessageList());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析从服务器接受的协议，并根据需要订制业务需求
     *
     * @param msg
     */
    private void parseServerMsg(Map<String, Object> msg) {
        if (msg.get("type") != null) {

            //服务器反馈错误信息
            if (msg.get("type").equals("error")) {
                Log.d("TAG", "parseServerMsg: msg.toString()=" + msg.toString());
                //结束心跳和获取弹幕线程
                this.readyFlag = false;
            }

            /***@TODO 根据业务需求来处理获取到的所有弹幕及礼物信息***********/

            //判断消息类型
            if (msg.get("type").equals("chatmsg")) {//弹幕消息
                String info = msg.get("txt").toString();
                Log.i("Info", "弹幕信息：" + info);
                if (adapter.isRefresh()) {
                    saveList.add(msg);
                } else {
                    list.add(msg);
                    list.addAll(saveList);
                    saveList.clear();
                    handler.sendEmptyMessage(0);
                }
            }
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (!adapter.isRefresh()) {
                adapter.chagreRefreshState();
                adapter.notifyDataSetChanged();
            }
        }
    };

    public static int getStatusHeight(Activity activity) {
        int statusHeight = 0;
        Rect localRect = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(localRect);
        statusHeight = localRect.top;
        if (0 == statusHeight) {
            Class<?> localClass;
            try {
                localClass = Class.forName("com.android.internal.R$dimen");
                Object localObject = localClass.newInstance();
                int i5 = Integer.parseInt(localClass.getField("status_bar_height").get(localObject).toString());
                statusHeight = activity.getResources().getDimensionPixelSize(i5);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (NumberFormatException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        return statusHeight;
    }
}
