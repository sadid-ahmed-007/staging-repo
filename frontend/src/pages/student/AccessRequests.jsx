import React, { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-hot-toast';
import {
 Building2,
 Mail,
 Calendar,
 Clock,
 CheckCircle2,
 XCircle,
 AlertTriangle,
 ChevronDown,
 ChevronUp,
 ShieldCheck,
 ShieldAlert,
 Trash2,
 RefreshCw,
 Info,
 ExternalLink,
 Ban,
 Award,
 GraduationCap
} from 'lucide-react';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/shared/Card';
import Badge from '../../components/shared/Badge';
import Button from '../../components/shared/Button';
import Modal from '../../components/shared/Modal';
import ConfirmModal from '../../components/shared/ConfirmModal';
import LoadingSpinner from '../../components/shared/LoadingSpinner';
import ErrorMessage from '../../components/shared/ErrorMessage';
import api from '../../services/api';
import { formatDate } from '../../utils/helpers';

function timeAgo(isoString) {
 if (!isoString) return '';
 const date = new Date(isoString);
 const now = new Date();
 const diffSec = Math.floor((now.getTime() - date.getTime()) / 1000);
 if (diffSec < 60) return 'Just now';
 if (diffSec < 3600) {
 const mins = Math.floor(diffSec / 60);
 return `${mins} minute${mins > 1 ? 's' : ''} ago`;
 }
 if (diffSec < 86400) {
 const hours = Math.floor(diffSec / 3600);
 return `${hours} hour${hours > 1 ? 's' : ''} ago`;
 }
 const days = Math.floor(diffSec / 86400);
 if (days < 30) {
 return `${days} day${days > 1 ? 's' : ''} ago`;
 }
 return formatDate(isoString);
}

function GrantDaysBadge({ daysRemaining }) {
 if (daysRemaining < 7) {
 return (
 <Badge variant="danger" size="sm" className="flex items-center gap-1 font-medium">
 <Clock className="w-3 h-3" />
 {daysRemaining <= 0 ? 'Expired' : `${daysRemaining} days remaining`}
 </Badge>
 );
 } else if (daysRemaining <= 30) {
 return (
 <Badge variant="warning" size="sm" className="flex items-center gap-1 font-medium">
 <Clock className="w-3 h-3" />
 {daysRemaining} days remaining
 </Badge>
 );
 } else {
 return (
 <Badge variant="success" size="sm" className="flex items-center gap-1 font-medium">
 <Clock className="w-3 h-3" />
 {daysRemaining} days remaining
 </Badge>
 );
 }
}

export default function StudentAccessRequests() {
 const [loading, setLoading] = useState(true);
 const [error, setError] = useState('');

 // Data
 const [pendingRequests, setPendingRequests] = useState([]);
 const [activeGrants, setActiveGrants] = useState([]);
 const [historyRequests, setHistoryRequests] = useState([]);

 // Collapsible History State
 const [historyExpanded, setHistoryExpanded] = useState(false);

 // Approve Modal State
 const [approveModalOpen, setApproveModalOpen] = useState(false);
 const [targetRequest, setTargetRequest] = useState(null);
 const [grantDuration, setGrantDuration] = useState(30);
 const [approveMessage, setApproveMessage] = useState('');
 const [approving, setApproving] = useState(false);

 // Reject Modal State
 const [rejectModalOpen, setRejectModalOpen] = useState(false);
 const [rejectReason, setRejectReason] = useState('');
 const [rejecting, setRejecting] = useState(false);

 // Revoke Modal State
 const [revokeModalOpen, setRevokeModalOpen] = useState(false);
 const [targetGrant, setTargetGrant] = useState(null);
 const [revoking, setRevoking] = useState(false);

 // Fetch all data
 const fetchData = useCallback(async () => {
 setLoading(true);
 setError('');
 try {
 const [requestsRes, grantsRes] = await Promise.all([
 api.get('/student/access-requests', { params: { status: 'all', page: 0, size: 100 } }),
 api.get('/student/access-grants'),
 ]);

 if (requestsRes.data.success) {
 const all = requestsRes.data.data || [];
 setPendingRequests(all.filter((r) => r.status === 'pending'));
 setHistoryRequests(all.filter((r) => r.status !== 'pending'));
 }

 if (grantsRes.data.success) {
 setActiveGrants(grantsRes.data.active || []);
 }
 } catch (err) {
 console.error('Failed to load student access data:', err);
 setError(err.response?.data?.message || 'Failed to load access requests.');
 } finally {
 setLoading(false);
 }
 }, []);

 useEffect(() => {
 fetchData();
 }, [fetchData]);

 // Approve Flow
 const openApproveModal = (req) => {
 setTargetRequest(req);
 setGrantDuration(req.requestedDurationDays || 30);
 setApproveMessage('');
 setApproveModalOpen(true);
 };

 const handleConfirmApprove = async (e) => {
 e.preventDefault();
 if (!targetRequest) return;

 const duration = parseInt(grantDuration, 10);
 if (isNaN(duration) || duration < 1 || duration > 365) {
 toast.error('Grant duration must be between 1 and 365 days.');
 return;
 }

 setApproving(true);
 try {
 const { data } = await api.post(`/student/access-requests/${targetRequest.id}/respond`, {
 approved: true,
 durationDays: duration,
 responseMessage: approveMessage.trim() || undefined,
 });

 if (data.success) {
 toast.success('Access granted successfully');
 setApproveModalOpen(false);
 setTargetRequest(null);
 window.dispatchEvent(new Event('access_requests_updated'));
 fetchData();
 } else {
 toast.error(data.message || 'Failed to approve request');
 }
 } catch (err) {
 console.error('Failed to approve request:', err);
 toast.error(err.response?.data?.message || 'Failed to approve request');
 } finally {
 setApproving(false);
 }
 };

 // Reject Flow
 const openRejectModal = (req) => {
 setTargetRequest(req);
 setRejectReason('');
 setRejectModalOpen(true);
 };

 const handleConfirmReject = async (e) => {
 e.preventDefault();
 if (!targetRequest) return;

 const reason = rejectReason.trim();
 if (reason.length < 10) {
 toast.error('Reason for rejection must be at least 10 characters.');
 return;
 }

 setRejecting(true);
 try {
 const { data } = await api.post(`/student/access-requests/${targetRequest.id}/respond`, {
 approved: false,
 responseMessage: reason,
 });

 if (data.success) {
 toast.success('Request rejected');
 setRejectModalOpen(false);
 setTargetRequest(null);
 window.dispatchEvent(new Event('access_requests_updated'));
 fetchData();
 } else {
 toast.error(data.message || 'Failed to reject request');
 }
 } catch (err) {
 console.error('Failed to reject request:', err);
 toast.error(err.response?.data?.message || 'Failed to reject request');
 } finally {
 setRejecting(false);
 }
 };

 // Revoke Flow
 const openRevokeModal = (grant) => {
 setTargetGrant(grant);
 setRevokeModalOpen(true);
 };

 const handleConfirmRevoke = async () => {
 if (!targetGrant) return;
 setRevoking(true);
 try {
 const { data } = await api.delete(`/student/access-grants/${targetGrant.id}`);
 if (data.success) {
 toast.success('Access revoked');
 setRevokeModalOpen(false);
 setTargetGrant(null);
 fetchData();
 } else {
 toast.error(data.message || 'Failed to revoke access');
 }
 } catch (err) {
 console.error('Failed to revoke access:', err);
 toast.error(err.response?.data?.message || 'Failed to revoke access');
 } finally {
 setRevoking(false);
 }
 };

 return (
 <DashboardLayout>
 <div className="space-y-8 max-w-5xl mx-auto">
 {/* Page Header */}
 <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
 <div>
 <h1 className="text-2xl font-bold text-[var(--text-primary)] tracking-tight">
 Access Requests
 </h1>
 <p className="text-sm text-[var(--text-secondary)] mt-1">
 Manage who can view your academic certificates
 </p>
 </div>
 <Button
 variant="outline"
 size="sm"
 onClick={fetchData}
 disabled={loading}
 className="self-start sm:self-center"
 >
 <RefreshCw className={`w-4 h-4 mr-1.5 ${loading ? 'animate-spin' : ''}`} />
 Refresh
 </Button>
 </div>

 {error && <ErrorMessage message={error} retry={fetchData} />}

 {loading ? (
 <div className="flex min-h-[40vh] items-center justify-center">
 <LoadingSpinner />
 </div>
 ) : (
 <div className="space-y-10">
 {/* ───────────────────────────────────────────────────────────────── */}
 {/* SECTION 1: INCOMING REQUESTS (PENDING) */}
 {/* ───────────────────────────────────────────────────────────────── */}
 <section className="space-y-4">
 <div className="flex items-center justify-between">
 <div className="flex items-center gap-2">
 <h2 className="text-lg font-bold text-[var(--text-primary)]">
 Pending Requests
 </h2>
 {pendingRequests.length > 0 && (
 <Badge variant="warning" size="sm" className="font-semibold">
 {pendingRequests.length}
 </Badge>
 )}
 </div>
 </div>

 {pendingRequests.length === 0 ? (
 <Card className="p-6 text-center border border-[var(--border)] bg-[var(--bg-surface)]">
 <p className="text-sm text-[var(--text-secondary)]">
 No incoming access requests
 </p>
 </Card>
 ) : (
 <div className="space-y-4">
 {pendingRequests?.map((req) => (
 <Card
 key={req.id}
 className="p-5 sm:p-6 border border-[var(--border)] bg-[var(--bg-surface)] hover:border-[var(--brand)]/30 transition-all shadow-sm"
 >
 <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-6">
 {/* Left: Company Icon + Name + Verifier Email */}
 <div className="flex items-start gap-3.5 min-w-[220px] max-w-sm">
 <div className="w-11 h-11 rounded-xl bg-[var(--brand-light)] text-[var(--brand)] flex items-center justify-center shrink-0 border border-[var(--brand)]/20 shadow-xs">
 <Building2 className="w-5 h-5" />
 </div>
 <div className="min-w-0">
 <h3 className="text-base font-bold text-[var(--text-primary)] truncate">
 {req.verifierCompany || req.verifierName || 'Independent Verifier'}
 </h3>
 {req.verifierName && req.verifierCompany && (
 <p className="text-xs text-[var(--text-secondary)] font-medium truncate">
 {req.verifierName}
 </p>
 )}
 {req.verifierEmail && (
 <p className="text-xs text-[var(--text-muted)] truncate flex items-center gap-1 mt-0.5">
 <Mail className="w-3 h-3 shrink-0" />
 <span>{req.verifierEmail}</span>
 </p>
 )}
 </div>
 </div>

 {/* Center: Purpose + Scope Badge + Requested Duration + Relative Time */}
 <div className="flex-1 space-y-2 text-sm">
 {/* Scope indicator */}
 {(req.requestScope === 'specific' || req.requestScope === 'specific_certificate' || !!req.certificateId) ? (
 <div className="flex items-center gap-2 p-2 rounded-lg bg-[var(--brand-light)]/40 border border-[var(--brand)]/20 text-xs text-[var(--brand)] font-medium">
 <Award className="w-4 h-4 shrink-0" />
 <span>
 Requesting access to certificate: <strong>{req.certificateName || 'Specific Certificate'}</strong> ({req.certificateSerial || 'Serial not specified'})
 </span>
 </div>
 ) : (
 <div className="flex items-center gap-2 p-2 rounded-lg bg-[var(--bg-elevated)] border border-[var(--border)] text-xs text-[var(--text-secondary)]">
 <GraduationCap className="w-4 h-4 text-[var(--brand)] shrink-0" />
 <span>Requesting access to <strong>All Certificates</strong></span>
 </div>
 )}

 <div className="rounded-lg bg-[var(--bg-elevated)]/50 p-3 text-[var(--text-primary)] border border-[var(--border)] text-xs sm:text-sm whitespace-pre-wrap break-words leading-relaxed">
 {req.purpose}
 </div>
 <div className="flex flex-wrap items-center gap-4 text-xs text-[var(--text-muted)]">
 <span className="font-medium text-[var(--brand)]">
 Requesting access for: <strong>{req.requestedDurationDays || 30} days</strong>
 </span>
 <span>•</span>
 <span className="flex items-center gap-1">
 <Clock className="w-3.5 h-3.5" />
 Received: {timeAgo(req.requestedAt)}
 </span>
 </div>
 </div>

 {/* Right: Approve & Reject Buttons Stacked */}
 <div className="flex flex-row lg:flex-col gap-2.5 w-full lg:w-32 shrink-0">
 <Button
 variant="success"
 size="sm"
 className="flex-1 lg:w-full font-medium"
 onClick={() => openApproveModal(req)}
 >
 <CheckCircle2 className="w-4 h-4 mr-1.5" />
 Approve
 </Button>
 <Button
 variant="secondary"
 size="sm"
 className="flex-1 lg:w-full font-medium text-[var(--danger)] hover:bg-[var(--danger)]/10 border-[var(--danger)]/40"
 onClick={() => openRejectModal(req)}
 >
 <XCircle className="w-4 h-4 mr-1.5" />
 Reject
 </Button>
 </div>
 </div>
 </Card>
 ))}
 </div>
 )}
 </section>

 {/* ───────────────────────────────────────────────────────────────── */}
 {/* SECTION 2: CURRENTLY GRANTED ACCESS */}
 {/* ───────────────────────────────────────────────────────────────── */}
 <section className="space-y-4">
 <div className="flex items-center justify-between">
 <div className="flex items-center gap-2">
 <h2 className="text-lg font-bold text-[var(--text-primary)]">
 Currently Granted Access
 </h2>
 {activeGrants.length > 0 && (
 <Badge variant="success" size="sm" className="font-semibold">
 {activeGrants.length} Active
 </Badge>
 )}
 </div>
 </div>

 {activeGrants.length === 0 ? (
 <Card className="p-6 text-center border border-[var(--border)] bg-[var(--bg-surface)]">
 <p className="text-sm text-[var(--text-secondary)]">
 No one currently has access to your certificates
 </p>
 </Card>
 ) : (
 <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
 {activeGrants?.map((grant) => (
 <Card
 key={grant.id}
 className="p-5 border border-[var(--border)] bg-[var(--bg-surface)] flex flex-col justify-between hover:shadow-sm transition-all"
 >
 <div className="space-y-3">
 <div className="flex items-start justify-between gap-3">
 <div className="flex items-start gap-3">
 <div className="w-10 h-10 rounded-lg bg-[var(--brand-light)] text-[var(--brand)] flex items-center justify-center shrink-0 border border-[var(--brand)]/20">
 <Building2 className="w-5 h-5" />
 </div>
 <div className="min-w-0">
 <h3 className="font-bold text-[var(--text-primary)] text-sm truncate">
 {grant.verifierCompany || grant.verifierName || 'Verifier'}
 </h3>
 {grant.verifierEmail && (
 <p className="text-xs text-[var(--text-muted)] truncate">
 {grant.verifierEmail}
 </p>
 )}
 </div>
 </div>
 <GrantDaysBadge daysRemaining={grant.daysRemaining} />
 </div>

 <div className="pt-2 border-t border-[var(--border)] space-y-1 text-xs text-[var(--text-secondary)]">
 <div className="flex items-center justify-between">
 <span className="text-[var(--text-muted)]">Scope:</span>
 {(grant.grantScope === 'specific' || grant.grantScope === 'specific_certificate' || !!grant.certificateId) ? (
 <Badge variant="primary" size="sm" className="flex items-center gap-1 font-medium truncate max-w-[200px]" title={`${grant.certificateName || 'Certificate'} (${grant.certificateSerial || ''})`}>
 <Award className="w-3 h-3 shrink-0" />
 <span className="truncate">{grant.certificateSerial || 'Single Cert'}</span>
 </Badge>
 ) : (
 <Badge variant="default" size="sm" className="flex items-center gap-1 font-medium">
 <GraduationCap className="w-3 h-3 shrink-0" />
 All Certificates
 </Badge>
 )}
 </div>
 {(grant.grantScope === 'specific' || grant.grantScope === 'specific_certificate' || !!grant.certificateId) && grant.certificateName && (
 <p className="text-[11px] text-[var(--text-muted)] truncate" title={grant.certificateName}>
 {grant.certificateName}
 </p>
 )}
 <p className="flex items-center justify-between">
 <span className="text-[var(--text-muted)]">Granted on:</span>
 <span className="font-medium text-[var(--text-primary)]">
 {grant.grantedAt ? formatDate(grant.grantedAt) : 'N/A'}
 </span>
 </p>
 <p className="flex items-center justify-between">
 <span className="text-[var(--text-muted)]">Expires on:</span>
 <span className="font-medium text-[var(--text-primary)]">
 {grant.expiresAt ? formatDate(grant.expiresAt) : 'N/A'}
 </span>
 </p>
 </div>
 </div>

 <div className="mt-4 pt-3 border-t border-[var(--border)] flex justify-end">
 <Button
 variant="secondary"
 size="sm"
 onClick={() => openRevokeModal(grant)}
 className="text-[var(--danger)] hover:bg-[var(--danger)]/10 border-[var(--danger)]/40 font-medium text-xs h-8"
 >
 <Trash2 className="w-3.5 h-3.5 mr-1.5" />
 Revoke Access
 </Button>
 </div>
 </Card>
 ))}
 </div>
 )}
 </section>

 {/* ───────────────────────────────────────────────────────────────── */}
 {/* SECTION 3: REQUEST HISTORY (COLLAPSIBLE) */}
 {/* ───────────────────────────────────────────────────────────────── */}
 <section className="space-y-4">
 <div
 className="flex items-center justify-between p-4 rounded-xl border border-[var(--border)] bg-[var(--bg-surface)] cursor-pointer hover:bg-[var(--bg-elevated)]/30 transition-colors select-none"
 onClick={() => setHistoryExpanded((prev) => !prev)}
 >
 <div className="flex items-center gap-2.5">
 <h2 className="text-base font-bold text-[var(--text-primary)]">
 Request History
 </h2>
 <Badge variant="default" size="sm">
 {historyRequests.length}
 </Badge>
 </div>
 <div className="text-[var(--text-muted)]">
 {historyExpanded ? (
 <ChevronUp className="w-5 h-5" />
 ) : (
 <ChevronDown className="w-5 h-5" />
 )}
 </div>
 </div>

 {historyExpanded && (
 <Card className="p-0 overflow-hidden border border-[var(--border)] shadow-sm">
 {historyRequests.length === 0 ? (
 <div className="p-6 text-center text-sm text-[var(--text-secondary)]">
 No historical access requests found.
 </div>
 ) : (
 <div className="overflow-x-auto">
 <table className="w-full text-left border-collapse text-xs sm:text-sm">
 <thead>
 <tr className="border-b border-[var(--border)] bg-[var(--bg-elevated)]/50 text-xs font-semibold uppercase tracking-wider text-[var(--text-secondary)]">
 <th className="py-3 px-4 sm:px-6">Company</th>
 <th className="py-3 px-4 sm:px-6">Scope</th>
 <th className="py-3 px-4 sm:px-6">Purpose</th>
 <th className="py-3 px-4 sm:px-6">Status</th>
 <th className="py-3 px-4 sm:px-6">Date</th>
 <th className="py-3 px-4 sm:px-6">Response</th>
 </tr>
 </thead>
 <tbody className="divide-y divide-[var(--border)]">
 {historyRequests?.map((item) => (
 <tr
 key={item.id}
 className="hover:bg-[var(--bg-elevated)]/20 transition-colors"
 >
 {/* Company */}
 <td className="py-3.5 px-4 sm:px-6 font-medium text-[var(--text-primary)]">
 <div>
 <p className="font-semibold">{item.verifierCompany || item.verifierName || 'Verifier'}</p>
 {item.verifierName && item.verifierCompany && (
 <p className="text-xs text-[var(--text-muted)]">{item.verifierName}</p>
 )}
 </div>
 </td>

 {/* Scope */}
 <td className="py-3.5 px-4 sm:px-6 whitespace-nowrap text-xs">
 {item.requestScope === 'specific' ? (
 <Badge variant="primary" size="sm" className="flex items-center gap-1 w-fit" title={`${item.certificateName || 'Certificate'} (${item.certificateSerial})`}>
 <Award className="w-3 h-3 shrink-0" />
 <span>{item.certificateSerial || 'Single Cert'}</span>
 </Badge>
 ) : (
 <Badge variant="default" size="sm" className="flex items-center gap-1 w-fit">
 <GraduationCap className="w-3 h-3 shrink-0" />
 <span>All Certs</span>
 </Badge>
 )}
 </td>

 {/* Purpose */}
 <td className="py-3.5 px-4 sm:px-6 text-[var(--text-secondary)] max-w-xs">
 <span
 className="inline-block truncate max-w-[200px] cursor-help"
 title={item.purpose}
 >
 {item.purpose || '—'}
 </span>
 </td>

 {/* Status */}
 <td className="py-3.5 px-4 sm:px-6 whitespace-nowrap">
 {item.status === 'approved' ? (
 <Badge variant="success" size="sm">
 Approved
 </Badge>
 ) : item.status === 'rejected' ? (
 <Badge variant="danger" size="sm">
 Rejected
 </Badge>
 ) : (
 <Badge variant="default" size="sm">
 {item.status || 'Cancelled'}
 </Badge>
 )}
 </td>

 {/* Date */}
 <td className="py-3.5 px-4 sm:px-6 text-[var(--text-secondary)] whitespace-nowrap text-xs">
 {item.respondedAt
 ? formatDate(item.respondedAt)
 : item.requestedAt
 ? formatDate(item.requestedAt)
 : '—'}
 </td>

 {/* Response Message */}
 <td className="py-3.5 px-4 sm:px-6 text-[var(--text-muted)] text-xs max-w-xs truncate">
 {item.responseMessage ? (
 <span title={item.responseMessage}>
 {item.responseMessage}
 </span>
 ) : (
 '—'
 )}
 </td>
 </tr>
 ))}
 </tbody>
 </table>
 </div>
 )}
 </Card>
 )}
 </section>
 </div>
 )}

 {/* ───────────────────────────────────────────────────────────────── */}
 {/* MODAL: APPROVE ACCESS */}
 {/* ───────────────────────────────────────────────────────────────── */}
 <Modal
 isOpen={approveModalOpen}
 onClose={() => !approving && setApproveModalOpen(false)}
 title="Approve Access Request"
 size="md"
 >
 {targetRequest && (
 <form onSubmit={handleConfirmApprove} className="space-y-4">
 {/* Verifier Details Top Card */}
 <div className="p-3.5 rounded-lg bg-[var(--bg-elevated)] border border-[var(--border)] space-y-2 text-xs sm:text-sm">
 <div className="flex items-center justify-between">
 <span className="font-semibold text-[var(--text-primary)]">
 {targetRequest.verifierCompany || targetRequest.verifierName}
 </span>
 {targetRequest.verifierEmail && (
 <span className="text-[var(--text-muted)] text-xs">
 {targetRequest.verifierEmail}
 </span>
 )}
 </div>
 <p className="text-xs text-[var(--text-secondary)] italic border-l-2 border-[var(--brand)] pl-2.5 my-1">
 "{targetRequest.purpose}"
 </p>
 </div>

 {/* Scope Notice */}
 {(targetRequest.requestScope === 'specific' || targetRequest.requestScope === 'specific_certificate' || !!targetRequest.certificateId) ? (
 <div className="p-3 rounded-lg bg-[var(--brand-light)]/40 border border-[var(--brand)]/30 text-xs text-[var(--text-primary)] space-y-1">
 <div className="flex items-center gap-1.5 font-semibold text-[var(--brand)]">
 <Award className="w-4 h-4" />
 <span>Single Certificate Access Only</span>
 </div>
 <p className="text-[var(--text-secondary)]">
 Approving will allow this verifier to view <strong>{targetRequest.certificateName || 'Specific Certificate'}</strong> (Serial: {targetRequest.certificateSerial || 'N/A'}). All your other certificates remain private.
 </p>
 </div>
 ) : (
 <div className="p-3 rounded-lg bg-[var(--bg-elevated)] border border-[var(--border)] text-xs text-[var(--text-secondary)] flex items-start gap-2">
 <GraduationCap className="w-4 h-4 text-[var(--brand)] shrink-0 mt-0.5" />
 <span>Approving will allow this verifier to view <strong>all</strong> your academic certificates for the specified duration.</span>
 </div>
 )}

 {/* Duration Input */}
 <div className="space-y-2">
 <div className="flex justify-between items-center text-sm">
 <label htmlFor="grant-duration" className="font-medium text-[var(--text-primary)]">
 Grant access for how many days? <span className="text-[var(--danger)]">*</span>
 </label>
 <span className="font-bold text-[var(--brand)] text-sm">
 {grantDuration} days
 </span>
 </div>
 <div className="flex items-center gap-3">
 <input
 id="grant-duration-slider"
 type="range"
 min="1"
 max="365"
 value={grantDuration}
 onChange={(e) => setGrantDuration(Number(e.target.value))}
 className="flex-1 accent-[var(--brand)] cursor-pointer"
 />
 <input
 id="grant-duration"
 type="number"
 min="1"
 max="365"
 value={grantDuration}
 onChange={(e) => {
 const val = Number(e.target.value);
 if (!isNaN(val)) setGrantDuration(val);
 }}
 className="w-20 h-9 px-2.5 text-center text-sm font-semibold rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] text-[var(--text-primary)] outline-none focus:border-[var(--brand)]"
 />
 </div>
 <div className="flex justify-between text-[11px] text-[var(--text-muted)] px-1">
 <span>1 day</span>
 <span>30 days</span>
 <span>90 days</span>
 <span>365 days</span>
 </div>
 </div>

 {/* Optional Response Message */}
 <div className="space-y-1.5 pt-1">
 <label htmlFor="approve-message" className="block text-sm font-medium text-[var(--text-primary)]">
 Response message <span className="text-xs font-normal text-[var(--text-muted)]">(optional)</span>
 </label>
 <textarea
 id="approve-message"
 rows={3}
 value={approveMessage}
 onChange={(e) => setApproveMessage(e.target.value)}
 placeholder="Thanks for your request. Access has been granted."
 className="w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] p-3 text-sm text-[var(--text-primary)] outline-none focus:border-[var(--brand)] resize-none transition-all"
 />
 </div>

 {/* Action Buttons */}
 <div className="pt-4 flex justify-end gap-2 border-t border-[var(--border)]">
 <Button
 type="button"
 variant="secondary"
 size="md"
 onClick={() => setApproveModalOpen(false)}
 disabled={approving}
 >
 Cancel
 </Button>
 <Button
 type="submit"
 variant="success"
 size="md"
 loading={approving}
 disabled={approving || grantDuration < 1}
 >
 Approve Access
 </Button>
 </div>
 </form>
 )}
 </Modal>

 {/* ───────────────────────────────────────────────────────────────── */}
 {/* MODAL: REJECT ACCESS */}
 {/* ───────────────────────────────────────────────────────────────── */}
 <Modal
 isOpen={rejectModalOpen}
 onClose={() => !rejecting && setRejectModalOpen(false)}
 title="Reject Access Request"
 size="md"
 >
 {targetRequest && (
 <form onSubmit={handleConfirmReject} className="space-y-4">
 <p className="text-xs text-[var(--text-secondary)]">
 You are about to decline certificate access for{' '}
 <strong className="text-[var(--text-primary)]">
 {targetRequest.verifierCompany || targetRequest.verifierName || 'this verifier'}
 </strong>.
 </p>

 {/* Rejection Reason Textarea */}
 <div className="space-y-1.5">
 <div className="flex justify-between items-center text-sm">
 <label htmlFor="reject-reason" className="font-medium text-[var(--text-primary)]">
 Reason for rejection <span className="text-[var(--danger)]">*</span>
 </label>
 <span className={`text-xs ${rejectReason.trim().length >= 10 ? 'text-[var(--text-muted)]' : 'text-[var(--danger)]'}`}>
 {rejectReason.trim().length} / min 10 chars
 </span>
 </div>
 <textarea
 id="reject-reason"
 rows={4}
 value={rejectReason}
 onChange={(e) => setRejectReason(e.target.value)}
 placeholder="Explain why you are declining access (e.g. Unrecognized verifier, request not authorized)..."
 className="w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] p-3 text-sm text-[var(--text-primary)] outline-none focus:border-[var(--danger)] resize-none transition-all"
 required
 />
 {rejectReason.trim().length > 0 && rejectReason.trim().length < 10 && (
 <p className="text-xs text-[var(--danger)]">
 Reason must be at least 10 characters.
 </p>
 )}
 </div>

 {/* Action Buttons */}
 <div className="pt-4 flex justify-end gap-2 border-t border-[var(--border)]">
 <Button
 type="button"
 variant="secondary"
 size="md"
 onClick={() => setRejectModalOpen(false)}
 disabled={rejecting}
 >
 Cancel
 </Button>
 <Button
 type="submit"
 variant="danger"
 size="md"
 loading={rejecting}
 disabled={rejecting || rejectReason.trim().length < 10}
 >
 Reject Request
 </Button>
 </div>
 </form>
 )}
 </Modal>

 {/* ───────────────────────────────────────────────────────────────── */}
 {/* MODAL: REVOKE ACTIVE GRANT CONFIRMATION */}
 {/* ───────────────────────────────────────────────────────────────── */}
 <ConfirmModal
 isOpen={revokeModalOpen}
 onClose={() => !revoking && setRevokeModalOpen(false)}
 onConfirm={handleConfirmRevoke}
 title="Revoke Access"
 message="This will immediately remove their access. They will no longer be able to view your certificates."
 confirmText="Revoke Access"
 confirmVariant="danger"
 isLoading={revoking}
 />
 </div>
 </DashboardLayout>
 );
}
