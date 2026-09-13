import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";

// Firebase configuration for project: webdashboard-d040c
const firebaseConfig = {
  apiKey: "AIzaSyAZl_hz3aXRXWLAzBco8wefyJbH6Tqbr6s",
  authDomain: "webdashboard-d040c.firebaseapp.com",
  projectId: "webdashboard-d040c",
  storageBucket: "webdashboard-d040c.firebasestorage.app",
  messagingSenderId: "735666011006",
  appId: "1:735666011006:web:6f5e7d8002787fba7b912f",
  measurementId: "G-Q6J0JYM4QC"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);

// Initialize Firestore
const db = getFirestore(app);

export { app, db };
