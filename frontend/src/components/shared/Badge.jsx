import React from 'react';
import { cn } from '../../utils/helpers';

export default function Badge({ children, variant = 'default', size = 'md', dot = false, className = '' }) {
 const variants = {
 default: 'bg-gray-100 text-[var(--text-secondary)] ', // Cancelled/Neutral
 primary: 'bg-[var(--brand)]/10 text-[var(--brand)]', 
 success: 'bg-green-100 text-green-700 /30 ', // Active/Success
 warning: 'bg-yellow-100 text-yellow-700 /30 ', // Pending/Warning
 danger: 'bg-red-100 text-red-700 /30 ', // Rejected/Error
 info: 'bg-blue-100 text-blue-700 /30 ', 
 orange: 'bg-orange-100 text-orange-700 /30 ', // Withdrawn
 };

 const sizes = {
 sm: 'text-[10px] px-2 py-0.5',
 md: 'text-xs px-2.5 py-0.5',
 lg: 'text-sm px-3 py-1',
 };

 const dotColors = {
 default: 'bg-gray-500',
 primary: 'bg-[var(--brand)]',
 success: 'bg-green-500',
 warning: 'bg-yellow-500',
 danger: 'bg-red-500',
 info: 'bg-blue-500',
 orange: 'bg-orange-500',
 };

 return (
 <span className={cn('inline-flex items-center rounded-full font-medium', variants[variant], sizes[size], className)}>
 {dot && (
 <span className={cn('mr-1.5 h-1.5 w-1.5 rounded-full', dotColors[variant])} />
 )}
 {children}
 </span>
 );
}
