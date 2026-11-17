const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, now) {
  return {
    userId: uid,
    displayName: "Amani Ochieng",
    phoneNumber: "0712 345 678",
    planName: "SafaOne PrePay 5G",
    airtimeBalance: 245.5,
    mpesaBalance: 12450.0,
    savingsBalance: 3500.0,
    fulizaLimit: 5000.0,
    fulizaUsed: 0.0,
    dataBalanceMb: 4250,
    minutesBalance: 145,
    smsBalance: 420,
    bongaPoints: 1280,
    fibrePlanName: "Gold 60 Mbps",
    fibreMonthlyFee: 6299.0,
    fibreActive: true,
    fibreExpiryLabel: "Renews Dec 15",
    hideBalances: false,
    createdAt: now,
    updatedAt: now,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write profiles, transactions, bundles, or payees", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, now))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("active_bundles").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("saved_payees").get()
  );
});

test("AccountProfile: owner create, partial update, and security guards", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Valid create
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created))
  );

  // Valid partial update (privacy toggle + balance update)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      mpesaBalance: 11950.0,
      bongaPoints: 1290,
      hideBalances: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field ("shadow update")
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp spoofing
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      mpesaBalance: 99999.0,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Reject negative balance
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      mpesaBalance: -10.0,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user isolation: Bob cannot read or update Alice's profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Listing /users is forbidden
  await assertFails(aliceDb.collection("users").get());
});

test("Transactions & ActiveBundles: owner create and query succeed; immutable & isolated", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  const txRef = aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1");
  await assertSucceeds(
    txRef.set({
      userId: ALICE_UID,
      receiptCode: "UKG7X92M1P",
      type: "SEND_MONEY",
      title: "Sent to Grace Wanjiku",
      counterparty: "0722 890 123",
      amount: 500.0,
      fee: 7.0,
      direction: "DEBIT",
      source: "MPESA",
      balanceAfter: 11943.0,
      note: "Lunch",
      createdAt: now,
    })
  );

  // Optional note omitted
  const txRef2 = aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_2");
  await assertSucceeds(
    txRef2.set({
      userId: ALICE_UID,
      receiptCode: "UKG7X92M2Q",
      type: "BUY_BUNDLE",
      title: "Tunukiwa 2GB Daily",
      counterparty: "SafaOne Bundles",
      amount: 50.0,
      fee: 0.0,
      direction: "DEBIT",
      source: "AIRTIME",
      balanceAfter: 195.5,
      createdAt: now,
    })
  );

  // Reject @DocumentId "id" key inside payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_3").set({
      id: "tx_3",
      userId: ALICE_UID,
      receiptCode: "UKG7X92M3R",
      type: "DEPOSIT",
      title: "Agent Deposit",
      counterparty: "Agent 20411",
      amount: 1000.0,
      fee: 0.0,
      direction: "CREDIT",
      source: "MPESA",
      balanceAfter: 12943.0,
      createdAt: now,
    })
  );

  // Reject updating immutable transaction
  await assertFails(txRef.update({ amount: 1.0 }));

  // Owner list query matches Kotlin repository
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").orderBy("createdAt", "desc").get()
  );

  // Cross-user isolation
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );

  // ActiveBundle create & list
  const bundleRef = aliceDb.collection("users").doc(ALICE_UID).collection("active_bundles").doc("b_1");
  await assertSucceeds(
    bundleRef.set({
      userId: ALICE_UID,
      name: "Tunukiwa 2GB Daily",
      category: "TUNUKIWA",
      dataMb: 2048,
      minutes: 0,
      smsCount: 0,
      validityLabel: "Valid 24 hrs",
      priceKes: 50.0,
      createdAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("active_bundles").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("active_bundles").get()
  );
});

test("SavedPayees: CRUD with optional accountRef and owner isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  const payeeRef = aliceDb.collection("users").doc(ALICE_UID).collection("saved_payees").doc("p_1");
  await assertSucceeds(
    payeeRef.set({
      userId: ALICE_UID,
      name: "Kenya Power Tokens",
      payeeType: "PAYBILL",
      identifier: "888880",
      accountRef: "1420988711",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    payeeRef.update({
      name: "KPLC Prepaid",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("saved_payees").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("saved_payees").get()
  );
  await assertSucceeds(payeeRef.delete());
});
