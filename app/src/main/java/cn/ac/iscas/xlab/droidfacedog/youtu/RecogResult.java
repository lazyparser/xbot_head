package cn.ac.iscas.xlab.droidfacedog.youtu;

import android.util.JsonReader;
import android.util.Log;

import java.io.IOException;

/**
 * Created by w on 3/25/17.
 */

public final class RecogResult {
    double mConfidence;
    String mId;
    int mRet;

    public double getConfidence() {
        return mConfidence;
    }

    public void setConfidence(double mConfidence) {
        this.mConfidence = mConfidence;
    }

    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }

    public int getRet() {
        return mRet;
    }

    public void setRet(int mRet) {
        this.mRet = mRet;
    }

    public boolean parseFrom(JsonReader reader) {
        //Ref: youtu hezi manual v1.2, page 3, recognition
        // {
        //     Confidence: Float, #threshold is configurable by users.
        //     Id: String, #hexified utf-8 name
        //     Ret: Int # Ret=0 -> everything is ok.
        // }
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (key.equalsIgnoreCase("Confidence"))
                    mConfidence = reader.nextDouble();
                else if (key.equalsIgnoreCase("Id"))
                    mId = reader.nextString();
                else if (key.equalsIgnoreCase("Ret"))
                    mRet = reader.nextInt();
                else
                    throw new IOException("INVALID OR BROKEN RECOG RESULT JSON FILE.");
            }
            reader.endObject();
        } catch (IOException e) {
            Log.e("YOUTU", "INVALID OR BROKEN RECOG RESULT JSON FILE.");
            e.printStackTrace();
            return false;
        }
        return mRet == 0;
    }

}
