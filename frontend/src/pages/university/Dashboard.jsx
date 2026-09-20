import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
 Users,
 GraduationCap,
 Award,
 AlertTriangle,
 Calendar,
 UserPlus,
 UserMinus,
 CheckCircle,
 XCircle,
 Clock,
 ArrowRight,
 Layers,
 ExternalLink,
 PlusCircle,
 ShieldCheck,
} from 'lucide-react';
import DashboardLayout from '../../components/layout/DashboardLayout';
import StatCard from '../../components/shared/StatCard';
import LoadingSpinner from '../../components/shared/LoadingSpinner';
import Card from '../../components/shared/Card';
import Badge from '../../components/shared/Badge';
import Button from '../../components/shared/Button';
import Modal from '../../components/shared/Modal';
import api from '../../services/api';
import { useAuth } from '../../contexts/AuthContext';
import { cn } from '../../utils/helpers';

function formatRelativeTime(dateString) {
 if (!dateString) return '';
 const date = new Date(dateString);
 const now = new Date();
 const diffInSeconds = Math.floor((now - date) / 1000);

 if (diffInSeconds < 60) return 'Just now';
 const diffInMinutes = Math.floor(diffInSeconds / 60);
 if (diffInMinutes < 60) return `${diffInMinutes}m ago`;
 const diffInHours = Math.floor(diffInMinutes / 60);
 if (diffInHours < 24) return `${diffInHours} ${diffInHours === 1 ? 'hour' : 'hours'} ago`;
 const diffInDays = Math.floor(diffInHours / 24);
 if (diffInDays < 30) return `${diffInDays} ${diffInDays === 1 ? 'day' : 'days'} ago`;
 const diffInMonths = Math.floor(diffInDays / 30);
 if (diffInMonths < 12) return `${diffInMonths} ${diffInMonths === 1 ? 'month' : 'months'} ago`;
 const diffInYears = Math.floor(diffInMonths / 12);
 return `${diffInYears} ${diffInYears === 1 ? 'year' : 'years'} ago`;
}

function getActivityConfig(action) {
 const norm = (action || '').toUpperCase();
 if (norm.includes('CERTIFICATE_ISSUED') || norm.includes('CERTIFICATE_CREATE')) {
 return {
 icon: <Award className="h-4 w-4 text-emerald-600 " />,
 bg: 'bg-emerald-500/10 border-emerald-500/20 text-emerald-600 ',
 label: 'Certificate Issued',
 };
 }
 if (norm.includes('STUDENT_ENROLLED') || norm.includes('ENROLLMENT_CREATED')) {
 return {
 icon: <UserPlus className="h-4 w-4 text-blue-600 " />,
 bg: 'bg-blue-500/10 border-blue-500/20 text-blue-600 ',
 label: 'Student Enrolled',
 };
 }
 if (norm.includes('STUDENT_WITHDRAWN') || norm.includes('WITHDRAWAL_REQUESTED')) {
 return {
 icon: <UserMinus className="h-4 w-4 text-rose-600 " />,
 bg: 'bg-rose-500/10 border-rose-500/20 text-rose-600 ',
 label: 'Student Withdrawn',
 };
 }
 if (norm.includes('GRADUATION_DATE_EXTENDED') || norm.includes('DATE_EXTENDED')) {
 return {
 icon: <Calendar className="h-4 w-4 text-amber-600 " />,
 bg: 'bg-amber-500/10 border-amber-500/20 text-amber-600 ',
 label: 'Graduation Extended',
 };
 }
 if (norm.includes('WITHDRAWAL_APPROVED')) {
 return {
 icon: <CheckCircle className="h-4 w-4 text-emerald-600 " />,
 bg: 'bg-emerald-500/10 border-emerald-500/20 text-emerald-600 ',
 label: 'Withdrawal Approved',
 };
 }
 if (norm.includes('WITHDRAWAL_REJECTED')) {
 return {
 icon: <XCircle className="h-4 w-4 text-rose-600 " />,
 bg: 'bg-rose-500/10 border-rose-500/20 text-rose-600 ',
 label: 'Withdrawal Rejected',
 };
 }
 return {
 icon: <Clock className="h-4 w-4 text-[var(--brand)]" />,
 bg: 'bg-[var(--brand)]/10 border-[var(--brand)]/20 text-[var(--brand)]',
 label: norm.replace(/_/g, ' '),
 };
}

