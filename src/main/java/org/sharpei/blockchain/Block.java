package org.sharpei.blockchain;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.logging.log4j.Level;
import org.sharpei.utils.BlockchainUtils;
import org.sharpei.utils.GsonUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
@Log4j2
@Getter
@Setter
public class Block {

    @Expose
    private Long index;
    @Expose
    private Date timestamp;
    @Expose
    private byte[] hash;
    @Expose
    private byte[] previousBlockHash;
    @Expose
    private byte[] merkleRoot;
    @Expose
    private List<Transaction> transactions;
    @Expose
    private int transactionsCounter;
    @Expose
    private int bits;
    @Expose
    private long miningTime;
    @Expose
    private int nonce;

    protected Block(Block previousBlock, List<Transaction> transactions, int difficulty) {
        previousBlockHash = previousBlock.getHash();
        index = previousBlock.getIndex() + 1L;
        timestamp = new Date(System.currentTimeMillis());
        this.transactions = transactions;
        transactionsCounter = transactions.size();
        merkleRoot = BlockchainUtils.produceMerkleTree(this);
        bits = difficulty;
        hash = proofOfWork(this, difficulty);
        miningTime = System.currentTimeMillis();

    }

    protected Block(Transaction genesisTransaction) {
        //This is the Genesis block
        index = 0L;
        timestamp = new Date(System.currentTimeMillis());
        transactions = Collections.singletonList(genesisTransaction);
        transactionsCounter = 1;
        merkleRoot = BlockchainUtils.produceMerkleTree(this);
        hash = BlockchainUtils.produceHash(this);

    }

    public byte[] proofOfWork(Block block, int difficulty) {
        String target = new String(new char[difficulty]).replace('\0', '0');
        int nonce = 0;
        long startTime = System.currentTimeMillis();
        long endTime = 0;
        byte[] hash = null;
        while (endTime < 60_000L) {
            log.info("Trying nonce " + nonce + " for block " + block.getIndex() + " at difficulty " + difficulty);
            block.setNonce(nonce++);
            hash = BlockchainUtils.produceHash(block);
            endTime = System.currentTimeMillis() - startTime;
            if (StringUtils.newStringUtf8(hash).substring(0, difficulty).equals(target)) {
                log.log(Level.INFO,"Block " + block.getIndex() + " mined in " + endTime + "ms at difficulty " + difficulty);
                return hash;
            }
        }
        return hash;
    }

    public String toString() {
        return GsonUtils.printBlock(this);
    }
}
