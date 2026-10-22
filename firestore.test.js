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

// --- SECURITY BOUNDS TESTS ---

test("Unauthenticated user: cannot read users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
});

test("Authenticated user: Alice cannot write Bob's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).set({
    userId: BOB_UID,
    displayName: "Bob",
    username: "bob",
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("Authenticated user: Alice can create her own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    displayName: "Alice",
    username: "alice",
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("Authenticated user: Alice can create a group and Bob who is member can read it", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("groups").doc("group_1").set({
    id: "group_1",
    name: "Friends Group",
    creatorId: ALICE_UID,
    adminIds: [ALICE_UID],
    memberIds: [ALICE_UID, BOB_UID],
    createdAt: new Date(),
    updatedAt: new Date()
  }));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("groups").doc("group_1").get());
});

test("Non-sender cannot delete someone else's message", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("messages").doc("msg_1").set({
      id: "msg_1",
      chatId: "group_1",
      senderId: ALICE_UID,
      senderName: "Alice",
      recipientIds: [ALICE_UID, BOB_UID],
      text: "Hello everyone",
      seenBy: [ALICE_UID],
      createdAt: new Date()
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("messages").doc("msg_1").delete());

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("messages").doc("msg_1").delete());
});
