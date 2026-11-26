const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_ub_123";
const BOB_UID = "bob_biust_456";
const CHARLIE_UID = "charlie_bac_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

async function seedVerifiedStudent(uid, uni, domain, name) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const ts = pastDate(10000);
    await db.collection("users").doc(uid).set({
      userId: uid,
      googleEmail: `${uid}@gmail.com`,
      studentEmail: `${uid}@${domain}`,
      verifiedDomain: domain,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("studentProfiles").doc(uid).set({
      userId: uid,
      displayName: name,
      university: uni,
      faculty: "Computer Science",
      yearOfStudy: 3,
      bio: "Botswana student",
      verifiedDomain: domain,
      acBwVerified: true,
      discoverable: true,
      createdAt: ts,
      updatedAt: ts,
    });
  });
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

// --- 1. UNAUTHENTICATED & PII ISOLATION TESTS ---

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("studentProfiles").where("discoverable", "==", true).get());
  await assertFails(unauthDb.collection("posts").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("studyGroups").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("channels").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("listings").where("listingStatus", "==", "available").get());
  await assertFails(unauthDb.collection("events").where("visibility", "==", "public").get());
});

test("Unregistered signed-in user can read own non-existent studentProfile without PERMISSION_DENIED", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Newly signed-in user observes own missing profile -> succeeds with exists == false
  const mySnap = await assertSucceeds(aliceDb.collection("studentProfiles").doc(ALICE_UID).get());
  if (mySnap.exists) {
    throw new Error("Expected unregistered studentProfile to not exist yet");
  }

  // Other users cannot read a non-existent profile
  await assertFails(bobDb.collection("studentProfiles").doc(ALICE_UID).get());

  // Checking own member channels before creating a DM works via array-contains query
  await assertSucceeds(aliceDb.collection("channels").where("memberUids", "array-contains", ALICE_UID).get());
});

test("Registration enforces .ac.bw email domain & isolates PII in /users", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate();

  // Reject non-.ac.bw email
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      googleEmail: "alice@gmail.com",
      studentEmail: "alice@gmail.com",
      verifiedDomain: "gmail.com",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject .co.bw email (must be .ac.bw)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      googleEmail: "alice@gmail.com",
      studentEmail: "alice@ub.co.bw",
      verifiedDomain: "ub.co.bw",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject profile without atomic /users/{uid} .ac.bw doc
  await assertFails(
    aliceDb.collection("studentProfiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Modise",
      university: "UB",
      faculty: "Computing",
      yearOfStudy: 2,
      bio: "",
      verifiedDomain: "ub.ac.bw",
      acBwVerified: true,
      discoverable: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Atomic batch registration with valid .ac.bw succeeds
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("users").doc(ALICE_UID), {
    userId: ALICE_UID,
    googleEmail: "alice@gmail.com",
    studentEmail: "202201122@ub.ac.bw",
    verifiedDomain: "ub.ac.bw",
    createdAt: ts,
    updatedAt: ts,
  });
  batch.set(aliceDb.collection("studentProfiles").doc(ALICE_UID), {
    userId: ALICE_UID,
    displayName: "Alice Modise",
    university: "UB",
    faculty: "Computer Science",
    yearOfStudy: 2,
    bio: "CSI241 student",
    verifiedDomain: "ub.ac.bw",
    acBwVerified: true,
    discoverable: true,
    createdAt: ts,
    updatedAt: ts,
  });
  await assertSucceeds(batch.commit());

  // Bob CANNOT read Alice's private /users/{uid} PII or list /users
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());

  // Bob CAN query discoverable studentProfiles
  await assertSucceeds(bobDb.collection("studentProfiles").where("discoverable", "==", true).get());
  // Unfiltered studentProfiles list fails
  await assertFails(bobDb.collection("studentProfiles").get());
});

// --- 2. STUDY GROUPS & JOIN REQUEST WORKFLOW TESTS ---

