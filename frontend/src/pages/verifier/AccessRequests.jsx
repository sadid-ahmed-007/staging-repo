import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import {
  FileText,
  Clock,
  CheckCircle2,
  XCircle,
  Ban,
  ChevronLeft,
  ChevronRight,
  Eye,
  AlertTriangle,
  RefreshCw,
  Search,
  ExternalLink,
  Info
} from 'lucide-react';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/shared/Card';
import Badge from '../../components/shared/Badge';
import Button from '../../components/shared/Button';
import LoadingSpinner from '../../components/shared/LoadingSpinner';
import EmptyState from '../../components/shared/EmptyState';
import ConfirmModal from '../../components/shared/ConfirmModal';
import api from '../../services/api';
import { formatDate } from '../../utils/helpers';

const TABS = [
  { id: 'all', label: 'All' },
  { id: 'pending', label: 'Pending' },
  { id: 'approved', label: 'Approved' },
  { id: 'rejected', label: 'Rejected' },
  { id: 'cancelled', label: 'Cancelled' },
];

function timeAgo(isoString) {
  if (!isoString) return '';
  const date = new Date(isoString);
  const now = new Date();
  const diffSec = Math.floor((now.getTime() - date.getTime()) / 1000);
  if (diffSec < 60) return 'Just now';
  if (diffSec < 3600) return `${Math.floor(diffSec / 60)}m ago`;
  if (diffSec < 86400) return `${Math.floor(diffSec / 3600)}h ago`;
  const days = Math.floor(diffSec / 86400);
  if (days < 30) return `${days}d ago`;
  return formatDate(isoString);
}

function isAccessFinished(req) {
  if (req.hasActiveAccess !== undefined && req.hasActiveAccess !== null) {
    return !req.hasActiveAccess;
  }
  if (req.accessExpiresAt) {
    return new Date(req.accessExpiresAt) <= new Date();
  }
  if (req.respondedAt && req.requestedDurationDays) {
    const expiry = new Date(req.respondedAt);
    expiry.setDate(expiry.getDate() + Number(req.requestedDurationDays));
    return expiry <= new Date();
  }
  return false;
}

