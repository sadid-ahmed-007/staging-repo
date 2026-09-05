import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import {
  Users, Search, ChevronLeft, ChevronRight,
  GraduationCap, Building2, ShieldCheck, UserCog,
  CheckCircle, XCircle, Clock, X, RefreshCw, UserCheck, Ban, RotateCcw
} from 'lucide-react';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/shared/Card';
import Button from '../../components/shared/Button';
import Badge from '../../components/shared/Badge';
import LoadingSpinner from '../../components/shared/LoadingSpinner';
import Modal from '../../components/shared/Modal';
import api from '../../services/api';
import { formatDate } from '../../utils/helpers';

const STATUS_TABS = [
  { value: 'all', label: 'All' },
  { value: 'pending', label: 'Pending' },
  { value: 'approved', label: 'Approved' },
  { value: 'suspended', label: 'Suspended' }
];

const ROLE_OPTIONS = [
  { value: 'all', label: 'All Roles' },
  { value: 'student', label: 'Students' },
  { value: 'university', label: 'Universities' },
  { value: 'verifier', label: 'Verifiers' },
];

const ROLE_CONFIG = {
  student:    { label: 'Student',    color: 'info', icon: GraduationCap },
  university: { label: 'University', color: 'success', icon: Building2 },
  verifier:   { label: 'Verifier',   color: 'warning', icon: ShieldCheck },
  admin:      { label: 'Admin',      color: 'danger',   icon: UserCog },
};

const PER_PAGE_OPTIONS = [25, 50, 100];

function timeAgo(iso) {
  if (!iso) return '';
  const diff = Math.floor((Date.now() - new Date(iso).getTime()) / 1000);
  if (diff < 60) return `${diff}s ago`;
  if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
  if (diff < 2592000) return `${Math.floor(diff / 86400)}d ago`;
  return `${Math.floor(diff / 2592000)}mo ago`;
}

function RoleBadge({ role }) {
  const cfg = ROLE_CONFIG[role] || { label: role, color: 'default' };
  return <Badge variant={cfg.color}>{cfg.label}</Badge>;
}

function RoleIcon({ role, className = '' }) {
  const cfg = ROLE_CONFIG[role];
  if (!cfg) return null;
  const Icon = cfg.icon;
  return <Icon className={className} />;
}

