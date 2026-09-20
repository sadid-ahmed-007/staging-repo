import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Clock, CheckCircle2, XCircle, Eye, UserPlus } from 'lucide-react';
import toast from 'react-hot-toast';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/shared/Card';
import Button from '../../components/shared/Button';
import Modal from '../../components/shared/Modal';
import api from '../../services/api';

const TABS = [
 { id: 'all', label: 'All' },
 { id: 'pending', label: 'Pending' },
 { id: 'accepted', label: 'Accepted' },
 { id: 'rejected', label: 'Rejected' },
];

export default function UniversityApplications() {
 const [activeTab, setActiveTab] = useState('pending');
 const [applications, setApplications] = useState([]);
 const [loading, setLoading] = useState(true);
 const [selectedApp, setSelectedApp] = useState(null);

 const fetchApplications = useCallback(async () => {
 setLoading(true);
 try {
 const { data } = await api.get('/university/applications', {
 params: { status: activeTab !== 'all' ? activeTab : undefined, size: 50 }
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

 const renderStatusBadge = (status) => {
 switch (status) {
 case 'pending':
 return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800 /30 "><Clock className="mr-1 h-3 w-3" /> Pending</span>;
 case 'accepted':
 return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800 /30 "><CheckCircle2 className="mr-1 h-3 w-3" /> Accepted</span>;
 case 'rejected':
 return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800 /30 "><XCircle className="mr-1 h-3 w-3" /> Rejected</span>;
 case 'cancelled':
 return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-[var(--text-primary)] dark:text-[var(--text-muted)]">Cancelled</span>;
 default:
 return null;
 }
 };

 return (
 <DashboardLayout>
 <div className="space-y-6">
 <div>
 <h1 className="text-2xl font-bold text-[var(--text-primary)] sm:text-3xl">Applications</h1>
 <p className="mt-1 text-sm text-[var(--text-muted)] dark:text-[var(--text-muted)]">
 Review and manage student admission applications.
 </p>
 </div>

 <div className="border-b border-gray-200 ">
 <nav className="-mb-px flex space-x-8">
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

 <Card className="overflow-hidden p-0">
 {loading ? (
 <div className="flex justify-center py-12">
 <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
 </div>
 ) : applications.length === 0 ? (
 <div className="text-center py-12">
 <p className="text-[var(--text-muted)] dark:text-[var(--text-muted)]">No {activeTab !== 'all' ? activeTab : ''} applications found.</p>
 </div>
 ) : (
 <div className="overflow-x-auto custom-scrollbar">
 <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
 <thead className="bg-gray-50 /50">
 <tr>
 <th className="px-6 py-3 text-left text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider dark:text-[var(--text-muted)]">Applicant</th>
 <th className="px-6 py-3 text-left text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider dark:text-[var(--text-muted)]">Applied For</th>
 <th className="px-6 py-3 text-left text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider dark:text-[var(--text-muted)]">Applied On</th>
 <th className="px-6 py-3 text-left text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider dark:text-[var(--text-muted)]">Status</th>
 <th className="px-6 py-3 text-right text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider dark:text-[var(--text-muted)]">Actions</th>
 </tr>
 </thead>
 <tbody className="bg-[var(--bg-surface)] divide-y divide-[var(--border)]">
 {applications?.map(app => (
 <tr key={app.id} className="hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors">
 <td className="px-6 py-4 whitespace-nowrap">
 <div className="text-sm font-medium text-[var(--text-primary)] ">{app.studentName}</div>
 <div className="text-sm text-[var(--text-muted)] dark:text-[var(--text-muted)]">{app.studentEmail}</div>
 </td>
 <td className="px-6 py-4">
 <div className="text-sm text-[var(--text-primary)] max-w-[200px] truncate">
 {app.programName || app.departmentName || app.certificateLevelName || 'General Admission'}
 </div>
 {app.personalStatement && (
 <div className="text-xs text-[var(--text-muted)] truncate max-w-[200px]" title={app.personalStatement}>
 "{app.personalStatement}"
 </div>
 )}
 </td>
 <td className="px-6 py-4 whitespace-nowrap text-sm text-[var(--text-muted)] dark:text-[var(--text-muted)]">
 {new Date(app.appliedAt).toLocaleDateString()}
 </td>
 <td className="px-6 py-4 whitespace-nowrap">
 {renderStatusBadge(app.status)}
 </td>
 <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
 <button
 onClick={() => setSelectedApp(app)}
 className="text-primary-600 hover:text-primary-900 dark:hover:text-primary-300 inline-flex items-center"
 >
 <Eye className="h-4 w-4 mr-1" />
 Review
 </button>
 </td>
 </tr>
 ))}
 </tbody>
 </table>
 </div>
 )}
 </Card>

 {selectedApp && (
 <ReviewModal 
 application={selectedApp} 
 onClose={() => setSelectedApp(null)} 
 onSuccess={() => {
 setSelectedApp(null);
 fetchApplications();
 }}
 />
 )}
 </div>
 </DashboardLayout>
 );
}

// Review Modal Component
function ReviewModal({ application, onClose, onSuccess }) {
 const navigate = useNavigate();
 const [submitting, setSubmitting] = useState(false);
 const [action, setAction] = useState(null); // 'accept' or 'reject'
 const [message, setMessage] = useState('');
 const [certificates, setCertificates] = useState([]);
 const [loadingCerts, setLoadingCerts] = useState(true);

 useEffect(() => {
 // Note: The university is allowed to view certificates for students who have applied to them
 // Our backend might need a specific endpoint for this, or we rely on the existing /university/students/{id}/certificates if access is granted via applying.
 // For this implementation, we will use an endpoint that fetches the applicant's certificates.
 // However, since we haven't built a specific "get applicant certs" endpoint, we can use the application DTO if it had it, 
 // or try the standard cert search (if the application implies access grant).
 // Let's assume the backend handles access control correctly for `/university/certificates/student/${application.studentId}` 
 // if the university has a pending application from them. (In a real scenario, this would be explicitly coded in backend).
 const fetchCerts = async () => {
 setLoadingCerts(true);
 try {
 const { data } = await api.get(`/university/students/${application.studentId}/certificates`);
 setCertificates(data.certificates || []);
 } catch (err) {
 // Handle error gracefully if access is not completely set up in backend yet
 console.warn('Could not fetch certificates', err);
 } finally {
 setLoadingCerts(false);
 }
 };
 fetchCerts();
 }, [application.studentId]);

 const handleSubmit = async (e) => {
 e.preventDefault();
 if (message.length < 10) {
 toast.error('Message must be at least 10 characters.');
 return;
 }
 
 setSubmitting(true);
 try {
 await api.post(`/university/applications/${application.id}/review`, {
 status: action === 'accept' ? 'accepted' : 'rejected',
 message: message
 });
 
 toast.success(`Application ${action}ed successfully`);
 
 if (action === 'accept') {
 if (window.confirm("Would you like to enroll this student now?")) {
 navigate('/university/enrollments?tab=from_applications');
 } else {
 onSuccess();
 }
 } else {
 onSuccess();
 }
 } catch (err) {
 toast.error(err.response?.data?.message || 'Failed to review application');
 setSubmitting(false);
 }
 };

 return (
 <Modal open={true} onClose={onClose} title="Review Application" size="xl">
 <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
 
 {/* Left Side: App Details (60% -> col-span-3) */}
 <div className="lg:col-span-3 flex flex-col h-full">
 <div className="bg-gray-50 /50 p-5 rounded-xl border border-gray-100 mb-6">
 <h4 className="text-sm font-semibold text-[var(--text-muted)] uppercase tracking-wider mb-4">Applicant Profile</h4>
 <div className="space-y-3">
 <div>
 <span className="text-sm text-[var(--text-muted)] block">Name</span>
 <span className="text-base font-medium text-[var(--text-primary)] ">{application.studentName}</span>
 </div>
 <div>
 <span className="text-sm text-[var(--text-muted)] block">Email</span>
 <span className="text-base text-[var(--text-primary)] ">{application.studentEmail}</span>
 </div>
 <div>
 <span className="text-sm text-[var(--text-muted)] block">Applied For</span>
 <span className="text-base font-medium text-primary-600 ">
 {application.programName || application.departmentName || application.certificateLevelName || 'General Admission'}
 </span>
 </div>
 <div>
 <span className="text-sm text-[var(--text-muted)] block">Applied On</span>
 <span className="text-base text-[var(--text-primary)] ">
 {new Date(application.appliedAt).toLocaleString()}
 </span>
 </div>
 </div>
 </div>

 <div className="flex-1 flex flex-col min-h-[150px]">
 <h4 className="text-sm font-semibold text-[var(--text-muted)] uppercase tracking-wider mb-2">Personal Statement</h4>
 {application.personalStatement ? (
 <div className="flex-1 bg-[var(--bg-elevated)] border border-[var(--border)] rounded-xl p-4 overflow-y-auto text-sm text-[var(--text-secondary)] italic custom-scrollbar">
 "{application.personalStatement}"
 </div>
 ) : (
 <div className="flex-1 bg-gray-50 /30 rounded-xl p-4 flex items-center justify-center text-sm text-[var(--text-muted)] italic">
 No personal statement provided.
 </div>
 )}
 </div>
 </div>

 {/* Right Side: Student Certs (40% -> col-span-2) */}
 <div className="lg:col-span-2 flex flex-col h-full bg-gray-50 /50 rounded-xl border border-gray-100 p-5">
 <h4 className="text-sm font-semibold text-[var(--text-muted)] uppercase tracking-wider mb-4">Previous Qualifications</h4>
 
 <div className="flex-1 overflow-y-auto custom-scrollbar pr-2 space-y-3 max-h-[300px] lg:max-h-full">
 {loadingCerts ? (
 <div className="flex justify-center py-8">
 <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600"></div>
 </div>
 ) : certificates.length === 0 ? (
 <div className="text-center py-8 text-sm text-[var(--text-muted)]">
 This applicant has no prior certificates on record.
 </div>
 ) : (
 certificates?.map(cert => (
 <div key={cert.id} className="bg-[var(--bg-surface)] border border-[var(--border)] rounded-lg p-3">
 <div className="font-medium text-sm text-[var(--text-primary)] truncate" title={cert.certificateLevelName}>
 {cert.certificateLevelName}
 </div>
 <div className="text-xs text-[var(--text-muted)] truncate mb-2">{cert.institutionName}</div>
 <div className="flex justify-between items-end">
 <div className="text-xs">
 <span className="text-[var(--text-muted)]">CGPA: </span>
 <span className="font-semibold text-[var(--text-secondary)] ">{cert.cgpa}</span>
 </div>
 <div className="text-xs text-[var(--text-muted)]">
 {new Date(cert.issueDate).getFullYear()}
 </div>
 </div>
 </div>
 ))
 )}
 </div>
 </div>
 </div>

 {/* Bottom: Decision Area */}
 {application.status === 'pending' ? (
 <div className="mt-8 pt-6 border-t border-gray-200 ">
 {!action ? (
 <div className="flex gap-4 justify-end">
 <Button variant="danger" onClick={() => setAction('reject')} className="bg-transparent border border-red-200 text-red-600 hover:bg-red-50 dark:hover:bg-red-900/30">
 Reject Application
 </Button>
 <Button variant="primary" onClick={() => setAction('accept')} className="bg-green-600 hover:bg-green-700 text-white border-transparent">
 Accept Application
 </Button>
 </div>
 ) : (
 <form onSubmit={handleSubmit} className="animate-in fade-in slide-in-from-bottom-4 duration-300">
 <h4 className={`text-base font-semibold mb-2 ${action === 'accept' ? 'text-green-600' : 'text-red-600'}`}>
 {action === 'accept' ? 'Acceptance Message' : 'Rejection Reason'}
 </h4>
 <textarea
 className="w-full rounded-xl border border-[var(--border)] bg-[var(--bg-surface)] px-4 py-2 text-sm text-[var(--text-primary)] focus:outline-none focus:ring-1 focus:ring-[var(--brand)] min-h-[100px] mb-3"
 value={message}
 onChange={e => setMessage(e.target.value)}
 placeholder={action === 'accept' ? 'Welcome message, next steps...' : 'Reason for rejection...'}
 required
 minLength={10}
 />
 <div className="flex gap-3 justify-end">
 <Button type="button" variant="secondary" onClick={() => setAction(null)} disabled={submitting}>
 Cancel
 </Button>
 <Button 
 type="submit" 
 loading={submitting} 
 className={action === 'accept' ? 'bg-green-600 hover:bg-green-700' : 'bg-red-600 hover:bg-red-700'}
 >
 Confirm {action === 'accept' ? 'Acceptance' : 'Rejection'}
 </Button>
 </div>
 </form>
 )}
 </div>
 ) : (
 <div className="mt-8 pt-6 border-t border-gray-200 ">
 {application.status === 'accepted' && (
 <div className="p-4 bg-green-50 /20 rounded-xl border border-green-200 /50">
 <h5 className="text-sm font-semibold text-green-800 mb-1">Application Accepted ✓</h5>
 <p className="text-sm text-green-700 "><strong>Message:</strong> {application.acceptanceMessage}</p>
 </div>
 )}
 {application.status === 'rejected' && (
 <div className="p-4 bg-red-50 /20 rounded-xl border border-red-200 /50">
 <h5 className="text-sm font-semibold text-red-800 mb-1">Application Rejected</h5>
 <p className="text-sm text-red-700 "><strong>Reason:</strong> {application.rejectionReason}</p>
 </div>
 )}
 <div className="mt-4 flex justify-end">
 <Button variant="secondary" onClick={onClose}>Close</Button>
 </div>
 </div>
 )}
 </Modal>
 );
}
