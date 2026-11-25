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

test("Unauthenticated user: denied across all user subcollections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      title: "Kopi",
      amount: 25000,
      type: "EXPENSE",
      category: "Makanan",
      occurredAtEpochMs: Date.now() - 5000,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("savingsGoals").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("savingsLogs").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("budgets").get());
});

test("Cross-user isolation: Alice cannot read or write Bob's data", async () => {
  const past = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(BOB_UID).collection("transactions").doc("tx_bob").set({
      userId: BOB_UID,
      title: "Makan Siang",
      amount: 45000,
      type: "EXPENSE",
      category: "Makanan",
      occurredAtEpochMs: Date.now() - 10000,
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("users").doc(BOB_UID).collection("savingsGoals").doc("goal_bob").set({
      userId: BOB_UID,
      name: "Dana Darurat",
      targetAmount: 10000000,
      currentAmount: 2500000,
      iconKey: "emergency",
      colorHex: "#0F5243",
      createdAt: past,
      updatedAt: past,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("transactions").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("transactions").doc("tx_bob").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("transactions").doc("tx_bob").update({ title: "Ubah" }));
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("savingsGoals").doc("goal_bob").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("budgets").get());
});

test("Transactions: Kotlin-aligned create, partial update, delete, and strict bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const txCol = aliceDb.collection("users").doc(ALICE_UID).collection("transactions");

  // Valid create (no @DocumentId `id` field, optional `note` omitted)
  await assertSucceeds(
    txCol.doc("tx_1").set({
      userId: ALICE_UID,
      title: "Belanja Harian",
      amount: 120000,
      type: "EXPENSE",
      category: "Belanja",
      occurredAtEpochMs: Date.now() - 5000,
      createdAt: created,
      updatedAt: created,
    })
  );

  // List query matching Kotlin repository
  await assertSucceeds(txCol.get());

  // Valid partial update with optional `note`
  await assertSucceeds(
    txCol.doc("tx_1").update({
      title: "Belanja Mingguan",
      amount: 150000,
      note: "Sayur dan buah",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject `@DocumentId` key `id` on create
  await assertFails(
    txCol.doc("tx_bad_id").set({
      id: "tx_bad_id",
      userId: ALICE_UID,
      title: "Extra ID",
      amount: 10000,
      type: "EXPENSE",
      category: "Lainnya",
      occurredAtEpochMs: Date.now() - 1000,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject immutable createdAt modification on update
  await assertFails(
    txCol.doc("tx_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject negative/zero amount and ghost fields
  await assertFails(
    txCol.doc("tx_zero").set({
      userId: ALICE_UID,
      title: "Invalid",
      amount: 0,
      type: "EXPENSE",
      category: "Makanan",
      occurredAtEpochMs: Date.now() - 1000,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    txCol.doc("tx_1").update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject future timestamp
  await assertFails(
    txCol.doc("tx_future").set({
      userId: ALICE_UID,
      title: "Future",
      amount: 5000,
      type: "EXPENSE",
      category: "Makanan",
      occurredAtEpochMs: Date.now() - 1000,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Delete succeeds for owner
  await assertSucceeds(txCol.doc("tx_1").delete());
});

test("SavingsGoals & SavingsLogs: atomic deposit/withdraw, parent existence, and balance invariants", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const goalsCol = aliceDb.collection("users").doc(ALICE_UID).collection("savingsGoals");
  const logsCol = aliceDb.collection("users").doc(ALICE_UID).collection("savingsLogs");

  // Create savings goal
  await assertSucceeds(
    goalsCol.doc("goal_1").set({
      userId: ALICE_UID,
      name: "Dana Darurat",
      targetAmount: 10000000,
      currentAmount: 1000000,
      iconKey: "emergency",
      colorHex: "#0F5243",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid metadata edit
  await assertSucceeds(
    goalsCol.doc("goal_1").update({
      name: "Dana Darurat Utama",
      targetAmount: 12000000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Valid balance update + log creation (mirrors WriteBatch in Kotlin)
  await assertSucceeds(
    goalsCol.doc("goal_1").update({
      currentAmount: 1500000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    logsCol.doc("log_1").set({
      userId: ALICE_UID,
      goalId: "goal_1",
      goalName: "Dana Darurat Utama",
      amount: 500000,
      actionType: "DEPOSIT",
      note: "Sisihkan gaji",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Reject negative balance on goal
  await assertFails(
    goalsCol.doc("goal_1").update({
      currentAmount: -100,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject orphaned savings log without existing goal
  await assertFails(
    logsCol.doc("log_orphan").set({
      userId: ALICE_UID,
      goalId: "missing_goal",
      goalName: "Hantu",
      amount: 100000,
      actionType: "DEPOSIT",
      createdAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable savingsLog update
  await assertFails(
    logsCol.doc("log_1").update({
      amount: 999999,
    })
  );
});

test("MonthlyBudgets: valid create, limit update, and immutable month/category guard", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const budgetsCol = aliceDb.collection("users").doc(ALICE_UID).collection("budgets");

  await assertSucceeds(
    budgetsCol.doc("budget_makanan_2026_11").set({
      userId: ALICE_UID,
      category: "Makanan",
      limitAmount: 1500000,
      monthKey: "2026-11",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    budgetsCol.doc("budget_makanan_2026_11").update({
      limitAmount: 2000000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject changing monthKey or category in update
  await assertFails(
    budgetsCol.doc("budget_makanan_2026_11").update({
      monthKey: "2026-12",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject invalid monthKey format
  await assertFails(
    budgetsCol.doc("budget_bad").set({
      userId: ALICE_UID,
      category: "Hiburan",
      limitAmount: 500000,
      monthKey: "2026/11",
      createdAt: created,
      updatedAt: created,
    })
  );
});
