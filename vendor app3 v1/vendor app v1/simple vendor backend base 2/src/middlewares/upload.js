const multer = require('multer');
const path = require('path');
const fs = require('fs');
const config = require('../config/env');

// Ensure upload folders exist
const profileDir = path.join(config.UPLOAD_PATH, 'profiles');
const docDir = path.join(config.UPLOAD_PATH, 'documents');

if (!fs.existsSync(profileDir)) fs.mkdirSync(profileDir, { recursive: true });
if (!fs.existsSync(docDir)) fs.mkdirSync(docDir, { recursive: true });

// Storage config for Profile Photos
const profileStorage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, profileDir);
  },
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1e9);
    const ext = path.extname(file.originalname).toLowerCase();
    cb(null, `vendor-${req.vendorId || 'profile'}-${uniqueSuffix}${ext}`);
  },
});

// Storage config for KYC Documents
const docStorage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, docDir);
  },
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1e9);
    const ext = path.extname(file.originalname).toLowerCase();
    cb(null, `doc-${req.vendorId || 'kyc'}-${uniqueSuffix}${ext}`);
  },
});

// File filter for images (Profile photo)
const imageFileFilter = (req, file, cb) => {
  const allowedTypes = /jpeg|jpg|png|webp/;
  const ext = allowedTypes.test(path.extname(file.originalname).toLowerCase());
  const mime = allowedTypes.test(file.mimetype);

  if (ext && mime) {
    cb(null, true);
  } else {
    cb(new Error('Invalid file type. Only JPEG, PNG, and WebP images are allowed for profile photo.'), false);
  }
};

// File filter for KYC documents (Images and PDFs)
const docFileFilter = (req, file, cb) => {
  const allowedExts = /jpeg|jpg|png|webp|pdf/;
  const ext = allowedExts.test(path.extname(file.originalname).toLowerCase());
  const allowedMimes = ['image/jpeg', 'image/png', 'image/webp', 'image/jpg', 'application/pdf'];
  const mime = allowedMimes.includes(file.mimetype);

  if (ext && mime) {
    cb(null, true);
  } else {
    cb(new Error('Invalid document format. Only PDF, JPEG, PNG, and WebP files are allowed.'), false);
  }
};

const uploadProfilePhoto = multer({
  storage: profileStorage,
  limits: { fileSize: 5 * 1024 * 1024 }, // 5 MB
  fileFilter: imageFileFilter,
});

const uploadKycDocument = multer({
  storage: docStorage,
  limits: { fileSize: config.MAX_FILE_SIZE_MB * 1024 * 1024 }, // 10 MB
  fileFilter: docFileFilter,
});

module.exports = {
  uploadProfilePhoto,
  uploadKycDocument,
};
