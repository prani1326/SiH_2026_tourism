/**
 * Input validation helpers
 */
const isValidEmail = (email) => {
  if (!email || typeof email !== 'string') return false;
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return re.test(email.trim());
};

const isValidMobile = (mobile) => {
  if (!mobile || typeof mobile !== 'string') return false;
  const cleaned = mobile.trim().replace(/^(\+91|91|0)/, '');
  return /^[6-9]\d{9}$/.test(cleaned);
};

const normalizeMobile = (mobile) => {
  if (!mobile) return '';
  const cleaned = mobile.toString().trim().replace(/^(\+91|91|0)/, '');
  return cleaned;
};

const isValidCoordinate = (lat, lng) => {
  const latitude = parseFloat(lat);
  const longitude = parseFloat(lng);
  return (
    !isNaN(latitude) &&
    !isNaN(longitude) &&
    latitude >= -90 &&
    latitude <= 90 &&
    longitude >= -180 &&
    longitude <= 180
  );
};

module.exports = {
  isValidEmail,
  isValidMobile,
  normalizeMobile,
  isValidCoordinate,
};