test("Study group creation, join request, and creator approval update group & linked channel", async () => {
  await seedVerifiedStudent(ALICE_UID, "UB", "ub.ac.bw", "Alice");
  await seedVerifiedStudent(BOB_UID, "UB", "ub.ac.bw", "Bob");

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate();

  // Alice creates StudyGroup + linked private ChatChannel atomically
  const createBatch = aliceDb.batch();
  createBatch.set(aliceDb.collection("studyGroups").doc("grp_1"), {
    creatorId: ALICE_UID,
    creatorName: "Alice",
    creatorUniversity: "UB",
    university: "UB",
    courseCode: "CSI241",
    studyTopic: "Trees and Graph Algorithms",
    description: "Revising for test 2",
    meetingSchedule: "Thu 17:00 UB Library",
    maxMembers: 6,
    memberUids: [ALICE_UID],
    pendingRequestUids: [],
    visibility: "public",
    groupStatus: "open",
    linkedChannelId: "grp_1",
    createdAt: ts,
    updatedAt: ts,
  });
  createBatch.set(aliceDb.collection("channels").doc("grp_1"), {
    name: "CSI241 • Trees and Graph Algorithms",
    description: "UB study group chat",
    university: "UB",
    courseCode: "CSI241",
    channelType: "STUDY_GROUP",
    visibility: "direct",
    creatorId: ALICE_UID,
    memberUids: [ALICE_UID],
    lastMessageText: "Study group room created",
    lastMessageSender: "Alice",
    createdAt: ts,
    updatedAt: ts,
  });
  await assertSucceeds(createBatch.commit());

  // Bob lists public study groups
  await assertSucceeds(bobDb.collection("studyGroups").where("visibility", "==", "public").get());
  await assertFails(bobDb.collection("studyGroups").get());

  // Bob cannot directly read private linked channel before approval
  await assertFails(bobDb.collection("channels").doc("grp_1").get());

  // Bob requests to join atomically
  const reqBatch = bobDb.batch();
  reqBatch.set(bobDb.collection("studyGroups").doc("grp_1").collection("joinRequests").doc(BOB_UID), {
    groupId: "grp_1",
    groupCreatorId: ALICE_UID,
    requesterId: BOB_UID,
    requesterName: "Bob",
    requesterUniversity: "UB",
    requesterFaculty: "Computer Science",
    note: "Need help with Dijkstra",
    requestStatus: "pending",
    createdAt: ts,
    updatedAt: ts,
  });
  reqBatch.update(bobDb.collection("studyGroups").doc("grp_1"), {
    pendingRequestUids: [BOB_UID],
    updatedAt: pastDate(500),
  });
  await assertSucceeds(reqBatch.commit());

  // Charlie cannot approve Bob's request
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(
    charlieDb.collection("studyGroups").doc("grp_1").collection("joinRequests").doc(BOB_UID).update({
      requestStatus: "accepted",
      updatedAt: pastDate(300),
    })
  );

  // Alice views join requests with matching query
  await assertSucceeds(
    aliceDb.collection("studyGroups").doc("grp_1").collection("joinRequests")
      .where("groupCreatorId", "==", ALICE_UID)
      .get()
  );

  // Alice approves Bob atomically (request -> accepted, group memberUids, channel memberUids)
  const approveBatch = aliceDb.batch();
  approveBatch.update(aliceDb.collection("studyGroups").doc("grp_1").collection("joinRequests").doc(BOB_UID), {
    requestStatus: "accepted",
    updatedAt: pastDate(200),
  });
  approveBatch.update(aliceDb.collection("studyGroups").doc("grp_1"), {
    memberUids: [ALICE_UID, BOB_UID],
    pendingRequestUids: [],
    groupStatus: "open",
    updatedAt: pastDate(200),
  });
  approveBatch.update(aliceDb.collection("channels").doc("grp_1"), {
    memberUids: [ALICE_UID, BOB_UID],
    updatedAt: pastDate(200),
  });
  await assertSucceeds(approveBatch.commit());

  // Now Bob CAN read and send messages in the linked study group channel!
  await assertSucceeds(bobDb.collection("channels").doc("grp_1").get());
  await assertSucceeds(
    bobDb.collection("channels").doc("grp_1").collection("messages").doc("msg_1").set({
      channelId: "grp_1",
      senderId: BOB_UID,
      senderName: "Bob",
      senderUniversity: "UB",
      text: "Thanks for adding me!",
      visibility: "direct",
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: pastDate(100),
    })
  );
  await assertSucceeds(
    bobDb.collection("channels").doc("grp_1").collection("messages")
      .where("memberUids", "array-contains", BOB_UID)
      .get()
  );
});

