import React, { useEffect } from 'react';
import { Link } from 'react-router-dom';

export const NoEncontrado: React.FC = () => {
  useEffect(() => {
    document.title = 'Página no encontrada · Expreso Marvisur';
  }, []);
  return (
    <div className="text-center py-16 sm:py-24 max-w-lg mx-auto space-y-6">
      <div className="w-20 h-20 bg-rojo-claro text-rojo font-black text-3xl rounded-3xl mx-auto flex items-center justify-center shadow-sm">
        404
      </div>
      <div className="space-y-2">
        <h1 className="text-2xl sm:text-3xl font-extrabold text-azul-noche">
          Página no encontrada
        </h1>
        <p className="text-sm text-slate-600 leading-relaxed">
          La dirección que buscas no existe o fue movida. Puedes rastrear tu encomienda o volver a la página principal.
        </p>
      </div>

      <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-4">
        <Link
          to="/"
          className="w-full sm:w-auto px-6 py-3 rounded-xl font-bold text-white bg-rojo hover:bg-rojo-profundo transition-colors text-sm"
        >
          Volver al Inicio
        </Link>
        <Link
          to="/rastreo"
          className="w-full sm:w-auto px-6 py-3 rounded-xl font-bold text-azul-noche bg-white border border-slate-300 hover:bg-slate-50 transition-colors text-sm"
        >
          Rastrear Encomienda
        </Link>
      </div>
    </div>
  );
};
