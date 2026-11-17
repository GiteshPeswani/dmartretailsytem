package com.dmart.blockchain.entity;

public enum BlockchainStatus {
    NOT_REGISTERED,  // product not yet sent to blockchain
    NOT_SUBMITTED,   // order not yet sent to blockchain
    PENDING,         // tx submitted, awaiting confirmation
    CONFIRMED,       // tx mined and confirmed
    FAILED           // tx failed or timed out
}
