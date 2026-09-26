import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import toast from 'react-hot-toast';
import { Lock } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import Button from '../shared/Button';
import Input from '../shared/Input';
import Card from '../shared/Card';

const loginSchema = yup.object().shape({
  email: yup.string().email('Please enter a valid email address').required('Email is required'),
  password: yup.string().required('Password is required'),
});

export default function LoginForm() {
  const [serverError, setServerError] = useState('');
  const [deactivatedData, setDeactivatedData] = useState(null);
  const [isReactivating, setIsReactivating] = useState(false);
  const [confirmPassword, setConfirmPassword] = useState('');

  const { login, reactivate } = useAuth();
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    getValues,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: yupResolver(loginSchema),
  });

  const onSubmit = async (data) => {
    setServerError('');
    setDeactivatedData(null);

    try {
      const response = await login(data);
      toast.success('Logged in successfully');
      redirectUser(response.user);
    } catch (err) {
      if (err.response?.data?.code === 'ACCOUNT_DEACTIVATED') {
        setDeactivatedData(err.response.data);
      } else {
        let errorMsg = 'Login failed';
        if (!err.response) {
          errorMsg = 'Network error. Please ensure the backend server is running.';
        } else {
          errorMsg = err.response.data?.message || err.response.data?.error || 'Login failed';
        }
        setServerError(errorMsg);
        toast.error(errorMsg);
      }
    }
  };

  const redirectUser = (user) => {
    if (user.role === 'student') navigate('/student/dashboard');
    else if (user.role === 'university') navigate('/university/dashboard');
    else if (user.role === 'verifier') navigate('/verifier/dashboard');
    else if (user.role === 'admin') navigate('/admin/dashboard');
  };

  const handleReactivate = async () => {
    if (!confirmPassword) {
      toast.error('Please enter your password to confirm');
      return;
    }
    
    setIsReactivating(true);
    setServerError('');
    
    try {
      const response = await reactivate({ email: deactivatedData.email, password: confirmPassword });
      toast.success('Account reactivated. Welcome back!');
      redirectUser(response.user);
    } catch (err) {
      const errorMsg = err.response?.data?.message || err.response?.data?.error || 'Reactivation failed';
      setServerError(errorMsg);
      toast.error(errorMsg);
    } finally {
      setIsReactivating(false);
    }
  };

  if (deactivatedData) {
    const formattedDate = deactivatedData.deactivatedAt 
      ? new Date(deactivatedData.deactivatedAt).toLocaleDateString(undefined, { 
          year: 'numeric', month: 'long', day: 'numeric' 
        })
      : 'recently';

    return (
      <div className="w-full text-center">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-[var(--bg-surface-hover)] mb-6">
          <Lock className="h-8 w-8 text-[var(--text-secondary)]" />
        </div>
        
        <h2 className="text-2xl font-bold text-[var(--text-primary)] mb-2">
          Your account is deactivated
        </h2>
        
        <p className="text-[var(--text-secondary)] mb-8 px-4 text-sm">
          This account was deactivated on {formattedDate}. Your data is safe and nothing has been deleted.
        </p>

        {serverError && (
          <div className="rounded-[8px] border border-[var(--danger)]/30 bg-[var(--danger)]/10 px-4 py-3 text-sm text-[var(--danger)] mb-6 text-left">
            {serverError}
          </div>
        )}

        <div className="space-y-6 text-left">
          <div>
            <label className="block text-sm font-medium text-[var(--text-secondary)] mb-1">
              Confirm your password to reactivate
            </label>
            <Input
              type="password"
              placeholder="••••••••"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              disabled={isReactivating}
            />
          </div>

          <Button 
            onClick={handleReactivate} 
            loading={isReactivating} 
            className="w-full" 
            size="lg"
          >
            Yes, Reactivate
          </Button>

          <div className="text-center pt-2">
            <button 
              type="button"
              onClick={() => {
                setDeactivatedData(null);
                setServerError('');
                setConfirmPassword('');
              }}
              className="text-sm font-medium text-[var(--text-secondary)] hover:text-[var(--text-primary)] transition-colors"
            >
              Not you? Go back to login
            </button>
          </div>
          
          <p className="text-xs text-[var(--text-muted)] text-center mt-6">
            If you did not deactivate this account, please contact support.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full">
      <div className="space-y-2 mb-8">
        <h2 className="text-3xl font-bold text-[var(--text-primary)]">Welcome back</h2>
        <p className="text-[var(--text-secondary)]">Sign in to your account</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
        {serverError && (
          <div className="rounded-[8px] border border-[var(--danger)]/30 bg-[var(--danger)]/10 px-4 py-3 text-sm text-[var(--danger)]">
            {serverError}
          </div>
        )}

        <Input
          type="email"
          label="Email Address"
          placeholder="you@example.com"
          {...register('email')}
          error={errors.email?.message}
        />

        <div>
          <div className="flex items-center justify-between mb-1">
            <label className="block text-sm font-medium text-[var(--text-secondary)]">Password</label>
            <Link to="/forgot-password" className="text-sm font-medium text-[var(--brand)] hover:underline">Forgot password?</Link>
          </div>
          <Input
            type="password"
            placeholder="••••••••"
            {...register('password')}
            error={errors.password?.message}
          />
        </div>

        <Button type="submit" loading={isSubmitting} className="w-full mt-2" size="lg">
          Sign In
        </Button>
      </form>

      <div className="mt-8 flex items-center justify-center space-x-4">
        <span className="h-[1px] w-full bg-[var(--border)]"></span>
        <span className="text-xs font-semibold uppercase text-[var(--text-muted)]">or</span>
        <span className="h-[1px] w-full bg-[var(--border)]"></span>
      </div>

      <p className="mt-8 text-center text-sm text-[var(--text-secondary)]">
        Don&apos;t have an account?{' '}
        <Link to="/register" className="font-semibold text-[var(--brand)] hover:underline">
          Register
        </Link>
      </p>
    </div>
  );
}
