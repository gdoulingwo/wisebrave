package link_work.wisebrave;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;

public class BreakPhoneUtils {

    private final static String TELEPHONY_SERVICE = "phone";

    public static String getContactNameFromPhoneBook(Context context, String phoneNum) {

        String name = null;
        int index = phoneNum.indexOf("+86");
        if (index < 0) {
            //没+86
            name = l_getContactNameFromPhoneBook(context, phoneNum);
            if (name == null) {
                name = l_getContactNameFromPhoneBook(context, "+86" + phoneNum);
            }
        } else if (index == 0) {
            //有+86
            name = l_getContactNameFromPhoneBook(context, phoneNum);
            if (name == null) {
                phoneNum = phoneNum.replace("+86", "");
                name = l_getContactNameFromPhoneBook(context, phoneNum);
            }
        }

        return name;
    }

    public static String l_getContactNameFromPhoneBook(Context context, String phoneNum) {
        String contactName = null;
        ContentResolver cr = context.getContentResolver();
        Cursor pCur = cr.query(Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, phoneNum),
                new String[]{PhoneLookup._ID,
                        PhoneLookup.NUMBER,
                        PhoneLookup.DISPLAY_NAME,
                        PhoneLookup.TYPE,
                        PhoneLookup.LABEL}, null, null, null);

        assert pCur != null;
        if (pCur.moveToFirst()) {
            Log.d("TAG", "提取成功");
            contactName = pCur
                    .getString(pCur
                            .getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            pCur.close();
        }

        Log.d("TAG", "----------------号码：--------------" + phoneNum + "名字：" + contactName);

        return contactName;
    }
}