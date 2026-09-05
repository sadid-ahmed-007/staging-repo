import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import toast from 'react-hot-toast';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/shared/Card';
import Badge from '../../components/shared/Badge';
import Button from '../../components/shared/Button';
import LoadingSpinner from '../../components/shared/LoadingSpinner';
import ErrorMessage from '../../components/shared/ErrorMessage';
import EmptyState from '../../components/shared/EmptyState';
import api from '../../services/api';
import { formatDate } from '../../utils/helpers';
import { downloadCertificatePDF, previewCertificatePDF } from '../../services/certificateService';
import { FileText, ArrowLeft, Search, Lock, Download, ShieldCheck, Clock, Loader2, Eye } from 'lucide-react';

export default function VerifierAccessibleCertificates() {
  const navigate = useNavigate();
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  // Selected Student View State
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [studentCertificates, setStudentCertificates] = useState([]);
  const [certsLoading, setCertsLoading] = useState(false);
  const [downloadingId, setDownloadingId] = useState(null);

  const fetchStudents = useCallback(async () => {
    setLoading(true);
    try {
      setError('');
      const { data } = await api.get('/verifier/accessible-certificates');
      if (data.success) {
        setStudents(data.data || []);
      } else {
        setError('Failed to load accessible students');
      }
    } catch (err) {
      console.error('Error fetching students:', err);
      setError(err.response?.data?.message || 'Failed to load accessible students');
      toast.error('Failed to load accessible students');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchStudents();
  }, [fetchStudents]);

  const handleSelectStudent = async (student) => {
    setSelectedStudent(student);
    setCertsLoading(true);
    try {
      const { data } = await api.get(`/verifier/accessible-certificates/${student.studentId}`);
      if (data.success) {
        setStudentCertificates(data.data || []);
      } else {
        toast.error('Failed to load certificates for this student');
        setSelectedStudent(null);
      }
    } catch (err) {
      console.error('Failed to fetch student certificates:', err);
      toast.error(err.response?.data?.message || 'Failed to load certificates');
      setSelectedStudent(null);
    } finally {
      setCertsLoading(false);
    }
  };

  const handleBack = () => {
    setSelectedStudent(null);
    setStudentCertificates([]);
  };

  const handleDownloadPdf = async (certificate) => {
    try {
      setDownloadingId(certificate.id);
      await downloadCertificatePDF(certificate.id, certificate.serial, '/verifier/certificates');
    } catch (err) {
      console.error('Failed to download certificate:', err);
      toast.error('Failed to download certificate');
    } finally {
      setDownloadingId(null);
    }
  };

  const handleVerify = (certificate) => {
    navigate(`/verifier/verify-certificate?serial=${certificate.serial}`);
  };

  const getInitials = (name) => {
    if (!name) return 'S';
    const parts = name.trim().split(' ');
    if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    return name.substring(0, 2).toUpperCase();
  };

  const isExpiringSoon = (expiresAt) => {
    if (!expiresAt) return false;
    const diffTime = new Date(expiresAt).getTime() - new Date().getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays > 0 && diffDays <= 7;
  };

  const getDaysUntilExpiry = (expiresAt) => {
    if (!expiresAt) return 0;
    const diffTime = new Date(expiresAt).getTime() - new Date().getTime();
    return Math.max(0, Math.ceil(diffTime / (1000 * 60 * 60 * 24)));
  };

  if (selectedStudent) {
    return (
      <DashboardLayout>
        <div className="space-y-[24px]">
          <div className="flex items-center gap-4">
            <button
              onClick={handleBack}
              className="p-2 rounded-full hover:bg-[var(--bg-elevated)] text-[var(--text-secondary)] transition-colors"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>
            <div>
              <h1 className="text-xl font-semibold text-[var(--text-primary)]">{selectedStudent.studentName}</h1>
              <p className="text-sm text-[var(--text-secondary)] mt-1">
                Certificates accessed via granted permission
              </p>
            </div>
          </div>

          {certsLoading ? (
            <div className="flex min-h-[30vh] items-center justify-center">
              <LoadingSpinner />
            </div>
          ) : studentCertificates.length === 0 ? (
            <EmptyState
              title="No Certificates Found"
              message={`${selectedStudent.studentName} has no active certificates.`}
              icon={FileText}
            />
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-[24px]">
              {studentCertificates.map((cert) => (
                <Card key={cert.id} className="flex flex-col h-full hover:shadow-lg transition-shadow">
                  <div className="flex justify-between items-start mb-4">
                    <Badge variant="primary">{cert.certificateLevel}</Badge>
                    <Badge variant={cert.status === 'revoked' ? 'danger' : 'success'}>
                      {cert.status === 'revoked' ? 'Revoked' : 'Active'}
                    </Badge>
                  </div>

                  <div className="flex-1 space-y-2">
                    <p className="text-sm font-semibold text-[var(--text-primary)]">{cert.institutionName}</p>
                    {cert.certificateName && (
                      <p className="text-sm text-[var(--text-secondary)]">{cert.certificateName}</p>
                    )}
                    <p className="text-xs text-[var(--text-muted)]">
                      Issued: {cert.issueDate ? formatDate(cert.issueDate) : 'N/A'}
                    </p>
                    <p className="font-mono text-xs text-[var(--text-muted)] truncate" title={cert.serial}>
                      Serial: {cert.serial}
                    </p>
                  </div>

                  <div className="mt-4 pt-4 border-t border-[var(--border)] flex justify-between items-center">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleVerify(cert)}
                    >
                      <ShieldCheck className="w-4 h-4 mr-1.5" />
                      Verify
                    </Button>
                    
                    {cert.isPubliclyShareable && (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleDownloadPdf(cert)}
                        disabled={downloadingId === cert.id}
                      >
                        {downloadingId === cert.id ? (
                          <Loader2 className="w-4 h-4 animate-spin" />
                        ) : (
                          <Download className="w-4 h-4" />
                        )}
                      </Button>
                    )}
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div className="space-y-[24px]">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-xl font-semibold text-[var(--text-primary)]">Accessible Certificates</h1>
            <p className="text-sm text-[var(--text-secondary)] mt-1">
              Students who have granted you access to their certificates
            </p>
          </div>
        </div>

        {loading ? (
          <div className="flex min-h-[50vh] items-center justify-center">
            <LoadingSpinner />
          </div>
        ) : error ? (
          <ErrorMessage message={error} retry={fetchStudents} />
        ) : students.length === 0 ? (
          <EmptyState
            title="No accessible certificates yet"
            message="Search for a student and request access to view their certificates"
            icon={Lock}
            action={
              <Button onClick={() => navigate('/verifier/verify-certificate')} variant="primary" className="mt-4">
                <Search className="w-4 h-4 mr-2" />
                Search Students
              </Button>
            }
          />
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-[24px]">
            {students.map((student) => {
              const expiringSoon = isExpiringSoon(student.accessExpiresAt);
              return (
                <Card key={student.studentId} className="flex flex-col hover:shadow-md transition-shadow">
                  <div className="flex items-start gap-4">
                    <div className="w-12 h-12 rounded-full bg-[var(--brand-light)] text-[var(--brand)] flex items-center justify-center font-bold text-lg shrink-0">
                      {getInitials(student.studentName)}
                    </div>
                    <div className="flex-1 min-w-0">
                      <h3 className="font-semibold text-[var(--text-primary)] truncate" title={student.studentName}>
                        {student.studentName}
                      </h3>
                      {student.studentEmail && (
                        <p className="text-sm text-[var(--text-secondary)] truncate">
                          {student.studentEmail}
                        </p>
                      )}
                    </div>
                  </div>

                  <div className="mt-5 space-y-3 flex-1">
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-[var(--text-muted)]">Certificates</span>
                      <span className="font-medium text-[var(--text-primary)]">
                        {student.certificateCount} {student.certificateCount === 1 ? 'certificate' : 'certificates'}
                      </span>
                    </div>
                    
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-[var(--text-muted)]">Access Until</span>
                      {expiringSoon ? (
                        <Badge variant="warning" className="flex items-center gap-1">
                          <Clock className="w-3 h-3" />
                          Expires in {getDaysUntilExpiry(student.accessExpiresAt)} days
                        </Badge>
                      ) : (
                        <span className="font-medium text-[var(--text-primary)]">
                          {formatDate(student.accessExpiresAt)}
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="mt-5 pt-4 border-t border-[var(--border)]">
                    <Button
                      variant="outline"
                      className="w-full justify-center"
                      onClick={() => handleSelectStudent(student)}
                    >
                      <Eye className="w-4 h-4 mr-2" />
                      View Certificates
                    </Button>
                  </div>
                </Card>
              );
            })}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
