package aj.z85j;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class Z85Test {

    public static Stream<Integer> invalidEncodedSize() {
        return Stream.of(
                1, 2, 3, 4,
                6, 7, 8, 9,
                11, 12, 13, 14);
    }

    public static Stream<Integer> validEncodedSize() {
        return Stream.of(0, 5, 10, 15, 20);
    }

    @Test
    @Tag("long")
    @Disabled
    void unsignedIntDiv85() {
        for (long i = 0; i < 1L << 32; i++) {
            assertEquals((int) (i / 85), Z85.unsignedIntDiv85((int) i));
        }
    }

    @Test
    @Tag("long")
    @Disabled
    void unsignedIntDiv5() {
        for (long i = 0; i < 1L << 32; i++) {
            assertEquals((int) (i / 5), Z85.unsignedIntDiv5((int) i));
        }
    }

    @Test
    @Disabled
    void javaMaxArraySize() {
        byte[] a = new byte[Z85.JAVA_MAX_ARRAY_SIZE];
        a = new byte[0];
        assertThrows(OutOfMemoryError.class, () -> {
            byte[] b = new byte[Z85.JAVA_MAX_ARRAY_SIZE + 1];
        });
    }

    @Test
    void maxInputLength() {
        assertAll(
                () -> assertEquals(Integer.MAX_VALUE - 7, Z85.encodedLength(Z85.MAX_INPUT_LENGTH - 4)),
                () -> assertEquals(Integer.MAX_VALUE - 2, Z85.encodedLength(Z85.MAX_INPUT_LENGTH)),
                () -> assertTrue(Z85.encodedLength(Z85.MAX_INPUT_LENGTH + 1) < 0) // overflow
        );
    }

    @ValueSource(ints = {0, 4, 8, 12, 16})
    @ParameterizedTest
    void encodedLengthValid(int value) {
        assertEquals(value * 5 / 4, Z85.encodedLength(value));
    }

    @ValueSource(ints = {
            1, 2, 3,
            5, 6, 7,
            9, 10, 11})
    @ParameterizedTest
    void checkEncodedLengthInvalid(int value) {
        assertThrows(IllegalArgumentException.class, () -> Z85.checkEncodedLength(value, value));
    }

    @ValueSource(ints = {Integer.MAX_VALUE, Z85.MAX_INPUT_LENGTH + 1, Z85.MAX_INPUT_LENGTH + 4})
    @ParameterizedTest
    void checkEncodedLengthNotFitsToJavaArray(int value) {
        assertThrows(IllegalArgumentException.class, () -> Z85.checkEncodedLength(value, value));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, Integer.MIN_VALUE})
    void checkEncodedLengthNegative(int value) {
        assertThrows(IllegalArgumentException.class, () -> Z85.checkEncodedLength(value, value));
    }

    @Test
    void checkEncodedLengthSizeOutOfArray() {
        assertThrows(IllegalArgumentException.class, () -> Z85.checkEncodedLength(20, 21));
    }

    private static Stream<String> outOfIntegerRange() {
        return Stream.of(
                "#####", // [84, 84, 84, 84, 84]
                "%nSc1", // [1, 0, 0, 0, 0] ~ 2^32
                "%nSc2" // [1, 0, 0, 0, 1] ~ 2^32 + 1
        );
    }

    private static Stream<Arguments> validPairs() {
        return Stream.of(
                Arguments.of("", new byte[0]),
                Arguments.of("%nSc0", new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}),
                Arguments.of("00000", new byte[4]),
                Arguments.of("01234", intToBytes(4 + 85 * (3 + 85 * (2 + 85 * 1)))),
                Arguments.of("04321", intToBytes(1 + 85 * (2 + 85 * (3 + 85 * 4)))),
                Arguments.of("Hello", new byte[]{(byte) 0x86, (byte) 0x4F, (byte) 0xD2, (byte) 0x6F}),
                Arguments.of("HelloWorld", new byte[]{(byte) 0x86, (byte) 0x4F, (byte) 0xD2, (byte) 0x6F, (byte) 0xB5, (byte) 0x59, (byte) 0xF7, (byte) 0x5B})
        );
    }

    private static Stream<Character> invalidCharacters() {
        return Stream.of(',', ' ', '\\', '`', '\t', 'č', 'ž', 'á', '\u0000');
    }

    @ParameterizedTest
    @MethodSource("validPairs")
    void encodeValidChars(String expected, byte[] binary) {
        byte[] encoded = Z85.encode(binary);
        assertEquals(expected, new String(encoded));
    }

    @ParameterizedTest
    @MethodSource("validPairs")
    void decodeValidChars(String encoded, byte[] expected) {
        byte[] decoded = Z85.decode(encoded.getBytes());
        assertEquals(new String(expected), new String(decoded));
    }

    @ParameterizedTest
    @MethodSource("validPairs")
    void decodeValidCharsFromString(String encoded, byte[] expected) {
        byte[] decoded = Z85.decode(encoded);
        assertEquals(new String(expected), new String(decoded));
    }

    @ParameterizedTest
    @MethodSource("validPairs")
    void encodeDecodeValidChars(String encodedOriginal, byte[] binary) {
        byte[] encoded = Z85.encode(binary);
        byte[] encodedDecoded = Z85.decode(encoded);
        assertEquals(new String(binary), new String(encodedDecoded));
    }

    @ParameterizedTest
    @MethodSource("validPairs")
    void encodeDecodeValidCharsFromString(String encodedOriginal, byte[] binary) {
        byte[] encoded = Z85.encode(binary);
        byte[] encodedDecoded = Z85.decode(new String(encoded));
        assertEquals(new String(binary), new String(encodedDecoded));
    }

    @ParameterizedTest
    @MethodSource("validPairs")
    void decodeEncodeValidChars(String encoded, byte[] binary) {
        byte[] decoded = Z85.decode(encoded.getBytes());
        byte[] decodedEncoded = Z85.encode(decoded);
        assertEquals(encoded, new String(decodedEncoded));
    }

    @ParameterizedTest
    @MethodSource("validPairs")
    void decodeEncodeValidCharsFromString(String encoded, byte[] binary) {
        byte[] decoded = Z85.decode(encoded);
        byte[] decodedEncoded = Z85.encode(decoded);
        assertEquals(encoded, new String(decodedEncoded));
    }

    @ParameterizedTest
    @MethodSource("validPairs")
    void isValidValidChars(String encoded, byte[] binary) {
        assertTrue(Z85.isValid(encoded.getBytes()));
    }

    @ParameterizedTest
    @MethodSource("validPairs")
    void isValidValidCharsFromString(String encoded, byte[] binary) {
        assertTrue(Z85.isValid(encoded));
    }

    @MethodSource("invalidCharacters")
    @ParameterizedTest
    void decodeInvalidCharacters(char c) {
        for (int i = 0; i < 5; i++) {
            byte[] bytes = oneInvalidEncodingCharacter(c, i);
            assertThrows(IllegalArgumentException.class, () -> Z85.decode(bytes), "position " + i + " for char " + c);
        }
    }

    @MethodSource("invalidCharacters")
    @ParameterizedTest
    void decodeInvalidCharactersFromString(char c) {
        for (int i = 0; i < 5; i++) {
            byte[] bytes = oneInvalidEncodingCharacter(c, i);
            assertThrows(IllegalArgumentException.class, () -> Z85.decode(new String(bytes)), "position " + i + " for char " + c);
        }
    }

    @MethodSource("outOfIntegerRange")
    @ParameterizedTest
    void decodeInvalidOutOfIntegerRange(String value) {
        assertThrows(IllegalArgumentException.class, () -> Z85.decode(value.getBytes()));
    }

    @MethodSource("outOfIntegerRange")
    @ParameterizedTest
    void decodeInvalidOutOfIntegerRangeFromString(String value) {
        assertThrows(IllegalArgumentException.class, () -> Z85.decode(value));
    }

    @MethodSource("invalidEncodedSize")
    @ParameterizedTest
    void checkDecodedLengthNotMultiplyBy5(int value) {
        assertThrows(IllegalArgumentException.class, () -> Z85.checkDecodedLength(value, value));
    }

    @ValueSource(ints = {-1, -2, Integer.MIN_VALUE + 1, Integer.MIN_VALUE})
    @ParameterizedTest
    void checkDecodedLengthNegative(int value) {
        assertThrows(IllegalArgumentException.class, () -> Z85.checkDecodedLength(value, value));
    }

    @Test
    void checkDecodedLengthSizeOutOfArray() {
        assertThrows(IllegalArgumentException.class, () -> Z85.checkDecodedLength(20, 21));
    }

    @Test
    @Tag("long")
    @Disabled
    void decodeEncodeAllValid() {
        for (long i = 0; i < 1L << 32; i++) {
            byte[] sourceEncoded = encodeNaive(i);
            byte[] targetDecoded = Z85.decode(sourceEncoded);
            byte[] targetEncoded = Z85.encode(targetDecoded);
            assertArrayEquals(sourceEncoded, targetEncoded, Long.toHexString(i));
        }
    }

    @Test
    @Tag("long")
    @Disabled
    void decodeEncodeAllValidFromString() {
        for (long i = 0; i < 1L << 32; i++) {
            byte[] sourceEncoded = encodeNaive(i);
            byte[] targetDecoded = Z85.decode(new String(sourceEncoded));
            byte[] targetEncoded = Z85.encode(targetDecoded);
            assertArrayEquals(sourceEncoded, targetEncoded, Long.toHexString(i));
        }
    }


    @Test
    @Tag("long")
    @Disabled
    void decodeEncodeAllTooHigh() {
        for (long i = 1L << 32; i < 85 * 85 * 85 * 85 * 85L; i++) {
            byte[] sourceEncoded = encodeNaive(i);
            assertThrows(IllegalArgumentException.class, () -> Z85.decode(sourceEncoded));
        }
    }

    @Test
    @Tag("long")
    @Disabled
    void decodeEncodeAllTooHighFromString() {
        for (long i = 1L << 32; i < 85 * 85 * 85 * 85 * 85L; i++) {
            byte[] sourceEncoded = encodeNaive(i);
            assertThrows(IllegalArgumentException.class, () -> Z85.decode(new String(sourceEncoded)));
        }
    }

    @Test
    @Tag("long")
    @Disabled
    void encodeDecodeAll() {
        for (long i = 0; i < 1L << 32; i++) {
            byte[] sourceBinary = intToBytes((int) i);
            byte[] targetEncoded = Z85.encode(sourceBinary);
            byte[] targetBinary = Z85.decode(targetEncoded);
            assertArrayEquals(sourceBinary, targetBinary, Long.toHexString(i));
        }
    }

    @Test
    @Tag("long")
    @Disabled
    void encodeDecodeAllFromString() {
        for (long i = 0; i < 1L << 32; i++) {
            byte[] sourceBinary = intToBytes((int) i);
            byte[] targetEncoded = Z85.encode(sourceBinary);
            byte[] targetBinary = Z85.decode(new String(targetEncoded));
            assertArrayEquals(sourceBinary, targetBinary, Long.toHexString(i));
        }
    }

    @MethodSource("invalidCharacters")
    @ParameterizedTest
    void isValidInvalidCharacters(char c) {
        for (int i = 0; i < 5; i++) {
            byte[] bytes = oneInvalidEncodingCharacter(c, i);
            assertFalse(Z85.isValid(bytes));
        }
    }

    @MethodSource("invalidCharacters")
    @ParameterizedTest
    void isValidInvalidCharactersFromString(char c) {
        for (int i = 0; i < 5; i++) {
            byte[] bytes = oneInvalidEncodingCharacter(c, i);
            assertFalse(Z85.isValid(new String(bytes)));
        }
    }

    @MethodSource("outOfIntegerRange")
    @ParameterizedTest
    void isValidOutOfIntegerRange(String encoded) {
        assertFalse(Z85.isValid(encoded.getBytes()));
    }

    @MethodSource("outOfIntegerRange")
    @ParameterizedTest
    void isValidOutOfIntegerRangeFromString(String encoded) {
        assertFalse(Z85.isValid(encoded));
    }

    @MethodSource("invalidEncodedSize")
    @ParameterizedTest
    void isValidInvalidSize(int value) {
        String encoded = repeatCharacter('0', value);
        assertFalse(Z85.isValid(encoded.getBytes()));
    }

    @MethodSource("invalidEncodedSize")
    @ParameterizedTest
    void isValidInvalidSizeFromString(int value) {
        String encoded = repeatCharacter('0', value);
        assertFalse(Z85.isValid(encoded));
    }

    @MethodSource("validEncodedSize")
    @ParameterizedTest
    void isValidCorrectSize(int value) {
        String encoded = repeatCharacter('0', value);
        assertTrue(Z85.isValid(encoded.getBytes()));
    }

    @MethodSource("validEncodedSize")
    @ParameterizedTest
    void isValidCorrectSizeFromString(int value) {
        String encoded = repeatCharacter('0', value);
        assertTrue(Z85.isValid(encoded));
    }

    @Test
    @Tag("long")
    @Disabled
    void isValidAllValid() {
        for (long i = 0; i < 1L << 32; i++) {
            byte[] sourceEncoded = encodeNaive(i);
            assertTrue(Z85.isValid(sourceEncoded));
        }
    }

    @Test
    @Tag("long")
    @Disabled
    void isValidAllValidFromString() {
        for (long i = 0; i < 1L << 32; i++) {
            byte[] sourceEncoded = encodeNaive(i);
            assertTrue(Z85.isValid(new String(sourceEncoded)));
        }
    }


    @Test
    @Tag("long")
    @Disabled
    void isValidAllTooHigh() {
        for (long i = 1L << 32; i < 85 * 85 * 85 * 85 * 85L; i++) {
            byte[] sourceEncoded = encodeNaive(i);
            assertFalse(Z85.isValid(sourceEncoded));
        }
    }

    @Test
    @Tag("long")
    @Disabled
    void isValidAllTooHighFromString() {
        for (long i = 1L << 32; i < 85 * 85 * 85 * 85 * 85L; i++) {
            byte[] sourceEncoded = encodeNaive(i);
            assertFalse(Z85.isValid(new String(sourceEncoded)));
        }
    }

    private static byte[] encodeNaive(long i) {
        byte[] encoded = new byte[5];
        encoded[4] = Z85.encoderMap[(int) (i % 85)];
        encoded[3] = Z85.encoderMap[(int) (i / 85 % 85)];
        encoded[2] = Z85.encoderMap[(int) (i / (85 * 85) % 85)];
        encoded[1] = Z85.encoderMap[(int) (i / (85 * 85 * 85) % 85)];
        encoded[0] = Z85.encoderMap[(int) (i / (85 * 85 * 85 * 85) % 85)];
        return encoded;
    }

    private static byte[] intToBytes(int i) {
        byte[] b = new byte[4];
        b[3] = (byte) (i >>> 0);
        b[2] = (byte) (i >>> 8);
        b[1] = (byte) (i >>> 16);
        b[0] = (byte) (i >>> 24);
        return b;
    }

    private static String repeatCharacter(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    private static byte[] oneInvalidEncodingCharacter(char c, int pos) {
        byte[] bytes = "AAAAA".getBytes();
        bytes[pos] = (byte) c;
        return bytes;
    }
}