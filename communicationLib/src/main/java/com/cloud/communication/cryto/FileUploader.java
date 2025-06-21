package com.cloud.communication.cryto;

import static com.cloud.communication.cryto.ConversionUtils.bufferToString;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;


public class FileUploader {

    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB

    // Internal state
    private static final Map<String, byte[]> upload = new ConcurrentHashMap<>();
    private static final Map<String, Integer> chunkParts = new ConcurrentHashMap<>();
    private static final Map<String, Integer> chunkLength = new ConcurrentHashMap<>();
    private static final Map<String, BiConsumer<String, Integer>> chunkRequest = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static void startSendFileAsync(File file) {
        new Thread(() -> startSendFile(file)).start();
    }

    public static void startSendFile(File file) {
        try {
            byte[] fileData = Files.readAllBytes(file.toPath());

            String fullPath = file.getName(); // Or construct full path if needed

            upload.put(fullPath, fileData);

            // Start sending from chunk 1
            chunkRequestCallback(fullPath, 1);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void chunkRequestCallback(String fullFileName, int chunkNumber) {
        // This simulates the JavaScript chunkRequestCallback
        setFile(fullFileName, chunkNumber, upload.get(fullFileName));
    }

    private static void setFile(String fullFileName, int chunkNumber, byte[] data) {
        int fileLength = data.length;
        int position = (chunkNumber - 1) * CHUNK_SIZE;
        int toTake = Math.min(CHUNK_SIZE, fileLength - position);

        byte[] chunkData = new byte[toTake];
        System.arraycopy(data, position, chunkData, 0, toTake);

        uploadFile(fullFileName, chunkData, chunkNumber, fileLength, FileUploader::chunkRequestCallback);

        // Waiting for server response to continue (will call chunkRequestCallback)
    }

    private static void uploadFile(String fullFileName, byte[] chunkData, int chunkNumber, int fileLength,
                                   BiConsumer<String, Integer> chunkRequestCallback) {
        if (chunkNumber <= 0) {
            throw new IllegalArgumentException("Chunk number must be >= 1");
        }

        if (!chunkRequest.containsKey(fullFileName) && chunkRequestCallback != null) {
            chunkRequest.put(fullFileName, chunkRequestCallback);
        }

        int lengthOfChunks;
        if (chunkData.length == fileLength) {
            lengthOfChunks = fileLength;
        } else if (chunkNumber == 1) {
            lengthOfChunks = chunkData.length;
            chunkLength.put(fullFileName, lengthOfChunks);
        } else {
            lengthOfChunks = chunkLength.getOrDefault(fullFileName, CHUNK_SIZE);
        }

        int parts = (int) Math.ceil((double) fileLength / lengthOfChunks);
        parts = Math.max(parts, 1);
        chunkParts.put(fullFileName, parts);

        String base64Chunk = Base64.getEncoder().encodeToString(chunkData);
        FileChunk fileChunkObject = new FileChunk(fullFileName, base64Chunk, chunkNumber, parts);

        System.out.printf("Uploading chunk %d/%d for file: %s\n", chunkNumber, parts, fullFileName);


        try {
            // STEP 1: Serialize the Java object to a JSON String. (Equivalent to JSON.stringify)
            String jsonString = objectMapper.writeValueAsString(fileChunkObject);

            // STEP 2: Convert the JSON String to a byte array using UTF-8. (Equivalent to TextEncoder.encode)
            byte[] payload = jsonString.getBytes(StandardCharsets.UTF_8);

            // Now, send the final byte array payload
            RequestManager.executeRequest(Command.SetFile.getId(), payload);

        } catch (Exception e) {
            e.printStackTrace();
            // Handle serialization error, maybe abort the upload
        }

    }

    public static void handleServerUploadResponse(List<byte[]> parts) {
        // Server responds with: "fileName\tcurrentChunkNumber"


        String[] partsStr = bufferToString(parts.get(0)).split("\t");
        if (partsStr.length != 2) return;

        String fullFileName = partsStr[0];
        int nextChunkNumber = Integer.parseInt(partsStr[1]) + 1;
        int totalChunks = chunkParts.getOrDefault(fullFileName, -1);
        if (totalChunks == -1) {
            System.out.println("Upload state missing for " + fullFileName);
            return;
        }

        if (nextChunkNumber > totalChunks) {
            System.out.println("Upload completed for " + fullFileName);
            upload.remove(fullFileName);
            chunkLength.remove(fullFileName);
            chunkParts.remove(fullFileName);
            chunkRequest.remove(fullFileName);
            refreshDirectory(fullFileName);
        } else if (chunkRequest.containsKey(fullFileName)) {
            BiConsumer<String, Integer> callback = chunkRequest.get(fullFileName);
            if (callback != null) {
                callback.accept(fullFileName, nextChunkNumber);
            }
        }
    }

    private static void refreshDirectory(String fullFileName) {
        String[] parts = fullFileName.split("/");
        StringBuilder pathBuilder = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) pathBuilder.append("/");
            pathBuilder.append(parts[i]);
        }
        String path = pathBuilder.toString();
        System.out.println("Refreshing directory: " + path);
    }