export default function AdminUsers() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const searchInputRef = useRef(null);
  const debounceRef = useRef(null);

  // Data
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [lastPage, setLastPage] = useState(1);
  const [perPage, setPerPage] = useState(25);
  const [pendingCount, setPendingCount] = useState(0);

  // Filters
  const [statusTab, setStatusTab] = useState(searchParams.get('status') || 'all');
  const [role, setRole] = useState(searchParams.get('role') || 'all');
  const [searchQuery, setSearchQuery] = useState(searchParams.get('search') || '');

  // Action modals
  const [suspendModal, setSuspendModal] = useState(null);
  const [suspendReason, setSuspendReason] = useState('');
  const [actionLoading, setActionLoading] = useState(null);

  const fetchUsers = useCallback(async (page = 1, overrides = {}) => {
    setLoading(true);
    try {
      const params = {
        page: page - 1, // Spring Boot is 0-indexed
        size: perPage,
        status: overrides.status ?? statusTab,
        role: overrides.role ?? role,
        search: overrides.search ?? searchQuery,
      };
      
      // Clean empty params
      Object.keys(params).forEach((k) => { if (params[k] === '' || params[k] === 'all') delete params[k]; });
      
      const { data } = await api.get('/admin/users', { params });
      
      const pageData = data.data;
      setUsers(pageData.content || []);
      setTotal(pageData.totalElements || 0);
      setCurrentPage((pageData.number + 1) || 1);
      setLastPage(pageData.totalPages || 1);
      
      // Update pending count if we are on 'pending' tab
      if ((overrides.status ?? statusTab) === 'pending') {
          setPendingCount(pageData.totalElements || 0);
      }
    } catch {
      toast.error('Failed to load users.');
    } finally {
      setLoading(false);
    }
  }, [perPage, statusTab, role, searchQuery]);

  useEffect(() => {
    const urlStatus = searchParams.get('status') || 'all';
    const urlRole = searchParams.get('role') || 'all';
    const urlSearch = searchParams.get('search') || '';
    setStatusTab(urlStatus);
    setRole(urlRole);
    setSearchQuery(urlSearch);
  }, [searchParams]);

  useEffect(() => {
    fetchUsers(1);
  }, [statusTab, role, perPage]);

  // Initial fetch for pending count if we didn't start on the pending tab
  useEffect(() => {
      if (statusTab !== 'pending') {
          api.get('/admin/users', { params: { status: 'pending', size: 1, page: 0 }})
             .then(({ data }) => setPendingCount(data.data.totalElements || 0))
             .catch(() => {});
      }
  }, [statusTab]);

  const handleSearchChange = (value) => {
    setSearchQuery(value);
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      fetchUsers(1, { search: value });
    }, 400);
  };

  const handleStatusTab = (tab) => {
    setStatusTab(tab);
    setSearchParams((prev) => { prev.set('status', tab); return prev; });
  };

  const handlePageChange = (page) => {
    fetchUsers(page);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleApprove = async (userId) => {
    setActionLoading(userId);
    try {
      await api.post(`/admin/users/${userId}/approve`);
      toast.success('User approved.');
      setPendingCount((prev) => Math.max(0, prev - 1));
      
      if (statusTab === 'pending') {
        setUsers((prev) => prev.filter((u) => u.id !== userId));
        setTotal((prev) => Math.max(0, prev - 1));
      } else {
        setUsers((prev) => prev.map((u) => u.id === userId ? { ...u, isApproved: true } : u));
      }
    } catch (err) {
      toast.error(err.response?.data?.error || 'Failed to approve.');
    } finally {
      setActionLoading(null);
    }
  };

  const handleSuspend = async () => {
    if (!suspendReason.trim()) return;
    setActionLoading(suspendModal);
    try {
      await api.post(`/admin/users/${suspendModal}/suspend`, { reason: suspendReason });
      toast.success('User suspended.');
      setUsers((prev) => prev.map((u) => u.id === suspendModal ? { ...u, isSuspended: true } : u));
      setSuspendModal(null);
      setSuspendReason('');
    } catch (err) {
      toast.error(err.response?.data?.error || 'Failed to suspend.');
    } finally {
      setActionLoading(null);
    }
  };

  const handleUnsuspend = async (userId) => {
    setActionLoading(userId);
    try {
      await api.post(`/admin/users/${userId}/unsuspend`);
      toast.success('User unsuspended.');
      setUsers((prev) => prev.map((u) => u.id === userId ? { ...u, isSuspended: false } : u));
    } catch (err) {
      toast.error(err.response?.data?.error || 'Failed to unsuspend.');
    } finally {
      setActionLoading(null);
    }
  };

  const clearFilters = () => {
    setRole('all');
    setSearchQuery('');
    setSearchParams((prev) => {
      const nextParams = new URLSearchParams();
      if (statusTab && statusTab !== 'all') {
        nextParams.set('status', statusTab);
      }
      return nextParams;
    });
    fetchUsers(1, { status: statusTab, role: 'all', search: '' });
  };

  const hasActiveFilters = role !== 'all' || searchQuery;

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.15em] text-[var(--brand)]">Admin</p>
            <h1 className="mt-2 text-3xl font-bold text-[var(--text-primary)] flex items-center gap-3">
              <Users className="h-7 w-7 text-[var(--brand)]" />
              User Management
            </h1>
            <p className="mt-1 text-sm text-[var(--text-secondary)]">
              {loading ? 'Loading...' : `${total} user${total !== 1 ? 's' : ''} total`}
            </p>
          </div>
          <div className="flex gap-3">
            <Button variant="secondary" onClick={() => fetchUsers(currentPage)} disabled={loading}>
              <RefreshCw className={`mr-2 h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
              Refresh
            </Button>
          </div>
        </div>

        {/* Filter bar */}
        <Card>
          {/* Status tabs */}
          <div className="flex gap-1 p-1 bg-[var(--bg-elevated)] rounded-xl mb-4">
            {STATUS_TABS.map(({ value, label }) => (
              <button
                key={value}
                onClick={() => handleStatusTab(value)}
                className={`flex-1 rounded-lg px-4 py-2 text-sm font-medium transition ${
                  statusTab === value
                    ? 'bg-[var(--bg-base)] text-[var(--text-primary)] shadow-sm'
                    : 'text-[var(--text-secondary)] hover:text-[var(--text-primary)]'
                }`}
              >
                {label}
                {value === 'pending' && pendingCount > 0 && statusTab !== 'pending' && (
                  <span className="ml-1.5 inline-flex h-5 min-w-[20px] items-center justify-center rounded-full bg-[var(--warning)]/10 text-[10px] font-bold text-[var(--warning)] px-1.5">!</span>
                )}
              </button>
            ))}
          </div>

          <div className="grid gap-4 grid-cols-1 md:grid-cols-12">
            {/* Search */}
            <div className="md:col-span-8 lg:col-span-8">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[var(--text-muted)]" />
                <input
                  ref={searchInputRef}
                  value={searchQuery}
                  onChange={(e) => handleSearchChange(e.target.value)}
                  placeholder="Search by name, email..."
                  className="w-full rounded-xl border border-[var(--border)] bg-[var(--bg-base)] pl-10 pr-10 py-2.5 text-sm text-[var(--text-primary)] placeholder-[var(--text-muted)] focus:outline-none focus:ring-1 focus:ring-[var(--brand)] focus:border-[var(--brand)]"
                />
                {searchQuery && (
                  <button
                    onClick={() => handleSearchChange('')}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)] hover:text-[var(--text-secondary)]"
                  >
                    <X className="h-4 w-4" />
                  </button>
                )}
              </div>
            </div>

            {/* Role */}
            <div className="md:col-span-4 lg:col-span-4">
              <select
                value={role}
                onChange={(e) => setRole(e.target.value)}
                className="w-full rounded-xl border border-[var(--border)] bg-[var(--bg-base)] px-3 py-2.5 text-sm text-[var(--text-primary)] focus:outline-none focus:ring-1 focus:ring-[var(--brand)] focus:border-[var(--brand)]"
              >
                {ROLE_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
              </select>
            </div>
          </div>

          {hasActiveFilters && (
            <div className="mt-3 flex items-center gap-3">
              <button
                onClick={clearFilters}
                className="flex items-center gap-1.5 text-xs font-medium text-[var(--brand)] hover:text-[var(--brand-dark)]"
              >
                <X className="h-3 w-3" /> Clear all filters
              </button>
            </div>
          )}
        </Card>

        {/* Table */}
        <Card>
          {loading ? (
            <div className="flex justify-center py-16">
              <LoadingSpinner />
            </div>
          ) : users.length === 0 ? (
            <div className="flex flex-col items-center py-16 gap-4 text-center">
              <div className="h-16 w-16 flex items-center justify-center rounded-full bg-[var(--bg-elevated)]">
                <Users className="h-8 w-8 text-[var(--text-muted)]" />
              </div>
              <div>
                <p className="font-semibold text-[var(--text-primary)]">No users found</p>
                <p className="text-sm text-[var(--text-secondary)] mt-1">
                  {hasActiveFilters ? 'Try adjusting your filters.' : 'No users in the system yet.'}
                </p>
              </div>
            </div>
          ) : (
            <div className="overflow-x-auto -mx-4 sm:mx-0">
              <table className="w-full min-w-[800px] text-sm">
                <thead>
                  <tr className="border-b border-[var(--border)]">
                    {['User', 'Status', 'Joined', 'Actions'].map((h) => (
                      <th key={h} className="py-3 px-4 text-left text-xs font-semibold uppercase tracking-wide text-[var(--text-muted)] first:pl-0 last:text-right">
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--border)]">
                  {users.map((user) => {
                    const cfg = ROLE_CONFIG[user.role] || {};
                    return (
                      <tr key={user.id} className="hover:bg-[var(--bg-hover)] transition group">
                        {/* User info */}
                        <td className="px-4 py-4 pl-0">
                          <div className="flex items-center gap-4">
                            <div className={`flex h-11 w-11 flex-shrink-0 items-center justify-center rounded-full bg-[var(--${cfg.color})]/10`}>
                              <RoleIcon role={user.role} className={`h-5 w-5 text-[var(--${cfg.color})]`} />
                            </div>
                            <div className="min-w-0 space-y-1">
                              <p className="text-sm font-medium text-[var(--text-primary)] truncate">
                                {user.profile || 'N/A'}
                              </p>
                              <p className="text-xs text-[var(--text-secondary)] truncate">{user.email}</p>
                              <div>
                                <RoleBadge role={user.role} />
                              </div>
                            </div>
                          </div>
                        </td>

                        {/* Status */}
                        <td className="px-4 py-4">
                          <div className="space-y-1">
                            <div className="flex items-center gap-1.5">
                              {user.isEmailVerified ? (
                                <CheckCircle className="h-3.5 w-3.5 text-[var(--success)]" />
                              ) : (
                                <XCircle className="h-3.5 w-3.5 text-[var(--text-muted)]" />
                              )}
                              <span className="text-xs text-[var(--text-secondary)]">Email</span>
                            </div>
                            <div className="flex items-center gap-1.5">
                              {user.isApproved ? (
                                <CheckCircle className="h-3.5 w-3.5 text-[var(--success)]" />
                              ) : (
                                <Clock className="h-3.5 w-3.5 text-[var(--warning)]" />
                              )}
                              <span className="text-xs text-[var(--text-secondary)]">
                                {user.isApproved ? 'Approved' : 'Pending'}
                              </span>
                            </div>
                            {user.isSuspended && (
                              <div className="mt-1">
                                <Badge variant="danger">Suspended</Badge>
                              </div>
                            )}
                          </div>
                        </td>

                        {/* Joined */}
                        <td className="px-4 py-4">
                          <div>
                            <p className="text-xs text-[var(--text-primary)]">
                              {user.createdAt ? formatDate(user.createdAt) : '—'}
                            </p>
                            <p className="text-[10px] text-[var(--text-muted)]">{timeAgo(user.createdAt)}</p>
                          </div>
                        </td>

                        {/* Actions */}
                        <td className="px-4 py-4 text-right">
                          <div className="flex items-center justify-end gap-2">
                            {!user.isApproved && user.role !== 'admin' && (
                              <button
                                onClick={() => handleApprove(user.id)}
                                disabled={actionLoading === user.id}
                                className="inline-flex items-center gap-1.5 rounded-lg bg-[var(--success)]/10 text-[var(--success)] hover:bg-[var(--success)]/20 px-3 py-1.5 text-xs font-medium transition"
                              >
                                <UserCheck className="h-3.5 w-3.5" /> Approve
                              </button>
                            )}

                            {user.isApproved && user.role !== 'admin' && !user.isSuspended && (
                              <button
                                onClick={() => setSuspendModal(user.id)}
                                className="inline-flex items-center gap-1.5 rounded-lg border border-[var(--danger)]/30 px-3 py-1.5 text-xs font-medium text-[var(--danger)] hover:bg-[var(--danger)]/10 transition"
                                title="Suspend User"
                              >
                                <Ban className="h-3.5 w-3.5" /> Suspend
                              </button>
                            )}
                            
                            {user.isApproved && user.role !== 'admin' && user.isSuspended && (
                              <button
                                onClick={() => handleUnsuspend(user.id)}
                                disabled={actionLoading === user.id}
                                className="inline-flex items-center gap-1.5 rounded-lg border border-[var(--success)]/30 px-3 py-1.5 text-xs font-medium text-[var(--success)] hover:bg-[var(--success)]/10 transition"
                                title="Unsuspend User"
                              >
                                <RotateCcw className="h-3.5 w-3.5" /> Unsuspend
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}

          {/* Pagination */}
          <div className="mt-6 flex flex-col sm:flex-row items-center justify-between gap-4 border-t border-[var(--border)] pt-4">
            <div className="flex items-center gap-3">
              <p className="text-sm text-[var(--text-secondary)]">
                Page {currentPage} of {Math.max(1, lastPage)} · {total} users
              </p>
              <select
                value={perPage}
                onChange={(e) => setPerPage(Number(e.target.value))}
                className="rounded-lg border border-[var(--border)] bg-[var(--bg-base)] px-2 py-1 text-xs text-[var(--text-primary)]"
              >
                {PER_PAGE_OPTIONS.map((n) => (
                  <option key={n} value={n}>{n}/page</option>
                ))}
              </select>
            </div>
            <div className="flex gap-2">
              <Button variant="secondary" onClick={() => handlePageChange(currentPage - 1)} disabled={currentPage <= 1 || loading}>
                <ChevronLeft className="h-4 w-4 mr-1" /> Prev
              </Button>
              <div className="hidden sm:flex gap-1">
                {Array.from({ length: Math.min(5, Math.max(1, lastPage)) }, (_, i) => {
                  const p = Math.max(1, Math.min(currentPage - 2, Math.max(1, lastPage) - 4)) + i;
                  if (p > Math.max(1, lastPage)) return null;
                  return (
                    <button
                      key={p}
                      onClick={() => handlePageChange(p)}
                      className={`h-9 w-9 rounded-lg text-sm font-medium transition ${
                        p === currentPage
                          ? 'bg-[var(--brand)] text-white'
                          : 'border border-[var(--border)] text-[var(--text-primary)] hover:bg-[var(--bg-hover)]'
                      }`}
                    >
                      {p}
                    </button>
                  );
                })}
              </div>
              <Button variant="secondary" onClick={() => handlePageChange(currentPage + 1)} disabled={currentPage >= Math.max(1, lastPage) || loading}>
                Next <ChevronRight className="h-4 w-4 ml-1" />
              </Button>
            </div>
          </div>
        </Card>
      </div>

      {/* Suspend Modal */}
      <Modal
        open={!!suspendModal}
        onClose={() => { setSuspendModal(null); setSuspendReason(''); }}
        title="Suspend User"
        size="sm"
      >
        <div className="space-y-4">
          <p className="text-sm text-[var(--text-secondary)]">
            This will suspend the user, preventing them from logging in.
          </p>
          <textarea
            value={suspendReason}
            onChange={(e) => setSuspendReason(e.target.value)}
            placeholder="Reason for suspension..."
            rows={3}
            className="w-full rounded-xl border border-[var(--border)] bg-[var(--bg-base)] px-4 py-3 text-sm text-[var(--text-primary)] placeholder-[var(--text-muted)] focus:outline-none focus:ring-1 focus:ring-[var(--brand)] focus:border-[var(--brand)] resize-none"
          />
          <div className="flex justify-end gap-3">
            <Button variant="secondary" onClick={() => { setSuspendModal(null); setSuspendReason(''); }}>
              Cancel
            </Button>
            <Button
              variant="danger"
              onClick={handleSuspend}
              loading={actionLoading === suspendModal}
              disabled={!suspendReason.trim()}
            >
              Suspend User
            </Button>
          </div>
        </div>
      </Modal>
    </DashboardLayout>
  );
}
