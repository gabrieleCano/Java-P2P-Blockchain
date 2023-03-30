package org.sharpei.blockchain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class GenesisWallet {

    private Blockchain blockchain;
    private Wallet wallet;
}
