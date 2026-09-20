import React from 'react';
import { Dialog, DialogPanel, DialogTitle } from '@headlessui/react';
import { X } from 'lucide-react';
import Button from './Button';
import { cn } from '../../utils/helpers';

export default function Modal({ isOpen, open, onClose, title, size = 'md', children, footer }) {
 const actualIsOpen = isOpen !== undefined ? isOpen : open;
 const safeOnClose = typeof onClose === 'function' ? onClose : () => {};
 
 const sizes = {
 sm: 'max-w-[400px]',
 md: 'max-w-[600px]',
 lg: 'max-w-[800px]',
 xl: 'max-w-[1000px]',
 };

 return (
 <Dialog open={actualIsOpen} onClose={safeOnClose} className="relative z-50">
 <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" aria-hidden="true" />
 <div className="fixed inset-0 flex items-center justify-center p-4">
 <DialogPanel className={cn(
 "w-full bg-[var(--bg-surface)] rounded-[16px] shadow-[var(--shadow-lg)] max-h-[90vh] flex flex-col relative",
 sizes[size]
 )}>
 {title && (
 <div className="flex items-center justify-between border-b border-[var(--border)] px-[24px] py-[20px]">
 <DialogTitle className="text-base font-semibold text-[var(--text-primary)] pr-8">
 {title}
 </DialogTitle>
 </div>
 )}
 
 <div className="absolute right-3 top-3 z-10">
 <button 
 onClick={safeOnClose} 
 className="flex items-center justify-center w-[44px] h-[44px] rounded-full text-[var(--text-muted)] hover:text-[var(--text-primary)] hover:bg-[var(--bg-elevated)] transition-colors"
 aria-label="Close"
 >
 <X className="h-5 w-5" />
 </button>
 </div>

 <div className="overflow-y-auto p-[24px]">
 {children}
 </div>
 {footer && (
 <div className="flex justify-end gap-2 border-t border-[var(--border)] px-[24px] py-[16px]">
 {footer}
 </div>
 )}
 </DialogPanel>
 </div>
 </Dialog>
 );
}
