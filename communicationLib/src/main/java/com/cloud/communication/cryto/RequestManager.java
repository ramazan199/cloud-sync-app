package com.cloud.communication.cryto;

import kotlin.Pair;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.cloud.communication.cryto.Command.getCommandName;
import static com.cloud.communication.cryto.ConversionUtils.base64ToBuffer;
import static com.cloud.communication.cryto.ConversionUtils.int32ToBuffer;
import static com.cloud.communication.cryto.ConversionUtils.joinBuffers;
import static com.cloud.communication.cryto.CryptoUtils.alertBox;
import static com.cloud.communication.cryto.CryptoUtils.splitData;
import static com.cloud.communication.cryto.FileUploader.handleServerUploadResponse;
import static com.cloud.communication.cryto.FileUploader.startSendFileAsync;
import static com.cloud.communication.cryto.QrCodeHandler.setClient;
import static com.cloud.communication.cryto.encryption.AesEncryption.decryptData;
import static com.cloud.communication.cryto.encryption.AesEncryption.encryptData;
import static com.cloud.communication.cryto.encryption.RsaEncryption.createRsaPublicKey;
import static com.cloud.communication.cryto.encryption.XorEncryption.decryptXorAB;

import com.cloud.communication.cryto.encryption.RsaEncryption;


public class RequestManager {

    private static final List<Pair<Integer, byte[]>> spooler = new ArrayList<>();
    private static final AtomicInteger concurrentRequest = new AtomicInteger(0);
    private static final int maxConcurrentRequest = 5;

    private static final OkHttpClient client = new OkHttpClient();

    // TODO: add to settings file
    public static String proxy = "http://proxy.tc0.it:5050";
//    public static String proxy = "http://195.20.235.5:5050";


    // Executor for background tasks (like Kotlin coroutines)
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static synchronized void enqueueRequest(Integer commandId, byte[] data) {
        spooler.add(new Pair<>(commandId, data));
        tryStartNext();
    }

    private static synchronized void tryStartNext() {
        if (concurrentRequest.get() < maxConcurrentRequest) {
            Pair<Integer, byte[]> nextRequest = null;
            if (!spooler.isEmpty()) {
                nextRequest = spooler.remove(spooler.size() - 1);
            }
            if (nextRequest != null) {
                concurrentRequest.incrementAndGet();
                executeRequest(nextRequest.getFirst(), nextRequest.getSecond());
            }
        }
    }

