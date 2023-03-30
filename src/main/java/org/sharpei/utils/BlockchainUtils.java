package org.sharpei.utils;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.sharpei.blockchain.Block;
import org.sharpei.blockchain.Transaction;
import org.sharpei.blockchain.Utxo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BlockchainUtils {

    public static byte[] produceHash(Block block) {
        String stringToHash = StringUtils.newStringUtf8(Optional.ofNullable(block.getPreviousBlockHash()).orElse(new byte[0]))
                + block.getTimestamp() + block.getNonce() + block.getBits()
                + StringUtils.newStringUtf8(Optional.ofNullable(block.getMerkleRoot()).orElse(new byte[0]));

        return produceHash(stringToHash);
    }

    public static byte[] produceHash(Transaction transaction) {

        String stringToHash = Hex.encodeHexString(transaction.getSender().getPublicKey().getEncoded())
                + Hex.encodeHexString(transaction.getRecipient().getPublicKey().getEncoded())
                + transaction.getTimestamp()
                + transaction.getAmount()
                + StringUtils.newStringUtf8(Optional.ofNullable(transaction.getUtxosMerkleRoot()).orElse(new byte[0]));

        return produceHash(stringToHash);
    }

    public static byte[] produceHash(Utxo utxo) {
        String stringToHash = Hex.encodeHexString(utxo.getPublicKey().getEncoded())
                + utxo.getAmount()
                + utxo.getTimestamp();

        return produceHash(stringToHash);
    }

    public static byte[] produceHash(String string) {
        return DigestUtils.sha256Hex(string).getBytes();
    }


    public static byte[] produceMerkleTree(Block block) {
        if (CollectionUtils.isNotEmpty(block.getTransactions())) {
            List<byte[]> bytes = block.getTransactions().stream().map(Transaction::getHash).collect(Collectors.toList());
            return produceMerkleTree(bytes);
        } else {
            return new byte[0];
        }
    }

    public static byte[] produceMerkleTree(Transaction transaction) {
        byte[] inputUtxosMerkleTree;
        byte[] outputUtxosMerkleTree;

        if (CollectionUtils.isNotEmpty(transaction.getInputUtxos())) {
            List<byte[]> bytes = transaction.getInputUtxos().stream().map(Utxo::getHash).collect(Collectors.toList());
            inputUtxosMerkleTree = produceMerkleTree(bytes);
        } else {
            inputUtxosMerkleTree = new byte[0];
        }

        if (CollectionUtils.isNotEmpty(transaction.getOutputUtxos())) {
            List<byte[]> bytes = transaction.getOutputUtxos().stream().map(Utxo::getHash).collect(Collectors.toList());
            outputUtxosMerkleTree = produceMerkleTree(bytes);
        } else {
            outputUtxosMerkleTree = new byte[0];
        }
        List<byte[]> bytes = new ArrayList<>();
        bytes.add(inputUtxosMerkleTree);
        bytes.add(outputUtxosMerkleTree);
        return produceMerkleTree(bytes);
    }

    public static byte[] produceMerkleTree(List<byte[]> bytesTohash) {
        List<byte[]> bytes = new ArrayList<>();
        for (int i = 0; i < bytesTohash.size(); i++) {
            if (bytesTohash.size() > i + 1) {
                String sumString = Hex.encodeHexString(bytesTohash.get(i))
                        + Hex.encodeHexString(bytesTohash.get(i + 1));
                bytes.add(produceHash(sumString));
                i++;
            } else {
                bytes.add(produceHash(Hex.encodeHexString(bytesTohash.get(i))));
            }
        }

        if (bytes.size() > 1) {
            return produceMerkleTree(bytes);
        } else {
            return bytes.get(0);
        }
    }

}
