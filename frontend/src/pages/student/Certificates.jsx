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
import { formatDate } from '../../utils/helpers';
import { downloadCertificatePDF, previewCertificatePDF } from '../../services/certificateService';
import { FileText, Download, RefreshCw, Loader2 } from 'lucide-react';

export default function StudentCertificates() {
  const [certificates, setCertificates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [downloadingId, setDownloadingId] = useState(null);
  const [selectedCertificate, setSelectedCertificate] = useState(null);
  const [detailsLoading, setDetailsLoading] = useState(false);
  
  const [copiedSerial, setCopiedSerial] = useState(null);
  const [copiedLink, setCopiedLink] = useState(false);

  // Status filter and search
  const [statusFilter, setStatusFilter] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');

  const filteredCertificates = certificates.filter((cert) => {
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
      const { data } = await api.get('/student/certificates');
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
      toast.success('Certificate downloaded successfully');
    } catch (err) {
      console.error('Failed to download certificate:', err);
      let errorMsg = 'Failed to download certificate';
      if (err?.response?.data instanceof Blob) {
        try {
          const text = await err.response.data.text();
          const json = JSON.parse(text);
          errorMsg = json.error || json.message || errorMsg;
        } catch (e) {
          errorMsg = err.message;
        }
      } else {
        errorMsg = err?.response?.data?.message || err.message || errorMsg;
      }
      toast.error(errorMsg);
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
      let errorMsg = 'Failed to preview certificate';
      if (err?.response?.data instanceof Blob) {
        try {
          const text = await err.response.data.text();
          const json = JSON.parse(text);
          errorMsg = json.error || json.message || errorMsg;
        } catch (e) {
          errorMsg = err.message;
        }
      } else {
        errorMsg = err?.response?.data?.message || err.message || errorMsg;
      }
      toast.error(errorMsg);
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
    try {
      const newStatus = !currentStatus;
      const { data } = await api.patch(`/student/certificates/${certificateId}/visibility`, {
        isPubliclyShareable: newStatus
      });
      if (data.success) {
        setCertificates((current) =>
          current.map((cert) =>
            cert.id === certificateId ? { ...cert, isPubliclyShareable: newStatus } : cert
          )
        );
        toast.success(`Certificate marked as ${newStatus ? 'public' : 'private'}`);
      }
    } catch (err) {
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

        {/* Filter Row */}
        <div className="flex flex-col sm:flex-row gap-4 items-center">
          <SearchBar 
            value={searchQuery} 
            onChange={setSearchQuery} 
            placeholder="Search by serial, level, or institution..." 
            className="flex-1"
          />
          <div className="w-full sm:w-48">
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
        </div>

        {loading ? (
          <div className="flex min-h-[50vh] items-center justify-center">
            <LoadingSpinner />
          </div>
        ) : error ? (
          <ErrorMessage message={error} retry={fetchCertificates} />
        ) : filteredCertificates.length === 0 ? (
          <EmptyState
            title="No Certificates Found"
            message={searchQuery.trim() ? `No certificates match your search.` : `You don't have any certificates issued yet.`}
            icon={FileText}
          />
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-[24px]">
            {filteredCertificates.map((certificate) => (
              <Card key={certificate.id} className="flex flex-col h-full hover:shadow-lg transition-shadow">
                {/* Top */}
                <div className="flex justify-between items-start mb-4">
                  <Badge variant="primary">{certificate.certificateLevel}</Badge>
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
