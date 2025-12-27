// Quick script to upload puzzle to Firestore
// Run with: node upload_puzzle.js

const admin = require('firebase-admin');
const fs = require('fs');

// Initialize Firebase Admin
const serviceAccount = require('./serviceAccountKey.json'); // You'll need to download this from Firebase

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

// Read puzzle data
const puzzleData = JSON.parse(fs.readFileSync('./sample_puzzle_firestore.json', 'utf8'));

// Upload to Firestore
async function uploadPuzzle() {
  try {
    await db.collection('puzzles').doc(puzzleData.puzzleId).set(puzzleData);
    console.log('✅ Puzzle uploaded successfully!');
    console.log('Document ID:', puzzleData.puzzleId);
  } catch (error) {
    console.error('❌ Error uploading puzzle:', error);
  }
  process.exit();
}

uploadPuzzle();


