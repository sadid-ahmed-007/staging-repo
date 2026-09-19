import { useEffect, useState, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import { QRCodeSVG } from 'qrcode.react';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/shared/Card';
import Badge from '../../components/shared/Badge';
import Button from '../../components/shared/Button';
import Modal from '../../components/shared/Modal';
import LoadingSpinner from '../../components/shared/LoadingSpinner';
import ErrorMessage from '../../components/shared/ErrorMessage';
import EmptyState from '../../components/shared/EmptyState';
import SearchBar from '../../components/shared/SearchBar';
import SelectField from '../../components/shared/SelectField';
import ToggleSwitch from '../../components/shared/ToggleSwitch';
import CertificateDetailModal from '../../components/certificates/CertificateDetailModal';
import api from '../../services/api';
import { formatDate, cn } from '../../utils/helpers';
import { downloadCertificatePDF, previewCertificatePDF } from '../../services/certificateService';
import { FileText, Download, RefreshCw, Loader2, Globe, Shield, Award, X } from 'lucide-react';

export default function StudentCertificates() {
  const [certificates, setCertificates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [downloadingId, setDownloadingId] = useState(null);
  const [selectedCertificate, setSelectedCertificate] = useState(null);
  const [detailsLoading, setDetailsLoading] = useState(false);
  
  const [copiedSerial, setCopiedSerial] = useState(null);
  const [copiedLink, setCopiedLink] = useState(false);

  // URL search params sync
  const [searchParams, setSearchParams] = useSearchParams();
  const visibilityParam = searchParams.get('visibility') || searchParams.get('filter');
  const [visibilityFilter, setVisibilityFilter] = useState(
    ['all', 'public', 'private'].includes(visibilityParam?.toLowerCase())
      ? visibilityParam.toLowerCase()
      : 'all'
  );

  // Status filter and search
  const [statusFilter, setStatusFilter] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');

  // Keep state synced with URL changes (e.g. back/forward or clicking dashboard links)
  useEffect(() => {
    const param = searchParams.get('visibility') || searchParams.get('filter');
    if (param && ['all', 'public', 'private'].includes(param.toLowerCase())) {
      setVisibilityFilter(param.toLowerCase());
    } else if (!param) {
      setVisibilityFilter('all');
    }
  }, [searchParams]);

  const handleVisibilityChange = (newVisibility) => {
    setVisibilityFilter(newVisibility);
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      if (newVisibility === 'all') {
        next.delete('visibility');
        next.delete('filter');
      } else {
        next.set('visibility', newVisibility);
        next.delete('filter');
      }
      return next;
    });
  };

  // Certificate counts for convenient tabs
  const totalCount = certificates.length;
  const publicCount = certificates.filter((c) => Boolean(c.isPubliclyShareable)).length;
  const privateCount = certificates.filter((c) => !c.isPubliclyShareable).length;

  const filteredCertificates = certificates.filter((cert) => {
    // Apply visibility filter
    if (visibilityFilter === 'public' && !cert.isPubliclyShareable) return false;
    if (visibilityFilter === 'private' && cert.isPubliclyShareable) return false;

    // Apply status filter
    if (statusFilter === 'active' && cert.status === 'revoked') return false;
    if (statusFilter === 'revoked' && cert.status !== 'revoked') return false;

    // Apply search query filter
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const matchesSearch = 
        (cert.serial && cert.serial.toLowerCase().includes(q)) ||
        (cert.certificateLevel && cert.certificateLevel.toLowerCase().includes(q)) ||
        (cert.certificateName && cert.certificateName.toLowerCase().includes(q)) ||
        (cert.institutionName && cert.institutionName.toLowerCase().includes(q)) ||
        (cert.issueDate && formatDate(cert.issueDate).toLowerCase().includes(q));
      
      if (!matchesSearch) return false;
    }
    
    return true;
  });

  const fetchCertificates = useCallback(async () => {
    setLoading(true);
    try {
      setError('');
      const { data } = await api.get('/student/certificates', { params: { size: 100 } });
      if (data.success) {
        setCertificates(data.data || []);
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

  const handleDownloadPdf = async (certificate) => {
    try {
      setDownloadingId(certificate.id);
      await downloadCertificatePDF(certificate.id, certificate.serial, '/student/certificates');
      toast.success('Certificate downloaded');
    } catch (err) {
      console.error('Failed to download certificate:', err);
      toast.error('Failed to download certificate. Try again.');
    } finally {
      setDownloadingId(null);
    }
  };

  const handlePreviewPdf = async (certificate) => {
    try {
      await previewCertificatePDF(certificate.id, '/student/certificates');
      toast.success('Certificate preview opened in a new tab');
    } catch (err) {
      console.error('Failed to preview certificate:', err);
      toast.error('Failed to preview certificate');
    }
  };

  const openCertificateDetails = async (certificateListObj) => {
    setDetailsLoading(true);
    // Show a skeleton or loading state in modal by setting an empty selectedCertificate first
    setSelectedCertificate({ id: certificateListObj.id, loading: true });
    try {
      const { data } = await api.get(`/student/certificates/${certificateListObj.id}`);
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

  const toggleVisibility = async (certificateId, currentStatus) => {
    const newStatus = !currentStatus;

    // Immediate optimistic UI update
    setCertificates((current) =>
      current.map((cert) =>
        cert.id === certificateId ? { ...cert, isPubliclyShareable: newStatus } : cert
      )
    );
    setSelectedCertificate((prev) =>
      prev && prev.id === certificateId ? { ...prev, isPubliclyShareable: newStatus } : prev
    );

    // API call in background
    try {
      const { data } = await api.patch(`/student/certificates/${certificateId}/visibility`, {
        isPubliclyShareable: newStatus
      });
      if (data.success) {
        toast.success(`Certificate set to ${newStatus ? 'Public' : 'Private'}`);
      }
    } catch (err) {
      // Revert the toggle on failure + show error toast
      setCertificates((current) =>
        current.map((cert) =>
          cert.id === certificateId ? { ...cert, isPubliclyShareable: currentStatus } : cert
        )
      );
      setSelectedCertificate((prev) =>
        prev && prev.id === certificateId ? { ...prev, isPubliclyShareable: currentStatus } : prev
      );
      console.error('Failed to toggle visibility:', err);
      toast.error(err.response?.data?.message || 'Failed to update certificate visibility');
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-[24px]">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-xl font-semibold text-[var(--text-primary)]">My Certificates</h1>
            <p className="text-sm text-[var(--text-secondary)] mt-1">Manage your academic certificates and control who can view them.</p>
          </div>
          <div>
            <Button variant="outline" onClick={fetchCertificates} loading={loading} aria-label="Refresh certificates">
              <RefreshCw className="h-4 w-4 mr-2" />
              Refresh
            </Button>
          </div>
        </div>

        {/* Visibility / Filter Tabs */}
        <div className="flex border-b border-[var(--border)] gap-2 sm:gap-6 overflow-x-auto pb-px scrollbar-hide">
          <button
            type="button"
            onClick={() => handleVisibilityChange('all')}
            className={cn(
              "pb-3 text-sm font-medium transition-colors border-b-2 flex items-center gap-2 whitespace-nowrap",
              visibilityFilter === 'all'
                ? "border-[var(--brand)] text-[var(--brand)] font-semibold"
                : "border-transparent text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:border-[var(--border)]"
            )}
          >
            <Award className="h-4 w-4" />
            <span>All Certificates</span>
            <span className={cn(
              "ml-1 text-xs px-2 py-0.5 rounded-full font-medium transition-colors",
              visibilityFilter === 'all'
                ? "bg-[var(--brand-light)]/20 text-[var(--brand)] dark:bg-[var(--brand-light)]/10"
                : "bg-[var(--bg-elevated)] text-[var(--text-muted)]"
            )}>
              {totalCount}
            </span>
          </button>

          <button
            type="button"
            onClick={() => handleVisibilityChange('public')}
            className={cn(
              "pb-3 text-sm font-medium transition-colors border-b-2 flex items-center gap-2 whitespace-nowrap",
              visibilityFilter === 'public'
                ? "border-green-500 text-green-600 dark:text-green-400 font-semibold"
                : "border-transparent text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:border-[var(--border)]"
            )}
          >
            <Globe className="h-4 w-4 text-green-500" />
            <span>Public Certificates</span>
            <span className={cn(
              "ml-1 text-xs px-2 py-0.5 rounded-full font-medium transition-colors",
              visibilityFilter === 'public'
                ? "bg-green-100 text-green-700 dark:bg-green-950/50 dark:text-green-300"
                : "bg-[var(--bg-elevated)] text-[var(--text-muted)]"
            )}>
              {publicCount}
            </span>
          </button>

          <button
            type="button"
            onClick={() => handleVisibilityChange('private')}
            className={cn(
              "pb-3 text-sm font-medium transition-colors border-b-2 flex items-center gap-2 whitespace-nowrap",
              visibilityFilter === 'private'
                ? "border-gray-500 text-gray-800 dark:text-gray-200 font-semibold"
                : "border-transparent text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:border-[var(--border)]"
            )}
          >
            <Shield className="h-4 w-4 text-gray-500" />
            <span>Private Certificates</span>
            <span className={cn(
              "ml-1 text-xs px-2 py-0.5 rounded-full font-medium transition-colors",
              visibilityFilter === 'private'
                ? "bg-gray-200 text-gray-800 dark:bg-gray-800 dark:text-gray-200"
                : "bg-[var(--bg-elevated)] text-[var(--text-muted)]"
            )}>
              {privateCount}
            </span>
          </button>
        </div>

        {/* Filter Row */}
        <div className="flex flex-col sm:flex-row gap-4 items-center">
          <SearchBar 
            value={searchQuery} 
            onChange={setSearchQuery} 
            placeholder="Search by serial, level, or institution..." 
            className="flex-1"
          />
          <div className="w-full sm:w-44">
            <SelectField
              value={visibilityFilter}
              onChange={handleVisibilityChange}
              options={[
                { value: 'all', label: 'All Visibility' },
                { value: 'public', label: 'Public Only' },
                { value: 'private', label: 'Private Only' },
              ]}
            />
          </div>
          <div className="w-full sm:w-40">
            <SelectField
              value={statusFilter}
              onChange={setStatusFilter}
              options={[
                { value: 'all', label: 'All Status' },
                { value: 'active', label: 'Active' },
                { value: 'revoked', label: 'Revoked' },
              ]}
            />
          </div>
          {(visibilityFilter !== 'all' || statusFilter !== 'all' || searchQuery.trim()) && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                handleVisibilityChange('all');
                setStatusFilter('all');
                setSearchQuery('');
              }}
              className="text-xs text-[var(--text-muted)] hover:text-[var(--text-primary)] shrink-0"
            >
              <X className="w-3.5 h-3.5 mr-1" />
              Reset
            </Button>
          )}
        </div>

        {loading ? (
          <div className="flex min-h-[50vh] items-center justify-center">
            <LoadingSpinner />
          </div>
        ) : error ? (
          <ErrorMessage message={error} retry={fetchCertificates} />
        ) : filteredCertificates.length === 0 ? (
          <EmptyState
            title={certificates.length === 0 ? "No certificates yet" : "No certificates match filters"}
            description={
              certificates.length === 0 
                ? "You have not been issued any certificates yet." 
                : `No certificates match your selected ${visibilityFilter !== 'all' ? `"${visibilityFilter}"` : ''} filter options.`
            }
            icon={FileText}
            action={
              certificates.length > 0 ? (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    handleVisibilityChange('all');
                    setStatusFilter('all');
                    setSearchQuery('');
                  }}
                >
                  Clear all filters
                </Button>
              ) : null
            }
          />
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-[24px]">
            {filteredCertificates.map((certificate) => (
              <Card key={certificate.id} className="flex flex-col h-full hover:shadow-lg transition-shadow">
                {/* Top */}
                <div className="flex justify-between items-start mb-4">
                  <div className="flex items-center gap-1.5 flex-wrap">
                    <Badge variant="primary">{certificate.certificateLevel}</Badge>
                    <span className={cn(
                      "inline-flex items-center gap-1 text-xs px-2 py-0.5 rounded-full font-medium",
                      certificate.isPubliclyShareable
                        ? "bg-green-100 text-green-700 dark:bg-green-950/40 dark:text-green-300 border border-green-200 dark:border-green-800/40"
                        : "bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300 border border-gray-200 dark:border-gray-700"
                    )}>
                      {certificate.isPubliclyShareable ? (
                        <>
                          <Globe className="w-3 h-3 text-green-600 dark:text-green-400" />
                          Public
                        </>
                      ) : (
                        <>
                          <Shield className="w-3 h-3 text-gray-500" />
                          Private
                        </>
                      )}
                    </span>
                  </div>
                  <Badge variant={certificate.status === 'revoked' ? 'danger' : 'success'}>
                    {certificate.status === 'revoked' ? 'Revoked' : 'Active'}
                  </Badge>
                </div>

                {/* Body */}
                <div className="flex-1 space-y-2 cursor-pointer" onClick={() => openCertificateDetails(certificate)}>
                  <p className="text-sm font-semibold text-[var(--text-primary)]">{certificate.institutionName}</p>
                  {certificate.certificateName && (
                    <p className="text-sm text-[var(--text-secondary)]">{certificate.certificateName}</p>
                  )}
                  <p className="text-xs text-[var(--text-muted)]">
                    Issued: {certificate.issueDate ? formatDate(certificate.issueDate) : 'N/A'}
                  </p>
                  <p className="font-mono text-xs text-[var(--text-muted)] truncate" title={certificate.serial}>
                    Serial: {certificate.serial}
                  </p>
                </div>

                {/* Footer */}
                <div className="mt-4 pt-4 border-t border-[var(--border)] flex justify-between items-center">
                  <div className="flex items-center">
                    <ToggleSwitch
                      checked={certificate.isPubliclyShareable}
                      onChange={() => toggleVisibility(certificate.id, certificate.isPubliclyShareable)}
                      label="Public"
                    />
                  </div>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleDownloadPdf(certificate)}
                    disabled={downloadingId === certificate.id}
                  >
                    {downloadingId === certificate.id ? (
                      <Loader2 className="w-4 h-4 animate-spin" />
                    ) : (
                      <Download className="w-4 h-4" />
                    )}
                  </Button>
                </div>
              </Card>
            ))}
          </div>
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
        role="student"
      />
    </DashboardLayout>
  );
}
