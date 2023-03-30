package org.sharpei.blockchain;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.sharpei.utils.BlockchainUtils;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class Transaction {
    @Expose
    private Wallet sender;
    @Expose
    private Wallet recipient;
    @Expose
    private Date timestamp;
    @Expose
    private Double amount;
    @Expose
    private byte[] hash;
    private byte[] signature;
    @Expose
    private byte[] utxosMerkleRoot;
    @Expose
    private List<Utxo> inputUtxos;
    @Expose
    private List<Utxo> outputUtxos;

    public Transaction(Wallet sender, Wallet recipient, Double amount) {
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
        this.timestamp = new Date(System.currentTimeMillis());
    }

    public void produceHash() throws IOException {

        if (hash == null) {
            utxosMerkleRoot = BlockchainUtils.produceMerkleTree(this);
            hash = BlockchainUtils.produceHash(this);
            if (CollectionUtils.isNotEmpty(outputUtxos)) {
                outputUtxos.forEach(c -> c.setParentTransaction(hash));
            }
        }
    }
}
