MCGA Frontend

This is a minimal Vue 3 + Vite frontend for the Minecraft Gaiety Accelerator backend.

Features:
- Token input saved to cookie (used as Authorization header)
- UI panels to call all controller endpoints exposed by the backend (server, chest_info, stat/overhead)

Quick start (macOS / zsh):

1. cd into the frontend folder

```bash
cd frontend
```

2. Install dependencies

```bash
npm install
```

3. Run development server

```bash
npm run dev
```

The dev server proxies API calls to http://localhost:8080 by default (see vite.config.ts). If your backend runs on a different port, update the proxy settings in `vite.config.ts`.

Notes:
- Token is stored in a cookie named `mcga_token` and applied as the `Authorization` header for requests.
- The UI is intentionally simple — it exposes the endpoints so you can exercise them. You can extend components as needed.
{
  "name": "mcga-frontend",
  "version": "0.1.0",
  "private": true,
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview --port 5173"
  },
  "dependencies": {
    "axios": "^1.4.0",
    "js-cookie": "^3.0.5",
    "vue": "^3.3.4"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^4.1.0",
    "typescript": "^5.4.0",
    "vite": "^5.2.0"
  }
}

