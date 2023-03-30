package org.sharpei.main;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sharpei.blockchain.Blockchain;
import org.sharpei.blockchain.GenesisWallet;
import org.sharpei.blockchain.Transaction;
import org.sharpei.p2p.Miner;
import org.sharpei.p2p.Peer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@Getter
public class SharPeiChain {

    private static List<Peer> peerList;
    private static List<Miner> minerList;
    private static ServerSocket serverSocket;
    private static SharPeiChain sharPeiChain;
    private Blockchain blockchain;

    public static void main(String[] args) throws Exception {
        if (sharPeiChain == null) {
            sharPeiChain = new SharPeiChain();
        }
        while (true) {
            Socket socket = serverSocket.accept();
            if (CollectionUtils.isEmpty(peerList)) {
                peerList = new ArrayList<>();
                minerList = new ArrayList<>();
                GenesisWallet genesisWallet = Blockchain.createBlockchain();
                Peer firstPeer = new Peer(socket, genesisWallet, sharPeiChain);
                peerList.add(firstPeer);
            } else {
                PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
                printWriter.println("Are you a miner? (y or n)");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String input;
                while((input = bufferedReader.readLine()) != null){
                    if (StringUtils.equalsIgnoreCase(input, "y") || StringUtils.equalsIgnoreCase(input, "n")) break;
                    else printWriter.println("Wrong input. Retry");
                }
                if(StringUtils.equalsIgnoreCase(input, "y")){
                    minerList.add(new Miner(socket, sharPeiChain.getBlockchain(),sharPeiChain));
                }else{
                    peerList.add(new Peer(socket, sharPeiChain.getBlockchain(), sharPeiChain));
                }
            }

        }
    }

    private SharPeiChain() throws IOException {
        serverSocket = new ServerSocket(8080);
    }

    public void listPeers(Peer peer) {
        if (peerList.size() > 1) {
            for (Peer currentPeer : peerList) {
                if (!StringUtils.equals(currentPeer.getPeerName(), peer.getPeerName())) {
                    peer.getPrintWriter().println(currentPeer.getPeerName());
                }
            }
        } else {
            peer.getPrintWriter().println("You are the only peer :')");
        }
    }

    public void broadcastTransactionRequest(Transaction transaction) throws Exception {
        for (Miner currentMiner : minerList) {
            currentMiner.addTransaction(transaction);
        }
    }

    public void broadcast(Blockchain blockchain, Peer peer) throws Exception {
        this.blockchain = blockchain;
        for (Peer currentPeer : peerList) {
                currentPeer.setBlockchain(blockchain, peer.getPeerName());
        }
        for (Miner currentMiner : minerList) {
            currentMiner.setBlockchain(blockchain);
        }
    }

    public void broadcast(Blockchain blockchain, Miner miner) throws Exception {
        this.blockchain = blockchain;
        for (Peer currentPeer : peerList) {
            currentPeer.setBlockchain(blockchain, miner.getPeerName());
        }
        for (Miner currentMiner : minerList) {
                currentMiner.setBlockchain(blockchain);
        }
    }

    public void leave(Peer peer) {
        for (Peer currentPeer : peerList) {
            if (!StringUtils.equals(currentPeer.getPeerName(), peer.getPeerName())) {
                currentPeer.getPrintWriter().println(peer.getPeerName() + " left the network");
            }
        }
        peerList.removeIf(peer1 -> peer1.getSocket().equals(peer.getSocket()));
    }

    public Peer findPeerByName(String name) {
        return peerList.stream().filter(peer1 -> peer1.getPeerName().equals(name)).findFirst().orElse(null);
    }

//    public static void main(String[] args) throws Exception {
//        Wallet genesisWallet = Blockchain.createBlockchain();
//        Blockchain blockchain = Blockchain.getInstance();
//        Wallet wallet_1 = blockchain.createWallet();
//        Wallet wallet_2 = blockchain.createWallet();
//        Wallet wallet_3 = blockchain.createWallet();
//
////        genesisWallet.sendTransactionRequest(wallet_1, 5.0);
////        genesisWallet.sendTransactionRequest(wallet_1, 15.0);
////        genesisWallet.sendTransactionRequest(wallet_2, 10.0);
//
////        genesisWallet.sendTransactionRequest(wallet_3, 20.0);
//
//        wallet_1.sendTransactionRequest(wallet_3, 7.0);
//        wallet_2.sendTransactionRequest(wallet_3, 1.9);
//
//        wallet_3.sendTransactionRequest(wallet_1, 7.0);
//        wallet_2.sendTransactionRequest(wallet_3, 1.9);
//
//        log.info(blockchain.getWalletBalance(wallet_3));
//        log.info(blockchain.getWalletBalance(wallet_2));
//        log.info(blockchain.getWalletBalance(wallet_1));
//        log.info(blockchain.getWalletBalance(genesisWallet));
//
//        blockchain.validateBlockchain();
//        GsonUtils.printBlockchain(blockchain);
////
//        //TEST TEST TEST
//    }
}