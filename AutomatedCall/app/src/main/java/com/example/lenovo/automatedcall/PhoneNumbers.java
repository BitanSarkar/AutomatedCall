package com.example.lenovo.automatedcall;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by LENOVO on 26-May-19.
 */

public class PhoneNumbers{
    private String ph;
    public PhoneNumbers(String phoneNumber){
        phoneNumber = validate(phoneNumber);
        this.ph = phoneNumber;
    }

    protected PhoneNumbers(Parcel in) {
        ph = in.readString();
    }

    private String validate(String ph){
        String res = "";
        ph=ph.trim();
        for(int i = 0; i < ph.length(); i++){
            char ch = ph.charAt(i);
            if((int)ch>=48 && (int)ch<58)
                res = res + ch;
        }
        if(res.length() <= 10)
            return res;
        res = res.substring(res.length() - 10);
        return res;
    }
    public String getph(){
        return this.ph;
    }
    public void setph(String phoneNumber){
        phoneNumber = validate(phoneNumber);
        this.ph = phoneNumber;
    }
}
