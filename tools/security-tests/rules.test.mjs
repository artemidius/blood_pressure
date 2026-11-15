import { after, before, beforeEach, test } from 'node:test';
import { readFileSync } from 'node:fs';
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc, deleteDoc, collection, getDocs } from 'firebase/firestore';

let env;
const reading = { systolic: 120, diastolic: 80, timestamp: 1704067200000 };
before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-blood-pressure',
    firestore: { host: '127.0.0.1', port: 8089, rules: readFileSync(new URL('../../firestore.rules', import.meta.url), 'utf8') }
  });
});
beforeEach(async () => { await env.clearFirestore(); });
after(async () => { await env?.cleanup(); });

test('owner can create, read, list, update, and delete readings', async () => {
  const db = env.authenticatedContext('alice').firestore();
  const ref = doc(db, 'blood_pressure/alice/data/one');
  await assertSucceeds(setDoc(ref, reading));
  await assertSucceeds(getDoc(ref));
  await assertSucceeds(getDocs(collection(db, 'blood_pressure/alice/data')));
  await assertSucceeds(setDoc(ref, { ...reading, systolic: 130 }));
  await assertSucceeds(deleteDoc(ref));
});
test('unauthenticated access is denied', async () => {
  const ref = doc(env.unauthenticatedContext().firestore(), 'blood_pressure/alice/data/one');
  await assertFails(getDoc(ref));
  await assertFails(setDoc(ref, reading));
});
test('another account cannot read, write, list, or delete owner records', async () => {
  const db = env.authenticatedContext('bob').firestore();
  const ref = doc(db, 'blood_pressure/alice/data/one');
  await assertFails(getDoc(ref));
  await assertFails(getDocs(collection(db, 'blood_pressure/alice/data')));
  await assertFails(setDoc(ref, reading));
  await assertFails(deleteDoc(ref));
});
test('malformed measurements and extra fields are rejected', async () => {
  const ref = doc(env.authenticatedContext('alice').firestore(), 'blood_pressure/alice/data/one');
  for (const invalid of [
    {...reading, systolic: 0}, {...reading, diastolic: 301}, {...reading, systolic: 70},
    {...reading, systolic: '120'}, {...reading, timestamp: -1}, {...reading, extra: true}
  ]) await assertFails(setDoc(ref, invalid));
});
test('unrelated collections are denied', async () => {
  const ref = doc(env.authenticatedContext('alice').firestore(), 'other/one');
  await assertFails(getDoc(ref));
  await assertFails(setDoc(ref, reading));
});
