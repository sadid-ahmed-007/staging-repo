import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
 Bell,
 GraduationCap,
 Award,
 UserPlus,
 UserX,
 CheckCircle,
 XCircle,
 AlertCircle,
 Calendar,
 Check,
 CheckCheck,
 X
} from 'lucide-react';
import api from '../../services/api';
import { useOutsideClick } from '../../hooks/useOutsideClick';
import { cn, timeAgo } from '../../utils/helpers';
import { useNotifications } from '../../contexts/NotificationContext';

const TYPE_CONFIG = {
 CERTIFICATE_ISSUED: { icon: Award, color: 'text-green-500', bg: 'bg-green-100 /30' },
 ENROLLMENT_CONFIRMED: { icon: GraduationCap, color: 'text-blue-500', bg: 'bg-blue-100 /30' },
 ACCESS_REQUEST: { icon: UserPlus, color: 'text-orange-500', bg: 'bg-orange-100 /30' },
 ACCESS_APPROVED: { icon: CheckCircle, color: 'text-green-500', bg: 'bg-green-100 /30' },
 ACCESS_REJECTED: { icon: XCircle, color: 'text-red-500', bg: 'bg-red-100 /30' },
 ACCESS_REVOKED: { icon: UserX, color: 'text-red-500', bg: 'bg-red-100 /30' },
 WITHDRAWAL_REQUESTED: { icon: AlertCircle, color: 'text-yellow-500', bg: 'bg-yellow-100 /30' },
 WITHDRAWAL_APPROVED: { icon: CheckCircle, color: 'text-green-500', bg: 'bg-green-100 /30' },
 WITHDRAWAL_REJECTED: { icon: XCircle, color: 'text-red-500', bg: 'bg-red-100 /30' },
 ACCOUNT_APPROVED: { icon: CheckCircle, color: 'text-green-500', bg: 'bg-green-100 /30' },
 GRADUATION_EXTENDED: { icon: Calendar, color: 'text-blue-500', bg: 'bg-blue-100 /30' },
 INFO: { icon: Bell, color: 'text-[var(--text-muted)]', bg: 'bg-gray-100 ' },
};

