import React from 'react';

export type TipoAlerta = 'error' | 'advertencia' | 'exito' | 'info';

interface AlertaProps {
  tipo?: TipoAlerta;
  titulo?: string;
  mensaje: React.ReactNode;
  accion?: React.ReactNode;
  className?: string;
}

export const Alerta: React.FC<AlertaProps> = ({
  tipo = 'info',
  titulo,
  mensaje,
  accion,
  className = '',
}) => {
  const estilos = {
    error: {
      fondo: 'bg-red-50 border-red-300 text-red-900',
      icono: (
        <svg className="w-5 h-5 text-red-600 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
      ),
    },
    advertencia: {
      fondo: 'bg-amber-50 border-amber-300 text-amber-900',
      icono: (
        <svg className="w-5 h-5 text-amber-600 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      ),
    },
    exito: {
      fondo: 'bg-emerald-50 border-emerald-300 text-emerald-900',
      icono: (
        <svg className="w-5 h-5 text-emerald-600 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      ),
    },
    info: {
      fondo: 'bg-blue-50 border-blue-300 text-blue-900',
      icono: (
        <svg className="w-5 h-5 text-blue-600 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      ),
    },
  }[tipo];

  return (
    <div
      role="alert"
      className={`border rounded-xl p-4 flex gap-3 shadow-sm ${estilos.fondo} ${className}`}
    >
      <div className="mt-0.5">{estilos.icono}</div>
      <div className="flex-1">
        {titulo && <h4 className="font-bold text-sm mb-1">{titulo}</h4>}
        <div className="text-sm leading-relaxed">{mensaje}</div>
        {accion && <div className="mt-3">{accion}</div>}
      </div>
    </div>
  );
};
