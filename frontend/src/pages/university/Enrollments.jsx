import { useEffect, useState, useCallback, useRef } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import {
  Users,
  UserPlus,
  GraduationCap,
  LogOut,
  AlertCircle,
  Search,
  Eye,
  MoreVertical,
  Pencil,
  Calendar,
  MessageSquare,
  CheckCircle2,
  XCircle,
  RefreshCw,
  X,
  AlertTriangle,
  FileText,
  Clock,
  ChevronLeft,
  ChevronRight,
  School,
  Check,
  Building,
  BookOpen,
  Award,
  Loader2
} from 'lucide-react';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/shared/Card';
import Badge from '../../components/shared/Badge';
import Button from '../../components/shared/Button';
import Input from '../../components/shared/Input';
import Modal from '../../components/shared/Modal';
import LoadingSpinner from '../../components/shared/LoadingSpinner';
import ErrorMessage from '../../components/shared/ErrorMessage';
import EmptyState from '../../components/shared/EmptyState';
import SearchBar from '../../components/shared/SearchBar';
import CertificateDetailModal from '../../components/certificates/CertificateDetailModal';
import api from '../../services/api';
import { formatDate, cn } from '../../utils/helpers';
import { downloadCertificatePDF, previewCertificatePDF } from '../../services/certificateService';

const statusBadgeVariants = {
  active: 'success',
  graduated: 'primary',
  withdrawn: 'danger',
  withdrawal_requested: 'warning',
};

const statusDisplayLabels = {
  all: 'All',
  active: 'Active',
  graduated: 'Graduated',
  withdrawn: 'Withdrawn',
  withdrawal_requested: 'Withdrawal Requested',
};

