import React, { useState, useEffect } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import toast from 'react-hot-toast';
import Modal from '../shared/Modal';
import Badge from '../shared/Badge';
import Button from '../shared/Button';
import LoadingSpinner from '../shared/LoadingSpinner';
import api from '../../services/api';
import { formatDate } from '../../utils/helpers';
import { Eye, Download, Copy, Check, Loader2, Clock, ShieldX, RotateCcw, Shield, AlertCircle, Lock, ShieldCheck } from 'lucide-react';

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
  const [linkRevealed, setLinkRevealed] = useState(false);
  const [shareLink, setShareLink] = useState('');
  const [loadingShareLink, setLoadingShareLink] = useState(false);
  const [shareLinkError, setShareLinkError] = useState(false);

  // When modal opens or certificate changes, fetch share link
  useEffect(() => {
    if (!open || !certificate?.id || role === 'verifier') {
      setShareLink('');
      setLoadingShareLink(false);
      setShareLinkError(false);
      setCopiedLink(false);
      setLinkRevealed(false);
      return;
    }

    const getBasePath = () => {
      if (role === 'admin') return '/admin/certificates';
      if (role === 'university') return '/university/certificates';
      return '/student/certificates';
    };

    let isMounted = true;
    const fetchShareLink = async () => {
      setLoadingShareLink(true);
      setShareLinkError(false);
      try {
        const basePath = getBasePath();
        const res = await api.get(`${basePath}/${certificate.id}/share-link`);
        const link = res.data?.shareLink || res.data?.data?.shareLink;
        if (isMounted) {
          if (link) {
            setShareLink(link);
          } else if (certificate.serial) {
            setShareLink(`${window.location.origin}/verify?s=${encodeURIComponent(certificate.serial)}`);
          }
        }
      } catch (err) {
        console.warn('Could not fetch custom share-link, using standard verification URL:', err);
        if (isMounted) {
          if (certificate.serial) {
            setShareLink(`${window.location.origin}/verify?s=${encodeURIComponent(certificate.serial)}`);
            setShareLinkError(false);
          } else {
            setShareLinkError(true);
          }
        }
      } finally {
        if (isMounted) {
          setLoadingShareLink(false);
        }
      }
    };

    fetchShareLink();
    return () => {
      isMounted = false;
    };
  }, [open, certificate?.id, certificate?.shareLink, certificate?.serial, role]);

  if (!open) return null;

  const getMaskedLink = (link, serial) => {
    if (!link) return '';
    const sIndex = link.indexOf('?s=');
    if (sIndex !== -1) {
      const prefix = link.substring(0, sIndex + 3);
      const serialSnippet = serial ? serial.substring(0, 10) : '';
      return `${prefix}${serialSnippet}...`;
    }
    return link.length > 38 ? `${link.substring(0, 38)}...` : link;
  };

  const handleCopyVerificationLink = async () => {
    let linkToCopy = shareLink;

    if (!linkToCopy && certificate?.id && role !== 'verifier') {
      try {
        setLoadingShareLink(true);
        const basePath = role === 'admin'
          ? '/admin/certificates'
          : role === 'university'
          ? '/university/certificates'
          : '/student/certificates';
        const res = await api.get(`${basePath}/${certificate.id}/share-link`);
        linkToCopy = res.data?.shareLink || res.data?.data?.shareLink;
        if (linkToCopy) {
          setShareLink(linkToCopy);
        }
      } catch (err) {
        if (certificate.serial) {
          linkToCopy = `${window.location.origin}/verify?s=${encodeURIComponent(certificate.serial)}`;
          setShareLink(linkToCopy);
        } else {
          toast.error('Failed to get share link');
          setLoadingShareLink(false);
          return;
        }
      } finally {
        setLoadingShareLink(false);
      }
    }

    if (!linkToCopy && certificate?.serial) {
      linkToCopy = `${window.location.origin}/verify?s=${encodeURIComponent(certificate.serial)}`;
      setShareLink(linkToCopy);
    }

    if (linkToCopy) {
      try {
        await navigator.clipboard.writeText(linkToCopy);
        setCopiedLink(true);
        setLinkRevealed(true);
        toast.success('Share link copied to clipboard');
        setTimeout(() => setCopiedLink(false), 2000);
      } catch (err) {
        console.error('Failed to copy share link:', err);
        toast.error('Failed to copy link to clipboard');
      }
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
              {role !== 'student' && (
                <DetailTile 
                  label="Student" 
                  value={
                    certificate.student?.name || 
                    certificate.studentName || 
                    certificate.student_name || 
                    certificate.issuedName || 
                    certificate.issued_name
                  } 
                />
              )}
              <DetailTile 
                label="Institution" 
                value={
                  certificate.institution?.name || 
                  certificate.institutionName || 
                  certificate.institution_name
                } 
              />
              <DetailTile label="Issue Date" value={formatDate(certificate.issueDate || certificate.issue_date)} />
              <DetailTile label="Program / Degree" value={certificate.program || certificate.enrollment?.program || certificate.certificateName || certificate.certificate_name} />
              <DetailTile label="Department" value={certificate.department || certificate.enrollment?.department} />
              <DetailTile label="Major" value={certificate.major || certificate.enrollment?.major} />
              <DetailTile label="CGPA" value={certificate.cgpa != null ? certificate.cgpa : certificate.enrollment?.cgpa} />
              {certificate.degreeClass && (
                <DetailTile label="Degree Class" value={certificate.degreeClass} />
              )}
              <DetailTile label="Registration / Roll No" value={certificate.rollNumber || certificate.enrollment?.rollNumber || certificate.enrollment?.enrollmentNumber || certificate.enrollmentNumber || certificate.roll_number} />
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

          {/* Verifier Actions: Academic Inspection Only (No verification link or QR code) */}
          {role === 'verifier' && (
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 rounded-2xl border border-[var(--border)] p-4 bg-[var(--bg-surface)]">
              <div className="flex items-center gap-2.5">
                <ShieldCheck className="h-5 w-5 text-emerald-500 shrink-0" />
                <div>
                  <p className="text-sm font-semibold text-[var(--text-primary)]">Verified Access Active</p>
                  <p className="text-xs text-[var(--text-secondary)]">You are reviewing authorized certificate details for this student.</p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                {certificate.isPubliclyShareable ? (
                  <>
                    {onPreviewPdf && (
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
                    )}
                    {onDownloadPdf && (
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
                    )}
                  </>
                ) : (
                  <span className="text-xs text-[var(--text-muted)] flex items-center gap-1.5 px-3 py-1.5 bg-[var(--bg-elevated)] rounded-lg border border-[var(--border)]">
                    <Lock className="w-3.5 h-3.5 text-[var(--text-muted)]" />
                    Private PDF (Download Restricted)
                  </span>
                )}
              </div>
            </div>
          )}

          {/* Verification Section (Only visible to Student, University, and Admin) */}
          {role !== 'verifier' && (
            <div className="flex flex-col sm:flex-row gap-6 items-center sm:items-stretch rounded-2xl border border-[var(--border)] p-5 bg-[var(--bg-surface)]">
              <div className="flex-1 space-y-4 w-full">
                <div>
                  <h3 className="text-sm font-semibold uppercase tracking-[0.18em] text-[var(--text-muted)]">Verification Link</h3>
                  <p className="mt-1 text-sm text-[var(--text-secondary)]">
                    Use this link or the QR code to verify this certificate's authenticity.
                  </p>
                  <div className="mt-3 rounded-lg bg-[var(--bg-elevated)] p-3 border border-[var(--border)] break-all text-sm font-mono text-[var(--text-primary)]">
                    {loadingShareLink && !shareLink ? (
                      <span className="text-[var(--text-muted)] italic animate-pulse">Loading share link...</span>
                    ) : shareLinkError && !shareLink ? (
                      <span className="text-[var(--danger)]">Verification link unavailable</span>
                    ) : shareLink ? (
                      linkRevealed ? shareLink : getMaskedLink(shareLink, certificate.serial)
                    ) : (
                      <span className="text-[var(--text-muted)] italic">Generating share link...</span>
                    )}
                  </div>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Button
                    size="sm"
                    onClick={handleCopyVerificationLink}
                    disabled={loadingShareLink && !shareLink}
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
                        Copy Link
                      </>
                    )}
                  </Button>
                  {onPreviewPdf && (
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
                  )}
                  {onDownloadPdf && (
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
                  )}
                </div>
              </div>
              
              {/* Right Column: QR Code */}
              <div className="shrink-0 flex flex-col items-center justify-center rounded-xl border border-[var(--border)] bg-white p-4 shadow-sm min-w-[200px]">
                {loadingShareLink && !shareLink ? (
                  <div className="w-[180px] h-[180px] flex flex-col items-center justify-center rounded-lg bg-gray-50 border border-dashed border-gray-200 animate-pulse">
                    <Loader2 className="h-8 w-8 text-[var(--brand)] animate-spin mb-2" />
                    <span className="text-xs text-gray-500 font-medium">Generating QR...</span>
                  </div>
                ) : shareLink ? (
                  <>
                    <QRCodeSVG
                      value={shareLink}
                      size={180}
                      level="H"
                      includeMargin={true}
                      fgColor="#0f172a"
                      bgColor="#ffffff"
                    />
                    <p className="mt-2 text-[10px] font-semibold uppercase tracking-wider text-gray-500">Scan to verify</p>
                  </>
                ) : (
                  <div className="w-[180px] h-[180px] flex flex-col items-center justify-center rounded-lg bg-gray-50 border border-dashed border-gray-200 text-center p-4">
                    <AlertCircle className="h-8 w-8 text-gray-400 mb-2" />
                    <span className="text-xs font-semibold text-gray-500">QR unavailable</span>
                  </div>
                )}
              </div>
            </div>
          )}
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
