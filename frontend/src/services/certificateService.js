import axios from 'axios';

const getToken = () => localStorage.getItem('token');
const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export const createPdfBlobUrl = async (certificateId, basePath = '/student/certificates') => {
  const token = getToken();
  const url = `${BASE_URL}${basePath}/${certificateId}/pdf`;

  const response = await axios.get(url, {
    responseType: 'blob',
    headers: {
      Accept: 'application/pdf',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });

  const blob = response.data instanceof Blob
    ? response.data
    : new Blob([response.data], { type: 'application/pdf' });

  return window.URL.createObjectURL(blob);
};

export const downloadCertificatePDF = async (certificateId, serial, basePath = '/student/certificates') => {
  const url = await createPdfBlobUrl(certificateId, basePath);
  const link = document.createElement('a');

  link.href = url;
  link.setAttribute('download', `${serial || `certificate-${certificateId}`}.pdf`);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

export const previewCertificatePDF = async (certificateId, basePath = '/student/certificates') => {
  const url = await createPdfBlobUrl(certificateId, basePath);
  const previewWindow = window.open(url, '_blank', 'noopener,noreferrer');

  if (!previewWindow) {
    window.URL.revokeObjectURL(url);
    throw new Error('Popup blocked. Please allow popups to preview the certificate.');
  }

  window.setTimeout(() => window.URL.revokeObjectURL(url), 60_000);
  return previewWindow;
};