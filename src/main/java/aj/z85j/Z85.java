package aj.z85j;

import java.util.Arrays;

import static java.lang.Byte.toUnsignedInt;
import static java.lang.Integer.toUnsignedLong;

//  Z85 encoder respecting ZeroMQ's RFC32
public class Z85 {

    static final byte[] encoderMap = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ.-:+=^!/*?&<>()[]{}@%$#".getBytes();

    private static final byte[] decoderMap;

    static {
        decoderMap = new byte[256];// intentionally 256, not 128 to avoid check if byte is below 128
        Arrays.fill(decoderMap, (byte) 0xFF);
        for (int i = 0; i < 85; i++) {
            decoderMap[toUnsignedInt(encoderMap[i])] = (byte) i;
        }
    }

    /**
     * Encodes binary data to Z85.
     *
     * @param binary binary data
     * @return data encoded in Z85
     * @throws IllegalArgumentException thrown when invalid characters are found, encoded data exceeds maximum java array size, size of input array is not multiple of 4
     */
    public static byte[] encode(byte[] binary) {
        return encode(binary, binary.length);
    }

    /**
     * Encodes binary data to Z85.
     *
     * @param binary binary data
     * @param size   number of bytes of input array are taken into account
     * @return data encoded in Z85
     * @throws IllegalArgumentException thrown when invalid characters are found, encoded data exceeds maximum java array size, given size is not multiple of 4 is greater than array size
     */
    public static byte[] encode(byte[] binary, int size) {
        byte[] encoded = new byte[(int) checkEncodedLength(binary.length, size)];
        for (int i = 0, j = 0; i < binary.length; i += 4, j += 5) {
            int chunk = UnsafeByteArrayAccess.bytesToInt(binary, i);

            int div1 = unsignedIntDiv85(chunk);
            int div2 = unsignedIntDiv85(div1);
            encoded[j + 4] = encoderMap[(int) (toUnsignedLong(chunk) - div1 * 85L)];
            encoded[j + 3] = encoderMap[div1 - div2 * 85];

            int div3 = unsignedIntDiv85(div2);
            int div4 = unsignedIntDiv85(div3);
            encoded[j + 2] = encoderMap[div2 - div3 * 85];
            encoded[j + 1] = encoderMap[div3 - div4 * 85];

            encoded[j + 0] = encoderMap[div4];
        }
        return encoded;
    }

    static final int JAVA_MAX_ARRAY_SIZE = Integer.MAX_VALUE - 2;
    // 1_717_986_916 / 4 * 5 = 2_147_483_645 = Integer.MAX_VALUE - 2
    static final int MAX_INPUT_LENGTH = 1_717_986_916;

    static int checkEncodedLength(int arraySize, int givenSize) {
        if (givenSize > arraySize) {
            throw new IllegalArgumentException("Size is greater than array size: " + givenSize + " > " + arraySize);
        } else if ((givenSize & 3) != 0) {
            throw new IllegalArgumentException("Size of must be multiple of 4: " + givenSize);
        } else if (givenSize < 0) {
            throw new IllegalArgumentException("Size must be positive: " + givenSize);
        } else if (givenSize > MAX_INPUT_LENGTH) {
            throw new IllegalArgumentException("Not enough space for encoding, " + givenSize + " bytes, " + MAX_INPUT_LENGTH + " bytes are allowed at most");
        } else {
            return encodedLength(givenSize);
        }
    }

    static int encodedLength(int size) {
        return ((size + 3) >> 2) * 5;
    }

    /**
     * Decodes binary data from Z85.
     *
     * @param encoded data encoded in Z85
     * @return binary data
     * @throws IllegalArgumentException thrown when invalid characters are found, decoded data exceeds integer range, size of input array is not multiple of 5
     */
    public static byte[] decode(byte[] encoded) {
        return decode(encoded, encoded.length);
    }

    /**
     * Decodes binary data from Z85.
     *
     * @param encoded data encoded in Z85
     * @param size    number of bytes of input array are taken into account
     * @return binary data
     * @throws IllegalArgumentException thrown when invalid characters are found, decoded data exceeds integer range, given size is not multiple of 5 or is greater than array size
     */
    public static byte[] decode(byte[] encoded, int size) {
        byte[] decoded = new byte[checkDecodedLength(encoded.length, size)];
        for (int i = 0, j = 0; i < encoded.length; i += 5, j += 4) {
            if (containsInvalidCharactersInFive(encoded, i)) {
                throw new IllegalArgumentException("Invalid encoding at position " + i);
            }
            long chunk = decodeFive(encoded, i);
            if ((chunk >>> 32) != 0) { // decoded value outside integer range
                throw new IllegalArgumentException("Invalid encoding at position " + i);
            }
            UnsafeByteArrayAccess.intToBytes(decoded, j, (int) chunk);
        }
        return decoded;
    }

    /**
     * Decodes string from Z85.
     *
     * @param encoded string encoded in Z85
     * @return binary data
     * @throws IllegalArgumentException thrown when invalid characters are found, decoded data exceeds integer range, size of input array is not multiple of 5
     */
    public static byte[] decode(String encoded) {
        return decode(encoded, encoded.length());
    }

