package cn.ac.iscas.xlab.droidfacedog;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.ac.iscas.xlab.droidfacedog.entity.UserInfo;

/**
 * Created by lisongting on 2017/7/11.
 */

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.UserItemViewHolder> {

    public static final String TAG = "UserListAdapter";
    private LayoutInflater inflater;
    private Context context;
    private List<UserInfo> userInfoList;

    public UserListAdapter(Context context){
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        userInfoList = new ArrayList<>();

    }
    public UserListAdapter(Context context,
                           List<UserInfo> userInfoList){
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.userInfoList = userInfoList;

    }
    @Override
    public UserItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.item_user, parent, false);
        UserItemViewHolder holder = new UserItemViewHolder(itemView);
        return holder;
    }

    @Override
    public void onBindViewHolder(UserItemViewHolder holder, int position) {

        UserInfo info = userInfoList.get(position);
        Bitmap bitmap = info.getFace();
        String name = info.getName();
        holder.userImg.setImageBitmap(bitmap);
        holder.userName.setText(name);

    }

    @Override
    public int getItemCount() {
        if (userInfoList == null) {
            return 0;
        } else {
            return userInfoList.size();
        }
    }

    public void addUser(UserInfo user) {
        int size = userInfoList.size();

        if (size == 0) {
            userInfoList.add(user);
            notifyDataSetChanged();
        }else  {
            //先检查列表中是否已经有相同的用户，如果有，则不添加了
            for(int i=0;i<size;i++) {
                UserInfo tmp = userInfoList.get(i);
                if (user.getName().equals(tmp.getName())) {
                    break;
                }else if(i==size-1){
                    userInfoList.add(user);
                    notifyDataSetChanged();
                }
            }
        }
//        Log.i(TAG, "userInfoList:"+userInfoList.toString());


    }

    public class UserItemViewHolder extends RecyclerView.ViewHolder{

        private ImageView userImg;
        private TextView userName;

        public UserItemViewHolder(View itemView) {
            super(itemView);
            userImg = (ImageView) itemView.findViewById(R.id.tv_user_head);
            userName = (TextView) itemView.findViewById(R.id.tv_user_name);
            userImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }
    }
}
