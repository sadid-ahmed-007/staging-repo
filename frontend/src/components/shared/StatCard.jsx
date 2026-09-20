import React from 'react';
import Card from './Card';
import { cn } from '../../utils/helpers';
import { ArrowUpRight, ArrowDownRight } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function StatCard({ 
 title, 
 label, // for backward compatibility
 value, 
 icon, 
 trend, 
 trendValue, 
 variant = 'default', 
 color, // for backward compatibility
 loading = false,
 to = '#',
 tooltip = '',
 pulse = false
}) {
 const actualTitle = title || label;
 const actualVariant = variant !== 'default' ? variant : (color || 'default');

 const iconColors = {
 default: 'bg-blue-50 text-blue-600 /40 ',
 primary: 'bg-indigo-50 text-indigo-600 /40 ',
 blue: 'bg-blue-50 text-blue-600 /40 ',
 success: 'bg-emerald-50 text-emerald-600 /40 ',
 green: 'bg-emerald-50 text-emerald-600 /40 ',
 warning: 'bg-amber-50 text-amber-600 /40 ',
 yellow: 'bg-amber-50 text-amber-600 /40 ',
 orange: 'bg-orange-50 text-orange-600 /40 ',
 danger: 'bg-rose-50 text-rose-600 /40 ',
 red: 'bg-rose-50 text-rose-600 /40 ',
 purple: 'bg-purple-50 text-purple-600 /40 ',
 gray: 'bg-gray-100 text-[var(--text-secondary)] dark:text-[var(--text-muted)]',
 };

 if (loading) {
 return (
 <Card className="h-full">
 <div className="flex items-start justify-between gap-3">
 <div className="min-w-0 flex-1">
 <div className="h-4 w-24 animate-pulse rounded bg-[var(--bg-elevated)]" />
 <div className="mt-3 h-8 w-16 animate-pulse rounded bg-[var(--bg-elevated)]" />
 {trend && <div className="mt-2 h-3 w-12 animate-pulse rounded bg-[var(--bg-elevated)]" />}
 </div>
 <div className="h-11 w-11 shrink-0 animate-pulse rounded-xl bg-[var(--bg-elevated)]" />
 </div>
 </Card>
 );
 }

 return (
 <Link to={to} title={tooltip} className="block h-full transition-transform hover:-translate-y-1 hover:scale-[1.01]">
 <Card className="relative h-full flex flex-col justify-between overflow-hidden">
 {pulse && (
 <span className="absolute top-3 right-3 flex h-2.5 w-2.5" title="Requires attention">
 <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75"></span>
 <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-amber-500"></span>
 </span>
 )}
 <div className="flex items-start justify-between gap-3">
 <div className="min-w-0 flex-1">
 <p className="text-sm font-medium text-[var(--text-secondary)] line-clamp-1" title={actualTitle}>
 {actualTitle}
 </p>
 <p className="mt-2 text-2xl font-bold tracking-tight text-[var(--text-primary)]">
 {value}
 </p>
 {trend && trendValue && (
 <div className={cn(
 "mt-2 flex items-center text-xs font-medium",
 trend === 'up' ? "text-[var(--success)]" : "text-[var(--danger)]"
 )}>
 {trend === 'up' ? <ArrowUpRight className="mr-1 h-3 w-3" /> : <ArrowDownRight className="mr-1 h-3 w-3" />}
 {trendValue}
 </div>
 )}
 </div>
 {icon && (
 <div className={cn("flex h-11 w-11 shrink-0 items-center justify-center rounded-xl", iconColors[actualVariant] || iconColors.default)}>
 {icon}
 </div>
 )}
 </div>
 </Card>
 </Link>
 );
}
