import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { 
 User, Mail, Calendar, LogIn, Lock, Phone, MapPin, 
 Globe, Building, Shield, CheckCircle, GraduationCap,
 Eye, EyeOff, Save, Check, AlertTriangle, Trash2, UserX
} from 'lucide-react';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/shared/Card';
import Button from '../../components/shared/Button';
import Input from '../../components/shared/Input';
import Badge from '../../components/shared/Badge';
import LoadingSpinner from '../../components/shared/LoadingSpinner';
import ChangeRequestModal from '../../components/shared/ChangeRequestModal';
import { Camera } from 'lucide-react';
import api from '../../services/api';
import { useAuth } from '../../contexts/AuthContext';
import { formatDate, formatDateTime, roleLabel, cn } from '../../utils/helpers';
import { useNavigate } from 'react-router-dom';

export default function Profile() {
 const { user, logout } = useAuth();
 const navigate = useNavigate();
 
 const [loading, setLoading] = useState(true);
 const [profileData, setProfileData] = useState(null);
 const [activeTab, setActiveTab] = useState('personal');
 
 // Tab 1 Form State
 const [editForm, setEditForm] = useState({});
 const [savingProfile, setSavingProfile] = useState(false);
 
 // Tab 2 Form State
 const [passwordForm, setPasswordForm] = useState({
 currentPassword: '',
 newPassword: '',
 confirmNewPassword: ''
 });
 const [showPwd, setShowPwd] = useState({ current: false, new: false, confirm: false });
 const [savingPassword, setSavingPassword] = useState(false);
 const [pwdError, setPwdError] = useState('');

 const [pendingRequests, setPendingRequests] = useState([]);
 const [changeRequestModal, setChangeRequestModal] = useState({ isOpen: false, field: null, currentValue: null });
 const [avatarUploadLoading, setAvatarUploadLoading] = useState(false);

 // Deactivation / deletion state
 const [showDeactivateModal, setShowDeactivateModal] = useState(false);
 const [showDeleteModal, setShowDeleteModal] = useState(false);
 const [dangerPassword, setDangerPassword] = useState('');
 const [dangerReason, setDangerReason] = useState('');
 const [dangerLoading, setDangerLoading] = useState(false);


 const loadProfile = async () => {
 setLoading(true);
 try {
 const { data } = await api.get('/profile');
 const { data: requestsData } = await api.get('/profile/change-requests');
 setPendingRequests(requestsData.data || []);
 setProfileData(data);
 
 // Initialize edit form based on role
 const initialForm = {};
 if (data.role === 'student' && data.student) {
 initialForm.phone = data.student.phone || '';
 initialForm.address = data.student.address || '';
 } else if (data.role === 'university' && data.university) {
 initialForm.phone = data.university.phone || '';
 initialForm.address = data.university.address || '';
 initialForm.city = data.university.city || '';
 initialForm.website = data.university.website || '';
 initialForm.defaultAuthorityName = data.university.defaultAuthorityName || '';
 initialForm.defaultAuthorityTitle = data.university.defaultAuthorityTitle || '';
 } else if (data.role === 'verifier' && data.verifier) {
 initialForm.phone = data.verifier.phone || '';
 initialForm.website = data.verifier.website || '';
 initialForm.purpose = data.verifier.purpose || '';
 }
 setEditForm(initialForm);
 } catch (error) {
 toast.error('Failed to load profile information.');
 } finally {
 setLoading(false);
 }
 };

 useEffect(() => {
 loadProfile();
 }, []);

 
 const handleAvatarUpload = async (e) => {
 const file = e.target.files[0];
 if (!file) return;
 
 if (file.size > 300 * 1024) {
 toast.error('Image must be under 300KB.');
 return;
 }
 
 setAvatarUploadLoading(true);
 try {
 const formData = new FormData();
 formData.append('avatar', file);
 
 const { data } = await api.post('/profile/avatar', formData, {
 headers: { 'Content-Type': 'multipart/form-data' }
 });
 
 toast.success('Profile picture updated!');
 loadProfile();
 } catch (err) {
 toast.error(err.response?.data?.message || 'Failed to upload avatar.');
 } finally {
 setAvatarUploadLoading(false);
 }
 };

 const handleAvatarRemove = async () => {
 if (!window.confirm('Are you sure you want to remove your profile picture?')) return;
 setAvatarUploadLoading(true);
 try {
 await api.delete('/profile/avatar');
 toast.success('Profile picture removed!');
 loadProfile();
 } catch (err) {
 toast.error('Failed to remove avatar.');
 } finally {
 setAvatarUploadLoading(false);
 }
 };

 const handleProfileSave = async (e) => {
 e.preventDefault();
 setSavingProfile(true);
 try {
 await api.put('/profile', editForm);
 toast.success('Profile updated successfully.');
 loadProfile();
 } catch (error) {
 toast.error(error.response?.data?.message || 'Profile update failed.');
 } finally {
 setSavingProfile(false);
 }
 };

 const handlePasswordSave = async (e) => {
 e.preventDefault();
 setPwdError('');

 if (!passwordForm.currentPassword) {
 setPwdError('Current password is required.');
 return;
 }
 if (passwordForm.newPassword.length < 8) {
 setPwdError('New password must be at least 8 characters long.');
 return;
 }
 if (passwordForm.newPassword !== passwordForm.confirmNewPassword) {
 setPwdError('New passwords do not match.');
 return;
 }

 setSavingPassword(true);
 try {
 const { data } = await api.patch('/profile/password', passwordForm);
 toast.success(data.message || 'Password changed. Please log in again.');
 
 setTimeout(async () => {
 await logout();
 navigate('/login');
 }, 2000);
 
 } catch (error) {
 setPwdError(error.response?.data?.message || 'Failed to change password.');
 } finally {
 setSavingPassword(false);
 }
 };

 const handleDeactivate = async () => {
 if (!dangerPassword) { toast.error('Password is required.'); return; }
 setDangerLoading(true);
 try {
 await api.post('/profile/deactivate', { password: dangerPassword });
 toast.success('Account deactivated. You will be logged out.');
 setTimeout(async () => { await logout(); navigate('/login'); }, 2000);
 } catch (err) {
 toast.error(err.response?.data?.message || 'Failed to deactivate account.');
 } finally {
 setDangerLoading(false);
 }
 };

 const handleRequestDeletion = async () => {
 if (!dangerPassword) { toast.error('Password is required.'); return; }
 setDangerLoading(true);
 try {
 await api.post('/profile/request-deletion', { password: dangerPassword, reason: dangerReason });
 toast.success('Deletion request submitted. You will be logged out.');
 setTimeout(async () => { await logout(); navigate('/login'); }, 2500);
 } catch (err) {
 toast.error(err.response?.data?.message || 'Failed to submit deletion request.');
 } finally {
 setDangerLoading(false);
 }
 };

 if (loading || !profileData) {
 return (
 <DashboardLayout>
 <div className="flex min-h-[50vh] items-center justify-center"><LoadingSpinner /></div>
 <ChangeRequestModal
 isOpen={changeRequestModal.isOpen}
 onClose={() => setChangeRequestModal({ isOpen: false, field: null, currentValue: null })}
 field={changeRequestModal.field}
 currentValue={changeRequestModal.currentValue}
 onSuccess={() => loadProfile()}
 />
 </DashboardLayout>
 );
 }

 // Derived display values
 let displayName = profileData.email;
 if (profileData.role === 'student' && profileData.student) {
 const s = profileData.student;
 displayName = `${s.firstName} ${s.middleName || ''} ${s.lastName}`.replace(/\s+/g, ' ').trim();
 } else if (profileData.role === 'university' && profileData.university) {
 displayName = profileData.university.name;
 } else if (profileData.role === 'verifier' && profileData.verifier) {
 displayName = profileData.verifier.companyName;
 }
 
 const getInitials = (name) => {
 const parts = name.trim().split(' ');
 if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
 return name.substring(0, 2).toUpperCase();
 };

 
 const openChangeRequest = (name, label, value) => {
 setChangeRequestModal({
 isOpen: true,
 field: { name, label },
 currentValue: value
 });
 };

 const getPwdStrength = (pwd) => {
 if (!pwd) return { label: '', color: 'bg-gray-200' };
 if (pwd.length < 6) return { label: 'Weak', color: 'bg-red-500', w: 'w-1/3' };
 if (pwd.length < 10) return { label: 'Medium', color: 'bg-amber-500', w: 'w-2/3' };
 return { label: 'Strong', color: 'bg-green-500', w: 'w-full' };
 };

 const pwdStrength = getPwdStrength(passwordForm.newPassword);

 const tabs = [
 { id: 'personal', label: 'Personal Information' },
 { id: 'security', label: 'Security' }
 ];
 if (profileData.role === 'student') {
 tabs.push({ id: 'enrollment', label: 'Current Enrollment' });
 }

 return (
 <DashboardLayout>
 <div className="mx-auto max-w-6xl space-y-6">
 <div>
 <h1 className="text-2xl font-bold tracking-tight text-[var(--text-primary)] sm:text-3xl">
 My Profile
 </h1>
 <p className="mt-1 text-sm text-[var(--text-secondary)]">
 Manage your account settings and preferences
 </p>
 </div>

 <div className="flex flex-col gap-6 lg:flex-row lg:items-start">
 
 {/* LEFT COLUMN: Profile Card (1/3) */}
 <div className="w-full lg:w-1/3 shrink-0">
 <Card className="overflow-hidden">
 <div className="h-24 bg-gradient-to-r from-[var(--brand)] to-blue-500"></div>
 <div className="px-6 pb-6 relative">
 
 <div className="absolute -top-12 left-6 h-24 w-24 rounded-full border-4 border-[var(--bg-surface)] bg-[var(--bg-surface)] shadow-sm flex items-center justify-center text-3xl font-bold text-[var(--brand)] overflow-hidden group">
 {profileData?.student?.avatarUrl || profileData?.university?.avatarUrl || profileData?.verifier?.avatarUrl ? (
 <img src={profileData?.student?.avatarUrl || profileData?.university?.avatarUrl || profileData?.verifier?.avatarUrl} alt="Avatar" className="w-full h-full object-cover" />
 ) : (
 getInitials(displayName)
 )}
 <label className="absolute inset-0 bg-black/50 hidden group-hover:flex flex-col items-center justify-center cursor-pointer text-white transition-all">
 {avatarUploadLoading ? <LoadingSpinner /> : <Camera className="w-6 h-6" />}
 <input type="file" className="hidden" accept="image/png, image/jpeg, image/webp" onChange={handleAvatarUpload} disabled={avatarUploadLoading} />
 </label>
 </div>

 
 <div className="pt-14">
 <h2 className="text-xl font-bold text-[var(--text-primary)]">{displayName}</h2>
 {(profileData?.student?.avatarUrl || profileData?.university?.avatarUrl || profileData?.verifier?.avatarUrl) && (
 <button onClick={handleAvatarRemove} className="text-xs text-red-500 hover:text-red-700 mt-1">
 Remove photo
 </button>
 )}
 <Badge variant="primary" className="mt-2 capitalize">{roleLabel(profileData.role)}</Badge>
 
 <div className="mt-6 space-y-4 border-t border-[var(--border)] pt-6">
 <div className="flex items-center gap-3 text-sm text-[var(--text-secondary)]">
 <Mail className="h-4 w-4 text-[var(--text-muted)] shrink-0" />
 <span className="truncate">{profileData.email}</span>
 <Lock className="h-3 w-3 text-[var(--text-muted)] shrink-0 ml-auto" title="Cannot be changed here" />
 </div>
 <div className="flex items-center gap-3 text-sm text-[var(--text-secondary)]">
 <Calendar className="h-4 w-4 text-[var(--text-muted)] shrink-0" />
 <span>Member since {formatDate(profileData.createdAt)}</span>
 </div>
 <div className="flex items-center gap-3 text-sm text-[var(--text-secondary)]">
 <LogIn className="h-4 w-4 text-[var(--text-muted)] shrink-0" />
 <span>Last login: {profileData.lastLoginAt ? formatDateTime(profileData.lastLoginAt) : 'Just now'}</span>
 </div>
 </div>
 </div>
 </div>
 </Card>
 </div>

 {/* RIGHT COLUMN: Tabs (2/3) */}
 <div className="w-full lg:w-2/3 min-w-0">
 <div className="mb-4 flex gap-2 overflow-x-auto border-b border-[var(--border)] pb-px scrollbar-hide">
 {tabs?.map((tab) => (
 <button
 key={tab.id}
 onClick={() => setActiveTab(tab.id)}
 className={cn(
 "whitespace-nowrap px-4 py-2.5 text-sm font-medium transition-colors border-b-2",
 activeTab === tab.id
 ? "border-[var(--brand)] text-[var(--brand)]"
 : "border-transparent text-[var(--text-secondary)] hover:border-[var(--border)] hover:text-[var(--text-primary)]"
 )}
 >
 {tab.label}
 </button>
 ))}
 </div>

 <Card className="p-6">
 
 {/* TAB 1: PERSONAL INFORMATION */}
 {activeTab === 'personal' && (
 <div className="animate-in fade-in duration-300">
 <h3 className="text-lg font-semibold text-[var(--text-primary)] mb-6">Personal Information</h3>
 
 <form onSubmit={handleProfileSave} className="space-y-8">
 
 {/* Read-only Section */}
 {profileData.role !== 'admin' && (
 <div className="space-y-4">
 <h4 className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">Locked Information</h4>
 <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
 
 {profileData.role === 'student' && profileData.student && (
 <>
 <LockedField label="First Name" name="firstName" value={profileData.student.firstName} pendingRequests={pendingRequests} onRequesChange={openChangeRequest} />
 <LockedField label="Last Name" name="lastName" value={profileData.student.lastName} pendingRequests={pendingRequests} onRequesChange={openChangeRequest} />
 <LockedField label="Date of Birth" name="dateOfBirth" value={formatDate(profileData.student.dateOfBirth)} pendingRequests={pendingRequests} onRequesChange={openChangeRequest} />
 <LockedField label="Student ID" value={profileData.student.studentId} />
 <LockedField label="NID / Birth Cert" name="nid" value={profileData.student.nidHash === 'Set' ? 'Set ✓' : 'Not provided'} pendingRequests={pendingRequests} onRequesChange={openChangeRequest} />
 </>
 )}

 {profileData.role === 'university' && profileData.university && (
 <>
 <LockedField label="Institution Name" name="name" value={profileData.university.name} pendingRequests={pendingRequests} onRequesChange={openChangeRequest} />
 <LockedField label="Registration Number" value={profileData.university.registrationNumber} />
 </>
 )}

 {profileData.role === 'verifier' && profileData.verifier && (
 <>
 <LockedField label="Company Name" name="companyName" value={profileData.verifier.companyName} pendingRequests={pendingRequests} onRequesChange={openChangeRequest} />
 <LockedField label="Registration Number" value={profileData.verifier.registrationNumber || 'N/A'} />
 </>
 )}

 </div>
 </div>
 )}

 {profileData.role === 'admin' && (
 <div className="py-8 text-center text-sm text-[var(--text-muted)]">
 No additional profile fields for Admin role.
 </div>
 )}

 {/* Editable Section */}
 {profileData.role !== 'admin' && (
 <div className="space-y-4 pt-6 border-t border-[var(--border)]">
 <h4 className="text-xs font-semibold uppercase tracking-wider text-[var(--brand)]">Editable Information</h4>
 <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
 
 {profileData.role === 'student' && (
 <>
 <Input label="Phone Number" value={editForm.phone || ''} onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })} />
 <div className="sm:col-span-2">
 <label className="mb-1 block text-sm font-medium text-[var(--text-secondary)]">Address</label>
 <textarea
 className="w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] px-3 py-2 text-sm text-[var(--text-primary)] focus:border-[var(--brand)] focus:outline-none focus:ring-1 focus:ring-[var(--brand)] transition-colors min-h-[80px]"
 value={editForm.address || ''}
 onChange={(e) => setEditForm({ ...editForm, address: e.target.value })}
 />
 </div>
 </>
 )}

 {profileData.role === 'university' && (
 <>
 <Input label="City" value={editForm.city || ''} onChange={(e) => setEditForm({ ...editForm, city: e.target.value })} />
 <Input label="Phone" value={editForm.phone || ''} onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })} />
 <Input label="Website" value={editForm.website || ''} onChange={(e) => setEditForm({ ...editForm, website: e.target.value })} />
 <Input label="Default Authority Name" value={editForm.defaultAuthorityName || ''} onChange={(e) => setEditForm({ ...editForm, defaultAuthorityName: e.target.value })} placeholder="e.g. Dr. John Doe" />
 <Input label="Default Authority Title" value={editForm.defaultAuthorityTitle || ''} onChange={(e) => setEditForm({ ...editForm, defaultAuthorityTitle: e.target.value })} placeholder="e.g. Vice Chancellor" />
 <div className="sm:col-span-2">
 <label className="mb-1 block text-sm font-medium text-[var(--text-secondary)]">Address</label>
 <textarea
 className="w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] px-3 py-2 text-sm text-[var(--text-primary)] focus:border-[var(--brand)] focus:outline-none focus:ring-1 focus:ring-[var(--brand)] transition-colors min-h-[80px]"
 value={editForm.address || ''}
 onChange={(e) => setEditForm({ ...editForm, address: e.target.value })}
 />
 </div>
 </>
 )}

 {profileData.role === 'verifier' && (
 <>
 <Input label="Phone" value={editForm.phone || ''} onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })} />
 <Input label="Website" value={editForm.website || ''} onChange={(e) => setEditForm({ ...editForm, website: e.target.value })} />
 <div className="sm:col-span-2">
 <label className="mb-1 block text-sm font-medium text-[var(--text-secondary)]">Purpose of Verification</label>
 <textarea
 className="w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] px-3 py-2 text-sm text-[var(--text-primary)] focus:border-[var(--brand)] focus:outline-none focus:ring-1 focus:ring-[var(--brand)] transition-colors min-h-[80px]"
 value={editForm.purpose || ''}
 onChange={(e) => setEditForm({ ...editForm, purpose: e.target.value })}
 />
 </div>
 </>
 )}

 </div>
 
 <div className="pt-4 flex justify-end">
 <Button type="submit" loading={savingProfile}>
 <Save className="w-4 h-4 mr-2" />
 Save Changes
 </Button>
 </div>
 </div>
 )}
 </form>
 </div>
 )}

 {/* TAB 2: SECURITY */}
 {activeTab === 'security' && (
 <div className="animate-in fade-in duration-300 space-y-8">
 <div>
 <h3 className="text-lg font-semibold text-[var(--text-primary)] mb-6">Change Password</h3>
 <form onSubmit={handlePasswordSave} className="space-y-4 max-w-md">
 
 {pwdError && (
 <div className="p-3 text-sm text-red-600 bg-red-50 rounded-lg border border-red-100">
 {pwdError}
 </div>
 )}

 <div className="relative">
 <Input 
 label="Current Password" 
 type={showPwd.current ? "text" : "password"} 
 value={passwordForm.currentPassword}
 onChange={(e) => setPasswordForm({ ...passwordForm, currentPassword: e.target.value })}
 required
 />
 <button type="button" onClick={() => setShowPwd({ ...showPwd, current: !showPwd.current })} className="absolute right-3 top-[34px] text-[var(--text-muted)] hover:text-[var(--text-secondary)]">
 {showPwd.current ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
 </button>
 </div>

 <div className="relative">
 <Input 
 label="New Password" 
 type={showPwd.new ? "text" : "password"} 
 value={passwordForm.newPassword}
 onChange={(e) => setPasswordForm({ ...passwordForm, newPassword: e.target.value })}
 required
 minLength={8}
 />
 <button type="button" onClick={() => setShowPwd({ ...showPwd, new: !showPwd.new })} className="absolute right-3 top-[34px] text-[var(--text-muted)] hover:text-[var(--text-secondary)]">
 {showPwd.new ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
 </button>
 
 {passwordForm.newPassword && (
 <div className="mt-2">
 <div className="flex items-center justify-between text-xs mb-1">
 <span className="text-[var(--text-muted)]">Password strength:</span>
 <span className="font-medium text-[var(--text-secondary)]">{pwdStrength.label}</span>
 </div>
 <div className="h-1.5 w-full bg-[var(--bg-elevated)] rounded-full overflow-hidden">
 <div className={cn("h-full transition-all duration-300", pwdStrength.color, pwdStrength.w)}></div>
 </div>
 </div>
 )}
 </div>

 <div className="relative">
 <Input 
 label="Confirm New Password" 
 type={showPwd.confirm ? "text" : "password"} 
 value={passwordForm.confirmNewPassword}
 onChange={(e) => setPasswordForm({ ...passwordForm, confirmNewPassword: e.target.value })}
 required
 minLength={8}
 />
 <button type="button" onClick={() => setShowPwd({ ...showPwd, confirm: !showPwd.confirm })} className="absolute right-3 top-[34px] text-[var(--text-muted)] hover:text-[var(--text-secondary)]">
 {showPwd.confirm ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
 </button>
 </div>

 <div className="pt-2">
 <Button type="submit" loading={savingPassword}>Change Password</Button>
 </div>
 </form>
 </div>

 <div className="pt-8 border-t border-[var(--border)]">
 <h3 className="text-lg font-semibold text-[var(--text-primary)] mb-6">Account Information</h3>
 <div className="grid grid-cols-1 sm:grid-cols-2 gap-y-4 gap-x-8 max-w-2xl">
 <div className="flex flex-col gap-1">
 <span className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">Email Address</span>
 <div className="flex items-center gap-2 text-sm text-[var(--text-primary)] font-medium">
 {profileData.email}
 <Lock className="w-3 h-3 text-[var(--text-muted)]" />
 </div>
 </div>
 <div className="flex flex-col gap-1">
 <span className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">Account Status</span>
 <div className="flex items-center gap-1.5 text-sm font-medium text-green-600">
 <CheckCircle className="w-4 h-4" />
 Approved
 </div>
 </div>
 <div className="flex flex-col gap-1">
 <span className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">Email Verification</span>
 <div className="flex items-center gap-1.5 text-sm font-medium text-green-600">
 <CheckCircle className="w-4 h-4" />
 Verified
 </div>
 </div>
 <div className="flex flex-col gap-1">
 <span className="text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">Account Created</span>
 <div className="text-sm font-medium text-[var(--text-primary)]">
 {formatDateTime(profileData.createdAt)}
 </div>
 </div>
 </div>
 </div>

 {/* DANGER ZONE */}
 <div className="pt-8 border-t-2 border-dashed border-red-200 /40">
 <div className="flex items-center gap-2 mb-1">
 <AlertTriangle className="w-5 h-5 text-red-500" />
 <h3 className="text-lg font-semibold text-red-600 ">Danger Zone</h3>
 </div>
 <p className="text-sm text-[var(--text-secondary)] mb-6">These actions are irreversible. Please proceed with caution.</p>

 <div className="space-y-4 max-w-lg">
 {/* Deactivate */}
 <div className="rounded-xl border border-amber-200 /40 bg-amber-50 /10 p-5">
 <div className="flex items-start gap-3">
 <UserX className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
 <div className="flex-1">
 <h4 className="font-semibold text-amber-900 ">Deactivate Account</h4>
 <p className="text-sm text-amber-700 /80 mt-1">
 Your account will be temporarily disabled. Contact admin to reactivate.
 </p>
 </div>
 </div>
 {showDeactivateModal ? (
 <div className="mt-4 space-y-3">
 <Input
 label="Confirm your password"
 type="password"
 value={dangerPassword}
 onChange={(e) => setDangerPassword(e.target.value)}
 placeholder="Your current password"
 />
 <div className="flex gap-2">
 <Button variant="secondary" size="sm" onClick={() => { setShowDeactivateModal(false); setDangerPassword(''); }}>Cancel</Button>
 <button
 onClick={handleDeactivate}
 disabled={dangerLoading}
 className="px-4 py-2 text-sm font-semibold rounded-lg bg-amber-600 text-white hover:bg-amber-700 transition-colors disabled:opacity-50"
 >
 {dangerLoading ? 'Processing...' : 'Confirm Deactivation'}
 </button>
 </div>
 </div>
 ) : (
 <button
 onClick={() => setShowDeactivateModal(true)}
 className="mt-4 px-4 py-2 text-sm font-semibold rounded-lg border border-amber-400 text-amber-700 hover:bg-amber-100 dark:hover:bg-amber-900/30 transition-colors"
 >
 Deactivate My Account
 </button>
 )}
 </div>

 {/* Request Deletion */}
 <div className="rounded-xl border border-red-200 /40 bg-red-50 /10 p-5">
 <div className="flex items-start gap-3">
 <Trash2 className="w-5 h-5 text-red-600 shrink-0 mt-0.5" />
 <div className="flex-1">
 <h4 className="font-semibold text-red-900 ">Request Account Deletion</h4>
 <p className="text-sm text-red-700 /80 mt-1">
 Submit a deletion request to an administrator. Your account will be deactivated immediately and permanently deleted after admin review.
 </p>
 </div>
 </div>
 {showDeleteModal ? (
 <div className="mt-4 space-y-3">
 <Input
 label="Confirm your password"
 type="password"
 value={dangerPassword}
 onChange={(e) => setDangerPassword(e.target.value)}
 placeholder="Your current password"
 />
 <div>
 <label className="block text-sm font-medium text-[var(--text-secondary)] mb-1">Reason (optional)</label>
 <textarea
 className="w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] px-3 py-2 text-sm text-[var(--text-primary)] focus:border-red-400 focus:outline-none focus:ring-1 focus:ring-red-400 min-h-[80px]"
 value={dangerReason}
 onChange={(e) => setDangerReason(e.target.value)}
 placeholder="Why do you want to delete your account?"
 />
 </div>
 <div className="flex gap-2">
 <Button variant="secondary" size="sm" onClick={() => { setShowDeleteModal(false); setDangerPassword(''); setDangerReason(''); }}>Cancel</Button>
 <button
 onClick={handleRequestDeletion}
 disabled={dangerLoading}
 className="px-4 py-2 text-sm font-semibold rounded-lg bg-red-600 text-white hover:bg-red-700 transition-colors disabled:opacity-50"
 >
 {dangerLoading ? 'Processing...' : 'Submit Deletion Request'}
 </button>
 </div>
 </div>
 ) : (
 <button
 onClick={() => setShowDeleteModal(true)}
 className="mt-4 px-4 py-2 text-sm font-semibold rounded-lg border border-red-400 text-red-700 hover:bg-red-100 dark:hover:bg-red-900/30 transition-colors"
 >
 Request Account Deletion
 </button>
 )}
 </div>
 </div>
 </div>

 </div>
 )}

 {/* TAB 3: CURRENT ENROLLMENT (Student Only) */}
 {activeTab === 'enrollment' && profileData.role === 'student' && (
 <div className="animate-in fade-in duration-300">
 <h3 className="text-lg font-semibold text-[var(--text-primary)] mb-6">Current Enrollment</h3>
 
 {profileData.currentEnrollment ? (
 <div className="rounded-xl border border-[var(--brand)] bg-[var(--brand-light)] p-6 dark:border-[var(--brand)]/30 dark:bg-[var(--brand)]/10">
 <div className="flex items-start justify-between mb-4">
 <div className="flex items-center gap-3">
 <div className="h-12 w-12 rounded-lg bg-[var(--bg-surface)] dark:bg-[var(--bg-surface)] flex items-center justify-center border border-[var(--border)] shrink-0 shadow-sm text-[var(--brand)]">
 <Building className="w-6 h-6" />
 </div>
 <div>
 <h4 className="text-xl font-bold text-[var(--text-primary)]">
 {profileData.currentEnrollment.institutionName}
 </h4>
 <p className="text-sm text-[var(--brand)] font-medium mt-0.5">
 {profileData.student.studentId}
 </p>
 </div>
 </div>
 <Badge variant="success" className="uppercase">{profileData.currentEnrollment.status}</Badge>
 </div>

 <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mt-6">
 <div>
 <p className="text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider">Program</p>
 <p className="text-sm font-medium text-[var(--text-primary)] mt-1">{profileData.currentEnrollment.program}</p>
 </div>
 <div>
 <p className="text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider">Batch</p>
 <p className="text-sm font-medium text-[var(--text-primary)] mt-1">{profileData.currentEnrollment.batch}</p>
 </div>
 <div>
 <p className="text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider">Enrollment Date</p>
 <p className="text-sm font-medium text-[var(--text-primary)] mt-1">
 {formatDate(profileData.currentEnrollment.enrollmentDate || new Date().toISOString()) /* fallback since API didn't return enrollmentDate */}
 </p>
 </div>
 <div>
 <p className="text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider">Expected Graduation</p>
 <p className="text-sm font-medium text-[var(--text-primary)] mt-1">
 {formatDate(profileData.currentEnrollment.expectedGraduationDate)}
 </p>
 </div>
 </div>

 <div className="mt-8 pt-4 border-t border-[var(--brand)]/20">
 <Button 
 variant="secondary" 
 className="w-full sm:w-auto"
 onClick={() => navigate('/student/my-university')}
 >
 View Full Details &rarr;
 </Button>
 </div>
 </div>
 ) : (
 <div className="rounded-xl border border-dashed border-[var(--border)] bg-[var(--bg-surface)] p-12 text-center">
 <GraduationCap className="mx-auto h-12 w-12 text-[var(--text-muted)]" />
 <h4 className="mt-4 text-lg font-medium text-[var(--text-primary)]">Not currently enrolled</h4>
 <p className="mt-1 text-sm text-[var(--text-secondary)] mb-6">
 You are not currently enrolled in any university program.
 </p>
 <Button onClick={() => navigate('/student/universities')}>
 Browse Universities
 </Button>
 </div>
 )}
 </div>
 )}

 </Card>
 </div>
 </div>
 </div>
 <ChangeRequestModal
 isOpen={changeRequestModal.isOpen}
 onClose={() => setChangeRequestModal({ isOpen: false, field: null, currentValue: null })}
 field={changeRequestModal.field}
 currentValue={changeRequestModal.currentValue}
 onSuccess={() => loadProfile()}
 />
 </DashboardLayout>
 );
}


function LockedField({ label, value, name, pendingRequests, onRequesChange }) {
 const isPending = pendingRequests?.some(req => req.fieldName === name && req.status === 'pending');

 return (
 <div className="bg-[var(--bg-elevated)] border border-[var(--border)] rounded-lg p-3 relative group transition-colors hover:border-[var(--brand)]/30">
 <div className="flex items-center justify-between mb-1">
 <span className="text-xs font-medium text-[var(--text-secondary)]">{label}</span>
 {isPending ? (
 <span className="text-[10px] font-semibold uppercase tracking-wider text-amber-500 bg-amber-50 px-1.5 py-0.5 rounded">Pending</span>
 ) : (
 <Lock className="w-3 h-3 text-[var(--text-muted)]" />
 )}
 </div>
 <div className="text-sm font-medium text-[var(--text-primary)] truncate" title={value}>{value || '-'}</div>
 
 {!isPending && name && (
 <div className="absolute inset-0 bg-black/5 hidden group-hover:flex items-center justify-center rounded-lg backdrop-blur-[1px]">
 <button 
 onClick={() => onRequesChange(name, label, value)}
 className="bg-[var(--bg-surface)] text-[var(--brand)] text-xs font-semibold px-3 py-1.5 rounded-md shadow-sm border border-[var(--brand)] hover:bg-[var(--brand)] hover:text-white transition-colors"
 >
 Request Change
 </button>
 </div>
 )}
 </div>
 );
}
