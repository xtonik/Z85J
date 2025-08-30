package aj.z85j;

import java.nio.ByteOrder;

public class UnsafeByteArrayAccess {
    private static final sun.misc.Unsafe UNSAFE;
    private static final long BYTE_ARRAY_OFFSET;

    static {
        try {
            java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (sun.misc.Unsafe) f.get(null);
            BYTE_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int bytesToInt(byte[] bytes, int pos) {
        int value = UNSAFE.getInt(bytes, BYTE_ARRAY_OFFSET + pos);
        return ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? Integer.reverseBytes(value) : value;
    }

    public static void intToBytes(byte[] bytes, int pos, int i) {
        int value = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? Integer.reverseBytes(i) : i;
        UNSAFE.putInt(bytes, BYTE_ARRAY_OFFSET + pos, value);
    }
}