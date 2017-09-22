package link_work.wisebrave;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class Config {
    private static final String KEY_NAME = "name";
    private static final String KEY_ADDR = "address";
    private static final String KEY_VALID = "valid";
    private static final String DATABASE = "Config";
    private String name;
    private String address;
    private boolean valid = false;
    private SharedPreferences sp;
    //private SharedPreferences sp = null;

    public Config(Context context) {
        super();
        sp = context.getSharedPreferences(DATABASE, Context.MODE_PRIVATE);
        valid = sp.getBoolean(KEY_VALID, false);
        name = sp.getString(KEY_NAME, "");
        address = sp.getString(KEY_ADDR, "");
        //address = new byte[6];
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public void clear_config() {
        this.setValid(false);
        Editor editor = sp.edit();
        editor.clear();
        editor.commit();
    }

    public void save_config(String ble_name, String ble_addr) {
        this.setValid(true);
        this.name = ble_name;
        this.address = ble_addr;

        Editor editor = sp.edit();
        editor.putString(KEY_NAME, ble_name);
        editor.putString(KEY_ADDR, ble_addr);
        editor.putBoolean(KEY_VALID, true);
        editor.commit();
    }

    public String getName() {
        Log.d("Consig", "dev_name:" + name);
        return name;
    }

    public String getAddress() {
        return address;
    }
}

