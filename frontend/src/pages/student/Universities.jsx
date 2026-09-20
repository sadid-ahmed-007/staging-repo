import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { Search, MapPin, Building2, CalendarDays, ShieldCheck, CheckCircle2, XCircle, Clock, X } from 'lucide-react';
import toast from 'react-hot-toast';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/shared/Card';
import Button from '../../components/shared/Button';
import Modal from '../../components/shared/Modal';
import api from '../../services/api';

export default function Universities() {
 const [universities, setUniversities] = useState([]);
 const [applications, setApplications] = useState([]);
 const [searchQuery, setSearchQuery] = useState('');
 const [debouncedSearch, setDebouncedSearch] = useState('');
 const [loading, setLoading] = useState(true);
 const [selectedUni, setSelectedUni] = useState(null);

 // Debounce search
 useEffect(() => {
 const handler = setTimeout(() => {
 setDebouncedSearch(searchQuery);
 }, 300);
 return () => clearTimeout(handler);
 }, [searchQuery]);

 const fetchData = useCallback(async () => {
 setLoading(true);
 try {
 const [uniRes, appRes] = await Promise.all([
 api.get('/public/universities', { params: { search: debouncedSearch } }),
 api.get('/student/applications', { params: { size: 100 } })
 ]);
 setUniversities(uniRes.data.data?.data || []);
 setApplications(appRes.data?.data || []);
 } catch (err) {
 toast.error('Failed to fetch universities');
 console.error(err);
 } finally {
 setLoading(false);
 }
 }, [debouncedSearch]);

 useEffect(() => {
 fetchData();
 }, [fetchData]);

 // Map of universityId -> application
 const appMap = useMemo(() => {
 const map = {};
 applications.forEach(app => {
 // Only keep the most recent application status (applications should be sorted by appliedAt desc from backend)
 if (!map[app.universityId]) {
 map[app.universityId] = app;
 }
 });
 return map;
 }, [applications]);

 return (
 <DashboardLayout>
 <div className="space-y-6">
 <div>
 <h1 className="text-2xl font-bold text-[var(--text-primary)] sm:text-3xl">Apply to a University</h1>
 <p className="mt-1 text-sm text-[var(--text-muted)] dark:text-[var(--text-muted)]">
 Browse verified universities and submit your application for enrollment.
 </p>
 </div>

 {/* Search */}
 <Card>
 <div className="relative">
 <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3">
 <Search className="h-5 w-5 text-[var(--text-muted)]" />
 </div>
 <input
 type="text"
 className="block w-full rounded-xl border-gray-300 pl-10 focus:border-primary-500 focus:ring-primary-500 sm:text-sm h-11"
 placeholder="Search by university name..."
 value={searchQuery}
 onChange={(e) => setSearchQuery(e.target.value)}
 />
 </div>
 </Card>

 {loading ? (
 <div className="flex justify-center items-center py-12">
 <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
 </div>
 ) : universities.length === 0 ? (
 <div className="text-center py-12 bg-[var(--bg-surface)] rounded-2xl border border-gray-200 ">
 <h3 className="text-lg font-medium text-[var(--text-primary)] mb-2">No universities found.</h3>
 <p className="text-[var(--text-muted)] dark:text-[var(--text-muted)]">Try adjusting your search criteria.</p>
 </div>
 ) : (
 <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
 {universities?.map(uni => {
 const existingApp = appMap[uni.id];
 return (
 <Card key={uni.id} className="flex flex-col h-full hover:shadow-lg transition-shadow">
 <div className="flex items-start justify-between mb-4">
 <div className="h-12 w-12 rounded-xl bg-primary-50 /20 flex items-center justify-center shrink-0">
 <Building2 className="h-6 w-6 text-primary-600 " />
 </div>
 {existingApp && existingApp.status !== 'cancelled' && (
 <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium capitalize
 ${existingApp.status === 'pending' ? 'bg-yellow-100 text-yellow-800 /30 ' :
 existingApp.status === 'accepted' ? 'bg-green-100 text-green-800 /30 ' :
 'bg-red-100 text-red-800 /30 '}`}>
 {existingApp.status}
 </span>
 )}
 </div>
 
 <h3 className="text-lg font-bold text-[var(--text-primary)] mb-2">{uni.name}</h3>
 <div className="flex items-center gap-2 text-sm text-[var(--text-muted)] dark:text-[var(--text-muted)] mb-4">
 <MapPin className="h-4 w-4" /> {uni.city}
 </div>
 
 <div className="mb-4">
 <p className="text-sm text-[var(--text-secondary)] font-medium mb-2">
 {uni.programCount} Available Programs
 </p>
 <div className="flex flex-wrap gap-2">
 {uni.levels?.map(lvl => (
 <span key={lvl.id} className="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-gray-100 text-[var(--text-primary)] ">
 {lvl.shortName}
 </span>
 ))}
 </div>
 </div>

 <div className="mt-auto pt-4 border-t border-gray-100 ">
 <Button 
 className="w-full" 
 onClick={() => setSelectedUni(uni)}
 variant={existingApp && existingApp.status !== 'cancelled' ? 'secondary' : 'primary'}
 >
 {existingApp && existingApp.status !== 'cancelled' ? 'View Application' : 'View & Apply'}
 </Button>
 </div>
 </Card>
 );
 })}
 </div>
 )}

 {selectedUni && (
 <ApplicationPanel 
 university={selectedUni} 
 existingApp={appMap[selectedUni.id]} 
 onClose={() => setSelectedUni(null)} 
 onSuccess={() => {
 setSelectedUni(null);
 fetchData();
 }}
 />
 )}
 </div>
 </DashboardLayout>
 );
}

// Application Panel Component
function ApplicationPanel({ university, existingApp, onClose, onSuccess }) {
 const [certLevelId, setCertLevelId] = useState('');
 const [departmentId, setDepartmentId] = useState('');
 const [programId, setProgramId] = useState('');
 const [personalStatement, setPersonalStatement] = useState('');
 const [submitting, setSubmitting] = useState(false);

 // Filter available options
 const availableDepartments = university.levels?.find(l => l.id == certLevelId)?.departments || [];
 const availablePrograms = availableDepartments.find(d => d.id == departmentId)?.programs || [];

 const handleCancelApplication = async () => {
 if (!window.confirm("Are you sure? You can re-apply later.")) return;
 setSubmitting(true);
 try {
 await api.delete(`/student/applications/${existingApp.id}`);
 toast.success("Application cancelled");
 onSuccess();
 } catch (err) {
 toast.error(err.response?.data?.message || "Failed to cancel");
 } finally {
 setSubmitting(false);
 }
 };

 const handleSubmit = async (e) => {
 e.preventDefault();
 setSubmitting(true);
 try {
 const payload = {
 university_id: university.id,
 certificate_level_id: certLevelId || null,
 department_id: departmentId || null,
 program_id: programId || null,
 personal_statement: personalStatement
 };
 await api.post('/student/applications', payload);
 toast.success("Application submitted successfully!");
 onSuccess();
 } catch (err) {
 toast.error(err.response?.data?.message || "Failed to submit application");
 } finally {
 setSubmitting(false);
 }
 };

 return (
 <Modal open={true} onClose={onClose} title={university.name} size="xl">
 <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
 {/* Left: University Info */}
 <div className="space-y-6">
 <div>
 <h4 className="text-lg font-medium text-[var(--text-primary)] mb-2">About</h4>
 <div className="flex items-center gap-2 text-sm text-[var(--text-muted)] mb-2">
 <MapPin className="h-4 w-4" /> {university.city}, {university.address}
 </div>
 {university.website && (
 <a href={university.website.startsWith('http') ? university.website : `https://${university.website}`} target="_blank" rel="noreferrer" className="text-sm text-primary-600 hover:underline block mb-2">
 Visit Website
 </a>
 )}
 </div>

 <div>
 <h4 className="text-lg font-medium text-[var(--text-primary)] mb-4">Available Programs</h4>
 <div className="space-y-4 max-h-[400px] overflow-y-auto pr-2 custom-scrollbar">
 {university.levels?.map(lvl => (
 <div key={lvl.id} className="bg-gray-50 /50 p-4 rounded-lg">
 <h5 className="font-medium text-[var(--text-primary)] mb-2">{lvl.name} ({lvl.shortName})</h5>
 {lvl.departments?.length > 0 ? (
 <div className="space-y-3">
 {lvl.departments?.map(dept => (
 <div key={dept.id} className="pl-3 border-l-2 border-primary-200 ">
 <h6 className="text-sm font-medium text-[var(--text-secondary)] mb-1">{dept.name}</h6>
 <ul className="list-disc list-inside text-sm text-[var(--text-muted)] dark:text-[var(--text-muted)] space-y-1 pl-2">
 {dept.programs?.map(prog => (
 <li key={prog.id}>{prog.name}</li>
 ))}
 </ul>
 </div>
 ))}
 </div>
 ) : (
 <p className="text-sm text-[var(--text-muted)]">No departments found.</p>
 )}
 </div>
 ))}
 </div>
 </div>
 </div>

 {/* Right: Application Form or Status */}
 <div className="bg-gray-50 rounded-xl p-6 border border-gray-200 ">
 <h4 className="text-lg font-medium text-[var(--text-primary)] mb-6">Application</h4>
 
 {existingApp && existingApp.status !== 'cancelled' ? (
 <div className="space-y-6">
 {existingApp.status === 'pending' && (
 <div className="text-center p-6 bg-yellow-50 /20 rounded-xl border border-yellow-200 /50">
 <Clock className="mx-auto h-12 w-12 text-yellow-500 mb-3" />
 <h5 className="text-lg font-semibold text-yellow-800 ">Application Pending</h5>
 <p className="text-sm text-yellow-700 mt-2">
 Your application is currently under review by the university.
 </p>
 <Button variant="danger" className="mt-6" onClick={handleCancelApplication} disabled={submitting}>
 Cancel Application
 </Button>
 </div>
 )}
 {existingApp.status === 'accepted' && (
 <div className="p-6 bg-green-50 /20 rounded-xl border border-green-200 /50">
 <CheckCircle2 className="h-10 w-10 text-green-500 mb-3" />
 <h5 className="text-lg font-semibold text-green-800 ">Application Accepted ✓</h5>
 {existingApp.acceptanceMessage && (
 <div className="mt-4 p-4 bg-[var(--bg-surface)] rounded-lg text-sm text-[var(--text-secondary)] ">
 <strong>Message from university:</strong><br />
 {existingApp.acceptanceMessage}
 </div>
 )}
 </div>
 )}
 {existingApp.status === 'rejected' && (
 <div className="p-6 bg-red-50 /20 rounded-xl border border-red-200 /50">
 <XCircle className="h-10 w-10 text-red-500 mb-3" />
 <h5 className="text-lg font-semibold text-red-800 ">Application Not Accepted</h5>
 {existingApp.rejectionReason && (
 <div className="mt-4 p-4 bg-[var(--bg-surface)] rounded-lg text-sm text-[var(--text-secondary)] ">
 <strong>Reason:</strong><br />
 {existingApp.rejectionReason}
 </div>
 )}
 </div>
 )}
 </div>
 ) : (
 <form onSubmit={handleSubmit} className="space-y-4">
 {existingApp?.status === 'cancelled' && (
 <div className="mb-4 p-3 bg-gray-100 rounded-lg text-sm text-[var(--text-secondary)] dark:text-[var(--text-muted)]">
 You previously cancelled an application to this university. You may apply again below.
 </div>
 )}
 
 <div>
 <label className="block text-sm font-medium text-[var(--text-secondary)] mb-1">
 Certificate Level
 </label>
 <select
 value={certLevelId}
 onChange={e => {
 setCertLevelId(e.target.value);
 setDepartmentId('');
 setProgramId('');
 }}
 className="w-full rounded-lg border-gray-300 bg-[var(--bg-surface)] px-4 py-2 text-sm focus:border-primary-500 focus:ring-primary-500 "
 >
 <option value="">Select level...</option>
 {university.levels?.map(l => (
 <option key={l.id} value={l.id}>{l.name}</option>
 ))}
 </select>
 </div>

 <div>
 <label className="block text-sm font-medium text-[var(--text-secondary)] mb-1">
 Department
 </label>
 <select
 value={departmentId}
 onChange={e => {
 setDepartmentId(e.target.value);
 setProgramId('');
 }}
 disabled={!certLevelId}
 className="w-full rounded-lg border-gray-300 bg-[var(--bg-surface)] px-4 py-2 text-sm focus:border-primary-500 focus:ring-primary-500 disabled:opacity-50"
 >
 <option value="">Select department...</option>
 {availableDepartments?.map(d => (
 <option key={d.id} value={d.id}>{d.name}</option>
 ))}
 </select>
 </div>

 <div>
 <label className="block text-sm font-medium text-[var(--text-secondary)] mb-1">
 Program
 </label>
 <select
 value={programId}
 onChange={e => setProgramId(e.target.value)}
 disabled={!departmentId}
 className="w-full rounded-lg border-gray-300 bg-[var(--bg-surface)] px-4 py-2 text-sm focus:border-primary-500 focus:ring-primary-500 disabled:opacity-50"
 >
 <option value="">Select program...</option>
 {availablePrograms?.map(p => (
 <option key={p.id} value={p.id}>{p.name}</option>
 ))}
 </select>
 </div>

 <div>
 <label className="block text-sm font-medium text-[var(--text-secondary)] mb-1">
 Personal Statement <span className="text-[var(--text-muted)] font-normal">(Optional)</span>
 </label>
 <textarea
 value={personalStatement}
 onChange={e => setPersonalStatement(e.target.value)}
 placeholder="Tell the university why you want to join this program..."
 className="w-full rounded-lg border-gray-300 bg-[var(--bg-surface)] px-4 py-2 text-sm focus:border-primary-500 focus:ring-primary-500 min-h-[120px]"
 maxLength={1000}
 />
 <div className="text-right text-xs text-[var(--text-muted)] mt-1">
 {personalStatement.length}/1000
 </div>
 </div>

 <div className="pt-4">
 <Button type="submit" loading={submitting} className="w-full">
 Submit Application
 </Button>
 </div>
 </form>
 )}
 </div>
 </div>
 </Modal>
 );
}
