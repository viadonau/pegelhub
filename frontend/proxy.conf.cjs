const target = process.env['PEGELHUB_API_PROXY_TARGET'] || 'http://localhost:8080';

module.exports = {
  '/api': {
    target,
    secure: false,
    changeOrigin: true,
  },
};
