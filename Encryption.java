package il.ac.kinneret.mjmay.hls.hlsjava.model;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;
/**
 * Class to perform encryption and decryption operations of messages and files
 * @authors Sasha Chernin & Hai Palatshi
 */
public class Encryption {

    public static SecretKey secretKey;



    static {
        try {
            secretKey = retrieveKey();
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves password from config file and transforms it into a sha256 digest to use as a key
     * @return SecretKeySpec object that is initialized with the sha256 version of the password
     */
    public static SecretKey retrieveKey() throws NoSuchAlgorithmException, IOException {
        FileReader file = new FileReader("Config");
        BufferedReader buffer = new BufferedReader(file);
        //read the 1st line
        String keyText = buffer.readLine();

        // String to bytes array
        byte[] arr = keyText.getBytes(StandardCharsets.UTF_8);
        // bytes array to sha-256 hash
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return new SecretKeySpec(digest.digest(arr), 0, digest.digest(arr).length, "AES");
    }

    /**
     * Encrypts string in AES-CBC mode
     * @param value The string that is being encrypted
     * @return The encrypted string
     */
    public static String encryptMessage(String value) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");

            // generate iv
            SecureRandom randomSecureRandom = new SecureRandom();
            byte[] iv = new byte[cipher.getBlockSize()];
            randomSecureRandom.nextBytes(iv);
            IvParameterSpec ivParams = new IvParameterSpec(iv);

            LoggerFile.getInstance().info("The random iv before encryption: "+ivParams.getIV());


            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParams);

            // preform encryption in the message
            byte[] encrypted = cipher.doFinal(value.getBytes());

            ByteArrayOutputStream output = new ByteArrayOutputStream();

            // write iv to the first 16 bytes of output array
            output.write(ivParams.getIV());
            // write the encrypted message to the output array
            output.write(encrypted);

            // convert output array to bytes array and return it
            byte[] out = output.toByteArray();

            return Base64.getEncoder().encodeToString(out);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Decrypts string in AES-CBC mode
     * @param encrypted The string that is being decrypted
     * @return The decrypted string
     */
    public static String decryptMessage(String encrypted) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IOException, IllegalBlockSizeException, BadPaddingException {

                byte[] StrToDecrypt = Base64.getDecoder().decode(encrypted);

                byte[] strToEnc = Arrays.copyOfRange(StrToDecrypt, 16, StrToDecrypt.length);

                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
                cipher.init(Cipher.DECRYPT_MODE, secretKey,
                        new IvParameterSpec(StrToDecrypt, 0, 16));

                LoggerFile.getInstance().info("The iv before decryption: " + cipher.getIV());

                byte[] original = cipher.doFinal(strToEnc);

                return new String(original);


    }

    /**
     * Encrypts file in AES-CTR mode
     * @param originalFile The full path of the file including the file name
     * @param fileName The full path of the target file including the file name
     */
    public static void encryptFile(String originalFile ,String fileName) throws NoSuchPaddingException, NoSuchAlgorithmException, IOException, InvalidKeyException, InvalidAlgorithmParameterException {

        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        // generate iv
        SecureRandom randomSecureRandom = new SecureRandom();
        byte[] iv = new byte[cipher.getBlockSize()];
        randomSecureRandom.nextBytes(iv);
        IvParameterSpec ivParams = new IvParameterSpec(iv);
        LoggerFile.getInstance().info("The random iv before encryption: "+ivParams.getIV());


        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParams);
        byte[] array = Files.readAllBytes(Paths.get(originalFile));

        try (FileOutputStream fileOut = new FileOutputStream(fileName);
             CipherOutputStream cipherOut = new CipherOutputStream(fileOut, cipher)) {
            fileOut.write(ivParams.getIV());
            cipherOut.write(array);
            fileOut.close();
            cipherOut.close();
        }
    }

    /**
     * Decrypts file in AES-CTR mode
     * @param fileName The full path of the file including the file name
     * @param decName The full path of the target file including the file name
     */
    public static void decryptFile(String fileName, String decName) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException {
        byte[] array = Files.readAllBytes(Paths.get(fileName));
        byte[] withoutIV = Arrays.copyOfRange(array, 16, array.length);
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        try (FileInputStream fileIn = new FileInputStream(fileName)) {
            byte[] fileIv = new byte[16];
            fileIn.read(fileIv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(fileIv));

            LoggerFile.getInstance().info("The iv before decryption: "+cipher.getIV());

            try (FileOutputStream fileOut = new FileOutputStream(decName);
                 CipherOutputStream cipherOut = new CipherOutputStream(fileOut, cipher)) {
                cipherOut.write(withoutIV);
                fileOut.close();
                cipherOut.close();
            }

        } catch (IOException | InvalidAlgorithmParameterException | InvalidKeyException e) {
            e.printStackTrace();
            LoggerFile.getInstance().info("Can't decrypt file: "+ decName);

        }
    }
}
