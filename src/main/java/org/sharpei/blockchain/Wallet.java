package org.sharpei.blockchain;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.security.*;

@Log4j2
public class Wallet {

    private final KeyPair keyPair;
    private final Signature ecdsaSign;

    public Wallet(KeyPair keyPair, Signature ecdsaSign) {
        this.keyPair = keyPair;
        this.ecdsaSign = ecdsaSign;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public Transaction sendTransactionRequest(Wallet recipient, Double amount) throws SignatureException, InvalidKeyException, IOException {
        Transaction transaction = new Transaction(this, recipient, amount);
        transaction.produceHash();
        signTransaction(transaction);
        return transaction;
    }

    public void sendTransactionRequest(Blockchain blockchain, Wallet recipient, Double amount) throws Exception {
        Transaction transaction = new Transaction(this, recipient, amount);
        blockchain.transactionIsValid(transaction);
        transaction.produceHash();
        signTransaction(transaction);
        blockchain.addTransaction(transaction);
    }

    private void signTransaction(Transaction transaction) throws InvalidKeyException, SignatureException {
        ecdsaSign.initSign(keyPair.getPrivate());
        ecdsaSign.update(transaction.getHash());
        byte[] signature = ecdsaSign.sign();
        transaction.setSignature(signature);
    }
}