export default function Enrollments() {
  const [searchParams, setSearchParams] = useSearchParams();

  // Enrollments List State
  const [enrollments, setEnrollments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [stats, setStats] = useState({
    total: 0,
    active: 0,
    graduated: 0,
    withdrawn: 0,
    pendingWithdrawals: 0,
  });

  // Filter & Search State
  const [statusFilter, setStatusFilter] = useState(searchParams.get('status') || 'all');
  const [searchInput, setSearchInput] = useState(searchParams.get('search') || '');
  const [debouncedSearch, setDebouncedSearch] = useState(searchInput);
  const [currentPage, setCurrentPage] = useState(1);
  const [pagination, setPagination] = useState({
    currentPage: 1,
    totalPages: 1,
    totalItems: 0,
    perPage: 25,
  });

  // Action Menu State (3-dot dropdown)
  const [openMenuId, setOpenMenuId] = useState(null);
  const menuContainerRef = useRef(null);

  // Modals State
  const [showEnrollModal, setShowEnrollModal] = useState(false);
  const [viewingEnrollment, setViewingEnrollment] = useState(null);
  const [editingEnrollment, setEditingEnrollment] = useState(null);
  const [extendingEnrollment, setExtendingEnrollment] = useState(null);
  const [withdrawingEnrollment, setWithdrawingEnrollment] = useState(null);
  const [respondingEnrollment, setRespondingEnrollment] = useState(null);

  // Certificate Modal states
  const [selectedCertificate, setSelectedCertificate] = useState(null);
  const [certDetailsLoading, setCertDetailsLoading] = useState(false);
  const [downloadingId, setDownloadingId] = useState(null);

  // Close 3-dot menu on click outside
  useEffect(() => {
    function handleClickOutside(e) {
      if (menuContainerRef.current && !menuContainerRef.current.contains(e.target)) {
        setOpenMenuId(null);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Debounce search input (500ms)
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(searchInput);
      setCurrentPage(1);
    }, 500);
    return () => clearTimeout(handler);
  }, [searchInput]);

  // Read status query param on mount or URL change
  useEffect(() => {
    const paramStatus = searchParams.get('status');
    if (paramStatus && paramStatus !== statusFilter) {
      setStatusFilter(paramStatus);
      setCurrentPage(1);
    }
  }, [searchParams]);

  // Sync with URL query params
  useEffect(() => {
    const params = new URLSearchParams();
    if (statusFilter !== 'all') params.set('status', statusFilter);
    if (debouncedSearch) params.set('search', debouncedSearch);
    if (currentPage > 1) params.set('page', currentPage);
    setSearchParams(params, { replace: true });
  }, [statusFilter, debouncedSearch, currentPage, setSearchParams]);

  // Check URL action=enroll
  useEffect(() => {
    if (searchParams.get('action') === 'enroll') {
      setShowEnrollModal(true);
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev);
        next.delete('action');
        return next;
      }, { replace: true });
    }
  }, [searchParams, setSearchParams]);

  // Fetch enrollments from backend
  const fetchEnrollments = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get('/university/enrollments', {
        params: {
          status: statusFilter,
          search: debouncedSearch.trim() || undefined,
          page: currentPage - 1,
          size: 25,
        },
      });

      const data = response.data;
      if (data.success) {
        setEnrollments(data.enrollments || []);
        if (data.pagination) {
          setPagination({
            currentPage: (data.pagination.currentPage ?? 0) + 1,
            totalPages: data.pagination.totalPages || 1,
            totalItems: data.pagination.totalItems || 0,
            perPage: data.pagination.perPage || 25,
          });
        }
        if (data.stats) {
          setStats({
            total: data.stats.total ?? 0,
            active: data.stats.active ?? 0,
            graduated: data.stats.graduated ?? 0,
            withdrawn: data.stats.withdrawn ?? 0,
            pendingWithdrawals: data.stats.pendingWithdrawals ?? 0,
          });
        }
      }
    } catch (err) {
      console.error('Error fetching enrollments:', err);
      const msg = err.response?.data?.message || 'Failed to load enrollments';
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  }, [statusFilter, debouncedSearch, currentPage]);

  useEffect(() => {
    fetchEnrollments();
  }, [fetchEnrollments]);

  // Handle status tab change
  const handleTabChange = (tabKey) => {
    setStatusFilter(tabKey);
    setCurrentPage(1);
  };

  // Certificate Handlers
  const handleViewCertificate = async (certificateId) => {
    setCertDetailsLoading(true);
    setSelectedCertificate({ id: certificateId, loading: true });
    try {
      const { data } = await api.get(`/university/certificates/${certificateId}`);
      if (data.success) {
        setSelectedCertificate(data.data);
      }
    } catch (err) {
      console.error('Failed to fetch details', err);
      toast.error('Failed to load certificate details');
      setSelectedCertificate(null);
    } finally {
      setCertDetailsLoading(false);
    }
  };

  const handleDownloadPdf = async (certificate) => {
    try {
      setDownloadingId(certificate.id);
      await downloadCertificatePDF(certificate.id, certificate.serial, '/university/certificates');
      toast.success('Certificate downloaded successfully');
    } catch (err) {
      console.error('Failed to download certificate:', err);
      toast.error(err.response?.data?.message || 'Failed to download certificate');
    } finally {
      setDownloadingId(null);
    }
  };

  const handlePreviewPdf = async (certificate) => {
    try {
      await previewCertificatePDF(certificate.id, '/university/certificates');
      toast.success('Certificate preview opened in a new tab');
    } catch (err) {
      console.error('Failed to preview certificate:', err);
      toast.error(err.response?.data?.message || 'Failed to preview certificate');
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-6 pb-12" ref={menuContainerRef}>
        {/* ================================================================= */}
        {/* HEADER ROW                                                        */}
        {/* ================================================================= */}
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-2xl font-bold text-[var(--text-primary)] sm:text-3xl">
              Enrollments
            </h1>
            <p className="mt-1 text-sm text-[var(--text-secondary)]">
              Manage student enrollments, academic progress, graduation timelines, and withdrawal requests.
            </p>
          </div>
          <Button
            variant="primary"
            onClick={() => setShowEnrollModal(true)}
            className="self-start sm:self-auto shadow-sm"
          >
            <UserPlus className="mr-2 h-4 w-4" />
            Enroll Student
          </Button>
        </div>

        {/* ================================================================= */}
        {/* STATS ROW (Clickable summary cards)                              */}
        {/* ================================================================= */}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {/* Card 1: Total Enrolled */}
          <Link
            to="/university/enrollments?status=active"
            onClick={() => handleTabChange('active')}
            className={cn(
              "group flex items-center justify-between gap-4 rounded-xl border p-4 shadow-sm transition-all duration-200 cursor-pointer hover:-translate-y-0.5 hover:shadow-md",
              statusFilter === 'active'
                ? "border-indigo-500/60 ring-2 ring-indigo-500/20 bg-[var(--bg-elevated)]"
                : "border-[var(--border)] bg-[var(--bg-surface)] hover:border-indigo-300 dark:hover:border-indigo-700"
            )}
          >
            <div className="flex items-center gap-4 min-w-0">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600 transition-transform group-hover:scale-105 dark:bg-indigo-950/40 dark:text-indigo-400">
                <Users className="h-6 w-6" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">
                  Total Enrolled
                </p>
                <h3 className="mt-0.5 text-2xl font-bold text-[var(--text-primary)]">
                  {stats.active}
                </h3>
                <p className="text-xs text-[var(--text-secondary)] truncate">
                  Currently active students
                </p>
              </div>
            </div>
            <ChevronRight className={cn(
              "h-4 w-4 shrink-0 transition-all duration-200",
              statusFilter === 'active'
                ? "text-indigo-600 dark:text-indigo-400 translate-x-0.5"
                : "text-[var(--text-muted)] opacity-40 group-hover:opacity-100 group-hover:translate-x-1"
            )} />
          </Link>

          {/* Card 2: Graduated */}
          <Link
            to="/university/enrollments?status=graduated"
            onClick={() => handleTabChange('graduated')}
            className={cn(
              "group flex items-center justify-between gap-4 rounded-xl border p-4 shadow-sm transition-all duration-200 cursor-pointer hover:-translate-y-0.5 hover:shadow-md",
              statusFilter === 'graduated'
                ? "border-emerald-500/60 ring-2 ring-emerald-500/20 bg-[var(--bg-elevated)]"
                : "border-[var(--border)] bg-[var(--bg-surface)] hover:border-emerald-300 dark:hover:border-emerald-700"
            )}
          >
            <div className="flex items-center gap-4 min-w-0">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600 transition-transform group-hover:scale-105 dark:bg-emerald-950/40 dark:text-emerald-400">
                <GraduationCap className="h-6 w-6" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">
                  Graduated
                </p>
                <h3 className="mt-0.5 text-2xl font-bold text-[var(--text-primary)]">
                  {stats.graduated}
                </h3>
                <p className="text-xs text-[var(--text-secondary)] truncate">
                  Successfully graduated
                </p>
              </div>
            </div>
            <ChevronRight className={cn(
              "h-4 w-4 shrink-0 transition-all duration-200",
              statusFilter === 'graduated'
                ? "text-emerald-600 dark:text-emerald-400 translate-x-0.5"
                : "text-[var(--text-muted)] opacity-40 group-hover:opacity-100 group-hover:translate-x-1"
            )} />
          </Link>

          {/* Card 3: Withdrawn */}
          <Link
            to="/university/enrollments?status=withdrawn"
            onClick={() => handleTabChange('withdrawn')}
            className={cn(
              "group flex items-center justify-between gap-4 rounded-xl border p-4 shadow-sm transition-all duration-200 cursor-pointer hover:-translate-y-0.5 hover:shadow-md",
              statusFilter === 'withdrawn'
                ? "border-rose-500/60 ring-2 ring-rose-500/20 bg-[var(--bg-elevated)]"
                : "border-[var(--border)] bg-[var(--bg-surface)] hover:border-rose-300 dark:hover:border-rose-700"
            )}
          >
            <div className="flex items-center gap-4 min-w-0">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-rose-50 text-rose-600 transition-transform group-hover:scale-105 dark:bg-rose-950/40 dark:text-rose-400">
                <LogOut className="h-6 w-6" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">
                  Withdrawn
                </p>
                <h3 className="mt-0.5 text-2xl font-bold text-[var(--text-primary)]">
                  {stats.withdrawn}
                </h3>
                <p className="text-xs text-[var(--text-secondary)] truncate">
                  Withdrawn enrollments
                </p>
              </div>
            </div>
            <ChevronRight className={cn(
              "h-4 w-4 shrink-0 transition-all duration-200",
              statusFilter === 'withdrawn'
                ? "text-rose-600 dark:text-rose-400 translate-x-0.5"
                : "text-[var(--text-muted)] opacity-40 group-hover:opacity-100 group-hover:translate-x-1"
            )} />
          </Link>

          {/* Card 4: Pending Withdrawals */}
          <Link
            to="/university/enrollments?status=withdrawal_requested"
            onClick={() => handleTabChange('withdrawal_requested')}
            className={cn(
              "group flex items-center justify-between gap-4 rounded-xl border p-4 shadow-sm transition-all duration-200 cursor-pointer hover:-translate-y-0.5 hover:shadow-md",
              statusFilter === 'withdrawal_requested'
                ? "border-amber-500/60 ring-2 ring-amber-500/20 bg-[var(--bg-elevated)]"
                : "border-[var(--border)] bg-[var(--bg-surface)] hover:border-amber-300 dark:hover:border-amber-700"
            )}
          >
            <div className="flex items-center gap-4 min-w-0">
              <div className="relative flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-amber-50 text-amber-600 transition-transform group-hover:scale-105 dark:bg-amber-950/40 dark:text-amber-400">
                <AlertCircle className="h-6 w-6" />
                {stats.pendingWithdrawals > 0 && (
                  <span className="absolute -top-1 -right-1 flex h-3 w-3">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75"></span>
                    <span className="relative inline-flex rounded-full h-3 w-3 bg-amber-500"></span>
                  </span>
                )}
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">
                  Pending Withdrawals
                </p>
                <h3 className="mt-0.5 text-2xl font-bold text-[var(--text-primary)]">
                  {stats.pendingWithdrawals}
                </h3>
                <p className="text-xs text-[var(--text-secondary)] truncate">
                  Awaiting university response
                </p>
              </div>
            </div>
            <ChevronRight className={cn(
              "h-4 w-4 shrink-0 transition-all duration-200",
              statusFilter === 'withdrawal_requested'
                ? "text-amber-600 dark:text-amber-400 translate-x-0.5"
                : "text-[var(--text-muted)] opacity-40 group-hover:opacity-100 group-hover:translate-x-1"
            )} />
          </Link>
        </div>

        {/* ================================================================= */}
        {/* FILTER ROW: Status Tabs + Search Input                           */}
        {/* ================================================================= */}
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          {/* Status Tabs */}
          <div className="flex flex-wrap items-center gap-1.5 rounded-xl border border-[var(--border)] bg-[var(--bg-surface)] p-1.5 shadow-sm">
            {[
              { key: 'all', label: 'All' },
              { key: 'active', label: 'Active' },
              { key: 'graduated', label: 'Graduated' },
              { key: 'withdrawn', label: 'Withdrawn' },
              { key: 'withdrawal_requested', label: 'Withdrawal Requested', count: stats.pendingWithdrawals },
            ].map((tab) => {
              const isActive = statusFilter === tab.key;
              return (
                <button
                  key={tab.key}
                  type="button"
                  onClick={() => handleTabChange(tab.key)}
                  className={cn(
                    'flex items-center gap-2 rounded-lg px-3.5 py-1.5 text-xs font-semibold transition-all',
                    isActive
                      ? 'bg-[var(--brand)] text-white shadow-sm'
                      : 'text-[var(--text-secondary)] hover:bg-[var(--bg-elevated)] hover:text-[var(--text-primary)]'
                  )}
                >
                  <span>{tab.label}</span>
                  {tab.count !== undefined && tab.count > 0 && (
                    <span
                      className={cn(
                        'flex h-4 min-w-[16px] items-center justify-center rounded-full px-1 text-[10px] font-bold',
                        isActive
                          ? 'bg-white text-[var(--brand)]'
                          : 'bg-amber-500 text-white'
                      )}
                    >
                      {tab.count}
                    </span>
                  )}
                </button>
              );
            })}
          </div>

          {/* Search Input (debounced 500ms) */}
          <div className="relative w-full max-w-md">
            <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--text-muted)]" />
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Search by student name or enrollment number..."
              className="h-10 w-full rounded-xl border border-[var(--border)] bg-[var(--bg-surface)] pl-10 pr-9 text-sm text-[var(--text-primary)] placeholder-[var(--text-muted)] outline-none transition-all focus:border-[var(--brand)] focus:ring-2 focus:ring-[var(--brand)]/10"
            />
            {searchInput && (
              <button
                type="button"
                onClick={() => setSearchInput('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)] hover:text-[var(--text-primary)]"
              >
                <X className="h-4 w-4" />
              </button>
            )}
          </div>
        </div>

        {/* ================================================================= */}
        {/* ENROLLMENTS TABLE                                                 */}
        {/* ================================================================= */}
        <div className="overflow-hidden rounded-xl border border-[var(--border)] bg-[var(--bg-surface)] shadow-sm">
          {loading ? (
            <div className="flex min-h-[340px] flex-col items-center justify-center gap-3 py-16">
              <LoadingSpinner size="lg" />
              <p className="text-sm font-medium text-[var(--text-secondary)]">Loading enrollments...</p>
            </div>
          ) : error ? (
            <div className="flex min-h-[300px] flex-col items-center justify-center p-8 text-center">
              <AlertCircle className="h-12 w-12 text-[var(--danger)]" />
              <h3 className="mt-3 text-base font-semibold text-[var(--text-primary)]">Error Loading Enrollments</h3>
              <p className="mt-1 text-sm text-[var(--text-secondary)]">{error}</p>
              <Button variant="secondary" size="sm" onClick={fetchEnrollments} className="mt-4">
                <RefreshCw className="mr-1.5 h-3.5 w-3.5" />
                Retry
              </Button>
            </div>
          ) : enrollments.length === 0 ? (
            <div className="py-12 px-4">
              <EmptyState
                icon={Users}
                title="No students enrolled yet."
                message={
                  debouncedSearch
                    ? `No enrollments matching "${debouncedSearch}". Try adjusting your filters.`
                    : statusFilter !== 'all'
                    ? `No enrollments currently in "${statusDisplayLabels[statusFilter]}" status.`
                    : "Click 'Enroll Student' to get started."
                }
                action={() => setShowEnrollModal(true)}
                actionLabel="Enroll First Student"
              />
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="border-b border-[var(--border)] bg-[var(--bg-elevated)]/60 text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">
                  <tr>
                    <th className="px-5 py-3.5">Student Name</th>
                    <th className="px-5 py-3.5">Student Email</th>
                    <th className="px-5 py-3.5">Enrollment #</th>
                    <th className="px-5 py-3.5">Program / Department</th>
                    <th className="px-5 py-3.5">Batch</th>
                    <th className="px-5 py-3.5">Status</th>
                    <th className="px-5 py-3.5">Expected Graduation</th>
                    <th className="px-5 py-3.5 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--border)]">
                  {enrollments.map((enr) => {
                    const status = enr.status?.toLowerCase() || 'active';

                    return (
                      <tr
                        key={enr.id}
                        className="transition-colors hover:bg-[var(--bg-elevated)]/40"
                      >
                        {/* Student Name */}
                        <td className="px-5 py-4 font-semibold text-[var(--text-primary)]">
                          <div className="flex flex-col">
                            <span>{enr.studentName || '—'}</span>
                            {enr.studentIdInUniversity && (
                              <span className="text-xs font-normal text-[var(--text-muted)]">
                                ID: {enr.studentIdInUniversity}
                              </span>
                            )}
                          </div>
                        </td>

                        {/* Student Email */}
                        <td className="px-5 py-4 text-[var(--text-secondary)]">
                          {enr.studentEmail || '—'}
                        </td>

                        {/* Enrollment Number (monospace) */}
                        <td className="px-5 py-4">
                          <span className="inline-block rounded border border-[var(--brand)]/20 bg-[var(--brand-light)]/40 px-2 py-0.5 font-mono text-xs font-medium text-[var(--brand)] dark:bg-[var(--brand-light)]/10">
                            {enr.enrollmentNumber || '—'}
                          </span>
                        </td>

                        {/* Program / Department */}
                        <td className="px-5 py-4">
                          <div className="max-w-xs">
                            <p className="font-medium text-[var(--text-primary)] truncate" title={enr.program}>
                              {enr.program || '—'}
                            </p>
                            <p className="text-xs text-[var(--text-secondary)] truncate" title={enr.department}>
                              {enr.department || '—'}
                              {enr.major && ` • ${enr.major}`}
                            </p>
                          </div>
                        </td>

                        {/* Batch */}
                        <td className="px-5 py-4 text-[var(--text-secondary)]">
                          {enr.batch || '—'}
                        </td>

                        {/* Status Badge */}
                        <td className="px-5 py-4">
                          <Badge
                            variant={statusBadgeVariants[status] || 'default'}
                            dot
                            size="sm"
                          >
                            {status === 'withdrawal_requested'
                              ? 'WITHDRAWAL REQUESTED'
                              : status.toUpperCase()}
                          </Badge>
                        </td>

                        {/* Expected Graduation */}
                        <td className="px-5 py-4 text-[var(--text-secondary)]">
                          {formatDate(enr.expectedGraduationDate)}
                        </td>

                        {/* Actions */}
                        <td className="px-5 py-4 text-right">
                          <Button
                            variant="secondary"
                            size="sm"
                            onClick={() => setViewingEnrollment(enr)}
                            className="h-8 px-3 text-xs font-medium"
                          >
                            <Eye className="mr-1.5 h-3.5 w-3.5" />
                            View
                          </Button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}

          {/* =============================================================== */}
          {/* PAGINATION ROW (25 per page)                                    */}
          {/* =============================================================== */}
          {!loading && enrollments.length > 0 && (
            <div className="flex flex-col items-center justify-between gap-4 border-t border-[var(--border)] px-6 py-4 sm:flex-row">
              <p className="text-xs text-[var(--text-secondary)]">
                Showing{' '}
                <span className="font-semibold text-[var(--text-primary)]">
                  {(pagination.currentPage - 1) * pagination.perPage + 1}
                </span>{' '}
                to{' '}
                <span className="font-semibold text-[var(--text-primary)]">
                  {Math.min(pagination.currentPage * pagination.perPage, pagination.totalItems)}
                </span>{' '}
                of{' '}
                <span className="font-semibold text-[var(--text-primary)]">
                  {pagination.totalItems}
                </span>{' '}
                enrollments
              </p>

              <div className="flex items-center gap-2">
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                  disabled={pagination.currentPage <= 1}
                  className="h-8 text-xs"
                >
                  <ChevronLeft className="mr-1 h-3.5 w-3.5" />
                  Previous
                </Button>
                <div className="flex items-center px-2 text-xs font-medium text-[var(--text-secondary)]">
                  Page {pagination.currentPage} of {pagination.totalPages}
                </div>
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => setCurrentPage((p) => Math.min(pagination.totalPages, p + 1))}
                  disabled={pagination.currentPage >= pagination.totalPages}
                  className="h-8 text-xs"
                >
                  Next
                  <ChevronRight className="ml-1 h-3.5 w-3.5" />
                </Button>
              </div>
            </div>
          )}
        </div>

        {/* ================================================================= */}
        {/* MODAL 1: ENROLL STUDENT MODAL                                     */}
        {/* ================================================================= */}
        {showEnrollModal && (
          <EnrollStudentModal
            isOpen={showEnrollModal}
            onClose={() => setShowEnrollModal(false)}
            onSuccess={() => {
              setShowEnrollModal(false);
              fetchEnrollments();
            }}
          />
        )}

        {/* ================================================================= */}
        {/* MODAL 2: VIEW ENROLLMENT MODAL                                    */}
        {/* ================================================================= */}
        {viewingEnrollment && (
          <ViewEnrollmentModal
            enrollment={viewingEnrollment}
            onClose={() => setViewingEnrollment(null)}
            onEdit={(enr) => setEditingEnrollment(enr)}
            onExtend={(enr) => setExtendingEnrollment(enr)}
            onWithdraw={(enr) => setWithdrawingEnrollment(enr)}
            onRespond={(enr) => setRespondingEnrollment(enr)}
            onViewCertificate={handleViewCertificate}
          />
        )}

        {/* ================================================================= */}
        {/* MODAL 3: EDIT ENROLLMENT MODAL                                    */}
        {/* ================================================================= */}
        {editingEnrollment && (
          <EditEnrollmentModal
            enrollment={editingEnrollment}
            onClose={() => setEditingEnrollment(null)}
            onSuccess={() => {
              setEditingEnrollment(null);
              fetchEnrollments();
            }}
          />
        )}

        {/* ================================================================= */}
        {/* MODAL 4: EXTEND GRADUATION DATE MODAL                             */}
        {/* ================================================================= */}
        {extendingEnrollment && (
          <ExtendGraduationModal
            enrollment={extendingEnrollment}
            onClose={() => setExtendingEnrollment(null)}
            onSuccess={() => {
              setExtendingEnrollment(null);
              fetchEnrollments();
            }}
          />
        )}

        {/* ================================================================= */}
        {/* MODAL 5: DIRECT WITHDRAW MODAL                                    */}
        {/* ================================================================= */}
        {withdrawingEnrollment && (
          <DirectWithdrawModal
            enrollment={withdrawingEnrollment}
            onClose={() => setWithdrawingEnrollment(null)}
            onSuccess={() => {
              setWithdrawingEnrollment(null);
              fetchEnrollments();
            }}
          />
        )}

        {/* ================================================================= */}
        {/* MODAL 6: RESPOND TO WITHDRAWAL MODAL                              */}
        {/* ================================================================= */}
        {respondingEnrollment && (
          <RespondWithdrawalModal
            enrollment={respondingEnrollment}
            onClose={() => setRespondingEnrollment(null)}
            onSuccess={() => {
              setRespondingEnrollment(null);
              fetchEnrollments();
            }}
          />
        )}

        {/* ================================================================= */}
        {/* CERTIFICATE DETAIL MODAL                                          */}
        {/* ================================================================= */}
        <CertificateDetailModal
          open={!!selectedCertificate}
          onClose={() => setSelectedCertificate(null)}
          certificate={selectedCertificate}
          loading={certDetailsLoading}
          downloadingId={downloadingId}
          onDownloadPdf={handleDownloadPdf}
          onPreviewPdf={handlePreviewPdf}
          role="university"
        />
      </div>
    </DashboardLayout>
  );
}

// =============================================================================
// MODAL 1: ENROLL STUDENT MODAL (2 Steps)
// =============================================================================
function EnrollStudentModal({ isOpen, onClose, onSuccess }) {
  const [step, setStep] = useState(1); // 1 = search student, 2 = fill form
  const [searchQuery, setSearchQuery] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const [searchResults, setSearchResults] = useState([]);
  const [selectedStudent, setSelectedStudent] = useState(null);

  // Step 2 form fields
  const [formData, setFormData] = useState({
    studentIdInUniversity: '',
    program: '',
    certificateLevelId: '',
    departmentId: '',
    majorId: '',
    batch: '',
    enrollmentDate: new Date().toISOString().split('T')[0],
    expectedGraduationDate: '',
  });

  const [certLevels, setCertLevels] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [majors, setMajors] = useState([]);

  useEffect(() => {
    if (step === 2) {
      api.get('/university/certificate-levels')
        .then(res => {
          if (res.data.success) {
            setCertLevels(res.data.certificate_levels.filter(l => l.isActive !== false));
          }
        }).catch(err => console.error('Failed to load cert levels', err));
    }
  }, [step]);

  useEffect(() => {
    if (formData.certificateLevelId) {
      api.get('/university/departments', { params: { certificate_level_id: formData.certificateLevelId } })
        .then(res => {
          if (res.data.success) {
            setDepartments(res.data.departments.filter(d => d.isActive !== false));
          }
        }).catch(err => console.error('Failed to load departments', err));
    } else {
      setDepartments([]);
    }
    // reset downstream fields only if not initial load mapping (in enroll mode it is always user interaction)
    setFormData(prev => ({ ...prev, departmentId: '', majorId: '' }));
  }, [formData.certificateLevelId]);

  useEffect(() => {
    if (formData.departmentId) {
      api.get('/university/majors', { params: { department_id: formData.departmentId } })
        .then(res => {
          if (res.data.success) {
            setMajors(res.data.majors.filter(m => m.isActive !== false));
          }
        }).catch(err => console.error('Failed to load majors', err));
    } else {
      setMajors([]);
    }
    setFormData(prev => ({ ...prev, majorId: '' }));
  }, [formData.departmentId]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Step 1: Debounced student search or manual search
  const handleSearchStudents = async (query) => {
    const q = (query || searchQuery).trim();
    if (q.length < 2) {
      setSearchResults([]);
      return;
    }

    setIsSearching(true);
    try {
      const response = await api.get('/university/students/search-to-enroll', {
        params: { q },
      });
      if (response.data.success) {
        setSearchResults(response.data.students || []);
      }
    } catch (err) {
      console.error('Failed to search students:', err);
      toast.error(err.response?.data?.message || 'Failed to search students');
    } finally {
      setIsSearching(false);
    }
  };

  const handleSelectStudent = (student) => {
    setSelectedStudent(student);
    setStep(2);
  };

  const handleSubmitEnrollment = async (e) => {
    e.preventDefault();
    if (!selectedStudent) {
      toast.error('Please select a student first');
      setStep(1);
      return;
    }

    if (!formData.studentIdInUniversity.trim()) {
      toast.error('Student ID in University is required');
      return;
    }
    if (!formData.certificateLevelId) {
      toast.error('Certificate Level is required');
      return;
    }
    if (!formData.departmentId) {
      toast.error('Department is required');
      return;
    }
    if (!formData.program.trim()) {
      toast.error('Program is required');
      return;
    }
    if (!formData.batch.trim()) {
      toast.error('Batch is required');
      return;
    }
    if (!formData.expectedGraduationDate) {
      toast.error('Expected Graduation Date is required');
      return;
    }

    if (new Date(formData.expectedGraduationDate) <= new Date(formData.enrollmentDate)) {
      toast.error('Expected graduation date must be after the enrollment date');
      return;
    }

    setIsSubmitting(true);
    try {
      const payload = {
        studentEmail: selectedStudent.email,
        studentIdInUniversity: formData.studentIdInUniversity.trim(),
        program: formData.program.trim(),
        certificateLevelId: formData.certificateLevelId ? parseInt(formData.certificateLevelId, 10) : undefined,
        departmentId: formData.departmentId ? parseInt(formData.departmentId, 10) : undefined,
        majorId: formData.majorId ? parseInt(formData.majorId, 10) : undefined,
        batch: formData.batch.trim(),
        enrollmentDate: formData.enrollmentDate,
        expectedGraduationDate: formData.expectedGraduationDate,
      };

      const response = await api.post('/university/enrollments', payload);
      if (response.data.success) {
        toast.success(response.data.message || 'Student enrolled successfully');
        onSuccess();
      }
    } catch (err) {
      console.error('Failed to enroll student:', err);
      const errors = err.response?.data?.errors;
      const errorMsg = err.response?.data?.message || (errors && typeof errors === 'object' ? Object.values(errors).flat().join(', ') : null) || 'Failed to enroll student';
      toast.error(errorMsg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={step === 1 ? 'Enroll Student — Step 1: Select Student' : 'Enroll Student — Step 2: Academic Details'}
      size="lg"
    >
      {step === 1 ? (
        <div className="space-y-4">
          <p className="text-xs text-[var(--text-secondary)]">
            Search approved students by their full name (partial match) or verified email address (exact match).
          </p>

          <div className="flex gap-2">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--text-muted)]" />
              <input
                type="text"
                autoFocus
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  if (e.target.value.trim().length >= 2) {
                    handleSearchStudents(e.target.value);
                  } else {
                    setSearchResults([]);
                  }
                }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    handleSearchStudents();
                  }
                }}
                placeholder="Search by student email or name..."
                className="h-10 w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] pl-9 pr-3 text-sm text-[var(--text-primary)] outline-none focus:border-[var(--brand)] focus:ring-2 focus:ring-[var(--brand)]/10"
              />
            </div>
            <Button
              type="button"
              variant="secondary"
              onClick={() => handleSearchStudents()}
              loading={isSearching}
              className="shrink-0"
            >
              Search
            </Button>
          </div>

          {/* Search Results List */}
          <div className="mt-4 max-h-[320px] overflow-y-auto space-y-2 pr-1">
            {isSearching ? (
              <div className="flex flex-col items-center justify-center py-8">
                <LoadingSpinner size="md" />
                <p className="mt-2 text-xs text-[var(--text-secondary)]">Searching approved students...</p>
              </div>
            ) : searchResults.length > 0 ? (
              searchResults.map((student) => {
                const canEnroll = student.canEnroll !== false;
                return (
                  <div
                    key={student.id}
                    onClick={() => canEnroll && handleSelectStudent(student)}
                    className={cn(
                      'flex items-center justify-between rounded-xl border p-3.5 transition-all',
                      canEnroll
                        ? 'border-[var(--border)] hover:border-[var(--brand)] hover:bg-[var(--bg-elevated)]/50 cursor-pointer'
                        : 'border-dashed border-[var(--border)] opacity-60 bg-[var(--bg-elevated)]/20 cursor-not-allowed'
                    )}
                  >
                    <div>
                      <h4 className="text-sm font-semibold text-[var(--text-primary)]">
                        {student.name}
                      </h4>
                      <p className="text-xs text-[var(--text-secondary)]">{student.email}</p>
                      {student.activeInstitution && (
                        <p className="mt-1 text-[11px] text-amber-600 dark:text-amber-400">
                          Active at: {student.activeInstitution}
                        </p>
                      )}
                    </div>
                    <div>
                      {canEnroll ? (
                        <span className="inline-flex items-center rounded-lg bg-[var(--brand-light)] px-3 py-1 text-xs font-semibold text-[var(--brand)]">
                          Select
                        </span>
                      ) : (
                        <Badge variant="warning" size="sm">
                          {student.currentEnrollmentStatus?.toUpperCase() || 'ALREADY ENROLLED'}
                        </Badge>
                      )}
                    </div>
                  </div>
                );
              })
            ) : searchQuery.trim().length >= 2 ? (
              <div className="rounded-xl border border-dashed border-[var(--border)] p-6 text-center">
                <p className="text-xs text-[var(--text-secondary)]">
                  No verified students found matching "{searchQuery}".
                </p>
              </div>
            ) : (
              <div className="rounded-xl border border-dashed border-[var(--border)] p-6 text-center text-xs text-[var(--text-muted)]">
                Type at least 2 characters to search for a student.
              </div>
            )}
          </div>
        </div>
      ) : (
        /* Step 2: Fill Form */
        <form onSubmit={handleSubmitEnrollment} className="space-y-4">
          {/* Selected Student Banner */}
          <div className="flex items-center justify-between rounded-xl border border-[var(--brand)]/30 bg-[var(--brand-light)]/20 p-3.5">
            <div>
              <p className="text-xs font-medium uppercase tracking-wider text-[var(--brand)]">
                Selected Student
              </p>
              <h4 className="text-sm font-bold text-[var(--text-primary)]">
                {selectedStudent?.name}
              </h4>
              <p className="text-xs text-[var(--text-secondary)]">{selectedStudent?.email}</p>
            </div>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => setStep(1)}
              className="text-xs text-[var(--brand)] hover:underline"
            >
              Change Student
            </Button>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            {/* Student ID in University */}
            <div className="sm:col-span-2">
              <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
                Assign Student ID <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                required
                value={formData.studentIdInUniversity}
                onChange={(e) => setFormData({ ...formData, studentIdInUniversity: e.target.value })}
                placeholder="e.g. UIU2026-CSE-001"
                className="h-10 w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] px-3 text-sm text-[var(--text-primary)] outline-none focus:border-[var(--brand)] focus:ring-2 focus:ring-[var(--brand)]/10"
              />
            </div>

            {/* Certificate Level */}
            <div>
              <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
                Certificate Level <span className="text-rose-500">*</span>
              </label>
              <select
                required
                value={formData.certificateLevelId}
                onChange={(e) => setFormData({ ...formData, certificateLevelId: e.target.value })}
                className="h-10 w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] px-3 text-sm text-[var(--text-primary)] outline-none focus:border-[var(--brand)] focus:ring-2 focus:ring-[var(--brand)]/10"
              >
                <option value="">Select Level...</option>
                {certLevels.map(l => (
                  <option key={l.id} value={l.id}>{l.name} ({l.shortCode})</option>
                ))}
              </select>
            </div>

            {/* Department */}
            <div>
              <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
                Department <span className="text-rose-500">*</span>
              </label>
              <select
                required
                disabled={!formData.certificateLevelId}
                value={formData.departmentId}
                onChange={(e) => setFormData({ ...formData, departmentId: e.target.value })}
                className="h-10 w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] px-3 text-sm text-[var(--text-primary)] outline-none focus:border-[var(--brand)] focus:ring-2 focus:ring-[var(--brand)]/10 disabled:opacity-50"
              >
                <option value="">Select Department...</option>
                {departments.map(d => (
                  <option key={d.id} value={d.id}>{d.name} ({d.shortCode})</option>
                ))}
              </select>
            </div>

            {/* Major (optional) */}
            <div>
              <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
                Major <span className="text-xs font-normal text-[var(--text-muted)]">(Optional)</span>
              </label>
              <select
                disabled={!formData.departmentId || majors.length === 0}
                value={formData.majorId}
                onChange={(e) => setFormData({ ...formData, majorId: e.target.value })}
                className="h-10 w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] px-3 text-sm text-[var(--text-primary)] outline-none focus:border-[var(--brand)] focus:ring-2 focus:ring-[var(--brand)]/10 disabled:opacity-50"
              >
                <option value="">{majors.length === 0 && formData.departmentId ? 'No majors available' : 'Select Major...'}</option>
                {majors.map(m => (
                  <option key={m.id} value={m.id}>{m.name}</option>
                ))}
              </select>
            </div>

            {/* Program */}
            <div>
              <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
                Program Title <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                required
                value={formData.program}
                onChange={(e) => setFormData({ ...formData, program: e.target.value })}
                placeholder="e.g. B.Sc. in Computer Science & Engineering"
                className="h-10 w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] px-3 text-sm text-[var(--text-primary)] outline-none focus:border-[var(--brand)] focus:ring-2 focus:ring-[var(--brand)]/10"
              />
            </div>

            {/* Batch */}
            <div>
              <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
                Batch <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                required
                value={formData.batch}
                onChange={(e) => setFormData({ ...formData, batch: e.target.value })}
                placeholder="e.g. 2024-Spring"
                className="h-10 w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] px-3 text-sm text-[var(--text-primary)] outline-none focus:border-[var(--brand)] focus:ring-2 focus:ring-[var(--brand)]/10"
              />
            </div>

            {/* Enrollment Date */}
            <div>
              <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
                Enrollment Date <span className="text-rose-500">*</span>
              </label>
              <input
                type="date"
                required
                value={formData.enrollmentDate}
                onChange={(e) => setFormData({ ...formData, enrollmentDate: e.target.value })}
                className="h-10 w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] px-3 text-sm text-[var(--text-primary)] outline-none focus:border-[var(--brand)] focus:ring-2 focus:ring-[var(--brand)]/10"
              />
            </div>

            {/* Expected Graduation Date */}
            <div>
              <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
                Expected Graduation Date <span className="text-rose-500">*</span>
              </label>
              <input
                type="date"
                required
                value={formData.expectedGraduationDate}
                onChange={(e) => setFormData({ ...formData, expectedGraduationDate: e.target.value })}
                className="h-10 w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] px-3 text-sm text-[var(--text-primary)] outline-none focus:border-[var(--brand)] focus:ring-2 focus:ring-[var(--brand)]/10"
              />
            </div>
          </div>

          {/* Buttons */}
          <div className="flex justify-end gap-2 pt-4 border-t border-[var(--border)]">
            <Button type="button" variant="secondary" onClick={() => setStep(1)}>
              Back to Search
            </Button>
            <Button type="submit" variant="primary" loading={isSubmitting}>
              Submit Enrollment
            </Button>
          </div>
        </form>
      )}
    </Modal>
  );
}

