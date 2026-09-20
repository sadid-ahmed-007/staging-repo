import { Link } from 'react-router-dom';
import Button from '../components/shared/Button';

export default function NotFound() {
 return (
 <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4 ">
 <div className="max-w-md text-center">
 <p className="text-sm font-semibold uppercase tracking-[0.15em] text-primary-600">404</p>
 <h1 className="mt-3 text-4xl font-bold text-[var(--text-primary)] ">Page not found</h1>
 <p className="mt-4 text-[var(--text-secondary)] dark:text-[var(--text-muted)]">The page you requested does not exist or has been moved.</p>
 <div className="mt-6">
 <Link to="/"><Button>Go home</Button></Link>
 </div>
 </div>
 </div>
 );
}
