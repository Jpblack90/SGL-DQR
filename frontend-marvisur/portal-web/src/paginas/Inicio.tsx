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
          Cotiza tu tarifa, genera tu <strong>Código Express</strong> para ventanilla, rastrea tu envío en tiempo real y retira con tu <strong>Billetera Digital</strong>. Fricción Cero sin contraseñas.
        </p>

        {/* Accesos rápidos principales */}
        <div className="pt-2 flex flex-wrap items-center justify-center gap-3">
          <Link
            to="/cotiza"
            className="w-full sm:w-auto inline-flex items-center justify-center px-7 py-3.5 rounded-xl font-bold text-white bg-rojo hover:bg-rojo-profundo shadow-lg shadow-rojo/25 transition-all transform active:scale-95 focus-visible:outline-rojo"
          >
            <svg className="w-5 h-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
            </svg>
            Cotiza tu Envío
          </Link>

          <Link
            to="/pre-registro"
            className="w-full sm:w-auto inline-flex items-center justify-center px-7 py-3.5 rounded-xl font-bold text-azul-noche bg-white hover:bg-slate-50 border border-slate-300 shadow-sm transition-all transform active:scale-95 focus-visible:outline-rojo"
          >
            <svg className="w-5 h-5 mr-2 text-rojo" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
            Pre-registro Express
          </Link>

          <Link
            to="/rastreo"
            className="w-full sm:w-auto inline-flex items-center justify-center px-6 py-3.5 rounded-xl font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 transition-all text-sm"
          >
            <svg className="w-4 h-4 mr-1.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            Rastrear Encomienda
          </Link>
        </div>
      </section>

      {/* Tarjetas de servicios principales (Grid de 4 servicios) */}
      <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        {/* Tarjeta Cotiza */}
        <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-md transition-shadow flex flex-col justify-between">
          <div>
            <div className="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center mb-4">
              <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
              </svg>
            </div>
            <h3 className="text-lg font-bold text-azul-noche mb-2">Cotización en Línea</h3>
            <p className="text-xs sm:text-sm text-slate-600 leading-relaxed mb-4">
              Calcula al instante la tarifa referencial por peso y origen/destino para planificar tu envío.
            </p>
          </div>
          <Link to="/cotiza" className="text-xs font-bold text-amber-600 hover:text-amber-800 inline-flex items-center gap-1">
            Cotizar ahora →
          </Link>
        </div>

        {/* Tarjeta Pre-registro */}
        <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-md transition-shadow flex flex-col justify-between">
          <div>
            <div className="w-12 h-12 rounded-xl bg-rojo-claro text-rojo flex items-center justify-center mb-4">
              <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
            <h3 className="text-lg font-bold text-azul-noche mb-2">Código Express</h3>
            <p className="text-xs sm:text-sm text-slate-600 leading-relaxed mb-4">
              Ahorra tiempo en ventanilla: ingresa los datos de envío y presenta tu código QR al depositar.
            </p>
          </div>
          <Link to="/pre-registro" className="text-xs font-bold text-rojo hover:text-rojo-profundo inline-flex items-center gap-1">
            Pre-registrar →
          </Link>
        </div>

        {/* Tarjeta Rastreo */}
        <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-md transition-shadow flex flex-col justify-between">
          <div>
            <div className="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center mb-4">
              <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
              </svg>
            </div>
            <h3 className="text-lg font-bold text-azul-noche mb-2">Rastreo Inmediato</h3>
            <p className="text-xs sm:text-sm text-slate-600 leading-relaxed mb-4">
              Consulta en vivo la línea de tiempo y ubicación de tu encomienda con tu comprobante oficial y DNI.
            </p>
          </div>
          <Link to="/rastreo" className="text-xs font-bold text-blue-600 hover:text-blue-800 inline-flex items-center gap-1">
            Rastrear ahora →
          </Link>
        </div>

        {/* Tarjeta Billetera Digital */}
        <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-md transition-shadow flex flex-col justify-between">
          <div>
            <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center mb-4">
              <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
              </svg>
            </div>
            <h3 className="text-lg font-bold text-azul-noche mb-2">Billetera Digital</h3>
            <p className="text-xs sm:text-sm text-slate-600 leading-relaxed mb-4">
              Tu llave criptográfica QR2 para retiro seguro de un solo uso en ventanilla de destino.
            </p>
          </div>
          <Link to="/billetera" className="text-xs font-bold text-emerald-600 hover:text-emerald-800 inline-flex items-center gap-1">
            Ver Billetera →
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
            Ecosistema de Doble Validación Criptográfica
          </h2>
          <p className="text-sm sm:text-base text-slate-300 leading-relaxed mb-6">
            Combinamos una etiqueta física indeleble (QR1) adherida al paquete con una llave digital única (QR2) entregada exclusivamente al destinatario. La validación cruzada en almacén garantiza que nadie pueda retirar tu carga sin autorización.
          </p>
          <div className="flex flex-wrap gap-4 text-xs">
            <div className="flex items-center gap-2 bg-white/10 px-4 py-2 rounded-xl border border-white/10">
              <span className="text-emerald-400 font-bold">✓</span> Validación en tiempo real
            </div>
            <div className="flex items-center gap-2 bg-white/10 px-4 py-2 rounded-xl border border-white/10">
              <span className="text-emerald-400 font-bold">✓</span> Invalidación irreversible del QR2
            </div>
            <div className="flex items-center gap-2 bg-white/10 px-4 py-2 rounded-xl border border-white/10">
              <span className="text-emerald-400 font-bold">✓</span> Cero contraseñas vulnerables
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};
