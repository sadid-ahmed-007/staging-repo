import { useEffect, useState, useCallback } from 'react';
import toast from 'react-hot-toast';
import {
 GraduationCap,
 AlertCircle,
 Clock,
 Building2,
 CheckCircle2,
 XCircle,
 BookOpen,
 FileText,
 Calendar,
 AlertTriangle,
 School,
 Send
} from 'lucide-react';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/shared/Card';
import Button from '../../components/shared/Button';
import Badge from '../../components/shared/Badge';
import LoadingSpinner from '../../components/shared/LoadingSpinner';
import ConfirmModal from '../../components/shared/ConfirmModal';
import api from '../../services/api';
import { formatDate, cn } from '../../utils/helpers';

export default function MyUniversity() {
 const [loading, setLoading] = useState(true);
 const [enrollment, setEnrollment] = useState(null);
 const [withdrawalStatus, setWithdrawalStatus] = useState(null);

 // Form & Confirm Modal state
 const [withdrawalReason, setWithdrawalReason] = useState('');
 const [showConfirmModal, setShowConfirmModal] = useState(false);
 const [isSubmittingWithdrawal, setIsSubmittingWithdrawal] = useState(false);
 const [showCancelModal, setShowCancelModal] = useState(false);
 const [isCancellingWithdrawal, setIsCancellingWithdrawal] = useState(false);

 const loadData = useCallback(async () => {
 setLoading(true);
 try {
 const [enrollmentRes, statusRes] = await Promise.all([
 api.get('/student/enrollment').catch(() => ({ data: { data: null } })),
 api.get('/student/enrollment/withdrawal-status').catch(() => ({ data: { data: null } })),
 ]);

 if (enrollmentRes.data?.success && enrollmentRes.data?.data) {
 setEnrollment(enrollmentRes.data.data);
 } else {
 setEnrollment(null);
 }

 if (statusRes.data?.success && statusRes.data?.data?.hasRequest) {
 setWithdrawalStatus(statusRes.data.data);
 } else {
 setWithdrawalStatus(null);
 }
 } catch (err) {
 console.error('Failed to load university information:', err);
 toast.error('Failed to load university information.');
 } finally {
 setLoading(false);
 }
 }, []);

 useEffect(() => {
 loadData();
 }, [loadData]);

 const handleConfirmWithdrawal = async () => {
 if (!enrollment || withdrawalReason.trim().length < 20) {
 toast.error('Withdrawal reason must be at least 20 characters');
 return;
 }

 setIsSubmittingWithdrawal(true);
 try {
 const response = await api.post(
 `/student/enrollment/${enrollment.id}/request-withdrawal`,
 { reason: withdrawalReason.trim() }
 );

 if (response.data.success) {
 toast.success(response.data.message || 'Withdrawal request submitted successfully');
 setShowConfirmModal(false);
 setWithdrawalReason('');
 await loadData();
 }
 } catch (err) {
 console.error('Failed to request withdrawal:', err);
 toast.error(err.response?.data?.message || 'Failed to submit withdrawal request');
 } finally {
 setIsSubmittingWithdrawal(false);
 }
 };

 const handleCancelWithdrawal = async () => {
 setIsCancellingWithdrawal(true);
 try {
 const response = await api.delete('/student/enrollment/withdrawal-request');
 if (response.data.success) {
 toast.success(response.data.message || 'Withdrawal request cancelled successfully');
 setShowCancelModal(false);
 await loadData();
 }
 } catch (err) {
 console.error('Failed to cancel withdrawal request:', err);
 toast.error(err.response?.data?.message || 'Failed to cancel withdrawal request');
 } finally {
 setIsCancellingWithdrawal(false);
 }
 };

 if (loading) {
 return (
 <DashboardLayout>
 <div className="flex min-h-[50vh] items-center justify-center">
 <LoadingSpinner />
 </div>
 </DashboardLayout>
 );
 }

 // Not currently enrolled: center empty state on the page
 if (!enrollment) {
 return (
 <DashboardLayout>
 <div className="space-y-6">
 <div>
 <h1 className="text-2xl font-bold text-[var(--text-primary)]">My University</h1>
 <p className="text-sm text-[var(--text-secondary)] mt-1">
 View your academic enrollment status and university details.
 </p>
 </div>

 <div className="flex min-h-[55vh] items-center justify-center">
 <div className="max-w-md w-full rounded-2xl border border-dashed border-[var(--border)] bg-[var(--bg-surface)] p-10 text-center shadow-sm">
 <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600 /40 ">
 <GraduationCap className="h-8 w-8" />
 </div>
 <h2 className="mt-4 text-lg font-bold text-[var(--text-primary)]">
 You are not currently enrolled
 </h2>
 <p className="mt-2 text-sm text-[var(--text-secondary)]">
 You do not have an active enrollment at any university. Once an institution enrolls you, your full academic record, progress, and status will appear here.
 </p>
 </div>
 </div>
 </div>
 </DashboardLayout>
 );
 }

 const isWithdrawalPending =
 enrollment.status === 'withdrawal_requested' || withdrawalStatus?.status === 'pending';
 const isApproved =
 enrollment.status === 'withdrawn' || withdrawalStatus?.status === 'approved';
 const isRejected =
 withdrawalStatus?.status === 'rejected' && enrollment.status === 'active';

 return (
 <DashboardLayout>
 <div className="space-y-6 pb-12">
 {/* Page Header */}
 <div className="flex items-center justify-between">
 <div>
 <h1 className="text-2xl font-bold text-[var(--text-primary)]">My University</h1>
 <p className="text-sm text-[var(--text-secondary)] mt-1">
 Manage your academic enrollment, view status, and request withdrawal.
 </p>
 </div>
 </div>

 {/* ================================================================= */}
 {/* SECTION 1: CURRENT ENROLLMENT CARD */}
 {/* ================================================================= */}
 <Card className="shadow-sm border-[var(--border)]">
 <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4 pb-6 border-b border-[var(--border)]">
 <div className="space-y-1">
 <div className="flex flex-wrap items-center gap-3">
 <h2 className="text-xl font-bold text-[var(--text-primary)] sm:text-2xl">
 {enrollment.institutionName}
 </h2>
 {enrollment.enrollmentNumber && (
 <span className="rounded-md border border-[var(--brand)]/20 bg-[var(--brand-light)]/40 px-2.5 py-0.5 font-mono text-xs font-semibold text-[var(--brand)] dark:bg-[var(--brand-light)]/10">
 {enrollment.enrollmentNumber}
 </span>
 )}
 </div>
 <p className="text-sm font-medium text-[var(--text-secondary)]">
 {enrollment.program}
 </p>
 </div>

 {/* Status Badge */}
 <div>
 {enrollment.status === 'active' ? (
 <Badge variant="success" dot size="lg">
 Active
 </Badge>
 ) : isWithdrawalPending ? (
 <span className="inline-flex items-center gap-1.5 rounded-full border border-amber-300 bg-amber-50 px-3.5 py-1 text-xs font-semibold text-amber-700 /40 /40 ">
 <span className="relative flex h-2 w-2">
 <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-amber-400 opacity-75"></span>
 <span className="relative inline-flex h-2 w-2 rounded-full bg-amber-500"></span>
 </span>
 Withdrawal Pending
 </span>
 ) : enrollment.status === 'withdrawn' ? (
 <Badge variant="danger" dot size="lg">
 Withdrawn
 </Badge>
 ) : enrollment.status === 'graduated' ? (
 <Badge variant="primary" dot size="lg">
 Graduated
 </Badge>
 ) : (
 <Badge variant="default" dot size="lg">
 {enrollment.status?.toUpperCase()}
 </Badge>
 )}
 </div>
 </div>

 {/* Details Grid */}
 <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 pt-6 text-sm">
 <div>
 <p className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">
 Department
 </p>
 <p className="mt-1 font-medium text-[var(--text-primary)]">
 {enrollment.department || '—'}
 </p>
 </div>

 <div>
 <p className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">
 Major
 </p>
 <p className="mt-1 font-medium text-[var(--text-primary)]">
 {enrollment.major || 'None'}
 </p>
 </div>

 <div>
 <p className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">
 Batch
 </p>
 <p className="mt-1 font-medium text-[var(--text-primary)]">
 {enrollment.batch || '—'}
 </p>
 </div>

 <div>
 <p className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">
 Assigned Student ID
 </p>
 <p className="mt-1 font-medium text-[var(--text-primary)]">
 {enrollment.studentIdInUniversity || (
 <span className="text-[var(--text-muted)] italic">Not assigned yet</span>
 )}
 </p>
 </div>

 <div>
 <p className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">
 Enrollment Date
 </p>
 <p className="mt-1 font-medium text-[var(--text-primary)]">
 {formatDate(enrollment.enrollmentDate)}
 </p>
 </div>

 <div>
 <p className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">
 Expected Graduation
 </p>
 <p className="mt-1 font-medium text-[var(--text-primary)]">
 {formatDate(enrollment.expectedGraduationDate)}
 </p>
 </div>

 {enrollment.actualGraduationDate && (
 <div>
 <p className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">
 {enrollment.status === 'withdrawn' ? 'Withdrawn On' : 'Graduated On'}
 </p>
 <p className="mt-1 font-medium text-[var(--text-primary)]">
 {formatDate(enrollment.actualGraduationDate)}
 </p>
 </div>
 )}
 </div>
 </Card>

 {/* ================================================================= */}
 {/* SECTION 3: WITHDRAWAL STATUS */}
 {/* ================================================================= */}
 {/* Pending Card */}
 {isWithdrawalPending && (
 <div className="rounded-xl border border-amber-300 bg-amber-50/70 p-5 /40 /20 shadow-sm">
 <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
 <div className="flex items-center gap-2.5 text-amber-800 ">
 <Clock className="h-5 w-5 shrink-0 text-amber-600 " />
 <h3 className="text-base font-bold">
 Your withdrawal request is pending university review
 </h3>
 </div>
 <Button
 variant="outline"
 size="sm"
 onClick={() => setShowCancelModal(true)}
 className="border-amber-400 bg-[var(--bg-surface)]/80 text-amber-900 hover:bg-amber-100 /40 dark:hover:bg-amber-900/60 shadow-xs shrink-0 self-start sm:self-auto"
 >
 Cancel Request
 </Button>
 </div>
 <div className="mt-3 rounded-lg border border-amber-200/80 bg-[var(--bg-surface)]/70 p-3.5 /40 /10">
 <p className="text-xs font-semibold text-amber-900 ">
 Reason submitted:
 </p>
 <p className="mt-1 text-xs italic text-[var(--text-secondary)]">
 "{withdrawalStatus?.reason || enrollment.withdrawalReason || 'No details provided.'}"
 </p>
 </div>
 <div className="mt-3 flex flex-wrap items-center justify-between gap-2">
 <p className="text-xs font-medium text-amber-700 ">
 Submitted on: {formatDate(withdrawalStatus?.createdAt || enrollment.withdrawalRequestedAt)}
 </p>
 <span className="text-xs text-amber-700/80 /80">
 You can cancel this request at any time before the university reviews it.
 </span>
 </div>
 </div>
 )}

 {/* Approved Card */}
 {isApproved && (
 <div className="rounded-xl border border-emerald-300 bg-emerald-50/70 p-5 /40 /20 shadow-sm">
 <div className="flex items-center gap-2.5 text-emerald-800 ">
 <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-600 " />
 <h3 className="text-base font-bold">
 Your withdrawal has been approved
 </h3>
 </div>
 {withdrawalStatus?.rejectionNote && (
 <div className="mt-3 rounded-lg border border-emerald-200/80 bg-[var(--bg-surface)]/70 p-3.5 /40 /10">
 <p className="text-xs font-semibold text-emerald-900 ">
 Response from university:
 </p>
 <p className="mt-1 text-xs italic text-[var(--text-secondary)]">
 "{withdrawalStatus.rejectionNote}"
 </p>
 </div>
 )}
 <p className="mt-3 text-xs font-medium text-emerald-700 ">
 Enrollment status is now marked as <strong>Withdrawn</strong>.
 {withdrawalStatus?.reviewedAt && ` • Reviewed on: ${formatDate(withdrawalStatus.reviewedAt)}`}
 </p>
 </div>
 )}

 {/* Rejected Card */}
 {isRejected && (
 <div className="rounded-xl border border-rose-300 bg-rose-50/70 p-5 /40 /20 shadow-sm">
 <div className="flex items-center gap-2.5 text-rose-800 ">
 <XCircle className="h-5 w-5 shrink-0 text-rose-600 " />
 <h3 className="text-base font-bold">
 Your withdrawal request was rejected
 </h3>
 </div>
 {withdrawalStatus?.rejectionNote && (
 <div className="mt-3 rounded-lg border border-rose-200/80 bg-[var(--bg-surface)]/70 p-3.5 /40 /10">
 <p className="text-xs font-semibold text-rose-900 ">
 Response message from university:
 </p>
 <p className="mt-1 text-xs italic text-[var(--text-secondary)]">
 "{withdrawalStatus.rejectionNote}"
 </p>
 </div>
 )}
 <p className="mt-3 text-xs font-medium text-rose-700 ">
 Your enrollment remains <strong>Active</strong>. You may submit a new request if needed.
 {withdrawalStatus?.reviewedAt && ` • Reviewed on: ${formatDate(withdrawalStatus.reviewedAt)}`}
 </p>
 </div>
 )}

 {/* ================================================================= */}
 {/* SECTION 2: WITHDRAWAL REQUEST (Only if status = active) */}
 {/* ================================================================= */}
 {enrollment.status === 'active' && !isWithdrawalPending && (
 <Card className="shadow-sm border-[var(--border)]">
 <div className="border-b border-[var(--border)] pb-4">
 <h3 className="text-lg font-bold text-[var(--text-primary)]">
 Request Withdrawal
 </h3>
 <p className="text-xs text-[var(--text-secondary)] mt-1">
 Submitting a withdrawal request initiates the formal process to discontinue your studies at this institution. Your request will be sent to the university administration for review.
 </p>
 </div>

 <div className="pt-5 space-y-4">
 <div>
 <div className="flex items-center justify-between mb-1.5">
 <label className="text-xs font-semibold text-[var(--text-secondary)]">
 Reason for withdrawal <span className="text-rose-500">*</span>
 </label>
 <span
 className={cn(
 'text-[11px] font-medium',
 withdrawalReason.trim().length < 20
 ? 'text-rose-500'
 : 'text-emerald-600 '
 )}
 >
 {withdrawalReason.trim().length} / 20 min characters
 </span>
 </div>
 <textarea
 rows={4}
 value={withdrawalReason}
 onChange={(e) => setWithdrawalReason(e.target.value)}
 placeholder="Please state your reasons for requesting withdrawal in detail (min 20 characters)..."
 className="w-full rounded-xl border border-[var(--border)] bg-[var(--bg-surface)] p-3 text-sm text-[var(--text-primary)] outline-none transition-all focus:border-[var(--brand)] focus:ring-2 focus:ring-[var(--brand)]/10"
 />
 </div>

 <div>
 <Button
 variant="secondary"
 onClick={() => setShowConfirmModal(true)}
 disabled={withdrawalReason.trim().length < 20}
 className="border-[var(--border-strong)] text-[var(--text-primary)] hover:border-rose-400 hover:text-rose-600"
 >
 <Send className="mr-2 h-4 w-4" />
 Submit Withdrawal Request
 </Button>
 </div>
 </div>
 </Card>
 )}

 {/* Confirm Dialog Modal */}
 <ConfirmModal
 isOpen={showConfirmModal}
 onClose={() => setShowConfirmModal(false)}
 onConfirm={handleConfirmWithdrawal}
 title="Confirm Withdrawal Request"
 message="Are you sure? Your enrollment will be marked as pending withdrawal. The university will review and respond."
 confirmText="Confirm Request"
 confirmVariant="warning"
 isDestructive={false}
 loading={isSubmittingWithdrawal}
 />

 {/* Cancel Withdrawal Confirmation Modal */}
 <ConfirmModal
 isOpen={showCancelModal}
 onClose={() => setShowCancelModal(false)}
 onConfirm={handleCancelWithdrawal}
 title="Cancel Withdrawal Request"
 message="Are you sure you want to cancel your withdrawal request? The request will be withdrawn and your enrollment will remain active."
 confirmText="Yes, Cancel Request"
 confirmVariant="danger"
 isDestructive={true}
 loading={isCancellingWithdrawal}
 />
 </div>
 </DashboardLayout>
 );
}