// =============================================================================
// MODAL 2: VIEW ENROLLMENT MODAL (Read-Only)
// =============================================================================
function ViewEnrollmentModal({ enrollment, onClose, onEdit, onExtend, onWithdraw, onRespond, onViewCertificate }) {
  if (!enrollment) return null;

  return (
    <Modal
      isOpen={Boolean(enrollment)}
      onClose={onClose}
      title="Enrollment Details"
      size="md"
      footer={
        <div className="flex w-full flex-col sm:flex-row items-center justify-between gap-2">
          <div className="flex flex-wrap items-center gap-2 w-full sm:w-auto">
            {enrollment.status === 'active' && onEdit && (
              <Button size="sm" variant="secondary" onClick={() => { onClose(); onEdit(enrollment); }}>
                <Pencil className="mr-1.5 h-3.5 w-3.5" />
                Edit
              </Button>
            )}
            {enrollment.status === 'active' && onExtend && (
              <Button size="sm" variant="secondary" onClick={() => { onClose(); onExtend(enrollment); }}>
                <Calendar className="mr-1.5 h-3.5 w-3.5" />
                Extend Date
              </Button>
            )}
            {enrollment.status === 'active' && onWithdraw && (
              <Button size="sm" variant="danger" className="bg-rose-100 text-rose-700 hover:bg-rose-200 border-none" onClick={() => { onClose(); onWithdraw(enrollment); }}>
                <LogOut className="mr-1.5 h-3.5 w-3.5" />
                Withdraw
              </Button>
            )}
            {enrollment.status === 'withdrawal_requested' && onRespond && (
              <Button size="sm" variant="primary" className="bg-amber-500 hover:bg-amber-600 border-none text-white" onClick={() => { onClose(); onRespond(enrollment); }}>
                <MessageSquare className="mr-1.5 h-3.5 w-3.5" />
                Respond to Request
              </Button>
            )}
            {enrollment.status === 'graduated' && enrollment.certificateId && onViewCertificate && (
              <Button size="sm" variant="primary" className="bg-emerald-600 hover:bg-emerald-700 border-none text-white" onClick={() => { onClose(); onViewCertificate(enrollment.certificateId); }}>
                <Award className="mr-1.5 h-3.5 w-3.5" />
                View Certificate
              </Button>
            )}
          </div>
          <Button variant="secondary" onClick={onClose} className="w-full sm:w-auto shrink-0">
            Close
          </Button>
        </div>
      }
    >
      <div className="space-y-6">
        {/* Student Information Banner */}
        <div className="rounded-xl border border-[var(--border)] bg-[var(--bg-elevated)]/40 p-4">
          <div className="flex items-start justify-between">
            <div>
              <h3 className="text-base font-bold text-[var(--text-primary)]">
                {enrollment.studentName || 'Unknown Student'}
              </h3>
              <p className="text-xs text-[var(--text-secondary)]">{enrollment.studentEmail || '—'}</p>
              {enrollment.studentIdInUniversity && (
                <p className="mt-1 text-xs font-medium text-[var(--brand)]">
                  University Student ID: {enrollment.studentIdInUniversity}
                </p>
              )}
            </div>
            <Badge
              variant={statusBadgeVariants[enrollment.status?.toLowerCase()] || 'default'}
              dot
            >
              {enrollment.status?.toUpperCase() || 'UNKNOWN'}
            </Badge>
          </div>
        </div>

        {/* Academic Details Grid */}
        <div className="grid grid-cols-2 gap-4 text-xs">
          <div>
            <p className="text-[var(--text-muted)] uppercase tracking-wide font-semibold">Enrollment #</p>
            <p className="mt-1 font-mono text-sm font-semibold text-[var(--brand)]">
              {enrollment.enrollmentNumber || '—'}
            </p>
          </div>
          <div>
            <p className="text-[var(--text-muted)] uppercase tracking-wide font-semibold">Batch</p>
            <p className="mt-1 font-medium text-[var(--text-primary)]">
              {enrollment.batch || '—'}
            </p>
          </div>
          <div>
            <p className="text-[var(--text-muted)] uppercase tracking-wide font-semibold">Program</p>
            <p className="mt-1 font-medium text-[var(--text-primary)]">
              {enrollment.program || '—'}
            </p>
          </div>
          <div>
            <p className="text-[var(--text-muted)] uppercase tracking-wide font-semibold">Department</p>
            <p className="mt-1 font-medium text-[var(--text-primary)]">
              {enrollment.department || '—'}
            </p>
          </div>
          <div>
            <p className="text-[var(--text-muted)] uppercase tracking-wide font-semibold">Major</p>
            <p className="mt-1 font-medium text-[var(--text-primary)]">
              {enrollment.major || 'None'}
            </p>
          </div>
          <div>
            <p className="text-[var(--text-muted)] uppercase tracking-wide font-semibold">Enrollment Date</p>
            <p className="mt-1 font-medium text-[var(--text-primary)]">
              {formatDate(enrollment.enrollmentDate)}
            </p>
          </div>
          <div>
            <p className="text-[var(--text-muted)] uppercase tracking-wide font-semibold">Expected Graduation</p>
            <p className="mt-1 font-medium text-[var(--text-primary)]">
              {formatDate(enrollment.expectedGraduationDate)}
            </p>
          </div>
          {enrollment.actualGraduationDate && (
            <div>
              <p className="text-[var(--text-muted)] uppercase tracking-wide font-semibold">Actual Graduation / Withdrawn Date</p>
              <p className="mt-1 font-medium text-[var(--text-primary)]">
                {formatDate(enrollment.actualGraduationDate)}
              </p>
            </div>
          )}
        </div>

        {/* Withdrawal Details Callout (if withdrawal_requested or has withdrawalReason) */}
        {(enrollment.status === 'withdrawal_requested' || enrollment.withdrawalReason) && (
          <div className="rounded-xl border border-amber-300 bg-amber-50/60 p-4 dark:border-amber-800/40 dark:bg-amber-950/20">
            <div className="flex items-center gap-2 text-amber-700 dark:text-amber-400">
              <AlertTriangle className="h-4 w-4 shrink-0" />
              <h4 className="text-xs font-bold uppercase tracking-wider">
                Withdrawal Request Notice
              </h4>
            </div>
            <blockquote className="mt-2 text-xs italic text-[var(--text-secondary)]">
              "{enrollment.withdrawalReason || 'No detailed reason specified.'}"
            </blockquote>
            {enrollment.withdrawalRequestedAt && (
              <p className="mt-2 text-[11px] text-[var(--text-muted)]">
                Requested on: {formatDate(enrollment.withdrawalRequestedAt)}
              </p>
            )}
          </div>
        )}
      </div>
    </Modal>
  );
}

