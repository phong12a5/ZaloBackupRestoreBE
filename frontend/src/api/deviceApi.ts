import apiClient from './axios';
import type { Device, BackedUpAccount } from '@/types';

// Lấy danh sách thiết bị
export const getMyDevices = async (): Promise<Device[]> => {
  const response = await apiClient.get<Device[]>('/api/devices/user/me');
  return response.data;
};

// Lấy chi tiết thiết bị
export const getDeviceById = async (deviceId: string): Promise<Device> => {
  const response = await apiClient.get<Device>(`/api/devices/${deviceId}`);
  return response.data;
};

// Yêu cầu backup
export const requestDeviceBackup = async (deviceId: string): Promise<void> => {
  await apiClient.post(`/api/devices/${deviceId}/backup`);
};

// Yêu cầu export friends
export const requestFriendsExport = async (deviceId: string): Promise<void> => {
  await apiClient.post(`/api/devices/${deviceId}/export-friends`);
};

// Lấy danh sách tài khoản đã backup
export const getMyBackedUpAccounts = async (): Promise<BackedUpAccount[]> => {
  const response = await apiClient.get<BackedUpAccount[]>('/api/devices/user/me/accounts');
  return response.data;
};

// Xóa một tài khoản đã backup
export const deleteBackedUpAccount = async (backedUpAccountId: string): Promise<void> => {
  await apiClient.delete(`/api/devices/backups/${backedUpAccountId}`);
};