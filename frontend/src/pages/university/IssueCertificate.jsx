import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import AsyncSelect from 'react-select/async';
import debounce from 'lodash/debounce';
import toast from 'react-hot-toast';
import {
 Award,
 UploadCloud,
 FileText,
 Download,
 CheckCircle2,
 AlertCircle,
 XCircle,
 Copy,
 ExternalLink,
 RefreshCw,
 Search,
 User,
 GraduationCap,
 Calendar,
 Building2,
 ShieldCheck,
 Clock,
 Sparkles,
 Info
} from 'lucide-react';

import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/shared/Card';
import Input from '../../components/shared/Input';
import Button from '../../components/shared/Button';
import Badge from '../../components/shared/Badge';
import Modal from '../../components/shared/Modal';
import LoadingSpinner from '../../components/shared/LoadingSpinner';
import api from '../../services/api';


export default function IssueCertificate() {
 const navigate = useNavigate();
 const location = useLocation();

 // Active tab: 'single' | 'batch'
 const [activeTab, setActiveTab] = useState('single');

 // University Profile defaults
 const [universityProfile, setUniversityProfile] = useState(null);
 const [defaultAuthorityName, setDefaultAuthorityName] = useState('');
 const [defaultAuthorityTitle, setDefaultAuthorityTitle] = useState('');
 const [certificateLevels, setCertificateLevels] = useState([]);

 // ---------------------------------------------------------------------------
 // Load University Profile
 // ---------------------------------------------------------------------------
 useEffect(() => {
 const fetchProfile = async () => {
 try {
 const res = await api.get('/university/profile');
 if (res.data?.success && res.data?.profile) {
 const prof = res.data.profile;
 setUniversityProfile(prof);
 const authName = prof.defaultAuthorityName || prof.default_authority_name || '';
 const authTitle = prof.defaultAuthorityTitle || prof.default_authority_title || '';
 setDefaultAuthorityName(authName);
 setDefaultAuthorityTitle(authTitle);
 }
 } catch (err) {
 // Fallback to /api/auth/me or continue gracefully
 try {
 const meRes = await api.get('/auth/me');
 if (meRes.data?.user) {
 setUniversityProfile(meRes.data.user);
 }
 } catch (_ignored) {}
 }
 };
 const fetchLevels = async () => {
 try {
 const res = await api.get('/university/certificate-levels');
 if (res.data?.success) {
 const activeLevels = res.data.certificate_levels
 .filter(l => l.isActive !== false)
 .map(l => ({ value: l.name, label: l.name }));
 setCertificateLevels(activeLevels.length > 0 ? activeLevels : [
 { value: 'Bachelor', label: 'Bachelor' },
 { value: 'Master', label: 'Master' }
 ]);
 }
 } catch (err) {
 console.error('Failed to fetch certificate levels:', err);
 }
 };
 fetchProfile();
 fetchLevels();
 }, []);

 return (
 <DashboardLayout>
 <div className="space-y-6 pb-12">
 {/* Page Header */}
 <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
 <div>
 <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-[var(--brand)]">
 <Award className="h-4 w-4" />
 <span>Academic Credential Registry</span>
 </div>
 <h1 className="mt-1 text-2xl sm:text-3xl font-bold text-[var(--text-primary)]">
 Issue Certificate
 </h1>
 <p className="mt-1 text-sm text-[var(--text-secondary)]">
 Issue verified, tamper-proof academic credentials individually or in batch.
 </p>
 </div>
 </div>

 {/* Tab Switching Navigation */}
 <div className="flex items-center gap-3 border-b border-[var(--border)] pb-4">
 <Button
 variant={activeTab === 'single' ? 'primary' : 'secondary'}
 onClick={() => setActiveTab('single')}
 icon={<Award className="h-4 w-4" />}
 className="transition-all duration-200"
 >
 Single Certificate
 </Button>
 <Button
 variant={activeTab === 'batch' ? 'primary' : 'secondary'}
 onClick={() => setActiveTab('batch')}
 icon={<UploadCloud className="h-4 w-4" />}
 className="transition-all duration-200"
 >
 Batch Upload
 </Button>
 </div>

 {/* Tab Content */}
 {activeTab === 'single' ? (
 <SingleCertificateTab
 defaultAuthorityName={defaultAuthorityName}
 defaultAuthorityTitle={defaultAuthorityTitle}
 universityProfile={universityProfile}
 locationState={location.state}
 certificateLevels={certificateLevels}
 />
 ) : (
 <BatchUploadTab
 defaultAuthorityName={defaultAuthorityName}
 defaultAuthorityTitle={defaultAuthorityTitle}
 certificateLevels={certificateLevels}
 />
 )}
 </div>
 </DashboardLayout>
 );
}

/* =============================================================================
 TAB 1: SINGLE CERTIFICATE
 ============================================================================= */