// =============================================================================
// MODAL 3: EDIT ENROLLMENT MODAL
// =============================================================================
function EditEnrollmentModal({ enrollment, onClose, onSuccess }) {
  const [formData, setFormData] = useState({
    program: enrollment.program || '',
    certificateLevelId: enrollment.certificateLevelId || '',
    departmentId: enrollment.departmentId || '',
    majorId: enrollment.majorId || '',
    batch: enrollment.batch || '',
    expectedGraduationDate: enrollment.expectedGraduationDate || '',
  });
  
  const [certLevels, setCertLevels] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [majors, setMajors] = useState([]);

  useEffect(() => {
    api.get('/university/certificate-levels')
      .then(res => {
        if (res.data.success) {
          setCertLevels(res.data.certificate_levels.filter(l => l.isActive !== false || l.id === enrollment.certificateLevelId));
        }
      }).catch(err => console.error(err));
  }, [enrollment.certificateLevelId]);

  useEffect(() => {
    if (formData.certificateLevelId) {
      api.get('/university/departments', { params: { certificate_level_id: formData.certificateLevelId } })
        .then(res => {
          if (res.data.success) {
            setDepartments(res.data.departments.filter(d => d.isActive !== false || d.id === enrollment.departmentId));
          }
        }).catch(err => console.error(err));
    } else {
      setDepartments([]);
    }
  }, [formData.certificateLevelId, enrollment.departmentId]);

  useEffect(() => {
    if (formData.departmentId) {
      api.get('/university/majors', { params: { department_id: formData.departmentId } })
        .then(res => {
          if (res.data.success) {
            setMajors(res.data.majors.filter(m => m.isActive !== false || m.id === enrollment.majorId));
          }
        }).catch(err => console.error(err));
    } else {
      setMajors([]);
    }
  }, [formData.departmentId, enrollment.majorId]);

  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      const payload = {
        program: formData.program.trim() || undefined,
        certificateLevelId: formData.certificateLevelId ? parseInt(formData.certificateLevelId, 10) : undefined,
        departmentId: formData.departmentId ? parseInt(formData.departmentId, 10) : undefined,
        majorId: formData.majorId ? parseInt(formData.majorId, 10) : undefined,
        batch: formData.batch.trim() || undefined,
        expectedGraduationDate: formData.expectedGraduationDate || undefined,
      };

      const response = await api.put(`/university/enrollments/${enrollment.id}`, payload);
      if (response.data.success) {
        toast.success(response.data.message || 'Enrollment updated successfully');
        onSuccess();
      }
    } catch (err) {
      console.error('Failed to update enrollment:', err);
      toast.error(err.response?.data?.message || 'Failed to update enrollment');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={Boolean(enrollment)}
      onClose={onClose}
      title={`Edit Enrollment — ${enrollment.studentName}`}
      size="md"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <p className="text-xs text-[var(--text-secondary)]">
          Update the program, department, batch, or expected graduation date. All fields are optional; student status cannot be modified here.
        </p>

        <div>
          <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
            Certificate Level
          </label>
          <select
            value={formData.certificateLevelId}
            onChange={(e) => setFormData({ ...formData, certificateLevelId: e.target.value, departmentId: '', majorId: '' })}
            className="h-10 w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] px-3 text-sm text-[var(--text-primary)] outline-none focus:border-[var(--brand)] focus:ring-2 focus:ring-[var(--brand)]/10"
          >
            <option value="">Select Level...</option>
            {certLevels.map(l => (
              <option key={l.id} value={l.id}>{l.name} ({l.shortCode})</option>
            ))}
          </select>
        </div>

        <div>
          <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
            Department
          </label>
          <select
            disabled={!formData.certificateLevelId}
            value={formData.departmentId}
            onChange={(e) => setFormData({ ...formData, departmentId: e.target.value, majorId: '' })}
            className="h-10 w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] px-3 text-sm text-[var(--text-primary)] outline-none focus:border-[var(--brand)] focus:ring-2 focus:ring-[var(--brand)]/10 disabled:opacity-50"
          >
            <option value="">Select Department...</option>
            {departments.map(d => (
              <option key={d.id} value={d.id}>{d.name} ({d.shortCode})</option>
            ))}
          </select>
        </div>

        <div>
          <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
            Major
          </label>
          <select
            disabled={!formData.departmentId || majors.length === 0}
            value={formData.majorId}
            onChange={(e) => setFormData({ ...formData, majorId: e.target.value })}
            className="h-10 w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] px-3 text-sm text-[var(--text-primary)] outline-none focus:border-[var(--brand)] focus:ring-2 focus:ring-[var(--brand)]/10 disabled:opacity-50"
          >
            <option value="">{majors.length === 0 && formData.departmentId ? 'No majors available' : 'Select Major...'}</option>
            {majors.map(m => (
              <option key={m.id} value={m.id}>{m.name}</option>
            ))}
          </select>
        </div>
        
        <div>
          <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
            Program Title
          </label>
          <input
            type="text"
            value={formData.program}
            onChange={(e) => setFormData({ ...formData, program: e.target.value })}
            className="h-10 w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] px-3 text-sm text-[var(--text-primary)] outline-none focus:border-[var(--brand)] focus:ring-2 focus:ring-[var(--brand)]/10"
          />
        </div>

        <div>
          <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
            Batch
          </label>
          <input
            type="text"
            value={formData.batch}
            onChange={(e) => setFormData({ ...formData, batch: e.target.value })}
            className="h-10 w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] px-3 text-sm text-[var(--text-primary)] outline-none focus:border-[var(--brand)] focus:ring-2 focus:ring-[var(--brand)]/10"
          />
        </div>

        <div>
          <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
            Expected Graduation Date
          </label>
          <input
            type="date"
            value={formData.expectedGraduationDate}
            onChange={(e) => setFormData({ ...formData, expectedGraduationDate: e.target.value })}
            className="h-10 w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] px-3 text-sm text-[var(--text-primary)] outline-none focus:border-[var(--brand)] focus:ring-2 focus:ring-[var(--brand)]/10"
          />
        </div>

        <div className="flex justify-end gap-2 pt-4 border-t border-[var(--border)]">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" variant="primary" loading={isSubmitting}>
            Save Changes
          </Button>
        </div>
      </form>
    </Modal>
  );
}

