package demo.zouyan.com.socketdemo;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

/**
 * Created by Boosal on 2017/10/26.
 */

public class DanmuAdapter extends BaseAdapter {
    private List<Map<String,Object>> list;
    private Context context;

    public DanmuAdapter(List<Map<String, Object>> list, Context context) {
        this.list = list;
        this.context = context;
    }


    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView==null){
            convertView = LayoutInflater.from(context).inflate(R.layout.item_lv_danmu,null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }else {
            holder = (ViewHolder) convertView.getTag();
        }
        Map<String,Object> map = list.get(position);
        holder.tv_txt.setText((String) map.get("txt"));
        holder.tv_nn.setText((String) map.get("nn")+" : ");
//        String nl = (String) map.get("nl");
//        if(nl!=null&&Integer.parseInt(nl)>0){
//            holder.ll_content.setBackgroundColor(getRandomColor());
//        }
        if(position==list.size()-1){
            isRefresh = false;
        }
        return convertView;
    }
    class ViewHolder{
        TextView tv_nn;
        TextView tv_txt;
        LinearLayout ll_content;
        public ViewHolder(View itemView) {
            tv_nn = itemView.findViewById(R.id.tv_nn);
            tv_txt = itemView.findViewById(R.id.tv_txt);
            ll_content = itemView.findViewById(R.id.ll_content);
        }
    }

    private boolean isRefresh = false;

    public boolean isRefresh() {
        return isRefresh;
    }

    public void chagreRefreshState() {
        isRefresh = !isRefresh;
    }
    private int getRandomColor() {
        SecureRandom rgen = new SecureRandom();
        return Color.HSVToColor(150, new float[]{
                rgen.nextInt(359), 1, 1
        });
    }
}
