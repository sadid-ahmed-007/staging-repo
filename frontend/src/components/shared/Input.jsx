import React, { forwardRef } from 'react';
import { cn } from '../../utils/helpers';

const Input = forwardRef(({ 
 label, 
 error, 
 hint, 
 prefix, 
 suffix, 
 size = 'md', 
 required, 
 className = '', 
 id, 
 ...props 
}, ref) => {
 const inputId = id || props.name;
 
 const sizes = {
 sm: 'h-[36px]',
 md: 'h-[40px]',
 };

 const hasError = !!error;

 return (
 <div className={className}>
 {label && (
 <label htmlFor={inputId} className="mb-1 block text-sm font-medium text-[var(--text-secondary)]">
 {label} {required && <span className="text-[var(--danger)]">*</span>}
 </label>
 )}
 
 <div className="relative flex items-center">
 {prefix && <div className="absolute left-3 flex items-center text-[var(--text-muted)]">{prefix}</div>}
 <input
 id={inputId}
 ref={ref}
 required={required}
 className={cn(
 'w-full rounded-md border border-[var(--border)] bg-[var(--bg-surface)] text-sm text-[var(--text-primary)] outline-none transition-all',
 prefix ? 'pl-9' : 'pl-3',
 suffix ? 'pr-9' : 'pr-3',
 sizes[size],
 hasError
 ? 'border-[var(--danger)] focus:ring-2 focus:ring-[var(--danger)] focus:ring-opacity-50'
 : 'focus:border-[var(--brand)] focus:ring-2 focus:ring-[var(--brand)] focus:ring-opacity-50'
 )}
 {...props}
 />
 {suffix && <div className="absolute right-3 flex items-center text-[var(--text-muted)]">{suffix}</div>}
 </div>

 {hasError ? (
 <p className="mt-1 text-xs text-[var(--danger)]">{error}</p>
 ) : hint ? (
 <p className="mt-1 text-xs text-[var(--text-muted)]">{hint}</p>
 ) : null}
 </div>
 );
});

Input.displayName = 'Input';
export default Input;
