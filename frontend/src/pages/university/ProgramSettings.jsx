import { useState, useEffect, useMemo } from 'react';
import toast from 'react-hot-toast';
import {
 Layers,
 GraduationCap,
 Building2,
 BookOpen,
 Plus,
 Pencil,
 CheckCircle2,
 AlertCircle,
 Clock,
 Hash,
 ChevronRight,
 Info,
 RefreshCw,
 Search,
 Sparkles,
} from 'lucide-react';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/shared/Card';
import Button from '../../components/shared/Button';
import Input from '../../components/shared/Input';
import Badge from '../../components/shared/Badge';
import Modal from '../../components/shared/Modal';
import ToggleSwitch from '../../components/shared/ToggleSwitch';
import LoadingSpinner from '../../components/shared/LoadingSpinner';
import EmptyState from '../../components/shared/EmptyState';
import api from '../../services/api';
import { cn } from '../../utils/helpers';

export default function ProgramSettings() {
 const [loading, setLoading] = useState(true);
 const [structure, setStructure] = useState([]);
 const [selectedLevelId, setSelectedLevelId] = useState(null);
 const [selectedDeptId, setSelectedDeptId] = useState(null);

 // Modals state
 const [levelModal, setLevelModal] = useState({ open: false, isEdit: false, data: null });
 const [deptModal, setDeptModal] = useState({ open: false, isEdit: false, data: null });
 const [programModal, setProgramModal] = useState({ open: false, isEdit: false, data: null });

 // Form states
 const [levelForm, setLevelForm] = useState({ name: '', shortName: '', serialPrefix: '', durationYears: 4 });
 const [deptForm, setDeptForm] = useState({ name: '', code: '' });
 const [programForm, setProgramForm] = useState({ name: '', shortName: '' });
 const [submitting, setSubmitting] = useState(false);

 // Load structure
 const fetchStructure = async (keepSelection = true) => {
 try {
 setLoading(true);
 const res = await api.get('/university/program-structure', {
 params: { includeInactive: true },
 });
 if (res.data?.success) {
 const levels = res.data.data?.certificateLevels || [];
 setStructure(levels);

 // Auto-select first level if none selected or if previous selection gone
 if (!keepSelection || !selectedLevelId) {
 if (levels.length > 0) {
 setSelectedLevelId(levels[0].id);
 if (levels[0].departments?.length > 0) {
 setSelectedDeptId(levels[0].departments[0].id);
 } else {
 setSelectedDeptId(null);
 }
 } else {
 setSelectedLevelId(null);
 setSelectedDeptId(null);
 }
 }
 }
 } catch (err) {
 console.error('Failed to load program structure', err);
 toast.error(err.response?.data?.message || 'Failed to load academic program structure');
 } finally {
 setLoading(false);
 }
 };

 useEffect(() => {
 fetchStructure(false);
 }, []);

 // Selected entities derived from structure
 const selectedLevel = useMemo(() => {
 return structure.find((lvl) => lvl.id === selectedLevelId) || null;
 }, [structure, selectedLevelId]);

 const departmentsList = useMemo(() => {
 return selectedLevel?.departments || [];
 }, [selectedLevel]);

 const selectedDept = useMemo(() => {
 return departmentsList.find((d) => d.id === selectedDeptId) || null;
 }, [departmentsList, selectedDeptId]);

 const programsList = useMemo(() => {
 return selectedDept?.programs || [];
 }, [selectedDept]);

 // Keep selectedDept valid when level changes
 useEffect(() => {
 if (selectedLevel) {
 const depts = selectedLevel.departments || [];
 if (!depts.some((d) => d.id === selectedDeptId)) {
 setSelectedDeptId(depts.length > 0 ? depts[0].id : null);
 }
 } else {
 setSelectedDeptId(null);
 }
 }, [selectedLevelId, structure]);

 // ---------------------------------------------------------------------------
 // TOGGLE ACTIVE HANDLERS (PATCH)
 // ---------------------------------------------------------------------------
 const handleToggleLevelActive = async (level) => {
 const nextState = !level.isActive;
 try {
 await api.patch(`/university/certificate-levels/${level.id}`, {
 isActive: nextState,
 });
 toast.success(
 nextState
 ? `Certificate level "${level.shortName || level.name}" activated`
 : `Certificate level "${level.shortName || level.name}" deactivated`
 );
 fetchStructure(true);
 } catch (err) {
 console.error('Failed to toggle level active', err);
 toast.error(err.response?.data?.message || 'Failed to update certificate level status');
 }
 };

 const handleToggleDeptActive = async (dept) => {
 const nextState = !dept.isActive;
 try {
 await api.patch(`/university/departments/${dept.id}`, {
 isActive: nextState,
 });
 toast.success(
 nextState
 ? `Department "${dept.code || dept.name}" activated`
 : `Department "${dept.code || dept.name}" deactivated`
 );
 fetchStructure(true);
 } catch (err) {
 console.error('Failed to toggle department active', err);
 toast.error(err.response?.data?.message || 'Failed to update department status');
 }
 };

 const handleToggleProgramActive = async (prog) => {
 const nextState = !prog.isActive;
 try {
 await api.patch(`/university/programs/${prog.id}`, {
 isActive: nextState,
 });
 toast.success(
 nextState
 ? `Program "${prog.shortName || prog.name}" activated`
 : `Program "${prog.shortName || prog.name}" deactivated`
 );
 fetchStructure(true);
 } catch (err) {
 console.error('Failed to toggle program active', err);
 toast.error(err.response?.data?.message || 'Failed to update program status');
 }
 };

 // ---------------------------------------------------------------------------
 // CERTIFICATE LEVEL MODAL
 // ---------------------------------------------------------------------------
 const openAddLevelModal = () => {
 setLevelForm({ name: '', shortName: '', serialPrefix: '', durationYears: 4 });
 setLevelModal({ open: true, isEdit: false, data: null });
 };

 const openEditLevelModal = (level, e) => {
 e?.stopPropagation();
 setLevelForm({
 name: level.name || '',
 shortName: level.shortName || '',
 serialPrefix: level.serialPrefix || '',
 durationYears: level.durationYears || 4,
 });
 setLevelModal({ open: true, isEdit: true, data: level });
 };

 const handleSubmitLevel = async (e) => {
 e.preventDefault();
 if (!levelForm.name.trim() || !levelForm.shortName.trim() || !levelForm.serialPrefix.trim()) {
 toast.error('Please complete all required fields.');
 return;
 }
 setSubmitting(true);
 try {
 const payload = {
 name: levelForm.name.trim(),
 shortName: levelForm.shortName.trim(),
 serialPrefix: levelForm.serialPrefix.trim().toUpperCase(),
 durationYears: parseInt(levelForm.durationYears, 10) || 4,
 };

 if (levelModal.isEdit) {
 await api.put(`/university/certificate-levels/${levelModal.data.id}`, payload);
 toast.success('Certificate level updated successfully');
 } else {
 const res = await api.post('/university/certificate-levels', payload);
 toast.success('Certificate level created successfully');
 if (res.data?.data?.id) {
 setSelectedLevelId(res.data.data.id);
 }
 }
 setLevelModal({ open: false, isEdit: false, data: null });
 fetchStructure(true);
 } catch (err) {
 console.error('Failed to save certificate level', err);
 toast.error(err.response?.data?.message || 'Failed to save certificate level');
 } finally {
 setSubmitting(false);
 }
 };

 // ---------------------------------------------------------------------------
 // DEPARTMENT MODAL
 // ---------------------------------------------------------------------------
 const openAddDeptModal = () => {
 if (!selectedLevelId) {
 toast.error('Please select a certificate level first');
 return;
 }
 setDeptForm({ name: '', code: '' });
 setDeptModal({ open: true, isEdit: false, data: null });
 };

 const openEditDeptModal = (dept, e) => {
 e?.stopPropagation();
 setDeptForm({
 name: dept.name || '',
 code: dept.code || '',
 });
 setDeptModal({ open: true, isEdit: true, data: dept });
 };

 const handleSubmitDept = async (e) => {
 e.preventDefault();
 if (!deptForm.name.trim() || !deptForm.code.trim()) {
 toast.error('Please complete all required fields.');
 return;
 }
 setSubmitting(true);
 try {
 const payload = {
 certificateLevelId: selectedLevelId,
 name: deptForm.name.trim(),
 code: deptForm.code.trim().toUpperCase(),
 };

 if (deptModal.isEdit) {
 await api.put(`/university/departments/${deptModal.data.id}`, {
 name: payload.name,
 code: payload.code,
 });
 toast.success('Department updated successfully');
 } else {
 const res = await api.post('/university/departments', payload);
 toast.success('Department created successfully');
 if (res.data?.data?.id) {
 setSelectedDeptId(res.data.data.id);
 }
 }
 setDeptModal({ open: false, isEdit: false, data: null });
 fetchStructure(true);
 } catch (err) {
 console.error('Failed to save department', err);
 toast.error(err.response?.data?.message || 'Failed to save department');
 } finally {
 setSubmitting(false);
 }
 };

 // ---------------------------------------------------------------------------
 // PROGRAM MODAL
 // ---------------------------------------------------------------------------
 const openAddProgramModal = () => {
 if (!selectedDeptId) {
 toast.error('Please select a department first');
 return;
 }
 setProgramForm({ name: '', shortName: '' });
 setProgramModal({ open: true, isEdit: false, data: null });
 };

 const openEditProgramModal = (prog, e) => {
 e?.stopPropagation();
 setProgramForm({
 name: prog.name || '',
 shortName: prog.shortName || '',
 });
 setProgramModal({ open: true, isEdit: true, data: prog });
 };

 const handleSubmitProgram = async (e) => {
 e.preventDefault();
 if (!programForm.name.trim()) {
 toast.error('Full program name is required.');
 return;
 }
 setSubmitting(true);
 try {
 const payload = {
 departmentId: selectedDeptId,
 name: programForm.name.trim(),
 shortName: programForm.shortName.trim() || undefined,
 };

 if (programModal.isEdit) {
 await api.put(`/university/programs/${programModal.data.id}`, {
 name: payload.name,
 shortName: payload.shortName,
 });
 toast.success('Program updated successfully');
 } else {
 await api.post('/university/programs', payload);
 toast.success('Program created successfully');
 }
 setProgramModal({ open: false, isEdit: false, data: null });
 fetchStructure(true);
 } catch (err) {
 console.error('Failed to save program', err);
 toast.error(err.response?.data?.message || 'Failed to save program');
 } finally {
 setSubmitting(false);
 }
 };

 return (
 <DashboardLayout>
 <div className="space-y-6 pb-12">
 {/* Page Header */}
 <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
 <div>
 <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-[var(--brand)]">
 <Layers className="h-4 w-4" />
 <span>Academic Settings</span>
 </div>
 <h1 className="mt-1 text-2xl sm:text-3xl font-bold text-[var(--text-primary)]">
 Academic Program Structure
 </h1>
 <p className="mt-1 text-sm text-[var(--text-secondary)]">
 Configure degree certificate levels, associated departments, and academic programs.
 </p>
 </div>

 <Button
 variant="secondary"
 size="sm"
 onClick={() => fetchStructure(true)}
 loading={loading}
 icon={<RefreshCw className="h-4 w-4" />}
 >
 Refresh
 </Button>
 </div>

 {/* Informative Guidance Banner */}
 <div className="flex items-start gap-3 rounded-xl border border-[var(--brand)]/20 bg-[var(--brand-light)]/20 p-4 text-xs text-[var(--text-secondary)]">
 <Info className="h-5 w-5 text-[var(--brand)] shrink-0 mt-0.5" />
 <div className="space-y-1">
 <p className="font-semibold text-[var(--text-primary)]">
 Hierarchy: Certificate Level &rarr; Department &rarr; Degree Program
 </p>
 <p>
 Selecting a degree level displays its associated departments; selecting a department displays its programs.
 The program's full title is directly used as the official name on issued certificates and automates expected graduation calculations.
 </p>
 </div>
 </div>

 {/* 3-Column Interactive Layout */}
 {loading && structure.length === 0 ? (
 <div className="flex min-h-[400px] flex-col items-center justify-center p-12">
 <LoadingSpinner size="lg" />
 <p className="mt-3 text-sm font-medium text-[var(--text-secondary)]">
 Loading academic program structure...
 </p>
 </div>
 ) : (
 <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
 {/* =============================================================== */}
 {/* COLUMN 1: CERTIFICATE LEVELS */}
 {/* =============================================================== */}
 <Card className="p-4 sm:p-5 flex flex-col h-full space-y-4 border border-[var(--border)] shadow-sm">
 <div className="flex items-center justify-between border-b border-[var(--border)] pb-3">
 <div className="flex items-center gap-2">
 <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-[var(--brand-light)] text-[var(--brand)]">
 <GraduationCap className="h-4 w-4" />
 </div>
 <div>
 <h2 className="text-base font-bold text-[var(--text-primary)]">
 1. Degree Levels
 </h2>
 <p className="text-xs text-[var(--text-muted)]">
 {structure.length} total levels
 </p>
 </div>
 </div>
 <Button
 size="sm"
 variant="primary"
 onClick={openAddLevelModal}
 icon={<Plus className="h-4 w-4" />}
 className="h-8 px-2.5 text-xs font-semibold"
 >
 Add Level
 </Button>
 </div>

 {structure.length === 0 ? (
 <div className="py-8 text-center">
 <EmptyState
 icon={GraduationCap}
 title="No Certificate Levels"
 message="Add your university's degree tiers (e.g. BSc, MSc, MBA, PhD)."
 action={openAddLevelModal}
 actionLabel="Add Level"
 />
 </div>
 ) : (
 <div className="space-y-2.5 max-h-[640px] overflow-y-auto pr-1">
 {structure?.map((level) => {
 const isSelected = level.id === selectedLevelId;
 const isActive = level.isActive !== false;

 return (
 <div
 key={level.id}
 onClick={() => setSelectedLevelId(level.id)}
 className={cn(
 'group relative rounded-xl border p-3.5 transition-all cursor-pointer select-none',
 isSelected
 ? 'border-[var(--brand)] bg-[var(--brand-light)]/20 shadow-sm ring-1 ring-[var(--brand)]/30'
 : 'border-[var(--border)] hover:border-[var(--brand)]/50 hover:bg-[var(--bg-elevated)]/40',
 !isActive && 'opacity-65'
 )}
 >
 <div className="flex items-start justify-between gap-2">
 <div className="space-y-1 min-w-0 flex-1">
 <div className="flex items-center gap-2 flex-wrap">
 <span className="font-bold text-sm text-[var(--text-primary)]">
 {level.shortName || level.name}
 </span>
 <Badge
 variant={isActive ? 'success' : 'default'}
 size="sm"
 >
 {isActive ? 'Active' : 'Inactive'}
 </Badge>
 {level.serialPrefix && (
 <span className="font-mono text-[11px] rounded bg-[var(--bg-elevated)] px-1.5 py-0.5 border border-[var(--border)] text-[var(--text-secondary)]">
 Prefix: {level.serialPrefix}
 </span>
 )}
 </div>
 <p className="text-xs text-[var(--text-secondary)] truncate" title={level.name}>
 {level.name}
 </p>
 <div className="flex items-center gap-3 text-[11px] text-[var(--text-muted)] pt-1">
 <span className="flex items-center gap-1">
 <Clock className="h-3 w-3" />
 {level.durationYears || 4} Years Duration
 </span>
 <span>•</span>
 <span>{level.departments?.length || 0} Depts</span>
 </div>
 </div>

 <div className="flex items-center gap-1 shrink-0 ml-2" onClick={(e) => e.stopPropagation()}>
 <button
 type="button"
 onClick={(e) => openEditLevelModal(level, e)}
 className="p-1.5 text-[var(--text-muted)] hover:text-[var(--brand)] rounded-lg hover:bg-[var(--bg-surface)] transition-colors"
 title="Edit level"
 >
 <Pencil className="h-3.5 w-3.5" />
 </button>
 <button
 type="button"
 role="switch"
 aria-checked={isActive}
 onClick={() => handleToggleLevelActive(level)}
 title={isActive ? 'Click to deactivate' : 'Click to activate'}
 className={cn(
 'relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 focus:outline-none',
 isActive ? 'bg-emerald-500' : 'bg-gray-300 '
 )}
 >
 <span
 className={cn(
 'pointer-events-none inline-block h-4 w-4 rounded-full bg-[var(--bg-surface)] shadow ring-0 transition-transform duration-200',
 isActive ? 'translate-x-4' : 'translate-x-0'
 )}
 />
 </button>
 </div>
 </div>

 {isSelected && (
 <div className="absolute -right-2 top-1/2 -translate-y-1/2 hidden lg:flex h-5 w-5 items-center justify-center rounded-full bg-[var(--brand)] text-white shadow-sm">
 <ChevronRight className="h-3.5 w-3.5" />
 </div>
 )}
 </div>
 );
 })}
 </div>
 )}
 </Card>

 {/* =============================================================== */}
 {/* COLUMN 2: DEPARTMENTS */}
 {/* =============================================================== */}
 <Card className="p-4 sm:p-5 flex flex-col h-full space-y-4 border border-[var(--border)] shadow-sm">
 <div className="flex items-center justify-between border-b border-[var(--border)] pb-3">
 <div className="flex items-center gap-2">
 <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-500/10 text-indigo-600 ">
 <Building2 className="h-4 w-4" />
 </div>
 <div>
 <h2 className="text-base font-bold text-[var(--text-primary)]">
 2. Departments
 </h2>
 <p className="text-xs text-[var(--text-muted)]">
 {selectedLevel
 ? `Under ${selectedLevel.shortName || selectedLevel.name} (${departmentsList.length})`
 : 'Select a level first'}
 </p>
 </div>
 </div>
 {selectedLevel && (
 <Button
 size="sm"
 variant="primary"
 onClick={openAddDeptModal}
 icon={<Plus className="h-4 w-4" />}
 className="h-8 px-2.5 text-xs font-semibold"
 >
 Add Dept
 </Button>
 )}
 </div>

 {!selectedLevel ? (
 <div className="flex flex-col items-center justify-center py-12 text-center text-xs text-[var(--text-muted)] border border-dashed border-[var(--border)] rounded-xl p-6">
 <GraduationCap className="h-8 w-8 mb-2 opacity-40" />
 <p className="font-semibold text-sm text-[var(--text-secondary)]">No Certificate Level Selected</p>
 <p className="mt-1">Select a degree level from Column 1 to manage its departments.</p>
 </div>
 ) : departmentsList.length === 0 ? (
 <div className="py-8 text-center">
 <EmptyState
 icon={Building2}
 title="No Departments"
 message={`No academic departments added under ${selectedLevel.shortName || selectedLevel.name} yet.`}
 action={openAddDeptModal}
 actionLabel="Add Department"
 />
 </div>
 ) : (
 <div className="space-y-2.5 max-h-[640px] overflow-y-auto pr-1">
 {departmentsList?.map((dept) => {
 const isSelected = dept.id === selectedDeptId;
 const isActive = dept.isActive !== false;

 return (
 <div
 key={dept.id}
 onClick={() => setSelectedDeptId(dept.id)}
 className={cn(
 'group relative rounded-xl border p-3.5 transition-all cursor-pointer select-none',
 isSelected
 ? 'border-[var(--brand)] bg-[var(--brand-light)]/20 shadow-sm ring-1 ring-[var(--brand)]/30'
 : 'border-[var(--border)] hover:border-[var(--brand)]/50 hover:bg-[var(--bg-elevated)]/40',
 !isActive && 'opacity-65'
 )}
 >
 <div className="flex items-start justify-between gap-2">
 <div className="space-y-1 min-w-0 flex-1">
 <div className="flex items-center gap-2 flex-wrap">
 <span className="font-bold text-sm text-[var(--text-primary)]">
 {dept.code || dept.name}
 </span>
 <Badge
 variant={isActive ? 'success' : 'default'}
 size="sm"
 >
 {isActive ? 'Active' : 'Inactive'}
 </Badge>
 </div>
 <p className="text-xs text-[var(--text-secondary)] truncate" title={dept.name}>
 {dept.name}
 </p>
 <div className="flex items-center gap-3 text-[11px] text-[var(--text-muted)] pt-1">
 <span>{dept.programs?.length || 0} Programs Offered</span>
 </div>
 </div>

 <div className="flex items-center gap-1 shrink-0 ml-2" onClick={(e) => e.stopPropagation()}>
 <button
 type="button"
 onClick={(e) => openEditDeptModal(dept, e)}
 className="p-1.5 text-[var(--text-muted)] hover:text-[var(--brand)] rounded-lg hover:bg-[var(--bg-surface)] transition-colors"
 title="Edit department"
 >
 <Pencil className="h-3.5 w-3.5" />
 </button>
 <button
 type="button"
 role="switch"
 aria-checked={isActive}
 onClick={() => handleToggleDeptActive(dept)}
 title={isActive ? 'Click to deactivate' : 'Click to activate'}
 className={cn(
 'relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 focus:outline-none',
 isActive ? 'bg-emerald-500' : 'bg-gray-300 '
 )}
 >
 <span
 className={cn(
 'pointer-events-none inline-block h-4 w-4 rounded-full bg-[var(--bg-surface)] shadow ring-0 transition-transform duration-200',
 isActive ? 'translate-x-4' : 'translate-x-0'
 )}
 />
 </button>
 </div>
 </div>

 {isSelected && (
 <div className="absolute -right-2 top-1/2 -translate-y-1/2 hidden lg:flex h-5 w-5 items-center justify-center rounded-full bg-[var(--brand)] text-white shadow-sm">
 <ChevronRight className="h-3.5 w-3.5" />
 </div>
 )}
 </div>
 );
 })}
 </div>
 )}
 </Card>

 {/* =============================================================== */}
 {/* COLUMN 3: PROGRAMS */}
 {/* =============================================================== */}
 <Card className="p-4 sm:p-5 flex flex-col h-full space-y-4 border border-[var(--border)] shadow-sm">
 <div className="flex items-center justify-between border-b border-[var(--border)] pb-3">
 <div className="flex items-center gap-2">
 <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-500/10 text-emerald-600 ">
 <BookOpen className="h-4 w-4" />
 </div>
 <div>
 <h2 className="text-base font-bold text-[var(--text-primary)]">
 3. Academic Programs
 </h2>
 <p className="text-xs text-[var(--text-muted)]">
 {selectedDept
 ? `Under ${selectedDept.code || selectedDept.name} (${programsList.length})`
 : 'Select a department first'}
 </p>
 </div>
 </div>
 {selectedDept && (
 <Button
 size="sm"
 variant="primary"
 onClick={openAddProgramModal}
 icon={<Plus className="h-4 w-4" />}
 className="h-8 px-2.5 text-xs font-semibold"
 >
 Add Program
 </Button>
 )}
 </div>

 {!selectedDept ? (
 <div className="flex flex-col items-center justify-center py-12 text-center text-xs text-[var(--text-muted)] border border-dashed border-[var(--border)] rounded-xl p-6">
 <Building2 className="h-8 w-8 mb-2 opacity-40" />
 <p className="font-semibold text-sm text-[var(--text-secondary)]">No Department Selected</p>
 <p className="mt-1">Select an academic department from Column 2 to manage its degree programs.</p>
 </div>
 ) : programsList.length === 0 ? (
 <div className="py-8 text-center">
 <EmptyState
 icon={BookOpen}
 title="No Programs Offered"
 message={`No degree programs configured under ${selectedDept.name} yet.`}
 action={openAddProgramModal}
 actionLabel="Add Program"
 />
 </div>
 ) : (
 <div className="space-y-2.5 max-h-[640px] overflow-y-auto pr-1">
 {programsList?.map((prog) => {
 const isActive = prog.isActive !== false;

 return (
 <div
 key={prog.id}
 className={cn(
 'rounded-xl border border-[var(--border)] p-3.5 transition-all bg-[var(--bg-surface)] hover:border-[var(--brand)]/40 hover:bg-[var(--bg-elevated)]/30',
 !isActive && 'opacity-65'
 )}
 >
 <div className="flex items-start justify-between gap-2">
 <div className="space-y-1.5 min-w-0 flex-1">
 <div className="flex items-center gap-2 flex-wrap">
 <span className="font-bold text-sm text-[var(--text-primary)]">
 {prog.shortName || prog.name}
 </span>
 <Badge
 variant={isActive ? 'success' : 'default'}
 size="sm"
 >
 {isActive ? 'Active' : 'Inactive'}
 </Badge>
 </div>
 <p className="text-xs text-[var(--text-secondary)] leading-relaxed">
 {prog.name}
 </p>
 <div className="flex items-center gap-2 pt-0.5">
 <span className="inline-flex items-center gap-1 text-[11px] text-[var(--brand)] font-medium">
 <Sparkles className="h-3 w-3" />
 Printed on Certificate
 </span>
 </div>
 </div>

 <div className="flex items-center gap-1 shrink-0 ml-2" onClick={(e) => e.stopPropagation()}>
 <button
 type="button"
 onClick={(e) => openEditProgramModal(prog, e)}
 className="p-1.5 text-[var(--text-muted)] hover:text-[var(--brand)] rounded-lg hover:bg-[var(--bg-elevated)] transition-colors"
 title="Edit program"
 >
 <Pencil className="h-3.5 w-3.5" />
 </button>
 <button
 type="button"
 role="switch"
 aria-checked={isActive}
 onClick={() => handleToggleProgramActive(prog)}
 title={isActive ? 'Click to deactivate' : 'Click to activate'}
 className={cn(
 'relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 focus:outline-none',
 isActive ? 'bg-emerald-500' : 'bg-gray-300 '
 )}
 >
 <span
 className={cn(
 'pointer-events-none inline-block h-4 w-4 rounded-full bg-[var(--bg-surface)] shadow ring-0 transition-transform duration-200',
 isActive ? 'translate-x-4' : 'translate-x-0'
 )}
 />
 </button>
 </div>
 </div>
 </div>
 );
 })}
 </div>
 )}
 </Card>
 </div>
 )}
 </div>

 {/* ===================================================================== */}
 {/* MODAL: ADD / EDIT CERTIFICATE LEVEL */}
 {/* ===================================================================== */}
 <Modal
 isOpen={levelModal.open}
 onClose={() => setLevelModal({ open: false, isEdit: false, data: null })}
 title={levelModal.isEdit ? 'Edit Degree Level' : 'Add Degree Level'}
 size="md"
 >
 <form onSubmit={handleSubmitLevel} className="space-y-4">
 <Input
 label="Degree Full Name"
 placeholder="e.g. Bachelor of Science"
 value={levelForm.name}
 onChange={(e) => setLevelForm({ ...levelForm, name: e.target.value })}
 required
 hint="Full academic degree title"
 />

 <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
 <Input
 label="Short Name / Code"
 placeholder="e.g. BSc"
 value={levelForm.shortName}
 onChange={(e) => setLevelForm({ ...levelForm, shortName: e.target.value })}
 required
 hint="Abbreviated label"
 />

 <Input
 label="Serial Prefix"
 placeholder="e.g. BSC"
 value={levelForm.serialPrefix}
 onChange={(e) => setLevelForm({ ...levelForm, serialPrefix: e.target.value.toUpperCase() })}
 required
 hint="Appears on certificate serials"
 />
 </div>

 <Input
 label="Program Duration (Years)"
 type="number"
 min={1}
 max={10}
 value={levelForm.durationYears}
 onChange={(e) => setLevelForm({ ...levelForm, durationYears: e.target.value })}
 required
 hint="Used to auto-calculate expected graduation date"
 />

 <div className="flex justify-end gap-2 pt-4 border-t border-[var(--border)]">
 <Button
 type="button"
 variant="secondary"
 onClick={() => setLevelModal({ open: false, isEdit: false, data: null })}
 >
 Cancel
 </Button>
 <Button type="submit" variant="primary" loading={submitting}>
 {levelModal.isEdit ? 'Save Changes' : 'Create Level'}
 </Button>
 </div>
 </form>
 </Modal>

 {/* ===================================================================== */}
 {/* MODAL: ADD / EDIT DEPARTMENT */}
 {/* ===================================================================== */}
 <Modal
 isOpen={deptModal.open}
 onClose={() => setDeptModal({ open: false, isEdit: false, data: null })}
 title={deptModal.isEdit ? 'Edit Department' : 'Add Academic Department'}
 size="md"
 >
 <form onSubmit={handleSubmitDept} className="space-y-4">
 <div className="rounded-lg bg-[var(--brand-light)]/20 p-3 text-xs text-[var(--text-secondary)]">
 Department under: <strong className="text-[var(--text-primary)]">{selectedLevel?.name} ({selectedLevel?.shortName})</strong>
 </div>

 <Input
 label="Department Name"
 placeholder="e.g. Computer Science and Engineering"
 value={deptForm.name}
 onChange={(e) => setDeptForm({ ...deptForm, name: e.target.value })}
 required
 />

 <Input
 label="Department Code"
 placeholder="e.g. CSE"
 value={deptForm.code}
 onChange={(e) => setDeptForm({ ...deptForm, code: e.target.value.toUpperCase() })}
 required
 hint="Short uppercase department acronym"
 />

 <div className="flex justify-end gap-2 pt-4 border-t border-[var(--border)]">
 <Button
 type="button"
 variant="secondary"
 onClick={() => setDeptModal({ open: false, isEdit: false, data: null })}
 >
 Cancel
 </Button>
 <Button type="submit" variant="primary" loading={submitting}>
 {deptModal.isEdit ? 'Save Changes' : 'Create Department'}
 </Button>
 </div>
 </form>
 </Modal>

 {/* ===================================================================== */}
 {/* MODAL: ADD / EDIT PROGRAM */}
 {/* ===================================================================== */}
 <Modal
 isOpen={programModal.open}
 onClose={() => setProgramModal({ open: false, isEdit: false, data: null })}
 title={programModal.isEdit ? 'Edit Program' : 'Add Academic Program'}
 size="md"
 >
 <form onSubmit={handleSubmitProgram} className="space-y-4">
 <div className="rounded-lg bg-[var(--brand-light)]/20 p-3 text-xs text-[var(--text-secondary)]">
 Program under: <strong className="text-[var(--text-primary)]">{selectedDept?.name} ({selectedDept?.code})</strong>
 </div>

 <Input
 label="Full Program Name (Certificate Title)"
 placeholder="e.g. Bachelor of Science in Computer Science and Engineering"
 value={programForm.name}
 onChange={(e) => setProgramForm({ ...programForm, name: e.target.value })}
 required
 hint="This exact name will be automatically populated on certificates"
 />

 <Input
 label="Short Display Name"
 placeholder="e.g. BSc in CSE"
 value={programForm.shortName}
 onChange={(e) => setProgramForm({ ...programForm, shortName: e.target.value })}
 hint="UI-friendly label for dropdowns and enrollment lists"
 />

 <div className="flex justify-end gap-2 pt-4 border-t border-[var(--border)]">
 <Button
 type="button"
 variant="secondary"
 onClick={() => setProgramModal({ open: false, isEdit: false, data: null })}
 >
 Cancel
 </Button>
 <Button type="submit" variant="primary" loading={submitting}>
 {programModal.isEdit ? 'Save Changes' : 'Create Program'}
 </Button>
 </div>
 </form>
 </Modal>
 </DashboardLayout>
 );
}