export default function UniversityDashboard() {
 const [stats, setStats] = useState(null);
 const [activities, setActivities] = useState([]);
 const [loading, setLoading] = useState(true);
 const [loadingActivities, setLoadingActivities] = useState(true);
 const [allActivityModalOpen, setAllActivityModalOpen] = useState(false);
 const { user } = useAuth();

 useEffect(() => {
 const fetchDashboard = async () => {
 try {
 const { data } = await api.get('/university/dashboard');
 setStats(data.data || data);
 } catch (error) {
 console.error('Failed to fetch dashboard stats:', error);
 } finally {
 setLoading(false);
 }
 };

 const fetchActivities = async () => {
 try {
 const { data } = await api.get('/university/dashboard/activity');
 const list = data.activities || data.activity || data.data || [];
 setActivities(list);
 } catch (error) {
 console.error('Failed to fetch dashboard activity:', error);
 } finally {
 setLoadingActivities(false);
 }
 };

 fetchDashboard();
 fetchActivities();
 }, []);

 if (loading) {
 return (
 <DashboardLayout>
 <div className="flex min-h-[50vh] items-center justify-center">
 <LoadingSpinner />
 </div>
 </DashboardLayout>
 );
 }

 return (
 <DashboardLayout>
 <div className="space-y-6 pb-10">
 {/* Header with Quick Actions */}
 <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
 <div>
 <h1 className="text-2xl font-bold text-[var(--text-primary)]">
 University Dashboard
 </h1>
 <p className="text-sm text-[var(--text-secondary)] mt-1">
 Welcome back{user?.name ? `, ${user.name}` : ''} • Manage academic enrollments &amp; certificates
 </p>
 </div>

 <div className="flex items-center gap-2.5">
 <Link to="/university/issue-certificate">
 <Button variant="primary" size="sm" icon={<Award className="h-4 w-4" />}>
 Issue Certificate
 </Button>
 </Link>
 <Link to="/university/enrollments">
 <Button variant="secondary" size="sm" icon={<UserPlus className="h-4 w-4" />}>
 Enroll Student
 </Button>
 </Link>
 </div>
 </div>

 {/* SECTION 3: Pending Actions Banner (Full Width) */}
 {stats?.pendingWithdrawals > 0 && (
 <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-amber-300 bg-amber-50 /50 /40 p-4 shadow-sm transition-all animate-fadeIn">
 <div className="flex items-center gap-3">
 <span className="inline-flex items-center gap-1.5 rounded-full bg-amber-500/20 px-2.5 py-1 text-xs font-bold text-amber-800 border border-amber-500/30">
 <AlertTriangle className="h-3.5 w-3.5" />
 <span>Action Needed</span>
 </span>
 <div>
 <p className="text-sm font-semibold text-amber-900 ">
 {stats.pendingWithdrawals} withdrawal request{stats.pendingWithdrawals > 1 ? 's' : ''} pending review
 </p>
 <p className="text-xs text-amber-700 mt-0.5">
 Students have submitted withdrawal requests that require institution review and decision.
 </p>
 </div>
 </div>
 <Link
 to="/university/enrollments?status=withdrawal_requested"
 className="inline-flex items-center gap-1.5 rounded-lg bg-amber-600 px-3.5 py-1.5 text-xs font-semibold text-white transition-colors hover:bg-amber-700 dark:hover:bg-amber-600 shadow-sm"
 >
 <span>Review Requests</span>
 <ArrowRight className="h-3.5 w-3.5" />
 </Link>
 </div>
 )}

 {/* 5 Stat Cards */}
 <div className="grid gap-4 grid-cols-1 sm:grid-cols-2 lg:grid-cols-5">
 <StatCard
 icon={<Users className="h-5 w-5" />}
 label="Total Enrolled"
 value={stats?.totalEnrolled ?? 0}
 color="blue"
 to="/university/enrollments?status=active"
 />
 <StatCard
 icon={<GraduationCap className="h-5 w-5" />}
 label="Graduated Students"
 value={stats?.graduatedStudents ?? 0}
 color="green"
 to="/university/enrollments?status=graduated"
 />
 <StatCard
 icon={<Award className="h-5 w-5" />}
 label="Certificates Issued"
 value={stats?.certificatesIssued ?? 0}
 color="purple"
 to="/university/certificates"
 />
 <StatCard
 icon={<AlertTriangle className="h-5 w-5" />}
 label="Pending Withdrawals"
 value={stats?.pendingWithdrawals ?? 0}
 color="orange"
 pulse={(stats?.pendingWithdrawals ?? 0) > 0}
 to="/university/enrollments?status=withdrawal_requested"
 />
 <StatCard
 icon={<Calendar className="h-5 w-5" />}
 label="This Month's Certificates"
 value={stats?.thisMonthCertificates ?? 0}
 color="blue"
 to="/university/certificates"
 />
 </div>

 {/* Split Section: 60% Left (Recent Activity) / 40% Right (Program Overview) */}
 <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
 {/* SECTION 1: Recent Activity Feed (left column, 60% -> lg:col-span-7) */}
 <div className="lg:col-span-7">
 <Card className="p-5 shadow-sm space-y-4">
 <div className="flex items-center justify-between border-b border-[var(--border)] pb-3">
 <div className="flex items-center gap-2.5">
 <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-[var(--brand-light)] text-[var(--brand)]">
 <Clock className="h-4 w-4" />
 </span>
 <div>
 <h2 className="text-base font-bold text-[var(--text-primary)]">
 Recent Activity
 </h2>
 <p className="text-xs text-[var(--text-muted)]">
 Chronological log of academic events &amp; administrative actions
 </p>
 </div>
 </div>
 <Badge variant="default" size="sm">
 {activities.length} Recent
 </Badge>
 </div>

 {loadingActivities ? (
 <div className="flex flex-col items-center justify-center py-10">
 <LoadingSpinner size="md" />
 <p className="mt-2 text-xs text-[var(--text-secondary)]">Loading activity feed...</p>
 </div>
 ) : activities.length > 0 ? (
 <div className="divide-y divide-[var(--border)]">
 {activities.slice(0, 10).map((act, index) => {
 const cfg = getActivityConfig(act.action);
 return (
 <div
 key={act.id || index}
 className="flex items-start gap-3 py-3 first:pt-1 last:pb-1 group transition-colors hover:bg-[var(--bg-elevated)]/40 rounded-lg px-2"
 >
 <div
 className={cn(
 'mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border',
 cfg.bg
 )}
 >
 {cfg.icon}
 </div>
 <div className="flex-1 min-w-0">
 <p className="text-xs sm:text-sm text-[var(--text-primary)] leading-snug">
 {act.description || act.action}
 </p>
 <p className="mt-1 text-[11px] text-[var(--text-muted)] flex items-center gap-1.5">
 <Clock className="h-3 w-3" />
 <span>{formatRelativeTime(act.createdAt || act.created_at)}</span>
 </p>
 </div>
 </div>
 );
 })}
 </div>
 ) : (
 <div className="rounded-xl border border-dashed border-[var(--border)] py-10 px-4 text-center">
 <Clock className="mx-auto h-8 w-8 text-[var(--text-muted)] opacity-50" />
 <p className="mt-2 text-sm font-semibold text-[var(--text-primary)]">
 No recent activity
 </p>
 <p className="mt-1 text-xs text-[var(--text-muted)]">
 Issuances, enrollments, and status updates will appear here automatically.
 </p>
 </div>
 )}

 {activities.length > 0 && (
 <div className="pt-2 border-t border-[var(--border)] flex justify-center">
 <button
 type="button"
 onClick={() => setAllActivityModalOpen(true)}
 className="inline-flex items-center gap-1.5 text-xs font-semibold text-[var(--brand)] hover:underline"
 >
 <span>View all activity</span>
 <ArrowRight className="h-3.5 w-3.5" />
 </button>
 </div>
 )}
 </Card>
 </div>

 {/* SECTION 2: Quick Stats Summary / Program Overview (right column, 40% -> lg:col-span-5) */}
 <div className="lg:col-span-5">
 <Card className="p-5 shadow-sm space-y-4">
 <div className="flex items-center justify-between border-b border-[var(--border)] pb-3">
 <div className="flex items-center gap-2.5">
 <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-500/10 text-indigo-600 ">
 <Layers className="h-4 w-4" />
 </span>
 <div>
 <h2 className="text-base font-bold text-[var(--text-primary)]">
 Program Overview
 </h2>
 <p className="text-xs text-[var(--text-muted)]">
 Active students &amp; certificates per degree level
 </p>
 </div>
 </div>
 <Link
 to="/university/program-settings"
 className="text-xs font-semibold text-[var(--brand)] hover:underline flex items-center gap-1"
 >
 <span>Manage</span>
 <ExternalLink className="h-3 w-3" />
 </Link>
 </div>

 {stats?.programBreakdown && stats.programBreakdown.length > 0 ? (
 <div className="space-y-2.5">
 {stats.programBreakdown?.map((item, idx) => (
 <div
 key={item.shortName || idx}
 className="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 rounded-xl border border-[var(--border)] bg-[var(--bg-elevated)]/50 p-3.5 transition-all hover:border-[var(--brand)]/40 hover:bg-[var(--bg-elevated)]"
 >
 <div className="flex items-center gap-2.5 min-w-0">
 <span className="inline-flex items-center justify-center rounded-lg bg-[var(--brand-light)] px-2.5 py-1 text-xs font-bold text-[var(--brand)] shrink-0">
 {item.shortName || item.levelName}
 </span>
 <span className="text-xs sm:text-sm font-semibold text-[var(--text-primary)] truncate">
 {item.levelName}
 </span>
 </div>

 <div className="flex items-center gap-3 shrink-0 text-xs">
 <div className="flex items-center gap-1.5 text-[var(--text-secondary)]">
 <Users className="h-3.5 w-3.5 text-blue-500" />
 <span>
 <strong className="font-bold text-[var(--text-primary)]">
 {item.activeStudents}
 </strong>{' '}
 active
 </span>
 </div>
 <div className="flex items-center gap-1.5 text-[var(--text-secondary)]">
 <Award className="h-3.5 w-3.5 text-emerald-500" />
 <span>
 <strong className="font-bold text-[var(--text-primary)]">
 {item.certsIssued}
 </strong>{' '}
 issued
 </span>
 </div>
 </div>
 </div>
 ))}
 </div>
 ) : (
 <div className="rounded-xl border border-dashed border-[var(--border)] py-8 px-4 text-center">
 <Layers className="mx-auto h-7 w-7 text-[var(--text-muted)] opacity-50" />
 <p className="mt-2 text-xs font-semibold text-[var(--text-primary)]">
 No active academic levels configured
 </p>
 <p className="mt-1 text-[11px] text-[var(--text-muted)]">
 Define certificate levels, departments, and programs for this university.
 </p>
 <Link
 to="/university/program-settings"
 className="mt-3 inline-flex items-center gap-1 rounded-lg bg-[var(--brand-light)] px-3 py-1.5 text-xs font-semibold text-[var(--brand)] hover:bg-[var(--brand-light)]/80 transition"
 >
 Configure Programs &rarr;
 </Link>
 </div>
 )}

 {/* Summary totals footer in card */}
 {stats?.programBreakdown && stats.programBreakdown.length > 0 && (
 <div className="pt-3 border-t border-[var(--border)] flex items-center justify-between text-xs text-[var(--text-muted)]">
 <span>{stats.programBreakdown.length} active level{stats.programBreakdown.length > 1 ? 's' : ''}</span>
 <Link
 to="/university/program-settings"
 className="font-semibold text-[var(--brand)] hover:underline flex items-center gap-1"
 >
 <span>View Academic Structure</span>
 <ArrowRight className="h-3 w-3" />
 </Link>
 </div>
 )}
 </Card>
 </div>
 </div>

 {/* Modal: View All Activity */}
 <Modal
 isOpen={allActivityModalOpen}
 onClose={() => setAllActivityModalOpen(false)}
 title="All Recent Activity"
 size="lg"
 >
 <div className="space-y-4">
 <p className="text-xs text-[var(--text-secondary)]">
 Complete audit log of recent administrative actions performed by your institution.
 </p>

 <div className="max-h-[420px] overflow-y-auto divide-y divide-[var(--border)] pr-1">
 {activities.length > 0 ? (
 activities?.map((act, index) => {
 const cfg = getActivityConfig(act.action);
 return (
 <div key={act.id || index} className="flex items-start gap-3 py-3">
 <div
 className={cn(
 'mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border',
 cfg.bg
 )}
 >
 {cfg.icon}
 </div>
 <div className="flex-1 min-w-0">
 <p className="text-sm text-[var(--text-primary)] font-medium">
 {act.description || act.action}
 </p>
 <div className="flex items-center gap-2 mt-1 text-xs text-[var(--text-muted)]">
 <Badge variant="default" size="sm">
 {cfg.label}
 </Badge>
 <span>•</span>
 <span>{formatRelativeTime(act.createdAt || act.created_at)}</span>
 <span>•</span>
 <span>{act.createdAt ? new Date(act.createdAt).toLocaleString() : ''}</span>
 </div>
 </div>
 </div>
 );
 })
 ) : (
 <p className="py-6 text-center text-xs text-[var(--text-muted)]">
 No activity records found.
 </p>
 )}
 </div>

 <div className="flex justify-end pt-3 border-t border-[var(--border)]">
 <Button variant="secondary" onClick={() => setAllActivityModalOpen(false)}>
 Close
 </Button>
 </div>
 </div>
 </Modal>
 </div>
 </DashboardLayout>
 );
}
