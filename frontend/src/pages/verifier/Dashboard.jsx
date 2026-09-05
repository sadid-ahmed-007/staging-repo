import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Users, Clock, Calendar, ShieldCheck, CheckCircle, XCircle } from 'lucide-react';
import DashboardLayout from '../../components/layout/DashboardLayout';
import StatCard from '../../components/shared/StatCard';
import LoadingSpinner from '../../components/shared/LoadingSpinner';
import api from '../../services/api';
import { useAuth } from '../../contexts/AuthContext';

export default function VerifierDashboard() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const { user } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const { data } = await api.get('/verifier/dashboard');
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
            Verifier Dashboard
          </h1>
          <p className="text-sm text-[var(--text-secondary)] mt-1">
            Welcome back{user?.name ? `, ${user.name}` : ''}
          </p>
        </div>

        <div className="grid gap-6 grid-cols-1 md:grid-cols-2 lg:grid-cols-3">
          <StatCard 
            icon={<Users className="h-5 w-5 text-blue-500" />} 
            label="Accessible Students" 
            value={stats?.accessibleStudents ?? 0} 
            color="blue"
          />
          <StatCard 
            icon={<Clock className="h-5 w-5 text-yellow-500" />} 
            label="Pending Requests" 
            value={stats?.pendingRequests ?? 0} 
            color="yellow"
          />
          <div 
            onClick={() => navigate('/verifier/verification-history?filter=today')}
            className="cursor-pointer transition-transform hover:scale-[1.02]"
          >
            <StatCard 
              icon={<Calendar className="h-5 w-5 text-green-500" />} 
              label="Verifications Today" 
              value={stats?.verificationsToday ?? 0} 
              color="green"
            />
          </div>
          <div 
            onClick={() => navigate('/verifier/verification-history')}
            className="cursor-pointer transition-transform hover:scale-[1.02]"
          >
            <StatCard 
              icon={<ShieldCheck className="h-5 w-5 text-blue-500" />} 
              label="Total Verifications" 
              value={stats?.totalVerifications ?? 0} 
              color="blue"
            />
          </div>
          <StatCard 
            icon={<CheckCircle className="h-5 w-5 text-green-500" />} 
            label="Approved Requests" 
            value={stats?.approvedRequests ?? 0} 
            color="green"
          />
          <StatCard 
            icon={<XCircle className="h-5 w-5 text-red-500" />} 
            label="Rejected Requests" 
            value={stats?.rejectedRequests ?? 0} 
            color="red"
          />
        </div>
      </div>
    </DashboardLayout>
  );
}
