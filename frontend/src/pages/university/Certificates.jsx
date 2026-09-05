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
import api from '../../services/api';
import { formatDate } from '../../utils/helpers';
import { downloadCertificatePDF, previewCertificatePDF } from '../../services/certificateService';
import { FileText, Eye, Download, RefreshCw, Award, Loader2 } from 'lucide-react';

export default function UniversityCertificates() {
  const [certificates, setCertificates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [downloadingId, setDownloadingId] = useState(null);
  
  // Details Modal
  const [selectedCertificate, setSelectedCertificate] = useState(null);
  const [detailsLoading, setDetailsLoading] = useState(false);

  // Filters
  const [levelFilter, setLevelFilter] = useState('all');
  const [monthFilter, setMonthFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // Local filtering based on all fetched certificates
  const filteredCertificates = certificates.filter((cert) => {
    // Level filter
    if (levelFilter !== 'all') {
      const matchLevel = cert.certificateLevel?.toLowerCase() === levelFilter.toLowerCase();
      if (!matchLevel) return false;
    }

    // Month filter (issueDate)
    if (monthFilter && cert.issueDate) {
      const certMonth = cert.issueDate.substring(0, 7); // yyyy-mm
      if (certMonth !== monthFilter) return false;
    }

    // Search query filter
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const matchesSearch = 
        (cert.serial && cert.serial.toLowerCase().includes(q)) ||
        (cert.studentName && cert.studentName.toLowerCase().includes(q)) ||
        (cert.certificateName && cert.certificateName.toLowerCase().includes(q));
      
      if (!matchesSearch) return false;
    }
    
    return true;
  });

  const fetchCertificates = useCallback(async () => {
    setLoading(true);
    try {
      setError('');
      const { data } = await api.get('/university/certificates');
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

  const handleDownloadPdf = async (certificate) => {
    try {
      setDownloadingId(certificate.id);
      await downloadCertificatePDF(certificate.id, certificate.serial, '/university/certificates');
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
      await previewCertificatePDF(certificate.id, '/university/certificates');
      toast.success('Certificate preview opened in a new tab');
    } catch (err) {
      console.error('Failed to preview certificate:', err);
      toast.error(err.response?.data?.message || 'Failed to preview certificate');
    }
  };

  const openCertificateDetails = async (certificateListObj) => {
    setDetailsLoading(true);
    setSelectedCertificate({ id: certificateListObj.id, loading: true });
    try {
      const { data } = await api.get(`/university/certificates/${certificateListObj.id}`);
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

  return (
    <DashboardLayout>
      <div className="space-y-[24px]">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-xl font-semibold text-[var(--text-primary)]">Issued Certificates</h1>
            <p className="text-sm text-[var(--text-secondary)] mt-1">
              {loading ? 'Loading...' : `You have issued ${certificates.length} certificates in total.`}
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
              placeholder="Search by student name, serial, or program..." 
              className="flex-1"
            />
            <div className="w-full md:w-48">
              <SelectField
                value={levelFilter}
                onChange={setLevelFilter}
                options={[
                  { value: 'all', label: 'All Levels' },
                  { value: 'Bachelor', label: 'Bachelor' },
                  { value: 'Master', label: 'Master' },
                  { value: 'PhD', label: 'PhD' },
                ]}
              />
            </div>
            <div className="w-full md:w-48">
              <input
                type="month"
                value={monthFilter}
                onChange={(e) => setMonthFilter(e.target.value)}
                className="w-full rounded-xl border border-[var(--border)] bg-[var(--bg-surface)] px-4 py-[9px] text-sm text-[var(--text-primary)] focus:border-[var(--brand)] focus:outline-none focus:ring-1 focus:ring-[var(--brand)]"
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
            message={searchQuery || levelFilter !== 'all' || monthFilter ? `No certificates match your search filters.` : `You haven't issued any certificates yet.`}
            icon={Award}
          />
        ) : (
          <Card className="overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-sm text-left">
                <thead className="bg-[var(--bg-elevated)] text-[var(--text-secondary)] text-xs uppercase font-semibold">
                  <tr>
                    <th className="px-6 py-4 rounded-tl-xl">Serial</th>
                    <th className="px-6 py-4">Student Name</th>
                    <th className="px-6 py-4">Certificate</th>
                    <th className="px-6 py-4">Level</th>
                    <th className="px-6 py-4">CGPA</th>
                    <th className="px-6 py-4">Issue Date</th>
                    <th className="px-6 py-4">Status</th>
                    <th className="px-6 py-4 text-right rounded-tr-xl">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[var(--border)]">
                  {filteredCertificates.map((cert) => (
                    <tr key={cert.id} className="hover:bg-[var(--bg-elevated)] transition-colors">
                      <td className="px-6 py-4">
                        <span className="font-mono text-xs bg-[var(--bg-surface)] border border-[var(--border)] px-2 py-1 rounded">
                          {cert.serial}
                        </span>
                      </td>
                      <td className="px-6 py-4 font-medium text-[var(--text-primary)]">
                        {cert.studentName}
                      </td>
                      <td className="px-6 py-4 text-[var(--text-secondary)] max-w-xs truncate" title={cert.certificateName}>
                        {cert.certificateName}
                      </td>
                      <td className="px-6 py-4">
                        <Badge variant="primary">{cert.certificateLevel}</Badge>
                      </td>
                      <td className="px-6 py-4 text-[var(--text-primary)] font-medium">
                        {cert.cgpa}
                      </td>
                      <td className="px-6 py-4 text-[var(--text-secondary)] whitespace-nowrap">
                        {cert.issueDate ? formatDate(cert.issueDate) : 'N/A'}
                      </td>
                      <td className="px-6 py-4">
                        <Badge variant={cert.status === 'revoked' ? 'danger' : 'success'}>
                          {cert.status === 'revoked' ? 'Revoked' : 'Active'}
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
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleDownloadPdf(cert)}
                            disabled={downloadingId === cert.id}
                            title="Download PDF"
                          >
                            {downloadingId === cert.id ? (
                              <Loader2 className="h-4 w-4 animate-spin" />
                            ) : (
                              <Download className="h-4 w-4" />
                            )}
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
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
        role="university"
      />
    </DashboardLayout>
  );
}
