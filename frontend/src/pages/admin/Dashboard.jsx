import { useEffect, useState } from 'react';
import { AlertCircle, Users, Award, Building, GraduationCap, Briefcase } from 'lucide-react';
import { useLocation } from 'react-router-dom';
import DashboardLayout from '../../components/layout/DashboardLayout';
import StatCard from '../../components/shared/StatCard';
import LoadingSpinner from '../../components/shared/LoadingSpinner';
import api from '../../services/api';
import { useAuth } from '../../contexts/AuthContext';

export default function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const { user } = useAuth();
  const location = useLocation();

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const { data } = await api.get('/admin/dashboard');
        setStats(data.data);
      } catch (error) {
        console.error('Failed to fetch dashboard:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchDashboard();
  }, [location]);

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
            Admin Dashboard
          </h1>
          <p className="text-sm text-[var(--text-secondary)] mt-1">
            Welcome back, System Admin
          </p>
        </div>

        <div className="grid gap-6 grid-cols-1 md:grid-cols-2 lg:grid-cols-3">
          <StatCard 
            icon={<AlertCircle className="h-5 w-5 text-red-500" />} 
            label="Pending Approvals" 
            value={stats?.pendingApprovals ?? 0} 
            color="red"
            to="/admin/users?status=pending"
          />
          <StatCard 
            icon={<Users className="h-5 w-5 text-blue-500" />} 
            label="Total Users" 
            value={stats?.totalUsers ?? 0} 
            color="blue"
            to="/admin/users"
          />
          <StatCard 
            icon={<Award className="h-5 w-5 text-blue-500" />} 
            label="Total Certificates" 
            value={stats?.totalCertificates ?? 0} 
            color="blue"
          />
          <StatCard 
            icon={<Building className="h-5 w-5 text-green-500" />} 
            label="Total Universities" 
            value={stats?.totalUniversities ?? 0} 
            color="green"
            to="/admin/users?role=university"
          />
          <StatCard 
            icon={<GraduationCap className="h-5 w-5 text-green-500" />} 
            label="Total Students" 
            value={stats?.totalStudents ?? 0} 
            color="green"
            to="/admin/users?role=student"
          />
          <StatCard 
            icon={<Briefcase className="h-5 w-5 text-gray-500" />} 
            label="Total Verifiers" 
            value={stats?.totalVerifiers ?? 0} 
            color="gray"
            to="/admin/users?role=verifier"
          />
        </div>
      </div>
    </DashboardLayout>
  );
}