    public static void executeRequest(Integer commandId, byte[] data) {
        if (commandId == null) {
            alertBox("Command does not exist");
            requestDone();
            return;
        }

        byte[] requestData = data != null ? data : new byte[0];
        String proxyUrl = proxy + "/data";
        HttpUrl baseUrl = HttpUrl.parse(proxyUrl);
        if (baseUrl == null) {
            alertBox("Invalid proxy URL");
            requestDone();
            return;
        }

        var urlBuilder = baseUrl.newBuilder();
        urlBuilder.addQueryParameter("cid", SessionManager.getCurrentSession().getClientId());

        String purpose = null;
        if (commandId == Command.SetClient.getId() || commandId == Command.GetEncryptedQR.getId()) {
            urlBuilder.addQueryParameter("sid", SessionManager.getCurrentSession().getServerId());
            purpose = getCommandName(commandId);
            urlBuilder.addQueryParameter("purpose", purpose);
        }

        HttpUrl url = urlBuilder.build();
        Request.Builder requestBuilder = new Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .header("Accept-Language", "en-US,en;q=0.9");

        boolean isGet = commandId == Command.GetPushNotifications.getId();
        if (isGet) {
            getRequest(requestBuilder);
            return;
        }


        if (purpose != null) {
            postRequest(requestBuilder, requestData);
            return;
        }


        if (SessionManager.getCurrentSession().getDeviceKey() == null) {
            alertBox("Unregistered user. You need to log in to the server to initialize the encryption.");
            requestDone();
            return;
        }


        byte[] cmdBuffer = int32ToBuffer(commandId);
        if (requestData.length == 0) {
            requestData = cmdBuffer;
        } else {
            requestData = joinBuffers(cmdBuffer, requestData);
        }

        // Encrypt and send asynchronously
        byte[] finalRequestData = requestData;
        byte[] encrypted;
        try {
            encrypted = encryptData(finalRequestData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        postRequest(requestBuilder, encrypted);

        requestDone();
    }

    private static void getRequest(Request.Builder requestBuilder) {
        getEnqueue(requestBuilder.build());
    }

    private static void postRequest(Request.Builder requestBuilder, byte[] data) {
        MediaType mediaType = MediaType.parse("application/octet-stream");
        RequestBody body = RequestBody.create(data, mediaType);
        Request request = requestBuilder.post(body).build();
        getEnqueue(request);
    }

    private static void getEnqueue(Request request) {
        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                alertBox("HTTP Request error: " + e.getMessage());
                requestDone();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                requestDone();
                int code = response.code();
                switch (code) {
                    case 404:
                        alertBox("Status 404: Cloud not found by SID. No cloud with this User ID has registered in the proxy.");
                        break;
                    case 503:
                        alertBox("Status 503: Max request concurrent limit reached.");
                        break;
                    case 421:
                        alertBox("Status 421: The cloud is not logged into the proxy. Please restart it.");
                        break;
                    case 200:
                        handleSuccessfulResponse(response);
                        break;
                    default:
                        alertBox("HTTP error code: " + code);
                        break;
                }
            }
        });
    }

    private static void handleSuccessfulResponse(Response response) {
        try (ResponseBody body = response.body()) {
            if (body == null) {
                alertBox("Response body is null.");
                return;
            }

            String responseText = body.string();
            if (responseText.isEmpty()) {
                alertBox("Response body is empty.");
                return;
            }

            // Handle response asynchronously
            executor.submit(() -> {
                try {
                    handleResponse(responseText);
                } catch (Exception e) {
                    e.printStackTrace();
                    alertBox("Error processing response: " + e.getMessage());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            alertBox("Error reading response body: " + e.getMessage());
        }
    }

    private static void handleResponse(String responseText) {
        try {
            Session session = SessionManager.getCurrentSession();
            if (session == null) {
                alertBox("No active session found.");
                return;
            }

            if (session.getQRkey() != null) {
                getEncryptedQR(responseText);
            } else if (session.getDeviceKey() != null || session.getEncryptionType() != null) {
                var decrypted = decryptData(base64ToBuffer(responseText));
                onResponse(decrypted);
            } else {
                var response = base64ToBuffer(responseText);
                var decrypted = RsaEncryption.decryptData(SessionManager.getCurrentSession().getPrivateKey(), response);
                onResponse(decrypted);
            }

        } catch (Exception e) {
            e.printStackTrace();
            alertBox("Error handling response: " + e.getMessage());
        }
    }

    private static synchronized void requestDone() {
        concurrentRequest.decrementAndGet();
        tryStartNext();
    }


    public static void getEncryptedQR(String encryptedDataB64) throws Exception {
        byte[] encryptedData = base64ToBuffer(encryptedDataB64);
        byte[] decryptedData = decryptXorAB(SessionManager.getCurrentSession().getQRkey(), encryptedData);
        SessionManager.getCurrentSession().setQRkey(null); // clear QR key

        int offset = 0;
        // First byte is 'type'
        int type = decryptedData[offset] & 0xFF;
        if (type != 2) {
            throw new RuntimeException("QR code format not supported!");
        }
        offset += 1;

        int mSize = 2048 / 8; // 256 bytes modulus size
        byte[] modulus = Arrays.copyOfRange(decryptedData, offset, offset + mSize);
        offset += mSize;
        byte[] exponent = Arrays.copyOfRange(decryptedData, offset, offset + 3);
        PublicKey rsaPubKey = createRsaPublicKey(modulus, exponent);
        setClient(rsaPubKey);
    }


    public static void onResponse(byte[] binary) {

        // Parse first 4 bytes into int (big-endian, like JS DataView by default)
        ByteBuffer buffer = ByteBuffer.wrap(binary);
        buffer.order(ByteOrder.LITTLE_ENDIAN); // network order

        int commandId = buffer.getInt(); // read first 4 bytes
        String command = getCommandName(commandId);
        if (Command.Authentication.getId() == commandId) {
            AuthSuccess();
            return;
        }

        // The remaining data
        byte[] data = Arrays.copyOfRange(binary, 4, binary.length);

        // Split data to params
        var params = splitData(data);

        // For example:
        System.out.println("Command ID: " + commandId);
        System.out.println("Command: " + command);
        System.out.println("Params: " + params);
        if(Command.SetFile.getId() == commandId){
            handleServerUploadResponse(params);
        }
        if(Command.Pair.getId() == commandId){
            Pairing.Pair(params);
        }
    }

    private static void AuthSuccess() {
        File file = new File("C:/Users/Ramazan/Downloads/the_cloud_ppt.pdf");
//        var file2 = new File("C:\\Users\\Ramazan\\Downloads\\encrypted messenger.pdf");
        startSendFileAsync(file);
//        startSendFileAsync(file2);
        //TODO: SaveClient();
    }


//    private static class Pair<F, S> {
//        public final F first;
//        public final S second;
//
//        Pair(F first, S second) {
//            this.first = first;
//            this.second = second;
//        }
//    }
}
