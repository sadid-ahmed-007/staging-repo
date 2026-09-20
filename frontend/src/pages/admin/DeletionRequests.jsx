import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { Trash2, CheckCircle, XCircle, Clock, User, RefreshCw } from 'lucide-react';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/shared/Card';
import Button from '../../components/shared/Button';
import Badge from '../../components/shared/Badge';
import LoadingSpinner from '../../components/shared/LoadingSpinner';
import EmptyState from '../../components/shared/EmptyState';
import api from '../../services/api';
import { formatDateTime, cn } from '../../utils/helpers';
import { useNavigate } from 'react-router-dom';

const STATUS_TABS = [
 { id: 'pending', label: 'Pending', icon: Clock },
 { id: 'completed', label: 'Completed', icon: CheckCircle },
 { id: 'cancelled', label: 'Cancelled', icon: XCircle },
];

export default function DeletionRequests() {
 const navigate = useNavigate();
 const [status, setStatus] = useState('pending');
 const [requests, setRequests] = useState([]);
 const [loading, setLoading] = useState(true);
 const [actionLoading, setActionLoading] = useState(null);

 const fetchRequests = async () => {
 setLoading(true);
 try {
 const { data } = await api.get('/admin/deletion-requests', { params: { status } });
 setRequests(data.data || []);
 } catch {
 toast.error('Failed to load deletion requests.');
 } finally {
 setLoading(false);
 }
 };

 useEffect(() => { fetchRequests(); }, [status]);

 const handleComplete = async (id) => {
 if (!window.confirm('Permanently delete this account? This cannot be undone.')) return;
 setActionLoading(id + '-complete');
 try {
 await api.post(`/admin/deletion-requests/${id}/complete`);
 toast.success('Account deletion completed.');
 setRequests(prev => prev.filter(r => r.id !== id));
 } catch (err) {
 toast.error(err.response?.data?.message || 'Failed to complete deletion.');
 } finally {
 setActionLoading(null);
 }
 };

 const handleCancel = async (id) => {
 setActionLoading(id + '-cancel');
 try {
 await api.post(`/admin/deletion-requests/${id}/cancel`);
 toast.success('Deletion request cancelled. Account reactivated.');
 setRequests(prev => prev.filter(r => r.id !== id));
 } catch (err) {
 toast.error(err.response?.data?.message || 'Failed to cancel request.');
 } finally {
 setActionLoading(null);
 }
 };

 const roleColor = (role) => {
 if (role === 'student') return 'primary';
 if (role === 'university') return 'warning';
 if (role === 'verifier') return 'info';
 return 'default';
 };

 return (
 <DashboardLayout>
 <div className="mx-auto max-w-5xl space-y-6">
 {/* Header */}
 <div className="flex items-start justify-between">
 <div>
 <h1 className="text-2xl font-bold tracking-tight text-[var(--text-primary)] sm:text-3xl">
 Account Deletion Requests
 </h1>
 <p className="mt-1 text-sm text-[var(--text-secondary)]">
 Review and action user-submitted account deletion requests
 </p>
 </div>
 <Button variant="secondary" onClick={fetchRequests} disabled={loading}>
 <RefreshCw className={cn("w-4 h-4 mr-2", loading && "animate-spin")} />
 Refresh
 </Button>
 </div>

 {/* Status Tabs */}
 <div className="flex gap-1 rounded-xl bg-[var(--bg-elevated)] p-1">
 {STATUS_TABS?.map(({ id, label, icon: Icon }) => (
 <button
 key={id}
 onClick={() => setStatus(id)}
 className={cn(
 "flex flex-1 items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition-all",
 status === id
 ? "bg-[var(--bg-surface)] text-[var(--text-primary)] shadow-sm"
 : "text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
 )}
 >
 <Icon className="w-4 h-4" />
 <span className="hidden sm:inline">{label}</span>
 </button>
 ))}
 </div>

 {/* Content */}
 <Card>
 {loading ? (
 <div className="flex justify-center py-16">
 <LoadingSpinner />
 </div>
 ) : requests.length === 0 ? (
 <div className="p-4">
 <EmptyState 
 icon={Trash2} 
 title={`No ${status} requests`}
 message={status === 'pending' ? "No accounts are awaiting deletion approval." : `No ${status} deletion requests found.`}
 />
 </div>
 ) : (
 <div className="divide-y divide-[var(--border)]">
 {requests?.map((req) => (
 <div key={req.id} className="flex flex-col sm:flex-row sm:items-center gap-4 p-5 hover:bg-[var(--bg-elevated)] transition-colors">
 {/* User info */}
 <div className="flex items-center gap-3 flex-1 min-w-0">
 <div className="h-10 w-10 rounded-full bg-[var(--bg-elevated)] border border-[var(--border)] flex items-center justify-center shrink-0">
 <User className="w-5 h-5 text-[var(--text-muted)]" />
 </div>
 <div className="min-w-0">
 <div className="flex items-center gap-2 flex-wrap">
 <p
 className="text-sm font-semibold text-[var(--brand)] cursor-pointer hover:underline truncate"
 onClick={() => navigate(`/admin/users/${req.user_id}`)}
 >
 {req.user_email}
 </p>
 <Badge variant={roleColor(req.user_role)} className="capitalize text-[10px] shrink-0">
 {req.user_role}
 </Badge>
 </div>
 {req.reason && (
 <p className="text-xs text-[var(--text-secondary)] mt-0.5 truncate" title={req.reason}>
 Reason: {req.reason}
 </p>
 )}
 <p className="text-xs text-[var(--text-muted)] mt-0.5">
 Requested {formatDateTime(req.requested_at)}
 {req.completed_at && ` · ${status === 'completed' ? 'Completed' : 'Cancelled'} ${formatDateTime(req.completed_at)}`}
 </p>
 </div>
 </div>

 {/* Actions */}
 {status === 'pending' && (
 <div className="flex items-center gap-2 shrink-0">
 <Button
 variant="secondary"
 size="sm"
 onClick={() => handleCancel(req.id)}
 loading={actionLoading === req.id + '-cancel'}
 disabled={!!actionLoading}
 >
 <XCircle className="w-4 h-4 mr-1.5" />
 Cancel Request
 </Button>
 <button
 onClick={() => handleComplete(req.id)}
 disabled={!!actionLoading}
 className="flex items-center gap-1.5 px-3 py-2 text-sm font-semibold rounded-lg bg-red-600 text-white hover:bg-red-700 transition-colors disabled:opacity-50"
 >
 {actionLoading === req.id + '-complete' ? (
 <RefreshCw className="w-4 h-4 animate-spin" />
 ) : (
 <Trash2 className="w-4 h-4" />
 )}
 Delete Account
 </button>
 </div>
 )}

 {status !== 'pending' && (
 <Badge variant={status === 'completed' ? 'danger' : 'warning'} className="shrink-0 capitalize">
 {status}
 </Badge>
 )}
 </div>
 ))}
 </div>
 )}
 </Card>
 </div>
 </DashboardLayout>
 );
}
