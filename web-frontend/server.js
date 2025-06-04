const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const path = require('path');

const app = express();
const PORT = 5000;

// Serve static files from the current directory
app.use(express.static('.'));

// Proxy API requests to the Task Service
app.use('/api', createProxyMiddleware({
    target: 'http://localhost:8001',
    changeOrigin: true,
    pathRewrite: {
        '^/api': ''
    },
    onError: (err, req, res) => {
        console.error('Proxy error:', err);
        res.status(500).json({
            code: 500,
            message: 'Task Service unavailable',
            error: err.message
        });
    }
}));

// Serve the main page
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'index.html'));
});

app.listen(PORT, '0.0.0.0', () => {
    console.log(`多语言音频翻译系统前端服务器运行在: http://0.0.0.0:${PORT}`);
    console.log(`Task Service API代理: http://localhost:8001 -> /api`);
});