// =============================================================================
// MODAL 4: EXTEND GRADUATION DATE MODAL
// =============================================================================
function ExtendGraduationModal({ enrollment, onClose, onSuccess }) {
  const [newDate, setNewDate] = useState('');
  const [reason, setReason] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const currentDate = enrollment.expectedGraduationDate || '';
  const isDateInvalid = Boolean(newDate && currentDate && newDate <= currentDate);
  const isReasonInvalid = reason.trim().length < 10;
  const canSubmit = Boolean(newDate && !isDateInvalid && !isReasonInvalid);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!canSubmit) return;

    setIsSubmitting(true);
    try {
      const response = await api.patch(`/university/enrollments/${enrollment.id}/extend-graduation`, {
        newExpectedGraduationDate: newDate,
        reason: reason.trim(),
      });
      if (response.data.success) {
        toast.success(response.data.message || 'Graduation date extended successfully');
        onSuccess();
      }
    } catch (err) {
      console.error('Failed to extend graduation date:', err);
      toast.error(err.response?.data?.message || 'Failed to extend graduation date');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={Boolean(enrollment)}
      onClose={onClose}
      title="Extend Graduation Date"
      size="md"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Student info box */}
        <div className="rounded-xl border border-[var(--border)] bg-[var(--bg-elevated)]/30 p-3 text-xs">
          <p className="font-semibold text-[var(--text-primary)]">{enrollment.studentName}</p>
          <p className="text-[var(--text-secondary)]">Enrollment #: {enrollment.enrollmentNumber}</p>
          <p className="mt-1 text-[var(--brand)] font-medium">
            Current Expected Date: {formatDate(enrollment.expectedGraduationDate)}
          </p>
        </div>

        {/* Date picker */}
        <div>
          <label className="mb-1 block text-xs font-semibold text-[var(--text-secondary)]">
            New Expected Graduation Date <span className="text-rose-500">*</span>
          </label>
          <input
            type="date"
            required
            value={newDate}
            min={currentDate ? new Date(new Date(currentDate).getTime() + 86400000).toISOString().split('T')[0] : undefined}
            onChange={(e) => setNewDate(e.target.value)}
            className={cn(
              'h-10 w-full rounded-lg border bg-[var(--bg-surface)] px-3 text-sm text-[var(--text-primary)] outline-none transition-all',
              isDateInvalid
                ? 'border-rose-500 focus:ring-2 focus:ring-rose-500/20'
                : 'border-[var(--border)] focus:border-[var(--brand)] focus:ring-2 focus:ring-[var(--brand)]/10'
            )}
          />
          {isDateInvalid && (
            <p className="mt-1 text-xs text-rose-500 font-medium">
              New date must be strictly after the current expected date ({formatDate(currentDate)}).
            </p>
          )}
        </div>

        {/* Reason Textarea */}
        <div>
          <div className="mb-1 flex items-center justify-between">
            <label className="text-xs font-semibold text-[var(--text-secondary)]">
              Reason for Extension <span className="text-rose-500">*</span>
            </label>
            <span
              className={cn(
                'text-[11px] font-medium',
                reason.trim().length < 10 ? 'text-rose-500' : 'text-emerald-600 dark:text-emerald-400'
              )}
            >
              {reason.trim().length} / 10 min chars
            </span>
          </div>
          <textarea
            required
            rows={3}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="Explain why this student's expected graduation date is being extended..."
            className="w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] p-3 text-sm text-[var(--text-primary)] outline-none focus:border-[var(--brand)] focus:ring-2 focus:ring-[var(--brand)]/10"
          />
        </div>

        <div className="flex justify-end gap-2 pt-4 border-t border-[var(--border)]">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" variant="primary" disabled={!canSubmit} loading={isSubmitting}>
            Confirm Extension
          </Button>
        </div>
      </form>
    </Modal>
  );
}

