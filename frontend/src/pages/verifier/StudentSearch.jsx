import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import {
 Search,
 Mail,
 Award,
 CreditCard,
 Fingerprint,
 ShieldCheck,
 Building2,
 GraduationCap,
 Clock,
 ExternalLink,
 CheckCircle2,
 AlertCircle,
 Shield,
 HelpCircle,
 Loader2,
 User
} from 'lucide-react';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/shared/Card';
import Badge from '../../components/shared/Badge';
import Button from '../../components/shared/Button';
import Modal from '../../components/shared/Modal';
import api from '../../services/api';
import { formatDate } from '../../utils/helpers';

const TABS = [
 { id: 'email', label: 'Email', icon: Mail, placeholder: 'e.g. student@example.com' },
 { id: 'serial', label: 'Certificate Serial', icon: Award, placeholder: 'e.g. UIU-2026-BSCSE-001 or certificate serial' },
 { id: 'nid', label: 'NID / Birth Certificate', icon: Fingerprint, placeholder: '10-17 digit national identity number' },
];

const DURATION_OPTIONS = [
 { label: '7 days', value: 7 },
 { label: '30 days', value: 30 },
 { label: '90 days', value: 90 },
 { label: 'Custom', value: 'custom' },
];

export default function StudentSearch() {
 const navigate = useNavigate();
 const [searchParams] = useSearchParams();

 // Search State
 const [activeTab, setActiveTab] = useState('email');
 const [query, setQuery] = useState('');
 const [searching, setSearching] = useState(false);
 const [searchError, setSearchError] = useState('');
 const [hasSearched, setHasSearched] = useState(false);
 const [searchResult, setSearchResult] = useState(null); // null, { found: false }, or { found: true, student: {...} }

 // Check URL params on initial load
 useEffect(() => {
 const qParam = searchParams.get('q') || searchParams.get('query');
 const typeParam = searchParams.get('type');
 if (typeParam && TABS.some(t => t.id === typeParam)) {
 setActiveTab(typeParam);
 }
 if (qParam) {
 setQuery(qParam);
 }
 }, [searchParams]);

 // Modal State
 const [isModalOpen, setIsModalOpen] = useState(false);
 const [accessScope, setAccessScope] = useState('specific'); // 'specific' | 'all'
 const [purpose, setPurpose] = useState('');
 const [durationOption, setDurationOption] = useState(30);
 const [customDays, setCustomDays] = useState('30');
 const [confirmedLegitimate, setConfirmedLegitimate] = useState(false);
 const [submittingRequest, setSubmittingRequest] = useState(false);

 // Tab change handler
 const handleTabChange = (tabId) => {
 setActiveTab(tabId);
 setQuery('');
 setSearchError('');
 setHasSearched(false);
 setSearchResult(null);
 };

 // Validation
 const validateQuery = () => {
 const trimmed = query.trim();
 if (!trimmed) {
 setSearchError('Please enter a search identifier.');
 return false;
 }

 if (activeTab === 'email') {
 const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
 if (!emailRegex.test(trimmed)) {
 setSearchError('Please enter a valid email address (e.g. name@domain.com).');
 return false;
 }
 } else if (activeTab === 'serial') {
 if (trimmed.length < 3) {
 setSearchError('Certificate Serial must be at least 3 characters.');
 return false;
 }
 } else if (activeTab === 'nid') {
 const nidRegex = /^\d{10,17}$/;
 if (!nidRegex.test(trimmed)) {
 setSearchError('National ID must be a numeric value between 10 and 17 digits.');
 return false;
 }
 } else if (activeTab === 'student_id') {
 if (trimmed.length < 2) {
 setSearchError('Student ID must be at least 2 characters.');
 return false;
 }
 }

 setSearchError('');
 return true;
 };

 // Perform search
 const handleSearch = async (e) => {
 if (e) e.preventDefault();
 if (!validateQuery()) return;

 setSearching(true);
 setSearchError('');
 setHasSearched(true);
 setSearchResult(null);

 try {
 const { data } = await api.get('/verifier/search', {
 params: {
 type: activeTab,
 q: query.trim()
 }
 });

 if (data.success && data.found) {
 setSearchResult({
 found: true,
 student: data.student
 });
 } else {
 setSearchResult({
 found: false,
 message: data.message || 'No student found with this identifier'
 });
 }
 } catch (err) {
 console.error('Search failed:', err);
 const msg = err.response?.data?.message || 'An error occurred while searching. Please try again.';
 setSearchError(msg);
 setSearchResult(null);
 } finally {
 setSearching(false);
 }
 };

 // Open modal
 const openRequestModal = () => {
 setPurpose('');
 setDurationOption(30);
 setCustomDays('30');
 setConfirmedLegitimate(false);
 setAccessScope(searchResult?.student?.searchedCertificate ? 'specific' : 'all');
 setIsModalOpen(true);
 };

 // Submit access request
 const handleSubmitRequest = async (e) => {
 e.preventDefault();
 if (!searchResult?.student?.id) return;

 const trimmedPurpose = purpose.trim();
 if (trimmedPurpose.length < 20) {
 toast.error('Purpose must be at least 20 characters.');
 return;
 }
 if (trimmedPurpose.length > 500) {
 toast.error('Purpose cannot exceed 500 characters.');
 return;
 }

 const durationDays = durationOption === 'custom' ? parseInt(customDays, 10) : durationOption;
 if (isNaN(durationDays) || durationDays < 1 || durationDays > 365) {
 toast.error('Duration must be between 1 and 365 days.');
 return;
 }

 if (!confirmedLegitimate) {
 toast.error('Please confirm the request is for legitimate purposes.');
 return;
 }

 const hasSearchedCert = !!searchResult?.student?.searchedCertificate;
 const isRequestingAll = !hasSearchedCert || accessScope === 'all';
 const certId = !isRequestingAll ? searchResult.student.searchedCertificate.id : null;

 setSubmittingRequest(true);
 try {
 const { data } = await api.post('/verifier/access-requests', {
 studentId: searchResult.student.id,
 purpose: trimmedPurpose,
 requestedDurationDays: durationDays,
 certificateId: certId,
 requestAllCertificates: isRequestingAll
 });

 if (data.success) {
 toast.success(
 isRequestingAll
 ? 'Access request for all certificates sent successfully'
 : 'Access request for certificate sent successfully'
 );
 setIsModalOpen(false);
 // Update local state to pending request
 setSearchResult((prev) => {
 if (!prev?.student) return prev;
 return {
 ...prev,
 student: {
 ...prev.student,
 hasPendingRequest: true
 }
 };
 });
 } else {
 toast.error(data.message || 'Failed to send access request.');
 }
 } catch (err) {
 console.error('Request failed:', err);
 toast.error(err.response?.data?.message || 'Failed to send access request.');
 } finally {
 setSubmittingRequest(false);
 }
 };

 const getInitials = (name) => {
 if (!name) return 'S';
 const parts = name.trim().split(' ');
 if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
 return name.substring(0, 2).toUpperCase();
 };

 return (
 <DashboardLayout>
 <div className="max-w-4xl mx-auto space-y-8">
 {/* Header */}
 <div className="text-center sm:text-left">
 <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[var(--brand-light)] text-[var(--brand)] text-xs font-semibold uppercase tracking-wider mb-2">
 <Shield className="w-3.5 h-3.5" />
 Verified Credential Search
 </div>
 <h1 className="text-2xl sm:text-3xl font-bold text-[var(--text-primary)] tracking-tight">
 Search Student
 </h1>
 <p className="mt-1 text-sm sm:text-base text-[var(--text-secondary)]">
 Search by exact identifier only. Names are not accepted.
 </p>
 </div>

 {/* Search Card */}
 <Card className="p-6 sm:p-8 shadow-sm border border-[var(--border)] bg-[var(--bg-surface)] backdrop-blur-md">
 {/* Tabs */}
 <div className="flex border-b border-[var(--border)] gap-2 sm:gap-6 mb-6 overflow-x-auto">
 {TABS?.map((tab) => {
 const Icon = tab.icon;
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
 <Icon className="w-4 h-4" />
 {tab.label}
 </button>
 );
 })}
 </div>

 {/* Form */}
 <form onSubmit={handleSearch} className="space-y-4">
 <div>
 <label htmlFor="search-input" className="block text-sm font-medium text-[var(--text-primary)] mb-1.5">
 {activeTab === 'email' && 'Student Email Address'}
 {activeTab === 'serial' && 'Certificate Serial Number'}
 {activeTab === 'nid' && 'National ID / Birth Certificate Number'}
 {activeTab === 'student_id' && 'Student Roll / Enrollment ID'}
 </label>

 <div className="relative flex items-center">
 <div className="absolute left-3.5 text-[var(--text-muted)] pointer-events-none">
 {activeTab === 'email' && <Mail className="w-5 h-5" />}
 {activeTab === 'serial' && <Award className="w-5 h-5" />}
 {activeTab === 'nid' && <Fingerprint className="w-5 h-5" />}
 {activeTab === 'student_id' && <CreditCard className="w-5 h-5" />}
 </div>

 <input
 id="search-input"
 type={activeTab === 'email' ? 'email' : 'text'}
 value={query}
 onChange={(e) => {
 setQuery(e.target.value);
 if (searchError) setSearchError('');
 }}
 placeholder={TABS.find((t) => t.id === activeTab)?.placeholder}
 className={`w-full h-11 pl-11 pr-28 rounded-lg border bg-[var(--bg-surface)] text-sm text-[var(--text-primary)] outline-none transition-all ${
 searchError
 ? 'border-[var(--danger)] focus:border-[var(--danger)] focus:shadow-[0_0_0_3px_rgba(239,68,68,0.15)]'
 : 'border-[var(--border)] focus:border-[var(--brand)] focus:shadow-[0_0_0_3px_rgba(99,102,241,0.15)]'
 }`}
 />

 <div className="absolute right-1.5">
 <Button
 type="submit"
 variant="primary"
 size="sm"
 loading={searching}
 disabled={searching || !query.trim()}
 className="h-8 px-4"
 >
 {!searching && <Search className="w-3.5 h-3.5 mr-1.5" />}
 Search
 </Button>
 </div>
 </div>

 {/* Validation / error message */}
 {searchError && (
 <div className="flex items-center gap-1.5 mt-2 text-xs text-[var(--danger)]">
 <AlertCircle className="w-4 h-4 shrink-0" />
 <span>{searchError}</span>
 </div>
 )}

 {/* Helper Notices */}
 {activeTab === 'serial' && (
 <p className="mt-2.5 text-xs text-[var(--text-muted)] flex items-center gap-1.5">
 <Award className="w-4 h-4 text-[var(--brand)] shrink-0" />
 <span>Search using the unique serial printed on the candidate's certificate to find and request verified access.</span>
 </p>
 )}

 {activeTab === 'nid' && (
 <p className="mt-2.5 text-xs text-[var(--text-muted)] flex items-center gap-1.5">
 <ShieldCheck className="w-4 h-4 text-[var(--brand)] shrink-0" />
 <span>Your search is secure. NID numbers are never stored as plain text.</span>
 </p>
 )}
 </div>
 </form>
 </Card>

 {/* Results Area */}
 {searching && (
 <div className="py-12 flex flex-col items-center justify-center space-y-3">
 <Loader2 className="w-8 h-8 animate-spin text-[var(--brand)]" />
 <p className="text-sm text-[var(--text-secondary)]">Searching registry for exact matching identifier...</p>
 </div>
 )}

 {!searching && hasSearched && searchResult && (
 <div className="transition-all duration-300">
 {/* NOT FOUND STATE */}
 {!searchResult.found ? (
 <Card className="p-8 text-center border border-[var(--border)] bg-[var(--bg-elevated)]/40 rounded-xl">
 <div className="w-12 h-12 mx-auto rounded-full bg-[var(--bg-elevated)] flex items-center justify-center text-[var(--text-muted)] mb-3">
 <HelpCircle className="w-6 h-6" />
 </div>
 <h3 className="text-base font-semibold text-[var(--text-primary)]">
 No student found with this identifier
 </h3>
 <p className="text-sm text-[var(--text-secondary)] mt-1 max-w-md mx-auto">
 Make sure the information is exactly correct. Students who have disabled verifier searches will not appear.
 </p>
 </Card>
 ) : (
 /* FOUND STATE */
 <div className="rounded-xl border-2 border-[var(--success)] bg-[var(--bg-surface)] p-6 sm:p-7 shadow-sm transition-all">
 <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6">
 {/* Student Details */}
 <div className="flex items-start gap-4">
 <div className="w-14 h-14 rounded-full bg-[var(--brand-light)] text-[var(--brand)] flex items-center justify-center font-bold text-xl shrink-0 border border-[var(--brand)]/20 shadow-sm">
 {getInitials(searchResult.student?.name)}
 </div>
 <div className="space-y-1 min-w-0">
 <div className="flex items-center gap-2 flex-wrap">
 <h2 className="text-xl sm:text-2xl font-bold text-[var(--text-primary)] truncate">
 {searchResult.student?.name}
 </h2>
 <Badge variant="success" size="sm" className="flex items-center gap-1">
 <CheckCircle2 className="w-3 h-3" />
 Verified Student
 </Badge>
 </div>

 {searchResult.student?.email && (
 <p className="text-sm text-[var(--text-secondary)] flex items-center gap-1.5">
 <Mail className="w-3.5 h-3.5 text-[var(--text-muted)]" />
 {searchResult.student.email}
 </p>
 )}

 {searchResult.student?.studentId && (
 <p className="text-sm text-[var(--text-secondary)] flex items-center gap-1.5">
 <CreditCard className="w-3.5 h-3.5 text-[var(--text-muted)]" />
 <span>Student ID: <strong className="text-[var(--text-primary)]">{searchResult.student.studentId}</strong></span>
 </p>
 )}

 <div className="pt-1 flex items-center gap-1.5 text-xs text-[var(--text-muted)]">
 <GraduationCap className="w-4 h-4 text-[var(--brand)] shrink-0" />
 <span>
 {searchResult.student?.currentEnrollment ? (
 <>
 <strong className="text-[var(--text-secondary)]">{searchResult.student.currentEnrollment.institutionName}</strong>
 {searchResult.student.currentEnrollment.program ? ` — ${searchResult.student.currentEnrollment.program}` : ''}
 </>
 ) : (
 'Not enrolled'
 )}
 </span>
 </div>
 </div>
 </div>

 {/* Actions according to access state */}
 <div className="sm:text-right shrink-0 flex flex-col items-start sm:items-end justify-center gap-1.5">
 {searchResult.student?.hasActiveAccess ? (
 <div>
 <Button
 variant="success"
 size="md"
 onClick={() => navigate(`/verifier/accessible-certificates/${searchResult.student.id}`)}
 className="font-semibold shadow-sm"
 >
 <CheckCircle2 className="w-4 h-4 mr-2" />
 View Certificates
 </Button>
 <p className="text-xs text-[var(--success)] mt-1.5 font-medium">
 Active access granted
 </p>
 </div>
 ) : searchResult.student?.hasPendingRequest ? (
 <div>
 <Button
 variant="secondary"
 size="md"
 disabled
 className="opacity-70 cursor-not-allowed"
 >
 <Clock className="w-4 h-4 mr-2" />
 Request Pending
 </Button>
 <p className="text-xs text-[var(--text-muted)] mt-1.5 max-w-xs sm:text-right">
 Your access request is awaiting the student's response
 </p>
 </div>
 ) : (
 <div>
 <Button
 variant="primary"
 size="md"
 onClick={openRequestModal}
 className="font-semibold shadow-sm"
 >
 Request Access
 </Button>
 </div>
 )}
 </div>
 </div>

 {/* Searched Certificate Banner (When searched by Serial) */}
 {searchResult.student?.searchedCertificate && (
 <div className="mt-5 pt-4 border-t border-[var(--border)]">
 <div className="p-3.5 rounded-lg bg-[var(--bg-elevated)]/70 border border-[var(--border)] flex flex-col sm:flex-row sm:items-center justify-between gap-3">
 <div className="flex items-center gap-3">
 <div className="w-9 h-9 rounded-lg bg-[var(--brand-light)] text-[var(--brand)] flex items-center justify-center shrink-0">
 <Award className="w-5 h-5" />
 </div>
 <div>
 <div className="flex items-center gap-2">
 <span className="text-xs font-semibold uppercase tracking-wider text-[var(--brand)]">Target Certificate</span>
 {(searchResult.student.searchedCertificate.certificateLevel || searchResult.student.searchedCertificate.educationLevel) && (
 <Badge variant="primary" size="sm">
 {searchResult.student.searchedCertificate.certificateLevel || searchResult.student.searchedCertificate.educationLevel}
 </Badge>
 )}
 </div>
 <p className="text-sm font-semibold text-[var(--text-primary)]">
 {searchResult.student.searchedCertificate.certificateName}
 </p>
 <p className="text-xs text-[var(--text-secondary)] font-mono">
 Serial: <strong className="text-[var(--text-primary)]">{searchResult.student.searchedCertificate.serial || searchResult.student.searchedCertificate.serialNumber}</strong>
 </p>
 </div>
 </div>
 {searchResult.student.searchedCertificate.issueDate && (
 <div className="text-xs text-[var(--text-muted)] sm:text-right">
 <span>Issued: {formatDate(searchResult.student.searchedCertificate.issueDate)}</span>
 </div>
 )}
 </div>
 </div>
 )}
 </div>
 )}
 </div>
 )}

 {/* Request Access Modal */}
 <Modal
 isOpen={isModalOpen}
 onClose={() => !submittingRequest && setIsModalOpen(false)}
 title="Request Student Certificate Access"
 size="md"
 >
 {searchResult?.student && (
 <form onSubmit={handleSubmitRequest} className="space-y-5">
 {/* Student Details Top Banner */}
 <div className="rounded-lg bg-[var(--bg-elevated)] p-3.5 border border-[var(--border)] flex items-center gap-3">
 <div className="w-10 h-10 rounded-full bg-[var(--brand-light)] text-[var(--brand)] font-bold flex items-center justify-center text-sm shrink-0">
 {getInitials(searchResult.student.name)}
 </div>
 <div className="min-w-0 flex-1">
 <h4 className="text-sm font-semibold text-[var(--text-primary)] truncate">
 {searchResult.student.name}
 </h4>
 <p className="text-xs text-[var(--text-secondary)] truncate">
 {searchResult.student.email || 'Verified Student Record'}
 </p>
 </div>
 </div>

 {/* Access Scope Selector */}
 {searchResult.student.searchedCertificate ? (
 <div className="space-y-2">
 <label className="block text-sm font-medium text-[var(--text-primary)]">
 Access Scope <span className="text-[var(--danger)]">*</span>
 </label>
 <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
 <button
 type="button"
 onClick={() => setAccessScope('specific')}
 className={`p-3 text-left rounded-lg border transition-all ${
 accessScope === 'specific'
 ? 'border-[var(--brand)] bg-[var(--brand-light)]/40 ring-1 ring-[var(--brand)]'
 : 'border-[var(--border)] bg-[var(--bg-surface)] hover:border-[var(--border-strong)]'
 }`}
 >
 <div className="flex items-center justify-between mb-1">
 <span className="text-xs font-bold text-[var(--text-primary)] flex items-center gap-1.5">
 <Award className="w-3.5 h-3.5 text-[var(--brand)]" />
 This Certificate Only
 </span>
 <span className={`w-3.5 h-3.5 rounded-full border flex items-center justify-center ${
 accessScope === 'specific' ? 'border-[var(--brand)] bg-[var(--brand)]' : 'border-[var(--border-strong)]'
 }`}>
 {accessScope === 'specific' && <span className="w-1.5 h-1.5 rounded-full bg-[var(--bg-surface)]" />}
 </span>
 </div>
 <p className="text-[11px] text-[var(--text-secondary)] leading-tight">
 Request access only for <strong>{searchResult.student.searchedCertificate.certificateName}</strong> ({searchResult.student.searchedCertificate.serial || searchResult.student.searchedCertificate.serialNumber})
 </p>
 </button>

 <button
 type="button"
 onClick={() => setAccessScope('all')}
 className={`p-3 text-left rounded-lg border transition-all ${
 accessScope === 'all'
 ? 'border-[var(--brand)] bg-[var(--brand-light)]/40 ring-1 ring-[var(--brand)]'
 : 'border-[var(--border)] bg-[var(--bg-surface)] hover:border-[var(--border-strong)]'
 }`}
 >
 <div className="flex items-center justify-between mb-1">
 <span className="text-xs font-bold text-[var(--text-primary)] flex items-center gap-1.5">
 <GraduationCap className="w-3.5 h-3.5 text-[var(--brand)]" />
 All Certificates
 </span>
 <span className={`w-3.5 h-3.5 rounded-full border flex items-center justify-center ${
 accessScope === 'all' ? 'border-[var(--brand)] bg-[var(--brand)]' : 'border-[var(--border-strong)]'
 }`}>
 {accessScope === 'all' && <span className="w-1.5 h-1.5 rounded-full bg-[var(--bg-surface)]" />}
 </span>
 </div>
 <p className="text-[11px] text-[var(--text-secondary)] leading-tight">
 Request comprehensive access to view all academic certificates owned by this student.
 </p>
 </button>
 </div>
 </div>
 ) : (
 <div className="p-3 rounded-lg bg-[var(--bg-elevated)]/60 border border-[var(--border)] flex items-center gap-2 text-xs text-[var(--text-secondary)]">
 <GraduationCap className="w-4 h-4 text-[var(--brand)] shrink-0" />
 <span>Scope: Access will be requested for all certificates of this student.</span>
 </div>
 )}

 {/* Purpose textarea */}
 <div className="space-y-1.5">
 <div className="flex justify-between items-center text-sm">
 <label htmlFor="purpose-input" className="font-medium text-[var(--text-primary)]">
 Why do you need access? <span className="text-[var(--danger)]">*</span>
 </label>
 <span className={`text-xs ${purpose.trim().length >= 20 ? 'text-[var(--text-muted)]' : 'text-[var(--danger)]'}`}>
 {purpose.length} / 500 (min 20)
 </span>
 </div>
 <textarea
 id="purpose-input"
 rows={4}
 value={purpose}
 onChange={(e) => setPurpose(e.target.value.slice(0, 500))}
 placeholder="Explain the reason for requesting access (e.g., employment background verification, academic evaluation, visa application)..."
 className="w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] p-3 text-sm text-[var(--text-primary)] outline-none transition-all focus:border-[var(--brand)] focus:shadow-[0_0_0_3px_rgba(99,102,241,0.12)] resize-none"
 required
 />
 {purpose.trim().length > 0 && purpose.trim().length < 20 && (
 <p className="text-xs text-[var(--danger)]">
 Please provide at least 20 characters explaining your purpose.
 </p>
 )}
 </div>

 {/* Access Duration */}
 <div className="space-y-2">
 <label className="block text-sm font-medium text-[var(--text-primary)]">
 Access Duration
 </label>
 <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
 {DURATION_OPTIONS?.map((opt) => {
 const isSelected = durationOption === opt.value;
 return (
 <button
 key={String(opt.value)}
 type="button"
 onClick={() => setDurationOption(opt.value)}
 className={`py-2 px-3 text-xs font-medium rounded-lg border transition-all text-center ${
 isSelected
 ? 'border-[var(--brand)] bg-[var(--brand-light)] text-[var(--brand)] font-semibold'
 : 'border-[var(--border)] bg-[var(--bg-surface)] text-[var(--text-secondary)] hover:border-[var(--border-strong)]'
 }`}
 >
 {opt.label}
 </button>
 );
 })}
 </div>

 {durationOption === 'custom' && (
 <div className="pt-2">
 <label htmlFor="custom-duration" className="block text-xs font-medium text-[var(--text-secondary)] mb-1">
 Specify days (1 to 365):
 </label>
 <input
 id="custom-duration"
 type="number"
 min="1"
 max="365"
 value={customDays}
 onChange={(e) => setCustomDays(e.target.value)}
 className="w-32 h-9 px-3 text-sm rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] text-[var(--text-primary)] outline-none focus:border-[var(--brand)]"
 />
 </div>
 )}
 </div>

 {/* Confirmation Checkbox */}
 <div className="pt-2">
 <label className="flex items-start gap-2.5 cursor-pointer text-sm select-none">
 <input
 type="checkbox"
 checked={confirmedLegitimate}
 onChange={(e) => setConfirmedLegitimate(e.target.checked)}
 className="mt-1 h-4 w-4 rounded border-[var(--border)] text-[var(--brand)] focus:ring-[var(--brand)]"
 />
 <span className="text-xs sm:text-sm text-[var(--text-secondary)]">
 I confirm this request is for legitimate purposes and aligns with verified evaluation standards.
 </span>
 </label>
 </div>

 {/* Modal Footer Buttons */}
 <div className="pt-4 flex justify-end gap-2 border-t border-[var(--border)]">
 <Button
 type="button"
 variant="secondary"
 size="md"
 onClick={() => setIsModalOpen(false)}
 disabled={submittingRequest}
 >
 Cancel
 </Button>
 <Button
 type="submit"
 variant="primary"
 size="md"
 loading={submittingRequest}
 disabled={!confirmedLegitimate || purpose.trim().length < 20 || submittingRequest}
 >
 Send Access Request
 </Button>
 </div>
 </form>
 )}
 </Modal>
 </div>
 </DashboardLayout>
 );
}
