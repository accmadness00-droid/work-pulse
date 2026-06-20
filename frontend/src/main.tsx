import React from "react";
import ReactDOM from "react-dom/client";
import App from "./app/App";
import { I18nProvider } from "./shared/i18n/I18nProvider";
import { LocalizedConfigProvider } from "./shared/i18n/LocalizedConfigProvider";
import "./index.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <I18nProvider>
      <LocalizedConfigProvider>
        <App />
      </LocalizedConfigProvider>
    </I18nProvider>
  </React.StrictMode>
);
