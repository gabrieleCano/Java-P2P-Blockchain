package org.sharpei.blockchain;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.sharpei.utils.BlockchainUtils;
import org.sharpei.utils.GsonUtils;

import java.security.PublicKey;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
public class Utxo {
    @Expose
    private byte[] parentTransaction;
    @Expose
    private Double amount;
    @Expose
    private PublicKey publicKey;
    @Expose
    private Date timestamp;
    @Expose
    private byte[] hash;

    public Utxo(PublicKey publicKey, Double amount) {
        this.publicKey = publicKey;
        this.amount = amount;
        timestamp = new Date(System.currentTimeMillis());
        hash = BlockchainUtils.produceHash(this);
    }

    public String toString() {
        return GsonUtils.printUtxo(this);
    }
}