// --- 3. CAMPUS FEED, CHANNELS, MARKETPLACE & EVENTS TESTS ---

test("Campus posts, likes, comments, marketplace terminal state, and events RSVP enforce parity", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(5000);

  // Post create + like + atomic comment
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      authorUniversity: "UB",
      category: "ANNOUNCEMENT",
      title: "BIUST Hackathon",
      body: "Sign up this Friday",
      visibility: "public",
      likedByUids: [],
      commentCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").update({
      likedByUids: [BOB_UID],
      updatedAt: pastDate(1000),
    })
  );
  // Bob cannot spoof Alice's like
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      likedByUids: [BOB_UID, ALICE_UID],
      updatedAt: pastDate(900),
    })
  );

  // Marketplace: Alice creates item, Bob cannot mark sold, Alice marks sold, then locked
  await assertSucceeds(
    aliceDb.collection("listings").doc("item_1").set({
      sellerId: ALICE_UID,
      sellerName: "Alice",
      sellerUniversity: "UB",
      title: "Engineering Maths Textbook",
      description: "Clean condition",
      category: "TEXTBOOKS",
      priceBwp: 250,
      condition: "GOOD",
      pickupSpot: "UB Student Centre",
      listingStatus: "available",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    bobDb.collection("listings").doc("item_1").update({
      listingStatus: "sold",
      updatedAt: pastDate(500),
    })
  );
  await assertSucceeds(
    aliceDb.collection("listings").doc("item_1").update({
      listingStatus: "sold",
      updatedAt: pastDate(400),
    })
  );
  // Terminal state locking: cannot modify after sold
  await assertFails(
    aliceDb.collection("listings").doc("item_1").update({
      listingStatus: "available",
      updatedAt: pastDate(200),
    })
  );

  // Events: Alice creates event, Bob RSVPs
  await assertSucceeds(
    aliceDb.collection("events").doc("evt_1").set({
      organizerId: ALICE_UID,
      organizerName: "Alice",
      university: "UB",
      title: "GDSC DevFest Gaborone",
      description: "Kotlin & Cloud workshops",
      venue: "Block 247 Auditorium",
      eventDateLabel: "Sat, 28 Nov 2026 • 10:00",
      eventDateIso: "2026-11-28",
      category: "CLUBS",
      visibility: "public",
      attendeeUids: [ALICE_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("events").doc("evt_1").update({
      attendeeUids: [ALICE_UID, BOB_UID],
      updatedAt: pastDate(200),
    })
  );
  // Future timestamp rejected
  await assertFails(
    bobDb.collection("events").doc("evt_1").update({
      attendeeUids: [ALICE_UID],
      updatedAt: new Date(Date.now() + 120000),
    })
  );
});

// --- 4. REAL-TIME USER PRESENCE TESTS ---

test("User presence allows owner updates, public list query, missing-doc get, and denies spoofing", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(2000);

  // Unauthenticated cannot read or write presence
  await assertFails(unauthDb.collection("userPresence").where("visibility", "==", "public").get());
  await assertFails(
    unauthDb.collection("userPresence").doc(ALICE_UID).set({
      userId: ALICE_UID,
      status: "online",
      visibility: "public",
      lastActiveAt: ts,
      updatedAt: ts,
    })
  );

  // Signed-in user can read non-existent peer presence without PERMISSION_DENIED
  const missingSnap = await assertSucceeds(bobDb.collection("userPresence").doc(ALICE_UID).get());
  if (missingSnap.exists) {
    throw new Error("Expected missing presence doc to not exist");
  }

  // Alice writes initial online presence via .set()
  await assertSucceeds(
    aliceDb.collection("userPresence").doc(ALICE_UID).set({
      userId: ALICE_UID,
      status: "online",
      visibility: "public",
      lastActiveAt: ts,
      updatedAt: ts,
    })
  );

  // Alice updates status to away and then offline via .set() (matches repository write)
  await assertSucceeds(
    aliceDb.collection("userPresence").doc(ALICE_UID).set({
      userId: ALICE_UID,
      status: "away",
      visibility: "public",
      lastActiveAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("userPresence").doc(ALICE_UID).set({
      userId: ALICE_UID,
      status: "offline",
      visibility: "public",
      lastActiveAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Bob can read Alice's presence and run filtered list query, but NOT unfiltered list
  await assertSucceeds(bobDb.collection("userPresence").doc(ALICE_UID).get());
  await assertSucceeds(bobDb.collection("userPresence").where("visibility", "==", "public").get());
  await assertFails(bobDb.collection("userPresence").get());

  // Bob CANNOT overwrite Alice's presence
  await assertFails(
    bobDb.collection("userPresence").doc(ALICE_UID).set({
      userId: ALICE_UID,
      status: "offline",
      visibility: "public",
      lastActiveAt: pastDate(300),
      updatedAt: pastDate(300),
    })
  );

  // Invalid status or future timestamp rejected
  await assertFails(
    aliceDb.collection("userPresence").doc(ALICE_UID).set({
      userId: ALICE_UID,
      status: "busy",
      visibility: "public",
      lastActiveAt: pastDate(200),
      updatedAt: pastDate(200),
    })
  );
  await assertFails(
    aliceDb.collection("userPresence").doc(ALICE_UID).set({
      userId: ALICE_UID,
      status: "online",
      visibility: "public",
      lastActiveAt: new Date(Date.now() + 120000),
      updatedAt: pastDate(200),
    })
  );
});

// --- 5. FCM DEVICE TOKENS & REAL-TIME NOTIFICATIONS TESTS ---

test("FCM tokens are strictly private and notifications enforce recipient/sender isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(1500);

  // Alice saves her FCM token under /users/{uid}/fcmTokens/{tokenId}
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("fcmTokens").doc("tok_abc123").set({
      userId: ALICE_UID,
      token: "fcm_sample_token_1234567890",
      platform: "android",
      updatedAt: ts,
    })
  );

  // Bob CANNOT read or write Alice's FCM tokens
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("fcmTokens").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("fcmTokens").doc("tok_hack").set({
      userId: BOB_UID,
      token: "fcm_hack_token_1234567890",
      platform: "android",
      updatedAt: ts,
    })
  );

  // Bob creates a POST_REPLY notification for Alice
  await assertSucceeds(
    bobDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1").set({
      recipientId: ALICE_UID,
      senderId: BOB_UID,
      senderName: "Bob",
      senderUniversity: "BIUST",
      notifType: "POST_REPLY",
      title: "Bob replied to your post",
      body: "Meet at UB Library at 16:00!",
      targetId: "post_1",
      read: false,
      createdAt: ts,
    })
  );

  // Bob CANNOT read Alice's notifications or spoof a third user's senderId
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("notifications").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_spoof").set({
      recipientId: ALICE_UID,
      senderId: CHARLIE_UID,
      senderName: "Charlie",
      senderUniversity: "BAC",
      notifType: "CHAT_MESSAGE",
      title: "Spoofed",
      body: "Fake sender",
      targetId: "room_1",
      read: false,
      createdAt: ts,
    })
  );

  // Alice CANNOT send a self-notification, CAN read and mark her notifications read
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_self").set({
      recipientId: ALICE_UID,
      senderId: ALICE_UID,
      senderName: "Alice",
      senderUniversity: "UB",
      notifType: "POST_REPLY",
      title: "Self",
      body: "Self test",
      targetId: "post_1",
      read: false,
      createdAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("notifications").get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1").update({
      read: true,
    })
  );
});