function SingleCertificateTab({
 defaultAuthorityName,
 defaultAuthorityTitle,
 universityProfile,
 locationState,
 certificateLevels,
}) {
 const navigate = useNavigate();

 // Search State
 const [selectedOption, setSelectedOption] = useState(null);
 const [defaultOptions, setDefaultOptions] = useState([]);

 // Form State
 const todayStr = useMemo(() => new Date().toISOString().split('T')[0], []);
 const [formData, setFormData] = useState({
 certificateName: '',
 certificateLevel: '',
 department: '',
 major: '',
 session: '',
 cgpa: '',
 degreeClass: '',
 issueDate: todayStr,
 convocationDate: '',
 authorityName: '',
 authorityTitle: '',
 });

 const [submitting, setSubmitting] = useState(false);
 const [fieldErrors, setFieldErrors] = useState({});

 // Success Modal State
 const [successModalOpen, setSuccessModalOpen] = useState(false);
 const [issuedSerial, setIssuedSerial] = useState('');
 const [issuedStudentName, setIssuedStudentName] = useState('');

 // Update authority defaults when profile loads
 useEffect(() => {
 setFormData((prev) => ({
 ...prev,
 authorityName: prev.authorityName || defaultAuthorityName,
 authorityTitle: prev.authorityTitle || defaultAuthorityTitle,
 }));
 }, [defaultAuthorityName, defaultAuthorityTitle]);

 // Load initial active enrolled students so they show immediately when clicked
 useEffect(() => {
 let isMounted = true;
 api.get('/university/students/search', { params: { q: '' } })
 .then((res) => {
 if (!isMounted) return;
 const students = res.data?.students || [];
 const options = students?.map((s) => ({
 value: s.id || s.studentId,
 label: `${s.name || s.studentName} (${s.rollNumber || s.roll_number || s.enrollmentNumber || 'No ID'})`,
 student: s,
 }));
 setDefaultOptions(options);
 })
 .catch((err) => {
 console.error('Failed to pre-load enrolled students:', err);
 });
 return () => {
 isMounted = false;
 };
 }, []);

 // Handle pre-selected student from location.state if navigated from Enrollments
 useEffect(() => {
 if (locationState?.preSelectedStudent) {
 const s = locationState.preSelectedStudent;
 const opt = {
 value: s.id || s.studentId,
 label: `${s.name || s.studentName} (${s.rollNumber || s.roll_number || s.enrollmentNumber || 'No ID'})`,
 student: s,
 };
 setSelectedOption(opt);
 applyStudentDetails(s);
 }
 }, [locationState]);

 // Fast search function responding to keyboard typing
 const debouncedFetchStudents = useMemo(() => {
 const loadFunc = (inputValue, callback) => {
 const query = (inputValue || '').trim();
 api.get('/university/students/search', {
 params: { q: query },
 })
 .then((res) => {
 const students = res.data?.students || [];
 const options = students?.map((s) => ({
 value: s.id || s.studentId,
 label: `${s.name || s.studentName} (${s.rollNumber || s.roll_number || s.enrollmentNumber || 'No ID'})`,
 student: s,
 }));
 callback(options);
 })
 .catch(() => {
 callback([]);
 });
 };
 return debounce(loadFunc, 150);
 }, []);

 const loadOptions = (inputValue, callback) => {
 debouncedFetchStudents(inputValue, callback);
 };

 // Populate fields when student is chosen
 const applyStudentDetails = async (student) => {
 if (!student) return;
 let certName = student.programName || student.certificateName || student.program || '';
 let certLevel = student.certificateLevelName || student.certificateLevel || '';
 let dept = student.department || '';
 let session = student.session || 'Spring 2026';
 let programId = student.programId || null;
 let enrollmentId = student.enrollmentId || student.id;

 if ((!certLevel || !certName) && enrollmentId) {
 try {
 const res = await api.get(`/university/enrollments/${enrollmentId}`);
 const enr = res.data?.enrollment || res.data?.data;
 if (enr) {
 certName = enr.programName || enr.program || certName;
 certLevel = enr.certificateLevelName || enr.certificateLevel || certLevel;
 dept = enr.department || dept;
 session = enr.batch || session;
 if (enr.programId) programId = enr.programId;
 }
 } catch (_e) {
 // use available details
 }
 }

 setFormData((prev) => ({
 ...prev,
 certificateName: certName,
 certificateLevel: certLevel,
 department: dept,
 major: student.major || prev.major || '',
 session: session || prev.session || 'Spring 2026',
 programId: programId || prev.programId,
 }));
 };

 const handleStudentSelect = (option) => {
 setSelectedOption(option);
 setFieldErrors((prev) => ({ ...prev, student: null }));
 if (option?.student) {
 applyStudentDetails(option.student);
 }
 };

 const handleInputChange = (field, value) => {
 setFormData((prev) => ({ ...prev, [field]: value }));
 if (fieldErrors[field]) {
 setFieldErrors((prev) => ({ ...prev, [field]: null }));
 }
 if ((field === 'cgpa' || field === 'degreeClass') && value) {
 setFieldErrors((prev) => {
 const next = { ...prev };
 delete next.cgpa;
 delete next.degreeClass;
 return next;
 });
 }
 };

 // Form Submission
 const handleSubmit = async (e) => {
 e.preventDefault();

 const errors = {};
 if (!selectedOption?.student) {
 errors.student = 'Please search and select an enrolled student';
 }
 if (!formData.certificateName?.trim()) {
 errors.certificateName = 'Certificate name is required (select an enrolled student)';
 }
 if (!formData.certificateLevel?.trim()) {
 errors.certificateLevel = 'Certificate level is required from student enrollment';
 }
 if (!formData.session?.trim()) {
 errors.session = 'Academic session is required';
 }
 if (!formData.issueDate) {
 errors.issueDate = 'Issue date is required';
 }
 if (!formData.authorityName?.trim()) {
 errors.authorityName = 'Authority name is required';
 }
 if (!formData.authorityTitle?.trim()) {
 errors.authorityTitle = 'Authority title is required';
 }

 const hasCgpa = formData.cgpa !== '' && formData.cgpa !== null && formData.cgpa !== undefined;
 const hasDegreeClass = Boolean(formData.degreeClass?.trim());

 if (!hasCgpa && !hasDegreeClass) {
 errors.cgpa = 'Either CGPA or Degree Class is required';
 errors.degreeClass = 'Either CGPA or Degree Class is required';
 } else if (hasCgpa) {
 const num = parseFloat(formData.cgpa);
 if (isNaN(num) || num < 0 || num > 4.0) {
 errors.cgpa = 'CGPA must be between 0.00 and 4.00';
 }
 }

 if (Object.keys(errors).length > 0) {
 setFieldErrors(errors);
 toast.error('Please complete all required fields correctly.');
 return;
 }

 setSubmitting(true);
 try {
 const payload = {
 studentId: selectedOption.student.id || selectedOption.student.studentId,
 enrollmentId: selectedOption.student.enrollmentId || null,
 programId: formData.programId || selectedOption.student.programId || null,
 certificateName: formData.certificateName.trim(),
 certificateLevel: formData.certificateLevel.trim(),
 department: formData.department?.trim() || null,
 major: formData.major?.trim() || null,
 session: formData.session.trim(),
 cgpa: formData.cgpa ? parseFloat(formData.cgpa) : null,
 degreeClass: formData.degreeClass?.trim() || null,
 issueDate: formData.issueDate,
 convocationDate: formData.convocationDate || null,
 authorityName: formData.authorityName.trim(),
 authorityTitle: formData.authorityTitle.trim(),
 };

 const res = await api.post('/university/certificates', payload);
 const resData = res.data?.data || res.data || {};
 const generatedSerial = resData.serial || 'BSC-26-000001M';

 toast.success('Certificate issued successfully');
 setIssuedSerial(generatedSerial);
 setIssuedStudentName(
 selectedOption.student.name || selectedOption.student.studentName || 'Student'
 );
 setSuccessModalOpen(true);
 } catch (err) {
 const msg =
 err.response?.data?.message ||
 err.response?.data?.error ||
 'Failed to issue certificate. Please check details and try again.';
 toast.error(msg);
 // We keep the form filled so user does not lose data!
 } finally {
 setSubmitting(false);
 }
 };

 // Reset Form for next certificate
 const handleResetForm = () => {
 setSelectedOption(null);
 setFormData({
 certificateName: '',
 certificateLevel: '',
 department: '',
 major: '',
 session: '',
 cgpa: '',
 degreeClass: '',
 issueDate: todayStr,
 convocationDate: '',
 authorityName: defaultAuthorityName,
 authorityTitle: defaultAuthorityTitle,
 });
 setFieldErrors({});
 setSuccessModalOpen(false);
 };

 const copySerialToClipboard = () => {
 if (issuedSerial) {
 navigator.clipboard.writeText(issuedSerial);
 toast.success('Serial number copied to clipboard!');
 }
 };

 return (
 <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
 {/* Left Column: Form (8 cols) */}
 <div className="lg:col-span-8 space-y-6">
 <form onSubmit={handleSubmit} className="space-y-6">
 {/* PART 1 — Select Student */}
 <Card className="space-y-4">
 <div className="flex items-center gap-2 border-b border-[var(--border)] pb-3">
 <span className="flex h-6 w-6 items-center justify-center rounded-full bg-[var(--brand-light)] text-xs font-bold text-[var(--brand)]">
 1
 </span>
 <h2 className="text-base font-semibold text-[var(--text-primary)]">
 Select Student
 </h2>
 </div>

 <div>
 <label className="block text-sm font-medium text-[var(--text-secondary)] mb-1.5">
 Search Enrolled Student <span className="text-[var(--danger)]">*</span>
 </label>
 <div className="relative">
 <AsyncSelect
 value={selectedOption}
 onChange={handleStudentSelect}
 loadOptions={loadOptions}
 defaultOptions={defaultOptions.length > 0 ? defaultOptions : true}
 openMenuOnClick={true}
 placeholder="Click to view enrolled students, or type to search..."
 noOptionsMessage={({ inputValue }) =>
 'No enrolled active students found'
 }
 loadingMessage={() => 'Searching enrolled students...'}
 isClearable
 formatOptionLabel={(option) => {
 const s = option.student;
 return (
 <div className="py-1">
 <div className="font-semibold text-[var(--text-primary)]">
 {s.name || s.studentName}
 </div>
 <div className="flex flex-wrap items-center gap-2 text-xs text-[var(--text-muted)] mt-0.5">
 <span>
 ID: <strong className="text-[var(--text-secondary)]">{s.rollNumber || s.roll_number || 'N/A'}</strong>
 </span>
 <span>•</span>
 <span>
 Enrollment: <strong className="text-[var(--text-secondary)]">{s.enrollmentNumber || s.enrollment_number || 'N/A'}</strong>
 </span>
 {s.program && (
 <>
 <span>•</span>
 <span>{s.program}</span>
 </>
 )}
 </div>
 </div>
 );
 }}
 styles={{
 control: (base, state) => ({
 ...base,
 backgroundColor: 'var(--bg-surface)',
 borderColor: fieldErrors.student
 ? 'var(--danger)'
 : state.isFocused
 ? 'var(--brand)'
 : 'var(--border)',
 borderRadius: '8px',
 minHeight: '44px',
 boxShadow: state.isFocused
 ? '0 0 0 3px rgba(99, 102, 241, 0.15)'
 : 'none',
 '&:hover': {
 borderColor: state.isFocused ? 'var(--brand)' : 'var(--border-strong)',
 },
 }),
 menu: (base) => ({
 ...base,
 backgroundColor: 'var(--bg-surface)',
 border: '1px solid var(--border)',
 borderRadius: '8px',
 boxShadow: 'var(--shadow-lg)',
 zIndex: 999,
 }),
 option: (base, state) => ({
 ...base,
 backgroundColor: state.isSelected
 ? 'var(--brand)'
 : state.isFocused
 ? 'var(--bg-elevated)'
 : 'transparent',
 color: state.isSelected ? '#fff' : 'var(--text-primary)',
 cursor: 'pointer',
 }),
 singleValue: (base) => ({
 ...base,
 color: 'var(--text-primary)',
 }),
 input: (base) => ({
 ...base,
 color: 'var(--text-primary)',
 }),
 }}
 />
 </div>
 {fieldErrors.student && (
 <p className="mt-1 text-xs text-[var(--danger)]">{fieldErrors.student}</p>
 )}
 </div>

 {/* Read-only Student Summary Card */}
 {selectedOption?.student && (
 <div className="mt-4 rounded-xl border border-[var(--border)] bg-[var(--bg-elevated)] p-4 shadow-sm animate-fadeIn">
 <div className="flex items-start justify-between gap-4">
 <div className="space-y-1">
 <div className="flex items-center gap-2">
 <h3 className="text-base font-bold text-[var(--text-primary)]">
 {selectedOption.student.name || selectedOption.student.studentName}
 </h3>
 <Badge
 variant={
 selectedOption.student.status === 'active' ||
 selectedOption.student.status === 'enrolled'
 ? 'success'
 : selectedOption.student.status === 'graduated'
 ? 'info'
 : 'default'
 }
 size="sm"
 >
 {selectedOption.student.status
 ? selectedOption.student.status.toUpperCase()
 : 'ENROLLED'}
 </Badge>
 </div>
 <p className="text-xs text-[var(--text-muted)]">
 {selectedOption.student.email}
 </p>
 </div>
 <div className="rounded-lg bg-[var(--brand-light)] px-2.5 py-1 text-xs font-semibold text-[var(--brand)]">
 Student ID: {selectedOption.student.rollNumber || selectedOption.student.roll_number || 'N/A'}
 </div>
 </div>

 <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 gap-3 pt-3 border-t border-[var(--border)] text-xs">
 <div>
 <span className="text-[var(--text-muted)]">Enrollment Number:</span>
 <p className="font-semibold text-[var(--text-primary)] mt-0.5">
 {selectedOption.student.enrollmentNumber || selectedOption.student.enrollment_number || 'N/A'}
 </p>
 </div>
 <div>
 <span className="text-[var(--text-muted)]">Program / Department:</span>
 <p className="font-semibold text-[var(--text-primary)] mt-0.5">
 {selectedOption.student.department || selectedOption.student.program || 'N/A'}
 {selectedOption.student.major ? ` (${selectedOption.student.major})` : ''}
 </p>
 </div>
 <div>
 <span className="text-[var(--text-muted)]">Expected Graduation:</span>
 <p className="font-semibold text-[var(--text-primary)] mt-0.5">
 {selectedOption.student.expectedGraduationDate ||
 selectedOption.student.expected_graduation_date ||
 'Not specified'}
 </p>
 </div>
 <div>
 <span className="text-[var(--text-muted)]">Enrolled Session:</span>
 <p className="font-semibold text-[var(--text-primary)] mt-0.5">
 {selectedOption.student.session || 'N/A'}
 </p>
 </div>
 </div>
 </div>
 )}
 </Card>

 {/* PART 2 — Certificate Details */}
 <Card className="space-y-5">
 <div className="flex items-center gap-2 border-b border-[var(--border)] pb-3">
 <span className="flex h-6 w-6 items-center justify-center rounded-full bg-[var(--brand-light)] text-xs font-bold text-[var(--brand)]">
 2
 </span>
 <h2 className="text-base font-semibold text-[var(--text-primary)]">
 Certificate Details
 </h2>
 </div>

 {/* Auto-filled Academic Program Structure from Enrollment */}
 <div className="rounded-xl border border-[var(--border)] bg-[var(--bg-elevated)]/60 p-4 space-y-3">
 <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 border-b border-[var(--border)]/60 pb-3">
 <div>
 <span className="text-[11px] font-semibold text-[var(--text-muted)] uppercase tracking-wider block">
 Certificate Level
 </span>
 <div className="mt-1">
 {formData.certificateLevel ? (
 <Badge variant="primary" size="md" className="font-semibold text-xs">
 {formData.certificateLevel}
 </Badge>
 ) : (
 <span className="text-xs italic text-[var(--text-muted)]">
 Auto-filled when student is selected
 </span>
 )}
 </div>
 </div>

 <div>
 <span className="text-[11px] font-semibold text-[var(--text-muted)] uppercase tracking-wider block">
 Department
 </span>
 <p className="text-sm font-semibold text-[var(--text-primary)] mt-1">
 {formData.department || (
 <span className="font-normal italic text-[var(--text-muted)] text-xs">
 Auto-filled when student is selected
 </span>
 )}
 </p>
 </div>
 </div>

 <div>
 <span className="text-[11px] font-semibold text-[var(--text-muted)] uppercase tracking-wider block">
 Certificate Name
 </span>
 <p className="text-base font-bold text-[var(--text-primary)] mt-1">
 {formData.certificateName || (
 <span className="text-sm font-normal italic text-[var(--text-muted)]">
 Auto-filled from student's enrollment program
 </span>
 )}
 </p>
 </div>

 <div className="flex items-start gap-2 pt-2.5 border-t border-[var(--border)]/60 text-xs text-[var(--text-secondary)]">
 <Info className="h-4 w-4 shrink-0 text-[var(--brand)] mt-0.5" />
 <span>
 Certificate details are automatically filled from the student's enrollment program. Contact support if details are incorrect.
 </span>
 </div>
 </div>

 <div className="space-y-4 pt-1">
 {/* Session & Major */}
 <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
 <Input
 label="Session"
 placeholder="e.g. Spring 2026"
 value={formData.session}
 onChange={(e) => handleInputChange('session', e.target.value)}
 error={fieldErrors.session}
 required
 />

 <Input
 label="Major (Optional)"
 placeholder="e.g. Software Engineering"
 value={formData.major}
 onChange={(e) => handleInputChange('major', e.target.value)}
 hint="Specialization from enrollment"
 />
 </div>

 {/* CGPA & Degree Class */}
 <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
 <Input
 label="CGPA"
 type="number"
 step="0.01"
 min="0.00"
 max="4.00"
 placeholder="e.g. 3.85"
 value={formData.cgpa}
 onChange={(e) => handleInputChange('cgpa', e.target.value)}
 error={fieldErrors.cgpa}
 hint="Scale of 0.00 to 4.00 (required if Degree Class omitted)"
 />

 <Input
 label="Degree Class"
 placeholder="e.g. First Class, Distinction"
 value={formData.degreeClass}
 onChange={(e) => handleInputChange('degreeClass', e.target.value)}
 error={fieldErrors.degreeClass}
 hint="e.g. First Class (required if CGPA omitted)"
 />
 </div>

 {/* Dates */}
 <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
 <Input
 label="Issue Date"
 type="date"
 value={formData.issueDate}
 onChange={(e) => handleInputChange('issueDate', e.target.value)}
 error={fieldErrors.issueDate}
 required
 />

 <Input
 label="Convocation Date (Optional)"
 type="date"
 value={formData.convocationDate}
 onChange={(e) => handleInputChange('convocationDate', e.target.value)}
 />
 </div>

 {/* Authority Name & Authority Title */}
 <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
 <Input
 label="Authority Name"
 placeholder="e.g. Prof. Dr. John Smith"
 value={formData.authorityName}
 onChange={(e) => handleInputChange('authorityName', e.target.value)}
 error={fieldErrors.authorityName}
 hint="Pre-filled from university profile"
 required
 />

 <Input
 label="Authority Title"
 placeholder="e.g. Vice Chancellor"
 value={formData.authorityTitle}
 onChange={(e) => handleInputChange('authorityTitle', e.target.value)}
 error={fieldErrors.authorityTitle}
 hint="Pre-filled from university profile"
 required
 />
 </div>
 </div>

 {/* Submit Button */}
 <div className="pt-4 border-t border-[var(--border)] flex items-center justify-between">
 <p className="text-xs text-[var(--text-muted)]">
 * All fields are required unless marked optional
 </p>
 <Button
 type="submit"
 loading={submitting}
 disabled={!selectedOption?.student || submitting}
 icon={<Award className="h-4 w-4" />}
 className="px-6"
 >
 Issue Certificate
 </Button>
 </div>
 </Card>
 </form>
 </div>

 {/* Right Column: Live Certificate Preview (4 cols) */}
 <div className="lg:col-span-4">
 <div className="sticky top-6 space-y-4">
 <Card className="p-4">
 <div className="flex items-center justify-between mb-3">
 <span className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">
 Live Certificate Preview
 </span>
 <Badge variant="primary" size="sm">
 Real-time
 </Badge>
 </div>

 {/* Academic Certificate Mock Frame */}
 <div
 className="aspect-[1.414/1] w-full rounded-lg bg-[#faf8f5] p-3 shadow-md border-4 border-[#1e293b] text-center flex flex-col justify-between relative overflow-hidden"
 style={{
 boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1)',
 }}
 >
 {/* Inner Gold Border */}
 <div className="w-full h-full border border-[#d97706]/70 rounded p-2.5 flex flex-col justify-between relative">
 {/* Header Section */}
 <div>
 <p className="text-[9px] uppercase tracking-widest font-serif font-bold text-[#0f172a]">
 {universityProfile?.name || 'Your University'}
 </p>
 <p className="text-[7px] italic font-serif text-[#b45309] mt-0.5">
 Certificate of Academic Achievement
 </p>
 </div>

 {/* Body / Student Section */}
 <div className="my-auto py-1">
 <p className="text-[6px] text-[var(--text-muted)] font-serif">This is to certify that</p>
 <h4 className="text-[11px] font-bold text-[#0f172a] tracking-tight font-serif mt-0.5">
 {selectedOption?.student?.name ||
 selectedOption?.student?.studentName ||
 'Student Full Name'}
 </h4>
 <p className="text-[6.5px] text-[var(--text-secondary)] font-serif leading-tight mt-1 max-w-[90%] mx-auto">
 has completed the prescribed course of study for the degree of
 </p>
 <p className="text-[8px] font-bold text-[#1e3a8a] mt-0.5">
 {formData.certificateName || 'Degree / Certificate Title'}
 </p>
 {formData.major && (
 <p className="text-[6.5px] text-[var(--text-secondary)] italic">Major in {formData.major}</p>
 )}
 </div>

 {/* Details Footer */}
 <div className="space-y-1 text-[6px] text-[var(--text-secondary)] border-t border-gray-200 pt-1">
 <div className="flex justify-between items-center px-1 font-mono">
 <span>
 {formData.cgpa ? (
 <>CGPA: <strong>{formData.cgpa}</strong></>
 ) : formData.degreeClass ? (
 <>Class: <strong>{formData.degreeClass}</strong></>
 ) : (
 <>CGPA: <strong>N/A</strong></>
 )}
 </span>
 <span>
 Session: <strong>{formData.session || 'N/A'}</strong>
 </span>
 </div>
 <div className="flex justify-between items-end px-1 pt-1">
 <div className="text-left">
 <div className="h-5 w-5 bg-gray-200 border border-gray-300 rounded flex items-center justify-center text-[5px] text-[var(--text-muted)] font-mono">
 QR
 </div>
 <span className="text-[5px] text-[var(--text-muted)] font-mono block mt-0.5">
 Serial: BSC-XX-XXXXXX
 </span>
 </div>
 <div className="text-right">
 <div className="w-16 border-t border-gray-400 mb-0.5" />
 <p className="font-bold text-[6px] text-[#0f172a]">
 {formData.authorityName || 'Authority Name'}
 </p>
 <p className="text-[5px] text-[var(--text-muted)]">
 {formData.authorityTitle || 'Authority Title'}
 </p>
 </div>
 </div>
 </div>
 </div>
 </div>

 <div className="mt-3 flex items-start gap-2 rounded-lg bg-[var(--bg-elevated)] p-2.5 text-xs text-[var(--text-secondary)] border border-[var(--border)]">
 <Info className="h-4 w-4 text-[var(--brand)] shrink-0 mt-0.5" />
 <span>
 Generated certificates are cryptographically recorded and verifiable via QR code.
 </span>
 </div>
 </Card>
 </div>
 </div>

 {/* Success Modal Showing Generated Serial Number Prominently */}
 <Modal
 isOpen={successModalOpen}
 onClose={() => setSuccessModalOpen(false)}
 size="md"
 >
 <div className="text-center py-4 space-y-4">
 <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-emerald-500/10 text-emerald-600 ">
 <CheckCircle2 className="h-10 w-10" />
 </div>

 <div>
 <h3 className="text-xl font-bold text-[var(--text-primary)]">
 Certificate issued!
 </h3>
 <p className="text-sm text-[var(--text-secondary)] mt-1">
 Issued for <strong className="text-[var(--text-primary)]">{issuedStudentName}</strong>
 </p>
 </div>

 {/* Prominent Serial Box */}
 <div className="my-3 rounded-xl border border-emerald-500/30 bg-emerald-500/5 p-4 text-center">
 <p className="text-xs uppercase tracking-wider font-semibold text-emerald-600 mb-1">
 Certificate Serial Number
 </p>
 <div className="flex items-center justify-center gap-2">
 <span className="font-mono text-2xl font-extrabold tracking-wider text-[var(--text-primary)]">
 {issuedSerial}
 </span>
 <button
 type="button"
 onClick={copySerialToClipboard}
 className="p-1.5 text-[var(--text-muted)] hover:text-[var(--brand)] rounded-lg hover:bg-[var(--bg-elevated)] transition-colors"
 title="Copy Serial Number"
 >
 <Copy className="h-4 w-4" />
 </button>
 </div>
 </div>

 <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
 <Button
 variant="secondary"
 onClick={() => navigate('/university/certificates')}
 icon={<ExternalLink className="h-4 w-4" />}
 className="w-full sm:w-auto"
 >
 View Certificate
 </Button>
 <Button
 variant="primary"
 onClick={handleResetForm}
 icon={<RefreshCw className="h-4 w-4" />}
 className="w-full sm:w-auto"
 >
 Issue Another
 </Button>
 </div>
 </div>
 </Modal>
 </div>
 );
}

