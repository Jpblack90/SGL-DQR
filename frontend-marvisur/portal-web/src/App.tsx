import React from 'react';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { Layout } from './componentes/Layout';
import { Agencias } from './paginas/Agencias';
import { Billetera } from './paginas/Billetera';
import { Inicio } from './paginas/Inicio';
import { NoEncontrado } from './paginas/NoEncontrado';
import { Rastreo } from './paginas/Rastreo';

export const App: React.FC = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Inicio />} />
          <Route path="rastreo" element={<Rastreo />} />
          <Route path="agencias" element={<Agencias />} />
          <Route path="billetera" element={<Billetera />} />
          <Route path="*" element={<NoEncontrado />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
};

export default App;
