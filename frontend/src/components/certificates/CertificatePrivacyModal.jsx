import { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import Modal from '../shared/Modal';
import Button from '../shared/Button';
import ToggleSwitch from '../shared/ToggleSwitch';
import api from '../../services/api';
import { Settings2, Globe, ShieldOff, Bell, BellOff, Info } from 'lucide-react';

/**
 * CertificatePrivacyModal
 *
 * Displays granular privacy settings for a single certificate and allows
 * the student to update them via PATCH /api/student/certificates/{id}/privacy.
 *
 * Props:
 *   open          – boolean
 *   onClose       – function
 *   certificate   – the certificate object (must have id + privacy fields)
 *   onSaved       – function(updatedPrivacy) called after successful save
 */
export default function CertificatePrivacyModal({ open, onClose, certificate, onSaved }) {
  const [saving, setSaving] = useState(false);

  // Local draft of privacy toggles
  const [draft, setDraft] = useState({
    isPubliclyShareable: true,
    allowAnonymousVerification: true,
    notifyOnVerification: true,
    notifyOnAnonymousOnly: false,
  });

  // Sync draft when certificate changes (or modal opens)
  useEffect(() => {
    if (certificate) {
      setDraft({
        isPubliclyShareable:        certificate.isPubliclyShareable        ?? true,
        allowAnonymousVerification:  certificate.allowAnonymousVerification  ?? true,
        notifyOnVerification:        certificate.notifyOnVerification        ?? true,
        notifyOnAnonymousOnly:       certificate.notifyOnAnonymousOnly       ?? false,
      });
    }
  }, [certificate, open]);

  if (!open || !certificate) return null;

  const toggle = (field) => (value) => setDraft((prev) => ({ ...prev, [field]: value }));

  const handleSave = async () => {
    setSaving(true);
    try {
      const { data } = await api.patch(`/student/certificates/${certificate.id}/privacy`, draft);
      if (data.success) {
        toast.success('Privacy settings saved');
        onSaved && onSaved({ ...draft });
        onClose();
      } else {
        toast.error(data.message || 'Failed to save privacy settings');
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save privacy settings');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Certificate Privacy Settings" size="sm">
      <div className="space-y-5">
        {/* Header hint */}
        <p className="text-sm text-[var(--text-secondary)]">
          Control how this certificate can be verified and when you receive notifications.
        </p>

        <div className="divide-y divide-[var(--border)]">
          {/* Toggle 1: Public sharing */}
          <div className="py-4">
            <ToggleSwitch
              checked={draft.isPubliclyShareable}
              onChange={toggle('isPubliclyShareable')}
              label={
                <span className="flex items-center gap-2 font-semibold text-sm text-[var(--text-primary)]">
                  <Globe className="h-4 w-4 text-green-500" />
                  Allow public sharing
                </span>
              }
              description="Others can find and view this certificate's details."
            />
          </div>

          {/* Toggle 2: Anonymous verification */}
          <div className="py-4">
            <ToggleSwitch
              checked={draft.allowAnonymousVerification}
              onChange={toggle('allowAnonymousVerification')}
              label={
                <span className="flex items-center gap-2 font-semibold text-sm text-[var(--text-primary)]">
                  <ShieldOff className="h-4 w-4 text-amber-500" />
                  Allow anonymous verification
                </span>
              }
              description="Anyone with the serial number and your date of birth can verify this certificate without logging in. Disable this if you only want verified employers to check your certificate."
            />
          </div>

          {/* Toggle 3: Notify on verification */}
          <div className="py-4">
            <ToggleSwitch
              checked={draft.notifyOnVerification}
              onChange={toggle('notifyOnVerification')}
              label={
                <span className="flex items-center gap-2 font-semibold text-sm text-[var(--text-primary)]">
                  <Bell className="h-4 w-4 text-blue-500" />
                  Notify me when verified
                </span>
              }
              description="Receive an email and notification every time someone verifies this certificate."
            />

            {/* Sub-toggle: only shown when notify is ON */}
            {draft.notifyOnVerification && (
              <div className="mt-3 ml-5 pl-4 border-l-2 border-[var(--border)]">
                <ToggleSwitch
                  checked={draft.notifyOnAnonymousOnly}
                  onChange={toggle('notifyOnAnonymousOnly')}
                  label={
                    <span className="flex items-center gap-2 text-sm font-medium text-[var(--text-secondary)]">
                      <BellOff className="h-3.5 w-3.5 text-[var(--text-muted)]" />
                      Only notify me for anonymous verifications
                    </span>
                  }
                  description="Skip notifications when a known verifier checks your certificate."
                />
              </div>
            )}
          </div>
        </div>

        {/* Save button */}
        <div className="flex justify-end">
          <Button onClick={handleSave} loading={saving} className="gap-2">
            <Settings2 className="h-4 w-4" />
            Save Settings
          </Button>
        </div>

        {/* Footer note */}
        <div className="flex items-start gap-2 rounded-lg bg-[var(--bg-elevated)] border border-[var(--border)] px-3 py-2.5">
          <Info className="h-3.5 w-3.5 mt-0.5 shrink-0 text-[var(--text-muted)]" />
          <p className="text-xs text-[var(--text-muted)]">
            These settings apply to this certificate only.
            Set defaults for new certificates in your{' '}
            <a href="/settings?tab=general" className="underline hover:text-[var(--brand)] transition-colors">
              account settings
            </a>.
          </p>
        </div>
      </div>
    </Modal>
  );
}
