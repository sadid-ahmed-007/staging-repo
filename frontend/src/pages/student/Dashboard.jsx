import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Award, Globe, Shield, Clock, CheckCircle, GraduationCap, ArrowRight, Building2, BookOpen } from 'lucide-react';
import DashboardLayout from '../../components/layout/DashboardLayout';
import Card from '../../components/shared/Card';
import StatCard from '../../components/shared/StatCard';
import Badge from '../../components/shared/Badge';
import LoadingSpinner from '../../components/shared/LoadingSpinner';
import EmptyState from '../../components/shared/EmptyState';
import api from '../../services/api';
import { useAuth } from '../../contexts/AuthContext';
import { formatDate } from '../../utils/helpers';

export default function StudentDashboard() {
  const [stats, setStats] = useState(null);
  const [enrollment, setEnrollment] = useState(null);
  const [loading, setLoading] = useState(true);
  const { user } = useAuth();

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        const [dashboardRes, enrollmentRes] = await Promise.all([
          api.get('/student/dashboard').catch(() => ({ data: { data: null } })),
          api.get('/student/enrollment').catch(() => ({ data: { data: null } })),
        ]);

        if (dashboardRes.data?.data) {
          setStats(dashboardRes.data.data);
        }
        if (enrollmentRes.data?.success && enrollmentRes.data?.data) {
          setEnrollment(enrollmentRes.data.data);
        } else {
          setEnrollment(null);
        }
      } catch (error) {
        console.error('Failed to fetch dashboard data:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

  if (loading) {
    return (
      <DashboardLayout>
        <div className="flex min-h-[50vh] items-center justify-center">
          <LoadingSpinner />
        </div>
      </DashboardLayout>
    );
  }

  const isEnrolled = enrollment && (enrollment.status === 'active' || enrollment.status === 'withdrawal_requested');

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-xl font-semibold text-[var(--text-primary)]">
            Student Dashboard
          </h1>
          <p className="text-sm text-[var(--text-secondary)] mt-1">
            Welcome back{user?.name ? `, ${user.name}` : ''}
          </p>
        </div>

        {/* Stat Cards */}
        <div className="grid gap-6 grid-cols-1 md:grid-cols-2 lg:grid-cols-5">
          <StatCard 
            icon={<Award className="h-5 w-5 text-blue-500" />} 
            label="Total Certificates" 
            value={stats?.totalCertificates ?? 0} 
            color="blue"
            to="/student/certificates?visibility=all"
          />
          <StatCard 
            icon={<Globe className="h-5 w-5 text-green-500" />} 
            label="Public Certificates" 
            value={stats?.publicCertificates ?? 0} 
            color="green"
            to="/student/certificates?visibility=public"
          />
          <StatCard 
            icon={<Shield className="h-5 w-5 text-gray-500" />} 
            label="Private Certificates" 
            value={stats?.privateCertificates ?? 0} 
            color="gray"
            to="/student/certificates?visibility=private"
          />
          <StatCard 
            icon={<Clock className="h-5 w-5 text-yellow-500" />} 
            label="Pending Requests" 
            value={stats?.pendingAccessRequests ?? 0} 
            color="yellow"
            to="/student/access-requests?tab=pending"
          />
          <StatCard 
            icon={<CheckCircle className="h-5 w-5 text-blue-500" />} 
            label="Active Grants" 
            value={stats?.activeAccessGrants ?? 0} 
            color="blue"
            to="/student/access-requests?tab=grants"
          />
        </div>

        {/* Current Enrollment Card */}
        <div className="grid gap-6 grid-cols-1">
          <Card>
            <div className="flex items-center justify-between mb-5">
              <h2 className="text-base font-semibold text-[var(--text-primary)] flex items-center gap-2">
                <GraduationCap className="h-5 w-5 text-[var(--brand)]" />
                Current Enrollment
              </h2>
            </div>

            {isEnrolled ? (
              <div className="rounded-xl border border-[var(--border)] bg-[var(--bg-elevated)]/30 p-5 transition-all">
                <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
                  <div className="space-y-1">
                    <div className="flex flex-wrap items-center gap-3">
                      <h3 className="text-xl font-bold text-[var(--text-primary)]">
                        {enrollment.institutionName}
                      </h3>
                      {enrollment.enrollmentNumber && (
                        <span className="rounded border border-[var(--brand)]/20 bg-[var(--brand-light)]/40 px-2 py-0.5 font-mono text-xs font-medium text-[var(--brand)] dark:bg-[var(--brand-light)]/10">
                          {enrollment.enrollmentNumber}
                        </span>
                      )}
                    </div>
                    <p className="text-sm font-medium text-[var(--text-secondary)]">
                      {enrollment.program} — {enrollment.department}
                      {enrollment.major ? ` (${enrollment.major})` : ''}
                    </p>
                    <p className="text-xs text-[var(--text-muted)]">
                      Batch: <span className="font-medium text-[var(--text-secondary)]">{enrollment.batch || 'N/A'}</span>
                      {enrollment.studentIdInUniversity && ` • ID: ${enrollment.studentIdInUniversity}`}
                    </p>
                  </div>

                  {/* Status badge */}
                  <div>
                    {enrollment.status === 'active' ? (
                      <Badge variant="success" dot size="md">
                        Active
                      </Badge>
                    ) : enrollment.status === 'withdrawal_requested' ? (
                      <span className="inline-flex items-center gap-1.5 rounded-full border border-amber-300 bg-amber-50 px-3 py-1 text-xs font-semibold text-amber-700 dark:border-amber-800/40 dark:bg-amber-950/40 dark:text-amber-300">
                        <span className="relative flex h-2 w-2">
                          <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-amber-400 opacity-75"></span>
                          <span className="relative inline-flex h-2 w-2 rounded-full bg-amber-500"></span>
                        </span>
                        Withdrawal Pending
                      </span>
                    ) : (
                      <Badge variant="default" dot size="md">
                        {enrollment.status?.toUpperCase()}
                      </Badge>
                    )}
                  </div>
                </div>

                <div className="mt-4 pt-4 border-t border-[var(--border)] flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs text-[var(--text-secondary)]">
                  <div>
                    <span>Expected Graduation: </span>
                    <span className="font-semibold text-[var(--text-primary)]">
                      {formatDate(enrollment.expectedGraduationDate)}
                    </span>
                  </div>
                  <Link
                    to="/student/my-university"
                    className="inline-flex items-center text-xs font-semibold text-[var(--brand)] hover:underline"
                  >
                    View Full Details
                    <ArrowRight className="ml-1 h-3.5 w-3.5" />
                  </Link>
                </div>
              </div>
            ) : (
              <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-[var(--border)] bg-[var(--bg-surface)] p-8 text-center">
                <GraduationCap className="h-12 w-12 text-[var(--text-muted)]" />
                <h3 className="mt-3 text-base font-semibold text-[var(--text-primary)]">
                  Not currently enrolled
                </h3>
                <p className="mt-1 max-w-sm text-xs text-[var(--text-secondary)]">
                  Contact a university to get enrolled.
                </p>
              </div>
            )}
          </Card>
        </div>
      </div>
    </DashboardLayout>
  );
}