// =============================================================================
// MODAL 5: DIRECT WITHDRAW MODAL
// =============================================================================
function DirectWithdrawModal({ enrollment, onClose, onSuccess }) {
  const [reason, setReason] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const isReasonValid = reason.trim().length >= 20;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!isReasonValid) {
      toast.error('Withdrawal reason must be at least 20 characters');
      return;
    }

    setIsSubmitting(true);
    try {
      const response = await api.post(`/university/enrollments/${enrollment.id}/withdraw`, {
        reason: reason.trim(),
      });
      if (response.data.success) {
        toast.success(response.data.message || 'Student withdrawn successfully');
        onSuccess();
      }
    } catch (err) {
      console.error('Failed to withdraw student:', err);
      toast.error(err.response?.data?.message || 'Failed to withdraw student');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={Boolean(enrollment)}
      onClose={onClose}
      title="Withdraw Student"
      size="md"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Warning Banner */}
        <div className="rounded-xl border border-rose-300 bg-rose-50/70 p-3.5 text-xs text-rose-700 dark:border-rose-900/40 dark:bg-rose-950/30 dark:text-rose-300">
          <div className="flex items-center gap-2 font-bold">
            <AlertTriangle className="h-4 w-4 shrink-0" />
            Warning: Direct Withdrawal
          </div>
          <p className="mt-1">
            This will immediately withdraw <strong>{enrollment.studentName}</strong> ({enrollment.enrollmentNumber}) from your institution. The student will no longer be active.
          </p>
        </div>

        {/* Reason Textarea */}
        <div>
          <div className="mb-1 flex items-center justify-between">
            <label className="text-xs font-semibold text-[var(--text-secondary)]">
              Reason for Withdrawal <span className="text-rose-500">*</span>
            </label>
            <span
              className={cn(
                'text-[11px] font-medium',
                reason.trim().length < 20 ? 'text-rose-500' : 'text-emerald-600 dark:text-emerald-400'
              )}
            >
              {reason.trim().length} / 20 min chars
            </span>
          </div>
          <textarea
            required
            rows={4}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="Provide a formal reason for withdrawing this student (min 20 characters)..."
            className="w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] p-3 text-sm text-[var(--text-primary)] outline-none focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20"
          />
        </div>

        <div className="flex justify-end gap-2 pt-4 border-t border-[var(--border)]">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="danger"
            disabled={!isReasonValid}
            loading={isSubmitting}
            className="bg-rose-600 hover:bg-rose-700 text-white"
          >
            Confirm Withdrawal
          </Button>
        </div>
      </form>
    </Modal>
  );
}

