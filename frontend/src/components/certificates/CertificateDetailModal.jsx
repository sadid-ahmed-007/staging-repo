import React, { useState } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import Modal from '../shared/Modal';
import Badge from '../shared/Badge';
import Button from '../shared/Button';
import LoadingSpinner from '../shared/LoadingSpinner';
import { formatDate } from '../../utils/helpers';
import { Eye, Download, Copy, Check, Loader2, Clock, ShieldX, RotateCcw, Shield } from 'lucide-react';

export default function CertificateDetailModal({
  open,
  onClose,
  certificate,
  loading,
  downloadingId,
  onDownloadPdf,
  onPreviewPdf,
  role = 'student',
}) {
  const [copiedSerial, setCopiedSerial] = useState(null);
  const [copiedLink, setCopiedLink] = useState(false);

  if (!open) return null;

  const getVerificationUrl = (cert) => {
    return cert.shareLink || `${window.location.origin}/verify?s=${encodeURIComponent(cert.serial)}`;
  };

  const handleCopyVerificationLink = async (cert) => {
    try {
      await navigator.clipboard.writeText(getVerificationUrl(cert));
      setCopiedLink(true);
      setTimeout(() => setCopiedLink(false), 2000);
    } catch (err) {
      console.error('Failed to copy share link:', err);
    }
  };

  const handleCopySerial = async (serial) => {
    try {
      await navigator.clipboard.writeText(serial);
      setCopiedSerial(serial);
      setTimeout(() => setCopiedSerial(null), 2000);
    } catch (err) {
      console.error('Failed to copy serial:', err);
    }
  };

  const formatTS = (ts) => {
    if (!ts) return '—';
    try {
      return new Date(ts).toLocaleString('en-GB', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit',
      });
    } catch { return ts; }
  };

  return (
    <Modal open={open} onClose={onClose} title="Certificate Details" size="lg">
      {loading || certificate?.loading ? (
        <div className="flex min-h-[300px] items-center justify-center">
          <LoadingSpinner />
        </div>
      ) : certificate ? (
        <div className="space-y-6">
          <div className="space-y-5">
            <h2 className="text-2xl font-bold text-[var(--text-primary)]">
              {certificate.certificateLevel}
              {certificate.certificateName && certificate.certificateName !== certificate.certificateLevel && (
                <span className="text-xl font-normal text-[var(--text-secondary)] block sm:inline sm:ml-2">
                  {certificate.certificateName}
                </span>
              )}
            </h2>
            <div className="rounded-2xl border border-[var(--border)] bg-[var(--bg-elevated)] p-5">
              <div className="flex flex-wrap items-center gap-2">
                {role === 'student' && (
                  <Badge
                    variant={certificate.isPubliclyShareable ? 'warning' : 'success'}
                    className="transition-colors duration-300"
                  >
                    {certificate.isPubliclyShareable ? 'Public' : 'Private'}
                  </Badge>
                )}
                {certificate.status === 'revoked' || certificate.revokedAt ? (
                  <Badge variant="danger">Revoked</Badge>
                ) : role !== 'student' && (
                  <Badge variant="success">Active</Badge>
                )}
              </div>
              <p className="mt-4 text-sm uppercase tracking-[0.18em] text-[var(--text-muted)]">Serial Number</p>
              <div className="flex items-center gap-3 mt-1">
                <p className="font-mono text-lg font-semibold text-[var(--text-primary)]">{certificate.serial}</p>
                <button
                  onClick={() => handleCopySerial(certificate.serial)}
                  className="p-1.5 rounded-lg text-[var(--text-muted)] hover:text-[var(--brand)] hover:bg-[var(--brand-light)] transition flex items-center gap-1"
                  title="Copy serial number"
                >
                  {copiedSerial === certificate.serial ? (
                    <>
                      <Check className="h-5 w-5 text-green-600 animate-fade-in" />
                      <span className="text-xs text-green-600 font-medium">Copied!</span>
                    </>
                  ) : (
                    <Copy className="h-5 w-5" />
                  )}
                </button>
              </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-2 md:grid-cols-3">
              {role !== 'student' && <DetailTile label="Student" value={certificate.student?.name || certificate.student_name} />}
              <DetailTile label="Institution" value={certificate.institution?.name || certificate.institution_name} />
              <DetailTile label="Issue Date" value={formatDate(certificate.issueDate || certificate.issue_date)} />
              <DetailTile label="Program" value={certificate.enrollment?.program || certificate.certificate_name} />
              <DetailTile label="Major" value={certificate.major} />
              <DetailTile label="CGPA" value={certificate.cgpa} />
              <DetailTile label="Registration No" value={certificate.enrollment?.rollNumber || certificate.enrollment?.enrollmentNumber || certificate.roll_number} />
            </div>
            
            {/* Admin Revocation History Section */}
            {role === 'admin' && certificate.revocationHistory && certificate.revocationHistory.length > 0 && (
              <div className="mt-6 pt-5 border-t border-[var(--border)]">
                <h3 className="flex items-center gap-2 text-sm font-bold text-[var(--text-primary)] mb-4">
                  <Clock className="h-4 w-4 text-[var(--text-muted)]" />
                  Revocation History
                </h3>
                <div className="relative">
                  <div className="absolute left-3.5 top-0 bottom-0 w-px bg-[var(--border)]" />
                  <div className="space-y-4">
                    {/* Issued entry */}
                    {(certificate.issueDate || certificate.issue_date) && (
                      <div className="relative flex gap-4">
                        <div className="relative z-10 flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-blue-500 ring-2 ring-white dark:ring-gray-900">
                          <Shield className="h-3 w-3 text-white" />
                        </div>
                        <div className="flex-1 min-w-0 pb-2">
                          <div className="flex flex-wrap items-center gap-2">
                            <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300">
                              Issued
                            </span>
                            <span className="text-xs text-[var(--text-muted)]">System</span>
                          </div>
                          <p className="mt-1 text-xs text-[var(--text-secondary)]">
                            Certificate issued on {formatDate(certificate.issueDate || certificate.issue_date)}
                          </p>
                        </div>
                      </div>
                    )}
                    {/* Reverse history to show newest first */}
                    {[...certificate.revocationHistory].reverse().map((entry, i) => {
                      const isRevoke = entry.action === 'revoked';
                      const actor = entry.performedByName || `User #${entry.performedBy}`;
                      const actorRole = entry.performedByRole || '—';
                      return (
                        <div key={i} className="relative flex gap-4">
                          <div className={`relative z-10 flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full ${isRevoke ? 'bg-red-500' : 'bg-green-500'} ring-2 ring-white dark:ring-gray-900`}>
                            {isRevoke ? (
                              <ShieldX className="h-3 w-3 text-white" />
                            ) : (
                              <RotateCcw className="h-3 w-3 text-white" />
                            )}
                          </div>
                          <div className="flex-1 min-w-0 pb-2">
                            <div className="flex flex-wrap items-center gap-2">
                              <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ${isRevoke ? 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300' : 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300'}`}>
                                {isRevoke ? 'Revoked' : 'Restored'}
                              </span>
                              <span className="text-xs text-[var(--text-secondary)]">
                                by <span className="font-medium text-[var(--text-primary)]">{actor}</span>
                                {' '}<span className="italic text-[var(--text-muted)]">({actorRole})</span>
                              </span>
                            </div>
                            <p className="mt-1 text-xs text-[var(--text-secondary)] leading-relaxed">
                              <span className="font-medium">Reason:</span> {entry.reason || '—'}
                            </p>
                            <p className="mt-0.5 text-xs text-[var(--text-muted)]">{formatTS(entry.timestamp)}</p>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Verification Section */}
          <div className="flex flex-col sm:flex-row gap-6 items-center sm:items-stretch rounded-2xl border border-[var(--border)] p-5 bg-[var(--bg-surface)]">
            {role === 'student' && (
              <div className="flex-1 space-y-4 w-full">
                <div>
                  <h3 className="text-sm font-semibold uppercase tracking-[0.18em] text-[var(--text-muted)]">Verification Link</h3>
                  <p className="mt-1 text-sm text-[var(--text-secondary)]">
                    Use this link or the QR code to verify this certificate's authenticity.
                  </p>
                  <div className="mt-3 rounded-lg bg-[var(--bg-elevated)] p-3 border border-[var(--border)] break-all text-sm font-mono text-[var(--text-primary)]">
                    {getVerificationUrl(certificate)}
                  </div>
                </div>
                <div className="flex gap-2 overflow-x-auto pb-1 whitespace-nowrap">
                  <Button
                    size="sm"
                    onClick={() => handleCopyVerificationLink(certificate)}
                    className="shrink-0"
                  >
                    {copiedLink ? (
                      <>
                        <Check className="mr-1.5 h-3.5 w-3.5" />
                        Copied!
                      </>
                    ) : (
                      <>
                        <Copy className="mr-1.5 h-3.5 w-3.5" />
                        Copy Share Link
                      </>
                    )}
                  </Button>
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => onPreviewPdf(certificate)}
                    disabled={downloadingId === certificate.id}
                    className="shrink-0"
                  >
                    <Eye className="mr-1.5 h-3.5 w-3.5" />
                    Preview PDF
                  </Button>
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => onDownloadPdf(certificate)}
                    disabled={downloadingId === certificate.id}
                    className="shrink-0"
                  >
                    {downloadingId === certificate.id ? (
                      <Loader2 className="mr-1.5 h-3.5 w-3.5 animate-spin" />
                    ) : (
                      <Download className="mr-1.5 h-3.5 w-3.5" />
                    )}
                    Download PDF
                  </Button>
                </div>
              </div>
            )}

            {role !== 'student' && (
              <div className="flex-1 flex flex-col justify-center gap-3">
                <Button
                  onClick={() => onPreviewPdf(certificate)}
                  disabled={downloadingId === certificate.id}
                  className="w-full sm:w-auto"
                  variant="outline"
                >
                  <Eye className="mr-1.5 h-4 w-4" />
                  Preview PDF
                </Button>
                <Button
                  onClick={() => onDownloadPdf(certificate)}
                  disabled={downloadingId === certificate.id}
                  className="w-full sm:w-auto"
                >
                  {downloadingId === certificate.id ? (
                    <Loader2 className="mr-1.5 h-4 w-4 animate-spin" />
                  ) : (
                    <Download className="mr-1.5 h-4 w-4" />
                  )}
                  Download PDF
                </Button>
              </div>
            )}
            
            <div className="shrink-0 flex flex-col items-center justify-center rounded-xl border border-[var(--border)] bg-white p-3 shadow-sm">
              <QRCodeSVG
                value={getVerificationUrl(certificate)}
                size={120}
                level="H"
                includeMargin={true}
                fgColor="#0f172a"
                bgColor="#ffffff"
              />
              <p className="mt-2 text-[10px] font-semibold uppercase tracking-wider text-gray-500">Scan to verify</p>
            </div>
          </div>
        </div>
      ) : null}
    </Modal>
  );
}

function DetailTile({ label, value }) {
  return (
    <div className="rounded-2xl border border-[var(--border)] bg-[var(--bg-surface)] px-4 py-3 shadow-sm">
      <p className="text-xs font-semibold uppercase tracking-[0.16em] text-[var(--text-muted)]">{label}</p>
      <p className="mt-1 text-sm font-medium text-[var(--text-primary)]">{value || 'N/A'}</p>
    </div>
  );
}