/* =============================================================================
 TAB 2: BATCH UPLOAD
 ============================================================================= */
function BatchUploadTab({
 defaultAuthorityName,
 defaultAuthorityTitle,
 certificateLevels,
}) {
 const todayStr = useMemo(() => new Date().toISOString().split('T')[0], []);

 // Step 2: Common Details State
 const [commonDetails, setCommonDetails] = useState({
 certificateName: '',
 certificateLevel: '',
 department: '',
 major: '',
 session: '',
 issueDate: todayStr,
 convocationDate: '',
 authorityName: '',
 authorityTitle: '',
 });

 // Step 3: File State
 const [selectedFile, setSelectedFile] = useState(null);
 const [uploading, setUploading] = useState(false);
 const [results, setResults] = useState(null);

 // Pre-fill authority defaults
 useEffect(() => {
 setCommonDetails((prev) => ({
 ...prev,
 authorityName: prev.authorityName || defaultAuthorityName,
 authorityTitle: prev.authorityTitle || defaultAuthorityTitle,
 }));
 }, [defaultAuthorityName, defaultAuthorityTitle]);

 const handleCommonChange = (field, value) => {
 setCommonDetails((prev) => ({ ...prev, [field]: value }));
 };

 // Step 1: Download CSV Template
 const handleDownloadTemplate = async () => {
 try {
 const res = await api.get('/university/certificates/batch-template', {
 responseType: 'blob',
 });
 const blob = new Blob([res.data], { type: 'text/csv' });
 const url = window.URL.createObjectURL(blob);
 const link = document.createElement('a');
 link.href = url;
 link.setAttribute('download', 'certificate_batch_template.csv');
 document.body.appendChild(link);
 link.click();
 link.remove();
 toast.success('Template downloaded successfully');
 } catch (err) {
 toast.error('Failed to download CSV template');
 }
 };

 // Step 3: Handle CSV File Selection
 const handleFileChange = (e) => {
 const file = e.target.files?.[0];
 if (file) {
 if (!file.name.endsWith('.csv')) {
 toast.error('Please upload a valid .csv file only');
 return;
 }
 setSelectedFile(file);
 }
 };

 // Step 3: Upload and Issue
 const handleUploadAndIssue = async (e) => {
 e.preventDefault();
 if (!selectedFile) {
 toast.error('Please select a CSV file first');
 return;
 }
 if (!commonDetails.certificateName?.trim()) {
 toast.error('Certificate Name is required in common details');
 return;
 }
 if (!commonDetails.session?.trim()) {
 toast.error('Session is required in common details');
 return;
 }
 if (!commonDetails.issueDate) {
 toast.error('Issue Date is required in common details');
 return;
 }
 if (!commonDetails.authorityName?.trim()) {
 toast.error('Authority Name is required');
 return;
 }
 if (!commonDetails.authorityTitle?.trim()) {
 toast.error('Authority Title is required');
 return;
 }

 setUploading(true);
 const formData = new FormData();
 formData.append('csvFile', selectedFile);
 formData.append('certificateName', commonDetails.certificateName.trim());
 formData.append('certificateLevel', commonDetails.certificateLevel);
 formData.append('session', commonDetails.session.trim());
 formData.append('issueDate', commonDetails.issueDate);
 if (commonDetails.convocationDate) {
 formData.append('convocationDate', commonDetails.convocationDate);
 }
 formData.append('authorityName', commonDetails.authorityName.trim());
 formData.append('authorityTitle', commonDetails.authorityTitle.trim());

 try {
 const res = await api.post('/university/certificates/batch', formData, {
 headers: {
 'Content-Type': 'multipart/form-data',
 },
 });

 const batchResult = res.data?.data || res.data?.results || res.data || {};
 setResults(batchResult);
 toast.success(res.data?.message || 'Batch certificates processed!');
 } catch (err) {
 const msg = err.response?.data?.message || 'Batch processing failed';
 toast.error(msg);
 } finally {
 setUploading(false);
 }
 };

 // Download results as CSV
 const handleDownloadResults = () => {
 if (!results) return;

 let csvContent = 'data:text/csv;charset=utf-8,';
 csvContent += 'Student Email,Status,Serial Number,Reason / Details\n';

 // Successful
 (results.issuedSerials || []).forEach((item) => {
 csvContent += `"${item.studentEmail || ''}","SUCCESS","${item.serial || ''}","Issued to ${item.studentName || ''}"\n`;
 });

 // Failed
 (results.errors || []).forEach((err) => {
 csvContent += `"${err.studentEmail || ''}","FAILED","N/A","Row ${err.rowNumber || ''}: ${err.reason || ''}"\n`;
 });

 const encodedUri = encodeURI(csvContent);
 const link = document.createElement('a');
 link.setAttribute('href', encodedUri);
 link.setAttribute('download', `batch_results_${new Date().toISOString().split('T')[0]}.csv`);
 document.body.appendChild(link);
 link.click();
 link.remove();
 };

 const handleResetBatch = () => {
 setSelectedFile(null);
 setResults(null);
 };

 const totalProcessed = results?.totalProcessed ?? (results?.processed ?? 0);
 const totalSuccessful = results?.successful ?? 0;
 const totalFailed = results?.failed ?? 0;

 return (
 <div className="space-y-6">
 {/* Step 1 Card: Download Template */}
 <Card className="space-y-4">
 <div className="flex items-center gap-2 border-b border-[var(--border)] pb-3">
 <span className="flex h-6 w-6 items-center justify-center rounded-full bg-[var(--brand-light)] text-xs font-bold text-[var(--brand)]">
 1
 </span>
 <h2 className="text-base font-semibold text-[var(--text-primary)]">
 Step 1: Download CSV Template
 </h2>
 </div>

 <p className="text-sm text-[var(--text-secondary)]">
 Fill this template with student emails and their certificate details. One student per row.
 </p>

 {/* Requirements notice */}
 <div className="rounded-xl border border-[var(--brand)]/20 bg-[var(--brand)]/5 p-3.5 space-y-1.5 text-xs text-[var(--text-secondary)]">
 <p className="font-semibold text-[var(--brand)] flex items-center gap-1.5">
 <Info className="h-4 w-4 shrink-0" />
 Batch Upload Validation Rules:
 </p>
 <ul className="list-disc pl-5 space-y-1">
 <li><strong>Student Status:</strong> Students must have an <strong>approved account</strong> and an <strong>active enrollment</strong> at your institution. Withdrawn, suspended, or unapproved students will be rejected.</li>
 <li><strong>Automatic Academic Details:</strong> Certificate Name, Certificate Level, and Department are automatically pulled from each student's enrollment program.</li>
 <li><strong>CGPA &amp; Degree Class:</strong> Either <code>cgpa</code> or <code>degree_class</code> can be empty, but <strong>both cannot be empty</strong> (at least one must be provided per student).</li>
 </ul>
 </div>

 {/* Expected Columns Table */}
 <div className="overflow-x-auto rounded-lg border border-[var(--border)]">
 <table className="w-full text-left text-xs">
 <thead className="bg-[var(--bg-elevated)] border-b border-[var(--border)] text-[var(--text-secondary)]">
 <tr>
 <th className="px-3 py-2 font-medium">student_email</th>
 <th className="px-3 py-2 font-medium">cgpa</th>
 <th className="px-3 py-2 font-medium">degree_class</th>
 </tr>
 </thead>
 <tbody className="divide-y divide-[var(--border)] text-[var(--text-primary)]">
 <tr>
 <td className="px-3 py-2 font-mono text-[var(--text-secondary)]">student1@example.com</td>
 <td className="px-3 py-2 font-mono text-[var(--text-secondary)]">3.75</td>
 <td className="px-3 py-2 text-[var(--text-secondary)]">First Class</td>
 </tr>
 <tr>
 <td className="px-3 py-2 font-mono text-[var(--text-secondary)]">student2@example.com</td>
 <td className="px-3 py-2 font-mono text-[var(--text-secondary)]">3.85</td>
 <td className="px-3 py-2 text-[var(--text-muted)] italic font-mono">&lt;empty&gt;</td>
 </tr>
 <tr>
 <td className="px-3 py-2 font-mono text-[var(--text-secondary)]">student3@example.com</td>
 <td className="px-3 py-2 text-[var(--text-muted)] italic font-mono">&lt;empty&gt;</td>
 <td className="px-3 py-2 text-[var(--text-secondary)]">First Class</td>
 </tr>
 </tbody>
 </table>
 </div>

 <div>
 <Button
 variant="secondary"
 onClick={handleDownloadTemplate}
 icon={<Download className="h-4 w-4" />}
 >
 Download Template
 </Button>
 </div>
 </Card>

 {/* Step 2 Card: Fill Common Details */}
 <Card className="space-y-4">
 <div className="flex items-center gap-2 border-b border-[var(--border)] pb-3">
 <span className="flex h-6 w-6 items-center justify-center rounded-full bg-[var(--brand-light)] text-xs font-bold text-[var(--brand)]">
 2
 </span>
 <h2 className="text-base font-semibold text-[var(--text-primary)]">
 Step 2: Fill Common Details
 </h2>
 </div>

 <p className="text-sm text-[var(--text-secondary)]">
 These details apply to ALL students in the CSV batch:
 </p>

 <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
 <Input
 label="Certificate Name"
 placeholder="Bachelor of Science in Computer Science and Engineering"
 value={commonDetails.certificateName}
 onChange={(e) => handleCommonChange('certificateName', e.target.value)}
 required
 />

 <div>
 <label className="block text-sm font-medium text-[var(--text-secondary)] mb-1">
 Certificate Level <span className="text-[var(--danger)]">*</span>
 </label>
 <select
 value={commonDetails.certificateLevel}
 onChange={(e) => handleCommonChange('certificateLevel', e.target.value)}
 className="w-full h-[40px] px-3 rounded-[8px] border border-[var(--border)] bg-[var(--bg-surface)] text-sm text-[var(--text-primary)] outline-none focus:border-[var(--brand)] focus:shadow-[0_0_0_3px_rgba(99,102,241,0.12)] transition-all"
 >
 <option value="">Select Level</option>
 {certificateLevels?.map((lvl) => (
 <option key={lvl.value} value={lvl.value}>
 {lvl.label}
 </option>
 ))}
 </select>
 </div>

 <Input
 label="Session"
 placeholder="e.g. Spring 2026"
 value={commonDetails.session}
 onChange={(e) => handleCommonChange('session', e.target.value)}
 required
 />

 <Input
 label="Issue Date"
 type="date"
 value={commonDetails.issueDate}
 onChange={(e) => handleCommonChange('issueDate', e.target.value)}
 required
 />

 <Input
 label="Convocation Date (Optional)"
 type="date"
 value={commonDetails.convocationDate}
 onChange={(e) => handleCommonChange('convocationDate', e.target.value)}
 />

 <Input
 label="Authority Name"
 placeholder="e.g. Prof. Dr. John Smith"
 value={commonDetails.authorityName}
 onChange={(e) => handleCommonChange('authorityName', e.target.value)}
 hint="Pre-filled from university profile"
 required
 />

 <Input
 label="Authority Title"
 placeholder="e.g. Vice Chancellor"
 value={commonDetails.authorityTitle}
 onChange={(e) => handleCommonChange('authorityTitle', e.target.value)}
 hint="Pre-filled from university profile"
 required
 />
 </div>
 </Card>

 {/* Step 3 Card: Upload CSV */}
 <Card className="space-y-4">
 <div className="flex items-center gap-2 border-b border-[var(--border)] pb-3">
 <span className="flex h-6 w-6 items-center justify-center rounded-full bg-[var(--brand-light)] text-xs font-bold text-[var(--brand)]">
 3
 </span>
 <h2 className="text-base font-semibold text-[var(--text-primary)]">
 Step 3: Upload CSV
 </h2>
 </div>

 <div className="space-y-4">
 <div className="flex flex-col items-center justify-center rounded-xl border-2 border-dashed border-[var(--border)] p-6 bg-[var(--bg-elevated)] hover:bg-[var(--bg-surface)] transition-all cursor-pointer relative">
 <input
 type="file"
 accept=".csv"
 onChange={handleFileChange}
 className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
 />
 <UploadCloud className="h-10 w-10 text-[var(--text-muted)] mb-2" />
 <p className="text-sm font-semibold text-[var(--text-primary)]">
 {selectedFile ? selectedFile.name : 'Click or drag CSV file to upload'}
 </p>
 <p className="text-xs text-[var(--text-muted)] mt-1">Accepts .csv only</p>
 {selectedFile && (
 <Badge variant="primary" size="sm" className="mt-3">
 Selected: {selectedFile.name} ({(selectedFile.size / 1024).toFixed(1)} KB)
 </Badge>
 )}
 </div>

 <div className="flex items-center justify-between pt-2">
 <p className="text-xs text-[var(--text-muted)]">
 Ensure student emails in CSV match existing enrolled students.
 </p>
 <Button
 variant="primary"
 onClick={handleUploadAndIssue}
 disabled={
 !selectedFile ||
 uploading ||
 !commonDetails.certificateName ||
 !commonDetails.session ||
 !commonDetails.issueDate
 }
 loading={uploading}
 icon={<UploadCloud className="h-4 w-4" />}
 >
 {uploading ? 'Processing certificates...' : 'Upload and Issue'}
 </Button>
 </div>
 </div>
 </Card>

 {/* Results Display */}
 {results && (
 <Card className="space-y-5 animate-fadeIn">
 <div className="flex items-center justify-between border-b border-[var(--border)] pb-3">
 <h3 className="text-base font-bold text-[var(--text-primary)]">
 Batch Issuance Results
 </h3>
 <div className="flex items-center gap-2">
 <Button
 variant="secondary"
 size="sm"
 onClick={handleDownloadResults}
 icon={<Download className="h-3.5 w-3.5" />}
 >
 Download Results
 </Button>
 <Button
 variant="ghost"
 size="sm"
 onClick={handleResetBatch}
 icon={<RefreshCw className="h-3.5 w-3.5" />}
 >
 Issue More
 </Button>
 </div>
 </div>

 {/* Summary Row */}
 <div className="flex flex-wrap items-center gap-4 text-sm font-semibold p-3 rounded-lg bg-[var(--bg-elevated)] border border-[var(--border)]">
 <span className="text-emerald-600 flex items-center gap-1.5">
 <CheckCircle2 className="h-4 w-4" />
 {totalSuccessful} successful
 </span>
 <span className="text-[var(--text-muted)]">|</span>
 <span className="text-rose-600 flex items-center gap-1.5">
 <XCircle className="h-4 w-4" />
 {totalFailed} failed
 </span>
 </div>

 {/* If All Successful */}
 {totalFailed === 0 && totalSuccessful > 0 && (
 <div className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 p-4">
 <div className="flex items-center gap-2 text-emerald-700 font-bold mb-3">
 <CheckCircle2 className="h-5 w-5" />
 All certificates issued successfully!
 </div>
 <div className="divide-y divide-emerald-500/20 max-h-60 overflow-y-auto">
 {(results.issuedSerials || []).map((s, idx) => (
 <div key={idx} className="py-2 flex items-center justify-between text-xs">
 <span className="font-mono font-bold text-emerald-800 ">
 {s.serial}
 </span>
 <span className="text-emerald-700 ">
 {s.studentName} ({s.studentEmail})
 </span>
 </div>
 ))}
 </div>
 </div>
 )}

 {/* If Some Failed */}
 {totalFailed > 0 && (
 <div className="space-y-4">
 <div className="space-y-2">
 <h4 className="text-sm font-semibold text-rose-600 flex items-center gap-1.5">
 <AlertCircle className="h-4 w-4" />
 Failed Rows:
 </h4>
 <div className="overflow-x-auto rounded-lg border border-rose-200 /40 bg-rose-50/50 /20">
 <table className="w-full text-left text-xs">
 <thead className="border-b border-rose-200 /40 text-[var(--text-secondary)] bg-rose-100/40 /20">
 <tr>
 <th className="px-3 py-2 font-medium">Row number</th>
 <th className="px-3 py-2 font-medium">Student Email</th>
 <th className="px-3 py-2 font-medium">Reason for failure</th>
 </tr>
 </thead>
 <tbody className="divide-y divide-rose-200 dark:divide-rose-900/40 text-[var(--text-primary)]">
 {(results.errors || []).map((err, idx) => (
 <tr key={idx} className="hover:bg-rose-100/20">
 <td className="px-3 py-2 font-mono font-bold">
 Row {err.rowNumber || err.row || idx + 1}
 </td>
 <td className="px-3 py-2 font-mono text-[var(--text-secondary)]">
 {err.studentEmail || err.student_email || 'N/A'}
 </td>
 <td className="px-3 py-2 text-rose-600 ">
 {err.reason || err.error || 'Validation error'}
 </td>
 </tr>
 ))}
 </tbody>
 </table>
 </div>
 </div>

 {/* Issued serials shown separately below */}
 {totalSuccessful > 0 && (
 <div className="space-y-2 pt-2">
 <h4 className="text-sm font-semibold text-emerald-600 flex items-center gap-1.5">
 <CheckCircle2 className="h-4 w-4" />
 Issued Serials ({totalSuccessful}):
 </h4>
 <div className="overflow-x-auto rounded-lg border border-[var(--border)] max-h-56 divide-y divide-[var(--border)]">
 {(results.issuedSerials || []).map((s, idx) => (
 <div
 key={idx}
 className="px-3 py-2 flex items-center justify-between text-xs bg-[var(--bg-elevated)]"
 >
 <span className="font-mono font-bold text-[var(--brand)]">
 {s.serial}
 </span>
 <span className="text-[var(--text-secondary)]">
 {s.studentName} ({s.studentEmail})
 </span>
 </div>
 ))}
 </div>
 </div>
 )}
 </div>
 )}
 </Card>
 )}
 </div>
 );
}
