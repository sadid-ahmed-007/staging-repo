import { useEffect, useState } from 'react';
import { Users, GraduationCap, Award, AlertTriangle, Calendar } from 'lucide-react';
import DashboardLayout from '../../components/layout/DashboardLayout';
import StatCard from '../../components/shared/StatCard';
import LoadingSpinner from '../../components/shared/LoadingSpinner';
import api from '../../services/api';
import { useAuth } from '../../contexts/AuthContext';

export default function UniversityDashboard() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const { user } = useAuth();

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const { data } = await api.get('/university/dashboard');
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

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-xl font-semibold text-[var(--text-primary)]">
            University Dashboard
          </h1>
          <p className="text-sm text-[var(--text-secondary)] mt-1">
            Welcome back{user?.name ? `, ${user.name}` : ''}
          </p>
        </div>

        <div className="grid gap-6 grid-cols-1 md:grid-cols-2 lg:grid-cols-5">
          <StatCard 
            icon={<Users className="h-5 w-5 text-blue-500" />} 
            label="Total Enrolled" 
            value={stats?.totalEnrolled ?? 0} 
            color="blue"
          />
          <StatCard 
            icon={<GraduationCap className="h-5 w-5 text-green-500" />} 
            label="Graduated Students" 
            value={stats?.graduatedStudents ?? 0} 
            color="green"
          />
          <StatCard 
            icon={<Award className="h-5 w-5 text-blue-500" />} 
            label="Certificates Issued" 
            value={stats?.certificatesIssued ?? 0} 
            color="blue"
          />
          <StatCard 
            icon={<AlertTriangle className="h-5 w-5 text-yellow-500" />} 
            label="Pending Withdrawals" 
            value={stats?.pendingWithdrawals ?? 0} 
            color="yellow"
          />
          <StatCard 
            icon={<Calendar className="h-5 w-5 text-green-500" />} 
            label="This Month's Certificates" 
            value={stats?.thisMonthCertificates ?? 0} 
            color="green"
          />
        </div>
      </div>
    </DashboardLayout>
  );
}