// =============================================================================
// MODAL 6: RESPOND TO WITHDRAWAL MODAL
// =============================================================================
function RespondWithdrawalModal({ enrollment, onClose, onSuccess }) {
  const [decision, setDecision] = useState(null); // 'approve' | 'reject'
  const [responseMessage, setResponseMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const isMessageValid = responseMessage.trim().length >= 10;
  const canSubmit = Boolean(decision && isMessageValid);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!canSubmit) return;

    setIsSubmitting(true);
    try {
      const response = await api.post(`/university/enrollments/${enrollment.id}/respond-withdrawal`, {
        approved: decision === 'approve',
        responseMessage: responseMessage.trim(),
      });

      if (response.data.success) {
        toast.success(response.data.message || 'Withdrawal request responded successfully');
        window.dispatchEvent(new Event('withdrawal_requests_updated'));
        onSuccess();
      }
    } catch (err) {
      console.error('Failed to respond to withdrawal:', err);
      toast.error(err.response?.data?.message || 'Failed to respond to withdrawal request');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={Boolean(enrollment)}
      onClose={onClose}
      title="Respond to Student Withdrawal Request"
      size="md"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Student info & withdrawal reason */}
        <div className="rounded-xl border border-[var(--border)] bg-[var(--bg-elevated)]/30 p-3.5 space-y-2">
          <div className="flex items-center justify-between">
            <h4 className="text-sm font-bold text-[var(--text-primary)]">
              {enrollment.studentName}
            </h4>
            <span className="font-mono text-xs text-[var(--brand)]">
              {enrollment.enrollmentNumber}
            </span>
          </div>
          <div className="rounded-lg border border-amber-200/60 bg-amber-50/50 p-2.5 dark:border-amber-800/30 dark:bg-amber-950/20">
            <p className="text-[11px] font-semibold text-amber-700 dark:text-amber-400">
              Student's Reason for Request:
            </p>
            <p className="mt-0.5 text-xs italic text-[var(--text-secondary)]">
              "{enrollment.withdrawalReason || 'No reason provided.'}"
            </p>
          </div>
        </div>

        {/* Two choice buttons */}
        <div>
          <label className="mb-2 block text-xs font-semibold text-[var(--text-secondary)]">
            Select Your Decision:
          </label>
          <div className="grid grid-cols-2 gap-3">
            <button
              type="button"
              onClick={() => setDecision('approve')}
              className={cn(
                'flex items-center justify-center gap-2 rounded-xl border-2 py-3 px-4 text-xs font-bold transition-all',
                decision === 'approve'
                  ? 'border-emerald-500 bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300 shadow-sm'
                  : 'border-[var(--border)] bg-[var(--bg-surface)] text-[var(--text-secondary)] hover:border-emerald-400 hover:text-emerald-600'
              )}
            >
              <CheckCircle2 className="h-4 w-4" />
              Approve Withdrawal
            </button>

            <button
              type="button"
              onClick={() => setDecision('reject')}
              className={cn(
                'flex items-center justify-center gap-2 rounded-xl border-2 py-3 px-4 text-xs font-bold transition-all',
                decision === 'reject'
                  ? 'border-rose-500 bg-rose-50 text-rose-700 dark:bg-rose-950/40 dark:text-rose-300 shadow-sm'
                  : 'border-[var(--border)] bg-[var(--bg-surface)] text-[var(--text-secondary)] hover:border-rose-400 hover:text-rose-600'
              )}
            >
              <XCircle className="h-4 w-4" />
              Reject Request
            </button>
          </div>
        </div>

        {/* Response message textarea (shown when decision is picked) */}
        {decision && (
          <div className="animate-in fade-in">
            <div className="mb-1 flex items-center justify-between">
              <label className="text-xs font-semibold text-[var(--text-secondary)]">
                Response Message ({decision === 'approve' ? 'Confirmation Notes' : 'Rejection Reason'}){' '}
                <span className="text-rose-500">*</span>
              </label>
              <span
                className={cn(
                  'text-[11px] font-medium',
                  responseMessage.trim().length < 10 ? 'text-rose-500' : 'text-emerald-600 dark:text-emerald-400'
                )}
              >
                {responseMessage.trim().length} / 10 min chars
              </span>
            </div>
            <textarea
              required
              rows={3}
              value={responseMessage}
              onChange={(e) => setResponseMessage(e.target.value)}
              placeholder={
                decision === 'approve'
                  ? 'Enter final remarks for student withdrawal approval...'
                  : 'Enter the rationale for rejecting the withdrawal request...'
              }
              className="w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] p-3 text-sm text-[var(--text-primary)] outline-none focus:border-[var(--brand)] focus:ring-2 focus:ring-[var(--brand)]/10"
            />
          </div>
        )}

        <div className="flex justify-end gap-2 pt-4 border-t border-[var(--border)]">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant={decision === 'approve' ? 'success' : decision === 'reject' ? 'danger' : 'primary'}
            disabled={!canSubmit}
            loading={isSubmitting}
          >
            Submit Decision
          </Button>
        </div>
      </form>
    </Modal>
  );
}
