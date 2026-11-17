const { ethers } = require("hardhat");
const fs = require("fs");
const path = require("path");

async function main() {
  console.log("=== Deploying RetailAuditRegistry ===\n");

  const [deployer] = await ethers.getSigners();
  const balance = await ethers.provider.getBalance(deployer.address);

  console.log("Deployer address :", deployer.address);
  console.log("Deployer balance :", ethers.formatEther(balance), "ETH\n");

  const Factory = await ethers.getContractFactory("RetailAuditRegistry");
  const registry = await Factory.deploy();
  await registry.waitForDeployment();

  const contractAddress = await registry.getAddress();
  const deployTx = registry.deploymentTransaction();

  console.log("Contract deployed to  :", contractAddress);
  console.log("Deployment tx hash    :", deployTx.hash);
  console.log("Block number          :", deployTx.blockNumber ?? "(pending)");

  // ── Paths ──────────────────────────────────────────────────────────────────
  const deploymentsDir = path.join(__dirname, "../deployments");
  const backendResDir  = path.join(__dirname, "../../backend/src/main/resources/blockchain");
  const frontendAssDir = path.join(__dirname, "../../frontend/src/assets/blockchain");

  [deploymentsDir, backendResDir, frontendAssDir].forEach(
    (d) => fs.mkdirSync(d, { recursive: true })
  );

  // ── ABI source ────────────────────────────────────────────────────────────
  const artifactPath = path.join(
    __dirname,
    "../artifacts/contracts/RetailAuditRegistry.sol/RetailAuditRegistry.json"
  );
  const artifact = JSON.parse(fs.readFileSync(artifactPath, "utf8"));

  const deploymentInfo = {
    contractName:    "RetailAuditRegistry",
    contractAddress,
    deployerAddress: deployer.address,
    transactionHash: deployTx.hash,
    network:         "localhost",
    chainId:         1337,
    deployedAt:      new Date().toISOString(),
    abi:             artifact.abi
  };

  // ── Save deployment artefacts ──────────────────────────────────────────────
  fs.writeFileSync(
    path.join(deploymentsDir, "localhost.json"),
    JSON.stringify(deploymentInfo, null, 2)
  );
  console.log("\nDeployment info  →", path.join(deploymentsDir, "localhost.json"));

  const contractConfig = {
    contractAddress,
    chainId: 1337,
    rpcUrl:  "http://127.0.0.1:8545"
  };

  // Backend
  fs.writeFileSync(
    path.join(backendResDir, "contract-config.json"),
    JSON.stringify(contractConfig, null, 2)
  );
  fs.copyFileSync(artifactPath, path.join(backendResDir, "RetailAuditRegistry.json"));
  console.log("Backend config   →", backendResDir);

  // Frontend
  fs.writeFileSync(
    path.join(frontendAssDir, "contract-config.json"),
    JSON.stringify(contractConfig, null, 2)
  );
  fs.copyFileSync(artifactPath, path.join(frontendAssDir, "RetailAuditRegistry.json"));
  console.log("Frontend config  →", frontendAssDir);

  console.log("\n✅ Deployment complete!");
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error(err);
    process.exit(1);
  });
