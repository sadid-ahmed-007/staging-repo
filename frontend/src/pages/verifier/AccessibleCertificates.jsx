import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import {
 FileText,
 ArrowLeft,
 Search,
 Lock,
 Download,
 ShieldCheck,
 Clock,
 Loader2,
 Eye,
 AlertCircle,
 Building2,
 Calendar,
 Hash,
 ShieldAlert,
 RefreshCw,
 UserPlus,
 X,
 GraduationCap
} from 'lucide-react';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/shared/Card';
import Badge from '../../components/shared/Badge';
import Button from '../../components/shared/Button';
import Modal from '../../components/shared/Modal';
import LoadingSpinner from '../../components/shared/LoadingSpinner';
import ErrorMessage from '../../components/shared/ErrorMessage';
import EmptyState from '../../components/shared/EmptyState';
import api from '../../services/api';
import { formatDate } from '../../utils/helpers';
import { downloadCertificatePDF } from '../../services/certificateService';
import CertificateDetailModal from '../../components/certificates/CertificateDetailModal';

function getDaysRemaining(expiresAt) {
 if (!expiresAt) return 0;
 const now = new Date();
 const exp = new Date(expiresAt);
 const diffTime = exp.getTime() - now.getTime();
 return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
}

function DaysRemainingBadge({ expiresAt }) {
 const days = getDaysRemaining(expiresAt);
 if (days < 7) {
 return (
 <Badge variant="danger" size="sm" className="flex items-center gap-1 font-medium">
 <Clock className="w-3 h-3" />
 {days <= 0 ? 'Expired' : 'Expiring soon'}
 </Badge>
 );
 } else if (days <= 30) {
 return (
 <Badge variant="warning" size="sm" className="flex items-center gap-1 font-medium">
 <Clock className="w-3 h-3" />
 {days} days left
 </Badge>
 );
 } else {
 return (
 <Badge variant="success" size="sm" className="flex items-center gap-1 font-medium">
 <Clock className="w-3 h-3" />
 {days} days left
 </Badge>
 );
 }
}

