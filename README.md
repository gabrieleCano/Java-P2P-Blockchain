# BlockchainDay
Blockchain Coding - a step-by-step blockchain in Java

Hello fellas!

First of all, thank you for follow the project SHArpeiChain, born for the "TUTT-O-BLOCK" event (the Blockchain Day in Naples, Italy) by Napoli Blockchain.

Here the only goals are:
 - learn the blockchain technology;
 - test yourself;
 - share ideas;
 - discover and speriment new concepts.
 
So feel free to download, fork, pull request and (why not?) hack!

Any suggestions and contributions (in terms of development) will be appreciated and evaluated. You can contact me here or on the brand new Discord channel https://discord.gg/uXqGdeKr

-------------------------------------------------------------
This section will depend on the current state of the project
-------------------------------------------------------------
Current Features:
- Chain Data Structure: Java HashMap
- Digital Signatures: SHA256withECDSA by BouncyCastle
- Double-spending solution: UTXO
- Consensus: Proof-of-Work
- Network: P2P with Discovery server

These will be the next steps:

- Decouple peer actions from node action;
- Decouple mining actions from peer action;
- Define the reward mechanisms for miners;
- Develop a cli for peers;
- Develop a cli for miners;
- Improve the Peer Discovery Protocol performed by the node;
- Deploy the node software to some servers to test the blockchain on a real network.

To run the software:
- Execute mainmethod in SharPeiChain class (this will run the node thread on localhost:8080)
- Run telnet 127.0.0.1 8080 on one or more Terminal windows. 
