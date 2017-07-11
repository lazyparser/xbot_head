package cn.ac.iscas.xlab.droidfacedog;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by lisongting on 2017/7/11.
 */

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.UserItemViewHolder> {

    private LayoutInflater inflater;
    private Context context;
    private List<Bitmap> userBitmapList;
    private List<String> userNameList;

    public UserListAdapter(Context context,
                           List<Bitmap> userBitmapList,
                           List<String> userNameList){
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.userBitmapList = userBitmapList;
        this.userNameList = userNameList;

    }
    @Override
    public UserItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.item_user, parent, false);
        UserItemViewHolder holder = new UserItemViewHolder(itemView);
        return holder;
    }

    @Override
    public void onBindViewHolder(UserItemViewHolder holder, int position) {
        Bitmap bitmap = userBitmapList.get(position);
        String name = userNameList.get(position);
        holder.userImg.setImageBitmap(bitmap);
        holder.userName.setText(name);

    }

    @Override
    public int getItemCount() {
        return userNameList.size();
    }

    public class UserItemViewHolder extends RecyclerView.ViewHolder{

        private ImageView userImg;
        private TextView userName;

        public UserItemViewHolder(View itemView) {
            super(itemView);
            userImg = (ImageView) itemView.findViewById(R.id.tv_user_head);
            userName = (TextView) itemView.findViewById(R.id.tv_user_name);
        }
    }
}
