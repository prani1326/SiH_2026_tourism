/**
 * Sanitization utilities for input/output security.
 */

/**
 * Strips potentially dangerous HTML tags and characters to mitigate XSS in text fields.
 * Preserves normal text, punctuation, and Unicode characters.
 */
function sanitizeText(input) {
  if (typeof input !== 'string') return input;
  return input
    .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
    .replace(/<iframe\b[^<]*(?:(?!<\/iframe>)<[^<]*)*<\/iframe>/gi, '')
    .replace(/<object\b[^<]*(?:(?!<\/object>)<[^<]*)*<\/object>/gi, '')
    .replace(/<embed\b[^<]*(?:(?!<\/embed>)<[^<]*)*<\/embed>/gi, '')
    .replace(/javascript:/gi, '')
    .replace(/on\w+\s*=/gi, '')
    .trim();
}

/**
 * Escapes values for CSV exports to prevent CSV Formula Injection (CWE-1236).
 * Prepends a single quote `'` if the cell begins with dangerous formula triggers (=, +, -, @, \t, \r).
 */
function sanitizeCsvValue(val) {
  if (val === null || val === undefined) return '';
  const str = String(val);
  const dangerousChars = ['=', '+', '-', '@', '\t', '\r'];
  if (dangerousChars.some((char) => str.startsWith(char))) {
    return `'${str}`;
  }
  return str;
}

module.exports = {
  sanitizeText,
  sanitizeCsvValue,
};
