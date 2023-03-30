package org.sharpei.p2p;

import lombok.Getter;
import lombok.Setter;
import org.sharpei.blockchain.Blockchain;
import org.sharpei.blockchain.Transaction;
import org.sharpei.blockchain.Wallet;
import org.sharpei.main.SharPeiChain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

@Getter
@Setter
public class Miner extends Thread {

    private Blockchain blockchain;

    private Wallet wallet;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    private Socket socket;
    private String peerName;
    private SharPeiChain sharPeiChain;


    public Miner(Socket socket, Blockchain blockchain, SharPeiChain sharPeiChain) throws Exception {
        this.sharPeiChain = sharPeiChain;
        this.socket = socket;
        this.blockchain = blockchain;
        this.wallet = blockchain.createWallet();
        printWriter = new PrintWriter(socket.getOutputStream(), true);
        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        printWriter.println("Connected successfully to the network. Please enter your name:");
        peerName = bufferedReader.readLine();
        printWriter.println("Welcome " + peerName + "!");
    }

    public void addTransaction(Transaction transaction) throws Exception {
        printWriter.println("Received transaction to validate.");
        blockchain.transactionIsValid(transaction);
        printWriter.println("transaction is valid.");
        blockchain.addTransaction(transaction);
        printWriter.println("Broadcasting new Blockchain");
        sharPeiChain.broadcast(blockchain, this);
        printWriter.println("Received valid transaction.");
    }

    public void setBlockchain(Blockchain blockchain) throws Exception {
        if(this.isAlive()){
            this.interrupt();
        }else{
            this.blockchain = blockchain;
            if(this.blockchain.itsTmeToMine()){
                printWriter.println("Its time to mine!");
                long start = System.currentTimeMillis();
                this.blockchain.addBlock();
                long end = System.currentTimeMillis() - start;
                printWriter.println("Block " + (blockchain.getBlockCounter()-1) + " mined in " + end + " ms" + " at difficulty " + this.blockchain.getDifficulty());
                sharPeiChain.broadcast(this.blockchain, this);
            }
        }
    }

    public void run() {
        try {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
