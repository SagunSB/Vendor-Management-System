import axios from 'axios';

const api = axios.create({ baseURL: 'http://localhost:8080/api' });

export function setToken(token) {
  api.defaults.headers.common.Authorization = `Bearer ${token}`;
}

const saved = JSON.parse(localStorage.getItem('civwbms.session') || 'null');
if (saved?.token) setToken(saved.token);

export default api;
