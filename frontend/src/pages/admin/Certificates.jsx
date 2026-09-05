import React, { useEffect, useState, useCallback } from 'react';
import toast from 'react-hot-toast';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/shared/Card';
import Badge from '../../components/shared/Badge';
import Button from '../../components/shared/Button';
import LoadingSpinner from '../../components/shared/LoadingSpinner';
import ErrorMessage from '../../components/shared/ErrorMessage';
import EmptyState from '../../components/shared/EmptyState';
import SearchBar from '../../components/shared/SearchBar';
import SelectField from '../../components/shared/SelectField';
import CertificateDetailModal from '../../components/certificates/CertificateDetailModal';
import RevocationModal from '../../components/shared/RevocationModal';
import RestoreModal from '../../components/shared/RestoreModal';
import api from '../../services/api';
import { formatDate } from '../../utils/helpers';
import { FileText, Eye, RefreshCw, ShieldX, RotateCcw } from 'lucide-react';
import { downloadCertificatePDF, previewCertificatePDF } from '../../services/certificateService';

export default function AdminCertificates() {
  const [certificates, setCertificates] = useState([]);
  const [filteredCertificates, setFilteredCertificates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  // Details Modal
  const [selectedCertificate, setSelectedCertificate] = useState(null);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [downloadingId, setDownloadingId] = useState(null);

  // Revoke Modal
  const [revokeTarget, setRevokeTarget] = useState(null);
  const [revokeLoading, setRevokeLoading] = useState(false);

  // Restore Modal
  const [restoreTarget, setRestoreTarget] = useState(null);
  const [restoreLoading, setRestoreLoading] = useState(false);

  // Filters
  const [statusFilter, setStatusFilter] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');

  const fetchCertificates = useCallback(async () => {
    setLoading(true);
    try {
      setError('');
      const { data } = await api.get('/admin/certificates');
      if (data.success) {
        setCertificates(data.data?.content || data.data || []);
      } else {
        setError('Failed to load certificates');
      }
    } catch (err) {
      console.error('Error fetching certificates:', err);
      setError(err.response?.data?.message || 'Failed to load certificates');
      toast.error('Failed to load certificates');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchCertificates();
  }, [fetchCertificates]);

  // Local filtering based on all fetched certificates
  useEffect(() => {
    let result = [...certificates];
    
    // Status filter
    if (statusFilter === 'active') {
      result = result.filter(cert => cert.status !== 'revoked' && !cert.revokedAt);
    } else if (statusFilter === 'revoked') {
      result = result.filter(cert => cert.status === 'revoked' || cert.revokedAt);
    }

    // Search query filter
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      result = result.filter((cert) =>
        (cert.serial && cert.serial.toLowerCase().includes(q)) ||
        (cert.studentName && cert.studentName.toLowerCase().includes(q)) ||
        (cert.institutionName && cert.institutionName.toLowerCase().includes(q))
      );
    }
    
    setFilteredCertificates(result);
  }, [certificates, statusFilter, searchQuery]);

  const openCertificateDetails = async (certificateListObj) => {
    setDetailsLoading(true);
    setSelectedCertificate({ id: certificateListObj.id, loading: true });
    try {
      const { data } = await api.get(`/admin/certificates/${certificateListObj.id}`);
      if (data.success) {
        setSelectedCertificate(data.data);
      }
    } catch (err) {
      console.error('Failed to fetch details', err);
      toast.error('Failed to load certificate details');
      setSelectedCertificate(null);
    } finally {
      setDetailsLoading(false);
    }
  };

  const closeCertificateDetails = () => {
    setSelectedCertificate(null);
  };

  const handleDownloadPdf = async (certificate) => {
    try {
      setDownloadingId(certificate.id);
      await downloadCertificatePDF(certificate.id, certificate.serial, '/admin/certificates');
      toast.success('Certificate downloaded successfully');
    } catch (err) {
      console.error('Failed to download certificate:', err);
      toast.error(err.response?.data?.message || 'Failed to download certificate');
    } finally {
      setDownloadingId(null);
    }
  };

  const handlePreviewPdf = async (certificate) => {
    try {
      await previewCertificatePDF(certificate.id, '/admin/certificates');
      toast.success('Certificate preview opened in a new tab');
    } catch (err) {
      console.error('Failed to preview certificate:', err);
      toast.error(err.response?.data?.message || 'Failed to preview certificate');
    }
  };

  // Revoke Handlers
  const handleRevokeConfirm = async (reason) => {
    if (!revokeTarget) return;
    setRevokeLoading(true);
    try {
      const { data } = await api.post(`/admin/certificates/${revokeTarget.id}/revoke`, { reason });
      if (data.success) {
        toast.success('Certificate revoked');
        setCertificates(prev => prev.map(c => c.id === revokeTarget.id ? { ...c, status: 'revoked', revokedAt: new Date().toISOString() } : c));
        setRevokeTarget(null);
        // Update details modal if it's currently open
        if (selectedCertificate?.id === revokeTarget.id) {
          setSelectedCertificate(prev => ({ ...prev, status: 'revoked', revokedAt: new Date().toISOString() }));
          // Ideally we would refetch the details to get the full revocation history update, 
          // but for simplicity, we just trigger a refetch if it's open.
          openCertificateDetails(revokeTarget);
        }
      }
    } catch (err) {
      toast.error(err.response?.data?.errors?.reason || err.response?.data?.message || 'Failed to revoke certificate');
    } finally {
      setRevokeLoading(false);
    }
  };

  // Restore Handlers
  const handleRestoreConfirm = async (reason) => {
    if (!restoreTarget) return;
    setRestoreLoading(true);
    try {
      const { data } = await api.post(`/admin/certificates/${restoreTarget.id}/restore`, { reason });
      if (data.success) {
        toast.success('Certificate restored');
        setCertificates(prev => prev.map(c => c.id === restoreTarget.id ? { ...c, status: 'active', revokedAt: null } : c));
        setRestoreTarget(null);
        // Update details modal if it's currently open
        if (selectedCertificate?.id === restoreTarget.id) {
          openCertificateDetails(restoreTarget);
        }
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to restore certificate');
    } finally {
      setRestoreLoading(false);
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-[24px]">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.15em] text-[var(--brand)]">Admin</p>
            <h1 className="text-2xl font-bold text-[var(--text-primary)] flex items-center gap-3">
              <FileText className="h-7 w-7 text-[var(--brand)]" />
              All Certificates
            </h1>
            <p className="text-sm text-[var(--text-secondary)] mt-1">
              {loading ? 'Loading...' : `${filteredCertificates.length} of ${certificates.length} certificates`}
            </p>
          </div>
          <div>
            <Button variant="outline" onClick={fetchCertificates} loading={loading}>
              <RefreshCw className="h-4 w-4 mr-2" />
              Refresh
            </Button>
          </div>
        </div>

        {/* Filter Row */}
        <Card className="p-4">
          <div className="flex flex-col md:flex-row gap-4 items-center">
            <SearchBar 
              value={searchQuery} 
              onChange={setSearchQuery} 
              placeholder="Search by serial, student name, or institution..." 
              className="flex-1"
            />
            <div className="w-full md:w-48">
              <SelectField
                value={statusFilter}
                onChange={setStatusFilter}
                options={[
                  { value: 'all', label: 'All Statuses' },
                  { value: 'active', label: 'Active' },
                  { value: 'revoked', label: 'Revoked' },
                ]}
              />
            </div>
          </div>
        </Card>

        {loading ? (
          <div className="flex min-h-[50vh] items-center justify-center">
            <LoadingSpinner />
          </div>
        ) : error ? (
          <ErrorMessage message={error} retry={fetchCertificates} />
        ) : filteredCertificates.length === 0 ? (
          <EmptyState
            title="No Certificates Found"
            message={searchQuery || statusFilter !== 'all' ? `No certificates match your search filters.` : `No certificates found in the registry.`}
            icon={FileText}
          />
        ) : (
          <Card className="overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-sm text-left">
                <thead className="bg-[var(--bg-elevated)] text-[var(--text-secondary)] text-xs uppercase font-semibold">
                  <tr>
                    <th className="px-6 py-4 rounded-tl-xl">Serial</th>
                    <th className="px-6 py-4">Student</th>
                    <th className="px-6 py-4">Institution</th>
                    <th className="px-6 py-4">Level</th>
                    <th className="px-6 py-4">Issue Date</th>
                    <th className="px-6 py-4">Status</th>
                    <th className="px-6 py-4 text-right rounded-tr-xl">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--border)]">
                  {filteredCertificates.map((cert) => {
                    const isRevoked = cert.status === 'revoked' || cert.revokedAt;
                    return (
                      <tr key={cert.id} className="hover:bg-[var(--bg-elevated)] transition-colors">
                        <td className="px-6 py-4">
                          <span className="font-mono text-xs bg-[var(--bg-surface)] border border-[var(--border)] px-2 py-1 rounded">
                            {cert.serial}
                          </span>
                        </td>
                        <td className="px-6 py-4 font-medium text-[var(--text-primary)]">
                          {cert.studentName}
                        </td>
                        <td className="px-6 py-4 text-[var(--text-secondary)] max-w-[200px] truncate" title={cert.institutionName}>
                          {cert.institutionName}
                        </td>
                        <td className="px-6 py-4">
                          <Badge variant="primary">{cert.certificateLevel}</Badge>
                        </td>
                        <td className="px-6 py-4 text-[var(--text-secondary)] whitespace-nowrap">
                          {cert.issueDate ? formatDate(cert.issueDate) : 'N/A'}
                        </td>
                        <td className="px-6 py-4">
                          <Badge variant={isRevoked ? 'danger' : 'success'}>
                            {isRevoked ? 'Revoked' : 'Active'}
                          </Badge>
                        </td>
                        <td className="px-6 py-4 text-right">
                          <div className="flex items-center justify-end gap-2">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => openCertificateDetails(cert)}
                              title="View Details"
                            >
                              <Eye className="h-4 w-4" />
                            </Button>
                            
                            {isRevoked ? (
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => setRestoreTarget(cert)}
                                title="Restore Certificate"
                                className="text-green-600 hover:text-green-700 hover:bg-green-50"
                              >
                                <RotateCcw className="h-4 w-4" />
                              </Button>
                            ) : (
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => setRevokeTarget(cert)}
                                title="Revoke Certificate"
                                className="text-red-600 hover:text-red-700 hover:bg-red-50"
                              >
                                <ShieldX className="h-4 w-4" />
                              </Button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </Card>
        )}
      </div>

      <CertificateDetailModal
        open={!!selectedCertificate}
        onClose={closeCertificateDetails}
        certificate={selectedCertificate}
        loading={detailsLoading}
        downloadingId={downloadingId}
        onDownloadPdf={handleDownloadPdf}
        onPreviewPdf={handlePreviewPdf}
        role="admin"
      />

      <RevocationModal
        open={!!revokeTarget}
        onClose={() => setRevokeTarget(null)}
        onConfirm={handleRevokeConfirm}
        loading={revokeLoading}
        certificate={revokeTarget}
      />

      <RestoreModal
        open={!!restoreTarget}
        onClose={() => setRestoreTarget(null)}
        onConfirm={handleRestoreConfirm}
        loading={restoreLoading}
        certificate={restoreTarget}
      />
    </DashboardLayout>
  );
}
