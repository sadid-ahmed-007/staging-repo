import { useEffect, useState } from 'react';
import { Award, Globe, Shield, Clock, CheckCircle, GraduationCap } from 'lucide-react';
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
  const [loading, setLoading] = useState(true);
  const { user } = useAuth();

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const { data } = await api.get('/student/dashboard');
        setStats(data.data);
      } catch (error) {
        console.error('Failed to fetch dashboard:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchDashboard();
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

  const enrollment = stats?.currentEnrollment;

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

        <div className="grid gap-6 grid-cols-1 md:grid-cols-2 lg:grid-cols-5">
          <StatCard 
            icon={<Award className="h-5 w-5 text-blue-500" />} 
            label="Total Certificates" 
            value={stats?.totalCertificates ?? 0} 
            color="blue"
          />
          <StatCard 
            icon={<Globe className="h-5 w-5 text-green-500" />} 
            label="Public Certificates" 
            value={stats?.publicCertificates ?? 0} 
            color="green"
          />
          <StatCard 
            icon={<Shield className="h-5 w-5 text-gray-500" />} 
            label="Private Certificates" 
            value={stats?.privateCertificates ?? 0} 
            color="gray"
          />
          <StatCard 
            icon={<Clock className="h-5 w-5 text-yellow-500" />} 
            label="Pending Requests" 
            value={stats?.pendingAccessRequests ?? 0} 
            color="yellow"
          />
          <StatCard 
            icon={<CheckCircle className="h-5 w-5 text-blue-500" />} 
            label="Active Grants" 
            value={stats?.activeAccessGrants ?? 0} 
            color="blue"
          />
        </div>

        <div className="grid gap-6 grid-cols-1">
          <Card>
            <h2 className="text-lg font-semibold text-[var(--text-primary)] mb-6 flex items-center gap-2">
              <GraduationCap className="h-5 w-5" /> Current Enrollment
            </h2>
            {enrollment ? (
              <div className="flex flex-col md:flex-row justify-between border rounded-xl p-5 border-[var(--border)]">
                <div>
                  <h3 className="text-lg font-bold text-[var(--text-primary)]">{enrollment.institutionName}</h3>
                  <p className="text-[var(--text-secondary)] mt-1">{enrollment.program} (Batch {enrollment.batch})</p>
                  <p className="text-sm text-[var(--text-muted)] mt-2">
                    Enrolled: {formatDate(enrollment.enrollmentDate)} | 
                    Expected Graduation: {formatDate(enrollment.expectedGraduationDate)}
                  </p>
                </div>
                <div className="mt-4 md:mt-0 flex items-start">
                  <Badge variant={enrollment.status === 'active' ? 'success' : 'warning'}>
                    {enrollment.status.toUpperCase()}
                  </Badge>
                </div>
              </div>
            ) : (
              <EmptyState title="Not currently enrolled" message="You don't have an active enrollment at any institution." />
            )}
          </Card>
        </div>
      </div>
    </DashboardLayout>
  );
}
