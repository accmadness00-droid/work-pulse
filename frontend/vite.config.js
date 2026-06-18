import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";
export default defineConfig({
    plugins: [react()],
    build: {
        chunkSizeWarningLimit: 1100
    },
    server: {
        port: 5173
    }
});
