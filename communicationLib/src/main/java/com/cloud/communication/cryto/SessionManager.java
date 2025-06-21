package com.cloud.communication.cryto;

import static com.cloud.communication.cryto.RSAKeyManager.exportCryptoKey;
import static com.cloud.communication.cryto.RSAKeyManager.generateKeyPair;

import java.util.concurrent.CompletableFuture;

public class SessionManager {

    private static Session currentSession = new Session();

    public static Session getCurrentSession() {
        return currentSession;
    }

    public static CompletableFuture<Void> resetSession(Object id) {
        currentSession = new Session(id);

        return generateKeyPair()
                .thenCompose(keyPair -> {
                    currentSession.setPublicKey(keyPair.getPublic());
                    currentSession.setPrivateKey(keyPair.getPrivate());
                    return exportCryptoKey(currentSession.getPublicKey());
                })
                .thenAccept(pair -> {
                    currentSession.setPublicKeyB64(pair.getFirst());
                    currentSession.setClientId(pair.getSecond());
                });
    }

    public static  CompletableFuture<Void>  resetSession() {
        return resetSession(0);
    }
}
