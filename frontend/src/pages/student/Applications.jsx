import React, { useState, useEffect, useCallback } from 'react';
import { Building2, XCircle, CheckCircle2, Clock, Trash2, X } from 'lucide-react';
import toast from 'react-hot-toast';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/shared/Card';
import Button from '../../components/shared/Button';
import api from '../../services/api';

const TABS = [
 { id: 'all', label: 'All Applications' },
 { id: 'pending', label: 'Pending' },
 { id: 'accepted', label: 'Accepted' },
 { id: 'rejected', label: 'Rejected' },
 { id: 'cancelled', label: 'Cancelled' },
];

export default function StudentApplications() {
 const [activeTab, setActiveTab] = useState('all');
 const [applications, setApplications] = useState([]);
 const [loading, setLoading] = useState(true);
 const [cancellingId, setCancellingId] = useState(null);

 const fetchApplications = useCallback(async () => {
 setLoading(true);
 try {
 const { data } = await api.get('/student/applications', {
 params: { status: activeTab, size: 50 } // Load enough for most students
 });
 setApplications(data.data || []);
 } catch (err) {
 toast.error('Failed to load applications');
 } finally {
 setLoading(false);
 }
 }, [activeTab]);

 useEffect(() => {
 fetchApplications();
 }, [fetchApplications]);

 const handleCancel = async (id) => {
 if (!window.confirm("Are you sure you want to cancel this application? You can re-apply later.")) {
 return;
 }
 setCancellingId(id);
 try {
 await api.delete(`/student/applications/${id}`);
 toast.success("Application cancelled");
 // Optimistically update
 setApplications(prev => 
 activeTab === 'pending' 
 ? prev.filter(app => app.id !== id) 
 : prev?.map(app => app.id === id ? { ...app, status: 'cancelled' } : app)
 );
 } catch (err) {
 toast.error(err.response?.data?.message || "Failed to cancel application");
 } finally {
 setCancellingId(null);
 }
 };

 const renderStatusBadge = (status) => {
 switch (status) {
 case 'pending':
 return <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-yellow-100 text-yellow-800 /30 "><Clock className="h-3.5 w-3.5" /> Pending</span>;
 case 'accepted':
 return <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-green-100 text-green-800 /30 "><CheckCircle2 className="h-3.5 w-3.5" /> Accepted</span>;
 case 'rejected':
 return <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-red-100 text-red-800 /30 "><XCircle className="h-3.5 w-3.5" /> Rejected</span>;
 case 'cancelled':
 return <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-gray-100 text-[var(--text-primary)] dark:text-[var(--text-muted)]"><X className="h-3.5 w-3.5" /> Cancelled</span>;
 default:
 return null;
 }
 };

 return (
 <DashboardLayout>
 <div className="space-y-6">
 <div>
 <h1 className="text-2xl font-bold text-[var(--text-primary)] sm:text-3xl">My Applications</h1>
 <p className="mt-1 text-sm text-[var(--text-muted)] dark:text-[var(--text-muted)]">
 Track the status of your university applications.
 </p>
 </div>

 <div className="border-b border-gray-200 ">
 <nav className="-mb-px flex space-x-8 overflow-x-auto custom-scrollbar">
 {TABS?.map(tab => (
 <button
 key={tab.id}
 onClick={() => setActiveTab(tab.id)}
 className={`
 whitespace-nowrap pb-4 px-1 border-b-2 font-medium text-sm transition-colors
 ${activeTab === tab.id
 ? 'border-primary-500 text-primary-600 '
 : 'border-transparent text-[var(--text-muted)] hover:text-[var(--text-secondary)] hover:border-gray-300 dark:text-[var(--text-muted)] dark:hover:text-gray-300'
 }
 `}
 >
 {tab.label}
 </button>
 ))}
 </nav>
 </div>

 {loading ? (
 <div className="flex justify-center py-12">
 <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
 </div>
 ) : applications.length === 0 ? (
 <div className="text-center py-12 bg-[var(--bg-surface)] rounded-2xl border border-gray-200 ">
 <h3 className="text-lg font-medium text-[var(--text-primary)] mb-2">No applications found</h3>
 <p className="text-[var(--text-muted)] dark:text-[var(--text-muted)]">
 {activeTab === 'all' 
 ? "You haven't applied to any universities yet." 
 : `You don't have any ${activeTab} applications.`}
 </p>
 </div>
 ) : (
 <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-2 xl:grid-cols-3">
 {applications?.map(app => (
 <Card key={app.id} className="flex flex-col h-full hover:shadow-lg transition-shadow">
 <div className="flex items-start gap-4 mb-4">
 <div className="h-12 w-12 rounded-xl bg-gray-100 flex items-center justify-center shrink-0 border border-gray-200 ">
 <span className="text-lg font-bold text-[var(--text-muted)]">
 {app.universityName?.charAt(0).toUpperCase()}
 </span>
 </div>
 <div className="flex-1 min-w-0">
 <h3 className="text-lg font-bold text-[var(--text-primary)] truncate" title={app.universityName}>
 {app.universityName}
 </h3>
 <div className="mt-1 flex items-center gap-2">
 {renderStatusBadge(app.status)}
 </div>
 </div>
 </div>

 <div className="space-y-2 mb-4 flex-grow">
 <div className="text-sm">
 <span className="text-[var(--text-muted)] dark:text-[var(--text-muted)] block mb-1">Applied For:</span>
 <div className="font-medium text-[var(--text-primary)] ">
 {app.programName || app.departmentName || app.certificateLevelName || 'General Admission'}
 </div>
 </div>
 
 {app.personalStatement && (
 <div className="mt-3">
 <span className="text-[var(--text-muted)] dark:text-[var(--text-muted)] block mb-1 text-sm">Personal Statement:</span>
 <p className="text-sm text-[var(--text-secondary)] line-clamp-3 italic bg-gray-50 /50 p-3 rounded-lg">
 "{app.personalStatement}"
 </p>
 </div>
 )}

 {app.status === 'accepted' && app.acceptanceMessage && (
 <div className="mt-3 bg-green-50 /20 p-3 rounded-lg border border-green-100 /50">
 <p className="text-sm text-green-800 ">
 <strong>Message:</strong> {app.acceptanceMessage}
 </p>
 </div>
 )}

 {app.status === 'rejected' && app.rejectionReason && (
 <div className="mt-3 bg-red-50 /20 p-3 rounded-lg border border-red-100 /50">
 <p className="text-sm text-red-800 ">
 <strong>Reason:</strong> {app.rejectionReason}
 </p>
 </div>
 )}
 </div>

 <div className="mt-auto pt-4 border-t border-gray-100 flex items-center justify-between">
 <span className="text-xs text-[var(--text-muted)] dark:text-[var(--text-muted)]">
 Applied: {new Date(app.appliedAt).toLocaleDateString()}
 </span>
 
 {app.status === 'pending' && (
 <Button 
 variant="danger" 
 size="sm" 
 onClick={() => handleCancel(app.id)}
 loading={cancellingId === app.id}
 className="bg-transparent border border-red-200 text-red-600 hover:bg-red-50 dark:hover:bg-red-900/30"
 >
 <Trash2 className="h-4 w-4 mr-1.5" />
 Cancel
 </Button>
 )}
 </div>
 </Card>
 ))}
 </div>
 )}
 </div>
 </DashboardLayout>
 );
}
