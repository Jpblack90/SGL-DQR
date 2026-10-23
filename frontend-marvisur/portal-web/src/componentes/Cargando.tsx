import React from 'react';

interface CargandoProps {
  texto?: string;
  className?: string;
}

export const Cargando: React.FC<CargandoProps> = ({
  texto = 'Cargando información...',
  className = '',
}) => {
  return (
    <div
      role="status"
      aria-live="polite"
      className={`flex flex-col items-center justify-center p-8 text-azul-noche-mutado ${className}`}
    >
      <div className="relative w-12 h-12 mb-3">
        <div className="w-12 h-12 rounded-full border-4 border-rojo/20 border-t-rojo animate-spin" />
      </div>
      <p className="text-sm font-medium animate-pulse">{texto}</p>
      <span className="sr-only">{texto}</span>
    </div>
  );
};

export const EsqueletoTarjeta: React.FC<{ lineas?: number }> = ({ lineas = 3 }) => {
  return (
    <div
      role="status"
      aria-label="Cargando contenido"
      className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200 animate-pulse space-y-4"
    >
      <div className="h-6 bg-slate-200 rounded w-1/3" />
      <div className="space-y-2">
        {Array.from({ length: lineas }).map((_, i) => (
          <div key={i} className="h-4 bg-slate-100 rounded w-full" />
        ))}
      </div>
      <span className="sr-only">Cargando...</span>
    </div>
  );
};
