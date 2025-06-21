package com.cloud.communication.cryto;


import java.security.PublicKey;
import java.util.Arrays;

import static com.cloud.communication.cryto.ConversionUtils.base64ToBuffer;
import static com.cloud.communication.cryto.ConversionUtils.bufferToString;
import static com.cloud.communication.cryto.ConversionUtils.byteArrayToHex;
import static com.cloud.communication.cryto.ConversionUtils.int16ToBuffer;
import static com.cloud.communication.cryto.ConversionUtils.int32ToBuffer;
import static com.cloud.communication.cryto.ConversionUtils.joinBuffers;
import static com.cloud.communication.cryto.CryptoUtils.alertBox;
import static com.cloud.communication.cryto.encryption.RsaEncryption.encryptData;


public class QrCodeHandler {
    public static void onQrCodeAcquires(String qrCode) {
        SessionManager.resetSession()
                .thenRun(() -> {
                    byte[] qr;
                    try {
                        qr = base64ToBuffer(qrCode);
                    } catch (Exception e) {
                        alertBox("Wrong QR code!");
                        return;
                    }

                    int offset = 0;
                    int type = qr[offset] & 0xFF;
                    if(type != 2){
                        alertBox("QR code format not supported!");
                        return;
                    }
                    offset++;
                    handleQrCode(qr, offset);
                });
    }


    private static void handleQrCode(byte[] qr, int offsetStart) {
        int offset = offsetStart;
        Session session = SessionManager.getCurrentSession();

        session.setQRkey(Arrays.copyOfRange(qr, offset, offset + 24));
        offset += 24;

        session.setServerId(byteArrayToHex(Arrays.copyOfRange(qr, offset, offset + 8)));
        offset += 8;

        session.setEntryPoint(bufferToString(Arrays.copyOfRange(qr, offset, qr.length)));
//        entryPointToProxy(session.getEntryPoint());

        RequestManager.executeRequest(Command.GetEncryptedQR.getId(), null);
    }



    static int chunkSize = 1024 * 256;
    static short thumbnailSize = 80;    // note: use `short` since it’s int16 in JS

    public static void setClient(PublicKey rsaPubKey) throws Exception {

        SessionManager.getCurrentSession().setDeviceKey(null);
        var publicKeyB64 = SessionManager.getCurrentSession().getPublicKeyB64();
        var clientPublicKey = base64ToBuffer(publicKeyB64);

        // Prepare clientSetting buffer: chunkSize (int32), thumbnailSize (int16), then clientPublicKey bytes
        byte[] chunkSizeBytes = int32ToBuffer(chunkSize);
        byte[] thumbnailSizeBytes = int16ToBuffer(thumbnailSize);

        // Join buffers
        byte[] clientSetting = joinBuffers(chunkSizeBytes, thumbnailSizeBytes);
        clientSetting = joinBuffers(clientSetting, clientPublicKey);

        // Encrypt using RSA public key
        byte[] clientSettingEncrypted = encryptData(rsaPubKey, clientSetting);

        // Send request (assume executeRequest is a method you have)
        RequestManager.executeRequest(Command.SetClient.getId(), clientSettingEncrypted);

//        executeRequest(Command.SetClient.getId(), clientSettingEncrypted);
    }

}
