import React from 'react';
import { Link } from 'react-router-dom';

export const Inicio: React.FC = () => {
  return (
    <div className="space-y-12 sm:space-y-16">
      {/* Sección Hero */}
      <section className="text-center py-6 sm:py-12 px-2 max-w-4xl mx-auto space-y-6">
        <span className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full text-xs font-bold uppercase tracking-wider bg-rojo-claro text-rojo border border-rojo/20">
          <span className="w-2 h-2 rounded-full bg-rojo animate-pulse" />
          Tecnología Criptográfica Doble QR
        </span>

        <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight text-azul-noche leading-[1.15]">
          Tu carga y encomiendas seguras con <span className="text-rojo">Expreso Marvisur</span>
        </h1>

        <p className="text-base sm:text-lg text-slate-600 max-w-2xl mx-auto leading-relaxed">
          Rastrea tu envío en tiempo real y retira tus encomiendas con tu <strong>Billetera Digital</strong> de un solo uso. Máxima seguridad sin contraseñas ni trámites lentos.
        </p>

        <div className="pt-2 flex flex-col sm:flex-row items-center justify-center gap-4">
          <Link
            to="/rastreo"
            className="w-full sm:w-auto inline-flex items-center justify-center px-8 py-3.5 rounded-xl font-bold text-white bg-rojo hover:bg-rojo-profundo shadow-lg shadow-rojo/25 transition-all transform active:scale-95 focus-visible:outline-rojo"
          >
            <svg className="w-5 h-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            Rastrear mi Encomienda
          </Link>

          <Link
            to="/billetera"
            className="w-full sm:w-auto inline-flex items-center justify-center px-8 py-3.5 rounded-xl font-bold text-azul-noche bg-white hover:bg-slate-50 border border-slate-300 shadow-sm transition-all transform active:scale-95 focus-visible:outline-rojo"
          >
            <svg className="w-5 h-5 mr-2 text-rojo" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z" />
            </svg>
            Ir a la Billetera Digital
          </Link>
        </div>
      </section>

      {/* Tarjetas de servicios principales */}
      <section className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white rounded-2xl p-6 sm:p-8 border border-slate-200 shadow-sm hover:shadow-md transition-shadow">
          <div className="w-12 h-12 rounded-xl bg-rojo-claro text-rojo flex items-center justify-center mb-5">
            <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>
          </div>
          <h3 className="text-xl font-bold text-azul-noche mb-2">Rastreo Inmediato</h3>
          <p className="text-sm text-slate-600 leading-relaxed mb-4">
            Ingresa tu número de comprobante y tu documento de identidad para conocer la ubicación exacta y el estado de tu carga.
          </p>
          <Link to="/rastreo" className="text-sm font-bold text-rojo hover:text-rojo-profundo inline-flex items-center gap-1">
            Consultar ahora →
          </Link>
        </div>

        <div className="bg-white rounded-2xl p-6 sm:p-8 border border-slate-200 shadow-sm hover:shadow-md transition-shadow">
          <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center mb-5">
            <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
            </svg>
          </div>
          <h3 className="text-xl font-bold text-azul-noche mb-2">Billetera Digital</h3>
          <p className="text-sm text-slate-600 leading-relaxed mb-4">
            Tu llave digital de retiro en tu celular. El código QR2 se genera al vuelo y se invalida automáticamente tras la entrega.
          </p>
          <Link to="/billetera" className="text-sm font-bold text-blue-600 hover:text-blue-800 inline-flex items-center gap-1">
            Ver Billetera →
          </Link>
        </div>

        <div className="bg-white rounded-2xl p-6 sm:p-8 border border-slate-200 shadow-sm hover:shadow-md transition-shadow">
          <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center mb-5">
            <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          </div>
          <h3 className="text-xl font-bold text-azul-noche mb-2">Agencias a Nivel Nacional</h3>
          <p className="text-sm text-slate-600 leading-relaxed mb-4">
            Encuentra las direcciones y horarios de atención de nuestras agencias en Lima, Arequipa, Huancayo y todo el Perú.
          </p>
          <Link to="/agencias" className="text-sm font-bold text-emerald-600 hover:text-emerald-800 inline-flex items-center gap-1">
            Ver Agencias →
          </Link>
        </div>
      </section>

      {/* Propuesta de valor y seguridad */}
      <section className="bg-gradient-to-br from-azul-noche to-azul-noche-suave text-white rounded-3xl p-8 sm:p-12 shadow-xl">
        <div className="max-w-3xl">
          <span className="text-rojo text-xs font-bold uppercase tracking-wider mb-2 block">
            Cero Suplantaciones · Cero Fraudes
          </span>
          <h2 className="text-2xl sm:text-4xl font-extrabold mb-4 leading-tight">
            ¿Cómo protege el sistema Doble QR tu encomienda?
          </h2>
          <p className="text-slate-300 text-sm sm:text-base leading-relaxed mb-8">
            Cada encomienda cuenta con dos códigos independientes: un código físico en la etiqueta (QR1) y una llave digital única enviada a tu Billetera (QR2). La entrega solo es autorizada cuando ambos códigos coinciden en el sistema y se invalida de inmediato.
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 pt-2 border-t border-slate-700/60">
            <div>
              <div className="text-2xl font-black text-rojo">100%</div>
              <div className="text-xs text-slate-400 mt-1">Hashes criptográficos SHA-256 en base de datos.</div>
            </div>
            <div>
              <div className="text-2xl font-black text-emerald-400">&lt; 1 seg</div>
              <div className="text-xs text-slate-400 mt-1">Validación cruzada inmediata en ventanilla.</div>
            </div>
            <div>
              <div className="text-2xl font-black text-amber-400">1 Solo Uso</div>
              <div className="text-xs text-slate-400 mt-1">El QR2 queda invalidado de forma irreversible.</div>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};
