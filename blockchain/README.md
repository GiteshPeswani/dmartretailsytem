# D-Mart Blockchain — Smart Contracts

This directory contains the Solidity smart contracts, Hardhat configuration, deployment scripts, and automated tests for the **Blockchain-Based D-Mart Retail System**.

## Contract: `RetailAuditRegistry.sol`

An immutable on-chain audit registry that stores:
- **Product registrations** — a hash of immutable product data, written once at product creation time.
- **Order lifecycle events** — an append-only chain of audit events for every order status transition.

### Key Features
- Role-based operator access control (owner + granted operators)
- Custom Solidity errors for clear revert reasons
- Enforced order-status state machine (prevents illegal transitions)
- Chained audit events via `prevEventHash` (tamper evidence)
- Public hash-verification functions (anyone can verify off-chain data against on-chain hashes)

---

## Prerequisites

| Tool | Version |
|------|---------|
| Node.js | ≥ 18 |
| npm | ≥ 9 |

---

## Setup

```bash
cd blockchain
npm install
```

---

## Compile Contracts

```bash
npx hardhat compile
```

Artefacts are written to `artifacts/` and `cache/`.

---

## Run Tests

```bash
npx hardhat test
```

---

## Start Local Blockchain Node

Open a **dedicated terminal** and run:

```bash
npx hardhat node
```

This starts an EVM-compatible local blockchain on `http://127.0.0.1:8545` (Chain ID 1337) with 20 funded test accounts.  
**Keep this terminal running** while the backend and frontend are active.

---

## Deploy Contract

With the Hardhat node running, open a **second terminal**:

```bash
npx hardhat run scripts/deploy.js --network localhost
```

The deploy script:
1. Deploys `RetailAuditRegistry`
2. Saves `deployments/localhost.json` (address + ABI)
3. Copies `contract-config.json` and the ABI to `../backend/src/main/resources/blockchain/`
4. Copies `contract-config.json` and the ABI to `../frontend/src/assets/blockchain/`

---

## Environment Variables

Copy `.env.example` to `.env`:

```bash
copy .env.example .env
```

The default `PRIVATE_KEY` is Hardhat Account #0 — valid only for local development.

---

## Order Status Transitions (State Machine)

```
PENDING ──► CONFIRMED ──► PROCESSING ──► SHIPPED ──► DELIVERED
   │              │               │
   └──────────────┴───────────────┴────────────────► CANCELLED
```

- `DELIVERED` and `CANCELLED` are **terminal** — no further transitions allowed.
- Any out-of-order transition reverts with `InvalidStatusTransition`.

---

## Ports

| Service | Port |
|---------|------|
| Hardhat local node | 8545 |
