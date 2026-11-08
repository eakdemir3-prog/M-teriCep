// MüşteriCep anlık bildirimleri.
// - Yeni randevu isteği / randevu → işletmeye bildirim
// - İşletme onaylar / reddeder / iptal eder → müşteriye bildirim
// - Müşteri iptal eder → işletmeye bildirim
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const admin = require("firebase-admin");

admin.initializeApp();
setGlobalOptions({ region: "europe-west1", maxInstances: 5 });
const db = admin.firestore();

function when(start) {
  const d = new Date(start);
  const opts = { timeZone: "Europe/Istanbul" };
  const date = d.toLocaleDateString("tr-TR", { ...opts, day: "numeric", month: "long", weekday: "short" });
  const time = d.toLocaleTimeString("tr-TR", { ...opts, hour: "2-digit", minute: "2-digit" });
  return `${date} ${time}`;
}

async function sendTo(uid, title, body, appointmentId) {
  if (!uid) return;
  const snap = await db.collection("users").doc(uid).get();
  const tokens = (snap.get("fcmTokens") || []).filter(Boolean);
  if (tokens.length === 0) return;
  const res = await admin.messaging().sendEachForMulticast({
    tokens,
    data: { title, body, appointmentId: appointmentId || "" },
    android: { priority: "high" },
  });
  // Geçersiz anahtarları temizle.
  const dead = [];
  res.responses.forEach((r, i) => {
    const code = r.error && r.error.code;
    if (code === "messaging/registration-token-not-registered" || code === "messaging/invalid-registration-token") dead.push(tokens[i]);
  });
  if (dead.length) {
    await db.collection("users").doc(uid).update({ fcmTokens: admin.firestore.FieldValue.arrayRemove(...dead) });
  }
}

exports.onAppointmentCreated = onDocumentCreated("appointments/{id}", async (event) => {
  const a = event.data && event.data.data();
  if (!a || !a.customerUid) return; // İşletmenin kendi eklediği randevu: bildirim yok.
  const title = a.status === "PENDING" ? "Yeni randevu isteği" : "Yeni randevu";
  const body = `${a.customerName} • ${a.serviceName} • ${when(a.start)}`;
  await sendTo(a.businessId, title, body, event.params.id);
});

exports.onAppointmentUpdated = onDocumentUpdated("appointments/{id}", async (event) => {
  const before = event.data.before.data();
  const after = event.data.after.data();
  if (!before || !after || before.status === after.status) return;
  const id = event.params.id;
  const w = `${after.serviceName} • ${when(after.start)}`;
  const cancelledByCustomer = after.status === "CANCELLED" && after.cancelledBy && after.cancelledBy === after.customerUid;

  if (cancelledByCustomer) {
    await sendTo(after.businessId, "Randevu iptal edildi", `${after.customerName} • ${w}`, id);
    return;
  }
  if (!after.customerUid) return;
  const map = {
    CONFIRMED: ["Randevunuz onaylandı", `${after.businessName} • ${w}`],
    REJECTED: ["Randevu isteğiniz karşılanamadı", `${after.businessName} • ${w}. Başka bir saat seçebilirsiniz.`],
    CANCELLED: ["Randevunuz iptal edildi", `${after.businessName} • ${w}`],
  };
  const m = map[after.status];
  if (m) await sendTo(after.customerUid, m[0], m[1], id);
});
