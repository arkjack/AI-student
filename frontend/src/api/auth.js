/* 认证相关 API */
import request from './request'

export const login = (data) => request.post('/auth/login', data)

export const register = (data) => request.post('/auth/register', data)

export const getMe = () => request.get('/user/me')

export const getClasses = () => request.get('/auth/classes')
