/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        rojo: {
          DEFAULT: '#E30613',
          profundo: '#D5011D',
          claro: '#FEE2E2',
        },
        'azul-noche': {
          DEFAULT: '#0F172A',
          suave: '#1E293B',
          mutado: '#334155',
        },
        fondo: {
          DEFAULT: '#F8FAFC',
          tarjeta: '#FFFFFF',
        },
        verde: {
          DEFAULT: '#10B981',
          claro: '#D1FAE5',
          oscuro: '#065F46',
        },
        ambar: {
          DEFAULT: '#F59E0B',
          claro: '#FEF3C7',
          oscuro: '#92400E',
        },
        azul: {
          DEFAULT: '#2563EB',
          claro: '#DBEAFE',
          oscuro: '#1E40AF',
        },
      },
      fontFamily: {
        sans: ['Poppins', 'system-ui', 'sans-serif'],
      },
      screens: {
        xs: '360px',
      },
    },
  },
  plugins: [],
};
