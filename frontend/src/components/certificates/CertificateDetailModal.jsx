import React, { useState, useEffect } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import toast from 'react-hot-toast';
import Modal from '../shared/Modal';
import Badge from '../shared/Badge';
import Button from '../shared/Button';
import LoadingSpinner from '../shared/LoadingSpinner';
import api from '../../services/api';
import { formatDate } from '../../utils/helpers';
import {
 Eye, Download, Copy, Check, Loader2, Clock, ShieldX, RotateCcw,
 Shield, AlertCircle, Lock, ShieldCheck, AlertTriangle, Ban,
} from 'lucide-react';

export default function CertificateDetailModal({
 open,
 onClose,
 certificate,
 loading,
 downloadingId,
 onDownloadPdf,
 onPreviewPdf,
 role = 'student',
 onRevoked, // callback(certId) — called after successful revoke/revalidate
}) {
 const [copiedSerial, setCopiedSerial] = useState(null);
 const [copiedLink, setCopiedLink] = useState(false);
 const [linkRevealed, setLinkRevealed] = useState(false);
 const [shareLink, setShareLink] = useState('');
 const [loadingShareLink, setLoadingShareLink] = useState(false);
 const [shareLinkError, setShareLinkError] = useState(false);

 // Revoke modal state
 const [showRevokeModal, setShowRevokeModal] = useState(false);
 const [revokeReason, setRevokeReason] = useState('');
 const [revokeLoading, setRevokeLoading] = useState(false);

 // Revalidate modal state
 const [showRevalidateModal, setShowRevalidateModal] = useState(false);
 const [revalidateReason, setRevalidateReason] = useState('');
 const [revalidateLoading, setRevalidateLoading] = useState(false);

 // When modal opens or certificate changes, fetch share link (student only)
 useEffect(() => {
 if (!open || !certificate?.id || role !== 'student') {
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

 const isRevoked = certificate?.status === 'revoked' || !!certificate?.revokedAt;
 const revokedByRole = certificate?.revokedByRole;

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

 // ── Revoke handler ────────────────────────────────────────────────────────

 const handleRevoke = async () => {
 if (revokeReason.trim().length < 10) {
 toast.error('Reason must be at least 10 characters');
 return;
 }
 setRevokeLoading(true);
 try {
 const basePath = role === 'admin' ? '/admin/certificates' : '/university/certificates';
 await api.post(`${basePath}/${certificate.id}/revoke`, { reason: revokeReason.trim() });
 toast.success('Certificate revoked successfully');
 setShowRevokeModal(false);
 setRevokeReason('');
 onRevoked && onRevoked(certificate.id);
 onClose();
 } catch (err) {
 toast.error(err.response?.data?.message || 'Failed to revoke certificate');
 } finally {
 setRevokeLoading(false);
 }
 };

 // ── Revalidate handler ────────────────────────────────────────────────────

 const handleRevalidate = async () => {
 setRevalidateLoading(true);
 try {
 const basePath = role === 'admin' ? '/admin/certificates' : '/university/certificates';
 await api.post(`${basePath}/${certificate.id}/revalidate`, { reason: revalidateReason.trim() });
 toast.success('Certificate revalidated successfully');
 setShowRevalidateModal(false);
 setRevalidateReason('');
 onRevoked && onRevoked(certificate.id);
 onClose();
 } catch (err) {
 toast.error(err.response?.data?.message || 'Failed to revalidate certificate');
 } finally {
 setRevalidateLoading(false);
 }
 };

 return (
 <>
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
 {isRevoked ? (
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

 {/* ── Revocation Banner ───────────────────────────────────── */}
 {isRevoked && (
 <div className="rounded-xl border p-4 border-red-500 bg-red-50 dark:bg-red-950/30">
 <div className="flex items-start gap-3">
 <ShieldX className="h-5 w-5 mt-0.5 shrink-0 text-red-600" />
 <div className="flex-1 min-w-0">
 <div className="flex flex-wrap items-center gap-2">
 <p className="text-sm font-bold text-red-700 dark:text-red-400">
 This certificate is revoked
 </p>
 <span className="inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-400">
 {revokedByRole === 'admin' ? 'By Administrator' : 'By University'}
 </span>
 </div>
 {certificate.revokedAt && (
 <p className="mt-1 text-xs text-[var(--text-muted)]">
 Revoked on: {formatTS(certificate.revokedAt)}
 </p>
 )}
 {certificate.revocationReason && (
 <blockquote className="mt-2 rounded-lg border-l-4 border-red-300 bg-white/50 dark:bg-black/20 px-3 py-2 text-xs italic text-[var(--text-secondary)]">
 "{certificate.revocationReason}"
 </blockquote>
 )}

 {/* University: revalidate button or admin-lock notice */}
 {role === 'university' && (
 <div className="mt-3">
 {revokedByRole === 'university' ? (
 <Button
 size="sm"
 variant="secondary"
 className="border-green-500 text-green-700 hover:bg-green-50 dark:hover:bg-green-950/30"
 onClick={() => setShowRevalidateModal(true)}
 >
 <RotateCcw className="h-3.5 w-3.5 mr-1.5" />
 Revalidate Certificate
 </Button>
 ) : revokedByRole === 'admin' ? (
 <div className="flex items-center gap-1.5 mt-1">
 <Ban className="h-3.5 w-3.5 text-[var(--text-muted)]" />
 <span className="text-xs text-[var(--text-muted)]">
 Revoked by administrator — cannot be revalidated by university
 </span>
 </div>
 ) : null}
 </div>
 )}

 {/* Admin: always show revalidate */}
 {role === 'admin' && (
 <div className="mt-3">
 <Button
 size="sm"
 variant="secondary"
 className="border-green-500 text-green-700 hover:bg-green-50 dark:hover:bg-green-950/30"
 onClick={() => setShowRevalidateModal(true)}
 >
 <RotateCcw className="h-3.5 w-3.5 mr-1.5" />
 Revalidate Certificate
 </Button>
 {revokedByRole === 'university' && (
 <p className="mt-1 text-[10px] text-[var(--text-muted)]">
 Originally revoked by the university
 </p>
 )}
 </div>
 )}
 </div>
 </div>
 </div>
 )}

 <div className="grid gap-4 sm:grid-cols-2">
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
 <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold bg-blue-100 text-blue-700 /30 ">
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
 <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ${isRevoke ? 'bg-red-100 text-red-700 /30 ' : 'bg-green-100 text-green-700 /30 '}`}>
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

 {/* Revoke Action (active certificates only, university/admin) */}
 {(role === 'university' || role === 'admin') && !isRevoked && (
 <div className="pt-2 border-t border-[var(--border)]">
 <Button
 variant="ghost"
 size="sm"
 className="text-red-600 hover:bg-red-50 dark:hover:bg-red-950/30"
 onClick={() => setShowRevokeModal(true)}
 >
 <ShieldX className="h-3.5 w-3.5 mr-1.5" />
 Revoke Certificate
 </Button>
 </div>
 )}

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

 {/* Verification Section (Only visible to Student — QR code and share link are student privacy tools) */}
 {role === 'student' && (
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
 
 {/* Right Column: QR Code — students only */}
 {role === 'student' && (
 <div className="shrink-0 flex flex-col items-center justify-center rounded-xl border border-[var(--border)] bg-white p-4 shadow-sm min-w-[200px]">
 {loadingShareLink && !shareLink ? (
 <div className="w-[180px] h-[180px] flex flex-col items-center justify-center rounded-lg bg-gray-50 border border-dashed border-gray-200 animate-pulse">
 <Loader2 className="h-8 w-8 text-[var(--brand)] animate-spin mb-2" />
 <span className="text-xs text-[var(--text-muted)] font-medium">Generating QR...</span>
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
 <p className="mt-2 text-[10px] font-semibold uppercase tracking-wider text-[var(--text-muted)]">Scan to verify</p>
 </>
 ) : (
 <div className="w-[180px] h-[180px] flex flex-col items-center justify-center rounded-lg bg-gray-50 border border-dashed border-gray-200 text-center p-4">
 <AlertCircle className="h-8 w-8 text-[var(--text-muted)] mb-2" />
 <span className="text-xs font-semibold text-[var(--text-muted)]">QR unavailable</span>
 </div>
 )}
 </div>
 )}
 </div>
 )}
 </div>
 ) : null}
 </Modal>

 {/* ── Revoke Modal ─────────────────────────────────────────────────── */}
 <Modal
 open={showRevokeModal}
 onClose={() => { setShowRevokeModal(false); setRevokeReason(''); }}
 title="Revoke Certificate"
 size="sm"
 >
 <div className="space-y-4">
 <div className="flex items-start gap-3 rounded-xl border border-red-200 bg-red-50/60 /40 /20 p-4">
 <AlertTriangle className="h-5 w-5 text-red-600 shrink-0 mt-0.5" />
 <div>
 <p className="text-sm font-semibold text-red-700 ">Revoking this certificate will mark it as invalid.</p>
 <p className="text-xs text-red-600/80 /70 mt-1">
 Anyone verifying it will see it as revoked.
 {role === 'university' && ' Only you can undo this — unless overridden by an admin.'}
 </p>
 </div>
 </div>
 <div>
 <label className="block text-sm font-semibold text-[var(--text-primary)] mb-1.5">
 Reason <span className="text-red-500">*</span>
 </label>
 <textarea
 rows={3}
 value={revokeReason}
 onChange={(e) => setRevokeReason(e.target.value)}
 placeholder="Explain why this certificate is being revoked (min 10 characters)..."
 className="w-full rounded-xl border border-[var(--border)] bg-[var(--bg-surface)] px-4 py-3 text-sm text-[var(--text-primary)] placeholder-[var(--text-muted)] focus:border-red-400 focus:outline-none focus:ring-1 focus:ring-red-400 resize-none"
 />
 <p className={`mt-1 text-xs ${revokeReason.length < 10 && revokeReason.length > 0 ? 'text-red-500' : 'text-[var(--text-muted)]'}`}>
 {revokeReason.length}/10 minimum characters
 </p>
 </div>
 <div className="flex gap-2 justify-end pt-1">
 <Button variant="secondary" onClick={() => { setShowRevokeModal(false); setRevokeReason(''); }}>
 Cancel
 </Button>
 <Button
 variant="danger"
 onClick={handleRevoke}
 loading={revokeLoading}
 disabled={revokeReason.trim().length < 10 || revokeLoading}
 className="bg-red-600 hover:bg-red-700 text-white border-none"
 >
 <ShieldX className="h-3.5 w-3.5 mr-1.5" />
 Revoke Certificate
 </Button>
 </div>
 </div>
 </Modal>

 {/* ── Revalidate Modal ─────────────────────────────────────────────── */}
 <Modal
 open={showRevalidateModal}
 onClose={() => { setShowRevalidateModal(false); setRevalidateReason(''); }}
 title="Revalidate Certificate"
 size="sm"
 >
 <div className="space-y-4">
 <div className="flex items-start gap-3 rounded-xl border border-green-200 bg-green-50/60 /40 /20 p-4">
 <RotateCcw className="h-5 w-5 text-green-600 shrink-0 mt-0.5" />
 <div>
 <p className="text-sm font-semibold text-green-700 ">Revalidate this certificate?</p>
 <p className="text-xs text-green-600/80 /70 mt-1">
 This will restore the certificate to active status and make it valid for verification again.
 </p>
 </div>
 </div>
 <div>
 <label className="block text-sm font-semibold text-[var(--text-primary)] mb-1.5">
 Reason <span className="text-[var(--text-muted)] font-normal">(optional)</span>
 </label>
 <textarea
 rows={2}
 value={revalidateReason}
 onChange={(e) => setRevalidateReason(e.target.value)}
 placeholder="Reason for revalidation (optional)..."
 className="w-full rounded-xl border border-[var(--border)] bg-[var(--bg-surface)] px-4 py-3 text-sm text-[var(--text-primary)] placeholder-[var(--text-muted)] focus:border-green-400 focus:outline-none focus:ring-1 focus:ring-green-400 resize-none"
 />
 </div>
 <div className="flex gap-2 justify-end pt-1">
 <Button variant="secondary" onClick={() => { setShowRevalidateModal(false); setRevalidateReason(''); }}>
 Cancel
 </Button>
 <Button
 onClick={handleRevalidate}
 loading={revalidateLoading}
 disabled={revalidateLoading}
 className="bg-green-600 hover:bg-green-700 text-white border-none"
 >
 <RotateCcw className="h-3.5 w-3.5 mr-1.5" />
 Confirm Revalidate
 </Button>
 </div>
 </div>
 </Modal>
 </>
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
