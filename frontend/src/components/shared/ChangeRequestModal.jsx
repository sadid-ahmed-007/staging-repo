import { useState } from 'react';
import toast from 'react-hot-toast';
import { X, Upload, FileText, CheckCircle } from 'lucide-react';
import Button from './Button';
import Input from './Input';
import api from '../../services/api';

export default function ChangeRequestModal({ 
 isOpen, 
 onClose, 
 field, 
 currentValue, 
 onSuccess 
}) {
 const [loading, setLoading] = useState(false);
 const [form, setForm] = useState({
 requestedValue: '',
 reason: '',
 });
 const [file, setFile] = useState(null);

 if (!isOpen) return null;

 const handleSubmit = async (e) => {
 e.preventDefault();
 if (form.reason.length < 20) {
 toast.error('Reason must be at least 20 characters long.');
 return;
 }
 
 setLoading(true);
 try {
 const formData = new FormData();
 formData.append('fieldName', field.name);
 formData.append('requestedValue', form.requestedValue);
 formData.append('reason', form.reason);
 if (file) {
 formData.append('supportingDocument', file);
 }

 await api.post('/profile/change-request', formData, {
 headers: { 'Content-Type': 'multipart/form-data' }
 });

 toast.success('Change request submitted successfully.');
 onSuccess();
 onClose();
 } catch (error) {
 toast.error(error.response?.data?.message || 'Failed to submit request.');
 } finally {
 setLoading(false);
 }
 };

 const handleFileChange = (e) => {
 const selectedFile = e.target.files[0];
 if (selectedFile) {
 if (selectedFile.size > 5 * 1024 * 1024) {
 toast.error('Document must be under 5MB.');
 return;
 }
 setFile(selectedFile);
 }
 };

 return (
 <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-in fade-in duration-200">
 <div className="bg-[var(--bg-surface)] rounded-xl w-full max-w-lg shadow-xl overflow-hidden flex flex-col max-h-[90vh]">
 
 {/* Header */}
 <div className="flex items-center justify-between p-6 border-b border-[var(--border)] shrink-0">
 <div>
 <h2 className="text-xl font-bold text-[var(--text-primary)]">Request Change</h2>
 <p className="text-sm text-[var(--text-secondary)] mt-1">
 Submit a request to update your {field.label}
 </p>
 </div>
 <button onClick={onClose} className="p-2 text-[var(--text-muted)] hover:bg-[var(--bg-elevated)] rounded-lg transition-colors">
 <X className="w-5 h-5" />
 </button>
 </div>

 {/* Body */}
 <div className="p-6 overflow-y-auto">
 <form id="change-request-form" onSubmit={handleSubmit} className="space-y-6">
 
 <div className="p-4 bg-[var(--bg-elevated)] rounded-lg border border-[var(--border)]">
 <span className="text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider">Current {field.label}</span>
 <p className="mt-1 font-medium text-[var(--text-primary)]">{currentValue || 'Not provided'}</p>
 </div>

 <Input 
 label={`New ${field.label}`}
 value={form.requestedValue}
 onChange={(e) => setForm({ ...form, requestedValue: e.target.value })}
 placeholder={`Enter new ${field.label.toLowerCase()}`}
 required
 />

 <div>
 <label className="block text-sm font-medium text-[var(--text-secondary)] mb-1">
 Reason for Change
 </label>
 <textarea
 className="w-full rounded-lg border border-[var(--border)] bg-[var(--bg-surface)] px-3 py-2 text-sm text-[var(--text-primary)] focus:border-[var(--brand)] focus:outline-none focus:ring-1 focus:ring-[var(--brand)] transition-colors min-h-[100px]"
 value={form.reason}
 onChange={(e) => setForm({ ...form, reason: e.target.value })}
 placeholder="Explain why this change is necessary (min 20 characters)"
 required
 minLength={20}
 />
 <p className="text-xs text-[var(--text-muted)] mt-1">
 {form.reason.length < 20 ? `${20 - form.reason.length} more characters required` : 'Length is sufficient'}
 </p>
 </div>

 <div>
 <label className="block text-sm font-medium text-[var(--text-secondary)] mb-1">
 Supporting Document (Optional)
 </label>
 <div className="mt-1 flex justify-center px-6 pt-5 pb-6 border-2 border-[var(--border)] border-dashed rounded-lg hover:border-[var(--brand)]/50 transition-colors bg-[var(--bg-elevated)]">
 <div className="space-y-1 text-center">
 {!file ? (
 <>
 <Upload className="mx-auto h-8 w-8 text-[var(--text-muted)] mb-3" />
 <div className="flex text-sm text-[var(--text-secondary)] justify-center">
 <label className="relative cursor-pointer rounded-md font-medium text-[var(--brand)] hover:text-[var(--brand-dark)] focus-within:outline-none focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-[var(--brand)]">
 <span>Upload a file</span>
 <input type="file" className="sr-only" onChange={handleFileChange} accept=".pdf,.jpg,.jpeg,.png" />
 </label>
 </div>
 <p className="text-xs text-[var(--text-muted)] mt-2">
 PDF, PNG, JPG up to 5MB
 </p>
 </>
 ) : (
 <div className="flex flex-col items-center justify-center">
 <FileText className="mx-auto h-8 w-8 text-[var(--brand)] mb-2" />
 <p className="text-sm font-medium text-[var(--text-primary)] max-w-xs truncate">{file.name}</p>
 <p className="text-xs text-[var(--text-muted)]">{(file.size / 1024 / 1024).toFixed(2)} MB</p>
 <button 
 type="button" 
 onClick={() => setFile(null)}
 className="mt-3 text-xs text-red-500 hover:text-red-700 font-medium"
 >
 Remove file
 </button>
 </div>
 )}
 </div>
 </div>
 </div>
 
 </form>
 </div>

 {/* Footer */}
 <div className="p-6 border-t border-[var(--border)] bg-[var(--bg-elevated)] flex justify-end gap-3 shrink-0">
 <Button type="button" variant="secondary" onClick={onClose} disabled={loading}>
 Cancel
 </Button>
 <Button type="submit" form="change-request-form" loading={loading}>
 Submit Request
 </Button>
 </div>
 </div>
 </div>
 );
}
