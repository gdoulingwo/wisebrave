package link_work.wisebrave.BleMsg;

/**
 * The type Ble cmd 06 get data.
 */
/*
* 获取手环监测的实时数据
* */
public class BleCmd06_getData extends BaseBleMessage {


    /**
     * 手环发送：68  A0  0000  08  16	；
     * 手机回复的数据域的时间应该是4个字节，
     * 这4个字节是当前时间转换成秒的一个数字的表示（低字节在前，高字节在后），
     * 例如当前时间是2014-09-19 16:28:05.834，
     * 转换成毫秒是1411115285834，转换成秒则是1411115285，
     * 转换成字节则是54  1B  E9  15，
     * 则手机回复给手环的信息应该是：68  20  0400  15E91B54  F9  16
     */
    public static byte mTheCmd = 0x06;

    /**
     * Deal ble response byte [ ].
     *
     * @param notifyData the notify data
     * @param dataLen    the data len
     * @return the byte [ ]
     */
    public byte[] dealBleResponse(byte[] notifyData, int dataLen) {
//		syncCurrentTime();
        return null;
    }


    //6820  0800 00 00 00 00 55 72 6e bf 8416
    // 6820 0400 55 72 6f ac 6e16

    /**
     * 打开心率测试
     *
     * @return 返回心率测试响应数据包
     */
    public byte[] onHR() {
        byte[] data = new byte[1];
        data[0] = 0x01;

        return setMessageByteData(mTheCmd, data, data.length);
    }

    /**
     * 关闭心率测试
     *
     * @return 返回心率测试响应数据包
     */
    public byte[] offHR() {
        byte[] data = new byte[1];
        data[0] = 0x02;

        return setMessageByteData(mTheCmd, data, data.length);
    }

}
