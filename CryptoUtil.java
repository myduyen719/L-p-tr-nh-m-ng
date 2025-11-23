package smtpapp;

public class CryptoUtil {
    private static final String KEY = "BTL_LTM"; // Khóa mã hóa đơn giản

    // Mã hóa (XOR)
    public static String encrypt(String text) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char originalChar = text.charAt(i);
            char keyChar = KEY.charAt(i % KEY.length());
            char encryptedChar = (char) (originalChar ^ keyChar);
            result.append(encryptedChar);
        }
        return result.toString();
    }

    // Giải mã (XOR, cùng thuật toán)
    public static String decrypt(String encryptedText) {
        // Vì XOR là hàm đối xứng, mã hóa và giải mã dùng chung logic
        return encrypt(encryptedText); 
    }
}