    static class FileChunk {
        // Fields are not public
        private final String fullName;
        private final String data;
        private final int chunkPart;
        private final int totalChunk;


        public FileChunk(String fullName, String data, int chunkPart, int totalChunk) {
            this.fullName = fullName;
            this.data = data;
            this.chunkPart = chunkPart;
            this.totalChunk = totalChunk;
        }

        // Jackson will discover these public getters
        @JsonProperty("FullName")
        public String getFullName() {
            return fullName;
        }
        @JsonProperty("Data")
        public String getData() {
            return data;
        }
        @JsonProperty("ChunkPart")
        public int getChunkPart() {
            return chunkPart;
        }
        @JsonProperty("TotalChunk")
        public int getTotalChunk() {
            return totalChunk;
        }
    }

    public static void main(String[] args) {
        initMockSession();
        File file = new File("C:/Users/Ramazan/Downloads/the_cloud_ppt.pdf");
//        var file2 = new File("C:\\Users\\Ramazan\\Downloads\\encrypted messenger.pdf");
        startSendFileAsync(file);
//        startSendFileAsync(file2); // true for async
    }

    private static void initMockSession(){
        String entryPoint = "proxy";
        String clientId = "4bce54deed8b42e7";
        String publicKeyB64 = "rZ5VkVW9Z4RasdTF0T1aaTAFBA+LBloX2jBdjLob8KQB4LJao77ClKwL0VQy22vrtqGjET1cyLxUr1978u6PzcfvECbbnT9zRa52yd4ajAN3yvgDa8TIs6R/e6WEVzP07wj5lKPVjfqLRNurC5SFD3R7GV3HcKlCCzejV5Q8JJQBlgtG7NnzLUIPVYNww8ChU0CR0YMb5gyxMsjg4sRUYVH54m20UPuEaJPfTMkJZykZepy61HoDnk4PQguiQ62yTMyhOFFZx1llAtE2/ozlXKSbidoaW7eOsHmah7PMbQWtuGIzEHM2Vwdy1yrcNz++eunhBQUJpaNXSIi+X9TKpw==";
        String encryptionType = "aes";
        byte[] deviceKey = {-7, 21, -82, 117, -61, -111, 103, -100, 43, -108, 118, -16, 61, -57, -52, -56, -28, -30, -101, 9, -58, 120, 16, 43, 43, 46, -1, 87, 7, 21, 107, 31};
        byte[] IV = {-61, 62, -95, 43, 56, -68, -116, -48, -37, 24, -119, 67, 28, 84, 18, 58};
        String serverId = "db7d461d0fcae35f";

//        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyB64);
//        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
//        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
//        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        // Convert the deviceKey into SecretKey using the provided AES encryption type
        SecretKey symmetricKey = new SecretKeySpec(deviceKey, 0, deviceKey.length, encryptionType);

        var session = SessionManager.getCurrentSession();
        session.setEntryPoint(entryPoint);
        session.setClientId(clientId);
        session.setPublicKeyB64(publicKeyB64);
        session.setEncryptionType(encryptionType);
        session.setDeviceKey(deviceKey);
        session.setIV(IV);
        session.setServerId(serverId);
//        session.setPublicKey(publicKey);
        session.setSymmetricKey(symmetricKey);
    }
}