export default function NotificationDropdown() {
 const [isOpen, setIsOpen] = useState(false);
 const [notifications, setNotifications] = useState([]);
 const { unreadCount, setUnreadCount } = useNotifications();
 const dropdownRef = useRef(null);
 const navigate = useNavigate();

 const closeDropdown = useCallback(() => setIsOpen(false), []);
 useOutsideClick(dropdownRef, closeDropdown);

 const fetchNotifications = async () => {
 try {
 const { data } = await api.get('/notifications');
 setNotifications(data.data || []);
 if (data.unread_count !== undefined) {
 setUnreadCount(data.unread_count);
 }
 } catch (error) {
 console.error('Failed to fetch notifications', error);
 }
 };

 useEffect(() => {
 if (isOpen) {
 fetchNotifications();
 }
 }, [isOpen]);

 const markAsRead = async (id, e) => {
 e.stopPropagation();
 try {
 await api.post(`/notifications/${id}/read`);
 setNotifications(prev => prev?.map(n => n.id === id ? { ...n, read: true } : n));
 setUnreadCount(prev => Math.max(0, prev - 1));
 } catch (error) {
 console.error('Failed to mark notification as read', error);
 }
 };

 const dismissNotification = async (id, e) => {
 e.stopPropagation();
 try {
 await api.post(`/notifications/${id}/read`);
 setNotifications(prev => prev.filter(n => n.id !== id));
 setUnreadCount(prev => Math.max(0, prev - 1));
 } catch (error) {
 console.error('Failed to dismiss notification', error);
 }
 };

 const markAllAsRead = async () => {
 try {
 await api.post('/notifications/read-all');
 setNotifications(prev => prev?.map(n => ({ ...n, read: true })));
 setUnreadCount(0);
 } catch (error) {
 console.error('Failed to mark all as read', error);
 }
 };

 const handleNotificationClick = (notification) => {
 if (!notification.read) {
 markAsRead(notification.id, { stopPropagation: () => { } });
 }
 setIsOpen(false);
 if (notification.actionUrl) {
 navigate(notification.actionUrl);
 }
 };

 return (
 <div className="relative" ref={dropdownRef}>
 <button
 onClick={() => setIsOpen(!isOpen)}
 className="w-[36px] h-[36px] flex items-center justify-center rounded-md text-[var(--text-secondary)] hover:bg-[var(--bg-elevated)] hover:text-[var(--text-primary)] transition-colors relative"
 >
 <Bell className="w-5 h-5" />
 {unreadCount > 0 && (
 <span className="absolute top-1 right-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-500 text-[10px] font-bold text-white px-1 ring-2 ring-[var(--bg-surface)]">
 {unreadCount > 9 ? '9+' : unreadCount}
 </span>
 )}
 </button>

 {isOpen && (
 <div className="absolute right-0 mt-2 w-[380px] max-h-[480px] bg-[var(--bg-surface)] border border-[var(--border)] rounded-xl shadow-lg z-50 flex flex-col overflow-hidden">
 <div className="px-4 py-3 border-b border-[var(--border)] flex items-center justify-between">
 <h3 className="text-sm font-semibold text-[var(--text-primary)]">Notifications</h3>
 {unreadCount > 0 && (
 <button
 onClick={markAllAsRead}
 className="px-2 py-1 rounded text-xs font-medium text-[var(--brand)] hover:bg-[var(--brand-light)] transition-colors flex items-center gap-1 cursor-pointer"
 >
 <CheckCheck className="w-4 h-4" />
 <span>Mark all as read</span>
 </button>
 )}
 </div>

 <div className="overflow-y-auto flex-1">
 {notifications.length === 0 ? (
 <div className="p-6 text-center text-sm text-[var(--text-muted)]">
 No notifications
 </div>
 ) : (
 <div className="divide-y divide-[var(--border)]">
 {notifications?.map((notification) => {
 const config = TYPE_CONFIG[notification.type] || TYPE_CONFIG.INFO;
 const Icon = config.icon;

 return (
 <div
 key={notification.id}
 onClick={() => handleNotificationClick(notification)}
 className={cn(
 "p-[12px_16px] min-h-[64px] hover:bg-[var(--bg-elevated)] transition-colors cursor-pointer group flex gap-3",
 !notification.read ? "bg-blue-50 /10" : "bg-[var(--bg-surface)]"
 )}
 >
 <div className={cn("flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center", config.bg, config.color)}>
 <Icon className="w-5 h-5" />
 </div>
 <div className="flex-1 min-w-0 flex flex-col justify-center">
 <div className="flex items-start justify-between gap-2">
 <p className={cn("text-sm text-[var(--text-primary)] leading-tight", !notification.read ? "font-bold" : "font-medium")}>
 {notification.title}
 </p>
 <span className="text-xs text-[var(--text-muted)] whitespace-nowrap">
 {timeAgo(notification.createdAt)}
 </span>
 </div>
 <p className="text-xs text-[var(--text-secondary)] mt-1 truncate max-w-[240px]">
 {notification.message}
 </p>
 </div>
 
 <div className="flex-shrink-0 flex items-center gap-1">
 {!notification.read && (
 <button
 onClick={(e) => markAsRead(notification.id, e)}
 className="p-1 rounded-full text-blue-500 hover:bg-blue-100 dark:hover:bg-blue-900/30 transition-colors"
 title="Mark as read"
 >
 <Check className="w-4 h-4" />
 </button>
 )}
 <button
 onClick={(e) => dismissNotification(notification.id, e)}
 className="p-1 rounded-full text-[var(--text-muted)] hover:text-[var(--text-primary)] hover:bg-[var(--bg-elevated)] transition-colors opacity-0 group-hover:opacity-100"
 title="Dismiss"
 >
 <X className="w-3.5 h-3.5" />
 </button>
 </div>
 </div>
 );
 })}
 </div>
 )}
 </div>

 <div className="p-2 border-t border-[var(--border)]">
 <button
 onClick={() => { setIsOpen(false); navigate('/notifications'); }}
 className="w-full py-2 text-sm text-center font-medium text-[var(--brand)] hover:bg-[var(--brand-light)] rounded-md transition-colors"
 >
 See all notifications &rarr;
 </button>
 </div>
 </div>
 )}
 </div>
 );
}
