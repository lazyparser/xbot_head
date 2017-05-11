package cn.ac.iscas.xlab.droidfacedog;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by lisongting on 2017/5/10.
 */

public class SeekbarPreference extends Preference implements SeekBar.OnSeekBarChangeListener{

    private int maxValue = 100;
    private static int progress;
    private SeekBar seekBar;
    private TextView textView;

    public SeekbarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        seekBar = new SeekBar(context,attrs);
        seekBar.setEnabled(true);
        setLayoutResource(R.layout.preference_seekbar);
    }

    public SeekbarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr,0);
    }

    public SeekbarPreference(Context context, AttributeSet attrs) {
        super(context, attrs,0);
    }

    public SeekbarPreference(Context context)  {
        super(context,null);
        Log.i("tag", "SeekbarPreference(Context context) ");
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        super.onCreateView(parent);
        Log.i("tag", "SeekbarPreference----onCreateView");
        View v = LayoutInflater.from(getContext()).inflate(R.layout.preference_seekbar, parent,false);
        setPersistent(true);
        return v;
    }

    @Override
    protected void onBindView(View view) {
        Log.i("tag", "SeekbarPreference----onBindView");
        super.onBindView(view);

        progress = getPersistedInt(60);
        seekBar = (SeekBar) view.findViewById(R.id.id_seekbar_threshold);
        textView = (TextView) view.findViewById(R.id.id_tv_threshold_value);

        seekBar.setMax(maxValue);
        seekBar.setProgress(progress);
        seekBar.setEnabled(isEnabled());
        seekBar.setOnSeekBarChangeListener(this);
        textView.setText(getSummary());
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int p, boolean fromUser) {
        progress = p;
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        progress = seekBar.getProgress();
    }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        progress = seekBar.getProgress();
        if (getOnPreferenceChangeListener() != null) {
            getOnPreferenceChangeListener().onPreferenceChange(this, progress);
        }
        persistInt(progress);
    }

    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (seekBar == null) {
            seekBar = new SeekBar(getContext());
        }
        seekBar.setProgress(restoreValue ?getPersistedInt(60) : (Integer) defaultValue);
        Log.i("tag", "---------onSetInitialValue");
    }


    //自定义Preference使用Parcelable来存储数据的。这样下次启动该界面时才能恢复出原来设置好的值
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.savedProgress = progress;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        seekBar.setProgress(myState.savedProgress);
    }

    private static class SavedState extends BaseSavedState {
        int savedProgress;

        public SavedState(Parcel source) {
            super(source);
            savedProgress = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(SeekbarPreference.progress);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    public int getInt(String key, int defValue) {
        if (progress != 0) {
            return progress;
        }else {
            return defValue;
        }
    }
}