export default function VerifierAccessRequests() {
  const navigate = useNavigate();

  const [activeTab, setActiveTab] = useState('all');
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 25;

  // Cancellation Modal State
  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [cancellingRequest, setCancellingRequest] = useState(null);
  const [cancellingLoading, setCancellingLoading] = useState(false);

  const fetchRequests = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get('/verifier/access-requests', {
        params: {
          status: activeTab,
          page,
          size: pageSize,
        },
      });

      if (data.success) {
        setRequests(data.data || []);
        setTotalPages(data.pages || 1);
        setTotalElements(data.total || 0);
      } else {
        toast.error(data.message || 'Failed to load access requests.');
      }
    } catch (err) {
      console.error('Error fetching access requests:', err);
      toast.error(err.response?.data?.message || 'Failed to load access requests.');
    } finally {
      setLoading(false);
    }
  }, [activeTab, page]);

  useEffect(() => {
    fetchRequests();
  }, [fetchRequests]);

  const handleTabChange = (tabId) => {
    setActiveTab(tabId);
    setPage(0);
  };

  const openCancelModal = (request) => {
    setCancellingRequest(request);
    setCancelModalOpen(true);
  };

  const handleConfirmCancel = async () => {
    if (!cancellingRequest) return;
    setCancellingLoading(true);
    try {
      const { data } = await api.delete(`/verifier/access-requests/${cancellingRequest.id}`);
      if (data.success) {
        toast.success('Access request cancelled successfully');
        setCancelModalOpen(false);
        setCancellingRequest(null);
        fetchRequests();
      } else {
        toast.error(data.message || 'Failed to cancel request');
      }
    } catch (err) {
      console.error('Failed to cancel request:', err);
      toast.error(err.response?.data?.message || 'Failed to cancel request');
    } finally {
      setCancellingLoading(false);
    }
  };

  const renderStatusBadge = (status, accessFinished) => {
    switch (status?.toLowerCase()) {
      case 'pending':
        return (
          <Badge variant="warning" size="sm" className="capitalize">
            <Clock className="w-3 h-3 mr-1" />
            Pending
          </Badge>
        );
      case 'approved':
        if (accessFinished) {
          return (
            <Badge variant="default" size="sm" className="capitalize">
              <Clock className="w-3 h-3 mr-1" />
              Access Expired
            </Badge>
          );
        }
        return (
          <Badge variant="success" size="sm" className="capitalize">
            <CheckCircle2 className="w-3 h-3 mr-1" />
            Approved
          </Badge>
        );
      case 'rejected':
        return (
          <Badge variant="danger" size="sm" className="capitalize">
            <XCircle className="w-3 h-3 mr-1" />
            Rejected
          </Badge>
        );
      case 'cancelled':
        return (
          <Badge variant="default" size="sm" className="capitalize">
            <Ban className="w-3 h-3 mr-1" />
            Cancelled
          </Badge>
        );
      default:
        return (
          <Badge variant="default" size="sm" className="capitalize">
            {status || 'Unknown'}
          </Badge>
        );
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-[var(--text-primary)] tracking-tight">
              My Access Requests
            </h1>
            <p className="text-sm text-[var(--text-secondary)] mt-1">
              Track and manage all certificate access requests sent to students
            </p>
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={fetchRequests}
              disabled={loading}
            >
              <RefreshCw className={`w-4 h-4 mr-1.5 ${loading ? 'animate-spin' : ''}`} />
              Refresh
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={() => navigate('/verifier/search')}
            >
              <Search className="w-4 h-4 mr-1.5" />
              Search Student
            </Button>
          </div>
        </div>

        {/* Tabs Bar */}
        <div className="flex border-b border-[var(--border)] gap-2 sm:gap-6 overflow-x-auto">
          {TABS.map((tab) => {
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => handleTabChange(tab.id)}
                className={`flex items-center gap-2 pb-3 px-2 text-sm font-medium border-b-2 whitespace-nowrap transition-all ${
                  isActive
                    ? 'border-[var(--brand)] text-[var(--brand)]'
                    : 'border-transparent text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:border-[var(--border-strong)]'
                }`}
              >
                {tab.label}
              </button>
            );
          })}
        </div>

        {/* Content Body */}
        {loading ? (
          <div className="flex min-h-[40vh] items-center justify-center">
            <LoadingSpinner />
          </div>
        ) : requests.length === 0 ? (
          <EmptyState
            title={activeTab === 'all' ? 'No access requests yet' : `No ${activeTab} access requests`}
            message={
              activeTab === 'pending'
                ? "You don't have any requests currently awaiting a student's response."
                : activeTab === 'approved'
                ? "No access requests have been approved yet."
                : "You haven't submitted any access requests in this category."
            }
            icon={FileText}
            action={
              <Button
                variant="primary"
                size="sm"
                onClick={() => navigate('/verifier/search')}
                className="mt-4"
              >
                Search Students
              </Button>
            }
          />
        ) : (
          <Card className="p-0 overflow-hidden border border-[var(--border)] shadow-sm">
            {/* Table */}
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-sm">
                <thead>
                  <tr className="border-b border-[var(--border)] bg-[var(--bg-elevated)]/50 text-xs font-semibold uppercase tracking-wider text-[var(--text-secondary)]">
                    <th className="py-3.5 px-4 sm:px-6">Student Name</th>
                    <th className="py-3.5 px-4 sm:px-6">Purpose</th>
                    <th className="py-3.5 px-4 sm:px-6">Requested Duration</th>
                    <th className="py-3.5 px-4 sm:px-6">Status</th>
                    <th className="py-3.5 px-4 sm:px-6">Requested On</th>
                    <th className="py-3.5 px-4 sm:px-6">Responded On</th>
                    <th className="py-3.5 px-4 sm:px-6 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--border)]">
                  {requests.map((req) => {
                    const isPending = req.status === 'pending';
                    const isApproved = req.status === 'approved';
                    const accessFinished = isApproved && isAccessFinished(req);
                    const truncatedPurpose =
                      req.purpose && req.purpose.length > 60
                        ? `${req.purpose.slice(0, 60)}...`
                        : req.purpose;

                    return (
                      <tr
                        key={req.id}
                        className="hover:bg-[var(--bg-elevated)]/30 transition-colors"
                      >
                        {/* Student Name */}
                        <td className="py-4 px-4 sm:px-6 font-medium text-[var(--text-primary)]">
                          <div className="flex items-center gap-2">
                            <span className="truncate">{req.studentName || 'Unknown Student'}</span>
                          </div>
                        </td>

                        {/* Purpose */}
                        <td className="py-4 px-4 sm:px-6 text-[var(--text-secondary)] max-w-xs">
                          <span
                            className="cursor-help inline-block truncate max-w-[240px]"
                            title={req.purpose}
                          >
                            {truncatedPurpose || '—'}
                          </span>
                        </td>

                        {/* Duration */}
                        <td className="py-4 px-4 sm:px-6 text-[var(--text-primary)] font-medium whitespace-nowrap">
                          {req.requestedDurationDays ? `${req.requestedDurationDays} days` : '—'}
                        </td>

                        {/* Status */}
                        <td className="py-4 px-4 sm:px-6 whitespace-nowrap">
                          {renderStatusBadge(req.status, accessFinished)}
                        </td>

                        {/* Requested On */}
                        <td className="py-4 px-4 sm:px-6 text-[var(--text-secondary)] whitespace-nowrap">
                          <span title={req.requestedAt ? new Date(req.requestedAt).toLocaleString() : ''}>
                            {timeAgo(req.requestedAt)}
                          </span>
                        </td>

                        {/* Responded On */}
                        <td className="py-4 px-4 sm:px-6 text-[var(--text-secondary)] whitespace-nowrap">
                          {req.respondedAt ? (
                            <span title={new Date(req.respondedAt).toLocaleString()}>
                              {timeAgo(req.respondedAt)}
                            </span>
                          ) : isPending ? (
                            <span className="text-xs text-[var(--text-muted)] italic">
                              Awaiting response
                            </span>
                          ) : (
                            '—'
                          )}
                        </td>

                        {/* Actions */}
                        <td className="py-4 px-4 sm:px-6 text-right whitespace-nowrap">
                          {isPending ? (
                            <Button
                              variant="secondary"
                              size="sm"
                              onClick={() => openCancelModal(req)}
                              className="text-[var(--danger)] hover:bg-[var(--danger)]/10 border-[var(--danger)]/30"
                            >
                              Cancel
                            </Button>
                          ) : isApproved && !accessFinished ? (
                            <Button
                              variant="primary"
                              size="sm"
                              onClick={() =>
                                navigate(
                                  req.studentId
                                    ? `/verifier/accessible-certificates/${req.studentId}`
                                    : '/verifier/accessible-certificates'
                                )
                              }
                            >
                              <Eye className="w-3.5 h-3.5 mr-1" />
                              View Certificates
                            </Button>
                          ) : (
                            <span className="text-[var(--text-muted)] text-sm">—</span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Pagination Controls */}
            {totalPages > 1 && (
              <div className="flex flex-col sm:flex-row items-center justify-between gap-4 p-4 border-t border-[var(--border)] bg-[var(--bg-elevated)]/20 text-xs text-[var(--text-secondary)]">
                <div>
                  Showing {page * pageSize + 1} to{' '}
                  {Math.min((page + 1) * pageSize, totalElements)} of {totalElements} requests
                </div>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0 || loading}
                    className="h-8 px-2.5"
                  >
                    <ChevronLeft className="w-4 h-4 mr-1" />
                    Previous
                  </Button>
                  <span className="px-2 font-medium text-[var(--text-primary)]">
                    Page {page + 1} of {totalPages}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                    disabled={page >= totalPages - 1 || loading}
                    className="h-8 px-2.5"
                  >
                    Next
                    <ChevronRight className="w-4 h-4 ml-1" />
                  </Button>
                </div>
              </div>
            )}
          </Card>
        )}

        {/* Cancellation Confirmation Dialog */}
        <ConfirmModal
          isOpen={cancelModalOpen}
          onClose={() => !cancellingLoading && setCancelModalOpen(false)}
          onConfirm={handleConfirmCancel}
          title="Cancel Access Request"
          message={`Are you sure you want to cancel your access request for ${
            cancellingRequest?.studentName || 'this student'
          }? This action will permanently retract the request.`}
          confirmText="Yes, Cancel Request"
          confirmVariant="danger"
          isLoading={cancellingLoading}
        />
      </div>
    </DashboardLayout>
  );
}
