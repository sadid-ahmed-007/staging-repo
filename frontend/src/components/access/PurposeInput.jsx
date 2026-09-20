export default function PurposeInput({ value, onChange, error, minLength = 20, maxLength = 500, placeholder = "Explain why you need access to the student's certificates..." }) {
 return (
 <div>
 <label className="mb-1 block text-sm font-medium text-[var(--text-secondary)] ">
 Purpose
 </label>
 <textarea
 value={value}
 onChange={onChange}
 rows={5}
 maxLength={maxLength}
 className="w-full rounded-lg border border-gray-300 bg-[var(--bg-surface)] px-3 py-2.5 text-sm text-[var(--text-primary)] outline-none transition focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500 "
 placeholder={placeholder}
 />
 <div className="mt-1 flex items-center justify-between text-xs text-[var(--text-muted)] dark:text-[var(--text-muted)]">
 <span>Minimum {minLength} characters, maximum {maxLength} characters.</span>
 <span>{value.length}/{maxLength}</span>
 </div>
 {error ? <p className="mt-1 text-xs text-red-600 ">{error}</p> : null}
 </div>
 );
}
