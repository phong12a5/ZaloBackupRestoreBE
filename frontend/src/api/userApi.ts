import apiClient from './axios';
import type { UserSafeDto } from '@/types';

export const getCurrentUser = async (): Promise<UserSafeDto> => {
  const response = await apiClient.get<UserSafeDto>('/users/me');
  return response.data;
};

export const getAllUsers = async (): Promise<UserSafeDto[]> => {
  const response = await apiClient.get<UserSafeDto[]>('/users/all');
  return response.data;
};