export default function VerifierAccessibleCertificates() {
 const navigate = useNavigate();
 const { studentId } = useParams();

 // List View State
 const [students, setStudents] = useState([]);
 const [loadingList, setLoadingList] = useState(true);
 const [listError, setListError] = useState('');
 const [listSearchQuery, setListSearchQuery] = useState('');
 const [listStatusFilter, setListStatusFilter] = useState('all'); // 'all' | 'active' | 'expiring' | 'expired'

 // Student Detail View State
 const [studentData, setStudentData] = useState(null);
 const [studentCerts, setStudentCerts] = useState([]);
 const [accessExpires, setAccessExpires] = useState(null);
 const [loadingDetail, setLoadingDetail] = useState(false);
 const [detailError, setDetailError] = useState('');
 const [isForbidden, setIsForbidden] = useState(false);
 const [downloadingId, setDownloadingId] = useState(null);
 const [certSearchQuery, setCertSearchQuery] = useState('');
 const [selectedCertificate, setSelectedCertificate] = useState(null);

 // Request Full Access Modal State
 const [isRequestAllModalOpen, setIsRequestAllModalOpen] = useState(false);
 const [requestAllPurpose, setRequestAllPurpose] = useState('');
 const [requestAllDuration, setRequestAllDuration] = useState(30);
 const [submittingRequestAll, setSubmittingRequestAll] = useState(false);

 const handleSendAllRequest = async (e) => {
 e.preventDefault();
 if (!studentData?.id) return;
 const trimmed = requestAllPurpose.trim();
 if (trimmed.length < 20) {
 toast.error('Purpose must be at least 20 characters.');
 return;
 }
 setSubmittingRequestAll(true);
 try {
 const { data } = await api.post('/verifier/access-requests', {
 studentId: studentData.id,
 purpose: trimmed,
 requestedDurationDays: requestAllDuration,
 requestAllCertificates: true
 });
 if (data.success) {
 toast.success('Access request for all certificates sent to student');
 setIsRequestAllModalOpen(false);
 setRequestAllPurpose('');
 setStudentData((prev) => prev ? { ...prev, hasPendingAllRequest: true } : prev);
 } else {
 toast.error(data.message || 'Failed to send request');
 }
 } catch (err) {
 toast.error(err.response?.data?.message || 'Failed to send access request');
 } finally {
 setSubmittingRequestAll(false);
 }
 };

 // Fetch list of accessible students
 const fetchStudents = useCallback(async () => {
 setLoadingList(true);
 setListError('');
 try {
 const { data } = await api.get('/verifier/accessible-certificates');
 if (data.success) {
 setStudents(data.data || []);
 } else {
 setListError(data.message || 'Failed to load accessible students');
 }
 } catch (err) {
 console.error('Error fetching accessible students:', err);
 setListError(err.response?.data?.message || 'Failed to load accessible students');
 } finally {
 setLoadingList(false);
 }
 }, []);

 // Fetch single student's accessible certificates
 const fetchStudentCertificates = useCallback(async (id) => {
 setLoadingDetail(true);
 setDetailError('');
 setIsForbidden(false);
 try {
 const { data } = await api.get(`/verifier/accessible-certificates/${id}`);
 if (data.success) {
 setStudentData(data.student || null);
 setStudentCerts(data.data || []);
 setAccessExpires(data.accessExpires || null);
 } else {
 setDetailError(data.message || 'Failed to load certificates');
 }
 } catch (err) {
 console.error('Error fetching student certificates:', err);
 if (err.response?.status === 403) {
 setIsForbidden(true);
 setDetailError(
 err.response?.data?.message ||
 "You do not have active access to this student's certificates. Access may have expired or been revoked."
 );
 } else {
 setDetailError(err.response?.data?.message || 'Failed to load certificates');
 }
 } finally {
 setLoadingDetail(false);
 }
 }, []);

 useEffect(() => {
 if (studentId) {
 fetchStudentCertificates(studentId);
 } else {
 fetchStudents();
 }
 }, [studentId, fetchStudents, fetchStudentCertificates]);

 // Counts for status filter pills
 const studentCounts = useMemo(() => {
 let active = 0;
 let expiring = 0;
 let expired = 0;
 students.forEach((s) => {
 const days = getDaysRemaining(s.accessExpiresAt);
 if (days <= 0) {
 expired++;
 } else {
 active++;
 if (days <= 7) expiring++;
 }
 });
 return { all: students.length, active, expiring, expired };
 }, [students]);

 // Real-time filtered accessible students
 const filteredStudents = useMemo(() => {
 let result = students;

 // Filter by status tab
 if (listStatusFilter === 'active') {
 result = result.filter((s) => getDaysRemaining(s.accessExpiresAt) > 0);
 } else if (listStatusFilter === 'expiring') {
 result = result.filter((s) => {
 const days = getDaysRemaining(s.accessExpiresAt);
 return days > 0 && days <= 7;
 });
 } else if (listStatusFilter === 'expired') {
 result = result.filter((s) => getDaysRemaining(s.accessExpiresAt) <= 0);
 }

 // Filter by search query (matches name, email, institution, department)
 if (listSearchQuery.trim()) {
 const q = listSearchQuery.trim().toLowerCase();
 result = result.filter((s) => {
 const nameMatch = s.studentName && s.studentName.toLowerCase().includes(q);
 const emailMatch = s.studentEmail && s.studentEmail.toLowerCase().includes(q);
 const instMatch = s.institutionName && s.institutionName.toLowerCase().includes(q);
 const deptMatch = s.department && s.department.toLowerCase().includes(q);
 return nameMatch || emailMatch || instMatch || deptMatch;
 });
 }

 return result;
 }, [students, listSearchQuery, listStatusFilter]);

 // Real-time filtered certificates for single student detail view
 const filteredCerts = useMemo(() => {
 if (!certSearchQuery.trim()) return studentCerts;
 const q = certSearchQuery.trim().toLowerCase();
 return studentCerts.filter((c) => {
 const nameMatch = c.certificateName && c.certificateName.toLowerCase().includes(q);
 const levelMatch = c.certificateLevel && c.certificateLevel.toLowerCase().includes(q);
 const deptMatch = c.department && c.department.toLowerCase().includes(q);
 const majorMatch = c.major && c.major.toLowerCase().includes(q);
 const serialMatch = c.serial && c.serial.toLowerCase().includes(q);
 const instMatch = c.institutionName && c.institutionName.toLowerCase().includes(q);
 return nameMatch || levelMatch || deptMatch || majorMatch || serialMatch || instMatch;
 });
 }, [studentCerts, certSearchQuery]);

 const handleDownloadPdf = async (certificate) => {
 try {
 setDownloadingId(certificate.id);
 await downloadCertificatePDF(
 certificate.id,
 certificate.serial,
 '/verifier/accessible-certificates/certificates'
 );
 toast.success('Certificate download started');
 } catch (err) {
 console.error('Failed to download certificate:', err);
 toast.error('Failed to download certificate PDF');
 } finally {
 setDownloadingId(null);
 }
 };

 const handleVerify = (certificate) => {
 navigate(`/verifier/verify-certificate?serial=${encodeURIComponent(certificate.serial)}`);
 };

 const getInitials = (name) => {
 if (!name) return 'S';
 const parts = name.trim().split(' ');
 if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
 return name.substring(0, 2).toUpperCase();
 };

 // ───────────────────────────────────────────────────────────────────────────
 // VIEW: SINGLE STUDENT'S ACCESSIBLE CERTIFICATES
 // ───────────────────────────────────────────────────────────────────────────
 if (studentId) {
 return (
 <DashboardLayout>
 <div className="space-y-6">
 {/* Header & Back Button */}
 <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
 <div className="flex items-center gap-3">
 <Button
 variant="secondary"
 size="sm"
 onClick={() => navigate('/verifier/accessible-certificates')}
 className="h-9 px-2.5"
 >
 <ArrowLeft className="w-4 h-4 mr-1" />
 Back
 </Button>
 <div>
 <div className="flex items-center gap-2 flex-wrap">
 <h1 className="text-xl sm:text-2xl font-bold text-[var(--text-primary)]">
 {studentData?.name || 'Student Certificates'}
 </h1>
 {studentData?.hasAllAccess ? (
 <Badge variant="default" size="sm">
 All Certificates Access
 </Badge>
 ) : (
 <Badge variant="primary" size="sm">
 Single Certificate Access
 </Badge>
 )}
 </div>
 <p className="text-xs sm:text-sm text-[var(--text-secondary)] mt-0.5">
 {studentData?.email ? (
 <span>{studentData.email} • Verified Access Granted</span>
 ) : (
 'Verified Access Granted'
 )}
 </p>
 </div>
 </div>

 {accessExpires && !isForbidden && (
 <div className="flex items-center gap-3 text-xs sm:text-sm">
 <span className="text-[var(--text-secondary)]">
 Access expires: <strong>{formatDate(accessExpires)}</strong>
 </span>
 <DaysRemainingBadge expiresAt={accessExpires} />
 </div>
 )}
 </div>

 {/* Upgrade Banner for Single Certificate Access */}
 {!isForbidden && studentData && !studentData.hasAllAccess && (
 <div className="p-4 rounded-xl border border-[var(--brand)]/25 bg-[var(--brand-light)]/25 flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-2xs">
 <div className="flex items-start gap-3.5">
 <div className="w-10 h-10 rounded-xl bg-[var(--brand)] text-white flex items-center justify-center shrink-0 shadow-xs">
 <GraduationCap className="w-5 h-5" />
 </div>
 <div>
 <h3 className="text-sm font-bold text-[var(--text-primary)]">
 Curious about other degrees or certificates for {studentData.name}?
 </h3>
 <p className="text-xs text-[var(--text-secondary)] mt-0.5 max-w-xl">
 You currently hold verified access to this specific certificate only. You can easily request comprehensive access to view all academic certificates owned by this candidate.
 </p>
 </div>
 </div>
 <div className="shrink-0 sm:self-center">
 {studentData.hasPendingAllRequest ? (
 <Badge variant="warning" size="md" className="flex items-center gap-1.5 py-1.5 px-3 font-medium">
 <Clock className="w-4 h-4" />
 Full Access Request Pending
 </Badge>
 ) : (
 <Button
 variant="primary"
 size="sm"
 onClick={() => {
 setRequestAllPurpose('');
 setRequestAllDuration(30);
 setIsRequestAllModalOpen(true);
 }}
 className="font-medium shadow-xs whitespace-nowrap"
 >
 <GraduationCap className="w-4 h-4 mr-1.5" />
 Request All Certificates
 </Button>
 )}
 </div>
 </div>
 )}

 {/* Loading */}
 {loadingDetail ? (
 <div className="flex min-h-[45vh] items-center justify-center">
 <LoadingSpinner />
 </div>
 ) : isForbidden ? (
 /* 403 Forbidden State */
 <Card className="p-8 text-center border border-[var(--danger)]/30 bg-[var(--danger)]/5 rounded-xl">
 <div className="w-14 h-14 mx-auto rounded-full bg-[var(--danger)]/15 flex items-center justify-center text-[var(--danger)] mb-4">
 <ShieldAlert className="w-7 h-7" />
 </div>
 <h3 className="text-lg font-bold text-[var(--text-primary)]">
 Access Permission Denied (403)
 </h3>
 <p className="text-sm text-[var(--text-secondary)] mt-2 max-w-lg mx-auto">
 {detailError ||
 "You do not have active access to this student's certificates. Access may have expired or been revoked by the student."}
 </p>
 <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
 <Button
 variant="outline"
 onClick={() => navigate('/verifier/accessible-certificates')}
 >
 <ArrowLeft className="w-4 h-4 mr-1.5" />
 Back to Accessible Students
 </Button>
 <Button
 variant="primary"
 onClick={() => navigate('/verifier/search')}
 >
 <Search className="w-4 h-4 mr-1.5" />
 Request Access Again
 </Button>
 </div>
 </Card>
 ) : detailError ? (
 <ErrorMessage
 message={detailError}
 retry={() => fetchStudentCertificates(studentId)}
 />
 ) : studentCerts.length === 0 ? (
 <EmptyState
 title="No Certificates Found"
 message={`${studentData?.name || 'This student'} does not have any active certificates issued.`}
 icon={FileText}
 action={
 <Button
 variant="outline"
 size="sm"
 onClick={() => navigate('/verifier/accessible-certificates')}
 className="mt-4"
 >
 Back to Students
 </Button>
 }
 />
 ) : (
 <div className="space-y-4">
 {/* In-page Certificate Search Toolbar */}
 <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-[var(--bg-surface)] p-3 rounded-xl border border-[var(--border)]">
 <div className="relative flex-1 max-w-md">
 <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)] pointer-events-none" />
 <input
 type="text"
 value={certSearchQuery}
 onChange={(e) => setCertSearchQuery(e.target.value)}
 placeholder="Filter certificates by degree, major, serial..."
 className="w-full h-9 pl-9 pr-8 text-sm rounded-lg border border-[var(--border)] bg-[var(--bg-base)] text-[var(--text-primary)] placeholder:text-[var(--text-muted)] focus:outline-none focus:border-[var(--brand)] transition-colors"
 />
 {certSearchQuery && (
 <button
 onClick={() => setCertSearchQuery('')}
 className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[var(--text-muted)] hover:text-[var(--text-primary)] p-0.5"
 title="Clear filter"
 >
 <X className="w-3.5 h-3.5" />
 </button>
 )}
 </div>

 <div className="text-xs text-[var(--text-secondary)] font-medium">
 Showing {filteredCerts.length} of {studentCerts.length} certificates
 </div>
 </div>

 {filteredCerts.length === 0 ? (
 <EmptyState
 title="No matching certificates found"
 message={`No certificates matched your search for "${certSearchQuery}".`}
 icon={Search}
 action={
 <Button
 variant="outline"
 size="sm"
 onClick={() => setCertSearchQuery('')}
 className="mt-3"
 >
 Clear Filter
 </Button>
 }
 />
 ) : (
 /* Certificate Cards Grid */
 <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
 {filteredCerts?.map((cert) => (
 <Card
 key={cert.id}
 className="flex flex-col h-full border border-[var(--border)] hover:border-[var(--brand)]/40 hover:shadow-md transition-all duration-200"
 >
 {/* Top Badges */}
 <div className="flex justify-between items-start mb-4">
 <Badge variant="primary" size="sm">
 {cert.certificateLevel || 'Degree'}
 </Badge>
 <Badge
 variant={cert.status === 'revoked' ? 'danger' : 'success'}
 size="sm"
 >
 {cert.status === 'revoked' ? 'Revoked' : 'Active'}
 </Badge>
 </div>

 {/* Body */}
 <div className="flex-1 space-y-2.5">
 <p 
 className="text-base font-semibold text-[var(--text-primary)] leading-tight hover:text-[var(--brand)] cursor-pointer transition-colors"
 onClick={() => setSelectedCertificate(cert)}
 title="Click to view certificate details"
 >
 {cert.institutionName}
 </p>

 {cert.certificateName && (
 <p 
 className="text-sm text-[var(--text-secondary)] font-medium hover:text-[var(--brand)] cursor-pointer transition-colors"
 onClick={() => setSelectedCertificate(cert)}
 title="Click to view certificate details"
 >
 {cert.certificateName}
 </p>
 )}

 {(cert.department || cert.major) && (
 <p className="text-xs text-[var(--text-muted)]">
 {cert.department} {cert.major ? `• ${cert.major}` : ''}
 </p>
 )}

 <div className="pt-1 text-xs space-y-1 text-[var(--text-muted)]">
 <p className="flex items-center gap-1.5">
 <Calendar className="w-3.5 h-3.5 text-[var(--brand)]" />
 <span>Issued: {cert.issueDate ? formatDate(cert.issueDate) : 'N/A'}</span>
 </p>
 <p className="flex items-center gap-1.5 font-mono truncate" title={cert.serial}>
 <Hash className="w-3.5 h-3.5 text-[var(--text-muted)]" />
 <span>Serial: {cert.serial}</span>
 </p>
 </div>
 </div>

 {/* Footer Actions */}
 <div className="mt-5 pt-4 border-t border-[var(--border)] flex flex-wrap items-center justify-between gap-2">
 <Button
 variant="primary"
 size="sm"
 onClick={() => setSelectedCertificate(cert)}
 className="font-medium"
 title="View complete certificate details"
 >
 <Eye className="w-4 h-4 mr-1.5" />
 Details
 </Button>

 <div className="flex items-center gap-1.5">
 <Button
 variant="outline"
 size="sm"
 onClick={() => handleVerify(cert)}
 className="font-medium"
 title="Verify on public registry"
 >
 <ShieldCheck className="w-4 h-4 mr-1 text-[var(--brand)]" />
 Verify
 </Button>

 {cert.isPubliclyShareable ? (
 <Button
 variant="secondary"
 size="sm"
 onClick={() => handleDownloadPdf(cert)}
 disabled={downloadingId === cert.id}
 title="Download Certificate PDF"
 >
 {downloadingId === cert.id ? (
 <Loader2 className="w-4 h-4 animate-spin text-[var(--brand)]" />
 ) : (
 <>
 <Download className="w-4 h-4 mr-1" />
 PDF
 </>
 )}
 </Button>
 ) : (
 <span
 className="text-xs text-[var(--text-muted)] flex items-center gap-1"
 title="Student has not enabled public download"
 >
 <Lock className="w-3 h-3" />
 Private
 </span>
 )}
 </div>
 </div>
 </Card>
 ))}
 </div>
 )}
 </div>
 )}
 </div>

 {/* Certificate Details Modal */}
 <CertificateDetailModal
 open={!!selectedCertificate}
 onClose={() => setSelectedCertificate(null)}
 certificate={selectedCertificate}
 role="verifier"
 downloadingId={downloadingId}
 onDownloadPdf={handleDownloadPdf}
 />

 {/* Request Full Access Modal */}
 <Modal
 isOpen={isRequestAllModalOpen}
 onClose={() => !submittingRequestAll && setIsRequestAllModalOpen(false)}
 title="Request All Certificates Access"
 size="md"
 >
 {studentData && (
 <form onSubmit={handleSendAllRequest} className="space-y-4">
 <div className="rounded-lg bg-[var(--bg-elevated)] p-3.5 border border-[var(--border)] flex items-center gap-3">
 <div className="w-10 h-10 rounded-full bg-[var(--brand-light)] text-[var(--brand)] font-bold flex items-center justify-center text-sm shrink-0">
 {getInitials(studentData.name)}
 </div>
 <div className="min-w-0 flex-1">
 <h4 className="text-sm font-semibold text-[var(--text-primary)] truncate">
 {studentData.name}
 </h4>
 <p className="text-xs text-[var(--text-secondary)] truncate">
 {studentData.email || 'Verified Candidate'}
 </p>
 </div>
 </div>

 <div className="p-3 rounded-lg bg-[var(--brand-light)]/30 border border-[var(--brand)]/20 text-xs text-[var(--brand)] flex items-center gap-2 font-medium">
 <GraduationCap className="w-4 h-4 shrink-0" />
 <span>Requesting comprehensive access to all academic certificates of this student.</span>
 </div>

 <div className="space-y-1.5">
 <label className="block text-sm font-medium text-[var(--text-primary)]">
 Why do you need full access? <span className="text-[var(--danger)]">*</span>
 </label>
 <textarea
 rows={3}
 value={requestAllPurpose}
 onChange={(e) => setRequestAllPurpose(e.target.value)}
 placeholder="Explain why you need access to all certificates (e.g. verifying full educational qualifications for employment)..."
 className="w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] p-3 text-sm text-[var(--text-primary)] outline-none focus:border-[var(--brand)] resize-none transition-all"
 required
 />
 {requestAllPurpose.trim().length > 0 && requestAllPurpose.trim().length < 20 && (
 <p className="text-xs text-[var(--danger)]">
 Please provide at least 20 characters explaining your purpose.
 </p>
 )}
 </div>

 <div className="space-y-1.5">
 <label className="block text-sm font-medium text-[var(--text-primary)]">
 Requested Duration
 </label>
 <div className="grid grid-cols-3 gap-2">
 {[7, 30, 90].map((d) => (
 <button
 key={d}
 type="button"
 onClick={() => setRequestAllDuration(d)}
 className={`py-2 px-3 text-xs font-medium rounded-lg border transition-all text-center ${
 requestAllDuration === d
 ? 'border-[var(--brand)] bg-[var(--brand-light)] text-[var(--brand)] font-semibold'
 : 'border-[var(--border)] bg-[var(--bg-surface)] text-[var(--text-secondary)] hover:border-[var(--border-strong)]'
 }`}
 >
 {d} days
 </button>
 ))}
 </div>
 </div>

 <div className="pt-4 flex justify-end gap-2 border-t border-[var(--border)]">
 <Button
 type="button"
 variant="secondary"
 size="md"
 onClick={() => setIsRequestAllModalOpen(false)}
 disabled={submittingRequestAll}
 >
 Cancel
 </Button>
 <Button
 type="submit"
 variant="primary"
 size="md"
 loading={submittingRequestAll}
 disabled={requestAllPurpose.trim().length < 20 || submittingRequestAll}
 >
 Send Access Request
 </Button>
 </div>
 </form>
 )}
 </Modal>
 </DashboardLayout>
 );
 }

 // ───────────────────────────────────────────────────────────────────────────
 // VIEW: LIST OF ACCESSIBLE STUDENTS (GROUPED)
 // ───────────────────────────────────────────────────────────────────────────
 return (
 <DashboardLayout>
 <div className="space-y-6">
 {/* Top Header */}
 <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
 <div>
 <h1 className="text-2xl font-bold text-[var(--text-primary)] tracking-tight">
 Accessible Certificates
 </h1>
 <p className="text-sm text-[var(--text-secondary)] mt-1">
 Search and view certificates from students who have granted you access
 </p>
 </div>
 <div className="flex items-center gap-2">
 <Button
 variant="outline"
 size="sm"
 onClick={fetchStudents}
 disabled={loadingList}
 title="Refresh student list"
 >
 <RefreshCw className={`w-4 h-4 mr-1.5 ${loadingList ? 'animate-spin' : ''}`} />
 Refresh
 </Button>
 <Button
 variant="primary"
 size="sm"
 onClick={() => navigate('/verifier/search')}
 >
 <UserPlus className="w-4 h-4 mr-1.5" />
 Request New Access
 </Button>
 </div>
 </div>

 {/* Content */}
 {loadingList ? (
 <div className="flex min-h-[45vh] items-center justify-center">
 <LoadingSpinner />
 </div>
 ) : listError ? (
 <ErrorMessage message={listError} retry={fetchStudents} />
 ) : students.length === 0 ? (
 <EmptyState
 title="No accessible certificates yet"
 message="You don't have access to any student certificates right now. Search for a student by email, certificate serial, or NID to request verified access."
 icon={Lock}
 action={
 <Button
 variant="primary"
 onClick={() => navigate('/verifier/search')}
 className="mt-4"
 >
 <Search className="w-4 h-4 mr-1.5" />
 Search Registry & Request Access
 </Button>
 }
 />
 ) : (
 <div className="space-y-5">
 {/* Search and Status Filters Toolbar */}
 <div className="space-y-3 bg-[var(--bg-surface)] p-4 rounded-xl border border-[var(--border)] shadow-sm">
 <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-3">
 {/* Search Bar Input */}
 <div className="relative flex-1 max-w-xl">
 <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-[var(--text-muted)] pointer-events-none" />
 <input
 type="text"
 value={listSearchQuery}
 onChange={(e) => setListSearchQuery(e.target.value)}
 placeholder="Search by student name, email, or university..."
 className="w-full h-10 pl-10 pr-9 text-sm rounded-lg border border-[var(--border)] bg-[var(--bg-base)] text-[var(--text-primary)] placeholder:text-[var(--text-muted)] focus:outline-none focus:border-[var(--brand)] transition-all"
 />
 {listSearchQuery && (
 <button
 onClick={() => setListSearchQuery('')}
 className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)] hover:text-[var(--text-primary)] p-0.5 rounded-full"
 title="Clear search"
 >
 <X className="w-4 h-4" />
 </button>
 )}
 </div>

 {/* Status Filter Chips */}
 <div className="flex items-center gap-1.5 overflow-x-auto pb-1 lg:pb-0">
 <button
 type="button"
 onClick={() => setListStatusFilter('all')}
 className={`px-3 py-1.5 text-xs font-medium rounded-full transition-all flex items-center gap-1.5 whitespace-nowrap cursor-pointer ${
 listStatusFilter === 'all'
 ? 'bg-[var(--brand)] text-white shadow-sm'
 : 'bg-[var(--bg-base)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] border border-[var(--border)]'
 }`}
 >
 All
 <span
 className={`px-1.5 py-0.5 rounded-full text-[10px] font-semibold ${
 listStatusFilter === 'all'
 ? 'bg-[var(--bg-surface)]/20 text-white'
 : 'bg-[var(--bg-surface)] text-[var(--text-muted)]'
 }`}
 >
 {studentCounts.all}
 </span>
 </button>

 <button
 type="button"
 onClick={() => setListStatusFilter('active')}
 className={`px-3 py-1.5 text-xs font-medium rounded-full transition-all flex items-center gap-1.5 whitespace-nowrap cursor-pointer ${
 listStatusFilter === 'active'
 ? 'bg-emerald-600 text-white shadow-sm'
 : 'bg-[var(--bg-base)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] border border-[var(--border)]'
 }`}
 >
 Active
 <span
 className={`px-1.5 py-0.5 rounded-full text-[10px] font-semibold ${
 listStatusFilter === 'active'
 ? 'bg-[var(--bg-surface)]/20 text-white'
 : 'bg-[var(--bg-surface)] text-[var(--text-muted)]'
 }`}
 >
 {studentCounts.active}
 </span>
 </button>

 <button
 type="button"
 onClick={() => setListStatusFilter('expiring')}
 className={`px-3 py-1.5 text-xs font-medium rounded-full transition-all flex items-center gap-1.5 whitespace-nowrap cursor-pointer ${
 listStatusFilter === 'expiring'
 ? 'bg-amber-600 text-white shadow-sm'
 : 'bg-[var(--bg-base)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] border border-[var(--border)]'
 }`}
 >
 Expiring Soon
 <span
 className={`px-1.5 py-0.5 rounded-full text-[10px] font-semibold ${
 listStatusFilter === 'expiring'
 ? 'bg-[var(--bg-surface)]/20 text-white'
 : 'bg-[var(--bg-surface)] text-[var(--text-muted)]'
 }`}
 >
 {studentCounts.expiring}
 </span>
 </button>

 <button
 type="button"
 onClick={() => setListStatusFilter('expired')}
 className={`px-3 py-1.5 text-xs font-medium rounded-full transition-all flex items-center gap-1.5 whitespace-nowrap cursor-pointer ${
 listStatusFilter === 'expired'
 ? 'bg-rose-600 text-white shadow-sm'
 : 'bg-[var(--bg-base)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] border border-[var(--border)]'
 }`}
 >
 Expired
 <span
 className={`px-1.5 py-0.5 rounded-full text-[10px] font-semibold ${
 listStatusFilter === 'expired'
 ? 'bg-[var(--bg-surface)]/20 text-white'
 : 'bg-[var(--bg-surface)] text-[var(--text-muted)]'
 }`}
 >
 {studentCounts.expired}
 </span>
 </button>
 </div>
 </div>

 {/* Active Filter Info Banner */}
 {(listSearchQuery || listStatusFilter !== 'all') && (
 <div className="flex items-center justify-between text-xs text-[var(--text-muted)] pt-1 border-t border-[var(--border)]">
 <span>
 Showing {filteredStudents.length} of {students.length} accessible students
 {listSearchQuery && ` matching "${listSearchQuery}"`}
 </span>
 <button
 onClick={() => {
 setListSearchQuery('');
 setListStatusFilter('all');
 }}
 className="text-[var(--brand)] hover:underline font-medium cursor-pointer"
 >
 Reset filters
 </button>
 </div>
 )}
 </div>

 {/* Zero Filtered Results */}
 {filteredStudents.length === 0 ? (
 <Card className="p-8 text-center border border-[var(--border)] rounded-xl bg-[var(--bg-surface)]">
 <div className="w-12 h-12 mx-auto rounded-full bg-[var(--brand-light)] text-[var(--brand)] flex items-center justify-center mb-3">
 <Search className="w-6 h-6" />
 </div>
 <h3 className="text-base font-semibold text-[var(--text-primary)]">
 No matching accessible students found
 </h3>
 <p className="text-xs sm:text-sm text-[var(--text-secondary)] mt-1 max-w-md mx-auto">
 {listSearchQuery
 ? `No students in your accessible list matched "${listSearchQuery}".`
 : 'No accessible students match the selected filter status.'}
 </p>
 <div className="mt-5 flex flex-wrap items-center justify-center gap-3">
 <Button
 variant="outline"
 size="sm"
 onClick={() => {
 setListSearchQuery('');
 setListStatusFilter('all');
 }}
 >
 Clear Filter
 </Button>
 {listSearchQuery && (
 <Button
 variant="primary"
 size="sm"
 onClick={() =>
 navigate(`/verifier/search?query=${encodeURIComponent(listSearchQuery)}`)
 }
 >
 <UserPlus className="w-4 h-4 mr-1.5" />
 Search Registry to Request Access
 </Button>
 )}
 </div>
 </Card>
 ) : (
 /* Accessible Students Grid */
 <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
 {filteredStudents?.map((student) => {
 const daysRemaining = getDaysRemaining(student.accessExpiresAt);
 const isExpired = daysRemaining <= 0;

 return (
 <Card
 key={student.studentId}
 className="flex flex-col border border-[var(--border)] hover:border-[var(--brand)]/40 hover:shadow-md transition-all duration-200"
 >
 {/* Avatar and Student Info */}
 <div className="flex items-start gap-3.5">
 <div className="w-12 h-12 rounded-full bg-[var(--brand-light)] text-[var(--brand)] flex items-center justify-center font-bold text-lg shrink-0 border border-[var(--brand)]/20 shadow-sm">
 {getInitials(student.studentName)}
 </div>
 <div className="min-w-0 flex-1">
 <h3
 className="font-semibold text-base text-[var(--text-primary)] truncate"
 title={student.studentName}
 >
 {student.studentName}
 </h3>
 {student.studentEmail && (
 <p className="text-xs text-[var(--text-secondary)] truncate">
 {student.studentEmail}
 </p>
 )}
 {student.institutionName && (
 <p className="text-xs text-[var(--text-muted)] truncate flex items-center gap-1.5 mt-1">
 <Building2 className="w-3.5 h-3.5 text-[var(--brand)] shrink-0" />
 <span className="truncate">{student.institutionName}</span>
 </p>
 )}
 </div>
 </div>

 {/* Grant Metadata */}
 <div className="mt-5 space-y-2.5 flex-1 text-xs sm:text-sm">
 <div className="flex items-center justify-between">
 <span className="text-[var(--text-muted)]">Access Scope</span>
 {student.hasAllAccess ? (
 <Badge variant="default" size="sm" className="font-medium">
 All Certificates
 </Badge>
 ) : (
 <Badge variant="primary" size="sm" className="font-medium">
 Specific ({student.certificateCount})
 </Badge>
 )}
 </div>

 <div className="flex items-center justify-between">
 <span className="text-[var(--text-muted)]">Certificates</span>
 <span className="font-semibold text-[var(--text-primary)]">
 {student.certificateCount}{' '}
 {student.certificateCount === 1 ? 'certificate' : 'certificates'}
 </span>
 </div>

 <div className="flex items-center justify-between">
 <span className="text-[var(--text-muted)]">Access expires</span>
 <span className="font-medium text-[var(--text-primary)]">
 {student.accessExpiresAt ? formatDate(student.accessExpiresAt) : 'N/A'}
 </span>
 </div>

 <div className="flex items-center justify-between pt-1">
 <span className="text-[var(--text-muted)]">Status</span>
 <DaysRemainingBadge expiresAt={student.accessExpiresAt} />
 </div>
 </div>

 {/* Action */}
 <div className="mt-5 pt-4 border-t border-[var(--border)]">
 {!isExpired ? (
 <Button
 variant="outline"
 className="w-full justify-center font-medium hover:border-[var(--brand)] hover:text-[var(--brand)]"
 onClick={() =>
 navigate(`/verifier/accessible-certificates/${student.studentId}`)
 }
 >
 <Eye className="w-4 h-4 mr-1.5" />
 View Certificates
 </Button>
 ) : (
 <div className="text-center py-1.5 rounded-lg bg-[var(--bg-base)] border border-[var(--border)]">
 <span className="text-xs font-medium text-[var(--text-muted)] flex items-center justify-center gap-1.5">
 <Clock className="w-3.5 h-3.5 text-[var(--danger)]" />
 Access Expired
 </span>
 </div>
 )}
 </div>
 </Card>
 );
 })}
 </div>
 )}
 </div>
 )}
 </div>
 </DashboardLayout>
 );
}
