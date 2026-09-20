import React, { useState, useEffect } from 'react';
import { 
 History as HistoryIcon, 
 Send, 
 UserPlus, 
 CalendarDays, 
 LogOut, 
 CheckCircle2, 
 XCircle, 
 Award, 
 Shield, 
 ShieldOff,
 X
} from 'lucide-react';
import toast from 'react-hot-toast';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/shared/Card';
import api from '../../services/api';

const EVENT_TYPES = [
 { id: 'all', label: 'All Events' },
 { id: 'applications', label: 'Applications' },
 { id: 'enrollments', label: 'Enrollments' },
 { id: 'certificates', label: 'Certificates' },
 { id: 'access', label: 'Access Grants' }
];

export default function StudentHistory() {
 const [filter, setFilter] = useState('all');
 const [loading, setLoading] = useState(true);
 const [events, setEvents] = useState([]);

 useEffect(() => {
 const fetchHistory = async () => {
 setLoading(true);
 try {
 const { data } = await api.get('/student/history');
 const historyData = data.data;
 
 // Transform the 5 sections into a flat chronological list of events
 let allEvents = [];

 // 1. Applications
 historyData.applications?.forEach(app => {
 allEvents.push({
 id: `app-submit-${app.id}`,
 type: 'APPLICATION_SUBMITTED',
 date: new Date(app.appliedAt),
 data: app,
 category: 'applications'
 });
 
 if (app.status === 'accepted') {
 allEvents.push({
 id: `app-accept-${app.id}`,
 type: 'APPLICATION_ACCEPTED',
 date: new Date(app.reviewedAt || app.appliedAt),
 data: app,
 category: 'applications'
 });
 } else if (app.status === 'rejected') {
 allEvents.push({
 id: `app-reject-${app.id}`,
 type: 'APPLICATION_REJECTED',
 date: new Date(app.reviewedAt || app.appliedAt),
 data: app,
 category: 'applications'
 });
 } else if (app.status === 'cancelled') {
 allEvents.push({
 id: `app-cancel-${app.id}`,
 type: 'APPLICATION_CANCELLED',
 date: new Date(app.cancelledAt || app.appliedAt),
 data: app,
 category: 'applications'
 });
 }
 });

 // 2. Enrollments
 historyData.enrollments?.forEach(enr => {
 allEvents.push({
 id: `enr-start-${enr.id}`,
 type: 'ENROLLMENT_STARTED',
 date: new Date(enr.enrollmentDate || enr.createdAt),
 data: enr,
 category: 'enrollments'
 });
 // Not tracking graduation extension specifically unless we have a history table for it,
 // but we can just show enrollment started.
 });

 // 3. Withdrawal Requests
 historyData.withdrawalRequests?.forEach(wr => {
 allEvents.push({
 id: `wr-req-${wr.id}`,
 type: 'WITHDRAWAL_REQUESTED',
 date: new Date(wr.createdAt),
 data: wr,
 category: 'enrollments'
 });
 if (wr.status === 'approved') {
 allEvents.push({
 id: `wr-appr-${wr.id}`,
 type: 'WITHDRAWAL_APPROVED',
 date: new Date(wr.reviewedAt || wr.createdAt),
 data: wr,
 category: 'enrollments'
 });
 } else if (wr.status === 'rejected') {
 allEvents.push({
 id: `wr-rej-${wr.id}`,
 type: 'WITHDRAWAL_REJECTED',
 date: new Date(wr.reviewedAt || wr.createdAt),
 data: wr,
 category: 'enrollments'
 });
 }
 });

 // 4. Certificates
 historyData.certificates?.forEach(cert => {
 allEvents.push({
 id: `cert-issue-${cert.id}`,
 type: 'CERTIFICATE_ISSUED',
 date: new Date(cert.issueDate),
 data: cert,
 category: 'certificates'
 });
 });

 // 5. Access Grants
 historyData.accessGrants?.forEach(grant => {
 allEvents.push({
 id: `grant-add-${grant.id}`,
 type: 'ACCESS_GRANTED',
 date: new Date(grant.grantedAt),
 data: grant,
 category: 'access'
 });
 if (grant.revokedAt) {
 allEvents.push({
 id: `grant-rev-${grant.id}`,
 type: 'ACCESS_REVOKED',
 date: new Date(grant.revokedAt),
 data: grant,
 category: 'access'
 });
 }
 });

 // Sort descending by date
 allEvents.sort((a, b) => b.date.getTime() - a.date.getTime());
 setEvents(allEvents);

 } catch (err) {
 toast.error("Failed to load student history");
 console.error(err);
 } finally {
 setLoading(false);
 }
 };

 fetchHistory();
 }, []);

 const filteredEvents = events.filter(e => filter === 'all' || e.category === filter);

 const renderEventCard = (event) => {
 const { type, data, date } = event;
 const formattedDate = date.toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' });
 const formattedTime = date.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });

 let icon, title, colorClass, details;

 switch (type) {
 case 'APPLICATION_SUBMITTED':
 icon = <Send className="h-5 w-5 text-blue-600 " />;
 colorClass = "bg-blue-50 /30 border-blue-200 ";
 title = `Applied to ${data.universityName}`;
 details = "Submitted application for admission.";
 break;
 case 'APPLICATION_ACCEPTED':
 icon = <CheckCircle2 className="h-5 w-5 text-green-600 " />;
 colorClass = "bg-green-50 /30 border-green-200 ";
 title = `Application Accepted by ${data.universityName}`;
 details = data.acceptanceMessage ? `Message: "${data.acceptanceMessage}"` : "Congratulations!";
 break;
 case 'APPLICATION_REJECTED':
 icon = <XCircle className="h-5 w-5 text-red-600 " />;
 colorClass = "bg-red-50 /30 border-red-200 ";
 title = `Application Not Accepted by ${data.universityName}`;
 details = data.rejectionReason ? `Reason: "${data.rejectionReason}"` : "Application was rejected.";
 break;
 case 'APPLICATION_CANCELLED':
 icon = <X className="h-5 w-5 text-[var(--text-secondary)] dark:text-[var(--text-muted)]" />;
 colorClass = "bg-gray-50 border-gray-200 ";
 title = `Cancelled application to ${data.universityName}`;
 details = "You withdrew your application.";
 break;
 case 'ENROLLMENT_STARTED':
 icon = <UserPlus className="h-5 w-5 text-emerald-600 " />;
 colorClass = "bg-emerald-50 /30 border-emerald-200 ";
 title = `Enrolled at ${data.institutionName}`;
 details = `Program: ${data.program || 'General'}, Batch: ${data.batch || 'N/A'}, Enrollment #: ${data.enrollmentNumber}`;
 break;
 case 'WITHDRAWAL_REQUESTED':
 icon = <LogOut className="h-5 w-5 text-orange-600 " />;
 colorClass = "bg-orange-50 /30 border-orange-200 ";
 title = `Requested withdrawal`;
 details = `Reason: "${data.reason}"`;
 break;
 case 'WITHDRAWAL_APPROVED':
 icon = <CheckCircle2 className="h-5 w-5 text-green-600 " />;
 colorClass = "bg-green-50 /30 border-green-200 ";
 title = `Withdrawal Approved`;
 details = "Your withdrawal request was approved by the university.";
 break;
 case 'WITHDRAWAL_REJECTED':
 icon = <XCircle className="h-5 w-5 text-red-600 " />;
 colorClass = "bg-red-50 /30 border-red-200 ";
 title = `Withdrawal Rejected`;
 details = data.rejectionNote ? `Note: "${data.rejectionNote}"` : "Your withdrawal request was denied.";
 break;
 case 'CERTIFICATE_ISSUED':
 icon = <Award className="h-6 w-6 text-indigo-600 " />;
 colorClass = "bg-indigo-50 /30 border-indigo-200 shadow-md transform hover:scale-[1.01] transition-transform";
 title = `Graduated: ${data.certificateName}`;
 details = `Institution: ${data.institutionName} | Level: ${data.certificateLevel} | Serial: ${data.serial}`;
 break;
 case 'ACCESS_GRANTED':
 icon = <Shield className="h-5 w-5 text-blue-600 " />;
 colorClass = "bg-blue-50 /30 border-blue-200 ";
 title = `Granted Access to Verifier #${data.verifierId}`;
 details = `Expires on: ${data.expiresAt ? new Date(data.expiresAt).toLocaleDateString() : 'Never'}`;
 break;
 case 'ACCESS_REVOKED':
 icon = <ShieldOff className="h-5 w-5 text-red-600 " />;
 colorClass = "bg-red-50 /30 border-red-200 ";
 title = `Revoked Access from Verifier #${data.verifierId}`;
 details = "Access was manually revoked.";
 break;
 default:
 icon = <HistoryIcon className="h-5 w-5 text-[var(--text-muted)]" />;
 colorClass = "bg-gray-50 border-gray-200";
 title = "Unknown Event";
 details = "";
 }

 return (
 <div key={event.id} className="relative pl-8 sm:pl-32 py-4 group">
 {/* Timeline line */}
 <div className="absolute top-0 bottom-0 left-4 sm:left-28 w-px bg-gray-200 group-last:bottom-auto group-last:h-full"></div>
 
 {/* Timeline dot/icon wrapper */}
 <div className={`absolute left-0 sm:left-24 h-8 w-8 rounded-full border-2 border-white bg-[var(--bg-surface)] flex items-center justify-center transform -translate-x-1.5 sm:translate-x-0 ${type === 'CERTIFICATE_ISSUED' ? 'ring-4 ring-indigo-100 dark:ring-indigo-900 z-10' : ''}`}>
 {icon}
 </div>

 {/* Date on the left (desktop only) */}
 <div className="hidden sm:block absolute left-0 top-6 w-20 text-right">
 <div className="text-xs font-bold text-[var(--text-primary)] leading-tight">
 {date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
 </div>
 <div className="text-xs text-[var(--text-muted)]">
 {date.getFullYear()}
 </div>
 </div>

 {/* Card */}
 <div className={`relative p-4 rounded-xl border ${colorClass}`}>
 <div className="flex justify-between items-start mb-1 gap-4">
 <h3 className="font-bold text-[var(--text-primary)] text-base">
 {title}
 </h3>
 <span className="text-xs font-medium text-[var(--text-muted)] whitespace-nowrap block sm:hidden">
 {formattedDate}
 </span>
 </div>
 {details && (
 <p className="text-sm text-[var(--text-secondary)] mt-1">
 {details}
 </p>
 )}
 <span className="text-xs text-[var(--text-muted)] mt-2 block sm:hidden">
 {formattedTime}
 </span>
 <span className="hidden sm:block text-xs text-[var(--text-muted)] mt-2">
 {formattedTime}
 </span>
 </div>
 </div>
 );
 };

 return (
 <DashboardLayout>
 <div className="space-y-6 max-w-4xl mx-auto">
 <div className="text-center sm:text-left">
 <h1 className="text-2xl font-bold text-[var(--text-primary)] sm:text-3xl">My History</h1>
 <p className="mt-1 text-sm text-[var(--text-muted)] dark:text-[var(--text-muted)]">
 A complete record of your academic journey on EduAuth Registry.
 </p>
 </div>

 <Card className="sticky top-0 z-20">
 <div className="flex flex-wrap gap-2">
 {EVENT_TYPES?.map(t => (
 <button
 key={t.id}
 onClick={() => setFilter(t.id)}
 className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
 filter === t.id 
 ? 'bg-primary-600 text-white shadow-sm' 
 : 'bg-gray-100 text-[var(--text-secondary)] hover:bg-gray-200 dark:hover:bg-gray-700'
 }`}
 >
 {t.label}
 </button>
 ))}
 </div>
 </Card>

 {loading ? (
 <div className="flex justify-center py-12">
 <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
 </div>
 ) : filteredEvents.length === 0 ? (
 <div className="text-center py-12 bg-[var(--bg-surface)] rounded-2xl border border-gray-200 ">
 <HistoryIcon className="h-12 w-12 text-[var(--text-muted)] mx-auto mb-3 opacity-50" />
 <h3 className="text-lg font-medium text-[var(--text-primary)] mb-2">No history found.</h3>
 <p className="text-[var(--text-muted)] dark:text-[var(--text-muted)]">
 Start your journey by applying to a university.
 </p>
 </div>
 ) : (
 <div className="mt-8 pb-12">
 {filteredEvents?.map(renderEventCard)}
 </div>
 )}
 </div>
 </DashboardLayout>
 );
}
