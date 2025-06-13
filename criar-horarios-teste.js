// Script para criar horários de teste via API
// Substitua SEU_TOKEN_ADMIN pelo seu token JWT de admin

const axios = require('axios');

const token = 'eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIyNTMzMTM0MDEzNzAwNTA1NjAiLCJpYXQiOjE3NDk4MjM3MjgsImV4cCI6MTc0OTkxMDEyOH0.HPV__IGvsqdFh9SklaKe2A5PgmdjVxS5T6hrcKVgJ9dA-1VCmh73xjj7TeX40GWaS9oNhqhd50JBU_i4QSHWMw'; // Coloque aqui seu token JWT de admin
const apiUrl = 'http://localhost:8080/api/timeslots';

const slots = [
  {
    startTime: '2025-06-14T14:00:00',
    endTime: '2025-06-14T15:00:00',
    description: 'Teste horário 1'
  },
  {
    startTime: '2025-06-14T16:00:00',
    endTime: '2025-06-14T17:00:00',
    description: 'Teste horário 2'
  },
  {
    startTime: '2025-06-15T10:00:00',
    endTime: '2025-06-15T11:00:00',
    description: 'Teste horário 3'
  }
];

(async () => {
  for (const slot of slots) {
    try {
      const res = await axios.post(apiUrl, slot, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      console.log('Criado:', res.data);
    } catch (err) {
      console.error('Erro ao criar:', err.response?.data || err.message);
    }
  }
})();
