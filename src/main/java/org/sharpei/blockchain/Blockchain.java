package org.sharpei.blockchain;

import com.google.gson.JsonElement;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.Level;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.sharpei.utils.BlockchainUtils;
import org.sharpei.utils.GsonUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.*;
import java.util.*;

@Log4j2
public class Blockchain {

    //    private static Blockchain blockchain;
    private final Map<byte[], Block> blockMap;
    private Block latestBlock;
    private final KeyPairGenerator keyPairGenerator;
    public final Signature ecdsaVerify;
    private int transactionsCounter;
    private int blockCounter = 0;
    private List<Transaction> transactionsPool;
    private static final int TRANSACTIONS_BLOCK_LIMIT = 2;
    private int difficulty;
    private long totalElapsedInMillis;
    private final int bitsEvaluationChunk = 1;   //2016;
    private final int maxMiningTimeInMillis = 10_000;//600_000;

    private Blockchain() {
        blockMap = new HashMap<>();
        transactionsPool = new ArrayList<>();
        transactionsCounter = 0;
        difficulty = 1;

        Security.addProvider(new BouncyCastleProvider());
        //Definiamo lo standard che vogliamo utilizzare
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("Secp256k1"); //B-571
        try {
            //ECDSA indica l'algoritmo che vogliamo utilizzare per definire i valori, e di conseguenza le chiavi, sulla curva
            keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
            keyPairGenerator.initialize(ecSpec, new SecureRandom());
            //con SHA2567 indichiamo l'hash function con la quale verrá criptato il messaggio e che sará elaborato con l'algoritmo ECDSA
            ecdsaVerify = Signature.getInstance("SHA256withECDSA", "BC");
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    public void getCurrentBlockInfos(PrintWriter printWriter) {
        printWriter.println("Last Block Hash:" + Hex.encodeHexString(latestBlock.getHash()));
        printWriter.println("Last Block Number:" + blockCounter);
        if (CollectionUtils.isNotEmpty(transactionsPool)) {
            printWriter.println("Current transactions in pool:" + transactionsPool.size());
        }
    }

    public int getBlockCounter() {
        return blockCounter;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public Block getLatestBlock() {
        return latestBlock;
    }

    public Block getBlock(byte[] hash) {
        return blockMap.get(hash);
    }

    public List<Transaction> getTransactionPool() {
        return transactionsPool;
    }

    public void addTransaction(Transaction transaction) throws Exception {
        if (CollectionUtils.isEmpty(transactionsPool)
                || transactionsPool.stream().noneMatch(transaction1 -> transaction1.getHash().equals(transaction))) {
            signatureisValid(transaction);
            transactionsCounter++;
            transactionsPool.add(transaction);
        }
//        addBlock();
    }

    public Wallet createWallet() throws Exception {

        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        Wallet wallet = new Wallet(keyPair, ecdsaVerify);

        Transaction transaction = new Transaction(wallet, wallet, 10.0);

        List<Utxo> outputUtxos = Collections.singletonList(new Utxo(transaction.getRecipient().getPublicKey(), transaction.getAmount()));
        transaction.setOutputUtxos(outputUtxos);
        transaction.produceHash();
        ecdsaVerify.initSign(keyPair.getPrivate());
        ecdsaVerify.update(transaction.getHash());
        byte[] signature = ecdsaVerify.sign();
        transaction.setSignature(signature);
        addTransaction(transaction);

        return wallet;
    }

    public void transactionIsValid(Transaction transaction) throws Exception {

        Map<PublicKey, Utxo> publicKeyUtxoMap = new HashMap<>();
        publicKeyUtxoMap.put(transaction.getSender().getPublicKey(), null);
        publicKeyUtxoMap.put(transaction.getRecipient().getPublicKey(), null);

        findLastSenderAndRecipientUtxo(transactionsPool, publicKeyUtxoMap, transaction.getSender().getPublicKey(), transaction.getRecipient().getPublicKey());

        Block currentReadingBlock = latestBlock;
        while ((publicKeyUtxoMap.get(transaction.getSender().getPublicKey()) == null || publicKeyUtxoMap.get(transaction.getRecipient().getPublicKey()) == null) && currentReadingBlock != null) {
            findLastSenderAndRecipientUtxo(currentReadingBlock.getTransactions(), publicKeyUtxoMap, transaction.getSender().getPublicKey(), transaction.getRecipient().getPublicKey());
            currentReadingBlock = blockMap.get(currentReadingBlock.getPreviousBlockHash());
        }
        Utxo senderLastUtxo = publicKeyUtxoMap.get(transaction.getSender().getPublicKey());
        Utxo recipientLastUtxo = publicKeyUtxoMap.get(transaction.getRecipient().getPublicKey());

        if (senderLastUtxo == null || recipientLastUtxo == null || transaction.getAmount() > senderLastUtxo.getAmount()) {
            String message = "Cannot found previous UTXOs or you don't have sufficient funds";
            throw new Exception(message);
        } else {
            List<Utxo> inputUtxos = new ArrayList<>();
            inputUtxos.add(senderLastUtxo);
            inputUtxos.add(recipientLastUtxo);

            List<Utxo> outputUtxos = new ArrayList<>();
            double finalAmount = transaction.getAmount() + (recipientLastUtxo != null ? recipientLastUtxo.getAmount() : 0D);
            Utxo recipientNewUtxo = new Utxo(transaction.getRecipient().getPublicKey(), finalAmount);
            Utxo senderNewUtxo = new Utxo(transaction.getSender().getPublicKey(), senderLastUtxo.getAmount() - transaction.getAmount());

            outputUtxos.add(recipientNewUtxo);
            outputUtxos.add(senderNewUtxo);

            transaction.setInputUtxos(inputUtxos);
            transaction.setOutputUtxos(outputUtxos);
        }
    }

    private void signatureisValid(Transaction transaction) throws Exception {
        ecdsaVerify.initVerify(transaction.getSender().getPublicKey());
        ecdsaVerify.update(transaction.getHash());
        if (!ecdsaVerify.verify(transaction.getSignature())) {
            String message = "Invalid signature on transaction";
            log.log(Level.ERROR, message);
            throw new Exception(message);
        }
    }

    private void findLastSenderAndRecipientUtxo(List<Transaction> transactions, Map<PublicKey, Utxo> publicKeyUtxoMap, PublicKey senderPublicKey, PublicKey recipientPublicKey) throws Exception {
        if (CollectionUtils.isNotEmpty(transactions)) {
            for (int i = transactions.size() - 1; i > -1 && (publicKeyUtxoMap.get(senderPublicKey) == null || publicKeyUtxoMap.get(recipientPublicKey) == null); i--) {
                Transaction currentReadingTransaction = transactions.get(i);
                signatureisValid(currentReadingTransaction);
                if (CollectionUtils.isNotEmpty(currentReadingTransaction.getOutputUtxos())
                        && (publicKeyUtxoMap.get(senderPublicKey) == null &&
                        (currentReadingTransaction.getRecipient().getPublicKey().equals(senderPublicKey)
                                || currentReadingTransaction.getSender().getPublicKey().equals(senderPublicKey)))
                        || (publicKeyUtxoMap.get(recipientPublicKey) == null &&
                        (currentReadingTransaction.getRecipient().getPublicKey().equals(recipientPublicKey)
                                || currentReadingTransaction.getSender().getPublicKey().equals(recipientPublicKey)))) {
                    for (int j = currentReadingTransaction.getOutputUtxos().size() - 1; j > -1 && (publicKeyUtxoMap.get(senderPublicKey) == null || publicKeyUtxoMap.get(recipientPublicKey) == null); j--) {
                        Utxo currentReadingUtxo = currentReadingTransaction.getOutputUtxos().get(j);
                        if (publicKeyUtxoMap.get(senderPublicKey) == null && currentReadingUtxo.getPublicKey().equals(senderPublicKey)) {
                            publicKeyUtxoMap.put(senderPublicKey, currentReadingUtxo);
                        } else if (publicKeyUtxoMap.get(recipientPublicKey) == null && currentReadingUtxo.getPublicKey().equals(recipientPublicKey)) {
                            publicKeyUtxoMap.put(recipientPublicKey, currentReadingUtxo);
                        }
                    }
                }
            }
        }
    }

    public void addBlock() {
        transactionsCounter += transactionsPool.size();
        long startTime = System.currentTimeMillis();
        Block block = new Block(this.latestBlock, transactionsPool, difficulty);
        totalElapsedInMillis += System.currentTimeMillis() - startTime;

        if ((block.getIndex() % bitsEvaluationChunk) == 0) {
            //Ricalcola difficoltà
            long avgMiningTime = totalElapsedInMillis / bitsEvaluationChunk;

            if (avgMiningTime > maxMiningTimeInMillis * 1.25D) {
                if (difficulty > 1) {
                    difficulty -= 1;
                }
            } else if (avgMiningTime < maxMiningTimeInMillis * 0.75D) {
                difficulty += 1;
            }
        }
        blockMap.put(block.getHash(), block);
        latestBlock = block;
        transactionsPool = new ArrayList<>();
        blockCounter++;
    }

    public boolean itsTmeToMine() {
        return CollectionUtils.isNotEmpty(this.transactionsPool) && this.transactionsPool.size() >= TRANSACTIONS_BLOCK_LIMIT;
    }

    public static GenesisWallet createBlockchain() throws SignatureException, InvalidKeyException, IOException {
//        if (blockchain == null) {
        Blockchain blockchain = new Blockchain();
        KeyPair keyPair = blockchain.keyPairGenerator.generateKeyPair();
        Wallet wallet = new Wallet(keyPair, blockchain.ecdsaVerify);
        Transaction transaction = new Transaction(wallet, wallet, 50.0);

        List<Utxo> outputUtxos = Collections.singletonList(new Utxo(transaction.getRecipient().getPublicKey(), transaction.getAmount()));
        transaction.setOutputUtxos(outputUtxos);
        transaction.produceHash();

        blockchain.ecdsaVerify.initSign(keyPair.getPrivate());
        blockchain.ecdsaVerify.update(transaction.getHash());
        byte[] signature = blockchain.ecdsaVerify.sign();
        transaction.setSignature(signature);
        blockchain.ecdsaVerify.initVerify(transaction.getSender().getPublicKey());
        blockchain.ecdsaVerify.update(transaction.getHash());
        boolean verifiedSign = blockchain.ecdsaVerify.verify(transaction.getSignature());
        if (verifiedSign) {
            blockchain.transactionsCounter++;
            Block block = new Block(transaction);
            blockchain.blockMap.put(block.getHash(), block);
            blockchain.latestBlock = block;
            blockchain.blockCounter = 1;
        } else {
            wallet = null;
        }
//        }
        return new GenesisWallet(blockchain, wallet);
    }

    public String getWalletBalance(Wallet wallet) {
        Utxo lastUtxo = null;
        if (CollectionUtils.isNotEmpty(transactionsPool)) {
            for (int i = transactionsPool.size() - 1; i > -1 && lastUtxo == null; i--) {
                if (transactionsPool.get(i).getRecipient().getPublicKey().equals(wallet.getPublicKey())
                        || transactionsPool.get(i).getSender().getPublicKey().equals(wallet.getPublicKey())) {
                    if (CollectionUtils.isNotEmpty(transactionsPool.get(i).getOutputUtxos())) {
                        for (int j = transactionsPool.get(i).getOutputUtxos().size() - 1; j > -1 && lastUtxo == null; j--) {
                            if (transactionsPool.get(i).getOutputUtxos().get(j).getPublicKey().equals(wallet.getPublicKey())) {
                                lastUtxo = transactionsPool.get(i).getOutputUtxos().get(j);
                            }
                        }
                    }
                }
            }
        }
        if (lastUtxo == null) {
            Block currentReadingBlock = latestBlock;
            while (currentReadingBlock != null && lastUtxo == null) {
                if (CollectionUtils.isNotEmpty(currentReadingBlock.getTransactions())) {
                    for (int i = currentReadingBlock.getTransactions().size() - 1; i > -1 && lastUtxo == null; i--) {
                        if (currentReadingBlock.getTransactions().get(i).getRecipient().getPublicKey().equals(wallet.getPublicKey())
                                || currentReadingBlock.getTransactions().get(i).getSender().getPublicKey().equals(wallet.getPublicKey())) {
                            if (CollectionUtils.isNotEmpty(currentReadingBlock.getTransactions().get(i).getOutputUtxos())) {
                                for (int j = currentReadingBlock.getTransactions().get(i).getOutputUtxos().size() - 1; j > -1 && lastUtxo == null; j--) {
                                    if (currentReadingBlock.getTransactions().get(i).getOutputUtxos().get(j).getPublicKey().equals(wallet.getPublicKey())) {
                                        lastUtxo = currentReadingBlock.getTransactions().get(i).getOutputUtxos().get(j);
                                    }
                                }
                            }
                        }
                    }
                }
                currentReadingBlock = blockMap.get(currentReadingBlock.getPreviousBlockHash());
            }
        }
        if (lastUtxo != null) {
            return lastUtxo.getAmount().toString();
        }
        return "Empty wallet";
    }

//    public static Blockchain getInstance() {
//        return blockchain;
//    }

    public void readBlockchain() {
        log.log(Level.INFO, "This is the latest Block:");
        log.log(Level.INFO, latestBlock);

        byte[] previousBlockHash = latestBlock.getPreviousBlockHash();

        while (previousBlockHash != null) {
            Block currentBlock = blockMap.get(previousBlockHash);
            log.log(Level.INFO, "This is Block number " + currentBlock.getIndex());
            log.log(Level.INFO, currentBlock);
            previousBlockHash = currentBlock.getPreviousBlockHash();
        }
    }


    public List<JsonElement> getJson() {
        List<JsonElement> jsonElements = new ArrayList<>();
        jsonElements.add(GsonUtils.blockToJsonElement(latestBlock));

        byte[] previousBlockHash = latestBlock.getPreviousBlockHash();

        while (previousBlockHash != null) {
            Block currentBlock = blockMap.get(previousBlockHash);
            jsonElements.add(GsonUtils.blockToJsonElement(currentBlock));
            previousBlockHash = currentBlock.getPreviousBlockHash();
        }

        return jsonElements;
    }

    public String toString() {
        return GsonUtils.printBlockchain(this);
    }

    public void validateBlockchain() throws IOException {
        Block currentLatestBlock = latestBlock;

        byte[] previousBlockHash = latestBlock.getPreviousBlockHash();

        while (previousBlockHash != null) {
            if (CollectionUtils.isNotEmpty(currentLatestBlock.getTransactions())) {
                for (Transaction transaction : currentLatestBlock.getTransactions()) {
                    byte[] merkleRootCalculated = BlockchainUtils.produceMerkleTree(transaction);
                    if (!Arrays.equals(transaction.getUtxosMerkleRoot(), merkleRootCalculated)) {
                        log.log(Level.ERROR, "Blockchain is invalid");
                        return;
                    }
                    byte[] hash = BlockchainUtils.produceHash(transaction);
                    if (!Arrays.equals(transaction.getHash(), hash)) {
                        log.log(Level.ERROR, "Blockchain is invalid");
                        return;
                    }
                    if (CollectionUtils.isNotEmpty(transaction.getOutputUtxos())) {
                        for (Utxo utxo : transaction.getOutputUtxos()) {
                            if (!Arrays.equals(utxo.getParentTransaction(), hash)) {
                                log.log(Level.ERROR, "Blockchain is invalid");
                                return;
                            }
                        }
                    }
                }

                byte[] merkleRootCalculated = BlockchainUtils.produceMerkleTree(currentLatestBlock);
                if (!Arrays.equals(currentLatestBlock.getMerkleRoot(), merkleRootCalculated)) {
                    log.log(Level.ERROR, "Blockchain is invalid");
                    return;
                }
            }

            Block currentReadingBlock = blockMap.get(previousBlockHash);
            if (currentReadingBlock.getIndex() + 1L != currentLatestBlock.getIndex()
                    || currentLatestBlock.getTimestamp().getTime() < currentReadingBlock.getTimestamp().getTime()) {
                log.log(Level.ERROR, "Blockchain is invalid");
                return;
            }

            byte[] currentBlockHash = currentReadingBlock.getHash();
            byte[] calculatedCurrentBlockHash = BlockchainUtils.produceHash(currentReadingBlock);
            if (!Arrays.equals(calculatedCurrentBlockHash, currentLatestBlock.getPreviousBlockHash())
                    || !Arrays.equals(calculatedCurrentBlockHash, currentBlockHash)) {
                log.log(Level.ERROR, "Blockchain is invalid");
                return;
            }

            previousBlockHash = currentReadingBlock.getPreviousBlockHash();
            currentLatestBlock = currentReadingBlock;
        }
        log.log(Level.INFO, "Blockchain is valid");
    }
}