// --- 6. REPORT POST & ADMIN MODERATION NOTIFICATION TESTS ---

test("Reporting a post atomically creates postReport & adminNotification; enforces strict admin-only isolation", async () => {
  const ADMIN_UID = "admin_mod_001";
  const ts = pastDate(3000);

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      role: "admin",
    });
    await db.collection("posts").doc("post_flag_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      authorUniversity: "UB",
      category: "CAMPUS_LIFE",
      title: "Suspicious link promo",
      body: "Click here for free cash",
      visibility: "public",
      likedByUids: [],
      commentCount: 0,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  const reportId = `post_flag_1_${BOB_UID}`;
  const adminNotifId = `report_${reportId}`;

  // Bob CANNOT escalate himself to admin
  await assertFails(
    bobDb.collection("admins").doc(BOB_UID).set({ userId: BOB_UID, role: "admin" })
  );

  // Regular student can check own admin status cleanly (exists == false)
  const bobAdminCheck = await assertSucceeds(bobDb.collection("admins").doc(BOB_UID).get());
  if (bobAdminCheck.exists) {
    throw new Error("Non-admin should not have an admin role doc");
  }

  // Alice CANNOT report her own post
  const selfBatch = aliceDb.batch();
  selfBatch.set(aliceDb.collection("postReports").doc(`post_flag_1_${ALICE_UID}`), {
    postId: "post_flag_1",
    postAuthorId: ALICE_UID,
    postAuthorName: "Alice",
    postTitle: "Suspicious link promo",
    postExcerpt: "Click here for free cash",
    reporterId: ALICE_UID,
    reporterName: "Alice",
    reporterUniversity: "UB",
    reason: "SPAM",
    details: "Self test",
    status: "pending",
    createdAt: ts,
    updatedAt: ts,
  });
  await assertFails(selfBatch.commit());

  // Bob CANNOT create an orphan adminNotification without a backing postReport
  await assertFails(
    bobDb.collection("adminNotifications").doc("orphan_notif").set({
      reportId: "nonexistent_report",
      postId: "post_flag_1",
      postAuthorId: ALICE_UID,
      postTitle: "Suspicious link promo",
      reporterId: BOB_UID,
      reporterName: "Bob",
      reporterUniversity: "BIUST",
      reason: "SPAM",
      details: "Looks like phishing",
      title: "Post reported: Spam or Scam",
      body: "Bob (BIUST) reported Alice's post",
      read: false,
      createdAt: ts,
    })
  );

  // Bob submits an atomic batch creating postReport + adminNotification
  const reportBatch = bobDb.batch();
  reportBatch.set(bobDb.collection("postReports").doc(reportId), {
    postId: "post_flag_1",
    postAuthorId: ALICE_UID,
    postAuthorName: "Alice",
    postTitle: "Suspicious link promo",
    postExcerpt: "Click here for free cash",
    reporterId: BOB_UID,
    reporterName: "Bob",
    reporterUniversity: "BIUST",
    reason: "SPAM",
    details: "Looks like a scam link",
    status: "pending",
    createdAt: ts,
    updatedAt: ts,
  });
  reportBatch.set(bobDb.collection("adminNotifications").doc(adminNotifId), {
    reportId,
    postId: "post_flag_1",
    postAuthorId: ALICE_UID,
    postTitle: "Suspicious link promo",
    reporterId: BOB_UID,
    reporterName: "Bob",
    reporterUniversity: "BIUST",
    reason: "SPAM",
    details: "Looks like a scam link",
    title: "Post reported: Spam or Scam",
    body: "Bob (BIUST) reported Alice's post \"Suspicious link promo\"",
    read: false,
    createdAt: ts,
  });
  await assertSucceeds(reportBatch.commit());

  // Bob CAN query his own submitted reports, CANNOT query all reports or adminNotifications
  await assertSucceeds(bobDb.collection("postReports").where("reporterId", "==", BOB_UID).get());
  await assertFails(bobDb.collection("postReports").get());
  await assertFails(bobDb.collection("adminNotifications").get());
  await assertFails(charlieDb.collection("postReports").doc(reportId).get());
  await assertFails(aliceDb.collection("adminNotifications").doc(adminNotifId).get());

  // Non-admin CANNOT mark report reviewed or mark adminNotification read
  await assertFails(
    bobDb.collection("postReports").doc(reportId).update({
      status: "reviewed",
      updatedAt: pastDate(500),
    })
  );

  // Admin CAN list adminNotifications, list all postReports, mark alert read, resolve report, and remove flagged post
  await assertSucceeds(adminDb.collection("adminNotifications").get());
  await assertSucceeds(adminDb.collection("postReports").get());
  await assertSucceeds(
    adminDb.collection("adminNotifications").doc(adminNotifId).update({
      read: true,
    })
  );
  await assertSucceeds(
    adminDb.collection("postReports").doc(reportId).update({
      status: "reviewed",
      updatedAt: pastDate(200),
    })
  );
  await assertSucceeds(adminDb.collection("posts").doc("post_flag_1").delete());
});


