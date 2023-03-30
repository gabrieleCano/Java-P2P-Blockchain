package org.sharpei.p2p;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.sharpei.blockchain.Blockchain;
import org.sharpei.blockchain.GenesisWallet;
import org.sharpei.blockchain.Transaction;
import org.sharpei.blockchain.Wallet;
import org.sharpei.main.SharPeiChain;
import org.sharpei.utils.GsonUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

@Getter
@Setter
@Log4j2
public class Peer extends Thread {

    private Blockchain blockchain;

    private Wallet wallet;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    private Socket socket;
    private String peerName;
    private SharPeiChain sharPeiChain;

    public Peer(Socket socket, Blockchain blockchain, SharPeiChain sharPeiChain) throws Exception {
        this.sharPeiChain = sharPeiChain;
        this.socket = socket;
        this.blockchain = blockchain;
        this.wallet = blockchain.createWallet();
        printWriter = new PrintWriter(socket.getOutputStream(), true);
        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        printWriter.println("Connected successfully to the network. Please enter your name:");
        peerName = bufferedReader.readLine();
        printWriter.println("Welcome " + peerName + "!");
        sharPeiChain.broadcast(blockchain, this);
        this.start();

    }

    public Peer(Socket socket, GenesisWallet genesisWallet, SharPeiChain sharPeiChain) throws Exception {
        this.sharPeiChain = sharPeiChain;
        this.socket = socket;
        this.blockchain = genesisWallet.getBlockchain();
        this.wallet = genesisWallet.getWallet();
        printWriter = new PrintWriter(socket.getOutputStream(), true);
        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        printWriter.println("Connected successfully to the network. Please enter your name:");
        peerName = bufferedReader.readLine();
        printWriter.println("Welcome " + peerName + ", you are the first peer!");
        sharPeiChain.broadcast(blockchain, this);
        this.start();
    }

    public void setBlockchain(Blockchain blockchain, String peerName){
        printWriter.println("New blockchain received from " + peerName);
        if(blockchain.getBlockCounter() > this.blockchain.getBlockCounter()){
            printWriter.println("And a new block was created.");
        }
        this.blockchain = blockchain;
    }

    public void run() {

        String input;
        boolean flag = true;
        try {
            while (flag) {
                printWriter.println("");
                printWriter.println("1. Send token");
                printWriter.println("2. Get my balance");
                printWriter.println("3. List Peers");
                printWriter.println("4. Download the blockchain");
                printWriter.println("5. Print current block infos");
                printWriter.println("6. Leave the network");

                if (((input = bufferedReader.readLine()) != null)) {
                    switch (input) {
                        case "1":
                            printWriter.println("Insert the recipient peer name:");
                            String peerName = bufferedReader.readLine();
                            Peer recipientPeer = sharPeiChain.findPeerByName(peerName);
                            if(recipientPeer == null){
                                printWriter.println("Cannot found a peer with the name " + peerName);
                            }else{
                                printWriter.println("Insert the amount:");
                                Double amount = Double.parseDouble(bufferedReader.readLine());
                                try{
                                    Transaction transaction = wallet.sendTransactionRequest(recipientPeer.getWallet(), amount);
                                    sharPeiChain.broadcastTransactionRequest(transaction);
                                    printWriter.println("Transaction sent to the Miners.");
                                }catch (Exception e){
                                    printWriter.println();
                                    printWriter.println(e.getMessage());
                                }
                                printWriter.println("Your balance: " + blockchain.getWalletBalance(wallet));
                            }
                            break;
                        case "2":
                            printWriter.println("Your balance: " + blockchain.getWalletBalance(wallet));
                            break;
                        case "3":
                            sharPeiChain.listPeers(this);
                            break;
                        case "4":
                            GsonUtils.printBlockchain(blockchain);
                            break;
                        case "5":
                            blockchain.getCurrentBlockInfos(printWriter);
                            break;
                        case "6":
                            printWriter.println("KA - CHOW!");
                            flag = false;
                            break;
                        default:
                            printWriter.println("Wrong input. Try again");
                            break;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
            sharPeiChain.leave(this);
            try {
                bufferedReader.close();
                printWriter.close();
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }
}
