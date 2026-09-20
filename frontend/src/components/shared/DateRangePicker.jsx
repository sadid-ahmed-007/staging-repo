import { useRef } from 'react';
import { Calendar, X } from 'lucide-react';
import { cn } from '../../utils/helpers';

/**
 * DateRangePicker — a professional, design-system-aware date range filter.
 *
 * Props:
 * fromDate string | '' — ISO date string (YYYY-MM-DD) or empty
 * toDate string | ''
 * onFromChange (value: string) => void
 * onToChange (value: string) => void
 * onClear () => void
 * className string (optional)
 */
export default function DateRangePicker({
 fromDate,
 toDate,
 onFromChange,
 onToChange,
 onClear,
 className = '',
}) {
 const fromRef = useRef(null);
 const toRef = useRef(null);
 const hasValue = fromDate || toDate;

 return (
 <div className={cn('flex items-center gap-2 flex-wrap', className)}>
 {/* From Date */}
 <div className="relative">
 <label className="absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none">
 <Calendar className="w-4 h-4 text-[var(--text-muted)]" />
 </label>
 <div className="absolute left-9 top-1/2 -translate-y-1/2 pointer-events-none">
 <span className="text-xs text-[var(--text-muted)] font-medium">From</span>
 </div>
 <input
 ref={fromRef}
 type="date"
 value={fromDate || ''}
 onChange={(e) => onFromChange(e.target.value)}
 className="date-range-input pl-[72px]"
 style={{ colorScheme: 'light dark' }}
 />
 </div>

 {/* Separator */}
 <span className="text-[var(--text-muted)] text-sm font-medium select-none">—</span>

 {/* To Date */}
 <div className="relative">
 <label className="absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none">
 <Calendar className="w-4 h-4 text-[var(--text-muted)]" />
 </label>
 <div className="absolute left-9 top-1/2 -translate-y-1/2 pointer-events-none">
 <span className="text-xs text-[var(--text-muted)] font-medium">To</span>
 </div>
 <input
 ref={toRef}
 type="date"
 value={toDate || ''}
 min={fromDate || ''}
 onChange={(e) => onToChange(e.target.value)}
 className="date-range-input pl-[56px]"
 style={{ colorScheme: 'light dark' }}
 />
 </div>

 {/* Clear Button */}
 {hasValue && (
 <button
 onClick={onClear}
 className="flex items-center gap-1 text-xs font-medium text-[var(--text-muted)] hover:text-[var(--danger)] transition-colors px-2 py-1.5 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/10"
 >
 <X className="w-3.5 h-3.5" />
 Clear
 </button>
 )}
 </div>
 );
}
