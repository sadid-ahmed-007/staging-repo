export default function AccessDurationSelect({ value, onChange, error }) {
 return (
 <div>
 <label className="mb-1 block text-sm font-medium text-[var(--text-secondary)] ">
 Access duration
 </label>
 <select
 value={value}
 onChange={onChange}
 className="w-full rounded-lg border border-gray-300 bg-[var(--bg-surface)] px-3 py-2.5 text-sm text-[var(--text-primary)] outline-none transition focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500 "
 >
 <option value="">Select duration</option>
 <option value="7">7 days</option>
 <option value="30">30 days</option>
 <option value="90">90 days</option>
 <option value="365">365 days</option>
 </select>
 {error ? <p className="mt-1 text-xs text-red-600 ">{error}</p> : null}
 </div>
 );
}
