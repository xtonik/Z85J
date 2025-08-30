package aj.z85j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static java.lang.Byte.toUnsignedInt;

class Z85Benchmark {

    @ValueSource(ints = {4, 32, 1024, 1024 * 1024})
    @ParameterizedTest
    void encode(int size) {
        byte[] binary = new byte[size];
        int repeats = 5;
        for (int i = 0; i < repeats; i++) {
            long start = System.currentTimeMillis();
            for (int j = 0; j < Integer.MAX_VALUE / size / 10; j++) {
                Z85.encode(binary);
            }
            long end = System.currentTimeMillis() - start;
            System.out.println("encode optimized: " + end + " ms");
        }

        for (int i = 0; i < repeats; i++) {
            long start = System.currentTimeMillis();
            for (int j = 0; j < Integer.MAX_VALUE / size / 10; j++) {
                encodeNaive(binary);
            }
            long end = System.currentTimeMillis() - start;
            System.out.println("encode naive: " + end + " ms");
        }
    }

    private static byte[] encodeNaive(byte[] binary) {
        byte[] encoded = new byte[Z85.checkEncodedLength(binary.length, binary.length)];
        for (int i = 0, j = 0; i < binary.length; i += 4, j += 5) {
            long chunk = (Byte.toUnsignedLong(binary[i])) << 24
                    | (toUnsignedInt(binary[i + 1])) << 16
                    | (toUnsignedInt(binary[i + 2])) << 8
                    | (toUnsignedInt(binary[i + 3])) << 0;
            int div12 = (int) (chunk / 85);
            encoded[j + 4] = Z85.encoderMap[(int) (chunk - div12 * 85)];
            encoded[j + 3] = Z85.encoderMap[div12 % 85];
            encoded[j + 2] = Z85.encoderMap[div12 / (85) % 85];
            encoded[j + 1] = Z85.encoderMap[div12 / (85 * 85) % 85];
            encoded[j + 0] = Z85.encoderMap[div12 / (85 * 85 * 85) % 85];
        }
        return encoded;
    }

    @ValueSource(ints = {5, 40, 1000, 1_000_000})
    @ParameterizedTest
    void decode(int size) {
        byte[] encodedAsByteArray = new byte[size];
        Arrays.fill(encodedAsByteArray, (byte) '0');
        String encodedAsString = new String(encodedAsByteArray);
        int repeats = 5;
        for (int i = 0; i < repeats; i++) {
            long start = System.currentTimeMillis();
            for (int j = 0; j < Integer.MAX_VALUE / size / 10; j++) {
                Z85.decode(encodedAsByteArray);
            }
            long end = System.currentTimeMillis() - start;
            System.out.println("decode from byte array: " + end + " ms");
        }

        for (int i = 0; i < repeats; i++) {
            long start = System.currentTimeMillis();
            for (int j = 0; j < Integer.MAX_VALUE / size / 10; j++) {
                Z85.decode(encodedAsString);
            }
            long end = System.currentTimeMillis() - start;
            System.out.println("decode from String: " + end + " ms");
        }
    }

    @ValueSource(ints = {5, 40, 1000, 1_000_000})
    @ParameterizedTest
    void isValidVsDecode(int size) {
        byte[] encodedAsByteArray = new byte[size];
        Arrays.fill(encodedAsByteArray, (byte) '0');
        String encodedAsString = new String(encodedAsByteArray);
        int repeats = 5;
        for (int i = 0; i < repeats; i++) {
            long start = System.currentTimeMillis();
            for (int j = 0; j < Integer.MAX_VALUE / size / 10; j++) {
                Z85.isValid(encodedAsByteArray);
            }
            long end = System.currentTimeMillis() - start;
            System.out.println("isValid(): " + end + " ms");
        }

        for (int i = 0; i < repeats; i++) {
            long start = System.currentTimeMillis();
            for (int j = 0; j < Integer.MAX_VALUE / size / 10; j++) {
                Z85.decode(encodedAsString);
            }
            long end = System.currentTimeMillis() - start;
            System.out.println("decode() from string: " + end + " ms");
        }

        for (int i = 0; i < repeats; i++) {
            long start = System.currentTimeMillis();
            for (int j = 0; j < Integer.MAX_VALUE / size / 10; j++) {
                try {
                    Z85.decode(encodedAsString);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("should never happen");
                }
            }
            long end = System.currentTimeMillis() - start;
            System.out.println("decode() from string checked: " + end + " ms");
        }

        for (int i = 0; i < repeats; i++) {
            long start = System.currentTimeMillis();
            for (int j = 0; j < Integer.MAX_VALUE / size / 10; j++) {
                try {
                    Z85.decode(encodedAsByteArray);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("should never happen");
                }
            }
            long end = System.currentTimeMillis() - start;
            System.out.println("decode(): " + end + " ms");
        }

        for (int i = 0; i < repeats; i++) {
            long start = System.currentTimeMillis();
            for (int j = 0; j < Integer.MAX_VALUE / size / 10; j++) {
                try {
                    Z85.decode(encodedAsByteArray);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("should never happen");
                }
            }
            long end = System.currentTimeMillis() - start;
            System.out.println("decode() checked: " + end + " ms");
        }
    }

    @Test
    void unsignedIntDiv85() {
        int repeats = 5;
        int cycles = 1_000_000_000;
        for (int i = 0; i < repeats; i++) {
            long sum = 0;
            long start = System.currentTimeMillis();
            for (int j = 0; j < cycles; j++) {
                sum += Z85.unsignedIntDiv85(j);
            }
            long end = System.currentTimeMillis() - start;
            System.out.println("unsignedIntDiv85(sum=" + sum + "): " + end + " ms");
        }

        for (int i = 0; i < repeats; i++) {
            long sum = 0;
            long start = System.currentTimeMillis();
            for (int j = 0; j < cycles; j++) {
                sum += j / 85; // signed int division for simplicity
            }
            long end = System.currentTimeMillis() - start;
            System.out.println("x/85       (sum=" + sum + "): " + end + " ms");
        }
    }

    @Test
    void unsignedIntDiv5() {
        int repeats = 5;
        int cycles = 1_000_000_000;
        for (int i = 0; i < repeats; i++) {
            long sum = 0;
            long start = System.currentTimeMillis();
            for (int j = 0; j < cycles; j++) {
                sum += Z85.unsignedIntDiv5(j);
            }
            long end = System.currentTimeMillis() - start;
            System.out.println("unsignedIntDiv5(sum=" + sum + "): " + end + " ms");
        }

        for (int i = 0; i < repeats; i++) {
            long sum = 0;
            long start = System.currentTimeMillis();
            for (int j = 0; j < cycles; j++) {
                sum += j / 5;// signed int division for simplicity
            }
            long end = System.currentTimeMillis() - start;
            System.out.println("x/5       (sum=" + sum + "): " + end + " ms");
        }
    }
}