    /**
     * Decodes string from Z85.
     *
     * @param encoded string encoded in Z85
     * @param size    number of bytes of input array are taken into account
     * @return binary data
     * @throws IllegalArgumentException thrown when invalid characters are found, decoded data exceeds integer range, given size is not multiple of 5 or is greater than array size
     */
    public static byte[] decode(String encoded, int size) {
        byte[] decoded = new byte[checkDecodedLength(encoded.length(), size)];
        for (int i = 0, j = 0; i < encoded.length(); i += 5, j += 4) {
            if (containsNonAsciiInFive(encoded, i)) { // to avoid array index out of bounds exception
                throw new IllegalArgumentException("Invalid encoding at position " + i);
            }
            if (containsInvalidCharactersInFive(encoded, i)) {
                throw new IllegalArgumentException("Invalid encoding at position " + i);
            }
            long chunk = decodeFive(encoded, i);
            if ((chunk >>> 32) != 0) { // decoded value outside integer range
                throw new IllegalArgumentException("Invalid encoding at position " + i);
            }
            UnsafeByteArrayAccess.intToBytes(decoded, j, (int) chunk);
        }
        return decoded;
    }

    static int checkDecodedLength(int arraySize, int givenSize) {
        if (givenSize > arraySize) {
            throw new IllegalArgumentException("Size is greater than length of array: " + givenSize + " > " + arraySize);
        } else if (givenSize < 0) {
            throw new IllegalArgumentException("Size must be positive: " + givenSize);
        } else {
            int sizeDiv5 = unsignedIntDiv5(givenSize);
            if (givenSize != sizeDiv5 * 5) {
                throw new IllegalArgumentException("Length of array must be multiple of 5: " + givenSize);
            }
            return sizeDiv5 * 4;
        }
    }

    /**
     * Validates if byte array conforms Z85 encoding according to ZeroMQ RFC32: length of array is divisible by 5, all chunks mapped into unsigned integer range, no invalid characters, no padding is allowed.
     *
     * @param encoded encoded byte array
     * @return true, when valid
     */
    public static boolean isValid(byte[] encoded) {
        if ((unsignedIntDiv5(encoded.length) * 5 != encoded.length)) {
            return false;
        }
        for (int i = 0, j = 0; i < encoded.length; i += 5, j += 4) {
            if (containsInvalidCharactersInFive(encoded, i)) {
                return false;
            }
            if ((decodeFive(encoded, i) >>> 32) != 0) { // decoded value outside integer range
                return false;
            }
        }
        return true;
    }

    /**
     * Validates if string conforms Z85 encoding according to ZeroMQ RFC32: length of string is divisible by 5, all chunks mapped into unsigned integer range, no invalid characters, no padding is allowed.
     *
     * @param encoded encoded string
     * @return true, when valid
     */
    public static boolean isValid(String encoded) {
        if (unsignedIntDiv5(encoded.length()) * 5 != encoded.length()) {
            return false;
        }
        for (int i = 0, j = 0; i < encoded.length(); i += 5, j += 4) {
            if (containsNonAsciiInFive(encoded, i)) { // to avoid array index out of bounds exception
                return false;
            }
            if (containsInvalidCharactersInFive(encoded, i)) {
                return false;
            }
            long chunk = decodeFive(encoded, i);
            if ((chunk >>> 32) != 0) { // decoded value outside integer range
                return false;
            }
        }
        return true;
    }

    private static final int MOD5 = (int) ((1L << 32) / 5);

    static int unsignedIntDiv5(int i) {
        return (int) (((toUnsignedLong(i) + 1) * MOD5) >>> 32);
    }

    private static final int MOD85 = (int) ((1L << 32) / 85);

    static int unsignedIntDiv85(int i) {
        return (int) (((toUnsignedLong(i) + 1) * MOD85) >>> 32);
    }

    private static boolean containsInvalidCharactersInFive(String encoded, int pos) {
        return ((decoderMap[encoded.charAt(pos + 4)]
                | decoderMap[encoded.charAt(pos + 3)]
                | decoderMap[encoded.charAt(pos + 2)]
                | decoderMap[encoded.charAt(pos + 1)]
                | decoderMap[encoded.charAt(pos + 0)]) & 0xFF_FF_FF_00) != 0;
    }

    private static boolean containsNonAsciiInFive(String encoded, int pos) {
        return ((encoded.charAt(pos)
                | encoded.charAt(pos + 1)
                | encoded.charAt(pos + 2)
                | encoded.charAt(pos + 3)
                | encoded.charAt(pos + 4)) & 0xFF_FF_FF_00) != 0;
    }

    private static boolean containsInvalidCharactersInFive(byte[] encoded, int pos) {
        return ((decoderMap[toUnsignedInt(encoded[pos + 4])]
                | decoderMap[toUnsignedInt(encoded[pos + 3])]
                | decoderMap[toUnsignedInt(encoded[pos + 2])]
                | decoderMap[toUnsignedInt(encoded[pos + 1])]
                | decoderMap[toUnsignedInt(encoded[pos + 0])]) & 0xFF_FF_FF_00) != 0;
    }

    private static long decodeFive(String encoded, int pos) {
        return decoderMap[encoded.charAt(pos + 4)]
                + 85 * (decoderMap[encoded.charAt(pos + 3)]
                + 85 * (decoderMap[encoded.charAt(pos + 2)]
                + 85 * (decoderMap[encoded.charAt(pos + 1)]
                + 85L * (decoderMap[encoded.charAt(pos + 0)]))));
    }

    private static long decodeFive(byte[] encoded, int pos) {
        return decoderMap[toUnsignedInt(encoded[pos + 4])]
                + 85 * (decoderMap[toUnsignedInt(encoded[pos + 3])]
                + 85 * (decoderMap[toUnsignedInt(encoded[pos + 2])]
                + 85 * (decoderMap[toUnsignedInt(encoded[pos + 1])]
                + 85L * (decoderMap[toUnsignedInt(encoded[pos + 0])]))));
    }
}
